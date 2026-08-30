# Layer 2 Phase 1 - Operational Foundation Evidence

Date: 2026-08-30
Starting HEAD: `857e3d0f204bd35bac289900f213e3fae579b4e5`
Scope: integrated operational foundation only

## Evidence Boundary

This evidence covers the first Layer 2 foundation boundary: controlled tenant
provisioning, authenticated role and warehouse scope, catalog and inventory
baselines, connector configuration, dashboard truth, activity/audit evidence,
and negative authority checks.

It deliberately does not create or import orders, post webhook or CSV source
events, run replay, execute fulfillment, create Scenario fixtures, or exercise a
hosted environment. No `OWNER-ACCEPT-02` state was read or changed. The test
uses an isolated disposable Spring test context with H2, not Render PostgreSQL.

## 1. Starting Repository State

The repository HEAD immediately before this Phase 1 work was:

```text
857e3d0f204bd35bac289900f213e3fae579b4e5
```

Pre-existing unrelated worktree files were preserved and were not included in
this change:

- `frontend/Dockerfile`
- `.gitattributes`
- `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`
- `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`

## 2. Disposable Fixtures

### Tenant A

| Field | Value |
| --- | --- |
| Tenant code | `L2-TENANT-A` |
| Warehouses | `L2-WH-A`, `L2-WH-B` |
| Purpose | Two-site tenant and warehouse-scope boundary |
| Provisioning result | `READY` |

### Tenant B

| Field | Value |
| --- | --- |
| Tenant code | `L2-TENANT-B` |
| Warehouse | `L2-WH-B-ONLY` |
| Purpose | Cross-tenant isolation boundary |
| Provisioning result | `READY` |

All fixture credentials are synthetic test values in the ignored test process;
no runtime, hosted, or pilot credentials are recorded in this evidence.

## 3. Provisioning Results

Tenant A was provisioned through the controlled `POST /api/access/tenants`
platform provisioning path with two explicit warehouses and explicit role
assignments. Tenant B was provisioned through the same path with one warehouse
and a tenant administrator.

Provisioning created the tenant, workspace context, warehouses, operators, and
users only. Immediately after provisioning, both tenants had zero:

- products
- inventory records
- integration connectors
- orders
- fulfillment tasks
- active alerts
- current recommendations
- Scenario runs
- inbound integration records
- replay records

This confirms that onboarding did not silently seed operational state.

## 4. Authenticated Role Matrix

The test authenticated every configured Phase 1 identity through the real
session endpoint and re-read the resulting session. Password-change-required
role users completed the supported session password-change flow before the
final session assertion.

| Tenant | Identity | Role | Expected warehouse scope | Result |
| --- | --- | --- | --- | --- |
| A | `l2.a.admin` | `TENANT_ADMIN` | tenant-wide (`[]`) | authenticated |
| A | `l2.a.integration.admin` | `INTEGRATION_ADMIN` | `L2-WH-A` | authenticated |
| A | `l2.a.integration.operator` | `INTEGRATION_OPERATOR` | `L2-WH-A` | authenticated |
| A | `l2.a.integration.admin.b` | `INTEGRATION_ADMIN` | `L2-WH-B` | authenticated |
| A | `l2.a.integration.operator.b` | `INTEGRATION_OPERATOR` | `L2-WH-B` | authenticated |
| A | `l2.a.review.owner` | `REVIEW_OWNER` | `L2-WH-A` | authenticated |
| A | `l2.a.final.approver` | `FINAL_APPROVER` | `L2-WH-A` | authenticated |
| A | `l2.a.escalation.owner` | `ESCALATION_OWNER` | `L2-WH-A` | authenticated |
| B | `l2.b.admin` | `TENANT_ADMIN` | tenant-wide (`[]`) | authenticated |

The test also asserts that each session's returned role and warehouse scope
exactly match the expected fixture.

## 5. Platform Owner Separation

The platform-admin header is accepted for controlled tenant provisioning only.
The same platform header without a tenant session is rejected for tenant
product creation with HTTP `403`. This prevents a platform provisioning
credential from being treated as an authenticated tenant operator.

This Phase does not claim full hosted Platform Owner UI or control-plane proof;
that remains outside this local foundation test.

## 6. Product Creation

Tenant A created exactly three products through the tenant-scoped product API:

- `L2-SKU-A`
- `L2-SKU-B`
- `L2-SKU-C`

Tenant B created one independent product with the same external SKU value
`L2-SKU-A`. The persisted product identifiers are distinct, and each response
returned its own tenant code and tenant-scoped internal SKU.

## 7. Product Tenant Isolation

Tenant A reads three products and cannot see Tenant B's product. Tenant B reads
one product and cannot see Tenant A's other product names. A Tenant B attempt to
update Tenant A's product was rejected with HTTP `404`, and the Tenant A
product count remained unchanged.

## 8. Inventory Baseline

Tenant A received six inventory records, three products in each of two
warehouses:

| Tenant | Warehouse | Product SKUs | Quantity available | Reserved |
| --- | --- | --- | --- | --- |
| A | `L2-WH-A` | A, B, C | 100 each | 0 each |
| A | `L2-WH-B` | A, B, C | 50 each | 0 each |
| B | `L2-WH-B-ONLY` | A | 40 | 0 |

The test asserts the actual API fields `quantityAvailable` and
`quantityReserved`, not display-only aliases.

## 9. Inventory Warehouse and Tenant Isolation

Tenant A's inventory read contains only `L2-WH-A` and `L2-WH-B`. Tenant B's
inventory read contains exactly `L2-WH-B-ONLY`.

The following unauthorized inventory mutations were rejected with HTTP `403`:

- `REVIEW_OWNER` attempting an inventory update
- A warehouse-A integration operator targeting warehouse B
- A warehouse-B integration operator targeting warehouse A

No inventory count changed after those rejected requests.

## 10. Connector Configuration

Tenant A configured two order connectors for `L2-WH-A` through the integration
admin surface:

| Connector | Enabled | Mode | Validation | Transformation | Default warehouse |
| --- | --- | --- | --- | --- | --- |
| `l2_active_a` | true | `REALTIME_PUSH` | `STANDARD` | `NORMALIZE_CODES` | `L2-WH-A` |
| `l2_disabled_a` | false | `REALTIME_PUSH` | `STANDARD` | `NORMALIZE_CODES` | `L2-WH-A` |

The test also verifies the connector response contains Tenant A ownership and
the expected warehouse.

## 11. Connector Authority

The following connector creation attempts were rejected with HTTP `403`:

- A warehouse-A integration admin targeting warehouse B
- A warehouse-B integration admin targeting warehouse A
- Tenant Admin attempting to manage an integration connector

Connector count remained unchanged after the rejected requests.

## 12. Operational Zero-State and Downstream Boundaries

After the catalog, inventory, and connector baseline was created, both tenants
still had no operational order or downstream execution state:

| Domain | Tenant A | Tenant B |
| --- | ---: | ---: |
| Orders | 0 | 0 |
| Fulfillment tasks | 0 | 0 |
| Inbound records | 0 | 0 |
| Replay records | 0 | 0 |
| Scenario runs | 0 | 0 |
| Active alerts | 0 | 0 |
| Current recommendations | 0 | 0 |

No source event was ingested. Connector configuration alone did not create an
order, fulfillment task, replay item, alert, or recommendation.

## 13. Dashboard Truth Ledger

The dashboard summaries were read through each tenant's authenticated admin
session.

| Metric | Tenant A | Tenant B |
| --- | ---: | ---: |
| Products | 3 | 1 |
| Warehouses | 2 | 1 |
| Inventory records | 6 | 1 |
| Orders | 0 | 0 |
| Fulfillment backlog | 0 | 0 |
| Active alerts | 0 | 0 |
| Recommendations | 0 | 0 |

The dashboard matched the persisted catalog/inventory baseline and did not
invent operational attention for an unexecuted foundation setup.

## 14. Activity Evidence

Tenant A's recent business events contained the expected event types:

- `PRODUCT_CATALOG_UPDATED`
- `INVENTORY_UPDATED`
- `INTEGRATION_CONNECTOR_UPDATED`

These are configuration/baseline events. There was no order, fulfillment,
inbound, replay, alert, or recommendation event.

## 15. Audit Evidence

The onboarding audit was read immediately after the Tenant A admin session was
established and contained `TENANT_ONBOARDED`. The later bounded recent audit
window contained successful:

- `PRODUCT_CREATED`
- `INVENTORY_UPDATED`
- `INTEGRATION_CONNECTOR_UPDATED`

