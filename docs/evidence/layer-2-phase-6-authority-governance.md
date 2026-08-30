# Layer 2 Phase 6: Whole-System Authority, Isolation and Scenario Governance

## Scope and evidence posture

This report closes the bounded Layer 2 Phase 6 review at the repository revision
that existed before this evidence-only change: `5415869fe26b6ffcd71d9195099717b0f45d40c3`.
The review covers tenant, role, warehouse, assignment, workflow-state, platform,
operational, and Scenario governance boundaries across the existing system.

No production code, frontend code, configuration, deployment, hosted tenant, or
`OWNER-ACCEPT-02` data was changed. The local tests use disposable H2 fixtures and
the established MockMvc/API paths. No hosted proof was run in this phase.

The evidence reuses the existing cross-domain authority suite rather than adding
a duplicate test class. The principal executable sources are:

- `backend/src/test/java/com/synapsecore/PlatformTenantAccessBoundaryIntegrationTest.java`
- `backend/src/test/java/com/synapsecore/MvpFlowIntegrationTest.java`
- the Layer 2 Phase 1-4 integration suites
- `backend/src/test/java/com/synapsecore/config/WebSocketAccessBoundaryTest.java`

## Authority model

SynapseCore authority is evaluated as a tuple: tenant, role, warehouse scope,
persisted assignment, and workflow state. A successful authentication response is
not by itself permission to perform an unrelated action. Scenario governance is
planning and decision support only: approval does not execute operational work,
create an Order, mutate Inventory, perform Fulfillment, or publish projected
intelligence as live truth. `/api/scenarios/{id}/execute` remains compatibility
`410 Gone`.

## Classification key

- **A:** an observed implementation contradiction or release-blocking authority failure.
- **B:** an implementation gap that should be fixed before claiming the contract.
- **C:** bounded evidence not executed in this local phase, without an observed product contradiction.
- **D:** future infrastructure or scale evolution outside this phase.

## 83-item closure report

