package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.AuditStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop20ByOrderByCreatedAtDesc();

    List<AuditLog> findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(String tenantCode);

    long countByStatusAndCreatedAtAfter(AuditStatus status, Instant createdAt);

    long countByTenantCodeIgnoreCaseAndStatusAndCreatedAtAfter(String tenantCode, AuditStatus status, Instant createdAt);

    Optional<AuditLog> findTopByStatusOrderByCreatedAtDesc(AuditStatus status);

    Optional<AuditLog> findTopByTenantCodeIgnoreCaseAndStatusOrderByCreatedAtDesc(String tenantCode, AuditStatus status);
}
