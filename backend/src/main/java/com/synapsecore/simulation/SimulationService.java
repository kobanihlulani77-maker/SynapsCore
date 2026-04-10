package com.synapsecore.simulation;

import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderItemRequest;
import com.synapsecore.domain.dto.SimulationStatusResponse;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.service.OrderService;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.fulfillment.FulfillmentService;
import com.synapsecore.tenant.TenantContextService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final SimulationStateService simulationStateService;
    private final InventoryRepository inventoryRepository;
    private final OrderService orderService;
    private final BusinessEventService businessEventService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final AuditLogService auditLogService;
    private final FulfillmentService fulfillmentService;
    private final TenantContextService tenantContextService;

    public SimulationStatusResponse start() {
        boolean changed = simulationStateService.activate();
        if (changed) {
            String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
            businessEventService.record(BusinessEventType.SIMULATION_STARTED, "simulation", "Simulation mode started.");
            operationalStateChangePublisher.publish(OperationalUpdateType.SIMULATION_STATE, "simulation");
            auditLogService.recordSuccess(
                "SIMULATION_STARTED",
                "simulation-control",
                "simulation",
                "SimulationMode",
                tenantCode,
                "Simulation mode activated for tenant " + tenantCode + "."
            );
        }
        return simulationStateService.getStatus();
    }

    public SimulationStatusResponse stop() {
        boolean changed = simulationStateService.deactivate();
        if (changed) {
            String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
            businessEventService.record(BusinessEventType.SIMULATION_STOPPED, "simulation", "Simulation mode stopped.");
            operationalStateChangePublisher.publish(OperationalUpdateType.SIMULATION_STATE, "simulation");
            auditLogService.recordSuccess(
                "SIMULATION_STOPPED",
                "simulation-control",
                "simulation",
                "SimulationMode",
                tenantCode,
                "Simulation mode deactivated for tenant " + tenantCode + "."
            );
        }
        return simulationStateService.getStatus();
    }

    public SimulationStatusResponse status() {
        return simulationStateService.getStatus();
    }

    @Scheduled(fixedDelayString = "${synapsecore.simulation.interval-ms}")
    public void generateSimulatedOrder() {
        if (!simulationStateService.isActive()) {
            return;
        }

        List<Inventory> availableInventory = inventoryRepository.findAvailableInventoryByTenantCode(
            tenantContextService.getCurrentTenantCodeOrDefault());
        if (availableInventory.isEmpty()) {
            return;
        }

        Inventory selectedInventory = availableInventory.get(ThreadLocalRandom.current().nextInt(availableInventory.size()));
        int quantity = (int) Math.min(selectedInventory.getQuantityAvailable(), ThreadLocalRandom.current().nextInt(1, 4));
        if (quantity <= 0) {
            return;
        }

        BigDecimal unitPrice = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(20, 180))
            .setScale(2, java.math.RoundingMode.HALF_UP);

        OrderCreateRequest request = new OrderCreateRequest(
            null,
            selectedInventory.getWarehouse().getCode(),
            List.of(new OrderItemRequest(selectedInventory.getProduct().getSku(), quantity, unitPrice))
        );

        try {
            orderService.createOrder(request, "simulation");
            fulfillmentService.advanceSimulationFlow();
        } catch (Exception exception) {
            log.debug("Simulation skipped an order tick: {}", exception.getMessage());
        }
    }
}
