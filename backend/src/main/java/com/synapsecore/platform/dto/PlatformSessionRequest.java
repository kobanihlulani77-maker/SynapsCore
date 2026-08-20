package com.synapsecore.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record PlatformSessionRequest(
    @NotBlank String username,
    @NotBlank String password
) {
}
