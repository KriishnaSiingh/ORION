package io.orion.audit.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.orion.audit.application.port.AuditLogRepository;
import io.orion.audit.domain.AuditLogEntry;
import io.orion.audit.domain.AuditLogFilter;
import io.orion.shared.paging.PageResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import io.orion.shared.audit.AuditEvent;
import java.util.*;

@Repository
class JdbcAuditLogRepository implements AuditLogRepository {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final String COLUMNS = """
        id, tenant_id, actor_id, actor_email, action, outcome, resource_type, resource_id,
        details, ip_address, user_agent, request_id, created_at""";

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;

    JdbcAuditLogRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    @Override public void append(AuditLogEntry e) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", e.id()).addValue("tenantId", e.tenantId()).addValue("actorId", e.actorId())
                .addValue("actorEmail", e.actorEmail()).addValue("action", e.action())
                .addValue("outcome", e.outcome().name()).addValue("resourceType", e.resourceType())
                .addValue("resourceId", e.resourceId()).addValue("details", toJson(e.details()))
                .addValue("ip", e.ipAddress()).addValue("ua", e.userAgent()).addValue("requestId", e.requestId())
                .addValue("createdAt", e.createdAt().atOffset(ZoneOffset.UTC));

        jdbc.update("INSERT INTO audit_logs (" + COLUMNS + ") VALUES (:id, :tenantId, :actorId, :actorEmail, :action, :outcome, :resourceType, :resourceId, CAST(:details AS jsonb), :ip, :ua, :requestId, :createdAt)", params);
    }

    @Override public PageResult<AuditLogEntry> search(UUID tenantId, AuditLogFilter f, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE tenant_id = :tenantId");
        Map<String, Object> p = new HashMap<>();
        p.put("tenantId", tenantId);
        if (f.action() != null)       { where.append(" AND action = :action");               p.put("action", f.action()); }
        if (f.actorId() != null)      { where.append(" AND actor_id = :actorId");            p.put("actorId", f.actorId()); }
        if (f.resourceType() != null) { where.append(" AND resource_type = :resourceType");  p.put("resourceType", f.resourceType()); }
        if (f.resourceId() != null)   { where.append(" AND resource_id = :resourceId");      p.put("resourceId", f.resourceId()); }
        if (f.outcome() != null)      { where.append(" AND outcome = :outcome");             p.put("outcome", f.outcome().name()); }
        if (f.from() != null)         { where.append(" AND created_at >= :from");            p.put("from", f.from().atOffset(ZoneOffset.UTC)); }
        if (f.to() != null)           { where.append(" AND created_at < :to");               p.put("to", f.to().atOffset(ZoneOffset.UTC)); }

        long total = jdbc.queryForObject("SELECT count(*) FROM audit_logs" + where, p, Long.class);

        MapSqlParameterSource pageParams = new MapSqlParameterSource();
        p.forEach(pageParams::addValue);
        pageParams.addValue("size", size);
        pageParams.addValue("offset", (long) page * size);

        List<AuditLogEntry> items = jdbc.query("SELECT " + COLUMNS + " FROM audit_logs" + where
                        + " ORDER BY created_at DESC, id DESC LIMIT :size OFFSET :offset",
                pageParams, this::map);
        return new PageResult<>(items, page, size, total);
    }

    private AuditLogEntry map(ResultSet rs, int row) throws SQLException {
        return new AuditLogEntry(
                rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                rs.getObject("actor_id", UUID.class), rs.getString("actor_email"), rs.getString("action"),
                AuditEvent.Outcome.valueOf(rs.getString("outcome")),
                rs.getString("resource_type"), rs.getString("resource_id"),
                fromJson(rs.getString("details")), rs.getString("ip_address"), rs.getString("user_agent"),
                rs.getString("request_id"), rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }

    private String toJson(Map<String, Object> details) {
        try { return mapper.writeValueAsString(details); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Audit details not serializable", e); }
    }

    private Map<String, Object> fromJson(String json) {
        try { return mapper.readValue(json, MAP_TYPE); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Corrupt audit details", e); }
    }
}
