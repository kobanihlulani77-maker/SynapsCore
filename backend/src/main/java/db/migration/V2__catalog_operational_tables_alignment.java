package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__catalog_operational_tables_alignment extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureProductsColumns(connection);
        ensureBusinessEventColumns(connection);
        ensureAuditLogColumns(connection);
        ensureOperationalDispatchColumns(connection);
    }

    private void ensureProductsColumns(Connection connection) throws SQLException {
        if (!tableExists(connection, "products")) {
            return;
        }

        ensureColumnExists(connection, "products", "tenant_id", "bigint");
        ensureColumnExists(connection, "products", "catalog_sku", "varchar(64)");
        backfillCatalogSku(connection);
    }

    private void ensureBusinessEventColumns(Connection connection) throws SQLException {
        if (!tableExists(connection, "business_events")) {
            return;
        }

        ensureColumnExists(connection, "business_events", "tenant_code", "varchar(64)");
        ensureColumnExists(connection, "business_events", "event_type", "varchar(48)");
        ensureColumnExists(connection, "business_events", "source", "varchar(80)");
        ensureColumnExists(connection, "business_events", "payload_summary", "varchar(2048)");
        ensureColumnExists(connection, "business_events", "created_at", "timestamp");
    }

    private void ensureAuditLogColumns(Connection connection) throws SQLException {
        if (!tableExists(connection, "audit_logs")) {
            return;
        }

        ensureColumnExists(connection, "audit_logs", "tenant_code", "varchar(64)");
        ensureColumnExists(connection, "audit_logs", "action", "varchar(96)");
        ensureColumnExists(connection, "audit_logs", "actor", "varchar(96)");
        ensureColumnExists(connection, "audit_logs", "source", "varchar(128)");
        ensureColumnExists(connection, "audit_logs", "target_type", "varchar(96)");
        ensureColumnExists(connection, "audit_logs", "target_ref", "varchar(256)");
        ensureColumnExists(connection, "audit_logs", "status", "varchar(32)");
        ensureColumnExists(connection, "audit_logs", "details", "varchar(2048)");
        ensureColumnExists(connection, "audit_logs", "request_id", "varchar(64)");
        ensureColumnExists(connection, "audit_logs", "created_at", "timestamp");
    }

    private void ensureOperationalDispatchColumns(Connection connection) throws SQLException {
        if (!tableExists(connection, "operational_dispatch_work_items")) {
            return;
        }

        ensureColumnExists(connection, "operational_dispatch_work_items", "tenant_code", "varchar(80)");
        ensureColumnExists(connection, "operational_dispatch_work_items", "update_type", "varchar(48)");
        ensureColumnExists(connection, "operational_dispatch_work_items", "source", "varchar(120)");
        ensureColumnExists(connection, "operational_dispatch_work_items", "request_id", "varchar(80)");
        ensureColumnExists(connection, "operational_dispatch_work_items", "status", "varchar(24)");
        ensureColumnExists(connection, "operational_dispatch_work_items", "attempt_count", "integer");
        ensureColumnExists(connection, "operational_dispatch_work_items", "occurred_at", "timestamp");
        ensureColumnExists(connection, "operational_dispatch_work_items", "processed_at", "timestamp");
        ensureColumnExists(connection, "operational_dispatch_work_items", "last_error", "varchar(320)");
        ensureColumnExists(connection, "operational_dispatch_work_items", "created_at", "timestamp");
        ensureColumnExists(connection, "operational_dispatch_work_items", "updated_at", "timestamp");
    }

    private void backfillCatalogSku(Connection connection) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "select id, sku, catalog_sku from products where catalog_sku is null or trim(catalog_sku) = ''");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                long id = rows.getLong("id");
                String sku = rows.getString("sku");
                String derivedCatalogSku = deriveCatalogSku(sku);
                if (derivedCatalogSku == null || derivedCatalogSku.isBlank()) {
                    continue;
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "update products set catalog_sku = ? where id = ?")) {
                    update.setString(1, derivedCatalogSku);
                    update.setLong(2, id);
                    update.executeUpdate();
                }
            }
        }
    }

    private String deriveCatalogSku(String storedSku) {
        if (storedSku == null || storedSku.isBlank()) {
            return null;
        }
        String trimmed = storedSku.trim().toUpperCase(Locale.ROOT);
        int separatorIndex = trimmed.indexOf("::");
        if (separatorIndex >= 0 && separatorIndex + 2 < trimmed.length()) {
            return trimmed.substring(separatorIndex + 2);
        }
        return trimmed;
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                String existingTableName = tables.getString("TABLE_NAME");
                if (existingTableName != null && existingTableName.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(null, null, null, null)) {
            while (columns.next()) {
                String existingTableName = columns.getString("TABLE_NAME");
                String existingColumnName = columns.getString("COLUMN_NAME");
                if (existingTableName != null
                    && existingColumnName != null
                    && existingTableName.equalsIgnoreCase(tableName)
                    && existingColumnName.equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void ensureColumnExists(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table " + tableName + " add column " + columnName + " " + definition);
        }
    }
}
