# Company User Provisioning Runbook

This is the internal SynapseCore Phase 4 runbook for provisioning Company 1 user access inside an already-created tenant/workspace.

The customer does not register themselves, create their own account, create a tenant, or choose SynapseCore roles. The SynapseCore platform owner provisions approved users, verifies authentication and authorization, records evidence, and prepares access material for a later controlled handoff.

This runbook is grounded in current repository behavior. It does not invent email invitations, MFA, SSO, temporary-password links, account deletion, or customer self-registration.

## Phase Boundary

Phase 4 includes:

- receiving the approved Phase 2 user list
- confirming the Phase 3 tenant/workspace is verified
- mapping business responsibilities to actual SynapseCore roles
- creating or updating operator lanes
- creating user accounts
- assigning accounts to the correct tenant and operator lane
- issuing safe initial credential material through current capabilities
- verifying login, roles, tenant scope, and negative authorization
- classifying bootstrap identities
- recording evidence
- handing a verified user/access matrix to Phase 5

Phase 4 does not include:

- configuring connectors
- loading Company 1 products, inventory, orders, or CSV files
- configuring alerts, recommendations, scenarios, or approval policies
- customer-facing handoff
- building public registration
- adding SSO, MFA, or enterprise IAM

## Current User Model

In current SynapseCore, a user is a tenant-scoped sign-in identity.

| Area | Current implementation |
| --- | --- |
| Entity | `com.synapsecore.domain.entity.AccessUser` |
| Table | `access_users` |
| Primary key | `id`, generated `Long`, database identity |
| Tenant relationship | `tenant_id` many-to-one to `Tenant` |
| Username | `username`, max 80, normalized lowercase |
| Full/display name | `fullName`, max 120 |
| Password storage | `passwordHash`, BCrypt encoded |
| Active flag | `active`, default `true` |
| Password-change flag | `passwordChangeRequired`, default `false` |
| Session invalidation field | `sessionVersion`, default `1` |
| Operator relationship | required many-to-one `access_operator_id` |
| Password timestamp | `passwordUpdatedAt` |
| Lifecycle timestamps | `createdAt`, `updatedAt` |
| Unique constraint | tenant + username |
| Repository | `AccessUserRepository` |
| Service | `AccessAdministrationService`, `AuthSessionService` |
| Controller | `AccessController`, `AuthController` |

What exactly is a user today:

- a login account scoped to one tenant
- authenticated with tenant code, username, and password
- authorized through the linked operator lane
- not an email-invitation record
- not a global account across tenants
- not a platform-wide identity

## Current Operator Model

An operator is the operational persona that carries roles and warehouse scope.

| Area | Current implementation |
| --- | --- |
| Entity | `com.synapsecore.domain.entity.AccessOperator` |
| Table | `access_operators` |
| Primary key | `id`, generated `Long`, database identity |
| Tenant relationship | `tenant_id` many-to-one to `Tenant` |
| Actor identifier | `actorName`, max 80 |
| Display name | `displayName`, max 80 |
| Description | `description`, max 160 |
| Active flag | `active`, default `true` |
| Roles table | `access_operator_roles` |
| Warehouse scope table | `access_operator_warehouse_scopes` |
| Unique constraint | tenant + actor name |
| Repository | `AccessOperatorRepository` |
| Service | `AccessAdministrationService`, `AccessDirectoryService` |

Every sign-in user must map to an active operator for usable access. A user can be active, but still blocked if the linked operator is inactive. Operator roles and warehouse scopes define what the signed-in person can do.

Do not create duplicate users/operators unnecessarily. For Company 1, create one operator lane per distinct permission/scope profile, then map one or more users to those lanes only when that mapping is intentional and approved.

## Actual Supported Roles

The authoritative role enum is `SynapseAccessRole`.

