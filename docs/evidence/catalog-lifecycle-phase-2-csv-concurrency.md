# Catalog Lifecycle Phase 2 Evidence

**Scope:** CSV transaction truth, catalog side-effect boundaries, tenant-qualified identity, and concurrent Product writes.

**Verification date:** 2026-08-28

**Phase status:** Complete. Catalog Phase 3 was not started.

## 1. Starting HEAD

`1de2bb4872e4926ffbc09e87968953e1f29c5eb2`

The Phase 1 evidence history remains in [`catalog-lifecycle-phase-1-authority-identity.md`](catalog-lifecycle-phase-1-authority-identity.md). Existing unrelated worktree changes were preserved and were not staged.

## 2. CSV Transaction Boundary

`ProductService.importProducts` is an outer `@Transactional` operation. It parses and validates each row, accumulates row results, flushes Product writes, records the catalog business event, queues the operational dispatch work item, records the catalog audit entry, and returns the import response only after those operations complete.

Row-level validation and duplicate failures are represented in the response and do not abort other valid rows. A failure after the row loop, including a final persistence failure in the catalog evidence path, propagates out of the transaction and prevents a success response.

The production defect found in this phase was that `CoreIdentityWriteIsolationService` always opened a `REQUIRES_NEW` transaction. That allowed catalog evidence rows to commit independently before a later audit failure rolled back the Product transaction. The smallest correction makes writes join the active transaction and flush there; the existing isolated sequence-repair path remains for calls made without an active transaction.

## 3. Mixed CSV Result

Fixture: three rows, two valid rows and one row with a missing required name.

- HTTP result: `200 OK`.
- Response: `totalRows=3`, `created=2`, `updated=0`, `failed=1`.
- Row statuses: `CREATED`, `FAILED`, `CREATED`.
- The response includes the two created catalog identities.

## 4. Committed DB Result

After the mixed import, the two valid catalog SKUs were present for `STARTER-OPS`; the invalid row SKU was absent. This checks the response against persisted tenant-scoped Product state rather than trusting the response alone.

## 5. Duplicate Same-File Result

Fixture: `phase2-dup-100` and `PHASE2-DUP-100` in one file.

- HTTP result: `200 OK` with one created row and one failed row.
- The second row is explicitly `FAILED` with the duplicate-in-file outcome.
- Exactly one logical Product exists after normalization.

## 6. Existing Product CSV Update

An existing Product was created, then imported through CSV with changed name and category.

- HTTP result: `200 OK` with `updated=1`.
- Product database ID stayed unchanged.
- Tenant ownership stayed `STARTER-OPS`.
- Name and category changed to the CSV values.

## 7. Cross-Tenant Same SKU

The test provisions a separate synthetic tenant through the supported platform-admin endpoint. The same catalog SKU can exist once in each tenant because the persisted internal identity is tenant-qualified. No cross-tenant Product ownership was observed.

The cross-tenant fixture uses `PHASE2-SECOND-OPS`; its credentials and token are test-only values inside the test source and are not deployment secrets.

## 8. Forced Final Transaction Failure

The test temporarily adds an H2-only check constraint that rejects the final `product-catalog` audit target for `phase2-rollback.csv`, then imports one otherwise valid Product.

- The request returns `409 Conflict` through the existing catalog conflict mapping.
- The Product is absent after the request.
- No successful catalog event, catalog audit row, or operational dispatch work item remains from the rejected transaction.
- The temporary constraint is dropped in a `finally` block.

This is a test fixture only; it does not modify production schema or data.

## 9. Response Versus Commit Truth

No successful CSV response is returned when the final transaction boundary fails. The Product response and database state therefore agree for the tested failure path: failure is surfaced instead of reporting rows that did not commit.

## 10. Audit/Event Rollback Truth

The forced final-audit failure verifies rollback of the Product, business-event, audit, and operational-dispatch writes as one catalog transaction. The assertion excludes any intentional generic request-rejection audit produced by the existing exception boundary.

## 11. After-Commit Realtime Truth

The source path is:

```text
catalog transaction commits
  -> OperationalStateChangedEvent
  -> OperationalStateChangeListener
  -> AFTER_COMMIT asynchronous dispatch drain
  -> realtime operational update
```

`OperationalStateChangeListener` is configured as an asynchronous `@TransactionalEventListener` at `AFTER_COMMIT`. The transaction test proves that the dispatch work item cannot survive a failed catalog commit, and the existing `OperationalStateChangeListenerTest`, `OperationalDispatchQueueServiceTest`, and websocket tests passed in the full backend run. No browser-hosted realtime proof was rerun for this Catalog-only phase.

## 12. Concurrent Same-SKU Creation

Two concurrent `STARTER-OPS` creates for `PHASE2-CONCURRENT-100` produced:

- exactly one `201 Created`;
- exactly one `409 Conflict`;
- exactly one persisted Product.

The database uniqueness boundary is therefore effective for the tested same-SKU race.

## 13. Concurrent Case-Variant Creation

Two concurrent creates for `phase2-case-100` and `PHASE2-CASE-100` produced:

- exactly one `201 Created`;
- exactly one `409 Conflict`;
- exactly one persisted logical SKU after normalization.

The losing request is not silently treated as success.

## 14. Concurrent Update Semantics

Two concurrent updates targeted the same tenant Product ID.

- Both requests returned `200 OK`.
- The final name was one of the two submitted values, as expected for last-commit-wins field updates without a version/ETag contract.
- Product ID, tenant ownership, and tenant-qualified SKU remained unchanged.

The current contract is identity-safe but does not provide optimistic version conflict detection.

## 15. Orphan Adoption Competition

