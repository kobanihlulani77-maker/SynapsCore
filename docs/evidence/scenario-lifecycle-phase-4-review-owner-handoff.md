# Scenario Lifecycle Phase 4: Review Owner Handoff and Assignment Enforcement

**Status:** Technical evidence complete; owner operational walkthrough remains deferred
**Evidence date:** 2026-08-28
**Starting repository HEAD:** `a5883bb40a4ca4322cfb2170fcc6b3ded44262b3`
**Scope:** Prove that a saved plan reaches its persisted Review Owner and that review authority cannot be claimed by another identity

## 1. Phase Boundary

This phase proves handoff and review authority only. It does not approve,
reject, final-approve, escalate, execute, or create a real order from the
target plans. The isolated integration test creates synthetic test fixtures in
an in-memory database; it does not change a hosted or customer tenant.

## 2. Current Review Handoff Path

The current implementation path is:

```text
POST /api/scenarios/save
  -> ScenarioController.saveScenarioPlan
  -> AccessControlService.requireWorkspaceWarehouseAccess
  -> ScenarioHistoryService.savePlan
  -> ScenarioProjectionService.projectOrderImpact
  -> ScenarioRiskPolicyService.assess
  -> resolveRequestedBy from the authenticated session actor
  -> resolveReviewOwner for the selected warehouse
  -> ScenarioRunRepository saves SAVED_PLAN
  -> BusinessEventService records SCENARIO_SAVED
  -> ScenarioSaveResponse

GET /api/scenarios/history
  -> ScenarioController.getScenarioHistory
  -> tenant and warehouse-scoped ScenarioHistoryService query
  -> ScenarioRunResponse with requester, warehouse, assignment, state, and risk

GET /api/scenarios/{id}/request
  -> ScenarioController.getScenarioRequest
  -> authenticated tenant lookup by id
  -> warehouse access check for the current operator
  -> stored proposal reload

POST /api/scenarios/{id}/approve or /reject
  -> ScenarioHistoryService.getScenarioRun
  -> tenant and warehouse boundary
  -> authenticated session actor and declared role match
  -> required workflow role
  -> warehouse access
  -> requester/reviewer distinctness
  -> persisted assignment match
  -> state mutation only after every predicate passes
```

There is no separate review-queue persistence or push handoff service. The
existing review work surface is built from tenant-scoped Scenario history and
the request-detail endpoint. Assignment is carried by `ScenarioRun.reviewOwner`.

## 3. Persisted Assignment Authority

At save time, `reviewOwner` is resolved by the backend. For the standard Review
Owner policy, the target must be an active operator in the current tenant with
the configured `REVIEW_OWNER` role and an explicit scope for the selected
warehouse. Bootstrap tenant-wide operators and tenant-wide Review Owners are
not accepted as warehouse Review Owners. The requester and reviewer must be
different.

The persisted `ScenarioRun.reviewOwner` is the authority used later by
`requireAssignedReviewOwner`. Role, warehouse, a frontend dropdown, or a
submitted actor name cannot replace that persisted assignment.

The guard is strict in both directions:

```text
reviewOwner is blank/null
  -> deny review action

reviewOwner does not equal authenticated actor
  -> deny review action

reviewOwner equals authenticated actor
  -> continue only after session, role, warehouse, requester, and state checks
```

This also protects malformed or legacy records with no assignment: an
otherwise valid same-warehouse Review Owner cannot claim them.

## 4. Visibility Versus Authority

The current visibility contract is deliberately broader than action authority.
An active same-warehouse Review Owner may see a Scenario history row and open
its stored request, even when that reviewer is not the persisted assignee. The
row still identifies the real `reviewOwner`, and the decision endpoint rejects
the unassigned reviewer.

Warehouse-scoped operators cannot open a plan in another warehouse. A session
from another tenant receives a scoped not-found result for a direct Scenario
id. Tenant-wide visibility does not grant Review Owner authority.

