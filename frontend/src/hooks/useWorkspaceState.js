import { useRef, useState } from 'react'
import {
  buildProductOptions,
  buildWarehouseOptions,
  createDefaultAccessOperatorForm,
  createDefaultAccessUserForm,
  createDefaultCatalogForm,
  createDefaultWorkspaceSecurityForm,
  createDefaultWorkspaceSettingsForm,
  createScenarioForm,
  createScenarioLine,
  defaultScenarioHistoryFilters,
  defaultScenarioRequester,
  defaultScenarioReviewOwner,
  defaultTenantOnboardingForm,
  emptyRequestState,
  emptySnapshot,
} from '../config/workspaceModel'

export default function useWorkspaceState({ initialPage }) {
  const [snapshot, setSnapshot] = useState(emptySnapshot)
  const [currentPage, setCurrentPage] = useState(initialPage)
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

  const mergeSnapshot = (partial) => setSnapshot((current) => ({ ...current, ...partial, generatedAt: new Date().toISOString() }))
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
      return {
        ...item,
        selectedProduct,
        quantityNumber,
        unitPriceNumber,
        valid: Boolean(item.productSku)
          && Number.isFinite(quantityNumber)
          && quantityNumber >= 1
          && Number.isFinite(unitPriceNumber)
          && unitPriceNumber > 0,
      }
    })
    return {
      productOptions,
      lines,
      requestItems: lines.map((item) => ({
        productSku: item.productSku,
        quantity: item.quantityNumber,
        unitPrice: item.unitPriceNumber,
      })),
      inputValid: Boolean(form.warehouseCode) && lines.length > 0 && lines.every((item) => item.valid),
    }
  }

  const updateScenarioField = (setter, field, value) => setter((current) => (
    field === 'warehouseCode'
      ? normalizeScenarioForm({ ...current, warehouseCode: value }, snapshot.inventory)
      : { ...current, [field]: value }
  ))
  const updateScenarioLine = (setter, lineId, field, value) => setter((current) => ({
    ...current,
    items: current.items.map((item) => item.id === lineId ? { ...item, [field]: value } : item),
  }))
  const addScenarioLine = (setter, warehouseCode) => {
    const fallbackSku = buildProductOptions(snapshot.inventory, warehouseCode)[0]?.sku || ''
    setter((current) => ({ ...current, items: [...current.items, createScenarioLine(fallbackSku)] }))
  }
  const removeScenarioLine = (setter, lineId) => setter((current) => (
    current.items.length === 1 ? current : { ...current, items: current.items.filter((item) => item.id !== lineId) }
  ))

  return {
    snapshot,
    setSnapshot,
    currentPage,
    setCurrentPage,
    workspaceSearch,
    setWorkspaceSearch,
    clockTick,
    setClockTick,
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
    setConnectionState,
    pageState,
    setPageState,
    actionState,
    setActionState,
    systemRuntimeState,
    setSystemRuntimeState,
    tenantDirectoryState,
    setTenantDirectoryState,
    tenantOnboardingForm,
    setTenantOnboardingForm,
    tenantOnboardingState,
    setTenantOnboardingState,
    accessAdminState,
    setAccessAdminState,
    workspaceSettingsForm,
    setWorkspaceSettingsForm,
    workspaceSecurityForm,
    setWorkspaceSecurityForm,
    workspaceWarehouseDrafts,
    setWorkspaceWarehouseDrafts,
    workspaceConnectorDrafts,
    setWorkspaceConnectorDrafts,
    accessOperatorForm,
    setAccessOperatorForm,
    accessUserForm,
    setAccessUserForm,
    catalogState,
    setCatalogState,
    catalogForm,
    setCatalogForm,
    operatorDirectoryState,
    setOperatorDirectoryState,
    integrationConnectorState,
    setIntegrationConnectorState,
    integrationReplayState,
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
    scenarioReviewNote,
    setScenarioReviewNote,
    scenarioRevisionSource,
    setScenarioRevisionSource,
    scenarioState,
    setScenarioState,
    comparisonState,
    setComparisonState,
    scenarioExecutionState,
    setScenarioExecutionState,
    scenarioLoadState,
    setScenarioLoadState,
    scenarioSaveState,
    setScenarioSaveState,
    scenarioApprovalState,
    setScenarioApprovalState,
    scenarioRejectionState,
    setScenarioRejectionState,
    scenarioEscalationAckState,
    setScenarioEscalationAckState,
    scenarioHistoryFilters,
    setScenarioHistoryFilters,
    scenarioHistoryState,
    setScenarioHistoryState,
    searchInputRef,
    mergeSnapshot,
    resetSignedInWorkspace,
    normalizeScenarioForm,
    buildScenarioContext,
    updateScenarioField,
    updateScenarioLine,
    addScenarioLine,
    removeScenarioLine,
  }
}
