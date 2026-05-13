# Troubleshooting Index

This document is the routing page for troubleshooting SynapseCore.

Use it when something is wrong and you need to quickly find:

- the right script
- the right runbook
- the right deep-reference doc

## Start With Classification

First question:

is this a frontend issue, backend issue, dependency issue, proof issue, or local setup issue?

Useful first checks:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\repo-health.ps1
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-local-connections.ps1
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

## Frontend Opens But Login Fails

Likely areas:

- backend unavailable
- auth session issue
- workspace code misunderstanding
- CORS or cookie posture issue

Read:

- [frontend-flow.md](frontend-flow.md)
- [environment-reference.md](environment-reference.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)

Run:

- `scripts/check-local-connections.ps1`
- `scripts/check-live-connections.ps1`

## Backend Health Times Out

Likely areas:

- DB unavailable
- Redis unavailable
- backend startup hung
- hosting/runtime issue

Read:

- [deployment-recovery-guide.md](deployment-recovery-guide.md)
- [render-recovery-playbook.md](render-recovery-playbook.md)
- [database-and-migrations.md](database-and-migrations.md)

Run:

- `scripts/check-live-connections.ps1`
- `scripts/recovery-checklist.ps1`

## Readiness Fails But Liveness Works

Likely meaning:

- backend process is alive
- dependencies or startup trust are not ready

Read:

- [operations-reliability.md](operations-reliability.md)
- [failure-classification-matrix.md](failure-classification-matrix.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)

## Websocket Reconnecting Or Stale Live State

Likely areas:

- websocket endpoint issue
- Redis or realtime posture issue
- snapshot still available but live trust degraded

Read:

- [system-communication-map.md](system-communication-map.md)
- [operations-reliability.md](operations-reliability.md)
- [operator-incident-guide.md](operator-incident-guide.md)

Run:

- `scripts/check-local-connections.ps1`
- `scripts/check-live-connections.ps1`
- `scripts/verify-realtime.ps1`

## Replay Queue Looks Wrong Or Replay Stalls

Likely areas:

- connector degraded or disabled
- backend availability problem
- replay visibility mismatch
- proof blocked by unhealthy runtime

Read:

- [replay-recovery.md](replay-recovery.md)
- [failure-classification-matrix.md](failure-classification-matrix.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)

## Postgres Password Fails Locally

Likely areas:

- Docker Postgres vs Windows Postgres conflict
- wrong host target
- stale password assumption

Read:

- [local-runbook.md](local-runbook.md)
- [local-recovery-playbook.md](local-recovery-playbook.md)
- [database-and-migrations.md](database-and-migrations.md)

## Port 8080 Already In Use

Likely areas:

- backend container already using `8080`
- host backend already running

Read:

- [infrastructure-handbook.md](infrastructure-handbook.md)
- [local-runbook.md](local-runbook.md)

## Render Frontend Is Live But Backend Times Out

Meaning:

- frontend shell is reachable
- backend trust is not established
- hosted proof should pause

Read:

- [render-recovery-playbook.md](render-recovery-playbook.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)
- [proof-and-validation.md](proof-and-validation.md)

Run:

- `scripts/check-live-connections.ps1`

## Hosted Proof Should Run Or Not?

Run:

- `scripts/check-live-connections.ps1`
- `scripts/explain-proof-system.ps1`

Read:

- [proof-and-validation.md](proof-and-validation.md)
- [hosted-proof.md](hosted-proof.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)

If readiness, auth, or websocket trust are down, do not run hosted proof.

## Environment Drift

Likely areas:

- wrong local env file
- wrong prod env template usage
- wrong API or WS URLs
- placeholder values left in env files

Run:

- `scripts/env-sanity-check.ps1`
- `scripts/check-prod-config.ps1`
- `scripts/release-readiness.ps1`

Read:

- [environment-reference.md](environment-reference.md)
- [release-process.md](release-process.md)

## Docs Or Scripts Confusion

Read:

- [documentation-map.md](documentation-map.md)
- [scripts-reference.md](scripts-reference.md)

Run:

- `scripts/script-help.ps1`
- `scripts/project-map.ps1`

## Bottom Line

Troubleshooting should follow the same discipline as proof:

- classify first
- inspect the right layer
- use the right script
- use the matching runbook
- do not guess when the system is telling you something important
