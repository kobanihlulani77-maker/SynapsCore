package com.synapsecore.event;

import com.synapsecore.domain.entity.BusinessEvent;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.tenant.TenantContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessEventService {

    private final BusinessEventRepository businessEventRepository;
    private final TenantContextService tenantContextService;

    public void record(BusinessEventType eventType, String source, String payloadSummary) {
        businessEventRepository.save(BusinessEvent.builder()
            .tenantCode(tenantContextService.getCurrentTenantCodeOrDefault())
            .eventType(eventType)
            .source(source)
            .payloadSummary(payloadSummary)
            .build());
    }
}
