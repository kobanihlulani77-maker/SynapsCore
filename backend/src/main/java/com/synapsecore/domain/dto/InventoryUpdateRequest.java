package com.synapsecore.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryUpdateRequest(
    @NotBlank @Size(max = 64) String productSku,
    @NotBlank @Size(max = 40) String warehouseCode,
    @NotNull @Min(0) Long quantityAvailable,
    @NotNull @Min(0) Long reorderThreshold
) {
}
