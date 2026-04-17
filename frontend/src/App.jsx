import { startTransition, useDeferredValue, useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import AppShell from './layout/AppShell'
import Sidebar from './layout/Sidebar'
import Topbar from './layout/Topbar'
import WorkspaceUtilityRail from './layout/WorkspaceUtilityRail'
import WorkspacePageHeader from './layout/WorkspacePageHeader'
import WorkspaceNotices from './components/WorkspaceNotices'
import DashboardPage from './pages/Dashboard'
import AlertsPage from './pages/Alerts'
import RecommendationsPage from './pages/Recommendations'
import OrdersPage from './pages/Orders'
import InventoryPage from './pages/Inventory'
import IntegrationsPage from './pages/Integrations'
import LocationsPage from './pages/Locations'
import ReplayPage from './pages/Replay'
import RuntimePage from './pages/Runtime'
import SettingsPage from './pages/Settings'
import ProfilePage from './pages/Profile'
import SignInPage from './pages/SignIn'
import ApprovalsPage from './pages/Approvals'
import AuditPage from './pages/Audit'
import TenantsPage from './pages/Tenants'
import ScenarioControlPage from './pages/ScenarioControl'
import ScenarioPlannerPage from './pages/ScenarioPlanner'
import ScenarioHistoryPage from './pages/ScenarioHistory'
import FulfillmentPage from './pages/Fulfillment'
import EscalationsPage from './pages/Escalations'
import UsersPage from './pages/Users'
import PlatformAdminPage from './pages/PlatformAdmin'
import SystemConfigPage from './pages/SystemConfig'
import ReleasesPage from './pages/Releases'
import PublicExperience from './pages/PublicExperience'

const runtimeConfig = globalThis.__SYNAPSE_RUNTIME_CONFIG__ || {}
const workspacePreferenceStorageKey = 'synapsecore.workspacePreference'
const postAuthRedirectStorageKey = 'synapsecore.postAuthRedirect'
const trimTrailingSlash = (value = '') => value.replace(/\/+$/, '')
const isLocalHostname = (hostname = '') => ['localhost', '127.0.0.1', '0.0.0.0'].includes((hostname || '').toLowerCase())
const isExplicitNativeRealtimeUrl = (value = '') => /^wss?:/i.test(String(value).trim())
const normalizeAbsoluteUrl = (value) => {
  if (!value || !String(value).trim()) return ''
  try {
    return trimTrailingSlash(new URL(String(value).trim(), globalThis.location?.origin || undefined).toString())
  } catch {
    return trimTrailingSlash(String(value).trim())
  }
}
const resolveApiBaseUrl = () => {
  const configuredUrl = runtimeConfig.apiUrl || import.meta.env.VITE_API_URL
  if (configuredUrl) return normalizeAbsoluteUrl(configuredUrl)
  const browserOrigin = globalThis.location?.origin || ''
  return browserOrigin && !isLocalHostname(globalThis.location?.hostname || '') ? trimTrailingSlash(browserOrigin) : 'http://localhost:8080'
}
const resolveRealtimeUrl = (configuredUrl, apiBaseUrl, preferredProtocol) => {
  const rawValue = configuredUrl || (apiBaseUrl ? `${apiBaseUrl}/ws` : '')
  if (!rawValue || !String(rawValue).trim()) return ''
  try {
    const normalizedUrl = new URL(rawValue, globalThis.location?.origin || undefined)
    if (preferredProtocol === 'ws') {
      if (normalizedUrl.protocol === 'http:') normalizedUrl.protocol = 'ws:'
      if (normalizedUrl.protocol === 'https:') normalizedUrl.protocol = 'wss:'
    } else {
      if (normalizedUrl.protocol === 'ws:') normalizedUrl.protocol = 'http:'
      if (normalizedUrl.protocol === 'wss:') normalizedUrl.protocol = 'https:'
    }
    return trimTrailingSlash(normalizedUrl.toString())
  } catch {
    const nextValue = trimTrailingSlash(String(rawValue).trim())
    if (preferredProtocol === 'ws') {
      return nextValue
        .replace(/^http:/i, 'ws:')
        .replace(/^https:/i, 'wss:')
    }
    return nextValue
      .replace(/^ws:/i, 'http:')
      .replace(/^wss:/i, 'https:')
  }
}
const readStoredJson = (storage, key, fallbackValue) => {
  try {
    const rawValue = storage?.getItem?.(key)
    return rawValue ? JSON.parse(rawValue) : fallbackValue
  } catch {
    return fallbackValue
  }
}
const writeStoredJson = (storage, key, value) => {
  try {
    storage?.setItem?.(key, JSON.stringify(value))
  } catch {
    // Storage is optional in embedded or locked-down browsers.
  }
}
const removeStoredValue = (storage, key) => {
  try {
    storage?.removeItem?.(key)
  } catch {
    // Ignore storage cleanup issues so sign-in flow stays usable.
  }
}
const rememberedWorkspacePreference = readStoredJson(globalThis.localStorage, workspacePreferenceStorageKey, null)
const configuredRealtimeUrl = runtimeConfig.wsUrl || import.meta.env.VITE_WS_URL || ''
const apiUrl = resolveApiBaseUrl()
const websocketBrokerUrl = isExplicitNativeRealtimeUrl(configuredRealtimeUrl)
  ? resolveRealtimeUrl(configuredRealtimeUrl, apiUrl, 'ws')
  : ''
const sockJsUrl = resolveRealtimeUrl(configuredRealtimeUrl, apiUrl, 'http')
const wsUrl = websocketBrokerUrl || sockJsUrl
const realtimeTransportLabel = websocketBrokerUrl ? 'Native WebSocket / STOMP' : (sockJsUrl ? 'SockJS / STOMP' : 'Realtime not configured')
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
const defaultSignInTenantCode = rememberedWorkspacePreference?.remember ? (rememberedWorkspacePreference.tenantCode || '') : ''
const defaultSignInUsername = rememberedWorkspacePreference?.remember ? (rememberedWorkspacePreference.username || '') : ''
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
const readPendingPostAuthPage = () => {
  const pageKey = readStoredJson(globalThis.sessionStorage, postAuthRedirectStorageKey, '')
  return pageLookup[pageKey]?.audience === 'app' ? pageKey : ''
}
const storePendingPostAuthPage = (pageKey) => {
  if (pageLookup[pageKey]?.audience === 'app') {
    writeStoredJson(globalThis.sessionStorage, postAuthRedirectStorageKey, pageKey)
  }
}
const clearPendingPostAuthPage = () => removeStoredValue(globalThis.sessionStorage, postAuthRedirectStorageKey)
const routeAliases = {
  '/overview': '/dashboard',
  '/risk': '/alerts',
  '/operations': '/orders',
  '/planning': '/scenarios',
  '/workspace': '/users',
}
const navGroups = [
  { label: 'Overview', keys: ['dashboard', 'alerts', 'recommendations'] },
  { label: 'Operations', keys: ['orders', 'inventory', 'locations', 'fulfillment'] },
  { label: 'Control', keys: ['scenarios', 'scenario-history', 'approvals', 'escalations'] },
  { label: 'Systems', keys: ['integrations', 'replay', 'runtime', 'audit'] },
  { label: 'Settings', keys: ['users', 'settings', 'profile', 'platform', 'tenants', 'system-config', 'releases'] },
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
const readResponsePayload = async (response) => {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    try {
      return await response.json()
    } catch {
      return null
    }
  }

  try {
    const text = await response.text()
    return text ? { message: text } : null
  } catch {
    return null
  }
}
const extractResponseErrorMessage = (response, payload, fallbackMessage) => {
  if (typeof payload === 'string' && payload.trim()) return payload.trim()
  if (payload && typeof payload === 'object') {
    if (typeof payload.message === 'string' && payload.message.trim()) return payload.message.trim()
    if (typeof payload.detail === 'string' && payload.detail.trim()) return payload.detail.trim()
    if (typeof payload.error === 'string' && payload.error.trim()) return payload.error.trim()
    if (typeof payload.title === 'string' && payload.title.trim()) return payload.title.trim()
  }

  if (response.status === 401) return 'Your session is missing or expired. Sign in again to reopen the workspace.'
  if (response.status === 403) return 'This operator does not have permission to perform that action.'
  return fallbackMessage
}

export default function App() {
  const [snapshot, setSnapshot] = useState(emptySnapshot)
  const [currentPage, setCurrentPage] = useState(() => resolvePageFromPath())
  const [workspaceSearch, setWorkspaceSearch] = useState('')
  const [rememberWorkspace, setRememberWorkspace] = useState(() => rememberedWorkspacePreference?.remember ?? true)
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
  const [selectedIntegrationConnectorId, setSelectedIntegrationConnectorId] = useState(null)
  const [selectedReplayRecordId, setSelectedReplayRecordId] = useState(null)
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
  const resetSignedInWorkspace = () => {
    setSnapshot(emptySnapshot)
    setScenarioHistoryState({ loading: false, error: '', items: [] })
    setOperatorDirectoryState({ loading: false, error: '', items: [] })
    setSystemRuntimeState({ loading: false, error: '', runtime: null })
    setAccessAdminState({ loading: false, error: '', success: '', workspace: null, operators: [], users: [] })
  }
  const handleExpiredSession = (message, attemptedPage = currentPage) => {
    storePendingPostAuthPage(pageLookup[attemptedPage]?.audience === 'app' ? attemptedPage : 'dashboard')
    resetSignedInWorkspace()
    setConnectionState('signed-out')
    setPasswordChangeState({ loading: false, error: '', success: '', form: createDefaultPasswordChangeForm() })
    setAuthSessionState((current) => ({
      ...current,
      loading: false,
      action: '',
      session: null,
      password: '',
      error: message,
    }))
    redirectToPage('sign-in')
  }
  const fetchApi = (path, init = {}) => {
    const headers = new Headers(init.headers || {})
    if (authSessionState.session?.tenantCode) {
      headers.set('X-Synapse-Tenant', authSessionState.session.tenantCode)
    }
    return fetch(`${apiUrl}${path}`, { credentials: 'include', ...init, headers })
  }
  const fetchJson = async (path, init = {}, options = {}) => {
    let response
    try {
      response = await fetchApi(path, init)
    } catch {
      throw new Error(`Unable to reach the SynapseCore backend at ${apiUrl}. Check the live API URL, CORS policy, and backend availability.`)
    }

    const payload = await readResponsePayload(response)
    if (!response.ok) {
      const message = extractResponseErrorMessage(response, payload, `Request to ${path} failed.`)
      if (response.status === 401 && !options.ignoreUnauthorized) {
        handleExpiredSession(message, currentPage)
      }
      throw new Error(message)
    }

    return payload
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
  const redirectToPage = (pageKey) => {
    const nextPath = buildPagePath(pageKey)
    if (globalThis.location?.pathname !== nextPath) {
      globalThis.history?.replaceState?.({}, '', nextPath)
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
    if (!sockJsUrl && !websocketBrokerUrl) {
      setConnectionState('degraded')
      return () => { active = false }
    }
    const topicPrefix = buildTenantTopicPrefix(activeTenantCode)
    setConnectionState('connecting')
    const client = new Client({
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      brokerURL: /^wss?:/i.test(websocketBrokerUrl) ? websocketBrokerUrl : undefined,
      webSocketFactory: /^wss?:/i.test(websocketBrokerUrl) ? undefined : () => new SockJS(sockJsUrl),
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
      onWebSocketError: () => setConnectionState('degraded'),
      onWebSocketClose: () => setConnectionState('reconnecting'),
    })
    client.activate()
    return () => { active = false; client.deactivate() }
  }, [activeTenantCode, websocketBrokerUrl, sockJsUrl])

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
    if (!rememberWorkspace) {
      removeStoredValue(globalThis.localStorage, workspacePreferenceStorageKey)
      return
    }

    writeStoredJson(globalThis.localStorage, workspacePreferenceStorageKey, {
      remember: true,
      tenantCode: authSessionState.tenantCode.trim(),
      username: authSessionState.username.trim(),
    })
  }, [rememberWorkspace, authSessionState.tenantCode, authSessionState.username])

  useEffect(() => {
    if (authSessionState.loading) return
    const currentMeta = pageLookup[currentPage] || pageLookup.landing
    if (!authSessionState.session && currentMeta.audience === 'app') {
      storePendingPostAuthPage(currentPage)
      redirectToPage('sign-in')
    }
  }, [authSessionState.loading, authSessionState.session, currentPage])

  useEffect(() => {
    if (authSessionState.loading || !authSessionState.session || currentPage !== 'sign-in') return
    const nextPage = readPendingPostAuthPage() || 'dashboard'
    clearPendingPostAuthPage()
    navigateToPage(nextPage)
  }, [authSessionState.loading, authSessionState.session, currentPage])

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
      await fetchJson(`/api/simulation/${nextAction}`, { method: 'POST' })
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
      const payload = await fetchJson('/api/integrations/orders/connectors', {
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
      const payload = await fetchJson(`/api/integrations/orders/replay/${recordId}`, { method: 'POST' })
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
  const selectedTenantOption = tenantDirectoryState.items.find((tenant) => tenant.code === authSessionState.tenantCode.trim())
  const signInWorkspaceHint = tenantDirectoryState.error
    ? 'Workspace directory lookup is unavailable. Enter the tenant code manually and continue with a valid operator account.'
    : tenantDirectoryState.loading
      ? 'Loading the active workspace directory so operators can sign in against the live tenant list.'
      : selectedTenantOption
        ? `Signing into ${selectedTenantOption.name}.`
        : 'Enter the tenant code exactly as it exists in SynapseCore, or pick it from the live directory suggestions.'
  const signInConfigHint = `API ${apiUrl || 'missing'} | Realtime ${wsUrl || 'missing'} | Transport ${realtimeTransportLabel}`
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
  const showRiskAlertsPanel = false
  const showRiskRecommendationsPanel = false
  const showInventoryPanel = false
  const showFulfillmentPanel = false
  const showOrdersPanel = false
  const showRuntimePanel = isRuntimePage
  const showIncidentPanel = isRuntimePage
  const showBusinessEventsPanel = isAuditPage
  const showAuditPanel = isAuditPage
  const showIntegrationConnectorsPanel = false
  const showIntegrationImportsPanel = false
  const showReplayQueuePanel = false
  const showAccessAdminPanel = false
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
      const payload = await fetchJson('/api/access/admin/workspace', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tenantName: workspaceSettingsForm.tenantName.trim(),
          description: workspaceSettingsForm.description.trim(),
        }),
      })

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
      const payload = await fetchJson('/api/access/admin/workspace/security', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          passwordRotationDays: Number.parseInt(workspaceSecurityForm.passwordRotationDays, 10),
          sessionTimeoutMinutes: Number.parseInt(workspaceSecurityForm.sessionTimeoutMinutes, 10),
          invalidateOtherSessions: workspaceSecurityForm.invalidateOtherSessions,
        }),
      })

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
      const payload = await fetchJson(`/api/access/admin/workspace/warehouses/${warehouseId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: draft.name.trim(),
          location: draft.location.trim(),
        }),
      })

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
      const payload = await fetchJson(`/api/access/admin/workspace/connectors/${connectorId}`, {
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
      const payload = await fetchJson('/api/scenarios/order-impact', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ warehouseCode: scenarioForm.warehouseCode, items: primaryContext.requestItems }) })
      setScenarioState({ loading: false, error: '', result: payload })
      await Promise.all([refreshSnapshotQuietly(), refreshScenarioHistoryQuietly()])
    } catch (error) {
      setScenarioState({ loading: false, error: error.message, result: null })
    }
  }

  async function compareScenarios() {
    setComparisonState({ loading: true, error: '', result: null })
    try {
      const payload = await fetchJson('/api/scenarios/order-impact/compare', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          primaryLabel: 'Scenario A',
          primary: { warehouseCode: scenarioForm.warehouseCode, items: primaryContext.requestItems },
          alternativeLabel: 'Scenario B',
          alternative: { warehouseCode: comparisonForm.warehouseCode, items: alternativeContext.requestItems },
        }),
      })
      setComparisonState({ loading: false, error: '', result: payload })
      await Promise.all([refreshSnapshotQuietly(), refreshScenarioHistoryQuietly()])
    } catch (error) {
      setComparisonState({ loading: false, error: error.message, result: null })
    }
  }

  async function executeScenario(scenarioId) {
    setScenarioExecutionState({ loadingId: scenarioId, error: '', success: '' })
    try {
      const payload = await fetchJson(`/api/scenarios/${scenarioId}/execute`, { method: 'POST' })
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
      const payload = await fetchJson(`/api/scenarios/${scenarioId}/request`)
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
      const payload = await fetchJson('/api/scenarios/save', {
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
      const payload = await fetchJson(`/api/scenarios/${scenarioId}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ actorRole: scenarioActorRole, approverName: signedInActorName, approvalNote: scenarioReviewNote.trim() || null }),
      })
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
      const payload = await fetchJson(`/api/scenarios/${scenarioId}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ actorRole: scenarioActorRole, reviewerName: signedInActorName, reason: scenarioReviewNote.trim() }),
      })
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
      const payload = await fetchJson(`/api/scenarios/${scenarioId}/acknowledge-escalation`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ actorRole: scenarioActorRole, acknowledgedBy: signedInActorName, note: scenarioReviewNote.trim() }),
      })
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

  function handleSignInSubmit(event) {
    event.preventDefault()
    const signInBusy = authSessionState.loading && authSessionState.action === 'signin'
    if (!signInBusy && authSessionState.tenantCode.trim() && authSessionState.username.trim() && authSessionState.password.trim()) {
      signInOperator()
    }
  }

  async function signInOperator() {
    setAuthSessionState((current) => ({ ...current, loading: true, error: '', action: 'signin' }))
    setPasswordChangeState((current) => ({ ...current, error: '', success: '' }))
    try {
      const payload = await fetchJson('/api/auth/session/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tenantCode: authSessionState.tenantCode.trim(),
          username: authSessionState.username.trim(),
          password: authSessionState.password,
        }),
      }, { ignoreUnauthorized: true })

      if (rememberWorkspace) {
        writeStoredJson(globalThis.localStorage, workspacePreferenceStorageKey, {
          remember: true,
          tenantCode: payload.tenantCode || authSessionState.tenantCode.trim(),
          username: payload.username || authSessionState.username.trim(),
        })
      } else {
        removeStoredValue(globalThis.localStorage, workspacePreferenceStorageKey)
      }

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
      const nextPage = readPendingPostAuthPage() || 'dashboard'
      clearPendingPostAuthPage()
      navigateToPage(nextPage)
    } catch (error) {
      setAuthSessionState((current) => ({ ...current, loading: false, error: error.message, action: '' }))
    }
  }

  async function signOutOperator() {
    setAuthSessionState((current) => ({ ...current, loading: true, error: '', action: 'signout' }))
    setPasswordChangeState((current) => ({ ...current, loading: false, error: '', success: '', form: createDefaultPasswordChangeForm() }))
    try {
      const payload = await fetchJson('/api/auth/session/logout', { method: 'POST' })
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
      resetSignedInWorkspace()
      clearPendingPostAuthPage()
      navigateToPage('sign-in')
    } catch (error) {
      setAuthSessionState((current) => ({ ...current, loading: false, error: error.message, action: '' }))
    }
  }

  async function changeSignedInPassword() {
    setPasswordChangeState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const payload = await fetchJson('/api/auth/session/password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          currentPassword: passwordChangeState.form.currentPassword,
          newPassword: passwordChangeState.form.newPassword,
        }),
      })

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
      const payload = await fetchJson('/api/access/tenants', {
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

      const tenants = await fetchJson('/api/access/tenants')
      setTenantDirectoryState({ loading: false, error: '', items: tenants })
      setTenantOnboardingState({
        loading: false,
        error: '',
        success: `${payload.tenantName} is ready. Bootstrap admin ${payload.adminUsername} and executive approver ${payload.executiveUsername} were created. Reset the executive password from Users before first use.`,
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
      const payload = await fetchJson(editing ? `/api/access/admin/operators/${accessOperatorForm.id}` : '/api/access/admin/operators', {
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
      const payload = await fetchJson(editing ? `/api/access/admin/users/${accessUserForm.id}` : '/api/access/admin/users', {
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
      const payload = await fetchJson(`/api/access/admin/users/${accessUserForm.id}/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          password: accessUserForm.password,
        }),
      })

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

  const scenarioDecisionContext = {
    getScenarioApprovalRole,
    getScenarioRejectionRole,
    scenarioApprovalState,
    scenarioRejectionState,
    scenarioExecutionState,
    scenarioEscalationAckState,
    scenarioLoadState,
    signedInSession,
    scenarioReviewNote,
    scenarioActorRole,
    setScenarioActorRole,
    scenarioActorRoles,
    signedInRoles,
    signedInWarehouseScopes,
    hasWarehouseScope,
    formatCodeLabel,
    formatTimestamp,
    approveScenarioPlan,
    rejectScenarioPlan,
    executeScenario,
    acknowledgeScenarioEscalation,
    loadScenarioIntoPlanner,
    setScenarioReviewNote,
  }

  const scenarioPlannerContext = {
    isAuthenticated,
    isScenariosPage,
    comparisonState,
    scenarioState,
    scenarioExecutionState,
    scenarioLoadState,
    scenarioSaveState,
    scenarioApprovalState,
    scenarioRejectionState,
    scenarioRevisionSource,
    setScenarioRevisionSource,
    setScenarioHistoryFilters,
    defaultScenarioHistoryFilters,
    scenarioRequestedBy,
    scenarioReviewOwner,
    signedInSession,
    scenarioPlanName,
    setScenarioPlanName,
    requesterOperators,
    setScenarioRequestedBy,
    scenarioActorRole,
    setScenarioActorRole,
    scenarioActorRoles,
    reviewOwnerOperators,
    setScenarioReviewOwner,
    scenarioReviewNote,
    setScenarioReviewNote,
    operatorDirectoryState,
    saveScenarioPlan,
    analyzeScenario,
    compareScenarios,
    primaryContext,
    alternativeContext,
    scenarioForm,
    setScenarioForm,
    comparisonForm,
    setComparisonForm,
    hasWarehouseScope,
    signedInWarehouseScopes,
    currency,
    comparisonPrimarySummary,
    comparisonAlternativeSummary,
    formatRelativeHours,
    formatCodeLabel,
    warehouseOptions,
    addScenarioLine,
    updateScenarioField,
    updateScenarioLine,
    removeScenarioLine,
  }

  if (isPublicPage && effectivePageMeta.key === 'sign-in') {
    return (
      <SignInPage
        context={{
          effectivePageMeta,
          navigateToPage,
          publicPages,
          tenantDirectoryState,
          authSessionState,
          setAuthSessionState,
          rememberWorkspace,
          setRememberWorkspace,
          handleSignInSubmit,
          selectedTenantOption,
          wsUrl,
          signInWorkspaceHint,
          signInConfigHint,
        }}
      />
    )
  }

  if (isPublicPage) {
    return <PublicExperience context={{ effectivePageMeta, navigateToPage, publicPages }} />
  }

  return (
    <AppShell
      currentPage={currentPage}
      pageGroup={effectivePageMeta.group || 'workspace'}
      sidebar={(
        <Sidebar
          signedInSession={signedInSession}
          navigateToPage={navigateToPage}
          navGroups={navGroups}
          pageLookup={pageLookup}
          currentPage={currentPage}
          pageBadgeMap={pageBadgeMap}
          connectionState={connectionState}
          formatCodeLabel={formatCodeLabel}
        />
      )}
      topbar={(
        <Topbar
          effectivePageMeta={effectivePageMeta}
          workspaceSearch={workspaceSearch}
          setWorkspaceSearch={setWorkspaceSearch}
          searchInputRef={searchInputRef}
          firstWorkspaceSearchResult={firstWorkspaceSearchResult}
          navigateToPage={navigateToPage}
          hasWorkspaceSearch={hasWorkspaceSearch}
          workspaceSearchMatchCount={workspaceSearchMatchCount}
          workspaceSearchSections={workspaceSearchSections}
          pageSectionActions={pageSectionActions}
          jumpToPageSection={jumpToPageSection}
          liveClockLabel={liveClockLabel}
          connectionState={connectionState}
          formatCodeLabel={formatCodeLabel}
          globalNotificationCount={globalNotificationCount}
          topbarQuickActions={topbarQuickActions}
          pageState={{ ...pageState, onRefresh: fetchSnapshot }}
          actionState={actionState}
          systemRuntimeState={{ ...systemRuntimeState, onRefresh: fetchSystemRuntime }}
          showOperationalTools={showOperationalTools}
          snapshot={snapshot}
          toggleSimulation={toggleSimulation}
          signedInSession={signedInSession}
          signOutOperator={signOutOperator}
          authSessionState={authSessionState}
        />
      )}
      utilityRail={(
        <WorkspaceUtilityRail
          context={{
            isAuthenticated,
            recommendationNow,
            recommendationSoon,
            recommendationWatch,
            activeAlerts,
            selectedAlertId,
            selectedRecommendationId,
            selectedOrderId,
            selectedInventoryId,
            currentPage,
            snapshot,
            formatCodeLabel,
            currency,
            formatRelativeHours,
            lowStockInventory,
            highRiskInventory,
            fastMovingInventory,
            warehouseOptions,
            fulfillmentOverview,
            selectedRuntimeIncident,
            runtime,
            formatTimestamp,
            formatMetricValue,
            systemIncidents,
            pendingReplayCount,
            selectedAuditTrace,
            accessAdminOperators,
            accessAdminUsers,
            workspaceAdmin,
            selectedAccessSubject,
            selectedTenantPortfolio,
            tenantDirectoryState,
            frontendBuildVersion,
            apiUrl,
            wsUrl,
            realtimeTransportLabel,
            effectivePageMeta,
            pageStatusMap,
            navigateToPage,
            connectionState,
            urgentActions,
            utilityTimeline,
            passwordChangeRequired,
            passwordRotationRequired,
            signedInSession,
            signedInRoles,
            signedInWarehouseScopes,
            enabledConnectorCount,
            scenarioHistoryItems,
            selectedHistoryScenario,
            pendingApprovalScenarios,
            overdueScenarios,
            approvedScenarios,
            rejectedScenarios,
            selectedApprovalScenario,
            escalatedScenarios,
            selectedEscalationScenario,
          }}
        />
      )}
    >
            <WorkspacePageHeader
              showDashboardHero={showDashboardHero}
              controlHighlights={controlHighlights}
              pendingReviewCount={pendingReviewCount}
              pendingReplayCount={pendingReplayCount}
              systemIncidents={systemIncidents}
              pageStatusMap={pageStatusMap}
              appPageCount={appPageCount}
              enabledConnectorCount={enabledConnectorCount}
              snapshot={snapshot}
              effectivePageMeta={effectivePageMeta}
              currentPage={currentPage}
            />
            <WorkspaceNotices pageError={pageState.error} actionError={actionState.error} authError={authSessionState.error} />
            <DashboardPage context={{ isAuthenticated, isDashboardPage, warehouseOptions, snapshot, fulfillmentOverview, activeAlerts, urgentActions, navigateToPage, setSelectedAlertId, setSelectedRecommendationId, setSelectedScenarioId, pendingApprovalScenarios, runtime, systemIncidents, utilityTimeline, formatCodeLabel, formatTimestamp, formatBuildValue, getRuntimeStatusClassName, enabledConnectorCount, pendingReplayCount, pageLoading: pageState.loading }} />
            <AlertsPage context={{ isAuthenticated, isAlertsPage, activeAlerts, selectedAlertId, setSelectedAlertId, formatTimestamp }} />
            <RecommendationsPage context={{ isAuthenticated, isRecommendationsPage, snapshot, recommendationNow, recommendationSoon, recommendationWatch, selectedRecommendationId, setSelectedRecommendationId, formatTimestamp }} />
            <OrdersPage context={{ isAuthenticated, isOrdersPage, snapshot, fulfillmentOverview, selectedOrderId, setSelectedOrderId, summary, warehouseOptions, currency, formatCodeLabel, formatRelativeHours, formatTimestamp }} />
            <InventoryPage context={{ isAuthenticated, isInventoryPage, snapshot, selectedInventoryId, setSelectedInventoryId, lowStockInventory, highRiskInventory, fastMovingInventory, warehouseOptions, formatCodeLabel, formatRelativeHours }} />
            <LocationsPage context={{ isAuthenticated, isLocationsPage, warehouseOptions, snapshot, fulfillmentOverview, activeAlerts, formatCodeLabel }} />
            <FulfillmentPage context={{ isAuthenticated, isFulfillmentPage, delayedFulfillments, fulfillmentOverview, warehouseOptions, formatCodeLabel, formatRelativeHours, getFulfillmentStatusClassName, enabledConnectorCount, snapshot, pendingReplayCount }} />
            <ScenarioControlPage context={{ isAuthenticated, isScenariosPage, isScenarioHistoryPage, isApprovalsPage, isEscalationsPage, scenarioHistoryItems, pendingApprovalScenarios, approvedScenarios, rejectedScenarios, overdueScenarios, approvalBoard, formatCodeLabel }} />
            <ScenarioPlannerPage context={scenarioPlannerContext} />
            <ScenarioHistoryPage context={{ isAuthenticated, isScenarioHistoryPage, scenarioHistoryItems, selectedHistoryScenario, setSelectedScenarioId, formatCodeLabel, formatTimestamp, scenarioDecisionContext }} />
            <ApprovalsPage context={{ isAuthenticated, isApprovalsPage, pendingApprovalScenarios, approvedScenarios, rejectedScenarios, overdueScenarios, selectedApprovalScenario, setSelectedScenarioId, formatCodeLabel, formatTimestamp, snapshot, scenarioDecisionContext }} />
            <EscalationsPage context={{ isAuthenticated, isEscalationsPage, snapshot, systemIncidents, escalatedScenarios, selectedEscalationScenario, setSelectedScenarioId, formatCodeLabel, formatTimestamp, getIncidentStatusClassName, scenarioDecisionContext }} />
            <IntegrationsPage context={{ isAuthenticated, isIntegrationsPage, snapshot, selectedIntegrationConnectorId, setSelectedIntegrationConnectorId, enabledConnectorCount, pendingReplayCount, systemIncidents, navigateToPage, formatCodeLabel, formatTimestamp, getImportStatusClassName }} />
            <ReplayPage context={{ isAuthenticated, isReplayPage, snapshot, selectedReplayRecordId, setSelectedReplayRecordId, pendingReplayCount, integrationReplayState, replayFailedIntegration, signedInSession, signedInRoles, signedInWarehouseScopes, hasWarehouseScope, navigateToPage, formatCodeLabel, formatTimestamp, getReplayStatusClassName }} />
            <RuntimePage context={{ isAuthenticated, isRuntimePage, runtime, systemIncidents, selectedRuntimeIncidentKey, setSelectedRuntimeIncidentKey, navigateToPage, formatCodeLabel, formatMetricValue, formatTimestamp, getIncidentStatusClassName, getRuntimeStatusClassName }} />
            <AuditPage context={{ isAuthenticated, isAuditPage, snapshot, systemIncidents, pendingReplayCount, recentBusinessEvents, recentAuditEntries, selectedAuditTrace, setSelectedTraceEntryKey, formatCodeLabel, formatTimestamp, navigateToPage }} />
            <UsersPage context={{ isAuthenticated, isUsersPage, accessAdminOperators, accessAdminUsers, workspaceAdmin, selectedAccessSubject, setSelectedAccessSubjectKey, formatCodeLabel, navigateToPage }} />
            <SettingsPage context={{ isAuthenticated, isSettingsPage, workspaceAdmin, accessAdminState, canManageTenantAccess, workspaceSettingsForm, setWorkspaceSettingsForm, workspaceSecurityForm, setWorkspaceSecurityForm, saveWorkspaceSettings, saveWorkspaceSecuritySettings, selectedWorkspaceWarehouse, selectedWorkspaceWarehouseDraft, selectedWorkspaceConnector, selectedWorkspaceConnectorDraft, selectedWorkspaceConnectorOwnerOptions, setSelectedWorkspaceWarehouseId, setSelectedWorkspaceConnectorId, setWorkspaceWarehouseDrafts, setWorkspaceConnectorDrafts, saveWorkspaceWarehouse, saveWorkspaceConnectorSupport, formatCodeLabel, integrationSyncModes, integrationValidationPolicies, integrationTransformationPolicies }} />
            <ProfilePage context={{ isAuthenticated, isProfilePage, passwordChangeRequired, passwordRotationRequired, activeAlerts, pendingApprovalScenarios, systemIncidents, signedInSession, signedInRoles, signedInWarehouseScopes, signedInSessionExpiresAt, signedInPasswordExpiresAt, formatCodeLabel, formatTimestamp, passwordChangeState, setPasswordChangeState, changeSignedInPassword, signOutOperator, authSessionState, navigateToPage }} />
            <PlatformAdminPage context={{ isAuthenticated, isPlatformPage, runtime, tenantDirectoryState, systemIncidents, pendingReplayCount, selectedTenantPortfolio, setSelectedTenantPortfolioCode, signedInSession, formatBuildValue, formatCodeLabel, formatTimestamp, getIncidentStatusClassName, getRuntimeStatusClassName, navigateToPage }} />
            <TenantsPage context={{ isAuthenticated, isTenantsPage, tenantDirectoryState, signedInSession, signedInRoles, tenantOnboardingState, tenantOnboardingForm, setTenantOnboardingForm, onboardTenant, signInOperator, authSessionState, setAuthSessionState }} />
            <SystemConfigPage context={{ isAuthenticated, isSystemConfigPage, runtime, formatMetricValue }} />
            <ReleasesPage context={{ isAuthenticated, isReleasesPage, runtime, formatBuildValue, formatCodeLabel, formatTimestamp, frontendBuildVersion, frontendBuildCommit, frontendBuildTime, apiUrl, wsUrl, realtimeTransportLabel, getRuntimeStatusClassName }} />
    </AppShell>
  )
}
