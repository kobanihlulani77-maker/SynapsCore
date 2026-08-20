# Company Pre-Handover Verification Record

Use this template for each real pilot company after Phases 2-7 are complete and before customer access is handed over. Follow [company-pre-handover-verification-checklist.md](../company-pre-handover-verification-checklist.md).

Do not store passwords, connector tokens, platform/bootstrap tokens, database credentials, raw customer payloads, private customer rows, session cookies, backup files, or unredacted logs in this record.

## Record Control

| Field | Value |
| --- | --- |
| Company |  |
| Environment classification | ACTUAL COMPANY PILOT / REHEARSAL |
| Tenant code |  |
| Tenant ID |  |
| Verification date/time and timezone |  |
| Technical verifier |  |
| Release commit |  |
| Frontend URL |  |
| Backend URL |  |
| Expected user count |  |
| Expected connector |  |
| Expected data scope |  |
| Record status | DRAFT / COMPLETE / SUPERSEDED |

Target confirmation statement:

```text
I confirm this record targets the company and tenant identified above. It is not
a proof, demo, seed, or unrelated tenant unless Environment classification is
explicitly REHEARSAL.
```

Confirmed by:

Date/time:

## Allowed Results

Use only:

- `PASS`
- `PASS WITH ACCEPTED OPERATING CONDITION`
- `OUT OF PILOT`
- `FAIL - HANDOVER BLOCKER`
- `NOT APPLICABLE`

Blocker severity, when applicable:

- `CRITICAL`
- `HIGH`
- `MEDIUM`
- `LOW`

## Required Evidence Gate

| Required input | Record/reference | Approved? | Result | Notes |
| --- | --- | --- | --- | --- |
| Phase 2 intake |  |  |  |  |
| Phase 3 tenant provisioning |  |  |  |  |
| Phase 4 user provisioning |  |  |  |  |
| Phase 5 integration provisioning |  |  |  |  |
| Phase 6 data mapping |  |  |  |  |
| Phase 6 data onboarding |  |  |  |  |
| Phase 7 operational configuration baseline |  |  |  |  |
| Final pre-pilot release evidence |  |  |  |  |
| Current backup/checkpoint evidence |  |  |  |  |
| Named support/operational owners |  |  |  |  |

If any required scope evidence is absent, record:

`PRE-HANDOVER BLOCKED - PROVISIONING EVIDENCE INCOMPLETE`

## Tenant Identity

| Check | Expected | Actual | Evidence reference | Result | Severity/notes |
| --- | --- | --- | --- | --- | --- |
| Tenant exists |  |  |  |  |  |
| Tenant ID |  |  |  |  |  |
| Tenant code |  |  |  |  |  |
| Company display name |  |  |  |  |  |
| Active/enabled state |  |  |  |  |  |
| Description/metadata |  |  |  |  |  |
| No unexpected duplicate tenant |  |  |  |  |  |
| No proof/demo/test naming contamination |  |  |  |  |  |
| Matches approved intake and Phase 7 baseline |  |  |  |  |  |

## Unexpected Data Review

| Object class | Expected count/state | Actual count/state | Unexplained identifiers | Evidence | Result | Resolution owner |
| --- | --- | --- | --- | --- | --- | --- |
| Users/operators |  |  |  |  |  |  |
| Connectors |  |  |  |  |  |  |
| Products/catalog |  |  |  |  |  |  |
| Inventory |  |  |  |  |  |  |
| Orders |  |  |  |  |  |  |
| Replay records |  |  |  |  |  |  |
| Scenarios/approvals |  |  |  |  |  |  |
| Other bootstrap/test records |  |  |  |  |  |  |

Foreign-tenant data observed: YES / NO

If YES, Critical blocker ID:

## User Roster Reconciliation

| Approved user | Actual username | Full name match | Tenant match | Enabled state | AccessOperator link | Role(s) | Warehouse scope | Password change state | Login verified | Result |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |  |  |  |

| Count | Value |
| --- | --- |
| Approved users |  |
| Actual customer users |  |
| Exact match | YES / NO |
| Duplicate accounts |  |
| Unexplained accounts |  |

## Bootstrap And Internal Accounts

| Identity | Privilege/role | Classification | Active? | Required reason or disposition | Evidence | Result |
| --- | --- | --- | --- | --- | --- | --- |
|  |  | INTERNAL REQUIRED / DISABLED / CONVERTED TO APPROVED CUSTOMER USER / UNEXPECTED |  |  |  |  |

