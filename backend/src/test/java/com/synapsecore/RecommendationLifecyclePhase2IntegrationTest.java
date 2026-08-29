package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.decision.RecommendationReconciliationService;
import com.synapsecore.decision.RecommendationService;
import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationStatus;
import com.synapsecore.domain.entity.RecommendationType;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.fulfillment.FulfillmentAssessment;
import com.synapsecore.intelligence.InventoryInsight;
import com.synapsecore.prediction.StockPrediction;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationLifecyclePhase2IntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RecommendationReconciliationService reconciliationService;

    @Test
    void inventoryRecommendationsCoverReorderUrgencyTransferAndSourceDrivenRetirement() {
        Tenant tenant = tenant("REC-P2-" + UUID.randomUUID());
        Warehouse north = warehouse(tenant, "WH-NORTH");
        Warehouse coast = warehouse(tenant, "WH-COAST");
        Product reorderProduct = product(tenant, "SKU-REC-P2-" + UUID.randomUUID());
        Product transferProduct = product(tenant, "SKU-TRANSFER-P2-" + UUID.randomUUID());

        Inventory reorderInventory = inventory(north, reorderProduct, 5, 10);
        InventoryInsight normalLowStock = new InventoryInsight(true, false, false, false,
            AlertSeverity.HIGH, "HIGH", "Low stock.");
        InventoryInsight urgentLowStock = new InventoryInsight(true, false, true, false,
            AlertSeverity.CRITICAL, "CRITICAL", "Urgent low stock.");
        StockPrediction prediction = new StockPrediction(0, 0, null, false, false, false);

        Recommendation reorder = recommendationService.createForInventory(
            reorderInventory, normalLowStock, prediction, "phase2-test");
        assertThat(reorder.getType()).isEqualTo(RecommendationType.REORDER_STOCK);

        Recommendation urgent = recommendationService.createForInventory(
            reorderInventory, urgentLowStock, prediction, "phase2-test");
        assertThat(urgent.getId()).isEqualTo(reorder.getId());
        assertThat(urgent.getType()).isEqualTo(RecommendationType.REORDER_URGENTLY);

        Inventory destination = inventory(coast, transferProduct, 2, 10);
        Inventory source = inventory(north, transferProduct, 30, 10);
        long destinationBefore = destination.getQuantityAvailable();
        long sourceBefore = source.getQuantityAvailable();
        long orderCountBefore = customerOrderRepository.countByTenant_CodeIgnoreCase(tenant.getCode());

        Recommendation transfer = recommendationService.createForInventory(
            destination, normalLowStock, prediction, "phase2-test");
        assertThat(transfer.getType()).isEqualTo(RecommendationType.TRANSFER_STOCK);
        assertThat(transfer.getSuggestedQuantity()).isEqualTo(8L);
        assertThat(destination.getQuantityAvailable()).isEqualTo(destinationBefore);
        assertThat(source.getQuantityAvailable()).isEqualTo(sourceBefore);
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(tenant.getCode()))
            .isEqualTo(orderCountBefore);

        source.setQuantityOnHand(11L);
        source.setQuantityAvailable(11L);
        inventoryRepository.saveAndFlush(source);
        recommendationService.createForInventory(source,
            new InventoryInsight(false, false, false, false, AlertSeverity.MEDIUM, "LOW", "Stable."),
            prediction, "phase2-source-change");

        Recommendation retiredTransfer = recommendationRepository.findById(transfer.getId()).orElseThrow();
        assertThat(retiredTransfer.getStatus()).isEqualTo(RecommendationStatus.RETIRED);

        source.setQuantityOnHand(30L);
        source.setQuantityAvailable(30L);
        inventoryRepository.saveAndFlush(source);
        Recommendation recurringTransfer = recommendationService.createForInventory(
            destination, normalLowStock, prediction, "phase2-transfer-recur");
        assertThat(recurringTransfer.getType()).isEqualTo(RecommendationType.TRANSFER_STOCK);
        assertThat(recurringTransfer.getId()).isNotEqualTo(transfer.getId());
    }

    @Test
    void fulfillmentRecommendationsFollowAnomalyDelayBacklogPrecedenceAndLifecycle() {
        Tenant tenant = tenant("REC-P2-" + UUID.randomUUID());
        Warehouse warehouse = warehouse(tenant, "WH-NORTH");
        CustomerOrder order = customerOrder(tenant, warehouse, "ORDER-REC-P2-" + UUID.randomUUID());
        FulfillmentTask task = fulfillmentTask(tenant, warehouse, order);

        FulfillmentAssessment backlog = assessment(true, false, false);
        FulfillmentAssessment delay = assessment(false, true, false);
        FulfillmentAssessment anomaly = assessment(false, false, true);
        FulfillmentAssessment clear = assessment(false, false, false);

        Recommendation prioritized = recommendationService.createForFulfillment(task, backlog, "phase2-test");
        assertThat(prioritized.getType()).isEqualTo(RecommendationType.PRIORITIZE_FULFILLMENT);

        Recommendation escalated = recommendationService.createForFulfillment(task, delay, "phase2-test");
        assertThat(escalated.getId()).isEqualTo(prioritized.getId());
        assertThat(escalated.getType()).isEqualTo(RecommendationType.ESCALATE_LOGISTICS);

        Recommendation investigated = recommendationService.createForFulfillment(task, anomaly, "phase2-test");
        assertThat(investigated.getId()).isEqualTo(prioritized.getId());
        assertThat(investigated.getType()).isEqualTo(RecommendationType.INVESTIGATE_LOGISTICS_ANOMALY);

        Recommendation retired = recommendationService.createForFulfillment(task, clear, "phase2-test");
        assertThat(retired.getId()).isEqualTo(prioritized.getId());
        assertThat(retired.getStatus()).isEqualTo(RecommendationStatus.RETIRED);

        Recommendation recurring = recommendationService.createForFulfillment(task, backlog, "phase2-test");
        assertThat(recurring.getId()).isNotEqualTo(prioritized.getId());
        assertThat(recurring.getType()).isEqualTo(RecommendationType.PRIORITIZE_FULFILLMENT);
    }

    @Test
    void reconciliationRebuildsAdvisoryStateFromCommittedInventoryTruth() {
        Tenant tenant = tenant("REC-P2-" + UUID.randomUUID());
        Warehouse warehouse = warehouse(tenant, "WH-NORTH");
        Product product = product(tenant, "SKU-RECON-" + UUID.randomUUID());
        Inventory inventory = inventory(warehouse, product, 7, 10);

        reconciliationService.reconcileNow();
        Recommendation recommendation = recommendationRepository
            .findAllByTenant_CodeIgnoreCaseAndStatusOrderByUpdatedAtDesc(tenant.getCode(), RecommendationStatus.CURRENT)
            .stream()
            .filter(candidate -> candidate.getProduct() != null
                && candidate.getProduct().getId().equals(product.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(recommendation.getType()).isEqualTo(RecommendationType.REORDER_STOCK);

        inventory.setQuantityOnHand(20L);
        inventory.setQuantityAvailable(20L);
        inventoryRepository.saveAndFlush(inventory);
        reconciliationService.reconcileNow();

        assertThat(recommendationRepository.findById(recommendation.getId()).orElseThrow().getStatus())
            .isEqualTo(RecommendationStatus.RETIRED);
    }

    private Tenant tenant(String code) {
        return tenantRepository.save(Tenant.builder().code(code).name("Recommendation Phase 2").build());
    }

    private Warehouse warehouse(Tenant tenant, String code) {
        return warehouseRepository.save(Warehouse.builder()
            .tenant(tenant).code(code).name(code).location(code + " location").build());
    }

    private Product product(Tenant tenant, String sku) {
        return productRepository.save(Product.builder()
            .tenant(tenant).catalogSku(sku).name("Recommendation item").category("Test").build());
    }

    private Inventory inventory(Warehouse warehouse, Product product, long quantity, long threshold) {
        return inventoryRepository.save(Inventory.builder()
            .warehouse(warehouse).product(product).quantityOnHand(quantity).quantityReserved(0L)
            .quantityAvailable(quantity).reorderThreshold(threshold).build());
    }

    private CustomerOrder customerOrder(Tenant tenant, Warehouse warehouse, String externalId) {
        return customerOrderRepository.save(CustomerOrder.builder()
            .tenant(tenant).warehouse(warehouse).externalOrderId(externalId).status(OrderStatus.CREATED)
            .totalAmount(BigDecimal.ZERO).build());
    }

    private FulfillmentTask fulfillmentTask(Tenant tenant, Warehouse warehouse, CustomerOrder order) {
        return fulfillmentTaskRepository.save(FulfillmentTask.builder()
            .tenant(tenant).warehouse(warehouse).customerOrder(order).status(FulfillmentStatus.QUEUED)
            .totalUnits(4).fulfilledUnits(0).build());
    }

    private FulfillmentAssessment assessment(boolean backlog, boolean delay, boolean anomaly) {
        return new FulfillmentAssessment(
            backlog ? 4 : 0,
            backlog ? 1 : 0,
            delay ? 2 : 0,
            backlog ? 1d : 0d,
            backlog ? 4d : null,
            null,
            delay ? -2d : null,
            backlog,
            delay,
            anomaly,
            anomaly ? AlertSeverity.CRITICAL : AlertSeverity.HIGH,
            anomaly ? "CRITICAL" : "HIGH",
            "Phase 2 recommendation assessment");
    }

    @Autowired
    private com.synapsecore.domain.repository.FulfillmentTaskRepository fulfillmentTaskRepository;
}
