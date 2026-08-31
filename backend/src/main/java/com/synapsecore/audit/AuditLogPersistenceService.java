package com.synapsecore.audit;

import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.service.CoreIdentityWriteIsolationService;
import com.synapsecore.platform.PlatformMetadataChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogPersistenceService {

    private final AuditLogRepository auditLogRepository;
    private final RequestTraceContext requestTraceContext;
    private final CoreIdentityWriteIsolationService coreIdentityWriteIsolationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void recordForTenant(String tenantCode,
                                String action,
                                String actor,
                                String source,
                                String targetType,
                                String targetRef,
                                AuditStatus status,
                                String details) {
        AuditLog logEntry = AuditLog.builder()
            .tenantCode(tenantCode)
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
        applicationEventPublisher.publishEvent(new PlatformMetadataChangedEvent(logEntry.getCreatedAt()));
    }
}
