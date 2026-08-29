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
 * Gives live Recommendations explicit operational identity and currentness.
 * Ambiguous legacy rows fail migration instead of receiving invented scope.
 */
public class V11__recommendation_identity_scope_currentness extends BaseJavaMigration {

    private static final Pattern TRANSFER_TITLE = Pattern.compile(
        "^Transfer stock for SKU (.+?) from (.+?) to (.+?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVENTORY_TITLE = Pattern.compile(
        "^(?:Urgent reorder|Prepare replenishment|Reorder stock) for SKU (.+?) at (.+?)$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern FULFILLMENT_TITLE = Pattern.compile(
        "^(?:Investigate logistics anomaly in|Escalate delivery risk for|Prioritize fulfillment backlog for) (.+?)$",
        Pattern.CASE_INSENSITIVE);

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        addColumns(connection);
        assertNoOrphanedTenants(connection);
        backfillStructuredIdentity(connection);
        assertIdentityComplete(connection);
        retireHistoricalDuplicates(connection);
        addForeignKeys(connection);
        setRequiredColumns(connection);
        createCurrentConditionIndex(connection);
    }

    private void addColumns(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table recommendations add column if not exists warehouse_id bigint");
            statement.execute("alter table recommendations add column if not exists product_id bigint");
            statement.execute("alter table recommendations add column if not exists source_warehouse_id bigint");
            statement.execute("alter table recommendations add column if not exists destination_warehouse_id bigint");
            statement.execute("alter table recommendations add column if not exists source_type varchar(64)");
            statement.execute("alter table recommendations add column if not exists source_ref varchar(256)");
            statement.execute("alter table recommendations add column if not exists condition_key varchar(320)");
            statement.execute("alter table recommendations add column if not exists status varchar(32)");
            statement.execute("alter table recommendations add column if not exists updated_at timestamp(6) with time zone");
            statement.execute("alter table recommendations add column if not exists suggested_quantity bigint");
        }
    }

    private void assertNoOrphanedTenants(Connection connection) throws SQLException {
        if (count(connection, "select count(*) from recommendations where tenant_id is null") > 0) {
            throw new IllegalStateException(
                "Recommendation identity migration stopped: existing Recommendations without tenant ownership are ambiguous and were not reassigned.");
        }
    }

    private void backfillStructuredIdentity(Connection connection) throws SQLException {
        String select = "select id, tenant_id, type, title, created_at from recommendations "
            + "where warehouse_id is null or source_type is null or source_ref is null "
            + "or condition_key is null or status is null or updated_at is null";
        try (PreparedStatement statement = connection.prepareStatement(select);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                long id = rows.getLong("id");
                long tenantId = rows.getLong("tenant_id");
                String type = rows.getString("type");
                String title = rows.getString("title");
                Identity identity = resolveIdentity(connection, id, tenantId, type, title);
                updateIdentity(connection, id, identity, rows.getTimestamp("created_at"));
            }
        }
    }

    private Identity resolveIdentity(Connection connection,
                                     long recommendationId,
                                     long tenantId,
                                     String type,
                                     String title) throws SQLException {
        String safeTitle = title == null ? "" : title.trim();
        Matcher transfer = TRANSFER_TITLE.matcher(safeTitle);
        Matcher inventory = INVENTORY_TITLE.matcher(safeTitle);
        if (transfer.matches()) {
            ProductWarehouse receiving = findProductWarehouse(connection, tenantId, transfer.group(1), transfer.group(3), recommendationId);
            long sourceWarehouseId = findWarehouse(connection, tenantId, transfer.group(2), recommendationId);
            return new Identity(receiving.productId(), receiving.warehouseId(), sourceWarehouseId, receiving.warehouseId(),
                "INVENTORY", "inventory:" + receiving.productId() + ":" + receiving.warehouseId(),
                "INVENTORY|" + receiving.productId() + "|" + receiving.warehouseId());
        }
        if (inventory.matches()) {
            ProductWarehouse productWarehouse = findProductWarehouse(connection, tenantId, inventory.group(1), inventory.group(2), recommendationId);
            return new Identity(productWarehouse.productId(), productWarehouse.warehouseId(), null, productWarehouse.warehouseId(),
                "INVENTORY", "inventory:" + productWarehouse.productId() + ":" + productWarehouse.warehouseId(),
                "INVENTORY|" + productWarehouse.productId() + "|" + productWarehouse.warehouseId());
        }

        Matcher fulfillment = FULFILLMENT_TITLE.matcher(safeTitle);
        if (fulfillment.matches()) {
            long warehouseId = findWarehouse(connection, tenantId, fulfillment.group(1), recommendationId);
            return new Identity(null, warehouseId, null, warehouseId,
                "FULFILLMENT", "fulfillment:" + warehouseId, "FULFILLMENT|" + warehouseId);
        }
        throw ambiguous(recommendationId,
            "type " + type + " and title do not identify a tenant-owned warehouse/product source");
    }

    private ProductWarehouse findProductWarehouse(Connection connection,
                                                  long tenantId,
                                                  String sku,
                                                  String warehouseCode,
                                                  long recommendationId) throws SQLException {
        String sql = "select p.id as product_id, w.id as warehouse_id "
            + "from products p join inventory i on i.product_id = p.id "
            + "join warehouses w on w.id = i.warehouse_id "
            + "where p.tenant_id = ? and upper(coalesce(p.catalog_sku, p.sku)) = upper(?) "
            + "and w.tenant_id = ? and upper(w.code) = upper(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tenantId);
            statement.setString(2, sku.trim());
            statement.setLong(3, tenantId);
            statement.setString(4, warehouseCode.trim());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw ambiguous(recommendationId, "no tenant-owned inventory identity exists for SKU "
                        + sku.trim() + " and warehouse " + warehouseCode.trim());
                }
                return new ProductWarehouse(row.getLong("product_id"), row.getLong("warehouse_id"));
            }
        }
    }

    private long findWarehouse(Connection connection, long tenantId, String warehouseCode, long recommendationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "select id from warehouses where tenant_id = ? and upper(code) = upper(?)")) {
            statement.setLong(1, tenantId);
            statement.setString(2, warehouseCode.trim());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw ambiguous(recommendationId, "warehouse " + warehouseCode.trim() + " is not owned by the tenant");
                }
                return row.getLong(1);
            }
        }
    }

    private void updateIdentity(Connection connection, long id, Identity identity, java.sql.Timestamp createdAt) throws SQLException {
        String sql = "update recommendations set product_id = ?, warehouse_id = ?, source_warehouse_id = ?, "
            + "destination_warehouse_id = ?, source_type = ?, source_ref = ?, condition_key = ?, status = 'CURRENT', "
            + "updated_at = ? where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (identity.productId() == null) statement.setNull(1, java.sql.Types.BIGINT); else statement.setLong(1, identity.productId());
            statement.setLong(2, identity.warehouseId());
            if (identity.sourceWarehouseId() == null) statement.setNull(3, java.sql.Types.BIGINT); else statement.setLong(3, identity.sourceWarehouseId());
            if (identity.destinationWarehouseId() == null) statement.setNull(4, java.sql.Types.BIGINT); else statement.setLong(4, identity.destinationWarehouseId());
            statement.setString(5, identity.sourceType());
            statement.setString(6, identity.sourceRef());
            statement.setString(7, identity.conditionKey());
            statement.setTimestamp(8, createdAt);
            statement.setLong(9, id);
            statement.executeUpdate();
        }
    }

    private void assertIdentityComplete(Connection connection) throws SQLException {
        String sql = "select count(*) from recommendations where tenant_id is null or warehouse_id is null "
            + "or source_type is null or source_type = '' or source_ref is null or source_ref = '' "
            + "or condition_key is null or condition_key = '' or status is null or updated_at is null "
            + "or (source_type = 'INVENTORY' and product_id is null)";
        if (count(connection, sql) > 0) {
            throw new IllegalStateException("Recommendation identity migration stopped: one or more rows remain structurally ambiguous.");
        }
    }

    private void retireHistoricalDuplicates(Connection connection) throws SQLException {
        execute(connection, "update recommendations older set status = 'RETIRED', updated_at = current_timestamp "
            + "where older.status = 'CURRENT' and exists (select 1 from recommendations newer "
            + "where newer.status = 'CURRENT' and newer.tenant_id = older.tenant_id "
            + "and newer.condition_key = older.condition_key "
            + "and (newer.created_at > older.created_at or (newer.created_at = older.created_at and newer.id > older.id)))");
    }

    private void addForeignKeys(Connection connection) throws SQLException {
        addForeignKey(connection, "fk_recommendations_product", "product_id", "products");
        addForeignKey(connection, "fk_recommendations_warehouse", "warehouse_id", "warehouses");
        addForeignKey(connection, "fk_recommendations_source_warehouse", "source_warehouse_id", "warehouses");
        addForeignKey(connection, "fk_recommendations_destination_warehouse", "destination_warehouse_id", "warehouses");
    }

    private void addForeignKey(Connection connection, String name, String column, String table) throws SQLException {
        if (!constraintExists(connection, name)) {
            execute(connection, "alter table recommendations add constraint " + name + " foreign key (" + column + ") references " + table);
        }
    }

    private void setRequiredColumns(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table recommendations alter column tenant_id set not null");
            statement.execute("alter table recommendations alter column warehouse_id set not null");
            statement.execute("alter table recommendations alter column source_type set not null");
            statement.execute("alter table recommendations alter column source_ref set not null");
            statement.execute("alter table recommendations alter column condition_key set not null");
            statement.execute("alter table recommendations alter column status set not null");
            statement.execute("alter table recommendations alter column updated_at set not null");
        }
    }

    private void createCurrentConditionIndex(Connection connection) throws SQLException {
        if (connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")) {
            execute(connection, "create unique index if not exists ux_recommendations_current_condition "
                + "on recommendations (tenant_id, condition_key) where status = 'CURRENT'");
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

    private IllegalStateException ambiguous(long id, String detail) {
        return new IllegalStateException("Recommendation identity migration stopped for Recommendation " + id + ": " + detail + ".");
    }

    private record ProductWarehouse(long productId, long warehouseId) { }

    private record Identity(Long productId,
                            long warehouseId,
                            Long sourceWarehouseId,
                            Long destinationWarehouseId,
                            String sourceType,
                            String sourceRef,
                            String conditionKey) { }
}
