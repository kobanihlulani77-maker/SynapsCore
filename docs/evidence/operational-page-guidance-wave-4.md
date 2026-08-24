# Operational Page Guidance Wave 4 Evidence

Date: 2026-08-24

## Closure Scope

Wave 4 covers the remaining administrative and lighter tenant surfaces plus
the application-wide public/auth and role-aware consistency review:

- `/users`
- `/company-settings`
- `/profile`
- `/create-workspace`
- `/sign-in` and `/platform-sign-in`
- public routes and role-aware next actions

Phase 14 remains held until this closure is formally reviewed. No Phase 14
work was started during this verification.

## Deployment And Proof Baseline

- Repository HEAD: `800fcefb70454fdb57a7791a50e9483748655417`
- Served frontend asset observed: `index-2InG-0Wb.js`
- The served asset confirms a new frontend deployment, but the runtime does
  not expose an exact source commit, so the asset and repository revision are
  recorded separately.
- Live connection classification supplied after backend recovery:

```text
FRONTEND_UP=True
BACKEND_UP=True
DB_READY=True
AUTH_READY=True
WS_READY=True
PROOF_ALLOWED=True
```

- Hosted production proof: `6/6 PASS`
- Existing proof tenant/state was reused: `HOSTED-PROOF-WAVE2-20260824`
- No new tenant was created during this closure.

The attempted `prepare-hosted-proof.ps1` run refused tenant inspection because
the PowerShell session did not contain the private platform/bootstrap token.
This is classified as an operator-preparation condition, not a product
defect, Render outage, or proof failure. The ignored proof state remained
valid and Playwright completed the full 6/6 proof without printing secrets.

## Wave 4 Implementation Evidence

### Users

The page now explains the six tenant role lanes, tenant-wide versus warehouse
scope, role/scope consequences, access readback expectations, and the
Tenant-Admin boundary. Loading, unavailable, and empty states are distinct;
an empty roster is not presented as a completed load. Rendered classification:
**STRONG for the supported Tenant Admin surface**.

The prepared operations-lead identity carried Tenant Admin plus additional
tenant roles in its live session. A separate Integration Admin session was
used as the non-admin rendered check. It did not receive Users or Company
Settings access and direct route navigation returned it to Dashboard.

The first rendered review found one real issue: the page summary displayed
`0 tenant users managed` while the access readout was still loading. The fix
now renders a loading/unavailable state until the access read completes. The
updated deployed bundle was rechecked in visible Chrome; the old zero-loading
state was absent and the loaded roster rendered correctly.

### Company Settings

The page identifies tenant-scoped configuration, supported workspace/security/
warehouse/connector boundaries, material-change consequences, and save/readback
expectations. It does not imply control of platform infrastructure, DB, Redis,
deployment, MFA/SSO, or arbitrary workflow rules. Rendered classification:
**ADEQUATE, appropriate to the currently supported configuration surface**.

### Profile

The page remains intentionally simple and distinguishes identity, tenant,
roles, warehouse scope, password rotation, session posture, and the controls
that remain with an administrator. It does not advertise self-service role,
warehouse, tenant, governance, MFA/SSO, or unsupported recovery controls.
Rendered classification: **INTENTIONALLY SIMPLE / STRONG FOR PURPOSE**.

### Create Workspace And Public/Auth Boundaries

`/create-workspace` states that the customer submits company context and a
proposed first-admin brief; Platform Owner provisioning remains the controlled
next step. It does not claim that a customer self-creates a live tenant.
`/sign-in` clearly requires workspace code, username, and password. The
dedicated `/platform-sign-in` surface identifies the separate control-plane
identity. Public home, product, and contact routes remain pilot-scoped and do
not claim ERP/WMS replacement, automatic consequential decisions, HA,
unlimited scale, arbitrary integrations, or customer self-provisioning.

## Headed Chrome Verification

Verification used installed visible Google Chrome at `1366x768`.

### Public/auth routes

All five public routes rendered without overflow, blank states, console errors,
page errors, or HTTP error responses:

| Route | Result |
| --- | --- |
| `/` | PASS |
| `/product` | PASS |
| `/create-workspace` | PASS |
| `/sign-in` | PASS |
| `/contact` | PASS |

The dedicated platform sign-in route was also rendered successfully. A public
route sweep observed one navigation-cancelled request on `/product` and no
HTTP error; this was normal browser navigation cleanup, not a failed resource.

### Tenant routes

The final slower sweep rendered all 19 actual tenant routes with their expected
headings, no sign-in redirects, no horizontal overflow, no console/page errors,
and no HTTP 4xx/5xx responses:

`/dashboard`, `/alerts`, `/recommendations`, `/orders`, `/inventory`,
`/catalog`, `/locations`, `/fulfillment`, `/scenarios`, `/scenario-history`,
`/approvals`, `/escalations`, `/integrations`, `/replay-queue`, `/runtime`,
`/audit-events`, `/users`, `/company-settings`, `/profile`.

The integration-admin session separately rendered `/integrations`,
`/replay-queue`, `/audit-events`, and `/profile`, while `/users` and
`/company-settings` correctly redirected to Dashboard under frontend route
policy. The operations-lead session rendered the Wave 4 administrative routes
and systems routes. Its role claims included the supported integration and
escalation permissions, so the fixture is not a pure single-role Tenant Admin;
this is a fixture limitation, not a tenant-boundary bypass.

Realtime visibly moved from `Connecting` to `Live control signal` after warm-up;
runtime moved from pending to an operational state. Navigation-cancelled
requests during rapid route changes were expected and no unexpected browser
errors were observed.

### Platform routes

The dedicated platform sign-in route rendered correctly. Direct navigation to
`/platform-admin`, `/tenant-management`, `/system-config`, `/platform-activity`,
and `/releases` while signed out consistently returned the platform sign-in
surface. This reconfirms the protected boundary. Authenticated metadata-only
platform content remains covered by the accepted Wave 1 evidence; no platform
secret was needed or printed for this closure.

## Route Census And Page Depth

The current registry reconciles to 30 routes:

- Public: 5
- Tenant workspace: 19
- Platform control plane, including platform sign-in: 6

Depth census:

- **STRONG: 16** tenant operational routes
- **ADEQUATE: 7** two tenant administration routes plus five authenticated platform routes
- **INTENTIONALLY SIMPLE: 7** five public routes, platform sign-in, and Profile
- **SHALLOW: 0**
- **CRUD-LIKE: 0**
- **MISLEADING: 0**
- **BROKEN: 0**

The classifications reflect current supported responsibility, not a claim that
every page has equal operational depth.

## Consistency Review

- Tenant/workspace/company language remains audience-specific and consistent.
- Warehouse/location means operating scope; runtime health remains a separate trust signal.
- Recommendations guide decisions; scenarios model decisions; Preview is not executable.
- Review, final approval, escalation acknowledgement, and governed execution remain distinct.
- CSV failure evidence, correction, duplicate check, replay, and post-replay verification remain the proven recovery lane.
- Disabled-webhook replay/readback remains a documented limitation rather than an implied capability.
- Customer source systems remain authoritative during the pilot, while supported Tenant Admin inventory maintenance remains explicit.
- Platform Owner is not described as a tenant role.
- No new raw payload, secret, credential, or cross-tenant data exposure was introduced.
- Representative next actions land on existing meaningful routes; role-aware navigation avoids known unauthorized destinations.

## Carried Medium/Low Limitations

These are not Wave 4 Critical or High blockers:

- restricted-warehouse rendered proof remains limited by the tenant-wide fixture
- destructive fault-injected error-state screenshots are not part of this sweep
- disabled-webhook replay/readback remains limited
- import warehouse attribution remains limited
- managed provider restore evidence is not proven in this browser gate
- bounded live scale evidence, HA/failover, MFA/SSO/OIDC, generic approval, and arbitrary connector frameworks remain outside supported scope
- customer self-provisioning is intentionally unsupported; Platform Owner provisioning is required

## Verification

- Frontend lint: PASS before this evidence-only closure
- Frontend build: PASS before this evidence-only closure
- Frontend verify: PASS before this evidence-only closure
- Frontend lint/build/verify after the loading-state fix: PASS
- Hosted proof after the loading-state fix: `6/6 PASS`
- Headed Chrome at `1366x768`: PASS for all public/auth and tenant route checks
- Browser console/page errors: none observed
- HTTP 4xx/5xx during final route sweep: none observed
- `git diff --check`: PASS
- docs link check: PASS, 764 local links
- secret scan: PASS, 0 critical findings; 5 known fixture findings

## Wave 4 Verdict

**OPERATIONAL PAGE GUIDANCE WAVE 4 ACCEPTED WITH DOCUMENTED MEDIUM/LOW LIMITATIONS**

Critical blockers: `0`

High blockers: `0`
