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
import com.synapsecore.config.SynapsePlatformOwnerProperties;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.IntegrationInboundStatus;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.RecommendationStatus;
import com.synapsecore.domain.entity.ScenarioRun;
import com.synapsecore.domain.entity.ScenarioRunType;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.event.OperationalDispatchQueueService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    "synapsecore.bootstrap.platform-admin-token=l2-p7-platform-token",
    "synapsecore.bootstrap.initial-token=l2-p7-bootstrap-token",
    "synapsecore.platform-owner.username=l2-p7-platform-owner",
    "synapsecore.platform-owner.password-hash=configured-by-test-fixture",
    "synapsecore.platform-owner.display-name=Layer 2 Phase 7 Platform Owner",
    "synapsecore.realtime.broker-mode=SIMPLE_IN_MEMORY"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Layer2Phase7FullOperationalAcceptanceIntegrationTest {

    private static final String TENANT_A = "L2-P7-ACCEPT-A";
    private static final String TENANT_B = "L2-P7-ACCEPT-B";
    private static final String NORTH = "L2-P7-WH-NORTH";
    private static final String SOUTH = "L2-P7-WH-SOUTH";
    private static final String BETA = "L2-P7-WH-BETA";
    private static final String ADMIN_PASSWORD = "L2-P7-Admin-2026!";
    private static final String INITIAL_PASSWORD = "L2-P7-Initial-2026!";
    private static final String ROLE_PASSWORD = "L2-P7-Role-2026!";
    private static final String WEBHOOK_TOKEN = "l2-p7-webhook-token";
    private static final String PLATFORM_PASSWORD = "L2-P7-Platform-2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private IntegrationInboundRecordRepository inboundRecordRepository;

    @Autowired
    private IntegrationReplayRecordRepository replayRecordRepository;

    @Autowired
    private ScenarioRunRepository scenarioRunRepository;

    @Autowired
    private BusinessEventRepository businessEventRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private OperationalDispatchWorkItemRepository dispatchWorkItemRepository;

    @Autowired
    private OperationalDispatchQueueService dispatchQueueService;

    @Autowired
    private SynapsePlatformOwnerProperties platformOwnerProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void completeOperationalStoryPreservesAuthorityAndTruthAcrossTheSystem() throws Exception {
        platformOwnerProperties.setPasswordHash(passwordEncoder.encode(PLATFORM_PASSWORD));
        provisionTenant(tenantAPayload());
        provisionTenant(tenantBPayload());

        MockHttpSession tenantAdmin = login(TENANT_A, "p7.tenant.admin", ADMIN_PASSWORD, "TENANT_ADMIN", List.of());
        MockHttpSession tenantBAdmin = login(TENANT_B, "p7.beta.admin", ADMIN_PASSWORD, "TENANT_ADMIN", List.of());
        MockHttpSession northIntegrationAdmin = login(TENANT_A, "p7.north.integration.admin", INITIAL_PASSWORD,
            "INTEGRATION_ADMIN", List.of(NORTH));
        MockHttpSession northIntegrationOperator = login(TENANT_A, "p7.north.integration.operator", INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(NORTH));
        MockHttpSession northReviewOwner = login(TENANT_A, "p7.north.review", INITIAL_PASSWORD,
            "REVIEW_OWNER", List.of(NORTH));
        MockHttpSession northFinalApprover = login(TENANT_A, "p7.north.final", INITIAL_PASSWORD,
            "FINAL_APPROVER", List.of(NORTH));
        MockHttpSession northEscalationOwner = login(TENANT_A, "p7.north.escalation", INITIAL_PASSWORD,
            "ESCALATION_OWNER", List.of(NORTH));
        MockHttpSession southIntegrationOperator = login(TENANT_A, "p7.south.integration.operator", INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(SOUTH));
        MockHttpSession tenantBOperator = login(TENANT_B, "p7.beta.integration.operator", INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(BETA));

        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A)).isZero();
        assertThat(inventoryRepository.countByTenantCode(TENANT_A)).isZero();
        assertThat(tenantFulfillmentCount()).isZero();
        assertThat(replayRecordRepository.findAll().stream()
            .filter(record -> TENANT_A.equalsIgnoreCase(record.getTenantCode())).count()).isZero();

        for (String sku : List.of("P7-SKU-A", "P7-SKU-B", "P7-SKU-C", "P7-SKU-RECOVERY")) {
            createProduct(tenantAdmin, sku);
        }
        assertThat(inventoryRepository.countByTenantCode(TENANT_A)).isZero();
        updateInventory(tenantAdmin, "P7-SKU-A", NORTH, 100, 20);
        updateInventory(tenantAdmin, "P7-SKU-B", NORTH, 80, 20);
        updateInventory(tenantAdmin, "P7-SKU-C", NORTH, 60, 20);
        updateInventory(tenantAdmin, "P7-SKU-A", SOUTH, 50, 10);
        updateInventory(tenantAdmin, "P7-SKU-B", SOUTH, 50, 10);
        updateInventory(tenantAdmin, "P7-SKU-C", SOUTH, 50, 10);
        createProduct(tenantBAdmin, "P7-SKU-A");
        updateInventory(tenantBAdmin, "P7-SKU-A", BETA, 70, 10);

        createConnector(northIntegrationAdmin, "p7-north-webhook", "WEBHOOK_ORDER", NORTH, WEBHOOK_TOKEN);
        createConnector(northIntegrationAdmin, "p7-north-csv", "CSV_ORDER_IMPORT", NORTH, "p7-csv-token");
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A)).isZero();
        assertThat(tenantFulfillmentCount()).isZero();

        String primaryOrderId = "L2-P7-ORDER-001";
        mockMvc.perform(post("/api/integrations/orders/webhook")
                .header("X-Synapse-Connector-Token", WEBHOOK_TOKEN)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"sourceSystem":"p7-north-webhook","externalOrderId":"%s","warehouseCode":"%s",
                     "customerReference":"P7-CUSTOMER-001","occurredAt":"2026-08-30T08:00:00Z",
                     "items":[{"productSku":"P7-SKU-A","quantity":8,"unitPrice":10.00},
                              {"productSku":"P7-SKU-B","quantity":6,"unitPrice":12.00}]}
                    """.formatted(primaryOrderId, NORTH)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.order.externalOrderId").value(primaryOrderId))
            .andExpect(jsonPath("$.order.warehouseCode").value(NORTH))
            .andExpect(jsonPath("$.order.items.length()").value(2));

        assertLedger(primaryOrderId, "P7-SKU-A", 8, 8, 0, 100, 8, 92, OrderStatus.RECEIVED,
            FulfillmentStatus.QUEUED);
        assertLedger(primaryOrderId, "P7-SKU-B", 6, 6, 0, 80, 6, 74, OrderStatus.RECEIVED,
            FulfillmentStatus.QUEUED);
        assertThat(fulfillmentTaskRepository.findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(TENANT_A, primaryOrderId))
            .get().extracting(task -> task.getTotalUnits(), task -> task.getFulfilledUnits(), task -> task.getWarehouse().getCode())
            .containsExactly(14, 0, NORTH);

        String pickingId = "l2-p7-primary-picking";
        fulfillment(northIntegrationOperator, primaryOrderId, "PICKING", null, null, pickingId);
        fulfillment(northIntegrationOperator, primaryOrderId, "PACKED", null, null, "l2-p7-primary-packed");
        fulfillment(northIntegrationOperator, primaryOrderId, "DISPATCHED", 3, "P7-SKU-A", "l2-p7-primary-a-3");
        assertLedger(primaryOrderId, "P7-SKU-A", 8, 5, 3, 97, 5, 92, OrderStatus.PARTIALLY_FULFILLED,
            FulfillmentStatus.DISPATCHED);
        fulfillment(northIntegrationOperator, primaryOrderId, "DISPATCHED", 5, "P7-SKU-A", "l2-p7-primary-a-5");
        assertLedger(primaryOrderId, "P7-SKU-A", 8, 0, 8, 92, 0, 92, OrderStatus.PARTIALLY_FULFILLED,
            FulfillmentStatus.DISPATCHED);
        fulfillment(northIntegrationOperator, primaryOrderId, "DISPATCHED", 6, "P7-SKU-B", "l2-p7-primary-b-6");
        assertLedger(primaryOrderId, "P7-SKU-B", 6, 0, 6, 74, 0, 74, OrderStatus.FULFILLED,
            FulfillmentStatus.DISPATCHED);
        fulfillment(northIntegrationOperator, primaryOrderId, "DELIVERED", null, null, "l2-p7-primary-delivery");
        assertThat(customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(TENANT_A, primaryOrderId))
            .get().extracting(CustomerOrder::getStatus).isEqualTo(OrderStatus.DELIVERED);
        assertThat(fulfillmentTaskRepository.findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(TENANT_A, primaryOrderId))
            .get().extracting(task -> task.getStatus()).isEqualTo(FulfillmentStatus.DELIVERED);
        fulfillment(northIntegrationOperator, primaryOrderId, "DELIVERED", null, null, "l2-p7-primary-delivery");
        assertLedger(primaryOrderId, "P7-SKU-A", 8, 0, 8, 92, 0, 92, OrderStatus.DELIVERED,
            FulfillmentStatus.DELIVERED);

        updateInventory(tenantAdmin, "P7-SKU-C", NORTH, 10, 20);
        JsonNode alertFeed = json(get("/api/alerts").session(tenantAdmin));
        JsonNode recommendationFeed = json(get("/api/recommendations").session(tenantAdmin));
        assertThat(alertFeed.path("activeAlerts").size()).isGreaterThanOrEqualTo(1);
        assertThat(recommendationFeed.isArray()).isTrue();
        assertThat(alertRepository.findAllByTenant_CodeIgnoreCaseAndStatusOrderByCreatedAtDesc(TENANT_A,
                AlertStatus.ACTIVE).stream()
            .allMatch(alert -> NORTH.equalsIgnoreCase(alert.getWarehouse().getCode()))).isTrue();

        long ordersBeforeRecovery = customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A);
        String recoveryOrderId = "L2-P7-RECOVERY-001";
        MockMultipartFile failedCsv = new MockMultipartFile("file", "l2-p7-recovery.csv", "text/csv",
            ("sourceSystem,externalOrderId,warehouseCode,productSku,quantity,unitPrice\n"
                + "p7-north-csv,%s,%s,P7-SKU-RECOVERY,2,14.00\n").formatted(recoveryOrderId, NORTH)
                .getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/integrations/orders/csv-import")
                .file(failedCsv).session(northIntegrationOperator).param("sourceSystem", "p7-north-csv"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordersImported").value(0))
            .andExpect(jsonPath("$.ordersFailed").value(1))
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INVENTORY_NOT_FOUND"));
        var failedInbound = inboundRecordRepository.findAll().stream()
            .filter(record -> TENANT_A.equalsIgnoreCase(record.getTenantCode()))
            .filter(record -> recoveryOrderId.equals(record.getExternalOrderId())).findFirst().orElseThrow();
        var failedReplay = replayRecordRepository.findAll().stream()
            .filter(record -> TENANT_A.equalsIgnoreCase(record.getTenantCode()))
            .filter(record -> recoveryOrderId.equals(record.getExternalOrderId())).findFirst().orElseThrow();
        assertThat(failedInbound.getStatus()).isEqualTo(IntegrationInboundStatus.REPLAY_QUEUED);
        assertThat(failedReplay.getStatus()).isEqualTo(IntegrationReplayStatus.PENDING);
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A)).isEqualTo(ordersBeforeRecovery);
        assertThat(fulfillmentTaskRepository
            .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(TENANT_A, recoveryOrderId)).isEmpty();

        updateInventory(tenantAdmin, "P7-SKU-RECOVERY", NORTH, 10, 0);
        mockMvc.perform(post("/api/integrations/orders/replay/" + failedReplay.getId())
                .session(northIntegrationOperator))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.replay.status").value("REPLAYED"))
            .andExpect(jsonPath("$.order.externalOrderId").value(recoveryOrderId));
        assertThat(customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(TENANT_A, recoveryOrderId))
            .isPresent();
        assertThat(inventoryFor("P7-SKU-RECOVERY", NORTH)).extracting(Inventory::getQuantityOnHand,
            Inventory::getQuantityReserved, Inventory::getQuantityAvailable).containsExactly(10L, 2L, 8L);
        assertThat(fulfillmentTaskRepository.findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(TENANT_A, recoveryOrderId))
            .get().extracting(task -> task.getStatus()).isEqualTo(FulfillmentStatus.QUEUED);
        mockMvc.perform(post("/api/integrations/orders/replay/" + failedReplay.getId())
                .session(northIntegrationOperator)).andExpect(status().isBadRequest());

        long ordersBeforeScenario = customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A);
        long fulfillmentBeforeScenario = tenantFulfillmentCount();
        Inventory scenarioInventoryBefore = inventoryFor("P7-SKU-A", NORTH);
        long alertsBeforeScenario = tenantAlertCount();
        long recommendationsBeforeScenario = tenantRecommendationCount();
        long replayBeforeScenario = replayRecordRepository.findAll().stream()
            .filter(record -> TENANT_A.equalsIgnoreCase(record.getTenantCode())).count();

        mockMvc.perform(post("/api/scenarios/order-impact").session(northIntegrationOperator)
                .contentType(APPLICATION_JSON)
                .content(scenarioOrder("P7-SKU-A", NORTH, 2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warehouseCode").value(NORTH))
            .andExpect(jsonPath("$.projectedInventory").isArray());
        ScenarioRun preview = latestScenario(ScenarioRunType.PREVIEW);
        assertThat(preview.getWarehouseCode()).isEqualTo(NORTH);
        assertOperationalCounts(ordersBeforeScenario, fulfillmentBeforeScenario, alertsBeforeScenario,
            recommendationsBeforeScenario, replayBeforeScenario, scenarioInventoryBefore);

        MvcResult savedResponse = mockMvc.perform(post("/api/scenarios/save").session(northIntegrationOperator)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"title":"P7 North governed plan","requestedBy":"P7 North Integration Operator",
                     "reviewOwner":"P7 North Review Owner",
                     "request":{"warehouseCode":"%s","items":[{"productSku":"P7-SKU-A","quantity":2,"unitPrice":10.00}]}}
                    """.formatted(NORTH)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requestedBy").value("P7 North Integration Operator"))
            .andExpect(jsonPath("$.reviewOwner").value("P7 North Review Owner"))
            .andExpect(jsonPath("$.executable").value(false))
            .andReturn();
        long savedScenarioId = objectMapper.readTree(savedResponse.getResponse().getContentAsString())
            .path("scenarioRunId").asLong();
        ScenarioRun savedPlan = scenarioRunRepository.findById(savedScenarioId).orElseThrow();
        assertThat(savedPlan.getRequestedBy()).isEqualTo("P7 North Integration Operator");
        assertThat(savedPlan.getReviewOwner()).isEqualTo("P7 North Review Owner");
        assertThat(savedPlan.getApprovalStage().name()).isEqualTo("PENDING_REVIEW");
        assertThat(savedPlan.getApprovalPolicy().name()).isEqualTo("STANDARD");

        mockMvc.perform(post("/api/scenarios/" + savedScenarioId + "/execute").session(northReviewOwner))
            .andExpect(status().isGone());
        mockMvc.perform(post("/api/scenarios/" + savedScenarioId + "/approve").session(northReviewOwner)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"P7 North Review Owner\",\"approvalNote\":\"P7 review complete\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
        assertOperationalCounts(ordersBeforeScenario, fulfillmentBeforeScenario, alertsBeforeScenario,
            recommendationsBeforeScenario, replayBeforeScenario, scenarioInventoryBefore);
        assertThat(scenarioRunRepository.findById(savedScenarioId).orElseThrow().getApprovalStage().name())
            .isEqualTo("APPROVED");
        assertThat(businessEventRepository.findAll().stream()
            .filter(event -> TENANT_A.equalsIgnoreCase(event.getTenantCode()))
            .noneMatch(event -> event.getEventType() == BusinessEventType.SCENARIO_EXECUTED)).isTrue();

        mockMvc.perform(post("/api/inventory/update").session(southIntegrationOperator)
                .contentType(APPLICATION_JSON)
                .content(inventoryPayload("P7-SKU-A", NORTH, 90, 10)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/fulfillment/updates").session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(fulfillmentBody(primaryOrderId, "PICKING", null, null)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders").session(northReviewOwner)
                .contentType(APPLICATION_JSON)
                .content(orderPayload("P7-REVIEW-DENIED", NORTH, "P7-SKU-A", 1)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders/" + primaryOrderId + "/transition").session(tenantBOperator)
                .contentType(APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\",\"restockInventory\":false,\"note\":\"cross tenant denial\"}"))
            .andExpect(status().isNotFound());
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_B)).isZero();
        assertThat(inventoryForTenantB("P7-SKU-A", BETA)).extracting(Inventory::getQuantityOnHand,
            Inventory::getQuantityReserved, Inventory::getQuantityAvailable).containsExactly(70L, 0L, 70L);

        MockHttpSession platformSession = platformLogin();
        mockMvc.perform(get("/api/platform/overview").session(platformSession)).andExpect(status().isOk());
        mockMvc.perform(post("/api/inventory/update").session(platformSession)
                .contentType(APPLICATION_JSON)
                .content(inventoryPayload("P7-SKU-A", NORTH, 90, 10)))
            .andExpect(status().isForbidden());

        awaitDispatchCompletion();
        assertThat(dispatchWorkItemRepository.findAll().stream()
            .filter(item -> TENANT_A.equalsIgnoreCase(item.getTenantCode()))
            .map(item -> item.getStatus()))
            .doesNotContain(OperationalDispatchStatus.PENDING, OperationalDispatchStatus.PROCESSING);
        JsonNode dashboard = json(get("/api/dashboard/snapshot").session(tenantAdmin));
        assertThat(dashboard.path("summary").path("totalOrders").asLong()).isEqualTo(2);
        assertThat(dashboard.path("summary").path("totalProducts").asLong()).isEqualTo(4);
        assertThat(dashboard.path("summary").path("totalWarehouses").asLong()).isEqualTo(2);
        assertThat(businessEventRepository.findAll().stream()
            .filter(event -> TENANT_A.equalsIgnoreCase(event.getTenantCode()))
            .map(event -> event.getEventType()))
            .contains(BusinessEventType.ORDER_INGESTED, BusinessEventType.FULFILLMENT_UPDATED,
                BusinessEventType.INTEGRATION_REPLAY_COMPLETED, BusinessEventType.SCENARIO_SAVED,
                BusinessEventType.SCENARIO_APPROVED);
        assertThat(auditLogRepository.findAll().stream()
            .filter(log -> TENANT_A.equalsIgnoreCase(log.getTenantCode()))
            .filter(log -> log.getStatus() == AuditStatus.SUCCESS)
            .map(log -> log.getTargetRef()))
            .anyMatch(target -> target.contains(primaryOrderId));
        assertThat(customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(TENANT_A, recoveryOrderId))
            .get().extracting(CustomerOrder::getStatus).isEqualTo(OrderStatus.RECEIVED);
        assertThat(northFinalApprover).isNotNull();
        assertThat(northEscalationOwner).isNotNull();
        assertThat(pickingId).isNotBlank();
    }

    private void assertOperationalCounts(long orders, long fulfillments, long alerts, long recommendations,
                                         long replays, Inventory expectedInventory) {
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(TENANT_A)).isEqualTo(orders);
        assertThat(tenantFulfillmentCount()).isEqualTo(fulfillments);
        assertThat(tenantAlertCount()).isEqualTo(alerts);
        assertThat(tenantRecommendationCount()).isEqualTo(recommendations);
        assertThat(replayRecordRepository.findAll().stream()
            .filter(record -> TENANT_A.equalsIgnoreCase(record.getTenantCode())).count()).isEqualTo(replays);
        assertThat(inventoryFor("P7-SKU-A", NORTH)).extracting(Inventory::getQuantityOnHand,
            Inventory::getQuantityReserved, Inventory::getQuantityAvailable)
            .containsExactly(expectedInventory.getQuantityOnHand(), expectedInventory.getQuantityReserved(),
                expectedInventory.getQuantityAvailable());
    }

    private long tenantAlertCount() {
        return alertRepository.findAllByTenant_CodeIgnoreCaseOrderByUpdatedAtDesc(TENANT_A).size();
    }

    private long tenantFulfillmentCount() {
        return fulfillmentTaskRepository.countByTenant_CodeIgnoreCaseAndStatusIn(TENANT_A,
            List.of(FulfillmentStatus.values()));
    }

    private long tenantRecommendationCount() {
        return java.util.Arrays.stream(RecommendationStatus.values())
            .mapToLong(status -> recommendationRepository
                .findAllByTenant_CodeIgnoreCaseAndStatusOrderByUpdatedAtDesc(TENANT_A, status).size())
            .sum();
    }

    private void assertLedger(String orderId, String sku, int ordered, int reserved, int fulfilled, long onHand,
                              long reservedStock, long available, OrderStatus orderStatus,
                              FulfillmentStatus fulfillmentStatus) {
        CustomerOrder order = customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(TENANT_A, orderId)
            .orElseThrow();
        var item = order.getItems().stream()
            .filter(candidate -> sku.equalsIgnoreCase(candidate.getProduct().resolveCatalogSku())).findFirst().orElseThrow();
        Inventory inventory = inventoryFor(sku, NORTH);
        var task = fulfillmentTaskRepository.findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(TENANT_A, orderId)
            .orElseThrow();
        assertThat(item.getQuantity()).isEqualTo(ordered);
        assertThat(item.getReservedQuantity()).isEqualTo(reserved);
        assertThat(item.getFulfilledQuantity()).isEqualTo(fulfilled);
        assertThat(inventory).extracting(Inventory::getQuantityOnHand, Inventory::getQuantityReserved,
            Inventory::getQuantityAvailable).containsExactly(onHand, reservedStock, available);
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(task.getStatus()).isEqualTo(fulfillmentStatus);
        assertThat(ordered).isEqualTo(fulfilled + reserved + item.getCancelledQuantity());
        assertThat(available).isEqualTo(onHand - reservedStock);
    }

    private ScenarioRun latestScenario(ScenarioRunType type) {
        return scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == type).findFirst().orElseThrow();
    }

    private void awaitDispatchCompletion() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (!dispatchQueueService.isDraining()) {
                dispatchQueueService.processPendingWork();
            }
            boolean inFlight = dispatchWorkItemRepository.findAll().stream()
                .filter(item -> TENANT_A.equalsIgnoreCase(item.getTenantCode()))
                .anyMatch(item -> item.getStatus() == OperationalDispatchStatus.PENDING
                    || item.getStatus() == OperationalDispatchStatus.PROCESSING);
            if (!inFlight && !dispatchQueueService.isDraining()) {
                return;
            }
            Thread.sleep(50);
        }
    }

    private JsonNode json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
        throws Exception {
        return objectMapper.readTree(mockMvc.perform(request).andExpect(status().isOk()).andReturn()
            .getResponse().getContentAsString());
    }

    private void fulfillment(MockHttpSession session, String orderId, String status, Integer units, String sku,
                             String requestId) throws Exception {
        mockMvc.perform(post("/api/fulfillment/updates").session(session).header("X-Request-Id", requestId)
                .contentType(APPLICATION_JSON).content(fulfillmentBody(orderId, status, units, sku)))
            .andExpect(status().isOk());
    }

    private String fulfillmentBody(String orderId, String status, Integer units, String sku) {
        String quantity = units == null ? "null" : units.toString();
        String product = sku == null ? "null" : "\"" + sku + "\"";
        return "{\"externalOrderId\":\"%s\",\"status\":\"%s\",\"fulfilledUnits\":%s,\"fulfilledProductSku\":%s,\"note\":\"Layer 2 Phase 7 acceptance\"}"
            .formatted(orderId, status, quantity, product);
    }

    private String scenarioOrder(String sku, String warehouse, int quantity) {
        return "{\"warehouseCode\":\"%s\",\"items\":[{\"productSku\":\"%s\",\"quantity\":%d,\"unitPrice\":10.00}]}"
            .formatted(warehouse, sku, quantity);
    }

    private String orderPayload(String id, String warehouse, String sku, int quantity) {
        return "{\"externalOrderId\":\"%s\",\"warehouseCode\":\"%s\",\"items\":[{\"productSku\":\"%s\",\"quantity\":%d,\"unitPrice\":10.00}]}"
            .formatted(id, warehouse, sku, quantity);
    }

    private String inventoryPayload(String sku, String warehouse, long quantity, long threshold) {
        return "{\"productSku\":\"%s\",\"warehouseCode\":\"%s\",\"quantityAvailable\":%d,\"reorderThreshold\":%d}"
            .formatted(sku, warehouse, quantity, threshold);
    }

    private void createProduct(MockHttpSession session, String sku) throws Exception {
        mockMvc.perform(post("/api/products").session(session).contentType(APPLICATION_JSON)
                .content("{\"sku\":\"%s\",\"name\":\"Layer 2 Phase 7 %s\",\"category\":\"Acceptance\"}"
                    .formatted(sku, sku)))
            .andExpect(status().isCreated());
    }

    private void updateInventory(MockHttpSession session, String sku, String warehouse, long quantity, long threshold)
        throws Exception {
        mockMvc.perform(post("/api/inventory/update").session(session).contentType(APPLICATION_JSON)
                .content(inventoryPayload(sku, warehouse, quantity, threshold)))
            .andExpect(status().isOk());
    }

    private void createConnector(MockHttpSession session, String source, String type, String warehouse, String token)
        throws Exception {
        mockMvc.perform(post("/api/integrations/orders/connectors").session(session).contentType(APPLICATION_JSON)
                .content("""
                    {"sourceSystem":"%s","type":"%s","displayName":"Layer 2 Phase 7 %s","enabled":true,
                     "syncMode":"%s","validationPolicy":"RELAXED","transformationPolicy":"NORMALIZE_CODES",
                     "allowDefaultWarehouseFallback":false,"defaultWarehouseCode":"%s","inboundAccessToken":"%s"}
                    """.formatted(source, type, source, "WEBHOOK_ORDER".equals(type) ? "REALTIME_PUSH" : "BATCH_FILE_DROP",
                    warehouse, token)))
            .andExpect(status().isOk());
    }

    private Inventory inventoryFor(String sku, String warehouse) {
        var product = productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(TENANT_A, sku).orElseThrow();
        var site = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(TENANT_A, warehouse).orElseThrow();
        return inventoryRepository.findByProductIdAndWarehouseId(product.getId(), site.getId()).orElseThrow();
    }

    private Inventory inventoryForTenantB(String sku, String warehouse) {
        var product = productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(TENANT_B, sku).orElseThrow();
        var site = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(TENANT_B, warehouse).orElseThrow();
        return inventoryRepository.findByProductIdAndWarehouseId(product.getId(), site.getId()).orElseThrow();
    }

    private MockHttpSession platformLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/platform/session/login").contentType(APPLICATION_JSON)
                .content("{\"username\":\"l2-p7-platform-owner\",\"password\":\"%s\"}".formatted(PLATFORM_PASSWORD)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.signedIn").value(true)).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MockHttpSession login(String tenant, String username, String password, String role, List<String> scopes)
        throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/session/login").contentType(APPLICATION_JSON)
                .content(loginPayload(tenant, username, password)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem(role))).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        if (body.path("passwordChangeRequired").asBoolean()) {
            MvcResult changed = mockMvc.perform(post("/api/auth/session/password").session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}"
                        .formatted(password, ROLE_PASSWORD)))
                .andExpect(status().isOk()).andReturn();
            session = (MockHttpSession) changed.getRequest().getSession(false);
        }
        JsonNode sessionBody = json(get("/api/auth/session").session(session));
        assertThat(sessionBody.path("tenantCode").asText()).isEqualTo(tenant);
        assertThat(arrayTexts(sessionBody.path("warehouseScopes"))).containsExactlyElementsOf(scopes);
        return session;
    }

    private List<String> arrayTexts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private void provisionTenant(String payload) throws Exception {
        mockMvc.perform(post("/api/access/tenants").header("X-Synapse-Platform-Admin-Token", "l2-p7-platform-token")
                .contentType(APPLICATION_JSON).content(payload)).andExpect(status().isOk());
    }

    private String loginPayload(String tenant, String username, String password) {
        return "{\"tenantCode\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}"
            .formatted(tenant, username, password);
    }

    private String tenantAPayload() {
        return """
            {"tenantCode":"L2-P7-ACCEPT-A","tenantName":"Layer 2 Phase 7 Acceptance A",
             "description":"Disposable full operational acceptance tenant.","adminFullName":"P7 Tenant Administrator",
             "adminUsername":"p7.tenant.admin","adminPassword":"%s","primaryLocation":"P7 North",
             "warehouses":[{"code":"L2-P7-WH-NORTH","name":"P7 North","location":"North"},
                           {"code":"L2-P7-WH-SOUTH","name":"P7 South","location":"South"}],
             "users":[
               {"username":"p7.tenant.admin","fullName":"P7 Tenant Administrator","operatorActorName":"P7 Tenant Administrator","roles":["TENANT_ADMIN"],"warehouseScopes":[]},
               {"username":"p7.north.integration.admin","fullName":"P7 North Integration Admin","operatorActorName":"P7 North Integration Admin","roles":["INTEGRATION_ADMIN"],"warehouseScopes":["L2-P7-WH-NORTH"],"initialPassword":"%s"},
               {"username":"p7.north.integration.operator","fullName":"P7 North Integration Operator","operatorActorName":"P7 North Integration Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["L2-P7-WH-NORTH"],"initialPassword":"%s"},
               {"username":"p7.north.review","fullName":"P7 North Review Owner","operatorActorName":"P7 North Review Owner","roles":["REVIEW_OWNER"],"warehouseScopes":["L2-P7-WH-NORTH"],"initialPassword":"%s"},
               {"username":"p7.north.final","fullName":"P7 North Final Approver","operatorActorName":"P7 North Final Approver","roles":["FINAL_APPROVER"],"warehouseScopes":["L2-P7-WH-NORTH"],"initialPassword":"%s"},
               {"username":"p7.north.escalation","fullName":"P7 North Escalation Owner","operatorActorName":"P7 North Escalation Owner","roles":["ESCALATION_OWNER"],"warehouseScopes":["L2-P7-WH-NORTH"],"initialPassword":"%s"},
               {"username":"p7.south.integration.operator","fullName":"P7 South Integration Operator","operatorActorName":"P7 South Integration Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["L2-P7-WH-SOUTH"],"initialPassword":"%s"}
             ],"requiredRoles":[]}
            """.formatted(ADMIN_PASSWORD, INITIAL_PASSWORD, INITIAL_PASSWORD, INITIAL_PASSWORD, INITIAL_PASSWORD,
            INITIAL_PASSWORD, INITIAL_PASSWORD);
    }

    private String tenantBPayload() {
        return """
            {"tenantCode":"L2-P7-ACCEPT-B","tenantName":"Layer 2 Phase 7 Acceptance B",
             "description":"Disposable tenant isolation control.","adminFullName":"P7 Tenant B Administrator",
             "adminUsername":"p7.beta.admin","adminPassword":"%s","primaryLocation":"P7 Beta",
             "warehouses":[{"code":"L2-P7-WH-BETA","name":"P7 Beta","location":"Beta"}],
             "users":[
               {"username":"p7.beta.admin","fullName":"P7 Tenant B Administrator","operatorActorName":"P7 Tenant B Administrator","roles":["TENANT_ADMIN"],"warehouseScopes":[]},
               {"username":"p7.beta.integration.operator","fullName":"P7 Beta Integration Operator","operatorActorName":"P7 Beta Integration Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["L2-P7-WH-BETA"],"initialPassword":"%s"}
             ],"requiredRoles":[]}
            """.formatted(ADMIN_PASSWORD, INITIAL_PASSWORD);
    }
}
