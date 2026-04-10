package com.synapsecore.config;

import com.synapsecore.auth.AuthSessionService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private final List<String> allowedOrigins;
    private final SynapseAccessProperties accessProperties;
    private final AuthSessionService authSessionService;

    public WebSocketConfig(SynapseCorsProperties corsProperties,
                           SynapseAccessProperties accessProperties,
                           AuthSessionService authSessionService) {
        this.allowedOrigins = corsProperties.getAllowedOrigins();
        this.accessProperties = accessProperties;
        this.authSessionService = authSessionService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var endpoint = registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));

        if (!accessProperties.isAllowHeaderFallback()) {
            endpoint.addInterceptors(new AuthenticatedTenantHandshakeInterceptor(authSessionService));
        }

        endpoint.withSockJS();
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
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            var session = servletRequest.getServletRequest().getSession(false);
            if (session == null || !authSessionService.hasSessionIdentity(session)) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            return authSessionService.resolveAuthenticatedSession(session)
                .map(authenticatedSession -> {
                    attributes.put(
                        SESSION_TENANT_CODE_ATTRIBUTE,
                        authenticatedSession.tenant().getCode().trim().toUpperCase(Locale.ROOT)
                    );
                    return true;
                })
                .orElseGet(() -> {
                    response.setStatusCode(HttpStatus.FORBIDDEN);
                    return false;
                });
        }

        @Override
        public void afterHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                   Exception exception) {
        }
    }

    private static final class TenantSubscriptionChannelInterceptor implements ChannelInterceptor {

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
                throw new IllegalArgumentException(
                    "Realtime subscriptions are limited to the signed-in tenant context."
                );
            }

            return message;
        }
    }
}
