export const publicPages = [
  {
    key: 'landing',
    path: '/',
    audience: 'public',
    label: 'Home',
    title: 'Run operations from one live command center.',
    description: 'SynapseCore connects orders, inventory, integrations, replay recovery, approvals, alerts, and live operational signals into one tenant-based control platform.',
    focus: ['Live visibility', 'Prediction', 'Controlled action'],
  },
  {
    key: 'product',
    path: '/product',
    audience: 'public',
    label: 'Product',
    title: 'One operating system across orders, stock, fulfillment, recovery, and control.',
    description: 'See how SynapseCore turns live business activity into an enterprise command surface for decisions, operational recovery, and external handoff.',
    focus: ['Operational awareness', 'Action guidance', 'Trust layer'],
  },
  {
    key: 'create-workspace',
    path: '/create-workspace',
    audience: 'public',
    label: 'Create Workspace',
    title: 'Prepare a company workspace for pilot onboarding.',
    description: 'Capture company identity, proposed first-admin details, and rollout context for Platform Owner provisioning before the operational team signs in.',
    focus: ['Company setup', 'Workspace identity', 'Guided rollout'],
  },
  {
    key: 'sign-in',
    path: '/sign-in',
    audience: 'public',
    label: 'Sign In',
    title: 'Access your operational workspace.',
    description: 'Sign in to the right company workspace and move from visibility to action without leaving the control center.',
    focus: ['Workspace access', 'Protected actions', 'Operator identity'],
  },
  {
    key: 'contact',
    path: '/contact',
    audience: 'public',
    label: 'Start Pilot',
    title: 'Prepare a company workspace and start a serious operational pilot.',
    description: 'Capture the business pressure, company context, and rollout needs required to stand up a controlled SynapseCore workspace.',
    focus: ['Business fit', 'Operational challenge', 'Deployment readiness'],
  },
]

