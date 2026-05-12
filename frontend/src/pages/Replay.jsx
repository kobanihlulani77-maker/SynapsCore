import { useEffect, useState } from 'react'
import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { MetricCard } from '../components/Card'

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
    fetchJson,
  } = context

  if (!isAuthenticated || !isReplayPage) {
    return null
  }

  const queuedRecords = snapshot.integrationReplayQueue
  const selectedRecord = queuedRecords.find((record) => record.id === selectedReplayRecordId)
    || queuedRecords.find((record) => record.status === 'PENDING')
    || queuedRecords[0]
  const snapshotSelectedConnector = selectedRecord
    ? snapshot.integrationConnectors.find((connector) => connector.sourceSystem === selectedRecord.sourceSystem && connector.type === selectedRecord.connectorType)
    : null
  const [selectedConnectorOverride, setSelectedConnectorOverride] = useState(null)

  useEffect(() => {
    let active = true
    let refreshTimeoutId = null

    const clearRefreshTimer = () => {
      if (refreshTimeoutId !== null) {
        globalThis.clearTimeout(refreshTimeoutId)
        refreshTimeoutId = null
      }
    }

    async function loadSelectedConnector() {
      if (!selectedRecord?.sourceSystem || !selectedRecord?.connectorType || !fetchJson) {
        if (active) {
          setSelectedConnectorOverride(null)
        }
        return
      }

      try {
        const connectorPayload = await fetchJson(
          `/api/integrations/orders/connectors?sourceSystem=${encodeURIComponent(selectedRecord.sourceSystem)}&type=${encodeURIComponent(selectedRecord.connectorType)}`,
          globalThis.AbortSignal?.timeout ? { signal: globalThis.AbortSignal.timeout(8_000) } : {},
        )
        const exactConnector = Array.isArray(connectorPayload)
          ? connectorPayload.find((connector) => connector.sourceSystem === selectedRecord.sourceSystem && connector.type === selectedRecord.connectorType) || null
          : null

        if (!active) {
          return
        }

        setSelectedConnectorOverride(exactConnector)

        if (selectedRecord.status === 'PENDING' && exactConnector && !exactConnector.enabled) {
          clearRefreshTimer()
          refreshTimeoutId = globalThis.setTimeout(() => {
            refreshTimeoutId = null
            void loadSelectedConnector()
          }, 2_000)
        }
      } catch {
        if (!active) {
          return
        }
        setSelectedConnectorOverride(null)
        if (selectedRecord.status === 'PENDING') {
          clearRefreshTimer()
          refreshTimeoutId = globalThis.setTimeout(() => {
            refreshTimeoutId = null
            void loadSelectedConnector()
          }, 2_000)
        }
      }
    }

    void loadSelectedConnector()

    return () => {
      active = false
      clearRefreshTimer()
    }
  }, [
    fetchJson,
    selectedRecord?.id,
    selectedRecord?.sourceSystem,
    selectedRecord?.connectorType,
    selectedRecord?.status,
    snapshot.integrationConnectors,
  ])

  const selectedConnector = selectedConnectorOverride || snapshotSelectedConnector
  const replayBlockedByEligibility = Boolean(
    selectedRecord?.nextEligibleAt
      && Number.isFinite(Date.parse(selectedRecord.nextEligibleAt))
      && Date.parse(selectedRecord.nextEligibleAt) > Date.now()
  )
  const replayBlockedByConnector = selectedConnector ? !selectedConnector.enabled : false
  const replayBlockedMessage = replayBlockedByConnector
    ? `Connector ${selectedRecord?.sourceSystem || 'unknown'} is disabled. Re-enable it before replaying failed inbound work.`
    : replayBlockedByEligibility
      ? `This replay record is gated until ${formatTimestamp(selectedRecord?.nextEligibleAt)}.`
      : ''
  const failedCount = queuedRecords.filter((record) => record.status === 'REPLAY_FAILED').length
  const recoveredCount = queuedRecords.filter((record) => record.status === 'REPLAYED').length
  const eligiblePendingCount = queuedRecords.filter((record) => record.status === 'PENDING' && !record.nextEligibleAt).length

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Replay queue</p>
            <h2>Recover failed inbound work safely</h2>
          </div>
          <span className="panel-badge integration-badge">{pendingReplayCount}</span>
        </div>

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Failed inbound recovery surface</strong>
            <p>
              Recovery keeps failed source activity visible, actionable, and auditable. Operators should be able to see
              the failed source, replay eligibility, connector posture, and safe next action without guessing.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Connector aware</span>
              <span className="workspace-meta-pill">Audit visible</span>
              <span className="workspace-meta-pill">Manual recovery safe</span>
            </div>
          </div>
          <div className="ops-command-actions">
            <button className="secondary-button" onClick={() => selectedRecord && setSelectedReplayRecordId(selectedRecord.id)} disabled={!selectedRecord} type="button">
              Review next replay item
            </button>
            <button className="ghost-button" onClick={() => navigateToPage('integrations')} type="button">
              Open connector health
            </button>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Waiting" value={queuedRecords.filter((item) => item.status === 'PENDING').length} accent="amber" note="Replay records still waiting for operator or connector recovery." />
          <MetricCard label="Eligible now" value={eligiblePendingCount} accent="blue" note="Pending records that can move immediately into manual recovery." />
          <MetricCard label="Replay failed" value={failedCount} accent="rose" note="Records that attempted recovery but still need another intervention." />
          <MetricCard label="Recovered" value={recoveredCount} accent="teal" note="Inbound records already brought back into the live operational flow." />
        </div>

        {integrationReplayState.error ? <p className="error-text">{integrationReplayState.error}</p> : null}
        {integrationReplayState.success ? <p className="success-text">{integrationReplayState.success}</p> : null}

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Failed inbound queue</strong>
              <span className="scenario-type-tag">{queuedRecords.length}</span>
            </div>
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
                    disabled={
                      integrationReplayState.loadingId === selectedRecord.id
                      || !signedInSession
                      || !signedInRoles.some((role) => role === 'INTEGRATION_OPERATOR' || role === 'INTEGRATION_ADMIN')
                      || !hasWarehouseScope(signedInWarehouseScopes, selectedRecord.warehouseCode)
                      || replayBlockedByEligibility
                      || replayBlockedByConnector
                    }
                    type="button"
                  >
                    {integrationReplayState.loadingId === selectedRecord.id ? 'Replaying...' : 'Replay Into Live Flow'}
                  </button>
                  <button className="ghost-button" onClick={() => navigateToPage('integrations')} type="button">View Connector Health</button>
                </div>
                {replayBlockedMessage ? <p className="muted-text">{replayBlockedMessage}</p> : null}
                <p className="muted-text">Recovery keeps failed inbound activity visible, actionable, and auditable instead of hidden inside scripts or operator guesswork.</p>
              </div>
            ) : <EmptyState>Select a replay record to inspect failure reason, eligibility, connector posture, and safe recovery options.</EmptyState>}
          </article>
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Connector recovery posture</strong>
              <span className="scenario-type-tag">{selectedConnector ? formatCodeLabel(selectedConnector.healthStatus || 'unknown') : 'Waiting'}</span>
            </div>
            {selectedConnector ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedConnector.displayName}</strong>
                  <p>{selectedConnector.healthSummary || selectedConnector.notes || 'Connector posture is visible here once connector metadata is loaded.'}</p>
                  <p className="muted-text">
                    {selectedConnector.enabled ? 'Connector enabled' : 'Connector disabled'}
                    {' | '}
                    Pending replay {selectedConnector.pendingReplayCount || 0}
                    {' | '}
                    Dead-letter {selectedConnector.deadLetterCount || 0}
                  </p>
                </div>
              </div>
            ) : <EmptyState>The connector serving this replay record will appear here once it can be resolved from the workspace connector portfolio.</EmptyState>}
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Recovery operating rules</strong>
              <span className="scenario-type-tag">Safe action</span>
            </div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>What needs attention first</strong>
                <p>
                  {replayBlockedByConnector
                    ? 'The connector must be re-enabled before manual replay is allowed.'
                    : replayBlockedByEligibility
                      ? 'This replay item is held by eligibility timing. Wait until the record becomes available again.'
                      : selectedRecord
                        ? 'The replay item is ready for guided recovery through the live operator flow.'
                        : 'Select a replay record to review its recovery posture.'}
                </p>
              </div>
              <div className="signal-list-item">
                <strong>What this page should do</strong>
                <p>Show why inbound work failed, whether it is safe to replay now, and how connector health affects recovery without pushing teams into hidden scripts.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
