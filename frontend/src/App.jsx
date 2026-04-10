import { startTransition, useDeferredValue, useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const runtimeConfig = globalThis.__SYNAPSE_RUNTIME_CONFIG__ || {}
const apiUrl = runtimeConfig.apiUrl || import.meta.env.VITE_API_URL || 'http://localhost:8080'
const wsUrl = runtimeConfig.wsUrl || import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'
const frontendBuildVersion = runtimeConfig.appBuildVersion || import.meta.env.VITE_APP_BUILD_VERSION || 'local-dev'
const frontendBuildCommit = runtimeConfig.appBuildCommit || import.meta.env.VITE_APP_BUILD_COMMIT || 'local-dev'
const frontendBuildTime = runtimeConfig.appBuildTime || import.meta.env.VITE_APP_BUILD_TIME || 'untracked'
const emptySnapshot = {
  summary: null,
  alerts: { activeAlerts: [], recentAlerts: [] },
  recommendations: [],
  inventory: [],
  fulfillment: { backlogCount: 0, overdueDispatchCount: 0, delayedShipmentCount: 0, atRiskCount: 0, activeFulfillments: [], generatedAt: null },
  recentOrders: [],
  recentEvents: [],
  auditLogs: [],
  systemIncidents: [],
  integrationConnectors: [],
  integrationImportRuns: [],
  integrationReplayQueue: [],
  scenarioNotifications: [],
  slaEscalations: [],
  recentScenarios: [],
  simulation: { active: false, updatedAt: null },
  generatedAt: null,
}
const emptyRequestState = { loading: false, error: '', result: null }
const defaultScenarioHistoryFilters = { type: 'ALL', approvalStatus: 'ALL', approvalPolicy: 'ALL', approvalStage: 'ALL', warehouseCode: 'ALL', requestedBy: '', reviewOwner: '', finalApprovalOwner: '', minimumReviewPriority: 'ALL', overdueOnly: false, slaEscalatedOnly: false }
const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })
const defaultSignInTenantCode = ''
const defaultSignInUsername = ''
const defaultScenarioRequester = 'Operations Planner'
const defaultScenarioReviewOwner = 'Operations Lead'
const defaultTenantOnboardingForm = { tenantCode: '', tenantName: '', description: '', adminFullName: '', adminUsername: '', adminPassword: '', primaryLocation: '', secondaryLocation: '' }
const createDefaultWorkspaceSettingsForm = () => ({ tenantName: '', description: '' })
const createDefaultWorkspaceSecurityForm = () => ({ passwordRotationDays: '90', sessionTimeoutMinutes: '480', invalidateOtherSessions: false })
const createDefaultAccessOperatorForm = () => ({ id: null, actorName: '', displayName: '', description: '', rolesText: 'REVIEW_OWNER', warehouseScopesText: '', active: true })
const createDefaultAccessUserForm = () => ({ id: null, username: '', fullName: '', password: '', operatorActorName: '', active: true })
const createDefaultPasswordChangeForm = () => ({ currentPassword: '', newPassword: '', confirmPassword: '' })
const buildWorkspaceWarehouseDrafts = (workspace) => Object.fromEntries((workspace?.warehouses || []).map((warehouse) => [warehouse.id, { name: warehouse.name, location: warehouse.location }]))
const buildWorkspaceConnectorDrafts = (workspace) => Object.fromEntries((workspace?.connectors || []).map((connector) => [connector.id, {
  supportOwnerActorName: connector.supportOwnerActorName || '',
  syncMode: connector.syncMode || 'REALTIME_PUSH',
  syncIntervalMinutes: connector.syncIntervalMinutes == null ? '' : String(connector.syncIntervalMinutes),
  validationPolicy: connector.validationPolicy || 'STANDARD',
  transformationPolicy: connector.transformationPolicy || 'NONE',
  allowDefaultWarehouseFallback: Boolean(connector.allowDefaultWarehouseFallback),
  notes: connector.notes || '',
}]))
const buildAccessOperatorForm = (operator) => ({
  ...createDefaultAccessOperatorForm(),
  id: operator.id,
  actorName: operator.actorName,
  displayName: operator.displayName,
  description: operator.description || '',
  rolesText: operator.roles.join(','),
  warehouseScopesText: operator.warehouseScopes.join(','),
  active: operator.active,
})
const buildAccessUserForm = (user) => ({
  ...createDefaultAccessUserForm(),
  id: user.id,
  username: user.username,
  fullName: user.fullName,
  operatorActorName: user.operatorActorName,
  active: user.active,
})
const createScenarioLine = (productSku = '') => ({ id: `${Date.now()}-${Math.random().toString(16).slice(2)}`, productSku, quantity: '1', unitPrice: '95.00' })
const createScenarioForm = (productSku = '') => ({ warehouseCode: '', items: [createScenarioLine(productSku)] })
const buildWarehouseOptions = (inventory) => [...new Map(inventory.map((item) => [item.warehouseCode, { code: item.warehouseCode, name: item.warehouseName }])).values()]
const buildProductOptions = (inventory, warehouseCode) => [...new Map(inventory.filter((item) => item.warehouseCode === warehouseCode).map((item) => [item.productSku, { sku: item.productSku, name: item.productName, quantityAvailable: item.quantityAvailable, reorderThreshold: item.reorderThreshold }])).values()]
const formatRelativeHours = (hours) => {
  if (hours == null) return 'Monitoring'
  if (hours < 0) {
    const overdueHours = Math.abs(hours)
    return overdueHours < 1 ? `${Math.max(overdueHours * 60, 1).toFixed(0)} min overdue` : `${overdueHours.toFixed(1)} hrs overdue`
  }
  return hours < 1 ? `${Math.max(hours * 60, 1).toFixed(0)} min` : `${hours.toFixed(1)} hrs`
}
const formatTimestamp = (value) => value ? new Date(value).toLocaleString() : 'Monitoring'
const formatMetricValue = (value) => Number.isFinite(value) ? Math.round(value) : 0
const formatBuildValue = (value) => value || 'untracked'
const formatCodeLabel = (value) => !value ? 'Unknown' : value.toLowerCase().split('_').map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(' ')
const parseCsvValues = (value) => value.split(',').map((part) => part.trim()).filter(Boolean)
const buildTenantTopicPrefix = (tenantCode) => tenantCode ? `/topic/tenant/${tenantCode.trim().toUpperCase()}` : ''
const getImportStatusClassName = (status) => status === 'SUCCESS' ? 'status-success' : status === 'PARTIAL_SUCCESS' ? 'status-partial' : 'status-failure'
const getReplayStatusClassName = (status) => status === 'PENDING' ? 'status-partial' : status === 'REPLAY_FAILED' ? 'status-failure' : 'status-success'
const getRuntimeStatusClassName = (status) => ['UP', 'CORRECT', 'ACCEPTING_TRAFFIC'].includes(status) ? 'status-success' : status === 'UNKNOWN' ? 'status-partial' : 'status-failure'
const getIncidentStatusClassName = (severity) => severity === 'CRITICAL' || severity === 'HIGH' ? 'status-failure' : 'status-partial'
const getFulfillmentStatusClassName = (status) => status === 'DELIVERED' || status === 'DISPATCHED' ? 'status-success' : status === 'DELAYED' || status === 'EXCEPTION' ? 'status-failure' : 'status-partial'
const scenarioActorRoles = ['REVIEW_OWNER', 'FINAL_APPROVER', 'ESCALATION_OWNER']
const integrationActorRoles = ['INTEGRATION_ADMIN', 'INTEGRATION_OPERATOR']
const integrationSyncModes = ['REALTIME_PUSH', 'BATCH_FILE_DROP', 'SCHEDULED_PULL']
const integrationValidationPolicies = ['STANDARD', 'STRICT', 'RELAXED']
const integrationTransformationPolicies = ['NONE', 'NORMALIZE_CODES']
const getScenarioApprovalRole = (scenario) => scenario.approvalPolicy === 'ESCALATED' && scenario.approvalStage === 'PENDING_FINAL_APPROVAL' ? 'FINAL_APPROVER' : 'REVIEW_OWNER'
const getScenarioRejectionRole = (scenario) => scenario.approvalStage === 'PENDING_FINAL_APPROVAL' ? 'FINAL_APPROVER' : 'REVIEW_OWNER'
const summarizeImpact = (impact) => ({ lowStockItems: impact.projectedInventory.filter((item) => item.lowStock).length, criticalItems: impact.projectedInventory.filter((item) => item.riskLevel === 'critical').length, highRiskItems: impact.projectedInventory.filter((item) => item.riskLevel === 'high').length, alertCount: impact.projectedAlerts.length, recommendationCount: impact.projectedRecommendations.length })
const buildRevisionTitle = (title, revisionNumber) => `${title} Rev ${revisionNumber}`
const hasActiveScenarioHistoryFilters = (filters) => filters.type !== 'ALL' || filters.approvalStatus !== 'ALL' || filters.approvalPolicy !== 'ALL' || filters.approvalStage !== 'ALL' || filters.warehouseCode !== 'ALL' || Boolean(filters.requestedBy.trim()) || Boolean(filters.reviewOwner.trim()) || Boolean(filters.finalApprovalOwner.trim()) || filters.minimumReviewPriority !== 'ALL' || filters.overdueOnly || filters.slaEscalatedOnly
const resolvePreferredOperatorName = (operators, preferredName) => operators.find((operator) => operator.actorName === preferredName)?.actorName || operators[0]?.actorName || ''
const hasWarehouseScope = (warehouseScopes, warehouseCode) => !warehouseCode || !(warehouseScopes?.length) || warehouseScopes.some((scope) => scope === warehouseCode.toUpperCase())
const dedupeById = (items) => Array.from(new Map(items.filter(Boolean).map((item) => [item.id, item])).values())
const buildAccessOperatorsPath = (tenantCode) => tenantCode ? `/api/access/operators?tenantCode=${encodeURIComponent(tenantCode)}` : '/api/access/operators'
const buildScenarioHistoryPath = (filters) => {
  const params = new URLSearchParams()
  if (filters.type !== 'ALL') params.set('type', filters.type)
  if (filters.approvalStatus !== 'ALL') params.set('approvalStatus', filters.approvalStatus)
  if (filters.approvalPolicy !== 'ALL') params.set('approvalPolicy', filters.approvalPolicy)
  if (filters.approvalStage !== 'ALL') params.set('approvalStage', filters.approvalStage)
  if (filters.warehouseCode !== 'ALL') params.set('warehouseCode', filters.warehouseCode)
  if (filters.requestedBy.trim()) params.set('requestedBy', filters.requestedBy.trim())
  if (filters.reviewOwner.trim()) params.set('reviewOwner', filters.reviewOwner.trim())
  if (filters.finalApprovalOwner.trim()) params.set('finalApprovalOwner', filters.finalApprovalOwner.trim())
  if (filters.minimumReviewPriority !== 'ALL') params.set('minimumReviewPriority', filters.minimumReviewPriority)
  if (filters.overdueOnly) params.set('overdueOnly', 'true')
  if (filters.slaEscalatedOnly) params.set('slaEscalatedOnly', 'true')
  return params.size ? `/api/scenarios/history?${params.toString()}` : '/api/scenarios/history'
}
const publicPages = [
  {
    key: 'landing',
    path: '/',
    audience: 'public',
    label: 'Home',
    title: 'Turn operations into decisions.',
    description: 'A real-time operational intelligence platform for inventory, fulfillment, logistics, approvals, and control.',
    focus: ['Live visibility', 'Prediction', 'Controlled action'],
  },
  {
    key: 'product',
    path: '/product',
    audience: 'public',
    label: 'Product',
    title: 'One operating picture across orders, stock, logistics, and control.',
    description: 'See how SynapseCore turns live business activity into a premium command center for decisions and execution.',
    focus: ['Operational awareness', 'Action guidance', 'Trust layer'],
  },
  {
    key: 'sign-in',
    path: '/sign-in',
    audience: 'public',
    label: 'Sign In',
    title: 'Access your operational workspace.',
    description: 'Sign in to the right company workspace and move from visibility to action without leaving the control center.',
    focus: ['Tenant access', 'Protected actions', 'Operator identity'],
  },
  {
    key: 'contact',
    path: '/contact',
    audience: 'public',
    label: 'Contact',
    title: 'Tell us what operational pressure you need to solve.',
    description: 'Capture the business challenge, company context, and scale signals needed to shape a SynapseCore rollout.',
    focus: ['Business fit', 'Operational challenge', 'Deployment readiness'],
  },
]

const appPages = [
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
    key: 'locations',
    path: '/locations',
    audience: 'app',
    group: 'core',
    label: 'Locations',
    title: 'Warehouse and site health',
    description: 'Monitor operational health across locations, from stock posture to backlog and local issues.',
    focus: ['Location health', 'Pressure by site', 'Drill-down'],
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
  {
    key: 'platform',
    path: '/platform-admin',
    audience: 'app',
    group: 'admin',
    label: 'Platform Admin',
    title: 'Platform overview and cross-tenant trust',
    description: 'Use the platform view to understand global health, cross-tenant posture, and release readiness.',
    focus: ['Platform health', 'Tenant posture', 'Release state'],
  },
  {
    key: 'tenants',
    path: '/tenant-management',
    audience: 'app',
    group: 'admin',
    label: 'Tenant Management',
    title: 'Tenant onboarding and workspace rollout',
    description: 'Bootstrap new workspaces, inspect tenant setup, and move new companies cleanly into live use.',
    focus: ['Tenant creation', 'Workspace setup', 'Rollout readiness'],
  },
  {
    key: 'system-config',
    path: '/system-config',
    audience: 'app',
    group: 'admin',
    label: 'System Config',
    title: 'System configuration and operational defaults',
    description: 'Review runtime posture, connector defaults, dispatch intervals, and the system-wide control envelope.',
    focus: ['System defaults', 'Dispatch posture', 'Operational rules'],
  },
  {
    key: 'releases',
    path: '/releases',
    audience: 'app',
    group: 'admin',
    label: 'Releases',
    title: 'Release, deployment, and environment',
    description: 'Track build fingerprints, runtime versions, deployment health, and environment readiness.',
    focus: ['Build fingerprint', 'Deployment health', 'Environment posture'],
  },
]

const allPages = [...publicPages, ...appPages]
const pageLookup = Object.fromEntries(allPages.map((page) => [page.key, page]))
const routeAliases = {
  '/overview': '/dashboard',
  '/risk': '/alerts',
  '/operations': '/orders',
  '/planning': '/scenarios',
  '/workspace': '/users',
}
const navGroups = [
  { label: 'Core', keys: ['dashboard', 'alerts', 'recommendations', 'orders', 'inventory', 'locations', 'fulfillment'] },
  { label: 'Control', keys: ['scenarios', 'scenario-history', 'approvals', 'escalations'] },
  { label: 'Systems', keys: ['integrations', 'replay', 'runtime', 'audit'] },
  { label: 'Admin', keys: ['users', 'settings', 'profile', 'platform', 'tenants', 'system-config', 'releases'] },
]
const pageSectionMap = {
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
}
const resolvePageFromPath = (pathname = globalThis.location?.pathname || '/') => {
  const normalizedPath = pathname.replace(/\/+$/, '') || '/'
  const routedPath = routeAliases[normalizedPath] || normalizedPath
  return allPages.find((page) => page.path === routedPath)?.key || 'landing'
}
const buildPagePath = (pageKey) => pageLookup[pageKey]?.path || '/'

function SummaryCard({ label, value, accent }) {
  return <article className="summary-card"><span className="summary-label">{label}</span><strong className={`summary-value accent-${accent}`}>{value}</strong></article>
}

export default function App() {
  const [snapshot, setSnapshot] = useState(emptySnapshot)
  const [currentPage, setCurrentPage] = useState(() => resolvePageFromPath())
  const [workspaceSearch, setWorkspaceSearch] = useState('')
  const [rememberWorkspace, setRememberWorkspace] = useState(true)
  const [clockTick, setClockTick] = useState(() => Date.now())
  const [selectedAlertId, setSelectedAlertId] = useState(null)
  const [selectedRecommendationId, setSelectedRecommendationId] = useState(null)
  const [selectedOrderId, setSelectedOrderId] = useState(null)
  const [selectedInventoryId, setSelectedInventoryId] = useState(null)
  const [selectedScenarioId, setSelectedScenarioId] = useState(null)
  const [selectedRuntimeIncidentKey, setSelectedRuntimeIncidentKey] = useState(null)
  const [selectedTraceEntryKey, setSelectedTraceEntryKey] = useState(null)
  const [selectedAccessSubjectKey, setSelectedAccessSubjectKey] = useState(null)
  const [selectedTenantPortfolioCode, setSelectedTenantPortfolioCode] = useState(null)
  const [selectedWorkspaceWarehouseId, setSelectedWorkspaceWarehouseId] = useState(null)
  const [selectedWorkspaceConnectorId, setSelectedWorkspaceConnectorId] = useState(null)
  const [connectionState, setConnectionState] = useState('connecting')
  const [pageState, setPageState] = useState({ loading: true, error: '' })
  const [actionState, setActionState] = useState({ loading: false, error: '' })
  const [systemRuntimeState, setSystemRuntimeState] = useState({ loading: true, error: '', runtime: null })
  const [authSessionState, setAuthSessionState] = useState({ loading: true, error: '', session: null, tenantCode: defaultSignInTenantCode, username: defaultSignInUsername, password: '', action: '' })
  const [tenantDirectoryState, setTenantDirectoryState] = useState({ loading: true, error: '', items: [] })
  const [tenantOnboardingForm, setTenantOnboardingForm] = useState(defaultTenantOnboardingForm)
  const [tenantOnboardingState, setTenantOnboardingState] = useState({ loading: false, error: '', success: '', result: null })
  const [accessAdminState, setAccessAdminState] = useState({ loading: false, error: '', success: '', workspace: null, operators: [], users: [] })
  const [workspaceSettingsForm, setWorkspaceSettingsForm] = useState(createDefaultWorkspaceSettingsForm)
  const [workspaceSecurityForm, setWorkspaceSecurityForm] = useState(createDefaultWorkspaceSecurityForm)
  const [workspaceWarehouseDrafts, setWorkspaceWarehouseDrafts] = useState({})
  const [workspaceConnectorDrafts, setWorkspaceConnectorDrafts] = useState({})
  const [accessOperatorForm, setAccessOperatorForm] = useState(createDefaultAccessOperatorForm)
  const [accessUserForm, setAccessUserForm] = useState(createDefaultAccessUserForm)
  const [passwordChangeState, setPasswordChangeState] = useState({ loading: false, error: '', success: '', form: createDefaultPasswordChangeForm() })
  const [operatorDirectoryState, setOperatorDirectoryState] = useState({ loading: true, error: '', items: [] })
  const [integrationConnectorState, setIntegrationConnectorState] = useState({ loadingKey: null, error: '', success: '' })
  const [integrationReplayState, setIntegrationReplayState] = useState({ loadingId: null, error: '', success: '' })
  const [integrationActorRole, setIntegrationActorRole] = useState('INTEGRATION_ADMIN')
  const [scenarioForm, setScenarioForm] = useState(createScenarioForm())
  const [comparisonForm, setComparisonForm] = useState(createScenarioForm())
  const [scenarioPlanName, setScenarioPlanName] = useState('')
  const [scenarioRequestedBy, setScenarioRequestedBy] = useState(defaultScenarioRequester)
  const [scenarioReviewOwner, setScenarioReviewOwner] = useState(defaultScenarioReviewOwner)
  const [scenarioActorRole, setScenarioActorRole] = useState('REVIEW_OWNER')
  const [scenarioReviewNote, setScenarioReviewNote] = useState('')
  const [scenarioRevisionSource, setScenarioRevisionSource] = useState(null)
  const [scenarioState, setScenarioState] = useState(emptyRequestState)
  const [comparisonState, setComparisonState] = useState(emptyRequestState)
  const [scenarioExecutionState, setScenarioExecutionState] = useState({ loadingId: null, error: '', success: '' })
  const [scenarioLoadState, setScenarioLoadState] = useState({ loadingId: null, error: '', success: '' })
  const [scenarioSaveState, setScenarioSaveState] = useState({ loading: false, error: '', success: '' })
  const [scenarioApprovalState, setScenarioApprovalState] = useState({ loadingId: null, error: '', success: '' })
  const [scenarioRejectionState, setScenarioRejectionState] = useState({ loadingId: null, error: '', success: '' })
  const [scenarioEscalationAckState, setScenarioEscalationAckState] = useState({ loadingId: null, error: '', success: '' })
  const [scenarioHistoryFilters, setScenarioHistoryFilters] = useState(defaultScenarioHistoryFilters)
  const [scenarioHistoryState, setScenarioHistoryState] = useState({ loading: true, error: '', items: [] })
  const searchInputRef = useRef(null)

  const mergeSnapshot = (partial) => setSnapshot((current) => ({ ...current, ...partial, generatedAt: new Date().toISOString() }))
  const activeTenantCode = authSessionState.session?.tenantCode || ''
  const fetchApi = (path, init = {}) => {
    const headers = new Headers(init.headers || {})
    if (authSessionState.session?.tenantCode) {
      headers.set('X-Synapse-Tenant', authSessionState.session.tenantCode)
    }
    return fetch(`${apiUrl}${path}`, { credentials: 'include', ...init, headers })
  }
  const fetchJson = async (path) => {
    const response = await fetchApi(path)
    if (!response.ok) throw new Error(`Unable to load ${path}.`)
    return response.json()
  }
  const prefersReducedMotion = globalThis.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches ?? false
  const navigateToPage = (pageKey) => {
    const nextPath = buildPagePath(pageKey)
    if (globalThis.location?.pathname !== nextPath) {
      globalThis.history?.pushState?.({}, '', nextPath)
    }
    startTransition(() => {
      setCurrentPage(pageKey)
    })
    globalThis.scrollTo?.({ top: 0, behavior: prefersReducedMotion ? 'auto' : 'smooth' })
  }
  const jumpToPageSection = (targetId) => {
    if (!targetId) return
    const target = globalThis.document?.getElementById?.(targetId)
    target?.scrollIntoView?.({ behavior: prefersReducedMotion ? 'auto' : 'smooth', block: 'start' })
  }

  async function fetchAccessAdminData() {
    const [workspace, operators, users] = await Promise.all([
      fetchJson('/api/access/admin/workspace'),
      fetchJson('/api/access/admin/operators'),
      fetchJson('/api/access/admin/users'),
    ])
    const defaultOperatorActorName = operators.find((operator) => operator.active)?.actorName || operators[0]?.actorName || ''
    setAccessAdminState((current) => ({ ...current, loading: false, error: '', workspace, operators, users }))
    setWorkspaceSettingsForm({
      tenantName: workspace.tenantName || '',
      description: workspace.description || '',
    })
    setWorkspaceSecurityForm({
      passwordRotationDays: String(workspace.securitySettings?.passwordRotationDays || 90),
      sessionTimeoutMinutes: String(workspace.securitySettings?.sessionTimeoutMinutes || 480),
      invalidateOtherSessions: false,
    })
    setWorkspaceWarehouseDrafts(buildWorkspaceWarehouseDrafts(workspace))
    setWorkspaceConnectorDrafts(buildWorkspaceConnectorDrafts(workspace))
    setAccessUserForm((current) => ({
      ...current,
      operatorActorName: operators.some((operator) => operator.actorName === current.operatorActorName)
        ? current.operatorActorName
        : defaultOperatorActorName,
    }))
  }

  async function fetchSnapshot() {
    const nextSnapshot = await fetchJson('/api/dashboard/snapshot')
    setSnapshot({
      ...emptySnapshot,
      ...nextSnapshot,
      recentEvents: nextSnapshot.recentEvents ?? [],
      auditLogs: nextSnapshot.auditLogs ?? [],
      systemIncidents: nextSnapshot.systemIncidents ?? [],
      integrationConnectors: nextSnapshot.integrationConnectors ?? [],
      integrationImportRuns: nextSnapshot.integrationImportRuns ?? [],
      integrationReplayQueue: nextSnapshot.integrationReplayQueue ?? [],
      scenarioNotifications: nextSnapshot.scenarioNotifications ?? [],
      slaEscalations: nextSnapshot.slaEscalations ?? [],
      recentScenarios: nextSnapshot.recentScenarios ?? [],
      generatedAt: nextSnapshot.generatedAt ?? new Date().toISOString(),
    })
    setPageState({ loading: false, error: '' })
  }

  async function refreshSnapshotQuietly() {
    try {
      await fetchSnapshot()
      await refreshSystemRuntimeQuietly()
    } catch {
      // Keep planning feedback visible even if the secondary snapshot refresh misses.
    }
  }

  async function fetchSystemRuntime() {
    const runtime = await fetchJson('/api/system/runtime')
    setSystemRuntimeState({ loading: false, error: '', runtime })
  }

  async function refreshSystemRuntimeQuietly() {
    try {
      await fetchSystemRuntime()
    } catch {
      // Keep runtime feedback visible even if the secondary refresh misses.
    }
  }

  async function refreshScenarioHistoryQuietly(filters = scenarioHistoryFilters) {
    try {
      const history = await fetchJson(buildScenarioHistoryPath(filters))
      setScenarioHistoryState({ loading: false, error: '', items: history })
    } catch (error) {
      setScenarioHistoryState((current) => current.items.length
        ? { ...current, loading: false }
        : { loading: false, error: error.message, items: [] })
    }
  }

  function normalizeScenarioForm(currentForm, inventory) {
    if (!inventory.length) return currentForm
    const warehouseOptions = buildWarehouseOptions(inventory)
    const nextWarehouseCode = currentForm.warehouseCode || warehouseOptions[0]?.code || ''
    const productOptions = buildProductOptions(inventory, nextWarehouseCode)
    const fallbackSku = productOptions[0]?.sku || ''
    const currentItems = currentForm.items.length ? currentForm.items : [createScenarioLine(fallbackSku)]
    const nextItems = currentItems.map((item, index) => {
      const nextProductSku = productOptions.some((product) => product.sku === item.productSku) ? item.productSku : (index === 0 ? fallbackSku : item.productSku || fallbackSku)
      return nextProductSku === item.productSku ? item : { ...item, productSku: nextProductSku }
    })
    const itemsUnchanged = currentItems.length === nextItems.length && currentItems.every((item, index) => item.productSku === nextItems[index].productSku)
    return currentForm.warehouseCode === nextWarehouseCode && itemsUnchanged ? currentForm : { ...currentForm, warehouseCode: nextWarehouseCode, items: nextItems }
  }

  function buildScenarioContext(form) {
    const productOptions = buildProductOptions(snapshot.inventory, form.warehouseCode)
    const lines = form.items.map((item) => {
      const quantityNumber = Number.parseInt(item.quantity, 10)
      const unitPriceNumber = Number.parseFloat(item.unitPrice)
      const selectedProduct = productOptions.find((product) => product.sku === item.productSku)
      return { ...item, selectedProduct, quantityNumber, unitPriceNumber, valid: Boolean(item.productSku) && Number.isFinite(quantityNumber) && quantityNumber >= 1 && Number.isFinite(unitPriceNumber) && unitPriceNumber > 0 }
    })
    return { productOptions, lines, requestItems: lines.map((item) => ({ productSku: item.productSku, quantity: item.quantityNumber, unitPrice: item.unitPriceNumber })), inputValid: Boolean(form.warehouseCode) && lines.length > 0 && lines.every((item) => item.valid) }
  }

  useEffect(() => {
    const handlePopState = () => setCurrentPage(resolvePageFromPath())
    globalThis.addEventListener?.('popstate', handlePopState)
    return () => globalThis.removeEventListener?.('popstate', handlePopState)
  }, [])

  useEffect(() => {
    const timer = globalThis.setInterval?.(() => setClockTick(Date.now()), 30000)
    return () => globalThis.clearInterval?.(timer)
  }, [])

  useEffect(() => {
    const handleCommandKeys = (event) => {
      const target = event.target
      const isTextInput = target instanceof HTMLElement && (
        target.tagName === 'INPUT'
        || target.tagName === 'TEXTAREA'
        || target.tagName === 'SELECT'
        || target.isContentEditable
      )

      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        searchInputRef.current?.focus()
        searchInputRef.current?.select?.()
        return
      }

      if (event.key === '/' && !isTextInput) {
        event.preventDefault()
        searchInputRef.current?.focus()
        searchInputRef.current?.select?.()
        return
      }

      if (event.key === 'Escape') {
        if (workspaceSearch) {
          setWorkspaceSearch('')
        }
        if (target === searchInputRef.current) {
          searchInputRef.current?.blur()
        }
      }
    }

    globalThis.addEventListener?.('keydown', handleCommandKeys)
    return () => globalThis.removeEventListener?.('keydown', handleCommandKeys)
  }, [workspaceSearch])

  useEffect(() => {
    let active = true
    async function loadSnapshot() {
      if (!authSessionState.session?.tenantCode) {
        if (active) {
          setSnapshot(emptySnapshot)
          setPageState({ loading: false, error: '' })
        }
        return
      }
      try { await fetchSnapshot() } catch (error) { if (active) setPageState({ loading: false, error: error.message }) }
    }
    loadSnapshot()
    if (!authSessionState.session?.tenantCode) {
      setConnectionState('signed-out')
      return () => { active = false }
    }
    const topicPrefix = buildTenantTopicPrefix(activeTenantCode)
    const client = new Client({
      reconnectDelay: 5000,
      webSocketFactory: () => new SockJS(wsUrl),
      onConnect: () => {
        setConnectionState('live')
        client.subscribe(`${topicPrefix}/dashboard.summary`, (message) => mergeSnapshot({ summary: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/alerts`, (message) => mergeSnapshot({ alerts: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/recommendations`, (message) => mergeSnapshot({ recommendations: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/inventory`, (message) => mergeSnapshot({ inventory: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/fulfillment.overview`, (message) => mergeSnapshot({ fulfillment: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/orders.recent`, (message) => mergeSnapshot({ recentOrders: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/events.recent`, (message) => mergeSnapshot({ recentEvents: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/audit.recent`, (message) => mergeSnapshot({ auditLogs: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/system.incidents`, (message) => mergeSnapshot({ systemIncidents: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/integrations.connectors`, (message) => mergeSnapshot({ integrationConnectors: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/integrations.imports`, (message) => mergeSnapshot({ integrationImportRuns: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/integrations.replay`, (message) => mergeSnapshot({ integrationReplayQueue: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/scenarios.notifications`, (message) => mergeSnapshot({ scenarioNotifications: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/scenarios.escalated`, (message) => mergeSnapshot({ slaEscalations: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/simulation.status`, (message) => mergeSnapshot({ simulation: JSON.parse(message.body) }))
      },
      onStompError: () => setConnectionState('degraded'),
      onWebSocketClose: () => setConnectionState('reconnecting'),
    })
    client.activate()
    return () => { active = false; client.deactivate() }
  }, [activeTenantCode])

  useEffect(() => {
    let active = true
    async function loadTenants() {
      try {
        const tenants = await fetchJson('/api/access/tenants')
        if (active) {
          setTenantDirectoryState({ loading: false, error: '', items: tenants })
          setAuthSessionState((current) => ({
            ...current,
            tenantCode: current.session?.tenantCode || current.tenantCode || tenants[0]?.code || '',
          }))
        }
      } catch (error) {
        if (active) {
          setTenantDirectoryState({ loading: false, error: error.message, items: [] })
        }
      }
    }
    loadTenants()
    return () => { active = false }
  }, [activeTenantCode])

  useEffect(() => {
    let active = true
    async function loadOperators() {
      if (!authSessionState.session?.tenantCode) {
        if (active) {
          setOperatorDirectoryState({ loading: false, error: '', items: [] })
        }
        return
      }
      try {
        const operators = await fetchJson(buildAccessOperatorsPath(activeTenantCode))
        if (active) {
          setOperatorDirectoryState({ loading: false, error: '', items: operators })
        }
      } catch (error) {
        if (active) {
          setOperatorDirectoryState({ loading: false, error: error.message, items: [] })
        }
      }
    }
    loadOperators()
    return () => { active = false }
  }, [activeTenantCode])

  useEffect(() => {
    let active = true
    async function loadAccessAdminData() {
      if (!(authSessionState.session?.roles || []).includes('TENANT_ADMIN')) {
        if (active) {
          setAccessAdminState({ loading: false, error: '', success: '', workspace: null, operators: [], users: [] })
          setWorkspaceSettingsForm(createDefaultWorkspaceSettingsForm())
          setWorkspaceSecurityForm(createDefaultWorkspaceSecurityForm())
          setWorkspaceWarehouseDrafts({})
          setWorkspaceConnectorDrafts({})
          setAccessOperatorForm(createDefaultAccessOperatorForm())
          setAccessUserForm(createDefaultAccessUserForm())
        }
        return
      }

      if (active) {
        setAccessAdminState((current) => ({ ...current, loading: true, error: '' }))
      }
      try {
        const [workspace, operators, users] = await Promise.all([
          fetchJson('/api/access/admin/workspace'),
          fetchJson('/api/access/admin/operators'),
          fetchJson('/api/access/admin/users'),
        ])
        if (active) {
          const defaultOperatorActorName = operators.find((operator) => operator.active)?.actorName || operators[0]?.actorName || ''
          setAccessAdminState((current) => ({ ...current, loading: false, error: '', workspace, operators, users }))
          setWorkspaceSettingsForm({
            tenantName: workspace.tenantName || '',
            description: workspace.description || '',
          })
          setWorkspaceSecurityForm({
            passwordRotationDays: String(workspace.securitySettings?.passwordRotationDays || 90),
            sessionTimeoutMinutes: String(workspace.securitySettings?.sessionTimeoutMinutes || 480),
            invalidateOtherSessions: false,
          })
          setWorkspaceWarehouseDrafts(buildWorkspaceWarehouseDrafts(workspace))
          setWorkspaceConnectorDrafts(buildWorkspaceConnectorDrafts(workspace))
          setAccessUserForm((current) => ({
            ...current,
            operatorActorName: operators.some((operator) => operator.actorName === current.operatorActorName)
              ? current.operatorActorName
              : defaultOperatorActorName,
          }))
        }
      } catch (error) {
        if (active) {
          setAccessAdminState((current) => ({ ...current, loading: false, error: error.message, workspace: null, operators: [], users: [] }))
        }
      }
    }
    loadAccessAdminData()
    return () => { active = false }
  }, [authSessionState.session, activeTenantCode])

  useEffect(() => {
    let active = true
    async function loadSystemRuntime() {
      if (!authSessionState.session?.tenantCode) {
        if (active) {
          setSystemRuntimeState({ loading: false, error: '', runtime: null })
        }
        return
      }
      try {
        const runtime = await fetchJson('/api/system/runtime')
        if (active) {
          setSystemRuntimeState({ loading: false, error: '', runtime })
        }
      } catch (error) {
        if (active) {
          setSystemRuntimeState({ loading: false, error: error.message, runtime: null })
        }
      }
    }
    loadSystemRuntime()
    return () => { active = false }
  }, [authSessionState.session])

  useEffect(() => {
    let active = true
    async function loadAuthSession() {
      try {
        const session = await fetchJson('/api/auth/session')
        if (active) {
          setAuthSessionState((current) => ({
            ...current,
            loading: false,
            error: '',
            action: '',
            session: session.signedIn ? session : null,
            tenantCode: session.signedIn ? session.tenantCode : (current.tenantCode || defaultSignInTenantCode),
            username: session.signedIn ? session.username : current.username,
            password: '',
          }))
          setPasswordChangeState((current) => ({
            ...current,
            loading: false,
            error: '',
            success: session.signedIn ? current.success : '',
            form: session.signedIn ? current.form : createDefaultPasswordChangeForm(),
          }))
        }
      } catch (error) {
        if (active) {
          setAuthSessionState((current) => ({
            ...current,
            loading: false,
            error: error.message,
            action: '',
            session: null,
            password: '',
          }))
          setPasswordChangeState((current) => ({ ...current, loading: false, error: '', form: createDefaultPasswordChangeForm() }))
        }
      }
    }
    loadAuthSession()
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!snapshot.inventory.length) return
    setScenarioForm((current) => normalizeScenarioForm(current, snapshot.inventory))
    setComparisonForm((current) => normalizeScenarioForm(current, snapshot.inventory))
  }, [snapshot.inventory])

  useEffect(() => {
    if (!operatorDirectoryState.items.length) return

    const availableOperators = operatorDirectoryState.items.filter((operator) => hasWarehouseScope(operator.warehouseScopes, scenarioForm.warehouseCode))
    const reviewOwnerOptions = availableOperators.filter((operator) => operator.roles.includes('REVIEW_OWNER'))

    if (!availableOperators.some((operator) => operator.actorName === scenarioRequestedBy)) {
      setScenarioRequestedBy(resolvePreferredOperatorName(availableOperators, defaultScenarioRequester))
    }
    if (!reviewOwnerOptions.some((operator) => operator.actorName === scenarioReviewOwner)) {
      setScenarioReviewOwner(resolvePreferredOperatorName(reviewOwnerOptions, defaultScenarioReviewOwner))
    }
  }, [
    operatorDirectoryState.items,
    scenarioForm.warehouseCode,
    scenarioRequestedBy,
    scenarioReviewOwner,
  ])

  useEffect(() => {
    if (!authSessionState.session) return

    const sessionRoles = authSessionState.session.roles ?? []
    setScenarioRequestedBy(authSessionState.session.actorName)

    if (!sessionRoles.includes(scenarioActorRole)) {
      const fallbackScenarioRole = scenarioActorRoles.find((role) => sessionRoles.includes(role))
      if (fallbackScenarioRole) {
        setScenarioActorRole(fallbackScenarioRole)
      }
    }
    if (!sessionRoles.includes(integrationActorRole)) {
      const fallbackIntegrationRole = integrationActorRoles.find((role) => sessionRoles.includes(role))
      if (fallbackIntegrationRole) {
        setIntegrationActorRole(fallbackIntegrationRole)
      }
    }
  }, [authSessionState.session, integrationActorRole, scenarioActorRole])

  useEffect(() => {
    let active = true
    async function loadScenarioHistory() {
      if (!authSessionState.session?.tenantCode) {
        if (active) {
          setScenarioHistoryState({ loading: false, error: '', items: [] })
        }
        return
      }
      setScenarioHistoryState((current) => ({ ...current, loading: true, error: '' }))
      try {
        const history = await fetchJson(buildScenarioHistoryPath(scenarioHistoryFilters))
        if (active) {
          setScenarioHistoryState({ loading: false, error: '', items: history })
        }
      } catch (error) {
        if (active) {
          setScenarioHistoryState({ loading: false, error: error.message, items: [] })
        }
      }
    }
    loadScenarioHistory()
    return () => { active = false }
  }, [scenarioHistoryFilters, activeTenantCode])

  async function toggleSimulation(nextAction) {
    setActionState({ loading: true, error: '' })
    try {
      const response = await fetchApi(`/api/simulation/${nextAction}`, { method: 'POST' })
      if (!response.ok) throw new Error(`Unable to ${nextAction} simulation.`)
      await fetchSnapshot()
      setActionState({ loading: false, error: '' })
    } catch (error) {
      setActionState({ loading: false, error: error.message })
    }
  }

  async function toggleConnector(connector) {
    const loadingKey = `${connector.sourceSystem}:${connector.type}`
    setIntegrationConnectorState({ loadingKey, error: '', success: '' })
    try {
      const response = await fetchApi('/api/integrations/orders/connectors', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sourceSystem: connector.sourceSystem,
          type: connector.type,
          displayName: connector.displayName,
          enabled: !connector.enabled,
          syncMode: connector.syncMode,
          syncIntervalMinutes: connector.syncIntervalMinutes,
          validationPolicy: connector.validationPolicy,
          transformationPolicy: connector.transformationPolicy,
          allowDefaultWarehouseFallback: connector.allowDefaultWarehouseFallback,
          defaultWarehouseCode: connector.defaultWarehouseCode,
          notes: connector.notes,
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to update connector.')
      setIntegrationConnectorState({
        loadingKey: null,
        error: '',
        success: `${payload.displayName} ${payload.enabled ? 'enabled' : 'disabled'}.`,
      })
      await refreshSnapshotQuietly()
    } catch (error) {
      setIntegrationConnectorState({ loadingKey: null, error: error.message, success: '' })
    }
  }

  async function replayFailedIntegration(recordId) {
    setIntegrationReplayState({ loadingId: recordId, error: '', success: '' })
    try {
      const response = await fetchApi(`/api/integrations/orders/replay/${recordId}`, { method: 'POST' })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to replay failed inbound order.')
      setIntegrationReplayState({
        loadingId: null,
        error: '',
        success: `Replayed ${payload.order.externalOrderId} into the live order flow.`,
      })
      await refreshSnapshotQuietly()
    } catch (error) {
      setIntegrationReplayState({ loadingId: null, error: error.message, success: '' })
    }
  }

  const updateScenarioField = (setter, field, value) => setter((current) => field === 'warehouseCode' ? normalizeScenarioForm({ ...current, warehouseCode: value }, snapshot.inventory) : { ...current, [field]: value })
  const updateScenarioLine = (setter, lineId, field, value) => setter((current) => ({ ...current, items: current.items.map((item) => item.id === lineId ? { ...item, [field]: value } : item) }))
  const addScenarioLine = (setter, warehouseCode) => {
    const fallbackSku = buildProductOptions(snapshot.inventory, warehouseCode)[0]?.sku || ''
    setter((current) => ({ ...current, items: [...current.items, createScenarioLine(fallbackSku)] }))
  }
  const removeScenarioLine = (setter, lineId) => setter((current) => current.items.length === 1 ? current : { ...current, items: current.items.filter((item) => item.id !== lineId) })

  const summary = snapshot.summary
  const runtime = systemRuntimeState.runtime
  const warehouseOptions = buildWarehouseOptions(snapshot.inventory)
  const operators = operatorDirectoryState.items
  const signedInSession = authSessionState.session
  const signedInRoles = signedInSession?.roles || []
  const signedInWarehouseScopes = signedInSession?.warehouseScopes || []
  const isAuthenticated = Boolean(signedInSession)
  const passwordRotationRequired = Boolean(signedInSession?.passwordRotationRequired)
  const passwordChangeRequired = Boolean(signedInSession?.passwordChangeRequired)
  const signedInSessionExpiresAt = signedInSession?.sessionExpiresAt
  const signedInPasswordExpiresAt = signedInSession?.passwordExpiresAt
  const canManageTenantAccess = signedInRoles.includes('TENANT_ADMIN')
  const workspaceAdmin = accessAdminState.workspace
  const accessAdminOperators = accessAdminState.operators
  const accessAdminUsers = accessAdminState.users
  const requesterOperators = operators.filter((operator) => hasWarehouseScope(operator.warehouseScopes, scenarioForm.warehouseCode))
  const reviewOwnerOperators = operators.filter((operator) => operator.roles.includes('REVIEW_OWNER') && hasWarehouseScope(operator.warehouseScopes, scenarioForm.warehouseCode))
  const signedInActorName = signedInSession?.actorName || ''
  const primaryContext = buildScenarioContext(scenarioForm)
  const alternativeContext = buildScenarioContext(comparisonForm)
  const historyWarehouseOptions = [{ code: 'ALL', name: 'All warehouses' }, ...warehouseOptions]
  const scenarioHistoryItems = scenarioHistoryState.items
  const scenarioHistoryEmptyMessage = hasActiveScenarioHistoryFilters(scenarioHistoryFilters)
    ? 'No planning runs match the current ownership, approval, policy, priority, overdue, or SLA escalation filters.'
    : 'Scenario previews and comparisons will appear here after operators explore what-if plans.'
  const enabledConnectorCount = snapshot.integrationConnectors.filter((connector) => connector.enabled).length
  const pendingReplayCount = snapshot.integrationReplayQueue.length
  const pendingReviewCount = scenarioHistoryItems.filter((scenario) => scenario.approvalStatus === 'PENDING_APPROVAL').length
  const activeDecisionCount = (summary?.activeAlerts ?? 0) + (summary?.recommendationsCount ?? 0)
  const systemIncidents = snapshot.systemIncidents
  const fulfillmentOverview = snapshot.fulfillment || emptySnapshot.fulfillment
  const recentAuditEntries = snapshot.auditLogs.slice(0, 6)
  const recentBusinessEvents = snapshot.recentEvents.slice(0, 6)
  const selectedRuntimeIncident = systemIncidents.find((incident) => incident.incidentKey === selectedRuntimeIncidentKey) || systemIncidents[0]
  const selectedAuditTrace = recentAuditEntries
    .map((log) => ({ ...log, traceType: 'audit', traceKey: `audit-${log.id}` }))
    .concat(recentBusinessEvents.map((event) => ({ ...event, traceType: 'event', traceKey: `event-${event.id}` })))
    .find((entry) => entry.traceKey === selectedTraceEntryKey)
    || (recentAuditEntries[0] ? { ...recentAuditEntries[0], traceType: 'audit', traceKey: `audit-${recentAuditEntries[0].id}` } : null)
    || (recentBusinessEvents[0] ? { ...recentBusinessEvents[0], traceType: 'event', traceKey: `event-${recentBusinessEvents[0].id}` } : null)
  const accessSubjects = [
    ...accessAdminOperators.map((operator) => ({ ...operator, subjectType: 'operator', subjectKey: `operator-${operator.id}` })),
    ...accessAdminUsers.map((user) => ({ ...user, subjectType: 'user', subjectKey: `user-${user.id}` })),
  ]
  const selectedAccessSubject = accessSubjects.find((subject) => subject.subjectKey === selectedAccessSubjectKey) || accessSubjects[0]
  const selectedTenantPortfolio = tenantDirectoryState.items.find((tenant) => tenant.code === selectedTenantPortfolioCode) || tenantDirectoryState.items[0]
  const selectedWorkspaceWarehouse = workspaceAdmin?.warehouses?.find((warehouse) => warehouse.id === selectedWorkspaceWarehouseId) || workspaceAdmin?.warehouses?.[0]
  const selectedWorkspaceWarehouseDraft = selectedWorkspaceWarehouse ? (workspaceWarehouseDrafts[selectedWorkspaceWarehouse.id] || { name: selectedWorkspaceWarehouse.name, location: selectedWorkspaceWarehouse.location }) : null
  const selectedWorkspaceConnector = workspaceAdmin?.connectors?.find((connector) => connector.id === selectedWorkspaceConnectorId) || workspaceAdmin?.connectors?.[0]
  const selectedWorkspaceConnectorDraft = selectedWorkspaceConnector
    ? (workspaceConnectorDrafts[selectedWorkspaceConnector.id] || {
        supportOwnerActorName: selectedWorkspaceConnector.supportOwnerActorName || '',
        syncMode: selectedWorkspaceConnector.syncMode || 'REALTIME_PUSH',
        syncIntervalMinutes: selectedWorkspaceConnector.syncIntervalMinutes == null ? '' : String(selectedWorkspaceConnector.syncIntervalMinutes),
        validationPolicy: selectedWorkspaceConnector.validationPolicy || 'STANDARD',
        transformationPolicy: selectedWorkspaceConnector.transformationPolicy || 'NONE',
        allowDefaultWarehouseFallback: Boolean(selectedWorkspaceConnector.allowDefaultWarehouseFallback),
        notes: selectedWorkspaceConnector.notes || '',
      })
    : null
  const selectedWorkspaceConnectorOwnerOptions = selectedWorkspaceConnector
    ? accessAdminOperators.filter((operator) => operator.active && operatorCanOwnConnector(operator, selectedWorkspaceConnector))
    : []
  const currentPageMeta = pageLookup[currentPage] || pageLookup.landing
  const effectivePageMeta = !isAuthenticated && currentPageMeta.audience === 'app' ? pageLookup['sign-in'] : currentPageMeta
  const isPublicPage = effectivePageMeta.audience === 'public'
  const isDashboardPage = currentPage === 'dashboard'
  const isAlertsPage = currentPage === 'alerts'
  const isRecommendationsPage = currentPage === 'recommendations'
  const isOrdersPage = currentPage === 'orders'
  const isInventoryPage = currentPage === 'inventory'
  const isLocationsPage = currentPage === 'locations'
  const isFulfillmentPage = currentPage === 'fulfillment'
  const isScenariosPage = currentPage === 'scenarios'
  const isScenarioHistoryPage = currentPage === 'scenario-history'
  const isApprovalsPage = currentPage === 'approvals'
  const isEscalationsPage = currentPage === 'escalations'
  const isIntegrationsPage = currentPage === 'integrations'
  const isReplayPage = currentPage === 'replay'
  const isRuntimePage = currentPage === 'runtime'
  const isAuditPage = currentPage === 'audit'
  const isUsersPage = currentPage === 'users'
  const isSettingsPage = currentPage === 'settings'
  const isProfilePage = currentPage === 'profile'
  const isPlatformPage = currentPage === 'platform'
  const isTenantsPage = currentPage === 'tenants'
  const isSystemConfigPage = currentPage === 'system-config'
  const isReleasesPage = currentPage === 'releases'
  const pageBadgeMap = {
    dashboard: summary?.totalOrders ?? 0,
    alerts: summary?.activeAlerts ?? snapshot.alerts.activeAlerts.length,
    recommendations: summary?.recommendationsCount ?? snapshot.recommendations.length,
    orders: summary?.recentOrderCount ?? snapshot.recentOrders.length,
    inventory: summary?.inventoryRecordsCount ?? snapshot.inventory.length,
    locations: summary?.totalWarehouses ?? warehouseOptions.length,
    fulfillment: fulfillmentOverview.backlogCount + fulfillmentOverview.delayedShipmentCount,
    scenarios: pendingReviewCount,
    'scenario-history': scenarioHistoryItems.length,
    approvals: pendingReviewCount,
    escalations: snapshot.slaEscalations.length || systemIncidents.length,
    integrations: snapshot.integrationConnectors.length,
    replay: pendingReplayCount,
    runtime: systemIncidents.length,
    audit: snapshot.auditLogs.length,
    users: accessAdminUsers.length,
    settings: canManageTenantAccess ? (workspaceAdmin?.warehouses?.length || 0) : 0,
    profile: signedInSession ? 1 : 0,
    platform: systemIncidents.length,
    tenants: tenantDirectoryState.items.length,
    'system-config': runtime?.backbone?.pendingDispatchCount ?? 0,
    releases: runtime?.build?.version ? 1 : 0,
    landing: 0,
    product: 0,
    'sign-in': 0,
    contact: 0,
  }
  const pageStatusMap = {
    dashboard: isAuthenticated ? (summary?.lastUpdatedAt ? `Updated ${formatTimestamp(summary.lastUpdatedAt)}` : 'Waiting for live summary') : 'Sign in to unlock the control center',
    alerts: isAuthenticated ? (snapshot.alerts.activeAlerts.length ? `${snapshot.alerts.activeAlerts.length} active operational alert${snapshot.alerts.activeAlerts.length === 1 ? '' : 's'}` : 'No active alert pressure') : 'Protected by workspace sign-in',
    recommendations: isAuthenticated ? (snapshot.recommendations.length ? `${snapshot.recommendations.length} recommendation${snapshot.recommendations.length === 1 ? '' : 's'} waiting for review` : 'No immediate recommendation pressure') : 'Protected by workspace sign-in',
    orders: isAuthenticated ? (summary?.recentOrderCount ? `${summary.recentOrderCount} order${summary.recentOrderCount === 1 ? '' : 's'} moved recently` : 'Order flow is currently quiet') : 'Protected by workspace sign-in',
    inventory: isAuthenticated ? (summary?.lowStockItems ? `${summary.lowStockItems} low-stock item${summary.lowStockItems === 1 ? '' : 's'}` : 'Inventory posture is stable') : 'Protected by workspace sign-in',
    locations: isAuthenticated ? `${warehouseOptions.length} operational location${warehouseOptions.length === 1 ? '' : 's'} tracked` : 'Protected by workspace sign-in',
    fulfillment: isAuthenticated ? (fulfillmentOverview.backlogCount ? `${fulfillmentOverview.backlogCount} backlog item${fulfillmentOverview.backlogCount === 1 ? '' : 's'} active` : 'Fulfillment lanes are clear') : 'Protected by workspace sign-in',
    scenarios: isAuthenticated ? 'Model operational changes before they touch live flow' : 'Protected by workspace sign-in',
    'scenario-history': isAuthenticated ? `${scenarioHistoryItems.length} scenario run${scenarioHistoryItems.length === 1 ? '' : 's'} in view` : 'Protected by workspace sign-in',
    approvals: isAuthenticated ? (pendingReviewCount ? `${pendingReviewCount} plan${pendingReviewCount === 1 ? '' : 's'} need approval` : 'Approval queues are clear') : 'Protected by workspace sign-in',
    escalations: isAuthenticated ? (snapshot.slaEscalations.length ? `${snapshot.slaEscalations.length} escalation${snapshot.slaEscalations.length === 1 ? '' : 's'} require ownership` : 'Escalation inbox is clear') : 'Protected by workspace sign-in',
    integrations: isAuthenticated ? `${enabledConnectorCount}/${snapshot.integrationConnectors.length || 0} connectors enabled` : 'Protected by workspace sign-in',
    replay: isAuthenticated ? (pendingReplayCount ? `${pendingReplayCount} replay item${pendingReplayCount === 1 ? '' : 's'} waiting` : 'Replay queue is clear') : 'Protected by workspace sign-in',
    runtime: isAuthenticated ? (runtime?.overallStatus ? `Runtime ${runtime.overallStatus}` : 'Loading trust signals') : 'Protected by workspace sign-in',
    audit: isAuthenticated ? (snapshot.auditLogs.length ? `${snapshot.auditLogs.length} recent audit entr${snapshot.auditLogs.length === 1 ? 'y' : 'ies'}` : 'Traceability feed is quiet') : 'Protected by workspace sign-in',
    users: isAuthenticated ? (canManageTenantAccess ? `${accessAdminUsers.length} tenant user${accessAdminUsers.length === 1 ? '' : 's'} managed` : 'Tenant admin access required') : 'Sign in with an admin workspace account',
    settings: isAuthenticated ? (canManageTenantAccess ? 'Workspace controls ready for tenant configuration' : 'Tenant admin access required') : 'Sign in with an admin workspace account',
    profile: isAuthenticated ? (passwordChangeRequired || passwordRotationRequired ? 'Password hygiene needs attention' : 'Personal access posture is healthy') : 'Sign in to access profile controls',
    platform: isAuthenticated ? 'Cross-tenant and release trust view' : 'Protected by workspace sign-in',
    tenants: isAuthenticated ? `${tenantDirectoryState.items.length} tenant workspace${tenantDirectoryState.items.length === 1 ? '' : 's'} visible` : 'Sign in to view tenant posture',
    'system-config': isAuthenticated ? 'Inspect dispatch, runtime, and policy defaults' : 'Protected by workspace sign-in',
    releases: isAuthenticated ? `Backend ${formatBuildValue(runtime?.build?.version)}` : 'Protected by workspace sign-in',
    landing: 'See what SynapseCore becomes at launch',
    product: 'Explore the full operating model',
    'sign-in': 'Access the live operational workspace',
    contact: 'Capture the business challenge and rollout context',
  }
  const showOperationalTools = Boolean(runtime?.activeProfiles?.some((profile) => ['local', 'dev'].includes(profile)))
  const metrics = [
    { label: 'Total Orders', value: summary ? summary.totalOrders : pageState.loading ? 'Loading' : 'No data', accent: 'amber' },
    { label: 'Active Alerts', value: summary ? summary.activeAlerts : pageState.loading ? 'Loading' : 'No data', accent: 'rose' },
    { label: 'Low Stock Items', value: summary ? summary.lowStockItems : pageState.loading ? 'Loading' : 'No data', accent: 'orange' },
    { label: 'Recommendations', value: summary ? summary.recommendationsCount : pageState.loading ? 'Loading' : 'No data', accent: 'teal' },
    { label: 'Fulfillment Backlog', value: summary ? summary.fulfillmentBacklogCount : pageState.loading ? 'Loading' : 'No data', accent: 'slate' },
    { label: 'Delayed Shipments', value: summary ? summary.delayedShipmentCount : pageState.loading ? 'Loading' : 'No data', accent: 'rose' },
    { label: 'Products', value: summary ? summary.totalProducts : pageState.loading ? 'Loading' : 'No data', accent: 'blue' },
    { label: 'Warehouses', value: summary ? summary.totalWarehouses : pageState.loading ? 'Loading' : 'No data', accent: 'slate' },
  ]
  const controlHighlights = [
    {
      label: 'Workspace Session',
      value: signedInSession ? signedInSession.tenantName || signedInSession.tenantCode : 'No Active Session',
      note: signedInSession ? `${signedInSession.displayName} is operating as ${signedInSession.actorName}.` : 'Sign in with a workspace account before taking protected actions.',
      tone: signedInSession ? 'success' : 'warning',
    },
    {
      label: 'Decision Pressure',
      value: activeDecisionCount ? `${activeDecisionCount} open signals` : 'Stable',
      note: activeDecisionCount ? 'Alerts and recommendations are live across the tenant decision layer.' : 'No active alert or recommendation pressure right now.',
      tone: activeDecisionCount ? 'danger' : 'success',
    },
    {
      label: 'Operational Throughput',
      value: summary?.recentOrderCount ? `${summary.recentOrderCount} recent orders` : 'Monitoring',
      note: fulfillmentOverview.backlogCount
        ? `${fulfillmentOverview.backlogCount} backlog item${fulfillmentOverview.backlogCount === 1 ? '' : 's'} and ${fulfillmentOverview.delayedShipmentCount} delayed shipment${fulfillmentOverview.delayedShipmentCount === 1 ? '' : 's'} are active.`
        : `${enabledConnectorCount}/${snapshot.integrationConnectors.length || 0} connectors are enabled with no fulfillment backlog right now.`,
      tone: fulfillmentOverview.backlogCount || pendingReplayCount ? 'warning' : 'neutral',
    },
  ]
  const showDashboardHero = isDashboardPage
  const showSummaryMetrics = isDashboardPage
  const showScenarioPlanner = isScenariosPage
  const showScenarioHistory = isScenariosPage || isScenarioHistoryPage
  const showScenarioNotifications = isScenariosPage
  const showEscalationInbox = isScenariosPage
  const showRiskAlertsPanel = isDashboardPage
  const showRiskRecommendationsPanel = isDashboardPage
  const showInventoryPanel = isDashboardPage
  const showFulfillmentPanel = isDashboardPage
  const showOrdersPanel = isDashboardPage
  const showRuntimePanel = isDashboardPage || isRuntimePage
  const showIncidentPanel = isDashboardPage || isRuntimePage
  const showBusinessEventsPanel = isDashboardPage || isAuditPage
  const showAuditPanel = isDashboardPage || isAuditPage
  const showIntegrationConnectorsPanel = isIntegrationsPage
  const showIntegrationImportsPanel = isIntegrationsPage
  const showReplayQueuePanel = isReplayPage || isIntegrationsPage
  const showAccessAdminPanel = isUsersPage || isTenantsPage
  const showTenantManagementPanel = isTenantsPage
  const showLocationsExperience = isLocationsPage
  const appPageCount = appPages.length
  const urgentActions = [
    ...snapshot.alerts.activeAlerts.slice(0, 3).map((alert) => ({
      id: `alert-${alert.id}`,
      kicker: formatCodeLabel(alert.severity),
      title: alert.title,
      note: alert.recommendedAction || alert.impactSummary,
      target: 'alerts',
    })),
    ...snapshot.recommendations.slice(0, 3).map((recommendation) => ({
      id: `recommendation-${recommendation.id}`,
      kicker: formatCodeLabel(recommendation.priority),
      title: recommendation.title,
      note: recommendation.description,
      target: 'recommendations',
    })),
  ].slice(0, 5)
  const activeAlerts = snapshot.alerts.activeAlerts
  const lowStockInventory = snapshot.inventory.filter((item) => item.lowStock)
  const highRiskInventory = snapshot.inventory.filter((item) => item.riskLevel === 'critical' || item.riskLevel === 'high')
  const fastMovingInventory = [...snapshot.inventory].sort((left, right) => (right.unitsPerHour || 0) - (left.unitsPerHour || 0)).slice(0, 5)
  const delayedFulfillments = fulfillmentOverview.activeFulfillments.filter((task) => task.fulfillmentStatus === 'DELAYED' || task.riskLevel === 'critical' || task.riskLevel === 'high')
  const pendingApprovalScenarios = scenarioHistoryItems.filter((scenario) => scenario.approvalStatus === 'PENDING_APPROVAL')
  const approvedScenarios = scenarioHistoryItems.filter((scenario) => scenario.approvalStatus === 'APPROVED')
  const rejectedScenarios = scenarioHistoryItems.filter((scenario) => scenario.approvalStatus === 'REJECTED')
  const overdueScenarios = scenarioHistoryItems.filter((scenario) => scenario.overdue)
  const escalatedScenarios = snapshot.slaEscalations
  const approvalBoard = pendingApprovalScenarios.slice(0, 6)
  const approvalQueueScenarios = dedupeById([...pendingApprovalScenarios, ...overdueScenarios, ...approvedScenarios, ...rejectedScenarios])
  const selectedHistoryScenario = scenarioHistoryItems.find((scenario) => scenario.id === selectedScenarioId) || scenarioHistoryItems[0]
  const selectedApprovalScenario = approvalQueueScenarios.find((scenario) => scenario.id === selectedScenarioId)
    || pendingApprovalScenarios[0]
    || overdueScenarios[0]
    || approvalQueueScenarios[0]
  const selectedEscalationScenario = escalatedScenarios.find((scenario) => scenario.id === selectedScenarioId) || escalatedScenarios[0]
  const recommendationNow = snapshot.recommendations.filter((recommendation) => ['CRITICAL', 'HIGH'].includes(recommendation.priority))
  const recommendationSoon = snapshot.recommendations.filter((recommendation) => recommendation.priority === 'MEDIUM')
  const recommendationWatch = snapshot.recommendations.filter((recommendation) => !['CRITICAL', 'HIGH', 'MEDIUM'].includes(recommendation.priority))
  const globalNotificationCount = activeAlerts.length + pendingReplayCount + pendingApprovalScenarios.length + systemIncidents.length
  const liveClockLabel = new Date(clockTick).toLocaleString()
  const deferredWorkspaceSearch = useDeferredValue(workspaceSearch.trim().toLowerCase())
  const hasWorkspaceSearch = Boolean(deferredWorkspaceSearch)
  const workspaceSearchSections = hasWorkspaceSearch
    ? [
        {
          key: 'pages',
          label: 'Pages',
          items: appPages
            .filter((page) => page.label.toLowerCase().includes(deferredWorkspaceSearch) || page.title.toLowerCase().includes(deferredWorkspaceSearch))
            .map((page) => ({
              id: `page-${page.key}`,
              title: page.label,
              meta: page.description,
              target: page.key,
            })),
        },
        {
          key: 'orders',
          label: 'Orders',
          items: snapshot.recentOrders
            .filter((order) => order.externalOrderId.toLowerCase().includes(deferredWorkspaceSearch))
            .slice(0, 3)
            .map((order) => ({
              id: `order-${order.id}`,
              title: order.externalOrderId,
              meta: `${order.warehouseName} | ${currency.format(order.totalAmount)}`,
              target: 'orders',
            })),
        },
        {
          key: 'alerts',
          label: 'Alerts',
          items: activeAlerts
            .filter((alert) => alert.title.toLowerCase().includes(deferredWorkspaceSearch))
            .slice(0, 3)
            .map((alert) => ({
              id: `alert-search-${alert.id}`,
              title: alert.title,
              meta: alert.impactSummary,
              target: 'alerts',
            })),
        },
        {
          key: 'incidents',
          label: 'Incidents',
          items: systemIncidents
            .filter((incident) => incident.title.toLowerCase().includes(deferredWorkspaceSearch) || incident.detail.toLowerCase().includes(deferredWorkspaceSearch))
            .slice(0, 3)
            .map((incident) => ({
              id: `incident-search-${incident.incidentKey}`,
              title: incident.title,
              meta: `${formatCodeLabel(incident.severity)} | ${incident.context}`,
              target: 'runtime',
            })),
        },
      ]
        .map((section) => ({ ...section, items: section.items.slice(0, 3) }))
        .filter((section) => section.items.length)
        .slice(0, 4)
    : []
  const workspaceSearchMatchCount = workspaceSearchSections.reduce((total, section) => total + section.items.length, 0)
  const firstWorkspaceSearchResult = workspaceSearchSections[0]?.items?.[0] || null
  const pageSectionActions = pageSectionMap[currentPage] || []
  const topbarQuickActions = (() => {
    switch (currentPage) {
      case 'dashboard':
        return [
          { label: 'Open alerts', target: 'alerts' },
          { label: 'Open approvals', target: 'approvals' },
        ]
      case 'alerts':
        return [
          { label: 'Open recommendations', target: 'recommendations' },
          { label: 'Open runtime', target: 'runtime' },
        ]
      case 'recommendations':
        return [
          { label: 'Open alerts', target: 'alerts' },
          { label: 'Open orders', target: 'orders' },
        ]
      case 'orders':
        return [
          { label: 'Open fulfillment', target: 'fulfillment' },
          { label: 'Open inventory', target: 'inventory' },
        ]
      case 'inventory':
        return [
          { label: 'Open locations', target: 'locations' },
          { label: 'Open recommendations', target: 'recommendations' },
        ]
      case 'scenario-history':
        return [
          { label: 'Open scenarios', target: 'scenarios' },
          { label: 'Open approvals', target: 'approvals' },
        ]
      case 'approvals':
        return [
          { label: 'Open escalations', target: 'escalations' },
          { label: 'Open scenario history', target: 'scenario-history' },
        ]
      case 'runtime':
        return [
          { label: 'Open audit', target: 'audit' },
          { label: 'Open releases', target: 'releases' },
        ]
      case 'audit':
        return [
          { label: 'Open replay', target: 'replay' },
          { label: 'Open runtime', target: 'runtime' },
        ]
      case 'users':
        return [
          { label: 'Open settings', target: 'settings' },
          { label: 'Open profile', target: 'profile' },
        ]
      case 'settings':
        return [
          { label: 'Open users', target: 'users' },
          { label: 'Open platform', target: 'platform' },
        ]
      case 'platform':
        return [
          { label: 'Open releases', target: 'releases' },
          { label: 'Open tenants', target: 'tenants' },
        ]
      case 'releases':
        return [
          { label: 'Open runtime', target: 'runtime' },
          { label: 'Open platform', target: 'platform' },
        ]
      default:
        return [
          { label: 'Open alerts', target: 'alerts' },
          { label: 'Open approvals', target: 'approvals' },
        ]
    }
  })()
  const utilityTimeline = [
    ...systemIncidents.slice(0, 2).map((incident) => ({
      id: `incident-${incident.incidentKey}`,
      title: incident.title,
      meta: `${formatCodeLabel(incident.severity)} incident`,
      timestamp: incident.createdAt,
    })),
    ...snapshot.recentEvents.slice(0, 2).map((event) => ({
      id: `event-${event.id}`,
      title: formatCodeLabel(event.eventType),
      meta: event.payloadSummary,
      timestamp: event.createdAt,
    })),
    ...snapshot.auditLogs.slice(0, 2).map((log) => ({
      id: `audit-${log.id}`,
      title: formatCodeLabel(log.action),
      meta: `${log.targetType} | ${log.targetRef}`,
      timestamp: log.createdAt,
    })),
  ].slice(0, 6)

  function resolveDefaultManagedOperator(preferredActorName = '') {
    if (preferredActorName && accessAdminOperators.some((operator) => operator.actorName === preferredActorName)) {
      return preferredActorName
    }
    return accessAdminOperators.find((operator) => operator.active)?.actorName || accessAdminOperators[0]?.actorName || ''
  }

  function resetAccessOperatorEditor() {
    setAccessOperatorForm(createDefaultAccessOperatorForm())
  }

  function resetAccessUserEditor(preferredActorName = '') {
    setAccessUserForm({
      ...createDefaultAccessUserForm(),
      operatorActorName: resolveDefaultManagedOperator(preferredActorName),
    })
  }

  function editTenantOperator(operator) {
    setAccessOperatorForm(buildAccessOperatorForm(operator))
    setAccessAdminState((current) => ({ ...current, error: '', success: '' }))
  }

  function editTenantUser(user) {
    setAccessUserForm(buildAccessUserForm(user))
    setAccessAdminState((current) => ({ ...current, error: '', success: '' }))
  }

  function operatorCanOwnConnector(operator, connector) {
    return !connector.defaultWarehouseCode || !operator.warehouseScopes.length || operator.warehouseScopes.includes(connector.defaultWarehouseCode)
  }

  async function saveWorkspaceSettings() {
    setAccessAdminState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const response = await fetchApi('/api/access/admin/workspace', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tenantName: workspaceSettingsForm.tenantName.trim(),
          description: workspaceSettingsForm.description.trim(),
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to update tenant workspace settings.')

      await fetchAccessAdminData()
      setAccessAdminState((current) => ({
        ...current,
        loading: false,
        error: '',
        success: `${payload.tenantName} workspace settings were updated.`,
      }))
    } catch (error) {
      setAccessAdminState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  async function saveWorkspaceSecuritySettings() {
    setAccessAdminState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const response = await fetchApi('/api/access/admin/workspace/security', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          passwordRotationDays: Number.parseInt(workspaceSecurityForm.passwordRotationDays, 10),
          sessionTimeoutMinutes: Number.parseInt(workspaceSecurityForm.sessionTimeoutMinutes, 10),
          invalidateOtherSessions: workspaceSecurityForm.invalidateOtherSessions,
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to update tenant security settings.')

      await fetchAccessAdminData()
      setAccessAdminState((current) => ({
        ...current,
        loading: false,
        error: '',
        success: workspaceSecurityForm.invalidateOtherSessions
          ? `Security settings updated. Other active ${payload.tenantCode} sessions must sign in again.`
          : `Security settings updated for ${payload.tenantName}.`,
      }))
    } catch (error) {
      setAccessAdminState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  async function saveWorkspaceWarehouse(warehouseId) {
    const draft = workspaceWarehouseDrafts[warehouseId]
    if (!draft) return
    setAccessAdminState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const response = await fetchApi(`/api/access/admin/workspace/warehouses/${warehouseId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: draft.name.trim(),
          location: draft.location.trim(),
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to update workspace warehouse settings.')

      await fetchAccessAdminData()
      setAccessAdminState((current) => ({
        ...current,
        loading: false,
        error: '',
        success: `${payload.code} workspace settings were updated.`,
      }))
    } catch (error) {
      setAccessAdminState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  async function saveWorkspaceConnectorSupport(connectorId) {
    const draft = workspaceConnectorDrafts[connectorId]
    if (!draft) return
    setAccessAdminState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const response = await fetchApi(`/api/access/admin/workspace/connectors/${connectorId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          supportOwnerActorName: draft.supportOwnerActorName,
          syncMode: draft.syncMode,
          syncIntervalMinutes: draft.syncMode === 'SCHEDULED_PULL' && draft.syncIntervalMinutes ? Number.parseInt(draft.syncIntervalMinutes, 10) : null,
          validationPolicy: draft.validationPolicy,
          transformationPolicy: draft.transformationPolicy,
          allowDefaultWarehouseFallback: draft.allowDefaultWarehouseFallback,
          notes: draft.notes,
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to update connector support ownership.')

      await fetchAccessAdminData()
      setAccessAdminState((current) => ({
        ...current,
        loading: false,
        error: '',
        success: `${payload.displayName} connector policy was updated.`,
      }))
    } catch (error) {
      setAccessAdminState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }
  const comparisonPrimarySummary = comparisonState.result ? summarizeImpact(comparisonState.result.primary) : null
  const comparisonAlternativeSummary = comparisonState.result ? summarizeImpact(comparisonState.result.alternative) : null

  async function analyzeScenario() {
    setScenarioState({ loading: true, error: '', result: null })
    try {
      const response = await fetchApi('/api/scenarios/order-impact', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ warehouseCode: scenarioForm.warehouseCode, items: primaryContext.requestItems }) })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to analyze scenario.')
      setScenarioState({ loading: false, error: '', result: payload })
      await Promise.all([refreshSnapshotQuietly(), refreshScenarioHistoryQuietly()])
    } catch (error) {
      setScenarioState({ loading: false, error: error.message, result: null })
    }
  }

  async function compareScenarios() {
    setComparisonState({ loading: true, error: '', result: null })
    try {
      const response = await fetchApi('/api/scenarios/order-impact/compare', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          primaryLabel: 'Scenario A',
          primary: { warehouseCode: scenarioForm.warehouseCode, items: primaryContext.requestItems },
          alternativeLabel: 'Scenario B',
          alternative: { warehouseCode: comparisonForm.warehouseCode, items: alternativeContext.requestItems },
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to compare scenarios.')
      setComparisonState({ loading: false, error: '', result: payload })
      await Promise.all([refreshSnapshotQuietly(), refreshScenarioHistoryQuietly()])
    } catch (error) {
      setComparisonState({ loading: false, error: error.message, result: null })
    }
  }

  async function executeScenario(scenarioId) {
    setScenarioExecutionState({ loadingId: scenarioId, error: '', success: '' })
    try {
      const response = await fetchApi(`/api/scenarios/${scenarioId}/execute`, { method: 'POST' })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to execute scenario.')
      setScenarioExecutionState({
        loadingId: null,
        error: '',
        success: `Executed ${payload.scenarioTitle} as live order ${payload.order.externalOrderId}.`,
      })
      await Promise.all([fetchSnapshot(), refreshScenarioHistoryQuietly()])
    } catch (error) {
      setScenarioExecutionState({ loadingId: null, error: error.message, success: '' })
    }
  }

  async function loadScenarioIntoPlanner(scenarioId) {
    setScenarioLoadState({ loadingId: scenarioId, error: '', success: '' })
    try {
      const response = await fetchApi(`/api/scenarios/${scenarioId}/request`)
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to load scenario into planner.')
      const sourceScenario = scenarioHistoryItems.find((scenario) => scenario.id === scenarioId)
      setScenarioForm({
        warehouseCode: payload.request.warehouseCode,
        items: payload.request.items.map((item) => ({
          id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
          productSku: item.productSku,
          quantity: String(item.quantity),
          unitPrice: String(item.unitPrice),
        })),
      })
      if (sourceScenario?.type === 'SAVED_PLAN' && sourceScenario.approvalStatus === 'REJECTED') {
        const nextRevisionNumber = (sourceScenario.revisionNumber ?? 1) + 1
        setScenarioRevisionSource({
          id: sourceScenario.id,
          title: sourceScenario.title,
          revisionNumber: nextRevisionNumber,
        })
        setScenarioPlanName(buildRevisionTitle(payload.scenarioTitle, nextRevisionNumber))
      } else {
        setScenarioRevisionSource(null)
        setScenarioPlanName(payload.scenarioTitle)
      }
      setScenarioRequestedBy(sourceScenario?.requestedBy || defaultScenarioRequester)
      setScenarioReviewOwner(sourceScenario?.reviewOwner || defaultScenarioReviewOwner)
      setScenarioState(emptyRequestState)
      setScenarioLoadState({
        loadingId: null,
        error: '',
        success: `Loaded ${payload.scenarioTitle} into Scenario A.`,
      })
    } catch (error) {
      setScenarioLoadState({ loadingId: null, error: error.message, success: '' })
    }
  }

  async function saveScenarioPlan() {
    setScenarioSaveState({ loading: true, error: '', success: '' })
    try {
      const response = await fetchApi('/api/scenarios/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: scenarioPlanName.trim(),
          requestedBy: authSessionState.session?.actorName || scenarioRequestedBy.trim(),
          reviewOwner: scenarioReviewOwner.trim(),
          revisionOfScenarioRunId: scenarioRevisionSource?.id ?? null,
          request: { warehouseCode: scenarioForm.warehouseCode, items: primaryContext.requestItems },
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to save scenario plan.')
      setScenarioSaveState({
        loading: false,
        error: '',
        success: payload.revisionNumber > 1
          ? `Saved revision ${payload.revisionNumber} of ${payload.title} for ${payload.warehouseCode} as ${formatCodeLabel(payload.reviewPriority)} priority with ${formatCodeLabel(payload.approvalPolicy)} approval (score ${payload.riskScore}). Due ${formatTimestamp(payload.approvalDueAt)}.`
          : `Saved plan ${payload.title} for ${payload.warehouseCode} as ${formatCodeLabel(payload.reviewPriority)} priority with ${formatCodeLabel(payload.approvalPolicy)} approval (score ${payload.riskScore}). Due ${formatTimestamp(payload.approvalDueAt)}.`,
      })
      setScenarioRevisionSource(null)
      await Promise.all([fetchSnapshot(), refreshScenarioHistoryQuietly()])
    } catch (error) {
      setScenarioSaveState({ loading: false, error: error.message, success: '' })
    }
  }

  async function approveScenarioPlan(scenarioId) {
    setScenarioApprovalState({ loadingId: scenarioId, error: '', success: '' })
    try {
      const response = await fetchApi(`/api/scenarios/${scenarioId}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ actorRole: scenarioActorRole, approverName: signedInActorName, approvalNote: scenarioReviewNote.trim() || null }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to approve scenario plan.')
      setScenarioApprovalState({
        loadingId: null,
        error: '',
        success: payload.executionReady
          ? `Approved ${payload.title} for execution under ${formatCodeLabel(payload.approvalPolicy)} approval.`
          : `Recorded owner review for ${payload.title}. Final approval is still required by ${payload.finalApprovalOwner || 'the assigned final approver'} before ${formatTimestamp(payload.approvalDueAt)}.`,
      })
      await Promise.all([fetchSnapshot(), refreshScenarioHistoryQuietly()])
    } catch (error) {
      setScenarioApprovalState({ loadingId: null, error: error.message, success: '' })
    }
  }

  async function rejectScenarioPlan(scenarioId) {
    setScenarioRejectionState({ loadingId: scenarioId, error: '', success: '' })
    try {
      const response = await fetchApi(`/api/scenarios/${scenarioId}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ actorRole: scenarioActorRole, reviewerName: signedInActorName, reason: scenarioReviewNote.trim() }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to reject scenario plan.')
      setScenarioRejectionState({
        loadingId: null,
        error: '',
        success: `Rejected ${payload.title} by ${payload.rejectedBy}.`,
      })
      await Promise.all([fetchSnapshot(), refreshScenarioHistoryQuietly()])
    } catch (error) {
      setScenarioRejectionState({ loadingId: null, error: error.message, success: '' })
    }
  }

  async function acknowledgeScenarioEscalation(scenarioId) {
    setScenarioEscalationAckState({ loadingId: scenarioId, error: '', success: '' })
    try {
      const response = await fetchApi(`/api/scenarios/${scenarioId}/acknowledge-escalation`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ actorRole: scenarioActorRole, acknowledgedBy: signedInActorName, note: scenarioReviewNote.trim() }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to acknowledge SLA escalation.')
      setScenarioEscalationAckState({
        loadingId: null,
        error: '',
        success: `Acknowledged escalated plan ${payload.title} as ${payload.slaAcknowledgedBy}.`,
      })
      await Promise.all([fetchSnapshot(), refreshScenarioHistoryQuietly()])
    } catch (error) {
      setScenarioEscalationAckState({ loadingId: null, error: error.message, success: '' })
    }
  }

  async function signInOperator() {
    setAuthSessionState((current) => ({ ...current, loading: true, error: '', action: 'signin' }))
    setPasswordChangeState((current) => ({ ...current, error: '', success: '' }))
    try {
      const response = await fetchApi('/api/auth/session/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tenantCode: authSessionState.tenantCode.trim(),
          username: authSessionState.username.trim(),
          password: authSessionState.password,
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to sign in operator.')
      setAuthSessionState((current) => ({
        ...current,
        loading: false,
        error: '',
        action: '',
        session: payload,
        tenantCode: payload.tenantCode || current.tenantCode,
        username: payload.username,
        password: '',
      }))
      setPasswordChangeState({ loading: false, error: '', success: '', form: createDefaultPasswordChangeForm() })
      navigateToPage('dashboard')
    } catch (error) {
      setAuthSessionState((current) => ({ ...current, loading: false, error: error.message, action: '' }))
    }
  }

  async function signOutOperator() {
    setAuthSessionState((current) => ({ ...current, loading: true, error: '', action: 'signout' }))
    setPasswordChangeState((current) => ({ ...current, loading: false, error: '', success: '', form: createDefaultPasswordChangeForm() }))
    try {
      const response = await fetchApi('/api/auth/session/logout', { method: 'POST' })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to sign out operator.')
      setAuthSessionState((current) => ({
        ...current,
        loading: false,
        error: '',
        action: '',
        session: payload.signedIn ? payload : null,
        tenantCode: payload.signedIn ? payload.tenantCode || current.tenantCode : '',
        username: payload.signedIn ? current.username : '',
        password: '',
      }))
      setSnapshot(emptySnapshot)
      setScenarioHistoryState({ loading: false, error: '', items: [] })
      setOperatorDirectoryState({ loading: false, error: '', items: [] })
      setSystemRuntimeState({ loading: false, error: '', runtime: null })
      navigateToPage('sign-in')
    } catch (error) {
      setAuthSessionState((current) => ({ ...current, loading: false, error: error.message, action: '' }))
    }
  }

  async function changeSignedInPassword() {
    setPasswordChangeState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const response = await fetchApi('/api/auth/session/password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          currentPassword: passwordChangeState.form.currentPassword,
          newPassword: passwordChangeState.form.newPassword,
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to change the current password.')

      setAuthSessionState((current) => ({ ...current, session: payload, password: '' }))
      setPasswordChangeState({
        loading: false,
        error: '',
        success: `Password updated for ${payload.username}.`,
        form: createDefaultPasswordChangeForm(),
      })
    } catch (error) {
      setPasswordChangeState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  async function onboardTenant() {
    setTenantOnboardingState({ loading: true, error: '', success: '', result: null })
    try {
      const response = await fetchApi('/api/access/tenants', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tenantCode: tenantOnboardingForm.tenantCode.trim(),
          tenantName: tenantOnboardingForm.tenantName.trim(),
          description: tenantOnboardingForm.description.trim(),
          adminFullName: tenantOnboardingForm.adminFullName.trim(),
          adminUsername: tenantOnboardingForm.adminUsername.trim(),
          adminPassword: tenantOnboardingForm.adminPassword,
          primaryLocation: tenantOnboardingForm.primaryLocation.trim(),
          secondaryLocation: tenantOnboardingForm.secondaryLocation.trim(),
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to onboard the tenant workspace.')

      const tenants = await fetchJson('/api/access/tenants')
      setTenantDirectoryState({ loading: false, error: '', items: tenants })
      setTenantOnboardingState({
        loading: false,
        error: '',
        success: `${payload.tenantName} is ready. Bootstrap admin ${payload.adminUsername} and executive approver ${payload.executiveUsername} were created.`,
        result: payload,
      })
      setAuthSessionState((current) => ({
        ...current,
        tenantCode: payload.tenantCode,
        username: payload.adminUsername,
        password: tenantOnboardingForm.adminPassword,
      }))
      setTenantOnboardingForm(defaultTenantOnboardingForm)
    } catch (error) {
      setTenantOnboardingState({ loading: false, error: error.message, success: '', result: null })
    }
  }

  async function saveTenantOperator() {
    setAccessAdminState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const editing = Boolean(accessOperatorForm.id)
      const response = await fetchApi(editing ? `/api/access/admin/operators/${accessOperatorForm.id}` : '/api/access/admin/operators', {
        method: editing ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          actorName: accessOperatorForm.actorName.trim(),
          displayName: accessOperatorForm.displayName.trim(),
          description: accessOperatorForm.description.trim(),
          active: accessOperatorForm.active,
          roles: parseCsvValues(accessOperatorForm.rolesText).map((role) => role.toUpperCase()),
          warehouseScopes: parseCsvValues(accessOperatorForm.warehouseScopesText).map((scope) => scope.toUpperCase()),
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to create the tenant operator.')

      await fetchAccessAdminData()
      setAccessAdminState((current) => ({
        ...current,
        loading: false,
        error: '',
        success: editing
          ? `Operator ${payload.actorName} was updated and now carries ${payload.roles.length} role lane(s).`
          : `Operator ${payload.actorName} is ready with ${payload.roles.length} role lane(s).`,
      }))
      resetAccessOperatorEditor()
      setAccessUserForm((current) => ({
        ...current,
        operatorActorName: current.id ? current.operatorActorName : resolveDefaultManagedOperator(payload.actorName),
      }))
      const operators = await fetchJson(buildAccessOperatorsPath(activeTenantCode))
      setOperatorDirectoryState({ loading: false, error: '', items: operators })
    } catch (error) {
      setAccessAdminState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  async function saveTenantUser() {
    setAccessAdminState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const editing = Boolean(accessUserForm.id)
      const response = await fetchApi(editing ? `/api/access/admin/users/${accessUserForm.id}` : '/api/access/admin/users', {
        method: editing ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(editing
          ? {
              fullName: accessUserForm.fullName.trim(),
              active: accessUserForm.active,
              operatorActorName: accessUserForm.operatorActorName,
            }
          : {
              username: accessUserForm.username.trim(),
              fullName: accessUserForm.fullName.trim(),
              password: accessUserForm.password,
              operatorActorName: accessUserForm.operatorActorName,
            }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || (editing ? 'Unable to update the tenant user.' : 'Unable to create the tenant user.'))

      await fetchAccessAdminData()
      setAccessAdminState((current) => ({
        ...current,
        loading: false,
        error: '',
        success: editing
          ? `User ${payload.username} now maps to ${payload.operatorActorName} and is ${payload.active ? 'active' : 'inactive'}.`
          : `User ${payload.username} now signs in as ${payload.operatorActorName} and must rotate the issued password at first sign-in.`,
      }))
      resetAccessUserEditor(payload.operatorActorName)
    } catch (error) {
      setAccessAdminState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  async function resetTenantUserPassword() {
    if (!accessUserForm.id) return
    setAccessAdminState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const response = await fetchApi(`/api/access/admin/users/${accessUserForm.id}/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          password: accessUserForm.password,
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.message || 'Unable to reset the tenant user password.')

      await fetchAccessAdminData()
      setAccessAdminState((current) => ({
        ...current,
        loading: false,
        error: '',
        success: `Password reset completed for ${payload.username}. They must rotate it at next sign-in.`,
      }))
      setAccessUserForm((current) => ({ ...current, password: '' }))
    } catch (error) {
      setAccessAdminState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  function renderScenarioEditor(title, form, setter, context) {
    return (
      <article className="scenario-editor-card">
        <div className="planner-line-header">
          <div><p className="panel-kicker">Scenario editor</p><h2>{title}</h2></div>
          <button className="ghost-button" onClick={() => addScenarioLine(setter, form.warehouseCode)} disabled={!context.productOptions.length} type="button">Add Line</button>
        </div>
        <div className="planner-controls">
          <label className="field"><span>Warehouse</span><select value={form.warehouseCode} onChange={(event) => updateScenarioField(setter, 'warehouseCode', event.target.value)}>{warehouseOptions.map((warehouse) => <option key={warehouse.code} value={warehouse.code}>{warehouse.name}</option>)}</select></label>
        </div>
        <div className="planner-lines">
          {context.lines.map((item, index) => (
            <div key={item.id} className="planner-line-card">
              <div className="planner-line-header">
                <strong>Line {index + 1}</strong>
                {form.items.length > 1 ? <button className="planner-remove" onClick={() => removeScenarioLine(setter, item.id)} type="button">Remove</button> : null}
              </div>
              <div className="planner-line-grid">
                <label className="field"><span>Product</span><select value={item.productSku} onChange={(event) => updateScenarioLine(setter, item.id, 'productSku', event.target.value)}>{context.productOptions.map((product) => <option key={product.sku} value={product.sku}>{product.name} ({product.sku})</option>)}</select></label>
                <label className="field"><span>Quantity</span><input type="number" min="1" value={item.quantity} onChange={(event) => updateScenarioLine(setter, item.id, 'quantity', event.target.value)} /></label>
                <label className="field"><span>Unit Price</span><input type="number" min="0.01" step="0.01" value={item.unitPrice} onChange={(event) => updateScenarioLine(setter, item.id, 'unitPrice', event.target.value)} /></label>
              </div>
              {item.selectedProduct ? <p className="muted-text planner-note">Current buffer: {item.selectedProduct.quantityAvailable} available against a threshold of {item.selectedProduct.reorderThreshold}.</p> : null}
            </div>
          ))}
        </div>
      </article>
    )
  }

  function renderLocationsOverview() {
    const warehouseHealthCards = warehouseOptions.map((warehouse) => {
      const warehouseInventory = snapshot.inventory.filter((item) => item.warehouseCode === warehouse.code)
      const lowStockCount = warehouseInventory.filter((item) => item.lowStock).length
      const highRiskCount = warehouseInventory.filter((item) => item.riskLevel === 'critical' || item.riskLevel === 'high').length
      const orderLoad = snapshot.recentOrders.filter((order) => order.warehouseCode === warehouse.code).length
      const fulfillmentLoad = fulfillmentOverview.activeFulfillments.filter((task) => task.warehouseCode === warehouse.code)
      const delayedCount = fulfillmentLoad.filter((task) => task.fulfillmentStatus === 'DELAYED').length
      const healthScore = Math.max(38, 100 - (lowStockCount * 10) - (highRiskCount * 12) - (delayedCount * 12) - (orderLoad * 4))

      return {
        ...warehouse,
        lowStockCount,
        highRiskCount,
        orderLoad,
        backlogCount: fulfillmentLoad.length,
        delayedCount,
        healthScore,
      }
    })
    const selectedWarehouse = [...warehouseHealthCards].sort((left, right) => (right.delayedCount + right.highRiskCount + right.lowStockCount) - (left.delayedCount + left.highRiskCount + left.lowStockCount))[0]
    const selectedWarehouseInventory = selectedWarehouse ? snapshot.inventory.filter((item) => item.warehouseCode === selectedWarehouse.code) : []
    const selectedWarehouseAlerts = selectedWarehouse ? activeAlerts.filter((alert) => alert.warehouseCode === selectedWarehouse.code).slice(0, 4) : []
    const selectedWarehouseRecommendations = selectedWarehouse ? snapshot.recommendations.filter((recommendation) => recommendation.warehouseCode === selectedWarehouse.code).slice(0, 3) : []

    return (
      <section className="content-grid" hidden={!isAuthenticated || !isLocationsPage}>
        <article className="panel panel-wide" id="dashboard-act-now">
          <div className="panel-header">
            <div><p className="panel-kicker">Locations</p><h2>Operational health across sites</h2></div>
            <span className="panel-badge inventory-badge">{warehouseHealthCards.length}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Sites" value={warehouseHealthCards.length} accent="blue" />
            <SummaryCard label="Low-stock lanes" value={warehouseHealthCards.filter((warehouse) => warehouse.lowStockCount).length} accent="amber" />
            <SummaryCard label="Backlog sites" value={warehouseHealthCards.filter((warehouse) => warehouse.backlogCount).length} accent="orange" />
            <SummaryCard label="Delayed sites" value={warehouseHealthCards.filter((warehouse) => warehouse.delayedCount).length} accent="rose" />
          </div>
          <div className="warehouse-grid">
            {warehouseHealthCards.length ? warehouseHealthCards.map((warehouse) => (
              <article key={warehouse.code} className="warehouse-health-card">
                <div className="stack-title-row">
                  <strong>{warehouse.name}</strong>
                  <span className={`status-tag ${warehouse.healthScore >= 80 ? 'status-success' : warehouse.healthScore >= 60 ? 'status-partial' : 'status-failure'}`}>Health {warehouse.healthScore}</span>
                </div>
                <p>{warehouse.code}</p>
                <div className="warehouse-stat-grid">
                  <div><span>Low stock</span><strong>{warehouse.lowStockCount}</strong></div>
                  <div><span>High risk</span><strong>{warehouse.highRiskCount}</strong></div>
                  <div><span>Orders</span><strong>{warehouse.orderLoad}</strong></div>
                  <div><span>Backlog</span><strong>{warehouse.backlogCount}</strong></div>
                </div>
                <p className="muted-text">{warehouse.delayedCount ? `${warehouse.delayedCount} delayed shipment lane${warehouse.delayedCount === 1 ? '' : 's'}.` : 'No delayed fulfillment lanes right now.'}</p>
              </article>
            )) : <div className="empty-state">Warehouse health cards will appear once inventory and fulfillment data are active.</div>}
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>{selectedWarehouse ? `${selectedWarehouse.name} detail` : 'Location detail'}</strong>
                <span className={`status-tag ${selectedWarehouse && selectedWarehouse.healthScore >= 80 ? 'status-success' : selectedWarehouse && selectedWarehouse.healthScore >= 60 ? 'status-partial' : 'status-failure'}`}>
                  {selectedWarehouse ? `Health ${selectedWarehouse.healthScore}` : 'Waiting'}
                </span>
              </div>
              {selectedWarehouse ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedWarehouse.code}</strong>
                    <p>{selectedWarehouse.orderLoad} recent orders | {selectedWarehouse.backlogCount} active fulfillment lanes</p>
                    <p className="muted-text">{selectedWarehouse.lowStockCount} low-stock items | {selectedWarehouse.highRiskCount} high-risk items | {selectedWarehouse.delayedCount} delayed lanes</p>
                  </div>
                  <div className="signal-list-item">
                    <strong>Inventory focus</strong>
                    <p>{selectedWarehouseInventory.filter((item) => item.lowStock).length} low-stock SKUs in this site.</p>
                    <p className="muted-text">{selectedWarehouseInventory.slice(0, 3).map((item) => item.productName).join(', ') || 'Waiting for live inventory mix.'}</p>
                  </div>
                </div>
              ) : <div className="empty-state">Select a location lane to inspect local stock, order flow, and site-level pressure.</div>}
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Site action queue</strong>
                <span className="scenario-type-tag">{(selectedWarehouseAlerts.length + selectedWarehouseRecommendations.length) || 0}</span>
              </div>
              <div className="signal-list">
                {selectedWarehouseAlerts.map((alert) => (
                  <div key={alert.id} className="signal-list-item">
                    <div className="stack-title-row">
                      <strong>{alert.title}</strong>
                      <span className={`severity-tag severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span>
                    </div>
                    <p>{alert.impactSummary}</p>
                    <p className="muted-text">{alert.recommendedAction}</p>
                  </div>
                ))}
                {selectedWarehouseRecommendations.map((recommendation) => (
                  <div key={recommendation.id} className="signal-list-item">
                    <div className="stack-title-row">
                      <strong>{recommendation.title}</strong>
                      <span className={`priority-tag priority-${recommendation.priority.toLowerCase()}`}>{recommendation.priority}</span>
                    </div>
                    <p>{recommendation.description}</p>
                  </div>
                ))}
                {!selectedWarehouseAlerts.length && !selectedWarehouseRecommendations.length ? <div className="empty-state">No location-specific actions are active for the hottest site right now.</div> : null}
              </div>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderDashboardOverview() {
    if (!isAuthenticated || !isDashboardPage) {
      return null
    }

    const warehousePressureCards = warehouseOptions.slice(0, 4).map((warehouse) => {
      const warehouseInventory = snapshot.inventory.filter((item) => item.warehouseCode === warehouse.code)
      const warehouseFulfillment = fulfillmentOverview.activeFulfillments.filter((task) => task.warehouseCode === warehouse.code)
      const alertCount = activeAlerts.filter((alert) => alert.warehouseCode === warehouse.code).length
      return {
        code: warehouse.code,
        name: warehouse.name,
        lowStockCount: warehouseInventory.filter((item) => item.lowStock).length,
        backlogCount: warehouseFulfillment.length,
        delayedCount: warehouseFulfillment.filter((task) => task.fulfillmentStatus === 'DELAYED').length,
        alertCount,
      }
    })

    return (
      <section className="content-grid dashboard-command-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Act now</p><h2>Urgent operational actions</h2></div>
            <span className="panel-badge recommendation-badge">{urgentActions.length}</span>
          </div>
          <div className="act-now-grid">
            {urgentActions.length ? urgentActions.map((action) => (
              <button key={action.id} className="stack-card action-card" onClick={() => navigateToPage(action.target)} type="button">
                <div className="stack-title-row">
                  <strong>{action.title}</strong>
                  <span className="scenario-type-tag">{action.kicker}</span>
                </div>
                <p>{action.note}</p>
              </button>
            )) : <div className="empty-state">No urgent actions are waiting right now. SynapseCore is still watching live state and risk drift.</div>}
          </div>
        </article>
        <article className="panel" id="dashboard-live-state">
          <div className="panel-header">
            <div><p className="panel-kicker">Risk heat</p><h2>Pressure by location</h2></div>
            <span className="panel-badge alert-badge">{warehousePressureCards.length}</span>
          </div>
          <div className="stack-list">
            {warehousePressureCards.length ? warehousePressureCards.map((warehouse) => (
              <div key={warehouse.code} className="stack-card">
                <div className="stack-title-row">
                  <strong>{warehouse.name}</strong>
                  <span className={`status-tag ${warehouse.delayedCount || warehouse.alertCount ? 'status-failure' : warehouse.lowStockCount || warehouse.backlogCount ? 'status-partial' : 'status-success'}`}>
                    {warehouse.delayedCount || warehouse.alertCount ? 'Hot' : warehouse.lowStockCount || warehouse.backlogCount ? 'Watch' : 'Stable'}
                  </span>
                </div>
                <p>{warehouse.code}</p>
                <p className="muted-text">{warehouse.alertCount} alerts | {warehouse.lowStockCount} low stock | {warehouse.backlogCount} backlog | {warehouse.delayedCount} delayed</p>
              </div>
            )) : <div className="empty-state">Warehouse heat will appear as soon as inventory and fulfillment signals are active.</div>}
          </div>
        </article>
      </section>
    )
  }

  function renderAlertsCenter() {
    if (!isAuthenticated || !isAlertsPage) {
      return null
    }

    const selectedAlert = activeAlerts.find((alert) => alert.id === selectedAlertId) || activeAlerts[0]
    const criticalAlertCount = activeAlerts.filter((alert) => alert.severity === 'CRITICAL').length
    const highAlertCount = activeAlerts.filter((alert) => alert.severity === 'HIGH').length

    return (
      <section className="content-grid alerts-center-grid">
        <article className="panel panel-wide" id="alerts-feed">
          <div className="panel-header">
            <div><p className="panel-kicker">Alerts center</p><h2>Operational warnings in one lane</h2></div>
            <span className="panel-badge alert-badge">{activeAlerts.length}</span>
          </div>
          <div className="filter-chip-row">
            <span className="scenario-type-tag">Severity</span>
            <span className="scenario-type-tag">Warehouse</span>
            <span className="scenario-type-tag">Type</span>
            <span className="scenario-type-tag">Status</span>
            <span className="scenario-type-tag">Search</span>
          </div>
          <div className="stack-list">
            {activeAlerts.length ? activeAlerts.map((alert) => (
              <button
                key={alert.id}
                className={`stack-card selectable-card ${selectedAlert?.id === alert.id ? 'is-selected' : ''}`}
                onClick={() => setSelectedAlertId(alert.id)}
                type="button"
              >
                <div className="stack-title-row">
                  <strong>{alert.title}</strong>
                  <div className="stack-tag-row">
                    <span className={`severity-tag severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span>
                    {alert.warehouseCode ? <span className="scenario-type-tag">{alert.warehouseCode}</span> : null}
                  </div>
                </div>
                <p>{alert.description}</p>
                <p className="muted-text">{alert.impactSummary}</p>
                <p className="action-line">Recommended action: {alert.recommendedAction}</p>
              </button>
            )) : <div className="empty-state">No active alerts. This page becomes the command lane when operational risk starts forming.</div>}
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card" id="alerts-response">
              <div className="stack-title-row">
                <strong>Severity posture</strong>
                <span className="scenario-type-tag">{activeAlerts.length}</span>
              </div>
              <div className="utility-metric-grid">
                <div><span>Critical</span><strong>{criticalAlertCount}</strong></div>
                <div><span>High</span><strong>{highAlertCount}</strong></div>
                <div><span>Warehouses hit</span><strong>{new Set(activeAlerts.map((alert) => alert.warehouseCode).filter(Boolean)).size}</strong></div>
                <div><span>Actionable</span><strong>{activeAlerts.filter((alert) => Boolean(alert.recommendedAction)).length}</strong></div>
              </div>
              <p className="muted-text">Use severity, warehouse, and actionability together to decide what the team needs to resolve first.</p>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Alert response model</strong>
                <span className="scenario-type-tag">Action-first</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>What is wrong</strong>
                  <p>Alerts explain the operational issue, the affected lane, and the likely impact before teams need to drill further.</p>
                </div>
                <div className="signal-list-item">
                  <strong>What to do next</strong>
                  <p>Every strong alert should point the team toward the next action, whether that is replenishment, transfer, replay, escalation, or approval.</p>
                </div>
              </div>
            </article>
          </div>
        </article>
        <article className="panel">
          <div className="panel-header">
            <div><p className="panel-kicker">Selected alert</p><h2>Impact and next step</h2></div>
            <span className="panel-badge notification-badge">{selectedAlert ? selectedAlert.severity : 'Clear'}</span>
          </div>
          {selectedAlert ? (
            <div className="stack-list">
              <div className="stack-card">
                <div className="stack-title-row">
                  <strong>{selectedAlert.title}</strong>
                  <span className={`severity-tag severity-${selectedAlert.severity.toLowerCase()}`}>{selectedAlert.severity}</span>
                </div>
                <p>{selectedAlert.description}</p>
                <p className="muted-text">{selectedAlert.impactSummary}</p>
                <p className="muted-text">Warehouse {selectedAlert.warehouseCode || 'Tenant-wide'} | {formatTimestamp(selectedAlert.createdAt)}</p>
                <p className="action-line">Action: {selectedAlert.recommendedAction}</p>
              </div>
            </div>
          ) : <div className="empty-state">Select an alert from the list to review the likely impact and response path.</div>}
        </article>
      </section>
    )
  }

  function renderRecommendationsCenter() {
    if (!isAuthenticated || !isRecommendationsPage) {
      return null
    }

    const recommendationCandidates = [...recommendationNow, ...recommendationSoon, ...recommendationWatch]
    const columns = [
      { title: 'Urgent now', items: recommendationNow, tone: 'priority-high' },
      { title: 'Important soon', items: recommendationSoon, tone: 'priority-medium' },
      { title: 'Watch', items: recommendationWatch, tone: 'priority-low' },
    ]
    const selectedRecommendation = recommendationCandidates.find((recommendation) => recommendation.id === selectedRecommendationId) || recommendationCandidates[0]

    return (
      <section className="content-grid">
        <article className="panel panel-wide" id="recommendations-lanes">
          <div className="panel-header">
            <div><p className="panel-kicker">Recommendations center</p><h2>Ranked action queue for operators</h2></div>
            <span className="panel-badge recommendation-badge">{snapshot.recommendations.length}</span>
          </div>
          <div className="recommendation-board">
            {columns.map((column) => (
              <article key={column.title} className="recommendation-column">
                <div className="stack-title-row">
                  <strong>{column.title}</strong>
                  <span className={`status-tag ${column.tone}`}>{column.items.length}</span>
                </div>
                <div className="stack-list compact-stack-list">
                  {column.items.length ? column.items.map((recommendation) => (
                    <button
                      key={recommendation.id}
                      className={`stack-card selectable-card ${selectedRecommendation?.id === recommendation.id ? 'is-selected' : ''}`}
                      onClick={() => setSelectedRecommendationId(recommendation.id)}
                      type="button"
                    >
                      <div className="stack-title-row">
                        <strong>{recommendation.title}</strong>
                        <span className={`priority-tag priority-${recommendation.priority.toLowerCase()}`}>{recommendation.priority}</span>
                      </div>
                      <p>{recommendation.description}</p>
                      <p className="muted-text">{formatTimestamp(recommendation.createdAt)}</p>
                    </button>
                  )) : <div className="empty-state">No items in this action lane.</div>}
                </div>
              </article>
            ))}
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card" id="recommendations-focus">
              <div className="stack-title-row">
                <strong>Selected recommendation</strong>
                <span className="scenario-type-tag">{selectedRecommendation ? selectedRecommendation.priority : 'Clear'}</span>
              </div>
              {selectedRecommendation ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedRecommendation.title}</strong>
                    <p>{selectedRecommendation.description}</p>
                    <p className="muted-text">Created {formatTimestamp(selectedRecommendation.createdAt)}</p>
                  </div>
                </div>
              ) : <div className="empty-state">When the system has action guidance, the leading recommendation appears here with its operating context.</div>}
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Recommendation posture</strong>
                <span className="scenario-type-tag">{snapshot.recommendations.length}</span>
              </div>
              <div className="utility-metric-grid">
                <div><span>Urgent now</span><strong>{recommendationNow.length}</strong></div>
                <div><span>Important soon</span><strong>{recommendationSoon.length}</strong></div>
                <div><span>Watch</span><strong>{recommendationWatch.length}</strong></div>
                <div><span>Action lanes</span><strong>{new Set(snapshot.recommendations.map((item) => item.warehouseCode).filter(Boolean)).size || 'All'}</strong></div>
              </div>
              <p className="muted-text">This page exists to help the team act faster, not just review data. The best items should feel immediately executable.</p>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderOrdersCenter() {
    if (!isAuthenticated || !isOrdersPage) {
      return null
    }

    const orderCards = snapshot.recentOrders.map((order) => {
      const relatedFulfillment = fulfillmentOverview.activeFulfillments.find((task) => task.externalOrderId === order.externalOrderId)
      return {
        ...order,
        relatedFulfillment,
      }
    })
    const selectedOrder = orderCards.find((order) => order.id === selectedOrderId) || orderCards[0]
    const loadedWarehouses = new Set(orderCards.map((order) => order.warehouseCode).filter(Boolean)).size

    return (
      <section className="content-grid orders-center-grid">
        <article className="panel panel-wide" id="orders-stream">
          <div className="panel-header">
            <div><p className="panel-kicker">Orders operations</p><h2>Monitor the live order stream</h2></div>
            <span className="panel-badge order-badge">{orderCards.length}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Recent orders" value={summary?.recentOrderCount ?? orderCards.length} accent="amber" />
            <SummaryCard label="Delayed linked lanes" value={orderCards.filter((order) => order.relatedFulfillment?.fulfillmentStatus === 'DELAYED').length} accent="rose" />
            <SummaryCard label="Backlog-linked orders" value={orderCards.filter((order) => order.relatedFulfillment).length} accent="orange" />
            <SummaryCard label="Warehouses under flow" value={warehouseOptions.length} accent="blue" />
          </div>
          <div className="stack-list">
            {orderCards.length ? orderCards.map((order) => (
              <button
                key={order.id}
                className={`stack-card selectable-card ${selectedOrder?.id === order.id ? 'is-selected' : ''}`}
                onClick={() => setSelectedOrderId(order.id)}
                type="button"
              >
                <div className="stack-title-row">
                  <strong>{order.externalOrderId}</strong>
                  <span className="order-total">{currency.format(order.totalAmount)}</span>
                </div>
                <p>{order.warehouseName} | {order.itemCount} units</p>
                <p className="muted-text">{formatTimestamp(order.createdAt)}</p>
                {order.relatedFulfillment ? <p className="muted-text">Fulfillment {formatCodeLabel(order.relatedFulfillment.fulfillmentStatus)} | Dispatch due {formatRelativeHours(order.relatedFulfillment.hoursUntilDispatchDue)}</p> : <p className="muted-text">Awaiting fulfillment lane linkage.</p>}
              </button>
            )) : <div className="empty-state">No recent orders are visible yet. As order events arrive, this page becomes the live operational queue.</div>}
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card" id="orders-focus">
              <div className="stack-title-row">
                <strong>Selected order lane</strong>
                <span className="scenario-type-tag">{selectedOrder ? selectedOrder.warehouseCode : 'Waiting'}</span>
              </div>
              {selectedOrder ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedOrder.externalOrderId}</strong>
                    <p>{selectedOrder.warehouseName} | {selectedOrder.itemCount} units | {currency.format(selectedOrder.totalAmount)}</p>
                    <p className="muted-text">Created {formatTimestamp(selectedOrder.createdAt)}</p>
                    <p className="muted-text">{selectedOrder.relatedFulfillment ? `Fulfillment ${formatCodeLabel(selectedOrder.relatedFulfillment.fulfillmentStatus)} | Delivery due ${formatRelativeHours(selectedOrder.relatedFulfillment.hoursUntilDeliveryDue)}` : 'Fulfillment lane is still being linked.'}</p>
                  </div>
                </div>
              ) : <div className="empty-state">As order events arrive, the lead order lane appears here with fulfillment impact and SLA posture.</div>}
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Order flow posture</strong>
                <span className="scenario-type-tag">{orderCards.length}</span>
              </div>
              <div className="utility-metric-grid">
                <div><span>Warehouses under flow</span><strong>{loadedWarehouses}</strong></div>
                <div><span>Linked fulfillment</span><strong>{orderCards.filter((order) => order.relatedFulfillment).length}</strong></div>
                <div><span>Delayed lanes</span><strong>{orderCards.filter((order) => order.relatedFulfillment?.fulfillmentStatus === 'DELAYED').length}</strong></div>
                <div><span>High value</span><strong>{orderCards.filter((order) => order.totalAmount >= 500).length}</strong></div>
              </div>
              <p className="muted-text">This lane helps operators understand which order flow is driving downstream inventory, fulfillment, and alert pressure.</p>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderInventoryCenter() {
    if (!isAuthenticated || !isInventoryPage) {
      return null
    }

    const selectedInventoryItem = snapshot.inventory.find((item) => item.id === selectedInventoryId) || highRiskInventory[0] || lowStockInventory[0] || snapshot.inventory[0]

    return (
      <section className="content-grid inventory-intelligence-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Inventory intelligence</p><h2>Stock posture, velocity, and risk</h2></div>
            <span className="panel-badge inventory-badge">{snapshot.inventory.length}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Low stock" value={lowStockInventory.length} accent="orange" />
            <SummaryCard label="High risk" value={highRiskInventory.length} accent="rose" />
            <SummaryCard label="Fast movers" value={fastMovingInventory.length} accent="teal" />
            <SummaryCard label="Warehouses" value={warehouseOptions.length} accent="blue" />
          </div>
          <div className="inventory-spotlight-grid" id="inventory-spotlight">
            <article className="stack-card">
              <div className="stack-title-row"><strong>Low-stock focus</strong><span className="status-tag status-failure">{lowStockInventory.length}</span></div>
              <div className="stack-list compact-stack-list">
                {lowStockInventory.slice(0, 5).map((item) => (
                  <button
                    key={item.id}
                    className={`stack-card stack-card-compact selectable-card ${selectedInventoryItem?.id === item.id ? 'is-selected' : ''}`}
                    onClick={() => setSelectedInventoryId(item.id)}
                    type="button"
                  >
                    <strong>{item.productName}</strong>
                    <p>{item.warehouseName}</p>
                    <p className="muted-text">{item.quantityAvailable} available | Threshold {item.reorderThreshold} | Stockout {formatRelativeHours(item.hoursToStockout)}</p>
                  </button>
                ))}
                {!lowStockInventory.length ? <div className="empty-state">No low-stock items right now.</div> : null}
              </div>
            </article>
            <article className="stack-card">
              <div className="stack-title-row"><strong>Fast-moving items</strong><span className="status-tag status-partial">{fastMovingInventory.length}</span></div>
              <div className="stack-list compact-stack-list">
                {fastMovingInventory.map((item) => (
                  <button
                    key={item.id}
                    className={`stack-card stack-card-compact selectable-card ${selectedInventoryItem?.id === item.id ? 'is-selected' : ''}`}
                    onClick={() => setSelectedInventoryId(item.id)}
                    type="button"
                  >
                    <strong>{item.productName}</strong>
                    <p>{item.warehouseName}</p>
                    <p className="muted-text">{(item.unitsPerHour || 0).toFixed(1)} units/hr | Risk {formatCodeLabel(item.riskLevel)}</p>
                  </button>
                ))}
                {!fastMovingInventory.length ? <div className="empty-state">Velocity metrics will appear as order demand accumulates.</div> : null}
              </div>
            </article>
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card" id="inventory-focus">
              <div className="stack-title-row">
                <strong>Selected inventory lane</strong>
                <span className={`status-tag ${selectedInventoryItem ? `risk-${selectedInventoryItem.riskLevel}` : 'status-partial'}`}>{selectedInventoryItem ? formatCodeLabel(selectedInventoryItem.riskLevel) : 'Waiting'}</span>
              </div>
              {selectedInventoryItem ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedInventoryItem.productName}</strong>
                    <p>{selectedInventoryItem.warehouseName} | {selectedInventoryItem.productSku}</p>
                    <p className="muted-text">{selectedInventoryItem.quantityAvailable} available | Threshold {selectedInventoryItem.reorderThreshold} | Velocity {(selectedInventoryItem.unitsPerHour || 0).toFixed(1)} units/hr</p>
                    <p className="muted-text">Stockout forecast {formatRelativeHours(selectedInventoryItem.hoursToStockout)}</p>
                  </div>
                </div>
              ) : <div className="empty-state">The most pressured inventory lane appears here so teams can understand risk without scanning every row first.</div>}
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Inventory operating posture</strong>
                <span className="scenario-type-tag">{snapshot.inventory.length}</span>
              </div>
              <div className="utility-metric-grid">
                <div><span>Low stock</span><strong>{lowStockInventory.length}</strong></div>
                <div><span>Critical + high</span><strong>{highRiskInventory.length}</strong></div>
                <div><span>Fast movers</span><strong>{fastMovingInventory.length}</strong></div>
                <div><span>Warehouses</span><strong>{warehouseOptions.length}</strong></div>
              </div>
              <p className="muted-text">This page should feel like the inventory brain of the product: thresholds, velocity, risk, and recommended response all in one place.</p>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderFulfillmentCenter() {
    if (!isAuthenticated || !isFulfillmentPage) {
      return null
    }

    const selectedFulfillment = delayedFulfillments[0] || fulfillmentOverview.activeFulfillments[0]
    const lanePressure = warehouseOptions.map((warehouse) => {
      const tasks = fulfillmentOverview.activeFulfillments.filter((task) => task.warehouseCode === warehouse.code)
      return {
        code: warehouse.code,
        name: warehouse.name,
        total: tasks.length,
        delayed: tasks.filter((task) => task.fulfillmentStatus === 'DELAYED').length,
        exceptions: tasks.filter((task) => task.fulfillmentStatus === 'EXCEPTION').length,
        dispatched: tasks.filter((task) => task.fulfillmentStatus === 'DISPATCHED').length,
      }
    }).filter((lane) => lane.total)

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Fulfillment and logistics</p><h2>Backlog, dispatch, and delivery pressure</h2></div>
            <span className="panel-badge fulfillment-badge">{fulfillmentOverview.activeFulfillments.length}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Backlog" value={fulfillmentOverview.backlogCount} accent="amber" />
            <SummaryCard label="Overdue dispatch" value={fulfillmentOverview.overdueDispatchCount} accent="orange" />
            <SummaryCard label="Delayed shipments" value={fulfillmentOverview.delayedShipmentCount} accent="rose" />
            <SummaryCard label="At risk" value={fulfillmentOverview.atRiskCount} accent="teal" />
          </div>
          <div className="warehouse-grid">
            {delayedFulfillments.length ? delayedFulfillments.map((task) => (
              <article key={task.id} className="warehouse-health-card">
                <div className="stack-title-row">
                  <strong>{task.externalOrderId}</strong>
                  <span className={`status-tag ${getFulfillmentStatusClassName(task.fulfillmentStatus)}`}>{formatCodeLabel(task.fulfillmentStatus)}</span>
                </div>
                <p>{task.warehouseName}</p>
                <p className="muted-text">Dispatch due {formatRelativeHours(task.hoursUntilDispatchDue)} | Delivery {formatRelativeHours(task.hoursUntilDeliveryDue)}</p>
                <p>{task.impactSummary}</p>
              </article>
            )) : <div className="empty-state">No delayed or high-risk fulfillment lanes right now.</div>}
          </div>
          <div className="experience-grid experience-grid-three">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Lane pressure</strong>
                <span className="scenario-type-tag">{lanePressure.length}</span>
              </div>
              <div className="signal-list">
                {lanePressure.length ? lanePressure.map((lane) => (
                  <div key={lane.code} className="signal-list-item">
                    <div className="stack-title-row">
                      <strong>{lane.name}</strong>
                      <span className={`status-tag ${lane.delayed || lane.exceptions ? 'status-failure' : lane.total ? 'status-partial' : 'status-success'}`}>
                        {lane.delayed || lane.exceptions ? 'Pressed' : 'Flowing'}
                      </span>
                    </div>
                    <p>{lane.total} active lanes | {lane.dispatched} dispatched</p>
                    <p className="muted-text">{lane.delayed} delayed | {lane.exceptions} exceptions</p>
                  </div>
                )) : <div className="empty-state">Fulfillment lane pressure will appear once dispatch and delivery activity is flowing.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Selected fulfillment detail</strong>
                <span className={`status-tag ${selectedFulfillment ? getFulfillmentStatusClassName(selectedFulfillment.fulfillmentStatus) : 'status-partial'}`}>
                  {selectedFulfillment ? formatCodeLabel(selectedFulfillment.fulfillmentStatus) : 'Waiting'}
                </span>
              </div>
              {selectedFulfillment ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedFulfillment.externalOrderId}</strong>
                    <p>{selectedFulfillment.warehouseName}</p>
                    <p className="muted-text">Dispatch due {formatRelativeHours(selectedFulfillment.hoursUntilDispatchDue)} | Delivery due {formatRelativeHours(selectedFulfillment.hoursUntilDeliveryDue)}</p>
                    <p className="muted-text">{selectedFulfillment.impactSummary}</p>
                  </div>
                </div>
              ) : <div className="empty-state">Select a fulfillment lane to inspect dispatch timing, delivery pressure, and the likely operational impact.</div>}
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Delivery support posture</strong>
                <span className="scenario-type-tag">{enabledConnectorCount}/{snapshot.integrationConnectors.length || 0} live</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Connector support</strong>
                  <p>{enabledConnectorCount} enabled connectors supporting inbound operational flow.</p>
                  <p className="muted-text">{pendingReplayCount} replay item{pendingReplayCount === 1 ? '' : 's'} waiting for recovery.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Fulfillment posture</strong>
                  <p>{fulfillmentOverview.delayedShipmentCount} delayed shipments | {fulfillmentOverview.overdueDispatchCount} overdue dispatch lanes</p>
                  <p className="muted-text">Use the recommendations and replay lanes to recover delivery pressure before it spreads.</p>
                </div>
              </div>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderScenarioControlOverview() {
    if (!isAuthenticated || !(isScenariosPage || isScenarioHistoryPage || isApprovalsPage || isEscalationsPage)) {
      return null
    }

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Control center</p><h2>Planning, approvals, and escalations</h2></div>
            <span className="panel-badge scenario-badge">{scenarioHistoryItems.length}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Pending approvals" value={pendingApprovalScenarios.length} accent="amber" />
            <SummaryCard label="Approved" value={approvedScenarios.length} accent="teal" />
            <SummaryCard label="Rejected" value={rejectedScenarios.length} accent="rose" />
            <SummaryCard label="Overdue" value={overdueScenarios.length} accent="orange" />
          </div>
          <div className="approval-board">
            {approvalBoard.length ? approvalBoard.map((scenario) => (
              <div key={scenario.id} className="stack-card">
                <div className="stack-title-row">
                  <strong>{scenario.title}</strong>
                  <div className="stack-tag-row">
                    {scenario.reviewPriority ? <span className={`priority-tag priority-${scenario.reviewPriority.toLowerCase()}`}>{formatCodeLabel(scenario.reviewPriority)}</span> : null}
                    {scenario.overdue ? <span className="status-tag status-failure">Overdue</span> : null}
                  </div>
                </div>
                <p>{scenario.summary}</p>
                <p className="muted-text">{scenario.warehouseCode || 'Tenant-wide'} | {formatCodeLabel(scenario.approvalStage || 'not_required')} | Review owner {scenario.reviewOwner || 'Unassigned'}</p>
              </div>
            )) : <div className="empty-state">Saved plans and approval queues will appear here once operators start routing decisions.</div>}
          </div>
        </article>
      </section>
    )
  }

  function renderScenarioDecisionConsole(scenario, title, emptyMessage) {
    if (!scenario) {
      return (
        <article className="stack-card section-card">
          <div className="stack-title-row">
            <strong>{title}</strong>
            <span className="scenario-type-tag">Waiting</span>
          </div>
          <div className="empty-state">{emptyMessage}</div>
        </article>
      )
    }

    const approvalRole = getScenarioApprovalRole(scenario)
    const rejectionRole = getScenarioRejectionRole(scenario)
    const approvalActionLabel = scenario.approvalPolicy === 'ESCALATED' && scenario.approvalStage === 'PENDING_REVIEW'
      ? 'Owner Review'
      : scenario.approvalPolicy === 'ESCALATED'
        ? 'Final Approve'
        : 'Approve Plan'
    const approvalNoteRequired = scenario.approvalPolicy === 'ESCALATED' || scenario.approvalStage === 'PENDING_FINAL_APPROVAL'
    const canLoadScenario = Boolean(scenario.loadable)
    const canApproveScenario = scenario.type === 'SAVED_PLAN' && scenario.approvalStatus === 'PENDING_APPROVAL'
    const canRejectScenario = scenario.type === 'SAVED_PLAN' && scenario.approvalStatus !== 'REJECTED'
    const canExecuteScenario = Boolean(scenario.executable)
    const canAcknowledgeEscalation = Boolean(scenario.slaEscalated && !scenario.slaAcknowledged)
    const approvalDisabled = scenarioApprovalState.loadingId === scenario.id
      || !signedInSession
      || (approvalNoteRequired && !scenarioReviewNote.trim())
      || scenarioActorRole !== approvalRole
      || !signedInRoles.includes(approvalRole)
      || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)
    const rejectionDisabled = scenarioRejectionState.loadingId === scenario.id
      || !signedInSession
      || !scenarioReviewNote.trim()
      || scenarioActorRole !== rejectionRole
      || !signedInRoles.includes(rejectionRole)
      || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)
    const escalationDisabled = scenarioEscalationAckState.loadingId === scenario.id
      || !signedInSession
      || !scenarioReviewNote.trim()
      || scenarioActorRole !== 'ESCALATION_OWNER'
      || !signedInRoles.includes('ESCALATION_OWNER')
      || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)

    return (
      <article className="stack-card section-card">
        <div className="stack-title-row">
          <strong>{title}</strong>
          <span className="scenario-type-tag">{formatCodeLabel(scenario.approvalStatus || scenario.type)}</span>
        </div>
        <div className="signal-list">
          <div className="signal-list-item">
            <strong>{scenario.title}</strong>
            <p>{scenario.summary}</p>
            <p className="muted-text">
              {scenario.warehouseCode ? `${scenario.warehouseCode} | ` : ''}
              {formatCodeLabel(scenario.type)}
              {scenario.reviewPriority ? ` | ${formatCodeLabel(scenario.reviewPriority)} priority` : ''}
            </p>
            <p className="muted-text">
              {scenario.requestedBy ? `Requested by ${scenario.requestedBy}` : 'Requester pending'}
              {scenario.reviewOwner ? ` | Review owner ${scenario.reviewOwner}` : ''}
              {scenario.finalApprovalOwner ? ` | Final approver ${scenario.finalApprovalOwner}` : ''}
            </p>
            <p className="muted-text">
              Approval needs {formatCodeLabel(approvalRole)} | Rejection needs {formatCodeLabel(rejectionRole)}
              {scenario.approvalDueAt ? ` | Due ${formatTimestamp(scenario.approvalDueAt)}` : ''}
            </p>
          </div>
        </div>
        <div className="session-control-row">
          <label className="field planner-name-field">
            <span>Acting As</span>
            <select value={scenarioActorRole} onChange={(event) => setScenarioActorRole(event.target.value)}>
              {scenarioActorRoles.map((role) => <option key={role} value={role}>{formatCodeLabel(role)}</option>)}
            </select>
          </label>
          <label className="field planner-name-field">
            <span>Decision Note</span>
            <input
              type="text"
              maxLength="240"
              placeholder={canAcknowledgeEscalation ? 'Required to acknowledge the escalation' : approvalNoteRequired ? 'Recommended for final approval; required for rejection' : 'Required for rejection'}
              value={scenarioReviewNote}
              onChange={(event) => setScenarioReviewNote(event.target.value)}
            />
          </label>
        </div>
        {scenarioLoadState.error ? <p className="error-text">{scenarioLoadState.error}</p> : null}
        {scenarioLoadState.success ? <p className="success-text">{scenarioLoadState.success}</p> : null}
        {scenarioApprovalState.error ? <p className="error-text">{scenarioApprovalState.error}</p> : null}
        {scenarioApprovalState.success ? <p className="success-text">{scenarioApprovalState.success}</p> : null}
        {scenarioRejectionState.error ? <p className="error-text">{scenarioRejectionState.error}</p> : null}
        {scenarioRejectionState.success ? <p className="success-text">{scenarioRejectionState.success}</p> : null}
        {scenarioExecutionState.error ? <p className="error-text">{scenarioExecutionState.error}</p> : null}
        {scenarioExecutionState.success ? <p className="success-text">{scenarioExecutionState.success}</p> : null}
        {scenarioEscalationAckState.error ? <p className="error-text">{scenarioEscalationAckState.error}</p> : null}
        {scenarioEscalationAckState.success ? <p className="success-text">{scenarioEscalationAckState.success}</p> : null}
        <div className="history-action-row">
          {canLoadScenario ? (
            <button
              className="ghost-button"
              onClick={() => loadScenarioIntoPlanner(scenario.id)}
              disabled={scenarioLoadState.loadingId === scenario.id}
              type="button"
            >
              {scenarioLoadState.loadingId === scenario.id ? 'Loading...' : 'Load Into Planner'}
            </button>
          ) : null}
          {canApproveScenario ? (
            <button
              className="approve-button"
              onClick={() => approveScenarioPlan(scenario.id)}
              disabled={approvalDisabled}
              type="button"
            >
              {scenarioApprovalState.loadingId === scenario.id ? 'Approving...' : approvalActionLabel}
            </button>
          ) : null}
          {canRejectScenario ? (
            <button
              className="reject-button"
              onClick={() => rejectScenarioPlan(scenario.id)}
              disabled={rejectionDisabled}
              type="button"
            >
              {scenarioRejectionState.loadingId === scenario.id ? 'Rejecting...' : 'Reject Plan'}
            </button>
          ) : null}
          {canExecuteScenario ? (
            <button
              className="secondary-button"
              onClick={() => executeScenario(scenario.id)}
              disabled={scenarioExecutionState.loadingId === scenario.id}
              type="button"
            >
              {scenarioExecutionState.loadingId === scenario.id ? 'Executing...' : 'Execute Scenario'}
            </button>
          ) : null}
          {canAcknowledgeEscalation ? (
            <button
              className="approve-button"
              onClick={() => acknowledgeScenarioEscalation(scenario.id)}
              disabled={escalationDisabled}
              type="button"
            >
              {scenarioEscalationAckState.loadingId === scenario.id ? 'Acknowledging...' : 'Acknowledge Escalation'}
            </button>
          ) : null}
        </div>
        {!canLoadScenario && !canApproveScenario && !canRejectScenario && !canExecuteScenario && !canAcknowledgeEscalation ? (
          <p className="muted-text">This scenario is visible for traceability and comparison, but it does not need another live action right now.</p>
        ) : null}
      </article>
    )
  }

  function renderScenarioHistoryExperience() {
    if (!isAuthenticated || !isScenarioHistoryPage) {
      return null
    }

    const executableScenarios = scenarioHistoryItems.filter((scenario) => scenario.executable).slice(0, 4)
    const revisionScenarios = scenarioHistoryItems.filter((scenario) => scenario.revisionNumber).slice(0, 4)

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Scenario history</p><h2>Saved plans, revisions, and compare posture</h2></div>
            <span className="panel-badge scenario-badge">{scenarioHistoryItems.length}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Saved plans" value={scenarioHistoryItems.filter((scenario) => scenario.type === 'SAVED_PLAN').length} accent="blue" />
            <SummaryCard label="Comparisons" value={scenarioHistoryItems.filter((scenario) => scenario.type === 'COMPARISON').length} accent="teal" />
            <SummaryCard label="Revisions" value={scenarioHistoryItems.filter((scenario) => scenario.revisionNumber).length} accent="amber" />
            <SummaryCard label="Executable" value={scenarioHistoryItems.filter((scenario) => scenario.executable).length} accent="orange" />
          </div>
          <div className="approval-board">
            {scenarioHistoryItems.slice(0, 6).map((scenario) => (
              <button
                key={scenario.id}
                className={`stack-card selectable-card ${selectedHistoryScenario?.id === scenario.id ? 'is-selected' : ''}`}
                onClick={() => setSelectedScenarioId(scenario.id)}
                type="button"
              >
                <div className="stack-title-row">
                  <strong>{scenario.title}</strong>
                  <div className="stack-tag-row">
                    <span className="scenario-type-tag">{formatCodeLabel(scenario.type)}</span>
                    {scenario.revisionNumber ? <span className="status-tag status-partial">Rev {scenario.revisionNumber}</span> : null}
                  </div>
                </div>
                <p>{scenario.summary}</p>
                <p className="muted-text">{scenario.recommendedOption || 'No recommended option'} | {formatTimestamp(scenario.createdAt)}</p>
              </button>
            ))}
            {!scenarioHistoryItems.length ? <div className="empty-state">Scenario history will fill up after planners start previewing and saving alternative operating paths.</div> : null}
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Selected scenario memory</strong>
                <span className="scenario-type-tag">{selectedHistoryScenario ? formatCodeLabel(selectedHistoryScenario.type) : 'Waiting'}</span>
              </div>
              {selectedHistoryScenario ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedHistoryScenario.title}</strong>
                    <p>{selectedHistoryScenario.summary}</p>
                    <p className="muted-text">
                      {selectedHistoryScenario.warehouseCode ? `${selectedHistoryScenario.warehouseCode} | ` : ''}
                      {selectedHistoryScenario.recommendedOption || 'No recommended option'}
                    </p>
                    <p className="muted-text">
                      Approval {formatCodeLabel(selectedHistoryScenario.approvalStatus)}
                      {selectedHistoryScenario.approvalPolicy ? ` | Policy ${formatCodeLabel(selectedHistoryScenario.approvalPolicy)}` : ''}
                      {selectedHistoryScenario.approvalStage ? ` | Stage ${formatCodeLabel(selectedHistoryScenario.approvalStage)}` : ''}
                    </p>
                    <p className="muted-text">
                      {selectedHistoryScenario.requestedBy ? `Requested by ${selectedHistoryScenario.requestedBy}` : 'Requester pending'}
                      {selectedHistoryScenario.reviewOwner ? ` | Review owner ${selectedHistoryScenario.reviewOwner}` : ''}
                      {selectedHistoryScenario.finalApprovalOwner ? ` | Final approver ${selectedHistoryScenario.finalApprovalOwner}` : ''}
                    </p>
                    {selectedHistoryScenario.approvalDueAt ? <p className={`muted-text${selectedHistoryScenario.overdue ? ' overdue-text' : ''}`}>Due {formatTimestamp(selectedHistoryScenario.approvalDueAt)}</p> : null}
                    {selectedHistoryScenario.revisionNumber ? <p className="muted-text">Revision {selectedHistoryScenario.revisionNumber}{selectedHistoryScenario.revisionOfScenarioRunId ? ` | Based on ${selectedHistoryScenario.revisionOfScenarioRunId}` : ''}</p> : null}
                    {selectedHistoryScenario.approvalNote ? <p className="muted-text">Approval note: {selectedHistoryScenario.approvalNote}</p> : null}
                    {selectedHistoryScenario.rejectionReason ? <p className="muted-text">Review note: {selectedHistoryScenario.rejectionReason}</p> : null}
                  </div>
                </div>
              ) : <div className="empty-state">Select a saved plan or revision to inspect its decision memory and next action posture.</div>}
            </article>
            {renderScenarioDecisionConsole(selectedHistoryScenario, 'Scenario action console', 'Choose a scenario run to load it into the planner, approve it, reject it, or push it into execution.')}
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Execution-ready plans</strong>
                <span className="scenario-type-tag">{executableScenarios.length}</span>
              </div>
              <div className="signal-list">
                {executableScenarios.length ? executableScenarios.map((scenario) => (
                  <div key={scenario.id} className="signal-list-item">
                    <strong>{scenario.title}</strong>
                    <p>{scenario.summary}</p>
                    <p className="muted-text">{scenario.reviewOwner || 'No review owner'} | {scenario.approvedBy || 'Pending execution'}</p>
                  </div>
                )) : <div className="empty-state">Executable plans appear here once approved scenarios are ready to be pushed into the live flow.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Revision memory</strong>
                <span className="scenario-type-tag">{revisionScenarios.length}</span>
              </div>
              <div className="signal-list">
                {revisionScenarios.length ? revisionScenarios.map((scenario) => (
                  <div key={scenario.id} className="signal-list-item">
                    <strong>{scenario.title}</strong>
                    <p>{scenario.recommendedOption || 'Decision path recorded in scenario history.'}</p>
                    <p className="muted-text">Rev {scenario.revisionNumber} | {formatTimestamp(scenario.createdAt)}</p>
                  </div>
                )) : <div className="empty-state">Revisions show how operators refined plans before final execution.</div>}
              </div>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderApprovalsExperience() {
    if (!isAuthenticated || !isApprovalsPage) {
      return null
    }

    return (
      <section className="content-grid approvals-center-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Approvals center</p><h2>Pending, approved, rejected, and overdue decisions</h2></div>
            <span className="panel-badge scenario-badge">{pendingApprovalScenarios.length}</span>
          </div>
          <div className="approval-status-grid">
            <article className="stack-card">
              <div className="stack-title-row"><strong>Pending</strong><span className="status-tag status-partial">{pendingApprovalScenarios.length}</span></div>
              <div className="stack-list compact-stack-list">
                {pendingApprovalScenarios.slice(0, 4).map((scenario) => (
                  <button
                    key={scenario.id}
                    className={`stack-card stack-card-compact selectable-card ${selectedApprovalScenario?.id === scenario.id ? 'is-selected' : ''}`}
                    onClick={() => setSelectedScenarioId(scenario.id)}
                    type="button"
                  >
                    <strong>{scenario.title}</strong>
                    <p className="muted-text">{scenario.reviewOwner || 'Review owner'} | {formatCodeLabel(scenario.approvalStage || 'pending_approval')}</p>
                  </button>
                ))}
                {!pendingApprovalScenarios.length ? <div className="empty-state">No pending approvals right now.</div> : null}
              </div>
            </article>
            <article className="stack-card">
              <div className="stack-title-row"><strong>Approved</strong><span className="status-tag status-success">{approvedScenarios.length}</span></div>
              <div className="stack-list compact-stack-list">
                {approvedScenarios.slice(0, 4).map((scenario) => (
                  <button
                    key={scenario.id}
                    className={`stack-card stack-card-compact selectable-card ${selectedApprovalScenario?.id === scenario.id ? 'is-selected' : ''}`}
                    onClick={() => setSelectedScenarioId(scenario.id)}
                    type="button"
                  >
                    <strong>{scenario.title}</strong>
                    <p className="muted-text">{scenario.approvedBy || 'Approved'} | {formatTimestamp(scenario.createdAt)}</p>
                  </button>
                ))}
                {!approvedScenarios.length ? <div className="empty-state">Approved plans will appear here.</div> : null}
              </div>
            </article>
            <article className="stack-card">
              <div className="stack-title-row"><strong>Rejected</strong><span className="status-tag status-failure">{rejectedScenarios.length}</span></div>
              <div className="stack-list compact-stack-list">
                {rejectedScenarios.slice(0, 4).map((scenario) => (
                  <button
                    key={scenario.id}
                    className={`stack-card stack-card-compact selectable-card ${selectedApprovalScenario?.id === scenario.id ? 'is-selected' : ''}`}
                    onClick={() => setSelectedScenarioId(scenario.id)}
                    type="button"
                  >
                    <strong>{scenario.title}</strong>
                    <p className="muted-text">{scenario.rejectedBy || 'Rejected'} | {scenario.rejectionReason || 'Reason recorded in plan history'}</p>
                  </button>
                ))}
                {!rejectedScenarios.length ? <div className="empty-state">Rejected plans will appear here.</div> : null}
              </div>
            </article>
            <article className="stack-card">
              <div className="stack-title-row"><strong>Overdue</strong><span className="status-tag status-failure">{overdueScenarios.length}</span></div>
              <div className="stack-list compact-stack-list">
                {overdueScenarios.slice(0, 4).map((scenario) => (
                  <button
                    key={scenario.id}
                    className={`stack-card stack-card-compact selectable-card ${selectedApprovalScenario?.id === scenario.id ? 'is-selected' : ''}`}
                    onClick={() => setSelectedScenarioId(scenario.id)}
                    type="button"
                  >
                    <strong>{scenario.title}</strong>
                    <p className="muted-text">Due {formatTimestamp(scenario.approvalDueAt)} | {scenario.slaEscalated ? 'Escalated' : 'Awaiting action'}</p>
                  </button>
                ))}
                {!overdueScenarios.length ? <div className="empty-state">No overdue approval items.</div> : null}
              </div>
            </article>
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Approval path focus</strong>
                <span className="scenario-type-tag">{selectedApprovalScenario ? formatCodeLabel(selectedApprovalScenario.approvalStage || selectedApprovalScenario.approvalStatus || 'pending_approval') : 'Clear'}</span>
              </div>
              {selectedApprovalScenario ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedApprovalScenario.title}</strong>
                    <p>{selectedApprovalScenario.summary}</p>
                    <p className="muted-text">
                      Requester {selectedApprovalScenario.requestedBy || 'Unknown'}
                      {selectedApprovalScenario.reviewOwner ? ` | Review owner ${selectedApprovalScenario.reviewOwner}` : ''}
                    </p>
                    <p className="muted-text">
                      Final approver {selectedApprovalScenario.finalApprovalOwner || 'Not assigned'}
                      {selectedApprovalScenario.approvalDueAt ? ` | Due ${formatTimestamp(selectedApprovalScenario.approvalDueAt)}` : ''}
                    </p>
                    <p className="muted-text">
                      Approval {formatCodeLabel(selectedApprovalScenario.approvalStatus)}
                      {selectedApprovalScenario.approvalPolicy ? ` | Policy ${formatCodeLabel(selectedApprovalScenario.approvalPolicy)}` : ''}
                      {selectedApprovalScenario.reviewPriority ? ` | ${formatCodeLabel(selectedApprovalScenario.reviewPriority)} priority` : ''}
                    </p>
                    {selectedApprovalScenario.overdue ? <p className="error-text">This approval lane has breached its expected decision timing.</p> : null}
                  </div>
                </div>
              ) : <div className="empty-state">When a plan is waiting on approval, its decision path and due pressure will appear here.</div>}
            </article>
            {renderScenarioDecisionConsole(selectedApprovalScenario, 'Approval action console', 'Select a queued decision to review it, approve it, reject it, or route it back into the planner.')}
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Decision workload</strong>
                <span className="scenario-type-tag">{pendingApprovalScenarios.length + overdueScenarios.length}</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Pending review</strong>
                  <p>{pendingApprovalScenarios.length} plans are waiting on review or final approval.</p>
                  <p className="muted-text">Use this page to balance volume before the queue turns into escalation pressure.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Overdue risk</strong>
                  <p>{overdueScenarios.length} plans have breached the expected approval timing.</p>
                  <p className="muted-text">{overdueScenarios.filter((scenario) => scenario.slaEscalated).length} are already escalated.</p>
                </div>
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Ops notices</strong>
                <span className="scenario-type-tag">{snapshot.scenarioNotifications.length}</span>
              </div>
              <div className="signal-list">
                {snapshot.scenarioNotifications.length ? snapshot.scenarioNotifications.slice(0, 4).map((notification) => (
                  <div key={`${notification.type}-${notification.scenarioRunId}-${notification.createdAt}`} className="signal-list-item">
                    <strong>{notification.title}</strong>
                    <p>{notification.message}</p>
                    <p className="muted-text">
                      {notification.warehouseCode ? `${notification.warehouseCode} | ` : ''}
                      {notification.approvalStage ? `Stage ${formatCodeLabel(notification.approvalStage)} | ` : ''}
                      {notification.actor ? `${notification.actionRequired ? 'Assigned to' : 'Handled by'} ${notification.actor}` : 'Monitoring'}
                    </p>
                    <p className={`muted-text${notification.actionRequired ? ' overdue-text' : ''}`}>
                      {notification.actionRequired && notification.dueAt ? `Due ${formatTimestamp(notification.dueAt)} | ` : ''}
                      {formatTimestamp(notification.createdAt)}
                    </p>
                  </div>
                )) : <div className="empty-state">Approval-related notifications appear here when plans are rerouted, accepted, or need faster ownership.</div>}
              </div>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderEscalationsExperience() {
    if (!isAuthenticated || !isEscalationsPage) {
      return null
    }

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Escalation inbox</p><h2>Urgent operational items needing ownership</h2></div>
            <span className="panel-badge notification-badge">{snapshot.slaEscalations.length + systemIncidents.length}</span>
          </div>
          <div className="approval-board">
            {escalatedScenarios.map((scenario) => (
              <button
                key={scenario.id}
                className={`stack-card selectable-card ${selectedEscalationScenario?.id === scenario.id ? 'is-selected' : ''}`}
                onClick={() => setSelectedScenarioId(scenario.id)}
                type="button"
              >
                <div className="stack-title-row">
                  <strong>{scenario.title}</strong>
                  <div className="stack-tag-row">
                    <span className="policy-tag policy-escalated">SLA Escalated</span>
                    {scenario.reviewPriority ? <span className={`priority-tag priority-${scenario.reviewPriority.toLowerCase()}`}>{formatCodeLabel(scenario.reviewPriority)}</span> : null}
                  </div>
                </div>
                <p>{scenario.summary}</p>
                <p className="muted-text">Stage {formatCodeLabel(scenario.approvalStage)} | Final approver {scenario.finalApprovalOwner || 'Monitoring'}</p>
              </button>
            ))}
            {systemIncidents.slice(0, 4).map((incident) => (
              <div key={incident.incidentKey} className="stack-card">
                <div className="stack-title-row">
                  <strong>{incident.title}</strong>
                  <span className={`status-tag ${getIncidentStatusClassName(incident.severity)}`}>{formatCodeLabel(incident.severity)}</span>
                </div>
                <p>{incident.detail}</p>
                <p className="muted-text">{incident.context}</p>
              </div>
            ))}
            {!snapshot.slaEscalations.length && !systemIncidents.length ? <div className="empty-state">No escalations are active right now. This page becomes the operational inbox when SLA pressure or trust incidents need faster ownership.</div> : null}
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Escalation focus</strong>
                <span className="scenario-type-tag">{selectedEscalationScenario ? formatCodeLabel(selectedEscalationScenario.approvalStage || 'pending_approval') : 'Clear'}</span>
              </div>
              {selectedEscalationScenario ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedEscalationScenario.title}</strong>
                    <p>{selectedEscalationScenario.summary}</p>
                    <p className="muted-text">
                      Escalated to {selectedEscalationScenario.slaEscalatedTo || 'Monitoring'}
                      {selectedEscalationScenario.slaEscalatedAt ? ` | ${formatTimestamp(selectedEscalationScenario.slaEscalatedAt)}` : ''}
                    </p>
                    <p className="muted-text">
                      Review owner {selectedEscalationScenario.reviewOwner || 'Unassigned'}
                      {selectedEscalationScenario.finalApprovalOwner ? ` | Final approver ${selectedEscalationScenario.finalApprovalOwner}` : ''}
                    </p>
                    <p className={`muted-text${selectedEscalationScenario.overdue ? ' overdue-text' : ''}`}>
                      Due {formatTimestamp(selectedEscalationScenario.approvalDueAt)}
                      {selectedEscalationScenario.slaAcknowledged ? ` | Acknowledged by ${selectedEscalationScenario.slaAcknowledgedBy}` : ' | Waiting on ownership'}
                    </p>
                    {selectedEscalationScenario.slaAcknowledgementNote ? <p className="muted-text">Acknowledgement note: {selectedEscalationScenario.slaAcknowledgementNote}</p> : null}
                  </div>
                </div>
              ) : <div className="empty-state">No approval lanes are currently in escalation. This page is ready when SLA pressure starts forming.</div>}
            </article>
            {renderScenarioDecisionConsole(selectedEscalationScenario, 'Escalation action console', 'Select an escalated plan to acknowledge it, reject it, or move it toward final approval.')}
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Trust incidents</strong>
                <span className="scenario-type-tag">{systemIncidents.length}</span>
              </div>
              <div className="signal-list">
                {systemIncidents.length ? systemIncidents.slice(0, 4).map((incident) => (
                  <div key={incident.incidentKey} className="signal-list-item">
                    <strong>{incident.title}</strong>
                    <p>{incident.detail}</p>
                    <p className="muted-text">{incident.context} | {formatTimestamp(incident.createdAt)}</p>
                  </div>
                )) : <div className="empty-state">Runtime and connector incidents appear here when they need urgent operational ownership.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Escalation ownership</strong>
                <span className="scenario-type-tag">{escalatedScenarios.length}</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Active escalations</strong>
                  <p>{escalatedScenarios.length} approval lane{escalatedScenarios.length === 1 ? '' : 's'} currently require faster ownership.</p>
                  <p className="muted-text">{escalatedScenarios.filter((scenario) => scenario.slaAcknowledged).length} already have explicit acknowledgment.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Related incidents</strong>
                  <p>{systemIncidents.length} trust incident{systemIncidents.length === 1 ? '' : 's'} sit beside the escalation queue.</p>
                  <p className="muted-text">Use this page as the command inbox when approval risk and runtime risk collide.</p>
                </div>
              </div>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderIntegrationsOverview() {
    if (!isAuthenticated || !isIntegrationsPage) {
      return null
    }

    const connectorSpotlights = snapshot.integrationConnectors.slice(0, 5)
    const recentImportRuns = snapshot.integrationImportRuns.slice(0, 4)
    const selectedConnector = connectorSpotlights[0]
    const unownedConnectors = snapshot.integrationConnectors.filter((connector) => !connector.supportOwnerActorName).length
    const scheduledConnectors = snapshot.integrationConnectors.filter((connector) => connector.syncMode === 'SCHEDULED_PULL').length

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Integrations</p><h2>Connector health and operational telemetry</h2></div>
            <span className="panel-badge integration-badge">{snapshot.integrationConnectors.length}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Enabled connectors" value={enabledConnectorCount} accent="teal" />
            <SummaryCard label="Import runs" value={snapshot.integrationImportRuns.length} accent="blue" />
            <SummaryCard label="Replay queued" value={pendingReplayCount} accent="amber" />
            <SummaryCard label="Support incidents" value={systemIncidents.length} accent="rose" />
          </div>
          <div className="experience-grid experience-grid-three">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Connector portfolio</strong>
                <span className="scenario-type-tag">{connectorSpotlights.length ? 'Live' : 'Pending'}</span>
              </div>
              <div className="signal-list">
                {connectorSpotlights.length ? connectorSpotlights.map((connector) => (
                  <div key={`${connector.sourceSystem}:${connector.type}`} className="signal-list-item">
                    <div className="stack-title-row">
                      <strong>{connector.displayName}</strong>
                      <span className={`status-tag ${connector.enabled ? 'status-success' : 'status-failure'}`}>{connector.enabled ? 'Healthy' : 'Disabled'}</span>
                    </div>
                    <p>{connector.sourceSystem} | {formatCodeLabel(connector.type)}</p>
                    <p className="muted-text">
                      {formatCodeLabel(connector.syncMode)}
                      {connector.syncIntervalMinutes ? ` every ${connector.syncIntervalMinutes} min` : ''}
                      {connector.defaultWarehouseCode ? ` | Default ${connector.defaultWarehouseCode}` : ' | Tenant-wide'}
                    </p>
                  </div>
                )) : <div className="empty-state">Connectors will appear here once the workspace is integrated with external systems.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Selected connector detail</strong>
                <span className="scenario-type-tag">{selectedConnector ? formatCodeLabel(selectedConnector.validationPolicy) : 'Waiting'}</span>
              </div>
              {selectedConnector ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedConnector.displayName}</strong>
                    <p>{selectedConnector.notes || 'No connector notes yet. Use this space to capture support ownership and operating assumptions.'}</p>
                    <p className="muted-text">Source {selectedConnector.sourceSystem} | Owner {selectedConnector.supportOwnerDisplayName || 'Unassigned'} | Transform {formatCodeLabel(selectedConnector.transformationPolicy)}</p>
                    <p className="muted-text">{selectedConnector.allowDefaultWarehouseFallback ? 'Warehouse fallback is enabled for missing inbound lane data.' : 'Warehouse fallback is off. Payloads must arrive with a valid lane.'}</p>
                  </div>
                  <div className="signal-chip-grid">
                    <span className="scenario-type-tag">{formatCodeLabel(selectedConnector.syncMode)}</span>
                    <span className="scenario-type-tag">{formatCodeLabel(selectedConnector.type)}</span>
                    <span className="scenario-type-tag">{selectedConnector.defaultWarehouseCode || 'No default warehouse'}</span>
                  </div>
                  <div className="history-action-row">
                    <button className="ghost-button" onClick={() => navigateToPage('settings')} type="button">Manage Policies</button>
                    <button className="ghost-button" onClick={() => navigateToPage('replay')} type="button">Open Replay Queue</button>
                  </div>
                </div>
              ) : <div className="empty-state">Choose a connector lane to review support ownership, policy posture, and recovery routes.</div>}
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Integration posture</strong>
                <span className="scenario-type-tag">{recentImportRuns.length} recent runs</span>
              </div>
              <div className="utility-metric-grid">
                <div><span>Scheduled pull</span><strong>{scheduledConnectors}</strong></div>
                <div><span>Unowned</span><strong>{unownedConnectors}</strong></div>
                <div><span>Replay queue</span><strong>{pendingReplayCount}</strong></div>
                <div><span>Support incidents</span><strong>{systemIncidents.length}</strong></div>
              </div>
              <div className="signal-list">
                {recentImportRuns.length ? recentImportRuns.map((run) => (
                  <div key={run.id} className="signal-list-item">
                    <div className="stack-title-row">
                      <strong>{run.fileName || `${formatCodeLabel(run.connectorType)} ingress`}</strong>
                      <span className={`status-tag ${getImportStatusClassName(run.status)}`}>{formatCodeLabel(run.status)}</span>
                    </div>
                    <p>{run.sourceSystem} | {run.recordsReceived} rows</p>
                    <p className="muted-text">{run.ordersImported} imported | {run.ordersFailed} failed | {formatTimestamp(run.createdAt)}</p>
                  </div>
                )) : <div className="empty-state">Import telemetry will appear once webhook, CSV, or scheduled sync activity is flowing.</div>}
              </div>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderReplayOverview() {
    if (!isAuthenticated || !isReplayPage) {
      return null
    }

    const queuedRecords = snapshot.integrationReplayQueue
    const selectedRecord = queuedRecords.find((record) => record.status === 'PENDING') || queuedRecords[0]
    const failedCount = queuedRecords.filter((record) => record.status === 'REPLAY_FAILED').length
    const recoveredCount = queuedRecords.filter((record) => record.status === 'REPLAYED').length

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Replay queue</p><h2>Recover failed inbound work safely</h2></div>
            <span className="panel-badge integration-badge">{pendingReplayCount}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Waiting" value={queuedRecords.filter((item) => item.status === 'PENDING').length} accent="amber" />
            <SummaryCard label="Replay failed" value={failedCount} accent="rose" />
            <SummaryCard label="Recovered" value={recoveredCount} accent="teal" />
            <SummaryCard label="Connectors" value={snapshot.integrationConnectors.length} accent="blue" />
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Failed inbound queue</strong>
                <span className="scenario-type-tag">{queuedRecords.length}</span>
              </div>
              {integrationReplayState.error ? <p className="error-text">{integrationReplayState.error}</p> : null}
              {integrationReplayState.success ? <p className="success-text">{integrationReplayState.success}</p> : null}
              <div className="signal-list">
                {queuedRecords.length ? queuedRecords.map((record) => (
                  <div key={record.id} className="signal-list-item">
                    <div className="stack-title-row">
                      <strong>{record.externalOrderId}</strong>
                      <span className={`status-tag ${getReplayStatusClassName(record.status)}`}>{formatCodeLabel(record.status)}</span>
                    </div>
                    <p>{record.sourceSystem} | {record.warehouseCode || 'Unknown lane'}</p>
                    <p className="muted-text">{record.failureMessage}</p>
                    <p className="muted-text">Attempts {record.replayAttemptCount} | Queued {formatTimestamp(record.createdAt)}</p>
                  </div>
                )) : <div className="empty-state">No failed inbound items are waiting. Recovery is currently clear.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Recovery detail</strong>
                <span className="scenario-type-tag">{selectedRecord ? formatCodeLabel(selectedRecord.connectorType) : 'Clear'}</span>
              </div>
              {selectedRecord ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedRecord.externalOrderId}</strong>
                    <p>{selectedRecord.failureMessage}</p>
                    <p className="muted-text">
                      Source {selectedRecord.sourceSystem} | Warehouse {selectedRecord.warehouseCode || 'Unknown'} | Attempts {selectedRecord.replayAttemptCount}
                    </p>
                    <p className="muted-text">
                      {selectedRecord.lastAttemptedAt ? `Last attempted ${formatTimestamp(selectedRecord.lastAttemptedAt)} | ` : ''}
                      Queued {formatTimestamp(selectedRecord.createdAt)}
                    </p>
                    {selectedRecord.lastReplayMessage ? <p className="muted-text">Last replay note: {selectedRecord.lastReplayMessage}</p> : null}
                  </div>
                  <div className="history-action-row">
                    <button
                      className="secondary-button"
                      onClick={() => replayFailedIntegration(selectedRecord.id)}
                      disabled={integrationReplayState.loadingId === selectedRecord.id || !signedInSession || !signedInRoles.some((role) => role === 'INTEGRATION_OPERATOR' || role === 'INTEGRATION_ADMIN') || !hasWarehouseScope(signedInWarehouseScopes, selectedRecord.warehouseCode)}
                      type="button"
                    >
                      {integrationReplayState.loadingId === selectedRecord.id ? 'Replaying...' : 'Replay Into Live Flow'}
                    </button>
                    <button className="ghost-button" onClick={() => navigateToPage('integrations')} type="button">View Connector Health</button>
                  </div>
                  <p className="muted-text">Recovery keeps failed inbound activity visible, actionable, and auditable instead of hidden inside scripts or operator guesswork.</p>
                </div>
              ) : <div className="empty-state">Select a replay record to inspect failure reason, attempts, and recovery posture.</div>}
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderRuntimeOverview() {
    if (!isAuthenticated || !isRuntimePage) {
      return null
    }

    const runtimeSignalCards = runtime
      ? [
        { label: 'Readiness', value: formatCodeLabel(runtime.readinessState), note: 'Current service acceptance posture' },
        { label: 'Queue depth', value: runtime.backbone.pendingDispatchCount, note: 'Pending work in dispatch backbone' },
        { label: 'Failed dispatch', value: runtime.backbone.failedDispatchCount, note: 'Dispatch work needing operator attention' },
        { label: 'Observed', value: formatTimestamp(runtime.observedAt), note: 'Latest runtime observation point' },
      ]
      : []

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Runtime observability</p><h2>Service health, queue pressure, and incident trust</h2></div>
            <span className={`panel-badge ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'audit-badge'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Readiness" value={runtime ? formatCodeLabel(runtime.readinessState) : '...'} accent="teal" />
            <SummaryCard label="Dispatch queued" value={runtime ? runtime.backbone.pendingDispatchCount : '...'} accent="amber" />
            <SummaryCard label="Failed dispatch" value={runtime ? runtime.backbone.failedDispatchCount : '...'} accent="rose" />
            <SummaryCard label="Incidents" value={systemIncidents.length} accent="blue" />
          </div>
          <div className="experience-grid experience-grid-three">
            <article className="stack-card section-card" id="runtime-health">
              <div className="stack-title-row">
                <strong>Health board</strong>
                <span className={`status-tag ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'status-partial'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
              </div>
              <div className="signal-list">
                {runtimeSignalCards.length ? runtimeSignalCards.map((card) => (
                  <div key={card.label} className="signal-list-item">
                    <strong>{card.label}</strong>
                    <p>{card.value}</p>
                    <p className="muted-text">{card.note}</p>
                  </div>
                )) : <div className="empty-state">Runtime signals will appear once the service heartbeat is available.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Backbone and metrics</strong>
                <span className="scenario-type-tag">Prometheus ready</span>
              </div>
              {runtime ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>Dispatch backbone</strong>
                    <p>Drains every {runtime.backbone.dispatchIntervalMs} ms in batches of {runtime.backbone.batchSize}.</p>
                    <p className="muted-text">Oldest queued work {runtime.backbone.oldestPendingAgeSeconds == null ? 'clear' : `${runtime.backbone.oldestPendingAgeSeconds}s`} | Latest processed {formatTimestamp(runtime.backbone.latestProcessedAt)}</p>
                  </div>
                  <div className="signal-list-item">
                    <strong>Telemetry window</strong>
                    <p>{runtime.diagnostics.windowHours} hrs | {runtime.diagnostics.businessEventsInWindow} business events | {runtime.diagnostics.integrationEventsInWindow} integration events</p>
                    <p className="muted-text">Latest business event {formatTimestamp(runtime.diagnostics.latestBusinessEventAt)} | Latest failure audit {formatTimestamp(runtime.diagnostics.latestFailureAt)}</p>
                  </div>
                  <div className="signal-list-item">
                    <strong>Metrics surface</strong>
                    <p>Orders {formatMetricValue(runtime.metrics.ordersIngested)} | Fulfillment {formatMetricValue(runtime.metrics.fulfillmentUpdates)} | Dispatch processed {formatMetricValue(runtime.metrics.dispatchProcessed)}</p>
                    <p className="muted-text">Prometheus metrics are exposed for production scraping at <code>/actuator/prometheus</code>.</p>
                  </div>
                </div>
              ) : <div className="empty-state">Queue, diagnostics, and metrics posture will appear once runtime data loads.</div>}
            </article>
            <article className="stack-card section-card" id="runtime-incident-lane">
              <div className="stack-title-row">
                <strong>Incident lane</strong>
                <span className="scenario-type-tag">{systemIncidents.length}</span>
              </div>
              <div className="signal-list">
                {systemIncidents.length ? systemIncidents.slice(0, 4).map((incident) => (
                  <button
                    key={incident.incidentKey}
                    className={`signal-list-item selectable-card ${selectedRuntimeIncident?.incidentKey === incident.incidentKey ? 'is-selected' : ''}`}
                    onClick={() => setSelectedRuntimeIncidentKey(incident.incidentKey)}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{incident.title}</strong>
                      <span className={`status-tag ${getIncidentStatusClassName(incident.severity)}`}>{formatCodeLabel(incident.severity)}</span>
                    </div>
                    <p>{incident.detail}</p>
                    <p className="muted-text">{incident.context} | {formatTimestamp(incident.createdAt)}</p>
                  </button>
                )) : <div className="empty-state">No active runtime incidents. This lane lights up when trust or backbone issues need operator attention.</div>}
              </div>
            </article>
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Incident focus</strong>
                <span className={`status-tag ${selectedRuntimeIncident ? getIncidentStatusClassName(selectedRuntimeIncident.severity) : 'status-partial'}`}>
                  {selectedRuntimeIncident ? formatCodeLabel(selectedRuntimeIncident.severity) : 'Clear'}
                </span>
              </div>
              {selectedRuntimeIncident ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedRuntimeIncident.title}</strong>
                    <p>{selectedRuntimeIncident.detail}</p>
                    <p className="muted-text">{selectedRuntimeIncident.context}</p>
                    <p className="muted-text">Observed {formatTimestamp(selectedRuntimeIncident.createdAt)}</p>
                  </div>
                </div>
              ) : <div className="empty-state">When runtime or connector trust issues appear, this page will hold the lead incident context here.</div>}
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Runtime response posture</strong>
                <span className="scenario-type-tag">{runtime?.overallStatus || 'Loading'}</span>
              </div>
              <div className="utility-metric-grid">
                <div><span>Queue pending</span><strong>{runtime?.backbone?.pendingDispatchCount ?? 0}</strong></div>
                <div><span>Failed dispatch</span><strong>{runtime?.backbone?.failedDispatchCount ?? 0}</strong></div>
                <div><span>High severity</span><strong>{systemIncidents.filter((incident) => ['CRITICAL', 'HIGH'].includes(incident.severity)).length}</strong></div>
                <div><span>Oldest queued</span><strong>{runtime?.backbone?.oldestPendingAgeSeconds == null ? 'Clear' : `${runtime.backbone.oldestPendingAgeSeconds}s`}</strong></div>
              </div>
              <div className="history-action-row">
                <button className="ghost-button" onClick={() => navigateToPage('audit')} type="button">Open Audit</button>
                <button className="ghost-button" onClick={() => navigateToPage('releases')} type="button">Open Releases</button>
              </div>
              <p className="muted-text">This page should help teams decide whether the issue is operational noise, queue pressure, or a release/runtime trust problem.</p>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderAuditOverview() {
    if (!isAuthenticated || !isAuditPage) {
      return null
    }

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Audit and events</p><h2>Trace the live business timeline</h2></div>
            <span className="panel-badge audit-badge">{snapshot.auditLogs.length + snapshot.recentEvents.length}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Audit entries" value={snapshot.auditLogs.length} accent="blue" />
            <SummaryCard label="Business events" value={snapshot.recentEvents.length} accent="teal" />
            <SummaryCard label="Incidents" value={systemIncidents.length} accent="rose" />
            <SummaryCard label="Replay queued" value={pendingReplayCount} accent="amber" />
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card" id="audit-events">
              <div className="stack-title-row">
                <strong>Business event timeline</strong>
                <span className="scenario-type-tag">{recentBusinessEvents.length}</span>
              </div>
              <div className="signal-list">
                {recentBusinessEvents.length ? recentBusinessEvents.map((event) => (
                  <button
                    key={event.id}
                    className={`signal-list-item selectable-card ${selectedAuditTrace?.traceKey === `event-${event.id}` ? 'is-selected' : ''}`}
                    onClick={() => setSelectedTraceEntryKey(`event-${event.id}`)}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{formatCodeLabel(event.eventType)}</strong>
                      <span className="scenario-type-tag">{event.source}</span>
                    </div>
                    <p>{event.payloadSummary}</p>
                    <p className="muted-text">{formatTimestamp(event.createdAt)}</p>
                  </button>
                )) : <div className="empty-state">Business events will stream here as SynapseCore processes operational change.</div>}
              </div>
            </article>
            <article className="stack-card section-card" id="audit-logs">
              <div className="stack-title-row">
                <strong>Audit explorer</strong>
                <span className="scenario-type-tag">{recentAuditEntries.length}</span>
              </div>
              <div className="signal-list">
                {recentAuditEntries.length ? recentAuditEntries.map((log) => (
                  <button
                    key={log.id}
                    className={`signal-list-item selectable-card ${selectedAuditTrace?.traceKey === `audit-${log.id}` ? 'is-selected' : ''}`}
                    onClick={() => setSelectedTraceEntryKey(`audit-${log.id}`)}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{formatCodeLabel(log.action)}</strong>
                      <span className={`status-tag status-${log.status.toLowerCase()}`}>{log.status}</span>
                    </div>
                    <p>{log.details}</p>
                    <p className="muted-text">{log.targetType} | {log.targetRef} | {formatTimestamp(log.createdAt)}</p>
                    <p className="trace-line">Request {log.requestId}</p>
                  </button>
                )) : <div className="empty-state">Audit traces will appear once protected operational actions are flowing.</div>}
              </div>
            </article>
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Trace focus</strong>
                <span className="scenario-type-tag">{selectedAuditTrace ? formatCodeLabel(selectedAuditTrace.traceType) : 'Waiting'}</span>
              </div>
              {selectedAuditTrace ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedAuditTrace.traceType === 'audit' ? formatCodeLabel(selectedAuditTrace.action) : formatCodeLabel(selectedAuditTrace.eventType)}</strong>
                    <p>{selectedAuditTrace.traceType === 'audit' ? selectedAuditTrace.details : selectedAuditTrace.payloadSummary}</p>
                    <p className="muted-text">
                      {selectedAuditTrace.traceType === 'audit'
                        ? `${selectedAuditTrace.targetType} | ${selectedAuditTrace.targetRef}`
                        : `${selectedAuditTrace.source} | ${selectedAuditTrace.id}`}
                    </p>
                    <p className="muted-text">{formatTimestamp(selectedAuditTrace.createdAt)}</p>
                    {selectedAuditTrace.traceType === 'audit' ? <p className="trace-line">Request {selectedAuditTrace.requestId}</p> : null}
                  </div>
                </div>
              ) : <div className="empty-state">Select an audit entry or business event to inspect the exact trace lane and timing context.</div>}
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Traceability posture</strong>
                <span className="scenario-type-tag">{snapshot.auditLogs.length + snapshot.recentEvents.length}</span>
              </div>
              <div className="utility-metric-grid">
                <div><span>Audit</span><strong>{snapshot.auditLogs.length}</strong></div>
                <div><span>Events</span><strong>{snapshot.recentEvents.length}</strong></div>
                <div><span>Replay</span><strong>{pendingReplayCount}</strong></div>
                <div><span>Incidents</span><strong>{systemIncidents.length}</strong></div>
              </div>
              <div className="history-action-row">
                <button className="ghost-button" onClick={() => navigateToPage('runtime')} type="button">Open Runtime</button>
                <button className="ghost-button" onClick={() => navigateToPage('replay')} type="button">Open Replay</button>
              </div>
              <p className="muted-text">This page should help support and operators line events, protected actions, and recovery flow back to one trusted timeline.</p>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderUsersOverview() {
    if (!isAuthenticated || !isUsersPage) {
      return null
    }

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">{isUsersPage ? 'Access control' : 'Workspace settings'}</p><h2>{isUsersPage ? 'Manage operators, roles, and user access' : 'Manage workspace policies and tenant controls'}</h2></div>
            <span className="panel-badge integration-badge">{accessAdminOperators.length + accessAdminUsers.length}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Operators" value={accessAdminOperators.length} accent="teal" />
            <SummaryCard label="Users" value={accessAdminUsers.length} accent="blue" />
            <SummaryCard label="Warehouses" value={workspaceAdmin?.warehouses?.length || 0} accent="amber" />
            <SummaryCard label="Support incidents" value={workspaceAdmin?.supportIncidents?.length || 0} accent="rose" />
          </div>
          <div className="experience-grid experience-grid-three">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Operator lanes</strong>
                <span className="scenario-type-tag">{accessAdminOperators.length}</span>
              </div>
              <div className="signal-list">
                {accessAdminOperators.length ? accessAdminOperators.slice(0, 5).map((operator) => (
                  <button
                    key={operator.id}
                    className={`signal-list-item selectable-card ${selectedAccessSubject?.subjectKey === `operator-${operator.id}` ? 'is-selected' : ''}`}
                    onClick={() => setSelectedAccessSubjectKey(`operator-${operator.id}`)}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{operator.displayName}</strong>
                      <span className={`status-tag ${operator.active ? 'status-success' : 'status-failure'}`}>{operator.active ? 'Active' : 'Inactive'}</span>
                    </div>
                    <p>{operator.actorName}</p>
                    <p className="muted-text">{operator.roles.map((role) => formatCodeLabel(role)).join(', ') || 'No roles assigned'}</p>
                    <p className="muted-text">{operator.warehouseScopes.length ? `Warehouse scope ${operator.warehouseScopes.join(', ')}` : 'Tenant-wide lane'}</p>
                  </button>
                )) : <div className="empty-state">Operator lanes appear here once tenant admins start assigning roles and warehouse scope.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>User roster</strong>
                <span className="scenario-type-tag">{accessAdminUsers.length}</span>
              </div>
              <div className="signal-list">
                {accessAdminUsers.length ? accessAdminUsers.slice(0, 5).map((user) => (
                  <button
                    key={user.id}
                    className={`signal-list-item selectable-card ${selectedAccessSubject?.subjectKey === `user-${user.id}` ? 'is-selected' : ''}`}
                    onClick={() => setSelectedAccessSubjectKey(`user-${user.id}`)}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{user.fullName}</strong>
                      <span className={`status-tag ${user.active ? 'status-success' : 'status-failure'}`}>{user.active ? 'Enabled' : 'Disabled'}</span>
                    </div>
                    <p>{user.username}</p>
                    <p className="muted-text">Operator lane {user.operatorDisplayName || user.operatorActorName}</p>
                  </button>
                )) : <div className="empty-state">User accounts will appear here once access lifecycle flows are active.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Access posture</strong>
                <span className="scenario-type-tag">{workspaceAdmin?.recentSupportActivity?.length || 0} activity</span>
              </div>
              <div className="utility-metric-grid">
                <div><span>Reset required</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersRequiringPasswordChange || 0}</strong></div>
                <div><span>Rotation overdue</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersPastPasswordRotation || 0}</strong></div>
                <div><span>Blocked by lane</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersBlockedByInactiveOperator || 0}</strong></div>
                <div><span>High severity</span><strong>{workspaceAdmin?.supportDiagnostics?.highSeverityIncidentCount || 0}</strong></div>
              </div>
              <p className="muted-text">Tenant admins can create, scope, deactivate, rotate, and recover access without leaving the workspace.</p>
            </article>
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Access focus</strong>
                <span className="scenario-type-tag">{selectedAccessSubject ? formatCodeLabel(selectedAccessSubject.subjectType) : 'Waiting'}</span>
              </div>
              {selectedAccessSubject ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedAccessSubject.subjectType === 'operator' ? selectedAccessSubject.displayName : selectedAccessSubject.fullName}</strong>
                    <p>{selectedAccessSubject.subjectType === 'operator' ? selectedAccessSubject.actorName : selectedAccessSubject.username}</p>
                    <p className="muted-text">
                      {selectedAccessSubject.subjectType === 'operator'
                        ? (selectedAccessSubject.roles.map((role) => formatCodeLabel(role)).join(', ') || 'No roles assigned')
                        : `Operator lane ${selectedAccessSubject.operatorDisplayName || selectedAccessSubject.operatorActorName}`}
                    </p>
                    <p className="muted-text">
                      {selectedAccessSubject.subjectType === 'operator'
                        ? (selectedAccessSubject.warehouseScopes.length ? `Warehouse scope ${selectedAccessSubject.warehouseScopes.join(', ')}` : 'Tenant-wide lane')
                        : (selectedAccessSubject.warehouseScopes.length ? `Warehouse scope ${selectedAccessSubject.warehouseScopes.join(', ')}` : 'Tenant-wide lane')}
                    </p>
                    <p className="muted-text">
                      {selectedAccessSubject.active ? 'Active access posture' : 'Inactive access posture'}
                      {selectedAccessSubject.passwordChangeRequired ? ' | Password reset required' : ''}
                    </p>
                  </div>
                </div>
              ) : <div className="empty-state">Select an operator lane or user account to inspect the exact scope and access posture.</div>}
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Access response posture</strong>
                <span className="scenario-type-tag">{workspaceAdmin?.recentSupportActivity?.length || 0} activity</span>
              </div>
              <div className="utility-metric-grid">
                <div><span>Resets</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersRequiringPasswordChange || 0}</strong></div>
                <div><span>Rotation overdue</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersPastPasswordRotation || 0}</strong></div>
                <div><span>Blocked</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersBlockedByInactiveOperator || 0}</strong></div>
                <div><span>Unowned connectors</span><strong>{workspaceAdmin?.supportDiagnostics?.connectorsWithoutSupportOwner || 0}</strong></div>
              </div>
              <div className="history-action-row">
                <button className="ghost-button" onClick={() => navigateToPage('settings')} type="button">Open Settings</button>
                <button className="ghost-button" onClick={() => navigateToPage('profile')} type="button">Open Profile</button>
              </div>
              <p className="muted-text">This page should help tenant admins understand who can act, where they can act, and which access lanes need cleanup first.</p>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderSettingsExperience() {
    if (!isAuthenticated || !isSettingsPage) {
      return null
    }

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Company settings</p><h2>Workspace profile, security, and operational defaults</h2></div>
            <span className="panel-badge scenario-badge">{workspaceAdmin?.warehouses?.length || 0}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Rotation days" value={workspaceAdmin?.securitySettings?.passwordRotationDays || 0} accent="blue" />
            <SummaryCard label="Session timeout" value={workspaceAdmin?.securitySettings?.sessionTimeoutMinutes || 0} accent="teal" />
            <SummaryCard label="Warehouse lanes" value={workspaceAdmin?.warehouses?.length || 0} accent="amber" />
            <SummaryCard label="Connectors" value={workspaceAdmin?.connectors?.length || 0} accent="rose" />
          </div>
          {accessAdminState.error ? <p className="error-text">{accessAdminState.error}</p> : null}
          {accessAdminState.success ? <p className="success-text">{accessAdminState.success}</p> : null}
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card" id="settings-profile">
              <div className="stack-title-row"><strong>Workspace profile</strong><span className="status-tag status-success">Editable</span></div>
              <div className="session-control-row">
                <label className="field planner-name-field">
                  <span>Tenant Name</span>
                  <input value={workspaceSettingsForm.tenantName} onChange={(event) => setWorkspaceSettingsForm((current) => ({ ...current, tenantName: event.target.value }))} placeholder="Tenant workspace name" disabled={accessAdminState.loading || !canManageTenantAccess} />
                </label>
                <label className="field planner-name-field">
                  <span>Description</span>
                  <input value={workspaceSettingsForm.description} onChange={(event) => setWorkspaceSettingsForm((current) => ({ ...current, description: event.target.value }))} placeholder="Operational workspace summary" disabled={accessAdminState.loading || !canManageTenantAccess} />
                </label>
              </div>
              <div className="history-action-row">
                <button className="secondary-button" onClick={saveWorkspaceSettings} disabled={accessAdminState.loading || !canManageTenantAccess || !workspaceSettingsForm.tenantName.trim()} type="button">
                  {accessAdminState.loading ? 'Working...' : 'Save Workspace'}
                </button>
              </div>
              <p className="muted-text">{workspaceSettingsForm.description || workspaceAdmin?.description || 'Add a workspace description to explain the company operating scope.'}</p>
            </article>
            <article className="stack-card section-card" id="settings-security">
              <div className="stack-title-row"><strong>Security policy</strong><span className="status-tag status-partial">Tenant policy</span></div>
              <div className="session-control-row">
                <label className="field planner-name-field">
                  <span>Password Rotation Days</span>
                  <input value={workspaceSecurityForm.passwordRotationDays} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, passwordRotationDays: event.target.value }))} inputMode="numeric" disabled={accessAdminState.loading || !canManageTenantAccess} />
                </label>
                <label className="field planner-name-field">
                  <span>Session Timeout Minutes</span>
                  <input value={workspaceSecurityForm.sessionTimeoutMinutes} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, sessionTimeoutMinutes: event.target.value }))} inputMode="numeric" disabled={accessAdminState.loading || !canManageTenantAccess} />
                </label>
                <label className="field checkbox-field">
                  <span>Invalidate Other Sessions</span>
                  <input className="checkbox-input" type="checkbox" checked={workspaceSecurityForm.invalidateOtherSessions} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, invalidateOtherSessions: event.target.checked }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                </label>
              </div>
              <div className="history-action-row">
                <button
                  className="ghost-button"
                  onClick={saveWorkspaceSecuritySettings}
                  disabled={
                    accessAdminState.loading
                    || !canManageTenantAccess
                    || Number.parseInt(workspaceSecurityForm.passwordRotationDays, 10) < 7
                    || Number.parseInt(workspaceSecurityForm.sessionTimeoutMinutes, 10) < 15
                  }
                  type="button"
                >
                  {accessAdminState.loading ? 'Working...' : 'Save Security Policy'}
                </button>
              </div>
              <p className="muted-text">{workspaceSecurityForm.invalidateOtherSessions ? 'Other active tenant sessions will be forced to sign in again after this update.' : 'Leave session invalidation off when you only want to tune the tenant policy for future sessions.'}</p>
            </article>
          </div>
          <div className="experience-grid experience-grid-three">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Warehouse defaults</strong>
                <span className="scenario-type-tag">{workspaceAdmin?.warehouses?.length || 0}</span>
              </div>
              <div className="signal-list">
                {workspaceAdmin?.warehouses?.length ? workspaceAdmin.warehouses.map((warehouse) => (
                  <button
                    key={warehouse.id}
                    className={`signal-list-item selectable-card ${selectedWorkspaceWarehouse?.id === warehouse.id ? 'is-selected' : ''}`}
                    onClick={() => setSelectedWorkspaceWarehouseId(warehouse.id)}
                    type="button"
                  >
                    <strong>{warehouse.name}</strong>
                    <p>{warehouse.code}</p>
                    <p className="muted-text">{warehouse.location || 'Location not defined yet'}</p>
                  </button>
                )) : <div className="empty-state">Warehouse and site defaults will appear here when the workspace is configured.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Connector ownership</strong>
                <span className="scenario-type-tag">{workspaceAdmin?.connectors?.length || 0}</span>
              </div>
              <div className="signal-list">
                {workspaceAdmin?.connectors?.length ? workspaceAdmin.connectors.map((connector) => (
                  <button
                    key={connector.id}
                    className={`signal-list-item selectable-card ${selectedWorkspaceConnector?.id === connector.id ? 'is-selected' : ''}`}
                    onClick={() => setSelectedWorkspaceConnectorId(connector.id)}
                    type="button"
                  >
                    <strong>{connector.displayName}</strong>
                    <p>{connector.sourceSystem} | {formatCodeLabel(connector.syncMode)}</p>
                    <p className="muted-text">{connector.supportOwnerDisplayName || 'No support owner assigned yet'}</p>
                  </button>
                )) : <div className="empty-state">Connector policy cards will appear here once integration lanes are created for the workspace.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Policy envelope</strong>
                <span className="scenario-type-tag">Workspace-wide</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Password rotation</strong>
                  <p>{workspaceSecurityForm.passwordRotationDays} day cycle</p>
                  <p className="muted-text">Keeps tenant accounts moving through predictable security hygiene windows.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Session timeout</strong>
                  <p>{workspaceSecurityForm.sessionTimeoutMinutes} minute limit</p>
                  <p className="muted-text">Prevents stale operations consoles from lingering with live control authority.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Session invalidation</strong>
                  <p>{workspaceSecurityForm.invalidateOtherSessions ? 'On next save' : 'No forced invalidation'}</p>
                  <p className="muted-text">Use when tenant admins need a fast reset after a support or access event.</p>
                </div>
              </div>
            </article>
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Warehouse focus</strong>
                <span className="scenario-type-tag">{selectedWorkspaceWarehouse?.code || 'Waiting'}</span>
              </div>
              {selectedWorkspaceWarehouse && selectedWorkspaceWarehouseDraft ? (
                <>
                  <div className="session-control-row">
                    <label className="field planner-name-field">
                      <span>Name</span>
                      <input value={selectedWorkspaceWarehouseDraft.name} onChange={(event) => setWorkspaceWarehouseDrafts((current) => ({ ...current, [selectedWorkspaceWarehouse.id]: { ...selectedWorkspaceWarehouseDraft, name: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                    </label>
                    <label className="field planner-name-field">
                      <span>Location</span>
                      <input value={selectedWorkspaceWarehouseDraft.location} onChange={(event) => setWorkspaceWarehouseDrafts((current) => ({ ...current, [selectedWorkspaceWarehouse.id]: { ...selectedWorkspaceWarehouseDraft, location: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                    </label>
                  </div>
                  <div className="history-action-row">
                    <button className="ghost-button" onClick={() => saveWorkspaceWarehouse(selectedWorkspaceWarehouse.id)} disabled={accessAdminState.loading || !canManageTenantAccess || !selectedWorkspaceWarehouseDraft.name.trim() || !selectedWorkspaceWarehouseDraft.location.trim()} type="button">Save Warehouse</button>
                  </div>
                  <p className="muted-text">Use this lane to keep the site name and location clean before operators depend on it across alerts, inventory, and approvals.</p>
                </>
              ) : <div className="empty-state">Select a warehouse lane to tune its name and location.</div>}
            </article>
            <article className="stack-card section-card" id="settings-connectors">
              <div className="stack-title-row">
                <strong>Connector focus</strong>
                <span className="scenario-type-tag">{selectedWorkspaceConnector ? formatCodeLabel(selectedWorkspaceConnector.syncMode) : 'Waiting'}</span>
              </div>
              {selectedWorkspaceConnector && selectedWorkspaceConnectorDraft ? (
                <>
                  <p className="muted-text">{selectedWorkspaceConnector.sourceSystem} | {formatCodeLabel(selectedWorkspaceConnector.type)}{selectedWorkspaceConnector.defaultWarehouseCode ? ` | Default warehouse ${selectedWorkspaceConnector.defaultWarehouseCode}` : ' | Tenant-wide connector lane'}</p>
                  <div className="session-control-row">
                    <label className="field planner-name-field">
                      <span>Sync Mode</span>
                      <select value={selectedWorkspaceConnectorDraft.syncMode} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, syncMode: event.target.value, syncIntervalMinutes: event.target.value === 'SCHEDULED_PULL' ? selectedWorkspaceConnectorDraft.syncIntervalMinutes || '60' : '' } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                        {integrationSyncModes.map((mode) => <option key={mode} value={mode}>{formatCodeLabel(mode)}</option>)}
                      </select>
                    </label>
                    <label className="field planner-name-field">
                      <span>Validation</span>
                      <select value={selectedWorkspaceConnectorDraft.validationPolicy} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, validationPolicy: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                        {integrationValidationPolicies.map((policy) => <option key={policy} value={policy}>{formatCodeLabel(policy)}</option>)}
                      </select>
                    </label>
                    <label className="field planner-name-field">
                      <span>Transform</span>
                      <select value={selectedWorkspaceConnectorDraft.transformationPolicy} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, transformationPolicy: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                        {integrationTransformationPolicies.map((policy) => <option key={policy} value={policy}>{formatCodeLabel(policy)}</option>)}
                      </select>
                    </label>
                    <label className="field checkbox-field">
                      <span>Warehouse Fallback</span>
                      <input className="checkbox-input" type="checkbox" checked={selectedWorkspaceConnectorDraft.allowDefaultWarehouseFallback} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, allowDefaultWarehouseFallback: event.target.checked } }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                    </label>
                  </div>
                  <div className="session-control-row">
                    <label className="field planner-name-field">
                      <span>Sync Interval Minutes</span>
                      <input value={selectedWorkspaceConnectorDraft.syncIntervalMinutes} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, syncIntervalMinutes: event.target.value } }))} placeholder={selectedWorkspaceConnectorDraft.syncMode === 'SCHEDULED_PULL' ? '60' : 'Only used for scheduled pull'} inputMode="numeric" disabled={accessAdminState.loading || !canManageTenantAccess || selectedWorkspaceConnectorDraft.syncMode !== 'SCHEDULED_PULL'} />
                    </label>
                    <label className="field planner-name-field">
                      <span>Support Owner</span>
                      <select value={selectedWorkspaceConnectorDraft.supportOwnerActorName} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, supportOwnerActorName: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                        <option value="">Unassigned</option>
                        {selectedWorkspaceConnectorOwnerOptions.map((operator) => <option key={operator.id} value={operator.actorName}>{operator.displayName}</option>)}
                      </select>
                    </label>
                    <label className="field planner-name-field">
                      <span>Support Notes</span>
                      <input value={selectedWorkspaceConnectorDraft.notes} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, notes: event.target.value } }))} placeholder="Connector ownership or escalation notes" disabled={accessAdminState.loading || !canManageTenantAccess} />
                    </label>
                  </div>
                  <div className="history-action-row">
                    <button className="ghost-button" onClick={() => saveWorkspaceConnectorSupport(selectedWorkspaceConnector.id)} disabled={accessAdminState.loading || !canManageTenantAccess || (selectedWorkspaceConnectorDraft.syncMode === 'SCHEDULED_PULL' && Number.parseInt(selectedWorkspaceConnectorDraft.syncIntervalMinutes, 10) < 15)} type="button">Save Connector Policy</button>
                  </div>
                  <p className="muted-text">{selectedWorkspaceConnector.supportOwnerDisplayName ? `Current owner: ${selectedWorkspaceConnector.supportOwnerDisplayName}` : 'No active support owner assigned yet.'}</p>
                </>
              ) : <div className="empty-state">Select a connector lane to tune sync policy, ownership, and fallback behavior.</div>}
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderPlatformAdminExperience() {
    if (!isAuthenticated || !isPlatformPage) {
      return null
    }

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Platform admin</p><h2>Cross-tenant overview and release trust</h2></div>
            <span className={`panel-badge ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'audit-badge'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Tenants" value={tenantDirectoryState.items.length} accent="blue" />
            <SummaryCard label="Incidents" value={systemIncidents.length} accent="rose" />
            <SummaryCard label="Pending dispatch" value={runtime?.backbone?.pendingDispatchCount ?? 0} accent="amber" />
            <SummaryCard label="Replay pressure" value={pendingReplayCount} accent="teal" />
          </div>
          <div className="experience-grid experience-grid-three">
            <article className="stack-card section-card" id="platform-portfolio">
              <div className="stack-title-row">
                <strong>Tenant portfolio</strong>
                <span className="scenario-type-tag">{tenantDirectoryState.items.length}</span>
              </div>
              <div className="signal-list">
                {tenantDirectoryState.items.length ? tenantDirectoryState.items.map((tenant) => (
                  <button
                    key={tenant.code}
                    className={`signal-list-item selectable-card ${selectedTenantPortfolio?.code === tenant.code ? 'is-selected' : ''}`}
                    onClick={() => setSelectedTenantPortfolioCode(tenant.code)}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{tenant.name}</strong>
                      <span className={`status-tag ${signedInSession?.tenantCode === tenant.code ? 'status-success' : 'status-partial'}`}>{tenant.code}</span>
                    </div>
                    <p>{tenant.description || 'Operational workspace ready for rollout.'}</p>
                    <p className="muted-text">Cross-tenant posture can be managed from the global admin lane.</p>
                  </button>
                )) : <div className="empty-state">Tenant portfolio visibility will appear here as workspaces are created.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Platform incidents</strong>
                <span className="scenario-type-tag">{systemIncidents.length}</span>
              </div>
              <div className="signal-list">
                {systemIncidents.length ? systemIncidents.slice(0, 4).map((incident) => (
                  <div key={incident.incidentKey} className="signal-list-item">
                    <div className="stack-title-row">
                      <strong>{incident.title}</strong>
                      <span className={`status-tag ${getIncidentStatusClassName(incident.severity)}`}>{formatCodeLabel(incident.severity)}</span>
                    </div>
                    <p>{incident.detail}</p>
                    <p className="muted-text">{formatTimestamp(incident.createdAt)}</p>
                  </div>
                )) : <div className="empty-state">No active cross-tenant incidents are visible right now.</div>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Release trust</strong>
                <span className={`status-tag ${runtime?.secureSessionCookies ? 'status-success' : 'status-partial'}`}>{runtime?.secureSessionCookies ? 'Secure cookies' : 'Local HTTP'}</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Backend build</strong>
                  <p>{formatBuildValue(runtime?.build?.version)}</p>
                  <p className="muted-text">Commit {formatBuildValue(runtime?.build?.commit)} | Observed {formatTimestamp(runtime?.observedAt)}</p>
                </div>
                <div className="signal-list-item">
                  <strong>Dispatch posture</strong>
                  <p>Pending {runtime?.backbone?.pendingDispatchCount ?? 0} | Failed {runtime?.backbone?.failedDispatchCount ?? 0}</p>
                  <p className="muted-text">Use this lane to watch backbone pressure before it turns into tenant-facing impact.</p>
                </div>
              </div>
            </article>
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card" id="platform-focus">
              <div className="stack-title-row">
                <strong>Tenant focus</strong>
                <span className="scenario-type-tag">{selectedTenantPortfolio?.code || 'Waiting'}</span>
              </div>
              {selectedTenantPortfolio ? (
                <div className="signal-list">
                  <div className="signal-list-item">
                    <strong>{selectedTenantPortfolio.name}</strong>
                    <p>{selectedTenantPortfolio.description || 'Operational workspace ready for rollout.'}</p>
                    <p className="muted-text">Tenant code {selectedTenantPortfolio.code}</p>
                    <p className="muted-text">{signedInSession?.tenantCode === selectedTenantPortfolio.code ? 'Current signed-in tenant workspace.' : 'Cross-tenant portfolio visibility only.'}</p>
                  </div>
                </div>
              ) : <div className="empty-state">Select a tenant workspace to inspect its rollout posture and cross-tenant context.</div>}
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Platform response posture</strong>
                <span className="scenario-type-tag">{runtime?.overallStatus || 'Loading'}</span>
              </div>
              <div className="utility-metric-grid">
                <div><span>Tenants</span><strong>{tenantDirectoryState.items.length}</strong></div>
                <div><span>Incidents</span><strong>{systemIncidents.length}</strong></div>
                <div><span>Queued</span><strong>{runtime?.backbone?.pendingDispatchCount ?? 0}</strong></div>
                <div><span>Replay</span><strong>{pendingReplayCount}</strong></div>
              </div>
              <div className="history-action-row">
                <button className="ghost-button" onClick={() => navigateToPage('releases')} type="button">Open Releases</button>
                <button className="ghost-button" onClick={() => navigateToPage('runtime')} type="button">Open Runtime</button>
              </div>
              <p className="muted-text">This lane should help platform owners judge tenant rollout health against incidents, queue pressure, and release trust in one view.</p>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderSystemConfigExperience() {
    if (!isAuthenticated || !isSystemConfigPage) {
      return null
    }

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">System configuration</p><h2>Runtime defaults, dispatch cadence, and control envelope</h2></div>
            <span className="panel-badge audit-badge">{runtime?.activeProfiles?.join(', ') || 'Loading'}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Dispatch interval" value={runtime ? `${runtime.backbone.dispatchIntervalMs} ms` : '...'} accent="blue" />
            <SummaryCard label="Batch size" value={runtime?.backbone?.batchSize ?? 0} accent="teal" />
            <SummaryCard label="Simulation interval" value={runtime ? `${runtime.simulationIntervalMs} ms` : '...'} accent="amber" />
            <SummaryCard label="Allowed origins" value={runtime?.allowedOrigins?.length ?? 0} accent="rose" />
          </div>
          <div className="approval-board">
            <div className="stack-card">
              <div className="stack-title-row"><strong>Realtime and queue backbone</strong><span className="status-tag status-success">Configured</span></div>
              <p>Dispatch queue drains every {runtime?.backbone?.dispatchIntervalMs ?? '...'} ms in batches of {runtime?.backbone?.batchSize ?? '...'}.</p>
              <p className="muted-text">Oldest queued work {runtime?.backbone?.oldestPendingAgeSeconds == null ? 'clear' : `${runtime.backbone.oldestPendingAgeSeconds}s`} | Failed dispatch {runtime?.backbone?.failedDispatchCount ?? 0}</p>
            </div>
            <div className="stack-card">
              <div className="stack-title-row"><strong>Session and origin posture</strong><span className={`status-tag ${runtime?.secureSessionCookies ? 'status-success' : 'status-partial'}`}>{runtime?.secureSessionCookies ? 'Secure' : 'Local HTTP'}</span></div>
              <p>Allowed origins {runtime?.allowedOrigins?.join(', ') || 'Loading'}.</p>
              <p className="muted-text">{runtime?.headerFallbackEnabled ? 'Header fallback remains available.' : 'Session-only tenant resolution is active.'}</p>
            </div>
          </div>
          <div className="experience-grid experience-grid-three">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Operational defaults</strong>
                <span className="scenario-type-tag">{runtime?.activeProfiles?.join(', ') || 'Loading'}</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Simulation cadence</strong>
                  <p>{runtime ? `${runtime.simulationIntervalMs} ms` : '...'}</p>
                  <p className="muted-text">Controls the local live-stream behavior used for product verification and scenario movement.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Header fallback</strong>
                  <p>{runtime?.headerFallbackEnabled ? 'Enabled' : 'Disabled'}</p>
                  <p className="muted-text">Session-only tenant resolution is the secure production target.</p>
                </div>
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Dispatch envelope</strong>
                <span className="scenario-type-tag">Queue-backed</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Queued now</strong>
                  <p>{runtime?.backbone?.pendingDispatchCount ?? 0}</p>
                  <p className="muted-text">Tracks the internal operational dispatch queue used to fan out state changes safely.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Processed total</strong>
                  <p>{formatMetricValue(runtime?.metrics?.dispatchProcessed)}</p>
                  <p className="muted-text">Use together with failures and oldest age to decide when to intervene operationally.</p>
                </div>
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Origin and cookie posture</strong>
                <span className="scenario-type-tag">{runtime?.allowedOrigins?.length ?? 0} origins</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Allowed origins</strong>
                  <p>{runtime?.allowedOrigins?.join(', ') || 'Loading'}</p>
                  <p className="muted-text">Review this before rollout to ensure browser sessions, CORS, and realtime connect cleanly.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Session cookies</strong>
                  <p>{runtime?.secureSessionCookies ? 'Secure cookies enabled' : 'Local HTTP cookie posture'}</p>
                  <p className="muted-text">Flip to secure cookies in public deployment so tenant sessions behave correctly over HTTPS.</p>
                </div>
              </div>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderReleaseExperience() {
    if (!isAuthenticated || !isReleasesPage) {
      return null
    }

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Release and environment</p><h2>Deployment fingerprint, uptime posture, and environment trust</h2></div>
            <span className={`panel-badge ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'audit-badge'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Backend version" value={formatBuildValue(runtime?.build?.version)} accent="blue" />
            <SummaryCard label="Frontend version" value={formatBuildValue(frontendBuildVersion)} accent="teal" />
            <SummaryCard label="Commit" value={formatBuildValue(runtime?.build?.commit).slice(0, 7)} accent="amber" />
            <SummaryCard label="Profile" value={runtime?.activeProfiles?.join(', ') || '...'} accent="rose" />
          </div>
          <div className="approval-board" id="releases-builds">
            <div className="stack-card">
              <div className="stack-title-row"><strong>Backend build</strong><span className="status-tag status-success">{formatBuildValue(runtime?.build?.version)}</span></div>
              <p>Commit {formatBuildValue(runtime?.build?.commit)} | Built {formatBuildValue(runtime?.build?.builtAt)}</p>
              <p className="muted-text">Observed {formatTimestamp(runtime?.observedAt)}</p>
            </div>
            <div className="stack-card">
              <div className="stack-title-row"><strong>Frontend build</strong><span className="status-tag status-success">{formatBuildValue(frontendBuildVersion)}</span></div>
              <p>Commit {formatBuildValue(frontendBuildCommit)} | Built {formatBuildValue(frontendBuildTime)}</p>
              <p className="muted-text">API {apiUrl} | WS {wsUrl}</p>
            </div>
          </div>
          <div className="experience-grid experience-grid-three">
            <article className="stack-card section-card" id="releases-checklist">
              <div className="stack-title-row">
                <strong>Environment checklist</strong>
                <span className="scenario-type-tag">{runtime?.activeProfiles?.join(', ') || 'Loading'}</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Runtime readiness</strong>
                  <p>{runtime ? formatCodeLabel(runtime.readinessState) : 'Loading'}</p>
                  <p className="muted-text">The environment should report UP before teams start using the control center live.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Realtime endpoint</strong>
                  <p>{wsUrl}</p>
                  <p className="muted-text">This must align with the frontend runtime config for live updates and incident lanes.</p>
                </div>
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Release handoff</strong>
                <span className="scenario-type-tag">Operational</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Build fingerprint</strong>
                  <p>Backend {formatBuildValue(runtime?.build?.version)} | Frontend {formatBuildValue(frontendBuildVersion)}</p>
                  <p className="muted-text">Operators and support teams can correlate incidents, logs, and dashboards to the same release.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Observed time</strong>
                  <p>{formatTimestamp(runtime?.observedAt)}</p>
                  <p className="muted-text">Use this as the trust checkpoint after every rollout or environment restart.</p>
                </div>
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Go-live steps</strong>
                <span className="scenario-type-tag">Checklist</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>1. Verify runtime</strong>
                  <p>Check health, runtime, incidents, and Prometheus before handing the environment to operators.</p>
                </div>
                <div className="signal-list-item">
                  <strong>2. Verify workspace flows</strong>
                  <p>Sign in, open dashboard, test integrations, and confirm planning and approval actions on the live stack.</p>
                </div>
                <div className="signal-list-item">
                  <strong>3. Confirm traceability</strong>
                  <p>Audit, events, incidents, and release fingerprint should all line up to the same environment version.</p>
                </div>
              </div>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderProfileExperience() {
    if (!isAuthenticated || !isProfilePage) {
      return null
    }

    const sessionHealthNeedsAction = passwordChangeRequired || passwordRotationRequired
    const sessionQuickActions = [
      { title: 'Open alerts', note: `${activeAlerts.length} active alert${activeAlerts.length === 1 ? '' : 's'} in the workspace`, target: 'alerts' },
      { title: 'Open approvals', note: `${pendingApprovalScenarios.length} decision${pendingApprovalScenarios.length === 1 ? '' : 's'} waiting on review`, target: 'approvals' },
      { title: 'Open runtime', note: `${systemIncidents.length} runtime incident${systemIncidents.length === 1 ? '' : 's'} visible`, target: 'runtime' },
    ]

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Profile</p><h2>Personal access and session posture</h2></div>
            <span className="panel-badge audit-badge">{signedInSession ? 'Active' : 'Signed Out'}</span>
          </div>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Roles" value={signedInRoles.length} accent="blue" />
            <SummaryCard label="Warehouse scopes" value={signedInWarehouseScopes.length || 'All'} accent="teal" />
            <SummaryCard label="Action pressure" value={sessionHealthNeedsAction ? 'Attention' : 'Healthy'} accent="amber" />
            <SummaryCard label="Live notices" value={activeAlerts.length + systemIncidents.length} accent="rose" />
          </div>
          <div className="profile-grid">
            <article className="stack-card">
              <div className="stack-title-row">
                <strong>{signedInSession?.displayName || 'No active session'}</strong>
                <span className={`status-tag ${sessionHealthNeedsAction ? 'status-failure' : 'status-success'}`}>{sessionHealthNeedsAction ? 'Action Needed' : 'Healthy'}</span>
              </div>
              <p>{signedInSession ? `${signedInSession.username} in ${signedInSession.tenantName || signedInSession.tenantCode}` : 'Sign in to manage your operator identity.'}</p>
              <p className="muted-text">Actor {signedInSession?.actorName || 'Unavailable'} | Roles {signedInRoles.length ? signedInRoles.map((role) => formatCodeLabel(role)).join(', ') : 'None'}</p>
              <p className="muted-text">Warehouse scope {signedInWarehouseScopes.length ? signedInWarehouseScopes.join(', ') : 'All warehouses in tenant scope'}.</p>
              <p className="muted-text">Session expires {signedInSessionExpiresAt ? formatTimestamp(signedInSessionExpiresAt) : 'per tenant policy'}.</p>
              <p className="muted-text">Password expires {signedInPasswordExpiresAt ? formatTimestamp(signedInPasswordExpiresAt) : 'per tenant policy'}.</p>
            </article>
            <article className="stack-card">
              <div className="stack-title-row">
                <strong>Change password</strong>
                <span className="scenario-type-tag">Secure session</span>
              </div>
              <div className="session-control-row">
                <label className="field session-field">
                  <span>Current Password</span>
                  <input
                    type="password"
                    value={passwordChangeState.form.currentPassword}
                    onChange={(event) => setPasswordChangeState((current) => ({ ...current, form: { ...current.form, currentPassword: event.target.value } }))}
                    placeholder="Enter current password"
                    disabled={passwordChangeState.loading}
                  />
                </label>
                <label className="field session-field">
                  <span>New Password</span>
                  <input
                    type="password"
                    value={passwordChangeState.form.newPassword}
                    onChange={(event) => setPasswordChangeState((current) => ({ ...current, form: { ...current.form, newPassword: event.target.value } }))}
                    placeholder="Choose a stronger password"
                    disabled={passwordChangeState.loading}
                  />
                </label>
                <label className="field session-field">
                  <span>Confirm Password</span>
                  <input
                    type="password"
                    value={passwordChangeState.form.confirmPassword}
                    onChange={(event) => setPasswordChangeState((current) => ({ ...current, form: { ...current.form, confirmPassword: event.target.value } }))}
                    placeholder="Repeat new password"
                    disabled={passwordChangeState.loading}
                  />
                </label>
              </div>
              <div className="history-action-row">
                <button
                  className="secondary-button"
                  onClick={changeSignedInPassword}
                  disabled={
                    passwordChangeState.loading
                    || passwordChangeState.form.currentPassword.trim().length < 8
                    || passwordChangeState.form.newPassword.trim().length < 8
                    || passwordChangeState.form.newPassword !== passwordChangeState.form.confirmPassword
                  }
                  type="button"
                >
                  {passwordChangeState.loading ? 'Updating Password...' : 'Update Password'}
                </button>
                <button className="ghost-button" onClick={signOutOperator} disabled={authSessionState.loading} type="button">
                  {authSessionState.action === 'signout' ? 'Signing Out...' : 'Sign Out'}
                </button>
              </div>
              {passwordChangeState.error ? <p className="error-text">{passwordChangeState.error}</p> : null}
              {passwordChangeState.success ? <p className="success-text">{passwordChangeState.success}</p> : null}
            </article>
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Session hygiene</strong>
                <span className={`status-tag ${sessionHealthNeedsAction ? 'status-failure' : 'status-success'}`}>{sessionHealthNeedsAction ? 'Needs action' : 'Healthy'}</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Password posture</strong>
                  <p>{passwordChangeRequired ? 'Password change required before the next secure action.' : passwordRotationRequired ? 'Password rotation window has elapsed.' : 'Password is inside policy.'}</p>
                  <p className="muted-text">Expires {signedInPasswordExpiresAt ? formatTimestamp(signedInPasswordExpiresAt) : 'per tenant policy'}.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Session expiry</strong>
                  <p>{signedInSessionExpiresAt ? formatTimestamp(signedInSessionExpiresAt) : 'Controlled by tenant security policy.'}</p>
                  <p className="muted-text">Use sign out when leaving a live operations console to avoid stale control access.</p>
                </div>
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Quick routes</strong>
                <span className="scenario-type-tag">{sessionQuickActions.length}</span>
              </div>
              <div className="signal-list">
                {sessionQuickActions.map((action) => (
                  <button key={action.title} className="utility-action" onClick={() => navigateToPage(action.target)} type="button">
                    <span>Workspace action</span>
                    <strong>{action.title}</strong>
                    <p>{action.note}</p>
                  </button>
                ))}
              </div>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderTenantManagementExperience() {
    if (!isAuthenticated || !isTenantsPage) {
      return null
    }

    const currentTenant = tenantDirectoryState.items.find((tenant) => tenant.code === signedInSession?.tenantCode)

    return (
      <section className="content-grid">
        <article className="panel panel-wide">
          <div className="panel-header">
            <div><p className="panel-kicker">Tenant management</p><h2>Bootstrap and monitor workspace rollout</h2></div>
            <span className="panel-badge scenario-badge">{tenantDirectoryState.items.length}</span>
          </div>
          <p className="muted-text integration-note">
            {signedInRoles.includes('TENANT_ADMIN')
              ? 'Create a new tenant workspace with starter warehouses, connector posture, an admin account, and an approval lane.'
              : 'Tenant creation is restricted to tenant admins with rollout authority.'}
          </p>
          <div className="summary-grid compact-summary-grid">
            <SummaryCard label="Visible workspaces" value={tenantDirectoryState.items.length} accent="blue" />
            <SummaryCard label="Admin access" value={signedInRoles.includes('TENANT_ADMIN') ? 'Granted' : 'Restricted'} accent="teal" />
            <SummaryCard label="Current workspace" value={currentTenant?.code || 'None'} accent="amber" />
            <SummaryCard label="Last rollout" value={tenantOnboardingState.result?.tenantCode || 'Waiting'} accent="rose" />
          </div>
          <div className="tenant-rollout-grid">
            <article className="stack-card">
              <div className="stack-title-row">
                <strong>New workspace</strong>
                <span className={`status-tag ${signedInRoles.includes('TENANT_ADMIN') ? 'status-success' : 'status-failure'}`}>
                  {signedInRoles.includes('TENANT_ADMIN') ? 'Ready' : 'Restricted'}
                </span>
              </div>
              <div className="session-control-row">
                <label className="field session-field">
                  <span>Tenant Code</span>
                  <input value={tenantOnboardingForm.tenantCode} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, tenantCode: event.target.value }))} placeholder="ACME-OPS" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} />
                </label>
                <label className="field session-field">
                  <span>Tenant Name</span>
                  <input value={tenantOnboardingForm.tenantName} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, tenantName: event.target.value }))} placeholder="Acme Operations" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} />
                </label>
                <label className="field session-field">
                  <span>Description</span>
                  <input value={tenantOnboardingForm.description} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, description: event.target.value }))} placeholder="Regional operating workspace" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} />
                </label>
                <label className="field session-field">
                  <span>Admin Full Name</span>
                  <input value={tenantOnboardingForm.adminFullName} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, adminFullName: event.target.value }))} placeholder="Amina Dlamini" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} />
                </label>
                <label className="field session-field">
                  <span>Admin Username</span>
                  <input value={tenantOnboardingForm.adminUsername} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, adminUsername: event.target.value }))} placeholder="amina.admin" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} />
                </label>
                <label className="field session-field">
                  <span>Admin Password</span>
                  <input type="password" value={tenantOnboardingForm.adminPassword} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, adminPassword: event.target.value }))} placeholder="Choose a strong bootstrap password" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} />
                </label>
                <label className="field session-field">
                  <span>Primary Location</span>
                  <input value={tenantOnboardingForm.primaryLocation} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, primaryLocation: event.target.value }))} placeholder="Johannesburg" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} />
                </label>
                <label className="field session-field">
                  <span>Secondary Location</span>
                  <input value={tenantOnboardingForm.secondaryLocation} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, secondaryLocation: event.target.value }))} placeholder="Cape Town" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} />
                </label>
              </div>
              <div className="history-action-row">
                <button
                  className="secondary-button"
                  onClick={onboardTenant}
                  disabled={
                    tenantOnboardingState.loading
                    || !signedInRoles.includes('TENANT_ADMIN')
                    || !tenantOnboardingForm.tenantCode.trim()
                    || !tenantOnboardingForm.tenantName.trim()
                    || !tenantOnboardingForm.adminFullName.trim()
                    || !tenantOnboardingForm.adminUsername.trim()
                    || !tenantOnboardingForm.adminPassword.trim()
                    || !tenantOnboardingForm.primaryLocation.trim()
                  }
                  type="button"
                >
                  {tenantOnboardingState.loading ? 'Creating Workspace...' : 'Create Tenant Workspace'}
                </button>
                {tenantOnboardingState.result ? (
                  <button
                    className="ghost-button"
                    onClick={signInOperator}
                    disabled={authSessionState.loading || authSessionState.username.trim() !== tenantOnboardingState.result.adminUsername || authSessionState.tenantCode.trim() !== tenantOnboardingState.result.tenantCode}
                    type="button"
                  >
                    {authSessionState.action === 'signin' ? 'Opening Workspace...' : 'Continue As Workspace Admin'}
                  </button>
                ) : null}
              </div>
              {tenantOnboardingState.error ? <p className="error-text">{tenantOnboardingState.error}</p> : null}
              {tenantOnboardingState.success ? <p className="success-text">{tenantOnboardingState.success}</p> : null}
              {tenantOnboardingState.result ? (
                <p className="muted-text">
                  Executive approver sign-in: {tenantOnboardingState.result.executiveUsername} / {tenantOnboardingState.result.executivePassword} | Warehouses {tenantOnboardingState.result.starterWarehouseCodes.join(', ')}
                </p>
              ) : null}
            </article>
            <article className="stack-card">
              <div className="stack-title-row">
                <strong>Existing workspaces</strong>
                <span className="status-tag status-partial">Portfolio</span>
              </div>
              <div className="stack-list tenant-list-stack">
                {tenantDirectoryState.items.length ? tenantDirectoryState.items.map((tenant) => (
                  <div key={tenant.code} className="stack-card stack-card-compact">
                    <div className="stack-title-row">
                      <strong>{tenant.name}</strong>
                      <span className={`status-tag ${signedInSession?.tenantCode === tenant.code ? 'status-success' : 'status-partial'}`}>
                        {signedInSession?.tenantCode === tenant.code ? 'Current' : tenant.code}
                      </span>
                    </div>
                    <p>{tenant.code}</p>
                    <div className="history-action-row">
                      <button className="ghost-button" onClick={() => setAuthSessionState((current) => ({ ...current, tenantCode: tenant.code }))} type="button">Set Sign-In Target</button>
                    </div>
                  </div>
                )) : <div className="empty-state">{tenantDirectoryState.loading ? 'Loading tenant portfolio...' : 'Tenant workspaces will appear here after the first rollout.'}</div>}
              </div>
            </article>
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Rollout checklist</strong>
                <span className="scenario-type-tag">{tenantOnboardingState.result ? 'Recent rollout' : 'Ready'}</span>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>1. Create workspace</strong>
                  <p>Register a tenant code, name, admin identity, and starter locations so the company has its own operating boundary.</p>
                </div>
                <div className="signal-list-item">
                  <strong>2. Open admin session</strong>
                  <p>Continue as the new workspace admin, tune settings, assign access lanes, and review connector posture.</p>
                </div>
                <div className="signal-list-item">
                  <strong>3. Move into live operations</strong>
                  <p>Connect integrations, onboard operators, and shift the team into dashboard, alerts, approvals, and runtime trust workflows.</p>
                </div>
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Portfolio posture</strong>
                <span className="scenario-type-tag">{tenantDirectoryState.items.length}</span>
              </div>
              <div className="signal-list">
                {tenantDirectoryState.items.length ? tenantDirectoryState.items.slice(0, 4).map((tenant) => (
                  <div key={tenant.code} className="signal-list-item">
                    <strong>{tenant.name}</strong>
                    <p>{tenant.code}</p>
                    <p className="muted-text">{signedInSession?.tenantCode === tenant.code ? 'Current workspace session is operating here.' : 'Ready for sign-in targeting and rollout operations.'}</p>
                  </div>
                )) : <div className="empty-state">Workspace portfolio cards will appear here as tenant rollout begins.</div>}
              </div>
            </article>
          </div>
        </article>
      </section>
    )
  }

  function renderWorkspaceUtilityRail() {
    if (!isAuthenticated) {
      return null
    }

    const recommendationCandidates = [...recommendationNow, ...recommendationSoon, ...recommendationWatch]
    const focusedAlert = activeAlerts.find((alert) => alert.id === selectedAlertId) || activeAlerts[0]
    const focusedRecommendation = recommendationCandidates.find((recommendation) => recommendation.id === selectedRecommendationId) || recommendationCandidates[0]
    const focusedOrder = snapshot.recentOrders.find((order) => order.id === selectedOrderId) || snapshot.recentOrders[0]
    const focusedInventory = snapshot.inventory.find((item) => item.id === selectedInventoryId) || highRiskInventory[0] || lowStockInventory[0] || snapshot.inventory[0]
    const pageUtilityContext = (() => {
      switch (currentPage) {
        case 'alerts':
          return {
            kicker: 'Alert focus',
            state: focusedAlert ? formatCodeLabel(focusedAlert.severity) : 'Clear',
            title: focusedAlert ? focusedAlert.title : 'No active alerts',
            detail: focusedAlert ? focusedAlert.recommendedAction || focusedAlert.impactSummary : 'This lane stays quiet until operational risk begins forming.',
            metrics: [
              { label: 'Critical', value: activeAlerts.filter((alert) => alert.severity === 'CRITICAL').length },
              { label: 'High', value: activeAlerts.filter((alert) => alert.severity === 'HIGH').length },
              { label: 'Warehouses', value: new Set(activeAlerts.map((alert) => alert.warehouseCode).filter(Boolean)).size },
              { label: 'Actionable', value: activeAlerts.filter((alert) => Boolean(alert.recommendedAction)).length },
            ],
            actions: [
              { title: 'Open recommendations', note: 'Move from warning to next action.', target: 'recommendations' },
            ],
          }
        case 'recommendations':
          return {
            kicker: 'Action focus',
            state: focusedRecommendation ? focusedRecommendation.priority : 'Clear',
            title: focusedRecommendation ? focusedRecommendation.title : 'No active recommendations',
            detail: focusedRecommendation ? focusedRecommendation.description : 'Guided actions will appear here as the system starts ranking what operators should do next.',
            metrics: [
              { label: 'Urgent', value: recommendationNow.length },
              { label: 'Soon', value: recommendationSoon.length },
              { label: 'Watch', value: recommendationWatch.length },
              { label: 'Alerts', value: activeAlerts.length },
            ],
            actions: [
              { title: 'Open alerts', note: 'See the risk that is driving action guidance.', target: 'alerts' },
            ],
          }
        case 'orders':
          return {
            kicker: 'Order focus',
            state: focusedOrder ? focusedOrder.warehouseCode || 'Tenant-wide' : 'Waiting',
            title: focusedOrder ? focusedOrder.externalOrderId : 'No active order lane',
            detail: focusedOrder ? `${focusedOrder.warehouseName} | ${focusedOrder.itemCount} units | ${currency.format(focusedOrder.totalAmount)}` : 'Orders will appear here as soon as the live flow starts moving.',
            metrics: [
              { label: 'Recent', value: snapshot.recentOrders.length },
              { label: 'Warehouses', value: new Set(snapshot.recentOrders.map((order) => order.warehouseCode).filter(Boolean)).size },
              { label: 'Delayed', value: snapshot.recentOrders.filter((order) => fulfillmentOverview.activeFulfillments.some((task) => task.externalOrderId === order.externalOrderId && task.fulfillmentStatus === 'DELAYED')).length },
              { label: 'Backlog', value: fulfillmentOverview.backlogCount },
            ],
            actions: [
              { title: 'Open fulfillment', note: 'Follow the downstream dispatch and delivery lane.', target: 'fulfillment' },
            ],
          }
        case 'inventory':
          return {
            kicker: 'Inventory focus',
            state: focusedInventory ? formatCodeLabel(focusedInventory.riskLevel) : 'Waiting',
            title: focusedInventory ? focusedInventory.productName : 'No active inventory focus',
            detail: focusedInventory ? `${focusedInventory.warehouseName} | ${focusedInventory.quantityAvailable} available | Stockout ${formatRelativeHours(focusedInventory.hoursToStockout)}` : 'Inventory intelligence becomes active as stock posture and velocity start diverging.',
            metrics: [
              { label: 'Low stock', value: lowStockInventory.length },
              { label: 'High risk', value: highRiskInventory.length },
              { label: 'Fast movers', value: fastMovingInventory.length },
              { label: 'Warehouses', value: warehouseOptions.length },
            ],
            actions: [
              { title: 'Open locations', note: 'View site-level pressure behind the inventory lane.', target: 'locations' },
            ],
          }
        case 'runtime':
          return {
            kicker: 'Runtime focus',
            state: selectedRuntimeIncident ? formatCodeLabel(selectedRuntimeIncident.severity) : (runtime?.overallStatus || 'Loading'),
            title: selectedRuntimeIncident ? selectedRuntimeIncident.title : 'Runtime health board',
            detail: selectedRuntimeIncident
              ? `${selectedRuntimeIncident.context} | ${formatTimestamp(selectedRuntimeIncident.createdAt)}`
              : runtime
                ? `Readiness ${formatCodeLabel(runtime.readinessState)} | Pending ${runtime.backbone.pendingDispatchCount} | Failed ${runtime.backbone.failedDispatchCount}`
                : 'Runtime, queue, and incident posture will appear here once the trust surface is loaded.',
            metrics: [
              { label: 'Incidents', value: systemIncidents.length },
              { label: 'Queued', value: runtime?.backbone?.pendingDispatchCount ?? 0 },
              { label: 'Failed', value: runtime?.backbone?.failedDispatchCount ?? 0 },
              { label: 'Metrics', value: formatMetricValue(runtime?.metrics?.dispatchProcessed) },
            ],
            actions: [
              { title: 'Open releases', note: 'Check deployment fingerprint beside runtime trust.', target: 'releases' },
            ],
          }
        case 'audit':
          return {
            kicker: 'Trace focus',
            state: selectedAuditTrace ? formatCodeLabel(selectedAuditTrace.traceType) : 'Waiting',
            title: selectedAuditTrace
              ? selectedAuditTrace.traceType === 'audit'
                ? formatCodeLabel(selectedAuditTrace.action)
                : formatCodeLabel(selectedAuditTrace.eventType)
              : 'No trace selected',
            detail: selectedAuditTrace
              ? selectedAuditTrace.traceType === 'audit'
                ? `${selectedAuditTrace.targetType} | ${selectedAuditTrace.targetRef} | ${formatTimestamp(selectedAuditTrace.createdAt)}`
                : `${selectedAuditTrace.source} | ${formatTimestamp(selectedAuditTrace.createdAt)}`
              : 'Audit and business timeline entries will appear here as soon as protected actions and live events accumulate.',
            metrics: [
              { label: 'Audit', value: snapshot.auditLogs.length },
              { label: 'Events', value: snapshot.recentEvents.length },
              { label: 'Replay', value: pendingReplayCount },
              { label: 'Incidents', value: systemIncidents.length },
            ],
            actions: [
              { title: 'Open runtime', note: 'Pair traceability with the live trust lane.', target: 'runtime' },
            ],
          }
        case 'users':
          return {
            kicker: 'Access focus',
            state: selectedAccessSubject ? formatCodeLabel(selectedAccessSubject.subjectType) : 'Waiting',
            title: selectedAccessSubject
              ? selectedAccessSubject.subjectType === 'operator'
                ? selectedAccessSubject.displayName
                : selectedAccessSubject.fullName
              : 'No access subject selected',
            detail: selectedAccessSubject
              ? selectedAccessSubject.subjectType === 'operator'
                ? `${selectedAccessSubject.actorName} | ${selectedAccessSubject.roles.map((role) => formatCodeLabel(role)).join(', ') || 'No roles'}`
                : `${selectedAccessSubject.username} | ${selectedAccessSubject.operatorDisplayName || selectedAccessSubject.operatorActorName}`
              : 'Operator lanes and user accounts will surface here as tenant admins start shaping workspace access.',
            metrics: [
              { label: 'Operators', value: accessAdminOperators.length },
              { label: 'Users', value: accessAdminUsers.length },
              { label: 'Resets', value: workspaceAdmin?.supportDiagnostics?.activeUsersRequiringPasswordChange || 0 },
              { label: 'Blocked', value: workspaceAdmin?.supportDiagnostics?.activeUsersBlockedByInactiveOperator || 0 },
            ],
            actions: [
              { title: 'Open settings', note: 'Tune workspace policy alongside access lanes.', target: 'settings' },
            ],
          }
        case 'settings':
          return {
            kicker: 'Workspace focus',
            state: workspaceAdmin?.tenantName ? 'Editable' : 'Loading',
            title: workspaceAdmin?.tenantName || 'Workspace settings',
            detail: workspaceAdmin
              ? `${workspaceAdmin.warehouses?.length || 0} warehouse lanes | ${workspaceAdmin.connectors?.length || 0} connectors | Rotation ${workspaceAdmin.securitySettings?.passwordRotationDays || 0} days`
              : 'Workspace profile, security posture, and connector ownership will appear here once tenant admin data loads.',
            metrics: [
              { label: 'Warehouses', value: workspaceAdmin?.warehouses?.length || 0 },
              { label: 'Connectors', value: workspaceAdmin?.connectors?.length || 0 },
              { label: 'Rotation', value: workspaceAdmin?.securitySettings?.passwordRotationDays || 0 },
              { label: 'Timeout', value: workspaceAdmin?.securitySettings?.sessionTimeoutMinutes || 0 },
            ],
            actions: [
              { title: 'Open users', note: 'Shift from policy to who can act inside it.', target: 'users' },
            ],
          }
        case 'platform':
          return {
            kicker: 'Platform focus',
            state: selectedTenantPortfolio ? selectedTenantPortfolio.code : (runtime?.overallStatus || 'Loading'),
            title: selectedTenantPortfolio ? selectedTenantPortfolio.name : 'Platform overview',
            detail: selectedTenantPortfolio
              ? selectedTenantPortfolio.description || 'Operational workspace ready for rollout.'
              : 'Cross-tenant health, queue posture, and release trust appear here for platform operators.',
            metrics: [
              { label: 'Tenants', value: tenantDirectoryState.items.length },
              { label: 'Incidents', value: systemIncidents.length },
              { label: 'Queued', value: runtime?.backbone?.pendingDispatchCount ?? 0 },
              { label: 'Replay', value: pendingReplayCount },
            ],
            actions: [
              { title: 'Open releases', note: 'Review build and environment trust next.', target: 'releases' },
            ],
          }
        case 'releases':
          return {
            kicker: 'Release focus',
            state: runtime?.overallStatus || 'Loading',
            title: `Backend ${formatBuildValue(runtime?.build?.version)}`,
            detail: `Frontend ${formatBuildValue(frontendBuildVersion)} | Commit ${formatBuildValue(runtime?.build?.commit).slice(0, 7)} | Observed ${formatTimestamp(runtime?.observedAt)}`,
            metrics: [
              { label: 'Profile', value: runtime?.activeProfiles?.join(', ') || 'Loading' },
              { label: 'Readiness', value: runtime ? formatCodeLabel(runtime.readinessState) : 'Loading' },
              { label: 'API', value: apiUrl ? 'Configured' : 'Default' },
              { label: 'WS', value: wsUrl ? 'Configured' : 'Default' },
            ],
            actions: [
              { title: 'Open runtime', note: 'Check live trust after release posture.', target: 'runtime' },
            ],
          }
        case 'scenario-history':
          return {
            kicker: 'Scenario focus',
            state: selectedHistoryScenario ? formatCodeLabel(selectedHistoryScenario.type) : 'Waiting',
            title: selectedHistoryScenario ? selectedHistoryScenario.title : 'No saved plan selected',
            detail: selectedHistoryScenario
              ? `${selectedHistoryScenario.warehouseCode || 'Tenant-wide'} | ${formatCodeLabel(selectedHistoryScenario.approvalStatus)} | ${selectedHistoryScenario.recommendedOption || 'Decision path recorded'}`
              : 'Saved plans, revisions, and compare memory will become actionable here as teams explore scenarios.',
            metrics: [
              { label: 'History', value: scenarioHistoryItems.length },
              { label: 'Revisions', value: scenarioHistoryItems.filter((scenario) => scenario.revisionNumber).length },
              { label: 'Executable', value: scenarioHistoryItems.filter((scenario) => scenario.executable).length },
              { label: 'Escalated', value: scenarioHistoryItems.filter((scenario) => scenario.slaEscalated).length },
            ],
            actions: [
              { title: 'Open planner', note: 'Shift from saved memory back into the decision lab.', target: 'scenarios' },
            ],
          }
        case 'approvals':
          return {
            kicker: 'Approval focus',
            state: selectedApprovalScenario ? formatCodeLabel(selectedApprovalScenario.approvalStage || selectedApprovalScenario.approvalStatus) : 'Clear',
            title: selectedApprovalScenario ? selectedApprovalScenario.title : 'No approval selected',
            detail: selectedApprovalScenario
              ? `${selectedApprovalScenario.reviewOwner || 'Unassigned'} | Due ${formatTimestamp(selectedApprovalScenario.approvalDueAt)}`
              : 'Pending, approved, rejected, and overdue decisions will appear here as soon as control queues begin filling.',
            metrics: [
              { label: 'Pending', value: pendingApprovalScenarios.length },
              { label: 'Overdue', value: overdueScenarios.length },
              { label: 'Approved', value: approvedScenarios.length },
              { label: 'Rejected', value: rejectedScenarios.length },
            ],
            actions: [
              { title: 'Open escalations', note: 'Check the inbox when the queue starts breaching SLA.', target: 'escalations' },
            ],
          }
        case 'escalations':
          return {
            kicker: 'Escalation focus',
            state: selectedEscalationScenario ? 'Escalated' : systemIncidents.length ? 'Incident pressure' : 'Clear',
            title: selectedEscalationScenario ? selectedEscalationScenario.title : systemIncidents[0]?.title || 'No escalations active',
            detail: selectedEscalationScenario
              ? `Escalated to ${selectedEscalationScenario.slaEscalatedTo || 'Monitoring'} | Due ${formatTimestamp(selectedEscalationScenario.approvalDueAt)}`
              : systemIncidents[0]?.detail || 'The escalation inbox remains quiet until SLA or trust pressure needs immediate ownership.',
            metrics: [
              { label: 'Escalated', value: escalatedScenarios.length },
              { label: 'Acknowledged', value: escalatedScenarios.filter((scenario) => scenario.slaAcknowledged).length },
              { label: 'Incidents', value: systemIncidents.length },
              { label: 'Critical', value: systemIncidents.filter((incident) => ['CRITICAL', 'HIGH'].includes(incident.severity)).length },
            ],
            actions: [
              { title: 'Open runtime', note: 'Inspect the trust layer beside the escalation queue.', target: 'runtime' },
            ],
          }
        default:
          return {
            kicker: 'Page focus',
            state: effectivePageMeta.label,
            title: effectivePageMeta.title,
            detail: pageStatusMap[currentPage],
            metrics: [
              { label: 'Focus 1', value: effectivePageMeta.focus[0] || 'Live state' },
              { label: 'Focus 2', value: effectivePageMeta.focus[1] || 'Action' },
              { label: 'Focus 3', value: effectivePageMeta.focus[2] || 'Trust' },
              { label: 'Group', value: effectivePageMeta.group ? formatCodeLabel(effectivePageMeta.group) : 'Workspace' },
            ],
            actions: [],
          }
      }
    })()

    return (
      <aside className="workspace-utility-rail">
        <article className="utility-card" id="workspace-trust-rail">
          <div className="utility-card-header">
            <p className="panel-kicker">Realtime state</p>
            <span className={`utility-state utility-${connectionState}`}>{connectionState === 'live' ? 'Live' : formatCodeLabel(connectionState)}</span>
          </div>
          <strong>{snapshot.simulation.active ? 'Continuous activity is flowing' : 'Monitoring live operating state'}</strong>
          <p className="muted-text">
            {snapshot.generatedAt ? `Snapshot ${formatTimestamp(snapshot.generatedAt)}` : 'Awaiting the first synchronized snapshot.'}
          </p>
          <div className="utility-metric-grid">
            <div><span>Alerts</span><strong>{snapshot.alerts.activeAlerts.length}</strong></div>
            <div><span>Actions</span><strong>{snapshot.recommendations.length}</strong></div>
            <div><span>Replay</span><strong>{pendingReplayCount}</strong></div>
            <div><span>Incidents</span><strong>{systemIncidents.length}</strong></div>
          </div>
        </article>
        <article className="utility-card" id="workspace-page-focus">
          <div className="utility-card-header">
            <p className="panel-kicker">{pageUtilityContext.kicker}</p>
            <span className="utility-state utility-good">{pageUtilityContext.state}</span>
          </div>
          <strong>{pageUtilityContext.title}</strong>
          <p className="muted-text">{pageUtilityContext.detail}</p>
          <div className="utility-metric-grid">
            {pageUtilityContext.metrics.map((metric) => (
              <div key={metric.label}>
                <span>{metric.label}</span>
                <strong>{metric.value}</strong>
              </div>
            ))}
          </div>
          {pageUtilityContext.actions.length ? (
            <div className="stack-list compact-stack-list">
              {pageUtilityContext.actions.map((action) => (
                <button key={action.title} className="utility-action" onClick={() => navigateToPage(action.target)} type="button">
                  <span>Quick route</span>
                  <strong>{action.title}</strong>
                  <p>{action.note}</p>
                </button>
              ))}
            </div>
          ) : null}
        </article>
        <article className="utility-card">
          <div className="utility-card-header">
            <p className="panel-kicker">Act now</p>
            <span className="scenario-type-tag">{urgentActions.length ? `${urgentActions.length} items` : 'Stable'}</span>
          </div>
          <div className="stack-list compact-stack-list">
            {urgentActions.length ? urgentActions.map((action) => (
              <button key={action.id} className="utility-action" onClick={() => navigateToPage(action.target)} type="button">
                <span>{action.kicker}</span>
                <strong>{action.title}</strong>
                <p>{action.note}</p>
              </button>
            )) : <div className="empty-state">No immediate operational action pressure right now.</div>}
          </div>
        </article>
        <article className="utility-card">
          <div className="utility-card-header">
            <p className="panel-kicker">Activity stream</p>
            <span className="scenario-type-tag">{utilityTimeline.length}</span>
          </div>
          <div className="utility-timeline">
            {utilityTimeline.length ? utilityTimeline.map((item) => (
              <div key={item.id} className="utility-timeline-item">
                <strong>{item.title}</strong>
                <p>{item.meta}</p>
                <span>{formatTimestamp(item.timestamp)}</span>
              </div>
            )) : <div className="empty-state">Business activity, incidents, and audit events will begin streaming here as the workspace operates.</div>}
          </div>
        </article>
        <article className="utility-card">
          <div className="utility-card-header">
            <p className="panel-kicker">Operator</p>
            <span className={`utility-state ${passwordChangeRequired || passwordRotationRequired ? 'utility-alert' : 'utility-good'}`}>
              {passwordChangeRequired || passwordRotationRequired ? 'Attention' : 'Healthy'}
            </span>
          </div>
          <strong>{signedInSession.displayName}</strong>
          <p className="muted-text">{signedInSession.tenantName || signedInSession.tenantCode} | {signedInSession.actorName}</p>
          <p className="muted-text">Roles {signedInRoles.length ? signedInRoles.map((role) => formatCodeLabel(role)).join(', ') : 'None'}</p>
          <p className="muted-text">Warehouse scope {signedInWarehouseScopes.length ? signedInWarehouseScopes.join(', ') : 'Tenant-wide'}</p>
        </article>
      </aside>
    )
  }

  function renderPublicExperience() {
    const publicShellClassName = `public-shell public-page-${effectivePageMeta.key}`
    const featureCards = [
      { title: 'Live visibility', body: 'Orders, inventory, locations, fulfillment, incidents, and connectors pulled into one operational picture.' },
      { title: 'Prediction and guidance', body: 'Detect risk early, estimate near-term impact, and surface what the team should do next.' },
      { title: 'Control and trust', body: 'Run scenarios, route approvals, recover failed inbound work, and keep runtime confidence visible.' },
    ]

    if (effectivePageMeta.key === 'sign-in') {
      return (
        <main className={publicShellClassName}>
          <header className="public-topbar">
            <button className="brand-lockup brand-button" onClick={() => navigateToPage('landing')} type="button">
              <span className="brand-mark">S</span>
              <span><strong>SynapseCore</strong><small>Operational intelligence operating system</small></span>
            </button>
            <nav className="public-nav">
              {publicPages.filter((page) => page.key !== 'sign-in').map((page) => (
                <button key={page.key} className="ghost-button" onClick={() => navigateToPage(page.key)} type="button">{page.label}</button>
              ))}
            </nav>
          </header>
          <section className="public-signin-shell">
            <article className="public-signin-story">
              <p className="eyebrow">Operational workspace</p>
              <h1>{effectivePageMeta.title}</h1>
              <p>{effectivePageMeta.description}</p>
              <div className="public-feature-stack">
                {featureCards.map((feature) => (
                  <article key={feature.title} className="public-feature-card">
                    <strong>{feature.title}</strong>
                    <p>{feature.body}</p>
                  </article>
                ))}
              </div>
            </article>
            <article className="public-signin-card">
              <p className="panel-kicker">Sign in</p>
              <h2>Open your company command center</h2>
              <div className="signin-form-grid">
                <label className="field">
                  <span>Tenant workspace</span>
                  <select
                    value={authSessionState.tenantCode}
                    onChange={(event) => setAuthSessionState((current) => ({ ...current, tenantCode: event.target.value }))}
                    disabled={authSessionState.loading || !tenantDirectoryState.items.length}
                  >
                    <option value="">Select a tenant workspace</option>
                    {tenantDirectoryState.items.map((tenant) => <option key={tenant.code} value={tenant.code}>{tenant.name}</option>)}
                  </select>
                </label>
                <label className="field">
                  <span>Username</span>
                  <input
                    type="text"
                    value={authSessionState.username}
                    onChange={(event) => setAuthSessionState((current) => ({ ...current, username: event.target.value }))}
                    placeholder="workspace.admin"
                    disabled={authSessionState.loading}
                  />
                </label>
                <label className="field">
                  <span>Password</span>
                  <input
                    type="password"
                    value={authSessionState.password}
                    onChange={(event) => setAuthSessionState((current) => ({ ...current, password: event.target.value }))}
                    placeholder="Enter workspace password"
                    disabled={authSessionState.loading}
                  />
                </label>
              </div>
              <div className="signin-meta-row">
                <label className="checkbox-field inline-checkbox">
                  <input className="checkbox-input" type="checkbox" checked={rememberWorkspace} onChange={(event) => setRememberWorkspace(event.target.checked)} />
                  <span>Remember this workspace on this device</span>
                </label>
                <button className="ghost-button" type="button">Forgot password</button>
              </div>
              <div className="history-action-row">
                <button className="primary-button" onClick={signInOperator} disabled={authSessionState.loading || !authSessionState.tenantCode.trim() || !authSessionState.username.trim() || !authSessionState.password.trim()} type="button">
                  {authSessionState.action === 'signin' ? 'Signing In...' : 'Sign In'}
                </button>
                <button className="ghost-button" onClick={() => navigateToPage('product')} type="button">Product Overview</button>
                <button className="ghost-button" type="button">Enterprise SSO</button>
              </div>
              <p className="muted-text integration-note">
                {tenantDirectoryState.error
                  ? tenantDirectoryState.error
                  : `Use a valid workspace account for ${tenantDirectoryState.items.find((tenant) => tenant.code === authSessionState.tenantCode)?.name || 'your tenant workspace'}.`}
              </p>
              {authSessionState.error ? <p className="error-text">{authSessionState.error}</p> : null}
            </article>
          </section>
        </main>
      )
    }

    return (
      <main className={publicShellClassName}>
        <header className="public-topbar">
          <button className="brand-lockup brand-button" onClick={() => navigateToPage('landing')} type="button">
            <span className="brand-mark">S</span>
            <span><strong>SynapseCore</strong><small>Operational intelligence operating system</small></span>
          </button>
          <nav className="public-nav">
            {publicPages.filter((page) => page.key !== 'sign-in').map((page) => (
              <button key={page.key} className={`ghost-button ${effectivePageMeta.key === page.key ? 'nav-selected' : ''}`} onClick={() => navigateToPage(page.key)} type="button">{page.label}</button>
            ))}
          </nav>
          <button className="primary-button" onClick={() => navigateToPage('sign-in')} type="button">Sign In</button>
        </header>
        <section className="public-hero">
          <div>
            <p className="eyebrow">Premium operational command center</p>
            <h1>{effectivePageMeta.title}</h1>
            <p className="hero-copy">{effectivePageMeta.description}</p>
            <div className="history-action-row">
              <button className="primary-button" onClick={() => navigateToPage('sign-in')} type="button">Open Workspace</button>
              <button className="ghost-button" onClick={() => navigateToPage(effectivePageMeta.key === 'landing' ? 'product' : 'contact')} type="button">
                {effectivePageMeta.key === 'landing' ? 'Explore Product' : 'Request Walkthrough'}
              </button>
            </div>
          </div>
          <div className="public-hero-metric-grid">
            {featureCards.map((feature, index) => (
              <article key={feature.title} className="public-metric-card">
                <span>{`0${index + 1}`}</span>
                <strong>{feature.title}</strong>
                <p>{feature.body}</p>
              </article>
            ))}
          </div>
        </section>
        {effectivePageMeta.key === 'landing' ? (
          <>
          <section className="public-section-grid">
            <article className="public-section-card">
              <p className="panel-kicker">Why it matters</p>
              <h2>Most systems record operations. SynapseCore turns them into decisions.</h2>
              <p>Bring orders, stock, fulfillment, incidents, integrations, and approvals into one live command center so teams do not have to stitch the business together manually.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">How it works</p>
              <h2>Receive, interpret, predict, guide, and execute.</h2>
              <p>SynapseCore ingests activity, updates the operational model, detects risk, predicts near-term pressure, and routes the right next action through controlled workflows.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">Built for</p>
              <h2>Inventory, fulfillment, logistics, and operations-heavy teams.</h2>
              <p>Use one platform to reduce blind spots, align teams faster, and keep control when pressure rises across sites, systems, and approval lanes.</p>
            </article>
          </section>
          <section className="public-section-grid">
            <article className="public-section-card">
              <p className="panel-kicker">Control center preview</p>
              <h2>See operations, risk, and action on one screen.</h2>
              <p>Executive summary cards, act-now actions, live risk posture, approvals, runtime trust, and event history all sit inside the same operating surface.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">Integrations preview</p>
              <h2>Pull fragmented systems into one operational truth.</h2>
              <p>Connectors, import history, failed inbound replay, and support ownership remain visible instead of becoming invisible scripts around the business.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">Go-live path</p>
              <h2>Roll out by workspace, not by chaos.</h2>
              <p>Create tenant workspaces, onboard users and warehouse lanes, tune security, and move teams into a clean operational control model.</p>
            </article>
          </section>
          <section className="public-story-grid">
            <article className="public-section-card public-story-card">
              <p className="panel-kicker">Industries served</p>
              <h2>Built for operations-heavy teams, not generic admin work.</h2>
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Retail and multi-site commerce</strong>
                  <p>Keep stock posture, fulfillment pressure, and urgent actions visible across stores and warehouse lanes.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Distribution and wholesale</strong>
                  <p>Coordinate stock rebalancing, connector health, and approval-driven replenishment across the network.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Logistics and last-mile operations</strong>
                  <p>See backlog, delayed shipments, replay failures, and runtime trust without hopping between systems.</p>
                </div>
              </div>
            </article>
            <article className="public-section-card public-story-card">
              <p className="panel-kicker">Operational loop</p>
              <h2>Receive, interpret, predict, guide, and execute.</h2>
              <div className="signal-list">
                <div className="signal-list-item"><strong>1. Receive activity</strong><p>Orders, stock updates, connector events, approvals, manual actions, and simulation activity arrive continuously.</p></div>
                <div className="signal-list-item"><strong>2. Understand what matters</strong><p>SynapseCore updates the live model, surfaces risk, estimates near-term pressure, and ranks the next action.</p></div>
                <div className="signal-list-item"><strong>3. Move the business safely</strong><p>Teams act from one workspace through recommendations, approvals, replay, escalation, and controlled execution.</p></div>
              </div>
            </article>
          </section>
          <section className="public-cta-banner">
            <div>
              <p className="panel-kicker">See the full system</p>
              <h2>One platform for awareness, action, and trust under pressure.</h2>
              <p>SynapseCore is designed to feel like a premium operational operating system from the first login, not a generic dashboard template.</p>
            </div>
            <div className="history-action-row">
              <button className="primary-button" onClick={() => navigateToPage('product')} type="button">Explore Product System</button>
              <button className="ghost-button" onClick={() => navigateToPage('contact')} type="button">Plan Rollout</button>
            </div>
          </section>
          </>
        ) : null}
        {effectivePageMeta.key === 'product' ? (
          <>
          <section className="public-section-grid">
            <article className="public-section-card">
              <p className="panel-kicker">Live visibility</p>
              <h2>See orders, stock, locations, fulfillment, and incidents in one control surface.</h2>
              <p>SynapseCore sits above connected systems and updates the business picture in real time instead of leaving operators to merge fragmented views manually.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">Prediction and action</p>
              <h2>Move from "what happened" to "what matters next."</h2>
              <p>Operational risk, recommendations, approvals, escalations, replay, and execution all stay inside one premium command-center workflow.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">Trust layer</p>
              <h2>Use runtime health, audit, events, incidents, and build fingerprints to operate with confidence.</h2>
              <p>The platform is designed to feel calm under pressure, with the engineering trust surface visible to both operators and admins.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">Planning and control</p>
              <h2>Run scenarios, route approvals, and execute with traceability.</h2>
              <p>Compare options before touching live flow, escalate overdue decisions, and keep the decision path visible from request to execution.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">Integrations</p>
              <h2>See connected systems as one operational layer.</h2>
              <p>Monitor connector health, inspect import history, recover failed inbound items, and keep support ownership visible in one place.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">Workspace administration</p>
              <h2>Give every company a controlled operating workspace.</h2>
              <p>Manage tenants, roles, warehouse lanes, security policies, and support posture without leaving the product.</p>
            </article>
          </section>
          <section className="public-story-grid">
            <article className="public-section-card public-story-card">
              <p className="panel-kicker">How teams move through the product</p>
              <h2>Awareness first. Control second. Trust throughout.</h2>
              <div className="signal-list">
                <div className="signal-list-item"><strong>Dashboard and alerts</strong><p>Give operators instant awareness of what changed, what is risky, and what requires action now.</p></div>
                <div className="signal-list-item"><strong>Recommendations and approvals</strong><p>Shift from visibility into structured action with ranked guidance and controlled decision lanes.</p></div>
                <div className="signal-list-item"><strong>Integrations, replay, and runtime</strong><p>Keep the trust layer visible so support teams can recover issues quickly without losing operational context.</p></div>
              </div>
            </article>
            <article className="public-section-card public-story-card">
              <p className="panel-kicker">Page system</p>
              <h2>A full operating surface, not a single dashboard.</h2>
              <div className="signal-chip-grid">
                <span className="scenario-type-tag">Dashboard</span>
                <span className="scenario-type-tag">Alerts</span>
                <span className="scenario-type-tag">Recommendations</span>
                <span className="scenario-type-tag">Orders</span>
                <span className="scenario-type-tag">Inventory</span>
                <span className="scenario-type-tag">Locations</span>
                <span className="scenario-type-tag">Fulfillment</span>
                <span className="scenario-type-tag">Scenarios</span>
                <span className="scenario-type-tag">Approvals</span>
                <span className="scenario-type-tag">Integrations</span>
                <span className="scenario-type-tag">Replay</span>
                <span className="scenario-type-tag">Runtime</span>
              </div>
              <p className="muted-text">Each page raises awareness, supports action, or builds trust so a company can truly run operations from one interface.</p>
            </article>
          </section>
          </>
        ) : null}
        {effectivePageMeta.key === 'contact' ? (
          <>
          <section className="public-section-grid">
            <article className="public-section-card">
              <p className="panel-kicker">Request walkthrough</p>
              <h2>Prepare the rollout conversation.</h2>
              <div className="signin-form-grid">
                <label className="field"><span>Company name</span><input placeholder="Acme Operations" /></label>
                <label className="field"><span>Industry</span><input placeholder="Retail, logistics, distribution..." /></label>
                <label className="field"><span>Email</span><input placeholder="ops@company.com" /></label>
                <label className="field"><span>Company size</span><input placeholder="50-500 staff or multiple sites" /></label>
                <label className="field field-wide"><span>Operational challenge</span><textarea placeholder="Describe the main stock, fulfillment, or integration pressure you need to solve." /></label>
              </div>
              <div className="history-action-row">
                <button className="primary-button" type="button">Request Walkthrough</button>
                <button className="ghost-button" onClick={() => navigateToPage('sign-in')} type="button">Go To Sign In</button>
              </div>
              <p className="muted-text">Capture company context, rollout pressure, and the first operational problem to solve.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">Best fit</p>
              <h2>Ideal for operations-heavy teams that need one control surface.</h2>
              <p>Retail, e-commerce, distribution, logistics, and multi-site operating teams get the most value when multiple systems, locations, and decision lanes need to be understood together.</p>
            </article>
            <article className="public-section-card">
              <p className="panel-kicker">Rollout outcomes</p>
              <h2>Focus the first launch on the highest-pressure workflow.</h2>
              <p>Most teams start with live visibility, early risk detection, connector health, and approval control before expanding into wider planning and intelligence layers.</p>
            </article>
          </section>
          <section className="public-story-grid">
            <article className="public-section-card public-story-card">
              <p className="panel-kicker">What to bring</p>
              <h2>Come with the operating pressure, not a perfect requirements sheet.</h2>
              <div className="signal-list">
                <div className="signal-list-item"><strong>Core workflow</strong><p>Which operational loop hurts most today: stockouts, backlog, delays, approvals, or integration failures?</p></div>
                <div className="signal-list-item"><strong>Systems already in use</strong><p>List the ERP, WMS, OMS, spreadsheets, courier feeds, or APIs that currently hold your operating signals.</p></div>
                <div className="signal-list-item"><strong>Decision ownership</strong><p>Identify who needs to approve actions, who operates the floor, and who supports connector or incident recovery.</p></div>
              </div>
            </article>
            <article className="public-section-card public-story-card">
              <p className="panel-kicker">Rollout shape</p>
              <h2>Start narrow, then deepen into the full command center.</h2>
              <div className="signal-list">
                <div className="signal-list-item"><strong>Phase 1</strong><p>Live visibility, alerts, recommendations, and trust surfaces for the highest-pressure operating lane.</p></div>
                <div className="signal-list-item"><strong>Phase 2</strong><p>Approvals, replay, tenant administration, and stronger connector policy around that same workflow.</p></div>
                <div className="signal-list-item"><strong>Phase 3</strong><p>Broader planning, intelligence, and cross-location optimization once the first lane is stable.</p></div>
              </div>
            </article>
          </section>
          </>
        ) : null}
      </main>
    )
  }

  if (isPublicPage) {
    return renderPublicExperience()
  }

  return (
    <div className={`workspace-shell page-group-${effectivePageMeta.group || 'workspace'} page-${currentPage}`}>
      <aside className="workspace-sidebar">
        <button className="brand-lockup brand-button workspace-brand" onClick={() => navigateToPage('dashboard')} type="button">
          <span className="brand-mark">S</span>
          <span><strong>SynapseCore</strong><small>{signedInSession?.tenantName || signedInSession?.tenantCode || 'Operational workspace'}</small></span>
        </button>
        <div className="workspace-switcher">
          <span className="workspace-switcher-label">Workspace</span>
          <strong>{signedInSession?.tenantName || signedInSession?.tenantCode || 'Signed out'}</strong>
          <p>{signedInSession ? `${signedInSession.displayName} | ${signedInSession.actorName}` : 'Use the sign-in page to open a tenant workspace.'}</p>
        </div>
        <nav className="workspace-nav">
          {navGroups.map((group) => (
            <div key={group.label} className="workspace-nav-group">
              <p>{group.label}</p>
              <div className="workspace-nav-links">
                {group.keys.map((pageKey) => {
                  const page = pageLookup[pageKey]
                  return (
                    <button
                      key={page.key}
                      className={`workspace-nav-link ${currentPage === page.key ? 'workspace-nav-link-active' : ''}`}
                      onClick={() => navigateToPage(page.key)}
                      type="button"
                    >
                      <span>{page.label}</span>
                      <strong>{pageBadgeMap[page.key] || 0}</strong>
                    </button>
                  )
                })}
              </div>
            </div>
          ))}
        </nav>
        <div className="workspace-sidebar-footer">
          <div className="workspace-sidebar-status">
            <span className={`live-dot status-${connectionState}`} />
            <span>{connectionState === 'live' ? 'Realtime live' : `Realtime ${formatCodeLabel(connectionState)}`}</span>
          </div>
          <button className="ghost-button" onClick={() => navigateToPage('profile')} type="button">Profile & Session</button>
        </div>
      </aside>
      <div className="workspace-frame">
        <header className="workspace-topbar">
          <div>
            <p className="eyebrow">{effectivePageMeta.group ? effectivePageMeta.group.toUpperCase() : 'WORKSPACE'}</p>
            <h1>{effectivePageMeta.title}</h1>
            <p className="workspace-topbar-copy">{effectivePageMeta.description}</p>
          </div>
          <div className="workspace-topbar-actions">
            <div className="workspace-topbar-search">
              <label className="field workspace-search-field">
                <span>Global search</span>
                <input
                  ref={searchInputRef}
                  type="text"
                  value={workspaceSearch}
                  onChange={(event) => setWorkspaceSearch(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' && firstWorkspaceSearchResult) {
                      event.preventDefault()
                      navigateToPage(firstWorkspaceSearchResult.target)
                      setWorkspaceSearch('')
                    }
                  }}
                  placeholder="Search pages, orders, or alerts"
                />
              </label>
              {hasWorkspaceSearch ? (
                <div className="workspace-search-results">
                  <div className="workspace-search-results-header">
                    <strong>{workspaceSearchMatchCount ? `${workspaceSearchMatchCount} match${workspaceSearchMatchCount === 1 ? '' : 'es'}` : 'No matches yet'}</strong>
                    <div className="workspace-search-shortcuts">
                      <span className="workspace-shortcut-chip">/ focus</span>
                      <span className="workspace-shortcut-chip">Enter open</span>
                      <button className="ghost-button workspace-search-clear" onClick={() => setWorkspaceSearch('')} type="button">Clear</button>
                    </div>
                  </div>
                  {workspaceSearchSections.length ? workspaceSearchSections.map((section) => (
                    <section key={section.key} className="workspace-search-group">
                      <div className="workspace-search-group-title">
                        <span>{section.label}</span>
                        <small>{section.items.length}</small>
                      </div>
                      {section.items.map((item) => (
                        <button
                          key={item.id}
                          className="workspace-search-result"
                          onClick={() => {
                            navigateToPage(item.target)
                            setWorkspaceSearch('')
                          }}
                          type="button"
                        >
                          <strong>{item.title}</strong>
                          <span>{item.meta}</span>
                        </button>
                      ))}
                    </section>
                  )) : <div className="empty-state">No pages, orders, alerts, or incidents match the current search.</div>}
                </div>
              ) : null}
            </div>
            <div className="workspace-command-bar" aria-label="Page focus">
              {pageSectionActions.length
                ? pageSectionActions.map((action) => (
                  <button
                    key={action.targetId}
                    className="hero-jump-link"
                    onClick={() => jumpToPageSection(action.targetId)}
                    type="button"
                  >
                    {action.label}
                  </button>
                ))
                : effectivePageMeta.focus.map((focusItem) => <span key={focusItem} className="hero-jump-link">{focusItem}</span>)}
            </div>
            <div className="workspace-status-strip">
              <span className="workspace-status-pill">{liveClockLabel}</span>
              <span className={`workspace-status-pill status-${connectionState}`}>{connectionState === 'live' ? 'Live system' : formatCodeLabel(connectionState)}</span>
              <button className="workspace-status-pill workspace-status-button" onClick={() => navigateToPage('alerts')} type="button">
                Notifications {globalNotificationCount}
              </button>
            </div>
            <div className="workspace-topbar-controls">
              {topbarQuickActions.map((action) => (
                <button key={action.label} className="ghost-button" onClick={() => navigateToPage(action.target)} type="button">{action.label}</button>
              ))}
              <button className="ghost-button" onClick={async () => { await Promise.all([fetchSnapshot(), fetchSystemRuntime()]) }} disabled={pageState.loading || actionState.loading || systemRuntimeState.loading} type="button">Refresh</button>
              {showOperationalTools ? (
                <button className="primary-button" onClick={() => toggleSimulation(snapshot.simulation.active ? 'stop' : 'start')} disabled={actionState.loading} type="button">
                  {actionState.loading ? 'Working...' : snapshot.simulation.active ? 'Stop Live Stream' : 'Start Live Stream'}
                </button>
              ) : null}
              <button className="ghost-button" onClick={() => navigateToPage('profile')} type="button">{signedInSession?.displayName || 'Profile'}</button>
              <button className="ghost-button" onClick={signOutOperator} disabled={authSessionState.loading} type="button">
                {authSessionState.action === 'signout' ? 'Signing Out...' : 'Sign Out'}
              </button>
            </div>
          </div>
        </header>
        <div className="workspace-body">
          <main className="workspace-content">
            {showDashboardHero ? (
              <section className="command-hero">
                <div className="command-hero-copy">
                  <p className="eyebrow">Premium operational command center</p>
                  <h2>Operate live business pressure with calm, visible control.</h2>
                  <p className="hero-copy">One workspace for orders, stock, fulfillment, connectors, approvals, incidents, and access control. SynapseCore keeps the operating picture live, ranked, and actionable.</p>
                  <div className="hero-highlight-grid">
                    {controlHighlights.map((highlight) => (
                      <article key={highlight.label} className={`hero-highlight tone-${highlight.tone}`}>
                        <span className="hero-highlight-label">{highlight.label}</span>
                        <strong>{highlight.value}</strong>
                        <p>{highlight.note}</p>
                      </article>
                    ))}
                  </div>
                </div>
                <div className="command-hero-aside">
                  <article className="command-hero-panel">
                    <p className="panel-kicker">Workspace pulse</p>
                    <h3>{pendingReviewCount || pendingReplayCount || systemIncidents.length ? 'Attention needed' : 'Operational posture stable'}</h3>
                    <p>{pageStatusMap.dashboard}</p>
                    <div className="command-hero-metrics">
                      <div><span>Pages</span><strong>{appPageCount}</strong></div>
                      <div><span>Connectors</span><strong>{enabledConnectorCount}/{snapshot.integrationConnectors.length || 0}</strong></div>
                      <div><span>Replay</span><strong>{pendingReplayCount}</strong></div>
                      <div><span>Approvals</span><strong>{pendingReviewCount}</strong></div>
                    </div>
                  </article>
                </div>
              </section>
            ) : (
              <section className="page-intro-panel">
                <div>
                  <p className="panel-kicker">Current page</p>
                  <h2>{effectivePageMeta.label}</h2>
                  <p>{pageStatusMap[currentPage]}</p>
                </div>
                <div className="page-intro-tags">
                  {effectivePageMeta.focus.map((focusItem) => <span key={focusItem} className="scenario-type-tag">{focusItem}</span>)}
                </div>
              </section>
            )}
            {pageState.error ? <p className="error-text">Snapshot load issue: {pageState.error}</p> : null}
            {actionState.error ? <p className="error-text">{actionState.error}</p> : null}
            {authSessionState.error ? <p className="error-text">{authSessionState.error}</p> : null}
            {renderDashboardOverview()}
            {renderAlertsCenter()}
            {renderRecommendationsCenter()}
            {renderOrdersCenter()}
            {renderInventoryCenter()}
            {renderFulfillmentCenter()}
            {renderScenarioControlOverview()}
            {renderScenarioHistoryExperience()}
            {renderApprovalsExperience()}
            {renderEscalationsExperience()}
            {renderIntegrationsOverview()}
            {renderReplayOverview()}
            {renderRuntimeOverview()}
            {renderAuditOverview()}
            {renderUsersOverview()}
            {renderSettingsExperience()}
            {renderPlatformAdminExperience()}
            {renderSystemConfigExperience()}
            {renderReleaseExperience()}
        <section className="summary-grid" hidden={!isAuthenticated || !showSummaryMetrics}>{metrics.map((metric) => <SummaryCard key={metric.label} {...metric} />)}</section>
        <section className="content-grid planner-grid" id="planner" hidden={!isAuthenticated || !showScenarioPlanner}>
          <article className="panel panel-wide">
            <div className="panel-header">
              <div><p className="panel-kicker">Scenario planning</p><h2>Compare proposed order plans</h2></div>
              <span className="panel-badge planner-badge">{comparisonState.result ? 'Comparison ready' : scenarioState.result ? 'Preview ready' : 'Planner'}</span>
            </div>
            <div className="planner-action-bar">
              <div>
                <p className="muted-text">Preview a single plan or compare two proposed order mixes before they touch live inventory.</p>
                {scenarioRevisionSource ? (
                  <div className="revision-banner">
                    <span>Revision mode: saving will create revision {scenarioRevisionSource.revisionNumber} of {scenarioRevisionSource.title}.</span>
                    <button className="ghost-button" onClick={() => setScenarioRevisionSource(null)} type="button">Exit Revision Mode</button>
                  </div>
                ) : null}
                <div className="history-quick-actions">
                  <button
                    className="ghost-button"
                    onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, requestedBy: scenarioRequestedBy.trim() }))}
                    disabled={!scenarioRequestedBy.trim()}
                    type="button"
                  >
                    My Requests
                  </button>
                  <button
                    className="ghost-button"
                    onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', reviewOwner: scenarioReviewOwner.trim() }))}
                    disabled={!scenarioReviewOwner.trim()}
                    type="button"
                  >
                    My Review Queue
                  </button>
                  <button
                    className="ghost-button"
                    onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', reviewOwner: scenarioReviewOwner.trim(), minimumReviewPriority: 'HIGH' }))}
                    disabled={!scenarioReviewOwner.trim()}
                    type="button"
                  >
                    High-Risk Queue
                  </button>
                  <button
                    className="ghost-button"
                    onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', reviewOwner: scenarioReviewOwner.trim(), approvalPolicy: 'ESCALATED' }))}
                    disabled={!scenarioReviewOwner.trim()}
                    type="button"
                  >
                    Escalated Queue
                  </button>
                  <button
                    className="ghost-button"
                    onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', approvalPolicy: 'ESCALATED', approvalStage: 'PENDING_FINAL_APPROVAL' }))}
                    type="button"
                  >
                    Final Approval Queue
                  </button>
                  <button
                    className="ghost-button"
                    onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', approvalPolicy: 'ESCALATED', approvalStage: 'PENDING_FINAL_APPROVAL', finalApprovalOwner: signedInSession?.actorName || '' }))}
                    disabled={!signedInSession}
                    type="button"
                  >
                    My Final Approvals
                  </button>
                  <button
                    className="ghost-button"
                    onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', overdueOnly: true }))}
                    type="button"
                  >
                    Overdue Queue
                  </button>
                  <button
                    className="ghost-button"
                    onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', slaEscalatedOnly: true }))}
                    type="button"
                  >
                    SLA Escalated Queue
                  </button>
                </div>
              </div>
              <div className="planner-actions">
                <label className="field planner-name-field">
                  <span>Plan Name</span>
                  <input
                    type="text"
                    maxLength="120"
                    placeholder="North restock option"
                    value={scenarioPlanName}
                    onChange={(event) => setScenarioPlanName(event.target.value)}
                  />
                </label>
                <label className="field planner-name-field">
                  <span>Requested By</span>
                  <select value={scenarioRequestedBy} onChange={(event) => setScenarioRequestedBy(event.target.value)} disabled={!requesterOperators.length || Boolean(signedInSession)}>
                    {requesterOperators.length
                      ? requesterOperators.map((operator) => <option key={operator.actorName} value={operator.actorName}>{operator.displayName}</option>)
                      : <option value="">Loading operators...</option>}
                  </select>
                </label>
                <label className="field planner-name-field">
                  <span>Acting As</span>
                  <select value={scenarioActorRole} onChange={(event) => setScenarioActorRole(event.target.value)}>
                    {scenarioActorRoles.map((role) => <option key={role} value={role}>{formatCodeLabel(role)}</option>)}
                  </select>
                </label>
                <label className="field planner-name-field">
                  <span>Signed In As</span>
                  <input type="text" value={signedInSession ? signedInSession.displayName : 'Sign in to review or approve'} readOnly />
                </label>
                <label className="field planner-name-field">
                  <span>Review Owner</span>
                  <select value={scenarioReviewOwner} onChange={(event) => setScenarioReviewOwner(event.target.value)} disabled={!reviewOwnerOperators.length}>
                    {reviewOwnerOperators.length
                      ? reviewOwnerOperators.map((operator) => <option key={operator.actorName} value={operator.actorName}>{operator.displayName}</option>)
                      : <option value="">No review owners configured</option>}
                  </select>
                </label>
                <label className="field planner-name-field">
                  <span>Review Note</span>
                  <input
                    type="text"
                    maxLength="240"
                    placeholder="Required when rejecting a saved plan"
                    value={scenarioReviewNote}
                    onChange={(event) => setScenarioReviewNote(event.target.value)}
                  />
                </label>
                <p className="muted-text planner-note">Review and approval actions use the signed-in operator session. Warehouse-scoped operators only appear for the selected warehouse, and review actions are blocked outside their assigned lanes.</p>
                {operatorDirectoryState.error ? <p className="error-text">{operatorDirectoryState.error}</p> : null}
                <button className="ghost-button" onClick={saveScenarioPlan} disabled={scenarioSaveState.loading || !primaryContext.inputValid || !scenarioPlanName.trim() || !scenarioRequestedBy || !scenarioReviewOwner || !signedInSession || !hasWarehouseScope(signedInWarehouseScopes, scenarioForm.warehouseCode)}>{scenarioSaveState.loading ? 'Saving...' : 'Save Scenario A'}</button>
                <button className="secondary-button" onClick={analyzeScenario} disabled={scenarioState.loading || !primaryContext.inputValid}>{scenarioState.loading ? 'Analyzing...' : 'Preview Scenario A'}</button>
                <button className="compare-button" onClick={compareScenarios} disabled={comparisonState.loading || !primaryContext.inputValid || !alternativeContext.inputValid}>{comparisonState.loading ? 'Comparing...' : 'Compare A vs B'}</button>
              </div>
            </div>
            <div className="planner-compare-grid">
              {renderScenarioEditor('Scenario A', scenarioForm, setScenarioForm, primaryContext)}
              {renderScenarioEditor('Scenario B', comparisonForm, setComparisonForm, alternativeContext)}
            </div>
            {scenarioState.error ? <p className="error-text">{scenarioState.error}</p> : null}
            {comparisonState.error ? <p className="error-text">{comparisonState.error}</p> : null}
            {scenarioExecutionState.error ? <p className="error-text">{scenarioExecutionState.error}</p> : null}
            {scenarioExecutionState.success ? <p className="success-text">{scenarioExecutionState.success}</p> : null}
            {scenarioLoadState.error ? <p className="error-text">{scenarioLoadState.error}</p> : null}
            {scenarioLoadState.success ? <p className="success-text">{scenarioLoadState.success}</p> : null}
            {scenarioSaveState.error ? <p className="error-text">{scenarioSaveState.error}</p> : null}
            {scenarioSaveState.success ? <p className="success-text">{scenarioSaveState.success}</p> : null}
            {scenarioApprovalState.error ? <p className="error-text">{scenarioApprovalState.error}</p> : null}
            {scenarioApprovalState.success ? <p className="success-text">{scenarioApprovalState.success}</p> : null}
            {scenarioRejectionState.error ? <p className="error-text">{scenarioRejectionState.error}</p> : null}
            {scenarioRejectionState.success ? <p className="success-text">{scenarioRejectionState.success}</p> : null}
            {comparisonState.result ? (
              <div className="comparison-shell">
                <div className="comparison-banner">
                  <strong>Recommended option: {comparisonState.result.summary.recommendedOption}</strong>
                  <p>{comparisonState.result.summary.rationale}</p>
                </div>
                <div className="comparison-grid">
                  <article className="comparison-card">
                    <div className="panel-header">
                      <div><p className="panel-kicker">Scenario A</p><h2>{comparisonState.result.primaryLabel}</h2></div>
                      <span className="panel-badge compare-score-badge">Score {comparisonState.result.summary.primaryRiskScore}</span>
                    </div>
                    <div className="scenario-grid comparison-stats">
                      <article className="scenario-stat"><span className="summary-label">Low Stock</span><strong>{comparisonPrimarySummary.lowStockItems}</strong></article>
                      <article className="scenario-stat"><span className="summary-label">Critical</span><strong>{comparisonPrimarySummary.criticalItems}</strong></article>
                      <article className="scenario-stat"><span className="summary-label">Alerts</span><strong>{comparisonPrimarySummary.alertCount}</strong></article>
                      <article className="scenario-stat"><span className="summary-label">Actions</span><strong>{comparisonPrimarySummary.recommendationCount}</strong></article>
                    </div>
                  </article>
                  <article className="comparison-card">
                    <div className="panel-header">
                      <div><p className="panel-kicker">Scenario B</p><h2>{comparisonState.result.alternativeLabel}</h2></div>
                      <span className="panel-badge compare-score-badge">Score {comparisonState.result.summary.alternativeRiskScore}</span>
                    </div>
                    <div className="scenario-grid comparison-stats">
                      <article className="scenario-stat"><span className="summary-label">Low Stock</span><strong>{comparisonAlternativeSummary.lowStockItems}</strong></article>
                      <article className="scenario-stat"><span className="summary-label">Critical</span><strong>{comparisonAlternativeSummary.criticalItems}</strong></article>
                      <article className="scenario-stat"><span className="summary-label">Alerts</span><strong>{comparisonAlternativeSummary.alertCount}</strong></article>
                      <article className="scenario-stat"><span className="summary-label">Actions</span><strong>{comparisonAlternativeSummary.recommendationCount}</strong></article>
                    </div>
                  </article>
                </div>
              </div>
            ) : null}
            {scenarioState.result ? (
              <>
                <div className="scenario-grid">
                  <article className="scenario-stat"><span className="summary-label">Projected Order Value</span><strong>{currency.format(scenarioState.result.projectedOrderValue)}</strong></article>
                  <article className="scenario-stat"><span className="summary-label">Units Impacted</span><strong>{scenarioState.result.totalUnits}</strong></article>
                  <article className="scenario-stat"><span className="summary-label">Projected Alerts</span><strong>{scenarioState.result.projectedAlerts.length}</strong></article>
                  <article className="scenario-stat"><span className="summary-label">Projected Actions</span><strong>{scenarioState.result.projectedRecommendations.length}</strong></article>
                </div>
                <div className="content-grid planner-results">
                  <article className="panel planner-subpanel">
                    <div className="panel-header"><div><p className="panel-kicker">Projected inventory</p><h2>Scenario A posture</h2></div></div>
                    <div className="stack-list">
                      {scenarioState.result.projectedInventory.map((item) => (
                        <div key={`${item.id}-${item.productSku}`} className="stack-card">
                          <div className="stack-title-row"><strong>{item.productName}</strong><span className={`risk-chip risk-${item.riskLevel}`}>{item.riskLevel}</span></div>
                          <p>{item.warehouseName} | {item.quantityAvailable} units after impact</p>
                          <p className="muted-text">Threshold {item.reorderThreshold} | Stockout {formatRelativeHours(item.hoursToStockout)}</p>
                        </div>
                      ))}
                    </div>
                  </article>
                  <article className="panel planner-subpanel">
                    <div className="panel-header"><div><p className="panel-kicker">Projected response</p><h2>Scenario A alerts and actions</h2></div></div>
                    <div className="stack-list">
                      {scenarioState.result.projectedAlerts.map((alert) => (
                        <div key={alert.title} className="stack-card">
                          <div className="stack-title-row"><strong>{alert.title}</strong><span className={`severity-tag severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span></div>
                          <p>{alert.description}</p>
                          <p className="muted-text">{alert.impactSummary}</p>
                        </div>
                      ))}
                      {scenarioState.result.projectedRecommendations.map((recommendation) => (
                        <div key={recommendation.title} className="stack-card">
                          <div className="stack-title-row"><strong>{recommendation.title}</strong><span className={`priority-tag priority-${recommendation.priority.toLowerCase()}`}>{recommendation.priority}</span></div>
                          <p>{recommendation.description}</p>
                        </div>
                      ))}
                      {!scenarioState.result.projectedAlerts.length && !scenarioState.result.projectedRecommendations.length ? <div className="empty-state">This scenario stays within the current operating buffer.</div> : null}
                    </div>
                  </article>
                </div>
              </>
            ) : <div className="empty-state planner-empty">Build one or two proposed order plans to preview risk and compare the safer option before committing activity.</div>}
          </article>
        </section>
        <section className="content-grid" id="risk" hidden={!isAuthenticated || !(showRiskAlertsPanel || showRiskRecommendationsPanel)}>
          <article className="panel" hidden={!showRiskAlertsPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Immediate risk</p><h2>Active alerts</h2></div>
              <span className="panel-badge alert-badge">{snapshot.alerts.activeAlerts.length}</span>
            </div>
            <div className="stack-list">
              {snapshot.alerts.activeAlerts.length ? snapshot.alerts.activeAlerts.map((alert) => (
                <div key={alert.id} className="stack-card">
                  <div className="stack-title-row"><strong>{alert.title}</strong><span className={`severity-tag severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span></div>
                  <p>{alert.description}</p>
                  <p className="muted-text">{alert.impactSummary}</p>
                  <p className="action-line">Action: {alert.recommendedAction}</p>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading operational alerts...' : 'No active operational alerts. SynapseCore is monitoring live state.'}</div>}
            </div>
          </article>
          <article className="panel" hidden={!showRiskRecommendationsPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Decision layer</p><h2>Recommendations</h2></div>
              <span className="panel-badge recommendation-badge">{snapshot.recommendations.length}</span>
            </div>
            <div className="stack-list">
              {snapshot.recommendations.length ? snapshot.recommendations.map((recommendation) => (
                <div key={recommendation.id} className="stack-card">
                  <div className="stack-title-row"><strong>{recommendation.title}</strong><span className={`priority-tag priority-${recommendation.priority.toLowerCase()}`}>{recommendation.priority}</span></div>
                  <p>{recommendation.description}</p>
                  <p className="muted-text">{formatTimestamp(recommendation.createdAt)}</p>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading recommendation stream...' : 'Recommendations will appear here when SynapseCore detects meaningful risk.'}</div>}
            </div>
          </article>
        </section>
        <section className="content-grid lower-grid" id="operations" hidden={!isAuthenticated || !(showInventoryPanel || showFulfillmentPanel || showOrdersPanel)}>
          <article className="panel panel-wide" hidden={!showInventoryPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Inventory health</p><h2>Live stock posture</h2></div>
              <span className="panel-badge inventory-badge">{snapshot.inventory.length}</span>
            </div>
            <div className="table-shell">
              <table>
                <thead>
                  <tr><th>Product</th><th>Warehouse</th><th>Available</th><th>Threshold</th><th>Risk</th><th>Rate</th><th>Stockout</th></tr>
                </thead>
                <tbody>
                  {snapshot.inventory.length ? snapshot.inventory.map((item) => (
                    <tr key={item.id}>
                      <td><strong>{item.productName}</strong><span>{item.productSku}</span></td>
                      <td><strong>{item.warehouseName}</strong><span>{item.warehouseCode}</span></td>
                      <td>{item.quantityAvailable}</td>
                      <td>{item.reorderThreshold}</td>
                      <td><span className={`risk-chip risk-${item.riskLevel}`}>{item.riskLevel}</span></td>
                      <td>{item.unitsPerHour?.toFixed(1) || '0.0'} units/hr</td>
                      <td>{formatRelativeHours(item.hoursToStockout)}</td>
                    </tr>
                  )) : <tr className="table-empty-row"><td colSpan="7">{pageState.loading ? 'Loading live inventory posture...' : 'No inventory records are available yet.'}</td></tr>}
                </tbody>
              </table>
            </div>
          </article>
          <article className="panel" id="fulfillment" hidden={!showFulfillmentPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Fulfillment lane</p><h2>Backlog and delivery pressure</h2></div>
              <span className="panel-badge fulfillment-badge">{fulfillmentOverview.activeFulfillments.length}</span>
            </div>
            <div className="summary-grid">
              <SummaryCard label="Backlog" value={fulfillmentOverview.backlogCount} accent="amber" />
              <SummaryCard label="Overdue Dispatch" value={fulfillmentOverview.overdueDispatchCount} accent="orange" />
              <SummaryCard label="Delayed Shipments" value={fulfillmentOverview.delayedShipmentCount} accent="rose" />
              <SummaryCard label="At Risk" value={fulfillmentOverview.atRiskCount} accent="teal" />
            </div>
            <div className="stack-list">
              {fulfillmentOverview.activeFulfillments.length ? fulfillmentOverview.activeFulfillments.map((task) => (
                <div key={task.id} className="stack-card">
                  <div className="stack-title-row">
                    <strong>{task.externalOrderId}</strong>
                    <div className="stack-tag-row">
                      <span className={`status-tag ${getFulfillmentStatusClassName(task.fulfillmentStatus)}`}>{formatCodeLabel(task.fulfillmentStatus)}</span>
                      <span className={`risk-chip risk-${task.riskLevel}`}>{task.riskLevel}</span>
                    </div>
                  </div>
                  <p>{task.warehouseName} | {task.itemCount} units{task.carrier ? ` | ${task.carrier}` : ''}</p>
                  <p className="muted-text">
                    Dispatch due {formatRelativeHours(task.hoursUntilDispatchDue)}
                    {task.expectedDeliveryAt ? ` | Delivery ${formatRelativeHours(task.hoursUntilDeliveryDue)}` : ''}
                    {task.trackingReference ? ` | Tracking ${task.trackingReference}` : ''}
                  </p>
                  <p>{task.impactSummary}</p>
                  <p className="muted-text">
                    Backlog growth {task.backlogGrowthPerHour.toFixed(1)} / hr
                    {task.estimatedBacklogClearHours != null ? ` | Clear in ${formatRelativeHours(task.estimatedBacklogClearHours)}` : ''}
                  </p>
                  <p className="muted-text">Updated {formatTimestamp(task.updatedAt)}</p>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading fulfillment pressure...' : 'Orders will open a fulfillment lane here once dispatch and delivery activity starts moving.'}</div>}
            </div>
          </article>
          <article className="panel" id="integrations" hidden={!showOrdersPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Recent activity</p><h2>Order stream</h2></div>
              <span className="panel-badge order-badge">{snapshot.recentOrders.length}</span>
            </div>
            <div className="stack-list">
              {snapshot.recentOrders.length ? snapshot.recentOrders.map((order) => (
                <div key={order.id} className="stack-card">
                  <div className="stack-title-row"><strong>{order.externalOrderId}</strong><span className="order-total">{currency.format(order.totalAmount)}</span></div>
                  <p>{order.warehouseName} | {order.itemCount} units</p>
                  <p className="muted-text">{formatTimestamp(order.createdAt)}</p>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading recent order activity...' : <>No orders yet. Use the simulation or POST to <code>/api/orders</code>.</>}</div>}
            </div>
          </article>
        </section>
        <section className="content-grid expansion-grid">
          <article className="panel" id="system" hidden={!isAuthenticated || !showRuntimePanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Runtime trust</p><h2>System status</h2></div>
              <span className={`panel-badge ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'audit-badge'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
            </div>
            <div className="scenario-grid runtime-grid">
              <article className="scenario-stat"><span className="summary-label">Readiness</span><strong>{runtime ? formatCodeLabel(runtime.readinessState) : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Liveness</span><strong>{runtime ? formatCodeLabel(runtime.livenessState) : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Profile</span><strong>{runtime ? runtime.activeProfiles.join(', ') : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Cookie Security</span><strong>{runtime ? runtime.secureSessionCookies ? 'Secure' : 'Local Http' : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Replay Queue</span><strong>{runtime ? runtime.telemetry.replayQueueDepth : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Disabled Connectors</span><strong>{runtime ? runtime.telemetry.disabledConnectorCount : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Import Issues</span><strong>{runtime ? runtime.telemetry.recentImportIssues : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Audit Failures</span><strong>{runtime ? runtime.telemetry.recentAuditFailures : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Active Alerts</span><strong>{runtime ? runtime.telemetry.activeAlertCount : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Delay Pressure</span><strong>{runtime ? runtime.telemetry.delayedFulfillmentCount : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Dispatch Queue</span><strong>{runtime ? runtime.telemetry.dispatchQueueDepth : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Dispatch Failures</span><strong>{runtime ? runtime.telemetry.failedDispatchCount : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Orders Ingested</span><strong>{runtime ? formatMetricValue(runtime.metrics.ordersIngested) : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Fulfillment Updates</span><strong>{runtime ? formatMetricValue(runtime.metrics.fulfillmentUpdates) : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Dispatch Processed</span><strong>{runtime ? formatMetricValue(runtime.metrics.dispatchProcessed) : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Replay Attempts</span><strong>{runtime ? formatMetricValue(runtime.metrics.replayAttempts) : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Event Window</span><strong>{runtime ? `${runtime.diagnostics.windowHours} hrs` : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Business Events</span><strong>{runtime ? runtime.diagnostics.businessEventsInWindow : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Integration Events</span><strong>{runtime ? runtime.diagnostics.integrationEventsInWindow : '...'}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Scenario Events</span><strong>{runtime ? runtime.diagnostics.scenarioEventsInWindow : '...'}</strong></article>
            </div>
            <div className="stack-list">
              {systemRuntimeState.error ? <p className="error-text">{systemRuntimeState.error}</p> : null}
              {runtime ? (
                <div className="stack-card">
                  <div className="stack-title-row">
                    <strong>{formatCodeLabel(runtime.applicationName.replace(/-/g, '_'))}</strong>
                    <div className="stack-tag-row">
                      <span className={`status-tag ${getRuntimeStatusClassName(runtime.overallStatus)}`}>{runtime.overallStatus}</span>
                      <span className={`status-tag ${runtime.headerFallbackEnabled ? 'status-partial' : 'status-success'}`}>{runtime.headerFallbackEnabled ? 'Header Fallback On' : 'Session Only'}</span>
                    </div>
                  </div>
                  <p>Observed {formatTimestamp(runtime.observedAt)} with simulation interval {runtime.simulationIntervalMs} ms.</p>
                  <p className="muted-text">Backend build {formatBuildValue(runtime.build.version)} | commit {formatBuildValue(runtime.build.commit)} | built {formatBuildValue(runtime.build.builtAt)}</p>
                  <p className="muted-text">Frontend build {formatBuildValue(frontendBuildVersion)} | commit {formatBuildValue(frontendBuildCommit)} | built {formatBuildValue(frontendBuildTime)}</p>
                  <p className="muted-text">Dispatch queue drains every {runtime.backbone.dispatchIntervalMs} ms in batches of {runtime.backbone.batchSize}. Pending {runtime.backbone.pendingDispatchCount} | Failed {runtime.backbone.failedDispatchCount}.</p>
                  <p className="muted-text">Telemetry tracks replay backlog, dispatch queue health, and recent business-event pressure inside the operational engine.</p>
                  <p className="muted-text">Latest dispatch completion: {formatTimestamp(runtime.backbone.latestProcessedAt)} | Oldest queued work: {runtime.backbone.oldestPendingAgeSeconds == null ? 'clear' : `${runtime.backbone.oldestPendingAgeSeconds}s`}</p>
                  <p className="muted-text">Tenant metrics: {formatMetricValue(runtime.metrics.dispatchQueued)} queued | {formatMetricValue(runtime.metrics.dispatchProcessed)} processed | {formatMetricValue(runtime.metrics.dispatchFailures)} failed.</p>
                  <p className="muted-text">Latest business event: {formatTimestamp(runtime.diagnostics.latestBusinessEventAt)} | Latest failure audit: {formatTimestamp(runtime.diagnostics.latestFailureAt)}</p>
                  <p className="muted-text">Allowed origins: {runtime.allowedOrigins.join(', ')}</p>
                  <p className="muted-text">API {apiUrl} | WS {wsUrl}</p>
                </div>
              ) : <div className="empty-state">{systemRuntimeState.loading ? 'Loading runtime trust signals...' : 'Runtime health data is not available yet.'}</div>}
            </div>
          </article>
          <article className="panel" hidden={!isAuthenticated || !showIncidentPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Operational trust</p><h2>System incident inbox</h2></div>
              <span className="panel-badge notification-badge">{systemIncidents.length}</span>
            </div>
            <div className="stack-list">
              {systemIncidents.length ? systemIncidents.map((incident) => (
                <div key={incident.incidentKey} className="stack-card">
                  <div className="stack-title-row">
                    <strong>{incident.title}</strong>
                    <div className="stack-tag-row">
                      <span className={`status-tag ${getIncidentStatusClassName(incident.severity)}`}>{formatCodeLabel(incident.type)}</span>
                      <span className={`status-tag ${getIncidentStatusClassName(incident.severity)}`}>{formatCodeLabel(incident.severity)}</span>
                    </div>
                  </div>
                  <p>{incident.detail}</p>
                  <p className="muted-text">{incident.context}</p>
                  <p className="muted-text">{formatTimestamp(incident.createdAt)}</p>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading system incidents...' : 'No current operational trust incidents. Replay, connector, approval, and failure signals will surface here when attention is needed.'}</div>}
            </div>
          </article>
          <article className="panel" id="history" hidden={!isAuthenticated || !showBusinessEventsPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Operational pulse</p><h2>Business event timeline</h2></div>
              <span className="panel-badge event-badge">{snapshot.recentEvents.length}</span>
            </div>
            <div className="stack-list">
              {snapshot.recentEvents.length ? snapshot.recentEvents.map((event) => (
                <div key={event.id} className="stack-card">
                  <div className="stack-title-row"><strong>{formatCodeLabel(event.eventType)}</strong><span className="event-type-tag">{event.source}</span></div>
                  <p>{event.payloadSummary}</p>
                  <p className="muted-text">{formatTimestamp(event.createdAt)}</p>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading business event timeline...' : 'Business events will appear here as SynapseCore processes operations.'}</div>}
            </div>
          </article>
          <article className="panel" hidden={!isAuthenticated || !showAuditPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Traceability</p><h2>Audit trail</h2></div>
              <span className="panel-badge audit-badge">{snapshot.auditLogs.length}</span>
            </div>
            <div className="stack-list">
              {snapshot.auditLogs.length ? snapshot.auditLogs.map((log) => (
                <div key={log.id} className="stack-card">
                  <div className="stack-title-row"><strong>{formatCodeLabel(log.action)}</strong><span className={`status-tag status-${log.status.toLowerCase()}`}>{log.status}</span></div>
                  <p>{log.details}</p>
                  <p className="muted-text">{log.targetType} | {log.targetRef} | {formatTimestamp(log.createdAt)}</p>
                  <p className="trace-line">Request: {log.requestId}</p>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading audit trail...' : 'Audit entries will appear here for processed and rejected operational requests.'}</div>}
            </div>
          </article>
          <article className="panel" hidden={!isAuthenticated || !showIntegrationConnectorsPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Integration control</p><h2>Connector status</h2></div>
              <span className="panel-badge integration-badge">{snapshot.integrationConnectors.length}</span>
            </div>
            <div className="integration-actor-bar">
              <label className="field">
                <span>Signed In As</span>
                <input value={signedInSession ? signedInSession.displayName : 'Sign in to manage integrations'} readOnly />
              </label>
              <label className="field">
                <span>Role Check</span>
                <select value={integrationActorRole} onChange={(event) => setIntegrationActorRole(event.target.value)}>
                  {integrationActorRoles.map((role) => <option key={role} value={role}>{formatCodeLabel(role)}</option>)}
                </select>
              </label>
            </div>
            <p className="muted-text integration-note">Connector changes require {formatCodeLabel('INTEGRATION_ADMIN')}. Replay accepts {formatCodeLabel('INTEGRATION_OPERATOR')} or {formatCodeLabel('INTEGRATION_ADMIN')}. Protected integration actions now use the signed-in operator session.</p>
            {operatorDirectoryState.error ? <p className="error-text">{operatorDirectoryState.error}</p> : null}
            <div className="stack-list">
              {integrationConnectorState.error ? <p className="error-text">{integrationConnectorState.error}</p> : null}
              {integrationConnectorState.success ? <p className="muted-text">{integrationConnectorState.success}</p> : null}
              {snapshot.integrationConnectors.length ? snapshot.integrationConnectors.map((connector) => {
                const connectorKey = `${connector.sourceSystem}:${connector.type}`
                return (
                  <div key={connectorKey} className="stack-card">
                    <div className="stack-title-row">
                      <strong>{connector.displayName}</strong>
                      <div className="stack-tag-row">
                        <span className="scenario-type-tag">{formatCodeLabel(connector.type)}</span>
                        <span className="scenario-type-tag">{formatCodeLabel(connector.syncMode)}</span>
                        <span className="scenario-type-tag">{formatCodeLabel(connector.validationPolicy)}</span>
                        <span className={`status-tag ${connector.enabled ? 'status-success' : 'status-failure'}`}>
                          {connector.enabled ? 'Enabled' : 'Disabled'}
                        </span>
                      </div>
                    </div>
                    <p>{connector.sourceSystem}{connector.defaultWarehouseCode ? ` | Default ${connector.defaultWarehouseCode}` : ''}</p>
                    <p className="muted-text">
                      {formatCodeLabel(connector.transformationPolicy)} transform
                      {connector.allowDefaultWarehouseFallback ? ' | Warehouse fallback on' : ' | Warehouse fallback off'}
                      {connector.syncIntervalMinutes ? ` | Every ${connector.syncIntervalMinutes} min` : ''}
                    </p>
                    {connector.notes ? <p className="muted-text">{connector.notes}</p> : null}
                    <p className="muted-text">Updated {formatTimestamp(connector.updatedAt)}</p>
                    <div className="history-action-row">
                      <button
                        className={connector.enabled ? 'ghost-button' : 'secondary-button'}
                        onClick={() => toggleConnector(connector)}
                        disabled={integrationConnectorState.loadingKey === connectorKey || !signedInSession || !signedInRoles.includes('INTEGRATION_ADMIN') || integrationActorRole !== 'INTEGRATION_ADMIN' || (connector.defaultWarehouseCode && !hasWarehouseScope(signedInWarehouseScopes, connector.defaultWarehouseCode))}
                        type="button"
                      >
                        {integrationConnectorState.loadingKey === connectorKey ? 'Saving...' : connector.enabled ? 'Disable Connector' : 'Enable Connector'}
                      </button>
                    </div>
                  </div>
                )
              }) : <div className="empty-state">{pageState.loading ? 'Loading connector status...' : 'Connector management appears here once integration routes are configured.'}</div>}
            </div>
          </article>
          <article className="panel" hidden={!isAuthenticated || !showIntegrationImportsPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Integration telemetry</p><h2>Recent import runs</h2></div>
              <span className="panel-badge integration-badge">{snapshot.integrationImportRuns.length}</span>
            </div>
            <div className="stack-list">
              {snapshot.integrationImportRuns.length ? snapshot.integrationImportRuns.map((run) => (
                <div key={run.id} className="stack-card">
                  <div className="stack-title-row">
                    <strong>{run.fileName || `${formatCodeLabel(run.connectorType)} ingress`}</strong>
                    <div className="stack-tag-row">
                      <span className="scenario-type-tag">{formatCodeLabel(run.connectorType)}</span>
                      <span className={`status-tag ${getImportStatusClassName(run.status)}`}>{formatCodeLabel(run.status)}</span>
                    </div>
                  </div>
                  <p>{run.sourceSystem} | {run.recordsReceived} rows received</p>
                  <p className="muted-text">{run.ordersImported} imported | {run.ordersFailed} failed</p>
                  <p className="muted-text">{run.summary}</p>
                  <p className="muted-text">{formatTimestamp(run.createdAt)}</p>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading integration telemetry...' : 'Webhook and CSV import runs will appear here as external systems feed SynapseCore.'}</div>}
            </div>
          </article>
          <article className="panel" hidden={!isAuthenticated || !showReplayQueuePanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Recovery queue</p><h2>Failed inbound orders</h2></div>
              <span className="panel-badge integration-badge">{snapshot.integrationReplayQueue.length}</span>
            </div>
            <div className="stack-list">
              {integrationReplayState.error ? <p className="error-text">{integrationReplayState.error}</p> : null}
              {integrationReplayState.success ? <p className="muted-text">{integrationReplayState.success}</p> : null}
              {snapshot.integrationReplayQueue.length ? snapshot.integrationReplayQueue.map((record) => (
                <div key={record.id} className="stack-card">
                  <div className="stack-title-row">
                    <strong>{record.externalOrderId}</strong>
                    <div className="stack-tag-row">
                      <span className="scenario-type-tag">{formatCodeLabel(record.connectorType)}</span>
                      <span className={`status-tag ${getReplayStatusClassName(record.status)}`}>{formatCodeLabel(record.status)}</span>
                    </div>
                  </div>
                  <p>{record.sourceSystem} | {record.warehouseCode}</p>
                  <p>{record.failureMessage}</p>
                  <p className="muted-text">
                    Attempts {record.replayAttemptCount}
                    {record.lastReplayMessage ? ` | Last replay: ${record.lastReplayMessage}` : ''}
                  </p>
                  <p className="muted-text">
                    {record.lastAttemptedAt ? `Last attempted ${formatTimestamp(record.lastAttemptedAt)} | ` : ''}
                    Queued {formatTimestamp(record.createdAt)}
                  </p>
                  <div className="history-action-row">
                    <button
                      className="secondary-button"
                      onClick={() => replayFailedIntegration(record.id)}
                      disabled={integrationReplayState.loadingId === record.id || !signedInSession || !signedInRoles.some((role) => role === 'INTEGRATION_OPERATOR' || role === 'INTEGRATION_ADMIN') || !hasWarehouseScope(signedInWarehouseScopes, record.warehouseCode)}
                      type="button"
                    >
                      {integrationReplayState.loadingId === record.id ? 'Replaying...' : 'Replay Into Live Flow'}
                    </button>
                  </div>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading recovery queue...' : 'No failed inbound orders are waiting for replay. External ingestion is currently clear.'}</div>}
            </div>
          </article>
          <article className="panel panel-wide" id="access-admin" hidden={!isAuthenticated || !showAccessAdminPanel}>
            <div className="panel-header">
              <div><p className="panel-kicker">Tenant access admin</p><h2>Operators and user accounts</h2></div>
              <span className="panel-badge integration-badge">{accessAdminOperators.length + accessAdminUsers.length}</span>
            </div>
            <p className="muted-text integration-note">
              {signedInRoles.includes('TENANT_ADMIN')
                ? 'Create tenant-scoped operators, assign warehouse lanes, and issue sign-in accounts without leaving the control center.'
                : 'Sign in as a tenant admin to manage operators, warehouse scopes, and tenant user accounts.'}
            </p>
            <article className="comparison-card">
              <div className="panel-header">
                <div><p className="panel-kicker">Workspace settings</p><h2>Tenant metadata and support controls</h2></div>
                <span className="panel-badge scenario-badge">{workspaceAdmin?.supportSummary?.activeIncidentCount ?? 0}</span>
              </div>
              <div className="session-control-row">
                <label className="field session-field">
                  <span>Tenant Name</span>
                  <input value={workspaceSettingsForm.tenantName} onChange={(event) => setWorkspaceSettingsForm((current) => ({ ...current, tenantName: event.target.value }))} placeholder="Tenant workspace name" disabled={accessAdminState.loading || !canManageTenantAccess} />
                </label>
                <label className="field session-field">
                  <span>Description</span>
                  <input value={workspaceSettingsForm.description} onChange={(event) => setWorkspaceSettingsForm((current) => ({ ...current, description: event.target.value }))} placeholder="Operational workspace summary" disabled={accessAdminState.loading || !canManageTenantAccess} />
                </label>
              </div>
              <div className="demo-action-row">
                <button className="secondary-button" onClick={saveWorkspaceSettings} disabled={accessAdminState.loading || !canManageTenantAccess || !workspaceSettingsForm.tenantName.trim()} type="button">
                  {accessAdminState.loading ? 'Working...' : 'Save Workspace'}
                </button>
              </div>
              {workspaceAdmin?.securitySettings ? (
                <div className="stack-card">
                  <div className="stack-title-row">
                    <strong>Tenant security policy</strong>
                    <div className="stack-tag-row">
                      <span className="status-tag status-success">Policy v{workspaceAdmin.securitySettings.securityPolicyVersion}</span>
                    </div>
                  </div>
                  <p className="muted-text">Control how quickly passwords rotate and how long tenant sessions stay valid before the platform requires a fresh sign-in.</p>
                  <div className="session-control-row">
                    <label className="field session-field">
                      <span>Password Rotation Days</span>
                      <input value={workspaceSecurityForm.passwordRotationDays} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, passwordRotationDays: event.target.value }))} inputMode="numeric" disabled={accessAdminState.loading || !canManageTenantAccess} />
                    </label>
                    <label className="field session-field">
                      <span>Session Timeout Minutes</span>
                      <input value={workspaceSecurityForm.sessionTimeoutMinutes} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, sessionTimeoutMinutes: event.target.value }))} inputMode="numeric" disabled={accessAdminState.loading || !canManageTenantAccess} />
                    </label>
                    <label className="field checkbox-field">
                      <span>Invalidate Other Sessions</span>
                      <input className="checkbox-input" type="checkbox" checked={workspaceSecurityForm.invalidateOtherSessions} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, invalidateOtherSessions: event.target.checked }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                    </label>
                  </div>
                  <div className="history-action-row">
                    <button
                      className="ghost-button"
                      onClick={saveWorkspaceSecuritySettings}
                      disabled={
                        accessAdminState.loading
                        || !canManageTenantAccess
                        || Number.parseInt(workspaceSecurityForm.passwordRotationDays, 10) < 7
                        || Number.parseInt(workspaceSecurityForm.sessionTimeoutMinutes, 10) < 15
                      }
                      type="button"
                    >
                      {accessAdminState.loading ? 'Working...' : 'Save Security Policy'}
                    </button>
                  </div>
                  <p className="muted-text">{workspaceSecurityForm.invalidateOtherSessions ? 'Other active tenant sessions will be forced to sign in again after this update.' : 'Leave session invalidation off when you only want to tune the tenant policy for future sessions.'}</p>
                </div>
              ) : null}
              {workspaceAdmin?.supportSummary ? (
                <div className="summary-grid">
                  <SummaryCard label="Warehouses" value={workspaceAdmin.supportSummary.warehouseCount} accent="blue" />
                  <SummaryCard label="Active Operators" value={workspaceAdmin.supportSummary.activeOperatorCount} accent="teal" />
                  <SummaryCard label="Active Users" value={workspaceAdmin.supportSummary.activeUserCount} accent="amber" />
                  <SummaryCard label="Pending Approvals" value={workspaceAdmin.supportSummary.pendingApprovalCount} accent="orange" />
                  <SummaryCard label="Replay Queue" value={workspaceAdmin.supportSummary.replayQueueDepth} accent="rose" />
                  <SummaryCard label="Active Incidents" value={workspaceAdmin.supportSummary.activeIncidentCount} accent="slate" />
                </div>
              ) : null}
              {workspaceAdmin?.supportDiagnostics ? (
                <div className="stack-card">
                  <div className="stack-title-row">
                    <strong>Support diagnostics</strong>
                    <div className="stack-tag-row">
                      <span className="status-tag status-partial">Tenant admin lane</span>
                    </div>
                  </div>
                  <p className="muted-text">Track workspace trust pressure across password hygiene, blocked access lanes, unowned connectors, and high-severity incidents without leaving tenant admin.</p>
                  <div className="summary-grid">
                    <SummaryCard label="Reset Required" value={workspaceAdmin.supportDiagnostics.activeUsersRequiringPasswordChange} accent="amber" />
                    <SummaryCard label="Rotation Overdue" value={workspaceAdmin.supportDiagnostics.activeUsersPastPasswordRotation} accent="orange" />
                    <SummaryCard label="Blocked Users" value={workspaceAdmin.supportDiagnostics.activeUsersBlockedByInactiveOperator} accent="rose" />
                    <SummaryCard label="Unowned Connectors" value={workspaceAdmin.supportDiagnostics.connectorsWithoutSupportOwner} accent="teal" />
                    <SummaryCard label="High Severity" value={workspaceAdmin.supportDiagnostics.highSeverityIncidentCount} accent="slate" />
                  </div>
                  <p className="muted-text">
                    {workspaceAdmin.supportDiagnostics.latestSupportAuditAt
                      ? `Latest support activity ${formatTimestamp(workspaceAdmin.supportDiagnostics.latestSupportAuditAt)}`
                      : 'Support activity will appear here after the first tenant admin, access, or security action.'}
                  </p>
                </div>
              ) : null}
              <div className="planner-compare-grid">
                <article className="comparison-card">
                  <div className="panel-header">
                    <div><p className="panel-kicker">Support incidents</p><h2>Tenant issues needing attention</h2></div>
                    <span className="panel-badge scenario-badge">{workspaceAdmin?.supportIncidents?.length || 0}</span>
                  </div>
                  <div className="stack-list">
                    {workspaceAdmin?.supportIncidents?.length ? workspaceAdmin.supportIncidents.map((incident) => (
                      <div key={incident.incidentKey} className="stack-card">
                        <div className="stack-title-row">
                          <strong>{incident.title}</strong>
                          <div className="stack-tag-row">
                            <span className={`status-tag ${getIncidentStatusClassName(incident.severity)}`}>{formatCodeLabel(incident.severity)}</span>
                            <span className={`status-tag ${incident.actionRequired ? 'status-failure' : 'status-success'}`}>{incident.actionRequired ? 'Action Required' : 'Monitoring'}</span>
                          </div>
                        </div>
                        <p>{incident.detail}</p>
                        <p className="muted-text">{incident.context} | {formatTimestamp(incident.createdAt)}</p>
                      </div>
                    )) : <div className="empty-state">{accessAdminState.loading ? 'Loading tenant support incidents...' : 'No active tenant support incidents. Workspace controls and integrations are currently healthy.'}</div>}
                  </div>
                </article>
                <article className="comparison-card">
                  <div className="panel-header">
                    <div><p className="panel-kicker">Support activity</p><h2>Recent admin and security events</h2></div>
                    <span className="panel-badge scenario-badge">{workspaceAdmin?.recentSupportActivity?.length || 0}</span>
                  </div>
                  <div className="stack-list">
                    {workspaceAdmin?.recentSupportActivity?.length ? workspaceAdmin.recentSupportActivity.map((activity) => (
                      <div key={activity.id} className="stack-card">
                        <div className="stack-title-row">
                          <strong>{activity.title}</strong>
                          <div className="stack-tag-row">
                            <span className="scenario-type-tag">{formatCodeLabel(activity.category)}</span>
                            <span className={`status-tag ${activity.status === 'FAILURE' ? 'status-failure' : 'status-success'}`}>{formatCodeLabel(activity.status)}</span>
                          </div>
                        </div>
                        <p>{activity.details}</p>
                        <p className="muted-text">
                          {activity.actor} | {formatCodeLabel(activity.action)}
                          {activity.targetRef ? ` | ${activity.targetRef}` : ''}
                        </p>
                        <p className="muted-text">Request {activity.requestId} | {formatTimestamp(activity.createdAt)}</p>
                      </div>
                    )) : <div className="empty-state">{accessAdminState.loading ? 'Loading support activity...' : 'Recent tenant admin, access, and security events will appear here after workspace changes occur.'}</div>}
                  </div>
                </article>
              </div>
              <div className="planner-compare-grid">
                <article className="comparison-card">
                  <div className="panel-header">
                    <div><p className="panel-kicker">Warehouse settings</p><h2>Names and locations</h2></div>
                    <span className="panel-badge scenario-badge">{workspaceAdmin?.warehouses?.length || 0}</span>
                  </div>
                  <div className="stack-list">
                    {workspaceAdmin?.warehouses?.length ? workspaceAdmin.warehouses.map((warehouse) => {
                      const draft = workspaceWarehouseDrafts[warehouse.id] || { name: warehouse.name, location: warehouse.location }
                      return (
                        <div key={warehouse.id} className="stack-card">
                          <div className="stack-title-row">
                            <strong>{warehouse.code}</strong>
                            <div className="stack-tag-row">
                              <span className="status-tag status-success">Active</span>
                            </div>
                          </div>
                          <div className="session-control-row">
                            <label className="field session-field">
                              <span>Name</span>
                              <input value={draft.name} onChange={(event) => setWorkspaceWarehouseDrafts((current) => ({ ...current, [warehouse.id]: { ...draft, name: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                            </label>
                            <label className="field session-field">
                              <span>Location</span>
                              <input value={draft.location} onChange={(event) => setWorkspaceWarehouseDrafts((current) => ({ ...current, [warehouse.id]: { ...draft, location: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                            </label>
                          </div>
                          <div className="history-action-row">
                            <button className="ghost-button" onClick={() => saveWorkspaceWarehouse(warehouse.id)} disabled={accessAdminState.loading || !canManageTenantAccess || !draft.name.trim() || !draft.location.trim()} type="button">Save Warehouse</button>
                          </div>
                        </div>
                      )
                    }) : <div className="empty-state">{accessAdminState.loading ? 'Loading workspace warehouses...' : 'Warehouse settings will appear here once the tenant workspace is loaded.'}</div>}
                  </div>
                </article>
                <article className="comparison-card">
                  <div className="panel-header">
                    <div><p className="panel-kicker">Connector support</p><h2>Owners and support notes</h2></div>
                    <span className="panel-badge scenario-badge">{workspaceAdmin?.connectors?.length || 0}</span>
                  </div>
                  <div className="stack-list">
                    {workspaceAdmin?.connectors?.length ? workspaceAdmin.connectors.map((connector) => {
                      const draft = workspaceConnectorDrafts[connector.id] || {
                        supportOwnerActorName: connector.supportOwnerActorName || '',
                        syncMode: connector.syncMode || 'REALTIME_PUSH',
                        syncIntervalMinutes: connector.syncIntervalMinutes == null ? '' : String(connector.syncIntervalMinutes),
                        validationPolicy: connector.validationPolicy || 'STANDARD',
                        transformationPolicy: connector.transformationPolicy || 'NONE',
                        allowDefaultWarehouseFallback: Boolean(connector.allowDefaultWarehouseFallback),
                        notes: connector.notes || '',
                      }
                      const ownerOptions = accessAdminOperators.filter((operator) => operator.active && operatorCanOwnConnector(operator, connector))
                      return (
                        <div key={connector.id} className="stack-card">
                          <div className="stack-title-row">
                            <strong>{connector.displayName}</strong>
                            <div className="stack-tag-row">
                              <span className="scenario-type-tag">{formatCodeLabel(connector.syncMode)}</span>
                              <span className="scenario-type-tag">{formatCodeLabel(connector.validationPolicy)}</span>
                              <span className={`status-tag ${connector.enabled ? 'status-success' : 'status-failure'}`}>{connector.enabled ? 'Enabled' : 'Disabled'}</span>
                            </div>
                          </div>
                          <p>{connector.sourceSystem} | {formatCodeLabel(connector.type)}</p>
                          <p className="muted-text">{connector.defaultWarehouseCode ? `Default warehouse ${connector.defaultWarehouseCode}` : 'Tenant-wide connector lane'}</p>
                          <div className="session-control-row">
                            <label className="field session-field">
                              <span>Sync Mode</span>
                              <select value={draft.syncMode} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [connector.id]: { ...draft, syncMode: event.target.value, syncIntervalMinutes: event.target.value === 'SCHEDULED_PULL' ? draft.syncIntervalMinutes || '60' : '' } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                                {integrationSyncModes.map((mode) => <option key={mode} value={mode}>{formatCodeLabel(mode)}</option>)}
                              </select>
                            </label>
                            <label className="field session-field">
                              <span>Validation</span>
                              <select value={draft.validationPolicy} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [connector.id]: { ...draft, validationPolicy: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                                {integrationValidationPolicies.map((policy) => <option key={policy} value={policy}>{formatCodeLabel(policy)}</option>)}
                              </select>
                            </label>
                            <label className="field session-field">
                              <span>Transform</span>
                              <select value={draft.transformationPolicy} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [connector.id]: { ...draft, transformationPolicy: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                                {integrationTransformationPolicies.map((policy) => <option key={policy} value={policy}>{formatCodeLabel(policy)}</option>)}
                              </select>
                            </label>
                            <label className="field checkbox-field">
                              <span>Warehouse Fallback</span>
                              <input className="checkbox-input" type="checkbox" checked={draft.allowDefaultWarehouseFallback} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [connector.id]: { ...draft, allowDefaultWarehouseFallback: event.target.checked } }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                            </label>
                          </div>
                          <div className="session-control-row">
                            <label className="field session-field">
                              <span>Sync Interval Minutes</span>
                              <input value={draft.syncIntervalMinutes} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [connector.id]: { ...draft, syncIntervalMinutes: event.target.value } }))} placeholder={draft.syncMode === 'SCHEDULED_PULL' ? '60' : 'Only used for scheduled pull'} inputMode="numeric" disabled={accessAdminState.loading || !canManageTenantAccess || draft.syncMode !== 'SCHEDULED_PULL'} />
                            </label>
                            <label className="field session-field">
                              <span>Support Owner</span>
                              <select value={draft.supportOwnerActorName} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [connector.id]: { ...draft, supportOwnerActorName: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                                <option value="">Unassigned</option>
                                {ownerOptions.map((operator) => <option key={operator.id} value={operator.actorName}>{operator.displayName}</option>)}
                              </select>
                            </label>
                            <label className="field session-field">
                              <span>Support Notes</span>
                              <input value={draft.notes} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [connector.id]: { ...draft, notes: event.target.value } }))} placeholder="Connector ownership or escalation notes" disabled={accessAdminState.loading || !canManageTenantAccess} />
                            </label>
                          </div>
                          <p className="muted-text">{connector.supportOwnerDisplayName ? `Current owner: ${connector.supportOwnerDisplayName}` : 'No active support owner assigned yet.'}</p>
                          <div className="history-action-row">
                            <button className="ghost-button" onClick={() => saveWorkspaceConnectorSupport(connector.id)} disabled={accessAdminState.loading || !canManageTenantAccess || (draft.syncMode === 'SCHEDULED_PULL' && Number.parseInt(draft.syncIntervalMinutes, 10) < 15)} type="button">Save Connector Policy</button>
                          </div>
                        </div>
                      )
                    }) : <div className="empty-state">{accessAdminState.loading ? 'Loading workspace connectors...' : 'Connector support ownership will appear here once the tenant workspace is loaded.'}</div>}
                  </div>
                </article>
              </div>
            </article>
            <div className="planner-compare-grid">
              <article className="comparison-card">
                <div className="panel-header">
                  <div><p className="panel-kicker">{accessOperatorForm.id ? 'Edit operator' : 'New operator'}</p><h2>{accessOperatorForm.id ? 'Update an access lane' : 'Create an access lane'}</h2></div>
                  <span className="panel-badge scenario-badge">{accessAdminOperators.length}</span>
                </div>
                <div className="session-control-row">
                  <label className="field session-field">
                    <span>Actor Name</span>
                    <input value={accessOperatorForm.actorName} onChange={(event) => setAccessOperatorForm((current) => ({ ...current, actorName: event.target.value }))} placeholder="North Review Manager" disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                  <label className="field session-field">
                    <span>Display Name</span>
                    <input value={accessOperatorForm.displayName} onChange={(event) => setAccessOperatorForm((current) => ({ ...current, displayName: event.target.value }))} placeholder="North Review Manager" disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                  <label className="field session-field">
                    <span>Roles</span>
                    <input value={accessOperatorForm.rolesText} onChange={(event) => setAccessOperatorForm((current) => ({ ...current, rolesText: event.target.value }))} placeholder="REVIEW_OWNER,FINAL_APPROVER" disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                  <label className="field session-field">
                    <span>Warehouse Scopes</span>
                    <input value={accessOperatorForm.warehouseScopesText} onChange={(event) => setAccessOperatorForm((current) => ({ ...current, warehouseScopesText: event.target.value }))} placeholder="WH-NORTH,WH-COAST" disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                  <label className="field session-field">
                    <span>Description</span>
                    <input value={accessOperatorForm.description} onChange={(event) => setAccessOperatorForm((current) => ({ ...current, description: event.target.value }))} placeholder="Scoped reviewer for a warehouse lane" disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                  <label className="field checkbox-field">
                    <span>Active</span>
                    <input className="checkbox-input" type="checkbox" checked={accessOperatorForm.active} onChange={(event) => setAccessOperatorForm((current) => ({ ...current, active: event.target.checked }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                </div>
                <div className="demo-action-row">
                  <button
                    className="secondary-button"
                    onClick={saveTenantOperator}
                    disabled={accessAdminState.loading || !canManageTenantAccess || !accessOperatorForm.actorName.trim() || !accessOperatorForm.displayName.trim() || !parseCsvValues(accessOperatorForm.rolesText).length}
                    type="button"
                  >
                    {accessAdminState.loading ? 'Working...' : accessOperatorForm.id ? 'Save Operator' : 'Create Operator'}
                  </button>
                  {accessOperatorForm.id ? (
                    <button className="ghost-button" onClick={resetAccessOperatorEditor} disabled={accessAdminState.loading} type="button">Clear</button>
                  ) : null}
                </div>
                <div className="stack-list">
                  {accessAdminOperators.length ? accessAdminOperators.map((operator) => (
                    <div key={operator.id} className="stack-card">
                      <div className="stack-title-row">
                        <strong>{operator.displayName}</strong>
                        <div className="stack-tag-row">
                          <span className={`status-tag ${operator.active ? 'status-success' : 'status-failure'}`}>{operator.active ? 'Active' : 'Inactive'}</span>
                        </div>
                      </div>
                      <p>{operator.actorName}</p>
                      <p className="muted-text">{operator.roles.length ? operator.roles.map((role) => formatCodeLabel(role)).join(', ') : 'No roles assigned yet'}</p>
                      <p className="muted-text">{operator.warehouseScopes.length ? `Warehouse lanes: ${operator.warehouseScopes.join(', ')}` : 'Warehouse lanes: tenant-wide'}</p>
                      <div className="history-action-row">
                        <button className="ghost-button" onClick={() => editTenantOperator(operator)} disabled={accessAdminState.loading || !canManageTenantAccess} type="button">Edit</button>
                      </div>
                    </div>
                  )) : <div className="empty-state">{accessAdminState.loading ? 'Loading operators...' : 'No managed operators yet beyond the seeded tenant directory.'}</div>}
                </div>
              </article>
              <article className="comparison-card">
                <div className="panel-header">
                  <div><p className="panel-kicker">{accessUserForm.id ? 'Edit user' : 'New user'}</p><h2>{accessUserForm.id ? 'Manage sign-in access' : 'Issue sign-in access'}</h2></div>
                  <span className="panel-badge scenario-badge">{accessAdminUsers.length}</span>
                </div>
                <div className="session-control-row">
                  <label className="field session-field">
                    <span>Username</span>
                    <input value={accessUserForm.username} onChange={(event) => setAccessUserForm((current) => ({ ...current, username: event.target.value }))} placeholder="north.review.manager" disabled={accessAdminState.loading || !canManageTenantAccess || Boolean(accessUserForm.id)} />
                  </label>
                  <label className="field session-field">
                    <span>Full Name</span>
                    <input value={accessUserForm.fullName} onChange={(event) => setAccessUserForm((current) => ({ ...current, fullName: event.target.value }))} placeholder="North Review Manager" disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                  <label className="field session-field">
                    <span>{accessUserForm.id ? 'New Password' : 'Password'}</span>
                    <input type="password" value={accessUserForm.password} onChange={(event) => setAccessUserForm((current) => ({ ...current, password: event.target.value }))} placeholder={accessUserForm.id ? 'Enter a reset password' : 'Choose a password'} disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                  <label className="field session-field">
                    <span>Operator</span>
                    <select value={accessUserForm.operatorActorName} onChange={(event) => setAccessUserForm((current) => ({ ...current, operatorActorName: event.target.value }))} disabled={accessAdminState.loading || !canManageTenantAccess || !accessAdminOperators.length}>
                      {accessAdminOperators.length
                        ? accessAdminOperators.filter((operator) => operator.active).map((operator) => <option key={operator.id} value={operator.actorName}>{operator.displayName}</option>)
                        : <option value="">Create an operator first</option>}
                    </select>
                  </label>
                  <label className="field checkbox-field">
                    <span>Active</span>
                    <input className="checkbox-input" type="checkbox" checked={accessUserForm.active} onChange={(event) => setAccessUserForm((current) => ({ ...current, active: event.target.checked }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                </div>
                {accessUserForm.id ? <p className="muted-text">Usernames stay fixed. Change operator mapping or active state here, and use the password field only when you want to issue a reset.</p> : null}
                <div className="demo-action-row">
                  <button
                    className="secondary-button"
                    onClick={saveTenantUser}
                    disabled={accessAdminState.loading || !canManageTenantAccess || !accessUserForm.fullName.trim() || !accessUserForm.operatorActorName || (!accessUserForm.id && (!accessUserForm.username.trim() || !accessUserForm.password.trim()))}
                    type="button"
                  >
                    {accessAdminState.loading ? 'Working...' : accessUserForm.id ? 'Save User' : 'Create User'}
                  </button>
                  {accessUserForm.id ? (
                    <button className="ghost-button" onClick={resetTenantUserPassword} disabled={accessAdminState.loading || !canManageTenantAccess || accessUserForm.password.trim().length < 8} type="button">Reset Password</button>
                  ) : null}
                  {accessUserForm.id ? (
                    <button className="ghost-button" onClick={() => resetAccessUserEditor(accessUserForm.operatorActorName)} disabled={accessAdminState.loading} type="button">Clear</button>
                  ) : null}
                </div>
                {accessAdminState.error ? <p className="error-text">{accessAdminState.error}</p> : null}
                {accessAdminState.success ? <p className="success-text">{accessAdminState.success}</p> : null}
                <div className="stack-list">
                  {accessAdminUsers.length ? accessAdminUsers.map((user) => (
                    <div key={user.id} className="stack-card">
                      <div className="stack-title-row">
                        <strong>{user.fullName}</strong>
                        <div className="stack-tag-row">
                          <span className={`status-tag ${user.active ? 'status-success' : 'status-failure'}`}>{user.active ? 'Active' : 'Inactive'}</span>
                          {user.passwordChangeRequired ? <span className="status-tag status-partial">Password Reset Required</span> : null}
                        </div>
                      </div>
                      <p>{user.username} | {user.operatorDisplayName}</p>
                      <p className="muted-text">{user.roles.length ? user.roles.map((role) => formatCodeLabel(role)).join(', ') : 'No roles assigned yet'}</p>
                      <p className="muted-text">{user.warehouseScopes.length ? `Warehouse lanes: ${user.warehouseScopes.join(', ')}` : 'Warehouse lanes: tenant-wide'}</p>
                      <p className="muted-text">Password updated {user.passwordUpdatedAt ? formatTimestamp(user.passwordUpdatedAt) : 'Monitoring'}.</p>
                      <div className="history-action-row">
                        <button className="ghost-button" onClick={() => editTenantUser(user)} disabled={accessAdminState.loading || !canManageTenantAccess} type="button">Edit</button>
                      </div>
                    </div>
                  )) : <div className="empty-state">{accessAdminState.loading ? 'Loading tenant users...' : 'No additional tenant user accounts have been created yet.'}</div>}
                </div>
              </article>
            </div>
          </article>
          <article className="panel" hidden={!isAuthenticated || !showScenarioNotifications}>
            <div className="panel-header">
              <div><p className="panel-kicker">Ops notices</p><h2>Scenario notifications</h2></div>
              <span className="panel-badge notification-badge">{snapshot.scenarioNotifications.length}</span>
            </div>
            <div className="stack-list">
              {snapshot.scenarioNotifications.length ? snapshot.scenarioNotifications.map((notification) => (
                <div key={`${notification.type}-${notification.scenarioRunId}-${notification.createdAt}`} className="stack-card">
                  <div className="stack-title-row">
                    <strong>{notification.title}</strong>
                    <div className="stack-tag-row">
                      <span className="scenario-type-tag">{formatCodeLabel(notification.type)}</span>
                      {notification.reviewPriority ? <span className={`priority-tag priority-${notification.reviewPriority.toLowerCase()}`}>{formatCodeLabel(notification.reviewPriority)}</span> : null}
                      <span className={`status-tag ${notification.actionRequired ? 'status-failure' : 'status-success'}`}>
                        {notification.actionRequired ? 'Action Required' : 'Owned'}
                      </span>
                    </div>
                  </div>
                  <p>{notification.message}</p>
                  <p className="muted-text">
                    {notification.warehouseCode ? `${notification.warehouseCode} | ` : ''}
                    {notification.approvalStage ? `Stage ${formatCodeLabel(notification.approvalStage)} | ` : ''}
                    {notification.actor ? `${notification.actionRequired ? 'Assigned to' : 'Handled by'} ${notification.actor}` : 'Monitoring'}
                  </p>
                  {notification.note ? <p className="muted-text">Note: {notification.note}</p> : null}
                  <p className={`muted-text${notification.actionRequired ? ' overdue-text' : ''}`}>
                    {notification.actionRequired && notification.dueAt ? `Due ${formatTimestamp(notification.dueAt)} | ` : ''}
                    {formatTimestamp(notification.createdAt)}
                  </p>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading scenario notifications...' : 'Scenario notifications will appear here when critical saved plans are rerouted or formally owned.'}</div>}
            </div>
          </article>
          <article className="panel" hidden={!isAuthenticated || !showEscalationInbox}>
            <div className="panel-header">
              <div><p className="panel-kicker">Escalation inbox</p><h2>SLA escalations</h2></div>
              <span className="panel-badge scenario-badge">{snapshot.slaEscalations.length}</span>
            </div>
            <div className="stack-list">
              {scenarioEscalationAckState.error ? <p className="error-text">{scenarioEscalationAckState.error}</p> : null}
              {scenarioEscalationAckState.success ? <p className="muted-text">{scenarioEscalationAckState.success}</p> : null}
              {snapshot.slaEscalations.length ? snapshot.slaEscalations.map((scenario) => (
                <div key={scenario.id} className="stack-card">
                  <div className="stack-title-row">
                    <strong>{scenario.title}</strong>
                    <div className="stack-tag-row">
                      {scenario.reviewPriority ? <span className={`priority-tag priority-${scenario.reviewPriority.toLowerCase()}`}>{formatCodeLabel(scenario.reviewPriority)}</span> : null}
                      <span className="policy-tag policy-escalated">SLA Escalated</span>
                    </div>
                  </div>
                  <p>{scenario.summary}</p>
                  <p className="muted-text">
                    {scenario.warehouseCode ? `${scenario.warehouseCode} | ` : ''}
                    Stage {formatCodeLabel(scenario.approvalStage)} | Final approver {scenario.finalApprovalOwner || 'Monitoring'}
                  </p>
                  <p className="muted-text">Escalated to {scenario.slaEscalatedTo} at {formatTimestamp(scenario.slaEscalatedAt)}</p>
                  <p className="muted-text">Acknowledgment requires {formatCodeLabel('ESCALATION_OWNER')}.</p>
                  <p className="error-text">Due {formatTimestamp(scenario.approvalDueAt)} and still pending.</p>
                  <div className="history-action-row">
                    <button
                      className="approve-button"
                      onClick={() => acknowledgeScenarioEscalation(scenario.id)}
                      disabled={scenarioEscalationAckState.loadingId === scenario.id || !signedInSession || !scenarioReviewNote.trim() || scenarioActorRole !== 'ESCALATION_OWNER' || !signedInRoles.includes('ESCALATION_OWNER') || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)}
                      type="button"
                    >
                      {scenarioEscalationAckState.loadingId === scenario.id ? 'Acknowledging...' : 'Acknowledge Escalation'}
                    </button>
                  </div>
                </div>
              )) : <div className="empty-state">{pageState.loading ? 'Loading escalation inbox...' : 'No unacknowledged SLA-escalated plans. Critical reviews are currently within SLA or already owned.'}</div>}
            </div>
          </article>
          <article className="panel" hidden={!isAuthenticated || !showScenarioHistory}>
            <div className="panel-header">
              <div><p className="panel-kicker">Planning memory</p><h2>Recent scenario history</h2></div>
              <span className="panel-badge scenario-badge">{scenarioHistoryItems.length}</span>
            </div>
            <div className="history-filter-bar">
              <div className="history-filter-grid">
                <label className="field">
                  <span>Type</span>
                  <select value={scenarioHistoryFilters.type} onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, type: event.target.value }))}>
                    <option value="ALL">All runs</option>
                    <option value="PREVIEW">Preview</option>
                    <option value="COMPARISON">Comparison</option>
                    <option value="SAVED_PLAN">Saved plan</option>
                    <option value="EXECUTION">Execution</option>
                  </select>
                </label>
                <label className="field">
                  <span>Approval</span>
                  <select value={scenarioHistoryFilters.approvalStatus} onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, approvalStatus: event.target.value }))}>
                    <option value="ALL">All approval states</option>
                    <option value="NOT_REQUIRED">Not required</option>
                    <option value="PENDING_APPROVAL">Pending approval</option>
                    <option value="APPROVED">Approved</option>
                    <option value="REJECTED">Rejected</option>
                  </select>
                </label>
                <label className="field">
                  <span>Policy</span>
                  <select value={scenarioHistoryFilters.approvalPolicy} onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, approvalPolicy: event.target.value }))}>
                    <option value="ALL">All policies</option>
                    <option value="STANDARD">Standard</option>
                    <option value="ESCALATED">Escalated</option>
                  </select>
                </label>
                <label className="field">
                  <span>Stage</span>
                  <select value={scenarioHistoryFilters.approvalStage} onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, approvalStage: event.target.value }))}>
                    <option value="ALL">All stages</option>
                    <option value="NOT_REQUIRED">Not required</option>
                    <option value="PENDING_REVIEW">Pending review</option>
                    <option value="PENDING_FINAL_APPROVAL">Pending final approval</option>
                    <option value="APPROVED">Approved</option>
                    <option value="REJECTED">Rejected</option>
                  </select>
                </label>
                <label className="field">
                  <span>Warehouse</span>
                  <select value={scenarioHistoryFilters.warehouseCode} onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, warehouseCode: event.target.value }))}>
                    {historyWarehouseOptions.map((warehouse) => <option key={warehouse.code} value={warehouse.code}>{warehouse.name}</option>)}
                  </select>
                </label>
                <label className="field">
                  <span>Requested By</span>
                  <input
                    type="text"
                    placeholder="Filter owner"
                    value={scenarioHistoryFilters.requestedBy}
                    onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, requestedBy: event.target.value }))}
                  />
                </label>
                <label className="field">
                  <span>Review Owner</span>
                  <input
                    type="text"
                    placeholder="Filter reviewer"
                    value={scenarioHistoryFilters.reviewOwner}
                    onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, reviewOwner: event.target.value }))}
                  />
                </label>
                <label className="field">
                  <span>Final Approver</span>
                  <input
                    type="text"
                    placeholder="Filter final approver"
                    value={scenarioHistoryFilters.finalApprovalOwner}
                    onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, finalApprovalOwner: event.target.value }))}
                  />
                </label>
                <label className="field">
                  <span>Minimum Priority</span>
                  <select value={scenarioHistoryFilters.minimumReviewPriority} onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, minimumReviewPriority: event.target.value }))}>
                    <option value="ALL">All priorities</option>
                    <option value="MEDIUM">Medium and above</option>
                    <option value="HIGH">High and above</option>
                    <option value="CRITICAL">Critical only</option>
                  </select>
                </label>
                <label className="field checkbox-field">
                  <span>Overdue Only</span>
                  <input
                    className="checkbox-input"
                    type="checkbox"
                    checked={scenarioHistoryFilters.overdueOnly}
                    onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, overdueOnly: event.target.checked }))}
                  />
                </label>
                <label className="field checkbox-field">
                  <span>SLA Escalated</span>
                  <input
                    className="checkbox-input"
                    type="checkbox"
                    checked={scenarioHistoryFilters.slaEscalatedOnly}
                    onChange={(event) => setScenarioHistoryFilters((current) => ({ ...current, slaEscalatedOnly: event.target.checked }))}
                  />
                </label>
              </div>
              <div className="history-filter-actions">
                <p className="muted-text">Showing {scenarioHistoryItems.length} planning runs.</p>
                {hasActiveScenarioHistoryFilters(scenarioHistoryFilters) ? (
                  <button className="ghost-button" onClick={() => setScenarioHistoryFilters(defaultScenarioHistoryFilters)} type="button">Clear Filters</button>
                ) : null}
              </div>
            </div>
            <div className="stack-list">
              {scenarioHistoryState.error ? <p className="error-text">{scenarioHistoryState.error}</p> : null}
              {scenarioHistoryItems.length ? scenarioHistoryItems.map((scenario) => (
                <div key={scenario.id} className="stack-card">
                  <div className="stack-title-row">
                    <strong>{scenario.title}</strong>
                    <div className="stack-tag-row">
                      <span className="scenario-type-tag">{formatCodeLabel(scenario.type)}</span>
                      {scenario.reviewPriority ? <span className={`priority-tag priority-${scenario.reviewPriority.toLowerCase()}`}>{formatCodeLabel(scenario.reviewPriority)}</span> : null}
                      {scenario.overdue ? <span className="status-tag status-failure">Overdue</span> : null}
                      {scenario.slaEscalated ? <span className="policy-tag policy-escalated">SLA Escalated</span> : null}
                      {scenario.slaAcknowledged ? <span className="status-tag status-success">Acknowledged</span> : null}
                    </div>
                  </div>
                  <p>{scenario.summary}</p>
                  <p className="muted-text">
                    {scenario.warehouseCode ? `${scenario.warehouseCode} | ` : ''}
                    Approval: {formatCodeLabel(scenario.approvalStatus)}
                    {scenario.approvalPolicy ? ` | Policy ${formatCodeLabel(scenario.approvalPolicy)}` : ''}
                    {scenario.approvalStage ? ` | Stage ${formatCodeLabel(scenario.approvalStage)}` : ''}
                    {scenario.requestedBy ? ` | Requested by ${scenario.requestedBy}` : ''}
                    {scenario.reviewOwner ? ` | Review owner ${scenario.reviewOwner}` : ''}
                    {scenario.finalApprovalOwner ? ` | Final approver ${scenario.finalApprovalOwner}` : ''}
                    {scenario.slaEscalatedTo ? ` | Escalated to ${scenario.slaEscalatedTo}` : ''}
                    {scenario.approvedBy ? ` | Approved by ${scenario.approvedBy}` : ''}
                    {scenario.rejectedBy ? ` | Rejected by ${scenario.rejectedBy}` : ''}
                  </p>
                  {scenario.reviewPriority ? (
                    <div className="history-risk-line">
                      <span className={`priority-tag priority-${scenario.reviewPriority.toLowerCase()}`}>{formatCodeLabel(scenario.reviewPriority)} review</span>
                      {scenario.approvalPolicy ? <span className={`policy-tag policy-${scenario.approvalPolicy.toLowerCase()}`}>{formatCodeLabel(scenario.approvalPolicy)} policy</span> : null}
                      {scenario.approvalStage ? <span className={`stage-tag stage-${scenario.approvalStage.toLowerCase().replace(/_/g, '-')}`}>{formatCodeLabel(scenario.approvalStage)}</span> : null}
                      {scenario.riskScore != null ? <span className="history-risk-score">Risk score {scenario.riskScore}</span> : null}
                      {scenario.approvalDueAt ? <span className={`history-risk-score${scenario.overdue ? ' overdue-text' : ''}`}>Due {formatTimestamp(scenario.approvalDueAt)}</span> : null}
                    </div>
                  ) : null}
                  {scenario.approvalPolicy === 'ESCALATED' ? <p className="muted-text">Escalated plans require owner review first, then final approval from {scenario.finalApprovalOwner || 'the assigned final approver'}, who must be distinct from both the requester and the owner reviewer.</p> : null}
                  {scenario.approvalStatus === 'PENDING_APPROVAL' ? <p className="muted-text">Approval requires {formatCodeLabel(getScenarioApprovalRole(scenario))}. Rejection requires {formatCodeLabel(getScenarioRejectionRole(scenario))}.</p> : null}
                  {scenario.overdue ? <p className="error-text">Approval SLA is overdue for the current review stage.</p> : null}
                  {scenario.slaEscalated ? <p className="muted-text">SLA escalation rerouted this plan to {scenario.slaEscalatedTo} at {formatTimestamp(scenario.slaEscalatedAt)}.</p> : null}
                  {scenario.slaAcknowledged ? <p className="muted-text">Escalation acknowledged by {scenario.slaAcknowledgedBy} at {formatTimestamp(scenario.slaAcknowledgedAt)}{scenario.slaAcknowledgementNote ? ` | Note: ${scenario.slaAcknowledgementNote}` : ''}</p> : null}
                  {scenario.reviewApprovedBy ? <p className="muted-text">Owner review: {scenario.reviewApprovedBy} at {formatTimestamp(scenario.reviewApprovedAt)}{scenario.reviewApprovalNote ? ` | Note: ${scenario.reviewApprovalNote}` : ''}</p> : null}
                  {scenario.revisionNumber ? <p className="muted-text">Revision {scenario.revisionNumber}{scenario.revisionOfScenarioRunId ? ` | Based on plan ${scenario.revisionOfScenarioRunId}` : ''}</p> : null}
                  {scenario.rejectionReason ? <p className="muted-text">Review note: {scenario.rejectionReason}</p> : null}
                  {scenario.approvalNote ? <p className="muted-text">Approval note: {scenario.approvalNote}</p> : null}
                  <p className="muted-text">Recommended option: {scenario.recommendedOption || 'No action needed'} | {formatTimestamp(scenario.createdAt)}</p>
                  {scenario.loadable || scenario.executable || (scenario.type === 'SAVED_PLAN' && scenario.approvalStatus !== 'REJECTED') ? (
                    <div className="history-action-row">
                      {scenario.loadable ? (
                        <button
                          className="ghost-button"
                          onClick={() => loadScenarioIntoPlanner(scenario.id)}
                          disabled={scenarioLoadState.loadingId === scenario.id}
                          type="button"
                        >
                          {scenarioLoadState.loadingId === scenario.id ? 'Loading...' : 'Load Into Planner'}
                        </button>
                      ) : null}
                      {scenario.type === 'SAVED_PLAN' && scenario.approvalStatus === 'PENDING_APPROVAL' ? (
                        <button
                          className="approve-button"
                          onClick={() => approveScenarioPlan(scenario.id)}
                          disabled={scenarioApprovalState.loadingId === scenario.id || !signedInSession || ((scenario.approvalPolicy === 'ESCALATED' || scenario.approvalStage === 'PENDING_FINAL_APPROVAL') && !scenarioReviewNote.trim()) || scenarioActorRole !== getScenarioApprovalRole(scenario) || !signedInRoles.includes(getScenarioApprovalRole(scenario)) || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)}
                          type="button"
                        >
                          {scenarioApprovalState.loadingId === scenario.id ? 'Approving...' : scenario.approvalPolicy === 'ESCALATED' && scenario.approvalStage === 'PENDING_REVIEW' ? 'Owner Review' : scenario.approvalPolicy === 'ESCALATED' ? 'Final Approve' : 'Approve Plan'}
                        </button>
                      ) : null}
                      {scenario.type === 'SAVED_PLAN' && scenario.approvalStatus !== 'REJECTED' ? (
                        <button
                          className="reject-button"
                          onClick={() => rejectScenarioPlan(scenario.id)}
                          disabled={scenarioRejectionState.loadingId === scenario.id || !signedInSession || !scenarioReviewNote.trim() || scenarioActorRole !== getScenarioRejectionRole(scenario) || !signedInRoles.includes(getScenarioRejectionRole(scenario)) || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)}
                          type="button"
                        >
                          {scenarioRejectionState.loadingId === scenario.id ? 'Rejecting...' : 'Reject Plan'}
                        </button>
                      ) : null}
                      {scenario.executable ? (
                        <button
                          className="secondary-button"
                          onClick={() => executeScenario(scenario.id)}
                          disabled={scenarioExecutionState.loadingId === scenario.id}
                          type="button"
                        >
                          {scenarioExecutionState.loadingId === scenario.id ? 'Executing...' : 'Execute Scenario'}
                        </button>
                      ) : null}
                    </div>
                  ) : null}
                </div>
              )) : <div className="empty-state">{scenarioHistoryState.loading ? 'Loading planning history...' : scenarioHistoryEmptyMessage}</div>}
            </div>
          </article>
        </section>
        {renderLocationsOverview()}
        {renderProfileExperience()}
        {renderTenantManagementExperience()}
          </main>
          {renderWorkspaceUtilityRail()}
        </div>
      </div>
    </div>
  )
}
