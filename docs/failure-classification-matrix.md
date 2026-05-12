# Failure Classification Matrix

This matrix helps classify failures consistently across frontend behavior, backend health, operator impact, and hosted proof impact.

| Symptom | Likely cause | Affected systems | Operator impact | Proof impact | Recovery action | Severity |
| --- | --- | --- | --- | --- | --- | --- |
| Readiness timeout | DB or Redis unavailable, or backend startup blocked | Backend, runtime trust, auth, proof | High | Stop proof | Check DB/Redis, backend logs, rerun live checks | Critical |
| Backend health timeout | Backend unavailable or hung startup | Backend, frontend data fetches, runtime | High | Stop proof | Check logs, deployment status, dependencies | Critical |
| Liveness up, readiness down | App alive but dependency posture not ready | Backend, runtime, auth may degrade | Medium to High | Stop proof | Confirm DB/Redis and readiness surfaces | High |
| Websocket reconnecting | SockJS/STOMP instability, Redis/realtime degradation, backend pressure | Frontend live state, dashboard trust | Medium | Pause realtime-dependent proof | Check `/ws/info`, Redis, runtime posture | High |
| Replay queue stuck | Replay record blocked, connector disabled, backend/replay issue | Replay queue, integrations, orders | Medium to High | Replay proof impacted | Inspect connector health, replay eligibility, runtime state | High |
| DB unavailable | Postgres down or unreachable | Backend, readiness, auth, replay, scenarios | High | Stop proof | Restore DB, wait for readiness, rerun checks | Critical |
| Connector degraded | Source system issue, connector disabled, failed imports | Integrations, replay queue, dashboard trust | Medium | Integration/replay proof may fail | Inspect integrations page, recent import history, replay queue | High |
| Runtime trust degraded | Backend dependency issue, queue pressure, auth/realtime issue | Runtime page, dashboard trust, incidents | Medium | Proof may need pause depending on exact endpoint state | Check runtime/incidents, readiness, auth, ws | High |
| Client-abort noise | Browser navigation teardown, connection closed by client | Logs only, usually not user-facing | Low | Usually none | Correlate with request IDs and failing steps before escalating | Low |
| Broken pipe warnings | Client disconnect or browser teardown | Logs only unless correlated to real failure | Low | Usually none | Treat as noise unless tied to failed requests or proof | Low |
| CORS failures | Origin mismatch, error-path CORS inconsistency, backend edge issue | Browser fetches, frontend pages | Medium to High | Browser proof blocked | Inspect response headers, backend error paths, allowed origins | High |
| Replay visibility mismatch | UI stale, replay state drift, backend/replay readback issue | Replay queue page, operator trust | Medium | Replay proof impacted | Compare backend replay state and UI detail panel behavior | High |
| Stale runtime snapshot | Backend unavailable, snapshot cache lag, realtime degradation | Runtime page, dashboard trust | Medium | Runtime-related proof may fail | Check runtime endpoint, readiness, websocket posture | Medium |
| Slow connector telemetry | Heavy connector summary path, backend load, upstream pressure | Integrations page, replay/operator clarity | Low to Medium | Integration proof may slow or fail | Inspect connector endpoint timings, recent import history, runtime pressure | Medium |
| Auth session timeout | Backend unavailable, session/Redis issue, startup blocked | Sign-in flow, authenticated shell | High | Stop proof | Check `/api/auth/session`, readiness, Redis posture | Critical |
| Frontend live but backend unavailable | Static deploy healthy, backend unhealthy | Public shell, all backend-driven pages | High | Stop proof | Classify backend state; do not blame frontend deploy first | Critical |

## Severity Interpretation

- `Low`
  - informational or log-noise until correlated to real failure
- `Medium`
  - degraded user experience or partial operational trust loss
- `High`
  - meaningful operational impairment, do not trust some product paths
- `Critical`
  - hosted proof must stop and recovery must focus on infrastructure or backend trust

## Usage Rule

Use this matrix to avoid misclassifying:

- infrastructure failures as frontend regressions
- harmless log noise as incidents
- replay or realtime drift as “just slow”

SynapseCore reliability depends on correct classification before repair.
