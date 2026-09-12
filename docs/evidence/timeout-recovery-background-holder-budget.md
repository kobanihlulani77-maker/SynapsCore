# Timeout Recovery - Background Holder Budget

## Bounded Question

Can SynapseCore's configured background executors, by themselves on one default
application instance, supply the ten simultaneous long-running owners observed
in the historical Hikari starvation window?

## Source Ownership Map

`SchedulingConfig` assigns all ordinary scheduled jobs to one
`SynapseScheduled-*` worker by default. Replay automation and scheduled connector
pulls therefore serialize with other main-scheduler jobs.

Recommendation reconciliation has one separate
`SynapseRecommendationScheduled-*` worker. The completed hosted reconciliation
capture remains healthy and is not reopened by this source check.

The operational dispatch executor has core size one and maximum size two, but
all calls enter one instance-level `AtomicBoolean` drainer guard. Only one caller
can execute queue selection and fan-out at a time; another scheduled or async
caller returns immediately while draining is active.

Therefore, on one application instance using the checked defaults, the
meaningful background work budget is bounded to:

- one ordinary scheduled job;
- one recommendation reconciliation job;
- one active dispatch drain.

This is an executor/entrypoint bound, not an assertion that all three always
hold JDBC connections or that each has equal duration.

## Focused Verification

**15 tests passed, 0 failures/errors/skips**:

- 3 background concurrency configuration tests;
- 2 dispatch service ownership/context tests;
- 6 recommendation reconciliation observability tests;
- 4 Replay atomicity tests.

Log: `backend/target/background-holder-budget-focused.log`.

The preceding full suite on the same production source passed **353 tests, 0
failures/errors/skips**. No production, test, frontend, configuration, pool,
timeout, scheduler, or infrastructure change was made in this phase.

## Classification

`BACKGROUND_EXECUTORS_ALONE_AS_TEN_HOLDERS = RULED_DOWN_FOR_ONE_DEFAULT_INSTANCE`

This does not rule out overlap between the bounded background budget and HTTP
transactions, multiple application instances, or a single operation retaining
more than one connection through nested transaction propagation. Product and
identity double-borrow paths were handled in their own earlier phases.

The next holder investigation should measure the required atomic Order and
Fulfillment write path, especially work between SQL calls, without splitting
its transaction before a concrete long-hold seam is proven.
