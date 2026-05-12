# Current Limitations

This document exists to increase trust by being explicit about what is still immature, what is pilot-ready but not fully enterprise-ready, and what still depends heavily on infrastructure quality.

## Why This Document Exists

The platform is stronger when it is honest about its limits.

SynapseCore already has meaningful product scope and real proof discipline, but that does not mean every enterprise concern is already solved.

## What Is Still Immature

Areas that are still relatively immature include:

- broad connector breadth
- broader enterprise identity integration
- large-scale deployment and failover posture
- deeper queue and worker separation story
- very large-scale observability maturity

These are not hidden problems. They are the next layers of hardening.

## What Is Pilot-Ready Only

Several parts of the platform are strong enough for pilots and serious controlled scope, but should not yet be overclaimed as universal enterprise scale:

- current connector portfolio
- current Render-centered deployment posture
- current proof-backed replay and scenario scope
- current local full-stack ergonomics

Pilot-ready does not mean trivial. It means:

- real enough to validate value
- not yet broad enough to claim every enterprise rollout shape

## What Depends On Infrastructure Quality

The platform depends heavily on infrastructure quality in the following areas:

- DB availability
- Redis availability
- backend readiness
- websocket/SockJS reliability
- deployed session posture

When infrastructure degrades, the product should stay honest, but user experience is still affected.

## What Is Not Fully Enterprise-Ready Yet

The platform is not fully enterprise-ready yet in these areas:

- high-availability deployment story
- deeper identity federation
- broader delegated administration
- richer connector ecosystem
- larger-scale operational benchmarking
- more mature secrets lifecycle

## What Still Needs Operational Hardening

Operational hardening still needed includes:

- stronger dependency outage behavior
- broader degraded-state confidence under load
- richer restore/rollback rehearsal discipline
- more explicit queue and worker resilience
- stronger metrics and tracing maturity

## What Still Needs Connector Maturity

Connector maturity still needs:

- broader connector coverage
- clearer onboarding patterns for more source types
- stronger support and scale posture for a larger integration estate

## What Still Needs HA / Scaling Work

HA and scale work still needed includes:

- clearer multi-node deployment posture
- stronger distributed worker story
- deeper pub/sub and fanout hardening
- richer performance and scale validation

## Honest Bottom Line

The right way to describe SynapseCore today is:

- real
- meaningful
- pilot-capable
- operationally disciplined
- still maturing toward broader enterprise hardening

That honesty is part of the product’s credibility.
