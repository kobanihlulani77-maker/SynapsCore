# Company Data Onboarding Runbook

This is the internal SynapseCore Phase 6 runbook for onboarding approved Company 1 pilot data into an already-created tenant with verified users and one verified integration lane.

Phase 6 is a controlled data movement phase. It is not alert configuration, recommendation tuning, scenario governance, customer handover, or a generic ETL build.

## Phase Boundary

Phase 6 includes:

- documenting the actual catalog, inventory, warehouse, order, and order-item models
- choosing the official onboarding path per data domain
- mapping approved Company 1 fields to SynapseCore fields
- validating data before it enters the pilot tenant
- loading synthetic, representative, controlled test, and bounded live pilot data in stages
- reconciling source counts and identifiers against SynapseCore readback
- recording import evidence without storing customer payloads in Git
- defining correction, reimport, rollback, and stop conditions
- handing verified data state to Phase 7 operational configuration

Phase 6 does not include:

- configuring alert policies
- configuring recommendations
- configuring scenarios or approval governance
- starting pre-handover verification
- handing customer access over
- loading full production history
- creating customer self-service import infrastructure
- manually editing database rows

## Current Data Model Summary

| Domain | Entity | Table | Tenant boundary | Official Phase 6 role |
| --- | --- | --- | --- | --- |
| Catalog | `Product` | `products` | `tenant_id`; visible SKU is `catalogSku`; stored SKU is internal `TENANT::SKU` | Product identity foundation. |
| Warehouse/location | `Warehouse` | `warehouses` | `tenant_id`; unique tenant + code | Location/site identity foundation. |
| Inventory | `Inventory` | `inventory` | derived from warehouse tenant; product and warehouse must match tenant | Product + warehouse stock baseline. |
| Orders | `CustomerOrder` | `customer_orders` | `tenant_id`; unique tenant + external order id | Live operational order records. |
| Order items | `OrderItem` | `order_items` | synchronized from customer order tenant | Product lines inside an order. |
| Integration inbound | `IntegrationInboundRecord` | `integration_inbound_records` | tenant code field | Failed/accepted inbound evidence. |
| Replay | `IntegrationReplayRecord` | `integration_replay_records` | tenant code field | Recovery queue and replay evidence. |

## Product / Catalog Model

In current SynapseCore, a product is a tenant-owned catalog identity used by inventory, order items, scenarios, alerts, recommendations, and integration validation.

| Field | Type/constraint | Classification | Notes |
| --- | --- | --- | --- |
| `id` | generated `Long` | System-generated | Internal primary key. |
| `tenant` | many-to-one `Tenant` | System-controlled | Must be explicit; products cannot persist without tenant. |
| `sku` | unique, length 128 | System-controlled internal identifier | Persisted as `TENANT_CODE::CATALOG_SKU`. |
| `catalogSku` | length 64 | Required for creation | Tenant-visible SKU; normalized uppercase. |
| `name` | required, length 120 | Required for creation | Product display name. |
| `category` | required, length 120 | Required for creation | Product category/group. |
| `createdAt` | `Instant` | System-generated | Set on create. |
| `updatedAt` | `Instant` | System-generated | Updated on create/update. |
| `inventoryRecords` | one-to-many | System relationship | Inventory records referencing the product. |
| `orderItems` | one-to-many | System relationship | Order items referencing the product. |

Current SKU rules:

- SKU is required.
- SKU is trimmed and uppercased.
- Maximum tenant-visible SKU length is 64.
- SKU must start with a letter or number.
- SKU may contain letters, numbers, dots, underscores, and hyphens.
- SKU uniqueness is tenant-scoped through `catalogSku`.
- Internal `sku` is globally unique because it includes the tenant prefix.
- Catalog import rejects duplicate SKUs inside the same file.

No current product fields exist for:

- price
- cost
- unit of measure
- status
- external supplier id
- product description
- barcode
- customer-specific attributes

Do not onboard those fields into SynapseCore during Phase 6 unless a future product change adds supported fields.

## Inventory Model

Inventory is one record per product + warehouse pair.

