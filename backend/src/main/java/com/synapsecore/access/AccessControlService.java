package com.synapsecore.access;

import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.config.SynapseAccessProperties;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.scenario.ScenarioActorRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    public static final String ACTOR_HEADER = "X-Synapse-Actor";
    public static final String TENANT_HEADER = "X-Synapse-Tenant";
    public static final String ROLES_HEADER = "X-Synapse-Roles";

    private final AccessDirectoryService accessDirectoryService;
    private final AuthSessionService authSessionService;
    private final SynapseAccessProperties accessProperties;

    public SynapseActorContext requireScenarioActor(ScenarioActorRole declaredRole,
                                                    String declaredActorName,
                                                    String actionDescription) {
        SynapseAccessRole requiredRole = mapScenarioRole(declaredRole);
        SynapseActorContext actor = requireAnyRole(Set.of(requiredRole), actionDescription);
        if (!actor.actorName().equalsIgnoreCase(declaredActorName.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Current actor " + actor.actorName() + " cannot " + actionDescription
                    + " while declaring " + declaredActorName.trim() + ".");
        }
        return actor;
    }

    public SynapseActorContext requireIntegrationAdmin(String actionDescription) {
        return requireAnyRole(Set.of(SynapseAccessRole.INTEGRATION_ADMIN), actionDescription);
    }

    public SynapseActorContext requireIntegrationOperator(String actionDescription) {
        return requireAnyRole(Set.of(SynapseAccessRole.INTEGRATION_OPERATOR, SynapseAccessRole.INTEGRATION_ADMIN),
            actionDescription);
    }

    public SynapseActorContext requireTenantAdmin(String actionDescription) {
        return requireAnyRole(Set.of(SynapseAccessRole.TENANT_ADMIN), actionDescription);
    }

    public SynapseActorContext requireTenantAdminControl(String actionDescription) {
        AccessOperator operator = requireCurrentOperator(actionDescription);
        if (operator == null) {
            return new SynapseActorContext("dev-anonymous", Set.of());
        }
        if (!operator.getRoles().contains(SynapseAccessRole.TENANT_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Signed-in operator " + operator.getActorName()
                    + " does not have one of the required roles "
                    + Set.of(SynapseAccessRole.TENANT_ADMIN)
                    + " to " + actionDescription + ".");
        }
        return new SynapseActorContext(operator.getActorName(), operator.getRoles());
    }

    public SynapseActorContext requireWorkspaceAccess(String actionDescription) {
        AccessOperator operator = requireCurrentOperator(actionDescription);
        if (operator == null) {
            return new SynapseActorContext("dev-anonymous", Set.of());
        }
        return new SynapseActorContext(operator.getActorName(), operator.getRoles());
    }

    public SynapseActorContext requireWorkspaceWarehouseAccess(String warehouseCode, String actionDescription) {
        AccessOperator operator = requireCurrentOperator(actionDescription);
        if (operator == null) {
            return new SynapseActorContext("dev-anonymous", Set.of());
        }
        accessDirectoryService.requireWarehouseAccess(operator, warehouseCode, actionDescription);
        return new SynapseActorContext(operator.getActorName(), operator.getRoles());
    }

    private SynapseActorContext requireAnyRole(Set<SynapseAccessRole> requiredRoles, String actionDescription) {
        HttpServletRequest request = currentRequest();
        HttpSession session = request.getSession(false);
        if (session != null && authSessionService.hasSessionIdentity(session)) {
            var authenticatedSession = authSessionService.requireAuthenticatedSession(session, actionDescription);
            var sessionOperator = accessDirectoryService.requireActiveOperator(
                authenticatedSession.operator().getActorName(),
                authenticatedSession.tenant().getCode(),
                actionDescription
            );
            boolean authorizedBySession = sessionOperator.getRoles().stream().anyMatch(requiredRoles::contains);
            if (!authorizedBySession) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Signed-in operator " + sessionOperator.getActorName()
                        + " does not have one of the required roles " + requiredRoles
                        + " to " + actionDescription + ".");
            }
            return new SynapseActorContext(sessionOperator.getActorName(), sessionOperator.getRoles());
        }

        if (!accessProperties.isAllowHeaderFallback()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "A signed-in user session is required to " + actionDescription + ".");
        }

        String actorName = normalizeActorName(request.getHeader(ACTOR_HEADER));
        String tenantCode = normalizeTenantCode(request.getHeader(TENANT_HEADER));
        Set<SynapseAccessRole> actorRoles = parseRoles(request.getHeader(ROLES_HEADER));
        var operator = accessDirectoryService.requireActiveOperator(actorName, tenantCode, actionDescription);

        EnumSet<SynapseAccessRole> claimedButUnassigned = EnumSet.noneOf(SynapseAccessRole.class);
        for (SynapseAccessRole actorRole : actorRoles) {
            if (!operator.getRoles().contains(actorRole)) {
                claimedButUnassigned.add(actorRole);
            }
        }
        if (!claimedButUnassigned.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Current actor " + operator.getActorName() + " cannot claim roles "
                    + claimedButUnassigned + " because assigned roles are " + operator.getRoles() + ".");
        }

        boolean authorized = actorRoles.stream().anyMatch(requiredRoles::contains);
        if (!authorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Current actor " + operator.getActorName() + " must declare one of the required roles "
                    + requiredRoles + " to " + actionDescription + ".");
        }

        return new SynapseActorContext(operator.getActorName(), actorRoles);
    }

    private AccessOperator requireCurrentOperator(String actionDescription) {
        HttpServletRequest request = currentRequest();
        HttpSession session = request.getSession(false);
        if (session != null && authSessionService.hasSessionIdentity(session)) {
            var authenticatedSession = authSessionService.requireAuthenticatedSession(session, actionDescription);
            return accessDirectoryService.requireActiveOperator(
                authenticatedSession.operator().getActorName(),
                authenticatedSession.tenant().getCode(),
                actionDescription
            );
        }

        if (!accessProperties.isAllowHeaderFallback()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "A signed-in user session is required to " + actionDescription + ".");
        }

        String actorNameHeader = request.getHeader(ACTOR_HEADER);
        if (actorNameHeader == null || actorNameHeader.isBlank()) {
            return null;
        }

        String actorName = normalizeActorName(actorNameHeader);
        String tenantCode = normalizeTenantCode(request.getHeader(TENANT_HEADER));
        return accessDirectoryService.requireActiveOperator(actorName, tenantCode, actionDescription);
    }

    private String normalizeActorName(String actorName) {
        if (actorName == null || actorName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Header " + ACTOR_HEADER + " is required for this operation.");
        }
        return actorName.trim();
    }

    private String normalizeTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return null;
        }
        return tenantCode.trim();
    }

    private Set<SynapseAccessRole> parseRoles(String rawRoles) {
        if (rawRoles == null || rawRoles.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Header " + ROLES_HEADER + " is required for this operation.");
        }

        EnumSet<SynapseAccessRole> roles = EnumSet.noneOf(SynapseAccessRole.class);
        for (String rawRole : rawRoles.split(",")) {
            String normalized = rawRole.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            try {
                roles.add(SynapseAccessRole.valueOf(normalized.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown access role in " + ROLES_HEADER + ": " + normalized);
            }
        }

        if (roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Header " + ROLES_HEADER + " must include at least one access role.");
        }
        return roles;
    }

    private SynapseAccessRole mapScenarioRole(ScenarioActorRole role) {
        return switch (role) {
            case REVIEW_OWNER -> SynapseAccessRole.REVIEW_OWNER;
            case FINAL_APPROVER -> SynapseAccessRole.FINAL_APPROVER;
            case ESCALATION_OWNER -> SynapseAccessRole.ESCALATION_OWNER;
            case REQUESTER -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scenario requester role is not valid for protected review actions.");
        };
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Request context is required for access-controlled operations.");
        }
        return attributes.getRequest();
    }
}
