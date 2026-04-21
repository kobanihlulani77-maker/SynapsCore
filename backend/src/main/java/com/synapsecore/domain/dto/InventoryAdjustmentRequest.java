package com.synapsecore.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentRequest(
    @NotBlank @Size(max = 64) String productSku,
    @NotBlank @Size(max = 40) String warehouseCode,
    @NotNull Long quantityDelta,
    @NotBlank @Size(max = 320) String reason
) {
}
