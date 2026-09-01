# Scenario Lifecycle Phase 8: Rejection and Revision Evidence

Status: Phase 4C implementation and focused local verification complete; deployment and hosted verification remain required for final closure.

This evidence records the rejection and revision boundary without claiming that a local H2 test is the same as deployed proof. It covers saved-plan rejection, immutable rejected history, linear revision lineage, authority enforcement, and the requirement that hypothetical scenario work does not create operational truth.

## Scope

Phase 8 covers:

- rejection of a saved scenario plan by the correct governance identity;
- rejection-stage and approval-policy validation;
- preservation of rejected history and review evidence;
- resubmission as a new revision rather than mutation of the rejected plan;
- denial of approval or execution after rejection;
- rejection authority across tenant, role, warehouse, requester, and session boundaries;
- no order, inventory, fulfillment, dispatch, alert, or recommendation side effects from rejection, revision, or PREVIEW checks.

## Linear Revision Contract

Scenario revision semantics are **LINEAR**:

```text
Rejected ScenarioRun N
  -> at most one immediate child
  -> child is a new SAVED_PLAN with revisionNumber N+1
  -> child is reviewed under fresh governance assignment
```

The same rejected parent cannot be resubmitted twice. The application locks the
tenant-scoped source row only after projection work has completed, checks for an
existing child while holding that lock, and keeps the lock through child
persistence and the `SCENARIO_RESUBMITTED` event. A duplicate successor returns
`409 CONFLICT`, identifies the existing child when available, creates no second
ScenarioRun, and creates no second resubmission event.

`revisionNumber` is not globally unique: independent root families may each
have a revision 2. The database constraint is only on the nullable
`revision_of_scenario_run_id`; multiple root rows remain valid because SQL
unique constraints allow multiple `NULL` values.

Revisioning must remain in the source warehouse. A cross-warehouse request is
rejected; an operator may instead create a new independent root Scenario in the
other warehouse.

The phase does not execute a positive business order flow, perform CSV recovery, or claim hosted proof for this runtime change.

## Implementation Under Review

The production seam is [ScenarioHistoryService.java](/C:/Users/asus/Downloads/synapsecore_starter/synapsecore/backend/src/main/java/com/synapsecore/scenario/ScenarioHistoryService.java), exposed through the rejection endpoint in `ScenarioController`:

```text
POST /api/scenarios/{scenarioRunId}/reject
```

Revision creation remains the existing save flow:

```text
POST /api/scenarios/save
revisionOfScenarioRunId = rejected scenario id
```

The direct test coverage is in [PlatformTenantAccessBoundaryIntegrationTest.java](/C:/Users/asus/Downloads/synapsecore_starter/synapsecore/backend/src/test/java/com/synapsecore/PlatformTenantAccessBoundaryIntegrationTest.java), method `scenarioPhaseEightRejectionRevisionPreservesGovernanceHistoryAndOperationalTruth`.

## 1. Rejection Authority

The backend is authoritative. The client-supplied actor role and reviewer name are checked against the authenticated session, the tenant, the required stage role, the assigned operator, and the scenario warehouse.

For a standard plan:

```text
PENDING_APPROVAL + PENDING_REVIEW
  -> assigned REVIEW_OWNER only
  -> REJECTED
```

For an escalated plan:

```text
PENDING_APPROVAL + PENDING_REVIEW
  -> assigned REVIEW_OWNER only

PENDING_APPROVAL + PENDING_FINAL_APPROVAL
  -> assigned FINAL_APPROVER only
```

The requester cannot review or reject their own plan. Tenant Admin remains a plan requester/configuration authority, not a substitute reviewer. Integration Admin, Integration Operator, Escalation Owner, and unrelated final approvers cannot reject a review-stage plan.

## 2. Negative Authority Matrix

The Phase 8 integration test proves the following denial paths:

| Attempt | Expected result | Evidence |
| --- | --- | --- |
| Different same-warehouse Review Owner | Denied | Assignment mismatch |
| Correct role in wrong warehouse | Denied | Warehouse scope mismatch |
| Operator from another tenant | Not found | Tenant-scoped lookup |
| Tenant Admin declaring Review Owner | Forbidden | Role/session authority mismatch |
| Integration Admin declaring Review Owner | Forbidden | Role/session authority mismatch |
| Integration Operator declaring Review Owner | Forbidden | Role/session authority mismatch |
| Escalation Owner declaring Review Owner | Forbidden | Role/session authority mismatch |
| Final Approver declaring Review Owner | Forbidden | Role/session authority mismatch |
| Assigned reviewer spoofing another reviewer name | Forbidden | Authenticated identity mismatch |
| Blank or whitespace-only reason | Bad request | DTO validation |
| Rejection from an invalid workflow stage | Bad request | Policy/stage guard |
| Rejection of an already rejected plan | Bad request | Terminal-state guard |
| Rejection of an approved plan | Bad request | Approved-state guard |
| Rejection of PREVIEW | Bad request | Only saved plans are rejectable |
| Anonymous rejection | Denied by service authority | Existing access boundary coverage |

## 3. Rejection State Transition

On a valid rejection, the service:

1. Loads the scenario through the tenant-scoped lookup.
2. Confirms the plan is a saved plan in `PENDING_APPROVAL`.
3. Confirms the policy and stage select the correct governance role.
4. Confirms the authenticated operator, assignment, warehouse, and requester/reviewer distinction.
5. Sets `approvalStatus=REJECTED` and `approvalStage=REJECTED`.
6. Stores the rejecting identity, timestamp, and trimmed reason.
7. Clears pending approval fields and approval due time.
8. Preserves earlier Review Owner evidence when a Final Approver rejects after review.
9. Records one `SCENARIO_REJECTED` business event.

Review Owner rejection at `PENDING_REVIEW` has no prior review decision to preserve, so review-evidence fields remain clear. Final Approver rejection after a successful Review Owner decision preserves `reviewApprovedBy`, `reviewApprovedAt`, and `reviewApprovalNote` as governance history.

## 4. Terminal-State Protection

Rejected plans are terminal records for the original revision:

```text
REJECTED
  -> cannot be approved
  -> cannot be executed
  -> cannot be rejected again
  -> remains visible in history
  -> may be loaded as the source for a new revision
```

The duplicate rejection path now fails before it can perform authority-dependent work or record a second business event. An approved plan cannot be contradicted by a later rejection request.

## 5. Revision Model

Revision is append-only history, not in-place editing or branching:

```text
Rejected revision N
  -> save with revisionOfScenarioRunId=N
  -> only if N has no existing immediate child
  -> new scenario id
  -> revisionNumber=N+1
  -> new request payload and projection
  -> new assigned Review Owner
  -> PENDING_APPROVAL
```

The rejected source retains its original:

- scenario id;
- creation time;
- request payload;
- rejection reason;
- rejecting identity;
- rejected state;
- audit/business-event history.

The Phase 4C regression test proves a revised quantity is stored on the new
revision while the original quantity remains unchanged. It also proves
sequential duplicate rejection, concurrent same-parent submission safety,
direct database uniqueness protection, revision 1 -> 2 -> 3 chaining,
independent revision families, same-warehouse enforcement, and independent
cross-warehouse root creation. Revision lineage is represented by
`revisionOfScenarioRunId` and `revisionNumber`.

## 6. Final-Stage Rejection Evidence

The escalated-path test proves:

```text
Review Owner approves
  -> PENDING_FINAL_APPROVAL
  -> review identity and note are persisted

Assigned Final Approver rejects
  -> REJECTED
  -> Final Approver identity and reason are persisted
  -> earlier Review Owner evidence remains persisted
```

This separates final rejection authority from review history. Rejection does not erase the fact that a prior governance stage was completed.

