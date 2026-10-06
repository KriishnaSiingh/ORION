package io.orion.identity.application.port;

import io.orion.identity.domain.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface RefreshTokenRepository {
    void save(RefreshToken token);
    Optional<RefreshToken> findByHash(String tokenHash);
    boolean revoke(UUID tokenId, Instant at);
    void revokeAllForUser(UUID tenantId, UUID userId, Instant at);
}
