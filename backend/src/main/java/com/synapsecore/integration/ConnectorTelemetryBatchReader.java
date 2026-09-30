package com.synapsecore.integration;

import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationImportStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ConnectorTelemetryBatchReader {

    private static final int SOURCE_BATCH_SIZE = 200;
    private final NamedParameterJdbcTemplate jdbc;

    record Key(String sourceSystem, IntegrationConnectorType type) {
        static Key of(String sourceSystem, IntegrationConnectorType type) {
            return new Key(sourceSystem.toLowerCase(Locale.ROOT), type);
        }
    }

    static final class Snapshot {
        Instant lastActivityAt;
        Instant lastSuccessfulActivityAt;
        IntegrationImportStatus lastImportStatus;
        Instant lastImportAt;
        long recentInboundFailureCount;
        long pendingReplayCount;
        long deadLetterCount;
        IntegrationFailureCode inboundFailureCode;
        String inboundFailureMessage;
        Instant inboundFailureAt;
        IntegrationFailureCode replayFailureCode;
        String replayFailureMessage;
        Instant replayFailureAt;
        Instant oldestPendingReplayAt;
    }

    Map<Key, Snapshot> read(String tenantCode, Collection<IntegrationConnector> connectors, Instant windowStart) {
        Map<Key, Snapshot> snapshots = new HashMap<>();
        for (IntegrationConnector connector : connectors) {
            snapshots.put(Key.of(connector.getSourceSystem(), connector.getType()), new Snapshot());
        }
        if (snapshots.isEmpty()) {
            return snapshots;
        }

        List<String> sources = snapshots.keySet().stream().map(Key::sourceSystem).distinct().toList();
        for (int start = 0; start < sources.size(); start += SOURCE_BATCH_SIZE) {
            List<String> batch = sources.subList(start, Math.min(start + SOURCE_BATCH_SIZE, sources.size()));
            var parameters = new MapSqlParameterSource()
                .addValue("tenant", tenantCode)
                .addValue("sources", batch)
                .addValue("windowStart", Timestamp.from(windowStart));
            readInbound(parameters, snapshots);
            readReplay(parameters, snapshots);
            readImports(parameters, snapshots);
        }
        return snapshots;
    }

    private void readInbound(MapSqlParameterSource parameters, Map<Key, Snapshot> snapshots) {
        jdbc.query("""
            with ranked as (
                select lower(source_system) as source_key, connector_type, created_at, updated_at,
                    status, failure_code, failure_message,
                    row_number() over (partition by lower(source_system), connector_type
                        order by created_at desc, id desc) as latest_rank,
                    row_number() over (partition by lower(source_system), connector_type
                        order by case when status in ('ACCEPTED', 'REPLAYED') then created_at end desc nulls last,
                            id desc) as success_rank,
                    row_number() over (partition by lower(source_system), connector_type
                        order by case when status in ('REJECTED', 'REPLAY_QUEUED') then updated_at end desc nulls last,
                            id desc) as failure_rank,
                    sum(case when status in ('REJECTED', 'REPLAY_QUEUED') and created_at > :windowStart
                        then 1 else 0 end) over (partition by lower(source_system), connector_type) as failure_count
                from integration_inbound_records
                where lower(tenant_code) = lower(:tenant) and lower(source_system) in (:sources)
            )
            select * from ranked where latest_rank = 1 or success_rank = 1 or failure_rank = 1
            """, parameters, rs -> {
                Snapshot snapshot = snapshotFor(rs, snapshots);
                if (snapshot == null) {
                    return;
                }
                snapshot.recentInboundFailureCount = rs.getLong("failure_count");
                String status = rs.getString("status");
                if (rs.getLong("latest_rank") == 1) {
                    snapshot.lastActivityAt = instant(rs, "created_at");
                }
                if (rs.getLong("success_rank") == 1
                    && ("ACCEPTED".equals(status) || "REPLAYED".equals(status))) {
                    snapshot.lastSuccessfulActivityAt = instant(rs, "created_at");
                }
                if (rs.getLong("failure_rank") == 1
                    && ("REJECTED".equals(status) || "REPLAY_QUEUED".equals(status))) {
                    snapshot.inboundFailureCode = failureCode(rs, "failure_code");
                    snapshot.inboundFailureMessage = rs.getString("failure_message");
                    snapshot.inboundFailureAt = instant(rs, "updated_at");
                }
            });
    }

    private void readReplay(MapSqlParameterSource parameters, Map<Key, Snapshot> snapshots) {
        jdbc.query("""
            with ranked as (
                select lower(source_system) as source_key, connector_type, created_at, updated_at,
                    status, failure_code, failure_message,
                    row_number() over (partition by lower(source_system), connector_type
                        order by case when status in ('REPLAY_FAILED', 'DEAD_LETTERED') then updated_at end desc nulls last,
                            id desc) as issue_rank,
                    row_number() over (partition by lower(source_system), connector_type
                        order by case when status in ('PENDING', 'REPLAY_FAILED') then created_at end asc nulls last,
                            id asc) as pending_rank,
                    sum(case when status in ('PENDING', 'REPLAY_FAILED') then 1 else 0 end)
                        over (partition by lower(source_system), connector_type) as pending_count,
                    sum(case when status = 'DEAD_LETTERED' then 1 else 0 end)
                        over (partition by lower(source_system), connector_type) as dead_count
                from integration_replay_records
                where lower(tenant_code) = lower(:tenant) and lower(source_system) in (:sources)
            )
            select * from ranked where issue_rank = 1 or pending_rank = 1
            """, parameters, rs -> {
                Snapshot snapshot = snapshotFor(rs, snapshots);
                if (snapshot == null) {
                    return;
                }
                snapshot.pendingReplayCount = rs.getLong("pending_count");
                snapshot.deadLetterCount = rs.getLong("dead_count");
                String status = rs.getString("status");
                if (rs.getLong("issue_rank") == 1
                    && ("REPLAY_FAILED".equals(status) || "DEAD_LETTERED".equals(status))) {
                    snapshot.replayFailureCode = failureCode(rs, "failure_code");
                    snapshot.replayFailureMessage = rs.getString("failure_message");
                    snapshot.replayFailureAt = instant(rs, "updated_at");
                }
                if (rs.getLong("pending_rank") == 1
                    && ("PENDING".equals(status) || "REPLAY_FAILED".equals(status))) {
                    snapshot.oldestPendingReplayAt = instant(rs, "created_at");
                }
            });
    }

    private void readImports(MapSqlParameterSource parameters, Map<Key, Snapshot> snapshots) {
        jdbc.query("""
            with ranked as (
                select lower(source_system) as source_key, connector_type, created_at, status,
                    row_number() over (partition by lower(source_system), connector_type
                        order by created_at desc, id desc) as latest_rank
                from integration_import_runs
                where lower(tenant_code) = lower(:tenant) and lower(source_system) in (:sources)
            )
            select * from ranked where latest_rank = 1
            """, parameters, rs -> {
                Snapshot snapshot = snapshotFor(rs, snapshots);
                if (snapshot != null) {
                    snapshot.lastImportStatus = IntegrationImportStatus.valueOf(rs.getString("status"));
                    snapshot.lastImportAt = instant(rs, "created_at");
                }
            });
    }

    private static Snapshot snapshotFor(ResultSet rs, Map<Key, Snapshot> snapshots) throws SQLException {
        return snapshots.get(Key.of(rs.getString("source_key"),
            IntegrationConnectorType.valueOf(rs.getString("connector_type"))));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static IntegrationFailureCode failureCode(ResultSet rs, String column) throws SQLException {
        String code = rs.getString(column);
        return code == null ? null : IntegrationFailureCode.valueOf(code);
    }
}
