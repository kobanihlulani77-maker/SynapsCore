# Live Deployment Runbook

This runbook is the practical path to deploy SynapseCore onto a single Ubuntu VPS.

## Recommended Target

- Ubuntu `24.04 LTS`
- `2 vCPU / 4 GB RAM`
- public DNS for:
  - `app.example.com`
  - `api.example.com`

## 1. Prepare The Host

SSH into the server:

```bash
ssh root@YOUR_SERVER_IP
```

Install Docker Engine and the Compose plugin using Docker's official Ubuntu flow:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc" | sudo tee /etc/apt/sources.list.d/docker.sources > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
sudo docker run hello-world
```

## 2. Copy SynapseCore To The Host

Choose a stable location, for example:

```bash
sudo mkdir -p /opt/synapsecore
sudo chown "$USER":"$USER" /opt/synapsecore
cd /opt/synapsecore
git clone YOUR_REPOSITORY_URL synapsecore
cd synapsecore
```

## 3. Prepare Real Prod Env Files

Generate working env targets:

```bash
bash scripts/prepare-prod-envs.sh
```

This gives you:

- `infrastructure/env/backend.prod.env`
- `infrastructure/env/frontend.prod.env`
- `infrastructure/env/edge.prod.env`

Update them with real values:

- backend:
  - `CORS_ALLOWED_ORIGINS=https://app.example.com`
  - `SESSION_COOKIE_SECURE=true`
  - `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN=<one-time-secret>` only if this is the first tenant on an empty production database
  - `SYNAPSECORE_PLATFORM_ADMIN_TOKEN=<rotated-secret>` for ongoing production tenant provisioning after bootstrap
  - `SYNAPSECORE_BUILD_VERSION`
  - `SYNAPSECORE_BUILD_COMMIT`
  - `SYNAPSECORE_BUILD_TIME`
- frontend:
  - `VITE_API_URL=https://api.example.com`
  - `VITE_WS_URL=https://api.example.com/ws`
  - `VITE_APP_BUILD_VERSION`
  - `VITE_APP_BUILD_COMMIT`
  - `VITE_APP_BUILD_TIME`
- edge:
  - `SYNAPSECORE_APP_DOMAIN=app.example.com`
  - `SYNAPSECORE_API_DOMAIN=api.example.com`
  - `SYNAPSECORE_ACME_EMAIL=ops@example.com`

For the first self-hosted rollout, Postgres and Redis can stay on the Compose-internal network:

- `DB_HOST=postgres`
- `SPRING_DATA_REDIS_URL=redis://redis:6379`

After the first tenant workspace is created successfully, remove `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN` from the backend env and redeploy so the bootstrap lane is closed again.
Use `SYNAPSECORE_PLATFORM_ADMIN_TOKEN` through the `X-Synapse-Platform-Admin-Token` header for any later production tenant provisioning.
Do not expect ordinary tenant-admin sessions to create additional tenant workspaces in production after that point. This is intentionally blocked.

## Live Verification Credentials

Hosted browser proof must use real tenant accounts, not seed assumptions. Create or refresh the proof lane with production APIs only:

```powershell
$env:PLAYWRIGHT_BASE_URL="https://synapscore-frontend-3.onrender.com"
$env:PLAYWRIGHT_API_BASE_URL="https://synapscore-3.onrender.com"
$env:PLAYWRIGHT_TENANT_CODE="<hosted-proof-tenant-code>"
$env:PLAYWRIGHT_TENANT_ADMIN_USERNAME="<tenant-admin-username>"
$env:PLAYWRIGHT_TENANT_ADMIN_PASSWORD="<tenant-admin-password>"
$env:PLAYWRIGHT_PLANNER_USERNAME="<planner-operator-username>"
$env:PLAYWRIGHT_PLANNER_PASSWORD="<planner-operator-password>"
$env:PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME="<integration-admin-username>"
$env:PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD="<integration-admin-password>"
$env:SYNAPSECORE_PLATFORM_ADMIN_TOKEN="<render-platform-admin-token>"
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
```

Credential model:

- tenant admin: tenant code from `PLAYWRIGHT_TENANT_CODE`, username from `PLAYWRIGHT_TENANT_ADMIN_USERNAME`, password from `PLAYWRIGHT_TENANT_ADMIN_PASSWORD`, operator `Operations Lead`, roles `TENANT_ADMIN`, `REVIEW_OWNER`, `ESCALATION_OWNER`, `INTEGRATION_ADMIN`, and `INTEGRATION_OPERATOR`. This account verifies sign-in, tenant admin pages, catalog onboarding, orders, inventory, scenarios, and user management.
- planner/operator: tenant code from `PLAYWRIGHT_TENANT_CODE`, username from `PLAYWRIGHT_PLANNER_USERNAME`, password from `PLAYWRIGHT_PLANNER_PASSWORD`, operator `Operations Planner`, no tenant-admin roles. This account verifies protected-route access and non-admin access boundaries.
- integration admin: tenant code from `PLAYWRIGHT_TENANT_CODE`, username from `PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME`, password from `PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD`, operator `Integration Lead`, roles `INTEGRATION_ADMIN` and `INTEGRATION_OPERATOR`. This account verifies connectors, CSV ingestion, replay, and recovery flows.

Create/reset path:

- If the proof tenant does not exist, `scripts/prepare-hosted-proof.ps1` creates it through `POST /api/access/tenants`. Use `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN` with `X-Synapse-Bootstrap-Token` for the first tenant, or `SYNAPSECORE_PLATFORM_ADMIN_TOKEN` with `X-Synapse-Platform-Admin-Token` for later tenant provisioning.
- If the proof tenant exists, the script signs in as the tenant admin, upserts proof operators through `/api/access/admin/operators`, creates or remaps users through `/api/access/admin/users`, resets passwords through `/api/access/admin/users/{userId}/reset-password`, then clears forced password-change flags by logging in and changing from a temporary password to the supplied proof password.
- If the tenant exists but the tenant-admin password is unknown, use another active tenant admin to reset it or create a fresh verification tenant code. Platform admin tokens intentionally do not bypass tenant user administration.

After preparation, run:

```powershell
cd frontend
npm.cmd run test:e2e:prod
```

Rotate or disable verification credentials after the proof run when required by the customer environment.

## 4. Run Release Readiness

Before rollout:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env bash scripts/release-readiness.sh
```

That confirms:

- prod config safety
- compose validity
- release fingerprint values
- rollout command set

## 5. Start The Stack

From the repo root:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env bash scripts/start-prod.sh
```

For a real public rollout with HTTPS and separate app/api domains:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env EDGE_ENV_FILE=./env/edge.prod.env bash scripts/start-public-prod.sh
```

## 6. Verify The Deployment

Run:

```bash
FRONTEND_URL=https://app.example.com BACKEND_URL=https://api.example.com bash scripts/verify-deployment.sh
```

Then do a manual live pass:

- sign in
- load dashboard summary
- inspect system runtime
- inspect system incidents
- run the browser proof from `frontend` with `npm.cmd run test:e2e:prod` on Windows or `npm run test:e2e:prod` where supported
- create a test order
- confirm inventory, alerts, and recommendations update

## 7. Protect The Host

Minimum host hygiene:

- keep SSH key-based access only
- disable password SSH auth if possible
- keep the OS patched
- keep a recent Postgres backup

Backup and restore helpers:

```bash
bash scripts/backup-postgres.sh
bash scripts/restore-postgres.sh --file backups/<backup>.sql --yes
```

On Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backup-postgres.ps1
powershell -ExecutionPolicy Bypass -File scripts\restore-postgres.ps1 -BackupFile backups\<backup>.sql -Yes
powershell -ExecutionPolicy Bypass -File scripts\verify-restore-drill.ps1
```

## 8. DNS / HTTPS Note

SynapseCore now includes a built-in public edge option through:

- `infrastructure/docker-compose.public.yml`
- `infrastructure/Caddyfile`

Point your DNS records at the VPS:

- `app.example.com` -> server public IP
- `api.example.com` -> server public IP

Then launch the public compose with `edge.prod.env` filled in.

Keep:

- `SESSION_COOKIE_SECURE=true`
- `CORS_ALLOWED_ORIGINS` narrowed to the real frontend origin