export const appPages = [
  {
    key: 'dashboard',
    path: '/dashboard',
    audience: 'app',
    group: 'core',
    label: 'Dashboard',
    title: 'Live operational command center',
    description: 'See what is happening now, what is at risk, and what the business needs to act on next.',
    focus: ['Act now', 'Live state', 'Trust layer'],
  },
  {
    key: 'alerts',
    path: '/alerts',
    audience: 'app',
    group: 'core',
    label: 'Alerts',
    title: 'Operational warning center',
    description: 'Review what is wrong, where it is happening, why it matters, and what should happen next.',
    focus: ['Severity', 'Impact', 'Action'],
  },
  {
    key: 'recommendations',
    path: '/recommendations',
    audience: 'app',
    group: 'core',
    label: 'Recommendations',
    title: 'Action queue for the operating team',
    description: 'Move from understanding to action with ranked operational guidance tied to live state.',
    focus: ['Urgent now', 'Important soon', 'Operational guidance'],
  },
  {
    key: 'orders',
    path: '/orders',
    audience: 'app',
    group: 'core',
    label: 'Orders',
    title: 'Live order operations',
    description: 'Track live order flow, warehouse assignment, and the order stream driving stock and fulfillment pressure.',
    focus: ['Order flow', 'Warehouse assignment', 'SLA pressure'],
  },
  {
    key: 'inventory',
    path: '/inventory',
    audience: 'app',
    group: 'core',
    label: 'Inventory',
    title: 'Inventory intelligence',
    description: 'Use the inventory brain page to understand thresholds, velocity, stockout windows, and recommended actions.',
    focus: ['Stock posture', 'Risk level', 'Depletion forecast'],
  },
  {
    key: 'catalog',
    path: '/catalog',
    audience: 'app',
    group: 'core',
    label: 'Catalog',
    title: 'Tenant product catalog',
    description: 'Create, update, and import tenant-owned product SKUs so a company can onboard without manual database work.',
    focus: ['Product creation', 'CSV import', 'Tenant ownership'],
  },
  {
    key: 'locations',
    path: '/locations',
    audience: 'app',
    group: 'core',
    label: 'Locations',
    title: 'Warehouse and site health',
    description: 'Understand warehouse scope, derived pressure, and local evidence without confusing it with runtime health.',
    focus: ['Location scope', 'Pressure by site', 'Evidence'],
  },
  {
    key: 'fulfillment',
    path: '/fulfillment',
    audience: 'app',
    group: 'core',
    label: 'Fulfillment',
    title: 'Fulfillment and logistics pressure',
    description: 'Operate backlog, dispatch, delayed shipments, and lane-level logistics risk from one page.',
    focus: ['Backlog', 'Delayed shipments', 'Lane pressure'],
  },
  {
    key: 'scenarios',
    path: '/scenarios',
    audience: 'app',
    group: 'control',
    label: 'Scenarios',
    title: 'Decision lab and scenario planning',
    description: 'Model changes before they go live, compare options, and move the best plan toward approval.',
    focus: ['What-if planning', 'Compare options', 'Submit for review'],
  },
  {
    key: 'scenario-history',
    path: '/scenario-history',
    audience: 'app',
    group: 'control',
    label: 'Scenario History',
    title: 'Scenario history and compare',
    description: 'Track previous scenarios, reload them into the planner, and compare them against the live operating state.',
    focus: ['Saved plans', 'Revision flow', 'Compare history'],
  },
  {
    key: 'approvals',
    path: '/approvals',
    audience: 'app',
    group: 'control',
    label: 'Approvals',
    title: 'Approvals center',
    description: 'See what is waiting on review, what is approved, what is rejected, and which actions are overdue.',
    focus: ['Pending review', 'Final approval', 'Approval path'],
  },
  {
    key: 'escalations',
    path: '/escalations',
    audience: 'app',
    group: 'control',
    label: 'Escalations',
    title: 'Operational escalation inbox',
    description: 'Surface urgent approval bottlenecks, repeated failures, unresolved critical items, and escalation ownership.',
    focus: ['Urgent items', 'SLA escalation', 'Operational inbox'],
  },
  {
    key: 'integrations',
    path: '/integrations',
    audience: 'app',
    group: 'systems',
    label: 'Integrations',
    title: 'Connector management and telemetry',
    description: 'Operate connected systems, inspect health, and understand recent import and sync behavior.',
    focus: ['Connector health', 'Import history', 'Support ownership'],
  },
  {
    key: 'replay',
    path: '/replay-queue',
    audience: 'app',
    group: 'systems',
    label: 'Replay Queue',
    title: 'Failed inbound recovery',
    description: 'Inspect failed inbound work, understand why it broke, and replay it safely into the live flow.',
    focus: ['Failed events', 'Recovery controls', 'Replay history'],
  },
  {
    key: 'runtime',
    path: '/runtime',
    audience: 'app',
    group: 'systems',
    label: 'Runtime',
    title: 'Runtime, incidents, and observability',
    description: 'Use the trust layer to monitor service health, incidents, queue pressure, and deployment fingerprints.',
    focus: ['Runtime state', 'Incidents', 'Metrics'],
  },
  {
    key: 'audit',
    path: '/audit-events',
    audience: 'app',
    group: 'systems',
    label: 'Audit & Events',
    title: 'Audit trail and business events',
    description: 'Trace what happened, who acted, what changed, and how the live business state evolved.',
    focus: ['Business timeline', 'Audit trail', 'Recoverability'],
  },
  {
    key: 'users',
    path: '/users',
    audience: 'app',
    group: 'admin',
    label: 'Users',
    title: 'Users and access control',
    description: 'Manage operators, roles, warehouse scopes, passwords, and the tenant access lifecycle.',
    focus: ['Users', 'Roles', 'Warehouse lanes'],
  },
  {
    key: 'settings',
    path: '/company-settings',
    audience: 'app',
    group: 'admin',
    label: 'Company Settings',
    title: 'Tenant and workspace settings',
    description: 'Configure workspace metadata, security policies, warehouse details, and connector support ownership.',
    focus: ['Workspace profile', 'Security policy', 'Connector ownership'],
  },
  {
    key: 'profile',
    path: '/profile',
    audience: 'app',
    group: 'admin',
    label: 'Profile',
    title: 'Personal profile and session controls',
    description: 'Review your current identity, password posture, session expiry, and personal account hygiene.',
    focus: ['Current session', 'Password rotation', 'Personal security'],
  },
]

export const platformPages = [
  {
    key: 'platform-sign-in',
    path: '/platform-sign-in',
    audience: 'platform',
    label: 'Platform Owner Sign In',
    title: 'Access the SynapseCore control plane.',
    description: 'Authenticate with the dedicated platform-owner account. Customer workspace credentials cannot open this surface.',
    focus: ['Platform authority', 'Metadata only', 'Separate session'],
  },
  {
    key: 'platform',
    path: '/platform-admin',
    audience: 'platform',
    label: 'Platform Overview',
    title: 'Platform overview and cross-tenant trust',
    description: 'Review metadata-only platform posture, tenant support signals, runtime health, and release trust.',
  },
  {
    key: 'tenants',
    path: '/tenant-management',
    audience: 'platform',
    label: 'Tenant Directory',
    title: 'Tenant onboarding and workspace rollout',
    description: 'Provision isolated workspaces and review portfolio-level support metadata.',
  },
  {
    key: 'system-config',
    path: '/system-config',
    audience: 'platform',
    label: 'Platform Runtime',
    title: 'System configuration and operational defaults',
    description: 'Inspect display-safe runtime, dispatch, origin, session, and realtime posture.',
  },
  {
    key: 'platform-activity',
    path: '/platform-activity',
    audience: 'platform',
    label: 'Platform Activity',
    title: 'Platform activity and evidence',
    description: 'Review metadata-only activity signals across the platform without opening tenant payloads.',
  },
  {
    key: 'releases',
    path: '/releases',
    audience: 'platform',
    label: 'Release Trust',
    title: 'Release, deployment, and environment',
    description: 'Compare deployed build identity with current runtime evidence before accepting a release.',
  },
]

export const allPages = [...publicPages, ...appPages, ...platformPages]
export const pageLookup = Object.fromEntries(allPages.map((page) => [page.key, page]))

export const routeAliases = {
  '/overview': '/dashboard',
  '/risk': '/alerts',
  '/operations': '/orders',
  '/planning': '/scenarios',
  '/workspace': '/users',
}

export const navGroups = [
  { label: 'Overview', keys: ['dashboard', 'alerts', 'recommendations'] },
  { label: 'Operations', keys: ['orders', 'inventory', 'catalog', 'locations', 'fulfillment'] },
  { label: 'Control', keys: ['scenarios', 'scenario-history', 'approvals', 'escalations'] },
  { label: 'Systems', keys: ['integrations', 'replay', 'runtime', 'audit'] },
  { label: 'Settings', keys: ['users', 'settings', 'profile'] },
]

const pageRolePolicy = {
  approvals: ['REVIEW_OWNER', 'FINAL_APPROVER'],
  escalations: ['ESCALATION_OWNER'],
  integrations: ['INTEGRATION_ADMIN', 'INTEGRATION_OPERATOR'],
  replay: ['INTEGRATION_ADMIN', 'INTEGRATION_OPERATOR'],
  users: ['TENANT_ADMIN'],
  settings: ['TENANT_ADMIN'],
}

export const canAccessWorkspacePage = (pageKey, roles = []) => {
  const requiredRoles = pageRolePolicy[pageKey]
  return !requiredRoles || requiredRoles.some((role) => roles.includes(role))
}

export const buildRoleAwareNavGroups = (roles = []) => navGroups
  .map((group) => ({
    ...group,
    keys: group.keys.filter((pageKey) => canAccessWorkspacePage(pageKey, roles)),
  }))
  .filter((group) => group.keys.length)

export const pageSectionMap = {
  dashboard: [
    { label: 'Act now', targetId: 'dashboard-act-now' },
    { label: 'Live state', targetId: 'dashboard-live-state' },
    { label: 'Trust layer', targetId: 'workspace-trust-rail' },
  ],
  alerts: [
    { label: 'Severity', targetId: 'alerts-feed' },
    { label: 'Impact', targetId: 'alerts-response' },
    { label: 'Action', targetId: 'workspace-page-focus' },
  ],
  recommendations: [
    { label: 'Urgent now', targetId: 'recommendations-lanes' },
    { label: 'Important soon', targetId: 'recommendations-focus' },
    { label: 'Operational guidance', targetId: 'workspace-page-focus' },
  ],
  orders: [
    { label: 'Order flow', targetId: 'orders-stream' },
    { label: 'Warehouse assignment', targetId: 'orders-focus' },
    { label: 'SLA pressure', targetId: 'workspace-page-focus' },
  ],
  inventory: [
    { label: 'Stock posture', targetId: 'inventory-spotlight' },
    { label: 'Risk level', targetId: 'inventory-focus' },
    { label: 'Depletion forecast', targetId: 'workspace-page-focus' },
  ],
  runtime: [
    { label: 'Runtime state', targetId: 'runtime-health' },
    { label: 'Incidents', targetId: 'runtime-incident-lane' },
    { label: 'Metrics', targetId: 'workspace-page-focus' },
  ],
  audit: [
    { label: 'Business timeline', targetId: 'audit-events' },
    { label: 'Audit trail', targetId: 'audit-logs' },
    { label: 'Recoverability', targetId: 'workspace-page-focus' },
  ],
  catalog: [
    { label: 'Catalog workflow', targetId: 'catalog-workflow' },
    { label: 'Import outcomes', targetId: 'workspace-page-focus' },
    { label: 'Product evidence', targetId: 'workspace-page-focus' },
  ],
  locations: [
    { label: 'Location scope', targetId: 'locations-state' },
    { label: 'Pressure by site', targetId: 'locations-state' },
    { label: 'Site action queue', targetId: 'workspace-page-focus' },
  ],
  fulfillment: [
    { label: 'Fulfillment state', targetId: 'fulfillment-state' },
    { label: 'Lane pressure', targetId: 'workspace-page-focus' },
    { label: 'Delivery support', targetId: 'workspace-page-focus' },
  ],
  'scenario-history': [
    { label: 'Decision memory', targetId: 'scenario-history-evidence' },
    { label: 'Scenario records', targetId: 'scenario-history-evidence' },
    { label: 'Action console', targetId: 'workspace-page-focus' },
  ],
  escalations: [
    { label: 'Escalation inbox', targetId: 'escalation-inbox' },
    { label: 'Ownership focus', targetId: 'workspace-page-focus' },
    { label: 'Governance boundary', targetId: 'workspace-page-focus' },
  ],
  settings: [
    { label: 'Workspace profile', targetId: 'settings-profile' },
    { label: 'Security policy', targetId: 'settings-security' },
    { label: 'Connector ownership', targetId: 'settings-connectors' },
  ],
  platform: [
    { label: 'Platform health', targetId: 'platform-portfolio' },
    { label: 'Tenant posture', targetId: 'platform-focus' },
    { label: 'Release state', targetId: 'workspace-page-focus' },
  ],
  releases: [
    { label: 'Build fingerprint', targetId: 'releases-builds' },
    { label: 'Deployment health', targetId: 'releases-checklist' },
    { label: 'Environment posture', targetId: 'workspace-page-focus' },
  ],
  'platform-activity': [
    { label: 'Activity feed', targetId: 'platform-activity-feed' },
    { label: 'Evidence boundary', targetId: 'platform-activity-boundary' },
    { label: 'Next checks', targetId: 'platform-activity-next-checks' },
  ],
}

export const resolvePageFromPath = (pathname = globalThis.location?.pathname || '/') => {
  const normalizedPath = pathname.replace(/\/+$/, '') || '/'
  const routedPath = routeAliases[normalizedPath] || normalizedPath
  return allPages.find((page) => page.path === routedPath)?.key || 'landing'
}

export const buildPagePath = (pageKey) => pageLookup[pageKey]?.path || '/'
