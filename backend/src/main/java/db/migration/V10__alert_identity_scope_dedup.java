package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Adds structured Alert identity and prevents duplicate active conditions.
 * Ambiguous legacy rows fail migration instead of receiving an invented owner.
 */
public class V10__alert_identity_scope_dedup extends BaseJavaMigration {

    private static final Pattern INVENTORY_TITLE = Pattern.compile(
        "^(?:Low stock detected for SKU|Depletion risk rising for SKU)\\s+(.+)\\s+in\\s+(.+)$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern FULFILLMENT_TITLE = Pattern.compile(
        "^(?:Fulfillment backlog building|Delivery delay risk rising|Logistics anomaly detected)\\s+in\\s+(.+)$",
        Pattern.CASE_INSENSITIVE);

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")) {
            return;
        }

        addColumns(connection);
        assertNoOrphanedTenants(connection);
        backfillStructuredIdentity(connection);
        assertIdentityComplete(connection);
        assertNoActiveDuplicates(connection);
        addForeignKeys(connection);
        setRequiredColumns(connection);
        createActiveConditionIndex(connection);
    }

    private void addColumns(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table alerts add column if not exists warehouse_id bigint");
            statement.execute("alter table alerts add column if not exists product_id bigint");
            statement.execute("alter table alerts add column if not exists source_type varchar(64)");
            statement.execute("alter table alerts add column if not exists source_ref varchar(256)");
            statement.execute("alter table alerts add column if not exists condition_key varchar(320)");
        }
    }

    private void assertNoOrphanedTenants(Connection connection) throws SQLException {
        if (count(connection, "select count(*) from alerts where tenant_id is null") > 0) {
            throw new IllegalStateException(
                "Alert identity migration stopped: existing Alerts without tenant ownership are ambiguous and were not reassigned.");
        }
    }

    private void backfillStructuredIdentity(Connection connection) throws SQLException {
        String select = "select id, tenant_id, type, title from alerts "
            + "where warehouse_id is null or source_type is null or source_ref is null or condition_key is null";
        try (PreparedStatement statement = connection.prepareStatement(select);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                long alertId = rows.getLong("id");
                long tenantId = rows.getLong("tenant_id");
                String type = rows.getString("type");
                String title = rows.getString("title");
                if (isInventoryType(type)) {
                    Matcher matcher = INVENTORY_TITLE.matcher(title == null ? "" : title);
                    if (!matcher.matches()) {
                        throw ambiguous(alertId, "inventory title does not identify a product and warehouse");
                    }
                    Identity identity = findInventoryIdentity(connection, tenantId, matcher.group(1), matcher.group(2));
                    updateIdentity(connection, alertId, identity.productId(), identity.warehouseId(),
                        "INVENTORY_PRODUCT_WAREHOUSE", identity.productId() + "@" + identity.warehouseId(),
                        type + "|PRODUCT:" + identity.productId() + "|WAREHOUSE:" + identity.warehouseId());
                } else {
                    Matcher matcher = FULFILLMENT_TITLE.matcher(title == null ? "" : title);
                    if (!matcher.matches()) {
                        throw ambiguous(alertId, "fulfillment title does not identify a warehouse");
                    }
                    long warehouseId = findWarehouseId(connection, tenantId, matcher.group(1), alertId);
                    updateIdentity(connection, alertId, null, warehouseId,
                        "FULFILLMENT_WAREHOUSE", matcher.group(1).trim(), type + "|WAREHOUSE:" + warehouseId);
                }
            }
        }
    }

    private Identity findInventoryIdentity(Connection connection,
                                           long tenantId,
                                           String productSku,
                                           String warehouseCode) throws SQLException {
        String sql = "select p.id as product_id, w.id as warehouse_id "
            + "from products p join inventory i on i.product_id = p.id "
            + "join warehouses w on w.id = i.warehouse_id "
            + "where p.tenant_id = ? and upper(coalesce(p.catalog_sku, p.sku)) = upper(?) "
            + "and w.tenant_id = ? and upper(w.code) = upper(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tenantId);
            statement.setString(2, productSku.trim());
            statement.setLong(3, tenantId);
            statement.setString(4, warehouseCode.trim());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw ambiguous(0, "no tenant-owned inventory identity exists for SKU "
                        + productSku.trim() + " and warehouse " + warehouseCode.trim());
                }
                return new Identity(row.getLong("product_id"), row.getLong("warehouse_id"));
            }
        }
    }

    private long findWarehouseId(Connection connection,
                                 long tenantId,
                                 String warehouseCode,
                                 long alertId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "select id from warehouses where tenant_id = ? and upper(code) = upper(?)")) {
            statement.setLong(1, tenantId);
            statement.setString(2, warehouseCode.trim());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw ambiguous(alertId, "warehouse " + warehouseCode.trim() + " is not owned by the Alert tenant");
                }
                return row.getLong("id");
            }
        }
    }

    private void updateIdentity(Connection connection,
                                long alertId,
                                Long productId,
                                long warehouseId,
                                String sourceType,
                                String sourceRef,
                                String conditionKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "update alerts set product_id = ?, warehouse_id = ?, source_type = ?, source_ref = ?, condition_key = ? where id = ?")) {
            if (productId == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, productId);
            }
            statement.setLong(2, warehouseId);
            statement.setString(3, sourceType);
            statement.setString(4, sourceRef);
            statement.setString(5, conditionKey);
            statement.setLong(6, alertId);
            statement.executeUpdate();
        }
    }

    private void assertIdentityComplete(Connection connection) throws SQLException {
        String sql = "select count(*) from alerts where tenant_id is null or warehouse_id is null "
            + "or source_type is null or source_type = '' or source_ref is null or source_ref = '' "
            + "or condition_key is null or condition_key = '' "
            + "or (type in ('LOW_STOCK', 'DEPLETION_RISK') and product_id is null)";
        if (count(connection, sql) > 0) {
            throw new IllegalStateException("Alert identity migration stopped: one or more rows remain structurally ambiguous.");
        }
    }

    private void assertNoActiveDuplicates(Connection connection) throws SQLException {
        String sql = "select tenant_id, condition_key, count(*) from alerts where status = 'ACTIVE' "
            + "group by tenant_id, condition_key having count(*) > 1";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            if (rows.next()) {
                throw new IllegalStateException("Alert identity migration stopped: duplicate ACTIVE condition_key exists for tenant "
                    + rows.getLong(1) + ". Resolve the duplicate operational records before migration.");
            }
        }
    }

    private void addForeignKeys(Connection connection) throws SQLException {
        if (!constraintExists(connection, "fk_alerts_product")) {
            execute(connection, "alter table alerts add constraint fk_alerts_product foreign key (product_id) references products");
        }
        if (!constraintExists(connection, "fk_alerts_warehouse")) {
            execute(connection, "alter table alerts add constraint fk_alerts_warehouse foreign key (warehouse_id) references warehouses");
        }
    }

    private void setRequiredColumns(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table alerts alter column tenant_id set not null");
            statement.execute("alter table alerts alter column warehouse_id set not null");
            statement.execute("alter table alerts alter column source_type set not null");
            statement.execute("alter table alerts alter column source_ref set not null");
            statement.execute("alter table alerts alter column condition_key set not null");
        }
    }

    private void createActiveConditionIndex(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("create unique index if not exists ux_alerts_active_condition "
                + "on alerts (tenant_id, condition_key) where status = 'ACTIVE'");
        }
    }

    private boolean constraintExists(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "select count(*) from information_schema.table_constraints where lower(constraint_name) = lower(?)")) {
            statement.setString(1, name);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() && row.getLong(1) > 0;
            }
        }
    }

    private long count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet row = statement.executeQuery(sql)) {
            row.next();
            return row.getLong(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean isInventoryType(String type) {
        return "LOW_STOCK".equals(type) || "DEPLETION_RISK".equals(type);
    }

    private IllegalStateException ambiguous(long alertId, String detail) {
        return new IllegalStateException("Alert identity migration stopped for Alert " + alertId + ": " + detail + ".");
    }

    private record Identity(long productId, long warehouseId) {
    }
}
