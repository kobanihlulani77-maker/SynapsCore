# Release Process

This document explains the safe release process for SynapseCore.

It focuses on engineering discipline, operational truth, and proof-driven signoff rather than loose “ship it” behavior.

## Release Philosophy

A SynapseCore release is not just:

- build frontend
- push backend
- hope the runtime settles

A real release should preserve:

- frontend/backend contract truth
- readiness and auth trust
- websocket trust
- replay reliability
- proof integrity

## Release Types

### Frontend-Only Release

Use when:

- UI, docs, styling, or frontend verification logic changed
- backend contracts did not change

Required caution:

- do not break proof-critical labels casually
- do not break route flow or loading/degraded-state UX

### Backend Release

Use when:

- API, replay, auth, scenario, runtime, or integration behavior changed

Required caution:

- DB posture matters
- readiness posture matters
- replay and proof behavior may be affected

### Full-Stack Release

Use when:

- frontend and backend evolve together
- selector, route, API, and proof interactions need end-to-end revalidation

## Pre-Release Checklist

Before any release, confirm:

- intended scope is clear
- env posture is understood
- release does not rely on hidden local state
- local-only files are not staged
- proof assumptions still make sense

## Frontend Pre-Release Checks

Useful commands:

```powershell
cd frontend
npm.cmd run lint
npm.cmd run build
npm.cmd run verify
```

These confirm:

- frontend source policy
- build correctness
- proof-critical label presence
- frontend documentation dependencies

## Backend Pre-Release Checks

Useful checks may include:

- backend integration tests
- migration validation
- runtime-sensitive flow testing

Examples:

```powershell
cd backend
cmd /c mvnw.cmd test
```

```powershell
cd ..
powershell -ExecutionPolicy Bypass -File scripts\validate-flyway.ps1
```

Use targeted backend tests when a change is narrow and high-risk.

## Environment And Config Checks

Useful commands:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\env-sanity-check.ps1
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-prod-config.ps1
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts\release-readiness.ps1
```

These checks help catch:

- bad env posture
- placeholder values
- incorrect cookie or header-fallback posture
- bad release fingerprints

## Live Gating Before Hosted Proof

Before hosted proof, verify live trust first:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Hosted proof should only proceed when:

- frontend is up
- backend responds
- readiness responds
- auth session responds
- websocket info responds

If those are not true, the correct action is to pause proof, not to weaken it.

## Hosted Proof Signoff

Official order:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1

cd frontend
npm.cmd run test:e2e:prod
```

Hosted proof is the strongest final signoff lane for deployed truth.

## Post-Release Checks

After deployment:

- check live frontend URL
- check backend health/readiness/liveness
- check auth/session endpoint
- check websocket info endpoint
- check runtime trust page if appropriate
- confirm proof-critical flows if proof is allowed

## Rollback Thinking

Before a serious backend or infra release, know:

- how to pause rollout
- how to redeploy the previous known good version
- whether DB restore might be needed
- whether connector or replay safety requires operational pause

Rollback readiness is part of release readiness.

## When Not To Release

Do not release casually when:

- backend readiness is already unhealthy
- DB or Redis posture is unknown
- proof blockers are being normalized
- local-only assumptions are still present
- env posture is unclear

## Release-Related Docs

- [go-live-checklist.md](go-live-checklist.md)
- [render-ops-runbook.md](render-ops-runbook.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)
- [environment-reference.md](environment-reference.md)
- [proof-and-validation.md](proof-and-validation.md)

## Bottom Line

The SynapseCore release process is built around one principle:

do not claim a successful release until the deployed system has earned trust again.
