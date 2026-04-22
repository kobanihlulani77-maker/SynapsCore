package com.synapsecore.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductUpsertRequest(
    @NotBlank
    @Size(max = 64)
    String sku,

    @NotBlank
    @Size(max = 120)
    String name,

    @NotBlank
    @Size(max = 120)
    String category
) {
}
