import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Locale;
import java.util.Properties;

public class MeasurePlatformAuditSql {
    private static final String AUDIT_SELECT = """
        explain (analyze, buffers)
        select id, action, actor, created_at, details, request_id,
               source, status, target_ref, target_type, tenant_code
        from audit_logs order by created_at desc fetch first 20 rows only
        """;

    public static void main(String[] args) throws Exception {
        try {
            int samples = Integer.parseInt(args[0]);
            if (samples < 1 || samples > 10) {
                throw new IllegalArgumentException("Sample count must be between 1 and 10.");
            }
            String secretUrl = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
            URI uri = URI.create(secretUrl);
            String userInfo = uri.getUserInfo();
            if (uri.getHost() == null || !uri.getHost().toLowerCase(Locale.ROOT).endsWith(".render.com")
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
                    statement.setQueryTimeout(5);
                    statement.execute("set transaction read only");
                    statement.execute("set local statement_timeout = '5000ms'");
                    try (ResultSet metadata = statement.executeQuery("""
                        select pg_backend_pid(), coalesce(bool_or(i.indisvalid), false)
                        from pg_class c join pg_index i on i.indexrelid = c.oid
                        where c.relname = 'idx_audit_logs_created_at_desc'
                          and c.relnamespace = (select oid from pg_namespace where nspname = current_schema())
                        """)) {
                        metadata.next();
                        System.out.println("UTC=" + Instant.now() + " POSTGRES_PID=" + metadata.getInt(1)
                            + " AUDIT_INDEX_VALID=" + metadata.getBoolean(2));
                        if (!metadata.getBoolean(2)) {
                            throw new IllegalStateException("V15 audit-order index is absent or invalid; no timing was taken.");
                        }
                    }
                    for (int sample = 0; sample <= samples; sample++) {
                        System.out.println(sample == 0 ? "WARMUP" : "SAMPLE_" + sample);
                        try (ResultSet plan = statement.executeQuery(AUDIT_SELECT)) {
                            while (plan.next()) {
                                System.out.println(plan.getString(1));
                            }
                        }
                    }
                } finally {
                    connection.rollback();
                }
            }
        } catch (SQLException exception) {
            System.err.println("PostgreSQL measurement failed; SQLState=" + exception.getSQLState()
                + ". No database URL or row data was printed.");
            System.exit(1);
        } catch (IllegalStateException exception) {
            System.err.println("Measurement stopped: " + exception.getMessage());
            System.exit(1);
        } catch (RuntimeException exception) {
            System.err.println("Measurement stopped: invalid input or local runtime failure. No URL was printed.");
            System.exit(1);
        }
    }
}
