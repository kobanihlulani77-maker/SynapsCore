# Connector telemetry batching - 2026-09-30

## Boundary and cause

The prior [hosted trace](hosted-connector-telemetry-amplification-2026-09-30.md)
measured 58 connector summaries at 8,834 ms in the direct handler and 8,532 ms
in the Dashboard snapshot's connector section. The list mapped every connector
through `describeConnector()`, which made six latest-record and three count
queries per connector, plus optional support-owner lookup. That is at least 522
telemetry queries for 58 connectors. The trace did not measure SQL execution or
connection hold time, so it did not establish the historical ten Hikari holders.

## Bounded correction

The general connector-list path now reads the visible tenant's inbound, Replay,
and import telemetry through three window queries per batch of up to 200 source
names. Each query returns at most the latest relevant rows per connector, not
stored request payloads or unbounded history. Support-owner identities are
looked up together. Selection and warehouse visibility are applied before the
batch read. Counts and latest failure fields remain tenant- and connector-type
scoped. Equal-timestamp latest rows use ID as a deterministic tie-breaker.

The filtered source-plus-type exact-readback path is unchanged. Individual
`describeConnector()` callers remain on the existing path; this correction
targets the list shared by Integrations, Dashboard, and Runtime. The list itself
is still unpaginated and may need a separately justified high-volume boundary.

## Local verification

- `ConnectorTelemetryBatchReaderIntegrationTest` checks mixed latest/status
  records, inbound failure-window counts, pending/dead Replay counts, oldest
  pending item, import status, and tenant/type isolation on H2. A 58-connector
  fixture dispatches exactly three telemetry queries, rather than a query per
  connector.
- `MvpFlowIntegrationTest` compares list responses with the established
  single-connector descriptor and exercises the connector API.
- `backend/mvnw.cmd test` completed on September 30: 68 Surefire suites,
  396 tests, zero failures, zero errors, zero skips. This includes the affected
  Dashboard, Runtime, integration, and operational test lanes; it is local H2
  evidence, not hosted PostgreSQL performance evidence.

## Remaining gate

After exact-SHA CI and deployment, use one warm authenticated read against the
existing proof tenant to compare the same connector-list and snapshot endpoints
with the prior 58-connector result. Record deployed revision, connector count,
request IDs, client/handler/composition durations, and Hikari/DB state. If the
window queries remain slow, inspect PostgreSQL plans and table/index pressure
before adding an index or changing infrastructure. No hosted latency or Hikari
improvement is claimed from the local tests. H1/H7 and the rest of M1 remain open.
