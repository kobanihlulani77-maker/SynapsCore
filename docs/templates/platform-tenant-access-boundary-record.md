# Platform And Tenant Access Boundary Record

Completed live access-gate record. Do not include passwords, hashes, session cookies, bootstrap tokens, platform automation tokens, connector credentials, or customer payloads.

## Record Identity

| Field | Value |
| --- | --- |
| Review date | 2026-08-22 |
| Reviewer | SynapseCore access-gate engineering review |
| Release/RC | Company 1 pre-provisioning access gate |
| Release commit | `ee821d8` corrective build |
| Environment | Local integration proof and hosted Render rehearsal |
| Result | Accepted with documented limitations, pending gate-owner review |
| Render status | Available; frontend, backend, DB readiness, auth, and websocket checks healthy |

## Platform Owner Authority

| Field | Evidence |
| --- | --- |
| Human auth method | Dedicated username/password endpoint with BCrypt verification and server-side platform session |
| Session timeout | Bounded server-side inactivity timeout configured by platform-owner settings |
| Platform routes | Platform Overview, Tenant Directory, Platform Runtime, Platform Activity, Release Trust |
| Protected platform APIs | `/api/platform/overview`, `/api/platform/tenants`, `/api/platform/runtime`, `/api/platform/activity` |
| Login/logout audit evidence | Live login and logout completed; signed-out APIs denied |
| Tenant session denied platform API | All six tenant roles denied |
| Automation tokens absent from browser | Source check and rendered flow confirmed |
| Deployment credential configuration verified | Live platform-owner login succeeded; no secret value recorded |

## Platform Data Classification

| Category | Returned | Intentionally excluded | Evidence |
| --- | --- | --- | --- |
| Platform health | Yes | Raw tenant payload | Live platform overview/runtime |
| Tenant summary metadata | Yes | Tenant business rows | Live tenant directory |
| Platform activity metadata | Yes | Raw event details and payload summaries | Live platform activity |
| Release/runtime identity | Yes | Origins, secrets, tenant payload | Live Release Trust and Platform Runtime |
| Raw tenant orders/inventory | No | Intentionally excluded | Platform response/privacy inspection |
| Raw inbound/replay payloads | No | Intentionally excluded | Platform response/privacy inspection |
| Credentials/secrets | No | Intentionally excluded | Source and live response inspection |

## Tenant Role Matrix

| Role | Expected pages | Read checks | Write checks | Expected denials | Result |
| --- | --- | --- | --- | --- | --- |
| `TENANT_ADMIN` | General workspace, Users, Company Settings | Tenant-wide operational reads | Tenant administration and product/settings writes | Integrations/replay, governance-only actions, platform, scenario execution | PASS |
| `REVIEW_OWNER` | General workspace, Approvals | Scoped operational/governance reads | Assigned review and eligible scenario execution | Integrations, Users, Escalations, platform | PASS |
| `FINAL_APPROVER` | General workspace, Approvals | Scoped operational/governance reads | Assigned final approval and eligible scenario execution | Integrations, Users, Escalations, platform | PASS |
| `ESCALATION_OWNER` | General workspace, Escalations | Scoped operational/escalation reads | Assigned escalation acknowledgement | Approvals, Integrations, Users, platform, scenario execution | PASS |
| `INTEGRATION_ADMIN` | General workspace, Integrations, Replay Queue | Connector/import/replay reads | Connector policy and replay actions | Users, governance, platform, scenario execution | PASS |
| `INTEGRATION_OPERATOR` | General workspace, Integrations, Replay Queue | Connector/import/replay reads | Eligible replay actions | Connector policy, Users, governance, platform, scenario execution | PASS |

## Integration And Replay Read Matrix

| Role | Connectors | Import history | Replay queue | Purpose/justification | Result |
| --- | --- | --- | --- | --- | --- |
| `TENANT_ADMIN` | Denied full endpoint | Denied | Denied | Dedicated workspace support metadata only | PASS |
| `REVIEW_OWNER` | Denied | Denied | Denied | Governance evidence comes from scenario/approval APIs | PASS |
| `FINAL_APPROVER` | Denied | Denied | Denied | Governance evidence comes from scenario/approval APIs | PASS |
| `ESCALATION_OWNER` | Denied | Denied | Denied | Assigned evidence comes from escalation/scenario APIs | PASS |
| `INTEGRATION_ADMIN` | Allowed | Allowed | Allowed | Connector administration and recovery ownership | PASS |
| `INTEGRATION_OPERATOR` | Allowed read | Allowed | Allowed | Operational monitoring and replay | PASS |

## Realtime Subscription Boundary

| Check | Result/evidence |
| --- | --- |
| Cross-tenant topic denied | PASS |
| Non-integration role denied raw integration topic | PASS |
| Warehouse-scoped user denied tenant-wide raw operational topics | PASS |
| Scoped integration user receives metadata-only change signal | PASS |
| Filtered REST refresh used after change signal | PASS |
| Tenant-wide integration role allowed authorized raw integration topic | PASS; temporary scope was restored |

## Warehouse Scope

