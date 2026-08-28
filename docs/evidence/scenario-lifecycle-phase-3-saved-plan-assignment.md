# Scenario Lifecycle Phase 3: Saved Plan and Review Owner Assignment

**Status:** Technical evidence complete; operational owner walkthrough remains deferred
**Evidence date:** 2026-08-28
**Starting repository HEAD:** `b80675f807620fdc3dd8b16506b6b38d8eb9f34f`
**Scope:** Save a truthful governed proposal and route it to the exact warehouse Review Owner

## 1. Phase Boundary

This phase proves only that an authenticated operator can preserve a Scenario
proposal as a `SAVED_PLAN` and assign it to a valid warehouse-scoped
`REVIEW_OWNER` without making the hypothetical proposal an actual operation.

This phase does not review, approve, final-approve, reject, escalate, execute,
create real orders, or hand off to an ERP/WMS/source system. Phase 4 and later
workflow decisions remain out of scope.

The owner walkthrough uses a fresh tenant only when the required operational
fixtures already exist. The preserved `OWNER-ACCEPT-02` tenant is intentionally
not populated with artificial product or inventory data solely for this phase.

## 2. Current Repository Truth

The save path is:

```text
POST /api/scenarios/save
  -> ScenarioController.saveScenarioPlan
  -> AccessControlService.requireWorkspaceWarehouseAccess
  -> ScenarioHistoryService.savePlan
  -> ScenarioProjectionService.projectOrderImpact
  -> ScenarioRiskPolicyService.assess
  -> tenant policy determines approval policy
  -> session actor binds requester
  -> active role and warehouse checks validate Review Owner
  -> ScenarioRunRepository saves SAVED_PLAN
  -> BusinessEventService records SCENARIO_SAVED
  -> ScenarioSaveResponse
```

`ScenarioHistoryService.savePlan` recalculates the projection at save time using
the submitted order request. It stores the original request as a JSON
`requestPayload`, plus a derived summary and recommended option. The current
`ScenarioRun` entity does not store projected alert/recommendation arrays as
separate fields. Those arrays remain Scenario projection output; the saved
record preserves the request and derived planning evidence rather than creating
live operational intelligence.

## 3. Persisted Saved-Plan Fields

For a valid save, the current implementation persists:

| Field | Current meaning |
|---|---|
| `tenant` | Current authenticated tenant; tenant context is not client-selectable |
| `type` | `SAVED_PLAN` |
| `title` | User-supplied plan title, trimmed and validated |
| `summary` | Derived planning summary containing projected units, risk priority, score, and exposure counts |
| `recommendedOption` | Derived projected action or the neutral review option |
| `warehouseCode` | The warehouse from the submitted Scenario request |
| `requestPayload` | Serialized `OrderCreateRequest` used to reload the proposal |
| `approvalStatus` | `PENDING_APPROVAL` |
| `approvalPolicy` | `STANDARD` or `ESCALATED`, from the tenant policy and risk priority |
| `approvalStage` | `PENDING_REVIEW` |
| `reviewPriority` / `riskScore` | Derived from the projection |
| `requestedBy` | Canonical authenticated session actor |
| `reviewOwner` | Valid active assigned Review Owner for the selected warehouse |
| `finalApprovalOwner` | Current policy-selected final approver for later workflow stages |
| `approvalDueAt` | Policy-derived review deadline |
| `revisionOfScenarioRunId` / `revisionNumber` | Revision lineage when a rejected saved plan is resubmitted |
| `createdAt` | Persistence timestamp |

The initial saved state is never `APPROVED`, `REJECTED`, `EXECUTED`, or
`FINAL_APPROVED`. It is not executable until later governed workflow state makes
it eligible.

## 4. Requester and Warehouse Rules

- The controller requires an authenticated active operator with access to the
  request warehouse before save processing starts.
