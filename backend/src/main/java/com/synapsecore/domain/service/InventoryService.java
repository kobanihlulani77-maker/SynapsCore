package com.synapsecore.domain.service;

import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.InventoryAdjustmentRequest;
import com.synapsecore.domain.dto.InventoryReceiptRequest;
import com.synapsecore.domain.dto.InventoryReconciliationRequest;
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
import com.synapsecore.tenant.TenantScopeGuard;
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
    private final TenantScopeGuard tenantScopeGuard;

    @Transactional
    public InventoryStatusResponse updateInventory(InventoryUpdateRequest request) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Warehouse warehouse = requireWarehouse(tenantCode, request.warehouseCode(), "inventory update");
        Product product = requireProduct(tenantCode, request.productSku(), "inventory update");
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseIdForUpdate(product.getId(), warehouse.getId())
            .orElse(Inventory.builder().tenant(warehouse.getTenant()).product(product).warehouse(warehouse).build());

        if (inventory.getId() != null) {
            tenantScopeGuard.requireInventoryForTenant(inventory, warehouse, tenantCode, "inventory update");
        }

        long reserved = inventory.getQuantityReserved() == null ? 0L : inventory.getQuantityReserved();
        inventory.setTenant(warehouse.getTenant());
        inventory.setQuantityOnHand(request.quantityAvailable() + reserved);
        inventory.setQuantityReserved(reserved);
        inventory.setReorderThreshold(request.reorderThreshold());
        inventory.synchronizeStockModel();

        Inventory savedInventory = saveAndPublishInventoryChange(
            inventory,
            tenantCode,
            "inventory-api",
            BusinessEventType.INVENTORY_UPDATED,
            "Inventory baseline set for " + product.resolveCatalogSku() + " in " + warehouse.getCode()
                + " to on-hand " + savedInventoryDescriptionOnHand(inventory) + ".",
            "INVENTORY_UPDATED",
            product.resolveCatalogSku() + "@" + warehouse.getCode(),
            "Inventory baseline set to on-hand " + inventory.getQuantityOnHand()
                + ", reserved " + inventory.getQuantityReserved()
                + ", available " + inventory.getQuantityAvailable()
                + ", threshold " + inventory.getReorderThreshold()
        );

        return toInventoryStatusResponse(savedInventory);
    }

    @Transactional
    public InventoryStatusResponse receiveInventory(InventoryReceiptRequest request) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Inventory inventory = requireInventoryForUpdate(tenantCode, request.warehouseCode(), request.productSku(), "inventory receipt");
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + request.quantityReceived());
        long inbound = inventory.getQuantityInbound() == null ? 0L : inventory.getQuantityInbound();
        inventory.setQuantityInbound(Math.max(inbound - request.quantityReceived(), 0L));
        inventory.setLastReceivedAt(java.time.Instant.now());
        inventory.synchronizeStockModel();

        Inventory savedInventory = saveAndPublishInventoryChange(
            inventory,
            tenantCode,
            "inventory-receipt",
            BusinessEventType.INVENTORY_RECEIVED,
            "Received " + request.quantityReceived() + " units of " + inventory.getProduct().resolveCatalogSku()
                + " into " + inventory.getWarehouse().getCode() + ".",
            "INVENTORY_RECEIVED",
            inventory.getProduct().resolveCatalogSku() + "@" + inventory.getWarehouse().getCode(),
            buildInventoryActionDetails(
                "Received " + request.quantityReceived() + " units.",
                request.note(),
                savedInventoryDescription(inventory)
            )
        );
        return toInventoryStatusResponse(savedInventory);
    }

    @Transactional
    public InventoryStatusResponse adjustInventory(InventoryAdjustmentRequest request) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Inventory inventory = requireInventoryForUpdate(tenantCode, request.warehouseCode(), request.productSku(), "inventory adjustment");
        long nextOnHand = inventory.getQuantityOnHand() + request.quantityDelta();
        if (nextOnHand < inventory.getQuantityReserved()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Adjustment would drop on-hand stock below reserved commitments for SKU "
                    + inventory.getProduct().resolveCatalogSku() + ".");
        }

        inventory.setQuantityOnHand(nextOnHand);
        inventory.setLastAdjustedAt(java.time.Instant.now());
        inventory.synchronizeStockModel();

        Inventory savedInventory = saveAndPublishInventoryChange(
            inventory,
            tenantCode,
            "inventory-adjustment",
            BusinessEventType.INVENTORY_ADJUSTED,
            "Adjusted " + inventory.getProduct().resolveCatalogSku() + " in " + inventory.getWarehouse().getCode()
                + " by " + request.quantityDelta() + " units.",
            "INVENTORY_ADJUSTED",
            inventory.getProduct().resolveCatalogSku() + "@" + inventory.getWarehouse().getCode(),
            buildInventoryActionDetails(
                "Adjusted by " + request.quantityDelta() + " units.",
                request.reason(),
                savedInventoryDescription(inventory)
            )
        );
        return toInventoryStatusResponse(savedInventory);
    }

    @Transactional
    public InventoryStatusResponse reconcileInventory(InventoryReconciliationRequest request) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Inventory inventory = requireInventoryForUpdate(tenantCode, request.warehouseCode(), request.productSku(), "inventory reconciliation");
        if (request.countedOnHand() < inventory.getQuantityReserved()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Reconciliation count is lower than reserved commitments for SKU "
                    + inventory.getProduct().resolveCatalogSku() + ".");
        }

        long previousOnHand = inventory.getQuantityOnHand();
        inventory.setQuantityOnHand(request.countedOnHand());
        inventory.setReconciliationVariance(request.countedOnHand() - previousOnHand);
        inventory.setLastReconciledAt(java.time.Instant.now());
        inventory.synchronizeStockModel();

        Inventory savedInventory = saveAndPublishInventoryChange(
            inventory,
            tenantCode,
            "inventory-reconciliation",
            BusinessEventType.INVENTORY_RECONCILED,
            "Reconciled " + inventory.getProduct().resolveCatalogSku() + " in " + inventory.getWarehouse().getCode()
                + " to counted on-hand " + request.countedOnHand() + ".",
            "INVENTORY_RECONCILED",
            inventory.getProduct().resolveCatalogSku() + "@" + inventory.getWarehouse().getCode(),
            buildInventoryActionDetails(
                "Reconciled to counted on-hand " + request.countedOnHand() + ".",
                request.note(),
                savedInventoryDescription(inventory)
            )
        );
        return toInventoryStatusResponse(savedInventory);
    }

    @Transactional
    public Inventory reserveStock(String tenantCode,
                                  Warehouse warehouse,
                                  Product product,
                                  int quantity,
                                  String source,
                                  String reason) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseIdForUpdate(product.getId(), warehouse.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No inventory found for SKU " + product.resolveCatalogSku() + " in warehouse " + warehouse.getCode()));
        tenantScopeGuard.requireInventoryForTenant(inventory, warehouse, tenantCode, "inventory reservation");
        if (inventory.getQuantityAvailable() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Insufficient inventory for SKU " + product.resolveCatalogSku() + " in warehouse " + warehouse.getCode());
        }

        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
        inventory.synchronizeStockModel();
        return saveAndPublishInventoryChange(
            inventory,
            tenantCode,
            source,
            BusinessEventType.INVENTORY_UPDATED,
            "Reserved " + quantity + " units of " + product.resolveCatalogSku() + " in " + warehouse.getCode() + ".",
            "INVENTORY_RESERVED",
            product.resolveCatalogSku() + "@" + warehouse.getCode(),
            buildInventoryActionDetails("Reserved " + quantity + " units.", reason, savedInventoryDescription(inventory))
        );
    }

    @Transactional
    public Inventory releaseReservedStock(Inventory inventory, long quantity, String source, String reason) {
        inventory = lockInventoryForUpdate(inventory, "inventory reservation release");
        long releasable = Math.min(quantity, inventory.getQuantityReserved());
        if (releasable <= 0) {
            return inventory;
        }
        inventory.setQuantityReserved(inventory.getQuantityReserved() - releasable);
        inventory.synchronizeStockModel();
        return saveAndPublishInventoryChange(
            inventory,
            inventory.getTenant().getCode(),
            source,
            BusinessEventType.INVENTORY_UPDATED,
            "Released " + releasable + " reserved units of " + inventory.getProduct().resolveCatalogSku()
                + " in " + inventory.getWarehouse().getCode() + ".",
            "INVENTORY_RESERVATION_RELEASED",
            inventory.getProduct().resolveCatalogSku() + "@" + inventory.getWarehouse().getCode(),
            buildInventoryActionDetails("Released " + releasable + " reserved units.", reason, savedInventoryDescription(inventory))
        );
    }

    @Transactional
    public Inventory fulfillReservedStock(Inventory inventory, long quantity, String source, String reason) {
        inventory = lockInventoryForUpdate(inventory, "inventory reservation fulfillment");
        if (quantity < 1) {
            return inventory;
        }
        if (inventory.getQuantityReserved() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot fulfill more reserved stock than is currently reserved for SKU "
                    + inventory.getProduct().resolveCatalogSku() + ".");
        }
        if (inventory.getQuantityOnHand() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot fulfill more stock than is currently on hand for SKU "
                    + inventory.getProduct().resolveCatalogSku() + ".");
        }

        inventory.setQuantityReserved(inventory.getQuantityReserved() - quantity);
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - quantity);
        inventory.synchronizeStockModel();
        return saveAndPublishInventoryChange(
            inventory,
            inventory.getTenant().getCode(),
            source,
            BusinessEventType.INVENTORY_UPDATED,
            "Committed " + quantity + " reserved units of " + inventory.getProduct().resolveCatalogSku()
                + " from " + inventory.getWarehouse().getCode() + ".",
            "INVENTORY_COMMITTED",
            inventory.getProduct().resolveCatalogSku() + "@" + inventory.getWarehouse().getCode(),
            buildInventoryActionDetails("Committed " + quantity + " reserved units.", reason, savedInventoryDescription(inventory))
        );
    }

    @Transactional
    public Inventory restockReturnedStock(Inventory inventory, long quantity, String source, String reason) {
        inventory = lockInventoryForUpdate(inventory, "inventory return restock");
        if (quantity < 1) {
            return inventory;
        }
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + quantity);
        inventory.synchronizeStockModel();
        return saveAndPublishInventoryChange(
            inventory,
            inventory.getTenant().getCode(),
            source,
            BusinessEventType.INVENTORY_RECEIVED,
            "Returned " + quantity + " units of " + inventory.getProduct().resolveCatalogSku()
                + " back into " + inventory.getWarehouse().getCode() + ".",
            "INVENTORY_RETURNED",
            inventory.getProduct().resolveCatalogSku() + "@" + inventory.getWarehouse().getCode(),
            buildInventoryActionDetails("Restocked " + quantity + " returned units.", reason, savedInventoryDescription(inventory))
        );
    }

    @Transactional
    public void reevaluateOperationalSignals(String tenantCode,
                                             Warehouse warehouse,
                                             Product product,
                                             String source) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No inventory found for SKU " + product.resolveCatalogSku() + " in warehouse " + warehouse.getCode()));
        tenantScopeGuard.requireInventoryForTenant(inventory, warehouse, tenantCode, "inventory signal refresh");
        inventoryMonitoringService.evaluateAfterChange(inventory, source);
    }

    @Transactional(readOnly = true)
    public Inventory requireInventory(String tenantCode, String warehouseCode, String productSku, String context) {
        Warehouse warehouse = requireWarehouse(tenantCode, warehouseCode, context);
        Product product = requireProduct(tenantCode, productSku, context);
        return inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
            .map(inventory -> {
                tenantScopeGuard.requireInventoryForTenant(inventory, warehouse, tenantCode, context);
                return inventory;
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Inventory not found for SKU " + product.resolveCatalogSku() + " in warehouse " + warehouse.getCode()));
    }

    private Inventory requireInventoryForUpdate(String tenantCode, String warehouseCode, String productSku, String context) {
        Warehouse warehouse = requireWarehouse(tenantCode, warehouseCode, context);
        Product product = requireProduct(tenantCode, productSku, context);
        return inventoryRepository.findByProductIdAndWarehouseIdForUpdate(product.getId(), warehouse.getId())
            .map(inventory -> {
                tenantScopeGuard.requireInventoryForTenant(inventory, warehouse, tenantCode, context);
                return inventory;
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Inventory not found for SKU " + product.resolveCatalogSku() + " in warehouse " + warehouse.getCode()));
    }

    private Inventory lockInventoryForUpdate(Inventory inventory, String context) {
        if (inventory.getId() == null) {
            return inventory;
        }
        Inventory lockedInventory = inventoryRepository.findByIdForUpdate(inventory.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Inventory record no longer exists for " + context + "."));
        tenantScopeGuard.requireInventoryForTenant(
            lockedInventory,
            lockedInventory.getWarehouse(),
            lockedInventory.getTenant().getCode(),
            context
        );
        return lockedInventory;
    }

    @Transactional(readOnly = true)
    public Product requireProduct(String tenantCode, String productSku, String context) {
        return productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(tenantCode, productSku.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Product not found for tenant " + tenantCode + ": " + productSku + " (" + context + ")"));
    }

    @Transactional(readOnly = true)
    public Warehouse requireWarehouse(String tenantCode, String warehouseCode, String context) {
        Warehouse warehouse = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(tenantCode, warehouseCode.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Warehouse not found: " + warehouseCode));
        tenantScopeGuard.requireWarehouseForTenant(warehouse, tenantCode, context);
        return warehouse;
    }

    private Inventory saveAndPublishInventoryChange(Inventory inventory,
                                                    String tenantCode,
                                                    String source,
                                                    BusinessEventType eventType,
                                                    String eventSummary,
                                                    String auditAction,
                                                    String targetRef,
                                                    String auditDetails) {
        Inventory savedInventory = inventoryRepository.save(inventory);
        tenantScopeGuard.requireInventoryForTenant(savedInventory, savedInventory.getWarehouse(), tenantCode, "inventory persistence");
        businessEventService.record(eventType, source, eventSummary);
        inventoryMonitoringService.evaluateAfterChange(savedInventory, source);
        operationalStateChangePublisher.publish(OperationalUpdateType.INVENTORY_UPDATE, source);
        auditLogService.recordSuccess(
            auditAction,
            source,
            source,
            "Inventory",
            targetRef,
            auditDetails
        );
        return savedInventory;
    }

    private String buildInventoryActionDetails(String action, String noteOrReason, String stateSummary) {
        if (noteOrReason == null || noteOrReason.isBlank()) {
            return action + " " + stateSummary;
        }
        return action + " Note: " + noteOrReason.trim() + ". " + stateSummary;
    }

    private String savedInventoryDescription(Inventory inventory) {
        return "On-hand " + inventory.getQuantityOnHand()
            + ", reserved " + inventory.getQuantityReserved()
            + ", inbound " + inventory.getQuantityInbound()
            + ", available " + inventory.getQuantityAvailable()
            + ", threshold " + inventory.getReorderThreshold() + ".";
    }

    private long savedInventoryDescriptionOnHand(Inventory inventory) {
        return inventory.getQuantityOnHand() == null ? 0L : inventory.getQuantityOnHand();
    }

    private InventoryStatusResponse toInventoryStatusResponse(Inventory inventory) {
        StockPrediction prediction = stockPredictionService.estimate(inventory);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction);

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
}
