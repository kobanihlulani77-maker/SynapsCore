# Tenant workspace incident composition, October 15, 2026

## Exact Live boundary

PR #18 merged as `30b23a8`; Render showed that SHA Live before the probe.
The six-flag live gate passed. The existing ignored proof identity for
`HOSTED-PROOF-5-20260902-144935` authenticated successfully, but login took
4,899.4 ms, so this is not a clean fully warm baseline. No connector or
operational fixture was created or changed. The workspace warm-up and two
subsequent GETs each returned HTTP 200 with all 131 connectors.

| Request ID | Client ms | Composition ms | Incident ms | Connector ms | Response/count ms |
| --- | ---: | ---: | ---: | ---: | ---: |
| `ff0c6db6-40ee-4b3e-a6d4-c2db212ceb25` | 5,641.3 | 4,087 | 2,092 | 986 | 698 |
| `af61c25e-eca1-418f-95ec-9c9aff0ba446` | 3,323.2 | 2,200 | 1,107 | 303 | 489 |
| `2b6a6f64-29f5-45ce-86b5-7933f31590c6` | 3,585.5 | 2,382 | 1,384 | 212 | 608 |

These requests ran at UTC 10:48:33-10:48:45 on Render instance `dnwq7`.
The warm-up's paired slow-HTTP warning reported 5,045 ms server wall,
199 ms request-thread CPU, and Hikari total=10/active=0/idle=10/waiting=0
**at completion**. Its 4,087 ms composition included 148 ms thread CPU;
its incident section included 76 ms CPU. The remaining time is not thereby
identified as SQL time, connection-acquisition wait, or a database lock.
The differences between client, HTTP, and composition durations also remain
unattributed. No 204 occurred in this sample; the historical 204 browser
outcome is still open.

Across all three traces, incident composition is the largest measured
workspace section. Source inspection shows it loads audit, replay, connector,
scenario, inbound, and failed-dispatch sources. The workspace had already
loaded tenant-wide connectors, yet incident composition called the general
connector reader again. Its existing snapshot-taking API filters the supplied
connectors by the current operator's warehouse scope when creating incidents.
The section trace does not establish that this duplicate call accounts for
the entire 1.1-2.1 second incident section.

## Smallest correction and local boundary

The workspace now passes its already-composed connector snapshot into that
existing incident API. No transaction boundary, scope predicate, Hikari
setting, timeout, or response contract was changed. In the fixed H2 fixture,
the 12-connector workspace read prepared 31 statements before this change
and 29 afterward. The new bound failed before the change and passed after.
The same test confirms a warehouse-scoped Tenant Admin still receives all
tenant connector settings, while the incident list includes the disabled
WH-NORTH connector but excludes the disabled WH-COAST connector. The existing
incident snapshot-reuse tests remain green.

Because two removed H2 statements do not explain all hosted wall time, a
slow-only nested incident composition trace now separates operator, audit,
replay, connectors, scenario notifications, inbound, dispatch, and merge
sections. This diagnostic is not exact SQL timing. Full backend/CI and
exact-revision hosted results must be appended before claiming further
improvement. H1/H2/H7/H12, historical Hikari holder mapping, and M1 remain
open.

## Exact Live after-state, October 15

PR #19 merged as `87a3e6790ba837d755dd0f0eaa24b78ad182ab11`. Render
showed it as the last successfully deployed commit and Live before the
authenticated probe. The six-flag gate passed. Readiness and unauthenticated
session preflight took 1,731.2 and 758.2 ms; login took 1,172.9 ms; the
post-login checks took 376.1 and 1,145 ms. This is a bounded active-instance
observation, not proof of a sustained workload envelope. The authenticated
runtime reports the configured static value `render-deploy`, so Render's
Deploys page, not that runtime field, establishes the exact SHA.

All three read-only workspace requests returned HTTP 200 with 131 connectors
on instance `xzd26`:

| Request ID | UTC start | Client ms | Composition ms / CPU ms | Incident ms / CPU ms | Scenario notification ms | Audit ms | Response/count ms |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `c3272aa0-325b-46c3-b6f0-b1db67f7df09` | 11:34:43.387Z | 6,926.4 | 3,706 / 111 | 1,399 / 43 | 399 | 195 | 1,206 |
| `056c57c4-f659-4021-855e-bdc317d6ea0d` | 11:34:50.340Z | 4,214.0 | 3,099 / 81 | 1,304 / 39 | 498 | 300 | 701 |
| `0994428b-ba43-4b28-b4fc-6bc2925675f6` | 11:34:54.597Z | 4,344.6 | 3,101 / 79 | 1,600 / 39 | 502 | 400 | 802 |

