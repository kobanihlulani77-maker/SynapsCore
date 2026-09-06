package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.access.dto.AccessOperatorResponse;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.config.SynapseStarterProperties;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioRun;
import com.synapsecore.domain.entity.ScenarioRunType;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.TenantOperationalPolicy;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.service.CoreIdentityWriteIsolationService;
import com.synapsecore.domain.service.IdentitySequenceMigrationService;
import com.synapsecore.domain.service.TenantOperationalPolicyService;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.scenario.ScenarioHistoryService;
import com.synapsecore.scenario.ScenarioSlaEscalationService;
import com.synapsecore.tenant.TenantContextService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

@DataJpaTest(showSql = false)
@Import({CoreIdentityWriteIsolationService.class, IdentitySequenceMigrationService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ScenarioSlaEscalationConcurrencyIntegrationTest {

    private ScenarioHistoryService history;
    @Autowired private ScenarioRunRepository runs;
    @Autowired private TenantRepository tenants;
    @Autowired private BusinessEventRepository events;
    @Autowired private CoreIdentityWriteIsolationService identityWrites;
    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JdbcTemplate jdbc;
    private ScenarioSlaEscalationService sla;
    private TenantContextService context;
    private Tenant tenant;
    private CyclicBarrier overlap;

    @BeforeEach
    void prepareTenant() {
        tenant = tenants.saveAndFlush(Tenant.builder().code("SLA-" + UUID.randomUUID())
            .name("SLA concurrency proof").active(true).build());
        context = new TenantContextService(null, null, null, null, null) {
            @Override public String getCurrentTenantCodeOrDefault() { return tenant.getCode(); }
        };
        AccessDirectoryService directory = new AccessDirectoryService(null, null, null, null, null) {
            @Override public List<AccessOperatorResponse> getActiveOperators(String tenantCode) {
                return List.of(operator("final.original", SynapseAccessRole.FINAL_APPROVER),
                    operator("final.alternate", SynapseAccessRole.FINAL_APPROVER),
                    operator("escalation.owner", SynapseAccessRole.ESCALATION_OWNER));
            }
        };
        TenantOperationalPolicyService policies = new TenantOperationalPolicyService(null, null, null) {
            private final ThreadLocal<Boolean> arrived = ThreadLocal.withInitial(() -> false);
            @Override public TenantOperationalPolicy getCurrentPolicy() {
                if (overlap != null && !arrived.get()) {
                    arrived.set(true);
                    try {
                        overlap.await(5, TimeUnit.SECONDS);
                    } catch (Exception exception) {
                        throw new IllegalStateException("Could not align readers", exception);
                    }
                }
                return TenantOperationalPolicy.builder().build();
            }
        };
        BusinessEventService businessEvents = new BusinessEventService(events, new RequestTraceContext(),
            context, identityWrites, publisher);
        sla = proxied(businessEvents);
        history = new ScenarioHistoryService(runs, new ObjectMapper().findAndRegisterModules(), null, null,
            businessEvents, null, directory, context, policies, new SynapseStarterProperties(), sla);
    }

    @Test
    void concurrentDetailHistoryAndNotificationReadsEscalateExactlyOnce() throws Exception {
        ScenarioRun plan = overduePlan();
        List<Long> operationalBefore = operationalCounts();
        // All three entrypoints have read the un-escalated row before resolving owners.
        overlap = new CyclicBarrier(3);

        var executor = Executors.newFixedThreadPool(3);
        try {
            List<Future<?>> readers = List.of(
                executor.submit(() -> history.getScenarioRun(plan.getId())),
                executor.submit(() -> history.getRecentScenarioRuns()),
                executor.submit(() -> history.getScenarioNotifications()));
            for (Future<?> reader : readers) {
                reader.get(15, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(escalationCount(plan)).isEqualTo(1);
        ScenarioRun after = runs.findById(plan.getId()).orElseThrow();
        assertThat(after.getSlaEscalatedAt()).isNotNull();
        assertThat(after.getSlaEscalatedTo()).isEqualTo("escalation.owner");
        assertThat(after.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        assertThat(after.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);
        assertThat(after.getApprovedAt()).isNull();
        overlap = null;
        history.getScenarioRun(plan.getId());
        history.getRecentScenarioRuns();
        history.getScenarioNotifications();
        assertThat(escalationCount(plan)).isEqualTo(1);
        assertThat(operationalCounts()).isEqualTo(operationalBefore);
    }

    @Test
    void eventFailureRollsBackTheMarkerAndAllowsOneCleanRetry() {
        ScenarioRun plan = overduePlan();
        BusinessEventService failingEvents = new BusinessEventService(events, new RequestTraceContext(),
            context, identityWrites, publisher) {
            @Override public void recordForTenant(String code, BusinessEventType type, String source, String summary) {
                super.recordForTenant(code, type, source, summary);
                throw new IllegalStateException("Event transaction failure");
            }
        };
        assertThatThrownBy(() -> escalate(proxied(failingEvents), plan, "WH-NORTH", "final.original"))
            .isInstanceOf(IllegalStateException.class).hasMessage("Event transaction failure");
        assertThat(runs.findById(plan.getId()).orElseThrow().getSlaEscalatedAt()).isNull();
        assertThat(escalationCount(plan)).isZero();
        escalate(sla, plan, "WH-NORTH", "final.original");
        assertThat(escalationCount(plan)).isEqualTo(1);
    }

    @Test
    void tenantWarehouseAndChangedAssignmentAreRecheckedBeforeMutation() {
        ScenarioRun plan = overduePlan();
        assertThatThrownBy(() -> sla.escalateIfEligible("OTHER-TENANT", plan.getId(), "WH-NORTH",
            "final.original", "final.alternate", "escalation.owner"))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(404));
        escalate(sla, plan, "WH-COAST", "final.original");
        escalate(sla, plan, "WH-NORTH", "stale.owner");
        assertThat(runs.findById(plan.getId()).orElseThrow().getSlaEscalatedAt()).isNull();
        assertThat(escalationCount(plan)).isZero();
    }

    @Test
    void changedDeadlineAndCompletedDecisionsAreNotEscalated() {
        ScenarioRun future = overduePlan();
        future.setApprovalDueAt(Instant.now().plusSeconds(60));
        runs.saveAndFlush(future);
        escalate(sla, future, "WH-NORTH", "final.original");
        assertThat(escalationCount(future)).isZero();
        for (ScenarioApprovalStatus status : List.of(ScenarioApprovalStatus.REJECTED, ScenarioApprovalStatus.APPROVED)) {
            ScenarioRun completed = overduePlan();
            completed.setApprovalStatus(status);
            runs.saveAndFlush(completed);
            escalate(sla, completed, "WH-NORTH", "final.original");
            ScenarioRun after = runs.findById(completed.getId()).orElseThrow();
            assertThat(after.getApprovalStatus()).isEqualTo(status);
            assertThat(after.getSlaEscalatedAt()).isNull();
            assertThat(escalationCount(completed)).isZero();
        }
    }

    private ScenarioRun escalate(ScenarioSlaEscalationService service, ScenarioRun plan,
                                 String warehouse, String expectedOwner) {
        return service.escalateIfEligible(tenant.getCode(), plan.getId(), warehouse,
            expectedOwner, "final.alternate", "escalation.owner");
    }

    private ScenarioSlaEscalationService proxied(BusinessEventService businessEvents) {
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(new ScenarioSlaEscalationService(runs, businessEvents));
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(interceptor);
        return (ScenarioSlaEscalationService) proxy.getProxy();
    }

    private List<Long> operationalCounts() {
        return List.of("customer_orders", "inventory", "fulfillment_tasks", "alerts", "recommendations",
                "operational_dispatch_work_items").stream()
            .map(table -> jdbc.queryForObject("select count(*) from " + table, Long.class)).toList();
    }

    private long escalationCount(ScenarioRun plan) {
        return events.findAll().stream()
            .filter(event -> tenant.getCode().equals(event.getTenantCode()))
            .filter(event -> event.getEventType() == BusinessEventType.SCENARIO_SLA_ESCALATED)
            .filter(event -> event.getPayloadSummary().contains(plan.getTitle()))
            .count();
    }

    private ScenarioRun overduePlan() {
        return runs.saveAndFlush(ScenarioRun.builder().tenant(tenant).type(ScenarioRunType.SAVED_PLAN)
            .title("Overdue " + UUID.randomUUID()).summary("Governance only").warehouseCode("WH-NORTH")
            .approvalPolicy(ScenarioApprovalPolicy.ESCALATED)
            .approvalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL)
            .approvalStatus(ScenarioApprovalStatus.PENDING_APPROVAL)
            .approvalDueAt(Instant.now().minusSeconds(60))
            .finalApprovalOwner("final.original").reviewOwner("review.owner").requestedBy("requester")
            .build());
    }

    private AccessOperatorResponse operator(String name, SynapseAccessRole role) {
        return new AccessOperatorResponse(null, tenant.getCode(), tenant.getName(), name, name,
            List.of(role), List.of("WH-NORTH"), true, "Test owner", Instant.now(), Instant.now(), 0L);
    }
}
