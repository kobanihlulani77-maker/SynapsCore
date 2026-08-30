# SynapseCore Domain 13 Phase 2 Evidence

## Workspace, Warehouse, and Security-Policy Lifecycle

**Status:** `TENANT / WORKSPACE ADMIN LIFECYCLE VERIFIED AND OPERATIONALLY COMPLETE FOR CONTROLLED B2B PILOT - OWNER LIVE WALKTHROUGH DEFERRED`

**Starting HEAD:** `5ca036d26f7090c74c3645d0e042c8314c8b44af`

**Previous phase:** Domain 13 Phase 1, commit `72a8c1efcbc5f89bdafff0f26abf21dae910cad9`, remains closed.

**Scope:** This phase hardens configuration evolution after onboarding. It does not reopen the catalog, inventory, orders, fulfillment, integrations, replay, alerts, recommendations, dashboard, auth, or scenario lifecycle programs. Existing domain behavior is protected by the full regression suite.

## Evidence Summary

- Workspace profile updates are tenant-scoped and version-checked.
- Tenant ID and tenant code remain structural and immutable; the display name and description are editable.
- Security settings preserve the supported bounds of 7-365 password-rotation days and 15-1440 session-timeout minutes.
- Tenant workspace, warehouse metadata, connector support, and security updates use optimistic version checks.
- A Tenant Admin can add a real warehouse after onboarding without creating business data, users, connectors, or fake activity.
- Warehouse codes are normalized and remain immutable after creation.
- Warehouses use an active/retired lifecycle. Retirement preserves history and blocks unsafe current dependencies.
- Retired warehouses are excluded from operational warehouse lists and rejected by representative inventory and scenario processing seams.
- Active required-role coverage and readiness are evaluated against active warehouses only.
- Scoped operators cannot be widened by a warehouse being added or retired; the final scoped warehouse cannot silently become tenant-wide authority.
- Connector support ownership remains tenant-scoped metadata and does not grant integration roles or expose connector secrets.
- Successful administrative mutations are audited; rejected mutations do not receive success audit entries.
- The frontend reads the persisted response after mutations and submits the returned versions for subsequent edits.

## Final Workspace-Admin Contract

The supported administrative surface is:

| Area | Contract |
|---|---|
| Workspace | Update current tenant name and description only. |
| Identity | Tenant ID and tenant code are stable keys and are not renameable here. |
| Security | Update bounded password rotation, bounded session timeout, and optional session invalidation. |
| Warehouse create | Add a tenant-scoped code, name, and location; create configuration only. |
| Warehouse metadata | Update name and location with version protection; code remains stable. |
| Warehouse lifecycle | Retire or reactivate with version protection and current-dependency checks. |
| Connector support | Update support owner and supported connector policy only; no ingestion lifecycle changes. |
| Readiness | Report configuration readiness with exact reasons; readiness does not require business data. |

## 71-Point Closure Record

