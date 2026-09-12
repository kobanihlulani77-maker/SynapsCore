package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.sun.net.httpserver.HttpServer;
import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.integration.IntegrationScheduledPullWorkerService;
import com.zaxxer.hikari.HikariDataSource;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
    "spring.datasource.hikari.maximum-pool-size=10",
    "spring.datasource.hikari.minimum-idle=10",
    "spring.datasource.hikari.connection-timeout=2000",
    "spring.task.scheduling.enabled=false",
    "synapsecore.integration.pull-worker.enabled=false",
    "synapsecore.integration.pull-worker.allow-local-targets=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(OrderConnectionDemandIntegrationTest.ConnectionDemandTestConfiguration.class)
class ScheduledPullExternalDelayConnectionDemandIntegrationTest {

    private static final int ORDER_WORKERS = 10;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationScheduledPullWorkerService pullWorker;

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
    private DataSource dataSource;

    @Autowired
    private OrderConnectionDemandIntegrationTest.OrderConnectionHoldProbe orderProbe;

    @RepeatedTest(3)
    @Timeout(45)
    void slowExternalBodyHoldsNoConnectionWhileTenOrdersUseThePool() throws Exception {
        CountDownLatch responseHeadersSent = new CountDownLatch(1);
        CountDownLatch releaseResponseBody = new CountDownLatch(1);
        HttpServer server = startSlowResponseServer(responseHeadersSent, releaseResponseBody);
        String suffix = String.valueOf(System.nanoTime());
        String skuPrefix = "PULL-ORDER-DEMAND-" + suffix + "-";
        String source = "pull-demand-" + suffix;
        prepareFixtures(skuPrefix, source, server.getAddress().getPort());
        orderProbe.awaitCompleteRelease();
        orderProbe.arm(skuPrefix, ORDER_WORKERS);

        ExecutorService executor = Executors.newFixedThreadPool(ORDER_WORKERS + 1);
        try {
            Future<Integer> pull = executor.submit(() -> pullWorker.processDuePulls(1));
            assertThat(responseHeadersSent.await(10, TimeUnit.SECONDS)).isTrue();
            awaitPoolIdle();

            List<Future<Integer>> orders = new ArrayList<>();
            for (int index = 0; index < ORDER_WORKERS; index++) {
                int requestIndex = index;
                orders.add(executor.submit(() -> createOrder(skuPrefix, suffix, requestIndex)));
            }

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> order : orders) {
                statuses.add(order.get(30, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsOnly(201);
            assertThat(orderProbe.transactionActiveForEveryWorker()).isTrue();
            assertThat(orderProbe.activeAtBarrier()).isEqualTo(ORDER_WORKERS);
            assertThat(orderProbe.idleAtBarrier()).isZero();
            assertThat(orderProbe.waitersAtBarrier()).isLessThan(ORDER_WORKERS);

            releaseResponseBody.countDown();
            assertThat(pull.get(10, TimeUnit.SECONDS)).isEqualTo(1);
            assertConnectorCompleted(source);
            assertCommittedOrders(skuPrefix, suffix);
            orderProbe.awaitCompleteRelease();
            System.out.printf(
                "Scheduled pull delay orders=%d activeDuringBody=0 activeAtOrderBarrier=%d "
                    + "idleAtOrderBarrier=%d waitersAtOrderBarrier=%d connectorStatus=SUCCESS%n",
                statuses.size(),
                orderProbe.activeAtBarrier(),
                orderProbe.idleAtBarrier(),
                orderProbe.waitersAtBarrier()
            );
        } finally {
            releaseResponseBody.countDown();
            orderProbe.disarm();
            server.stop(0);
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private HttpServer startSlowResponseServer(CountDownLatch responseHeadersSent,
                                               CountDownLatch releaseResponseBody) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/orders", exchange -> {
            try {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, 0);
                responseHeadersSent.countDown();
                if (!releaseResponseBody.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release scheduled-pull response body");
                }
                byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    private void prepareFixtures(String skuPrefix, String source, int port) {
        transactionTemplate.executeWithoutResult(status -> {
            var tenant = tenantRepository.findByCodeIgnoreCase("STARTER-OPS").orElseThrow();
            var warehouse = warehouseRepository
                .findByTenant_CodeIgnoreCaseAndCode("STARTER-OPS", "WH-NORTH")
                .orElseThrow();
            connectorRepository.save(IntegrationConnector.builder()
                .tenant(tenant)
                .sourceSystem(source)
                .type(IntegrationConnectorType.WEBHOOK_ORDER)
                .displayName("Pull Demand " + source)
                .enabled(true)
                .syncMode(IntegrationSyncMode.SCHEDULED_PULL)
                .syncIntervalMinutes(15)
                .pullEndpointUrl("http://127.0.0.1:" + port + "/orders")
                .validationPolicy(IntegrationValidationPolicy.STANDARD)
                .transformationPolicy(IntegrationTransformationPolicy.NORMALIZE_CODES)
                .defaultWarehouseCode(warehouse.getCode())
                .build());

            for (int index = 0; index < ORDER_WORKERS; index++) {
                Product product = productRepository.save(Product.builder()
                    .tenant(tenant)
                    .catalogSku(skuPrefix + index)
                    .name("Pull Order Demand " + index)
                    .category("Verification")
                    .build());
                inventoryRepository.save(Inventory.builder()
                    .tenant(tenant)
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
        String externalOrderId = "PULL-HTTP-ORDER-" + suffix + "-" + index;
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
                .header("X-Request-Id", "pull-order-demand-" + suffix + "-" + index)
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andReturn()
            .getResponse()
            .getStatus();
    }

    private void awaitPoolIdle() throws Exception {
        HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (pool.getHikariPoolMXBean().getActiveConnections() == 0) {
                assertThat(pool.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
                return;
            }
            Thread.sleep(20);
        }
        assertThat(pool.getHikariPoolMXBean().getActiveConnections())
            .as("external scheduled-pull response-body delay must not retain JDBC")
            .isZero();
    }

    private void assertConnectorCompleted(String source) {
        IntegrationConnector connector = connectorRepository
            .findByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseAndType(
                "STARTER-OPS", source, IntegrationConnectorType.WEBHOOK_ORDER)
            .orElseThrow();
        assertThat(connector.getLastPullStatus()).isEqualTo("SUCCESS");
        assertThat(connector.getLastPullAttemptAt()).isNotNull();
        assertThat(connector.getLastPullSuccessAt()).isNotNull();
    }

    private void assertCommittedOrders(String skuPrefix, String suffix) {
        transactionTemplate.executeWithoutResult(status -> {
            var warehouse = warehouseRepository
                .findByTenant_CodeIgnoreCaseAndCode("STARTER-OPS", "WH-NORTH")
                .orElseThrow();
            for (int index = 0; index < ORDER_WORKERS; index++) {
                String externalOrderId = "PULL-HTTP-ORDER-" + suffix + "-" + index;
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
        });
    }

    private RequestPostProcessor accessHeaders(String actor, String... roles) {
        return request -> {
            request.addHeader("X-Synapse-Actor", actor);
            request.addHeader("X-Synapse-Roles", String.join(",", roles));
            return request;
        };
    }
}
