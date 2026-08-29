package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.domain.entity.Inventory;
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
import java.util.stream.IntStream;
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
class InventoryConcurrencyIntegrationTest {

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
    void concurrentReservationsDoNotOversellSingleAvailableUnit() throws Exception {
        String productSku = "SKU-CONC-" + System.nanoTime();
        transactionTemplate.executeWithoutResult(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            Product product = productRepository.save(Product.builder()
                .tenant(warehouse.getTenant())
                .catalogSku(productSku)
                .name("Concurrency Proof Module")
                .category("Verification")
                .build());
            inventoryRepository.save(Inventory.builder()
                .tenant(warehouse.getTenant())
                .product(product)
                .warehouse(warehouse)
                .quantityOnHand(1L)
                .quantityReserved(0L)
                .quantityInbound(0L)
                .quantityAvailable(1L)
                .reorderThreshold(0L)
                .build());
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Integer>> attempts = IntStream.rangeClosed(1, 2)
            .mapToObj(attempt -> (Callable<Integer>) () -> {
                start.await(5, TimeUnit.SECONDS);
                String requestBody = """
                    {
                      "externalOrderId": "CONC-%d-%d",
                      "warehouseCode": "WH-NORTH",
                      "items": [
                        {
                          "productSku": "%s",
                          "quantity": 1,
                          "unitPrice": 95.00
                        }
                      ]
                    }
                    """.formatted(System.nanoTime(), attempt, productSku);

                return mockMvc.perform(post("/api/orders")
                        .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                        .header("X-Synapse-Tenant", "STARTER-OPS")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            })
            .toList();

        List<Future<Integer>> futures = attempts.stream().map(executor::submit).toList();
        start.countDown();
        List<Integer> statuses;
        try {
            statuses = futures.stream()
                .map(future -> {
                    try {
                        return future.get(10, TimeUnit.SECONDS);
                    } catch (Exception exception) {
                        throw new AssertionError("Concurrent order attempt did not complete cleanly.", exception);
                    }
                })
                .toList();
        } finally {
            executor.shutdownNow();
        }

        assertThat(statuses).contains(201);
        assertThat(statuses.stream().filter(status -> status == 201)).hasSize(1);
        assertThat(statuses.stream().filter(status -> status == 409)).hasSize(1);

        Inventory finalInventory = transactionTemplate.execute(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            var product = productRepository
                .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase("STARTER-OPS", productSku)
                .orElseThrow();
            return inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).orElseThrow();
        });

        assertThat(finalInventory.getQuantityOnHand()).isEqualTo(1L);
        assertThat(finalInventory.getQuantityReserved()).isEqualTo(1L);
        assertThat(finalInventory.getQuantityAvailable()).isEqualTo(0L);
    }

    @Test
    void concurrentDuplicateExternalOrderIdCreatesOneOrderAndReturnsOneConflict() throws Exception {
        String productSku = "SKU-CONC-DUPLICATE-ID-" + System.nanoTime();
        String externalOrderId = "CONC-DUPLICATE-ID-" + System.nanoTime();
        createInventory(productSku, 2L);

        String orderBody = """
            {
              "externalOrderId":"%s",
              "warehouseCode":"WH-NORTH",
              "items":[{"productSku":"%s","quantity":1,"unitPrice":10.00}]
            }
            """.formatted(externalOrderId, productSku);

        List<Integer> statuses = runConcurrently(List.of(
            () -> mockMvc.perform(post("/api/orders")
                    .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                    .header("X-Synapse-Tenant", "STARTER-OPS")
                    .contentType(APPLICATION_JSON)
                    .content(orderBody))
                .andReturn().getResponse().getStatus(),
            () -> mockMvc.perform(post("/api/orders")
                    .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                    .header("X-Synapse-Tenant", "STARTER-OPS")
                    .contentType(APPLICATION_JSON)
                    .content(orderBody))
                .andReturn().getResponse().getStatus()
        ));

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        Inventory finalInventory = loadInventory(productSku, "WH-NORTH");
        assertThat(finalInventory.getQuantityOnHand()).isEqualTo(2L);
        assertThat(finalInventory.getQuantityReserved()).isEqualTo(1L);
        assertThat(finalInventory.getQuantityAvailable()).isEqualTo(1L);
        assertThat(customerOrderRepository
            .findByTenant_CodeIgnoreCaseAndExternalOrderId("STARTER-OPS", externalOrderId))
            .isPresent();
    }

