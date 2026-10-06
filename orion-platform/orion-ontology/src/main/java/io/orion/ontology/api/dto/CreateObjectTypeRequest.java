package io.orion.ontology.api.dto;

import io.orion.ontology.domain.OntologyNames;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateObjectTypeRequest(
    @NotBlank @Pattern(regexp = OntologyNames.OBJECT_TYPE_REGEX, message = "must be PascalCase, 2-63 chars, e.g. BankAccount") String apiName,
    @NotBlank @Size(max = 100) String displayName,
    @Size(max = 1000) String description,
    @Size(max = 50) String icon,
    @Pattern(regexp = OntologyNames.COLOR_REGEX, message = "must look like #3366FF") String color) {}
