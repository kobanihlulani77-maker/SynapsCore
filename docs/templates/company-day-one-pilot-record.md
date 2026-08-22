# Company Day-One Pilot Record

Use this controlled record with the [Company 1 Day-One Pilot Guide](../company-day-one-pilot-guide.md). Complete it for the actual company operating window.

Do not record passwords, tokens, secret values, session cookies, connector credentials, raw inbound/replay payloads, database credentials, platform credentials, or unnecessary personal data. Use sanitized evidence references.

## Record Control

| Field | Value |
| --- | --- |
| Record ID | |
| Company | |
| Tenant/workspace code | |
| Pilot scope reference | |
| Phase 8 authorization reference | |
| Phase 9 handover reference | |
| Release/version/commit | |
| Operating date | |
| Timezone | |
| Planned window | |
| Pilot Owner | |
| Customer Operations Owner | |
| SynapseCore Platform Owner | |
| Record status | `DRAFT` / `ACTIVE` / `CLOSED` |

## Entry Conditions

Use `PASS`, `PASS WITH ACCEPTED CONDITION`, `FAIL`, or `OUT OF PILOT`.

| Condition | Result | Evidence reference | Owner | Condition/action |
| --- | --- | --- | --- | --- |
| Actual company tenant verified | | | | |
| Phase 8 authorizes handover | | | | |
| Required Phase 9 handover complete | | | | |
| Approved users reconciled | | | | |
| Bootstrap/internal identities disposed | | | | |
| Roles confirmed | | | | |
| Warehouse scopes confirmed | | | | |
| Approved connector configured | | | | |
| Approved data loaded/reconciled | | | | |
| Operational baseline frozen | | | | |
| Tenant Runtime checked | | | | |
| Platform Runtime checked | | | | |
| Realtime checked | | | | |
| Backup/recovery evidence current | | | | |
| Support contacts confirmed | | | | |
| Pilot scope confirmed | | | | |
| Customer acknowledgement complete | | | | |

Entry decision: `GO` / `HOLD` / `STOP`

Decision by:

Date/time:

## Participants And Authority

| Participant | Organization | SynapseCore role/responsibility | Warehouse scope | Present? | Decision authority |
| --- | --- | --- | --- | --- | --- |
| Platform Owner / Operator | SynapseCore | | | | |
| Tenant Admin | Company | | | | |
| Integration Admin | Company | | | | |
| Integration Operator | Company | | | | |
| Review Owner | Company | | | | |
| Final Approver | Company | | | | |
| Escalation Owner | Company | | | | |
| Business/Operations Observer | Company | | | | |
| Data/Source Owner | Company | | | | |
| Pilot Owner | | | | | `GO` / `HOLD` / `STOP` |

Roles not used by this pilot:

Approved role combinations and separation-of-duty controls:

## First 30 Minutes

| Sequence | Check | Result | Evidence reference | Owner | Decision |
| --- | --- | --- | --- | --- | --- |
| Platform opening | Platform Overview | | | | |
| Platform opening | Tenant Directory Company identity/support state | | | | |
| Platform opening | Platform Runtime/readiness | | | | |
| Platform opening | Platform Activity | | | | |
| Platform opening | Release Trust | | | | |
| Tenant opening | Tenant/identity correct | | | | |
| Tenant opening | Users/roles/scopes correct | | | | |
| Tenant opening | No unexpected privileged/bootstrap identity | | | | |
| User opening | Required user sign-ins | | | | |
| User opening | Expected navigation and reads | | | | |
| User opening | No foreign/unexpected data | | | | |
| Connector opening | Connector/source/type/enabled state | | | | |
| Connector opening | Recent inbound/import state | | | | |
| Connector opening | Replay Queue inspected, no action forced | | | | |

T+30 decision: `GO` / `HOLD` / `STOP`

## First Live Data Reconciliation

Use sanitized identifiers. Mark excluded domains `OUT OF PILOT`.

