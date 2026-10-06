package io.orion.ontology.infrastructure.persistence;

import io.orion.ontology.application.port.LinkTypeRepository;
import io.orion.ontology.domain.EntityStatus;
import io.orion.ontology.domain.LinkType;
import io.orion.ontology.domain.Cardinality;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

@Repository
class JdbcLinkTypeRepository implements LinkTypeRepository {

    private final JdbcClient jdbc;
    JdbcLinkTypeRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override public void insert(LinkType l) {
        try {
            jdbc.sql("""
                INSERT INTO link_types (id, tenant_id, api_name, display_name, description, source_object_type_id,
                    target_object_type_id, cardinality, is_bidirectional, status, revision,
                    created_at, updated_at, created_by, updated_by)
                VALUES (:id, :tenantId, :apiName, :displayName, :description, :source, :target, :cardinality,
                    :bidirectional, :status, :revision, :createdAt, :updatedAt, :createdBy, :updatedBy)
                """).params(params(l)).update();
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw io.orion.shared.error.OrionException.conflict("Link type '" + l.apiName() + "' already exists (archived link types keep their name)");
        }
    }

    @Override public boolean update(LinkType l, long expectedRevision) {
        Map<String, Object> p = params(l);
        p.put("expected", expectedRevision);
        return jdbc.sql("""
            UPDATE link_types
               SET display_name = :displayName, description = :description, cardinality = :cardinality,
                   is_bidirectional = :bidirectional, status = :status, revision = :revision,
                   updated_at = :updatedAt, updated_by = :updatedBy
             WHERE tenant_id = :tenantId AND id = :id AND revision = :expected
            """).params(p).update() == 1;
    }

    @Override public Optional<LinkType> findById(UUID tenantId, UUID id) {
        return jdbc.sql("SELECT * FROM link_types WHERE tenant_id = :t AND id = :id")
                .param("t", tenantId).param("id", id).query(JdbcLinkTypeRepository::map).optional();
    }

    @Override public List<LinkType> findAll(UUID tenantId, boolean includeArchived) {
        return jdbc.sql("SELECT * FROM link_types WHERE tenant_id = :t"
                        + (includeArchived ? "" : " AND status = 'ACTIVE'") + " ORDER BY api_name")
                .param("t", tenantId).query(JdbcLinkTypeRepository::map).list();
    }

    @Override public List<LinkType> findActiveReferencing(UUID tenantId, UUID objectTypeId) {
        return jdbc.sql("""
                SELECT * FROM link_types
                 WHERE tenant_id = :t AND status = 'ACTIVE'
                   AND (source_object_type_id = :id OR target_object_type_id = :id)
                 ORDER BY api_name""")
                .param("t", tenantId).param("id", objectTypeId).query(JdbcLinkTypeRepository::map).list();
    }

    private static Map<String, Object> params(LinkType l) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", l.id()); p.put("tenantId", l.tenantId()); p.put("apiName", l.apiName());
        p.put("displayName", l.displayName()); p.put("description", l.description());
        p.put("source", l.sourceObjectTypeId()); p.put("target", l.targetObjectTypeId());
        p.put("cardinality", l.cardinality().name()); p.put("bidirectional", l.bidirectional());
        p.put("status", l.status().name()); p.put("revision", l.revision());
        p.put("createdAt", JdbcTime.ts(l.createdAt())); p.put("updatedAt", JdbcTime.ts(l.updatedAt()));
        p.put("createdBy", l.createdBy()); p.put("updatedBy", l.updatedBy());
        return p;
    }

    private static LinkType map(ResultSet rs, int row) throws SQLException {
        return new LinkType(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                rs.getString("api_name"), rs.getString("display_name"), rs.getString("description"),
                rs.getObject("source_object_type_id", UUID.class), rs.getObject("target_object_type_id", UUID.class),
                Cardinality.valueOf(rs.getString("cardinality")), rs.getBoolean("is_bidirectional"),
                EntityStatus.valueOf(rs.getString("status")), rs.getLong("revision"),
                JdbcTime.instant(rs, "created_at"), JdbcTime.instant(rs, "updated_at"),
                rs.getObject("created_by", UUID.class), rs.getObject("updated_by", UUID.class));
    }
}
