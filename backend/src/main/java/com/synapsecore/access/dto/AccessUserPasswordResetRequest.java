package com.synapsecore.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccessUserPasswordResetRequest(
    @NotBlank
    @Size(min = 8, max = 120)
    String password
) {
}
