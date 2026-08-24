# Company 1 Pilot Change Management

**Status:** Phase 13 operational control document
**Audience:** Company 1 Pilot Owner, Platform Owner, Tenant Admin, Integration Admin, operators, support, and engineering
**Scope:** Changes made while the controlled Company 1 pilot is active
**Authority:** This guide controls intentional change. It does not expand product scope, replace the source system of record, or bypass backend authorization.

## 1. Purpose

Phase 12 controls abnormal conditions:

```text
INCIDENT -> CONTAIN -> RECOVER -> VERIFY -> RESUME
```

Phase 13 controls deliberate changes:

```text
REQUEST -> ASSESS -> AUTHORIZE -> IMPLEMENT -> VERIFY -> ACCEPT or ROLLBACK
```

The purpose is to prevent uncontrolled pilot drift. A customer request is not automatically an authorized change. A deployment being successful is not the same as the release being accepted. An API response of `200` is not sufficient evidence that a change worked.

The governing principles are:

- truth before appearance;
- no unapproved material change;
- evidence before change;
- least privilege;
- source systems remain authoritative;
- one controlled material change at a time where practical;
- before state must be known;
- after state must be verified;
- rollback must be possible, or the risk must be explicitly accepted;
- incident recovery is not a shortcut around change management;
- pilot scope must not expand accidentally.

## 2. What Counts As A Pilot Change

A **pilot change** is an intentional modification to authority, tenant/workspace configuration, warehouse scope, connector behavior, data mapping, operational policy, governance assignment, deployment, security posture, or approved pilot scope that could alter what users can do, what data enters the system, what the system displays, or what operational result is produced.

Examples include:

- create, disable, enable, or reset a user;
- create or update an operator;
- change a role or warehouse scope;
- add, enable, disable, or configure a connector;
- change connector policy or support ownership;
- correct catalog, SKU, or mapping data;
- perform approved inventory maintenance;
- change operational thresholds or implemented settings;
- reassign Review Owner, Final Approver, or Escalation Owner;
- deploy a backend or frontend revision;
- add a warehouse, connector lane, operator group, or workflow to pilot scope;
- request a capability currently documented as unproven.

### 2.1 Routine Operation Versus Controlled Change Versus Incident

| Type | Meaning | Examples | Record |
| --- | --- | --- | --- |
| Routine operation | Expected work inside the approved scope with no authority, configuration, release, or scope change | Review dashboard, process an approved operational input, inspect alerts, perform an already-authorized replay | Daily Operator Record and normal activity evidence |
| Controlled change | Intentional material modification that can be assessed, authorized, verified, and reversed or explicitly accepted | Role change, connector policy, warehouse scope, mapping correction, deployment | Company Pilot Change Record |
| Incident response | Abnormal state where trust, safety, or recovery is uncertain | Unauthorized access, wrong-warehouse mutation, readiness failure, unexplained duplicate, failed verification | Phase 12 Incident Recovery Record |

If a controlled change fails and trust is no longer established, stop the change and transition it to Phase 12. Do not create a second unrelated change to hide the first result.

## 3. Change Classes

| Class | Current pilot meaning | Typical owner |
| --- | --- | --- |
| Access change | User, operator, password, role, or session access change | Tenant Admin; Platform Owner for platform access |
| Tenant configuration | Workspace settings and approved tenant administration | Tenant Admin |
| Warehouse/scope change | Warehouse assignment, scope, or warehouse configuration | Tenant Admin with Pilot Owner review |
| Integration change | Connector creation, policy, configuration, enablement, disablement, or support metadata | Integration Admin |
| Data/mapping change | Catalog, SKU, import mapping, or source-alignment correction | Customer Source Owner and approved operator |
| Inventory maintenance | Approved direct inventory correction within tenant and warehouse authority | Tenant Admin |
| Order/fulfillment change | Direct operational write within integration role and warehouse authority | Integration Admin/Operator |
| Operational policy change | Implemented threshold, setting, or supported policy value | Tenant Admin or Platform Owner according to scope |
| Governance configuration | Review Owner, Final Approver, or Escalation Owner assignment | Tenant Admin/Pilot Owner |
| Deployment/release | Frontend, backend, infrastructure, migration, or configuration release | Platform Owner/Engineering |
| Pilot scope change | Addition of site, warehouse, connector lane, workflow, user group, or material operator count | Pilot Owner |
| Security change | Credential rotation, role elevation, session invalidation, or security configuration | Platform Owner/Security Owner |

