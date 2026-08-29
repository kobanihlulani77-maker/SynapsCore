# Alerts Lifecycle Phase 1: Identity, Scope, Deduplication, and Delivery

Status: implemented and locally verified on 2026-08-29. This evidence closes
the Alert Phase 1 identity, tenant/warehouse scope, deduplication, and delivery
contract. It does not start Alert Phase 2.

## 1. Scope

This phase hardens existing operational Alerts without expanding the Alert
product surface. The affected paths are inventory evaluation, fulfillment
evaluation, REST reads, dashboard counts, realtime delivery, migration safety,
and the existing Alerts frontend.

## 2. Supported Alert Types

The current model has five operational types:

- `LOW_STOCK`
- `DEPLETION_RISK`
- `FULFILLMENT_BACKLOG`
- `DELIVERY_DELAY_RISK`
- `FULFILLMENT_ANOMALY`

## 3. Classification

The Phase 1 findings are classified as follows:

| Finding | Classification | Result |
| --- | --- | --- |
| Structured Alert identity | A - required operational capability | Implemented |
| Tenant and warehouse integrity | A - required operational capability | Implemented |
| Product/source identity | A - required operational capability | Implemented |
| Scoped REST and dashboard reads | A - required operational capability | Implemented |
| Realtime delivery boundary | A - required operational capability | Implemented |
| Active-condition deduplication | A - required operational capability | Implemented |
| Acknowledgement/manual resolution | B - intentional current boundary | Not added; source conditions control lifecycle |
| Cross-node live concurrency observation | C - evidence gap | PostgreSQL safeguards are present; production concurrency observation remains follow-up evidence |
| Richer assignment, severity, and reconciliation workflows | D - future extension | Out of Phase 1 |

## 4. Structured Identity

`Alert` now stores `tenant`, non-null `warehouse`, optional `product`,
`sourceType`, `sourceRef`, and `conditionKey` as separate fields. Type and
status remain explicit enum values. The display title is presentation data and
is not used for authorization or active-condition identity.

## 5. Tenant Integrity

The entity requires a tenant and warehouse before persistence. Inventory Alerts
derive tenant ownership from the warehouse and fulfillment Alerts derive it
from the fulfillment task. The response exposes the tenant context through the
tenant-scoped endpoint rather than allowing an Alert to float between tenants.

## 6. Migration Safety

`V10__alert_identity_scope_dedup` adds the structured columns, backfills only
deterministically resolvable legacy rows, adds product and warehouse foreign
keys, and rejects orphaned, ambiguous, incomplete, or duplicate active rows.
It does not invent a tenant for ambiguous historical data. The supported
PostgreSQL migration creates the active-condition uniqueness index.

## 7. Warehouse Scope Contract

All five current operational Alert types are warehouse-scoped:

- inventory Alerts identify one product and one warehouse;
- fulfillment Alerts identify one warehouse and have no product identity.

No current Alert type is declared tenant-wide. If a future tenant-wide type is
introduced, it must explicitly carry a tenant-wide source identity rather than
falling through to a display fallback.

## 8. Product and Source Identity

Inventory conditions use `INVENTORY_PRODUCT_WAREHOUSE` and a product/warehouse
source reference. Fulfillment conditions use `FULFILLMENT_WAREHOUSE` and the
warehouse code as their source reference. The response includes `productSku`,
`sourceType`, and `sourceRef` where applicable.

## 9. API Response Contract

`AlertResponse` now includes `warehouseCode`, `productSku`, `sourceType`,
`sourceRef`, and `updatedAt` in addition to the existing alert fields. The
frontend and API documentation use these structured fields instead of parsing
titles.

## 10. Repository Query Shape

Tenant Alert reads use entity graphs for tenant, warehouse, and product. Active
condition lookup uses `(tenant, type, status, conditionKey)`. The old title-based
active lookup is no longer part of the lifecycle path.

## 11. REST Warehouse Scope

