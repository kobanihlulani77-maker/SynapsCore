export const emptySnapshot = {
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
  generatedAt: null,
}

export const emptyRequestState = { loading: false, error: '', result: null }

export const defaultScenarioHistoryFilters = {
  type: 'ALL',
  approvalStatus: 'ALL',
  approvalPolicy: 'ALL',
  approvalStage: 'ALL',
  warehouseCode: 'ALL',
  requestedBy: '',
  reviewOwner: '',
  finalApprovalOwner: '',
  minimumReviewPriority: 'ALL',
  overdueOnly: false,
  slaEscalatedOnly: false,
}

export const currency = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
})

export const defaultScenarioRequester = 'Operations Planner'
export const defaultScenarioReviewOwner = 'Operations Lead'

export const defaultTenantOnboardingForm = {
  tenantCode: '',
  tenantName: '',
  description: '',
  adminFullName: '',
  adminUsername: '',
  adminPassword: '',
  primaryLocation: '',
  secondaryLocation: '',
}

export const createDefaultWorkspaceSettingsForm = () => ({ tenantName: '', description: '' })
export const createDefaultWorkspaceSecurityForm = () => ({ passwordRotationDays: '90', sessionTimeoutMinutes: '480', invalidateOtherSessions: false })
export const createDefaultAccessOperatorForm = () => ({ id: null, actorName: '', displayName: '', description: '', rolesText: 'REVIEW_OWNER', warehouseScopesText: '', active: true })
export const createDefaultAccessUserForm = () => ({ id: null, username: '', fullName: '', password: '', operatorActorName: '', active: true })
export const createDefaultPasswordChangeForm = () => ({ currentPassword: '', newPassword: '', confirmPassword: '' })
export const createDefaultCatalogForm = () => ({ id: null, sku: '', name: '', category: '' })

export const buildWorkspaceWarehouseDrafts = (workspace) => Object.fromEntries(
  (workspace?.warehouses || []).map((warehouse) => [
    warehouse.id,
    { name: warehouse.name, location: warehouse.location },
  ]),
)

export const buildWorkspaceConnectorDrafts = (workspace) => Object.fromEntries(
  (workspace?.connectors || []).map((connector) => [
    connector.id,
    {
      supportOwnerActorName: connector.supportOwnerActorName || '',
      syncMode: connector.syncMode || 'REALTIME_PUSH',
      syncIntervalMinutes: connector.syncIntervalMinutes == null ? '' : String(connector.syncIntervalMinutes),
      pullEndpointUrl: connector.pullEndpointUrl || '',
      validationPolicy: connector.validationPolicy || 'STANDARD',
      transformationPolicy: connector.transformationPolicy || 'NONE',
      allowDefaultWarehouseFallback: Boolean(connector.allowDefaultWarehouseFallback),
      notes: connector.notes || '',
    },
  ]),
)

export const buildAccessOperatorForm = (operator) => ({
  ...createDefaultAccessOperatorForm(),
  id: operator.id,
  actorName: operator.actorName,
  displayName: operator.displayName,
  description: operator.description || '',
  rolesText: operator.roles.join(','),
  warehouseScopesText: operator.warehouseScopes.join(','),
  active: operator.active,
})

export const buildAccessUserForm = (user) => ({
  ...createDefaultAccessUserForm(),
  id: user.id,
  username: user.username,
  fullName: user.fullName,
  operatorActorName: user.operatorActorName,
  active: user.active,
})

export const createScenarioLine = (productSku = '') => ({
  id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
  productSku,
  quantity: '1',
  unitPrice: '95.00',
})

export const createScenarioForm = (productSku = '') => ({
  warehouseCode: '',
  items: [createScenarioLine(productSku)],
})

export const buildWarehouseOptions = (inventory) => [
  ...new Map(inventory.map((item) => [item.warehouseCode, { code: item.warehouseCode, name: item.warehouseName }])).values(),
]

export const buildProductOptions = (inventory, warehouseCode) => [
  ...new Map(
    inventory
      .filter((item) => item.warehouseCode === warehouseCode)
      .map((item) => [item.productSku, {
        sku: item.productSku,
        name: item.productName,
        quantityAvailable: item.quantityAvailable,
        reorderThreshold: item.reorderThreshold,
      }]),
  ).values(),
]

export const formatRelativeHours = (hours) => {
  if (hours == null) return 'Monitoring'
  if (hours < 0) {
    const overdueHours = Math.abs(hours)
    return overdueHours < 1 ? `${Math.max(overdueHours * 60, 1).toFixed(0)} min overdue` : `${overdueHours.toFixed(1)} hrs overdue`
  }
  return hours < 1 ? `${Math.max(hours * 60, 1).toFixed(0)} min` : `${hours.toFixed(1)} hrs`
}