Do not invent a generic rules engine, arbitrary connector framework, generic approval engine, or customer self-service capability. A change class exists only where the current product and operating model support it.

## 4. Risk Model

| Risk | Definition | Default control |
| --- | --- | --- |
| `LOW` | Bounded and reversible with little operational consequence | Proportionate approval, before/after evidence, targeted verification |
| `MEDIUM` | Material pilot workflow impact is possible, but scope and rollback are understood | Owner authorization, affected-lane hold if needed, explicit verification and monitoring |
| `HIGH` | Large operational, security, authority, data, or recovery impact, or difficult rollback | Pilot Owner and responsible owner approval, affected-lane `HOLD`, detailed evidence and revalidation |
| `CRITICAL` | Change could weaken tenant isolation, enable unauthorized authority, expose secrets, corrupt data, or expand unsupported scope during active pilot | Do not proceed as routine change; stop or seek exceptional authorization with explicit pilot boundary |

Risk is assessed from actual impact, not from the number of lines changed. A small role or warehouse-scope change can be higher risk than a large cosmetic change.

## 5. Change Request

Every material request answers:

- What is changing?
- Why is it needed now?
- Who requested it?
- Which tenant, warehouse, workflow, connector, or release is affected?
- Who owns implementation?
- Who authorizes it?
- What is the current state?
- What is the desired state?
- What could be affected?
- What dependencies or collisions exist?
- How will success be verified?
- How will rollback work?
- What happens if rollback is impossible?
- Does the request expand a documented limitation or pilot boundary?

The requester, authorizer, implementer, and verifier should be separate when practical. One person may hold multiple roles only when the risk is low and the record explains why separation was not practical.

## 6. Authority Model

Authority comes from the authenticated role, tenant, warehouse scope, assignment, and current state. A requester does not receive authority merely by asking for a change.

| Role | Change authority in current pilot | Explicit boundary |
| --- | --- | --- |
| `TENANT_ADMIN` | Tenant users/operators, tenant roles, warehouse scopes, workspace settings, and approved inventory maintenance | No platform authority, direct order/fulfillment authority, or human-session ingestion solely from this role |
| `INTEGRATION_ADMIN` | Connector and integration configuration; approved integration writes and recovery | No tenant user administration or governance authority solely from this role |
| `INTEGRATION_OPERATOR` | Approved operational integration writes and replay/recovery actions within assigned warehouse scope | No connector policy changes, tenant administration, or governance actions solely from this role |
| `REVIEW_OWNER` | Assigned review decision | Must not approve an unassigned workflow |
| `FINAL_APPROVER` | Assigned final governance decision | Must not execute preview or unassigned work |
| `ESCALATION_OWNER` | Assigned escalation acknowledgement and coordination | Must not approve, reject, execute, or replay by this role alone |
| Platform Owner | Platform deployment, runtime, release, platform metadata, and support controls | Must not use global authority to bypass tenant business roles or browse raw tenant payloads casually |
| Customer Source Owner | Authoritative source data and source-side correction | Does not automatically authorize SynapseCore changes |
| Pilot Owner | Pilot scope, exception, resume, and customer-level authorization | Does not replace backend role enforcement |

No recovery or change procedure may bypass normal backend authority.

## 7. Before-State Evidence

Before implementing a material change, capture only the evidence needed to compare before and after state:

- current user/operator identity, active state, role, and warehouse scope;
- current tenant/workspace and warehouse metadata;
- current connector identity, type, state, policy, and support owner;
- current mapping or source value;
- current inventory quantity and warehouse for a maintenance change;
- current governance assignment and workflow state;
- current Platform/Tenant Runtime and Activity state;
- current release/commit and deployment state;
- current pilot scope and approved limitation boundary;
- relevant request IDs or evidence references.

Do not record passwords, password hashes, session cookies, bearer tokens, platform-owner credentials, connector secrets, customer credentials, raw inbound payloads, or copied environment files.

## 8. Change Windows And One-Change Principle

Changes should occur in a lower-risk operating window where:

- the affected workflow can be bounded;
- the owner and verifier are available;
- source-system support is available;
- evidence can be captured before and after;
- rollback ownership is clear;
- customer communication can be sent if needed.

