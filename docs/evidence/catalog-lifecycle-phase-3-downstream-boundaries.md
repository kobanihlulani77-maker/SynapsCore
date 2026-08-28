# Catalog / Products Phase 3 Evidence

**Scope:** Downstream integrity and boundary verification after the accepted Catalog / Products Phase 1 and Phase 2 work.

**Verification date:** 2026-08-28

**Phase status:** Focused downstream boundary verification complete. Inventory and other feature-lifecycle domains were not started.

## 1. Starting HEAD

`939a03cafcf19826b2a2c1ac480ba599921712bc`

The existing unrelated worktree items were preserved and not staged:

- `frontend/Dockerfile`
- `.gitattributes`
- `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`
- `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`

## 2. Product to Inventory

Catalog Product has no Warehouse relationship. Product create, update, and CSV import persist tenant-wide master data only. They do not create Inventory rows and do not change inventory quantities.

The focused test created a Product, created its Inventory baseline through the supported inventory API, then updated the Product directly and through CSV. The Inventory row remained present with the same `quantityAvailable` and `quantityOnHand` values.

Result: **PASS.**

## 3. Product to Order Resolution

Order creation resolves each submitted operator-facing SKU through:

```text
OrderService
  -> InventoryService.requireProduct(tenantCode, productSku, ...)
  -> ProductRepository tenant + catalogSku lookup
  -> OrderItem product foreign-key binding
```

A known catalog SKU created an order and returned the expected SKU and Product name. The Product is selected within the active tenant; the order request does not select a Product by an unscoped database identifier.

Result: **PASS.**

## 4. Unknown SKU

An order containing a SKU absent from the active tenant returned HTTP `404 Not Found`. No Product was created as a side effect and no order was persisted for that request.

Result: **PASS.**

## 5. Cross-Tenant SKU

The focused test provisioned a separate H2 test tenant, created `PHASE3-CROSS-TENANT-100` there, and attempted to order that SKU from `STARTER-OPS`. The active-tenant lookup returned HTTP `404 Not Found`; the SKU remained absent from `STARTER-OPS` and present only in the second tenant.

The same catalog SKU is therefore allowed to exist in separate tenants, while operational resolution remains tenant-scoped.

Result: **PASS.**

## 6. Historical Order After Product Update

The current implementation stores the OrderItem Product foreign-key relationship, quantity, reservation/fulfillment facts, unit price, and order-level facts. It does not store a Product name/category snapshot on OrderItem.

After creating an order, updating the Product name and category, and reading the order again:

- order ID and external order ID were unchanged;
- order status, warehouse, creation time, quantity, reservation, and unit price were unchanged;
- the displayed Product name followed the current Product master record.

Result: **PASS for the current contract.**

## 7. Historical-Truth Contract

The current contract is:

```text
transactional order facts remain historical
  quantity, price, reservation, fulfillment, status, warehouse, timestamps

Product descriptive master data remains live
  current catalog SKU/name/category are resolved from Product at read time
```

This is not immutable Product-description history. If a pilot requires historical Product name/category snapshots, that is a future product/data-model decision, not an unreported Phase 3 defect.

The focused test compares persisted timestamps within a 1 ms tolerance because H2 and PostgreSQL can serialize database timestamps at different sub-millisecond precision.

## 8. SKU / catalogSku / internalSku Consistency

The tested and existing response contract is:

| Field | Current meaning |
| --- | --- |
| `sku` | Compatibility-facing normalized catalog SKU |
| `catalogSku` | Normalized operator/integration-facing catalog SKU |
| `internalSku` | Tenant-qualified identity in the form `TENANT::CATALOGSKU` |
| `tenantCode` | Owning tenant/workspace code |

The round-trip test submitted a lower-case SKU and verified normalized uppercase `sku` and `catalogSku`, plus the tenant-qualified `internalSku`. Existing Catalog Phase 1 evidence covers the broader identity and normalization rules.

Result: **PASS.** The duplicated compatibility fields remain a documented integration-review consideration, not a demonstrated defect.

## 9. Product to Alert

Product create, update, and CSV import do not invoke live Alert persistence or low-stock evaluation. The focused test captured the tenant Alert row count before and after catalog mutations and observed no change.

Inventory changes may evaluate operational alert rules, but that is a separate Inventory boundary and was not conflated with a catalog mutation.

Result: **PASS.**

## 10. Product to Recommendation

Product create, update, and CSV import do not invoke live Recommendation persistence. The focused test captured the tenant Recommendation row count before and after catalog mutations and observed no change.

Recommendation calculation may consume operational inventory/order facts elsewhere; catalog master-data refresh does not itself assert a live recommendation.

Result: **PASS.**