| Role | Meaning | Tenant scope | Key backend capabilities | Frontend behavior | Company 1 suitability |
| --- | --- | --- | --- | --- | --- |
| `TENANT_ADMIN` | Tenant access/configuration administrator | Current tenant only | Manage users, operators, workspace settings, security settings, product create/update/import, operational policy update, tenant-admin surfaces | Enables admin editing and access management controls | Suitable only for 1-2 approved Company 1 admins; high impact |
| `REVIEW_OWNER` | Scenario review owner | Current tenant and warehouse scope | Approve/reject review-stage scenarios where actor and scope match | Scenario/approval controls for review actions | Suitable for operational managers/planners who approve first-stage decisions |
| `FINAL_APPROVER` | Final approval authority | Current tenant and warehouse scope | Final approval for escalated scenarios where actor and scope match | Final approval controls for escalated decisions | Suitable for senior approvers only; high impact |
| `ESCALATION_OWNER` | Escalation acknowledgement owner | Current tenant and warehouse scope | Acknowledge escalations where actor and scope match | Escalation controls | Suitable for escalation coordinators |
| `INTEGRATION_ADMIN` | Connector administrator and replay operator | Current tenant | Create/update integration connectors and perform replay operations | Integration admin actions and replay actions | Suitable for technical operations lead; high impact |
| `INTEGRATION_OPERATOR` | Replay/recovery operator | Current tenant and warehouse scope where applicable | Replay failed inbound orders; integration operator actions | Replay queue actions | Suitable for recovery operator |

Important distinction:

- `REQUESTER` exists in `ScenarioActorRole`, not in `SynapseAccessRole`.
- A requester can be named in scenario payloads, but it is not an assignable user access role.

## Platform vs Customer Roles

Current application roles are tenant-scoped; there is no customer-assignable global platform-admin role in `SynapseAccessRole`.

Platform-owner authority is currently handled separately through:

- `X-Synapse-Platform-Admin-Token` for tenant creation
- `X-Synapse-Bootstrap-Token` for first empty-database bootstrap
- operational access to deployment/configuration secrets

Company 1 ordinary users must never receive:

- platform-admin token
- bootstrap token
- deployment secrets
- database credentials
- Render/admin credentials
- proof-state credentials

High-impact tenant roles that require explicit approval:

- `TENANT_ADMIN`
- `FINAL_APPROVER`
- `INTEGRATION_ADMIN`

Operational tenant roles that may fit normal pilot users:

- `REVIEW_OWNER`
- `ESCALATION_OWNER`
- `INTEGRATION_OPERATOR`

Read-only review limitation:

- there is no explicit read-only role today. Any active signed-in user with an active operator can access general workspace read surfaces through `requireWorkspaceAccess`. A no-role operator can be used for low-privilege monitoring, but it is not a formal read-only role with route-by-route policy.

## Business Responsibility Mapping

Use least privilege. Map the responsibility, not the job title.

| Business responsibility | Required SynapseCore capability | Minimum current role/profile |
| --- | --- | --- |
| Ordinary operational monitoring | Sign in, view dashboard/runtime/orders/inventory/alerts/recommendations where permitted by workspace access | Active operator with no roles or narrow operational roles |
| Replay/recovery operation | View replay queue and replay failed inbound records | `INTEGRATION_OPERATOR` |
| Connector administration | Create/update connectors and manage integration policy | `INTEGRATION_ADMIN` |
| Scenario review | Review-stage scenario approval/rejection | `REVIEW_OWNER` |
| Final approval | Final approval on escalated scenario flow | `FINAL_APPROVER` |
| Escalation acknowledgement | Acknowledge escalations | `ESCALATION_OWNER` |
| Tenant administration | Manage access, workspace settings, product catalog mutation, security policy | `TENANT_ADMIN` |
| Read-only business review | Low-risk browsing without admin actions | No-role active operator, documented limitation |

Do not assign all roles to a company manager just because they are senior. Assign only the role needed for the pilot lane.

## Current User Creation Mechanisms

| Mechanism | Classification | Notes |
| --- | --- | --- |
| `POST /api/access/admin/operators` | Supported for company pilot | Creates an operator lane; requires tenant admin session. |
| `PUT /api/access/admin/operators/{operatorId}` | Supported for company pilot | Updates active state, roles, display metadata, warehouse scope. |
| `POST /api/access/admin/users` | Supported for company pilot | Creates a tenant-scoped sign-in account; requires tenant admin session. |
| `PUT /api/access/admin/users/{userId}` | Supported for company pilot | Updates full name, active state, operator mapping; does not change username. |
| `POST /api/access/admin/users/{userId}/reset-password` | Supported for company pilot | Resets password, marks password change required, increments session version. |
| `/users` page | Inspection surface | Shows user/operator posture; current editing actions are handled through shared admin state/hooks, not a full separate wizard on the page itself. |
| `/settings` page | Tenant admin settings surface | Supports workspace/security/warehouse/connector support settings, not primary user creation. |
| `/tenant-management` page | Internal tenant rollout surface | Creates tenant/workspace, not Phase 4 user roster. |
| Tenant onboarding bootstrap | Bootstrap only | Creates bootstrap users/operators during Phase 3 tenant creation. Must be reconciled before handoff. |
| `scripts/prepare-hosted-proof.ps1` | Proof/test only, partially reusable | Ensures proof operators/users through supported APIs; proof credentials are not reusable. |
| `scripts/verify-company-readiness.ps1` | Local/self-host rehearsal | Exercises user lifecycle and many later-phase flows; not live Company 1 user provisioning. |
| Seeds/test fixtures | Development/test only | Not Company 1 provisioning. |
| Direct database manipulation | Unsafe | Not approved for normal pilot user provisioning. |

## Hosted-Proof User Provisioning Trace

Hosted proof uses supported tenant-admin APIs:

1. `prepare-hosted-proof.ps1` signs in as the proof tenant admin.
2. It calls `GET /api/access/admin/operators`.
3. It creates or updates proof operators through `POST`/`PUT /api/access/admin/operators`.
4. It calls `GET /api/access/admin/users`.
5. It creates or updates proof users through `POST`/`PUT /api/access/admin/users`.
6. It uses `POST /api/access/admin/users/{userId}/reset-password` when required.
7. It signs in as the created proof user with a temporary password.
8. It calls `/api/auth/session/password` to change to the final proof password.
9. Playwright validates role-gated browser behavior.

Reusable:

- duplicate lookup before create
- create/update operator lane before creating users
- create/reset user through tenant-admin API
- immediate login verification
- password-change verification
- fail-fast behavior

Not reusable:

- proof usernames
- proof passwords
- proof tenant names
- proof product/data setup
- ignored proof state file as a customer secret store

## Bootstrap Identities from Tenant Creation

Tenant onboarding currently creates these bootstrap operators:

| Operator | Roles | Disposition |
| --- | --- | --- |
| `Operations Lead` | `TENANT_ADMIN`, `REVIEW_OWNER`, `ESCALATION_OWNER`, `INTEGRATION_ADMIN`, `INTEGRATION_OPERATOR` | Keep as internal provisioning account initially; disable or replace before handoff unless explicitly approved as a customer admin lane. |
| `Executive Operations Director` | `FINAL_APPROVER` | Reset/convert only if matching an approved Company 1 final approver; otherwise keep internal/disabled before handoff. |
| `Operations Planner` | no roles | May be used as low-privilege operator lane if approved; otherwise treat as bootstrap/default only. |

Tenant onboarding currently creates these bootstrap users:

| User | Password behavior | Disposition |
| --- | --- | --- |
| Initial admin username supplied during tenant creation | Password supplied by platform operator; `passwordChangeRequired=false` | Treat as temporary SynapseCore provisioning admin. Do not hand to customer without Phase 4 approval/rotation. |
| Generated executive username | Random generated password not returned to operator; `passwordChangeRequired=true` | Not usable until reset by tenant admin. Convert only if approved or disable/remap during Phase 4. |

