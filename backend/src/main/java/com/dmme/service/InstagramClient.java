package com.dmme.service;

import com.dmme.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over the Meta Graph API for Instagram messaging + comments.
 *
 * Uses the Send API on the connected Facebook Page. All calls require the
 * page access token obtained during the Instagram connect (OAuth) flow.
 *
 * Docs: Instagram Messaging API (Messenger Platform) + Instagram Graph API.
 */
@Service
public class InstagramClient {

    private final WebClient web;
    private final WebClient igWeb;
    private final AppProperties props;

    public InstagramClient(AppProperties props) {
        this.props = props;
        this.web = WebClient.builder().baseUrl(props.getInstagram().getGraphBaseUrl()).build();
        this.igWeb = WebClient.builder().baseUrl(props.getInstagram().getIgGraphBaseUrl()).build();
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
            Map<?, ?> res = web.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/" + igsid)
                            .queryParam("fields", "is_user_follow_business")
                            .queryParam("access_token", pageAccessToken)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return res != null && Boolean.TRUE.equals(res.get("is_user_follow_business"));
        } catch (Exception e) {
            return false;
        }
    }

    private void post(String path, String accessToken, Map<String, Object> body) {
        web.post()
                .uri(uriBuilder -> uriBuilder.path(path)
                        .queryParam("access_token", accessToken).build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
