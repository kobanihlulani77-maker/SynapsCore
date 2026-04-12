import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { SummaryCard } from '../components/Card'

export default function ReplayPage({ context }) {
  const {
    isAuthenticated,
    isReplayPage,
    snapshot,
    selectedReplayRecordId,
    setSelectedReplayRecordId,
    pendingReplayCount,
    integrationReplayState,
    replayFailedIntegration,
    signedInSession,
    signedInRoles,
    signedInWarehouseScopes,
    hasWarehouseScope,
    navigateToPage,
    formatCodeLabel,
    formatTimestamp,
    getReplayStatusClassName,
  } = context

  if (!isAuthenticated || !isReplayPage) {
    return null
  }

  const queuedRecords = snapshot.integrationReplayQueue
  const selectedRecord = queuedRecords.find((record) => record.id === selectedReplayRecordId)
    || queuedRecords.find((record) => record.status === 'PENDING')
    || queuedRecords[0]
  const failedCount = queuedRecords.filter((record) => record.status === 'REPLAY_FAILED').length
  const recoveredCount = queuedRecords.filter((record) => record.status === 'REPLAYED').length

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Replay queue</p><h2>Recover failed inbound work safely</h2></div>
          <span className="panel-badge integration-badge">{pendingReplayCount}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Waiting" value={queuedRecords.filter((item) => item.status === 'PENDING').length} accent="amber" />
          <SummaryCard label="Replay failed" value={failedCount} accent="rose" />
          <SummaryCard label="Recovered" value={recoveredCount} accent="teal" />
          <SummaryCard label="Connectors" value={snapshot.integrationConnectors.length} accent="blue" />
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Failed inbound queue</strong>
              <span className="scenario-type-tag">{queuedRecords.length}</span>
            </div>
            {integrationReplayState.error ? <p className="error-text">{integrationReplayState.error}</p> : null}
            {integrationReplayState.success ? <p className="success-text">{integrationReplayState.success}</p> : null}
            <div className="signal-list">
              {queuedRecords.length ? queuedRecords.map((record) => (
                <button
                  key={record.id}
                  className={`signal-list-item selectable-card system-select-card ${selectedRecord?.id === record.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedReplayRecordId(record.id)}
                  type="button"
                >
                  <div className="stack-title-row">
                    <strong>{record.externalOrderId}</strong>
                    <span className={`status-tag ${getReplayStatusClassName(record.status)}`}>{formatCodeLabel(record.status)}</span>
                  </div>
                  <p>{record.sourceSystem} | {record.warehouseCode || 'Unknown lane'}</p>
                  <p className="muted-text">{record.failureMessage}</p>
                  <p className="muted-text">Attempts {record.replayAttemptCount} | Queued {formatTimestamp(record.createdAt)}</p>
                </button>
              )) : <EmptyState>No failed inbound items are waiting. Recovery is currently clear.</EmptyState>}
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Recovery detail</strong>
              <span className="scenario-type-tag">{selectedRecord ? formatCodeLabel(selectedRecord.connectorType) : 'Clear'}</span>
            </div>
            {selectedRecord ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedRecord.externalOrderId}</strong>
                  <p>{selectedRecord.failureMessage}</p>
                  <p className="muted-text">
                    Source {selectedRecord.sourceSystem} | Warehouse {selectedRecord.warehouseCode || 'Unknown'} | Attempts {selectedRecord.replayAttemptCount}
                  </p>
                  <p className="muted-text">
                    {selectedRecord.lastAttemptedAt ? `Last attempted ${formatTimestamp(selectedRecord.lastAttemptedAt)} | ` : ''}
                    Queued {formatTimestamp(selectedRecord.createdAt)}
                  </p>
                  {selectedRecord.lastReplayMessage ? <p className="muted-text">Last replay note: {selectedRecord.lastReplayMessage}</p> : null}
                </div>
                <div className="history-action-row">
                  <button
                    className="secondary-button"
                    onClick={() => replayFailedIntegration(selectedRecord.id)}
                    disabled={integrationReplayState.loadingId === selectedRecord.id || !signedInSession || !signedInRoles.some((role) => role === 'INTEGRATION_OPERATOR' || role === 'INTEGRATION_ADMIN') || !hasWarehouseScope(signedInWarehouseScopes, selectedRecord.warehouseCode)}
                    type="button"
                  >
                    {integrationReplayState.loadingId === selectedRecord.id ? 'Replaying...' : 'Replay Into Live Flow'}
                  </button>
                  <button className="ghost-button" onClick={() => navigateToPage('integrations')} type="button">View Connector Health</button>
                </div>
                <p className="muted-text">Recovery keeps failed inbound activity visible, actionable, and auditable instead of hidden inside scripts or operator guesswork.</p>
              </div>
            ) : <EmptyState>Select a replay record to inspect failure reason, attempts, and recovery posture.</EmptyState>}
          </article>
        </div>
      </Panel>
    </section>
  )
}
