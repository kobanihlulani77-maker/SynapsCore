package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
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
class OrderLifecyclePhase2IntegrationTest {

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
    private TransactionTemplate transactionTemplate;

    @Test
    void partialDispatchKeepsOrderFulfillmentAndInventoryAligned() throws Exception {
        String externalOrderId = "ORDER-P2-PARTIAL-" + System.nanoTime();
        String productSku = "SKU-ORDER-P2-PARTIAL-" + System.nanoTime();
        createInventory(productSku, 10L);
        createOrder(externalOrderId, productSku, 10);

        fulfillmentRequest(externalOrderId, "DISPATCHED", 4).andExpect(status().isOk());

        CustomerOrder order = loadOrder(externalOrderId);
        Inventory inventory = loadInventory(productSku);
        FulfillmentTask task = loadTask(externalOrderId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FULFILLED);
        assertThat(order.getItems().get(0).getFulfilledQuantity()).isEqualTo(4);
        assertThat(order.getItems().get(0).getReservedQuantity()).isEqualTo(6);
        assertThat(task.getFulfilledUnits()).isEqualTo(4);
        assertThat(inventory.getQuantityOnHand()).isEqualTo(6L);
        assertThat(inventory.getQuantityReserved()).isEqualTo(6L);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(0L);
    }

    @Test
    void failureAfterPartialFulfillmentReleasesOnlyOutstandingReservation() throws Exception {
        String externalOrderId = "ORDER-P2-FAILURE-" + System.nanoTime();
        String productSku = "SKU-ORDER-P2-FAILURE-" + System.nanoTime();
        createInventory(productSku, 10L);
        createOrder(externalOrderId, productSku, 10);
        fulfillmentRequest(externalOrderId, "DISPATCHED", 4).andExpect(status().isOk());

        transitionOrder(externalOrderId, "FAILED")
            .andExpect(status().isOk());

        CustomerOrder order = loadOrder(externalOrderId);
        Inventory inventory = loadInventory(productSku);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(order.getItems().get(0).getFulfilledQuantity()).isEqualTo(4);
        assertThat(order.getItems().get(0).getCancelledQuantity()).isEqualTo(6);
        assertThat(order.getItems().get(0).getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getQuantityOnHand()).isEqualTo(6L);
        assertThat(inventory.getQuantityReserved()).isEqualTo(0L);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(6L);
    }

