package io.orion.ontology.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.orion.ontology.application.port.OntologyLedger;
import io.orion.ontology.domain.OntologyChange;
import io.orion.shared.paging.PageResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

@Repository
class JdbcOntologyLedger implements OntologyLedger {

    private final JdbcClient jdbc;
    private final ObjectMapper mapper;

    JdbcOntologyLedger(JdbcClient jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    @Override public long nextVersion(UUID tenantId) {
        return jdbc.sql("""
                INSERT INTO ontology_versions (tenant_id, current_version) VALUES (:t, 1)
                ON CONFLICT (tenant_id) DO UPDATE
                   SET current_version = ontology_versions.current_version + 1, updated_at = now()
                RETURNING current_version
                """).param("t", tenantId).query(Long.class).single();
    }

    @Override public long currentVersion(UUID tenantId) {
        return jdbc.sql("SELECT current_version FROM ontology_versions WHERE tenant_id = :t")
                .param("t", tenantId).query(Long.class).optional().orElse(0L);
    }

    @Override public void recordChange(OntologyChange c) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", c.id()); p.put("tenantId", c.tenantId()); p.put("version", c.ontologyVersion());
        p.put("entityType", c.entityType().name()); p.put("entityId", c.entityId());
        p.put("changeType", c.changeType().name()); p.put("before", toJson(c.before())); p.put("after", toJson(c.after()));
        p.put("changedBy", c.changedBy()); p.put("changedAt", JdbcTime.ts(c.changedAt()));
        jdbc.sql("""
            INSERT INTO ontology_changes (id, tenant_id, ontology_version, entity_type, entity_id, change_type,
                snapshot_before, snapshot_after, changed_by, changed_at)
            VALUES (:id, :tenantId, :version, :entityType, :entityId, :changeType,
                CAST(:before AS jsonb), CAST(:after AS jsonb), :changedBy, :changedAt)
            """).params(p).update();
    }

    @Override public PageResult<OntologyChange> findChanges(UUID tenantId, int page, int size) {
        long total = jdbc.sql("SELECT count(*) FROM ontology_changes WHERE tenant_id = :t")
                .param("t", tenantId).query(Long.class).single();
        List<OntologyChange> items = jdbc.sql("""
                SELECT * FROM ontology_changes WHERE tenant_id = :t
                ORDER BY ontology_version DESC LIMIT :size OFFSET :offset""")
                .param("t", tenantId).param("size", size).param("offset", (long) page * size)
                .query(this::map).list();
        return new PageResult<>(items, page, size, total);
    }

    private OntologyChange map(ResultSet rs, int row) throws SQLException {
        return new OntologyChange(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                rs.getLong("ontology_version"), io.orion.ontology.domain.OntologyChange.EntityType.valueOf(rs.getString("entity_type")),
                rs.getObject("entity_id", UUID.class), io.orion.ontology.domain.OntologyChange.ChangeType.valueOf(rs.getString("change_type")),
                fromJson(rs.getString("snapshot_before")), fromJson(rs.getString("snapshot_after")),
                rs.getObject("changed_by", UUID.class), JdbcTime.instant(rs, "changed_at"));
    }

    private String toJson(Object o) {
        if (o == null) return null;
        try { return mapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Snapshot not serializable", e); }
    }

    private Object fromJson(String json) {
        if (json == null) return null;
        try { return mapper.readValue(json, Object.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Corrupt snapshot", e); }
    }
}