Rejected requests were visible as `REQUEST_REJECTED` audit entries, while the
successful-audit count and business-event count remained unchanged after the
negative authority checks. This is intentional failure visibility, not a
successful mutation.

## 16. Negative Authority and False-Success Results

Eight unauthorized paths were exercised across tenant, warehouse, role, and
platform boundaries. All were rejected at the HTTP boundary and no successful
mutation occurred afterward:

1. cross-tenant product update
2. Review Owner inventory update
3. warehouse-A integration operator targeting warehouse B
4. warehouse-B integration operator targeting warehouse A
5. platform header attempting tenant product creation
6. warehouse-A integration admin targeting warehouse B connector
7. warehouse-B integration admin targeting warehouse A connector
8. Tenant Admin attempting connector management

The test compares product, inventory, connector, successful-audit, and
business-event counts before and after these attempts. No false success was
observed.

## 17. Cross-Domain Integrated Test

The new test is:

```text
com.synapsecore.Layer2Phase1OperationalFoundationIntegrationTest
```

The test method provisions both tenants, authenticates the role matrix, creates
the catalog and inventory baselines, configures connectors, reads dashboard /
activity / audit surfaces, exercises negative authority paths, and rechecks
persisted state in one isolated Spring Boot test context.

## 18. Verification Results

### New focused proof

Command:

```powershell
cd backend
cmd /c mvnw.cmd -Dtest=Layer2Phase1OperationalFoundationIntegrationTest test
```

Result:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

The local test context validated all 13 Flyway migrations and initialized
Hikari, JPA/EntityManager, MockMvc, and the Spring simple broker. This is local
test evidence, not a claim about Render runtime health.

### Relevant existing regressions

Command:

```powershell
cd backend
cmd /c mvnw.cmd -q '-Dtest=Domain11ControlledProvisioningIntegrationTest,PlatformTenantAccessBoundaryIntegrationTest,CatalogDownstreamBoundaryIntegrationTest' test
```

Results:

| Test class | Tests | Failures | Errors |
| --- | ---: | ---: | ---: |
| `Domain11ControlledProvisioningIntegrationTest` | 9 | 0 | 0 |
| `PlatformTenantAccessBoundaryIntegrationTest` | 34 | 0 | 0 |
| `CatalogDownstreamBoundaryIntegrationTest` | 5 | 0 | 0 |
| Total existing regression set | 48 | 0 | 0 |

Combined with the new test, the Phase 1 focused evidence is 49 tests with zero
failures and zero errors across the new foundation proof and relevant existing
boundary coverage.

## 19. Classification

### Classification A - Product defects

`0` found in this Phase.

No production source change was required.

### Classification B - Intentional product boundaries

- Tenant onboarding intentionally creates identity/workspace context without
  operational transactions.
- Connector configuration is not source-system ingestion.
- Platform provisioning authorization is not tenant operational authority.
- Rejected requests remain visible in audit evidence without becoming successful
  mutations.

### Classification C - Evidence deferred

- Render PostgreSQL and Redis deployment verification
- hosted tenant/session proof for this Layer 2 fixture
- real frontend browser walkthrough for this foundation fixture
- source webhook and CSV ingestion
- replay eligibility, replay execution, and duplicate safety
- realtime client subscription/refresh proof
- dependency outage and recovery behavior

### Classification D - Future evolution

- production-scale load and concurrency beyond this bounded foundation
- background worker/queue separation
- horizontally scaled realtime delivery
- broader connector and source-system coverage
- managed backup/restore and enterprise deployment evidence

## 20. Files and Scope of Change

Intended Phase 1 files:

- `backend/src/test/java/com/synapsecore/Layer2Phase1OperationalFoundationIntegrationTest.java`
- `docs/evidence/layer-2-phase-1-operational-foundation.md`

No frontend, backend production, infrastructure, configuration, deployment,
hosted proof, or unrelated worktree files were changed.

## 21. Final Verdict

Because Classification A remaining is zero and all focused checks passed:

```text
LAYER 2 PHASE 1 - OPERATIONAL FOUNDATION VERIFIED AND CROSS-DOMAIN BASELINE COHERENT
```

This verdict is bounded to the operational foundation described above. It does
not authorize Layer 2 Phase 2, source ingestion, replay, fulfillment, or
hosted proof without a separate scope decision and evidence cycle.
