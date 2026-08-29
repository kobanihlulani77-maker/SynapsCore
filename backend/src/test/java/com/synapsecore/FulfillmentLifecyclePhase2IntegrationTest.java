package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.domain.entity.Alert;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.AlertType;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderItem;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationType;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FulfillmentLifecyclePhase2IntegrationTest {

    private static final String TENANT = "STARTER-OPS";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void concurrentDifferentSkuDispatchesPreserveEveryOrderLine() throws Exception {
        String firstSku = sku("DIFFERENT-A");
        String secondSku = sku("DIFFERENT-B");
        createInventory(firstSku, 5);
        createInventory(secondSku, 5);
        String orderId = createOrder("DIFFERENT-SKU", List.of(line(firstSku, 5), line(secondSku, 5)));

        List<Integer> statuses = runConcurrently(List.of(
            () -> fulfillmentRequest(orderId, "DISPATCHED", 5, firstSku, "phase2-different-a-" + System.nanoTime()),
            () -> fulfillmentRequest(orderId, "DISPATCHED", 5, secondSku, "phase2-different-b-" + System.nanoTime())
        ));

        assertThat(statuses).containsExactly(200, 200);
        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder order = loadOrderInTransaction(orderId);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FULFILLED);
            assertThat(itemFor(order, firstSku).getFulfilledQuantity()).isEqualTo(5);
            assertThat(itemFor(order, firstSku).getReservedQuantity()).isZero();
            assertThat(itemFor(order, secondSku).getFulfilledQuantity()).isEqualTo(5);
            assertThat(itemFor(order, secondSku).getReservedQuantity()).isZero();
            FulfillmentTask task = loadTaskInTransaction(orderId);
            assertThat(task.getStatus()).isEqualTo(FulfillmentStatus.DISPATCHED);
            assertThat(task.getFulfilledUnits()).isEqualTo(10);
            assertInventoryInTransaction(firstSku, 0, 0);
            assertInventoryInTransaction(secondSku, 0, 0);
        });
    }

    @Test
    void concurrentSameSkuPartialDispatchesAccumulateWithoutLostUpdates() throws Exception {
        String productSku = sku("SAME-SKU");
        createInventory(productSku, 10);
        String orderId = createOrder("SAME-SKU", List.of(line(productSku, 10)));

        List<Integer> partialStatuses = runConcurrently(List.of(
            () -> fulfillmentRequest(orderId, "DISPATCHED", 3, productSku, "phase2-same-3-" + System.nanoTime()),
            () -> fulfillmentRequest(orderId, "DISPATCHED", 4, productSku, "phase2-same-4-" + System.nanoTime())
        ));

        assertThat(partialStatuses).containsExactly(200, 200);
        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder order = loadOrderInTransaction(orderId);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FULFILLED);
            assertThat(itemFor(order, productSku).getFulfilledQuantity()).isEqualTo(7);
            assertThat(itemFor(order, productSku).getReservedQuantity()).isEqualTo(3);
            assertThat(loadTaskInTransaction(orderId).getFulfilledUnits()).isEqualTo(7);
            assertInventoryInTransaction(productSku, 3, 3);
        });

        assertThat(fulfillmentRequest(orderId, "DISPATCHED", 3, productSku, "phase2-same-final-" + System.nanoTime()))
            .isEqualTo(200);
        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder order = loadOrderInTransaction(orderId);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FULFILLED);
            assertThat(itemFor(order, productSku).getFulfilledQuantity()).isEqualTo(10);
            assertThat(itemFor(order, productSku).getReservedQuantity()).isZero();
            assertThat(loadTaskInTransaction(orderId).getFulfilledUnits()).isEqualTo(10);
            assertInventoryInTransaction(productSku, 0, 0);
        });
    }

    @Test
    void dispatchAndCancelRaceLeavesOneCoherentCancelledOutcome() throws Exception {
        String productSku = sku("CANCEL-RACE");
        createInventory(productSku, 10);
        String orderId = createOrder("CANCEL-RACE", List.of(line(productSku, 10)));

        List<Integer> statuses = runConcurrently(List.of(
            () -> fulfillmentRequest(orderId, "DISPATCHED", 5, productSku, "phase2-cancel-dispatch-" + System.nanoTime()),
            () -> transitionOrder(orderId, "CANCELLED")
        ));

        assertThat(statuses).contains(200);
        assertThat(statuses).allSatisfy(value -> assertThat(value).isIn(200, 400, 409));
        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder order = loadOrderInTransaction(orderId);
            OrderItem item = itemFor(order, productSku);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(item.getReservedQuantity()).isZero();
            assertThat(item.getFulfilledQuantity() + item.getCancelledQuantity()).isEqualTo(10);
            assertInventoryInTransaction(productSku, 10 - item.getFulfilledQuantity(), 0);
        });
    }

    @Test
    void dispatchAndExceptionRacePreservesConsumedUnitsAndReleasesOnlyRemainder() throws Exception {
        String productSku = sku("EXCEPTION-RACE");
        createInventory(productSku, 10);
        String orderId = createOrder("EXCEPTION-RACE", List.of(line(productSku, 10)));

        List<Integer> statuses = runConcurrently(List.of(
            () -> fulfillmentRequest(orderId, "DISPATCHED", 5, productSku, "phase2-exception-dispatch-" + System.nanoTime()),
            () -> fulfillmentRequest(orderId, "EXCEPTION", null, null, "phase2-exception-" + System.nanoTime())
        ));

        assertThat(statuses).contains(200);
        assertThat(statuses).allSatisfy(value -> assertThat(value).isIn(200, 400, 409));
        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder order = loadOrderInTransaction(orderId);
            OrderItem item = itemFor(order, productSku);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
            assertThat(loadTaskInTransaction(orderId).getStatus()).isEqualTo(FulfillmentStatus.EXCEPTION);
            assertThat(item.getReservedQuantity()).isZero();
            assertThat(item.getFulfilledQuantity() + item.getCancelledQuantity()).isEqualTo(10);
            assertInventoryInTransaction(productSku, 10 - item.getFulfilledQuantity(), 0);
        });
    }

    @Test
    void deliveryAndPartialDispatchRaceProducesWholeOrderDeliveryWithoutOverconsumption() throws Exception {
        String firstSku = sku("DELIVERY-RACE-A");
        String secondSku = sku("DELIVERY-RACE-B");
        createInventory(firstSku, 5);
        createInventory(secondSku, 5);
        String orderId = createOrder("DELIVERY-RACE", List.of(line(firstSku, 5), line(secondSku, 5)));

        List<Integer> statuses = runConcurrently(List.of(
            () -> fulfillmentRequest(orderId, "DISPATCHED", 5, firstSku, "phase2-delivery-partial-" + System.nanoTime()),
            () -> fulfillmentRequest(orderId, "DELIVERED", null, null, "phase2-delivery-whole-" + System.nanoTime())
        ));

        assertThat(statuses).contains(200);
        assertThat(statuses).allSatisfy(value -> assertThat(value).isIn(200, 400, 409));
        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder order = loadOrderInTransaction(orderId);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
            assertThat(loadTaskInTransaction(orderId).getStatus()).isEqualTo(FulfillmentStatus.DELIVERED);
            assertThat(loadTaskInTransaction(orderId).getFulfilledUnits()).isEqualTo(10);
            assertThat(itemFor(order, firstSku).getFulfilledQuantity()).isEqualTo(5);
            assertThat(itemFor(order, firstSku).getReservedQuantity()).isZero();
            assertThat(itemFor(order, secondSku).getFulfilledQuantity()).isEqualTo(5);
            assertThat(itemFor(order, secondSku).getReservedQuantity()).isZero();
            assertInventoryInTransaction(firstSku, 0, 0);
            assertInventoryInTransaction(secondSku, 0, 0);
        });
    }

    @Test
    void delayAndDispatchRaceRemainsAValidDelayedOrRejectedRecoveryState() throws Exception {
        String productSku = sku("DELAY-RACE");
        createInventory(productSku, 10);
        String orderId = createOrder("DELAY-RACE", List.of(line(productSku, 10)));

        List<Integer> statuses = runConcurrently(List.of(
            () -> fulfillmentRequest(orderId, "DELAYED", null, null, "phase2-delay-" + System.nanoTime()),
            () -> fulfillmentRequest(orderId, "DISPATCHED", 10, productSku, "phase2-delay-dispatch-" + System.nanoTime())
        ));

        assertThat(statuses).contains(200);
        assertThat(statuses).allSatisfy(value -> assertThat(value).isIn(200, 400, 409));
        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder order = loadOrderInTransaction(orderId);
            FulfillmentTask task = loadTaskInTransaction(orderId);
            assertThat(task.getStatus()).isEqualTo(FulfillmentStatus.DELAYED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.BLOCKED);
            assertThat(itemFor(order, productSku).getFulfilledQuantity()).isIn(0, 10);
            assertThat(itemFor(order, productSku).getReservedQuantity()).isEqualTo(10 - itemFor(order, productSku).getFulfilledQuantity());
        });
    }

    @Test
    void exceptionAndDeliveryRaceHasExactlyOneTerminalOperationalTruth() throws Exception {
        String productSku = sku("TERMINAL-RACE");
        createInventory(productSku, 10);
        String orderId = createOrder("TERMINAL-RACE", List.of(line(productSku, 10)));

        List<Integer> statuses = runConcurrently(List.of(
            () -> fulfillmentRequest(orderId, "EXCEPTION", null, null, "phase2-terminal-exception-" + System.nanoTime()),
            () -> fulfillmentRequest(orderId, "DELIVERED", null, null, "phase2-terminal-delivery-" + System.nanoTime())
        ));

        assertThat(statuses).contains(200);
        assertThat(statuses).allSatisfy(value -> assertThat(value).isIn(200, 400, 409));
        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder order = loadOrderInTransaction(orderId);
            FulfillmentTask task = loadTaskInTransaction(orderId);
            OrderItem item = itemFor(order, productSku);
            assertThat(item.getReservedQuantity()).isZero();
            assertThat(item.getFulfilledQuantity() + item.getCancelledQuantity()).isEqualTo(10);
            if (task.getStatus() == FulfillmentStatus.EXCEPTION) {
                assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
                assertThat(item.getFulfilledQuantity()).isZero();
                assertThat(item.getCancelledQuantity()).isEqualTo(10);
                assertInventoryInTransaction(productSku, 10, 0);
            } else {
                assertThat(task.getStatus()).isEqualTo(FulfillmentStatus.DELIVERED);
                assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
                assertThat(item.getFulfilledQuantity()).isEqualTo(10);
                assertThat(item.getCancelledQuantity()).isZero();
                assertInventoryInTransaction(productSku, 0, 0);
            }
        });
    }

    @Test
    void differentOrdersSharingInventoryCommitWithoutOversell() throws Exception {
        String productSku = sku("SHARED-INVENTORY");
        createInventory(productSku, 10);
        String firstOrder = createOrder("SHARED-A", List.of(line(productSku, 5)));
        String secondOrder = createOrder("SHARED-B", List.of(line(productSku, 5)));

        List<Integer> statuses = runConcurrently(List.of(
            () -> fulfillmentRequest(firstOrder, "DISPATCHED", 5, productSku, "phase2-shared-a-" + System.nanoTime()),
            () -> fulfillmentRequest(secondOrder, "DISPATCHED", 5, productSku, "phase2-shared-b-" + System.nanoTime())
        ));

        assertThat(statuses).containsExactly(200, 200);
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(loadOrderInTransaction(firstOrder).getStatus()).isEqualTo(OrderStatus.FULFILLED);
            assertThat(loadOrderInTransaction(secondOrder).getStatus()).isEqualTo(OrderStatus.FULFILLED);
            assertInventoryInTransaction(productSku, 0, 0);
        });
    }

    @Test
    void fulfillmentSignalsDeduplicateAndExposeOverdueAnomalyTruth() throws Exception {
        String productSku = sku("SIGNALS");
        createInventory(productSku, 2);
        String orderId = createOrder("SIGNALS", List.of(line(productSku, 2)));
        transactionTemplate.executeWithoutResult(status -> {
            FulfillmentTask task = loadTaskInTransaction(orderId);
            task.setPromisedDispatchAt(Instant.now().minusSeconds(60));
            fulfillmentTaskRepository.save(task);
        });

        mockMvc.perform(get("/api/fulfillment")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", TENANT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overdueDispatchCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        String firstRequestId = "phase2-signal-exception-" + System.nanoTime();
        mockMvc.perform(post("/api/fulfillment/updates")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", TENANT)
                .header("X-Request-Id", firstRequestId)
                .contentType(APPLICATION_JSON)
                .content(fulfillmentBody(orderId, "EXCEPTION", null, null, null, null, "signal exception")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anomalyDetected").value(true))
            .andExpect(jsonPath("$.warehouseCode").value("WH-NORTH"));

        String title = "Logistics anomaly detected in WH-NORTH";
        String recommendationTitle = "Investigate logistics anomaly in WH-NORTH";
        List<Alert> anomalyAlerts = alertRepository.findAll().stream()
            .filter(alert -> alert.getType() == AlertType.FULFILLMENT_ANOMALY)
            .filter(alert -> alert.getStatus() == AlertStatus.ACTIVE)
            .filter(alert -> title.equals(alert.getTitle()))
            .toList();
        List<Recommendation> anomalyRecommendations = recommendationRepository.findAll().stream()
            .filter(recommendation -> recommendation.getType() == RecommendationType.INVESTIGATE_LOGISTICS_ANOMALY)
            .filter(recommendation -> recommendationTitle.equals(recommendation.getTitle()))
            .toList();
        assertThat(anomalyAlerts).hasSize(1);
        assertThat(anomalyRecommendations).isNotEmpty();
        int recommendationCountAfterFirstAnomaly = anomalyRecommendations.size();

        mockMvc.perform(post("/api/fulfillment/updates")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", TENANT)
                .header("X-Request-Id", "phase2-signal-exception-repeat-" + System.nanoTime())
                .contentType(APPLICATION_JSON)
                .content(fulfillmentBody(orderId, "EXCEPTION", null, null, null, null, "signal exception")))
            .andExpect(status().isOk());

        assertThat(recommendationRepository.findAll().stream()
            .filter(recommendation -> recommendation.getType() == RecommendationType.INVESTIGATE_LOGISTICS_ANOMALY)
            .filter(recommendation -> recommendationTitle.equals(recommendation.getTitle()))
            .toList()).hasSize(recommendationCountAfterFirstAnomaly);
    }

    @Test
    void carrierAndTrackingUpdatesRemainLatestStateEvidenceAcrossPartialDispatches() throws Exception {
        String productSku = sku("CARRIER");
        createInventory(productSku, 10);
        String orderId = createOrder("CARRIER", List.of(line(productSku, 10)));

        assertThat(fulfillmentRequest(orderId, "DISPATCHED", 4, productSku, "phase2-carrier-a-" + System.nanoTime(), "Carrier A", "TRACK-A"))
            .isEqualTo(200);
        assertThat(fulfillmentRequest(orderId, "DISPATCHED", 6, productSku, "phase2-carrier-b-" + System.nanoTime(), "Carrier B", "TRACK-B"))
            .isEqualTo(200);

        transactionTemplate.executeWithoutResult(status -> {
            FulfillmentTask task = loadTaskInTransaction(orderId);
            assertThat(task.getStatus()).isEqualTo(FulfillmentStatus.DISPATCHED);
            assertThat(task.getFulfilledUnits()).isEqualTo(10);
            assertThat(task.getCarrier()).isEqualTo("Carrier B");
            assertThat(task.getTrackingReference()).isEqualTo("TRACK-B");
            assertThat(loadOrderInTransaction(orderId).getStatus()).isEqualTo(OrderStatus.FULFILLED);
            assertInventoryInTransaction(productSku, 0, 0);
        });
    }

    private String createOrder(String label, List<String> lines) throws Exception {
        String orderId = "FULFILL-P2-" + label + "-" + System.nanoTime();
        String body = """
            {"externalOrderId":"%s","warehouseCode":"WH-NORTH","items":[%s]}
            """.formatted(orderId, String.join(",", lines));
        mockMvc.perform(post("/api/orders")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", TENANT)
                .contentType(APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());
        return orderId;
    }

    private void createInventory(String productSku, long quantity) {
        transactionTemplate.executeWithoutResult(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            Product product = productRepository.save(Product.builder()
                .tenant(warehouse.getTenant())
                .catalogSku(productSku)
                .name("Fulfillment Phase 2 Product")
                .category("Verification")
                .build());
            inventoryRepository.save(Inventory.builder()
                .tenant(warehouse.getTenant())
                .product(product)
                .warehouse(warehouse)
                .quantityOnHand(quantity)
                .quantityReserved(0L)
                .quantityInbound(0L)
                .quantityAvailable(quantity)
                .reorderThreshold(0L)
                .build());
        });
    }

    private String line(String productSku, int quantity) {
        return "{\"productSku\":\"%s\",\"quantity\":%d,\"unitPrice\":10.00}".formatted(productSku, quantity);
    }

    private int fulfillmentRequest(String orderId,
                                   String fulfillmentStatus,
                                   Integer fulfilledUnits,
                                   String fulfilledProductSku,
                                   String requestId) throws Exception {
        return fulfillmentRequest(orderId, fulfillmentStatus, fulfilledUnits, fulfilledProductSku, requestId, null, null);
    }

    private int fulfillmentRequest(String orderId,
                                   String fulfillmentStatus,
                                   Integer fulfilledUnits,
                                   String fulfilledProductSku,
                                   String requestId,
                                   String carrier,
                                   String trackingReference) throws Exception {
        return mockMvc.perform(post("/api/fulfillment/updates")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", TENANT)
                .header("X-Request-Id", requestId)
                .contentType(APPLICATION_JSON)
                .content(fulfillmentBody(orderId, fulfillmentStatus, fulfilledUnits, fulfilledProductSku,
                    carrier, trackingReference, "Fulfillment Phase 2 proof")))
            .andReturn()
            .getResponse()
            .getStatus();
    }

    private String fulfillmentBody(String orderId,
                                   String fulfillmentStatus,
                                   Integer fulfilledUnits,
                                   String fulfilledProductSku,
                                   String carrier,
                                   String trackingReference,
                                   String note) {
        String quantity = fulfilledUnits == null ? "null" : fulfilledUnits.toString();
        String sku = fulfilledProductSku == null ? "null" : "\"" + fulfilledProductSku + "\"";
        String carrierJson = carrier == null ? "null" : "\"" + carrier + "\"";
        String trackingJson = trackingReference == null ? "null" : "\"" + trackingReference + "\"";
        return """
            {"externalOrderId":"%s","status":"%s","fulfilledUnits":%s,"fulfilledProductSku":%s,"carrier":%s,"trackingReference":%s,"note":"%s"}
            """.formatted(orderId, fulfillmentStatus, quantity, sku, carrierJson, trackingJson, note);
    }

    private int transitionOrder(String orderId, String orderStatus) throws Exception {
        return mockMvc.perform(post("/api/orders/" + orderId + "/transition")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", TENANT)
                .contentType(APPLICATION_JSON)
                .content("{\"status\":\"%s\",\"note\":\"Fulfillment Phase 2 proof\"}".formatted(orderStatus)))
            .andReturn()
            .getResponse()
            .getStatus();
    }

    private List<Integer> runConcurrently(List<Callable<Integer>> operations) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(operations.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = operations.stream()
            .map(operation -> executor.submit(() -> {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Concurrency proof did not start all operations.");
                }
                return operation.call();
            }))
            .toList();
        start.countDown();
        try {
            return futures.stream().map(future -> {
                try {
                    return future.get(20, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new AssertionError("Concurrent fulfillment operation did not complete cleanly.", exception);
                }
            }).toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private CustomerOrder loadOrderInTransaction(String orderId) {
        return customerOrderRepository
            .findByTenant_CodeIgnoreCaseAndExternalOrderId(TENANT, orderId)
            .orElseThrow();
    }

    private FulfillmentTask loadTaskInTransaction(String orderId) {
        return fulfillmentTaskRepository
            .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(TENANT, orderId)
            .orElseThrow();
    }

    private OrderItem itemFor(CustomerOrder order, String productSku) {
        return order.getItems().stream()
            .filter(item -> productSku.equalsIgnoreCase(item.getProduct().resolveCatalogSku()))
            .findFirst()
            .orElseThrow();
    }

    private void assertInventoryInTransaction(String productSku, long onHand, long reserved) {
        var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
        Product product = productRepository
            .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(TENANT, productSku)
            .orElseThrow();
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).orElseThrow();
        assertThat(inventory.getQuantityOnHand()).isEqualTo(onHand);
        assertThat(inventory.getQuantityReserved()).isEqualTo(reserved);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(onHand - reserved);
    }

    private String sku(String label) {
        return "SKU-FULFILL-P2-" + label + "-" + System.nanoTime();
    }

    private static RequestPostProcessor accessHeaders(String actorName, String roles) {
        return request -> {
            request.addHeader("X-Synapse-Actor", actorName);
            request.addHeader("X-Synapse-Roles", roles);
            return request;
        };
    }
}
