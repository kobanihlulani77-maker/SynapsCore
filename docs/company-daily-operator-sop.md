# Company 1 Daily Operator SOP

This is the canonical SynapseCore Phase 11 standard operating procedure for a normal Company 1 pilot day after Day One has been accepted.

It converts the accepted [Day-One Pilot Guide](company-day-one-pilot-guide.md) baseline into a repeatable daily routine. It does not provision access, expand scope, change architecture, replace the source system, or perform incident recovery. Material incidents hand off to Phase 12.

## Operating Principles

- Truth over fake success.
- Evidence before assumption.
- Intelligence before automation.
- Human governance for high-impact actions.
- Company source systems remain authoritative during the pilot.
- Failure must be visible and recovery controlled.
- Least privilege and tenant isolation apply every day.
- When in doubt, do not replay.
- No unsupported claims or improvised production experiments.

The daily rhythm is:

```text
Opening checks
-> explicit GO / HOLD / STOP decision
-> bounded operating window
-> periodic trust and reconciliation checks
-> controlled event/exception handling
-> end-of-day close
-> explicit next-day carry-forward
```

Complete [company-daily-operator-record.md](templates/company-daily-operator-record.md) for every pilot operating day. Store sanitized evidence references, never secrets or raw customer payloads.

## Phase Boundary And Source Records

Phase 11 may operate only inside the approved records produced by:

- [Phase 2 Pilot Intake](company-1-pilot-intake-pack.md);
- [Phase 3 Tenant/Workspace Provisioning](company-tenant-workspace-provisioning-runbook.md);
- [Phase 4 User Provisioning](company-user-provisioning-runbook.md);
- [Phase 5 Integration Setup](company-integration-setup-runbook.md);
- [Phase 6 Data Onboarding](company-data-onboarding-runbook.md);
- [Phase 7 Operational Configuration](company-operational-configuration-runbook.md);
- [Phase 8 Pre-Handover Verification](company-pre-handover-verification-checklist.md);
- [Phase 9 Customer Handover](company-customer-handover-procedure.md);
- [Platform/Tenant Access Boundary](platform-control-plane-access-boundary.md); and
- [Phase 10 Day-One Pilot Guide](company-day-one-pilot-guide.md).

The [Pilot Operations Runbook](pilot-operations-runbook.md), [Support Playbook](support-playbook.md), [Pilot Rollback And Escalation](pilot-rollback-and-escalation.md), [Backup And Restore Runbook](backup-restore-runbook.md), and [Operator Incident Guide](operator-incident-guide.md) remain supporting references. This SOP supplies the company-specific daily sequence and evidence contract.

## 1. Daily Entry Conditions

Before normal operation starts, the opening owners must confirm:

| Condition | Daily evidence | Owner | If not true |
| --- | --- | --- | --- |
| Phase 10 Day One accepted | Completed Day-One record and accepted baseline | Pilot Owner | Do not start Phase 11 operation |
| Tenant remains active and correct | Tenant Directory metadata plus tenant identity | Platform Owner / Tenant Admin | `STOP` if wrong or unexpectedly inactive |
| Required users/operators remain active | Current session identity and access-change record | Tenant Admin | `HOLD` affected role/workflow |
| Roles remain expected | Approved role matrix compared after any change | Tenant Admin / Pilot Owner | `HOLD`; investigate widening or missing authority |
| Warehouse scopes remain expected | Session/readback and latest access record | Tenant Admin | `STOP` for bypass; otherwise hold affected user |
| Connector has not drifted | Connector identity, type, state, policy, support owner | Integration Admin | `HOLD` connector lane |
| Pilot scope remains approved | Current operating record matches frozen scope | Pilot Owner | Do not use new scope |
| Source systems can support the lane | Source owner opening confirmation | Customer Source Owner | `HOLD` affected data/inbound lane |
| Platform Runtime is acceptable | Platform Runtime and Platform Activity | Platform Owner | `HOLD` or `STOP` according to impact |
| Tenant Runtime is acceptable | Tenant Runtime and recent Tenant Activity | Customer Operations Owner | `HOLD` or `STOP` according to impact |
| Realtime state is known | Product connection state and approved trust evidence | Platform Owner / Operator | Use explicit degraded control or hold |
| No unresolved prior `STOP` | Previous daily/incident record | Pilot Owner | Normal operation must not start |
| Prior `HOLD` items reviewed | Owner, scope, control, and next decision recorded | Pilot Owner | Keep affected workflow held |

