package db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class V6PayloadColumnTypeAlignmentTest {

    @Test
    void oidPayloadColumnsUsePostgresqlCatalogAndLargeObjectCopyStrategy() throws Exception {
        V6__payload_column_type_alignment migration = new V6__payload_column_type_alignment();
        AtomicReference<String> columnTypeLookupSql = new AtomicReference<>();
        List<String> executedSql = new ArrayList<>();
        List<Long> unlinkedObjectIds = new ArrayList<>();

        DatabaseMetaData metadata = metadataProxy(
            List.of("integration_inbound_records"),
            List.of(
                Map.of("TABLE_NAME", "integration_inbound_records", "COLUMN_NAME", "request_payload")
            )
        );

        Connection connection = (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getMetaData" -> metadata;
                case "prepareStatement" -> preparedStatementProxy(
                    (String) args[0],
                    columnTypeLookupSql,
                    unlinkedObjectIds
                );
                case "createStatement" -> statementProxy(executedSql);
                case "setSavepoint" -> savepoint("v6");
                case "rollback", "releaseSavepoint", "close", "commit" -> null;
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            }
        );

        migration.alignPayloadColumn(connection, "integration_inbound_records", "request_payload", "text", true);

        assertThat(columnTypeLookupSql.get()).contains("pg_catalog.format_type");
        assertThat(columnTypeLookupSql.get()).doesNotContain("information_schema.indexes");
        assertThat(executedSql).anySatisfy(sql -> assertThat(sql)
            .contains("alter table integration_inbound_records add column request_payload_v6_migrated text"));
        assertThat(executedSql).anySatisfy(sql -> assertThat(sql)
            .contains("convert_from(pg_catalog.lo_get(request_payload), current_setting('server_encoding'))"));
        assertThat(executedSql).anySatisfy(sql -> assertThat(sql)
            .contains("alter table integration_inbound_records drop column request_payload"));
        assertThat(executedSql).anySatisfy(sql -> assertThat(sql)
            .contains("alter table integration_inbound_records rename column request_payload_v6_migrated to request_payload"));
        assertThat(executedSql).anySatisfy(sql -> assertThat(sql)
            .contains("alter table integration_inbound_records alter column request_payload set not null"));
        assertThat(unlinkedObjectIds).containsExactly(41L);
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

    private static PreparedStatement preparedStatementProxy(String sql,
                                                           AtomicReference<String> columnTypeLookupSql,
                                                           List<Long> unlinkedObjectIds) {
        AtomicReference<Object[]> parameters = new AtomicReference<>(new Object[4]);
        return (PreparedStatement) Proxy.newProxyInstance(
            PreparedStatement.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "setString", "setLong" -> {
                    Object[] current = parameters.get();
                    current[(Integer) args[0] - 1] = args[1];
                    yield null;
                }
                case "executeQuery" -> {
                    if (sql.contains("pg_catalog.format_type")) {
                        columnTypeLookupSql.set(sql);
                        yield resultSetFromRows(List.of(Map.of("1", "oid")));
                    }
                    yield resultSetFromRows(List.of());
                }
                case "execute" -> {
                    if (sql.contains("pg_catalog.lo_unlink")) {
                        Object[] current = parameters.get();
                        unlinkedObjectIds.add(((Number) current[0]).longValue());
                    }
                    yield true;
                }
                case "close" -> null;
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
                case "executeQuery" -> {
                    String sql = (String) args[0];
                    if (sql.contains("pg_largeobject_metadata")) {
                        yield resultSetFromRows(List.of(Map.of("1", 41L)));
                    }
                    yield resultSetFromRows(List.of());
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
                case "getLong" -> {
                    Object value = rows.get(index.get()).get(String.valueOf(args[0]));
                    yield value == null ? 0L : ((Number) value).longValue();
                }
                case "close" -> null;
                case "wasNull" -> false;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Savepoint savepoint(String name) {
        return new Savepoint() {
            @Override
            public int getSavepointId() {
                return 0;
            }

            @Override
            public String getSavepointName() {
                return name;
            }
        };
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
