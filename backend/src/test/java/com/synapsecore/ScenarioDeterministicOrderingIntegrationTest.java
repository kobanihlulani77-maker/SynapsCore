package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioRun;
import com.synapsecore.domain.entity.ScenarioRunType;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.repository.TenantRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ScenarioDeterministicOrderingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScenarioRunRepository scenarioRunRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void scenarioHistoryUsesIdTieBreakerForFilteredAndBoundedReads() throws Exception {
        Tenant tenant = createTenant("PHASE4A-HISTORY");
        ScenarioRun first = savePreview(tenant, "History tie first", "WH-NORTH");
        ScenarioRun second = savePreview(tenant, "History tie second", "WH-NORTH");
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        forceCreatedAt(first, timestamp);
        forceCreatedAt(second, timestamp);

        List<Long> expectedOrder = List.of(second.getId(), first.getId());
        for (int attempt = 0; attempt < 5; attempt++) {
            String response = mockMvc.perform(get("/api/scenarios/history")
                    .header("X-Synapse-Tenant", tenant.getCode())
                    .param("type", "PREVIEW")
                    .param("warehouseCode", "WH-NORTH")
                    .param("limit", "2"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            assertThat(ids(response)).containsExactlyElementsOf(expectedOrder);
        }

        String boundedResponse = mockMvc.perform(get("/api/scenarios/history")
                .header("X-Synapse-Tenant", tenant.getCode())
                .param("type", "PREVIEW")
                .param("warehouseCode", "WH-NORTH")
                .param("limit", "1"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(ids(boundedResponse)).containsExactly(second.getId());
    }

    @Test
    void activeEscalationNotificationsUseIdTieBreakerBeforeTopTwelveCutoff() throws Exception {
        Tenant tenant = createTenant("PHASE4A-ACTIVE");
        Instant eventTimestamp = Instant.parse("2026-01-02T00:00:00Z");
        List<Long> createdIds = new ArrayList<>();

        IntStream.rangeClosed(1, 13).forEach(index -> createdIds.add(saveEscalatedPlan(
            tenant,
            "Active escalation tie " + index,
            eventTimestamp,
            null
        ).getId()));

        String response = mockMvc.perform(get("/api/scenarios/notifications")
                .header("X-Synapse-Tenant", tenant.getCode())
                .param("limit", "12"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        List<Long> expectedOrder = createdIds.reversed().subList(0, 12);
        assertThat(ids(response)).containsExactlyElementsOf(expectedOrder);
    }

    @Test
    void acknowledgedNotificationsUseIdTieBreaker() throws Exception {
        Tenant tenant = createTenant("PHASE4A-ACK");
        Instant eventTimestamp = Instant.parse("2026-01-03T00:00:00Z");
        ScenarioRun first = saveEscalatedPlan(tenant, "Acknowledged tie first", eventTimestamp, eventTimestamp);
        ScenarioRun second = saveEscalatedPlan(tenant, "Acknowledged tie second", eventTimestamp, eventTimestamp);

        String response = mockMvc.perform(get("/api/scenarios/notifications")
                .header("X-Synapse-Tenant", tenant.getCode())
                .param("limit", "2"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(ids(response)).containsExactly(second.getId(), first.getId());
    }

    @Test
    void mixedNotificationTimelineUsesEventTimeThenScenarioId() throws Exception {
        Tenant tenant = createTenant("PHASE4A-MIXED");
        Instant eventTimestamp = Instant.parse("2026-01-04T00:00:00Z");
        ScenarioRun active = saveEscalatedPlan(tenant, "Mixed active", eventTimestamp, null);
        ScenarioRun acknowledged = saveEscalatedPlan(tenant, "Mixed acknowledged", eventTimestamp, eventTimestamp);

        String response = mockMvc.perform(get("/api/scenarios/notifications")
                .header("X-Synapse-Tenant", tenant.getCode())
                .param("limit", "2"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(ids(response)).containsExactly(acknowledged.getId(), active.getId());
    }

    private Tenant createTenant(String code) {
        return tenantRepository.saveAndFlush(Tenant.builder()
            .code(code)
            .name(code + " Determinism")
            .description("Phase 4A deterministic ordering fixture.")
            .build());
    }

    private ScenarioRun savePreview(Tenant tenant, String title, String warehouseCode) {
        return scenarioRunRepository.saveAndFlush(ScenarioRun.builder()
            .tenant(tenant)
            .type(ScenarioRunType.PREVIEW)
            .title(title)
            .summary("Deterministic history fixture.")
            .warehouseCode(warehouseCode)
            .approvalStatus(ScenarioApprovalStatus.NOT_REQUIRED)
            .approvalPolicy(ScenarioApprovalPolicy.STANDARD)
            .approvalStage(ScenarioApprovalStage.NOT_REQUIRED)
            .build());
    }

    private ScenarioRun saveEscalatedPlan(Tenant tenant,
                                           String title,
                                           Instant escalatedAt,
                                           Instant acknowledgedAt) {
        return scenarioRunRepository.saveAndFlush(ScenarioRun.builder()
            .tenant(tenant)
            .type(ScenarioRunType.SAVED_PLAN)
            .title(title)
            .summary("Deterministic notification fixture.")
            .warehouseCode("WH-NORTH")
            .approvalStatus(ScenarioApprovalStatus.PENDING_APPROVAL)
            .approvalPolicy(ScenarioApprovalPolicy.ESCALATED)
            .approvalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL)
            .approvalDueAt(Instant.parse("2030-01-01T00:00:00Z"))
            .slaEscalatedTo("Phase 4A Escalation Owner")
            .slaEscalatedAt(escalatedAt)
            .slaAcknowledgedBy(acknowledgedAt == null ? null : "Phase 4A Escalation Owner")
            .slaAcknowledgedAt(acknowledgedAt)
            .build());
    }

    private void forceCreatedAt(ScenarioRun scenarioRun, Instant timestamp) {
        jdbcTemplate.update(
            "update scenario_runs set created_at = ? where id = ?",
            Timestamp.from(timestamp),
            scenarioRun.getId()
        );
    }

    private List<Long> ids(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        return StreamSupport.stream(root.spliterator(), false)
            .map(node -> node.path("scenarioRunId").isMissingNode()
                ? node.path("id").asLong()
                : node.path("scenarioRunId").asLong())
            .toList();
    }
}