| Field | Type/constraint | Classification | Notes |
| --- | --- | --- | --- |
| `id` | generated `Long` | System-generated | Internal primary key. |
| `tenant` | many-to-one `Tenant` | System-controlled | Synchronized from warehouse tenant. |
| `product` | required many-to-one `Product` | Required relationship | Product must already exist in same tenant. |
| `warehouse` | required many-to-one `Warehouse` | Required relationship | Warehouse must already exist in same tenant. |
| `quantityAvailable` | required `Long` | System-derived from on-hand minus reserved | Set by stock synchronization. |
| `quantityOnHand` | `Long`, default 0 | Required for baseline through update API | Normalized non-negative. |
| `quantityReserved` | `Long`, default 0 | System-managed by orders/fulfillment | Normalized non-negative. |
| `quantityInbound` | `Long`, default 0 | System-managed field | Normalized non-negative; no bulk onboarding path today. |
| `reorderThreshold` | required `Long` | Required for baseline | Normalized non-negative. |
| `lastReceivedAt` | `Instant` | System-generated by receipt | Optional/system-managed. |
| `lastAdjustedAt` | `Instant` | System-generated by adjustment | Optional/system-managed. |
| `lastReconciledAt` | `Instant` | System-generated by reconciliation | Optional/system-managed. |
| `reconciliationVariance` | `Long` | System-generated by reconciliation | Optional/system-managed. |
| `updatedAt` | `Instant` | System-generated | Updated on create/update. |

Uniqueness:

- one inventory row per product + warehouse (`uk_inventory_product_warehouse`)

Important inventory behavior:

- `POST /api/inventory/update` can create or update the baseline row.
- `POST /api/inventory/receive`, `/adjust`, and `/reconcile` require the row to already exist.
- Negative requested quantities are rejected by DTO validation for update/receive/reconcile.
- Negative adjustments are allowed only if on-hand would not fall below reserved commitments.
- Inventory readback includes risk fields from intelligence/prediction services.
- Inventory adjustments remain operationally sensitive; during the pilot, the company's stock-control source remains the source of truth unless the pilot explicitly approves otherwise.

## Warehouse / Location Model

Current SynapseCore represents operational locations through `Warehouse`.

| Field | Type/constraint | Classification | Notes |
| --- | --- | --- | --- |
| `id` | generated `Long` | System-generated | Internal primary key. |
| `tenant` | many-to-one `Tenant` | System-controlled | Warehouse belongs to one tenant. |
| `code` | required length 40 | Required identity | Unique per tenant. |
| `name` | required length 120 | Required | Display name. |
| `location` | required length 120 | Required | Human-readable location/site. |
| `createdAt` | `Instant` | System-generated | Set on create. |
| `updatedAt` | `Instant` | System-generated | Updated on create/update. |

Phase 3 creates the initial company warehouse lanes through tenant onboarding. Phase 6 may map Company 1 source locations to those existing warehouse codes, but it must not create random new location identities from spelling differences.

## Order Model

In current SynapseCore, an order is a tenant-owned live operational record with a warehouse and one or more product-backed line items.

| Field | Type/constraint | Classification | Notes |
| --- | --- | --- | --- |
| `id` | generated `Long` | System-generated | Internal primary key. |
| `tenant` | many-to-one `Tenant` | System-controlled | Tenant must match warehouse tenant. |
| `externalOrderId` | required length 80 | Required or generated | Preserves source order identity when supplied. |
| `status` | `OrderStatus` | System-managed | Created as `CREATED`, immediately moved to `RECEIVED` on create. |
| `statusReason` | length 320 | System-managed | Explains latest transition/source. |
| `totalAmount` | decimal 14,2 | System-calculated | Sum of quantity x unit price. |
| `warehouse` | required many-to-one `Warehouse` | Required | Must belong to tenant. |
| `createdAt` | `Instant` | System-generated | Set on create. |
| `updatedAt` | `Instant` | System-generated | Updated on change. |
| lifecycle timestamps | `Instant` fields | System-generated | Set during status transitions. |
| `items` | one-to-many `OrderItem` | Required relationship | Cascade persisted with order. |

`OrderItem` fields:

| Field | Type/constraint | Classification | Notes |
| --- | --- | --- | --- |
| `id` | generated `Long` | System-generated | Internal primary key. |
| `tenant` | many-to-one `Tenant` | System-controlled | Synchronized from customer order. |
| `customerOrder` | required many-to-one | Required relationship | Parent order. |
| `product` | required many-to-one | Required relationship | Product must already exist. |
| `quantity` | required integer | Required | Minimum 1 on request. |
| `reservedQuantity` | integer default 0 | System-managed | Set during order creation/reservation. |
| `fulfilledQuantity` | integer default 0 | System-managed | Updated during fulfillment lifecycle. |
| `cancelledQuantity` | integer default 0 | System-managed | Updated during cancellation. |
| `returnedQuantity` | integer default 0 | System-managed | Updated during return. |
| `unitPrice` | decimal 14,2 | Required | Minimum `0.01`; max 12 integer digits and 2 decimals on API. |

