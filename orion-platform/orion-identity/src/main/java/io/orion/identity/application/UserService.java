package io.orion.identity.application;

import io.orion.identity.application.port.RoleRepository;
import io.orion.identity.application.port.UserRepository;
import io.orion.identity.domain.User;
import io.orion.shared.audit.AuditEvent;
import io.orion.shared.audit.AuditPublisher;
import io.orion.shared.paging.PageResult;
import io.orion.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;

@Service
public class UserService {

    public record CreateUserCommand(String email, String name, String password, Set<String> roles) {}

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository users;
    private final RoleRepository roles;
    private final io.orion.identity.application.port.PasswordHasher hasher;
    private final Clock clock;
    private final AuditPublisher audit;

    public UserService(UserRepository users, RoleRepository roles, io.orion.identity.application.port.PasswordHasher hasher, Clock clock, AuditPublisher audit) {
        this.users = users; this.roles = roles; this.hasher = hasher; this.clock = clock; this.audit = audit;
    }

    @Transactional
    public User createUser(CreateUserCommand cmd) {
        UUID tenantId = TenantContext.requireTenantId();
        io.orion.shared.tenant.CurrentUser actor = TenantContext.requireUser();

        Set<String> known = roles.findAllNames(tenantId);
        Set<String> unknown = new TreeSet<>(cmd.roles());
        unknown.removeAll(known);
        if (!unknown.isEmpty()) {
            throw new io.orion.shared.error.OrionException(io.orion.shared.error.ErrorCode.VALIDATION_FAILED, "Unknown roles: " + unknown);
        }

        User user = users.save(User.create(tenantId, cmd.email(), cmd.name(),
                hasher.hash(cmd.password()), cmd.roles(), clock.instant()));

        audit.publish(AuditEvent.of(IdentityAuditActions.USER_CREATED)
                .tenant(tenantId).actor(actor.userId(), actor.email())
                .resource("User", user.id()).detail("email", user.email()).build());

        return user;
    }

    @Transactional(readOnly = true)
    public PageResult<User> listUsers(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return users.findAll(TenantContext.requireTenantId(), Math.max(page, 0), safeSize);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        io.orion.shared.tenant.CurrentUser me = TenantContext.requireUser();
        return users.findById(me.tenantId(), me.userId())
                .orElseThrow(() -> new io.orion.shared.error.OrionException(io.orion.shared.error.ErrorCode.NOT_FOUND, "User not found"));
    }
}
