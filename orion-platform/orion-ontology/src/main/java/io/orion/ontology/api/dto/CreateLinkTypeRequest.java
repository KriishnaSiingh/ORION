package io.orion.ontology.api.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;
import io.orion.ontology.domain.Cardinality;

public record CreateLinkTypeRequest(
    @NotBlank @Pattern(regexp = io.orion.ontology.domain.OntologyNames.LINK_TYPE_REGEX, message = "must be UPPER_SNAKE_CASE, 2-63 chars, e.g. WORKS_AT") String apiName,
    @NotBlank @Size(max = 100) String displayName,
    @Size(max = 1000) String description,
    @NotNull UUID sourceObjectTypeId,
    @NotNull UUID targetObjectTypeId,
    @NotNull Cardinality cardinality,
    boolean bidirectional) {}