Provisioning admin disposition:

Unexplained privileged account present: YES / NO

## Login And Session Verification

Do not record credentials.

| User | Login verified | Correct tenant/session | Correct identity | Password-change behavior | First-login flow | Logout verified | Evidence timestamp | Result |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  | YES / NO |  |  |  |  |  |  |  |

## Role And Authorization Matrix

| User | Role | Allowed backend action | Allowed result | Denied backend action | Denied result | Evidence | Final result |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |

## Platform Access Negative Tests

| Customer session test | Expected | Actual | Evidence | Result | Severity/notes |
| --- | --- | --- | --- | --- | --- |
| Tenant creation/bootstrap administration denied | Denied |  |  |  |  |
| Cross-tenant directory/tenant management denied | Denied |  |  |  |  |
| Platform configuration denied | Denied |  |  |  |  |
| Platform secrets inaccessible | Denied/no disclosure |  |  |  |  |
| Another company administration denied | Denied |  |  |  |  |

## Cross-Tenant Isolation

Record only authorized safe reference identifiers.

| Object/action | Reference tenant/object | Expected | Actual | Disclosure observed? | Evidence | Result |
| --- | --- | --- | --- | --- | --- | --- |
| Read product/catalog object |  | 403/404/no disclosure |  |  |  |  |
| Read inventory/warehouse object |  | 403/404/no disclosure |  |  |  |  |
| Read/mutate order ID |  | 403/404/no disclosure |  |  |  |  |
| Read/mutate connector ID |  | 403/404/no disclosure |  |  |  |  |
| Read/replay replay record |  | 403/404/no disclosure |  |  |  |  |
| Read/mutate scenario ID |  | 403/404/no disclosure |  |  |  |  |
| Alter tenant settings |  | 403/404/no disclosure |  |  |  |  |

## Connector Reconciliation

| Field | Approved Phase 5/7 value | Actual | Evidence | Result |
| --- | --- | --- | --- | --- |
| Tenant |  |  |  |  |
| Connector ID |  |  |  |  |
| Source system |  |  |  |  |
| Display name |  |  |  |  |
| Type |  |  |  |  |
| Enabled state |  |  |  |  |
| Sync mode |  |  |  |  |
| Cadence |  |  |  |  |
| Pull URL, if applicable |  |  |  |  |
| Validation policy |  |  |  |  |
| Transformation policy |  |  |  |  |
| Mapping version |  |  |  |  |
| Warehouse fallback |  |  |  |  |
| Default warehouse |  |  |  |  |
| Support owner |  |  |  |  |
| Notes |  |  |  |  |
| No unexplained extra connector |  |  |  |  |

## Connector Secret Posture

| Check | Evidence without secret value | Result | Owner/notes |
| --- | --- | --- | --- |
| Required secret configured |  |  |  |
| Only approved hint/status visible |  |  |  |
| Secret absent from Git/docs/evidence |  |  |  |
| Frontend does not reveal raw secret |  |  |  |
| Ordinary logs do not expose secret |  |  |  |
| Secret holder/handoff status recorded |  |  |  |
| Rotation procedure known |  |  |  |
| No-HMAC condition accepted if applicable |  |  |  |

## Connector Disable And Enable

| Test | Synthetic request/reference | Expected | Actual | Evidence | Result |
| --- | --- | --- | --- | --- | --- |
| Disable connector |  | Lane disabled |  |  |  |
| Inbound while disabled |  | Blocked/rejected/paused truthfully |  |  |  |
| Enable connector |  | Lane enabled |  |  |  |
| Inbound after enable |  | Approved lane restored |  |  |  |

If not safely executable, accepted prior evidence and reason:

## Catalog Reconciliation

| Check | Approved source | Actual | Difference | Evidence | Result |
| --- | --- | --- | --- | --- | --- |
| Product count |  |  |  |  |  |
| Deterministic SKU set |  |  |  |  |  |
| Representative names/categories |  |  |  |  |  |
| Tenant ownership |  |  |  |  |  |
| Duplicate SKUs | None expected |  |  |  |  |
| Proof/test products | None expected |  |  |  |  |

## Inventory Reconciliation