1. **Starting HEAD:** `5415869fe26b6ffcd71d9195099717b0f45d40c3` on `main`; this was the actual pre-Phase 6 HEAD.
2. **Fixture design:** Existing authority fixtures provision disposable Tenant A/Tenant B data, separate warehouses, isolated users, products, inventory, Orders, integrations, and Scenarios through supported test/API paths.
3. **Actor/role/scope matrix:** TENANT_ADMIN is tenant-wide for supported setup/catalog/inventory; Integration Admin/Operator, Review Owner, Final Approver, and Escalation Owner are role-specific and warehouse-scoped where assigned.
4. **Tenant-A operational state:** The rehearsal tenant has independently persisted catalog, inventory, Order, integration, replay, Scenario, Activity, Audit, Dashboard, and Runtime state in the local boundary suite.
5. **Tenant-B operational state:** The isolation fixture persists independent tenant data and verifies that Tenant A sessions cannot read or mutate it.
6. **WH-A/WH-B isolation:** Warehouse-scoped identities can read and mutate only their assigned lane; tenant-wide identities retain supported tenant-wide authority.
7. **Integration role matrix:** Integration Admin and Integration Operator are separated from governance roles; source ingress, connector administration, and replay operations are checked against role and scope.
8. **Tenant Admin boundary:** Tenant Admin is not treated as universal integration, fulfillment, replay, or Scenario-governance authority; unsupported cross-domain actions are denied.
9. **Governance-role boundary:** Review, final approval, and escalation responsibilities are distinct and tied to persisted workflow assignments.
10. **Integration-role admin/governance boundary:** Integration identities cannot approve, reject, or bypass Scenario governance merely because they can perform source-system work.
11. **Scoped read:** Warehouse-scoped reads filter operational data, integrations, replay, intelligence, Dashboard, Activity, Runtime, and Scenario visibility to the authorized lane where the domain supports scope.
12. **Cross-tenant read:** Tenant A sessions cannot read Tenant B operational or governance data through direct tenant endpoints.
13. **Dashboard scope:** Dashboard summaries use tenant and warehouse authority rather than exposing another tenant or unauthorized warehouse pressure.
14. **Activity/Audit scope:** Tenant Activity and Audit evidence remain tenant-scoped; platform views remain metadata-first and do not become a tenant business-data bypass.
15. **Realtime regression:** Websocket access-boundary tests cover anonymous, unknown, cross-tenant, raw-feed, scoped-feed, and authorized destination behavior without changing REST authority.
16. **Platform Owner boundary:** Platform Owner uses a dedicated platform session and control-plane endpoints; a tenant session does not inherit platform authority.
17. **Platform/tenant session replacement:** Switching from a platform session to a tenant login replaces authority instead of combining both session identities.
18. **Explicit tenant-wide intent:** Empty warehouse scope is interpreted as tenant-wide authority only for roles and endpoints whose product contract explicitly permits it.
19. **Scenario preview:** Preview calculates what-if effects from the selected tenant, warehouse, product, and proposed action without making the projection operational truth.
20. **Preview zero mutation:** Scenario preview does not create Orders, mutate Inventory, dispatch Fulfillment, persist live Alerts or Recommendations, or publish projected conditions as live operational state.
21. **Wrong warehouse preview:** A scoped requester cannot preview against an unauthorized warehouse; tenant and warehouse checks occur before projection work.
22. **Scenario save:** Save persists a governed planning record with tenant, requester, warehouse, proposal, status, and evidence needed for later review.
23. **Requester identity:** The persisted requester is derived from the authenticated session actor, not a caller-supplied identity field.
24. **Review Owner assignment:** Review assignment requires an eligible REVIEW_OWNER with an explicit matching warehouse scope and persists that assignment.
25. **Bootstrap-admin reviewer exclusion:** The bootstrap/tenant-wide administrative identity is not silently substituted for an assigned Review Owner in the governed path.
26. **Wrong-review-owner:** A different or unassigned Review Owner cannot make the assigned review decision.
27. **Wrong-warehouse reviewer:** A Review Owner assigned to another warehouse cannot review the Scenario.
28. **Self-review:** The requester cannot review its own governed Scenario; the separation-of-duties rule is enforced at the decision boundary.
29. **STANDARD workflow:** Standard-risk Scenarios use the persisted review stage and assigned Review Owner before any later final governance step.
30. **ESCALATED initial:** Escalated-risk Scenarios enter the escalated governance path and do not skip required review or final approval stages.
31. **Premature final approver:** A Final Approver cannot decide while the Scenario is still at the review stage.
32. **Review Owner advance:** Only the assigned Review Owner can advance a pending review through the supported governance transition.
33. **Review Owner final denial:** The assigned Review Owner can reject at the review stage; rejection is recorded as governance history and does not perform operational work.
34. **Wrong final approver:** A Final Approver without the persisted assignment, correct role, warehouse scope, or workflow stage is denied.
35. **Assigned final approver:** The assigned Final Approver can perform the final governance decision when the Scenario reaches the applicable stage.
36. **Separation of duties:** Requester, Review Owner, Final Approver, and Escalation Owner responsibilities are not interchangeable.
37. **Review rejection:** Review rejection ends the applicable governed path without creating live operational effects.
38. **Final rejection:** Final rejection records the governance outcome without creating an Order, Inventory change, Fulfillment task, or live intelligence side effect.
39. **Revision/resubmission:** Revision and resubmission preserve governance chronology and require the new decision path to re-evaluate assignment and stage rules.
40. **Review-stage SLA non-escalation:** The current contract keeps review decision authority with the Review Owner; existing Phase 7 evidence treats review-stage SLA escalation as bounded C evidence rather than granting escalation authority.
41. **Final-stage SLA escalation:** Final-stage overdue handling is separate from final decision authority and is covered by the existing assigned Escalation Owner SLA boundary tests.
42. **Escalation Owner acknowledgement:** An assigned Escalation Owner may acknowledge supported overdue governance attention only; acknowledgement does not approve or reject.
43. **Wrong Escalation Owner:** An unassigned or wrong-warehouse Escalation Owner cannot acknowledge the Scenario SLA.
44. **Escalation Owner approval denial:** Escalation Owner authority does not become Review Owner or Final Approver authority.
45. **Final after SLA acknowledgement:** SLA acknowledgement preserves the pending governance stage and leaves the assigned final decision owner in control.
46. **Zero operational mutation:** Preview, save, review, rejection, revision, approval, and SLA acknowledgement preserve live Orders, Inventory, Fulfillment, Alerts, Recommendations, and source-system truth unless an explicitly supported source event changes them.
47. **`/execute`:** `POST /api/scenarios/{id}/execute` remains compatibility `410 Gone`; no role receives an operational Scenario execute capability.
48. **No execution evidence:** Existing Scenario evidence does not treat an approved plan as an executed Order, Inventory mutation, Fulfillment action, or live intelligence promotion.
49. **Later real source event:** Later authoritative source activity is the only path that can establish corresponding live operational truth; Scenario approval alone cannot do so.
50. **Scenario/live independence:** Scenario projections and governance history remain separate from live Dashboard, Alerts, Recommendations, Runtime incidents, and operational Activity.
51. **Scenario history scope:** Scenario history is tenant and warehouse-authority aware and does not expose another tenant's governance records.
52. **Scenario notification scope:** Scenario notifications are filtered by tenant, warehouse, role, and assignment; direct endpoint checks are included in the authority suite.
53. **Actor spoofing:** Caller-supplied actor identity cannot replace the authenticated session actor for Scenario or operational mutations.
54. **Role spoofing:** Caller-supplied role or warehouse fields cannot elevate the authenticated role or scope.
55. **Session revocation:** Disabled users and changed scopes are revalidated; existing sessions lose authority when the persisted identity is no longer eligible.
56. **Inactive warehouse:** Inactive or unauthorized warehouse references are rejected rather than treated as an implicit tenant-wide fallback.
57. **Successful Audit identity:** Successful governed and operational mutations record the authenticated actor, tenant, target, request, and status where the domain's audit contract requires it.
58. **Denied-action false-success matrix:** Denied role, tenant, warehouse, assignment, stage, anonymous, and platform/tenant crossover attempts do not report successful business mutation.
59. **Scenario Activity chronology:** Scenario governance history records planning and decision chronology without claiming that a projection became live operational activity.
60. **Operational Activity separation:** Operational Activity remains sourced from actual operational events, not hypothetical Scenario output.
61. **Authority truth ledger:** Tenant, role, warehouse, assignment, and workflow state are checked together at the protected boundary; a valid login alone is insufficient.
62. **Isolation truth ledger:** Tenant qualification is retained across repositories, summaries, integrations, replay, Scenario, Activity, Audit, Runtime, and websocket destination checks.
63. **Tests added:** No new Phase 6 test class was added; existing coverage already exercises the requested cross-domain seams without duplication.
64. **Tests reused:** `PlatformTenantAccessBoundaryIntegrationTest` (34), `MvpFlowIntegrationTest` (90), Layer 2 Phase 1 (1), Phase 2 (1), Phase 3 (4), Phase 4 (3), and `WebSocketAccessBoundaryTest` (10).
65. **Production defects:** No new production defect was observed in the reviewed authority, isolation, or Scenario governance paths.
66. **Authority/security defects:** No Classification A or B authority contradiction was found in the exercised local contracts.
67. **Fixes:** No runtime fix was needed; this phase adds only the evidence document.
68. **Focused result:** The exact focused set passed: 143 tests, 0 failures, 0 errors, 0 skipped.
69. **Adjacent regression:** The focused set includes Scenario, authentication/session, admin, warehouse, platform, integration, Layer 2, and websocket boundary regressions; all passed.
70. **Full backend result:** Full local backend suite passed: 279 tests, 0 failures, 0 errors, 0 skipped.
71. **Frontend result:** No frontend files changed, so frontend verification was not required for this evidence-only phase; the previously accepted frontend baseline remains the applicable reference.
72. **Docs/diff/secret scan:** Final repository checks are recorded below after this document is created: docs links, secret scan, and `git diff --check` must be green before commit.
73. **Files changed:** Intended Phase 6 change is this evidence document only; unrelated `frontend/Dockerfile`, `.gitattributes`, and the two Scenario evidence files remain untouched and unstaged.
74. **Commits:** Evidence commit `94085abb99190d7a773cb16bff61dcc36cfe4acd` contains this document only; no production, frontend, configuration, or deployment file is included.
75. **GitHub Actions run:** Exact-main CI run `33336268412` for commit `94085abb99190d7a773cb16bff61dcc36cfe4acd` completed with `success`.
76. **Retained Phase 5 C:** Frontend realtime harness depth, dedicated cache/broker exercises, and after-commit publisher-failure execution remain the explicitly bounded Phase 5 C items.
77. **Remaining B:** 0 known Phase 6 implementation blockers from the exercised authority and governance contracts.
78. **Remaining C:** Dedicated hosted scoped-user timing, destructive live scope-removal and warehouse-retirement exercises, authenticated hosted warehouse realtime, owner/browser walkthrough, and any source-event correlation not executed by the local suites remain C evidence gaps.
79. **Remaining D:** Broader distributed realtime, HA, queue/worker separation, and scale evolution remain outside this bounded authority phase.
80. **Critical blockers:** 0 identified.
81. **High blockers:** 0 identified.
82. **Classification A remaining:** 0. The remaining C items are explicitly bounded evidence gaps, not observed product contradictions.
83. **Final verdict:** `LAYER 2 PHASE 6 - WHOLE-SYSTEM AUTHORITY, TENANT ISOLATION, WAREHOUSE ISOLATION AND SCENARIO GOVERNANCE VERIFIED FOR CONTROLLED PILOT; DEDICATED LIVE EVIDENCE REMAINS CLASSIFICATION C`. Phase 7/final technical acceptance is not started.

## Verification commands

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd test

cd ..
powershell -ExecutionPolicy Bypass -File scripts\docs-link-check.ps1
powershell -ExecutionPolicy Bypass -File scripts\secret-scan.ps1
git diff --check
```

No hosted proof is part of this Phase 6 local evidence pass.
