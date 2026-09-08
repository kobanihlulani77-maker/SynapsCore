# Timeout Recovery - Product Preflight Connection Demand

## Scope and Starting Point

Starting HEAD: `fee1e7b` on 2026-09-08, after the
[SLA caller-boundary local/CI gate](timeout-recovery-sla-readonly-callers.md).
This is the next bounded HTTP holder investigation from the
[recovery map](../hosted-timeout-recovery-map.md), not a broad hosted E2E run.

## Exact Owner Chain

Both production callsites are non-transactional controller methods:

```text
POST /api/products or POST /api/products/import
  -> ProductController (tenant-admin authorization retained)
  -> ProductService.createProduct / importProducts [old REQUIRED transaction]
  -> catalog transaction borrows connection A
  -> IdentitySequenceMigrationService.synchronizeCoreIdentitySequences [REQUIRES_NEW]
  -> suspends A and requests connection B
  -> sequence method / SQL cannot start until B is available
```

The previous independent identity-repair correction did not remove these
product service transaction wrappers. Catalog ownership startup migration
already performs its sequence preflight before its separate transaction.

## Direct Red Proof

`ProductPreflightConnectionDemandTest` uses the actual ProductService class,
actual sequence service, Spring annotation transaction proxies,
JpaTransactionManager, Hibernate, and Hikari over an isolated H2 database.
The transaction manager aligns ten first connection acquisitions with a
barrier. It does not inject a database lock or slow SQL.

A tenant-resolution probe immediately after preflight stops the call with a
known exception after inserting a rollback witness inside the catalog write
transaction. This proves entry, transaction presence, and rollback, not a
successful end-to-end product HTTP response. Unused downstream collaborators
are deliberately absent. Existing catalog integration tests cover the real
write, import, and downstream behavior.

Against unchanged production: **4 tests, 4 failures, 0 errors/skips**.
Log: `backend/target/product-preflight-red.log`.

- A single create/import used two simultaneously held connections, not one.
- Ten creates: peak active 10, peak waiters 10, catalog boundary reached 0/10.
- Ten imports: peak active 10, peak waiters 10, catalog boundary reached 0/10.
- Hikari acquisition timed out at the test-only 1500 ms limit. Production's
  connection timeout was not changed.

The exact deadlock topology is ten outer catalog transactions retaining their
connections while all ten independent preflight transactions wait to borrow.
No PostgreSQL lock is necessary to reproduce it.

## Smallest Correction

Product create/import now finish the independent sequence preflight before
starting the catalog write with the existing application TransactionTemplate
(REQUIRED). The original catalog body remains inside that transaction.
Update-product behavior is unchanged. No new independent transaction was added.

Product, business event, audit, and dispatch work retain their shared write
transaction; failure rolls it back. Sequence logic, synchronization, advisory
lock SQL, roles, tenant boundaries, CSV behavior, retries, pool size, scheduler
settings, infrastructure, and frontend code are unchanged.

The corrected initial overlap proof reached catalog work 10/10 for both create
and import, with peak active 10 and peak acquisition waiters 0. Each top-level
request uses one connection at a time rather than retaining an outer connection
while requesting a second one.

Two additional tests preserve explicit outer-caller rollback. An already-active
caller still retains its own connection during independent preflight (depth 2).
No current production caller of these methods has that shape; only the two
controller callsites were found. This residual contract is documented rather
than claiming all possible nested callers are single-connection.

## Verification

Final focused tests: **49 passed, 0 failures/errors/skips**. These include six
new preflight/rollback cases, six identity tests, eight catalog-concurrency
tests, five downstream-boundary tests, and 24 production-hardening tests.
Log: `backend/target/product-preflight-focused-final.log`.

In this final focused run, each ten-request case reached catalog work 10/10;
peak active was 10 and peak waiters was 2. Transient contention at a pool's
capacity is not the old circular hold-and-wait: each request now releases its
preflight connection before acquiring its catalog connection, and the tests
prove progress and complete release. Zero queueing is not claimed.

Full backend: **348 tests passed, 0 failures/errors/skips**, BUILD SUCCESS;
5m20s, completed `2026-09-08T13:14:34Z`.
Log: `backend/target/product-preflight-full-suite.log`.
Backend packaging passed (exit 0); log: `backend/target/product-preflight-package.log`.
CI is pending at this pre-push checkpoint. No hosted verification or served
revision is claimed.

## Remaining Boundaries

This proves a concrete HTTP entrypoint connection-demand defect locally. It
does not identify the ten historical hosted requests, measure PostgreSQL
advisory-lock duration, or establish production throughput. H2 does not prove
PostgreSQL sequence concurrency behavior; hosted verification remains gated.

Chrome is not connected in the current tool inventory. Do not start broad
hosted proof or infer deployment from push/CI. Confirm the exact served revision
and warm baseline before a bounded hosted validation.

The synchronized sequence service can still serialize callers after they borrow
a connection, and large catalog imports still perform their row work in one
transaction. These are separate duration/throughput questions, not erased by
removing this double-borrow deadlock. They require evidence before any further
refactor. Historical Hikari attribution and overall recovery remain OPEN.
