# SynapseCore Leakage Audit

## Audit Scope

This audit is about leakage and unsafe exposure, not feature completeness.

It covers:

- tracked environment files
- tracked Playwright artifacts
- outward-facing docs and scripts
- frontend build output when present
- committed fixture credentials in source and tests
- safe API error behavior
- cross-tenant visibility boundaries

## Current Verified Results

### No tracked runtime-secret files

- no tracked `.env`
- no tracked environment override files

### No tracked proof artifacts

- no tracked `frontend/playwright-report/`
- no tracked `frontend/test-results/`

### No outward-facing proof password leakage

The current scan did not find hosted-proof password literals in:

- `README.md`
- `docs/`
- `scripts/`

### No outward-facing platform token leakage

The current scan did not find bootstrap or platform-admin token literals in:

- `README.md`
- `docs/`
- `scripts/`

### Safe client error contract improved

Malformed JSON and invalid enum request bodies now return safe client responses with `requestId`, rather than falling through to a generic unexpected server path.

### Client-abort noise classification is already in place

`Broken pipe` and `ClientAbortException` are treated as operational noise unless they correlate with a failing request or proof step. They are not treated as product-path failures by default.

## Tenant Isolation Findings

Automated security verification now proves that a signed-in tenant session cannot use another tenant's identity or data across:

- products
- inventory
- recent orders
- alerts
- recommendations
- replay queue
- dashboard snapshot
- runtime diagnostics
- access-admin user directory
- operator directory lookup

It also proves that replay records cannot be replayed across tenant boundaries by another tenant's operator.

## Role Enforcement Findings

Automated security verification now proves that:

- a planner cannot create users
- an integration admin cannot perform tenant-admin workspace mutations
- a normal operator cannot reset passwords for other users

These are signed-in backend-enforced `403` paths, not UI-only checks.

## Fixture-Debt Findings

These are not current production-secret leaks, but they are committed deterministic credentials that should be treated as internal-only development/test debt:

- `backend/src/main/java/com/synapsecore/auth/StarterAccessUsers.java`
- backend hardening/integration test suites with explicit test-only passwords and bootstrap/platform-admin tokens

Why this matters:

- prod bootstrap and platform-admin behavior are already controlled by environment variables and prod profile rules
- hosted proof credentials are not committed in outward-facing docs
- but committed fixture credentials in source can still confuse operational review if they are not clearly treated as non-production fixtures

## Open Security Debt

### Multipart upload ceiling

- Status: locally proven
- Reason: the backend now enforces a deliberate CSV import size ceiling and the security suite verifies a safe `413` response with `requestId`
- Remaining action: confirm the same behavior once on the hosted Render deployment after merge

### Bundle verification depends on build artifact presence

- Status: conditionally verified
- Reason: bundle scanning is only meaningful when `frontend/dist` exists
- Action: run a fresh frontend build before final release-candidate signoff and rescan

## Recommended Interpretation

- outward-facing leak posture: strong
- tenant isolation posture: strong
- role enforcement posture: strong
- auth error-message safety posture: strong
- committed fixture hygiene: still needs cleanup or explicit long-term acceptance
- upload-abuse ceiling: locally closed, hosted confirmation still advisable after deploy

## Verification Commands

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\secret-scan.ps1
```

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd test -Dtest=SecurityHardeningIntegrationTest,SecurityVerificationIntegrationTest,ProductionHardeningIntegrationTest
```
