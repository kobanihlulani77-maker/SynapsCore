import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { SummaryCard } from '../components/Card'

export default function RuntimePage({ context }) {
  const {
    isAuthenticated,
    isRuntimePage,
    runtime,
    systemIncidents,
    selectedRuntimeIncidentKey,
    setSelectedRuntimeIncidentKey,
    navigateToPage,
    formatCodeLabel,
    formatMetricValue,
    formatTimestamp,
    getIncidentStatusClassName,
    getRuntimeStatusClassName,
  } = context

  if (!isAuthenticated || !isRuntimePage) {
    return null
  }

  const selectedRuntimeIncident = systemIncidents.find((incident) => incident.incidentKey === selectedRuntimeIncidentKey) || systemIncidents[0]
  const connectorDiagnostics = runtime?.connectorDiagnostics || []
  const runtimeSignalCards = runtime
    ? [
      { label: 'Readiness', value: formatCodeLabel(runtime.readinessState), note: 'Current service acceptance posture' },
      { label: 'Queue depth', value: runtime.backbone.pendingDispatchCount, note: 'Pending work in dispatch backbone' },
      { label: 'Failed dispatch', value: runtime.backbone.failedDispatchCount, note: 'Dispatch work needing operator attention' },
      { label: 'Realtime broker', value: formatCodeLabel(runtime.backbone.realtimeBrokerMode || 'unknown'), note: 'Current websocket delivery strategy' },
      { label: 'Observed', value: formatTimestamp(runtime.observedAt), note: 'Latest runtime observation point' },
    ]
    : []

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Runtime observability</p><h2>Service health, queue pressure, and incident trust</h2></div>
          <span className={`panel-badge ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'audit-badge'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Readiness" value={runtime ? formatCodeLabel(runtime.readinessState) : '...'} accent="teal" />
          <SummaryCard label="Dispatch queued" value={runtime ? runtime.backbone.pendingDispatchCount : '...'} accent="amber" />
          <SummaryCard label="Failed dispatch" value={runtime ? runtime.backbone.failedDispatchCount : '...'} accent="rose" />
          <SummaryCard label="Incidents" value={systemIncidents.length} accent="blue" />
        </div>
        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card" id="runtime-health">
            <div className="stack-title-row">
              <strong>Health board</strong>
              <span className={`status-tag ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'status-partial'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
            </div>
            <div className="signal-list">
              {runtimeSignalCards.length ? runtimeSignalCards.map((card) => (
                <div key={card.label} className="signal-list-item">
                  <strong>{card.label}</strong>
                  <p>{card.value}</p>
                  <p className="muted-text">{card.note}</p>
                </div>
              )) : <EmptyState>Runtime signals will appear once the service heartbeat is available.</EmptyState>}
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Backbone and metrics</strong>
              <span className="scenario-type-tag">Prometheus ready</span>
            </div>
            {runtime ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>Dispatch backbone</strong>
                  <p>Drains every {runtime.backbone.dispatchIntervalMs} ms in batches of {runtime.backbone.batchSize}.</p>
                  <p className="muted-text">Oldest queued work {runtime.backbone.oldestPendingAgeSeconds == null ? 'clear' : `${runtime.backbone.oldestPendingAgeSeconds}s`} | Latest processed {formatTimestamp(runtime.backbone.latestProcessedAt)}</p>
                </div>
                <div className="signal-list-item">
                  <strong>Realtime broker</strong>
                  <p>{formatCodeLabel(runtime.backbone.realtimeBrokerMode || 'unknown')}</p>
                  <p className="muted-text">{runtime.backbone.realtimeBrokerDetail || 'Tenant-scoped websocket publishing is behind a replaceable broker boundary.'}</p>
                  <p className="muted-text">
                    {runtime.backbone.realtimeExternalBrokerConfigured
                      ? 'External broker relay is active, so realtime can scale beyond a single app node.'
                      : 'External broker relay is not active. Current realtime delivery is truthful but single-node only.'}
                  </p>
                </div>
                <div className="signal-list-item">
                  <strong>Telemetry window</strong>
                  <p>{runtime.diagnostics.windowHours} hrs | {runtime.diagnostics.businessEventsInWindow} business events | {runtime.diagnostics.integrationEventsInWindow} integration events</p>
                  <p className="muted-text">Latest business event {formatTimestamp(runtime.diagnostics.latestBusinessEventAt)} | Latest failure audit {formatTimestamp(runtime.diagnostics.latestFailureAt)}</p>
                </div>
                <div className="signal-list-item">
                  <strong>Metrics surface</strong>
                  <p>Orders {formatMetricValue(runtime.metrics.ordersIngested)} | Fulfillment {formatMetricValue(runtime.metrics.fulfillmentUpdates)} | Dispatch processed {formatMetricValue(runtime.metrics.dispatchProcessed)}</p>
                  <p className="muted-text">Prometheus metrics are exposed for production scraping at <code>/actuator/prometheus</code>.</p>
                </div>
                <div className="signal-list-item">
                  <strong>Connector diagnostics</strong>
                  {connectorDiagnostics.length ? (
                    <>
                      <p>{connectorDiagnostics.length} connector lane{connectorDiagnostics.length === 1 ? '' : 's'} currently need trust review.</p>
                      <p className="muted-text">
                        {connectorDiagnostics[0].displayName}
                        {connectorDiagnostics[0].lastFailureCode ? ` | ${formatCodeLabel(connectorDiagnostics[0].lastFailureCode)}` : ''}
                        {connectorDiagnostics[0].oldestPendingReplayAgeSeconds != null ? ` | Oldest replay ${connectorDiagnostics[0].oldestPendingReplayAgeSeconds}s` : ''}
                      </p>
                    </>
                  ) : (
                    <>
                      <p>No degraded connector telemetry is active right now.</p>
                      <p className="muted-text">When integration lanes slip, the latest failure and replay age will show here.</p>
                    </>
                  )}
                </div>
              </div>
            ) : <EmptyState>Queue, diagnostics, and metrics posture will appear once runtime data loads.</EmptyState>}
          </article>
          <article className="stack-card section-card" id="runtime-incident-lane">
            <div className="stack-title-row">
              <strong>Incident lane</strong>
              <span className="scenario-type-tag">{systemIncidents.length}</span>
            </div>
            <div className="signal-list">
              {systemIncidents.length ? systemIncidents.slice(0, 4).map((incident) => (
                <button
                  key={incident.incidentKey}
                  className={`signal-list-item selectable-card ${selectedRuntimeIncident?.incidentKey === incident.incidentKey ? 'is-selected' : ''}`}
                  onClick={() => setSelectedRuntimeIncidentKey(incident.incidentKey)}
                  type="button"
                >
                  <div className="stack-title-row">
                    <strong>{incident.title}</strong>
                    <span className={`status-tag ${getIncidentStatusClassName(incident.severity)}`}>{formatCodeLabel(incident.severity)}</span>
                  </div>
                  <p>{incident.detail}</p>
                  <p className="muted-text">{incident.context} | {formatTimestamp(incident.createdAt)}</p>
                </button>
              )) : <EmptyState>No active runtime incidents. This lane lights up when trust or backbone issues need operator attention.</EmptyState>}
            </div>
          </article>
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Incident focus</strong>
              <span className={`status-tag ${selectedRuntimeIncident ? getIncidentStatusClassName(selectedRuntimeIncident.severity) : 'status-partial'}`}>
                {selectedRuntimeIncident ? formatCodeLabel(selectedRuntimeIncident.severity) : 'Clear'}
              </span>
            </div>
            {selectedRuntimeIncident ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedRuntimeIncident.title}</strong>
                  <p>{selectedRuntimeIncident.detail}</p>
                  <p className="muted-text">{selectedRuntimeIncident.context}</p>
                  <p className="muted-text">Observed {formatTimestamp(selectedRuntimeIncident.createdAt)}</p>
                </div>
              </div>
            ) : <EmptyState>When runtime or connector trust issues appear, this page will hold the lead incident context here.</EmptyState>}
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Runtime response posture</strong>
              <span className="scenario-type-tag">{runtime?.overallStatus || 'Loading'}</span>
            </div>
            <div className="utility-metric-grid">
              <div><span>Queue pending</span><strong>{runtime?.backbone?.pendingDispatchCount ?? 0}</strong></div>
              <div><span>Failed dispatch</span><strong>{runtime?.backbone?.failedDispatchCount ?? 0}</strong></div>
              <div><span>Realtime</span><strong>{formatCodeLabel(runtime?.backbone?.realtimeBrokerMode || 'unknown')}</strong></div>
              <div><span>Scale mode</span><strong>{runtime?.backbone?.realtimeSingleNodeOnly ? 'Single node' : 'Multi node'}</strong></div>
              <div><span>High severity</span><strong>{systemIncidents.filter((incident) => ['CRITICAL', 'HIGH'].includes(incident.severity)).length}</strong></div>
              <div><span>Oldest queued</span><strong>{runtime?.backbone?.oldestPendingAgeSeconds == null ? 'Clear' : `${runtime.backbone.oldestPendingAgeSeconds}s`}</strong></div>
            </div>
            <div className="signal-list">
              {connectorDiagnostics.length ? connectorDiagnostics.slice(0, 2).map((connector) => (
                <div key={`${connector.sourceSystem}:${connector.connectorType}`} className="signal-list-item">
                  <strong>{connector.displayName}</strong>
                  <p>{connector.lastFailureMessage || connector.healthSummary}</p>
                  <p className="muted-text">
                    {connector.sourceSystem} | {formatCodeLabel(connector.healthStatus)}
                    {connector.lastFailureCode ? ` | ${formatCodeLabel(connector.lastFailureCode)}` : ''}
                    {connector.oldestPendingReplayAgeSeconds != null ? ` | Replay age ${connector.oldestPendingReplayAgeSeconds}s` : ''}
                  </p>
                </div>
              )) : null}
            </div>
            <div className="history-action-row">
              <button className="ghost-button" onClick={() => navigateToPage('audit')} type="button">Open Audit</button>
              <button className="ghost-button" onClick={() => navigateToPage('releases')} type="button">Open Releases</button>
            </div>
            <p className="muted-text">This page should help teams decide whether the issue is operational noise, queue pressure, or a release/runtime trust problem.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}
