package io.orion.ontology.api.dto;

import io.orion.ontology.domain.LinkType;
import java.time.Instant;
import java.util.UUID;

public record LinkTypeResponse(UUID id, String apiName, String displayName, String description,
                               UUID sourceObjectTypeId, UUID targetObjectTypeId, String cardinality,
                               boolean bidirectional, String status, long revision, Instant createdAt, Instant updatedAt) {
    public static LinkTypeResponse from(LinkType l) {
        return new LinkTypeResponse(l.id(), l.apiName(), l.displayName(), l.description(), l.sourceObjectTypeId(),
                l.targetObjectTypeId(), l.cardinality().name(), l.bidirectional(), l.status().name(),
                l.revision(), l.createdAt(), l.updatedAt());
    }
}
