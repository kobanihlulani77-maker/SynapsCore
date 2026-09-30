# Hosted connector telemetry amplification - 2026-09-30

## Deployment and warm boundary

Render deploy `dep-dauf00vf3r2c73erpu1g` reported `09e963f` Live after a
4m50s rollout. Exact-SHA GitHub CI succeeded and the six-flag public readiness
check passed. The following were single, authenticated reads against the
existing proof tenant; no tenant, connector, order, or inventory was changed.
The proof credentials remained in the ignored local state and were not printed.

## Correlated observations

At 11:25 UTC, request `c432eb05-eca5-46b5-ad52-683e86cc9ed5` returned
`GET /api/dashboard/snapshot` 200 in 16,998 ms at the client. The Render
HTTP trace recorded 16,598 ms in the handler. The new composition trace
recorded 14,091 ms; `connectors=8,532` ms and `audit=1,660` ms. The remaining
sections were each below 700 ms. Composition and request duration differed by
about 2.5 seconds after the timed service method; this capture does not identify
whether that difference was serialization or other handler work. The Hikari
snapshot at HTTP completion was 0 active/10 idle/0 waiting. A completion-time
sample does not prove the pool was idle throughout the request.

At 11:27 UTC, request `4ac1f2db-dc03-49ea-bb7f-0bf147971d47` returned
`GET /api/integrations/orders/connectors` 200 with 58 connector responses in
9,233 ms at the client and 8,834 ms in the Render handler. Another bounded
comparison returned the 58-connector list in 7,726 ms and a filtered
one-connector list in 781 ms. These are individual samples, not p95 or a
capacity envelope.

## Source-level cause and limit

`IntegrationConnectorService.getConnectors()` maps each selected connector
through `describeConnector()` inside one read-only transaction.
`buildTelemetry()` performs six latest-record lookups and three count queries
per connector; `resolveSupportOwner()` can add a further lookup. For 58
connectors, this path can issue at least 522 telemetry queries, apart from
connector selection, session/operator resolution, and optional owner lookups.
The list is unpaginated. Its query count grows linearly with connector count;
the direct list and filtered comparison are consistent with this amplification.

This identifies an H2 read-composition defect and a plausible contributor to
long-held read connections, not the exact historical ten Hikari holders.
Neither SQL execution time nor borrow/release lifetime was captured for these
requests. No PostgreSQL lock, missing index, or backend CPU saturation may be
asserted from this trace alone.

## Next correction and proof

Batch tenant-scoped connector telemetry by connector identity so query count
stays bounded as connector count grows. Preserve health status, latest failure,
import status, replay counts/age, support owner, warehouse visibility, and
exact-readback semantics. Do not load unbounded inbound/replay history into
Java or parallelize per-connector queries across the ten-connection pool.
Prove response equivalence for mixed status/tie/tenant/warehouse fixtures and
assert bounded query count. Then compare warm hosted 58-connector list and
snapshot latency on the exact deployed revision, with Hikari/DB activity and
request IDs in the same window. Revisit indexes only if the batched query plan
still shows a measured database bottleneck.
