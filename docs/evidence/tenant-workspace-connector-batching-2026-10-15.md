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

## Exit boundary

No hosted after-measurement or browser result is claimed here. Before calling
the operator path improved in production, verify an exact Live revision and
collect bounded warm authenticated workspace client/HTTP pairs for a tenant
with a known connector count. Confirm a complete workspace DTO and compare
durations with the historical window, separating cold start and concurrent
workload. A 204 must be classified by client outcome and abort reason rather
than counted as a successful settings read. This correction does not explain
historical Hikari 10/10 starvation or close H1/H2/H7, H4/H11, or M1.