`AlertScopeService` loads tenant-owned active/recent Alerts and filters them by
the current operator's warehouse assignments. A North-scoped operator receives
North Alerts only; a Coast-scoped operator receives Coast Alerts only; a
tenant-wide operator receives both. The filter is applied before response
construction.

## 12. Tenant Isolation

All reads remain tenant-code constrained, and the focused integration test
creates two tenants and verifies that neither tenant can read the other's
Alerts. A warehouse scope cannot widen a tenant boundary.

## 13. Role Scope

The current product permits broad Alert read access for supported tenant roles,
but every read remains tenant- and warehouse-constrained. Platform control-plane
access is separate and is not granted by an Alert read. Phase 1 does not change
the role catalog.

## 14. Dashboard Counts

Dashboard active-alert counts use `AlertScopeService.countVisibleActiveAlerts`,
the same visibility rule as the Alert feed. Scoped operators bypass the shared
tenant-wide Redis summary cache, and scoped summaries are not written back to
that cache.

## 15. Runtime Counts

Runtime telemetry uses the same scope-aware active count. This prevents a
warehouse-scoped runtime surface from reporting another warehouse's Alert
pressure.

## 16. Realtime Raw Topic

The raw `/alerts` topic remains available only to sessions that are allowed to
receive tenant-wide raw operational topics. Warehouse-scoped sessions are
rejected from raw Alert subscriptions by `WebSocketConfig`.

## 17. Realtime Scoped Topic

Warehouse-scoped clients subscribe to `/alerts.changed`, a generic changed
signal containing no raw Alert payload. The frontend then refreshes the
filtered REST feed and dashboard summary through the normal tenant/warehouse
scope path.

## 18. Realtime Tenant Boundary

The websocket access boundary continues to reject destinations outside the
session tenant. The focused websocket tests cover cross-tenant rejection,
integration-role restrictions, and rejection of raw Alert subscriptions for
warehouse-scoped sessions.

## 19. Frontend Alert Identity

The Alerts page displays `warehouseCode`, `productSku`, and source metadata from
the API response. It does not derive scope from the title.

## 20. Frontend Dashboard Identity

Dashboard Alert metadata uses an actual warehouse code. `Tenant-wide` is shown
only when the response explicitly identifies a tenant-wide source; otherwise an
unavailable scope is shown instead of inventing one.

## 21. Ownership Wording

The previous `Assign during triage` wording was removed. The current UI says
`Source-condition managed` and explains that the source condition controls the
active/resolved lifecycle. This matches the implemented boundary.

## 22. Acknowledgement Boundary

Acknowledgement and manual Alert assignment/resolution were not implemented in
Phase 1 because the current source-condition lifecycle does not promise those
actions. This is an intentional current boundary, not a hidden capability.

## 23. Active Condition Key

Inventory keys are type plus product id plus warehouse id. Fulfillment keys are
type plus warehouse id. The key is stable across title changes and represents
the operational condition, not a display sentence.

## 24. Database Deduplication Guarantee

On PostgreSQL, migration v10 creates the partial unique index
`ux_alerts_active_condition` over `(tenant_id, condition_key)` for `ACTIVE`
rows. This is the database backstop for one active Alert per condition.

## 25. Transaction Locking

`AlertConditionLockService` serializes condition evaluation in-process and uses
`pg_advisory_xact_lock(hashtext(conditionKey))` on PostgreSQL for app-node
coordination. The lock is held through transaction completion when a transaction
is active.

## 26. Reentrant Evaluation Correction

A nested evaluation of the same condition can occur during order creation when
inventory signals are evaluated before and after fulfillment initialization.
The lock service now reuses the outer lock without incrementing the reentrant
count. This prevents a leaked lock from blocking later fulfillment writes.

## 27. Concurrent Operational Regression

The full backend suite includes the existing concurrent inventory, order, and
fulfillment paths. After the reentrant-lock correction, those paths passed
together with the new Alert tests. The prior intermediate run's H2 timeouts
were reproduced, isolated against the clean pre-Alert revision, and corrected
without weakening concurrency assertions.

## 28. Title Mutation

