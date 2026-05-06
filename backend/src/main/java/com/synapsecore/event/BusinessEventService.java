package com.synapsecore.event;

import com.synapsecore.domain.entity.BusinessEvent;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.service.IdentitySequenceMigrationService;
import com.synapsecore.tenant.TenantContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessEventService {

    private final BusinessEventRepository businessEventRepository;
    private final RequestTraceContext requestTraceContext;
    private final TenantContextService tenantContextService;
    private final IdentitySequenceMigrationService identitySequenceMigrationService;

    public void record(BusinessEventType eventType, String source, String payloadSummary) {
        recordForTenant(null, eventType, source, payloadSummary);
    }

    public void recordForTenant(String tenantCode,
                                BusinessEventType eventType,
                                String source,
                                String payloadSummary) {
        BusinessEvent event = BusinessEvent.builder()
            .tenantCode(resolveTenantCode(tenantCode))
            .eventType(eventType)
            .source(source)
            .payloadSummary(payloadSummary)
            .build();
        try {
            businessEventRepository.save(event);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Business event persistence conflicted; synchronizing core identity sequences and retrying once.");
            identitySequenceMigrationService.synchronizeCoreIdentitySequences();
            businessEventRepository.save(event);
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
            log.warn("Business event is missing tenant context and will be recorded as {}.",
                RequestTraceContext.MISSING_TENANT_CONTEXT);
            return RequestTraceContext.MISSING_TENANT_CONTEXT;
        }
    }
}