- If a non-empty `requestedBy` value is supplied, it must match the authenticated
  session actor. A different same-tenant, tenant-wide, wrong-warehouse,
  cross-tenant, or inactive requester is rejected.
- The saved warehouse is taken from the Scenario request and is preserved in
  the response, database record, reload endpoint, and history.
- Tenant Admin is tenant-wide when its warehouse scope is empty, so it may save
  plans for either established warehouse. That breadth does not widen reviewer
  eligibility.
- A missing warehouse is rejected by request validation/access checks and has no
  reviewer assignment path.

## 5. Review Owner Eligibility

The Review Owner is an assignment target, not the acting identity.

For a selected warehouse, the explicit reviewer must be:

- active;
- in the current tenant;
- assigned the configured `REVIEW_OWNER` role;
- explicitly scoped to the selected warehouse;
- different from the requester; and
- not the bootstrap tenant-wide admin or another tenant-wide reviewer.

The frontend filters candidates when the warehouse changes and excludes the
current actor. The backend repeats the role, tenant, active-state, warehouse,
explicit-scope, and self-review checks. The backend is authoritative.

The current supported API behavior also accepts an empty `reviewOwner` by
selecting an eligible active reviewer for the selected warehouse. This is
automatic assignment within the same save operation, not an unvalidated or
tenant-wide reviewer fallback. The frontend normally supplies an explicit
candidate before enabling Save.

## 6. North and Coast Assignment Evidence

`PlatformTenantAccessBoundaryIntegrationTest` provisions two synthetic
warehouses and warehouse-scoped role identities in the same tenant.

The Phase 3 test proves:

| Requester scope | Scenario warehouse | Expected assigned reviewer | Result |
|---|---|---|---|
| North-scoped Integration Operator | `WH-NORTH` fixture warehouse | North-scoped `REVIEW_OWNER` | `SAVED_PLAN` created |
| Coast-scoped Integration Operator | `WH-COAST` fixture warehouse | Coast-scoped `REVIEW_OWNER` | `SAVED_PLAN` created |
| Tenant Admin | `WH-NORTH` | North-scoped reviewer only | Existing boundary test passes |
| Tenant Admin | `WH-COAST` | Coast-scoped reviewer only | Existing boundary test passes |

The fixture resolves warehouse codes from the persisted onboarding result rather
than assuming which sorted warehouse is North or Coast. The saved response,
database record, reload endpoint, and filtered history all preserve the chosen
warehouse and assignment.

## 7. Negative API Matrix

The repository-backed tests cover these outcomes:

| Attempt | Result |
|---|---|
| Anonymous save | `403` |
| Missing warehouse | `400` |
| Scoped requester saving to another warehouse | `403` |
| Wrong-warehouse Review Owner | `403` |
| Cross-tenant Review Owner | `400` because the target is not a current-tenant operator |
| Inactive Review Owner | `400` |
| Wrong-role operator as reviewer | `400` |
| Bootstrap tenant-wide reviewer target | `400` |
| Tenant-wide reviewer target without explicit warehouse scope | `400` |
| Requester assigned as Review Owner | `400` |
| Client `requestedBy` spoof | `403` |
| Empty reviewer field | Eligible warehouse reviewer is selected automatically by the supported save contract |

No negative request creates a valid saved plan as a result of the rejected
assignment. Existing creation-boundary tests also cover missing products,
invalid quantities, and anonymous or cross-warehouse Scenario requests.

## 8. Projection Preservation and Side Effects

The Phase 3 focused test captures before/after values around two valid saves.
It verifies:

- North and Coast request warehouse codes remain unchanged;
- request quantities and SKU are present in the persisted `requestPayload`;
- the derived summary retains projected planning context;
- the reload endpoint returns the saved request;
- history reports the real requester, warehouse, Review Owner, pending state,
  and `executable=false`;
