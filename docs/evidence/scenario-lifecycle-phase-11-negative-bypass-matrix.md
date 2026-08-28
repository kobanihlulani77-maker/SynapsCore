# Scenario Lifecycle Phase 11: Negative and Bypass Matrix

## Scope and verdict

This evidence record attacks the complete current Scenario surface for identity,
tenant, warehouse, assignment, workflow-state, SLA, revision, execution, and
projected-intelligence boundaries. It does not start Phase 12, expand the
integration model, or introduce a Scenario-to-source-system correlation claim.

The governing separation remains:

> A Scenario projection describes what could happen. Governance records what
> people decided. Authoritative operational data is required before a business
> operation becomes actual.

**Current technical verdict:** `PHASE 11 ACCEPTED - FULL SCENARIO
NEGATIVE/BYPASS MATRIX VERIFIED - READY FOR PHASE 12`

The repository and local integration evidence show zero Critical or High
Scenario bypasses. The current working revision includes two narrow fixes:

1. Direct Scenario notifications now filter by the authenticated operator's
   warehouse scope.
2. Scenario warehouse authorization now runs before overdue SLA escalation, so
   a denied warehouse request cannot mutate the Scenario.

The live deployed negative matrix was rerun after commit `0abc908` was pushed
to `origin/main`. Render does not expose a commit identifier through the public
health endpoints, so the deployment association is recorded by the push and
the subsequent live readiness/proof sequence rather than by an unauthenticated
revision endpoint.

## 1. Starting revision

- Starting repository revision: `376e9190a69bac7c24340954f88ce4099d3bfc06`.
- Reviewed working revision: local changes after that revision in
  `ScenarioController`, `ScenarioHistoryService`, and
  `PlatformTenantAccessBoundaryIntegrationTest`.
- Unrelated pre-existing worktree changes were not staged or modified:
  `frontend/Dockerfile`, `.gitattributes`,
  `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`, and
  `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`.

## 2. Complete production endpoint inventory

| Method | Path | Purpose | Auth | Role | Warehouse | Assignment/state | Kind |
| --- | --- | --- | --- | --- | --- | --- | --- |
| POST | `/api/scenarios/order-impact` | Single-plan what-if preview | Required | Workspace actor | Request warehouse | Valid request and inventory | Read-only projection with Scenario evidence |
| POST | `/api/scenarios/order-impact/compare` | Compare two what-if plans | Required | Workspace actor | Both request warehouses | Valid request and inventory | Read-only projection with comparison evidence |
| POST | `/api/scenarios/save` | Persist governed saved plan | Required | Workspace actor | Request warehouse | Requester session, eligible Review Owner, policy | Mutation of Scenario planning evidence |
| POST | `/api/scenarios/{id}/approve` | Review or final approval | Required | Exact required governance role | Scenario warehouse | Exact persisted owner, stage, note where required | Governance mutation |
| POST | `/api/scenarios/{id}/reject` | Reject a saved plan | Required | Assigned Review Owner or assigned Final Approver as stage requires | Scenario warehouse | Exact persisted owner and compatible stage | Governance mutation |
| POST | `/api/scenarios/{id}/acknowledge-escalation` | Acknowledge overdue final-approval SLA | Required | Exact assigned Escalation Owner | Scenario warehouse | Overdue escalated final-approval state | Governance mutation only |
| POST | `/api/scenarios/{id}/execute` | Retained compatibility route | Required for lookup | No execution authority | Existing Scenario scope | Object must be in tenant/scope | Always retired; in-scope result is HTTP 410 |
| GET | `/api/scenarios/{id}/request` | Read loadable Scenario request | Required | Workspace access | Scenario warehouse | Tenant and warehouse lookup | Read-only |
| GET | `/api/scenarios/history` | Filter Scenario history and queues | Required | Workspace access | Current operator scope filters results | Query filters do not grant access | Read-only |
| GET | `/api/scenarios/notifications` | Read Scenario governance notifications | Required | Workspace access | Current operator scope filters results | SLA evaluation is read-triggered as designed | Read-only response; may advance overdue SLA as designed |

No separate production candidate endpoint, Scenario execution service, Scenario
worker, or Scenario-specific impersonation endpoint was found. Reviewer,
Final Approver, and Escalation Owner candidate lists are provided by the access
directory APIs and are subject to the existing tenant-admin/workspace rules.

## 3. Production service and caller inventory

| Entry point | Current callers and responsibility |
| --- | --- |
| `ScenarioController` | The ten HTTP routes above; delegates all authority-sensitive work to access and Scenario services. |
| `ScenarioAnalysisService` | Runs preview or comparison projection, records Scenario-local evidence, and records a planning event. |
| `ScenarioProjectionService` | Reads tenant warehouse/product/inventory data and calculates projected inventory, risk, alerts, and recommendations in memory. |
| `ScenarioHistoryService` | Saves previews/plans, resolves identities and assignments, performs review/final/rejection/SLA/revision transitions, filters history, and denies legacy execution. |
| `AccessControlService` | Resolves authenticated actor, required role, tenant access, and request warehouse access. |
| `AccessDirectoryService` | Enforces active operator status, role membership, tenant membership, warehouse scopes, and exact operator assignment checks. |
| `OperationalViewService` | Supplies Scenario history and notifications to dashboard snapshots; downstream snapshot mapping applies current operator visibility. |
| `SystemIncidentService` | Reads Scenario notifications for runtime/incident presentation and applies current-operator visibility. |
| `RealtimeService` | Publishes the existing tenant raw Scenario notification topic; warehouse-scoped sessions cannot subscribe to raw topics and use filtered APIs. |
| `ScenarioRunRepository` | Persists Scenario-local records and supports tenant-scoped history queries. |
| Scheduled/background callers | No Scenario execution scheduler, automatic approval listener, distributed worker, or hidden Scenario mutation caller was found. SLA advancement occurs through the existing service read path. |

The historical `SCENARIO_EXECUTED` enum/event representation remains readable
for compatibility. Current production code does not create execution runs or
execute an order from a Scenario.

## 4. Role by action matrix

The backend is authoritative. The matrix below describes the intended current
contract; the focused integration suite exercises the meaningful denials and
positive paths rather than relying on hidden frontend navigation.

| Role | Preview/compare | Save | Review decision | Final approval | Reject | SLA acknowledge | Execute |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `TENANT_ADMIN` | Allowed within tenant/warehouse authority | Allowed within tenant/warehouse authority | Denied unless also the exact assigned governance role, and self-review remains denied | Denied unless exact assigned independent Final Approver, and self-approval remains denied | Denied unless exact assigned governance role | Denied | Retired HTTP 410 only when object is in scope |
| `REVIEW_OWNER` | Allowed within assigned warehouse | Allowed within assigned warehouse | Allowed only when exact persisted Review Owner and stage match | Denied as a Final Approver | Allowed only when exact persisted Review Owner and stage match | Denied | Retired HTTP 410 only when object is in scope |
| `FINAL_APPROVER` | Allowed within assigned warehouse | Allowed within assigned warehouse | Denied | Allowed only after required review and exact persisted Final Approver | Allowed only in the compatible final-approval stage and exact assignment | Denied | Retired HTTP 410 only when object is in scope |
| `ESCALATION_OWNER` | Allowed within assigned warehouse | Allowed within assigned warehouse | Denied | Denied | Denied as a governance decision owner | Allowed only for exact assigned overdue final-approval escalation | Retired HTTP 410 only when object is in scope |
| `INTEGRATION_ADMIN` | Allowed within assigned warehouse | Allowed within assigned warehouse | Denied | Denied | Denied | Denied | Retired HTTP 410 only when object is in scope |
| `INTEGRATION_OPERATOR` | Allowed within assigned warehouse | Allowed within assigned warehouse | Denied | Denied | Denied | Denied | Retired HTTP 410 only when object is in scope |
| Platform Owner | Separate platform control-plane identity only | No tenant Scenario authority | Denied | Denied | Denied | Denied | Denied |

