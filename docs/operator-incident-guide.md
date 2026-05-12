# Operator Incident Guide

This guide helps operators interpret the product’s visible states without needing deep backend knowledge.

The goal is simple:

operators should know what is informational, what is degraded, what is dangerous, and what should trigger escalation.

## State Meanings

### Degraded

Meaning:

- the system is still partly functioning
- but one or more trust or data paths are weakened

Examples:

- runtime warnings
- connector health degraded
- replay backlog rising
- websocket reconnecting

Operator meaning:

- proceed carefully
- verify whether action is still safe

### Waiting

Meaning:

- the platform is explicitly not pretending the result is already complete
- a process may still be starting, replaying, approving, or reconnecting

Operator meaning:

- waiting is safer than false success
- do not assume the final business result yet

### Reconnecting

Meaning:

- realtime connection is degraded or re-establishing

Operator meaning:

- dashboard may not be fully live right now
- verify critical actions against page state and runtime trust

### Replay Pending

Meaning:

- failed inbound work exists and is still awaiting recovery

Operator meaning:

- something operationally important did not enter the live flow yet
- inspect failure reason and connector state

### Manual Review

Meaning:

- a scenario or decision is waiting for a person to act

Operator meaning:

- system is behaving safely by requiring review
- this is not an error by itself

### Approval Blocked

Meaning:

- an action cannot safely continue until the right review or approval occurs

Operator meaning:

- do not try to work around it casually
- route to the correct reviewer or owner

### Runtime Trust Warning

Meaning:

- backend, dependency, or system-trust posture is degraded enough that operators should be cautious

Operator meaning:

- trust the warning
- do not assume business state is fully current without checking

## What Is Informational

Usually informational:

- a waiting state with no accompanying incident
- review-required state
- predictable queued replay state
- controlled reconnecting during brief recovery

These are not necessarily dangerous, but they still need attention if they persist.

## What Is Operationally Dangerous

Operationally dangerous states include:

- backend unavailable
- runtime trust degraded with missing auth or websocket readiness
- replay queue stuck and growing
- connector disabled unexpectedly
- approvals blocked around time-sensitive actions
- stale or misleading live posture

These should not be treated as cosmetic UI issues.

## What Should Trigger Escalation

Escalate when:

- degraded state persists
- replay backlog is growing without clear recovery
- auth/session is failing for operators
- runtime warnings align with user-visible failure
- approvals are blocked and operational work is waiting
- connector state is unhealthy and no owner is responding

## Operator Bottom Line

SynapseCore is designed to show uncomfortable truths rather than hide them.

Operators should read states like degraded, reconnecting, waiting, and replay pending as operational signals, not merely UI decoration.
