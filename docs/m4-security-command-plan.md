# SynapseCore M4 — Security Command Plan and Adversarial Acceptance

**Owner:** SynapseCore owner; security lead manages review and risk recommendation; independent reviewer validates findings and fixes.
**Program anchor:** M4 — Security and full adversarial verification.
**Status (2026-10-04):** PLANNED. M1 remains OPEN; M4 is BLOCKED by M2/M3.
**Next gates:** M5 remediation and independent re-verification; M6 connected-engine security acceptance; M7 customer-environment launch authorization; M8 monitoring and incident response.
**Authority:** This is a plan, not proof of completed controls, permission for destructive testing, approval to spend, or authorization for a company pilot.

## 1. Mission and non-negotiable invariants

Protect the confidentiality, integrity, availability, authorization, recoverability and auditability of SynapseCore **as an operational intelligence and control engine**, not merely a web application.

- No tenant, warehouse, operator, connector or platform actor may escape its approved authority through REST, WebSocket, cached data, asynchronous work, exports, administration, audit or recovery.
- No unauthorized read, mutation, event, recommendation, approval, replay or identity change; denied attempts must have zero unauthorized side effects.
- Untrusted and duplicated integrations cannot forge authoritative operational facts or create duplicate legal business effects.
- Scenario projections and approvals never execute live business changes; the unsupported Scenario execute route remains 410.
- Accepted order, inventory, reservation, fulfillment, governance and replay outcomes remain consistent through concurrency, failure and recovery.
- Security events and failures are visible and attributable without exposing passwords, tokens, cookies or sensitive payloads.
- A compromised credential, dependency outage or overload must have a bounded, detectable and recoverable impact within the approved pilot envelope.

The owner approves pilot scope, budgets, expenses, residual risk and final go/no-go. The security lead defines threats, evidence and severity; engineers fix findings; a reviewer independent of the change checks closure. Customer stakeholders approve access and testing of their environment. Where one person must fill multiple roles, document the review limitation rather than claim independent testing.

## 2. Ownership across the M stages

| Stage | Security obligation | Required result |
| --- | --- | --- |
| M0 | Inspect current controls, source, evidence, architecture and unknowns. | Honest baseline. |
| M1 — current | Preserve security while hardening; immediately address any confirmed serious exposure, authority breach or corruption. H6, H9, H10 and H11 feed security. | No dangerous invariant ignored for later. |
| M2 | Verify auth, cookie/Origin/CSRF/CORS, tenant scoping, Redis sessions, WebSockets and sensitive flows against the exact hosted build repeatedly. | Revision-bound hosted input to M4. |
| M3 | Finalize clear role/scope, safe confirmation, approval, session, errors, disabled actions and recovery UX. | Finished pilot-scope operator surfaces. |
| **M4** | Run full independent threat, code, configuration, data, infrastructure, browser, integration, operational, load and adversarial assessment below. | Complete attack/assurance ledger, findings and explicit M4 verdict. |
| M5 | Root-cause, minimally fix and independently retest defects; rerun invalidated previous gates. | Zero unresolved pilot-blocking, Critical or in-scope High findings. |
| M6 | Verify security while the full operational engine, operators, integrations, database, dispatch, realtime and recovery operate together. | Technical security and connected-engine sign-off. |
| M7 | Apply real company identities, data, mappings, contracts, deployment, roles, integrations, backup and incident arrangements; rerun applicable proof. | Separate customer-specific launch sign-off. |
| M8 | Monitor, patch, review access, respond to threats, rehearse recovery and pause unsafe reliance. | Measured operating evidence and incident history. |

M4 is the formal security-assurance stage, **not the first day security matters**. Serious flaws discovered earlier are addressed immediately under the active gate. Never move stage status merely because this plan exists.

## 3. Crown jewels, attack surface and threat actors

**Crown jewels:** company and operator identity; platform/bootstrap privileges; sessions and connector secrets; tenant/warehouse mappings; orders/lines/reservations/fulfillment; inventory balances; intelligence and source provenance; replay and dispatch state; Scenario review/approval evidence; audit and events; PostgreSQL, Redis, backups; production deployment and CI credentials; personal/customer data.

