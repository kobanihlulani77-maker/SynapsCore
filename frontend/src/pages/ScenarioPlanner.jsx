import ScenarioEditor from '../components/ScenarioEditor'

export default function ScenarioPlannerPage({ context }) {
  const {
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
  } = context

  if (!isAuthenticated || !isScenariosPage) {
    return null
  }
  const plannerLifecycleLabel = comparisonState.result
    ? 'Compared'
    : scenarioState.result
      ? 'Previewed'
      : scenarioRevisionSource
        ? 'Revision'
        : 'Draft'
  const plannerNextStep = !primaryContext.inputValid
    ? 'Complete Scenario A inputs before preview or save.'
    : scenarioState.result
      ? 'Review preview evidence, compare alternatives if needed, then save for governance.'
      : 'Preview Scenario A before the plan moves toward approval.'
  const previewBoundary = scenarioState.result
    ? 'PREVIEW ONLY — NOT EXECUTABLE. Review projected impact, then save the plan if it should enter governance.'
    : 'PREVIEW is analysis only. It never grants execution authority or bypasses approval.'
  const saveBlockedReason = !primaryContext.inputValid
    ? 'Scenario A inputs are incomplete.'
    : !scenarioPlanName.trim()
      ? 'Plan name is required before saving.'
      : !scenarioRequestedBy || !scenarioReviewOwner
        ? 'Requester and review owner must be selected.'
        : !signedInSession
          ? 'Sign in before saving a governed plan.'
          : !hasWarehouseScope(signedInWarehouseScopes, scenarioForm.warehouseCode)
            ? 'This operator does not have scope for the selected warehouse.'
            : 'Ready to save for approval routing.'
  const compareBlockedReason = !primaryContext.inputValid || !alternativeContext.inputValid
    ? 'Both Scenario A and Scenario B need valid inputs before comparison.'
    : 'Ready to compare Scenario A against Scenario B.'

  return (
    <section className="content-grid planner-grid" id="planner">
      <article className="panel panel-wide">
        <div className="panel-header">
          <div><p className="panel-kicker">Scenario planning</p><h2>Compare proposed order plans</h2></div>
          <span className="panel-badge planner-badge">{comparisonState.result ? 'Comparison ready' : scenarioState.result ? 'Preview ready' : 'Planner'}</span>
        </div>
        <div className="workflow-decision-hero scenario-planner-hero">
          <div className="workflow-decision-copy">
            <p className="panel-kicker">Planning sequence</p>
            <div className="runtime-decision-title">
              <span className="runtime-decision-badge status-partial">{plannerLifecycleLabel}</span>
              <h2>{scenarioPlanName.trim() || 'Define the scenario before governance.'}</h2>
            </div>
            <p>{plannerNextStep}</p>
            <div className="workflow-action-card scenario-preview-boundary">
              <span>Execution boundary</span>
              <strong>PREVIEW IS NOT EXECUTABLE</strong>
              <p>{previewBoundary}</p>
            </div>
            {scenarioRevisionSource ? (
              <div className="revision-banner">
                <span>Revision mode: saving will create revision {scenarioRevisionSource.revisionNumber} of {scenarioRevisionSource.title}.</span>
                <button className="ghost-button" onClick={() => setScenarioRevisionSource(null)} type="button">Exit Revision Mode</button>
              </div>
            ) : null}
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Step 1 Identity</span>
              <span className="workspace-meta-pill">Step 2 Inputs</span>
              <span className="workspace-meta-pill">Step 3 Preview</span>
              <span className="workspace-meta-pill">Step 4 Compare / Save</span>
            </div>
          </div>
          <div className="workflow-action-console scenario-planner-actions">
            <div className="workflow-action-card">
              <span>Next governance step</span>
              <strong>{saveBlockedReason}</strong>
              <p>{compareBlockedReason}</p>
            </div>
            <button className="secondary-button" onClick={analyzeScenario} disabled={scenarioState.loading || !primaryContext.inputValid}>{scenarioState.loading ? 'Analyzing...' : 'Preview Scenario A'}</button>
            <button className="compare-button" onClick={compareScenarios} disabled={comparisonState.loading || !primaryContext.inputValid || !alternativeContext.inputValid}>{comparisonState.loading ? 'Comparing...' : 'Compare A vs B'}</button>
            <button className="ghost-button" onClick={saveScenarioPlan} disabled={scenarioSaveState.loading || !primaryContext.inputValid || !scenarioPlanName.trim() || !scenarioRequestedBy || !scenarioReviewOwner || !signedInSession || !hasWarehouseScope(signedInWarehouseScopes, scenarioForm.warehouseCode)}>{scenarioSaveState.loading ? 'Saving...' : 'Save Scenario A'}</button>
          </div>
        </div>
        <div className="planner-action-bar scenario-planner-workflow">
          <div className="scenario-filter-rail">
            <div className="stack-title-row">
              <strong>Review queues</strong>
              <span className="scenario-type-tag">Secondary</span>
            </div>
            <div className="history-quick-actions">
              <button
                className="ghost-button"
                onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, requestedBy: scenarioRequestedBy.trim() }))}
                disabled={!scenarioRequestedBy.trim()}
                type="button"
              >
                My Requests
              </button>
              <button
                className="ghost-button"
                onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', reviewOwner: scenarioReviewOwner.trim() }))}
                disabled={!scenarioReviewOwner.trim()}
                type="button"
              >
                My Review Queue
              </button>
              <button
                className="ghost-button"
                onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', reviewOwner: scenarioReviewOwner.trim(), minimumReviewPriority: 'HIGH' }))}
                disabled={!scenarioReviewOwner.trim()}
                type="button"
              >
                High-Risk Queue
              </button>
              <button
                className="ghost-button"
                onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', reviewOwner: scenarioReviewOwner.trim(), approvalPolicy: 'ESCALATED' }))}
                disabled={!scenarioReviewOwner.trim()}
                type="button"
              >
                Escalated Queue
              </button>
              <button
                className="ghost-button"
                onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', approvalPolicy: 'ESCALATED', approvalStage: 'PENDING_FINAL_APPROVAL' }))}
                type="button"
              >
                Final Approval Queue
              </button>
              <button
                className="ghost-button"
                onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', approvalPolicy: 'ESCALATED', approvalStage: 'PENDING_FINAL_APPROVAL', finalApprovalOwner: signedInSession?.actorName || '' }))}
                disabled={!signedInSession}
                type="button"
              >
                My Final Approvals
              </button>
              <button
                className="ghost-button"
                onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', overdueOnly: true }))}
                type="button"
              >
                Overdue Queue
              </button>
              <button
                className="ghost-button"
                onClick={() => setScenarioHistoryFilters((current) => ({ ...defaultScenarioHistoryFilters, approvalStatus: 'PENDING_APPROVAL', slaEscalatedOnly: true }))}
                type="button"
              >
                SLA Escalated Queue
              </button>
            </div>
          </div>
          <div className="planner-actions scenario-identity-panel">
            <div className="stack-title-row">
              <strong>Scenario identity and governance</strong>
              <span className="scenario-type-tag">{plannerLifecycleLabel}</span>
            </div>
            <label className="field planner-name-field">
              <span>Plan Name</span>
              <input
                type="text"
                maxLength="120"
                placeholder="North restock option"
                value={scenarioPlanName}
                onChange={(event) => setScenarioPlanName(event.target.value)}
              />
            </label>
            <label className="field planner-name-field">
              <span>Requested By</span>
              <input
                type="text"
                value={signedInSession?.actorName || scenarioRequestedBy || (requesterOperators.length ? 'Sign in to identify requester' : 'Loading operators...')}
                readOnly
              />
            </label>
            <label className="field planner-name-field">
              <span>Signed In As</span>
              <input type="text" value={signedInSession ? signedInSession.displayName : 'Sign in to review or approve'} readOnly />
            </label>
            <label className="field planner-name-field">
              <span>Review Owner</span>
              <select value={scenarioReviewOwner} onChange={(event) => setScenarioReviewOwner(event.target.value)} disabled={!reviewOwnerOperators.length}>
                {reviewOwnerOperators.length
                  ? reviewOwnerOperators.map((operator) => <option key={operator.actorName} value={operator.actorName}>{operator.displayName}</option>)
                  : <option value="">No review owners configured</option>}
              </select>
            </label>
            <label className="field planner-name-field">
              <span>Review Note</span>
              <input
                type="text"
                maxLength="240"
                placeholder="Required when rejecting a saved plan"
                value={scenarioReviewNote}
                onChange={(event) => setScenarioReviewNote(event.target.value)}
              />
            </label>
            <p className="muted-text planner-note">Review and approval actions use the signed-in operator session. Warehouse-scoped operators only appear for the selected warehouse, and review actions are blocked outside their assigned lanes.</p>
            {operatorDirectoryState.error ? <p className="error-text">{operatorDirectoryState.error}</p> : null}
          </div>
        </div>
        <div className="planner-compare-grid">
          <ScenarioEditor
            title="Scenario A"
            form={scenarioForm}
            setter={setScenarioForm}
            context={primaryContext}
            warehouseOptions={warehouseOptions}
            addScenarioLine={addScenarioLine}
            updateScenarioField={updateScenarioField}
            updateScenarioLine={updateScenarioLine}
            removeScenarioLine={removeScenarioLine}
          />
          <ScenarioEditor
            title="Scenario B"
            form={comparisonForm}
            setter={setComparisonForm}
            context={alternativeContext}
            warehouseOptions={warehouseOptions}
            addScenarioLine={addScenarioLine}
            updateScenarioField={updateScenarioField}
            updateScenarioLine={updateScenarioLine}
            removeScenarioLine={removeScenarioLine}
          />
        </div>
        {scenarioState.error ? <p className="error-text">{scenarioState.error}</p> : null}
        {comparisonState.error ? <p className="error-text">{comparisonState.error}</p> : null}
        {scenarioExecutionState.error ? <p className="error-text">{scenarioExecutionState.error}</p> : null}
        {scenarioExecutionState.success ? <p className="success-text">{scenarioExecutionState.success}</p> : null}
        {scenarioLoadState.error ? <p className="error-text">{scenarioLoadState.error}</p> : null}
        {scenarioLoadState.success ? <p className="success-text">{scenarioLoadState.success}</p> : null}
        {scenarioSaveState.error ? <p className="error-text">{scenarioSaveState.error}</p> : null}
        {scenarioSaveState.success ? <p className="success-text">{scenarioSaveState.success}</p> : null}
        {scenarioApprovalState.error ? <p className="error-text">{scenarioApprovalState.error}</p> : null}
        {scenarioApprovalState.success ? <p className="success-text">{scenarioApprovalState.success}</p> : null}
        {scenarioRejectionState.error ? <p className="error-text">{scenarioRejectionState.error}</p> : null}
        {scenarioRejectionState.success ? <p className="success-text">{scenarioRejectionState.success}</p> : null}
        {comparisonState.result ? (
          <div className="comparison-shell">
            <div className="comparison-banner">
              <strong>Recommended option: {comparisonState.result.summary.recommendedOption}</strong>
              <p>{comparisonState.result.summary.rationale}</p>
            </div>
            <div className="comparison-grid">
              <article className="comparison-card">
                <div className="panel-header">
                  <div><p className="panel-kicker">Scenario A</p><h2>{comparisonState.result.primaryLabel}</h2></div>
                  <span className="panel-badge compare-score-badge">Score {comparisonState.result.summary.primaryRiskScore}</span>
                </div>
                <div className="scenario-grid comparison-stats">
                  <article className="scenario-stat"><span className="summary-label">Low Stock</span><strong>{comparisonPrimarySummary.lowStockItems}</strong></article>
                  <article className="scenario-stat"><span className="summary-label">Critical</span><strong>{comparisonPrimarySummary.criticalItems}</strong></article>
                  <article className="scenario-stat"><span className="summary-label">Alerts</span><strong>{comparisonPrimarySummary.alertCount}</strong></article>
                  <article className="scenario-stat"><span className="summary-label">Actions</span><strong>{comparisonPrimarySummary.recommendationCount}</strong></article>
                </div>
              </article>
              <article className="comparison-card">
                <div className="panel-header">
                  <div><p className="panel-kicker">Scenario B</p><h2>{comparisonState.result.alternativeLabel}</h2></div>
                  <span className="panel-badge compare-score-badge">Score {comparisonState.result.summary.alternativeRiskScore}</span>
                </div>
                <div className="scenario-grid comparison-stats">
                  <article className="scenario-stat"><span className="summary-label">Low Stock</span><strong>{comparisonAlternativeSummary.lowStockItems}</strong></article>
                  <article className="scenario-stat"><span className="summary-label">Critical</span><strong>{comparisonAlternativeSummary.criticalItems}</strong></article>
                  <article className="scenario-stat"><span className="summary-label">Alerts</span><strong>{comparisonAlternativeSummary.alertCount}</strong></article>
                  <article className="scenario-stat"><span className="summary-label">Actions</span><strong>{comparisonAlternativeSummary.recommendationCount}</strong></article>
                </div>
              </article>
            </div>
          </div>
        ) : null}
        {scenarioState.result ? (
          <>
            <div className="scenario-grid">
              <article className="scenario-stat"><span className="summary-label">Projected Order Value</span><strong>{currency.format(scenarioState.result.projectedOrderValue)}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Units Impacted</span><strong>{scenarioState.result.totalUnits}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Projected Alerts</span><strong>{scenarioState.result.projectedAlerts.length}</strong></article>
              <article className="scenario-stat"><span className="summary-label">Projected Actions</span><strong>{scenarioState.result.projectedRecommendations.length}</strong></article>
            </div>
            <div className="content-grid planner-results">
              <article className="panel planner-subpanel">
                <div className="panel-header"><div><p className="panel-kicker">Projected inventory</p><h2>Scenario A posture</h2></div></div>
                <div className="stack-list">
                  {scenarioState.result.projectedInventory.map((item) => (
                    <div key={`${item.id}-${item.productSku}`} className="stack-card">
                      <div className="stack-title-row"><strong>{item.productName}</strong><span className={`risk-chip risk-${item.riskLevel}`}>{item.riskLevel}</span></div>
                      <p>{item.warehouseName} | {item.quantityAvailable} units after impact</p>
                      <p className="muted-text">Threshold {item.reorderThreshold} | Stockout {formatRelativeHours(item.hoursToStockout)}</p>
                    </div>
                  ))}
                </div>
              </article>
              <article className="panel planner-subpanel">
                <div className="panel-header"><div><p className="panel-kicker">Projected response</p><h2>Scenario A alerts and actions</h2></div></div>
                <div className="stack-list">
                  {scenarioState.result.projectedAlerts.map((alert) => (
                    <div key={alert.title} className="stack-card">
                      <div className="stack-title-row"><strong>{alert.title}</strong><span className={`severity-tag severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span></div>
                      <p>{alert.description}</p>
                      <p className="muted-text">{alert.impactSummary}</p>
                    </div>
                  ))}
                  {scenarioState.result.projectedRecommendations.map((recommendation) => (
                    <div key={recommendation.title} className="stack-card">
                      <div className="stack-title-row"><strong>{recommendation.title}</strong><span className={`priority-tag priority-${recommendation.priority.toLowerCase()}`}>{recommendation.priority}</span></div>
                      <p>{recommendation.description}</p>
                    </div>
                  ))}
                  {!scenarioState.result.projectedAlerts.length && !scenarioState.result.projectedRecommendations.length ? <div className="empty-state">This scenario stays within the current operating buffer.</div> : null}
                </div>
              </article>
            </div>
          </>
        ) : <div className="empty-state planner-empty">Build one or two proposed order plans to preview risk and compare the safer option before committing activity.</div>}
      </article>
    </section>
  )
}
