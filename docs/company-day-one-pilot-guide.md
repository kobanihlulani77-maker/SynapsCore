# Company 1 Day-One Pilot Guide

This is the canonical SynapseCore Phase 10 operating guide for the first real day of an authorized Company 1 pilot.

It begins only after the company tenant has passed Phase 8 pre-handover verification and Phase 9 customer handover. It does not provision a tenant, create users, deliver credentials, expand scope, replace the source system, or authorize unsupported recovery behavior.

Day One is designed to be controlled, observable, reversible, and truthful.

## Operating Principles

- Truth over fake success.
- Evidence before assumption.
- Intelligence before automation.
- Human governance for high-impact actions.
- Company source systems remain authoritative during the controlled pilot.
- Failure must be visible.
- Recovery must be controlled.
- No unsupported pilot claims.

The operating sequence is:

```text
Verify entry conditions
-> open the operating window
-> confirm identity and scope
-> reconcile first live data
-> observe realtime and intelligence
-> govern consequential actions
-> classify and control failures
-> make GO / HOLD / STOP decisions
-> close and record Day One
```

Use [company-day-one-pilot-record.md](templates/company-day-one-pilot-record.md) as the controlled evidence record. Store references to approved evidence, not credentials, tokens, raw customer payloads, or platform secrets.

## Phase Boundary

Phase 10 starts after:

- [Company Pilot Intake](company-1-pilot-intake-pack.md) defines the approved company problem, users, data, systems, scope, and success criteria;
- [Tenant And Workspace Provisioning](company-tenant-workspace-provisioning-runbook.md) establishes the tenant boundary;
- [User Provisioning](company-user-provisioning-runbook.md) reconciles bootstrap identities and approved roles;
- [Integration Setup](company-integration-setup-runbook.md) verifies the approved connector lane;
- [Data Onboarding](company-data-onboarding-runbook.md) reconciles the approved bounded dataset;
- [Operational Configuration](company-operational-configuration-runbook.md) freezes alerts, recommendations, replay, governance, settings, and runtime expectations;
- [Pre-Handover Verification](company-pre-handover-verification-checklist.md) authorizes customer handover; and
- [Customer Handover](company-customer-handover-procedure.md) completes approved first-login readiness.

Phase 10 does not repeat those procedures. It consumes their approved records and operates inside their boundaries.

## 1. Day-One Entry Conditions

Every applicable condition must be confirmed before the operating window opens.

| Entry condition | Required evidence | Owner | Failure decision |
| --- | --- | --- | --- |
| Actual Company 1 tenant/workspace verified | Phase 3 provisioning record and Phase 8 tenant identity result | SynapseCore Platform Owner | Do not start |
| Handover authorized | Signed Phase 8 decision is `AUTHORIZED FOR CUSTOMER HANDOVER` or `AUTHORIZED FOR CUSTOMER HANDOVER WITH ACCEPTED OPERATING CONDITIONS` | Pilot Owner | Do not start |
| Required customer handover complete | Phase 9 record shows required users at `FIRST LOGIN VERIFIED` or `HANDOVER COMPLETE` | Access Owner | Do not start for affected user; hold if role is essential |
| Approved users only | Phase 2 roster reconciled with active Phase 4 accounts/operators | Tenant Admin / Access Owner | Do not start |
| Bootstrap/internal identities disposed correctly | Phase 4/8 record explains disabled, rotated, retained, or removed access | Access Owner | Do not start if privileged identity is unexplained |
| Roles confirmed | Actual role assignments match the approved responsibility matrix | Tenant Admin / Pilot Owner | Do not start affected workflow |
| Warehouse scopes confirmed | Each scoped operator sees only approved warehouses; empty scope is intentionally tenant-wide | Tenant Admin / Access Owner | Do not start affected workflow |
| Approved connector configured | Phase 5 connector identity, type, policy, source, and support owner match readback | Integration Admin / SynapseCore Integration Owner | Hold connector lane |
| Approved data loaded and reconciled | Phase 6 counts, identifiers, mappings, and source comparisons accepted | Data Owner | Hold affected data lane |
| Operational policies baselined | Phase 7 baseline and feature-scope matrix frozen | Pilot Owner / Tenant Admin | Hold affected workflow |
| Tenant Runtime trust checked | Tenant Runtime shows an understood `SAFE`, `WATCH`, or accepted controlled condition | Customer operating owner | Do not start sensitive workflow if `STOP` |
| Platform Runtime checked | Platform Owner confirms platform readiness and no unexplained degradation | SynapseCore Platform Owner | Do not start if platform trust is insufficient |
| Realtime checked | WebSocket/SockJS trust evidence and authenticated tenant behavior verified | SynapseCore Platform Owner | Start only with explicit degraded-mode decision if safe |
| Backup/recovery evidence current | Application-level restore evidence current; provider limitation accepted and recorded | Backup/Restore Owner | Do not expand reliance; stop if agreed recovery prerequisite is absent |
| Support contacts confirmed | Named Company and SynapseCore contacts, channels, and escalation authority | Pilot Success Owner | Do not start |
| Pilot scope confirmed | One workspace, approved users, approved connector lane, approved workflows, bounded data, and operating window recorded | Pilot Owner | Do not start |
| Customer acknowledgement complete | Phase 9 acknowledgement covers scope, source-of-truth, limitations, support, and stop path | Customer Pilot Owner | Do not start |

If any required condition is false, unknown, stale, or unsupported:

`DAY ONE DOES NOT START.`

An out-of-scope feature is not an entry failure. It remains unavailable and is not demonstrated or used.

## 2. People And Responsibilities

Use the actual Phase 2 approved roster. The six supported tenant roles are capabilities, not a requirement for six different people. Combining roles requires explicit approval and must not undermine the agreed separation of duty.

| Participant | Day-One responsibility | Owns | Does not own |
| --- | --- | --- | --- |
| SynapseCore Platform Owner / Operator | Opens and monitors the platform operating window | Platform health, tenant support state, platform Runtime, platform Activity, release trust, deployment escalation | Customer business decisions or casual inspection of raw customer payloads |
| Customer Tenant Admin | Confirms tenant identity, user posture, roles, warehouse scopes, and approved tenant configuration | Tenant access/configuration issues inside approved change control | Platform administration, connector operations without integration role, governance decisions without governance role |
| Customer Integration Admin | Confirms approved connector policy and technical lane | Connector configuration, enablement state, support ownership, authorized replay | Business-source correction without source owner approval; tenant/platform administration |
| Customer Integration Operator | Observes imports/failures and performs authorized replay | Evidence review and approved replay within tenant/warehouse scope | Connector mutation, governance decisions, replay when safety is uncertain |
| Customer Governance Roles | Review, approve/reject, or acknowledge assigned scenarios | Assigned governance stage and evidence | Substituting for another governance role or acting outside warehouse scope |
| Customer Business/Operations Observers | Confirm business interpretation and source-system truth | Source comparison, business impact, operator feedback | High-impact application actions without assigned authority |
| Customer Data/Source Owner | Resolves source values, mappings, and reconciliation questions | Source correctness and approved corrections | Hidden database changes or unsupported SynapseCore overrides |
| Pilot Owner | Controls scope and checkpoint decisions | GO/HOLD/STOP decision with named technical and business owners | Ignoring a security, isolation, corruption, or authority stop condition |

Responsibility routing:

| Problem | First owner | Required escalation |
| --- | --- | --- |
| Login or access problem | Tenant Admin / Access Owner | SynapseCore support if session or backend issue |
| Tenant configuration issue | Tenant Admin | Pilot Owner before high-impact change |
| Source-data mismatch | Customer Data/Source Owner | SynapseCore Data Owner for mapping/readback evidence |
| Connector issue | Integration Admin | SynapseCore Integration Owner if backend/runtime involved |
| Replay decision | Authorized Integration Operator/Admin | Source owner and Pilot Owner when duplicate or business risk exists |
| Governance decision | Assigned governance role | Pilot Owner if assignment, evidence, or separation is unclear |
| Degraded platform condition | SynapseCore Platform Owner | Deployment/Incident Owner |
| Pilot hold/stop | Pilot Owner with Customer Operations Owner | Security/Incident Owner for mandatory stop conditions |

## 3. First 30 Minutes

Do not generate artificial traffic merely to make the dashboard move.

### Minute 0-10: Platform opening

SynapseCore Platform Owner:

1. Confirm the approved release identity and operating window.
2. Open Platform Overview.
3. Confirm Company 1 appears once in Tenant Directory with the expected support state.
4. Open Platform Runtime and confirm liveness/readiness and dependencies are trustworthy.
5. Review Platform Activity for unexplained platform or tenant support conditions.
6. Review Release Trust and confirm the deployed release matches the frozen pilot build.
7. Confirm no Critical or High unresolved condition exists.
8. Record `GO`, `HOLD`, or `STOP` for platform opening.

The Platform Owner does not use this opening check to browse raw Company 1 orders, inventory, product data, inbound payloads, replay payloads, connector secrets, or credentials.

### Minute 10-20: Tenant and identity opening

Customer Tenant Admin:

1. Sign in through the approved frontend URL.
2. Confirm the Company 1 tenant/workspace code and displayed identity.
3. Confirm expected Users and Company Settings navigation.
4. Compare the active user roster, roles, operator links, and warehouse scopes with Phase 4/8 evidence.
5. Confirm no unexpected privileged or bootstrap account is active.
6. Confirm the tenant configuration still matches the frozen Phase 7 baseline.

Each required customer user:

1. Signs in using their own account.
2. Confirms their identity and assigned role.
3. Confirms intended navigation is visible and irrelevant navigation is absent.
4. Confirms the assigned warehouse scope.
5. Opens one expected read surface.
6. Reports any foreign, unexpected, privileged, or missing information immediately.

### Minute 20-30: Connector and operating-lane opening

Integration Admin/Operator:

1. Confirm the approved connector identity, type, source system, enabled state, and expected policy.
2. Inspect recent import/connector evidence for unexplained failures or stale state.
3. Confirm Replay Queue state without replaying anything.
4. Confirm the operator is in the intended tenant and warehouse scope.
5. Confirm source-system contacts are reachable.

Customer operating users:

1. Open only the pages in the approved pilot scope.
2. Confirm representative data belongs to Company 1 and the intended warehouse.
3. Confirm empty states are credible rather than filled with demo/proof records.

If anything is unexpected, stop progression and classify it before live operation continues.

## 4. First Live Data Confirmation

Dashboard appearance alone is not proof. For every in-scope domain, compare:

```text
Source value -> SynapseCore value -> Expected interpretation
```

| Domain | Source evidence | SynapseCore readback | Confirmation |
| --- | --- | --- | --- |
| Catalog/products | Approved source SKU, name, price, reorder threshold, and identifier mapping | Catalog page/API for same tenant SKU | Identity and business meaning match; no proof/demo or foreign records |
| Inventory | Source product, warehouse, on-hand/available quantity, and relevant threshold | Inventory page/API for same SKU and warehouse | Quantity and warehouse interpretation match the Phase 6 baseline |
| Orders | Source external order ID, warehouse, items, quantities, status, and time | Orders page/API and detail panel | Order appears once, belongs to correct warehouse, and status meaning is understood |
| Alerts | Source operational condition and configured policy | Alerts page/API | Alert condition, severity, explanation, and state match evidence |
| Recommendations | Source operational pressure and supporting data | Recommendations page/API | Recommendation is understandable decision support, not an automatic action |
| Connector evidence | Source send/import reference and timestamp | Integrations/import history | Source, type, outcome, counts, and failure evidence match |
| Activity | Known user/system action and timestamp | Tenant Activity/Audit | Tenant-scoped event is attributable and contains no foreign activity |

Do not require domains excluded by the approved pilot. Mark them `OUT OF PILOT` in the Day-One record.

For each sampled record, capture identifiers and sanitized evidence references. Do not copy raw payloads into the record.

## 5. Source-Of-Truth Rule

During the controlled pilot, Company 1's approved source systems remain authoritative.

If SynapseCore and the source disagree:

1. Do not silently overwrite the source.
2. Pause the affected workflow.
3. Record the source value, SynapseCore value, time, tenant, warehouse, and evidence reference.
4. Classify the discrepancy as source data, mapping, timing, connector, application interpretation, or unknown.
5. Confirm the correct value with the Customer Data/Source Owner.
6. Correct source data, mapping, connector configuration, or SynapseCore data only through an authorized supported path.
7. Re-run reconciliation.
8. Resume only after the owner records an accepted result.

Manual production database edits are not an authorized correction path.

## 6. Realtime Confirmation

Realtime is a trust capability, not decoration.

Day-One confirmation includes:

1. Platform Owner confirms the approved `/ws/info` or equivalent connection evidence is responsive.
2. A signed-in tenant user observes the product connection state.
3. One naturally occurring or explicitly approved safe change is compared with its source timestamp.
4. The expected tenant page updates without a manual refresh when the connection is live.
5. The same change is verified through the authoritative API/snapshot state.
6. The observer confirms no other tenant or unauthorized warehouse data appeared.

Current behavior uses the `/ws` SockJS/STOMP endpoint and tenant-scoped topics. When the frontend cannot sustain realtime, it can show `reconnecting` or `degraded` and use periodic snapshot refresh. A degraded fallback is not equivalent to a live connection.

If realtime is degraded:

- state the condition truthfully;
- verify backend readiness and tenant snapshot reads;
- tell operators which screens may update late;
- use approved manual refresh or degraded polling;
- avoid time-sensitive consequential actions when freshness cannot be established;
- classify the affected workflow `HOLD` if stale state could cause harm.

## 7. First Alert Or Recommendation

Use this sequence:

```text
Operational condition
-> source and SynapseCore evidence
-> alert or recommendation
-> human interpretation
-> permitted next step
-> observed outcome
```

Operator procedure:

1. Confirm the alert/recommendation belongs to Company 1 and an in-scope warehouse/domain.
2. Open the supporting inventory, order, fulfillment, connector, or Runtime evidence.
3. Compare the underlying source-system state.
4. Decide whether the item is informational, requires investigation, or requires an authorized workflow.
5. Use only a supported action and assigned role.
6. Record the interpretation and outcome if it affects the pilot lane.

Recommendations are advisory. They do not automatically mutate inventory, create orders, approve scenarios, replay failures, or execute business actions. Operators must not treat every recommendation as an instruction to execute.

## 8. First Governance Event

Run this only if scenarios/governance are explicitly in Company 1 pilot scope and the action is safe for the approved data lane.

```text
Preview
-> compare if useful
-> save plan
-> assigned REVIEW_OWNER decision
-> assigned FINAL_APPROVER decision when escalated
-> ESCALATION_OWNER acknowledgement when applicable
-> governed execution by REVIEW_OWNER or FINAL_APPROVER
-> live order/effects
-> history, Activity, and realtime confirmation
```

Before starting:

- verify tenant, warehouse, SKU lines, quantities, prices, requester, and business intent;
- verify the saved plan and current approval stage;
- verify role assignment and separation of duty;
- verify Runtime is trustworthy;
- understand that execution creates a live order and related inventory/fulfillment side effects.

Authority rules:

- `REVIEW_OWNER` acts only on assigned review-stage work and may execute only an approved saved plan with stored request payloads;
- `FINAL_APPROVER` acts only on assigned final-stage work and may execute only an approved saved plan with stored request payloads;
- `ESCALATION_OWNER` acknowledges assigned escalation conditions but does not approve or execute by that role alone;
- `TENANT_ADMIN`, `INTEGRATION_ADMIN`, and `INTEGRATION_OPERATOR` do not gain scenario execution authority from those roles;
- scenario previews are loadable planning evidence, not executable live-order commands;
- wrong-role, wrong-tenant, wrong-stage, or wrong-warehouse attempts must be denied.

SynapseCore does not expose a generic Approval entity. Approval is the governed lifecycle of a saved `ScenarioRun`.

## 9. First Integration Failure

Use this controlled process:

```text
Failure observed
-> stop assumptions
-> preserve evidence
-> identify cause
-> correct the prerequisite through a supported path
-> confirm replay authorization and eligibility
-> check duplicate risk
-> replay once if safe
-> verify live result and audit evidence
```

Required checks before replay:

- correct Company 1 tenant;
- correct connector and source system;
- correct external order ID;
- correct warehouse and operator warehouse scope;
- understood failure code/message;
- corrected source/prerequisite;
- no existing successful order for the same tenant/external ID;
- record is eligible and not dead-lettered;
- connector state is understood;
- operator has `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN`;
- source owner agrees where business truth may have changed.

Replay only once, then verify the resulting order/effect appears once and the replay record, import evidence, Activity, and realtime state agree.

`WHEN IN DOUBT, DO NOT REPLAY.`

## 10. Recovery Boundary

The proven Company 1 recovery lane is:

```text
CSV failed inbound
-> visible failure evidence
-> supported correction
-> authorized replay
-> duplicate-safe result
```

Do not advertise disabled-webhook recovery as proven.

Current accepted limitation:

