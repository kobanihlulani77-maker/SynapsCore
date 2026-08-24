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
    signedInRoles,
  } = context

  if (!isAuthenticated || !isIntegrationsPage) {
    return null
  }

  const connectorPortfolio = snapshot.integrationConnectors
  const recentImportRuns = snapshot.integrationImportRuns.slice(0, 4)
  const unownedConnectors = connectorPortfolio.filter((connector) => !connector.supportOwnerActorName).length
  const realtimeConnectors = connectorPortfolio.filter((connector) => connector.syncMode === 'REALTIME_PUSH').length
  const connectedSystemCount = new Set(connectorPortfolio.map((connector) => connector.sourceSystem)).size
  const fallbackEnabledCount = connectorPortfolio.filter((connector) => connector.allowDefaultWarehouseFallback).length
  const disabledConnectorCount = connectorPortfolio.filter((connector) => connector.healthStatus === 'OFFLINE').length
  const degradedConnectorCount = connectorPortfolio.filter((connector) => connector.healthStatus === 'DEGRADED').length
  const connectorAttentionCount = disabledConnectorCount + degradedConnectorCount + unownedConnectors
  const connectorTrustPosture = disabledConnectorCount
    ? 'Recovery required'
    : degradedConnectorCount || pendingReplayCount
      ? 'Limited trust'
      : connectorPortfolio.length
        ? 'Trusted lanes'
        : 'Not connected'
  const supportedModeLabel = (connector) => (connector?.supportedSyncModes || [])
    .map((mode) => formatCodeLabel(mode))
    .join(' | ')
  const canManageConnectorPolicy = signedInRoles.includes('INTEGRATION_ADMIN')

  const getConnectorTone = (connector) => {
    if (connector.healthStatus === 'OFFLINE') {
      return 'failure'
    }
    if (connector.healthStatus === 'DEGRADED') {
      return 'partial'
    }
    return 'success'
  }

  const getConnectorStatusClassName = (connector) => {
    if (connector.healthStatus === 'OFFLINE') {
      return 'status-failure'
    }
    if (connector.healthStatus === 'DEGRADED') {
      return 'status-partial'
    }
    return 'status-success'
  }

  const getConnectorStatusLabel = (connector) => {
    if (connector.healthStatus === 'OFFLINE') {
      return 'Offline'
    }
    if (connector.healthStatus === 'DEGRADED') {
      return 'Degraded'
    }
    return 'Live'
  }

  const getConnectorAttentionScore = (connector) => {
    if (!connector) {
      return 0
    }
    return [
      connector.healthStatus === 'OFFLINE' ? 100 : 0,
      connector.healthStatus === 'DEGRADED' ? 70 : 0,
      connector.pendingReplayCount ? 50 : 0,
      connector.recentInboundFailureCount ? 35 : 0,
      !connector.supportOwnerActorName ? 20 : 0,
    ].reduce((total, score) => total + score, 0)
  }

  const connectorSpotlights = [...connectorPortfolio]
    .sort((left, right) => getConnectorAttentionScore(right) - getConnectorAttentionScore(left))
    .slice(0, 6)
  const selectedConnector = connectorPortfolio.find((connector) => connector.id === selectedIntegrationConnectorId) || connectorSpotlights[0]
  const connectorType = selectedConnector?.type?.toUpperCase() || ''
  const selectedConnectorLimitation = selectedConnector?.healthStatus === 'OFFLINE' && connectorType.includes('WEBHOOK')
    ? 'Disabled-webhook replay/readback is not fully proven. Do not assume a disabled webhook can be recovered through the same evidence lane as CSV failed-inbound recovery.'
    : selectedConnector && (connectorType.includes('CSV') || connectorType.includes('FILE'))
      ? 'CSV failed-inbound recovery is the proven pilot recovery lane. Confirm the source correction and duplicate safety before replay.'
      : ''

  const formatReplayAge = (ageSeconds) => {
    if (ageSeconds == null) {
      return null
    }
    if (ageSeconds < 60) {
      return `${ageSeconds}s`
    }
    if (ageSeconds < 3600) {
      return `${Math.floor(ageSeconds / 60)}m`
    }
    if (ageSeconds < 86400) {
      return `${Math.floor(ageSeconds / 3600)}h`
    }
    return `${Math.floor(ageSeconds / 86400)}d`
  }

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Integrations</p>
            <h2>Connector health and operational telemetry</h2>
          </div>
          <span className="panel-badge integration-badge">{connectorPortfolio.length}</span>
        </div>

        <div className="workflow-decision-hero connector-trust-hero">
          <div className="workflow-decision-copy">
            <strong>Connector operations surface</strong>
            <p>
              This is where companies should understand what external systems are connected, which lanes are healthy,
              and where support ownership, validation policy, or replay pressure is starting to affect operational trust.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">{connectorTrustPosture}</span>
              <span className="workspace-meta-pill">{pendingReplayCount} replay waiting</span>
              <span className="workspace-meta-pill">{unownedConnectors} ownership gaps</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Trust posture</span>
              <strong>{connectorTrustPosture}</strong>
              <p>{connectorAttentionCount ? `${connectorAttentionCount} connector signals need ownership, remediation, or recovery review.` : 'Connector lanes are not currently reporting operational pressure.'}</p>
            </div>
            <div className="workflow-action-card">
              <span>Recovery route</span>
              <strong>{pendingReplayCount ? 'Replay queue active' : 'Replay queue clear'}</strong>
              <p>Failed inbound records should move through replay instead of being repaired with hidden database edits.</p>
            </div>
            <div className="ops-command-actions">
              <button className="secondary-button" onClick={() => selectedConnector && setSelectedIntegrationConnectorId(selectedConnector.id)} disabled={!selectedConnector} type="button">
                Inspect connector
              </button>
              <button className="ghost-button" onClick={() => navigateToPage('replay')} type="button">
                Open replay queue
              </button>
            </div>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Enabled connectors" value={enabledConnectorCount} accent="teal" note="Connector lanes currently allowed to feed live operational state." />
          <MetricCard label="Connected systems" value={connectedSystemCount} accent="blue" note="Distinct external operating systems represented in the workspace." />
          <MetricCard label="Replay queued" value={pendingReplayCount} accent="amber" note="Inbound records still waiting on recovery or connector remediation." />
          <MetricCard label="Attention needed" value={connectorAttentionCount} accent="rose" note="Connectors that need ownership, remediation, or active support." />
        </div>

        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Connector trust queue</strong>
              <span className="scenario-type-tag">{connectorSpotlights.length ? 'Live' : 'Pending'}</span>
            </div>
            <p className="muted-text">Attention-heavy connectors rise first so failures, replay pressure, and ownership gaps are not buried below healthy lanes.</p>
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
                    tone={getConnectorTone(connector)}
                    meta={`${formatCodeLabel(connector.syncMode)}${connector.syncIntervalMinutes ? ` every ${connector.syncIntervalMinutes} min` : ''}${connector.lastActivityAt ? ` | Last activity ${formatTimestamp(connector.lastActivityAt)}` : connector.updatedAt ? ` | Updated ${formatTimestamp(connector.updatedAt)}` : ''}`}
                    action={(
                      <span className={`status-tag ${getConnectorStatusClassName(connector)}`}>
                        {getConnectorStatusLabel(connector)}
                      </span>
                    )}
                  />
                  <div className="attention-card-meta connector-card-meta">
                    <span>Owner: {connector.supportOwnerDisplayName || 'Unassigned'}</span>
                    <span>Replay: {connector.pendingReplayCount || 0}</span>
                    <span>Failures: {connector.recentInboundFailureCount || 0}</span>
                  </div>
                </button>
              )) : <EmptyState>Connectors will appear here once the workspace is integrated with external systems.</EmptyState>}
            </div>
          </article>

          <article className="stack-card section-card workflow-selected-panel connector-detail-panel">
            <div className="stack-title-row">
              <strong>Selected connector detail</strong>
              <span className="scenario-type-tag">{selectedConnector ? formatCodeLabel(selectedConnector.validationPolicy) : 'Waiting'}</span>
            </div>
            {selectedConnector ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedConnector.displayName}</strong>
                  <p>{selectedConnector.healthSummary || selectedConnector.notes || 'No connector notes yet. Capture support ownership and operating assumptions here.'}</p>
                  <p className="muted-text">Source {selectedConnector.sourceSystem} | Owner {selectedConnector.supportOwnerDisplayName || 'Unassigned'} | Pending replay {selectedConnector.pendingReplayCount || 0} | Dead-letter {selectedConnector.deadLetterCount || 0}</p>
                  <p className="muted-text">{selectedConnector.allowDefaultWarehouseFallback ? 'Warehouse fallback is enabled for missing inbound lane data.' : 'Warehouse fallback is off. Payloads must arrive with a valid lane.'}</p>
                  {selectedConnector.supportBoundary ? <p className="muted-text">{selectedConnector.supportBoundary}</p> : null}
                </div>
                <div className="utility-metric-grid">
                  <div><span>Health</span><strong>{getConnectorStatusLabel(selectedConnector)}</strong></div>
                  <div><span>Sync mode</span><strong>{formatCodeLabel(selectedConnector.syncMode)}</strong></div>
                  <div><span>Failures</span><strong>{selectedConnector.recentInboundFailureCount || 0}</strong></div>
                  <div><span>Replay waiting</span><strong>{selectedConnector.pendingReplayCount || 0}</strong></div>
                  <div><span>Owner</span><strong>{selectedConnector.supportOwnerDisplayName ? 'Assigned' : 'Missing'}</strong></div>
                  <div><span>Fallback</span><strong>{selectedConnector.allowDefaultWarehouseFallback ? 'On' : 'Off'}</strong></div>
                </div>
                <p className="muted-text">
                  Validation {formatCodeLabel(selectedConnector.validationPolicy)}
                  {selectedConnector.mappingVersion ? ` | Mapping v${selectedConnector.mappingVersion}` : ''}
                  {selectedConnector.syncIntervalMinutes ? ` | Sync cadence ${selectedConnector.syncIntervalMinutes} min` : ' | Event-driven cadence'}
                  {selectedConnector.lastPullStatus ? ` | Pull ${formatCodeLabel(selectedConnector.lastPullStatus)}` : ''}
                </p>
                {selectedConnector.supportedSyncModes?.length ? <p className="muted-text">Supported sync modes {supportedModeLabel(selectedConnector)}</p> : null}
                  {selectedConnector.lastFailureMessage ? (
                  <p className="muted-text">
                    Latest failure {selectedConnector.lastFailureCode ? formatCodeLabel(selectedConnector.lastFailureCode) : 'Unknown'}
                    {selectedConnector.lastFailureAt ? ` | ${formatTimestamp(selectedConnector.lastFailureAt)}` : ''}
                    {` | ${selectedConnector.lastFailureMessage}`}
                  </p>
                  ) : null}
                {selectedConnectorLimitation ? <p className="muted-text">Pilot recovery boundary: {selectedConnectorLimitation}</p> : null}
                {selectedConnector.oldestPendingReplayAgeSeconds != null ? (
                  <p className="muted-text">
                    Oldest replay waiting {formatReplayAge(selectedConnector.oldestPendingReplayAgeSeconds)}
                    {selectedConnector.oldestPendingReplayAt ? ` | queued ${formatTimestamp(selectedConnector.oldestPendingReplayAt)}` : ''}
                  </p>
                ) : null}
                <div className="history-action-row">
                  {canManageConnectorPolicy ? <button className="ghost-button" onClick={() => navigateToPage('settings')} type="button">Manage Policies</button> : <span className="muted-text">Connector policy changes require Integration Admin.</span>}
                  <button className="ghost-button" onClick={() => navigateToPage('replay')} type="button">Open Replay Queue</button>
                </div>
              </div>
            ) : <EmptyState>Choose a connector lane to review support ownership, sync mode, policy posture, and recovery readiness.</EmptyState>}
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Integration posture</strong>
              <span className="scenario-type-tag">{recentImportRuns.length} recent runs</span>
            </div>
            <div className="utility-metric-grid">
              <div><span>Realtime push</span><strong>{realtimeConnectors}</strong></div>
              <div><span>Unowned</span><strong>{unownedConnectors}</strong></div>
              <div><span>Degraded</span><strong>{degradedConnectorCount}</strong></div>
              <div><span>Fallback on</span><strong>{fallbackEnabledCount}</strong></div>
              <div><span>Support incidents</span><strong>{systemIncidents.length}</strong></div>
            </div>
                <p className="muted-text">Connector operations should make it obvious which company system lanes are trusted, which ones are limited, and where recovery posture is currently fragile.</p>
            {selectedConnectorLimitation ? <p className="muted-text">This limitation is shown because the selected connector state makes it operationally relevant; it does not change backend eligibility.</p> : null}
          </article>
        </div>

        <div className="experience-grid experience-grid-split">
          <ActivityFeed
            title="Recent sync and import flow"
            kicker="Data flow"
            items={recentImportRuns.map((run) => ({
              id: run.id,
              title: run.fileName || `${formatCodeLabel(run.connectorType)} ingress`,
              body: `${run.sourceSystem} | ${run.recordsReceived} rows`,
              meta: `${run.ordersImported} imported | ${run.ordersFailed} failed | ${formatTimestamp(run.createdAt)}`,
            }))}
            emptyMessage="Import telemetry will appear once webhook or CSV activity is flowing."
          />

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>What companies should feel here</strong>
              <span className="scenario-type-tag">Trust</span>
            </div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Operational clarity</strong>
                <p>SynapseCore should feel like it is sitting above the company system estate, showing which lanes are connected, trusted, degraded, or waiting on ownership.</p>
              </div>
              <div className="signal-list-item">
                <strong>Honest limitations</strong>
                <p>Connector sync modes, support boundaries, replay posture, and fallback behavior should all be visible without turning the page into a backend diagnostic dump.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
