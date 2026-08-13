# Company 1 Presentation Pack

This is the official internal guide for presenting SynapseCore to the first prospective controlled pilot company.

It is written for the SynapseCore platform owner or presenter. It is not a sales deck, a product roadmap, a feature wishlist, or a promise of broad enterprise readiness.

The purpose is simple:

**Help the presenter explain SynapseCore honestly, discover the company's real operational problem, demonstrate only real product capability, and decide whether the company should move to Phase 2: Pilot Intake.**

## Repository Truth Used

This guide is grounded in the current repository and release evidence:

- [final-pre-pilot-release-gate.md](final-pre-pilot-release-gate.md)
- [verification-status.md](verification-status.md)
- [official-pilot-program.md](official-pilot-program.md)
- [company-explainer.md](company-explainer.md)
- [system-architecture.md](system-architecture.md)
- [system-flow.md](system-flow.md)
- [performance-scale-proof.md](performance-scale-proof.md)
- [backup-restore-runbook.md](backup-restore-runbook.md)
- [security-and-trust-model.md](security-and-trust-model.md)
- [hosted-proof.md](hosted-proof.md)
- frontend route registry: `frontend/src/config/pageRegistry.js`
- frontend routed pages: `frontend/src/components/AppRoutes.jsx`
- hosted proof: `frontend/tests/prod-proof.spec.mjs`

Do not add claims in the meeting that are not supported by these sources.

## Current Accepted Position

Accepted release classification:

`READY FOR CONTROLLED COMPANY 1 PILOT - WITH DOCUMENTED OPERATING CONDITIONS`

Accepted evidence:

- final hosted proof: `6 / 6`
- Gate 4 controls: `201 / 201`
- backend tests: `133 / 133`
- frontend lint/build/verify: PASS
- replacement Render PostgreSQL database validated through readiness and hosted proof
- application-level backup/restore proof accepted with documented limitation
- controlled local performance/scale proof accepted with documented limitation

Known operating conditions:

- 1 workspace
- 3 to 5 operators
- 1 connector lane initially
- bounded real-company operational slice
- existing ERP, WMS, ecommerce, spreadsheet, or source system remains authoritative
- provider-level managed restore evidence still needs to be captured before reliance expands
- live Render saturation is not proven

## Official Product Positioning

Position SynapseCore as:

**An Intelligent Operations Platform.**

Use these concepts:

- operational visibility
- coordination
- recovery
- governed decision-support
- realtime operational state
- evidence-driven operation
- runtime trust
- failure honesty
- intelligence inside the workflow

Do not call SynapseCore:

- AI-powered
- a full ERP replacement
- a full WMS replacement
- an autonomous company operator
- an unrestricted automatic decision maker
- an enterprise-wide replacement system
- infinitely scalable
- proven enterprise HA

## Customer Setup Model

For Company 1, the customer does not self-provision SynapseCore.

The customer experience should be:

1. SynapseCore prepares the company environment.
2. SynapseCore creates tenant/workspace records.
3. SynapseCore creates approved users.
4. SynapseCore assigns roles.
5. SynapseCore configures the agreed connector or data path.
6. SynapseCore loads/maps pilot data.
7. SynapseCore verifies the workspace.
8. Customer receives approved access.
9. Customer signs in.
10. Customer uses the prepared workspace.

The customer should not be expected to:

- create the tenant
- create the workspace
- configure infrastructure
- understand internal IDs
- configure database settings
- create backend resources
- manage platform secrets
- build integrations themselves
- perform technical platform setup

Pre-provisioned does not mean source-hardcoded secrets.

Never hardcode:

- passwords
- API keys
- database credentials
- OAuth secrets
- private tokens
- session secrets
- customer private credentials

## First Meeting Objective

The first meeting is not "show every feature and sell everything."

The goal is to:

1. understand the company's operational problem
2. understand their current systems and workflow
3. identify one bounded operational pilot use case
4. demonstrate the relevant SynapseCore operational workflow
5. explain the controlled pilot model
6. determine whether the company is a valid fit
7. collect enough information to proceed to Phase 2

