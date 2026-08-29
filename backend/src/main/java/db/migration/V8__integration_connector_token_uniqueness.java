package db.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V8__integration_connector_token_uniqueness extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")) {
            return;
        }
        if (!tableExists(connection, "integration_connectors")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("create unique index if not exists ux_integration_connectors_inbound_token_hash "
                + "on integration_connectors (inbound_access_token_hash) "
                + "where inbound_access_token_hash is not null");
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (var tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