`TENANT_ADMIN` with empty warehouse scopes is tenant-wide for tenant
administration and workspace-visible reads. That does not convert the identity
into a Review Owner, Final Approver, or Escalation Owner.

## 5. Workflow state by action matrix

| State/type | Review approve | Final approve | Reject | SLA acknowledge | Execute |
| --- | --- | --- | --- | --- | --- |
| `PREVIEW` | Denied | Denied | Denied | Denied | No operational execution; compatibility denial only if in-scope |
| Saved plan `PENDING_REVIEW` | Exact assigned Review Owner only | Denied | Assigned Review Owner only under the established rejection contract | Denied, including when review deadline is past under Model B | No operational execution |
| Saved plan `PENDING_FINAL_APPROVAL` | No repeated review bypass | Exact assigned independent Final Approver only | Compatible final-stage owner only | Only after overdue escalation and exact assigned Escalation Owner | No operational execution |
| Saved plan `APPROVED` | No contradictory transition | Idempotent/contractual retry only where supported | Denied | Denied | No operational execution |
| Saved plan `REJECTED` | Denied | Denied | Denied or idempotent according to current transition contract | Denied | No operational execution |
| Revision record | Starts as a new pending governance record | Follows its own assignment and stage | Follows its own assignment and stage | Follows its own SLA | No operational execution |
| `COMPARISON` | Denied | Denied | Denied | Denied | No operational execution and not loadable as an order request |

No incompatible action falls through to a successful transition.

## 6. Tenant and warehouse isolation

### Tenant boundary

Scenario lookup is by current tenant code and Scenario id. Tenant B cannot read,
approve, reject, revise, acknowledge, or use the compatibility route for a
Scenario belonging to Tenant A. The focused access-boundary and security suites
cover cross-tenant Scenario identifiers and mutations.

### Warehouse boundary

Scenario creation, preview, save, history, request read, governance actions,
notifications, and compatibility lookup apply warehouse authority. A North
operator cannot take a consequential action on a Coast Scenario. The direct
notifications endpoint was corrected in this phase so a North response cannot
contain Coast escalation notifications.

The second fix ensures authorization runs before SLA advancement. A denied
warehouse request therefore cannot trigger `slaEscalatedAt` or
`slaEscalatedTo` mutation on the hidden Scenario.

## 7. Assignment and identity attacks

The request DTOs contain human-readable fields for protocol compatibility, but
they do not establish authority by themselves.

- `requestedBy` is bound to the authenticated session actor; a mismatch is
  rejected.
- The review owner is resolved and validated as an active explicit
  warehouse-scoped `REVIEW_OWNER`, and cannot be the requester.
- Review approval and rejection require the exact persisted Review Owner.
- Final approval requires the exact persisted independent Final Approver after
  the required review stage.
- Escalation acknowledgement requires the exact persisted Escalation Owner and
  only acknowledges SLA attention; it never approves or rejects.
- Request body actor names and roles are checked against the authenticated
  session and current role membership.
- Platform Owner session identity is separate from tenant Scenario authority.
- A multi-role identity cannot use a second role to bypass self-review,
  reviewer/final separation, assignment, or workflow state.

Focused evidence covers requester spoofing, reviewer spoofing, final-approver
spoofing, escalation-owner spoofing, stale/inactive identities, same-role wrong
assignments, and self-governance attempts.

## 8. Anonymous, inactive, and session-switching results

- Anonymous protected Scenario endpoints are denied; production fallback is not
  used for authenticated deployment behavior.
- Inactive users cannot authenticate or perform governance actions through stale
  persisted assignments.
- Tenant and role sessions are resolved per request. No Scenario authority is
  taken from a previous browser user or from an Acting As field; no Acting As
  feature exists in the current Scenario path.
- Explicit test-profile compatibility headers remain test-only behavior and do
  not establish production identity.

## 9. SLA Model B result

The repository contract is Model B:

- An overdue `PENDING_REVIEW` Scenario remains Review Owner-owned and is not
  escalated to an Escalation Owner.
- An overdue `PENDING_FINAL_APPROVAL` Scenario may be rerouted to the exact
  eligible Escalation Owner for SLA attention.