**Surfaces:** frontend and its build; HTTP/REST, sessions and exports; tenant and platform control planes; CSV, webhook, pull and direct API integrations; outbound remote fetch; WebSocket handshake, SEND and SUBSCRIBE; scheduler and worker context; database transactions, cache and Pub/Sub; Render/Docker/network/TLS/proxy; GitHub/actions/packages/images; logs, backups and support workflows.

**Actors:** anonymous attacker; credential-stuffing actor; stolen-session holder; compromised operator, tenant admin or platform account; malicious insider; cross-tenant adversary; compromised supplier, upstream API or connector; malicious import sender; compromised dependency/build account; and an attacker exploiting concurrency, degraded dependencies or scarce resources.

**Trust boundaries:** internet to proxy/backend; session to tenant, role, warehouse and object; connector identity to mapped authoritative fact; tenant plane to platform plane; PostgreSQL commit to dispatch/Redis/STOMP/browser; hypothetical Scenario to governed approval; deployment secret to application/logs/backups; support identity to customer scope. Browser CORS or hidden buttons are not substitutes for server authorization.

## 4. Required security control and attack register

Each control is required to have an **actual observed result** for every applicable pilot-scope test. The descriptions are requirements, not claims that current code already satisfies them.

| ID | Family | Verification and required control |
| --- | --- | --- |
| SEC-01 | Inventory and exposure | Enumerate all HTTP routes/verbs, realtime destinations, workers, exports, hosts and sensitive data; reject unintended debug, deprecated and admin exposure. |
| SEC-02 | Authentication | Test invalid/disabled identities, generic errors, credential-stuffing/brute-force defense, password policy/storage, safe provisioning/reset, recovery and tenant non-enumeration. Assess privileged MFA and the pilot's actual contractual requirement; do not claim unbuilt SSO/MFA. |
| SEC-03 | Sessions | Test fixation, renewal, logout, expiry, password change, scope/role revocation, concurrent sessions and re-use of old session material. Verify real Redis state and Secure/HttpOnly/SameSite cookie attributes and accepted revocation time. |
| SEC-04 | RBAC and least privilege | Verify every protected read, write, approval, export, replay, admin action and actor identity with all supported roles. Deny forged actor fields, role escalation and stale authorization. |
| SEC-05 | Tenant/warehouse/IDOR | Use two tenants, at least two warehouses and representative roles; swap IDs, list filters, bodies and query scopes across business data, admin, Scenario, replay, diagnostics, audit, exports, caches and jobs. Zero unauthorized data or metadata. |
| SEC-06 | Platform boundary | Tenant session cannot reach platform control; platform session cannot silently become tenant operator. Bootstrap/provisioning credentials, tenant metadata and support views remain scoped. |
| SEC-07 | Browser security | Verify exact allowed origins, credentials, CSRF/Origin independently of CORS, TLS/HSTS and appropriate headers, XSS-safe rendering, output encoding, clickjacking, redirects, downloads and no embedded client-bundle secrets. |
| SEC-08 | API security | Test object/field/function authorization, mass assignment, input validation, injection, ambiguous bodies, pagination/size ceilings, safe errors, content types and expensive query behavior. |
| SEC-09 | Abuse and rate limiting | Verify authentication, onboarding, password, privileged and integration limits plus high-risk read paths where justified. Check trusted-proxy/X-Forwarded-For spoofing, distributed counters, tenant fairness, exhaustion limits and safe 429 handling; do not equate app rate limits with DDoS protection. |
| SEC-10 | Integration identity | Test scoped webhook/source credentials, secret disable/rotation, wrong-tenant connector, duplicate/conflicting IDs, idempotency, replay/freshness where supported, map-to-tenant authority and exactly one legal business effect. |
| SEC-11 | Imports and remote fetch | Test malformed/oversized/chunked CSV/JSON, row limits, partial-success accounting, safe exports/formula risks where applicable, SSRF, redirects, DNS changes, metadata/private IP destinations, slow upstream and body/time budgets. |
| SEC-12 | Operational integrity | Race reserve, adjust, partial dispatch, cancel, return and duplicate intake. Check legal transitions, stock conservation, intelligence from actual facts, unchanged state on rejection, and authoritative ledger reconciliation. |
| SEC-13 | Replay and dispatch | Test ownership, manual-only policy, cross-tenant replay, lease race/expiry, restarts, duplicate events and write failures. Preserve audit/history and avoid duplicate business effects. |
| SEC-14 | Scenario governance | Test requester identity, assigned review, self-review denial, final approver, escalation, wrong warehouse, simultaneous decisions and terminal states; Scenario must never execute live mutations. |
| SEC-15 | WebSocket/Redis | Forge handshake, SEND, SUBSCRIBE and destinations; test session/scope revocation, cross-tenant Pub/Sub, cache/future keys, reconnect, dropped/reordered updates, Redis loss and authoritative REST repair. |
| SEC-16 | PostgreSQL/data access | Verify least-privileged runtime/migration users, network isolation, parameterization, tenant-scoped repositories, transaction/rollback/deadlock correctness, migrations, backup access and restore. Evaluate DB-level defense in depth from measured risk. |
| SEC-17 | Secrets and rotation | Inventory real and fixture credentials; scan source/history where accessible, CI, frontend bundles, containers, logs and artifacts. Revoke and rotate platform, database, Redis, integration and deployment secrets; no customer values in Git. |
| SEC-18 | Infrastructure | Verify production profile, disabled header fallback, actuator restrictions, TLS/proxy trust, service exposure, container permissions/user, environment separation, access to Render/GitHub, exact serving revision, safe release and rollback. |
| SEC-19 | Supply chain and SDLC | Review Java/npm dependencies, lockfiles, containers/base images, CI actions, SBOM where feasible, scan results by exploitability, branch protections, release access and artifact provenance. Scans do not replace code review or penetration testing. |
| SEC-20 | Data/privacy | Classify customer/personal data; review purpose, minimization, encryption-in-transit/at-rest capability, authorized access/export, retention/deletion, backup treatment, cross-border processing and vendor terms. Map applicable POPIA obligations with qualified review; do not claim compliance from tests alone. |
| SEC-21 | Audit and log security | Record safe UTC/request/actor/tenant/scope/outcome correlations; protect log access, retention and tamper risk. Test rejected operations, secret redaction and forensic reconstruction without sensitive payload leakage. |
| SEC-22 | Monitoring and response | Define abuse/security signals, alert ownership, severity, escalation, incident containment, session/secret revocation, evidence preservation, source reconciliation, customer/provider/regulatory notification decisions and post-incident review. Rehearse an incident. |
| SEC-23 | Availability/fault tolerance | In an isolated approved environment test constrained mixed load, hostile inputs, queue growth, source/DB/Redis outages, crash and representative soak; prove fail-closed authority, bounded recovery, fair tenant treatment and no corruption. |
| SEC-24 | People and third parties | Review least-privilege GitHub/Render/support access, operator onboarding/offboarding, credential handoff, support impersonation policy, provider dependencies, incident contacts, customer responsibilities and periodic access review. |
| SEC-25 | Proof integrity | Review test oracles, mutation before/after evidence, skipped or flaky cases, production/H2 differences, exact SHA, served configuration, true two-tenant fixtures, real PostgreSQL/Redis and independent review. No silent exclusions. |

