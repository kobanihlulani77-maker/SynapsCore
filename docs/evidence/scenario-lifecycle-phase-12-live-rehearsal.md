# Scenario Lifecycle Phase 12: Live Owner Rehearsal

## Verdict

`PHASE 12 LIVE REHEARSAL DEFERRED - SCENARIO TECHNICAL LIFECYCLE VERIFIED - AWAITING LEGITIMATE OPERATIONAL DATA`

This is a controlled deferral, not a product failure. The Phase 12 owner
rehearsal must demonstrate a real operational context without converting a
synthetic proof fixture into customer-like truth.

## 1. Deployment precheck

- Repository `HEAD`: `6cd6080ef04f5037a3f90587f4db05af7c81f9f3`.
- `origin/main`: `6cd6080ef04f5037a3f90587f4db05af7c81f9f3`.
- Served frontend bundle: `/assets/index-BzAOgsyP.js`.
- Live readiness returned:
  `FRONTEND_UP=True`, `BACKEND_UP=True`, `DB_READY=True`,
  `AUTH_READY=True`, `WS_READY=True`, `PROOF_ALLOWED=True`.
- The live deployment is healthy enough for a future rehearsal. No Scenario
  rehearsal or hosted proof was run in this Phase 12 attempt.

## 2. Legitimate-data determination

The latest supplied live census for `OWNER-ACCEPT-02` reported:

| Operational input | Observed result |
| --- | ---: |
| Persisted warehouses | 2: `WH-NORTH`, `WH-COAST` |
| Products | 0 |
| Inventory rows | 0 |
| Orders | 0 |
| Connectors | 0 |
| Imports | 0 |
| Replay items | 0 |
| Scenario history | 0 |
| Scenario notifications | 0 |
| Recommendations | 0 |
| Alert envelopes | 1, with 0 active and 0 recent alerts |

There is therefore no legitimate product plus actual-inventory pair from which
to calculate a meaningful what-if Scenario. The hosted-proof tenant is not a
substitute: its records are synthetic verification fixtures and must not be
presented as Company 1 operational data.

The owner acceptance credentials are private and are not recorded here. A
fresh authenticated census should be performed when legitimate operational
data is onboarded; this document does not claim that the tenant changed after
the supplied census.

## 3. Why the rehearsal stops

Phase 12 explicitly prohibits creating arbitrary products, injecting inventory,
inventing orders, manufacturing external-system outcomes, or using proof data
to force a green lifecycle. Without real operational data, the following
claims cannot honestly be made:

- a real requester selected a real product and actual inventory;
- a projection was calculated against legitimate operational state;
- the before/after operational boundary was measured;
- the approved plan can be compared with authoritative source data.

The technical Scenario lifecycle remains covered by the Phase 0-11 repository
and hosted evidence. Phase 12 acceptance is deferred until the owner tenant
has legitimate operational inputs.

## 4. Exact future owner rehearsal script

### Identities and scope

Use the private credentials for the active persisted identities; do not create
new roles or use Acting As:

- Tenant: `OWNER-ACCEPT-02`.
- Primary warehouse: `WH-NORTH`.
- Requester: the assigned North `INTEGRATION_OPERATOR` identity, for example
  `accept.north.integrationoperator` if it remains active.
- Review Owner: the persisted North `REVIEW_OWNER`, for example
  `accept.north.review` if it remains active and assigned.
- Final Approver: the persisted North `FINAL_APPROVER`, for example
  `accept.north.final`, only if the real policy naturally escalates.
- Coast identities must not be used for the North Scenario.

Reconfirm each identity, role, active state, and warehouse scope immediately
before the rehearsal. Usernames are non-secret identifiers; passwords remain
private and must never enter evidence.

### Prerequisites

The owner tenant must contain all of the following from a legitimate source or
approved company onboarding process:

1. One active product with a known SKU.
2. One actual inventory row for that SKU in `WH-NORTH`.
3. A known safe proposal quantity that does not intentionally create a
   destructive or extreme operation.
4. A current baseline for inventory, relevant orders, live alerts,
   recommendations, dashboard state, and Scenario history.
5. Active assigned North requester and governance identities.

### Browser steps

1. Open the deployed frontend and sign in as the North Integration Operator.
2. Confirm the session identity is the signed-in requester and the scope is
   `WH-NORTH`; do not use an actor-switching control.
3. Open **Scenarios** and choose the legitimate product and North warehouse.
4. Enter the safe proposed quantity and run **PREVIEW**.
5. Capture the actual inventory, requested quantity, projected inventory,
   projected risk, projected warnings, and projected recommendations.
6. Immediately reread inventory, orders, live Alerts, live Recommendations,
   dashboard operational state, and Runtime. Every actual operational value
   must remain unchanged; only the Scenario projection may show hypothetical
   impact.
7. Confirm only the assigned North Review Owner is selectable. The requester,
   Coast Review Owner, Operations Lead, and unrelated roles must not be
   selectable.
