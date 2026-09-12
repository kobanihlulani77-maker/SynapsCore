package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.config.AsyncExecutionConfig;
import com.synapsecore.config.SchedulingConfig;
import com.synapsecore.decision.RecommendationReconciliationService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class BackgroundConcurrencyConfigTest {

    @Test
    void scheduledWorkUsesOneSharedWorkerByDefault() {
        SchedulingConfig configuration = new SchedulingConfig();
        ThreadPoolTaskScheduler scheduler = configuration.synapseScheduledTaskScheduler();

        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    void recommendationReconciliationUsesItsOwnBoundedScheduler() throws NoSuchMethodException {
        SchedulingConfig configuration = new SchedulingConfig();
        ThreadPoolTaskScheduler scheduler = configuration.synapseRecommendationTaskScheduler();

        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
            Scheduled scheduled = RecommendationReconciliationService.class
                .getMethod("reconcileOnSchedule")
                .getAnnotation(Scheduled.class);
            assertThat(scheduled.scheduler()).isEqualTo("synapseRecommendationTaskScheduler");
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    void scheduledMethodQualifiersRouteWorkToTheConfiguredExecutors() throws InterruptedException {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(SchedulingConfig.class, SchedulerRoutingProbe.class);
            context.refresh();

            SchedulerRoutingProbe probe = context.getBean(SchedulerRoutingProbe.class);
            assertThat(probe.awaitInvocations()).isTrue();
            assertThat(probe.defaultThread()).startsWith("SynapseScheduled-");
            assertThat(probe.recommendationThread()).startsWith("SynapseRecommendationScheduled-");
        }
    }

    @Test
    void operationalDispatchUsesASeparateBoundedWorkerBudgetByDefault() {
        AsyncExecutionConfig configuration = new AsyncExecutionConfig();
        Executor executor = configuration.operationalDispatchExecutor();
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;

        try {
            assertThat(taskExecutor.getCorePoolSize()).isEqualTo(1);
            assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(2);
        } finally {
            taskExecutor.shutdown();
        }
    }

    static class SchedulerRoutingProbe {
        private final CountDownLatch invocations = new CountDownLatch(2);
        private final AtomicReference<String> defaultThread = new AtomicReference<>();
        private final AtomicReference<String> recommendationThread = new AtomicReference<>();

        @Scheduled(initialDelay = 0, fixedDelay = 3_600_000)
        public void runOnDefaultScheduler() {
            defaultThread.compareAndSet(null, Thread.currentThread().getName());
            invocations.countDown();
        }

        @Scheduled(
            scheduler = "synapseRecommendationTaskScheduler",
            initialDelay = 0,
            fixedDelay = 3_600_000
        )
        public void runOnRecommendationScheduler() {
            recommendationThread.compareAndSet(null, Thread.currentThread().getName());
            invocations.countDown();
        }

        boolean awaitInvocations() throws InterruptedException {
            return invocations.await(5, TimeUnit.SECONDS);
        }

        String defaultThread() {
            return defaultThread.get();
        }

        String recommendationThread() {
            return recommendationThread.get();
        }
    }
}
