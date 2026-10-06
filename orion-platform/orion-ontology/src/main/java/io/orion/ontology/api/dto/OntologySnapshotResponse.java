package io.orion.ontology.api.dto;

import io.orion.ontology.application.OntologyQueryService;
import java.util.List;

public record OntologySnapshotResponse(long version, List<ObjectTypeResponse> objectTypes, List<LinkTypeResponse> linkTypes) {
    public static OntologySnapshotResponse from(OntologyQueryService.OntologySnapshot s) {
        return new OntologySnapshotResponse(s.version(),
                s.objectTypes().stream().map(ObjectTypeResponse::from).toList(),
                s.linkTypes().stream().map(LinkTypeResponse::from).toList());
    }
}