- disabled `WEBHOOK_ORDER` ingestion can return `403` and record failure/import evidence while the corresponding filtered replay queue remains empty on Render;
- local integration proof expects a `CONNECTOR_DISABLED` pending replay row;
- the mismatch is Medium because disabled-webhook replay is outside the approved Company 1 recovery lane;
- if Company 1 requires this recovery behavior, it becomes a High blocker and the access/recovery gate must reopen before enablement.

If webhook ingestion is configured and verified, normal ingestion may be used within scope. For the disabled-webhook failure/readback case, preserve evidence and escalate. Do not improvise a manual database fix or promise replay recovery.

## 11. Tenant Activity And Platform Activity

Tenant Activity answers: what happened inside this tenant's operational boundary?

Company tenant users may see appropriate tenant-scoped operational/audit activity according to their access. The activity can support order, inventory, connector, replay, scenario, and user-action interpretation.

Platform Activity answers: which platform or tenant support condition needs platform attention?

The Platform Owner sees metadata-level platform/support activity. The normal Platform Control Plane must not expose raw Company 1 orders, order items, product rows, inventory rows, inbound/replay payloads, connector secrets, or customer credentials.

Any foreign tenant activity on a Company 1 tenant surface is a `STOP` condition.

## 12. Runtime Trust

The two views answer different questions.

| View | Question | Audience | Expected content |
| --- | --- | --- | --- |
| Tenant Runtime (`/runtime`) | Is my tenant operating correctly? | Signed-in Company 1 tenant users | Tenant-safe readiness, connector/replay/import/alert/fulfillment posture, realtime mode, incidents, and trust signals |
| Platform Runtime (`/system-config` and `/releases`) | Is SynapseCore infrastructure/platform operating correctly? | Dedicated Platform Owner session | Platform readiness, deployment/build identity, aggregate posture, and platform configuration/trust metadata |

Tenant users do not receive platform profiles, CORS origins, service/instance identity, platform authority, or the global tenant directory. Platform Owner support remains metadata-first and does not include tenant impersonation or casual raw-payload browsing.

Interpret tenant Runtime classifications as:

- `SAFE`: continue inside approved pilot scope;
- `WATCH`: continue only with named observation and understood impact;
- `STOP`: pause sensitive operations and restore trust first.

## 13. Day-One Observation Windows

Use relative checkpoints unless Company 1 approved exact times.

| Checkpoint | Required inspection | Decision owner |
| --- | --- | --- |
| `T+0` | Entry conditions, release identity, platform/tenant Runtime, identity/scope, connector baseline, known limitations | Platform Owner and Pilot Owner |
| `T+30 minutes` | First sign-ins, expected navigation, first live data samples, no foreign data, realtime state, support issues | Tenant Admin and Pilot Owner |
| `T+2 hours` | Connector/import outcomes, failed inbound, source reconciliation, alerts/recommendations, governance state, operator feedback | Integration Owner, Data Owner, Operations Owner |
| `Midday` | Platform health, degraded periods, unresolved issues, replay decisions/results, scope drift, customer confidence, GO/HOLD/STOP | Pilot Owner and Customer Operations Owner |
| `End of day` | Full reconciliation, actions, audit/activity, incidents, unresolved risk, accepted limitations, next-day baseline | All named owners |

At every checkpoint record one of `GO`, `HOLD`, or `STOP`. Silence is not a decision.

## 14. Issue Classification

Use one primary type and any relevant secondary type.

| Type | Meaning | Example |
| --- | --- | --- |
| `ACCESS` | Identity, login, role, session, warehouse scope, or authorization | Approved operator cannot sign in; unexpected route authority |
| `DATA` | Source/readback mismatch, mapping, integrity, duplication, or corruption | Inventory quantity differs from source |
| `CONNECTOR` | Connector state, credential, mapping, inbound, or telemetry | Approved CSV import fails validation |
| `REPLAY` | Failure visibility, eligibility, duplicate safety, or replay outcome | Pending record absent or replay creates unclear result |
| `GOVERNANCE` | Scenario assignment, stage, separation, approval, rejection, acknowledgement, or execution | Wrong role can attempt final approval |
| `REALTIME` | WebSocket, subscription, freshness, reconnect, or degraded fallback | UI remains reconnecting while snapshot reads work |
| `PLATFORM` | Readiness, database, Redis/session, deployment, release, or platform Runtime | Backend readiness fails |
| `USER EXPERIENCE` | User can operate safely but presentation or workflow causes confusion | Action state is unclear but authority is correct |
| `SCOPE / AUTHORIZATION` | Requested behavior is outside approved pilot or unsupported | Customer asks for disabled-webhook replay |

