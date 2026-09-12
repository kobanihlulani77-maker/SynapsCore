package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationReplayRecord;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.integration.IntegrationFailureCode;
import com.synapsecore.integration.IntegrationReplayService;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Instant;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
    "spring.datasource.hikari.maximum-pool-size=10",
    "spring.datasource.hikari.minimum-idle=10",
    "spring.datasource.hikari.connection-timeout=2000",
    "spring.task.scheduling.enabled=false",
    "synapsecore.integration.replay.backoff-seconds=300"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(ReplayOrderConnectionDemandIntegrationTest.ConnectionDemandTestConfiguration.class)
class ReplayOrderConnectionDemandIntegrationTest {

    private static final int HTTP_ORDER_WORKERS = 9;
    private static final int TOTAL_WORKERS = 10;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationReplayService integrationReplayService;

    @Autowired
    private IntegrationReplayRecordRepository replayRecordRepository;

    @Autowired
    private IntegrationConnectorRepository connectorRepository;

    @Autowired
    private TenantRepository tenantRepository;

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
    private ReplayOrderConnectionProbe probe;

    @RepeatedTest(3)
    @Timeout(45)
    void failedReplayAndNineOrdersProgressWithoutNestedConnectionBorrow() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String skuPrefix = "REPLAY-ORDER-DEMAND-" + suffix + "-";
        String replayOrderId = "REPLAY-DEMAND-" + suffix;
        Long replayId = prepareFixtures(suffix, skuPrefix, replayOrderId);
        probe.arm(skuPrefix, replayId, TOTAL_WORKERS);

        ExecutorService executor = Executors.newFixedThreadPool(TOTAL_WORKERS);
        try {
            Future<Integer> replay = executor.submit(() -> integrationReplayService.processAutomatedReplayBatch(1));
            List<Future<Integer>> orders = new ArrayList<>();
            for (int index = 0; index < HTTP_ORDER_WORKERS; index++) {
                int requestIndex = index;
                orders.add(executor.submit(() -> createOrder(skuPrefix, suffix, requestIndex)));
            }

            assertThat(replay.get(30, TimeUnit.SECONDS)).isZero();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> order : orders) {
                statuses.add(order.get(30, TimeUnit.SECONDS));
            }

