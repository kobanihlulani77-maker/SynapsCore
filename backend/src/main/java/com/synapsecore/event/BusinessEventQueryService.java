package com.synapsecore.event;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.domain.dto.BusinessEventResponse;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.tenant.TenantContextService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessEventQueryService {

    private final BusinessEventRepository businessEventRepository;
    private final TenantContextService tenantContextService;
    private final AccessDirectoryService accessDirectoryService;

    public List<BusinessEventResponse> getRecentEvents() {
        if (accessDirectoryService.getCurrentOperator()
            .map(accessDirectoryService::getWarehouseScopes)
            .map(scopes -> !scopes.isEmpty())
            .orElse(false)) {
            return List.of();
        }
        return businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(
                tenantContextService.getCurrentTenantCodeOrDefault())
            .stream()
            .map(event -> new BusinessEventResponse(
                event.getId(),
                event.getEventType(),
                event.getSource(),
                event.getPayloadSummary(),
                event.getCreatedAt()
            ))
            .toList();
    }
}
