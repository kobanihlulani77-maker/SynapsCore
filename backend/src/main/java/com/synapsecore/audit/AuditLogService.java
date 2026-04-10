package com.synapsecore.audit;

import com.synapsecore.domain.dto.AuditLogResponse;
import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.tenant.TenantContextService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final RequestTraceContext requestTraceContext;
    private final TenantContextService tenantContextService;

    public void recordSuccess(String action,
                              String actor,
                              String source,
                              String targetType,
                              String targetRef,
                              String details) {
        record(action, actor, source, targetType, targetRef, AuditStatus.SUCCESS, details);
    }

    public void recordFailure(String action,
                              String actor,
                              String source,
                              String targetType,
                              String targetRef,
                              String details) {
        record(action, actor, source, targetType, targetRef, AuditStatus.FAILURE, details);
    }

    public List<AuditLogResponse> getRecentAuditLogs() {
        return auditLogRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(
                requestTraceContext.getCurrentTenantOrDefault())
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

    private void record(String action,
                        String actor,
                        String source,
                        String targetType,
                        String targetRef,
                        AuditStatus status,
                        String details) {
        auditLogRepository.save(AuditLog.builder()
            .tenantCode(resolveTenantCode())
            .action(action)
            .actor(actor)
            .source(source)
            .targetType(targetType)
            .targetRef(targetRef)
            .status(status)
            .details(details)
            .requestId(requestTraceContext.getRequiredRequestId())
            .build());
    }

    private String resolveTenantCode() {
        return requestTraceContext.getCurrentTenant()
            .filter(tenantCode -> !tenantCode.isBlank())
            .filter(tenantCode -> !RequestTraceContext.DEFAULT_TENANT.equalsIgnoreCase(tenantCode))
            .orElseGet(tenantContextService::getCurrentTenantCodeOrDefault);
    }
}
