package io.orion.identity.application.port;

import io.orion.identity.domain.User;
import io.orion.shared.paging.PageResult;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID tenantId, UUID userId);
    Optional<User> findByEmail(UUID tenantId, String email);
    PageResult<User> findAll(UUID tenantId, int page, int size);
    void recordLogin(UUID tenantId, UUID userId, Instant at);
}
