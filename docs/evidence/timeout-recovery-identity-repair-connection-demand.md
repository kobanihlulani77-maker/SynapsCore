# Timeout Recovery: Identity Repair Connection Demand

Date: 2026-09-06.
Starting HEAD: `2986004157cfdd277b461785f7c296af43ef6713`.

## Verdict and Scope

One connection-starvation mechanism is reproduced locally and corrected:
an independent core-identity repair borrowed an unused wrapper connection,
then requested another connection for the sequence service's own transaction.
Ten aligned repairs could occupy all ten Hikari connections while all ten
callers waited for another connection.

This proves a defect in the current application call path. It does not identify
the ten holders in any historical hosted incident or establish that this was
the only cause of hosted timeouts. No hosted failure was induced for this work.

## Exact Owner

The affected branch starts in
[CoreIdentityWriteIsolationService](../../backend/src/main/java/com/synapsecore/domain/service/CoreIdentityWriteIsolationService.java)
when an independent write fails with `DataIntegrityViolationException`.
Audit persistence, business-event persistence, and dispatch enqueue use this
helper. Writes already inside an application transaction follow another branch.

Before the change:

```text
persistWithSequenceRepair (no existing transaction)
  -> executeRequiresNew(writeAction)
  -> real uniqueness failure; write transaction rolls back and releases
  -> synchronizeCoreIdentitySequencesSafely
     -> TransactionTemplate(REQUIRES_NEW): borrow connection A
     -> proxied IdentitySequenceMigrationService.synchronizeCoreIdentitySequences
        -> @Transactional(REQUIRES_NEW): suspend A, request connection B
        -> sequence SQL, commit B
     -> commit and release unused connection A
  -> executeRequiresNew(writeAction): one retry
```

At the controlled overlap point, each of ten callers holds A and requests B.
The held wrapper connections have no business SQL to finish. The nested calls
cannot enter the sequence method until another connection becomes available.
This is application transaction nesting, not a PostgreSQL lock test.

The sequence method's Java monitor is entered after transaction interception.
Removing the redundant wrapper does not change that monitor, the sequence
method's transaction isolation, or its PostgreSQL advisory lock.

## Reproduction

[CoreIdentityConnectionDemandTest](../../backend/src/test/java/com/synapsecore/domain/service/CoreIdentityConnectionDemandTest.java)
uses real Hikari connections, H2, Hibernate, `JpaTransactionManager`, a shared
JPA EntityManager, and Spring annotation-based transaction interception around
the actual sequence service. It does not replace the sequence method with an
unproxied stub.

A duplicate primary-key insert triggers the real recovery branch. A test-only
barrier aligns the ten repair transaction starts after their failed writes have
rolled back. It does not add slow SQL, external traffic, or a database lock.
The test uses a 1.5-second connection-acquisition timeout to bound failure time;
production Hikari settings are unchanged.

Initial JDBC reproduction: 2 tests, 2 failures, 0 errors.
Production-shaped JPA reproduction before correction: 2 tests, 2 failures,
0 errors.

Observed before correction:

| Measurement | Result |
| --- | --- |
| Peak connections for one independent repair | 2 |
| Hikari pool size for concurrent case | 10 |
| Peak active connections | 10 |
| Peak acquisition waiters | 10 |
| Failed concurrent repairs in the captured JPA run | 10 |
| Failure | Could not open JPA EntityManager for transaction |
| SQL/row lock injection | None |

The JPA run logged Hikari `total=10, active=10, idle=0` with acquisition waiters
and timeouts. This occurred before entry into the nested sequence transaction.

The earlier unit tests used an unproxied sequence-service subclass and an
unbounded `DriverManagerDataSource`, so they could not expose this connection
demand. Those existing tests remain intact.

## Smallest Correction

Call the proxied sequence service directly after the failed independent write
has rolled back. Its existing `REQUIRES_NEW` annotation owns the repair
transaction. Remove only the redundant surrounding transaction template call.

The retry still has a separate transaction. There is still only one retry.
Existing transaction writes still keep their auxiliary evidence in the same
commit or rollback. No pool, scheduler, timeout, SQL, migration, or frontend
setting changes.

## Verification

Focused command from `backend`:

```powershell
cmd /c "mvnw.cmd -q -Dtest=CoreIdentityConnectionDemandTest,CoreIdentityWriteIsolationServiceTest test"
```

Focused result after correction: **6 tests, 0 failures, 0 errors, 0 skipped**.

The four new tests verify:

- One independent repair uses one connection; the failed attempt rolls back its witness row and the successful retry commits its row.
- Ten aligned repairs finish with the ten-connection pool; every retry persists exactly once and all connections return.
- A second conflict propagates without another retry or leaked connection.
- An existing transaction rolls back its primary write and auxiliary evidence together.

Full backend suite: **322 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
Command: `cmd /c mvnw.cmd test`. Duration: 5m44s; completed
2026-09-06T11:02:47Z. The suite includes the four new connection tests and the
existing domain, security, migration, replay, Scenario, and realtime tests.
Local generated log: `backend/target/timeout-recovery-full-suite.log`.

Backend package: `cmd /c mvnw.cmd -q -DskipTests package`, exit 0, after the full
suite. Documentation links: 781 checked, none missing. `git diff --check` passes.
Frontend code was not changed; no additional frontend or hosted E2E run was
performed for this bounded local correction.

Hosted deployment and verification: pending.

## Remaining Work

These observations remain open and are not included in this correction:

- Request timing begins after session resolution in `RequestTraceFilter`; early failures/delay can escape that timer and correlation setup.
- Product watchdog inspection needs the application pool, and scheduler selection uses `getIfUnique()` while two scheduler beans exist.
- Runtime reads may synchronously drain dispatch work; historical latency attribution still needs focused proof.
- Product/import outer transactions also call a `REQUIRES_NEW` sequence method. Their connection overlap requires its own test; the helper correction does not change those entrypoints.
- Long order/fulfillment transactions, other background overlap, resource pressure, and frontend convergence retain their existing open classifications.

Recommendation reconciliation stays ruled down for its completed capture.
The 35-minute healthy control remains valid. Historical Hikari ownership is
still inconclusive. Overall timeout recovery and Consistency Phase 5.1 remain
open; whole-system repeatability is not accepted by this local test result.
