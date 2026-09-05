package com.synapsecore.domain.service;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentitySequenceMigrationService {

    private static final String CORE_IDENTITY_WRITE_LOCK_KEY = "synapsecore.core-identity-writes";

    private static final List<String> CORE_IDENTITY_TABLES = List.of(
        "products",
        "business_events",
        "audit_logs",
        "operational_dispatch_work_items"
    );

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void synchronizeCoreIdentitySequences() {
        String databaseProductName = databaseProductName();
        if (databaseProductName.contains("postgresql")) {
            acquirePostgresCoreIdentityWriteLock();
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

    public void acquireCoreIdentityWriteLock() {
        if (databaseProductName().contains("postgresql")) {
            acquirePostgresCoreIdentityWriteLock();
        }
    }

    private void acquirePostgresCoreIdentityWriteLock() {
        jdbcTemplate.execute(
            "select pg_advisory_xact_lock(hashtext('" + CORE_IDENTITY_WRITE_LOCK_KEY + "'))"
        );
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

        long maxId = maxIdentityValue(tableName);
        long nextId = jdbcTemplate.queryForObject(
            "select case when is_called then last_value + 1 else last_value end from " + sequenceName,
            Long.class
        );
        if (nextId <= maxId) {
            long repairedNextId = maxId + 1;
            jdbcTemplate.execute("select setval('" + escapeSqlLiteral(sequenceName) + "', " + repairedNextId + ", false)");
            log.info("Repaired PostgreSQL identity sequence {} for table {} to next id {}.", sequenceName, tableName, repairedNextId);
        }
    }

    private void synchronizeH2IdentitySequence(String tableName) {
        if (!tableExists(tableName)) {
            return;
        }

        long maxId = maxIdentityValue(tableName);
        Long nextId = jdbcTemplate.queryForObject(
            """
            select identity_base
            from information_schema.columns
            where upper(table_name) = upper(?)
              and upper(column_name) = 'ID'
              and is_identity = 'YES'
            """,
            Long.class,
            tableName
        );
        if (nextId == null || nextId <= maxId) {
            long repairedNextId = maxId + 1;
            jdbcTemplate.execute("alter table " + tableName + " alter column id restart with " + repairedNextId);
            log.info("Repaired H2 identity for table {} to next id {}.", tableName, repairedNextId);
        }
    }

    private long maxIdentityValue(String tableName) {
        Long maxId = jdbcTemplate.queryForObject(
            "select coalesce(max(id), 0) from " + tableName,
            Long.class
        );
        return maxId == null ? 0L : maxId;
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
