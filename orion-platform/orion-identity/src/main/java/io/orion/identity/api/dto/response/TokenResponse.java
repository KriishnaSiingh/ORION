package io.orion.identity.api.dto.response;

import io.orion.identity.application.AuthService.AuthTokens;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    public static TokenResponse from(AuthTokens t) {
        return new TokenResponse(t.accessToken(), t.refreshToken(), t.tokenType(), t.expiresIn());
    }
}
