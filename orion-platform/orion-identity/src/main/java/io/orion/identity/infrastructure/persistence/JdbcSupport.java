package io.orion.identity.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class JdbcSupport {
    private JdbcSupport() {}
    static OffsetDateTime ts(Instant i) { return i == null ? null : i.atOffset(ZoneOffset.UTC); }
    static Instant instant(ResultSet rs, String col) throws SQLException {
        OffsetDateTime v = rs.getObject(col, OffsetDateTime.class);
        return v == null ? null : v.toInstant();
    }
}