For a high-risk change, place the affected workflow on `HOLD` before implementation.

Where practical:

```text
one material change -> verify -> monitor -> next change
```

Do not bundle a connector change, role change, mapping change, and deployment unless their dependency/order is required and recorded. If a bundle is necessary, define the sequence, shared rollback implications, and verification boundary.

## 9. Access Change Procedure

### 9.1 Create User Or Operator

1. Confirm the request, business owner, tenant, workspace, role, and warehouse scope.
2. Confirm the role is supported and least-privilege for the work.
3. Record the before state: user absent or existing state, tenant, requested role, and scope.
4. Tenant Admin implements through the supported access path.
5. Verify fresh sign-in, identity, expected navigation, allowed action, denied action, and warehouse behavior.
6. Record the after state without recording the password.

### 9.2 Disable Or Enable

1. Confirm the user, reason, effective time, and operational handover.
2. Record current role, scope, active session expectation, and affected work.
3. Implement through the supported access path.
4. Verify fresh session denial or restoration and protected backend action behavior.
5. If a stale session behaves unexpectedly, preserve it as evidence and repeat with a fresh session; do not call the change complete from navigation alone.

### 9.3 Password Reset

1. Confirm identity and authorization through the supported owner.
2. Do not place the new password in the change record, logs, screenshots, Git, or proof state.
3. Verify the intended user can authenticate and that the old session/credential behavior matches the supported session policy.
4. Confirm sign-out and fresh sign-in.

### 9.4 Role Change

1. Record old and new role(s), reason, tenant, and scope.
2. Confirm the new authority is necessary and approved.
3. Implement through the supported access path.
4. Verify one expected allowed operation and one expected denied operation.
5. Verify direct route/API denial, not only hidden navigation.
6. Record session refresh/revocation behavior and the new role readback.

## 10. Warehouse-Scope Change Procedure

Warehouse scope is high impact. An empty scope means tenant-wide authority and must never be treated as harmless.

1. Record previous scope and the requested new scope.
2. Identify Warehouse A and Warehouse B or the specific operational boundary.
3. Confirm business reason, authorizer, and affected workflows.
4. Hold affected operational writes if scope could collide with active work.
5. Implement through the supported access path.
6. Verify the assigned identity can perform the expected Warehouse A operation.
7. Verify Warehouse B is denied or allowed exactly as intended.
8. Verify fresh-session persistence and no unintended empty/tenant-wide scope.
9. Reconcile any operation made near the change boundary.

An unexpected empty scope is a `HIGH` access concern and may become `CRITICAL` if it enables an incorrect mutation.

## 11. Connector Change Procedure

Before a connector change, capture connector identity, type, enabled state, policy, support owner, recent import state, and expected source traffic. Do not capture secrets.

After creating or changing a connector:

1. Verify connector metadata and policy readback.
2. Confirm intended enabled/disabled behavior.
3. Test only approved synthetic or pilot-scoped input.
4. Verify import/failure evidence, Runtime diagnostics, Activity, and source relationship where relevant.
5. Confirm replay behavior remains inside the approved lane.
6. Monitor the connector through an agreed checkpoint.

### 11.1 Disable Or Enable

Disabling a connector can create failed inbound work and freshness impact. Before disabling, understand expected traffic and the source-system fallback. After enabling, do not blindly replay everything that failed while disabled. Apply Phase 12 eligibility, warehouse, lock, source-correction, and duplicate checks.

The disabled-webhook replay/readback limitation remains documented. Company 1's proven recovery lane is CSV failed inbound through controlled replay.

## 12. Data And Mapping Change Procedure

The source system remains authoritative.

1. Capture the before value, source reference, mapping/SKU/product identifier, tenant, warehouse, and affected workflow.
2. Identify the authoritative source owner.
3. Confirm the change is a supported correction, not an attempt to hide a mismatch.
4. Implement through the supported UI/API/import path.
5. Verify SynapseCore readback and source agreement.
6. Test the affected inbound or downstream flow with approved data.
7. Reconcile alerts, recommendations, inventory, orders, and activity where relevant.

Do not manually alter database rows to make a screen look correct.

## 13. Inventory-Maintenance Change

Approved direct inventory maintenance belongs to `TENANT_ADMIN` within tenant and warehouse authority.

Record:

- why the correction is required;
- source evidence;
- warehouse and SKU;
- before quantity;
- authorized corrected quantity;
- after readback;
- source reconciliation;
- affected alerts or recommendations.

Verify the role, tenant, warehouse, quantity, persisted result, and audit/activity evidence. Casual inventory editing is outside the pilot control model.

## 14. Order And Fulfillment Change

Direct order and fulfillment writes belong to the authorized integration roles and warehouse scope. They are appropriate only for an approved operational integration action, correction, or controlled test.

Do not allow Tenant Admin, Review Owner, Final Approver, or Escalation Owner identities to perform unrelated direct order/fulfillment mutations solely because they can access the UI.

After a consequential write, verify:

- role and warehouse authority;
- expected order/fulfillment result;
- no duplicate or wrong-object result;
- Activity/audit and realtime readback;
- source-system reconciliation.

## 15. Governance Assignment Change

Before changing Review Owner, Final Approver, or Escalation Owner:

1. Record current assignment, workflow/scenario state, warehouse, and reason.
2. Confirm the reassignment is not intended to bypass a decision.
3. Authorize the new assignment through the supported tenant administration path.
4. Verify the new assigned owner is allowed.
5. Verify the old/unassigned owner is denied.
6. Verify wrong-warehouse behavior.
7. Confirm workflow state remains valid and no approval/execution side effect changed.

If a governance change is made while a decision is active, hold the decision until assignment and state are revalidated.

## 16. Operational Policy Change

For implemented thresholds, settings, or policies, record old value, new value, reason, owner, affected scope, expected impact, and verification. Do not describe the product as having a generic policy engine when the current implementation exposes only specific supported settings.

Verify persisted readback, affected operator behavior, alerts/recommendations where relevant, and source agreement.

## 17. Deployment And Release Change

For every proposed deployment:

- record current known-good revision;
- record target revision and reason;
- identify frontend/backend/infrastructure scope;
- define affected feature and pilot workflow;
- identify owner and verifier;
- define health/readiness/auth/websocket checks;
- define rollback revision and compatibility concerns;
- define whether hosted proof is required.

After deployment, verify where relevant:

- frontend reachable;
- backend reachable;
- database ready;
- authentication/session ready;
- websocket info ready;
- Runtime and Release Trust evidence;
- affected feature behavior;
- security boundary if changed;
- no unrelated scope or contract drift.

Do not claim automatic rollback. A deployment is not accepted merely because the service started.

## 18. Release Acceptance States

| State | Meaning |
| --- | --- |
| `CHANGE PLANNED` | Request exists but is not authorized |
| `AUTHORIZED` | Required owner approval exists and implementation may begin |
| `IN PROGRESS` | Implementation is underway within the approved window |
| `DEPLOYED` | New state is present, but verification is incomplete |
| `VERIFYING` | Before/after, authority, data, runtime, and affected-flow checks are underway |
| `ACCEPTED` | Required verification passed and no unresolved material action remains |
| `ACCEPTED WITH ACTIONS` | Safe to continue with bounded follow-up owner, control, and checkpoint |
| `ROLLED BACK` | Prior known-good state restored and post-rollback verification passed |
| `FAILED` | Implementation or verification failed; affected workflow remains held |
| `HOLD` | Change cannot safely proceed or resume until a stated condition is met |

If the failure means trust is no longer established, the change becomes a Phase 12 incident.

## 19. Pilot Scope Change

The following are scope changes, not routine configuration:

- adding a warehouse or site;
- adding a connector lane;
- adding a new operational workflow;
- adding a user group or materially increasing operator count;
- enabling a previously unsupported recovery claim;
- adding consequential automation;
- expanding from one controlled lane to another business process.

Scope change requires:

1. request and business reason;
2. affected tenant/warehouse/workflow definition;
3. risk and capacity assessment;
4. new before-state evidence;
5. proof and onboarding plan;
6. Pilot Owner authorization;
7. explicit acceptance or rejection.

Evidence for one warehouse, connector, operator group, or workflow does not automatically prove an expanded scope.

## 20. Limitation-Boundary Change

Requests involving disabled-webhook replay, provider-managed restore guarantees, larger scale, HA/failover, MFA/SSO/OIDC, arbitrary integrations, generic approvals, or customer self-provisioning are not routine changes.

They require a new implementation and proof, or an explicit exclusion from the pilot. Do not remove a limitation from documentation because a customer requested it.

## 21. Security-Sensitive Change