8. Save the governed plan and verify requester, warehouse, projection, risk,
   assignment, and initial state.
9. Sign out. Do not switch accounts inside the same session.
10. Sign in as the exact assigned North Review Owner and open the plan from the
    legitimate review surface.
11. Verify requester, warehouse, proposal, projection, risk, assignment, and
    state. The Review Owner makes the review decision; no other identity does.
12. If the legitimate policy is **STANDARD**, approve once, verify `APPROVED`,
    reviewer identity, note behavior, and history, then stop governance.
13. If the legitimate policy is **ESCALATED**, approve as Review Owner,
    sign out, sign in as the assigned North Final Approver, verify the handoff,
    provide the required note, approve, and stop governance.
14. Do not wait for or manufacture SLA expiry in the primary rehearsal.
15. After approval, confirm there is no Execute control and no order,
    inventory, fulfillment, dispatch, live alert, or live recommendation
    mutation caused by the Scenario.
16. Inspect Scenario history for analysis, save, requester, warehouse,
    assignments, decisions, notes, timestamps, and final governance state.
17. Do not claim Scenario execution or Scenario-to-order correlation. Any
    later authoritative source event is a separate operational fact.

### Expected outcome

The truthful end state is:

`real operational context -> hypothetical projection -> governed decision -> no internal execution`

SynapseCore stops at the approved decision. The customer’s authoritative
operational system performs any later business action.

## 5. Evidence to capture when data exists

Capture screenshots or sanitized records for the Scenario page, requester
identity, warehouse scope, selected Review Owner, preview projection, saved
plan, review decision, final approval if applicable, history, and before/after
operational counts. Do not capture passwords, tokens, cookies, raw credentials,
or sensitive customer payloads.

Also record browser console/network results. Expected deliberate authorization
denials are not incidents; unexpected 4xx/5xx responses, failed resources,
React errors, stale identity, or operational mutations stop the rehearsal.

## 6. Required Phase 12 return fields

| # | Field | Current result |
| ---: | --- | --- |
| 1 | Starting HEAD | `6cd6080ef04f5037a3f90587f4db05af7c81f9f3` |
| 2 | Deployed version/bundle | Healthy deployment; `/assets/index-BzAOgsyP.js` |
| 3 | Live readiness | All required classifications `True` |
| 4 | Legitimate operational data available | `NO - latest supplied owner census is empty` |
| 5 | Tenant | `OWNER-ACCEPT-02` |
| 6 | Warehouse | Not selected; awaiting legitimate data |
| 7 | Requester | Not used in a Phase 12 rehearsal |
| 8 | Review Owner | Not used in a Phase 12 rehearsal |
| 9 | Final Approver | Not used in a Phase 12 rehearsal |
| 10 | Product/SKU | None available in the owner tenant census |
| 11 | Starting operational state | No products, inventory, or orders; no active/recent alerts |
| 12 | Scenario request | Deferred; no legitimate proposal exists |
| 13 | Projection result | Not run |
| 14 | Risk | Not run |
| 15 | Projected alerts | Not run |
| 16 | Projected actions/recommendations | Not run |
| 17 | Live Alerts before/after | Not applicable; latest census had 0 active and 0 recent |
| 18 | Live Recommendations before/after | Not run; latest census had 0 |
| 19 | Inventory before/after | Not run; latest census had 0 rows |
| 20 | Orders before/after | Not run; latest census had 0 |
| 21 | Fulfillment/dispatch before/after | Not run |
| 22 | Save result | Not run |
| 23 | Review Owner handoff | Not run |
| 24 | Review decision | Not run |
| 25 | Final Approval | Not applicable |
| 26 | Final Scenario state | No Phase 12 Scenario created |
| 27 | Execution boundary | Preserved by Phase 11; not exercised in Phase 12 |
| 28 | Scenario history | No Phase 12 history entry |
| 29 | Frontend result | Live shell reachable; no Scenario rehearsal rendered |
| 30 | Console/network result | Readiness checks clean; no rehearsal browser capture |
| 31 | Defects found | None from the deferred rehearsal |
| 32 | Fixes | None |
| 33 | Verification after fixes | No fixes; Phase 11 hosted proof remained green |
| 34 | Manual screenshots/evidence | Not captured; no legitimate rehearsal occurred |
| 35 | Critical blockers | `0` |
| 36 | High blockers | `0` |
| 37 | Medium/Low findings | Legitimate operational data prerequisite remains open |
| 38 | Final Scenario readiness | Technical lifecycle ready; owner rehearsal pending data |
| 39 | Phase 12 verdict | Deferred under the no-manufactured-data rule |

## 7. Resume condition

Resume Phase 12 only after legitimate operational data is onboarded into
`OWNER-ACCEPT-02` and a fresh authenticated census confirms the product,
inventory, warehouse, and identity prerequisites. Then rerun the exact owner
script above from a clean baseline. Do not start CSV, webhook/replay, broad
isolation, or another feature phase as part of this deferral.
