# Data Governance And Retention

This document explains how SynapseCore should think about data ownership, tenant boundaries, audit evidence, retention, and privacy posture.

It is not a legal compliance certification. It is an engineering and operating reference for responsible pilot and platform work.

## Governance Scope

SynapseCore stores operational data for tenant workspaces.

Important data families include:

- tenant workspace records
- operators and users
- auth/session metadata
- products and warehouses
- inventory records
- orders and fulfillment tasks
- integration connectors
- inbound integration records
- replay queue records
- alerts and recommendations
- scenario plans, approvals, rejections, escalations, and execution records
- audit logs
- business events
- runtime-supporting operational records

## Tenant Boundary

The tenant workspace is the central data boundary.

Expected behavior:

- operational records are tenant-scoped
- API access should resolve or require tenant context
- proof must validate tenant-scoped behavior
- operators should not see another tenant's operational state
- support/admin surfaces must be treated as sensitive

Tenant isolation is a product trust requirement, not only a database modeling detail.

## Operational Data Categories

| Category | Examples | Sensitivity | Notes |
|---|---|---|---|
| Workspace identity | tenant name, code, settings | Medium | Controls workspace boundary |
| User/operator data | usernames, roles, actor names | Medium to High | Identity and access governance |
| Catalog data | SKUs, names, categories | Medium | Business-sensitive operational data |
| Inventory data | stock levels, warehouses, thresholds | High | Can affect operational decisions |
| Orders | external order IDs, line items, warehouse routing | High | Core operational truth |
| Integration data | source systems, connector config, failures | High | Reveals operational dependencies |
| Replay data | failed inbound payloads and reasons | High | Must be visible but controlled |
| Scenario data | planned actions, approvals, execution | High | May drive live operations |
| Audit/business events | operator actions, system events | High | Evidence trail |
| Runtime diagnostics | incidents, queue depth, health posture | Medium to High | Operational trust evidence |

## Replay Data Governance

Replay records are intentionally durable enough for recovery and review.

They may include:

- external order ID
- warehouse code
- product SKU
- failed request payload
- failure code
- failure message
- replay attempts
- replay status

Governance expectation:

- replay records must remain operator-visible until resolved or dead-lettered
- replay records should not hide failures for the sake of green dashboards
- manual review is safer than silent discard
- retention should eventually be configurable by tenant and compliance needs

## Audit And Business Event Retention

Audit and business events provide operational accountability.

They answer:

- who acted
- what changed
- when it happened
- which tenant was affected
- which source or request caused the event
- whether the action succeeded or failed

Current posture:

- audit and event records are part of the operational data model
- retention policy is not yet a mature enterprise archive program
- longer-term retention, archival, and deletion workflow should be hardened before large enterprise commitments

## Session And Auth Data

SynapseCore uses session-backed auth behavior.

Governance concerns:

- session identity must stay tenant-aware
- production sessions should be secure-cookie aligned
- Redis/session state is an operational dependency
- password and account lifecycle events should remain auditable

Current limitations:

- advanced SSO/SAML/OIDC maturity is roadmap territory
- deeper enterprise identity lifecycle integration still needs hardening

## Data Minimization

Recommended posture:

- store operational data that supports command-center decisions
- avoid storing secrets in docs or source
- keep connector tokens hashed or otherwise protected where applicable
- do not log sensitive payloads casually
- avoid turning proof data into hidden production data

## Deletion And Export Posture

Current honest posture:

- SynapseCore has tenant-scoped operational models, but a full enterprise tenant export/deletion lifecycle should be treated as future hardening unless explicitly implemented and verified.
- Pilot programs should define what data may be cleared, retained, exported, or restored before rollout.

Future hardening should include:

- tenant export process
- tenant deletion process
- retention configuration
- audit retention policy
- replay retention policy
- legal hold posture if required

## Backup And Retention Relationship

Retention is not only database rows.

Retention also includes:

- backups
- exported reports
- generated company-fit artifacts
- logs
- screenshots
- Playwright reports
- local test artifacts

Local artifacts should not be treated as governed production evidence unless intentionally captured and reviewed.

## Generated Artifacts

Generated company-fit reports live under `docs/generated/`.

Governance expectation:

- treat generated reports as reference artifacts, not runtime source of truth
- avoid embedding secrets or live customer-sensitive data
- review generated outputs before sharing externally

## Compliance Boundary

SynapseCore docs should not claim compliance certifications that have not been formally earned.

Do not claim:

- SOC 2 certification
- ISO 27001 certification
- HIPAA readiness
- PCI readiness
- GDPR compliance

unless those programs are actually implemented, audited, and maintained.

## Reviewer Questions

Technical reviewers should ask:

- What data is tenant-scoped?
- What data can cross tenant boundaries?
- What audit events exist?
- How long are replay records retained?
- How are backups retained?
- Can a tenant be exported?
- Can a tenant be deleted?
- How are connector secrets protected?
- What is logged during failures?
- What proof exists for tenant isolation?

## Related Docs

- [security-and-trust-model.md](security-and-trust-model.md)
- [current-limitations.md](current-limitations.md)
- [database-and-migrations.md](database-and-migrations.md)
- [backup-restore-runbook.md](backup-restore-runbook.md)
- [technical-reviewer-guide.md](technical-reviewer-guide.md)

## Bottom Line

SynapseCore's trust story depends on tenant-scoped operational truth.

The next maturity step is not only more features. It is stronger governance around how long operational truth is retained, how it is restored, and how it is removed when required.

