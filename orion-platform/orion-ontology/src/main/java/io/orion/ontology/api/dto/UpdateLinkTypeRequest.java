package io.orion.ontology.api.dto;

import jakarta.validation.constraints.*;
import io.orion.ontology.domain.Cardinality;

public record UpdateLinkTypeRequest(
    @NotBlank @Size(max = 100) String displayName,
    @Size(max = 1000) String description,
    @NotNull Cardinality cardinality,
    @NotNull Boolean bidirectional,
    @NotNull @Min(1) Long expectedRevision) {}
