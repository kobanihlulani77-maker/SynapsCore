# Company 1 Incident Recovery Record

Use one record for every material incident and near miss during the Company 1 pilot. Do not place secrets or raw sensitive payloads in this document.

## Record Control

- Incident ID/reference:
- Company/workspace:
- Date/time detected (timezone):
- Date/time resolved or current status:
- Reporter/observer:
- Incident owner:
- Pilot Owner:
- Severity: `LOW` / `MEDIUM` / `HIGH` / `CRITICAL`
- Operating decision: `GO` / `HOLD` / `STOP`
- Status: `OPEN` / `CONTAINED` / `MITIGATING` / `VERIFYING` / `MONITORING` / `CLOSED` / `REOPENED`
- Phase/checkpoint:

## Scope And Classification

- Tenant/workspace:
- Warehouse(s):
- Workflow/capability:
- User role or identity class, without credentials:
- Connector/import/replay reference, if applicable:
- Scenario/approval/execution reference, if applicable:
- Category: `ACCESS` / `TENANT` / `WAREHOUSE` / `DATA` / `CONNECTOR` / `REPLAY` / `GOVERNANCE` / `REALTIME` / `RUNTIME` / `DATABASE` / `BACKUP` / `DEPLOYMENT` / `SECURITY` / `SUPPORT` / `OTHER`
- Customer/source-system impact:

## What Happened

- Symptom, in factual terms:
- User-visible result:
- Immediate impact:
- Affected capability or records:
- First observed time:
- Last known good time and evidence reference:
- Recent release/configuration/role/data changes:
- Was this a near miss? Why?

## Trust Assessment

- Authority trusted: `YES` / `NO` / `UNKNOWN`; evidence:
- Tenant isolation trusted: `YES` / `NO` / `UNKNOWN`; evidence:
- Warehouse scope trusted: `YES` / `NO` / `UNKNOWN`; evidence:
- Data trusted: `YES` / `NO` / `UNKNOWN`; evidence:
- Runtime/readiness trusted: `YES` / `NO` / `UNKNOWN`; evidence:
- Integration/replay trusted: `YES` / `NO` / `UNKNOWN`; evidence:
- Realtime trusted: `YES` / `NO` / `UNKNOWN`; evidence:

## Evidence Preserved

- Browser route/page and visible state:
- Request IDs and HTTP statuses:
- Runtime/health/readiness/liveness results:
- Auth-session result:
- Websocket info/connection result:
- Tenant or Platform Activity reference:
- Runtime/incident reference:
- Connector/import/replay evidence reference:
- Scenario/approval/execution evidence reference:
- Release/deployment/commit reference:
- Screenshot or report path, with secrets removed:
- Source-system comparison/reference:

**Do not record:** passwords, password hashes, session cookies, bearer/bootstrap/platform-owner tokens, connector secrets, customer credentials, copied environment files, or unnecessary raw payloads.

## Immediate Response

- Initial classification and rationale:
- Actions stopped or held:
- Connector disabled or isolated? If yes, who and when:
- Source-system fallback activated? How:
- Users/operators notified:
- Platform Owner notified:
- Customer/Pilot Owner notified:
- Evidence preservation completed by:
- Any unsafe retry or replay prevented:

## Investigation

- Suspected immediate cause:
- Confirmed root cause:
- If unknown, investigation owner and next checkpoint:
- Affected systems: frontend / backend / PostgreSQL / Redis-session / websocket / connector / replay / deployment / source system / other:
- Did any unauthorized action occur?
- Did any wrong-tenant or wrong-warehouse data/action occur?
- Did any duplicate, missing, or wrong-object result occur?
- Did any secret or sensitive data exposure occur?

## Correction And Recovery

- Correction applied through supported path:
- Was a rollback required? Decision and approver:
- Was a restore required? Backup/restore reference and approver:
- Was replay required? Replay ID, eligibility checks, operator, and result:
- Duplicate check result:
- Data reconciliation result:
- Authority recheck result:
- Runtime/readiness/auth/websocket recheck result:
- Connector/replay/activity/audit recheck result:
- Focused proof or hosted proof required? Why:
- Proof result/reference:
- Monitoring window after recovery:

## Resume Decision

- Affected scope may resume? `YES` / `NO` / `PARTIAL`
- Resume conditions:
- Compensating controls:
- Unaffected work that may continue:
- Explicit approver:
- Approval time:
- Customer communication sent:
- Communication reference:

## Closure And Follow-Up

- Closure statement:
- Why the system is trusted again:
- Residual risk or accepted limitation:
- Follow-up issue/reference:
- Owner:
- Due checkpoint:
- Reopen condition:
- Customer sign-off, if required:
- Pilot Owner sign-off:
- Engineering/Incident Owner sign-off:
- Final closure time:

## Post-Incident Review

- What protected the pilot?
- What control was missing or unclear?
- What should be improved before the next operating checkpoint?
- Is a product change justified by evidence? `YES` / `NO`
- Is a documentation/runbook change justified? `YES` / `NO`
- Is the issue a Phase 13 or later evolution item? `YES` / `NO`
- Lessons shared with operators and support:
