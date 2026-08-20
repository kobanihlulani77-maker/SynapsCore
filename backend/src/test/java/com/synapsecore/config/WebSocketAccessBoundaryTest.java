package com.synapsecore.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
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
            "/topic/tenant/ACCESS-BOUNDARY-REHEARSAL/integrations.connectors",
            Set.of("INTEGRATION_OPERATOR"),
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

    private Message<?> subscribe(String destination, Set<String> roles, boolean tenantWide) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionAttributes(Map.of(
            "synapsecoreTenantCode", "ACCESS-BOUNDARY-REHEARSAL",
            WebSocketConfig.SESSION_ROLES_ATTRIBUTE, roles,
            WebSocketConfig.SESSION_TENANT_WIDE_ATTRIBUTE, tenantWide
        ));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return interceptor.preSend(message, null);
    }
}
