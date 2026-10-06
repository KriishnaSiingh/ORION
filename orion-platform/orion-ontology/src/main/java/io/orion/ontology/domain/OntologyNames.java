package io.orion.ontology.domain;

import io.orion.shared.error.OrionException;
import java.util.*;
import java.util.regex.Pattern;

public final class OntologyNames {
    private OntologyNames() {}

    // Also used by DTO validation, so the rule lives in exactly one place.
    public static final String OBJECT_TYPE_REGEX = "^[A-Z][A-Za-z0-9]{1,62}$";   // Person, BankAccount
    public static final String PROPERTY_REGEX    = "^[a-z][A-Za-z0-9]{1,62}$";   // dateOfBirth
    public static final String LINK_TYPE_REGEX   = "^[A-Z][A-Z0-9_]{1,62}$";     // OWNS, WORKS_AT
    public static final String COLOR_REGEX       = "^#[0-9A-Fa-f]{6}$";

    private static final Pattern OBJECT_TYPE = Pattern.compile(OBJECT_TYPE_REGEX);
    private static final Pattern PROPERTY    = Pattern.compile(PROPERTY_REGEX);
    private static final Pattern LINK_TYPE   = Pattern.compile(LINK_TYPE_REGEX);
    private static final Pattern COLOR       = Pattern.compile(COLOR_REGEX);

    /** System fields the graph layer will write on every object. */
    private static final Set<String> RESERVED_PROPERTY_NAMES =
            Set.of("id", "tenantId", "version", "createdAt", "updatedAt", "deletedAt");

    public static String requireObjectTypeName(String v) { return require(OBJECT_TYPE, v, "Object type apiName", "PascalCase, e.g. BankAccount"); }
    public static String requireLinkTypeName(String v)   { return require(LINK_TYPE, v, "Link type apiName", "UPPER_SNAKE_CASE, e.g. WORKS_AT"); }
    public static String requirePropertyName(String v) {
        require(PROPERTY, v, "Property apiName", "camelCase, e.g. dateOfBirth");
        if (RESERVED_PROPERTY_NAMES.contains(v)) {
            throw OrionException.validation("Property apiName '" + v + "' is reserved");
        }
        return v;
    }

    public static String requireDisplayName(String v) {
        if (v == null || v.isBlank()) throw OrionException.validation("displayName must not be blank");
        String t = v.trim();
        if (t.length() > 100) throw OrionException.validation("displayName must be at most 100 characters");
        return t;
    }

    public static String requireColor(String v) {
        if (v == null || v.isBlank()) return null;
        if (!COLOR.matcher(v).matches()) throw OrionException.validation("color must look like #3366FF");
        return v;
    }

    public static String blankToNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }

    private static String require(Pattern p, String v, String what, String hint) {
        if (v == null || !p.matcher(v).matches()) {
            throw OrionException.validation(what + " must be " + hint + " (2-63 chars)");
        }
        return v;
    }
}
