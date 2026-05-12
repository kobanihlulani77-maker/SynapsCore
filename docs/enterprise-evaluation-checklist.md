# Enterprise Evaluation Checklist

This checklist helps technical reviewers and enterprise stakeholders evaluate SynapseCore responsibly.

It is intended to support architecture review, operational fit review, and pilot go/no-go conversations.

## Architecture Fit

Questions to ask:

- Does the tenant workspace model align with the company's operating structure?
- Does the React frontend plus Spring Boot backend model fit existing technical standards?
- Is PostgreSQL acceptable as the operational source of truth?
- Is Redis acceptable for session and realtime support?
- Are the current deployment assumptions compatible with the company's environment?

Already proven:

- a real frontend/backend split exists
- tenant-scoped operational surfaces exist
- runtime, replay, scenarios, and operational pages exist as working product domains

Needs deeper hardening:

- broader high-availability posture
- deeper horizontal scaling strategy
- more mature enterprise deployment topology

## Operational Fit

Questions to ask:

- Does the company actually suffer from fragmented operational truth?
- Are replay and recovery meaningful business concerns?
- Do approvals require more visible control?
- Would operators benefit from runtime trust inside the product?
- Is one live command surface more valuable than more isolated tools?

Already proven:

- the platform has coherent operational surfaces
- replay, approvals, runtime, and integrations are first-class concepts

Needs deeper hardening:

- wider operational stress proof
- larger-scale rollout evidence

## Integration Fit

Questions to ask:

- Are webhook, CSV, or scheduled pull lanes enough for the initial pilot?
- Which source systems matter first?
- Who owns connector support and recovery?
- How often do inbound failures currently create manual work?

Already proven:

- integration visibility and replay model are real product features

Still roadmap or bounded:

- broad connector marketplace
- high-volume multi-system expansion without further hardening

## Infrastructure Fit

Questions to ask:

- Can the company support the required backend, DB, and Redis dependencies?
- Is the company comfortable with readiness/liveness-driven trust?
- Can the team distinguish frontend shell availability from backend readiness?
- Does the hosting environment support the required operational discipline?

Already proven:

- health, readiness, auth, and websocket trust are treated explicitly

Needs deeper hardening:

- more mature infrastructure recovery posture
- stronger deployment resilience at scale

## Security Posture

Questions to ask:

- Is the current auth/session model acceptable for a pilot?
- Are tenant boundaries clear enough?
- Is rate limiting and CORS posture understood?
- Which enterprise identity requirements are mandatory before broader rollout?

Already proven:

- auth/session, tenant scoping, and security/trust discipline are present
- leakage and security testing are part of the project discipline

Still needed for larger enterprise confidence:

- SSO or federation maturity
- advanced RBAC
- broader secrets lifecycle maturity

## Recovery Posture

Questions to ask:

- Is failed inbound work currently hidden in the buyer's environment?
- Would replay visibility materially improve operations?
- Is manual recovery traceability important?
- Can operators distinguish waiting, degraded, and replay-pending states safely?

Already proven:

- replay and recovery are visible product concepts
- the platform does not treat recovery as a hidden support-only action

Needs deeper hardening:

- more scale and sustained failure testing
- broader operational playbooks across environments

## Observability And Runtime Trust

Questions to ask:

- Do operators need runtime trust cues in the same product they use for recovery and control?
- Do technical reviewers want truthful degraded-state behavior instead of cosmetic health?
- Is the company prepared to treat runtime truth as part of operational UX?

Already proven:

- runtime trust is integrated into the product model
- degraded state is not intentionally hidden

Needs deeper hardening:

- richer metrics, tracing, and long-horizon observability

## Operational Maturity

Questions to ask:

- Is the company comfortable starting with a bounded pilot rather than a broad replacement?
- Are pilot sponsors aligned on success metrics?
- Can the company support real operator adoption?
- Is the company looking for a command-center layer or just a report viewer?

Healthy evaluation answer:

SynapseCore is strongest when the company needs live coordination, recovery, and trust more than another passive dashboard.

## Current Limitations To Review Explicitly

Reviewers should explicitly acknowledge:

- current connector breadth is limited
- large-enterprise hardening is not complete
- backend dependency health still matters significantly
- hosted proof is rigorous, but not a substitute for broader enterprise-scale validation
- some capabilities are pilot-strong before they are large-enterprise mature

## Bottom Line

The right enterprise evaluation question is not:

"Does this already claim everything?"

The right question is:

"Does this platform solve our operational coordination problem in a way that is truthful, governable, and worth piloting?"