Use [OWASP ASVS v5.0.0 stable](https://github.com/OWASP/ASVS/releases) as a requirements checklist (Level 2 as a working baseline where applicable, with risk-selected additions), and [OWASP API Security Top 10 2023](https://owasp.org/API-Security/) as a threat checklist. Map applicable requirement IDs in the final coverage matrix. Neither replaces SynapseCore-specific operational and cross-domain invariants.

## 5. M4 test campaign — required order

1. **Scope and authorization:** approve frozen backend/frontend SHA, actual deployed environment, synthetic accounts, routes, two tenants/warehouse fixtures, dataset, load rates, duration, tools, safe test window, backup, incident contacts and stop limits. Confirm M2/M3 entry. No test of unrelated third-party or customer assets without permission.
2. **Baseline and mapping:** enumerate entry points, privileged operations, API-role matrix, trust/data flows, crown jewels, existing tests, CI/deployment configuration and prior failures; record before-state ledgers and open uncertainties.
3. **Static/manual review:** independently inspect exact source and configuration, authorization paths, supply chain, build output, session handling, tenant context, query boundaries, connector fetch paths and frontend rendering. Triage scanners for actual reachability, not raw counts.
4. **Focused adversarial checks:** authentication/session; tenant/warehouse/IDOR; roles/actor forgery; platform separation; malformed inputs; CSRF/CORS/browser; integration identity, SSRF and imports; replay/Scenario; WebSocket/Redis; secrets/exposure. Every rejected attempt requires a database/event/cache/UI no-effect oracle, not only a status code.
5. **Cross-system and concurrency:** simultaneously exercise multiple scoped operators, connectors, schedulers, operational mutations, approval conflicts, replay, dispatch and browser updates. Reconcile source -> persisted truth -> audit/event -> notification -> REST -> UI, per tenant and warehouse.
6. **Resilience and recovery:** on an isolated production-shaped target, test approved stress/soak, Redis/DB/upstream outages, process restart, secret rotation and revocation, session expiry during an action, sanitized backup restore, and one simulated security incident. Declare resource, RPO/RTO and stop budgets before running.
7. **Hosted proof:** confirm exact serving SHA, secure cookies, origin/CSRF/CORS, TLS/proxy behavior, rate limits and trusted-IP derivation, log redaction, new frontend bundle secret scan, role/isolation/realtime/replay outcomes against the actual deployed pair.
8. **Independent assessment:** reproduce and classify each finding, record negative results, identify unknowns, agree root-cause questions, issue signed M4 assessment and hand findings to M5.

Never launch destructive, saturation, port-scanning, fault-injection or restore tests against a shared Render Free DB or customer systems. Authorization to run hosted E2E is not authorization to overload the provider. Stop at unsafe mutation, unauthorized data exposure, corruption, loss of recovery, or exceeded test budget. Keep first-failure evidence.

## 6. Measurable acceptance and severity

Every test record: ID/control/reference; exact source and served build; environment/production flags; synthetic tenant, warehouse and role; UTC window; authorized input/rate; expected and actual HTTP/DB/event/realtime/UI outcomes; request IDs and sanitized traces; pass/fail/blocked/skipped; severity; owner; reproducibility; evidence location; reviewer and accepted residual risk.

**Core oracles:**
- Zero unauthorized tenant/warehouse/object reads, metadata disclosure, business writes, broadcasts, actor changes, approvals or recovery actions.
- Rejected operations leave unchanged business ledgers and no unauthorized dispatch, cache mutation or event.
- Session revocation and integration-key rotation invalidate former authority within predeclared, measured bounds.
- Accepted duplicate/concurrent business input preserves idempotency, inventory conservation, legal governance and complete audit.
- No live or customer secret exposed in outward source/docs, build, logs, errors or test artifacts.
- Accepted workload meets predeclared throughput, latency, data freshness, DB/Hikari/Redis headroom, tenant fairness and recovery thresholds; no silent false success.
- Sanitized backup restores under owner-approved measured RPO/RTO; incident owner can reconstruct and contain a simulated event.

Set numeric workload, repetition, alert response, session revocation, restore and retention limits **before** tests and record the decision owner. Do not invent thresholds after a failure or weaken them to obtain a pass.

**Critical:** serious tenant isolation breach, platform takeover, unauthorized high-impact mutation, critical real-secret exposure or unrecoverable corruption: stop affected work and block downstream stages. **High:** material exploitable privacy, authority, availability or recovery weakness in pilot scope: block pilot until independently verified. **Medium/Low:** assess effect and reachability; fix proportionately or require bounded limitation, mitigation, owner and explicit acceptance. A Medium can be a gate blocker. An untested critical path is OPEN, never a pass.

**M4 exit:** every predeclared in-scope attack has an inspectable result, all critical paths are covered, findings and negative results are documented, and the reviewer/owner accept a complete examination. M4 may pass *to M5* with findings; M4 is not pilot authorization.

**M5 exit:** zero unresolved in-scope Critical, High or other pilot blockers; root cause and smallest justified fix; failing-then-passing regression; affected local/CI/hosted proof; independent confirmation; rerun any invalidated M2/M3/M4 checks.

**M6 exit:** frozen final candidate demonstrates all security oracles alongside the full connected engine, mixed concurrency, UI, realtime, governance and recovery. Owner signs technical readiness only after accepted complete evidence.

**M7 exit:** applicable tests rerun against actual customer identities, tenant/warehouse scopes, source mappings, environment, workload and contract. Confirm privileged access, credential handoff, personal-data obligations, always-on measured hosting, backup, rollback, security/incident contacts and explicit customer/owner launch approval.

## 7. M7/M8 security operations and incident command

Maintain a privileged-user/secret inventory, access reviews, operator offboarding, vulnerability/dependency review, scoped backups and restore drills, safe retained logs, alert ownership, incident communications, periodic credential rotation on risk or agreement, and customer/provider security contacts.

**Incident sequence:** detect -> record UTC/request/actor/tenant/source -> assess scope and impact -> contain affected account, token, connector, tenant action or service -> preserve sanitized evidence -> revoke/rotate compromised access -> reconcile authoritative operational and audit state -> recover/replay only through approved controls -> rerun negative, isolation, integrity and realtime checks -> determine customer/provider/regulator notification responsibilities with the designated legal/responsible person -> publish corrective-action and independent-review record. Pause the pilot's affected reliance on isolation failure, corruption, unbounded privileged activity or loss of recovery.

Customer agreements and onboarding must define company data ownership, authorized processing/integration, access/export/deletion/retention, backup handling, privileged support, incident coordination, notification decisions, operating limits and exit/revocation. Review applicable South African POPIA and contractual obligations with appropriately qualified advice, rather than treating a technical scan as a legal opinion.

## 8. Baseline, gaps and required outputs

**Documented existing inputs:** workspace/session-first authentication and production Redis-session intent; production header-fallback restriction; backend role/tenant negative tests; selected rate limits; exact-origin CORS tests; safe error/requestId cases; local CSV size rejection; Replay and Scenario governance; restricted production actuator endpoints; secret/leak scans; restore scripts and historical production-shaped proof. Their results remain revision- and environment-specific.

**Not yet accepted for M4:** full fresh-candidate review; hosted cookie/Origin/CSRF/CORS; complete warehouse/IDOR and WebSocket/cache/worker scoping; trusted-proxy rate-limit behavior; current outbound fetch and import attack surface; secrets and bundle after the final build; log redaction; independent dependency/container/CI review; incident rehearsal; repeated secure concurrency/failure/restore; applicable customer privacy/contract scope and M6/M7 security proof. These are verification requirements, not assertions that vulnerabilities exist.

**Do not auto-add:** enterprise SSO/SAML/OIDC, broad SIEM, WAF, SOC 2 certification, multi-region recovery, DB row-level security or purchased testing tools without a measured threat, contractual need, justified benefit and owner decision. No paid capacity or security service without prior owner approval. Never weaken an in-scope security invariant to fit Render Free.

**Deliverables:** authorization/scope record; asset and trust-boundary inventory; threat/risk register; route-by-role and tenant/warehouse matrix; SEC-01–25 control/ASVS coverage ledger; code/dependency/secrets/config review; adversarial and negative-effect evidence; approved load/fault/recovery and incident-drill reports; signed M4 assessment; M5 fix/re-verification register; M6 whole-engine acceptance; M7 customer-security launch record; M8 monitoring and response playbooks.

**Connected documents:** [master engineering readiness map](SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md), [security and trust model](security-and-trust-model.md), [security test plan](security-test-plan.md), [leakage audit](leakage-audit.md), [platform boundary](platform-control-plane-access-boundary.md), [role authority gate](role-authority-hardening-gate.md).
