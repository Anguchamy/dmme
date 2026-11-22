package com.dmme.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {}

    public record EmailPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            String emailRedirectTo
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record RecoverRequest(
            @NotBlank @Email String email,
            String redirectTo
    ) {}

    public record OAuthStartRequest(
            @NotBlank String redirectTo,
            @NotBlank String codeChallenge
    ) {}

    public record OAuthExchangeRequest(
            @NotBlank String code,
            @NotBlank String codeVerifier
    ) {}

    public record OAuthUrlResponse(String url) {}

    public record UpdatePasswordRequest(@NotBlank String password) {}
}
