package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
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
}
