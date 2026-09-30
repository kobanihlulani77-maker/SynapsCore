# Platform tenant count query bounds - 2026-09-30

## Boundary and change

`PlatformControlPlaneService.getTenants()` previously issued seven count queries
for every tenant after loading the tenant list. The platform overview calls this
method as one of its sections, so its count-query demand grew as `1 + 7N` for
`N` tenants, before Runtime and Activity work. This is a proven code-level
amplification, not proof that it owned the historical ten occupied Hikari
connections.

The seven counts now use tenant-grouped repository queries, followed by one
tenant list query. The count-query demand is `7 + 1`, independent of tenant
count. The grouping normalizes tenant codes for the same case-insensitive
matching used by the former scoped counts. Missing groups remain zero; inactive
tenant status and attention-status precedence are unchanged. Null tenant codes
in inbound/replay records are excluded because the old per-tenant lookups could
not match them either.

## Verification and limits

- `backend/mvnw.cmd -Dtest=PlatformTenantAccessBoundaryIntegrationTest test`:
  36 tests, 0 failures, 0 errors, 0 skipped on 2026-09-30.
- The new multi-tenant integration assertion compares every exposed count and
  support state with the original tenant-scoped repository counts for two
  provisioned tenants.
- The suite uses Flyway V1-V14 on H2 in PostgreSQL mode. It validates JPQL
  startup and local behavior, not PostgreSQL query plans or hosted timing.
- Grouped scans may still be expensive with large event/replay/alert tables.
  No hosted p50/p95, query-plan, pool-headroom, or pilot workload budget is
  established by this test. H1, H2, and H7 remain open.

This extends the earlier [overview transaction boundary](platform-overview-transaction-boundary-2026-09-27.md)
without reclassifying the historical Hikari event or closing M1.