| Domain | Source reference/value | SynapseCore reference/value | Expected interpretation | Result | Owner |
| --- | --- | --- | --- | --- | --- |
| Catalog/products | | | | | |
| Inventory | | | | | |
| Orders | | | | | |
| Alerts | | | | | |
| Recommendations | | | | | |
| Connector evidence | | | | | |
| Tenant Activity | | | | | |

Source-of-truth mismatches:

| Issue ID | Domain | Source value | SynapseCore value | Classification | Immediate control | Owner | Resolution state |
| --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | | | |

## Realtime Confirmation

| Check | Result | Evidence reference | Impact/condition |
| --- | --- | --- | --- |
| Approved realtime endpoint trust evidence | | | |
| Authenticated tenant connection state | | | |
| Safe/natural change observed | | | |
| Expected page updated | | | |
| Snapshot/API agrees | | | |
| No foreign/wrong-warehouse update | | | |
| Degraded fallback behavior, if applicable | | | |

Realtime decision: `LIVE` / `DEGRADED WITH CONTROL` / `HOLD` / `STOP`

## Alert And Recommendation Observation

| Type | Sanitized reference | Supporting evidence | Human interpretation | Permitted next step | Outcome |
| --- | --- | --- | --- | --- | --- |
| Alert | | | | | |
| Recommendation | | | | | |

Confirm no recommendation was treated as automatic execution: `YES` / `NO`

## Governance First Use

Status: `IN PILOT` / `OUT OF PILOT` / `NOT EXERCISED ON DAY ONE`

| Stage | Actor/role | Warehouse | Evidence reference | Result |
| --- | --- | --- | --- | --- |
| Preview | | | | |
| Compare | | | | |
| Save plan | | | | |
| Review decision | `REVIEW_OWNER` | | | |
| Final decision if required | `FINAL_APPROVER` | | | |
| Escalation acknowledgement if applicable | `ESCALATION_OWNER` | | | |
| Governed execution if authorized | `REVIEW_OWNER` / `FINAL_APPROVER` | | | |
| Live result/history/activity | | | | |

Wrong-role/wrong-scope denial evidence reference:

## Integration Failure And Recovery

Status: `NO FAILURE` / `OBSERVED` / `RECOVERED` / `HELD` / `ESCALATED`

| Field | Value |
| --- | --- |
| Sanitized failure reference | |
| Connector/source/type | |
| Warehouse | |
| Failure code/message summary | |
| Evidence preserved | |
| Cause classification | |
| Supported correction | |
| Replay role/authorization | |
| Eligibility confirmed | |
| Duplicate check | |
| Source owner confirmation | |
| Replay attempted | `NO` / `YES - ONCE` |
| Result appears once | |
| Replay/import/activity evidence agrees | |
| Final disposition | |

Recovery lane used: `CSV PROVEN LANE` / `OTHER - ESCALATION REQUIRED` / `NOT APPLICABLE`

Confirm disabled-webhook replay was not claimed as proven: `YES` / `NO`

## Activity And Runtime Separation

| Check | Result | Evidence reference |
| --- | --- | --- |
| Tenant Activity contains only appropriate Company tenant activity | | |
| Platform Activity remains metadata-level | | |
| Tenant Runtime answers tenant trust question | | |
| Platform Runtime answers platform trust question | | |
| Tenant user denied platform authority | | |
| Platform surface does not expose raw tenant payloads | | |

## Observation Checkpoints

| Checkpoint | Platform/tenant trust | Connector/replay | Data reconciliation | Governance/intelligence | Customer/support observations | Decision | Decision owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| T+0 | | | | | | `GO` / `HOLD` / `STOP` | |
| T+30 minutes | | | | | | `GO` / `HOLD` / `STOP` | |
| T+2 hours | | | | | | `GO` / `HOLD` / `STOP` | |
| Midday | | | | | | `GO` / `HOLD` / `STOP` | |
| End of day | | | | | | `GO` / `HOLD` / `STOP` | |

## Issues

Primary types: `ACCESS`, `DATA`, `CONNECTOR`, `REPLAY`, `GOVERNANCE`, `REALTIME`, `PLATFORM`, `USER EXPERIENCE`, `SCOPE / AUTHORIZATION`.

