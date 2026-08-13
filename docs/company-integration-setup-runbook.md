# Company Integration Setup Runbook

This is the internal SynapseCore Phase 5 runbook for establishing the first Company 1 integration lane after the tenant/workspace and approved users have already been provisioned.

This phase connects one controlled external-system lane to the Company 1 tenant. It does not load the final company operating dataset, configure operational rules, create new integration types, or bypass the product APIs.

## Phase Boundary

Phase 5 includes:

- confirming the approved Phase 2 integration intake
- confirming the Phase 3 tenant/workspace boundary
- confirming the Phase 4 integration admin and replay operator users
- choosing one initial connector lane
- creating or updating the connector through the supported backend API
- documenting the connector secret posture without storing secrets in Git
- testing valid inbound flow with synthetic or approved sample data
- testing failure handling, replay, role boundaries, and tenant isolation
- proving that the connector can be disabled or rotated safely
- handing a verified connector lane to Phase 6 company data onboarding

Phase 5 does not include:

- importing the final Company 1 catalog, inventory, or order history
- configuring final alerts, recommendations, scenarios, or approval policy
- customer-facing handover
- manually editing database rows
- building customer-specific integration code
- adding new connector types
- treating SynapseCore as the company's ERP, WMS, ecommerce system, or integration middleware replacement

## Current Connector Model

In current SynapseCore, an integration connector is a tenant-owned lane that controls how external order data can enter the operational workspace.

| Area | Current implementation |
| --- | --- |
| Entity | `IntegrationConnector` |
| Table | `integration_connectors` |
| Tenant ownership | `tenant_id` many-to-one relationship to `Tenant` |
| Unique identity | tenant + `sourceSystem` + `type` |
| Supported types | `WEBHOOK_ORDER`, `CSV_ORDER_IMPORT` |
| Supported sync modes | `REALTIME_PUSH`, `BATCH_FILE_DROP`, `SCHEDULED_PULL` |
| Validation policies | `STANDARD`, `STRICT`, `RELAXED` |
| Transformation policies | `NONE`, `NORMALIZE_CODES` |
| Mapping version | only `1` is supported |
| Secret-bearing field | inbound connector token only |
| Secret storage | SHA-256 hash plus masked hint; raw token is not returned |
| Health states | `LIVE`, `DEGRADED`, `OFFLINE` |
| Delete/archive support | no delete/archive endpoint today; disable instead |

The connector is not a general ETL pipeline. It is currently an order-ingestion lane with replay visibility and operational telemetry.

## Supported Connector Types

| Connector type | Purpose | Supported input | Supported sync mode | Replay support | Notes |
| --- | --- | --- | --- | --- | --- |
| `WEBHOOK_ORDER` | Inbound order events | JSON order payload | `REALTIME_PUSH` or `SCHEDULED_PULL` | Yes | Best first pilot lane when the source can push or expose JSON order events. |
| `CSV_ORDER_IMPORT` | Batch order file intake | Multipart CSV file | `BATCH_FILE_DROP` | Yes for grouped order failures | This is order CSV import, not catalog import. |

## Connector Creation Paths

| Path | Status | Use in Phase 5 |
| --- | --- | --- |
| `POST /api/integrations/orders/connectors` | Supported protected API | Official creation/update path. |
| `/integrations` UI | Supported visibility/recovery surface | Use to inspect connector health, import runs, and replay pressure after creation. |
| Workspace settings connector support UI | Supported limited support update path | Use to update owner, sync mode, policy, fallback, notes, and support posture after connector exists. |
| Hosted proof preparation script | Proof-only | Do not use as Company 1 provisioning. |
| Starter/dev seeding | Dev/proof convenience only | Do not use for Company 1. |
| Direct SQL | Not supported | Do not use. |

## Required Roles

| Action | Required role |
| --- | --- |
| View connectors | Signed-in workspace access |
| Create or update connector through integration API | `INTEGRATION_ADMIN` |
| Update connector support fields through workspace admin | Tenant admin path in workspace administration |
| Replay failed inbound record | `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN` |
| Verify normal operator cannot replay | A signed-in user without integration replay roles |

The recommended Phase 5 operator model is:

- one approved Company 1 integration admin user
- one approved Company 1 replay operator user
- one normal operator user for negative authorization checks
- one SynapseCore internal provisioning operator if the pilot process keeps setup work internal

## Secret And Credential Model

