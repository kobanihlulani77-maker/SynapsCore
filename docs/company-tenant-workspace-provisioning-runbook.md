# Company Tenant and Workspace Provisioning Runbook

This is the internal SynapseCore Phase 3 runbook for creating a company tenant/workspace for a controlled pilot.

The customer does not create their own tenant or workspace. SynapseCore provisions the company environment, verifies it, records evidence, and only then hands the verified tenant/workspace boundary to Phase 4 user provisioning.

This runbook is grounded in the current repository implementation. It does not invent a future tenant model, self-service onboarding platform, manual SQL path, or customer-specific source code.

## Phase Boundary

Phase 3 includes:

- confirming the approved Phase 2 intake
- choosing the company tenant code and display name
- creating the tenant/workspace through the supported backend onboarding API
- verifying persistence and tenant isolation
- recording provisioning evidence
- handing the verified tenant/workspace boundary to Phase 4

Phase 3 does not include:

- creating the final customer user roster
- configuring customer connectors
- collecting connector credentials
- loading customer product, inventory, or order data
- writing customer-specific application code
- manually editing database rows

## Current Tenant Model

In current SynapseCore, the tenant is the company workspace boundary.

| Area | Current implementation |
| --- | --- |
| Entity | `com.synapsecore.domain.entity.Tenant` |
| Table | `tenants` |
| Primary key | `id`, generated `Long`, database identity |
| Public/external identifier | `code` |
| Display name | `name` |
| Description | `description` |
| Active flag | `active`, default `true` |
| Security settings | `passwordRotationDays`, `sessionTimeoutMinutes`, `securityPolicyVersion` |
| Timestamps | `createdAt`, `updatedAt` |
| Unique constraint | `code` is unique |
| Repository | `TenantRepository` |
| Creation service | `TenantOnboardingService` |
| Workspace admin service | `TenantWorkspaceAdministrationService` |
| Controller | `AccessController` under `/api/access` |

The tenant owns or scopes operational records through tenant references or tenant code fields, including users, operators, warehouses, products, inventory, orders, integrations, replay records, scenarios, recommendations, alerts, events, and audit activity.

## Current Workspace Model

There is no separate `Workspace` entity or workspace table today.

The product uses the word "workspace" for the tenant-scoped operating environment exposed to users. The backend workspace view is returned by:

- `GET /api/access/admin/workspace`
- `PUT /api/access/admin/workspace`
- `PUT /api/access/admin/workspace/security`
- `PUT /api/access/admin/workspace/warehouses/{warehouseId}`
- `PUT /api/access/admin/workspace/connectors/{connectorId}`

`TenantWorkspaceResponse` is a response model over the current tenant, not a separate persisted workspace record. It includes:

- tenant id/code/name/description/active state
- security settings
- support summary
- support diagnostics
- active incidents
- recent support activity
- warehouses
- connectors
- tenant timestamps

For Company 1, "workspace created" means "tenant created and verified as the company workspace boundary."

## Existing Creation Mechanisms

| Mechanism | Classification | Notes |
| --- | --- | --- |
| `POST /api/access/tenants` with `X-Synapse-Platform-Admin-Token` | Supported for pilot | Official Company 1 provisioning path after at least one tenant exists. |
| `POST /api/access/tenants` with `X-Synapse-Bootstrap-Token` | Initial bootstrap only | Only for the first tenant on an empty database. Not the normal Company 1 path after platform bootstrap. |
| `/tenant-management` authenticated UI | Internal admin surface | Calls `/api/access/tenants`; production success depends on backend authorization policy. Not the canonical Company 1 operator runbook path. |
| `scripts/prepare-hosted-proof.ps1` | Proof/test only, partially reusable | Reuses the supported API, readiness warm-up, duplicate checks, and generated proof state. Proof tenant/user/data setup is not Company 1 provisioning. |
| `scripts/verify-company-readiness.ps1` | Local/self-host rehearsal | Creates a temporary rehearsal tenant and exercises later-phase flows. Not a Phase 3 live Company 1 provisioning command. |
| Starter seed behavior | Development/bootstrap support | Controlled by `synapsecore.starter.*` settings. In production, tenant-onboarding starter inventory/connectors default to disabled. |
| Public workspace creation route | Not supported | Customers do not self-provision. The public experience provides product/contact information and login to an already-provisioned workspace. |
| Direct database manipulation | Unsafe | Not approved for Company 1 provisioning except emergency recovery under a separately approved incident plan. |

