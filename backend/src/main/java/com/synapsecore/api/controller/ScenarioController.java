package com.synapsecore.api.controller;

import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioReviewPriority;
import com.synapsecore.domain.entity.ScenarioRunType;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.access.AccessControlService;
import com.synapsecore.scenario.ScenarioAnalysisService;
import com.synapsecore.scenario.ScenarioExecutionService;
import com.synapsecore.scenario.ScenarioHistoryService;
import com.synapsecore.scenario.dto.ScenarioApprovalRequest;
import com.synapsecore.scenario.dto.ScenarioApprovalResponse;
import com.synapsecore.scenario.dto.ScenarioCompareRequest;
import com.synapsecore.scenario.dto.ScenarioComparisonResponse;
import com.synapsecore.scenario.dto.ScenarioEscalationAcknowledgementRequest;
import com.synapsecore.scenario.dto.ScenarioExecutionResponse;
import com.synapsecore.scenario.dto.ScenarioHistoryFilter;
import com.synapsecore.scenario.dto.ScenarioNotificationResponse;
import com.synapsecore.scenario.dto.ScenarioOrderImpactResponse;
import com.synapsecore.scenario.dto.ScenarioRejectionRequest;
import com.synapsecore.scenario.dto.ScenarioRejectionResponse;
import com.synapsecore.scenario.dto.ScenarioRequestResponse;
import com.synapsecore.scenario.dto.ScenarioRunResponse;
import com.synapsecore.scenario.dto.ScenarioSaveRequest;
import com.synapsecore.scenario.dto.ScenarioSaveResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final AccessControlService accessControlService;
    private final ScenarioAnalysisService scenarioAnalysisService;
    private final ScenarioHistoryService scenarioHistoryService;
    private final ScenarioExecutionService scenarioExecutionService;

    @PostMapping("/order-impact")
    @ResponseStatus(HttpStatus.OK)
    public ScenarioOrderImpactResponse analyzeOrderImpact(@Valid @RequestBody OrderCreateRequest request) {
        accessControlService.requireWorkspaceWarehouseAccess(request.warehouseCode(), "analyze scenario order impact");
        return scenarioAnalysisService.analyzeOrderImpact(request);
    }

    @PostMapping("/order-impact/compare")
    @ResponseStatus(HttpStatus.OK)
    public ScenarioComparisonResponse compareOrderImpact(@Valid @RequestBody ScenarioCompareRequest request) {
        accessControlService.requireWorkspaceWarehouseAccess(
            request.primary().warehouseCode(),
            "compare scenario order impact"
        );
        accessControlService.requireWorkspaceWarehouseAccess(
            request.alternative().warehouseCode(),
            "compare scenario order impact"
        );
        return scenarioAnalysisService.compareOrderImpact(request);
    }

    @PostMapping("/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ScenarioSaveResponse saveScenarioPlan(@Valid @RequestBody ScenarioSaveRequest request) {
        accessControlService.requireWorkspaceWarehouseAccess(
            request.request().warehouseCode(),
            "save scenario plans"
        );
        return scenarioHistoryService.savePlan(request);
    }

    @PostMapping("/{scenarioRunId}/approve")
    @ResponseStatus(HttpStatus.OK)
    public ScenarioApprovalResponse approveScenarioPlan(@PathVariable long scenarioRunId,
                                                        @Valid @RequestBody ScenarioApprovalRequest request) {
        return scenarioHistoryService.approvePlan(scenarioRunId, request);
    }

    @PostMapping("/{scenarioRunId}/reject")
    @ResponseStatus(HttpStatus.OK)
    public ScenarioRejectionResponse rejectScenarioPlan(@PathVariable long scenarioRunId,
                                                        @Valid @RequestBody ScenarioRejectionRequest request) {
        return scenarioHistoryService.rejectPlan(scenarioRunId, request);
    }

    @PostMapping("/{scenarioRunId}/acknowledge-escalation")
    @ResponseStatus(HttpStatus.OK)
    public ScenarioRunResponse acknowledgeEscalation(@PathVariable long scenarioRunId,
                                                     @Valid @RequestBody ScenarioEscalationAcknowledgementRequest request) {
        return scenarioHistoryService.acknowledgeSlaEscalation(scenarioRunId, request);
    }

    @PostMapping("/{scenarioRunId}/execute")
    @ResponseStatus(HttpStatus.OK)
    public ScenarioExecutionResponse executeScenario(@PathVariable long scenarioRunId) {
        accessControlService.requireWorkspaceAccess("execute approved scenarios");
        return scenarioExecutionService.execute(scenarioRunId);
    }

    @GetMapping("/{scenarioRunId}/request")
    public ScenarioRequestResponse getScenarioRequest(@PathVariable long scenarioRunId) {
        accessControlService.requireWorkspaceAccess("view saved scenario requests");
        return scenarioHistoryService.getScenarioRequest(scenarioRunId);
    }

    @GetMapping("/history")
    public List<ScenarioRunResponse> getScenarioHistory(@RequestParam(required = false) ScenarioRunType type,
                                                        @RequestParam(required = false) ScenarioApprovalStatus approvalStatus,
                                                        @RequestParam(required = false) ScenarioApprovalPolicy approvalPolicy,
                                                        @RequestParam(required = false) ScenarioApprovalStage approvalStage,
                                                        @RequestParam(required = false) String warehouseCode,
                                                        @RequestParam(required = false) String requestedBy,
                                                        @RequestParam(required = false) String reviewOwner,
                                                        @RequestParam(required = false) String finalApprovalOwner,
                                                        @RequestParam(required = false) ScenarioReviewPriority minimumReviewPriority,
                                                        @RequestParam(required = false) Boolean overdueOnly,
                                                        @RequestParam(required = false) Boolean slaEscalatedOnly,
                                                        @RequestParam(required = false) Boolean slaAcknowledged,
                                                        @RequestParam(required = false) Integer limit) {
        accessControlService.requireWorkspaceAccess("view scenario history");
        return scenarioHistoryService.getScenarioRuns(
            new ScenarioHistoryFilter(type, approvalStatus, approvalPolicy, approvalStage, warehouseCode, requestedBy, reviewOwner, finalApprovalOwner, minimumReviewPriority, overdueOnly, slaEscalatedOnly, slaAcknowledged, limit));
    }

    @GetMapping("/notifications")
    public List<ScenarioNotificationResponse> getScenarioNotifications(@RequestParam(required = false) Integer limit) {
        accessControlService.requireWorkspaceAccess("view scenario notifications");
        return scenarioHistoryService.getScenarioNotifications(limit == null ? 8 : limit);
    }
}
