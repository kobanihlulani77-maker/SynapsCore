# Product Decision Principles

These are permanent principles for how SynapseCore should evolve.

They protect the platform from shallow feature growth, fake confidence, hidden operational risk, and unnecessary complexity.

## 1. Truth Before Appearance

SynapseCore should show the real state of operations and infrastructure, even when that state is degraded.

A polished false-success state is worse than an honest waiting, reconnecting, failed, blocked, or degraded state.

## 2. Reliability Before Feature Count

More features do not matter if operators cannot trust the system.

Reliability includes:

- backend readiness
- DB availability
- Redis/session posture
- websocket behavior
- proof stability
- replay determinism
- deployment confidence

## 3. Intelligence Before Automation

SynapseCore should help people understand operational context before it automates action.

Operational intelligence includes:

- alerts
- recommendations
- scenario framing
- runtime trust
- replay visibility
- approval ownership

Automation should come only after the workflow is understood and governed.

## 4. Human Decision Support Over Hidden Automation

When an action is risky, the platform should support human decision-making instead of silently executing.

Scenarios and approvals exist because operations often require context, accountability, and timing judgment.

## 5. Evidence Before Implementation

Future work should start from real evidence:

- pilot feedback
- customer workflow observation
- hosted proof result
- runtime signal
- incident report
- support burden
- measurable outcome

Ideas without evidence should be parked, not implemented.

## 6. Simplicity Over Unnecessary Complexity

SynapseCore should not add architectural or product complexity before it is justified.

Do not introduce:

- speculative infrastructure
- broad connector claims
- excessive configuration
- confusing UI layers
- premature enterprise patterns

Complexity must earn its place through operational evidence.

## 7. Pilot Validation Before Broad Rollout

A change that affects real operators should be validated in a controlled pilot before broader claims are made.

Pilot validation should confirm:

- the workflow improved
- operators understood the change
- support burden did not increase
- proof remained healthy
- rollback posture was clear

## 8. Operational Confidence Before Expansion

Expansion should happen only when the current scope is stable and trusted.

Examples:

- add connector breadth after replay and connector visibility are stable
- expand roles after auth/session and approval governance are clear
- scale infrastructure after monitoring shows scale pressure
- introduce automation after human decision paths are proven

## 9. Runtime Trust Is Product Trust

Runtime health is not only an infrastructure concern.

If readiness, websocket, DB, Redis, auth, or connector health is degraded, operators should know. Proof should pause. Support should classify. Product should not pretend.

## 10. Replay Is A First-Class Product Feature

Failed inbound work should not be hidden in logs or manual cleanup.

Replay should remain:

- visible
- reviewable
- deterministic where supported
- auditable
- proof-protected

## 11. Tenant Context Must Stay Explicit

Tenant/workspace context is foundational.

Future changes must preserve:

- tenant-scoped data
- tenant-scoped users
- tenant-scoped realtime updates
- tenant-scoped proof behavior

## 12. Documentation Is Product Quality

For an enterprise platform, undocumented behavior is fragile behavior.

Changes should update the appropriate docs:

- product knowledge
- engineering guidance
- runbooks
- support playbooks
- proof docs
- release evidence

## Bottom Line

SynapseCore should become smarter, more reliable, and more trusted before it becomes broader.
