package com.synapsecore.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.access.SynapseActorContext;
import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.domain.dto.AlertFeedResponse;
import com.synapsecore.domain.dto.DashboardSnapshotResponse;
import com.synapsecore.domain.dto.FulfillmentOverviewResponse;
import com.synapsecore.domain.service.OperationalViewService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DashboardSnapshotAuthorityConsistencyTest {

    private static final SynapseActorContext TENANT_WIDE =
        new SynapseActorContext("Operations Lead", Set.of(SynapseAccessRole.TENANT_ADMIN), List.of());
    private static final SynapseActorContext NORTH_ONLY =
        new SynapseActorContext("Operations Lead", Set.of(SynapseAccessRole.TENANT_ADMIN), List.of("WH-NORTH"));

    @Test
    void rejectsSnapshotWhenWarehouseAuthorityChangesDuringComposition() {
        DashboardController controller = controller(TENANT_WIDE, NORTH_ONLY);

        assertThatThrownBy(controller::getSnapshot)
            .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void returnsSnapshotWhenAuthorityRemainsCurrent() {
        DashboardController controller = controller(TENANT_WIDE, TENANT_WIDE);

        assertThat(controller.getSnapshot().fulfillment().activeFulfillments()).isEmpty();
    }

    private DashboardController controller(SynapseActorContext initial, SynapseActorContext afterComposition) {
        AtomicInteger checks = new AtomicInteger();
        AccessControlService access = new AccessControlService(null, null, null) {
            @Override
            public SynapseActorContext requireWorkspaceAccess(String actionDescription) {
                return checks.getAndIncrement() == 0 ? initial : afterComposition;
            }
        };
        AuthSessionService auth = new AuthSessionService(null, null, null, null, null, null, null) {
            @Override
            public <T> T withSnapshotIdentity(Supplier<T> read) {
                return read.get();
            }
        };
        OperationalViewService view = new OperationalViewService(
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        ) {
            @Override
            public DashboardSnapshotResponse getSnapshot() {
                return new DashboardSnapshotResponse(
                    null, new AlertFeedResponse(List.of(), List.of()), List.of(), List.of(),
                    new FulfillmentOverviewResponse(0, 0, 0, 0, List.of(), Instant.now()),
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(), Instant.now()
                );
            }
        };
        return new DashboardController(access, auth, null, view);
    }
}
