# Warm-runtime pool pressure - 2026-09-27

## Controlled boundary

The backend Render deploy page showed `4f91d89` live, and the frontend static
site showed `81ceb69` live. GitHub CI for both commits completed successfully.
The six-flag live gate reported frontend, backend, DB readiness, auth, WebSocket,
and proof allowance true. A single focused hosted Replay proof then completed
authenticated session, dashboard summary/snapshot, and Runtime warm-up before
entering the test. This was not a cold-start failure. The test was interrupted
after the first pool-pressure trigger; its outcome is **not** pass or fail.

## UTC evidence from the backend instance

All entries below came from Render application logs for instance `wrwp4`.
The new bounded request diagnostic was present, which confirms the backend
instrumentation ran on the deployed revision.

| UTC | Observation |
| --- | --- |
| `14:23:38.084` | `GET /api/dashboard/snapshot` returned 200 after 13,879 ms; request `9f9c0a1b-c2ed-4ba7-8a62-0c11d70db330`; end-of-request Hikari active 0, idle 10, waiting 0. |
| `14:23:59.785` | `GET /api/system/runtime` returned 200 after 21,367 ms; request `bc905de4-a4c9-4c61-aceb-2bfa84c20151`; end-of-request Hikari active 1, idle 9, waiting 0. |
| `14:24:26.678` | Platform overview returned 200 after 7,051 ms; request `879979a8-b0ad-421a-a9f7-20f90a10b7c6`; Hikari active 2, idle 8, waiting 0 at completion. |
| `14:24:32.678` | Scheduled dispatch START/COMPLETE samples showed Hikari active 6, idle 4, waiting 0; dispatch itself processed 0. |
| `14:24:34.179` | Scheduled dispatch START/COMPLETE samples showed Hikari **total 10, active 10, idle 0, waiting 1**; dispatch processed 0 and completed in 0 ms. This is the first captured pool-pressure trigger. |
| `14:24:35.779` | The next dispatch sample showed active 6, idle 4, waiting 0. The observed saturation was transient in these samples. |
| `14:24:55.282` | Dashboard snapshot returned 200 after 23,000 ms; request `58e4eb26-c64f-46c7-92ef-458bb8a7477e`; Hikari active 7, idle 3, waiting 0 at completion. |
| `14:24:55.380` | Another snapshot returned 200 after 12,301 ms; request `ba0feb7b-46b4-4adc-934a-e6c7fe3b7db0`; Hikari active 7, idle 3, waiting 0 at completion. |
| `14:24:55.483` | Another snapshot returned 200 after 33,602 ms; request `c48ed1b1-d122-4abd-b24c-656598b3c4d1`; Hikari active 7, idle 3, waiting 0 at completion. |

The slow-request filter reported `identityMs=0` and nearly all measured time
in its `handlerMs` bucket for these requests. That bucket includes controller,
service, DB acquisition/SQL, serialization, and downstream filters; it does
**not** prove Java CPU or a held transaction. The Hikari values are snapshots
at log time, not a continuous trace of each request's connection ownership.

## Classification and next proof

`ACTIVE_RUNTIME_HIKARI_PRESSURE` is proven at one instant after a warm baseline.
`ACTIVE_RUNTIME_BACKEND_LATENCY` is also proven, including when end-of-request
Hikari had idle capacity. The evidence does **not** prove that all slow calls
were caused by pool waiting, or that dispatch owned any of the ten connections.
The zero-work dispatch task merely exposed the pool MXBean state. Platform
overview requests were also active; their timing alone does not establish
that they held JDBC connections. The historical sustained Hikari timeout and
this transient point sample must not be conflated.

The next exact boundary is to map the ten JDBC holders during a recurrence to
PostgreSQL PIDs and active/idle-in-transaction/lock state, then to request IDs
or scheduler work and the owning Java transactions. A simultaneous thread dump
or equivalent bounded runtime trace is needed for non-SQL time. Do not widen
Hikari, raise timeouts, or change transaction boundaries based only on these
completion-time pool snapshots. H1/H2/H7 and hosted Replay convergence remain
open; no capacity or transaction fix was applied in this capture.
