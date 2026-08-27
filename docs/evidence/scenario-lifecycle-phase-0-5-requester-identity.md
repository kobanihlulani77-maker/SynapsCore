# Scenario Lifecycle Phase 0.5: Session Identity and Review Assignment Alignment

**Status:** Local implementation and verification complete; post-deployment live verification pending
**Continuation baseline:** `05e30795367ff3d127119e8e5d182d146f290b82`
**Scope:** Bind Scenario identity and reviewer assignment to the authenticated session and selected warehouse
**Evidence date:** 2026-08-28

## Purpose

This continuation closes the remaining Scenario identity and assignment seam
after requester binding. A Scenario must record the authenticated operator as
the requester, and its Review Owner must be an active, explicitly assigned
`REVIEW_OWNER` for the selected warehouse. Tenant administration may still see
and manage all tenant operators, but that administrative visibility does not
make every operator eligible for Scenario review.

This phase does not implement the complete Scenario lifecycle. It does not
change executor policy, add a role, or claim post-deployment live evidence.

## Authority Rules

### Person acting

The authenticated session actor is the only person recorded as `requestedBy`.
The browser displays `Signed In As` and `Requested By` as read-only identity
information. There is no user-selectable Acting As control. The request field
remains in the API for compatibility, but a non-empty value must match the
authenticated actor; it is not an authority source.

Decision endpoints continue to use the authenticated actor checks already
provided by `AccessControlService.requireScenarioActor`. A submitted decision
actor is treated as a consistency assertion and cannot replace the session
identity.

### Review Owner

For a Scenario with warehouse `WH-NORTH`, only an active operator with the
`REVIEW_OWNER` role and an explicit `WH-NORTH` warehouse scope is eligible. The
same rule applies to `WH-COAST`. The requester cannot review their own plan.

The frontend and backend both exclude:

- inactive operators;
- operators with the wrong role;
- operators assigned to another warehouse;
- tenant-wide operators with an empty warehouse scope;
- generated bootstrap tenant admins; and
- the authenticated requester.

If no warehouse is selected, the frontend shows no reviewer candidates and the
backend does not auto-select a broad reviewer. An explicitly submitted owner
still has to satisfy the same warehouse-specific rule.

### Operations Lead classification

The generated `Operations Lead` is a bootstrap tenant administrator with a
multi-role convenience identity. Its administrative record remains visible in
the tenant directory and it may be used for tenant administration. It is not a
customer-facing Scenario Review Owner merely because it carries a reviewer
role. A generated bootstrap tenant admin or any tenant-wide Review Owner is
rejected as a Scenario reviewer target.

## Implemented Changes

- `ScenarioController` passes the authenticated actor into the save service.
- `ScenarioHistoryService` persists that actor as `requestedBy` and rejects a
  mismatched client requester.
- Review Owner auto-selection requires an explicit selected-warehouse scope.
- Explicit Review Owner selection is checked for active status, role,
  warehouse scope, requester separation, and bootstrap-identity exclusion.
- The frontend filters the same candidate set and starts with no misleading
  `Operations Lead` default.
- Loading a historical Scenario restores `Requested By` from the current
  authenticated session rather than trusting stale historical display data.
- Assignment validation text uses assignment language rather than an Acting As
  concept.

## Files Changed In This Continuation

- `backend/src/main/java/com/synapsecore/access/AccessDirectoryService.java`
- `backend/src/main/java/com/synapsecore/scenario/ScenarioHistoryService.java`
- `backend/src/test/java/com/synapsecore/PlatformTenantAccessBoundaryIntegrationTest.java`
- `frontend/src/config/workspaceModel.js`
- `frontend/src/hooks/useScenarioActions.js`
- `frontend/src/hooks/useWorkspaceBootstrap.js`
- `frontend/src/hooks/useWorkspacePageContexts.js`

The unrelated local changes to `frontend/Dockerfile`, `.gitattributes`, and
`docs/evidence/scenario-lifecycle-phase-0-authority-census.md` are not part of
this evidence change.

## Verification Evidence

### Focused backend authority suite

Command:

```powershell
cd backend
cmd /c mvnw.cmd -Dtest=PlatformTenantAccessBoundaryIntegrationTest test
```

Result: **17 tests passed, 0 failures, 0 errors, 0 skipped.**

The suite covers:

- authenticated requester binding;
- requester substitution rejection for same-tenant, tenant-wide, wrong-
  warehouse, cross-tenant, and inactive operators;
- bootstrap `Operations Lead` rejection as Review Owner;
- tenant-wide `REVIEW_OWNER` rejection as Review Owner;
- explicit North and Coast reviewer acceptance;
- wrong-warehouse reviewer rejection; and
- warehouse-specific automatic reviewer selection.

### Full backend suite

The full backend suite was rerun after the final continuation edits.

Command:

```powershell
cd backend
cmd /c mvnw.cmd test
```

Result: **155 tests passed, 0 failures, 0 errors, 0 skipped.**

The suite completed with `BUILD SUCCESS`, including authentication, tenant
isolation, Scenario governance, replay, realtime, migration, and production
hardening coverage.

### Frontend and repository checks

Results:

- `npm.cmd run lint`: passed;
- `npm.cmd run build`: passed;
- `npm.cmd run verify`: passed;
- `git diff --check`: passed; and
- `scripts/secret-scan.ps1`: expected existing fixture findings only and zero
  critical findings.

## Boundary Interpretation

The tenant directory is an administrative surface. It may list the tenant's
operators and their roles/scopes, including the bootstrap administrator.
Scenario assignment is narrower: the selected warehouse determines the
eligible Review Owner set, and only explicitly scoped reviewers in that set
can receive the review responsibility.

This is a local implementation and test result. It does not claim that the
new revision has been deployed or that the rule has been exercised against the
live Render tenant. Post-deployment live verification remains required before
closing this phase.

## Phase Boundary

Phase 0.5 is locally implemented and verified. Phase 1 remains separate and
must cover the complete Scenario lifecycle: deterministic requester/reviewer
assignment, approvals, escalation, execution, replay, and duplicate-safety
evidence. No Phase 1 action was performed here.
