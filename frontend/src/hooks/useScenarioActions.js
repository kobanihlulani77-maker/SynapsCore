export default function useScenarioActions({
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
}) {
  async function analyzeScenario() {
    setScenarioState({ loading: true, error: '', result: null })
    try {
      const payload = await fetchJson('/api/scenarios/order-impact', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          warehouseCode: scenarioForm.warehouseCode,
          items: primaryContext.requestItems,
        }),
      })
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

  return {
    analyzeScenario,
    compareScenarios,
    executeScenario,
    loadScenarioIntoPlanner,
    saveScenarioPlan,
    approveScenarioPlan,
    rejectScenarioPlan,
    acknowledgeScenarioEscalation,
  }
}