Every issue record includes:

- issue ID;
- what happened;
- observer;
- Company 1 tenant/workspace;
- date/time and timezone;
- sanitized evidence reference;
- affected domain/warehouse;
- user and business impact;
- severity;
- owner;
- current state;
- immediate control;
- next action;
- checkpoint decision;
- resolution and verification.

Do not place secrets, credentials, tokens, session cookies, raw payloads, or unnecessary personal data in issue records.

## 15. Stop And Degradation Conditions

### Mandatory STOP

- cross-tenant data exposure;
- unauthorized Platform Control Plane access;
- unexpected raw tenant data exposure through platform surfaces;
- warehouse-scope bypass;
- authentication or authorization bypass;
- unauthorized consequential action;
- duplicate consequential execution or repeated incorrect replay into live flow;
- unexplained source-versus-SynapseCore mismatch affecting safe operation;
- loss of trustworthy audit/evidence for a consequential action;
- critical secret/security exposure;
- unrecoverable or unexplained data corruption;
- governance action applied to the wrong object;
- sustained backend unavailability during the active operating window.

### Possible controlled HOLD or degraded operation

- intermittent realtime disconnect where authenticated snapshots remain current and no time-sensitive action depends on push freshness;
- one connector lane degraded while all inbound is safely paused at the source;
- a non-critical page unavailable while the approved operating lane remains intact;
- an alert/recommendation interpretation issue with no automatic mutation;
- an individual user access problem when that user is not required for safe governance or recovery;
- slow telemetry or stale dashboard snapshot that can be reconciled through authoritative reads.

Degraded operation requires a named owner, explicit affected scope, compensating control, communication, and review time. It must never be silently relabeled healthy.

## 16. GO / HOLD / STOP Model

| Decision | Criteria | Action |
| --- | --- | --- |
| `GO` | Required evidence is current; platform/tenant trust is sufficient; source comparison is acceptable; roles/scopes are correct; no Critical/High unresolved issue affects the lane | Continue the approved pilot scope and monitor at the next checkpoint |
| `HOLD` | A bounded issue affects one user, domain, connector, or workflow; impact is understood; source systems remain safe; no mandatory-stop condition exists | Pause only the affected workflow, preserve evidence, assign owner, apply compensating control, set review time |
| `STOP` | Mandatory-stop condition, unknown material authority/data effect, or inability to maintain operational truth | Stop the pilot or affected operating scope immediately, return to source-system process, preserve evidence, escalate, and require explicit reauthorization |

Only a named Pilot Owner and Customer Operations Owner may declare `GO` after a material hold. A mandatory security/isolation stop cannot be overruled for schedule convenience.

## 17. Customer Communication

Use factual, time-bound language.

### Healthy operation

> SynapseCore is operating within the approved Company 1 pilot scope. Current platform, tenant, connector, and data checks are acceptable. The next checkpoint is [checkpoint].

### Degraded realtime

> Realtime updates are degraded. Authenticated snapshot reads remain [available/unavailable]. Operators should expect delayed screen updates and must not rely on push freshness for consequential actions until we confirm recovery.

### Connector failure

> The approved connector lane reported a failure at [time]. New affected input is [paused/controlled]. We are preserving evidence and checking source, mapping, connector, and duplicate state before any recovery action.

### Data mismatch

> SynapseCore and the authoritative source disagree for [sanitized reference]. The affected workflow is on hold while the source owner and SynapseCore team reconcile the values. No silent overwrite will be performed.

### Replay pending

> A failed inbound item is under review. Replay has not been authorized yet. We will replay only after cause correction, eligibility, warehouse scope, and duplicate safety are confirmed.

### Pilot hold

> The [workflow/domain] pilot lane is on hold from [time]. Existing source-system operations remain authoritative. We are investigating [factual condition] and will issue the next decision at [checkpoint].

### Pilot stop

> The pilot or affected scope has been stopped because operational trust cannot currently be maintained. Company 1 should use the agreed source-system process. Evidence is preserved and resumption requires explicit customer and SynapseCore authorization.

## 18. Platform Owner Day-One View

