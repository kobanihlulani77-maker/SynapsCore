package com.synapsecore.platform;

import com.synapsecore.domain.entity.IntegrationInboundStatus;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.service.SystemRuntimeService;
import com.synapsecore.platform.dto.PlatformActivityResponse;
import com.synapsecore.platform.dto.PlatformOverviewResponse;
import com.synapsecore.platform.dto.PlatformRuntimeResponse;
import com.synapsecore.platform.dto.PlatformTenantSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformControlPlaneService {

    private final TenantRepository tenantRepository;
    private final AccessUserRepository accessUserRepository;
    private final AccessOperatorRepository accessOperatorRepository;
    private final IntegrationConnectorRepository integrationConnectorRepository;
    private final IntegrationInboundRecordRepository integrationInboundRecordRepository;
    private final IntegrationReplayRecordRepository integrationReplayRecordRepository;
    private final AlertRepository alertRepository;
    private final BusinessEventRepository businessEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final SystemRuntimeService systemRuntimeService;

    @Transactional(readOnly = true)
    public PlatformOverviewResponse getOverview() {
        return new PlatformOverviewResponse(
            getRuntime(),
            getTenants(),
            getActivity(),
            Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<PlatformTenantSummary> getTenants() {
        return tenantRepository.findAllByOrderByNameAsc().stream()
            .map(tenant -> {
                String tenantCode = tenant.getCode();
                long disabledConnectors = integrationConnectorRepository
                    .countByTenant_CodeIgnoreCaseAndEnabledFalse(tenantCode);
                long failedInbound = integrationInboundRecordRepository
                    .countByTenantCodeIgnoreCaseAndStatusIn(
                        tenantCode,
                        List.of(IntegrationInboundStatus.REJECTED, IntegrationInboundStatus.REPLAY_QUEUED));
                long replayAttention = integrationReplayRecordRepository
                    .countByTenantCodeIgnoreCaseAndStatusIn(
                        tenantCode,
                        List.of(
                            IntegrationReplayStatus.PENDING,
                            IntegrationReplayStatus.REPLAY_FAILED,
                            IntegrationReplayStatus.DEAD_LETTERED));
                long activeAlerts = alertRepository
                    .countByTenant_CodeIgnoreCaseAndStatus(tenantCode, AlertStatus.ACTIVE);
                String supportState = !tenant.isActive()
                    ? "INACTIVE"
                    : disabledConnectors + failedInbound + replayAttention + activeAlerts > 0 ? "ATTENTION" : "HEALTHY";
                return new PlatformTenantSummary(
                    tenant.getId(),
                    tenantCode,
                    tenant.getName(),
                    tenant.isActive(),
                    accessUserRepository.countByTenant_CodeIgnoreCaseAndActiveTrue(tenantCode),
                    accessOperatorRepository.countByTenant_CodeIgnoreCaseAndActiveTrue(tenantCode),
                    integrationConnectorRepository.countByTenant_CodeIgnoreCase(tenantCode),
                    disabledConnectors,
                    failedInbound,
                    replayAttention,
                    activeAlerts,
                    supportState,
                    tenant.getUpdatedAt()
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformActivityResponse> getActivity() {
        List<PlatformActivityResponse> activity = new ArrayList<>();
        businessEventRepository.findTop20ByOrderByCreatedAtDesc().forEach(event -> activity.add(
            new PlatformActivityResponse(
                normalizeTenantCode(event.getTenantCode()),
                "BUSINESS_EVENT",
                event.getEventType().name(),
                "RECORDED",
                event.getCreatedAt()
            )
        ));
        auditLogRepository.findTop20ByOrderByCreatedAtDesc().forEach(log -> activity.add(
            new PlatformActivityResponse(
                normalizeTenantCode(log.getTenantCode()),
                "AUDIT",
                log.getAction(),
                log.getStatus().name(),
                log.getCreatedAt()
            )
        ));
        return activity.stream()
            .sorted(Comparator.comparing(PlatformActivityResponse::observedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(20)
            .toList();
    }

    public PlatformRuntimeResponse getRuntime() {
        return systemRuntimeService.getPlatformRuntimeStatus();
    }

    private String normalizeTenantCode(String tenantCode) {
        return tenantCode == null || tenantCode.isBlank() ? "PLATFORM" : tenantCode.trim();
    }
}
