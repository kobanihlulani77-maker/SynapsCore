# Render Recovery Playbook

This playbook explains how to interpret and recover the live Render deployment when frontend, backend, DB, Redis, readiness, or websocket behavior degrades.

## Render Service Map

Current live services:

- frontend static site: `synapscore-frontend-3`
- backend web service: `synapscore-3`
- managed Postgres: `synapscore-postgres`
- managed Redis: `synapscore-redis`

Live URLs:

- frontend: `https://synapscore-frontend-3.onrender.com`
- backend: `https://synapscore-3.onrender.com`

## Render Backend Behavior

The backend is a dependency-heavy live service. It depends on:

- PostgreSQL
- Redis
- successful startup
- healthy readiness posture

That means a backend timeout can imply:

- backend not booted
- backend startup hung
- DB unavailable
- Redis unavailable
- service edge waiting on a blocked startup path

## Cold Starts

Render cold starts can slow early requests, especially on lower-tier hosting.

What that means operationally:

- do not treat the very first slow request as proof of platform failure
- but do not treat repeated readiness or auth/ws timeouts as acceptable noise either

A slow warm-up can be normal.
A repeated no-response health/auth/ws pattern is not proof-ready.

## DB-Off Behavior

If the managed database is off or unreachable:

- readiness should fail
- backend health may time out
- auth/session may time out
- websocket info may time out
- hosted proof must stop

This is not just a database problem. It is a system trust problem.

## Redis Impact

If Redis is degraded or unavailable:

- readiness may fail
- auth/session may degrade
- websocket and realtime trust may degrade
- distributed session and pub/sub posture may no longer be safe

## What Timeouts Mean

Timeouts with no status and no body usually imply something broader than a clean application error.

Likely interpretations:

- backend startup blocked
- dependency unavailable
- service not responding at the edge yet
- app not healthy enough to answer

This is different from a structured `503` response.

## What Suggests Hung Startup

Signs that suggest the backend is hung or blocked at startup:

- health endpoint times out
- readiness times out
- liveness times out
- auth/session times out
- websocket info times out

When all of those happen together, do not treat it as a narrow frontend or selector issue.

## When To Redeploy

Redeploy when:

- new code was already pushed and runtime should refresh
- logs suggest the backend is stuck in a bad deploy state
- dependencies have recovered and the service still does not resume healthy behavior

## When NOT To Redeploy

Do not redeploy reflexively when:

- DB is still known down
- Redis is still known down
- the platform has not yet been classified

Fixing dependencies first may be the real step.

## Cache-Clear Posture

Render frontend static caching can make the frontend shell appear healthy even while the backend is unhealthy.

Interpretation:

- cached frontend HTML proving `200` is not proof that the backend recovered
- always check backend endpoints separately

## Proof Rerun Sequence After Recovery

Only after recovery:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1

cd frontend
npm.cmd run test:e2e:prod
```

Do not skip the live check step.

## Exact Health Endpoints

Use:

- `https://synapscore-3.onrender.com/actuator/health`
- `https://synapscore-3.onrender.com/actuator/health/readiness`
- `https://synapscore-3.onrender.com/actuator/health/liveness`
- `https://synapscore-3.onrender.com/api/auth/session`
- `https://synapscore-3.onrender.com/ws/info`

## Curl Checks

```powershell
curl.exe -i --max-time 20 https://synapscore-frontend-3.onrender.com
curl.exe -i --max-time 20 https://synapscore-3.onrender.com/actuator/health
curl.exe -i --max-time 20 https://synapscore-3.onrender.com/actuator/health/readiness
curl.exe -i --max-time 20 https://synapscore-3.onrender.com/actuator/health/liveness
curl.exe -i --max-time 20 https://synapscore-3.onrender.com/api/auth/session
curl.exe -i --max-time 20 https://synapscore-3.onrender.com/ws/info
```

## What Healthy Responses Should Look Like

Healthy enough for proof:

- frontend root returns `200`
- health returns `200`
- liveness returns `200`
- readiness returns `200`
- auth/session returns JSON
- websocket info returns SockJS JSON

## What Unhealthy Responses Imply

### Frontend `200`, backend timeouts

Implication:

- frontend deploy is healthy
- backend is unavailable or startup-blocked

### Liveness `200`, readiness fails

Implication:

- app process alive
- dependencies or readiness posture still broken

### Auth/session or ws fail while health is up

Implication:

- functional browser-facing trust is degraded
- proof should still pause until fixed

## Bottom Line

Render recovery should follow one rule:

classify first, restore second, prove last.
