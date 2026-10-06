package io.orion.ontology.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.orion.ontology.application.port.ObjectTypeRepository;
import io.orion.ontology.domain.EntityStatus;
import io.orion.ontology.domain.ObjectType;
import io.orion.ontology.domain.PropertyDefinition;
import io.orion.shared.error.OrionException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

@Repository
class JdbcObjectTypeRepository implements ObjectTypeRepository {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final JdbcClient jdbc;
    private final ObjectMapper mapper;

    JdbcObjectTypeRepository(JdbcClient jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    @Override public void insert(ObjectType t) {
        try {
            jdbc.sql("""
                INSERT INTO object_types (id, tenant_id, api_name, display_name, description, icon, color, status,
                                          revision, created_at, updated_at, created_by, updated_by)
                VALUES (:id, :tenantId, :apiName, :displayName, :description, :icon, :color, :status,
                        :revision, :createdAt, :updatedAt, :createdBy, :updatedBy)
                """).params(typeParams(t)).update();
        } catch (DuplicateKeyException e) {
            throw OrionException.conflict("Object type '" + t.apiName() + "' already exists (archived types keep their name)");
        }
    }

    @Override public boolean update(ObjectType t, long expectedRevision) {
        Map<String, Object> p = typeParams(t);
        p.put("expected", expectedRevision);
        return jdbc.sql("""
            UPDATE object_types
               SET display_name = :displayName, description = :description, icon = :icon, color = :color,
                   status = :status, revision = :revision, updated_at = :updatedAt, updated_by = :updatedBy
             WHERE tenant_id = :tenantId AND id = :id AND revision = :expected
            """).params(p).update() == 1;
    }

    @Override public Optional<ObjectType> findById(UUID tenantId, UUID id) {
        return jdbc.sql("SELECT * FROM object_types WHERE tenant_id = :t AND id = :id")
                .param("t", tenantId).param("id", id).query(JdbcObjectTypeRepository::mapType).optional()
                .map(t -> t.withProperties(jdbc.sql("""
                        SELECT * FROM properties WHERE tenant_id = :t AND object_type_id = :id
                        ORDER BY created_at, id""")
                        .param("t", tenantId).param("id", id).query(this::mapProperty).list()));
    }

    @Override public List<ObjectType> findAll(UUID tenantId, boolean includeArchived) {
        List<ObjectType> types = jdbc.sql("SELECT * FROM object_types WHERE tenant_id = :t"
                        + (includeArchived ? "" : " AND status = 'ACTIVE'") + " ORDER BY api_name")
                .param("t", tenantId).query(JdbcObjectTypeRepository::mapType).list();

        Map<UUID, List<PropertyDefinition>> byType = new HashMap<>();
        jdbc.sql("SELECT * FROM properties WHERE tenant_id = :t"
                        + (includeArchived ? "" : " AND status = 'ACTIVE'") + " ORDER BY created_at, id")
                .param("t", tenantId)
                .query((rs, i) -> Map.entry(rs.getObject("object_type_id", UUID.class), mapProperty(rs, i)))
                .list()
                .forEach(e -> byType.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue()));

        return types.stream().map(t -> t.withProperties(byType.getOrDefault(t.id(), List.of()))).toList();
    }

    @Override public void insertProperty(UUID tenantId, UUID objectTypeId, PropertyDefinition p) {
        try {
            jdbc.sql("""
                INSERT INTO properties (id, tenant_id, object_type_id, api_name, display_name, description, data_type,
                    is_required, is_unique, is_searchable, is_filterable, is_multi_valued, config, status, revision,
                    created_at, updated_at)
                VALUES (:id, :tenantId, :objectTypeId, :apiName, :displayName, :description, :dataType,
                    :required, :unique, :searchable, :filterable, :multiValued, CAST(:config AS jsonb), :status, :revision,
                    :createdAt, :updatedAt)
                """).params(propertyParams(tenantId, objectTypeId, p)).update();
        } catch (DuplicateKeyException e) {
            throw OrionException.conflict("Property '" + p.apiName() + "' already exists on this object type (archived properties keep their name)");
        }
    }