export const formatTimestamp = (value) => (value ? new Date(value).toLocaleString() : 'Monitoring')
export const formatMetricValue = (value) => (Number.isFinite(value) ? Math.round(value) : 0)
export const formatBuildValue = (value) => value || 'untracked'
export const formatCodeLabel = (value) => !value
  ? 'Unknown'
  : value
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ')

export const parseCsvValues = (value) => value.split(',').map((part) => part.trim()).filter(Boolean)
export const buildTenantTopicPrefix = (tenantCode) => (tenantCode ? `/topic/tenant/${tenantCode.trim().toUpperCase()}` : '')
export const getImportStatusClassName = (status) => (status === 'SUCCESS' ? 'status-success' : status === 'PARTIAL_SUCCESS' ? 'status-partial' : 'status-failure')
export const getReplayStatusClassName = (status) => (status === 'PENDING' ? 'status-partial' : status === 'REPLAY_FAILED' ? 'status-failure' : 'status-success')
export const getRuntimeStatusClassName = (status) => (['UP', 'CORRECT', 'ACCEPTING_TRAFFIC'].includes(status) ? 'status-success' : status === 'UNKNOWN' ? 'status-partial' : 'status-failure')
export const getIncidentStatusClassName = (severity) => (severity === 'CRITICAL' || severity === 'HIGH' ? 'status-failure' : 'status-partial')
export const getFulfillmentStatusClassName = (status) => (status === 'DELIVERED' || status === 'DISPATCHED' ? 'status-success' : status === 'DELAYED' || status === 'EXCEPTION' ? 'status-failure' : 'status-partial')

export const scenarioActorRoles = ['REVIEW_OWNER', 'FINAL_APPROVER', 'ESCALATION_OWNER']
export const integrationActorRoles = ['INTEGRATION_ADMIN', 'INTEGRATION_OPERATOR']
export const integrationValidationPolicies = ['STANDARD', 'STRICT', 'RELAXED']
export const integrationTransformationPolicies = ['NONE', 'NORMALIZE_CODES']

export const getScenarioApprovalRole = (scenario) => (
  scenario.approvalPolicy === 'ESCALATED' && scenario.approvalStage === 'PENDING_FINAL_APPROVAL'
    ? 'FINAL_APPROVER'
    : 'REVIEW_OWNER'
)

export const getScenarioRejectionRole = (scenario) => (
  scenario.approvalStage === 'PENDING_FINAL_APPROVAL' ? 'FINAL_APPROVER' : 'REVIEW_OWNER'
)

export const summarizeImpact = (impact) => ({
  lowStockItems: impact.projectedInventory.filter((item) => item.lowStock).length,
  criticalItems: impact.projectedInventory.filter((item) => item.riskLevel === 'critical').length,
  highRiskItems: impact.projectedInventory.filter((item) => item.riskLevel === 'high').length,
  alertCount: impact.projectedAlerts.length,
  recommendationCount: impact.projectedRecommendations.length,
})

export const buildRevisionTitle = (title, revisionNumber) => `${title} Rev ${revisionNumber}`

export const hasActiveScenarioHistoryFilters = (filters) => (
  filters.type !== 'ALL'
  || filters.approvalStatus !== 'ALL'
  || filters.approvalPolicy !== 'ALL'
  || filters.approvalStage !== 'ALL'
  || filters.warehouseCode !== 'ALL'
  || Boolean(filters.requestedBy.trim())
  || Boolean(filters.reviewOwner.trim())
  || Boolean(filters.finalApprovalOwner.trim())
  || filters.minimumReviewPriority !== 'ALL'
  || filters.overdueOnly
  || filters.slaEscalatedOnly
)

export const resolvePreferredOperatorName = (operators, preferredName) => (
  operators.find((operator) => operator.actorName === preferredName)?.actorName
  || operators[0]?.actorName
  || ''
)

export const hasWarehouseScope = (warehouseScopes, warehouseCode) => (
  !warehouseCode
  || !(warehouseScopes?.length)
  || warehouseScopes.some((scope) => scope === warehouseCode.toUpperCase())
)

export const buildAccessOperatorsPath = (tenantCode) => (
  tenantCode ? `/api/access/operators?tenantCode=${encodeURIComponent(tenantCode)}` : '/api/access/operators'
)

export const buildScenarioHistoryPath = (filters) => {
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