The test inserts a tenantless legacy Product through the test database fixture, adopts it into `STARTER-OPS`, then submits the same catalog SKU from `PHASE2-SECOND-OPS`.

- The first tenant adopts the orphan.
- The second tenant creates its own tenant-owned Product.
- The original Product is not switched to the second tenant.

This proves ownership cannot be taken over after adoption in the exercised sequential competition path.

## 16. Orphan Versus Owned Product Collision

Orphan matching considers tenantless legacy rows and does not treat an already tenant-owned Product in another tenant as an orphan candidate. The Phase 1 orphan adoption tests and the Phase 2 two-tenant assertion passed. No cross-tenant takeover was demonstrated.

## 17. Operational Side Effects

The mixed import test captured tenant counts before and after the successful catalog import. Inventory, customer orders, alerts, and recommendations counts were unchanged.

The intended catalog evidence path remains separate: catalog business event, catalog audit, and operational dispatch work item. Product CSV import does not create inventory quantities, orders, live alerts, or live recommendations.

## 18. Production Defects

One real production transaction-boundary defect was found and corrected:

- `CoreIdentityWriteIsolationService` previously forced event, audit, and dispatch writes into independent `REQUIRES_NEW` transactions even when called inside the outer catalog import transaction.
- A final audit failure could therefore leave a business event or dispatch row committed after Product rollback.

No API route, DTO contract, frontend behavior, tenant rule, or database migration was changed.

## 19. Test and Fixture Defects

The Phase 2 harness required corrections during implementation:

- Mockito was not available in the repository, so the proof uses the existing Spring test context and a temporary database constraint instead.
- The forced failure was moved from a source-wide business-event constraint to a filename-specific audit constraint so existing fixture rows are not accidentally invalidated.
- Existing baseline counts are used so intentional request-rejection audit rows do not look like leaked successful writes.
- Tenant and Product ownership assertions use JDBC where a lazy JPA Tenant proxy would otherwise make the assertion misleading.
- The case-variant race now asserts the expected losing `409` response, not only the one-row outcome.

## 20. Fixes Applied

Production:

- Active catalog transactions now keep Product, catalog business event, audit, and operational dispatch writes in the same commit boundary.
- An `EntityManager.flush()` makes the final write failure observable before returning from the transaction.

Test-only:

- Added focused CSV/concurrency integration coverage.
- Added explicit no-side-effect count assertions.
- Added explicit case-variant conflict assertion.

## 21. Focused Tests

Command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd -Dtest=CatalogConcurrencyIntegrationTest test
```

Result after the final test tightening: `8` tests run, `0` failures, `0` errors, `BUILD SUCCESS`.

The run validated Flyway migrations `v1` through `v7`, Hikari startup, JPA initialization, the Spring test dispatcher, and the in-memory websocket broker under the test profile.

## 22. Full Backend Result

Command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd test
```

Result: `179` tests run, `0` failures, `0` errors, `0` skipped, `BUILD SUCCESS`.

The full run passed the existing security, websocket, deployment-hardening, inventory, MVP flow, access-boundary, migration, and operational dispatch suites in addition to the Catalog Phase 2 suite.

## 23. Frontend Checks

No frontend files were changed for this phase. Frontend lint/build/verify and hosted Playwright proof were not rerun. The existing hosted proof baseline is prior evidence, not new Phase 2 evidence.

## 24. Files Changed

Phase 2 files:

- [`CoreIdentityWriteIsolationService.java`](../../backend/src/main/java/com/synapsecore/domain/service/CoreIdentityWriteIsolationService.java) — production transaction-boundary correction.
- [`CatalogConcurrencyIntegrationTest.java`](../../backend/src/test/java/com/synapsecore/CatalogConcurrencyIntegrationTest.java) — focused CSV, rollback, concurrency, ownership, and side-effect proof.
- [`catalog-lifecycle-phase-2-csv-concurrency.md`](catalog-lifecycle-phase-2-csv-concurrency.md) — this evidence record.

Pre-existing unrelated worktree items were not changed or staged:

- `frontend/Dockerfile`
- `.gitattributes`
- `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`
- `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`

The existing Phase 1 evidence document was preserved.

## 25. Commits

No commit was created in this phase. The starting HEAD remains `1de2bb4872e4926ffbc09e87968953e1f29c5eb2`.

## 26. Deployment

No deployment was performed. Hosted proof was not rerun because the phase changed a backend transaction seam and added backend tests only; deployment requires an explicit follow-up decision after reviewing this evidence and the remaining limitations.

## 27. Critical Blockers

None identified.

## 28. High Blockers

None identified for the tested Catalog Phase 2 scope.

## 29. Medium/Low Limitations

- The focused and full backend suites use the repository's H2 `test` profile, not a live PostgreSQL instance.
- The concurrency tests use concurrent requests within one Spring test process; they do not establish a multi-process or multi-node PostgreSQL isolation guarantee.
- Orphan competition proof is sequential ownership competition, not two simultaneous orphan adopters racing on PostgreSQL.
- Concurrent updates intentionally document last-commit-wins behavior; there is no optimistic locking/version conflict contract.
- After-commit dispatch is source- and unit-tested here, but the deployed browser realtime path was not rerun in this phase.
- No frontend checks or hosted proof were needed because no frontend files changed.

## 30. Readiness for Catalog Phase 3

`READY`, subject to the documented limitations above. Phase 3 itself was not started.

## 31. Verdict

**PHASE 2 PASSED — CSV TRANSACTION TRUTH, SIDE-EFFECT BOUNDARIES, AND CONCURRENCY VERIFIED — READY FOR CATALOG PHASE 3.**

The key correction is that a final catalog evidence failure now rolls back the complete catalog write path instead of allowing independently committed event or dispatch evidence to survive a failed Product transaction.
