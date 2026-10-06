package io.orion.ontology.api;

import io.orion.ontology.application.OntologyQueryService;
import io.orion.ontology.api.dto.OntologyChangeResponse;
import io.orion.ontology.api.dto.OntologySnapshotResponse;
import io.orion.shared.paging.PageResult;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ontology")
public class OntologyController {
    private final OntologyQueryService service;
    public OntologyController(OntologyQueryService service) { this.service = service; }

    @GetMapping
    public OntologySnapshotResponse snapshot() { return OntologySnapshotResponse.from(service.snapshot()); }

    @GetMapping("/changes")
    public PageResult<OntologyChangeResponse> changes(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return service.changes(page, size).map(OntologyChangeResponse::from);
    }
}
