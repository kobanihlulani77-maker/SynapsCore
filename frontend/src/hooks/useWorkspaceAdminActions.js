export default function useWorkspaceAdminActions({
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
  workspaceWarehouseCreateForm,
  setWorkspaceWarehouseCreateForm,
}) {
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

  async function saveWorkspaceSettings() {
    setAccessAdminState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const payload = await fetchJson('/api/access/admin/workspace', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tenantName: workspaceSettingsForm.tenantName.trim(),
          description: workspaceSettingsForm.description.trim(),
          version: workspaceSettingsForm.version,
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
          version: workspaceSecurityForm.version,
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
          version: draft.version,
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
          syncIntervalMinutes: draft.syncMode === 'SCHEDULED_PULL' ? Number(draft.syncIntervalMinutes || 15) : null,
          pullEndpointUrl: draft.syncMode === 'SCHEDULED_PULL' ? draft.pullEndpointUrl.trim() : null,
          validationPolicy: draft.validationPolicy,
          transformationPolicy: draft.transformationPolicy,
          allowDefaultWarehouseFallback: draft.allowDefaultWarehouseFallback,
          notes: draft.notes,
          version: draft.version,
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

  async function createWorkspaceWarehouse() {
    setAccessAdminState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const payload = await fetchJson('/api/access/admin/workspace/warehouses', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code: workspaceWarehouseCreateForm.code.trim().toUpperCase(),
          name: workspaceWarehouseCreateForm.name.trim(),
          location: workspaceWarehouseCreateForm.location.trim(),
        }),
      })
      await fetchAccessAdminData()
      setWorkspaceWarehouseCreateForm({ code: '', name: '', location: '' })
      setAccessAdminState((current) => ({ ...current, loading: false, error: '', success: `${payload.code} warehouse was created.` }))
    } catch (error) {
      setAccessAdminState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  async function changeWorkspaceWarehouseLifecycle(warehouseId, action) {
    setAccessAdminState((state) => ({ ...state, loading: true, error: '', success: '' }))
    try {
      const workspace = await fetchJson('/api/access/admin/workspace')
      const target = workspace.warehouses?.find((item) => item.id === warehouseId)
      const payload = await fetchJson(`/api/access/admin/workspace/warehouses/${warehouseId}/${action}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ version: target?.version }),
      })
      await fetchAccessAdminData()
      setAccessAdminState((state) => ({ ...state, loading: false, error: '', success: `${payload.code} warehouse is now ${payload.active ? 'active' : 'retired'}.` }))
    } catch (error) {
      setAccessAdminState((state) => ({ ...state, loading: false, error: error.message, success: '' }))
    }
  }

  const retireWorkspaceWarehouse = (warehouseId) => changeWorkspaceWarehouseLifecycle(warehouseId, 'retire')
  const reactivateWorkspaceWarehouse = (warehouseId) => changeWorkspaceWarehouseLifecycle(warehouseId, 'reactivate')

  async function saveTenantOperator() {
    setAccessAdminState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const editing = Boolean(accessOperatorForm.id)
      const warehouseScopes = parseCsvValues(accessOperatorForm.warehouseScopesText).map((scope) => scope.toUpperCase())
      const payload = await fetchJson(editing ? `/api/access/admin/operators/${accessOperatorForm.id}` : '/api/access/admin/operators', {
        method: editing ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          actorName: accessOperatorForm.actorName.trim(),
          displayName: accessOperatorForm.displayName.trim(),
          description: accessOperatorForm.description.trim(),
          active: accessOperatorForm.active,
          roles: parseCsvValues(accessOperatorForm.rolesText).map((role) => role.toUpperCase()),
          warehouseScopes,
          tenantWide: warehouseScopes.length === 0 ? accessOperatorForm.tenantWide : false,
          ...(editing ? { version: accessOperatorForm.version } : {}),
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
              version: accessUserForm.version,
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

  return {
    resetAccessOperatorEditor,
    resetAccessUserEditor,
    resolveDefaultManagedOperator,
    saveWorkspaceSettings,
    saveWorkspaceSecuritySettings,
    saveWorkspaceWarehouse,
    saveWorkspaceConnectorSupport,
    createWorkspaceWarehouse,
    retireWorkspaceWarehouse,
    reactivateWorkspaceWarehouse,
    saveTenantOperator,
    saveTenantUser,
    resetTenantUserPassword,
  }
}
