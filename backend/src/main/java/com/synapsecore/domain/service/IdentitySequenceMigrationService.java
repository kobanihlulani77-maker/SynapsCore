package com.synapsecore.domain.service;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentitySequenceMigrationService {

    private static final List<String> CORE_IDENTITY_TABLES = List.of(
        "products",
        "business_events",
        "audit_logs",
        "operational_dispatch_work_items"
    );

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public synchronized void synchronizeCoreIdentitySequences() {
        String databaseProductName = databaseProductName();
        if (databaseProductName.contains("postgresql")) {
            for (String tableName : CORE_IDENTITY_TABLES) {
                synchronizePostgresIdentitySequence(tableName);
            }
            return;
        }

        if (databaseProductName.contains("h2")) {
            for (String tableName : CORE_IDENTITY_TABLES) {
                synchronizeH2IdentitySequence(tableName);
            }
            return;
        }

        log.info("Skipping identity-sequence migration for unsupported database product {}.", databaseProductName);
    }

    private void synchronizePostgresIdentitySequence(String tableName) {
        if (!tableExists(tableName)) {
            return;
        }

        String sequenceName = jdbcTemplate.queryForObject(
            "select pg_get_serial_sequence(?, 'id')",
            String.class,
            tableName
        );
        if (sequenceName == null || sequenceName.isBlank()) {
            return;
        }

        long nextId = nextIdentityValue(tableName);
        jdbcTemplate.execute("select setval('" + escapeSqlLiteral(sequenceName) + "', " + nextId + ", false)");
        log.info("Synchronized PostgreSQL identity sequence {} for table {} to next id {}.", sequenceName, tableName, nextId);
    }

    private void synchronizeH2IdentitySequence(String tableName) {
        if (!tableExists(tableName)) {
            return;
        }

        long nextId = nextIdentityValue(tableName);
        jdbcTemplate.execute("alter table " + tableName + " alter column id restart with " + nextId);
        log.info("Synchronized H2 identity for table {} to next id {}.", tableName, nextId);
    }

    private long nextIdentityValue(String tableName) {
        Long nextId = jdbcTemplate.queryForObject(
            "select coalesce(max(id), 0) + 1 from " + tableName,
            Long.class
        );
        return nextId == null ? 1L : nextId;
    }

    private boolean tableExists(String tableName) {
        Long count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where lower(table_name) = ?
            """,
            Long.class,
            tableName.toLowerCase(Locale.ROOT)
        );
        return count != null && count > 0;
    }

    private String databaseProductName() {
        return jdbcTemplate.execute((ConnectionCallback<String>) connection ->
            connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT)
        );
    }

    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }
}
