package io.orion.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(max = 63) String tenantSlug,
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(max = 128) String password) {}
