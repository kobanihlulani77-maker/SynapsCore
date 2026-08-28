# Scenario Lifecycle Phase 1: Creation Authority

**Status:** Phase 1 implementation and automated evidence complete; owner verification remains pending
**Evidence date:** 2026-08-28
**Starting deployed HEAD:** `ba2e2fcf2deba417af0d59af2f66d090732a441d`
**Scope:** Scenario origin and creation authority only

## 1. Phase Boundary

This phase determines who may originate Scenario planning records and under what
tenant and warehouse conditions. It covers `PREVIEW`, `COMPARISON`, and
`SAVED_PLAN` creation paths, requester identity binding, reviewer selection, and
negative authorization inputs.

It does not execute or approve a Scenario, reject a plan, escalate a plan, test
Final Approval, test duplicate protection, test CSV recovery, or begin Phase 2.
The test fixture uses synthetic tenant data and does not represent customer data.

## 2. Intended Creation Policy

The current implementation supports this policy:

> Any authenticated, active tenant operator with access to the selected warehouse
> may originate a Scenario preview, comparison, or governed saved plan.

Creation authority does not grant review, final approval, escalation, or execution
authority. Those are separate workflow and assignment checks.

An empty warehouse scope means tenant-wide access. A non-empty scope limits the
identity to the listed warehouses. `TENANT_ADMIN` is therefore tenant-wide in the
current model when its scope is empty; it is not automatically a reviewer merely
because it is a tenant administrator.

The bootstrap `Operations Lead` is evaluated separately as a multi-role identity.
It is not used to define the single-role policy and is excluded from the normal
warehouse-specific reviewer choice where the explicit reviewer rules require a
dedicated assigned identity.

## 3. Repository Trace Before Implementation

The policy was derived from the existing implementation rather than invented for
the test:

| Layer | Evidence | Result |
|---|---|---|
| Frontend page access | `frontend/src/config/pageRegistry.js` | `scenarios` has no dedicated role restriction beyond authenticated workspace access |
| Frontend requester | `frontend/src/pages/ScenarioPlanner.jsx` | Requested By is read-only and comes from the authenticated session actor |
| Frontend reviewer choices | `frontend/src/hooks/useWorkspacePageContexts.js` | Active `REVIEW_OWNER` candidates must match the selected warehouse or be tenant-wide, and the current actor is excluded |
| Preview API | `ScenarioController` and `AccessControlService` | Requires an authenticated actor with selected warehouse access |
| Comparison API | `ScenarioController` and `AccessControlService` | Requires access to both primary and alternative warehouses |
| Save API | `ScenarioController` and `ScenarioHistoryService` | Requires selected warehouse access, binds requester to the authenticated actor, and validates the assigned reviewer |
| Persistence | `ScenarioAnalysisService` and `ScenarioHistoryService` | Planning evidence and saved plans are persisted without creating an operational order |
| Governance | `ScenarioHistoryService` | Reviewer role, assignment, warehouse, distinct requester, and later workflow state are checked separately |

## 4. Role Creation Matrix

The following matrix is the intended result for an active, authenticated identity
with access to the selected warehouse. `Allowed` means origin/creation only; it
does not imply approval or execution authority.

| Tenant role | Open Scenarios | `PREVIEW` | `COMPARISON` | `SAVED_PLAN` | Boundary |
|---|---:|---:|---:|---:|---|
| `TENANT_ADMIN` | Allowed | Allowed | Allowed | Allowed | Tenant-wide when scope is empty; otherwise scoped |
| `INTEGRATION_ADMIN` | Allowed | Allowed | Allowed | Allowed | Selected warehouse scope applies |
| `INTEGRATION_OPERATOR` | Allowed | Allowed | Allowed | Allowed | Selected warehouse scope applies |
| `REVIEW_OWNER` | Allowed | Allowed | Allowed | Allowed | Creation does not make the actor the reviewer automatically |
| `FINAL_APPROVER` | Allowed | Allowed | Allowed | Allowed | Final approval remains state-specific and assigned |
| `ESCALATION_OWNER` | Allowed | Allowed | Allowed | Allowed | Escalation acknowledgement remains separate |

The automated Phase 1 matrix exercised all six roles across North and Coast
warehouse-scoped identities, plus tenant-wide Tenant Admin coverage. Every
role completed preview, comparison, and saved-plan creation inside its allowed
warehouse context.

## 5. Operations Lead and Tenant-Wide Identities

`Operations Lead` is a bootstrap multi-role identity with tenant-wide scope. It
can originate planning records through the same workspace and warehouse access
check, but it must not be treated as proof that every single role has governance
authority. The dedicated role matrix above is the authoritative creation model.

The saved-plan path still requires an explicit valid reviewer. A tenant-wide or
bootstrap identity is not accepted as an explicit reviewer when the reviewer
policy requires a warehouse-eligible assigned Review Owner. The requester and
reviewer must also be distinct.

## 6. Automated Evidence

### 6.1 Positive matrix

`PlatformTenantAccessBoundaryIntegrationTest.scenarioCreationAuthorityAllowsAllRolesWithinWarehouseScope`
exercised twelve synthetic identity/warehouse combinations:

- Tenant Admin in North and Coast
- Integration Admin in North and Coast
- Integration Operator in North and Coast
- Review Owner in North and Coast
- Final Approver in North and Coast
- Escalation Owner in North and Coast

