# Official Pilot Program

This document is the canonical operating model for selecting, preparing, running, measuring, pausing, expanding, and completing SynapseCore pilots.

Supporting pilot documents may provide checklists, FAQs, rollback detail, or release-candidate context, but this document is the source of truth for the pilot program.

## Pilot Purpose

The pilot exists to answer one practical question:

**Can SynapseCore improve the visibility, coordination, and recovery of one real operational workflow without disrupting the company's existing operations?**

SynapseCore pilots exist to prove operational usefulness inside a narrow, controlled lane. They do not try to prove full enterprise production readiness, ERP replacement, every possible integration, global high availability, or unrestricted autonomous execution.

The first pilot lane is intentionally narrow because the platform should earn trust through evidence before scope expands. A narrow lane makes failures easier to classify, rollback easier to execute, operator feedback easier to understand, and tenant isolation easier to prove.

During the pilot, the company's ERP, WMS, ecommerce platform, or manual fallback remains the system of record. SynapseCore acts as an operational visibility layer, coordination layer, recovery layer, and decision-support layer.

## Pilot Principles

- Existing systems remain the system of record.
- There is no company-wide rollout during the pilot.
- Product scope does not expand during the pilot unless a formal pilot review approves a targeted change.
- Tenant data must remain isolated.
- Human approval remains in control for governed actions.
- Failures remain visible instead of being hidden behind fake success states.
- Recovery must be traceable through replay, history, runtime state, or incident evidence.
- Evidence drives decisions.
- The pilot must remain reversible.
- Operator feedback is treated as product evidence.
- Hosted proof remains a deployment confidence gate, not a sales demonstration.
- The pilot pauses when runtime truth, tenant isolation, replay correctness, or data trust is in question.

## Company Selection Criteria

Best-fit company types:

- Small-to-mid distributor.
- Warehouse operator.
- Ecommerce fulfilment company.
- Logistics operator with one suitable site.

Required company traits:

- Executive sponsor.
- Operations owner.
- Technical contact.
- Selected warehouse or operating site.
- Small operator team.
- Catalogue and inventory subset.
- One controllable integration lane.
- Existing fallback process.
- Willingness to attend weekly reviews.
- Agreement that SynapseCore does not replace the system of record during the pilot.

Poor-fit company traits:

- Expects ERP replacement.
- Expects company-wide production deployment immediately.
- Requires a broad connector marketplace before starting.
- Requires autonomous execution without human governance.
- Requires global high availability during the pilot.
- Requires unsupported industry workflows.
- Has no operational owner.
- Has no fallback process.
- Cannot isolate one safe operational lane.

## Two-Company Pilot Model

SynapseCore uses a staged two-company pilot model.

Stage 1: Company 1 proves operational value.

Stage 2: Company 2 proves repeatability, supportability, and real tenant isolation.

Do not onboard both companies simultaneously. Company 2 begins only after Company 1 passes the defined success gate or produces only targeted, manageable issues that are understood and corrected.

The first pilot answers:

**Does SynapseCore solve a real operational problem?**

The second pilot answers:

**Can SynapseCore solve the same class of problem for another company independently and safely?**

## Company 1 Pilot

Recommended company: small-to-mid distributor or warehouse operator.

Recommended operational lane: inbound order and inventory exception management.

Company 1 scope:

- One workspace.
- One warehouse or operating site.
- One tenant administrator.
- Three to five operators.
- Limited product catalogue.
- Controlled inventory baseline.
- One integration lane.
- Replay/recovery workflow.
- Alerts and recommendations.
- One approval/scenario workflow.
- Runtime trust monitoring.

SynapseCore responsibilities:

- Operator authentication.
- Role-based access.
- Catalogue visibility.
- Inventory visibility.
- Order visibility.
- One inbound integration lane.
- Failure detection.
- Replay/recovery.
- Alerts and recommendations.
- Controlled scenario approval/execution.
- Runtime trust and realtime dashboard state.

Outside SynapseCore during Company 1:

- ERP replacement.
- WMS replacement.
- Financial accounting.
- Payroll.
- Full procurement.
- Every warehouse.
- Every integration.
- Unrestricted autonomous execution.
- Company-wide production rollout.

## Company 1 Timeline

Total Company 1 duration: 6 weeks.

### Week 0: Preparation

Objective: prove the pilot can begin safely.

Activities:

- Record backup/restore drill.
- Verify cookie/session/CSRF posture.
- Create pilot workspace.
- Verify tenant isolation posture.
- Confirm admin and operator roles.
- Confirm selected site.
- Prepare catalogue subset.
- Prepare inventory baseline.
- Define one integration lane.
- Confirm fallback process.
- Assign rollback owner.
- Confirm technical and operational contacts.
- Run live connection check and require `PROOF_ALLOWED=True`.
- Confirm hosted proof is green on the deployed version.

Evidence collected:

- Live connection output.
- Hosted proof result.
- Backup/restore evidence.
- Workspace and role setup notes.
- System-of-record fallback confirmation.

Success question:

**Can the pilot begin without creating avoidable operational or data-risk exposure?**

Stop conditions:

- `PROOF_ALLOWED=False`.
- Backup cannot be restored.
- Tenant isolation is not verified.
- Roles are unclear.
- Fallback owner is not assigned.
- Integration lane cannot be controlled.

### Week 1: Onboarding And Operator Understanding

Objective: determine whether operators can understand the platform without continuous technical assistance.

Activities:

- Onboard operators.
- Walk through sign-in and session behavior.
- Walk through the command-center dashboard.
- Confirm catalogue and inventory visibility.
- Explain runtime trust.
- Explain healthy, degraded, waiting, and reconnecting states.
- Confirm roles and permissions.

Evidence collected:

- Onboarding time.
- Login problems.
- Terminology confusion.
- Navigation confusion.
- Operator confidence.
- Questions asked.

Success question:

**Can operators understand the selected lane and trust surfaces well enough to participate in the pilot?**

Stop conditions:

- Operators cannot sign in reliably.
- Role visibility is wrong.
- Runtime state is misleading.
- Operators cannot identify the fallback process.

### Week 2: Controlled Operational Flow

Objective: determine whether SynapseCore improves visibility into the selected operational lane.

Activities:

- Process controlled inbound events or orders.
- Observe inventory and order visibility.
- Review alerts and recommendations.
- Confirm dashboard updates.
- Record operator actions.

Evidence collected:

- Number of events processed.
- Successful events.
- Failed events.
- Alerts generated.
- Recommendations generated.
- API failures.
- Websocket/realtime stability.
- Operator interventions.
- Manual reconciliation effort.

Success question:

**Can SynapseCore make the selected operational lane easier to see, coordinate, and understand?**

Stop conditions:

- Unexplained data corruption.
- Severe mismatch with the system of record caused by SynapseCore.
- Persistent backend readiness failure.
- Persistent realtime failure that hides operational truth.

### Week 3: Failure And Replay/Recovery Exercise

Objective: determine whether operators can safely recover a failed operational event.

Activities:

- Introduce a controlled invalid or blocked inbound event.
- Confirm failure appears in replay queue.
- Review error reason.
- Correct connector or input condition.
- Run Replay Into Live Flow.
- Confirm recovered order/event appears.
- Confirm history and runtime surfaces reflect recovery.

Evidence collected:

- Time to detect.
- Time to understand.
- Time to recover.
- Replay success.
- Duplicate prevention.
- Operator understanding.
- Escalation required.

Success question:

**Can operators recover a failed event without hidden data loss or unsafe duplicate records?**

Stop conditions:

- Replay creates duplicate records.
- Replay creates the wrong operational record.
- Replay bypasses tenant, role, or approval expectations.
- Failure is not visible.

### Week 4: Approval And Scenario Execution

Objective: determine whether SynapseCore supports governed operational decisions without bypassing human control.

Activities:

- Create a controlled scenario.
- Route it to approval.
- Test approve and reject paths.
- Execute approved scenario.
- Verify resulting order, inventory, or operational state.
- Confirm audit/history visibility.

Evidence collected:

- Approval turnaround.
- Role-gating accuracy.
- Operator confidence.
- Scenario clarity.
- Execution reliability.
- Unexpected system state.

Success question:

**Can SynapseCore support human-governed operational execution safely?**

Stop conditions:

- Role or approval bypass.
- Scenario execution creates incorrect operational state.
- Audit/history is missing.
- Operators cannot determine approval ownership.

### Week 5: Evidence Review

Objective: decide whether to continue, continue with targeted fixes, pause and correct, or end the pilot.

Activities:

- Review with executive sponsor.
- Review with operations owner.
- Review with technical contact.
- Review with participating operators.
- Review technical incidents.
- Review operator feedback.
- Review replay outcomes.
- Review approval outcomes.
- Review operational metrics.
- Review onboarding friction.
- Review support effort.
- Review system trust.

Evidence collected:

- Pilot evidence report.
- Incident log.
- Operator feedback summary.
- Technical findings.
- Operational findings.
- Improvement backlog.
- Continue/pause/end decision.
- Lessons learned.
- Proof status.
- Next-release recommendations.

Success question:

**Did SynapseCore create enough operational value, safely enough, for the company to continue or expand in a controlled way?**

Stop conditions:

- Critical unresolved defect.
- Tenant isolation concern.
- Company does not trust runtime state.
- Support load is not manageable.
- Existing system of record conflicts are unresolved.

## Company 2 Pilot

Recommended company: ecommerce fulfilment company or second warehouse/logistics operator.

Company 2 should begin only after Company 1 evidence review. The goal is not new functionality. The goal is repeatability and tenant isolation.

Company 2 must use:

- Separate workspace.
- Separate users.
- Separate catalogue.
- Separate inventory.
- Separate connector/input source.
- Separate approvals.
- Separate operational data.

Company 2 proves:

- Repeatable onboarding.
- Tenant isolation.
- Independent operations.
- Shared deployment safety.
- Manageable support across two companies.
- Independent replay queues.
- Independent dashboards.
- Independent alerts and recommendations.
- Independent users and roles.

Company 1 may remain active during Company 2 only if Company 1 was stable and approved to continue.

## Multi-Tenant Validation

Explicitly verify:

- Company A users cannot authenticate into Company B workspace.
- Company A APIs cannot access Company B data.
- Company A websocket subscriptions cannot receive Company B events.
- Orders remain tenant-scoped.
- Inventory remains tenant-scoped.
- Replay remains tenant-scoped.
- Alerts and recommendations remain tenant-scoped.
- Scenarios and approvals remain tenant-scoped.
- Users and settings remain tenant-scoped.
- One tenant's connector failure does not affect another tenant.
- Deployment changes affect both tenants predictably without mixing data.

Multi-tenant validation fails immediately if either company can see, modify, replay, approve, execute, or receive realtime updates for the other company's data.

## Pre-Pilot Conditions

| Condition | Classification | Notes |
| --- | --- | --- |
| Live connection gate returns `PROOF_ALLOWED=True` | COMPLETE | Current readiness must still be checked immediately before pilot start. |
| Hosted proof is green on deployed version | COMPLETE | Must be refreshed after runtime/proof-covered changes. |
| Backup/restore drill recorded | MUST COMPLETE BEFORE PILOT | Required before live company data. |
| Cookie/session/CSRF posture verified | MUST COMPLETE BEFORE PILOT | Required for browser-based hosted use. |
| Repository hygiene complete | MUST COMPLETE BEFORE EXTERNAL REVIEW | Remove accidental artifacts before sharing with reviewers. |
| Isolated workspace prepared | MUST COMPLETE BEFORE PILOT | Pilot must not share tenant state. |
| Roles verified | MUST COMPLETE BEFORE PILOT | Tenant admin, operator/planner, and integration admin should be clear. |
| Integration lane controlled | MUST COMPLETE BEFORE PILOT | No uncontrolled production ingestion. |
| Rollback owner assigned | MUST COMPLETE BEFORE PILOT | Someone must own pause/recovery decisions. |
| System of record confirmed | MUST COMPLETE BEFORE PILOT | ERP/WMS/manual fallback remains authoritative. |
| Support contacts confirmed | MUST COMPLETE BEFORE PILOT | Executive, operational, and technical contacts must be named. |
| Weekly review schedule confirmed | OPTIONAL DURING PILOT | Strongly recommended before Week 1. |
| Operator feedback template prepared | OPTIONAL DURING PILOT | Useful for consistent evidence capture. |

## Evidence Collection

Technical evidence:

- Health/readiness.
- Auth/session stability.
- Websocket stability.
- API failures.
- Unexplained `5xx` responses.
- Replay success.
- Scenario approval/execution.
- Tenant-isolation evidence.
- Backup/restore evidence.

Operational evidence:

- Events/orders monitored.
- Inventory exceptions.
- Failed events.
- Recovered events.
- Alert usefulness.
- Recommendation usefulness.
- Time to detect.
- Time to recover.
- Manual reconciliation effort.

Human evidence:

- Operator confidence.
- Terminology clarity.
- Navigation friction.
- Support requests.
- Repeated mistakes.
- Missing information.
- Trust in runtime status.

Business evidence:

- Improved visibility.
- Reduced uncertainty.
- Clearer ownership.
- Faster issue detection.
- Better recovery coordination.
- Willingness to continue.

Do not fabricate ROI. Report observed evidence and proposed targets separately.

## Success Gates

Pilot 1 succeeds only if:

- There is no tenant-isolation concern.
- There is no unexplained corruption.
- There is no role bypass.
- Replay does not create duplicates.
- Operators can use the selected lane.
- Runtime state is understandable.
- Failure and recovery are visible.
- Support load is manageable.
- The company wants to continue.

Company 2 succeeds only if:

- The Company 1 value is repeatable.
- Tenant isolation remains intact.
- Onboarding is repeatable.
- Support remains manageable.
- Deployment changes do not mix tenant data.
- Independent replay, dashboard, alert, recommendation, order, inventory, user, setting, scenario, and approval state remains intact.

No expansion happens automatically after either success gate.

## Stop Conditions

Pause immediately if any occur:

- Tenant leakage.
- Data corruption.
- Replay duplicate or wrong records.
- Unexplained data loss.
- Role or approval bypass.
- Restore failure.
- Persistent auth instability.
- Persistent backend readiness failure.
- Persistent realtime failure hiding truth.
- Customer-data security concern.
- Severe mismatch caused by SynapseCore.

## Rollback Sequence

1. Stop new integration input.
2. Preserve evidence and logs.
3. Keep the existing system of record active.
4. Disable affected connector.
5. Reconcile records against the system of record.
6. Classify the issue.
7. Fix only the exact defect.
8. Run live checks.
9. Run hosted proof if covered behavior changed.
10. Resume only with company and technical approval.

## Pilot Graduation Criteria

A pilot is considered complete only when:

- The agreed operational lane has been completed.
- Operators are comfortable using the selected lane.
- No critical unresolved defects remain.
- Replay/recovery has been tested.
- Tenant isolation has been proven.
- Success evidence has been reviewed.
- The company wants to continue or has made an explicit end decision.
- Engineering approves continuation.
- Support posture is acceptable.
- Proof status is recorded.

Graduation does not automatically mean broad production rollout.

## Expansion Criteria

Expansion happens only if:

- The pilot passed.
- Evidence supports expansion.
- The company requests expansion.
- Engineering approves expansion.
- Infrastructure can support expansion.
- New scope remains reversible.
- New scope does not bypass human governance.
- New scope does not weaken proof, runtime trust, or tenant isolation.

Expansion candidates may include more operators, a larger catalogue subset, a second site, or another controlled integration lane. They should not include unsupported enterprise-scale claims without additional hardening.

## Program Timeline

Recommended program timeline:

- Company 1 preparation and execution: 6 weeks.
- Evidence review and targeted fixes: 2-4 weeks.
- Company 2 preparation and execution: 4-5 weeks.
- Final multi-tenant review: 1 week.

Expected total: 13-16 weeks.

Do not rush Company 2. The second pilot is the repeatability and tenant-isolation proof, not a sales acceleration step.

## Pilot Outputs

At the end of each pilot, produce:

- Pilot evidence report.
- Incident log.
- Operator feedback summary.
- Technical findings.
- Operational findings.
- Improvement backlog.
- Continue/pause/end decision.
- Lessons learned.
- Proof status.
- Next-release recommendations.

Recommended templates:

- [templates/pilot-evidence-template.md](templates/pilot-evidence-template.md)
- [templates/pilot-weekly-review-template.md](templates/pilot-weekly-review-template.md)
- [templates/pilot-final-report-template.md](templates/pilot-final-report-template.md)
- [templates/pilot-incident-log-template.md](templates/pilot-incident-log-template.md)

## Official Commands

Run the live connection gate before pilot start:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Proceed only when:

```text
FRONTEND_UP=True
BACKEND_UP=True
DB_READY=True
AUTH_READY=True
WS_READY=True
PROOF_ALLOWED=True
```

Prepare hosted proof when the live gate allows it:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
```

Run hosted proof from the frontend:

```powershell
cd frontend
npm.cmd run test:e2e:prod
```

Hosted proof should not run when backend readiness, DB readiness, auth/session, or websocket trust is unhealthy.
