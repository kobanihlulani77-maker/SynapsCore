package com.synapsecore.fulfillment;

import com.synapsecore.alert.AlertService;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.decision.RecommendationService;
import com.synapsecore.domain.dto.FulfillmentOverviewResponse;
import com.synapsecore.domain.dto.FulfillmentStatusResponse;
import com.synapsecore.domain.dto.FulfillmentUpdateRequest;
import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.tenant.TenantContextService;
import com.synapsecore.tenant.TenantScopeGuard;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FulfillmentService {

    private static final List<FulfillmentStatus> BACKLOG_STATUSES = List.of(
        FulfillmentStatus.QUEUED,
        FulfillmentStatus.PICKING,
        FulfillmentStatus.PACKED
    );
    private static final List<FulfillmentStatus> ACTIVE_STATUSES = List.of(
        FulfillmentStatus.QUEUED,
        FulfillmentStatus.PICKING,
        FulfillmentStatus.PACKED,
        FulfillmentStatus.DISPATCHED,
        FulfillmentStatus.DELAYED,
        FulfillmentStatus.EXCEPTION
    );
    private static final List<FulfillmentStatus> BACKLOG_CLEARING_STATUSES = List.of(
        FulfillmentStatus.DISPATCHED,
        FulfillmentStatus.DELIVERED
    );

    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final RecommendationService recommendationService;
    private final AlertService alertService;
    private final BusinessEventService businessEventService;
    private final AuditLogService auditLogService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final TenantContextService tenantContextService;
    private final TenantScopeGuard tenantScopeGuard;
    private final OperationalMetricsService operationalMetricsService;

    @Value("${synapsecore.fulfillment.default-dispatch-hours:2}")
    private long defaultDispatchHours;

    @Value("${synapsecore.fulfillment.default-delivery-hours:24}")
    private long defaultDeliveryHours;

    @Value("${synapsecore.fulfillment.analysis-window-hours:6}")
    private long analysisWindowHours;

    @Transactional
    public FulfillmentTask initializeForOrder(CustomerOrder order, String source) {
        tenantScopeGuard.requireCustomerOrder(order, "fulfillment initialization");
        Instant queuedAt = order.getCreatedAt() == null ? Instant.now() : order.getCreatedAt();
        FulfillmentTask task = fulfillmentTaskRepository.save(FulfillmentTask.builder()
            .tenant(order.getTenant())
            .customerOrder(order)
            .warehouse(order.getWarehouse())
            .status(FulfillmentStatus.QUEUED)
            .queuedAt(queuedAt)
            .promisedDispatchAt(queuedAt.plus(defaultDispatchHours, ChronoUnit.HOURS))
            .expectedDeliveryAt(queuedAt.plus(defaultDeliveryHours, ChronoUnit.HOURS))
            .exceptionCount(0)
            .note("Order queued for fulfillment.")
            .build());
        tenantScopeGuard.requireFulfillmentTask(task, "fulfillment initialization");

        businessEventService.record(
            BusinessEventType.FULFILLMENT_UPDATED,
            source,
            "Order " + order.getExternalOrderId() + " entered the fulfillment queue in " + order.getWarehouse().getCode() + "."
        );
        auditLogService.recordSuccess(
            "FULFILLMENT_QUEUED",
            source,
            source,
            "FulfillmentTask",
            order.getExternalOrderId(),
            "Queued order " + order.getExternalOrderId() + " for fulfillment in " + order.getWarehouse().getCode() + "."
        );

        operationalMetricsService.recordFulfillmentUpdate(order.getTenant().getCode(), task.getStatus(), source);
        evaluateTask(task, source);
        return task;
    }

    @Transactional
    public FulfillmentStatusResponse recordUpdate(FulfillmentUpdateRequest request) {
        return recordUpdate(request, "fulfillment-api");
    }

    @Transactional
    public FulfillmentStatusResponse recordUpdate(FulfillmentUpdateRequest request, String source) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        CustomerOrder order = customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantCode, request.externalOrderId().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + request.externalOrderId()));
        tenantScopeGuard.requireCustomerOrder(order, "fulfillment update");
        FulfillmentTask task = fulfillmentTaskRepository.findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(tenantCode, order.getExternalOrderId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Fulfillment task not found for order " + request.externalOrderId()));
        tenantScopeGuard.requireFulfillmentTask(task, "fulfillment update");

        applyUpdate(task, request);
        order.setStatus(mapOrderStatus(task.getStatus()));
        customerOrderRepository.save(order);
        FulfillmentTask savedTask = fulfillmentTaskRepository.save(task);
        tenantScopeGuard.requireFulfillmentTask(savedTask, "fulfillment update");

        BusinessEventType eventType = switch (savedTask.getStatus()) {
            case DELAYED -> BusinessEventType.DELIVERY_DELAY_REPORTED;
            case EXCEPTION -> BusinessEventType.FULFILLMENT_ANOMALY_DETECTED;
            default -> BusinessEventType.FULFILLMENT_UPDATED;
        };
        businessEventService.record(
            eventType,
            source,
            "Order " + order.getExternalOrderId() + " moved to " + savedTask.getStatus()
                + " in " + savedTask.getWarehouse().getCode() + "."
        );
        auditLogService.recordSuccess(
            "FULFILLMENT_UPDATED",
            source,
            source,
            "FulfillmentTask",
            order.getExternalOrderId(),
            "Updated fulfillment for " + order.getExternalOrderId() + " to " + savedTask.getStatus() + "."
        );

        operationalMetricsService.recordFulfillmentUpdate(tenantCode, savedTask.getStatus(), source);
        evaluateTask(savedTask, source);
        operationalStateChangePublisher.publish(OperationalUpdateType.FULFILLMENT_UPDATE, source);
        return toResponse(savedTask, buildWarehouseAssessment(savedTask, Instant.now()));
    }

    @Transactional(readOnly = true)
    public FulfillmentOverviewResponse getOverview() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        List<FulfillmentTask> activeTasks = fulfillmentTaskRepository
            .findAllByTenant_CodeIgnoreCaseAndStatusInOrderByUpdatedAtDesc(tenantCode, ACTIVE_STATUSES);
        if (activeTasks.isEmpty()) {
            return new FulfillmentOverviewResponse(0, 0, 0, 0, List.of(), Instant.now());
        }

        Instant now = Instant.now();
        Map<Long, FulfillmentAssessment> assessmentByTaskId = new LinkedHashMap<>();
        for (FulfillmentTask task : activeTasks) {
            assessmentByTaskId.put(task.getId(), buildWarehouseAssessment(task, now));
        }

        long backlogCount = activeTasks.stream().filter(task -> BACKLOG_STATUSES.contains(task.getStatus())).count();
        long overdueDispatchCount = activeTasks.stream()
            .filter(task -> BACKLOG_STATUSES.contains(task.getStatus()))
            .filter(task -> task.getPromisedDispatchAt() != null && task.getPromisedDispatchAt().isBefore(now))
            .count();
        long delayedShipmentCount = activeTasks.stream().filter(task -> isDeliveryDelayed(task, now)).count();
        long atRiskCount = assessmentByTaskId.values().stream()
            .filter(assessment -> assessment.backlogRisk() || assessment.deliveryDelayRisk() || assessment.anomalyDetected())
            .count();

        List<FulfillmentStatusResponse> responses = activeTasks.stream()
            .sorted(Comparator.comparing(FulfillmentTask::getUpdatedAt).reversed())
            .limit(12)
            .map(task -> toResponse(task, assessmentByTaskId.get(task.getId())))
            .toList();

        return new FulfillmentOverviewResponse(
            backlogCount,
            overdueDispatchCount,
            delayedShipmentCount,
            atRiskCount,
            responses,
            now
        );
    }

    @Transactional
    public Optional<FulfillmentStatusResponse> advanceSimulationFlow() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        return fulfillmentTaskRepository.findTop1ByTenant_CodeIgnoreCaseAndStatusInOrderByUpdatedAtAsc(tenantCode, ACTIVE_STATUSES)
            .map(task -> {
                FulfillmentStatus nextStatus = nextSimulationStatus(task.getStatus());
                Instant now = Instant.now();
                FulfillmentUpdateRequest request = new FulfillmentUpdateRequest(
                    task.getCustomerOrder().getExternalOrderId(),
                    nextStatus,
                    nextStatus == FulfillmentStatus.DISPATCHED || task.getCarrier() != null ? defaultCarrier(task) : null,
                    nextStatus == FulfillmentStatus.DISPATCHED || task.getTrackingReference() != null ? defaultTracking(task) : null,
                    task.getPromisedDispatchAt(),
                    nextStatus == FulfillmentStatus.DISPATCHED && task.getExpectedDeliveryAt() == null
                        ? now.plus(defaultDeliveryHours, ChronoUnit.HOURS)
                        : task.getExpectedDeliveryAt(),
                    now,
                    buildSimulationNote(nextStatus)
                );
                return recordUpdate(request, "simulation");
            });
    }

    private void applyUpdate(FulfillmentTask task, FulfillmentUpdateRequest request) {
        Instant occurredAt = request.occurredAt() == null ? Instant.now() : request.occurredAt();
        task.setStatus(request.status());
        task.setCarrier(request.carrier() == null || request.carrier().isBlank() ? task.getCarrier() : request.carrier().trim());
        task.setTrackingReference(request.trackingReference() == null || request.trackingReference().isBlank()
            ? task.getTrackingReference()
            : request.trackingReference().trim());
        if (request.promisedDispatchAt() != null) {
            task.setPromisedDispatchAt(request.promisedDispatchAt());
        }
        if (request.expectedDeliveryAt() != null) {
            task.setExpectedDeliveryAt(request.expectedDeliveryAt());
        }
        if (request.note() != null && !request.note().isBlank()) {
            task.setNote(request.note().trim());
        }

        switch (request.status()) {
            case QUEUED -> {
                task.setQueuedAt(occurredAt);
                if (task.getPromisedDispatchAt() == null) {
                    task.setPromisedDispatchAt(occurredAt.plus(defaultDispatchHours, ChronoUnit.HOURS));
                }
                if (task.getExpectedDeliveryAt() == null) {
                    task.setExpectedDeliveryAt(occurredAt.plus(defaultDeliveryHours, ChronoUnit.HOURS));
                }
            }
            case PICKING, PACKED -> {
                if (task.getPromisedDispatchAt() == null) {
                    task.setPromisedDispatchAt(task.getQueuedAt().plus(defaultDispatchHours, ChronoUnit.HOURS));
                }
            }
            case DISPATCHED -> {
                task.setDispatchedAt(occurredAt);
                if (task.getExpectedDeliveryAt() == null) {
                    task.setExpectedDeliveryAt(occurredAt.plus(defaultDeliveryHours, ChronoUnit.HOURS));
                }
            }
            case DELAYED -> {
                if (task.getExpectedDeliveryAt() == null) {
                    task.setExpectedDeliveryAt(occurredAt.plus(defaultDeliveryHours, ChronoUnit.HOURS));
                }
            }
            case DELIVERED -> {
                if (task.getDispatchedAt() == null) {
                    task.setDispatchedAt(occurredAt);
                }
                task.setDeliveredAt(occurredAt);
            }
            case EXCEPTION -> task.setExceptionCount(task.getExceptionCount() + 1);
            default -> {
            }
        }
    }

    private void evaluateTask(FulfillmentTask task, String source) {
        FulfillmentAssessment assessment = buildWarehouseAssessment(task, Instant.now());
        Recommendation recommendation = recommendationService.createForFulfillment(task, assessment, source);
        alertService.syncFulfillmentAlerts(task, assessment, recommendation, source);
    }

    private FulfillmentAssessment buildWarehouseAssessment(FulfillmentTask task, Instant now) {
        String tenantCode = task.getTenant().getCode();
        Long warehouseId = task.getWarehouse().getId();
        List<FulfillmentTask> warehouseTasks = fulfillmentTaskRepository
            .findAllByTenant_CodeIgnoreCaseAndStatusInOrderByUpdatedAtDesc(tenantCode, ACTIVE_STATUSES)
            .stream()
            .filter(candidate -> candidate.getWarehouse().getId().equals(warehouseId))
            .toList();

        long backlogCount = warehouseTasks.stream().filter(candidate -> BACKLOG_STATUSES.contains(candidate.getStatus())).count();
        long overdueDispatchCount = warehouseTasks.stream()
            .filter(candidate -> BACKLOG_STATUSES.contains(candidate.getStatus()))
            .filter(candidate -> candidate.getPromisedDispatchAt() != null && candidate.getPromisedDispatchAt().isBefore(now))
            .count();
        long delayedShipmentCount = warehouseTasks.stream().filter(candidate -> isDeliveryDelayed(candidate, now)).count();

        Instant analysisStart = now.minus(analysisWindowHours, ChronoUnit.HOURS);
        long recentOrderIntake = customerOrderRepository.countByTenant_CodeIgnoreCaseAndWarehouse_IdAndCreatedAtAfter(
            tenantCode,
            warehouseId,
            analysisStart
        );
        long recentThroughput = fulfillmentTaskRepository.countByTenant_CodeIgnoreCaseAndWarehouse_IdAndStatusInAndUpdatedAtAfter(
            tenantCode,
            warehouseId,
            BACKLOG_CLEARING_STATUSES,
            analysisStart
        );

        double backlogGrowthPerHour = ((double) recentOrderIntake - recentThroughput) / Math.max(analysisWindowHours, 1);
        Double estimatedBacklogClearHours = null;
        if (backlogCount > 0 && recentThroughput > 0) {
            double throughputPerHour = (double) recentThroughput / Math.max(analysisWindowHours, 1);
            estimatedBacklogClearHours = throughputPerHour > 0 ? backlogCount / throughputPerHour : null;
        } else if (backlogCount > 0 && backlogGrowthPerHour > 0) {
            estimatedBacklogClearHours = backlogCount / backlogGrowthPerHour;
        }

        Double hoursUntilDispatchDue = task.getPromisedDispatchAt() == null || !BACKLOG_STATUSES.contains(task.getStatus())
            ? null
            : hoursBetween(now, task.getPromisedDispatchAt());
        Double hoursUntilDeliveryDue = task.getExpectedDeliveryAt() == null || task.getStatus() == FulfillmentStatus.DELIVERED
            ? null
            : hoursBetween(now, task.getExpectedDeliveryAt());

        boolean backlogRisk = overdueDispatchCount > 0
            || backlogCount >= 4
            || backlogGrowthPerHour > 1.0
            || (estimatedBacklogClearHours != null && estimatedBacklogClearHours >= 6);
        boolean deliveryDelayRisk = isDeliveryDelayed(task, now);
        boolean anomalyDetected = task.getStatus() == FulfillmentStatus.EXCEPTION
            || delayedShipmentCount >= 2
            || overdueDispatchCount >= 2
            || (backlogCount >= 6 && backlogGrowthPerHour > 0.5)
            || task.getExceptionCount() > 0;

        AlertSeverity severity;
        if (anomalyDetected || overdueDispatchCount >= 2 || (hoursUntilDeliveryDue != null && hoursUntilDeliveryDue <= -2)) {
            severity = AlertSeverity.CRITICAL;
        } else if (backlogRisk || deliveryDelayRisk) {
            severity = AlertSeverity.HIGH;
        } else {
            severity = AlertSeverity.MEDIUM;
        }

        String riskLevel = severity == AlertSeverity.CRITICAL ? "critical" : severity == AlertSeverity.HIGH ? "high" : "stable";

        String impactSummary;
        if (anomalyDetected) {
            impactSummary = "Repeated logistics exceptions or stacked late shipments are forming in "
                + task.getWarehouse().getCode()
                + ". Backlog is " + backlogCount + " and delayed deliveries are " + delayedShipmentCount + ".";
        } else if (deliveryDelayRisk) {
            impactSummary = "Delivery pressure is rising for " + task.getWarehouse().getCode()
                + ". " + formatSignedHours(hoursUntilDeliveryDue, "delivery SLA") + ".";
        } else if (backlogRisk) {
            impactSummary = "Fulfillment backlog is building in " + task.getWarehouse().getCode()
                + " with " + backlogCount + " open warehouse tasks"
                + (estimatedBacklogClearHours != null
                    ? " and roughly " + String.format(Locale.US, "%.1f", estimatedBacklogClearHours) + " hours to clear at the current pace."
                    : ".");
        } else {
            impactSummary = "Fulfillment is operating within the current dispatch and delivery SLA lane.";
        }

        return new FulfillmentAssessment(
            backlogCount,
            overdueDispatchCount,
            delayedShipmentCount,
            backlogGrowthPerHour,
            estimatedBacklogClearHours,
            hoursUntilDispatchDue,
            hoursUntilDeliveryDue,
            backlogRisk,
            deliveryDelayRisk,
            anomalyDetected,
            severity,
            riskLevel,
            impactSummary
        );
    }

    private FulfillmentStatusResponse toResponse(FulfillmentTask task, FulfillmentAssessment assessment) {
        return new FulfillmentStatusResponse(
            task.getId(),
            task.getCustomerOrder().getExternalOrderId(),
            task.getCustomerOrder().getStatus(),
            task.getStatus(),
            task.getWarehouse().getCode(),
            task.getWarehouse().getName(),
            task.getCustomerOrder().getItems().stream().mapToInt(item -> item.getQuantity()).sum(),
            task.getCarrier(),
            task.getTrackingReference(),
            task.getQueuedAt(),
            task.getPromisedDispatchAt(),
            task.getExpectedDeliveryAt(),
            task.getDispatchedAt(),
            task.getDeliveredAt(),
            assessment.backlogGrowthPerHour(),
            assessment.estimatedBacklogClearHours(),
            assessment.hoursUntilDispatchDue(),
            assessment.hoursUntilDeliveryDue(),
            assessment.backlogRisk(),
            assessment.deliveryDelayRisk(),
            assessment.anomalyDetected(),
            assessment.riskLevel(),
            assessment.impactSummary(),
            task.getUpdatedAt()
        );
    }

    private OrderStatus mapOrderStatus(FulfillmentStatus status) {
        return switch (status) {
            case QUEUED -> OrderStatus.RECEIVED;
            case DELIVERED -> OrderStatus.COMPLETED;
            default -> OrderStatus.PROCESSING;
        };
    }

    private boolean isDeliveryDelayed(FulfillmentTask task, Instant now) {
        return task.getStatus() == FulfillmentStatus.DELAYED
            || task.getStatus() == FulfillmentStatus.EXCEPTION
            || (task.getStatus() != FulfillmentStatus.DELIVERED
                && task.getExpectedDeliveryAt() != null
                && task.getExpectedDeliveryAt().isBefore(now));
    }

    private Double hoursBetween(Instant from, Instant to) {
        return Duration.between(from, to).toMinutes() / 60.0;
    }

    private String formatSignedHours(Double hours, String label) {
        if (hours == null) {
            return "Monitoring " + label + ".";
        }
        if (hours < 0) {
            return String.format(Locale.US, "%.1f hours overdue on %s", Math.abs(hours), label);
        }
        return String.format(Locale.US, "%.1f hours remaining before %s", hours, label);
    }

    private FulfillmentStatus nextSimulationStatus(FulfillmentStatus current) {
        return switch (current) {
            case QUEUED -> FulfillmentStatus.PICKING;
            case PICKING -> FulfillmentStatus.PACKED;
            case PACKED -> FulfillmentStatus.DISPATCHED;
            case DISPATCHED -> ThreadLocalRandom.current().nextBoolean() ? FulfillmentStatus.DELIVERED : FulfillmentStatus.DELAYED;
            case DELAYED -> FulfillmentStatus.DELIVERED;
            case EXCEPTION -> FulfillmentStatus.PICKING;
            case DELIVERED -> FulfillmentStatus.DELIVERED;
        };
    }

    private String defaultCarrier(FulfillmentTask task) {
        return task.getCarrier() != null && !task.getCarrier().isBlank() ? task.getCarrier() : "Synapse Courier";
    }

    private String defaultTracking(FulfillmentTask task) {
        return task.getTrackingReference() != null && !task.getTrackingReference().isBlank()
            ? task.getTrackingReference()
            : "TRK-" + task.getCustomerOrder().getExternalOrderId();
    }

    private String buildSimulationNote(FulfillmentStatus nextStatus) {
        return switch (nextStatus) {
            case PICKING -> "Simulation advanced the order into active warehouse picking.";
            case PACKED -> "Simulation packed the order for dispatch handoff.";
            case DISPATCHED -> "Simulation dispatched the order into the delivery lane.";
            case DELAYED -> "Simulation detected a delivery slowdown and marked the lane delayed.";
            case DELIVERED -> "Simulation completed delivery for the order.";
            case EXCEPTION -> "Simulation triggered a logistics exception for investigation.";
            default -> "Simulation refreshed the fulfillment lane.";
        };
    }
}
