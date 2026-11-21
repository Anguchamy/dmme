package com.dmme.service;

import com.dmme.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over the Instagram Graph API (Instagram Login) for messaging + comments.
 *
 * Tokens from the Instagram Login OAuth flow are Instagram User access tokens and
 * must be sent to {@code graph.instagram.com}, not {@code graph.facebook.com}.
 *
 * Docs: Instagram API with Instagram Login — Messaging + Comments.
 */
@Service
public class InstagramClient {

    private static final Logger log = LoggerFactory.getLogger(InstagramClient.class);

    private final WebClient igWeb;
    private final AppProperties props;

    public InstagramClient(AppProperties props) {
        this.props = props;
        ExchangeFilterFunction errorFilter = graphErrorFilter();
        this.igWeb = WebClient.builder()
                .baseUrl(props.getInstagram().getIgGraphBaseUrl())
                .filter(errorFilter)
                .build();
    }

    private static ExchangeFilterFunction graphErrorFilter() {
        return (request, next) -> next.exchange(request).flatMap(response -> {
            if (!response.statusCode().isError()) {
                return Mono.just(response);
            }
            String path = request.url().getPath();
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> {
                        InstagramApiException ex = InstagramApiException.parse(response.statusCode(), path, body);
                        log.warn(
                                "Instagram Graph API error: status={} path={} type={} code={} subcode={} message=\"{}\" fbtrace_id={}",
                                ex.getHttpStatus().value(),
                                ex.getRequestPath(),
                                orDash(ex.getGraphType()),
                                orDash(ex.getGraphCode()),
                                orDash(ex.getErrorSubcode()),
                                ex.getGraphMessage() != null ? ex.getGraphMessage() : "",
                                orDash(ex.getFbtraceId()));
                        return Mono.error(ex);
                    });
        });
    }

    private static String orDash(Object value) {
        return value != null ? String.valueOf(value) : "-";
    }

    /**
     * Subscribe the connected Instagram account to this app's webhook fields
     * (comments + messages). Without this, Meta does not deliver comment/DM
     * events for the account even when the app-level webhook is configured.
     */
    public String subscribeToWebhooks(String accessToken, String igUserId) {
        return igWeb.post()
                .uri(b -> b.path("/" + igUserId + "/subscribed_apps")
                        .queryParam("subscribed_fields", "comments,messages,live_comments")
                        .queryParam("access_token", accessToken)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Read which webhook fields this account is currently subscribed to.
     * Useful for diagnosing "automation never fires" — if data is empty the
     * account is not subscribed and Meta will not deliver comment/DM events.
     */
    public String getSubscribedApps(String accessToken, String igUserId) {
        return igWeb.get()
                .uri(b -> b.path("/" + igUserId + "/subscribed_apps")
                        .queryParam("access_token", accessToken)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * List the connected account's recent media (posts + reels) via the
     * Instagram Graph API, so the creator can pick a post visually instead of
     * pasting a media id.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listMedia(String accessToken, String igUserId) {
        Map<?, ?> res = igWeb.get()
                .uri(b -> b.path("/" + igUserId + "/media")
                        .queryParam("fields", "id,caption,media_type,media_url,thumbnail_url,permalink,timestamp")
                        .queryParam("limit", 50)
                        .queryParam("access_token", accessToken)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (res == null || res.get("data") == null) return List.of();
        return (List<Map<String, Object>>) res.get("data");
    }

    /** Send a plain-text DM to an Instagram-scoped user id (IGSID). */
    public void sendText(String pageAccessToken, String recipientIgsid, String text) {
        Map<String, Object> payload = Map.of(
                "recipient", Map.of("id", recipientIgsid),
                "message", Map.of("text", text)
        );
        post("/me/messages", pageAccessToken, payload);
    }

    /**
     * Private reply to a commenter via {@code recipient.comment_id}.
     * Required for comment→DM: comment webhook {@code from.id} is not always a valid
     * messaging IGSID; Meta resolves the recipient from the comment id instead.
     *
     * @return the commenter's IGSID from the API response, or null if absent
     */
    @SuppressWarnings("unchecked")
    public String sendPrivateReply(String pageAccessToken, String commentId, String text) {
        Map<String, Object> payload = Map.of(
                "recipient", Map.of("comment_id", commentId),
                "message", Map.of("text", text)
        );
        Map<?, ?> res = igWeb.post()
                .uri(uriBuilder -> uriBuilder.path("/me/messages")
                        .queryParam("access_token", pageAccessToken).build())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (res == null || res.get("recipient_id") == null) return null;
        return String.valueOf(res.get("recipient_id"));
    }

    /**
     * Send a button template DM. Buttons are [{title,url}] pairs rendered as
     * web_url buttons in a generic/button template.
     */
    public void sendButtons(String pageAccessToken, String recipientIgsid,
                            String text, List<Map<String, String>> buttons) {
        var buttonPayload = buttons.stream()
                .map(b -> Map.<String, Object>of(
                        "type", "web_url",
                        "title", b.getOrDefault("title", "Open"),
                        "url", b.getOrDefault("url", "https://")))
                .toList();

        Map<String, Object> attachment = Map.of(
                "type", "template",
                "payload", Map.of(
                        "template_type", "button",
                        "text", text,
                        "buttons", buttonPayload));

        Map<String, Object> payload = Map.of(
                "recipient", Map.of("id", recipientIgsid),
                "message", Map.of("attachment", attachment));

        post("/me/messages", pageAccessToken, payload);
    }

    /** Post a public reply on a comment. */
    public void replyToComment(String pageAccessToken, String commentId, String message) {
        post("/" + commentId + "/replies", pageAccessToken,
                Map.of("message", message));
    }

    /** Check whether the given IGSID follows the connected account. */
    public boolean isFollower(String pageAccessToken, String igUserId, String igsid) {
        try {
            Map<?, ?> res = igWeb.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/" + igsid)
                            .queryParam("fields", "is_user_follow_business")
                            .queryParam("access_token", pageAccessToken)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return res != null && Boolean.TRUE.equals(res.get("is_user_follow_business"));
        } catch (InstagramApiException e) {
            log.warn(
                    "Follow check failed due to Graph API error (treating as non-follower, not confirmed): "
                            + "status={} path={} type={} code={} subcode={} message=\"{}\" fbtrace_id={}",
                    e.getHttpStatus().value(),
                    e.getRequestPath(),
                    orDash(e.getGraphType()),
                    orDash(e.getGraphCode()),
                    orDash(e.getErrorSubcode()),
                    e.getGraphMessage() != null ? e.getGraphMessage() : "",
                    orDash(e.getFbtraceId()));
            return false;
        } catch (Exception e) {
            log.warn("Follow check failed due to unexpected error (treating as non-follower, not confirmed): {}",
                    e.getMessage());
            return false;
        }
    }

    /** POST to Instagram Graph ({@code graph.instagram.com}) with an IG user access token. */
    private String post(String path, String accessToken, Map<String, Object> body) {
        return igWeb.post()
                .uri(uriBuilder -> uriBuilder.path(path)
                        .queryParam("access_token", accessToken).build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
