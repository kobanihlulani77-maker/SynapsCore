# Company User Provisioning Record Template

Use this internal template after completing Phase 4 user provisioning for a controlled pilot company.

Do not record passwords, password hashes, reset secrets, platform-admin tokens, bootstrap tokens, session cookies, connector secrets, or proof-state values.

## Company And Tenant

| Field | Value |
| --- | --- |
| Company legal/trading name |  |
| Tenant id |  |
| Tenant code |  |
| Workspace display name |  |
| Environment | LOCAL / STAGING-PROOF / LIVE-PILOT |
| Backend URL |  |
| Frontend URL |  |
| Phase 2 intake reference |  |
| Phase 3 provisioning record reference |  |

## Provisioning Operator

| Field | Value |
| --- | --- |
| SynapseCore operator name/role |  |
| Tenant-admin account used |  |
| Provisioning date/time |  |
| Approved user count |  |
| Actual user count created |  |

## User Matrix

| User | Business responsibility | Username | Operator actor name | Roles | Warehouse scope | Status | Auth verified | Role verified | Handoff ready |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  | YES / NO | YES / NO | YES / NO |

## Operator Lanes

| Operator actor name | Display name | Roles | Warehouse scope | Active | Created/updated | Notes |
| --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |

## Account Verification

| Check | Result | Evidence |
| --- | --- | --- |
| Correct tenant code in session | PASS / FAIL |  |
| User belongs to intended tenant | PASS / FAIL |  |
| Operator mapping correct | PASS / FAIL |  |
| Roles match approved matrix | PASS / FAIL |  |
| Warehouse scope correct | PASS / FAIL / N/A |  |
| Initial login works | PASS / FAIL |  |
| Password change verified where appropriate | PASS / FAIL / DEFERRED |  |
| Old password rejected after change/reset | PASS / FAIL / DEFERRED |  |
| Empty-state login renders safely | PASS / FAIL |  |

## Negative Authorization

| Check | Result | Evidence |
| --- | --- | --- |
| Non-admin cannot manage users/operators | PASS / FAIL / N/A |  |
| Non-integration role cannot replay/manage connectors | PASS / FAIL / N/A |  |
| Non-approver cannot approve restricted scenario | PASS / FAIL / N/A |  |
| Company user cannot access another tenant data | PASS / FAIL / N/A |  |
| Customer user has no platform/bootstrap token | PASS / FAIL |  |

## Bootstrap Identity Disposition

| Bootstrap identity | Current status | Decision | Evidence |
| --- | --- | --- | --- |
| Operations Lead operator |  | KEEP INTERNAL / CONVERT / DISABLE / DEFER |  |
| Operations Lead admin user |  | KEEP INTERNAL / CONVERT / DISABLE / DEFER |  |
| Executive Operations Director operator |  | KEEP INTERNAL / CONVERT / DISABLE / DEFER |  |
| Executive generated user |  | KEEP INTERNAL / CONVERT / DISABLE / DEFER |  |
| Operations Planner operator |  | KEEP INTERNAL / CONVERT / DISABLE / DEFER |  |

## Credential Handoff Status

| Field | Value |
| --- | --- |
| Initial credentials generated uniquely | YES / NO |
| Passwords stored in approved secure channel | YES / NO / NOT YET |
| Passwords absent from Git/docs | YES / NO |
| Username/password separated for handoff | YES / NO |
| Customer password change required | YES / NO |
| Handoff blocked pending secure channel | YES / NO |

## Revocation Readiness

| Check | Result |
| --- | --- |
| Disable-user path known | YES / NO |
| Disable-operator path known | YES / NO |
| Password reset path known | YES / NO |
| Existing-session invalidation behavior understood | YES / NO |
| Emergency access owner identified | YES / NO |

## Issues

| Issue | Severity | Owner | Resolution |
| --- | --- | --- | --- |
|  |  |  |  |

## Phase 4 Verdict

| Field | Value |
| --- | --- |
| Phase 4 verdict |  |
| Accepted by |  |
| Acceptance timestamp |  |
| Phase 5 authorized | YES / NO |
| Phase 5 handoff notes |  |
