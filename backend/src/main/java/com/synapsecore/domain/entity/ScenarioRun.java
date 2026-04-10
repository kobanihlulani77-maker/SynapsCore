package com.synapsecore.domain.entity;

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
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scenario_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScenarioRunType type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2048)
    private String summary;

    @Column(length = 80)
    private String recommendedOption;

    @Column(length = 40)
    private String warehouseCode;

    @Column(length = 4096)
    private String requestPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScenarioApprovalStatus approvalStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ScenarioApprovalPolicy approvalPolicy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScenarioApprovalStage approvalStage;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private ScenarioReviewPriority reviewPriority;

    private Integer riskScore;

    @Column(length = 80)
    private String requestedBy;

    @Column(length = 80)
    private String reviewOwner;

    @Column(length = 80)
    private String finalApprovalOwner;

    private Instant approvalDueAt;

    @Column(length = 80)
    private String slaEscalatedTo;

    private Instant slaEscalatedAt;

    @Column(length = 80)
    private String slaAcknowledgedBy;

    private Instant slaAcknowledgedAt;

    @Column(length = 240)
    private String slaAcknowledgementNote;

    @Column(length = 80)
    private String approvedBy;

    private Instant approvedAt;

    @Column(length = 240)
    private String approvalNote;

    @Column(length = 80)
    private String reviewApprovedBy;

    private Instant reviewApprovedAt;

    @Column(length = 240)
    private String reviewApprovalNote;

    @Column(length = 80)
    private String rejectedBy;

    private Instant rejectedAt;

    @Column(length = 240)
    private String rejectionReason;

    private Long revisionOfScenarioRunId;

    private Integer revisionNumber;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