- The Escalation Owner can acknowledge overdue attention only; acknowledgement
  preserves `PENDING_APPROVAL` and never performs a governance decision.
- Wrong owner, wrong role, wrong warehouse, wrong tenant, non-overdue, preview,
  approved, rejected, and anonymous acknowledgement attempts are denied.

The pre-existing Phase 7 tests plus the Phase 11 wrong-warehouse mutation test
cover this boundary.

## 10. Preview/live intelligence separation

`ScenarioProjectionService` creates a copied in-memory inventory view and calls
preview calculation methods. It may return projected low-stock warnings, risk,
and recommendations in the Scenario response. It does not persist live Alert
or Recommendation records and does not publish projected operational alerts.

The Scenario and operational contexts remain separate:

| Scenario projection | Live operational truth |
| --- | --- |
| Projected inventory and risk | Persisted inventory from an actual authorized write or source event |
| Projected alert | Live Alert record from actual operational evaluation |
| Scenario recommendation | Live Recommendation record from actual operational evaluation |
| Planning/history evidence | Operational Activity and Runtime state |

The existing MVP and access-boundary tests verify that preview, save, review,
approval, rejection, and revision do not mutate orders, inventory,
fulfillment, dispatch, live alerts, or live recommendations. A Scenario is not
matched to an actual order by SKU, quantity, warehouse, or timestamp.

## 11. Rejection, revision, duplicate, and contradiction results

- Rejection is terminal for the original record; it is not silently reset to
  pending.
- A valid revision creates a new Scenario record with a revision parent and
  leaves the rejected parent unchanged.
- A revision parent must be a same-tenant, in-scope rejected saved plan.
  Preview, comparison, approved, nonexistent, cross-tenant, and wrong-
  warehouse parents are rejected.
- Repeated incompatible approval, rejection, final-approval, and acknowledgement
  calls cannot produce two contradictory successful terminal decisions.
- SLA acknowledgement is idempotent according to the existing service contract
  and does not create an operational order or approval.
- No broad concurrency redesign was introduced; database/service behavior stays
  within the existing Scenario model.

## 12. Legacy execution and hidden-caller result

The retained `POST /api/scenarios/{id}/execute` route is compatibility behavior,
not an execution feature. An in-scope request receives HTTP 410 with the
external-handoff message after scope lookup. Out-of-scope and anonymous
requests are denied before that compatibility result.

Repository search found:

- no `ScenarioExecutionService` production caller;
- no current creator of `ScenarioRunType.EXECUTION`;
- no current producer of a new `SCENARIO_EXECUTED` event;
- no Scenario-to-`OrderService` execution path;
- no automatic approval listener or scheduled Scenario executor.

Historical enum values and runtime read representations remain only for
compatibility with old data.

## 13. History, notifications, filters, and enumeration

- `/history` is tenant-scoped and applies the current operator's warehouse
  scopes in its query specification.
- Query filters, owner filters, status filters, priority filters, overdue
  filters, and limits do not grant access outside the current scope.
- `/notifications` now filters the response by authenticated warehouse scopes
  after combining active and acknowledged notifications and before applying the
  response limit.
- Warehouse-scoped sessions are prevented from subscribing to raw tenant-wide
  Scenario topics. They receive filtered operational data through tenant APIs.
- Cross-tenant Scenario IDs return not-found behavior from tenant-scoped lookup.
- Direct comparison records intentionally have no single warehouse because a
  comparison may contain two warehouses; they are not executable or loadable as
  order requests and are not exposed in scoped history queries. This remains a
  low-severity object-existence/visibility consideration, not a consequential
  authority bypass.
- No Scenario count or pagination total exposes records outside the filtered
  history query.

## 14. Frontend and backend authority alignment

The frontend Scenario surface is not treated as the security boundary. The
current proof and source review verify that it does not present an `Acting As`
control or an Execute Scenario action. Backend checks remain authoritative for:

- role and assignment;
- tenant and warehouse scope;
- workflow stage;
- approval policy and risk-derived escalation;
- note requirements; and
- retired execution behavior.