| Issue ID | Type | Time | Observer | What happened | Tenant/warehouse | Evidence | Impact/severity | Owner | Immediate control | State | Next action |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | | | | | | | |

For a material incident, open a separate [Pilot Incident Log](pilot-incident-log-template.md) and reference it here.

## Stop-Condition Review

| Stop condition | Observed? | Evidence/decision |
| --- | --- | --- |
| Cross-tenant data exposure | | |
| Unauthorized Platform Control Plane access | | |
| Unexpected raw tenant data exposure | | |
| Warehouse-scope bypass | | |
| Authentication/authorization bypass | | |
| Unauthorized consequential action | | |
| Duplicate consequential execution | | |
| Unexplained material source mismatch | | |
| Loss of trustworthy audit/evidence | | |
| Critical security/secret issue | | |
| Unrecoverable/unexplained corruption | | |
| Wrong-object governance action | | |
| Sustained backend unavailability | | |

## Customer Communications

| Time | Condition | Approved factual message reference | Audience | Sent by | Next update |
| --- | --- | --- | --- | --- | --- |
| | | | | | |

## Role-By-Role Day-One Record

Use only roles assigned in the approved Company 1 roster.

| Role | Identity correct | Scope correct | Expected navigation/read | Allowed action | Forbidden route/API denied | Platform denied | Sign-out | Issues |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `TENANT_ADMIN` | | | | | | | | |
| `INTEGRATION_ADMIN` | | | | | | | | |
| `INTEGRATION_OPERATOR` | | | | | | | | |
| `REVIEW_OWNER` | | | | | | | | |
| `FINAL_APPROVER` | | | | | | | | |
| `ESCALATION_OWNER` | | | | | | | | |

## End-Of-Day Review

| Review area | Result | Evidence/action |
| --- | --- | --- |
| Uptime/degraded periods | | |
| Connector conditions | | |
| Failed inbound | | |
| Replays and duplicate safety | | |
| Alerts/recommendations | | |
| Governance actions | | |
| Access/scope issues | | |
| Source reconciliation | | |
| Activity/audit | | |
| Customer observations | | |
| Support actions | | |
| Backup/recovery posture | | |
| Unresolved problems | | |

## Limitations Carried Forward

| Limitation | Encountered? | Impact/control | Owner | Review point |
| --- | --- | --- | --- | --- |
| Disabled-webhook replay/readback not proven | | Use proven CSV recovery lane; escalate webhook failure/readback mismatch | | |
| Import runs lack authoritative warehouse association | | Do not claim import-run warehouse attribution | | |
| Stale session may require fresh sign-in | | Preserve evidence; fresh sign-in; verify denial | | |
| Provider-managed restore not drilled | | Existing systems remain authoritative; do not expand reliance | | |
| Live Render saturation not proven | | Remain inside accepted small pilot envelope | | |
| Single deployment posture/no HA claim | | Use stop/fallback process | | |

## Final Day-One Decision

Select exactly one:

- `DAY ONE ACCEPTED`
- `DAY ONE ACCEPTED WITH ACTIONS`
- `DAY ONE NOT ACCEPTED - PILOT HOLD`

Decision:

Decision rationale:

Critical issues open:

High issues open:

Actions carried forward:

Compensating controls:

Next operating boundary:

Phase 11 Daily Operator SOP authorized: `YES` / `NO`

## Sign-Off

| Authority | Name/role | Decision | Date/time |
| --- | --- | --- | --- |
| SynapseCore Platform Owner | | | |
| SynapseCore Pilot Owner | | | |
| Customer Operations Owner | | | |
| Customer Data/Source Owner, if required | | | |
| Customer Governance Owner, if required | | | |

## Phase 10 Control-Model Verdict

Select exactly one for the prepared Phase 10 process:

- `COMPANY PILOT PHASE 10 ACCEPTED`
- `COMPANY PILOT PHASE 10 ACCEPTED WITH DOCUMENTED LIMITATION`
- `COMPANY PILOT PHASE 10 NOT ACCEPTED - DAY-ONE CONTROL MODEL INCOMPLETE`
