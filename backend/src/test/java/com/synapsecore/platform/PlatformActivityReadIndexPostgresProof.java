package com.synapsecore.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PlatformActivityReadIndexPostgresProof {

    @Autowired
    private DataSource dataSource;

    @Test
    void migratedAuditIndexIsValidAndSupportsNewestFirstRead() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).containsIgnoringCase("PostgreSQL");
            try (ResultSet result = statement.executeQuery("""
                select i.indisvalid
                from pg_class c join pg_index i on i.indexrelid = c.oid
                where c.relname = 'idx_audit_logs_created_at_desc'
                  and c.relnamespace = (select oid from pg_namespace where nspname = current_schema())
                """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBoolean(1)).isTrue();
            }

            statement.execute("set enable_seqscan = off");
            try (ResultSet plan = statement.executeQuery("""
                explain select id, action, actor, created_at, details, request_id,
                    source, status, target_ref, target_type, tenant_code
                from audit_logs order by created_at desc fetch first 20 rows only
                """)) {
                StringBuilder lines = new StringBuilder();
                while (plan.next()) {
                    lines.append(plan.getString(1)).append('\n');
                }
                assertThat(lines.toString()).contains("idx_audit_logs_created_at_desc");
            } finally {
                statement.execute("reset enable_seqscan");
            }
        }
    }
}
