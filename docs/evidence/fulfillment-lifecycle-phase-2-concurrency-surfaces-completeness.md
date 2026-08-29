# Fulfillment / Dispatch Phase 2: Concurrency, Operational Signals, Surfaces, and Completeness

Status: verified locally for the controlled-pilot contract; owner-managed live walkthrough remains deferred

Date: 2026-08-29

## 1. Starting point and evidence boundary

- Starting HEAD: `233b37b4182f98c371ba07d161a3ca3eed0cfae0`
- Test profile: Spring `test`, isolated H2 database, Flyway migrations, MockMvc.
- Focused class: `FulfillmentLifecyclePhase2IntegrationTest`.
- Full backend run after the focused evidence and assertion maintenance: 225 tests, 0 failures, 0 errors.
- Frontend verification: `npm.cmd run verify` passed; launch-readiness checked 72 source files and the Vite production build passed.
- PostgreSQL-specific execution, forced browser reconnect, and the owner-managed live `OWNER-ACCEPT-02` walkthrough are not claimed by this local evidence.
- No production runtime behavior was changed in Phase 2.

The full run exposed one stale pre-existing race assertion in `OrderLifecyclePhase2IntegrationTest`: a legitimate serialized loser returned HTTP 409, while the assertion allowed only 200/400. The assertion was aligned with the established conflict contract; the affected test then passed and the full suite returned green.

## 2. Requested Phase 2 results

1. **Starting HEAD:** `233b37b4182f98c371ba07d161a3ca3eed0cfae0`.
2. **Different-SKU concurrency:** PASS. Concurrent dispatches for two lines both committed; both line quantities, reservations, inventory rows, order status, and aggregate task count agree.
3. **Same-SKU concurrency:** PASS. Concurrent +3 and +4 dispatches accumulated to 7 fulfilled and 3 reserved, then the final +3 completed the line without a lost update.
4. **Partial-dispatch/cancel race:** PASS. The order ended cancelled with consumed units preserved, remaining units cancelled, reservation zero, and inventory matching the consumed quantity. A serialized loser may return 400/409.
5. **Partial-dispatch/exception race:** PASS. The order ended failed; consumed units were preserved and only the outstanding reservation was released. A serialized loser may return 400/409.
6. **Delivery/dispatch race:** PASS. Direct `DELIVERED` catch-up won or followed the partial update without over-consumption; all lines and the task reached delivered/complete truth.
7. **Delay/dispatch race:** PASS for the current contract. The race produced a valid delayed/blocked state or a rejected competing update; no contradictory completed state was accepted.
8. **Exception/delivery race:** PASS. Exactly one terminal operational truth remained: failed/exception with released reservation, or delivered with fulfilled reservation consumption.
9. **Multi-Order shared-Inventory concurrency:** PASS. Two orders sharing one product committed without oversell and both persisted coherent fulfilled state.
10. **Duplicate same-SKU classification:** **B, intentional boundary.** The current order model matches fulfillment by SKU and preserves aggregate quantities. It does not promise distinct business identity for duplicate SKU lines; that is acceptable only while the pilot does not assign separate meaning to those lines.
11. **One-task-per-Order classification:** **B, intentional boundary.** One task supports repeated partial dispatches, multiple products, carrier/tracking replacement, and final delivery for the agreed single-warehouse order lane.
12. **Split-warehouse classification:** **B for the current pilot, D for expansion.** Orders are warehouse-owned and the current contract does not represent one order being fulfilled from North and Coast simultaneously.
13. **External shipment/event identity classification:** **B for the current integration contract, D for richer shipment identity.** `X-Request-Id` is the stable mutation/retry identity; there is no separate shipment or fulfillment-event entity.
14. **Carrier/tracking model:** PASS for current scope. The latest carrier/tracking pair is persisted as current task evidence across partial updates; distinct per-parcel identity is not modeled.
15. **Active backlog:** PASS. Actionable `QUEUED`, `PICKING`, `PACKED`, and applicable delayed work are visible; cancelled, failed, returned, and delivered orders are excluded.
16. **Overdue dispatch:** PASS. An unfinished task past `promisedDispatchAt` contributes overdue pressure; terminal/completed work does not.
17. **Delivery-delay signal:** PASS. Delivery-risk state is generated for the correct tenant/warehouse/task context and resolves when the condition clears through the existing alert lifecycle.
18. **Backlog Alert:** PASS. `FULFILLMENT_BACKLOG` is produced from real warehouse pressure and is scoped to the warehouse that has the backlog.
19. **Anomaly signal:** PASS. `EXCEPTION` produces active `FULFILLMENT_ANOMALY` evidence and the corresponding logistics-anomaly recommendation while the failed task is no longer actionable execution work.
20. **Fulfillment Recommendations:** PASS. `PRIORITIZE_FULFILLMENT`, `ESCALATE_LOGISTICS`, and `INVESTIGATE_LOGISTICS_ANOMALY` are derived from persisted fulfillment conditions, not scenario projection.
21. **Recommendation dedup:** PASS. Repeating an identical anomaly condition does not increase the matching recommendation within the deduplication window; changed pressure may legitimately create a refreshed recommendation.
22. **Dashboard result:** PASS by source and integration coverage. Dashboard fulfillment counts derive from the fulfillment overview and alert/recommendation state, preserving active, delayed, overdue, and risk distinctions.
23. **Warehouse Dashboard result:** PASS by service filtering and existing boundary coverage. Warehouse-scoped views use the order/task warehouse and do not expose another warehouse's pressure.
24. **Realtime result:** PASS by source/test coverage. Fulfillment mutation publishes the existing after-commit operational refresh path for `fulfillment.overview`, `orders.recent`, and `dashboard.summary`; websocket success is not required for the database mutation.
25. **Warehouse realtime result:** PASS by authorization coverage. Existing websocket boundary tests reject broad tenant collections for warehouse-scoped identities while allowing the permitted scoped topic.
26. **Degraded/reconnect result:** **C, evidence gap.** Existing shared frontend/runtime behavior and source evidence cover degraded and reconnecting states, but this phase did not inject a browser transport failure and observe a fresh fulfillment snapshot.
27. **Frontend operational completeness:** **B, sufficient for the intended workflow.** The Fulfillment page exposes order, warehouse, status, quantity/impact, timing, delay/exception, carrier/tracking, and risk context. It is observation-first because supported fulfillment writes arrive through integrations/APIs.
28. **External execution boundary:** PASS. The UI and docs describe recorded/received fulfillment evidence and coordination; SynapseCore does not claim to physically dispatch a shipment or replace the source execution system.
29. **Activity/audit result:** PASS by Phase 1 evidence and current audit detail. Consequential updates carry actor/source, tenant, warehouse/order context, product/quantity where applicable, status, request ID, and timestamp.
30. **Failure visibility:** PASS. Invalid transitions, wrong SKU/warehouse, over-quantity, terminal mutation, and retry payload mismatch are rejected and remain diagnosable through response/audit/runtime evidence.
31. **PostgreSQL proof classification:** **C, evidence gap.** The focused concurrency proof uses H2. The production code uses pessimistic order/task/inventory locks and database constraints, but a PostgreSQL-shaped run was not executed in this phase.
32. **`occurredAt`/history classification:** **B for current scope, D for richer history.** Current state plus consequential audit evidence satisfies the controlled pilot contract; a durable detailed event timeline is not present.
33. **Consolidated limitations/gaps:** See Section 4.

