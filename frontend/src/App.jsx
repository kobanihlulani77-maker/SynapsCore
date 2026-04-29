import { startTransition, useRef, useState } from 'react'
import WorkspaceAuthenticatedApp from './components/WorkspaceAuthenticatedApp'
import SignInPage from './pages/SignIn'
import PublicExperience from './pages/PublicExperience'
import useApi from './hooks/useApi'
import useAuth from './hooks/useAuth'
import useWorkspaceBootstrap from './hooks/useWorkspaceBootstrap'
import useCatalogActions from './hooks/useCatalogActions'
import useWorkspaceChrome from './hooks/useWorkspaceChrome'
import useIntegrationActions from './hooks/useIntegrationActions'
import useWorkspaceRealtime from './hooks/useWorkspaceRealtime'
import useScenarioActions from './hooks/useScenarioActions'
import useWorkspaceAdminActions from './hooks/useWorkspaceAdminActions'
import useWorkspaceSessionActions from './hooks/useWorkspaceSessionActions'
import useWorkspaceShell from './hooks/useWorkspaceShell'
import {
  postAuthRedirectStorageKey,
  readStoredJson,
  removeStoredValue,
  workspacePreferenceStorageKey,
  writeStoredJson,
} from './services/api'
import {
  buildPagePath,
  navGroups,
  pageLookup,
  publicPages,
  resolvePageFromPath,
} from './config/pageRegistry'
import {
  buildAccessOperatorsPath,
  buildProductOptions,
  buildRevisionTitle,
  buildScenarioHistoryPath,
  buildTenantTopicPrefix,
  buildWarehouseOptions,
  buildWorkspaceConnectorDrafts,
  buildWorkspaceWarehouseDrafts,
  createDefaultAccessOperatorForm,
  createDefaultAccessUserForm,
  createDefaultCatalogForm,
  createDefaultPasswordChangeForm,
  createDefaultWorkspaceSecurityForm,
  createDefaultWorkspaceSettingsForm,
  createScenarioForm,
  createScenarioLine,
  currency,
  defaultScenarioHistoryFilters,
  defaultScenarioRequester,
  defaultScenarioReviewOwner,
  defaultTenantOnboardingForm,
  emptyRequestState,
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
  hasActiveScenarioHistoryFilters,
  hasWarehouseScope,
  integrationActorRoles,
  integrationTransformationPolicies,
  integrationValidationPolicies,
  parseCsvValues,
  resolvePreferredOperatorName,
  scenarioActorRoles,
  summarizeImpact,
} from './config/workspaceModel'

