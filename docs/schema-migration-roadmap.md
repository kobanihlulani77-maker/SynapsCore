# Schema Migration Workflow

This document is the strict migration-discipline workflow for SynapseCore.

## Current State

Current production posture:

- profile: `prod`
- database: PostgreSQL
- current startup validation mechanism: Hibernate `ddl-auto=validate`
- Flyway is active with versioned Java migrations and baseline coverage for the current managed schema
- production-hardening tests run with migration-backed schema validation

## Production Standard

SynapseCore now uses:

- explicit versioned migrations
- application startup that validates schema instead of mutating it implicitly
- repeatable rollout behavior across local, staging, and production-style validation

## Why This Matters

Implicit schema mutation hides risk:

- not all schema changes are safe on populated tables
- change ordering is implicit instead of reviewable
- rollback behavior is unclear
- hosted failures can appear only after deployment

The earlier inventory-column and catalog-write failures are the clearest examples of why SynapseCore now keeps schema evolution explicit and reviewable.

## Version-Controlled Migration Workflow

### Baseline

- the current managed schema is represented by:
  - versioned corrective migrations `V1` through `V4`
  - full baseline coverage in `V5__full_schema_baseline`
- fresh environments bootstrap through Flyway, not Hibernate DDL mutation

### Runtime

- production starts with `ddl-auto=validate`
- schema mismatch fails startup
- Flyway runs before JPA validation on startup

### Change Process

1. add the next versioned migration for every schema change
2. run `scripts/validate-flyway.ps1`
3. run backend tests on validation posture
4. deploy only when Flyway validation and tests are green

### Release Safety

- backup-before-migration remains mandatory for production rollout
- restore drill expectations remain part of the live operations runbook
- staging-to-production ordering must preserve migration version order

## Exact Repo Areas Involved

- `backend/pom.xml`
- `backend/src/main/resources/application-prod.yml`
- `backend/src/main/resources/application.yml`
- `render.yaml`
- future `backend/src/main/resources/db/migration/`
- release and deployment scripts under `scripts/`

## Exact Working Practice

- use `scripts/export-flyway-baseline.ps1` only when intentionally regenerating the managed baseline from the validated backend model
- use `scripts/validate-flyway.ps1` before deployment changes
- do not reintroduce `ddl-auto=update` in production profiles or production-hardening tests
