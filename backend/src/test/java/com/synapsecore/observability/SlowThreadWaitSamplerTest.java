package com.synapsecore.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SlowThreadWaitSamplerTest {

    @Test
    void samplesBeforeTheOldFiveSecondBlindWindow() throws InterruptedException {
        try (SlowThreadWaitSampler sampler = SlowThreadWaitSampler.start()) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
            while (sampler.summary().samples() < 2 && System.nanoTime() < deadline) {
                Thread.sleep(100);
            }
            assertThat(sampler.summary().samples()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void distinguishesPoolAcquisitionFromJdbcAndJavaWaits() {
        StackTraceElement hikari = frame("com.zaxxer.hikari.pool.HikariPool", "getConnection");
        StackTraceElement hikariStatement = frame("com.zaxxer.hikari.pool.HikariProxyPreparedStatement", "executeQuery");
        StackTraceElement postgres = frame("org.postgresql.core.PGStream", "receiveChar");
        StackTraceElement redis = frame("io.lettuce.core.RedisFuture", "await");

        assertThat(SlowThreadWaitSampler.classify(Thread.State.WAITING,
            new StackTraceElement[]{hikari})).isEqualTo("HIKARI_ACQUIRE");
        assertThat(SlowThreadWaitSampler.classify(Thread.State.RUNNABLE,
            new StackTraceElement[]{postgres})).isEqualTo("POSTGRES_JDBC");
        assertThat(SlowThreadWaitSampler.classify(Thread.State.RUNNABLE,
            new StackTraceElement[]{postgres, hikariStatement})).isEqualTo("POSTGRES_JDBC");
        assertThat(SlowThreadWaitSampler.classify(Thread.State.WAITING,
            new StackTraceElement[]{redis})).isEqualTo("REDIS_CLIENT");
        assertThat(SlowThreadWaitSampler.classify(Thread.State.BLOCKED,
            new StackTraceElement[0])).isEqualTo("JAVA_MONITOR");
        assertThat(SlowThreadWaitSampler.classify(Thread.State.TIMED_WAITING,
            new StackTraceElement[0])).isEqualTo("JAVA_WAIT");
        assertThat(SlowThreadWaitSampler.classify(Thread.State.RUNNABLE,
            new StackTraceElement[0])).isEqualTo("JAVA_ACTIVE_OR_OTHER");
        assertThat(SlowThreadWaitSampler.representativeFrame("POSTGRES_JDBC",
            new StackTraceElement[]{postgres})).isEqualTo("org.postgresql.core.PGStream#receiveChar");
        assertThat(SlowThreadWaitSampler.representativeFrame("POSTGRES_JDBC",
            new StackTraceElement[]{postgres, frame("com.synapsecore.domain.service.OperationalViewService", "getRecentOrders")}))
            .isEqualTo("org.postgresql.core.PGStream#receiveChar via com.synapsecore.domain.service.OperationalViewService#getRecentOrders");
    }

    private StackTraceElement frame(String className, String methodName) {
        return new StackTraceElement(className, methodName, null, -1);
    }
}
