export default function ScenarioDecisionConsole({ scenario, title, emptyMessage, context, surface = 'governance' }) {
  const {
    getScenarioApprovalRole,
    getScenarioRejectionRole,
    scenarioApprovalState,
    scenarioRejectionState,
    scenarioEscalationAckState,
    scenarioLoadState,
    signedInSession,
    scenarioReviewNote,
    signedInRoles,
    signedInWarehouseScopes,
    hasWarehouseScope,
    formatCodeLabel,
    formatTimestamp,
    approveScenarioPlan,
    rejectScenarioPlan,
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
  const canRejectScenario = scenario.type === 'SAVED_PLAN' && scenario.approvalStatus === 'PENDING_APPROVAL'
  const canAcknowledgeEscalation = Boolean(scenario.slaEscalated && !scenario.slaAcknowledged)
  const assignedApprovalOwner = scenario.approvalStage === 'PENDING_FINAL_APPROVAL'
    ? scenario.finalApprovalOwner
    : scenario.reviewOwner
  const approvalAssignmentMatches = !assignedApprovalOwner
    || !signedInSession?.actorName
    || assignedApprovalOwner.toLowerCase() === signedInSession.actorName.toLowerCase()
  const actionStateSummary = canApproveScenario
    ? !approvalAssignmentMatches
      ? `Not assigned to this operator. ${formatCodeLabel(approvalRole)} work is assigned to ${assignedApprovalOwner}.`
      : `${approvalActionLabel} needs ${formatCodeLabel(approvalRole)} authority${approvalNoteRequired ? ' and a decision note' : ''}.`
      : scenario.type === 'PREVIEW'
        ? 'PREVIEW is analysis only and cannot be executed. Save a governed plan if the proposed change should enter approval.'
      : canRejectScenario
        ? `Reject Plan needs ${formatCodeLabel(rejectionRole)} authority and a decision note.`
        : canLoadScenario
          ? 'Load Into Planner opens this scenario for revision or comparison.'
          : canAcknowledgeEscalation
            ? 'Acknowledge Escalation records ownership of the overdue escalation.'
            : 'No live action is required for this scenario right now.'
  const approvalDisabled = scenarioApprovalState.loadingId === scenario.id
    || !signedInSession
    || (approvalNoteRequired && !scenarioReviewNote.trim())
    || !signedInRoles.includes(approvalRole)
    || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)
    || !approvalAssignmentMatches
  const rejectionDisabled = scenarioRejectionState.loadingId === scenario.id
    || !signedInSession
    || !scenarioReviewNote.trim()
    || !signedInRoles.includes(rejectionRole)
    || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)
    || !approvalAssignmentMatches
  const escalationDisabled = scenarioEscalationAckState.loadingId === scenario.id
    || !signedInSession
    || !scenarioReviewNote.trim()
    || !signedInRoles.includes('ESCALATION_OWNER')
    || !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)
  const approvalBlocker = !canApproveScenario
    ? ''
    : !signedInSession
      ? 'Sign in before approving.'
      : !signedInRoles.includes(approvalRole)
        ? `The signed-in operator does not have ${formatCodeLabel(approvalRole)} authority.`
      : !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)
        ? 'This operator does not have the required warehouse scope.'
        : !approvalAssignmentMatches
          ? `This decision is assigned to ${assignedApprovalOwner}. Do not act from this session.`
        : approvalNoteRequired && !scenarioReviewNote.trim()
            ? 'Add a decision note before approval.'
            : 'Approval action is available.'
  const rejectionBlocker = !canRejectScenario
    ? ''
    : !signedInSession
      ? 'Sign in before rejecting.'
      : !signedInRoles.includes(rejectionRole)
        ? `The signed-in operator does not have ${formatCodeLabel(rejectionRole)} authority.`
        : !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)
          ? 'This operator does not have the required warehouse scope.'
          : !approvalAssignmentMatches
            ? `This decision is assigned to ${assignedApprovalOwner}. Do not act from this session.`
          : !scenarioReviewNote.trim()
            ? 'Add a decision note before rejection.'
            : 'Rejection action is available.'
  const escalationBlocker = !canAcknowledgeEscalation
    ? ''
    : !signedInSession
      ? 'Sign in before acknowledging escalation.'
      : !signedInRoles.includes('ESCALATION_OWNER')
        ? 'The signed-in operator does not have Escalation Owner authority.'
        : !hasWarehouseScope(signedInWarehouseScopes, scenario.warehouseCode)
          ? 'This operator does not have the required warehouse scope.'
          : !scenarioReviewNote.trim()
            ? 'Add a decision note before acknowledging.'
            : 'Escalation acknowledgement is available.'

  return (
    <article className="stack-card section-card scenario-decision-console">
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
      <div className="workflow-action-card scenario-action-meaning">
        <span>Governance consequence</span>
        <strong>{actionStateSummary}</strong>
        <p>
          {scenario.approvalStatus === 'PENDING_APPROVAL'
            ? 'Approval moves the plan forward under its policy; rejection prevents this saved plan version from proceeding.'
            : scenario.type === 'PREVIEW'
              ? 'Preview output is evidence for analysis only. It does not represent an operational change.'
              : scenario.approvalStatus === 'APPROVED'
                ? 'Governance is complete. The approved decision is ready for external operational follow-through.'
                : 'This surface keeps the decision evidence visible even when no action is currently available.'}
        </p>
      </div>
      {surface === 'escalation' ? (
        <p className="muted-text">Escalation acknowledgment records ownership of the escalation only. Approval and rejection remain governed decisions; external operational action stays outside SynapseCore.</p>
      ) : null}
      <div className="session-control-row">
        <label className="field planner-name-field">
          <span>Signed In As</span>
          <input type="text" value={signedInSession ? `${signedInSession.displayName} (${signedInRoles.map(formatCodeLabel).join(', ')})` : 'Sign in to use governed actions'} readOnly />
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
      {scenarioEscalationAckState.error ? <p className="error-text">{scenarioEscalationAckState.error}</p> : null}
      {scenarioEscalationAckState.success ? <p className="success-text">{scenarioEscalationAckState.success}</p> : null}
      <div className="history-action-row scenario-action-row" aria-label="Scenario action controls">
        {canLoadScenario ? (
          <button className="ghost-button" onClick={() => loadScenarioIntoPlanner(scenario.id)} disabled={scenarioLoadState.loadingId === scenario.id} type="button">
            {scenarioLoadState.loadingId === scenario.id ? 'Loading...' : 'Load Into Planner'}
          </button>
        ) : null}
        {canApproveScenario ? (
          <button className="approve-button" onClick={() => approveScenarioPlan(scenario.id, approvalRole)} disabled={approvalDisabled} type="button">
            {scenarioApprovalState.loadingId === scenario.id ? 'Approving...' : approvalActionLabel}
          </button>
        ) : null}
        {canRejectScenario ? (
          <button className="reject-button" onClick={() => rejectScenarioPlan(scenario.id, rejectionRole)} disabled={rejectionDisabled} type="button">
            {scenarioRejectionState.loadingId === scenario.id ? 'Rejecting...' : 'Reject Plan'}
          </button>
        ) : null}
        {canAcknowledgeEscalation ? (
          <button className="approve-button" onClick={() => acknowledgeScenarioEscalation(scenario.id)} disabled={escalationDisabled} type="button">
            {scenarioEscalationAckState.loadingId === scenario.id ? 'Acknowledging...' : 'Acknowledge Escalation'}
          </button>
        ) : null}
      </div>
      <div className="workflow-blocker-list" aria-label="Scenario action availability">
        {approvalBlocker ? <p className={approvalDisabled ? 'muted-text' : 'success-text'}>{approvalBlocker}</p> : null}
        {rejectionBlocker ? <p className={rejectionDisabled ? 'muted-text' : 'success-text'}>{rejectionBlocker}</p> : null}
        {escalationBlocker ? <p className={escalationDisabled ? 'muted-text' : 'success-text'}>{escalationBlocker}</p> : null}
      </div>
      {!canLoadScenario && !canApproveScenario && !canRejectScenario && !canAcknowledgeEscalation ? (
        <p className="muted-text">{scenario.type === 'PREVIEW'
          ? 'This preview is visible for analysis only. Save it for governance if the decision needs review.'
          : scenario.approvalStatus === 'APPROVED'
            ? 'Governance is complete. The approved decision is ready for external operational follow-through.'
            : 'This scenario is visible for traceability and comparison, but it does not need another governance action right now.'}</p>
      ) : null}
    </article>
  )
}