    @Override public boolean updateProperty(UUID tenantId, UUID objectTypeId, PropertyDefinition p, long expectedRevision) {
        Map<String, Object> params = propertyParams(tenantId, objectTypeId, p);
        params.put("expected", expectedRevision);
        return jdbc.sql("""
            UPDATE properties
               SET display_name = :displayName, description = :description, is_required = :required,
                   is_unique = :unique, is_searchable = :searchable, is_filterable = :filterable,
                   config = CAST(:config AS jsonb), status = :status, revision = :revision, updated_at = :updatedAt
             WHERE tenant_id = :tenantId AND object_type_id = :objectTypeId AND id = :id AND revision = :expected
            """).params(params).update() == 1;
    }

    private static Map<String, Object> typeParams(ObjectType t) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", t.id()); p.put("tenantId", t.tenantId()); p.put("apiName", t.apiName());
        p.put("displayName", t.displayName()); p.put("description", t.description());
        p.put("icon", t.icon()); p.put("color", t.color()); p.put("status", t.status().name());
        p.put("revision", t.revision()); p.put("createdAt", JdbcTime.ts(t.createdAt()));
        p.put("updatedAt", JdbcTime.ts(t.updatedAt())); p.put("createdBy", t.createdBy()); p.put("updatedBy", t.updatedBy());
        return p;
    }

    private Map<String, Object> propertyParams(UUID tenantId, UUID objectTypeId, PropertyDefinition d) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", d.id()); p.put("tenantId", tenantId); p.put("objectTypeId", objectTypeId);
        p.put("apiName", d.apiName()); p.put("displayName", d.displayName()); p.put("description", d.description());
        p.put("dataType", d.dataType().name()); p.put("required", d.required()); p.put("unique", d.unique());
        p.put("searchable", d.searchable()); p.put("filterable", d.filterable()); p.put("multiValued", d.multiValued());
        p.put("config", config(d)); p.put("status", d.status().name()); p.put("revision", d.revision());
        p.put("createdAt", JdbcTime.ts(d.createdAt())); p.put("updatedAt", JdbcTime.ts(d.updatedAt()));
        return p;
    }

    private static ObjectType mapType(ResultSet rs, int row) throws SQLException {
        return new ObjectType(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                rs.getString("api_name"), rs.getString("display_name"), rs.getString("description"),
                rs.getString("icon"), rs.getString("color"), EntityStatus.valueOf(rs.getString("status")),
                rs.getLong("revision"), JdbcTime.instant(rs, "created_at"), JdbcTime.instant(rs, "updated_at"),
                rs.getObject("created_by", UUID.class), rs.getObject("updated_by", UUID.class), List.of());
    }

    private PropertyDefinition mapProperty(ResultSet rs, int row) throws SQLException {
        return new PropertyDefinition(rs.getObject("id", UUID.class), rs.getString("api_name"),
                rs.getString("display_name"), rs.getString("description"), io.orion.ontology.domain.DataType.valueOf(rs.getString("data_type")),
                rs.getBoolean("is_required"), rs.getBoolean("is_unique"), rs.getBoolean("is_searchable"),
                rs.getBoolean("is_filterable"), rs.getBoolean("is_multi_valued"), enumValues(rs.getString("config")),
                EntityStatus.valueOf(rs.getString("status")), rs.getLong("revision"),
                JdbcTime.instant(rs, "created_at"), JdbcTime.instant(rs, "updated_at"));
    }

    private String config(PropertyDefinition p) {
        Map<String, Object> cfg = new HashMap<>();
        if (!p.enumValues().isEmpty()) cfg.put("enumValues", p.enumValues());
        try { return mapper.writeValueAsString(cfg); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    private List<String> enumValues(String json) {
        try {
            Object v = mapper.readValue(json, MAP).get("enumValues");
            return v instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of();
        } catch (JsonProcessingException e) { throw new IllegalStateException("Corrupt property config", e); }
    }
}