If a prior `STOP` condition remains unresolved:

`NORMAL OPERATION MUST NOT START.`

An unresolved High or Critical issue cannot become normal because a new calendar day began.

## 2. Daily Operating Rhythm

| Stage | Purpose | Required output |
| --- | --- | --- |
| Start of operating day | Establish platform, tenant, access, connector, source, and scope trust | Opening `GO`, `HOLD`, or `STOP` |
| Operating window | Perform only approved work with role and warehouse controls | Operational evidence and visible outcomes |
| Mid-morning check | Detect early connector, data, realtime, or access drift | Updated state and owner for anomalies |
| Midday check | Reconcile source and SynapseCore state; review alerts, replay, governance, and support | Midday `GO`, `HOLD`, or `STOP` |
| Mid-afternoon check | Confirm held items, changes, freshness, and end-of-day risk | Closing plan and unresolved-item owners |
| End-of-day close | Reconcile, classify, communicate, and carry forward | Daily closing state and next-day entry conditions |

Exact clock times belong in the approved company operating schedule, not this generic SOP.

## 3. Platform Owner Opening Check

The Platform Owner/operator performs these checks before customer operating work begins:

1. Confirm the expected frozen pilot release in Release Trust.
2. Open Platform Overview and confirm the platform is available without unexplained degradation.
3. Open Tenant Directory and confirm Company 1 appears once, active, and in the expected support state.
4. Open Platform Runtime and inspect backend readiness, database/session dependency posture, realtime mode, queue/operational posture, and release identity.
5. Open Platform Activity and inspect recent metadata-level deployment, support, and tenant attention conditions.
6. Review previous unresolved support or incident records.
7. Confirm disabled connectors and replay/import attention counts are expected.
8. Record platform opening state and an explicit decision.

Classify as:

- `HEALTHY`: platform trust supports the approved operating lane;
- `DEGRADED`: impact is understood and a bounded control exists;
- `HOLD`: affected workflow cannot safely start;
- `STOP`: platform trust, isolation, authority, or data integrity cannot be established.

The Platform Owner uses metadata-first support surfaces and must not casually browse raw tenant orders, products, inventory, inbound/replay payloads, connector secrets, or customer credentials.

## 4. Customer Tenant Opening Check

Each required customer operating role confirms:

1. Sign-in succeeds using the approved tenant workspace and personal account.
2. Displayed tenant and user identity are correct.
3. Expected navigation appears and irrelevant role navigation remains absent.
4. Warehouse scope matches the approved assignment.
5. Tenant Runtime shows an understood trust state.
6. Recent Tenant Activity belongs to Company 1 and is explainable.
7. In-scope operational pages show plausible current data or truthful empty states.
8. Integration roles confirm connector/import/replay state where relevant.
9. No unexpected role, scope, data, or configuration change is visible.

Foreign tenant data, foreign Tenant Activity, unexpected platform authority, or warehouse-scope bypass is an immediate `STOP`.

## 5. Role-By-Role Daily Responsibilities

Roles are capabilities, not job titles. A person may hold multiple approved roles, but effective authority remains the union of active roles and warehouse scope. Separation-of-duty controls must remain explicit.