const rememberedWorkspacePreference = readStoredJson(globalThis.localStorage, workspacePreferenceStorageKey, null)
const defaultSignInTenantCode = rememberedWorkspacePreference?.remember ? (rememberedWorkspacePreference.tenantCode || '') : ''
const defaultSignInUsername = rememberedWorkspacePreference?.remember ? (rememberedWorkspacePreference.username || '') : ''
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
export default function App() {
  const [snapshot, setSnapshot] = useState(emptySnapshot)
  const [currentPage, setCurrentPage] = useState(() => resolvePageFromPath())
  const [workspaceSearch, setWorkspaceSearch] = useState('')
  const [clockTick, setClockTick] = useState(() => Date.now())
  const [selectedAlertId, setSelectedAlertId] = useState(null)
  const [selectedRecommendationId, setSelectedRecommendationId] = useState(null)
  const [selectedOrderId, setSelectedOrderId] = useState(null)
  const [selectedInventoryId, setSelectedInventoryId] = useState(null)
  const [selectedCatalogProductId, setSelectedCatalogProductId] = useState(null)
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
  const [catalogState, setCatalogState] = useState({ loading: true, error: '', success: '', products: [], importResult: null })
  const [catalogForm, setCatalogForm] = useState(createDefaultCatalogForm)
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
  const {
    rememberWorkspace,
    setRememberWorkspace,
    authSessionState,
    setAuthSessionState,
    passwordChangeState,
    setPasswordChangeState,
  } = useAuth({
    defaultTenantCode: defaultSignInTenantCode,
    defaultUsername: defaultSignInUsername,
    rememberedWorkspacePreference,
    createDefaultPasswordChangeForm,
  })

  const mergeSnapshot = (partial) => setSnapshot((current) => ({ ...current, ...partial, generatedAt: new Date().toISOString() }))
  const activeTenantCode = authSessionState.session?.tenantCode || ''
  const resetSignedInWorkspace = () => {
    setSnapshot(emptySnapshot)
    setScenarioHistoryState({ loading: false, error: '', items: [] })
    setOperatorDirectoryState({ loading: false, error: '', items: [] })
    setSystemRuntimeState({ loading: false, error: '', runtime: null })
    setAccessAdminState({ loading: false, error: '', success: '', workspace: null, operators: [], users: [] })
    setCatalogState({ loading: false, error: '', success: '', products: [], importResult: null })
    setCatalogForm(createDefaultCatalogForm())
    setSelectedCatalogProductId(null)
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
  const {
    apiUrl,
    fetchJson,
    frontendBuildCommit,
    frontendBuildTime,
    frontendBuildVersion,
    realtimeTransportLabel,
    sockJsUrl,
    websocketBrokerUrl,
    wsUrl,
  } = useApi({
    authSession: authSessionState.session,
    currentPage,
    onUnauthorized: handleExpiredSession,
  })
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

  useWorkspaceShell({
    resolvePageFromPath,
    setCurrentPage,
    setClockTick,
    searchInputRef,
    workspaceSearch,
    setWorkspaceSearch,
  })

  const {
    fetchAccessAdminData,
    fetchSnapshot,
    fetchCatalogProducts,
    fetchSystemRuntime,
    refreshScenarioHistoryQuietly,
    refreshSnapshotQuietly,
  } = useWorkspaceBootstrap({
    activeTenantCode,
    authSessionState,
    currentPage,
    rememberWorkspace,
    fetchJson,
    navigateToPage,
    redirectToPage,
    buildAccessOperatorsPath,
    buildScenarioHistoryPath,
    createDefaultAccessOperatorForm,
    createDefaultAccessUserForm,
    createDefaultCatalogForm,
    createDefaultPasswordChangeForm,
    createDefaultWorkspaceSecurityForm,
    createDefaultWorkspaceSettingsForm,
    defaultScenarioRequester,
    defaultScenarioReviewOwner,
    defaultSignInTenantCode,
    emptySnapshot,
    handleExpiredSession,
    activePageRequiresAuth: (pageMeta) => pageMeta.audience === 'app',
    pageLookup,
    readPendingPostAuthPage,
    clearPendingPostAuthPage,
    storePendingPostAuthPage,
    workspacePreferenceStorageKey,
    writeStoredJson,
    removeStoredValue,
    buildWorkspaceWarehouseDrafts,
    buildWorkspaceConnectorDrafts,
    hasActiveScenarioHistoryFilters,
    resolvePreferredOperatorName,
    hasWarehouseScope,
    operatorDirectoryState,
    accessAdminStateSetter: setAccessAdminState,
    accessUserFormSetter: setAccessUserForm,
    workspaceSettingsFormSetter: setWorkspaceSettingsForm,
    workspaceSecurityFormSetter: setWorkspaceSecurityForm,
    workspaceWarehouseDraftsSetter: setWorkspaceWarehouseDrafts,
    workspaceConnectorDraftsSetter: setWorkspaceConnectorDrafts,
    accessOperatorFormSetter: setAccessOperatorForm,
    catalogStateSetter: setCatalogState,
    selectedCatalogProductIdSetter: setSelectedCatalogProductId,
    snapshotSetter: setSnapshot,
    pageStateSetter: setPageState,
    systemRuntimeStateSetter: setSystemRuntimeState,
    tenantDirectoryStateSetter: setTenantDirectoryState,
    authSessionStateSetter: setAuthSessionState,
    passwordChangeStateSetter: setPasswordChangeState,
    operatorDirectoryStateSetter: setOperatorDirectoryState,
    scenarioHistoryStateSetter: setScenarioHistoryState,
    scenarioForm,
    scenarioRequestedBy,
    scenarioRequestedBySetter: setScenarioRequestedBy,
    scenarioReviewOwner,
    scenarioReviewOwnerSetter: setScenarioReviewOwner,
    scenarioActorRole,
    scenarioActorRoleSetter: setScenarioActorRole,
    scenarioActorRoles,
    integrationActorRole,
    integrationActorRoleSetter: setIntegrationActorRole,
    integrationActorRoles,
    snapshotInventory: snapshot.inventory,
    setScenarioForm,
    setComparisonForm,
    normalizeScenarioForm,
    scenarioHistoryFilters,
  })

  useWorkspaceRealtime({
    activeTenantCode,
    signedInTenantCode: authSessionState.session?.tenantCode,
    websocketBrokerUrl,
    sockJsUrl,
    buildTenantTopicPrefix,
    fetchSnapshot,
    fetchCatalogProducts,
    mergeSnapshot,
    setSnapshot,
    setPageState,
    setCatalogState,
    setConnectionState,
    emptySnapshot,
  })

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
        pullEndpointUrl: selectedWorkspaceConnector.pullEndpointUrl || '',
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
    metrics,
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
    showDashboardHero,
    showSummaryMetrics,
    showScenarioPlanner,
    showScenarioHistory,
    showScenarioNotifications,
    showEscalationInbox,
    showRiskAlertsPanel,
    showRiskRecommendationsPanel,
    showInventoryPanel,
    showFulfillmentPanel,
    showOrdersPanel,
    showRuntimePanel,
    showIncidentPanel,
    showBusinessEventsPanel,
    showAuditPanel,
    showIntegrationConnectorsPanel,
    showIntegrationImportsPanel,
    showReplayQueuePanel,
    showAccessAdminPanel,
    showTenantManagementPanel,
    showLocationsExperience,
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

  function operatorCanOwnConnector(operator, connector) {
    return !connector.defaultWarehouseCode || !operator.warehouseScopes.length || operator.warehouseScopes.includes(connector.defaultWarehouseCode)
  }
  const comparisonPrimarySummary = comparisonState.result ? summarizeImpact(comparisonState.result.primary) : null
  const comparisonAlternativeSummary = comparisonState.result ? summarizeImpact(comparisonState.result.alternative) : null
  const {
    handleSignInSubmit,
    signInOperator,
    signOutOperator,
    changeSignedInPassword,
    onboardTenant,
  } = useWorkspaceSessionActions({
    authSessionState,
    setAuthSessionState,
    passwordChangeState,
    setPasswordChangeState,
    tenantOnboardingForm,
    setTenantOnboardingForm,
    setTenantOnboardingState,
    setTenantDirectoryState,
    rememberWorkspace,
    fetchJson,
    navigateToPage,
    resetSignedInWorkspace,
    defaultTenantOnboardingForm,
    createDefaultPasswordChangeForm,
    workspacePreferenceStorageKey,
    writeStoredJson,
    removeStoredValue,
    readPendingPostAuthPage,
    clearPendingPostAuthPage,
  })
  const {
    resetCatalogForm,
    saveCatalogProduct,
    importCatalogProducts,
  } = useCatalogActions({
    canManageTenantAccess,
    catalogForm,
    createDefaultCatalogForm,
    fetchCatalogProducts,
    fetchJson,
    setCatalogForm,
    setCatalogState,
    setSelectedCatalogProductId,
  })
  const {
    toggleConnector,
    replayFailedIntegration,
  } = useIntegrationActions({
    fetchJson,
    refreshSnapshotQuietly,
    setIntegrationConnectorState,
    setIntegrationReplayState,
  })
  const {
    saveWorkspaceSettings,
    saveWorkspaceSecuritySettings,
    saveWorkspaceWarehouse,
    saveWorkspaceConnectorSupport,
    saveTenantOperator,
    saveTenantUser,
    resetTenantUserPassword,
  } = useWorkspaceAdminActions({
    accessAdminOperators,
    accessOperatorForm,
    accessUserForm,
    activeTenantCode,
    buildAccessOperatorsPath,
    createDefaultAccessOperatorForm,
    createDefaultAccessUserForm,
    fetchAccessAdminData,
    fetchJson,
    parseCsvValues,
    setAccessAdminState,
    setAccessOperatorForm,
    setAccessUserForm,
    setOperatorDirectoryState,
    workspaceConnectorDrafts,
    workspaceSecurityForm,
    workspaceSettingsForm,
    workspaceWarehouseDrafts,
  })
  const {
    analyzeScenario,
    compareScenarios,
    executeScenario,
    loadScenarioIntoPlanner,
    saveScenarioPlan,
    approveScenarioPlan,
    rejectScenarioPlan,
    acknowledgeScenarioEscalation,
  } = useScenarioActions({
    alternativeContext,
    authSessionState,
    buildRevisionTitle,
    comparisonForm,
    defaultScenarioRequester,
    defaultScenarioReviewOwner,
    emptyRequestState,
    fetchJson,
    fetchSnapshot,
    formatCodeLabel,
    formatTimestamp,
    primaryContext,
    refreshScenarioHistoryQuietly,
    refreshSnapshotQuietly,
    scenarioActorRole,
    scenarioForm,
    scenarioHistoryItems,
    scenarioPlanName,
    scenarioRequestedBy,
    scenarioReviewNote,
    scenarioReviewOwner,
    scenarioRevisionSource,
    setComparisonState,
    setScenarioApprovalState,
    setScenarioEscalationAckState,
    setScenarioExecutionState,
    setScenarioForm,
    setScenarioLoadState,
    setScenarioPlanName,
    setScenarioRequestedBy,
    setScenarioRejectionState,
    setScenarioReviewOwner,
    setScenarioRevisionSource,
    setScenarioSaveState,
    setScenarioState,
    signedInActorName,
  })
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
    <WorkspaceAuthenticatedApp
      currentPage={currentPage}
      effectivePageMeta={effectivePageMeta}
      navGroups={navGroups}
      pageLookup={pageLookup}
      pageBadgeMap={pageBadgeMap}
      connectionState={connectionState}
      formatCodeLabel={formatCodeLabel}
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
      globalNotificationCount={globalNotificationCount}
      topbarQuickActions={topbarQuickActions}
      pageState={pageState}
      fetchSnapshot={fetchSnapshot}
      actionState={actionState}
      systemRuntimeState={systemRuntimeState}
      fetchSystemRuntime={fetchSystemRuntime}
      signedInSession={signedInSession}
      signOutOperator={signOutOperator}
      authSessionState={authSessionState}
      utilityRailContext={{
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
      showDashboardHero={showDashboardHero}
      controlHighlights={controlHighlights}
      pendingReviewCount={pendingReviewCount}
      pendingReplayCount={pendingReplayCount}
      systemIncidents={systemIncidents}
      pageStatusMap={pageStatusMap}
      appPageCount={appPageCount}
      enabledConnectorCount={enabledConnectorCount}
      snapshot={snapshot}
      dashboardContext={{ isAuthenticated, isDashboardPage, warehouseOptions, snapshot, fulfillmentOverview, activeAlerts, urgentActions, navigateToPage, setSelectedAlertId, setSelectedRecommendationId, setSelectedScenarioId, pendingApprovalScenarios, runtime, systemIncidents, utilityTimeline, formatCodeLabel, formatTimestamp, formatBuildValue, getRuntimeStatusClassName, enabledConnectorCount, pendingReplayCount, pageLoading: pageState.loading }}
      alertsContext={{ isAuthenticated, isAlertsPage, activeAlerts, selectedAlertId, setSelectedAlertId, formatTimestamp }}
      recommendationsContext={{ isAuthenticated, isRecommendationsPage, snapshot, recommendationNow, recommendationSoon, recommendationWatch, selectedRecommendationId, setSelectedRecommendationId, formatTimestamp }}
      ordersContext={{ isAuthenticated, isOrdersPage, snapshot, fulfillmentOverview, selectedOrderId, setSelectedOrderId, summary, warehouseOptions, currency, formatCodeLabel, formatRelativeHours, formatTimestamp }}
      inventoryContext={{ isAuthenticated, isInventoryPage, snapshot, selectedInventoryId, setSelectedInventoryId, lowStockInventory, highRiskInventory, fastMovingInventory, warehouseOptions, formatCodeLabel, formatRelativeHours }}
      catalogContext={{ isAuthenticated, isCatalogPage, catalogState, catalogForm, setCatalogForm, selectedCatalogProductId, setSelectedCatalogProductId, saveCatalogProduct, importCatalogProducts, resetCatalogForm, canManageTenantAccess }}
      locationsContext={{ isAuthenticated, isLocationsPage, warehouseOptions, snapshot, fulfillmentOverview, activeAlerts, formatCodeLabel }}
      fulfillmentContext={{ isAuthenticated, isFulfillmentPage, delayedFulfillments, fulfillmentOverview, warehouseOptions, formatCodeLabel, formatRelativeHours, getFulfillmentStatusClassName, enabledConnectorCount, snapshot, pendingReplayCount }}
      scenarioControlContext={{ isAuthenticated, isScenariosPage, isScenarioHistoryPage, isApprovalsPage, isEscalationsPage, scenarioHistoryItems, pendingApprovalScenarios, approvedScenarios, rejectedScenarios, overdueScenarios, approvalBoard, formatCodeLabel }}
      scenarioPlannerContext={scenarioPlannerContext}
      scenarioHistoryContext={{ isAuthenticated, isScenarioHistoryPage, scenarioHistoryItems, selectedHistoryScenario, setSelectedScenarioId, formatCodeLabel, formatTimestamp, scenarioDecisionContext }}
      approvalsContext={{ isAuthenticated, isApprovalsPage, pendingApprovalScenarios, approvedScenarios, rejectedScenarios, overdueScenarios, selectedApprovalScenario, setSelectedScenarioId, formatCodeLabel, formatTimestamp, snapshot, scenarioDecisionContext }}
      escalationsContext={{ isAuthenticated, isEscalationsPage, snapshot, systemIncidents, escalatedScenarios, selectedEscalationScenario, setSelectedScenarioId, formatCodeLabel, formatTimestamp, getIncidentStatusClassName, scenarioDecisionContext }}
      integrationsContext={{ isAuthenticated, isIntegrationsPage, snapshot, selectedIntegrationConnectorId, setSelectedIntegrationConnectorId, enabledConnectorCount, pendingReplayCount, systemIncidents, navigateToPage, formatCodeLabel, formatTimestamp, getImportStatusClassName }}
      replayContext={{ isAuthenticated, isReplayPage, snapshot, selectedReplayRecordId, setSelectedReplayRecordId, pendingReplayCount, integrationReplayState, replayFailedIntegration, signedInSession, signedInRoles, signedInWarehouseScopes, hasWarehouseScope, navigateToPage, formatCodeLabel, formatTimestamp, getReplayStatusClassName }}
      runtimeContext={{ isAuthenticated, isRuntimePage, runtime, systemIncidents, selectedRuntimeIncidentKey, setSelectedRuntimeIncidentKey, navigateToPage, formatCodeLabel, formatMetricValue, formatTimestamp, getIncidentStatusClassName, getRuntimeStatusClassName }}
      auditContext={{ isAuthenticated, isAuditPage, snapshot, systemIncidents, pendingReplayCount, recentBusinessEvents, recentAuditEntries, selectedAuditTrace, setSelectedTraceEntryKey, formatCodeLabel, formatTimestamp, navigateToPage }}
      usersContext={{ isAuthenticated, isUsersPage, accessAdminOperators, accessAdminUsers, workspaceAdmin, selectedAccessSubject, setSelectedAccessSubjectKey, formatCodeLabel, navigateToPage }}
      settingsContext={{ isAuthenticated, isSettingsPage, workspaceAdmin, accessAdminState, canManageTenantAccess, workspaceSettingsForm, setWorkspaceSettingsForm, workspaceSecurityForm, setWorkspaceSecurityForm, saveWorkspaceSettings, saveWorkspaceSecuritySettings, selectedWorkspaceWarehouse, selectedWorkspaceWarehouseDraft, selectedWorkspaceConnector, selectedWorkspaceConnectorDraft, selectedWorkspaceConnectorOwnerOptions, setSelectedWorkspaceWarehouseId, setSelectedWorkspaceConnectorId, setWorkspaceWarehouseDrafts, setWorkspaceConnectorDrafts, saveWorkspaceWarehouse, saveWorkspaceConnectorSupport, formatCodeLabel, integrationValidationPolicies, integrationTransformationPolicies }}
      profileContext={{ isAuthenticated, isProfilePage, passwordChangeRequired, passwordRotationRequired, activeAlerts, pendingApprovalScenarios, systemIncidents, signedInSession, signedInRoles, signedInWarehouseScopes, signedInSessionExpiresAt, signedInPasswordExpiresAt, formatCodeLabel, formatTimestamp, passwordChangeState, setPasswordChangeState, changeSignedInPassword, signOutOperator, authSessionState, navigateToPage }}
      platformAdminContext={{ isAuthenticated, isPlatformPage, runtime, tenantDirectoryState, systemIncidents, pendingReplayCount, selectedTenantPortfolio, setSelectedTenantPortfolioCode, signedInSession, formatBuildValue, formatCodeLabel, formatTimestamp, getIncidentStatusClassName, getRuntimeStatusClassName, navigateToPage }}
      tenantsContext={{ isAuthenticated, isTenantsPage, tenantDirectoryState, signedInSession, signedInRoles, tenantOnboardingState, tenantOnboardingForm, setTenantOnboardingForm, onboardTenant, signInOperator, authSessionState, setAuthSessionState }}
      systemConfigContext={{ isAuthenticated, isSystemConfigPage, runtime, formatMetricValue }}
      releasesContext={{ isAuthenticated, isReleasesPage, runtime, formatBuildValue, formatCodeLabel, formatTimestamp, frontendBuildVersion, frontendBuildCommit, frontendBuildTime, apiUrl, wsUrl, realtimeTransportLabel, getRuntimeStatusClassName }}
    />
  )
}
