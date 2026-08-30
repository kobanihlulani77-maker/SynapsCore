# Domain 11: Controlled Provisioning and Public Entry Closure

Status: fresh explicit provisioning implemented and locally verified; deployment proof pending

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
tenant payload, and requires a tenant administrator plus any roles explicitly
listed in `requiredRoles`. When a required role is declared, its authority must
cover every configured warehouse; an undeclared role is not manufactured merely
to make the tenant appear ready. Empty warehouse scope is the established
tenant-wide convention; a non-empty scope is explicit site authority.

The transaction creates the tenant, warehouses, operators, and users atomically.
Duplicate tenant codes return HTTP 409, including the database uniqueness race.
Invalid plans fail before a successful response. Generated temporary credentials
are returned only in the protected provisioning response, stored only as a
BCrypt hash, and marked for password change; hashes and secrets are never
returned.

Production and the default application configuration require explicit tenant
provisioning and disable starter inventory and connector seeding. Test/demo
compatibility fixtures may explicitly enable legacy fallback behavior through
test configuration; that compatibility path is not the production onboarding
contract. A fresh controlled company therefore does not receive fake products,
inventory, orders, alerts, recommendations, replay records, or scenario
activity merely because it was provisioned.

## Fresh-tenant invariant closure

The new focused coverage verifies that a clean controlled tenant can be
provisioned with exactly the supplied configuration:

- one configured warehouse is persisted as exactly one warehouse
- three configured warehouses are persisted as exactly three warehouses
- warehouse codes, names, and locations are taken from the request rather than
  the `WH-NORTH`/`WH-COAST` examples
- a Review Owner with `ALPHA-DC` scope is not granted `BETA-DC` authority
- a minimal request containing only explicit `TENANT_ADMIN` and
  `INTEGRATION_ADMIN` users creates no synthetic Review Owner, Final Approver,
  or Escalation Owner
- a missing role named in `requiredRoles` fails the request before the tenant
  is persisted
- the new tenant has zero products, inventory rows, connectors, orders,
  fulfillment tasks, alerts, recommendations, scenarios, inbound records, and
  replay records

The production path now requires callers to supply the actual company users,
roles, warehouse scopes, and any workflow roles needed for readiness. It does
not generate a generic Operations Lead, Operations Planner, Executive, or
warehouse governance identities for a real tenant.

## Focused verification

`Domain11ControlledProvisioningIntegrationTest` covers 9 tests:

- anonymous provisioning denial
- custom warehouse creation without the WH-NORTH/WH-COAST production assumption
- explicit review/final-approval warehouse scopes
- `READY` response and protected temporary credential handoff metadata
- absence of password hashes in the response
- duplicate tenant conflict behavior
- invalid warehouse scope rejection
- `PLATFORM_OWNER` rejection from the tenant role model
- exact one-warehouse and three-warehouse configuration
- explicit warehouse-scoped Review Owner authority
- minimal explicit user creation without synthetic governance identities
- failure when an explicitly required role has no warehouse coverage
- zero operational state after fresh provisioning

The existing public control test now verifies contact navigation and the absence
of the public Create Workspace CTA/form. The removed `CreateWorkspace.jsx` page
and route are no longer part of the public application registry.

Local verification recorded for this closure:

- `Domain11ControlledProvisioningIntegrationTest`: 9 tests, 0 failures, 0 errors
- full backend Maven suite: 253 tests, 0 failures, 0 errors
- frontend lint, build, and verify: passed
- documentation link check: 777 links checked, none missing

The first full-suite attempt exposed a timing-sensitive failure in the existing
inventory concurrency test and an expected compatibility fixture assertion after
the planner fallback was removed. The planner fixture was restored only behind
the non-explicit test compatibility setting; the concurrency test passed in an
isolated rerun, and the complete suite then passed 253/253.

This fresh-provisioning change has not yet been deployed or live-proven. The
post-deployment evidence below remains valid for the earlier deployed revision;
a new live proof run is required after deployment of this change.

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
