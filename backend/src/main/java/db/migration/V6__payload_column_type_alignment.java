package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V6__payload_column_type_alignment extends BaseJavaMigration {

    private static final String POSTGRES_COLUMN_TYPE_SQL = """
        select lower(pg_catalog.format_type(attr.atttypid, attr.atttypmod))
        from pg_catalog.pg_attribute attr
        join pg_catalog.pg_class rel on rel.oid = attr.attrelid
        join pg_catalog.pg_namespace nsp on nsp.oid = rel.relnamespace
        where attr.attnum > 0
          and not attr.attisdropped
          and lower(nsp.nspname) = lower(current_schema())
          and lower(rel.relname) = lower(?)
          and lower(attr.attname) = lower(?)
        """;

    private static final String EXISTING_LARGE_OBJECT_OIDS_SQL_TEMPLATE = """
        select distinct %s
        from %s
        where %s is not null
          and exists (
              select 1
              from pg_catalog.pg_largeobject_metadata lom
              where lom.oid = %s
          )
        """;

    private static final String OID_TO_TEXT_COPY_SQL_TEMPLATE = """
        update %s
        set %s = %s
        where %s is null
        """;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String databaseProduct = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (!databaseProduct.contains("postgresql")) {
            return;
        }

        alignPayloadColumn(connection, "integration_inbound_records", "request_payload", "text", true);
        alignPayloadColumn(connection, "integration_replay_records", "request_payload", "text", true);
        alignPayloadColumn(connection, "scenario_runs", "request_payload", "varchar(4096)", false);
    }

    void alignPayloadColumn(Connection connection,
                            String tableName,
                            String columnName,
                            String targetType,
                            boolean required) throws SQLException {
        if (!tableExists(connection, tableName) || !columnExists(connection, tableName, columnName)) {
            cleanupTemporaryColumn(connection, tableName, temporaryColumnName(columnName));
            return;
        }

        String actualType = loadPostgresColumnType(connection, tableName, columnName);
        if (actualType == null) {
            cleanupTemporaryColumn(connection, tableName, temporaryColumnName(columnName));
            return;
        }

        if (typeMatches(actualType, targetType)) {
            cleanupTemporaryColumn(connection, tableName, temporaryColumnName(columnName));
            return;
        }

        if ("oid".equals(normalizeType(actualType))) {
            migrateOidBackedPayloadColumn(connection, tableName, columnName, targetType, required);
            return;
        }

        alterExistingCharacterPayloadColumn(connection, tableName, columnName, targetType, required);
    }

    String loadPostgresColumnType(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(POSTGRES_COLUMN_TYPE_SQL)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return null;
    }

    private void migrateOidBackedPayloadColumn(Connection connection,
                                               String tableName,
                                               String columnName,
                                               String targetType,
                                               boolean required) throws SQLException {
        String temporaryColumnName = temporaryColumnName(columnName);
        ensureTemporaryColumn(connection, tableName, temporaryColumnName, targetType);

        Set<Long> largeObjectIds = collectLargeObjectIds(connection, tableName, columnName);
        copyOidPayloadIntoTemporaryColumn(connection, tableName, columnName, temporaryColumnName, targetType);
        unlinkLargeObjects(connection, largeObjectIds);

        execute(connection, "alter table " + tableName + " drop column " + columnName);
        execute(connection, "alter table " + tableName + " rename column " + temporaryColumnName + " to " + columnName);
        if (required) {
            execute(connection, "alter table " + tableName + " alter column " + columnName + " set not null");
        }
    }

    private void alterExistingCharacterPayloadColumn(Connection connection,
                                                     String tableName,
                                                     String columnName,
                                                     String targetType,
                                                     boolean required) throws SQLException {
        String usingExpression = castExpression(columnName, targetType);
        execute(
            connection,
            "alter table " + tableName
                + " alter column " + columnName
                + " type " + targetType
                + " using " + usingExpression
        );
        if (required) {
            execute(connection, "alter table " + tableName + " alter column " + columnName + " set not null");
        }
    }

    private void ensureTemporaryColumn(Connection connection,
                                       String tableName,
                                       String temporaryColumnName,
                                       String targetType) throws SQLException {
        if (!columnExists(connection, tableName, temporaryColumnName)) {
            execute(
                connection,
                "alter table " + tableName
                    + " add column " + temporaryColumnName + " " + targetType
            );
        }
    }

    private void cleanupTemporaryColumn(Connection connection,
                                        String tableName,
                                        String temporaryColumnName) throws SQLException {
        if (tableExists(connection, tableName) && columnExists(connection, tableName, temporaryColumnName)) {
            execute(connection, "alter table " + tableName + " drop column " + temporaryColumnName);
        }
    }

    private Set<Long> collectLargeObjectIds(Connection connection,
                                            String tableName,
                                            String columnName) throws SQLException {
        String sql = EXISTING_LARGE_OBJECT_OIDS_SQL_TEMPLATE.formatted(columnName, tableName, columnName, columnName);
        Set<Long> objectIds = new LinkedHashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                objectIds.add(resultSet.getLong(1));
            }
        }
        return objectIds;
    }

    private void copyOidPayloadIntoTemporaryColumn(Connection connection,
                                                   String tableName,
                                                   String sourceColumnName,
                                                   String targetColumnName,
                                                   String targetType) throws SQLException {
        String preferredExpression = castExpression(decodedLargeObjectExpression(sourceColumnName, true), targetType);
        String fallbackExpression = castExpression(decodedLargeObjectExpression(sourceColumnName, false), targetType);
        String preferredSql = OID_TO_TEXT_COPY_SQL_TEMPLATE.formatted(
            tableName,
            targetColumnName,
            preferredExpression,
            targetColumnName
        );
        String fallbackSql = OID_TO_TEXT_COPY_SQL_TEMPLATE.formatted(
            tableName,
            targetColumnName,
            fallbackExpression,
            targetColumnName
        );

        Savepoint savepoint = connection.setSavepoint();
        try {
            execute(connection, preferredSql);
            connection.releaseSavepoint(savepoint);
        } catch (SQLException preferredFailure) {
            connection.rollback(savepoint);
            execute(connection, fallbackSql);
        }
    }

    private String decodedLargeObjectExpression(String sourceColumnName, boolean preferUtf8Decode) {
        String largeObjectExpression = preferUtf8Decode
            ? "pg_catalog.convert_from(pg_catalog.lo_get(" + sourceColumnName + "), current_setting('server_encoding'))"
            : "pg_catalog.encode(pg_catalog.lo_get(" + sourceColumnName + "), 'escape')";

        return """
            case
                when %1$s is null then null
                when exists (
                    select 1
                    from pg_catalog.pg_largeobject_metadata lom
                    where lom.oid = %1$s
                ) then %2$s
                else %1$s::text
            end
            """.formatted(sourceColumnName, largeObjectExpression).trim();
    }

    private void unlinkLargeObjects(Connection connection, Set<Long> largeObjectIds) throws SQLException {
        if (largeObjectIds.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("select pg_catalog.lo_unlink(?)")) {
            for (Long objectId : largeObjectIds) {
                statement.setLong(1, objectId);
                statement.execute();
            }
        }
    }

    private String castExpression(String expression, String targetType) {
        String normalizedTargetType = normalizeType(targetType);
        if (normalizedTargetType.startsWith("varchar(")) {
            int maxLength = Integer.parseInt(
                normalizedTargetType.substring("varchar(".length(), normalizedTargetType.length() - 1)
            );
            return "substring((" + expression + ") from 1 for " + maxLength + ")";
        }
        return "(" + expression + ")::" + targetType;
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

    private boolean typeMatches(String actualType, String expectedType) {
        return normalizeType(actualType).equals(normalizeType(expectedType));
    }

    private String normalizeType(String typeName) {
        return typeName
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("character varying", "varchar");
    }

    private String temporaryColumnName(String columnName) {
        return columnName + "_v6_migrated";
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
