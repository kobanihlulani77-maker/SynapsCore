package com.synapsecore.integration;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationImportRun;
import com.synapsecore.domain.entity.IntegrationImportStatus;
import com.synapsecore.domain.repository.IntegrationImportRunRepository;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.integration.dto.IntegrationImportRunResponse;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.tenant.TenantContextService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntegrationImportRunService {

    private final IntegrationImportRunRepository integrationImportRunRepository;
    private final AccessDirectoryService accessDirectoryService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final TenantContextService tenantContextService;
    private final OperationalMetricsService operationalMetricsService;

    public IntegrationImportRun recordRun(String sourceSystem,
                                          IntegrationConnectorType connectorType,
                                          String fileName,
                                          int recordsReceived,
                                          int ordersImported,
                                          int ordersFailed,
                                          String summary) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        IntegrationImportRun run = integrationImportRunRepository.save(IntegrationImportRun.builder()
            .tenantCode(tenantCode)
            .sourceSystem(sourceSystem)
            .connectorType(connectorType)
            .fileName(fileName)
            .recordsReceived(recordsReceived)
            .ordersImported(ordersImported)
            .ordersFailed(ordersFailed)
            .status(resolveStatus(ordersImported, ordersFailed))
            .summary(summary)
            .build());
        operationalMetricsService.recordIntegrationImportRun(tenantCode, sourceSystem, run.getStatus());
        operationalStateChangePublisher.publish(OperationalUpdateType.INTEGRATION_STATE, "integration-import");
        return run;
    }

    public List<IntegrationImportRunResponse> getRecentRuns() {
        // Import runs currently have no authoritative warehouse column. Redact
        // them for scoped operators rather than leaking tenant-wide telemetry.
        if (accessDirectoryService.getCurrentOperator()
            .map(operator -> operator.getWarehouseScopes() != null && !operator.getWarehouseScopes().isEmpty())
            .orElse(false)) {
            return List.of();
        }
        return integrationImportRunRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(
                tenantContextService.getCurrentTenantCodeOrDefault())
            .stream()
            .map(run -> new IntegrationImportRunResponse(
                run.getId(),
                run.getSourceSystem(),
                run.getConnectorType(),
                run.getFileName(),
                run.getRecordsReceived(),
                run.getOrdersImported(),
                run.getOrdersFailed(),
                run.getStatus(),
                run.getSummary(),
                run.getCreatedAt()
            ))
            .toList();
    }

    private IntegrationImportStatus resolveStatus(int ordersImported, int ordersFailed) {
        if (ordersImported == 0) {
            return IntegrationImportStatus.FAILURE;
        }
        if (ordersFailed > 0) {
            return IntegrationImportStatus.PARTIAL_SUCCESS;
        }
        return IntegrationImportStatus.SUCCESS;
    }
}
