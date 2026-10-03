package com.synapsecore.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OperationalDispatchClaimIntegrationTest {

    @Autowired
    private OperationalDispatchWorkItemRepository repository;

    @Test
    void expiredClaimCanBeRecoveredWithoutAllowingOldAttemptToFinishIt() {
        Instant now = Instant.now();
        OperationalDispatchWorkItem item = repository.saveAndFlush(OperationalDispatchWorkItem.builder()
            .tenantCode("DISPATCH-RECOVERY-" + UUID.randomUUID())
            .updateType(OperationalUpdateType.ORDER_FLOW)
            .source("test")
            .requestId(UUID.randomUUID().toString())
            .status(OperationalDispatchStatus.PENDING)
            .occurredAt(now)
            .build());

        assertThat(repository.claimForDispatch(
            item.getId(), 0, OperationalDispatchStatus.PENDING,
            OperationalDispatchStatus.PROCESSING, now.minusSeconds(300), now.minusSeconds(600)
        )).isEqualTo(1);
        assertThat(repository.claimForDispatch(
            item.getId(), 0, OperationalDispatchStatus.PENDING,
            OperationalDispatchStatus.PROCESSING, now.minusSeconds(300), now
        )).isZero();
        assertThat(repository.findReadyForDispatch(
            OperationalDispatchStatus.PENDING, OperationalDispatchStatus.PROCESSING,
            now.minusSeconds(300), PageRequest.of(0, 100)
        )).extracting(OperationalDispatchWorkItem::getId).contains(item.getId());

        assertThat(repository.claimForDispatch(
            item.getId(), 1, OperationalDispatchStatus.PENDING,
            OperationalDispatchStatus.PROCESSING, now.minusSeconds(300), now
        )).isEqualTo(1);
        assertThat(repository.findReadyForDispatch(
            OperationalDispatchStatus.PENDING, OperationalDispatchStatus.PROCESSING,
            now.minusSeconds(300), PageRequest.of(0, 100)
        )).extracting(OperationalDispatchWorkItem::getId).doesNotContain(item.getId());

        assertThat(repository.completeDispatch(
            item.getId(), 1, OperationalDispatchStatus.PROCESSING,
            OperationalDispatchStatus.COMPLETED, now.plusSeconds(1)
        )).isZero();
        assertThat(repository.failDispatch(
            item.getId(), 1, OperationalDispatchStatus.PROCESSING,
            OperationalDispatchStatus.FAILED, "stale worker", now.plusSeconds(1)
        )).isZero();
        assertThat(repository.completeDispatch(
            item.getId(), 2, OperationalDispatchStatus.PROCESSING,
            OperationalDispatchStatus.COMPLETED, now.plusSeconds(2)
        )).isEqualTo(1);

        OperationalDispatchWorkItem completed = repository.findById(item.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(OperationalDispatchStatus.COMPLETED);
        assertThat(completed.getAttemptCount()).isEqualTo(2);
        assertThat(completed.getProcessedAt()).isNotNull();
        assertThat(completed.getLastError()).isNull();
    }

    @Test
    void simultaneousClaimsHaveOneWinner() throws Exception {
        Instant now = Instant.now();
        OperationalDispatchWorkItem item = repository.saveAndFlush(OperationalDispatchWorkItem.builder()
            .tenantCode("DISPATCH-RACE-" + UUID.randomUUID())
            .updateType(OperationalUpdateType.ORDER_FLOW)
            .source("test")
            .requestId(UUID.randomUUID().toString())
            .status(OperationalDispatchStatus.PENDING)
            .occurredAt(now)
            .build());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> claim = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Dispatch claim barrier timed out");
                }
                return repository.claimForDispatch(
                    item.getId(), 0, OperationalDispatchStatus.PENDING,
                    OperationalDispatchStatus.PROCESSING, now.minusSeconds(300), Instant.now()
                );
            };
            Future<Integer> first = workers.submit(claim);
            Future<Integer> second = workers.submit(claim);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(1, 0);
        }

        OperationalDispatchWorkItem claimed = repository.findById(item.getId()).orElseThrow();
        assertThat(claimed.getStatus()).isEqualTo(OperationalDispatchStatus.PROCESSING);
        assertThat(claimed.getAttemptCount()).isEqualTo(1);
    }
}
