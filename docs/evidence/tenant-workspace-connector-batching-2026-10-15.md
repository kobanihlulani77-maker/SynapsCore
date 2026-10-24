# Tenant workspace connector composition boundary, October 15, 2026

## Operator outcome and historical signal

`GET /api/access/admin/workspace` is the Tenant Admin settings and support
bootstrap read. Its response must retain tenant-wide connector configuration,
including when the admin's operational warehouse scope is narrower. A failed or
empty response leaves the workspace settings surface without its authoritative
connector state.

Render application logs for the October 13 `jlls7` instance include workspace
reads at 25,606 ms (204, request `19399af9-3e37-4538-999b-d4f92675fc45`),
30,406 ms (200, `95969023-1c52-4df9-b714-69795bd21a66`), and 30,404 ms
(200, `02e938ec-bf0e-484e-80ad-3c927991e6a0`). Later the same instance
logged 18,408 and 19,128 ms reads with 204. These are server-side durations,
not paired browser timings. Request-thread CPU was hundreds of milliseconds,
and the completion-time Hikari snapshots had zero waiters; neither observation
locates the in-flight wait. The browser outcome and abort reason for the 204s
remain unknown.

An October 20 source check narrows the server-side meaning of those 204
warnings. `AccessController.getWorkspace()` returns a workspace DTO, not an
intentional no-content response. [`RequestTraceFilter`](../../backend/src/main/java/com/synapsecore/audit/RequestTraceFilter.java)
sets 204 when an uncommitted response encounters an abort-like exception,
including a broken pipe or connection reset; its
[`RequestTraceFilterTest`](../../backend/src/test/java/com/synapsecore/audit/RequestTraceFilterTest.java)
asserts that a broken-pipe path records 204 in the response and HTTP metrics.
The [API exception handler](../../backend/src/main/java/com/synapsecore/api/controller/ApiExceptionHandler.java)
and [framework error controller](../../backend/src/main/java/com/synapsecore/api/controller/FrameworkErrorController.java)
also map recognized client-abort paths to 204. Thus a server-side 204 warning
can be an abort-handling classification rather than a successful empty
workspace. The historical logs do not identify which abort path ran, who
closed the connection, whether any 204 reached the browser, or what the
operator saw. This source/test finding does not explain the preceding
18-25-second server wall time or the historical pool holders.
The focused local `RequestTraceFilterTest` run on October 20 passed 15/15;
that is local abort-classification evidence, not a reproduction of the
historical hosted request.

Source inspection gives a narrower conditional conclusion: if a browser
receives a 204 for this GET, `readResponsePayload()` returns `null`, and
`fetchAccessAdminData()` cannot read `workspace.tenantName`; its caller catches
the error and shows the workspace error state. This is not proof that the
historical browser received either 204 or that the abort began in the browser.

The earlier [connector telemetry batching proof](connector-telemetry-batching-2026-09-30.md)
showed nine telemetry queries per connector before the general list was batched.
Source inspection found that workspace composition still mapped each connector
through that individual `describeConnector()` path. This is a concrete query
amplifier, but the historical Render warnings do not identify its exact share
of wall time or prove it caused either 204.

## Bounded red/green proof

The authenticated `TenantWorkspaceAdminPhase2IntegrationTest` compared an
empty workspace read with a read after adding 12 connectors. With the original
workspace composition, the second read prepared **110 more statements** than
the first; the new bound of at most 12 failed. This is local H2 evidence of
connector-count-dependent SQL, not hosted PostgreSQL timing.

The correction routes workspace connector composition through the existing
batched telemetry and support-owner reader. It does not substitute the general
`getConnectors()` endpoint: that endpoint filters warehouse-scoped operators,
whereas Tenant Admin workspace settings must retain all connectors in the
current tenant. The new method selects only the current tenant, keeps the
existing type/source ordering, and uses the same batch formatter as the normal
list. The individual descriptor and exact-readback paths remain unchanged.

After the correction, the same 12-connector statement-growth assertion passed.
The test also confirmed that a warehouse-scoped Tenant Admin still receives all
12 workspace connectors, while a connector belonging to another tenant is not
returned. The adjacent focused H2 run passed 41/41 tests across workspace,
connector batching, and platform/tenant access boundaries before the final
cross-tenant fixture was added; the final focused workspace test then passed
with that fixture. The first full backend run failed two SLA concurrency tests
because their null-backed connector-service double overrode only the former
workspace list method. After the double returned the same empty list for the
new workspace method, that class passed. The subsequent `backend`-directory
`.\mvnw.cmd -q test`
run passed **412 tests across 72 suites, 0 failures, 0 errors, 0 skips**. This
is local H2 regression evidence, not PostgreSQL, CI, or hosted performance proof.

## Hosted after-boundary on exact revision

Render Deploys showed merge `8128fd1` Live after a 5m21s rollout; all six
live-connection flags passed. A signed-in OWNER-ACCEPT-02 Tenant Admin browser
tab rendered Company Settings but had zero connectors, so it could not test
the high-volume batch. A separate bounded authenticated API probe used the
existing ignored local proof state for `HOSTED-PROOF-5-20260902-144935` (no
credential output or fixture mutations). At UTC 2026-10-15 10:23:28, login
succeeded but took 4,400.9 ms, so the warm baseline is imperfect. The first
workspace read returned HTTP 200 with 131 connectors in 3,813.5 ms. Five
subsequent complete reads of that same tenant returned HTTP 200 and 131
connectors each:

| UTC response end | Request ID | Client ms |
| --- | --- | ---: |
| 10:23:42.280 | `06b22e84-f0e6-49e0-b89d-274aa803819e` | 5,061.4 |
| 10:23:46.737 | `5049b678-c478-44ef-817a-5687c840f6fd` | 4,428.3 |
| 10:23:51.078 | `aed756d2-9233-45e0-91ed-26aac118e6ab` | 4,325.9 |
| 10:23:55.273 | `93be0ab0-c389-4b8c-a293-ae41b1dca3f5` | 4,186.2 |
| 10:24:01.928 | `e525f596-49d7-43e3-aef3-549f67fb6637` | 6,639.5 |

The last request matches a Render warning on new Live instance `n4b55`:
HTTP 200, server 6,102 ms, request-thread CPU 96 ms, Hikari
total=10/active=2/idle=8/waiting=0 **at completion**. The client/server
duration difference is about 538 ms, but this is not a routing or network
attribution. Completion-time pool state does not establish whether this
request waited for a connection in flight. No 204 occurred in this sample.

The earlier 25-30 second warnings were different windows and workloads, so
this is not a controlled before/after speedup claim. The 131-connector path
still spends material wall time with little request-thread CPU. Source
inspection shows workspace composition also calls incident composition, which
independently reads connector telemetry; its share of this wall time is
unmeasured. The next bounded diagnostic records workspace composition section
wall/CPU timing only when it exceeds two seconds. Its request ID comes from
the existing request-trace log context; section timing alone is not exact SQL
or JDBC connection-hold timing.

## Exit boundary

After the section diagnostic is Live, capture one warm same-request trace for
a connector-bearing tenant. Investigate the dominant measured section, then
make only the smallest justified correction and repeat. A historical 204 still
requires client outcome and abort classification. The batching correction
does not explain historical Hikari 10/10 starvation or close H1/H2/H7,
H4/H11, or M1.
