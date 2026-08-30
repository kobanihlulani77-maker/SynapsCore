# Runtime Lifecycle Closure Evidence

## Scope

This record covers Domain 16 Runtime only. Catalog, Inventory, Orders,
Fulfillment, Integrations, Replay, Scenarios, Alerts, Recommendations,
Dashboard, Public Entry, Auth, Tenant Administration, Warehouse Context, and
Activity/Audit remain closed and were not reopened.

Starting repository HEAD for this Runtime cycle:

`fce9a3e253184064ab971741dd000b1d1645a1af`

The working tree also contained unrelated local changes before this cycle:

- `frontend/Dockerfile`
- `.gitattributes`
- `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`
- `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`

Those files are not part of Runtime closure work.

## Runtime Contract

- **Liveness** means that the process is alive.
- **Readiness** means that the instance can safely serve expected production traffic.
- **UP** is reserved for a live process whose dependency-aware readiness health is up.
- **DEGRADED** describes a live process that is not fully ready or has a non-core capability impaired.
- **DOWN** describes failed process liveness or unavailable safe operation.
- **UNKNOWN** is not emitted by the current response model; inability to confirm readiness must not become `UP`.

The custom runtime status now consults the Spring readiness health group in
addition to application liveness/readiness availability state. This prevents a
required PostgreSQL or production Redis health failure from being represented as
custom overall `UP`.

## Production Topology

Render serves a static frontend and a single backend web service. The backend
uses managed PostgreSQL for authoritative business persistence and managed
Redis for production sessions and Redis Pub/Sub. Flyway and JPA validation are
startup-critical. Current deployment is intentionally single-instance; HA,
multi-region, distributed scheduling, and automatic failover are not present.

## Dependency Classification

| Dependency | Runtime meaning | Classification |
| --- | --- | --- |
| PostgreSQL | Authoritative business persistence | Required |
| Redis session store | Authenticated authority verification | Required |
| Flyway/JPA validation | Safe schema startup | Startup-critical |
| Redis rate limiting | Distributed throttling | Degradable to bounded local fallback in single-node topology |
| Redis realtime Pub/Sub | Cross-node/live delivery capability | Important, REST remains authoritative |
| Dashboard cache | Read optimization | Degradable; live database calculation remains available |
| External connector | Tenant-specific source | Connector-local failure; must not make the platform globally unready |

## Health and Startup

The configured actuator groups are:

- `/actuator/health/liveness`: liveness state and ping.
- `/actuator/health/readiness`: readiness state, database, Redis, and ping.
- `/actuator/health`: aggregate health.

Production actuator exposure is restricted to health. Health responses must not
contain datasource URLs, credentials, Redis URLs, connector secrets, session
values, or private stack traces. `/actuator/info`, `/actuator/metrics`, and
`/actuator/prometheus` are not public production metadata sources.

Production startup explicitly sets the `prod` profile through `render.yaml`,
uses PostgreSQL/Redis configuration, disables automatic starter seeding and
default tenant fallback, validates the schema, and stores sessions in Redis.
The official production path therefore does not silently use the development
seed path. Alternate deployment paths still need to preserve that explicit
profile requirement.

Expected startup order is:

`configuration -> datasource -> Flyway -> JPA validation -> application ready -> schedulers/business traffic`

The production profile now enables Spring graceful HTTP shutdown with a bounded
30-second shutdown phase. Scheduler and async executor shutdown waits remain
bounded at 30 seconds and 15 seconds respectively.

## Runtime Failure Boundaries

- PostgreSQL failure must not produce a false successful business mutation. The existing transaction boundary remains authoritative; current generic persistence failures may be returned as safe `500` responses rather than a universal `503`.
- PostgreSQL recovery has no application recovery controller. Pool/JDBC recovery and controlled restart behavior require focused execution evidence.
- Redis session failure must not grant authority. If session state is unverifiable, re-login is valid; client headers cannot restore authority in production.
- Redis rate-limit failure uses process-local enforcement only in the current single-backend topology. The fallback is now bounded at 10,000 active keys, removes expired entries during fallback evaluation, and fails closed for new keys at capacity.
- Realtime Pub/Sub failure may degrade live delivery after persistence. REST/domain state remains authoritative and committed business data is not rolled back solely because delivery failed.
- Dashboard cache failure falls back to live database calculation.
- Connector timeouts are bounded at approximately 10 seconds for connection and 20 seconds for fetch. Connector failure remains connector/import/replay evidence.

