package io.orion.identity.api.dto.request;

import jakarta.validation.constraints.*;

public record RegisterTenantRequest(
    @NotBlank @Size(max = 200) String tenantName,
    @NotBlank @Pattern(regexp = "^[a-z0-9]([a-z0-9-]{1,61}[a-z0-9])$",
             message = "3-63 chars, lowercase letters, digits and hyphens; cannot start/end with a hyphen") String tenantSlug,
    @NotBlank @Email @Size(max = 320) String adminEmail,
    @NotBlank @Size(max = 200) String adminName,
    @NotBlank @Size(min = 12, max = 128) String password) {}
