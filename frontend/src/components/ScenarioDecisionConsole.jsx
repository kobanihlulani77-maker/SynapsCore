export default function ScenarioDecisionConsole({ scenario, title, emptyMessage, context }) {
  const {
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
  } = context

  if (!scenario) {
    return (
      <article className="stack-card section-card">
        <div className="stack-title-row">
          <strong>{title}</strong>
          <span className="scenario-type-tag">Waiting</span>
        </div>
        <div className="empty-state">{emptyMessage}</div>
      </article>
    )
  }

  const approvalRole = getScenarioApprovalRole(scenario)
  const rejectionRole = getScenarioRejectionRole(scenario)
  const approvalActionLabel = scenario.approvalPolicy === 'ESCALATED' && scenario.approvalStage === 'PENDING_REVIEW'
    ? 'Owner Review'
    : scenario.approvalPolicy === 'ESCALATED'
      ? 'Final Approve'
      : 'Approve Plan'
  const approvalNoteRequired = scenario.approvalPolicy === 'ESCALATED' || scenario.approvalStage === 'PENDING_FINAL_APPROVAL'
  const canLoadScenario = Boolean(scenario.loadable)
  const canApproveScenario = scenario.type === 'SAVED_PLAN' && scenario.approvalStatus === 'PENDING_APPROVAL'
  const canRejectScenario = scenario.type === 'SAVED_PLAN' && scenario.approvalStatus !== 'REJECTED'
  const canExecuteScenario = Boolean(scenario.executable)
  const canAcknowledgeEscalation = Boolean(scenario.slaEscalated && !scenario.slaAcknowledged)
  const approvalDisabled = scenarioApprovalState.loadingId === scenario.id
    || !signedInSession
    || (approvalNoteRequired && !scenarioReviewNote.trim())
    || scenarioActorRole !== approvalRole
    || !signedInRoles.includes(approvalRole)
    || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)
  const rejectionDisabled = scenarioRejectionState.loadingId === scenario.id
    || !signedInSession
    || !scenarioReviewNote.trim()
    || scenarioActorRole !== rejectionRole
    || !signedInRoles.includes(rejectionRole)
    || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)
  const escalationDisabled = scenarioEscalationAckState.loadingId === scenario.id
    || !signedInSession
    || !scenarioReviewNote.trim()
    || scenarioActorRole !== 'ESCALATION_OWNER'
    || !signedInRoles.includes('ESCALATION_OWNER')
    || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)

  return (
    <article className="stack-card section-card">
      <div className="stack-title-row">
        <strong>{title}</strong>
        <span className="scenario-type-tag">{formatCodeLabel(scenario.approvalStatus || scenario.type)}</span>
      </div>
      <div className="signal-list">
        <div className="signal-list-item">
          <strong>{scenario.title}</strong>
          <p>{scenario.summary}</p>
          <p className="muted-text">
            {scenario.warehouseCode ? `${scenario.warehouseCode} | ` : ''}
            {formatCodeLabel(scenario.type)}
            {scenario.reviewPriority ? ` | ${formatCodeLabel(scenario.reviewPriority)} priority` : ''}
          </p>
          <p className="muted-text">
            {scenario.requestedBy ? `Requested by ${scenario.requestedBy}` : 'Requester pending'}
            {scenario.reviewOwner ? ` | Review owner ${scenario.reviewOwner}` : ''}
            {scenario.finalApprovalOwner ? ` | Final approver ${scenario.finalApprovalOwner}` : ''}
          </p>
          <p className="muted-text">
            Approval needs {formatCodeLabel(approvalRole)} | Rejection needs {formatCodeLabel(rejectionRole)}
            {scenario.approvalDueAt ? ` | Due ${formatTimestamp(scenario.approvalDueAt)}` : ''}
          </p>
        </div>
      </div>
      <div className="session-control-row">
        <label className="field planner-name-field">
          <span>Acting As</span>
          <select value={scenarioActorRole} onChange={(event) => setScenarioActorRole(event.target.value)}>
            {scenarioActorRoles.map((role) => <option key={role} value={role}>{formatCodeLabel(role)}</option>)}
          </select>
        </label>
        <label className="field planner-name-field">
          <span>Decision Note</span>
          <input
            type="text"
            maxLength="240"
            placeholder={canAcknowledgeEscalation ? 'Required to acknowledge the escalation' : approvalNoteRequired ? 'Recommended for final approval; required for rejection' : 'Required for rejection'}
            value={scenarioReviewNote}
            onChange={(event) => setScenarioReviewNote(event.target.value)}
          />
        </label>
      </div>
      {scenarioLoadState.error ? <p className="error-text">{scenarioLoadState.error}</p> : null}
      {scenarioLoadState.success ? <p className="success-text">{scenarioLoadState.success}</p> : null}
      {scenarioApprovalState.error ? <p className="error-text">{scenarioApprovalState.error}</p> : null}
      {scenarioApprovalState.success ? <p className="success-text">{scenarioApprovalState.success}</p> : null}
      {scenarioRejectionState.error ? <p className="error-text">{scenarioRejectionState.error}</p> : null}
      {scenarioRejectionState.success ? <p className="success-text">{scenarioRejectionState.success}</p> : null}
      {scenarioExecutionState.error ? <p className="error-text">{scenarioExecutionState.error}</p> : null}
      {scenarioExecutionState.success ? <p className="success-text">{scenarioExecutionState.success}</p> : null}
      {scenarioEscalationAckState.error ? <p className="error-text">{scenarioEscalationAckState.error}</p> : null}
      {scenarioEscalationAckState.success ? <p className="success-text">{scenarioEscalationAckState.success}</p> : null}
      <div className="history-action-row">
        {canLoadScenario ? (
          <button className="ghost-button" onClick={() => loadScenarioIntoPlanner(scenario.id)} disabled={scenarioLoadState.loadingId === scenario.id} type="button">
            {scenarioLoadState.loadingId === scenario.id ? 'Loading...' : 'Load Into Planner'}
          </button>
        ) : null}
        {canApproveScenario ? (
          <button className="approve-button" onClick={() => approveScenarioPlan(scenario.id)} disabled={approvalDisabled} type="button">
            {scenarioApprovalState.loadingId === scenario.id ? 'Approving...' : approvalActionLabel}
          </button>
        ) : null}
        {canRejectScenario ? (
          <button className="reject-button" onClick={() => rejectScenarioPlan(scenario.id)} disabled={rejectionDisabled} type="button">
            {scenarioRejectionState.loadingId === scenario.id ? 'Rejecting...' : 'Reject Plan'}
          </button>
        ) : null}
        {canExecuteScenario ? (
          <button className="secondary-button" onClick={() => executeScenario(scenario.id)} disabled={scenarioExecutionState.loadingId === scenario.id} type="button">
            {scenarioExecutionState.loadingId === scenario.id ? 'Executing...' : 'Execute Scenario'}
          </button>
        ) : null}
        {canAcknowledgeEscalation ? (
          <button className="approve-button" onClick={() => acknowledgeScenarioEscalation(scenario.id)} disabled={escalationDisabled} type="button">
            {scenarioEscalationAckState.loadingId === scenario.id ? 'Acknowledging...' : 'Acknowledge Escalation'}
          </button>
        ) : null}
      </div>
      {!canLoadScenario && !canApproveScenario && !canRejectScenario && !canExecuteScenario && !canAcknowledgeEscalation ? (
        <p className="muted-text">This scenario is visible for traceability and comparison, but it does not need another live action right now.</p>
      ) : null}
    </article>
  )
}