## 7. Operational Truth Invariant

The Phase 8 test captures counts before the scenario operations and confirms they are unchanged afterward:

- customer orders;
- inventory rows;
- fulfillment tasks;
- operational dispatch work items;
- alert records;
- recommendation records.

Rejection and revision are planning/governance operations. They do not execute an order, mutate inventory, create live alerts, or publish live recommendations. A rejected plan can be revised without turning the hypothetical projection into operational truth.

## 8. Automated Results

Phase 4C focused lineage suite:

```text
cmd /c mvnw.cmd -Dtest=ScenarioRevisionLineageEvidenceIntegrationTest test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The suite runs Flyway through migration v14 in the H2 test profile and verifies
the unique parent constraint through a direct JDBC insert attempt. Existing
tenant and cross-tenant authority coverage remains in the Phase 8/11 focused
suites; it is not replaced by the lineage test.

Focused authority/rejection suite:

```text
cmd /c mvnw.cmd -Dtest=PlatformTenantAccessBoundaryIntegrationTest test
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Earlier full backend baseline (before Phase 4C):

```text
cmd /c mvnw.cmd test
Tests run: 169, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The current full backend regression after the Phase 4C implementation also
passed:

```text
cmd /c mvnw.cmd test
Tests run: 312, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Frontend verification:

```text
npm.cmd run lint   PASS
npm.cmd run build  PASS
npm.cmd run verify PASS
```

Repository checks:

```text
git diff --check                         PASS
scripts\secret-scan.ps1                 PASS; 0 critical, 5 known fixture findings
scripts\docs-link-check.ps1            PASS; 769 local links checked
```

## 9. Confirmed Defects Fixed

### Duplicate rejection authority bypass

The previous idempotent branch returned a prior rejection response without rechecking rejection authority. That allowed an in-scope operator to receive a successful-looking duplicate response. Duplicate rejection is now a terminal bad request and cannot create a second rejection event.

### Final-stage history loss

The previous rejection path always cleared Review Owner evidence. Final Approver rejection now preserves the completed Review Owner decision and note while clearing only the fields that represent the final approval attempt.

### Invalid stage fall-through

Rejection now explicitly validates standard and escalated workflow stages before resolving rejection authority. Invalid stage combinations fail instead of being treated as ordinary Review Owner rejection.

## 10. Scope and Limitations

- The Phase 4C focused evidence runs against the repository test profile and H2, not the deployed PostgreSQL instance.
- Migration `V14__scenario_revision_lineage_uniqueness` fails safely when existing duplicate parent links are found; it does not delete or merge rows. The hosted PostgreSQL census supplied for this phase returned zero duplicate parent groups.
- Post-deployment live readiness must confirm that the deployed revision is serving the change.
- This phase intentionally does not rerun the full hosted proof because its positive flows create operational fixtures and the Phase 8 acceptance boundary excludes positive execution, CSV recovery, and inventory mutation.
- The existing frontend proof remains the authority for browser behavior; no frontend runtime code was changed in Phase 8.
- The unrelated working-tree changes `frontend/Dockerfile`, `.gitattributes`, and the Phase 0 census document are intentionally excluded from this closure.

## 11. Required Post-Deployment Check

After the intended backend deployment completes, run:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

The deployment is operationally ready for later hosted verification only when the script reports:

```text
FRONTEND_UP=True
BACKEND_UP=True
DB_READY=True
AUTH_READY=True
WS_READY=True
PROOF_ALLOWED=True
```

This check confirms connection and readiness prerequisites. It does not replace the Phase 8 automated authority evidence above.

## 12. Phase 4C Closure Position

The linear revision implementation is locally verified by the focused suite and
the full backend regression suite. Final Phase 4C closure still requires exact
main CI, deployment readiness, and disposable hosted sequential/concurrent
proof. No hosted claim is made by this local evidence alone.
