package io.orion.audit.domain;

import io.orion.shared.audit.AuditEvent;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record AuditLogFilter(String action, UUID actorId, String resourceType, String resourceId,
                             AuditEvent.Outcome outcome, Instant from, Instant to) {

    public Map<String, Object> describe() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (action != null)       m.put("action", action);
        if (actorId != null)      m.put("actorId", actorId.toString());
        if (resourceType != null) m.put("resourceType", resourceType);
        if (resourceId != null)   m.put("resourceId", resourceId);
        if (outcome != null)      m.put("outcome", outcome.name());
        if (from != null)         m.put("from", from.toString());
        if (to != null)           m.put("to", to.toString());
        return m;
    }
}
