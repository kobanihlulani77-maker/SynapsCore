# Company Pre-Handover Verification Checklist

This is the authoritative internal SynapseCore Phase 8 gate for a provisioned and configured pilot company. Run it after Phases 2-7 are complete and before any customer receives access.

Phase 8 verifies the exact company environment. It does not provision the tenant, create customer users, load customer data, redesign the product, or create customer-facing handover material.

## Phase Boundary

The controlled sequence is:

```text
APPROVED INTAKE
-> TENANT PROVISIONED
-> USERS PROVISIONED
-> CONNECTOR CONFIGURED
-> DATA ONBOARDED
-> OPERATIONAL CONFIGURATION FROZEN
-> PHASE 8 PRE-HANDOVER VERIFICATION
-> CUSTOMER HANDOVER MAY BE AUTHORIZED
```

If no real company tenant exists, this document and its record template may be reviewed or rehearsed against an approved proof environment. That proves only that the process is executable. It does not authorize Company 1 handover.

Current Phase 8 classification at document creation:

`CHECKLIST/PROCESS READY - COMPANY-SPECIFIC EXECUTION REQUIRED`

## Non-Negotiable Rules

- Confirm the target company and tenant before making any request.
- Never treat a proof tenant as a customer tenant.
- Use supported application APIs and product surfaces; do not edit database rows.
- Do not store passwords, raw connector tokens, platform tokens, database credentials, raw customer payloads, or sensitive row-level data in evidence.
- Do not automatically delete unexpected users, connectors, or data. Classify them, pause where required, and resolve them through the approved operating procedure.
- Do not force out-of-scope business functionality merely to complete a checklist row.
- Reuse accepted platform evidence where it proves a platform-wide control, but execute company-specific identity, authorization, reconciliation, isolation, and scope checks against the actual company tenant.
- No unresolved Critical or High blocker may remain before handover.

## Authoritative Evidence Chain

Phase 8 connects these sources. It does not replace them.

| Phase or gate | Required source | What Phase 8 consumes |
| --- | --- | --- |
| Phase 2 | [company-1-pilot-intake-pack.md](company-1-pilot-intake-pack.md) | Approved company identity, pilot scope, users, systems, data authority, success criteria, contacts, risks, and operating envelope. |
| Phase 3 | [company-tenant-workspace-provisioning-runbook.md](company-tenant-workspace-provisioning-runbook.md) and [company-provisioning-record.md](templates/company-provisioning-record.md) | Tenant identity, initial state, bootstrap identities, warehouses, isolation evidence, and provisioning verdict. |
| Phase 4 | [company-user-provisioning-runbook.md](company-user-provisioning-runbook.md) and [company-user-provisioning-record.md](templates/company-user-provisioning-record.md) | Approved-to-actual user roster, AccessUser/AccessOperator links, roles, warehouse scopes, login evidence, and bootstrap account disposition. |
| Phase 5 | [company-integration-setup-runbook.md](company-integration-setup-runbook.md) and [company-integration-provisioning-record.md](templates/company-integration-provisioning-record.md) | Connector identity, type, source, policies, secret posture, role tests, disablement, replay evidence, and support ownership. |
| Phase 6 | [company-data-onboarding-runbook.md](company-data-onboarding-runbook.md), [company-data-mapping-record.md](templates/company-data-mapping-record.md), and [company-data-onboarding-record.md](templates/company-data-onboarding-record.md) | Approved mappings, counts, reconciliation, integrity evidence, readback, source-of-truth decision, and bounded dataset classification. |
| Phase 7 | [company-operational-configuration-runbook.md](company-operational-configuration-runbook.md) and [company-operational-configuration-record.md](templates/company-operational-configuration-record.md) | Frozen feature scope, policy baseline, replay rules, governance, scenario scope, settings, realtime expectations, limitations, and support handoff. |
| Final release gate | [final-pre-pilot-release-gate.md](final-pre-pilot-release-gate.md) and [verification-status.md](verification-status.md) | Release identity, current proof baseline, final platform classification, deployment evidence, stop conditions, and known operating conditions. |
| Scale | [performance-scale-proof.md](performance-scale-proof.md) | Proven local envelope and explicit limits on what the evidence does not prove. |
| Recovery | [backup-restore-runbook.md](backup-restore-runbook.md) | Application backup/restore evidence, recovery procedure, ownership, and provider-level limitation. |
| Security | [security-and-trust-model.md](security-and-trust-model.md) and [security-test-plan.md](security-test-plan.md) | Existing auth, session, tenant isolation, CORS, rate-limit, secret, leakage, and trust controls. |
| Controls | [pre-pilot-gate-4-control-verification.md](pre-pilot-gate-4-control-verification.md) | Accepted 201/201 control evidence and focused responsive/keyboard baseline. |
| Hosted proof | [hosted-proof.md](hosted-proof.md) | Deployed auth, page, catalog, realtime, replay, scenario, role, operational-surface, and rate-limit proof. |

## Required Input Gate

Before verification starts, obtain completed and approved references for:

- Phase 2 approved intake
- Phase 3 tenant provisioning record
- Phase 4 user provisioning record
- Phase 5 integration provisioning record
- Phase 6 data mapping and onboarding records
- Phase 7 frozen operational configuration record
- release commit and final pre-pilot evidence
- current backup/checkpoint evidence
- named operational and support owners

If any Phase 2-7 record required by the approved pilot scope is absent, stop and classify:

`PRE-HANDOVER BLOCKED - PROVISIONING EVIDENCE INCOMPLETE`

## Result States

Every check must end in exactly one state:

| State | Meaning |
| --- | --- |
| `PASS` | The requirement is met and evidence is recorded. |
| `PASS WITH ACCEPTED OPERATING CONDITION` | The supported path works, a known limitation remains, and an accountable owner has accepted the condition. |
| `OUT OF PILOT` | The capability is explicitly outside the approved company scope and is not represented as enabled. |
| `FAIL - HANDOVER BLOCKER` | The requirement is not met and customer handover cannot proceed. |
| `NOT APPLICABLE` | The check cannot apply to the selected connector, workflow, or operating model; the reason is recorded. |

`Looks okay`, `probably`, and blank cells are not valid results.

## Blocker Severity

| Severity | Definition | Examples | Handover effect |
| --- | --- | --- | --- |
| Critical | Immediate threat to tenant, identity, secret, or data integrity. | Tenant leakage, auth bypass, wrong-tenant mutation, data corruption, secret exposure, destructive wrong governance action. | Stop immediately. No handover. |
| High | Core approved pilot lane cannot be operated safely. | Approved user cannot operate, core connector broken, material data mismatch, unsafe replay, required approval path broken, backup absent. | No handover until resolved and retested. |
| Medium | Secondary approved capability is degraded or needs a bounded workaround. | Non-core visibility issue, manual workaround with clear ownership. | Requires explicit acceptance and action plan. |
| Low | Non-blocking operational inconvenience. | Minor evidence or usability friction that does not compromise control reachability. | Record for pilot follow-up. |

## 1. Target Environment Confirmation

The verifier must display and read back the following before testing:

| Field | Required value |
| --- | --- |
| Environment | Local rehearsal / hosted proof rehearsal / actual company pilot |
| Frontend URL | Exact origin |
| Backend URL | Exact origin |
| Company | Legal or approved pilot name |
| Tenant code | Exact immutable tenant code |
| Tenant ID | Exact persisted tenant identifier |
| Expected user count | From Phase 2/4 |
| Expected connector | Source, type, and connector ID from Phase 5 |
| Expected data scope | Counts/domains from Phase 6 |
| Release commit | Exact deployed commit |
| Operator | Person performing verification |
| Date/time | Timezone-qualified timestamp |

The operator must confirm:

```text
I am verifying the named company tenant above. This is not a proof, demo, seed,
or unrelated customer tenant unless the record is explicitly marked REHEARSAL.
```

A target mismatch is a High blocker. A proof tenant recorded as Company 1 evidence invalidates the verification record.

## 2. Tenant Identity And Unexpected-State Verification

### Tenant identity

Use the supported workspace administration read path and the Phase 3 record to verify:

- tenant exists
- tenant ID, code, and display name match
- active/enabled state is correct where exposed
- description and supported metadata match the approved intake
- no unexpected duplicate tenant exists in the authorized administrative view
- no proof, demo, seed, `HOSTED-PROOF`, `STARTER`, or test naming contaminates the customer tenant
- current configuration corresponds to the approved intake and frozen Phase 7 baseline

Record actual and expected values. Do not infer identity from the browser URL alone.

### Empty or unexpected data

Compare the expected Phase 3/6 state with current application readback. Look for:

- proof/demo products, inventory, orders, scenarios, replay rows, or audit fixtures
- bootstrap business records not listed in the provisioning record
- test-prefixed identifiers
- unexplained users or operators
- unexplained connectors
- records apparently belonging to another company

Do not delete automatically. Record object type, non-sensitive identifier, tenant context, likely origin, owner, and resolution.

Any unexplained foreign-tenant data is a Critical blocker.

## 3. User, Bootstrap, Login, And Authorization Verification

### Roster reconciliation

Compare the Phase 2 approved user list and Phase 4 provisioning record with actual `AccessUser` and `AccessOperator` readback.

For every approved user verify:

- full name and username/email
- correct tenant
- expected enabled/disabled state
- linked AccessOperator
- least-privilege role set
- warehouse scope
- `passwordChangeRequired` state where intended
- no accidental duplicate account

Produce exact totals:

```text
APPROVED USERS = <count>
ACTUAL CUSTOMER USERS = <count>
MATCH = YES / NO
```

### Bootstrap account review

Classify every non-customer identity as exactly one of:

- `INTERNAL REQUIRED`
- `DISABLED`
- `CONVERTED TO APPROVED CUSTOMER USER`
- `UNEXPECTED`

Record the provisioning admin disposition. No unexplained privileged bootstrap identity may remain active. An unexplained privileged account is a Critical blocker until access and history are understood.

### Login and logout

For each approved account, using a safe credential-handling process:

- login succeeds
- session resolves to the correct tenant and identity
- password-change requirement behaves as intended
- first-login flow completes without breaking access
- logout succeeds and the protected session is no longer usable

Record only `LOGIN VERIFIED YES/NO`, timestamp, route, and verifier. Never record the password.

### Allowed and denied action matrix

For every approved user, execute at least one expected allowed backend action and one expected denied backend action. Sidebar visibility is supporting UX evidence, not authorization proof.

| User | Role | Allowed test | Expected | Actual | Denied test | Expected | Actual |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  | 2xx |  |  | 403/404 as designed |  |

Use real role boundaries for `TENANT_ADMIN`, `REVIEW_OWNER`, `FINAL_APPROVER`, `ESCALATION_OWNER`, `INTEGRATION_ADMIN`, and `INTEGRATION_OPERATOR` only when those roles are assigned and in scope.

### Platform access negative test

Verify customer sessions cannot access:

- tenant creation/bootstrap administration
- cross-tenant directory or tenant-management capabilities
- platform-level configuration and secrets
- another company's administration
- platform/admin tokens or infrastructure controls

Any successful platform authority leakage is a Critical blocker.

### Cross-tenant isolation

Use an authorized safe reference tenant/object set and actual supported endpoints. Verify the company session cannot read or mutate another tenant's:

- product/catalog object
- inventory or warehouse object
- order
- connector ID
- replay record
- scenario run
- tenant settings

Expected behavior is `403`, `404`, or no disclosure according to the endpoint contract. Do not treat a different error as a pass without understanding it. Any successful cross-tenant read or mutation is a Critical blocker.

## 4. Connector Verification

### Inventory and policy reconciliation

Compare the Phase 5 connector record and Phase 7 policy baseline with actual connector readback:

- tenant, connector ID, source system, display name, and type
- `WEBHOOK_ORDER` or `CSV_ORDER_IMPORT`
- enabled/disabled state
- sync mode and cadence
- pull URL only if relevant and approved
- validation and transformation policy
- mapping version
- default-warehouse fallback and code
- notes and support owner
- no unexplained extra connector

Any drift must be explained, approved, corrected through supported APIs, and retested.

### Secret posture

Do not reveal a secret value. Verify:

- required connector secret is configured where needed
- only an approved hint/status is visible
- raw secret is absent from Git, docs, screenshots, evidence, browser payloads, and ordinary logs
- handoff/holder status and rotation procedure are recorded
- webhook operation without HMAC remains an explicit accepted operating condition when applicable

Secret exposure is a Critical blocker.

### Controlled disable/enable test

Where safe and before a live source is enabled:

1. Disable the connector through the supported control.
2. Send only an approved synthetic request.
3. Confirm inbound work is blocked/rejected/paused according to the actual connector contract and visible evidence is truthful.
4. Re-enable through the supported control.
5. Confirm the approved lane resumes with a new synthetic request.

Do not interrupt uncontrolled live traffic. If the source cannot be safely isolated, use accepted Phase 5 evidence and mark the company-specific test with an accepted condition and owner.

## 5. Catalog, Inventory, Order, And Integrity Reconciliation

### Catalog

Compare the approved Phase 6 mapping/onboarding record with `Product` readback:

- expected product count
- deterministic SKU set
- representative name/category values
- tenant ownership
- no unexplained duplicates
- no proof/test products

### Inventory

Compare expected rows by product SKU and warehouse code:

- row count
- Product association
- Warehouse association
- quantity and reorder threshold
- tenant ownership
- no orphan rows
- no unexplained negative/impossible values
- no duplicate product/warehouse identity

### Orders

Compare the accepted-source register with order readback:

- accepted order count
- external order ID uniqueness within the tenant
- tenant and warehouse
- expected status and item relationships
- no duplicate accepted order
- every rejected row accounted for in Phase 6/replay evidence
- no unexpected proof/test orders

Orders are create-only by tenant and external order ID. Do not attempt to "correct" an existing order through unsupported reimport.

### Relational integrity

Verify through application readback and approved administrative evidence where necessary:

- no inventory references a missing product
- no inventory/order references a wrong warehouse
- no order item references a missing product
- no cross-tenant relationship
- no duplicate product/warehouse inventory identity
- no duplicate tenant/external-order identity
- no duplicate successful recovery for one intended order
- all reconciliation differences are explained

Data corruption or a cross-tenant relationship is a Critical blocker.

### Source-of-truth confirmation

Before handover, record:

```text
COMPANY SYSTEM = business source of truth
SYNAPSCORE = pilot coordination, visibility, recovery, and governed decision layer
```

Document any approved exception. Unclear record ownership is a High blocker because it makes correction and rollback unsafe.

## 6. Customer-Role Readback

Using an approved customer-role session, verify:

### Dashboard

- correct tenant loads
- counts broadly agree with reconciled data
- no other-tenant data appears
- no fatal error occurs
- realtime or degraded state is truthful

### Catalog

- intended role can access representative approved products
- representative SKU/name/category matches backend reconciliation

### Inventory

- representative SKU, warehouse, quantity, and threshold/risk state match the approved record

### Orders

- representative external ID, status, items, warehouse, and operational state match backend/source reconciliation

This is focused functional readback, not another visual redesign or exhaustive control audit.

## 7. Alerts And Recommendations

### Alert verification

Use a safe deterministic condition or an approved pre-handover synthetic state:

```text
CONDITION
-> SYSTEM-GENERATED ALERT
-> CORRECT TENANT
-> EXPECTED TYPE/SEVERITY/STATE
-> EXPECTED USER CAN SEE IT
```

Record the condition, timestamp, non-sensitive evidence reference, observed alert, tenant, severity/state, and role visibility. Do not claim a test if no condition was created or observed.

### Recommendation verification

Where deterministic generation is supported:

```text
CONDITION
-> SYSTEM-GENERATED RECOMMENDATION
-> SUPPORTING EVIDENCE
-> CORRECT TENANT
-> EXPECTED PRIORITY/ROLE VISIBILITY
-> NO UNEXPECTED BUSINESS MUTATION
```

Recommendations are decision support. Confirm observing a recommendation does not automatically change inventory, orders, or another business record.

There is no generic alert rule builder, recipient model, recommendation rule engine, dismissal/suppression, or automatic recommendation execution. Carry those as scope or operating conditions rather than failed tests.

## 8. Replay And Recovery

Use one deterministic synthetic inbound failure in an isolated pre-handover lane:

```text
FAILED INBOUND
-> VISIBLE FAILURE EVIDENCE
-> CORRECT TENANT
-> ELIGIBILITY CONFIRMED
-> AUTHORIZED ROLE
-> MANUAL REPLAY
-> EXPECTED LIVE-FLOW RESULT
-> AUDIT/HISTORY CONFIRMATION
-> NO DUPLICATE
```

Verify the operator has the required integration role and warehouse scope. Confirm the replayed order/result appears once and the resolved record cannot produce an unsafe duplicate.

Execute at least one negative test:

- unauthorized user cannot replay, or
- ineligible/blocked record cannot replay, or
- already handled record cannot create a duplicate

An uncontrolled duplicate replay, wrong-tenant replay, or replay by an unauthorized actor is a Critical blocker. A broken required replay lane is High.

## 9. Approval, Separation Of Duty, And Scenarios

### Approval verification

If approvals are in pilot, use a safe `ScenarioRun` fixture:

```text
PENDING
-> AUTHORIZED REVIEW/APPROVAL OR REJECTION
-> PERSISTED EXPECTED STATE
```

Also prove an unauthorized role is denied. If approvals are out of scope, record `OUT OF PILOT`; do not invent another workflow.

### Separation of duty

Compare the Phase 4 role matrix and Phase 7 governance baseline:

- requester
- review owner
- final approver
- escalation owner
- users who hold multiple or conflicting roles
- technical enforcement versus procedural control

Escalated scenarios technically enforce important requester/reviewer/final-approver separation, while broader separation remains partly procedural. Record the operating control and owner. If the approved company governance cannot be maintained, classify the actual risk; wrong governance authority is Critical.

### Scenario verification

If scenarios are in pilot, use approved deterministic test data:

```text
PREVIEW -> COMPARE -> SAVE -> GOVERNANCE -> APPROVED EXECUTION
```

Confirm execution creates the expected `OrderService` result for the intended tenant and warehouse only. If scenarios are out of scope, record `OUT OF PILOT` and do not execute.

## 10. Settings, Warehouses, And Operator Scope

### Tenant settings

Compare actual supported settings with the frozen Phase 7 record:

- tenant name and description
- password rotation days
- session timeout minutes
- warehouse metadata
- connector support/policy
- tenant operational policy

Do not record or test fictional workspace settings.

### Warehouses and locations

Verify code, name, location, and operational scope against the approved records. Confirm spelling variants did not create duplicate identities.

### Operator scope

For every AccessOperator record:

| Operator | Role(s) | Warehouse scope | Operational lane | Expected access | Result |
| --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |

An empty warehouse scope means tenant-wide warehouse access in the current model. It must be intentional. Connector-specific scopes do not exist; compensate through roles, warehouse scopes, and operating procedure.

## 11. Realtime, Runtime, And Failure Honesty

### Realtime

Verify:

- `/ws/info` responds
- an approved user establishes an authenticated realtime connection
- one controlled tenant event reaches the expected screen
- no event from the safe reference tenant appears in the company session
- reconnect/fallback behavior is understood

One controlled company event path is sufficient. This is not a load test.

### Degraded realtime

Using an already verified safe mechanism or accepted platform evidence, confirm a lost/degraded connection does not present a fake live state. Verify `reconnecting`/`degraded` truth and the implemented polling/manual fallback behavior. Do not disrupt a customer operating window merely to create evidence.

### Runtime and trust

Verify `/api/system/runtime` and the Runtime surface:

- evidence is visible to the intended approved role
- status is truthful and read-only
- customer users cannot mutate infrastructure through Runtime
- readiness, realtime broker, dispatch, connector diagnostics, and incidents are interpreted according to the actual payload

### Failure honesty

Use accepted tests or one safe controlled path to confirm failure does not become:

- fake healthy state
- fake success message
- infinite loading
- hidden connector failure
- hidden blocked replay

Record the reused proof/control evidence or the company-specific observation.

## 12. Backup, Restore, Security, And Actuator Gate

### Backup

Before handover confirm:

- backup procedure and owner exist
- most recent successful backup/checkpoint is identified
- artifact is outside Git
- checksum exists where applicable
- retention expectation is recorded
- restore operator and recovery owner are named
- restore procedure is understood

Backup absence is a High blocker.

### Restore readiness

Carry the current evidence truth:

| Layer | Current status |
| --- | --- |
| Application-level PostgreSQL backup/restore | Proven through Gate 2 evidence. |
| Provider-level Render restore | Documented limitation unless later company-specific evidence proves it. |

Record whether the pilot owner explicitly accepts the provider condition. Do not perform a destructive provider restore solely for Phase 8.

### Security baseline

Reuse accepted security evidence and add company-specific access checks for:

- authentication and session behavior
- role authorization
- tenant isolation
- production CORS posture
- actuator restriction
- secret handling
- auth rate limiting
- sensitive error leakage

This is not a new penetration test.

### Actuator

Verify deployed behavior:

| Endpoint | Expected |
| --- | --- |
| `/actuator/health` | Healthy response according to deployed contract. |
| `/actuator/health/liveness` | Healthy. |
| `/actuator/health/readiness` | Healthy, including required dependencies. |
| `/actuator/metrics` | Restricted from public/customer access. |
| `/actuator/prometheus` | Restricted from public/customer access. |

Unhealthy readiness blocks handover. Public exposure of restricted operational endpoints is a security blocker requiring classification.

## 13. Pilot Envelope And Dataset Size

Compare the actual proposal with accepted evidence:

| Dimension | Current controlled-pilot evidence/recommendation | Company actual | Result |
| --- | --- | --- | --- |
| Tenants/workspaces | 1 |  |  |
| Active operators | 3-5 recommended; 25 authenticated local read operators proven |  |  |
| Connector lanes | 1 initially |  |  |
| Realtime clients | 50 local clients proven |  |  |
| Read throughput | About 41 RPS local soak, p95 under 500 ms |  |  |
| Products | Bounded pilot dataset; record actual count |  |  |
| Inventory rows | Bounded pilot dataset; record actual count |  |  |
| Order rate | Bounded approved lane; record actual rate |  |  |
| Inbound rate | Bounded approved lane; record actual rate |  |  |

Classify each data/load dimension as:

- `WITHIN PILOT ENVELOPE`
- `TECHNICAL REVIEW REQUIRED`
- `OUTSIDE CURRENT EVIDENCE`

Do not convert local proof into a claim about Render saturation, HA, multi-region, or enterprise scale.

## 14. Focused Responsive And Control Reachability

Gate 4 already established 201/201 control classifications. Phase 8 performs only company-specific checks at the primary operating viewports:

- login and first-login controls are reachable
- navigation is usable
- primary in-scope pilot actions are reachable
- company name, SKU, external ID, and warehouse values do not clip or block critical actions
- no company-specific data causes horizontal overflow or inaccessible controls

Record viewport(s), representative long values, and result. Do not reopen the full UI hardening program.

## 15. Customer Scope, Limitations, Support, And Stop Conditions

### Feature-scope matrix

For every Phase 7 feature:

- `IN PILOT`: configured and tested in Phase 8
- `OUT OF PILOT`: not represented to the customer as an enabled capability
- `INTERNAL ONLY`: inaccessible or not presented as a customer operating surface

Cover Dashboard, Catalog, Inventory, Orders, Alerts, Recommendations, Integrations, Replay, Approvals, Scenarios, Runtime, Settings, Users/Admin, and platform administration.

### Known limitations

Classify every applicable limitation as:

- `ACCEPTED OPERATING CONDITION`
- `OUT OF PILOT`
- `MUST FIX BEFORE HANDOVER`
- `POST-PILOT`

At minimum review:

- no MFA/SSO/invitations/customer forgot-password flow
- no formal read-only role
- no connector-specific scope
- no webhook HMAC verification
- no arbitrary mapping UI
- no inventory CSV
- no per-import rollback
- no automatic retention cleanup
- no generic alert/recommendation rule engine or recipient model
- no universal separation-of-duty engine
- provider-level restore evidence limitation

### Support ownership

Require a named person or accountable role for:

- deployment
- backup
- restore
- incident response
- access provisioning
- connector support
- company business contact
- company technical contact
- rollback

No critical responsibility may remain `TBD`.

### Customer support path

Confirm the internal intake and escalation path for:

- login issue
- data mismatch
- connector issue
- replay problem
- approval issue
- system outage

Phase 8 verifies readiness of this path. Phase 9 will create customer-facing handover material.

### Pilot stop conditions

The minimum immediate-stop set is:

- tenant leakage
- authorization bypass
- data corruption
- wrong-tenant write
- uncontrolled duplicate replay
- wrong governance action
- secret exposure
- unrecoverable database failure
- severe repeated availability failure

For each, identify who can declare `PAUSE`, who investigates, who authorizes `ROLLBACK`, and how Company 1 returns to its source system.

## 16. Handover Decision

The company-specific final decision must be exactly one:

- `AUTHORIZED FOR CUSTOMER HANDOVER`
- `AUTHORIZED FOR CUSTOMER HANDOVER WITH ACCEPTED OPERATING CONDITIONS`
- `NOT AUTHORIZED - PRE-HANDOVER BLOCKERS REMAIN`

If only the checklist or a proof/rehearsal environment was assessed:

- `CHECKLIST/PROCESS READY - COMPANY-SPECIFIC EXECUTION REQUIRED`

The record requires sign-off from:

- Technical Verifier
- Platform Owner
- Pilot Owner
- date/time
- accepted conditions

Phase 9 cannot begin for a real customer without this authorization.

## 17. Automation Coverage And Script Mapping

### Primary automation decision

`scripts/verify-company-readiness.ps1` is not the primary live Company 1 execution layer.

It creates a synthetic tenant, users, operators, connectors, orders, inventory pressure, replay records, and scenario executions. That mutation is appropriate for local/self-host rehearsal but unsafe as a generic read-only pre-handover verifier for an already configured customer tenant.

No new wrapper is added in Phase 8. A safe useful wrapper would still require authenticated company-specific sessions and object identifiers, while roster approval, source reconciliation, separation of duty, backup acceptance, support ownership, and handover authorization remain human evidence. Adding a shallow endpoint wrapper would risk overstating coverage.

### Current script mapping