Security-sensitive changes include:

- platform-owner credential rotation;
- tenant password reset;
- role elevation;
- warehouse-scope widening;
- session invalidation;
- security policy/configuration change;
- secret/configuration changes.

No plaintext secret belongs in a change record. Verify revocation, fresh-session identity, allowed action, denied action, and relevant leakage/security checks after the change.

## 22. Verification Model

Verification is specific to the change. Select the relevant evidence rather than running every check for every low-risk action.

Possible evidence includes:

- backend result and persisted state;
- frontend/UI readback;
- Activity and Runtime evidence;
- source reconciliation;
- expected allowed API;
- expected denied API;
- tenant and warehouse scope;
- realtime event or trustworthy snapshot;
- connector/import/replay state;
- absence of unintended side effects;
- release and deployment evidence;
- focused or full hosted proof when runtime/proof-covered behavior changed.

Verification must answer: what changed, what stayed unchanged, what was allowed, what was denied, and whether the source system agrees.

## 23. Failed Change And Phase 12 Transition

When verification fails:

1. Stop unrelated changes.
2. Preserve before/after evidence and request IDs.
3. Decide whether the change can be corrected immediately without compounding risk.
4. Decide whether rollback is safe and compatible.
5. Place the affected workflow on `HOLD` where required.
6. Check for data, duplicate, authority, scope, runtime, or security impact.
7. Create a Phase 12 incident when trust is no longer established.

```text
CHANGE FAILURE -> CLASSIFY -> HOLD/STOP -> PHASE 12 INCIDENT WHEN TRUST IS UNCERTAIN
```

Do not use a change record to conceal an incident.

## 24. Rollback Decision

Before rollback, answer:

- Was the prior state actually known-good?
- Is rollback compatible with current database/schema state?
- What business effects already occurred?
- Can those effects be reversed safely?
- Could rollback create duplicate, stale, or wrong-scope state?
- What source reconciliation is required afterward?
- Who owns and verifies the rollback?

Configuration or deployment rollback does not automatically reverse business effects already produced. Follow the Phase 12 recovery pack where data, replay, or restore is involved.

## 25. Change Communication

Use factual messages with no unsupported ETA:

| State | Suggested language |
| --- | --- |
| Planned | "A change to [scope] is proposed for [reason]. It is not yet authorized or active." |
| Authorized | "The [change] is authorized for [scope]. Verification and rollback ownership are assigned." |
| Starting | "The approved [change] is starting. The affected [workflow] is [continuing/held]." |
| Deployed | "The [revision/configuration] is deployed. Verification is in progress; it is not yet accepted." |
| Accepted | "The [change] passed the defined verification for [scope] and is accepted." |
| Accepted with actions | "The change is accepted for [scope] with [action], owner [owner], and review [checkpoint]." |
| Held | "The change is held because [factual condition]. No further implementation is authorized." |
| Failed | "The change did not pass verification. [Scope] remains held while we classify the result." |
| Rollback starting | "Rollback of [change] is authorized and starting. Existing business effects will be reconciled separately." |
| Rollback complete | "Rollback completed for [scope]. Post-rollback health, authority, data, and workflow verification passed/ remains open." |

## 26. Emergency Change

An emergency change may use a shortened approval path when immediate risk is greater than the change risk. It does not mean:

- skip evidence;
- skip authorization;
- skip verification;
- bypass backend authority;
- hide the reason or affected scope.

Record the emergency reason, approver, implementer, before state, action, after state, verification, and after-action review. An emergency change caused by an incident must link to the Phase 12 incident record.

## 27. Change History And Collision Control

Completed change records, Activity, Runtime, release evidence, and daily records together answer: **What changed between yesterday and today?** Do not invent a new runtime change-log system. Use the existing records and evidence references.

Potential collisions include:

- two people editing the same connector policy;
- role change during an active governance decision;
- deployment during replay recovery;
- warehouse-scope change during an operational write;
- mapping change during active inbound load.

When collision risk exists, hold one change, identify the owner, and sequence the work. An active incident takes priority over unrelated planned change in the affected area.

## 28. Customer Request Versus Authorization

A customer request is input, not authorization. Separate requester, authorizer, implementer, verifier, and customer approver where practical. Use proportionate control: do not create heavy bureaucracy for a low-risk routine action, but do not treat a material authority, scope, data, connector, or release change as routine.

