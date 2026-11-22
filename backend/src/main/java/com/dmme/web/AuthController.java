package com.dmme.web;

import com.dmme.service.SupabaseAuthClient;
import com.dmme.web.dto.AuthDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Proxies Supabase Auth so the browser never holds the Supabase anon key.
 * Session JWTs are still Supabase-issued and verified by {@code SupabaseJwtFilter}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SupabaseAuthClient auth;

    public AuthController(SupabaseAuthClient auth) {
        this.auth = auth;
    }

    @PostMapping("/signup")
    public Map<String, Object> signup(@Valid @RequestBody EmailPasswordRequest req) {
        return auth.signUp(req.email(), req.password(), req.emailRedirectTo());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        return auth.signIn(req.email(), req.password());
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody RecoverRequest req) {
        auth.recover(req.email(), req.redirectTo());
        return Map.of("status", "ok");
    }

    /**
     * Completes the password-reset email flow. Caller must send the recovery
     * access token from the email link (Authorization: Bearer …).
     */
    @PostMapping("/update-password")
    public Map<String, Object> updatePassword(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UpdatePasswordRequest req) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Recovery session required to set a new password");
        }
        return auth.updatePassword(authorization.substring(7), req.password());
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            auth.logout(authorization.substring(7));
        }
        return Map.of("status", "ok");
    }

    @PostMapping("/oauth/google")
    public OAuthUrlResponse googleStart(@Valid @RequestBody OAuthStartRequest req) {
        return new OAuthUrlResponse(auth.googleAuthorizeUrl(req.redirectTo(), req.codeChallenge()));
    }

    @PostMapping("/oauth/exchange")
    public Map<String, Object> oauthExchange(@Valid @RequestBody OAuthExchangeRequest req) {
        return auth.exchangePkce(req.code(), req.codeVerifier());
    }
}
