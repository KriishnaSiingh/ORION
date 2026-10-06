package io.orion.identity.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record Tenant(UUID id, String name, String slug, Status status, Instant createdAt) {
    public enum Status { ACTIVE, SUSPENDED }

    public static Tenant create(String name, String slug, Instant now) {
        return new Tenant(UUID.randomUUID(), name.trim(), slug.trim().toLowerCase(Locale.ROOT), Status.ACTIVE, now);
    }
    public boolean isActive() { return status == Status.ACTIVE; }
}
