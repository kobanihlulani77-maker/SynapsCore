package db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

class V7IntegrationConstraintAlignmentTest {

    @Test
    void postgresReplayStatusConstraintIsRebuiltWithDeadLetteredSupport() throws Exception {
        V7__integration_constraint_alignment migration = new V7__integration_constraint_alignment();
        List<String> executedSql = new ArrayList<>();
        AtomicInteger preparedQueryCount = new AtomicInteger();

        DatabaseMetaData metadata = metadataProxy(
            List.of("integration_replay_records"),
            List.of(Map.of("TABLE_NAME", "integration_replay_records", "COLUMN_NAME", "status"))
        );

        Connection connection = (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getMetaData" -> metadataWithProduct(metadata, "PostgreSQL 17");
                case "prepareStatement" -> preparedStatementProxy(preparedQueryCount);
                case "createStatement" -> statementProxy(executedSql);
                case "close", "commit", "rollback" -> null;
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            }
        );

        migration.migrate(contextProxy(connection));

        assertThat(preparedQueryCount.get()).isGreaterThan(0);
        assertThat(executedSql).anySatisfy(sql -> assertThat(sql)
            .contains("alter table integration_replay_records drop constraint if exists integration_replay_records_status_check"));
        assertThat(executedSql).anySatisfy(sql -> assertThat(sql)
            .contains("alter table integration_replay_records add constraint chk_integration_replay_status_synapsecore")
            .contains("'PENDING'")
            .contains("'REPLAY_FAILED'")
            .contains("'DEAD_LETTERED'")
            .contains("'REPLAYED'"));
    }

    private static DatabaseMetaData metadataProxy(List<String> tableNames, List<Map<String, String>> columns) {
        return (DatabaseMetaData) Proxy.newProxyInstance(
            DatabaseMetaData.class.getClassLoader(),
            new Class<?>[]{DatabaseMetaData.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getTables" -> resultSetFromRows(tableNames.stream()
                    .map(tableName -> Map.of("TABLE_NAME", tableName))
                    .toList());
                case "getColumns" -> resultSetFromRows(columns);
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static DatabaseMetaData metadataWithProduct(DatabaseMetaData delegate, String productName) {
        return (DatabaseMetaData) Proxy.newProxyInstance(
            DatabaseMetaData.class.getClassLoader(),
            new Class<?>[]{DatabaseMetaData.class},
            (proxy, method, args) -> {
                if ("getDatabaseProductName".equals(method.getName())) {
                    return productName;
                }
                return method.invoke(delegate, args);
            }
        );
    }

    private static Context contextProxy(Connection connection) {
        return (Context) Proxy.newProxyInstance(
            Context.class.getClassLoader(),
            new Class<?>[]{Context.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getConnection" -> connection;
                case "getConfiguration" -> nullConfiguration();
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Configuration nullConfiguration() {
        return (Configuration) Proxy.newProxyInstance(
            Configuration.class.getClassLoader(),
            new Class<?>[]{Configuration.class},
            (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private static PreparedStatement preparedStatementProxy(AtomicInteger preparedQueryCount) {
        return (PreparedStatement) Proxy.newProxyInstance(
            PreparedStatement.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "setString", "close" -> null;
                case "executeQuery" -> {
                    int index = preparedQueryCount.getAndIncrement();
                    if (index == 0) {
                        yield resultSetFromRows(List.of(Map.of("1", "integration_replay_records_status_check")));
                    }
                    yield resultSetFromRows(List.of());
                }
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Statement statementProxy(List<String> executedSql) {
        return (Statement) Proxy.newProxyInstance(
            Statement.class.getClassLoader(),
            new Class<?>[]{Statement.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "execute" -> {
                    executedSql.add((String) args[0]);
                    yield true;
                }
                case "close" -> null;
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static ResultSet resultSetFromRows(List<? extends Map<String, ?>> rows) {
        AtomicInteger index = new AtomicInteger(-1);
        return (ResultSet) Proxy.newProxyInstance(
            ResultSet.class.getClassLoader(),
            new Class<?>[]{ResultSet.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "next" -> index.incrementAndGet() < rows.size();
                case "getString" -> {
                    Map<String, ?> row = rows.get(index.get());
                    if (args[0] instanceof String columnLabel) {
                        Object value = row.get(columnLabel);
                        yield value == null ? null : String.valueOf(value);
                    }
                    Object value = row.get(String.valueOf(args[0]));
                    yield value == null ? null : String.valueOf(value);
                }
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
}
