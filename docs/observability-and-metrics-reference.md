# Observability And Metrics Reference

This document explains how SynapseCore exposes runtime truth, operational telemetry, incidents, health, and metrics.

It is a reference for operators, reviewers, and developers who need to understand what the system is saying when it is healthy, degraded, or blocked.

## Observability Philosophy

SynapseCore treats runtime truth as part of the product.

That means:

- degraded states should be visible
- replay and connector failures should not be hidden
- readiness matters more than static frontend availability
- proof should pause when runtime trust is missing
- operators should see enough detail to classify incidents

## Main Runtime Surfaces

| Surface | Purpose | Typical Consumer |
|---|---|---|
| `/actuator/health` | General health | scripts, operators |
| `/actuator/health/liveness` | App process alive | infrastructure |
| `/actuator/health/readiness` | App ready for traffic | proof gate, operators |
| `/api/auth/session` | session/auth posture | frontend, proof |
| `/ws/info` | SockJS/websocket availability | frontend, proof |
| `/api/system/runtime` | runtime trust summary | runtime page, reviewers |
| `/api/system/incidents` | active operational incidents | operators |
| `/api/dashboard/snapshot` | full operational snapshot | frontend, proof |
| `/actuator/prometheus` | metrics scrape target when explicitly exposed outside production | internal metrics stack |

## Health vs Readiness

Liveness means the app process is alive.

Readiness means the app should be able to serve trusted traffic.

Important distinction:

- liveness can work while readiness fails
- readiness failure should block hosted proof
- frontend availability does not prove backend readiness
- backend startup without DB trust is not enough for operational classification

## Runtime Summary

`GET /api/system/runtime` is the broad runtime trust endpoint.

It includes:

- application name
- active profiles
- build identity
- liveness state
- readiness state
- CORS/session posture
- telemetry summary
- backbone/realtime posture
- metrics summary
- diagnostics summary
- connector diagnostics

Use it when:

- a reviewer asks what the running system believes about itself
- readiness is technically up but operators see degraded behavior
- connector or replay state looks suspicious
- proof failed and needs classification

## Telemetry Signals

Runtime telemetry can include:

- disabled connector count
- replay queue depth
- dead-letter queue depth
- recent import issues
- recent inbound rejections
- recent audit failures
- active alert count
- fulfillment backlog count
- delayed fulfillment count
- dispatch queue depth
- failed dispatch count

Interpretation:

- `replayQueueDepth > 0` means recovery work is visible and pending
- `deadLetterQueueDepth > 0` means manual intervention is likely needed
- disabled connectors may be intentional or operationally dangerous
- recent inbound rejections indicate integration pressure
- failed dispatch count indicates runtime fan-out/backbone pressure

## Runtime Incidents

`GET /api/system/incidents` collects operator-facing incident signals.

Incident families can include:

- audit failures
- inbound rejections
- replay backlog
- connector disabled/degraded
- dispatch failures
- control notices

Incidents are not only errors. Some are action-required operational facts.

## Dashboard Snapshot

`GET /api/dashboard/snapshot` is the full operational snapshot endpoint.

It supports:

- dashboard visibility
- realtime fallback validation
- proof assertions
- operator state reconstruction

If websocket updates are delayed, the snapshot gives the frontend a trusted REST view.

## Websocket Observability

The frontend expects realtime updates through the backend websocket/SockJS layer.

Key check:

```powershell
curl.exe -i http://localhost:8080/ws/info
```

Live:

```powershell
curl.exe -i https://synapscore-3.onrender.com/ws/info
```

Healthy websocket posture means:

- `/ws/info` responds
- frontend can connect
- realtime indicators can leave reconnecting/waiting state
- proof can validate realtime behavior

## Prometheus Metrics

`/actuator/prometheus` is intentionally not part of the anonymous production HTTP surface.

In production, public actuator exposure is limited to the health endpoint family:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`

Metrics instrumentation remains useful, but production scraping should be added through a private, authenticated, or otherwise controlled monitoring path before a pilot depends on it.

Current docs should not claim a complete enterprise metrics stack unless one is actually deployed and operated.

Use metrics as:

- an internal instrumentation surface
- a future observability integration point
- a local/reviewer check for instrumented runtime posture

Do not confuse endpoint presence with:

- managed alerting
- long-term metrics storage
- dashboards
- tracing
- SLO enforcement

## Logs

Backend logs are still important for:

- startup validation
- Flyway migration posture
- DB/Redis connection problems
- replay automation behavior
- scheduler failures
- CORS/session issues
- unexpected exceptions

Docker local logs:

```powershell
docker logs synapse_backend --tail 120
```

Compose logs:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\infrastructure
docker compose logs --tail 120
```

## Proof-Relevant Observability

Hosted proof should require:

- frontend reachable
- backend reachable
- readiness up
- auth/session responding
- websocket info responding
- proof tenant and proof user behavior working
- dashboard snapshot responding

The live connection script classifies this:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Only `PROOF_ALLOWED=True` should unlock hosted proof.

## Common Interpretations

| Signal | Meaning | Action |
|---|---|---|
| Frontend `200`, backend timeout | Static shell is alive, operational backend is not trusted | Pause proof |
| Liveness up, readiness down | App exists but dependency posture is not ready | Check DB/Redis/logs |
| Auth fails | Session or backend trust issue | Check backend, CORS, Redis/session |
| `/ws/info` fails | Realtime layer unavailable | Pause realtime proof |
| Replay queue grows | Inbound failures are waiting | Review connector and replay queue |
| Dead-letter grows | Automated/manual recovery exhausted | Escalate to operator review |
| Runtime incidents present | System has visible operational pressure | Classify before proving |

## Current Limitations

Current honest limitations:

- tracing is not yet a mature distributed tracing stack
- metrics endpoint exists, but full enterprise metrics operations still need hardening
- alert routing and escalation outside the app should be expanded for large enterprise use
- SLO dashboards and error budgets are roadmap items

## Related Docs

- [runtime-observability.md](runtime-observability.md)
- [operations-reliability.md](operations-reliability.md)
- [failure-classification-matrix.md](failure-classification-matrix.md)
- [proof-and-validation.md](proof-and-validation.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)

## Bottom Line

SynapseCore observability is strongest when it is used as a truth system: classify runtime state first, recover second, prove last.
