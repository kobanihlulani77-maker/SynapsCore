# Render Operations Runbook

This runbook explains how to inspect, classify, and operate the live Render deployment without guessing.

## Live Render Services

Defined in `render.yaml`:

- frontend static site: `synapscore-frontend-3`
- backend web service: `synapscore-3`
- managed Postgres: `synapscore-postgres`
- managed Redis: `synapscore-redis`

Live URLs:

- frontend: `https://synapscore-frontend-3.onrender.com`
- backend: `https://synapscore-3.onrender.com`

## Service Responsibilities

### Frontend service

Responsibilities:

- serve the public homepage and authenticated SPA shell
- publish static assets
- read runtime-config and connect to the backend API and `/ws`

### Backend service

Responsibilities:

- serve all operational APIs
- serve auth/session
- serve runtime and health endpoints
- host the SockJS realtime endpoint

### Database dependency

The backend depends on PostgreSQL for:

- startup validity
- operational data
- replay records
- scenario history
- runtime trust posture

If DB is unavailable, backend readiness must not be trusted.

### Redis dependency

The backend depends on Redis in production for:

- session storage
- distributed realtime pub/sub

If Redis is unavailable, session or realtime posture can degrade or fail, and readiness may not pass.

## Live Health Endpoints

Important endpoints:

- `/`
- `/actuator/health`
- `/actuator/health/readiness`
- `/actuator/health/liveness`
- `/api/auth/session`
- `/ws/info`
- `/api/system/runtime`
- `/api/system/incidents`

## What Healthy Looks Like

Healthy live posture means:

- frontend root returns `200`
- backend health returns `200`
- liveness returns `200`
- readiness returns `200`
- auth session returns a JSON payload
- websocket info returns SockJS JSON

## What Timeout Means

If endpoints time out with no status and no body, treat that differently from a clean `503`.

Timeout interpretation:

- backend service may not be booted
- backend may be hung during startup
- backend may be blocked by DB/Redis unavailability
- the service edge may not be able to hand back a normal HTTP response yet

That is broader than a simple readiness failure.

## How DB Off Affects Backend

If DB is down or unreachable:

- readiness should fail
- startup may hang or fail
- health endpoints may not answer in time
- auth/session and websocket info may also time out

In that state, do not run hosted proof.

## How To Check Logs

Use the Render dashboard for:

- backend service logs
- deploy logs
- database status
- Redis status

Look for:

- datasource connection failures
- Flyway migration failures
- Redis connection failures
- startup hangs
- repeated readiness probe failures

## How To Redeploy

When code is already pushed and runtime should be refreshed:

- trigger a Render redeploy from the dashboard
- or let auto-deploy run from `main`

After redeploy, do not jump straight to hosted proof. Check health first.

## When To Run Hosted Proof

Run hosted proof only when:

- frontend root is reachable
- backend health responds
- backend liveness responds
- backend readiness responds
- auth session endpoint responds
- websocket info endpoint responds

Official proof commands:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1

cd frontend
npm.cmd run test:e2e:prod
```

## When Not To Run Hosted Proof

Do not run hosted proof when:

- backend endpoints time out
- readiness is not passing
- auth session does not answer
- websocket info does not answer
- database or Redis are known down

## Exact Live Connection Check Commands

```powershell
curl.exe -i --max-time 20 https://synapscore-frontend-3.onrender.com
curl.exe -i --max-time 20 https://synapscore-3.onrender.com/actuator/health
curl.exe -i --max-time 20 https://synapscore-3.onrender.com/actuator/health/readiness
curl.exe -i --max-time 20 https://synapscore-3.onrender.com/actuator/health/liveness
curl.exe -i --max-time 20 https://synapscore-3.onrender.com/api/auth/session
curl.exe -i --max-time 20 https://synapscore-3.onrender.com/ws/info
```

Or use the scripted version:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

## Classification Guide

### Frontend up, backend timeouts

Likely meaning:

- frontend deployment is live
- backend app is unavailable or startup-blocked
- do not run proof

### Backend liveness up, readiness down

Likely meaning:

- app process is alive
- DB or Redis is not ready
- do not run proof yet

### Health and readiness up, auth or ws failing

Likely meaning:

- app booted
- functional surface still degraded
- inspect auth/session or realtime-specific logs

### Everything up

Meaning:

- the system is ready for hosted proof revalidation

## Render Bottom Line

Render operations should be treated as a controlled readiness ladder:

1. frontend reachable
2. backend liveness reachable
3. backend readiness reachable
4. auth/session reachable
5. websocket info reachable
6. only then run hosted proof
