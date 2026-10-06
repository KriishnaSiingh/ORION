package io.orion.ontology.api.dto;

import io.orion.ontology.domain.PropertyDefinition;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PropertyResponse(UUID id, String apiName, String displayName, String description, String dataType,
                               boolean required, boolean unique, boolean searchable, boolean filterable,
                               boolean multiValued, List<String> enumValues, String status, long revision,
                               Instant createdAt, Instant updatedAt) {
    public static PropertyResponse from(PropertyDefinition p) {
        return new PropertyResponse(p.id(), p.apiName(), p.displayName(), p.description(), p.dataType().name(),
                p.required(), p.unique(), p.searchable(), p.filterable(), p.multiValued(), p.enumValues(),
                p.status().name(), p.revision(), p.createdAt(), p.updatedAt());
    }
}
