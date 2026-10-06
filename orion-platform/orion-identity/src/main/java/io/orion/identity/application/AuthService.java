package io.orion.identity.application;

import io.orion.identity.application.port.*;
import io.orion.identity.domain.*;
import io.orion.shared.audit.AuditEvent;
import io.orion.shared.audit.AuditPublisher;
import io.orion.shared.error.OrionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class AuthService {

    public record RegisterTenantCommand(String tenantName, String tenantSlug,
                                        String adminEmail, String adminName, String password) {}
    public record LoginCommand(String tenantSlug, String email, String password) {}

    private final TenantRepository tenants;
    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher hasher;
    private final TokenService tokenService;
    private final AuditPublisher audit;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(TenantRepository tenants, UserRepository users, RoleRepository roles,
                       RefreshTokenRepository refreshTokens, PasswordHasher hasher,
                       TokenService tokenService, AuditPublisher audit, Clock clock) {
        this.tenants = tenants; this.users = users; this.roles = roles;
        this.refreshTokens = refreshTokens; this.hasher = hasher;
        this.tokenService = tokenService; this.audit = audit; this.clock = clock;
        this.dummyHash = hasher.hash(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthTokens registerTenant(RegisterTenantCommand cmd) {
        Instant now = clock.instant();
        Tenant tenant = tenants.save(Tenant.create(cmd.tenantName(), cmd.tenantSlug(), now));
        roles.saveAll(Arrays.stream(SystemRole.values()).map(r -> Role.system(tenant.id(), r, now)).toList());
        User admin = users.save(User.create(tenant.id(), cmd.adminEmail(), cmd.adminName(),
                hasher.hash(cmd.password()), Set.of(SystemRole.TENANT_ADMIN.name()), now));

        audit.publish(AuditEvent.of(IdentityAuditActions.TENANT_REGISTERED)
                .tenant(tenant.id()).actor(admin.id(), admin.email())
                .resource("Tenant", tenant.id()).detail("slug", tenant.slug()).build());
        return issueTokens(admin, now);
    }

    public AuthTokens login(LoginCommand cmd) {
        Instant now = clock.instant();
        String slug = cmd.tenantSlug().trim().toLowerCase(Locale.ROOT);
        String email = User.normalizeEmail(cmd.email());

        Optional<Tenant> tenant = tenants.findBySlug(slug).filter(Tenant::isActive);
        Optional<User> user = tenant.flatMap(t -> users.findByEmail(t.id(), email)).filter(User::isActive);

        boolean passwordOk = hasher.matches(cmd.password(), user.map(User::passwordHash).orElse(dummyHash));
        if (user.isEmpty() || !passwordOk) {
            String reason = tenant.isEmpty() ? "UNKNOWN_OR_INACTIVE_TENANT"
                          : user.isEmpty()   ? "UNKNOWN_OR_DISABLED_USER" : "BAD_PASSWORD";
            audit.publish(AuditEvent.of(IdentityAuditActions.LOGIN_FAILED)
                    .outcome(AuditEvent.Outcome.FAILURE)
                    .tenant(tenant.map(Tenant::id).orElse(null))
                    .actor(user.map(User::id).orElse(null), email)
                    .resource("User", user.map(User::id).orElse(null))
                    .detail("tenantSlug", slug).detail("reason", reason).build());
            throw OrionException.unauthenticated("Invalid credentials");
        }

        users.recordLogin(user.get().tenantId(), user.get().id(), now);
        audit.publish(AuditEvent.of(IdentityAuditActions.LOGIN_SUCCEEDED)
                .tenant(user.get().tenantId()).actor(user.get().id(), user.get().email())
                .resource("User", user.get().id()).build());
        return issueTokens(user.get(), now);
    }

    @Transactional(noRollbackFor = OrionException.class)
    public AuthTokens refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokens.findByHash(tokenService.hashRefreshToken(rawRefreshToken))
                .orElseThrow(() -> OrionException.unauthenticated("Invalid refresh token"));

        if (!token.isRevoked() && token.isExpired(now)) {
            throw OrionException.unauthenticated("Refresh token expired");
        }
        if (token.isRevoked() || !refreshTokens.revoke(token.id(), now)) {
            refreshTokens.revokeAllForUser(token.tenantId(), token.userId(), now);
            audit.publish(AuditEvent.of(IdentityAuditActions.TOKEN_REUSE_DETECTED)
                    .outcome(AuditEvent.Outcome.FAILURE)
                    .tenant(token.tenantId()).actor(token.userId(), null)
                    .resource("User", token.userId()).detail("tokenId", token.id()).build());
            throw OrionException.unauthenticated("Refresh token reuse detected");
        }

        User user = users.findById(token.tenantId(), token.userId()).filter(User::isActive)
                .orElseThrow(() -> OrionException.unauthenticated("Invalid refresh token"));
        tenants.findById(user.tenantId()).filter(Tenant::isActive)
                .orElseThrow(() -> OrionException.unauthenticated("Invalid refresh token"));
        return issueTokens(user, now);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        Instant now = clock.instant();
        refreshTokens.findByHash(tokenService.hashRefreshToken(rawRefreshToken)).ifPresent(t -> {
            if (refreshTokens.revoke(t.id(), now)) {
                audit.publish(AuditEvent.of(IdentityAuditActions.LOGOUT)
                        .tenant(t.tenantId()).actor(t.userId(), null).resource("User", t.userId()).build());
            }
        });
    }

    private AuthTokens issueTokens(User user, Instant now) {
        TokenService.AccessToken access = tokenService.issueAccessToken(user);
        String rawRefresh = tokenService.generateRefreshToken();
        refreshTokens.save(RefreshToken.issue(user.tenantId(), user.id(),
                tokenService.hashRefreshToken(rawRefresh), now.plus(tokenService.refreshTokenTtl()), now));
        return new AuthTokens(access.value(), rawRefresh, "Bearer", access.expiresInSeconds());
    }

    public record AuthTokens(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
}
