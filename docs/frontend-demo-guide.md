# Frontend Demo Guide

## Goal

Use this guide to walk someone through the SynapsCore frontend as a polished enterprise operations product, even when the backend or database is not fully available.

## Local Run

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\frontend
npm.cmd install
npm.cmd run lint
npm.cmd run verify
npm.cmd run dev
```

Open:

```text
http://localhost:5173
```

## What Works Without Backend

These surfaces should still render cleanly when the backend or database is unavailable:

- Public homepage
- Controlled company provisioning is an internal/protected operation
- Sign-in experience and workspace copy
- Global design system, shell styling, and responsive layouts
- Empty, unavailable, and placeholder states on most authenticated surfaces if a session is already established in a local demo build

## Demo Preview Mode Status

- Demo preview mode is documented but not enabled in the runtime yet.
- Planned activation contract: `VITE_DEMO_MODE=true`
- See [docs/frontend-demo-mode.md](frontend-demo-mode.md) for the safe rollout plan.
- Do not present demo preview mode as live proof once it is implemented.

## What Requires Backend + Database

To show real operational flow, use a live backend and database for:

- Workspace authentication
- Dashboard live metrics
- Orders, inventory, alerts, recommendations
- Replay and integration recovery
- Scenario history, approvals, runtime data
- User/admin workspace data

## Recommended Demo Path

### 1. Start on the public homepage

Show:

- SynapsCore as a live operations command center
- Who it is for
- Pain points and product trust language

Message:

"This is not just reporting. It is the operational front door for a company workspace."

### 2. Explain Controlled Onboarding

Show:

- The controlled company rollout explanation on the homepage
- The existing-workspace sign-in surface
- The contact/pilot path for a new company conversation

Message:

"A workspace is the company boundary. SynapseCore provisions the approved
company setup, then operators enter with workspace code, username, and password."

Honest note:

- The UX is productized now.
- Full live provisioning can be connected to backend onboarding APIs later.

### 3. Open Sign In

Show:

- Company workspace code
- Operator identity
- Password/session posture

Message:

"This is the secure entry into a company operations workspace, not a generic admin login."

### 4. If backend is available, sign in and open Dashboard

Show:

- Live operational command center
- Executive signal cards
- Activity rail
- Guidance panel

Message:

"The dashboard is the control room. It shows what is live, what is at risk, and what the team should do next."

### 5. Walk the core operational surfaces

Recommended order:

1. Orders
2. Inventory
3. Alerts
4. Recommendations
5. Replay Queue
6. Integrations
7. Scenario History
8. Approvals

Message:

"Each page is designed as an operator work surface with summary posture, working area, and detail context."

### 6. Finish on admin and trust surfaces

Recommended order:

1. Users
2. Profile
3. Company Settings
4. Runtime
5. Releases / Platform Admin

Message:

"The support and trust surfaces are part of the same product, not a separate engineering console."

## If Backend Is Down During Demo

Use this fallback story:

- Start on homepage
- Move to Contact SynapseCore
- Move to Sign In
- Explain the authenticated command-center design using the now-polished layout and route structure
- Be explicit that live operational data requires backend connectivity

Do not claim that live order, replay, or runtime data is available if the backend is down.

## Frontend Verification Commands

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\frontend
npm.cmd run lint
npm.cmd run build
npm.cmd run verify
```

## Screenshot Capture Note

No extra screenshot tooling is required for a simple walkthrough.

Practical options:

- Use the browser's native screenshot capture
- Use operating-system screenshots
- If Codex browser automation is available in your environment, use it for route-by-route captures of localhost

## Demo Checklist

- Start with the public story before showing operations
- Explain workspace code in plain language
- Keep "tenant" language secondary to "company workspace"
- Show one operations page, one recovery page, and one trust/admin page
- Be honest about placeholder onboarding or backend-dependent data
