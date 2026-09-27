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
