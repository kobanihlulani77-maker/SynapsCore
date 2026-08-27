# Runtime, Activity, and Governance Scope Correction

Status: Source correction implemented and locally verified. Post-deployment live walkthrough remains required before CSV operational acceptance.

## Scope

This record addresses the fresh acceptance tenant observation in which a warehouse-scoped operator saw unrelated authorization failures and a static-resource failure as high-severity Runtime incidents. It does not start CSV testing, full operational acceptance, Wave 3, or Phase 14.

Acceptance tenant used for the reported observation: `OWNER-ACCEPT-02`.

## Starting Point

- Starting repository revision: `7cc82750f53962a2aa1fa8bb2553865133b9e856`
- Existing unrelated local changes intentionally preserved and excluded: `frontend/Dockerfile`, `.gitattributes`
- Existing backend baseline before this correction: 152 tests passed.

## Root Cause

The tenant Runtime incident list was assembled from several tenant-scoped sources, but the response did not carry warehouse attribution. Audit failures and failed dispatch work were also treated as unattributed tenant incidents. Every audit failure was mapped to `HIGH`, so an expected authorization response such as a Review Owner receiving a 403 could affect the Runtime trust count. Unexpected static-resource failures were sent through the generic high-severity operational alert hook, so a favicon failure could also pollute operational trust.

The normal tenant repositories already applied tenant context. The missing boundary was the warehouse-aware incident projection and the distinction between expected request denial and an operational failure.

## Attribution Model

### Before

| Evidence | Tenant boundary | Warehouse boundary | Runtime consequence |
| --- | --- | --- | --- |
| Audit failure | Current tenant audit query | None | All failures could become `HIGH` incidents |
| Inbound record | Current tenant repository query | Not exposed in response | Warehouse-specific records were indistinguishable to the client |
| Replay record | Current tenant replay service | Not exposed in response | Warehouse-specific replay pressure could not be filtered safely |
| Connector | Current tenant connector service | Not exposed in response | Non-live connector state could be shown outside its warehouse |
| Dispatch failure | Current tenant repository query | Not attributed | Shared/unattributed failures could affect scoped operators |
| Scenario notification | Current tenant scenario service | Present in notification data | Warehouse filtering was not applied by the incident projection |

### After

- `SystemIncidentResponse` carries additive `warehouseCode` metadata where the source knows it.
- Current-tenant filtering remains enforced by the existing services and repositories.
- Tenant-wide operators, including `TENANT_ADMIN`, retain the whole current-tenant incident view.
- Warehouse-scoped operators receive only incidents attributed to one of their allowed warehouses.
- Unattributed audit and dispatch evidence is not shown as a warehouse-specific incident to a scoped operator.
- A missing request operator is treated as a background/platform publication context, not as a reason to invent a warehouse scope.
- The frontend applies the same safe warehouse projection to realtime incident updates so a tenant-wide broadcast cannot bypass the scoped view.

## Classification Rules

The correction does not classify all HTTP statuses by number alone.

- `REQUEST_REJECTED` with a 403 detail is retained as audit evidence but is excluded from operational Runtime incidents. It does not create a tenant `HIGH` incident or trigger a Runtime stop for another operator.
- Favicon requests are excluded from operational incident projection. Unexpected favicon failures are logged as static-resource warnings and no longer emit the generic `API_UNEXPECTED_FAILURE` operational alert.
- Other non-request audit failures continue to be surfaced using the existing incident severity until a domain-specific classification is supported.
- Genuine inbound, replay, connector, scenario, and dispatch failures retain their existing domain severity and action-required behavior, subject to tenant and warehouse visibility.

This keeps authorization evidence visible to audit/security review without equating a correctly denied request with operational data loss or processing risk.

## Runtime Scope

### Platform Runtime

Platform Runtime remains the location for global infrastructure posture, cross-tenant technical health, deployment/release trust, and unattributed platform evidence. It is not changed into unrestricted raw tenant business-data access.

### Tenant Runtime

Tenant Runtime answers whether the current tenant's supported operations are trustworthy. It receives current-tenant connector, ingestion, replay, dispatch, scenario, and incident evidence. It does not intentionally expose another tenant's business activity.

### Warehouse-Scoped Tenant View

- `WH-NORTH` operators see attributable `WH-NORTH` evidence.
- `WH-COAST` operators see attributable `WH-COAST` evidence.
- Tenant-wide operators see both warehouse lanes and tenant-wide evidence.
- Evidence with no honest warehouse attribution is not assigned to a warehouse. For a scoped operator it is hidden from the scoped operational incident lane rather than shown as if it belonged to that warehouse.

## Activity Scope

Tenant Activity continues to use the current tenant context. Warehouse-specific activity is filtered where the underlying response carries a warehouse attribution; tenant-wide administration, security, and configuration activity remains tenant-wide rather than receiving a fabricated warehouse code. Platform Activity remains a separate metadata-first control-plane view.

