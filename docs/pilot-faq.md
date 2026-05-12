# Pilot FAQ

## Is SynapseCore production-ready?

SynapseCore has real product scope and real operational flows, but it should be described honestly as pilot-ready within bounded scope rather than universally production-ready for every enterprise situation.

It supports real auth, catalog, inventory, orders, integrations, replay, approvals, runtime trust, and hosted proof. Broader enterprise hardening is still needed before larger claims are justified.

## Is SynapseCore enterprise-ready?

Not in the sense of claiming every mature enterprise requirement is already solved.

It is better described as:

- serious pilot software
- technically credible
- operationally grounded
- still requiring additional hardening for broader enterprise rollout

## What is currently proven?

Currently proven at a meaningful level:

- the frontend command-center experience
- tenant workspace model
- auth/session flows
- core operational pages
- replay and recovery model
- scenario approval and execution flows
- runtime trust surfaces
- frontend verification discipline
- hosted proof discipline, when backend dependencies are healthy

## What happens when the DB is down?

When the database is unavailable, backend readiness may fail and the product should not pretend everything is healthy.

In that state:

- frontend may still load
- backend endpoints may fail or time out
- auth and websocket trust may be unavailable
- hosted proof should pause

This is treated as operational truth, not hidden behind a fake success state.

## What if realtime disconnects?

The platform should show reconnecting or degraded behavior rather than pretending live updates are still trustworthy.

That helps operators understand whether they are looking at current truth or stale truth.

## Can SynapseCore replace ERP systems?

No, not as an honest claim today.

SynapseCore is better understood as a live operational coordination layer above fragmented systems, not as a full ERP replacement.

## Does it support SSO?

Advanced enterprise identity features such as SSO, SAML, or broader OIDC maturity should be treated as hardening roadmap items rather than current core strengths.

## What infrastructure is recommended?

The platform currently assumes:

- a frontend deployment
- a backend deployment
- PostgreSQL
- Redis
- working health, readiness, auth, and websocket posture

The current platform benefits from disciplined infrastructure ownership rather than casual deployment assumptions.

## Why does hosted proof matter?

Hosted proof matters because it validates the real deployed system instead of relying on presentation-only confidence.

It helps protect against:

- selector drift
- UI regressions
- broken critical flows
- false trust when backend readiness is unhealthy

## Why are failures visible?

Because invisible failures are more dangerous than visible ones.

SynapseCore intentionally keeps:

- replay failures
- connector degradation
- readiness problems
- runtime trust warnings

operator-visible where appropriate so users can respond safely.

## Why are degraded states shown?

Because waiting, degraded, or reconnecting truth is safer than false reassurance.

The product is designed around operational honesty, especially in environments where hidden failure creates expensive downstream damage.

## Who should pilot it first?

The best first pilot customers are companies with:

- fragmented operational systems
- recurring inbound or connector issues
- approval friction
- stock or fulfillment pressure
- a real need for better live operational coordination

## Who should not pilot it yet?

Poor fit today includes:

- very small businesses needing only basic inventory or reporting
- organizations expecting a broad connector marketplace immediately
- buyers expecting a full ERP replacement
- companies unwilling to adopt the platform gradually and honestly

## What is the safest way to adopt it?

The safest path is:

- start with one workspace
- onboard a few clear roles
- use a meaningful but narrow catalog and inventory slice
- enable one real connector lane
- validate replay, approvals, and runtime trust
- expand only after the first operational lane is stable

## Bottom Line

SynapseCore is best evaluated as a serious operational pilot platform with clear differentiators in replay, approvals, runtime trust, and command-center coordination.