    @Test
    void cancellationAfterPartialFulfillmentReleasesOnlyOutstandingReservation() throws Exception {
        String externalOrderId = "ORDER-P2-CANCEL-PARTIAL-" + System.nanoTime();
        String productSku = "SKU-ORDER-P2-CANCEL-PARTIAL-" + System.nanoTime();
        createInventory(productSku, 10L);
        createOrder(externalOrderId, productSku, 10);
        fulfillmentRequest(externalOrderId, "DISPATCHED", 4).andExpect(status().isOk());

        transitionOrder(externalOrderId, "CANCELLED")
            .andExpect(status().isOk());

        CustomerOrder order = loadOrder(externalOrderId);
        Inventory inventory = loadInventory(productSku);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getItems().get(0).getFulfilledQuantity()).isEqualTo(4);
        assertThat(order.getItems().get(0).getCancelledQuantity()).isEqualTo(6);
        assertThat(order.getItems().get(0).getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getQuantityOnHand()).isEqualTo(6L);
        assertThat(inventory.getQuantityReserved()).isEqualTo(0L);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(6L);
    }

    @Test
    void terminalOrderCannotBeReopenedByManualTransition() throws Exception {
        String externalOrderId = "ORDER-P2-TERMINAL-" + System.nanoTime();
        String productSku = "SKU-ORDER-P2-TERMINAL-" + System.nanoTime();
        createInventory(productSku, 2L);
        createOrder(externalOrderId, productSku, 1);

        transitionOrder(externalOrderId, "CANCELLED")
            .andExpect(status().isOk());
        transitionOrder(externalOrderId, "PROCESSING")
            .andExpect(status().isBadRequest());

        assertThat(loadOrder(externalOrderId).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(loadInventory(productSku).getQuantityReserved()).isEqualTo(0L);
    }

    @Test
    void cancelAndFulfillRaceLeavesOneCoherentOrderAndInventoryTruth() throws Exception {
        String externalOrderId = "ORDER-P2-CANCEL-RACE-" + System.nanoTime();
        String productSku = "SKU-ORDER-P2-CANCEL-RACE-" + System.nanoTime();
        createInventory(productSku, 2L);
        createOrder(externalOrderId, productSku, 2);

        List<Integer> statuses = runConcurrently(
            () -> transitionOrder(externalOrderId, "CANCELLED").andReturn().getResponse().getStatus(),
            () -> fulfillmentRequest(externalOrderId, "DISPATCHED", 1).andReturn().getResponse().getStatus()
        );

        assertThat(statuses).allMatch(status -> status == 200 || status == 400);
        assertThat(statuses).contains(200);
        CustomerOrder order = loadOrder(externalOrderId);
        Inventory inventory = loadInventory(productSku);
        assertThat(order.getStatus()).isIn(OrderStatus.CANCELLED, OrderStatus.PARTIALLY_FULFILLED);
        assertThat(order.getItems().get(0).getFulfilledQuantity()
            + order.getItems().get(0).getCancelledQuantity()
            + order.getItems().get(0).getReservedQuantity()).isEqualTo(2);
        assertThat(inventory.getQuantityReserved()).isEqualTo(order.getItems().get(0).getReservedQuantity().longValue());
        assertThat(inventory.getQuantityOnHand()
            + order.getItems().get(0).getFulfilledQuantity())
            .as("onHand=%s fulfilled=%s cancelled=%s reserved=%s",
                inventory.getQuantityOnHand(),
                order.getItems().get(0).getFulfilledQuantity(),
                order.getItems().get(0).getCancelledQuantity(),
                order.getItems().get(0).getReservedQuantity())
            .isEqualTo(2L);
    }

    @Test
    void failureAndFulfillRaceCannotReopenFailedOrder() throws Exception {
        String externalOrderId = "ORDER-P2-FAIL-RACE-" + System.nanoTime();
        String productSku = "SKU-ORDER-P2-FAIL-RACE-" + System.nanoTime();
        createInventory(productSku, 2L);
        createOrder(externalOrderId, productSku, 2);

        List<Integer> statuses = runConcurrently(
            () -> transitionOrder(externalOrderId, "FAILED").andReturn().getResponse().getStatus(),
            () -> fulfillmentRequest(externalOrderId, "DISPATCHED", 1).andReturn().getResponse().getStatus()
        );

        assertThat(statuses).allMatch(status -> status == 200 || status == 400);
        assertThat(statuses).contains(200);
        CustomerOrder order = loadOrder(externalOrderId);
        Inventory inventory = loadInventory(productSku);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(order.getItems().get(0).getFulfilledQuantity()
            + order.getItems().get(0).getCancelledQuantity()
            + order.getItems().get(0).getReservedQuantity()).isEqualTo(2);
        assertThat(inventory.getQuantityReserved()).isEqualTo(0L);
        assertThat(inventory.getQuantityOnHand()
            + order.getItems().get(0).getFulfilledQuantity())
            .as("onHand=%s fulfilled=%s cancelled=%s reserved=%s",
                inventory.getQuantityOnHand(),
                order.getItems().get(0).getFulfilledQuantity(),
                order.getItems().get(0).getCancelledQuantity(),
                order.getItems().get(0).getReservedQuantity())
            .isEqualTo(2L);
    }

    private void createInventory(String productSku, long quantityOnHand) {
        transactionTemplate.executeWithoutResult(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            Product product = productRepository.save(Product.builder()
                .tenant(warehouse.getTenant())
                .catalogSku(productSku)
                .name("Order Phase 2 Product")
                .category("Verification")
                .build());
            inventoryRepository.save(Inventory.builder()
                .tenant(warehouse.getTenant())
                .product(product)
                .warehouse(warehouse)
                .quantityOnHand(quantityOnHand)
                .quantityReserved(0L)
                .quantityInbound(0L)
                .quantityAvailable(quantityOnHand)
                .reorderThreshold(0L)
                .build());
        });
    }

    private void createOrder(String externalOrderId, String productSku, int quantity) throws Exception {
        String body = """
            {
              "externalOrderId":"%s",
              "warehouseCode":"WH-NORTH",
              "items":[{"productSku":"%s","quantity":%d,"unitPrice":10.00}]
            }
            """.formatted(externalOrderId, productSku, quantity);
        mockMvc.perform(post("/api/orders")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", "STARTER-OPS")
                .contentType(APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions fulfillmentRequest(
            String externalOrderId, String fulfillmentStatus, Integer fulfilledUnits) throws Exception {
        String body = """
            {"externalOrderId":"%s","status":"%s","fulfilledUnits":%s,"note":"Order Phase 2 proof"}
            """.formatted(externalOrderId, fulfillmentStatus, fulfilledUnits);
        return mockMvc.perform(post("/api/fulfillment/updates")
            .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
            .header("X-Synapse-Tenant", "STARTER-OPS")
            .contentType(APPLICATION_JSON)
            .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions transitionOrder(
            String externalOrderId, String orderStatus) throws Exception {
        return mockMvc.perform(post("/api/orders/" + externalOrderId + "/transition")
            .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
            .header("X-Synapse-Tenant", "STARTER-OPS")
            .contentType(APPLICATION_JSON)
            .content("{\"status\":\"%s\",\"note\":\"Order Phase 2 proof\"}".formatted(orderStatus)));
    }

    private CustomerOrder loadOrder(String externalOrderId) {
        return transactionTemplate.execute(status -> customerOrderRepository
            .findByTenant_CodeIgnoreCaseAndExternalOrderId("STARTER-OPS", externalOrderId)
            .orElseThrow());
    }

    private Inventory loadInventory(String productSku) {
        return transactionTemplate.execute(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            var product = productRepository
                .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase("STARTER-OPS", productSku)
                .orElseThrow();
            return inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).orElseThrow();
        });
    }

    private FulfillmentTask loadTask(String externalOrderId) {
        return transactionTemplate.execute(status -> fulfillmentTaskRepository
            .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId("STARTER-OPS", externalOrderId)
            .orElseThrow());
    }

    private List<Integer> runConcurrently(Callable<Integer> first, Callable<Integer> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = List.of(
            executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return first.call();
            }),
            executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return second.call();
            })
        );
        start.countDown();
        try {
            return futures.stream().map(future -> {
                try {
                    return future.get(15, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new AssertionError("Concurrent Order Phase 2 action did not complete cleanly.", exception);
                }
            }).toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private static RequestPostProcessor accessHeaders(String actorName, String roles) {
        return request -> {
            request.addHeader("X-Synapse-Actor", actorName);
            request.addHeader("X-Synapse-Roles", roles);
            return request;
        };
    }
}
