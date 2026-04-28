package db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class V5FullSchemaBaselineTest {

    @Test
    void postgresqlIndexLookupUsesPgIndexesCatalog() throws SQLException {
        V5__full_schema_baseline migration = new V5__full_schema_baseline();
        AtomicReference<String> preparedSql = new AtomicReference<>();
        AtomicInteger rows = new AtomicInteger();

        ResultSet resultSet = resultSetProxy(
            () -> rows.getAndIncrement() == 0,
            columnLabel -> null,
            () -> true
        );
        PreparedStatement preparedStatement = preparedStatementProxy(resultSet);
        DatabaseMetaData metaData = metadataProxy("PostgreSQL 18.3", null);
        Connection connection = connectionProxy(metaData, preparedStatement, preparedSql);

        boolean exists = migration.indexExists(connection, "products", "idx_products_tenant_id");

        assertThat(exists).isTrue();
        assertThat(preparedSql.get()).contains("lower(tablename) = lower(?)");
        assertThat(preparedSql.get()).contains("from pg_indexes");
        assertThat(preparedSql.get().toLowerCase(Locale.ROOT)).doesNotContain("information_schema.indexes");
    }

    @Test
    void nonPostgreSqlIndexLookupFallsBackToJdbcMetadata() throws SQLException {
        V5__full_schema_baseline migration = new V5__full_schema_baseline();
        AtomicInteger rows = new AtomicInteger();

        ResultSet indexResultSet = resultSetProxy(
            () -> rows.getAndIncrement() == 0,
            columnLabel -> "idx_demo",
            () -> false
        );
        DatabaseMetaData metaData = metadataProxy("H2", indexResultSet);
        Connection connection = (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getMetaData" -> metaData;
                case "prepareStatement" -> throw new AssertionError("H2 metadata fallback should not prepare PostgreSQL index SQL.");
                case "close", "commit", "rollback" -> null;
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            }
        );

        boolean exists = migration.indexExists(connection, "demo_table", "idx_demo");

        assertThat(exists).isTrue();
    }

    private static Connection connectionProxy(DatabaseMetaData metaData,
                                              PreparedStatement preparedStatement,
                                              AtomicReference<String> preparedSql) {
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getMetaData" -> metaData;
                case "prepareStatement" -> {
                    preparedSql.set((String) args[0]);
                    yield preparedStatement;
                }
                case "close", "commit", "rollback" -> null;
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static DatabaseMetaData metadataProxy(String databaseProductName, ResultSet indexResultSet) {
        return (DatabaseMetaData) Proxy.newProxyInstance(
            DatabaseMetaData.class.getClassLoader(),
            new Class<?>[]{DatabaseMetaData.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getDatabaseProductName" -> databaseProductName;
                case "getIndexInfo" -> indexResultSet;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static PreparedStatement preparedStatementProxy(ResultSet resultSet) {
        return (PreparedStatement) Proxy.newProxyInstance(
            PreparedStatement.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "executeQuery" -> resultSet;
                case "setString", "close" -> null;
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static ResultSet resultSetProxy(IntBooleanSupplier nextSupplier,
                                            StringColumnSupplier stringSupplier,
                                            IntBooleanSupplier booleanSupplier) {
        return (ResultSet) Proxy.newProxyInstance(
            ResultSet.class.getClassLoader(),
            new Class<?>[]{ResultSet.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "next" -> nextSupplier.getAsBoolean();
                case "getString" -> stringSupplier.get((String) args[0]);
                case "getBoolean" -> booleanSupplier.getAsBoolean();
                case "close" -> null;
                case "wasNull" -> false;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == float.class) {
            return 0f;
        }
        return null;
    }

    @FunctionalInterface
    private interface IntBooleanSupplier {
        boolean getAsBoolean();
    }

    @FunctionalInterface
    private interface StringColumnSupplier {
        String get(String columnLabel);
    }
}
