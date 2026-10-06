package io.orion.identity.api.dto.request;

import jakarta.validation.constraints.*;
import java.util.Set;

public record CreateUserRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(min = 12, max = 128) String password,
    @NotEmpty Set<@NotBlank String> roles) {}
