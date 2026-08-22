# Company Daily Operator Record

Use this record with the [Company 1 Daily Operator SOP](../company-daily-operator-sop.md) for each normal pilot operating day.

Do not record passwords, tokens, secret values, session cookies, connector credentials, raw inbound/replay payloads, database credentials, platform credentials, or unnecessary personal data. Use sanitized evidence references.

## Record Control

| Field | Value |
| --- | --- |
| Record ID | |
| Company | |
| Tenant/workspace | |
| Operating date | |
| Timezone | |
| Release/version/commit | |
| Approved pilot scope reference | |
| Day-One acceptance reference | |
| Planned operating window | |
| Pilot Owner | |
| Customer Operations Owner | |
| SynapseCore Platform Owner | |
| Record status | `DRAFT` / `ACTIVE` / `CLOSED` |

## Prior-Day Carry-Forward

| Item ID | Severity | Category | Affected scope | Current control | Owner | Required opening action | State |
| --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | | | |

Previous closing state:

Unresolved `STOP` condition present: `YES` / `NO`

If yes, normal operation did not start: `CONFIRMED` / `NOT CONFIRMED`

## Daily Entry Conditions

Use `PASS`, `PASS WITH CONTROL`, `FAIL`, or `OUT OF PILOT`.

| Condition | Result | Evidence reference | Owner | Action/control |
| --- | --- | --- | --- | --- |
| Phase 10 Day One accepted | | | | |
| Tenant active/correct | | | | |
| Required users/operators active | | | | |
| Roles expected | | | | |
| Warehouse scopes expected | | | | |
| Connector configuration unchanged/approved | | | | |
| Pilot scope unchanged/approved | | | | |
| Source systems available enough | | | | |
| Platform Runtime acceptable | | | | |
| Tenant Runtime acceptable | | | | |
| Realtime state known | | | | |
| Prior `STOP` resolved | | | | |
| Prior `HOLD` items reviewed | | | | |

Opening decision: `GO` / `HOLD` / `STOP`

Decision owner and date/time:

## Platform Owner Opening

| Check | State | Evidence reference | Issue/action |
| --- | --- | --- | --- |
| Platform Overview | | | |
| Company tenant in Tenant Directory | | | |
| Platform Runtime/dependencies | | | |
| Platform Activity | | | |
| Release Trust | | | |
| Connector/replay/import attention | | | |
| Prior support issues | | | |

Platform opening state: `HEALTHY` / `DEGRADED` / `HOLD` / `STOP`

## Customer Tenant Opening

| Check | Result | Evidence reference | Issue/action |
| --- | --- | --- | --- |
| Tenant identity correct | | | |
| User identity correct | | | |
| Expected navigation | | | |
| Warehouse scope correct | | | |
| Tenant Runtime | | | |
| Recent Tenant Activity | | | |
| Connector state where relevant | | | |
| No foreign/unexpected data | | | |
| No unexpected role/scope change | | | |

## Role Checks And Actions

Complete only for roles active that day.

| Role | Identity/scope checked | Expected work | Actions performed | Prohibited boundary respected | Escalations | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| `TENANT_ADMIN` | | | | | | |
| `INTEGRATION_ADMIN` | | | | | | |
| `INTEGRATION_OPERATOR` | | | | | | |
| `REVIEW_OWNER` | | | | | | |
| `FINAL_APPROVER` | | | | | | |
| `ESCALATION_OWNER` | | | | | | |

## Operating Checkpoints

| Checkpoint | Platform/Tenant Runtime | Connector/replay | Realtime | Reconciliation | Alerts/recommendations | Governance | Decision | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Start of day | | | | | | | `GO` / `HOLD` / `STOP` | |
| Mid-morning | | | | | | | `GO` / `HOLD` / `STOP` | |
| Midday | | | | | | | `GO` / `HOLD` / `STOP` | |
| Mid-afternoon | | | | | | | `GO` / `HOLD` / `STOP` | |
| End of day | | | | | | | `GO` / `HOLD` / `STOP` | |

## Domain Reconciliation

Mark excluded domains `OUT OF PILOT`.

| Domain | Source period/sample | Source result | SynapseCore result | Time difference considered | Outcome | Owner/action |
| --- | --- | --- | --- | --- | --- | --- |
| Catalog/products | | | | | | |
| Inventory | | | | | | |
| Orders | | | | | | |
| Connector/import | | | | | | |

## Alerts

| Sanitized reference | Condition/evidence | Severity | Owner | Permitted response | Result/state |
| --- | --- | --- | --- | --- | --- |
| | | | | | |

## Recommendations

| Sanitized reference | Evidence reviewed | Interpretation | `ACCEPT` / `REJECT` / `DEFER` / `ESCALATE` | Governed next step | Result |
| --- | --- | --- | --- | --- | --- |
| | | | | | |

