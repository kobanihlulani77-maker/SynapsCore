# Tenant / Workspace Administration Phase 1 Evidence

## Scope

This evidence closes Domain 13, Phase 1: Identity, Role, Scope, and Governance Authority Lifecycle. The change is limited to tenant administration contracts, persistence, authority safety, concurrency protection, migration support, and the corresponding verification fixtures. No product workflow, frontend route, or backend business-domain API was expanded.

## Implemented contract

### Identity lifecycle

- Tenant Admin can create, update, activate, deactivate, and reset passwords for tenant users through the existing access administration APIs.
- Users remain linked to an operator identity. Creating a user against an inactive or missing operator is refused.
- User and operator responses now expose a persistence version so an administration screen can submit the version it read.
- Password reset remains a targeted session-invalidating operation. It does not change roles or warehouse authority.

### Role lifecycle

- The supported role set remains the existing six roles: `TENANT_ADMIN`, `REVIEW_OWNER`, `FINAL_APPROVER`, `ESCALATION_OWNER`, `INTEGRATION_ADMIN`, and `INTEGRATION_OPERATOR`.
- Unsupported roles, including `PLATFORM_OWNER`, are rejected by the tenant administration API.
- Required tenant roles are persisted in `tenant_required_roles` and are initialized from the tenant onboarding request instead of being a transient onboarding-only check.

### Scope lifecycle

- Tenant-wide authority is represented by an explicit `tenantWide=true` request and an empty persisted warehouse-scope collection.
- A scoped operator must provide one or more valid tenant warehouse codes.
- A create request that omits both `tenantWide` and `warehouseScopes` is rejected; it cannot silently become tenant-wide.
- A tenant-wide request cannot also contain non-blank warehouse scopes.
- An update that omits both scope fields preserves the current scope for compatibility with role-only edits.
- An update that intentionally changes scope must explicitly choose tenant-wide or provide valid warehouse codes.
- Null, blank, unknown, and cross-tenant warehouse scope values are rejected.

### Governance-holder safety

When a tenant declares a required role, operator and user mutations are checked against every tenant warehouse. A mutation is rejected if it would leave a warehouse without an active user attached to an active operator holding the required role, either scoped to that warehouse or tenant-wide. This prevents the last usable governance holder from being silently removed.

Tenant Admin safety remains enforced separately: the last active tenant-admin operator and the last usable tenant-admin sign-in lane cannot be removed.

### Concurrency safety

`AccessOperator` and `AccessUser` now use JPA optimistic versioning. Administrative updates must submit the version read by the caller. A missing or stale version returns HTTP 409 and does not apply the stale role, identity, active-state, or scope mutation. This prevents a stale administration screen from resurrecting revoked authority.

## Persistence and migration

- `V12__tenant_admin_authority_safety.java` adds non-null version columns to access operators and users.
- The same migration creates `tenant_required_roles` with a tenant foreign key and a supported-role constraint.
- The full-schema support baseline is aligned with the V12 structure.
- Flyway validation and application of all 12 migrations were observed during the full backend test run.

## Verification evidence

### Focused Phase 1 test

`TenantWorkspaceAdminPhase1IntegrationTest` passed:

- ambiguous operator creation is rejected;
- explicit scoped creation succeeds and returns a version;
- role-only update preserves existing scope;
- removal of the last required `REVIEW_OWNER` holder is rejected;
- stale operator update returns HTTP 409 rather than restoring the previous scope;
- unsupported roles are rejected;
- inactive operators cannot receive new user accounts.

Result: **4 tests, 0 failures, 0 errors, 0 skipped**.

### Full backend regression suite

The full Maven backend suite passed after updating two existing scenario fixtures to retain the version returned by `save()` during cleanup. The fixture correction is test-only and addresses detached-entity reuse after optimistic version advancement.

Result: **261 tests, 0 failures, 0 errors, 0 skipped**.

The suite includes the existing catalog, inventory, orders, fulfillment, integrations, replay, alerts, recommendations, dashboard, auth, security, realtime, platform-boundary, and migration tests. No existing authority or operational boundary test was weakened.

## Frontend administration compatibility

The existing workspace administration model now carries operator and user versions. Operator updates send explicit scope intent and the current operator version; user updates send the current user version. The existing UI route and API shapes remain in place, with additive request/response fields only. No frontend behavior outside workspace administration was changed.

## Classification

### A. Release blockers

None identified in the Phase 1 scope after the focused and full backend results above.

### B. High-priority limitations

- This phase protects mutations through the existing tenant administration API; it does not introduce a separate policy-management UI for editing `requiredRoles`.
- Optimistic versioning is applied to user and operator administrative updates. Password reset is intentionally a targeted credential/session operation and remains outside the stale role/scope update contract.
- Production deployment and live hosted evidence are separate gates. This local result does not claim that the new V12 migration has been applied to Render until the changed backend is deployed and its logs/readiness are checked.

### C. Medium-priority follow-up

- Add a direct API/admin-surface workflow for reviewing and changing persisted required-role policy if pilot operations require policy changes after onboarding.
- Add a dedicated end-to-end browser assertion for conflict feedback when two admin screens edit the same identity concurrently.

### D. Future evolution

- Extend versioned mutation handling to any future authority-bearing resource introduced outside the current user/operator boundary.
- Add richer audit correlation for rejected stale writes and policy-coverage failures if operational reporting requires it.

## Final status

For the repository and local verification scope, Domain 13 Phase 1 identity, authority, scope, governance-holder, and stale-update protections are implemented and verified. Phase 2 was not started. Hosted proof was not rerun because this phase did not request a deployment/live gate.

**TENANT / WORKSPACE ADMIN PHASE 1 VERIFIED - IDENTITY, AUTHORITY AND GOVERNANCE ADMINISTRATION COMPLETE FOR CONTROLLED PILOT**
