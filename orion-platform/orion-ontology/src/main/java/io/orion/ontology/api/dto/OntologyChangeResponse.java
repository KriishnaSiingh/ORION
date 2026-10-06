package io.orion.ontology.api.dto;

import io.orion.ontology.domain.OntologyChange;
import java.time.Instant;
import java.util.UUID;

public record OntologyChangeResponse(UUID id, long ontologyVersion, String entityType, UUID entityId,
                                     String changeType, Object before, Object after, UUID changedBy, Instant changedAt) {
    public static OntologyChangeResponse from(OntologyChange c) {
        return new OntologyChangeResponse(c.id(), c.ontologyVersion(), c.entityType().name(), c.entityId(),
                c.changeType().name(), c.before(), c.after(), c.changedBy(), c.changedAt());
    }
}
