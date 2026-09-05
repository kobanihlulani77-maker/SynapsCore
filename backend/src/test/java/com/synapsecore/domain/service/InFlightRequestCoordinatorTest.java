package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InFlightRequestCoordinatorTest {

    @Test
    void coalescesConcurrentRequestsForTheSameKey() throws Exception {
        InFlightRequestCoordinator<String> coordinator = new InFlightRequestCoordinator<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch supplierStarted = new CountDownLatch(1);
        CountDownLatch releaseSupplier = new CountDownLatch(1);
        AtomicInteger supplierCalls = new AtomicInteger();
        Thread releaser = null;

        try {
            Future<String> first = executor.submit(() -> coordinator.execute("tenant|actor", () -> {
                supplierCalls.incrementAndGet();
                supplierStarted.countDown();
                try {
                    assertThat(releaseSupplier.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                return "snapshot";
            }));
            assertThat(supplierStarted.await(5, TimeUnit.SECONDS)).isTrue();
            releaser = new Thread(() -> {
                try {
                    Thread.sleep(100);
                    releaseSupplier.countDown();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            releaser.start();

            assertThat(coordinator.execute("tenant|actor", () -> {
                supplierCalls.incrementAndGet();
                return "unexpected";
            })).isEqualTo("snapshot");
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("snapshot");
            assertThat(supplierCalls).hasValue(1);
        } finally {
            if (releaser != null) {
                releaser.join(5_000);
            }
            executor.shutdownNow();
        }
    }

    @Test
    void removesFailedRequestsSoTheNextAttemptCanRetry() {
        InFlightRequestCoordinator<String> coordinator = new InFlightRequestCoordinator<>();

        assertThatThrownBy(() -> coordinator.execute("tenant|actor", () -> {
            throw new IllegalStateException("first attempt failed");
        }))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("first attempt failed");

        assertThat(coordinator.execute("tenant|actor", () -> "retry")).isEqualTo("retry");
    }
}