The presenter should listen before showing screens.

## Opening Positioning

### 30-Second Explanation

Use this when the audience needs the shortest possible framing:

> SynapseCore is an intelligent operations platform for teams that need better visibility, recovery, and governed decision-support across orders, inventory, integrations, replay, approvals, scenarios, alerts, and runtime health. During the pilot, it does not replace your ERP, WMS, ecommerce platform, or source system. It sits beside those systems as a controlled command-center layer for one operational lane.

### 60-Second Explanation

Use this when the audience wants the practical problem and fit:

> Many operations teams run work across disconnected tools: order systems, spreadsheets, inventory systems, warehouse processes, integrations, and manual follow-up. When something fails, the team often discovers it late, reconciles manually, and struggles to prove what happened. SynapseCore is designed to bring that operational state into one command-center surface. It helps operators see what needs attention, inspect orders and inventory, understand alerts and recommendations, recover failed inbound data through replay, route governed decisions through approvals, and verify runtime trust. The Company 1 pilot starts small so we can prove value safely without asking you to replace your existing systems.

### 2-3 Minute Explanation

Use this when technical and operational stakeholders are both present:

> SynapseCore exists because operational work often breaks at the seams between systems. An order may come from one system, inventory from another, approvals through people, exceptions through messages, and integration failures through logs that operators never see. A dashboard alone is not enough because it may show state without helping the team recover, govern action, or understand whether the system itself is trustworthy.
>
> SynapseCore is an intelligent operations platform. Its current supported scope connects public entry, authenticated workspace access, dashboard snapshots, orders, catalog, inventory, alerts, recommendations, integrations, failed inbound replay, approvals, scenarios, runtime health, and audit-style operational visibility. The pilot does not make SynapseCore the source of record. Your existing ERP, WMS, ecommerce platform, spreadsheet process, or source system remains authoritative.
>
> The first pilot is intentionally narrow: one workspace, 3 to 5 operators, one connector or controlled data lane, and a bounded data slice. That lets us measure real usefulness, keep rollback practical, verify tenant isolation, and avoid pretending we have proven unrestricted enterprise scale before the evidence exists.

## Discovery Before Demo

Ask these questions before showing the product. The answers determine which screens matter and whether the company should proceed to Phase 2.

### Business And Operations

- Which operational process is most painful right now?
- Where does coordination break down?
- Which decisions are delayed because teams cannot see the right information?
- What work is currently reconciled manually?
- Where do failures happen but get discovered late?
- Which failures are hard to recover from?
- Which decisions require approval or governance?
- What operational state would managers want visible every day?
- What problem would be valuable enough to pilot first?

### Systems

- Which ERP is involved, if any?
- Which WMS is involved, if any?
- Which order system is authoritative?
- Which inventory system is authoritative?
- Are CSV/file processes currently used?
- Are APIs available?
- Are webhooks available?
- Are scheduled exports or scheduled pulls available?
- Are spreadsheets part of the operational process?
- Who owns each source system?

### Users

- Who currently operates the workflow?
- Who approves high-impact decisions?
- Who manages source systems?
- Who would join the pilot as a daily operator?
- Who should be the company business owner?
- Who should be the company technical contact?
- Who should receive admin-like visibility?
- Which users should be read-only or limited?

### Data

- Which catalog/product records matter for the pilot?
- Which inventory records matter?
- Which orders matter?
- What identifiers must be preserved from existing systems?
- What integration payloads or files exist today?
- How often does the data change?
- What data is sensitive?
- What data should not enter SynapseCore during the pilot?
- What reconciliation is required before handover?

### Pilot Value

- What measurable improvement would matter?
- What would make the first two weeks successful?
- What would make the pilot pause?
- What one workflow should be tested first?
- What existing process remains the fallback?
- Who will evaluate the result?

## Meeting Structure

Suggested structure:

| Segment | Purpose |
| --- | --- |
| Discovery | Understand the company's real operational pain before showing screens. |
| Positioning | Explain SynapseCore as an intelligent operations platform beside existing systems. |
| Demo | Tell one coherent operational story using real routes. |
| Company workflow discussion | Map the demo back to their current process. |
| Pilot framing | Confirm one bounded lane, users, data, success criteria, and limits. |
| Next step | Move to Phase 2 intake only if fit is clear enough. |

Do not make timing rigid. A highly technical audience may spend longer on proof, security, and readiness. An operations audience may spend longer on dashboard, orders, inventory, replay, and approvals.

## Official Demo Story

Do not demo "28 pages."

Tell one operational story:

```text
Current operational state
-> something requires attention
-> inspect underlying orders/inventory
-> review alerts and recommendations
-> inspect integration condition
-> show failure visibility
-> show replay/recovery
-> show governed decision or scenario
-> show runtime/trust
-> return to unified command center
```

## Demo Sequence

Use this sequence for a balanced business and technical audience.

| Step | Route | Screen | Purpose |
| --- | --- | --- | --- |
| 1 | `/` or `/product` | Public experience | Position SynapseCore honestly before sign-in. |
| 2 | `/sign-in` | Sign In | Show that customer operators receive approved access to a prepared workspace. |
| 3 | `/dashboard` | Dashboard | Establish the command-center view. |
| 4 | `/orders` | Orders | Show the operating lane and order pressure. |
| 5 | `/inventory` | Inventory | Show stock posture and operational consequences. |
| 6 | `/alerts` | Alerts | Show attention and severity. |
| 7 | `/recommendations` | Recommendations | Show decision-support, not hidden automation. |
| 8 | `/integrations` | Integrations | Show connector visibility and inbound trust. |
| 9 | `/replay-queue` | Replay Queue | Show failed inbound recovery. |
| 10 | `/scenarios` | Scenarios | Show planning before action. |
| 11 | `/approvals` | Approvals | Show governed decision flow. |
| 12 | `/runtime` | Runtime | Show health, readiness, realtime, incidents, and trust posture. |
| 13 | `/dashboard` | Dashboard | Return to the unified operating view. |

## Screen-By-Screen Presenter Guidance

| Screen | Presenter Should Say | Presenter Should Show | Business Problem Demonstrated | Do Not Say | Transition |
| --- | --- | --- | --- | --- | --- |
| Public experience | "This is the external explanation, but the real value is inside the operating workspace." | Show the command-center positioning and controlled pilot language. | Sets honest expectations before login. | "This replaces your whole stack." | Move to sign-in. |
| Sign In | "Company 1 receives approved access to a prepared workspace." | Show workspace code, username, password flow without exposing secrets. | Customer does not self-provision infrastructure. | "Anyone can self-create production access." | Sign in to the demo/proof workspace. |
| Dashboard | "This is the live command center: what is happening, what needs attention, and what changed." | Show summary, attention areas, runtime/trust indicators, recent operational state. | Fragmented state across systems becomes one operational surface. | "This proves every future workflow." | Open Orders. |
| Orders | "Orders show the active operational lane we would choose for the pilot." | Show recent orders, delayed/linked lanes, attention queue. | Order pressure and fulfillment visibility. | "This replaces the order source system." | Open Inventory. |
| Inventory | "Inventory connects operational state to stock pressure and decision-support." | Show risk level, thresholds, depletion/stock posture, selected item detail. | Operators see why action may be needed. | "This is the authoritative stock ledger during pilot." | Open Alerts. |
| Alerts | "Alerts surface operational exceptions that deserve attention." | Show severity, impact, focus/attention controls. | Prevents important exceptions from being buried. | "Alerts will always be perfect/noiseless." | Open Recommendations. |
| Recommendations | "Recommendations support decisions; they do not blindly run the company." | Show urgent/important lanes and recommended action context. | Helps teams move from visibility to considered action. | "The system autonomously makes business decisions." | Open Integrations. |
| Integrations | "Integration trust is visible because many operational failures start at system boundaries." | Show connector health, telemetry, import/sync behavior, replay pressure. | Makes inbound health inspectable. | "Every connector is already supported." | Open Replay Queue. |
| Replay Queue | "When inbound work fails, operators need a visible, governed recovery path." | Show failed inbound item, failure reason, replay eligibility, Replay Into Live Flow where safe. | Truth over fake success; recovery is visible. | "Replay fixes every possible external failure." | Open Scenarios. |
| Scenarios | "Scenarios let the team model an action before it becomes live." | Show scenario form, compare/preview/save flow, review ownership where available. | Planning before execution. | "Scenario output is guaranteed optimal." | Open Approvals. |
| Approvals | "Governed decisions should have visible review and role boundaries." | Show pending/review/final approval posture and decision console where appropriate. | Human governance for high-impact actions. | "Approvals are optional for all risky changes." | Open Runtime. |
| Runtime | "Runtime is the trust surface. If the system is degraded, operators should know." | Show readiness, websocket/realtime posture, incidents, metrics, deployment/runtime evidence. | Prevents false confidence in unhealthy states. | "There is zero downtime or perfect connectivity." | Return to Dashboard. |
| Dashboard return | "The loop returns to one unified view after inspection, recovery, and governance." | Show the same command-center frame with refreshed understanding. | Operators can navigate the loop without tool-hopping. | "This proves unlimited scale." | Move to pilot framing. |

