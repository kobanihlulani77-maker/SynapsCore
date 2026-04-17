package com.synapsecore.observability;

import com.synapsecore.domain.dto.SystemMetricsSummary;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.IntegrationImportStatus;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.event.OperationalUpdateType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class OperationalMetricsService {

    private static final List<FulfillmentStatus> BACKLOG_STATUSES = List.of(
        FulfillmentStatus.QUEUED,
        FulfillmentStatus.PICKING,
        FulfillmentStatus.PACKED
    );

    private final MeterRegistry meterRegistry;

    public OperationalMetricsService(MeterRegistry meterRegistry,
                                     AlertRepository alertRepository,
                                     FulfillmentTaskRepository fulfillmentTaskRepository,
                                     IntegrationReplayRecordRepository integrationReplayRecordRepository,
                                     OperationalDispatchWorkItemRepository operationalDispatchWorkItemRepository) {
        this.meterRegistry = meterRegistry;

        Gauge.builder("synapsecore.alerts.active", alertRepository, repository -> repository.countByStatus(AlertStatus.ACTIVE))
            .description("Current active alerts across the platform.")
            .register(meterRegistry);
        Gauge.builder(
                "synapsecore.fulfillment.backlog",
                fulfillmentTaskRepository,
                repository -> repository.countByStatusIn(BACKLOG_STATUSES)
            )
            .description("Current fulfillment backlog across all tenants.")
            .register(meterRegistry);
        Gauge.builder(
                "synapsecore.fulfillment.delayed",
                fulfillmentTaskRepository,
                repository -> repository.countByStatusIn(List.of(FulfillmentStatus.DELAYED, FulfillmentStatus.EXCEPTION))
            )
            .description("Current delayed or exceptional fulfillment lanes across all tenants.")
            .register(meterRegistry);
        Gauge.builder(
                "synapsecore.integration.replay.backlog",
                integrationReplayRecordRepository,
                repository -> repository.countByStatusIn(List.of(
                    IntegrationReplayStatus.PENDING,
                    IntegrationReplayStatus.REPLAY_FAILED,
                    IntegrationReplayStatus.DEAD_LETTERED
                ))
            )
            .description("Current integration replay backlog across all tenants.")
            .register(meterRegistry);
        Gauge.builder(
                "synapsecore.dispatch.queue.backlog",
                operationalDispatchWorkItemRepository,
                repository -> repository.countByStatusIn(List.of(OperationalDispatchStatus.PENDING, OperationalDispatchStatus.PROCESSING))
            )
            .description("Current persisted operational dispatch queue backlog.")
            .register(meterRegistry);
        Gauge.builder(
                "synapsecore.dispatch.queue.failed",
                operationalDispatchWorkItemRepository,
                repository -> repository.countByStatusIn(List.of(OperationalDispatchStatus.FAILED))
            )
            .description("Current failed operational dispatch work items awaiting investigation.")
            .register(meterRegistry);
    }

    public void recordOrderIngested(String tenantCode, String source) {
        meterRegistry.counter("synapsecore.orders.ingested", "tenant", normalizeTenantCode(tenantCode), "source", normalizeTag(source))
            .increment();
    }

    public void recordFulfillmentUpdate(String tenantCode, FulfillmentStatus status, String source) {
        meterRegistry.counter(
                "synapsecore.fulfillment.updates",
                "tenant",
                normalizeTenantCode(tenantCode),
                "status",
                status.name(),
                "source",
                normalizeTag(source)
            )
            .increment();
    }

    public void recordIntegrationImportRun(String tenantCode, String sourceSystem, IntegrationImportStatus status) {
        meterRegistry.counter(
                "synapsecore.integration.import.runs",
                "tenant",
                normalizeTenantCode(tenantCode),
                "source",
                normalizeTag(sourceSystem),
                "status",
                status.name()
            )
            .increment();
    }

    public void recordReplayAttempt(String tenantCode, boolean success) {
        meterRegistry.counter(
                "synapsecore.integration.replay.attempts",
                "tenant",
                normalizeTenantCode(tenantCode),
                "outcome",
                success ? "SUCCESS" : "FAILURE"
            )
            .increment();
    }

    public void recordDispatchQueued(String tenantCode, OperationalUpdateType updateType) {
        meterRegistry.counter(
                "synapsecore.dispatch.queued",
                "tenant",
                normalizeTenantCode(tenantCode),
                "updateType",
                updateType.name()
            )
            .increment();
    }

    public void recordDispatchProcessed(String tenantCode, OperationalUpdateType updateType) {
        meterRegistry.counter(
                "synapsecore.dispatch.processed",
                "tenant",
                normalizeTenantCode(tenantCode),
                "updateType",
                updateType.name()
            )
            .increment();
    }

    public void recordDispatchFailure(String tenantCode, OperationalUpdateType updateType) {
        meterRegistry.counter(
                "synapsecore.dispatch.failed",
                "tenant",
                normalizeTenantCode(tenantCode),
                "updateType",
                updateType.name()
            )
            .increment();
    }

    public SystemMetricsSummary snapshotForTenant(String tenantCode) {
        String normalizedTenantCode = normalizeTenantCode(tenantCode);
        return new SystemMetricsSummary(
            countForTenant("synapsecore.orders.ingested", normalizedTenantCode),
            countForTenant("synapsecore.fulfillment.updates", normalizedTenantCode),
            countForTenant("synapsecore.integration.import.runs", normalizedTenantCode),
            countForTenant("synapsecore.integration.replay.attempts", normalizedTenantCode),
            countForTenant("synapsecore.dispatch.queued", normalizedTenantCode),
            countForTenant("synapsecore.dispatch.processed", normalizedTenantCode),
            countForTenant("synapsecore.dispatch.failed", normalizedTenantCode)
        );
    }

    private double countForTenant(String metricName, String tenantCode) {
        Counter counter = meterRegistry.find(metricName)
            .tag("tenant", tenantCode)
            .counter();
        return counter == null ? 0.0 : counter.count();
    }

    private String normalizeTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return "UNSCOPED";
        }
        return tenantCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTag(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        return value.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
