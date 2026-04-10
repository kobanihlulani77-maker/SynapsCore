package com.synapsecore.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccessUserCreateRequest(
    @NotBlank
    @Size(max = 80)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "username must contain only letters, digits, dots, underscores, and hyphens")
    String username,

    @NotBlank
    @Size(max = 120)
    String fullName,

    @NotBlank
    @Size(min = 8, max = 120)
    String password,

    @NotBlank
    @Size(max = 80)
    String operatorActorName
) {
}
