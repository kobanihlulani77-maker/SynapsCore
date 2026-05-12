# Local Runbook

This runbook explains how to connect SynapseCore locally in the supported ways without changing product behavior.

## Local Modes

There are three practical local modes:

1. Docker infra only
   - PostgreSQL in Docker
   - Redis in Docker
   - backend on host
   - frontend on host
2. Full Docker Compose
   - PostgreSQL in Docker
   - Redis in Docker
   - backend in Docker
   - frontend in Docker
3. Host-only fallback
   - manually installed PostgreSQL
   - manually installed Redis
   - backend on host
   - frontend on host

## Shared Local Defaults

Expected local ports:

- Postgres: `5432`
- Redis: `6379`
- backend: `8080`
- frontend: `5173`

Useful local URLs:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- health: `http://localhost:8080/actuator/health`
- readiness: `http://localhost:8080/actuator/health/readiness`
- websocket info: `http://localhost:8080/ws/info`

Seeded local workspace commonly used by local verification scripts:

- workspace code: `STARTER-OPS`
- username: `operations.lead`
- password: `lead-2026`

## Path A — Docker Infra Only

This is the recommended local full-stack path when you want the real frontend and backend processes on the host.

### 1. Start infrastructure

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\infrastructure
docker compose up -d postgres redis
docker compose ps
```

### 2. Verify PostgreSQL and Redis

```powershell
docker exec synapse_postgres pg_isready -U postgres -d synapsecore
docker exec synapse_redis redis-cli ping
```

Expected:

- Postgres reports `accepting connections`
- Redis reports `PONG`

### 3. Start backend on host

Use the chosen local profile and datasource/Redis envs.

Example host-backed `dev` path:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SERVER_PORT="8080"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5432/synapsecore"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://127.0.0.1:5173"
$env:SESSION_COOKIE_SECURE="false"
$env:SESSION_COOKIE_SAME_SITE="Lax"
$env:ALLOW_HEADER_FALLBACK="true"
cmd /c mvnw.cmd spring-boot:run
```

### 4. Start frontend on host

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\frontend
npm.cmd run dev
```

If you use a local env file, it should point at:

```text
VITE_API_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
```

## Path B — Full Docker Compose

This is useful when you want the whole stack containerized.

### Start full stack

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\infrastructure
docker compose up --build -d
docker compose ps
```

Expected services:

- `synapse_postgres`
- `synapse_redis`
- `synapse_backend`
- `synapse_frontend`

Default URLs:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`

### Verify

```powershell
curl.exe http://localhost:8080/actuator/health
curl.exe http://localhost:8080/actuator/health/readiness
curl.exe http://localhost:8080/api/auth/session
curl.exe http://localhost:8080/ws/info
curl.exe http://localhost:5173
```

## Path C — Host-Only Fallback

Use this when Docker is unavailable but PostgreSQL and Redis are installed manually.

Requirements:

- PostgreSQL listening on `5432`
- Redis listening on `6379`

Useful checks:

```powershell
Get-NetTCPConnection -LocalPort 5432 -State Listen
Get-NetTCPConnection -LocalPort 6379 -State Listen
```

Then start backend and frontend on host using the same commands described in Path A.

## Common Local Errors

### Port 8080 already in use

Symptom:

- backend on host fails to bind `8080`

Common cause:

- `synapse_backend` Docker container is already running

Check:

```powershell
docker ps --filter "name=synapse_backend" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Stop it if you need host backend on `8080`:

```powershell
docker stop synapse_backend
```

### Port 5432 conflict between Docker and Windows PostgreSQL

Symptom:

- host backend connects to the wrong PostgreSQL instance
- Docker Postgres login works internally, but host JDBC auth fails

Check what owns port `5432`:

```powershell
Get-NetTCPConnection -LocalPort 5432 -State Listen
Get-Process -Id (Get-NetTCPConnection -LocalPort 5432 -State Listen | Select-Object -First 1).OwningProcess
```

Check Windows PostgreSQL services:

```powershell
Get-Service | Where-Object { $_.Name -match 'postgres' -or $_.DisplayName -match 'PostgreSQL' }
```

Temporary service stop if Docker should own the port:

```powershell
Stop-Service postgresql-x64-17
Stop-Service postgresql-x64-18
```

Warning:

- this is temporary operational work, not a product fix
- you can restart those services later with `Start-Service`

### Postgres recreated but still initializing

Symptom:

- `pg_isready` says `no response`
- `psql` says connection refused

Wait for health:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\infrastructure
do {
  docker compose ps postgres
  Start-Sleep -Seconds 5
} while ((docker inspect synapse_postgres --format "{{.State.Health.Status}}") -ne "healthy")
```

## Safe Local DB Volume Reset

Only do this when you intentionally want to wipe local Docker Postgres data.

Warning:

- this deletes the local PostgreSQL volume
- any local data in that volume is lost

Inspect the volume:

```powershell
docker volume ls --format "{{.Name}}"
```

Stop and remove the Postgres container:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\infrastructure
docker compose stop postgres
docker compose rm -f postgres
```

Delete the volume:

```powershell
docker volume rm infrastructure_postgres_data
```

Recreate Postgres:

```powershell
docker compose up -d postgres
docker compose ps
```

## Local Health Checks

After backend startup:

```powershell
curl.exe http://localhost:8080/actuator/health
curl.exe http://localhost:8080/actuator/health/readiness
curl.exe http://localhost:8080/api/auth/session
curl.exe http://localhost:8080/ws/info
```

## Local Verification Scripts

Useful repo scripts:

- `scripts\verify-deployment.ps1`
- `scripts\verify-realtime.ps1`
- `scripts\verify-company-readiness.ps1`
- `scripts\check-local-connections.ps1`

Recommended first run:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-local-connections.ps1
```

## Local Bottom Line

The local goal is not to invent a different SynapseCore. It is to connect the existing stack cleanly enough to validate the real frontend, real backend contracts, real seed workspace, and real verification scripts.
