# Runtime, Activity, and Governance Scope: Final Correction

Status: Source correction implemented. Post-deployment live evidence is required before this gate can be accepted.

This evidence record closes the final scope review for the fresh manual owner-acceptance tenant. It does not start CSV testing, fulfillment, tariff, full scenario acceptance, Wave 3, or Phase 14.

## Scope Under Review

Tenant context: `OWNER-ACCEPT-02`.

The review identified three related trust-boundary problems:

1. A failed platform-owner login could inherit an existing tenant session and be recorded as tenant activity or a tenant Runtime incident.
2. Tenant Activity had no warehouse attribution and could expose static-resource noise or broad authorization evidence to warehouse-scoped operators.
3. Scenario surfaces presented role selection as if an operator could act as another role, even though the backend correctly authorizes the authenticated operator and persisted assignment.

The correction keeps backend authority authoritative and narrows presentation and telemetry projections to honest scope.

## Root Causes

### Platform login context

`RequestTraceFilter` previously recognized only the tenant login endpoint as an authentication boundary. A platform login attempt made while a tenant session existed could therefore inherit the tenant actor and tenant code in request tracing. The exception audit path could then persist a platform 401 under the tenant.

### Activity and Runtime projection

Audit records have tenant, actor, source, target, status, detail, request, and time fields, but no warehouse field. A broad tenant activity projection cannot safely turn an unattributed 403 or static-resource request into warehouse activity. Runtime also consumes audit failures, so expected denials could be promoted into an operational incident unless classified separately.

### Scenario identity presentation

The UI exposed an `Acting As` role selector. That control was misleading: it suggested that a browser user could choose approval authority. The backend still checked the authenticated session, active operator, declared role, warehouse assignment, persisted Review Owner/Final Approver assignment, workflow state, and PREVIEW boundary.

## Final Scope Rules

| Surface or action | Final rule |
| --- | --- |
| Platform login | Platform login requests are anonymous/missing-tenant for request tracing. Platform authentication audit uses platform scope. |
| Tenant Activity | Current tenant only. Platform sources, platform-session records, favicon/static-resource records, and scoped-operator 403 request denials are excluded from tenant activity. |
| Platform Activity | Separate control-plane view. It may retain platform authorization evidence and metadata-first tenant health/support evidence. |
| Tenant Runtime | Current tenant operational evidence only. Expected 403 and favicon/static noise do not create operational incidents. |
| Warehouse-scoped Runtime | Only attributable incidents for assigned warehouses are shown. Unattributed audit/dispatch evidence is not assigned to a warehouse and is hidden from the scoped incident lane. |
| Tenant-wide Runtime | Tenant-wide operators, including `TENANT_ADMIN`, retain current-tenant operational visibility. |
| Requested By | The authenticated session actor is the requester for a saved governed plan. The planner displays this as read-only. |
| Signed In As | Display-only identity and current session roles. No role impersonation control exists. |
| Review Owner options | Only active operators with `REVIEW_OWNER` and eligibility for the selected warehouse are offered. The backend remains authoritative. |
| Scenario approval/rejection | The UI sends the workflow-required role metadata; the backend validates the current session, role, assignment, warehouse, note, and workflow state. |
| Escalation acknowledgement | Uses the governed `ESCALATION_OWNER` action. It is separate from review, final approval, and execution. |
| PREVIEW | Analysis only and never executable. Execution requires a saved, approved governed state. |

## Realtime Consistency

Tenant snapshots and realtime `/audit.recent` updates use the same tenant activity projection. The frontend also applies the existing warehouse filter to realtime incident updates. A tenant-wide publish therefore cannot make a warehouse-scoped operator see an unattributed or unrelated incident in the operational lane.

The system still distinguishes tenant metrics from warehouse metrics. Tenant-level dispatch, publish, lock, and runtime counters must not be read as site-specific counts unless the response includes an honest warehouse attribution.

## Governance Identity Contract

The scenario workflow is:

```text
authenticated operator
        |
        +--> requested-by identity comes from the session
        |
        +--> active eligible Review Owner is selected for the warehouse
        |
        +--> backend validates persisted assignment and role
        |
        +--> owner review or rejection
        |
        +--> assigned Final Approver when policy requires it
        |
        +--> governed execution only after the legal state is reached
```

The browser must never be treated as the authority source. A hidden or disabled control is not security evidence; direct URL and API denial must still be checked during live rehearsal.

## Implemented Source Changes

- Platform and tenant login paths are both treated as authentication boundaries by request tracing.
- Tenant Activity filters platform sources, platform-session records, favicon/static-resource records, and scoped-operator 403 request denials.
- Runtime excludes platform/static expected-denial noise from operational incident projection.
- Incident responses carry additive warehouse attribution where the source has it.
- Inbound, replay, connector, scenario, audit, and dispatch incident visibility remains tenant-scoped and is warehouse-filtered where attribution exists.
- Realtime frontend incident projection preserves warehouse scope.
- Scenario UI replaces `Acting As` with read-only `Signed In As` identity.
- Scenario `Requested By` is displayed as the authenticated session actor.
- Review Owner options exclude inactive operators and require the role plus selected warehouse eligibility.
- Approval and rejection actions send the workflow-required role metadata; escalation acknowledgement uses the fixed governed role.

No route, backend authority rule, tenant contract, authentication behavior, PREVIEW rule, or proof selector was intentionally weakened.

## Local Evidence

Local checks must include:

- backend full test suite with zero failures;
- frontend lint, build, and verify;
- `git diff --check`;
- documentation link check and secret scan where available.

Local evidence proves compilation, tests, and source behavior only. It does not close the deployed gate.

## Required Post-Deployment Evidence

On the deployed revision, a fresh synthetic rehearsal must prove:

- a failed platform login does not appear in tenant Runtime or scoped tenant Activity;
- Platform Activity retains correctly scoped platform security evidence;
- `TENANT_ADMIN` sees tenant-wide operational evidence;
- North and Coast operators see only their attributable warehouse evidence;
- assigned Review Owner is allowed;
- different or unassigned Review Owner is denied;
- wrong warehouse is denied;
- assigned Final Approver behavior is correct;
- PREVIEW execution is rejected;
- valid governed progression executes successfully;
- tenant Activity and Platform Activity remain separate;
- tenant Runtime and Platform Runtime remain separate;
- direct forbidden routes/APIs remain denied;
- no stale or disabled operator can act.

The previously reported hosted proof result remains evidence for its own prepared tenant and revision. It is not a substitute for this fresh post-correction scope rehearsal unless its deployed revision and fixtures are confirmed to match this correction.

## Limitations

- Audit records do not currently have warehouse attribution. Conservative filtering hides unattributed 403/audit evidence from scoped operational views rather than guessing ownership.
- Tenant-level metrics remain tenant-level metrics; they are not automatically site-level metrics.
- Platform Activity is metadata-first and must not expose raw customer orders, products, inventory, inbound payloads, replay payloads, connector secrets, or credentials.
- The current source correction does not create a new audit schema, event bus, or background processing architecture.
- A live rendered walkthrough is still required after deployment. Until that occurs, this correction is not accepted as live-proven.

## Gate Status

```text
Source correction: IMPLEMENTED
Local regression: PENDING FINAL RERUN
Post-deploy rendered scope evidence: PENDING
Critical blockers: NOT ASSESSED FOR THIS CORRECTION
High blockers: NOT ASSESSED FOR THIS CORRECTION
CSV / Phase 14: NOT STARTED
```