No unnecessary privileged account should remain active for convenience. Keep at least one usable tenant admin sign-in lane until a verified replacement exists.

## Password and Session Model

Password handling:

- `AuthConfig` uses `BCryptPasswordEncoder`
- passwords are stored as BCrypt hashes in `access_users.passwordHash`
- create/reset endpoints accept plaintext only in request body over the active API request
- responses do not return passwords or hashes
- audit logs record the action and user id/username, not password values

Supported today:

| Capability | Status | Notes |
| --- | --- | --- |
| Admin-created initial password | Supported | `POST /api/access/admin/users` requires `password`. |
| Admin password reset | Supported | `POST /api/access/admin/users/{userId}/reset-password`. |
| Forced password-change flag | Supported | New users and reset users are marked `passwordChangeRequired=true`. |
| User self-password change | Supported | `POST /api/auth/session/password`. |
| Password rotation awareness | Supported | Session response includes `passwordRotationRequired`. |
| Email invitation | Not supported | No email invitation/token flow exists. |
| Forgot password | Not supported | Admin reset is the current recovery path. |
| MFA | Not supported | Future hardening item. |
| SSO/SAML/OIDC | Not supported | Future enterprise hardening item. |
| Temporary link/token password reset | Not supported | Future hardening item. |
| User deletion | Not supported | Disable is the current reversible revocation path. |

## Initial Access Strategy

Official Company 1 strategy:

1. Create approved operator lanes first.
2. Create approved user accounts with unique strong initial passwords.
3. Mark credentials as temporary because current creation sets `passwordChangeRequired=true`.
4. Verify first login and require password change before handoff where operationally possible.
5. Prefer customer-changed passwords before day-one operational use.
6. Never store passwords in Git, docs, evidence records, screenshots, or shared notes.

Documented limitation:

- SynapseCore does not yet provide a customer-facing invitation or password-reset-token flow.
- The platform owner may temporarily know the initial password.
- This is manageable for a controlled pilot only if passwords are unique, securely generated, securely handed off, and changed by the user at first login.

This should be post-pilot hardening for broader enterprise rollout, not ignored.

## Credential Generation Rule

Initial passwords must be:

- unique per user
- at least 16 characters for pilot operations, even though backend minimum is 8
- generated by an approved password manager or secure generator
- not based on company name, username, role, birthday, location, or pilot name
- not reused from proof fixtures
- not logged
- not committed
- not stored in the provisioning record
- not shared in the same message/document as all identity and role details

Acceptable evidence wording:

```text
Initial credential generated and stored in approved secret channel reference: <reference only>
```

Never write the actual secret.

## Credential Handoff Posture

Phase 4 prepares access material but does not perform final customer handoff.

Identity information:

- tenant/workspace code
- URL
- username
- display name
- assigned responsibility
- role summary

Secret information:

- initial password
- reset password
- temporary secure channel reference

Keep identity information and secret information separate. Do not send username and password together in a broadly shared document.

If no secure customer secret channel exists yet, Phase 4 can still complete technical provisioning but must mark handoff as blocked until Phase 9/customer handoff defines the secure channel.

## Official Company 1 User Creation Mechanism

The official Phase 4 path is:

```text
Tenant-admin protected backend API, optionally operated through the existing authenticated admin UI.
```

Canonical API order:

1. `GET /api/access/admin/operators`
2. `POST /api/access/admin/operators` or `PUT /api/access/admin/operators/{operatorId}`
3. `GET /api/access/admin/users`
4. `POST /api/access/admin/users` or `PUT /api/access/admin/users/{userId}`
5. `POST /api/access/admin/users/{userId}/reset-password` when needed
6. `POST /api/auth/session/login`
7. `GET /api/auth/session`
8. `POST /api/auth/session/password` for user self-change verification
9. `POST /api/auth/session/logout`

