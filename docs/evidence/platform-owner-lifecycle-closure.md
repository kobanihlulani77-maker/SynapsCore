# Platform Owner Lifecycle Closure

## Scope

This evidence record covers the bounded Domain 18 Platform Owner closure cycle
started from `0901b468d9ad4a55b2568ca0528da4a16309ca2d`. It does not reopen any
closed domain and does not begin Integrated Layer 2 validation.

The closure addresses two required Layer-1 seams:

1. Align the Platform Owner provisioning surface with the explicit controlled
   production provisioning contract.
2. Ensure a cookie-backed high-impact provisioning request cannot be accepted
   from an untrusted browser origin.

## Final Platform Owner Contract

Platform Owner is the SynapseCore control-plane identity. It is not a tenant
role, Tenant Admin, impersonation mechanism, or raw tenant-operations browser.
Its current scope is platform identity/session, metadata-first tenant portfolio,
controlled company provisioning, platform runtime/release trust, support
metadata, and platform audit/accountability.

Platform and tenant authority remain mutually exclusive. A Platform Owner
session does not grant tenant business access, and a tenant session does not
grant `/api/platform/*` or global provisioning authority.

## Provisioning UI Alignment

The previous Platform Owner form exposed a legacy company/admin/location shape
but constructed synthetic `WH-NORTH`, `WH-COAST`, Operations Lead, Executive
Operations Director, and role assignments in the request. That did not match
controlled production provisioning and was corrected.

The supported UI now collects and submits:

- tenant code, tenant name, and optional description
- one or more explicit warehouses with code, name, and location
- one or more explicit users with username, full name, actor identity,
  optional display/description, initial password, roles, and warehouse scopes
- an explicit required-role policy, including an explicit no-mandatory-role
  choice

The UI maps the entered values to the existing `TenantOnboardingRequest`
transport and preserves the backend's final validation. It does not synthesize
personas, warehouses, starter operational data, or hidden administrators.

Success language now says provisioning/configuration is complete and explicitly
does not imply that products, inventory, integrations, or source-system data
are operationally ready. The result view derives created identities and scopes
from the response and does not echo passwords.

## Backend and Security Boundary

Controlled production remains explicit through `application-prod.yml`:

- explicit tenant provisioning is enabled
- starter inventory is disabled
- starter connectors are disabled
- compatibility tenant fallback is disabled
- tenant-admin onboarding fallback is disabled

The existing onboarding service validates nonempty explicit configuration,
warehouse uniqueness, user uniqueness, role values, scope references,
Tenant Admin coverage, required-role coverage, and duplicate tenant codes.

The cookie-backed Platform Owner branch of `POST /api/access/tenants` now also
requires a trusted application origin when an `Origin` header is present. The
existing CORS filter rejects an untrusted origin before controller execution;
the controller contains a matching trusted-origin guard as a defense-in-depth
check. Token/bootstrap provisioning paths are unchanged and do not rely on a
browser cookie.

This is not a claim that every browser threat model is exhaustively tested in
production. The local boundary test proves the hostile-origin request is
rejected and creates no tenant. A same-site/trusted-origin browser walkthrough
and a live non-destructive owner walkthrough remain evidence work, not a reason
to weaken the boundary.

## Provisioning and Data Results

The request model is `TenantOnboardingRequest` with explicit
`TenantWarehouseProvisioningRequest`, `TenantUserProvisioningRequest`, and
`requiredRoles` values. The UI requires a password of at least eight
characters for each supplied user; the backend remains authoritative.

The production contract creates exactly the requested warehouse count and only
the requested users, roles, and scopes. WH-NORTH and WH-COAST are acceptance
fixture examples, not defaults. No synthetic operational records are implied
by a successful configuration transaction.

Temporary credential handling remains response-only when the backend generates
one: hashes are persisted, a temporary value is not logged or audited, and the
frontend does not put it into persistent browser storage. The current UI takes
explicit initial passwords instead, so it does not need to display a generated
credential for the normal path.

## Evidence Matrix

