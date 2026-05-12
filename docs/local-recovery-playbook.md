# Local Recovery Playbook

This playbook explains how to recover local SynapseCore bring-up issues without changing product behavior.

It focuses on the failure classes already seen in local work:

- Docker down
- port collisions
- Postgres confusion
- backend container conflicts
- host/backend mismatch
- Redis missing

## Docker Down

Symptoms:

- `docker info` fails
- compose commands fail
- local Postgres/Redis containers are unavailable

Recovery:

1. start Docker Desktop or the Docker service
2. rerun:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\infrastructure
docker compose up -d postgres redis
docker compose ps
```

## Postgres Credential Mismatch

Symptoms:

- backend fails with password auth error
- Docker internal `psql` works
- host JDBC still fails

Likely cause:

- host process is hitting the wrong PostgreSQL instance
- Windows PostgreSQL and Docker PostgreSQL are conflicting

Recovery checks:

```powershell
Get-NetTCPConnection -LocalPort 5432 -State Listen
Get-Process -Id (Get-NetTCPConnection -LocalPort 5432 -State Listen | Select-Object -First 1).OwningProcess
Get-Service | Where-Object { $_.Name -match 'postgres' -or $_.DisplayName -match 'PostgreSQL' }
```

## Redis Unavailable

Symptoms:

- readiness fails
- backend may start partially but runtime/auth/realtime posture degrades

Recovery:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\infrastructure
docker compose up -d redis
docker exec synapse_redis redis-cli ping
```

Expected:

- `PONG`

## Port Collisions

### Port 8080 collision

Typical cause:

- `synapse_backend` container already owns `8080`

Check:

```powershell
docker ps --filter "name=synapse_backend" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Stop if you want host backend:

```powershell
docker stop synapse_backend
```

### Port 5432 collision

Typical cause:

- Docker Postgres and Windows Postgres both present

Temporary Windows service stop:

```powershell
Stop-Service postgresql-x64-17
Stop-Service postgresql-x64-18
```

Do not do this casually on a machine where those services are needed for other work without understanding the impact.

## Backend Startup Failure

Common classes:

- DB unavailable
- wrong JDBC target
- Redis unavailable
- port `8080` already in use

Recovery order:

1. verify Postgres
2. verify Redis
3. verify `8080` ownership
4. rerun backend

## Frontend / Backend Mismatch

Symptoms:

- frontend shell opens
- API calls fail
- sign-in or dashboard cannot load

Check frontend env target:

- `VITE_API_URL`
- `VITE_WS_URL`

Verify backend locally:

```powershell
curl.exe http://localhost:8080/actuator/health
curl.exe http://localhost:8080/api/auth/session
curl.exe http://localhost:8080/ws/info
```

## Local Env Drift

Symptoms:

- one machine works, another does not
- host backend or frontend points at the wrong target

Rules:

- do not commit `.env.local`
- prefer scripted or documented local commands
- verify actual target URLs before debugging app code

## Replay / Proof Local Issues

Local replay or proof-like issues should be interpreted carefully:

- local verification is useful
- but local success is not the same as deployed proof success

Use:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-local-connections.ps1
powershell -ExecutionPolicy Bypass -File scripts\verify-deployment.ps1
powershell -ExecutionPolicy Bypass -File scripts\verify-realtime.ps1
```

## Websocket Local Issues

Symptoms:

- `/ws/info` fails
- dashboard shows reconnecting or stale live state

Recovery checks:

- backend health
- backend readiness
- Redis posture in production-like mode
- frontend env points to correct `/ws`

## Safe DB Recreation Posture

Sometimes Docker Postgres must be recreated.

Warning:

- do not delete the local DB volume casually
- deleting the volume destroys local data

Safe sequence:

1. confirm the issue is not just startup delay
2. inspect container health
3. only then consider volume deletion

## What Should NOT Be Deleted Casually

Do not casually delete:

- Docker Postgres volume
- local DB data you still care about
- local env files you have not backed up
- replay or proof artifacts you still need for debugging

## Bottom Line

Local recovery should focus on environment and connection clarity first. Most local failures are infrastructure-target mismatches, not product code defects.
