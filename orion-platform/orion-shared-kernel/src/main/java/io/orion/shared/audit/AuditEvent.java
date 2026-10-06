package io.orion.shared.audit;

import io.orion.shared.tenant.TenantContext;
import java.util.*;

public record AuditEvent(UUID tenantId, UUID actorId, String actorEmail, String action, Outcome outcome,
                         String resourceType, String resourceId, Map<String, Object> details) {

    public enum Outcome { SUCCESS, FAILURE, DENIED }

    public AuditEvent {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(outcome, "outcome");
        details = details == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public static Builder of(String action) { return new Builder(action); }

    public static final class Builder {
        private final String action;
        private Outcome outcome = Outcome.SUCCESS;
        private UUID tenantId, actorId;
        private String actorEmail, resourceType, resourceId;
        private final Map<String, Object> details = new LinkedHashMap<>();

        private Builder(String action) { this.action = action; }

        public Builder outcome(Outcome outcome) { this.outcome = outcome; return this; }
        public Builder tenant(UUID tenantId) { this.tenantId = tenantId; return this; }
        public Builder actor(UUID actorId, String actorEmail) {
            this.actorId = actorId; this.actorEmail = actorEmail; return this;
        }
        /** Opt-in: fills tenant and actor from the authenticated request, if there is one. */
        public Builder actorFromContext() {
            TenantContext.current().ifPresent(u -> {
                this.tenantId = u.tenantId(); this.actorId = u.userId(); this.actorEmail = u.email();
            });
            return this;
        }
        public Builder resource(String type, Object id) {
            this.resourceType = type; this.resourceId = id == null ? null : id.toString(); return this;
        }
        public Builder detail(String key, Object value) { this.details.put(key, value); return this; }

        public AuditEvent build() {
            return new AuditEvent(tenantId, actorId, actorEmail, action, outcome, resourceType, resourceId, details);
        }
    }
}