The correction does not broaden Platform Activity or tenant access to raw customer orders, products, inventory, inbound payloads, replay payloads, connector secrets, or customer credentials.

## Dashboard Trust Rule

The Dashboard consumes the incident projection used by the authenticated tenant context. Because the projection is now scope-aware and excludes expected 403/static-resource noise, a `BLOCKED` or `STOP` posture is no longer caused solely by an unrelated authorization denial or favicon failure. Genuine relevant high-severity incidents continue to affect the existing trust posture.

No Dashboard copy or route was changed to hide a real failure.

## Governance Rules Confirmed by Source Review

- Scenario creators may plan and save through the existing policy; creation does not itself grant downstream approval authority.
- Review approval requires the active Review Owner role, warehouse eligibility, and the persisted assigned Review Owner where assignment is present.
- Final approval requires the active Final Approver role, warehouse eligibility, the persisted assignment/policy, and a legal workflow state.
- Escalation acknowledgement is a separate Escalation Owner responsibility. It is not review approval, final approval, or execution.
- `PREVIEW` is not executable. Governed execution requires the backend-approved saved workflow state.
- Backend authorization remains authoritative; frontend filtering is only a matching presentation safeguard.

## Changes Implemented

- Added warehouse attribution to the incident response projection without removing the existing eight-argument constructor used by current tests.
- Applied current-operator warehouse filtering to inbound, replay, connector, scenario, audit, and dispatch incident projections.
- Kept tenant-wide operators whole-tenant and excluded unattributed incident sources from scoped operator views.
- Removed expected 403 request denials and favicon request failures from operational incident projection.
- Suppressed the generic operational alert hook for favicon static-resource failures while retaining a warning and audit trace.
- Applied warehouse filtering to frontend realtime incident context.

No routes, scenario authority checks, product APIs, authentication behavior, or proof selectors were intentionally changed.

## Metric Scope Review

| Runtime item | Scope in current implementation | Interpretation |
| --- | --- | --- |
| Dispatch queued | Tenant telemetry/dispatch view where returned by tenant snapshot | Do not reinterpret as a warehouse count without attribution |
| Failed dispatch | Tenant repository query; work items are unattributed to warehouse | Tenant-wide evidence; hidden from scoped incident lane |
| Incidents | Current tenant incident projection; warehouse-filtered where attributable | Scoped operators see only relevant attributable incidents |
| Orders | Tenant-scoped and filtered by existing operator access in dashboard snapshot | Warehouse filtering follows existing order access model |
| Fulfillment | Tenant snapshot with existing operator filtering | Not presented as a global platform total in tenant context |
| Dispatch processed | Tenant-tagged operational metrics where available | Tenant metric, not a warehouse metric |
| Realtime publishes | Tenant-tagged metrics where available | Tenant metric; platform aggregate remains platform-only |
| Publish failures | Tenant-tagged metrics where available | Tenant metric; not a warehouse attribution |
| Lock conflicts | Tenant-tagged metrics where available | Tenant metric, not a warehouse-specific incident by itself |
| Latest dispatch / oldest queued | Tenant dispatch evidence | Tenant-wide unless a warehouse attribution is reported |

Global gauges and cross-tenant platform counters remain platform-runtime concerns. The tenant Runtime should not describe them as warehouse counts.

## Validation

Completed locally on this correction:

- Backend full suite: `152` tests, `0` failures, `0` errors.
- Frontend lint: PASS.
- Frontend build: PASS.
- Existing realtime DTO tests: PASS through the compatibility constructor.

The backend test output contains intentional test-controller error logs for the CORS/unexpected-failure test; the test suite passed. They are not evidence of a production regression.

Required after deployment:

- Live connection classification with `PROOF_ALLOWED=True`.
- Fresh-tenant rendered checks for Platform Owner, Tenant Admin, North-scoped operator, and Coast-scoped operator.
- Explicit expected-403 non-incident confirmation.
- Explicit static-resource failure non-STOP confirmation.
- Genuine tenant and warehouse incident visibility checks.
- Governance checks for assigned Review Owner, wrong assignment, wrong warehouse, assigned Final Approver, PREVIEW, and valid governed execution.
- Full hosted proof only after the deployed bundle and live prerequisites are healthy.

## Limitations

- Audit and failed-dispatch records do not currently carry warehouse attribution, so scoped operators do not receive them in the warehouse incident lane. This is conservative and avoids false ownership.
- Tenant Runtime still contains tenant-level metrics that are not warehouse dimensions. They must be read as tenant posture, not site-specific counts.
- Platform Activity and Platform Runtime remain separate control-plane surfaces; this correction does not add a platform incident API.
- Live evidence must be refreshed after deployment. Local tests do not substitute for the requested rendered acceptance walkthrough.

## Gate Status

Source correction: `IMPLEMENTED`.

Local regression evidence: `PASS`.

Runtime/activity/governance correction gate: `PENDING POST-DEPLOY LIVE EVIDENCE`.

CSV operational acceptance: `BLOCKED UNTIL THIS GATE CLOSES`.
