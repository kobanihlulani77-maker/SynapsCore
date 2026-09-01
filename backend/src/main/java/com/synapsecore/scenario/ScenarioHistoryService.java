package com.synapsecore.scenario;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.access.dto.AccessOperatorResponse;
import com.synapsecore.config.SynapseStarterProperties;
import com.synapsecore.domain.entity.ScenarioRun;
import com.synapsecore.domain.entity.ScenarioRunType;
import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioReviewPriority;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.service.TenantOperationalPolicyService;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.scenario.dto.ScenarioApprovalRequest;
import com.synapsecore.scenario.dto.ScenarioApprovalResponse;
import com.synapsecore.scenario.dto.ScenarioComparisonResponse;
import com.synapsecore.scenario.dto.ScenarioEscalationAcknowledgementRequest;
import com.synapsecore.scenario.dto.ScenarioHistoryFilter;
import com.synapsecore.scenario.dto.ScenarioNotificationResponse;
import com.synapsecore.scenario.dto.ScenarioNotificationType;
import com.synapsecore.scenario.dto.ScenarioOrderImpactResponse;
import com.synapsecore.scenario.dto.ScenarioRejectionRequest;
import com.synapsecore.scenario.dto.ScenarioRejectionResponse;
import com.synapsecore.scenario.dto.ScenarioRequestResponse;
import com.synapsecore.scenario.dto.ScenarioRunResponse;
import com.synapsecore.scenario.dto.ScenarioSaveRequest;
import com.synapsecore.scenario.dto.ScenarioSaveResponse;
import com.synapsecore.tenant.TenantContextService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ScenarioHistoryService {

    private static final int DEFAULT_NOTIFICATION_LIMIT = 8;

    private final ScenarioRunRepository scenarioRunRepository;
    private final ObjectMapper objectMapper;
    private final ScenarioProjectionService scenarioProjectionService;
    private final ScenarioRiskPolicyService scenarioRiskPolicyService;
    private final BusinessEventService businessEventService;
    private final AccessControlService accessControlService;
    private final AccessDirectoryService accessDirectoryService;
    private final TenantContextService tenantContextService;
    private final TenantOperationalPolicyService tenantOperationalPolicyService;
    private final SynapseStarterProperties starterProperties;

    private static final ScenarioHistoryFilter DEFAULT_HISTORY_FILTER =
        new ScenarioHistoryFilter(null, null, null, null, null, null, null, null, null, null, null, null, null);

    @Transactional
    public ScenarioSaveResponse savePlan(ScenarioSaveRequest request, String authenticatedActorName) {
        var tenant = tenantContextService.getCurrentTenantOrDefault();
        ScenarioRun revisionSource = resolveRevisionSource(request.revisionOfScenarioRunId());
        String warehouseCode = request.request().warehouseCode().trim();
        requireSameRevisionWarehouse(revisionSource, warehouseCode);
        ScenarioOrderImpactResponse projectedImpact = scenarioProjectionService.projectOrderImpact(request.request());
        ScenarioRiskAssessment riskAssessment = scenarioRiskPolicyService.assess(projectedImpact);
        ScenarioApprovalPolicy approvalPolicy = determineApprovalPolicy(riskAssessment.reviewPriority());
        String requestedBy = resolveRequestedBy(request.requestedBy(), warehouseCode, authenticatedActorName);
        String reviewOwner = resolveReviewOwner(request.reviewOwner(), warehouseCode, requestedBy);
        requireDistinctRequesterAndReviewer(requestedBy, reviewOwner, "save");
        if (revisionSource != null) {
            revisionSource = lockRevisionSource(tenant.getCode(), revisionSource.getId());
            requireSameRevisionWarehouse(revisionSource, warehouseCode);
            Long revisionSourceId = revisionSource.getId();
            scenarioRunRepository.findFirstByTenant_CodeIgnoreCaseAndRevisionOfScenarioRunIdOrderByIdAsc(
                    tenant.getCode(), revisionSourceId)
                .ifPresent(existingChild -> {
                    throw revisionConflict(revisionSourceId, existingChild.getId());
                });
        }

        ScenarioRun plannedRun = ScenarioRun.builder()
            .tenant(tenant)
            .type(ScenarioRunType.SAVED_PLAN)
            .title(request.title().trim())
            .summary(buildSavedPlanSummary(request.request(), projectedImpact, riskAssessment))
            .recommendedOption(buildRecommendedOption(projectedImpact))
            .warehouseCode(warehouseCode)
            .requestPayload(serializeRequest(request.request()))
            .approvalStatus(ScenarioApprovalStatus.PENDING_APPROVAL)
            .approvalPolicy(approvalPolicy)
            .approvalStage(ScenarioApprovalStage.PENDING_REVIEW)
            .reviewPriority(riskAssessment.reviewPriority())
            .riskScore(riskAssessment.score())
            .requestedBy(requestedBy)
            .reviewOwner(reviewOwner)
            .finalApprovalOwner(resolveFinalApprovalOwner(warehouseCode, reviewOwner))
            .approvalDueAt(resolveApprovalDueAt(approvalPolicy, ScenarioApprovalStage.PENDING_REVIEW, riskAssessment.reviewPriority()))
            .revisionOfScenarioRunId(revisionSource == null ? null : revisionSource.getId())
            .revisionNumber(revisionSource == null ? 1 : nextRevisionNumber(revisionSource))
            .build();
        ScenarioRun scenarioRun = savePlanRecord(plannedRun, revisionSource);

        if (revisionSource == null) {
            businessEventService.record(
                BusinessEventType.SCENARIO_SAVED,
                "scenario-planner",
                "Saved plan " + scenarioRun.getTitle() + " for warehouse " + scenarioRun.getWarehouseCode()
                    + " with " + scenarioRun.getReviewPriority() + " review priority (score "
                    + scenarioRun.getRiskScore() + ")."
            );
        } else {
            businessEventService.record(
                BusinessEventType.SCENARIO_RESUBMITTED,
                "scenario-planner",
                "Resubmitted rejected plan " + revisionSource.getTitle() + " as revision "
                    + scenarioRun.getRevisionNumber() + " (" + scenarioRun.getTitle() + ") with "
                    + scenarioRun.getReviewPriority() + " review priority (score "
                    + scenarioRun.getRiskScore() + ")."
            );
        }

        return new ScenarioSaveResponse(
            scenarioRun.getId(),
            scenarioRun.getType(),
            scenarioRun.getTitle(),
            scenarioRun.getWarehouseCode(),
            scenarioRun.getRequestedBy(),
            scenarioRun.getReviewOwner(),
            scenarioRun.getFinalApprovalOwner(),
            scenarioRun.getReviewPriority(),
            scenarioRun.getRiskScore(),
            scenarioRun.getApprovalPolicy(),
            scenarioRun.getApprovalStage(),
            scenarioRun.getRevisionOfScenarioRunId(),
            scenarioRun.getRevisionNumber(),
            false,
            scenarioRun.getApprovalStatus(),
            scenarioRun.getApprovalDueAt(),
            scenarioRun.getSlaEscalatedTo(),
            scenarioRun.getSlaEscalatedAt(),
            isSlaEscalated(scenarioRun),
            isOverdue(scenarioRun),
            scenarioRun.getCreatedAt()
        );
    }

    private ScenarioRun savePlanRecord(ScenarioRun plannedRun, ScenarioRun revisionSource) {
        try {
            return scenarioRunRepository.saveAndFlush(plannedRun);
        } catch (DataIntegrityViolationException exception) {
            if (revisionSource == null || !isRevisionLineageConstraint(exception)) {
                throw exception;
            }
            Long existingChildId = scenarioRunRepository
                .findFirstByTenant_CodeIgnoreCaseAndRevisionOfScenarioRunIdOrderByIdAsc(
                    revisionSource.getTenant().getCode(), revisionSource.getId())
                .map(ScenarioRun::getId)
                .orElse(null);
            throw revisionConflict(revisionSource.getId(), existingChildId);
        }
    }

    public ScenarioRun recordPreview(OrderCreateRequest request, ScenarioOrderImpactResponse response) {
        int lowStockItems = (int) response.projectedInventory().stream().filter(item -> item.lowStock()).count();
        ScenarioRiskAssessment riskAssessment = scenarioRiskPolicyService.assess(response);
        return scenarioRunRepository.save(ScenarioRun.builder()
            .tenant(tenantContextService.getCurrentTenantOrDefault())
            .type(ScenarioRunType.PREVIEW)
            .title("Order impact preview for " + response.warehouseCode())
            .summary("Projected " + response.totalUnits() + " units worth " + response.projectedOrderValue()
                + " with " + lowStockItems + " low-stock items, "
                + response.projectedAlerts().size() + " alerts, and "
                + response.projectedRecommendations().size() + " recommendations.")
            .recommendedOption(response.projectedRecommendations().isEmpty()
                ? "No action needed"
                : response.projectedRecommendations().get(0).title())
            .warehouseCode(response.warehouseCode())
            .requestPayload(serializeRequest(request))
            .approvalStatus(ScenarioApprovalStatus.NOT_REQUIRED)
            .approvalPolicy(ScenarioApprovalPolicy.STANDARD)
            .approvalStage(ScenarioApprovalStage.NOT_REQUIRED)
            .reviewPriority(riskAssessment.reviewPriority())
            .riskScore(riskAssessment.score())
            .build());
    }

    public ScenarioRun recordComparison(ScenarioComparisonResponse response) {
        return scenarioRunRepository.save(ScenarioRun.builder()
            .tenant(tenantContextService.getCurrentTenantOrDefault())
            .type(ScenarioRunType.COMPARISON)
            .title("Scenario comparison: " + response.primaryLabel() + " vs " + response.alternativeLabel())
            .summary(response.summary().rationale())
            .recommendedOption(response.summary().recommendedOption())
            .approvalStatus(ScenarioApprovalStatus.NOT_REQUIRED)
            .approvalPolicy(ScenarioApprovalPolicy.STANDARD)
            .approvalStage(ScenarioApprovalStage.NOT_REQUIRED)
            .build());
    }

    public ScenarioRun getScenarioRun(long scenarioRunId) {
        var currentOperator = accessDirectoryService.getCurrentOperator();
        if (currentOperator.isEmpty() && !starterProperties.isAllowDefaultTenantFallback()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "A signed-in user session is required to access scenario " + scenarioRunId + ".");
        }
        ScenarioRun run = scenarioRunRepository.findByTenant_CodeIgnoreCaseAndId(
            tenantContextService.getCurrentTenantCodeOrDefault(),
            scenarioRunId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Scenario not found: " + scenarioRunId));
        if (currentOperator.isPresent()) {
            accessDirectoryService.requireWarehouseAccess(
                currentOperator.get(),
                run.getWarehouseCode(),
                "access scenario " + run.getId());
        }
        return applySlaEscalationIfNeeded(run);
    }

    public ScenarioRequestResponse getScenarioRequest(long scenarioRunId) {
        ScenarioRun run = getScenarioRun(scenarioRunId);
        return new ScenarioRequestResponse(
            run.getId(),
            run.getTitle(),
            getLoadableOrderRequest(run),
            Instant.now()
        );
    }

    public ScenarioApprovalResponse approvePlan(long scenarioRunId, ScenarioApprovalRequest request) {
        ScenarioRun run = getScenarioRun(scenarioRunId);
        if (run.getType() != ScenarioRunType.SAVED_PLAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " cannot be approved. Only saved plans require approval.");
        }

        if (run.getApprovalStatus() == ScenarioApprovalStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " has already been rejected. Reload it into the planner and save a new plan before requesting approval again.");
        }

        if (run.getApprovalStatus() == ScenarioApprovalStatus.APPROVED) {
            requireApprovedApprovalRetryAuthority(run, request);
        } else {
            if (run.getApprovalStatus() != ScenarioApprovalStatus.PENDING_APPROVAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Scenario " + run.getId() + " cannot be approved from its current workflow state.");
            }
            if (run.getApprovalPolicy() == ScenarioApprovalPolicy.STANDARD
                && run.getApprovalStage() != ScenarioApprovalStage.PENDING_REVIEW) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Scenario " + run.getId() + " requires the pending review stage before approval.");
            }
            if (run.getApprovalPolicy() == ScenarioApprovalPolicy.ESCALATED
                && run.getApprovalStage() != ScenarioApprovalStage.PENDING_REVIEW
                && run.getApprovalStage() != ScenarioApprovalStage.PENDING_FINAL_APPROVAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Scenario " + run.getId() + " cannot be approved from its current workflow stage.");
            }
            String trimmedApproverName = request.approverName().trim();
            String approvalNote = request.approvalNote() == null ? "" : request.approvalNote().trim();
            accessControlService.requireScenarioActor(request.actorRole(), trimmedApproverName, "approve scenario plans");
            requireActorRole(run.getId(), "approve this scenario", request.actorRole(), requiredApprovalRole(run));
            requireWarehouseAccess(trimmedApproverName, run.getWarehouseCode(), "approve scenario plans");
            if (run.getApprovalPolicy() == ScenarioApprovalPolicy.ESCALATED) {
                run = processEscalatedApproval(run, trimmedApproverName, approvalNote);
            } else {
                requireDistinctRequesterAndReviewer(run.getRequestedBy(), trimmedApproverName, "approve");
                requireAssignedReviewOwner(run, trimmedApproverName, "approve");
                run.setApprovalStatus(ScenarioApprovalStatus.APPROVED);
                run.setApprovalStage(ScenarioApprovalStage.APPROVED);
                run.setApprovedBy(trimmedApproverName);
                run.setApprovedAt(Instant.now());
                run.setApprovalNote(approvalNote.isBlank() ? null : approvalNote);
                run.setApprovalDueAt(null);
                run.setRejectedBy(null);
                run.setRejectedAt(null);
                run.setRejectionReason(null);
                run = scenarioRunRepository.save(run);

                businessEventService.record(
                    BusinessEventType.SCENARIO_APPROVED,
                    "scenario-planner",
                    "Approved saved plan " + run.getTitle() + " for warehouse " + run.getWarehouseCode()
                        + " by " + run.getApprovedBy() + "."
                );
            }
        }

        return new ScenarioApprovalResponse(
            run.getId(),
            run.getTitle(),
            run.getApprovalStatus(),
            run.getApprovalPolicy(),
            run.getApprovalStage(),
            run.getFinalApprovalOwner(),
            run.getReviewApprovedBy(),
            run.getReviewApprovedAt(),
            run.getApprovedBy(),
            run.getApprovalNote(),
            run.getApprovedAt(),
            run.getApprovalDueAt(),
            run.getSlaEscalatedTo(),
            run.getSlaEscalatedAt(),
            isSlaEscalated(run),
            isOverdue(run),
            false
        );
    }

    public ScenarioRejectionResponse rejectPlan(long scenarioRunId, ScenarioRejectionRequest request) {
        ScenarioRun run = getScenarioRun(scenarioRunId);
        if (run.getType() != ScenarioRunType.SAVED_PLAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " cannot be rejected. Only saved plans go through review.");
        }

        if (run.getApprovalStatus() == ScenarioApprovalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " has already been approved and cannot be rejected.");
        }

        if (run.getApprovalStatus() == ScenarioApprovalStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " has already been rejected and cannot be rejected again.");
        }

        if (run.getApprovalStatus() != ScenarioApprovalStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " cannot be rejected from its current workflow state.");
        }
        if (run.getApprovalPolicy() == ScenarioApprovalPolicy.STANDARD
            && run.getApprovalStage() != ScenarioApprovalStage.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " requires the pending review stage before rejection.");
        }
        if (run.getApprovalPolicy() == ScenarioApprovalPolicy.ESCALATED
            && run.getApprovalStage() != ScenarioApprovalStage.PENDING_REVIEW
            && run.getApprovalStage() != ScenarioApprovalStage.PENDING_FINAL_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " cannot be rejected from its current workflow stage.");
        }

        String reviewerName = request.reviewerName().trim();
        ScenarioActorRole requiredRole = requiredRejectionRole(run);
        accessControlService.requireScenarioActor(request.actorRole(), reviewerName, "reject scenario plans");
        requireActorRole(run.getId(), "reject this scenario", request.actorRole(), requiredRole);
        requireWarehouseAccess(reviewerName, run.getWarehouseCode(), "reject scenario plans");
        requireDistinctRequesterAndReviewer(run.getRequestedBy(), reviewerName, "reject");
        if (requiredRole == ScenarioActorRole.REVIEW_OWNER) {
            requireAssignedReviewOwner(run, reviewerName, "reject");
        }
        if (requiredRole == ScenarioActorRole.FINAL_APPROVER) {
            requireAssignedFinalApprover(run, reviewerName, "reject");
        }

        run.setApprovalStatus(ScenarioApprovalStatus.REJECTED);
        run.setApprovalStage(ScenarioApprovalStage.REJECTED);
        run.setRejectedBy(reviewerName);
        run.setRejectedAt(Instant.now());
        run.setRejectionReason(request.reason().trim());
        run.setApprovedBy(null);
        run.setApprovedAt(null);
        run.setApprovalNote(null);
        run.setApprovalDueAt(null);
        if (requiredRole == ScenarioActorRole.REVIEW_OWNER) {
            run.setReviewApprovedBy(null);
            run.setReviewApprovedAt(null);
            run.setReviewApprovalNote(null);
        }
        run = scenarioRunRepository.save(run);

        businessEventService.record(
            BusinessEventType.SCENARIO_REJECTED,
            "scenario-planner",
            "Rejected saved plan " + run.getTitle() + " for warehouse " + run.getWarehouseCode()
                + " by " + run.getRejectedBy() + ". Reason: " + run.getRejectionReason()
        );

        return new ScenarioRejectionResponse(
            run.getId(),
            run.getTitle(),
            run.getApprovalStatus(),
            run.getRejectedBy(),
            run.getRejectedAt(),
            run.getRejectionReason()
        );
    }

    public ScenarioRunResponse acknowledgeSlaEscalation(long scenarioRunId,
                                                        ScenarioEscalationAcknowledgementRequest request) {
        ScenarioRun run = getScenarioRun(scenarioRunId);
        if (run.getType() != ScenarioRunType.SAVED_PLAN
            || run.getApprovalStatus() != ScenarioApprovalStatus.PENDING_APPROVAL
            || run.getApprovalPolicy() != ScenarioApprovalPolicy.ESCALATED
            || run.getApprovalStage() != ScenarioApprovalStage.PENDING_FINAL_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " cannot acknowledge SLA escalation outside a pending saved-plan review.");
        }
        if (!isSlaEscalated(run)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " has not been SLA escalated.");
        }

        requireActorRole(run.getId(), "acknowledge this escalation", request.actorRole(), ScenarioActorRole.ESCALATION_OWNER);
        String acknowledgedBy = request.acknowledgedBy().trim();
        accessControlService.requireScenarioActor(request.actorRole(), acknowledgedBy, "acknowledge escalated scenario plans");
        requireWarehouseAccess(acknowledgedBy, run.getWarehouseCode(), "acknowledge escalated scenario plans");
        requireAssignedEscalationOwner(run, acknowledgedBy, "acknowledge");
        if (isSlaAcknowledged(run)) {
            if (run.getSlaAcknowledgedBy() != null && run.getSlaAcknowledgedBy().equalsIgnoreCase(acknowledgedBy)) {
                return toScenarioRunResponse(run);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " has already been acknowledged by " + run.getSlaAcknowledgedBy() + ".");
        }

        run.setSlaAcknowledgedBy(acknowledgedBy);
        run.setSlaAcknowledgedAt(Instant.now());
        run.setSlaAcknowledgementNote(request.note().trim());
        run = scenarioRunRepository.save(run);

        businessEventService.record(
            BusinessEventType.SCENARIO_SLA_ACKNOWLEDGED,
            "scenario-planner",
            "Acknowledged SLA escalation for plan " + run.getTitle() + " by " + run.getSlaAcknowledgedBy()
                + ". Note: " + run.getSlaAcknowledgementNote()
        );
        return toScenarioRunResponse(run);
    }

    private OrderCreateRequest getLoadableOrderRequest(ScenarioRun run) {
        if (!isLoadable(run) || run.getRequestPayload() == null || run.getRequestPayload().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " cannot be loaded. Only preview scenarios and saved plans with stored request payloads are loadable.");
        }

        return deserializeOrderRequest(run, "loaded");
    }

    private OrderCreateRequest deserializeOrderRequest(ScenarioRun run, String actionLabel) {
        try {
            OrderCreateRequest request = objectMapper.readValue(run.getRequestPayload(), OrderCreateRequest.class);
            return new OrderCreateRequest(null, request.warehouseCode(), request.items());
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Stored scenario payload could not be " + actionLabel + " for scenario " + run.getId(), exception);
        }
    }

    public List<ScenarioRunResponse> getRecentScenarioRuns() {
        return getScenarioRuns(DEFAULT_HISTORY_FILTER);
    }

    public List<ScenarioNotificationResponse> getScenarioNotifications() {
        return getScenarioNotifications(DEFAULT_NOTIFICATION_LIMIT);
    }

    public List<ScenarioNotificationResponse> getScenarioNotifications(int limit) {
        return getScenarioNotifications(limit, null);
    }

    public List<ScenarioNotificationResponse> getScenarioNotifications(int limit, List<String> warehouseScopes) {
        applyPendingSlaEscalations();
        int effectiveLimit = limit <= 0 ? DEFAULT_NOTIFICATION_LIMIT : Math.min(limit, 12);

        List<ScenarioNotificationResponse> activeEscalations = scenarioRunRepository
            .findTop12ByTenant_CodeIgnoreCaseAndTypeAndApprovalStatusAndSlaEscalatedAtIsNotNullAndSlaAcknowledgedAtIsNullOrderBySlaEscalatedAtDescIdDesc(
                tenantContextService.getCurrentTenantCodeOrDefault(),
                ScenarioRunType.SAVED_PLAN,
                ScenarioApprovalStatus.PENDING_APPROVAL)
            .stream()
            .map(this::toEscalationNotification)
            .toList();

        List<ScenarioNotificationResponse> acknowledgedEscalations = scenarioRunRepository
            .findTop12ByTenant_CodeIgnoreCaseAndTypeAndSlaAcknowledgedAtIsNotNullOrderBySlaAcknowledgedAtDescIdDesc(
                tenantContextService.getCurrentTenantCodeOrDefault(),
                ScenarioRunType.SAVED_PLAN)
            .stream()
            .map(this::toAcknowledgementNotification)
            .toList();

        return java.util.stream.Stream.concat(activeEscalations.stream(), acknowledgedEscalations.stream())
            .sorted(Comparator.comparing(ScenarioNotificationResponse::createdAt).reversed()
                .thenComparing(ScenarioNotificationResponse::scenarioRunId, Comparator.reverseOrder()))
            .filter(notification -> isVisibleToWarehouseScopes(notification.warehouseCode(), warehouseScopes))
            .limit(effectiveLimit)
            .toList();
    }

    private boolean isVisibleToWarehouseScopes(String warehouseCode, List<String> warehouseScopes) {
        if (warehouseScopes == null || warehouseScopes.isEmpty()) {
            return true;
        }
        return warehouseCode != null && warehouseScopes.stream()
            .anyMatch(scope -> scope != null && scope.equalsIgnoreCase(warehouseCode));
    }

    public List<ScenarioRunResponse> getScenarioRunsForInbox(boolean slaEscalatedOnly, int limit) {
        return getScenarioRuns(new ScenarioHistoryFilter(
            ScenarioRunType.SAVED_PLAN,
            ScenarioApprovalStatus.PENDING_APPROVAL,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            slaEscalatedOnly,
            Boolean.FALSE,
            limit
        ));
    }

    public List<ScenarioRunResponse> getScenarioRuns(ScenarioHistoryFilter filter) {
        ScenarioHistoryFilter effectiveFilter = filter == null ? DEFAULT_HISTORY_FILTER : filter;
        applyPendingSlaEscalations();
        return scenarioRunRepository.findAll(
                buildSpecification(effectiveFilter),
                PageRequest.of(0, effectiveFilter.resolvedLimit(), Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id"))))
            .stream()
            .map(this::toScenarioRunResponse)
            .toList();
    }

    private boolean isLoadable(ScenarioRun run) {
        return run.getType() == ScenarioRunType.PREVIEW || run.getType() == ScenarioRunType.SAVED_PLAN;
    }

    private ScenarioRunResponse toScenarioRunResponse(ScenarioRun run) {
        return new ScenarioRunResponse(
            run.getId(),
            run.getType(),
            run.getTitle(),
            run.getSummary(),
            run.getRecommendedOption(),
            run.getWarehouseCode(),
            run.getRevisionOfScenarioRunId(),
            run.getRevisionNumber(),
            isLoadable(run) && run.getRequestPayload() != null && !run.getRequestPayload().isBlank(),
            false,
            run.getApprovalStatus(),
            run.getApprovalPolicy(),
            run.getApprovalStage(),
            run.getRequestedBy(),
            run.getReviewOwner(),
            run.getFinalApprovalOwner(),
            run.getReviewPriority(),
            run.getRiskScore(),
            run.getReviewApprovedBy(),
            run.getReviewApprovedAt(),
            run.getReviewApprovalNote(),
            run.getApprovedBy(),
            run.getApprovalNote(),
            run.getApprovedAt(),
            run.getApprovalDueAt(),
            run.getSlaEscalatedTo(),
            run.getSlaEscalatedAt(),
            isSlaEscalated(run),
            run.getSlaAcknowledgedBy(),
            run.getSlaAcknowledgedAt(),
            run.getSlaAcknowledgementNote(),
            isSlaAcknowledged(run),
            isOverdue(run),
            run.getRejectedBy(),
            run.getRejectedAt(),
            run.getRejectionReason(),
            run.getCreatedAt()
        );
    }

    private ScenarioNotificationResponse toEscalationNotification(ScenarioRun run) {
        String warehouseCode = run.getWarehouseCode() == null ? "Unknown warehouse" : run.getWarehouseCode();
        String actor = run.getSlaEscalatedTo() == null ? "Operations leadership" : run.getSlaEscalatedTo();
        String message = "Final approval for " + warehouseCode + " is overdue and was rerouted to " + actor
            + ". Keep the plan moving or formally reject it.";
        return new ScenarioNotificationResponse(
            run.getId(),
            ScenarioNotificationType.SLA_ESCALATED,
            "Critical plan rerouted: " + run.getTitle(),
            message,
            run.getWarehouseCode(),
            run.getReviewPriority(),
            run.getApprovalStage(),
            actor,
            null,
            true,
            run.getApprovalDueAt(),
            run.getSlaEscalatedAt()
        );
    }

    private ScenarioNotificationResponse toAcknowledgementNotification(ScenarioRun run) {
        String warehouseCode = run.getWarehouseCode() == null ? "Unknown warehouse" : run.getWarehouseCode();
        String actor = run.getSlaAcknowledgedBy() == null ? "Operations owner" : run.getSlaAcknowledgedBy();
        String message = actor + " acknowledged the rerouted approval for " + warehouseCode
            + " and took ownership of the follow-up.";
        return new ScenarioNotificationResponse(
            run.getId(),
            ScenarioNotificationType.SLA_ACKNOWLEDGED,
            "Escalation owned: " + run.getTitle(),
            message,
            run.getWarehouseCode(),
            run.getReviewPriority(),
            run.getApprovalStage(),
            actor,
            run.getSlaAcknowledgementNote(),
            false,
            run.getApprovalDueAt(),
            run.getSlaAcknowledgedAt()
        );
    }

    private Specification<ScenarioRun> buildSpecification(ScenarioHistoryFilter filter) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Specification<ScenarioRun> specification = (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(criteriaBuilder.upper(root.get("tenant").get("code")), tenantCode.toUpperCase());

        if (filter.type() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("type"), filter.type()));
        }

        if (filter.approvalStatus() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("approvalStatus"), filter.approvalStatus()));
        }

        if (filter.approvalPolicy() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("approvalPolicy"), filter.approvalPolicy()));
        }

        if (filter.approvalStage() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("approvalStage"), filter.approvalStage()));
        }

        String normalizedWarehouseCode = filter.normalizedWarehouseCode();
        if (normalizedWarehouseCode != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(criteriaBuilder.upper(root.get("warehouseCode")), normalizedWarehouseCode));
        }

        var currentOperator = accessDirectoryService.getCurrentOperator();
        if (currentOperator.isPresent() && !accessDirectoryService.getWarehouseScopes(currentOperator.get()).isEmpty()) {
            List<String> scopedWarehouses = accessDirectoryService.getWarehouseScopes(currentOperator.get()).stream()
                .map(String::toUpperCase)
                .toList();
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.upper(root.get("warehouseCode")).in(scopedWarehouses));
        }

        String normalizedRequestedBy = filter.normalizedRequestedBy();
        if (normalizedRequestedBy != null) {
            String searchPattern = "%" + normalizedRequestedBy + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("requestedBy")), searchPattern));
        }

        String normalizedReviewOwner = filter.normalizedReviewOwner();
        if (normalizedReviewOwner != null) {
            String searchPattern = "%" + normalizedReviewOwner + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("reviewOwner")), searchPattern));
        }

        String normalizedFinalApprovalOwner = filter.normalizedFinalApprovalOwner();
        if (normalizedFinalApprovalOwner != null) {
            String searchPattern = "%" + normalizedFinalApprovalOwner + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("finalApprovalOwner")), searchPattern));
        }

        if (filter.minimumReviewPriority() != null) {
            List<ScenarioReviewPriority> allowedPriorities = Arrays.stream(ScenarioReviewPriority.values())
                .filter(priority -> priority.ordinal() >= filter.minimumReviewPriority().ordinal())
                .toList();
            specification = specification.and((root, query, criteriaBuilder) ->
                root.get("reviewPriority").in(allowedPriorities));
        }

        if (filter.overdueOnlyEnabled()) {
            Instant now = Instant.now();
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("approvalStatus"), ScenarioApprovalStatus.PENDING_APPROVAL),
                criteriaBuilder.isNotNull(root.get("approvalDueAt")),
                criteriaBuilder.lessThanOrEqualTo(root.get("approvalDueAt"), now)
            ));
        }

        if (filter.slaEscalatedOnlyEnabled()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.isNotNull(root.get("slaEscalatedAt")));
        }

        if (filter.slaAcknowledged() != null) {
            specification = specification.and((root, query, criteriaBuilder) -> filter.slaAcknowledged()
                ? criteriaBuilder.isNotNull(root.get("slaAcknowledgedAt"))
                : criteriaBuilder.isNull(root.get("slaAcknowledgedAt")));
        }

        return specification;
    }

    private ScenarioRun resolveRevisionSource(Long revisionOfScenarioRunId) {
        if (revisionOfScenarioRunId == null) {
            return null;
        }

        ScenarioRun source = getScenarioRun(revisionOfScenarioRunId);
        return validateRevisionSource(source);
    }

    private ScenarioRun lockRevisionSource(String tenantCode, Long revisionOfScenarioRunId) {
        ScenarioRun source = scenarioRunRepository.findForRevisionUpdate(tenantCode, revisionOfScenarioRunId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Scenario not found: " + revisionOfScenarioRunId));
        return validateRevisionSource(source);
    }

    private ScenarioRun validateRevisionSource(ScenarioRun source) {
        if (source.getType() != ScenarioRunType.SAVED_PLAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + source.getId() + " cannot be revised. Only saved plans can become revision sources.");
        }
        if (source.getApprovalStatus() != ScenarioApprovalStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + source.getId() + " cannot be revised yet. Only rejected saved plans can be resubmitted as revisions.");
        }
        return source;
    }

    private void requireSameRevisionWarehouse(ScenarioRun revisionSource, String warehouseCode) {
        if (revisionSource != null && (revisionSource.getWarehouseCode() == null
            || !revisionSource.getWarehouseCode().equalsIgnoreCase(warehouseCode))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Scenario " + revisionSource.getId() + " can only be revised for warehouse "
                    + revisionSource.getWarehouseCode() + ". Create a new scenario for another warehouse.");
        }
    }

    private ResponseStatusException revisionConflict(Long parentId, Long childId) {
        String childDescription = childId == null ? "an existing revision" : "revision " + childId;
        return new ResponseStatusException(HttpStatus.CONFLICT,
            "Scenario " + parentId + " has already been revised as " + childDescription
                + ". Revise the latest rejected revision instead.");
    }

    private boolean isRevisionLineageConstraint(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                && "uk_scenario_revision_parent".equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("uk_scenario_revision_parent")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private int nextRevisionNumber(ScenarioRun revisionSource) {
        if (revisionSource.getRevisionNumber() == null || revisionSource.getRevisionNumber() < 1) {
            return 2;
        }
        return revisionSource.getRevisionNumber() + 1;
    }

    private String buildSavedPlanSummary(OrderCreateRequest request,
                                         ScenarioOrderImpactResponse projectedImpact,
                                         ScenarioRiskAssessment riskAssessment) {
        int totalUnits = request.items().stream().mapToInt(item -> item.quantity()).sum();
        int lowStockItems = (int) projectedImpact.projectedInventory().stream().filter(item -> item.lowStock()).count();
        int criticalItems = (int) projectedImpact.projectedInventory().stream()
            .filter(item -> "critical".equalsIgnoreCase(item.riskLevel()))
            .count();
        return "Saved plan for " + request.warehouseCode().trim()
            + " with " + request.items().size() + " line items and " + totalUnits + " projected units."
            + " Review priority " + riskAssessment.reviewPriority()
            + " (score " + riskAssessment.score() + ") with "
            + criticalItems + " critical exposures and " + lowStockItems + " low-stock items.";
    }

    private String buildRecommendedOption(ScenarioOrderImpactResponse projectedImpact) {
        if (projectedImpact.projectedRecommendations().isEmpty()) {
            return "Review and hand off when ready";
        }
        return projectedImpact.projectedRecommendations().get(0).title();
    }

    private String resolveRequestedBy(String requestedBy, String warehouseCode, String authenticatedActorName) {
        if (authenticatedActorName != null
            && !authenticatedActorName.isBlank()
            && !authenticatedActorName.equalsIgnoreCase("dev-anonymous")) {
            String sessionActor = authenticatedActorName.trim();
            if (requestedBy != null && !requestedBy.isBlank()
                && !sessionActor.equalsIgnoreCase(requestedBy.trim())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Scenario requester must match the authenticated session actor.");
            }
            requireWarehouseAccess(sessionActor, warehouseCode, "request scenarios for warehouse " + warehouseCode);
            return sessionActor;
        }

        if (requestedBy == null || requestedBy.isBlank()) {
            return resolvePreferredOperatorName(
                List.of(),
                null,
                "scenario requester",
                null,
                warehouseCode
            );
        }
        String requester = accessDirectoryService.requireActiveOperatorName(requestedBy.trim(), "scenario requester");
        requireWarehouseAccess(requester, warehouseCode, "request scenarios for warehouse " + warehouseCode);
        return requester;
    }

    private String resolveReviewOwner(String reviewOwner, String warehouseCode, String requestedBy) {
        var policy = tenantOperationalPolicyService.getCurrentPolicy();
        if (reviewOwner == null || reviewOwner.isBlank()) {
            return resolvePreferredOperatorName(
                List.of(),
                policy.getReviewOwnerRole(),
                "review owner",
                requestedBy,
                warehouseCode
            );
        }
        String resolvedReviewOwner = accessDirectoryService.requireOperatorWithRoleName(
            reviewOwner.trim(),
            policy.getReviewOwnerRole(),
            "review owner",
            warehouseCode
        );
        if (policy.getReviewOwnerRole() == SynapseAccessRole.REVIEW_OWNER
            && (!hasExplicitWarehouseScope(resolvedReviewOwner, warehouseCode)
                || isBootstrapTenantAdmin(resolvedReviewOwner))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Review owner must be an explicitly assigned reviewer for the selected warehouse.");
        }
        return resolvedReviewOwner;
    }

    private String resolveFinalApprovalOwner(String warehouseCode, String reviewOwner) {
        var policy = tenantOperationalPolicyService.getCurrentPolicy();
        return resolvePreferredOperatorName(
            List.of(),
            policy.getFinalApproverRole(),
            "final approval owner",
            reviewOwner,
            warehouseCode
        );
    }

    private String resolvePreferredOperatorName(List<String> preferredNames,
                                                SynapseAccessRole requiredRole,
                                                String fieldLabel) {
        return resolvePreferredOperatorName(preferredNames, requiredRole, fieldLabel, null, null);
    }

    private String resolvePreferredOperatorName(List<String> preferredNames,
                                                SynapseAccessRole requiredRole,
                                                String fieldLabel,
                                                String excludedActorName,
                                                String warehouseCode) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        String normalizedWarehouseCode = warehouseCode == null ? null : warehouseCode.trim().toUpperCase();
        List<com.synapsecore.access.dto.AccessOperatorResponse> availableOperators = accessDirectoryService.getActiveOperators(tenantCode).stream()
            .filter(operator -> requiredRole == null || operator.roles().contains(requiredRole))
            .filter(operator -> requiredRole != SynapseAccessRole.REVIEW_OWNER
                || (normalizedWarehouseCode != null
                    && !normalizedWarehouseCode.isBlank()
                    && !isBootstrapTenantAdmin(operator)
                    && operator.warehouseScopes().stream()
                        .anyMatch(scope -> scope.equalsIgnoreCase(normalizedWarehouseCode))))
            .filter(operator -> normalizedWarehouseCode == null || normalizedWarehouseCode.isBlank()
                || operator.warehouseScopes().isEmpty()
                || operator.warehouseScopes().stream()
                    .anyMatch(scope -> scope.equalsIgnoreCase(normalizedWarehouseCode)))
            .filter(operator -> excludedActorName == null || !operator.actorName().equalsIgnoreCase(excludedActorName))
            .sorted(Comparator
                .comparingInt((com.synapsecore.access.dto.AccessOperatorResponse operator) ->
                    normalizedWarehouseCode != null
                        && !normalizedWarehouseCode.isBlank()
                        && operator.warehouseScopes().stream()
                            .anyMatch(scope -> scope.equalsIgnoreCase(normalizedWarehouseCode))
                        ? 0
                        : operator.warehouseScopes().isEmpty() ? 1 : 2)
                .thenComparing(com.synapsecore.access.dto.AccessOperatorResponse::displayName, String.CASE_INSENSITIVE_ORDER))
            .toList();

        for (String preferredName : preferredNames) {
            String match = availableOperators.stream()
                .map(com.synapsecore.access.dto.AccessOperatorResponse::actorName)
                .filter(actorName -> actorName.equalsIgnoreCase(preferredName))
                .findFirst()
                .orElse(null);
            if (match != null) {
                return match;
            }
        }

        return availableOperators.stream()
            .map(com.synapsecore.access.dto.AccessOperatorResponse::actorName)
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No active " + fieldLabel + " is available in tenant " + tenantCode + "."
            ));
    }

    private boolean isBootstrapTenantAdmin(String actorName) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        return accessDirectoryService.getActiveOperators(tenantCode).stream()
            .filter(operator -> operator.actorName().equalsIgnoreCase(actorName))
            .anyMatch(this::isBootstrapTenantAdmin);
    }

    private boolean hasExplicitWarehouseScope(String actorName, String warehouseCode) {
        String normalizedWarehouseCode = warehouseCode == null ? "" : warehouseCode.trim();
        if (normalizedWarehouseCode.isBlank()) {
            return false;
        }
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        return accessDirectoryService.getActiveOperators(tenantCode).stream()
            .filter(operator -> operator.actorName().equalsIgnoreCase(actorName))
            .anyMatch(operator -> operator.warehouseScopes().stream()
                .anyMatch(scope -> scope.equalsIgnoreCase(normalizedWarehouseCode)));
    }

    private boolean isBootstrapTenantAdmin(AccessOperatorResponse operator) {
        return operator.roles().contains(SynapseAccessRole.TENANT_ADMIN)
            && operator.description() != null
            && operator.description().regionMatches(
                true,
                0,
                "Bootstrap tenant admin for ",
                0,
                "Bootstrap tenant admin for ".length()
            );
    }

    private void requireWarehouseAccess(String actorName, String warehouseCode, String actionDescription) {
        accessDirectoryService.requireOperatorWarehouseAccess(
            actorName,
            tenantContextService.getCurrentTenantCodeOrDefault(),
            warehouseCode,
            actionDescription + " for warehouse " + warehouseCode
        );
    }

    private ScenarioApprovalPolicy determineApprovalPolicy(ScenarioReviewPriority reviewPriority) {
        var policy = tenantOperationalPolicyService.getCurrentPolicy();
        if (reviewPriority.ordinal() >= policy.getEscalatedApprovalMinimumPriority().ordinal()) {
            return ScenarioApprovalPolicy.ESCALATED;
        }
        return ScenarioApprovalPolicy.STANDARD;
    }

    private Instant resolveApprovalDueAt(ScenarioApprovalPolicy approvalPolicy,
                                         ScenarioApprovalStage approvalStage,
                                         ScenarioReviewPriority reviewPriority) {
        if (approvalStage == null || approvalStage == ScenarioApprovalStage.APPROVED
            || approvalStage == ScenarioApprovalStage.REJECTED
            || approvalStage == ScenarioApprovalStage.NOT_REQUIRED) {
            return null;
        }

        var policy = tenantOperationalPolicyService.getCurrentPolicy();
        Duration duration = switch (approvalStage) {
            case PENDING_FINAL_APPROVAL -> Duration.ofHours(switch (reviewPriority) {
                case CRITICAL -> policy.getFinalApprovalHoursCritical();
                case HIGH -> policy.getFinalApprovalHoursHigh();
                case MEDIUM -> policy.getFinalApprovalHoursMedium();
            });
            case PENDING_REVIEW -> Duration.ofHours(switch (reviewPriority) {
                case CRITICAL -> policy.getReviewHoursCritical();
                case HIGH -> policy.getReviewHoursHigh();
                case MEDIUM -> policy.getReviewHoursMedium();
            });
            default -> null;
        };

        return duration == null ? null : Instant.now().plus(duration);
    }

    private boolean isOverdue(ScenarioRun run) {
        return run.getApprovalStatus() == ScenarioApprovalStatus.PENDING_APPROVAL
            && run.getApprovalDueAt() != null
            && !run.getApprovalDueAt().isAfter(Instant.now());
    }

    private boolean isSlaEscalated(ScenarioRun run) {
        return run.getSlaEscalatedAt() != null;
    }

    private boolean isSlaAcknowledged(ScenarioRun run) {
        return run.getSlaAcknowledgedAt() != null;
    }

    private ScenarioActorRole requiredApprovalRole(ScenarioRun run) {
        if (run.getApprovalPolicy() == ScenarioApprovalPolicy.ESCALATED
            && run.getApprovalStage() == ScenarioApprovalStage.PENDING_FINAL_APPROVAL) {
            return ScenarioActorRole.FINAL_APPROVER;
        }
        return ScenarioActorRole.REVIEW_OWNER;
    }

    private ScenarioActorRole requiredRejectionRole(ScenarioRun run) {
        if (run.getApprovalPolicy() == ScenarioApprovalPolicy.ESCALATED
            && run.getApprovalStage() == ScenarioApprovalStage.PENDING_FINAL_APPROVAL) {
            return ScenarioActorRole.FINAL_APPROVER;
        }
        return ScenarioActorRole.REVIEW_OWNER;
    }

    private void requireActorRole(long scenarioRunId,
                                  String actionDescription,
                                  ScenarioActorRole actualRole,
                                  ScenarioActorRole expectedRole) {
        if (actualRole != expectedRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + scenarioRunId + " requires actor role " + expectedRole
                    + " to " + actionDescription + ".");
        }
    }

    private void requireAssignedReviewOwner(ScenarioRun run, String actorName, String actionLabel) {
        if (run.getReviewOwner() == null || run.getReviewOwner().isBlank()
            || !run.getReviewOwner().equalsIgnoreCase(actorName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " requires the assigned review owner to " + actionLabel + " this plan.");
        }
    }

    private void requireDistinctRequesterAndReviewer(String requesterName, String reviewerName, String actionLabel) {
        if (requesterName != null && !requesterName.isBlank()
            && reviewerName != null && !reviewerName.isBlank()
            && requesterName.equalsIgnoreCase(reviewerName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario requires a review owner who is different from the requester to " + actionLabel + " this plan.");
        }
    }

    private void requireAssignedFinalApprover(ScenarioRun run, String actorName, String actionLabel) {
        if (run.getFinalApprovalOwner() == null || run.getFinalApprovalOwner().isBlank()
            || !run.getFinalApprovalOwner().equalsIgnoreCase(actorName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " requires the assigned final approval owner to " + actionLabel + " this plan.");
        }
    }

    private void requireApprovedApprovalRetryAuthority(ScenarioRun run, ScenarioApprovalRequest request) {
        String actorName = request.approverName().trim();
        accessControlService.requireScenarioActor(request.actorRole(), actorName, "retry scenario approval");
        ScenarioActorRole requiredRole = run.getApprovalPolicy() == ScenarioApprovalPolicy.ESCALATED
            ? ScenarioActorRole.FINAL_APPROVER
            : ScenarioActorRole.REVIEW_OWNER;
        requireActorRole(run.getId(), "retry scenario approval", request.actorRole(), requiredRole);
        requireWarehouseAccess(actorName, run.getWarehouseCode(), "retry scenario approval");
        if (requiredRole == ScenarioActorRole.FINAL_APPROVER) {
            requireAssignedFinalApprover(run, actorName, "retry approval");
        } else {
            requireAssignedReviewOwner(run, actorName, "retry approval");
        }
        if (run.getApprovedBy() == null || !run.getApprovedBy().equalsIgnoreCase(actorName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " has already been approved by another authorized actor.");
        }
    }

    private void requireAssignedEscalationOwner(ScenarioRun run, String actorName, String actionLabel) {
        if (run.getSlaEscalatedTo() == null || run.getSlaEscalatedTo().isBlank()
            || !run.getSlaEscalatedTo().equalsIgnoreCase(actorName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " requires the assigned escalation owner to " + actionLabel + " this escalation.");
        }
    }

    private void applyPendingSlaEscalations() {
        Instant now = Instant.now();
        scenarioRunRepository
            .findTop20ByTenant_CodeIgnoreCaseAndApprovalStatusAndApprovalPolicyAndApprovalStageAndApprovalDueAtLessThanEqualAndSlaEscalatedAtIsNullOrderByCreatedAtDescIdDesc(
                tenantContextService.getCurrentTenantCodeOrDefault(),
                ScenarioApprovalStatus.PENDING_APPROVAL,
                ScenarioApprovalPolicy.ESCALATED,
                ScenarioApprovalStage.PENDING_FINAL_APPROVAL,
                now)
            .forEach(this::applySlaEscalationIfNeeded);
    }

    private ScenarioRun applySlaEscalationIfNeeded(ScenarioRun run) {
        if (!isEligibleForSlaEscalation(run)) {
            return run;
        }

        String previousFinalApprovalOwner = run.getFinalApprovalOwner();
        String escalatedFinalApprovalOwner = resolveSlaEscalatedFinalApprovalOwner(run);
        String escalationOwner = resolveSlaEscalationOwner(run);
        run.setFinalApprovalOwner(escalatedFinalApprovalOwner);
        run.setSlaEscalatedTo(escalationOwner);
        run.setSlaEscalatedAt(Instant.now());
        run = scenarioRunRepository.save(run);

        businessEventService.record(
            BusinessEventType.SCENARIO_SLA_ESCALATED,
            "scenario-planner",
            "Escalated overdue plan " + run.getTitle() + " from final approver "
                + previousFinalApprovalOwner + " to " + escalatedFinalApprovalOwner
                + " with escalation owner " + escalationOwner + "."
        );
        return run;
    }

    private boolean isEligibleForSlaEscalation(ScenarioRun run) {
        return run.getType() == ScenarioRunType.SAVED_PLAN
            && run.getApprovalStatus() == ScenarioApprovalStatus.PENDING_APPROVAL
            && run.getApprovalPolicy() == ScenarioApprovalPolicy.ESCALATED
            && run.getApprovalStage() == ScenarioApprovalStage.PENDING_FINAL_APPROVAL
            && isOverdue(run)
            && run.getSlaEscalatedAt() == null;
    }

    private String resolveSlaEscalationOwner(ScenarioRun run) {
        var policy = tenantOperationalPolicyService.getCurrentPolicy();
        return resolvePreferredOperatorName(
            List.of(),
            policy.getEscalationOwnerRole(),
            "escalation owner",
            run.getFinalApprovalOwner(),
            run.getWarehouseCode()
        );
    }

    private String resolveSlaEscalatedFinalApprovalOwner(ScenarioRun run) {
        var policy = tenantOperationalPolicyService.getCurrentPolicy();
        try {
            return resolvePreferredOperatorName(
                List.of(),
                policy.getFinalApproverRole(),
                "escalated final approval owner",
                run.getFinalApprovalOwner(),
                run.getWarehouseCode()
            );
        } catch (ResponseStatusException exception) {
            if (run.getFinalApprovalOwner() != null && !run.getFinalApprovalOwner().isBlank()) {
                return run.getFinalApprovalOwner();
            }
            throw exception;
        }
    }

    private ScenarioRun processEscalatedApproval(ScenarioRun run,
                                                 String approverName,
                                                 String approvalNote) {
        if (approvalNote.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " requires an approval note before escalated approval can be granted.");
        }
        if (run.getRequestedBy() != null && run.getRequestedBy().equalsIgnoreCase(approverName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario " + run.getId() + " requires an approver who is different from the requester.");
        }

        if (run.getApprovalStage() == ScenarioApprovalStage.PENDING_REVIEW) {
            requireAssignedReviewOwner(run, approverName, "approve");

            run.setReviewApprovedBy(approverName);
            run.setReviewApprovedAt(Instant.now());
            run.setReviewApprovalNote(approvalNote);
            run.setApprovalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
            run.setApprovalDueAt(resolveApprovalDueAt(run.getApprovalPolicy(), ScenarioApprovalStage.PENDING_FINAL_APPROVAL, run.getReviewPriority()));
            run.setRejectedBy(null);
            run.setRejectedAt(null);
            run.setRejectionReason(null);
            run = scenarioRunRepository.save(run);

            businessEventService.record(
                BusinessEventType.SCENARIO_ESCALATION_ADVANCED,
                "scenario-planner",
                "Escalated saved plan " + run.getTitle() + " advanced to final approval after owner review by "
                    + run.getReviewApprovedBy() + ". Final approval owner: " + run.getFinalApprovalOwner() + "."
            );
            return run;
        }

        if (run.getApprovalStage() == ScenarioApprovalStage.PENDING_FINAL_APPROVAL) {
            requireAssignedFinalApprover(run, approverName, "approve");
            if (run.getReviewApprovedBy() != null && run.getReviewApprovedBy().equalsIgnoreCase(approverName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Scenario " + run.getId() + " requires a final approver who is different from the owner reviewer.");
            }

            run.setApprovalStatus(ScenarioApprovalStatus.APPROVED);
            run.setApprovalStage(ScenarioApprovalStage.APPROVED);
            run.setApprovedBy(approverName);
            run.setApprovedAt(Instant.now());
            run.setApprovalNote(approvalNote);
            run.setApprovalDueAt(null);
            run.setRejectedBy(null);
            run.setRejectedAt(null);
            run.setRejectionReason(null);
            run = scenarioRunRepository.save(run);

            businessEventService.record(
                BusinessEventType.SCENARIO_APPROVED,
                "scenario-planner",
                "Approved escalated saved plan " + run.getTitle() + " for warehouse " + run.getWarehouseCode()
                    + " by " + run.getApprovedBy() + " after owner review by " + run.getReviewApprovedBy() + "."
            );
            return run;
        }

        return run;
    }

    private String serializeRequest(OrderCreateRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Scenario request could not be stored.", exception);
        }
    }
}