For each combination the test verified:

- `POST /api/scenarios/order-impact` succeeds in the assigned warehouse;
- `POST /api/scenarios/order-impact/compare` succeeds for allowed warehouse inputs;
- `POST /api/scenarios/save` creates a `SAVED_PLAN` with the expected requester,
  warehouse, and assigned Review Owner.

### 6.2 Negative matrix

`PlatformTenantAccessBoundaryIntegrationTest.scenarioCreationRejectsAnonymousInvalidAndWrongWarehouseRequests`
verified:

| Input | Expected result | Evidence |
|---|---|---|
| Anonymous preview | `403` | No session is accepted |
| Anonymous comparison | `403` | No session is accepted |
| Anonymous save | `403` | No session is accepted |
| North operator targeting Coast preview | `403` | Warehouse scope enforced |
| North operator comparing North to Coast | `403` | Both comparison warehouses are checked |
| North operator saving to Coast | `403` | Warehouse scope enforced before save |
| Missing warehouse | `400` | Request validation |
| Missing product | `4xx` | Domain/request validation |
| Zero quantity | `400` | Request validation |

### 6.3 Requester identity

`scenarioSaveBindsRequesterToAuthenticatedSessionActor` verifies that the saved
requester is taken from the authenticated session. Same-tenant, tenant-wide,
wrong-warehouse, cross-tenant, and inactive requester spoof attempts are rejected
with the backend requester/session binding rule. The browser's read-only field is
therefore backed by a server-side invariant, not only a UI convention.

### 6.4 Reviewer assignment regression

Existing access-boundary coverage verifies that an explicitly supplied reviewer
must be active, must have `REVIEW_OWNER`, must be eligible for the Scenario
warehouse, and must not be the requester. Bootstrap or tenant-wide reviewer
targets that do not satisfy the explicit reviewer policy are rejected.

## 7. Fixture Corrections

The first focused run found test-fixture defects, not product defects:

- Coast equivalents for Integration Admin, Final Approver, and Escalation Owner
  were missing from the synthetic boundary fixture.
- An existing test assumed the newest twelve Scenario records were always its own
  records; the new matrix exposed that test-order sensitivity.

The test-only correction added the missing scoped identities and made the existing
warehouse test create and locate its own fresh record. No production source,
backend contract, frontend behavior, or authorization rule was changed.

## 8. Verification Result

Focused backend result:

```text
PlatformTenantAccessBoundaryIntegrationTest
Tests run: 19
Failures: 0
Errors: 0
Skipped: 0
```

The focused test ran against the Spring Boot integration context with repository
and authorization boundaries active. `git diff --check` is clean for the current
changes.

Full repository verification completed after the focused run:

| Gate | Result |
|---|---|
| Backend Maven suite | `157/157` passed; `0` failures; `0` errors; `0` skipped |
| Frontend lint | Passed; 72 source files checked |
| Frontend build | Passed; Vite production bundle generated |
| Frontend verify | Passed; lint and build completed |
| Secret scan | Passed; `0` critical findings; known fixture findings only |
| Diff check | Passed; no whitespace errors |

The backend suite emitted expected warnings for deliberate authorization
denials, disabled connector fixtures, and test-harness concurrency cases. No
unexpected test failure or production-source regression was reported.

No runtime code changed, so no deployment or hosted-proof rerun is claimed for
this phase. Existing hosted proof remains prior evidence, not Phase 1 live proof.

## 9. Owner Verification Matrix

Owner verification should use fresh synthetic identities and should confirm the
same policy in the deployed environment. Do not record passwords, tokens, session
cookies, or raw payloads.

| Identity | Valid origin scope | Expected valid actions | Expected denied action |
|---|---|---|---|
| North `TENANT_ADMIN` | North and Coast if tenant-wide | Preview, comparison, save with eligible reviewer | None for creation; governance remains separate |
| North `INTEGRATION_ADMIN` | North | Preview, comparison, save in North | Any creation request for Coast |
| North `INTEGRATION_OPERATOR` | North | Preview, comparison, save in North | Any creation request for Coast |
| North `REVIEW_OWNER` | North | Preview, comparison, save in North | Any creation request for Coast |
| North `FINAL_APPROVER` | North | Preview, comparison, save in North | Any creation request for Coast |
| North `ESCALATION_OWNER` | North | Preview, comparison, save in North | Any creation request for Coast |
| Coast scoped roles | Coast | Preview, comparison, save in Coast | Any creation request for North |

Owner verification must also confirm that reviewer choices are explicit,
warehouse-eligible, active, and distinct from the requester. It must not treat a
hidden UI option as sufficient evidence; direct API denial is required for the
negative cases.

## 10. Blockers and Phase Boundary

**Critical blockers:** 0 identified in repository-backed Phase 1 evidence

**High blockers:** 0 identified in repository-backed Phase 1 evidence

**Medium follow-up:** owner verification is still pending for this phase, and the
bootstrap multi-role identity should remain separate from customer-facing role
policy in future fixtures and documentation.

**Verdict:** `PHASE 1 READY FOR OWNER VERIFICATION`

Phase 2 must not begin until owner verification confirms the deployed creation
matrix and this evidence record is updated with the owner result.
