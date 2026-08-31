package com.synapsecore.platform;

import com.synapsecore.audit.AuditLogPersistenceService;
import com.synapsecore.config.SynapsePlatformOwnerProperties;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.platform.dto.PlatformSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PlatformOwnerSessionService {

    public static final String SESSION_USERNAME_KEY = "synapsecore.platform.username";
    public static final String SESSION_AUTHENTICATED_AT_KEY = "synapsecore.platform.authenticatedAt";
    private static final String PLATFORM_AUDIT_TENANT = "PLATFORM";

    private final SynapsePlatformOwnerProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogPersistenceService auditLogPersistenceService;

    public PlatformSessionResponse signIn(HttpServletRequest request, String username, String password) {
        if (!properties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Platform-owner sign-in is not configured for this environment.");
        }
        String normalizedUsername = username == null ? "" : username.trim();
        if (!normalizedUsername.equalsIgnoreCase(properties.getUsername().trim())
                || !passwordEncoder.matches(password, properties.getPasswordHash().trim())) {
            recordAudit(
                "PLATFORM_AUTH_LOGIN",
                "anonymous-platform-user",
                AuditStatus.FAILURE,
                "Platform-owner sign-in rejected."
            );
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid platform-owner credentials.");
        }

        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            invalidateSafely(existingSession);
        }
        HttpSession session = request.getSession(true);
        Instant authenticatedAt = Instant.now();
        session.setAttribute(SESSION_USERNAME_KEY, properties.getUsername().trim());
        session.setAttribute(SESSION_AUTHENTICATED_AT_KEY, authenticatedAt.toString());
        session.setMaxInactiveInterval((int) Duration.ofMinutes(properties.getSessionTimeoutMinutes()).getSeconds());
        recordAudit(
            "PLATFORM_AUTH_LOGIN",
            properties.getUsername().trim(),
            AuditStatus.SUCCESS,
            "Platform-owner session established."
        );
        return signedIn(authenticatedAt);
    }

    public PlatformSessionResponse getCurrentSession(HttpSession session) {
        return resolveAuthenticatedAt(session).map(this::signedIn).orElseGet(this::signedOut);
    }

    public PlatformSessionResponse signOut(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && hasSessionIdentity(session)) {
            recordAudit(
                "PLATFORM_AUTH_LOGOUT",
                readString(session, SESSION_USERNAME_KEY),
                AuditStatus.SUCCESS,
                "Platform-owner session ended."
            );
            invalidateSafely(session);
        }
        return signedOut();
    }

    public String requirePlatformOwner(HttpSession session, String actionDescription) {
        resolveAuthenticatedAt(session).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "A signed-in platform-owner session is required to " + actionDescription + "."
        ));
        return properties.getUsername().trim();
    }

    public boolean hasSessionIdentity(HttpSession session) {
        return readString(session, SESSION_USERNAME_KEY) != null;
    }

    public boolean hasAuthenticatedSession(HttpSession session) {
        return resolveAuthenticatedAt(session).isPresent();
    }

    public void clearSessionIdentity(HttpSession session) {
        if (session == null) {
            return;
        }
        try {
            session.removeAttribute(SESSION_USERNAME_KEY);
            session.removeAttribute(SESSION_AUTHENTICATED_AT_KEY);
        } catch (IllegalStateException ignored) {
            // The session is already invalidated.
        }
    }

    private Optional<Instant> resolveAuthenticatedAt(HttpSession session) {
        if (!hasSessionIdentity(session)) {
            return Optional.empty();
        }
        String username = readString(session, SESSION_USERNAME_KEY);
        String authenticatedAtValue = readString(session, SESSION_AUTHENTICATED_AT_KEY);
        if (!properties.isConfigured() || username == null
                || !username.equalsIgnoreCase(properties.getUsername().trim())
                || authenticatedAtValue == null) {
            clearSessionIdentity(session);
            return Optional.empty();
        }
        try {
            Instant authenticatedAt = Instant.parse(authenticatedAtValue);
            if (Instant.now().isAfter(authenticatedAt.plus(Duration.ofMinutes(properties.getSessionTimeoutMinutes())))) {
                clearSessionIdentity(session);
                return Optional.empty();
            }
            return Optional.of(authenticatedAt);
        } catch (java.time.format.DateTimeParseException exception) {
            clearSessionIdentity(session);
            return Optional.empty();
        }
    }

    private PlatformSessionResponse signedIn(Instant authenticatedAt) {
        return new PlatformSessionResponse(
            true,
            properties.getUsername().trim(),
            properties.getDisplayName(),
            authenticatedAt,
            authenticatedAt.plus(Duration.ofMinutes(properties.getSessionTimeoutMinutes()))
        );
    }

    private PlatformSessionResponse signedOut() {
        return new PlatformSessionResponse(false, null, null, null, null);
    }

    private String readString(HttpSession session, String key) {
        if (session == null) {
            return null;
        }
        try {
            Object value = session.getAttribute(key);
            return value instanceof String text && !text.isBlank() ? text.trim() : null;
        } catch (IllegalStateException exception) {
            return null;
        }
    }

    private void invalidateSafely(HttpSession session) {
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // The session is already invalidated.
        }
    }

    private void recordAudit(String action, String actor, AuditStatus status, String details) {
        auditLogPersistenceService.recordForTenant(
            PLATFORM_AUDIT_TENANT,
            action,
            actor,
            "platform-session",
            "PlatformControlPlane",
            "session",
            status,
            details
        );
    }
}