1. **Starting HEAD:** `5ca036d26f7090c74c3645d0e042c8314c8b44af` was the reviewed starting revision.
2. **Final workspace-admin contract:** Implemented through the existing tenant-admin controller, service, DTO, repository, and Company Settings seams.
3. **Workspace profile result:** Tenant Admin can update only the current tenant name and description; successful state is read back.
4. **Stable tenant identity result:** Tenant ID and tenant code are not accepted as update fields and remain unchanged.
5. **Workspace concurrency result:** Tenant `@Version` rejects missing or stale workspace versions with controlled HTTP 409 conflict.
6. **Security-policy contract:** Password rotation days, session timeout minutes, and the existing invalidate-other-sessions flag remain the supported fields.
7. **Validation result:** Existing DTO bounds enforce 7-365 days and 15-1440 minutes; malformed or out-of-range values are rejected.
8. **Security-policy concurrency:** Security updates require the current tenant version and cannot silently overwrite a newer policy.
9. **Policy-version result:** The existing security policy version continues to increment when other sessions are invalidated; the tenant version tracks configuration writes.
10. **Session-policy handoff:** Existing auth/session revocation behavior remains covered by the full regression suite; no Auth contract was changed.
11. **Warehouse lifecycle model:** Warehouses now have `active` state and JPA version metadata.
12. **Warehouse count/limit result:** No fixed count or North/Coast template is enforced; creation is data-driven and tenant-scoped.
13. **Post-onboarding warehouse creation:** `POST /api/access/admin/workspace/warehouses` creates a real warehouse with explicit code, name, and location.
14. **Warehouse create authority:** The route remains behind the existing Tenant Admin boundary and current tenant context.
15. **Warehouse code stability:** Codes are normalized to uppercase on create and are not accepted by the metadata-update DTO.
16. **Duplicate/concurrent creation:** Tenant-scoped uniqueness is checked before insert and the database uniqueness race is normalized to HTTP 409 after `saveAndFlush`.
17. **New-warehouse authority result:** Tenant-wide empty-scope semantics remain explicit; scoped operators do not gain a new warehouse automatically.
18. **New-warehouse requiredRoles result:** Required-role coverage and readiness inspect active warehouses, so an uncovered new active warehouse reports an exact not-ready reason rather than fabricated coverage.
19. **Warehouse lifecycle status:** Existing warehouses default to active; retirement is represented without physical deletion.
20. **Retirement result:** Tenant Admin can retire an eligible warehouse and later reactivate it using the current version.
21. **Retirement preconditions:** The service checks last-active-warehouse, scoped-operator, enabled-connector, inventory, order, fulfillment, and pending-governed-scenario dependencies.
22. **Active-dependency result:** Current operational dependencies produce controlled HTTP 409 responses and are not silently detached.
23. **Scope-on-retirement safety:** Retirement is blocked while active scoped operators reference the warehouse; no scope is removed automatically.
24. **Final-scoped-warehouse result:** The implementation never converts removal of a final scoped warehouse into an empty scope, because empty scope means tenant-wide authority.
25. **Required governance after retirement:** Required-role coverage ignores retired warehouses and continues to evaluate every active warehouse.
26. **Retired-warehouse new-work result:** Operational warehouse listing excludes retired records; representative inventory and scenario processing reject inactive warehouses.
27. **History preservation:** Retirement updates warehouse state only; it does not delete orders, inventory, fulfillment, alerts, recommendations, audit, or scenario evidence.
28. **Reactivation result:** Reactivation is supported as a reversible, version-checked lifecycle operation.
29. **Warehouse metadata update:** Name and location remain editable; the response returns active state and version.
30. **Warehouse concurrency result:** Stale metadata and lifecycle versions return controlled 409 conflicts.
31. **Frontend warehouse admin:** Company Settings can view actual warehouses, create one, edit metadata, and retire/reactivate with persisted readback.
32. **Template-warehouse result:** The frontend renders the API warehouse list and does not manufacture universal warehouse records.
33. **Fresh-tenant warehouse result:** The Phase 2 test creates an additional warehouse with no product, inventory, order, fulfillment, alert, recommendation, operator, or connector side effects.
34. **Workspace/settings readback:** Workspace bootstrap refetches persisted workspace, security, warehouse, and connector data after mutations.
35. **Conflict UX:** Version conflicts surface the backend message and stop the mutation; the frontend refresh path avoids blind stale retries.
36. **Connector support result:** Tenant Admin connector support updates remain tenant-scoped and preserve supported sync-mode validation.
37. **Connector support concurrency:** Connector support DTOs carry connector version and stale support writes return 409.
38. **Connector/role separation:** Support-owner metadata resolves an existing active operator and does not grant roles or alter operator authority.
39. **External-health wording result:** Workspace support state does not claim external source reconciliation or connector reachability; existing connector health boundaries remain intact.
40. **Ongoing readiness model:** Readiness checks tenant activity, an active usable Tenant Admin, at least one active warehouse, and required-role coverage across active warehouses.
41. **Ready-vs-populated result:** Business data is not required for administrative readiness; an empty but correctly configured tenant can be ready.
42. **Not-ready result:** Missing active warehouses, Tenant Admin, or required-role coverage produces explicit reasons rather than auto-created authority or fake data.
43. **Final-active-warehouse result:** The last active warehouse cannot be retired; the controlled contract requires at least one active location.
44. **Tenant-active-state boundary:** Tenant Admin has no tenant-suspension control; platform/control-plane state remains outside this surface.
45. **Audit result:** Workspace update, security update, warehouse create/update/retire/reactivate, and connector support update success paths record success audit events.
46. **Retry/duplicate result:** Duplicate warehouse codes conflict, stale version writes conflict, and repeated retirement is stable and does not delete data.
47. **Migrations:** `V13__workspace_warehouse_lifecycle_safety.java` adds tenant, warehouse, and connector versions plus warehouse active state; existing migration history was not edited.
48. **Existing-data migration:** Existing database warehouses default to active through the migration, preserving all existing operational records.
49. **Auth seam:** The security update remains within the existing auth policy/session contract; prior auth revocation tests remain green.
50. **Warehouse-authority seam:** Active warehouse scope validation filters retired warehouses and preserves explicit tenant-wide versus scoped semantics.
51. **Governance seam:** Required roles are checked per active warehouse; retirement removes only the retired location from future coverage evaluation.
52. **History seam:** Retirement is a state transition, not destructive deletion; historical foreign-key references remain available.
53. **Performance result:** Workspace read work remains bounded to the existing tenant-scoped collections and focused dependency checks; no speculative broad optimization was added.
54. **Failure matrix:** Invalid input, stale versions, duplicate code, active dependencies, last-warehouse retirement, scoped operators, inactive warehouse use, and non-admin boundaries are covered by service/controller behavior and regression assertions.
55. **Hosted proof:** No new hosted proof was run for this repository-only closure. Existing hosted proof remains the deployment baseline; owner/live mutation walkthrough is explicitly deferred rather than represented as current evidence.
56. **Production defects:** The implementation defects found in this phase were missing lifecycle/version seams and validation-before-mutation ordering; both were corrected.
57. **Frontend defects:** No frontend runtime defect was found after wiring the persisted version/create/lifecycle contract; lint, build, and verify passed.
58. **Fixes:** Added migration/entity state, DTOs, endpoints, readiness, retirement guards, active-warehouse enforcement, frontend forms/actions, compatibility test versions, and validation ordering.
59. **A/B/C/D table:** See the classification below.
60. **Focused tests:** `TenantWorkspaceAdminPhase2IntegrationTest` passed 2 tests; the focused compatibility run passed 92 tests total with 0 failures and 0 errors.
61. **Full backend:** Full Maven regression passed 263 tests, 0 failures, 0 errors, and 0 skipped tests.
62. **Frontend checks:** `npm.cmd run lint`, `npm.cmd run build`, and `npm.cmd run verify` passed; the frontend check inspected 71 source files.
63. **Flyway/docs/diff checks:** Flyway validated 13 migrations in test contexts; documentation link check passed for 778 local links; `git diff --check` is required before commit and recorded at closure.
64. **Files changed:** Backend lifecycle entities, DTOs, repositories, services, controller, migration, baseline, compatibility test; frontend workspace model/hooks/Settings; and this evidence file.
65. **Commits:** Phase 1 remains `72a8c1efcbc5f89bdafff0f26abf21dae910cad9`; Phase 2 commit is created after final diff checks.
66. **Critical blockers:** 0 identified in the reviewed implementation.
67. **High blockers:** 0 identified in the reviewed implementation.
68. **Classification A remaining:** 0; required administration and authority capabilities are implemented locally and covered by regression evidence.
69. **Owner/live evidence status:** Owner/live walkthrough and provider-specific Postgres migration confirmation are deferred evidence items, not product-authority gaps.
70. **Domain 13 final readiness:** Ready for the controlled B2B pilot boundary, subject to normal deployment migration observation and the deferred owner walkthrough.
71. **Phase 2 verdict:** `TENANT / WORKSPACE ADMIN LIFECYCLE VERIFIED AND OPERATIONALLY COMPLETE FOR CONTROLLED B2B PILOT - OWNER LIVE WALKTHROUGH DEFERRED`.