The frontend mirrors this distinction: the Scenario decision console renders
assignment and disables action controls when the current actor is not the
assigned owner. The backend remains authoritative for direct API attempts.

## 5. Automated Fixture

`PlatformTenantAccessBoundaryIntegrationTest` uses two isolated tenants and two
persisted warehouses. It creates:

- a North-scoped Review Owner and same-warehouse alternate reviewer;
- a Coast-scoped Review Owner;
- tenant admin, Integration Admin, Integration Operator, Final Approver, and
  Escalation Owner identities;
- an inactive Review Owner;
- an isolation-tenant administrator;
- a catalog product, inventory rows, and an existing order only for the
  boundary test database.

The test resolves the two warehouse codes from persisted fixture data rather
than assuming a display-name or sort order.

## 6. Handoff Evidence

For the first saved plan, the test verifies that:

- the database record is `SAVED_PLAN`;
- requester is the authenticated tenant-admin actor;
- warehouse is preserved;
- `reviewOwner` is the explicit North-scoped reviewer;
- status is `PENDING_APPROVAL`;
- stage is `PENDING_REVIEW`;
- executable is false;
- the assigned reviewer finds the plan through filtered Scenario history;
- history includes title, requester, warehouse, assignment, pending stage, and
  non-executable state;
- the assigned reviewer opens the stored request and sees the warehouse and
  product proposal;
- the Coast-scoped assigned reviewer receives the equivalent handoff for the
  Coast plan;
- no approval or rejection is performed by this Phase 4 test.

## 7. Authorization Matrix

| Attempt | Expected result | Evidence path |
|---|---|---|
| Assigned North Review Owner reads assigned plan | Allowed | History and request-detail API |
| Assigned Coast Review Owner reads assigned plan | Allowed | History and request-detail API |
| Same-warehouse alternate Review Owner acts | Denied with assigned-owner message | Direct approve API |
| Same-warehouse alternate Review Owner reads | Visible under current visibility contract | Request-detail API |
| Wrong-warehouse Review Owner reads or acts | Denied | Request-detail and approve APIs |
| Cross-tenant operator reads or acts | Scoped not-found | Request-detail and approve APIs |
| Tenant Admin acts as Review Owner | Denied by role/session authority | Direct approve API |
| Integration Admin acts as Review Owner | Denied by role authority | Direct approve API |
| Integration Operator acts as Review Owner | Denied by role authority | Direct approve API |
| Final Approver acts at Review Owner stage | Denied by required-role check | Direct approve API |
| Escalation Owner acts at Review Owner stage | Denied by required-role check | Direct approve API |
| Anonymous actor acts | Denied before tenant lookup | Direct approve API |
| Inactive assigned Review Owner acts | Denied by active-session/operator check | Direct approve API |
| PREVIEW is submitted to review | Denied; only saved plans require approval | Direct approve API |
| Saved plan with missing persisted reviewer is acted on | Denied | Direct approve API |
| Requester with a Review Owner identity self-reviews | Denied by existing self-review coverage | Existing boundary integration test |

The existing boundary suite also covers session actor binding and requester
spoofing. A client cannot use `approverName`, `reviewerName`, or a declared role
to become another actor; the current authenticated session must supply the
same active operator identity.

## 8. Assignment Change Observation

`ScenarioController` exposes Scenario creation, decision, escalation
acknowledgement, execution, request reload, history, and notifications. It does
not expose a post-save `PUT`/`PATCH` reassignment endpoint. `ScenarioSaveRequest`
can select a reviewer only while creating a new saved plan. The supported
resubmission path creates a new revision from a rejected plan rather than
silently rewriting the original record.

Therefore, a submitted plan's Review Owner cannot be changed by the normal
requester or Tenant Admin APIs in this phase. There is currently no operator
reassignment workflow for an inactive or unavailable assignee. If a reviewer
becomes inactive, the plan is safely blocked rather than silently reassigned;
operator reassignment is a later operational capability, not a Phase 4 change.

## 9. History and Audit Truth

