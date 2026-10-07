package db.migration;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Supports the platform activity feed's newest-audit-logs read without blocking writes during creation. */
public class V15__platform_audit_activity_order_index extends BaseJavaMigration {

    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("create index concurrently idx_audit_logs_created_at_desc "
                + "on audit_logs (created_at desc)");
        }
    }
}