| Role | What to check | What to act on | What not to touch | Escalate when | Evidence |
| --- | --- | --- | --- | --- | --- |
| `TENANT_ADMIN` | Users/operators after requested changes, roles, scopes, workspace/security settings, warehouse metadata | Approved access lifecycle and configuration support | Platform control plane; connector/replay detail or governance actions without another role | Unexpected account, privilege, scope, session, or settings drift | Access/change reference and verification |
| `INTEGRATION_ADMIN` | Connector identity/state/policy, recent imports, failure/replay posture, support owner | Approved connector change and safe replay | Tenant users, governance, platform settings, unapproved experiments | Connector drift, source mismatch, secret concern, unexplained telemetry | Connector state, request/result, change owner |
| `INTEGRATION_OPERATOR` | Import/failure evidence, replay queue, eligibility, warehouse, duplicates | Authorized replay of corrected eligible work | Connector policy, tenant users, governance, unsafe/dead-lettered replay | Cause, source state, scope, eligibility, or duplicate state is uncertain | Replay record, checks, result, final business state |
| `REVIEW_OWNER` | Assigned review queue, scenario evidence, stage, warehouse | Assigned review approve/reject; eligible governed execution | Final-stage substitution, connector/users/platform, out-of-scope execution | Assignment, stage, evidence, scope, or effect is unclear | Scenario ID, decision/note, resulting stage/history |
| `FINAL_APPROVER` | Assigned final queue, prior review, separation, evidence, warehouse | Assigned final approve/reject; eligible governed execution | Review substitution, connector/users/platform, self-conflicted action | Separation, assignment, evidence, or execution effect is unclear | Scenario ID, decision/note, resulting stage/history |
| `ESCALATION_OWNER` | Assigned escalations, due state, evidence, owner | Acknowledge and coordinate assigned escalation | Approve/reject/execute by this role alone; connector/users/platform | Ownership is wrong or material risk cannot be understood | Escalation ID, acknowledgement, coordination outcome |

All tenant roles may access common tenant pages only within tenant and warehouse constraints. Hidden navigation supports usability; backend authorization remains the security boundary.

## 6. Tenant Admin Daily SOP

The Tenant Admin does not need to mutate access every day. Review access when a request, anomaly, scheduled review, or prior-day action exists.

Procedure:

1. Confirm the signed-in tenant and Tenant Admin identity.
2. Review pending approved access requests and prior access-change actions.
3. Compare requested user/operator/role/scope change with the approved owner and pilot scope.
4. Use supported user/operator administration APIs/UI only.
5. For password reset, deliver the temporary secret through the approved separate channel and never record it.
6. Verify the changed user's next session reflects active state, roles, and warehouse scope.
7. Confirm old authority/session is revoked where the access model requires it.
8. Record the change, owner, reason, before/after authority, and verification.

`TENANT_ADMIN` is not Platform Owner. It cannot access `/api/platform/*`, global Tenant Directory, Platform Runtime, Release Trust, deployment secrets, or other tenants.

## 7. Integration Admin Daily SOP

Opening and periodic checks:

1. Confirm connector display name, source system, type, enabled/disabled state, sync/import posture, default warehouse behavior, and support owner.
2. Review recent imports and failure/replay attention.
3. Compare the current policy with the frozen Phase 7 baseline after any deployment or approved change.
4. Confirm disabled state is intentional and understood.
5. Confirm expected inbound cadence with the source owner rather than inferring it from dashboard counts.

Change procedure:

1. Verify an authorized owner and reason.
2. Confirm tenant, connector, source, and affected workflow.
3. Preserve the before state.
4. Apply one approved supported change.
5. Verify connector readback, inbound behavior, Runtime/Activity, and no cross-tenant effect.
6. Record the after state and decision.

Do not experiment casually in the live pilot, rotate credentials without an approved secure process, or change mappings/policy merely to clear an alert.

## 8. Integration Operator Daily SOP

Review the replay queue at opening, periodic checkpoints, and after connector/import failures.

For each failed inbound item:

```text
Failure
-> evidence
-> cause
-> correction
-> authorization
-> duplicate check
-> replay once
-> verify
```

Required procedure:

1. Confirm tenant, connector, source system, external order ID, warehouse, failure code/message, attempt state, and eligibility.
2. Compare source-system truth and confirm whether a successful order already exists.
3. Identify and correct the prerequisite through a supported source/application path.
4. Confirm replay authorization and assigned warehouse access.
5. Confirm the record is not already replayed or dead-lettered and `nextEligibleAt` does not block it.
6. Replay once only when every check passes.
7. Confirm the resulting order/effect appears once.
8. Confirm replay/import telemetry, Tenant Activity, and realtime/snapshot readback agree.
9. Record the decision and outcome.

