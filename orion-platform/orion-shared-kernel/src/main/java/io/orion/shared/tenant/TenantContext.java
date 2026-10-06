package io.orion.shared.tenant;

import io.orion.shared.error.OrionException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(CurrentUser user) { HOLDER.set(Objects.requireNonNull(user)); }

    public static Optional<CurrentUser> current() { return Optional.ofNullable(HOLDER.get()); }

    public static CurrentUser requireUser() {
        CurrentUser u = HOLDER.get();
        if (u == null) throw OrionException.unauthenticated("No authenticated user in context");
        return u;
    }

    public static UUID requireTenantId() { return requireUser().tenantId(); }

    public static void clear() { HOLDER.remove(); }
}
