package io.orion.shared.audit;

/** Cross-cutting actions only. Module-specific actions live in their own module. */
public final class AuditActions {
    private AuditActions() {}
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
}
