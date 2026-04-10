package com.synapsecore.access;

import com.synapsecore.access.dto.AccessOperatorResponse;
import com.synapsecore.access.dto.TenantResponse;
import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.config.SynapseAccessProperties;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.tenant.TenantContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccessDirectoryService {

    private final AccessOperatorRepository accessOperatorRepository;
    private final TenantRepository tenantRepository;
    private final TenantContextService tenantContextService;
    private final SynapseAccessProperties accessProperties;
    private final AuthSessionService authSessionService;

    public List<TenantResponse> getActiveTenants() {
        return tenantRepository.findAllByActiveTrueOrderByNameAsc().stream()
            .map(this::toTenantResponse)
            .toList();
    }

    public List<AccessOperatorResponse> getActiveOperators(String tenantCode) {
        List<AccessOperator> operators = tenantCode == null || tenantCode.isBlank()
            ? accessOperatorRepository.findAllByActiveTrueOrderByDisplayNameAsc()
            : accessOperatorRepository.findAllByTenant_CodeIgnoreCaseAndActiveTrueOrderByDisplayNameAsc(tenantCode.trim());
        return operators.stream()
            .map(this::toResponse)
            .toList();
    }

    public AccessOperator requireActiveOperator(String actorName, String actionDescription) {
        return requireActiveOperator(actorName, null, actionDescription);
    }

    public AccessOperator requireActiveOperator(String actorName, String tenantCode, String actionDescription) {
        if (tenantCode != null && !tenantCode.isBlank()) {
            return accessOperatorRepository.findByTenant_CodeIgnoreCaseAndActorNameIgnoreCaseAndActiveTrue(
                    tenantCode.trim(),
                    actorName.trim()
                )
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Current actor " + actorName.trim()
                        + " is not a known active operator in tenant " + tenantCode.trim()
                        + " and cannot " + actionDescription + "."
                ));
        }
        return accessOperatorRepository.findByActorNameIgnoreCaseAndActiveTrue(actorName.trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Current actor " + actorName.trim()
                    + " is not a known active operator and cannot " + actionDescription + "."
            ));
    }

    public String requireActiveOperatorName(String actorName, String fieldLabel) {
        return requireActiveOperatorForField(actorName, fieldLabel).getActorName();
    }

    public String requireOperatorWithRoleName(String actorName,
                                              SynapseAccessRole requiredRole,
                                              String fieldLabel) {
        return requireOperatorWithRoleName(actorName, requiredRole, fieldLabel, null);
    }

    public String requireOperatorWithRoleName(String actorName,
                                              SynapseAccessRole requiredRole,
                                              String fieldLabel,
                                              String warehouseCode) {
        AccessOperator operator = requireActiveOperatorForField(actorName, fieldLabel);
        if (!operator.getRoles().contains(requiredRole)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                capitalize(fieldLabel) + " must be an active operator with role " + requiredRole + "."
            );
        }
        requireWarehouseAccess(operator, warehouseCode, "act as " + fieldLabel);
        return operator.getActorName();
    }

    public Optional<AccessOperator> getCurrentOperator() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }

        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        if (session != null && authSessionService.hasSessionIdentity(session)) {
            return authSessionService.resolveAuthenticatedSession(session)
                .map(AuthSessionService.AuthenticatedSession::operator);
        }

        if (!accessProperties.isAllowHeaderFallback()) {
            return Optional.empty();
        }

        String actorHeader = request.getHeader(AccessControlService.ACTOR_HEADER);
        if (actorHeader == null || actorHeader.isBlank()) {
            return Optional.empty();
        }
        String tenantHeader = request.getHeader(AccessControlService.TENANT_HEADER);
        String tenantCode = tenantHeader == null || tenantHeader.isBlank()
            ? tenantContextService.getCurrentTenantCodeOrDefault()
            : tenantHeader.trim();
        return accessOperatorRepository.findByTenant_CodeIgnoreCaseAndActorNameIgnoreCaseAndActiveTrue(
            tenantCode,
            actorHeader.trim()
        );
    }

    public void requireOperatorWarehouseAccess(String actorName,
                                               String tenantCode,
                                               String warehouseCode,
                                               String actionDescription) {
        AccessOperator operator = requireActiveOperator(actorName, tenantCode, actionDescription);
        requireWarehouseAccess(operator, warehouseCode, actionDescription);
    }

    public void requireWarehouseAccess(AccessOperator operator,
                                       String warehouseCode,
                                       String actionDescription) {
        String normalizedWarehouseCode = normalizeWarehouseCode(warehouseCode);
        if (normalizedWarehouseCode == null || hasWarehouseAccess(operator, normalizedWarehouseCode)) {
            return;
        }
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Current actor " + operator.getActorName()
                + " is not assigned to warehouse " + normalizedWarehouseCode
                + " and cannot " + actionDescription + "."
        );
    }

    public boolean hasWarehouseAccess(AccessOperator operator, String warehouseCode) {
        String normalizedWarehouseCode = normalizeWarehouseCode(warehouseCode);
        if (normalizedWarehouseCode == null) {
            return true;
        }
        return operator.getWarehouseScopes() == null
            || operator.getWarehouseScopes().isEmpty()
            || operator.getWarehouseScopes().stream()
                .anyMatch(scope -> normalizedWarehouseCode.equalsIgnoreCase(scope));
    }

    public List<String> getWarehouseScopes(AccessOperator operator) {
        if (operator.getWarehouseScopes() == null || operator.getWarehouseScopes().isEmpty()) {
            return List.of();
        }
        return operator.getWarehouseScopes().stream()
            .map(scope -> scope.trim().toUpperCase(Locale.ROOT))
            .sorted()
            .toList();
    }

    private AccessOperator requireActiveOperatorForField(String actorName, String fieldLabel) {
        if (actorName == null || actorName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, capitalize(fieldLabel) + " is required.");
        }
        return accessOperatorRepository.findByTenant_CodeIgnoreCaseAndActorNameIgnoreCaseAndActiveTrue(
                tenantContextService.getCurrentTenantCodeOrDefault(),
                actorName.trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                capitalize(fieldLabel) + " must be an active known operator in the current tenant."
            ));
    }

    private AccessOperatorResponse toResponse(AccessOperator operator) {
        return new AccessOperatorResponse(
            operator.getId(),
            operator.getTenant() != null ? operator.getTenant().getCode() : null,
            operator.getTenant() != null ? operator.getTenant().getName() : null,
            operator.getActorName(),
            operator.getDisplayName(),
            operator.getRoles().stream().sorted(Comparator.comparing(Enum::name)).toList(),
            getWarehouseScopes(operator),
            operator.isActive(),
            operator.getDescription(),
            operator.getCreatedAt(),
            operator.getUpdatedAt()
        );
    }

    private TenantResponse toTenantResponse(Tenant tenant) {
        return new TenantResponse(
            tenant.getId(),
            tenant.getCode(),
            tenant.getName(),
            tenant.getDescription(),
            tenant.isActive(),
            tenant.getCreatedAt(),
            tenant.getUpdatedAt()
        );
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Field";
        }
        String trimmed = value.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }

    private String normalizeWarehouseCode(String warehouseCode) {
        if (warehouseCode == null || warehouseCode.isBlank()) {
            return null;
        }
        return warehouseCode.trim().toUpperCase(Locale.ROOT);
    }
}
