package io.orion.ontology.api;

import io.orion.ontology.application.LinkTypeService;
import io.orion.ontology.api.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ontology/link-types")
public class LinkTypeController {
    private final LinkTypeService service;
    public LinkTypeController(LinkTypeService service) { this.service = service; }

    @GetMapping
    public List<LinkTypeResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.list(includeArchived).stream().map(LinkTypeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public LinkTypeResponse get(@PathVariable UUID id) { return LinkTypeResponse.from(service.get(id)); }

    @PostMapping
    @PreAuthorize(io.orion.ontology.OntologyAccess.CAN_MODIFY)
    @ResponseStatus(HttpStatus.CREATED)
    public LinkTypeResponse create(@Valid @RequestBody CreateLinkTypeRequest r) {
        return LinkTypeResponse.from(service.create(new LinkTypeService.CreateLinkTypeCommand(
                r.apiName(), r.displayName(), r.description(), r.sourceObjectTypeId(), r.targetObjectTypeId(),
                r.cardinality(), r.bidirectional())));
    }

    @PutMapping("/{id}")
    @PreAuthorize(io.orion.ontology.OntologyAccess.CAN_MODIFY)
    public LinkTypeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateLinkTypeRequest r) {
        return LinkTypeResponse.from(service.update(id, new LinkTypeService.UpdateLinkTypeCommand(
                r.displayName(), r.description(), r.cardinality(), r.bidirectional(), r.expectedRevision())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(io.orion.ontology.OntologyAccess.CAN_MODIFY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) { service.archive(id); }
}