## Hosted-Proof Provisioning Trace

Hosted proof tenant creation works through the real backend API:

1. `scripts/prepare-hosted-proof.ps1` checks backend readiness, auth session, websocket/SockJS, and frontend shell.
2. It resolves proof values from parameters, environment variables, or the ignored hosted proof state file.
3. It checks `GET /api/access/tenants` for an existing proof tenant.
4. If the tenant does not exist, it calls `POST /api/access/tenants`.
5. On an empty database it uses `X-Synapse-Bootstrap-Token`.
6. After initial bootstrap it uses `X-Synapse-Platform-Admin-Token`.
7. The backend `AccessController` authorizes the request.
8. `TenantOnboardingService` creates the tenant, bootstrap operators, bootstrap users, and starter warehouses.
9. Hosted proof then creates/reconciles proof-specific users, products, inventory, replay, and scenario state through supported APIs.
10. Playwright reads the ignored proof state file and verifies browser flows.

Reusable for Company 1:

- readiness warm-up discipline
- duplicate tenant lookup through `GET /api/access/tenants`
- supported tenant creation through `POST /api/access/tenants`
- evidence capture of returned identifiers
- fail-fast behavior on authorization or validation errors

Not reusable as Company 1 provisioning:

- proof tenant naming
- proof passwords
- proof product SKU generation
- proof-specific planner and integration admin users
- proof data baseline
- Playwright state file storage

## Public Entry Classification

Supported public routes are the homepage, product, contact, and sign-in. The
customer-facing frontend does not collect provisioning credentials or create a
tenant. New company onboarding is performed through the protected provisioning
operation and followed by customer login.
- does not persist a live backend tenant/workspace
- does not create users in the database
- intentionally states that live tenant creation uses supported backend provisioning paths

Company 1 suitability:

- appropriate as a product-facing explanation of workspace setup
- not appropriate as Company 1 onboarding
- Company 1 users must not be told to self-create the workspace through this route

## Platform Admin Capability

Current platform/admin capabilities relevant to Phase 3:

- list active tenants through `GET /api/access/tenants`
- create tenant workspaces through `POST /api/access/tenants` when platform-admin or bootstrap authorization is valid
- inspect cross-tenant portfolio information in platform surfaces
- view current tenant workspace settings as a tenant admin
- update current tenant workspace name and description as a tenant admin
- update current tenant security settings as a tenant admin
- update current tenant warehouse display metadata as a tenant admin
- update connector support ownership as a tenant admin
- view current tenant users and operators as a tenant admin

Current limitations:

- no separate workspace entity
- no supported tenant delete endpoint
- no supported tenant deactivate endpoint exposed through `AccessController`
- no explicit "pilot status" field on `Tenant`
- no timezone field on `Tenant`
- no supported API to rename/change tenant code after creation

## Approved Phase 2 Inputs

Phase 3 may consume only approved Phase 2 inputs:

- company legal/trading name
- approved workspace display name
- business unit or operating lane
- pilot purpose
- approved operating scope
- primary location
- secondary location when applicable
- approved operator envelope, only as context for later Phase 4
- approved connector scope reference, only as context for later Phase 5
- approved data-domain scope, only as context for later Phase 6
- internal provisioning approval
- risk/precondition notes

Phase 3 must not ask the customer for:

- database ids
- internal tenant primary keys
- environment identifiers
- connector secrets
- customer user passwords
- production API keys

## Identifier Strategy

Tenant code is the operational workspace code.

Current constraints:

- `tenantCode` accepts letters, digits, and hyphens only: `[A-Za-z0-9-]+`
- maximum tenant code length is 64
- backend normalizes tenant code to uppercase
- tenant code must be unique in `tenants.code`
- tenant display name has maximum length 120
- tenant description has maximum length 240
- bootstrap admin username accepts letters, digits, dots, underscores, and hyphens: `[A-Za-z0-9._-]+`
- bootstrap admin username has maximum length 80 and is normalized to lowercase
- backend generated tenant id is not customer-chosen

