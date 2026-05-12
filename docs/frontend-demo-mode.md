# Frontend Demo Mode

## Status

Demo mode is planned, not enabled in the live frontend runtime today.

This document defines the safe direction for a future offline preview mode so SynapsCore surfaces can be reviewed when the backend or database is unavailable.

## Goal

Allow the frontend to render a controlled, clearly labeled preview experience for selected authenticated surfaces without pretending that live operational data is available.

## What Demo Mode Is

- An explicit preview-only frontend mode
- Enabled only by an environment flag such as `VITE_DEMO_MODE=true`
- Read-only
- Backed by static frontend fixtures, not hidden API fallbacks
- Clearly labeled in the UI as `Demo preview mode`

## What Demo Mode Is Not

- Not live production data
- Not a hosted proof substitute
- Not a silent fallback when the backend is down
- Not a fake "healthy" system state
- Not a replacement for real replay, approvals, runtime, or integration verification

## Safe Activation Contract

If implemented, demo mode should only activate when all of the following are true:

- `VITE_DEMO_MODE=true`
- Local or internal preview environment only
- A clear shell-level or page-level banner is visible
- No live session/auth state is implied unless explicitly mocked for preview

It should never activate automatically in production or during hosted proof runs.

## Suggested First Pages For Fixture Support

If we implement this in a later phase, start small:

1. Dashboard
2. Orders
3. Inventory
4. Replay Queue
5. Runtime

These are the highest-value surfaces for UI review when backend dependencies are paused.

## Fixture Guidelines

- Use static JSON fixtures inside the frontend only
- Keep fixture names explicit, for example `demoDashboardSnapshot`
- Prefer realistic but obviously non-live data
- Include empty, degraded, and healthy preview states where useful
- Keep actions disabled or clearly labeled when they would otherwise mutate state

## UI Labeling Requirements

Every demo-mode surface should make the state obvious:

- `Demo preview mode`
- `Static operational preview`
- `Actions are disabled in preview mode`

This avoids confusing demo screenshots with live hosted proof evidence.

## Recommended Rollout Plan

### Phase A

- Shell-level demo banner
- Dashboard-only fixtures
- No interactive mutations

### Phase B

- Add fixture-backed read-only operations pages
- Add degraded/empty preview variants

### Phase C

- Add preview toggles for internal product review only
- Document screenshot flows for consistent demo capture

## Relationship To Hosted Proof

Hosted proof remains the source of truth for:

- auth behavior
- replay and recovery
- integrations
- scenarios and approvals
- runtime trust
- live order and inventory behavior

Demo mode is only for frontend product review when those systems are intentionally unavailable.
