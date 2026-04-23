package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V1__inventory_stock_columns extends BaseJavaMigration {

    private static final String INVENTORY_TABLE = "inventory";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, INVENTORY_TABLE)) {
            return;
        }

        ensureColumnExists(connection, "quantity_on_hand");
        ensureColumnExists(connection, "quantity_reserved");
        ensureColumnExists(connection, "quantity_inbound");
        backfillStockColumns(connection);
        enforceNotNull(connection, "quantity_on_hand");
        enforceNotNull(connection, "quantity_reserved");
        enforceNotNull(connection, "quantity_inbound");
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

    private boolean columnExists(Connection connection, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(null, null, null, null)) {
            while (columns.next()) {
                String tableName = columns.getString("TABLE_NAME");
                String existingColumnName = columns.getString("COLUMN_NAME");
                if (tableName != null
                    && existingColumnName != null
                    && tableName.equalsIgnoreCase(INVENTORY_TABLE)
                    && existingColumnName.equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void ensureColumnExists(Connection connection, String columnName) throws SQLException {
        if (columnExists(connection, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table " + INVENTORY_TABLE + " add column " + columnName + " bigint");
        }
    }

    private void backfillStockColumns(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
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
    }

    private void enforceNotNull(Connection connection, String columnName) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        try (Statement statement = connection.createStatement()) {
            if (productName.contains("postgresql") || productName.contains("h2")) {
                statement.execute("alter table " + INVENTORY_TABLE + " alter column " + columnName + " set not null");
            }
        }
    }
}
