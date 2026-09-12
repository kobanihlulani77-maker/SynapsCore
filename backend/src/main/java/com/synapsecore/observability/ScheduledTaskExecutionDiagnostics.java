package com.synapsecore.observability;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.util.function.IntSupplier;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskExecutionDiagnostics {

    private final DataSource dataSource;

    public int observe(String taskName, IntSupplier work) {
        long startedAtNanos = System.nanoTime();
        PoolSnapshot startedPool = poolSnapshot();
        log.info(
            "Scheduled work task={} stage=START thread={} hikariTotal={} hikariActive={} hikariIdle={} hikariWaiting={}",
            taskName,
            Thread.currentThread().getName(),
            startedPool.total(),
            startedPool.active(),
            startedPool.idle(),
            startedPool.waiting()
        );

        try {
            int processed = work.getAsInt();
            PoolSnapshot completedPool = poolSnapshot();
            log.info(
                "Scheduled work task={} stage=COMPLETE thread={} durationMs={} processed={} hikariTotal={} hikariActive={} hikariIdle={} hikariWaiting={}",
                taskName,
                Thread.currentThread().getName(),
                elapsedMs(startedAtNanos),
                processed,
                completedPool.total(),
                completedPool.active(),
                completedPool.idle(),
                completedPool.waiting()
            );
            return processed;
        } catch (RuntimeException | Error exception) {
            PoolSnapshot failedPool = poolSnapshot();
            log.error(
                "Scheduled work task={} stage=FAILED thread={} durationMs={} failureType={} hikariTotal={} hikariActive={} hikariIdle={} hikariWaiting={}",
                taskName,
                Thread.currentThread().getName(),
                elapsedMs(startedAtNanos),
                exception.getClass().getSimpleName(),
                failedPool.total(),
                failedPool.active(),
                failedPool.idle(),
                failedPool.waiting()
            );
            throw exception;
        }
    }

    private PoolSnapshot poolSnapshot() {
        if (!(dataSource instanceof HikariDataSource hikariDataSource) || !hikariDataSource.isRunning()) {
            return PoolSnapshot.UNAVAILABLE;
        }
        HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
        if (pool == null) {
            return PoolSnapshot.UNAVAILABLE;
        }
        return new PoolSnapshot(
            pool.getTotalConnections(),
            pool.getActiveConnections(),
            pool.getIdleConnections(),
            pool.getThreadsAwaitingConnection()
        );
    }

    private long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    private record PoolSnapshot(int total, int active, int idle, int waiting) {
        private static final PoolSnapshot UNAVAILABLE = new PoolSnapshot(-1, -1, -1, -1);
    }
}