Order statuses:

- `CREATED`
- `RECEIVED`
- `PROCESSING`
- `PARTIALLY_FULFILLED`
- `FULFILLED`
- `DELIVERED`
- `CANCELLED`
- `RETURNED`
- `FAILED`
- `BLOCKED`

Idempotency:

- `customer_orders` has a unique constraint on tenant + `external_order_id`.
- `OrderService` checks `existsByTenant_CodeIgnoreCaseAndExternalOrderId`.
- Duplicate external order id in the same tenant returns conflict.
- Duplicate prevention is tenant-scoped, not connector-scoped.
- The same external order id must not be reused across Company 1 connector lanes unless the intent is to prove duplicate rejection.

## Domain Relationships And Creation Dependencies

```text
Tenant
  -> Warehouse
  -> Product
Product + Warehouse
  -> Inventory
Product + Warehouse + Inventory
  -> Order + OrderItem
Connector
  -> Inbound order
  -> OrderService
  -> Failed inbound / replay if order creation fails
Failed inbound
  -> Replay
  -> OrderService
```

Dependencies:

- Tenant must exist before anything else.
- Users/roles must exist before operator-managed onboarding.
- Warehouses must exist before inventory and orders.
- Products must exist before inventory and order items.
- Inventory must exist with sufficient available quantity before order creation can reserve stock.
- Connector lane must exist before connector-authenticated order ingestion.
- Failed inbound replay depends on the original connector, source system, warehouse, product, and inventory prerequisites becoming valid.

## Onboarding Mechanisms Found

### Catalog

| Mechanism | Classification | Notes |
| --- | --- | --- |
| `POST /api/products` | Supported for Company pilot | Tenant admin creates one product. |
| `PUT /api/products/{productId}` | Supported for Company pilot | Tenant admin updates SKU/name/category. |
| `POST /api/products/import` | Supported for Company pilot | Tenant admin uploads CSV with `sku`, `name`, `category`. |
| Catalog UI | Supported for Company pilot | Uses the API paths above. |
| Proof prep scripts | Proof/test only | Creates proof baseline, not Company 1 data. |
| Seed tooling | Seed only | Starter/dev data only. |
| Direct SQL | Unsafe | Do not use. |

### Inventory

| Mechanism | Classification | Notes |
| --- | --- | --- |
| `POST /api/inventory/update` | Supported but manual | Can create/update product + warehouse baseline. Official Phase 6 path for bounded pilot inventory. |
| `POST /api/inventory/receive` | Supported operational action | Requires existing inventory row. |
| `POST /api/inventory/adjust` | Supported operational action | Requires existing row and reason. |
| `POST /api/inventory/reconcile` | Supported operational action | Requires existing row. |
| Inventory UI | Readback/review only | No current manual edit form in UI. |
| Proof/readiness scripts | Proof/test only | Good rehearsal; not Company 1 data load. |
| Seed tooling | Seed only | Starter/dev data only. |
| Direct SQL | Unsafe | Do not use. |

### Orders

| Mechanism | Classification | Notes |
| --- | --- | --- |
| `POST /api/orders` | Supported for Company pilot | Direct signed-in order creation; requires warehouse access. |
| `POST /api/orders/{externalOrderId}/transition` | Supported lifecycle operation | Not an import path; use only for supported status changes. |
| `POST /api/integrations/orders/webhook` | Supported for Company pilot | Official order path if Phase 5 connector lane is webhook. |
| `POST /api/integrations/orders/csv-import` | Supported for Company pilot | Official order path if Phase 5 connector lane is CSV order import. |
| Scheduled pull | Supported with limitation | `WEBHOOK_ORDER` only and no outbound credential model. |
| Replay | Supported recovery path | Reprocesses failed inbound through `OrderService`. |
| Scenario execution | Phase 7/operations, not Phase 6 import | Do not use for onboarding Company 1 order data. |
| Direct SQL | Unsafe | Do not use. |

## Official Phase 6 Method Per Domain

| Domain | Official method | Reason |
| --- | --- | --- |
| Catalog | Product CSV import through `/api/products/import`, or single product API/UI for very small corrections | It is tenant-scoped, row-level, audited, and reports created/updated/failed rows. |
| Inventory | Repeated bounded calls to `POST /api/inventory/update` after catalog and warehouse validation | No inventory CSV exists today; update API is supported and can create baseline rows safely. |
| Orders | Use the Phase 5 verified connector lane, preferring webhook/CSV according to approved Company 1 source method; use direct `/api/orders` only for operator-created test orders | Connector path preserves inbound/replay evidence; direct API is valid but less representative of Company 1 source-system flow. |

