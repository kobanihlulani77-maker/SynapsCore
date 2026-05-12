import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { MetricCard } from '../components/Card'

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
  const highSeverityIncidents = systemIncidents.filter((incident) => ['CRITICAL', 'HIGH'].includes(incident.severity)).length
  const runtimeSignalCards = runtime
    ? [
      { label: 'Readiness', value: formatCodeLabel(runtime.readinessState), note: 'Current service acceptance posture.' },
      { label: 'Queue depth', value: runtime.backbone.pendingDispatchCount, note: 'Pending work currently inside the dispatch backbone.' },
      { label: 'Failed dispatch', value: runtime.backbone.failedDispatchCount, note: 'Dispatch work already needing operator attention.' },
      { label: 'Realtime broker', value: formatCodeLabel(runtime.backbone.realtimeBrokerMode || 'unknown'), note: 'Current websocket delivery strategy for live state.' },
      { label: 'Alert hook', value: runtime.backbone.alertHookConfigured ? 'Configured' : 'Not configured', note: 'Whether severe operational failures can trigger external alerting.' },
      { label: 'Observed', value: formatTimestamp(runtime.observedAt), note: 'Latest runtime observation point.' },
    ]
    : []

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Runtime observability</p>
            <h2>Service health, queue pressure, and incident trust</h2>
          </div>
          <span className={`panel-badge ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'audit-badge'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
        </div>

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Live platform trust center</strong>
            <p>
              Runtime should help non-developer operators understand whether the platform is live, degraded, noisy, or truly in need of intervention,
              while still keeping technical depth available for support and release review.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Live posture</span>
              <span className="workspace-meta-pill">Incident aware</span>
              <span className="workspace-meta-pill">Queue visible</span>
            </div>
          </div>
          <div className="ops-command-actions">
            <button className="secondary-button" onClick={() => navigateToPage('audit')} type="button">
              Open audit
            </button>
            <button className="ghost-button" onClick={() => navigateToPage('releases')} type="button">
              Open releases
            </button>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Readiness" value={runtime ? formatCodeLabel(runtime.readinessState) : '...'} accent="teal" note="Whether the platform is currently safe to accept live operational traffic." />
          <MetricCard label="Dispatch queued" value={runtime ? runtime.backbone.pendingDispatchCount : '...'} accent="amber" note="Pending internal dispatch work still waiting to fan out across the platform." />
          <MetricCard label="Failed dispatch" value={runtime ? runtime.backbone.failedDispatchCount : '...'} accent="rose" note="Queue work that has already crossed into operator attention territory." />
          <MetricCard label="Incidents" value={systemIncidents.length} accent="blue" note="Active runtime or connector incidents currently visible in the trust surface." />
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
              <strong>Queue and telemetry</strong>
              <span className="scenario-type-tag">Platform depth</span>
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
                </div>
                <div className="signal-list-item">
                  <strong>Metrics surface</strong>
                  <p>Orders {formatMetricValue(runtime.metrics.ordersIngested)} | Fulfillment {formatMetricValue(runtime.metrics.fulfillmentUpdates)} | Dispatch processed {formatMetricValue(runtime.metrics.dispatchProcessed)}</p>
                  <p className="muted-text">Realtime publishes {formatMetricValue(runtime.metrics.realtimePublishes)} | Publish failures {formatMetricValue(runtime.metrics.realtimePublishFailures)} | Lock conflicts {formatMetricValue(runtime.metrics.inventoryLockConflicts)}</p>
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
                  className={`signal-list-item selectable-card system-select-card ${selectedRuntimeIncident?.incidentKey === incident.incidentKey ? 'is-selected' : ''}`}
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
              <div><span>High severity</span><strong>{highSeverityIncidents}</strong></div>
              <div><span>Alert hook</span><strong>{runtime?.backbone?.alertHookConfigured ? 'Configured' : 'Off'}</strong></div>
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
              )) : (
                <div className="signal-list-item">
                  <strong>Operational interpretation</strong>
                  <p>No degraded connector telemetry is active right now.</p>
                  <p className="muted-text">Use this page to separate operational noise from genuine platform trust issues.</p>
                </div>
              )}
            </div>
            <p className="muted-text">This surface should help teams decide whether the issue is normal operational noise, queue pressure, or a genuine release/runtime trust problem.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}
