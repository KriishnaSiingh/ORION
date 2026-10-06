package io.orion.identity.application.port;

import io.orion.identity.domain.Tenant;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(UUID id);
    Optional<Tenant> findBySlug(String slug);
}