The live rendered role walkthrough remains a deployment-dependent evidence step;
it must be rerun against the deployed revision after release.

## 15. Operational side effects and activity integrity

Across the Scenario lifecycle, governance writes are limited to Scenario
records and truthful Scenario/business-event/audit evidence. They do not create
orders, reserve inventory, create fulfillment or dispatch work, or publish
projected live Alert/Recommendation state.

Expected event families include:

- `SCENARIO_ANALYZED`;
- `SCENARIO_COMPARED`;
- `SCENARIO_SAVED`;
- `SCENARIO_ESCALATION_ADVANCED`;
- `SCENARIO_SLA_ESCALATED`;
- `SCENARIO_SLA_ACKNOWLEDGED`;
- `SCENARIO_APPROVED`;
- `SCENARIO_REJECTED`; and
- `SCENARIO_RESUBMITTED`.

No new `SCENARIO_EXECUTED` event is created by the current compatibility route.

## 16. 51-point closure return

| # | Required result | Phase 11 result |
| ---: | --- | --- |
| 1 | Starting HEAD | Reviewed from `376e919...`; unrelated worktree changes preserved. |
| 2 | Full endpoint inventory | Ten current HTTP entry points inventoried above. |
| 3 | Full service/caller inventory | Controller, analysis, projection, history, access, operational view, runtime, realtime, repository, and absence of hidden workers inventoried above. |
| 4 | Role x action matrix | All six tenant roles plus Platform Owner classified above. |
| 5 | State x action matrix | Preview, pending review, pending final approval, approved, rejected, revision, and comparison classified above. |
| 6 | Tenant isolation result | Covered by tenant-scoped repository lookup and focused access/security tests. |
| 7 | Warehouse isolation result | Covered for reads and mutations; direct notifications fixed and tested. |
| 8 | Same-role wrong-assignment result | Wrong Review Owner, Final Approver, and Escalation Owner denied by focused tests. |
| 9 | Multi-role separation result | Existing governance tests preserve self-review and review/final separation. |
| 10 | Anonymous matrix | Protected Scenario endpoints denied. |
| 11 | Inactive matrix | Inactive identity and stale assignment paths denied. |
| 12 | Identity spoof matrix | Session actor binding rejects caller-declared identity mismatches. |
| 13 | Requester self-governance matrix | Self-review and self-final-approval paths denied. |
| 14 | Standard approval bypass matrix | Exact assigned Review Owner required. |
| 15 | Final Approval bypass matrix | Exact independent Final Approver and stage required. |
| 16 | SLA bypass matrix | Model B enforced; only overdue assigned final-stage escalation can be acknowledged. |
| 17 | Rejection/revision bypass matrix | Terminal rejection and new-record revision behavior preserved. |
| 18 | Duplicate/idempotency matrix | Existing service contract and tests preserve repeated-call safety. |
| 19 | Contradictory-decision matrix | Incompatible terminal transitions are rejected. |
| 20 | Legacy execution attack matrix | Compatibility route has no operational execution and returns 410 in scope. |
| 21 | Hidden execution caller result | No current Scenario execution service/caller/event producer found. |
| 22 | Projected/live Alert separation | Projection uses preview methods and does not persist live alerts. |
| 23 | Projected/live Recommendation separation | Projection uses preview methods and does not persist live recommendations. |
| 24 | Operational side-effect result | Orders, inventory, fulfillment, dispatch, alerts, and recommendations remain unchanged by governance. |
| 25 | History-access result | Tenant and warehouse scope filters apply to history. |
| 26 | Candidate-endpoint result | Candidate lists are access-directory APIs with tenant/role/warehouse rules; no Scenario candidate bypass found. |
| 27 | Object-enumeration result | Tenant isolation is enforced; comparison records have a documented low-severity visibility consideration. |
| 28 | Query/count leakage result | Filtered history and scoped notifications do not expose other warehouse records or counts. |
| 29 | Frontend role-gating result | Execute and Acting As controls absent; backend remains authoritative. |
| 30 | Stale-session/frontend-state result | Session-derived actor and scope are resolved per request; no stale authority path found. |
| 31 | Request-body tampering result | Actor, requester, owner, stage, policy, and state are server-validated or server-owned. |
| 32 | Risk-policy tampering result | Save path recalculates risk and derives policy from tenant operational policy. |
| 33 | Revision-parent tampering result | Parent is loaded in tenant/scope and must be a rejected saved plan. |
| 34 | Platform Owner result | Platform identity remains outside tenant Scenario governance. |
| 35 | False-correlation result | No Scenario-to-actual correlation field or similarity-based completion claim exists. |
| 36 | Critical defects found | 0. |
| 37 | High defects found | 0 after two narrow fixes. |
| 38 | Medium/Low findings | Comparison object visibility is a documented low-severity consideration; it is not a consequential authorization bypass. |
| 39 | Production fixes | Notifications scope filtering; authorize warehouse before SLA mutation. |
| 40 | Test/fixture corrections | Initial new-test session fixture was corrected from bootstrap `boundary.admin` to `boundary.tenant.admin`; no production expectation was weakened. |
| 41 | Expectation-change justification | The only changed expectation was a fixture correction: helper payload/session identity had to match the established requester contract. |
| 42 | Focused matrix result | `PlatformTenantAccessBoundaryIntegrationTest`: 30/30 passed. |
| 43 | Full backend result | 170/170 tests passed with zero failures/errors. |
| 44 | Frontend checks | No frontend source changed; the existing frontend verification baseline remains applicable. |
| 45 | Repository gates | `git diff --check` passed; unrelated worktree changes remain unstaged. |
| 46 | Files changed | Scenario controller, Scenario history service, focused access-boundary test, and this evidence record. |
| 47 | Commits | `0abc908` (`Close Scenario Phase 11 negative boundaries`) pushed to `origin/main`; unrelated worktree changes were not staged. |
| 48 | Deployment/live readiness | Post-push live checks returned `FRONTEND_UP=True`, `BACKEND_UP=True`, `DB_READY=True`, `AUTH_READY=True`, and `WS_READY=True`. |
| 49 | Manual/live negative proof | Hosted Playwright proof passed 6/6 after readiness became true, including governance/replay/browser role-gating coverage. |
| 50 | Readiness for Phase 12 | Ready; this closure stops at Phase 11 as required. |
| 51 | Phase 11 verdict | Full hosted proof passed with zero Critical or High Scenario bypasses identified; only the documented low-severity comparison visibility consideration remains. |