    @Test
    void concurrentUpdatesOnExistingRowRemainValid() throws Exception {
        String productSku = "SKU-CONC-UPDATE-" + System.nanoTime();
        createInventory(productSku, 10L);

        List<Integer> statuses = runConcurrently(List.of(
            () -> inventoryRequest("/api/inventory/update", """
                {"productSku":"%s","warehouseCode":"WH-NORTH","quantityAvailable":20,"reorderThreshold":4}
                """.formatted(productSku)),
            () -> inventoryRequest("/api/inventory/update", """
                {"productSku":"%s","warehouseCode":"WH-NORTH","quantityAvailable":30,"reorderThreshold":5}
                """.formatted(productSku))
        ));

        assertThat(statuses).containsExactlyInAnyOrder(200, 200);
        Inventory finalInventory = loadInventory(productSku, "WH-NORTH");
        assertThat(finalInventory.getQuantityAvailable()).isIn(20L, 30L);
        assertThat(finalInventory.getReorderThreshold()).isIn(4L, 5L);
    }

    @Test
    void concurrentReceiptsAccumulateWithoutLostStock() throws Exception {
        String productSku = "SKU-CONC-RECEIVE-" + System.nanoTime();
        createInventory(productSku, 10L);

        List<Integer> statuses = runConcurrently(List.of(
            () -> inventoryRequest("/api/inventory/receive", """
                {"productSku":"%s","warehouseCode":"WH-NORTH","quantityReceived":2}
                """.formatted(productSku)),
            () -> inventoryRequest("/api/inventory/receive", """
                {"productSku":"%s","warehouseCode":"WH-NORTH","quantityReceived":3}
                """.formatted(productSku))
        ));

        assertThat(statuses).containsExactlyInAnyOrder(200, 200);
        assertThat(loadInventory(productSku, "WH-NORTH").getQuantityOnHand()).isEqualTo(15L);
    }

    @Test
    void concurrentAdjustmentsAccumulateWithoutLostStock() throws Exception {
        String productSku = "SKU-CONC-ADJUST-" + System.nanoTime();
        createInventory(productSku, 10L);

        List<Integer> statuses = runConcurrently(List.of(
            () -> inventoryRequest("/api/inventory/adjust", """
                {"productSku":"%s","warehouseCode":"WH-NORTH","quantityDelta":2,"reason":"Phase 2 race A"}
                """.formatted(productSku)),
            () -> inventoryRequest("/api/inventory/adjust", """
                {"productSku":"%s","warehouseCode":"WH-NORTH","quantityDelta":3,"reason":"Phase 2 race B"}
                """.formatted(productSku))
        ));

        assertThat(statuses).containsExactlyInAnyOrder(200, 200);
        assertThat(loadInventory(productSku, "WH-NORTH").getQuantityOnHand()).isEqualTo(15L);
    }

    @Test
    void concurrentReconciliationsLeaveOneCompleteCount() throws Exception {
        String productSku = "SKU-CONC-RECONCILE-" + System.nanoTime();
        createInventory(productSku, 10L);

        List<Integer> statuses = runConcurrently(List.of(
            () -> inventoryRequest("/api/inventory/reconcile", """
                {"productSku":"%s","warehouseCode":"WH-NORTH","countedOnHand":18,"note":"Phase 2 count A"}
                """.formatted(productSku)),
            () -> inventoryRequest("/api/inventory/reconcile", """
                {"productSku":"%s","warehouseCode":"WH-NORTH","countedOnHand":22,"note":"Phase 2 count B"}
                """.formatted(productSku))
        ));

        assertThat(statuses).containsExactlyInAnyOrder(200, 200);
        assertThat(loadInventory(productSku, "WH-NORTH").getQuantityOnHand()).isIn(18L, 22L);
    }

