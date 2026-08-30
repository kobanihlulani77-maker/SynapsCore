package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.BusinessEvent;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderItem;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.event.OperationalDispatchQueueService;
import com.synapsecore.event.OperationalUpdateType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
    "synapsecore.starter.require-explicit-tenant-provisioning=true",
    "synapsecore.starter.seed-starter-inventory-on-tenant-onboarding=false",
    "synapsecore.starter.seed-starter-connectors-on-tenant-onboarding=false",
    "synapsecore.dashboard.cache-enabled=false",
    "synapsecore.integration.pull-worker.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Layer2Phase3FulfillmentConvergenceIntegrationTest {

    private static final String PLATFORM_ADMIN_TOKEN = "test-only-platform-admin-token";
    private static final String ADMIN_PASSWORD = "Layer2Phase3Admin!2026";
    private static final String INITIAL_PASSWORD = "Layer2Phase3Initial!2026";
    private static final String ROLE_PASSWORD = "Layer2Phase3Role!2026";
    private static final long DISPATCH_TIMEOUT_MILLIS = 5000;
    private static final long DISPATCH_POLL_MILLIS = 50;

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
    private BusinessEventRepository businessEventRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OperationalDispatchWorkItemRepository dispatchWorkItemRepository;

    @Autowired
    private OperationalDispatchQueueService dispatchQueueService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String suffix;
    private String tenantA;
    private String tenantB;
    private String warehouseA;
    private String warehouseB;
    private String tenantBWarehouse;
    private String skuA;
    private String skuB;
    private String skuC;
    private MockHttpSession tenantAdminA;
    private MockHttpSession integrationAdminA;
    private MockHttpSession integrationOperatorA;
    private MockHttpSession warehouseOperatorB;
    private MockHttpSession tenantAdminB;
    private MockHttpSession tenantBOperator;

    @BeforeEach
    void createDisposableLayer2Model() throws Exception {
        suffix = Long.toString(System.nanoTime());
        tenantA = "L2P3-A-" + suffix;
        tenantB = "L2P3-B-" + suffix;
        warehouseA = "L2P3-WHA-" + suffix;
        warehouseB = "L2P3-WHB-" + suffix;
        tenantBWarehouse = "L2P3-WHBT-" + suffix;
        skuA = "L2P3-SKU-A-" + suffix;
        skuB = "L2P3-SKU-B-" + suffix;
        skuC = "L2P3-SKU-C-" + suffix;

        provisionTenant(tenantPayloadA());
        provisionTenant(tenantPayloadB());

        tenantAdminA = login(tenantA, "p3.admin." + suffix, ADMIN_PASSWORD, "TENANT_ADMIN", List.of());
        integrationAdminA = login(tenantA, "p3.integration.admin." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_ADMIN", List.of(warehouseA));
        integrationOperatorA = login(tenantA, "p3.integration.operator." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(warehouseA));
        warehouseOperatorB = login(tenantA, "p3.warehouse.operator." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(warehouseB));
        tenantAdminB = login(tenantB, "p3.tenantb.admin." + suffix, ADMIN_PASSWORD,
            "TENANT_ADMIN", List.of());
        tenantBOperator = login(tenantB, "p3.tenantb.operator." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(tenantBWarehouse));

        for (String sku : List.of(skuA, skuB, skuC)) {
            createProduct(tenantAdminA, sku);
            updateInventory(tenantAdminA, sku, warehouseA, 100);
            updateInventory(tenantAdminA, sku, warehouseB, 100);
        }
        createProduct(tenantAdminB, skuA);
        updateInventory(tenantAdminB, skuA, tenantBWarehouse, 70);
    }

    @Test
    void primaryMultiLineOrderConvergesThroughFulfillmentAndDelivery() throws Exception {
        String orderId = "L2-P3-PRIMARY-001-" + suffix;
        createOrder(integrationAdminA, orderId, warehouseA, line(skuA, 8), line(skuB, 6));

        Ledger afterCreationA = ledger(orderId, skuA, warehouseA);
        Ledger afterCreationB = ledger(orderId, skuB, warehouseA);
        assertThat(afterCreationA).extracting(Ledger::ordered, Ledger::reserved, Ledger::fulfilled,
            Ledger::cancelled, Ledger::returned, Ledger::onHand, Ledger::reservedStock, Ledger::available)
            .containsExactly(8, 8, 0, 0, 0, 100L, 8L, 92L);
        assertThat(afterCreationB).extracting(Ledger::ordered, Ledger::reserved, Ledger::fulfilled,
            Ledger::cancelled, Ledger::returned, Ledger::onHand, Ledger::reservedStock, Ledger::available)
            .containsExactly(6, 6, 0, 0, 0, 100L, 6L, 94L);
        assertThat(afterCreationA.orderStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(afterCreationA.fulfillmentStatus()).isEqualTo(FulfillmentStatus.QUEUED);
        assertThat(afterCreationA.fulfilledUnits()).isZero();

        List<String> requestIds = new ArrayList<>();
        String pickingRequest = requestId("picking");
        requestIds.add(pickingRequest);
        fulfillment(integrationOperatorA, orderId, "PICKING", null, null, pickingRequest)
            .andExpect(status().isOk());
        assertStage(orderId, skuA, warehouseA, OrderStatus.PROCESSING, FulfillmentStatus.PICKING,
            8, 8, 0, 100, 8, 92);

        String packedRequest = requestId("packed");
        requestIds.add(packedRequest);
        fulfillment(integrationOperatorA, orderId, "PACKED", null, null, packedRequest)
            .andExpect(status().isOk());
        assertStage(orderId, skuA, warehouseA, OrderStatus.PROCESSING, FulfillmentStatus.PACKED,
            8, 8, 0, 100, 8, 92);
        mockMvc.perform(get("/api/fulfillment").session(integrationAdminA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.backlogCount").value(1));
        mockMvc.perform(get("/api/dashboard/snapshot").session(integrationAdminA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fulfillment.backlogCount").value(1));

        String firstDispatch = requestId("dispatch-a-3");
        requestIds.add(firstDispatch);
        fulfillment(integrationOperatorA, orderId, "DISPATCHED", 3, skuA, firstDispatch)
            .andExpect(status().isOk());
        assertStage(orderId, skuA, warehouseA, OrderStatus.PARTIALLY_FULFILLED, FulfillmentStatus.DISPATCHED,
            8, 5, 3, 97, 5, 92);
        assertStage(orderId, skuB, warehouseA, OrderStatus.PARTIALLY_FULFILLED, FulfillmentStatus.DISPATCHED,
            6, 6, 0, 100, 6, 94);

        String secondDispatch = requestId("dispatch-a-5");
        requestIds.add(secondDispatch);
        fulfillment(integrationOperatorA, orderId, "DISPATCHED", 5, skuA, secondDispatch)
            .andExpect(status().isOk());
        assertStage(orderId, skuA, warehouseA, OrderStatus.PARTIALLY_FULFILLED, FulfillmentStatus.DISPATCHED,
            8, 0, 8, 92, 0, 92);
        assertStage(orderId, skuB, warehouseA, OrderStatus.PARTIALLY_FULFILLED, FulfillmentStatus.DISPATCHED,
            6, 6, 0, 100, 6, 94);

        String finalDispatch = requestId("dispatch-b-6");
        requestIds.add(finalDispatch);
        fulfillment(integrationOperatorA, orderId, "DISPATCHED", 6, skuB, finalDispatch)
            .andExpect(status().isOk());
        assertStage(orderId, skuA, warehouseA, OrderStatus.FULFILLED, FulfillmentStatus.DISPATCHED,
            8, 0, 8, 92, 0, 92);
        assertStage(orderId, skuB, warehouseA, OrderStatus.FULFILLED, FulfillmentStatus.DISPATCHED,
            6, 0, 6, 94, 0, 94);

        Ledger beforeDeliveryA = ledger(orderId, skuA, warehouseA);
        Ledger beforeDeliveryB = ledger(orderId, skuB, warehouseA);
        String deliveryRequest = requestId("delivery");
        requestIds.add(deliveryRequest);
        fulfillment(integrationOperatorA, orderId, "DELIVERED", null, null, deliveryRequest)
            .andExpect(status().isOk());
        assertThat(ledger(orderId, skuA, warehouseA)).isEqualTo(beforeDeliveryA.withTerminal(
            OrderStatus.DELIVERED, FulfillmentStatus.DELIVERED));
        assertThat(ledger(orderId, skuB, warehouseA)).isEqualTo(beforeDeliveryB.withTerminal(
            OrderStatus.DELIVERED, FulfillmentStatus.DELIVERED));
        mockMvc.perform(get("/api/fulfillment").session(integrationAdminA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.backlogCount").value(0));
        mockMvc.perform(get("/api/dashboard/snapshot").session(integrationAdminA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fulfillment.backlogCount").value(0));

        awaitDispatchCompletion(Set.copyOf(requestIds));
        assertThat(dispatchWorkItemRepository.findAll().stream()
            .filter(item -> tenantA.equalsIgnoreCase(item.getTenantCode()))
            .filter(item -> requestIds.contains(item.getRequestId()))
            .map(OperationalDispatchWorkItem::getUpdateType))
            .containsOnly(OperationalUpdateType.FULFILLMENT_UPDATE, OperationalUpdateType.INVENTORY_UPDATE);

        List<BusinessEvent> orderEvents = businessEventRepository.findAll().stream()
            .filter(event -> tenantA.equalsIgnoreCase(event.getTenantCode()))
            .filter(event -> event.getPayloadSummary().contains(orderId))
            .toList();
        assertThat(orderEvents).extracting(BusinessEvent::getEventType)
            .contains(BusinessEventType.FULFILLMENT_UPDATED, BusinessEventType.ORDER_STATUS_TRANSITIONED);
        List<AuditLog> orderAudits = auditLogRepository.findAll().stream()
            .filter(log -> tenantA.equalsIgnoreCase(log.getTenantCode()))
            .filter(log -> orderId.equals(log.getTargetRef()))
            .filter(log -> log.getStatus() == AuditStatus.SUCCESS)
            .toList();
        assertThat(orderAudits).extracting(AuditLog::getAction)
            .contains("FULFILLMENT_UPDATED", "ORDER_STATUS_UPDATED");
    }

    @Test
    void rejectedFulfillmentIsAtomicAndAuthorityRemainsScoped() throws Exception {
        String orderId = "L2-P3-BOUNDARY-001-" + suffix;
        createOrder(integrationAdminA, orderId, warehouseA, line(skuA, 10));
        Ledger baseline = ledger(orderId, skuA, warehouseA);

        fulfillment(integrationOperatorA, orderId, "DISPATCHED", 1, "NOT-A-LINE", requestId("wrong-sku"))
            .andExpect(status().isBadRequest());
        assertThat(ledger(orderId, skuA, warehouseA)).isEqualTo(baseline);

        fulfillment(integrationOperatorA, orderId, "DISPATCHED", 11, skuA, requestId("over"))
            .andExpect(status().isBadRequest());
        fulfillment(integrationOperatorA, orderId, "PICKING", 1, null, requestId("non-dispatch-quantity"))
            .andExpect(status().isBadRequest());
        assertThat(ledger(orderId, skuA, warehouseA)).isEqualTo(baseline);

        String pickingRequest = requestId("picking");
        fulfillment(integrationOperatorA, orderId, "PICKING", null, null, pickingRequest)
            .andExpect(status().isOk());
        Ledger picking = ledger(orderId, skuA, warehouseA);
        assertThat(picking.orderStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(picking.fulfillmentStatus()).isEqualTo(FulfillmentStatus.PICKING);
        assertThat(picking).extracting(Ledger::fulfilled, Ledger::reserved, Ledger::onHand, Ledger::reservedStock)
            .containsExactly(0, 10, 100L, 10L);

        fulfillment(integrationOperatorA, orderId, "PICKING", null, null, requestId("same-state"))
            .andExpect(status().isOk());
        fulfillment(integrationOperatorA, orderId, "PACKED", null, null, requestId("packed"))
            .andExpect(status().isOk());
        fulfillment(integrationOperatorA, orderId, "PICKING", null, null, requestId("backward"))
            .andExpect(status().isBadRequest());
        assertThat(ledger(orderId, skuA, warehouseA).reserved()).isEqualTo(10);

        String partialRequest = requestId("partial");
        fulfillment(integrationOperatorA, orderId, "DISPATCHED", 4, skuA, partialRequest)
            .andExpect(status().isOk());
        Ledger afterPartial = ledger(orderId, skuA, warehouseA);
        fulfillment(integrationOperatorA, orderId, "DISPATCHED", 4, skuA, partialRequest)
            .andExpect(status().isOk());
        assertThat(ledger(orderId, skuA, warehouseA)).isEqualTo(afterPartial);
        fulfillment(integrationOperatorA, orderId, "DISPATCHED", 5, skuA, partialRequest)
            .andExpect(status().isConflict());
        assertThat(ledger(orderId, skuA, warehouseA)).isEqualTo(afterPartial);

        String multiLineId = "L2-P3-WRONG-LINE-" + suffix;
        createOrder(integrationAdminA, multiLineId, warehouseA, line(skuC, 5), line(skuB, 10));
        Ledger multiLineA = ledger(multiLineId, skuC, warehouseA);
        Ledger multiLineB = ledger(multiLineId, skuB, warehouseA);
        fulfillment(integrationOperatorA, multiLineId, "DISPATCHED", 6, skuC, requestId("line-over"))
            .andExpect(status().isBadRequest());
        assertThat(ledger(multiLineId, skuC, warehouseA)).isEqualTo(multiLineA);
        assertThat(ledger(multiLineId, skuB, warehouseA)).isEqualTo(multiLineB);

        Ledger controlWarehouse = stockLedger(skuA, tenantA, warehouseB);
        Ledger controlTenant = stockLedger(skuA, tenantB, tenantBWarehouse);
        fulfillment(warehouseOperatorB, orderId, "DISPATCHED", 1, skuA, requestId("wrong-warehouse"))
            .andExpect(status().isForbidden());
        fulfillment(tenantAdminA, orderId, "DISPATCHED", 1, skuA, requestId("tenant-admin"))
            .andExpect(status().isForbidden());
        fulfillment(tenantBOperator, orderId, "DISPATCHED", 1, skuA, requestId("wrong-tenant"))
            .andExpect(status().isNotFound());
        assertThat(ledger(orderId, skuA, warehouseA)).isEqualTo(afterPartial);
        assertThat(stockLedger(skuA, tenantA, warehouseB)).isEqualTo(controlWarehouse);
        assertThat(stockLedger(skuA, tenantB, tenantBWarehouse)).isEqualTo(controlTenant);
        assertThat(successfulAuditCount(orderId, "wrong-warehouse")).isZero();
        assertThat(successfulAuditCount(orderId, "tenant-admin")).isZero();
    }

    @Test
    void cancellationExceptionDelayAndReturnReleaseOnlyTheCorrectStock() throws Exception {
        String cancelId = "L2-P3-CANCEL-BEFORE-" + suffix;
        createOrder(integrationAdminA, cancelId, warehouseA, line(skuA, 6));
        Ledger cancelBefore = ledger(cancelId, skuA, warehouseA);
        transition(integrationAdminA, cancelId, "CANCELLED", false).andExpect(status().isOk());
        Ledger cancelled = ledger(cancelId, skuA, warehouseA);
        assertThat(cancelled).extracting(Ledger::fulfilled, Ledger::reserved, Ledger::cancelled,
            Ledger::onHand, Ledger::reservedStock, Ledger::available)
            .containsExactly(0, 0, 6, cancelBefore.onHand(), 0L, cancelBefore.onHand());
        assertThat(cancelled.orderStatus()).isEqualTo(OrderStatus.CANCELLED);

        String partialCancelId = "L2-P3-CANCEL-AFTER-" + suffix;
        createOrder(integrationAdminA, partialCancelId, warehouseA, line(skuB, 10));
        fulfillment(integrationOperatorA, partialCancelId, "DISPATCHED", 4, skuB, requestId("cancel-partial"))
            .andExpect(status().isOk());
        transition(integrationAdminA, partialCancelId, "CANCELLED", false).andExpect(status().isOk());
        Ledger partialCancelled = ledger(partialCancelId, skuB, warehouseA);
        assertThat(partialCancelled).extracting(Ledger::fulfilled, Ledger::reserved, Ledger::cancelled,
            Ledger::onHand, Ledger::reservedStock, Ledger::available)
            .containsExactly(4, 0, 6, 96L, 0L, 96L);

        String exceptionBeforeId = "L2-P3-EXCEPTION-BEFORE-" + suffix;
        createOrder(integrationAdminA, exceptionBeforeId, warehouseA, line(skuC, 5));
        fulfillment(integrationOperatorA, exceptionBeforeId, "EXCEPTION", null, null, requestId("exception-before"))
            .andExpect(status().isOk());
        Ledger exceptionBefore = ledger(exceptionBeforeId, skuC, warehouseA);
        assertThat(exceptionBefore).extracting(Ledger::fulfilled, Ledger::reserved, Ledger::cancelled,
            Ledger::onHand, Ledger::reservedStock, Ledger::available)
            .containsExactly(0, 0, 5, 100L, 0L, 100L);
        assertThat(exceptionBefore.orderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(exceptionBefore.fulfillmentStatus()).isEqualTo(FulfillmentStatus.EXCEPTION);

        String exceptionPartialId = "L2-P3-EXCEPTION-AFTER-" + suffix;
        createOrder(integrationAdminA, exceptionPartialId, warehouseA, line(skuA, 10));
        fulfillment(integrationOperatorA, exceptionPartialId, "DISPATCHED", 4, skuA, requestId("exception-partial-dispatch"))
            .andExpect(status().isOk());
        fulfillment(integrationOperatorA, exceptionPartialId, "EXCEPTION", null, null, requestId("exception-partial"))
            .andExpect(status().isOk());
        Ledger exceptionPartial = ledger(exceptionPartialId, skuA, warehouseA);
        assertThat(exceptionPartial).extracting(Ledger::fulfilled, Ledger::reserved, Ledger::cancelled,
            Ledger::onHand, Ledger::reservedStock, Ledger::available)
            .containsExactly(4, 0, 6, 96L, 0L, 96L);
        assertThat(exceptionPartial.orderStatus()).isEqualTo(OrderStatus.FAILED);

        String delayedId = "L2-P3-DELAYED-" + suffix;
        createOrder(integrationAdminA, delayedId, warehouseA, line(skuB, 3));
        fulfillment(integrationOperatorA, delayedId, "DELAYED", null, null, requestId("delayed"))
            .andExpect(status().isOk());
        Ledger delayed = ledger(delayedId, skuB, warehouseA);
        assertThat(delayed.orderStatus()).isEqualTo(OrderStatus.BLOCKED);
        assertThat(delayed.fulfillmentStatus()).isEqualTo(FulfillmentStatus.DELAYED);
        assertThat(delayed).extracting(Ledger::fulfilled, Ledger::reserved, Ledger::onHand, Ledger::reservedStock)
            .containsExactly(0, 3, 96L, 3L);
        fulfillment(integrationOperatorA, delayedId, "PICKING", null, null, requestId("delayed-recovery"))
            .andExpect(status().isOk());
        assertThat(ledger(delayedId, skuB, warehouseA).fulfillmentStatus()).isEqualTo(FulfillmentStatus.PICKING);

        String noRestockId = "L2-P3-RETURN-NO-RESTOCK-" + suffix;
        createOrder(integrationAdminA, noRestockId, warehouseA, line(skuC, 3));
        fulfillment(integrationOperatorA, noRestockId, "DISPATCHED", null, null, requestId("return-no-restock"))
            .andExpect(status().isOk());
        transition(integrationAdminA, noRestockId, "RETURNED", false).andExpect(status().isOk());
        Ledger noRestock = ledger(noRestockId, skuC, warehouseA);
        assertThat(noRestock).extracting(Ledger::fulfilled, Ledger::reserved, Ledger::returned,
            Ledger::onHand, Ledger::reservedStock, Ledger::available)
            .containsExactly(3, 0, 3, 97L, 0L, 97L);

        String restockId = "L2-P3-RETURN-RESTOCK-" + suffix;
        createOrder(integrationAdminA, restockId, warehouseA, line(skuA, 4));
        fulfillment(integrationOperatorA, restockId, "DISPATCHED", null, null, requestId("return-restock"))
            .andExpect(status().isOk());
        transition(integrationAdminA, restockId, "RETURNED", true).andExpect(status().isOk());
        Ledger restocked = ledger(restockId, skuA, warehouseA);
        assertThat(restocked).extracting(Ledger::fulfilled, Ledger::reserved, Ledger::returned,
            Ledger::onHand, Ledger::reservedStock, Ledger::available)
            .containsExactly(4, 0, 4, 96L, 0L, 96L);
        transition(integrationAdminA, restockId, "RETURNED", true).andExpect(status().isBadRequest());
        assertThat(ledger(restockId, skuA, warehouseA)).isEqualTo(restocked);
    }

    @Test
    void concurrentDispatchCannotOverFulfillOrCrossWarehouseInventory() throws Exception {
        String orderId = "L2-P3-CONCURRENT-001-" + suffix;
        createOrder(integrationAdminA, orderId, warehouseA, line(skuA, 10));
        String firstRequest = requestId("concurrent-a");
        String secondRequest = requestId("concurrent-b");
        List<Integer> statuses = runConcurrently(
            () -> fulfillmentStatus(integrationOperatorA, orderId, "DISPATCHED", 6, skuA, firstRequest),
            () -> fulfillmentStatus(integrationOperatorA, orderId, "DISPATCHED", 6, skuA, secondRequest)
        );
        assertThat(statuses).contains(200);
        assertThat(statuses).allSatisfy(value -> assertThat(value).isIn(200, 400, 409));
        Ledger result = ledger(orderId, skuA, warehouseA);
        assertThat(result.fulfilled()).isLessThanOrEqualTo(10);
        assertThat(result.reserved()).isGreaterThanOrEqualTo(0);
        assertThat(result.fulfilled() + result.reserved() + result.cancelled()).isEqualTo(10);
        assertThat(result.onHand()).isEqualTo(100L - result.fulfilled());
        assertThat(result.available()).isEqualTo(result.onHand() - result.reservedStock());
        assertThat(stockLedger(skuA, tenantA, warehouseB).onHand()).isEqualTo(100L);
    }

    private void assertStage(String orderId, String sku, String warehouse, OrderStatus orderStatus,
                             FulfillmentStatus fulfillmentStatus, int ordered, int reserved, int fulfilled,
                             long onHand, long reservedStock, long available) {
        Ledger ledger = ledger(orderId, sku, warehouse);
        assertThat(ledger).extracting(Ledger::ordered, Ledger::reserved, Ledger::fulfilled,
            Ledger::onHand, Ledger::reservedStock, Ledger::available)
            .containsExactly(ordered, reserved, fulfilled, onHand, reservedStock, available);
        assertThat(ledger.orderStatus()).isEqualTo(orderStatus);
        assertThat(ledger.fulfillmentStatus()).isEqualTo(fulfillmentStatus);
    }

    private Ledger ledger(String orderId, String sku, String warehouseCode) {
        return transactionTemplate.execute(status -> {
            CustomerOrder order = customerOrderRepository
                .findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantA, orderId)
                .orElseThrow();
            OrderItem item = order.getItems().stream()
                .filter(candidate -> sku.equalsIgnoreCase(candidate.getProduct().resolveCatalogSku()))
                .findFirst()
                .orElseThrow();
            Inventory inventory = inventoryFor(order.getTenant().getCode(), sku, warehouseCode);
            FulfillmentTask task = fulfillmentTaskRepository
                .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(tenantA, orderId)
                .orElseThrow();
            return new Ledger(item.getQuantity(), item.getReservedQuantity(), item.getFulfilledQuantity(),
                item.getCancelledQuantity(), item.getReturnedQuantity(), inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(), inventory.getQuantityAvailable(), order.getStatus(),
                task.getStatus(), task.getFulfilledUnits());
        });
    }

    private Ledger stockLedger(String sku, String tenantCode, String warehouseCode) {
        return transactionTemplate.execute(status -> {
            Inventory inventory = inventoryFor(tenantCode, sku, warehouseCode);
            return new Ledger(0, 0, 0, 0, 0, inventory.getQuantityOnHand(), inventory.getQuantityReserved(),
                inventory.getQuantityAvailable(), null, null, 0);
        });
    }

    private Inventory inventoryFor(String tenantCode, String sku, String warehouseCode) {
        var product = productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(tenantCode, sku)
            .orElseThrow();
        var warehouse = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(tenantCode, warehouseCode)
            .orElseThrow();
        return inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).orElseThrow();
    }

    private long successfulAuditCount(String orderId, String requestId) {
        return auditLogRepository.findAll().stream()
            .filter(log -> tenantA.equalsIgnoreCase(log.getTenantCode()))
            .filter(log -> orderId.equals(log.getTargetRef()))
            .filter(log -> requestId.equals(log.getRequestId()))
            .filter(log -> log.getStatus() == AuditStatus.SUCCESS)
            .count();
    }

    private void awaitDispatchCompletion(Set<String> requestIds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DISPATCH_TIMEOUT_MILLIS);
        while (System.nanoTime() < deadline) {
            List<OperationalDispatchWorkItem> items = dispatchWorkItemRepository.findAll().stream()
                .filter(item -> tenantA.equalsIgnoreCase(item.getTenantCode()))
                .filter(item -> requestIds.contains(item.getRequestId()))
                .toList();
            if (items.stream().map(OperationalDispatchWorkItem::getRequestId).distinct().count() == requestIds.size()
                && items.stream().allMatch(item -> item.getStatus() == OperationalDispatchStatus.COMPLETED)) {
                return;
            }
            if (!dispatchQueueService.isDraining()) {
                dispatchQueueService.processPendingWork();
            }
            Thread.sleep(DISPATCH_POLL_MILLIS);
        }
        List<OperationalDispatchWorkItem> remaining = dispatchWorkItemRepository.findAll().stream()
            .filter(item -> tenantA.equalsIgnoreCase(item.getTenantCode()))
            .filter(item -> requestIds.contains(item.getRequestId()))
            .toList();
        assertThat(remaining.stream().map(OperationalDispatchWorkItem::getRequestId).distinct().count())
            .isEqualTo(requestIds.size());
        assertThat(remaining)
            .allSatisfy(item -> assertThat(item.getStatus()).isEqualTo(OperationalDispatchStatus.COMPLETED));
    }

    private void provisionTenant(String payload) throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());
    }

    private MockHttpSession login(String tenantCode, String username, String password, String expectedRole,
                                  List<String> scopes) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content(loginPayload(tenantCode, username, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem(expectedRole)))
            .andReturn();
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        if (body.path("passwordChangeRequired").asBoolean()) {
            MvcResult changed = mockMvc.perform(post("/api/auth/session/password").session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}"
                        .formatted(password, ROLE_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
            session = (MockHttpSession) changed.getRequest().getSession(false);
        }
        JsonNode sessionBody = objectMapper.readTree(mockMvc.perform(get("/api/auth/session").session(session))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(arrayTexts(sessionBody.path("warehouseScopes"))).containsExactlyElementsOf(scopes);
        return session;
    }

    private void createProduct(MockHttpSession session, String sku) throws Exception {
        mockMvc.perform(post("/api/products").session(session)
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"%s\",\"name\":\"Layer 2 Phase 3 %s\",\"category\":\"Verification\"}"
                    .formatted(sku, sku)))
            .andExpect(status().isCreated());
    }

    private void updateInventory(MockHttpSession session, String sku, String warehouse, long quantity) throws Exception {
        mockMvc.perform(post("/api/inventory/update").session(session)
                .contentType(APPLICATION_JSON)
                .content("{\"productSku\":\"%s\",\"warehouseCode\":\"%s\",\"quantityAvailable\":%d,\"reorderThreshold\":0}"
                    .formatted(sku, warehouse, quantity)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantityOnHand").value(quantity))
            .andExpect(jsonPath("$.quantityReserved").value(0))
            .andExpect(jsonPath("$.quantityAvailable").value(quantity));
    }

    private void createOrder(MockHttpSession session, String orderId, String warehouse, String... lines) throws Exception {
        mockMvc.perform(post("/api/orders").session(session)
                .contentType(APPLICATION_JSON)
                .content("{\"externalOrderId\":\"%s\",\"warehouseCode\":\"%s\",\"items\":[%s]}"
                    .formatted(orderId, warehouse, String.join(",", lines))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.externalOrderId").value(orderId))
            .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    private org.springframework.test.web.servlet.ResultActions fulfillment(MockHttpSession session, String orderId,
                                                                            String fulfillmentStatus, Integer units,
                                                                            String sku, String requestId) throws Exception {
        return mockMvc.perform(post("/api/fulfillment/updates").session(session)
            .header("X-Request-Id", requestId)
            .contentType(APPLICATION_JSON)
            .content(fulfillmentBody(orderId, fulfillmentStatus, units, sku)));
    }

    private int fulfillmentStatus(MockHttpSession session, String orderId, String fulfillmentStatus, Integer units,
                                  String sku, String requestId) throws Exception {
        return fulfillment(session, orderId, fulfillmentStatus, units, sku, requestId)
            .andReturn().getResponse().getStatus();
    }

    private org.springframework.test.web.servlet.ResultActions transition(MockHttpSession session, String orderId,
                                                                           String orderStatus, boolean restock)
        throws Exception {
        return mockMvc.perform(post("/api/orders/" + orderId + "/transition").session(session)
            .contentType(APPLICATION_JSON)
            .content("{\"status\":\"%s\",\"restockInventory\":%s,\"note\":\"Layer 2 Phase 3 proof\"}"
                .formatted(orderStatus, restock)));
    }

    private List<Integer> runConcurrently(Callable<Integer> first, Callable<Integer> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = List.of(
            executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return first.call();
            }),
            executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return second.call();
            })
        );
        start.countDown();
        try {
            return futures.stream().map(future -> {
                try {
                    return future.get(20, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new AssertionError("Concurrent fulfillment operation did not complete cleanly.", exception);
                }
            }).toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private String line(String sku, int quantity) {
        return "{\"productSku\":\"%s\",\"quantity\":%d,\"unitPrice\":10.00}".formatted(sku, quantity);
    }

    private String fulfillmentBody(String orderId, String status, Integer units, String sku) {
        String quantity = units == null ? "null" : units.toString();
        String product = sku == null ? "null" : "\"" + sku + "\"";
        return "{\"externalOrderId\":\"%s\",\"status\":\"%s\",\"fulfilledUnits\":%s,\"fulfilledProductSku\":%s,\"note\":\"Layer 2 Phase 3 proof\"}"
            .formatted(orderId, status, quantity, product);
    }

    private String requestId(String label) {
        return "l2-p3-" + label + "-" + suffix;
    }

    private String loginPayload(String tenant, String username, String password) {
        return "{\"tenantCode\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}"
            .formatted(tenant, username, password);
    }

    private List<String> arrayTexts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private String tenantPayloadA() {
        return """
            {
              "tenantCode":"%s",
              "tenantName":"Layer 2 Phase 3 Tenant A",
              "description":"Disposable order fulfillment convergence tenant.",
              "adminFullName":"Layer 2 Phase 3 Tenant A Admin",
              "adminUsername":"p3.admin.%s",
              "adminPassword":"%s",
              "primaryLocation":"Layer 2 Region A",
              "warehouses":[
                {"code":"%s","name":"Layer 2 Phase 3 Warehouse A","location":"Region A"},
                {"code":"%s","name":"Layer 2 Phase 3 Warehouse B","location":"Region B"}
              ],
              "users":[
                {"username":"p3.admin.%s","fullName":"Layer 2 Phase 3 Tenant A Admin","operatorActorName":"Layer 2 Phase 3 Tenant A Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[]},
                {"username":"p3.integration.admin.%s","fullName":"Layer 2 Phase 3 Integration Admin","operatorActorName":"Layer 2 Phase 3 Integration Admin","roles":["INTEGRATION_ADMIN"],"warehouseScopes":["%s"],"initialPassword":"%s"},
                {"username":"p3.integration.operator.%s","fullName":"Layer 2 Phase 3 Integration Operator","operatorActorName":"Layer 2 Phase 3 Integration Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["%s"],"initialPassword":"%s"},
                {"username":"p3.warehouse.operator.%s","fullName":"Layer 2 Phase 3 Warehouse B Operator","operatorActorName":"Layer 2 Phase 3 Warehouse B Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["%s"],"initialPassword":"%s"}
              ],
              "requiredRoles":[]
            }
            """.formatted(tenantA, suffix, ADMIN_PASSWORD, warehouseA, warehouseB, suffix, suffix, warehouseA,
                INITIAL_PASSWORD, suffix, warehouseA, INITIAL_PASSWORD, suffix, warehouseB, INITIAL_PASSWORD);
    }

    private String tenantPayloadB() {
        return """
            {
              "tenantCode":"%s",
              "tenantName":"Layer 2 Phase 3 Tenant B",
              "description":"Disposable tenant isolation control.",
              "adminFullName":"Layer 2 Phase 3 Tenant B Admin",
              "adminUsername":"p3.tenantb.admin.%s",
              "adminPassword":"%s",
              "primaryLocation":"Layer 2 Region B",
              "warehouses":[{"code":"%s","name":"Layer 2 Phase 3 Tenant B Warehouse","location":"Region B"}],
              "users":[
                {"username":"p3.tenantb.admin.%s","fullName":"Layer 2 Phase 3 Tenant B Admin","operatorActorName":"Layer 2 Phase 3 Tenant B Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[]},
                {"username":"p3.tenantb.operator.%s","fullName":"Layer 2 Phase 3 Tenant B Operator","operatorActorName":"Layer 2 Phase 3 Tenant B Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["%s"],"initialPassword":"%s"}
              ],
              "requiredRoles":[]
            }
            """.formatted(tenantB, suffix, ADMIN_PASSWORD, tenantBWarehouse, suffix, suffix, tenantBWarehouse,
                INITIAL_PASSWORD);
    }

    private record Ledger(int ordered, int reserved, int fulfilled, int cancelled, int returned, long onHand,
                          long reservedStock, long available, OrderStatus orderStatus,
                          FulfillmentStatus fulfillmentStatus, int fulfilledUnits) {
        private Ledger withTerminal(OrderStatus order, FulfillmentStatus fulfillment) {
            return new Ledger(ordered, reserved, fulfilled, cancelled, returned, onHand, reservedStock, available,
                order, fulfillment, fulfilledUnits);
        }
    }
}
