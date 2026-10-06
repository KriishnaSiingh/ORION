package io.orion.ontology.domain;

import io.orion.shared.error.OrionException;
import java.time.Instant;
import java.util.*;

public record PropertyDefinition(UUID id, String apiName, String displayName, String description,
                                 DataType dataType, boolean required, boolean unique, boolean searchable,
                                 boolean filterable, boolean multiValued, List<String> enumValues,
                                 EntityStatus status, long revision, Instant createdAt, Instant updatedAt) {

    private static final int MAX_ENUM_VALUES = 100;
    private static final Set<DataType> UNIQUE_CAPABLE =
            EnumSet.of(DataType.STRING, DataType.INTEGER, DataType.DATE, DataType.DATETIME);

    public PropertyDefinition {
        enumValues = enumValues == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(enumValues));
    }

    public static PropertyDefinition create(String apiName, String displayName, String description, DataType dataType,
                                            boolean required, boolean unique, boolean searchable, boolean filterable,
                                            boolean multiValued, List<String> enumValues, Instant now) {
        Objects.requireNonNull(dataType, "dataType");
        PropertyDefinition p = new PropertyDefinition(UUID.randomUUID(), OntologyNames.requirePropertyName(apiName),
                OntologyNames.requireDisplayName(displayName), OntologyNames.blankToNull(description), dataType,
                required, unique, searchable, filterable, multiValued, enumValues,
                EntityStatus.ACTIVE, 1, now, now);
        p.validateShape();
        return p;
    }

    /** apiName, dataType and multiValued are immutable. Constraints can be loosened, never tightened. */
    public PropertyDefinition update(String displayName, String description, boolean required, boolean unique,
                                     boolean searchable, boolean filterable, List<String> newEnumValues, Instant now) {
        if (required && !this.required) throw OrionException.validation("A property cannot be made required after creation");
        if (unique && !this.unique)     throw OrionException.validation("A property cannot be made unique after creation");
        List<String> values = newEnumValues == null ? List.of() : newEnumValues;
        if (dataType == DataType.ENUM && !values.containsAll(enumValues)) {
            throw OrionException.validation("Enum values can only be added, never removed");
        }
        PropertyDefinition updated = new PropertyDefinition(id, apiName, OntologyNames.requireDisplayName(displayName),
                OntologyNames.blankToNull(description), dataType, required, unique, searchable, filterable,
                multiValued, values, status, revision + 1, createdAt, now);
        updated.validateShape();
        return updated;
    }

    public PropertyDefinition archive(Instant now) {
        return new PropertyDefinition(id, apiName, displayName, description, dataType, required, unique, searchable,
                filterable, multiValued, enumValues, EntityStatus.ARCHIVED, revision + 1, createdAt, now);
    }

    public boolean isActive() { return status == EntityStatus.ACTIVE; }

    private void validateShape() {
        if (dataType == DataType.ENUM) {
            if (enumValues.isEmpty() || enumValues.size() > MAX_ENUM_VALUES) {
                throw OrionException.validation("ENUM properties need between 1 and " + MAX_ENUM_VALUES + " enumValues");
            }
            if (enumValues.stream().anyMatch(v -> v == null || v.isBlank() || v.length() > 100)) {
                throw OrionException.validation("Each enum value must be non-blank and at most 100 characters");
            }
            if (new HashSet<>(enumValues).size() != enumValues.size()) {
                throw OrionException.validation("Enum values must be distinct");
            }
        } else if (!enumValues.isEmpty()) {
            throw OrionException.validation("enumValues is only allowed for ENUM properties");
        }
        if (unique && (multiValued || !UNIQUE_CAPABLE.contains(dataType))) {
            throw OrionException.validation("unique is only supported on single-valued STRING, INTEGER, DATE and DATETIME properties");
        }
        if (searchable && dataType != DataType.STRING) {
            throw OrionException.validation("searchable is only supported on STRING properties");
        }
    }
}