- customer order count is unchanged;
- inventory record count and available quantities are unchanged;
- persisted alert count is unchanged;
- persisted recommendation count is unchanged;
- only two Scenario records and two `SCENARIO_SAVED` planning events are added;
- no `SCENARIO_APPROVED` or `SCENARIO_EXECUTED` event is added.

Saving therefore preserves the proposal without claiming that the projected
condition occurred. A projected warning or recommendation remains Scenario
planning evidence, not a live Alert or Recommendation.

## 9. History, Activity, and Realtime

The save operation records `SCENARIO_SAVED` business history for planning and
governance traceability. The current save path does not call the realtime
operational update publisher. No operational alert, inventory, order,
fulfillment, dispatch, or runtime incident event is emitted by this phase.

History and reload visibility are tenant-scoped. Warehouse-scoped operators are
filtered to their assigned warehouse context by the Scenario history query and
the Scenario run access check. Visibility of a record is not treated as
approval authority; later decision endpoints apply their own assigned-role and
assignment checks.

## 10. Revision and Immutability Observation

The current supported revision path creates a new `SAVED_PLAN` from a rejected
saved plan and records `revisionOfScenarioRunId` plus an incremented revision
number. Phase 3 does not redesign that policy. The original record remains in
history, and the new proposal carries its own request payload and governance
assignment.

Direct post-save editing of the existing record is not introduced by Phase 3.
Any future revision semantics must preserve the rule that a reviewer never
silently reviews a materially replaced proposal.

## 11. Automated Verification

Focused command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd -Dtest=PlatformTenantAccessBoundaryIntegrationTest test
```

Final focused result:

```text
Tests run: 21
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

The first attempt was blocked by restricted Maven dependency networking. The
same command was rerun with dependency access and reached compilation. One
test expectation was corrected from an assumed risk priority to the actual
fixture policy result; one lazy tenant dereference was removed from the test
harness. Neither was a production defect.

The focused suite also emits expected logs for deliberately disabled webhook
and authorization scenarios. Those are test-fixture exercise paths, not Phase
3 save failures.

Required follow-up verification before committing:

- full backend Maven suite;
- frontend lint/build/verify, because repository verification remains a release
  gate;
- `git diff --check`;
- secret scan;
- documentation link check.

Hosted proof is not required for this test-only/evidence change. No production
runtime source, API contract, frontend behavior, database migration, or
deployment configuration changed.

## 12. Manual Owner Verification

Manual owner verification remains deferred when the preserved owner-acceptance
tenant lacks meaningful product/inventory data. No fake operational data should
be added only to satisfy this phase. A later operational acceptance run should
use legitimate catalog and inventory data to confirm:

- North save routes only to the North Review Owner;
- Coast save routes only to the Coast Review Owner;
- the assigned reviewer can find the governance work without performing review;
- wrong and unassigned reviewers cannot claim the assignment;
- projection output remains visibly hypothetical;
- live operational surfaces remain unchanged after save.

## 13. Defects and Changes

**Production defects found:** None.

**Test/evidence changes:**

- Added Phase 3 saved-plan persistence, assignment, side-effect, reload, and
  history assertions to the existing access-boundary integration suite.
- Added cross-tenant, inactive, wrong-role, and missing-warehouse save denial
  assertions.
- Added this evidence record.

**Runtime behavior changed:** No.

**Deployment required:** No. The change is test-only plus evidence
documentation.

## 14. Verdict

**Critical blockers:** 0 identified in repository-backed Phase 3 evidence.

**High blockers:** 0 identified in repository-backed Phase 3 evidence.

**Medium follow-up:** Manual owner walkthrough with meaningful operational data
remains deferred; current saved-plan revision policy remains an observation for
the later revision phase.

**Verdict:** `PHASE 3 TECHNICALLY ACCEPTED - OPERATIONAL OWNER WALKTHROUGH DEFERRED - READY FOR PHASE 4`

This verdict proves only that a truthful what-if proposal can be preserved and
handed to the correct warehouse Review Owner without pretending that a real
operation has occurred.
