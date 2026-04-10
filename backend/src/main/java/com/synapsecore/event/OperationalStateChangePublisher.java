package com.synapsecore.event;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.tenant.TenantContextService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationalStateChangePublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final RequestTraceContext requestTraceContext;
    private final TenantContextService tenantContextService;
    private final OperationalDispatchQueueService operationalDispatchQueueService;

    public void publish(OperationalUpdateType updateType, String source) {
        OperationalStateChangedEvent event = new OperationalStateChangedEvent(
            updateType,
            resolveTenantCode(),
            source,
            requestTraceContext.getRequiredRequestId(),
            Instant.now()
        );
        operationalDispatchQueueService.enqueue(event);
        applicationEventPublisher.publishEvent(event);
    }

    private String resolveTenantCode() {
        return requestTraceContext.getCurrentTenant()
            .filter(tenantCode -> !tenantCode.isBlank())
            .filter(tenantCode -> !RequestTraceContext.DEFAULT_TENANT.equalsIgnoreCase(tenantCode))
            .orElseGet(tenantContextService::getCurrentTenantCodeOrDefault);
    }
}
