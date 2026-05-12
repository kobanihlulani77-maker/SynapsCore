# Buyer Due-Diligence Guide

This guide is for companies evaluating SynapseCore seriously for a pilot, operational trial, or technical review.

It is written for:

- CTOs
- operations leaders
- technical reviewers
- infrastructure leads
- pilot sponsors

The goal is to explain the platform honestly, not to oversell it.

## What SynapseCore Is

SynapseCore is a tenant-based operations command platform.

It combines:

- live operational visibility
- inventory and order coordination
- integration visibility
- replay and recovery
- scenario approval and execution
- runtime trust

into one operational surface.

It is meant for businesses that already have systems, but still lack one trustworthy coordination layer across those systems.

## What SynapseCore Is Not

SynapseCore is not:

- a generic dashboard
- only BI or reporting
- only inventory software
- only order software
- only integration middleware
- only observability tooling
- a full ERP replacement

Those systems each address part of the problem. SynapseCore is aimed at the operational seams between them.

## Current Supported Operational Scope

The current supported platform scope is meaningful but intentionally bounded.

Current real scope includes:

- public entry experience and sign-in
- tenant workspace auth and session handling
- catalog and product onboarding
- warehouse-aware inventory views and actions
- order visibility
- integrations through webhook, CSV import, and scheduled pull patterns
- replay queue visibility and manual recovery flows
- scenario planning, approval, and execution
- runtime and incident trust surfaces
- hosted proof tooling for deployed frontend/backend validation

That is enough for serious pilot evaluation, but it is not the same as claiming complete enterprise breadth.

## Architecture Overview

SynapseCore currently uses:

- a React frontend for the public site and authenticated command center
- a Spring Boot backend for auth, domain logic, runtime, replay, and scenario control
- PostgreSQL as the operational record of truth
- Redis for session and distributed realtime posture
- SockJS/STOMP for tenant-scoped live updates

The platform is designed so the user interface is not just a report viewer. It is a control surface connected to the operational model underneath.

## Tenant Workspace Model

SynapseCore is organized around company workspaces.

That means:

- each company has a distinct operational workspace
- workspace access is bound to company context
- operators sign in with workspace code, username, and password
- operational data is scoped to that workspace

For buyers, this matters because the product is built around tenant isolation from the start rather than as an afterthought.

## Replay And Recovery Philosophy

One of the clearest product differentiators is that failed inbound work is treated as an operational object.

Instead of disappearing into logs or support queues, failed inbound work can be:

- surfaced
- explained
- repaired
- replayed intentionally
- traced after recovery

This is important for companies where integration failures create real operational cost.

## Proof Philosophy

SynapseCore uses hosted proof because the project treats real deployment truth as part of product quality.

Hosted proof exists to validate the real deployed frontend and backend together.

Proof is intentionally paused when:

- backend readiness is unhealthy
- auth/session is not responding
- websocket trust is missing

That is a meaningful due-diligence signal. The platform does not claim success when the runtime truth is missing.

## Current Deployment Posture

The current deployment posture supports real validation, but should be understood honestly.

Current reality:

- the frontend is productized and deployable
- the backend supports real business flows
- the platform can be validated locally and through hosted proof
- infrastructure dependency health still matters heavily
- larger enterprise deployment hardening is still a roadmap item

This is a serious pilot posture, not a blanket promise of large-scale enterprise maturity in every category.

## Operational Resilience Posture

The platform deliberately exposes degraded-state truth.

That includes:

- readiness vs liveness distinction
- runtime trust surfaces
- visible replay and connector degradation
- proof gating when infrastructure trust is missing

This posture is important for buyers who care more about truthful operations than polished concealment.

## Current Limitations

The strongest due-diligence posture is honest limitation disclosure.

Current limitations include:

- hosted proof still depends on backend, DB, and websocket readiness
- broader enterprise hardening is still needed for large-scale claims
- connector breadth is still intentionally limited
- advanced identity, HA, and deeper observability remain roadmap items
- infrastructure quality has a visible effect on runtime trust

These limitations do not invalidate pilot value. They define the safe adoption envelope.

## Realistic Adoption Expectations

A good buyer should evaluate SynapseCore as:

- a serious pilot candidate
- a platform for live operational control in bounded scope
- a strong fit where replay, visibility, approvals, and trust matter
- a system that should be adopted gradually and deliberately

They should not evaluate it as:

- a drop-in replacement for every existing platform
- an instant ERP replacement
- a finished global enterprise operating stack

## Questions A Serious Buyer Should Ask

Useful due-diligence questions include:

- Which operational lane are we piloting first?
- What failed inbound scenarios do we want to make visible?
- What runtime truth do we need operators to understand?
- Which approvals need governed execution?
- Which connector lanes matter most today?
- What infrastructure posture is required for trustworthy proof?
- What hardening do we require before broader rollout?

## Why This Platform Can Matter

SynapseCore matters when a company is not missing tools, but missing coordination.

That is the key lens for due diligence.

If the company already feels the cost of:

- fragmented systems
- manual reconciliation
- unclear failure ownership
- replay gaps
- slow approval flow
- runtime uncertainty

then the platform is worth serious evaluation.

## Bottom Line

SynapseCore should be evaluated as a live operational coordination platform with real replay, approval, runtime trust, and command-center scope.

It is credible because it is honest about both:

- what is already real
- what still needs hardening
