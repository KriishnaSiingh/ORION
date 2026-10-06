package io.orion.identity.application.port;

import java.time.Duration;

public interface TokenService {
    AccessToken issueAccessToken(io.orion.identity.domain.User user);
    String generateRefreshToken();
    String hashRefreshToken(String rawToken);
    Duration refreshTokenTtl();

    record AccessToken(String value, long expiresInSeconds) {}
}
