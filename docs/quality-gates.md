# Quality Gates

This document defines the gates that protect SynapseCore releases and pilot operation.

No gate should be skipped.

## Gate 1 - Repository

Required:

- `main` matches `origin/main`
- no uncommitted production changes
- no env files staged
- no proof state staged
- no Playwright reports staged
- no local archives staged unless a deliberate evidence policy exists

Commands:

```powershell
git status --short
git rev-parse HEAD
git rev-parse origin/main
```

## Gate 2 - Build

Frontend:

```powershell
cd frontend
npm.cmd run build
```

Backend when backend changed:

```powershell
cd backend
cmd /c mvnw.cmd test
```

## Gate 3 - Verification

Frontend launch readiness:

```powershell
cd frontend
npm.cmd run verify
```

Docs:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\docs-link-check.ps1
```

Repo health:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\repo-health.ps1
```

## Gate 4 - Live Connections

Command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Required:

- `FRONTEND_UP=True`
- `BACKEND_UP=True`
- `DB_READY=True`
- `AUTH_READY=True`
- `WS_READY=True`
- `PROOF_ALLOWED=True`

## Gate 5 - Hosted Proof

Run only when live connections are healthy.

Commands:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1

cd frontend
npm.cmd run test:e2e:prod
```

Required:

- full hosted proof passes
- evidence captured
- no proof passwords or local state committed

## Gate 6 - Release Candidate

Command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\pilot-rc-check.ps1
```

Required:

- `PILOT_RC_READY=True`
- no blockers
- current proof evidence referenced

## Gate 7 - Pilot

Required:

- pilot sponsor
- operations owner
- technical contact
- support path
- rollback owner
- workspace scope
- approved operators
- success metrics
- known exclusions reviewed

Docs:

- [pilot-release-candidate.md](pilot-release-candidate.md)
- [pilot-company-onboarding-checklist.md](pilot-company-onboarding-checklist.md)
- [pilot-operator-checklist.md](pilot-operator-checklist.md)
- [pilot-success-metrics.md](pilot-success-metrics.md)
- [pilot-rollback-and-escalation.md](pilot-rollback-and-escalation.md)

## Gate 8 - Production

Production requires more than pilot RC readiness.

Required before broader production:

- stronger backup/restore maturity
- support coverage
- incident process
- security review
- deployment rollback rehearsal
- connector ownership
- observability posture
- hosted proof on current deployment

Do not claim broad enterprise readiness from pilot proof alone.

## Gate Failure Rule

If a gate fails:

1. stop
2. classify
3. fix the real seam
4. rerun the failed gate
5. rerun downstream gates that depend on it

Do not bypass gates to preserve momentum.
