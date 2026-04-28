import { useDeferredValue } from 'react'
import { appPages, pageSectionMap } from '../config/pageRegistry'

const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })

export default function useWorkspaceChrome({
  currentPage,
  isAuthenticated,
  summary,
  snapshot,
  catalogState,
  warehouseOptions,
  fulfillmentOverview,
  pendingReviewCount,
  scenarioHistoryItems,
  systemIncidents,
  runtime,
  tenantDirectoryState,
  authSessionState,
  apiUrl,
  wsUrl,
  realtimeTransportLabel,
  signedInSession,
  activeDecisionCount,
  enabledConnectorCount,
  pendingReplayCount,
  canManageTenantAccess,
  accessAdminUsers,
  passwordChangeRequired,
  passwordRotationRequired,
  clockTick,
  workspaceSearch,
  selectedScenarioId,
  formatTimestamp,
  formatBuildValue,
  formatCodeLabel,
}) {
  const pageBadgeMap = {
    dashboard: summary?.totalOrders ?? 0,
    alerts: summary?.activeAlerts ?? snapshot.alerts.activeAlerts.length,
    recommendations: summary?.recommendationsCount ?? snapshot.recommendations.length,
    orders: summary?.recentOrderCount ?? snapshot.recentOrders.length,
    inventory: summary?.inventoryRecordsCount ?? snapshot.inventory.length,
    catalog: catalogState.products.length,
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
    settings: canManageTenantAccess ? (summary?.totalWarehouses ?? warehouseOptions.length) : 0,
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
    catalog: isAuthenticated ? `${catalogState.products.length} tenant product${catalogState.products.length === 1 ? '' : 's'} available` : 'Protected by workspace sign-in',
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
    { label: 'Total Orders', value: summary ? summary.totalOrders : 'Loading', accent: 'amber' },
    { label: 'Active Alerts', value: summary ? summary.activeAlerts : 'Loading', accent: 'rose' },
    { label: 'Low Stock Items', value: summary ? summary.lowStockItems : 'Loading', accent: 'orange' },
    { label: 'Recommendations', value: summary ? summary.recommendationsCount : 'Loading', accent: 'teal' },
    { label: 'Fulfillment Backlog', value: summary ? summary.fulfillmentBacklogCount : 'Loading', accent: 'slate' },
    { label: 'Delayed Shipments', value: summary ? summary.delayedShipmentCount : 'Loading', accent: 'rose' },
    { label: 'Products', value: summary ? summary.totalProducts : 'Loading', accent: 'blue' },
    { label: 'Warehouses', value: summary ? summary.totalWarehouses : 'Loading', accent: 'slate' },
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
  const approvalQueueScenarios = Array.from(new Map([...pendingApprovalScenarios, ...overdueScenarios, ...approvedScenarios, ...rejectedScenarios].map((scenario) => [scenario.id, scenario])).values())
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
          .map((page) => ({ id: `page-${page.key}`, title: page.label, meta: page.description, target: page.key })),
      },
      {
        key: 'orders',
        label: 'Orders',
        items: snapshot.recentOrders
          .filter((order) => order.externalOrderId.toLowerCase().includes(deferredWorkspaceSearch))
          .slice(0, 3)
          .map((order) => ({ id: `order-${order.id}`, title: order.externalOrderId, meta: `${order.warehouseName} | ${currency.format(order.totalAmount)}`, target: 'orders' })),
      },
      {
        key: 'alerts',
        label: 'Alerts',
        items: activeAlerts
          .filter((alert) => alert.title.toLowerCase().includes(deferredWorkspaceSearch))
          .slice(0, 3)
          .map((alert) => ({ id: `alert-search-${alert.id}`, title: alert.title, meta: alert.impactSummary, target: 'alerts' })),
      },
      {
        key: 'incidents',
        label: 'Incidents',
        items: systemIncidents
          .filter((incident) => incident.title.toLowerCase().includes(deferredWorkspaceSearch) || incident.detail.toLowerCase().includes(deferredWorkspaceSearch))
          .slice(0, 3)
          .map((incident) => ({ id: `incident-search-${incident.incidentKey}`, title: incident.title, meta: `${formatCodeLabel(incident.severity)} | ${incident.context}`, target: 'runtime' })),
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
        return [{ label: 'Open alerts', target: 'alerts' }, { label: 'Open approvals', target: 'approvals' }]
      case 'alerts':
        return [{ label: 'Open recommendations', target: 'recommendations' }, { label: 'Open runtime', target: 'runtime' }]
      case 'recommendations':
        return [{ label: 'Open alerts', target: 'alerts' }, { label: 'Open orders', target: 'orders' }]
      case 'orders':
        return [{ label: 'Open fulfillment', target: 'fulfillment' }, { label: 'Open inventory', target: 'inventory' }]
      case 'inventory':
        return [{ label: 'Open locations', target: 'locations' }, { label: 'Open recommendations', target: 'recommendations' }]
      case 'scenario-history':
        return [{ label: 'Open scenarios', target: 'scenarios' }, { label: 'Open approvals', target: 'approvals' }]
      case 'approvals':
        return [{ label: 'Open escalations', target: 'escalations' }, { label: 'Open scenario history', target: 'scenario-history' }]
      case 'runtime':
        return [{ label: 'Open audit', target: 'audit' }, { label: 'Open releases', target: 'releases' }]
      case 'audit':
        return [{ label: 'Open replay', target: 'replay' }, { label: 'Open runtime', target: 'runtime' }]
      case 'users':
        return [{ label: 'Open settings', target: 'settings' }, { label: 'Open profile', target: 'profile' }]
      case 'settings':
        return [{ label: 'Open users', target: 'users' }, { label: 'Open platform', target: 'platform' }]
      case 'platform':
        return [{ label: 'Open releases', target: 'releases' }, { label: 'Open tenants', target: 'tenants' }]
      case 'releases':
        return [{ label: 'Open runtime', target: 'runtime' }, { label: 'Open platform', target: 'platform' }]
      default:
        return [{ label: 'Open alerts', target: 'alerts' }, { label: 'Open approvals', target: 'approvals' }]
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

  return {
    selectedTenantOption,
    pageBadgeMap,
    pageStatusMap,
    signInWorkspaceHint,
    signInConfigHint,
    metrics,
    controlHighlights,
    urgentActions: [
      ...activeAlerts.slice(0, 3).map((alert) => ({
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
    ].slice(0, 5),
    activeAlerts,
    lowStockInventory,
    highRiskInventory,
    fastMovingInventory,
    delayedFulfillments,
    pendingApprovalScenarios,
    approvedScenarios,
    rejectedScenarios,
    overdueScenarios,
    escalatedScenarios,
    approvalBoard,
    approvalQueueScenarios,
    selectedHistoryScenario,
    selectedApprovalScenario,
    selectedEscalationScenario,
    recommendationNow,
    recommendationSoon,
    recommendationWatch,
    globalNotificationCount,
    liveClockLabel,
    workspaceSearchSections,
    workspaceSearchMatchCount,
    firstWorkspaceSearchResult,
    pageSectionActions,
    topbarQuickActions,
    utilityTimeline,
    showDashboardHero: currentPage === 'dashboard',
    showSummaryMetrics: currentPage === 'dashboard',
    showScenarioPlanner: currentPage === 'scenarios',
    showScenarioHistory: currentPage === 'scenarios' || currentPage === 'scenario-history',
    showScenarioNotifications: currentPage === 'scenarios',
    showEscalationInbox: currentPage === 'scenarios',
    showRiskAlertsPanel: false,
    showRiskRecommendationsPanel: false,
    showInventoryPanel: false,
    showFulfillmentPanel: false,
    showOrdersPanel: false,
    showRuntimePanel: currentPage === 'runtime',
    showIncidentPanel: currentPage === 'runtime',
    showBusinessEventsPanel: currentPage === 'audit',
    showAuditPanel: currentPage === 'audit',
    showIntegrationConnectorsPanel: false,
    showIntegrationImportsPanel: false,
    showReplayQueuePanel: false,
    showAccessAdminPanel: false,
    showTenantManagementPanel: currentPage === 'tenants',
    showLocationsExperience: currentPage === 'locations',
    appPageCount: appPages.length,
  }
}
