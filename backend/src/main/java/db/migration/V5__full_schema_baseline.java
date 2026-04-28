package db.migration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V5__full_schema_baseline extends BaseJavaMigration {

    private static final String BASELINE_RESOURCE = "db/support/full-schema-baseline.sql";
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("^create table if not exists\\s+([a-zA-Z0-9_]+)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_INDEX_PATTERN = Pattern.compile("^create index if not exists\\s+([a-zA-Z0-9_]+)\\s+on\\s+([a-zA-Z0-9_]+)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADD_CONSTRAINT_PATTERN = Pattern.compile("^alter table if exists\\s+([a-zA-Z0-9_]+)\\s+add constraint\\s+([a-zA-Z0-9_]+)\\s+", Pattern.CASE_INSENSITIVE);
    private static final String POSTGRES_INDEX_EXISTS_SQL = """
        select exists (
            select 1
            from pg_indexes
            where lower(schemaname) = lower(current_schema())
              and lower(tablename) = lower(?)
              and lower(indexname) = lower(?)
        )
        """;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (String statement : loadStatements()) {
            applyStatement(connection, statement);
        }
    }

    private void applyStatement(Connection connection, String statement) throws SQLException {
        Matcher createTableMatcher = CREATE_TABLE_PATTERN.matcher(statement);
        if (createTableMatcher.find()) {
            String tableName = createTableMatcher.group(1);
            if (!tableExists(connection, tableName)) {
                execute(connection, statement);
            }
            return;
        }

        Matcher createIndexMatcher = CREATE_INDEX_PATTERN.matcher(statement);
        if (createIndexMatcher.find()) {
            String indexName = createIndexMatcher.group(1);
            String tableName = createIndexMatcher.group(2);
            if (!indexExists(connection, tableName, indexName)) {
                execute(connection, statement);
            }
            return;
        }

        Matcher addConstraintMatcher = ADD_CONSTRAINT_PATTERN.matcher(statement);
        if (addConstraintMatcher.find()) {
            String tableName = addConstraintMatcher.group(1);
            String constraintName = addConstraintMatcher.group(2);
            if (tableExists(connection, tableName) && !constraintExists(connection, constraintName)) {
                execute(connection, statement);
            }
            return;
        }

        execute(connection, statement);
    }

    private void execute(Connection connection, String statement) throws SQLException {
        try (Statement jdbcStatement = connection.createStatement()) {
            jdbcStatement.execute(statement);
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

    boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        String databaseProduct = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (databaseProduct.contains("postgresql")) {
            try (PreparedStatement statement = connection.prepareStatement(POSTGRES_INDEX_EXISTS_SQL)) {
                statement.setString(1, tableName);
                statement.setString(2, indexName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() && resultSet.getBoolean(1);
                }
            }
        }

        try (ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
            while (indexes.next()) {
                String existingIndexName = indexes.getString("INDEX_NAME");
                if (existingIndexName != null && existingIndexName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean constraintExists(Connection connection, String constraintName) throws SQLException {
        String sql = """
            select count(*)
            from information_schema.table_constraints
            where lower(constraint_name) = lower(?)
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, constraintName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        }
    }

    private List<String> loadStatements() throws Exception {
        InputStream inputStream = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(BASELINE_RESOURCE);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find Flyway baseline resource " + BASELINE_RESOURCE + ".");
        }

        String sql;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            sql = reader.lines().collect(Collectors.joining("\n"));
        }

        String[] rawStatements = sql.split(";");
        List<String> statements = new ArrayList<>();
        for (String rawStatement : rawStatements) {
            String trimmed = rawStatement.trim();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }
        return statements;
    }
}
