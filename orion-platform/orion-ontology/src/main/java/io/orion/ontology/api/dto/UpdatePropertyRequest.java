package io.orion.ontology.api.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record UpdatePropertyRequest(
    @NotBlank @Size(max = 100) String displayName,
    @Size(max = 1000) String description,
    @NotNull Boolean required, @NotNull Boolean unique, @NotNull Boolean searchable, @NotNull Boolean filterable,
    @Size(max = 100) List<@NotBlank @Size(max = 100) String> enumValues,
    @NotNull @Min(1) Long expectedRevision) {}