SynapseCore currently supports an inbound connector token for external pushes and connector-authenticated CSV imports.

Rules:

- The inbound token is sent in the `X-Synapse-Connector-Token` header.
- The backend stores only a SHA-256 hash and a masked hint.
- The raw token is not returned by connector APIs.
- The token must be generated outside Git and recorded only in the secure pilot vault or customer-approved secret store.
- Rotation is done by posting a new `inboundAccessToken`.
- Clearing is done with `clearInboundAccessToken=true`.
- Do not put secrets in `notes`, `pullEndpointUrl`, CSV files, sample JSON, screenshots, docs, Git commits, or support messages.

Current limitation:

- There is no dedicated outbound credential model for scheduled pull connectors.
- `pullEndpointUrl` is returned through the API and UI, so credentials must not be embedded in that URL.
- If Company 1 requires OAuth, API keys, basic auth, mTLS, or signed outbound calls for scheduled pull, the first connector lane must use webhook/CSV instead or the pilot must document this as a blocker.
- There is no HMAC signature validation for inbound webhooks today; the implemented control is the connector token plus tenant/source/type resolution.

## Inbound Data Sensitivity

Inbound payloads can be persisted for traceability and replay in integration inbound/replay records. That gives the product operational memory, but it also means samples must be handled carefully.

Phase 5 sample-data rules:

- Use synthetic data unless the customer explicitly approves real samples.
- Remove personal data, payment data, or confidential customer references unless needed for the pilot lane.
- Keep sample payloads small and purpose-specific.
- Treat failed inbound records as sensitive operational evidence.
- Do not paste raw payloads with secrets into issue trackers or public reports.

## Official Phase 5 Setup Sequence

1. Confirm Phase 2 intake is approved.
2. Confirm Phase 3 tenant code and tenant name.
3. Confirm Phase 4 integration admin, replay operator, and normal operator users can sign in.
4. Choose exactly one first connector lane.
5. Confirm the source system name uses only letters, numbers, hyphens, and underscores.
6. Confirm connector type is `WEBHOOK_ORDER` or `CSV_ORDER_IMPORT`.
7. Confirm the source system is not already configured for the same connector type inside the tenant.
8. Confirm the target warehouse code and product SKUs are not final business data unless Phase 6 has explicitly authorized them.
9. Generate a new random inbound connector token if the lane uses connector-token authentication.
10. Create the connector through `POST /api/integrations/orders/connectors`.
11. Start disabled if the lane needs configuration review before any inbound acceptance.
12. Assign a support owner.
13. Configure validation policy, transformation policy, fallback behavior, and notes.
14. Enable only when the synthetic sample test is ready.
15. Run the valid inbound sample.
16. Run the expected failure sample.
17. Confirm the failed sample appears in the replay queue.
18. Confirm an authorized operator can replay only when prerequisites are satisfied.
19. Confirm an unauthorized operator cannot replay.
20. Confirm connector health, import runs, dashboard snapshot, and realtime update.
21. Disable the connector and verify inbound is blocked.
22. Rotate the token and verify the old token fails.
23. Record evidence in the provisioning record.
24. Hand the verified connector lane to Phase 6.

## Connector Configuration Payload

Use a signed-in `INTEGRATION_ADMIN` session or the approved internal provisioning mechanism for the current environment. Do not use database edits.

Example connector payload:

```json
{
  "sourceSystem": "company1_erp",
  "type": "WEBHOOK_ORDER",
  "displayName": "Company 1 ERP Orders",
  "enabled": false,
  "syncMode": "REALTIME_PUSH",
  "validationPolicy": "STANDARD",
  "transformationPolicy": "NORMALIZE_CODES",
  "mappingVersion": 1,
  "allowDefaultWarehouseFallback": true,
  "defaultWarehouseCode": "MAIN",
  "notes": "Initial Company 1 pilot order lane. No secrets.",
  "inboundAccessToken": "<store-outside-git>"
}
```

Important:

- Reposting the same tenant + `sourceSystem` + `type` updates the connector.
- Changing source system or type creates a different connector identity.
- There is no delete endpoint; disable unused connectors.
- `mappingVersion` must remain `1`.

## Webhook Order Lane

Endpoint:

```text
POST /api/integrations/orders/webhook
Header: X-Synapse-Connector-Token: <connector-token>
Content-Type: application/json
```

Payload shape:

