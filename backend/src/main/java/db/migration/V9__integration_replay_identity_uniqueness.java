package db.migration;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Prevents duplicate active recovery work for one tenant-wide business identity.
 */
public class V9__integration_replay_identity_uniqueness extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String databaseProduct = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (!databaseProduct.contains("postgresql")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("create unique index if not exists ux_integration_replay_active_identity "
                + "on integration_replay_records (lower(tenant_code), lower(external_order_id)) "
                + "where status in ('PENDING', 'REPLAY_FAILED', 'DEAD_LETTERED')");
        }
    }
}
