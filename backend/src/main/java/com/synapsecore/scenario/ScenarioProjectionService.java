package com.synapsecore.scenario;

import com.synapsecore.alert.AlertService;
import com.synapsecore.decision.RecommendationService;
import com.synapsecore.domain.dto.InventoryStatusResponse;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.intelligence.InventoryInsight;
import com.synapsecore.intelligence.InventoryIntelligenceService;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.prediction.StockPredictionService;
import com.synapsecore.scenario.dto.ScenarioAlertProjection;
import com.synapsecore.scenario.dto.ScenarioOrderImpactResponse;
import com.synapsecore.scenario.dto.ScenarioRecommendationProjection;
import com.synapsecore.tenant.TenantContextService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ScenarioProjectionService {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockPredictionService stockPredictionService;
    private final InventoryIntelligenceService inventoryIntelligenceService;
    private final RecommendationService recommendationService;
    private final AlertService alertService;
    private final TenantContextService tenantContextService;

    @Transactional(readOnly = true)
    public ScenarioOrderImpactResponse projectOrderImpact(OrderCreateRequest request) {
        Warehouse warehouse = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(
                tenantContextService.getCurrentTenantCodeOrDefault(),
                request.warehouseCode().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Warehouse not found: " + request.warehouseCode()));

        BigDecimal projectedOrderValue = BigDecimal.ZERO;
        int totalUnits = 0;
        Map<Long, InventoryProjectionInput> projectionsByInventoryId = new LinkedHashMap<>();

        for (var itemRequest : request.items()) {
            Product product = productRepository.findBySku(itemRequest.productSku().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + itemRequest.productSku()));

            Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No inventory found for SKU " + product.getSku() + " in warehouse " + warehouse.getCode()));

            InventoryProjectionInput projection = projectionsByInventoryId.computeIfAbsent(
                inventory.getId(),
                ignored -> new InventoryProjectionInput(inventory, 0)
            );

            int projectedQuantity = projection.requestedUnits() + itemRequest.quantity();
            if (inventory.getQuantityAvailable() < projectedQuantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient inventory for SKU " + product.getSku() + " in warehouse " + warehouse.getCode());
            }

            projectionsByInventoryId.put(inventory.getId(), new InventoryProjectionInput(inventory, projectedQuantity));
            projectedOrderValue = projectedOrderValue.add(
                itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()))
            );
            totalUnits += itemRequest.quantity();
        }

        List<InventoryStatusResponse> projectedInventory = new ArrayList<>();
        List<ScenarioAlertProjection> projectedAlerts = new ArrayList<>();
        List<ScenarioRecommendationProjection> projectedRecommendations = new ArrayList<>();

        for (InventoryProjectionInput projection : projectionsByInventoryId.values()) {
            Inventory projected = Inventory.builder()
                .id(projection.inventory().getId())
                .product(projection.inventory().getProduct())
                .warehouse(projection.inventory().getWarehouse())
                .quantityAvailable(projection.inventory().getQuantityAvailable() - projection.requestedUnits())
                .reorderThreshold(projection.inventory().getReorderThreshold())
                .updatedAt(Instant.now())
                .build();

            StockPrediction prediction = stockPredictionService.estimate(projected);
            InventoryInsight insight = inventoryIntelligenceService.evaluate(projected, prediction);
            ScenarioRecommendationProjection recommendation = recommendationService.previewForInventory(projected, insight, prediction);

            if (recommendation != null) {
                projectedRecommendations.add(recommendation);
            }
            projectedAlerts.addAll(alertService.previewInventoryAlerts(projected, insight, recommendation));
            projectedInventory.add(toInventoryStatusResponse(projected, insight, prediction));
        }

        return new ScenarioOrderImpactResponse(
            warehouse.getCode(),
            warehouse.getName(),
            projectedOrderValue,
            totalUnits,
            projectedInventory,
            projectedAlerts,
            projectedRecommendations,
            Instant.now()
        );
    }

    private InventoryStatusResponse toInventoryStatusResponse(Inventory inventory,
                                                              InventoryInsight insight,
                                                              StockPrediction prediction) {
        return new InventoryStatusResponse(
            inventory.getId(),
            inventory.getProduct().getSku(),
            inventory.getProduct().getName(),
            inventory.getProduct().getCategory(),
            inventory.getWarehouse().getCode(),
            inventory.getWarehouse().getName(),
            inventory.getQuantityAvailable(),
            inventory.getReorderThreshold(),
            insight.lowStock(),
            insight.rapidConsumption(),
            insight.riskLevel(),
            prediction.unitsPerHour(),
            prediction.hoursToStockout(),
            inventory.getUpdatedAt()
        );
    }

    private record InventoryProjectionInput(Inventory inventory, int requestedUnits) {
    }
}