| Check | Approved source | Actual | Difference | Evidence | Result |
| --- | --- | --- | --- | --- | --- |
| Row count |  |  |  |  |  |
| Product associations |  |  |  |  |  |
| Warehouse associations |  |  |  |  |  |
| Quantities |  |  |  |  |  |
| Reorder thresholds |  |  |  |  |  |
| Tenant ownership |  |  |  |  |  |
| Orphan rows | None expected |  |  |  |  |
| Negative/impossible values | None unexplained |  |  |  |  |
| Duplicate product/warehouse rows | None expected |  |  |  |  |

## Order Reconciliation

| Check | Approved source | Actual | Difference | Evidence | Result |
| --- | --- | --- | --- | --- | --- |
| Accepted order count |  |  |  |  |  |
| External ID uniqueness | Unique per tenant |  |  |  |  |
| Tenant/warehouse |  |  |  |  |  |
| Statuses |  |  |  |  |  |
| Item relationships |  |  |  |  |  |
| Duplicate accepted orders | None expected |  |  |  |  |
| Rejected rows accounted for |  |  |  |  |  |
| Proof/test orders | None expected |  |  |  |  |

## Relational Integrity

| Integrity check | Expected | Actual | Evidence | Result | Severity/notes |
| --- | --- | --- | --- | --- | --- |
| Inventory product references | No missing product |  |  |  |  |
| Inventory warehouse references | Company tenant only |  |  |  |  |
| Order item product references | No missing product |  |  |  |  |
| Order warehouse references | Company tenant only |  |  |  |  |
| Product/warehouse inventory identity | No duplicate |  |  |  |  |
| Tenant/external-order identity | No duplicate |  |  |  |  |
| Replay recovery identity | No duplicate successful recovery |  |  |  |  |
| Cross-tenant relationships | None |  |  |  |  |

## Source Of Truth

| Domain | Company source of truth | SynapseCore pilot role | Exception | Owner | Result |
| --- | --- | --- | --- | --- | --- |
| Catalog |  |  |  |  |  |
| Inventory |  |  |  |  |  |
| Orders |  |  |  |  |  |
| Connector failures/replay evidence |  |  |  |  |  |
| Scenarios/approvals |  |  |  |  |  |

## Dashboard And UI Readback

| Surface | Representative check | Backend/source expectation | Observed | Viewport | Evidence | Result |
| --- | --- | --- | --- | --- | --- | --- |
| Dashboard | Tenant/counts/status |  |  |  |  |  |
| Catalog | SKU/name/category |  |  |  |  |  |
| Inventory | SKU/warehouse/quantity/risk |  |  |  |  |  |
| Orders | External ID/status/items/warehouse |  |  |  |  |  |

Other-tenant data observed: YES / NO

Fatal or dishonest state observed: YES / NO

## Alerts

| Condition | Expected type/severity/state | Actual alert | Tenant | Visible role | Evidence | Result |
| --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |

## Recommendations

| Condition | Expected type/priority | Actual recommendation | Evidence basis | Tenant | Visible role | Unexpected mutation? | Result |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  | NO expected |  |

## Replay And Recovery

| Stage | Expected | Actual | Actor/role | Evidence | Result |
| --- | --- | --- | --- | --- | --- |
| Deterministic failed inbound created | Visible tenant-scoped failure |  |  |  |  |
| Eligibility evaluated | Correct eligible/blocked state |  |  |  |  |
| Authorized replay | Correct role and warehouse scope |  |  |  |  |
| Live-flow result | One intended result |  |  |  |  |
| Audit/history | Recorded |  |  |  |  |
| Duplicate check | No duplicate |  |  |  |  |

### Replay negative test

| Negative case | Expected | Actual | Evidence | Result |
| --- | --- | --- | --- | --- |
| Unauthorized / blocked / already handled | Denied or no unsafe duplicate |  |  |  |

## Approval And Scenario Governance

Approved pilot posture: APPROVALS IN / OUT; SCENARIOS IN / OUT

| Check | Expected | Actual | Evidence | Result |
| --- | --- | --- | --- | --- |
| Pending ScenarioRun | Correct tenant/state |  |  |  |
| Authorized approval/rejection | Persisted expected state |  |  |  |
| Unauthorized approval | Denied |  |  |  |
| Preview | No live mutation |  |  |  |
| Compare | No live mutation |  |  |  |
| Save/governance | Approved path |  |  |  |
| Execution, if in scope | One expected OrderService result |  |  |  |

## Separation Of Duty