## 29. Platform Owner Boundary

The Platform Owner may manage platform-level deployment, runtime, release, tenant metadata, and support changes. The Platform Owner must not use global authority to bypass tenant operational roles for business changes.

Tenant business changes use the correct tenant role and assignment. Platform support remains metadata-first and must not casually browse raw tenant orders, products, inventory, inbound/replay payloads, connector secrets, or customer credentials.

## 30. Daily Operating Boundary

Normal Phase 11 work does not require a formal Phase 13 record when it stays inside the approved scope and does not alter authority, configuration, mapping, policy, release, or pilot boundary. Examples include reviewing dashboard state, inspecting alerts, carrying out approved operational work, and performing a replay already authorized under the proven CSV lane.

Formal change management is required for material access, scope, connector, mapping, policy, governance, deployment, security, or pilot-scope changes.

## 31. Post-Change Monitoring

Some changes need monitoring after immediate verification:

- connector configuration or enablement;
- deployment/release;
- policy or threshold change;
- mapping correction;
- warehouse-scope change;
- pilot scope expansion.

Define the monitoring owner, evidence source, checkpoint, expected state, and reopen trigger. Reopen or create a Phase 12 incident if the same symptom returns, evidence conflicts, a new unauthorized action appears, reconciliation fails, or the workaround masks the cause.

## 32. Phase 14 Handoff

Phase 14 is the Pilot Completion Pack. Phase 13 should provide:

- completed change history;
- pilot scope changes;
- role and access changes;
- connector changes;
- data/mapping changes;
- deployment changes;
- rollbacks;
- failed changes;
- incident-linked changes;
- limitations added, removed, or reaffirmed;
- final operating scope;
- open actions and monitoring outcomes.

Do not start Phase 14 from this document.

## 33. Carried Limitations

Phase 13 preserves the Phase 12 and access-gate limitations:

- CSV is the proven replay recovery lane;
- disabled-webhook replay/readback is not deterministic on Render;
- import-run records lack authoritative warehouse association;
- provider-managed Render restore is not proven;
- load evidence is bounded and does not claim unrestricted Render scale;
- no HA or automatic failover claim exists;
- MFA/SSO/OIDC is not implemented;
- no arbitrary connector framework exists;
- no generic approval engine exists;
- unrestricted customer self-provisioning is not supported;
- Platform Owner support is metadata-first, not unrestricted tenant-data browsing;
- current Medium/Low authority limitations remain documented.

## 34. Practical Change Playbook

| Change type | Default risk | Requester | Authorizer | Implementer | Before evidence | Verification | Rollback | Incident trigger |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| User/role | Medium-High | Customer owner | Tenant Admin/Pilot Owner | Tenant Admin | Identity, old role/scope, reason | Fresh identity, allowed/denied action, sign-out | Restore prior role/scope if safe | Wrong authority or stale access persists |
| Warehouse scope | High | Customer owner | Tenant Admin/Pilot Owner | Tenant Admin | Old scope, warehouses, active work | A allowed, B denied/allowed, fresh session | Restore prior scope if no wrong-object effect | Empty scope, wrong warehouse, or mutation |
| Connector config | Medium-High | Integration owner | Integration Admin/Pilot Owner | Integration Admin | State, policy, source traffic | Connector/import/runtime/source evidence | Restore prior config or disable lane | Unknown inbound, duplicate, or source mismatch |
| Connector enable/disable | Medium-High | Integration owner | Integration Admin/Pilot Owner | Integration Admin | Traffic expectation, fallback | Expected failure/ingestion and replay boundary | Restore prior state and hold lane | Uncontrolled replay or freshness loss |
| Data mapping | Medium-High | Source owner | Source/Pilot Owner | Approved operator/engineering | Before value and source reference | Readback, source agreement, affected flow | Restore prior mapping if safe | Wrong data, duplicate, or unexplained mismatch |
| Inventory maintenance | Medium | Customer operator | Tenant Admin | Tenant Admin | SKU, warehouse, quantity, source | Quantity, source, activity, alerts | Correct through supported path | Wrong object, scope, or unreconciled state |
| Governance assignment | High | Pilot Owner | Tenant Admin/Pilot Owner | Tenant Admin | Current owner, scenario state | New allowed, old denied, preview denied | Restore assignment if no decision changed | Unauthorized approval or execution |
| Deployment/release | High | Engineering/Platform | Platform Owner/Pilot Owner | Engineering/Platform | Known-good revision, rollback, health plan | Frontend/backend/DB/auth/WS/runtime/affected proof | Compatible prior revision | Readiness, security, data, or proof regression |
| Pilot scope | High-Critical | Customer/Pilot Owner | Pilot Owner | Platform/implementation team | Current scope and proof boundary | New proof, onboarding, capacity, access, reconciliation | Remove new scope and hold it | Unsupported scope or trust not proven |

