package io.orion.ontology.domain;

import java.time.Instant;
import java.util.*;

public record ObjectType(UUID id, UUID tenantId, String apiName, String displayName, String description,
                         String icon, String color, EntityStatus status, long revision,
                         Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy,
                         List<PropertyDefinition> properties) {

    public ObjectType { properties = List.copyOf(properties); }

    public static ObjectType create(UUID tenantId, String apiName, String displayName, String description,
                                    String icon, String color, UUID actor, Instant now) {
        return new ObjectType(UUID.randomUUID(), tenantId, OntologyNames.requireObjectTypeName(apiName),
                OntologyNames.requireDisplayName(displayName), OntologyNames.blankToNull(description),
                OntologyNames.blankToNull(icon), OntologyNames.requireColor(color),
                EntityStatus.ACTIVE, 1, now, now, actor, actor, List.of());
    }

    public ObjectType update(String displayName, String description, String icon, String color, UUID actor, Instant now) {
        return new ObjectType(id, tenantId, apiName, OntologyNames.requireDisplayName(displayName),
                OntologyNames.blankToNull(description), OntologyNames.blankToNull(icon), OntologyNames.requireColor(color),
                status, revision + 1, createdAt, now, createdBy, actor, properties);
    }

    public ObjectType archive(UUID actor, Instant now) {
        return new ObjectType(id, tenantId, apiName, displayName, description, icon, color,
                EntityStatus.ARCHIVED, revision + 1, createdAt, now, createdBy, actor, properties);
    }

    public ObjectType withProperties(List<PropertyDefinition> newProperties) {
        return new ObjectType(id, tenantId, apiName, displayName, description, icon, color, status, revision,
                createdAt, updatedAt, createdBy, updatedBy, newProperties);
    }

    public ObjectType withActivePropertiesOnly() {
        return withProperties(properties.stream().filter(PropertyDefinition::isActive).toList());
    }

    public Optional<PropertyDefinition> findProperty(UUID propertyId) {
        return properties.stream().filter(p -> p.id().equals(propertyId)).findFirst();
    }

    public boolean isActive() { return status == EntityStatus.ACTIVE; }
}
