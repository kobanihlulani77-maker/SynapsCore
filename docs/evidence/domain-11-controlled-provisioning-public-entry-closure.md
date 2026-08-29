# Domain 11: Controlled Provisioning and Public Entry Closure

Status: implemented and locally verified

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

The live public-control execution was first attempted before deployment of this
revision and correctly exercised the still-deployed public bundle; it must be
rerun after the frontend deployment serves this revision. That pre-deployment
failure is deployment lag, not evidence to weaken the public route assertion.

## Boundaries not expanded here

This closure does not redesign authentication/session behavior, build an email
invitation system, add a Platform Owner provisioning wizard, or change tenant
role semantics outside the initial provisioning handoff. Those remain separate
review domains.