Fallback:

- Use the existing UI surfaces only when the tenant-admin session is healthy and the operator can clearly see the intended tenant and user.
- Do not use direct SQL.
- Do not use hosted-proof state files.

## API Contracts

Create operator:

```text
POST /api/access/admin/operators
Required role: TENANT_ADMIN
Tenant context: signed-in tenant admin session
```

```json
{
  "actorName": "North Review Manager",
  "displayName": "North Review Manager",
  "description": "Warehouse-scoped review owner for the approved pilot lane.",
  "active": true,
  "roles": ["REVIEW_OWNER"],
  "warehouseScopes": ["WH-NORTH"]
}
```

Create user:

```text
POST /api/access/admin/users
Required role: TENANT_ADMIN
Tenant context: signed-in tenant admin session
```

```json
{
  "username": "north.review.manager",
  "fullName": "North Review Manager",
  "password": "<unique-initial-password>",
  "operatorActorName": "North Review Manager"
}
```

Update user:

```text
PUT /api/access/admin/users/{userId}
```

```json
{
  "fullName": "North Review Manager",
  "active": true,
  "operatorActorName": "North Review Manager"
}
```

Reset password:

```text
POST /api/access/admin/users/{userId}/reset-password
```

```json
{
  "password": "<unique-reset-password>"
}
```

Expected user response:

- `id`
- `tenantCode`
- `tenantName`
- `username`
- `fullName`
- `operatorActorName`
- `operatorDisplayName`
- `roles`
- `warehouseScopes`
- `active`
- `passwordChangeRequired`
- `passwordUpdatedAt`
- `createdAt`
- `updatedAt`

Common failures:

| Status | Meaning | Action |
| --- | --- | --- |
| 400 | invalid field, invalid role, invalid warehouse scope, last-admin protection | Stop and correct approved values |
| 401 | no valid signed-in session | Sign in again as tenant admin |
| 403 | user lacks `TENANT_ADMIN` or required role | Stop; do not bypass |
| 404 | user/operator not in current tenant | Stop and verify tenant context |
| 409 | duplicate username/operator actor name | Stop and resolve duplicate |
| 5xx | backend/runtime/DB problem | Stop and recover infrastructure first |

## Pre-Creation Safety Check

Before every user creation, confirm:

- correct environment
- correct backend URL
- correct tenant code
- Phase 3 tenant/workspace verification passed
- user exists in approved Phase 2 list
- business identity matches approved record
- responsibility is known
- role mapping is least-privilege
- operator lane exists or is intentionally being created
- warehouse scope is approved and exists
- username is not duplicate in the tenant
- bootstrap/proof accounts are not being reused accidentally
- no secrets will be recorded

Stop if uncertain.

## Duplicate User Rules

Current database uniqueness:

- username is unique per tenant
- same username may exist in another tenant
- there is no email field on `AccessUser`
- full name is not unique

Before creation:

- check `GET /api/access/admin/users`
- check active and inactive users
- check visually similar usernames
- check proof/test-style usernames
- confirm the approved identity has not already been created under another spelling

If duplicate exists:

- do not create another account
- decide whether to update, reactivate, reset, or leave unchanged
- record the decision

## User Provisioning Sequence

### 1. Verify environment

Action:

- display backend URL, frontend URL, tenant code, company, and operator identity
- sign in as the approved tenant admin

Expected result:

- session belongs to Company 1 tenant

Failure condition:

- wrong tenant, wrong URL, missing admin session, or unhealthy backend

Evidence:

- session tenant code, admin username, timestamp

### 2. Verify Phase 3 handoff

Action:

- review Phase 3 provisioning record
- confirm tenant id/code/name and initial state

Expected result:

- tenant/workspace is accepted for Phase 4

Failure condition:

- missing Phase 3 evidence or unresolved provisioning limitation

Evidence:

- Phase 3 record reference

