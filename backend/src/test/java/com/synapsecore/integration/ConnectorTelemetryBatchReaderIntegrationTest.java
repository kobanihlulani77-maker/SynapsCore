package com.synapsecore.integration;

import static org.assertj.core.api.Assertions.assertThat;
import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationImportStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConnectorTelemetryBatchReaderIntegrationTest {

    private static final String TENANT = "BATCH-TELEMETRY-TEST";
    private static final String SOURCE = "mixed-feed";

    @Autowired
    private ConnectorTelemetryBatchReader reader;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void preservesLatestCountsFailurePrecedenceAndTenantTypeIsolation() {
        Instant old = Instant.parse("2026-01-01T00:00:00Z");
        Instant recent = Instant.parse("2026-02-01T00:00:00Z");
        Instant newest = Instant.parse("2026-02-02T00:00:00Z");
        inbound(TENANT, SOURCE, "WEBHOOK_ORDER", "ACCEPTED", old, old, null);
        inbound(TENANT, SOURCE, "WEBHOOK_ORDER", "REJECTED", recent, recent, "PRODUCT_NOT_FOUND");
        inbound(TENANT, SOURCE, "WEBHOOK_ORDER", "REPLAYED", newest, newest, null);
        inbound(TENANT, SOURCE, "WEBHOOK_ORDER", "REPLAY_QUEUED", recent, newest, "CONNECTOR_DISABLED");
        inbound("OTHER-TENANT", SOURCE, "WEBHOOK_ORDER", "REJECTED", newest, newest, "PRODUCT_NOT_FOUND");
        inbound(TENANT, SOURCE, "CSV_ORDER_IMPORT", "REJECTED", newest, newest, "PRODUCT_NOT_FOUND");
        replay(TENANT, SOURCE, "WEBHOOK_ORDER", "PENDING", old, old, null);
        replay(TENANT, SOURCE, "WEBHOOK_ORDER", "REPLAY_FAILED", recent, recent, "PRODUCT_NOT_FOUND");
        replay(TENANT, SOURCE, "WEBHOOK_ORDER", "DEAD_LETTERED", newest, newest, "CONNECTOR_DISABLED");
        replay("OTHER-TENANT", SOURCE, "WEBHOOK_ORDER", "PENDING", old, old, null);
        importRun(TENANT, SOURCE, "WEBHOOK_ORDER", "FAILURE", old);
        importRun(TENANT, SOURCE, "WEBHOOK_ORDER", "PARTIAL_SUCCESS", newest);

        var result = reader.read(TENANT, List.of(connector(SOURCE, IntegrationConnectorType.WEBHOOK_ORDER)),
            Instant.parse("2026-01-31T00:00:00Z"));

        assertThat(result).hasSize(1);
        var snapshot = result.get(ConnectorTelemetryBatchReader.Key.of(SOURCE, IntegrationConnectorType.WEBHOOK_ORDER));
        assertThat(snapshot.lastActivityAt).isEqualTo(newest);
        assertThat(snapshot.lastSuccessfulActivityAt).isEqualTo(newest);
        assertThat(snapshot.recentInboundFailureCount).isEqualTo(2);
        assertThat(snapshot.inboundFailureCode).isEqualTo(IntegrationFailureCode.CONNECTOR_DISABLED);
        assertThat(snapshot.inboundFailureAt).isEqualTo(newest);
        assertThat(snapshot.pendingReplayCount).isEqualTo(2);
        assertThat(snapshot.deadLetterCount).isEqualTo(1);
        assertThat(snapshot.replayFailureCode).isEqualTo(IntegrationFailureCode.CONNECTOR_DISABLED);
        assertThat(snapshot.oldestPendingReplayAt).isEqualTo(old);
        assertThat(snapshot.lastImportStatus).isEqualTo(IntegrationImportStatus.PARTIAL_SUCCESS);
        assertThat(snapshot.lastImportAt).isEqualTo(newest);
    }

    @Test
    void readsFiftyEightConnectorsWithThreeTelemetryQueries() {
        CountingJdbc countingJdbc = new CountingJdbc(jdbc.getDataSource());
        ConnectorTelemetryBatchReader boundedReader = new ConnectorTelemetryBatchReader(countingJdbc);
        List<IntegrationConnector> connectors = new ArrayList<>();
        for (int i = 0; i < 58; i++) {
            connectors.add(connector("source-" + i, IntegrationConnectorType.WEBHOOK_ORDER));
        }

        assertThat(boundedReader.read(TENANT, connectors, Instant.parse("2026-01-01T00:00:00Z")))
            .hasSize(58);
        assertThat(countingJdbc.queryCount).isEqualTo(3);
    }

    private IntegrationConnector connector(String source, IntegrationConnectorType type) {
        return IntegrationConnector.builder().sourceSystem(source).type(type).build();
    }

    private static final class CountingJdbc extends NamedParameterJdbcTemplate {
        private int queryCount;

        private CountingJdbc(DataSource dataSource) {
            super(dataSource);
        }

        @Override
        public void query(String sql, SqlParameterSource parameters, RowCallbackHandler handler) {
            queryCount++;
        }
    }

    private void inbound(String tenant, String source, String type, String status,
                         Instant created, Instant updated, String failureCode) {
        jdbc.update("""
            insert into integration_inbound_records
                (tenant_code, source_system, connector_type, status, request_payload,
                 failure_code, failure_message, created_at, updated_at)
            values (?, ?, ?, ?, '{}', ?, 'inbound failure', ?, ?)
            """, tenant, source, type, status, failureCode, Timestamp.from(created), Timestamp.from(updated));
    }

    private void replay(String tenant, String source, String type, String status,
                        Instant created, Instant updated, String failureCode) {
        jdbc.update("""
            insert into integration_replay_records
                (tenant_code, source_system, connector_type, external_order_id, warehouse_code,
                 status, replay_attempt_count, request_payload, failure_code, failure_message,
                 created_at, updated_at)
            values (?, ?, ?, ?, 'WH-NORTH', ?, 0, '{}', ?, 'replay failure', ?, ?)
            """, tenant, source, type, "ORDER-" + status + "-" + created, status, failureCode,
            Timestamp.from(created), Timestamp.from(updated));
    }

    private void importRun(String tenant, String source, String type, String status, Instant created) {
        jdbc.update("""
            insert into integration_import_runs
                (tenant_code, source_system, connector_type, status, records_received,
                 orders_imported, orders_failed, summary, created_at)
            values (?, ?, ?, ?, 1, 0, 1, 'test import', ?)
            """, tenant, source, type, status, Timestamp.from(created));
    }
}
