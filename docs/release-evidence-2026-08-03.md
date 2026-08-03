# Release Evidence - August 3, 2026

This document freezes the live proof evidence after the Render PostgreSQL replacement database was provisioned, migrated, bootstrapped, and revalidated through the hosted proof lane.

It is an evidence note, not a marketing claim.

## Release Snapshot

| Field | Value |
| --- | --- |
| Date | August 3, 2026 |
| Git commit | `00611ea4050105ad0dde1b25563d9b8f5eefd2e1` |
| Frontend | `https://synapscore-frontend-3.onrender.com` |
| Backend | `https://synapscore-3.onrender.com` |
| Proof tenant | `HOSTED-PROOF-2` |
| Proof tenant name | `HOSTED-PROOF 2 Hosted Verification` |
| Proof result | `6 passed (4.1m)` |
| Classification | `FULLY REAL for current supported scope` |

## What Was Revalidated

The hosted proof revalidated the current deployed platform against the replacement PostgreSQL database:

1. auth flow and the full authenticated page system
2. product catalog onboarding through tenant-scoped API and browser surface
3. realtime dashboard summary updates without browser refresh
4. replay recovery, scenario approval, execution, and browser role gating through the UI
5. alerts, recommendations, orders, inventory, integrations, users, profile, and settings connected to the live backend
6. frontend-visible backend auth rate limiting without stuck loading

## Infrastructure Truth

The replacement database is no longer just provisioned; it has been exercised through real application flows.

The proof path used:

- live Render frontend
- live Render backend
- replacement Render PostgreSQL database
- Redis-backed session/realtime posture where configured by the live backend
- generated proof tenant and proof users created through supported APIs
- Playwright browser proof against deployed services

No manual database row edits were used.

## Proof Preparation Truth

`scripts\prepare-hosted-proof.ps1` successfully:

- waited for backend readiness
- verified unauthenticated auth/session
- verified `/ws/info`
- verified the frontend sign-in shell
- created or prepared the proof workspace through supported tenant APIs
- ensured proof operators and users
- prepared catalog and inventory baseline
- verified authenticated dashboard summary, runtime, and dashboard snapshot

## Fixes Applied During Revalidation

The proof run exposed real proof-tooling and selector drift seams after frontend productization:

- hosted proof state was moved outside Playwright `test-results`
- hosted proof state reads were made BOM-safe for Windows-created JSON
- cooldown writes now preserve proof tenant and password state
- Alerts proof selector was updated from removed `#alerts-focus` to current `#alerts-response`
- Settings proof selector was updated from `Tenant Name` to `Company workspace name`

These fixes did not change backend contracts or product runtime behavior.

## Local Artifacts

The Playwright HTML report, screenshots, traces, and proof state remain local-only:

- `frontend\playwright-report\`
- `frontend\test-results\`
- `frontend\.hosted-proof\`

Do not commit proof passwords or local Playwright artifacts.

## Current Verdict

- `FRONTEND_UP=True`
- `BACKEND_UP=True`
- `DB_READY=True`
- `AUTH_READY=True`
- `WS_READY=True`
- `HOSTED_PROOF=PASSED`
- `FULL_SYSTEM_REVALIDATION=GREEN`

SynapseCore is ready for pilot-readiness packaging and controlled adoption work within the current supported scope.
