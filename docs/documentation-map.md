# Documentation Map

This document helps readers navigate the SynapseCore documentation without getting lost in overlap.

The repo contains product, architecture, recovery, proof, pilot, roadmap, reviewer, governance, observability, and risk material. That is useful only if people know where to start.

## Recommended Reading Path

If someone is new to the project, the recommended reading order is:

1. [README.md](../README.md)
2. [repository-review-report.md](repository-review-report.md)
3. [executive-summary.md](executive-summary.md)
4. [company-explainer.md](company-explainer.md)
5. [system-architecture.md](system-architecture.md)
6. [infrastructure-handbook.md](infrastructure-handbook.md)
7. [proof-and-validation.md](proof-and-validation.md)
8. [deployment-recovery-guide.md](deployment-recovery-guide.md)
9. [master-product-roadmap.md](master-product-roadmap.md)

That path moves from high-level meaning into real architecture, trust, recovery, and long-term direction.

## If You Are An Engineer

Read first:

- [system-architecture.md](system-architecture.md)
- [engineering-review.md](engineering-review.md)
- [maintainability-guide.md](maintainability-guide.md)
- [quality-gates.md](quality-gates.md)
- [environment-reference.md](environment-reference.md)
- [frontend-flow.md](frontend-flow.md)
- [backend-flow.md](backend-flow.md)
- [api-surface-reference.md](api-surface-reference.md)
- [system-communication-map.md](system-communication-map.md)
- [proof-and-validation.md](proof-and-validation.md)
- [contributor-guide.md](contributor-guide.md)
- [engineering-priorities.md](engineering-priorities.md)

Use these when:

- understanding code boundaries
- changing frontend or backend behavior
- protecting proof selectors
- understanding runtime trust
- following quality gates before release

## If You Are Supporting A Pilot

Read first:

- [support-playbook.md](support-playbook.md)
- [operations-handbook.md](operations-handbook.md)
- [change-management.md](change-management.md)
- [release-engineering.md](release-engineering.md)
- [repository-maturity.md](repository-maturity.md)

Use these when:

- classifying incidents
- preparing releases
- collecting evidence
- deciding whether a change is safe to deploy
- understanding current maturity without inflated enterprise claims

## If You Are An Operator

Read first:

- [company-explainer.md](company-explainer.md)
- [operator-incident-guide.md](operator-incident-guide.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)
- [failure-classification-matrix.md](failure-classification-matrix.md)
- [render-recovery-playbook.md](render-recovery-playbook.md)
- [local-recovery-playbook.md](local-recovery-playbook.md)
- [backup-restore-runbook.md](backup-restore-runbook.md)

Use these when:

- interpreting degraded states
- deciding whether to escalate
- understanding replay, waiting, reconnecting, or blocked behavior

## If You Are A Buyer Or Pilot Sponsor

Read first:

- [executive-summary.md](executive-summary.md)
- [company-explainer.md](company-explainer.md)
- [buyer-due-diligence-guide.md](buyer-due-diligence-guide.md)
- [pilot-adoption-roadmap.md](pilot-adoption-roadmap.md)
- [pilot-acceptance-criteria.md](pilot-acceptance-criteria.md)
- [pilot-faq.md](pilot-faq.md)

Use these when:

- evaluating fit
- planning a pilot
- understanding the safe adoption envelope

## If You Are A Technical Reviewer

Read first:

- [technical-reviewer-guide.md](technical-reviewer-guide.md)
- [security-and-trust-model.md](security-and-trust-model.md)
- [operations-reliability.md](operations-reliability.md)
- [proof-and-validation.md](proof-and-validation.md)
- [observability-and-metrics-reference.md](observability-and-metrics-reference.md)
- [data-governance-and-retention.md](data-governance-and-retention.md)
- [known-operational-risks.md](known-operational-risks.md)
- [current-limitations.md](current-limitations.md)
- [repository-maturity.md](repository-maturity.md)

Use these when:

- evaluating trust posture
- reviewing architecture discipline
- checking how honestly limitations are handled
- understanding evidence, retention, observability, and operational risk posture

## If You Need Recovery Guidance

Core docs:

- [deployment-recovery-guide.md](deployment-recovery-guide.md)
- [failure-classification-matrix.md](failure-classification-matrix.md)
- [render-recovery-playbook.md](render-recovery-playbook.md)
- [local-recovery-playbook.md](local-recovery-playbook.md)
- [backup-restore-runbook.md](backup-restore-runbook.md)
- [resilience-philosophy.md](resilience-philosophy.md)
- [troubleshooting-index.md](troubleshooting-index.md)

Use these when:

- backend is timing out
- readiness is failing
- DB or Redis are degraded
- proof must be paused
- restore posture needs to be classified

## If You Need Proof And Testing Guidance

Core docs:

- [proof-and-validation.md](proof-and-validation.md)
- [hosted-proof.md](hosted-proof.md)
- [proof-system-evolution.md](proof-system-evolution.md)
- [frontend-qa-checklist.md](frontend-qa-checklist.md)
- [frontend-demo-guide.md](frontend-demo-guide.md)
- [release-process.md](release-process.md)

Use these when:

- checking what is actually proven
- deciding whether proof should run
- protecting proof selectors and flows

## If You Need Product Vision And Roadmap

Core docs:

- [final-product-vision.md](final-product-vision.md)
- [product-purpose.md](product-purpose.md)
- [master-product-roadmap.md](master-product-roadmap.md)
- [platform-potential.md](platform-potential.md)
- [enterprise-hardening-roadmap.md](enterprise-hardening-roadmap.md)

Use these when:

- aligning product direction
- deciding what is current vs future
- understanding what still needs hardening

## If You Need Deep Reference Docs

These are stronger reference-style docs rather than first-read docs:

- [api-spec.md](api-spec.md)
- [api-surface-reference.md](api-surface-reference.md)
- [data-flow-playbook.md](data-flow-playbook.md)
- [infrastructure-communication-map.md](infrastructure-communication-map.md)
- [system-communication-map.md](system-communication-map.md)
- [master-project-tree.md](master-project-tree.md)
- [runtime-observability.md](runtime-observability.md)
- [observability-and-metrics-reference.md](observability-and-metrics-reference.md)
- [integration-operations.md](integration-operations.md)
- [database-and-migrations.md](database-and-migrations.md)
- [data-governance-and-retention.md](data-governance-and-retention.md)
- [known-operational-risks.md](known-operational-risks.md)

## If You Need Release Or Environment Guidance

Read first:

- [environment-reference.md](environment-reference.md)
- [release-process.md](release-process.md)
- [release-engineering.md](release-engineering.md)
- [quality-gates.md](quality-gates.md)
- [go-live-checklist.md](go-live-checklist.md)
- [backup-restore-runbook.md](backup-restore-runbook.md)
- [troubleshooting-index.md](troubleshooting-index.md)

## If You Need Pilot Acceptance Guidance

Read first:

- [pilot-program-guide.md](pilot-program-guide.md)
- [pilot-adoption-roadmap.md](pilot-adoption-roadmap.md)
- [pilot-acceptance-criteria.md](pilot-acceptance-criteria.md)
- [known-operational-risks.md](known-operational-risks.md)

Use these when:

- deciding whether a pilot is actually accepted
- separating local success from hosted readiness
- defining expansion criteria before adding more sites, connectors, or operators

## Overlap Guidance

Some overlap is intentional.

Examples:

- [company-explainer.md](company-explainer.md) is plain-language product framing
- [buyer-due-diligence-guide.md](buyer-due-diligence-guide.md) is the buyer-trust and evaluation layer
- [technical-reviewer-guide.md](technical-reviewer-guide.md) is the engineering credibility layer
- [proof-and-validation.md](proof-and-validation.md) is proof philosophy and verification discipline
- [deployment-recovery-guide.md](deployment-recovery-guide.md) is operational recovery guidance
- [data-governance-and-retention.md](data-governance-and-retention.md) is the data responsibility layer
- [known-operational-risks.md](known-operational-risks.md) is the living operational risk register

The goal is not zero overlap. The goal is that each document has a distinct audience and job.

## Bottom Line

If someone only reads one map document before diving deeper, it should be this one.

Use this page as the routing layer for the full documentation set.
