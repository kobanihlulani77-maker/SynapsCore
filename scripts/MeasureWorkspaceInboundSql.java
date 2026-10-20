import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

public class MeasureWorkspaceInboundSql {
    private static final int SOURCE_BATCH_SIZE = 200;
    private static final String INBOUND_QUERY = """
        with ranked as (
            select lower(source_system) as source_key, connector_type, created_at, updated_at,
                status, failure_code, failure_message,
                row_number() over (partition by lower(source_system), connector_type
                    order by created_at desc, id desc) as latest_rank,
                row_number() over (partition by lower(source_system), connector_type
                    order by case when status in ('ACCEPTED', 'REPLAYED') then created_at end desc nulls last,
                        id desc) as success_rank,
                row_number() over (partition by lower(source_system), connector_type
                    order by case when status in ('REJECTED', 'REPLAY_QUEUED') then updated_at end desc nulls last,
                        id desc) as failure_rank,
                sum(case when status in ('REJECTED', 'REPLAY_QUEUED') and created_at > ?
                    then 1 else 0 end) over (partition by lower(source_system), connector_type) as failure_count
            from integration_inbound_records
            where lower(tenant_code) = lower(?) and lower(source_system) in (%s)
        )
        select * from ranked where latest_rank = 1 or success_rank = 1 or failure_rank = 1
        """;

    public static void main(String[] args) throws Exception {
        try {
            if (args.length != 1 || !args[0].matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")) {
                throw new IllegalArgumentException("Expected one tenant code.");
            }
            String tenantCode = args[0];
            String secretUrl = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
            URI uri = URI.create(secretUrl);
            String userInfo = uri.getUserInfo();
            if (!List.of("postgres", "postgresql").contains(uri.getScheme())
                    || uri.getHost() == null || !uri.getHost().toLowerCase(Locale.ROOT).endsWith(".render.com")
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || userInfo == null || !userInfo.contains(":")) {
                throw new IllegalArgumentException("Expected a Render External PostgreSQL URL without query parameters.");
            }
            String[] credentials = userInfo.split(":", 2);
            String database = uri.getPath().substring(1);
            if (database.isBlank() || database.contains("/")) {
                throw new IllegalArgumentException("Expected one database name in the URL path.");
            }
            int port = uri.getPort() < 0 ? 5432 : uri.getPort();
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + database;
            Properties properties = new Properties();
            properties.setProperty("user", credentials[0]);
            properties.setProperty("password", credentials[1]);
            properties.setProperty("sslmode", "require");
            properties.setProperty("connectTimeout", "5");
            properties.setProperty("socketTimeout", "8");

            try (Connection connection = DriverManager.getConnection(jdbcUrl, properties)) {
                connection.setReadOnly(true);
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    statement.setQueryTimeout(3);
                    statement.execute("set transaction read only");
                    statement.execute("set local statement_timeout = '3000ms'");
                    List<String> sources = new ArrayList<>();
                    try (PreparedStatement sourceQuery = connection.prepareStatement("""
                        select distinct lower(c.source_system)
                        from integration_connectors c join tenants t on t.id = c.tenant_id
                        where lower(t.code) = lower(?)
                        order by 1 limit 201
                        """)) {
                        sourceQuery.setQueryTimeout(3);
                        sourceQuery.setString(1, tenantCode);
                        try (ResultSet rows = sourceQuery.executeQuery()) {
                            while (rows.next()) {
                                sources.add(rows.getString(1));
                            }
                        }
                    }
                    if (sources.isEmpty() || sources.size() > SOURCE_BATCH_SIZE) {
                        throw new IllegalStateException("Expected 1-200 distinct connector sources; no plan was run.");
                    }
                    String markers = sources.stream().map(ignored -> "?").collect(Collectors.joining(","));
                    String query = "explain (analyze, buffers, timing on) " + INBOUND_QUERY.formatted(markers);
                    try (PreparedStatement planQuery = connection.prepareStatement(query)) {
                        planQuery.setQueryTimeout(3);
                        planQuery.setTimestamp(1, Timestamp.from(Instant.now().minus(24, ChronoUnit.HOURS)));
                        planQuery.setString(2, tenantCode);
                        for (int index = 0; index < sources.size(); index++) {
                            planQuery.setString(index + 3, sources.get(index));
                        }
                        System.out.println("UTC=" + Instant.now() + " POSTGRES_PID=" + backendPid(connection)
                            + " SOURCE_COUNT=" + sources.size() + " QUERY=inbound WINDOW_HOURS=24 TIMEOUT_MS=3000");
                        try (ResultSet plan = planQuery.executeQuery()) {
                            while (plan.next()) {
                                System.out.println(plan.getString(1).replaceAll("'([^']|'')*'", "'[redacted]'"));
                            }
                        }
                    }
                } finally {
                    connection.rollback();
                }
            }
        } catch (SQLException exception) {
            System.err.println("PostgreSQL plan stopped; SQLState=" + exception.getSQLState()
                + ". No URL or row data was printed.");
            System.exit(1);
        } catch (RuntimeException exception) {
            System.err.println("Measurement stopped: invalid input, unsupported tenant size, or local runtime failure. No URL was printed.");
            System.exit(1);
        }
    }

    private static int backendPid(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("select pg_backend_pid()")) {
            row.next();
            return row.getInt(1);
        }
    }
}
