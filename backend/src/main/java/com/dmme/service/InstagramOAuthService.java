package com.dmme.service;

import com.dmme.config.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * Implements the "Instagram API with Instagram Login" OAuth flow.
 *
 * Steps:
 *  1. Redirect the creator to the authorize URL (built by {@link #authorizeUrl}).
 *  2. Instagram redirects back to our redirect URI with a short-lived {@code code}.
 *  3. Exchange the code for a short-lived token, upgrade it to a long-lived token,
 *     then fetch the Instagram user id + username ({@link #exchangeCode}).
 *
 * Docs: https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login
 */
@Service
public class InstagramOAuthService {

    private final AppProperties props;
    private final WebClient web = WebClient.builder().build();

    public InstagramOAuthService(AppProperties props) {
        this.props = props;
    }

    public boolean isConfigured() {
        var ig = props.getInstagram();
        return notBlank(ig.getAppId()) && notBlank(ig.getAppSecret()) && notBlank(ig.getRedirectUri());
    }

    /** Build the Instagram authorize URL the browser should be redirected to. */
    public String authorizeUrl(String state) {
        var ig = props.getInstagram();
        return UriComponentsBuilder.fromUriString(ig.getOauthAuthorizeUrl())
                .queryParam("client_id", ig.getAppId())
                .queryParam("redirect_uri", ig.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", ig.getScopes())
                .queryParam("state", state)
                .build(true)
                .toUriString();
    }

    public record ConnectedAccount(String igUserId, String igUsername, String accessToken, Instant expiresAt) {}

    /** Exchange the authorization code for a long-lived token and the account profile. */
    public ConnectedAccount exchangeCode(String code) {
        var ig = props.getInstagram();

        // 1. Short-lived token + user id.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", ig.getAppId());
        form.add("client_secret", ig.getAppSecret());
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", ig.getRedirectUri());
        form.add("code", code);

        Map<?, ?> shortRes = web.post()
                .uri(ig.getOauthTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (shortRes == null || shortRes.get("access_token") == null) {
            throw new IllegalStateException("Instagram did not return an access token");
        }
        String shortToken = String.valueOf(shortRes.get("access_token"));
        String userId = String.valueOf(shortRes.get("user_id"));

        // 2. Upgrade to a long-lived (60-day) token.
        Map<?, ?> longRes = web.get()
                .uri(ig.getIgGraphBaseUrl(), b -> b.path("/access_token")
                        .queryParam("grant_type", "ig_exchange_token")
                        .queryParam("client_secret", ig.getAppSecret())
                        .queryParam("access_token", shortToken)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String longToken = shortToken;
        Instant expiresAt = null;
        if (longRes != null && longRes.get("access_token") != null) {
            longToken = String.valueOf(longRes.get("access_token"));
            Object expiresIn = longRes.get("expires_in");
            if (expiresIn != null) {
                expiresAt = Instant.now().plusSeconds(Long.parseLong(String.valueOf(expiresIn)));
            }
        }

        // 3. Fetch the profile (username) for display.
        String finalToken = longToken;
        Map<?, ?> profile = web.get()
                .uri(ig.getIgGraphBaseUrl(), b -> b.path("/me")
                        .queryParam("fields", "user_id,username")
                        .queryParam("access_token", finalToken)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String username = null;
        if (profile != null) {
            if (profile.get("username") != null) username = String.valueOf(profile.get("username"));
            if (profile.get("user_id") != null) userId = String.valueOf(profile.get("user_id"));
        }

        return new ConnectedAccount(userId, username, longToken, expiresAt);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
