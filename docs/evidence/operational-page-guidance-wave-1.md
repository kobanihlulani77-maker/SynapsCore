# Operational Page Guidance Wave 1 Evidence

Status: implementation evidence record

## Scope

Wave 1 covers the Platform Owner control plane only:

- Platform Overview: `/platform-admin`
- Tenant Directory: `/tenant-management`
- Platform Runtime: `/system-config`
- Platform Activity: `/platform-activity`
- Release Trust: `/releases`
- Platform Owner sign-in: `/platform-sign-in`

Wave 2, tenant operating workflows, and Phase 14 are not part of this record.

## Starting Point

- Starting commit: `99d15902fa566333442f9264b2024555875e7e09`
- Unrelated pre-existing local changes intentionally excluded: `frontend/Dockerfile`, `.gitattributes`

## Architecture Decision

Before Wave 1, `PlatformApplication` owned session and routing but rendered a shallow inline substitute for the platform pages. Dedicated page files existed but were not authoritative. Wave 1 makes one architecture explicit:

1. `PlatformApplication` owns the dedicated platform session, protected data loading, route transitions, and state boundaries.
2. `PlatformAdmin.jsx`, `Tenants.jsx`, `SystemConfig.jsx`, and `Releases.jsx` are the rendered page authorities.
3. `PlatformActivity.jsx` is the dedicated metadata-only activity surface and reads `/api/platform/activity`.
4. Backend contracts and platform authorization remain unchanged.

No raw tenant business payloads are requested or rendered by these pages. Platform responses remain limited to tenant metadata/counts, runtime/build facts, and activity metadata.

## Route Inventory

Before: five platform registry entries including sign-in and four protected pages.

After: six platform registry entries including the new `/platform-activity` route. The protected backend routes remain `/api/platform/overview`, `/api/platform/tenants`, `/api/platform/runtime`, and `/api/platform/activity`.

## Page Changes

### Platform Overview

The page now renders through the dedicated Platform Owner page component. It presents cross-tenant posture, attention signals, runtime evidence, release trust, and an explicit metadata-only boundary. Empty portfolio and incident states remain distinct from the loading state owned by the application shell.

### Tenant Directory

The page now renders the dedicated onboarding/portfolio component. Provisioning is separated from portfolio review. The directory uses only the backend tenant summary fields: active state, user/operator counts, connector counts, inbound/replay attention, active alerts, support state, and update time. Creation remains an existing supported API flow and is not a database edit.

### Platform Runtime

The page now renders the dedicated runtime component. It translates actual runtime fields into display-safe guidance for liveness, readiness, realtime broker mode, queue pressure, origins, secure cookies, access pressure, and failure signals. Unknown values remain unknown; they are not converted into healthy status.

### Platform Activity

The new route reads the protected `/api/platform/activity` endpoint directly. It shows tenant code, category, condition, status, and observed time only. It explicitly distinguishes an empty feed from an unavailable feed and documents the privacy boundary and next checks.

### Release Trust

The page now renders the dedicated release component. It separates deployed build identity and runtime evidence from release acceptance. A reported build is not presented as proof of acceptance.

### Platform Owner Sign-In

The sign-in surface remains dedicated and separate from tenant authentication. Session errors remain visible, and signed-out users do not receive protected platform content.

## State Handling

- Loading: application-level loading state with `LoadingState`.
- Empty: page-specific empty messages, including the activity feed.
- Error: visible retry/error state; unavailable evidence is not presented as empty or healthy.
- Degraded: runtime status and queue/realtime signals remain visible as reported.
- Unknown: missing runtime/build/activity values retain an unknown or unreported label.
- Success: tenant creation reports success only after the backend returns successfully.

## Privacy And Authority Evidence

The backend source and targeted regression suite confirm that platform endpoints require the dedicated Platform Owner session. The frontend consumes only the metadata DTOs exposed by those endpoints. No frontend source change grants tenant roles platform authority.

## Verification

Observed in this Wave 1 pass:

- Frontend lint: PASS (`npm.cmd run lint`)
- Frontend production build: PASS (`npm.cmd run build`)
- Frontend verification: PASS (`npm.cmd run verify`)
- Backend platform access-boundary regression: PASS, 14 tests, 0 failures, 0 errors (`PlatformTenantAccessBoundaryIntegrationTest`)
- Documentation link check: PASS, 764 local links checked, none missing (`scripts/docs-link-check.ps1`)
- Secret scan: PASS, 0 critical findings. Five existing fixture findings are limited to committed starter/test fixture sources and are not Wave 1 changes (`scripts/secret-scan.ps1`)
- Git diff check: PASS (`git diff --check`)
- Repository health: `NEEDS_ATTENTION` because ignored local artifacts/logs and the unrelated pre-existing local changes are present; no cleanup was performed in this focused wave.
- Hosted proof: not rerun in this pass; no new hosted result is claimed here.
- Rendered browser walkthrough: not run in this pass; no rendered result is claimed here.

The remaining repository checks are recorded at closure time below rather than inferred.

## Findings

- Critical: none identified in the Wave 1 source review.
- High: none identified in the Wave 1 source review.
- Medium/Low: platform page rendering still depends on the existing shared shell CSS and should receive the planned cross-page responsive/rendered audit before final gate closure.
- Medium/Low: tenant provisioning remains an operationally sensitive action and should continue to be exercised only with disposable/synthetic workspaces during proof.

## Readiness

Wave 1 is ready for its focused validation and review. Wave 2 is not started by this change. Final acceptance remains dependent on the complete check set and any required rendered/live verification being run and recorded truthfully.

## Repository Closure

- Implementation commit: `440579994f720397dc45aca43dda72df9f2f0a17`
- Push result: pushed to `origin/main`
- Local HEAD and `origin/main`: matched at the implementation commit before this evidence closure update
- Final working tree after the focused commit: unrelated pre-existing `frontend/Dockerfile` modification and untracked `.gitattributes` remain unstaged; no Wave 1 files remain unstaged.

## Final Live / Rendered Validation

Validation date: 2026-08-24

The deployed frontend and backend were reachable during the final walkthrough. The backend reported the deployed runtime identity `render-deploy`; the frontend served the post-fix asset `assets/index-eWgrtGRH.js`. The frontend runtime does not expose a Git SHA, so the asset confirms the redeployed source behavior but is not claimed as an independently reported frontend commit identity. The source correction was committed and pushed as `e7985a3` (`Fix platform runtime contract rendering`).

### 33-Point Closure Report

