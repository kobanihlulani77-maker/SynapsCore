# Scenario Lifecycle Phase 6: Escalated Risk and Final Approval

## Scope

Phase 6 proves the escalated Scenario governance path through Final Approver
decision. It does not execute an approved Scenario, create a real order,
mutate inventory, start CSV/recovery work, or begin Phase 7. Final approval
means that the required human governance decision is complete; it does not mean
that the customer's business operation has happened.

## Evidence Status

- Starting repository revision: `d32d1b9` (Phase 5 evidence closure)
- Focused boundary suite: `24/24` passing.
- The Phase 6 test uses the existing synthetic
  `ACCESS-BOUNDARY-REHEARSAL` tenant and its two deterministic warehouse codes.
  It does not assume which warehouse sorts first.
- North and Coast identities, assignments, and passwords are synthetic test
  fixtures and are not reproduced here.
- Manual `OWNER-ACCEPT-02` walkthrough remains deferred; no operational data
  was manufactured solely to satisfy this phase.

## Escalation Predicate and Path

The existing Scenario save flow derives `ScenarioReviewPriority` and compares
it with the tenant policy's `escalatedApprovalMinimumPriority`. A plan at or
above the configured threshold receives `ScenarioApprovalPolicy.ESCALATED`.
Phase 6 does not change that calculation or threshold.

The approval path is:

```text
SAVED_PLAN / PENDING_REVIEW
        |
        | assigned Review Owner approval
        v
SAVED_PLAN / ESCALATED / PENDING_FINAL_APPROVAL
        |
        | exact assigned Final Approver approval
        v
SAVED_PLAN / ESCALATED / APPROVED
```

The controller endpoint is:

```text
POST /api/scenarios/{scenarioRunId}/approve
```

The production call chain is:

```text
ScenarioController.approveScenarioPlan
    -> ScenarioHistoryService.approvePlan
        -> getScenarioRun
        -> authenticated actor and tenant resolution
        -> requireActorRole
        -> requireWarehouseAccess
        -> processEscalatedApproval
            -> requireAssignedReviewOwner, or
            -> requireAssignedFinalApprover
        -> ScenarioRunRepository.save
        -> BusinessEventService.record
```

For `PENDING_REVIEW`, the required role is `REVIEW_OWNER` and the assigned
reviewer is recorded in `reviewApprovedBy`. For
`PENDING_FINAL_APPROVAL`, the required role is `FINAL_APPROVER` and the exact
persisted `finalApprovalOwner` is authoritative.

## Final Approver Authority

Final approval succeeds only when all of these conditions hold:

- the Scenario is a tenant-scoped `SAVED_PLAN`;
- approval status is `PENDING_APPROVAL`;
- approval stage is `PENDING_FINAL_APPROVAL`;
- the request is authenticated;
- the authenticated active operator has `FINAL_APPROVER`;
- the submitted approver name matches the authenticated session actor;
- the actor is allowed to operate in the Scenario warehouse;
- `finalApprovalOwner` is nonblank and exactly matches the actor;
- the actor is different from the requester;
- the actor is different from `reviewApprovedBy`;
- the approval note is nonblank for escalated approval.

Role membership and warehouse scope are necessary but insufficient. The
persisted assignment is the final authority boundary.

Final Approver selection during save is policy-driven through the active
tenant's Final Approver role and warehouse eligibility. Warehouse-specific
operators are preferred; the selected name is persisted on the Scenario. A
missing or blank assignment is now rejected at final approval rather than
being treated as implicitly authorized.

## Review Owner Handoff

The North positive fixture began as:

```text
approvalPolicy = ESCALATED
approvalStatus = PENDING_APPROVAL
approvalStage  = PENDING_REVIEW
reviewOwner    = assigned North Review Owner
finalApprovalOwner = assigned North Final Approver
```

The assigned Review Owner approval produced:

```text
approvalPolicy  = ESCALATED
approvalStatus  = PENDING_APPROVAL
approvalStage   = PENDING_FINAL_APPROVAL
reviewApprovedBy = assigned Review Owner
approvedBy      = null
executionReady  = false
```

