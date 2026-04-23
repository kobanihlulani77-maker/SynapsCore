# Live Deployment Runbook

This runbook is the practical operating path for deploying SynapseCore onto a real host.

## Recommended Baseline

- Ubuntu `24.04 LTS`
- `2 vCPU / 4 GB RAM` minimum for single-node rollout
- public DNS for separate app and API origins

## Host Preparation

Install:

- Docker Engine
- Docker Compose plugin
- Git

Clone the repo to a stable path such as `/opt/synapsecore/synapsecore`.

## Environment Preparation

Generate env targets:

```bash
bash scripts/prepare-prod-envs.sh
```

Key backend truths:

- `SPRING_PROFILES_ACTIVE=prod`
- `ALLOW_HEADER_FALLBACK=false`
- `SESSION_COOKIE_SECURE=true`
- `SYNAPSECORE_REALTIME_BROKER_MODE=SIMPLE_IN_MEMORY` unless external broker relay is actually provisioned
- `SPRING_JPA_HIBERNATE_DDL_AUTO=update` is still the current posture, but it is not the final target

Key frontend truths:

- `VITE_API_URL` must point at the real API origin
- `VITE_WS_URL` must point at the real `/ws` path

## Tenant Provisioning Truth

Production tenant creation is intentionally strict:

- first tenant on an empty production database: bootstrap token lane
- later tenant creation: platform-admin token lane
- signed-in tenant admins do not create new tenant workspaces in production

## Hosted Verification Credentials

Hosted browser proof must use real tenant accounts created or reset through production APIs.

Required env values:

```powershell
$env:PLAYWRIGHT_BASE_URL="<frontend-url>"
$env:PLAYWRIGHT_API_BASE_URL="<backend-url>"
$env:PLAYWRIGHT_TENANT_CODE="<proof-tenant>"
$env:PLAYWRIGHT_PROOF_PRODUCT_SKU="<tenant-specific-proof-sku>"
$env:PLAYWRIGHT_TENANT_ADMIN_USERNAME="<tenant-admin-user>"
$env:PLAYWRIGHT_TENANT_ADMIN_PASSWORD="<tenant-admin-password>"
$env:PLAYWRIGHT_PLANNER_USERNAME="<planner-user>"
$env:PLAYWRIGHT_PLANNER_PASSWORD="<planner-password>"
$env:PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME="<integration-admin-user>"
$env:PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD="<integration-admin-password>"
```

Preparation command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
```

Current truth:

- the proof pack is real
- user provisioning is real
- the hosted catalog onboarding path is still the main blocker

## Realtime Truth

Current single-node truth:

- `SIMPLE_IN_MEMORY` broker mode is acceptable for one backend instance
- it is not the final scale-out topology

Later production hardening:

- deploy external broker relay infrastructure
- switch to `EXTERNAL_BROKER`
- re-run browser and runtime verification after the change

## Schema Migration Truth

Current rollout posture still depends on Hibernate auto-update.

That is a temporary SaaS-foundation compromise, not final deployment discipline.

Before company-grade rollout, SynapseCore should move to explicit versioned migrations. See:

- [schema-migration-roadmap.md](schema-migration-roadmap.md)

## Verification Order

1. verify backend health and readiness
2. verify frontend loads and deep links work
3. verify sign-in and protected routes
4. verify dashboard, catalog, integrations, runtime, and audit pages
5. run hosted proof preparation
6. run browser proof
7. verify replay and runtime trust surfaces

## Bottom Line

Use this runbook as a real operational deployment guide, not a demo walkthrough.

If a capability is still partial, keep it labeled as partial in rollout discussions:

- hosted product onboarding proof
- explicit schema migration discipline
- external-broker realtime rollout
