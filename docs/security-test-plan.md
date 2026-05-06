# SynapseCore Security Test Plan

## Purpose

This plan verifies that SynapseCore's current supported scope is operationally safe before broader customer outreach. It is focused on leakage, tenant isolation, session safety, role enforcement, safe error handling, replay boundaries, and secret hygiene.

## Automated Coverage

### Backend security and safety suites

- `backend/src/test/java/com/synapsecore/SecurityHardeningIntegrationTest.java`
  - generic failed-login response
  - rate limiting on auth, tenant onboarding, access-admin mutations, and integration mutations
  - no session creation on failed auth
  - no audit-table amplification on invalid auth
  - exact-origin CORS behavior on allowed and rejected auth origins

- `backend/src/test/java/com/synapsecore/ProductionHardeningIntegrationTest.java`
  - safe root `/`
  - prod bootstrap and platform-admin behavior
  - runtime metadata posture
  - connector-authenticated ingress in prod
  - repository/entity tenant mismatch guardrails
  - schema and Flyway hardening checks

- `backend/src/test/java/com/synapsecore/SecurityVerificationIntegrationTest.java`
  - session reissue on sign-in and logout invalidation
  - signed-in planner/integration-admin/operator role enforcement on tenant-admin endpoints
  - cross-tenant isolation across:
    - products
    - inventory
    - orders
    - alerts
    - recommendations
    - replay queue
    - dashboard snapshot
    - runtime diagnostics
    - access-admin user directory
    - operator directory lookup
  - replay endpoint cross-tenant boundary protection
  - malformed JSON and invalid enum payloads return safe `400` responses with `requestId`
  - oversized CSV uploads return safe `413` responses with `requestId`
  - wrong tenant plus valid username stays on generic auth failure

- `backend/src/test/java/com/synapsecore/MvpFlowIntegrationTest.java`
  - deterministic replay/recovery contract
  - disabled connector replay queue creation
  - automated replay skip rules for manual-only recovery
  - cross-role replay ownership behavior
  - password-change and session invalidation flows

## Manual Verification Still Required

### Hosted cookie and CORS posture on Render

- Status: manual hosted check still required
- Why: backend tests prove exact-origin behavior in the test harness, but browser cookie policy must still be confirmed against the live hosted frontend/backend pair.
- Method:
  1. Inspect response headers from hosted auth/session endpoints.
  2. Confirm:
     - `SESSION_COOKIE_SECURE=true`
     - `SESSION_COOKIE_SAME_SITE` matches the deployed hosted-origin posture
     - no wildcard production CORS
- Expected:
  - exact hosted frontend origin allowed
  - credentials enabled only for intended origin
  - localhost-only origins used in dev, not in hosted production
- Risk: `medium`

### Bundle-level secret check after build

- Status: automated scan available, build step still required when verifying a fresh bundle
- Method:
  1. Run `npm.cmd run build` from `frontend`.
  2. Run `scripts/secret-scan.ps1`.
  3. Confirm `frontend/dist` contains no explicit bootstrap/platform tokens or hosted-proof password literals.
- Expected:
  - zero critical findings
- Risk: `low`

### Log redaction review

- Status: manual log tail review still required on hosted runtime
- Method:
  1. Inspect recent Render request logs after auth failures, replay failures, and CSV import failures.
  2. Confirm logs keep `requestId` but do not print:
     - plaintext passwords
     - session cookies
     - full sensitive payloads
- Expected:
  - request correlation preserved
  - secrets absent
- Risk: `medium`

## Current Known Residual Issues

### Committed fixture credentials

- `backend/src/main/java/com/synapsecore/auth/StarterAccessUsers.java` contains deterministic starter credentials for local seeded environments.
- backend test suites contain explicit test-only bootstrap/platform-admin tokens and test passwords.
- These are not production secrets, but they are committed fixture credentials and should be treated as internal-only development/test debt.

### Hosted cookie/CORS posture still needs live confirmation

- Local backend enforcement now proves safe malformed-input and oversized CSV rejection paths.
- The remaining hosted validation is browser/runtime oriented:
  - exact hosted cookie attributes
  - exact production CORS behavior on the deployed Render pair

## Expected Verification Commands

### Focused backend security suites

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd test -Dtest=SecurityHardeningIntegrationTest,SecurityVerificationIntegrationTest,ProductionHardeningIntegrationTest
```

### Repo leak scan

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\secret-scan.ps1
```

### Consolidated local verification

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\security-verify.ps1
```
