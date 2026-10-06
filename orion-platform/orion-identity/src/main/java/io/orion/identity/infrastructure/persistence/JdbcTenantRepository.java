package io.orion.identity.infrastructure.persistence;

import io.orion.identity.domain.Tenant;
import io.orion.identity.application.port.TenantRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcTenantRepository implements TenantRepository {
    private final NamedParameterJdbcTemplate jdbc;
    JdbcTenantRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public Tenant save(Tenant t) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", t.id()).addValue("name", t.name()).addValue("slug", t.slug())
                    .addValue("status", t.status().name()).addValue("createdAt", JdbcSupport.ts(t.createdAt()));
            jdbc.update("""
                INSERT INTO tenants (id, name, slug, status, created_at, updated_at)
                VALUES (:id, :name, :slug, :status, :createdAt, :createdAt)
                """, params);
        } catch (DuplicateKeyException e) {
            throw new io.orion.shared.error.OrionException(io.orion.shared.error.ErrorCode.CONFLICT, "Tenant slug '" + t.slug() + "' is already taken");
        }
        return t;
    }

    @Override public Optional<Tenant> findById(UUID id) {
        return jdbc.query("SELECT * FROM tenants WHERE id = :id",
                new MapSqlParameterSource("id", id), JdbcTenantRepository::map).stream().findFirst();
    }

    @Override public Optional<Tenant> findBySlug(String slug) {
        return jdbc.query("SELECT * FROM tenants WHERE slug = :slug",
                new MapSqlParameterSource("slug", slug), JdbcTenantRepository::map).stream().findFirst();
    }

    private static Tenant map(ResultSet rs, int row) throws SQLException {
        return new Tenant(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"),
                Tenant.Status.valueOf(rs.getString("status")), JdbcSupport.instant(rs, "created_at"));
    }
}