| Area | Result | Evidence |
| --- | --- | --- |
| Platform identity/session separation | PASS locally | `PlatformTenantAccessBoundaryIntegrationTest` covers dedicated platform session, replacement, logout, and tenant/platform authority separation. |
| Tenant users denied platform control plane | PASS locally | Platform boundary integration coverage for representative tenant roles and `/api/platform/*`. |
| Platform Owner denied tenant business APIs without tenant login | PASS locally | Platform boundary integration coverage for tenant business endpoints. |
| Header tampering | PASS locally | Platform boundary integration coverage rejects authority changes from tenant/platform headers. |
| Metadata-first privacy | PASS locally | Platform boundary responses and privacy assertions exclude raw tenant records and secret fields. |
| Explicit production provisioning | PASS by configuration and tests | `application-prod.yml` plus `Domain11ControlledProvisioningIntegrationTest`. |
| Exact users, roles, scopes, and warehouses | PASS locally | Controlled provisioning integration tests and the explicit UI request mapping. |
| Required-role failure | PASS locally | Controlled provisioning integration coverage verifies missing required coverage fails. |
| Duplicate tenant code | PASS locally | `duplicateTenantCodeReturnsConflict` in `Domain11ControlledProvisioningIntegrationTest`. |
| Late provisioning rollback | PASS locally | `TenantOnboardingRollbackIntegrationTest` injects a late connector failure and verifies tenant, warehouse, operator, user, and connector rows are absent. |
| Cookie provisioning from hostile origin | PASS locally | `cookieBackedPlatformProvisioningRequiresTrustedOriginWhenOriginIsProvided` returns 403 and verifies no tenant was persisted. |
| Trusted-origin cookie provisioning success | C deferred | No separate post-change browser/MockMvc success case was added because existing fixture counts are sensitive; the token/bootstrap paths and existing UI contract remain intact. |
| Concurrent duplicate provisioning | C deferred | Duplicate uniqueness is covered; no separate concurrent request race proof was run in this cycle. |
| Exact platform session-expiry timing | C deferred | Existing session and logout boundaries are covered; no timing-based production expiry rehearsal was run. |
| Hosted Platform Owner walkthrough | C deferred | No private owner credential was available to this shell; no credentials were invented or printed. |

## Audit and Runtime Truth

Existing platform boundary tests cover platform login/logout audit behavior and
metadata-first activity/runtime responses. Successful provisioning continues to
use the Platform Owner/control-plane actor context; password and token values
are not part of the audit contract.

The current design intentionally does not add tenant deletion, tenant
suspension, impersonation, platform RBAC, MFA/SSO, platform realtime, or
advanced portfolio operations in this closure. These remain B or D boundaries,
not missing fixes for this cycle.

## Verification

Local verification completed for this cycle:

- focused origin-boundary test: 1 test, 0 failures, 0 errors
- rollback test: 1 test, 0 failures, 0 errors
- full `PlatformTenantAccessBoundaryIntegrationTest`: 34 tests, 0 failures, 0 errors
- full backend suite: 270 tests, 0 failures, 0 errors
- frontend `npm.cmd run verify`: passed, including build and launch-readiness checks

The full backend run completed with Maven `BUILD SUCCESS`. Test logs include
expected warning/error lines from deliberate failure, authorization, duplicate,
and replay fixtures; they did not produce test failures.

Hosted proof was not claimed as a result of this local cycle. The safe next
hosted step is an unauthenticated/live health check after deployment. A
Platform Owner walkthrough requires the private credential path and must remain
non-destructive; it is not represented as completed here.

## Classification

### Classification A: required and resolved

- Platform Owner UI now matches explicit controlled provisioning.
- Cookie-backed provisioning rejects an untrusted origin before mutation.

### Classification B: intentional current boundary

- one environment-managed Platform Owner
- no MFA/SSO or platform RBAC
- no impersonation
- metadata-first support surface
- no tenant delete/suspend lifecycle
- manual/reload platform refresh
- restart/deploy-based credential rotation

### Classification C: evidence deferred

- trusted-origin cookie provisioning success walkthrough
- concurrent duplicate race proof
- exact hosted session-expiry timing
- hosted destructive provisioning/rollback
- live Platform Owner browser walkthrough

### Classification D: future evolution

- multiple Platform Owners and support roles
- MFA/SSO/OIDC
- audited impersonation, if ever approved
- tenant suspension lifecycle
- advanced tenant search/fleet analytics
- SIEM integration
- horizontally scaled control-plane operations

## Final Assessment

Critical blockers: **0**

High blockers: **0**

Classification A remaining: **0**

The bounded Layer-1 Platform Owner work is locally verified and operationally
complete for controlled B2B pilot scope, with the C evidence deferrals above.
Integrated Layer 2 validation is not started by this record.
