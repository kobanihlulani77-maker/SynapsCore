package db.migration;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Adds durable governance requirements and optimistic versions for admin records.
 */
public class V12__tenant_admin_authority_safety extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table access_operators add column if not exists version bigint not null default 0");
            statement.execute("alter table access_users add column if not exists version bigint not null default 0");
            statement.execute("""
                create table if not exists tenant_required_roles (
                    tenant_id bigint not null,
                    role_code varchar(32) not null,
                    primary key (tenant_id, role_code),
                    constraint ck_tenant_required_role_code check (role_code in (
                        'TENANT_ADMIN','REVIEW_OWNER','FINAL_APPROVER','ESCALATION_OWNER',
                        'INTEGRATION_ADMIN','INTEGRATION_OPERATOR'))
                )
                """);
            statement.execute("""
                do $$
                begin
                    if not exists (
                        select 1 from information_schema.table_constraints
                        where lower(constraint_name) = 'fk_tenant_required_roles_tenant'
                    ) then
                        alter table tenant_required_roles
                            add constraint fk_tenant_required_roles_tenant foreign key (tenant_id) references tenants;
                    end if;
                end $$
                """);
        }
    }
}
