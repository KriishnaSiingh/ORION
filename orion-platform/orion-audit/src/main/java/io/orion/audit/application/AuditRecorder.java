package io.orion.audit.application;

import io.orion.audit.application.port.AuditLogRepository;
import io.orion.shared.audit.AuditEvent;
import io.orion.shared.audit.AuditPublisher;
import io.orion.shared.request.RequestMetadata;
import io.orion.shared.request.RequestContext;
import io.orion.audit.domain.AuditLogEntry;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuditRecorder implements AuditPublisher {

    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditRecorder(AuditLogRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }

    @Override
    public void publish(AuditEvent e) {
        RequestMetadata meta = RequestContext.current();
        repository.append(new AuditLogEntry(
                UUID.randomUUID(), e.tenantId(), e.actorId(), clip(e.actorEmail(), 320),
                clip(e.action(), 100), e.outcome(), clip(e.resourceType(), 100), clip(e.resourceId(), 200),
                e.details(), clip(meta.ipAddress(), 45), clip(meta.userAgent(), 500),
                clip(meta.requestId(), 64), clock.instant()));
    }

    private static String clip(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