## Screens Deliberately Excluded From First Demo

These routes exist, but should usually be excluded from a first business demo unless the audience asks or the presenter is speaking with technical/admin stakeholders:

| Route | Screen | Reason To Exclude Initially |
| --- | --- | --- |
| `/catalog` | Catalog | Useful for onboarding, but not the strongest first narrative screen. |
| `/locations` | Locations | Use only if the pilot lane is site/warehouse focused. |
| `/fulfillment` | Fulfillment | Use only if fulfillment/logistics pressure is the main pain. |
| `/scenario-history` | Scenario History | Better after scenarios are understood. |
| `/escalations` | Escalations | Use if governance bottlenecks are central to the conversation. |
| `/audit-events` | Audit & Events | More useful for technical review or incident/recovery discussion. |
| `/users` | Users | Internal provisioning/admin topic, not first business value. |
| `/company-settings` | Company Settings | Internal/admin configuration. |
| `/profile` | Profile | Personal account hygiene, not first meeting value. |
| `/platform-admin` | Platform Admin | Platform-owner surface; do not center a customer business demo on it. |
| `/tenant-management` | Tenant Management | Internal provisioning surface; customer does not self-provision. |
| `/system-config` | System Config | Technical/admin surface. |
| `/releases` | Releases | Technical/reviewer surface. |

## Demo Fixture Requirements

Do not assume demo state exists. Prepare and verify it before the meeting.

| Demo State | Need | Current Classification |
| --- | --- | --- |
| Demo/proof workspace | Required for sign-in and app demo | CAN BE CREATED THROUGH EXISTING TOOLING |
| Demo user | Required for authenticated demo | CAN BE CREATED THROUGH EXISTING TOOLING |
| Role with operational access | Required for orders/inventory/replay/scenarios | CAN BE CREATED THROUGH EXISTING TOOLING |
| Catalog records | Needed for product/inventory context | CAN BE CREATED THROUGH EXISTING TOOLING |
| Inventory records | Needed for inventory pressure | CAN BE CREATED THROUGH EXISTING TOOLING |
| Orders | Needed for order lane story | CAN BE CREATED THROUGH EXISTING TOOLING |
| Alert | Needed for attention story | CAN BE CREATED THROUGH EXISTING TOOLING or system state |
| Recommendation | Needed for decision-support story | CAN BE CREATED THROUGH EXISTING TOOLING or system state |
| Connector | Needed for integration visibility | CAN BE CREATED THROUGH EXISTING TOOLING |
| Failed inbound item | Needed for replay story | CAN BE CREATED THROUGH EXISTING TOOLING |
| Replayable record | Needed for recovery story | CAN BE CREATED THROUGH EXISTING TOOLING |
| Scenario | Needed for planning/governance story | CAN BE CREATED THROUGH EXISTING TOOLING |
| Approval state | Needed for approval story | CAN BE CREATED THROUGH EXISTING TOOLING |
| Realtime event | Needed for live update proof | CAN BE CREATED THROUGH EXISTING TOOLING |
| Real Company 1 data | Not needed for first presentation | REQUIRES MANUAL PREPARATION after intake |
| Production Company 1 records | Not needed before provisioning phase | NOT APPROPRIATE FOR FIRST DEMO |