## 11. `INVENTORY_UPDATE` Event Consumer and Result

`ProductService.recordCatalogChange` performs three distinct actions:

1. records the catalog business event (`PRODUCT_CATALOG_UPDATED` or the relevant catalog action) with source `product-catalog`;
2. publishes the generic operational refresh type `INVENTORY_UPDATE` with source `product-catalog`;
3. records the tenant audit success entry.

The generic event is consumed after commit by `OperationalStateChangeListener`, which drains `OperationalDispatchWorkItem` rows through `OperationalDispatchQueueService`. For a non-integration update such as this one, the queue refreshes the dashboard summary and invokes tenant-scoped realtime broadcasts. It does not create Inventory, Alert, or Recommendation entities.

This is a refresh signal, not a claim that physical inventory changed.

Result: **PASS by source trace plus existing operational-dispatch/realtime test coverage.**

## 12. Realtime Tenant Scope

`RealtimeService.broadcastOperationalUpdates(tenantCode)` normalizes the explicit tenant and executes its reads inside that tenant context. Published destinations use tenant-qualified topics such as:

```text
/topic/tenant/{TENANT}/dashboard.summary
/topic/tenant/{TENANT}/inventory
/topic/tenant/{TENANT}/orders
/topic/tenant/{TENANT}/alerts
/topic/tenant/{TENANT}/recommendations
```

Catalog refresh therefore causes a tenant-scoped refresh/broadcast, not a cross-tenant Product or operational payload broadcast. Existing realtime service and dispatch tests cover the topic set and tenant argument. No new browser realtime run was required because no production or frontend code changed.

Result: **PASS.**

## 13. Product to Scenario Boundary

Scenario projection uses tenant + `catalogSku` Product resolution and separately requires warehouse Inventory for a projection. Product catalog writes do not mutate Scenario history or projection state.

This phase did not reopen or change the Scenario lifecycle. The accepted Scenario evidence remains the authority for approval, assignment, replay, and execution governance. The Catalog conclusion is limited to the boundary: Product is available as tenant-scoped master data; Scenario projection must calculate against the requested tenant and warehouse context.

Result: **PASS by source boundary; Scenario lifecycle remains unchanged.**

## 14. Dashboard Effect

The dashboard `totalProducts` value is currently inventory-backed through `countDistinctProductsByTenantCode`, not a count of catalog-only Product rows. A catalog-only Product therefore does not increment that metric.

The focused test verified that catalog creation did not change:

- `totalProducts`;
- `totalOrders`;
- `activeAlerts`;
- `lowStockItems`;
- `recommendationsCount`;
- `inventoryRecordsCount`.

The expected behavior is a dashboard refresh signal with unchanged operational metrics when no operational data changed.

Result: **PASS.**

## 15. Activity and Audit Truth

Successful Product mutations create catalog business/audit evidence with the catalog source and action. They do not create an operational activity record claiming an inventory, alert, recommendation, or order event occurred.

The Phase 2 catalog transaction proof established that Product, catalog event, audit, and dispatch writes share the catalog transaction boundary. The Phase 3 focused run observed the expected catalog log path and unchanged operational counts. No false operational activity was demonstrated.

Result: **PASS with the existing catalog-vs-operational evidence distinction.**

## 16. Frontend Identity

The existing Catalog frontend displays the normalized `product.sku` and uses the workspace SKU language already covered by Catalog Phase 1/2 evidence. The API response keeps `catalogSku` and `internalSku` distinct; no frontend source changed in this phase.

Result: **Existing frontend identity evidence remains accepted; no new frontend check required.**

## 17. Frontend Role Boundary

The Catalog surface continues to rely on the backend Tenant Admin mutation guard. Catalog Phase 1 evidence established read access versus Tenant Admin-only create/update/import authority; the Phase 3 negative smoke additionally confirmed a non-admin Product create returned HTTP `403 Forbidden`.

Hidden or disabled UI controls are not treated as the security boundary. No role or frontend behavior was changed.

Result: **PASS.**

## 18. Direct Negative Smoke

The focused test directly exercised:

- unknown Product update ID: HTTP `404 Not Found`;
- non-admin Product create: HTTP `403 Forbidden`;
- internal tenant-qualified SKU supplied as a writable catalog SKU: HTTP `400 Bad Request`;
- unknown order SKU: HTTP `404 Not Found`;
- cross-tenant order SKU: HTTP `404 Not Found`.

The expected denial responses were intentional boundary assertions, not unexpected failures.

Result: **PASS.**

## 19. Hosted Proof Status

The accepted Catalog Phase 2 closure contains the existing deployed Catalog onboarding proof:

```text
1 test passed
```

