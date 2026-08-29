package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.alert.AlertService;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.Alert;
import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.AlertType;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.intelligence.InventoryInsight;
import com.synapsecore.prediction.StockPrediction;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AlertLifecyclePhase1IntegrationTest {

    private static final String TENANT_CODE = "ALERT-PHASE-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AccessOperatorRepository accessOperatorRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    @Test
    void alertsAreScopedByWarehouseAndTenantWithStructuredIdentity() throws Exception {
        Tenant tenant = createTenant(TENANT_CODE);
        Warehouse north = createWarehouse(tenant, "WH-NORTH");
        Warehouse coast = createWarehouse(tenant, "WH-COAST");
        Product northProduct = createProduct(tenant, "SKU-NORTH-1");
        Product coastProduct = createProduct(tenant, "SKU-COAST-1");
        createOperator(tenant, "North Alert Reader", Set.of("WH-NORTH"));
        createOperator(tenant, "Coast Alert Reader", Set.of("WH-COAST"));
        createOperator(tenant, "Tenant Alert Reader", Set.of());

        saveAlert(tenant, north, northProduct, AlertType.LOW_STOCK,
            "North condition", "INVENTORY_PRODUCT_WAREHOUSE", "north-source", "north-key");
        saveAlert(tenant, coast, coastProduct, AlertType.LOW_STOCK,
            "Coast condition", "INVENTORY_PRODUCT_WAREHOUSE", "coast-source", "coast-key");

        mockMvc.perform(get("/api/alerts")
                .header("X-Synapse-Tenant", TENANT_CODE)
                .header("X-Synapse-Actor", "North Alert Reader")
                .header("X-Synapse-Roles", "INTEGRATION_OPERATOR"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.activeAlerts.length()")
                .value(1))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.activeAlerts[0].warehouseCode")
                .value("WH-NORTH"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.activeAlerts[0].productSku")
                .value("SKU-NORTH-1"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.activeAlerts[0].sourceType")
                .value("INVENTORY_PRODUCT_WAREHOUSE"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$..warehouseCode")
                .value(Matchers.not(Matchers.hasItem("WH-COAST"))));

        String coastResponse = mockMvc.perform(get("/api/alerts")
                .header("X-Synapse-Tenant", TENANT_CODE)
                .header("X-Synapse-Actor", "Coast Alert Reader")
                .header("X-Synapse-Roles", "INTEGRATION_OPERATOR"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(coastResponse).contains("WH-COAST").doesNotContain("SKU-NORTH-1");

        String tenantWideResponse = mockMvc.perform(get("/api/alerts")
                .header("X-Synapse-Tenant", TENANT_CODE)
                .header("X-Synapse-Actor", "Tenant Alert Reader")
                .header("X-Synapse-Roles", "TENANT_ADMIN"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(tenantWideResponse).contains("WH-NORTH", "WH-COAST", "SKU-NORTH-1", "SKU-COAST-1");
    }

    @Test
    void repeatedEvaluationUsesStructuredConditionIdentityAcrossProductsAndWarehouses() {
        Inventory northFlux = inventoryRepository.findByProductIdAndWarehouseId(
                productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase("STARTER-OPS", "SKU-FLX-100").orElseThrow().getId(),
                warehouseRepository.findByTenant_CodeIgnoreCaseAndCode("STARTER-OPS", "WH-NORTH").orElseThrow().getId())
            .orElseThrow();
        Inventory northVector = inventoryRepository.findByProductIdAndWarehouseId(
                productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase("STARTER-OPS", "SKU-VDR-210").orElseThrow().getId(),
                warehouseRepository.findByTenant_CodeIgnoreCaseAndCode("STARTER-OPS", "WH-NORTH").orElseThrow().getId())
            .orElseThrow();
        Inventory coastFlux = inventoryRepository.findByProductIdAndWarehouseId(
                productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase("STARTER-OPS", "SKU-FLX-100").orElseThrow().getId(),
                warehouseRepository.findByTenant_CodeIgnoreCaseAndCode("STARTER-OPS", "WH-COAST").orElseThrow().getId())
            .orElseThrow();
        InventoryInsight insight = new InventoryInsight(true, false, false, false, AlertSeverity.HIGH, "HIGH", "Test pressure.");
        StockPrediction prediction = new StockPrediction(0, 0, null, false, false, false);

        alertService.syncInventoryAlerts(northFlux, insight, prediction, null, "phase-1-test");
        alertService.syncInventoryAlerts(northVector, insight, prediction, null, "phase-1-test");
        alertService.syncInventoryAlerts(coastFlux, insight, prediction, null, "phase-1-test");

        Alert firstNorthAlert = alertRepository.findAll().stream()
            .filter(alert -> alert.getType() == AlertType.LOW_STOCK)
            .filter(alert -> alert.getProduct().getCatalogSku().equals("SKU-FLX-100"))
            .filter(alert -> alert.getWarehouse().getCode().equals("WH-NORTH"))
            .findFirst().orElseThrow();
        Long firstId = firstNorthAlert.getId();
        String conditionKey = firstNorthAlert.getConditionKey();
        firstNorthAlert.setTitle("Display wording changed without changing identity");
        alertRepository.save(firstNorthAlert);

        alertService.syncInventoryAlerts(northFlux, insight, prediction, null, "phase-1-test");

        List<Alert> activeLowStock = alertRepository.findAll().stream()
            .filter(alert -> alert.getType() == AlertType.LOW_STOCK && alert.getStatus() == AlertStatus.ACTIVE)
            .toList();
        assertThat(activeLowStock).hasSize(3);
        assertThat(activeLowStock).extracting(Alert::getConditionKey).doesNotHaveDuplicates();
        assertThat(activeLowStock.stream().filter(alert -> alert.getId().equals(firstId)).findFirst().orElseThrow().getConditionKey())
            .isEqualTo(conditionKey);
        assertThat(activeLowStock.stream().filter(alert -> alert.getId().equals(firstId)).findFirst().orElseThrow().getTitle())
            .isEqualTo("Display wording changed without changing identity");
    }

    private Tenant createTenant(String code) {
        return tenantRepository.save(Tenant.builder().code(code).name(code + " tenant").description("Alert Phase 1 fixture").build());
    }

    private Warehouse createWarehouse(Tenant tenant, String code) {
        return warehouseRepository.save(Warehouse.builder().tenant(tenant).code(code).name(code).location(code + " location").build());
    }

    private Product createProduct(Tenant tenant, String sku) {
        return productRepository.save(Product.builder().tenant(tenant).catalogSku(sku).name(sku).category("Alert test").build());
    }

    private void createOperator(Tenant tenant, String actorName, Set<String> warehouseScopes) {
        accessOperatorRepository.save(AccessOperator.builder()
            .tenant(tenant)
            .actorName(actorName)
            .displayName(actorName)
            .description("Alert Phase 1 fixture")
            .roles(EnumSet.of(com.synapsecore.access.SynapseAccessRole.INTEGRATION_OPERATOR))
            .warehouseScopes(warehouseScopes)
            .build());
    }

    private Alert saveAlert(Tenant tenant,
                            Warehouse warehouse,
                            Product product,
                            AlertType type,
                            String title,
                            String sourceType,
                            String sourceRef,
                            String conditionKey) {
        return alertRepository.save(Alert.builder()
            .tenant(tenant)
            .warehouse(warehouse)
            .product(product)
            .type(type)
            .severity(AlertSeverity.HIGH)
            .title(title)
            .description("Alert description")
            .impactSummary("Alert impact")
            .recommendedAction("Review the source condition")
            .sourceType(sourceType)
            .sourceRef(sourceRef)
            .conditionKey(conditionKey)
            .status(AlertStatus.ACTIVE)
            .build());
    }
}