1. Deployed revision: backend `render-deploy`; frontend post-fix asset `index-eWgrtGRH.js`; source fix `e7985a3` pushed to `origin/main`.
2. Platform sign-in: PASS. The dedicated `/platform-sign-in` shell loaded and the authorized Platform Owner session was established in the live browser.
3. Platform Overview: PASS. `/platform-admin` rendered `Platform overview and cross-tenant trust`.
4. Overview depth: PASS. Six workspaces, incidents, dispatch pressure, attention signals, current tenant context, scope boundary, and rollout/release actions rendered without raw customer payloads.
5. Tenant Directory: PASS. `/tenant-management` rendered `Tenant onboarding and workspace rollout`.
6. Tenant depth: PASS. Visible workspaces, provisioning posture, tenant isolation, workspace code boundary, administrator boundary, and rollout context rendered.
7. Platform Runtime: PASS. `/system-config` rendered `System configuration and operational defaults` and `Platform runtime trust`.
8. Runtime depth: PASS. Overall, liveness, readiness, realtime mode, secure-cookie posture, distributed mode, and dispatch counts rendered from the platform runtime DTO. The live response reported one failed dispatch and the page correctly showed `Investigate` rather than hiding the attention signal.
9. Platform Activity: PASS. `/platform-activity` rendered `Platform activity and evidence`.
10. Activity depth: PASS. Twenty metadata-only signals rendered with tenant code, category, condition, status, and observed time.
11. Release Trust: PASS. `/releases` rendered `Release, deployment, and environment` and the release trust surface.
12. Release depth: PASS. Backend version, reported commit, build time, frontend reported identity, profiles, readiness, and the distinction between runtime evidence and proof acceptance rendered.
13. Navigation: PASS. The five control-plane navigation buttons routed to their expected URLs; `Open workspace rollout` routed from `/platform-admin` to `/tenant-management`. Active styling was not used as evidence because the browser snapshot did not expose the CSS class state.
14. Direct route protection: PASS. After sign-out, direct navigation to `/platform-admin` returned the dedicated platform sign-in shell without control-plane navigation.
15. Tenant-role denial: PASS by the existing deployed boundary evidence and `PlatformTenantAccessBoundaryIntegrationTest` baseline. No tenant role was granted platform authority during this walkthrough.
16. Platform/tenant separation: PASS. The platform pages showed portfolio metadata, runtime/build facts, and activity metadata only; no orders, products, inventory, inbound bodies, replay payloads, connector secrets, or credentials were rendered.
17. Sign-out: PASS. `Sign out platform owner` returned the browser to `/platform-sign-in` and removed the authenticated control-plane navigation.
18. Loading state: PASS. Runtime/activity loading transitions were observed during route warm-up and resolved into page content. The post-fix runtime route no longer blanked after data resolution.
19. Empty state: NOT LIVE EXERCISED. The deployed account had six workspaces and twenty activity signals, so the natural empty feed state was not available without mutating live data. Source-level empty handling remains documented.
20. Error state: NOT LIVE EXERCISED. No safe live fault injection was performed. The source-level retry/error boundary remains documented; no fabricated live error result is claimed.
21. 1366x768 layout: PASS. All five authenticated platform routes reported `document` and `body` widths of 1366 at an inner width of 1366, with no horizontal overflow.
22. Privacy: PASS. The Activity page explicitly presented visible metadata and a not-visible list for customer payloads, replay bodies, secrets, and credentials.
23. Next-action guidance: PASS. Runtime displayed `Review dispatch pressure` / `Investigate` for the reported failed dispatch; Activity, Overview, and Release Trust linked operators to the appropriate next evidence surface.
24. Defect discovered: the first live deployment rendered `/system-config` blank and logged `Cannot read properties of undefined (reading 'dispatchIntervalMs')`; Platform Overview and Release Trust also read queue counts from the wrong tenant runtime nesting.
25. Defect fixed: `SystemConfig.jsx` now consumes the platform runtime DTO directly; `PlatformAdmin.jsx` and `Releases.jsx` now consume top-level platform dispatch counts. No backend contract or proof selector was changed.
26. Redeploy confirmation: the live frontend served a new asset containing the platform runtime trust/activity behavior and no old `runtime.backbone` marker. A fresh browser tab rendered `/system-config` with zero new browser errors.
27. Verification checks: frontend verify/build passed after the fix; live signed-out `/api/platform/overview`, `/api/platform/runtime`, and `/api/platform/activity` returned 403. `/api/platform/session` returned 200 with the signed-out session state, which is expected for session discovery.
28. Evidence/fix commit: source correction `e7985a3`; this document is the subsequent evidence-only closure update.
29. Push: source correction was pushed successfully to `origin/main`; the evidence update is pending its focused commit.
30. Critical blockers: 0 identified in the Wave 1 source and live walkthrough.
31. High blockers: 0 identified in the Wave 1 source and live walkthrough.
32. Wave 2 readiness: Wave 1 is closed; Wave 2 is not started by this validation. The real failed-dispatch signal should remain operationally visible and be handled through the existing support/recovery process.
33. Final Wave 1 verdict: `OPERATIONAL PAGE GUIDANCE WAVE 1 FULLY ACCEPTED`.

### Closure Notes

- The first live defect was treated as a real contract/rendering regression, not masked with a fallback or a selector change.
- The failed dispatch count is an actual backend-reported attention signal. It is not classified as a Wave 1 rendering blocker because the corrected page exposes it and directs investigation.
- No new tenant, business data, credentials, or database rows were created during rendered validation.
- Wave 2 and Phase 14 remain explicitly out of scope.
