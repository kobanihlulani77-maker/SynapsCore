# Operations Reliability

This guide explains how SynapseCore thinks about operational reliability, degraded states, backend availability, replay safety, and trust under real infrastructure conditions.

It is written for technical reviewers and operators who want to know whether the platform understands operational reality, not just feature behavior.

## Reliability Philosophy

SynapseCore reliability is based on a simple principle:

the system should not silently hide degraded operational truth.

That means:

- health must be explicit
- readiness must matter
- degraded state must be visible
- replay and connector failures must remain actionable
- frontend UX must reflect infrastructure truth as honestly as possible

## Readiness And Liveness

The platform distinguishes between:

- liveness
- readiness

### Liveness

Liveness answers:

- is the backend process alive enough to keep running?

### Readiness

Readiness answers:

- is the backend safe and ready for real traffic?

In the current system, readiness includes:

- application readiness state
- DB
- Redis
- ping

This distinction matters because a running process is not the same as a trustworthy operational platform.

## Degraded State Handling

Degraded state is part of the product, not just the infrastructure.

Examples of degraded state:

- backend reachable but readiness not passing
- realtime reconnecting
- DB unavailable
- Redis unavailable
- connector disabled or degraded
- replay backlog growing

The platform should:

- explain degradation
- avoid pretending data is fully live
- keep recovery paths visible

## Websocket Reliability

The realtime model uses SockJS/STOMP and tenant-scoped topics.

Reliability concerns include:

- websocket availability
- fallback transport support
- backend pub/sub posture
- frontend connection-state honesty

Websocket reliability matters because the command-center promise depends on live updates being meaningful, not merely connected once.

## Replay Recovery Reliability

Replay is a reliability feature as much as an integration feature.

It helps the platform remain operationally trustworthy when inbound work fails.

Reliable replay means:

- failures are visible
- replay records are not lost
- replay eligibility is predictable
- manual recovery is safe and traceable

This is why deterministic replay hardening was central to the proof evolution.

## Connector Visibility

Connectors are part of operational reliability because many incidents begin there.

Connector reliability posture should be visible through:

- enabled/disabled state
- health summary
- import history
- replay backlog
- failure reason

This turns “the system broke somewhere” into something operationally actionable.

## Runtime Trust

Runtime trust supports reliability by helping the team answer:

- is the system healthy?
- is it ready?
- is it degraded?
- is this a system issue or a business-state issue?

Runtime, incidents, health, and websocket checks all contribute to that answer.

## Approval Safety

Approvals matter to reliability because many risky actions should not be casually executed.

Reliable approval handling means:

- visible pending states
- role-aware action boundaries
- execution only from the right states
- escalation visibility when needed

That improves operational safety under pressure.

## Failure Visibility

SynapseCore intentionally keeps certain failures visible:

- replay queue rows
- disabled connectors
- runtime degradation
- stale or reconnecting states

This is an operational reliability choice. Hidden failure often creates larger downstream operational damage.

## Incident Posture

The incident and runtime surfaces should help teams distinguish:

- healthy
- degraded
- unavailable
- noisy but safe
- action-required

That is more useful than binary green/red uptime thinking in real operations.

## Local Vs Hosted Operational Differences

### Local

Local is useful for:

- frontend verification
- backend verification
- local full-stack experimentation
- replay/runtime path checks

But local is not the same as proof of deployed behavior.

### Hosted

Hosted adds real deployment conditions:

- separate frontend/backend origins
- session and CORS posture
- Redis-backed prod session expectations
- DB and runtime dependency reality
- cold starts and deployment drift

Hosted proof matters because these conditions can change the real behavior.

## Render Free-Tier Caveats

Render free-tier or low-tier deployment posture can introduce:

- slow cold starts
- readiness delays
- timeouts before full platform availability

These are not imaginary concerns. They affect whether proof should run and how the team interprets failures.

## DB-Off Behavior

When DB is off or unavailable:

- readiness should not pass
- backend may fail to answer in time
- auth session may fail
- websocket info may fail
- hosted proof should stay paused

This is broader than “just a database issue.” It becomes a whole backend trust issue.

## Backend Unavailable Behavior

When the backend itself is unavailable:

- the frontend shell may still be live
- login and page data will fail
- proof should not run

This is why live connection classification exists and why the system distinguishes frontend deployment success from actual operational readiness.

## Frontend-Safe Degradation

A reliable operational frontend should:

- degrade safely
- explain backend unavailability calmly
- keep the user informed without faking success
- preserve command-center trust even when the backend is down

This is one of the reasons the frontend productization work mattered.

## Bottom Line

Operations reliability in SynapseCore is about more than uptime. It is about whether the platform remains truthful, recoverable, and governable under real operational conditions.
