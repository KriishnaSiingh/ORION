package io.orion.audit.application;

import io.orion.audit.application.port.AuditLogRepository;
import io.orion.audit.domain.AuditLogEntry;
import io.orion.audit.domain.AuditLogFilter;
import io.orion.shared.audit.AuditEvent;
import io.orion.shared.audit.AuditPublisher;
import io.orion.shared.paging.PageResult;
import io.orion.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository repository;
    private final AuditPublisher audit;

    public AuditQueryService(AuditLogRepository repository, AuditPublisher audit) {
        this.repository = repository; this.audit = audit;
    }

    public PageResult<AuditLogEntry> search(AuditLogFilter filter, int page, int size) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new io.orion.shared.error.OrionException(io.orion.shared.error.ErrorCode.VALIDATION_FAILED, "'from' must not be after 'to'");
        }
        io.orion.shared.tenant.CurrentUser me = TenantContext.requireUser();
        PageResult<AuditLogEntry> result = repository.search(me.tenantId(), filter,
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        audit.publish(AuditEvent.of("AUDIT_LOG_VIEWED").actorFromContext()
                .resource("AuditLog", null).detail("filter", filter.describe()).build());
        return result;
    }
}
