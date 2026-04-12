import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { AlertCard, MetricCard } from '../components/Card'
import ActivityFeed from '../components/ActivityFeed'

export default function IntegrationsPage({ context }) {
  const {
    isAuthenticated,
    isIntegrationsPage,
    snapshot,
    selectedIntegrationConnectorId,
    setSelectedIntegrationConnectorId,
    enabledConnectorCount,
    pendingReplayCount,
    systemIncidents,
    navigateToPage,
    formatCodeLabel,
    formatTimestamp,
    getImportStatusClassName,
  } = context

  if (!isAuthenticated || !isIntegrationsPage) {
    return null
  }

  const connectorPortfolio = snapshot.integrationConnectors
  const connectorSpotlights = connectorPortfolio.slice(0, 6)
  const recentImportRuns = snapshot.integrationImportRuns.slice(0, 4)
  const selectedConnector = connectorPortfolio.find((connector) => connector.id === selectedIntegrationConnectorId) || connectorSpotlights[0]
  const unownedConnectors = connectorPortfolio.filter((connector) => !connector.supportOwnerActorName).length
  const scheduledConnectors = connectorPortfolio.filter((connector) => connector.syncMode === 'SCHEDULED_PULL').length
  const connectedSystemCount = new Set(connectorPortfolio.map((connector) => connector.sourceSystem)).size
  const fallbackEnabledCount = connectorPortfolio.filter((connector) => connector.allowDefaultWarehouseFallback).length
  const disabledConnectorCount = connectorPortfolio.filter((connector) => !connector.enabled).length

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Integrations</p><h2>Connector health and operational telemetry</h2></div>
          <span className="panel-badge integration-badge">{connectorPortfolio.length}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Enabled connectors" value={enabledConnectorCount} accent="teal" note="Routes actively feeding the workspace" />
          <MetricCard label="Connected systems" value={connectedSystemCount} accent="blue" note="Distinct external operating systems connected" />
          <MetricCard label="Replay queued" value={pendingReplayCount} accent="amber" note="Inbound items waiting on recovery" />
          <MetricCard label="Connector attention" value={disabledConnectorCount + unownedConnectors} accent="rose" note="Lanes that need ownership or remediation" />
        </div>
        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Connector portfolio</strong>
              <span className="scenario-type-tag">{connectorSpotlights.length ? 'Live' : 'Pending'}</span>
            </div>
            <div className="signal-list">
              {connectorSpotlights.length ? connectorSpotlights.map((connector) => (
                <button
                  key={`${connector.sourceSystem}:${connector.type}`}
                  className={`dashboard-card-button selectable-card system-select-card ${selectedConnector?.id === connector.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedIntegrationConnectorId(connector.id)}
                  type="button"
                >
                  <AlertCard
                    title={connector.displayName}
                    body={`${connector.sourceSystem} | ${formatCodeLabel(connector.type)}`}
                    tone={connector.enabled ? (connector.supportOwnerActorName ? 'success' : 'partial') : 'failure'}
                    meta={`${formatCodeLabel(connector.syncMode)}${connector.syncIntervalMinutes ? ` every ${connector.syncIntervalMinutes} min` : ''}${connector.updatedAt ? ` | Last sync ${formatTimestamp(connector.updatedAt)}` : ''}`}
                    action={
                      <span className={`status-tag ${connector.enabled ? (connector.supportOwnerActorName ? 'status-success' : 'status-partial') : 'status-failure'}`}>
                        {connector.enabled ? (connector.supportOwnerActorName ? 'Live' : 'Degraded') : 'Offline'}
                      </span>
                    }
                  />
                </button>
              )) : <EmptyState>Connectors will appear here once the workspace is integrated with external systems.</EmptyState>}
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Selected connector detail</strong>
              <span className="scenario-type-tag">{selectedConnector ? formatCodeLabel(selectedConnector.validationPolicy) : 'Waiting'}</span>
            </div>
            {selectedConnector ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedConnector.displayName}</strong>
                  <p>{selectedConnector.notes || 'No connector notes yet. Use this space to capture support ownership and operating assumptions.'}</p>
                  <p className="muted-text">Source {selectedConnector.sourceSystem} | Owner {selectedConnector.supportOwnerDisplayName || 'Unassigned'} | Failure count {snapshot.integrationReplayQueue.filter((record) => record.sourceSystem === selectedConnector.sourceSystem).length}</p>
                  <p className="muted-text">{selectedConnector.allowDefaultWarehouseFallback ? 'Warehouse fallback is enabled for missing inbound lane data.' : 'Warehouse fallback is off. Payloads must arrive with a valid lane.'}</p>
                </div>
                <div className="signal-list-item">
                  <strong>Connection posture</strong>
                  <p>{selectedConnector.enabled ? 'This connector is allowed to feed live operational state into the workspace.' : 'This connector is currently disabled and cannot feed live operational state.'}</p>
                  <p className="muted-text">
                    Validation {formatCodeLabel(selectedConnector.validationPolicy)}
                    {selectedConnector.syncIntervalMinutes ? ` | Sync cadence ${selectedConnector.syncIntervalMinutes} min` : ' | Event-driven cadence'}
                    {selectedConnector.updatedAt ? ` | Updated ${formatTimestamp(selectedConnector.updatedAt)}` : ''}
                  </p>
                </div>
                <div className="history-action-row">
                  <button className="ghost-button" onClick={() => navigateToPage('settings')} type="button">Manage Policies</button>
                  <button className="ghost-button" onClick={() => navigateToPage('replay')} type="button">Open Replay Queue</button>
                </div>
              </div>
            ) : <EmptyState>Choose a connector lane to review support ownership, policy posture, and recovery routes.</EmptyState>}
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Integration posture</strong>
              <span className="scenario-type-tag">{recentImportRuns.length} recent runs</span>
            </div>
            <div className="utility-metric-grid">
              <div><span>Scheduled pull</span><strong>{scheduledConnectors}</strong></div>
              <div><span>Unowned</span><strong>{unownedConnectors}</strong></div>
              <div><span>Fallback on</span><strong>{fallbackEnabledCount}</strong></div>
              <div><span>Support incidents</span><strong>{systemIncidents.length}</strong></div>
            </div>
            <ActivityFeed
              title="Recent sync and import flow"
              kicker="Data flow"
              items={recentImportRuns.map((run) => ({
                id: run.id,
                title: run.fileName || `${formatCodeLabel(run.connectorType)} ingress`,
                body: `${run.sourceSystem} | ${run.recordsReceived} rows`,
                meta: `${run.ordersImported} imported | ${run.ordersFailed} failed | ${formatTimestamp(run.createdAt)}`,
              }))}
              emptyMessage="Import telemetry will appear once webhook, CSV, or scheduled sync activity is flowing."
            />
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>What companies should feel here</strong>
                <p>SynapseCore is sitting above the company system estate, showing which lanes are connected, which ones are trusted, and where recovery is needed.</p>
                <p className="muted-text">Use this page to explain integration confidence, support ownership, and recovery readiness without leaving the operational workspace.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