This proves Review Owner approval does not finish an escalated plan and does not
make the reviewer the Final Approver.

## Positive Results

| Warehouse path | Review Owner | Final Approver | Result |
| --- | --- | --- | --- |
| `warehouseA` / North fixture | assigned North reviewer | assigned North final approver | review advanced to `PENDING_FINAL_APPROVAL`; final approval reached `APPROVED` |
| `warehouseB` / Coast fixture | assigned Coast reviewer | assigned Coast final approver | review advanced to `PENDING_FINAL_APPROVAL`; final approval reached `APPROVED` |

Both final decisions persisted the authenticated Final Approver and final note.
No Final Approver action was accepted before the Review Owner handoff.

## Negative Authority Matrix

| Attempt | Result |
| --- | --- |
| Review Owner tries to final-approve | denied; current stage requires `FINAL_APPROVER` |
| Same-warehouse unassigned Final Approver | denied; persisted assignment mismatch |
| Wrong-warehouse Final Approver | denied with HTTP 403 |
| Cross-tenant actor | denied with HTTP 404 and no cross-tenant detail |
| Tenant Admin | denied; role does not grant Final Approver authority |
| Integration Admin | denied |
| Integration Operator | denied |
| Escalation Owner | denied |
| Inactive assigned Final Approver | denied |
| Anonymous request | denied |
| Submitted actor spoof | denied because session actor and submitted name differ |
| Blank persisted Final Approver | denied; assignment is mandatory |

The negative matrix is exercised directly against the approval API rather than
relying on hidden frontend controls.

## Workflow State Results

- Escalated `PENDING_REVIEW` rejects a Final Approver request before Review Owner
  handoff.
- Escalated `PENDING_FINAL_APPROVAL` accepts only the assigned Final Approver.
- `PREVIEW` is rejected because only saved plans require approval.
- A `STANDARD` already-approved plan rejects a Final Approver retry because the
  current required role is Review Owner and the terminal approval identity is
  not bypassed.
- A rejected plan rejects final approval and requires resubmission as a new
  plan.
- A missing final-approval assignment rejects final approval.
- A blank escalated approval note rejects final approval.
- An already final-approved plan supports a safe retry only from the same
  authorized Final Approver; it creates no second governance event.

No state-skipping path was introduced.

## Separation of Duties

The implementation enforces two independent separations:

1. The requester cannot be the Final Approver.
2. The identity recorded in `reviewApprovedBy` cannot also be the Final
   Approver for the same escalated Scenario.

The test exercises both boundaries with isolated persisted fixtures. A human
who has multiple roles still cannot perform both governance stages for the same
Scenario when the persisted review identity matches that human.

## Duplicate and Contradictory Decisions

- A second final approval from the exact assigned Final Approver returns the
  existing approved state and creates no additional `SCENARIO_APPROVED` event.
- A final approval attempt by another actor after final approval is not treated
  as an authorized idempotent retry.
- Rejection after final approval is rejected with HTTP 400.
- Approval after rejection is rejected with HTTP 400.
- The phase does not claim a general concurrent transaction redesign; broad
  race/load behavior remains a later hardening concern.

## Note Rules

Escalated approval requires a nonblank note. The final note is trimmed and
persisted as governance evidence. Missing or whitespace-only notes are rejected
before the final transition.

## History and Activity

The final state preserves:

- Scenario identity and title;
- requester;
- warehouse;
- persisted Review Owner;
- Review Owner decision and timestamp;
- persisted Final Approver;
- Final Approver decision and timestamp;
- final note;
- approval policy and stage.

The service records `SCENARIO_ESCALATION_ADVANCED` for the Review Owner handoff
and `SCENARIO_APPROVED` for final approval. These are Scenario governance
events. They are not order completion, inventory movement, fulfillment,
dispatch, live Alert, Recommendation, or Runtime incident events.

No separate WebSocket event was added for Phase 6. Existing activity/realtime
behavior is not expanded merely to manufacture proof.

