package io.orion.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(UUID id, UUID tenantId, UUID userId, String tokenHash,
                           Instant expiresAt, Instant revokedAt, Instant createdAt) {
    public static RefreshToken issue(UUID tenantId, UUID userId, String tokenHash, Instant expiresAt, Instant now) {
        return new RefreshToken(UUID.randomUUID(), tenantId, userId, tokenHash, expiresAt, null, now);
    }
    public boolean isRevoked() { return revokedAt != null; }
    public boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }
}
