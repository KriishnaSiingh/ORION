package io.orion.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record Role(UUID id, UUID tenantId, String name, String description, boolean system, Instant createdAt) {
    public static Role system(UUID tenantId, SystemRole role, Instant now) {
        return new Role(UUID.randomUUID(), tenantId, role.name(), role.description(), true, now);
    }
}
