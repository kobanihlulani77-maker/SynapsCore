# Verification Status

Last checked: **2026-08-13**

This file is the strict product-truth record for SynapseCore. It does not treat local proof as the same thing as live hosted proof, and it does not treat a reachable frontend shell as proof that the operational backend is ready.

## Current Final Gate Classification

`READY FOR CONTROLLED COMPANY 1 PILOT - WITH DOCUMENTED OPERATING CONDITIONS`

Reason:

- frontend shell is reachable at `https://synapscore-frontend-3.onrender.com`
- backend is reachable at `https://synapscore-3.onrender.com`
- live health, liveness, readiness, auth/session, and `/ws/info` were verified from the operator PowerShell environment
- `PROOF_ALLOWED=True`
- hosted proof preparation succeeded against proof tenant `HOSTED-PROOF-3`
- final hosted proof passed `6 / 6`
- Gate 4 control verification remains accepted at `201 / 201`

## Most Recent Successful Proof Baseline

Final live proof evidence:

- live connection check: `PROOF_ALLOWED=True`
- proof tenant: `HOSTED-PROOF-3`
- hosted proof: `6 / 6`
- hosted proof duration: `2.9m`
- runtime/proof commit: `d096537`

Accepted Gate 4 baseline:

- control inventory: `201`
- individually verified controls: `201 / 201`
- unverified controls: `0`
- broken controls: `0`
- Gate 4 controls execution suite: `7 / 7`
- backend tests: `133 / 133`
- frontend lint/build/verify: PASS
- secret scan: PASS
- docs links: PASS

Gate 4 evidence:

- [pre-pilot-gate-4-control-verification.md](pre-pilot-gate-4-control-verification.md)

Final gate evidence:

- [final-pre-pilot-release-gate.md](final-pre-pilot-release-gate.md)

## Final Gate Checks Completed

| Check | Result |
| --- | --- |
| Runtime/proof commit | `d096537` |
| Backend tests | PASS, `133` tests, `0` failures, `0` errors |
| Frontend lint | PASS |
| Frontend build | PASS |
| Frontend verify | PASS |
| Control inventory | PASS, `201` controls |
| Gate 4 controls execution | PASS, `7 / 7` batches |
| Production config check | PASS |
| Docs link check | PASS |
| Secret scan | PASS, no critical findings |
| Live frontend | PASS, `200 OK` |
| Live backend health | PASS, `200 OK`, status `UP` |
| Live backend readiness | PASS, `200 OK`, status `UP` |
| Live backend liveness | PASS, `200 OK`, status `UP` |
| Live auth/session | PASS, `200 OK` anonymous session response |
| Live websocket info | PASS, `200 OK` SockJS response |
| Final hosted proof | PASS, `6 / 6` |
| Release tag | NOT CREATED |

## Important Environment Note

The operator PowerShell environment reached Render and ran the official proof successfully. The Codex runtime environment still could not connect to Render endpoints and reported all live checks unavailable.

That Codex-specific network-path failure is not treated as application evidence. Future final live verification should run from an environment with a confirmed network route to Render.

## Product Truth

SynapseCore is ready for a controlled Company 1 pilot inside the documented pilot envelope:

- 1 workspace
- 3 to 5 operators
- 1 connector lane initially
- bounded real operational data
- existing ERP/WMS/source systems remain authoritative
- backup evidence must be captured before operational reliance expands
- provider-managed restore remains a documented controlled-pilot limitation

SynapseCore is not being classified as enterprise general availability, HA-ready, or broadly production-scale.

## Next Valid Release Sequence

Before any new pilot handoff or release tag, rerun:

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

If those are green, Company 1 onboarding may proceed under the controlled pilot limits.
