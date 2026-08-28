# Scenario Lifecycle Phase 7: Escalation Owner and SLA Authority

## Scope

This document records the Phase 7 backend acceptance evidence for overdue,
escalated Scenario plans. It covers SLA escalation ownership and acknowledgement
only. It does not approve, reject, execute, or otherwise advance a Scenario, and
it does not create orders or mutate inventory.

The evidence is based on the source baseline `c9495927266896edc514bc0eb1c0ad2a2b2faece`
plus the Phase 7 changes in the working tree. The production change is intentionally
small: acknowledgement now requires the exact escalated workflow state and a
non-blank exact persisted escalation owner.

## Acceptance Census

1. **Starting HEAD:** `c9495927266896edc514bc0eb1c0ad2a2b2faece` before Phase 7 changes.
2. **SLA trigger:** `ScenarioHistoryService.applyPendingSlaEscalations()` queries tenant-scoped saved plans that are `PENDING_APPROVAL`, use `ESCALATED` policy, are at `PENDING_FINAL_APPROVAL`, have `approvalDueAt <= now`, and have no prior `slaEscalatedAt`.
3. **Deadline calculation:** `approvalDueAt` is an `Instant`; final-approval deadlines come from the current tenant operational policy and are computed as `Instant.now() + policy duration`.
4. **Deadline boundary:** The predicate uses `!dueAt.isAfter(Instant.now())`, so an exact deadline is eligible and a future deadline is not.
5. **Escalation assignment source:** The escalation owner is resolved from the current tenant policy's `escalationOwnerRole`, then filtered by active role and warehouse eligibility; the selected identity is persisted in `slaEscalatedTo`.
6. **Authority predicate:** Acknowledgement requires the saved-plan type, pending approval status, escalated policy, pending-final stage, an SLA timestamp, declared `ESCALATION_OWNER`, the authenticated actor, warehouse access, the exact persisted owner, and a non-blank note.
7. **Primary warehouse result:** The primary fixture warehouse (`warehouseA`, persisted as `WH-COAST` in this test setup) escalated to the scoped `boundary.escalation` owner and was acknowledged only by that owner.
8. **Secondary warehouse result:** The secondary fixture warehouse (`warehouseB`, persisted as `WH-NORTH` in this test setup) escalated to the separate scoped `boundary.escalation.b` owner and was acknowledged only by that owner.
9. **Same-warehouse alternate:** `boundary.escalation.alt` has the same warehouse scope as the primary owner but is not the persisted `slaEscalatedTo`; acknowledgement was denied.
10. **Wrong warehouse:** The owner scoped to the other fixture warehouse was denied with `403` when acknowledging the primary plan.
11. **Cross-tenant:** The isolation tenant session could not acknowledge the rehearsal tenant plan; the tenant-scoped lookup returned `404`.
12. **Tenant Admin:** Tenant Admin could not acknowledge an escalation; tenant-wide administration does not grant escalation-owner authority.
13. **Review Owner:** Review Owner could not acknowledge an escalation; review authority remains separate from SLA ownership.
14. **Final Approver:** Final Approver could not acknowledge an escalation; final approval authority remains separate from SLA acknowledgement.
15. **Integration Admin:** Integration Admin could not acknowledge an escalation.
16. **Integration Operator:** Integration Operator could not acknowledge an escalation.
17. **Inactive identity:** Existing inactive-role rejection coverage remains green in the full access-boundary suite; inactive authority is not accepted as a substitute owner.
18. **Anonymous session:** An empty session was denied with `403`.
19. **Actor spoofing:** A real escalation-owner session declaring a different actor was denied with `403`; the session actor remains canonical.
20. **Non-overdue plan:** A future-due plan was not escalated and acknowledgement was rejected as not SLA escalated.
21. **Wrong workflow state:** PREVIEW, approved, and rejected plans were denied; acknowledgement is not a general Scenario action.
22. **Duplicate acknowledgement:** A second acknowledgement by the same assigned owner is idempotent and returns the existing state.
23. **Review-stage SLA:** An overdue `PENDING_REVIEW` plan remained review-owned and did not receive SLA escalation. The review deadline supports overdue visibility, but the current product contract does not give an Escalation Owner an acknowledgement path at this stage.
24. **Final-stage SLA:** An overdue `PENDING_FINAL_APPROVAL` plan escalated automatically and persisted its escalation owner.
25. **Review bypass:** An Escalation Owner could not call the approval path as a Review Owner; the service required `FINAL_APPROVER` for the final stage.
26. **Final-approval bypass:** Acknowledgement did not approve the plan and did not bypass the final approver.
27. **Acknowledgement semantics:** Acknowledgement records owner follow-up only. It leaves `PENDING_FINAL_APPROVAL` and `PENDING_APPROVAL` unchanged.
28. **Note behavior:** The note is required by request validation and is trimmed before persistence; blank notes are rejected.
29. **History and audit:** Successful escalation records `SCENARIO_SLA_ESCALATED`; successful acknowledgement records `SCENARIO_SLA_ACKNOWLEDGED`. No new realtime event was introduced by Phase 7.
30. **Realtime/activity boundary:** Existing scenario escalation publication remains separate from operational alert/recommendation truth; no operational activity is fabricated by acknowledgement.
31. **Orders before/after:** The primary Phase 7 test observed no change in tenant order count before versus after escalation and acknowledgement.
32. **Inventory before/after:** The primary Phase 7 test observed no change in tenant inventory count.
33. **Fulfillment before/after:** The primary Phase 7 test observed no change in fulfillment task count.
34. **Dispatch before/after:** The primary Phase 7 test observed no change in operational dispatch work-item count.
35. **Alerts before/after:** The primary Phase 7 test observed no change in alert count.
36. **Recommendations before/after:** The primary Phase 7 test observed no change in recommendation count.
37. **Production defects found:** The prior acknowledgement precondition was too broad and allowed states outside the escalated final-approval workflow; a blank or missing persisted owner was not rejected strongly enough.
38. **Test/fixture defects found:** The new tests initially assumed literal warehouse labels, while the shared fixture assigns `warehouseA`/`warehouseB` by persisted name order. The tests were corrected to use authoritative fixture variables. One actor-spoof assertion also expected `400` although the enforced session mismatch correctly returns `403`.
39. **Expectation changes:** No security expectation was weakened. Only fixture naming and the expected HTTP status for actor spoofing were corrected to match the existing authority contract.
40. **Fixes applied:** `acknowledgeSlaEscalation` now requires `ESCALATED` plus `PENDING_FINAL_APPROVAL`; `requireAssignedEscalationOwner` now requires a non-blank exact persisted owner; Phase 7 tests cover positive, negative, duplicate, deadline, and side-effect cases.
41. **Focused tests:** `PlatformTenantAccessBoundaryIntegrationTest` passed `27/27` after the fixture corrections.
42. **Full backend suite:** `cmd /c mvnw.cmd test` passed `168/168` with zero failures, errors, or skips.
43. **Frontend checks:** Frontend source was not changed in Phase 7; frontend lint/build/verify remain the prior green baseline and are rerun as a repository gate below.
44. **Repository gates:** `git diff --check`, secret scan, and documentation-link check are required before commit; ignored proof state and local environment files remain untracked and unstaged.
45. **Files changed for Phase 7:** `ScenarioHistoryService.java`, `PlatformTenantAccessBoundaryIntegrationTest.java`, this evidence document, and `docs/INDEX.md`. Unrelated `frontend/Dockerfile`, `.gitattributes`, and Phase 0 census changes are not part of this closure.
46. **Deployment/live readiness:** Commit `0cc1723` was pushed to `origin/main`. After Render warm-up, the live check confirmed `FRONTEND_UP=True`, `BACKEND_UP=True`, `DB_READY=True`, `AUTH_READY=True`, `WS_READY=True`, and `PROOF_ALLOWED=True`; readiness, liveness, auth session, and SockJS returned `200`. The aggregate `/actuator/health` probe timed out during the check while its direct readiness/liveness probes were healthy, so that transient probe behavior remains an operational limitation. Hosted proof is not required for this backend-only change and Scenario execution remains out of scope.
47. **Critical blockers:** None identified by the Phase 7 focused or full backend suites.
48. **High blockers:** None identified by the Phase 7 focused or full backend suites.
49. **Medium/Low limitations:** The fixture uses a simple in-process policy/owner resolver and existing realtime broker; broader production scale, managed operational alerting, and a manual owner walkthrough remain outside this phase. The aggregate health endpoint timed out during the post-push probe even though direct readiness/liveness were healthy and the final connection classification allowed proof.
50. **Manual walkthrough:** Deferred. This phase is backend authority and SLA evidence; no browser Scenario execution was performed.
51. **Phase 8 readiness:** Phase 8 must not begin until the post-push live readiness check is green and this Phase 7 evidence is committed. No Phase 8 work is started by this document.
52. **Phase 7 verdict:** `PHASE 7 TECHNICALLY ACCEPTED — OPERATIONAL OWNER WALKTHROUGH DEFERRED — READY FOR PHASE 8`.

## Review-stage SLA Scope

The accepted SLA model is **Model B**: only an overdue `PENDING_FINAL_APPROVAL`
plan is automatically rerouted to an Escalation Owner for SLA acknowledgement.
The Review Owner remains the sole decision authority while a plan is in
`PENDING_REVIEW`.

Both pending stages have an `approvalDueAt` deadline and may therefore be
identified as overdue. That deadline is not, by itself, an escalation grant.
Review-stage overdue status supports visibility and follow-up by the assigned
Review Owner; it does not populate `slaEscalatedTo`, create a review-stage SLA
acknowledgement, or transfer review authority. The escalation fields, trigger
query, notification wording, and acknowledgement contract are intentionally
final-approval-specific. This preserves the distinction between:

```text
Review Owner       = decides the review stage
Escalation Owner   = acknowledges an overdue final-approval SLA only
Final Approver     = decides the final-approval stage
```

Introducing Escalation Owner handling for overdue `PENDING_REVIEW` plans would
be a new product contract and data-model decision, not a missing Phase 7 fix.

## Exact Runtime Boundary

The supported flow is:

```text
saved plan
  -> escalated policy + pending final approval
  -> approval deadline reached
  -> persisted escalation owner selected for the plan warehouse
  -> assigned escalation owner acknowledges with a note
  -> plan remains pending final approval
  -> assigned final approver remains responsible for approval/rejection
```

Acknowledgement is not approval, execution, inventory movement, order creation,
alert creation, or recommendation publication. The existing tenant-scoped
session and warehouse checks remain authoritative.