```json
{
  "sourceSystem": "company1_erp",
  "externalOrderId": "COMPANY1-SAMPLE-001",
  "warehouseCode": "MAIN",
  "customerReference": "SAMPLE-CUSTOMER",
  "occurredAt": "2026-08-13T10:00:00Z",
  "items": [
    {
      "productSku": "SKU-SAMPLE-001",
      "quantity": 1,
      "unitPrice": 10.00
    }
  ]
}
```

Successful result:

- connector token resolves the tenant and source system
- connector is checked for enabled state
- payload is validated
- product, warehouse, and inventory references are checked by the order flow
- inbound record is marked accepted
- import run is recorded
- order becomes visible in the workspace
- realtime operational state updates

Failure result:

- invalid token returns an authentication/authorization failure
- disabled connector returns connector-disabled behavior
- missing or invalid fields reject the request
- missing product, inventory, warehouse, or insufficient inventory moves into integration failure handling
- replay record is created when the failure reaches replayable order-ingestion handling

## CSV Order Import Lane

Endpoint:

```text
POST /api/integrations/orders/csv-import
Header: X-Synapse-Connector-Token: <connector-token>
Multipart field: file
Optional request parameter: sourceSystem
```

Connector-authenticated CSV import must provide the `sourceSystem` request parameter so the token can be matched to the configured CSV connector.

Required CSV columns:

```csv
externalOrderId,warehouseCode,productSku,quantity,unitPrice
COMPANY1-SAMPLE-CSV-001,MAIN,SKU-SAMPLE-001,1,10.00
```

Optional CSV column:

```csv
sourceSystem
```

Successful result:

- rows are grouped into orders by source system, external order id, and warehouse
- configured connector is required
- connector enabled state is enforced
- policy normalization and validation are applied
- orders are imported
- failed grouped orders are recorded with failure details
- import run is recorded as `SUCCESS`, `PARTIAL_SUCCESS`, or `FAILURE`

Current limitation:

- CSV import is batch file-drop only.
- `SCHEDULED_PULL` and `REALTIME_PUSH` are not supported for `CSV_ORDER_IMPORT`.

## Scheduled Pull Lane

Scheduled pull is implemented only for enabled `WEBHOOK_ORDER` connectors.

Current behavior:

- worker property: `synapsecore.integration.pull-worker.enabled`, default `true`
- worker interval property: `synapsecore.integration.pull-worker.interval-ms`, default `60000`
- worker batch property: `synapsecore.integration.pull-worker.batch-size`, default `10`
- fetch timeout property: `synapsecore.integration.pull-worker.fetch-timeout-seconds`, default `20`
- connector cadence: `syncIntervalMinutes`, minimum `15`, maximum `1440`
- request method: HTTP GET
- accepted endpoint scheme: absolute `http` or `https`
- request headers sent by SynapseCore:
  - `Accept: application/json`
  - `X-SynapseCore-Tenant: <tenantCode>`
  - `X-SynapseCore-Connector: <sourceSystem>`
- response accepted by parser:
  - a JSON order object
  - a JSON array of order objects
  - a JSON object with an `orders` array

Successful scheduled pull result:

- `lastPullAttemptAt` is updated
- `lastPullStatus` starts as `RUNNING`
- fetched orders are processed through the same order ingestion path
- `lastPullStatus` becomes `SUCCESS`, `PARTIAL_SUCCESS`, or `FAILURE`
- `lastPullMessage` records received/imported/failed counts
- import-run telemetry and realtime integration state are emitted

Current limitation:

- Scheduled pull has no dedicated outbound credential fields.
- Do not embed credentials in `pullEndpointUrl`.
- If the customer source requires authenticated outbound polling, treat scheduled pull as not ready for that lane.

## Validation And Transformation Policies

| Policy | Current behavior |
| --- | --- |
| `STANDARD` | Requires normal order validity and valid tenant data references. |
| `STRICT` | Requires webhook `customerReference` and `occurredAt`, blocks duplicate product lines, and enforces default warehouse mismatch checks when configured. |
| `RELAXED` | Deduplicates product lines by SKU before order creation. |
| `NORMALIZE_CODES` | Uppercases warehouse codes and product SKUs. |
| `NONE` | Preserves trimmed warehouse codes and product SKUs. |

Recommended first pilot setting:

