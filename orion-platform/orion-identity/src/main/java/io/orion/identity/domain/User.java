package io.orion.identity.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record User(UUID id, UUID tenantId, String email, String name, String passwordHash,
                   Status status, Set<String> roles, Instant createdAt, Instant lastLoginAt) {
    public enum Status { ACTIVE, DISABLED }

    public User { roles = Set.copyOf(roles); }

    public static User create(UUID tenantId, String email, String name, String passwordHash,
                              Set<String> roles, Instant now) {
        return new User(UUID.randomUUID(), tenantId, normalizeEmail(email), name.trim(),
                        passwordHash, Status.ACTIVE, roles, now, null);
    }

    public static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    public boolean isActive() { return status == Status.ACTIVE; }

    @Override public String toString() {
        return "User[id=%s, tenantId=%s, email=%s]".formatted(id, tenantId, email);
    }
}