## 35. Synthetic Change Examples

### A. Integration Operator Needs Warehouse B

This is a high-risk warehouse-scope change. Record current scope, reason, source owner, and active work; authorize with Tenant Admin/Pilot Owner; implement; verify Warehouse B allowed and Warehouse A behavior unchanged as intended. If an empty scope appears unexpectedly, hold the identity and treat it as a high-severity access concern.

### B. Connector Policy Must Change

Capture current policy, expected traffic, source owner, and replay implications. Authorize Integration Admin. Implement one policy change, test approved input, inspect import/Runtime/Activity, and monitor. If failed inbound or duplicate risk is unclear, hold the connector and use Phase 12 recovery controls.

### C. Product/SKU Mapping Correction

Use source-system evidence, record the old and desired mapping, change through the supported path, verify catalog and affected inbound behavior, and reconcile. If the source and SynapseCore disagree after correction, the change fails and may become a Phase 12 data incident.

### D. Review Owner Reassignment

Record current assignment and scenario state. Do not reassign to bypass a decision. Verify the new owner is allowed, the old owner is denied, wrong warehouse is denied, and no approval or execution state changed. If governance state becomes ambiguous, hold execution.

### E. New Backend Revision

Record known-good and target commits, scope, rollback, health plan, and proof impact. Deploy, warm, verify frontend/backend/DB/auth/WS/runtime, run affected proof, and accept only after evidence passes. A startup response alone is `DEPLOYED`, not `ACCEPTED`.

### F. Second Warehouse Requested

This is a pilot-scope change, not a normal warehouse edit. Assess tenant isolation, scope, data onboarding, operator roles, connector behavior, capacity, and proof. Proceed only after new evidence and Pilot Owner authorization. Do not infer the first warehouse proves the second.

### G. Unsupported Disabled-Webhook Recovery Requested

Do not remove the limitation or claim support. Hold the request outside the approved pilot lane. Either implement and re-prove the capability through a future authorized change, or explicitly exclude it and use the proven CSV recovery lane.

## 36. Runtime Review And Phase 13 Boundary

This is a documentation and operational-control phase. Repository inspection did not establish a new Critical or High runtime defect in the current change-control model. No backend/frontend behavior is changed by this pack.

Any future change that exposes a genuine authority, tenant-isolation, corruption, secret, or recovery defect must stop the change process and return to Phase 12 incident control before further implementation.

## 37. Phase 13 Completion Gate

Phase 13 is complete when:

1. every material Company 1 change has a request, risk, authority, before state, verification, and rollback decision;
2. routine operation is not overloaded with unnecessary records;
3. access, scope, connector, mapping, inventory, governance, release, security, and pilot-scope procedures are understood;
4. failed changes transition cleanly to Phase 12 when trust is uncertain;
5. accepted limitations remain visible;
6. change history can explain the difference between operating days;
7. Phase 14 receives the required change and scope evidence;
8. no unresolved Critical or High change-control blocker exists.

## Related References

- [Company 1 Daily Operator SOP](company-daily-operator-sop.md)
- [Company 1 Incident Rollback Recovery Pack](company-incident-rollback-recovery-pack.md)
- [Role Authority Hardening Gate](role-authority-hardening-gate.md)
- [Platform Control Plane And Tenant Access Boundary](platform-control-plane-access-boundary.md)
- [Integration Operations](integration-operations.md)
- [Replay And Recovery](replay-recovery.md)
- [Backup And Restore Runbook](backup-restore-runbook.md)
- [Release Engineering](release-engineering.md)
- [Support Playbook](support-playbook.md)
- [API Surface Reference](api-surface-reference.md)

## Final Principle

An active pilot must be changeable without becoming uncontrolled. Every material change needs a known before state, a legitimate owner, a bounded implementation, evidence-based verification, and a safe answer to what happens if it fails.
