# Database And Migrations

This document explains how SynapseCore uses PostgreSQL, how schema evolution is handled, and how database operations fit into local development, proof discipline, and deployment safety.

It complements:

- [schema-migration-roadmap.md](schema-migration-roadmap.md)
- [local-runbook.md](local-runbook.md)
- [render-ops-runbook.md](render-ops-runbook.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)

## Database Role In SynapseCore

PostgreSQL is the operational record of truth for SynapseCore.

It stores the state behind:

- tenant workspaces
- users and operators
- products and warehouses
- inventory posture
- orders
- integrations and replay records
- scenarios and approvals
- audit and business events
- incidents and runtime-supporting operational records

When PostgreSQL is unavailable, the platform is not merely missing analytics. It is missing core operational truth.

## Database Profiles And Environment Expectations

### Local `dev` Style

Typical posture:

- PostgreSQL reachable locally or through Docker
- Redis also available
- backend startup may use update-style local convenience settings

### `prod` Style

Typical posture:

- PostgreSQL explicitly configured through datasource env
- Flyway enabled
- Hibernate `ddl-auto=validate`
- startup should fail rather than mutate an unknown production schema

## Current Startup Expectations

Production-style startup expects:

- datasource values are valid
- PostgreSQL is reachable
- Flyway can run
- Hibernate validation can confirm the managed schema

If any of those fail, readiness may fail or the backend may not start correctly.

## Migration Philosophy

SynapseCore does not treat schema mutation as an invisible background detail.

Migration philosophy is:

- schema changes should be explicit
- production should validate schema rather than mutate it casually
- migration order should be reviewable
- release safety should include backup and restore thinking

That philosophy exists because schema drift can quietly break replay, auth, runtime trust, and proof.

## Flyway Posture

Current production-style posture includes:

- Flyway enabled
- explicit migration discipline
- validation-driven startup

Important supporting artifacts include:

- Spring application config in `backend/src/main/resources`
- migration-support material such as `backend/src/main/resources/db/support/full-schema-baseline.sql`
- scripts like `validate-flyway.ps1` and `export-flyway-baseline.ps1`

## Related Files

Relevant code and config areas:

- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`
- `backend/src/main/resources/application-prod.yml`
- `backend/src/main/resources/db/`
- `backend/pom.xml`
- `render.yaml`

## Safe Migration Workflow

Safe workflow:

1. make the schema change intentionally
2. add or update the explicit migration path
3. validate Flyway and backend startup expectations
4. run backend tests
5. deploy only with backup and rollback clarity

Do not treat production schema change as a casual side effect of application startup.

## Local Database Realities

Local development can encounter several real-world database issues:

- Docker Postgres vs Windows Postgres port conflicts
- host `localhost` vs `127.0.0.1` ambiguity
- wrong password assumptions
- backend container already bound to `8080`
- recreated Docker volumes still initializing

These are operational setup issues, not product-code failures.

## Local Database Reset Caution

If a Docker Postgres volume is deleted and recreated:

- all local database content in that volume is lost
- the service may take time to initialize
- login attempts may fail temporarily while startup is still in progress

That action should never be treated as casual cleanup.

## Backup And Restore

Important existing scripts:

- `scripts/backup-postgres.ps1`
- `scripts/restore-postgres.ps1`
- `scripts/verify-restore-drill.ps1`

Use them when:

- preparing releases
- validating recovery posture
- protecting production-like databases

Backup and restore are part of deployment trust, not only disaster planning.

## Common Database Failure Modes

### Password Authentication Failure

Likely causes:

- wrong host target
- Docker Postgres vs Windows Postgres conflict
- wrong password assumption

### Readiness Failure After Backend Start

Likely causes:

- DB unavailable
- Flyway blocked
- schema mismatch
- backend hung waiting for DB

### Local Port Conflicts

Likely causes:

- Windows PostgreSQL service already bound to `5432`
- Docker Postgres also trying to publish `5432`

## What This Means For Proof

Hosted proof should not run when:

- the backend is timing out
- readiness is failing
- auth session does not respond
- websocket info does not respond

Because DB health is part of that truth chain, database issues are proof blockers, not just backend implementation details.

## Related Scripts

- `scripts/validate-flyway.ps1`
- `scripts/backup-postgres.ps1`
- `scripts/restore-postgres.ps1`
- `scripts/verify-restore-drill.ps1`
- `scripts/check-local-connections.ps1`
- `scripts/check-live-connections.ps1`

## Bottom Line

PostgreSQL is one of the hardest trust dependencies in SynapseCore.

If the database is unhealthy, the platform should not pretend to be operationally trustworthy.

That is why database and migration discipline are part of product credibility.
