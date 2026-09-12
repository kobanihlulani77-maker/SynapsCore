package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.config.SchedulingConfig;
import java.util.Map;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class ProductWriteWatchdogWiringTest {

    @Test
    void twoProductionSchedulersStillArmTheProductWatchdogOnTheMainScheduler() {
        try (var context = context(true, "PostgreSQL")) {
            assertThat(context.getBeansOfType(TaskScheduler.class)).hasSize(2);
            var main = context.getBean("synapseScheduledTaskScheduler", ThreadPoolTaskScheduler.class);
            var recommendations = context.getBean("synapseRecommendationTaskScheduler", ThreadPoolTaskScheduler.class);
            var diagnostics = context.getBean(ProductWriteContentionDiagnostics.class);
            try (var watch = diagnostics.begin("watchdog-proof", "SYNTHETIC", System.nanoTime())) {
                assertThat(watch).isNotSameAs(ProductWriteContentionDiagnostics.ProductWriteWatch.NO_OP);
                assertThat(main.getScheduledThreadPoolExecutor().getQueue()).hasSize(1);
                assertThat(recommendations.getScheduledThreadPoolExecutor().getQueue()).isEmpty();
                Future<?> task = (Future<?>) main.getScheduledThreadPoolExecutor().getQueue().peek();
                watch.close();
                assertThat(task.isCancelled()).isTrue();
            }
        }
    }

    @Test
    void disabledSchedulingRemainsOptional() {
        try (var context = context(false, "PostgreSQL")) {
            assertThat(context.getBeansOfType(TaskScheduler.class)).isEmpty();
            assertThat(context.getBean(ProductWriteContentionDiagnostics.class)
                .begin("watchdog-proof", "SYNTHETIC", System.nanoTime()))
                .isSameAs(ProductWriteContentionDiagnostics.ProductWriteWatch.NO_OP);
        }
    }

    @Test
    void nonPostgresDoesNotArmTheWatchdog() {
        try (var context = context(true, "H2")) {
            assertThat(context.getBean(ProductWriteContentionDiagnostics.class)
                .begin("watchdog-proof", "SYNTHETIC", System.nanoTime()))
                .isSameAs(ProductWriteContentionDiagnostics.ProductWriteWatch.NO_OP);
            context.getBeansOfType(ThreadPoolTaskScheduler.class).values().forEach(scheduler ->
                assertThat(scheduler.getScheduledThreadPoolExecutor().getQueue()).isEmpty());
        }
    }

    private AnnotationConfigApplicationContext context(boolean enabled, String database) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("watchdog-proof",
            Map.of("spring.task.scheduling.enabled", Boolean.toString(enabled))));
        context.registerBean(JdbcTemplate.class, () -> new MetadataOnlyJdbc(database));
        context.register(SchedulingConfig.class, ProductWriteContentionDiagnostics.class);
        context.refresh();
        return context;
    }

    // Only the metadata/PID/SET prelude is simulated. Scheduler configuration,
    // injection, watchdog selection, scheduling, and cancellation are real.
    private static final class MetadataOnlyJdbc extends JdbcTemplate {
        private final String database;

        private MetadataOnlyJdbc(String database) { this.database = database; }

        @Override public void afterPropertiesSet() {}

        @SuppressWarnings("unchecked")
        @Override public <T> T execute(ConnectionCallback<T> action) { return (T) database; }

        @Override public <T> T queryForObject(String sql, Class<T> requiredType) {
            return requiredType.cast(123L);
        }

        @Override public void execute(String sql) {}
    }
}
