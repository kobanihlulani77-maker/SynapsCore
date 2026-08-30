package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.IntegrationInboundStatus;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderItem;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationImportRunRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.event.OperationalDispatchQueueService;
import com.synapsecore.event.OperationalUpdateType;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "synapsecore.starter.require-explicit-tenant-provisioning=true",
    "synapsecore.starter.seed-starter-inventory-on-tenant-onboarding=false",
    "synapsecore.starter.seed-starter-connectors-on-tenant-onboarding=false",
    "synapsecore.dashboard.cache-enabled=false",
    "synapsecore.integration.pull-worker.enabled=false",
    "synapsecore.integration.pull-worker.allow-local-targets=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Layer2Phase2IntakeReservationIntegrationTest {

    private static final String PLATFORM_ADMIN_TOKEN = "test-only-platform-admin-token";
    private static final String TENANT_A = "L2-P2-TENANT-A";
    private static final String TENANT_B = "L2-P2-TENANT-B";
    private static final String WH_A = "L2-P2-WH-A";
    private static final String WH_B = "L2-P2-WH-B";
    private static final String WH_B_ONLY = "L2-P2-WH-B-ONLY";
    private static final String ADMIN_PASSWORD = "Layer2Phase2Admin!2026";
    private static final String INITIAL_PASSWORD = "Layer2Phase2Initial!2026";
    private static final String ROLE_PASSWORD = "Layer2Phase2Role!2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private IntegrationConnectorRepository integrationConnectorRepository;

    @Autowired
    private IntegrationInboundRecordRepository inboundRecordRepository;

    @Autowired
    private IntegrationImportRunRepository importRunRepository;

    @Autowired
    private IntegrationReplayRecordRepository replayRecordRepository;

    @Autowired
    private BusinessEventRepository businessEventRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OperationalDispatchWorkItemRepository dispatchWorkItemRepository;

    @Autowired
    private OperationalDispatchQueueService dispatchQueueService;

    @Test
    void sourceIntakeCreatesOneReservedQueuedOrderAndFailuresRemainNonOperational() throws Exception {
        provisionTenant(tenantAPayload());
        provisionTenant(tenantBPayload());

        MockHttpSession tenantAdminA = login(TENANT_A, "l2.p2.admin", ADMIN_PASSWORD, "TENANT_ADMIN", List.of());
        MockHttpSession integrationAdminA = login(TENANT_A, "l2.p2.integration.admin", INITIAL_PASSWORD,
            "INTEGRATION_ADMIN", List.of(WH_A));
        MockHttpSession integrationOperatorA = login(TENANT_A, "l2.p2.integration.operator", INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(WH_A));
        MockHttpSession integrationOperatorA2 = login(TENANT_A, "l2.p2.integration.operator.two", INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(WH_A));
        MockHttpSession tenantAdminB = login(TENANT_B, "l2.p2.admin", ADMIN_PASSWORD, "TENANT_ADMIN", List.of());

        createProduct(tenantAdminA, "L2-SKU-A", "Layer 2 Phase 2 Product A");
        createProduct(tenantAdminA, "L2-SKU-B", "Layer 2 Phase 2 Product B");
        createProduct(tenantAdminA, "L2-SKU-C", "Layer 2 Phase 2 Product C");
        createProduct(tenantAdminA, "L2-SKU-NOINV", "Layer 2 Phase 2 No Inventory Product");
        createProduct(tenantAdminA, "L2-SKU-LOW", "Layer 2 Phase 2 Low Stock Product");
        createProduct(tenantAdminA, "L2-SKU-CONCURRENT", "Layer 2 Phase 2 Concurrent Product");
        createProduct(tenantAdminB, "L2-SKU-A", "Tenant B Same SKU Product");

        updateInventory(tenantAdminA, "L2-SKU-A", WH_A, 100, 10);
        updateInventory(tenantAdminA, "L2-SKU-B", WH_A, 80, 10);
        updateInventory(tenantAdminA, "L2-SKU-C", WH_A, 60, 10);
        updateInventory(tenantAdminA, "L2-SKU-CONCURRENT", WH_A, 20, 2);
        updateInventory(tenantAdminA, "L2-SKU-LOW", WH_A, 5, 1);
        updateInventory(tenantAdminA, "L2-SKU-A", WH_B, 100, 10);
        updateInventory(tenantAdminA, "L2-SKU-B", WH_B, 80, 10);
        updateInventory(tenantAdminA, "L2-SKU-C", WH_B, 60, 10);
        updateInventory(tenantAdminB, "L2-SKU-A", WH_B_ONLY, 40, 5);

        createConnector(integrationAdminA, "l2-p2-webhook-a", "WEBHOOK_ORDER", true, "REALTIME_PUSH", WH_A);
        createConnector(integrationAdminA, "l2-p2-csv-a", "CSV_ORDER_IMPORT", true, "BATCH_FILE_DROP", WH_A);
        createConnector(integrationAdminA, "l2-p2-disabled-csv-a", "CSV_ORDER_IMPORT", false, "BATCH_FILE_DROP", WH_A);

        Stock beforeA = stock("L2-SKU-A", WH_A);
        Stock beforeB = stock("L2-SKU-B", WH_A);
        Stock beforeC = stock("L2-SKU-C", WH_A);
        Stock beforeOtherWarehouseA = stock("L2-SKU-A", WH_B);
        long ordersBefore = customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A);
        long fulfillmentBefore = tenantFulfillmentCount(TENANT_A);
        mockMvc.perform(post("/api/integrations/orders/webhook")
                .session(integrationOperatorA)
                .contentType(APPLICATION_JSON)
                .content(webhookPayload("l2-p2-webhook-a", "L2-P2-HAPPY-001", WH_A,
                    "L2-SKU-A", 8, "L2-SKU-B", 6)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.order.externalOrderId").value("L2-P2-HAPPY-001"))
            .andExpect(jsonPath("$.order.warehouseCode").value(WH_A))
            .andExpect(jsonPath("$.order.status").value(OrderStatus.RECEIVED.name()))
            .andExpect(jsonPath("$.order.items.length()").value(2));

        CustomerOrder primaryOrder = order("L2-P2-HAPPY-001");
        assertThat(primaryOrder.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(primaryOrder.getWarehouse().getCode()).isEqualTo(WH_A);
        assertThat(primaryOrder.getItems()).extracting(item -> item.getProduct().getCatalogSku())
            .containsExactlyInAnyOrder("L2-SKU-A", "L2-SKU-B");
        assertThat(primaryOrder.getItems()).allSatisfy(item -> {
            assertThat(item.getQuantity()).isIn(6, 8);
            assertThat(item.getReservedQuantity()).isEqualTo(item.getQuantity());
            assertThat(item.getFulfilledQuantity()).isZero();
            assertThat(item.getCancelledQuantity()).isZero();
            assertThat(item.getReturnedQuantity()).isZero();
        });
        assertReservation(beforeA, stock("L2-SKU-A", WH_A), 8);
        assertReservation(beforeB, stock("L2-SKU-B", WH_A), 6);
        assertThat(stock("L2-SKU-C", WH_A)).isEqualTo(beforeC);
        assertThat(stock("L2-SKU-A", WH_B)).isEqualTo(beforeOtherWarehouseA);
        assertThat(stock("L2-SKU-A", WH_A).onHand()).isEqualTo(100);
        assertThat(stock("L2-SKU-B", WH_A).onHand()).isEqualTo(80);

        var primaryTask = fulfillmentTaskRepository
            .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(TENANT_A, "L2-P2-HAPPY-001")
            .orElseThrow();
        assertThat(primaryTask.getStatus()).isEqualTo(FulfillmentStatus.QUEUED);
        assertThat(primaryTask.getWarehouse().getCode()).isEqualTo(WH_A);
        assertThat(primaryTask.getTotalUnits()).isEqualTo(14);
        assertThat(primaryTask.getFulfilledUnits()).isZero();
        assertThat(tenantFulfillmentCount(TENANT_A)).isEqualTo(fulfillmentBefore + 1);
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A)).isEqualTo(ordersBefore + 1);

        assertThat(inboundRecordRepository.findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeOrderByCreatedAtDesc(
                TENANT_A, "l2-p2-webhook-a", com.synapsecore.domain.entity.IntegrationConnectorType.WEBHOOK_ORDER))
            .get().extracting(record -> record.getStatus()).isEqualTo(IntegrationInboundStatus.ACCEPTED);
        assertThat(businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(TENANT_A))
            .anyMatch(event -> event.getEventType() == BusinessEventType.ORDER_INGESTED
                && event.getPayloadSummary().contains("L2-P2-HAPPY-001"));
        assertThat(auditLogRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(TENANT_A))
            .anyMatch(audit -> audit.getAction().equals("ORDER_PROCESSED")
                && audit.getStatus() == AuditStatus.SUCCESS
                && audit.getTargetRef().equals("L2-P2-HAPPY-001"));
        assertThat(dispatchWorkItemRepository.findAll()).anyMatch(item ->
            item.getTenantCode().equalsIgnoreCase(TENANT_A)
                && item.getUpdateType() == OperationalUpdateType.ORDER_FLOW
                && item.getSource().equals("integration-webhook:l2-p2-webhook-a")
                && item.getStatus() != OperationalDispatchStatus.FAILED);
        assertThat(dispatchWorkItemRepository.findAll().stream()
            .filter(item -> item.getTenantCode().equalsIgnoreCase(TENANT_A))
            .filter(item -> item.getUpdateType() == OperationalUpdateType.ORDER_FLOW)
            .count()).isPositive();

        JsonNode dashboardAfterWebhook = readJson(get("/api/dashboard/summary").session(tenantAdminA));
        assertThat(dashboardAfterWebhook.path("totalOrders").asLong())
            .isEqualTo(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A));
        assertThat(dashboardAfterWebhook.path("fulfillmentBacklogCount").asLong()).isEqualTo(1);

        String csv = "externalOrderId,warehouseCode,productSku,quantity,unitPrice\n"
            + "L2-P2-CSV-001," + WH_A + ",L2-SKU-C,4,18.00\n"
            + "L2-P2-CSV-002," + WH_A + ",L2-SKU-A,2,18.00\n"
            + "L2-P2-CSV-002," + WH_A + ",L2-SKU-B,3,18.00\n"
            + "L2-P2-CSV-003," + WH_A + ",L2-SKU-MISSING,1,18.00\n";
        MockMultipartFile csvFile = new MockMultipartFile("file", "layer2-phase2.csv", "text/csv",
            csv.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/integrations/orders/csv-import")
                .file(csvFile)
                .session(integrationOperatorA)
                .param("sourceSystem", "l2-p2-csv-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rowsReceived").value(4))
            .andExpect(jsonPath("$.ordersImported").value(2))
            .andExpect(jsonPath("$.ordersFailed").value(1))
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("PRODUCT_NOT_FOUND"));
        assertThat(order("L2-P2-CSV-001").getItems()).hasSize(1);
        assertThat(order("L2-P2-CSV-002").getItems()).hasSize(2);
        assertThat(stock("L2-SKU-C", WH_A).reserved()).isEqualTo(4);
        assertThat(stock("L2-SKU-A", WH_A).reserved()).isEqualTo(10);
        assertThat(stock("L2-SKU-B", WH_A).reserved()).isEqualTo(9);
        var csvImport = importRunRepository
            .findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeOrderByCreatedAtDesc(
                TENANT_A, "l2-p2-csv-a", com.synapsecore.domain.entity.IntegrationConnectorType.CSV_ORDER_IMPORT)
            .orElseThrow();
        assertThat(csvImport.getOrdersImported()).isEqualTo(2);
        assertThat(csvImport.getOrdersFailed()).isEqualTo(1);
        assertThat(csvImport.getStatus()).isEqualTo(com.synapsecore.domain.entity.IntegrationImportStatus.PARTIAL_SUCCESS);

        mockMvc.perform(post("/api/orders")
                .session(integrationOperatorA)
                .contentType(APPLICATION_JSON)
                .content(orderPayload("L2-P2-DIRECT-001", WH_A, "L2-SKU-C", 5)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.externalOrderId").value("L2-P2-DIRECT-001"))
            .andExpect(jsonPath("$.status").value(OrderStatus.RECEIVED.name()));
        assertThat(stock("L2-SKU-C", WH_A).reserved()).isEqualTo(9);
        assertThat(fulfillmentTaskRepository
            .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(TENANT_A, "L2-P2-DIRECT-001"))
            .get().extracting(task -> task.getStatus()).isEqualTo(FulfillmentStatus.QUEUED);

        exerciseScheduledPull();
        assertThat(order("L2-P2-PULL-001").getWarehouse().getCode()).isEqualTo(WH_A);
        assertThat(stock("L2-SKU-C", WH_A).reserved()).isEqualTo(10);
        assertThat(integrationConnectorRepository.findByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseAndType(
                TENANT_A, "l2-p2-pull-a", com.synapsecore.domain.entity.IntegrationConnectorType.WEBHOOK_ORDER))
            .get().extracting(connector -> connector.getLastPullStatus()).isEqualTo("SUCCESS");

        long ordersBeforeDuplicate = customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A);
        Stock duplicateStock = stock("L2-SKU-A", WH_A);
        mockMvc.perform(post("/api/integrations/orders/webhook")
                .session(integrationOperatorA)
                .contentType(APPLICATION_JSON)
                .content(webhookPayload("l2-p2-webhook-a", "L2-P2-HAPPY-001", WH_A,
                    "L2-SKU-A", 8, null, 0)))
            .andExpect(status().isConflict());
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A)).isEqualTo(ordersBeforeDuplicate);
        assertThat(stock("L2-SKU-A", WH_A)).isEqualTo(duplicateStock);
        assertThat(replayRecordRepository.findAll()).noneMatch(record ->
            record.getExternalOrderId().equals("L2-P2-HAPPY-001"));

        assertConcurrentDuplicate(integrationOperatorA, integrationOperatorA2);

        assertFailedCsv(integrationOperatorA, "L2-P2-MISSING-PRODUCT", "L2-SKU-UNKNOWN", "PRODUCT_NOT_FOUND");
        assertFailedWebhook(integrationOperatorA, "L2-P2-MISSING-INVENTORY", "L2-SKU-NOINV", "INVENTORY_NOT_FOUND");
        assertFailedWebhook(integrationOperatorA, "L2-P2-INSUFFICIENT", "L2-SKU-LOW", "INSUFFICIENT_INVENTORY");
        assertFailedCsv(integrationOperatorA, "L2-P2-DISABLED", "L2-SKU-A", "CONNECTOR_DISABLED", "l2-p2-disabled-csv-a");

        assertThat(replayRecordRepository.findAll()).filteredOn(record -> record.getTenantCode().equalsIgnoreCase(TENANT_A))
            .allSatisfy(record -> assertThat(record.getStatus()).isEqualTo(IntegrationReplayStatus.PENDING));
        assertThat(replayRecordRepository.findAll()).filteredOn(record ->
            Set.of("L2-P2-MISSING-PRODUCT", "L2-P2-MISSING-INVENTORY", "L2-P2-INSUFFICIENT", "L2-P2-DISABLED")
                .contains(record.getExternalOrderId()))
            .extracting(record -> record.getFailureCode().name())
            .containsExactlyInAnyOrder("PRODUCT_NOT_FOUND", "INVENTORY_NOT_FOUND", "INSUFFICIENT_INVENTORY", "CONNECTOR_DISABLED");

        assertFailedDirectOrder(integrationOperatorA, "L2-P2-WRONG-WAREHOUSE", WH_B, "L2-SKU-A", 1);
        assertThat(customerOrderRepository.findAllByTenant_CodeIgnoreCaseAndWarehouse_CodeIgnoreCase(TENANT_A, WH_B))
            .isEmpty();

        Stock atomicBefore = stock("L2-SKU-A", WH_A);
        long atomicOrdersBefore = customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A);
        assertFailedDirectOrder(integrationOperatorA, "L2-P2-ATOMIC-FAILURE", WH_A, "L2-SKU-A", 5,
            "L2-SKU-UNKNOWN", 1);
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A)).isEqualTo(atomicOrdersBefore);
        assertThat(stock("L2-SKU-A", WH_A)).isEqualTo(atomicBefore);
        assertThat(tenantTasks(TENANT_A)).noneMatch(task ->
            task.getCustomerOrder().getExternalOrderId().equals("L2-P2-ATOMIC-FAILURE"));

        long tenantBOrdersBefore = customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_B);
        Stock tenantBStockBefore = stockForTenant(TENANT_B, "L2-SKU-A", WH_B_ONLY);
        mockMvc.perform(post("/api/orders")
                .session(integrationOperatorA)
                .contentType(APPLICATION_JSON)
                .content(orderPayload("L2-P2-CROSS-TENANT", WH_B_ONLY, "L2-SKU-A", 1)))
            .andExpect(status().is4xxClientError());
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_B)).isEqualTo(tenantBOrdersBefore);
        assertThat(stockForTenant(TENANT_B, "L2-SKU-A", WH_B_ONLY)).isEqualTo(tenantBStockBefore);
        assertThat(customerOrderRepository.findAllByTenant_CodeIgnoreCaseAndWarehouse_CodeIgnoreCase(TENANT_A, WH_B))
            .isEmpty();

        JsonNode dashboardAfterFailures = readJson(get("/api/dashboard/summary").session(tenantAdminA));
        assertThat(dashboardAfterFailures.path("totalOrders").asLong())
            .isEqualTo(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A));
        assertThat(dashboardAfterFailures.path("totalOrders").asLong()).isGreaterThan(0);
        assertThat(businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(TENANT_A))
            .noneMatch(event -> event.getEventType() == BusinessEventType.ORDER_INGESTED
                && event.getPayloadSummary().contains("L2-P2-ATOMIC-FAILURE"));
        assertThat(auditLogRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(TENANT_A))
            .noneMatch(audit -> audit.getAction().equals("ORDER_PROCESSED")
                && audit.getTargetRef().equals("L2-P2-ATOMIC-FAILURE"));

        int dispatched = dispatchQueueService.processPendingWork();
        assertThat(dispatched).isGreaterThanOrEqualTo(0);
        List<OperationalDispatchStatus> tenantAOrderFlowStatuses = dispatchWorkItemRepository.findAll().stream()
            .filter(item -> item.getTenantCode().equalsIgnoreCase(TENANT_A))
            .filter(item -> item.getUpdateType() == OperationalUpdateType.ORDER_FLOW)
            .map(com.synapsecore.domain.entity.OperationalDispatchWorkItem::getStatus)
            .toList();
        assertThat(tenantAOrderFlowStatuses)
            .as("Tenant A ORDER_FLOW dispatch statuses")
            .containsOnly(OperationalDispatchStatus.COMPLETED);
    }

    private void exerciseScheduledPull() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/orders", exchange -> {
            byte[] payload = ("[{\"externalOrderId\":\"L2-P2-PULL-001\",\"warehouseCode\":\"\","
                + "\"customerReference\":\"L2-P2-PULL-CUSTOMER\",\"occurredAt\":\"2026-08-30T10:30:00Z\","
                + "\"items\":[{\"productSku\":\"L2-SKU-C\",\"quantity\":1,\"unitPrice\":18.00}]}]")
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/orders";
            MockHttpSession integrationAdmin = login(TENANT_A, "l2.p2.integration.admin", ROLE_PASSWORD,
                "INTEGRATION_ADMIN", List.of(WH_A));
            createConnector(integrationAdmin, "l2-p2-pull-a", "WEBHOOK_ORDER", true, "SCHEDULED_PULL", WH_A, endpoint);
            int processed = scheduledPullWorker().processDuePulls(5);
            assertThat(processed).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Autowired
    private com.synapsecore.integration.IntegrationScheduledPullWorkerService scheduledPullWorker;

    private com.synapsecore.integration.IntegrationScheduledPullWorkerService scheduledPullWorker() {
        return scheduledPullWorker;
    }

    private void assertConcurrentDuplicate(MockHttpSession sessionOne, MockHttpSession sessionTwo) throws Exception {
        String externalId = "L2-P2-CONCURRENT-DUPLICATE";
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> first = executor.submit(() -> performConcurrentWebhook(sessionOne, externalId, start));
            Future<MvcResult> second = executor.submit(() -> performConcurrentWebhook(sessionTwo, externalId, start));
            start.countDown();
            MvcResult firstResult = first.get(20, TimeUnit.SECONDS);
            MvcResult secondResult = second.get(20, TimeUnit.SECONDS);
            List<Integer> statuses = List.of(firstResult.getResponse().getStatus(), secondResult.getResponse().getStatus());
            assertThat(statuses).contains(201);
            assertThat(statuses).allMatch(status -> status >= 200 && status < 500);
            assertThat(customerOrderRepository.existsByTenant_CodeIgnoreCaseAndExternalOrderId(TENANT_A, externalId))
                .isTrue();
            assertThat(stock("L2-SKU-CONCURRENT", WH_A).reserved()).isEqualTo(3);
            assertThat(tenantTasks(TENANT_A).stream()
                .filter(task -> task.getCustomerOrder().getExternalOrderId().equals(externalId))).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private MvcResult performConcurrentWebhook(MockHttpSession session, String externalId, CountDownLatch start)
        throws Exception {
        start.await(10, TimeUnit.SECONDS);
        return mockMvc.perform(post("/api/integrations/orders/webhook")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content(webhookPayload("l2-p2-webhook-a", externalId, WH_A,
                    "L2-SKU-CONCURRENT", 3, null, 0)))
            .andReturn();
    }

    private void assertFailedCsv(MockHttpSession session, String externalId, String sku, String expectedCode)
        throws Exception {
        assertFailedCsv(session, externalId, sku, expectedCode, "l2-p2-csv-a");
    }

    private void assertFailedCsv(MockHttpSession session, String externalId, String sku, String expectedCode,
                                 String sourceSystem) throws Exception {
        String csv = "externalOrderId,warehouseCode,productSku,quantity,unitPrice\n"
            + externalId + "," + WH_A + "," + sku + ",1,18.00\n";
        MockMultipartFile file = new MockMultipartFile("file", externalId + ".csv", "text/csv",
            csv.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/integrations/orders/csv-import")
                .file(file).session(session).param("sourceSystem", sourceSystem))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordersImported").value(0))
            .andExpect(jsonPath("$.ordersFailed").value(1))
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value(expectedCode));
        assertFalseOperational(externalId);
    }

    private void assertFailedWebhook(MockHttpSession session, String externalId, String sku, String expectedCode)
        throws Exception {
        long orderCount = customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A);
        mockMvc.perform(post("/api/integrations/orders/webhook")
                .session(session).contentType(APPLICATION_JSON)
                .content(webhookPayload("l2-p2-webhook-a", externalId, WH_A, sku, 8, null, 0)))
            .andExpect(status().is4xxClientError());
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A)).isEqualTo(orderCount);
        assertFalseOperational(externalId);
        assertThat(replayRecordRepository.findAll()).anyMatch(record ->
            record.getExternalOrderId().equals(externalId) && record.getFailureCode().name().equals(expectedCode));
    }

    private void assertFailedDirectOrder(MockHttpSession session, String externalId, String warehouse, String sku,
                                         int quantity, Object... secondLine) throws Exception {
        StringBuilder payload = new StringBuilder(orderPayload(externalId, warehouse, sku, quantity));
        if (secondLine.length > 0) {
            payload = new StringBuilder("{\"externalOrderId\":\"" + externalId + "\",\"warehouseCode\":\""
                + warehouse + "\",\"items\":[{\"productSku\":\"" + sku + "\",\"quantity\":" + quantity
                + ",\"unitPrice\":18.00},{\"productSku\":\"" + secondLine[0] + "\",\"quantity\":"
                + secondLine[1] + ",\"unitPrice\":18.00}]}");
        }
        mockMvc.perform(post("/api/orders").session(session).contentType(APPLICATION_JSON).content(payload.toString()))
            .andExpect(status().is4xxClientError());
        assertFalseOperational(externalId);
    }

    private void assertFalseOperational(String externalId) {
        assertThat(customerOrderRepository.existsByTenant_CodeIgnoreCaseAndExternalOrderId(TENANT_A, externalId))
            .isFalse();
        assertThat(tenantTasks(TENANT_A).stream())
            .noneMatch(task -> task.getCustomerOrder().getExternalOrderId().equals(externalId));
        assertThat(businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(TENANT_A))
            .noneMatch(event -> event.getEventType() == BusinessEventType.ORDER_INGESTED
                && event.getPayloadSummary().contains(externalId));
        assertThat(auditLogRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(TENANT_A))
            .noneMatch(audit -> audit.getAction().equals("ORDER_PROCESSED") && audit.getTargetRef().equals(externalId));
    }

    private CustomerOrder order(String externalId) {
        return customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(TENANT_A, externalId).orElseThrow();
    }

    private long tenantFulfillmentCount(String tenantCode) {
        return tenantTasks(tenantCode).size();
    }

    private List<com.synapsecore.domain.entity.FulfillmentTask> tenantTasks(String tenantCode) {
        return fulfillmentTaskRepository.findAllByTenant_CodeIgnoreCaseAndStatusInOrderByUpdatedAtDesc(
            tenantCode, List.of(FulfillmentStatus.values()));
    }

    private Stock stock(String sku, String warehouse) {
        return stockForTenant(TENANT_A, sku, warehouse);
    }

    private Stock stockForTenant(String tenantCode, String sku, String warehouse) {
        Product product = productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(tenantCode, sku).orElseThrow();
        return inventoryRepository.findByProductIdAndWarehouseId(product.getId(),
                warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(tenantCode, warehouse).orElseThrow().getId())
            .map(item -> new Stock(item.getQuantityOnHand(), item.getQuantityReserved(), item.getQuantityAvailable()))
            .orElseGet(() -> new Stock(0, 0, 0));
    }

    private void assertReservation(Stock before, Stock after, long reserved) {
        assertThat(after.onHand()).isEqualTo(before.onHand());
        assertThat(after.reserved()).isEqualTo(before.reserved() + reserved);
        assertThat(after.available()).isEqualTo(before.available() - reserved);
    }

    private void provisionTenant(String payload) throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON).content(payload))
            .andExpect(status().isOk());
    }

    private MockHttpSession login(String tenantCode, String username, String password, String expectedRole,
                                  List<String> scopes) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON).content(loginPayload(tenantCode, username, password)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem(expectedRole))).andReturn();
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        if (body.path("passwordChangeRequired").asBoolean()) {
            MvcResult changed = mockMvc.perform(post("/api/auth/session/password").session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}"
                        .formatted(password, ROLE_PASSWORD)))
                .andExpect(status().isOk()).andReturn();
            session = (MockHttpSession) changed.getRequest().getSession(false);
        }
        JsonNode sessionBody = readJson(get("/api/auth/session").session(session));
        assertThat(arrayTexts(sessionBody.path("warehouseScopes"))).containsExactlyElementsOf(scopes);
        return session;
    }

    private void createProduct(MockHttpSession session, String sku, String name) throws Exception {
        mockMvc.perform(post("/api/products").session(session).contentType(APPLICATION_JSON)
                .content("{\"sku\":\"%s\",\"name\":\"%s\",\"category\":\"Layer 2\"}"
                    .formatted(sku, name)))
            .andExpect(status().isCreated());
    }

    private void updateInventory(MockHttpSession session, String sku, String warehouse, long quantity, long threshold)
        throws Exception {
        mockMvc.perform(post("/api/inventory/update").session(session).contentType(APPLICATION_JSON)
                .content("{\"productSku\":\"%s\",\"warehouseCode\":\"%s\",\"quantityAvailable\":%d,\"reorderThreshold\":%d}"
                    .formatted(sku, warehouse, quantity, threshold)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.quantityOnHand").value(quantity))
            .andExpect(jsonPath("$.quantityReserved").value(0)).andExpect(jsonPath("$.quantityAvailable").value(quantity));
    }

    private void createConnector(MockHttpSession session, String source, String type, boolean enabled, String syncMode,
                                 String warehouse) throws Exception {
        createConnector(session, source, type, enabled, syncMode, warehouse, null);
    }

    private void createConnector(MockHttpSession session, String source, String type, boolean enabled, String syncMode,
                                 String warehouse, String endpoint) throws Exception {
        String pull = endpoint == null ? "" : ",\"pullEndpointUrl\":\"" + endpoint + "\",\"syncIntervalMinutes\":15";
        mockMvc.perform(post("/api/integrations/orders/connectors").session(session).contentType(APPLICATION_JSON)
                .content("{\"sourceSystem\":\"%s\",\"type\":\"%s\",\"displayName\":\"Layer 2 %s\",\"enabled\":%s,\"syncMode\":\"%s\",\"validationPolicy\":\"STANDARD\",\"transformationPolicy\":\"NORMALIZE_CODES\",\"allowDefaultWarehouseFallback\":true,\"defaultWarehouseCode\":\"%s\"%s}"
                    .formatted(source, type, source, enabled, syncMode, warehouse, pull)))
            .andExpect(status().isOk());
    }

    private JsonNode readJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
        throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String loginPayload(String tenant, String username, String password) {
        return "{\"tenantCode\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}"
            .formatted(tenant, username, password);
    }

    private String webhookPayload(String source, String externalId, String warehouse, String skuOne, int quantityOne,
                                  String skuTwo, int quantityTwo) {
        String second = skuTwo == null ? "" : ",\"items\":[{\"productSku\":\"%s\",\"quantity\":%d,\"unitPrice\":18.00},{\"productSku\":\"%s\",\"quantity\":%d,\"unitPrice\":18.00}]"
            .formatted(skuOne, quantityOne, skuTwo, quantityTwo);
        String items = skuTwo == null ? "\"items\":[{\"productSku\":\"%s\",\"quantity\":%d,\"unitPrice\":18.00}]"
            .formatted(skuOne, quantityOne) : second.substring(1);
        return "{\"sourceSystem\":\"%s\",\"externalOrderId\":\"%s\",\"warehouseCode\":\"%s\",\"customerReference\":\"L2-P2-CUSTOMER\",\"occurredAt\":\"2026-08-30T10:30:00Z\",%s}"
            .formatted(source, externalId, warehouse, items);
    }

    private String orderPayload(String externalId, String warehouse, String sku, int quantity) {
        return "{\"externalOrderId\":\"%s\",\"warehouseCode\":\"%s\",\"items\":[{\"productSku\":\"%s\",\"quantity\":%d,\"unitPrice\":18.00}]}"
            .formatted(externalId, warehouse, sku, quantity);
    }

    private List<String> arrayTexts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private String tenantAPayload() {
        return """
            {
              "tenantCode":"L2-P2-TENANT-A",
              "tenantName":"Layer 2 Phase 2 Tenant A",
              "description":"Disposable Layer 2 Phase 2 intake tenant.",
              "adminFullName":"Layer 2 Phase 2 Admin",
              "adminUsername":"l2.p2.admin",
              "adminPassword":"Layer2Phase2Admin!2026",
              "primaryLocation":"Layer 2 Region A",
              "warehouses":[
                {"code":"L2-P2-WH-A","name":"Layer 2 Phase 2 Warehouse A","location":"Region A"},
                {"code":"L2-P2-WH-B","name":"Layer 2 Phase 2 Warehouse B","location":"Region B"}
              ],
              "users":[
                {"username":"l2.p2.admin","fullName":"Layer 2 Phase 2 Admin","operatorActorName":"Layer 2 Phase 2 Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[]},
                {"username":"l2.p2.integration.admin","fullName":"Layer 2 Phase 2 Integration Admin","operatorActorName":"Layer 2 Phase 2 Integration Admin","roles":["INTEGRATION_ADMIN"],"warehouseScopes":["L2-P2-WH-A"],"initialPassword":"Layer2Phase2Initial!2026"},
                {"username":"l2.p2.integration.operator","fullName":"Layer 2 Phase 2 Integration Operator","operatorActorName":"Layer 2 Phase 2 Integration Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["L2-P2-WH-A"],"initialPassword":"Layer2Phase2Initial!2026"},
                {"username":"l2.p2.integration.operator.two","fullName":"Layer 2 Phase 2 Integration Operator Two","operatorActorName":"Layer 2 Phase 2 Integration Operator Two","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["L2-P2-WH-A"],"initialPassword":"Layer2Phase2Initial!2026"}
              ],
              "requiredRoles":[]
            }
            """;
    }

    private String tenantBPayload() {
        return """
            {
              "tenantCode":"L2-P2-TENANT-B",
              "tenantName":"Layer 2 Phase 2 Tenant B",
              "description":"Disposable Layer 2 Phase 2 isolation tenant.",
              "adminFullName":"Layer 2 Phase 2 Tenant B Admin",
              "adminUsername":"l2.p2.admin",
              "adminPassword":"Layer2Phase2Admin!2026",
              "primaryLocation":"Layer 2 Region B",
              "warehouses":[{"code":"L2-P2-WH-B-ONLY","name":"Layer 2 Phase 2 Tenant B Warehouse","location":"Region B"}],
              "users":[{"username":"l2.p2.admin","fullName":"Layer 2 Phase 2 Tenant B Admin","operatorActorName":"Layer 2 Phase 2 Tenant B Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[]}],
              "requiredRoles":[]
            }
            """;
    }

    private record Stock(long onHand, long reserved, long available) {
    }
}