The warm-up's same-request HTTP warning reported 5,303 ms server wall,
177 ms request-thread CPU, and Hikari total=10/active=1/idle=9/waiting=0
**at completion**. The client/server difference and the remaining time outside
workspace composition are not attributed. The two later requests did not
produce a slow-HTTP warning; their client timing and composition logs share
request IDs, but there is no matching server total to invent.

The nested incident logs have **no `connectors` section** when called from
the corrected workspace path, confirming snapshot reuse. Incident composition
still took 1.3-1.6 seconds. Its other measured sub-sections included replay
(109-304 ms), inbound (7-102 ms), and failed dispatch (100-200 ms). The
pre-change three-request window was 3.3-5.6 seconds client-side with
1.1-2.1 seconds in incidents. These separate small samples overlap and are
not a controlled like-for-like latency improvement or regression proof.
The two-query H2 reduction is real locally; its hosted wall-time benefit is
unproven. This window does not reproduce historical Hikari 10/10 starvation.

Source inspection shows `ScenarioHistoryService.getScenarioNotifications()`
first scans and may apply pending SLA escalations, then reads active and
acknowledged notifications. The 399-502 ms outer subsection does not identify
which step dominates, whether escalation wrote anything, SQL execution time,
or connection hold. If escalation is needed, the separate
`ScenarioSlaEscalationService.escalateIfEligible()` method owns the short
transaction that rechecks the row under a tenant-scoped lock and records the
event; the hosted sample does not show that this path ran. A slow-only split
of those existing steps is the next
bounded diagnostic; it does not alter Scenario governance or operational
state. Continue one causal boundary at a time. H1/H2/H7/H12 and M1 remain
open; M2 entry remains blocked.

## Scenario subsection after-state, October 18

PR #20 merged as `63e5614301c6978404cf4b025b35a26229c37afa`.
Render Deploys showed this exact commit as the last successfully deployed
backend and **Live** before the probe; the six-flag connection gate passed.
This deployment added slow-only wall/CPU subsection timing for the existing
Scenario notification read. It did not change query shape, transaction
boundaries, Hikari configuration, or functional behavior.

The existing ignored hosted-proof Tenant Admin identity was used only for
login and two `GET /api/access/admin/workspace` reads. No fixture or tenant
data was changed. Both responses were HTTP 200 and contained 133 connectors;
the earlier PR #19 sample contained 131, so this is **not** a like-for-like
latency comparison. Login completed in 1,192.9 ms. The first workspace read
was the warm-up, not a claim of a steady-state performance bound.

| Stage | UTC start | Request ID | Client ms | Workspace composition ms / CPU ms | Incident ms / CPU ms | Scenario notification ms | Other measured sections |
| --- | --- | --- | ---: | ---: | ---: | ---: | --- |
| Warm-up | 2026-10-18 09:05:17.711Z | `8242480c-9912-493d-9aaf-8bf7c458fb06` | 4,641.7 | 2,899 / 148 | 1,305 / 70 | 296 | connectors 595; replay 408; response/count 593; support audit 194 |
| Follow-up | 2026-10-18 09:05:22.431Z | `1a6be190-0147-4c3c-8c3c-a0939a315f95` | 1,979.6 | below the 2,000-ms composition logging threshold | no matching slow-section log | not measured | no matching HTTP/section warning |

The warm-up's same-request Render section logs came from instance `258gq`;
the follow-up did not emit a section log, so its instance is not established.
The warm-up's incident trace also measured operator 5, audit 101, inbound
103, dispatch 90 and merge 199 ms. The Scenario notification subsection was
below its own 300-ms logging threshold; its 296 ms is the **outer incident
section**, not an exact breakdown of SLA/active/acknowledged work. No matching
slow-HTTP warning was emitted for these requests, so server-total duration,
in-flight Hikari occupancy, PostgreSQL PID/SQL duration and the difference
between client and composition time remain unattributed. A point-in-time
connection gate cannot substitute for a same-request pool sample.

