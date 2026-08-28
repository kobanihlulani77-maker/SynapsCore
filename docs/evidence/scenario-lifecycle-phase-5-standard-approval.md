# Scenario Lifecycle Phase 5: Standard-Risk Review Owner Approval

## Scope

Phase 5 proves the standard-risk `SAVED_PLAN` review decision. It does not
perform final approval, escalation acknowledgement, scenario execution, real
order creation, inventory mutation, or CSV acceptance. An approved Scenario is
a governed decision; the customer's authoritative source system remains the
source of operational truth.

## Evidence Status

- Starting repository revision: `4b3da53` (Phase 4 accepted baseline)
- Phase 5 implementation/evidence revision: `c197c99` (`Prove standard Scenario approval governance`)
- Test fixture: `ACCESS-BOUNDARY-REHEARSAL`, with two persisted warehouses
  selected by the fixture's deterministic name ordering. The test refers to
  them as `warehouseA` and `warehouseB` so it does not make a North/Coast
  ordering assumption.
- Tenant identities and passwords are synthetic test fixtures only and are not
  reproduced in this document.
- Focused boundary suite: `23/23` passing after the test-isolation correction.
- Manual `OWNER-ACCEPT-02` walkthrough: deferred; that tenant is not used as a
  substitute for technical evidence and no operational data was manufactured
  for it.

## Production Path

The approval request enters:

1. `ScenarioController` at `POST /api/scenarios/{id}/approve`.
2. `ScenarioHistoryService.approvePlan(long, ScenarioApprovalRequest)`.
3. `getScenarioRun`, which resolves the tenant from the authenticated request
   and enforces warehouse access.
4. `AccessControlService.requireScenarioActor`, which binds the declared
   `REVIEW_OWNER` role and submitted name to the authenticated session actor.
5. `requireActorRole`, `requireWarehouseAccess`, requester/reviewer
   distinctness, and `requireAssignedReviewOwner`.
6. The Scenario repository and `BusinessEventService` for the persisted
   governance state and `SCENARIO_APPROVED` activity.

The approval request requires `actorRole` and a nonblank `approverName`.
`approvalNote` is optional for standard-risk approval. Blank or missing notes
are stored as `null`; nonblank notes are trimmed and preserved.

## Standard-Risk Predicate

The save path derives `ScenarioReviewPriority` and compares it with the
tenant's configured `escalatedApprovalMinimumPriority`. A plan below that
threshold is `STANDARD`; it does not require the Final Approver stage. Phase 5
does not change the threshold or the risk calculation. The deterministic
quantity-one fixture produces the standard path through the existing policy.

The production branch is explicit: `ScenarioApprovalPolicy.STANDARD` moves a
pending saved plan directly to `APPROVED`. `ESCALATED` calls the separate
escalated path and cannot use this direct transition.

## Authority Predicate

For a standard approval to succeed, all of the following must hold:

- the plan is a tenant-scoped `SAVED_PLAN`;
- the plan is still pending review/pending approval;
- the request is authenticated;
- the authenticated session actor is active and has `REVIEW_OWNER`;
- the submitted approver name equals that authenticated actor;
- the actor has access to the plan's warehouse;
- the actor is not the requester;
- the plan has a nonblank persisted `reviewOwner`;
- the persisted `reviewOwner` equals the authenticated actor;
- the declared workflow role is the role required by the current stage.

The request body cannot choose a different approving identity. Assignment and
warehouse scope are independently enforced.

## State Transition

Before approval:

```text
type            = SAVED_PLAN
approvalPolicy  = STANDARD
approvalStatus  = PENDING_APPROVAL
approvalStage   = PENDING_REVIEW
reviewOwner     = assigned active Review Owner
approvedBy      = null
executionReady  = false
```

After valid standard approval:

```text
approvalPolicy  = STANDARD
approvalStatus  = APPROVED
approvalStage   = APPROVED
approvedBy      = authenticated assigned Review Owner
approvalNote    = trimmed note or null
executionReady  = true according to the existing executable-state contract
```

No Final Approver action is required for this standard path. `executionReady`
describes the existing Scenario state; the Phase 5 approval request itself does
not execute the proposed business action.

## Positive Evidence

| Fixture | Assigned reviewer | Result |
| --- | --- | --- |
| Standard `warehouseA` plan | `boundary.review` | HTTP 200; `APPROVED`; `APPROVED` stage; authenticated reviewer persisted; note retained |
| Standard `warehouseB` plan | `boundary.review.b` | HTTP 200; `APPROVED`; `APPROVED` stage; authenticated reviewer persisted; absent note remains `null` |

The persisted entity was read back after both decisions. The standard North
path has no `reviewApprovedBy` because it did not pass through the escalated
review stage.

## Negative Authority Matrix

The Phase 4 boundary test in the same focused suite supplies the pending-plan
negative matrix used by Phase 5:

| Attempt | Expected result | Evidence |
| --- | --- | --- |
| Same-warehouse alternate Review Owner | HTTP 400; persisted assignment mismatch | `scenarioPhaseFourReviewHandoffRequiresPersistedAssignmentAndPreservesOperationalTruth` |
| Wrong-warehouse Review Owner | HTTP 403 | same test, mirrored plan checks |
| Cross-tenant reviewer | HTTP 404 without cross-tenant detail | same test |
| Requester | HTTP 403 | requester/reviewer distinctness checks |
| Requester who also has `REVIEW_OWNER` | HTTP 403 | self-review regression coverage in the boundary suite |
| `TENANT_ADMIN` | HTTP 403 | role matrix in the boundary suite |
| `INTEGRATION_ADMIN` | HTTP 403 | role matrix in the boundary suite |
| `INTEGRATION_OPERATOR` | HTTP 403 | role matrix in the boundary suite |
| `FINAL_APPROVER` at review stage | HTTP 403 | required role remains `REVIEW_OWNER` |
| `ESCALATION_OWNER` | HTTP 403 | role matrix in the boundary suite |
| Inactive assigned reviewer | HTTP 403 | assigned operator is disabled before approval |
| Anonymous request | HTTP 403 | production fallback is disabled |
| Missing persisted reviewer | HTTP 400 | assignment is mandatory |
| `PREVIEW` | HTTP 400; only saved plans are reviewable | direct approval attempt |
| Rejected saved plan | HTTP 400; resubmission is required | existing rejection-state coverage |

## Escalated Boundary

The Phase 5 test converts a saved fixture into an `ESCALATED` high-priority
pending plan without invoking a real escalation workflow. The assigned Review
Owner approves the review stage and receives:

```text
approvalPolicy = ESCALATED
approvalStatus = PENDING_APPROVAL
approvalStage  = PENDING_FINAL_APPROVAL
reviewApprovedBy = assigned Review Owner
approvedBy = null
executionReady = false
```

This proves that Review Owner approval cannot bypass Final Approver. Phase 6
owns the final approval action and is intentionally not started here.

## Duplicate and Contradictory Decisions

- A second approval request from the same assigned reviewer is intentionally
  idempotent: it returns the already-approved state and creates no second
  `SCENARIO_APPROVED` business event.
- A rejection after approval is now rejected with HTTP 400 and the message
  that the Scenario has already been approved. This closes the contradictory
  terminal-state gap found during Phase 5 inspection.
- The test does not claim to provide a broad concurrency redesign. Parallel
  race behavior remains a later hardening concern if operational evidence
  requires it.

## History, Activity, and Realtime

Successful standard approval persists the Scenario decision, authenticated
reviewer, timestamp, and optional note. It records `SCENARIO_APPROVED` through
`BusinessEventService`, so the governance decision is available to existing
tenant activity/runtime consumers. The event is not an order-completion,
inventory, alert, recommendation, or runtime-incident event.

No separate approval WebSocket event was added for Phase 5. Existing realtime
behavior is not expanded merely to create evidence. The test verifies the
business-event evidence and the absence of `SCENARIO_EXECUTED`.

## Operational Side-Effect Proof

The Phase 5 test captures values before approval and reads them after both
standard decisions and the escalated review transition:

| Operational surface | Expected after approval | Result |
| --- | --- | --- |
| Tenant order count | unchanged | pass |
| Tenant inventory count and quantities | unchanged | pass |
| Fulfillment task count | unchanged | pass |
| Dispatch work-item count | unchanged | pass |
| Live Alert count | unchanged | pass |
| Live Recommendation count | unchanged | pass |
| `SCENARIO_EXECUTED` event | absent | pass |

Projected intelligence remains Scenario-local what-if output. Approval means
the governed proposal was accepted; it does not make projected stock, alerts,
recommendations, fulfillment, or orders real.

## Defects and Corrections

### Production defect found

`rejectPlan` previously allowed an already approved saved plan to transition to
`REJECTED`. That contradicted the terminal governance boundary. The smallest
safe correction was an early HTTP 400 guard in
`ScenarioHistoryService.rejectPlan`; no broader workflow redesign was made.

### Test defect found

The existing Phase 3 test inspected the global latest 20 business events. JUnit
method order is not a contract, so a valid Phase 5 approval could appear in
that list and make Phase 3 fail even though Phase 3 itself created no approval.
The test now filters the tenant's events to those created after its own phase
start. This changes no product expectation; it removes order-dependent test
contamination.

No production behavior was weakened and no proof assertion was removed.

## Verification Record

Focused command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd -Dtest=PlatformTenantAccessBoundaryIntegrationTest test
```

Result: `23` tests, `0` failures, `0` errors, `BUILD SUCCESS`.

Final verification results:

- Full backend suite: `164/164` passing; `BUILD SUCCESS`.
- Frontend lint: pass.
- Frontend build: pass.
- Frontend verify: pass.
- Secret scan: pass; `0` critical findings and only the repository's known
  fixture credential findings.
- Documentation link check: pass; `767` local links checked.
- `git diff --check`: pass.
- Post-push live connection check: pass; `FRONTEND_UP=True`,
  `BACKEND_UP=True`, `DB_READY=True`, `AUTH_READY=True`, `WS_READY=True`, and
  `PROOF_ALLOWED=True`.

Hosted Playwright proof was not rerun: this Phase changed one backend terminal
state guard and its focused backend evidence, not a browser contract, and the
required live readiness check is green. No hosted proof is run while readiness
is unhealthy.

## Manual Owner Walkthrough

`OWNER-ACCEPT-02` is intentionally not treated as complete owner acceptance.
The manual tenant has workspace and role evidence, but no meaningful
operational products/inventory were created solely for this phase. Technical
standard-approval evidence is therefore complete independently; an operational
owner walkthrough remains deferred until a legitimate operational fixture is
available.

## Phase Boundary

Phase 5 stops after standard Review Owner approval and the escalated
Review-Owner-to-Final-Approver boundary. It does not final-approve, execute,
escalate, create orders, mutate inventory, or start Phase 6.
