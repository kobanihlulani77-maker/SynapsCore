# Performance And Scale Proof

This document records the controlled performance, concurrency, and scale evidence for the SynapseCore pre-pilot Gate 3 review.

The goal of this proof is not to claim enterprise-scale production readiness. The goal is to determine whether the current SynapseCore platform can safely support a controlled Company 1 pilot with a small number of authenticated operators, one workspace, and one operational lane while preserving runtime truth, replay visibility, realtime updates, and backend stability.

## Gate Result

Classification:

`PRE-PILOT GATE 3 ACCEPTED WITH DOCUMENTED LIMITATION`

Supported by current evidence:

- 25 authenticated concurrent read operators were functionally stable in local production-shaped Docker.
- 25 authenticated concurrent read operators sustained about 41 requests per second in focused and soak runs.
- The focused 25-user read run achieved 100% success with p95 latency below 500 ms.
- The 5-minute 25-user soak run achieved 100% success with p95 latency below 500 ms.
- 50 SockJS/STOMP clients connected successfully and received realtime dashboard events without missed delivery in the corrected trigger run.
- Controlled one-writer mutation traffic produced no 5xx responses, no backend crash, no DB deadlocks, and no data-integrity corruption.
- Backend readiness, auth session, and SockJS info endpoints remained healthy after the load runs.

Documented limitations:

- This was a local Docker proof, not a live Render load test.
- This was a single-backend, single-PostgreSQL, single-Redis posture, not a highly available deployment.
- The dataset was synthetic and pilot-sized, not a large production history.
- Multi-tenant concurrent load was not tested in this gate.
- The database pool reached visible pressure at 25 authenticated readers.
- Realtime event latency was functionally correct but variable across WebSocket stages.
- Mutation throughput is not proven for high-write workloads.
- Render/provider-level scaling, HA, backup retention, encryption, and managed restore behavior remain outside this proof.

## Environment

Target:

- local Docker Compose infrastructure
- backend container on `http://127.0.0.1:8080`
- Docker PostgreSQL on port `5432`
- Docker Redis on port `6379`
- no live Render load
- no production database load
- no manual database edits

Runtime observations:

- Backend started with the `dev` profile.
- Hikari started successfully.
- Flyway validated all migrations and reported schema version 7.
- JPA EntityManager initialized successfully.
- Tomcat started on port `8080`.
- Spring WebSocket broker started.
- Backend startup was slow but successful at about 54 seconds.

Docker host shape:

- 4 CPUs
- about 3.7 GiB Docker memory available
- Linux container runtime

Database and cache:

- PostgreSQL 15.17
- Redis 7.4.8
- PostgreSQL `max_connections=100`
- observed Hikari maximum pool size: 10

## Tooling

The pilot load proof uses a repository-native Node script:

- `frontend/scripts/pilot-load-check.mjs`
- npm command: `npm.cmd run test:load:pilot`

The script is intentionally scoped for controlled pre-pilot validation. It is not a replacement for a full load-testing platform such as k6, Gatling, JMeter, or Locust.

It measures:

- authenticated HTTP read traffic
- controlled mutation traffic
- dashboard/realtime SockJS event delivery
- backend actuator readiness during and after load
- JVM memory usage
- process and system CPU usage
- Hikari active, idle, pending, and max connections
- active HTTP request tasks
- dataset counts and integrity checks

It writes raw JSON results under:

- `frontend/test-results/pilot-load/`

Those files are local proof artifacts and should not be committed.

## HTTP Read Ramp

