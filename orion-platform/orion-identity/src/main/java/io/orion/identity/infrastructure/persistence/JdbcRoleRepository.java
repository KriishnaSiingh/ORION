package io.orion.identity.infrastructure.persistence;

import io.orion.identity.domain.Role;
import io.orion.identity.application.port.RoleRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Repository
class JdbcRoleRepository implements RoleRepository {
    private final NamedParameterJdbcTemplate jdbc;
    JdbcRoleRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void saveAll(Collection<Role> roles) {
        for (Role r : roles) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", r.id()).addValue("tenantId", r.tenantId()).addValue("name", r.name())
                    .addValue("description", r.description()).addValue("system", r.system())
                    .addValue("createdAt", JdbcSupport.ts(r.createdAt()));
            jdbc.update("""
                INSERT INTO roles (id, tenant_id, name, description, is_system, created_at)
                VALUES (:id, :tenantId, :name, :description, :system, :createdAt)
                """, params);
        }
    }

    @Override public Set<String> findAllNames(UUID tenantId) {
        MapSqlParameterSource params = new MapSqlParameterSource("t", tenantId);
        return new HashSet<>(jdbc.queryForList("SELECT name FROM roles WHERE tenant_id = :t", params, String.class));
    }
}
