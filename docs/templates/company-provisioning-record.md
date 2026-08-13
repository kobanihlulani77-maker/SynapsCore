# Company Provisioning Record Template

Use this template for each controlled pilot company after completing the tenant/workspace provisioning runbook.

Do not record passwords, tokens, session cookies, connector secrets, database credentials, or customer API keys in this file.

## Company

| Field | Value |
| --- | --- |
| Company legal/trading name |  |
| Business unit / operating lane |  |
| Primary contact reference |  |
| Phase 2 intake document/reference |  |
| Pilot purpose |  |
| Approved operating scope |  |
| Out-of-scope items |  |

## Intake Approval

| Field | Value |
| --- | --- |
| Phase 2 status |  |
| Approved by |  |
| Approval timestamp |  |
| Preconditions |  |
| Risks accepted |  |

## Environment

| Field | Value |
| --- | --- |
| Environment class | LOCAL / STAGING-PROOF / LIVE-PILOT |
| Backend URL |  |
| Frontend URL |  |
| Runtime/readiness checked | YES / NO |
| Operator role |  |
| Platform-admin token source reference | Secret manager reference only |

## Tenant

| Field | Value |
| --- | --- |
| Tenant id |  |
| Tenant code |  |
| Tenant display name |  |
| Tenant description |  |
| Active |  |
| Created at |  |
| Updated at |  |

## Workspace

Current implementation note: workspace is the tenant-scoped operating environment, not a separate workspace table.

| Field | Value |
| --- | --- |
| Workspace model | Tenant-backed workspace |
| Workspace display name |  |
| Workspace code |  |
| Security password rotation days |  |
| Security session timeout minutes |  |
| Security policy version |  |
| Starter warehouse codes |  |
| Primary location |  |
| Secondary location |  |

## Bootstrap Access Records

Record only identifiers. Do not record passwords.

| Record | Value |
| --- | --- |
| Bootstrap admin username |  |
| Bootstrap admin actor |  |
| Executive username |  |
| Executive actor |  |
| Password rotation required before handover | YES / NO |
| Phase 4 access reconciliation required | YES / NO |

## Initial State Counts

| Area | Count / Result |
| --- | --- |
| Operators |  |
| Users |  |
| Warehouses |  |
| Products |  |
| Inventory rows |  |
| Orders |  |
| Connectors |  |
| Replay queue records |  |
| Scenarios |  |
| Pending approvals |  |
| Alerts |  |
| Recommendations |  |
| Audit records |  |

## Isolation Verification

| Check | Result | Evidence |
| --- | --- | --- |
| Tenant directory duplicate check | PASS / FAIL |  |
| Workspace response tenant code matches | PASS / FAIL |  |
| Tenant-scoped users only | PASS / FAIL |  |
| Tenant-scoped products only | PASS / FAIL |  |
| Tenant-scoped inventory only | PASS / FAIL |  |
| Tenant-scoped orders only | PASS / FAIL |  |
| Tenant-scoped replay queue only | PASS / FAIL |  |
| Tenant-scoped scenarios only | PASS / FAIL |  |
| No unrelated customer data | PASS / FAIL |  |
| Cross-tenant negative check where safe | PASS / FAIL / NOT RUN |  |

## Issues

| Issue | Severity | Owner | Resolution |
| --- | --- | --- | --- |
|  |  |  |  |

## Rollback / Correction Posture

| Question | Answer |
| --- | --- |
| Any incorrect values? |  |
| Can they be corrected through supported API/UI? |  |
| Is tenant code correct? |  |
| Any need to abandon/recreate tenant? |  |
| Direct DB action avoided? | YES / NO |

## Phase 4 Authorization

| Field | Value |
| --- | --- |
| Phase 3 verdict |  |
| Phase 4 authorized | YES / NO |
| Authorized by |  |
| Authorization timestamp |  |
| User list source |  |
| Role mapping source |  |
| Notes for user provisioning |  |
