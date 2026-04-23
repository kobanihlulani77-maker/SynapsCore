# Schema Migration Roadmap

This document is the strict migration-discipline plan for SynapseCore.

## Current State

Current production posture:

- profile: `prod`
- database: PostgreSQL
- current schema evolution mechanism: Hibernate `ddl-auto=update`

That posture is acceptable for early SaaS iteration, but it is not the target production standard for company-grade change control.

## Target State

SynapseCore should move to:

- explicit versioned migrations
- application startup validating schema instead of mutating it implicitly
- repeatable rollout behavior across local, staging, and production

The recommended target is **Flyway**.

## Why This Still Matters

`ddl-auto=update` hides risk:

- not all schema changes are safe on populated tables
- change ordering is implicit instead of reviewable
- rollback behavior is unclear
- hosted failures can appear only after deployment

The recent inventory-column migration issue is the clearest example of why SynapseCore now needs explicit migration discipline.

## Recommended Migration Path

### Phase 1: Introduce Flyway without changing prod behavior yet

- add Flyway dependency
- create `db/migration/` baseline structure
- capture the current schema as a known baseline for fresh environments
- keep current production env on `ddl-auto=update` only until the baseline is validated

### Phase 2: Move non-destructive changes into versioned migrations

- add all new column/index/constraint changes as versioned SQL migrations
- perform backfill-first migrations for populated tables
- remove dependence on Hibernate for structural mutation

### Phase 3: Switch runtime posture

- change production to `ddl-auto=validate`
- fail startup if the schema is not at the expected migration version
- make Flyway execution part of deploy readiness

### Phase 4: Harden rollout operations

- add migration verification to release scripts
- document backup-before-migration and restore drill expectations
- verify staging-to-production ordering for every schema change

## Exact Repo Areas Involved

- `backend/pom.xml`
- `backend/src/main/resources/application-prod.yml`
- `backend/src/main/resources/application.yml`
- `render.yaml`
- future `backend/src/main/resources/db/migration/`
- release and deployment scripts under `scripts/`

## What Is Not Done Yet

This roadmap is documented, but the repo has **not** completed the Flyway cutover yet.

Until that work lands:

- production schema evolution is still partial
- hosted rollout safety is still partial
- SynapseCore should not claim final production migration discipline
