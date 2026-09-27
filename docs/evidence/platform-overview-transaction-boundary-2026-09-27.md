# Platform overview transaction boundary - 2026-09-27

## Evidence and decision

The warm hosted trace in [warm-runtime pool pressure](warm-runtime-pool-pressure-2026-09-27.md)
recorded a platform overview response taking 7,051 ms. The trace did not prove
that this request owned one of the ten JDBC connections at the later pool-pressure
instant. Inspection found that `PlatformControlPlaneService.getOverview()` was
annotated `@Transactional(readOnly = true)` while composing Runtime, all tenant
summaries, and recent activity. Runtime performs dispatch counts and readiness
evaluation; each tenant summary performs seven count queries. The outer
transaction was not a consistent cross-query snapshot under ordinary
`READ_COMMITTED` isolation, but it could retain a connection across those
independent sections after the first database call.

The smallest bounded change removes only the outer transaction. Existing
repository calls retain their normal query behavior. Standalone proxied calls
to `getTenants()` and `getActivity()` keep their read-only transactions; calls
from `getOverview()` to those same-class methods do not pass through that proxy.
This does not reduce the number of tenant count
queries or prove a hosted latency improvement. It narrows the maximum
service-owned connection-retention interval for overview composition.

## Verification and limits

`PlatformTenantAccessBoundaryIntegrationTest` passed locally: 35 tests,
0 failures, 0 errors, 0 skipped. Its new regression assertion checks Spring's
effective transaction attribute for `getOverview()` and calls the authorized
endpoint, asserting Runtime and tenant content. Existing tests cover the
platform-owner session boundary and prevent tenant sessions from inheriting
platform authority.

The full local backend suite also passed: 392 tests across 67 Surefire reports,
0 failures, 0 errors, 0 skipped. The documentation link check found no missing
local links among 911 checked links, and `git diff --check` found no whitespace
errors.

This is **locally verified**, not yet deployed or hosted-verified. H1 remains
open: the ten JDBC holders, PostgreSQL PIDs, and Java owners at the 10/10
Hikari event are still unknown. H2 remains open: platform tenant summaries
still have per-tenant query amplification, and representative data-size and
latency budgets have not been measured. Do not attribute the historical pool
starvation to this overview transaction or close M1 from this change.
