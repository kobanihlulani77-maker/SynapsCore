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