## 3. A/B/C/D classification table

| Area | Class | Evidence and closure meaning |
| --- | --- | --- |
| Same-order multi-SKU races | A | Required capability; 2/2 concurrent updates and aggregate state passed. |
| Same-SKU partial accumulation | A | Required capability; no lost update or oversell passed. |
| Cancel/fail/delivery terminal races | A | Required capability; serialized outcomes and accounting passed. |
| One-update retry identity | A | Existing Phase 1 contract; same fingerprint is repeat-safe and changed payload conflicts. |
| Active backlog and overdue pressure | A | Required operational signals passed with terminal exclusion. |
| Delay and exception signals | A | Required signals, alert sync, and recommendation mapping passed. |
| Dashboard and warehouse truth | A | Existing service/integration/boundary evidence supports the pilot surface. |
| Realtime topic publication and authority | A | Existing publisher and websocket boundary tests cover the current contract. |
| Operator observation surface | A | Existing Fulfillment UI exposes the required current operational context. |
| Source-system execution boundary | A | Product language preserves the external execution authority. |
| Duplicate SKU line identity | B | SKU-level aggregation is the explicit current model. |
| One task per order | B | Fits the agreed one-warehouse incremental fulfillment lane. |
| Split warehouse/order | B/D | Not part of the current pilot; future capability if required. |
| Shipment/event entity identity | B/D | Request ID is sufficient for current retry semantics; richer identity is future. |
| Latest carrier/tracking pair | B/D | Current task evidence is truthful; parcel-level history is future. |
| Direct delivery catch-up | B | Whole-order external snapshot semantics are explicit. |
| API/integration-only fulfillment mutation | B | Current workflow is source/integration-fed and command-center observed. |
| Detailed durable fulfillment timeline | B/D | Audit is sufficient now; richer event history is future. |
| PostgreSQL-specific concurrency run | C | Capability is implemented and H2-tested; deployment-shaped proof remains. |
| Forced browser reconnect proof | C | Capability is present in shared runtime behavior; fresh fault injection remains. |
| Owner live walkthrough | C | Deployment/owner-managed evidence, not manufactured by local tests. |
| Outbound WMS/carrier execution | D | SynapseCore records and coordinates; external systems remain execution authority. |

