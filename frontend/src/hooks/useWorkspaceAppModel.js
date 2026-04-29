import { startTransition } from 'react'
import useApi from './useApi'
import useAuth from './useAuth'
import useCatalogActions from './useCatalogActions'
import useIntegrationActions from './useIntegrationActions'
import useScenarioActions from './useScenarioActions'
import useWorkspaceAdminActions from './useWorkspaceAdminActions'
import useWorkspaceBootstrap from './useWorkspaceBootstrap'
import useWorkspaceRealtime from './useWorkspaceRealtime'
import useWorkspaceSessionActions from './useWorkspaceSessionActions'
import useWorkspaceShell from './useWorkspaceShell'
import useWorkspaceState from './useWorkspaceState'
import {
  postAuthRedirectStorageKey,
  readStoredJson,
  removeStoredValue,
  workspacePreferenceStorageKey,
  writeStoredJson,
} from '../services/api'
import {
  buildPagePath,
  pageLookup,
  resolvePageFromPath,
} from '../config/pageRegistry'
import {
  buildAccessOperatorsPath,
  buildRevisionTitle,
  buildScenarioHistoryPath,
  buildTenantTopicPrefix,
  buildWorkspaceConnectorDrafts,
  buildWorkspaceWarehouseDrafts,
  createDefaultAccessOperatorForm,
  createDefaultAccessUserForm,
  createDefaultCatalogForm,
  createDefaultPasswordChangeForm,
  createDefaultWorkspaceSecurityForm,
  createDefaultWorkspaceSettingsForm,
  defaultScenarioRequester,
  defaultScenarioReviewOwner,
  defaultTenantOnboardingForm,
  emptyRequestState,
  emptySnapshot,
  formatCodeLabel,
  formatTimestamp,
  hasActiveScenarioHistoryFilters,
  hasWarehouseScope,
  integrationActorRoles,
  parseCsvValues,
  resolvePreferredOperatorName,
  scenarioActorRoles,
} from '../config/workspaceModel'

const readRememberedWorkspacePreference = () => readStoredJson(globalThis.localStorage, workspacePreferenceStorageKey, null)

const resolveDefaultSignInTenantCode = (rememberedWorkspacePreference) => (
  rememberedWorkspacePreference?.remember ? (rememberedWorkspacePreference.tenantCode || '') : ''
)

const resolveDefaultSignInUsername = (rememberedWorkspacePreference) => (
  rememberedWorkspacePreference?.remember ? (rememberedWorkspacePreference.username || '') : ''
)

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

export default function useWorkspaceAppModel() {
  const rememberedWorkspacePreference = readRememberedWorkspacePreference()
  const defaultSignInTenantCode = resolveDefaultSignInTenantCode(rememberedWorkspacePreference)
  const defaultSignInUsername = resolveDefaultSignInUsername(rememberedWorkspacePreference)
  const workspaceState = useWorkspaceState({ initialPage: resolvePageFromPath() })
  const {
    currentPage,
    setCurrentPage,
    workspaceSearch,
    setWorkspaceSearch,
    setClockTick,
    searchInputRef,
    setConnectionState,
    setPageState,
    setSystemRuntimeState,
    setTenantDirectoryState,
    accessAdminState,
    setAccessAdminState,
    workspaceSettingsForm,
    workspaceSecurityForm,
    workspaceWarehouseDrafts,
    workspaceConnectorDrafts,
    accessOperatorForm,
    setAccessOperatorForm,
    accessUserForm,
    setAccessUserForm,
    snapshot,
    setSnapshot,
    catalogState,
    setCatalogState,
    setSelectedCatalogProductId,
    catalogForm,
    setCatalogForm,
    operatorDirectoryState,
    setOperatorDirectoryState,
    setIntegrationConnectorState,
    setIntegrationReplayState,
    integrationActorRole,
    setIntegrationActorRole,
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
    scenarioRevisionSource,
    setScenarioRevisionSource,
    setScenarioState,
    setComparisonState,
    setScenarioExecutionState,
    setScenarioLoadState,
    setScenarioSaveState,
    setScenarioApprovalState,
    setScenarioRejectionState,
    setScenarioEscalationAckState,
    scenarioHistoryFilters,
    scenarioHistoryState,
    setScenarioHistoryState,
    mergeSnapshot,
    resetSignedInWorkspace,
    normalizeScenarioForm,
    buildScenarioContext,
  } = workspaceState

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

  useWorkspaceShell({
    resolvePageFromPath,
    setCurrentPage,
    setClockTick,
    searchInputRef,
    workspaceSearch,
    setWorkspaceSearch,
  })

  const activeTenantCode = authSessionState.session?.tenantCode || ''
  const primaryContext = buildScenarioContext(scenarioForm)
  const alternativeContext = buildScenarioContext(comparisonForm)
  const signedInActorName = authSessionState.session?.actorName || ''
  const scenarioHistoryItems = scenarioHistoryState.items

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
    workspaceSettingsFormSetter: workspaceState.setWorkspaceSettingsForm,
    workspaceSecurityFormSetter: workspaceState.setWorkspaceSecurityForm,
    workspaceWarehouseDraftsSetter: workspaceState.setWorkspaceWarehouseDrafts,
    workspaceConnectorDraftsSetter: workspaceState.setWorkspaceConnectorDrafts,
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

  const sessionActions = useWorkspaceSessionActions({
    authSessionState,
    setAuthSessionState,
    passwordChangeState,
    setPasswordChangeState,
    tenantOnboardingForm: workspaceState.tenantOnboardingForm,
    setTenantOnboardingForm: workspaceState.setTenantOnboardingForm,
    setTenantOnboardingState: workspaceState.setTenantOnboardingState,
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

  const catalogActions = useCatalogActions({
    canManageTenantAccess: (authSessionState.session?.roles || []).includes('TENANT_ADMIN'),
    catalogForm,
    createDefaultCatalogForm,
    fetchCatalogProducts,
    fetchJson,
    setCatalogForm,
    setCatalogState,
    setSelectedCatalogProductId,
  })

  const integrationActions = useIntegrationActions({
    fetchJson,
    refreshSnapshotQuietly,
    setIntegrationConnectorState,
    setIntegrationReplayState,
  })

  const workspaceAdminActions = useWorkspaceAdminActions({
    accessAdminOperators: accessAdminState.operators,
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

  const scenarioActions = useScenarioActions({
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
    scenarioReviewNote: workspaceState.scenarioReviewNote,
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

  return {
    workspaceState,
    authContext: {
      rememberWorkspace,
      setRememberWorkspace,
      authSessionState,
      setAuthSessionState,
      passwordChangeState,
      setPasswordChangeState,
    },
    apiContext: {
      apiUrl,
      wsUrl,
      realtimeTransportLabel,
      frontendBuildVersion,
      frontendBuildCommit,
      frontendBuildTime,
    },
    navigation: {
      navigateToPage,
      jumpToPageSection,
    },
    bootstrapContext: {
      fetchSnapshot,
      fetchSystemRuntime,
    },
    sessionActions,
    catalogActions,
    integrationActions,
    workspaceAdminActions,
    scenarioActions,
  }
}
