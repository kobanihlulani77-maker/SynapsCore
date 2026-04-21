package com.synapsecore.access;

import com.synapsecore.access.dto.TenantOnboardingRequest;
import com.synapsecore.access.dto.TenantOnboardingResponse;
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
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public TenantOnboardingResponse onboardTenant(TenantOnboardingRequest request, String actorName) {
        String tenantCode = normalizeTenantCode(request.tenantCode());
        String adminUsername = normalizeUsername(request.adminUsername());
        String primaryLocation = request.primaryLocation().trim();
        String secondaryLocation = normalizeOptional(request.secondaryLocation());

        if (tenantRepository.findByCodeIgnoreCase(tenantCode).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Tenant code already exists: " + tenantCode);
        }
        if (accessUserRepository.findByTenant_CodeIgnoreCaseAndUsernameIgnoreCaseAndActiveTrue(tenantCode, adminUsername).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Admin username already exists in tenant " + tenantCode + ": " + adminUsername);
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
            .code(tenantCode)
            .name(request.tenantName().trim())
            .description(normalizeOptional(request.description()))
            .active(true)
            .build());

        AccessOperator operationsLead = accessOperatorRepository.save(AccessOperator.builder()
            .tenant(tenant)
            .actorName("Operations Lead")
            .displayName("Operations Lead")
            .description("Bootstrap tenant admin for " + tenant.getName() + ".")
            .active(true)
            .roles(EnumSet.of(
                SynapseAccessRole.TENANT_ADMIN,
                SynapseAccessRole.REVIEW_OWNER,
                SynapseAccessRole.ESCALATION_OWNER,
                SynapseAccessRole.INTEGRATION_ADMIN,
                SynapseAccessRole.INTEGRATION_OPERATOR
            ))
            .build());

        AccessOperator executiveApprover = accessOperatorRepository.save(AccessOperator.builder()
            .tenant(tenant)
            .actorName("Executive Operations Director")
            .displayName("Executive Operations Director")
            .description("Bootstrap final approver for " + tenant.getName() + ".")
            .active(true)
            .roles(EnumSet.of(SynapseAccessRole.FINAL_APPROVER))
            .build());

        accessOperatorRepository.save(AccessOperator.builder()
            .tenant(tenant)
            .actorName("Operations Planner")
            .displayName("Operations Planner")
            .description("Bootstrap planning operator for " + tenant.getName() + ".")
            .active(true)
            .roles(EnumSet.noneOf(SynapseAccessRole.class))
            .build());

        AccessUser adminUser = accessUserRepository.save(AccessUser.builder()
            .tenant(tenant)
            .username(adminUsername)
            .fullName(request.adminFullName().trim())
            .passwordHash(passwordEncoder.encode(request.adminPassword().trim()))
            .active(true)
            .passwordUpdatedAt(Instant.now())
            .operator(operationsLead)
            .build());

        String executiveUsername = tenantCode.toLowerCase(Locale.ROOT).replace('-', '.') + ".executive";
        String executivePassword = "exec-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        accessUserRepository.save(AccessUser.builder()
            .tenant(tenant)
            .username(executiveUsername)
            .fullName(tenant.getName() + " Executive Approver")
            .passwordHash(passwordEncoder.encode(executivePassword))
            .active(true)
            .passwordUpdatedAt(Instant.now())
            .passwordChangeRequired(true)
            .operator(executiveApprover)
            .build());

        Warehouse north = warehouseRepository.save(Warehouse.builder()
            .tenant(tenant)
            .code("WH-NORTH")
            .name(tenant.getName() + " North Hub")
            .location(primaryLocation)
            .build());
        Warehouse coast = warehouseRepository.save(Warehouse.builder()
            .tenant(tenant)
            .code("WH-COAST")
            .name(tenant.getName() + " Coast Hub")
            .location(secondaryLocation == null ? primaryLocation + " Reserve" : secondaryLocation)
            .build());

        if (starterProperties.isSeedStarterInventoryOnTenantOnboarding()) {
            seedStarterInventory(north, coast);
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

        return new TenantOnboardingResponse(
            tenant.getId(),
            tenant.getCode(),
            tenant.getName(),
            adminUser.getUsername(),
            operationsLead.getActorName(),
            executiveUsername,
            executiveApprover.getActorName(),
            List.of(north.getCode(), coast.getCode()),
            Instant.now()
        );
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
                .tenant(north.getTenant())
                .product(product)
                .warehouse(north)
                .quantityAvailable(28L + (index * 4L))
                .quantityOnHand(28L + (index * 4L))
                .quantityReserved(0L)
                .quantityInbound(0L)
                .reorderThreshold(16L + (index * 2L))
                .build());
            inventoryRepository.save(Inventory.builder()
                .tenant(coast.getTenant())
                .product(product)
                .warehouse(coast)
                .quantityAvailable(22L + (index * 4L))
                .quantityOnHand(22L + (index * 4L))
                .quantityReserved(0L)
                .quantityInbound(0L)
                .reorderThreshold(12L + (index * 2L))
                .build());
        }
    }

    private String normalizeTenantCode(String tenantCode) {
        return tenantCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
