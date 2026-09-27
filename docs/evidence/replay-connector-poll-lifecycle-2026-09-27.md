# Replay Connector Poll Lifecycle - 2026-09-27

## Boundary and evidence

The September 23 hosted Playwright failure reported connector `132` as enabled
(`version=1`, `updatedAt=2026-09-23T17:26:37.630816Z`) through its separate
authenticated API read. At `17:26:49.171Z`, the Replay page still showed the
selected connector as disabled and kept the manual action unavailable. The
socket was live. The diagnostic artifact did not capture the exact connector
request/response timeline or a successful browser snapshot read, so it does not
establish which network call, if any, delivered the newer state to that page.

Source inspection found a deterministic page-side polling failure. `useApi()`
creates `fetchJson` on each workspace render. The Replay connector effect depended
on that function and the snapshot connector array. A disabled exact-connector
read scheduled a two-second retry. A parent workspace render recreates
`fetchJson`, cleans up the effect, and clears that timer. Snapshot/realtime
refreshes can also change the connector array dependency and invalidate an
in-flight read; its completion is then ignored. Consequently a still-disabled
snapshot could keep the action blocked even after the backend enabled the
connector. This is a proven source-code lifecycle defect, not a proven exclusive
explanation of the historical failure or its concurrent pool pressure.

## Correction and checks

The exact read now uses the latest request helper through a ref, while the
effect follows stable selected-record and authenticated-session identity.
Snapshot refreshes and local connector state updates no longer cancel the
poll timer. The override is keyed to the tenant/session and connector identity;
when both exact read and snapshot have the connector, a newer snapshot version
takes precedence over an older exact response. Backend eligibility remains
authoritative when the operator submits replay.

On this change, `npm.cmd run verify`, `npm.cmd run test:convergence`, and
`npm.cmd run test:replay-convergence` passed. The last check renders the actual
Replay component in Chromium, changes the parent request helper every 50 ms,
delays exact connector responses, and verifies both eventual enablement and
newer disabled snapshot precedence. Browser behavior on a deployed version remains **OPEN**;
do not promote this local correction to hosted verification. The September 23
Hikari saturation and warm HTTP latency remain separate H1/H2/H7 work.

Next focused proof: on the exact served frontend revision and an authorized
synthetic tenant, record the connector update response, the page's exact
connector GET, snapshot response, and Replay button state with UTC times. A
passing focused proof closes this UI seam for that revision; a slow/failed GET
continues the backend latency investigation instead of changing UI timeouts.

## Focused hosted observation, 2026-09-27

The hosted frontend served the same production bundle bytes as the locally
built source containing the polling correction. This proves the served frontend
code, not a Git SHA: its embedded build commit is the static value
`render-deploy`. CI succeeded for `0485b63`; that alone does not prove the
backend revision. One warm focused Replay flow failed after the connector
update. Its separate authenticated backend read returned connector `134`,
`version=1`, `enabled=true`, updated at `13:51:53.475Z`.

The browser request trace gives the missing boundary:

| UTC | Browser observation |
| --- | --- |
| `13:51:46.639` | Dashboard snapshot GET began before connector enable. |
| `13:51:47.152` | Broad connector GET began; browser aborted it after 5.076 s. |
| `13:51:59.940` | First snapshot returned HTTP 200 after 13.301 s TTFB, with connector `version=0`, disabled. Request ID `b1431331-bc11-4ea2-9ba7-bdb3fcd0fcd4`. |
| `13:52:00.040` | New snapshot and broad connector GET began. |
| `13:52:04.997` | Broad connector GET was again aborted after 4.957 s. |
| `13:52:25.852` | Second snapshot returned HTTP 200 after 25.812 s TTFB, with connector `version=1`, enabled. Request ID `3ab14246-2602-4b25-a9f4-5100b3621473`. |

The Replay panel was initially blocked but became eligible while the diagnostic
read ran. The diagnostic had sampled its `exactReplayAction` before its own
snapshot fetch, then compared that stale sample to the later enabled page.
The proof helper now samples the final action after diagnostic reads and records
bounded browser request timing and connector version. Its old runtime-config
lookup also skipped browser-side auth/snapshot diagnostics for this build;
the helper now uses the configured hosted backend URL with bounded reads.

**Classification:** `CHROME_HTTP_SLOW` is proven for these snapshot requests.
The broad connector GETs reached their configured 5-second browser cutoff and
were aborted. The browser ultimately received the enabled state and rendered an
enabled action, so permanent frontend staleness is **not** established by this
run. The trace contained no exact filtered connector GET; why that page-side
read was absent remains open. No Hikari or PostgreSQL holder evidence was
captured for these request IDs, so do not infer a database lock, pool
starvation, or a backend transaction owner from this observation. The local
Chromium check was extended to enter Replay from an inactive route before
exercising the poll. Its button assertion passed, but the check had not yet
asserted a clean browser console. A later strict console assertion exposed a
React warning on that transition, as recorded below.

## Inactive-to-active hook-order correction

`ReplayPage` returned before calling `useState`, `useRef`, and `useEffect`
when the page was inactive. The same mounted component called those hooks
when Replay became active. A local Chromium check that first renders the
inactive route, then activates it, reproduced React's "Expected static flag
was missing" warning. This violates stable hook ordering even though its
original button assertion passed. The component now calls its hooks on every
render and gates the connector effect and UI output by authentication and
route activity. The strengthened check verifies no connector fetch before
activation, the subsequent polling and button transition, and zero page or
console errors. It passes locally, as does `npm run verify`.

This is a confirmed frontend defect and a narrow correction, not proof that
the missing filtered GET in the hosted trace had this exact cause. A deployed
browser proof must still establish that GET and bounded convergence. The
13/26-second snapshot responses remain a separate backend-latency finding.

**Next boundary:** correlate the two request IDs and browser UTC window with
backend entry/exit, snapshot composition, Hikari acquisition, and database
activity. Establish whether the delay is routing, pool acquisition, SQL,
non-SQL Java work, or contention. Do not increase the 5-second connector
cutoff or the hosted proof timeout as a substitute for explaining that latency.
H8 hosted convergence remains open; M1/M2 gates are unchanged.

The existing Render application logs had no matching line for either snapshot
request ID. A search for `HikariPool-1` showed startup/shutdown entries but no
pool-timeout entry for this run. Scheduler samples around `13:51:22Z` showed
two active of ten connections and zero waiters, but they do not cover every
millisecond of the later slow response. The absence of a per-request timing
line is an observability gap, not proof that the backend handler was fast.

To close that measurement gap, the request trace filter now emits a bounded
warning for API requests taking at least five seconds. It records the existing
request ID via MDC, matched route pattern rather than query/body, status,
whole-filter duration, session/identity-resolution duration, handler duration,
and an in-process Hikari snapshot. It does not borrow a connection, change the
HTTP outcome, or log credentials. It will distinguish early auth/session
delay from downstream handler time, but SQL versus Java time still requires
more targeted evidence after that split. Backend `4f91d89` later deployed and
emitted this warning during a warm focused proof. See the separate
[pool-pressure evidence](warm-runtime-pool-pressure-2026-09-27.md); that
capture does not establish the exact SQL or transaction owner.
