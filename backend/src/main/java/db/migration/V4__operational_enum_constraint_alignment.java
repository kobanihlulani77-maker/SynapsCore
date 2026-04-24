package db.migration;

import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.event.OperationalUpdateType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V4__operational_enum_constraint_alignment extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String databaseProduct = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (!databaseProduct.contains("postgresql")) {
            return;
        }

        alignEnumCheckConstraint(
            connection,
            "business_events",
            "event_type",
            "chk_business_events_event_type_synapsecore",
            Stream.of(BusinessEventType.values()).map(Enum::name).toList()
        );
        alignEnumCheckConstraint(
            connection,
            "audit_logs",
            "status",
            "chk_audit_logs_status_synapsecore",
            Stream.of(AuditStatus.values()).map(Enum::name).toList()
        );
        alignEnumCheckConstraint(
            connection,
            "operational_dispatch_work_items",
            "status",
            "chk_operational_dispatch_status_synapsecore",
            Stream.of(OperationalDispatchStatus.values()).map(Enum::name).toList()
        );
        alignEnumCheckConstraint(
            connection,
            "operational_dispatch_work_items",
            "update_type",
            "chk_operational_dispatch_update_type_synapsecore",
            Stream.of(OperationalUpdateType.values()).map(Enum::name).toList()
        );
    }

    private void alignEnumCheckConstraint(Connection connection,
                                          String tableName,
                                          String columnName,
                                          String targetConstraintName,
                                          List<String> allowedValues) throws SQLException {
        if (!tableExists(connection, tableName) || !columnExists(connection, tableName, columnName)) {
            return;
        }

        for (String constraintName : findColumnCheckConstraints(connection, tableName, columnName)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("alter table " + tableName + " drop constraint if exists " + constraintName);
            }
        }

        String allowedClause = allowedValues.stream()
            .map(value -> "'" + value.replace("'", "''") + "'")
            .collect(Collectors.joining(", "));
        String ddl = "alter table " + tableName
            + " add constraint " + targetConstraintName
            + " check (" + columnName + " in (" + allowedClause + "))";
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        }
    }

    private List<String> findColumnCheckConstraints(Connection connection,
                                                    String tableName,
                                                    String columnName) throws SQLException {
        String sql = """
            select distinct con.conname
            from pg_constraint con
            join pg_class rel on rel.oid = con.conrelid
            join pg_namespace nsp on nsp.oid = rel.relnamespace
            where con.contype = 'c'
              and lower(rel.relname) = lower(?)
              and lower(nsp.nspname) = lower(current_schema())
              and lower(pg_get_constraintdef(con.oid)) like ?
            """;

        List<String> constraintNames = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, "%" + columnName.toLowerCase(Locale.ROOT) + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    constraintNames.add(resultSet.getString(1));
                }
            }
        }
        return constraintNames;
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
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
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, null, null)) {
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
