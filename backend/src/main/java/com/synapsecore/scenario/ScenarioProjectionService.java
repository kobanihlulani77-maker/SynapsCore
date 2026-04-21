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
import com.synapsecore.tenant.TenantScopeGuard;
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
    private final TenantScopeGuard tenantScopeGuard;

    @Transactional(readOnly = true)
    public ScenarioOrderImpactResponse projectOrderImpact(OrderCreateRequest request) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Warehouse warehouse = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(
                tenantCode,
                request.warehouseCode().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Warehouse not found: " + request.warehouseCode()));
        tenantScopeGuard.requireWarehouseForTenant(warehouse, tenantCode, "scenario order impact projection");

        BigDecimal projectedOrderValue = BigDecimal.ZERO;
        int totalUnits = 0;
        Map<Long, InventoryProjectionInput> projectionsByInventoryId = new LinkedHashMap<>();

        for (var itemRequest : request.items()) {
            Product product = productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(tenantCode, itemRequest.productSku().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + itemRequest.productSku()));

            Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No inventory found for SKU " + product.resolveCatalogSku() + " in warehouse " + warehouse.getCode()));
            tenantScopeGuard.requireInventoryForTenant(inventory, warehouse, tenantCode, "scenario order impact projection");

            InventoryProjectionInput projection = projectionsByInventoryId.computeIfAbsent(
                inventory.getId(),
                ignored -> new InventoryProjectionInput(inventory, 0)
            );

            int projectedQuantity = projection.requestedUnits() + itemRequest.quantity();
            if (inventory.getQuantityAvailable() < projectedQuantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient inventory for SKU " + product.resolveCatalogSku() + " in warehouse " + warehouse.getCode());
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
                .tenant(projection.inventory().getTenant())
                .product(projection.inventory().getProduct())
                .warehouse(projection.inventory().getWarehouse())
                .quantityOnHand(projection.inventory().getQuantityOnHand())
                .quantityReserved(projection.inventory().getQuantityReserved() + (long) projection.requestedUnits())
                .quantityInbound(projection.inventory().getQuantityInbound())
                .quantityAvailable(projection.inventory().getQuantityAvailable() - projection.requestedUnits())
                .reorderThreshold(projection.inventory().getReorderThreshold())
                .lastReceivedAt(projection.inventory().getLastReceivedAt())
                .lastAdjustedAt(projection.inventory().getLastAdjustedAt())
                .lastReconciledAt(projection.inventory().getLastReconciledAt())
                .reconciliationVariance(projection.inventory().getReconciliationVariance())
                .updatedAt(Instant.now())
                .build();
            projected.synchronizeStockModel();

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
            inventory.getProduct().resolveCatalogSku(),
            inventory.getProduct().getName(),
            inventory.getProduct().getCategory(),
            inventory.getWarehouse().getCode(),
            inventory.getWarehouse().getName(),
            inventory.getQuantityAvailable(),
            inventory.getQuantityOnHand(),
            inventory.getQuantityReserved(),
            inventory.getQuantityInbound(),
            inventory.getReorderThreshold(),
            insight.lowStock(),
            insight.rapidConsumption(),
            insight.riskLevel(),
            prediction.unitsPerHour(),
            prediction.hoursToStockout(),
            inventory.getLastReceivedAt(),
            inventory.getLastAdjustedAt(),
            inventory.getLastReconciledAt(),
            inventory.getReconciliationVariance(),
            inventory.getUpdatedAt()
        );
    }

    private record InventoryProjectionInput(Inventory inventory, int requestedUnits) {
    }
}
