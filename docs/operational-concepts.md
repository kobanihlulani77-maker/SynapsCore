# Operational Concepts

This document defines the canonical product concepts used across SynapseCore.

These definitions should be used consistently in product docs, support conversations, implementation planning, buyer explanations, and engineering discussions.

## Workspace

A workspace is the tenant-scoped operating boundary where a company or pilot team uses SynapseCore.

A workspace contains the operational context for:

- users and roles
- catalog and products
- inventory
- orders
- integrations
- replay records
- scenarios
- approvals
- alerts and recommendations
- runtime visibility

The workspace matters because SynapseCore is a tenant-based SaaS platform. Product data, operational actions, proof setup, and user sessions must stay tied to the correct tenant context.

## Runtime

Runtime is the live operational health and trust surface of the platform.

It answers:

- is the backend reachable?
- is readiness healthy?
- is the database reachable?
- is Redis/session support healthy?
- is realtime responding?
- are connectors degraded?
- are incidents visible?

Runtime is not just infrastructure monitoring. In SynapseCore, runtime is part of the product because operators need to know whether the system can be trusted before acting on operational information.

## Replay

Replay is the recovery path for failed or blocked inbound operational work.

Replay exists because integration failures should not disappear. A failed inbound event should be visible, reviewable, and recoverable when safe.

Replay can involve:

- a failed connector or inbound payload
- a replay queue entry
- operator review
- manual replay into the live flow
- revalidation
- audit confirmation

Replay is not a workaround. It is a core product philosophy: operational systems must recover deterministically.

## Scenario

A scenario is a proposed operational action or decision path.

Scenarios help convert a recommendation or operational issue into a controlled execution path. A scenario may require approval before it can execute.

Examples:

- respond to inventory pressure
- act on a recommendation
- recover or execute an operational adjustment
- coordinate a controlled action after review

## Recommendation

A recommendation is system-visible guidance produced from operational context.

Recommendations help operators understand what might need attention. A recommendation should not be confused with automatic execution.

The usual path is:

```text
Signal -> Recommendation -> Scenario -> Approval if required -> Execution or rejection
```

## Alert

An alert is a visible warning that something needs attention.

Alerts are more immediate than general dashboard information. They can represent operational pressure, connector concerns, inventory risk, runtime trust issues, or a condition that should be reviewed.

An alert should answer:

- what happened?
- where is it happening?
- how serious is it?
- what should an operator inspect next?

## Approval

An approval is a governance step before an operational action proceeds.

Approval exists because not every operational action should execute immediately. Some actions require human review, role gating, or operational confirmation.

Approval paths can end in:

- approved and executed
- rejected and stopped
- revised and resubmitted
- left pending until ownership is clear

## Command Center

The command center is the authenticated SynapseCore workspace shell where operational work is coordinated.

It is not just a dashboard. It brings together:

- live dashboard visibility
- catalog and inventory surfaces
- orders
- integrations
- replay
- scenarios
- approvals
- alerts and recommendations
- runtime trust
- users and settings

The command center exists so operators can see, decide, recover, and govern work from one operational surface.

## Integration

An integration is an inbound or external-system connection that brings operational data into SynapseCore or exposes connector state.

Current supported integration posture is intentionally bounded and includes webhook, CSV, and scheduled pull style order ingestion patterns documented elsewhere in the repo.

Integration visibility matters because failed data movement is one of the main reasons operations become fragmented.

## Operational Intelligence

Operational intelligence is the combination of live state, business context, recommendations, alerts, scenario control, and runtime trust.

It is different from BI/reporting because it is meant to support current operational action, not only historical analysis.

Operational intelligence asks:

- what is happening now?
- what is at risk?
- what failed?
- what can be replayed?
- what needs approval?
- what is safe to execute?
- can the system be trusted right now?

## Audit

Audit is the trace of what happened, who acted, what changed, what failed, what was replayed, and what was approved or rejected.

Audit turns operational memory into product truth. It supports incident review, support escalation, pilot evidence, and future governance hardening.

## Degraded State

A degraded state means a system component is not fully healthy, but the product is still showing the truth instead of pretending everything is fine.

Examples:

- websocket reconnecting
- backend unavailable
- readiness failing
- connector degraded
- replay pending
- runtime trust warning

Degraded state visibility is a product feature because hidden failure is operationally dangerous.
