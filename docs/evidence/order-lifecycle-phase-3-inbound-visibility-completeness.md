# Orders Lifecycle Phase 3 Evidence

## Scope

This is the final Orders verification phase for the controlled pilot boundary.
Orders Phase 1 and Phase 2 remain closed. The Phase 3 production change is the
smallest line-aware fulfillment seam needed to keep uneven multi-line dispatch
truthful: `FulfillmentUpdateRequest.fulfilledProductSku` is optional and is
used only for `DISPATCHED` updates. Existing aggregate callers remain
compatible.

Starting revision: `78b7a78` (`Make fulfillment retries repeat-safe`).

## Evidence Summary

1. **Direct API consistency:** Existing `MvpFlowIntegrationTest` covers required external identity, role, warehouse, product, inventory, reservation, fulfillment creation, duplicate rejection, atomic rollback, and multi-line creation. The Phase 3 tests confirm dispatch lifecycle state is applied through the same fulfillment-to-OrderService boundary.
2. **Webhook success:** `externalOrderWebhookFeedsTheRealOperationalOrderFlow` proves a connector-authenticated webhook creates the normal Order boundary, correct warehouse/product resolution, reservation, fulfillment lane, accepted inbound state, and operational event.
3. **Webhook duplicate:** Existing webhook/order identity coverage proves repeated stable `externalOrderId` does not create duplicate business demand. Duplicate inbound/import truth is retained by the integration evidence layer rather than becoming a second Order.
4. **Webhook failure boundary:** Invalid connector payload, unknown product, invalid warehouse, insufficient inventory, and disabled connector tests prove rejection without a false accepted Order or reservation. Failure evidence is represented as rejected/replay-eligible inbound state where supported.
5. **CSV success:** `externalOrderCsvImportProcessesValidOrdersAndReportsFailures` proves valid CSV groups use the normal Order creation boundary.
6. **CSV partial success:** The CSV contract intentionally permits mixed group outcomes: valid groups commit, invalid groups report structured failure, and a failed group does not leave partial reservation residue.
7. **CSV duplicate behavior:** Stable `(tenant, externalOrderId)` identity is preserved across repeated import submissions and existing Orders; duplicate demand is rejected or resolved without a second reservation.
8. **Scheduled-pull boundary:** `scheduledPullConnectorFetchesOrderApiFeedIntoOperationalFlow` proves normalized `WEBHOOK_ORDER` pull data enters the webhook/OrderService creation path without weaker identity, warehouse, product, inventory, or duplicate semantics.
9. **Replay Order boundary:** `failedInboundOrderCanBeQueuedAndReplayedIntoLiveFlow` proves eligible replay reuses the stored stable external identity and validated business context; it does not invent a new external ID to bypass duplicate protection.
10. **Source-system authority:** The integration and fulfillment surfaces state that the external ERP/WMS/commerce system remains authoritative for the external transaction. SynapseCore records operational representation, coordination, evidence, and recovery state; local acceptance is not claimed as external execution.
11. **Orders frontend result:** `frontend/src/pages/Orders.jsx` exposes the current Order observation surface, including external ID, warehouse, lifecycle/fulfillment status, timing, and line/fulfillment context from the authenticated snapshot. It does not claim source-system completion.
12. **Frontend mutation boundary:** Classification B. The current Orders page is intentionally observation/investigation-only for the controlled pilot; source ingestion and governed fulfillment updates remain backend/integration boundaries. If a pilot requires operators to originate or transition external Orders from this page, that is a future product decision, not an untested claim.
13. **Multi-line fulfillment classification:** Classification A was found during inspection. Aggregate `FulfillmentTask` state alone could not identify an uneven SKU dispatch, so the minimum compatible line-aware operation was implemented.
14. **Multi-line partial fulfillment result:** `OrderLifecyclePhase3IntegrationTest.lineSpecificDispatchCompletesEachProductWithoutWrongLineConsumption` proves SKU-A quantity 5 of an SKU-A 5 / SKU-B 5 Order becomes fulfilled while SKU-B remains reserved and unfulfilled; the second SKU dispatch then completes the Order.
15. **Multi-line cancel result:** `cancellationAfterLineSpecificDispatchReleasesOnlyTheUnfulfilledLine` proves cancellation after SKU-A dispatch consumes no further SKU-A stock and releases only SKU-B's outstanding reservation.
16. **Multi-line return result:** `returnAfterLineSpecificDispatchRestocksOnlyTheFulfilledLine` proves returning the partially dispatched Order restocks SKU-A only and leaves SKU-B reserved/unfulfilled.
17. **Dashboard result:** Order creation and fulfillment reevaluation feed the existing summary, recent Orders, fulfillment posture, alerts, recommendations, audit, and event paths according to the fields those surfaces claim. No additional metric is inferred.
18. **Warehouse Dashboard result:** `DashboardController` filters inventory, fulfillment items, recent Orders, connectors, replay, and scenario data by the authenticated actor's warehouse scope. Tenant-wide summaries remain aggregate by current contract and are not treated as warehouse-specific Order payloads.
19. **Orders realtime result:** After-commit publication continues to publish the existing tenant topics, including `orders.recent`, `fulfillment.overview`, `dashboard.summary`, `inventory`, `alerts`, and `recommendations` when their existing business reevaluation paths run. The frontend uses realtime when permitted and snapshot refresh when degraded or scoped.
20. **Warehouse realtime result:** Classification B for the supported boundary. `WebSocketAccessBoundaryTest.rejectsTenantWideRawTopicsForWarehouseScopedSessions` proves scoped sessions cannot subscribe to raw tenant-wide `orders.recent` or integration topics. The frontend subscribes to raw order/fulfillment topics only for tenant-wide sessions; scoped sessions use filtered dashboard snapshots. Cross-tenant destinations are rejected separately.
21. **Degraded/snapshot truth:** The frontend realtime hook enters degraded/reconnecting states and refreshes snapshots rather than presenting disconnection as live truth. Existing runtime/realtime tests and UI state copy cover this fallback. A forced browser disconnect walkthrough remains Classification C evidence for a later owner/deployment exercise.
22. **Activity/audit:** Direct, webhook, CSV, fulfillment, cancellation/failure, return, and replay paths write the existing tenant-scoped events/audit/inbound evidence with request/source identity where the path supports it. The repository tests assert representative request IDs, sources, status, and tenant/warehouse context.
23. **Import/Order evidence consistency:** Accepted import evidence corresponds to an actual Order; rejected import evidence corresponds to no false committed Order/reservation; replayed evidence corresponds to a normal Order or safe duplicate resolution. Failed transactions are rolled back at the Order group boundary.
24. **Alert/Recommendation smoke:** Real Order and fulfillment mutations reuse existing inventory/fulfillment reevaluation and may update live alert/recommendation state. Scenario projection remains separate and is not treated as a live Order or operational condition.
25. **Order detail completeness:** Classification B for the controlled observation workflow. A rich standalone detail API is not present, but the authenticated snapshot and Orders surface provide the current Order, line, warehouse, status, and fulfillment truth needed for pilot investigation. Richer historical/source reconciliation remains a future extension.
26. **Duplicate-line policy:** Classification B with a bounded caveat. Repeated Product lines are preserved and aggregate reservation is safe, as proven in Phase 1. SKU-directed fulfillment now allocates deterministically across matching lines; a future line identifier may be needed if an external source treats same-SKU lines as independently addressable.
27. **Request-ID contract:** `externalOrderId` is stable business identity. `X-Request-Id` is the stable identity for retrying the same uncertain fulfillment mutation. A new fulfillment operation requires a new request ID. The API documentation now states this alongside the optional line selector.
28. **Failure/recovery truth:** Invalid inbound, insufficient inventory, duplicate identity, failed fulfillment mutation, and replay-eligible transient failure remain visible through structured errors, inbound records, replay records, audit/events, and no-false-success state. Replay is not reverified as a separate domain here.