`WHEN IN DOUBT, DO NOT REPLAY.`

## 9. Recovery Support Boundary

The proven Company 1 pilot recovery lane is:

```text
CSV failed inbound
-> visible evidence
-> supported correction
-> authorized replay
-> duplicate-safe result
```

Disabled-webhook replay recovery is not proven. If a webhook-related failure falls outside the proven lane:

- preserve sanitized evidence and request IDs;
- hold the affected workflow where freshness or duplication could matter;
- escalate to the Integration Owner and Pilot Owner;
- do not improvise a replay, database edit, or unsupported re-ingestion;
- reopen the recovery gate before promising or enabling that behavior for Company 1.

## 10. Catalog Daily Operations

Apply only if catalog is in pilot scope.

1. Treat the approved Company product source as authoritative.
2. Use tenant-visible SKU as the operational identity and preserve the approved mapping.
3. Review new/changed products through supported create, update, or CSV upsert paths.
4. Verify SKU, name, price, reorder threshold, active/business meaning, and tenant ownership after a change.
5. Reconcile a bounded sample with the source at the agreed checkpoint.
6. If a mismatch appears, hold affected downstream work, classify source/mapping/application cause, correct through a supported path, and reverify.

Never silently overwrite source truth or manually edit production database rows.

## 11. Inventory Daily Operations

Apply only if inventory is in pilot scope.

1. Confirm selected SKU and warehouse identities.
2. Compare approved source on-hand/available values with SynapseCore readback for the bounded daily sample.
3. Review thresholds, low-stock/depletion state, and related alerts/recommendations.
4. Investigate timing, reservation, inbound, mapping, and warehouse differences before declaring an error.
5. Use supported baseline update, receive, adjust, or reconcile paths only when authorized.
6. Record before/source value, approved correction, after value, actor, and evidence.

An unexplained material source/SynapseCore mismatch is `HOLD` until understood. Wrong-tenant or wrong-warehouse mutation is `STOP`.

## 12. Order Daily Operations

Apply only if orders are in pilot scope.

1. Review expected inbound volume and source references, not only dashboard counts.
2. Sample external order IDs, warehouses, item SKUs, quantities, timestamps, and statuses against the source.
3. Confirm each order appears once and belongs to the intended tenant/warehouse.
4. Interpret status in the context of connector, inventory reservation, and fulfillment evidence.
5. For missing or failed orders, inspect connector/import/replay evidence before retrying anything.
6. For duplicate concern, stop consequential processing and reconcile source plus SynapseCore identity.
7. Record material mismatches and outcomes.

A visible dashboard count alone does not prove order correctness.

## 13. Alerts Daily SOP

Use this sequence:

```text
Alert appears
-> inspect supporting evidence
-> identify the condition
-> assign operational owner
-> classify severity
-> take a permitted action
-> verify and record result
```

Alerts are system-generated inspection evidence, not operator-authored tasks. Confirm tenant, warehouse/domain, severity, description, impact, policy explanation, and source condition. An alert does not by itself authorize inventory mutation, connector change, replay, approval, or scenario execution.

## 14. Recommendations Daily SOP

Use this sequence:

```text
Recommendation
-> evidence review
-> human interpretation
-> accept, reject, defer, or escalate operational response
-> governed workflow where required
-> record outcome
```

Recommendations are advisory operational intelligence. They do not automatically mutate inventory, create orders, replay inbound records, approve scenarios, or execute plans. Record why a material recommendation was accepted, rejected, deferred, or escalated when it affects the pilot lane.

## 15. Governance Daily SOP

Apply only if scenario/governance workflows are in the approved scope.

```text
Preview
-> compare where useful
-> save plan
-> REVIEW_OWNER review
-> FINAL_APPROVER decision when escalated
-> ESCALATION_OWNER acknowledgement where applicable
-> authorized execution
-> resulting order/effects, history, Activity, and realtime verification
```

Before every decision or execution, verify tenant, assignment, stage, warehouse scope, requester/reviewer/final-approver separation, scenario payload, current Runtime trust, and intended live effect.

