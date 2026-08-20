# Company Customer Handover Record

This is an internal no-secret Phase 9 record. Complete it only for a company whose actual Phase 8 result authorizes customer handover.

Never store passwords, tokens, cookies, connector secrets, platform secrets, database credentials, private payloads, or backup artifacts here.

## Record Control

| Field | Value |
| --- | --- |
| Company |  |
| Tenant/workspace code |  |
| Frontend URL |  |
| Phase 8 record reference |  |
| Phase 8 decision |  |
| Phase 8 sign-off date |  |
| Handover owner |  |
| Pilot owner |  |
| Handover date |  |
| Pilot start date |  |
| Target pilot end date |  |
| Review checkpoints |  |
| Overall status |  |

Allowed overall statuses:

- `PREPARED - AWAITING PHASE 8 AUTHORIZATION`
- `AUTHORIZED - READY TO SEND ACCESS`
- `ACCESS DELIVERED - FIRST LOGIN PENDING`
- `FIRST LOGIN VERIFIED`
- `HANDOVER COMPLETE`
- `HANDOVER BLOCKED`

## Phase 8 Authorization Gate

| Check | Value/evidence | Result |
| --- | --- | --- |
| Actual company Phase 8 record exists |  |  |
| Decision authorizes handover |  |  |
| Technical Verifier signed |  |  |
| Platform Owner signed |  |  |
| Pilot Owner signed |  |  |
| No Critical blocker open |  |  |
| No High blocker open |  |  |
| Accepted conditions have owners |  |  |

Allowed handover decisions:

- `AUTHORIZED FOR CUSTOMER HANDOVER`
- `AUTHORIZED FOR CUSTOMER HANDOVER WITH ACCEPTED OPERATING CONDITIONS`

If neither applies, record `HANDOVER BLOCKED`.

## Pilot Scope

| Area | Approved value |
| --- | --- |
| Operator count |  |
| Connector lane |  |
| Data domains/scope |  |
| Warehouses |  |
| Source system(s) |  |
| Source-of-truth statement |  |

## Customer Feature Scope

| Feature | IN PILOT / OUT OF PILOT / INTERNAL ONLY | Approved role(s) | Warehouse scope | Customer action | Restriction |
| --- | --- | --- | --- | --- | --- |
| Dashboard |  |  |  |  |  |
| Catalog |  |  |  |  |  |
| Inventory |  |  |  |  |  |
| Orders |  |  |  |  |  |
| Alerts |  |  |  |  |  |
| Recommendations |  |  |  |  |  |
| Integrations |  |  |  |  |  |
| Replay |  |  |  |  |  |
| Approvals |  |  |  |  |  |
| Scenarios |  |  |  |  |  |
| Runtime |  |  |  |  |  |
| Settings |  |  |  |  |  |
| Tenant administration |  |  |  |  |  |
| Platform administration | INTERNAL ONLY | None | N/A | None | Never customer capability |

## Customer-Relevant Operating Conditions

| Condition | Customer must know? | Customer wording/reference | Owner | Accepted? |
| --- | --- | --- | --- | --- |
| No MFA/SSO |  |  |  |  |
| No invitation/automated forgot-password |  |  |  |  |
| Connector authentication/format constraint |  |  |  |  |
| Role/warehouse-scope limitation |  |  |  |  |
| Procedural separation of duty |  |  |  |  |
| Scenario scope/execution impact |  |  |  |  |
| Recovery/availability expectation |  |  |  |  |
| Other |  |  |  |  |

## Support Contacts

| Responsibility | Name/role | Approved contact channel | Verified? |
| --- | --- | --- | --- |
| SynapseCore primary support |  |  |  |
| SynapseCore technical escalation |  |  |  |
| Company business owner |  |  |  |
| Company technical contact |  |  |  |
| Company pilot owner |  |  |  |
| Access/reset authority |  |  |  |

## Internal Pre-Send Checklist

| Check | Evidence/reference | Result | Owner |
| --- | --- | --- | --- |
| Phase 8 authorization confirmed |  |  |  |
| Customer pack populated |  |  |  |
| Feature scope inserted |  |  |  |
| Out-of-scope areas inserted |  |  |  |
| Customer-relevant conditions inserted |  |  |  |
| Frontend URL verified |  |  |  |
| Pilot dates inserted |  |  |  |
| Support contacts complete |  |  |  |
| Identity and secret channels separated |  |  |  |
| Credential delivery channel ready |  |  |  |
| First-login support window ready |  |  |  |
| No secrets/customer-private evidence in pack |  |  |  |

## Per-User Handover

Do not record the secret.

| User | Role(s) | Tenant | Warehouse scope | Access approved | Account enabled | Password change required | Credential prepared | Identity verified | Secret delivery status | Secure-channel reference | First login | Password changed | Support shared | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  | PENDING / CONFIRMED |  | PENDING / DELIVERED / CONFIRMED / RESET REQUIRED / REVOKED |  |  |  |  |  |

## First-Login Confirmation

| User | Login success | Correct tenant | Correct identity | Expected role | Expected warehouse scope | Expected starting page | No unexpected access | Password changed if required | Evidence time | Result |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  | Dashboard / approved page |  |  |  |  |

## Handover Issues

| ID | User/area | Classification | Description | Owner | Resolution | Reverification | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  | CREDENTIAL ISSUE / ACCOUNT DISABLED / WRONG TENANT / ROLE ISSUE / SESSION ISSUE / SYSTEM OUTAGE / OTHER |  |  |  |  | OPEN / RESOLVED |

Any wrong-tenant access, unexpected privileged access, tenant leakage, or secret exposure must be escalated through the Phase 8/incident blocker process.

## Customer Acknowledgement

| Item | Delivered? | Understood? | Evidence/reference |
| --- | --- | --- | --- |
| Access and first-login instructions |  |  |  |
| Separate secure-secret process |  |  |  |
| Role expectations |  |  |  |
| Pilot feature scope/out-of-scope areas |  |  |  |
| Source-of-truth expectation |  |  |  |
| Replay/high-impact safety |  |  |  |
| Support/escalation path |  |  |  |
| Customer-relevant conditions |  |  |  |

| Field | Value |
| --- | --- |
| Customer representative |  |
| Acknowledgement date |  |
| Reference |  |

## Final Result

Mark exactly one:

| Result | Mark one |
| --- | --- |
| `PREPARED - AWAITING PHASE 8 AUTHORIZATION` |  |
| `AUTHORIZED - READY TO SEND ACCESS` |  |
| `ACCESS DELIVERED - FIRST LOGIN PENDING` |  |
| `FIRST LOGIN VERIFIED` |  |
| `HANDOVER COMPLETE` |  |
| `HANDOVER BLOCKED` |  |

Decision notes:

```text

```

| Sign-off | Name | Date/time | Reference |
| --- | --- | --- | --- |
| Handover owner |  |  |  |
| Platform owner |  |  |  |
| Pilot owner |  |  |  |

## Phase 10 Handoff

Complete only after actual handover reaches the required completion state.

| Input | Verified value/reference |
| --- | --- |
| Customer role(s) |  |
| Approved feature scope |  |
| Approved workflow |  |
| Support contacts |  |
| First-login completion |  |
| Pilot dates/checkpoints |  |
| Customer-visible conditions |  |
| Source-of-truth expectation |  |
