# SynapseCore Customer Handover Pack

This reusable pack is completed for a specific company only after its environment has passed the SynapseCore pre-handover verification gate. Blank fields mean the handover is not ready.

This pack contains access and pilot operating guidance. Your initial secret is delivered separately through the approved secure channel and never appears in this document.

## Welcome And Purpose

Welcome to your controlled SynapseCore pilot.

SynapseCore is the operations coordination layer for the approved pilot lane. It helps your team see operational state, investigate attention items, coordinate recovery, and use governed decisions from one company workspace.

Your existing approved ERP, WMS, ecommerce platform, or other business system remains the source of truth unless the pilot agreement explicitly says otherwise. The pilot does not require your company to abandon those systems.

## Pilot Details

| Field | Approved value |
| --- | --- |
| Company | `[COMPANY]` |
| Company workspace code | `[WORKSPACE CODE]` |
| SynapseCore frontend URL | `[APPROVED FRONTEND URL]` |
| Approved operators | `[COUNT]` |
| Approved connector lane | `[CONNECTOR DESCRIPTION]` |
| Approved data scope | `[BOUNDED DATA SCOPE]` |
| Handover date | `[DATE]` |
| Pilot start date | `[DATE]` |
| Target pilot end date | `[DATE]` |
| Review checkpoints | `[DATES OR CADENCE]` |

Use only the approved frontend URL for normal SynapseCore work. Do not use backend addresses, health endpoints, internal APIs, infrastructure dashboards, databases, Redis, bootstrap controls, or platform tokens.

## How To Access SynapseCore

You receive these identity details through the approved handover process:

| Item | Value |
| --- | --- |
| Company workspace code | `[WORKSPACE CODE]` |
| Assigned username | `[USERNAME]` |
| Assigned role summary | `[ROLE SUMMARY]` |

Your initial secret is supplied separately. Do not write it into this pack, a support ticket, a screenshot, or a shared team document.

There is no customer self-registration or invitation flow in the current pilot. Every account is prepared and approved before handover.

## First Login Quick Guide

1. Open the approved SynapseCore frontend URL.
2. On Company sign in, enter the supplied `Company workspace code`.
3. Enter your assigned `Username`.
4. Enter the initial secret supplied through the separate secure channel.
5. Select `Enter Platform`.
6. Confirm that SynapseCore opens the expected company workspace and normally starts at Dashboard.
7. Open Profile and confirm your displayed name, workspace, assigned roles, and warehouse scope.
8. If Profile shows `Password change required`, complete the password change before operational use.
9. Contact support immediately if the company workspace, identity, role, or data is not what you expect.

Do not share an account. Each approved operator must use their own assigned identity.

## Change Your Password

When a password change is required:

1. Open Profile.
2. Find `Change password`.
3. Enter `Current Password`.
4. Enter a unique `New Password`.
5. Enter the same value in `Confirm Password`.
6. Select `Update Password`.
7. Confirm the page reports that the password was updated.

Use a strong password that is unique to SynapseCore. Do not reuse another company or personal password. Do not share it with coworkers. SynapseCore support will never ask you to reveal your current password.

SynapseCore does not currently provide a `Forgot Password` button, email reset link, MFA, or SSO. If access is lost, contact the approved SynapseCore support channel or authorized tenant administrator. Avoid repeated rapid sign-in attempts because authentication rate limiting may temporarily reject further attempts.

## Your Role

Your assigned role is based on your pilot responsibility, not seniority. A role may be technically capable of an action that remains change-controlled during the pilot.

| Role | Purpose | Expected work | Do not do without approval | High-impact responsibility |
| --- | --- | --- | --- | --- |
| `TENANT_ADMIN` | Administer the approved company workspace. | Manage approved tenant users, workspace settings, security posture, warehouse metadata, and catalog administration where authorized. | Do not create other companies, access global tenants, change platform infrastructure, or make unapproved workspace/security changes. | Access, security, and workspace changes can affect every pilot operator. |
| `INTEGRATION_ADMIN` | Administer the approved connector lane. | Review connector state and perform specifically authorized connector configuration or support actions. | Do not rotate secrets, change source/mode/policy, or recreate a connector without an approved pilot change. | Connector changes can stop, redirect, or alter inbound activity. |
| `INTEGRATION_OPERATOR` | Investigate approved integration failures and recovery. | Inspect failed inbound evidence and perform replay only when the company replay procedure authorizes it. | Do not replay uncertain, duplicate-risk, wrong-tenant, or uncorrected failures. | Replay can introduce work into the live order flow. |
| `REVIEW_OWNER` | Review saved scenario plans. | Review evidence and approve or reject the review stage when assigned. | Do not approve without confirming the intended company, warehouse, and request. | Review decisions move governed scenarios toward execution. |
| `FINAL_APPROVER` | Give final approval for escalated scenario plans. | Confirm prior review, evidence, scope, and risk before final approval or rejection. | Do not approve casually or outside the assigned governance lane. | Final approval can make a scenario eligible for live execution. |
| `ESCALATION_OWNER` | Own acknowledgement of escalated scenario conditions. | Inspect and acknowledge assigned escalations and coordinate the next approved response. | Do not treat acknowledgement as generic company-wide workflow approval. | Escalation acknowledgement is part of the ScenarioRun governance trail. |