Execution authority belongs to an eligible `REVIEW_OWNER` or `FINAL_APPROVER` within role, assignment, stage, and warehouse constraints. There is no wrong-role shortcut and no generic universal approval workflow.

## 16. Warehouse Scope

Effective authority includes:

```text
authenticated tenant
+ active user
+ active operator
+ assigned role
+ warehouse scope
+ workflow assignment
```

Role alone is insufficient.

Current semantics:

`empty warehouse scope = tenant-wide warehouse access`

Treat an empty scope or scope widening as a high-impact access assignment. Investigate any unexpected widening immediately, preserve before/after evidence, apply the approved correction, and verify that an existing or fresh session reflects the new persisted boundary.

Import-run records currently lack authoritative warehouse association. Do not infer or claim warehouse attribution for those records.

## 17. Activity Monitoring

Tenant Activity should help answer:

- what changed;
- who or what acted;
- which tenant/workflow was affected;
- what condition occurred;
- what requires follow-up.

Use Activity with domain readback and source evidence; it is not a replacement for reconciliation.

Tenant users see permitted Company 1 operational/audit activity. Platform Owner sees metadata-level Platform Activity. Platform Activity must not expose raw customer orders, items, product/inventory rows, inbound/replay payloads, connector secrets, or credentials.

Foreign tenant Activity on a Company 1 surface is `STOP`.

## 18. Runtime Trust

| State | Tenant/Platform interpretation | Daily action |
| --- | --- | --- |
| `HEALTHY` | Required readiness, dependency, tenant operational, and realtime signals support the approved lane | `GO` with routine monitoring |
| `DEGRADED` | A known bounded signal is impaired; authoritative reads and safe control remain available | Continue only affected-safe work with explicit control and review time |
| `HOLD` | Freshness, connector, data, access, governance, or dependency trust is insufficient for one workflow | Pause that workflow and investigate |
| `STOP` | Isolation, authority, integrity, critical security, or platform truth cannot be established | Stop pilot or affected operating scope immediately |

Tenant Runtime asks, "Is my tenant operating correctly?" Platform Runtime asks, "Is SynapseCore infrastructure/platform operating correctly?" Do not expose platform internals to tenant users or use tenant Runtime as proof of platform-owner authority.

## 19. Realtime Daily Operation

Expected behavior:

- authenticated tenant-scoped SockJS/STOMP connection through `/ws`;
- tenant and role/warehouse-aware subscriptions;
- product connection state moves from connecting to live;
- relevant pages refresh after permitted events;
- on transport failure the UI may show reconnecting/degraded and perform periodic snapshot refresh.

When realtime is degraded:

1. State that it is degraded; do not label it live.
2. Confirm backend readiness, authenticated snapshot/API reads, and event time.
3. Identify pages/workflows whose freshness is affected.
4. Use approved manual refresh or polling fallback where trustworthy.
5. Hold time-sensitive consequential actions if freshness cannot be proven.
6. Escalate persistent degradation with timestamps and request IDs.

## 20. Periodic Data Reconciliation

Reconcile only the bounded, approved sample and domains in scope. The process may be manual because perfect automated reconciliation is not implemented.

| Domain | Compare | Minimum evidence | Material mismatch response |
| --- | --- | --- | --- |
| Catalog | Source count/sample SKU values vs SynapseCore | Count/time plus representative SKUs | Hold affected downstream item; classify and correct |
| Inventory | Source SKU/warehouse quantities vs SynapseCore | Sample list, source time, readback time | Hold affected warehouse/SKU workflow |
| Orders | Source external IDs/count/status/items vs SynapseCore | Bounded period and sampled IDs | Stop duplicate processing; inspect connector/replay evidence |
| Connector/import | Expected source transmissions vs recent imports/failures | Source reference, connector, result/count | Hold lane if unexplained gap exists |

Record source timestamp and SynapseCore readback timestamp so timing differences are not misclassified as corruption.

## 21. Issue Classification

Use one primary class:

