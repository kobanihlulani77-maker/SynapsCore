# Company Integration Provisioning Record

Use this template for Company 1 Pilot Phase 5 evidence. Do not store secrets, passwords, raw connector tokens, private customer credentials, or unredacted sensitive payloads in this file.

## Record Metadata

| Field | Value |
| --- | --- |
| Company | Company 1 |
| Tenant code |  |
| Tenant name |  |
| Prepared by |  |
| Reviewed by |  |
| Date prepared |  |
| Phase | Phase 5 - Integration Setup |
| Previous phase evidence | Phase 3 tenant/workspace and Phase 4 user provisioning accepted |

## Approved Integration Scope

| Field | Value |
| --- | --- |
| First connector lane name |  |
| Source system |  |
| Source system owner |  |
| Business process covered |  |
| Connector type | `WEBHOOK_ORDER` / `CSV_ORDER_IMPORT` |
| Sync mode | `REALTIME_PUSH` / `BATCH_FILE_DROP` / `SCHEDULED_PULL` |
| Pilot data type | Synthetic / approved sample |
| Final business data loaded? | No |
| Out of scope for this phase | Final catalog, inventory, order history, alerts, recommendations, scenarios, handover |

## Connector Configuration

| Field | Value |
| --- | --- |
| Connector id |  |
| Display name |  |
| Enabled at creation |  |
| Enabled for sample test |  |
| Validation policy |  |
| Transformation policy |  |
| Mapping version | 1 |
| Default warehouse fallback |  |
| Default warehouse code |  |
| Support owner actor name |  |
| Support owner display name |  |
| Notes reviewed for secrets |  |
| Pull endpoint reviewed for secrets |  |
| Inbound access configured |  |
| Inbound token hint only |  |
| Raw token stored in Git? | No |

## Secret Handling Confirmation

| Check | Result | Evidence |
| --- | --- | --- |
| Token generated outside Git |  |  |
| Token stored only in approved secret location |  |  |
| Token not printed in screenshots/log evidence |  |  |
| API response returned hint only |  |  |
| Token rotation tested |  |  |
| Old token rejected after rotation |  |  |
| Clear-token behavior understood |  |  |
| No credentials embedded in pull URL |  |  |

## Webhook Test Evidence

Complete only if the connector type is `WEBHOOK_ORDER`.

| Check | Result | Evidence |
| --- | --- | --- |
| Valid synthetic JSON submitted |  |  |
| HTTP result |  |  |
| Order created in Company 1 tenant |  |  |
| Import run recorded |  |  |
| Dashboard/realtime update observed |  |  |
| Invalid token rejected |  |  |
| Disabled connector rejected |  |  |
| Missing field failure captured |  |  |
| Missing product/warehouse failure captured |  |  |
| Duplicate external order behavior verified |  |  |

## CSV Import Test Evidence

Complete only if the connector type is `CSV_ORDER_IMPORT`.

| Check | Result | Evidence |
| --- | --- | --- |
| Required columns present |  |  |
| Synthetic CSV submitted |  |  |
| `sourceSystem` request parameter provided for token-authenticated import |  |  |
| HTTP result |  |  |
| Orders imported |  |  |
| Rows received |  |  |
| Orders failed |  |  |
| Import status | `SUCCESS` / `PARTIAL_SUCCESS` / `FAILURE` |
| Missing header rejected |  |  |
| Empty CSV rejected |  |  |
| Invalid source-system behavior verified |  |  |

## Scheduled Pull Evidence

Complete only if the connector uses `SCHEDULED_PULL`.

| Check | Result | Evidence |
| --- | --- | --- |
| Connector type is `WEBHOOK_ORDER` |  |  |
| Pull endpoint is absolute HTTP(S) URL |  |  |
| No credentials embedded in URL |  |  |
| External endpoint accepts unauthenticated GET or header-only pilot arrangement |  |  |
| Endpoint returns JSON order object, array, or `{ "orders": [...] }` |  |  |
| `syncIntervalMinutes` is between 15 and 1440 |  |  |
| `lastPullAttemptAt` updated |  |  |
| `lastPullStatus` observed |  |  |
| `lastPullMessage` reviewed |  |  |
| Import run recorded |  |  |

## Replay And Recovery Evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Failure created replay queue item |  |  |
| Replay record id |  |  |
| Failure code |  |  |
| Failure message redacted if needed |  |  |
| Normal operator blocked from replay |  |  |
| Integration operator/admin permitted to replay |  |  |
| Warehouse scope enforced |  |  |
| Replay blocked while connector disabled |  |  |
| Prerequisite fixed through supported path |  |  |
| Replay into live flow completed |  |  |
| Audit/history evidence captured |  |  |
| Realtime update observed |  |  |

## Tenant And Role Isolation Evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Connector response tenant code matches Company 1 |  |  |
| Another tenant cannot see Company 1 connector |  |  |
| Another tenant cannot ingest into Company 1 via session |  |  |
| Company 1 token does not resolve to another tenant |  |  |
| Non-admin cannot create/update connector |  |  |
| Non-replay operator cannot replay |  |  |
| Operator without warehouse scope cannot replay scoped record |  |  |

## Operational Visibility Evidence

| Surface | Observed | Evidence |
| --- | --- | --- |
| Integrations page connector visible |  |  |
| Connector health visible |  |  |
| Import run visible |  |  |
| Replay page visible |  |  |
| Dashboard snapshot updated |  |  |
| Runtime/realtime indicator acceptable |  |  |
| Alerts/recommendations not configured in this phase | Confirmed |  |

## Disablement And Emergency Stop

| Check | Result | Evidence |
| --- | --- | --- |
| Connector disabled through supported API/UI |  |  |
| New inbound traffic blocked while disabled |  |  |
| Existing replay behavior understood |  |  |
| Connector re-enabled only after approval |  |  |
| Emergency contact identified |  |  |

## Technical Contacts

| Role | Name | Contact | Notes |
| --- | --- | --- | --- |
| Business process owner |  |  |  |
| Source-system technical owner |  |  |  |
| SynapseCore integration support owner |  |  |  |
| Replay decision owner |  |  |  |
| Incident escalation contact |  |  |  |
| Secret holder |  |  |  |
| Test-data approver |  |  |  |
| Go/no-go approver |  |  |  |

## Limitations And Decisions

| Limitation or decision | Impact | Accepted by | Date |
| --- | --- | --- | --- |
| No connector delete/archive endpoint; disable instead |  |  |  |
| No arbitrary field-mapping UI; mapping version 1 only |  |  |  |
| No outbound scheduled-pull credential model |  |  |  |
| No HMAC webhook signatures today |  |  |  |
| Inbound payload retention treated as sensitive evidence |  |  |  |

## Phase 5 Verdict

Select one:

- `COMPANY PILOT PHASE 5 ACCEPTED`
- `COMPANY PILOT PHASE 5 ACCEPTED WITH DOCUMENTED LIMITATION`
- `COMPANY PILOT PHASE 5 NOT ACCEPTED - SAFE INTEGRATION SETUP INCOMPLETE`

Verdict:

Reason:

Approved for Phase 6 company data onboarding:

Approver:

Date:
