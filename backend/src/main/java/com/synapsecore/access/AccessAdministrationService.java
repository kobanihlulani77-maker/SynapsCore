package com.synapsecore.access;

import com.synapsecore.access.dto.AccessOperatorResponse;
import com.synapsecore.access.dto.AccessOperatorUpsertRequest;
import com.synapsecore.access.dto.AccessUserCreateRequest;
import com.synapsecore.access.dto.AccessUserPasswordResetRequest;
import com.synapsecore.access.dto.AccessUserResponse;
import com.synapsecore.access.dto.AccessUserUpdateRequest;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.AccessUser;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.tenant.TenantContextService;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccessAdministrationService {

    private final AccessOperatorRepository accessOperatorRepository;
    private final AccessUserRepository accessUserRepository;
    private final WarehouseRepository warehouseRepository;
    private final TenantContextService tenantContextService;
    private final AccessDirectoryService accessDirectoryService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public List<AccessOperatorResponse> getTenantOperators() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        return accessOperatorRepository.findAllByTenant_CodeIgnoreCaseOrderByDisplayNameAsc(tenantCode).stream()
            .map(this::toOperatorResponse)
            .toList();
    }

    public List<AccessUserResponse> getTenantUsers() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        return accessUserRepository.findAllByTenant_CodeIgnoreCaseOrderByFullNameAscUsernameAsc(tenantCode).stream()
            .map(this::toUserResponse)
            .toList();
    }

    @Transactional
    public AccessOperatorResponse createOperator(AccessOperatorUpsertRequest request, String actorName) {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        String normalizedActorName = normalizeActorName(request.actorName());
        if (accessOperatorRepository.findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase(tenant.getCode(), normalizedActorName).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Operator already exists in tenant " + tenant.getCode() + ": " + normalizedActorName);
        }

        AccessOperator operator = accessOperatorRepository.save(AccessOperator.builder()
            .tenant(tenant)
            .actorName(normalizedActorName)
            .displayName(request.displayName().trim())
            .description(normalizeOptional(request.description()))
            .active(request.active())
            .roles(normalizeRoles(request.roles()))
            .warehouseScopes(normalizeWarehouseScopes(tenant.getCode(), request.warehouseScopes()))
            .build());

        auditLogService.recordSuccess(
            "ACCESS_OPERATOR_CREATED",
            actorName,
            "tenant-admin",
            "AccessOperator",
            String.valueOf(operator.getId()),
            "Created operator " + operator.getActorName() + " with roles " + operator.getRoles()
                + " and warehouse scopes " + accessDirectoryService.getWarehouseScopes(operator) + "."
        );

        return toOperatorResponse(operator);
    }

    @Transactional
    public AccessOperatorResponse updateOperator(Long operatorId,
                                                 AccessOperatorUpsertRequest request,
                                                 String actorName) {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        AccessOperator operator = accessOperatorRepository.findByTenant_CodeIgnoreCaseAndId(tenant.getCode(), operatorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Operator not found in current tenant: " + operatorId));

        String normalizedActorName = normalizeActorName(request.actorName());
        Long existingOperatorId = operator.getId();
        accessOperatorRepository.findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase(tenant.getCode(), normalizedActorName)
            .filter(existing -> !existing.getId().equals(existingOperatorId))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Operator already exists in tenant " + tenant.getCode() + ": " + normalizedActorName);
            });

        EnumSet<SynapseAccessRole> nextRoles = normalizeRoles(request.roles());
        boolean nextActive = request.active();
        if (operator.isActive()
            && operator.getRoles().contains(SynapseAccessRole.TENANT_ADMIN)
            && (!nextActive || !nextRoles.contains(SynapseAccessRole.TENANT_ADMIN))) {
            if (countOtherActiveTenantAdmins(tenant.getCode(), operator.getId()) == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tenant must keep at least one active tenant admin.");
            }
            if (countOtherUsableTenantAdminUsers(tenant.getCode(), null, operator.getId()) == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tenant must keep at least one active tenant admin sign-in lane.");
            }
        }

        operator.setActorName(normalizedActorName);
        operator.setDisplayName(request.displayName().trim());
        operator.setDescription(normalizeOptional(request.description()));
        operator.setActive(nextActive);
        operator.setRoles(nextRoles);
        operator.setWarehouseScopes(normalizeWarehouseScopes(tenant.getCode(), request.warehouseScopes()));
        operator = accessOperatorRepository.save(operator);

        auditLogService.recordSuccess(
            "ACCESS_OPERATOR_UPDATED",
            actorName,
            "tenant-admin",
            "AccessOperator",
            String.valueOf(operator.getId()),
            "Updated operator " + operator.getActorName() + " with roles " + operator.getRoles()
                + " and warehouse scopes " + accessDirectoryService.getWarehouseScopes(operator) + "."
        );

        return toOperatorResponse(operator);
    }

    @Transactional
    public AccessUserResponse createUser(AccessUserCreateRequest request, String actorName) {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        String normalizedUsername = normalizeUsername(request.username());
        if (accessUserRepository.findByTenant_CodeIgnoreCaseAndUsernameIgnoreCase(tenant.getCode(), normalizedUsername).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Username already exists in tenant " + tenant.getCode() + ": " + normalizedUsername);
        }

        AccessOperator operator = accessDirectoryService.requireActiveOperator(
            request.operatorActorName().trim(),
            tenant.getCode(),
            "map new user accounts"
        );

        AccessUser user = accessUserRepository.save(AccessUser.builder()
            .tenant(tenant)
            .username(normalizedUsername)
            .fullName(request.fullName().trim())
            .passwordHash(passwordEncoder.encode(request.password().trim()))
            .active(true)
            .passwordChangeRequired(true)
            .passwordUpdatedAt(Instant.now())
            .operator(operator)
            .build());

        auditLogService.recordSuccess(
            "ACCESS_USER_CREATED",
            actorName,
            "tenant-admin",
            "AccessUser",
            String.valueOf(user.getId()),
            "Created user " + user.getUsername() + " mapped to operator " + operator.getActorName() + "."
        );

        return toUserResponse(user);
    }

    @Transactional
    public AccessUserResponse updateUser(Long userId, AccessUserUpdateRequest request, String actorName) {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        AccessUser user = accessUserRepository.findByTenant_CodeIgnoreCaseAndId(tenant.getCode(), userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "User not found in current tenant: " + userId));

        AccessOperator operator = accessDirectoryService.requireActiveOperator(
            request.operatorActorName().trim(),
            tenant.getCode(),
            "remap tenant user accounts"
        );

        boolean nextActive = request.active();
        boolean invalidateActiveSessions = user.isActive() != nextActive
            || user.getOperator() == null
            || !user.getOperator().getId().equals(operator.getId());
        if (isUsableTenantAdminUser(user)
            && (!nextActive || !isTenantAdminOperator(operator))
            && countOtherUsableTenantAdminUsers(tenant.getCode(), user.getId(), null) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Tenant must keep at least one active tenant admin sign-in lane.");
        }

        user.setFullName(request.fullName().trim());
        user.setActive(nextActive);
        user.setOperator(operator);
        if (invalidateActiveSessions) {
            user.setSessionVersion(user.getSessionVersion() + 1);
        }
        user = accessUserRepository.save(user);

        auditLogService.recordSuccess(
            "ACCESS_USER_UPDATED",
            actorName,
            "tenant-admin",
            "AccessUser",
            String.valueOf(user.getId()),
            "Updated user " + user.getUsername() + " mapped to operator " + operator.getActorName()
                + " with active=" + user.isActive() + "."
        );

        return toUserResponse(user);
    }

    @Transactional
    public AccessUserResponse resetUserPassword(Long userId,
                                                AccessUserPasswordResetRequest request,
                                                String actorName) {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        AccessUser user = accessUserRepository.findByTenant_CodeIgnoreCaseAndId(tenant.getCode(), userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "User not found in current tenant: " + userId));

        user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        user.setPasswordUpdatedAt(Instant.now());
        user.setPasswordChangeRequired(true);
        user.setSessionVersion(user.getSessionVersion() + 1);
        user = accessUserRepository.save(user);

        auditLogService.recordSuccess(
            "ACCESS_USER_PASSWORD_RESET",
            actorName,
            "tenant-admin",
            "AccessUser",
            String.valueOf(user.getId()),
            "Reset password for user " + user.getUsername() + "."
        );

        return toUserResponse(user);
    }

    private long countOtherActiveTenantAdmins(String tenantCode, Long excludedOperatorId) {
        return accessOperatorRepository.findAllByTenant_CodeIgnoreCaseOrderByDisplayNameAsc(tenantCode).stream()
            .filter(AccessOperator::isActive)
            .filter(operator -> !operator.getId().equals(excludedOperatorId))
            .filter(operator -> operator.getRoles().contains(SynapseAccessRole.TENANT_ADMIN))
            .count();
    }

    private long countOtherUsableTenantAdminUsers(String tenantCode, Long excludedUserId, Long excludedOperatorId) {
        return accessUserRepository.findAllByTenant_CodeIgnoreCaseOrderByFullNameAscUsernameAsc(tenantCode).stream()
            .filter(AccessUser::isActive)
            .filter(user -> excludedUserId == null || !user.getId().equals(excludedUserId))
            .filter(user -> user.getOperator() != null)
            .filter(user -> excludedOperatorId == null || !user.getOperator().getId().equals(excludedOperatorId))
            .filter(this::isUsableTenantAdminUser)
            .count();
    }

    private String normalizeActorName(String actorName) {
        if (actorName == null || actorName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "actorName is required.");
        }
        return actorName.trim();
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

    private EnumSet<SynapseAccessRole> normalizeRoles(List<SynapseAccessRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return EnumSet.noneOf(SynapseAccessRole.class);
        }
        return EnumSet.copyOf(roles);
    }

    private Set<String> normalizeWarehouseScopes(String tenantCode, List<String> warehouseScopes) {
        if (warehouseScopes == null || warehouseScopes.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<String> normalized = warehouseScopes.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(value -> value.trim().toUpperCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<String> validWarehouses = warehouseRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(tenantCode).stream()
            .map(warehouse -> warehouse.getCode().trim().toUpperCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toSet());
        List<String> invalid = normalized.stream()
            .filter(scope -> !validWarehouses.contains(scope))
            .sorted()
            .toList();
        if (!invalid.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unknown warehouse scopes for tenant " + tenantCode + ": " + invalid);
        }
        return normalized;
    }

    private AccessOperatorResponse toOperatorResponse(AccessOperator operator) {
        return new AccessOperatorResponse(
            operator.getId(),
            operator.getTenant() == null ? null : operator.getTenant().getCode(),
            operator.getTenant() == null ? null : operator.getTenant().getName(),
            operator.getActorName(),
            operator.getDisplayName(),
            operator.getRoles().stream().sorted(Comparator.comparing(Enum::name)).toList(),
            accessDirectoryService.getWarehouseScopes(operator),
            operator.isActive(),
            operator.getDescription(),
            operator.getCreatedAt(),
            operator.getUpdatedAt()
        );
    }

    private boolean isUsableTenantAdminUser(AccessUser user) {
        return user.isActive()
            && user.getOperator() != null
            && user.getOperator().isActive()
            && isTenantAdminOperator(user.getOperator());
    }

    private boolean isTenantAdminOperator(AccessOperator operator) {
        return operator.getRoles().contains(SynapseAccessRole.TENANT_ADMIN);
    }

    private AccessUserResponse toUserResponse(AccessUser user) {
        AccessOperator operator = user.getOperator();
        return new AccessUserResponse(
            user.getId(),
            user.getTenant() == null ? null : user.getTenant().getCode(),
            user.getTenant() == null ? null : user.getTenant().getName(),
            user.getUsername(),
            user.getFullName(),
            operator.getActorName(),
            operator.getDisplayName(),
            operator.getRoles().stream().sorted(Comparator.comparing(Enum::name)).toList(),
            accessDirectoryService.getWarehouseScopes(operator),
            user.isActive(),
            user.isPasswordChangeRequired(),
            user.getPasswordUpdatedAt(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