- `ACCESS`
- `DATA`
- `CONNECTOR`
- `REPLAY`
- `GOVERNANCE`
- `REALTIME`
- `PLATFORM`
- `USER EXPERIENCE`
- `SCOPE / AUTHORIZATION`

Every issue records date/time/timezone, tenant, observer, class, sanitized evidence, affected workflow/warehouse, impact, severity, owner, current state, immediate action, and resolution/next step. Never record credentials, tokens, session cookies, connector secrets, or raw sensitive payloads.

## 22. Incident Severity

Use the repository's impact-based posture:

| Severity | Meaning | Daily response |
| --- | --- | --- |
| `LOW` | Minor usability or informational issue; no authority, data, recovery, or operating-lane risk | Record and continue; schedule review |
| `MEDIUM` | Bounded impairment with a safe workaround/compensating control and known owner | Controlled degradation or scoped `HOLD`; monitor |
| `HIGH` | Material approved workflow, security, recovery, or trust concern that prevents safe affected-lane operation | Immediate `HOLD`; escalate and reverify before resuming |
| `CRITICAL` | Tenant isolation, unauthorized authority, corruption, secret exposure, wrong-object consequential action, or equivalent immediate trust failure | Immediate `STOP`; preserve evidence and invoke incident control |

Severity follows actual impact, not inconvenience or pressure to continue.

## 23. GO / HOLD / STOP During Normal Operation

| Decision | Daily example | Required action |
| --- | --- | --- |
| `GO` | Runtime healthy, connector expected, bounded reconciliation accepted, no unresolved material issue | Continue approved scope to next checkpoint |
| `HOLD` | One connector/warehouse lane has an understood mismatch; realtime degraded with uncertain freshness; replay eligibility unclear | Pause affected workflow, preserve evidence, assign owner and review time |
| `STOP` | Foreign data, platform authority leakage, scope bypass, corruption, secret exposure, duplicate consequential execution | Stop pilot or affected scope; return to source process and escalate |

A `HOLD` never authorizes work around the affected control. A `STOP` requires explicit resume authorization after classification, correction, and verification.

## 24. Stop Conditions

Carry forward all Phase 10 conditions:

- cross-tenant data or Activity exposure;
- unauthorized Platform Control Plane authority;
- unexpected raw tenant data exposure through platform surfaces;
- warehouse-scope bypass;
- authentication/authorization bypass;
- unauthorized or wrong-object consequential execution;
- customer credential, secret, or token exposure;
- unexplained corruption or unrecoverable material mismatch;
- duplicate consequential execution or unsafe repeated replay;
- loss of trustworthy audit/evidence;
- critical security issue;
- sustained backend unavailability during active operation.

Do not weaken a stop condition because the source system remains available or the frontend shell still loads.

## 25. Controlled Degradation

The full pilot need not stop when all of these are true:

- the issue is bounded to a known workflow/user/warehouse;
- no isolation, authority, corruption, secret, or duplicate-execution risk exists;
- source systems remain authoritative and safe;
- a compensating control is explicit;
- freshness and evidence remain sufficient for unaffected work;
- an owner and next review time are recorded;
- customer communication is factual.

Possible examples include realtime degradation with trustworthy snapshot fallback, one paused connector lane, one held warehouse workflow, a non-critical UX issue, or a paused recommendation workflow. Record the platform as degraded, not healthy.

## 26. Same-Day Change Control

No unapproved material change occurs during active operation.

Changes to role, warehouse scope, connector policy/configuration, data mapping, operational policy, or pilot scope require:

1. authorized business and technical owner;
2. stated reason and affected lane;
3. before-state evidence;
4. risk and rollback/hold decision;
5. supported implementation path;
6. after-state verification;
7. daily record entry and customer communication where relevant.

Major change-management design belongs to Phase 13. Phase 11 only enforces the immediate no-improvisation rule.

## 27. Access Change During The Day

