# Deployment Recovery Guide

This guide explains how SynapseCore should be interpreted and recovered when infrastructure or deployment dependencies fail.

It is written for operators, engineers, deployment reviewers, and anyone deciding whether the system is safe enough to trust or whether hosted proof must pause.

## Why This Guide Exists

SynapseCore is not just a UI with some APIs behind it. It is a live operational platform. That means infrastructure failures change what the product can safely claim at any given moment.

The platform must be able to answer:

- is the frontend up?
- is the backend up?
- is the database ready?
- is auth working?
- is realtime reachable?
- is hosted proof allowed right now?

## Operational Classification

The system currently uses these classifications in live checks:

- `FRONTEND_UP`
- `BACKEND_UP`
- `DB_READY`
- `AUTH_READY`
- `WS_READY`
- `PROOF_ALLOWED`

### What they mean

- `FRONTEND_UP`
  - the frontend shell is reachable
- `BACKEND_UP`
  - the backend is answering at least one meaningful surface
- `DB_READY`
  - readiness is passing, including DB and Redis expectations
- `AUTH_READY`
  - auth/session endpoint is responding
- `WS_READY`
  - websocket info endpoint is responding
- `PROOF_ALLOWED`
  - all trust prerequisites for hosted proof are satisfied

## Failure Cases

### DB is down

Expected effects:

- readiness should fail
- backend may hang or time out during startup
- auth/session may not answer
- websocket info may not answer
- hosted proof must pause

Why it matters:

- without DB, the platform loses operational truth
- proof cannot honestly validate replay, scenarios, auth, or command-center behavior

### Redis is down

Expected effects:

- readiness may fail
- session-backed auth may degrade
- realtime distribution may degrade
- websocket info may answer while full trust is still reduced

Why it matters:

- Redis is part of the current production session and realtime posture

### Backend is hung

Expected effects:

- health endpoints may time out
- auth/session may time out
- websocket info may time out
- frontend shell may still be live

Why it matters:

- a live frontend does not mean the operational platform is healthy

### Frontend is reachable but backend is unavailable

Expected effects:

- homepage may load
- sign-in may load
- authenticated data fetches will fail
- backend-dependent pages will degrade or stop
- hosted proof must not run

### Readiness fails

Expected effects:

- the app process may still be alive
- DB or Redis may still be unavailable
- proof must pause

Why readiness matters:

- liveness alone is not enough to trust a command-center platform

### Websocket layer fails

Expected effects:

- realtime state degrades
- dashboard may show reconnecting or stale posture
- the frontend may still fetch snapshot data, but it is no longer fully live

Why it matters:

- a command-center product must not pretend realtime trust exists when it does not

### Hosted proof should pause

Hosted proof must pause when:

- readiness is down
- auth/session is not responding
- websocket info is not responding
- backend is timing out
- DB or Redis are known unavailable

## Recovery Sequencing

Use this order.

### 1. Confirm the frontend and backend states separately

Do not assume frontend availability means backend readiness.

### 2. Check liveness and readiness

This separates:

- app process alive
- app ready for real traffic

### 3. Check auth/session and websocket info

These verify whether the browser-facing trust surfaces are functioning.

### 4. Check DB and Redis dependency posture

If readiness is not healthy, dependency state must be treated as suspect until confirmed.

### 5. Recover infrastructure first

Do not run hosted proof as a recovery probe.

### 6. Re-run live checks

Only when live checks are clean should proof preparation restart.

### 7. Resume hosted proof in order

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1

cd frontend
npm.cmd run test:e2e:prod
```

## Why Readiness Matters

Readiness matters because it is the first system-level confirmation that:

- DB is reachable
- Redis is reachable
- the backend is safe for real traffic

If readiness is down, the product cannot honestly say that replay, auth, runtime, and realtime are trustworthy.

## Why Proof Must Stop When Trust Is Missing

Hosted proof is a validation mechanism, not a restart strategy.

If the system is not ready:

- proof results are misleading
- failures get misclassified
- frontend regressions and infrastructure failures blur together

That is exactly what the proof discipline is meant to avoid.

## Safe Proof Resume Rule

Only resume hosted proof when:

- `FRONTEND_UP=True`
- `BACKEND_UP=True`
- `DB_READY=True`
- `AUTH_READY=True`
- `WS_READY=True`
- `PROOF_ALLOWED=True`

## Bottom Line

The correct recovery behavior in SynapseCore is:

- classify first
- restore dependencies second
- re-check trust surfaces third
- run proof last

That keeps deployment recovery honest and operationally safe.
