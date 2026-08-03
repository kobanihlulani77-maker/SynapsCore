# Proof And Validation

This guide explains the validation philosophy of SynapseCore, why hosted proof exists, how local verification fits into the picture, and what the team considers actually proven.

## Hosted Proof Philosophy

Hosted proof exists because feature claims should be tied to the real deployed system.

The platform uses proof to answer a hard question:

does the real frontend, talking to the real backend, with real auth, replay, realtime, and runtime behavior, actually work?

That is why hosted proof is central to engineering confidence.

## Why Proof Is Important

Proof is important because:

- UI polish can drift away from backend truth
- local success can hide deployed failure
- replay and realtime are easy to overclaim
- operational trust depends on full-chain validation

Hosted proof is the discipline that keeps the platform honest.

## Local Verification

Local verification is still important. It helps confirm:

- frontend quality
- backend logic
- local environment posture
- route and script correctness

Typical local verification layers include:

- `npm.cmd run verify`
- local backend integration tests
- `scripts\verify-deployment.ps1`
- `scripts\verify-realtime.ps1`
- `scripts\check-local-connections.ps1`

## Frontend Verify Scripts

The frontend verify path currently includes:

- check/lint-style frontend safety script
- build validation
- proof-label protection

Commands:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\frontend
npm.cmd run lint
npm.cmd run build
npm.cmd run verify
```

These do not prove the backend, but they do protect the frontend from careless regression.

## Backend Integration Tests

Backend integration tests matter because they validate:

- auth/session behavior
- tenant isolation
- replay behavior
- scenario behavior
- production-hardening assumptions
- CORS and security posture

Important suites include:

- `SecurityHardeningIntegrationTest`
- `SecurityVerificationIntegrationTest`
- `ProductionHardeningIntegrationTest`
- `MvpFlowIntegrationTest`

## Replay Proof Evolution

Replay proof evolved because replay is one of the highest-trust product claims.

The hardening work focused on:

- deterministic disabled-connector failure handling
- visible replay queue creation
- safe connector enable/replay sequence
- protection from automation stealing manual-only proof records
- browser-visible replay outcomes

That evolution matters because replay is easy to fake and hard to trust without full-path proof.

## Realtime Proof Evolution

Realtime proof evolved because a dashboard claiming to be live must actually prove it.

The proof grew to verify:

- websocket/SockJS reachability
- authenticated dashboard snapshot readiness
- browser-visible live updates without refresh
- runtime-aware degradation rules

This is part of what makes the command-center claim believable.

## Why Deterministic Replay Was Hardened

Deterministic replay was hardened because:

- failed inbound work must not vanish
- manual recovery should stay predictable
- proof should not pass by luck or timing noise
- recovery must remain operator-visible

Without deterministic replay, operational trust would be much lower.

## How Proof Prevents UI Drift

Proof prevents UI drift by tying browser assertions to stable product outcomes.

That is why proof-safe labels and selectors matter.

When the UI was heavily productized, the project preserved stable labels such as:

- `Replay Into Live Flow`
- `Scenario action console`
- `Approval action console`
- `Live operational command center`
- `Access your operational workspace.`
- `Failed inbound recovery`
- `Live order operations`
- `Operational warning center`

This keeps validation aligned with the real product surface.

## What Is Considered Fully Proven

The current standard for “fully proven” is not just a passing unit test or local build.

Something is meaningfully proven when:

- the deployed frontend and backend both participate
- readiness and trust preconditions are met
- the flow works in the browser
- the backend returns the expected operational result
- the UI reflects the real outcome

Examples of strong proof:

- auth/session through the real deployed UI
- replay through the real replay queue and live order flow
- scenario approval and execution through the real browser flow
- realtime dashboard updates without refresh

## What Is Not Proven Yet

Not everything is fully proven in every scale or deployment dimension.

Examples of what is not yet fully proven:

- large-enterprise scale behavior
- broad connector marketplace behavior
- advanced HA deployment posture
- deeper distributed job and queue separation
- stronger multi-region or large cluster realtime posture

Being explicit about this increases trust rather than reducing it.

## Local Verification Commands

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-local-connections.ps1 -FrontendUrl http://127.0.0.1:5173 -BackendUrl http://127.0.0.1:8080
powershell -ExecutionPolicy Bypass -File scripts\verify-deployment.ps1 -FrontendUrl http://127.0.0.1:5173 -BackendUrl http://127.0.0.1:8080
powershell -ExecutionPolicy Bypass -File scripts\verify-realtime.ps1 -FrontendUrl http://127.0.0.1:5173 -BackendUrl http://127.0.0.1:8080

cd frontend
npm.cmd run verify
```

On Windows local debugging, prefer `127.0.0.1` over `localhost` if endpoint checks disagree with port or service posture.

## Hosted Proof Commands

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1

cd frontend
npm.cmd run test:e2e:prod
```

## Proof Blockers

Hosted proof should be blocked when:

- backend health does not respond
- readiness does not pass
- auth session does not respond
- websocket info does not respond
- backend or DB is timing out

Proof should also be paused when:

- proof credentials are not correctly loaded
- local changes are not verified enough to justify a live rerun

## When Proof Should NOT Run

Do not run hosted proof when:

- frontend is up but backend is timing out
- DB/Redis availability is in question
- readiness is down
- live runtime trust is not established

Proof is not a deployment wake-up command. It is a deployment validation command.

## Bottom Line

The SynapseCore proof system matters because it keeps the product honest.

It is the mechanism that turns “this should work” into “this was proven against the real deployed platform.”
