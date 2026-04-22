package com.synapsecore.domain.service;

import jakarta.annotation.PostConstruct;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventorySchemaMigrationService {

    private static final String INVENTORY_TABLE = "inventory";
    private static final String QUANTITY_ON_HAND = "quantity_on_hand";
    private static final String QUANTITY_RESERVED = "quantity_reserved";
    private static final String QUANTITY_INBOUND = "quantity_inbound";

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void migrateOnStartup() {
        migrateInventoryStockColumns();
    }

    @Transactional
    public synchronized void migrateInventoryStockColumns() {
        if (!tableExists(INVENTORY_TABLE)) {
            log.info("Inventory table does not exist yet; skipping inventory stock-column schema migration.");
            return;
        }

        ensureColumnExists(QUANTITY_ON_HAND);
        ensureColumnExists(QUANTITY_RESERVED);
        ensureColumnExists(QUANTITY_INBOUND);
        backfillStockColumns();
        enforceNotNull(QUANTITY_ON_HAND);
        enforceNotNull(QUANTITY_RESERVED);
        enforceNotNull(QUANTITY_INBOUND);

        log.info("Inventory stock-column schema migration completed.");
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

    private boolean columnExists(String columnName) {
        Long count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.columns
            where lower(table_name) = ?
              and lower(column_name) = ?
            """,
            Long.class,
            INVENTORY_TABLE,
            columnName.toLowerCase(Locale.ROOT)
        );
        return count != null && count > 0;
    }

    private void ensureColumnExists(String columnName) {
        if (columnExists(columnName)) {
            return;
        }
        jdbcTemplate.execute("alter table " + INVENTORY_TABLE + " add column " + columnName + " bigint");
    }

    private void backfillStockColumns() {
        jdbcTemplate.update(
            """
            update inventory
            set quantity_on_hand = coalesce(quantity_on_hand, quantity_available, 0),
                quantity_reserved = coalesce(quantity_reserved, 0),
                quantity_inbound = coalesce(quantity_inbound, 0)
            where quantity_on_hand is null
               or quantity_reserved is null
               or quantity_inbound is null
            """
        );
    }

    private void enforceNotNull(String columnName) {
        jdbcTemplate.execute("alter table " + INVENTORY_TABLE + " alter column " + columnName + " set not null");
    }
}