## 4. Consolidated limitation and gap census

- **One `FulfillmentTask` per order:** current model; supports repeated updates but not independently tracked shipments.
- **No multiple-shipment entity:** future extension if one order needs separate parcel, carrier, or delivery lifecycles.
- **No split-warehouse fulfillment:** current order is warehouse-owned; North plus Coast allocation is outside the controlled pilot contract.
- **No separate external shipment/event ID:** `X-Request-Id` provides mutation retry identity; richer source event identity is future if integrations require it.
- **`X-Request-Id` integration contract:** callers must supply stable IDs for retry safety; changed payload reuse is rejected.
- **Duplicate same-SKU line ordering:** fulfillment totals are SKU-based; distinct line-level business meaning is not represented.
- **Aggregate task counter:** one order task counts fulfilled units across order lines; it is not a per-shipment ledger.
- **Direct-delivery catch-up:** `DELIVERED` represents a whole-order source snapshot and consumes remaining reservations once.
- **API-only fulfillment mutation:** no operator dispatch control is implied; the intended workflow observes supported source/integration updates.
- **Carrier/tracking informational model:** the latest pair is current task evidence, not a parcel history.
- **No durable detailed fulfillment timeline:** audit/current task state cover the current pilot; a richer event timeline is future.
- **PostgreSQL concurrency proof:** local focused tests run on H2; production-shaped PostgreSQL evidence remains C.
- **Browser forced reconnect evidence:** shared behavior exists, but a dedicated injected-failure browser run remains C.
- **Outbound WMS/carrier execution:** not claimed; external systems remain authoritative execution systems.
- **Owner live walkthrough:** `OWNER-ACCEPT-02` is owner-managed and remains C until exercised by the owner.

These are not A-class blockers for the stated controlled pilot lane. The model must be revisited if the pilot requires multi-warehouse orders, independently delivered parcels, or direct operator dispatch execution.

## 5. Required operational work and fixes

35. **Required operational work found:** None. The focused evidence found no A-class missing capability for the stated controlled-pilot contract.
36. **Production/frontend fixes:** None. No production runtime or frontend behavior changed. One stale pre-existing order concurrency test assertion was widened to accept the repository's legitimate HTTP 409 conflict result, and the Phase 2 test/evidence were added.
37. **Focused tests:** `FulfillmentLifecyclePhase2IntegrationTest`: 10 tests, 0 failures, 0 errors.
38. **Full backend:** 225 tests, 0 failures, 0 errors after the assertion maintenance.
39. **Frontend checks:** `npm.cmd run verify` passed, including frontend check and production build.

## 6. Files, commits, and gate status

40. **Files changed for this closure:**
   - `backend/src/test/java/com/synapsecore/FulfillmentLifecyclePhase2IntegrationTest.java`
   - `backend/src/test/java/com/synapsecore/OrderLifecyclePhase2IntegrationTest.java` (stale 409 race assertion maintenance)
   - `docs/evidence/fulfillment-lifecycle-phase-2-concurrency-surfaces-completeness.md`

   Unrelated local files were preserved and are not part of this closure: `frontend/Dockerfile`, `.gitattributes`, `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`, and `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`.

41. **Commit:** intended closure commit will contain only the files above; no environment files, generated reports, or build output are staged.
42. **Critical blockers:** 0.
43. **High blockers:** 0.
44. **Classification A remaining:** 0.
45. **Owner walkthrough status:** deferred and owner-managed; no synthetic operational evidence was manufactured.
46. **FULFILLMENT FINAL READINESS:** Ready for the controlled pilot contract, with PostgreSQL-shaped, browser reconnect, and owner walkthrough evidence explicitly outstanding as C items.
47. **PHASE 2 VERDICT:** **FULFILLMENT / DISPATCH LIFECYCLE VERIFIED AND OPERATIONALLY COMPLETE FOR CONTROLLED PILOT - OWNER LIVE WALKTHROUGH DEFERRED**

No next domain phase is started by this document.
