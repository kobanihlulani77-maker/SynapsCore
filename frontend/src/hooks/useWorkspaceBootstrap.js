import { useEffect } from 'react'

export default function useWorkspaceBootstrap({
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
  activePageRequiresAuth,
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
  hasExplicitWarehouseScope,
  isBootstrapTenantAdmin,
  operatorDirectoryState,
  catalogStateSetter,
  selectedCatalogProductIdSetter,
  accessAdminStateSetter,
  accessUserFormSetter,
  workspaceSettingsFormSetter,
  workspaceSecurityFormSetter,
  workspaceWarehouseDraftsSetter,
  workspaceConnectorDraftsSetter,
  accessOperatorFormSetter,
  snapshotSetter,
  pageStateSetter,
  systemRuntimeStateSetter,
  tenantDirectoryStateSetter,
  authSessionStateSetter,
  passwordChangeStateSetter,
  operatorDirectoryStateSetter,
  scenarioHistoryStateSetter,
  warehouseAccessStateSetter,
  scenarioForm,
  scenarioRequestedBy,
  scenarioRequestedBySetter,
  scenarioReviewOwner,
  scenarioReviewOwnerSetter,
  integrationActorRole,
  integrationActorRoleSetter,
  integrationActorRoles,
  snapshotInventory,
  setScenarioForm,
  setComparisonForm,
  normalizeScenarioForm,
  scenarioHistoryFilters,
  normalizeSnapshot,
}) {
  function mergeReplaySurfaceSnapshot(replaySurfaceData) {
    snapshotSetter((current) => {
      const previousSnapshot = current || emptySnapshot
      return normalizeSnapshot({
        ...emptySnapshot,
        ...previousSnapshot,
        integrationConnectors: replaySurfaceData?.integrationConnectors ?? previousSnapshot.integrationConnectors ?? [],
        integrationReplayQueue: replaySurfaceData?.integrationReplayQueue ?? previousSnapshot.integrationReplayQueue ?? [],
        generatedAt: previousSnapshot.generatedAt ?? new Date().toISOString(),
      }, previousSnapshot)
    })
  }

  function mergeOrderSurfaceSnapshot(orderSurfaceData) {
    snapshotSetter((current) => {
      const previousSnapshot = current || emptySnapshot
      return normalizeSnapshot({
        ...emptySnapshot,
        ...previousSnapshot,
        recentOrders: orderSurfaceData?.recentOrders ?? previousSnapshot.recentOrders ?? [],
        generatedAt: previousSnapshot.generatedAt ?? new Date().toISOString(),
      }, previousSnapshot)
    })
  }

  async function fetchReplaySurfaceData() {
    const replayQueueRequest = fetchJson(
      '/api/integrations/orders/replay-queue',
      globalThis.AbortSignal?.timeout
        ? { signal: globalThis.AbortSignal.timeout(8_000) }
        : {},
    )
    const connectorsRequest = fetchJson(
      '/api/integrations/orders/connectors',
      globalThis.AbortSignal?.timeout
        ? { signal: globalThis.AbortSignal.timeout(5_000) }
        : {},
    )

    const [integrationReplayQueueResult, integrationConnectorsResult] = await Promise.allSettled([
      replayQueueRequest,
      connectorsRequest,
    ])

    if (integrationReplayQueueResult.status !== 'fulfilled' && integrationConnectorsResult.status !== 'fulfilled') {
      throw integrationReplayQueueResult.reason || integrationConnectorsResult.reason || new Error('Replay surface data could not be loaded.')
    }

    return {
      integrationReplayQueue: integrationReplayQueueResult.status === 'fulfilled' && Array.isArray(integrationReplayQueueResult.value)
        ? integrationReplayQueueResult.value
        : null,
      integrationConnectors: integrationConnectorsResult.status === 'fulfilled' && Array.isArray(integrationConnectorsResult.value)
        ? integrationConnectorsResult.value
        : null,
    }
  }

  async function fetchOrderSurfaceData() {
    const recentOrders = await fetchJson(
      '/api/orders/recent',
      globalThis.AbortSignal?.timeout
        ? { signal: globalThis.AbortSignal.timeout(8_000) }
        : {},
    )

    return {
      recentOrders: Array.isArray(recentOrders) ? recentOrders : null,
    }
  }

  async function fetchWarehouseContext({ quiet = false } = {}) {
    if (!authSessionState.session?.tenantCode) {
      warehouseAccessStateSetter({ loading: false, error: '', items: [] })
      return []
    }

    if (!quiet) {
      warehouseAccessStateSetter((current) => ({ ...current, loading: true, error: '' }))
    }

    try {
      const [session, warehouses] = await Promise.all([
        fetchJson('/api/auth/session'),
        fetchJson('/api/warehouses'),
      ])
      if (session?.signedIn) {
        const currentSession = authSessionState.session
        const currentRoles = [...(currentSession?.roles || [])].sort().join('|')
        const nextRoles = [...(session.roles || [])].sort().join('|')
        const currentScopes = [...(currentSession?.warehouseScopes || [])].sort().join('|')
        const nextScopes = [...(session.warehouseScopes || [])].sort().join('|')
        if (
          currentSession?.tenantCode !== session.tenantCode
          || currentSession?.username !== session.username
          || currentRoles !== nextRoles
          || currentScopes !== nextScopes
        ) {
          authSessionStateSetter((current) => ({
            ...current,
            loading: false,
            error: '',
            action: '',
            session,
            tenantCode: session.tenantCode || current.tenantCode,
            username: session.username || current.username,
            password: '',
          }))
        }
      }
      const activeWarehouses = Array.isArray(warehouses)
        ? warehouses.filter((warehouse) => warehouse?.active !== false)
        : []
      warehouseAccessStateSetter({ loading: false, error: '', items: activeWarehouses })
      return activeWarehouses
    } catch (error) {
      // Fail closed for operation targets until authoritative access can be read again.
      warehouseAccessStateSetter({ loading: false, error: error.message, items: [] })
      return []
    }
  }

  async function fetchAccessAdminData() {
    const [workspace, operators, users] = await Promise.all([
      fetchJson('/api/access/admin/workspace'),
      fetchJson('/api/access/admin/operators'),
      fetchJson('/api/access/admin/users'),
    ])
    const defaultOperatorActorName = operators.find((operator) => operator.active)?.actorName || operators[0]?.actorName || ''
    accessAdminStateSetter((current) => ({ ...current, loading: false, error: '', workspace, operators, users }))
    workspaceSettingsFormSetter({
      tenantName: workspace.tenantName || '',
      description: workspace.description || '',
      version: workspace.version,
    })
    workspaceSecurityFormSetter({
      passwordRotationDays: String(workspace.securitySettings?.passwordRotationDays || 90),
      sessionTimeoutMinutes: String(workspace.securitySettings?.sessionTimeoutMinutes || 480),
      invalidateOtherSessions: false,
      version: workspace.version,
    })
    workspaceWarehouseDraftsSetter(buildWorkspaceWarehouseDrafts(workspace))
    workspaceConnectorDraftsSetter(buildWorkspaceConnectorDrafts(workspace))
    accessUserFormSetter((current) => ({
      ...current,
      operatorActorName: operators.some((operator) => operator.actorName === current.operatorActorName)
        ? current.operatorActorName
        : defaultOperatorActorName,
    }))
    await fetchWarehouseContext({ quiet: true })
  }

  async function fetchSnapshot() {
    const shouldHydrateReplaySurface = currentPage === 'replay' || currentPage === 'integrations'
    const shouldHydrateOrderSurface = currentPage === 'orders'
    const [snapshotResult, replaySurfaceResult, orderSurfaceResult] = await Promise.allSettled([
      fetchJson('/api/dashboard/snapshot'),
      shouldHydrateReplaySurface ? fetchReplaySurfaceData() : Promise.resolve(null),
      shouldHydrateOrderSurface ? fetchOrderSurfaceData() : Promise.resolve(null),
    ])

    if (
      snapshotResult.status !== 'fulfilled'
      && (!shouldHydrateReplaySurface || replaySurfaceResult.status !== 'fulfilled')
      && (!shouldHydrateOrderSurface || orderSurfaceResult.status !== 'fulfilled')
    ) {
      throw snapshotResult.reason || replaySurfaceResult.reason || orderSurfaceResult.reason || new Error('Workspace snapshot could not be loaded.')
    }

    const nextSnapshot = snapshotResult.status === 'fulfilled' ? snapshotResult.value : null
    const replaySurfaceData = replaySurfaceResult.status === 'fulfilled' ? replaySurfaceResult.value : null
    const orderSurfaceData = orderSurfaceResult.status === 'fulfilled' ? orderSurfaceResult.value : null
    const failedSources = []
    if (snapshotResult.status !== 'fulfilled') failedSources.push('Dashboard snapshot')
    if (shouldHydrateReplaySurface && replaySurfaceResult.status !== 'fulfilled') failedSources.push('Replay and integration surface')
    if (shouldHydrateOrderSurface && orderSurfaceResult.status !== 'fulfilled') failedSources.push('Order surface')

    snapshotSetter((current) => {
      const previousSnapshot = current || emptySnapshot
      const baseSnapshot = nextSnapshot || previousSnapshot
      return normalizeSnapshot({
        ...emptySnapshot,
        ...previousSnapshot,
        ...baseSnapshot,
        recentEvents: baseSnapshot.recentEvents ?? previousSnapshot.recentEvents ?? [],
        auditLogs: baseSnapshot.auditLogs ?? previousSnapshot.auditLogs ?? [],
        systemIncidents: baseSnapshot.systemIncidents ?? previousSnapshot.systemIncidents ?? [],
        recentOrders: orderSurfaceData?.recentOrders ?? baseSnapshot.recentOrders ?? previousSnapshot.recentOrders ?? [],
        integrationConnectors: replaySurfaceData?.integrationConnectors ?? baseSnapshot.integrationConnectors ?? previousSnapshot.integrationConnectors ?? [],
        integrationImportRuns: baseSnapshot.integrationImportRuns ?? previousSnapshot.integrationImportRuns ?? [],
        integrationReplayQueue: replaySurfaceData?.integrationReplayQueue ?? baseSnapshot.integrationReplayQueue ?? previousSnapshot.integrationReplayQueue ?? [],
        scenarioNotifications: baseSnapshot.scenarioNotifications ?? previousSnapshot.scenarioNotifications ?? [],
        slaEscalations: baseSnapshot.slaEscalations ?? previousSnapshot.slaEscalations ?? [],
        recentScenarios: baseSnapshot.recentScenarios ?? previousSnapshot.recentScenarios ?? [],
        generatedAt: baseSnapshot.generatedAt ?? previousSnapshot.generatedAt ?? new Date().toISOString(),
      }, previousSnapshot)
    })
    pageStateSetter((current) => ({
      ...current,
      loading: false,
      error: failedSources.length
        ? `${failedSources.join(', ')} unavailable. Retained values may be stale.`
        : '',
      freshness: failedSources.length ? (current.lastSuccessfulAt ? 'stale' : 'unknown') : 'current',
      lastSuccessfulAt: snapshotResult.status === 'fulfilled'
        ? (nextSnapshot?.generatedAt || nextSnapshot?.summary?.lastUpdatedAt || new Date().toISOString())
        : current.lastSuccessfulAt,
      degradedSources: failedSources,
    }))
  }

  async function fetchCatalogProducts(options = {}) {
    if (!options.quiet) {
      catalogStateSetter((current) => ({ ...current, loading: true, error: '', success: '' }))
    }
    try {
      const products = await fetchJson('/api/products')
      catalogStateSetter((current) => ({ ...current, loading: false, error: '', products, success: options.success || current.success }))
      selectedCatalogProductIdSetter((currentId) => (products.some((product) => product.id === currentId) ? currentId : products[0]?.id || null))
      return products
    } catch (error) {
      catalogStateSetter((current) => ({ ...current, loading: false, error: error.message }))
      if (!options.quiet) throw error
      return []
    }
  }

  async function fetchSystemRuntime() {
    const runtime = await fetchJson('/api/system/runtime')
    systemRuntimeStateSetter({ loading: false, error: '', runtime })
  }

  async function refreshSystemRuntimeQuietly() {
    try {
      await fetchSystemRuntime()
    } catch {
      // Keep runtime feedback visible even if the secondary refresh misses.
    }
  }

  async function refreshSnapshotQuietly() {
    try {
      await Promise.all([fetchSnapshot(), fetchCatalogProducts({ quiet: true })])
      await refreshSystemRuntimeQuietly()
    } catch {
      // Keep planning feedback visible even if the secondary snapshot refresh misses.
    }
  }

  async function refreshScenarioHistoryQuietly(filters = scenarioHistoryFilters) {
    try {
      const history = await fetchJson(buildScenarioHistoryPath(filters))
      scenarioHistoryStateSetter({ loading: false, error: '', items: history })
    } catch (error) {
      scenarioHistoryStateSetter((current) => current.items.length
        ? { ...current, loading: false }
        : { loading: false, error: error.message, items: [] })
    }
  }

  useEffect(() => {
    let active = true
    async function loadTenants() {
      const signedInSession = authSessionState.session
      if (active) {
        tenantDirectoryStateSetter({
          loading: false,
          error: '',
          items: signedInSession ? [{ code: signedInSession.tenantCode, name: signedInSession.tenantName }] : [],
        })
      }
    }
    loadTenants()
    return () => { active = false }
  }, [activeTenantCode, authSessionState.session])

  useEffect(() => {
    let active = true
    if (!authSessionState.session?.tenantCode) {
      warehouseAccessStateSetter({ loading: false, error: '', items: [] })
      return () => { active = false }
    }

    const refreshWarehouseContext = () => {
      if (active) void fetchWarehouseContext({ quiet: true })
    }
    refreshWarehouseContext()
    const refreshInterval = globalThis.setInterval(refreshWarehouseContext, 30_000)
    return () => {
      active = false
      globalThis.clearInterval(refreshInterval)
    }
  }, [activeTenantCode, authSessionState.session?.tenantCode])

  useEffect(() => {
    let active = true
    async function loadOperators() {
      if (!authSessionState.session?.tenantCode) {
        if (active) {
          operatorDirectoryStateSetter({ loading: false, error: '', items: [] })
        }
        return
      }
      try {
        const operators = await fetchJson(buildAccessOperatorsPath(activeTenantCode))
        if (active) {
          operatorDirectoryStateSetter({ loading: false, error: '', items: operators })
        }
      } catch (error) {
        if (active) {
          operatorDirectoryStateSetter({ loading: false, error: error.message, items: [] })
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
          accessAdminStateSetter({ loading: false, error: '', success: '', workspace: null, operators: [], users: [] })
          workspaceSettingsFormSetter(createDefaultWorkspaceSettingsForm())
          workspaceSecurityFormSetter(createDefaultWorkspaceSecurityForm())
          workspaceWarehouseDraftsSetter({})
          workspaceConnectorDraftsSetter({})
          accessOperatorFormSetter(createDefaultAccessOperatorForm())
          accessUserFormSetter(createDefaultAccessUserForm())
        }
        return
      }

      if (active) {
        accessAdminStateSetter((current) => ({ ...current, loading: true, error: '' }))
      }
      try {
        await fetchAccessAdminData()
      } catch (error) {
        if (active) {
          accessAdminStateSetter((current) => ({ ...current, loading: false, error: error.message, workspace: null, operators: [], users: [] }))
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
          systemRuntimeStateSetter({ loading: false, error: '', runtime: null })
        }
        return
      }
      try {
        const runtime = await fetchJson('/api/system/runtime')
        if (active) {
          systemRuntimeStateSetter({ loading: false, error: '', runtime })
        }
      } catch (error) {
        if (active) {
          systemRuntimeStateSetter({ loading: false, error: error.message, runtime: null })
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
          authSessionStateSetter((current) => ({
            ...current,
            loading: false,
            error: '',
            action: '',
            session: session.signedIn ? session : null,
            tenantCode: session.signedIn ? session.tenantCode : (current.tenantCode || defaultSignInTenantCode),
            username: session.signedIn ? session.username : current.username,
            password: session.signedIn ? '' : current.password,
          }))
          passwordChangeStateSetter((current) => ({
            ...current,
            loading: false,
            error: '',
            success: session.signedIn ? current.success : '',
            form: session.signedIn ? current.form : createDefaultPasswordChangeForm(),
          }))
        }
      } catch (error) {
        if (active) {
          authSessionStateSetter((current) => ({
            ...current,
            loading: false,
            error: error.message,
            action: '',
            session: null,
            password: current.password,
          }))
          passwordChangeStateSetter((current) => ({ ...current, loading: false, error: '', form: createDefaultPasswordChangeForm() }))
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
    if (!authSessionState.session && activePageRequiresAuth(currentMeta)) {
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
    if (!snapshotInventory.length) return
    setScenarioForm((current) => normalizeScenarioForm(current, snapshotInventory))
    setComparisonForm((current) => normalizeScenarioForm(current, snapshotInventory))
  }, [snapshotInventory])

  useEffect(() => {
    if (!operatorDirectoryState.items.length) return

    const scenarioWarehouseCode = scenarioForm.warehouseCode?.trim() || ''
    const availableOperators = scenarioWarehouseCode
      ? operatorDirectoryState.items.filter((operator) => hasWarehouseScope(operator.warehouseScopes, scenarioWarehouseCode))
      : []
    const reviewOwnerOptions = availableOperators.filter((operator) => (
      operator.roles.includes('REVIEW_OWNER')
      && hasExplicitWarehouseScope(operator.warehouseScopes, scenarioWarehouseCode)
      && !isBootstrapTenantAdmin(operator)
      && operator.actorName?.toLowerCase() !== authSessionState.session?.actorName?.toLowerCase()
    ))

    if (!availableOperators.some((operator) => operator.actorName === scenarioRequestedBy)) {
      scenarioRequestedBySetter(resolvePreferredOperatorName(availableOperators, defaultScenarioRequester))
    }
    if (!reviewOwnerOptions.some((operator) => operator.actorName === scenarioReviewOwner)) {
      scenarioReviewOwnerSetter(resolvePreferredOperatorName(reviewOwnerOptions, defaultScenarioReviewOwner))
    }
  }, [
    operatorDirectoryState.items,
    scenarioForm.warehouseCode,
    scenarioRequestedBy,
    scenarioReviewOwner,
    authSessionState.session?.actorName,
  ])

  useEffect(() => {
    if (!authSessionState.session) return

    const sessionRoles = authSessionState.session.roles ?? []
    scenarioRequestedBySetter(authSessionState.session.actorName)

    if (!sessionRoles.includes(integrationActorRole)) {
      const fallbackIntegrationRole = integrationActorRoles.find((role) => sessionRoles.includes(role))
      if (fallbackIntegrationRole) {
        integrationActorRoleSetter(fallbackIntegrationRole)
      }
    }
  }, [authSessionState.session, integrationActorRole])

  useEffect(() => {
    let active = true
    async function loadDedicatedIntegrationSurface() {
      if (!authSessionState.session?.tenantCode) {
        return
      }
      if (currentPage !== 'replay' && currentPage !== 'integrations') {
        return
      }
      try {
        const replaySurfaceData = await fetchReplaySurfaceData()
        if (!active) {
          return
        }
        mergeReplaySurfaceSnapshot(replaySurfaceData)
        pageStateSetter((current) => ({ ...current, loading: false, error: current.error || '' }))
      } catch (error) {
        if (!active) {
          return
        }
        pageStateSetter((current) => ({
          ...current,
          loading: false,
          error: current.error || `Replay surface load issue: ${error.message}`,
        }))
      }
    }
    loadDedicatedIntegrationSurface()
    return () => { active = false }
  }, [authSessionState.session, currentPage, activeTenantCode])

  useEffect(() => {
    let active = true
    async function loadDedicatedOrderSurface() {
      if (!authSessionState.session?.tenantCode) {
        return
      }
      if (currentPage !== 'orders') {
        return
      }
      try {
        const orderSurfaceData = await fetchOrderSurfaceData()
        if (!active) {
          return
        }
        mergeOrderSurfaceSnapshot(orderSurfaceData)
        pageStateSetter((current) => ({ ...current, loading: false, error: current.error || '' }))
      } catch (error) {
        if (!active) {
          return
        }
        pageStateSetter((current) => ({
          ...current,
          loading: false,
          error: current.error || `Orders surface load issue: ${error.message}`,
        }))
      }
    }
    loadDedicatedOrderSurface()
    return () => { active = false }
  }, [authSessionState.session, currentPage, activeTenantCode])

  useEffect(() => {
    let active = true
    async function loadScenarioHistory() {
      if (!authSessionState.session?.tenantCode) {
        if (active) {
          scenarioHistoryStateSetter({ loading: false, error: '', items: [] })
        }
        return
      }
      scenarioHistoryStateSetter((current) => ({ ...current, loading: true, error: '' }))
      try {
        const history = await fetchJson(buildScenarioHistoryPath(scenarioHistoryFilters))
        if (active) {
          scenarioHistoryStateSetter({ loading: false, error: '', items: history })
        }
      } catch (error) {
        if (active) {
          scenarioHistoryStateSetter({ loading: false, error: error.message, items: [] })
        }
      }
    }
    loadScenarioHistory()
    return () => { active = false }
  }, [scenarioHistoryFilters, activeTenantCode])

  return {
    fetchAccessAdminData,
    fetchSnapshot,
    fetchCatalogProducts,
    fetchSystemRuntime,
    refreshScenarioHistoryQuietly,
    refreshSnapshotQuietly,
    refreshSystemRuntimeQuietly,
  }
}
