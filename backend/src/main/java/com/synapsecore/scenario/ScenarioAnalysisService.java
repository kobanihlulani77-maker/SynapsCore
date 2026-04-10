package com.synapsecore.scenario;

import com.synapsecore.domain.dto.InventoryStatusResponse;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.scenario.dto.ScenarioCompareRequest;
import com.synapsecore.scenario.dto.ScenarioComparisonResponse;
import com.synapsecore.scenario.dto.ScenarioComparisonSummary;
import com.synapsecore.scenario.dto.ScenarioOrderImpactResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScenarioAnalysisService {

    private final ScenarioProjectionService scenarioProjectionService;
    private final ScenarioRiskPolicyService scenarioRiskPolicyService;
    private final ScenarioHistoryService scenarioHistoryService;
    private final BusinessEventService businessEventService;

    public ScenarioOrderImpactResponse analyzeOrderImpact(OrderCreateRequest request) {
        ScenarioOrderImpactResponse response = scenarioProjectionService.projectOrderImpact(request);
        scenarioHistoryService.recordPreview(request, response);
        businessEventService.record(
            BusinessEventType.SCENARIO_ANALYZED,
            "scenario-planner",
            "Previewed " + response.totalUnits() + " units for " + response.warehouseCode()
                + " with " + response.projectedAlerts().size() + " projected alerts."
        );
        return response;
    }

    public ScenarioComparisonResponse compareOrderImpact(ScenarioCompareRequest request) {
        ScenarioOrderImpactResponse primary = scenarioProjectionService.projectOrderImpact(request.primary());
        ScenarioOrderImpactResponse alternative = scenarioProjectionService.projectOrderImpact(request.alternative());

        int primaryRiskScore = scenarioRiskPolicyService.assess(primary).score();
        int alternativeRiskScore = scenarioRiskPolicyService.assess(alternative).score();
        String recommendedOption = primaryRiskScore == alternativeRiskScore
            ? "tie"
            : primaryRiskScore < alternativeRiskScore
                ? resolveLabel(request.primaryLabel(), "Primary")
                : resolveLabel(request.alternativeLabel(), "Alternative");

        ScenarioComparisonResponse response = new ScenarioComparisonResponse(
            resolveLabel(request.primaryLabel(), "Primary"),
            primary,
            resolveLabel(request.alternativeLabel(), "Alternative"),
            alternative,
            new ScenarioComparisonSummary(
                recommendedOption,
                primaryRiskScore,
                alternativeRiskScore,
                buildComparisonRationale(primary, alternative, request)
            ),
            Instant.now()
        );
        scenarioHistoryService.recordComparison(response);
        businessEventService.record(
            BusinessEventType.SCENARIO_COMPARED,
            "scenario-planner",
            "Compared " + response.primaryLabel() + " against " + response.alternativeLabel()
                + ". Recommended " + response.summary().recommendedOption() + "."
        );
        return response;
    }

    private String buildComparisonRationale(ScenarioOrderImpactResponse primary,
                                            ScenarioOrderImpactResponse alternative,
                                            ScenarioCompareRequest request) {
        int primaryLowStock = (int) primary.projectedInventory().stream().filter(InventoryStatusResponse::lowStock).count();
        int alternativeLowStock = (int) alternative.projectedInventory().stream().filter(InventoryStatusResponse::lowStock).count();
        int primaryCritical = (int) primary.projectedInventory().stream()
            .filter(item -> "critical".equalsIgnoreCase(item.riskLevel()))
            .count();
        int alternativeCritical = (int) alternative.projectedInventory().stream()
            .filter(item -> "critical".equalsIgnoreCase(item.riskLevel()))
            .count();

        if (primaryCritical == alternativeCritical && primaryLowStock == alternativeLowStock) {
            return "Both options create a similar inventory risk profile. Compare cost, timing, or warehouse priorities.";
        }

        String betterLabel = primaryCritical < alternativeCritical || (primaryCritical == alternativeCritical && primaryLowStock < alternativeLowStock)
            ? resolveLabel(request.primaryLabel(), "Primary")
            : resolveLabel(request.alternativeLabel(), "Alternative");

        return betterLabel + " keeps projected critical exposures at "
            + Math.min(primaryCritical, alternativeCritical)
            + " and projected low-stock items at "
            + Math.min(primaryLowStock, alternativeLowStock)
            + ", which is lower than the alternative plan.";
    }

    private String resolveLabel(String requestedLabel, String fallback) {
        if (requestedLabel == null || requestedLabel.isBlank()) {
            return fallback;
        }
        return requestedLabel.trim();
    }
}
