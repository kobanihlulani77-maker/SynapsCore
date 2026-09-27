# Inventory first-row race proof - 2026-09-27

## Why this was revisited

While verifying the slow-HTTP request diagnostic, the full backend suite found
one failure in `InventoryConcurrencyIntegrationTest`:
`concurrentFirstRowUpdatesDoNotCreateDuplicateInventoryRows` observed
`[200, 200]` rather than its strict `[200, 409]` expectation. The focused
test passed without modification. Production inventory code was not changed.

The update endpoint first queries the product/warehouse inventory row with a
pessimistic lock. When the row is absent, there is no row to lock; both truly
overlapping reads can attempt insertion and the database unique constraint
allows one row and rejects the competing insert. Merely releasing two Java
workers from a start latch does not prove their missing-row reads overlap.
If the first request commits before the second performs that read, the second
legitimately updates the now-existing row and both requests return `200`.
That is a different schedule, not evidence of duplicate inventory rows.

## Correction and boundary

The test now installs a test-only aspect around the locked repository read for
the one target product. Both requests must finish an empty-row read before
either proceeds to insertion. The test asserts that both reached this barrier,
keeps the original strict `[200, 409]` response expectation, and still checks
that exactly one inventory row was persisted. The barrier is disarmed in a
`finally` block and does not affect production code or other products.

The corrected focused test passed locally, including its barrier assertion.
The subsequent full backend suite completed with 391 tests, zero failures,
zero errors, and zero skips on 2026-09-27. This local result does not prove
hosted PostgreSQL behavior.
The established [inventory Phase 2 evidence](inventory-lifecycle-phase-2-concurrency-order-effects.md)
remains the source for broader inventory concurrency contracts.
