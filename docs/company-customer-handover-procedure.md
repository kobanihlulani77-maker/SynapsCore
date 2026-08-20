# Company Customer Handover Procedure

This is the authoritative internal SynapseCore Phase 9 procedure for preparing and delivering customer access after a real company environment has passed Phase 8.

It does not authorize Company 1, send credentials, create accounts, execute Phase 8, or begin day-one pilot operations.

## Mandatory Prerequisite

Open the completed [company-pre-handover-verification-record.md](templates/company-pre-handover-verification-record.md) for the actual company and confirm its signed decision is exactly one of:

- `AUTHORIZED FOR CUSTOMER HANDOVER`
- `AUTHORIZED FOR CUSTOMER HANDOVER WITH ACCEPTED OPERATING CONDITIONS`

These results do not authorize handover:

- `CHECKLIST/PROCESS READY - COMPANY-SPECIFIC EXECUTION REQUIRED`
- `NOT AUTHORIZED - PRE-HANDOVER BLOCKERS REMAIN`

If the actual company has not passed Phase 8, stop:

`DO NOT HAND OVER`

## Inputs Allowed From Phase 8

Phase 9 may consume only the information needed for safe customer access:

- company name and verified tenant/workspace code
- approved frontend URL
- approved users and access status
- roles and warehouse scopes described in customer language
- first-login/password-change requirements
- customer-visible feature scope
- customer-visible operating conditions
- support contacts
- pilot dates
- escalation, pause, and stop path
- signed handover authorization reference

Do not copy internal negative-test details, infrastructure evidence, platform tokens, secrets, private payloads, backup artifacts, proof credentials, or unrelated architecture into customer material.

## Customer-Facing And Internal Boundary

| Customer-facing | SynapseCore internal |
| --- | --- |
| Approved frontend URL | Phase 8 authorization reference |
| Workspace code and username | Recipient identity-verification evidence |
| First-login/password-change guide | Credential generation and delivery status |
| Assigned role description | Secret-channel reference without secret value |
| Approved feature scope | Per-user verification status |
| Customer-relevant limitations | Handover owner and pilot owner |
| Source-of-truth expectation | Internal issues and blocker disposition |
| Support and escalation path | Delivery/acknowledgement timestamps |
| Pilot dates and contacts | Platform/internal operating conditions |

Normal users receive the frontend URL only. Backend URLs, actuator endpoints, Render, databases, Redis, platform APIs, platform tokens, proof tooling, and bootstrap controls remain internal unless a separately approved integration process needs a specific endpoint.

## Authentication Truth To Preserve

- accounts are pre-created and tenant-scoped
- sign-in uses company workspace code, username, and password
- passwords are BCrypt-hashed at rest
- initial/reset passwords and `passwordChangeRequired` are supported
- users can change their own password from Profile
- an authorized tenant admin can reset a user password
- accounts can be disabled
- there is no self-registration, invitation, automated forgot-password, temporary reset-link, MFA, SSO, or user-delete flow

Never imply an unsupported recovery path.

## Handover Status Model

Use exactly one status per company and per user:

| Status | Meaning |
| --- | --- |
| `PREPARED - AWAITING PHASE 8 AUTHORIZATION` | Materials may be drafted, but access cannot be sent. |
| `AUTHORIZED - READY TO SEND ACCESS` | Phase 8 authorized handover and pre-send checks pass. |
| `ACCESS DELIVERED - FIRST LOGIN PENDING` | Identity and secret were delivered through approved separate channels; login is not yet verified. |
| `FIRST LOGIN VERIFIED` | Correct tenant, role, password posture, and initial access were confirmed. |
| `HANDOVER COMPLETE` | Required users completed handover and acknowledgement is recorded. |
| `HANDOVER BLOCKED` | An access, identity, role, tenant, session, system, or evidence issue prevents completion. |

## Build The Customer Pack

Populate [company-customer-handover-pack.md](company-customer-handover-pack.md) from the authorized company record:

1. Replace every placeholder with approved information.
2. Remove features and role sections that are out of pilot or irrelevant.
3. Mark remaining features `IN PILOT`, `OUT OF PILOT`, or `INTERNAL ONLY` exactly as Phase 8 authorized.
4. Include only customer-relevant operating conditions.
5. Add real pilot dates and review checkpoints without inventing them.
6. Confirm all critical contacts are present.
7. Confirm the source-of-truth and stop/escalation language matches the pilot agreement.
8. Confirm no credentials, tokens, internal URLs, proof values, or private payloads appear.

The pack is a concise handover guide, not the full Day-One operating manual.

## Role And Feature Translation

Use the actual assigned roles:

| Role | Customer wording | Internal control note |
| --- | --- | --- |
| `TENANT_ADMIN` | Administers the approved company workspace and approved access/settings. | Not platform admin; high-impact changes remain change-controlled. |
| `INTEGRATION_ADMIN` | Supports the approved connector lane. | Technical permission does not automatically authorize connector/source/secret/policy changes. |
| `INTEGRATION_OPERATOR` | Investigates and replays approved failed inbound work. | Replay requires source, tenant, correction, eligibility, and duplicate checks. |
| `REVIEW_OWNER` | Reviews saved scenario plans. | Applies only to ScenarioRun governance. |
| `FINAL_APPROVER` | Gives final approval or rejection for escalated scenario plans. | Confirm separation-of-duty and intended request before action. |
| `ESCALATION_OWNER` | Acknowledges and coordinates escalated scenario conditions. | Not a generic workflow approver. |

An active operator without a high-impact role may receive approved operational visibility. Because there is no formal read-only role, record this as a controlled-pilot condition where relevant.

## Credential Preparation And Delivery

### Rules

- Generate or reset a unique temporary password through the supported tenant-admin procedure.
- Confirm `passwordChangeRequired` has the intended value.
- Never record the password in Git, Markdown, a shared PDF, support ticket, public chat, screenshot, issue, or handover record.
- Keep identity information and secret information separate where practical.
- Deliver the secret only through the approved secure channel to the verified recipient.
- SynapseCore support must never ask for the user's current password.

### Delivery statuses

Record only:

- `PENDING`
- `DELIVERED`
- `CONFIRMED`
- `RESET REQUIRED`
- `REVOKED`

No field in the handover record stores the secret itself.

## Recipient Identity Confirmation

Before secret delivery, verify:

- the recipient is on the Phase 2 approved user list
- the business email/contact matches the approved record
- access remains approved
- the tenant/workspace code matches Phase 8
- the account is enabled
- role and warehouse scope match Phase 8
- the recipient identity was confirmed directly through the approved channel

Do not release access to an unverified forwarded contact. If identity cannot be confirmed, set `HANDOVER BLOCKED`.

## Internal Pre-Send Checklist

Every item must pass:

- actual company Phase 8 result authorizes handover
- approved recipient identity confirmed
- account enabled
- tenant, role, operator link, and warehouse scope confirmed
- `passwordChangeRequired` posture confirmed
- temporary credential generated/reset through supported procedure
- identity and secret delivery channels prepared separately
- customer handover pack populated from company-specific evidence
- customer feature scope and out-of-scope areas inserted
- customer-relevant operating conditions inserted
- frontend URL and pilot dates inserted
- support and escalation contacts complete
- no secret or sensitive internal evidence in customer material
- first-login support window scheduled
- [company-customer-handover-record.md](templates/company-customer-handover-record.md) opened and controlled

Any failed prerequisite sets `HANDOVER BLOCKED`.

## Per-User Handover

For each approved user record:

- user and company
- tenant/workspace code
- assigned role summary and warehouse scope
- access approval
- account enabled
- credential prepared status
- recipient identity verified
- secret delivery status and secure-channel reference
- first-login confirmation
- password-change status
- support contact shared
- final user status

Never record the actual password.

## Access Message

Use [company-customer-access-message.md](templates/company-customer-access-message.md). Populate company, frontend URL, workspace code, username, role summary, support contact, and pilot-start reference.

The message must say that the initial secret is delivered separately. Do not add a password field or send the reusable template.

## First-Login Support

During the first-login window, confirm:

- login succeeds
- correct tenant/workspace opens
- correct user identity appears
- expected role and warehouse scope appear
- expected starting page opens, normally Dashboard
- password is changed when required
- no unexpected access or company data appears
- user receives the support path

This confirms access only. It does not begin Day-One operational training.

## Handover Failure Path

If first login fails, stop pilot start for that user and classify:

- `CREDENTIAL ISSUE`
- `ACCOUNT DISABLED`
- `WRONG TENANT`
- `ROLE ISSUE`
- `SESSION ISSUE`
- `SYSTEM OUTAGE`
- `OTHER`

Set the user/company handover status to `HANDOVER BLOCKED` where appropriate. Resolve through the supported access or deployment procedure, repeat the relevant Phase 8 check, and reverify first login before completion.

If the user sees another company's data, unexpected privileged access, a wrong tenant, or exposed secret material, treat it as an urgent Phase 8/incident blocker rather than an ordinary login problem.

## Customer Acknowledgement

Record operational acknowledgement that the customer received and understood:

- access instructions and separate secret delivery
- pilot scope and out-of-scope areas
- assigned role expectations
- source-of-truth model
- replay/high-impact action safety
- support and escalation path
- customer-relevant operating conditions

Do not turn this acknowledgement into invented legal language.

## Completion Gate

Set `HANDOVER COMPLETE` only when:

- Phase 8 authorization remains valid
- every required user is confirmed or explicitly deferred by the pilot owner
- required first logins pass
- required password changes are complete
- customer pack and support contacts were delivered
- acknowledgement is recorded
- no unresolved handover blocker remains

Phase 9 acceptance means the package and process are ready. It does not mean a real company received access.

## Phase 10 Handoff

After an actual handover completes, Phase 10 may receive:

- customer role
- approved feature scope and workflow
- support contacts
- first-login completion
- pilot dates/checkpoints
- customer-visible operating conditions
- source-of-truth expectation

Do not create the Day-One Pilot Guide in Phase 9.

## Parked Work

Platform Control Plane and Tenant Access Boundary hardening remains separately authorized future work. It includes platform-owner authority, platform navigation, Platform Runtime versus Tenant Runtime, platform activity versus tenant activity, global metadata boundaries, tenant role/page visibility, and fresh access-boundary proof.

Do not solve or claim that future architecture in this handover procedure.
