package io.orion.ontology.api;

import io.orion.ontology.application.ObjectTypeService;
import io.orion.ontology.api.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ontology/object-types")
public class ObjectTypeController {
    private final ObjectTypeService service;
    public ObjectTypeController(ObjectTypeService service) { this.service = service; }

    @GetMapping
    public List<ObjectTypeResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.list(includeArchived).stream().map(ObjectTypeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ObjectTypeResponse get(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ObjectTypeResponse.from(service.get(id, includeArchived));
    }

    @PostMapping
    @PreAuthorize(io.orion.ontology.OntologyAccess.CAN_MODIFY)
    @ResponseStatus(HttpStatus.CREATED)
    public ObjectTypeResponse create(@Valid @RequestBody CreateObjectTypeRequest r) {
        return ObjectTypeResponse.from(service.create(new ObjectTypeService.CreateObjectTypeCommand(
                r.apiName(), r.displayName(), r.description(), r.icon(), r.color())));
    }

    @PutMapping("/{id}")
    @PreAuthorize(io.orion.ontology.OntologyAccess.CAN_MODIFY)
    public ObjectTypeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateObjectTypeRequest r) {
        return ObjectTypeResponse.from(service.update(id, new ObjectTypeService.UpdateObjectTypeCommand(
                r.displayName(), r.description(), r.icon(), r.color(), r.expectedRevision())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(io.orion.ontology.OntologyAccess.CAN_MODIFY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) { service.archive(id); }

    @PostMapping("/{id}/properties")
    @PreAuthorize(io.orion.ontology.OntologyAccess.CAN_MODIFY)
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse addProperty(@PathVariable UUID id, @Valid @RequestBody CreatePropertyRequest r) {
        return PropertyResponse.from(service.addProperty(id, new ObjectTypeService.CreatePropertyCommand(
                r.apiName(), r.displayName(), r.description(), r.dataType(), r.required(), r.unique(),
                r.searchable(), r.filterable(), r.multiValued(), r.enumValues())));
    }

    @PutMapping("/{id}/properties/{propertyId}")
    @PreAuthorize(io.orion.ontology.OntologyAccess.CAN_MODIFY)
    public PropertyResponse updateProperty(@PathVariable UUID id, @PathVariable UUID propertyId,
                                           @Valid @RequestBody UpdatePropertyRequest r) {
        return PropertyResponse.from(service.updateProperty(id, propertyId, new ObjectTypeService.UpdatePropertyCommand(
                r.displayName(), r.description(), r.required(), r.unique(), r.searchable(), r.filterable(),
                r.enumValues(), r.expectedRevision())));
    }

    @DeleteMapping("/{id}/properties/{propertyId}")
    @PreAuthorize(io.orion.ontology.OntologyAccess.CAN_MODIFY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveProperty(@PathVariable UUID id, @PathVariable UUID propertyId) {
        service.archiveProperty(id, propertyId);
    }
}
