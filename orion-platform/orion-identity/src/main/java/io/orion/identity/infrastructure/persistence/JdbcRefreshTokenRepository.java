package io.orion.identity.infrastructure.persistence;

import io.orion.identity.domain.RefreshToken;
import io.orion.identity.application.port.RefreshTokenRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcRefreshTokenRepository implements RefreshTokenRepository {
    private final NamedParameterJdbcTemplate jdbc;
    JdbcRefreshTokenRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void save(RefreshToken t) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", t.id()).addValue("tenantId", t.tenantId()).addValue("userId", t.userId())
                .addValue("hash", t.tokenHash()).addValue("expiresAt", JdbcSupport.ts(t.expiresAt()))
                .addValue("createdAt", JdbcSupport.ts(t.createdAt()));
        jdbc.update("""
            INSERT INTO refresh_tokens (id, tenant_id, user_id, token_hash, expires_at, created_at)
            VALUES (:id, :tenantId, :userId, :hash, :expiresAt, :createdAt)
            """, params);
    }

    @Override public Optional<RefreshToken> findByHash(String hash) {
        MapSqlParameterSource params = new MapSqlParameterSource("h", hash);
        return jdbc.query("SELECT * FROM refresh_tokens WHERE token_hash = :h", params, (rs, row) -> new RefreshToken(
                        rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                        rs.getObject("user_id", UUID.class), rs.getString("token_hash"),
                        JdbcSupport.instant(rs, "expires_at"), JdbcSupport.instant(rs, "revoked_at"),
                        JdbcSupport.instant(rs, "created_at")))
                .stream().findFirst();
    }

    @Override public boolean revoke(UUID tokenId, Instant at) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("at", JdbcSupport.ts(at)).addValue("id", tokenId);
        return jdbc.update("UPDATE refresh_tokens SET revoked_at = :at WHERE id = :id AND revoked_at IS NULL", params) == 1;
    }

    @Override public void revokeAllForUser(UUID tenantId, UUID userId, Instant at) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("at", JdbcSupport.ts(at)).addValue("t", tenantId).addValue("u", userId);
        jdbc.update("""
            UPDATE refresh_tokens SET revoked_at = :at
            WHERE tenant_id = :t AND user_id = :u AND revoked_at IS NULL
            """, params);
    }
}
