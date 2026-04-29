import useWorkspaceChrome from './useWorkspaceChrome'
import { navGroups, pageLookup, publicPages } from '../config/pageRegistry'
import {
  buildWarehouseOptions,
  currency,
  defaultScenarioHistoryFilters,
  emptySnapshot,
  formatBuildValue,
  formatCodeLabel,
  formatMetricValue,
  formatRelativeHours,
  formatTimestamp,
  getFulfillmentStatusClassName,
  getImportStatusClassName,
  getIncidentStatusClassName,
  getReplayStatusClassName,
  getRuntimeStatusClassName,
  getScenarioApprovalRole,
  getScenarioRejectionRole,
  hasWarehouseScope,
  integrationTransformationPolicies,
  integrationValidationPolicies,
  scenarioActorRoles,
  summarizeImpact,
} from '../config/workspaceModel'

export default function useWorkspacePageContexts({
  workspaceState,
  authContext,
  apiContext,
  navigation,
  bootstrapContext,
  sessionActions,
  catalogActions,
  integrationActions,
  workspaceAdminActions,
  scenarioActions,
}) {
  const {
    snapshot,
    currentPage,
    workspaceSearch,
    setWorkspaceSearch,
    clockTick,
    selectedAlertId,
    setSelectedAlertId,
    selectedRecommendationId,
    setSelectedRecommendationId,
    selectedOrderId,
    setSelectedOrderId,
    selectedInventoryId,
    setSelectedInventoryId,
    selectedCatalogProductId,
    setSelectedCatalogProductId,
    selectedScenarioId,
    setSelectedScenarioId,
    selectedRuntimeIncidentKey,
    setSelectedRuntimeIncidentKey,
    selectedTraceEntryKey,
    setSelectedTraceEntryKey,
    selectedAccessSubjectKey,
    setSelectedAccessSubjectKey,
    selectedTenantPortfolioCode,
    setSelectedTenantPortfolioCode,
    selectedWorkspaceWarehouseId,
    setSelectedWorkspaceWarehouseId,
    selectedWorkspaceConnectorId,
    setSelectedWorkspaceConnectorId,
    selectedIntegrationConnectorId,
    setSelectedIntegrationConnectorId,
    selectedReplayRecordId,
    setSelectedReplayRecordId,
    connectionState,
    pageState,
    actionState,
    systemRuntimeState,
    tenantDirectoryState,
    tenantOnboardingForm,
    setTenantOnboardingForm,
    tenantOnboardingState,
    accessAdminState,
    workspaceSettingsForm,
    setWorkspaceSettingsForm,
    workspaceSecurityForm,
    setWorkspaceSecurityForm,
    workspaceWarehouseDrafts,
    setWorkspaceWarehouseDrafts,
    workspaceConnectorDrafts,
    setWorkspaceConnectorDrafts,
    catalogState,
    catalogForm,
    setCatalogForm,
    operatorDirectoryState,
    integrationReplayState,
    scenarioForm,
    setScenarioForm,
    comparisonForm,
    setComparisonForm,
    scenarioPlanName,
    setScenarioPlanName,
    scenarioRequestedBy,
    setScenarioRequestedBy,
    scenarioReviewOwner,
    setScenarioReviewOwner,
    scenarioActorRole,
    setScenarioActorRole,
    scenarioReviewNote,
    setScenarioReviewNote,
    scenarioRevisionSource,
    setScenarioRevisionSource,
    scenarioState,
    comparisonState,
    scenarioExecutionState,
    scenarioLoadState,
    scenarioSaveState,
    scenarioApprovalState,
    scenarioRejectionState,
    scenarioEscalationAckState,
    scenarioHistoryFilters,
    setScenarioHistoryFilters,
    scenarioHistoryState,
    searchInputRef,
    buildScenarioContext,
    updateScenarioField,
    updateScenarioLine,
    addScenarioLine,
    removeScenarioLine,
  } = workspaceState

  const {
    rememberWorkspace,
    setRememberWorkspace,
    authSessionState,
    setAuthSessionState,
    passwordChangeState,
    setPasswordChangeState,
  } = authContext

  const {
    apiUrl,
    wsUrl,
    realtimeTransportLabel,
    frontendBuildVersion,
    frontendBuildCommit,
    frontendBuildTime,
  } = apiContext

  const {
    navigateToPage,
    jumpToPageSection,
  } = navigation

  const {
    fetchSnapshot,
    fetchSystemRuntime,
  } = bootstrapContext

  const {
    handleSignInSubmit,
    signInOperator,
    signOutOperator,
    changeSignedInPassword,
    onboardTenant,
  } = sessionActions

  const {
    resetCatalogForm,
    saveCatalogProduct,
    importCatalogProducts,
  } = catalogActions

  const {
    replayFailedIntegration,
  } = integrationActions

  const {
    saveWorkspaceSettings,
    saveWorkspaceSecuritySettings,
    saveWorkspaceWarehouse,
    saveWorkspaceConnectorSupport,
  } = workspaceAdminActions

  const {
    analyzeScenario,
    compareScenarios,
    executeScenario,
    loadScenarioIntoPlanner,
    saveScenarioPlan,
    approveScenarioPlan,
    rejectScenarioPlan,
    acknowledgeScenarioEscalation,
  } = scenarioActions

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
  const primaryContext = buildScenarioContext(scenarioForm)
  const alternativeContext = buildScenarioContext(comparisonForm)
  const scenarioHistoryItems = scenarioHistoryState.items
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
  const selectedWorkspaceWarehouseDraft = selectedWorkspaceWarehouse
    ? (workspaceWarehouseDrafts[selectedWorkspaceWarehouse.id] || {
        name: selectedWorkspaceWarehouse.name,
        location: selectedWorkspaceWarehouse.location,
      })
    : null
  const selectedWorkspaceConnector = workspaceAdmin?.connectors?.find((connector) => connector.id === selectedWorkspaceConnectorId) || workspaceAdmin?.connectors?.[0]
  const selectedWorkspaceConnectorDraft = selectedWorkspaceConnector
    ? (workspaceConnectorDrafts[selectedWorkspaceConnector.id] || {
        supportOwnerActorName: selectedWorkspaceConnector.supportOwnerActorName || '',
        syncMode: selectedWorkspaceConnector.syncMode || 'REALTIME_PUSH',
        syncIntervalMinutes: selectedWorkspaceConnector.syncIntervalMinutes == null ? '' : String(selectedWorkspaceConnector.syncIntervalMinutes),
        pullEndpointUrl: selectedWorkspaceConnector.pullEndpointUrl || '',
        validationPolicy: selectedWorkspaceConnector.validationPolicy || 'STANDARD',
        transformationPolicy: selectedWorkspaceConnector.transformationPolicy || 'NONE',
        allowDefaultWarehouseFallback: Boolean(selectedWorkspaceConnector.allowDefaultWarehouseFallback),
        notes: selectedWorkspaceConnector.notes || '',
      })
    : null
  const selectedWorkspaceConnectorOwnerOptions = selectedWorkspaceConnector
    ? accessAdminOperators.filter((operator) => (
      operator.active
      && (!selectedWorkspaceConnector.defaultWarehouseCode
        || !operator.warehouseScopes.length
        || operator.warehouseScopes.includes(selectedWorkspaceConnector.defaultWarehouseCode))
    ))
    : []
  const currentPageMeta = pageLookup[currentPage] || pageLookup.landing
  const effectivePageMeta = !isAuthenticated && currentPageMeta.audience === 'app' ? pageLookup['sign-in'] : currentPageMeta
  const isPublicPage = effectivePageMeta.audience === 'public'
  const isDashboardPage = currentPage === 'dashboard'
  const isAlertsPage = currentPage === 'alerts'
  const isRecommendationsPage = currentPage === 'recommendations'
  const isOrdersPage = currentPage === 'orders'
  const isInventoryPage = currentPage === 'inventory'
  const isCatalogPage = currentPage === 'catalog'
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

  const {
    selectedTenantOption,
    pageBadgeMap,
    pageStatusMap,
    signInWorkspaceHint,
    signInConfigHint,
    controlHighlights,
    urgentActions,
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
    showDashboardHero,
    appPageCount,
  } = useWorkspaceChrome({
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
  })

  const comparisonPrimarySummary = comparisonState.result ? summarizeImpact(comparisonState.result.primary) : null
  const comparisonAlternativeSummary = comparisonState.result ? summarizeImpact(comparisonState.result.alternative) : null

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

  return {
    routeState: {
      isPublicPage,
      effectivePageMeta,
      signInPageContext: {
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
      },
      publicExperienceContext: {
        effectivePageMeta,
        navigateToPage,
        publicPages,
      },
    },
    authenticatedAppProps: {
      currentPage,
      effectivePageMeta,
      navGroups,
      pageLookup,
      pageBadgeMap,
      connectionState,
      formatCodeLabel,
      workspaceSearch,
      setWorkspaceSearch,
      searchInputRef,
      firstWorkspaceSearchResult,
      navigateToPage,
      hasWorkspaceSearch: Boolean(workspaceSearch.trim()),
      workspaceSearchMatchCount,
      workspaceSearchSections,
      pageSectionActions,
      jumpToPageSection,
      liveClockLabel,
      globalNotificationCount,
      topbarQuickActions,
      pageState,
      fetchSnapshot,
      actionState,
      systemRuntimeState,
      fetchSystemRuntime,
      signedInSession,
      signOutOperator,
      authSessionState,
      utilityRailContext: {
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
      },
      showDashboardHero,
      controlHighlights,
      pendingReviewCount,
      pendingReplayCount,
      systemIncidents,
      pageStatusMap,
      appPageCount,
      enabledConnectorCount,
      snapshot,
      dashboardContext: { isAuthenticated, isDashboardPage, warehouseOptions, snapshot, fulfillmentOverview, activeAlerts, urgentActions, navigateToPage, setSelectedAlertId, setSelectedRecommendationId, setSelectedScenarioId, pendingApprovalScenarios, runtime, systemIncidents, utilityTimeline, formatCodeLabel, formatTimestamp, formatBuildValue, getRuntimeStatusClassName, enabledConnectorCount, pendingReplayCount, pageLoading: pageState.loading },
      alertsContext: { isAuthenticated, isAlertsPage, activeAlerts, selectedAlertId, setSelectedAlertId, formatTimestamp },
      recommendationsContext: { isAuthenticated, isRecommendationsPage, snapshot, recommendationNow, recommendationSoon, recommendationWatch, selectedRecommendationId, setSelectedRecommendationId, formatTimestamp },
      ordersContext: { isAuthenticated, isOrdersPage, snapshot, fulfillmentOverview, selectedOrderId, setSelectedOrderId, summary, warehouseOptions, currency, formatCodeLabel, formatRelativeHours, formatTimestamp },
      inventoryContext: { isAuthenticated, isInventoryPage, snapshot, selectedInventoryId, setSelectedInventoryId, lowStockInventory, highRiskInventory, fastMovingInventory, warehouseOptions, formatCodeLabel, formatRelativeHours },
      catalogContext: { isAuthenticated, isCatalogPage, catalogState, catalogForm, setCatalogForm, selectedCatalogProductId, setSelectedCatalogProductId, saveCatalogProduct, importCatalogProducts, resetCatalogForm, canManageTenantAccess },
      locationsContext: { isAuthenticated, isLocationsPage, warehouseOptions, snapshot, fulfillmentOverview, activeAlerts, formatCodeLabel },
      fulfillmentContext: { isAuthenticated, isFulfillmentPage, delayedFulfillments, fulfillmentOverview, warehouseOptions, formatCodeLabel, formatRelativeHours, getFulfillmentStatusClassName, enabledConnectorCount, snapshot, pendingReplayCount },
      scenarioControlContext: { isAuthenticated, isScenariosPage, isScenarioHistoryPage, isApprovalsPage, isEscalationsPage, scenarioHistoryItems, pendingApprovalScenarios, approvedScenarios, rejectedScenarios, overdueScenarios, approvalBoard, formatCodeLabel },
      scenarioPlannerContext,
      scenarioHistoryContext: { isAuthenticated, isScenarioHistoryPage, scenarioHistoryItems, selectedHistoryScenario, setSelectedScenarioId, formatCodeLabel, formatTimestamp, scenarioDecisionContext },
      approvalsContext: { isAuthenticated, isApprovalsPage, pendingApprovalScenarios, approvedScenarios, rejectedScenarios, overdueScenarios, selectedApprovalScenario, setSelectedScenarioId, formatCodeLabel, formatTimestamp, snapshot, scenarioDecisionContext },
      escalationsContext: { isAuthenticated, isEscalationsPage, snapshot, systemIncidents, escalatedScenarios, selectedEscalationScenario, setSelectedScenarioId, formatCodeLabel, formatTimestamp, getIncidentStatusClassName, scenarioDecisionContext },
      integrationsContext: { isAuthenticated, isIntegrationsPage, snapshot, selectedIntegrationConnectorId, setSelectedIntegrationConnectorId, enabledConnectorCount, pendingReplayCount, systemIncidents, navigateToPage, formatCodeLabel, formatTimestamp, getImportStatusClassName },
      replayContext: { isAuthenticated, isReplayPage, snapshot, selectedReplayRecordId, setSelectedReplayRecordId, pendingReplayCount, integrationReplayState, replayFailedIntegration, signedInSession, signedInRoles, signedInWarehouseScopes, hasWarehouseScope, navigateToPage, formatCodeLabel, formatTimestamp, getReplayStatusClassName },
      runtimeContext: { isAuthenticated, isRuntimePage, runtime, systemIncidents, selectedRuntimeIncidentKey, setSelectedRuntimeIncidentKey, navigateToPage, formatCodeLabel, formatMetricValue, formatTimestamp, getIncidentStatusClassName, getRuntimeStatusClassName },
      auditContext: { isAuthenticated, isAuditPage, snapshot, systemIncidents, pendingReplayCount, recentBusinessEvents, recentAuditEntries, selectedAuditTrace, setSelectedTraceEntryKey, formatCodeLabel, formatTimestamp, navigateToPage },
      usersContext: { isAuthenticated, isUsersPage, accessAdminOperators, accessAdminUsers, workspaceAdmin, selectedAccessSubject, setSelectedAccessSubjectKey, formatCodeLabel, navigateToPage },
      settingsContext: { isAuthenticated, isSettingsPage, workspaceAdmin, accessAdminState, canManageTenantAccess, workspaceSettingsForm, setWorkspaceSettingsForm, workspaceSecurityForm, setWorkspaceSecurityForm, saveWorkspaceSettings, saveWorkspaceSecuritySettings, selectedWorkspaceWarehouse, selectedWorkspaceWarehouseDraft, selectedWorkspaceConnector, selectedWorkspaceConnectorDraft, selectedWorkspaceConnectorOwnerOptions, setSelectedWorkspaceWarehouseId, setSelectedWorkspaceConnectorId, setWorkspaceWarehouseDrafts, setWorkspaceConnectorDrafts, saveWorkspaceWarehouse, saveWorkspaceConnectorSupport, formatCodeLabel, integrationValidationPolicies, integrationTransformationPolicies },
      profileContext: { isAuthenticated, isProfilePage, passwordChangeRequired, passwordRotationRequired, activeAlerts, pendingApprovalScenarios, systemIncidents, signedInSession, signedInRoles, signedInWarehouseScopes, signedInSessionExpiresAt, signedInPasswordExpiresAt, formatCodeLabel, formatTimestamp, passwordChangeState, setPasswordChangeState, changeSignedInPassword, signOutOperator, authSessionState, navigateToPage },
      platformAdminContext: { isAuthenticated, isPlatformPage, runtime, tenantDirectoryState, systemIncidents, pendingReplayCount, selectedTenantPortfolio, setSelectedTenantPortfolioCode, signedInSession, formatBuildValue, formatCodeLabel, formatTimestamp, getIncidentStatusClassName, getRuntimeStatusClassName, navigateToPage },
      tenantsContext: { isAuthenticated, isTenantsPage, tenantDirectoryState, signedInSession, signedInRoles, tenantOnboardingState, tenantOnboardingForm, setTenantOnboardingForm, onboardTenant, signInOperator, authSessionState, setAuthSessionState },
      systemConfigContext: { isAuthenticated, isSystemConfigPage, runtime, formatMetricValue },
      releasesContext: { isAuthenticated, isReleasesPage, runtime, formatBuildValue, formatCodeLabel, formatTimestamp, frontendBuildVersion, frontendBuildCommit, frontendBuildTime, apiUrl, wsUrl, realtimeTransportLabel, getRuntimeStatusClassName },
    },
  }
}
