package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
    "spring.datasource.hikari.maximum-pool-size=10",
    "spring.datasource.hikari.minimum-idle=10",
    "spring.datasource.hikari.connection-timeout=2000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(OrderConnectionDemandIntegrationTest.ConnectionDemandTestConfiguration.class)
class OrderConnectionDemandIntegrationTest {

    private static final int WORKERS = 10;

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

    @Autowired
    private OrderConnectionHoldProbe probe;

    @RepeatedTest(3)
    @Timeout(45)
    void tenDistinctOrdersProgressThroughTenConnectionPoolWithoutCircularBorrow() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String skuPrefix = "ORDER-DEMAND-" + suffix + "-";
        prepareDistinctInventory(skuPrefix);
        probe.arm(skuPrefix, WORKERS);

        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        List<Future<Integer>> requests = new ArrayList<>();
        try {
            for (int index = 0; index < WORKERS; index++) {
                int requestIndex = index;
                requests.add(executor.submit(() -> createOrder(skuPrefix, suffix, requestIndex)));
            }

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> request : requests) {
                statuses.add(request.get(30, TimeUnit.SECONDS));
            }

            assertThat(statuses).containsOnly(201);
            assertThat(probe.transactionActiveForEveryWorker()).isTrue();
            assertThat(probe.activeAtBarrier()).isEqualTo(WORKERS);
            assertThat(probe.idleAtBarrier()).isZero();
            assertThat(probe.waitersAtBarrier())
                .as("bounded after-commit overlap may queue, but ten Order transactions must not create ten nested borrowers")
                .isLessThan(WORKERS);
            assertCommittedState(skuPrefix, suffix);
            probe.awaitCompleteRelease();
            System.out.printf(
                "Order connection demand workers=%d activeAtBarrier=%d idleAtBarrier=%d waitersAtBarrier=%d completed=%d%n",
                WORKERS,
                probe.activeAtBarrier(),
                probe.idleAtBarrier(),
                probe.waitersAtBarrier(),
                statuses.size()
            );
        } finally {
            probe.disarm();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void prepareDistinctInventory(String skuPrefix) {
        transactionTemplate.executeWithoutResult(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            for (int index = 0; index < WORKERS; index++) {
                Product product = productRepository.save(Product.builder()
                    .tenant(warehouse.getTenant())
                    .catalogSku(skuPrefix + index)
                    .name("Order Demand " + index)
                    .category("Verification")
                    .build());
                inventoryRepository.save(Inventory.builder()
                    .tenant(warehouse.getTenant())
                    .product(product)
                    .warehouse(warehouse)
                    .quantityOnHand(2L)
                    .quantityReserved(0L)
                    .quantityInbound(0L)
                    .quantityAvailable(2L)
                    .reorderThreshold(0L)
                    .build());
            }
        });
    }

    private int createOrder(String skuPrefix, String suffix, int index) throws Exception {
        String externalOrderId = "ORDER-DEMAND-" + suffix + "-" + index;
        String requestBody = """
            {
              "externalOrderId":"%s",
              "warehouseCode":"WH-NORTH",
              "items":[{"productSku":"%s","quantity":1,"unitPrice":10.00}]
            }
            """.formatted(externalOrderId, skuPrefix + index);
        return mockMvc.perform(post("/api/orders")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", "STARTER-OPS")
                .header("X-Request-Id", "order-demand-" + suffix + "-" + index)
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andReturn()
            .getResponse()
            .getStatus();
    }

    private void assertCommittedState(String skuPrefix, String suffix) {
        transactionTemplate.executeWithoutResult(status -> {
            var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
            for (int index = 0; index < WORKERS; index++) {
                String externalOrderId = "ORDER-DEMAND-" + suffix + "-" + index;
                Product product = productRepository
                    .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase("STARTER-OPS", skuPrefix + index)
                    .orElseThrow();
                Inventory inventory = inventoryRepository
                    .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                    .orElseThrow();
                assertThat(inventory.getQuantityOnHand()).isEqualTo(2L);
                assertThat(inventory.getQuantityReserved()).isEqualTo(1L);
                assertThat(inventory.getQuantityAvailable()).isEqualTo(1L);
                assertThat(customerOrderRepository
                    .findByTenant_CodeIgnoreCaseAndExternalOrderId("STARTER-OPS", externalOrderId))
                    .isPresent();
                assertThat(fulfillmentTaskRepository
                    .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId("STARTER-OPS", externalOrderId))
                    .isPresent();
            }
        });
    }

    private RequestPostProcessor accessHeaders(String actor, String... roles) {
        return request -> {
            request.addHeader("X-Synapse-Actor", actor);
            request.addHeader("X-Synapse-Roles", String.join(",", roles));
            return request;
        };
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ConnectionDemandTestConfiguration {

        @Bean
        OrderConnectionHoldProbe orderConnectionHoldProbe(DataSource dataSource) throws Exception {
            return new OrderConnectionHoldProbe(dataSource.unwrap(HikariDataSource.class));
        }

        @Bean
        OrderConnectionBarrierAspect orderConnectionBarrierAspect(OrderConnectionHoldProbe probe) {
            return new OrderConnectionBarrierAspect(probe);
        }
    }

    static class OrderConnectionHoldProbe {

        private final HikariDataSource pool;
        private final AtomicInteger activeAtBarrier = new AtomicInteger();
        private final AtomicInteger idleAtBarrier = new AtomicInteger();
        private final AtomicInteger waitersAtBarrier = new AtomicInteger();
        private final AtomicBoolean everyWorkerTransactional = new AtomicBoolean(true);
        private volatile String skuPrefix;
        private volatile CyclicBarrier barrier;

        OrderConnectionHoldProbe(HikariDataSource pool) {
            this.pool = pool;
        }

        void arm(String skuPrefix, int workers) {
            this.skuPrefix = skuPrefix;
            activeAtBarrier.set(0);
            idleAtBarrier.set(0);
            waitersAtBarrier.set(0);
            everyWorkerTransactional.set(true);
            barrier = new CyclicBarrier(workers, () -> {
                activeAtBarrier.set(pool.getHikariPoolMXBean().getActiveConnections());
                idleAtBarrier.set(pool.getHikariPoolMXBean().getIdleConnections());
                waitersAtBarrier.set(pool.getHikariPoolMXBean().getThreadsAwaitingConnection());
            });
        }

        void holdAfterProductResolution(String productSku) {
            CyclicBarrier currentBarrier = barrier;
            if (currentBarrier == null || skuPrefix == null || !productSku.startsWith(skuPrefix)) {
                return;
            }
            everyWorkerTransactional.compareAndSet(
                true,
                TransactionSynchronizationManager.isActualTransactionActive()
            );
            try {
                currentBarrier.await(10, TimeUnit.SECONDS);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not align Order transactions at the connection-hold barrier", exception);
            }
        }

        boolean transactionActiveForEveryWorker() {
            return everyWorkerTransactional.get();
        }

        int activeAtBarrier() {
            return activeAtBarrier.get();
        }

        int idleAtBarrier() {
            return idleAtBarrier.get();
        }

        int waitersAtBarrier() {
            return waitersAtBarrier.get();
        }

        void awaitCompleteRelease() throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                if (pool.getHikariPoolMXBean().getActiveConnections() == 0
                    && pool.getHikariPoolMXBean().getThreadsAwaitingConnection() == 0) {
                    return;
                }
                Thread.sleep(20);
            }
            assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
            assertThat(pool.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
        }

        void disarm() {
            CyclicBarrier currentBarrier = barrier;
            barrier = null;
            skuPrefix = null;
            if (currentBarrier != null) {
                currentBarrier.reset();
            }
        }
    }

    @Aspect
    static class OrderConnectionBarrierAspect {

        private final OrderConnectionHoldProbe probe;

        OrderConnectionBarrierAspect(OrderConnectionHoldProbe probe) {
            this.probe = probe;
        }

        @Around("execution(* com.synapsecore.domain.service.InventoryService.requireProduct(..))")
        Object alignAfterProductResolution(ProceedingJoinPoint joinPoint) throws Throwable {
            Object result = joinPoint.proceed();
            Object[] arguments = joinPoint.getArgs();
            if (arguments.length > 1 && arguments[1] instanceof String productSku) {
                probe.holdAfterProductResolution(productSku);
            }
            return result;
        }
    }
}