| Control | Assigned user(s) | Technical enforcement | Procedural control | Conflict? | Owner/acceptance | Result |
| --- | --- | --- | --- | --- | --- | --- |
| Requester |  |  |  |  |  |  |
| Review owner |  |  |  |  |  |  |
| Final approver |  |  |  |  |  |  |
| Escalation owner |  |  |  |  |  |  |
| Multiple governance roles |  | Partial/current model |  |  |  |  |

## Integration Policy And Tenant Settings

| Setting | Frozen Phase 7 value | Actual | Evidence | Result |
| --- | --- | --- | --- | --- |
| Connector sync mode |  |  |  |  |
| Connector cadence |  |  |  |  |
| Pull URL, if relevant |  |  |  |  |
| Validation policy |  |  |  |  |
| Transformation policy |  |  |  |  |
| Warehouse fallback |  |  |  |  |
| Connector notes/support owner |  |  |  |  |
| Tenant name |  |  |  |  |
| Tenant description |  |  |  |  |
| Password rotation days |  |  |  |  |
| Session timeout minutes |  |  |  |  |
| Tenant operational policy |  |  |  |  |

## Warehouses And Operator Scope

| Warehouse code | Approved name/location | Actual | User/operator scope | Duplicate identity? | Evidence | Result |
| --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |

| Operator | Role(s) | Warehouse scope | Empty means tenant-wide accepted? | Operational lane | Expected access | Actual | Result |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |

## Realtime And Degraded State

| Check | Expected | Actual | Evidence | Result |
| --- | --- | --- | --- | --- |
| `/ws/info` | Responding |  |  |  |
| Authenticated connection | Connected for intended user |  |  |  |
| Tenant event | Expected screen updates |  |  |  |
| Foreign-tenant event | No disclosure |  |  |  |
| Reconnect/degraded state | Truthful state |  |  |  |
| Polling/manual fallback | Matches implementation |  |  |  |

## Runtime And Failure Honesty

| Check | Expected | Actual | Evidence | Result |
| --- | --- | --- | --- | --- |
| `/api/system/runtime` | Read-only truthful evidence |  |  |  |
| Runtime surface | Intended role can interpret |  |  |  |
| Runtime mutation | Customer cannot mutate infrastructure |  |  |  |
| Controlled failure | No fake healthy state |  |  |  |
| Failure feedback | No fake success/infinite loading |  |  |  |
| Connector/replay failure | Visible, not hidden |  |  |  |

## Backup And Restore Readiness

| Check | Value/evidence | Owner | Result | Accepted condition |
| --- | --- | --- | --- | --- |
| Backup procedure |  |  |  |  |
| Most recent successful backup/checkpoint |  |  |  |  |
| Artifact outside Git |  |  |  |  |
| Checksum, if applicable |  |  |  |  |
| Retention expectation |  |  |  |  |
| Recovery owner |  |  |  |  |
| Restore procedure known |  |  |  |  |
| Application-level restore | PROVEN |  |  |  |
| Provider-level Render restore | DOCUMENTED CONDITION unless later proven |  |  |  |
| Pilot owner accepts provider limitation |  |  |  |  |

## Security And Actuator

| Check | Expected | Actual/evidence | Result | Severity/notes |
| --- | --- | --- | --- | --- |
| Authentication/session | Correct tenant-scoped session |  |  |  |
| Role authorization | Allowed/denied matrix passes |  |  |  |
| Tenant isolation | No cross-tenant disclosure/mutation |  |  |  |
| CORS | Approved frontend origin only |  |  |  |
| Secret scan/posture | No critical leak |  |  |  |
| Rate limiting | Existing proof remains applicable |  |  |  |
| Sensitive errors | No secret/private leakage |  |  |  |
| `/actuator/health` | Healthy |  |  |  |
| `/actuator/health/liveness` | Healthy |  |  |  |
| `/actuator/health/readiness` | Healthy |  |  |  |
| `/actuator/metrics` | Restricted |  |  |  |
| `/actuator/prometheus` | Restricted |  |  |  |

## Pilot Scale And Dataset Envelope

