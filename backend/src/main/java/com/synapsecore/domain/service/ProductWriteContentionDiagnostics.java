package com.synapsecore.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/**
 * Captures bounded PostgreSQL lock metadata for a Product write without exposing
 * SQL parameters or making the diagnostic path externally callable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductWriteContentionDiagnostics {

    private static final long LOCK_TIMEOUT_MS = 15_000L;
    private static final long WATCHDOG_DELAY_MS = 3_000L;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<TaskScheduler> taskSchedulerProvider;

    public ProductWriteWatch begin(String requestId, String tenantCode, long startedAtNanos) {
        if (!isPostgres()) {
            return ProductWriteWatch.NO_OP;
        }

        Long backendPid = currentBackendPid();
        if (backendPid == null) {
            log.warn("Product write PostgreSQL session PID unavailable requestId={} tenant={} stage=PRODUCT_SAVE_START",
                requestId, tenantCode);
            return ProductWriteWatch.NO_OP;
        }

        try {
            jdbcTemplate.execute("set local lock_timeout = '" + LOCK_TIMEOUT_MS + "ms'");
        } catch (DataAccessException exception) {
            log.warn("Product write lock timeout could not be configured requestId={} tenant={} backendPid={}",
                requestId, tenantCode, backendPid);
        }

        log.info("Product write PostgreSQL session requestId={} tenant={} backendPid={} stage=PRODUCT_SAVE_START elapsedMs={}",
            requestId, tenantCode, backendPid, elapsedMs(startedAtNanos));

        TaskScheduler scheduler = taskSchedulerProvider.getIfAvailable();
        if (scheduler == null) {
            return ProductWriteWatch.NO_OP;
        }

        ScheduledFuture<?> watchdog = scheduler.schedule(
            () -> inspect(requestId, tenantCode, backendPid),
            Instant.now().plusMillis(WATCHDOG_DELAY_MS)
        );
        return new ProductWriteWatch(watchdog);
    }

    private void inspect(String requestId, String tenantCode, long targetPid) {
        try {
            List<Map<String, Object>> targetRows = jdbcTemplate.queryForList(
                """
                select pid,
                       state,
                       wait_event_type,
                       wait_event,
                       coalesce(extract(epoch from (clock_timestamp() - xact_start)) * 1000, 0) as xact_age_ms,
                       coalesce(extract(epoch from (clock_timestamp() - query_start)) * 1000, 0) as query_age_ms,
                       application_name
                from pg_stat_activity
                where pid = ?
                """,
                targetPid
            );
            if (targetRows.isEmpty()) {
                return;
            }

            Map<String, Object> target = targetRows.getFirst();
            List<Long> blockerPids = jdbcTemplate.queryForList(
                "select unnest(pg_blocking_pids(?))",
                Long.class,
                targetPid
            );
            if (blockerPids.isEmpty()
                && target.get("wait_event_type") == null
                && target.get("wait_event") == null) {
                return;
            }

            log.warn("Product write contention requestId={} tenant={} targetPid={} state={} waitEventType={} waitEvent={} xactAgeMs={} queryAgeMs={} applicationName={} blockingPids={}",
                requestId,
                tenantCode,
                target.get("pid"),
                target.get("state"),
                target.get("wait_event_type"),
                target.get("wait_event"),
                target.get("xact_age_ms"),
                target.get("query_age_ms"),
                target.get("application_name"),
                blockerPids);

            for (Long blockerPid : blockerPids) {
                List<Map<String, Object>> blockerRows = jdbcTemplate.queryForList(
                    """
                    select pid,
                           state,
                           wait_event_type,
                           wait_event,
                           coalesce(extract(epoch from (clock_timestamp() - xact_start)) * 1000, 0) as xact_age_ms,
                           coalesce(extract(epoch from (clock_timestamp() - query_start)) * 1000, 0) as query_age_ms,
                           application_name
                    from pg_stat_activity
                    where pid = ?
                    """,
                    blockerPid
                );
                if (!blockerRows.isEmpty()) {
                    Map<String, Object> blocker = blockerRows.getFirst();
                    log.warn("Product write blocker requestId={} tenant={} blockerPid={} state={} waitEventType={} waitEvent={} xactAgeMs={} queryAgeMs={} applicationName={}",
                        requestId,
                        tenantCode,
                        blocker.get("pid"),
                        blocker.get("state"),
                        blocker.get("wait_event_type"),
                        blocker.get("wait_event"),
                        blocker.get("xact_age_ms"),
                        blocker.get("query_age_ms"),
                        blocker.get("application_name"));
                }
            }
        } catch (DataAccessException exception) {
            log.warn("Product write contention probe unavailable requestId={} tenant={} targetPid={} reason={}",
                requestId, tenantCode, targetPid, exception.getMostSpecificCause() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMostSpecificCause().getClass().getSimpleName());
        }
    }

    private boolean isPostgres() {
        try {
            String databaseProduct = jdbcTemplate.execute(
                (ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());
            return databaseProduct.toLowerCase(java.util.Locale.ROOT).contains("postgresql");
        } catch (DataAccessException exception) {
            return false;
        }
    }

    private Long currentBackendPid() {
        try {
            return jdbcTemplate.queryForObject("select pg_backend_pid()", Long.class);
        } catch (DataAccessException exception) {
            return null;
        }
    }

    private long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    public static final class ProductWriteWatch implements AutoCloseable {
        public static final ProductWriteWatch NO_OP = new ProductWriteWatch(null);
        private final ScheduledFuture<?> watchdog;

        private ProductWriteWatch(ScheduledFuture<?> watchdog) {
            this.watchdog = watchdog;
        }

        @Override
        public void close() {
            if (watchdog != null) {
                watchdog.cancel(false);
            }
        }
    }
}
