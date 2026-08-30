package com.synapsecore.audit;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.domain.dto.AuditLogResponse;
import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.service.CoreIdentityWriteIsolationService;
import com.synapsecore.tenant.TenantContextService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final RequestTraceContext requestTraceContext;
    private final TenantContextService tenantContextService;
    private final CoreIdentityWriteIsolationService coreIdentityWriteIsolationService;
    private final AccessDirectoryService accessDirectoryService;

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
        if (isCurrentOperatorWarehouseScoped()) {
            return List.of();
        }
        return auditLogRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(
                tenantContextService.getCurrentTenantCodeOrDefault())
            .stream()
            .filter(this::isVisibleTenantActivity)
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

    private boolean isCurrentOperatorWarehouseScoped() {
        return accessDirectoryService.getCurrentOperator()
            .map(accessDirectoryService::getWarehouseScopes)
            .map(scopes -> !scopes.isEmpty())
            .orElse(false);
    }

    private boolean isVisibleTenantActivity(AuditLog log) {
        String source = log.getSource() == null ? "" : log.getSource().toLowerCase(java.util.Locale.ROOT);
        if (source.contains("/api/platform/") || source.contains("platform-session")
                || source.contains("/favicon") || source.contains("favicon.ico")) {
            return false;
        }
        if (!"REQUEST_REJECTED".equalsIgnoreCase(log.getAction()) || !isForbidden(log.getDetails())) {
            return true;
        }
        Optional<com.synapsecore.domain.entity.AccessOperator> currentOperator = accessDirectoryService.getCurrentOperator();
        return currentOperator.isEmpty()
            || currentOperator.get().getWarehouseScopes() == null
            || currentOperator.get().getWarehouseScopes().isEmpty();
    }

    private boolean isForbidden(String details) {
        return details != null && details.trim().matches("^403\\b.*");
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
        coreIdentityWriteIsolationService.persistWithSequenceRepair(
            "Audit log persistence",
            () -> auditLogRepository.save(logEntry)
        );
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
