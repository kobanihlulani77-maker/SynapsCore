# Engineering Review

This review captures the engineering state of SynapseCore after the pilot release candidate milestone.

It is based on the current repository structure, proof tooling, scripts, documentation, frontend, backend, infrastructure, and deployment posture. It does not propose product scope expansion.

## Current Baseline

Current proven state:

- hosted proof passed against the replacement Render PostgreSQL database
- result: `6 passed (4.1m)`
- pilot release candidate check passed
- current pilot RC proposal: `v0.9.0-pilot-rc1`
- live connection gate reports `PROOF_ALLOWED=True`
- frontend verify passes
- docs link check passes

## Review Scope

Reviewed areas:

- `frontend`
- `backend`
- `infrastructure`
- `scripts`
- `docs`
- deployment configuration
- testing and proof tooling
- operational runbooks
- repository hygiene

## Strengths

### Hosted proof discipline

The strongest engineering signal is the proof lane.

Evidence:

- `scripts\prepare-hosted-proof.ps1`
- `frontend\playwright.prod.config.mjs`
- `frontend\tests\prod-proof.spec.mjs`
- `docs\release-evidence-2026-08-03.md`

The proof validates real deployed frontend/backend behavior, not mock-only behavior.

### Runtime truth surfaces

The platform has explicit live trust surfaces:

- `/actuator/health`
- `/actuator/health/readiness`
- `/actuator/health/liveness`
- `/api/auth/session`
- `/ws/info`
- runtime page
- dashboard snapshot
- realtime state

This supports operational honesty.

### Backend modularity

The backend is organized into understandable packages:

- `access`
- `api.controller`
- `auth`
- `tenant`
- `domain`
- `integration`
- `scenario`
- `realtime`
- `event`
- `observability`
- `security`
- `config`

Controllers are generally separated from services and repositories, which supports maintainability.

### Frontend structure

The frontend separates:

- pages
- layout
- hooks
- services
- config
- reusable components
- proof tests

That makes the command-center UI easier to extend without mixing all behavior into one file.

### Documentation depth

The documentation set now covers:

- architecture
- local and Render operations
- proof
- recovery
- pilot release candidate
- limitations
- market and buyer context
- engineering discipline

The risk is no longer missing docs. The risk is keeping docs current.

## Technical Debt

### Documentation ownership and duplication

There are many strong documents. Some overlap is intentional, but ownership must now be explicit.

Risk:

- future contributors may update one doc and leave related docs stale

Recommended improvement:

- maintain `docs\documentation-map.md` and `docs\INDEX.md` as routing truth
- update proof/status docs whenever hosted proof or deployment status changes
- periodically retire or merge older narrative docs when they become redundant

### Script surface growth

The `scripts` folder has many explainers, checkers, deployment helpers, and proof helpers.

Risk:

- contributors may not know which script is authoritative
- similar scripts can drift

Recommended improvement:

- keep `docs\scripts-reference.md` and `scripts\script-help.ps1` authoritative
- prefer adding modes to existing scripts instead of creating new adjacent scripts
- label scripts as informational, verification, mutating, or deployment-affecting

### Local artifact noise

Common local artifacts are present:

- `frontend\playwright-report\`
- `frontend\test-results\`
- `frontend\.hosted-proof\`
- `frontend\proof-run-archive.zip`
- `frontend\dist\`
- backend logs and target outputs

Risk:

- accidental staging
- reviewer confusion

Recommended improvement:

- do not commit local proof archives unless an evidence-storage policy exists
- consider adding `frontend/proof-run-archive.zip` to `.gitignore` if it remains a repeated local artifact

### Proof state sensitivity

Hosted proof state is intentionally local and ignored.

Risk:

- generated proof passwords are sensitive
- deleting `.hosted-proof` requires proof state regeneration

Recommended improvement:

- keep proof state out of Git
- document recovery paths for lost proof state
- avoid putting proof passwords in terminal transcripts or docs

### Frontend selector sensitivity

Frontend productization can drift proof selectors.

Evidence:

- hosted proof needed updates for the Alerts response panel and Company Settings label

Recommended improvement:

- treat proof-critical labels and IDs as compatibility surfaces
- when redesigning pages, run isolated proof selectors before the full hosted proof

### Configuration drift

There are multiple environment paths:

- local Docker
- host frontend/backend
- full Docker compose
- Render frontend/backend
- proof env/state

Risk:

- `localhost` vs `127.0.0.1`
- host Postgres vs Docker Postgres
- proof token confusion

Recommended improvement:

- keep `docs\infrastructure-handbook.md` and `docs\environment-reference.md` current
- avoid adding new env files unless necessary

### Generated and visual artifacts

The repo contains some visual evidence files and generated company-fit reports.

Risk:

- unclear distinction between source docs, generated reference artifacts, and local outputs

Recommended improvement:

- define an evidence artifact policy before committing future archives or screenshots
- keep generated docs under `docs\generated` when intentionally retained

## Dead Code And Obsolete Helpers

No runtime dead-code removal is recommended in this phase.

Areas to review later:

- older shell helper scripts that overlap with PowerShell helpers
- legacy/demo language in older docs
- visual PNGs in `frontend` if not part of a deliberate evidence policy
- older generated reports if company-fit evidence is no longer needed in the main repo

Do not delete these casually. Classify first.

## Maintainability Concerns

Main concerns:

- docs and scripts require ownership discipline
- proof selectors must be treated as stable contracts
- environment handling is complex because local, Docker, Render, and proof paths all exist
- backend service count is growing, so package boundaries need continued discipline
- frontend hooks carry important orchestration logic and should stay carefully reviewed

## Reliability Concerns

Current reliability posture is pilot-ready, not enterprise-HA-ready.

Known limits:

- Render deployment posture is not a full HA architecture
- websocket scaling is not yet horizontally hardened
- background jobs are still inside current backend posture
- metrics/tracing maturity is not yet enterprise-grade
- backup/restore process exists but should be drilled on a schedule

## Security Concerns

Current security strengths:

- tenant-explicit access model
- session endpoint proof
- rate limiting proof
- CORS posture documented
- leakage/security docs exist
- secret scanning script exists

Security hardening still needed:

- SSO/SAML/OIDC
- advanced RBAC policy depth
- formal secrets management workflow
- stronger audit/event retention policy
- scheduled security review cadence

## Deployment Concerns

Deployment is proven for the current Render lane, but release discipline must remain strict.

Do not tag or release when:

- live connection check fails
- hosted proof evidence is stale after runtime changes
- local env or proof artifacts are staged
- docs say proof is green but current readiness is unknown

## Testing And Proof Concerns

Strong:

- backend tests exist
- frontend verify exists
- hosted proof is meaningful
- proof prep validates live readiness and authenticated warm-up

Needs continued maturity:

- avoid relying on one large browser proof only
- keep isolated proof commands for failure diagnosis
- add more targeted contract checks when backend APIs expand
- keep proof state handling conservative and secret-safe

## Engineering Classification

Current engineering maturity:

- pilot-ready for current supported scope
- credible for technical review
- not yet large-enterprise production mature

The right next work is disciplined hardening, not broad feature expansion.
