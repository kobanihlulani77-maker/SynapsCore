package com.synapsecore.domain.service;

import com.synapsecore.audit.AuditLogService;
import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.auth.DemoAccessUsers;
import com.synapsecore.domain.dto.SeedResetResponse;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.AccessUser;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationImportRunRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.OrderItemRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.simulation.SimulationStateService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class SeedService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final TenantRepository tenantRepository;
    private final AccessOperatorRepository accessOperatorRepository;
    private final AccessUserRepository accessUserRepository;
    private final IntegrationConnectorRepository integrationConnectorRepository;
    private final IntegrationImportRunRepository integrationImportRunRepository;
    private final IntegrationReplayRecordRepository integrationReplayRecordRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final OrderItemRepository orderItemRepository;
    private final AlertRepository alertRepository;
    private final RecommendationRepository recommendationRepository;
    private final BusinessEventRepository businessEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final ScenarioRunRepository scenarioRunRepository;
    private final SimulationStateService simulationStateService;
    private final IntegrationConnectorService integrationConnectorService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final TransactionTemplate transactionTemplate;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public boolean seedIfEmpty() {
        Boolean seeded = transactionTemplate.execute(status -> {
            if (productRepository.count() > 0 || warehouseRepository.count() > 0) {
                backfillTenantAwareData();
                return false;
            }

            seedStarterData();
            return true;
        });
        return Boolean.TRUE.equals(seeded);
    }

    public SeedResetResponse reseedStarterData() {
        simulationStateService.deactivate();
        SeedCounts counts = transactionTemplate.execute(status -> {
            clearExistingData();
            return seedStarterData();
        });
        operationalStateChangePublisher.publish(OperationalUpdateType.SYSTEM_RESEEDED, "dev-tools");
        auditLogService.recordSuccess(
            "SYSTEM_RESEEDED",
            "dev-tools",
            "dev-tools",
            "SeedBaseline",
            "starter-data",
            "Restored starter baseline with " + counts.inventoryRecordsSeeded() + " inventory records."
        );

        return new SeedResetResponse(
            "reseeded",
            counts.productsSeeded(),
            counts.warehousesSeeded(),
            counts.inventoryRecordsSeeded(),
            simulationStateService.getStatus(),
            Instant.now()
        );
    }

    private void clearExistingData() {
        alertRepository.deleteAllInBatch();
        recommendationRepository.deleteAllInBatch();
        fulfillmentTaskRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        customerOrderRepository.deleteAllInBatch();
        businessEventRepository.deleteAllInBatch();
        scenarioRunRepository.deleteAllInBatch();
        integrationImportRunRepository.deleteAllInBatch();
        integrationReplayRecordRepository.deleteAllInBatch();
        accessUserRepository.deleteAllInBatch();
        accessOperatorRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
        integrationConnectorRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        warehouseRepository.deleteAllInBatch();
        tenantRepository.deleteAllInBatch();
    }

    private SeedCounts seedStarterData() {
        Tenant tenant = tenantRepository.save(Tenant.builder()
            .code(DemoAccessUsers.DEFAULT_TENANT_CODE)
            .name("Synapse Demo Company")
            .description("Starter operating context for the default SynapseCore tenant.")
            .active(true)
            .build());
        var operators = accessOperatorRepository.saveAll(List.of(
            operator(tenant, "Operations Planner", "Operations Planner", "Default planning identity for scenario creation."),
            operator(tenant, "Operations Lead", "Operations Lead", "Default owner reviewer for scenario approvals.",
                SynapseAccessRole.TENANT_ADMIN, SynapseAccessRole.REVIEW_OWNER),
            operator(tenant, "Amina Planner", "Amina Planner", "Planning operator used in scenario exploration."),
            operator(tenant, "Lebo Planner", "Lebo Planner", "Planning operator used in scenario exploration."),
            operator(tenant, "Thando Planner", "Thando Planner", "Planning operator used in scenario exploration."),
            operator(tenant, "Ayo Planner", "Ayo Planner", "Cross-functional planner with owner-review capability.",
                SynapseAccessRole.REVIEW_OWNER),
            operator(tenant, "Naledi Lead", "Naledi Lead",
                "Senior operations reviewer who can also final-approve edge cases.",
                SynapseAccessRole.REVIEW_OWNER, SynapseAccessRole.FINAL_APPROVER),
            operator(tenant, "Jordan Lead", "Jordan Lead", "Operations lead for owner review routing.",
                SynapseAccessRole.REVIEW_OWNER),
            operator(tenant, "Ops Director", "Ops Director", "Fallback owner reviewer for edge-case review.",
                SynapseAccessRole.REVIEW_OWNER),
            operator(tenant, "Lebo Ops", "Lebo Ops", "Escalation handoff owner for urgent scenario queues.",
                SynapseAccessRole.REVIEW_OWNER, SynapseAccessRole.ESCALATION_OWNER),
            operator(tenant, "North Operations Director", "North Operations Director",
                "Final approver for north warehouse escalations.", Set.of("WH-NORTH"), SynapseAccessRole.FINAL_APPROVER),
            operator(tenant, "Coast Operations Director", "Coast Operations Director",
                "Final approver for coast warehouse escalations.", Set.of("WH-COAST"), SynapseAccessRole.FINAL_APPROVER),
            operator(tenant, "Executive Operations Director", "Executive Operations Director",
                "Executive fallback for escalated final approvals.", SynapseAccessRole.TENANT_ADMIN, SynapseAccessRole.FINAL_APPROVER),
            operator(tenant, "Integration Lead", "Integration Lead", "Inbound connector administrator.",
                SynapseAccessRole.INTEGRATION_ADMIN),
            operator(tenant, "Integration Operator", "Integration Operator",
                "Recovery operator for replaying failed inbound orders.",
                SynapseAccessRole.INTEGRATION_OPERATOR)
        ));
        seedAccessUsers(tenant, operators);

        Product fluxSensor = productRepository.save(Product.builder()
            .sku("SKU-FLX-100")
            .name("Flux Sensor")
            .category("Sensors")
            .build());
        Product vectorDrive = productRepository.save(Product.builder()
            .sku("SKU-VDR-210")
            .name("Vector Drive")
            .category("Power")
            .build());
        Product pulseRelay = productRepository.save(Product.builder()
            .sku("SKU-PLS-330")
            .name("Pulse Relay")
            .category("Control")
            .build());
        Product orbitValve = productRepository.save(Product.builder()
            .sku("SKU-ORB-440")
            .name("Orbit Valve")
            .category("Flow")
            .build());

        Warehouse north = warehouseRepository.save(Warehouse.builder()
            .tenant(tenant)
            .code("WH-NORTH")
            .name("Warehouse North")
            .location("Johannesburg")
            .build());
        Warehouse coast = warehouseRepository.save(Warehouse.builder()
            .tenant(tenant)
            .code("WH-COAST")
            .name("Warehouse Coast")
            .location("Durban")
            .build());

        List<Inventory> inventories = inventoryRepository.saveAll(List.of(
            Inventory.builder().product(fluxSensor).warehouse(north).quantityAvailable(28L).reorderThreshold(20L).build(),
            Inventory.builder().product(vectorDrive).warehouse(north).quantityAvailable(40L).reorderThreshold(18L).build(),
            Inventory.builder().product(pulseRelay).warehouse(north).quantityAvailable(18L).reorderThreshold(12L).build(),
            Inventory.builder().product(orbitValve).warehouse(north).quantityAvailable(32L).reorderThreshold(15L).build(),
            Inventory.builder().product(fluxSensor).warehouse(coast).quantityAvailable(22L).reorderThreshold(14L).build(),
            Inventory.builder().product(vectorDrive).warehouse(coast).quantityAvailable(30L).reorderThreshold(12L).build(),
            Inventory.builder().product(pulseRelay).warehouse(coast).quantityAvailable(26L).reorderThreshold(10L).build(),
            Inventory.builder().product(orbitValve).warehouse(coast).quantityAvailable(16L).reorderThreshold(8L).build()
        ));

        integrationConnectorService.seedStarterConnectors();

        return new SeedCounts(4, 2, inventories.size());
    }

    private void backfillTenantAwareData() {
        Tenant tenant = tenantRepository.findByCodeIgnoreCase(DemoAccessUsers.DEFAULT_TENANT_CODE)
            .orElseGet(() -> tenantRepository.save(Tenant.builder()
                .code(DemoAccessUsers.DEFAULT_TENANT_CODE)
                .name("Synapse Demo Company")
                .description("Starter operating context for the default SynapseCore tenant.")
                .active(true)
                .build()));

        boolean tenantChanged = false;
        if (tenant.getPasswordRotationDays() <= 0) {
            tenant.setPasswordRotationDays(90);
            tenantChanged = true;
        }
        if (tenant.getSessionTimeoutMinutes() <= 0) {
            tenant.setSessionTimeoutMinutes(480);
            tenantChanged = true;
        }
        if (tenant.getSecurityPolicyVersion() <= 0) {
            tenant.setSecurityPolicyVersion(1L);
            tenantChanged = true;
        }
        if (tenantChanged) {
            tenantRepository.save(tenant);
        }

        java.util.List<Warehouse> warehouseUpdates = new java.util.ArrayList<>();
        for (Warehouse warehouse : warehouseRepository.findAll()) {
            if (warehouse.getTenant() == null) {
                warehouse.setTenant(tenant);
                warehouseUpdates.add(warehouse);
            }
        }
        if (!warehouseUpdates.isEmpty()) {
            warehouseRepository.saveAll(warehouseUpdates);
        }

        java.util.List<AccessOperator> operatorUpdates = new java.util.ArrayList<>();
        for (AccessOperator operator : accessOperatorRepository.findAll()) {
            boolean changed = false;
            boolean defaultTenantOperator = operator.getTenant() != null
                && operator.getTenant().getCode().equalsIgnoreCase(tenant.getCode());
            if (operator.getTenant() == null) {
                operator.setTenant(tenant);
                changed = true;
                defaultTenantOperator = true;
            }
            if (defaultTenantOperator
                && ("Operations Lead".equalsIgnoreCase(operator.getActorName())
                || "Executive Operations Director".equalsIgnoreCase(operator.getActorName()))) {
                if (operator.getRoles() != null && operator.getRoles().add(SynapseAccessRole.TENANT_ADMIN)) {
                    changed = true;
                }
            }
            if (defaultTenantOperator
                && "North Operations Director".equalsIgnoreCase(operator.getActorName())
                && (operator.getWarehouseScopes() == null || !operator.getWarehouseScopes().contains("WH-NORTH"))) {
                operator.setWarehouseScopes(Set.of("WH-NORTH"));
                changed = true;
            }
            if (defaultTenantOperator
                && "Coast Operations Director".equalsIgnoreCase(operator.getActorName())
                && (operator.getWarehouseScopes() == null || !operator.getWarehouseScopes().contains("WH-COAST"))) {
                operator.setWarehouseScopes(Set.of("WH-COAST"));
                changed = true;
            }
            if (changed) {
                operatorUpdates.add(operator);
            }
        }
        if (!operatorUpdates.isEmpty()) {
            accessOperatorRepository.saveAll(operatorUpdates);
        }

        var operatorsByActor = accessOperatorRepository.findAllByTenant_CodeIgnoreCaseOrderByDisplayNameAsc(tenant.getCode()).stream()
            .collect(java.util.stream.Collectors.toMap(AccessOperator::getActorName, operator -> operator, (left, right) -> left));
        var usersByUsername = accessUserRepository.findAllByTenant_CodeIgnoreCaseOrderByFullNameAscUsernameAsc(tenant.getCode()).stream()
            .collect(java.util.stream.Collectors.toMap(user -> user.getUsername().toLowerCase(java.util.Locale.ROOT),
                user -> user, (left, right) -> left));
        java.util.List<AccessUser> updates = new java.util.ArrayList<>();

        for (DemoAccessUsers.DemoAccessUser demoUser : DemoAccessUsers.all()) {
            AccessOperator operator = operatorsByActor.get(demoUser.actorName());
            if (operator == null) {
                continue;
            }
            AccessUser existing = usersByUsername.get(demoUser.username().toLowerCase(java.util.Locale.ROOT));
            if (existing == null) {
                updates.add(AccessUser.builder()
                    .tenant(tenant)
                    .username(demoUser.username())
                    .fullName(demoUser.fullName())
                    .passwordHash(passwordEncoder.encode(demoUser.password()))
                    .active(true)
                    .operator(operator)
                    .build());
                continue;
            }

            boolean changed = false;
            if (existing.getOperator() == null || !existing.getOperator().getActorName().equals(operator.getActorName())) {
                existing.setOperator(operator);
                changed = true;
            }
            if (existing.getTenant() == null || !existing.getTenant().getCode().equalsIgnoreCase(demoUser.tenantCode())) {
                existing.setTenant(tenant);
                changed = true;
            }
            if (existing.getFullName() == null || existing.getFullName().isBlank()) {
                existing.setFullName(demoUser.fullName());
                changed = true;
            }
            if (existing.getPasswordHash() == null || existing.getPasswordHash().isBlank()) {
                existing.setPasswordHash(passwordEncoder.encode(demoUser.password()));
                changed = true;
            }
            if (existing.getPasswordUpdatedAt() == null) {
                existing.setPasswordUpdatedAt(Instant.now());
                changed = true;
            }
            if (existing.getSessionVersion() <= 0) {
                existing.setSessionVersion(1L);
                changed = true;
            }
            if (changed) {
                updates.add(existing);
            }
        }

        if (!updates.isEmpty()) {
            accessUserRepository.saveAll(updates);
        }

        java.util.List<com.synapsecore.domain.entity.IntegrationConnector> connectorUpdates = new java.util.ArrayList<>();
        for (var connector : integrationConnectorRepository.findAll()) {
            if (connector.getTenant() == null) {
                connector.setTenant(tenant);
                connectorUpdates.add(connector);
            }
        }
        if (!connectorUpdates.isEmpty()) {
            integrationConnectorRepository.saveAll(connectorUpdates);
        }

        java.util.List<com.synapsecore.domain.entity.CustomerOrder> orderUpdates = new java.util.ArrayList<>();
        for (var order : customerOrderRepository.findAll()) {
            if (order.getTenant() == null) {
                order.setTenant(order.getWarehouse() != null && order.getWarehouse().getTenant() != null
                    ? order.getWarehouse().getTenant()
                    : tenant);
                orderUpdates.add(order);
            }
        }
        if (!orderUpdates.isEmpty()) {
            customerOrderRepository.saveAll(orderUpdates);
        }

        java.util.List<FulfillmentTask> fulfillmentBackfills = new java.util.ArrayList<>();
        for (var order : customerOrderRepository.findAll()) {
            if (fulfillmentTaskRepository.findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(
                order.getTenant() == null ? tenant.getCode() : order.getTenant().getCode(),
                order.getExternalOrderId()
            ).isEmpty()) {
                Instant queuedAt = order.getCreatedAt() == null ? Instant.now() : order.getCreatedAt();
                fulfillmentBackfills.add(FulfillmentTask.builder()
                    .tenant(order.getTenant() == null ? tenant : order.getTenant())
                    .customerOrder(order)
                    .warehouse(order.getWarehouse())
                    .status(FulfillmentStatus.QUEUED)
                    .queuedAt(queuedAt)
                    .promisedDispatchAt(queuedAt.plusSeconds(2 * 3600))
                    .expectedDeliveryAt(queuedAt.plusSeconds(24 * 3600))
                    .note("Backfilled fulfillment lane for an existing order.")
                    .exceptionCount(0)
                    .build());
            }
        }
        if (!fulfillmentBackfills.isEmpty()) {
            fulfillmentTaskRepository.saveAll(fulfillmentBackfills);
        }

        java.util.List<com.synapsecore.domain.entity.Alert> alertUpdates = new java.util.ArrayList<>();
        for (var alert : alertRepository.findAll()) {
            if (alert.getTenant() == null) {
                alert.setTenant(tenant);
                alertUpdates.add(alert);
            }
        }
        if (!alertUpdates.isEmpty()) {
            alertRepository.saveAll(alertUpdates);
        }

        java.util.List<com.synapsecore.domain.entity.Recommendation> recommendationUpdates = new java.util.ArrayList<>();
        for (var recommendation : recommendationRepository.findAll()) {
            if (recommendation.getTenant() == null) {
                recommendation.setTenant(tenant);
                recommendationUpdates.add(recommendation);
            }
        }
        if (!recommendationUpdates.isEmpty()) {
            recommendationRepository.saveAll(recommendationUpdates);
        }

        java.util.List<com.synapsecore.domain.entity.ScenarioRun> scenarioUpdates = new java.util.ArrayList<>();
        for (var scenarioRun : scenarioRunRepository.findAll()) {
            if (scenarioRun.getTenant() == null) {
                scenarioRun.setTenant(tenant);
                scenarioUpdates.add(scenarioRun);
            }
        }
        if (!scenarioUpdates.isEmpty()) {
            scenarioRunRepository.saveAll(scenarioUpdates);
        }

        java.util.List<com.synapsecore.domain.entity.BusinessEvent> businessEventUpdates = new java.util.ArrayList<>();
        for (var event : businessEventRepository.findAll()) {
            if (event.getTenantCode() == null || event.getTenantCode().isBlank()) {
                event.setTenantCode(tenant.getCode());
                businessEventUpdates.add(event);
            }
        }
        if (!businessEventUpdates.isEmpty()) {
            businessEventRepository.saveAll(businessEventUpdates);
        }

        java.util.List<com.synapsecore.domain.entity.AuditLog> auditUpdates = new java.util.ArrayList<>();
        for (var auditLog : auditLogRepository.findAll()) {
            if (auditLog.getTenantCode() == null || auditLog.getTenantCode().isBlank()) {
                auditLog.setTenantCode(tenant.getCode());
                auditUpdates.add(auditLog);
            }
        }
        if (!auditUpdates.isEmpty()) {
            auditLogRepository.saveAll(auditUpdates);
        }

        java.util.List<com.synapsecore.domain.entity.IntegrationImportRun> importRunUpdates = new java.util.ArrayList<>();
        for (var importRun : integrationImportRunRepository.findAll()) {
            if (importRun.getTenantCode() == null || importRun.getTenantCode().isBlank()) {
                importRun.setTenantCode(tenant.getCode());
                importRunUpdates.add(importRun);
            }
        }
        if (!importRunUpdates.isEmpty()) {
            integrationImportRunRepository.saveAll(importRunUpdates);
        }

        java.util.List<com.synapsecore.domain.entity.IntegrationReplayRecord> replayUpdates = new java.util.ArrayList<>();
        for (var replayRecord : integrationReplayRecordRepository.findAll()) {
            if (replayRecord.getTenantCode() == null || replayRecord.getTenantCode().isBlank()) {
                replayRecord.setTenantCode(tenant.getCode());
                replayUpdates.add(replayRecord);
            }
        }
        if (!replayUpdates.isEmpty()) {
            integrationReplayRecordRepository.saveAll(replayUpdates);
        }

        integrationConnectorService.seedStarterConnectors();
    }

    private void seedAccessUsers(Tenant tenant, java.util.List<AccessOperator> operators) {
        var operatorsByActor = operators.stream()
            .collect(java.util.stream.Collectors.toMap(AccessOperator::getActorName, operator -> operator));
        java.util.List<AccessUser> users = DemoAccessUsers.all().stream()
            .map(user -> AccessUser.builder()
                .tenant(tenant)
                .username(user.username())
                .fullName(user.fullName())
                .passwordHash(passwordEncoder.encode(user.password()))
                .active(true)
                .passwordUpdatedAt(Instant.now())
                .operator(operatorsByActor.get(user.actorName()))
                .build())
            .toList();
        accessUserRepository.saveAll(users);
    }

    private AccessOperator operator(Tenant tenant,
                                    String actorName,
                                    String displayName,
                                    String description,
                                    SynapseAccessRole... roles) {
        return operator(tenant, actorName, displayName, description, Set.of(), roles);
    }

    private AccessOperator operator(Tenant tenant,
                                    String actorName,
                                    String displayName,
                                    String description,
                                    Set<String> warehouseScopes,
                                    SynapseAccessRole... roles) {
        return AccessOperator.builder()
            .tenant(tenant)
            .actorName(actorName)
            .displayName(displayName)
            .description(description)
            .active(true)
            .roles(roles.length == 0 ? EnumSet.noneOf(SynapseAccessRole.class) : EnumSet.copyOf(List.of(roles)))
            .warehouseScopes(warehouseScopes)
            .build();
    }

    private record SeedCounts(long productsSeeded, long warehousesSeeded, long inventoryRecordsSeeded) {
    }
}