Command shape:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\frontend
npm.cmd run test:load:pilot -- --stages 1,3,5,10,15,25 --wsStages 1,5,10,25,50 --durationSeconds 60 --warmupSeconds 10 --soakSeconds 300 --mutationUsers 1 --loginPauseMs 7000
```

Result artifact:

- `frontend/test-results/pilot-load/gate3-load-20260811205331.json`

Observed HTTP read stages:

| Stage | Requests | Approx RPS | Success | 4xx | 5xx | p50 | p95 | p99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 user | 100 | 1.67 | 100% | 0 | 0 | 32.57 ms | 390.79 ms | 501.02 ms |
| 3 users | 329 | 5.48 | 100% | 0 | 0 | 21.18 ms | 209.47 ms | 440.28 ms |
| 5 users | 541 | 9.02 | 100% | 0 | 0 | 20.43 ms | 204.12 ms | 364.46 ms |
| 10 users | 1050 | 17.50 | 100% | 0 | 0 | 31.07 ms | 277.64 ms | 477.43 ms |
| 15 users | 1465 | 24.42 | 100% | 0 | 0 | 41.77 ms | 404.87 ms | 804.13 ms |
| 25 users | 2339 | 38.98 | 100% | 0 | 0 | 47.28 ms | 602.68 ms | 1276.83 ms |

Interpretation:

- Read traffic was functionally stable through 25 concurrent authenticated operators.
- No read stage produced 4xx or 5xx errors.
- The 25-user short ramp exceeded the preferred 500 ms p95 threshold once, so the strict latency envelope should be treated as variable at that level.
- The 15-user stage met the preferred p95 threshold cleanly in the ramp.

## 25-User Soak

Observed soak:

| Stage | Duration | Requests | Approx RPS | Success | 4xx | 5xx | p50 | p95 | p99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 25-user read soak | 300 s | 12380 | 41.27 | 100% | 0 | 0 | 34.20 ms | 384.87 ms | 998.73 ms |

Interpretation:

- The 5-minute 25-user read soak stayed healthy.
- The soak achieved 100% read success.
- The p95 latency was under 500 ms during the longer 25-user soak.
- This supports Company 1 pilot confidence for read-heavy operator workloads.

## Focused Resource Run

Command shape:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\frontend
npm.cmd run test:load:pilot -- --stages 25 --skipWs true --skipSoak true --durationSeconds 120 --warmupSeconds 10 --mutationUsers 1 --loginPauseMs 7000 --resourceSampleSeconds 5
```

Result artifact:

- `frontend/test-results/pilot-load/gate3-load-20260811211507.json`

Observed read result:

| Stage | Duration | Requests | Approx RPS | Success | 4xx | 5xx | p50 | p95 | p99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 25-user read | 120 s | 4952 | 41.27 | 100% | 0 | 0 | 32.36 ms | 383.42 ms | 999.61 ms |

Peak resource observations during the 25-user focused read stage:

| Resource | Observed Peak |
| --- | ---: |
| Process CPU | 1.00 |
| System CPU | 0.99 |
| JVM memory used | about 335 MiB |
| JVM memory max | about 2.15 GiB |
| Live JVM threads | 57 |
| Hikari active connections | 10 |
| Hikari idle connections | 10 |
| Hikari pending connections | 10 |
| Hikari max connections | 10 |
| Active HTTP request tasks | 22 |

Interpretation:

- The backend survived the 25-user read-focused run without failure.
- CPU and Hikari pool pressure appeared at this level.
- The first observed scaling constraint is database connection pool and container CPU pressure, not application correctness.
- This is acceptable for the controlled pilot boundary, but it should guide future infrastructure tuning.

## Mutation Stage

The mutation stage used one controlled mutating operator while read traffic was also measured.

Observed in the ramp run:

| Stage | Requests | Approx RPS | Success | 4xx | 5xx | p50 | p95 | p99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 mutation user | 97 | 1.62 | 93% | 7% | 0 | 47.69 ms | 355.81 ms | 652.66 ms |

Observed in the focused resource run:

| Stage | Requests | Approx RPS | Success | 4xx | 5xx | p50 | p95 | p99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 mutation user | 219 | 1.83 | 80% | 20% | 0 | 33.69 ms | 89.34 ms | 339.51 ms |

Interpretation:

- Controlled mutation traffic did not generate server errors.
- 4xx responses were observed and are treated as governance, validation, authorization, or rate-limit responses unless later logs prove otherwise.
- No 5xx responses occurred.
- No data-integrity failure was observed.
- High write throughput is not proven by this gate.

## Realtime And WebSocket Proof