| Change | Safe procedure | Verification |
| --- | --- | --- |
| User disabled | Approve request; disable through tenant-admin path; record reason | Existing session loses authority; protected request denied; user cannot sign in |
| Operator disabled | Confirm business impact; disable operator | Workspace access denied; assigned workflows reassigned or held |
| Password reset | Verify recipient; reset through supported API; deliver secret separately | Session version changes; old session/secret unusable; required change completed |
| Role change | Approve least-privilege before/after matrix; update operator | Existing/fresh session shows new role; allowed and denied actions match |
| Warehouse scope change | Treat widening/empty scope as high impact; approve explicit warehouse list | Reads/writes and subscriptions reflect new scope; wrong warehouse denied |

Do not rely only on hidden navigation. Verify backend denial/allowance appropriate to the change. Unexpected stale-session behavior requires a fresh sign-in and preserved support evidence.

## 28. Support Escalation

| Concern | First contact | Escalation owner |
| --- | --- | --- |
| Platform/readiness/database/Redis/realtime infrastructure | SynapseCore Platform Owner | Deployment/Incident Owner |
| Connector configuration or telemetry | Customer Integration Admin | SynapseCore Integration Owner |
| Source data or mapping | Customer Data/Source Owner | SynapseCore Data Owner |
| Login, role, session, warehouse scope | Customer Tenant Admin | SynapseCore Access/Security Owner |
| Scenario approval/execution | Assigned governance role | Pilot Governance Owner |
| Replay uncertainty or duplicate risk | Integration Operator/Admin | Integration Owner plus Source/Pilot Owner |
| Security, isolation, secret exposure | SynapseCore Security/Incident Owner | Immediate Pilot Owner and Customer Operations Owner |

Use [support-playbook.md](support-playbook.md) for diagnostic response and [pilot-rollback-and-escalation.md](pilot-rollback-and-escalation.md) for pause/resume controls. Material incident execution belongs to Phase 12.

## 29. Customer Communication

Use concise factual language.

### Healthy

> The Company 1 pilot is operating within the approved scope. Current platform, tenant, connector, and reconciliation checks are acceptable. The next review is [checkpoint].

### Degraded

> SynapseCore is degraded in [specific area]. The affected scope is [scope]. The compensating control is [control], and the next decision is [checkpoint]. We are not describing the affected capability as healthy.

### Workflow hold

> The [workflow/warehouse] lane is on hold from [time] while we investigate [factual condition]. Company source systems remain authoritative. Unaffected approved work may continue only as stated.

### Connector issue

> The approved connector lane reported [factual condition] at [time]. Affected input is [paused/controlled]. Evidence is preserved and no recovery action will occur until cause and duplicate safety are confirmed.

### Replay pending

> The failed inbound item is under review. Replay is not yet authorized. We are checking cause correction, eligibility, warehouse scope, source truth, and duplicate risk.

### Recovery complete

> The supported recovery completed at [time]. The item was replayed once, the resulting state appears once, and connector/replay/source evidence has been reconciled. Monitoring continues until [checkpoint].

### Pilot stop

> The pilot or affected scope is stopped because operational trust cannot currently be maintained. Company 1 should use the agreed source-system process. Resumption requires classification, verification, and explicit authorization.

## 30. Daily Evidence Record

The daily record captures:

- company, tenant, date/timezone, release, pilot scope, and operating window;
- prior-day carry-forward and opening state;
- Platform Runtime, Tenant Runtime, connector, and realtime state;
- active users/role/scope confirmation where relevant;
- bounded catalog/inventory/order reconciliation;
- alerts and recommendations handled;
- replay and governance events;
- issues, severity, decisions, support/change actions, and communications;
- end-of-day state, next-day conditions, and sign-off.

The record contains references, not credentials, raw payloads, or secrets.

## 31. End-Of-Day Closing Procedure

Perform in this order:

1. Stop initiating non-essential consequential actions near close.
2. Review active alerts and their ownership/disposition.
3. Review material recommendations and responses.
4. Reconcile connector state, recent imports, failures, and source expectations.
5. Review failed inbound and every replay attempted that day.
6. Review pending/decided/executed governance items and live effects.
7. Review access, role, scope, password, or session changes.
8. Review Platform Runtime, Tenant Runtime, realtime degradation, and Activity.
9. Complete bounded source reconciliation for in-scope domains.
10. Classify every issue and support action with owner/state/next step.
11. Review accepted limitations encountered.
12. Select one daily closing state.
13. Communicate the factual close and next operating boundary.
14. Carry every unresolved item into the next-day opening section.