### 3. Map responsibility to role

Action:

- map approved business responsibility to minimum SynapseCore role and warehouse scope

Expected result:

- each user has a justified role/profile

Failure condition:

- vague job title, overbroad admin request, or unapproved warehouse scope

Evidence:

- role matrix row

### 4. Create or update operator lane

Action:

- create or update `AccessOperator`

Expected result:

- active operator has approved roles and warehouse scope

Failure condition:

- duplicate actor name, invalid role, invalid warehouse scope, last-admin conflict

Evidence:

- operator id, actor name, roles, warehouse scopes

### 5. Create user

Action:

- create `AccessUser` mapped to operator lane

Expected result:

- user exists, active, tenant code matches Company 1, password-change required is true

Failure condition:

- duplicate username, wrong tenant, weak/missing password, missing operator

Evidence:

- user id, username, full name, active status, operator mapping

### 6. Verify authentication

Action:

- sign in with tenant code, username, and initial password
- call `GET /api/auth/session`

Expected result:

- signed in true
- tenant code matches
- actor name matches operator lane
- roles and warehouse scopes match expected matrix

Failure condition:

- login failure, wrong tenant, wrong role, stale session, password rate limit

Evidence:

- auth verified yes/no, timestamp

### 7. Verify authorization

Action:

- test one allowed action/read surface and one denied action where practical

Expected result:

- allowed action succeeds
- denied action returns forbidden/no disclosure

Failure condition:

- excessive privileges or expected capability missing

Evidence:

- endpoints/routes checked and result

### 8. Verify password change

Action:

- where handoff posture allows, perform self-password change through `/profile` or `/api/auth/session/password`

Expected result:

- `passwordChangeRequired=false`
- old password no longer works
- current session refreshes with new session version

Failure condition:

- password cannot be changed, current password rejected unexpectedly, old password still works

Evidence:

- password change verified yes/no, no password value

### 9. Record user evidence

Action:

- complete `docs/templates/company-user-provisioning-record.md`

Expected result:

- every user row has identity, role, tenant, verification, handoff status, and issues

Failure condition:

- missing verification, secrets included, or unresolved role mismatch

Evidence:

- completed internal record

## Verification Procedures

Tenant association:

- `GET /api/auth/session` after login must report Company 1 tenant code/name.
- `GET /api/access/admin/users` as tenant admin must show the user only in Company 1.
- Business endpoints must not show proof or other tenant data.

Role verification:

- `TENANT_ADMIN`: can access user/admin endpoints; non-admin user must receive 403.
- `INTEGRATION_ADMIN`: can manage connectors; must not manage tenant access.
- `INTEGRATION_OPERATOR`: can replay records; must not manage connectors or tenant users.
- `REVIEW_OWNER`: can approve review-stage scenario only when actor and warehouse scope match.
- `FINAL_APPROVER`: can final-approve escalated scenario only when actor and warehouse scope match.
- `ESCALATION_OWNER`: can acknowledge escalations only when actor and scope match.
- no-role operator: can sign in and view general workspace surfaces; must not perform role-gated admin/replay/approval actions.

Cross-tenant negative test:

- do not damage proof/test tenants
- use existing safe proof/test tenant records where possible
- verify changing tenant headers, tenant codes, or object ids does not disclose another tenant's operational data

Platform-admin isolation:

- ordinary Company 1 users must not receive platform-admin/bootstrap tokens
- ordinary Company 1 users must not be able to create tenants in production
- ordinary Company 1 users must not access deployment/admin secrets

Frontend route/access checks:

- expected navigation renders without fatal errors
- role-restricted buttons/actions are unavailable or fail safely
- platform/admin surfaces do not grant platform-secret capability
- empty-state screens remain truthful before connectors/data exist

Do not redo the full exhaustive proof here; this is access provisioning proof only.

## Account Disable, Removal, and Revocation

Disable capability:

- supported through `PUT /api/access/admin/users/{userId}` with `active=false`
- supported for operators through `PUT /api/access/admin/operators/{operatorId}` with `active=false`
- disabling/remapping a user increments `sessionVersion`
- inactive users cannot sign in
- inactive operators invalidate existing sessions and block sign-in

Delete capability:

- no supported user delete endpoint
- no supported operator delete endpoint
- disable is the current reversible revocation path

Emergency revocation:

1. sign in as tenant admin
2. disable the user or operator lane
3. verify `GET /api/auth/session` for an existing session returns signed out where testable
4. verify new login fails
5. record incident/evidence
6. reset password if compromise is suspected

Do not directly delete database rows.

## Session Behavior

Current behavior:

- sessions are established by `POST /api/auth/session/login`
- session identity stores tenant code, actor, username, authenticated timestamp, user session version, and tenant security policy version
- production sessions are Redis-backed through Spring Session configuration
- sign-out clears identity and rotates session id
- password reset increments user `sessionVersion`
- user active-state changes and operator remaps increment user `sessionVersion`
- tenant security changes can increment tenant `securityPolicyVersion`
- stale session versions are rejected and return signed out/unauthorized behavior
- session expiry follows tenant `sessionTimeoutMinutes`

Rate limiting:

- authentication and password-change rate limiting are enforced by security filters
- provisioning verification must avoid repeated failed-login loops
- do not weaken rate limits for convenience

## Empty Initial Business State

Phase 4 occurs before connectors and Company 1 data are loaded.

New users may see:

- working sign-in
- dashboard shell with little or no operational data
- truthful empty states
- starter warehouses from tenant onboarding
- bootstrap audit/access posture

Do not load fake products, orders, inventory, connectors, replay records, or scenarios just to make user verification look full.

## Correction Path

Supported corrections:

| Field/action | Current capability |
| --- | --- |
| Full name | `PUT /api/access/admin/users/{userId}` |
| Active/inactive user | `PUT /api/access/admin/users/{userId}` |
| Operator mapping | `PUT /api/access/admin/users/{userId}` |
| Password | `POST /api/access/admin/users/{userId}/reset-password` |
| Operator display/description | `PUT /api/access/admin/operators/{operatorId}` |
| Operator roles | `PUT /api/access/admin/operators/{operatorId}` |
| Operator warehouse scopes | `PUT /api/access/admin/operators/{operatorId}` |
| Username | No supported update; create corrected account and disable bad account if necessary |
| Tenant reassignment | No supported update; create in correct tenant and disable wrong account |
| User deletion | No supported endpoint |
| Operator deletion | No supported endpoint |

Wrong-tenant user creation is a Phase 4 blocker. Do not improvise SQL. Disable the incorrect account if accessible through the correct tenant admin path, document the issue, and create the correct account only after approval.

## Failure Handling

| Failure | Detect | Stop | Recover | Verify |
| --- | --- | --- | --- | --- |
| Duplicate user | 409 or existing roster row | Do not create another account | Update/reset/reactivate existing approved account | Roster and login check |
| Invalid role | 400 enum/validation error | Do not substitute role names | Use exact `SynapseAccessRole` | Operator response roles |
| Wrong tenant | Session/response tenant mismatch | Stop immediately | Sign into correct tenant and assess exposure | Tenant association check |
| Invalid username | 400 validation error | Stop | Correct approved username | Create response |
| Weak/invalid password | 400 validation or policy failure | Stop | Generate compliant unique password | Login/password change |
| Unauthorized admin | 401/403 | Stop | Use verified tenant admin | Admin endpoint succeeds |
| Backend/DB failure | timeout/5xx/readiness issue | Stop | Recover infrastructure | Live connection check |
| Partial profile creation | user created but operator wrong/missing | Stop before handoff | Correct mapping or disable | Auth/role matrix |
| Login failure | 401/rate limit/error | Stop failed-login loop | Check credential/active/operator/session | One clean successful login |
| Role mismatch | session roles differ from matrix | Stop | Correct operator role/scope | Allowed/denied tests |