Corrected command shape:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\frontend
npm.cmd run test:load:pilot -- --skipHttp true --wsStages 1,5,10,25,50 --wsEventWaitMs 15000 --orderSku SKU-VDR-210 --warehouseCode WH-NORTH --loginPauseMs 7000
```

Result artifact:

- `frontend/test-results/pilot-load/gate3-load-20260811211158.json`

Observed WebSocket stages:

| Stage | Established | Missed Events | Duplicate Payloads | Extra Messages | Event p95 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 1 socket | 1/1 | 0 | 0 | 0 | 636.39 ms |
| 5 sockets | 5/5 | 0 | 0 | 5 | 1307.43 ms |
| 10 sockets | 10/10 | 0 | 0 | 0 | 424.51 ms |
| 25 sockets | 25/25 | 0 | 0 | 0 | 1180.71 ms |
| 50 sockets | 50/50 | 0 | 0 | 0 | 399.02 ms |

Interpretation:

- 50 SockJS/STOMP clients connected successfully.
- Realtime dashboard event delivery was functionally correct in the corrected run.
- No corrected-stage event was missed.
- Event latency was variable, with 5-socket and 25-socket stages exceeding a preferred 1-second p95 target.
- This is sufficient for controlled pilot visibility, but not enough to claim broad realtime scale.

## Dataset And Integrity

Dataset before focused resource run:

| Entity | Count |
| --- | ---: |
| Products | 46 |
| Inventory rows | 8 |
| Orders | 12 |
| Alerts | 1 |
| Recommendations | 12 |
| Connectors | 3 |
| Replay items | 0 |
| Scenarios | 0 |
| Users | 16 |

Dataset after focused resource run:

| Entity | Count |
| --- | ---: |
| Products | 93 |
| Inventory rows | 8 |
| Orders | 12 |
| Alerts | 1 |
| Recommendations | 12 |
| Connectors | 3 |
| Replay items | 0 |
| Scenarios | 0 |
| Users | 16 |

Integrity checks:

- duplicate product SKUs: 0
- impossible inventory rows: 0
- orders readable: true
- replay readable: true
- scenarios readable: true

Interpretation:

- The controlled mutation stage changed expected catalog-like data.
- No duplicate SKU or impossible inventory state was observed.
- Core operational surfaces remained readable after load.

## Backend, DB, And Redis After Load

Post-load endpoint checks:

- `/actuator/health/readiness`: HTTP 200, `UP`
- `/api/auth/session`: HTTP 200, anonymous session response
- `/ws/info`: HTTP 200, SockJS info response

Post-load container observations:

| Container | CPU | Memory |
| --- | ---: | ---: |
| backend | about 0.52% | about 622 MiB |
| postgres | about 2.15% | about 150 MiB |
| redis | about 0.49% | about 12 MiB |

PostgreSQL observations:

- active connections: 1
- idle connections: 10
- total observed connections: 11
- deadlocks: 0

Redis observations:

- connected clients: 3
- used memory: about 1.38 MiB
- rejected connections: 0

Backend logs:

- no out-of-memory evidence
- no deadlock evidence
- no application crash
- no error pattern observed during the reviewed load window
- Hibernate follow-on locking warnings were observed and should be tracked, but did not correspond to failed requests in this proof

## Company 1 Pilot Boundary

Recommended Company 1 pilot envelope:

- one tenant workspace
- one controlled operational lane
- 3 to 5 active operators
- one connector lane or controlled integration path
- proof-gated deployment changes
- backup/restore procedure understood before live operational reliance
- support escalation path defined

Safety margin:

- 3 operators are about 12% of the 25-operator read concurrency proof.
- 5 operators are about 20% of the 25-operator read concurrency proof.
- Against the stricter 15-operator ramp p95 baseline, 3 operators are about 20% and 5 operators are about 33%.

This is an appropriate safety margin for a controlled pilot. It is not a basis for claiming broad production scale.

## Current Scale Classification

| Classification | Status | Evidence |
| --- | --- | --- |
| Controlled pilot | Proven | 25 authenticated read operators, 50 WebSocket clients, healthy post-load checks |
| Small production | Plausible but unproven | Needs longer live-like duration, backup/restore proof, monitoring, operational history |
| Small/mid-market production | Not supported by current evidence | Needs larger datasets, multi-tenant load, write-heavy proof, metrics/tracing |
| Mid-market plus | Not supported | Needs HA architecture, worker separation, queueing, observability stack, backup maturity |
| Enterprise scale | Not supported | Needs enterprise security, SSO/RBAC, horizontal realtime scale, DR, audited operations |

## Accepted Limitations

This gate deliberately does not prove:

- Render live infrastructure load capacity
- high-availability failover
- multi-region operation
- horizontal backend scale
- WebSocket clustering
- queue-backed worker separation
- heavy connector ingestion throughput
- multi-tenant load isolation under pressure
- high-write mutation throughput
- enterprise-grade audit storage
- provider-level backup retention and restore automation

These limitations do not block a controlled Company 1 pilot, but they must remain visible.

## Follow-Up Engineering Priorities

Before broad production expansion:

- Add deeper load testing with larger datasets.
- Add multi-tenant concurrent load coverage.
- Add write-heavy operational proof separate from read-heavy operator proof.
- Tune or validate Hikari pool sizing under realistic DB capacity.
- Add metrics/tracing for request latency, DB pool pressure, WebSocket event timing, and connector work.
- Prove backup/restore on the selected production provider.
- Capture longer soak evidence after real pilot workflows are known.

## Gate 3 Verdict

SynapseCore has enough controlled local performance evidence to proceed toward the next pre-pilot gate.

The correct verdict is:

`PRE-PILOT GATE 3 ACCEPTED WITH DOCUMENTED LIMITATION`

Gate 4 can begin only after this limitation remains documented and accepted. Gate 4 should focus on exhaustive functional control verification, not on expanding the product scope.
