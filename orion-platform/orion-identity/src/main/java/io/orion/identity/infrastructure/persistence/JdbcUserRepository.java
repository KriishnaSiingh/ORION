package io.orion.identity.infrastructure.persistence;

import io.orion.identity.domain.User;
import io.orion.identity.application.port.UserRepository;
import io.orion.shared.paging.PageResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

@Repository
class JdbcUserRepository implements UserRepository {

    private static final String SELECT_USER = """
        SELECT u.id, u.tenant_id, u.email, u.name, u.password_hash, u.status, u.created_at, u.last_login_at,
               COALESCE(array_agg(r.name) FILTER (WHERE r.name IS NOT NULL), '{}') AS roles
        FROM users u
        LEFT JOIN user_roles ur ON ur.user_id = u.id AND ur.tenant_id = u.tenant_id
        LEFT JOIN roles r       ON r.id = ur.role_id AND r.tenant_id = ur.tenant_id
        """;

    private final NamedParameterJdbcTemplate jdbc;
    JdbcUserRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public User save(User u) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", u.id()).addValue("tenantId", u.tenantId()).addValue("email", u.email())
                    .addValue("name", u.name()).addValue("hash", u.passwordHash()).addValue("status", u.status().name())
                    .addValue("createdAt", JdbcSupport.ts(u.createdAt()));
            jdbc.update("""
                INSERT INTO users (id, tenant_id, email, name, password_hash, status, created_at, updated_at)
                VALUES (:id, :tenantId, :email, :name, :hash, :status, :createdAt, :createdAt)
                """, params);
        } catch (DuplicateKeyException e) {
            throw new io.orion.shared.error.OrionException(io.orion.shared.error.ErrorCode.CONFLICT, "A user with this email already exists in this tenant");
        }
        if (!u.roles().isEmpty()) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", u.id()).addValue("tenantId", u.tenantId()).addValue("names", u.roles());
            jdbc.update("""
                INSERT INTO user_roles (user_id, role_id, tenant_id)
                SELECT :userId, r.id, :tenantId FROM roles r
                WHERE r.tenant_id = :tenantId AND r.name IN (:names)
                """, params);
        }
        return u;
    }

    @Override public Optional<User> findById(UUID tenantId, UUID userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("t", tenantId).addValue("id", userId);
        return jdbc.query(SELECT_USER + " WHERE u.tenant_id = :t AND u.id = :id GROUP BY u.id",
                params, JdbcUserRepository::map).stream().findFirst();
    }

    @Override public Optional<User> findByEmail(UUID tenantId, String email) {
        MapSqlParameterSource params = new MapSqlParameterSource("t", tenantId).addValue("email", email);
        return jdbc.query(SELECT_USER + " WHERE u.tenant_id = :t AND u.email = :email GROUP BY u.id",
                params, JdbcUserRepository::map).stream().findFirst();
    }

    @Override public PageResult<User> findAll(UUID tenantId, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource("t", tenantId);
        long total = jdbc.queryForObject("SELECT count(*) FROM users WHERE tenant_id = :t", params, Long.class);

        MapSqlParameterSource listParams = new MapSqlParameterSource("t", tenantId)
                .addValue("size", size).addValue("offset", (long) page * size);
        List<User> items = jdbc.query(SELECT_USER + """
                 WHERE u.tenant_id = :t GROUP BY u.id
                 ORDER BY u.created_at DESC, u.id LIMIT :size OFFSET :offset
                """, listParams, JdbcUserRepository::map);
        return new PageResult<>(items, page, size, total);
    }

    @Override public void recordLogin(UUID tenantId, UUID userId, Instant at) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("at", JdbcSupport.ts(at)).addValue("t", tenantId).addValue("id", userId);
        jdbc.update("UPDATE users SET last_login_at = :at, updated_at = :at WHERE tenant_id = :t AND id = :id", params);
    }

    private static User map(ResultSet rs, int row) throws SQLException {
        java.sql.Array rolesArray = rs.getArray("roles");
        Set<String> roles = rolesArray == null ? Set.of() : Set.of((String[]) rolesArray.getArray());
        return new User(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                rs.getString("email"), rs.getString("name"), rs.getString("password_hash"),
                User.Status.valueOf(rs.getString("status")), roles,
                JdbcSupport.instant(rs, "created_at"), JdbcSupport.instant(rs, "last_login_at"));
    }
}