Recommended source of demo readiness:

- run live connection check
- run hosted proof preparation for the approved proof/demo tenant
- run hosted proof if this is a formal technical demonstration
- avoid creating Company 1 records until Phase 2/3 authorizes them

## Presenting Intelligence

Use this language:

> SynapseCore uses intelligence inside the operational workflow. Recommendations help operators understand possible actions, but humans retain governance for high-impact decisions. The platform should make evidence visible instead of hiding automated decisions behind the scenes.

Explain:

- recommendations support decisions
- evidence should be visible
- humans retain governance for high-impact actions
- intelligence is embedded in workflow
- SynapseCore does not blindly operate the company

Do not say:

- "AI makes the decision"
- "the system runs the company for you"
- "operators no longer need to review"
- "automation is always correct"

## Presenting Failure And Replay

Use this language:

> Replay matters because operational failures should not disappear into logs. If inbound work fails, SynapseCore is designed to make that failure visible, help the operator inspect why it failed, determine whether recovery is safe, and replay eligible work through the supported live flow with evidence.

Explain:

- failures are surfaced intentionally
- failed inbound work can become visible in the replay queue
- investigation matters before replay
- replay eligibility matters
- operator role and connector state matter
- recovery should produce confirmation and audit visibility where supported

Do not overstate replay:

- it is not universal recovery for every external system failure
- it should not bypass governance
- it should not hide repeated connector problems
- it should not be used casually against real customer data without pilot approval

Key phrase:

**Truth over fake success.**

## Presenting Realtime

Use this language:

> SynapseCore is designed so supported operational changes can appear in the workspace without operators constantly refreshing the browser. Hosted proof verifies realtime dashboard update behavior. Realtime still depends on network conditions, backend health, and websocket/SockJS readiness.

Do not promise:

- zero delay
- perfect internet conditions
- infinite WebSocket scale
- realtime behavior when readiness/websocket checks are unhealthy

Mention current proof:

- hosted proof covers realtime dashboard update behavior
- Gate 3 local proof connected 50 SockJS/STOMP clients and verified event delivery in the corrected trigger run

## Presenting Security And Trust

Business-level explanation:

> SynapseCore uses authenticated workspace access, tenant-scoped identity, role-aware surfaces, and controlled provisioning. The pilot workspace is prepared by the SynapseCore platform owner so customer operators receive approved access rather than configuring infrastructure themselves.

Explain at a high level:

- users sign in to a specific workspace
- tenants/workspaces are intended isolation boundaries
- roles affect access and actions
- production sessions are expected to be secure and Redis-backed
- runtime trust is visible
- proof stops when readiness/auth/websocket prerequisites are unhealthy

If a technical team asks deeper questions:

- refer to [security-and-trust-model.md](security-and-trust-model.md)
- refer to [technical-reviewer-guide.md](technical-reviewer-guide.md)
- refer to [proof-and-validation.md](proof-and-validation.md)
- refer to [final-pre-pilot-release-gate.md](final-pre-pilot-release-gate.md)

Do not improvise cryptographic, compliance, or SLA claims that are not documented.

## Presenting Scale

If asked "How many users can it handle?", answer:

> The current Company 1 pilot is scoped for 3 to 5 operators. That is the number we are proposing to run first. In controlled local production-shaped testing, SynapseCore supported 25 authenticated concurrent read operators, about 41 requests per second, p95 under 500 ms in the accepted focused/soak evidence, and 50 realtime clients connected and receiving events in the corrected realtime run. That evidence supports a small controlled pilot. It is not a measured live Render saturation limit and it is not an enterprise-wide scale claim.

