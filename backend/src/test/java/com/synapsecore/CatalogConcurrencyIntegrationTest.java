package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.repository.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogConcurrencyIntegrationTest {

    private static final String STARTER_TENANT = "STARTER-OPS";
    private static final String SECOND_TENANT = "PHASE2-SECOND-OPS";
    private static final String TEST_PLATFORM_ADMIN_TOKEN = "test-only-platform-admin-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mixedCsvResponseMatchesCommittedRows() throws Exception {
        int inventoryBefore = countTenantIdRows("inventory", STARTER_TENANT);
        int orderBefore = countTenantIdRows("customer_orders", STARTER_TENANT);
        int alertBefore = countTenantIdRows("alerts", STARTER_TENANT);
        int recommendationBefore = countTenantIdRows("recommendations", STARTER_TENANT);
        String csv = """
            sku,name,category
            PHASE2-MIX-A,Mixed Import A,Verification
            PHASE2-MIX-B,,Verification
            PHASE2-MIX-C,Mixed Import C,Verification
            """;

        MvcResult result = mockMvc.perform(multipart("/api/products/import")
                .file(new MockMultipartFile("file", "phase2-mixed.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(3))
            .andExpect(jsonPath("$.created").value(2))
            .andExpect(jsonPath("$.failed").value(1))
            .andExpect(jsonPath("$.rows[0].status").value("CREATED"))
            .andExpect(jsonPath("$.rows[1].status").value("FAILED"))
            .andExpect(jsonPath("$.rows[2].status").value("CREATED"))
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("PHASE2-MIX-A", "PHASE2-MIX-C");
        assertThat(productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(STARTER_TENANT, "PHASE2-MIX-A")).isPresent();
        assertThat(productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(STARTER_TENANT, "PHASE2-MIX-B")).isEmpty();
        assertThat(productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(STARTER_TENANT, "PHASE2-MIX-C")).isPresent();
        assertThat(countTenantIdRows("inventory", STARTER_TENANT)).isEqualTo(inventoryBefore);
        assertThat(countTenantIdRows("customer_orders", STARTER_TENANT)).isEqualTo(orderBefore);
        assertThat(countTenantIdRows("alerts", STARTER_TENANT)).isEqualTo(alertBefore);
        assertThat(countTenantIdRows("recommendations", STARTER_TENANT)).isEqualTo(recommendationBefore);
    }

    @Test
    void duplicateCaseVariantInsideCsvCreatesOneProductAndOneFailure() throws Exception {
        String csv = """
            sku,name,category
            phase2-dup-100,First Duplicate,Verification
            PHASE2-DUP-100,Second Duplicate,Verification
            """;

        mockMvc.perform(multipart("/api/products/import")
                .file(new MockMultipartFile("file", "phase2-duplicate.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(1))
            .andExpect(jsonPath("$.failed").value(1))
            .andExpect(jsonPath("$.rows[1].status").value("FAILED"));

        assertThat(countProducts(STARTER_TENANT, "PHASE2-DUP-100")).isEqualTo(1);
    }

    @Test
    void existingProductCsvUpdateKeepsProductIdentityAndTenant() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"PHASE2-UPDATE-100\",\"name\":\"Before\",\"category\":\"Initial\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        long productId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        String csv = """
            sku,name,category
            PHASE2-UPDATE-100,After,Updated
            """;
        mockMvc.perform(multipart("/api/products/import")
                .file(new MockMultipartFile("file", "phase2-update.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.updated").value(1))
            .andExpect(jsonPath("$.rows[0].status").value("UPDATED"));

        Product updated = productRepository.findById(productId).orElseThrow();
        assertThat(updated.getId()).isEqualTo(productId);
        assertThat(tenantCodeForProduct(productId)).isEqualTo(STARTER_TENANT);
        assertThat(updated.getName()).isEqualTo("After");
        assertThat(updated.getCategory()).isEqualTo("Updated");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void finalBusinessEventFailureDoesNotReturnOrCommitSuccessfulRows() throws Exception {
        String csv = """
            sku,name,category
            PHASE2-ROLLBACK-100,Rollback Product,Verification
            """;

        int eventCountBefore = countRows("business_events", STARTER_TENANT);
        int auditCountBefore = jdbcTemplate.queryForObject(
            "select count(*) from audit_logs where tenant_code = ? and source = 'product-catalog'",
            Integer.class,
            STARTER_TENANT
        );
        int dispatchCountBefore = countRows("operational_dispatch_work_items", STARTER_TENANT);
        jdbcTemplate.execute("alter table audit_logs add constraint phase2_forced_catalog_audit_failure check (target_ref <> 'phase2-rollback.csv')");
        try {
            mockMvc.perform(multipart("/api/products/import")
                    .file(new MockMultipartFile("file", "phase2-rollback.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                    .header("X-Synapse-Tenant", STARTER_TENANT)
                    .with(accessHeaders("Operations Lead", "TENANT_ADMIN")))
                .andExpect(status().isConflict());

            assertThat(productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(STARTER_TENANT, "PHASE2-ROLLBACK-100")).isEmpty();
            assertThat(jdbcTemplate.queryForObject(
                "select count(*) from business_events where tenant_code = ?",
                Integer.class,
                STARTER_TENANT
            )).isEqualTo(eventCountBefore);
            assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where tenant_code = ? and source = 'product-catalog'",
                Integer.class,
                STARTER_TENANT
            )).isEqualTo(auditCountBefore);
            assertThat(jdbcTemplate.queryForObject(
                "select count(*) from operational_dispatch_work_items where tenant_code = ?",
                Integer.class,
                STARTER_TENANT
            )).isEqualTo(dispatchCountBefore);
        } finally {
            jdbcTemplate.execute("alter table audit_logs drop constraint phase2_forced_catalog_audit_failure");
        }
    }

    @Test
    void concurrentSameSkuCreationPersistsAtMostOneProduct() throws Exception {
        List<Integer> statuses = runConcurrentRequests("PHASE2-CONCURRENT-100", List.of("PHASE2-CONCURRENT-100", "PHASE2-CONCURRENT-100"), sku ->
            mockMvc.perform(post("/api/products")
                    .header("X-Synapse-Tenant", STARTER_TENANT)
                    .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                    .contentType(APPLICATION_JSON)
                    .content("{\"sku\":\"" + sku + "\",\"name\":\"Concurrent Product\",\"category\":\"Verification\"}"))
                .andReturn()
                .getResponse()
                .getStatus());

        assertThat(statuses).contains(201);
        assertThat(statuses.stream().filter(status -> status == 201)).hasSize(1);
        assertThat(statuses.stream().filter(status -> status == 409)).hasSize(1);
        assertThat(countProducts(STARTER_TENANT, "PHASE2-CONCURRENT-100")).isEqualTo(1);
    }

    @Test
    void concurrentCaseVariantCreationRemainsOneLogicalSku() throws Exception {
        List<Integer> statuses = runConcurrentRequests("PHASE2-CASE-100", List.of("phase2-case-100", "PHASE2-CASE-100"), sku ->
            mockMvc.perform(post("/api/products")
                    .header("X-Synapse-Tenant", STARTER_TENANT)
                    .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                    .contentType(APPLICATION_JSON)
                    .content("{\"sku\":\"" + sku + "\",\"name\":\"Case Product\",\"category\":\"Verification\"}"))
                .andReturn()
                .getResponse()
                .getStatus());

        assertThat(statuses).contains(201);
        assertThat(statuses.stream().filter(status -> status == 201)).hasSize(1);
        assertThat(statuses.stream().filter(status -> status == 409)).hasSize(1);
        assertThat(countProducts(STARTER_TENANT, "PHASE2-CASE-100")).isEqualTo(1);
    }

    @Test
    void concurrentUpdatesPreserveProductIdentityAndTenant() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"PHASE2-UPDATE-RACE-100\",\"name\":\"Initial\",\"category\":\"Verification\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        long productId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        List<Integer> statuses = runConcurrentRequests("PHASE2-UPDATE-RACE-100", List.of("Update Alpha", "Update Beta"), name ->
            mockMvc.perform(put("/api/products/" + productId)
                    .header("X-Synapse-Tenant", STARTER_TENANT)
                    .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                    .contentType(APPLICATION_JSON)
                    .content("{\"sku\":\"PHASE2-UPDATE-RACE-100\",\"name\":\"" + name + "\",\"category\":\"Verification\"}"))
                .andReturn()
                .getResponse()
                .getStatus());

        assertThat(statuses).containsOnly(200);
        Product updated = productRepository.findById(productId).orElseThrow();
        assertThat(tenantCodeForProduct(productId)).isEqualTo(STARTER_TENANT);
        assertThat(updated.getSku()).isEqualTo("STARTER-OPS::PHASE2-UPDATE-RACE-100");
        assertThat(updated.getName()).isIn("Update Alpha", "Update Beta");
    }

    @Test
    void orphanCompetitionCannotSwitchAnAlreadyOwnedProduct() throws Exception {
        provisionSecondTenant();
        jdbcTemplate.update(
            "insert into products (tenant_id, sku, catalog_sku, name, category, created_at, updated_at) values (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            null,
            "PHASE2-ORPHAN-COMPETE::PHASE2-ORPHAN-COMPETE-100",
            "PHASE2-ORPHAN-COMPETE-100",
            "Legacy Orphan",
            "Legacy"
        );
        Product orphan = productRepository.findBySku("PHASE2-ORPHAN-COMPETE::PHASE2-ORPHAN-COMPETE-100").orElseThrow();

        mockMvc.perform(post("/api/products")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"PHASE2-ORPHAN-COMPETE-100\",\"name\":\"Starter Owner\",\"category\":\"Verification\"}"))
            .andExpect(status().isCreated());

        Product adopted = productRepository.findById(orphan.getId()).orElseThrow();
        assertThat(tenantCodeForProduct(adopted.getId())).isEqualTo(STARTER_TENANT);

        mockMvc.perform(post("/api/products")
                .header("X-Synapse-Tenant", SECOND_TENANT)
                .with(accessHeaders("Operations Lead", "TENANT_ADMIN"))
                .contentType(APPLICATION_JSON)
                .content("{\"sku\":\"PHASE2-ORPHAN-COMPETE-100\",\"name\":\"Second Tenant Product\",\"category\":\"Verification\"}"))
            .andExpect(status().isCreated());

        Product original = productRepository.findById(orphan.getId()).orElseThrow();
        assertThat(tenantCodeForProduct(original.getId())).isEqualTo(STARTER_TENANT);
        assertThat(countProducts(STARTER_TENANT, "PHASE2-ORPHAN-COMPETE-100")).isEqualTo(1);
        assertThat(countProducts(SECOND_TENANT, "PHASE2-ORPHAN-COMPETE-100")).isEqualTo(1);
    }

    private int countProducts(String tenantCode, String catalogSku) {
        return jdbcTemplate.queryForObject(
            "select count(*) from products p join tenants t on t.id = p.tenant_id where upper(t.code) = upper(?) and upper(coalesce(p.catalog_sku, p.sku)) = upper(?)",
            Integer.class,
            tenantCode,
            catalogSku
        );
    }

    private void provisionSecondTenant() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", TEST_PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "PHASE2-SECOND-OPS",
                      "tenantName": "Phase 2 Second Operations",
                      "description": "Synthetic Catalog Phase 2 cross-tenant fixture.",
                      "adminFullName": "Phase 2 Tenant Administrator",
                      "adminUsername": "phase2.second.admin",
                      "adminPassword": "phase2-second-password",
                      "primaryLocation": "North Hub",
                      "secondaryLocation": "Coast Hub"
                    }
                    """))
            .andExpect(status().isOk());
    }

    private int countRows(String tableName, String tenantCode) {
        return jdbcTemplate.queryForObject(
            "select count(*) from " + tableName + " where tenant_code = ?",
            Integer.class,
            tenantCode
        );
    }

    private int countTenantIdRows(String tableName, String tenantCode) {
        return jdbcTemplate.queryForObject(
            "select count(*) from " + tableName + " r join tenants t on t.id = r.tenant_id where upper(t.code) = upper(?)",
            Integer.class,
            tenantCode
        );
    }

    private String tenantCodeForProduct(long productId) {
        return jdbcTemplate.queryForObject(
            "select t.code from products p join tenants t on t.id = p.tenant_id where p.id = ?",
            String.class,
            productId
        );
    }

    private List<Integer> runConcurrentRequests(String label, List<String> values, RequestCall requestCall) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(values.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Integer>> attempts = IntStream.range(0, values.size())
            .mapToObj(index -> (Callable<Integer>) () -> {
                start.await(10, TimeUnit.SECONDS);
                return requestCall.run(values.get(index));
            })
            .toList();
        List<Future<Integer>> futures = attempts.stream().map(executor::submit).toList();
        start.countDown();
        try {
            return futures.stream()
                .map(future -> {
                    try {
                        return future.get(20, TimeUnit.SECONDS);
                    } catch (Exception exception) {
                        throw new AssertionError("Concurrent Catalog request did not complete for " + label + ".", exception);
                    }
                })
                .toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private static RequestPostProcessor accessHeaders(String actorName, String roles) {
        return request -> {
            request.addHeader("X-Synapse-Actor", actorName);
            request.addHeader("X-Synapse-Roles", roles);
            return request;
        };
    }

    @FunctionalInterface
    private interface RequestCall {
        int run(String value) throws Exception;
    }
}
