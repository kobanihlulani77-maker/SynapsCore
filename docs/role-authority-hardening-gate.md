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

Local proof is complete. Live Render proof must run after this commit is deployed before the gate is accepted as live-closed.

Minimum live checks:

1. Run `scripts\check-live-connections.ps1`.
2. Confirm frontend, backend, DB, auth, and websocket are ready.
3. Exercise the six-role rendered/API authority matrix against the deployed build.
4. Confirm wrong-role inventory/order/fulfillment/ingestion/governance/preview-execution attempts are denied by backend APIs.
5. Confirm Critical blockers: `0` and High blockers: `0`.

Do not start Phase 12 until live proof is recorded.

## Gate Verdict

Local verdict: **PASS**.

Live verdict: **PENDING DEPLOYED VERIFICATION**.

Phase 12: **STOPPED** until live verification closes with Critical blockers `0` and High blockers `0`.