Render's seven-day application-log search showed **33 visible**
`Slow scenario notification read` entries from October 15 through October 17
on the deployed diagnostic. These are threshold-selected logs, not a rate
over all reads and not all tied to the probe tenant. Their total wall time
ranged from 301 to 1,499 ms (median 509); median thread CPU was 9 ms. The
largest recorded subsection was `active` in 13 entries, `slaEscalation` in
10, `acknowledged` in 9 and `merge` in 1. Median subsection wall times were
102, 199, 102 and 0 ms respectively; maxima were 603, 495, 604 and 294 ms.
The largest subsection is **not** consistently the SLA scan. The trace
provides wall and current-thread CPU time around repository/service calls,
not exact SQL execution, connection-acquisition wait or JDBC hold time.
It does not prove that an SLA escalation write occurred in any listed read.

The first local probe attempt performed a login/read but suppressed its own
timing output; it is excluded from this sample. A second attempt logged in
successfully but returned workspace HTTP 403 because the temporary probe
lost its cookie session across a PowerShell function scope. The corrected
single-session probe produced the two HTTP 200 rows above. The 403 is a
probe-harness failure, not evidence of a hosted authorization regression.

**Decision:** Scenario notification reads can contribute hundreds of
milliseconds and sometimes over a second, but this evidence does not make
them the sole workspace delay or explain historical Hikari 10/10 starvation.
The current same-request trace distributes time across connector, incident,
replay, response/count and unmeasured HTTP overhead. Do not disable SLA
governance or widen timeouts. The next justified H1/H2/H7 discriminator is
to establish the round-trip/SQL-versus-wait cost of the serial workspace
read sections, starting with the six count calls inside `response`, while
preserving tenant/warehouse authority. Historical 204 client outcomes,
pool-holder ownership, repeated hosted latency and M1 exit remain open.

## Bounded workspace user-count correction, local candidate

The response/count section of the October 18 matched workspace read took
593 ms. Source inspection found six serial count calls in that section. Two
of them counted active/inactive users even though the same method had already
loaded the complete tenant-scoped user list. This is a proven redundant
query pair, not proof that all 593 ms or the whole HTTP delay came from it.

The focused H2 regression initially failed with 30 prepared statements for
the 12-connector workspace against the proposed 27-statement bound. The
service now derives active and inactive user counts from its already-loaded
tenant user list; no scope predicate, transaction, API field or operator
count query changed. A follow-up focused run passed with a tightened bound
of at most 28 statements, and asserted the support summary for one active
admin plus a scoped active admin and an inactive admin. The exact resulting
statement count was not printed, so only the bound and the source-level
removal of two repository count calls are claimed. The full local backend
run passed 412 tests across 72 Surefire reports with zero failures, errors
or skips; the documentation link check passed 1,041 local links. CI and an
exact-revision hosted after-state must be checked separately. This candidate
does not explain historical Hikari 10/10 starvation or close M1.

## User-count correction, exact Live after-state, October 18

