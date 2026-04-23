package com.synapsecore.audit;

import com.synapsecore.domain.dto.AuditLogResponse;
import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.service.IdentitySequenceMigrationService;
import com.synapsecore.tenant.TenantContextService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final RequestTraceContext requestTraceContext;
    private final TenantContextService tenantContextService;
    private final IdentitySequenceMigrationService identitySequenceMigrationService;

    public void recordSuccess(String action,
                              String actor,
                              String source,
                              String targetType,
                              String targetRef,
                              String details) {
        record(null, action, actor, source, targetType, targetRef, AuditStatus.SUCCESS, details);
    }

    public void recordSuccessForTenant(String tenantCode,
                                       String action,
                                       String actor,
                                       String source,
                                       String targetType,
                                       String targetRef,
                                       String details) {
        record(tenantCode, action, actor, source, targetType, targetRef, AuditStatus.SUCCESS, details);
    }

    public void recordFailure(String action,
                              String actor,
                              String source,
                              String targetType,
                              String targetRef,
                              String details) {
        record(null, action, actor, source, targetType, targetRef, AuditStatus.FAILURE, details);
    }

    public void recordFailureForTenant(String tenantCode,
                                       String action,
                                       String actor,
                                       String source,
                                       String targetType,
                                       String targetRef,
                                       String details) {
        record(tenantCode, action, actor, source, targetType, targetRef, AuditStatus.FAILURE, details);
    }

    public List<AuditLogResponse> getRecentAuditLogs() {
        return auditLogRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(
                tenantContextService.getCurrentTenantCodeOrDefault())
            .stream()
            .map(log -> new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getActor(),
                log.getSource(),
                log.getTargetType(),
                log.getTargetRef(),
                log.getStatus(),
                log.getDetails(),
                log.getRequestId(),
                log.getCreatedAt()
            ))
            .toList();
    }

    private void record(String tenantCode,
                        String action,
                        String actor,
                        String source,
                        String targetType,
                        String targetRef,
                        AuditStatus status,
                        String details) {
        AuditLog logEntry = AuditLog.builder()
            .tenantCode(resolveTenantCode(tenantCode))
            .action(action)
            .actor(actor)
            .source(source)
            .targetType(targetType)
            .targetRef(targetRef)
            .status(status)
            .details(details)
            .requestId(requestTraceContext.getRequiredRequestId())
            .build();
        try {
            auditLogRepository.save(logEntry);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Audit log persistence conflicted; synchronizing core identity sequences and retrying once.");
            identitySequenceMigrationService.synchronizeCoreIdentitySequences();
            auditLogRepository.save(logEntry);
        }
    }

    private String resolveTenantCode(String explicitTenantCode) {
        if (explicitTenantCode != null && !explicitTenantCode.isBlank()) {
            return explicitTenantCode.trim();
        }
        String traceTenantCode = requestTraceContext.getCurrentTenant()
            .filter(tenantCode -> !tenantCode.isBlank())
            .filter(tenantCode -> !RequestTraceContext.MISSING_TENANT_CONTEXT.equalsIgnoreCase(tenantCode))
            .orElse(null);
        if (traceTenantCode != null) {
            return traceTenantCode;
        }

        try {
            return tenantContextService.getCurrentTenantCodeOrDefault();
        } catch (IllegalStateException exception) {
            // Request-level failures can be rejected before a tenant context exists in prod.
            return RequestTraceContext.MISSING_TENANT_CONTEXT;
        }
    }
}