| Field | Evidence |
| --- | --- |
| Scoped tenant | Synthetic rehearsal tenant only |
| Scoped operator | Five single-role non-admin operators |
| Allowed warehouse | `WH-COAST` |
| Denied warehouse | `WH-NORTH` for scoped roles |
| Empty-scope semantics | Tenant-wide; exercised by `TENANT_ADMIN` and treated as high impact |
| Inventory read/write result | Allowed lane visible; wrong warehouse denied |
| Order read/transition result | Allowed lane visible; wrong warehouse denied |
| Fulfillment read/update result | Allowed lane visible; wrong warehouse denied |
| Scenario read/execute result | Scoped reads; execution limited to Review Owner/Final Approver; wrong role/warehouse denied |
| Replay read/action result | Integration roles only; scoped recovery proven through supported CSV lane |

## Session Revocation And Access Changes

| Check | Result/evidence |
| --- | --- |
| Disabled user loses session authority | PASS |
| Protected request after disable is denied | PASS |
| User remap invalidates the prior session | PASS through session-version behavior |
| Operator role/scope change affects existing session validation | PASS; temporary scope restored |
| Explicit tenant logout removes authority | PASS |
| Explicit platform logout removes authority | PASS |
| Empty-scope tenant-wide assignment reviewed as high impact | PASS |

## Activity Boundary

| Check | Result/evidence |
| --- | --- |
| Tenant A activity contains only Tenant A | PASS |
| Tenant B activity absent from Tenant A | PASS |
| Platform activity metadata fields only | PASS |
| Raw event/audit payload absent | PASS |

## Runtime Boundary

| Check | Result/evidence |
| --- | --- |
| Tenant trust fields present | PASS |
| Platform-only fields absent from tenant API | PASS |
| Platform runtime requires platform session | PASS |
| Secrets/config internals absent | PASS |

## Cross-Tenant Negative Tests

| Surface | Tenant A cannot read Tenant B | Tenant A cannot mutate Tenant B | Evidence |
| --- | --- | --- | --- |
| Products | PASS | PASS | Two-tenant API matrix |
| Inventory | PASS | PASS | Two-tenant and warehouse matrix |
| Orders/fulfillment | PASS | PASS | Two-tenant and warehouse matrix |
| Alerts/recommendations | PASS | PASS | Two-tenant API matrix |
| Connectors/replay | PASS | PASS | Integration-role and two-tenant matrix |
| Scenarios/governance | PASS | PASS | Role, actor, warehouse, and tenant matrix |
| Activity/audit | PASS | Not applicable to read projection | Tenant and platform metadata checks |
| Settings/users | PASS | PASS | Tenant-admin and cross-tenant denial matrix |

## Frontend Navigation And Direct Routes

| Check | Result/evidence |
| --- | --- |
| Role-aware tenant navigation | PASS for all six roles |
| Platform routes absent from tenant shell | PASS |
| Wrong-role route redirected safely | PASS; dashboard or tenant sign-in |
| Direct platform route requests platform sign-in | PASS |
| Backend denial remains authoritative | PASS; direct protected APIs exercised |

## Automated Verification

| Gate | Command | Result |
| --- | --- | --- |
| Backend authorization | `cmd /c mvnw.cmd test` | PASS `149/149` |
| Frontend lint | `npm.cmd run lint` | PASS |
| Frontend build | `npm.cmd run build` | PASS |
| Frontend verify | `npm.cmd run verify` | PASS |
| Six-role navigation/direct-route matrix | `npm.cmd run verify` plus rendered walkthrough | PASS |
| Documentation links | `scripts\docs-link-check.ps1` | PASS; 675 links checked, none missing |
| Secret scan | `scripts\secret-scan.ps1` | PASS; 0 critical findings, 5 known fixture findings |
| Diff hygiene | `git diff --check` | PASS |
| Hosted verification, if applicable | Render UI and direct API rehearsal | PASS with limitations below |

## Rehearsal

| Field | Value |
| --- | --- |
| Synthetic rehearsal tenant | `ACCESS-LIVE-20260822113537` |
| Synthetic isolation tenant | `ACCESS-ISO-20260822113537` |
| Synthetic users/roles | Six single-role tenant identities plus platform-owner verification; no passwords recorded |
| Synthetic warehouse scopes | `WH-COAST` scoped roles; tenant-wide admin; temporary scope changes restored |
| No customer data confirmed | Confirmed |

## Limitations And Blockers

### Limitations

- **Medium:** disabled `WEBHOOK_ORDER` rejects correctly but its failure can be absent from Render replay-queue readback. The proven Company 1 recovery lane remains CSV. If webhook recovery enters pilot scope, this becomes a High blocker until fixed and live-proven.
- **Medium:** import-run records do not carry authoritative warehouse association and therefore cannot be warehouse-filtered beyond the integration-role boundary.
- **Low:** one stale PowerShell session retained across deliberate revocation/redeployment returned `500`; a fresh active session returned the required `403`, and no authority was granted.
- Platform owner remains a single configured identity without MFA, SSO/OIDC, multi-owner RBAC, or self-service rotation.

### Critical blockers

- None.

### High blockers

- None.

### External pending evidence

- None for this access gate. Phase 10 remains intentionally stopped pending gate-owner review.

## Final Result

**Verdict:** Accepted with documented limitations, pending gate-owner review.

**Reason:** Dedicated platform authority, six-role tenant boundaries, tenant/warehouse isolation, session behavior, realtime policy, direct backend denials, rendered role separation, and platform privacy are proven. Critical blockers: `0`. High blockers: `0`.

**Phase 10 allowed:** No. Stop before Phase 10 until the gate owner reviews this evidence.

**Approver:** Pending gate-owner acceptance.

**Approval date:** Pending.
