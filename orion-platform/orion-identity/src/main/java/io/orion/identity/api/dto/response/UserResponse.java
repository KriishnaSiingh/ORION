package io.orion.identity.api.dto.response;

import io.orion.identity.domain.User;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id, UUID tenantId, String email, String name, String status,
                           Set<String> roles, Instant createdAt, Instant lastLoginAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.id(), u.tenantId(), u.email(), u.name(), u.status().name(),
                u.roles(), u.createdAt(), u.lastLoginAt());
    }
}
