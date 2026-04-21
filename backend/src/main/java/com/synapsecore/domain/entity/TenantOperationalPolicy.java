package com.synapsecore.domain.entity;

import com.synapsecore.access.SynapseAccessRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tenant_operational_policies",
    uniqueConstraints = @UniqueConstraint(name = "uk_tenant_operational_policy_tenant", columnNames = {"tenant_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantOperationalPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Builder.Default
    @Column(nullable = false)
    private double lowStockCriticalRatio = 0.5d;

    @Builder.Default
    @Column(nullable = false)
    private double depletionRiskHoursThreshold = 8d;

    @Builder.Default
    @Column(nullable = false)
    private double urgentDepletionRiskHoursThreshold = 4d;

    @Builder.Default
    @Column(nullable = false)
    private int rapidConsumptionUnitsMinimum = 5;

    @Builder.Default
    @Column(nullable = false)
    private double rapidConsumptionThresholdRatio = 0.5d;

    @Builder.Default
    @Column(nullable = false)
    private int backlogRiskCount = 4;

    @Builder.Default
    @Column(nullable = false)
    private int backlogCriticalCount = 6;

    @Builder.Default
    @Column(nullable = false)
    private double backlogClearHoursThreshold = 6d;

    @Builder.Default
    @Column(nullable = false)
    private int delayedShipmentCountThreshold = 2;

    @Builder.Default
    @Column(nullable = false)
    private int overdueDispatchCountThreshold = 2;

    @Builder.Default
    @Column(nullable = false)
    private double deliveryDelayToleranceHours = 2d;

    @Builder.Default
    @Column(nullable = false)
    private int highRiskScoreThreshold = 40;

    @Builder.Default
    @Column(nullable = false)
    private int criticalRiskScoreThreshold = 100;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AlertSeverity lowStockSeverity = AlertSeverity.HIGH;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AlertSeverity lowStockCriticalSeverity = AlertSeverity.CRITICAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AlertSeverity depletionRiskSeverity = AlertSeverity.HIGH;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AlertSeverity urgentDepletionRiskSeverity = AlertSeverity.CRITICAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AlertSeverity backlogRiskSeverity = AlertSeverity.HIGH;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AlertSeverity backlogCriticalSeverity = AlertSeverity.CRITICAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AlertSeverity deliveryDelaySeverity = AlertSeverity.HIGH;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AlertSeverity fulfillmentAnomalySeverity = AlertSeverity.CRITICAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private RecommendationPriority lowStockRecommendationPriority = RecommendationPriority.MEDIUM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private RecommendationPriority criticalLowStockRecommendationPriority = RecommendationPriority.CRITICAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private RecommendationPriority depletionRiskRecommendationPriority = RecommendationPriority.MEDIUM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private RecommendationPriority urgentDepletionRiskRecommendationPriority = RecommendationPriority.HIGH;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private RecommendationPriority backlogRecommendationPriority = RecommendationPriority.MEDIUM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private RecommendationPriority deliveryDelayRecommendationPriority = RecommendationPriority.HIGH;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private RecommendationPriority fulfillmentAnomalyRecommendationPriority = RecommendationPriority.CRITICAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ScenarioReviewPriority escalatedApprovalMinimumPriority = ScenarioReviewPriority.CRITICAL;

    @Builder.Default
    @Column(nullable = false)
    private int reviewHoursMedium = 8;

    @Builder.Default
    @Column(nullable = false)
    private int reviewHoursHigh = 4;

    @Builder.Default
    @Column(nullable = false)
    private int reviewHoursCritical = 2;

    @Builder.Default
    @Column(nullable = false)
    private int finalApprovalHoursMedium = 4;

    @Builder.Default
    @Column(nullable = false)
    private int finalApprovalHoursHigh = 2;

    @Builder.Default
    @Column(nullable = false)
    private int finalApprovalHoursCritical = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SynapseAccessRole reviewOwnerRole = SynapseAccessRole.REVIEW_OWNER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SynapseAccessRole finalApproverRole = SynapseAccessRole.FINAL_APPROVER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SynapseAccessRole escalationOwnerRole = SynapseAccessRole.ESCALATION_OWNER;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        applyPolicyDefaults();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        applyPolicyDefaults();
        updatedAt = Instant.now();
    }

    @PostLoad
    void onLoad() {
        applyPolicyDefaults();
    }

    private void applyPolicyDefaults() {
        if (lowStockSeverity == null) {
            lowStockSeverity = AlertSeverity.HIGH;
        }
        if (lowStockCriticalSeverity == null) {
            lowStockCriticalSeverity = AlertSeverity.CRITICAL;
        }
        if (depletionRiskSeverity == null) {
            depletionRiskSeverity = AlertSeverity.HIGH;
        }
        if (urgentDepletionRiskSeverity == null) {
            urgentDepletionRiskSeverity = AlertSeverity.CRITICAL;
        }
        if (backlogRiskSeverity == null) {
            backlogRiskSeverity = AlertSeverity.HIGH;
        }
        if (backlogCriticalSeverity == null) {
            backlogCriticalSeverity = AlertSeverity.CRITICAL;
        }
        if (deliveryDelaySeverity == null) {
            deliveryDelaySeverity = AlertSeverity.HIGH;
        }
        if (fulfillmentAnomalySeverity == null) {
            fulfillmentAnomalySeverity = AlertSeverity.CRITICAL;
        }
        if (lowStockRecommendationPriority == null) {
            lowStockRecommendationPriority = RecommendationPriority.MEDIUM;
        }
        if (criticalLowStockRecommendationPriority == null) {
            criticalLowStockRecommendationPriority = RecommendationPriority.CRITICAL;
        }
        if (depletionRiskRecommendationPriority == null) {
            depletionRiskRecommendationPriority = RecommendationPriority.MEDIUM;
        }
        if (urgentDepletionRiskRecommendationPriority == null) {
            urgentDepletionRiskRecommendationPriority = RecommendationPriority.HIGH;
        }
        if (backlogRecommendationPriority == null) {
            backlogRecommendationPriority = RecommendationPriority.MEDIUM;
        }
        if (deliveryDelayRecommendationPriority == null) {
            deliveryDelayRecommendationPriority = RecommendationPriority.HIGH;
        }
        if (fulfillmentAnomalyRecommendationPriority == null) {
            fulfillmentAnomalyRecommendationPriority = RecommendationPriority.CRITICAL;
        }
        if (escalatedApprovalMinimumPriority == null) {
            escalatedApprovalMinimumPriority = ScenarioReviewPriority.CRITICAL;
        }
    }
}