Nothing disappears because the day ended.

## 32. Daily Closing States

### `DAILY OPERATIONS ACCEPTED`

The approved lane operated safely, reconciliations are accepted, and no unresolved material action affects tomorrow's opening.

### `DAILY OPERATIONS ACCEPTED WITH OPEN ACTIONS`

The approved lane operated safely, but bounded Low/Medium actions remain with owner, control, and review point. No unresolved High/Critical issue permits normal affected-lane continuation.

### `DAILY OPERATIONS HOLD`

One or more workflows cannot safely resume at the next opening until specified conditions are corrected and verified.

### `DAILY OPERATIONS STOPPED`

A mandatory stop condition occurred or operational truth cannot be maintained. Phase 12 incident/rollback/recovery control is required before resumption.

## 33. Next-Day Carry-Forward

For every unresolved item record:

- issue/change ID;
- severity and category;
- affected tenant/workflow/warehouse;
- last known trustworthy state;
- evidence reference;
- current control;
- owner;
- required next action;
- required verification;
- next-day opening decision prerequisite.

An unresolved High or Critical item remains `HOLD` or `STOP`. It cannot be relabeled normal without evidence and authorization.

## 34. Phase 12 Handoff

Phase 12 is the Incident / Rollback / Recovery Pack. Phase 11 hands it:

- incident trigger and severity;
- affected Company tenant, workflow, connector, warehouse, users, and time window;
- last known trustworthy platform, tenant, source, and business state;
- sanitized evidence, request IDs, logs/screenshots/traces references;
- Platform Runtime, Tenant Runtime, Platform Activity, and Tenant Activity evidence;
- recent access, configuration, policy, mapping, deployment, or scope changes;
- failed inbound and replay history;
- governance decisions/executions and affected objects;
- source-system fallback state;
- current `HOLD` or `STOP` decision and decision owners;
- customer communications already issued.

Phase 11 does not perform Phase 12 design or claim recovery complete.

## Limitations Carried Forward

- Disabled-webhook replay/readback recovery is not proven. The pilot recovery lane remains CSV failed inbound through controlled replay.
- Import-run records lack authoritative warehouse association. Do not claim warehouse attribution for an import run.
- Provider-managed Render restore has not been drilled in repository evidence; application-level PostgreSQL restore is proven.
- Render/live scale evidence remains bounded to the accepted pilot envelope. Do not infer enterprise-wide saturation capacity.
- The deployment has no proven high-availability or automatic-failover posture.
- MFA/SSO/OIDC is not implemented.
- Scenario governance is not a generic approval engine.
- Arbitrary integration support is not implemented.
- Unrestricted customer self-provisioning is not supported.
- There is no formal read-only tenant role.
- Existing source systems remain authoritative.

## Phase 11 Completion Gate

The Phase 11 operating model is complete when:

- the accepted Phase 10 baseline is required before daily opening;
- every role has practical check/action/prohibition/escalation/evidence guidance;
- all in-scope domains have bounded daily procedures;
- Runtime, Activity, realtime, replay, governance, access, and reconciliation controls are explicit;
- GO/HOLD/STOP and Low/Medium/High/Critical classifications are operationalized;
- the daily record captures opening, operation, close, and carry-forward without secrets;
- material incidents hand off cleanly to Phase 12;
- accepted limitations remain visible;
- no runtime behavior or pilot scope was changed.

Canonical Phase 11 verdict options:

- `COMPANY PILOT PHASE 11 ACCEPTED`
- `COMPANY PILOT PHASE 11 ACCEPTED WITH DOCUMENTED LIMITATION`
- `COMPANY PILOT PHASE 11 NOT ACCEPTED - DAILY OPERATING MODEL INCOMPLETE`

Creating this SOP establishes the daily control model. A real daily operating verdict requires a completed [Daily Operator Record](templates/company-daily-operator-record.md) for that operating day.