Confirm no recommendation automatically executed: `YES` / `NO`

## Integration And Replay Events

| Failure/replay ID | Connector/source | Warehouse | Cause/correction | Authorization | Eligibility/duplicate check | Replayed once? | Verified result | State |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | | | | |

Recovery lane: `CSV PROVEN LANE` / `OUTSIDE PROVEN LANE - ESCALATED` / `NO RECOVERY EVENT`

Confirm disabled-webhook replay was not claimed as proven: `YES` / `NO`

## Governance Events

| Scenario ID | Type/stage | Warehouse | Assigned actor/role | Decision/action | Separation checked | Live effect/history verified | State |
| --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | | | |

## Access And Configuration Changes

| Change ID | Type | Approved owner/reason | Before state | Supported action | After-state/backend verification | Session revocation checked | State |
| --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | | | |

Change types include user/operator disablement, password reset, role, warehouse scope, connector policy/configuration, mapping, operational policy, and scope. Material changes require the separate approved change process.

## Runtime And Realtime

| Check | Opening | Midday | Closing | Degradation/control/evidence |
| --- | --- | --- | --- | --- |
| Platform Runtime | | | | |
| Tenant Runtime | | | | |
| Realtime connection state | | | | |
| Snapshot/fallback freshness | | | | |
| Platform Activity | | | | |
| Tenant Activity | | | | |

Confirm no foreign tenant Activity/data observed: `YES` / `NO`

## Issues

Categories: `ACCESS`, `DATA`, `CONNECTOR`, `REPLAY`, `GOVERNANCE`, `REALTIME`, `PLATFORM`, `USER EXPERIENCE`, `SCOPE / AUTHORIZATION`.

| Issue ID | Time | Category | Severity | Observer | Tenant/workflow/warehouse | Evidence | Impact | Owner | Immediate action | State | Resolution/next step |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | | | | | | | |

Open [pilot-incident-log-template.md](pilot-incident-log-template.md) for a material incident and reference it here.

## GO / HOLD / STOP Decisions

| Time | Decision | Affected scope | Evidence/reason | Compensating control | Decision owner | Next review |
| --- | --- | --- | --- | --- | --- | --- |
| | | | | | | |

## Support And Customer Communication

| Time | Concern/message | Audience/contact | Owner | Next update | Evidence reference |
| --- | --- | --- | --- | --- | --- |
| | | | | | |

## End-Of-Day Close

| Closing check | Result | Unresolved item/action |
| --- | --- | --- |
| Alerts reviewed | | |
| Recommendations reviewed | | |
| Connector failures/imports reviewed | | |
| Failed inbound reviewed | | |
| Replays verified | | |
| Governance items reviewed | | |
| Access/scope changes verified | | |
| Runtime/realtime degradation reviewed | | |
| Source reconciliation completed | | |
| Support issues classified | | |
| Known limitations reviewed | | |
| Next-day carry-forward complete | | |

## Daily Closing State

Select exactly one:

- `DAILY OPERATIONS ACCEPTED`
- `DAILY OPERATIONS ACCEPTED WITH OPEN ACTIONS`
- `DAILY OPERATIONS HOLD`
- `DAILY OPERATIONS STOPPED`

Closing state:

Rationale:

Critical issues open:

High issues open:

Held workflows:

Source-system fallback state:

## Next-Day Carry-Forward

| Item ID | Severity/category | Affected scope | Last trustworthy state | Evidence | Current control | Owner | Required verification | Opening prerequisite |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | | | | |

## Phase 12 Handoff

Phase 12 required: `YES` / `NO`

Incident trigger/reference:

Affected tenant/workflow/warehouse:

Last known trustworthy state:

Recent access/configuration/deployment changes:

Replay/governance history references:

Runtime/Activity evidence references:

Current `HOLD` / `STOP` state:

Customer communication issued:

## Limitations Encountered

| Limitation | Encountered? | Impact/control | Owner | Review point |
| --- | --- | --- | --- | --- |
| Disabled-webhook replay not proven | | | | |
| Import-run warehouse attribution absent | | | | |
| Provider-managed restore not drilled | | | | |
| Live Render scale bounded | | | | |
| Single deployment/no HA claim | | | | |
| Unsupported identity/integration/governance capability requested | | | | |

## Sign-Off

| Authority | Name/role | Decision | Date/time |
| --- | --- | --- | --- |
| Daily Platform Operator | | | |
| Customer Operations Owner | | | |
| Pilot Owner | | | |

## Phase 11 Control-Model Verdict

Select exactly one for the prepared Phase 11 process:

- `COMPANY PILOT PHASE 11 ACCEPTED`
- `COMPANY PILOT PHASE 11 ACCEPTED WITH DOCUMENTED LIMITATION`
- `COMPANY PILOT PHASE 11 NOT ACCEPTED - DAILY OPERATING MODEL INCOMPLETE`
