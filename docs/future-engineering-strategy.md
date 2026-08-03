# Future Engineering Strategy

This document describes how SynapseCore engineering should evolve over several years.

It is not a feature roadmap. It is an engineering maturity strategy.

## Strategic Principle

SynapseCore should evolve from a proven pilot platform into a more resilient enterprise operations platform through disciplined hardening, not speculative architecture.

Every future engineering investment should improve:

- reliability
- maintainability
- supportability
- security
- observability
- deployment confidence
- operational trust

## Scalability

Current state:

- single backend deployment posture
- PostgreSQL
- Redis-backed realtime/session posture where configured
- proof-validated current supported scope

Future direction:

- separate background workers from request-serving backend
- introduce queue architecture only when workload requires it
- define connector throughput expectations
- define DB index and query performance review cadence
- test replay and scenario flows under higher concurrency

Do not introduce distributed architecture before operational pressure justifies it.

## Resilience

Current state:

- readiness/liveness exist
- recovery docs exist
- hosted proof validates real flows
- DB replacement was recovered and revalidated

Future direction:

- scheduled restore drills
- dependency-failure rehearsals
- deployment rollback drills
- clearer incident severity levels
- automated smoke checks after redeploy
- more explicit degraded-state runbooks

## Observability

Current state:

- runtime page
- actuator endpoints
- incidents and diagnostics surfaces
- request IDs in proof failures

Future direction:

- structured metrics dashboard
- tracing across frontend, backend, DB, Redis, and connector flows
- alerting for readiness degradation
- replay backlog metrics
- connector health metrics
- proof trend history

## Connector Ecosystem

Current state:

- webhook order ingestion
- CSV import
- scheduled pull
- replay/recovery for supported inbound failures

Future direction:

- connector SDK or contract model
- connector certification checklist
- connector sandbox/testing harness
- connector-specific replay policies
- support ownership model

Do not market a broad connector marketplace before connector maturity exists.

## Deployment Maturity

Current state:

- Render deployment is proven for current scope
- Docker Compose supports local/self-host flows
- release candidate checks exist

Future direction:

- stronger CI/CD gates
- release evidence automation
- automated deployment smoke checks
- rollback rehearsal
- environment drift detection
- staged environments when pilot volume justifies them

## Security Maturity

Current state:

- tenant-scoped access model
- rate limiting
- CORS/session posture
- leakage/security docs
- secret scanning script

Future direction:

- formal security review cadence
- SSO/SAML/OIDC
- advanced RBAC
- secrets manager integration
- audit retention policy
- data retention policy enforcement
- tenant isolation testing expansion

## Developer Experience

Current state:

- docs are broad and deep
- scripts support verification and explanation
- local setup exists but has multiple paths

Future direction:

- clearer command taxonomy
- one authoritative onboarding path per environment
- scripted local port diagnostics
- better generated docs index checks
- smaller targeted test commands for proof failures

## Platform Engineering

Future platform engineering should focus on:

- repeatable environments
- infrastructure drift detection
- release automation
- observability platform
- backup/restore automation
- support evidence automation

Avoid premature platform work that does not reduce real operational risk.

## Enterprise Readiness

Enterprise readiness requires:

- stronger HA posture
- backup/restore proof
- security review
- SSO/RBAC maturity
- audit/retention maturity
- support coverage
- incident process
- performance/load evidence
- deployment rollback evidence

Current state is pilot-ready, not full-enterprise-ready.

## Engineering North Star

The engineering organization should optimize for operational truth:

- visible failures over hidden failures
- deterministic recovery over guesswork
- proof over assumption
- small safe changes over broad rewrites
- evidence over optimism
