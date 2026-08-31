package com.synapsecore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.platform.PlatformOwnerSessionService;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class WebSocketAccessBoundaryTest {

    private final WebSocketConfig.TenantSubscriptionChannelInterceptor interceptor =
        new WebSocketConfig.TenantSubscriptionChannelInterceptor();

    @Test
    void rejectsCrossTenantSubscription() {
        assertThatThrownBy(() -> subscribe(
            "/topic/tenant/OTHER/inventory",
            Set.of("INTEGRATION_OPERATOR"),
            true
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsIntegrationTopicsForGovernanceRoles() {
        assertThatThrownBy(() -> subscribe(
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/integrations.replay",
            Set.of("REVIEW_OWNER"),
            true
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTenantWideRawTopicsForWarehouseScopedSessions() {
        assertThatThrownBy(() -> subscribe(
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/orders.recent",
            Set.of("INTEGRATION_OPERATOR"),
            false
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subscribe(
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/alerts",
            Set.of("INTEGRATION_OPERATOR"),
            false
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subscribe(
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/integrations.connectors",
            Set.of("INTEGRATION_OPERATOR"),
            false
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subscribe(
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/events.recent",
            Set.of("REVIEW_OWNER"),
            false
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subscribe(
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/audit.recent",
            Set.of("REVIEW_OWNER"),
            false
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subscribe(
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/system.incidents",
            Set.of("REVIEW_OWNER"),
            false
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsSafeIntegrationChangeSignalForScopedIntegrationRole() {
        assertThatCode(() -> subscribe(
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/integrations.changed",
            Set.of("INTEGRATION_OPERATOR"),
            false
        )).doesNotThrowAnyException();
    }

    @Test
    void allowsRawIntegrationTopicsForTenantWideIntegrationRole() {
        assertThatCode(() -> subscribe(
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/integrations.replay",
            Set.of("INTEGRATION_ADMIN"),
            true
        )).doesNotThrowAnyException();
    }

    @Test
    void revalidatesCurrentHttpAuthorityBeforeRealtimeUse() {
        HttpSession httpSession = new org.springframework.mock.web.MockHttpSession();
        Tenant tenant = Tenant.builder().code("ACCESS-BOUNDARY-REHEARSAL").build();
        AccessOperator operator = AccessOperator.builder()
            .tenant(tenant)
            .actorName("boundary.integration.operator")
            .displayName("Boundary Integration Operator")
            .roles(Set.of(SynapseAccessRole.INTEGRATION_OPERATOR))
            .warehouseScopes(Set.of("WH-NORTH"))
            .active(true)
            .build();
        AuthSessionService authSessionService = new StubAuthSessionService(Optional.of(
            new AuthSessionService.AuthenticatedSession(
                null,
                operator,
                tenant,
                Instant.now().minusSeconds(10),
                Instant.now().plusSeconds(300),
                Instant.now().plusSeconds(300),
                false,
                false
            )
        ));

        WebSocketConfig.TenantSubscriptionChannelInterceptor currentInterceptor =
            new WebSocketConfig.TenantSubscriptionChannelInterceptor(authSessionService);
        assertThatThrownBy(() -> subscribe(
            currentInterceptor,
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/orders.recent",
            Set.of("INTEGRATION_OPERATOR"),
            true,
            httpSession
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsRealtimeUseAfterHttpSessionAuthorityIsRevoked() {
        HttpSession httpSession = new org.springframework.mock.web.MockHttpSession();
        AuthSessionService authSessionService = new StubAuthSessionService(Optional.empty());
        WebSocketConfig.TenantSubscriptionChannelInterceptor currentInterceptor =
            new WebSocketConfig.TenantSubscriptionChannelInterceptor(authSessionService);

        assertThatThrownBy(() -> subscribe(
            currentInterceptor,
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/integrations.changed",
            Set.of("INTEGRATION_OPERATOR"),
            false,
            httpSession
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsClientSendForKnownAndUnknownDestinations() {
        assertThatThrownBy(() -> message(
            StompCommand.SEND,
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/dashboard.summary",
            Set.of("TENANT_ADMIN"),
            true
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Client realtime SEND is not supported.");
        assertThatThrownBy(() -> message(
            StompCommand.SEND,
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/orders.recent",
            Set.of("TENANT_ADMIN"),
            true
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> message(
            StompCommand.SEND,
            "/topic/tenant/OTHER/alerts",
            Set.of("TENANT_ADMIN"),
            true
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> message(
            StompCommand.SEND,
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/unknown.topic",
            Set.of("TENANT_ADMIN"),
            true
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownAndNonTenantSubscriptions() {
        assertThatThrownBy(() -> subscribe("/topic/random", Set.of("TENANT_ADMIN"), true))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subscribe("/topic/platform/anything", Set.of("TENANT_ADMIN"), true))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subscribe("/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/unknown.topic", Set.of("TENANT_ADMIN"), true))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subscribe("/app/anything", Set.of("TENANT_ADMIN"), true))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsOnlyPlatformOwnerSubscriptionToPlatformActivitySignal() {
        HttpSession platformHttpSession = new org.springframework.mock.web.MockHttpSession();
        PlatformOwnerSessionService platformOwnerSessionService = new StubPlatformOwnerSessionService(true);
        WebSocketConfig.TenantSubscriptionChannelInterceptor currentInterceptor =
            new WebSocketConfig.TenantSubscriptionChannelInterceptor(null, platformOwnerSessionService, null);

        assertThat(platformMessage(currentInterceptor, StompCommand.SUBSCRIBE,
            "/topic/platform/activity.changed", platformHttpSession)).isNotNull();
        assertThatThrownBy(() -> subscribe(
            currentInterceptor,
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/events.recent",
            Set.of("TENANT_ADMIN"),
            true,
            platformHttpSession
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTenantAndAnonymousPlatformSubscriptions() {
        assertThatThrownBy(() -> subscribe(
            "/topic/platform/activity.changed",
            Set.of("TENANT_ADMIN"),
            true
        )).isInstanceOf(IllegalArgumentException.class);

        HttpSession anonymousSession = new org.springframework.mock.web.MockHttpSession();
        WebSocketConfig.TenantSubscriptionChannelInterceptor currentInterceptor =
            new WebSocketConfig.TenantSubscriptionChannelInterceptor(new StubAuthSessionService(Optional.empty()),
                new StubPlatformOwnerSessionService(false), null);
        assertThatThrownBy(() -> platformMessage(currentInterceptor, StompCommand.SUBSCRIBE,
            "/topic/platform/activity.changed", anonymousSession))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPlatformSubscriptionAfterPlatformSessionIsInvalidated() {
        HttpSession platformHttpSession = new org.springframework.mock.web.MockHttpSession();
        WebSocketConfig.TenantSubscriptionChannelInterceptor currentInterceptor =
            new WebSocketConfig.TenantSubscriptionChannelInterceptor(null, new StubPlatformOwnerSessionService(false), null);

        assertThatThrownBy(() -> platformMessage(currentInterceptor, StompCommand.SUBSCRIBE,
            "/topic/platform/activity.changed", platformHttpSession))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void suppressesUnauthorizedOutboundRecipientWithoutBlockingAuthorizedRecipient() {
        Message<?> authorized = message(
            StompCommand.MESSAGE,
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/alerts",
            Set.of("TENANT_ADMIN"),
            true
        );
        Message<?> scoped = message(
            StompCommand.MESSAGE,
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/alerts",
            Set.of("INTEGRATION_OPERATOR"),
            false
        );

        assertThat(authorized).isNotNull();
        assertThat(scoped).isNull();
    }

    private Message<?> subscribe(String destination, Set<String> roles, boolean tenantWide) {
        return message(interceptor, StompCommand.SUBSCRIBE, destination, roles, tenantWide, null);
    }

    private Message<?> subscribe(WebSocketConfig.TenantSubscriptionChannelInterceptor target,
                                 String destination,
                                 Set<String> roles,
                                 boolean tenantWide,
                                 HttpSession httpSession) {
        return message(target, StompCommand.SUBSCRIBE, destination, roles, tenantWide, httpSession);
    }

    private Message<?> message(StompCommand command,
                               String destination,
                               Set<String> roles,
                               boolean tenantWide) {
        return message(interceptor, command, destination, roles, tenantWide, null);
    }

    private Message<?> message(WebSocketConfig.TenantSubscriptionChannelInterceptor target,
                               StompCommand command,
                               String destination,
                               Set<String> roles,
                               boolean tenantWide,
                               HttpSession httpSession) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        Map<String, Object> attributes = new java.util.HashMap<>(Map.of(
            "synapsecoreTenantCode", "ACCESS-BOUNDARY-REHEARSAL",
            WebSocketConfig.SESSION_AUTHORITY_ATTRIBUTE, WebSocketConfig.TENANT_AUTHORITY,
            WebSocketConfig.SESSION_ROLES_ATTRIBUTE, roles,
            WebSocketConfig.SESSION_TENANT_WIDE_ATTRIBUTE, tenantWide
        ));
        if (httpSession != null) {
            attributes.put(WebSocketConfig.SESSION_HTTP_SESSION_ATTRIBUTE, httpSession);
        }
        accessor.setSessionAttributes(attributes);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return target.preSend(message, null);
    }

    private Message<?> platformMessage(WebSocketConfig.TenantSubscriptionChannelInterceptor target,
                                       StompCommand command,
                                       String destination,
                                       HttpSession httpSession) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        Map<String, Object> attributes = new java.util.HashMap<>(Map.of(
            WebSocketConfig.SESSION_AUTHORITY_ATTRIBUTE, WebSocketConfig.PLATFORM_AUTHORITY,
            WebSocketConfig.SESSION_TENANT_CODE_ATTRIBUTE, "",
            WebSocketConfig.SESSION_ROLES_ATTRIBUTE, Set.of(),
            WebSocketConfig.SESSION_TENANT_WIDE_ATTRIBUTE, false,
            WebSocketConfig.SESSION_HTTP_SESSION_ATTRIBUTE, httpSession
        ));
        accessor.setSessionAttributes(attributes);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return target.preSend(message, null);
    }

    private static final class StubAuthSessionService extends AuthSessionService {

        private final Optional<AuthenticatedSession> result;

        private StubAuthSessionService(Optional<AuthenticatedSession> result) {
            super(null, null, null, null, null, null, null);
            this.result = result;
        }

        @Override
        public Optional<AuthenticatedSession> resolveAuthenticatedSession(HttpSession session) {
            return result;
        }
    }

    private static final class StubPlatformOwnerSessionService extends PlatformOwnerSessionService {

        private final boolean authenticated;

        private StubPlatformOwnerSessionService(boolean authenticated) {
            super(null, null, null);
            this.authenticated = authenticated;
        }

        @Override
        public boolean hasAuthenticatedSession(HttpSession session) {
            return authenticated;
        }
    }
}
