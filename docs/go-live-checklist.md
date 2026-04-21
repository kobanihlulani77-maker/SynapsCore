# Go-Live Checklist

Use this checklist right before and right after the first live SynapseCore rollout.

## Before Rollout

- VPS created and reachable over SSH
- Docker Engine and Compose plugin installed
- repo copied to the server
- `backend.prod.env` created
- `frontend.prod.env` created
- `edge.prod.env` created for public rollout
- real domains set
- HTTPS plan decided
- build fingerprint values filled in
- `bash scripts/release-readiness.sh` passes
- or `powershell -ExecutionPolicy Bypass -File scripts\release-readiness.ps1` passes on Windows

## Rollout

- start with:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env bash scripts/start-prod.sh
```

- for public domains and HTTPS, start with:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env EDGE_ENV_FILE=./env/edge.prod.env bash scripts/start-public-prod.sh
```

- smoke verify with:

```bash
FRONTEND_URL=https://app.example.com BACKEND_URL=https://api.example.com bash scripts/verify-deployment.sh
```

- or on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-deployment.ps1 -FrontendUrl https://app.example.com -BackendUrl https://api.example.com
```

## Live Functional Checks

- `GET /actuator/health` is up
- `GET /actuator/health/readiness` is up
- `GET /api/system/runtime` loads
- `GET /api/system/incidents` loads
- frontend loads without console-blocking errors
- sign-in works
- dashboard snapshot loads
- WebSocket connection becomes live
- browser proof passes with `npm.cmd run test:e2e:prod` on Windows or `npm run test:e2e:prod` where supported
- development-only reseed helpers are absent from the production surface
- one test order succeeds
- inventory changes
- alerts and recommendations react

## Trust And Operations Checks

- backend and frontend build fingerprints match the intended release
- allowed origins are correct
- secure cookies are enabled on HTTPS
- header fallback is disabled in prod
- no unexpected replay backlog exists
- no disabled connector is accidental
- no critical system incident is unexplained

## Recovery Checks

- one backup created with:

```bash
bash scripts/backup-postgres.sh
```

Or on Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backup-postgres.ps1
powershell -ExecutionPolicy Bypass -File scripts\verify-restore-drill.ps1
```

- backup file stored outside the running container path if needed
- restore procedure understood before the system is relied on

## After Go-Live

- save the exact deployed env files securely
- record the deployed version / commit / build time
- keep the first clean backup
- document the public URLs
- document the operator test accounts used during launch