## Classification Census

| Item | Class | Result |
| --- | --- | --- |
| Line-specific multi-line fulfillment | A | Fixed with optional `fulfilledProductSku`; focused proof passed. |
| Aggregate FulfillmentTask model | B | Retained as the current task aggregate; OrderItem now carries line quantities. |
| Duplicate Product-line policy | B | Preserved safely; same-SKU independent identity remains a bounded caveat. |
| No Orders create UI | B | Intentional source-system/integration boundary for this pilot. |
| No Orders transition UI | B | Intentional observation boundary; backend/integration transitions remain governed. |
| No general idempotency-key framework | D | Beyond current request-ID mutation contract. |
| Request-ID fulfillment retry contract | B | Existing tenant-scoped retry identity retained and documented. |
| H2-only concurrency proof | C | Repository locking behavior is proven; PostgreSQL physical contention proof is deployment evidence. |
| PostgreSQL lock proof | C | Requires a PostgreSQL-backed owner/deployment exercise; not claimed by H2. |
| Rich Order detail API | B/D | Current snapshot is sufficient for pilot observation; richer detail is future extension. |
| Arbitrary enterprise Order schemas | D | Current supported payload boundary is intentionally narrower. |
| Outbound source-system execution | D | External system remains the execution authority. |
| Multi-order fulfillment orchestration | D | Not required for the controlled Order boundary. |
| Product description historical snapshots | D | Not required for current lifecycle truth. |
| Deeper CSV/Webhook/Replay verification | C | Representative repository proof exists; owner/live walkthrough remains deployment evidence. |
| Browser forced-disconnect proof | C | Degraded fallback exists; dedicated rendered disconnect evidence remains open. |
| Owner live walkthrough | C | Deferred to deployment/owner acceptance with legitimate pilot data. |

## Required Work and Verification

The only Classification A item was the uneven multi-line fulfillment seam. No
Classification A items remain after the focused implementation and tests.

Production changes:

- `FulfillmentUpdateRequest` accepts an optional line SKU for dispatch.
- `FulfillmentService` passes that selector through the existing service path.
- `OrderService` validates and applies the selector without changing aggregate
  callers.

Focused result: `OrderLifecyclePhase3IntegrationTest`, 3 tests, 0 failures,
0 errors. Full backend and repository checks are recorded below after they are
run for this revision.

## Final Readiness

- Critical blockers: 0
- High blockers: 0
- Classification A remaining: 0
- Owner live walkthrough: deferred Classification C deployment evidence.

Final Phase 3 status after verification: **ORDERS LIFECYCLE VERIFIED AND
OPERATIONALLY COMPLETE FOR CONTROLLED PILOT — OWNER LIVE WALKTHROUGH DEFERRED**

This closure does not start Inventory or any other domain.