Important wording:

- do not present 25 as the maximum platform limit
- do not present 25 as proven Render capacity
- do not imply high-write or multi-tenant scale is proven
- do not claim HA from Gate 3

## Presenting Readiness

If asked "Is SynapseCore production ready?", answer:

> SynapseCore is ready for a controlled Company 1 pilot with documented operating conditions. It has passed hosted proof, control verification, backend tests, frontend build/verify, security gate work, application-level backup/restore proof, and controlled local performance proof. We are not claiming unrestricted enterprise general availability, global HA, or broad production scale yet.

Mention current evidence:

- controls: `201 / 201`
- backend tests: `133 / 133`
- hosted proof: `6 / 6`
- build/verify: PASS
- security gate: accepted
- backup/restore: application-level proof accepted with documented limitation
- performance/scale: accepted with documented limitation

Known limitation to state if asked:

- provider-level Render restore evidence remains documented and should be captured before reliance expands

## Objection And Question Handling

| Question | Presenter Answer |
| --- | --- |
| What exactly is SynapseCore? | An intelligent operations platform that brings operational visibility, recovery, governed decision-support, realtime state, and runtime trust into one command-center workspace. |
| Is this AI? | Do not position it as AI-powered. The right phrase is intelligence inside the workflow: recommendations and operational signals support human decisions. |
| Does it replace our ERP? | No, not during the pilot. The ERP or source system remains authoritative. |
| Does it replace our WMS? | No, not during the pilot. SynapseCore can sit beside warehouse/inventory systems as a visibility and coordination layer. |
| Does it make decisions automatically? | High-impact actions should remain governed. SynapseCore supports decisions and approvals; it does not blindly operate the company. |
| How does it connect to our systems? | The pilot will use one agreed connector or controlled data path. Exact setup depends on Phase 2 intake and the provisioning audit. |
| What happens if integration fails? | Failure should become visible where supported. Operators can inspect failed inbound work and use replay when the record is eligible and recovery is safe. |
| What happens if SynapseCore fails? | The pilot pauses operational reliance, existing systems remain authoritative, and incident/rollback rules apply. |
| How is our data separated? | The platform is tenant/workspace scoped. Tenant isolation is a core trust model and must be verified during provisioning. |
| How many users can it support? | Company 1 starts with 3 to 5 operators. Gate 3 local proof supports confidence for that envelope but is not a live Render saturation limit. |
| Is it production ready? | It is ready for controlled pilot use with documented operating conditions, not broad enterprise general availability. |
| What do our employees configure? | For Company 1, they should not configure infrastructure or tenant setup. SynapseCore provisions the workspace and approved accounts. |
| How much data do you need? | A bounded real operational slice tied to the first pilot lane, not the whole company dataset. |
| How long would setup take? | It depends on intake quality, data availability, connector method, and verification. Do not promise a timeline before Phase 2. |
| What would the pilot prove? | Operational fit, visibility, integration usefulness, failure visibility, recovery, decision support, governance, realtime behavior, and operator usefulness. |
| What happens after the pilot? | Evidence is reviewed. The result may be continue, targeted fixes, expansion, production planning, or offboarding. |
| Can we expand later? | Yes, if pilot evidence justifies expansion and operational risk remains controlled. |
| What if we decide not to continue? | The pilot can be stopped. Existing systems remain authoritative, and offboarding should preserve customer data handling expectations. |

## Prohibited Claims

Do not claim:

- unlimited users
- unlimited scale
- zero downtime
- zero defects
- full ERP replacement
- full WMS replacement
- automatic support for every integration
- autonomous company management
- guaranteed cost savings
- guaranteed percentage improvements
- enterprise HA already proven
- provider disaster recovery already fully proven
- every industry workflow already supported
- customer self-service setup for the pilot
- secret handling through source code

## Pilot Framing

Recommended Company 1 starting shape:

| Area | Starting Scope |
| --- | --- |
| Workspaces | 1 |
| Operators | 3 to 5 |
| Initial connector lanes | 1 |
| Data | Bounded real-company operational slice |
| Systems of record | Existing company systems remain authoritative |