Never proceed to handoff if account state is uncertain.

## Environment Safety Controls

Before live Company 1 user provisioning, display:

```text
ENVIRONMENT:
BACKEND URL:
FRONTEND URL:
TENANT CODE:
COMPANY:
SIGNED-IN ADMIN:
PHASE 3 RECORD:
APPROVED USER COUNT:
```

Biggest manual risks:

- creating the user in a proof tenant
- assigning `TENANT_ADMIN` too broadly
- giving a customer platform/bootstrap token
- typo in username
- shared password across users
- leaving bootstrap privileged account active
- skipping negative authorization checks

## User Provisioning Evidence

Use `docs/templates/company-user-provisioning-record.md`.

Record:

- company
- tenant id/code
- approved business identity
- username
- display name
- operator actor name
- roles
- warehouse scopes
- active status
- password-change posture
- creation timestamp
- authentication verified
- authorization verified
- tenant isolation verified
- credential handoff status
- provisioning operator role
- issues

Do not record:

- password
- password hash
- token
- session cookie
- reset secret
- proof state content

## User Matrix Format

Use this summary before authorizing Phase 5:

| User | Business responsibility | SynapseCore role/profile | Tenant | Status | Auth verified | Role verified | Handoff ready |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |

Recommended pilot size:

- 3-5 operators
- create one by one
- avoid bulk tooling unless the pilot roster grows beyond manual-safe size

## Company Readiness Script Applicability

`scripts/verify-company-readiness.ps1` becomes more relevant after Phase 4 because it demonstrates:

- tenant-admin user lifecycle
- operator/user creation
- password reset
- role remapping
- disabled user/operator behavior
- replay/scenario/connector checks in rehearsal context

Still waits for Phase 5-7:

- real Company 1 connector setup
- real Company 1 data onboarding
- integration/replay with company-specific source systems
- company-specific operational configuration
- handoff/customer SOP checks

Do not claim Company 1 is fully ready after Phase 4. Phase 4 proves access, not integrations or operational data.

## Phase 4 Security Gate

Phase 4 is not accepted until:

- approved user list matches Phase 2
- correct tenant verified
- every required user exists or is explicitly deferred
- no accidental duplicate accounts exist
- user/operator mapping is correct
- roles are least-privilege and verified
- no customer user has unintended platform/admin capability
- authentication is verified
- representative negative authorization is verified
- tenant isolation is verified
- access revocation procedure is known
- bootstrap/provisioning accounts are classified
- password/secret material is absent from Git/docs
- handoff material is prepared safely
- user evidence record is complete

## Phase 5 Handoff

Phase 5 receives:

- verified tenant id/code
- verified Company 1 user matrix
- approved role matrix
- authentication results
- tenant isolation results
- admin/revocation posture
- temporary provisioning admin disposition
- Company 1 technical contact reference
- approved initial connector lane from Phase 2
- secure-secret-handoff status

Phase 5 does not receive:

- user passwords
- platform-admin token
- bootstrap token
- connector secrets unless separately approved for Phase 5

## Phase 4 Verdict Options

Use one of these only:

- `COMPANY PILOT PHASE 4 ACCEPTED`
- `COMPANY PILOT PHASE 4 ACCEPTED WITH DOCUMENTED LIMITATION`
- `COMPANY PILOT PHASE 4 NOT ACCEPTED - SAFE USER PROVISIONING INCOMPLETE`

Given the current implementation, the expected Phase 4 posture is usually:

```text
COMPANY PILOT PHASE 4 ACCEPTED WITH DOCUMENTED LIMITATION
```

Reason: safe tenant-admin user provisioning exists, password reset and self-change exist, and session invalidation is implemented; however, there is no email invitation, MFA, SSO, forgot-password flow, user delete endpoint, or formal read-only role.
