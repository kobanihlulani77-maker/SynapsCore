package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__operational_event_schema_hardening extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String databaseProduct = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);

        hardenBusinessEvents(connection, databaseProduct);
        hardenAuditLogs(connection, databaseProduct);
        hardenOperationalDispatchWorkItems(connection, databaseProduct);
    }

    private void hardenBusinessEvents(Connection connection, String databaseProduct) throws SQLException {
        if (!tableExists(connection, "business_events")) {
            return;
        }

        alterTextColumn(connection, databaseProduct, "business_events", "tenant_code", 64);
        alterTextColumn(connection, databaseProduct, "business_events", "event_type", 48);
        alterTextColumn(connection, databaseProduct, "business_events", "source", 80);
        alterTextColumn(connection, databaseProduct, "business_events", "payload_summary", 2048);
    }

    private void hardenAuditLogs(Connection connection, String databaseProduct) throws SQLException {
        if (!tableExists(connection, "audit_logs")) {
            return;
        }

        alterTextColumn(connection, databaseProduct, "audit_logs", "tenant_code", 64);
        alterTextColumn(connection, databaseProduct, "audit_logs", "action", 96);
        alterTextColumn(connection, databaseProduct, "audit_logs", "actor", 96);
        alterTextColumn(connection, databaseProduct, "audit_logs", "source", 128);
        alterTextColumn(connection, databaseProduct, "audit_logs", "target_type", 96);
        alterTextColumn(connection, databaseProduct, "audit_logs", "target_ref", 256);
        alterTextColumn(connection, databaseProduct, "audit_logs", "status", 32);
        alterTextColumn(connection, databaseProduct, "audit_logs", "details", 2048);
        alterTextColumn(connection, databaseProduct, "audit_logs", "request_id", 64);
    }

    private void hardenOperationalDispatchWorkItems(Connection connection, String databaseProduct) throws SQLException {
        if (!tableExists(connection, "operational_dispatch_work_items")) {
            return;
        }

        alterTextColumn(connection, databaseProduct, "operational_dispatch_work_items", "tenant_code", 80);
        alterTextColumn(connection, databaseProduct, "operational_dispatch_work_items", "update_type", 48);
        alterTextColumn(connection, databaseProduct, "operational_dispatch_work_items", "source", 120);
        alterTextColumn(connection, databaseProduct, "operational_dispatch_work_items", "request_id", 80);
        alterTextColumn(connection, databaseProduct, "operational_dispatch_work_items", "status", 24);
        alterTextColumn(connection, databaseProduct, "operational_dispatch_work_items", "last_error", 320);
    }

    private void alterTextColumn(Connection connection,
                                 String databaseProduct,
                                 String tableName,
                                 String columnName,
                                 int length) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            if (databaseProduct.contains("postgresql")) {
                statement.execute(
                    "alter table " + tableName
                        + " alter column " + columnName
                        + " type varchar(" + length + ") using " + columnName + "::text"
                );
            } else if (databaseProduct.contains("h2")) {
                statement.execute(
                    "alter table " + tableName
                        + " alter column " + columnName
                        + " varchar(" + length + ")"
                );
            }
        }
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
}
