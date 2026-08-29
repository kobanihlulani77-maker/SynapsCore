# Inventory Lifecycle Phase 3: Operational Intelligence and Runtime Truth

Status: technically verified from the existing repository test evidence. No production or frontend code was changed in this phase. OWNER-ACCEPT-02 manual operational walkthrough remains deferred.

## Evidence boundary

- Starting HEAD: `876c1b785681c6959877bcaafb91816bee1f43d2` (`876c1b7 Harden inventory concurrency and order effects`).
- Focused command: `cmd /c mvnw.cmd -Dtest=MvpFlowIntegrationTest,PlatformTenantAccessBoundaryIntegrationTest,RealtimeServiceTest,OperationalStateChangeListenerTest,OperationalDispatchQueueServiceTest,WebSocketAccessBoundaryTest test`.
- Focused result: `121` tests, `0` failures, `0` errors; `BUILD SUCCESS`.
- The integration suites use the existing H2 test profile. This is repository proof, not a replacement for owner-managed PostgreSQL/Render evidence.
- No hosted proof was run. No credentials, proof-state files, generated reports, or local-only artifacts are included.

## 1. Low-stock alert creation

**Result: PASS.** `MvpFlowIntegrationTest.inventoryUpdateFlowMarksLowStockImmediately` verifies that a real Inventory update crossing the configured threshold produces an active low-stock alert and an Inventory recommendation for the correct SKU. `InventoryIntelligenceService` calculates `quantityAvailable <= reorderThreshold`, and `AlertService.syncInventoryAlerts` persists the operational condition only after the real mutation path.

The alert title is display text, while tenant, warehouse, Product, source, and
active-condition identity are persisted structured fields. The test suite also
verifies structured alert responses.

## 2. Low-stock recovery

**Result: PASS.** `MvpFlowIntegrationTest.lowStockAlertResolvesWhenInventoryRecoversAboveThreshold` verifies that an active low-stock condition is resolved when a later real update restores stock above the threshold. The implementation keeps one active low-stock alert per tenant/Product/warehouse condition key and changes it to `RESOLVED`; it does not leave a stale active condition.

## 3. Depletion-risk calculation

**Result: PASS.** `MvpFlowIntegrationTest.depletionRiskIsDetectedBeforeThresholdBreachWhenDemandSpikes` verifies depletion risk while stock remains above the reorder threshold. `StockPredictionService` uses recent real order-item quantity for the same Product/Warehouse and the tenant operational policy to derive units-per-hour, stockout hours, depletion risk, urgent risk, and rapid consumption.

This is operational demand evidence, not Scenario projection output. The test checks the resulting alert, recommendation, and Inventory risk fields, including the projected stockout window.

## 4. Depletion-risk recovery

**Result: PASS.** `MvpFlowIntegrationTest.depletionRiskAlertResolvesWhenInventoryBufferRecovers` verifies that a later real Inventory recovery removes the active depletion-risk condition and leaves the prior alert visible as `RESOLVED` in recent history.

## 5. Inventory recommendation

**Result: PASS.** `MvpFlowIntegrationTest.recommendationsEndpointReturnsStructuredActionGuidance` and `criticalInventoryPressureProducesUrgentRecommendation` verify that actual Inventory pressure produces a structured recommendation with a truthful SKU, warehouse, type, priority, description, and policy explanation. `RecommendationService.createForInventory` is called by `InventoryMonitoringService` after a real mutation.

## 6. Same-tenant transfer recommendation

**Result: PASS.** `MvpFlowIntegrationTest.transferRecommendationIsGeneratedWhenAnotherWarehouseCanCoverShortfall` verifies a low North/available Coast fixture produces `TRANSFER_STOCK` with the correct source warehouse, receiving warehouse, SKU, and transfer quantity. `findTransferPlan` filters candidates by the current tenant, Product, and receiving warehouse, then requires sufficient transferable surplus.

No cross-tenant transfer path is introduced by this logic.

## 7. Recommendation deduplication

**Result: PASS with documented lifecycle scope.** `RecommendationService` reuses the latest recommendation when tenant, type, title, description, and creation time match within 1,800 seconds. This prevents repeated identical recommendations during the active 30-minute window. Recommendations do not currently have a separate resolved lifecycle; alert resolution is distinct from recommendation persistence.

The existing focused suite verifies structured and urgent recommendations, while the exact 30-minute clock boundary is source-supported rather than independently time-controlled in this phase.

## 8. Scenario separation

**Result: PASS.** Existing `MvpFlowIntegrationTest` Scenario Phase 2 tests capture Inventory, Orders, Fulfillment, Alerts, Recommendations, dispatch work, Scenario history, and event counts before preview. They verify preview changes only Scenario/planning evidence and does not mutate live Inventory, live Alerts, live Recommendations, Orders, Fulfillment, or dispatch state.

Scenario projection may reuse calculation logic, but `previewForInventory` returns projection DTOs and does not call the persistence method `createForInventory`. A hypothetical warning remains Scenario-local and is not an operational alert.

## 9. Dashboard operational truth

**Result: PASS for existing coverage.** `MvpFlowIntegrationTest.dashboardSummaryReturnsFullOperationalMetricSet` and `dashboardSnapshotReturnsExpandedControlCenterData` verify that Dashboard reads structured counts and snapshot surfaces for Inventory, alerts, recommendations, fulfillment, orders, events, audit, scenarios, and runtime incidents. `DashboardService` calculates tenant-scoped Inventory counts and low-stock counts from repositories and uses Redis only as a tenant-keyed summary cache.

The focused suite does not claim that every possible mutation changes every Dashboard field synchronously; it proves the supported snapshot contract and real mutation-to-summary paths.

## 10. Warehouse-scoped Dashboard and Inventory reads

**Result: PASS for authority boundaries.** `PlatformTenantAccessBoundaryIntegrationTest.directInventoryMutationsHonorWarehouseAuthorityAndPreserveWarehouseState` verifies separate North and Coast Inventory lanes, tenant-wide read visibility, warehouse-specific mutations, reservation preservation, receive, adjustment, and reconciliation. `warehouseScopeFiltersReadsAndDeniesWritesOutsideAssignedLane` provides the scoped-read/write boundary evidence.

The tenant context and warehouse scope guard prevent a scoped operator from using another warehouse’s Inventory lane. Tenant-wide sessions can see both warehouses inside their own tenant.

## 11. Inventory mutation to realtime dispatch

**Result: PASS for the supported after-commit path.** `InventoryService` publishes an operational update after a successful Inventory mutation. `OperationalStateChangePublisher` first persists a durable `PENDING` dispatch work item and publishes an application event. `OperationalStateChangeListener` handles the event at `AFTER_COMMIT` on the operational executor, and `OperationalDispatchQueueService` claims work, refreshes Dashboard summary, broadcasts tenant-scoped operational topics, and records completion metrics.

`MvpFlowIntegrationTest.committedRequestsDrainDispatchQueueAndExposePrometheusMetrics`, `OperationalStateChangeListenerTest`, and `OperationalDispatchQueueServiceTest` verify enqueue/drain, after-commit triggering, tenant batching, and metrics.

## 12. Tenant realtime isolation

**Result: PASS.** `RealtimeService` publishes only to `/topic/tenant/{TENANT}/...`. `WebSocketAccessBoundaryTest.rejectsCrossTenantSubscription` rejects a subscription whose destination names another tenant. The handshake attaches the authenticated tenant to the WebSocket session before subscriptions are accepted.

## 13. Warehouse realtime boundary

**Result: PASS for the supported scope model.** `WebSocketAccessBoundaryTest.rejectsTenantWideRawTopicsForWarehouseScopedSessions` rejects raw Inventory, Orders, and integration topics for warehouse-scoped sessions. The frontend `useWorkspaceRealtime` subscribes raw tenant-wide Inventory/Orders/Fulfillment topics only when the session has no warehouse scopes; scoped users refresh filtered snapshots through tenant APIs instead.

The safe operational topics and tenant-scoped routing remain available where the role permits them. This prevents a broad raw stream from becoming a warehouse-scope bypass.

## 14. Degraded mode and recovery

**Result: PASS by existing implementation and frontend contract; focused browser fault injection remains an evidence gap.** `useWorkspaceRealtime` exposes `connecting`, `live`, `reconnecting`, and `degraded` states. On transport failure it starts a 15-second snapshot refresh loop, keeps the degraded state visible, and stops the loop after a live connection returns. It never converts a failed transport into fake live status.

The focused backend realtime tests pass. A browser-level forced-disconnect recovery test is not part of this Phase 3 run, so that specific rendered recovery path remains a pilot/owner verification item.

## 15. Dispatch failure boundary

**Result: PASS for durable failure handling; fault-injection depth is limited.** The dispatch queue marks claimed work `PROCESSING`, increments attempts, broadcasts only after the claim, marks successful work `COMPLETED`, and marks runtime dispatch failures `FAILED` with bounded error text and failure metrics. Inventory persistence is completed in its own transaction before the after-commit dispatch listener runs, so a delivery failure does not roll back the durable stock mutation.

The current focused tests verify normal queue processing and metrics, not an injected realtime/Dashboard exception that forces a `FAILED` work item. That is a Medium evidence gap, not a demonstrated product defect.

## 16. Activity and audit truth

**Result: PASS for representative operational evidence.** `MvpFlowIntegrationTest.successfulRequestsReturnTraceIdAndWriteAuditLogs` and `rejectedRequestsReturnTraceIdAndWriteFailureAuditLogs` verify request IDs and audit outcomes. Existing Inventory boundary coverage verifies `INVENTORY_UPDATED` target references with SKU and warehouse, while the Inventory service paths use the existing audit/event services for update, receive, adjust, reconcile, reserve, release, fulfill, and return operations.

The evidence proves the supported audit contract and representative Inventory actions. It does not claim a new independent assertion for every mutation verb in this Phase 3 document.

## 17. Operational-truth boundary

**Result: PASS.** Product catalog changes do not call Inventory monitoring. Scenario preview does not persist live alert/recommendation entities. Real Inventory update, receive, adjust, reconcile, and order-driven reservation/fulfillment paths do invoke the operational monitoring seam. During the pilot, SynapseCore’s PostgreSQL Inventory view is an operational observation; an external WMS/source system remains authoritative for source reconciliation.

## 18. Frontend Inventory surface

**Result: PASS by source inspection.** `frontend/src/pages/Inventory.jsx` presents product, warehouse, available quantity, threshold, velocity, stockout window, risk level, warehouse coverage, and a signal matrix. A warehouse-scoped `TENANT_ADMIN` receives the controlled adjustment form with a required non-zero whole-unit delta and reason, then performs readback through `fetchSnapshot`.

The page intentionally exposes controlled adjustment only. Receive and reconcile remain API/integration/support operations, and Inventory CSV is not supported. This remains consistent with the supported frontend scope.

## 19. Direct negative smoke coverage

**Result: PASS for existing automated negative coverage.** The boundary suites cover wrong-warehouse Inventory operations, tenant isolation, non-admin authority, invalid/negative mutation behavior where applicable, unauthorized broad realtime topics, disabled integration/replay paths, and Scenario no-side-effect behavior. `WebSocketAccessBoundaryTest` directly rejects cross-tenant, governance-role integration, and warehouse-scoped raw subscriptions.

The Phase 3 run did not add a new negative suite because the existing boundary tests already exercise these contracts.

## 20. Production defects

**Result: none found in this phase.** Source inspection and the focused `121/121` suite did not expose a Phase 3 production defect. No runtime behavior was modified.

## 21. Test and fixture defects

**Result: none blocking.** The focused tests ran successfully with the existing fixtures. The test output includes intentional warnings for disabled connectors and rejected authentication/authorization cases; these are expected negative-path evidence, not failures.

## 22. Fixes applied

**Result: none.** No production code, frontend code, test code, configuration, or proof selectors were changed. The only intended Phase 3 file is this evidence document.

## 23. Focused tests

The focused result was:

```text
Tests run: 121, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Covered classes:

- `MvpFlowIntegrationTest` — 80 tests.
- `PlatformTenantAccessBoundaryIntegrationTest` — 31 tests.
- `RealtimeServiceTest` — 3 tests.
- `OperationalStateChangeListenerTest` — 1 test.
- `OperationalDispatchQueueServiceTest` — 1 test.
- `WebSocketAccessBoundaryTest` — 5 tests.

## 24. Full backend result

**Not rerun.** No production code changed in Phase 3, and the existing Phase 2 baseline is `194` tests, `0` failures, `0` errors. The focused Phase 3 suite is the appropriate verification for this evidence-only closure.

## 25. Frontend checks

**Not rerun.** No frontend files changed in Phase 3. Existing frontend behavior and the intentional Inventory limitation remain unchanged.

## 26. Files changed

Intended:

- `docs/evidence/inventory-lifecycle-phase-3-operational-intelligence.md`

Preserved and not staged:

- `frontend/Dockerfile`
- `.gitattributes`
- `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`
- `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`

## 27. Commit

The intended commit is the evidence document only. Commit SHA is recorded after commit execution.

## 28. Deployment and live status

No deployment was required. Hosted proof and owner-managed live checks were not run by this Phase 3 closure. The repository evidence is ready for a separate owner-managed live/Render verification when requested.

## 29. Critical blockers

**0 identified by the focused repository verification.**

## 30. High blockers

**0 identified by the focused repository verification.**

## 31. Medium/Low limitations

- The focused integration profile uses H2; PostgreSQL lock, isolation, and query-plan behavior remain deployment evidence responsibilities.
- Exact recommendation deduplication boundary timing is source-supported, not independently controlled with a clock fixture.
- Receive, adjustment, and reconciliation retry identity is now verified by `MvpFlowIntegrationTest.inventoryMutationRetriesReuseCommittedRequestIdentity`; automatic client retries remain out of scope.
- Dispatch fault injection is not covered by the focused suite; durable `FAILED` handling is source-supported and normal queue processing is tested.
- Browser-level forced realtime disconnect/recovery is not rerun here.
- The Inventory page exposes controlled adjustment only. Receive, reconcile, and CSV import remain API/integration surfaces, not new UI functionality.
- The OWNER-ACCEPT-02 manual walkthrough is deferred to the owner; no credentials or manual live claims are made here.

## 32. Owner walkthrough

**Deferred.** The owner should separately verify on a live seeded or pilot workspace: real PostgreSQL-backed low-stock creation/recovery, transfer guidance, Dashboard changes, realtime reconnect/degraded recovery, dispatch failure observability, and rendered `/inventory` behavior. This deferral does not block technical repository closure, but it prevents claiming full live operational acceptance.

## 33. Inventory readiness

The Inventory operational-intelligence seams are technically verified by the existing repository tests and source boundaries. Inventory is ready to move to owner-managed operational confirmation without starting another domain in this phase.

## 34. Verdict

**INVENTORY TECHNICALLY VERIFIED — OWNER OPERATIONAL WALKTHROUGH DEFERRED — READY TO SELECT NEXT DOMAIN**

Orders and the next feature lifecycle domain were not started.
