# Contributor Guide

This guide explains how to work safely in the SynapseCore codebase without damaging operational trust, proof discipline, or command-center coherence.

## Start With The Product Model

Before changing code, understand that SynapseCore is:

- tenant-scoped
- operationally honest
- replay-aware
- runtime-aware
- proof-protected

It is not a generic CRUD app where visual polish and backend behavior can drift independently without consequence.

## Understand The Main Layers

Core layers to understand first:

- `frontend/src/pages` for route-level product behavior
- `frontend/src/layout` for command-center shell behavior
- `frontend/src/services` for backend communication
- `backend/src/main/java/com/synapsecore` for core domain logic
- `backend/src/main/resources` for environment posture
- `frontend/tests` for hosted proof
- `scripts` for verification and operations helpers
- `docs` for architecture, runbooks, and system philosophy

## Do Not Break Proof Selectors Casually

Some visible labels are intentionally stable because hosted proof depends on them.

Treat proof-critical text and selectors carefully.

If a visible label must change:

- understand the hosted proof impact first
- update proof intentionally
- keep the new label stable and meaningful

Do not create accidental selector drift through casual wording changes.

## Do Not Add Fake Healthy States

SynapseCore values operational honesty.

That means contributors should not:

- hide degraded runtime posture behind cheerful UI
- suppress recovery visibility to make screenshots look cleaner
- fake live behavior when the backend is unavailable
- treat proof blockers as something to mask

If something is degraded, the UX should communicate that calmly and clearly.

## Preserve Replay Truth

Replay is one of the strongest reasons the platform exists.

Contributors should preserve:

- replay visibility
- replay traceability
- clear replay action semantics
- the distinction between failure, waiting, recovery, and success

Avoid turning replay into a vague background concept.

## Work Safely On The Frontend

When changing frontend code:

- keep the design system consistent
- protect command-center hierarchy
- preserve stable operational language
- avoid raw technical noise in user-facing states
- do not weaken empty, loading, or degraded-state UX

Always remember that the frontend is not just decorative. It is part of operational trust.

## Work Safely On The Backend

When changing backend code:

- preserve tenant enforcement
- preserve replay and scenario behavior carefully
- protect readiness, auth, and websocket trust
- be cautious about high-fanout or heavy read paths
- think about operational consequences, not only API correctness

Backend behavior often affects both proof and operator trust.

## Run Local Checks

Useful baseline checks:

```powershell
cd frontend
npm.cmd run verify
```

```powershell
cd ..
powershell -ExecutionPolicy Bypass -File scripts\check-local-connections.ps1
```

Use hosted proof only when live readiness, auth, and websocket trust are healthy.

## Understand Runtime Trust Philosophy

Runtime trust is part of the product, not just the platform plumbing.

Contributors should ask:

- what does the user see if the backend is degraded?
- what happens if readiness is false?
- what happens if websocket trust disappears?
- are we making the system more truthful or less truthful?

That mindset matters more than any single style preference.

## Read The Right Docs First

Strong starting docs:

- `docs/system-architecture.md`
- `docs/frontend-flow.md`
- `docs/backend-flow.md`
- `docs/proof-and-validation.md`
- `docs/current-limitations.md`
- `docs/infrastructure-handbook.md`
- `docs/technical-reviewer-guide.md`

## Bottom Line

The safest way to contribute to SynapseCore is to treat it as a real operational platform.

That means protecting:

- truth
- replay
- runtime trust
- tenant safety
- proof discipline

If a change makes the platform look better while making it less honest, it is probably the wrong change.