An active workspace operator may have no high-impact role and still receive approved operational visibility. The current pilot does not have a formal read-only role. Roles and warehouse scope determine why one operator may see or do something another cannot.

`TENANT_ADMIN` is a company-workspace role. It is not SynapseCore platform administration and does not authorize access to other companies, global platform controls, Render, infrastructure secrets, databases, or another tenant.

## Your Pilot Features

The final handover copy must match the company-specific Phase 8 authorization. Remove or mark every feature that is not approved.

| Feature | Pilot status | Available to role(s) | Purpose | Customer action | Restriction |
| --- | --- | --- | --- | --- | --- |
| Dashboard | `[IN PILOT / OUT OF PILOT]` | `[ROLES]` | Current operating picture and attention state. | Review counts, attention items, and live/degraded state. | Treat degraded status honestly; confirm uncertain data before high-impact action. |
| Catalog | `[STATUS]` | `[ROLES]` | Approved product readback and lookup. | Find representative products and report mismatches. | Mutation is tenant-admin/change-controlled during the pilot. |
| Inventory | `[STATUS]` | `[ROLES/WAREHOUSES]` | Quantity, warehouse, threshold, and risk visibility. | Inspect approved warehouse inventory. | Report mismatches; do not create arbitrary corrections. |
| Orders | `[STATUS]` | `[ROLES/WAREHOUSES]` | Approved inbound order visibility. | Inspect external ID, status, items, and warehouse. | Do not create duplicate corrections for an existing external ID. |
| Alerts | `[STATUS]` | `[ROLES]` | System-generated operational attention. | Inspect the evidence and act or escalate under the pilot procedure. | Not a customer-defined rule engine; no promised email/SMS recipients. |
| Recommendations | `[STATUS]` | `[ROLES]` | Evidence-backed decision support. | Review evidence and choose a governed human response. | Recommendations are not autonomous actions and do not execute themselves. |
| Integrations | `[STATUS]` | `[APPROVED INTEGRATION ROLES]` | Visibility into the approved connector lane. | Monitor state and follow the support procedure. | Only the approved connector and authorized changes are in scope. |
| Replay | `[STATUS]` | `[APPROVED INTEGRATION ROLES]` | Controlled recovery of supported failed inbound activity. | Inspect, verify, replay only when authorized, and confirm the result. | When in doubt, do not replay. |
| Approvals | `[STATUS]` | `[GOVERNANCE ROLES]` | Governance for saved ScenarioRun plans. | Review, approve, reject, or acknowledge only when assigned. | Not a generic company-wide approval engine. |
| Scenarios | `[STATUS]` | `[APPROVED ROLES]` | Preview, compare, save, govern, and optionally execute approved operational plans. | Use only approved deterministic data and governance. | Execution creates a live order and is high impact. |
| Runtime | `[STATUS]` | `[APPROVED ROLES]` | Read-only evidence about current operational trust. | Use it to understand healthy, degraded, or waiting state. | It does not configure infrastructure. |
| Settings | `[STATUS]` | `[APPROVED TENANT ADMINS]` | Approved company workspace controls. | Make only explicitly authorized pilot changes. | High-impact changes follow the pilot change process. |
| Administration | `[INTERNAL ONLY / LIMITED TENANT ADMIN]` | `[ROLES]` | Approved tenant access administration only. | Follow the documented access process. | Platform administration and other tenants are never customer capability. |

## What Is Out Of Scope

| Area | Customer-specific decision |
| --- | --- |
| Features excluded from the pilot | `[LIST]` |
| Data domains excluded from the pilot | `[LIST]` |
| Warehouses excluded from the pilot | `[LIST]` |
| Connectors excluded from the pilot | `[LIST]` |
| Scenario execution | `[IN / OUT]` |
| Customer-managed configuration | `[APPROVED ITEMS ONLY]` |

Do not treat a visible page or technical permission as approval to use an out-of-scope capability.

## Data Boundary And Source Of Truth

You should see only data belonging to your approved company workspace and assigned operational scope.

Report immediately if you see:

- another company's data
- an unexpected warehouse or connector
- an unknown user
- an unknown order, product, or replay item
- information that appears outside your approved company boundary

Your approved business system remains the source of truth. SynapseCore supports visibility, coordination, recovery, and governed decision-making during the pilot. If SynapseCore and the source system disagree, record the identifier and report the mismatch. Do not create duplicate records or unsupported corrections to make the screens match.

## Using The Pilot Safely

### Dashboard

Use Dashboard for the current operating picture, attention items, alerts, recommendations, and live/degraded indicators. It is a coordination view, not a replacement for your business source system.

### Catalog

Use Catalog to find and confirm approved products. Follow the pilot authority matrix for changes; onboarding and broad corrections may remain SynapseCore-operated.

### Inventory

Use Inventory to inspect quantities, warehouse associations, thresholds, and surfaced risk. Report a mismatch with the SKU, warehouse, observed value, expected value, and time.

### Orders

Use Orders to inspect approved inbound activity, external identifiers, status, items, and warehouse. Report incorrect or missing orders instead of creating another order with a different identifier.

### Alerts

Alerts are generated from supported operational conditions. Follow:

```text
ALERT -> INSPECT EVIDENCE -> ACT OR ESCALATE UNDER THE PILOT PROCEDURE
```

### Recommendations

Recommendations provide evidence-backed decision support:

```text
RECOMMENDATION -> EVIDENCE -> HUMAN REVIEW -> GOVERNED DECISION OR ACTION
```

Do not treat a recommendation as an autonomous instruction.

### Integrations

The current product supports `WEBHOOK_ORDER` and `CSV_ORDER_IMPORT` connector types. Your pilot includes only the connector lane stated in Pilot Details. Do not assume arbitrary systems or formats are supported.

### Replay

Failed inbound activity may appear in Replay when supported. An authorized operator must:

1. Inspect the failure and supporting evidence.
2. Confirm the source and company workspace.
3. Check duplicate risk and whether the intended order already exists.
4. Confirm the original cause has been corrected.
5. Replay only if the assigned role and company procedure authorize it.
6. Verify the resulting order or operational state.
7. Escalate if the result is unclear.

**When in doubt, do not replay.**

### Scenarios And Approvals

Include this section only when scenarios are in the approved pilot:

```text
PREVIEW -> COMPARE -> SAVE -> REVIEW/APPROVAL -> EXECUTE WHEN AUTHORIZED
```

Preview and comparison support planning. Saved plans enter the configured governance path. Scenario execution creates a live order in the current product, so do not experiment against live data or execute without approval.

### Runtime

Runtime is read-only operational evidence. It helps approved operators understand whether the service is healthy, degraded, waiting, or reconnecting. It does not provide customer control over hosting, databases, Redis, deployment, or platform infrastructure.

### Settings

Use Settings only for actions your role and the approved pilot change process allow. If a change affects access, security, warehouses, connector policy, or operational thresholds, request approval before changing it.

## What You Must Not Touch

Unless a specific approved procedure says otherwise, do not:

- access or alter platform infrastructure, Render, backend environment variables, databases, Redis, or secrets
- change tenant/workspace codes
- use platform bootstrap or global tenant controls
- access another company workspace
- change connector source, mode, policy, URL, mapping, or secret
- recreate a connector to resolve a support problem
- perform unsupported imports
- change another user's role or warehouse scope outside the approved access process
- execute a live scenario without required governance
- replay when the source, tenant, correction, or duplicate risk is uncertain

## Support Process

Use this process whenever something looks wrong:

```text
ISSUE
-> CHECK THE BASIC STATE
-> RECORD WHAT YOU SAW
-> CONTACT THE APPROVED SUPPORT CHANNEL
-> SHARE SAFE NON-SECRET CONTEXT
-> WAIT FOR GUIDANCE BEFORE HIGH-IMPACT ACTION
```

Provide:

- your name and company
- affected SynapseCore area
- date and time observed
- a safe order/SKU/connector/replay identifier when appropriate
- what you expected and what happened
- a screenshot only when it contains no password, token, customer-private payload, or prohibited information

Never send a password, connector token, session information, platform token, database credential, or unredacted sensitive payload.

## Issue Categories And Immediate Action

