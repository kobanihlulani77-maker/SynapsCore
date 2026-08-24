# Operational Page Guidance Wave 2 Evidence

Status: implementation evidence; rendered deployment closure follows after redeploy.

## Scope

Wave 2 covers the core tenant operational loop:

- `/dashboard`
- `/integrations`
- `/replay-queue`
- `/scenarios`
- `/approvals`
- `/orders`
- `/inventory`
- `/runtime`

Wave 3, Wave 4, and Phase 14 are not part of this record.

## Starting Point

- Starting commit: `1006c1bfe9f3f6265f48214acd8e96f7caf1a76c`
- Wave 1 was accepted before this implementation wave.
- Unrelated local changes intentionally excluded: `frontend/Dockerfile`, `.gitattributes`

## Implementation Summary

The existing operational pages were already materially deeper than CRUD screens. Wave 2 adds only evidence-backed gaps:

- Inventory now reflects the backend authority model: a warehouse-scoped `TENANT_ADMIN` may use the existing `/api/inventory/adjust` endpoint. Other roles receive evidence/read-only guidance rather than a misleading source-only statement.
- Inventory maintenance requires a non-zero whole-unit delta and reason, displays warehouse and before-value context, refreshes the snapshot after success, and reminds the operator that source reconciliation remains separate.
- Scenario Preview is explicitly marked `PREVIEW IS NOT EXECUTABLE` in the planner and decision console. Execution requires a saved, approved governed state.
- Approvals now distinguishes `Assigned to you`, `Not assigned to you`, and `Assignment not reported`, and shows warehouse-scope availability alongside the selected decision.
- Integrations surfaces the CSV recovery lane and disabled-webhook replay/readback limitation only when the selected connector state makes that limitation relevant.
- Replay now states the controlled recovery sequence, the duplicate/reconciliation check, and the requirement to verify the resulting effect rather than treating HTTP success as recovery completion.
- Orders now separates SynapseCore observation from source-authoritative or reconciled state and states that direct mutations remain role/backend controlled.

No backend endpoint, backend contract, route, proof selector, or visual theme was changed.

## Page Depth Classification

| Page | Before | After | Evidence-backed change |
| --- | --- | --- | --- |
| Dashboard | Strong | Strong | Existing attention cards, operating lanes, runtime trust, and next-action routing retained as the reference page. |
| Integrations | Strong | Strong | Contextual connector ownership, recovery boundary, and CSV/webhook limitation guidance. |
| Replay Queue | Strong | Strong | Explicit inspect/correct/duplicate-check/replay/verify sequence and unknown warehouse attribution wording. |
| Scenarios | Adequate | Strong | Preview is visibly analysis-only; saved-plan governance and execution boundary are explicit. |
| Approvals | Adequate | Strong | Assignment, warehouse scope, stage consequence, and next governance action are visible. |
| Orders | Adequate | Strong | Operating picture now states role limits and source/reconciliation trust boundaries. |
| Inventory | Misleading | Strong | Removed the false source-only authority statement and exposed the existing scoped tenant-admin maintenance lane. |
| Runtime | Strong | Strong | Existing readiness, realtime, queue, incident, and fallback interpretation retained. |

## Authority And Trust Model

- Role remains the action authority; warehouse scope remains the location boundary; backend enforcement remains authoritative.
- Inventory writes use the existing backend `TENANT_ADMIN` requirement and warehouse access check. The UI does not broaden that authority.
- Replay remains limited to the existing integration roles and warehouse scope. The UI does not infer eligibility from incomplete evidence.
- Scenario Preview cannot render an execution action even if an inconsistent payload reports `executable`; the frontend now requires a non-`PREVIEW` type as an additional guard, while backend enforcement remains authoritative.
- Approval assignment is displayed as reported by the scenario response. Missing assignment remains `Assignment not reported`; it is not converted into ownership.
- Orders and inventory describe SynapseCore observations separately from source-authoritative or reconciled facts.

## State Handling

- Loading: existing page/shell loading states remain distinct from empty content.
- Empty: existing page-specific empty states remain operationally meaningful, for example `No failed inbound items are waiting` and `No recent orders are visible yet`.
- Error: existing action error states remain visible; inventory adjustment errors do not fall back to a green or zero state.
- Degraded/attention: connector, replay, runtime, approval, order, and inventory pressure use existing reported state; no severity or root cause is invented.
- Success: inventory adjustment reports acceptance and triggers snapshot readback; replay/scenario actions retain their existing success/error state handling.

## Validation Record

Completed locally after implementation:

- `cd frontend; npm.cmd run lint`: PASS
- `cd frontend; npm.cmd run build`: PASS
- `cd frontend; npm.cmd run verify`: PASS
- `cd frontend; npm.cmd run test:controls:inventory -- --outputDir "$env:TEMP\\synapsecore-wave2-control-inventory"`: PASS; 222 controls inventoried, no repository artifact created
- `git diff --check`: PASS

Focused source review confirmed the inventory authority in `InventoryController` and `AccessControlService`, including `requireInventoryWrite` and warehouse-scope enforcement. No backend changes were required, so the complete backend suite is not repeated for this frontend-only implementation wave unless repository policy requires it.

## Rendered / Hosted Closure

Pending after the focused source commit is deployed:

- Rendered 1366x768 walkthrough of all eight routes.
- Loading, normal, empty, attention/degraded, and error states where naturally available.
- Synthetic proof action walkthroughs: integration attention, replay decision, scenario governance, Preview blocking, and inventory authority.
- Role/direct-route/backend authority checks using existing proof identities without recording credentials.
- Deployed revision confirmation.

The Wave 2 verdict remains open until those rendered checks are completed. No Wave 3 or Phase 14 work may start before closure.

## Known Limitations

- Disabled-webhook replay/readback remains not fully proven and is surfaced contextually rather than presented as a complete recovery lane.
- Import warehouse attribution remains unknown where the backend cannot establish it; Replay does not claim certainty.
- Source-system reconciliation remains a pilot responsibility unless the backend explicitly reports reconciliation evidence.
- Natural empty/error state coverage depends on available synthetic data and safe failure injection; no live data is deleted or manually altered to manufacture a state.
