package io.orion.audit.api;

import io.orion.audit.application.AuditQueryService;
import io.orion.audit.api.dto.AuditLogResponse;
import io.orion.audit.domain.AuditLogFilter;
import io.orion.shared.paging.PageResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogController {
    private final AuditQueryService service;
    public AuditLogController(AuditQueryService service) { this.service = service; }

    @GetMapping(value = "/logs", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public PageResult<AuditLogResponse> search(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) io.orion.shared.audit.AuditEvent.Outcome outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuditLogFilter filter = new AuditLogFilter(action, actorId, resourceType, resourceId, outcome, from, to);
        return service.search(filter, page, size).map(AuditLogResponse::from);
    }
}
