package io.orion.identity.application.port;

import io.orion.identity.domain.Role;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface RoleRepository {
    void saveAll(Collection<Role> roles);
    Set<String> findAllNames(UUID tenantId);
}
