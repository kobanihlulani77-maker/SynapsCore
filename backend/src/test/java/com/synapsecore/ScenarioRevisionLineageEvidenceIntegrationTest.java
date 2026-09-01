package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.domain.entity.BusinessEvent;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioRun;
import com.synapsecore.domain.entity.ScenarioRunType;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ScenarioRevisionLineageEvidenceIntegrationTest {

    private static final String STARTER_TENANT = "STARTER-OPS";
    private static final String NORTH_WAREHOUSE = "WH-NORTH";
    private static final String REQUESTER = "Ayo Planner";
    private static final String REVIEW_OWNER = "Naledi Lead";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScenarioRunRepository scenarioRunRepository;

    @Autowired
    private BusinessEventRepository businessEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void sequentialSameParentSubmissionsAllowOnlyOneImmediateSuccessor() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        ScenarioRun source = createSavedPlan("Phase 4C sequential source " + suffix);
        ScenarioRun rejectedSource = reject(source.getId());
        String originalPayload = rejectedSource.getRequestPayload();
        String originalReason = rejectedSource.getRejectionReason();
        String originalRejectedBy = rejectedSource.getRejectedBy();

        ScenarioRun firstRevision = createRevision(source.getId(), "Phase 4C sequential branch A " + suffix);
        int eventsBeforeDuplicate = scenarioEvents(BusinessEventType.SCENARIO_RESUBMITTED).size();
        MvcResult duplicateAttempt = submitRevision(source.getId(), "Phase 4C sequential branch B " + suffix);

        assertRejectedSource(rejectedSource, source.getId());
        assertPendingRevision(firstRevision, source.getId(), 2);
        assertThat(duplicateAttempt.getResponse().getStatus()).isEqualTo(409);
        assertThat(duplicateAttempt.getResponse().getContentAsString())
            .contains("already been revised", firstRevision.getId().toString());

        List<ScenarioRun> persistedChildren = freshChildren(source.getId());
        assertThat(persistedChildren).hasSize(1);
        assertThat(persistedChildren)
            .extracting(ScenarioRun::getRevisionNumber)
            .containsExactly(2);
        assertThat(persistedChildren)
            .extracting(ScenarioRun::getRevisionOfScenarioRunId)
            .containsOnly(source.getId());

        assertThat(scenarioEvents(BusinessEventType.SCENARIO_RESUBMITTED)).hasSize(eventsBeforeDuplicate);
        ScenarioRun sourceAfterDuplicate = freshScenario(source.getId());
        assertThat(sourceAfterDuplicate.getRequestPayload()).isEqualTo(originalPayload);
        assertThat(sourceAfterDuplicate.getRejectionReason()).isEqualTo(originalReason);
        assertThat(sourceAfterDuplicate.getRejectedBy()).isEqualTo(originalRejectedBy);
    }

    @Test
    void rejectedRevisionCanFormARevisionThreeChainWithFreshGovernanceState() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        ScenarioRun source = createSavedPlan("Phase 4C chain source " + suffix);
        reject(source.getId());

        ScenarioRun secondRevision = createRevision(source.getId(), "Phase 4C chain revision two " + suffix);
        ScenarioRun rejectedSecondRevision = reject(secondRevision.getId());
        ScenarioRun thirdRevision = createRevision(secondRevision.getId(), "Phase 4C chain revision three " + suffix);

        assertThat(rejectedSecondRevision.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.REJECTED);
        assertThat(rejectedSecondRevision.getRevisionOfScenarioRunId()).isEqualTo(source.getId());
        assertThat(rejectedSecondRevision.getRevisionNumber()).isEqualTo(2);

        assertPendingRevision(thirdRevision, secondRevision.getId(), 3);
        assertThat(thirdRevision.getType()).isEqualTo(ScenarioRunType.SAVED_PLAN);
        assertThat(thirdRevision.getWarehouseCode()).isEqualTo(NORTH_WAREHOUSE);
        assertThat(thirdRevision.getRequestedBy()).isEqualTo(REQUESTER);
        assertThat(thirdRevision.getReviewOwner()).isEqualTo(REVIEW_OWNER);
        assertThat(thirdRevision.getFinalApprovalOwner()).isNotBlank();

        assertThat(lineageIds(thirdRevision.getId()))
            .containsExactly(source.getId(), secondRevision.getId(), thirdRevision.getId());
        assertThat(freshScenario(source.getId()).getRevisionOfScenarioRunId()).isNull();
        assertThat(freshScenario(secondRevision.getId()).getRevisionOfScenarioRunId()).isEqualTo(source.getId());
        assertThat(freshScenario(secondRevision.getId()).getApprovalStatus())
            .isEqualTo(ScenarioApprovalStatus.REJECTED);
    }

    @Test
    void concurrentSameParentSubmissionsAllowExactlyOneSuccessor() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        ScenarioRun source = createSavedPlan("Phase 4C concurrent source " + suffix);
        reject(source.getId());

        String firstTitle = "Phase 4C concurrent branch A " + suffix;
        String secondTitle = "Phase 4C concurrent branch B " + suffix;
        CyclicBarrier barrier = new CyclicBarrier(3);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RevisionAttempt> first = executor.submit(() -> submitConcurrentRevision(
                barrier, source.getId(), firstTitle));
            Future<RevisionAttempt> second = executor.submit(() -> submitConcurrentRevision(
                barrier, source.getId(), secondTitle));

            barrier.await(10, TimeUnit.SECONDS);
            RevisionAttempt firstAttempt = first.get(30, TimeUnit.SECONDS);
            RevisionAttempt secondAttempt = second.get(30, TimeUnit.SECONDS);

            assertThat(firstAttempt.failure()).isNull();
            assertThat(secondAttempt.failure()).isNull();
            assertThat(List.of(firstAttempt.status(), secondAttempt.status()))
                .containsExactlyInAnyOrder(201, 409);

            List<ScenarioRun> persistedChildren = freshChildren(source.getId());
            assertThat(persistedChildren).hasSize(1);
            assertThat(persistedChildren)
                .extracting(ScenarioRun::getRevisionNumber)
                .containsExactly(2);
            assertThat(persistedChildren)
                .extracting(ScenarioRun::getRevisionOfScenarioRunId)
                .containsOnly(source.getId());
            assertThat(persistedChildren)
                .extracting(ScenarioRun::getId)
                .doesNotHaveDuplicates();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void databaseConstraintRejectsASecondSuccessorOutsideTheApplicationPath() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        ScenarioRun source = createSavedPlan("Phase 4C direct database source " + suffix);
        reject(source.getId());
        ScenarioRun firstRevision = createRevision(source.getId(), "Phase 4C direct database child " + suffix);
        Long tenantId = jdbcTemplate.queryForObject(
            "select tenant_id from scenario_runs where id = ?", Long.class, firstRevision.getId());

        assertThatThrownBy(() -> jdbcTemplate.update("""
            insert into scenario_runs (
                tenant_id, type, title, summary, approval_status, approval_policy, approval_stage,
                warehouse_code, revision_of_scenario_run_id, revision_number, created_at
            ) values (?, 'SAVED_PLAN', ?, 'Direct database duplicate successor',
                'PENDING_APPROVAL', 'STANDARD', 'PENDING_REVIEW', ?, ?, 2, current_timestamp)
            """, tenantId, "Phase 4C duplicate child " + suffix, NORTH_WAREHOUSE, source.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(freshChildren(source.getId())).hasSize(1);
    }

    @Test
    void independentRevisionFamiliesMayEachCreateTheirOwnSecondRevision() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        ScenarioRun firstSource = createSavedPlan("Phase 4C family A source " + suffix);
        reject(firstSource.getId());
        ScenarioRun firstChild = createRevision(firstSource.getId(), "Phase 4C family A child " + suffix);

        ScenarioRun secondSource = createSavedPlan("Phase 4C family B source " + suffix);
        reject(secondSource.getId());
        ScenarioRun secondChild = createRevision(secondSource.getId(), "Phase 4C family B child " + suffix);

        assertPendingRevision(firstChild, firstSource.getId(), 2);
        assertPendingRevision(secondChild, secondSource.getId(), 2);
        assertThat(freshChildren(firstSource.getId())).hasSize(1);
        assertThat(freshChildren(secondSource.getId())).hasSize(1);
    }

    @Test
    void revisionMustRemainInTheSourceWarehouseWhileASeparateWarehouseMayStartItsOwnPlan() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        ScenarioRun northSource = createSavedPlan("Phase 4C warehouse source " + suffix);
        reject(northSource.getId());

        MvcResult crossWarehouseAttempt = submitRevision(
            northSource.getId(), "Phase 4C cross warehouse revision " + suffix, "WH-COAST");
        assertThat(crossWarehouseAttempt.getResponse().getStatus()).isEqualTo(409);
        assertThat(freshChildren(northSource.getId())).isEmpty();

        ScenarioRun independentCoastPlan = createSavedPlan(
            "Phase 4C independent coast plan " + suffix, "WH-COAST");
        assertThat(independentCoastPlan.getRevisionOfScenarioRunId()).isNull();
        assertThat(independentCoastPlan.getRevisionNumber()).isEqualTo(1);
        assertThat(independentCoastPlan.getWarehouseCode()).isEqualTo("WH-COAST");
    }

    private ScenarioRun createSavedPlan(String title) throws Exception {
        return createSavedPlan(title, NORTH_WAREHOUSE);
    }

    private ScenarioRun createSavedPlan(String title, String warehouseCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/scenarios/save")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .contentType(APPLICATION_JSON)
                .content(scenarioPayload(title, null, warehouseCode)))
            .andExpect(status().isCreated())
            .andReturn();
        return freshScenario(scenarioId(result));
    }

    private ScenarioRun createRevision(long sourceId, String title) throws Exception {
        return freshScenario(scenarioId(submitRevision(sourceId, title)));
    }

    private MvcResult submitRevision(long sourceId, String title) throws Exception {
        return submitRevision(sourceId, title, NORTH_WAREHOUSE);
    }

    private MvcResult submitRevision(long sourceId, String title, String warehouseCode) throws Exception {
        return mockMvc.perform(post("/api/scenarios/save")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .contentType(APPLICATION_JSON)
                .content(scenarioPayload(title, sourceId, warehouseCode)))
            .andReturn();
    }

    private ScenarioRun reject(long scenarioId) throws Exception {
        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/reject")
                .header("X-Synapse-Tenant", STARTER_TENANT)
                .with(accessHeaders(REVIEW_OWNER, "REVIEW_OWNER"))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorRole": "REVIEW_OWNER",
                      "reviewerName": "Naledi Lead",
                      "reason": "Phase 4C revision lineage evidence rejection."
                    }
                    """))
            .andExpect(status().isOk());
        return freshScenario(scenarioId);
    }

    private RevisionAttempt submitConcurrentRevision(CyclicBarrier barrier, long sourceId, String title)
        throws Exception {
        barrier.await(10, TimeUnit.SECONDS);
        try {
            MvcResult result = mockMvc.perform(post("/api/scenarios/save")
                    .header("X-Synapse-Tenant", STARTER_TENANT)
                    .contentType(APPLICATION_JSON)
                    .content(scenarioPayload(title, sourceId)))
                .andReturn();
            return new RevisionAttempt(
                title,
                result.getResponse().getStatus(),
                result.getResponse().getStatus() == 201 ? scenarioId(result) : null,
                null
            );
        } catch (Exception exception) {
            return new RevisionAttempt(title, -1, null, exception);
        }
    }

    private String scenarioPayload(String title, Long revisionOfScenarioRunId) {
        return scenarioPayload(title, revisionOfScenarioRunId, NORTH_WAREHOUSE);
    }

    private String scenarioPayload(String title, Long revisionOfScenarioRunId, String warehouseCode) {
        String revisionField = revisionOfScenarioRunId == null
            ? "null"
            : revisionOfScenarioRunId.toString();
        return """
            {
              "title": "%s",
              "requestedBy": "%s",
              "reviewOwner": "%s",
              "revisionOfScenarioRunId": %s,
              "request": {
              "warehouseCode": "%s",
                "items": [
                  {
                    "productSku": "SKU-FLX-100",
                    "quantity": 2,
                    "unitPrice": 95.00
                  }
                ]
              }
            }
            """.formatted(title, REQUESTER, REVIEW_OWNER, revisionField, warehouseCode);
    }

    private long scenarioId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("scenarioRunId").asLong();
    }

    private ScenarioRun freshScenario(long scenarioId) {
        entityManager.clear();
        return scenarioRunRepository.findById(scenarioId).orElseThrow();
    }

    private List<ScenarioRun> freshChildren(long parentId) {
        entityManager.clear();
        return scenarioRunRepository.findAll().stream()
            .filter(run -> Long.valueOf(parentId).equals(run.getRevisionOfScenarioRunId()))
            .toList();
    }

    private List<Long> lineageIds(long scenarioId) {
        List<Long> lineage = new java.util.ArrayList<>();
        ScenarioRun current = freshScenario(scenarioId);
        while (current != null) {
            lineage.add(0, current.getId());
            Long parentId = current.getRevisionOfScenarioRunId();
            current = parentId == null ? null : freshScenario(parentId);
        }
        return lineage;
    }

    private List<BusinessEvent> scenarioEvents(BusinessEventType eventType) {
        return businessEventRepository.findAll().stream()
            .filter(event -> STARTER_TENANT.equalsIgnoreCase(event.getTenantCode()))
            .filter(event -> event.getEventType() == eventType)
            .toList();
    }

    private void assertRejectedSource(ScenarioRun scenarioRun, long sourceId) {
        assertThat(scenarioRun.getId()).isEqualTo(sourceId);
        assertThat(scenarioRun.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.REJECTED);
        assertThat(scenarioRun.getApprovalStage()).isEqualTo(ScenarioApprovalStage.REJECTED);
    }

    private void assertPendingRevision(ScenarioRun scenarioRun, long parentId, int revisionNumber) {
        assertThat(scenarioRun.getType()).isEqualTo(ScenarioRunType.SAVED_PLAN);
        assertThat(scenarioRun.getRevisionOfScenarioRunId()).isEqualTo(parentId);
        assertThat(scenarioRun.getRevisionNumber()).isEqualTo(revisionNumber);
        assertThat(scenarioRun.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);
        assertThat(scenarioRun.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_REVIEW);
        assertThat(scenarioRun.getRequestedBy()).isEqualTo(REQUESTER);
        assertThat(scenarioRun.getReviewOwner()).isEqualTo(REVIEW_OWNER);
    }

    private static RequestPostProcessor accessHeaders(String actorName, String roles) {
        return request -> {
            request.addHeader("X-Synapse-Actor", actorName);
            request.addHeader("X-Synapse-Roles", roles);
            return request;
        };
    }

    private record RevisionAttempt(String title, int status, Long scenarioId, Throwable failure) {
    }
}
