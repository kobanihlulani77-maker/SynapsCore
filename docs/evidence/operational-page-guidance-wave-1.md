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
