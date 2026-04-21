package com.synapsecore.prediction;

import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.repository.OrderItemRepository;
import com.synapsecore.domain.service.TenantOperationalPolicyService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockPredictionService {

    private final OrderItemRepository orderItemRepository;
    private final TenantOperationalPolicyService tenantOperationalPolicyService;

    public StockPrediction estimate(Inventory inventory) {
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        var policy = tenantOperationalPolicyService.getPolicy(
            inventory.getTenant() != null
                ? inventory.getTenant().getCode()
                : inventory.getWarehouse().getTenant().getCode()
        );
        long recentUnits = orderItemRepository.sumRecentQuantityByProductAndWarehouse(
            inventory.getProduct().getId(),
            inventory.getWarehouse().getId(),
            since
        );

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
}