## Background Work

The current scheduled services are integration pull, automated replay,
recommendation reconciliation, and operational dispatch. The dispatch executor
is bounded at core 2, maximum 4, queue 64. The current deployment assumes one
backend instance; adding replicas requires scheduler coordination review.

Some outer scheduler/database exception paths still require focused failure
injection to prove that subsequent scheduled executions continue and that the
failure is visible. No distributed scheduler or worker architecture is being
introduced in this cycle.

## Logging and Observability

Request IDs are supplied by `RequestTraceFilter` and retained in safe error
responses/log context. Realtime malformed-envelope diagnostics no longer include
raw payload previews; they use bounded size and fingerprint metadata instead.
The optional alert webhook now uses finite 5-second connect and 10-second read
timeouts. Metrics cover HTTP, auth, rate limiting, realtime, dispatch, replay,
inventory locking, and alert hooks.

Runtime incidents remain derived read-side projections. They are bounded and do
not become a new persisted incident-management product. Activity/Audit remains
the historical evidence boundary.

## Scenario Boundary Correction

Scenario is what-if intelligence plus governed decision/communication support.
Approval means governance is complete. Approval does not execute a Scenario,
create Orders, mutate Inventory, perform Fulfillment, or promote projected
Alerts/Recommendations into live operational truth. `POST /api/scenarios/{id}/execute`
is `410 Gone`. Real operational change requires later authoritative source-system
activity.

## Changes Made In This Cycle

1. Custom Runtime overall status now requires the dependency-aware Spring readiness health group to be `UP` before returning overall `UP`.
2. Redis rate-limit fallback now cleans expired counters, enforces a 10,000-key ceiling, and fails closed for new keys at capacity.
3. Redis fallback activation emits a bounded operational warning without principal data.
4. Malformed realtime envelope logs and alert-hook details no longer contain raw payload previews.
5. Optional alert webhook calls use bounded connection and read timeouts.
6. Production HTTP shutdown is explicitly graceful with a 30-second bound.
7. Focused tests cover fallback memory bounds and graceful-shutdown configuration.

## Verification Status

The local Maven attempt could not resolve the Spring Boot parent POM because the
sandbox could not access Maven Central. This is a Classification C local/sandbox
dependency-access limitation, not a Runtime product defect.

The exact candidate tree was verified by GitHub Actions:

- Candidate SHA: `22b89c607b79d5258caaa9445cb35c53aadf0b5a`
- Run: `33315989175`
- Status: `completed`
- Conclusion: `success`
- Backend `./mvnw test`: PASS
- Frontend dependency installation: PASS
- Frontend production build: PASS
- Compose validation input preparation: PASS
- Development Compose validation: PASS
- Production Compose validation: PASS

The candidate CI run is the authoritative compilation and test evidence for the
changed Runtime source/configuration. Main promotion, deployment, and safe
hosted Runtime proof remain separate gates.

No destructive hosted PostgreSQL or Redis outage test is required. Managed
Render restore remains unproven application evidence and is classified as a
provider boundary/evidence gap.

## A/B/C/D Census

| Class | Current Runtime items |
| --- | --- |
| A | None. The candidate CI passed backend tests, frontend build, and both Compose validations; the prior custom-status, fallback-bound, raw-payload-log, and unbounded-webhook concerns are verified corrections. |
| B | Single backend instance, no HA, no multi-region, no distributed scheduler, session loss requiring re-login, framework-default pools, derived runtime incidents, no `/actuator/info` commit endpoint. |
| C | Local/sandbox Maven Central access limitation, managed provider restore, actual Render PostgreSQL/Redis outage, exact Render SIGTERM timing, long-duration resource behavior, and owner/live Runtime walkthrough. |
| D | Active-active, multi-region, automatic failover, service mesh, centralized observability, distributed tracing, and automated disaster-recovery orchestration. |

## Closure Position

This record verifies the candidate CI gate. Runtime closure still requires safe
promotion to `main`, successful main CI, deployment health, and non-destructive
hosted Runtime proof.
No Realtime domain work has started.
