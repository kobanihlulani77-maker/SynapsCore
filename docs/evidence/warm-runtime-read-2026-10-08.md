# Warm runtime read continuation, October 8, 2026

## Revision and observation boundary

Render Deploys showed `d58db984ac427e07ec58da27d064279c28420cbb`
as the last successfully deployed, Live backend revision. The application
code is unchanged from the verified V15 deployment `5c321c1`; intervening
changes add scripts and documentation. The six-flag connection gate passed.
The following are naturally occurring application-log samples from Live
instance `gd99j`, not a controlled load test or a browser-to-JDBC trace.

| UTC log time | Request ID | Observation | HTTP wall / thread CPU | Hikari at HTTP completion |
| --- | --- | --- | --- | --- |
| 13:49:57.031 | `6e701882-6d17-4d19-a7ac-0feba45dab05` | dashboard snapshot | 5,003 / 108 ms | total 10, active 3, idle 7, waiting 0 |
| 13:50:01.535 | `ca74c723-bda5-4ba2-9472-605393f06797` | workspace read | 29,005 / 635 ms | total 10, active 2, idle 8, waiting 0 |
| 13:51:39.734 | `64feb765-64e3-40ec-b58d-400c6f1abfd3ca` | dashboard snapshot | 6,006 / 103 ms | total 10, active 0, idle 9, waiting 0 |

For the first snapshot, the same-request composition log at 13:49:56.830
reported 4,396 ms wall and 97 ms current-thread CPU. Sections took roughly
100-400 ms each, including audits 301, connectors 398, replay 299,
scenario notifications 295, fulfillment 400, recommendations 200, alert feed
290, summary 198, inventory 205, orders 295, events 396 and SLA 400 ms.
For the second snapshot, the same-request composition log reported 5,602 ms
wall and 93 ms CPU. Its sampled wait diagnostic included one `POSTGRES_JDBC`
sample via `AuthSessionService#resolveUser`; one stack sample does not
attribute the full 5.6 seconds. Neither snapshot was logged as a coalesced
waiter.

The low HTTP-thread CPU relative to wall time makes sustained CPU execution
on *that thread* an inadequate explanation for these requests. Serial
section times account for much of the composition wall time, but the logs do
not separate SQL execution, connection acquisition, network, off-thread
processing or host scheduling for each section. Hikari's completion-time
headroom does **not** prove the pool had headroom throughout the request.
Conversely, these samples do not show the historical `10/10 active, 0 idle,
waiters > 0` starvation state. The workspace request lacks a section/JDBC
timeline. There is no PostgreSQL PID, lock, query duration, host CPU/GC or
exact Chrome request timing for these IDs. No application, pool, timeout,
scheduler or infrastructure correction is justified from this capture alone.

## Classification and next boundary

This is **warm active-runtime HTTP latency observed, cause open**; it is not
an Activity-index regression, a proven Hikari-holder event, or a frontend
convergence defect. It reinforces the shared H1/H2/H7/H12 requirement to
measure acquisition, SQL and non-SQL time on one representative slow request
and to correlate instance resources over the same interval. A point sample
of idle dispatch later in the log window is not a negative proof for worker
overlap during the slow requests. The October 7 V15 audit SQL after-sample
and the still-missing like-for-like Activity HTTP timing are tracked in
[the platform activity boundary](platform-activity-read-boundary-2026-10-07.md).
M1 and M2 entry remain open and blocked respectively.