## Operational Side-Effect Proof

The test captures the following before the North/Coast governance paths and
checks them after final approval:

| Operational surface | Expected result | Result |
| --- | --- | --- |
| Tenant order count | unchanged | pass |
| Inventory count and quantities | unchanged | pass |
| Fulfillment count | unchanged | pass |
| Dispatch count | unchanged | pass |
| Live Alert count | unchanged | pass |
| Live Recommendation count | unchanged | pass |
| `SCENARIO_EXECUTED` event | absent | pass |

Projected inventory, risk, warnings, and recommendations remain hypothetical
Scenario intelligence after final approval. They do not become operational
truth until authoritative source-system data arrives.

## Execution Boundary

The Phase 6 test does not invoke `POST /api/scenarios/{id}/execute` and does not
create a real order. The repository still contains an explicit legacy
execution path whose existing contract treats an approved saved plan as
executable. That is the already-known deferred Approved Decision / External
Handoff Boundary limitation. It is not expanded or reinterpreted as part of
Phase 6.

## Defects and Corrections

### Production defects found

1. Approved-state approval retries returned success before validating the
   caller's current role, warehouse, assignment, or terminal identity. The
   service now validates an idempotent retry against the appropriate assigned
   Review Owner or Final Approver.
2. A blank `finalApprovalOwner` was not rejected by the assignment helper. The
   helper now requires a nonblank exact persisted assignment.
3. An invalid nonpending approval stage could fall through the escalated
   handler. Standard and escalated approval stages now have explicit allowed
   state guards.

These are narrow authority/state corrections. Risk thresholds, API shapes,
frontend behavior, and execution behavior were not redesigned.

### Test/fixture defects

No Phase 6 test expectation was corrected to hide a product defect. The test
uses direct repository state only to construct deterministic escalated fixtures;
it does not manually edit production data or invoke execution.

## Verification Record

Focused command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd -Dtest=PlatformTenantAccessBoundaryIntegrationTest test
```

Result: `24` tests, `0` failures, `0` errors, `BUILD SUCCESS`.

Full backend command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd test
```

Result: `165` tests, `0` failures, `0` errors, `0` skipped, `BUILD SUCCESS`.

Frontend commands:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\frontend
npm.cmd run lint
npm.cmd run build
npm.cmd run verify
```

Result: all passed. The frontend check covered 72 source files and 3
proof-critical documentation/label sets; the production build completed
successfully.

Repository gates:

- `git diff --check`: passed.
- `scripts\secret-scan.ps1`: passed; 0 critical findings and 5 previously
  documented fixture findings.
- `scripts\docs-link-check.ps1`: passed; 768 local links checked and none
  missing.

The production code change requires a post-push live readiness check. Hosted
Playwright proof is not required for this backend-only governance correction
unless live deployment evidence identifies a browser regression. The Phase 6
boundary remains governance-only and does not invoke Scenario execution.

Post-push deployment verification:

- Deployed revision: `162667d5f5424fb72d5afa302b93de9278039626`.
- `scripts\check-live-connections.ps1`: passed after network-enabled retry.
- `FRONTEND_UP=True`.
- `BACKEND_UP=True`.
- `DB_READY=True`.
- `AUTH_READY=True`.
- `WS_READY=True`.
- `PROOF_ALLOWED=True`.
- Hosted Playwright proof was not rerun because this Phase 6 correction is
  backend governance enforcement only, with no browser contract change, and
  the phase explicitly forbids Scenario execution.

## Manual Owner Walkthrough

`OWNER-ACCEPT-02` remains a workspace/role acceptance fixture without meaningful
operational product data. Manual business-data walkthrough is therefore
deferred. Technical acceptance is based on deterministic repository-backed
integration evidence and does not claim a customer walkthrough occurred.

## Phase Boundary

Phase 6 ends after Final Approver governance completion and evidence capture. It
does not execute an approved Scenario, create a real order, mutate inventory,
start CSV/recovery work, or begin Phase 7.
