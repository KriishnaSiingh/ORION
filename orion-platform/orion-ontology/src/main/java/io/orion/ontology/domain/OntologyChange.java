package io.orion.ontology.domain;

import java.time.Instant;
import java.util.UUID;

public record OntologyChange(UUID id, UUID tenantId, long ontologyVersion, EntityType entityType, UUID entityId,
                             ChangeType changeType, Object before, Object after, UUID changedBy, Instant changedAt) {
    public enum EntityType { OBJECT_TYPE, PROPERTY, LINK_TYPE }
    public enum ChangeType { CREATED, UPDATED, ARCHIVED }
}
