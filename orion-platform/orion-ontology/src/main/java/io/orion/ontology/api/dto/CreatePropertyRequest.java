package io.orion.ontology.api.dto;

import io.orion.ontology.domain.DataType;
import io.orion.ontology.domain.OntologyNames;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreatePropertyRequest(
    @NotBlank @Pattern(regexp = OntologyNames.PROPERTY_REGEX, message = "must be camelCase, 2-63 chars, e.g. dateOfBirth") String apiName,
    @NotBlank @Size(max = 100) String displayName,
    @Size(max = 1000) String description,
    @NotNull DataType dataType,
    boolean required, boolean unique, boolean searchable, boolean filterable, boolean multiValued,
    @Size(max = 100) List<@NotBlank @Size(max = 100) String> enumValues) {}
