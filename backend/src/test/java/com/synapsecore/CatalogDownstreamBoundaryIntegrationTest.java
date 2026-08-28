package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogDownstreamBoundaryIntegrationTest {

    private static final String STARTER_TENANT = "STARTER-OPS";
    private static final String SECOND_TENANT = "PHASE3-SECOND-OPS";
    private static final String TEST_PLATFORM_ADMIN_TOKEN = "test-only-platform-admin-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void productMutationsDoNotCreateOrChangeInventoryOrOperationalSignals() throws Exception {
        int alertBefore = countTenantIdRows("alerts", STARTER_TENANT);
        int recommendationBefore = countTenantIdRows("recommendations", STARTER_TENANT);

        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/products")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"PHASE3-BOUNDARY-100\",\"name\":\"Boundary Product\",\"category\":\"Verification\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
        long productId = created.path("id").asLong();
        long northWarehouseId = jdbcTemplate.queryForObject(
            "select w.id from warehouses w join tenants t on t.id = w.tenant_id where upper(t.code) = upper(?) and upper(w.code) = upper(?)",
            Long.class,
            STARTER_TENANT,
            "WH-NORTH"
        );

        assertThat(inventoryRepository.findByProductIdAndWarehouseId(productId, northWarehouseId)).isEmpty();

        mockMvc.perform(post("/api/inventory/update")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("{\"productSku\":\"PHASE3-BOUNDARY-100\",\"warehouseCode\":\"WH-NORTH\",\"quantityAvailable\":37,\"reorderThreshold\":10}"))
            .andExpect(status().isOk());
        Inventory baselineInventory = inventoryRepository.findByProductIdAndWarehouseId(productId, northWarehouseId).orElseThrow();
        long baselineAvailable = baselineInventory.getQuantityAvailable();
        long baselineOnHand = baselineInventory.getQuantityOnHand();

        mockMvc.perform(put("/api/products/" + productId)
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"PHASE3-BOUNDARY-100\",\"name\":\"Updated Boundary Product\",\"category\":\"Updated\"}"))
            .andExpect(status().isOk());

        String csv = "sku,name,category\nPHASE3-BOUNDARY-100,CSV Boundary Product,Imported\n";
        mockMvc.perform(multipart("/api/products/import")
                .file(new MockMultipartFile("file", "phase3-boundary.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.updated").value(1));

        Inventory afterCatalogMutations = inventoryRepository.findByProductIdAndWarehouseId(productId, northWarehouseId).orElseThrow();
        assertThat(afterCatalogMutations.getQuantityAvailable()).isEqualTo(baselineAvailable);
        assertThat(afterCatalogMutations.getQuantityOnHand()).isEqualTo(baselineOnHand);
        assertThat(countTenantIdRows("alerts", STARTER_TENANT)).isEqualTo(alertBefore);
        assertThat(countTenantIdRows("recommendations", STARTER_TENANT)).isEqualTo(recommendationBefore);
    }

    @Test
    void knownAndUnknownOrdersResolveTenantCatalogIdentityOnly() throws Exception {
        JsonNode product = createProduct("PHASE3-ORDER-100", "Order Resolution Product", "Verification", STARTER_TENANT);
        long productId = product.path("id").asLong();
        ensureInventory("PHASE3-ORDER-100", "WH-NORTH", 25, 5, STARTER_TENANT);

        mockMvc.perform(post("/api/orders")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Integration Operator", "INTEGRATION_OPERATOR"))
                .contentType(APPLICATION_JSON)
                .content("{\"externalOrderId\":\"PHASE3-ORDER-RESOLVE-100\",\"warehouseCode\":\"WH-NORTH\",\"items\":[{\"productSku\":\"PHASE3-ORDER-100\",\"quantity\":2,\"unitPrice\":12.50}]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].productSku").value("PHASE3-ORDER-100"))
            .andExpect(jsonPath("$.items[0].productName").value("Order Resolution Product"));

        long productCountBeforeUnknown = countProducts(STARTER_TENANT, "PHASE3-UNKNOWN-100");
        mockMvc.perform(post("/api/orders")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Integration Operator", "INTEGRATION_OPERATOR"))
                .contentType(APPLICATION_JSON)
                .content("{\"externalOrderId\":\"PHASE3-ORDER-UNKNOWN-100\",\"warehouseCode\":\"WH-NORTH\",\"items\":[{\"productSku\":\"PHASE3-UNKNOWN-100\",\"quantity\":1,\"unitPrice\":10.00}]}"))
            .andExpect(status().isNotFound());
        assertThat(countProducts(STARTER_TENANT, "PHASE3-UNKNOWN-100")).isEqualTo(productCountBeforeUnknown);

        provisionSecondTenant();
        createProduct("PHASE3-CROSS-TENANT-100", "Second Tenant Product", "Verification", SECOND_TENANT);
        mockMvc.perform(post("/api/orders")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Integration Operator", "INTEGRATION_OPERATOR"))
                .contentType(APPLICATION_JSON)
                .content("{\"externalOrderId\":\"PHASE3-CROSS-TENANT-ORDER\",\"warehouseCode\":\"WH-NORTH\",\"items\":[{\"productSku\":\"PHASE3-CROSS-TENANT-100\",\"quantity\":1,\"unitPrice\":10.00}]}"))
            .andExpect(status().isNotFound());
        assertThat(countProducts(STARTER_TENANT, "PHASE3-CROSS-TENANT-100")).isZero();
        assertThat(countProducts(SECOND_TENANT, "PHASE3-CROSS-TENANT-100")).isEqualTo(1);
        assertThat(productRepository.findById(productId)).isPresent();
    }

    @Test
    void productUpdateUsesCurrentMasterDataWithoutRewritingOrderFacts() throws Exception {
        JsonNode product = createProduct("PHASE3-HISTORY-100", "Original Name", "Original Category", STARTER_TENANT);
        long productId = product.path("id").asLong();
        ensureInventory("PHASE3-HISTORY-100", "WH-NORTH", 20, 5, STARTER_TENANT);

        JsonNode createdOrder = objectMapper.readTree(mockMvc.perform(post("/api/orders")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Integration Operator", "INTEGRATION_OPERATOR"))
                .contentType(APPLICATION_JSON)
                .content("{\"externalOrderId\":\"PHASE3-HISTORY-ORDER-100\",\"warehouseCode\":\"WH-NORTH\",\"items\":[{\"productSku\":\"PHASE3-HISTORY-100\",\"quantity\":3,\"unitPrice\":18.75}]}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
        String createdAt = createdOrder.path("createdAt").asText();
        String status = createdOrder.path("status").asText();
        String warehouseCode = createdOrder.path("warehouseCode").asText();
        String externalOrderId = createdOrder.path("externalOrderId").asText();
        int quantity = createdOrder.path("items").get(0).path("quantity").asInt();
        String unitPrice = createdOrder.path("items").get(0).path("unitPrice").asText();

        mockMvc.perform(put("/api/products/" + productId)
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"PHASE3-HISTORY-100\",\"name\":\"Current Master Name\",\"category\":\"Current Master Category\"}"))
            .andExpect(status().isOk());

        JsonNode readback = objectMapper.readTree(mockMvc.perform(get("/api/orders/recent")
                .param("externalOrderId", "PHASE3-HISTORY-ORDER-100")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()).get(0);

        assertThat(readback.path("externalOrderId").asText()).isEqualTo(externalOrderId);
        assertThat(readback.path("status").asText()).isEqualTo(status);
        assertThat(readback.path("warehouseCode").asText()).isEqualTo(warehouseCode);
        assertThat(Duration.between(Instant.parse(createdAt), Instant.parse(readback.path("createdAt").asText())).abs())
            .isLessThanOrEqualTo(Duration.ofMillis(1));
        assertThat(readback.path("items").get(0).path("productSku").asText()).isEqualTo("PHASE3-HISTORY-100");
        assertThat(readback.path("items").get(0).path("productName").asText()).isEqualTo("Current Master Name");
        assertThat(readback.path("items").get(0).path("quantity").asInt()).isEqualTo(quantity);
        assertThat(readback.path("items").get(0).path("unitPrice").asText()).isEqualTo(unitPrice);
    }

    @Test
    void productIdentityRoundTripSeparatesBusinessAndInternalSku() throws Exception {
        JsonNode response = createProduct("phase3-roundtrip-100", "Round Trip Product", "Verification", STARTER_TENANT);

        assertThat(response.path("sku").asText()).isEqualTo("PHASE3-ROUNDTRIP-100");
        assertThat(response.path("catalogSku").asText()).isEqualTo("PHASE3-ROUNDTRIP-100");
        assertThat(response.path("internalSku").asText()).isEqualTo("STARTER-OPS::PHASE3-ROUNDTRIP-100");
        assertThat(response.path("tenantCode").asText()).isEqualTo(STARTER_TENANT);

        mockMvc.perform(get("/api/products")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.sku == 'PHASE3-ROUNDTRIP-100')].catalogSku").value(org.hamcrest.Matchers.contains("PHASE3-ROUNDTRIP-100")))
            .andExpect(jsonPath("$[?(@.sku == 'PHASE3-ROUNDTRIP-100')].internalSku").value(org.hamcrest.Matchers.contains("STARTER-OPS::PHASE3-ROUNDTRIP-100")));
    }

    @Test
    void catalogRefreshChangesOnlyCatalogDashboardTruthAndNegativeSmokeStaysBounded() throws Exception {
        JsonNode before = objectMapper.readTree(mockMvc.perform(get("/api/dashboard/summary")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
        createProduct("PHASE3-DASHBOARD-100", "Dashboard Boundary Product", "Verification", STARTER_TENANT);
        JsonNode after = objectMapper.readTree(mockMvc.perform(get("/api/dashboard/summary")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());

        assertThat(after.path("totalProducts").asLong()).isEqualTo(before.path("totalProducts").asLong());
        assertThat(after.path("totalOrders").asLong()).isEqualTo(before.path("totalOrders").asLong());
        assertThat(after.path("activeAlerts").asLong()).isEqualTo(before.path("activeAlerts").asLong());
        assertThat(after.path("lowStockItems").asLong()).isEqualTo(before.path("lowStockItems").asLong());
        assertThat(after.path("recommendationsCount").asLong()).isEqualTo(before.path("recommendationsCount").asLong());
        assertThat(after.path("inventoryRecordsCount").asLong()).isEqualTo(before.path("inventoryRecordsCount").asLong());

        mockMvc.perform(post("/api/products")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Operator", "INTEGRATION_OPERATOR"))
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"PHASE3-DENIED-100\",\"name\":\"Denied\",\"category\":\"Verification\"}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/products/999999")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"PHASE3-UNKNOWN-UPDATE\",\"name\":\"Unknown\",\"category\":\"Verification\"}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/products")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"TENANT-A::CROSS-TENANT\",\"name\":\"Invalid Internal SKU\",\"category\":\"Verification\"}"))
            .andExpect(status().isBadRequest());
    }

    private JsonNode createProduct(String sku, String name, String category, String tenantCode) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/products")
                .header("X-Synapse-Tenant", tenantCode)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content(("{\"sku\":\"%s\",\"name\":\"%s\",\"category\":\"%s\"}").formatted(sku, name, category)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
    }

    private void ensureInventory(String sku, String warehouseCode, long available, long threshold, String tenantCode) throws Exception {
        mockMvc.perform(post("/api/inventory/update")
                .header("X-Synapse-Tenant", tenantCode)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content(("{\"productSku\":\"%s\",\"warehouseCode\":\"%s\",\"quantityAvailable\":%d,\"reorderThreshold\":%d}").formatted(sku, warehouseCode, available, threshold)))
            .andExpect(status().isOk());
    }

    private void provisionSecondTenant() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", TEST_PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "PHASE3-SECOND-OPS",
                      "tenantName": "Phase 3 Second Operations",
                      "description": "Synthetic downstream boundary fixture.",
                      "adminFullName": "Phase 3 Tenant Administrator",
                      "adminUsername": "phase3.second.admin",
                      "adminPassword": "phase3-second-password",
                      "primaryLocation": "North Hub",
                      "secondaryLocation": "Coast Hub"
                    }
                    """))
            .andExpect(status().isOk());
    }

    private int countProducts(String tenantCode, String catalogSku) {
        return jdbcTemplate.queryForObject(
            "select count(*) from products p join tenants t on t.id = p.tenant_id where upper(t.code) = upper(?) and upper(coalesce(p.catalog_sku, p.sku)) = upper(?)",
            Integer.class,
            tenantCode,
            catalogSku
        );
    }

    private int countTenantIdRows(String tableName, String tenantCode) {
        return jdbcTemplate.queryForObject(
            "select count(*) from " + tableName + " r join tenants t on t.id = r.tenant_id where upper(t.code) = upper(?)",
            Integer.class,
            tenantCode
        );
    }

    private static RequestPostProcessor accessHeaders(String actorName, String roles) {
        return request -> {
            request.addHeader("X-Synapse-Actor", actorName);
            request.addHeader("X-Synapse-Roles", roles);
            return request;
        };
    }
}
