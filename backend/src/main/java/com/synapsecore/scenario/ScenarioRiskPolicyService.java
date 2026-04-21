package com.synapsecore.scenario;

import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.service.TenantOperationalPolicyService;
import com.synapsecore.domain.entity.RecommendationPriority;
import com.synapsecore.domain.entity.ScenarioReviewPriority;
import com.synapsecore.scenario.dto.ScenarioOrderImpactResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScenarioRiskPolicyService {

    private final TenantOperationalPolicyService tenantOperationalPolicyService;

    public ScenarioRiskAssessment assess(ScenarioOrderImpactResponse response) {
        int score = calculateRiskScore(response);
        return new ScenarioRiskAssessment(score, determineReviewPriority(response, score));
    }

    private int calculateRiskScore(ScenarioOrderImpactResponse response) {
        int lowStockItems = (int) response.projectedInventory().stream().filter(item -> item.lowStock()).count();
        int criticalItems = (int) response.projectedInventory().stream()
            .filter(item -> "critical".equalsIgnoreCase(item.riskLevel()))
            .count();
        int highRiskItems = (int) response.projectedInventory().stream()
            .filter(item -> "high".equalsIgnoreCase(item.riskLevel()))
            .count();

        return (criticalItems * 100)
            + (highRiskItems * 25)
            + (lowStockItems * 20)
            + (response.projectedAlerts().size() * 10)
            + (response.projectedRecommendations().size() * 5);
    }

    private ScenarioReviewPriority determineReviewPriority(ScenarioOrderImpactResponse response, int score) {
        var policy = tenantOperationalPolicyService.getCurrentPolicy();
        boolean criticalExposure = response.projectedInventory().stream()
            .anyMatch(item -> "critical".equalsIgnoreCase(item.riskLevel()))
            || response.projectedAlerts().stream().anyMatch(alert -> alert.severity() == AlertSeverity.CRITICAL)
            || response.projectedRecommendations().stream()
                .anyMatch(recommendation -> recommendation.priority() == RecommendationPriority.CRITICAL);

        if (criticalExposure || score >= policy.getCriticalRiskScoreThreshold()) {
            return ScenarioReviewPriority.CRITICAL;
        }

        boolean highExposure = response.projectedInventory().stream()
            .anyMatch(item -> item.lowStock() || "high".equalsIgnoreCase(item.riskLevel()))
            || response.projectedAlerts().stream().anyMatch(alert -> alert.severity() == AlertSeverity.HIGH)
            || response.projectedRecommendations().stream()
                .anyMatch(recommendation -> recommendation.priority() == RecommendationPriority.HIGH);

        if (highExposure || score >= policy.getHighRiskScoreThreshold()) {
            return ScenarioReviewPriority.HIGH;
        }

        return ScenarioReviewPriority.MEDIUM;
    }
}
