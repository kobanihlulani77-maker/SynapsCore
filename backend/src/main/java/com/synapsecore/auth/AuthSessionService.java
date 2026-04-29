package com.synapsecore.auth;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.auth.dto.AuthSessionResponse;
import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.AccessUser;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.domain.repository.AccessUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthSessionService {

    public static final String SESSION_TENANT_CODE_KEY = "synapsecore.auth.tenantCode";
    public static final String SESSION_ACTOR_KEY = "synapsecore.auth.actor";
    public static final String SESSION_USERNAME_KEY = "synapsecore.auth.username";
    public static final String SESSION_AUTHENTICATED_AT_KEY = "synapsecore.auth.authenticatedAt";
    public static final String SESSION_USER_SESSION_VERSION_KEY = "synapsecore.auth.userSessionVersion";
    public static final String SESSION_TENANT_SECURITY_POLICY_VERSION_KEY = "synapsecore.auth.tenantSecurityPolicyVersion";

    private final AccessUserRepository accessUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;
    private final RequestTraceContext requestTraceContext;
    private final OperationalMetricsService operationalMetricsService;

    @Transactional(readOnly = true)
    public AuthSessionResponse signIn(jakarta.servlet.http.HttpServletRequest request,
                                      String tenantCode,
                                      String username,
                                      String password) {
        if (tenantCode == null || tenantCode.isBlank()) {
            operationalMetricsService.recordAuthAttempt(null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "tenantCode is required for sign-in.");
        }
        AccessUser user = resolveUser(tenantCode, username)
            .orElseThrow(() -> {
                log.warn("Sign-in rejected for tenant {} username {} because the user record was not found.", tenantCode, username);
                return invalidCredentials(tenantCode);
            });
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Sign-in rejected for tenant {} username {} due to invalid credentials.", tenantCode, username);
            throw invalidCredentials(tenantCode);
        }

        AccessOperator operator = user.getOperator();
        if (operator == null || !operator.isActive()) {
            log.warn("Sign-in rejected for tenant {} username {} because the operator is inactive.", tenantCode, username);
            throw signInRejected(tenantCode, "Signed-in operator is no longer active.");
        }

        Tenant tenant = resolveTenant(user, operator);
        if (tenant == null || !tenant.isActive()) {
            log.warn("Sign-in rejected for tenant {} username {} because the tenant is inactive.", tenantCode, username);
            throw signInRejected(tenantCode, "Signed-in tenant is no longer active.");
        }

        Instant authenticatedAt = Instant.now();
        jakarta.servlet.http.HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            try {
                existingSession.invalidate();
            } catch (IllegalStateException ignored) {
                // Another filter or container already disposed of the session.
            }
        }
        jakarta.servlet.http.HttpSession session = request.getSession(true);
        writeSession(session, user, operator, tenant, authenticatedAt);
        operationalMetricsService.recordAuthAttempt(tenant.getCode(), true);
        log.info("Signed in user {} for tenant {} as operator {}.", user.getUsername(), tenant.getCode(), operator.getActorName());
        return toResponse(buildSessionState(user, operator, tenant, authenticatedAt));
    }

    public AuthSessionResponse getCurrentSession(jakarta.servlet.http.HttpSession session) {
        if (session == null || !hasSessionIdentity(session)) {
            return signedOut();
        }
        return resolveAuthenticatedSession(session)
            .map(this::toResponse)
            .orElseGet(this::signedOut);
    }

    @Transactional
    public AuthSessionResponse changePassword(jakarta.servlet.http.HttpSession session,
                                              String currentPassword,
                                              String newPassword) {
        AuthenticatedSession authenticatedSession = requireAuthenticatedSession(session, "change the current password");
        AccessUser user = authenticatedSession.user();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "New password must be different from the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(Instant.now());
        user.setPasswordChangeRequired(false);
        user.setSessionVersion(user.getSessionVersion() + 1);
        user = accessUserRepository.save(user);

        Instant authenticatedAt = Instant.now();
        writeSession(session, user, authenticatedSession.operator(), authenticatedSession.tenant(), authenticatedAt);
        auditLogRepository.save(AuditLog.builder()
            .tenantCode(authenticatedSession.tenant().getCode())
            .action("AUTH_PASSWORD_CHANGED")
            .actor(authenticatedSession.operator().getActorName())
            .source("user-session")
            .targetType("AccessUser")
            .targetRef(String.valueOf(user.getId()))
            .status(AuditStatus.SUCCESS)
            .details("Changed the password for signed-in user " + user.getUsername() + ".")
            .requestId(requestTraceContext.getRequiredRequestId())
            .build());
        return toResponse(buildSessionState(user, authenticatedSession.operator(), authenticatedSession.tenant(), authenticatedAt));
    }

    public AuthSessionResponse signOut(jakarta.servlet.http.HttpSession session) {
        session.invalidate();
        return signedOut();
    }

    public boolean hasSessionIdentity(jakarta.servlet.http.HttpSession session) {
        if (session == null) {
            return false;
        }
        try {
            Object usernameAttribute = session.getAttribute(SESSION_USERNAME_KEY);
            return usernameAttribute instanceof String username && !username.isBlank();
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    public Optional<AuthenticatedSession> resolveAuthenticatedSession(jakarta.servlet.http.HttpSession session) {
        if (session == null || !hasSessionIdentity(session)) {
            return Optional.empty();
        }
        return validateSession(session, false, null);
    }

    public AuthenticatedSession requireAuthenticatedSession(jakarta.servlet.http.HttpSession session,
                                                            String actionDescription) {
        return validateSession(session, true, actionDescription)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "A signed-in user session is required to " + actionDescription + "."));
    }

    public void syncTenantSecurityPolicy(jakarta.servlet.http.HttpSession session,
                                         String tenantCode,
                                         long securityPolicyVersion,
                                         int sessionTimeoutMinutes) {
        if (session == null || !hasSessionIdentity(session)) {
            return;
        }
        String sessionTenantCode = readTenantCode(session);
        if (sessionTenantCode == null || !sessionTenantCode.equalsIgnoreCase(tenantCode)) {
            return;
        }
        session.setAttribute(SESSION_TENANT_SECURITY_POLICY_VERSION_KEY, securityPolicyVersion);
        session.setMaxInactiveInterval((int) Duration.ofMinutes(sessionTimeoutMinutes).getSeconds());
    }

    public record AuthenticatedSession(
        AccessUser user,
        AccessOperator operator,
        Tenant tenant,
        Instant authenticatedAt,
        Instant sessionExpiresAt,
        Instant passwordExpiresAt,
        boolean passwordChangeRequired,
        boolean passwordRotationRequired
    ) {
    }

    private Optional<AuthenticatedSession> validateSession(jakarta.servlet.http.HttpSession session,
                                                           boolean strict,
                                                           String actionDescription) {
        String username = readStringAttribute(session, SESSION_USERNAME_KEY);
        if (username == null) {
            return Optional.empty();
        }

        String tenantCode = readTenantCode(session);
        AccessUser user = resolveUser(tenantCode, username)
            .orElseGet(() -> handleInvalidSession(session, strict,
                "Signed-in user is no longer active.", actionDescription));
        if (user == null) {
            return Optional.empty();
        }

        AccessOperator operator = user.getOperator();
        if (operator == null || !operator.isActive()) {
            return Optional.ofNullable(handleInvalidSession(session, strict,
                "Signed-in operator is no longer active.", actionDescription));
        }

        Tenant tenant = resolveTenant(user, operator);
        if (tenant == null || !tenant.isActive()) {
            return Optional.ofNullable(handleInvalidSession(session, strict,
                "Signed-in tenant is no longer active.", actionDescription));
        }

        long sessionVersion = readLongAttribute(session, SESSION_USER_SESSION_VERSION_KEY);
        if (sessionVersion != user.getSessionVersion()) {
            return Optional.ofNullable(handleInvalidSession(session, strict,
                "Signed-in session is no longer valid. Please sign in again.", actionDescription));
        }

        long securityPolicyVersion = readLongAttribute(session, SESSION_TENANT_SECURITY_POLICY_VERSION_KEY);
        if (securityPolicyVersion != tenant.getSecurityPolicyVersion()) {
            return Optional.ofNullable(handleInvalidSession(session, strict,
                "Tenant security policy changed. Please sign in again.", actionDescription));
        }

        Instant authenticatedAt = readAuthenticatedAt(session);
        Instant sessionExpiresAt = authenticatedAt.plus(Duration.ofMinutes(tenant.getSessionTimeoutMinutes()));
        if (Instant.now().isAfter(sessionExpiresAt)) {
            return Optional.ofNullable(handleInvalidSession(session, strict,
                "Signed-in session has expired. Please sign in again.", actionDescription));
        }

        return Optional.of(buildSessionState(user, operator, tenant, authenticatedAt));
    }

    private AuthenticatedSession buildSessionState(AccessUser user,
                                                   AccessOperator operator,
                                                   Tenant tenant,
                                                   Instant authenticatedAt) {
        Instant sessionExpiresAt = authenticatedAt.plus(Duration.ofMinutes(tenant.getSessionTimeoutMinutes()));
        Instant passwordExpiresAt = user.getPasswordUpdatedAt().plus(Duration.ofDays(tenant.getPasswordRotationDays()));
        boolean passwordRotationRequired = Instant.now().isAfter(passwordExpiresAt);
        return new AuthenticatedSession(
            user,
            operator,
            tenant,
            authenticatedAt,
            sessionExpiresAt,
            passwordExpiresAt,
            user.isPasswordChangeRequired(),
            passwordRotationRequired
        );
    }

    private AuthSessionResponse toResponse(AuthenticatedSession authenticatedSession) {
        AccessUser user = authenticatedSession.user();
        AccessOperator operator = authenticatedSession.operator();
        Tenant tenant = authenticatedSession.tenant();
        List<com.synapsecore.access.SynapseAccessRole> roles = operator.getRoles().stream()
            .sorted(Comparator.comparing(Enum::name))
            .toList();
        return new AuthSessionResponse(
            true,
            tenant.getCode(),
            tenant.getName(),
            user.getUsername(),
            operator.getActorName(),
            user.getFullName(),
            roles,
            operator.getWarehouseScopes() == null ? List.of() : operator.getWarehouseScopes().stream().sorted().toList(),
            authenticatedSession.authenticatedAt(),
            authenticatedSession.sessionExpiresAt(),
            authenticatedSession.passwordExpiresAt(),
            authenticatedSession.passwordChangeRequired(),
            authenticatedSession.passwordRotationRequired(),
            tenant.getSessionTimeoutMinutes(),
            tenant.getPasswordRotationDays()
        );
    }

    private Optional<AccessUser> resolveUser(String tenantCode, String username) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return Optional.empty();
        }
        return accessUserRepository.findByTenant_CodeIgnoreCaseAndUsernameIgnoreCaseAndActiveTrue(
            tenantCode.trim(),
            username
        );
    }

    private Tenant resolveTenant(AccessUser user, AccessOperator operator) {
        if (user.getTenant() != null) {
            return user.getTenant();
        }
        return operator.getTenant();
    }

    private void writeSession(jakarta.servlet.http.HttpSession session,
                              AccessUser user,
                              AccessOperator operator,
                              Tenant tenant,
                              Instant authenticatedAt) {
        session.setAttribute(SESSION_USERNAME_KEY, user.getUsername());
        session.setAttribute(SESSION_TENANT_CODE_KEY, tenant.getCode());
        session.setAttribute(SESSION_ACTOR_KEY, operator.getActorName());
        session.setAttribute(SESSION_AUTHENTICATED_AT_KEY, authenticatedAt.toString());
        session.setAttribute(SESSION_USER_SESSION_VERSION_KEY, user.getSessionVersion());
        session.setAttribute(SESSION_TENANT_SECURITY_POLICY_VERSION_KEY, tenant.getSecurityPolicyVersion());
        session.setMaxInactiveInterval((int) Duration.ofMinutes(tenant.getSessionTimeoutMinutes()).getSeconds());
    }

    private Instant readAuthenticatedAt(jakarta.servlet.http.HttpSession session) {
        String rawValue = readStringAttribute(session, SESSION_AUTHENTICATED_AT_KEY);
        if (rawValue == null) {
            return Instant.now();
        }
        try {
            return Instant.parse(rawValue);
        } catch (java.time.format.DateTimeParseException ignored) {
            return Instant.now();
        }
    }

    private String readTenantCode(jakarta.servlet.http.HttpSession session) {
        return readStringAttribute(session, SESSION_TENANT_CODE_KEY);
    }

    private String readStringAttribute(jakarta.servlet.http.HttpSession session, String key) {
        try {
            Object value = session.getAttribute(key);
            if (value instanceof String rawValue && !rawValue.isBlank()) {
                return rawValue.trim();
            }
        } catch (IllegalStateException exception) {
            return null;
        }
        return null;
    }

    private long readLongAttribute(jakarta.servlet.http.HttpSession session, String key) {
        try {
            Object value = session.getAttribute(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String rawValue && !rawValue.isBlank()) {
                return Long.parseLong(rawValue.trim());
            }
        } catch (IllegalStateException | NumberFormatException exception) {
            return -1L;
        }
        return -1L;
    }

    private <T> T handleInvalidSession(jakarta.servlet.http.HttpSession session,
                                       boolean strict,
                                       String message,
                                       String actionDescription) {
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // Session is already gone; treat it as invalid either way.
        }
        if (strict) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
        }
        return null;
    }

    private AuthSessionResponse signedOut() {
        return new AuthSessionResponse(
            false,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            null,
            null,
            null,
            false,
            false,
            null,
            null
        );
    }

    private FastAuthFailureException invalidCredentials(String tenantCode) {
        return signInRejected(tenantCode, "Invalid operator credentials.");
    }

    private FastAuthFailureException signInRejected(String tenantCode, String message) {
        operationalMetricsService.recordAuthAttempt(tenantCode, false);
        return new FastAuthFailureException(message);
    }
}
