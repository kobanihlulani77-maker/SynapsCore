# Operational dispatch claim recovery - 2026-10-03

## Proven gap

The dispatch worker persisted each notification item as `PROCESSING` before
refreshing the Dashboard and broadcasting tenant-scoped realtime state. Its
scheduled scan selected only `PENDING`. A process stop after the claim but
before `COMPLETED` or `FAILED` therefore left the item outside every later
drain. The entity had no `@Version` field, so catching optimistic-lock
exceptions around ordinary `save` did not establish a cross-instance claim
or terminal-transition guard. This is a source-code lifecycle gap, not a
claim that a particular hosted item was observed stranded.

## Bounded correction

The scan now includes `PENDING` plus `PROCESSING` items whose `updatedAt` is
older than a five-minute configurable lease (`processing-lease-ms`, with a
one-minute floor). Claim, completion, and failure are conditional database
updates. The claim increments the existing `attemptCount`; terminal updates
require that exact attempt. A late old worker cannot mark a reclaimed item
complete or failed. A competing worker receives zero updated rows and does
not broadcast. No schema migration, Hikari change, scheduler-frequency change,
or operational order/inventory mutation is involved.

Recovery republishes a tenant-scoped notification from authoritative REST
state. It is **at least once**, not exactly once: an unusually slow worker
that survives past the lease can overlap a reclaim and emit a duplicate
signal. The attempt guard protects the durable queue state, not duplicate
publication. `FAILED` remains excluded from automatic recovery and visible
through the existing incident path. A database outage while recording the
terminal state leaves `PROCESSING` eligible for a later lease-based retry.

## Verification boundary

The final focused local run passed seven tests, zero failures/errors: the
service tests cover stale versus fresh work, `FAILED` exclusion, fanout, and
trace restoration; JPA integration tests cover reclaim, old-attempt rejection,
and simultaneous single-winner claims. The complete backend suite on the same
production code passed 70 suites and 401 tests with zero failures, errors,
or skips. The last subsequent edit only strengthened the stale-versus-fresh
test; its focused rerun passed. Production packaging also exited zero.

This is H2 in-memory database test evidence, **not** a real PostgreSQL race or
hosted restart proof. Docker Desktop's daemon was unavailable locally on
October 3, so no isolated PostgreSQL container was used. [Exact-SHA CI for
`a172a68`](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/37125342253)
completed successfully. Render subsequently showed
`a172a68ecbc460729b9beafbaeba8111426e7072` as the last successfully
deployed commit and marked [deploy `dep-db0ftvojo6nc739jer90`](https://dashboard.render.com/web/srv-d7d2s41j2pic73fakrag/deploys/dep-db0ftvojo6nc739jer90) Live. Recent
post-deploy `operational-dispatch` scans completed in 3-98 ms with
`processed=0` and `hikariWaiting=0` in the observed log slice. That is an
idle scheduler control, not a recovery exercise or a sustained headroom
measurement.

Crash timing, PostgreSQL concurrent claims, backlog recovery time,
pool headroom under actual dispatch work, and duplicate-signal effects remain
to verify. H4 and M1 stay open. The next production-shaped proof should run
on isolated PostgreSQL data or a safe disposable tenant, compare queue
status/attempts before and after a controlled worker interruption, and
preserve both authoritative state and realtime readback without mutating
customer operations.
