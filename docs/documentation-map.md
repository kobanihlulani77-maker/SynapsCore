# Documentation Map

This document helps readers navigate the SynapseCore documentation without getting lost in overlap.

The repo now contains product, architecture, recovery, proof, pilot, roadmap, and reviewer material. That is useful, but only if people know where to start.

## Recommended Reading Path

If someone is new to the project, the recommended reading order is:

1. [README.md](../README.md)
2. [executive-summary.md](executive-summary.md)
3. [company-explainer.md](company-explainer.md)
4. [system-architecture.md](system-architecture.md)
5. [infrastructure-handbook.md](infrastructure-handbook.md)
6. [proof-and-validation.md](proof-and-validation.md)
7. [deployment-recovery-guide.md](deployment-recovery-guide.md)
8. [master-product-roadmap.md](master-product-roadmap.md)

That path gives a good sequence from high-level meaning to real architecture, then into trust, recovery, and long-term direction.

## If You Are An Engineer

Read first:

- [system-architecture.md](system-architecture.md)
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

## If You Are An Operator

Read first:

- [company-explainer.md](company-explainer.md)
- [operator-incident-guide.md](operator-incident-guide.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)
- [render-recovery-playbook.md](render-recovery-playbook.md)
- [local-recovery-playbook.md](local-recovery-playbook.md)

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
- [current-limitations.md](current-limitations.md)

Use these when:

- evaluating trust posture
- reviewing architecture discipline
- checking how honestly limitations are handled

## If You Need Recovery Guidance

Core docs:

- [deployment-recovery-guide.md](deployment-recovery-guide.md)
- [failure-classification-matrix.md](failure-classification-matrix.md)
- [render-recovery-playbook.md](render-recovery-playbook.md)
- [local-recovery-playbook.md](local-recovery-playbook.md)
- [resilience-philosophy.md](resilience-philosophy.md)
- [troubleshooting-index.md](troubleshooting-index.md)

Use these when:

- backend is timing out
- readiness is failing
- DB or Redis are degraded
- proof must be paused

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
- [integration-operations.md](integration-operations.md)
- [database-and-migrations.md](database-and-migrations.md)

## If You Need Release Or Environment Guidance

Read first:

- [environment-reference.md](environment-reference.md)
- [release-process.md](release-process.md)
- [go-live-checklist.md](go-live-checklist.md)
- [troubleshooting-index.md](troubleshooting-index.md)

## Overlap Guidance

Some overlap is intentional.

Examples:

- [company-explainer.md](company-explainer.md) is plain-language product framing
- [buyer-due-diligence-guide.md](buyer-due-diligence-guide.md) is the buyer-trust and evaluation layer
- [technical-reviewer-guide.md](technical-reviewer-guide.md) is the engineering credibility layer
- [proof-and-validation.md](proof-and-validation.md) is proof philosophy and verification discipline
- [deployment-recovery-guide.md](deployment-recovery-guide.md) is operational recovery guidance

The goal is not zero overlap. The goal is that each document has a distinct audience and job.

## Bottom Line

If someone only reads one map document before diving deeper, it should be this one.

Use this page as the routing layer for the full documentation set.
