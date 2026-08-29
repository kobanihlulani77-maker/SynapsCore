package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.repository.CustomerOrderRepository;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderLifecyclePhase3IntegrationTest {

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
    private TransactionTemplate transactionTemplate;

    @Test
    void lineSpecificDispatchCompletesEachProductWithoutWrongLineConsumption() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String externalOrderId = "ORDER-P3-LINE-" + suffix;
        String skuA = "SKU-ORDER-P3-A-" + suffix;
        String skuB = "SKU-ORDER-P3-B-" + suffix;
        createInventory(skuA, 10L);
        createInventory(skuB, 10L);
        createOrder(externalOrderId, List.of(orderLine(skuA, 5), orderLine(skuB, 5)));

        fulfillmentRequest(externalOrderId, skuA, 5, "order-p3-line-a-" + suffix)
            .andExpect(status().isOk());

        CustomerOrder partialOrder = loadOrder(externalOrderId);
        assertThat(partialOrder.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FULFILLED);
        assertLine(partialOrder, skuA, 5, 5, 0, 0, 0);
        assertLine(partialOrder, skuB, 5, 0, 5, 0, 0);
        assertInventory(skuA, 5L, 0L, 5L);
        assertInventory(skuB, 10L, 5L, 5L);

        fulfillmentRequest(externalOrderId, skuB, 5, "order-p3-line-b-" + suffix)
            .andExpect(status().isOk());

        CustomerOrder completedOrder = loadOrder(externalOrderId);
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.FULFILLED);
        assertLine(completedOrder, skuA, 5, 5, 0, 0, 0);
        assertLine(completedOrder, skuB, 5, 5, 0, 0, 0);
        assertInventory(skuA, 5L, 0L, 5L);
        assertInventory(skuB, 5L, 0L, 5L);
    }

    @Test
    void cancellationAfterLineSpecificDispatchReleasesOnlyTheUnfulfilledLine() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String externalOrderId = "ORDER-P3-CANCEL-" + suffix;
        String skuA = "SKU-ORDER-P3-CANCEL-A-" + suffix;
        String skuB = "SKU-ORDER-P3-CANCEL-B-" + suffix;
        createInventory(skuA, 10L);
        createInventory(skuB, 10L);
        createOrder(externalOrderId, List.of(orderLine(skuA, 5), orderLine(skuB, 5)));

        fulfillmentRequest(externalOrderId, skuA, 5, "order-p3-cancel-a-" + suffix)
            .andExpect(status().isOk());
        transitionOrder(externalOrderId, "CANCELLED", false).andExpect(status().isOk());

        CustomerOrder order = loadOrder(externalOrderId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertLine(order, skuA, 5, 5, 0, 0, 0);
        assertLine(order, skuB, 5, 0, 0, 5, 0);
        assertInventory(skuA, 5L, 0L, 5L);
        assertInventory(skuB, 10L, 0L, 10L);
    }

    @Test
    void returnAfterLineSpecificDispatchRestocksOnlyTheFulfilledLine() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String externalOrderId = "ORDER-P3-RETURN-" + suffix;
        String skuA = "SKU-ORDER-P3-RETURN-A-" + suffix;
        String skuB = "SKU-ORDER-P3-RETURN-B-" + suffix;
        createInventory(skuA, 10L);
        createInventory(skuB, 10L);
        createOrder(externalOrderId, List.of(orderLine(skuA, 5), orderLine(skuB, 5)));

        fulfillmentRequest(externalOrderId, skuA, 5, "order-p3-return-a-" + suffix)
            .andExpect(status().isOk());
        transitionOrder(externalOrderId, "RETURNED", true).andExpect(status().isOk());

        CustomerOrder order = loadOrder(externalOrderId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURNED);
        assertLine(order, skuA, 5, 5, 0, 0, 5);
        assertLine(order, skuB, 5, 0, 5, 0, 0);
        assertInventory(skuA, 10L, 0L, 10L);
        assertInventory(skuB, 10L, 5L, 5L);
    }

    private void createInventory(String productSku, long quantityOnHand) {
        transactionTemplate.executeWithoutResult(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            Product product = productRepository.save(Product.builder()
                .tenant(warehouse.getTenant())
                .catalogSku(productSku)
                .name("Order Phase 3 Product")
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

    private void createOrder(String externalOrderId, List<String> lines) throws Exception {
        String body = """
            {
              "externalOrderId":"%s",
              "warehouseCode":"WH-NORTH",
              "items":[%s]
            }
            """.formatted(externalOrderId, String.join(",", lines));
        mockMvc.perform(post("/api/orders")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", "STARTER-OPS")
                .contentType(APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());
    }

    private String orderLine(String productSku, int quantity) {
        return "{\"productSku\":\"%s\",\"quantity\":%d,\"unitPrice\":10.00}"
            .formatted(productSku, quantity);
    }

    private org.springframework.test.web.servlet.ResultActions fulfillmentRequest(
            String externalOrderId,
            String productSku,
            int fulfilledUnits,
            String requestId) throws Exception {
        String body = """
            {"externalOrderId":"%s","status":"DISPATCHED","fulfilledUnits":%d,"fulfilledProductSku":"%s","note":"Order Phase 3 proof"}
            """.formatted(externalOrderId, fulfilledUnits, productSku);
        return mockMvc.perform(post("/api/fulfillment/updates")
            .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
            .header("X-Synapse-Tenant", "STARTER-OPS")
            .header("X-Request-Id", requestId)
            .contentType(APPLICATION_JSON)
            .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions transitionOrder(
            String externalOrderId, String orderStatus, boolean restockInventory) throws Exception {
        return mockMvc.perform(post("/api/orders/" + externalOrderId + "/transition")
            .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
            .header("X-Synapse-Tenant", "STARTER-OPS")
            .contentType(APPLICATION_JSON)
            .content("{\"status\":\"%s\",\"restockInventory\":%s,\"note\":\"Order Phase 3 proof\"}"
                .formatted(orderStatus, restockInventory)));
    }

    private CustomerOrder loadOrder(String externalOrderId) {
        return transactionTemplate.execute(status -> customerOrderRepository
            .findByTenant_CodeIgnoreCaseAndExternalOrderId("STARTER-OPS", externalOrderId)
            .orElseThrow());
    }

    private void assertLine(CustomerOrder order,
                            String productSku,
                            int quantity,
                            int fulfilled,
                            int reserved,
                            int cancelled,
                            int returned) {
        var item = order.getItems().stream()
            .filter(line -> productSku.equals(line.getProduct().resolveCatalogSku()))
            .findFirst()
            .orElseThrow();
        assertThat(item.getQuantity()).isEqualTo(quantity);
        assertThat(item.getFulfilledQuantity()).isEqualTo(fulfilled);
        assertThat(item.getReservedQuantity()).isEqualTo(reserved);
        assertThat(item.getCancelledQuantity()).isEqualTo(cancelled);
        assertThat(item.getReturnedQuantity()).isEqualTo(returned);
    }

    private void assertInventory(String productSku, long onHand, long reserved, long available) {
        Inventory inventory = transactionTemplate.execute(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            var product = productRepository
                .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase("STARTER-OPS", productSku)
                .orElseThrow();
            return inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).orElseThrow();
        });
        assertThat(inventory.getQuantityOnHand()).isEqualTo(onHand);
        assertThat(inventory.getQuantityReserved()).isEqualTo(reserved);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(available);
    }

    private static RequestPostProcessor accessHeaders(String actorName, String roles) {
        return request -> {
            request.addHeader("X-Synapse-Actor", actorName);
            request.addHeader("X-Synapse-Roles", roles);
            return request;
        };
    }
}