Recommended Company 1 code rule:

```text
<COMPANY-SHORT-NAME>-PILOT
```

Example only:

```text
ACME-PILOT
```

If the company has multiple business units, include the operating lane:

```text
ACME-DC-PILOT
```

Keep display name human-readable:

```text
Acme Distribution Pilot Workspace
```

Do not hardcode company identifiers in frontend or backend source.

## Official Company 1 Creation Mechanism

The approved Phase 3 creation mechanism is:

```text
Protected backend API: POST /api/access/tenants
Authorization: X-Synapse-Platform-Admin-Token
```

This is chosen because it is the real supported platform-admin tenant onboarding path and does not require direct database edits.

Fallback:

- if the database is empty and no tenant exists, use `X-Synapse-Bootstrap-Token` only for initial platform bootstrap
- if the platform-admin token is missing or invalid, stop and fix secret configuration before creating Company 1
- do not use direct SQL as a normal fallback

## API Contract

Method:

```text
POST
```

Endpoint:

```text
/api/access/tenants
```

Production authorization:

```text
X-Synapse-Platform-Admin-Token: <platform-admin-token-from-secret-manager>
```

First-empty-database bootstrap only:

```text
X-Synapse-Bootstrap-Token: <initial-bootstrap-token-from-secret-manager>
```

Request body:

```json
{
  "tenantCode": "ACME-PILOT",
  "tenantName": "Acme Distribution Pilot Workspace",
  "description": "Controlled pilot workspace for one approved operational lane.",
  "adminFullName": "SynapseCore Provisioning Admin",
  "adminUsername": "acme.provisioning.admin",
  "adminPassword": "<secure-temporary-provisioning-password>",
  "primaryLocation": "Johannesburg",
  "secondaryLocation": "Cape Town"
}
```

Expected response:

```json
{
  "tenantId": 123,
  "tenantCode": "ACME-PILOT",
  "tenantName": "Acme Distribution Pilot Workspace",
  "adminUsername": "acme.provisioning.admin",
  "adminActorName": "Operations Lead",
  "executiveUsername": "acme.pilot.executive",
  "executiveActorName": "Executive Operations Director",
  "starterWarehouseCodes": ["WH-NORTH", "WH-COAST"],
  "createdAt": "2026-08-13T00:00:00Z"
}
```

Common errors:

| Status | Meaning | Action |
| --- | --- | --- |
| 400 | Request validation failed | Correct field length, required fields, or allowed characters. |
| 403 | Token/session authorization failed | Stop; confirm correct environment and platform-admin token. |
| 409 | Tenant code already exists | Stop; investigate duplicate before creating another tenant. |
| 5xx | Backend/DB/runtime failure | Stop; run live connection/recovery checks before retrying. |

## Operator Command Pattern

Use this pattern from a controlled operator shell. Replace placeholders only at runtime. Do not paste secrets into docs or commits.

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore

$backendUrl = "https://synapscore-3.onrender.com"
$platformAdminToken = "<platform-admin-token-from-secret-manager>"

