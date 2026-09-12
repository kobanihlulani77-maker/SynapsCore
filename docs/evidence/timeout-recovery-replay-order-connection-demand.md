# Timeout Recovery - Replay/Order Connection Demand

## Bounded Question

Can an automated Replay attempt overlap nine independent HTTP Order writes in a
ten-connection pool, roll its failed Order work back, and persist durable Replay
failure evidence without a hidden nested connection borrow or pool deadlock?

This phase preserves the established per-record Replay transaction and Order
atomicity. It does not make the full Replay batch transactional and does not
infer hosted causality from scheduler duration.

## Source Ownership

`IntegrationReplayAutomationService.processAutomatedReplay()` has no outer
transaction. `IntegrationReplayService.processAutomatedReplayBatch()` selects
eligible IDs and processes each ID independently.

For each ID, `attemptAutomatedReplayById()` opens one `TransactionTemplate`
transaction. The Replay row is locked, connector and request state are
validated, and `OrderService.createOrderForTenant()` joins that transaction.
On a domain failure, the attempt is marked rollback-only. Only after that
transaction completes does `recordReplayFailureInFreshTransaction()` open a new
transaction to lock the Replay row and persist durable failure evidence.

## Direct Demand Proof

`ReplayOrderConnectionDemandIntegrationTest` uses the real automated Replay
service, real MockMvc Order endpoint, and a real ten-connection Hikari pool. It
creates one Replay whose Order must fail for insufficient inventory plus nine
independent valid HTTP Orders, all on unique products and inventory rows.

A barrier aligns all ten workers after product resolution while their actual
transactions hold all ten pool connections. A Replay-row lock probe also
registers transaction completion on the initial attempt and observes the second
lock used for durable failure recording.

Across three repetitions:

- active connections at the barrier: **10, 10, 10**
- idle connections at the barrier: **0, 0, 0**
- acquisition waiters: **0, 1, 1**
- valid HTTP Orders completed: **27 of 27 with HTTP 201**
- Replay-row lock calls per cycle: **2**
- initial rollback completed before fresh failure write: **true in every cycle**
- failed Replay Order/Inventory side effects: **none**
- durable Replay failure status and attempt count: **verified**
- final active connections and acquisition waiters: **0 after release**

The zero-or-one transient waiter is bounded after-commit overlap. It does not
prevent progress and is not the ten-waiter no-progress pattern required for a
circular borrow deadlock.

## Diagnostic False Red

The first repeated execution produced **3 tests, 2 failures** because the shared
test profile configures Replay backoff to zero. Repetitions two and three
therefore selected the preceding failed Replay record instead of the newly armed
fixture. That old SKU did not enter the current barrier, so the nine test Orders
timed out in the probe and returned synthetic HTTP 500 responses.

The logs proved the prior Replay ID was selected before each broken barrier. The
fixture now sets a 300-second backoff for this test class, ensuring each
repetition measures its newly created Replay. No production code changed in
response to this test-orchestration defect.

## Verification

Focused Replay/Order verification: **13 tests passed, 0 failures/errors/skips**.
The set covers the three repeated mixed-demand cycles, existing Replay
atomicity, Layer 2 Replay recovery, and the three repeated ordinary Order-demand
cycles.

Full backend verification: **360 tests passed, 0 failures/errors/skips**.
Backend packaging succeeded and produced the repackaged Spring Boot JAR. The
documentation check found **804 valid local links and no missing links**.
`git diff --check` completed with no whitespace errors.

## Classification and Limits

`REPLAY_ORDER_NESTED_CONNECTION_BORROW = RULED_DOWN_LOCALLY`

The failed Replay attempt releases its transaction before durable failure
recording borrows another connection. The joined Order work does not create a
second simultaneous borrow for the Replay thread. This does not prove hosted
PostgreSQL throughput or identify the historical ten hosted Hikari holders.

The next bounded holder family is scheduled pull: prove that external fetch and
response-body delay occur before ingestion transactions borrow JDBC connections,
then measure overlap with HTTP work. No broad hosted E2E should precede exact
served-revision and warm-baseline verification.
