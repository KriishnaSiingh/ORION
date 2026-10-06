package io.orion.ontology.infrastructure.persistence;

import java.sql.*;
import java.time.*;

final class JdbcTime {
    private JdbcTime() {}
    static OffsetDateTime ts(java.time.Instant i) { return i == null ? null : i.atOffset(ZoneOffset.UTC); }
    static java.time.Instant instant(ResultSet rs, String col) throws SQLException {
        OffsetDateTime v = rs.getObject(col, OffsetDateTime.class);
        return v == null ? null : v.toInstant();
    }
}
