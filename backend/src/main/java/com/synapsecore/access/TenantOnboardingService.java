package com.synapsecore.access;

import com.synapsecore.access.dto.TenantOnboardingRequest;
import com.synapsecore.access.dto.TenantOnboardingResponse;
import com.synapsecore.access.dto.TenantProvisionedUserResponse;
import com.synapsecore.access.dto.TenantUserProvisioningRequest;
import com.synapsecore.access.dto.TenantWarehouseProvisioningRequest;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.config.SynapseStarterProperties;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.AccessUser;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.observability.OperationalMetricsService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantOnboardingService {

    private final TenantRepository tenantRepository;
    private final AccessOperatorRepository accessOperatorRepository;
    private final AccessUserRepository accessUserRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final IntegrationConnectorService integrationConnectorService;
    private final AuditLogService auditLogService;
    private final SynapseStarterProperties starterProperties;
    private final OperationalMetricsService operationalMetricsService;

    @Transactional
    public TenantOnboardingResponse onboardTenant(TenantOnboardingRequest request, String actorName) {
        String tenantCode = normalizeTenantCode(request.tenantCode());
        String adminUsername = normalizeUsername(request.adminUsername());
        String primaryLocation = request.primaryLocation().trim();
        String secondaryLocation = normalizeOptional(request.secondaryLocation());
        List<TenantWarehouseProvisioningRequest> warehouseRequests = resolveWarehouses(request, tenantCode, primaryLocation, secondaryLocation);
        List<TenantUserProvisioningRequest> userRequests = resolveUsers(request, tenantCode, adminUsername);
        validateProvisioningPlan(tenantCode, adminUsername, warehouseRequests, userRequests, request.requiredRoles());

        if (tenantRepository.findByCodeIgnoreCase(tenantCode).isPresent()) {
            operationalMetricsService.recordTenantOperation(tenantCode, "TENANT_ONBOARDING", false);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant code already exists: " + tenantCode);
        }
        if (accessUserRepository.findByTenant_CodeIgnoreCaseAndUsernameIgnoreCaseAndActiveTrue(tenantCode, adminUsername).isPresent()) {
            operationalMetricsService.recordTenantOperation(tenantCode, "TENANT_ONBOARDING", false);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Admin username already exists in tenant " + tenantCode + ": " + adminUsername);
        }

        Tenant tenant;
        try {
            tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .code(tenantCode)
                .name(request.tenantName().trim())
                .description(normalizeOptional(request.description()))
                .active(true)
                .requiredRoles(normalizeRoles(request.requiredRoles()))
                .build());
        } catch (DataIntegrityViolationException exception) {
            operationalMetricsService.recordTenantOperation(tenantCode, "TENANT_ONBOARDING", false);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant code already exists: " + tenantCode);
        }

        Map<String, Warehouse> warehousesByCode = new LinkedHashMap<>();
        for (TenantWarehouseProvisioningRequest warehouseRequest : warehouseRequests) {
            Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
                .tenant(tenant)
                .code(normalizeWarehouseCode(warehouseRequest.code()))
                .name(warehouseRequest.name().trim())
                .location(warehouseRequest.location().trim())
                .build());
            warehousesByCode.put(warehouse.getCode(), warehouse);
        }

        Map<String, String> temporaryCredentialsByUsername = new HashMap<>();
        List<AccessUser> provisionedUsers = new ArrayList<>();
        for (TenantUserProvisioningRequest userRequest : userRequests) {
            String actorNameForUser = normalizeActorName(userRequest.operatorActorName());
            AccessOperator operator = accessOperatorRepository.save(AccessOperator.builder()
                .tenant(tenant)
                .actorName(actorNameForUser)
                .displayName(normalizeOptional(userRequest.operatorDisplayName()) == null
                    ? actorNameForUser
                    : userRequest.operatorDisplayName().trim())
                .description(normalizeOptional(userRequest.operatorDescription()))
                .active(true)
                .roles(normalizeRoles(userRequest.roles()))
                .warehouseScopes(normalizeWarehouseScopes(userRequest.warehouseScopes()))
                .build());

            String username = normalizeUsername(userRequest.username());
            boolean usesSuppliedAdminPassword = username.equals(adminUsername)
                && (userRequest.initialPassword() == null || userRequest.initialPassword().isBlank());
            String password = usesSuppliedAdminPassword
                ? request.adminPassword().trim()
                : normalizeOptional(userRequest.initialPassword());
            boolean generatedTemporaryCredential = password == null;
            if (generatedTemporaryCredential) {
                password = generateTemporaryPassword();
                temporaryCredentialsByUsername.put(username, password);
            }

            provisionedUsers.add(accessUserRepository.save(AccessUser.builder()
                .tenant(tenant)
                .username(username)
                .fullName(userRequest.fullName().trim())
                .passwordHash(passwordEncoder.encode(password))
                .active(true)
                .passwordUpdatedAt(Instant.now())
                .passwordChangeRequired(generatedTemporaryCredential || !usesSuppliedAdminPassword)
                .operator(operator)
                .build()));
        }

        // Keep the legacy planner fixture only for the non-explicit compatibility profile.
        // Production onboarding requires every role holder to be supplied explicitly.
        if (!starterProperties.isRequireExplicitTenantProvisioning()
            && (request.users() == null || request.users().isEmpty())) {
            accessOperatorRepository.save(AccessOperator.builder()
                .tenant(tenant)
                .actorName("Operations Planner")
                .displayName("Operations Planner")
                .description("Synthetic planning operator for compatibility fixtures.")
                .active(true)
                .roles(EnumSet.noneOf(SynapseAccessRole.class))
                .build());
        }

        AccessUser adminUser = provisionedUsers.stream()
            .filter(user -> user.getUsername().equalsIgnoreCase(adminUsername))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Initial tenant admin user was not provisioned."));
        AccessUser finalApprover = provisionedUsers.stream()
            .filter(user -> user.getOperator() != null
                && user.getOperator().getRoles().contains(SynapseAccessRole.FINAL_APPROVER))
            .findFirst()
            .orElse(null);

        if (starterProperties.isSeedStarterInventoryOnTenantOnboarding() && warehousesByCode.size() >= 2) {
            var warehouseIterator = warehousesByCode.values().iterator();
            seedStarterInventory(warehouseIterator.next(), warehouseIterator.next());
        }
        if (starterProperties.isSeedStarterConnectorsOnTenantOnboarding()) {
            integrationConnectorService.seedStarterConnectors(tenant);
        }

        auditLogService.recordSuccessForTenant(
            tenant.getCode(),
            "TENANT_ONBOARDED",
            actorName,
            "tenant-admin",
            "Tenant",
            tenant.getCode(),
            "Created tenant " + tenant.getName() + " with bootstrap admin " + adminUser.getUsername() + "."
        );
        operationalMetricsService.recordTenantOperation(tenant.getCode(), "TENANT_ONBOARDING", true);

        return new TenantOnboardingResponse(
            tenant.getId(),
            tenant.getCode(),
            tenant.getName(),
            adminUser.getUsername(),
            adminUser.getOperator().getActorName(),
            finalApprover == null ? null : finalApprover.getUsername(),
            finalApprover == null ? null : finalApprover.getOperator().getActorName(),
            List.copyOf(warehousesByCode.keySet()),
            Instant.now(),
            "READY",
            provisionedUsers.stream()
                .map(user -> new TenantProvisionedUserResponse(
                    user.getUsername(),
                    user.getOperator().getActorName(),
                    user.getOperator().getRoles().stream().sorted().toList(),
                    user.getOperator().getWarehouseScopes().stream().sorted().toList(),
                    temporaryCredentialsByUsername.containsKey(user.getUsername()),
                    temporaryCredentialsByUsername.get(user.getUsername())
                ))
                .toList()
        );
    }

    private List<TenantWarehouseProvisioningRequest> resolveWarehouses(TenantOnboardingRequest request,
                                                                       String tenantCode,
                                                                       String primaryLocation,
                                                                       String secondaryLocation) {
        if (request.warehouses() != null && !request.warehouses().isEmpty()) {
            return request.warehouses();
        }
        if (starterProperties.isRequireExplicitTenantProvisioning()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Explicit warehouse configuration is required for controlled tenant provisioning.");
        }
        return List.of(
            new TenantWarehouseProvisioningRequest("WH-NORTH", request.tenantName().trim() + " North Hub", primaryLocation),
            new TenantWarehouseProvisioningRequest("WH-COAST", request.tenantName().trim() + " Coast Hub",
                secondaryLocation == null ? primaryLocation + " Reserve" : secondaryLocation)
        );
    }

    private List<TenantUserProvisioningRequest> resolveUsers(TenantOnboardingRequest request,
                                                             String tenantCode,
                                                             String adminUsername) {
        if (request.users() != null && !request.users().isEmpty()) {
            return request.users();
        }
        if (starterProperties.isRequireExplicitTenantProvisioning()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Explicit initial user and role configuration is required for controlled tenant provisioning.");
        }
        String executiveUsername = tenantCode.toLowerCase(Locale.ROOT).replace('-', '.') + ".executive";
        return List.of(
            new TenantUserProvisioningRequest(
                adminUsername,
                request.adminFullName(),
                "Operations Lead",
                "Operations Lead",
                "Synthetic compatibility administrator.",
                 List.of(SynapseAccessRole.TENANT_ADMIN, SynapseAccessRole.REVIEW_OWNER,
                     SynapseAccessRole.ESCALATION_OWNER, SynapseAccessRole.INTEGRATION_ADMIN,
                     SynapseAccessRole.INTEGRATION_OPERATOR),
                 List.of(),
                 null
             ),
            new TenantUserProvisioningRequest(
                executiveUsername,
                tenantCode + " Executive Approver",
                "Executive Operations Director",
                "Executive Operations Director",
                "Synthetic compatibility final approver.",
                List.of(SynapseAccessRole.FINAL_APPROVER),
                List.of(),
                null
            )
        );
    }

    private void validateProvisioningPlan(String tenantCode,
                                          String adminUsername,
                                          List<TenantWarehouseProvisioningRequest> warehouses,
                                          List<TenantUserProvisioningRequest> users,
                                          List<SynapseAccessRole> requestedRequiredRoles) {
        if (warehouses.isEmpty() || users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Controlled provisioning requires warehouses and initial users.");
        }

        Set<String> warehouseCodes = new LinkedHashSet<>();
        for (TenantWarehouseProvisioningRequest warehouse : warehouses) {
            if (warehouse == null || warehouse.code() == null || warehouse.name() == null || warehouse.location() == null
                || warehouse.code().isBlank() || warehouse.name().isBlank() || warehouse.location().isBlank()
                || !warehouseCodes.add(normalizeWarehouseCode(warehouse.code()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Warehouse configuration must contain unique code, name, and location values.");
            }
        }

        Set<String> usernames = new HashSet<>();
        Set<String> actorNames = new HashSet<>();
        boolean tenantAdmin = false;
        for (TenantUserProvisioningRequest user : users) {
            if (user == null || user.username() == null || user.fullName() == null || user.operatorActorName() == null
                || user.username().isBlank() || user.fullName().isBlank() || user.operatorActorName().isBlank()
                || !usernames.add(normalizeUsername(user.username()))
                || !actorNames.add(normalizeActorName(user.operatorActorName()).toLowerCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Initial users and operators must be unique and complete.");
            }
            if (user.roles() == null || user.roles().isEmpty() || user.roles().stream().anyMatch(Objects::isNull)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Every initial user must have at least one approved role.");
            }
            Set<SynapseAccessRole> roles = normalizeRoles(user.roles());
            tenantAdmin |= roles.contains(SynapseAccessRole.TENANT_ADMIN);
            for (String scope : normalizeWarehouseScopes(user.warehouseScopes())) {
                if (!warehouseCodes.contains(scope)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Warehouse scope " + scope + " does not reference a configured warehouse.");
                }
            }
        }
        Set<SynapseAccessRole> requiredRoles = normalizeRoles(requestedRequiredRoles);
        if (!usernames.contains(adminUsername) || !tenantAdmin
            || requiredRoles.stream().anyMatch(requiredRole ->
                !hasAuthorityForEveryWarehouse(users, warehouseCodes, requiredRole))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Initial provisioning is missing the admin identity or required role coverage for every warehouse.");
        }
    }

    private boolean hasAuthorityForEveryWarehouse(List<TenantUserProvisioningRequest> users,
                                                  Set<String> warehouseCodes,
                                                  SynapseAccessRole requiredRole) {
        return warehouseCodes.stream().allMatch(warehouseCode -> users.stream().anyMatch(user -> {
            Set<SynapseAccessRole> roles = normalizeRoles(user.roles());
            Set<String> scopes = normalizeWarehouseScopes(user.warehouseScopes());
            return roles.contains(requiredRole) && (scopes.isEmpty() || scopes.contains(warehouseCode));
        }));
    }

    private String generateTemporaryPassword() {
        return "tmp-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "!";
    }

    private void seedStarterInventory(Warehouse north, Warehouse coast) {
        List<Product> products = productRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc("STARTER-OPS");
        if (products.isEmpty()) {
            products = List.of(
                productRepository.save(Product.builder().tenant(north.getTenant()).catalogSku("SKU-FLX-100").name("Flux Sensor").category("Sensors").build()),
                productRepository.save(Product.builder().tenant(north.getTenant()).catalogSku("SKU-VDR-210").name("Vector Drive").category("Power").build()),
                productRepository.save(Product.builder().tenant(north.getTenant()).catalogSku("SKU-PLS-330").name("Pulse Relay").category("Control").build()),
                productRepository.save(Product.builder().tenant(north.getTenant()).catalogSku("SKU-ORB-440").name("Orbit Valve").category("Flow").build())
            );
        } else {
            products = products.stream()
                .map(product -> productRepository.save(Product.builder()
                    .tenant(north.getTenant())
                    .catalogSku(product.resolveCatalogSku())
                    .name(product.getName())
                    .category(product.getCategory())
                    .build()))
                .toList();
        }
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            inventoryRepository.save(Inventory.builder()
                .tenant(north.getTenant()).product(product).warehouse(north)
                .quantityAvailable(28L + (index * 4L)).quantityOnHand(28L + (index * 4L))
                .quantityReserved(0L).quantityInbound(0L).reorderThreshold(16L + (index * 2L)).build());
            inventoryRepository.save(Inventory.builder()
                .tenant(coast.getTenant()).product(product).warehouse(coast)
                .quantityAvailable(22L + (index * 4L)).quantityOnHand(22L + (index * 4L))
                .quantityReserved(0L).quantityInbound(0L).reorderThreshold(12L + (index * 2L)).build());
        }
    }

    private String normalizeTenantCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUsername(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeActorName(String value) {
        return value.trim();
    }

    private String normalizeWarehouseCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private Set<SynapseAccessRole> normalizeRoles(List<SynapseAccessRole> roles) {
        return roles == null || roles.isEmpty() ? EnumSet.noneOf(SynapseAccessRole.class) : EnumSet.copyOf(roles);
    }

    private Set<String> normalizeWarehouseScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return scopes.stream()
            .filter(scope -> scope != null && !scope.isBlank())
            .map(this::normalizeWarehouseCode)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