Why start this way:

- keeps rollback practical
- limits operational blast radius
- makes tenant isolation easier to verify
- makes operator feedback easier to understand
- proves value through evidence before expansion
- avoids turning the first pilot into a general rollout

The pilot should prove:

- operational fit
- visibility
- integration usefulness
- failure visibility
- recovery
- decision support
- governance
- realtime behavior
- operator usefulness

## Presenter Pre-Meeting Checklist

Before any live presentation:

- Run `scripts/check-live-connections.ps1`.
- Confirm `PROOF_ALLOWED=True` if doing a live proof-backed demo.
- Confirm frontend loads.
- Confirm backend readiness.
- Confirm auth/session endpoint responds.
- Confirm `/ws/info` responds.
- Confirm demo workspace exists.
- Confirm demo account works.
- Confirm role is appropriate for the screens being shown.
- Confirm no customer/private data is exposed.
- Confirm catalog/inventory/order records support the story.
- Confirm connector/demo state is visible if integrations are shown.
- Confirm replay fixture exists if replay is shown.
- Confirm approval/scenario fixture exists if governance is shown.
- Confirm realtime behavior is healthy if realtime is mentioned.
- Clear unrelated browser sessions/tabs.
- Do not display `.env`, proof-state, tokens, backend logs with secrets, or database credentials.
- Prepare fallback screenshots or a recorded walkthrough if Render has a cold start, network issue, or temporary outage.
- Use a readable presentation resolution.
- Keep relevant docs ready: final gate, verification status, pilot program, security/trust, performance proof, backup/restore runbook.

## Go / No-Go Company Fit

Before moving to Phase 2, classify the company fit.

### Proceed To Pilot Intake

Use this result when:

- the use case fits current SynapseCore capability
- the pilot scope can be bounded
- one connector or data path appears feasible
- required data can be obtained legally and appropriately
- the company has a business owner
- the company has a technical contact
- requested scale fits 3 to 5 operators initially
- the company understands SynapseCore is not replacing everything immediately
- success can be measured

### Needs Technical Discovery

Use this result when:

- source system access is unclear
- integration method is unclear
- data ownership or sensitivity needs review
- requested workflow may fit but requires technical mapping
- role/access requirements are unclear
- the company wants proof of a specific connector path before intake

### Not Currently A Fit

Use this result when:

- the company needs unrestricted enterprise HA before pilot
- they require immediate ERP/WMS replacement
- they require many departments or sites on day one
- they need unsupported integration breadth immediately
- they cannot define a bounded pilot lane
- they cannot provide a business owner or technical contact
- they expect autonomous decision-making without governance
- they will not keep existing systems of record authoritative during the pilot

## Phase 2 Handoff Requirements

Do not build Phase 2 in this document. Capture only the information required to start it.

Phase 2: Pilot Intake Pack needs:

- company legal/display name
- primary business owner
- primary technical contact
- department/team for first pilot
- selected operational pain
- selected bounded pilot workflow
- current source systems
- integration/data method candidates
- first 3 to 5 users
- requested roles
- data scope
- sensitive data exclusions
- catalog/product sample
- inventory sample
- order sample
- connector or CSV/API/webhook/scheduled-file details
- approval/governance requirements
- replay/recovery expectations
- success metrics
- pause conditions
- fallback owner
- support contact path

The next phase should not create customer records yet unless intake has enough information and the provisioning audit confirms the supported mechanism.

## Phase 1 Verdict Standard

Phase 1 is acceptable when:

- the presenter can explain SynapseCore in 30 seconds, 60 seconds, and 2 to 3 minutes
- the presenter asks discovery questions before demoing
- the demo sequence uses only real routes and screens
- every demo screen has a purpose
- pilot scope is explained honestly
- prohibited claims are clear
- scale and readiness wording match evidence
- Company 1 setup is framed as operator-managed provisioning
- Phase 2 handoff information is explicit

Current Phase 1 verdict:

`COMPANY PILOT PHASE 1 ACCEPTED`
