# Timeout Recovery - Order Connection Demand

## Bounded Question

Can ten independent Order requests, each inside the required atomic Order
transaction, consume a ten-connection pool and still complete without each
request attempting a second connection borrow that deadlocks the pool?

This phase follows the
[Order/Fulfillment assessment scope correction](timeout-recovery-order-fulfillment-assessment-scope.md).
It does not infer the historical hosted Hikari holders from a numbered browser
test and does not reopen the completed recommendation-reconciliation capture.

## Direct Connection-Demand Proof

`OrderConnectionDemandIntegrationTest` uses the real MockMvc Order endpoint and
a real Hikari pool configured with ten connections. It prepares ten unique
products, inventory rows, and external Order IDs so the workers do not contend
on an intentional business row lock.

A test aspect proceeds through `InventoryService.requireProduct()` and then
aligns all ten workers at a barrier. At that point every outer
`OrderService.createOrderForTenant()` transaction is active and has already
executed SQL. The test records Hikari active, idle, and waiting counts before
allowing all workers to continue through reservation, Order persistence,
Fulfillment initialization, event/audit persistence, commit, and after-commit
dispatch.

The proof is repeated three times. Every cycle reached:

- active connections: **10**
- idle connections: **0**
- completed Order requests: **10 of 10**
- HTTP result: **201 for every request**
- committed Inventory reservation, Order, and Fulfillment state: **verified**
- final active connections and acquisition waiters: **0 after release**

The focused run's acquisition-waiter samples were **0, 0, and 1**; the full
backend rerun sampled **0, 1, and 1**. In every cycle the bounded waiter appeared
while all ten Order requests still completed and the pool then fully released.
That is compatible with after-commit dispatch overlap; it is not the ten nested
borrowers or persistent no-progress state required for a circular pool deadlock.

## Proof-Semantics Correction

An initial repeated run required exactly zero acquisition waiters and failed two
repetitions when a transient waiter was sampled. That assertion was stricter
than the product invariant: bounded queueing may occur when committed
after-commit work overlaps a saturated instant, provided requests make progress
and all connections release.

The final assertion rejects ten nested borrowers while retaining stronger
end-state checks for request completion, atomic persistence, and complete pool
release. No production code changed in response to the diagnostic-only
assertion failure.

## Verification

Combined focused verification: **26 tests passed, 0 failures/errors/skips**.
The set covers the repeated connection-demand proof, the warehouse-bounded
Fulfillment assessment regression, Inventory concurrency, Order lifecycle and
inbound consistency, and Layer 2 intake/reservation.

Full backend verification: **357 tests passed, 0 failures/errors/skips**.
Backend package verification passed and produced the repackaged Spring Boot
JAR. The documentation check found **803 valid local links and no missing
links**. `git diff --check` completed with no whitespace errors.

## Classification and Limits

`DISTINCT_ORDER_REQUIRED_TRANSACTION_CIRCULAR_BORROW = RULED_DOWN_LOCALLY`

This proves that ten distinct normal Order writes can temporarily occupy the
entire configured pool and still complete without a same-request second-borrow
deadlock. It does not prove hosted PostgreSQL throughput, hosted request latency,
or the exact ten owners in the historical
`total=10 active=10 idle=0 waiting>0` incident.

The next bounded holder phase is Replay/Order overlap: align per-record Replay
attempt transactions with independent HTTP Order work, preserve Replay
atomicity, and determine whether failure recording or Order creation creates
nested connection demand. Broad hosted E2E remains gated by exact served-
revision and warm-baseline verification.
