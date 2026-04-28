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
  scenarioForm,
  scenarioRequestedBy,
  scenarioRequestedBySetter,
  scenarioReviewOwner,
  scenarioReviewOwnerSetter,
  scenarioActorRole,
  scenarioActorRoleSetter,
  scenarioActorRoles,
  integrationActorRole,
  integrationActorRoleSetter,
  integrationActorRoles,
  snapshotInventory,
  setScenarioForm,
  setComparisonForm,
  normalizeScenarioForm,
  scenarioHistoryFilters,
}) {
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
    })
    workspaceSecurityFormSetter({
      passwordRotationDays: String(workspace.securitySettings?.passwordRotationDays || 90),
      sessionTimeoutMinutes: String(workspace.securitySettings?.sessionTimeoutMinutes || 480),
      invalidateOtherSessions: false,
    })
    workspaceWarehouseDraftsSetter(buildWorkspaceWarehouseDrafts(workspace))
    workspaceConnectorDraftsSetter(buildWorkspaceConnectorDrafts(workspace))
    accessUserFormSetter((current) => ({
      ...current,
      operatorActorName: operators.some((operator) => operator.actorName === current.operatorActorName)
        ? current.operatorActorName
        : defaultOperatorActorName,
    }))
  }

  async function fetchSnapshot() {
    const nextSnapshot = await fetchJson('/api/dashboard/snapshot')
    snapshotSetter({
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
    pageStateSetter({ loading: false, error: '' })
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
      try {
        const tenants = await fetchJson('/api/access/tenants')
        if (active) {
          tenantDirectoryStateSetter({ loading: false, error: '', items: tenants })
          authSessionStateSetter((current) => ({
            ...current,
            tenantCode: current.session?.tenantCode || current.tenantCode || tenants[0]?.code || '',
          }))
        }
      } catch (error) {
        if (active) {
          tenantDirectoryStateSetter({ loading: false, error: error.message, items: [] })
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

    const availableOperators = operatorDirectoryState.items.filter((operator) => hasWarehouseScope(operator.warehouseScopes, scenarioForm.warehouseCode))
    const reviewOwnerOptions = availableOperators.filter((operator) => operator.roles.includes('REVIEW_OWNER'))

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
  ])

  useEffect(() => {
    if (!authSessionState.session) return

    const sessionRoles = authSessionState.session.roles ?? []
    scenarioRequestedBySetter(authSessionState.session.actorName)

    if (!sessionRoles.includes(scenarioActorRole)) {
      const fallbackScenarioRole = scenarioActorRoles.find((role) => sessionRoles.includes(role))
      if (fallbackScenarioRole) {
        scenarioActorRoleSetter(fallbackScenarioRole)
      }
    }
    if (!sessionRoles.includes(integrationActorRole)) {
      const fallbackIntegrationRole = integrationActorRoles.find((role) => sessionRoles.includes(role))
      if (fallbackIntegrationRole) {
        integrationActorRoleSetter(fallbackIntegrationRole)
      }
    }
  }, [authSessionState.session, integrationActorRole, scenarioActorRole])

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