It exercised tenant-scoped Product API and browser Catalog onboarding against the deployed services. The deployed connection classification at that closure was:

```text
FRONTEND_UP=True
BACKEND_UP=True
DB_READY=True
AUTH_READY=True
WS_READY=True
PROOF_ALLOWED=True
```

No hosted proof was rerun for Phase 3 because this phase added only focused backend test coverage and evidence; no production or frontend runtime code changed. No new hosted tenant was created and no `OWNER-ACCEPT-02` data was manufactured or modified.

## 20. Production Defects

None found in the Phase 3 scope.

The source and focused tests support the intended boundary that catalog master-data changes are not operational inventory/order/alert/recommendation mutations.

## 21. Test / Fixture Defects

The focused harness required three non-production corrections during execution:

- order fixtures initially used `TENANT_ADMIN`; the Order API correctly requires `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN` for order creation;
- the dashboard expectation initially treated `totalProducts` as a catalog count; the implementation correctly defines it from inventory-backed distinct products;
- H2 timestamp serialization differed from the immediate response by sub-millisecond precision; the assertion now checks the persisted value within 1 ms.

These were test expectation/fixture issues. No production workaround was made.

## 22. Fixes

Test-only fixes:

- corrected order-creation fixtures to `INTEGRATION_OPERATOR`;
- corrected the dashboard assertion to the inventory-backed metric contract;
- made the historical timestamp assertion database-precision tolerant.

No backend runtime, API contract, frontend code, migration, or deployment configuration was changed.

## 23. Focused Tests

Command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd -Dtest=CatalogDownstreamBoundaryIntegrationTest test
```

Result:

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The run also validated the test-profile startup path: Hikari, Flyway migrations `v1` through `v7`, JPA/EntityManager, Spring MVC test dispatch, and the in-memory websocket broker initialized successfully.

## 24. Full Backend Result

Not rerun. No production code changed, and the Phase 3 brief explicitly limits verification to the focused downstream suite unless runtime behavior changes. The accepted Phase 2 baseline remains `179` backend tests passed.

## 25. Frontend Checks

Not rerun. No frontend files changed. Existing Catalog frontend identity/authority evidence and the accepted hosted Catalog onboarding proof remain valid prior evidence.

## 26. Files Changed

Phase 3 files:

- [`CatalogDownstreamBoundaryIntegrationTest.java`](../../backend/src/test/java/com/synapsecore/CatalogDownstreamBoundaryIntegrationTest.java) — focused downstream boundary integration coverage.
- [`catalog-lifecycle-phase-3-downstream-boundaries.md`](catalog-lifecycle-phase-3-downstream-boundaries.md) — this evidence record.

Unrelated worktree files were preserved and not staged; see Section 1.

## 27. Commits

No commit was created in this phase. No production code or frontend code changed.

## 28. Deployment / Live Readiness

No deployment was required or performed. The accepted hosted Catalog baseline remains `PROOF_ALLOWED=True` from Phase 2 closure evidence. A hosted rerun is not justified without production or frontend runtime changes.

## 29. Critical Blockers

None identified.

## 30. High Blockers

None identified for the Catalog / Products downstream boundary scope.

## 31. Medium / Low Limitations

- Focused Phase 3 tests use the repository H2 `test` profile, not a new live PostgreSQL run.
- Realtime tenant scoping is supported by source inspection and existing dispatch/realtime tests; no new hosted browser subscription capture was needed.
- Order responses resolve the current Product descriptive name; immutable historical Product name/category snapshots are not part of the current contract.
- Dashboard `totalProducts` is inventory-backed, so a catalog-only Product is not represented in that metric until Inventory exists.
- Product deletion/deactivation, catalog versioning, pricing history, external Product IDs, and optimistic locking remain outside current supported scope.
- Cross-tenant Product update denial is covered by accepted Phase 1 tenant-ownership evidence rather than duplicated in this Phase 3 class.

## 32. `OWNER-ACCEPT-02` Status

Not used, modified, or recreated. Phase 3 used isolated H2 fixtures and the existing accepted Catalog/Products hosted evidence. No manual live tenant data was changed.

## 33. Catalog Final Readiness

**READY.** Product downstream boundaries are consistent with the current implementation and accepted Catalog Phase 1/2 contracts. No Critical or High blocker was identified.

## 34. Phase 3 Verdict

**CATALOG / PRODUCTS LIFECYCLE VERIFIED — READY TO SELECT NEXT DOMAIN**

The Catalog / Products lifecycle is verified through its downstream boundaries: tenant-scoped identity, order resolution, inventory separation, catalog-only dashboard refresh behavior, audit truth, realtime tenant scope, and negative authorization. Inventory lifecycle work was not started.
