package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Enforces one immediate successor per Scenario revision source. */
public class V14__scenario_revision_lineage_uniqueness extends BaseJavaMigration {

    private static final String CONSTRAINT_NAME = "uk_scenario_revision_parent";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (constraintExists(connection)) {
            return;
        }
        assertNoDuplicateParents(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table scenario_runs add constraint " + CONSTRAINT_NAME
                + " unique (revision_of_scenario_run_id)");
        }
    }

    private void assertNoDuplicateParents(Connection connection) throws SQLException {
        String sql = "select min(tenant_id), revision_of_scenario_run_id, count(*) "
            + "from scenario_runs where revision_of_scenario_run_id is not null "
            + "group by revision_of_scenario_run_id having count(*) > 1";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            if (rows.next()) {
                throw new IllegalStateException("Scenario revision lineage migration stopped: tenant_id="
                    + rows.getLong(1) + ", parent_id=" + rows.getLong(2)
                    + ", child_count=" + rows.getLong(3) + ". Resolve duplicate successors before migrating.");
            }
        }
    }

    private boolean constraintExists(Connection connection) throws SQLException {
        String sql = "select count(*) from information_schema.table_constraints "
            + "where lower(table_name) = 'scenario_runs' and lower(constraint_name) = lower(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, CONSTRAINT_NAME);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() && rows.getLong(1) > 0;
            }
        }
    }
}
