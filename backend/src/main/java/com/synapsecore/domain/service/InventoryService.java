package com.synapsecore.domain.service;

import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.InventoryStatusResponse;
import com.synapsecore.domain.dto.InventoryUpdateRequest;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.intelligence.InventoryInsight;
import com.synapsecore.intelligence.InventoryIntelligenceService;
import com.synapsecore.intelligence.InventoryMonitoringService;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.prediction.StockPredictionService;
import com.synapsecore.tenant.TenantContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryMonitoringService inventoryMonitoringService;
    private final InventoryIntelligenceService inventoryIntelligenceService;
    private final StockPredictionService stockPredictionService;
    private final BusinessEventService businessEventService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final AuditLogService auditLogService;
    private final TenantContextService tenantContextService;

    @Transactional
    public InventoryStatusResponse updateInventory(InventoryUpdateRequest request) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Product product = productRepository.findBySku(request.productSku().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Product not found: " + request.productSku()));
        Warehouse warehouse = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(tenantCode, request.warehouseCode().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Warehouse not found: " + request.warehouseCode()));

        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
            .orElse(Inventory.builder().product(product).warehouse(warehouse).build());

        inventory.setQuantityAvailable(request.quantityAvailable());
        inventory.setReorderThreshold(request.reorderThreshold());

        Inventory savedInventory = inventoryRepository.save(inventory);
        businessEventService.record(
            BusinessEventType.INVENTORY_UPDATED,
            "inventory-api",
            product.getSku() + " inventory set to " + savedInventory.getQuantityAvailable()
                + " units in " + warehouse.getCode()
        );

        inventoryMonitoringService.evaluateAfterChange(savedInventory, "inventory-api");
        operationalStateChangePublisher.publish(OperationalUpdateType.INVENTORY_UPDATE, "inventory-api");
        auditLogService.recordSuccess(
            "INVENTORY_UPDATED",
            "inventory-api",
            "inventory-api",
            "Inventory",
            product.getSku() + "@" + warehouse.getCode(),
            "Inventory set to " + savedInventory.getQuantityAvailable()
                + " with threshold " + savedInventory.getReorderThreshold()
        );

        return toInventoryStatusResponse(savedInventory);
    }

    private InventoryStatusResponse toInventoryStatusResponse(Inventory inventory) {
        StockPrediction prediction = stockPredictionService.estimate(inventory);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction);

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
}