            assertThat(statuses).containsOnly(201);
            assertThat(probe.transactionActiveForEveryWorker()).isTrue();
            assertThat(probe.activeAtBarrier()).isEqualTo(TOTAL_WORKERS);
            assertThat(probe.idleAtBarrier()).isZero();
            assertThat(probe.waitersAtBarrier())
                .as("bounded after-commit overlap may queue, but Replay and Order must not create ten nested borrowers")
                .isLessThan(TOTAL_WORKERS);
            assertThat(probe.replayLockCalls()).isEqualTo(2);
            assertThat(probe.initialReplayCompletedBeforeFailureWrite()).isTrue();
            assertCommittedState(skuPrefix, suffix, replayId, replayOrderId);
            probe.awaitCompleteRelease();
            System.out.printf(
                "Replay/Order demand workers=%d activeAtBarrier=%d idleAtBarrier=%d waitersAtBarrier=%d "
                    + "replayLockCalls=%d rollbackCompletedBeforeFailureWrite=%s completedOrders=%d%n",
                TOTAL_WORKERS,
                probe.activeAtBarrier(),
                probe.idleAtBarrier(),
                probe.waitersAtBarrier(),
                probe.replayLockCalls(),
                probe.initialReplayCompletedBeforeFailureWrite(),
                statuses.size()
            );
        } finally {
            probe.disarm();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Long prepareFixtures(String suffix, String skuPrefix, String replayOrderId) {
        return transactionTemplate.execute(status -> {
            var tenant = tenantRepository.findByCodeIgnoreCase("STARTER-OPS").orElseThrow();
            var warehouse = warehouseRepository
                .findByTenant_CodeIgnoreCaseAndCode("STARTER-OPS", "WH-NORTH")
                .orElseThrow();
            String source = "replay-demand-" + suffix;
            connectorRepository.save(IntegrationConnector.builder()
                .tenant(tenant)
                .sourceSystem(source)
                .type(IntegrationConnectorType.CSV_ORDER_IMPORT)
                .displayName("Replay Demand " + suffix)
                .enabled(true)
                .syncMode(IntegrationSyncMode.BATCH_FILE_DROP)
                .validationPolicy(IntegrationValidationPolicy.STANDARD)
                .transformationPolicy(IntegrationTransformationPolicy.NORMALIZE_CODES)
                .defaultWarehouseCode(warehouse.getCode())
                .build());

            for (int index = 0; index < TOTAL_WORKERS; index++) {
                Product product = productRepository.save(Product.builder()
                    .tenant(tenant)
                    .catalogSku(skuPrefix + index)
                    .name("Replay Order Demand " + index)
                    .category("Verification")
                    .build());
                long quantity = index == HTTP_ORDER_WORKERS ? 1L : 2L;
                inventoryRepository.save(Inventory.builder()
                    .tenant(tenant)
                    .product(product)
                    .warehouse(warehouse)
                    .quantityOnHand(quantity)
                    .quantityReserved(0L)
                    .quantityInbound(0L)
                    .quantityAvailable(quantity)
                    .reorderThreshold(0L)
                    .build());
            }

            return replayRecordRepository.save(IntegrationReplayRecord.builder()
                .tenantCode(tenant.getCode())
                .sourceSystem(source)
                .connectorType(IntegrationConnectorType.CSV_ORDER_IMPORT)
                .externalOrderId(replayOrderId)
                .warehouseCode(warehouse.getCode())
                .requestPayload(orderPayload(replayOrderId, skuPrefix + HTTP_ORDER_WORKERS, 2))
                .failureCode(IntegrationFailureCode.INSUFFICIENT_INVENTORY)
                .failureMessage("Synthetic insufficient inventory for connection-demand proof")
                .status(IntegrationReplayStatus.PENDING)
                .replayAttemptCount(0)
                .nextEligibleAt(Instant.now().minusSeconds(1))
                .build()).getId();
        });
    }

    private int createOrder(String skuPrefix, String suffix, int index) throws Exception {
        String externalOrderId = "HTTP-ORDER-DEMAND-" + suffix + "-" + index;
        return mockMvc.perform(post("/api/orders")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .header("X-Synapse-Tenant", "STARTER-OPS")
                .header("X-Request-Id", "replay-order-demand-" + suffix + "-" + index)
                .contentType(APPLICATION_JSON)
                .content(orderPayload(externalOrderId, skuPrefix + index, 1)))
            .andReturn()
            .getResponse()
            .getStatus();
    }

    private String orderPayload(String externalOrderId, String productSku, int quantity) {
        return """
            {
              "externalOrderId":"%s",
              "warehouseCode":"WH-NORTH",
              "items":[{"productSku":"%s","quantity":%d,"unitPrice":10.00}]
            }
            """.formatted(externalOrderId, productSku, quantity);
    }

    private void assertCommittedState(String skuPrefix, String suffix, Long replayId, String replayOrderId) {
        transactionTemplate.executeWithoutResult(status -> {
            IntegrationReplayRecord failedReplay = replayRecordRepository.findById(replayId).orElseThrow();
            assertThat(failedReplay.getStatus()).isEqualTo(IntegrationReplayStatus.REPLAY_FAILED);
            assertThat(failedReplay.getReplayAttemptCount()).isEqualTo(1);
            assertThat(failedReplay.getFailureCode()).isEqualTo(IntegrationFailureCode.INSUFFICIENT_INVENTORY);
            assertThat(customerOrderRepository
                .findByTenant_CodeIgnoreCaseAndExternalOrderId("STARTER-OPS", replayOrderId))
                .isEmpty();

            var warehouse = warehouseRepository
                .findByTenant_CodeIgnoreCaseAndCode("STARTER-OPS", "WH-NORTH")
                .orElseThrow();
            for (int index = 0; index < HTTP_ORDER_WORKERS; index++) {
                String externalOrderId = "HTTP-ORDER-DEMAND-" + suffix + "-" + index;
                Product product = productRepository
                    .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase("STARTER-OPS", skuPrefix + index)
                    .orElseThrow();
                Inventory inventory = inventoryRepository
                    .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                    .orElseThrow();
                assertThat(inventory.getQuantityReserved()).isEqualTo(1L);
                assertThat(inventory.getQuantityAvailable()).isEqualTo(1L);
                assertThat(customerOrderRepository
                    .findByTenant_CodeIgnoreCaseAndExternalOrderId("STARTER-OPS", externalOrderId))
                    .isPresent();
                assertThat(fulfillmentTaskRepository
                    .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId("STARTER-OPS", externalOrderId))
                    .isPresent();
            }
            Product replayProduct = productRepository
                .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(
                    "STARTER-OPS", skuPrefix + HTTP_ORDER_WORKERS)
                .orElseThrow();
            Inventory replayInventory = inventoryRepository
                .findByProductIdAndWarehouseId(replayProduct.getId(), warehouse.getId())
                .orElseThrow();
            assertThat(replayInventory.getQuantityReserved()).isZero();
            assertThat(replayInventory.getQuantityAvailable()).isEqualTo(1L);
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
        ReplayOrderConnectionProbe replayOrderConnectionProbe(DataSource dataSource) throws Exception {
            return new ReplayOrderConnectionProbe(dataSource, dataSource.unwrap(HikariDataSource.class));
        }

        @Bean
        ReplayOrderConnectionAspect replayOrderConnectionAspect(ReplayOrderConnectionProbe probe) {
            return new ReplayOrderConnectionAspect(probe);
        }
    }

    static class ReplayOrderConnectionProbe {

        private final DataSource dataSource;
        private final HikariDataSource pool;
        private final AtomicInteger activeAtBarrier = new AtomicInteger();
        private final AtomicInteger idleAtBarrier = new AtomicInteger();
        private final AtomicInteger waitersAtBarrier = new AtomicInteger();
        private final AtomicInteger replayLockCalls = new AtomicInteger();
        private final AtomicBoolean everyWorkerTransactional = new AtomicBoolean(true);
        private final AtomicBoolean initialReplayCompleted = new AtomicBoolean();
        private final AtomicBoolean initialReplayCompletedBeforeFailureWrite = new AtomicBoolean();
        private volatile String skuPrefix;
        private volatile Long replayId;
        private volatile CyclicBarrier barrier;

        ReplayOrderConnectionProbe(DataSource dataSource, HikariDataSource pool) {
            this.dataSource = dataSource;
            this.pool = pool;
        }

        void arm(String skuPrefix, Long replayId, int workers) {
            this.skuPrefix = skuPrefix;
            this.replayId = replayId;
            activeAtBarrier.set(0);
            idleAtBarrier.set(0);
            waitersAtBarrier.set(0);
            replayLockCalls.set(0);
            everyWorkerTransactional.set(true);
            initialReplayCompleted.set(false);
            initialReplayCompletedBeforeFailureWrite.set(false);
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
                throw new IllegalStateException("Could not align Replay and Order transactions", exception);
            }
        }

        void observeReplayLock(Long recordId) {
            if (replayId == null || !replayId.equals(recordId)) {
                return;
            }
            everyWorkerTransactional.compareAndSet(
                true,
                TransactionSynchronizationManager.isActualTransactionActive()
                    && TransactionSynchronizationManager.hasResource(dataSource)
            );
            int invocation = replayLockCalls.incrementAndGet();
            if (invocation == 1) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        initialReplayCompleted.set(true);
                    }
                });
            } else if (invocation == 2) {
                initialReplayCompletedBeforeFailureWrite.set(initialReplayCompleted.get());
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

        int replayLockCalls() {
            return replayLockCalls.get();
        }

        boolean initialReplayCompletedBeforeFailureWrite() {
            return initialReplayCompletedBeforeFailureWrite.get();
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
            replayId = null;
            if (currentBarrier != null) {
                currentBarrier.reset();
            }
        }
    }

    @Aspect
    static class ReplayOrderConnectionAspect {

        private final ReplayOrderConnectionProbe probe;

        ReplayOrderConnectionAspect(ReplayOrderConnectionProbe probe) {
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

        @Around("execution(* com.synapsecore.domain.repository.IntegrationReplayRecordRepository.findByIdForUpdate(..))")
        Object observeReplayRecordLock(ProceedingJoinPoint joinPoint) throws Throwable {
            Object result = joinPoint.proceed();
            Object[] arguments = joinPoint.getArgs();
            if (arguments.length > 0 && arguments[0] instanceof Long recordId) {
                probe.observeReplayLock(recordId);
            }
            return result;
        }
    }
}
