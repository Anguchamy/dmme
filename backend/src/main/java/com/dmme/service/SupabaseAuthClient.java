package com.dmme.service;

import com.dmme.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Server-side client for Supabase GoTrue. The anon key stays on the backend only.
 */
@Component
public class SupabaseAuthClient {

    private final WebClient web;
    private final String projectUrl;
    private final String anonKey;
    private final ObjectMapper mapper;

    public SupabaseAuthClient(AppProperties props, ObjectMapper mapper) {
        // Accept either project root or a /rest/v1 URL — Auth needs the project origin.
        this.projectUrl = normalizeProjectUrl(props.getSupabase().getProjectUrl());
        this.anonKey = props.getSupabase().getAnonKey();
        this.mapper = mapper;
        this.web = WebClient.builder()
                .baseUrl(this.projectUrl)
                .defaultHeader("apikey", this.anonKey != null ? this.anonKey : "")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + (this.anonKey != null ? this.anonKey : ""))
                .build();
    }

    public Map<String, Object> signUp(String email, String password, String emailRedirectTo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        WebClient.RequestBodySpec req = web.post()
                .uri("/auth/v1/signup")
                .contentType(MediaType.APPLICATION_JSON);
        if (emailRedirectTo != null && !emailRedirectTo.isBlank()) {
            req = req.header("Redirect-To", emailRedirectTo);
        }
        return exchange(req.bodyValue(body));
    }

    public Map<String, Object> signIn(String email, String password) {
        return exchange(web.post()
                .uri(uriBuilder -> uriBuilder.path("/auth/v1/token")
                        .queryParam("grant_type", "password")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password)));
    }

    public Map<String, Object> refresh(String refreshToken) {
        return exchange(web.post()
                .uri(uriBuilder -> uriBuilder.path("/auth/v1/token")
                        .queryParam("grant_type", "refresh_token")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refresh_token", refreshToken)));
    }

    public void recover(String email, String redirectTo) {
        WebClient.RequestBodySpec req = web.post()
                .uri("/auth/v1/recover")
                .contentType(MediaType.APPLICATION_JSON);
        if (redirectTo != null && !redirectTo.isBlank()) {
            req = req.header("Redirect-To", redirectTo);
        }
        exchange(req.bodyValue(Map.of("email", email)));
    }

    /** Requires the recovery / logged-in user access token (not the anon key). */
    public Map<String, Object> updatePassword(String accessToken, String newPassword) {
        ensureConfigured();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = web.put()
                    .uri("/auth/v1/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("password", newPassword))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return body != null ? body : Map.of();
        } catch (WebClientResponseException e) {
            throw mapError(e);
        }
    }

    public void logout(String accessToken) {
        try {
            web.post()
                    .uri("/auth/v1/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            // Best-effort: client will clear local session regardless.
            if (e.getStatusCode().value() >= 500) {
                throw mapError(e);
            }
        }
    }

    public Map<String, Object> exchangePkce(String authCode, String codeVerifier) {
        return exchange(web.post()
                .uri(uriBuilder -> uriBuilder.path("/auth/v1/token")
                        .queryParam("grant_type", "pkce")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "auth_code", authCode,
                        "code_verifier", codeVerifier)));
    }

    public String googleAuthorizeUrl(String redirectTo, String codeChallenge) {
        return UriComponentsBuilder
                .fromHttpUrl(projectUrl + "/auth/v1/authorize")
                .queryParam("provider", "google")
                .queryParam("redirect_to", redirectTo)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build(true)
                .toUriString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchange(WebClient.RequestHeadersSpec<?> spec) {
        ensureConfigured();
        try {
            Map<String, Object> body = spec.retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return body != null ? body : Map.of();
        } catch (WebClientResponseException e) {
            throw mapError(e);
        }
    }

    private void ensureConfigured() {
        if (projectUrl == null || projectUrl.isBlank() || projectUrl.contains("your-project")) {
            throw new ResponseStatusException(BAD_GATEWAY, "Supabase project URL is not configured");
        }
        if (anonKey == null || anonKey.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Supabase anon key is not configured on the server");
        }
    }

    private ResponseStatusException mapError(WebClientResponseException e) {
        String message = "Authentication failed";
        try {
            JsonNode root = mapper.readTree(e.getResponseBodyAsString());
            if (root.hasNonNull("error_description")) {
                message = root.get("error_description").asText();
            } else if (root.hasNonNull("msg")) {
                message = root.get("msg").asText();
            } else if (root.hasNonNull("error")) {
                message = root.get("error").asText();
            } else if (root.hasNonNull("message")) {
                message = root.get("message").asText();
            }
        } catch (Exception ignored) {
            if (e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isBlank()) {
                message = e.getResponseBodyAsString();
            }
        }
        int status = e.getStatusCode().value();
        if (status == 400 || status == 422) {
            return new ResponseStatusException(BAD_REQUEST, message);
        }
        if (status == 401) {
            return new ResponseStatusException(UNAUTHORIZED, message);
        }
        return new ResponseStatusException(BAD_GATEWAY, message);
    }

    /**
     * Auth calls use {@code {project}/auth/v1/...}. If {@code SUPABASE_URL} was set to the
     * PostgREST base ({@code .../rest/v1}), strip that path so both formats work.
     */
    static String normalizeProjectUrl(String url) {
        if (url == null || url.isBlank()) return "";
        String u = url.trim();
        // Strip query/fragment if someone pasted a full REST URL.
        int q = u.indexOf('?');
        if (q >= 0) u = u.substring(0, q);
        int hash = u.indexOf('#');
        if (hash >= 0) u = u.substring(0, hash);
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        if (u.endsWith("/rest/v1")) {
            u = u.substring(0, u.length() - "/rest/v1".length());
        }
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
