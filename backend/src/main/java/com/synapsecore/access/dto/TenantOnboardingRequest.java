package com.synapsecore.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantOnboardingRequest(
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "[A-Za-z0-9-]+", message = "tenantCode must contain only letters, digits, and hyphens")
    String tenantCode,

    @NotBlank
    @Size(max = 120)
    String tenantName,

    @Size(max = 240)
    String description,

    @NotBlank
    @Size(max = 120)
    String adminFullName,

    @NotBlank
    @Size(max = 80)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "adminUsername must contain only letters, digits, dots, underscores, and hyphens")
    String adminUsername,

    @NotBlank
    @Size(min = 8, max = 120)
    String adminPassword,

    @NotBlank
    @Size(max = 120)
    String primaryLocation,

    @Size(max = 120)
    String secondaryLocation
) {
}