The Platform Owner monitors:

- Platform Overview for aggregate health and support posture;
- Tenant Directory for the Company 1 tenant identity and metadata-level support state;
- Platform Runtime for readiness, deployment, dependency, and platform trust signals;
- Platform Activity for metadata-level platform/support events;
- Release Trust for the frozen pilot release identity;
- Company 1 support indicators and declared incident state.

The Platform Owner must:

- confirm release and readiness before opening;
- classify degradation rather than hiding it;
- coordinate deployment/dependency response;
- protect separation between platform support metadata and tenant business data;
- never use platform authority as a substitute for a customer governance or replay role.

## 19. Tenant Admin Day-One View

The Customer Tenant Admin verifies:

- correct Company 1 tenant/workspace;
- correct active users and operator links;
- approved roles only;
- approved warehouse scopes, including intentional tenant-wide empty scopes;
- expected Users and Company Settings access;
- security, workspace, warehouse, and support-owner baseline;
- no unexpected bootstrap/proof/internal identities;
- no Platform Admin authority;
- no foreign tenant data.

The Tenant Admin does not manage Platform Runtime, the global tenant directory, platform releases, deployment secrets, connector/replay detail without an integration role, or governance actions without the applicable governance role.

## 20. Role-By-Role Day-One Expectations

All six roles retain common tenant reads such as Dashboard, Alerts, Recommendations, Orders, Inventory, Catalog, Locations, Fulfillment, Scenarios, Scenario History, Runtime, Audit, and Profile, subject to tenant and warehouse scope. Actual Company 1 people receive only approved roles.

| Role | What they should see | What they should do | What they should not do | Escalate when |
| --- | --- | --- | --- | --- |
| `TENANT_ADMIN` | Common workspace, Users, Company Settings; tenant-wide or explicitly approved scope | Verify roster, roles, scopes, settings, warehouse metadata, support ownership; perform approved access/config changes; perform approved inventory maintenance writes | Enter Platform Admin; use full integration/replay resources; create/transition live orders; update fulfillment; perform human-session ingestion; approve/execute scenarios unless separately assigned; browse another tenant | Wrong identity/scope, unexplained account, configuration drift, platform route/API access, foreign data |
| `INTEGRATION_ADMIN` | Common workspace, Integrations, Replay Queue, connector policy controls | Confirm connector state/policy; inspect imports/failures; authorize and perform safe replay; apply approved connector changes; perform approved inbound/order/fulfillment operational writes | Manage tenant users; make governance decisions; execute scenarios by this role; perform inventory maintenance writes; replay with unresolved source/duplicate risk | Connector/source mismatch, secret concern, missing failure evidence, replay uncertainty, wrong warehouse |
| `INTEGRATION_OPERATOR` | Common workspace, read-only connector/import views, Replay Queue within scope | Observe integration state; investigate failures; perform approved inbound/order/fulfillment operational writes; replay eligible corrected records once when authorized | Mutate connector policy; manage users; perform governance actions; perform inventory maintenance writes; bypass warehouse scope | Record not visible, eligibility unclear, dead-lettered item, duplicate risk, disabled-webhook limitation |
| `REVIEW_OWNER` | Common workspace and Approvals for assigned review-stage work | Review evidence; approve/reject assigned review stage; execute only approved saved plans in scope | Substitute for final approver/escalation owner; execute previews; manage connectors/users/platform; act outside assignment/scope | Wrong stage/actor, stale scenario, insufficient evidence, execution effect unclear |
| `FINAL_APPROVER` | Common workspace and Approvals for assigned final-stage work | Make assigned final decision with required evidence; execute only approved saved plans in scope | Perform review-stage substitution; execute previews; acknowledge escalation by this role; manage connector/users/platform | Separation-of-duty issue, requester/reviewer conflict, wrong assignment, stale or unsafe execution |
| `ESCALATION_OWNER` | Common workspace and Escalations for assigned conditions | Acknowledge and coordinate assigned escalation | Approve/reject or execute by this role alone; manage connectors/users/platform | Escalation ownership is wrong, condition cannot be understood, sensitive action is being requested |

Hidden navigation is not the security boundary. Backend authorization remains authoritative. Any unexpected successful direct route or API action must be treated according to impact, with authority leakage classified as a mandatory stop.

## 21. End-Of-Day Review

At the closeout, review and record:

- platform and tenant uptime or degraded windows;
- connector state and inbound counts;
- failed inbound items and their disposition;
- every replay, authorization, result, and duplicate-safety check;
- alerts and recommendations observed and interpreted;
- scenario previews, saved plans, approvals, rejections, acknowledgements, executions, and resulting effects;
- access, role, session, warehouse, and scope issues;
- source-versus-SynapseCore reconciliation samples;
- tenant and platform Activity evidence;
- customer observations and operator confusion;
- support actions and outstanding owners;
- accepted limitations encountered;
- backup/recovery posture changes;
- all GO/HOLD/STOP decisions.

No unresolved item may disappear between Day One and the daily operating baseline.

## 22. Day-One Acceptance States

Use exactly one final state.

### `DAY ONE ACCEPTED`

- all required checkpoints completed;
- approved operating lane remained trustworthy;
- source reconciliation accepted;
- no unresolved Critical or High issue;
- no unowned hold item;
- next-day baseline is clear.

### `DAY ONE ACCEPTED WITH ACTIONS`

- core approved lane operated safely;
- one or more bounded non-blocking issues or documented limitations remain;
- each action has severity, owner, compensating control, due checkpoint, and acceptance authority;
- no unresolved Critical or High issue affects safe continuation.

### `DAY ONE NOT ACCEPTED - PILOT HOLD`

- a mandatory-stop condition occurred;
- operational truth cannot be established;
- a Critical/High issue remains unresolved;
- required source, governance, recovery, access, or runtime control failed;
- resumption requires explicit re-verification and authorization.

Record the decision, approvers, date/time, evidence references, open actions, and exact next operating boundary.

## Phase 11 Handoff

Phase 11 is the Daily Operator SOP. It receives only the accepted Day-One baseline:

- confirmed operating window and contacts;
- active users, roles, and warehouse scopes;
- approved connector and data lanes;
- normal platform/tenant Runtime posture;
- normal connector/import/replay posture;
- source reconciliation baseline;
- alerts/recommendations interpretation agreed on Day One;
- governance assignment and execution rules;
- open actions and accepted limitations;
- checkpoint cadence and escalation thresholds;
- final Day-One acceptance state.

Phase 11 must not silently inherit an unresolved hold or expand Company 1 scope.

## Limitations Carried Forward

- Disabled-webhook replay/readback is not a proven Company 1 recovery capability. Use the proven CSV failed-inbound recovery lane.
- Import-run records do not have authoritative warehouse association. Do not make warehouse-attribution claims for import-run records.
- One stale pre-redeploy session produced a `500` on a denial attempt; fresh corrected sessions returned `403` and granted no authority. Require fresh sign-in when stale-session behavior is suspected.
- Provider-managed restore for the current Render database has not been drilled from the repository evidence. Application-level PostgreSQL backup/restore is proven; do not claim enterprise SLA-grade recovery.
- Live Render saturation is not proven. Keep the pilot to the accepted small operating envelope.
- The deployment remains a single backend/PostgreSQL/Redis posture. Do not claim high availability or automatic failover.
- There is no formal read-only tenant role, MFA, SSO/OIDC, universal approval workflow, arbitrary connector ecosystem, customer self-provisioning, or unrestricted automatic decision execution.
- Existing systems of record remain authoritative throughout the controlled pilot.

## Phase 10 Completion Gate

Phase 10 is complete only when:

- this guide is populated with the actual Company 1 scope through the controlled record;
- every entry condition is confirmed;
- required participants and decision authorities are named;
- all observation checkpoints have decisions;
- all in-scope live data samples are reconciled;
- any realtime, connector, replay, governance, access, or runtime issue has a truthful disposition;
- all mandatory stop conditions remain clear;
- end-of-day acceptance is signed;
- Phase 11 receives an accepted baseline rather than assumptions;
- no secrets or raw sensitive payloads appear in the record.

Canonical Phase 10 verdict options:

- `COMPANY PILOT PHASE 10 ACCEPTED`
- `COMPANY PILOT PHASE 10 ACCEPTED WITH DOCUMENTED LIMITATION`
- `COMPANY PILOT PHASE 10 NOT ACCEPTED - DAY-ONE CONTROL MODEL INCOMPLETE`

Creating this guide establishes the control model. A real Company 1 Day-One acceptance verdict requires an actual completed [Day-One Pilot Record](templates/company-day-one-pilot-record.md).
