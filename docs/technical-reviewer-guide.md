# Technical Reviewer Guide

This guide is for technical reviewers, CTOs, enterprise architects, infrastructure leads, and senior engineers evaluating SynapseCore as a serious operational platform.

It is not a sales doc. It explains the engineering philosophy, the validation mindset, and the operational discipline behind the product.

## Architecture Philosophy

SynapseCore is built as a tenant-based operations control platform, not as a reporting site with a backend attached.

The core architecture philosophy is:

- operational truth should be explicit
- failures should remain visible
- recovery should be deterministic
- approvals should be governed
- runtime trust should be visible to both operators and technical reviewers
- frontend UX should reflect infrastructure reality instead of hiding it

That is why the system includes:

- a React command-center frontend
- a Spring Boot backend with explicit domain and operational modules
- PostgreSQL as the record of truth
- Redis for session and distributed realtime posture
- SockJS/STOMP realtime channels
- replay and scenario control loops
- runtime, incident, audit, and proof tooling as first-class parts of the platform

## Tenant Isolation Model

Tenant isolation is one of the foundational credibility requirements of the system.

The platform enforces tenant/workspace boundaries through:

- tenant-scoped auth and session identity
- tenant-scoped repositories and queries
- tenant-scoped realtime topic prefixes
- tenant-aware replay queue visibility
- tenant-aware runtime and admin surfaces

The product is intentionally shaped around company workspaces rather than a flat global data model because operational trust breaks quickly if tenants can leak across boundaries.

## Replay-First Operational Philosophy

One of the strongest architectural choices in SynapseCore is that failed inbound work is treated as an operational object, not a hidden log artifact.

Replay-first philosophy means:

- failed inbound work should remain visible
- recoverable work should not be silently discarded
- recovery should preserve the link between original failure and final live outcome
- recovery actions should be intentional and traceable

This is why deterministic replay mattered so much in the proof hardening phase.

## Runtime Trust Philosophy

Most applications expose health endpoints for platforms and engineers. SynapseCore goes further and treats runtime trust as part of the product itself.

That means:

- liveness and readiness matter operationally
- runtime and incidents are visible inside the command-center product
- degraded-state UX matters
- backend/runtime problems should not be masked by the UI

The goal is for operators and technical reviewers to share one honest picture of the system’s current safety to act.

## Why Hosted Proof Exists

Hosted proof exists because many software products overstate what is real.

SynapseCore uses hosted proof to validate:

- the real frontend
- the real backend
- the real auth/session path
- the real replay flow
- the real scenario approval/execution path
- the real runtime and page surfaces

Hosted proof is important because it prevents the product from being described as “working” when only isolated internal paths were tested.

## Why Truthful Status Matters

Truthful status means:

- if the backend is unavailable, the platform should say so
- if readiness is not passing, hosted proof should stop
- if realtime is degraded, the UI should not pretend it is live
- if replay is blocked, the operator should understand why

This philosophy makes the system more trustworthy to technical reviewers, not less polished.

## Why Deterministic Replay Mattered

Replay hardening mattered because it touches many of the platform’s trust claims at once:

- data recovery
- connector visibility
- operator confidence
- runtime safety
- auditability

If replay is nondeterministic, the product stops being a control platform and becomes another source of operational ambiguity.

That is why replay proof was hardened around:

- structured disabled-connector failures
- manual-only replay eligibility for those failures
- deterministic visibility in the replay queue
- controlled replay into the live order flow

## Why Realtime Health Mattered

Realtime is valuable only if it is trustworthy.

SynapseCore cares about realtime health because:

- operators use the dashboard as a live command surface
- degraded realtime should not be misread as healthy live state
- backend/runtime stress can change the value of what the dashboard is showing

This is why the platform keeps:

- websocket info checks
- runtime trust indicators
- dashboard snapshot readiness checks
- proof gating before realtime-dependent flows begin

## Why Degraded-State UX Matters

Technical reviewer confidence increases when the UI does not hide degraded conditions.

Degraded-state UX matters because:

- operators should understand whether the platform is live, stale, reconnecting, or unavailable
- backend issues should not quietly degrade into false confidence
- the system should stay calm under failure, but still truthful

This is part of the engineering philosophy, not just the visual design.

## How Frontend / Backend Contracts Are Verified

Contracts are verified through:

- backend integration tests
- frontend verify/build checks
- proof-critical selector checks
- local verification scripts
- hosted proof preparation
- hosted Playwright proof

This creates a layered verification model:

1. backend logic confidence
2. frontend stability confidence
3. local environment confidence
4. live deployment confidence

## How Proof Selectors Are Protected

The project intentionally protects proof-critical labels because the product UI evolved significantly without abandoning real validation.

Stable labels include:

- `Replay Into Live Flow`
- `Scenario action console`
- `Approval action console`
- `Live operational command center`
- `Access your operational workspace.`
- `Failed inbound recovery`
- `Live order operations`
- `Operational warning center`

This protects the hosted proof path from casual UI drift without weakening the product surface.

## Why The Project Avoids Fake Demos

SynapseCore avoids fake demos because they create false engineering confidence.

The project tries to stay disciplined about:

- real proof on deployed services
- honest demo-mode boundaries
- no pretending runtime truth exists when dependencies are down
- no calling a feature “done” if the proof path is still unstable

That honesty is one of the system’s strongest engineering signals.

## Engineering Mindset

The engineering mindset behind SynapseCore can be summarized as:

- operational honesty
- failure visibility
- deterministic recovery
- runtime observability
- infrastructure-aware UX
- role-gated control instead of casual mutation

## Bottom Line

Technical reviewers should read SynapseCore as a platform that is deliberately trying to be truthful about operational reality.

Its strongest engineering signal is not just the code shape. It is the discipline around replay, runtime trust, hosted proof, and failure visibility.
