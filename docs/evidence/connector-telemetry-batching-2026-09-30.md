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

Exact-SHA CI [run 36713480849](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/36713480849)
completed successfully for `d966da6643f00097a2deb0c1c0e21a655bddc52b`.
Render showed that SHA Live as deploy `dep-daufq3mq1p3s738ibeu0` after a 4m35s
rollout, before the following read-only hosted check. The existing proof tenant
was used; no catalog, connector, inventory, or other business record was changed.

At 12:19 UTC, authenticated login returned 200 in 5,845 ms. Direct
`GET /api/integrations/orders/connectors` returned 200 with 58 connectors in
2,022 ms at the client (request `926961d3-7a97-4734-8c8d-b0903bc012f7`),
versus the earlier single 9,233 ms client sample with 58 connectors. This is a
bounded comparison, not a distribution or capacity guarantee. No slow-handler
line for that direct request was present in the searched Render logs.

`GET /api/dashboard/snapshot` returned 200 with 58 connector responses in
17,363 ms at the client (request `e17d293e-0079-4368-882b-fd6094bb3f31`).
The same request's Render trace recorded connector composition at **499 ms**,
down from the earlier **8,532 ms** sample. However, total snapshot composition
was **13,806 ms** and the HTTP handler was **16,875 ms**. The new section trace
showed audit=1,900 ms, summary=1,891 ms, fulfillment=1,299 ms,
incidents=994 ms, scenarioNotifications=907 ms, alertFeed=899 ms, sla=802 ms,
inventory=702 ms, replay=700 ms, and other sections below 700 ms each.
No single remaining section dominates like connector telemetry did. The
completion-time Hikari snapshot was total=10, active=0, idle=10, waiting=0;
it does not establish borrow/release timing or rule out earlier overlap.

The connector amplification is corrected and locally/CI verified, with a
measured hosted improvement in this window. Overall snapshot latency remains
unacceptable for an M1 exit claim. Next, correlate representative warm snapshot
and login requests with per-request connection acquisition/hold and resource
evidence before attributing the distributed residual time to PostgreSQL, Java,
or the Free instance's 0.15 CPU/512 MB limit. Inspect PostgreSQL plans only if
measured SQL state points there. H1/H7 and the rest of M1 remain open.
