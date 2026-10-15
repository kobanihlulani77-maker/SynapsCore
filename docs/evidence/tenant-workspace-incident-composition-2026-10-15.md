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
