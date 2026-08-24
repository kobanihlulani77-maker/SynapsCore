# Role Authority Hardening Gate

This record documents the pre-Phase-12 least-privilege hardening gate for tenant roles, integration ingress, scenario governance, and replay-adjacent authority.

It is an engineering evidence record, not a marketing or roadmap document.

## Scope

This gate closes four High findings from the platform/tenant authority census:

| Finding | Risk | Final state |
| --- | --- | --- |
| All six roles could mutate inventory, orders, and fulfillment | Excess authority across governance, admin, and integration responsibilities | Fixed with role-specific write gates |
| All six roles could perform session-auth webhook/CSV ingestion | Non-integration roles could enter data into live operational flow | Fixed with integration-role ingestion gate |
| Standard review approval/rejection did not enforce assigned review owner | Any review owner could act on another owner's assignment | Fixed with assigned review-owner enforcement |
| Governance roles could execute `PREVIEW` runs directly | Planning previews could bypass saved-plan approval semantics | Fixed; only approved `SAVED_PLAN` records with stored request payloads are executable |

## Final Authority Matrix

| Operation family | Required role | Additional enforcement |
| --- | --- | --- |
| Product/catalog create, update, import | `TENANT_ADMIN` | Tenant scope |
| Inventory update, receive, adjust, reconcile | `TENANT_ADMIN` | Tenant and warehouse scope |
| Direct order create and transition | `INTEGRATION_ADMIN` or `INTEGRATION_OPERATOR` | Tenant and warehouse scope |
| Fulfillment update | `INTEGRATION_ADMIN` or `INTEGRATION_OPERATOR` | Tenant and warehouse scope |
| Human-session webhook ingestion | `INTEGRATION_ADMIN` or `INTEGRATION_OPERATOR` | Tenant and connector policy |
| Human-session CSV ingestion | `INTEGRATION_ADMIN` or `INTEGRATION_OPERATOR` | Tenant and connector policy |
| Connector create/update | `INTEGRATION_ADMIN` | Tenant scope and connector policy |
| Manual replay | `INTEGRATION_ADMIN` or `INTEGRATION_OPERATOR` | Tenant, connector, warehouse, duplicate, and eligibility checks |
| Review-stage approval/rejection | Assigned `REVIEW_OWNER` | Tenant, warehouse, stage, and actor assignment |
| Final approval/rejection | Assigned `FINAL_APPROVER` | Tenant, warehouse, stage, and actor assignment |
| Escalation acknowledgement | Assigned `ESCALATION_OWNER` | Tenant, warehouse, escalation state, and actor assignment |
| Scenario execution | `REVIEW_OWNER` or `FINAL_APPROVER` | Approved `SAVED_PLAN` only; stored request payload required |

`PREVIEW` scenario runs remain loadable planning evidence. They are not executable live-order commands.

## Code-Level Changes

- `AccessControlService` now exposes dedicated gates for inventory writes, operational writes, and human-session ingestion.
- Inventory controllers use the inventory write gate.
- Order and fulfillment mutation controllers use the operational write gate.
- Webhook and CSV ingestion preserve connector-token ingestion while requiring integration roles for human-session ingestion.
- Scenario governance now enforces assigned review owner for standard review approve/reject paths.
- Escalation acknowledgement enforces the assigned escalation owner when present.
- Scenario execution rejects every non-approved saved plan, including previews.
- Hosted production proof now creates direct proof orders with an integration-authorized API context.

## Test Evidence

| Verification | Result |
| --- | --- |
| Focused access boundary plus websocket regression | PASS, `19/19` |
| MVP flow and inventory concurrency affected subset | PASS, `78/78` |
| Full backend suite | PASS, `152/152` |

The full backend suite is the current local baseline after this gate.

## Documentation Updates

The following docs were updated to align operator guidance with the hardened authority model:

- `docs/platform-control-plane-access-boundary.md`
- `docs/templates/platform-tenant-access-boundary-record.md`
- `docs/company-day-one-pilot-guide.md`
- `docs/company-daily-operator-sop.md`

## Medium Findings

| Finding | Gate decision |
| --- | --- |
| Escalation acknowledgement assignment was not enforced | Fixed in this gate |
| Tenant-wide activity/dashboard/read surfaces for scoped operators | Documented as a remaining pilot limitation unless a customer workflow proves it must be tightened before expansion |
| Tenant-admin connector support metadata overlap | Accepted for Company Settings support ownership metadata; full connector/import/replay APIs remain integration-role gated |
| Runtime connector diagnostics visibility | Accepted as tenant runtime trust visibility; raw payloads and connector secrets remain excluded |
| Import-run warehouse attribution | Documented limitation; import/replay actions remain integration-role gated |
| Disabled-webhook replay readback inconsistency | Still documented separately; CSV failed-inbound recovery remains the proven Company 1 recovery lane |

## Live Proof Status

Local proof is complete. The deployed authority implementation was exercised on Render after the hardening commit.

Minimum live checks:

1. Run `scripts\check-live-connections.ps1`.
2. Confirm frontend, backend, DB, auth, and websocket are ready.
3. Exercise the six-role rendered/API authority matrix against the deployed build. **Complete.**
4. Confirm wrong-role inventory/order/fulfillment/ingestion/governance/preview-execution attempts are denied by backend APIs. **Complete through the deployed role matrix, existing live governance evidence, and full hosted proof.**
5. Confirm Critical blockers: `0` and High blockers: `0`. **Complete.**

The six-role rehearsal used generated synthetic identities and disabled those identities during cleanup. No passwords, tokens, session cookies, or raw payloads were recorded.

### Current deployed evidence

| Evidence | Result |
| --- | --- |
| Live connection classification | `FRONTEND_UP=True`, `BACKEND_UP=True`, `DB_READY=True`, `AUTH_READY=True`, `WS_READY=True`, `PROOF_ALLOWED=True` |
| Deployed authority commit | `3fc927d8f1f3905013f057fc9ecc7a8f6596d2c6` |
| Repository proof correction commit | `f5d2f0a3d6fc2a4b9c72616068c57cb2e18fb4f9` |
| Six distinct role API/UI rehearsal | PASS, zero matrix failures |
| Full hosted production proof | PASS, six tests |
| Focused replay/governance proof | PASS |
| Live governance evidence | Existing deployed assignment and executable-state evidence retained in `docs/platform-control-plane-access-boundary.md`; fresh supplemental fixture bootstrap returned `401` for the proof-admin account and created no fixture |

### Six-role deployed matrix

The rehearsal used one identity per role: `TENANT_ADMIN`, `INTEGRATION_ADMIN`, `INTEGRATION_OPERATOR`, `REVIEW_OWNER`, `FINAL_APPROVER`, and `ESCALATION_OWNER`.

| Area | Result |
| --- | --- |
| Dedicated session identity | Each session returned exactly its assigned single role |
| Navigation and direct routes | Allowed role pages rendered; forbidden role pages were not advertised and direct platform/tenant-admin routes did not grant access |
| Platform control plane | All six tenant identities were denied `/api/platform/*` |
| Tenant administration | `TENANT_ADMIN` allowed; the other five roles denied users/workspace administration |
| Integrations and replay | `INTEGRATION_ADMIN` and `INTEGRATION_OPERATOR` allowed reads; other roles denied |
| Inventory writes | `TENANT_ADMIN` allowed; other roles denied |
| Order and fulfillment writes | Integration roles allowed in `WH-NORTH`; other roles denied |
| Session webhook and CSV ingestion | Integration roles allowed with temporary connector policy; other roles denied |
| Warehouse boundary | Scoped integration roles were allowed in `WH-NORTH` and denied in `WH-COAST`; tenant-wide admin semantics remained intentional |
| Tenant runtime and scenario reads | Allowed tenant-scoped reads; platform runtime remained separate and denied |
| Sign-out | Each rendered role completed sign-out back to the sign-in shell |

### High finding closure

| Finding | Live closure evidence | Result |
| --- | --- | --- |
| High 1: unrestricted inventory/order/fulfillment writes | Six-role live matrix exercised allowed and denied inventory, order, fulfillment, and wrong-warehouse paths | PASS |
| High 2: unrestricted session webhook/CSV ingestion | Six-role live matrix exercised enabled connector-backed webhook and CSV paths | PASS |
| High 3: unassigned review owner could approve | Deployed assignment evidence is recorded in `docs/platform-control-plane-access-boundary.md`; local assignment regression remains `152/152` | PASS with proof-account refresh follow-up |
| High 4: preview could execute | Full hosted proof and existing deployed live governance evidence cover preview/state gating; local regression remains `152/152` | PASS with proof-account refresh follow-up |

The supplemental governance runner received `401` while authenticating the existing proof-admin account after the six-role cleanup. It created no fixture and did not change the product verdict. Refreshing the ignored proof state is an operational evidence task before the next fresh governance rehearsal, not a role-authority bypass.

## Gate Verdict

Local verdict: **PASS**.

Live verdict: **ROLE AUTHORITY HARDENING GATE ACCEPTED WITH DOCUMENTED MEDIUM/LOW LIMITATIONS**.

Critical blockers: **0**.

High blockers: **0**.

Medium/Low limitations remain documented above and in the platform access-boundary record. Phase 12: **STOPPED**.
