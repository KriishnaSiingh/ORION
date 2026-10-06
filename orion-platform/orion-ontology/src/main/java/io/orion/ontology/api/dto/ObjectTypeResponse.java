package io.orion.ontology.api.dto;

import io.orion.ontology.domain.ObjectType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ObjectTypeResponse(UUID id, String apiName, String displayName, String description, String icon,
                                 String color, String status, long revision, Instant createdAt, Instant updatedAt,
                                 UUID createdBy, UUID updatedBy, List<PropertyResponse> properties) {
    public static ObjectTypeResponse from(ObjectType t) {
        return new ObjectTypeResponse(t.id(), t.apiName(), t.displayName(), t.description(), t.icon(), t.color(),
                t.status().name(), t.revision(), t.createdAt(), t.updatedAt(), t.createdBy(), t.updatedBy(),
                t.properties().stream().map(PropertyResponse::from).toList());
    }
}
