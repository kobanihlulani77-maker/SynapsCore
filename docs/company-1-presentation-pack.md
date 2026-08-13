# Company 1 Presentation Pack

This pack explains how to present SynapseCore to the first controlled pilot company.

It is not a sales deck, a feature roadmap, or a promise of broad enterprise readiness. It is a disciplined first-meeting and demo guide for explaining the current product honestly, showing the right surfaces, and setting up the pilot conversation without creating false expectations.

## Current Position

SynapseCore is ready for a controlled Company 1 pilot with documented operating conditions.

Accepted baseline:

- final pre-pilot classification: `READY FOR CONTROLLED COMPANY 1 PILOT - WITH DOCUMENTED OPERATING CONDITIONS`
- final hosted proof: `6 / 6`
- Gate 4 controls: `201 / 201`
- recommended pilot envelope: 1 workspace, 3 to 5 operators, 1 connector lane initially
- existing systems of record remain authoritative during the pilot

Do not present SynapseCore as general availability, high-availability enterprise infrastructure, a full ERP replacement, a full WMS replacement, or autonomous operations software.

## First Meeting Goal

The first meeting should answer five questions:

1. What operational problem does SynapseCore solve?
2. Where does SynapseCore fit beside the company's existing systems?
3. What will the pilot prove?
4. What will the pilot not attempt to prove yet?
5. What information is needed before a safe Company 1 workspace can be provisioned?

The meeting should end with a clear next step: complete pilot intake and perform the internal provisioning audit/setup sequence.

## Core Explanation

Use this short explanation:

> SynapseCore is an intelligent operations platform for teams that need better visibility, recovery, governed decision-making, and runtime trust across orders, inventory, integrations, replay, approvals, scenarios, alerts, and recommendations. During the pilot it does not replace your ERP, WMS, ecommerce platform, or existing source system. It sits beside those systems as a command-center layer for one controlled operational lane.

If they ask, "Is this just a dashboard?", answer:

> No. A dashboard mostly shows what already happened. SynapseCore is designed to help operators see operational state, understand what needs attention, recover failed inbound data, route governed decisions through approvals, and verify runtime trust. The product includes dashboard visibility, but the core value is operational coordination and recovery.

## What To Say

- SynapseCore is a command-center layer for operational visibility, coordination, replay/recovery, approvals, scenarios, and runtime trust.
- The first pilot is intentionally narrow so we can prove value safely.
- The company's existing operational systems remain the source of record.
- SynapseCore should earn trust through evidence before scope expands.
- Failures and degraded states are intentionally visible because hidden failure is operationally dangerous.
- The pilot is reversible.
- The customer receives a pre-provisioned workspace and approved user accounts; they do not configure infrastructure.

## What Not To Promise

Do not promise:

- full ERP replacement
- full WMS replacement
- global enterprise HA
- unrestricted multi-site rollout
- autonomous execution without human approval
- guaranteed cost savings
- unlimited connector compatibility
- SSO/SAML/OIDC if it is not part of the current supported scope
- production SLA unless an SLA has been defined and signed
- provider-level backup/restore maturity beyond the evidence already captured

Avoid phrases like:

- "AI-powered operations"
- "replaces your entire stack"
- "fully enterprise-ready"
- "plug in anything"
- "set it and forget it"
- "guaranteed savings"

Prefer phrases like:

- "intelligence inside the operational workflow"
- "controlled pilot"
- "existing systems stay authoritative"
- "operator-visible recovery"
- "governed decision support"
- "runtime trust"
- "evidence before expansion"

## First Meeting Structure

Recommended sequence:

1. Confirm the company's operational pain.
2. Explain SynapseCore's role as a command-center layer.
3. Explain the controlled pilot boundary.
4. Demonstrate the product in a safe flow.
5. Explain what the pilot will measure.
6. Explain what information is needed for intake.
7. Confirm next step: pilot intake and internal provisioning.

Time box:

| Segment | Time |
| --- | ---: |
| Problem framing | 5 minutes |
| Product explanation | 5 minutes |
| Guided demo | 20 minutes |
| Pilot boundary and success criteria | 10 minutes |
| Intake questions and next steps | 10 minutes |

## Recommended Demo Flow

Use this order because it tells the product story from operations visibility to recovery and governed action.

| Step | Screen | Why It Matters |
| --- | --- | --- |
| 1 | Public homepage | Explain positioning honestly before entering the workspace. |
| 2 | Sign In | Show that Company 1 users receive approved access to a ready workspace. |
| 3 | Dashboard | Show the command-center view and current operational state. |
| 4 | Orders | Show the operational lane the pilot will focus on. |
| 5 | Inventory | Show stock pressure, thresholds, and operational impact. |
| 6 | Alerts | Show what needs attention. |
| 7 | Recommendations | Show decision-support, not hidden automation. |
| 8 | Integrations | Show connector visibility and inbound trust. |
| 9 | Replay Queue | Show failed inbound recovery and why replay matters. |
| 10 | Approvals | Show governed decision control. |
| 11 | Scenarios | Show planning before execution. |
| 12 | Runtime | Show health, readiness, realtime, and trust posture. |

Do not start the demo with settings or admin pages unless the audience is technical. For executives and operations leaders, start with Dashboard, Orders, Inventory, Alerts, Recommendations, and Replay.

## Demo Script

Opening:

> The goal today is not to pretend SynapseCore is replacing your operational systems. The goal is to show how it can support one controlled operational lane by improving visibility, recovery, decision flow, and trust.

Dashboard:

> This is the command center. Operators should be able to see what is healthy, what needs attention, what changed, and where to go next without hunting across disconnected tools.

Orders:

> Orders are one of the clearest places where operational pressure appears. The pilot should select a bounded order lane so we can evaluate whether SynapseCore helps the team respond faster and with better traceability.

Inventory:

> Inventory connects the operational state to action. We are not claiming to replace the inventory source system in the pilot. We are showing how inventory visibility can inform alerts, recommendations, scenarios, and approvals.

Alerts:

> Alerts are the attention layer. The goal is not to create noise. The goal is to make important operational exceptions visible and classifiable.

Recommendations:

> Recommendations are decision-support. They should help operators understand possible action, but they do not silently take over operations.

Integrations:

> Integrations are where many real operational failures begin. SynapseCore makes connector status and inbound trust visible instead of hiding failure behind a silent sync.

Replay:

> Replay is central to the product. When inbound data fails, operators need a controlled way to review, recover, and replay into the live operational flow with evidence.

Approvals:

> Some decisions should be governed. Approvals make the decision path visible and role-aware.

Scenarios:

> Scenarios let the team examine operational options before committing to execution.

Runtime:

> Runtime is the trust surface. If the platform is degraded, operators should know. We prefer visible waiting or degraded states over fake success.

Close:

> If this pilot is successful, we should have evidence that one team can use SynapseCore to understand operational state, recover failure, govern decisions, and respond with more confidence inside a narrow lane.

## Audience-Specific Emphasis

| Audience | Emphasize | Avoid Leading With |
| --- | --- | --- |
| Executive | Pilot boundary, measurable outcomes, risk control, reversibility | Low-level configuration details |
| Operations manager | Dashboard, orders, inventory, alerts, recommendations, replay | Deep infrastructure |
| Warehouse manager | Inventory, orders, operational exceptions, daily SOP | Platform-admin surfaces |
| Technical reviewer | Tenant isolation, readiness, auth/session, websocket, proof, backup limitation | Sales claims |
| Operator | Where to click, what needs attention, what to do when degraded | Product roadmap |

## Pilot Explanation

Use this framing:

> The Company 1 pilot is a controlled operational trial. We choose one workspace, a small set of operators, one initial connector lane, and a bounded data scope. The pilot proves whether SynapseCore helps real operators coordinate work, recover failed inbound data, trust runtime state, and make governed decisions. If the pilot produces useful evidence, we can decide whether to expand carefully.

