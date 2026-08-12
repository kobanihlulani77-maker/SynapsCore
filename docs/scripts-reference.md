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

## `scripts\engineering-readiness.ps1`

Purpose:

- checks whether the repository is in engineering-ready shape after the Pilot Release Candidate milestone

What it checks:

- local repository alignment with `origin/main`
- uncommitted production changes
- tracked risky artifacts or env files
- engineering, release, proof, and operational documentation presence
- required operational scripts
- replacement-database hosted proof evidence
- docs link health
- frontend verify
- live connection posture when not skipped

Outputs:

- `ENGINEERING_READY=True`
- `ENGINEERING_READY=False`
- blockers and warnings

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- yes for full readiness, because live connection readiness is part of the gate
- no if run with `-SkipLiveCheck`, but that is not a full engineering-ready result

Recommended command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\engineering-readiness.ps1
```

## `scripts\evolution-check.ps1`

Purpose:

- summarizes whether the repository has the product evolution foundation needed for evidence-driven future work

What it checks:

- current release marker
- hosted proof evidence presence
- product evolution docs
- product knowledge and engineering foundation docs
- pilot release candidate documentation
- tracked risky artifact or env files
- uncommitted production changes
- outstanding improvement source docs

Outputs:

- `EVOLUTION_READY=True`
- `EVOLUTION_READY=False`
- blockers and warnings

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

Recommended command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\evolution-check.ps1
```

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

## `frontend\scripts\control-inventory.mjs`

Purpose:

- regenerates the authoritative source-level interactive control inventory for Pre-Pilot Gate 4

Checks:

- buttons
- inputs
- selects
- textareas
- checkboxes and radios
- forms
- role/button surfaces
- non-semantic interactive surfaces with handlers
- route/component ownership
- visible labels and selector hints
- static handler traces

Safe to run anytime:

- yes

Changes anything:

- no runtime behavior changes; it writes local report artifacts under `frontend\test-results\control-inventory\`

Requires backend or DB:

- no

Recommended command:

```powershell
cd frontend
npm.cmd run test:controls:inventory
```

Notes:

- generated artifacts should not be committed
- see [pre-pilot-gate-4-control-verification.md](pre-pilot-gate-4-control-verification.md) for current Gate 4 accounting

## `frontend\scripts\pilot-load-check.mjs`

Purpose:

- runs the controlled pre-pilot load, concurrency, resource, and realtime proof used by `npm.cmd run test:load:pilot`

Checks:

- authenticated HTTP read concurrency
- controlled mutation traffic
- backend readiness during load
- actuator resource metrics when available
- Hikari pool pressure when available
- SockJS/STOMP connection establishment
- realtime dashboard event delivery
- dataset counts and basic integrity after load

Safe to run anytime:

- no, run only against an authorized local or explicitly approved test environment

Changes anything:

- yes, it authenticates, creates proof traffic, and can create test catalog/order data through supported APIs

Requires backend or DB:

- yes

Recommended local command:

```powershell
cd frontend
npm.cmd run test:load:pilot -- --stages 1,3,5,10,15,25 --wsStages 1,5,10,25,50 --durationSeconds 60 --warmupSeconds 10 --soakSeconds 300 --mutationUsers 1 --loginPauseMs 7000
```

Notes:

- raw JSON output is written under `frontend\test-results\pilot-load\`
- do not commit raw load artifacts
- do not run this script against live Render unless the environment owner has explicitly approved a load window
- see [performance-scale-proof.md](performance-scale-proof.md) for the accepted Gate 3 evidence and limitations

## `scripts\product-knowledge-check.ps1`

Purpose:

- verifies that the canonical product-knowledge, industry, role-guide, and official pilot-program documents are present and linked from the main navigation

What it checks:

- product knowledge base
- operational concepts
- SynapseCore dictionary
- business process library
- executive, operator, warehouse, IT, and solution architect guides
- industry guides
- official pilot program
- pilot evidence templates
- README, docs index, and documentation map links

Outputs:

- `PRODUCT_KNOWLEDGE_READY`
- `READY_WITH_WARNINGS`
- `NEEDS_ATTENTION`
- blockers and warnings

Safe to run anytime:

- yes

Changes anything:

- no

Requires backend or DB:

- no

## Which Scripts To Run First

For repo hygiene:

- `scripts\repo-health.ps1`
- `scripts\evolution-check.ps1`
- `scripts\product-knowledge-check.ps1`
- `scripts\engineering-readiness.ps1`
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
