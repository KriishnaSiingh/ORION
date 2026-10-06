package io.orion.ontology.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LinkType(UUID id, UUID tenantId, String apiName, String displayName, String description,
                       UUID sourceObjectTypeId, UUID targetObjectTypeId, Cardinality cardinality,
                       boolean bidirectional, EntityStatus status, long revision,
                       Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {

    public static LinkType create(UUID tenantId, String apiName, String displayName, String description,
                                  UUID source, UUID target, Cardinality cardinality, boolean bidirectional,
                                  UUID actor, Instant now) {
        Objects.requireNonNull(cardinality, "cardinality");
        return new LinkType(UUID.randomUUID(), tenantId, OntologyNames.requireLinkTypeName(apiName),
                OntologyNames.requireDisplayName(displayName), OntologyNames.blankToNull(description),
                source, target, cardinality, bidirectional, EntityStatus.ACTIVE, 1, now, now, actor, actor);
    }

    /** Endpoints are immutable. Cardinality may only be loosened. */
    public LinkType update(String displayName, String description, Cardinality newCardinality,
                           boolean bidirectional, UUID actor, Instant now) {
        if (newCardinality.ordinal() < cardinality.ordinal()) {
            throw io.orion.shared.error.OrionException.validation("Cardinality can only be loosened (ONE_TO_ONE -> ONE_TO_MANY -> MANY_TO_MANY)");
        }
        return new LinkType(id, tenantId, apiName, OntologyNames.requireDisplayName(displayName),
                OntologyNames.blankToNull(description), sourceObjectTypeId, targetObjectTypeId, newCardinality,
                bidirectional, status, revision + 1, createdAt, now, createdBy, actor);
    }

    public LinkType archive(UUID actor, Instant now) {
        return new LinkType(id, tenantId, apiName, displayName, description, sourceObjectTypeId, targetObjectTypeId,
                cardinality, bidirectional, EntityStatus.ARCHIVED, revision + 1, createdAt, now, createdBy, actor);
    }

    public boolean isActive() { return status == EntityStatus.ACTIVE; }
}
