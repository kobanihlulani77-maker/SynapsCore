package com.synapsecore.scenario.dto;

import com.synapsecore.domain.dto.InventoryStatusResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ScenarioOrderImpactResponse(
    String warehouseCode,
    String warehouseName,
    BigDecimal projectedOrderValue,
    int totalUnits,
    List<InventoryStatusResponse> projectedInventory,
    List<ScenarioAlertProjection> projectedAlerts,
    List<ScenarioRecommendationProjection> projectedRecommendations,
    Instant analyzedAt
) {
}