## A/B/C/D Classification

| Class | Result |
|---|---|
| A - required operational or authority capability | **0 remaining.** Tenant-scoped workspace editing, version protection, real warehouse growth, lifecycle safety, readiness, active-work enforcement, and scope-preserving authority are implemented. |
| B - intentional current boundary | No physical warehouse deletion, immutable codes, no tenant merge/code rename, no tenant-admin suspension, no external-health claim, and no automatic company-structure synchronization. |
| C - evidence gap | Owner/live walkthrough, concurrent provider-level create race, and confirmation of the Postgres-specific migration branch after deployment. These do not justify claiming hosted proof for this revision. |
| D - future extension | Bulk warehouse import, hierarchy/groups, delegated configuration admins, directory synchronization, and configuration-change approvals remain future work. |

## Files and Verification

Intended Phase 2 files are the modified/new backend workspace-administration implementation and tests, the frontend workspace administration wiring, the V13 migration and schema baseline, and this evidence file. The pre-existing unrelated files `frontend/Dockerfile`, `.gitattributes`, `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`, and `docs/evidence/scenario-lifecycle-phase-10-source-observation.md` remain untouched and must not be included in the Phase 2 commit.

Verification completed:

```text
Focused backend: 92 tests, 0 failures, 0 errors
Full backend: 263 tests, 0 failures, 0 errors, 0 skipped
Frontend lint: passed
Frontend build: passed
Frontend verify: passed
Flyway test validation: 13 migrations validated/applied
Documentation links: 778 checked, none missing
```

No hosted proof was run during this closure because the requested Phase 2 evidence is repository-focused and the owner/live walkthrough is explicitly deferred.
