package com.synapsecore.prediction;

import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.repository.OrderItemRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockPredictionService {

    private final OrderItemRepository orderItemRepository;

    public StockPrediction estimate(Inventory inventory) {
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentUnits = orderItemRepository.sumRecentQuantityByProductAndWarehouse(
            inventory.getProduct().getId(),
            inventory.getWarehouse().getId(),
            since
        );

        double unitsPerHour = recentUnits;
        Double hoursToStockout = unitsPerHour > 0
            ? inventory.getQuantityAvailable() / unitsPerHour
            : null;

        boolean depletionRisk = hoursToStockout != null && hoursToStockout <= 8;
        boolean urgentRisk = hoursToStockout != null && hoursToStockout <= 4;
        boolean rapidConsumption = recentUnits >= Math.max(5, Math.round(inventory.getReorderThreshold() * 0.5));

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