- `WEBHOOK_ORDER`: `STANDARD` + `NORMALIZE_CODES`
- `CSV_ORDER_IMPORT`: `RELAXED` + `NORMALIZE_CODES`
- `STRICT` only after Company 1 confirms payload completeness and duplicate-line expectations

## Replay And Recovery Path

Replay is not a workaround. It is the supported recovery path for failed inbound order work.

Replay path:

1. Connector receives an inbound webhook, CSV group, or scheduled-pull order.
2. Validation or order creation fails.
3. Failure code and failure message are captured.
4. Inbound record is marked rejected or replay queued when applicable.
5. Replay record is created with tenant code, source system, connector type, payload, failure code, and retry metadata.
6. Replay queue becomes visible to operators.
7. Authorized integration operator reviews the failure.
8. Operator fixes the prerequisite issue outside hidden DB edits, such as enabling connector or loading the required product during the proper phase.
9. Operator clicks replay.
10. The failed inbound item re-enters the live order flow.
11. Audit, business event, import telemetry, and realtime integration state update.

Automation:

- automated replay exists
- default interval is controlled by `synapsecore.integration.replay.automation.interval-ms`, default `30000`
- max attempts are controlled by `synapsecore.integration.replay.max-attempts`, default `3`
- backoff is controlled by `synapsecore.integration.replay.backoff-seconds`, default `300`
- connector-disabled failures are manual-only

## Failure Codes To Expect

Common Phase 5 failure codes include:

- `CONNECTOR_NOT_CONFIGURED`
- `INVALID_CONNECTOR_TOKEN`
- `CONNECTOR_DISABLED`
- `MISSING_EXTERNAL_ORDER_ID`
- `MISSING_ITEMS`
- `MISSING_WAREHOUSE_CODE`
- `STRICT_WAREHOUSE_MISMATCH`
- `MISSING_CUSTOMER_REFERENCE`
- `MISSING_OCCURRED_AT`
- `DUPLICATE_PRODUCT_LINES`
- `MISSING_PRODUCT_SKU`
- `INVALID_SOURCE_SYSTEM`
- `INVALID_QUANTITY`
- `INVALID_UNIT_PRICE`
- `MISSING_FILE`
- `EMPTY_CSV`
- `MISSING_CSV_HEADER`
- `MISSING_HEADER_COLUMN`
- `CONNECTOR_SOURCE_MISMATCH`
- `UNSUPPORTED_MAPPING_VERSION`
- `WAREHOUSE_NOT_FOUND`
- `PRODUCT_NOT_FOUND`
- `INVENTORY_NOT_FOUND`
- `INSUFFICIENT_INVENTORY`
- `DUPLICATE_EXTERNAL_ORDER_ID`
- `UNKNOWN`

## Phase 5 Verification Checklist

### Pre-checks

- Confirm Company 1 tenant code.
- Confirm connector source-system naming.
- Confirm connector type.
- Confirm first lane is one connector only.
- Confirm Phase 4 integration admin can sign in.
- Confirm Phase 4 replay operator can sign in.
- Confirm normal operator can sign in for negative test.
- Confirm no final business dataset is being loaded.
- Confirm no secrets are stored in Git.
- Confirm token is stored only in the approved secret location.

### Creation checks

- Create connector through `POST /api/integrations/orders/connectors`.
- Verify connector appears under `/api/integrations/orders/connectors`.
- Verify connector appears in the Integrations UI.
- Verify tenant code on response matches Company 1.
- Verify `inboundAccessConfigured=true` when token is configured.
- Verify only masked token hint is visible.
- Verify raw token is never returned.
- Verify support owner is assigned.
- Verify health is `OFFLINE` when disabled.
- Verify health becomes `LIVE` or `DEGRADED` based on activity when enabled.

### Inbound success checks

- Submit one synthetic valid webhook order or CSV order.
- Confirm HTTP success response.
- Confirm order appears in Company 1 workspace only.
- Confirm import run appears.
- Confirm integration telemetry updates.
- Confirm dashboard/realtime state updates.
- Confirm no data appears in another tenant.

### Failure checks

- Submit invalid token.
- Submit disabled connector request.
- Submit missing product SKU.
- Submit unknown warehouse or product.
- Submit duplicate external order id.
- Submit unsupported mapping version during connector update.
- Confirm expected status and failure message.
- Confirm replay queue behavior where applicable.

### Replay checks