| Existing script behavior | Phase 8 area | Classification | Limitation |
| --- | --- | --- | --- |
| Checks public and authenticated frontend route HTTP responses | Focused route availability | `AUTOMATED BY SCRIPT` in rehearsal | Does not prove the approved customer can use each route or that company data renders correctly. |
| Signs in as seed and generated tenant users | Auth/session | `AUTOMATED BY SCRIPT` in rehearsal | Uses seed/generated credentials, not the approved company roster. |
| Checks readiness, dashboard, runtime, incidents | Runtime/trust | `AUTOMATED BY SCRIPT` in rehearsal | Does not replace live company deployment checks or operator interpretation. |
| Creates a synthetic tenant and checks tenant-scoped workspace | Tenant provisioning | `AUTOMATED BY SCRIPT` in rehearsal | Mutates state and is not Company 1 provisioning or pre-handover evidence. |
| Reads and mutates workspace/security/warehouse settings | Settings | `AUTOMATED BY SCRIPT` in rehearsal | Does not compare the actual company baseline to approved records. |
| Creates operator/user and tests one denied admin action | User/role boundary | `AUTOMATED BY SCRIPT` in rehearsal | Does not reconcile every approved user, login/logout, bootstrap identity, or role pair. |
| Creates connectors and changes support ownership | Connector setup | `AUTOMATED BY SCRIPT` in rehearsal | Does not verify customer secret custody, exact approved policy, or unexplained extra connectors. |
| Sends valid webhook and disabled CSV paths | Integration behavior | `AUTOMATED BY SCRIPT` in rehearsal | Uses synthetic source systems and payloads. |
| Creates inventory pressure and reads alerts/recommendations | Intelligence visibility | `AUTOMATED BY SCRIPT` in rehearsal | Does not prove Company 1's frozen thresholds or customer-specific visibility. |
| Creates fulfillment delay | Operational processing | `AUTOMATED BY SCRIPT` in rehearsal | Not a Phase 8 requirement unless in company scope. |
| Creates failure, replays with scoped operator, checks queue clears | Replay | `AUTOMATED BY SCRIPT` in rehearsal | Does not prove company-specific replay rules or all negative cases. |
| Saves, approves, and executes standard/escalated scenarios | Approval/scenario | `AUTOMATED BY SCRIPT` in rehearsal | Creates live test orders and cannot be used if scenarios are out of pilot. |
| Reads dashboard snapshot, events, audit, runtime | Readback/trust | `AUTOMATED BY SCRIPT` in rehearsal | Does not perform source reconciliation or prove no cross-tenant disclosure. |
| Exact Phase 2-7 evidence completeness | Evidence gate | `MANUAL CHECK` | Not represented in runtime data. |
| Actual roster and bootstrap disposition | User reconciliation | `MANUAL CHECK` | Requires approved business records and identity review. |
| Full allowed/denied matrix and platform negative tests | Authorization | `NOT CURRENTLY COVERED` | Existing script covers one representative denial only. |
| Cross-tenant object read/mutation matrix | Tenant isolation | `NOT CURRENTLY COVERED` | Must use authorized safe reference objects and actual company session. |
| Exact catalog/inventory/order reconciliation and integrity | Data | `MANUAL CHECK` | Requires approved source counts and deterministic identifier comparison. |
| Secret/log/browser leakage review and rotation custody | Secret posture | `MANUAL CHECK` | Must not be automated by printing secret values. |
| Realtime company event and no foreign-tenant event | Realtime | `NOT CURRENTLY COVERED` by this script | Hosted proof covers a proof tenant, not Company 1. |
| Backup artifact/checksum/owner/provider acceptance | Recovery | `MANUAL CHECK` | `verify-company-readiness.ps1` does not prove backup or restore. |
| Company scale/data-envelope comparison | Capacity | `MANUAL CHECK` | Requires proposed use and accepted evidence review. |
| Support ownership, limitations, stop conditions, sign-off | Governance | `MANUAL CHECK` | Human accountability cannot be inferred from endpoint health. |

Supporting automation remains useful:

| Tool | Safe Phase 8 use |
| --- | --- |
| `scripts/check-live-connections.ps1` | Deployment prerequisite: frontend, backend, DB readiness, auth, and websocket reachability. |
| `scripts/secret-scan.ps1` | Repository secret/leakage baseline; does not scan private infrastructure or all runtime logs. |
| `scripts/verify-company-readiness.ps1` | Local/self-host synthetic workflow rehearsal only. |
| `scripts/prepare-hosted-proof.ps1` plus hosted Playwright proof | Platform-wide deployed proof tenant validation; never substitute it for company-specific verification. |
| Gate 4 control execution tooling | Reuse accepted product-control evidence; perform focused company-value reachability checks separately. |

## 18. Rehearsal Policy

When no real company environment exists:

- use only an approved proof/rehearsal tenant
- mark the environment and every artifact `REHEARSAL`
- do not use real customer data or credentials
- do not record a Company 1 authorization verdict
- record which checklist sections were exercised
- record which checks remain company-specific

The allowed rehearsal result is:

`CHECKLIST/PROCESS READY - COMPANY-SPECIFIC EXECUTION REQUIRED`

At document creation, no Phase 8 company-specific rehearsal was run. The existing hosted proof and local readiness evidence were inspected as supporting platform evidence only.

## 19. Phase 9 Handoff Contract

Only after a real tenant is authorized, Phase 9 may receive:

- company name and verified tenant identity
- approved users and access status, without passwords
- frontend URL
- first-login requirement
- customer-visible feature scope
- customer-visible operating conditions
- support contact and escalation path
- pilot start date
- stop/rollback path
- signed handover authorization

Do not create customer handover material in Phase 8.

## Completion Record

Complete [company-pre-handover-verification-record.md](templates/company-pre-handover-verification-record.md). Store only sanitized evidence references. The record is incomplete until every applicable row has a valid result, all blockers have disposition, accepted conditions have owners, and all required sign-offs are present.
