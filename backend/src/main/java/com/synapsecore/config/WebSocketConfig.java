package com.synapsecore.config;

import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.platform.PlatformOwnerSessionService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    static final String SESSION_TENANT_CODE_ATTRIBUTE = "synapsecoreTenantCode";
    static final String SESSION_ROLES_ATTRIBUTE = "synapsecoreRoles";
    static final String SESSION_TENANT_WIDE_ATTRIBUTE = "synapsecoreTenantWide";
    static final String SESSION_HTTP_SESSION_ATTRIBUTE = "synapsecoreHttpSession";
    static final String SESSION_AUTHORITY_ATTRIBUTE = "synapsecoreAuthority";
    static final String TENANT_AUTHORITY = "TENANT";
    static final String PLATFORM_AUTHORITY = "PLATFORM";
    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final List<String> allowedOrigins;
    private final SynapseAccessProperties accessProperties;
    private final AuthSessionService authSessionService;
    private final PlatformOwnerSessionService platformOwnerSessionService;
    private final SynapseRealtimeProperties realtimeProperties;
    private final RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();

    public WebSocketConfig(SynapseCorsProperties corsProperties,
                           SynapseAccessProperties accessProperties,
                           AuthSessionService authSessionService,
                           PlatformOwnerSessionService platformOwnerSessionService,
                           SynapseRealtimeProperties realtimeProperties) {
        this.allowedOrigins = corsProperties.getAllowedOrigins();
        this.accessProperties = accessProperties;
        this.authSessionService = authSessionService;
        this.platformOwnerSessionService = platformOwnerSessionService;
        this.realtimeProperties = realtimeProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (realtimeProperties.stompRelayEnabled()) {
            var relay = registry.enableStompBrokerRelay("/topic")
                .setRelayHost(realtimeProperties.getRelayHost())
                .setRelayPort(realtimeProperties.getRelayPort())
                .setClientLogin(realtimeProperties.getRelayClientLogin())
                .setClientPasscode(realtimeProperties.getRelayClientPasscode())
                .setSystemLogin(realtimeProperties.getRelaySystemLogin())
                .setSystemPasscode(realtimeProperties.getRelaySystemPasscode())
                .setSystemHeartbeatSendInterval(realtimeProperties.getRelaySystemHeartbeatSendMs())
                .setSystemHeartbeatReceiveInterval(realtimeProperties.getRelaySystemHeartbeatReceiveMs());
            if (realtimeProperties.getRelayVirtualHost() != null && !realtimeProperties.getRelayVirtualHost().isBlank()) {
                relay.setVirtualHost(realtimeProperties.getRelayVirtualHost().trim());
            }
        } else {
            registry.enableSimpleBroker("/topic");
        }
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var endpoint = registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));

        if (!accessProperties.isAllowHeaderFallback()) {
            endpoint.addInterceptors(new AuthenticatedSessionHandshakeInterceptor(authSessionService, platformOwnerSessionService));
        }

        var sockJsRegistration = endpoint.withSockJS();
        if (!accessProperties.isAllowHeaderFallback()) {
            sockJsRegistration.setSessionCookieNeeded(true);
        }
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        if (!accessProperties.isAllowHeaderFallback()) {
            registration.interceptors(new TenantSubscriptionChannelInterceptor(authSessionService, platformOwnerSessionService, realtimeSessionRegistry));
        }
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        if (!accessProperties.isAllowHeaderFallback()) {
            registration.interceptors(new TenantSubscriptionChannelInterceptor(authSessionService, platformOwnerSessionService, realtimeSessionRegistry));
        }
    }

    private static final class AuthenticatedSessionHandshakeInterceptor implements HandshakeInterceptor {

        private final AuthSessionService authSessionService;
        private final PlatformOwnerSessionService platformOwnerSessionService;

        private AuthenticatedSessionHandshakeInterceptor(AuthSessionService authSessionService,
                                                         PlatformOwnerSessionService platformOwnerSessionService) {
            this.authSessionService = authSessionService;
            this.platformOwnerSessionService = platformOwnerSessionService;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       org.springframework.web.socket.WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
            if (!(request instanceof ServletServerHttpRequest servletRequest)) {
                log.warn("Realtime handshake rejected because the request was not servlet-backed: {}", request.getURI());
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            var session = servletRequest.getServletRequest().getSession(false);
            if (session == null) {
                log.warn("Realtime handshake rejected because no authenticated HTTP session was available for {}", request.getURI());
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            if (platformOwnerSessionService.hasAuthenticatedSession(session)) {
                attributes.put(SESSION_AUTHORITY_ATTRIBUTE, PLATFORM_AUTHORITY);
                attributes.put(SESSION_TENANT_CODE_ATTRIBUTE, "");
                attributes.put(SESSION_ROLES_ATTRIBUTE, Set.of());
                attributes.put(SESSION_TENANT_WIDE_ATTRIBUTE, false);
                attributes.put(SESSION_HTTP_SESSION_ATTRIBUTE, session);
                return true;
            }

            if (!authSessionService.hasSessionIdentity(session)) {
                log.warn("Realtime handshake rejected because no authenticated tenant or platform session was available for {}", request.getURI());
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            return authSessionService.resolveAuthenticatedSession(session)
                .map(authenticatedSession -> {
                    log.info("Realtime handshake accepted for tenant {} from origin {}",
                        authenticatedSession.tenant().getCode(),
                        servletRequest.getHeaders().getOrigin());
                    attributes.put(
                        SESSION_AUTHORITY_ATTRIBUTE,
                        TENANT_AUTHORITY
                    );
                    attributes.put(
                        SESSION_TENANT_CODE_ATTRIBUTE,
                        authenticatedSession.tenant().getCode().trim().toUpperCase(Locale.ROOT)
                    );
                    attributes.put(
                        SESSION_ROLES_ATTRIBUTE,
                        authenticatedSession.operator().getRoles().stream()
                            .map(Enum::name)
                            .collect(Collectors.toUnmodifiableSet())
                    );
                    attributes.put(
                        SESSION_TENANT_WIDE_ATTRIBUTE,
                        authenticatedSession.operator().getWarehouseScopes().isEmpty()
                    );
                    attributes.put(SESSION_HTTP_SESSION_ATTRIBUTE, session);
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("Realtime handshake rejected because the authenticated session could not be resolved for {}", request.getURI());
                    response.setStatusCode(HttpStatus.FORBIDDEN);
                    return false;
                });
        }

        @Override
        public void afterHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                   Exception exception) {
            if (exception != null) {
                log.warn("Realtime handshake completed with exception for {}: {}", request.getURI(), exception.getMessage());
            }
        }
    }

    static final class TenantSubscriptionChannelInterceptor implements ChannelInterceptor {

        private static final String PLATFORM_ACTIVITY_CHANGED_DESTINATION = "/TOPIC/PLATFORM/ACTIVITY.CHANGED";

        private static final Set<String> TENANT_WIDE_RAW_SUFFIXES = Set.of(
            "/INVENTORY",
            "/FULFILLMENT.OVERVIEW",
            "/ORDERS.RECENT",
            "/ALERTS",
            "/RECOMMENDATIONS",
            "/SCENARIOS.NOTIFICATIONS",
            "/SCENARIOS.ESCALATED",
            "/EVENTS.RECENT",
            "/AUDIT.RECENT",
            "/SYSTEM.INCIDENTS",
            "/INTEGRATIONS.CONNECTORS",
            "/INTEGRATIONS.IMPORTS",
            "/INTEGRATIONS.REPLAY"
        );

        private static final Set<String> KNOWN_TENANT_DESTINATION_SUFFIXES = Set.of(
            "/DASHBOARD.SUMMARY",
            "/ALERTS",
            "/ALERTS.CHANGED",
            "/RECOMMENDATIONS",
            "/RECOMMENDATIONS.CHANGED",
            "/INVENTORY",
            "/FULFILLMENT.OVERVIEW",
            "/ORDERS.RECENT",
            "/EVENTS.RECENT",
            "/AUDIT.RECENT",
            "/SYSTEM.INCIDENTS",
            "/INTEGRATIONS.CONNECTORS",
            "/INTEGRATIONS.IMPORTS",
            "/INTEGRATIONS.REPLAY",
            "/INTEGRATIONS.CHANGED",
            "/SCENARIOS.NOTIFICATIONS",
            "/SCENARIOS.ESCALATED"
        );

        private final AuthSessionService authSessionService;
        private final PlatformOwnerSessionService platformOwnerSessionService;
        private final RealtimeSessionRegistry realtimeSessionRegistry;

        TenantSubscriptionChannelInterceptor() {
            this(null, null, null);
        }

        TenantSubscriptionChannelInterceptor(AuthSessionService authSessionService) {
            this(authSessionService, null, null);
        }

        TenantSubscriptionChannelInterceptor(AuthSessionService authSessionService,
                                             RealtimeSessionRegistry realtimeSessionRegistry) {
            this(authSessionService, null, realtimeSessionRegistry);
        }

        TenantSubscriptionChannelInterceptor(AuthSessionService authSessionService,
                                             PlatformOwnerSessionService platformOwnerSessionService,
                                             RealtimeSessionRegistry realtimeSessionRegistry) {
            this.authSessionService = authSessionService;
            this.platformOwnerSessionService = platformOwnerSessionService;
            this.realtimeSessionRegistry = realtimeSessionRegistry;
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null || accessor.getCommand() == null) {
                return message;
            }

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                bindSession(accessor);
                return message;
            }

            if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                if (realtimeSessionRegistry != null) {
                    realtimeSessionRegistry.remove(accessor.getSessionId());
                }
                return message;
            }

            if (StompCommand.SEND.equals(accessor.getCommand())) {
                throw new IllegalArgumentException("Client realtime SEND is not supported.");
            }

            if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && !StompCommand.SEND.equals(accessor.getCommand())
                && !StompCommand.MESSAGE.equals(accessor.getCommand())) {
                return message;
            }

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                sessionAttributes = new java.util.HashMap<>();
                accessor.setSessionAttributes(sessionAttributes);
            }
            if (!refreshCurrentAuthority(accessor, sessionAttributes)) {
                if (StompCommand.MESSAGE.equals(accessor.getCommand())) {
                    return null;
                }
                throw new IllegalArgumentException("A current signed-in tenant session is required for realtime use.");
            }
            String tenantCode = sessionAttributes == null
                ? null
                : (String) sessionAttributes.get(SESSION_TENANT_CODE_ATTRIBUTE);

            if (tenantCode == null || tenantCode.isBlank()) {
                if (PLATFORM_AUTHORITY.equals(readAuthority(sessionAttributes))) {
                    if (!isPlatformDestination(accessor.getDestination())) {
                        return rejectOrDrop(
                            accessor.getCommand(),
                            "Platform-owner realtime is limited to the supported platform topic."
                        );
                    }
                    return message;
                }
                log.warn("Realtime subscription rejected because no signed-in tenant was attached to the STOMP session.");
                throw new IllegalArgumentException("A signed-in tenant session is required for realtime subscriptions.");
            }

            String destination = accessor.getDestination();
            if (destination == null || destination.isBlank()) {
                return rejectOrDrop(accessor.getCommand(), "A realtime destination is required.");
            }

            if (isPlatformDestination(destination)) {
                return rejectOrDrop(
                    accessor.getCommand(),
                    "Platform-owner realtime is limited to a dedicated platform session."
                );
            }

            String expectedPrefix = "/topic/tenant/" + tenantCode.toUpperCase(Locale.ROOT) + "/";
            String normalizedDestination = destination.trim().toUpperCase(Locale.ROOT);
            if (normalizedDestination.startsWith("/TOPIC/TENANT/")
                && !normalizedDestination.startsWith(expectedPrefix.toUpperCase(Locale.ROOT))) {
                log.warn("Realtime subscription rejected for tenant {} because destination {} escaped the tenant scope.",
                    tenantCode,
                    destination);
                return rejectOrDrop(
                    accessor.getCommand(),
                    "Realtime subscriptions are limited to the signed-in tenant context."
                );
            }

            if (!normalizedDestination.startsWith(expectedPrefix.toUpperCase(Locale.ROOT))) {
                return rejectOrDrop(
                    accessor.getCommand(),
                    "Realtime destinations are limited to supported tenant topics."
                );
            }

            String destinationSuffix = normalizedDestination.substring(expectedPrefix.length());
            if (!KNOWN_TENANT_DESTINATION_SUFFIXES.contains("/" + destinationSuffix)) {
                log.warn("Realtime destination rejected for tenant {} because topic {} is not supported.",
                    tenantCode,
                    destination);
                return rejectOrDrop(
                    accessor.getCommand(),
                    "Realtime destinations are limited to supported tenant topics."
                );
            }
            Set<String> roles = readRoles(sessionAttributes);
            boolean integrationRole = roles.contains(SynapseAccessRole.INTEGRATION_ADMIN.name())
                || roles.contains(SynapseAccessRole.INTEGRATION_OPERATOR.name());
            if (destinationSuffix.startsWith("INTEGRATIONS.") && !integrationRole) {
                log.warn("Realtime integration subscription rejected for tenant {} because the session lacks an integration role.",
                    tenantCode);
                return rejectOrDrop(
                    accessor.getCommand(),
                    "An integration role is required for integration realtime subscriptions."
                );
            }

            boolean tenantWide = Boolean.TRUE.equals(sessionAttributes.get(SESSION_TENANT_WIDE_ATTRIBUTE));
            String normalizedSuffix = "/" + destinationSuffix;
            if (TENANT_WIDE_RAW_SUFFIXES.contains(normalizedSuffix) && !tenantWide) {
                log.warn("Realtime raw subscription rejected for tenant {} because the session is warehouse-scoped.", tenantCode);
                return rejectOrDrop(
                    accessor.getCommand(),
                    "Warehouse-scoped sessions must refresh filtered operational data through tenant APIs."
                );
            }

            return message;
        }

        private Message<?> rejectOrDrop(StompCommand command, String reason) {
            if (StompCommand.MESSAGE.equals(command)) {
                return null;
            }
            throw new IllegalArgumentException(reason);
        }

        private void bindSession(StompHeaderAccessor accessor) {
            if (realtimeSessionRegistry == null) {
                return;
            }
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                return;
            }
            Object session = sessionAttributes.get(SESSION_HTTP_SESSION_ATTRIBUTE);
            if (session instanceof HttpSession httpSession) {
                realtimeSessionRegistry.bind(accessor.getSessionId(), httpSession);
            }
        }

        private boolean refreshCurrentAuthority(StompHeaderAccessor accessor,
                                                Map<String, Object> sessionAttributes) {
            if (authSessionService == null && platformOwnerSessionService == null) {
                return true;
            }
            Object rawSession = sessionAttributes.get(SESSION_HTTP_SESSION_ATTRIBUTE);
            HttpSession httpSession = rawSession instanceof HttpSession session
                ? session
                : realtimeSessionRegistry == null ? null : realtimeSessionRegistry.get(accessor.getSessionId());
            if (httpSession == null) {
                return false;
            }

            String authority = readAuthority(sessionAttributes);
            if (PLATFORM_AUTHORITY.equals(authority)) {
                return platformOwnerSessionService != null
                    && platformOwnerSessionService.hasAuthenticatedSession(httpSession);
            }

            if (authSessionService == null) {
                return false;
            }

            var authenticatedSession = authSessionService.resolveAuthenticatedSession(httpSession).orElse(null);
            if (authenticatedSession == null) {
                return false;
            }

            sessionAttributes.put(SESSION_AUTHORITY_ATTRIBUTE, TENANT_AUTHORITY);
            sessionAttributes.put(
                SESSION_TENANT_CODE_ATTRIBUTE,
                authenticatedSession.tenant().getCode().trim().toUpperCase(Locale.ROOT)
            );
            sessionAttributes.put(
                SESSION_ROLES_ATTRIBUTE,
                authenticatedSession.operator().getRoles().stream()
                    .map(Enum::name)
                    .collect(Collectors.toUnmodifiableSet())
            );
            sessionAttributes.put(
                SESSION_TENANT_WIDE_ATTRIBUTE,
                authenticatedSession.operator().getWarehouseScopes().isEmpty()
            );
            return true;
        }

        private String readAuthority(Map<String, Object> sessionAttributes) {
            Object value = sessionAttributes.get(SESSION_AUTHORITY_ATTRIBUTE);
            return value instanceof String authority ? authority.trim().toUpperCase(Locale.ROOT) : "";
        }

        private boolean isPlatformDestination(String destination) {
            return destination != null
                && PLATFORM_ACTIVITY_CHANGED_DESTINATION.equals(destination.trim().toUpperCase(Locale.ROOT));
        }

        private Set<String> readRoles(Map<String, Object> sessionAttributes) {
            Object value = sessionAttributes.get(SESSION_ROLES_ATTRIBUTE);
            if (!(value instanceof Set<?> values)) {
                return Set.of();
            }
            return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        }
    }

    private static final class RealtimeSessionRegistry {

        private final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();

        private void bind(String realtimeSessionId, HttpSession httpSession) {
            if (realtimeSessionId != null && httpSession != null) {
                sessions.put(realtimeSessionId, httpSession);
            }
        }

        private HttpSession get(String realtimeSessionId) {
            return realtimeSessionId == null ? null : sessions.get(realtimeSessionId);
        }

        private void remove(String realtimeSessionId) {
            if (realtimeSessionId != null) {
                sessions.remove(realtimeSessionId);
            }
        }
    }
}
