package com.synapsecore.domain.dto;

import java.time.Instant;

public record InventoryStatusResponse(
    Long id,
    String productSku,
    String productName,
    String category,
    String warehouseCode,
    String warehouseName,
    Long quantityAvailable,
    Long reorderThreshold,
    boolean lowStock,
    boolean rapidConsumption,
    String riskLevel,
    Double unitsPerHour,
    Double hoursToStockout,
    Instant updatedAt
) {
}
