# Timeout Recovery - Order/Fulfillment Assessment Scope

## Bounded Question

Does one required atomic Order write perform fulfillment assessment work for
unrelated warehouses while its JDBC transaction is open, causing connection
hold cost to grow with tenant-wide active fulfillment volume?

This phase follows the
[background holder budget](timeout-recovery-background-holder-budget.md). It
does not reopen the completed recommendation-reconciliation capture or infer
the historical ten Hikari holders from a numbered browser test.

## Transaction and Source Map

`OrderService.createOrderForTenant()` owns the required Order transaction. The
path resolves the warehouse and products, reserves inventory, persists the
Order, reevaluates inventory intelligence, initializes Fulfillment, records
business/audit evidence, and enqueues deferred dispatch work.

`FulfillmentService.initializeForOrder()` joins that transaction with default
REQUIRED propagation. No external HTTP call, sleep, future join, or ordinary
`REQUIRES_NEW` boundary was found in the inspected path.

The concrete expansion seam was
`FulfillmentService.buildWarehouseAssessment(FulfillmentTask, Instant)`. It
queried every active Fulfillment task for the tenant using an entity graph that
also hydrates tenant, Order, Order lines, and warehouse, and only then filtered
the result to the current warehouse in Java.

Therefore a North warehouse Order could load active Coast fulfillment work and
its Order lines while the North Order transaction retained its connection.
That is unnecessary Java and persistence work inside the atomic transaction;
it is not a PostgreSQL lock or a second connection borrow.

## Direct Red Proof

`OrderFulfillmentConnectionHoldIntegrationTest` prepared 24 active Coast
Fulfillment tasks, initialized a North Order, and measured Hibernate entity
hydration inside the still-open transaction.

Against unchanged production source: **1 test, 1 failure, 0 errors/skips**.
The North initialization loaded **119 entities**, violating the proof bound
that unrelated Coast volume must not scale North assessment hydration.

The final test compares a North baseline with a second North initialization
after the 24 unrelated Coast tasks are added. Statistics are captured before
commit so after-commit dispatch, dashboard refresh, and realtime work cannot
pollute the measurement.

## Smallest Correction

`FulfillmentTaskRepository` now exposes a tenant-, warehouse-, and status-
bounded query with the same required entity graph. The assessment calls that
query directly and no longer loads tenant-wide active tasks before filtering
in Java.

Order/Inventory atomicity, transaction propagation, fulfillment calculation,
events, audit, dispatch publication, authorities, scheduler behavior, Hikari
configuration, timeouts, and infrastructure are unchanged.

## Verification

Focused verification: **23 tests passed, 0 failures/errors/skips**. The set
covers the new connection-hold regression, Inventory concurrency, Order
lifecycle transitions and inbound consistency, and Layer 2 intake/reservation.

Full backend verification: **354 tests passed, 0 failures/errors/skips**.
Backend package verification passed and produced the repackaged Spring Boot
JAR.

Documentation-link and whitespace checks are recorded with the closure commit.
Hosted served-revision and warm-runtime proof remain separate deployment gates.

## Classification and Remaining Boundary

`ORDER_FULFILLMENT_CROSS_WAREHOUSE_HYDRATION = CORRECTED_LOCALLY`

This correction prevents fulfillment initialization cost from scaling with
active work in unrelated warehouses. It does not prove the exact owners in the
historical `total=10 active=10 idle=0 waiting>0` incident, establish hosted
request latency, or prove ten concurrent Order requests maintain adequate pool
headroom.

The next bounded phase is concurrent Order/Fulfillment transaction-demand
proof: align distinct Order requests, measure peak active connections and
waiters, prove one-connection-at-a-time behavior, and preserve reservation,
Order, and Fulfillment atomicity. No broad hosted E2E should precede the exact
served-revision and warm-baseline gates.
