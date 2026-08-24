# Company 1 Pilot Change Record

Use this record for material changes during the active Company 1 pilot. Do not record passwords, password hashes, session cookies, bearer tokens, bootstrap tokens, platform-owner credentials, connector secrets, customer credentials, raw inbound payloads, or copied environment files.

## Change Control

- Change ID:
- Tenant/workspace:
- Date/time requested (timezone):
- Requested by:
- Customer approver, if applicable:
- Authorized by:
- Implemented by:
- Verified by:
- Change class:
- Risk: `LOW` / `MEDIUM` / `HIGH` / `CRITICAL`
- State: `CHANGE PLANNED` / `AUTHORIZED` / `IN PROGRESS` / `DEPLOYED` / `VERIFYING` / `ACCEPTED` / `ACCEPTED WITH ACTIONS` / `ROLLED BACK` / `FAILED` / `HOLD`

## Request

- What is changing?
- Why is it needed?
- Affected warehouse(s):
- Affected workflow/capability:
- Affected connector/scenario/release, if applicable:
- Current approved pilot scope:
- Does this request expand pilot scope or a documented limitation? `YES` / `NO`
- Desired state:
- Potential impact:
- Dependencies:
- Collision risk with another change or incident:

## Before-State Evidence

- Current user/operator/role/scope state:
- Current connector/configuration/policy state:
- Current data/mapping/source value:
- Current governance assignment/workflow state:
- Current inventory or operational state:
- Current Runtime state:
- Current Activity evidence:
- Current release/commit/deployment state:
- Current pilot scope evidence:
- Evidence references:

## Authorization And Window

- Change window and reason:
- Affected workflow placed on `HOLD`? `YES` / `NO` / `NOT REQUIRED`
- Requester:
- Authorizer:
- Implementer:
- Verifier:
- Why separation was or was not practical:
- Required customer communication:
- Rollback owner:

## Implementation

- Implementation steps:
- Dependency/order constraints:
- Actual start time:
- Actual completion time:
- Implementation result:
- Any deviation from plan:
- Any incident created during implementation? `YES` / `NO`
- Incident reference, if applicable:

## Verification Plan And Evidence

- Backend/persisted-state check:
- UI readback check:
- Allowed operation check:
- Denied operation check:
- Tenant/warehouse scope check:
- Connector/import/replay check:
- Governance check:
- Runtime/readiness/auth/websocket check:
- Activity/audit/release evidence:
- Source-system reconciliation:
- Duplicate or unintended-side-effect check:
- Focused proof required? `YES` / `NO`
- Hosted proof required? `YES` / `NO`
- Verification evidence references:

## Rollback Plan And Result

- Known-good prior state/revision:
- Rollback steps:
- Is rollback compatible with current data/schema? `YES` / `NO` / `UNKNOWN`
- Business effects already created:
- Reconciliation required after rollback:
- Rollback required? `YES` / `NO`
- Rollback owner:
- Rollback result:
- Post-rollback verification:

## After State

- Actual resulting state:
- Expected state achieved? `YES` / `NO` / `PARTIAL`
- Authority and scope unchanged as intended? `YES` / `NO`
- Source-system agreement:
- Residual risk:
- Known limitation:
- Monitoring owner:
- Monitoring checkpoint:
- Reopen/incident trigger:

## Decision And Communication

- Final decision: `CHANGE ACCEPTED` / `CHANGE ACCEPTED WITH ACTIONS` / `CHANGE ROLLED BACK` / `CHANGE FAILED - HOLD` / `CHANGE ESCALATED TO INCIDENT`
- GO/HOLD/STOP decision:
- Affected scope may resume? `YES` / `NO` / `PARTIAL`
- Customer communication sent:
- Communication reference:
- Follow-up issue/reference:
- Owner and due checkpoint:

## Sign-Off

- Pilot Owner:
- Tenant/Customer Owner:
- Platform/Engineering Owner:
- Date/time:

## Phase 14 Handoff Fields

- Include in final change history? `YES` / `NO`
- Scope change reference:
- Access/role reference:
- Connector reference:
- Deployment/release reference:
- Rollback reference:
- Incident-linked reference:
- Limitation added/removed/reaffirmed:
- Final operating scope impact:
