# Frontend QA Checklist

## Build and Launch

- [ ] `npm.cmd run lint` completes successfully
- [ ] `npm.cmd run verify` completes successfully
- [ ] `npm.cmd run build` completes successfully
- [ ] `npm.cmd run dev` starts without route or asset errors
- [ ] Public homepage loads at `http://localhost:5173`

## Public Routes

- [ ] Homepage hero, trust, workspace explanation, and CTA sections render cleanly
- [ ] No public workspace-creation route or provisioning CTA is exposed
- [ ] Sign In page renders cleanly with strong workspace-code guidance
- [ ] Public navigation works on desktop and mobile widths

## Authentication and First-Run

- [ ] Sign-in fields, focus states, and loading states feel consistent
- [ ] Auth error states are calm and readable
- [ ] First-run / empty workspace dashboard does not feel broken
- [ ] Workspace language uses "company workspace" and "workspace code" where appropriate

## Authenticated Shell

- [ ] Sidebar hierarchy is readable on desktop
- [ ] Active route styling is obvious
- [ ] Topbar identity, runtime posture, and refresh controls fit without clipping
- [ ] Mobile/tablet shell collapses without overlapping content
- [ ] Keyboard focus remains visible across shell controls

## Dashboard

- [ ] Dashboard header, signal cards, lanes, and activity sections align cleanly
- [ ] Empty state onboarding checklist looks intentional
- [ ] Degraded/unavailable notices read clearly

## Core Operations Pages

- [ ] Orders page queue and selected detail layout remain readable
- [ ] Inventory spotlight and matrix remain readable on tablet widths
- [ ] Catalog forms and import results do not overflow
- [ ] Alerts severity tags and selected response state remain clear
- [ ] Recommendations lanes maintain hierarchy on smaller screens
- [ ] Replay Queue actions remain readable and safe-looking
- [ ] Integrations health/detail cards remain aligned
- [ ] Scenario History and Approvals action consoles remain readable and actionable

## Admin and Support Pages

- [ ] Users page clearly separates operators, accounts, and permission posture
- [ ] Profile page clearly shows workspace identity and security posture
- [ ] Company Settings forms stay readable and aligned on tablet/mobile
- [ ] Runtime diagnostics remain readable to non-developers
- [ ] Platform Admin, Workspace Rollout, System Config, and Releases feel productized, not internal-only

## Empty, Loading, and Error States

- [ ] No raw technical errors are shown without explanation
- [ ] Empty states explain what the page is for
- [ ] Loading states do not cause layout jumping or broken spacing
- [ ] Disabled buttons are visually distinct but still readable

## Accessibility

- [ ] Focus outlines are visible on buttons, links, inputs, and sortable table headers
- [ ] Form fields have visible labels
- [ ] Table sorting controls remain keyboard-focusable
- [ ] Color contrast is acceptable for badges, muted text, and status pills

## Proof Selector Safety

- [ ] `Replay Into Live Flow` label remains unchanged
- [ ] `Scenario action console` label remains unchanged
- [ ] `Approval action console` label remains unchanged
- [ ] `Live operational command center` label remains unchanged
- [ ] `Access your operational workspace.` label remains unchanged
- [ ] `Failed inbound recovery` label remains unchanged
- [ ] `Live order operations` label remains unchanged
- [ ] `Operational warning center` label remains unchanged

## Manual Browser Sweep

- [ ] Desktop width checked
- [ ] Tablet width checked
- [ ] Mobile width checked
- [ ] No major horizontal overflow on key routes
- [ ] No clipped primary actions on key routes

## Known Honest Boundaries

- [ ] Demo copy does not imply live backend data when backend is unavailable
- [ ] Controlled provisioning is described as an internal/protected operation, not public signup
- [ ] Demo preview mode remains clearly documented as planned or explicitly labeled if implemented later