| Dimension | Accepted evidence/recommendation | Company actual | Classification | Reviewer | Result |
| --- | --- | --- | --- | --- | --- |
| Tenant/workspace count | 1 |  |  |  |  |
| Active operators | 3-5 recommended; 25 local read operators proven |  |  |  |  |
| Connector lanes | 1 initially |  |  |  |  |
| Realtime clients | 50 local clients proven |  |  |  |  |
| Read throughput | About 41 local RPS, p95 under 500 ms |  |  |  |  |
| Product count | Bounded pilot set |  |  |  |  |
| Inventory row count | Bounded pilot set |  |  |  |  |
| Order rate | Bounded approved lane |  |  |  |  |
| Inbound rate | Bounded approved lane |  |  |  |  |

Allowed classifications: `WITHIN PILOT ENVELOPE`, `TECHNICAL REVIEW REQUIRED`, `OUTSIDE CURRENT EVIDENCE`.

## Focused Responsive And Control Reachability

| Check | Viewport/data condition | Evidence | Result | Notes |
| --- | --- | --- | --- | --- |
| Login accessible |  |  |  |  |
| Navigation usable |  |  |  |  |
| Primary pilot actions reachable |  |  |  |  |
| Long company name safe |  |  |  |  |
| Long SKU/external ID/warehouse safe |  |  |  |  |
| No critical clipping/overflow |  |  |  |  |

## Customer Feature Scope

| Feature | IN PILOT / OUT OF PILOT / INTERNAL ONLY | Configured? | Tested? | Customer-visible claim | Limitation | Result |
| --- | --- | --- | --- | --- | --- | --- |
| Dashboard |  |  |  |  |  |  |
| Catalog |  |  |  |  |  |  |
| Inventory |  |  |  |  |  |  |
| Orders |  |  |  |  |  |  |
| Alerts |  |  |  |  |  |  |
| Recommendations |  |  |  |  |  |  |
| Integrations |  |  |  |  |  |  |
| Replay |  |  |  |  |  |  |
| Approvals |  |  |  |  |  |  |
| Scenarios |  |  |  |  |  |  |
| Runtime |  |  |  |  |  |  |
| Settings |  |  |  |  |  |  |
| Users/Admin |  |  |  |  |  |  |
| Platform administration | INTERNAL ONLY |  |  | Not customer capability |  |  |

## Known Limitations

| Limitation | Classification | Pilot impact | Control/owner | Accepted by | Result |
| --- | --- | --- | --- | --- | --- |
| No MFA/SSO/invitations |  |  |  |  |  |
| No customer forgot-password flow |  |  |  |  |  |
| No formal read-only role |  |  |  |  |  |
| No connector-specific scope |  |  |  |  |  |
| No webhook HMAC verification |  |  |  |  |  |
| No arbitrary mapping UI |  |  |  |  |  |
| No inventory CSV |  |  |  |  |  |
| No per-import rollback |  |  |  |  |  |
| No automatic retention cleanup |  |  |  |  |  |
| No generic alert/recommendation rule engine |  |  |  |  |  |
| No universal separation-of-duty engine |  |  |  |  |  |
| Provider-level restore evidence limited |  |  |  |  |  |

Allowed classifications: `ACCEPTED OPERATING CONDITION`, `OUT OF PILOT`, `MUST FIX BEFORE HANDOVER`, `POST-PILOT`.

## Support Ownership And Customer Issue Path

| Responsibility | Named owner/role | Contact reference | Backup owner | Verified? | Result |
| --- | --- | --- | --- | --- | --- |
| Deployment |  |  |  |  |  |
| Backup |  |  |  |  |  |
| Restore |  |  |  |  |  |
| Incident response |  |  |  |  |  |
| Access provisioning |  |  |  |  |  |
| Connector support |  |  |  |  |  |
| Company business contact |  |  |  |  |  |
| Company technical contact |  |  |  |  |  |
| Rollback |  |  |  |  |  |

| Customer issue | Internal intake path | Escalation owner | Target response posture | Verified? |
| --- | --- | --- | --- | --- |
| Login |  |  |  |  |
| Data mismatch |  |  |  |  |
| Connector |  |  |  |  |
| Replay |  |  |  |  |
| Approval |  |  |  |  |
| System outage |  |  |  |  |

No critical owner may be `TBD`.

## Pilot Stop Conditions

| Stop condition | PAUSE authority | Investigator | ROLLBACK authority | Source-system fallback | Communication path | Verified? |
| --- | --- | --- | --- | --- | --- | --- |
| Tenant leakage |  |  |  |  |  |  |
| Authorization bypass |  |  |  |  |  |  |
| Data corruption |  |  |  |  |  |  |
| Wrong-tenant write |  |  |  |  |  |  |
| Uncontrolled duplicate replay |  |  |  |  |  |  |
| Wrong governance action |  |  |  |  |  |  |
| Secret exposure |  |  |  |  |  |  |
| Unrecoverable database failure |  |  |  |  |  |  |
| Severe repeated availability failure |  |  |  |  |  |  |

