package io.orion.audit.application.port;

import io.orion.audit.domain.AuditLogEntry;
import io.orion.audit.domain.AuditLogFilter;
import io.orion.shared.paging.PageResult;
import java.util.UUID;

public interface AuditLogRepository {
    void append(AuditLogEntry entry);
    PageResult<AuditLogEntry> search(UUID tenantId, AuditLogFilter filter, int page, int size);
}