$tenantPayload = @{
  tenantCode = "ACME-PILOT"
  tenantName = "Acme Distribution Pilot Workspace"
  description = "Controlled pilot workspace for one approved operational lane."
  adminFullName = "SynapseCore Provisioning Admin"
  adminUsername = "acme.provisioning.admin"
  adminPassword = "<secure-temporary-provisioning-password>"
  primaryLocation = "Johannesburg"
  secondaryLocation = "Cape Town"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "$backendUrl/api/access/tenants" `
  -Headers @{ "X-Synapse-Platform-Admin-Token" = $platformAdminToken } `
  -ContentType "application/json" `
  -Body $tenantPayload
```

Important current limitation: the current onboarding API requires an initial admin password and creates bootstrap access records. For Phase 3 this password must be a SynapseCore-owned temporary provisioning credential, not a customer password. Phase 4 must replace or rotate access before customer handover.

## Provisioning Sequence

### 1. Pre-check

Action:

- confirm Phase 2 intake decision is `APPROVED FOR PHASE 3 PROVISIONING`
- confirm live/proof/local environment target
- confirm backend readiness before any write
- confirm the intended tenant code and display name
- confirm the platform-admin token source

Expected result:

- operator knows exactly which environment and company will be affected

Failure condition:

- unclear company, unclear environment, missing approval, backend unhealthy, or missing token

Record:

- Phase 2 intake reference
- operator name/role
- environment URL
- proposed tenant code and display name

### 2. Duplicate check

Action:

```powershell
$backendUrl = "https://synapscore-3.onrender.com"
Invoke-RestMethod -Method Get -Uri "$backendUrl/api/access/tenants"
```

Expected result:

- no existing active tenant uses the same tenant code
- no confusing proof/test tenant resembles the company pilot name
- no previous pilot record exists without a decision

Failure condition:

- duplicate code, ambiguous name, or previous abandoned pilot

Record:

- duplicate check timestamp
- list result summary
- decision to continue, stop, or choose corrected tenant code

### 3. Prepare approved values

Action:

- normalize tenant code to the approved format
- prepare display name and description
- prepare primary and secondary location values
- generate a temporary SynapseCore-owned provisioning admin password outside the repository

Expected result:

- all request fields satisfy current backend validation

Failure condition:

- unsupported characters, missing location, too-long name/description, or customer password used

Record:

- request fields except password
- password storage location reference only, never the password

### 4. Execute creation

Action:

- call `POST /api/access/tenants` with platform-admin token

Expected result:

- HTTP 200
- response contains tenant id, tenant code, admin username, executive username, starter warehouse codes, and created timestamp

Failure condition:

- non-2xx response, partial/confusing response, or tenant code mismatch

Record:

- response fields except secrets
- request id if the backend returns one on failure

### 5. Verify workspace

Action:

- sign in as the temporary provisioning admin
- call `GET /api/access/admin/workspace`
- confirm `tenantId`, `tenantCode`, `tenantName`, `active`, security settings, warehouse list, and timestamps

Expected result:

- workspace response matches the created tenant
- starter warehouses are scoped to the tenant

Failure condition:

- sign-in fails, tenant mismatch, missing workspace, or unexpected cross-tenant data

Record:

- workspace response summary
- security settings summary
- starter warehouse codes

### 6. Verify isolation

Action:

- use the signed-in tenant session to inspect tenant-scoped endpoints
- confirm only the new tenant context is visible
- use another known proof/test tenant only for non-destructive negative checks where safe

Expected result:

- Company 1 session cannot view or mutate another tenant's admin workspace, users, products, inventory, orders, replay records, scenarios, or approvals

Failure condition:

- any endpoint leaks another tenant's operational data or allows cross-tenant mutation

Record:

- endpoints checked
- pass/fail classification
- any request ids for failures

### 7. Verify truthful initial state

Action:

- inspect dashboard, workspace admin, users, warehouses, products, inventory, orders, connectors, replay queue, scenarios, approvals, alerts, and recommendations

Expected production result:

- tenant exists
- bootstrap operators/users exist because current onboarding creates them
- two starter warehouses exist
- starter inventory/connectors should be absent unless the environment explicitly enables tenant-onboarding starter seeding
- no unrelated customer data appears

Failure condition:

- unexpected orders, replay records, scenarios, alerts, recommendations, or data from another tenant

Record:

- initial state counts
- whether starter seed flags are enabled in the target environment

### 8. Record evidence

Action:

- create an internal provisioning record from `docs/templates/company-provisioning-record.md`
- store it in the approved internal evidence location

Expected result:

- provisioning evidence is complete and contains no secrets

Failure condition:

- missing identifiers, missing verification, unclear status, or secrets included

Record:

- completed provisioning record
- next authorized phase

## Empty-State Verification

A new live production tenant is not expected to be a completely blank database boundary because current onboarding creates bootstrap access and warehouse records.

Expected bootstrap records:

- tenant row
- Operations Lead operator
- Executive Operations Director operator
- Operations Planner operator
- temporary provisioning admin user
- executive approver user requiring password change
- `WH-NORTH`
- `WH-COAST`
- audit entry for tenant onboarding

Production defaults in `application-prod.yml` disable starter inventory and starter connectors unless explicitly enabled:

- `SYNAPSECORE_STARTER_SEED_INVENTORY_ON_TENANT_ONBOARDING=false`
- `SYNAPSECORE_STARTER_SEED_CONNECTORS_ON_TENANT_ONBOARDING=false`

If products, inventory, or connectors appear unexpectedly, stop and confirm environment settings before Phase 4.

## Cross-Tenant Negative Test

Where safely possible:

1. sign in to Company 1 temporary provisioning admin
2. capture Company 1 scoped endpoint responses
3. sign out
4. sign in to a known non-customer proof/test tenant
5. confirm proof/test tenant data does not include Company 1 data
6. do not mutate proof tenants during this check

Safe checks:

- `GET /api/access/admin/workspace`
- `GET /api/access/admin/users`
- `GET /api/products`
- `GET /api/inventory`
- `GET /api/orders/recent`
- `GET /api/integrations/orders/replay-queue`
- `GET /api/scenarios/history`
- `GET /api/audit/recent`

Do not use direct database comparison as the normal pilot verification path.

## Status and Lifecycle

Current supported lifecycle state:

- `Tenant.active = true/false`

Current limitation:

- the backend has an `active` field but no supported public/admin endpoint for tenant deactivation
- there is no explicit `PILOT`, `TRIAL`, `PRODUCTION`, or `ARCHIVED` tenant status field

Operational tracking:

- pilot status belongs in the Phase 2 intake pack, this provisioning runbook evidence record, pilot records, and release evidence
- do not invent status names in source code or database rows

## Timezone and Regional Settings

Current limitation:

- no tenant timezone field exists in the current `Tenant` model
- primary and secondary locations are stored as warehouse location strings during onboarding

Operational handling:

- record the company timezone and regional assumptions in the provisioning record and Phase 2 intake
- do not create fake timezone settings in source or database

## Pilot Scope Storage

Current source of truth:

- Phase 2 intake pack for approved scope
- provisioning record for exact tenant/workspace identifiers and status
- pilot evidence records for operational use
- tenant description may contain a concise scope summary, but it is not a complete policy store

Do not encode pilot scope in customer-specific code, CSS, route logic, or hardcoded environment behavior.

## Admin Session Safety

Required practices:

- use a SynapseCore-controlled platform-admin token from the approved secret manager
- use a temporary SynapseCore-owned provisioning admin account for initial verification
- sign out after provisioning checks
- do not share platform-admin token with the customer
- do not share temporary provisioning credentials outside the provisioning/support team
- rotate or replace temporary access during Phase 4 before customer handover
- do not record passwords, tokens, cookies, or connector secrets in docs

## Rollback and Correction Before Phase 4

If creation is wrong before any customer users, connectors, or data are added:

| Problem | Supported action today |
| --- | --- |
| Wrong display name or description | Correct through `PUT /api/access/admin/workspace` after signing in as tenant admin. |
| Wrong security settings | Correct through `PUT /api/access/admin/workspace/security`. |
| Wrong warehouse display metadata | Correct through `PUT /api/access/admin/workspace/warehouses/{warehouseId}`. |
| Wrong tenant code | No supported rename path. Stop, document the abandoned tenant, and create a corrected tenant only after approval. |
| Duplicate tenant code | Creation should fail with 409. Stop and investigate. |
| Partial/confusing creation | Stop before Phase 4, capture logs/request ids, and escalate. |
| Need to delete/deactivate tenant | No supported API exposed today. Do not delete manually without an approved incident plan. |

Safer posture:

- correction is preferred when supported
- abandoned tenants must be documented
- direct deletion is not normal operations

## Failure Handling

| Failure | Detect | Stop condition | Recovery | Verify |
| --- | --- | --- | --- | --- |
| Duplicate identifier | 409 response or existing directory entry | Same company/code already exists | Resolve naming and prior pilot record | Re-run duplicate check |
| Invalid field | 400 validation response | Required field, length, or pattern failure | Correct approved values | Re-submit only after review |
| Authorization failure | 403 response | Missing/invalid platform-admin or bootstrap token | Confirm secret and environment | Re-run with valid token |
| DB/backend unavailable | Timeout, 5xx, readiness failure | Backend cannot prove persistence | Use recovery runbooks | Run live connection check |
| Partial creation uncertainty | Response interrupted or mixed logs | Unknown tenant state | Inspect tenant directory and logs | Verify before any retry |
| Stale admin session | 401/403 on workspace admin endpoints | Tenant admin cannot inspect workspace | Sign in again or reset via Phase 4 path | Re-check session endpoint |
| Wrong environment | Unexpected backend URL/profile/tenant directory | Any mismatch | Stop immediately | Confirm environment before writes |

Never continue to Phase 4 after uncertain tenant creation.

## Environment Safety

Before any live company provisioning, explicitly confirm:

- backend URL
- frontend URL if checking browser sign-in
- active profile/environment from runtime/ops evidence where available
- intended company
- intended tenant code
- intended display name
- operator identity
- platform-admin token source
- Phase 2 approval status

Environment classes:

- local: safe for rehearsal only
- staging/proof: safe for proof/test tenants only
- live pilot: only for approved pilot companies

Do not create a live company tenant while pointing at local or proof URLs, and do not create proof tenants while pointing at the live pilot evidence record.

## Pre-Provisioned vs Hardcoded

Allowed:

- pre-created tenant DB record through supported API
- pre-created workspace boundary through tenant onboarding
- pre-assigned tenant scope
- configured company metadata
- operator-managed provisioning
- approved company-specific evidence records

Not allowed:

- source-coded customer passwords
- source-coded API keys
- source-coded database credentials
- source-coded OAuth secrets
- customer-specific frontend logic
- customer-specific backend branches
- customer-specific CSS or route files
- manual production SQL as a normal onboarding path

## Company Readiness Script Applicability

`scripts/verify-company-readiness.ps1` is not a Phase 3 live provisioning script.

It currently performs a local/self-host readiness rehearsal using seed users. It creates a temporary tenant, then exercises many later-phase flows:

- workspace admin update
- security settings update
- operator/user creation
- connector creation
- webhook ingestion
- inventory pressure
- replay recovery
- scenario approval/execution
- runtime/audit/dashboard checks

Use after Phase 3 only as a rehearsal reference, not as Company 1 live provisioning evidence. Its full checks make more sense after later phases because Company 1 users, connectors, and data do not exist yet.

## Phase 3 Verification Gate

Phase 4 is not authorized until all of these are true:

- Phase 2 intake is approved
- correct environment is confirmed
- duplicate check passed
- tenant created through supported API
- workspace boundary confirmed through `GET /api/access/admin/workspace`
- tenant id/code/name recorded
- starter warehouse codes recorded
- current onboarding-created bootstrap users/operators understood
- tenant isolation verified
- empty/initial state verified honestly
- no unexpected business data appears
- no cross-tenant leakage observed
- no secrets stored in docs/source
- rollback/correction posture understood
- provisioning evidence record completed

## Phase 4 Handoff

Phase 4 receives:

- tenant id
- tenant code
- workspace display name
- environment
- approved Phase 2 intake reference
- approved user list from Phase 2
- approved responsibilities
- role-mapping inputs
- tenant/workspace verification result
- initial state counts
- known limitations
- Phase 3 approval/verdict

Phase 4 must not assume:

- customer users already exist
- connector credentials exist
- customer products/inventory/orders have been loaded
- tenant code can be changed later

## Phase 3 Verdict Options

Use one of these only:

- `COMPANY PILOT PHASE 3 ACCEPTED`
- `COMPANY PILOT PHASE 3 ACCEPTED WITH DOCUMENTED LIMITATION`
- `COMPANY PILOT PHASE 3 NOT ACCEPTED - SAFE PROVISIONING PATH INCOMPLETE`

Given the current implementation, the expected Phase 3 posture is usually:

```text
COMPANY PILOT PHASE 3 ACCEPTED WITH DOCUMENTED LIMITATION
```

Reason: a safe supported API path exists, but tenant onboarding currently creates bootstrap users/operators and starter warehouses as part of tenant creation. Phase 4 must reconcile access before customer handover.
