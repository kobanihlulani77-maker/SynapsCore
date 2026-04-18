package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.access.BootstrapAccessService;
import com.synapsecore.access.PlatformAdministrationAccessService;
import com.synapsecore.access.TenantOnboardingService;
import com.synapsecore.access.dto.TenantOnboardingRequest;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.domain.service.DataInitializer;
import com.synapsecore.domain.service.SeedService;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.simulation.SimulationService;
import com.synapsecore.tenant.TenantContextService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = {
    "spring.profiles.active=prod",
    "spring.datasource.url=jdbc:h2:mem:prodhardening;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.url=redis://localhost:6379",
    "synapsecore.bootstrap.initial-token=bootstrap-secret",
    "synapsecore.bootstrap.platform-admin-token=platform-admin-secret"
})
@AutoConfigureMockMvc
@Transactional
class ProductionHardeningIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private IntegrationConnectorRepository integrationConnectorRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private TenantContextService tenantContextService;

    @Autowired
    private TenantOnboardingService tenantOnboardingService;

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeedService seedService;

    @Autowired
    private IntegrationConnectorService integrationConnectorService;

    @Autowired(required = false)
    private DataInitializer dataInitializer;

    @Test
    void prodProfileDoesNotSeedStarterDataWhenAutoSeedDisabled() {
        assertThat(tenantRepository.count()).isZero();
        assertThat(productRepository.count()).isZero();
        assertThat(warehouseRepository.count()).isZero();
        assertThat(inventoryRepository.count()).isZero();
        assertThat(integrationConnectorRepository.count()).isZero();
    }

    @Test
    void prodProfileDoesNotRegisterStartupDataInitializer() {
        assertThat(dataInitializer).isNull();
    }

    @Test
    void prodProfileSeedServiceDoesNotBackfillWhenDemoSeedingIsDisabled() {
        productRepository.save(Product.builder()
            .sku("SKU-PROD-001")
            .name("Production Sensor")
            .category("Sensors")
            .build());

        assertThat(seedService.seedIfEmpty()).isFalse();
        assertThat(tenantRepository.count()).isZero();
        assertThat(integrationConnectorRepository.count()).isZero();
    }

    @Test
    void prodProfileStarterConnectorSeedingIsDisabled() {
        Tenant tenant = tenantRepository.save(Tenant.builder()
            .code("REAL-OPS")
            .name("Real Operations")
            .active(true)
            .build());

        integrationConnectorService.seedStarterConnectors(tenant);

        assertThat(integrationConnectorRepository.count()).isZero();
    }

    @Test
    void prodProfileRequiresExplicitTenantContextWhenDefaultFallbackDisabled() {
        RequestContextHolder.resetRequestAttributes();
        assertThatThrownBy(() -> tenantContextService.getCurrentTenantCodeOrDefault())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Tenant context is required");
    }

    @Test
    void prodProfileOnboardingCreatesWorkspaceWithoutDemoInventoryOrConnectors() {
        productRepository.save(Product.builder().sku("SKU-HARD-100").name("Hardening Sensor").category("Sensors").build());
        productRepository.save(Product.builder().sku("SKU-HARD-200").name("Hardening Relay").category("Control").build());

        var response = tenantOnboardingService.onboardTenant(new TenantOnboardingRequest(
            "REAL-OPS",
            "Real Operations",
            "Production tenant bootstrap",
            "Rina Patel",
            "rina.admin",
            "StrongPass-2026",
            "Johannesburg",
            "Cape Town"
        ), "platform-bootstrap");

        assertThat(response.tenantCode()).isEqualTo("REAL-OPS");
        assertThat(response.executiveUsername()).isEqualTo("real.ops.executive");
        assertThat(response.starterWarehouseCodes()).containsExactlyInAnyOrder("WH-NORTH", "WH-COAST");
        assertThat(warehouseRepository.count()).isEqualTo(2L);
        assertThat(inventoryRepository.count()).isZero();
        assertThat(integrationConnectorRepository.count()).isZero();
    }

    @Test
    void prodProfileDisablesSimulationControls() {
        assertThatThrownBy(() -> simulationService.start())
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Simulation is disabled in this environment.");
    }

    @Test
    void prodProfileRequiresBootstrapTokenForFirstTenantCreation() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "REAL-FIRST",
                      "tenantName": "Real First Tenant",
                      "description": "First production workspace",
                      "adminFullName": "Rina Patel",
                      "adminUsername": "rina.admin",
                      "adminPassword": "StrongPass-2026",
                      "primaryLocation": "Johannesburg",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("A valid bootstrap token is required to create the first tenant."))
            .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void prodProfileAllowsFirstTenantCreationWithBootstrapTokenOnlyOnce() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header(BootstrapAccessService.BOOTSTRAP_TOKEN_HEADER, "bootstrap-secret")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "REAL-FIRST",
                      "tenantName": "Real First Tenant",
                      "description": "First production workspace",
                      "adminFullName": "Rina Patel",
                      "adminUsername": "rina.admin",
                      "adminPassword": "StrongPass-2026",
                      "primaryLocation": "Johannesburg",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("REAL-FIRST"))
            .andExpect(jsonPath("$.executivePassword").doesNotExist());

        mockMvc.perform(post("/api/access/tenants")
                .header(BootstrapAccessService.BOOTSTRAP_TOKEN_HEADER, "bootstrap-secret")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "REAL-SECOND",
                      "tenantName": "Real Second Tenant",
                      "description": "Second production workspace",
                      "adminFullName": "Kabelo Dube",
                      "adminUsername": "kabelo.admin",
                      "adminPassword": "StrongPass-2026",
                      "primaryLocation": "Durban",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value(
                "Initial tenant bootstrap is not available after the first tenant has been created."))
            .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void prodProfileBlocksTenantAdminFromCreatingAdditionalTenantWorkspaces() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header(BootstrapAccessService.BOOTSTRAP_TOKEN_HEADER, "bootstrap-secret")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "REAL-FIRST",
                      "tenantName": "Real First Tenant",
                      "description": "First production workspace",
                      "adminFullName": "Rina Patel",
                      "adminUsername": "rina.admin",
                      "adminPassword": "StrongPass-2026",
                      "primaryLocation": "Johannesburg",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isOk());

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "REAL-FIRST",
                      "username": "rina.admin",
                      "password": "StrongPass-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);

        mockMvc.perform(post("/api/access/tenants")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "REAL-SECOND",
                      "tenantName": "Real Second Tenant",
                      "description": "Second production workspace",
                      "adminFullName": "Kabelo Dube",
                      "adminUsername": "kabelo.admin",
                      "adminPassword": "StrongPass-2026",
                      "primaryLocation": "Durban",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(
                "Tenant workspace creation requires platform administration in this environment."));
    }

    @Test
    void prodProfileAllowsAdditionalTenantCreationWithPlatformAdminToken() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header(BootstrapAccessService.BOOTSTRAP_TOKEN_HEADER, "bootstrap-secret")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "REAL-FIRST",
                      "tenantName": "Real First Tenant",
                      "description": "First production workspace",
                      "adminFullName": "Rina Patel",
                      "adminUsername": "rina.admin",
                      "adminPassword": "StrongPass-2026",
                      "primaryLocation": "Johannesburg",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/access/tenants")
                .header(PlatformAdministrationAccessService.PLATFORM_ADMIN_TOKEN_HEADER, "platform-admin-secret")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "REAL-SECOND",
                      "tenantName": "Real Second Tenant",
                      "description": "Second production workspace",
                      "adminFullName": "Kabelo Dube",
                      "adminUsername": "kabelo.admin",
                      "adminPassword": "StrongPass-2026",
                      "primaryLocation": "Durban",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("REAL-SECOND"))
            .andExpect(jsonPath("$.executivePassword").doesNotExist());
    }

    @Test
    void prodProfileRejectsInvalidPlatformAdminTokenForAdditionalTenantCreation() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header(BootstrapAccessService.BOOTSTRAP_TOKEN_HEADER, "bootstrap-secret")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "REAL-FIRST",
                      "tenantName": "Real First Tenant",
                      "description": "First production workspace",
                      "adminFullName": "Rina Patel",
                      "adminUsername": "rina.admin",
                      "adminPassword": "StrongPass-2026",
                      "primaryLocation": "Johannesburg",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/access/tenants")
                .header(PlatformAdministrationAccessService.PLATFORM_ADMIN_TOKEN_HEADER, "wrong-secret")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "REAL-SECOND",
                      "tenantName": "Real Second Tenant",
                      "description": "Second production workspace",
                      "adminFullName": "Kabelo Dube",
                      "adminUsername": "kabelo.admin",
                      "adminPassword": "StrongPass-2026",
                      "primaryLocation": "Durban",
                      "secondaryLocation": "Cape Town"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(
                "A valid platform admin token is required to create tenant workspaces in this environment."))
            .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void prodProfileAllowsConnectorAuthenticatedWebhookIngressWithoutWorkspaceSession() throws Exception {
        Product product = productRepository.save(Product.builder()
            .sku("SKU-HARD-100")
            .name("Hardening Sensor")
            .category("Sensors")
            .build());

        tenantOnboardingService.onboardTenant(new TenantOnboardingRequest(
            "REAL-OPS",
            "Real Operations",
            "Production tenant bootstrap",
            "Rina Patel",
            "rina.admin",
            "StrongPass-2026",
            "Johannesburg",
            "Cape Town"
        ), "platform-bootstrap");

        var tenant = tenantRepository.findByCodeIgnoreCase("REAL-OPS").orElseThrow();
        var warehouse = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode("REAL-OPS", "WH-NORTH").orElseThrow();

        inventoryRepository.save(com.synapsecore.domain.entity.Inventory.builder()
            .product(product)
            .warehouse(warehouse)
            .quantityAvailable(18L)
            .reorderThreshold(5L)
            .build());

        integrationConnectorRepository.save(com.synapsecore.domain.entity.IntegrationConnector.builder()
            .tenant(tenant)
            .sourceSystem("real_erp_north")
            .type(com.synapsecore.domain.entity.IntegrationConnectorType.WEBHOOK_ORDER)
            .displayName("Real ERP North Webhook")
            .enabled(true)
            .allowDefaultWarehouseFallback(true)
            .defaultWarehouseCode("WH-NORTH")
            .inboundAccessTokenHash(com.synapsecore.integration.IntegrationConnectorService.hashInboundAccessToken(
                "real-webhook-token-2026"
            ))
            .inboundAccessTokenHint("••••2026")
            .build());

        mockMvc.perform(post("/api/integrations/orders/webhook")
                .header(com.synapsecore.integration.IntegrationInboundAccessService.CONNECTOR_TOKEN_HEADER,
                    "real-webhook-token-2026")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem": "real_erp_north",
                      "externalOrderId": "REAL-ERP-1001",
                      "warehouseCode": "",
                      "customerReference": "ACCT-1001",
                      "occurredAt": "2026-04-02T08:30:00Z",
                      "items": [
                        {
                          "productSku": "SKU-HARD-100",
                          "quantity": 2,
                          "unitPrice": 120.00
                        }
                      ]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sourceSystem").value("real_erp_north"))
            .andExpect(jsonPath("$.order.externalOrderId").value("REAL-ERP-1001"))
            .andExpect(jsonPath("$.order.warehouseCode").value("WH-NORTH"));

        assertThat(customerOrderRepository.existsByTenant_CodeIgnoreCaseAndExternalOrderId("REAL-OPS", "REAL-ERP-1001"))
            .isTrue();
    }

    @Test
    void customerOrderPersistenceRejectsWarehouseTenantMismatch() {
        Tenant alpha = tenantRepository.save(Tenant.builder().code("ALPHA").name("Alpha").build());
        Tenant bravo = tenantRepository.save(Tenant.builder().code("BRAVO").name("Bravo").build());
        Warehouse bravoWarehouse = warehouseRepository.save(Warehouse.builder()
            .tenant(bravo)
            .code("WH-BRAVO")
            .name("Bravo Hub")
            .location("Cape Town")
            .build());

        assertThatThrownBy(() -> customerOrderRepository.saveAndFlush(CustomerOrder.builder()
                .tenant(alpha)
                .externalOrderId("ALPHA-ORD-001")
                .status(OrderStatus.RECEIVED)
                .totalAmount(BigDecimal.TEN)
                .warehouse(bravoWarehouse)
                .build()))
            .hasRootCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("assigned to tenant ALPHA")
            .hasMessageContaining("belongs to tenant BRAVO");
    }

    @Test
    void fulfillmentTaskPersistenceRejectsTenantMismatchAcrossOrderAndWarehouse() {
        Tenant alpha = tenantRepository.save(Tenant.builder().code("ALPHA").name("Alpha").build());
        Tenant bravo = tenantRepository.save(Tenant.builder().code("BRAVO").name("Bravo").build());
        Warehouse alphaWarehouse = warehouseRepository.save(Warehouse.builder()
            .tenant(alpha)
            .code("WH-ALPHA")
            .name("Alpha Hub")
            .location("Johannesburg")
            .build());
        Warehouse bravoWarehouse = warehouseRepository.save(Warehouse.builder()
            .tenant(bravo)
            .code("WH-BRAVO")
            .name("Bravo Hub")
            .location("Cape Town")
            .build());

        CustomerOrder order = customerOrderRepository.saveAndFlush(CustomerOrder.builder()
            .tenant(alpha)
            .externalOrderId("ALPHA-ORD-002")
            .status(OrderStatus.RECEIVED)
            .totalAmount(BigDecimal.ONE)
            .warehouse(alphaWarehouse)
            .build());

        assertThatThrownBy(() -> fulfillmentTaskRepository.saveAndFlush(FulfillmentTask.builder()
                .tenant(alpha)
                .customerOrder(order)
                .warehouse(bravoWarehouse)
                .status(FulfillmentStatus.QUEUED)
                .queuedAt(Instant.now())
                .build()))
            .hasRootCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("warehouse tenant BRAVO");
    }
}
