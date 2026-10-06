package io.orion.shared.tenant;

import java.util.Set;
import java.util.UUID;

public record CurrentUser(UUID userId, UUID tenantId, String email, Set<String> roles) {
    public CurrentUser { roles = Set.copyOf(roles); }
    public boolean hasRole(String role) { return roles.contains(role); }
}
