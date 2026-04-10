package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.AlertType;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.ScenarioRunType;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.domain.service.SeedService;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.simulation.SimulationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MvpFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AccessOperatorRepository accessOperatorRepository;

    @Autowired
    private AccessUserRepository accessUserRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private IntegrationConnectorRepository integrationConnectorRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private BusinessEventRepository businessEventRepository;

    @Autowired
    private ScenarioRunRepository scenarioRunRepository;

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private IntegrationReplayRecordRepository integrationReplayRecordRepository;

    @Autowired
    private OperationalDispatchWorkItemRepository operationalDispatchWorkItemRepository;

    @Autowired
    private SeedService seedService;

    @Test
    void orderIngestionDeductsInventoryAndCreatesOperationalResponses() throws Exception {
        Inventory inventory = loadInventory("SKU-FLX-100", "WH-NORTH");
        long startingQuantity = inventory.getQuantityAvailable();
        long threshold = inventory.getReorderThreshold();
        long startingOrderCount = customerOrderRepository.count();
        int quantityToOrder = (int) (startingQuantity - threshold + 1);

        String requestBody = """
            {
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": %d,
                  "unitPrice": 95.00
                }
              ]
            }
            """.formatted(quantityToOrder);

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.items[0].productSku").value("SKU-FLX-100"))
            .andExpect(jsonPath("$.itemCount").value(quantityToOrder));

        Inventory updatedInventory = loadInventory("SKU-FLX-100", "WH-NORTH");
        assertThat(updatedInventory.getQuantityAvailable()).isEqualTo(startingQuantity - quantityToOrder);

        assertThat(alertRepository.findTop12ByStatusOrderByCreatedAtDesc(AlertStatus.ACTIVE))
            .anyMatch(alert -> alert.getTitle().contains("SKU-FLX-100"));
        assertThat(recommendationRepository.findTop12ByOrderByCreatedAtDesc())
            .anyMatch(recommendation -> recommendation.getTitle().contains("SKU-FLX-100"));
        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .extracting(event -> event.getEventType())
            .contains(BusinessEventType.ORDER_INGESTED, BusinessEventType.FULFILLMENT_UPDATED, BusinessEventType.LOW_STOCK_DETECTED, BusinessEventType.RECOMMENDATION_GENERATED);

        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrders").value(Math.toIntExact(startingOrderCount + 1)))
            .andExpect(jsonPath("$.recentOrderCount").value(Math.toIntExact(startingOrderCount + 1)))
            .andExpect(jsonPath("$.fulfillmentBacklogCount").value(1))
            .andExpect(jsonPath("$.delayedShipmentCount").value(0))
            .andExpect(jsonPath("$.inventoryRecordsCount").value(8))
            .andExpect(jsonPath("$.simulationRunning").value(false));
    }

    @Test
    void inventoryUpdateFlowMarksLowStockImmediately() throws Exception {
        String requestBody = """
            {
              "productSku": "SKU-ORB-440",
              "warehouseCode": "WH-COAST",
              "quantityAvailable": 4,
              "reorderThreshold": 8
            }
            """;

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productSku").value("SKU-ORB-440"))
            .andExpect(jsonPath("$.lowStock").value(true));

        assertThat(alertRepository.findTop12ByStatusOrderByCreatedAtDesc(AlertStatus.ACTIVE))
            .anyMatch(alert -> alert.getTitle().contains("SKU-ORB-440"));
        assertThat(recommendationRepository.findTop12ByOrderByCreatedAtDesc())
            .anyMatch(recommendation -> recommendation.getTitle().contains("SKU-ORB-440"));
    }

    @Test
    void lowStockAlertIsReusedWhileConditionPersists() throws Exception {
        String firstUpdate = """
            {
              "productSku": "SKU-FLX-100",
              "warehouseCode": "WH-COAST",
              "quantityAvailable": 12,
              "reorderThreshold": 14
            }
            """;

        String secondUpdate = """
            {
              "productSku": "SKU-FLX-100",
              "warehouseCode": "WH-COAST",
              "quantityAvailable": 10,
              "reorderThreshold": 14
            }
            """;

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(firstUpdate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lowStock").value(true));

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(secondUpdate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lowStock").value(true));

        long activeLowStockAlerts = alertRepository.findTop12ByStatusOrderByCreatedAtDesc(AlertStatus.ACTIVE).stream()
            .filter(alert -> alert.getType() == AlertType.LOW_STOCK)
            .filter(alert -> alert.getTitle().contains("SKU-FLX-100"))
            .count();

        assertThat(activeLowStockAlerts).isEqualTo(1);
    }

    @Test
    void lowStockAlertResolvesWhenInventoryRecoversAboveThreshold() throws Exception {
        String lowStockUpdate = """
            {
              "productSku": "SKU-PLS-330",
              "warehouseCode": "WH-NORTH",
              "quantityAvailable": 8,
              "reorderThreshold": 12
            }
            """;

        String recoveryUpdate = """
            {
              "productSku": "SKU-PLS-330",
              "warehouseCode": "WH-NORTH",
              "quantityAvailable": 18,
              "reorderThreshold": 12
            }
            """;

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(lowStockUpdate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lowStock").value(true));

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(recoveryUpdate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lowStock").value(false));

        assertThat(alertRepository.findTop12ByStatusOrderByCreatedAtDesc(AlertStatus.ACTIVE))
            .noneMatch(alert -> alert.getTitle().contains("SKU-PLS-330"));

        assertThat(alertRepository.findTop12ByOrderByCreatedAtDesc())
            .anyMatch(alert -> alert.getTitle().contains("SKU-PLS-330") && alert.getStatus() == AlertStatus.RESOLVED);
    }

    @Test
    void alertsEndpointReturnsStructuredOperationalAlertFeed() throws Exception {
        String requestBody = """
            {
              "productSku": "SKU-ORB-440",
              "warehouseCode": "WH-COAST",
              "quantityAvailable": 4,
              "reorderThreshold": 8
            }
            """;

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeAlerts[0].type").value("LOW_STOCK"))
            .andExpect(jsonPath("$.activeAlerts[0].severity").exists())
            .andExpect(jsonPath("$.activeAlerts[0].title").value(org.hamcrest.Matchers.containsString("SKU-ORB-440")))
            .andExpect(jsonPath("$.activeAlerts[0].description").exists())
            .andExpect(jsonPath("$.activeAlerts[0].impactSummary").exists())
            .andExpect(jsonPath("$.activeAlerts[0].recommendedAction").exists())
            .andExpect(jsonPath("$.activeAlerts[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.activeAlerts[0].createdAt").exists())
            .andExpect(jsonPath("$.recentAlerts[0].type").value("LOW_STOCK"));
    }

    @Test
    void resolvedAlertsRemainVisibleInRecentFeed() throws Exception {
        String lowStockUpdate = """
            {
              "productSku": "SKU-FLX-100",
              "warehouseCode": "WH-NORTH",
              "quantityAvailable": 10,
              "reorderThreshold": 20
            }
            """;

        String recoveryUpdate = """
            {
              "productSku": "SKU-FLX-100",
              "warehouseCode": "WH-NORTH",
              "quantityAvailable": 24,
              "reorderThreshold": 20
            }
            """;

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(lowStockUpdate))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(recoveryUpdate))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeAlerts.length()").value(0))
            .andExpect(jsonPath("$.recentAlerts[0].title").value(org.hamcrest.Matchers.containsString("SKU-FLX-100")))
            .andExpect(jsonPath("$.recentAlerts[0].status").value("RESOLVED"));
    }

    @Test
    void recommendationsEndpointReturnsStructuredActionGuidance() throws Exception {
        String requestBody = """
            {
              "productSku": "SKU-VDR-210",
              "warehouseCode": "WH-COAST",
              "quantityAvailable": 5,
              "reorderThreshold": 12
            }
            """;

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").exists())
            .andExpect(jsonPath("$[0].title").value(org.hamcrest.Matchers.containsString("SKU-VDR-210")))
            .andExpect(jsonPath("$[0].description").exists())
            .andExpect(jsonPath("$[0].priority").exists())
            .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void criticalInventoryPressureProducesUrgentRecommendation() throws Exception {
        String requestBody = """
            {
              "productSku": "SKU-ORB-440",
              "warehouseCode": "WH-NORTH",
              "quantityAvailable": 1,
              "reorderThreshold": 15
            }
            """;

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lowStock").value(true));

        mockMvc.perform(get("/api/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("REORDER_URGENTLY"))
            .andExpect(jsonPath("$[0].priority").value("CRITICAL"))
            .andExpect(jsonPath("$[0].title").value(org.hamcrest.Matchers.containsString("SKU-ORB-440")));
    }

    @Test
    void inventoryUpdateCreatesInventoryRecordWhenMissing() throws Exception {
        Product transientProduct = productRepository.save(Product.builder()
            .sku("SKU-NEW-101")
            .name("New Inventory Product")
            .category("Diagnostics")
            .build());

        String requestBody = """
            {
              "productSku": "%s",
              "warehouseCode": "WH-COAST",
              "quantityAvailable": 14,
              "reorderThreshold": 5
            }
            """.formatted(transientProduct.getSku());

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productSku").value("SKU-NEW-101"))
            .andExpect(jsonPath("$.warehouseCode").value("WH-COAST"))
            .andExpect(jsonPath("$.quantityAvailable").value(14))
            .andExpect(jsonPath("$.reorderThreshold").value(5))
            .andExpect(jsonPath("$.lowStock").value(false));

        Long warehouseId = warehouseRepository.findByCode("WH-COAST").orElseThrow().getId();
        Inventory createdInventory = inventoryRepository.findByProductIdAndWarehouseId(transientProduct.getId(), warehouseId)
            .orElseThrow();
        assertThat(createdInventory.getQuantityAvailable()).isEqualTo(14);
        assertThat(createdInventory.getReorderThreshold()).isEqualTo(5);
    }

    @Test
    void inventoryEndpointReturnsOperationalViewOfCurrentState() throws Exception {
        mockMvc.perform(get("/api/inventory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].productSku").exists())
            .andExpect(jsonPath("$[0].warehouseCode").exists())
            .andExpect(jsonPath("$[0].quantityAvailable").exists())
            .andExpect(jsonPath("$[0].reorderThreshold").exists())
            .andExpect(jsonPath("$[0].riskLevel").exists());
    }

    @Test
    void recentOrdersEndpointReturnsOperationalHistoryForDashboardUse() throws Exception {
        String firstOrder = """
            {
              "externalOrderId": "ORD-RECENT-001",
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 2,
                  "unitPrice": 95.00
                }
              ]
            }
            """;

        String secondOrder = """
            {
              "externalOrderId": "ORD-RECENT-002",
              "warehouseCode": "WH-COAST",
              "items": [
                {
                  "productSku": "SKU-VDR-210",
                  "quantity": 1,
                  "unitPrice": 140.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(firstOrder))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(secondOrder))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].externalOrderId").value("ORD-RECENT-002"))
            .andExpect(jsonPath("$[0].warehouseCode").value("WH-COAST"))
            .andExpect(jsonPath("$[0].itemCount").value(1))
            .andExpect(jsonPath("$[0].items[0].productSku").value("SKU-VDR-210"))
            .andExpect(jsonPath("$[1].externalOrderId").value("ORD-RECENT-001"));
    }

    @Test
    void orderIngestionCreatesQueuedFulfillmentLaneAndExposesOverview() throws Exception {
        String requestBody = """
            {
              "externalOrderId": "FULFILL-1001",
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 1,
                  "unitPrice": 95.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated());

        assertThat(fulfillmentTaskRepository.findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId("SYNAPSE-DEMO", "FULFILL-1001"))
            .get()
            .extracting(task -> task.getStatus())
            .isEqualTo(FulfillmentStatus.QUEUED);

        mockMvc.perform(get("/api/fulfillment"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.backlogCount").value(1))
            .andExpect(jsonPath("$.activeFulfillments[0].externalOrderId").value("FULFILL-1001"))
            .andExpect(jsonPath("$.activeFulfillments[0].fulfillmentStatus").value("QUEUED"))
            .andExpect(jsonPath("$.activeFulfillments[0].impactSummary").exists());
    }

    @Test
    void fulfillmentUpdatesSurfaceBacklogAndDeliveryRiskSignals() throws Exception {
        for (int index = 1; index <= 4; index++) {
            String requestBody = """
                {
                  "externalOrderId": "FULFILL-BLK-%d",
                  "warehouseCode": "WH-NORTH",
                  "items": [
                    {
                      "productSku": "SKU-FLX-100",
                      "quantity": 1,
                      "unitPrice": 95.00
                    }
                  ]
                }
                """.formatted(index);

            mockMvc.perform(post("/api/orders")
                    .contentType(APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/fulfillment"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.backlogCount").value(4))
            .andExpect(jsonPath("$.atRiskCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recentAlerts[?(@.type == 'FULFILLMENT_BACKLOG')]").isNotEmpty());

        String delayUpdate = """
            {
              "externalOrderId": "FULFILL-BLK-1",
              "status": "DELAYED",
              "carrier": "Synapse Courier",
              "trackingReference": "TRK-FULFILL-BLK-1",
              "expectedDeliveryAt": "2026-04-01T08:00:00Z",
              "note": "Carrier lane fell behind the expected handoff."
            }
            """;

        mockMvc.perform(post("/api/fulfillment/updates")
                .contentType(APPLICATION_JSON)
                .content(delayUpdate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fulfillmentStatus").value("DELAYED"))
            .andExpect(jsonPath("$.deliveryDelayRisk").value(true))
            .andExpect(jsonPath("$.trackingReference").value("TRK-FULFILL-BLK-1"));

        mockMvc.perform(get("/api/fulfillment"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.delayedShipmentCount").value(1))
            .andExpect(jsonPath("$.activeFulfillments[?(@.externalOrderId == 'FULFILL-BLK-1')].fulfillmentStatus").value(org.hamcrest.Matchers.hasItem("DELAYED")));

        mockMvc.perform(get("/api/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.type == 'ESCALATE_LOGISTICS' || @.type == 'PRIORITIZE_FULFILLMENT')]").isNotEmpty());

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .extracting(event -> event.getEventType())
            .contains(BusinessEventType.FULFILLMENT_BACKLOG_DETECTED, BusinessEventType.DELIVERY_DELAY_REPORTED);
    }

    @Test
    void recentEventsEndpointReturnsOperationalEventTrail() throws Exception {
        String requestBody = """
            {
              "productSku": "SKU-ORB-440",
              "warehouseCode": "WH-COAST",
              "quantityAvailable": 4,
              "reorderThreshold": 8
            }
            """;

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/events/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventType").exists())
            .andExpect(jsonPath("$[0].source").exists())
            .andExpect(jsonPath("$[0].payloadSummary").exists())
            .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void productsEndpointReturnsSeededCatalogForReferenceViews() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].name").value("Flux Sensor"))
            .andExpect(jsonPath("$[0].sku").value("SKU-FLX-100"))
            .andExpect(jsonPath("$[1].name").value("Orbit Valve"))
            .andExpect(jsonPath("$[2].name").value("Pulse Relay"))
            .andExpect(jsonPath("$[3].name").value("Vector Drive"));
    }

    @Test
    void warehousesEndpointReturnsSeededLocationsForOperationalContext() throws Exception {
        mockMvc.perform(get("/api/warehouses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Warehouse Coast"))
            .andExpect(jsonPath("$[0].code").value("WH-COAST"))
            .andExpect(jsonPath("$[0].location").value("Durban"))
            .andExpect(jsonPath("$[1].name").value("Warehouse North"))
            .andExpect(jsonPath("$[1].code").value("WH-NORTH"))
            .andExpect(jsonPath("$[1].location").value("Johannesburg"));
    }

    @Test
    void dashboardSummaryReturnsFullOperationalMetricSet() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrders").exists())
            .andExpect(jsonPath("$.activeAlerts").exists())
            .andExpect(jsonPath("$.lowStockItems").exists())
            .andExpect(jsonPath("$.recommendationsCount").exists())
            .andExpect(jsonPath("$.fulfillmentBacklogCount").exists())
            .andExpect(jsonPath("$.delayedShipmentCount").exists())
            .andExpect(jsonPath("$.fulfillmentRiskCount").exists())
            .andExpect(jsonPath("$.totalProducts").value(4))
            .andExpect(jsonPath("$.totalWarehouses").value(2))
            .andExpect(jsonPath("$.recentOrderCount").exists())
            .andExpect(jsonPath("$.inventoryRecordsCount").value(8))
            .andExpect(jsonPath("$.simulationRunning").exists())
            .andExpect(jsonPath("$.lastUpdatedAt").exists());
    }

    @Test
    void dashboardSnapshotReturnsExpandedControlCenterData() throws Exception {
        String requestBody = """
            {
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 1,
                  "unitPrice": 95.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dashboard/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary").exists())
            .andExpect(jsonPath("$.fulfillment").exists())
            .andExpect(jsonPath("$.fulfillment.activeFulfillments[0].externalOrderId").exists())
            .andExpect(jsonPath("$.recentOrders[0].externalOrderId").exists())
            .andExpect(jsonPath("$.recentEvents[0].eventType").exists())
            .andExpect(jsonPath("$.auditLogs[0].action").exists())
            .andExpect(jsonPath("$.auditLogs[0].requestId").exists())
            .andExpect(jsonPath("$.recentScenarios").isArray())
            .andExpect(jsonPath("$.simulation.active").value(false))
            .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    void scenarioOrderImpactProjectsRiskWithoutPersistingOperationalState() throws Exception {
        Inventory inventoryBefore = loadInventory("SKU-FLX-100", "WH-NORTH");
        long startingQuantity = inventoryBefore.getQuantityAvailable();
        long startingOrders = customerOrderRepository.count();

        String requestBody = """
            {
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 9,
                  "unitPrice": 95.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/scenarios/order-impact")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.warehouseName").value("Warehouse North"))
            .andExpect(jsonPath("$.projectedOrderValue").value(855.0))
            .andExpect(jsonPath("$.totalUnits").value(9))
            .andExpect(jsonPath("$.projectedInventory[0].productSku").value("SKU-FLX-100"))
            .andExpect(jsonPath("$.projectedInventory[0].quantityAvailable").value(19))
            .andExpect(jsonPath("$.projectedInventory[0].lowStock").value(true))
            .andExpect(jsonPath("$.projectedAlerts[0].type").value("LOW_STOCK"))
            .andExpect(jsonPath("$.projectedRecommendations[0].type").value("TRANSFER_STOCK"))
            .andExpect(jsonPath("$.analyzedAt").exists());

        Inventory inventoryAfter = loadInventory("SKU-FLX-100", "WH-NORTH");
        assertThat(inventoryAfter.getQuantityAvailable()).isEqualTo(startingQuantity);
        assertThat(customerOrderRepository.count()).isEqualTo(startingOrders);
    }

    @Test
    void scenarioOrderImpactSupportsMultiLineOrderMixes() throws Exception {
        long startingOrders = customerOrderRepository.count();

        String requestBody = """
            {
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 9,
                  "unitPrice": 95.00
                },
                {
                  "productSku": "SKU-ORB-440",
                  "quantity": 3,
                  "unitPrice": 120.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/scenarios/order-impact")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectedOrderValue").value(1215.0))
            .andExpect(jsonPath("$.totalUnits").value(12))
            .andExpect(jsonPath("$.projectedInventory.length()").value(2))
            .andExpect(jsonPath("$..projectedInventory[?(@.productSku == 'SKU-FLX-100')].quantityAvailable")
                .value(org.hamcrest.Matchers.hasItem(19)))
            .andExpect(jsonPath("$..projectedInventory[?(@.productSku == 'SKU-FLX-100')].lowStock")
                .value(org.hamcrest.Matchers.hasItem(true)))
            .andExpect(jsonPath("$..projectedInventory[?(@.productSku == 'SKU-ORB-440')].quantityAvailable")
                .value(org.hamcrest.Matchers.hasItem(29)))
            .andExpect(jsonPath("$..projectedInventory[?(@.productSku == 'SKU-ORB-440')].lowStock")
                .value(org.hamcrest.Matchers.hasItem(false)));

        assertThat(customerOrderRepository.count()).isEqualTo(startingOrders);
    }

    @Test
    void scenarioComparisonRecommendsTheSaferOrderMix() throws Exception {
        String requestBody = """
            {
              "primaryLabel": "Conservative",
              "primary": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 2,
                    "unitPrice": 95.00
                  }
                ]
              },
              "alternativeLabel": "Aggressive",
              "alternative": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 9,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/order-impact/compare")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.primaryLabel").value("Conservative"))
            .andExpect(jsonPath("$.alternativeLabel").value("Aggressive"))
            .andExpect(jsonPath("$.primary.projectedInventory[0].lowStock").value(false))
            .andExpect(jsonPath("$.alternative.projectedInventory[0].lowStock").value(true))
            .andExpect(jsonPath("$.summary.recommendedOption").value("Conservative"))
            .andExpect(jsonPath("$.summary.primaryRiskScore").value(0))
            .andExpect(jsonPath("$.summary.alternativeRiskScore").value(org.hamcrest.Matchers.greaterThan(0)))
            .andExpect(jsonPath("$.summary.rationale").value(org.hamcrest.Matchers.containsString("Conservative")));
    }

    @Test
    void namedScenarioPlanCanBeSavedAndExposedInHistory() throws Exception {
        String requestBody = """
            {
              "title": "North replenishment option",
              "requestedBy": "Amina Planner",
              "request": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 4,
                    "unitPrice": 95.00
                  },
                  {
                    "productSku": "SKU-ORB-440",
                    "quantity": 2,
                    "unitPrice": 120.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("SAVED_PLAN"))
            .andExpect(jsonPath("$.title").value("North replenishment option"))
            .andExpect(jsonPath("$.warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.requestedBy").value("Amina Planner"))
            .andExpect(jsonPath("$.executable").value(false))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$.savedAt").exists());

        mockMvc.perform(get("/api/scenarios/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("SAVED_PLAN"))
            .andExpect(jsonPath("$[0].title").value("North replenishment option"))
            .andExpect(jsonPath("$[0].warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$[0].requestedBy").value("Amina Planner"))
            .andExpect(jsonPath("$[0].approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$[0].loadable").value(true))
            .andExpect(jsonPath("$[0].executable").value(false));

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_SAVED);
    }

    @Test
    void scenarioHistoryAndSnapshotExposePlanningMemory() throws Exception {
        String previewBody = """
            {
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 4,
                  "unitPrice": 95.00
                }
              ]
            }
            """;

        String comparisonBody = """
            {
              "primaryLabel": "Balanced",
              "primary": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 2,
                    "unitPrice": 95.00
                  }
                ]
              },
              "alternativeLabel": "Aggressive",
              "alternative": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 9,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/order-impact")
                .contentType(APPLICATION_JSON)
                .content(previewBody))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/scenarios/order-impact/compare")
                .contentType(APPLICATION_JSON)
                .content(comparisonBody))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/scenarios/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("COMPARISON"))
            .andExpect(jsonPath("$[0].title").value(org.hamcrest.Matchers.containsString("Balanced vs Aggressive")))
            .andExpect(jsonPath("$[0].recommendedOption").value("Balanced"))
            .andExpect(jsonPath("$[0].loadable").value(false))
            .andExpect(jsonPath("$[0].executable").value(false))
            .andExpect(jsonPath("$[0].approvalStatus").value("NOT_REQUIRED"))
            .andExpect(jsonPath("$[1].type").value("PREVIEW"))
            .andExpect(jsonPath("$[1].warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$[1].loadable").value(true))
            .andExpect(jsonPath("$[1].executable").value(true))
            .andExpect(jsonPath("$[1].approvalStatus").value("NOT_REQUIRED"))
            .andExpect(jsonPath("$[1].title").value(org.hamcrest.Matchers.containsString("WH-NORTH")))
            .andExpect(jsonPath("$[1].summary").exists())
            .andExpect(jsonPath("$[1].createdAt").exists());

        mockMvc.perform(get("/api/dashboard/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recentScenarios[0].type").value("COMPARISON"))
            .andExpect(jsonPath("$.recentScenarios[0].recommendedOption").value("Balanced"))
            .andExpect(jsonPath("$.recentScenarios[1].type").value("PREVIEW"))
            .andExpect(jsonPath("$.recentScenarios[1].loadable").value(true))
            .andExpect(jsonPath("$.recentScenarios[1].executable").value(true));

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_ANALYZED, BusinessEventType.SCENARIO_COMPARED);
    }

    @Test
    void executableScenarioPreviewCanBePromotedIntoLiveOrderFlow() throws Exception {
        Inventory inventoryBefore = loadInventory("SKU-FLX-100", "WH-NORTH");
        long startingQuantity = inventoryBefore.getQuantityAvailable();
        long startingOrders = customerOrderRepository.count();

        String previewBody = """
            {
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 4,
                  "unitPrice": 95.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/scenarios/order-impact")
                .contentType(APPLICATION_JSON)
                .content(previewBody))
            .andExpect(status().isOk());

        Long scenarioRunId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.PREVIEW)
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/scenarios/" + scenarioRunId + "/execute"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioRunId").value(scenarioRunId))
            .andExpect(jsonPath("$.scenarioTitle").value(org.hamcrest.Matchers.containsString("WH-NORTH")))
            .andExpect(jsonPath("$.order.externalOrderId").exists())
            .andExpect(jsonPath("$.order.warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.order.itemCount").value(4))
            .andExpect(jsonPath("$.executedAt").exists());

        Inventory inventoryAfter = loadInventory("SKU-FLX-100", "WH-NORTH");
        assertThat(inventoryAfter.getQuantityAvailable()).isEqualTo(startingQuantity - 4);
        assertThat(customerOrderRepository.count()).isEqualTo(startingOrders + 1);

        mockMvc.perform(get("/api/scenarios/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("EXECUTION"))
            .andExpect(jsonPath("$[0].recommendedOption").exists());

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_EXECUTED, BusinessEventType.ORDER_INGESTED);
    }

    @Test
    void executableScenarioPreviewCanBeLoadedBackIntoPlanner() throws Exception {
        String previewBody = """
            {
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 4,
                  "unitPrice": 95.00
                },
                {
                  "productSku": "SKU-ORB-440",
                  "quantity": 2,
                  "unitPrice": 120.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/scenarios/order-impact")
                .contentType(APPLICATION_JSON)
                .content(previewBody))
            .andExpect(status().isOk());

        Long scenarioRunId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.PREVIEW)
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(get("/api/scenarios/" + scenarioRunId + "/request"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioRunId").value(scenarioRunId))
            .andExpect(jsonPath("$.scenarioTitle").value(org.hamcrest.Matchers.containsString("WH-NORTH")))
            .andExpect(jsonPath("$.request.warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.request.externalOrderId").doesNotExist())
            .andExpect(jsonPath("$.request.items.length()").value(2))
            .andExpect(jsonPath("$.request.items[0].productSku").value("SKU-FLX-100"))
            .andExpect(jsonPath("$.loadedAt").exists());
    }

    @Test
    void savedScenarioPlanRequiresApprovalBeforeExecution() throws Exception {
        Inventory inventoryBefore = loadInventory("SKU-FLX-100", "WH-NORTH");
        long startingQuantity = inventoryBefore.getQuantityAvailable();
        long startingOrders = customerOrderRepository.count();

        String saveBody = """
            {
              "title": "North execution candidate",
              "requestedBy": "Lebo Planner",
              "request": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 3,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(saveBody))
            .andExpect(status().isCreated());

        Long savedPlanId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.SAVED_PLAN)
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(get("/api/scenarios/" + savedPlanId + "/request"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioRunId").value(savedPlanId))
            .andExpect(jsonPath("$.scenarioTitle").value("North execution candidate"))
            .andExpect(jsonPath("$.request.warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.request.items[0].quantity").value(3));

        mockMvc.perform(post("/api/scenarios/" + savedPlanId + "/execute"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("approved saved plans")));

        mockMvc.perform(post("/api/scenarios/" + savedPlanId + "/approve")
                .with(accessHeaders("Naledi Lead", "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "approverName": "Naledi Lead"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioRunId").value(savedPlanId))
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("Naledi Lead"))
            .andExpect(jsonPath("$.approvedAt").exists());

        mockMvc.perform(get("/api/scenarios/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("SAVED_PLAN"))
            .andExpect(jsonPath("$[0].approvalStatus").value("APPROVED"))
            .andExpect(jsonPath("$[0].approvedBy").value("Naledi Lead"))
            .andExpect(jsonPath("$[0].executable").value(true));

        mockMvc.perform(post("/api/scenarios/" + savedPlanId + "/execute"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioRunId").value(savedPlanId))
            .andExpect(jsonPath("$.order.warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.order.itemCount").value(3));

        Inventory inventoryAfter = loadInventory("SKU-FLX-100", "WH-NORTH");
        assertThat(inventoryAfter.getQuantityAvailable()).isEqualTo(startingQuantity - 3);
        assertThat(customerOrderRepository.count()).isEqualTo(startingOrders + 1);

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_SAVED, BusinessEventType.SCENARIO_APPROVED, BusinessEventType.SCENARIO_EXECUTED);
    }

    @Test
    void rejectedSavedPlanRemainsLoadableButCannotBeApprovedOrExecuted() throws Exception {
        String saveBody = """
            {
              "title": "Coast safety hold",
              "requestedBy": "Ayo Planner",
              "revisionOfScenarioRunId": null,
              "request": {
                "warehouseCode": "WH-COAST",
                "items": [
                  {
                    "productSku": "SKU-VDR-210",
                    "quantity": 2,
                    "unitPrice": 140.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(saveBody))
            .andExpect(status().isCreated());

        Long savedPlanId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.SAVED_PLAN)
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/scenarios/" + savedPlanId + "/reject")
                .with(accessHeaders("Naledi Lead", "FINAL_APPROVER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "FINAL_APPROVER",
                      "reviewerName": "Naledi Lead",
                      "reason": "Hold until supplier ETA is confirmed."
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires actor role REVIEW_OWNER")));

        mockMvc.perform(post("/api/scenarios/" + savedPlanId + "/reject")
                .with(accessHeaders("Naledi Lead", "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "reviewerName": "Naledi Lead",
                      "reason": "Hold until supplier ETA is confirmed."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("REJECTED"))
            .andExpect(jsonPath("$.rejectedBy").value("Naledi Lead"))
            .andExpect(jsonPath("$.rejectionReason").value("Hold until supplier ETA is confirmed."));

        mockMvc.perform(get("/api/scenarios/history")
                .param("approvalStatus", "REJECTED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Coast safety hold"))
            .andExpect(jsonPath("$[0].loadable").value(true))
            .andExpect(jsonPath("$[0].executable").value(false))
            .andExpect(jsonPath("$[0].rejectedBy").value("Naledi Lead"))
            .andExpect(jsonPath("$[0].rejectionReason").value("Hold until supplier ETA is confirmed."));

        mockMvc.perform(get("/api/scenarios/" + savedPlanId + "/request"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioTitle").value("Coast safety hold"));

        mockMvc.perform(post("/api/scenarios/" + savedPlanId + "/approve")
                .with(accessHeaders("Ops Director", "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "approverName": "Ops Director"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("save a new plan")));

        mockMvc.perform(post("/api/scenarios/" + savedPlanId + "/execute"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("approved saved plans")));

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_SAVED, BusinessEventType.SCENARIO_REJECTED);
    }

    @Test
    void rejectedSavedPlanCanBeResubmittedAsRevisionThroughSaveFlow() throws Exception {
        String initialSaveBody = """
            {
              "title": "North review candidate",
              "requestedBy": "Ayo Planner",
              "revisionOfScenarioRunId": null,
              "request": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 2,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(initialSaveBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.revisionNumber").value(1));

        Long rejectedPlanId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.SAVED_PLAN)
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/scenarios/" + rejectedPlanId + "/reject")
                .with(accessHeaders("Naledi Lead", "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "reviewerName": "Naledi Lead",
                      "reason": "Raise safety buffer before submitting."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("REJECTED"));

        String revisionSaveBody = """
            {
              "title": "North review candidate revised",
              "requestedBy": "Ayo Planner",
              "revisionOfScenarioRunId": %d,
              "request": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 1,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """.formatted(rejectedPlanId);

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(revisionSaveBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("North review candidate revised"))
            .andExpect(jsonPath("$.revisionOfScenarioRunId").value(rejectedPlanId))
            .andExpect(jsonPath("$.revisionNumber").value(2))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$.executable").value(false));

        mockMvc.perform(get("/api/scenarios/history")
                .param("type", "SAVED_PLAN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("North review candidate revised"))
            .andExpect(jsonPath("$[0].revisionOfScenarioRunId").value(rejectedPlanId))
            .andExpect(jsonPath("$[0].revisionNumber").value(2))
            .andExpect(jsonPath("$[0].approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$[1].approvalStatus").value("REJECTED"));

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_SAVED, BusinessEventType.SCENARIO_REJECTED, BusinessEventType.SCENARIO_RESUBMITTED);
    }

    @Test
    void scenarioHistorySupportsOperationalFilteringByTypeApprovalWarehouseAndRequester() throws Exception {
        String approvedNorthPlan = """
            {
              "title": "North replenishment plan",
              "requestedBy": "Thando Planner",
              "reviewOwner": "Naledi Lead",
              "request": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 2,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        String pendingCoastPlan = """
            {
              "title": "Coast overflow plan",
              "requestedBy": "Lebo Planner",
              "reviewOwner": "Jordan Lead",
              "request": {
                "warehouseCode": "WH-COAST",
                "items": [
                  {
                    "productSku": "SKU-VDR-210",
                    "quantity": 1,
                    "unitPrice": 140.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(approvedNorthPlan))
            .andExpect(status().isCreated());

        Long approvedPlanId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.SAVED_PLAN)
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/scenarios/" + approvedPlanId + "/approve")
                .with(accessHeaders("Naledi Lead", "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "approverName": "Naledi Lead"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(pendingCoastPlan))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/scenarios/history")
                .param("type", "SAVED_PLAN")
                .param("approvalStatus", "APPROVED")
                .param("warehouseCode", "wh-north")
                .param("requestedBy", "thando"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("North replenishment plan"))
            .andExpect(jsonPath("$[0].warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$[0].requestedBy").value("Thando Planner"))
            .andExpect(jsonPath("$[0].reviewOwner").value("Naledi Lead"))
            .andExpect(jsonPath("$[0].approvalStatus").value("APPROVED"))
            .andExpect(jsonPath("$[0].executable").value(true));
    }

    @Test
    void scenarioHistoryCanFilterPendingReviewQueueByReviewOwner() throws Exception {
        String firstPlan = """
            {
              "title": "North queue candidate",
              "requestedBy": "Ayo Planner",
              "reviewOwner": "Naledi Lead",
              "request": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 1,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        String secondPlan = """
            {
              "title": "Coast queue candidate",
              "requestedBy": "Ayo Planner",
              "reviewOwner": "Jordan Lead",
              "request": {
                "warehouseCode": "WH-COAST",
                "items": [
                  {
                    "productSku": "SKU-VDR-210",
                    "quantity": 1,
                    "unitPrice": 140.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(firstPlan))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reviewOwner").value("Naledi Lead"));

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(secondPlan))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reviewOwner").value("Jordan Lead"));

        mockMvc.perform(get("/api/scenarios/history")
                .param("approvalStatus", "PENDING_APPROVAL")
                .param("reviewOwner", "naledi"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("North queue candidate"))
            .andExpect(jsonPath("$[0].reviewOwner").value("Naledi Lead"))
            .andExpect(jsonPath("$[0].approvalStatus").value("PENDING_APPROVAL"));
    }

    @Test
    void savedPlansStoreReviewPriorityAndSupportHighRiskQueueFiltering() throws Exception {
        String routinePlan = """
            {
              "title": "Coast routine queue candidate",
              "requestedBy": "Ayo Planner",
              "reviewOwner": "Naledi Lead",
              "request": {
                "warehouseCode": "WH-COAST",
                "items": [
                  {
                    "productSku": "SKU-VDR-210",
                    "quantity": 1,
                    "unitPrice": 140.00
                  }
                ]
              }
            }
            """;

        String criticalPlan = """
            {
              "title": "North critical queue candidate",
              "requestedBy": "Ayo Planner",
              "reviewOwner": "Naledi Lead",
              "request": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 18,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(routinePlan))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reviewOwner").value("Naledi Lead"))
            .andExpect(jsonPath("$.reviewPriority").value("MEDIUM"))
            .andExpect(jsonPath("$.approvalPolicy").value("STANDARD"))
            .andExpect(jsonPath("$.riskScore").value(0));

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(criticalPlan))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reviewOwner").value("Naledi Lead"))
            .andExpect(jsonPath("$.reviewPriority").value("CRITICAL"))
            .andExpect(jsonPath("$.approvalPolicy").value("ESCALATED"))
            .andExpect(jsonPath("$.riskScore").value(135));

        mockMvc.perform(get("/api/scenarios/history")
                .param("approvalStatus", "PENDING_APPROVAL")
                .param("reviewOwner", "naledi")
                .param("minimumReviewPriority", "HIGH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("North critical queue candidate"))
            .andExpect(jsonPath("$[0].reviewOwner").value("Naledi Lead"))
            .andExpect(jsonPath("$[0].reviewPriority").value("CRITICAL"))
            .andExpect(jsonPath("$[0].approvalPolicy").value("ESCALATED"))
            .andExpect(jsonPath("$[0].riskScore").value(135))
            .andExpect(jsonPath("$[0].approvalStatus").value("PENDING_APPROVAL"));
    }

    @Test
    void escalatedPlansRequireAssignedReviewerAndApprovalNote() throws Exception {
        String criticalPlan = """
            {
              "title": "North escalation candidate",
              "requestedBy": "Ayo Planner",
              "reviewOwner": "Naledi Lead",
              "request": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 18,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(criticalPlan))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.approvalPolicy").value("ESCALATED"))
            .andExpect(jsonPath("$.approvalStage").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.approvalDueAt").exists())
            .andExpect(jsonPath("$.slaEscalated").value(false))
            .andExpect(jsonPath("$.overdue").value(false))
            .andExpect(jsonPath("$.finalApprovalOwner").value("North Operations Director"));

        Long scenarioId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.SAVED_PLAN)
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/approve")
                .with(accessHeaders("Ayo Planner", "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "approverName": "Ayo Planner"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires an approval note")));

        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/approve")
                .with(accessHeaders("Jordan Lead", "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "approverName": "Jordan Lead",
                      "approvalNote": "Projected exposure is acceptable."
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned review owner")));

        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/approve")
                .with(accessHeaders("Naledi Lead", "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "approverName": "Naledi Lead",
                      "approvalNote": "Owner review confirms the exposure and routing assumptions."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$.approvalPolicy").value("ESCALATED"))
            .andExpect(jsonPath("$.approvalStage").value("PENDING_FINAL_APPROVAL"))
            .andExpect(jsonPath("$.approvalDueAt").exists())
            .andExpect(jsonPath("$.slaEscalated").value(false))
            .andExpect(jsonPath("$.overdue").value(false))
            .andExpect(jsonPath("$.reviewApprovedBy").value("Naledi Lead"))
            .andExpect(jsonPath("$.executionReady").value(false));

        var overdueRun = scenarioRunRepository.findById(scenarioId).orElseThrow();
        overdueRun.setApprovalDueAt(Instant.now().minusSeconds(300));
        scenarioRunRepository.saveAndFlush(overdueRun);

        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/execute"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot be executed")));

        mockMvc.perform(get("/api/scenarios/history")
                .param("approvalPolicy", "ESCALATED")
                .param("approvalStage", "PENDING_FINAL_APPROVAL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("North escalation candidate"))
            .andExpect(jsonPath("$[0].approvalStage").value("PENDING_FINAL_APPROVAL"))
            .andExpect(jsonPath("$[0].finalApprovalOwner").value("Executive Operations Director"))
            .andExpect(jsonPath("$[0].approvalDueAt").exists())
            .andExpect(jsonPath("$[0].slaEscalated").value(true))
            .andExpect(jsonPath("$[0].slaEscalatedTo").value("Executive Operations Director"))
            .andExpect(jsonPath("$[0].slaEscalatedAt").exists())
            .andExpect(jsonPath("$[0].overdue").value(true))
            .andExpect(jsonPath("$[0].reviewApprovedBy").value("Naledi Lead"))
            .andExpect(jsonPath("$[0].reviewApprovalNote").value(org.hamcrest.Matchers.containsString("Owner review confirms")));

        mockMvc.perform(get("/api/scenarios/history")
                .param("approvalStatus", "PENDING_APPROVAL")
                .param("overdueOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("North escalation candidate"))
            .andExpect(jsonPath("$[0].slaEscalated").value(true))
            .andExpect(jsonPath("$[0].overdue").value(true));

        mockMvc.perform(get("/api/scenarios/history")
                .param("approvalStatus", "PENDING_APPROVAL")
                .param("slaEscalatedOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("North escalation candidate"))
            .andExpect(jsonPath("$[0].slaEscalatedTo").value("Executive Operations Director"));

        mockMvc.perform(get("/api/dashboard/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioNotifications.length()").value(1))
            .andExpect(jsonPath("$.scenarioNotifications[0].type").value("SLA_ESCALATED"))
            .andExpect(jsonPath("$.scenarioNotifications[0].title").value(org.hamcrest.Matchers.containsString("North escalation candidate")))
            .andExpect(jsonPath("$.scenarioNotifications[0].actor").value("Executive Operations Director"))
            .andExpect(jsonPath("$.scenarioNotifications[0].actionRequired").value(true))
            .andExpect(jsonPath("$.slaEscalations.length()").value(1))
            .andExpect(jsonPath("$.slaEscalations[0].title").value("North escalation candidate"))
            .andExpect(jsonPath("$.slaEscalations[0].slaEscalated").value(true))
            .andExpect(jsonPath("$.slaEscalations[0].slaEscalatedTo").value("Executive Operations Director"));

        mockMvc.perform(get("/api/scenarios/notifications"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("SLA_ESCALATED"))
            .andExpect(jsonPath("$[0].scenarioRunId").value(scenarioId))
            .andExpect(jsonPath("$[0].actionRequired").value(true))
            .andExpect(jsonPath("$[0].actor").value("Executive Operations Director"));

        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/acknowledge-escalation")
                .with(accessHeaders("Lebo Ops", "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "acknowledgedBy": "Lebo Ops",
                      "note": "Escalation accepted and routing owner informed."
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires actor role ESCALATION_OWNER")));

        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/acknowledge-escalation")
                .with(accessHeaders("Lebo Ops", "ESCALATION_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "ESCALATION_OWNER",
                      "acknowledgedBy": "Lebo Ops",
                      "note": "Escalation accepted and routing owner informed."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slaEscalated").value(true))
            .andExpect(jsonPath("$.slaAcknowledged").value(true))
            .andExpect(jsonPath("$.slaAcknowledgedBy").value("Lebo Ops"))
            .andExpect(jsonPath("$.slaAcknowledgementNote").value(org.hamcrest.Matchers.containsString("routing owner informed")));

        mockMvc.perform(get("/api/dashboard/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioNotifications.length()").value(1))
            .andExpect(jsonPath("$.scenarioNotifications[0].type").value("SLA_ACKNOWLEDGED"))
            .andExpect(jsonPath("$.scenarioNotifications[0].actor").value("Lebo Ops"))
            .andExpect(jsonPath("$.scenarioNotifications[0].actionRequired").value(false))
            .andExpect(jsonPath("$.slaEscalations.length()").value(0));

        mockMvc.perform(get("/api/scenarios/notifications"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("SLA_ACKNOWLEDGED"))
            .andExpect(jsonPath("$[0].scenarioRunId").value(scenarioId))
            .andExpect(jsonPath("$[0].actor").value("Lebo Ops"))
            .andExpect(jsonPath("$[0].note").value(org.hamcrest.Matchers.containsString("routing owner informed")))
            .andExpect(jsonPath("$[0].actionRequired").value(false));

        mockMvc.perform(get("/api/scenarios/history")
                .param("approvalStatus", "PENDING_APPROVAL")
                .param("slaEscalatedOnly", "true")
                .param("slaAcknowledged", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("North escalation candidate"))
            .andExpect(jsonPath("$[0].slaAcknowledged").value(true))
            .andExpect(jsonPath("$[0].slaAcknowledgedBy").value("Lebo Ops"));

        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/approve")
                .with(accessHeaders("Naledi Lead", "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "approverName": "Naledi Lead",
                      "approvalNote": "Final approval after review."
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires actor role FINAL_APPROVER")));

        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/approve")
                .with(accessHeaders("North Operations Director", "FINAL_APPROVER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "FINAL_APPROVER",
                      "approverName": "North Operations Director",
                      "approvalNote": "Final approval after review."
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned final approval owner")));

        mockMvc.perform(get("/api/scenarios/history")
                .param("approvalStage", "PENDING_FINAL_APPROVAL")
                .param("finalApprovalOwner", "executive operations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("North escalation candidate"))
            .andExpect(jsonPath("$[0].finalApprovalOwner").value("Executive Operations Director"))
            .andExpect(jsonPath("$[0].slaEscalated").value(true))
            .andExpect(jsonPath("$[0].overdue").value(true));

        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/approve")
                .with(accessHeaders("Executive Operations Director", "FINAL_APPROVER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "FINAL_APPROVER",
                      "approverName": "Executive Operations Director",
                      "approvalNote": "Final approval granted after owner review and risk acknowledgement."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
            .andExpect(jsonPath("$.approvalPolicy").value("ESCALATED"))
            .andExpect(jsonPath("$.approvalStage").value("APPROVED"))
            .andExpect(jsonPath("$.finalApprovalOwner").value("Executive Operations Director"))
            .andExpect(jsonPath("$.reviewApprovedBy").value("Naledi Lead"))
            .andExpect(jsonPath("$.approvedBy").value("Executive Operations Director"))
            .andExpect(jsonPath("$.approvalNote").value(org.hamcrest.Matchers.containsString("Final approval granted")))
            .andExpect(jsonPath("$.approvalDueAt").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.slaEscalated").value(true))
            .andExpect(jsonPath("$.slaEscalatedTo").value("Executive Operations Director"))
            .andExpect(jsonPath("$.overdue").value(false))
            .andExpect(jsonPath("$.executionReady").value(true));

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_ESCALATION_ADVANCED, BusinessEventType.SCENARIO_SLA_ESCALATED, BusinessEventType.SCENARIO_SLA_ACKNOWLEDGED, BusinessEventType.SCENARIO_APPROVED);
    }

    @Test
    void warehouseScopedApproversOnlySeeAndApproveAssignedWarehouseLanes() throws Exception {
        String northPlan = """
            {
              "title": "North scoped final approval",
              "requestedBy": "Ayo Planner",
              "reviewOwner": "Naledi Lead",
              "request": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 18,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        String coastPlan = """
            {
              "title": "Coast scoped final approval",
              "requestedBy": "Ayo Planner",
              "reviewOwner": "Naledi Lead",
              "request": {
                "warehouseCode": "WH-COAST",
                "items": [
                  {
                    "productSku": "SKU-ORB-440",
                    "quantity": 14,
                    "unitPrice": 160.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(northPlan))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.finalApprovalOwner").value("North Operations Director"));

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(coastPlan))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.finalApprovalOwner").value("Coast Operations Director"));

        Long northScenarioId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> "North scoped final approval".equals(run.getTitle()))
            .findFirst()
            .orElseThrow()
            .getId();
        Long coastScenarioId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> "Coast scoped final approval".equals(run.getTitle()))
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/scenarios/" + northScenarioId + "/approve")
                .with(accessHeaders("Naledi Lead", "REVIEW_OWNER"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "approverName": "Naledi Lead",
                      "approvalNote": "North owner review complete."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStage").value("PENDING_FINAL_APPROVAL"));

        mockMvc.perform(post("/api/scenarios/" + coastScenarioId + "/approve")
                .with(accessHeaders("Naledi Lead", "REVIEW_OWNER"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "approverName": "Naledi Lead",
                      "approvalNote": "Coast owner review complete."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStage").value("PENDING_FINAL_APPROVAL"));

        mockMvc.perform(get("/api/warehouses")
                .with(accessHeaders("North Operations Director", "FINAL_APPROVER"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value("WH-NORTH"));

        mockMvc.perform(get("/api/scenarios/history")
                .with(accessHeaders("North Operations Director", "FINAL_APPROVER"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .param("approvalStage", "PENDING_FINAL_APPROVAL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("North scoped final approval"))
            .andExpect(jsonPath("$[0].warehouseCode").value("WH-NORTH"));

        mockMvc.perform(post("/api/scenarios/" + coastScenarioId + "/approve")
                .with(accessHeaders("North Operations Director", "FINAL_APPROVER"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "FINAL_APPROVER",
                      "approverName": "North Operations Director",
                      "approvalNote": "Attempting to approve outside the assigned warehouse."
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("warehouse WH-COAST")));

        mockMvc.perform(post("/api/scenarios/" + coastScenarioId + "/approve")
                .with(accessHeaders("Coast Operations Director", "FINAL_APPROVER"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "FINAL_APPROVER",
                      "approverName": "Coast Operations Director",
                      "approvalNote": "Coast final approval granted inside the assigned lane."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("Coast Operations Director"));
    }

    @Test
    void comparisonScenarioCannotBeExecutedIntoLiveFlow() throws Exception {
        String comparisonBody = """
            {
              "primaryLabel": "Balanced",
              "primary": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 2,
                    "unitPrice": 95.00
                  }
                ]
              },
              "alternativeLabel": "Aggressive",
              "alternative": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 9,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/order-impact/compare")
                .contentType(APPLICATION_JSON)
                .content(comparisonBody))
            .andExpect(status().isOk());

        Long comparisonScenarioId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.COMPARISON)
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/scenarios/" + comparisonScenarioId + "/execute"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot be executed")));
    }

    @Test
    void comparisonScenarioCannotBeLoadedBackAsExecutableRequest() throws Exception {
        String comparisonBody = """
            {
              "primaryLabel": "Balanced",
              "primary": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 2,
                    "unitPrice": 95.00
                  }
                ]
              },
              "alternativeLabel": "Aggressive",
              "alternative": {
                "warehouseCode": "WH-NORTH",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 9,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """;

        mockMvc.perform(post("/api/scenarios/order-impact/compare")
                .contentType(APPLICATION_JSON)
                .content(comparisonBody))
            .andExpect(status().isOk());

        Long comparisonScenarioId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.COMPARISON)
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(get("/api/scenarios/" + comparisonScenarioId + "/request"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot be loaded")));
    }

    @Test
    void dashboardSummaryReflectsSimulationStateChanges() throws Exception {
        mockMvc.perform(post("/api/simulation/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulationRunning").value(true));

        mockMvc.perform(post("/api/simulation/stop"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulationRunning").value(false));
    }

    @Test
    void duplicateExternalOrderIdIsRejectedWithoutFurtherInventoryImpact() throws Exception {
        Inventory inventory = loadInventory("SKU-VDR-210", "WH-NORTH");
        long startingQuantity = inventory.getQuantityAvailable();

        String requestBody = """
            {
              "externalOrderId": "ORD-DUP-001",
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-VDR-210",
                  "quantity": 2,
                  "unitPrice": 140.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Order already exists for externalOrderId ORD-DUP-001"));

        Inventory updatedInventory = loadInventory("SKU-VDR-210", "WH-NORTH");
        assertThat(updatedInventory.getQuantityAvailable()).isEqualTo(startingQuantity - 2);
    }

    @Test
    void orderIngestionFailsClearlyWhenInventoryRecordIsMissing() throws Exception {
        Product transientProduct = productRepository.save(Product.builder()
            .sku("SKU-TST-999")
            .name("Test Product")
            .category("Diagnostics")
            .build());

        String requestBody = """
            {
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "%s",
                  "quantity": 1,
                  "unitPrice": 20.00
                }
              ]
            }
            """.formatted(transientProduct.getSku());

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("No inventory found for SKU SKU-TST-999 in warehouse WH-NORTH"));
    }

    @Test
    void simulationStartAndStopAreIdempotentAndVisible() throws Exception {
        mockMvc.perform(post("/api/simulation/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/simulation/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/simulation/stop"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/events/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventType").exists());

        long simulationStartedEvents = businessEventRepository.findTop20ByOrderByCreatedAtDesc().stream()
            .filter(event -> event.getEventType() == BusinessEventType.SIMULATION_STARTED)
            .count();
        assertThat(simulationStartedEvents).isEqualTo(1);
    }

    @Test
    void simulationTickUsesRealOrderFlowAndChangesOperationalState() throws Exception {
        long ordersBefore = customerOrderRepository.count();
        long totalInventoryBefore = inventoryRepository.findAll().stream()
            .mapToLong(Inventory::getQuantityAvailable)
            .sum();

        mockMvc.perform(post("/api/simulation/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));

        simulationService.generateSimulatedOrder();

        long ordersAfter = customerOrderRepository.count();
        long totalInventoryAfter = inventoryRepository.findAll().stream()
            .mapToLong(Inventory::getQuantityAvailable)
            .sum();

        assertThat(ordersAfter).isEqualTo(ordersBefore + 1);
        assertThat(totalInventoryAfter).isLessThan(totalInventoryBefore);

        mockMvc.perform(get("/api/orders/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].externalOrderId").value(org.hamcrest.Matchers.startsWith("SIM-")));

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(event -> event.getEventType() == BusinessEventType.ORDER_INGESTED && "simulation".equals(event.getSource()));

        mockMvc.perform(post("/api/simulation/stop"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void predictionLayerSurfacesStockoutWindowAcrossOperationalViews() throws Exception {
        Product transientProduct = productRepository.save(Product.builder()
            .sku("SKU-PRED-550")
            .name("Prediction Test Module")
            .category("Forecast")
            .build());
        var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
        inventoryRepository.save(Inventory.builder()
            .product(transientProduct)
            .warehouse(warehouse)
            .quantityAvailable(18L)
            .reorderThreshold(12L)
            .build());

        String demandOrder = """
            {
              "externalOrderId": "ORD-PRED-001",
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-PRED-550",
                  "quantity": 6,
                  "unitPrice": 140.00
                }
              ]
            }
            """;

        String lowStockUpdate = """
            {
              "productSku": "SKU-PRED-550",
              "warehouseCode": "WH-NORTH",
              "quantityAvailable": 6,
              "reorderThreshold": 12
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(demandOrder))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(lowStockUpdate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lowStock").value(true));

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeAlerts[0].title").value(org.hamcrest.Matchers.containsString("SKU-PRED-550")))
            .andExpect(jsonPath("$.activeAlerts[0].impactSummary").value(org.hamcrest.Matchers.containsString("within 1.0 hours")));

        mockMvc.perform(get("/api/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value(org.hamcrest.Matchers.containsString("SKU-PRED-550")))
            .andExpect(jsonPath("$[0].description").value(org.hamcrest.Matchers.containsString("Estimated stockout window is 1.0 hours")));

        mockMvc.perform(get("/api/inventory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.productSku == 'SKU-PRED-550' && @.warehouseCode == 'WH-NORTH')].rapidConsumption")
                .value(org.hamcrest.Matchers.hasItem(true)))
            .andExpect(jsonPath("$[?(@.productSku == 'SKU-PRED-550' && @.warehouseCode == 'WH-NORTH')].unitsPerHour")
                .value(org.hamcrest.Matchers.hasItem(6.0)))
            .andExpect(jsonPath("$[?(@.productSku == 'SKU-PRED-550' && @.warehouseCode == 'WH-NORTH')].hoursToStockout")
                .value(org.hamcrest.Matchers.hasItem(1.0)));
    }

    @Test
    void externalOrderWebhookFeedsTheRealOperationalOrderFlow() throws Exception {
        Inventory inventoryBefore = loadInventory("SKU-FLX-100", "WH-NORTH");
        long startingQuantity = inventoryBefore.getQuantityAvailable();

        String requestBody = """
            {
              "sourceSystem": "erp_north",
              "externalOrderId": "ERP-EXT-1001",
              "warehouseCode": "",
              "customerReference": "CUST-778",
              "occurredAt": "2026-04-01T09:30:00Z",
              "items": [
                {
                  "productSku": "sku-flx-100",
                  "quantity": 3,
                  "unitPrice": 95.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/integrations/orders/webhook")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sourceSystem").value("erp_north"))
            .andExpect(jsonPath("$.ingestionSource").value("integration-webhook:erp_north"))
            .andExpect(jsonPath("$.acceptedAt").exists())
            .andExpect(jsonPath("$.order.externalOrderId").value("ERP-EXT-1001"))
            .andExpect(jsonPath("$.order.warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.order.items[0].productSku").value("SKU-FLX-100"));

        Inventory inventoryAfter = loadInventory("SKU-FLX-100", "WH-NORTH");
        assertThat(inventoryAfter.getQuantityAvailable()).isEqualTo(startingQuantity - 3);

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(event -> event.getEventType() == BusinessEventType.ORDER_INGESTED
                && "integration-webhook:erp_north".equals(event.getSource()));
    }

    @Test
    void integrationConnectorsCanBeListedAndDisabledForWebhookIngress() throws Exception {
        mockMvc.perform(get("/api/access/tenants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("SYNAPSE-DEMO"))
            .andExpect(jsonPath("$[0].name").value("Synapse Demo Company"));

        mockMvc.perform(get("/api/access/operators"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.actorName == 'Integration Lead')].tenantCode")
                .value(org.hamcrest.Matchers.hasItem("SYNAPSE-DEMO")))
            .andExpect(jsonPath("$[?(@.actorName == 'Integration Lead')].tenantName")
                .value(org.hamcrest.Matchers.hasItem("Synapse Demo Company")))
            .andExpect(jsonPath("$[?(@.actorName == 'Integration Lead')].displayName")
                .value(org.hamcrest.Matchers.hasItem("Integration Lead")))
            .andExpect(jsonPath("$[?(@.actorName == 'Naledi Lead')].displayName")
                .value(org.hamcrest.Matchers.hasItem("Naledi Lead")));

        mockMvc.perform(get("/api/integrations/orders/connectors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.sourceSystem == 'erp_north' && @.type == 'WEBHOOK_ORDER')].displayName")
                .value(org.hamcrest.Matchers.hasItem("ERP North Webhook")))
            .andExpect(jsonPath("$[?(@.sourceSystem == 'erp_north' && @.type == 'WEBHOOK_ORDER')].syncMode")
                .value(org.hamcrest.Matchers.hasItem("REALTIME_PUSH")))
            .andExpect(jsonPath("$[?(@.sourceSystem == 'erp_north' && @.type == 'WEBHOOK_ORDER')].validationPolicy")
                .value(org.hamcrest.Matchers.hasItem("STANDARD")))
            .andExpect(jsonPath("$[?(@.sourceSystem == 'erp_north' && @.type == 'WEBHOOK_ORDER')].transformationPolicy")
                .value(org.hamcrest.Matchers.hasItem("NORMALIZE_CODES")))
            .andExpect(jsonPath("$[?(@.sourceSystem == 'erp_batch' && @.type == 'CSV_ORDER_IMPORT')].enabled")
                .value(org.hamcrest.Matchers.hasItem(true)));

        mockMvc.perform(post("/api/integrations/orders/connectors")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem": "erp_north",
                      "type": "WEBHOOK_ORDER",
                      "displayName": "ERP North Webhook",
                      "enabled": false,
                      "defaultWarehouseCode": "WH-NORTH",
                      "notes": "Temporarily paused for maintenance."
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("X-Synapse-Actor is required")));

        mockMvc.perform(post("/api/integrations/orders/connectors")
                .with(accessHeaders("Unknown Operator", "INTEGRATION_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem": "erp_north",
                      "type": "WEBHOOK_ORDER",
                      "displayName": "ERP North Webhook",
                      "enabled": false,
                      "defaultWarehouseCode": "WH-NORTH",
                      "notes": "Temporarily paused for maintenance."
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not a known active operator")));

        mockMvc.perform(post("/api/integrations/orders/connectors")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem": "erp_north",
                      "type": "WEBHOOK_ORDER",
                      "displayName": "ERP North Webhook",
                      "enabled": false,
                      "defaultWarehouseCode": "WH-NORTH",
                      "notes": "Temporarily paused for maintenance."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceSystem").value("erp_north"))
            .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/dashboard/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integrationConnectors[?(@.sourceSystem == 'erp_north' && @.type == 'WEBHOOK_ORDER')].enabled")
                .value(org.hamcrest.Matchers.hasItem(false)));

        String requestBody = """
            {
              "sourceSystem": "erp_north",
              "externalOrderId": "ERP-EXT-2001",
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 1,
                  "unitPrice": 95.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/integrations/orders/webhook")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("disabled")));
    }

    @Test
    void operatorSessionCanAuthenticateAndUseProtectedConnectorActions() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(false));

        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "integration.lead",
                      "password": "wrong-code"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid operator credentials."));

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "integration.lead",
                      "password": "integration-admin-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.tenantCode").value("SYNAPSE-DEMO"))
            .andExpect(jsonPath("$.tenantName").value("Synapse Demo Company"))
            .andExpect(jsonPath("$.username").value("integration.lead"))
            .andExpect(jsonPath("$.actorName").value("Integration Lead"))
            .andExpect(jsonPath("$.sessionTimeoutMinutes").value(480))
            .andExpect(jsonPath("$.passwordRotationDays").value(90))
            .andExpect(jsonPath("$.passwordChangeRequired").value(false))
            .andExpect(jsonPath("$.roles").isArray())
            .andReturn()
            .getRequest()
            .getSession(false);

        mockMvc.perform(get("/api/auth/session").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.tenantCode").value("SYNAPSE-DEMO"))
            .andExpect(jsonPath("$.tenantName").value("Synapse Demo Company"))
            .andExpect(jsonPath("$.username").value("integration.lead"))
            .andExpect(jsonPath("$.actorName").value("Integration Lead"));

        mockMvc.perform(post("/api/integrations/orders/connectors")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem": "erp_north",
                      "type": "WEBHOOK_ORDER",
                      "displayName": "ERP North Webhook",
                      "enabled": false,
                      "defaultWarehouseCode": "WH-NORTH",
                      "notes": "Temporarily paused through signed-in operator session."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/auth/session/logout").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(false));
    }

    @Test
    void tenantAdminCanOnboardTenantWorkspaceAndUseBootstrapSession() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "ACME-OPS",
                      "tenantName": "Acme Operations",
                      "description": "Starter workspace for Acme's operations team.",
                      "adminFullName": "Amina Dlamini",
                      "adminUsername": "amina.admin",
                      "adminPassword": "launchpad-2026",
                      "primaryLocation": "Johannesburg",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("ACME-OPS"))
            .andExpect(jsonPath("$.tenantName").value("Acme Operations"))
            .andExpect(jsonPath("$.adminUsername").value("amina.admin"))
            .andExpect(jsonPath("$.adminActorName").value("Operations Lead"))
            .andExpect(jsonPath("$.executiveUsername").value("acme.ops.executive"))
            .andExpect(jsonPath("$.executiveActorName").value("Executive Operations Director"))
            .andExpect(jsonPath("$.executivePassword").exists())
            .andExpect(jsonPath("$.starterWarehouseCodes")
                .value(org.hamcrest.Matchers.hasItems("WH-NORTH", "WH-COAST")))
            .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(get("/api/access/tenants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.code == 'ACME-OPS')].name")
                .value(org.hamcrest.Matchers.hasItem("Acme Operations")));

        mockMvc.perform(get("/api/access/operators")
                .param("tenantCode", "ACME-OPS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.actorName == 'Operations Lead')].tenantCode")
                .value(org.hamcrest.Matchers.hasItem("ACME-OPS")))
            .andExpect(jsonPath("$[?(@.actorName == 'Operations Lead')].roles[*]")
                .value(org.hamcrest.Matchers.hasItem("TENANT_ADMIN")))
            .andExpect(jsonPath("$[?(@.actorName == 'Executive Operations Director')].roles[*]")
                .value(org.hamcrest.Matchers.hasItem("FINAL_APPROVER")))
            .andExpect(jsonPath("$[?(@.actorName == 'Operations Planner')].actorName")
                .value(org.hamcrest.Matchers.hasItem("Operations Planner")));

        mockMvc.perform(get("/api/warehouses")
                .header("X-Synapse-Tenant", "ACME-OPS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code")
                .value(org.hamcrest.Matchers.hasItems("WH-NORTH", "WH-COAST")))
            .andExpect(jsonPath("$[?(@.code == 'WH-NORTH')].location")
                .value(org.hamcrest.Matchers.hasItem("Johannesburg")))
            .andExpect(jsonPath("$[?(@.code == 'WH-COAST')].location")
                .value(org.hamcrest.Matchers.hasItem("Cape Town")));

        mockMvc.perform(get("/api/integrations/orders/connectors")
                .header("X-Synapse-Tenant", "ACME-OPS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.type == 'WEBHOOK_ORDER')].sourceSystem")
                .value(org.hamcrest.Matchers.hasItems("acme_ops_north", "acme_ops_coast")))
            .andExpect(jsonPath("$[?(@.type == 'CSV_ORDER_IMPORT')].sourceSystem")
                .value(org.hamcrest.Matchers.hasItem("acme_ops_batch")));

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "ACME-OPS",
                      "username": "amina.admin",
                      "password": "launchpad-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.tenantCode").value("ACME-OPS"))
            .andExpect(jsonPath("$.tenantName").value("Acme Operations"))
            .andExpect(jsonPath("$.username").value("amina.admin"))
            .andExpect(jsonPath("$.actorName").value("Operations Lead"))
            .andExpect(jsonPath("$.passwordChangeRequired").value(false))
            .andExpect(jsonPath("$.roles[*]")
                .value(org.hamcrest.Matchers.hasItem("TENANT_ADMIN")))
            .andReturn()
            .getRequest()
            .getSession(false);

        mockMvc.perform(get("/api/auth/session").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.tenantCode").value("ACME-OPS"))
            .andExpect(jsonPath("$.username").value("amina.admin"))
            .andExpect(jsonPath("$.actorName").value("Operations Lead"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void onboardedTenantAdminCanOpenWorkspaceThroughSignedInSession() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "ACME-WORKSPACE",
                      "tenantName": "Acme Workspace",
                      "description": "Workspace payload verification.",
                      "adminFullName": "Amina Workspace",
                      "adminUsername": "amina.workspace",
                      "adminPassword": "launchpad-2026",
                      "primaryLocation": "Johannesburg",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("ACME-WORKSPACE"));

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "ACME-WORKSPACE",
                      "username": "amina.workspace",
                      "password": "launchpad-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.tenantCode").value("ACME-WORKSPACE"))
            .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItem("TENANT_ADMIN")))
            .andReturn()
            .getRequest()
            .getSession(false);

        mockMvc.perform(get("/api/access/admin/workspace").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("ACME-WORKSPACE"))
            .andExpect(jsonPath("$.tenantName").value("Acme Workspace"))
            .andExpect(jsonPath("$.supportSummary.warehouseCount").value(2))
            .andExpect(jsonPath("$.supportSummary.enabledConnectorCount")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.supportIncidents").isArray())
            .andExpect(jsonPath("$.recentSupportActivity").isArray())
            .andExpect(jsonPath("$.warehouses[?(@.code == 'WH-NORTH')].name")
                .value(org.hamcrest.Matchers.hasItem("Acme Workspace North Hub")))
            .andExpect(jsonPath("$.connectors[?(@.sourceSystem == 'acme_workspace_north')].supportOwnerActorName")
                .value(org.hamcrest.Matchers.hasItem("Operations Lead")));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void signedInScenarioSaveWorksOutsideTestTransaction() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "operations.lead",
                      "password": "lead-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.tenantCode").value("SYNAPSE-DEMO"))
            .andReturn()
            .getRequest()
            .getSession(false);

        mockMvc.perform(post("/api/scenarios/save")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "title": "Signed-in live save verification",
                      "request": {
                        "warehouseCode": "WH-NORTH",
                        "items": [
                          {
                            "productSku": "SKU-FLX-100",
                            "quantity": 12,
                            "unitPrice": 95.00
                          }
                        ]
                      }
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.warehouseCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.reviewOwner").isNotEmpty())
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"));
    }

    @Test
    void seedBackfillRemainsSafeAfterAdditionalTenantsExist() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "MULTI-OPS",
                      "tenantName": "Multi Ops Company",
                      "description": "Additional tenant to verify startup backfill safety.",
                      "adminFullName": "Multi Ops Admin",
                      "adminUsername": "multi.workspace",
                      "adminPassword": "ready-verify-2026",
                      "primaryLocation": "Johannesburg",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("MULTI-OPS"));

        assertThat(seedService.seedIfEmpty()).isFalse();
    }

    @Test
    void tenantAdminCanCreateScopedOperatorsAndUsersForWarehouseLanes() throws Exception {
        mockMvc.perform(post("/api/access/admin/operators")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorName": "North Review Manager",
                      "displayName": "North Review Manager",
                      "description": "Warehouse-scoped review owner for north operations.",
                      "active": true,
                      "roles": ["REVIEW_OWNER"],
                      "warehouseScopes": ["WH-NORTH"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actorName").value("North Review Manager"))
            .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItem("REVIEW_OWNER")))
            .andExpect(jsonPath("$.warehouseScopes[*]").value(org.hamcrest.Matchers.hasItem("WH-NORTH")));

        mockMvc.perform(get("/api/access/admin/operators")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.actorName == 'North Review Manager')].warehouseScopes[*]")
                .value(org.hamcrest.Matchers.hasItem("WH-NORTH")));

        mockMvc.perform(post("/api/access/admin/users")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "username": "north.review.manager",
                      "fullName": "North Review Manager",
                      "password": "north-lane-2026",
                      "operatorActorName": "North Review Manager"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("north.review.manager"))
            .andExpect(jsonPath("$.operatorActorName").value("North Review Manager"))
            .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItem("REVIEW_OWNER")))
            .andExpect(jsonPath("$.passwordChangeRequired").value(true))
            .andExpect(jsonPath("$.warehouseScopes[*]").value(org.hamcrest.Matchers.hasItem("WH-NORTH")));

        mockMvc.perform(get("/api/access/admin/users")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.username == 'north.review.manager')].operatorActorName")
                .value(org.hamcrest.Matchers.hasItem("North Review Manager")));

        mockMvc.perform(post("/api/scenarios/save")
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "title": "North tenant-admin lane test",
                      "requestedBy": "Ayo Planner",
                      "reviewOwner": "North Review Manager",
                      "request": {
                        "warehouseCode": "WH-NORTH",
                        "items": [
                          {
                            "productSku": "SKU-FLX-100",
                            "quantity": 18,
                            "unitPrice": 95.00
                          }
                        ]
                      }
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reviewOwner").value("North Review Manager"));

        mockMvc.perform(post("/api/scenarios/save")
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "title": "Coast tenant-admin lane test",
                      "requestedBy": "Ayo Planner",
                      "reviewOwner": "North Review Manager",
                      "request": {
                        "warehouseCode": "WH-COAST",
                        "items": [
                          {
                            "productSku": "SKU-ORB-440",
                            "quantity": 14,
                            "unitPrice": 160.00
                          }
                        ]
                      }
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("warehouse WH-COAST")));

        Long scenarioId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> "North tenant-admin lane test".equals(run.getTitle()))
            .findFirst()
            .orElseThrow()
            .getId();

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "north.review.manager",
                      "password": "north-lane-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.actorName").value("North Review Manager"))
            .andExpect(jsonPath("$.warehouseScopes[*]").value(org.hamcrest.Matchers.hasItem("WH-NORTH")))
            .andReturn()
            .getRequest()
            .getSession(false);

        mockMvc.perform(get("/api/warehouses").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value("WH-NORTH"));

        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/approve")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "approverName": "North Review Manager",
                      "approvalNote": "North lane owner review complete."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewApprovedBy").value("North Review Manager"));
    }

    @Test
    void tenantAdminCanManageUserLifecyclePasswordResetsAndOperatorActivation() throws Exception {
        mockMvc.perform(post("/api/access/admin/operators")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorName": "North Review Manager",
                      "displayName": "North Review Manager",
                      "description": "North review lane.",
                      "active": true,
                      "roles": ["REVIEW_OWNER"],
                      "warehouseScopes": ["WH-NORTH"]
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/access/admin/operators")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorName": "North Final Manager",
                      "displayName": "North Final Manager",
                      "description": "North final approval lane.",
                      "active": true,
                      "roles": ["FINAL_APPROVER"],
                      "warehouseScopes": ["WH-NORTH"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItem("FINAL_APPROVER")));

        mockMvc.perform(post("/api/access/admin/users")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "username": "north.lifecycle.manager",
                      "fullName": "North Lifecycle Manager",
                      "password": "north-lifecycle-2026",
                      "operatorActorName": "North Review Manager"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operatorActorName").value("North Review Manager"));

        Long lifecycleUserId = accessUserRepository
            .findByTenant_CodeIgnoreCaseAndUsernameIgnoreCase("SYNAPSE-DEMO", "north.lifecycle.manager")
            .orElseThrow()
            .getId();
        Long finalManagerOperatorId = accessOperatorRepository
            .findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase("SYNAPSE-DEMO", "North Final Manager")
            .orElseThrow()
            .getId();

        mockMvc.perform(put("/api/access/admin/users/" + lifecycleUserId)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "North Lifecycle Manager Updated",
                      "active": true,
                      "operatorActorName": "North Final Manager"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("North Lifecycle Manager Updated"))
            .andExpect(jsonPath("$.operatorActorName").value("North Final Manager"))
            .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItem("FINAL_APPROVER")));

        mockMvc.perform(post("/api/access/admin/users/" + lifecycleUserId + "/reset-password")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "password": "north-reset-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("north.lifecycle.manager"));

        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "north.lifecycle.manager",
                      "password": "north-lifecycle-2026"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid operator credentials."));

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "north.lifecycle.manager",
                      "password": "north-reset-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.actorName").value("North Final Manager"))
            .andExpect(jsonPath("$.passwordChangeRequired").value(true))
            .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItem("FINAL_APPROVER")))
            .andReturn()
            .getRequest()
            .getSession(false);

        mockMvc.perform(put("/api/access/admin/operators/" + finalManagerOperatorId)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorName": "North Final Manager",
                      "displayName": "North Final Manager",
                      "description": "North final approval lane.",
                      "active": false,
                      "roles": ["FINAL_APPROVER"],
                      "warehouseScopes": ["WH-NORTH"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/auth/session").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(false));

        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "north.lifecycle.manager",
                      "password": "north-reset-2026"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Signed-in operator is no longer active."));

        mockMvc.perform(put("/api/access/admin/operators/" + finalManagerOperatorId)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorName": "North Final Manager",
                      "displayName": "North Final Manager",
                      "description": "North final approval lane restored.",
                      "active": true,
                      "roles": ["FINAL_APPROVER"],
                      "warehouseScopes": ["WH-NORTH"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(put("/api/access/admin/users/" + lifecycleUserId)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "North Lifecycle Manager Updated",
                      "active": false,
                      "operatorActorName": "North Final Manager"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "north.lifecycle.manager",
                      "password": "north-reset-2026"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid operator credentials."));

        mockMvc.perform(put("/api/access/admin/users/" + lifecycleUserId)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "North Lifecycle Manager Updated",
                      "active": true,
                      "operatorActorName": "North Final Manager"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "north.lifecycle.manager",
                      "password": "north-reset-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.actorName").value("North Final Manager"));

        assertThat(auditLogRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(log -> "ACCESS_USER_UPDATED".equals(log.getAction()));
        assertThat(auditLogRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(log -> "ACCESS_USER_PASSWORD_RESET".equals(log.getAction()));
        assertThat(auditLogRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(log -> "ACCESS_OPERATOR_UPDATED".equals(log.getAction()));
    }

    @Test
    void tenantAdminCanManageWorkspaceSettingsWarehousesAndConnectorSupport() throws Exception {
        mockMvc.perform(get("/api/access/admin/workspace")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("SYNAPSE-DEMO"))
            .andExpect(jsonPath("$.tenantName").value("Synapse Demo Company"))
            .andExpect(jsonPath("$.securitySettings.passwordRotationDays").value(90))
            .andExpect(jsonPath("$.securitySettings.sessionTimeoutMinutes").value(480))
            .andExpect(jsonPath("$.securitySettings.securityPolicyVersion").value(1))
            .andExpect(jsonPath("$.supportSummary.warehouseCount").value(2))
            .andExpect(jsonPath("$.supportSummary.enabledConnectorCount")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.supportDiagnostics.activeUsersRequiringPasswordChange").value(0))
            .andExpect(jsonPath("$.supportDiagnostics.activeUsersPastPasswordRotation").value(0))
            .andExpect(jsonPath("$.supportDiagnostics.activeUsersBlockedByInactiveOperator").value(0))
            .andExpect(jsonPath("$.supportDiagnostics.connectorsWithoutSupportOwner").value(0))
            .andExpect(jsonPath("$.connectors[?(@.sourceSystem == 'erp_north')].syncMode")
                .value(org.hamcrest.Matchers.hasItem("REALTIME_PUSH")))
            .andExpect(jsonPath("$.connectors[?(@.sourceSystem == 'erp_north')].validationPolicy")
                .value(org.hamcrest.Matchers.hasItem("STANDARD")))
            .andExpect(jsonPath("$.supportIncidents").isArray())
            .andExpect(jsonPath("$.recentSupportActivity").isArray())
            .andExpect(jsonPath("$.warehouses[?(@.code == 'WH-NORTH')].name")
                .value(org.hamcrest.Matchers.hasItem("Warehouse North")))
            .andExpect(jsonPath("$.connectors[?(@.sourceSystem == 'erp_north')].supportOwnerActorName")
                .value(org.hamcrest.Matchers.hasItem("Operations Lead")));

        Long northWarehouseId = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode("SYNAPSE-DEMO", "WH-NORTH")
            .orElseThrow()
            .getId();
        Long northConnectorId = integrationConnectorRepository
            .findByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseAndType(
                "SYNAPSE-DEMO",
                "erp_north",
                com.synapsecore.domain.entity.IntegrationConnectorType.WEBHOOK_ORDER
            )
            .orElseThrow()
            .getId();

        mockMvc.perform(put("/api/access/admin/workspace")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantName": "Synapse Demo Company Updated",
                      "description": "Tenant admin support lane updated through workspace settings."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantName").value("Synapse Demo Company Updated"))
            .andExpect(jsonPath("$.description").value("Tenant admin support lane updated through workspace settings."));

        mockMvc.perform(put("/api/access/admin/workspace/warehouses/" + northWarehouseId)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "name": "Warehouse North Prime",
                      "location": "Johannesburg Prime"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("WH-NORTH"))
            .andExpect(jsonPath("$.name").value("Warehouse North Prime"))
            .andExpect(jsonPath("$.location").value("Johannesburg Prime"));

        mockMvc.perform(put("/api/access/admin/workspace/connectors/" + northConnectorId)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "supportOwnerActorName": "Coast Operations Director",
                      "notes": "This should be blocked by warehouse scope."
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("warehouse WH-NORTH")));

        mockMvc.perform(put("/api/access/admin/workspace/connectors/" + northConnectorId)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "supportOwnerActorName": "North Operations Director",
                      "syncMode": "SCHEDULED_PULL",
                      "syncIntervalMinutes": 60,
                      "validationPolicy": "STRICT",
                      "transformationPolicy": "NORMALIZE_CODES",
                      "allowDefaultWarehouseFallback": true,
                      "notes": "North operations director now owns webhook support."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supportOwnerActorName").value("North Operations Director"))
            .andExpect(jsonPath("$.supportOwnerDisplayName").value("North Operations Director"))
            .andExpect(jsonPath("$.syncMode").value("SCHEDULED_PULL"))
            .andExpect(jsonPath("$.syncIntervalMinutes").value(60))
            .andExpect(jsonPath("$.validationPolicy").value("STRICT"))
            .andExpect(jsonPath("$.transformationPolicy").value("NORMALIZE_CODES"))
            .andExpect(jsonPath("$.allowDefaultWarehouseFallback").value(true))
            .andExpect(jsonPath("$.notes").value("North operations director now owns webhook support."));

        mockMvc.perform(get("/api/access/admin/workspace")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantName").value("Synapse Demo Company Updated"))
            .andExpect(jsonPath("$.description").value("Tenant admin support lane updated through workspace settings."))
            .andExpect(jsonPath("$.supportDiagnostics.latestSupportAuditAt").isNotEmpty())
            .andExpect(jsonPath("$.recentSupportActivity[?(@.action == 'TENANT_WORKSPACE_UPDATED')].actor")
                .value(org.hamcrest.Matchers.hasItem("Operations Lead")))
            .andExpect(jsonPath("$.recentSupportActivity[?(@.action == 'TENANT_WAREHOUSE_UPDATED')].actor")
                .value(org.hamcrest.Matchers.hasItem("Operations Lead")))
            .andExpect(jsonPath("$.recentSupportActivity[?(@.action == 'TENANT_CONNECTOR_SUPPORT_UPDATED')].actor")
                .value(org.hamcrest.Matchers.hasItem("Operations Lead")))
            .andExpect(jsonPath("$.warehouses[?(@.code == 'WH-NORTH')].name")
                .value(org.hamcrest.Matchers.hasItem("Warehouse North Prime")))
            .andExpect(jsonPath("$.warehouses[?(@.code == 'WH-NORTH')].location")
                .value(org.hamcrest.Matchers.hasItem("Johannesburg Prime")))
            .andExpect(jsonPath("$.connectors[?(@.sourceSystem == 'erp_north')].supportOwnerActorName")
                .value(org.hamcrest.Matchers.hasItem("North Operations Director")))
            .andExpect(jsonPath("$.connectors[?(@.sourceSystem == 'erp_north')].syncMode")
                .value(org.hamcrest.Matchers.hasItem("SCHEDULED_PULL")))
            .andExpect(jsonPath("$.connectors[?(@.sourceSystem == 'erp_north')].syncIntervalMinutes")
                .value(org.hamcrest.Matchers.hasItem(60)))
            .andExpect(jsonPath("$.connectors[?(@.sourceSystem == 'erp_north')].notes")
                .value(org.hamcrest.Matchers.hasItem("North operations director now owns webhook support.")));

        assertThat(auditLogRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(log -> "TENANT_WORKSPACE_UPDATED".equals(log.getAction()));
        assertThat(auditLogRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(log -> "TENANT_WAREHOUSE_UPDATED".equals(log.getAction()));
        assertThat(auditLogRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(log -> "TENANT_CONNECTOR_SUPPORT_UPDATED".equals(log.getAction()));
    }

    @Test
    void tenantSecurityPolicySupportsPasswordChangeAndInvalidatesOtherSessions() throws Exception {
        MockHttpSession primarySession = (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "operations.lead",
                      "password": "lead-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.passwordChangeRequired").value(false))
            .andReturn()
            .getRequest()
            .getSession(false);

        MockHttpSession secondarySession = (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "integration.lead",
                      "password": "integration-admin-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andReturn()
            .getRequest()
            .getSession(false);

        primarySession = (MockHttpSession) (mockMvc.perform(post("/api/auth/session/password")
                .session(primarySession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "lead-2026",
                      "newPassword": "lead-2026-rotated"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.username").value("operations.lead"))
            .andExpect(jsonPath("$.passwordChangeRequired").value(false))
            .andExpect(jsonPath("$.passwordRotationRequired").value(false))
            .andReturn()
            .getRequest()
            .getSession(false));

        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "operations.lead",
                      "password": "lead-2026"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid operator credentials."));

        primarySession = (MockHttpSession) (mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "SYNAPSE-DEMO",
                      "username": "operations.lead",
                      "password": "lead-2026-rotated"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andReturn()
            .getRequest()
            .getSession(false));

        mockMvc.perform(put("/api/access/admin/workspace/security")
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .header("X-Synapse-Tenant", "SYNAPSE-DEMO")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "passwordRotationDays": 45,
                      "sessionTimeoutMinutes": 120,
                      "invalidateOtherSessions": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.securitySettings.passwordRotationDays").value(45))
            .andExpect(jsonPath("$.securitySettings.sessionTimeoutMinutes").value(120))
            .andExpect(jsonPath("$.securitySettings.securityPolicyVersion").value(2))
            .andExpect(jsonPath("$.recentSupportActivity[?(@.action == 'AUTH_PASSWORD_CHANGED')].actor")
                .value(org.hamcrest.Matchers.hasItem("Operations Lead")))
            .andExpect(jsonPath("$.recentSupportActivity[?(@.action == 'TENANT_SECURITY_SETTINGS_UPDATED')].actor")
                .value(org.hamcrest.Matchers.hasItem("Operations Lead")));

        mockMvc.perform(get("/api/auth/session").session(secondarySession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(false));

        assertThat(auditLogRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(log -> "AUTH_PASSWORD_CHANGED".equals(log.getAction()));
        assertThat(auditLogRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(log -> "TENANT_SECURITY_SETTINGS_UPDATED".equals(log.getAction()));
    }

    @Test
    void externalOrderWebhookRejectsInvalidConnectorPayload() throws Exception {
        String requestBody = """
            {
              "sourceSystem": "erp north",
              "externalOrderId": "",
              "warehouseCode": "WH-NORTH",
              "items": []
            }
            """;

        mockMvc.perform(post("/api/integrations/orders/webhook")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void externalOrderCsvImportProcessesValidOrdersAndReportsFailures() throws Exception {
        Inventory inventoryBefore = loadInventory("SKU-FLX-100", "WH-NORTH");
        long startingQuantity = inventoryBefore.getQuantityAvailable();

        String csvBody = """
            externalOrderId,warehouseCode,productSku,quantity,unitPrice
            CSV-EXT-1001,WH-NORTH,sku-flx-100,2,95.00
            CSV-EXT-1001,WH-NORTH,SKU-FLX-100,1,95.00
            CSV-EXT-1002,WH-NORTH,SKU-UNKNOWN,1,50.00
            """;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "orders.csv",
            "text/csv",
            csvBody.getBytes()
        );

        mockMvc.perform(multipart("/api/integrations/orders/csv-import")
                .file(file)
                .param("sourceSystem", "erp_batch"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceSystemDefault").value("erp_batch"))
            .andExpect(jsonPath("$.rowsReceived").value(3))
            .andExpect(jsonPath("$.ordersImported").value(1))
            .andExpect(jsonPath("$.ordersFailed").value(1))
            .andExpect(jsonPath("$.importedOrders[0].sourceSystem").value("erp_batch"))
            .andExpect(jsonPath("$.importedOrders[0].ingestionSource").value("integration-csv:erp_batch"))
            .andExpect(jsonPath("$.importedOrders[0].externalOrderId").value("CSV-EXT-1001"))
            .andExpect(jsonPath("$.importedOrders[0].lineItemCount").value(1))
            .andExpect(jsonPath("$.importedOrders[0].order.itemCount").value(3))
            .andExpect(jsonPath("$.failedOrders[0].externalOrderId").value("CSV-EXT-1002"))
            .andExpect(jsonPath("$.failedOrders[0].message").value(org.hamcrest.Matchers.containsString("Product not found")));

        mockMvc.perform(get("/api/integrations/orders/imports/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sourceSystem").value("erp_batch"))
            .andExpect(jsonPath("$[0].connectorType").value("CSV_ORDER_IMPORT"))
            .andExpect(jsonPath("$[0].status").value("PARTIAL_SUCCESS"))
            .andExpect(jsonPath("$[0].recordsReceived").value(3))
            .andExpect(jsonPath("$[0].ordersImported").value(1))
            .andExpect(jsonPath("$[0].ordersFailed").value(1));

        mockMvc.perform(get("/api/dashboard/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integrationImportRuns[0].sourceSystem").value("erp_batch"))
            .andExpect(jsonPath("$.integrationImportRuns[0].connectorType").value("CSV_ORDER_IMPORT"))
            .andExpect(jsonPath("$.integrationImportRuns[0].status").value("PARTIAL_SUCCESS"))
            .andExpect(jsonPath("$.integrationConnectors[?(@.sourceSystem == 'erp_batch' && @.type == 'CSV_ORDER_IMPORT')].displayName")
                .value(org.hamcrest.Matchers.hasItem("ERP Batch CSV Feed")));

        Inventory inventoryAfter = loadInventory("SKU-FLX-100", "WH-NORTH");
        assertThat(inventoryAfter.getQuantityAvailable()).isEqualTo(startingQuantity - 3);

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(event -> event.getEventType() == BusinessEventType.ORDER_INGESTED
                && "integration-csv:erp_batch".equals(event.getSource()));
        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(event -> event.getEventType() == BusinessEventType.INTEGRATION_IMPORT_PROCESSED
                && "integration-csv".equals(event.getSource()));
    }

    @Test
    void failedInboundOrderCanBeQueuedAndReplayedIntoLiveFlow() throws Exception {
        String csvBody = """
            externalOrderId,warehouseCode,productSku,quantity,unitPrice
            CSV-RPL-1001,WH-NORTH,SKU-RPL-778,3,88.00
            """;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "retry-orders.csv",
            "text/csv",
            csvBody.getBytes()
        );

        mockMvc.perform(multipart("/api/integrations/orders/csv-import")
                .file(file)
                .param("sourceSystem", "erp_batch"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordersImported").value(0))
            .andExpect(jsonPath("$.ordersFailed").value(1));

        var replayRecord = integrationReplayRecordRepository.findAll().stream()
            .findFirst()
            .orElseThrow();

        mockMvc.perform(get("/api/integrations/orders/replay-queue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].sourceSystem").value("erp_batch"))
            .andExpect(jsonPath("$[0].connectorType").value("CSV_ORDER_IMPORT"))
            .andExpect(jsonPath("$[0].externalOrderId").value("CSV-RPL-1001"))
            .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(get("/api/dashboard/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integrationReplayQueue.length()").value(1))
            .andExpect(jsonPath("$.integrationReplayQueue[0].externalOrderId").value("CSV-RPL-1001"));

        Product replayProduct = productRepository.save(Product.builder()
            .sku("SKU-RPL-778")
            .name("Replay Recovery Rotor")
            .category("Recovery")
            .build());

        inventoryRepository.save(Inventory.builder()
            .product(replayProduct)
            .warehouse(warehouseRepository.findByCode("WH-NORTH").orElseThrow())
            .quantityAvailable(14L)
            .reorderThreshold(4L)
            .build());

        mockMvc.perform(post("/api/integrations/orders/replay/" + replayRecord.getId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("X-Synapse-Actor is required")));

        mockMvc.perform(post("/api/integrations/orders/replay/" + replayRecord.getId())
                .with(accessHeaders("Integration Operator", "INTEGRATION_OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.replay.status").value("REPLAYED"))
            .andExpect(jsonPath("$.replay.replayAttemptCount").value(1))
            .andExpect(jsonPath("$.order.externalOrderId").value("CSV-RPL-1001"))
            .andExpect(jsonPath("$.order.warehouseCode").value("WH-NORTH"));

        mockMvc.perform(get("/api/integrations/orders/replay-queue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/dashboard/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integrationReplayQueue.length()").value(0));

        Inventory recoveredInventory = loadInventory("SKU-RPL-778", "WH-NORTH");
        assertThat(recoveredInventory.getQuantityAvailable()).isEqualTo(11L);

        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(event -> event.getEventType() == BusinessEventType.INTEGRATION_REPLAY_QUEUED);
        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(event -> event.getEventType() == BusinessEventType.INTEGRATION_REPLAY_COMPLETED);
    }

    @Test
    void depletionRiskIsDetectedBeforeThresholdBreachWhenDemandSpikes() throws Exception {
        Product transientProduct = productRepository.save(Product.builder()
            .sku("SKU-DSP-610")
            .name("Demand Spike Rotor")
            .category("Dynamics")
            .build());
        var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
        inventoryRepository.save(Inventory.builder()
            .product(transientProduct)
            .warehouse(warehouse)
            .quantityAvailable(100L)
            .reorderThreshold(20L)
            .build());

        String orderBody = """
            {
              "externalOrderId": "ORD-SPIKE-001",
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-DSP-610",
                  "quantity": 15,
                  "unitPrice": 88.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(orderBody))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeAlerts[0].type").value("DEPLETION_RISK"))
            .andExpect(jsonPath("$.activeAlerts[0].title").value(org.hamcrest.Matchers.containsString("SKU-DSP-610")))
            .andExpect(jsonPath("$.activeAlerts[0].impactSummary").value(org.hamcrest.Matchers.containsString("within 5.7 hours")));

        mockMvc.perform(get("/api/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("REORDER_STOCK"))
            .andExpect(jsonPath("$[0].title").value(org.hamcrest.Matchers.containsString("Prepare replenishment for SKU SKU-DSP-610")))
            .andExpect(jsonPath("$[0].description").value(org.hamcrest.Matchers.containsString("Review threshold settings and stage replenishment now")));

        mockMvc.perform(get("/api/inventory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.productSku == 'SKU-DSP-610' && @.warehouseCode == 'WH-NORTH')].lowStock")
                .value(org.hamcrest.Matchers.hasItem(false)))
            .andExpect(jsonPath("$[?(@.productSku == 'SKU-DSP-610' && @.warehouseCode == 'WH-NORTH')].rapidConsumption")
                .value(org.hamcrest.Matchers.hasItem(true)))
            .andExpect(jsonPath("$[?(@.productSku == 'SKU-DSP-610' && @.warehouseCode == 'WH-NORTH')].riskLevel")
                .value(org.hamcrest.Matchers.hasItem("high")));
    }

    @Test
    void depletionRiskAlertResolvesWhenInventoryBufferRecovers() throws Exception {
        Product transientProduct = productRepository.save(Product.builder()
            .sku("SKU-DSP-611")
            .name("Demand Spike Buffer")
            .category("Dynamics")
            .build());
        var warehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
        inventoryRepository.save(Inventory.builder()
            .product(transientProduct)
            .warehouse(warehouse)
            .quantityAvailable(100L)
            .reorderThreshold(20L)
            .build());

        String orderBody = """
            {
              "externalOrderId": "ORD-SPIKE-002",
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-DSP-611",
                  "quantity": 15,
                  "unitPrice": 88.00
                }
              ]
            }
            """;

        String recoveryBody = """
            {
              "productSku": "SKU-DSP-611",
              "warehouseCode": "WH-NORTH",
              "quantityAvailable": 180,
              "reorderThreshold": 20
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(orderBody))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(recoveryBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lowStock").value(false));

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeAlerts[*].title", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(
                org.hamcrest.Matchers.containsString("SKU-DSP-611")
            ))))
            .andExpect(jsonPath("$.recentAlerts[0].title").value(org.hamcrest.Matchers.containsString("SKU-DSP-611")))
            .andExpect(jsonPath("$.recentAlerts[0].type").value("DEPLETION_RISK"))
            .andExpect(jsonPath("$.recentAlerts[0].status").value("RESOLVED"));
    }

    @Test
    void transferRecommendationIsGeneratedWhenAnotherWarehouseCanCoverShortfall() throws Exception {
        Product transientProduct = productRepository.save(Product.builder()
            .sku("SKU-XFR-710")
            .name("Transfer Signal Module")
            .category("Coordination")
            .build());
        var northWarehouse = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
        var coastWarehouse = warehouseRepository.findByCode("WH-COAST").orElseThrow();

        inventoryRepository.save(Inventory.builder()
            .product(transientProduct)
            .warehouse(northWarehouse)
            .quantityAvailable(34L)
            .reorderThreshold(20L)
            .build());
        inventoryRepository.save(Inventory.builder()
            .product(transientProduct)
            .warehouse(coastWarehouse)
            .quantityAvailable(10L)
            .reorderThreshold(16L)
            .build());

        String requestBody = """
            {
              "productSku": "SKU-XFR-710",
              "warehouseCode": "WH-COAST",
              "quantityAvailable": 10,
              "reorderThreshold": 16
            }
            """;

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lowStock").value(true));

        mockMvc.perform(get("/api/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("TRANSFER_STOCK"))
            .andExpect(jsonPath("$[0].title").value("Transfer stock for SKU SKU-XFR-710 from WH-NORTH to WH-COAST"))
            .andExpect(jsonPath("$[0].description").value(org.hamcrest.Matchers.containsString("Transfer 6 units from Warehouse North to Warehouse Coast")));
    }

    @Test
    void successfulRequestsReturnTraceIdAndWriteAuditLogs() throws Exception {
        String requestBody = """
            {
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 2,
                  "unitPrice": 95.00
                }
              ]
            }
            """;

        String requestId = mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("X-Request-Id"))
            .andReturn()
            .getResponse()
            .getHeader("X-Request-Id");

        assertThat(requestId).isNotBlank();
        assertThat(auditLogRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(log -> "ORDER_PROCESSED".equals(log.getAction())
                && log.getStatus() == AuditStatus.SUCCESS
                && requestId.equals(log.getRequestId()));

        mockMvc.perform(get("/api/audit/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].action").exists())
            .andExpect(jsonPath("$[0].requestId").exists());
    }

    @Test
    void rejectedRequestsReturnTraceIdAndWriteFailureAuditLogs() throws Exception {
        String requestBody = """
            {
              "productSku": "SKU-UNKNOWN",
              "warehouseCode": "WH-NORTH",
              "quantityAvailable": 10,
              "reorderThreshold": 5
            }
            """;

        String responseRequestId = mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.requestId").exists())
            .andReturn()
            .getResponse()
            .getHeader("X-Request-Id");

        assertThat(responseRequestId).isNotBlank();
        assertThat(auditLogRepository.findTop20ByOrderByCreatedAtDesc())
            .anyMatch(log -> "REQUEST_REJECTED".equals(log.getAction())
                && log.getStatus() == AuditStatus.FAILURE
                && responseRequestId.equals(log.getRequestId()));
    }

    @Test
    void reseedRestoresStarterBaselineAndStopsSimulation() throws Exception {
        String orderBody = """
            {
              "warehouseCode": "WH-NORTH",
              "items": [
                {
                  "productSku": "SKU-FLX-100",
                  "quantity": 9,
                  "unitPrice": 95.00
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(orderBody))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/simulation/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/scenarios/order-impact")
                .contentType(APPLICATION_JSON)
                .content(orderBody))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/dev/reseed"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("reseeded"))
            .andExpect(jsonPath("$.productsSeeded").value(4))
            .andExpect(jsonPath("$.warehousesSeeded").value(2))
            .andExpect(jsonPath("$.inventoryRecordsSeeded").value(8))
            .andExpect(jsonPath("$.simulation.active").value(false));

        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrders").value(0))
            .andExpect(jsonPath("$.activeAlerts").value(0))
            .andExpect(jsonPath("$.lowStockItems").value(0))
            .andExpect(jsonPath("$.recommendationsCount").value(0))
            .andExpect(jsonPath("$.totalProducts").value(4))
            .andExpect(jsonPath("$.totalWarehouses").value(2))
            .andExpect(jsonPath("$.inventoryRecordsCount").value(8))
            .andExpect(jsonPath("$.simulationRunning").value(false));

        mockMvc.perform(get("/api/scenarios/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void systemRuntimeEndpointReportsCurrentTrustSurface() throws Exception {
        mockMvc.perform(get("/api/system/runtime"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationName").value("synapsecore-backend"))
            .andExpect(jsonPath("$.activeProfiles[0]").value("test"))
            .andExpect(jsonPath("$.overallStatus").value("UP"))
            .andExpect(jsonPath("$.livenessState").value("CORRECT"))
            .andExpect(jsonPath("$.readinessState").value("ACCEPTING_TRAFFIC"))
            .andExpect(jsonPath("$.headerFallbackEnabled").value(true))
            .andExpect(jsonPath("$.secureSessionCookies").value(false))
            .andExpect(jsonPath("$.simulationIntervalMs").value(60000))
            .andExpect(jsonPath("$.telemetry.disabledConnectorCount").value(0))
            .andExpect(jsonPath("$.telemetry.replayQueueDepth").value(0))
            .andExpect(jsonPath("$.telemetry.recentImportIssues").value(0))
            .andExpect(jsonPath("$.telemetry.recentAuditFailures").value(0))
            .andExpect(jsonPath("$.telemetry.activeAlertCount").value(0))
            .andExpect(jsonPath("$.telemetry.fulfillmentBacklogCount").value(0))
            .andExpect(jsonPath("$.telemetry.delayedFulfillmentCount").value(0))
            .andExpect(jsonPath("$.telemetry.dispatchQueueDepth").value(0))
            .andExpect(jsonPath("$.telemetry.failedDispatchCount").value(0))
            .andExpect(jsonPath("$.backbone.pendingDispatchCount").value(0))
            .andExpect(jsonPath("$.backbone.failedDispatchCount").value(0))
            .andExpect(jsonPath("$.backbone.dispatchIntervalMs").value(1500))
            .andExpect(jsonPath("$.backbone.batchSize").value(16))
            .andExpect(jsonPath("$.metrics.ordersIngested").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.metrics.fulfillmentUpdates").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.metrics.integrationImportRuns").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.metrics.replayAttempts").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.metrics.dispatchQueued").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.metrics.dispatchProcessed").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.metrics.dispatchFailures").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.diagnostics.windowHours").value(24))
            .andExpect(jsonPath("$.diagnostics.activeIncidentCount").value(0))
            .andExpect(jsonPath("$.allowedOrigins.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void systemRuntimeEndpointIncludesOperationalTelemetrySignals() throws Exception {
        String csvBody = """
            externalOrderId,warehouseCode,productSku,quantity,unitPrice
            CSV-TLM-1001,WH-NORTH,SKU-MISSING-404,3,88.00
            """;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "telemetry-orders.csv",
            "text/csv",
            csvBody.getBytes()
        );

        mockMvc.perform(multipart("/api/integrations/orders/csv-import")
                .file(file)
                .param("sourceSystem", "erp_batch"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordersImported").value(0))
            .andExpect(jsonPath("$.ordersFailed").value(1));

        mockMvc.perform(post("/api/inventory/update")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "productSku": "SKU-UNKNOWN",
                      "warehouseCode": "WH-NORTH",
                      "quantityAvailable": 5,
                      "reorderThreshold": 2
                    }
                    """))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/integrations/orders/connectors")
                .with(accessHeaders("Integration Lead", "INTEGRATION_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem": "erp_coast",
                      "type": "WEBHOOK_ORDER",
                      "displayName": "ERP Coast Webhook",
                      "enabled": false,
                      "defaultWarehouseCode": "WH-COAST",
                      "notes": "Paused for runtime telemetry verification."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));

        operationalDispatchWorkItemRepository.save(OperationalDispatchWorkItem.builder()
            .tenantCode("SYNAPSE-DEMO")
            .updateType(OperationalUpdateType.ORDER_FLOW)
            .source("runtime-test")
            .requestId("req-backbone-1")
            .status(OperationalDispatchStatus.FAILED)
            .attemptCount(1)
            .occurredAt(Instant.now())
            .lastError("Synthetic queue failure for runtime verification.")
            .build());

        mockMvc.perform(get("/api/system/runtime"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.telemetry.disabledConnectorCount").value(1))
            .andExpect(jsonPath("$.telemetry.replayQueueDepth").value(1))
            .andExpect(jsonPath("$.telemetry.recentImportIssues").value(1))
            .andExpect(jsonPath("$.telemetry.recentAuditFailures")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.telemetry.dispatchQueueDepth").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.telemetry.failedDispatchCount").value(1))
            .andExpect(jsonPath("$.backbone.failedDispatchCount").value(1))
            .andExpect(jsonPath("$.metrics.integrationImportRuns")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(1.0)))
            .andExpect(jsonPath("$.metrics.dispatchQueued")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(2.0)))
            .andExpect(jsonPath("$.metrics.dispatchProcessed")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.diagnostics.integrationEventsInWindow")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.diagnostics.failureAuditsInWindow")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.diagnostics.activeIncidentCount")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));

        mockMvc.perform(get("/api/system/incidents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.type == 'AUDIT_FAILURE')]")
                .value(org.hamcrest.Matchers.hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[?(@.type == 'REPLAY_BACKLOG')]")
                .value(org.hamcrest.Matchers.hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[?(@.type == 'BACKBONE_DISPATCH_FAILURE')]")
                .value(org.hamcrest.Matchers.hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[?(@.type == 'CONNECTOR_DISABLED')]")
                .value(org.hamcrest.Matchers.hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/dashboard/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.systemIncidents.length()")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void committedRequestsDrainDispatchQueueAndExposePrometheusMetrics() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "externalOrderId": "PROM-1001",
                      "warehouseCode": "WH-NORTH",
                      "items": [
                        {
                          "productSku": "SKU-FLX-100",
                          "quantity": 1,
                          "unitPrice": 95.00
                        }
                      ]
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/system/runtime"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.telemetry.dispatchQueueDepth").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.backbone.pendingDispatchCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.metrics.ordersIngested").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1.0)))
            .andExpect(jsonPath("$.metrics.dispatchProcessed").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1.0)));

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("synapsecore_dispatch_queue_backlog")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("synapsecore_orders_ingested_total")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("synapsecore_fulfillment_backlog")));
    }

    private Inventory loadInventory(String sku, String warehouseCode) {
        Long productId = productRepository.findBySku(sku).orElseThrow().getId();
        Long warehouseId = warehouseRepository.findByCode(warehouseCode).orElseThrow().getId();
        return inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId).orElseThrow();
    }

    private static RequestPostProcessor accessHeaders(String actorName, String roles) {
        return request -> {
            request.addHeader("X-Synapse-Actor", actorName);
            request.addHeader("X-Synapse-Roles", roles);
            return request;
        };
    }
}
