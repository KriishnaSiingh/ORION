package io.orion.audit.domain;

import io.orion.shared.audit.AuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogEntry(UUID id, UUID tenantId, UUID actorId, String actorEmail, String action,
                            AuditEvent.Outcome outcome, String resourceType, String resourceId,
                            Map<String, Object> details, String ipAddress, String userAgent,
                            String requestId, Instant createdAt) {}
