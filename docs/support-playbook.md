# Support Playbook

This playbook helps support engineers classify and respond to SynapseCore issues during pilot and operational use.

Support should classify first, restore second, and prove last.

## First Response Sequence

Run:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Classify:

- frontend reachable?
- backend reachable?
- readiness passing?
- auth/session responding?
- websocket info responding?
- affected tenant known?
- affected operator known?
- affected page/flow known?

Preserve evidence before making changes.

## Login Failures

Common causes:

- wrong tenant code
- wrong username/password
- expired or rotated password
- auth rate limit
- backend unavailable
- session cookie/CORS issue

Checks:

- `/api/auth/session`
- live connection gate
- browser error text
- rate-limit cooldown state if hosted proof recently ran

Response:

- do not reset passwords blindly
- verify tenant code and username
- wait for rate-limit cooldown if triggered
- escalate if valid credentials fail with readiness healthy

## Readiness Failures

Likely causes:

- DB unavailable
- migration/startup issue
- connection pool pressure
- backend startup hang
- dependency failure

Checks:

- `/actuator/health`
- `/actuator/health/readiness`
- `/actuator/health/liveness`
- backend logs
- DB availability

Response:

- do not run hosted proof
- classify DB vs backend app failure
- restore dependency first
- rerun live connection gate
- run proof only after `PROOF_ALLOWED=True`

## Websocket Failures

Likely causes:

- Redis/session dependency issue
- websocket endpoint unavailable
- browser/network interruption
- degraded realtime fallback

Checks:

- `/ws/info`
- runtime page
- dashboard realtime state
- browser console
- Redis availability if local/self-hosted

Response:

- confirm snapshot data still loads
- classify as realtime degraded rather than total outage when HTTP is healthy
- restore Redis/websocket path
- rerun realtime proof only after readiness is healthy

## Replay Issues

Likely causes:

- connector disabled
- failed inbound record not eligible
- replay already processed
- replay lock/contention
- malformed payload
- proof assumption drift

Checks:

- replay queue page
- integration connector page
- failed inbound reason
- backend replay response
- audit/business events

Response:

- do not manually edit replay rows
- repair connector or payload first
- replay intentionally
- preserve request ID and result
- rerun replay proof if behavior changed

## Approval Issues

Likely causes:

- role does not allow approval
- scenario status is not approval-ready
- scenario was rejected
- scenario already executed
- approval UI selector drift

Checks:

- approvals page
- scenario history
- signed-in roles
- scenario action console
- backend scenario response

Response:

- verify role boundary
- do not bypass approval in DB
- confirm the status transition
- rerun scenario proof if approval behavior changes

## Connector Failures

Likely causes:

- connector disabled intentionally
- unsupported connector mode
- malformed CSV/webhook payload
- missing support owner
- scheduled pull worker issue

Checks:

- integrations page
- connector policy
- import run
- failed inbound record
- replay queue

Response:

- classify connector failure code
- decide whether to repair source data or connector state
- replay only after repair
- document unsupported modes honestly

## Runtime Degradation

Likely causes:

- backend dependency issue
- DB/Redis pressure
- runtime incident
- Render cold start or restart
- websocket degraded state

Checks:

- runtime page
- health/readiness/liveness
- dashboard snapshot
- incidents
- live connection gate

Response:

- preserve incident context
- classify dependency vs application issue
- pause proof if trust gates are unhealthy
- resume only after readiness/auth/ws are healthy

## Deployment Issues

Likely causes:

- Render env drift
- DB credential issue
- missing Redis
- missing bootstrap/platform admin token
- Flyway migration failure
- CORS origin mismatch

Checks:

- Render env vars
- Render logs
- `render.yaml`
- health endpoints
- auth/session endpoint
- `/ws/info`

Response:

- do not edit production DB manually
- fix env/deployment seam
- redeploy if needed
- rerun live connection gate
- rerun proof only if runtime behavior or proof-covered flows changed

## Recovery Sequence

Use this order:

1. preserve evidence
2. classify failure
3. restore dependency or service
4. verify health/readiness/auth/ws
5. verify affected page/API
6. rerun targeted proof if needed
7. rerun full hosted proof only when justified
8. update evidence/status docs if trust posture changed

## Escalation Triggers

Escalate immediately for:

- tenant isolation concern
- security or secret concern
- unexplained data corruption
- replay inconsistency
- readiness failure during pilot window
- repeated auth/session failure
- production DB migration failure
