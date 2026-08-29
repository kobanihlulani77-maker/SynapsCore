package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FulfillmentLifecyclePhase1IntegrationTest {

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
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void rejectsBackwardTransitionsAfterDeliveredPackedAndException() throws Exception {
        String deliveredOrder = createOrder("DELIVERED", List.of(line("DELIVERED", 1)));
        fulfillment(deliveredOrder, "DELIVERED", null, null, "backward-delivered").andExpect(status().isOk());
        fulfillment(deliveredOrder, "PICKING", null, null, "backward-delivered-retry")
            .andExpect(status().isBadRequest());

        String packedOrder = createOrder("PACKED", List.of(line("PACKED", 1)));
        fulfillment(packedOrder, "PACKED", null, null, "backward-packed").andExpect(status().isOk());
        fulfillment(packedOrder, "PICKING", null, null, "backward-packed-retry")
            .andExpect(status().isBadRequest());

        String exceptionOrder = createOrder("EXCEPTION", List.of(line("EXCEPTION", 1)));
        fulfillment(exceptionOrder, "EXCEPTION", null, null, "backward-exception").andExpect(status().isOk());
        fulfillment(exceptionOrder, "PICKING", null, null, "backward-exception-retry")
            .andExpect(status().isBadRequest());
    }

    @Test
    void sameRequestIdRejectsDifferentConsequentialPayload() throws Exception {
        String sku = sku("RETRY");
        String orderId = createOrder("RETRY", List.of(line(sku, 10)));
        String requestId = "fulfillment-p1-retry-" + System.nanoTime();

        fulfillment(orderId, "DISPATCHED", 3, null, requestId).andExpect(status().isOk());
        fulfillment(orderId, "DISPATCHED", 3, null, requestId).andExpect(status().isOk());
        fulfillment(orderId, "DISPATCHED", 4, null, requestId).andExpect(status().isConflict());

        Inventory inventory = loadInventory(sku, "WH-NORTH");
        assertThat(inventory.getQuantityOnHand()).isEqualTo(7L);
        assertThat(inventory.getQuantityReserved()).isEqualTo(7L);
        assertThat(auditLogRepository.findAll().stream()
            .filter(log -> TENANT.equalsIgnoreCase(log.getTenantCode()))
            .filter(log -> "FULFILLMENT_UPDATED".equals(log.getAction()))
            .filter(log -> orderId.equals(log.getTargetRef()))
            .count()).isEqualTo(1);
    }

    @Test
    void fulfillmentExceptionReleasesUnfulfilledReservationForZeroAndPartialWork() throws Exception {
        String zeroSku = sku("EXCEPTION-ZERO");
        String zeroOrder = createOrder("EXCEPTION-ZERO", List.of(line(zeroSku, 10)));
        fulfillment(zeroOrder, "EXCEPTION", null, null, "exception-zero-" + System.nanoTime())
            .andExpect(status().isOk());
        assertFailedAndReleased(zeroOrder, zeroSku, 10L, 0);

        String partialSku = sku("EXCEPTION-PARTIAL");
        String partialOrder = createOrder("EXCEPTION-PARTIAL", List.of(line(partialSku, 10)));
        fulfillment(partialOrder, "DISPATCHED", 4, null, "exception-partial-dispatch-" + System.nanoTime())
            .andExpect(status().isOk());
        fulfillment(partialOrder, "EXCEPTION", null, null, "exception-partial-" + System.nanoTime())
            .andExpect(status().isOk());
        assertFailedAndReleased(partialOrder, partialSku, 6L, 4);
    }

    @Test
    void cancelledAndFailedOrdersAreNotDisplayedAsActiveFulfillmentWork() throws Exception {
        String cancelledOrder = createOrder("CANCELLED", List.of(line(sku("CANCELLED"), 1)));
        transition(cancelledOrder, "CANCELLED").andExpect(status().isOk());

        String failedOrder = createOrder("FAILED", List.of(line(sku("FAILED"), 1)));
        transition(failedOrder, "FAILED").andExpect(status().isOk());

        mockMvc.perform(get("/api/fulfillment")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", TENANT))
            .andExpect(status().isOk())
            .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                .doesNotContain(cancelledOrder)
                .doesNotContain(failedOrder));

        assertThat(loadTask(cancelledOrder)).isNotNull();
        assertThat(loadTask(failedOrder)).isNotNull();
    }

    @Test
    void rejectsZeroNegativeNonDispatchAndAggregateOverQuantity() throws Exception {
        String sku = sku("QUANTITY");
        String orderId = createOrder("QUANTITY", List.of(line(sku, 5)));

        fulfillment(orderId, "DISPATCHED", 0, null, "quantity-zero-" + System.nanoTime())
            .andExpect(status().isBadRequest());
        fulfillment(orderId, "DISPATCHED", -1, null, "quantity-negative-" + System.nanoTime())
            .andExpect(status().isBadRequest());
        fulfillment(orderId, "PICKING", 1, null, "quantity-non-dispatch-" + System.nanoTime())
            .andExpect(status().isBadRequest());
        fulfillment(orderId, "DISPATCHED", 100, null, "quantity-overage-" + System.nanoTime())
            .andExpect(status().isConflict());

        Inventory inventory = loadInventory(sku, "WH-NORTH");
        assertThat(inventory.getQuantityOnHand()).isEqualTo(5L);
        assertThat(inventory.getQuantityReserved()).isEqualTo(5L);
        assertThat(loadOrder(orderId).getStatus()).isEqualTo(OrderStatus.RECEIVED);
    }

    @Test
    void directDeliveryCompletesWholeMultiLineOrderAndRetryDoesNotConsumeAgain() throws Exception {
        String firstSku = sku("DIRECT-A");
        String secondSku = sku("DIRECT-B");
        createInventory(firstSku, 10);
        createInventory(secondSku, 10);
        String orderId = createOrder(
            "DIRECT-DELIVERY",
            List.of(
                "{\"productSku\":\"%s\",\"quantity\":5,\"unitPrice\":10.00}".formatted(firstSku),
                "{\"productSku\":\"%s\",\"quantity\":5,\"unitPrice\":10.00}".formatted(secondSku)
            )
        );
        String requestId = "direct-delivery-" + System.nanoTime();

        fulfillment(orderId, "DELIVERED", null, null, requestId).andExpect(status().isOk());
        fulfillment(orderId, "DELIVERED", null, null, requestId).andExpect(status().isOk());
        fulfillment(orderId, "DELIVERED", null, null, "direct-delivery-new-" + System.nanoTime())
            .andExpect(status().isOk());

        CustomerOrder order = loadOrder(orderId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        order.getItems().forEach(item -> {
            assertThat(item.getFulfilledQuantity()).isEqualTo(5);
            assertThat(item.getReservedQuantity()).isEqualTo(0);
        });
        assertThat(loadInventory(firstSku, "WH-NORTH").getQuantityOnHand()).isEqualTo(5L);
        assertThat(loadInventory(secondSku, "WH-NORTH").getQuantityOnHand()).isEqualTo(5L);
    }

    private void assertFailedAndReleased(String orderId, String productSku, long expectedOnHand, int expectedFulfilled) {
        CustomerOrder order = loadOrder(orderId);
        Inventory inventory = loadInventory(productSku, "WH-NORTH");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(order.getItems().get(0).getFulfilledQuantity()).isEqualTo(expectedFulfilled);
        assertThat(order.getItems().get(0).getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getQuantityOnHand()).isEqualTo(expectedOnHand);
        assertThat(inventory.getQuantityReserved()).isEqualTo(0L);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(expectedOnHand);
    }

    private String createOrder(String label, List<String> lines) throws Exception {
        String orderId = "FULFILL-P1-" + label + "-" + System.nanoTime();
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
                .name("Fulfillment Phase 1 Product")
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

    private ResultActions fulfillment(String orderId,
                                      String fulfillmentStatus,
                                      Integer fulfilledUnits,
                                      String fulfilledProductSku,
                                      String requestId) throws Exception {
        String quantity = fulfilledUnits == null ? "null" : fulfilledUnits.toString();
        String sku = fulfilledProductSku == null ? "null" : "\"" + fulfilledProductSku + "\"";
        String body = """
            {"externalOrderId":"%s","status":"%s","fulfilledUnits":%s,"fulfilledProductSku":%s,"note":"Fulfillment Phase 1 proof"}
            """.formatted(orderId, fulfillmentStatus, quantity, sku);
        return mockMvc.perform(post("/api/fulfillment/updates")
            .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
            .header("X-Synapse-Tenant", TENANT)
            .header("X-Request-Id", requestId)
            .contentType(APPLICATION_JSON)
            .content(body));
    }

    private ResultActions transition(String orderId, String orderStatus) throws Exception {
        return mockMvc.perform(post("/api/orders/" + orderId + "/transition")
            .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
            .header("X-Synapse-Tenant", TENANT)
            .contentType(APPLICATION_JSON)
            .content("{\"status\":\"%s\",\"note\":\"Fulfillment Phase 1 proof\"}".formatted(orderStatus)));
    }

    private CustomerOrder loadOrder(String orderId) {
        return transactionTemplate.execute(status -> customerOrderRepository
            .findByTenant_CodeIgnoreCaseAndExternalOrderId(TENANT, orderId)
            .orElseThrow());
    }

    private FulfillmentTask loadTask(String orderId) {
        return transactionTemplate.execute(status -> fulfillmentTaskRepository
            .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(TENANT, orderId)
            .orElseThrow());
    }

    private Inventory loadInventory(String productSku, String warehouseCode) {
        return transactionTemplate.execute(status -> {
            var warehouse = warehouseRepository.findByCode(warehouseCode).orElseThrow();
            var product = productRepository
                .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(TENANT, productSku)
                .orElseThrow();
            return inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).orElseThrow();
        });
    }

    private String line(String productSku, int quantity) {
        createInventory(productSku, quantity);
        return "{\"productSku\":\"%s\",\"quantity\":%d,\"unitPrice\":10.00}".formatted(productSku, quantity);
    }

    private String sku(String label) {
        return "SKU-FULFILL-P1-" + label + "-" + System.nanoTime();
    }

    private static RequestPostProcessor accessHeaders(String actorName, String roles) {
        return request -> {
            request.addHeader("X-Synapse-Actor", actorName);
            request.addHeader("X-Synapse-Roles", roles);
            return request;
        };
    }
}
