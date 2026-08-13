# Verification Status

Last checked: **2026-08-13 16:12:25 +02:00**

This file is the strict product-truth record for SynapseCore. It does not treat local proof as the same thing as live hosted proof, and it does not treat a reachable frontend shell as proof that the operational backend is ready.

## Current Final Gate Classification

`NOT READY FOR COMPANY 1 - PILOT BLOCKERS REMAIN`

Reason:

- frontend shell is reachable by `curl.exe` at `https://synapscore-frontend-3.onrender.com`
- backend is not reachable at `https://synapscore-3.onrender.com`
- live health, liveness, readiness, auth/session, and `/ws/info` could not be verified
- `PROOF_ALLOWED=False`
- final hosted proof was not run because the backend readiness/auth/WebSocket prerequisites are unavailable

## Most Recent Successful Proof Baseline

Accepted Gate 4 baseline:

- control inventory: `201`
- individually verified controls: `201 / 201`
- unverified controls: `0`
- broken controls: `0`
- Gate 4 controls execution suite: `7 / 7`
- backend tests: `133 / 133`
- hosted proof baseline: `6 / 6`
- frontend lint/build/verify: PASS
- secret scan: PASS
- docs links: `604 / 604`

Gate 4 evidence:

- [pre-pilot-gate-4-control-verification.md](pre-pilot-gate-4-control-verification.md)

Final gate evidence:

- [final-pre-pilot-release-gate.md](final-pre-pilot-release-gate.md)

## Final Gate Checks Completed

| Check | Result |
| --- | --- |
| HEAD/origin at start | MATCH: `3459f11c2af5c7760d962b596ec0759773d8a3a4` |
| Clean-worktree backend test | PASS, `133` tests, `0` failures, `0` errors |
| Clean-worktree frontend build | PASS |
| Clean-worktree frontend verify | PASS |
| Main backend test | PASS, `133` tests, `0` failures, `0` errors |
| Main frontend lint | PASS |
| Main frontend build | PASS |
| Main frontend verify | PASS |
| Control inventory | PASS, `201` controls |
| Production config check | PASS |
| Docs link check | PASS, `604` links, `0` missing |
| Secret scan | PASS, `0` critical findings |
| Live frontend | PASS by `curl.exe` |
| Live backend | FAIL, could not connect |
| Final hosted proof | BLOCKED, backend unavailable |
| Final controls suite | BLOCKED, backend unavailable |
| Release tag | NOT CREATED |

## Product Truth

SynapseCore remains a real operational platform for its currently supported scope, but the exact deployed build cannot be released to Company 1 while the backend service is unreachable.

The next valid release sequence is:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Only if the script returns:

```text
FRONTEND_UP=True
BACKEND_UP=True
DB_READY=True
AUTH_READY=True
WS_READY=True
PROOF_ALLOWED=True
```

then run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1

cd frontend
npm.cmd run test:e2e:prod
npm.cmd run test:controls:execution
```

Do not tag or freeze Company 1 release until hosted proof and final live controls are green again.
