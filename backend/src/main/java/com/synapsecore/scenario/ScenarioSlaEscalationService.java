package com.synapsecore.scenario;

import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioRun;
import com.synapsecore.domain.entity.ScenarioRunType;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.event.BusinessEventService;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ScenarioSlaEscalationService {

    private final ScenarioRunRepository scenarioRunRepository;
    private final BusinessEventService businessEventService;

    @Transactional
    public ScenarioRun escalateIfEligible(String tenantCode, long scenarioId, String warehouseCode,
                                         String expectedFinalOwner, String finalOwner, String escalationOwner) {
        // Resolve candidate owners before this short transaction. Recheck the row
        // under its tenant-scoped lock, then commit its marker and event together.
        ScenarioRun run = scenarioRunRepository.findForSlaUpdate(tenantCode, scenarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Scenario not found: " + scenarioId));
        Instant now = Instant.now();
        if (run.getType() != ScenarioRunType.SAVED_PLAN
            || run.getApprovalStatus() != ScenarioApprovalStatus.PENDING_APPROVAL
            || run.getApprovalPolicy() != ScenarioApprovalPolicy.ESCALATED
            || run.getApprovalStage() != ScenarioApprovalStage.PENDING_FINAL_APPROVAL
            || run.getApprovalDueAt() == null || run.getApprovalDueAt().isAfter(now)
            || run.getSlaEscalatedAt() != null
            || !Objects.equals(run.getWarehouseCode(), warehouseCode)
            || !Objects.equals(run.getFinalApprovalOwner(), expectedFinalOwner)) {
            return run;
        }
        run.setFinalApprovalOwner(finalOwner);
        run.setSlaEscalatedTo(escalationOwner);
        run.setSlaEscalatedAt(now);
        scenarioRunRepository.save(run);
        businessEventService.recordForTenant(tenantCode, BusinessEventType.SCENARIO_SLA_ESCALATED,
            "scenario-planner", "Escalated overdue plan " + run.getTitle() + " from final approver "
                + expectedFinalOwner + " to " + finalOwner + " with escalation owner " + escalationOwner + ".");
        return run;
    }
}