The pilot does not attempt to prove:

- full company-wide deployment
- high-write enterprise scale
- every connector type
- full system-of-record replacement
- autonomous operations
- long-term SLA coverage

## Safest Next Step

After the first meeting, do not jump straight into production-like onboarding.

Move in this order:

1. Complete Company Pilot Intake.
2. Perform the Company 1 provisioning audit.
3. Confirm pilot scope and success criteria.
4. Provision tenant/workspace/users internally.
5. Configure one connector lane or controlled data import path.
6. Run pre-handover verification.
7. Hand over approved access.
8. Begin day-one pilot guidance.

## Questions To Ask Company 1

Use these questions to prepare for the intake phase:

- Which operational lane causes the most visibility or recovery pain today?
- Which existing system is authoritative for that lane?
- Which team will use SynapseCore first?
- Who are the first 3 to 5 operators?
- What roles do those users need?
- What data should be included in the first bounded slice?
- What systems currently produce orders, inventory, or catalog records?
- What connector or import method is safest for the first pilot?
- What does a successful first two weeks look like?
- What would force the pilot to pause?
- Who approves configuration changes during the pilot?
- Who is the company technical contact?
- Who is the company operations owner?

## Objection Handling

| Question | Recommended Answer |
| --- | --- |
| Can this replace our ERP? | Not during the pilot. Your ERP or source system remains authoritative. SynapseCore supports visibility, coordination, recovery, and governed action around a controlled lane. |
| Is this production-ready? | It is ready for a controlled pilot with documented operating conditions. It is not being claimed as broad enterprise general availability. |
| What happens if the backend is down? | The pilot pauses operational reliance. SynapseCore shows degraded trust where possible, and existing systems remain the fallback. |
| What happens if an integration fails? | Failed inbound data should become visible, reviewable, and recoverable through replay, depending on the connector path used in the pilot. |
| Can operators execute actions automatically? | The current philosophy is governed decision-support. Human approval and visibility matter more than hidden automation. |
| Can we add more departments immediately? | Not at the start. Expansion should follow pilot evidence, not enthusiasm alone. |

## Demo Safety Rules

- Use the approved proof or pilot workspace only.
- Do not show secrets, private tokens, database credentials, or `.env` values.
- Do not manually edit production database rows during or after the demo.
- Do not create live customer records unless the provisioning phase has started.
- Do not promise unsupported connectors.
- Do not demo destructive replay or irreversible actions against real customer data without explicit pilot approval.
- If readiness, auth, or websocket checks are unhealthy, pause the live demo and classify the issue honestly.

## Evidence To Mention

Mention only what is already proven:

- hosted proof passed `6 / 6`
- Gate 4 controls verified `201 / 201`
- backend test suite passed `133 / 133`
- replacement Render PostgreSQL database was validated through readiness and hosted proof
- final pre-pilot classification is ready for controlled Company 1 pilot with documented operating conditions

Also mention limitations:

- provider-level managed restore for the current Render database still needs captured evidence before reliance expands
- live Render saturation is not proven
- the pilot is intentionally small
- existing systems remain authoritative

## What Good Looks Like

A strong first meeting outcome:

- Company 1 understands SynapseCore is a command-center layer, not a system-of-record replacement.
- The team agrees to one controlled pilot lane.
- The first operator group is known.
- The source systems and data scope are known.
- The company accepts that SynapseCore will be pre-provisioned by the platform owner.
- The next step is intake and provisioning audit, not feature expansion.

## Next Phase

The next phase should be:

**Company 1 Provisioning Audit**

That audit should map the real repository-supported capability for tenant creation, workspace creation, user provisioning, role assignment, connector setup, data onboarding, secrets handling, readiness checks, and company-specific verification before any Company 1 setup playbook is finalized.
