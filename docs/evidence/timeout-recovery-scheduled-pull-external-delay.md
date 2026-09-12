# Timeout Recovery - Scheduled Pull External Delay

## Bounded Question

Does a slow scheduled-pull HTTP response retain a JDBC connection while the
worker waits for or reads the external response body, reducing Hikari headroom
for concurrent operational requests?

This phase separates external latency from database-backed ingestion. It does
not infer connection hold from total scheduler duration and does not change
timeouts, scheduler frequency, Hikari, or infrastructure.

## Source Ownership

`IntegrationScheduledPullWorkerService.processScheduledPulls()` and
`processDuePulls()` have no outer transaction. The repository query that selects
due connectors completes before connector processing begins.

For each connector, `markPullAttempt()` performs a bounded repository save.
`fetchConnectorPayload()` then validates the target, sends the external HTTP
request, and reads the bounded response body. Ingestion services are called only
after that body has completed. Final connector state and failure evidence use
subsequent bounded repository/service transactions.

## Direct External-Delay Proof

`ScheduledPullExternalDelayConnectionDemandIntegrationTest` starts a local HTTP
endpoint that sends successful response headers but deliberately withholds its
body. The production pull worker processes a real enabled scheduled-pull
connector against that endpoint.

After the worker enters the blocked response-body read, the test waits for the
real ten-connection Hikari pool to report zero active connections. While the
pull remains blocked, ten independent real MockMvc Order requests are aligned
inside their required transactions so they consume the entire pool. The body is
released only after every Order completes.

Across three repetitions:

- active JDBC connections during external body delay: **0, 0, 0**
- active connections at the Order barrier: **10, 10, 10**
- idle connections at the Order barrier: **0, 0, 0**
- acquisition waiters at the Order barrier: **0, 1, 1**
- HTTP Orders completed: **30 of 30 with HTTP 201**
- scheduled-pull connector result: **SUCCESS in every cycle**
- connector attempt/success timestamps: **persisted**
- committed Inventory, Order, and Fulfillment state: **verified**
- final active connections and acquisition waiters: **0 after release**

The pull worker therefore consumes scheduler-thread time during the slow
external body but retains no JDBC connection. The zero-or-one Order waiter is
the already-observed bounded after-commit overlap and does not prevent progress.

## Verification

Focused scheduled-pull verification: **7 tests passed, 0 failures/errors/skips**.
The set covers the three repeated external-delay cycles, Layer 2 scheduled-pull
intake/reservation, and the three repeated ordinary Order-demand cycles.

Full backend verification: **363 tests passed, 0 failures/errors/skips**.

Additional closure gates:

- backend package: **BUILD SUCCESS**
- documentation links: **805 checked, none missing**
- `git diff --check`: **clean**

## Classification and Limits

`SCHEDULED_PULL_EXTERNAL_DELAY_JDBC_RETENTION = RULED_DOWN_LOCALLY`

This proves that external response/body delay in the current scheduled-pull
implementation does not hold a Hikari connection and does not prevent ten
concurrent distinct Orders from using the pool. It does not prove hosted network
latency, hosted PostgreSQL throughput, or the exact ten owners in the historical
Hikari starvation incident.

All four source-mapped holder families now have bounded local evidence. The next
step is a consolidated timeout-recovery checkpoint, CI verification for each
new proof commit, exact deployed-revision confirmation, and one warm-baseline
hosted classification run. Broad hosted E2E must still wait for the deployed
revision and warm baseline rather than elapsed deploy time.
