package com.synapsecore.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthSessionRequest(
    @Size(max = 64) String tenantCode,
    @NotBlank @Size(max = 80) String username,
    @NotBlank @Size(max = 120) String password
) {
}
