package io.orion.audit.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(UUID id, UUID actorId, String actorEmail, String action, String outcome,
                               String resourceType, String resourceId, Map<String, Object> details,
                               String ipAddress, String userAgent, String requestId, Instant createdAt) {
    public static AuditLogResponse from(io.orion.audit.domain.AuditLogEntry e) {
        return new AuditLogResponse(e.id(), e.actorId(), e.actorEmail(), e.action(), e.outcome().name(),
                e.resourceType(), e.resourceId(), e.details(), e.ipAddress(), e.userAgent(),
                e.requestId(), e.createdAt());
    }
}