- Confirm replay queue contains failed inbound item.
- Confirm normal operator cannot replay.
- Confirm integration operator can replay only if warehouse scope allows it.
- Fix prerequisite through supported setup path.
- Replay into live flow.
- Confirm replay result.
- Confirm audit/history evidence.
- Confirm realtime integration state update.

### Emergency checks

- Disable connector.
- Confirm new inbound traffic is blocked.
- Rotate token.
- Confirm old token fails.
- Confirm new token works after enablement.
- Clear token only if the lane should stop accepting connector-token traffic.

## Cross-Tenant And Role Negative Tests

Required negative tests:

- A connector token for Company 1 must not write into another tenant.
- A signed-in user from another tenant must not see Company 1 connectors.
- A signed-in user without `INTEGRATION_ADMIN` must not create/update connectors.
- A signed-in user without `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN` must not replay failed inbound work.
- A user without warehouse scope must not replay a record for that warehouse.

## Evidence To Capture

Capture evidence without secrets:

- tenant code and tenant name
- connector id
- source system
- connector type
- enabled/disabled state
- sync mode
- validation and transformation policy
- mapping version
- default warehouse fallback posture
- support owner
- inbound token hint only
- valid sample request id or timestamp
- valid sample result
- invalid token result
- disabled connector result
- replay queue record id
- replay result
- cross-tenant negative result
- role negative result
- import-run summary
- realtime/dashboard observation
- issues and limitations

Do not capture:

- raw inbound connector token
- passwords
- customer private credentials
- unredacted payloads containing sensitive business/customer data
- manual database screenshots

## Correction, Disablement, And Removal

Current product behavior:

- Connector configuration is updated through upsert.
- Connector support posture is updated through workspace connector support.
- Connector tokens can be rotated or cleared.
- Connectors can be disabled.
- Connectors cannot be deleted or archived through a supported endpoint today.

Operational response:

- If a connector is wrong but has no useful evidence, disable it and create the correct source/type lane.
- If the source/type is correct but configuration is wrong, update it through the supported API.
- If the token may be exposed, rotate immediately.
- If inbound traffic must stop, disable first, then investigate.
- Do not manually delete connector rows.

## Technical Contact Requirements

Before Phase 5 is accepted, Company 1 or the SynapseCore pilot owner must identify:

- business process owner
- technical source-system contact
- connector support owner inside SynapseCore
- replay decision owner
- incident escalation contact
- secret holder
- test-data approver
- go/no-go approver for enabling the lane

## Phase 6 Handoff

Phase 5 hands off to Phase 6 only when:

- the connector lane exists
- the connector lane is tenant-scoped
- the secret posture is recorded safely
- synthetic or approved sample flow has been tested
- replay behavior has been tested
- disable and token rotation behavior has been tested
- role and tenant negative checks pass
- known limitations are documented
- Company 1 confirms the first connector lane is acceptable for pilot use

Phase 6 then handles company data onboarding.

## Current Limitations

These limitations are not hidden. They are part of the safe pilot boundary:

- Only order ingestion connectors are implemented today.
- `WEBHOOK_ORDER` and `CSV_ORDER_IMPORT` are the only connector types.
- Mapping is fixed to version `1`; there is no arbitrary customer field-mapping UI.
- There is no connector delete/archive endpoint.
- There is no customer self-service connector provisioning workflow.
- There is no dedicated outbound secret model for scheduled pull.
- There is no HMAC webhook signature validation today.
- Inbound payload and replay payload retention must be treated as sensitive operational evidence.
- CSV import is order import only, not product/catalog onboarding.
- Final customer data loading belongs to Phase 6, not Phase 5.

## Acceptance Verdict Guidance

Use `COMPANY PILOT PHASE 5 ACCEPTED` only when the chosen connector lane is webhook or CSV, uses supported auth and policies, passes success/failure/replay/role/tenant checks, and has no blocking limitation for the selected Company 1 lane.

Use `COMPANY PILOT PHASE 5 ACCEPTED WITH DOCUMENTED LIMITATION` when the controlled initial lane is safe but at least one limitation remains documented, such as no connector deletion, no arbitrary mapping UI, no outbound scheduled-pull credentials, or no HMAC signatures.

Use `COMPANY PILOT PHASE 5 NOT ACCEPTED - SAFE INTEGRATION SETUP INCOMPLETE` when the connector cannot be created through supported APIs, cannot be safely authenticated, cannot be tenant-scoped, cannot be replay-tested, or requires unsupported credential handling.