## Automation And Manual Coverage

| Evidence/check | AUTOMATED BY SCRIPT / MANUAL CHECK / NOT CURRENTLY COVERED | Tool or owner | Execution/evidence reference | Result/notes |
| --- | --- | --- | --- | --- |
| Live connections |  | `check-live-connections.ps1` |  |  |
| Synthetic local workflow rehearsal |  | `verify-company-readiness.ps1` |  |  |
| Hosted proof tenant |  | Hosted proof tooling |  |  |
| Actual company user reconciliation | MANUAL CHECK |  |  |  |
| Actual cross-tenant object matrix | NOT CURRENTLY COVERED by readiness script |  |  |  |
| Actual company data reconciliation | MANUAL CHECK |  |  |  |
| Actual company realtime event | NOT CURRENTLY COVERED by readiness script |  |  |  |
| Backup/provider acceptance | MANUAL CHECK |  |  |  |
| Support/sign-off | MANUAL CHECK |  |  |  |

## Rehearsal Record

| Field | Value |
| --- | --- |
| Rehearsal performed | YES / NO |
| Rehearsal tenant |  |
| Clearly non-customer | YES / NO |
| Rehearsal date |  |
| Sections exercised |  |
| Result |  |
| Company-specific checks remaining |  |

Allowed rehearsal result:

`CHECKLIST/PROCESS READY - COMPANY-SPECIFIC EXECUTION REQUIRED`

## Blocker Register

| ID | Finding | Severity | Affected gate | Owner | Required resolution | Retest evidence | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  | OPEN / RESOLVED / ACCEPTED CONDITION |

Critical blockers open:

High blockers open:

No handover may be authorized while either count is greater than zero.

## Accepted Operating Conditions

| ID | Condition | Reason accepted | Pilot control | Owner | Review date | Accepted by |
| --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |

## Final Result Summary

| Area | PASS | PASS WITH CONDITION | OUT OF PILOT | FAIL/BLOCKER | NOT APPLICABLE |
| --- | ---: | ---: | ---: | ---: | ---: |
| Tenant |  |  |  |  |  |
| Users/login |  |  |  |  |  |
| Roles/isolation |  |  |  |  |  |
| Connector |  |  |  |  |  |
| Data/integrity |  |  |  |  |  |
| Alerts/recommendations |  |  |  |  |  |
| Replay |  |  |  |  |  |
| Approvals/scenarios |  |  |  |  |  |
| Settings/scopes |  |  |  |  |  |
| Realtime/runtime |  |  |  |  |  |
| Backup/security |  |  |  |  |  |
| Scale/UI reachability |  |  |  |  |  |
| Support/stop conditions |  |  |  |  |  |

## Handover Decision

Mark exactly one:

| Decision | Mark one |
| --- | --- |
| `AUTHORIZED FOR CUSTOMER HANDOVER` |  |
| `AUTHORIZED FOR CUSTOMER HANDOVER WITH ACCEPTED OPERATING CONDITIONS` |  |
| `NOT AUTHORIZED - PRE-HANDOVER BLOCKERS REMAIN` |  |
| `CHECKLIST/PROCESS READY - COMPANY-SPECIFIC EXECUTION REQUIRED` |  |

Decision basis:

```text

```

## Sign-Off

| Sign-off | Name | Decision | Date/time | Signature/reference |
| --- | --- | --- | --- | --- |
| Technical Verifier |  |  |  |  |
| Platform Owner |  |  |  |  |
| Pilot Owner |  |  |  |  |

Accepted conditions reference:

Phase 9 authorized to begin: YES / NO

## Phase 9 Handoff Inputs

Complete only after an actual company decision authorizes handover. Do not include credentials.

| Input | Verified value/reference |
| --- | --- |
| Company name |  |
| Tenant |  |
| Approved users |  |
| Access status |  |
| Frontend URL |  |
| First-login requirement |  |
| Customer-visible feature scope |  |
| Customer-visible operating conditions |  |
| Support contact |  |
| Pilot start date |  |
| Stop/escalation path |  |
| Handover authorization |  |
