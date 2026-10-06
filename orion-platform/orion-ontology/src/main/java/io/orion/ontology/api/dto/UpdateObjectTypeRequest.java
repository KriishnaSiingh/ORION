package io.orion.ontology.api.dto;

import jakarta.validation.constraints.*;

public record UpdateObjectTypeRequest(
    @NotBlank @Size(max = 100) String displayName,
    @Size(max = 1000) String description,
    @Size(max = 50) String icon,
    @Pattern(regexp = io.orion.ontology.domain.OntologyNames.COLOR_REGEX, message = "must look like #3366FF") String color,
    @NotNull @Min(1) Long expectedRevision) {}
