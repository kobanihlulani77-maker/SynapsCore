package db.migration;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds optimistic concurrency and non-destructive warehouse lifecycle state. */
public class V13__workspace_warehouse_lifecycle_safety extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table tenants add column if not exists version bigint not null default 0");
            statement.execute("alter table warehouses add column if not exists version bigint not null default 0");
            statement.execute("alter table warehouses add column if not exists active boolean not null default true");
            statement.execute("alter table integration_connectors add column if not exists version bigint not null default 0");
        }
    }
}