Internal import tooling required: **NO** for the current bounded Company 1 pilot envelope.

Reason:

- catalog has supported CSV import
- inventory has supported baseline update API
- orders have supported direct and connector ingestion paths
- Company 1 scope is bounded, not full migration

If Company 1 inventory volume becomes too large for repeated API calls, document it as "larger than tested - review required" before building any internal helper.

## Phase 2 Data Inputs Required

Before onboarding, confirm for each domain:

| Domain | Required Phase 2 input |
| --- | --- |
| Catalog | in-scope product SKUs, product names, categories, source-system owner, approved count, sample file, authorization. |
| Inventory | source inventory owner, warehouse/location mapping, SKU mapping, quantity semantics, reorder threshold rule, approved count, update frequency. |
| Orders | source system, connector type, external order id field, order item fields, warehouse code field, unit price availability, approved sample/live count. |

Out of scope unless explicitly approved:

- customer PII
- payment details
- full historical order archive
- confidential source-system metadata
- unsupported product attributes
- unsupported inventory fields
- unsupported order statuses

## Data Minimization Rules

Onboard only fields that SynapseCore currently stores and the pilot objective requires.

| Domain | Required minimum |
| --- | --- |
| Catalog | SKU, name, category |
| Inventory | product SKU, warehouse code, available quantity, reorder threshold |
| Order webhook | source system, external order id, warehouse code, items with SKU/quantity/unit price |
| Order CSV | source system, external order id, warehouse code, product SKU, quantity, unit price |

If a source file contains extra fields, exclude them from the import file or payload. Do not store unused customer data "just in case."

## Sample To Controlled Live Stages

1. Synthetic/redacted sample
2. Representative customer sample
3. Controlled test import
4. Reconcile and correct
5. Approved bounded live pilot data

Do not start with full production data.

## File And Payload Handling

Rules:

- Never commit customer data to Git.
- Never place customer files in `docs`, source folders, test fixtures, screenshots, Playwright reports, or generated public artifacts.
- Use an approved local working directory outside the repository, for example a private operator folder such as `C:\SynapseCorePilotData\Company1\`.
- Keep temporary transformed CSV files out of the repo.
- Delete temporary files after reconciliation unless retention is explicitly approved.
- Store only counts, hashes, identifiers, and redacted evidence in repository templates.
- If screenshots are needed, crop/redact payloads and secrets.

## Catalog Mapping Model

| Customer source field | SynapseCore field | Required? | Transformation | Validation | Synthetic example |
| --- | --- | --- | --- | --- | --- |
| product code / SKU | `catalogSku` through request `sku` | Yes | trim, uppercase | max 64; starts with letter/number; letters/numbers/dot/underscore/hyphen | `SKU-SAMPLE-001` |
| product name | `name` | Yes | trim | max 120, nonblank | `Sample Valve` |
| category / product family | `category` | Yes | trim | max 120, nonblank | `Industrial Parts` |
| unsupported fields | none | No | exclude | not imported | not stored |

Product CSV accepted headers:

```csv
sku,name,category
SKU-SAMPLE-001,Sample Valve,Industrial Parts
```

Accepted aliases:

- SKU: `sku`, `catalogsku`, `productsku`
- Name: `name`, `productname`
- Category: `category`

## Inventory Mapping Model

| Customer source field | SynapseCore field | Required? | Transformation | Validation | Synthetic example |
| --- | --- | --- | --- | --- | --- |
| product SKU | `productSku` | Yes | match catalog SKU rules; do not silently remap | product must exist in tenant catalog | `SKU-SAMPLE-001` |
| location/site code | `warehouseCode` | Yes | map to approved SynapseCore warehouse code | warehouse must exist in tenant | `WH-NORTH` |
| available quantity | `quantityAvailable` | Yes | integer/long | minimum 0 | `25` |
| reorder/safety level | `reorderThreshold` | Yes | integer/long | minimum 0 | `10` |

Initial inventory baseline endpoint:

```text
POST /api/inventory/update
```

Payload:

```json
{
  "productSku": "SKU-SAMPLE-001",
  "warehouseCode": "WH-NORTH",
  "quantityAvailable": 25,
  "reorderThreshold": 10
}
```

Quantity semantics:

- `quantityAvailable` on input becomes available stock after synchronization.
- Existing reserved stock is preserved during update.
- On-hand becomes requested available quantity plus existing reserved quantity.
- `quantityReserved` is controlled by orders and fulfillment.
- `quantityInbound` is not currently loaded through the baseline update request.

## Order Mapping Model

### Webhook order

| Customer source field | SynapseCore field | Required? | Transformation | Validation | Synthetic example |
| --- | --- | --- | --- | --- | --- |
| source system | `sourceSystem` | Yes | trimmed; connector may normalize to configured source | letters/numbers/hyphen/underscore | `company1_erp` |
| order id | `externalOrderId` | Yes for traceability | trim | max 80 on domain API; unique per tenant | `C1-ORDER-0001` |
| location/site | `warehouseCode` | Yes unless connector fallback is enabled | policy may normalize uppercase | must resolve to tenant warehouse | `WH-NORTH` |
| customer reference | `customerReference` | Required only for strict webhook | trim | required under `STRICT` | `SAMPLE-CUSTOMER` |
| event time | `occurredAt` | Required only for strict webhook | ISO instant | required under `STRICT` | `2026-08-13T10:00:00Z` |
| item SKU | `items[].productSku` | Yes | policy may normalize uppercase | product must exist | `SKU-SAMPLE-001` |
| item quantity | `items[].quantity` | Yes | integer | minimum 1 | `2` |
| unit price | `items[].unitPrice` | Yes | decimal | greater than 0 | `10.00` |

### CSV order import

Required headers:

```csv
externalOrderId,warehouseCode,productSku,quantity,unitPrice
```

Optional header:

```csv
sourceSystem
```

CSV order import groups rows by:

- source system
- external order id
- warehouse code

Rows in one group become one order with multiple items.

## Identifier Strategy

| Source identifier | SynapseCore identifier | Uniqueness scope | Tenant scope |
| --- | --- | --- | --- |
| Product SKU | `Product.catalogSku`; API `sku`; response `sku` and `catalogSku` | tenant-scoped visible SKU | product tenant |
| Internal product SKU | `Product.sku` | global internal unique value | built as `TENANT::SKU` |
| Warehouse/location code | `Warehouse.code` | tenant + code | warehouse tenant |
| Order id | `CustomerOrder.externalOrderId` | tenant + external order id | order tenant |
| Connector source | `IntegrationConnector.sourceSystem` | tenant + source + type | connector tenant |
| Inbound record | `IntegrationInboundRecord.externalOrderId` plus source/type | operational evidence, not uniqueness authority | tenant code |
| Replay record | `IntegrationReplayRecord.externalOrderId` plus source/type | recovery evidence | tenant code |

Do not replace customer source identifiers without a reversible mapping. If Company 1 uses long or unsuitable identifiers, record the approved shortened identifier and preserve the original outside SynapseCore evidence if needed.

## Warehouse / Location Normalization

Rules:

- Map every Company 1 location string to one approved SynapseCore `Warehouse.code`.
- Do not create separate warehouses for spelling differences such as `JHB`, `Johannesburg`, and `JHB DC` without explicit approval.
- Warehouse code max length is 40.
- Warehouse code uniqueness is tenant-scoped.
- Warehouse display name/location can be managed through workspace settings.
- Orders and inventory must reference the warehouse code, not the display name.

## Pre-Import Validation

Validate before any Company 1 data enters the tenant:

- environment and backend URL
- tenant code and display name
- operator role/session
- approved source file/system
- approved data domain
- record count
- required fields
- field lengths and types
- SKU format
- warehouse code mapping
- product references
- duplicate SKUs
- duplicate product + warehouse rows
- duplicate external order IDs
- unsupported order statuses
- blank values
- negative quantities
- malformed CSV rows
- source data outside approved pilot scope

Pass/fail criteria:

- all required fields are present
- identifiers match approved mapping
- no wrong-tenant references exist
- invalid records are counted and classified
- all exclusions are explained before import

## Data Quality Report

Every data load must produce a summary with:

- total records
- valid
- invalid
- duplicates
- missing required field
- unknown SKU
- unknown location
- invalid status
- invalid quantity
- malformed record
- unsupported field excluded
- other

Do not silently discard invalid records.

## Invalid Data Decisions

| Condition | Decision |
| --- | --- |
| Missing required catalog field | Reject until corrected. |
| Duplicate SKU inside catalog file | Reject duplicate row. |
| SKU already exists in tenant catalog | Update existing product through catalog import/update. |
| Unknown product in inventory/order | Hold or reject until catalog is corrected. |
| Unknown warehouse/location | Hold until location mapping is approved. |
| Negative inventory baseline | Reject. |
| Adjustment below reserved stock | Reject/conflict. |
| Duplicate order external id | Reject as duplicate/conflict. |
| Connector-disabled inbound | Failed inbound/replay queue where applicable. |
| Unsupported order status | Exclude from Phase 6 import; lifecycle transitions are a separate supported operation. |
| Sensitive extra field | Exclude unless explicitly approved and supported. |

## Authoritative Load Order

1. Tenant/workspace already verified in Phase 3
2. Users/roles already verified in Phase 4
3. Connector lane already verified in Phase 5
4. Confirm warehouse/location mapping
5. Load catalog
6. Reconcile catalog
7. Load inventory baselines
8. Reconcile inventory
9. Load order test/sample data through the approved order path
10. Reconcile orders
11. Validate frontend readback
12. Validate role visibility
13. Validate one realtime order update
14. Validate one deterministic failure/replay path
15. Stop and hand off to Phase 7

Orders must not be loaded before catalog and inventory prerequisites exist, because order creation reserves stock and rejects unknown/missing/insufficient inventory.

## Test Procedures

### Catalog test

Procedure:

- create one synthetic product through `POST /api/products`
- import one synthetic CSV with one new product and one update row
- include one duplicate row inside the CSV to prove row-level failure
- read back `GET /api/products`
- verify tenant code and SKU normalization

Expected current behavior:

- valid create returns `201`
- duplicate create returns conflict
- import reports `created`, `updated`, and `failed`
- failed import rows include row number, SKU, status, and message

### Inventory test

Procedure:

- confirm product exists
- confirm warehouse exists
- call `POST /api/inventory/update` with quantity and threshold
- call `GET /api/inventory`
- try unknown product
- try unknown warehouse
- try negative baseline value
- try update existing product + warehouse row

Expected current behavior:

- valid update creates or updates inventory row
- unknown product returns not found
- unknown warehouse returns not found
- negative baseline is rejected by validation
- repeated update changes baseline for the same product + warehouse pair

### Order test

Procedure:

- use the Phase 5 verified connector lane or direct API for synthetic test
- submit a valid order
- submit duplicate external order id
- submit unknown product
- submit malformed/invalid CSV row if CSV lane is used
- confirm replay path with a deterministic invalid inbound record where applicable
- read back `GET /api/orders/recent`

Expected current behavior:

- valid order creates `RECEIVED` order and reserves inventory
- duplicate tenant external order id conflicts
- unknown product/inventory/warehouse rejects and may create replay evidence when coming through integration flow
- CSV returns imported and failed order details

## Bulk Capability Assessment

| Domain | Capability | Phase 6 classification |
| --- | --- | --- |
| Catalog | CSV import with row-level outcomes | Ready as-is for bounded pilot. |
| Inventory | No CSV; repeated `POST /api/inventory/update` | Manual but practical for bounded pilot. |
| Orders | Webhook, order CSV import, scheduled pull with limitations | Ready as-is if using Phase 5-supported lane. |

If Company 1 proposes substantially larger data counts than the proofed pilot envelope, stop and require technical review before loading.

## Environment And Tenant Safety

Before import, display and record:

- environment
- backend URL
- company
- tenant code
- tenant display name
- data domain
- source file/system
- record count
- test or live mode
- operator name/role
- backup reference when applicable

Wrong-tenant safety:

- every API call must be performed in a signed-in Company 1 session or connector-token tenant context
- every readback must show Company 1 tenant code or Company 1-only data
- all created records must be counted under the intended tenant
- any cross-tenant write is `PILOT BLOCKER - CRITICAL`

## Pre-Import Backup Policy

For synthetic and redacted samples, a backup is optional if the data is clearly disposable and isolated.

For approved bounded live Company 1 pilot data:

- take a pre-import backup/checkpoint where operationally practical
- record the backup reference in the onboarding record
- do not proceed if backup capability is unavailable and the data load is not safely reversible through supported APIs

Current rollback is not per-import transactional across domains, so backup evidence matters.

## Import Execution Record

For each load, record:

- company
- tenant
- domain
- source
- source environment
- source of truth
- operator role
- start time
- end time
- source count
- accepted count
- rejected count
- duplicate count
- resulting database/readback count
- validation result
- reconciliation result
- backup reference
- issues
- approval

Do not include customer row contents.

## Reconciliation

After every import compare:

Catalog:

- approved source products
- SynapseCore `GET /api/products`
- SKU/name/category counts
- created/updated/failed import rows

Inventory:

- approved product/location rows
- SynapseCore `GET /api/inventory`
- product SKU + warehouse code + quantity/threshold
- low-stock flags expected from thresholds

Orders:

- accepted source orders
- SynapseCore `GET /api/orders/recent`
- external order id, warehouse code, item count, total amount
- failed inbound/replay records if applicable

Tolerance rule:

```text
SOURCE TOTAL - APPROVED REJECTIONS = EXPECTED SYNAPSCORE TOTAL
```

Every mismatch must be explained. Unexplained mismatch blocks Phase 7.

## Relational Integrity Procedure

Verify:

- no inventory row references a missing product
- no inventory row references a warehouse outside Company 1
- no order item references a missing product
- no order references a warehouse outside Company 1
- no duplicate product + warehouse inventory rows
- no duplicate tenant external order ids
- no replay record shows duplicate successful recovery for the same intended order
- no Company 1 data appears under another tenant

Use application readback first. Use backup/restore or administrative DB evidence only if already approved for operational evidence collection.

## Frontend Readback

Verify through the application:

- Catalog page shows imported products and import outcomes
- Inventory page shows product/warehouse stock posture
- Orders page shows accepted orders
- Dashboard reflects catalog/inventory/order activity where appropriate
- Integrations page shows connector telemetry if order data came through connector
- Replay page shows deterministic failed inbound/replay evidence when tested

This is functional readback only. Do not redesign UI in Phase 6.

## Role Visibility

Verify with representative Company 1 users:

- tenant admin can manage catalog and user/workspace setup
- workspace operator can view approved data where intended
- warehouse-scoped operator sees only allowed warehouse lanes where access controls apply
- non-integration operator cannot replay
- another tenant cannot see Company 1 data

Do not repeat the full Gate 4 exhaustive proof. This is targeted Phase 6 visibility.

## Realtime And Replay Validation

Realtime validation:

- submit one approved synthetic or controlled test order through the selected order path
- observe dashboard/order/integration state update without manual database edits
- record timestamp and route

Failure/replay validation:

- submit one deterministic invalid inbound record
- confirm visible failure evidence
- correct prerequisite through supported API/path
- replay where eligible
- confirm final business state

Do not use uncontrolled production data for failure tests.

## Correction And Reimport Semantics

| Domain | Current semantics | Correction path |
| --- | --- | --- |
| Catalog | Create, update, CSV upsert by tenant-visible SKU | Use `PUT /api/products/{id}` or CSV reimport. |
| Inventory | Baseline update creates/updates product + warehouse row | Use `POST /api/inventory/update`; use receive/adjust/reconcile for operational corrections after baseline exists. |
| Orders | Create-only by tenant external order id; lifecycle transition supported | Do not reimport same external id as update. Use lifecycle transition where supported or correct source/replay before creation. |
| Order CSV | Groups and creates orders; failed rows reported | Correct source CSV and retry with non-duplicate external ids or replay failed inbound if applicable. |
| Webhook | Creates order or fails into integration handling | Correct source/prerequisites and replay if queued. |

Do not manually edit database rows to correct Company 1 data.

## Source-Of-Truth Rules

| Domain | Company 1 source of truth | SynapseCore role |
| --- | --- | --- |
| Catalog | Approved Company 1 product source | Tenant-scoped operational reference for pilot workflows. |
| Inventory | Company stock-control/WMS/source inventory system | Visibility, baseline, risk, and pilot coordination surface. |
| Orders | Company order source or Phase 5 connector source | Live operational coordination and recovery surface. |
| Warehouses | Company location/site master agreed during Phase 3 | Tenant location identity used by inventory and orders. |

During the pilot, SynapseCore should not silently become master of external business truth unless a later signed decision changes the operating model.

## Controlled Live Cutover Gate

Before switching from sample/test to bounded live data, require:

- Phase 2 authorization
- Phase 3 tenant verified
- Phase 4 users/roles verified
- Phase 5 connector verified
- mapping approved
- sample data passed
- data quality accepted
- pre-import backup/checkpoint where required
- source owner approval
- operator confirmation
- bounded volume confirmed
- no unresolved wrong-tenant or identifier risk

## Test Data Cleanup

Rules:

- Keep synthetic IDs clearly prefixed, for example `SYNTH-` or `C1-TEST-`.
- Do not mix proof/demo records with Company 1 real pilot records unless explicitly approved.
- Prefer using a separate synthetic tenant for rehearsal.
- If test records were loaded into Company 1 and no supported delete exists, isolate them by identifier prefix and exclude them from reconciliation.
- Do not delete rows manually.

Current limitation:

- Supported product/order/inventory delete endpoints are not part of the current onboarding path.
- Cleanup may require isolation/exclusion rather than deletion.

## Data Retention Behavior

Current observed behavior:

- business records persist in PostgreSQL
- catalog, inventory, orders, inbound records, replay records, audit logs, business events, and telemetry are retained unless explicitly changed by application behavior
- inbound and replay payload evidence may persist
- no automatic retention/deletion policy is documented as implemented for Phase 6 customer payloads

Treat retention as a pilot governance limitation until a formal retention policy and cleanup mechanism exists.

## Sensitive Payload Handling

Access:

- signed-in workspace users can view operational surfaces according to their roles and warehouse scope
- integration/replay surfaces expose failure and recovery evidence
- backend records inbound payload/replay evidence for traceability

Handling expectations:

- use synthetic/redacted payloads for testing
- minimize customer references
- never store credentials in payloads
- never commit raw customer payloads
- redact screenshots and reports
- document who can view replay evidence during pilot

## Dataset Size Classification

Use these classifications:

- `WITHIN TESTED/COMPARABLE PILOT SIZE`: bounded data comparable to hosted proof/readiness/pilot-sized synthetic data.
- `LARGER THAN TESTED - REVIEW REQUIRED`: larger but still plausible pilot data; requires technical review and possibly staged batches.
- `UNSUITABLE WITHOUT ADDITIONAL LOAD/DATA TEST`: full production migration, large history import, or high-volume connector replay beyond current proof evidence.

Do not infer enterprise-scale data capacity from the 25-user controlled load test alone.

## Stop Conditions

Stop onboarding immediately for:

- wrong tenant
- unexpected source file
- unexplained count mismatch
- data corruption
- widespread invalid mapping
- duplicate explosion
- wrong SKU mapping
- wrong warehouse mapping
- unexpected destructive update
- repeated 5xx responses
- database integrity error
- sensitive data accidentally placed in repo or public artifact

Response:

1. Stop.
2. Preserve evidence.
3. Disable connector if inbound traffic is involved.
4. Restore if required and available.
5. Investigate.
6. Correct mapping/source.
7. Retest with sample data before continuing.

## Rollback Procedure

Supported rollback options:

- restore from pre-import backup/checkpoint
- correct catalog through update/reimport
- correct inventory through update/reconcile/adjust
- disable connector to stop new inbound orders
- reject/hold invalid records before import
- use order lifecycle transition where supported

Not promised:

- per-import transaction rollback across catalog, inventory, orders, inbound, replay, and audit
- manual SQL row deletion
- automatic customer data purge

## Company Readiness Script Applicability

`scripts/verify-company-readiness.ps1` remains a synthetic local/self-host rehearsal script.

Now provable after Phase 6:

- catalog path behavior
- inventory baseline/update behavior
- order connector path behavior
- replay path behavior
- role/session flow
- workspace data visibility

Still waits for later phases:

- Company 1 operational configuration
- final alert/recommendation/scenario governance
- pre-handover verification
- actual customer handover evidence

## Phase 6 Verification Gate

Authorize Phase 7 only when:

- approved data scope exists
- actual domain models are documented
- official ingestion method per domain is selected
- mappings are complete
- identifiers are mapped
- sample validation passed
- wrong-tenant safeguards are defined
- duplicate behavior is understood
- catalog path is verified
- inventory path is verified
- order path is verified
- invalid data behavior is verified
- reconciliation method is defined
- source-of-truth rules are documented
- rollback/correction is understood
- sensitive-data handling is documented
- no real customer data is committed to repository
- evidence templates are ready

## Phase 7 Handoff

Phase 7 receives:

- verified tenant
- verified users and roles
- verified connector
- approved and verified data mappings
- catalog state
- inventory state
- orders state
- source-of-truth definitions
- alert-relevant fields
- recommendation-relevant fields
- replay/recovery rules
- governance inputs
- scenario relevance
- success metrics from Phase 2
- data limitations

Stop after this handoff. Do not configure operational rules in Phase 6.

## Phase 6 Verdict Guidance

Use `COMPANY PILOT PHASE 6 ACCEPTED` only when Company 1 data can be onboarded through the supported paths above, with sample validation, reconciliation, tenant safety, and evidence complete.

Use `COMPANY PILOT PHASE 6 ACCEPTED WITH DOCUMENTED LIMITATION` when the controlled onboarding path is safe but limitations remain, such as manual inventory batching, no per-import rollback, no delete endpoint, no automatic retention policy, or no inventory CSV endpoint.

Use `COMPANY PILOT PHASE 6 NOT ACCEPTED - SAFE DATA ONBOARDING INCOMPLETE` when catalog, inventory, or order data cannot be loaded safely without unsupported database edits or unresolved wrong-tenant/identifier risk.
