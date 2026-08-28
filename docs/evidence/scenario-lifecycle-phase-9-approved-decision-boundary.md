# Scenario Lifecycle Phase 9: Approved Decision Boundary

## Closure scope

Phase 9 closes the boundary between a governed Scenario decision and the
authoritative system that performs the real business action. SynapseCore can
preview, save, review, approve, reject, revise, escalate, and audit a Scenario.
It does not execute an approved Scenario into a new order or inventory change.

The governing rule is:

> PREVIEW describes what would happen. APPROVED records that people agreed.
> Authoritative source data is still required before SynapseCore treats the
> business operation as real.

This phase does not start Phase 10, CSV/recovery work, or full source-system
reconciliation.

## Repository trace before the change

The legacy implementation contained an internal Scenario execution path:

1. `ScenarioController` injected `ScenarioExecutionService` and exposed
   `POST /api/scenarios/{scenarioRunId}/execute`.
2. `ScenarioExecutionService.execute(...)` loaded the Scenario, requested an
   executable order payload from `ScenarioHistoryService`, and called
   `OrderService.createOrder(...)`.
3. It then called `ScenarioHistoryService.recordExecution(...)`, which created
   a `ScenarioRunType.EXECUTION` history row containing the live order summary.
4. The service emitted `BusinessEventType.SCENARIO_EXECUTED` and returned an
   execution response.
5. `AccessControlService.requireScenarioExecutor(...)` allowed a
   `REVIEW_OWNER` or `FINAL_APPROVER` with warehouse access to use that path.
6. Read DTOs exposed `executable` and `executionReady` as true for saved,
   approved plans.

Repository inspection found no other production caller, scheduled listener, or
event listener that executed Scenarios. `SystemRuntimeService` and the schema
still retain historical `SCENARIO_EXECUTED` representations for compatibility
and readback; the Phase 9 implementation creates no new execution records or
events.

## Phase 9 implementation

The smallest production seam was closed rather than replacing the Scenario
engine:

- `ScenarioExecutionService` and its execution response DTO were removed.
- `ScenarioController` retains the old route only as an explicit compatibility
  denial. An authorized, in-scope request receives HTTP 410 with a message
  that approved decisions are handed off for external action.
- Existing tenant, session, and warehouse checks execute before that denial.
- `ScenarioHistoryService` no longer builds executable order requests or records
  execution history. Compatibility fields remain present but are always false.
- `AccessControlService.requireScenarioExecutor(...)` was removed because no
  Scenario executor authority remains.
- The frontend no longer presents an Execute Scenario control. Approval copy
  describes a governed decision ready for external follow-through.
- Hosted-proof helpers now verify an approved decision and the absence of an
  Execute control; they do not create an order from a Scenario.

The historical enum and database event values remain intentionally preserved so
existing historical data is readable. Preservation is not permission to create
new execution state.

## Resulting API boundary

For `POST /api/scenarios/{scenarioRunId}/execute` after Phase 9:

| Request context | Result |
| --- | --- |
| Authenticated tenant session with the Scenario in scope | HTTP 410; approved decisions are handed off for external action |
| Authenticated tenant session with a different warehouse scope | HTTP 403 |
| Authenticated session from another tenant | HTTP 404 |
| Anonymous or missing session | HTTP 403 |

The same compatibility denial is exercised against both standard-risk and
escalated approved plans. PREVIEW, pending-review, pending-final-approval,
rejected, and revised plans do not acquire an executable state.

No Scenario state grants execution authority to `TENANT_ADMIN`, `REVIEW_OWNER`,
`FINAL_APPROVER`, `INTEGRATION_ADMIN`, `INTEGRATION_OPERATOR`, or
`ESCALATION_OWNER`. Those roles retain only their existing governance or
operational permissions.

## Governance and mutation invariants

The focused boundary coverage verifies that an approved decision is not an
operational write. Across standard and escalated approval paths, an attempted
Scenario execution leaves unchanged:

- orders and order items
- inventory quantities
- fulfillment and dispatch state
- live alerts
- live recommendations
- Scenario execution-run count
- new `SCENARIO_EXECUTED` events

Approval, final approval, rejection, revision, assignment, warehouse scope,
tenant isolation, and existing Scenario history behavior remain covered by the
existing integration suites. The `executable` and `executionReady` response
fields remain for compatibility and report false.

## Focused verification

Passed locally:

- `MvpFlowIntegrationTest`: 80 tests, 0 failures/errors
- `PlatformTenantAccessBoundaryIntegrationTest`: 28 tests, 0 failures/errors
- Focused total: 108 tests, 0 failures/errors
- Full backend Maven suite: 169 tests, 0 failures/errors
- Frontend lint/check: passed; 72 source files checked
- Frontend production build: passed
- Frontend verify: passed
- `git diff --check`: passed; only normal LF-to-CRLF warnings were emitted

The full backend suite also continued to pass the existing realtime,
integration, replay, security, migration, and production-hardening coverage.
Expected test logs include deliberate disabled-connector failures and denied
authorization attempts; they are test evidence, not hidden success claims.

## Frontend proof boundary

The production proof contract now asserts:

- the Scenario reaches `APPROVED` with `executable === false`
- approval messaging says the decision is governed and ready for external
  action
- the Execute Scenario button is absent
- the Scenario remains a planning/governance surface rather than an order
  creation surface

The hosted positive proof suite was not rerun for this phase because its prior
execution lane intentionally created operational data. The Phase 9 change
removes that side effect, so a positive hosted run would require a new,
explicitly safe fixture contract rather than silently reusing an execution
assertion. Hosted readiness may be checked independently; readiness is not
substituted for proof of this code revision.

## Files in this closure

Intended Phase 9 changes are limited to the Scenario backend boundary, its
focused tests, the related frontend/proof wording and controls, and this
evidence record. The following pre-existing unrelated worktree changes are not
part of this closure and must remain unstaged:

- `frontend/Dockerfile`
- `.gitattributes`
- `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`

No credentials, passwords, tokens, proof-state JSON, or local environment files
are included in this evidence.

## Closure classification

- Critical blockers: 0
- High blockers: 0
- Phase 9 production boundary: enforced locally and covered by focused/full
  backend verification
- Hosted positive proof for this new negative boundary: intentionally deferred
  pending a safe non-mutating fixture path
- Phase 10: not started

The approved-decision boundary is closed: SynapseCore governs and records the
decision, then hands the decision to the external authoritative system instead
of executing it internally.
