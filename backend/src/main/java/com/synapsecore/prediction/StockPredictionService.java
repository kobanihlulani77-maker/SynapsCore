package com.synapsecore.prediction;

import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.TenantOperationalPolicy;
import com.synapsecore.domain.repository.OrderItemRepository;
import com.synapsecore.domain.service.TenantOperationalPolicyService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockPredictionService {

    private final OrderItemRepository orderItemRepository;
    private final TenantOperationalPolicyService tenantOperationalPolicyService;

    public StockPrediction estimate(Inventory inventory) {
        var policy = tenantOperationalPolicyService.getPolicy(
            inventory.getTenant() != null
                ? inventory.getTenant().getCode()
                : inventory.getWarehouse().getTenant().getCode()
        );
        return estimate(inventory, policy);
    }

    public StockPrediction estimate(Inventory inventory, TenantOperationalPolicy policy) {
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentUnits = orderItemRepository.sumRecentQuantityByProductAndWarehouse(
            inventory.getProduct().getId(),
            inventory.getWarehouse().getId(),
            since
        );
        return estimate(inventory, policy, recentUnits);
    }

    public StockPrediction estimate(Inventory inventory,
                                    TenantOperationalPolicy policy,
                                    long recentUnits) {

        double unitsPerHour = recentUnits;
        Double hoursToStockout = unitsPerHour > 0
            ? inventory.getQuantityAvailable() / unitsPerHour
            : null;

        boolean depletionRisk = hoursToStockout != null && hoursToStockout <= policy.getDepletionRiskHoursThreshold();
        boolean urgentRisk = hoursToStockout != null && hoursToStockout <= policy.getUrgentDepletionRiskHoursThreshold();
        boolean rapidConsumption = recentUnits >= Math.max(
            policy.getRapidConsumptionUnitsMinimum(),
            Math.round(inventory.getReorderThreshold() * policy.getRapidConsumptionThresholdRatio())
        );

        return new StockPrediction(
            recentUnits,
            unitsPerHour,
            hoursToStockout,
            depletionRisk,
            urgentRisk,
            rapidConsumption
        );
    }

    public Map<Long, Long> loadRecentUnitsByInventoryId(List<Inventory> inventories, Instant since) {
        if (inventories.isEmpty()) {
            return Map.of();
        }

        var productIds = new LinkedHashSet<Long>();
        var warehouseIds = new LinkedHashSet<Long>();
        inventories.forEach(inventory -> {
            productIds.add(inventory.getProduct().getId());
            warehouseIds.add(inventory.getWarehouse().getId());
        });

        Map<DemandKey, Long> recentUnitsByKey = new LinkedHashMap<>();
        orderItemRepository.sumRecentQuantityByProductAndWarehouse(productIds, warehouseIds, since)
            .forEach(summary -> recentUnitsByKey.put(
                new DemandKey(summary.getProductId(), summary.getWarehouseId()),
                summary.getRecentUnits()
            ));

        Map<Long, Long> recentUnitsByInventoryId = new LinkedHashMap<>();
        inventories.forEach(inventory -> recentUnitsByInventoryId.put(
            inventory.getId(),
            recentUnitsByKey.getOrDefault(
                new DemandKey(inventory.getProduct().getId(), inventory.getWarehouse().getId()),
                0L
            )
        ));
        return recentUnitsByInventoryId;
    }

    private record DemandKey(Long productId, Long warehouseId) {
    }
}
