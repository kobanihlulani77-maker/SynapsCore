package com.synapsecore.decision;

import com.synapsecore.audit.AuditLogService;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Records safe, tenant-qualified evidence for the scheduled reconciliation pass.
 * Audit rows are used so the existing authenticated tenant activity surface can
 * expose the evidence without introducing a diagnostics endpoint or a schema.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationReconciliationEvidenceService {

    public static final String SCHEDULED_TRIGGER = "SCHEDULED";
    public static final String COMPLETED = "COMPLETED";
    public static final String COMPLETED_WITH_FAILURES = "COMPLETED_WITH_FAILURES";
    public static final String FAILED = "FAILED";

    private static final String SYSTEM_ACTOR = "system-scheduler";
    private static final String SOURCE = "scheduler/recommendation-reconciliation";
    private static final String TARGET_TYPE = "RecommendationSchedulerRun";
    private static final String START_ACTION = "RECOMMENDATION_RECONCILIATION_STARTED";
    private static final String COMPLETE_ACTION = "RECOMMENDATION_RECONCILIATION_COMPLETED";

    private final AuditLogService auditLogService;

    public void recordStarted(String runId, Instant startedAt, Collection<String> tenantCodes) {
        tenantCodes.stream()
            .filter(this::isUsableTenantCode)
            .distinct()
            .sorted(Comparator.naturalOrder())
            .forEach(tenantCode -> safelyRecordStarted(runId, startedAt, tenantCode));
    }

    public void recordCompleted(String runId,
                                Instant startedAt,
                                Instant completedAt,
                                String outcome,
                                Map<String, ReconciliationTenantCounts> countsByTenant) {
        countsByTenant.entrySet().stream()
            .filter(entry -> isUsableTenantCode(entry.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> safelyRecordCompleted(
                runId,
                startedAt,
                completedAt,
                outcome,
                entry.getKey(),
                entry.getValue()
            ));
    }

    private void safelyRecordStarted(String runId, Instant startedAt, String tenantCode) {
        try {
            auditLogService.recordSuccessForTenant(
                tenantCode,
                START_ACTION,
                SYSTEM_ACTOR,
                SOURCE,
                TARGET_TYPE,
                runId,
                "runId=" + runId
                    + "|trigger=" + SCHEDULED_TRIGGER
                    + "|status=STARTED"
                    + "|startedAt=" + startedAt
            );
        } catch (RuntimeException exception) {
            log.warn("Could not record recommendation scheduler start evidence for tenant {} run {}: {}",
                tenantCode, runId, exception.getMessage());
        }
    }

    private void safelyRecordCompleted(String runId,
                                       Instant startedAt,
                                       Instant completedAt,
                                       String outcome,
                                       String tenantCode,
                                       ReconciliationTenantCounts counts) {
        String details = "runId=" + runId
            + "|trigger=" + SCHEDULED_TRIGGER
            + "|status=COMPLETED"
            + "|outcome=" + outcome
            + "|startedAt=" + startedAt
            + "|completedAt=" + completedAt
            + "|durationMs=" + Duration.between(startedAt, completedAt).toMillis()
            + "|inventory=" + counts.inventorySummary()
            + "|fulfillment=" + counts.fulfillmentSummary()
            + "|retirements=" + counts.retirementSummary();
        try {
            if (FAILED.equals(outcome)) {
                auditLogService.recordFailureForTenant(
                    tenantCode,
                    COMPLETE_ACTION,
                    SYSTEM_ACTOR,
                    SOURCE,
                    TARGET_TYPE,
                    runId,
                    details
                );
            } else {
                auditLogService.recordSuccessForTenant(
                    tenantCode,
                    COMPLETE_ACTION,
                    SYSTEM_ACTOR,
                    SOURCE,
                    TARGET_TYPE,
                    runId,
                    details
                );
            }
        } catch (RuntimeException exception) {
            log.warn("Could not record recommendation scheduler completion evidence for tenant {} run {}: {}",
                tenantCode, runId, exception.getMessage());
        }
    }

    private boolean isUsableTenantCode(String tenantCode) {
        return tenantCode != null && !tenantCode.isBlank();
    }

    public record ReconciliationTenantCounts(
        int inventoryAttempted,
        int inventorySucceeded,
        int inventoryFailed,
        int fulfillmentAttempted,
        int fulfillmentSucceeded,
        int fulfillmentFailed,
        int retirementsAttempted,
        int retirementsSucceeded,
        int retirementsFailed
    ) {

        public String inventorySummary() {
            return summary(inventoryAttempted, inventorySucceeded, inventoryFailed);
        }

        public String fulfillmentSummary() {
            return summary(fulfillmentAttempted, fulfillmentSucceeded, fulfillmentFailed);
        }

        public String retirementSummary() {
            return summary(retirementsAttempted, retirementsSucceeded, retirementsFailed);
        }

        private String summary(int attempted, int succeeded, int failed) {
            return "attempted:" + attempted + ",succeeded:" + succeeded + ",failed:" + failed;
        }
    }
}