Saving a plan records `SCENARIO_SAVED` and preserves requester, warehouse,
review owner, final approval owner, pending state, risk priority, risk score,
request payload, and deadline. History returns these values without replacing
the persisted assignment with the current viewer.

Negative review attempts do not record `SCENARIO_APPROVED`,
`SCENARIO_REJECTED`, or `SCENARIO_EXECUTED`. Opening history or request details
does not turn a projection into operational truth.

## 10. Operational Side-Effect Proof

The Phase 4 test captures counts before the handoff and denial exercise and
verifies after it that:

- customer orders are unchanged;
- inventory records are unchanged;
- inventory quantities are unchanged;
- live Alerts are unchanged;
- live Recommendations are unchanged;
- the target plan remains `PENDING_APPROVAL` and `PENDING_REVIEW`;
- only the synthetic saved-plan records and expected planning events exist;
- no approval, rejection, or execution event exists from the Phase 4 exercise.

The test's PREVIEW fixture intentionally records its normal
`SCENARIO_ANALYZED` planning event. That is not an operational alert,
recommendation, order, inventory mutation, or governance decision.

## 11. Production Defects Found and Fixes

Two real authorization-boundary defects were found during Phase 4 test
execution:

1. A saved record with a blank persisted `reviewOwner` previously allowed an
   otherwise valid same-warehouse Review Owner to act. The review guard now
   requires a nonblank persisted assignment matching the actor.
2. An anonymous direct review request previously attempted tenant resolution
   before returning an authorization denial and could surface as an internal
   `500` when default tenant fallback was disabled. Scenario record access now
   denies an unauthenticated actor before tenant lookup when the production
   fallback is disabled, while preserving the explicitly enabled test-profile
   fallback used by the existing MVP fixture tests.

Both fixes are narrow service-layer boundary corrections. They do not change
the Scenario API shape, approval policy, frontend behavior, database schema,
or later approval transitions.

## 12. Test and Fixture Defects

The first Phase 4 test attempt exposed two harness issues:

- the anonymous test expected a normal denial but the product returned `500`;
  this was classified as a production defect and fixed above;
- the event count omitted the intentionally recorded `SCENARIO_ANALYZED` event
  from the PREVIEW setup; the expectation was corrected to reflect the
  established planning-event contract.

No established authorization expectation was weakened.

## 13. Verification

Focused command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd -Dtest=PlatformTenantAccessBoundaryIntegrationTest test
```

Focused result after the fixes:

```text
Tests run: 22
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Full backend result:

```text
Tests run: 163
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Frontend results:

```text
npm.cmd run lint    PASS
npm.cmd run build   PASS
npm.cmd run verify  PASS
```

Repository safety results:

```text
git diff --check                          PASS
scripts\docs-link-check.ps1              CLEAN; 766 links checked
scripts\secret-scan.ps1                  PASS; 0 critical findings; 5 known fixture findings
```

Because production service code changed, the revision must be pushed and Render
readiness must be checked after deployment. Hosted approval/rejection proof is
not performed in Phase 4; the accepted owner walkthrough and positive approval
transition remain Phase 5 scope.

## 14. Owner Walkthrough Status

`OWNER-ACCEPT-02` remains intentionally free of fabricated catalog and
inventory data. No manual owner walkthrough is claimed here. This record is
technical integration evidence from an isolated fixture, not customer
operational evidence.

## 15. Phase 4 Verdict

The Phase 4 technical acceptance condition is met by the isolated boundary
coverage only after the full verification gates and deployment readiness check
are complete:

```text
persisted Review Owner
  -> assigned reviewer can find and reload the governed plan
  -> same-role unassigned reviewer cannot act
  -> wrong warehouse cannot act
  -> another tenant cannot target the plan
  -> other roles cannot claim Review Owner authority
  -> inactive or anonymous actors cannot act
  -> malformed missing assignment cannot be claimed
  -> no operational side effects occur
```

The owner operational walkthrough and positive approval transition are
explicitly deferred to Phase 5.
