# Recommendations Lifecycle Phase 1

## Verdict Scope

Phase 1 establishes the live Recommendation identity, authority, currentness,
and deduplication contract. Recommendations remain advisory. They do not execute
inventory, order, fulfillment, integration, replay, or ERP/WMS actions. Scenario
projections remain Scenario-local and do not create live Recommendation rows.

## Identity Model

Every Recommendation is now explicitly owned by a tenant and warehouse. The
structured fields are:

| Field | Meaning |
| --- | --- |
| `tenant` | Owning workspace; required and database-enforced. |
| `warehouse` | Primary operating warehouse; required. |
| `product` | Required for inventory-derived advice; absent for fulfillment advice. |
| `sourceType` | Bounded source family: `INVENTORY` or `FULFILLMENT`. |
| `sourceRef` | Stable source reference for the evaluated operational object. |
| `conditionKey` | Stable current-condition identity; not derived from display text. |
| `sourceWarehouse` | Transfer source warehouse, when applicable. |
| `destinationWarehouse` | Transfer destination warehouse, when applicable. |
| `suggestedQuantity` | Advisory transfer quantity, when applicable. |
| `status` | `CURRENT` or `RETIRED`. |
| `createdAt` / `updatedAt` | Lifecycle creation and refresh timestamps. |

Inventory advice uses `INVENTORY|product-id|warehouse-id`, so priority and
wording can change without creating a second current condition. Fulfillment
advice uses `FULFILLMENT|warehouse-id`, because the bounded assessment produces
one current warehouse fulfillment condition with the strongest applicable type.

## Type Identity

| Type | Source | Primary scope | Product | Transfer fields |
| --- | --- | --- | --- | --- |
| `REORDER_STOCK` | Inventory pressure | One warehouse | Required | None |
| `REORDER_URGENTLY` | Critical inventory pressure | One warehouse | Required | None |
| `TRANSFER_STOCK` | Inventory shortfall with network surplus | Destination plus source | Required | Both required |
| `PRIORITIZE_FULFILLMENT` | Fulfillment backlog | One warehouse | Not applicable | None |
| `ESCALATE_LOGISTICS` | Delivery delay | One warehouse | Not applicable | None |
| `INVESTIGATE_LOGISTICS_ANOMALY` | Fulfillment anomaly | One warehouse | Not applicable | None |

## Authority and Privacy

REST, Dashboard snapshot/counts, and realtime now use the same current set and
the same backend scope rule. A warehouse-scoped operator sees only advice for a
warehouse they are assigned to. `TRANSFER_STOCK` is stricter: the operator must
be assigned to both the source and destination warehouses. A tenant-wide
operator may see all current advice in that tenant. Cross-tenant reads remain
blocked by tenant context.

The raw `/recommendations` realtime topic is rejected for warehouse-scoped
sessions. Those sessions receive `/recommendations.changed` and refresh through
the filtered tenant API. A raw recommendation payload is therefore never sent
to a scoped browser for frontend filtering to clean up later.

## Currentness and Deduplication

`CURRENT` means the source condition is still present. `RETIRED` means the
source condition no longer justifies the advice. Age is not currentness.

The service updates the existing current row when the condition persists,
including priority upgrades, priority downgrades, wording changes, and transfer
quantity/source refreshes. When the source condition clears, the row is retired
and removed from the current API/dashboard/realtime feed. A later recurrence
creates a new current lifecycle while preserving the retired row.

PostgreSQL migration `V11__recommendation_identity_scope_currentness` adds the
structured columns, safely backfills recognizable legacy titles, refuses
ambiguous orphaned tenant rows, preserves history, and creates the partial
unique index `ux_recommendations_current_condition` on `(tenant_id, condition_key)`
for `CURRENT` rows. The service also serializes same-process condition
evaluation and uses a pessimistic row lock when a current row exists.

## Transfer Currentness

Transfer advice is advisory only. Changing the source inventory reevaluates
current transfers that depend on that source and retires advice when the source
no longer has safe surplus. Replenishing the destination or clearing its
shortfall retires the destination condition. A destination evaluation can
refresh the same condition with a different valid source.

## Boundaries Preserved

- Scenario preview calls projection logic only; it does not persist or publish a
  live Recommendation.
- Recommendation persistence remains inside the source inventory/fulfillment
  transaction for this phase. Transaction failure coupling is intentionally
  observed for the later lifecycle phase rather than redesigned here.
- Recommendations have no accept, dismiss, complete, or execute API. They are
  system-derived advisory guidance, not an action queue.
- Alert `recommendedAction` remains copied advisory text. Alert lifecycle and
  Recommendation lifecycle remain separate.
- Historical search, full retirement reconciliation, forced realtime disconnect
  testing, and multi-node PostgreSQL load behavior remain outside Phase 1.

## Classification

| Area | Classification | Result |
| --- | --- | --- |
| Tenant integrity | A | Fixed with required entity field and migration guard. |
| Structured warehouse/product/source identity | A | Fixed with typed relations and source fields. |
| Cross-warehouse transfer authority | A | Fixed and enforced as both-warehouse access. |
| REST scope | A | Fixed through `RecommendationScopeService`. |
| Dashboard scope/count | A | Fixed to filtered `CURRENT` advice. |
| Realtime scope | A | Fixed with raw-topic rejection and changed-signal refresh. |
| Frontend contract | A | Fixed to render structured scope, product, status, and update time. |
| Currentness/status | A | Fixed with `CURRENT` and `RETIRED`. |
| Condition identity | A | Fixed with stable inventory/fulfillment condition keys. |
| Active uniqueness | A | Fixed with PostgreSQL partial unique index plus locking. |
| Time-window deduplication | A | Removed as business identity; no 1,800-second rule remains. |
| Priority refresh | A | Same current row is refreshed up or down. |
| Condition clear/recurrence | A | Retirement and new recurrence lifecycle implemented. |
| Scenario separation | B | Existing intentional boundary preserved. |
| Operator action controls | B | Intentional advisory-only boundary preserved. |
| Multi-node concurrency evidence | C | Database invariant exists; distributed load proof belongs to a later phase. |
| History/search | D | Not required for current operator feed. |

Classification A remaining: **0**.

## Focused Verification

- `RecommendationLifecyclePhase1IntegrationTest`: current refresh preserves the
  row ID, clear retires the row, recurrence creates a new row, and transfer
  identity persists product/source/destination/quantity.
- `RecommendationScopeServiceTest`: warehouse-scoped operators cannot see a
  foreign warehouse or a cross-warehouse transfer without both scopes; tenant-
  wide operators can see both warehouse lanes.
- `MvpFlowIntegrationTest#recommendationsEndpointReturnsStructuredActionGuidance`:
  existing API behavior and migrated Spring context remain green.
- `RealtimeServiceTest`: existing operational topics plus the recommendation
  change signal remain green.

Repository-wide verification completed with the full backend suite covering 26
test classes and 238 tests: 0 failures, 0 errors, and 0 skipped. The focused
Recommendations tests and existing frontend checks also passed. No hosted proof
was run because this phase did not change the deployed runtime contract.

## Phase 1 Acceptance

Critical blockers: **0**
High blockers: **0**
Phase 2 readiness: **Ready after full verification closure**.
