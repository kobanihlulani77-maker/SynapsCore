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

Hosted browser proof must use real tenant accounts, not seed assumptions. Configure these before running `npm.cmd run test:e2e:prod` from `frontend`:

```text
PLAYWRIGHT_FRONTEND_URL=https://synapscore-frontend-3.onrender.com
PLAYWRIGHT_BACKEND_URL=https://synapscore-3.onrender.com
PLAYWRIGHT_TENANT_CODE=<real-company-tenant-code>
PLAYWRIGHT_OPERATIONS_LEAD_USERNAME=<tenant-admin-username>
PLAYWRIGHT_OPERATIONS_LEAD_PASSWORD=<tenant-admin-password>
PLAYWRIGHT_OPERATIONS_PLANNER_USERNAME=<planner-username>
PLAYWRIGHT_OPERATIONS_PLANNER_PASSWORD=<planner-password>
PLAYWRIGHT_INTEGRATION_LEAD_USERNAME=<integration-admin-username>
PLAYWRIGHT_INTEGRATION_LEAD_PASSWORD=<integration-admin-password>
```

Required roles:

- operations lead: `TENANT_ADMIN`
- operations planner: workspace operator with scenario/order visibility
- integration lead: `INTEGRATION_ADMIN`

Create or reset these accounts through tenant bootstrap/admin APIs, then rotate or disable verification credentials after the proof run when required by the customer environment.

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
