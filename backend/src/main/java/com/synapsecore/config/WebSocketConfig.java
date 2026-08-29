package com.synapsecore.config;

import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.access.SynapseAccessRole;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

    private static final String SESSION_TENANT_CODE_ATTRIBUTE = "synapsecoreTenantCode";
    static final String SESSION_ROLES_ATTRIBUTE = "synapsecoreRoles";
    static final String SESSION_TENANT_WIDE_ATTRIBUTE = "synapsecoreTenantWide";
    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final List<String> allowedOrigins;
    private final SynapseAccessProperties accessProperties;
    private final AuthSessionService authSessionService;
    private final SynapseRealtimeProperties realtimeProperties;

    public WebSocketConfig(SynapseCorsProperties corsProperties,
                           SynapseAccessProperties accessProperties,
                           AuthSessionService authSessionService,
                           SynapseRealtimeProperties realtimeProperties) {
        this.allowedOrigins = corsProperties.getAllowedOrigins();
        this.accessProperties = accessProperties;
        this.authSessionService = authSessionService;
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
            endpoint.addInterceptors(new AuthenticatedTenantHandshakeInterceptor(authSessionService));
        }

        var sockJsRegistration = endpoint.withSockJS();
        if (!accessProperties.isAllowHeaderFallback()) {
            sockJsRegistration.setSessionCookieNeeded(true);
        }
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        if (!accessProperties.isAllowHeaderFallback()) {
            registration.interceptors(new TenantSubscriptionChannelInterceptor());
        }
    }

    private static final class AuthenticatedTenantHandshakeInterceptor implements HandshakeInterceptor {

        private final AuthSessionService authSessionService;

        private AuthenticatedTenantHandshakeInterceptor(AuthSessionService authSessionService) {
            this.authSessionService = authSessionService;
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
            if (session == null || !authSessionService.hasSessionIdentity(session)) {
                log.warn("Realtime handshake rejected because no authenticated HTTP session was available for {}", request.getURI());
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            return authSessionService.resolveAuthenticatedSession(session)
                .map(authenticatedSession -> {
                    log.info("Realtime handshake accepted for tenant {} from origin {}",
                        authenticatedSession.tenant().getCode(),
                        servletRequest.getHeaders().getOrigin());
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

        private static final Set<String> TENANT_WIDE_RAW_SUFFIXES = Set.of(
            "/INVENTORY",
            "/FULFILLMENT.OVERVIEW",
            "/ORDERS.RECENT",
            "/ALERTS",
            "/SCENARIOS.NOTIFICATIONS",
            "/SCENARIOS.ESCALATED",
            "/INTEGRATIONS.CONNECTORS",
            "/INTEGRATIONS.IMPORTS",
            "/INTEGRATIONS.REPLAY"
        );

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null || accessor.getCommand() == null) {
                return message;
            }

            if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                return message;
            }

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            String tenantCode = sessionAttributes == null
                ? null
                : (String) sessionAttributes.get(SESSION_TENANT_CODE_ATTRIBUTE);

            if (tenantCode == null || tenantCode.isBlank()) {
                log.warn("Realtime subscription rejected because no signed-in tenant was attached to the STOMP session.");
                throw new IllegalArgumentException("A signed-in tenant session is required for realtime subscriptions.");
            }

            String destination = accessor.getDestination();
            if (destination == null || destination.isBlank()) {
                return message;
            }

            String expectedPrefix = "/topic/tenant/" + tenantCode.toUpperCase(Locale.ROOT) + "/";
            String normalizedDestination = destination.trim().toUpperCase(Locale.ROOT);
            if (normalizedDestination.startsWith("/TOPIC/TENANT/")
                && !normalizedDestination.startsWith(expectedPrefix.toUpperCase(Locale.ROOT))) {
                log.warn("Realtime subscription rejected for tenant {} because destination {} escaped the tenant scope.",
                    tenantCode,
                    destination);
                throw new IllegalArgumentException(
                    "Realtime subscriptions are limited to the signed-in tenant context."
                );
            }

            if (!normalizedDestination.startsWith(expectedPrefix.toUpperCase(Locale.ROOT))) {
                return message;
            }

            String destinationSuffix = normalizedDestination.substring(expectedPrefix.length());
            Set<String> roles = readRoles(sessionAttributes);
            boolean integrationRole = roles.contains(SynapseAccessRole.INTEGRATION_ADMIN.name())
                || roles.contains(SynapseAccessRole.INTEGRATION_OPERATOR.name());
            if (destinationSuffix.startsWith("INTEGRATIONS.") && !integrationRole) {
                log.warn("Realtime integration subscription rejected for tenant {} because the session lacks an integration role.",
                    tenantCode);
                throw new IllegalArgumentException("An integration role is required for integration realtime subscriptions.");
            }

            boolean tenantWide = Boolean.TRUE.equals(sessionAttributes.get(SESSION_TENANT_WIDE_ATTRIBUTE));
            String normalizedSuffix = "/" + destinationSuffix;
            if (TENANT_WIDE_RAW_SUFFIXES.contains(normalizedSuffix) && !tenantWide) {
                log.warn("Realtime raw subscription rejected for tenant {} because the session is warehouse-scoped.", tenantCode);
                throw new IllegalArgumentException(
                    "Warehouse-scoped sessions must refresh filtered operational data through tenant APIs."
                );
            }

            return message;
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
}
