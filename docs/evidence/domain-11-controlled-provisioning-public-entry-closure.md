# Domain 11: Controlled Provisioning and Public Entry Closure

Status: implemented and deployed proof verified

## Product contract

SynapseCore is a controlled B2B operations platform. Customers do not create
tenants or workspaces from the public site. The public entry points are the
homepage, product information, contact/pilot conversation, and sign-in to an
already-provisioned company workspace.

Company provisioning remains protected behind `POST /api/access/tenants`. It is
available only to the intentional bootstrap/platform administration boundary;
anonymous callers and ordinary tenant users are denied by the backend.

## Controlled provisioning model

The protected request supports an explicit approved configuration:

- tenant identity and description
- the company's actual warehouse codes, names, and locations
- initial users and operator identities
- tenant roles
- warehouse scopes
- supplied or generated temporary credentials

The service validates unique warehouse codes and user/operator identities,
rejects scopes that reference unknown warehouses, rejects `PLATFORM_OWNER` in a
tenant payload, and requires a tenant administrator plus review-owner and
final-approver coverage for every configured warehouse. Empty warehouse scope is
the established tenant-wide convention; a non-empty scope is explicit site
authority.

The transaction creates the tenant, warehouses, operators, and users atomically.
Duplicate tenant codes return HTTP 409, including the database uniqueness race.
Invalid plans fail before a successful response. Generated temporary credentials
are returned only in the protected provisioning response, stored only as a
BCrypt hash, and marked for password change; hashes and secrets are never
returned.

Starter inventory and connector seeding are disabled by the safe application
default. Test/demo compatibility fixtures may explicitly enable them through
test configuration. A fresh controlled company therefore does not receive fake
products, inventory, orders, alerts, recommendations, replay records, or
scenario activity merely because it was provisioned.

## Focused verification

`Domain11ControlledProvisioningIntegrationTest` covers:

- anonymous provisioning denial
- custom warehouse creation without the WH-NORTH/WH-COAST production assumption
- explicit review/final-approval warehouse scopes
- `READY` response and protected temporary credential handoff metadata
- absence of password hashes in the response
- duplicate tenant conflict behavior
- invalid warehouse scope rejection
- `PLATFORM_OWNER` rejection from the tenant role model

The existing public control test now verifies contact navigation and the absence
of the public Create Workspace CTA/form. The removed `CreateWorkspace.jsx` page
and route are no longer part of the public application registry.

Local verification recorded for this closure:

- `Domain11ControlledProvisioningIntegrationTest`: 4 tests, 0 failures, 0 errors
- full backend Maven suite: 248 tests across 29 suites, 0 failures, 0 errors
- frontend lint, build, and verify: passed
- documentation link check: 777 links checked, none missing

## Post-deployment closure evidence

The live connection check passed after the frontend deployment served the
corrected bundle:

- `FRONTEND_UP=True`
- `BACKEND_UP=True`
- `DB_READY=True`
- `AUTH_READY=True`
- `WS_READY=True`
- `PROOF_ALLOWED=True`
- deployed frontend bundle: `index-CXlr1fZ2.js`

The post-deployment controls execution completed all seven batches:

- 201 controls inventory, 0 unverified
- 170 verified working
- 24 role-restricted and verified
- 5 verified working with documented limitation
- 2 disabled by design and verified
- 0 unexpected network failures
- 0 HTTP 5xx responses

The full hosted proof then completed successfully:

- 6 tests passed
- 0 failed
- runtime readiness, authenticated dashboard snapshot, realtime, catalog,
  replay, scenario governance, role gating, operational surfaces, and auth
  rate-limit handling all passed
- total run time: approximately 3.6 minutes

During post-deployment verification, the browser surfaced an initial frontend
hydration crash on an incomplete dashboard snapshot. The frontend was hardened
with snapshot normalization and defensive dashboard hydration defaults in
commits `60b3611`, `9b1efca`, and `3221a19`; the corrected deployed bundle then
rendered the authenticated dashboard and passed the controls suite. The hosted
proof readiness helper was also aligned with the current truthful trust-rail
label `Last successful snapshot ...` rather than requiring the older `Snapshot
...` wording. Workspace and security mutation checks now wait for their actual
successful `PUT` response before asserting the UI result.

The green browser run still reports console HTTP 401 and 400 entries from
intentional negative-control coverage. These represent deliberate unauthorized
and invalid-input assertions; they were not unexpected network failures,
server errors, React errors, or unexplained application failures.

## Boundaries not expanded here

This closure does not redesign authentication/session behavior, build an email
invitation system, add a Platform Owner provisioning wizard, or change tenant
role semantics outside the initial provisioning handoff. Those remain separate
review domains.

No Phase 12 or later access/authentication review was started by this closure.