    @Test
    void concurrentFirstRowUpdatesDoNotCreateDuplicateInventoryRows() throws Exception {
        String productSku = "SKU-CONC-FIRST-" + System.nanoTime();
        createProduct(productSku);

        List<Integer> statuses = runConcurrently(List.of(
            () -> inventoryRequest("/api/inventory/update", """
                {"productSku":"%s","warehouseCode":"WH-NORTH","quantityAvailable":20,"reorderThreshold":4}
                """.formatted(productSku)),
            () -> inventoryRequest("/api/inventory/update", """
                {"productSku":"%s","warehouseCode":"WH-NORTH","quantityAvailable":30,"reorderThreshold":5}
                """.formatted(productSku))
        ));

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        Product product = productRepository
            .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase("STARTER-OPS", productSku)
            .orElseThrow();
        var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
        assertThat(inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())).isPresent();
        assertThat(inventoryRepository.findAll().stream()
            .filter(inventory -> inventory.getProduct().getId().equals(product.getId()))
            .filter(inventory -> inventory.getWarehouse().getId().equals(warehouse.getId())))
            .hasSize(1);
    }

    @Test
    void concurrentFulfillmentUpdatesDoNotDoubleCommitReservedStock() throws Exception {
        String externalOrderId = "CONC-FULFILL-" + System.nanoTime();
        String productSku = "SKU-CONC-FULFILL-" + System.nanoTime();
        createInventory(productSku, 2L);

        String orderBody = """
            {
              "externalOrderId":"%s",
              "warehouseCode":"WH-NORTH",
              "items":[{"productSku":"%s","quantity":2,"unitPrice":10.00}]
            }
            """.formatted(externalOrderId, productSku);
        assertThat(mockMvc.perform(post("/api/orders")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", "STARTER-OPS")
                .contentType(APPLICATION_JSON)
                .content(orderBody))
            .andReturn().getResponse().getStatus()).isEqualTo(201);

        List<Integer> statuses = runConcurrently(List.of(
            () -> fulfillmentRequest(externalOrderId),
            () -> fulfillmentRequest(externalOrderId)
        ));

        assertThat(statuses).containsExactlyInAnyOrder(200, 200);
        Inventory finalInventory = loadInventory(productSku, "WH-NORTH");
        assertThat(finalInventory.getQuantityOnHand()).isEqualTo(0L);
        assertThat(finalInventory.getQuantityReserved()).isEqualTo(0L);
        assertThat(finalInventory.getQuantityAvailable()).isEqualTo(0L);
        var order = customerOrderRepository
            .findByTenant_CodeIgnoreCaseAndExternalOrderId("STARTER-OPS", externalOrderId)
            .orElseThrow();
        assertThat(order.getItems().get(0).getFulfilledQuantity()).isEqualTo(2);
        assertThat(order.getItems().get(0).getReservedQuantity()).isEqualTo(0);
        assertThat(fulfillmentTaskRepository
            .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId("STARTER-OPS", externalOrderId))
            .isPresent();
    }

    @Test
    void insufficientOrderDoesNotPartiallyReserveInventory() throws Exception {
        String externalOrderId = "INSUFFICIENT-" + System.nanoTime();
        String productSku = "SKU-INSUFFICIENT-" + System.nanoTime();
        createInventory(productSku, 5L);

        String orderBody = """
            {
              "externalOrderId":"%s",
              "warehouseCode":"WH-NORTH",
              "items":[{"productSku":"%s","quantity":6,"unitPrice":10.00}]
            }
            """.formatted(externalOrderId, productSku);

        assertThat(mockMvc.perform(post("/api/orders")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", "STARTER-OPS")
                .contentType(APPLICATION_JSON)
                .content(orderBody))
            .andReturn().getResponse().getStatus()).isEqualTo(409);

        Inventory finalInventory = loadInventory(productSku, "WH-NORTH");
        assertThat(finalInventory.getQuantityOnHand()).isEqualTo(5L);
        assertThat(finalInventory.getQuantityReserved()).isEqualTo(0L);
        assertThat(finalInventory.getQuantityAvailable()).isEqualTo(5L);
        assertThat(customerOrderRepository
            .findByTenant_CodeIgnoreCaseAndExternalOrderId("STARTER-OPS", externalOrderId))
            .isEmpty();
    }

    @Test
    void cancellationReleasesReservationOnlyOnce() throws Exception {
        String externalOrderId = "CANCEL-" + System.nanoTime();
        String productSku = "SKU-CANCEL-" + System.nanoTime();
        createInventory(productSku, 5L);

        createOrder(externalOrderId, productSku, 2);
        assertThat(loadInventory(productSku, "WH-NORTH").getQuantityReserved()).isEqualTo(2L);

        transitionOrder(externalOrderId, "CANCELLED", false)
            .andExpect(status().isOk());

        Inventory releasedInventory = loadInventory(productSku, "WH-NORTH");
        assertThat(releasedInventory.getQuantityOnHand()).isEqualTo(5L);
        assertThat(releasedInventory.getQuantityReserved()).isEqualTo(0L);
        assertThat(releasedInventory.getQuantityAvailable()).isEqualTo(5L);

        transitionOrder(externalOrderId, "CANCELLED", false)
            .andExpect(status().isOk());
        Inventory afterDuplicate = loadInventory(productSku, "WH-NORTH");
        assertThat(afterDuplicate.getQuantityOnHand()).isEqualTo(5L);
        assertThat(afterDuplicate.getQuantityReserved()).isEqualTo(0L);
        assertThat(afterDuplicate.getQuantityAvailable()).isEqualTo(5L);
    }

    @Test
    void returnRestockRestoresFulfilledStockOnlyOnce() throws Exception {
        String externalOrderId = "RETURN-" + System.nanoTime();
        String productSku = "SKU-RETURN-" + System.nanoTime();
        createInventory(productSku, 5L);

        createOrder(externalOrderId, productSku, 2);
        assertThat(loadInventory(productSku, "WH-NORTH").getQuantityReserved()).isEqualTo(2L);
        assertThat(fulfillmentRequest(externalOrderId, "DELIVERED", null)).isEqualTo(200);

        Inventory fulfilledInventory = loadInventory(productSku, "WH-NORTH");
        assertThat(fulfilledInventory.getQuantityOnHand()).isEqualTo(3L);
        assertThat(fulfilledInventory.getQuantityReserved()).isEqualTo(0L);

        transitionOrder(externalOrderId, "RETURNED", true)
            .andExpect(status().isOk());

        Inventory restockedInventory = loadInventory(productSku, "WH-NORTH");
        assertThat(restockedInventory.getQuantityOnHand()).isEqualTo(5L);
        assertThat(restockedInventory.getQuantityReserved()).isEqualTo(0L);
        assertThat(restockedInventory.getQuantityAvailable()).isEqualTo(5L);

        transitionOrder(externalOrderId, "RETURNED", true)
            .andExpect(status().isBadRequest());
        Inventory afterDuplicate = loadInventory(productSku, "WH-NORTH");
        assertThat(afterDuplicate.getQuantityOnHand()).isEqualTo(5L);
        assertThat(afterDuplicate.getQuantityReserved()).isEqualTo(0L);
        assertThat(afterDuplicate.getQuantityAvailable()).isEqualTo(5L);
    }

    private void createProduct(String productSku) {
        transactionTemplate.executeWithoutResult(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            productRepository.save(Product.builder()
                .tenant(warehouse.getTenant())
                .catalogSku(productSku)
                .name("Concurrency Proof Product")
                .category("Verification")
                .build());
        });
    }

    private void createInventory(String productSku, long quantityOnHand) {
        transactionTemplate.executeWithoutResult(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            Product product = productRepository.save(Product.builder()
                .tenant(warehouse.getTenant())
                .catalogSku(productSku)
                .name("Concurrency Proof Product")
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

    private Inventory loadInventory(String productSku, String warehouseCode) {
        return transactionTemplate.execute(status -> {
            var warehouse = warehouseRepository.findByCode(warehouseCode).orElseThrow();
            var product = productRepository
                .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase("STARTER-OPS", productSku)
                .orElseThrow();
            return inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).orElseThrow();
        });
    }

    private int inventoryRequest(String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "STARTER-OPS")
                .contentType(APPLICATION_JSON)
                .content(body))
            .andReturn()
            .getResponse()
            .getStatus();
    }

    private int fulfillmentRequest(String externalOrderId) throws Exception {
        return fulfillmentRequest(externalOrderId, "DISPATCHED", 1);
    }

    private int fulfillmentRequest(String externalOrderId, String status, Integer fulfilledUnits) throws Exception {
        String fulfilledUnitsField = fulfilledUnits == null ? "null" : fulfilledUnits.toString();
        String body = """
            {"externalOrderId":"%s","status":"%s","fulfilledUnits":%s,"note":"Inventory Phase 2 order effect proof"}
            """.formatted(externalOrderId, status, fulfilledUnitsField);
        return mockMvc.perform(post("/api/fulfillment/updates")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", "STARTER-OPS")
                .contentType(APPLICATION_JSON)
                .content(body))
            .andReturn()
            .getResponse()
            .getStatus();
    }

    private void createOrder(String externalOrderId, String productSku, int quantity) throws Exception {
        String orderBody = """
            {
              "externalOrderId":"%s",
              "warehouseCode":"WH-NORTH",
              "items":[{"productSku":"%s","quantity":%d,"unitPrice":10.00}]
            }
            """.formatted(externalOrderId, productSku, quantity);
        assertThat(mockMvc.perform(post("/api/orders")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", "STARTER-OPS")
                .contentType(APPLICATION_JSON)
                .content(orderBody))
            .andReturn().getResponse().getStatus()).isEqualTo(201);
    }

    private org.springframework.test.web.servlet.ResultActions transitionOrder(
            String externalOrderId, String status, boolean restockInventory) throws Exception {
        return mockMvc.perform(post("/api/orders/" + externalOrderId + "/transition")
            .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
            .header("X-Synapse-Tenant", "STARTER-OPS")
            .contentType(APPLICATION_JSON)
            .content("{\"status\":\"%s\",\"note\":\"Inventory Phase 2 proof\",\"restockInventory\":%s}"
                .formatted(status, restockInventory)));
    }

    private List<Integer> runConcurrently(List<Callable<Integer>> attempts) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(attempts.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = attempts.stream()
            .map(attempt -> executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return attempt.call();
            }))
            .toList();
        start.countDown();
        try {
            return futures.stream().map(future -> {
                try {
                    return future.get(15, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new AssertionError("Concurrent Inventory Phase 2 attempt did not complete cleanly.", exception);
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