The focused Alert test changes an existing Alert title and reevaluates the same
condition. The Alert id and condition key remain unchanged, proving that title
changes do not create a second active condition.

## 29. Recurrence and History

When a condition clears, the existing Alert is resolved. A later recurrence may
create a new active history record while the condition key remains stable. The
active uniqueness guarantee does not erase historical evidence.

## 30. Orphan and Ambiguous Rows

The migration fails safely on orphaned tenant references or rows whose warehouse,
product, source, or condition identity cannot be determined. `SeedService` no
longer assigns unowned Alerts to a default tenant.

## 31. Scenario Separation

Scenario preview continues to use `ScenarioAlertProjection` and
`ScenarioRecommendationProjection`. The preview path calculates hypothetical
outputs and does not call the live Alert repository to persist operational
Alerts. Phase 1 includes only a separation smoke check; Scenario approval and
execution remain outside this phase.

## 32. Focused Backend Verification

The focused command covered `AlertLifecyclePhase1IntegrationTest`,
`RealtimeServiceTest`, and `WebSocketAccessBoundaryTest`, with the fulfillment
race included during regression diagnosis. Final focused results were 10 tests,
0 failures, and 0 errors; the combined post-fix verification also passed the
previously failing fulfillment race.

## 33. Full Backend Verification

The required full backend command completed successfully:

```text
Tests run: 234, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Flyway validated and applied the ten-test migrations, including v10, under the
H2 test profile.

## 34. Frontend Verification

The required frontend commands passed:

- `npm.cmd run lint`
- `npm.cmd run build`
- `npm.cmd run verify`

The launch-readiness check inspected 72 frontend source files and the existing
proof-critical labels. The production bundle built successfully.

## 35. Diff Verification

`git diff --check` is required before commit and is recorded with the final
closure result. No generated build output, proof state, environment file, or
Playwright artifact belongs in the commit.

## 36. Evidence Coverage Matrix

| Area | Evidence | Status |
| --- | --- | --- |
| Structured identity | Entity, DTO, service, focused test | Pass |
| Tenant integrity | Entity validation, migration, focused test | Pass |
| Warehouse REST scope | Scoped integration test | Pass |
| Dashboard/runtime scope | Shared scope service and cache guard | Pass |
| Websocket scope | Boundary test and change-signal test | Pass |
| Frontend display | Alerts and Dashboard source review plus verify | Pass |
| Active dedup | Stable key, partial unique index, locking service | Pass |
| Cross-write concurrency | Full inventory/order/fulfillment suite | Pass |
| Scenario isolation | Projection-only path source review | Pass |

## 37. Remaining Evidence Limit

The local tests use H2 for most integration coverage. PostgreSQL-specific
advisory locking and the PostgreSQL partial index are source/migration verified,
but a separately observed multi-node PostgreSQL concurrency run is not part of
this local gate. This is a C evidence gap for later operational validation, not
an unprotected production path.

## 38. Live/Hosted Status

No hosted proof was run for this phase. The change requires backend deployment
and the hosted proof should be rerun after deployment readiness is confirmed.
The local gate therefore proves source behavior and regression safety, not the
new revision's live deployment state.

## 39. Intended Change Boundary

The intended Alert Phase 1 files are the Alert entity, service, repository,
scope/locking services, migration/schema baseline, dashboard/runtime/realtime
consumers, Alert frontend surfaces, focused tests, API/evidence documentation,
and related realtime assertions. Unrelated local changes, including
`frontend/Dockerfile`, `.gitattributes`, and prior scenario evidence files,
must remain untouched and unstaged.

## 40. Phase 1 Verdict

The required operational defects were fixed: Alert identity is structured,
tenant and warehouse visibility is enforced consistently, scoped realtime
delivery is payload-safe, dashboard counts use authoritative scope, and active
deduplication is backed by PostgreSQL constraints plus transaction locking.

**ALERTS PHASE 1 IMPLEMENTED AND LOCALLY VERIFIED.**

Alert Phase 2 is not started. The hosted deployment proof and a dedicated
PostgreSQL multi-node concurrency observation remain follow-up evidence.
