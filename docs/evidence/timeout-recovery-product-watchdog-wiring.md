# Timeout Recovery - Product Watchdog Scheduler Wiring

## Bounded Question

Does the product contention watchdog still arm after recommendation scheduling
introduced a second TaskScheduler bean? This is a diagnostics availability
question, not attribution of historical Hikari starvation.

## Direct Evidence

`ProductWriteContentionDiagnostics.begin()` obtains a PostgreSQL PID and then
calls an unqualified `ObjectProvider<TaskScheduler>.getIfUnique()`. Production
`SchedulingConfig` defines both `synapseScheduledTaskScheduler` and
`synapseRecommendationTaskScheduler`. Neither is primary. The provider therefore
returns null and the method silently returns `ProductWriteWatch.NO_OP`.

The direct `ProductWriteWatchdogWiringTest` loads the actual SchedulingConfig and
diagnostic bean in a Spring context. Only JDBC metadata/PID/SET responses are
simulated; bean creation, dependency injection, scheduler selection, scheduling,
and cancellation are real. No PostgreSQL connection or hosted request is made.

Unchanged production: **3 tests, 1 failure, 0 errors/skips**. The two-scheduler
test received the NO_OP watch instead of an armed task. Scheduling-disabled and
non-PostgreSQL cases passed. Log: `backend/target/product-watchdog-wiring-red.log`.
Initial scratch JShell attempts failed classpath resolution and are not proof;
the Maven/Spring test above is the evidence basis.

## Correction and Verification

The preceding product preflight correction `6cb6ddb` passed all 348 tests in
CI run `34230885610`. Only then was watchdog production wiring changed.

Its constructor now qualifies the optional TaskScheduler provider with the
existing `synapseScheduledTaskScheduler` bean name. The recommendation scheduler
is not selected. No scheduler was added, resized, disabled, or rescheduled.
JDBC queries, pool settings, lock timeout, watchdog delay, and failure handling
are unchanged.

Focused verification: **15 tests passed, 0 failures/errors/skips**. This includes
the three direct watchdog wiring tests, six Product preflight connection-demand
tests, and six identity-sequence tests. Both ten-worker Product paths reached
10/10 catalog boundaries and released all connections. Log:
`backend/target/product-watchdog-wiring-focused.log`.

Full backend verification: **351 tests passed, 0 failures/errors/skips**,
BUILD SUCCESS in 4m32s, completed `2026-09-08T13:27:37Z`. Log:
`backend/target/product-watchdog-wiring-full-suite.log`. Backend packaging also
passed; log: `backend/target/product-watchdog-wiring-package.log`.

Committed and pushed as `9726cec28fe4a44d0f458f7f9554ce4842eaa158`.
[CI run 34691377338](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/34691377338)
passed all 351 backend tests in 4m12s, the frontend build, and both Compose
configuration checks on `2026-09-12`. The CI Product overlap cases reached
10/10 for both paths, with peak waiters 0 (create) and 5 (import).

Hosted served-revision verification remains a separate gate. No hosted health
or contention result is claimed from local or CI verification.

## Limits

Selecting the intended scheduler will not make this probe independent of
Hikari: inspection still uses the application JdbcTemplate, so an exhausted
pool can prevent it from acquiring a connection. Main scheduler backlog can
also delay it. This probe is a delayed sample, not continuous tracing or a
complete SQL/Java connection-hold timeline. Restoring it must not be described
as fixing pool starvation or proving hosted runtime health.