| Category | First action | Escalate when |
| --- | --- | --- |
| Access | Confirm the approved URL, workspace code, and assigned username; retry carefully. | Login still fails, tenant is wrong, account is disabled, or role is wrong. |
| Data mismatch | Record the safe identifier, expected value, observed value, and source-system reference. | Any material Catalog, Inventory, or Order mismatch exists. |
| Connector | Record status, time, and safe connector/source reference. | State is degraded, inbound activity is missing, or a change appears necessary. |
| Failed inbound / Replay | Inspect evidence and duplicate risk; do not replay if uncertain. | Cause is unclear, correction is unconfirmed, or replay result is unexpected. |
| Alert / Recommendation | Inspect the supporting operational evidence. | Evidence appears wrong, another tenant appears, or the action is high impact. |
| Approval / Scenario | Stop before approval or execution and record the scenario reference. | Actor, warehouse, request, or governance state is unexpected. |
| Realtime | Check whether the interface says reconnecting/degraded and avoid assuming data is live. | State stays degraded or conflicts with the source system. |
| System availability | Continue through the approved source-system process and notify support. | SynapseCore is unavailable or repeatedly inconsistent. |
| Other | Record safe context and contact support. | The issue affects trust or operational safety. |

### If login fails

- Confirm you are using the approved frontend URL.
- Confirm the exact company workspace code and assigned username.
- Re-enter the secret carefully.
- Avoid rapid repeated attempts because rate limiting may apply.
- Contact the approved reset authority; there is no automated forgot-password flow.

### If data looks wrong

```text
RECORD IDENTIFIER -> REPORT MISMATCH -> SOURCE/SYNAPSCORE OWNER RECONCILES
-> CORRECT THROUGH A SUPPORTED PATH
```

Do not create duplicate products, inventory entries, or orders as a shortcut.

### If the connector is degraded

Do not recreate the connector or alter its secret, source URL, policy, mapping, or mode unless the approved support owner authorizes the change.

### If replay safety is uncertain

Do not replay. Contact the approved `INTEGRATION_ADMIN` or SynapseCore support path.

### If SynapseCore is unavailable or degraded

Your source systems remain the business system of record. Do not continue a high-impact SynapseCore action when state is uncertain. Follow the existing source-system procedure, record what SynapseCore displayed, and notify support. The pilot does not promise zero downtime.

## Stop And Report Immediately

Stop the affected SynapseCore activity and report immediately if:

- another company's information appears
- data appears corrupted
- replay creates unexpected duplicate state
- unauthorized access appears possible
- an approval or execution affects the wrong object
- a password, token, or private credential is exposed
- SynapseCore repeatedly reports conflicting operational state

Do not investigate by attempting further high-impact changes.

## Customer-Relevant Operating Conditions

Populate only conditions relevant to the approved company pilot.

| Condition | What it means for this pilot | Customer action |
| --- | --- | --- |
| No MFA or SSO | Sign-in currently uses workspace code, username, and password. | Protect the account and report suspected exposure immediately. |
| No invitation or automated forgot-password flow | Accounts and resets are managed by approved administrators. | Contact the approved support/reset authority. |
| Connector limitations | Only the approved `WEBHOOK_ORDER` or `CSV_ORDER_IMPORT` lane is supported. | Do not assume another system or format is connected. |
| Webhook authentication condition | `[INCLUDE ONLY IF APPLICABLE]` | Follow the approved connector-security procedure. |
| Role or warehouse-scope limitation | `[COMPANY-SPECIFIC CONDITION]` | Use only the assigned operational lane. |
| Procedural separation of duty | `[INCLUDE IF APPLICABLE]` | Follow the named requester/reviewer/approver assignments. |
| Scenario scope | `[IN / OUT OF PILOT]` | Do not execute when out of scope or unapproved. |
| Recovery/availability expectation | Existing source systems remain available as the fallback. | Stop high-impact work when SynapseCore state is uncertain. |

Conditions classified as internal-only or post-pilot must not be copied here unless they affect customer behavior or informed consent.

## Contacts

| Responsibility | Contact |
| --- | --- |
| SynapseCore primary support | `[NAME / APPROVED CHANNEL]` |
| SynapseCore technical escalation | `[NAME / APPROVED CHANNEL]` |
| Company business owner | `[NAME / CONTACT]` |
| Company technical contact | `[NAME / CONTACT]` |
| Company pilot owner | `[NAME / CONTACT]` |

No critical contact may remain blank when actual access is delivered.

## Customer Acknowledgement

This acknowledgement confirms operational receipt, not a new legal agreement.

| Item received and understood | Acknowledged |
| --- | --- |
| Access instructions and separate secure-secret process |  |
| Company workspace and assigned username |  |
| Role expectations and restrictions |  |
| Pilot feature scope and out-of-scope areas |  |
| Source-of-truth expectation |  |
| Replay and high-impact action safety |  |
| Support and escalation process |  |
| Customer-relevant operating conditions |  |

| Field | Value |
| --- | --- |
| Customer representative |  |
| Role |  |
| Acknowledgement date |  |
| Reference |  |
