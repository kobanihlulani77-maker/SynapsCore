# Scenario Lifecycle Phase 0.5: Requester Identity

**Status:** Local implementation and verification complete; post-deployment live verification pending
**Starting HEAD:** `2bd8decbb51157a3b51c0d3bccc39af916153681`
**Scope:** Bind Scenario requester identity to the authenticated tenant session
**Evidence date:** 2026-08-27

## Purpose

Phase 0.5 closes the requester-identity seam identified in the Phase 0 authority census. A client must not be able to create a Scenario that appears to have been requested by another operator, even when that operator is active, belongs to the same tenant, has tenant-wide scope, or has access to the selected warehouse.

This phase does not implement the full Scenario lifecycle. It does not change executor policy, add a new role, test duplicate execution, or perform a full hosted lifecycle rehearsal.

## Previous Risk

Before this change, `ScenarioController` checked workspace and warehouse access and then passed the request to `ScenarioHistoryService.savePlan`. The service validated a submitted `requestedBy` operator for activity, tenant membership, and warehouse access, but did not require that name to match the authenticated session actor. The browser's session-derived requester display was therefore not a sufficient backend invariant.

## Implemented Invariant

The save path now:

1. obtains the authenticated `SynapseActorContext` from the access-control service;
2. passes the session actor's canonical `actorName` into `ScenarioHistoryService`;
3. uses that session actor as the persisted `requestedBy` value;
4. rejects a non-empty request `requestedBy` that differs from the session actor with HTTP 403;
5. still requires the canonical requester to be active and authorized for the selected warehouse; and
6. retains the request field for compatibility, treating it as an optional consistency assertion rather than an authority source.

The canonical identity is the authenticated operator `actorName`, not a username, display name, or client-selected label. Existing test-only `dev-anonymous` fallback behavior remains available for legacy non-production test paths; production access still requires an authenticated session.

## Files Changed

- `backend/src/main/java/com/synapsecore/api/controller/ScenarioController.java`
- `backend/src/main/java/com/synapsecore/scenario/ScenarioHistoryService.java`
- `backend/src/test/java/com/synapsecore/PlatformTenantAccessBoundaryIntegrationTest.java`

No frontend files, API route names, DTO fields, approval policy, or executor policy were changed.

## Verification Evidence

### Focused authority suite

Command:

```powershell
cd backend
cmd /c mvnw.cmd -Dtest=PlatformTenantAccessBoundaryIntegrationTest test
```

Result: **16 tests passed, 0 failures, 0 errors, 0 skipped.**

The new test proves that an authenticated `boundary.integration.operator` is persisted as the requester when the body agrees, and rejects requester substitutions for:

- another same-tenant operator;
- the tenant-wide `Operations Lead`;
- an operator scoped to the wrong warehouse;
- an operator from another tenant; and
- an inactive operator.

The existing requester/reviewer self-review save and approval regression also remains covered.

### Full backend suite

Command:

```powershell
cd backend
cmd /c mvnw.cmd test
```

Result: **154 tests passed, 0 failures, 0 errors, 0 skipped.**

The suite completed with `BUILD SUCCESS`. It exercised the new boundary test alongside the existing authentication, tenant isolation, scenario governance, replay, realtime, migration, and production-hardening tests.

### Frontend and repository checks

Results:

- `npm.cmd run lint`: passed;
- `npm.cmd run build`: passed;
- `npm.cmd run verify`: passed;
- `git diff --check`: passed; only normal LF-to-CRLF working-copy warnings were emitted;
- `scripts/secret-scan.ps1`: passed with zero critical findings. Five existing fixture findings were reported for committed starter/test credentials and are classified by that scanner as fixtures, not outward-facing secrets.

## Authority and Audit Boundaries

`ScenarioRun.requestedBy` and the save response now reflect the authenticated session actor. The existing tenant-scoped business event remains emitted for the save operation, but its current summary does not carry a separate requester field. This evidence does not claim an audit-log requester field that the current save path does not write.

The requester binding does not alter Preview or Comparison authority. Those operations do not consume `requestedBy` in the current analysis path. Review and rejection continue to use the existing assigned-actor and session-actor checks, including requester/reviewer separation.

## Deployment Status

No Render deployment or post-change live Scenario save was performed in this phase. The previously reported hosted proof result predates this backend change and is not reused as post-hardening evidence. After the commit is deployed, a minimal synthetic live save check should confirm:

```text
authenticated operator saves with matching requester -> allowed and persisted as that actor
same session submits a different requester -> HTTP 403
```

The full hosted proof should not be rerun solely for this Phase 0.5 closure unless the release process requires it; if run, its existing governance assertions must remain unchanged.

## Phase Boundary

Phase 0.5 is locally implemented and verified. Phase 1 remains separate and must cover the complete lifecycle: deterministic requester/reviewer assignment, approvals, escalation, execution, replay, and duplicate-safety evidence. No Phase 1 action was performed here.
