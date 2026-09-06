package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.ScenarioRun;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioRunType;
import java.time.Instant;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScenarioRunRepository extends JpaRepository<ScenarioRun, Long>, JpaSpecificationExecutor<ScenarioRun> {

    List<ScenarioRun> findTop12ByOrderByCreatedAtDescIdDesc();

    List<ScenarioRun> findTop12ByOrderByCreatedAtDesc();

    List<ScenarioRun> findTop12ByTenant_CodeIgnoreCaseOrderByCreatedAtDescIdDesc(String tenantCode);

    List<ScenarioRun> findTop12ByTenant_CodeIgnoreCaseOrderByCreatedAtDesc(String tenantCode);

    List<ScenarioRun> findTop20ByTenant_CodeIgnoreCaseAndApprovalStatusAndApprovalPolicyAndApprovalStageAndApprovalDueAtLessThanEqualAndSlaEscalatedAtIsNullOrderByCreatedAtDescIdDesc(
        String tenantCode,
        ScenarioApprovalStatus approvalStatus,
        ScenarioApprovalPolicy approvalPolicy,
        ScenarioApprovalStage approvalStage,
        Instant approvalDueAt
    );

    List<ScenarioRun> findTop20ByApprovalStatusAndApprovalPolicyAndApprovalStageAndApprovalDueAtLessThanEqualAndSlaEscalatedAtIsNullOrderByCreatedAtDesc(
        ScenarioApprovalStatus approvalStatus,
        ScenarioApprovalPolicy approvalPolicy,
        ScenarioApprovalStage approvalStage,
        Instant approvalDueAt
    );

    List<ScenarioRun> findTop20ByTenant_CodeIgnoreCaseAndApprovalStatusAndApprovalPolicyAndApprovalStageAndApprovalDueAtLessThanEqualAndSlaEscalatedAtIsNullOrderByCreatedAtDesc(
        String tenantCode,
        ScenarioApprovalStatus approvalStatus,
        ScenarioApprovalPolicy approvalPolicy,
        ScenarioApprovalStage approvalStage,
        Instant approvalDueAt
    );

    List<ScenarioRun> findTop12ByTypeAndApprovalStatusAndSlaEscalatedAtIsNotNullAndSlaAcknowledgedAtIsNullOrderBySlaEscalatedAtDesc(
        ScenarioRunType type,
        ScenarioApprovalStatus approvalStatus
    );

    List<ScenarioRun> findTop12ByTenant_CodeIgnoreCaseAndTypeAndApprovalStatusAndSlaEscalatedAtIsNotNullAndSlaAcknowledgedAtIsNullOrderBySlaEscalatedAtDescIdDesc(
        String tenantCode,
        ScenarioRunType type,
        ScenarioApprovalStatus approvalStatus
    );

    List<ScenarioRun> findTop12ByTypeAndSlaAcknowledgedAtIsNotNullOrderBySlaAcknowledgedAtDesc(
        ScenarioRunType type
    );

    List<ScenarioRun> findTop12ByTenant_CodeIgnoreCaseAndTypeAndSlaAcknowledgedAtIsNotNullOrderBySlaAcknowledgedAtDescIdDesc(
        String tenantCode,
        ScenarioRunType type
    );

    java.util.Optional<ScenarioRun> findByTenant_CodeIgnoreCaseAndId(String tenantCode, Long id);

    java.util.Optional<ScenarioRun> findFirstByTenant_CodeIgnoreCaseAndRevisionOfScenarioRunIdOrderByIdAsc(
        String tenantCode, Long revisionOfScenarioRunId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select run from ScenarioRun run where upper(run.tenant.code) = upper(:tenantCode) and run.id = :id")
    java.util.Optional<ScenarioRun> findForRevisionUpdate(@Param("tenantCode") String tenantCode, @Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select run from ScenarioRun run where upper(run.tenant.code) = upper(:tenantCode) and run.id = :id")
    java.util.Optional<ScenarioRun> findForSlaUpdate(@Param("tenantCode") String tenantCode, @Param("id") Long id);

    long countByTenant_CodeIgnoreCaseAndApprovalStatus(String tenantCode, ScenarioApprovalStatus approvalStatus);

    boolean existsByTenant_CodeIgnoreCaseAndWarehouseCodeIgnoreCaseAndApprovalStatusIn(String tenantCode,
                                                                                         String warehouseCode,
                                                                                         List<ScenarioApprovalStatus> statuses);
}
