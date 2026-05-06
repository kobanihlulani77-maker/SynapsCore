# Runtime And Observability Guide

This guide describes the trust surfaces operators should use to understand the live state of SynapseCore.

## Primary Trust Surfaces

SynapseCore exposes these core operational surfaces:

- `/`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/api/system/runtime`
- `/api/system/incidents`
- `/actuator/prometheus`

## Safe Root Response

The backend root `/` should return a safe service-status response.

Use it for:

- a basic "is the service answering?" probe
- quick human verification that the backend is serving a safe status payload instead of an unexpected `500`

## Health And Readiness

Use:

- liveness for platform health checks
- readiness before proof traffic or operator traffic that depends on database, Redis, and application readiness

Current readiness coverage includes:

- `readinessState`
- `db`
- `redis`
- `ping`

## Runtime Snapshot

`GET /api/system/runtime` is the authoritative runtime summary for the control center.

It should be used to confirm:

- build version and commit
- active profile
- header fallback posture
- secure cookie posture
- replay queue depth
- connector health and recent failures
- dispatch backlog and failure posture
- alert count and active incident count
- business-event and failure windows
- realtime broker mode

## Build Identity

Backend runtime identity can resolve from:

- `SYNAPSECORE_BUILD_COMMIT`
- Render's `RENDER_GIT_COMMIT`

That means runtime is the best live source for confirming which backend commit the deployment is actually serving.

## Incident Inbox

`GET /api/system/incidents` is the right place to understand active operational degradation such as:

- disabled connectors
- degraded connectors
- replay backlog pressure
- audit failure spikes
- failed dispatch work
- escalation-relevant control notices

## Metrics

The Prometheus surface should be used for deeper operational trending.

Important families include:

- auth failure and rate-limit metrics
- integration import runs
- replay attempts and backlog
- fulfillment backlog and delays
- dispatch queued, processed, and failed counts
- inventory lock-conflict signals

## Operational Noise Classification

Some logs are noise, not product breakage.

Treat these as `OPERATIONAL NOISE` when they happen during browser disconnects, navigation teardowns, or client-side aborts:

- `Broken pipe`
- `ClientAbortException`
- connection reset by peer
- established connection aborted

Those logs become real incidents only when:

- they line up with a failing proof step
- they line up with a user-visible error
- they share a requestId with a real API failure

## When To Escalate

Escalate immediately when:

- readiness is `UP` but the hosted proof lane is still failing
- runtime reports replay or connector pressure that operators cannot explain
- manual replay records disappear without a real recovery action
- auth rate limiting or session behavior changes from the proven hosted path

When runtime truth and hosted proof disagree, trust the hosted proof and investigate the gap.