## 17. Verification commands and next evidence gate

Completed:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd -Dtest=PlatformTenantAccessBoundaryIntegrationTest test
cmd /c mvnw.cmd test
```

Results:

- Focused access-boundary suite: 30 passed.
- Full backend suite: 170 passed.
- Flyway migrations, Hikari, JPA, and the in-memory realtime broker initialized
  successfully during the integration runs.
- Expected disabled-connector warnings and deliberate authorization-denial
  logs were observed; no unexpected test failure was present.
- `git diff --check`: passed.

The post-push live evidence gate completed with:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1

cd frontend
npm.cmd run test:e2e:prod
```

Live result:

- Connection check: all six readiness classifications were `True`, including
  `PROOF_ALLOWED=True`.
- Hosted proof: 6/6 tests passed in 7.6 minutes.
- The governance/replay test passed after exercising approval, execution
  blocking, replay, and browser role-gating behavior.
- The remaining page/integration and auth-rate-limit tests also passed.
- The runner emitted only the known Node `NO_COLOR`/`FORCE_COLOR` warnings and
  the expected slow-test advisory; no unexpected 4xx, 5xx, browser-console,
  resource, or React failure was reported.

The hosted run must inspect deliberate denials separately from unexpected 4xx,
5xx, browser-console, resource, or stale-state failures. It must not be run
against an unready backend, and it must not be used to justify weakening any
Scenario authority assertion.

## 18. Scope stop

Phase 11 stops here. Phase 12, full-system leakage work, CSV acceptance,
webhook/replay acceptance, and new Scenario features are not part of this
closure.
