package com.synapsecore.domain.service;

import com.synapsecore.domain.dto.TenantOperationalPolicyRequest;
import com.synapsecore.domain.dto.TenantOperationalPolicyResponse;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.TenantOperationalPolicy;
import com.synapsecore.domain.repository.TenantOperationalPolicyRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.tenant.TenantContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantOperationalPolicyService {

    private final TenantOperationalPolicyRepository tenantOperationalPolicyRepository;
    private final TenantRepository tenantRepository;
    private final TenantContextService tenantContextService;

    @Transactional(readOnly = true)
    public TenantOperationalPolicy getCurrentPolicy() {
        return getPolicy(tenantContextService.getCurrentTenantCodeOrDefault());
    }

    @Transactional(readOnly = true)
    public TenantOperationalPolicy getPolicy(String tenantCode) {
        return tenantOperationalPolicyRepository.findByTenant_CodeIgnoreCase(tenantCode)
            .orElseGet(() -> createDefaultPolicyForTenant(tenantCode));
    }

    @Transactional
    public TenantOperationalPolicyResponse updateCurrentPolicy(TenantOperationalPolicyRequest request) {
        TenantOperationalPolicy policy = getPolicy(tenantContextService.getCurrentTenantCodeOrDefault());
        policy.setLowStockCriticalRatio(request.lowStockCriticalRatio());
        policy.setDepletionRiskHoursThreshold(request.depletionRiskHoursThreshold());
        policy.setUrgentDepletionRiskHoursThreshold(request.urgentDepletionRiskHoursThreshold());
        policy.setRapidConsumptionUnitsMinimum(request.rapidConsumptionUnitsMinimum());
        policy.setRapidConsumptionThresholdRatio(request.rapidConsumptionThresholdRatio());
        policy.setBacklogRiskCount(request.backlogRiskCount());
        policy.setBacklogCriticalCount(request.backlogCriticalCount());
        policy.setBacklogClearHoursThreshold(request.backlogClearHoursThreshold());
        policy.setDelayedShipmentCountThreshold(request.delayedShipmentCountThreshold());
        policy.setOverdueDispatchCountThreshold(request.overdueDispatchCountThreshold());
        policy.setDeliveryDelayToleranceHours(request.deliveryDelayToleranceHours());
        policy.setHighRiskScoreThreshold(request.highRiskScoreThreshold());
        policy.setCriticalRiskScoreThreshold(request.criticalRiskScoreThreshold());
        if (request.lowStockSeverity() != null) {
            policy.setLowStockSeverity(request.lowStockSeverity());
        }
        if (request.lowStockCriticalSeverity() != null) {
            policy.setLowStockCriticalSeverity(request.lowStockCriticalSeverity());
        }
        if (request.depletionRiskSeverity() != null) {
            policy.setDepletionRiskSeverity(request.depletionRiskSeverity());
        }
        if (request.urgentDepletionRiskSeverity() != null) {
            policy.setUrgentDepletionRiskSeverity(request.urgentDepletionRiskSeverity());
        }
        if (request.backlogRiskSeverity() != null) {
            policy.setBacklogRiskSeverity(request.backlogRiskSeverity());
        }
        if (request.backlogCriticalSeverity() != null) {
            policy.setBacklogCriticalSeverity(request.backlogCriticalSeverity());
        }
        if (request.deliveryDelaySeverity() != null) {
            policy.setDeliveryDelaySeverity(request.deliveryDelaySeverity());
        }
        if (request.fulfillmentAnomalySeverity() != null) {
            policy.setFulfillmentAnomalySeverity(request.fulfillmentAnomalySeverity());
        }
        if (request.lowStockRecommendationPriority() != null) {
            policy.setLowStockRecommendationPriority(request.lowStockRecommendationPriority());
        }
        if (request.criticalLowStockRecommendationPriority() != null) {
            policy.setCriticalLowStockRecommendationPriority(request.criticalLowStockRecommendationPriority());
        }
        if (request.depletionRiskRecommendationPriority() != null) {
            policy.setDepletionRiskRecommendationPriority(request.depletionRiskRecommendationPriority());
        }
        if (request.urgentDepletionRiskRecommendationPriority() != null) {
            policy.setUrgentDepletionRiskRecommendationPriority(request.urgentDepletionRiskRecommendationPriority());
        }
        if (request.backlogRecommendationPriority() != null) {
            policy.setBacklogRecommendationPriority(request.backlogRecommendationPriority());
        }
        if (request.deliveryDelayRecommendationPriority() != null) {
            policy.setDeliveryDelayRecommendationPriority(request.deliveryDelayRecommendationPriority());
        }
        if (request.fulfillmentAnomalyRecommendationPriority() != null) {
            policy.setFulfillmentAnomalyRecommendationPriority(request.fulfillmentAnomalyRecommendationPriority());
        }
        if (request.escalatedApprovalMinimumPriority() != null) {
            policy.setEscalatedApprovalMinimumPriority(request.escalatedApprovalMinimumPriority());
        }
        policy.setReviewHoursMedium(request.reviewHoursMedium());
        policy.setReviewHoursHigh(request.reviewHoursHigh());
        policy.setReviewHoursCritical(request.reviewHoursCritical());
        policy.setFinalApprovalHoursMedium(request.finalApprovalHoursMedium());
        policy.setFinalApprovalHoursHigh(request.finalApprovalHoursHigh());
        policy.setFinalApprovalHoursCritical(request.finalApprovalHoursCritical());
        policy.setReviewOwnerRole(request.reviewOwnerRole());
        policy.setFinalApproverRole(request.finalApproverRole());
        policy.setEscalationOwnerRole(request.escalationOwnerRole());
        return toResponse(tenantOperationalPolicyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public TenantOperationalPolicyResponse getCurrentPolicyResponse() {
        return toResponse(getCurrentPolicy());
    }

    private TenantOperationalPolicy createDefaultPolicyForTenant(String tenantCode) {
        Tenant tenant = tenantRepository.findByCodeIgnoreCase(tenantCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Tenant not found for operational policy: " + tenantCode));
        return tenantOperationalPolicyRepository.save(TenantOperationalPolicy.builder()
            .tenant(tenant)
            .build());
    }

    private TenantOperationalPolicyResponse toResponse(TenantOperationalPolicy policy) {
        return new TenantOperationalPolicyResponse(
            policy.getId(),
            policy.getTenant().getCode(),
            policy.getLowStockCriticalRatio(),
            policy.getDepletionRiskHoursThreshold(),
            policy.getUrgentDepletionRiskHoursThreshold(),
            policy.getRapidConsumptionUnitsMinimum(),
            policy.getRapidConsumptionThresholdRatio(),
            policy.getBacklogRiskCount(),
            policy.getBacklogCriticalCount(),
            policy.getBacklogClearHoursThreshold(),
            policy.getDelayedShipmentCountThreshold(),
            policy.getOverdueDispatchCountThreshold(),
            policy.getDeliveryDelayToleranceHours(),
            policy.getHighRiskScoreThreshold(),
            policy.getCriticalRiskScoreThreshold(),
            policy.getLowStockSeverity(),
            policy.getLowStockCriticalSeverity(),
            policy.getDepletionRiskSeverity(),
            policy.getUrgentDepletionRiskSeverity(),
            policy.getBacklogRiskSeverity(),
            policy.getBacklogCriticalSeverity(),
            policy.getDeliveryDelaySeverity(),
            policy.getFulfillmentAnomalySeverity(),
            policy.getLowStockRecommendationPriority(),
            policy.getCriticalLowStockRecommendationPriority(),
            policy.getDepletionRiskRecommendationPriority(),
            policy.getUrgentDepletionRiskRecommendationPriority(),
            policy.getBacklogRecommendationPriority(),
            policy.getDeliveryDelayRecommendationPriority(),
            policy.getFulfillmentAnomalyRecommendationPriority(),
            policy.getEscalatedApprovalMinimumPriority(),
            policy.getReviewHoursMedium(),
            policy.getReviewHoursHigh(),
            policy.getReviewHoursCritical(),
            policy.getFinalApprovalHoursMedium(),
            policy.getFinalApprovalHoursHigh(),
            policy.getFinalApprovalHoursCritical(),
            policy.getReviewOwnerRole(),
            policy.getFinalApproverRole(),
            policy.getEscalationOwnerRole(),
            policy.getUpdatedAt()
        );
    }
}
