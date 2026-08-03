# Scripts Reference

This document explains the most important operational and verification scripts in the SynapseCore repository.

The goal is to help contributors and operators understand:

- what each script does
- when to run it
- whether it changes anything
- whether it depends on backend, DB, or live services

All scripts listed here are informational or verification-oriented unless explicitly noted otherwise in their own docs.

## `scripts\check-live-connections.ps1`

Purpose:

- checks live frontend and backend connection posture

What it checks:

- frontend URL
- backend health
- backend readiness
- backend liveness
- auth session endpoint
- websocket info endpoint

Outputs:

- `FRONTEND_UP`
- `BACKEND_UP`
- `DB_READY`
- `AUTH_READY`
- `WS_READY`
- `PROOF_ALLOWED`

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- yes, for meaningful success

## `scripts\check-local-connections.ps1`

Purpose:

- checks local frontend, backend, ports, and Docker posture

What it checks:

- local frontend, usually `http://127.0.0.1:5173`
- local backend health/readiness/auth/ws, usually `http://127.0.0.1:8080`
- ports `5173`, `8080`, `5432`, `6379`
- Docker Compose service posture when available
- TCP connectability for port posture, because Windows port enumeration can be misleading

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- only for healthy results

Recommended Windows local command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-local-connections.ps1 -FrontendUrl http://127.0.0.1:5173 -BackendUrl http://127.0.0.1:8080
```

## `scripts\explain-system.ps1`

Purpose:

- prints a concise system overview for local and live usage

Includes:

- services
- ports
- live URLs
- key docs
- proof commands
- local dev credentials

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## `scripts\explain-infrastructure.ps1`

Purpose:

- prints the infrastructure model and communication chain

Includes:

- service map
- local/live URLs
- proof command path
- current status assumptions

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## `scripts\explain-proof-system.ps1`

Purpose:

- prints the proof philosophy and proof command sequence

Includes:

- local verify commands
- hosted proof commands
- readiness expectations
- proof blockers
- when not to run proof

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## `scripts\recovery-checklist.ps1`

Purpose:

- prints the recovery sequence for local and live incidents

Includes:

- live checks
- local checks
- readiness/auth/ws endpoints
- proof rerun order
- when not to run proof

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## `scripts\project-map.ps1`

Purpose:

- prints a concise repository and roadmap map

Includes:

- major repo sections
- major docs
- frontend/backend paths
- infra paths
- proof commands
- operational scripts

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## `scripts\env-sanity-check.ps1`

Purpose:

- checks env template and config-reference posture

Checks:

- key env files exist
- key frontend and backend template variables exist
- local example guidance files are present

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## `scripts\docs-link-check.ps1`

Purpose:

- scans `README.md` and `docs/` markdown files for broken local links

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## `scripts\script-help.ps1`

Purpose:

- prints a categorized quick-help map for the most important scripts

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## `scripts\repo-health.ps1`

Purpose:

- checks repository cleanliness and signal-to-noise posture

Checks:

- git status
- local env files
- artifact directories
- frontend verify availability
- key docs
- key scripts

Outputs:

- `CLEAN`
- `LOCAL_ONLY_FILES_PRESENT`
- `ARTIFACTS_PRESENT`
- `NEEDS_ATTENTION`

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## `scripts\prepare-hosted-proof.ps1`

Purpose:

- prepares the hosted proof environment before Playwright runs
- generates proof tenant/operator values when absent
- writes ignored proof state to `frontend\.hosted-proof\hosted-proof-state.json`

Use when:

- live readiness/auth/ws posture is healthy
- a new replacement database needs the proof tenant recreated through supported APIs

Safe to run anytime:

- only when proof should actually proceed

Changes anything:

- environment/preparation behavior, but not product runtime logic

Requires backend or DB:

- yes, because hosted proof depends on them

Notes:

- `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN` is required only when the target production database is empty and the first tenant must be created.
- Playwright reads generated proof values from the ignored proof state file, so proof users/passwords do not need to be invented manually.

## `scripts\verify-deployment.ps1`

Purpose:

- local or environment deployment verification helper

Use when:

- checking whether frontend and backend are coherently reachable
- running the local/self-host smoke after frontend and backend are both up

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- for meaningful success, yes

Recommended Windows local command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-deployment.ps1 -FrontendUrl http://127.0.0.1:5173 -BackendUrl http://127.0.0.1:8080
```

## `scripts\verify-realtime.ps1`

Purpose:

- checks realtime connectivity posture

Use when:

- validating local or live realtime behavior
- confirming the dashboard can update through realtime without a browser refresh

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- yes, for meaningful success

Recommended Windows local command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-realtime.ps1 -FrontendUrl http://127.0.0.1:5173 -BackendUrl http://127.0.0.1:8080
```

## `frontend\scripts\frontend-check.mjs`

Purpose:

- frontend launch-readiness and policy check used by `npm.cmd run verify`

Checks:

- missing required docs
- proof-critical labels
- direct console logging
- debugger statements
- TODO or FIXME markers in launch-sensitive areas

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## Which Scripts To Run First

For repo hygiene:

- `scripts\repo-health.ps1`
- `scripts\docs-link-check.ps1`

For local stack understanding:

- `scripts\explain-system.ps1`
- `scripts\check-local-connections.ps1 -FrontendUrl http://127.0.0.1:5173 -BackendUrl http://127.0.0.1:8080`

For live deployment understanding:

- `scripts\explain-infrastructure.ps1`
- `scripts\check-live-connections.ps1`

For proof readiness:

- `scripts\explain-proof-system.ps1`
- `scripts\check-live-connections.ps1`
- `scripts\prepare-hosted-proof.ps1`

For release and env posture:

- `scripts\env-sanity-check.ps1`
- `scripts\check-prod-config.ps1`
- `scripts\release-readiness.ps1`
- `scripts\script-help.ps1`

For recovery posture:

- `scripts\recovery-checklist.ps1`

## Bottom Line

The scripts are meant to reduce guesswork, not replace judgment.

Use the checkers to classify reality first, then use the docs and runbooks to decide what to do next.