[PR #21](https://github.com/kobanihlulani77-maker/SynapsCore/pull/21)
merged as `c46635fa2db540aeaabd18379739c6a5cdbcd794` after both jobs in
[exact-head CI run 39115324988](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/39115324988)
passed. Render Deploys showed that merge **Live** after a 4m39s auto-deploy;
the post-deploy six-flag connection gate passed. This is a new production
revision, not a measurement of the earlier `63e5614` deployment.

The same ignored proof identity performed one login and three serial, read-only
`GET /api/access/admin/workspace` requests. No fixture or tenant data was
changed. All three returned complete HTTP 200 workspace payloads for
`HOSTED-PROOF-5-20260902-144935`, with 133 connectors, six active users and
zero inactive users. The first workspace request is a warm-up; the other two
are a small bounded follow-up sample, not a repeatability or load proof.

| Stage | UTC start | Request ID | Client duration | Outcome |
| --- | --- | --- | ---: | --- |
| Login | 2026-10-18 09:38:08.151Z | `056e69c9-fda3-4e4b-9e5c-c775bb6b8ed2` | 3,559.4 ms | HTTP 200, signed in |
| Workspace warm-up | 2026-10-18 09:38:11.799Z | `8e638f70-5ba6-4960-8b64-375227290d2b` | 4,581.9 ms | HTTP 200, complete DTO |
| Workspace follow-up 1 | 2026-10-18 09:38:16.509Z | `4d1f4faf-4ebe-48a9-8d50-9fb640e50f00` | 2,423.2 ms | HTTP 200, complete DTO |
| Workspace follow-up 2 | 2026-10-18 09:38:19.013Z | `f322907b-b4c3-4481-819a-3241113e3c20` | 2,226.4 ms | HTTP 200, complete DTO |

The warm-up's matched Render application logs came from instance `f48s8` on
HTTP thread `http-nio-0.0.0.0-10000-exec-9`. Workspace composition took
3,100 ms with 154 ms current-thread CPU. Its sections were tenant 107, users
196, warehouses 4, connectors 808, incidents 1,399, supportAudit 6 and
response/count 394 ms. Incident composition took 1,398 ms with 69 ms CPU:
operator 100, audit 104, replay 304, Scenario notifications 304, inbound 95,
dispatch 100 and merge 104 ms. The matching Scenario read split its 304 ms
among SLA escalation 103, active 99, acknowledged 98 and merge 0 ms. These
section timers include repository/service wall time, not exact SQL duration,
Hikari acquisition or connection hold. The matched logs did not include a
slow-HTTP warning; server-total time and the client/composition difference
remain unattributed. The two follow-ups emitted no matching slow-section log,
so their section timing and instance are not known.

The preceding `63e5614` sample on the **same 133-connector tenant** measured
4,641.7 ms warm-up and 1,979.6 ms follow-up client time. Its warm-up
response/count section was 593 ms versus 394 ms in this single after-sample,
while total workspace composition was 2,899 versus 3,100 ms. The local
two-query reduction is real, but these non-concurrent, differently loaded
samples do **not** establish a hosted latency improvement. Both deployments
returned complete 200 workspaces in the sampled window; neither reproduces
the historical 204 client outcome or identifies the historical Hikari 10/10
holders. M1 and H1/H2/H7/H12 remain open. The next discriminator is measured
SQL/connection versus non-SQL wait inside the remaining serial workspace
sections, with a representative client outcome rather than another speculative
timeout or capacity change.

## Workspace wait sampler, local diagnostic candidate

Source inspection confirms `TenantWorkspaceAdministrationService.getWorkspace()`
has no outer read-only transaction because incident SLA reads may transition
state. Connector description has its own read-only transaction; incident
composition calls separate audit, Replay, Scenario, inbound and dispatch
paths. Therefore the 3,100-ms workspace composition does not itself prove a
3,100-ms JDBC connection hold.

The next bounded diagnostic reuses the existing `SlowThreadWaitSampler` from
dashboard snapshots for workspace composition. Only a workspace exceeding
the existing two-second slow-section threshold logs its one-second request-
thread samples and Hikari active/waiting/idle extrema, alongside the existing
section wall/CPU times. The same candidate lowers the slow-HTTP warning
threshold from five to two seconds for `GET /api/access/admin/workspace`
only. This gives a same-request server HTTP total, identity/handler split,
CPU and completion-time Hikari snapshot; writes and other API routes retain
the five-second threshold. It may increase warning volume for this one read
route, but does not increase request timeout or change its response. The
HTTP threshold regression failed before the change (no two-second warning)
and passed after it, including POST unchanged and query-string redaction.
The sampler categorizes Hikari acquisition,
PostgreSQL JDBC, Redis, Java monitor/wait or other work; it does not change
query shape, transaction boundaries, pool size, scheduler cadence, timeout,
or response contract. The relevant local workspace/Scenario/sampler tests
passed 14/14 before the HTTP threshold addition. The filter regression
passed 15/15, and the complete backend rerun passed 413/413 across 72
Surefire reports with no failures, errors or skips. The local docs-link
check found 1,042 links and none missing. Exact-head CI and exact-revision
hosted diagnostic output are pending.

Sampling is not a SQL profiler. One-second samples can miss short waits and
cannot identify a PostgreSQL PID, exact query duration, acquired connection
owner or all overlapping Hikari holders. A future Live sample may rule in a
dominant wait category in that request; it cannot by itself close historical
Hikari 10/10 starvation, the 204 client-outcome gap, or M1.
