import { SummaryCard } from '../components/Card'
import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'

export default function AuditPage({ context }) {
  const {
    isAuthenticated,
    isAuditPage,
    snapshot,
    systemIncidents,
    pendingReplayCount,
    recentBusinessEvents,
    recentAuditEntries,
    selectedAuditTrace,
    setSelectedTraceEntryKey,
    formatCodeLabel,
    formatTimestamp,
    navigateToPage,
  } = context

  if (!isAuthenticated || !isAuditPage) return null

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Audit and events</p><h2>Trace the live business timeline</h2></div>
          <span className="panel-badge audit-badge">{snapshot.auditLogs.length + snapshot.recentEvents.length}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Audit entries" value={snapshot.auditLogs.length} accent="blue" />
          <SummaryCard label="Business events" value={snapshot.recentEvents.length} accent="teal" />
          <SummaryCard label="Incidents" value={systemIncidents.length} accent="rose" />
          <SummaryCard label="Replay queued" value={pendingReplayCount} accent="amber" />
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card" id="audit-events">
            <div className="stack-title-row"><strong>Business event timeline</strong><span className="scenario-type-tag">{recentBusinessEvents.length}</span></div>
            <div className="signal-list">
              {recentBusinessEvents.length ? recentBusinessEvents.map((event) => (
                <button key={event.id} className={`signal-list-item selectable-card ${selectedAuditTrace?.traceKey === `event-${event.id}` ? 'is-selected' : ''}`} onClick={() => setSelectedTraceEntryKey(`event-${event.id}`)} type="button">
                  <div className="stack-title-row">
                    <strong>{formatCodeLabel(event.eventType)}</strong>
                    <span className="scenario-type-tag">{event.source}</span>
                  </div>
                  <p>{event.payloadSummary}</p>
                  <p className="muted-text">{formatTimestamp(event.createdAt)}</p>
                </button>
              )) : <EmptyState>Business events will stream here as SynapseCore processes operational change.</EmptyState>}
            </div>
          </article>
          <article className="stack-card section-card" id="audit-logs">
            <div className="stack-title-row"><strong>Audit explorer</strong><span className="scenario-type-tag">{recentAuditEntries.length}</span></div>
            <div className="signal-list">
              {recentAuditEntries.length ? recentAuditEntries.map((log) => (
                <button key={log.id} className={`signal-list-item selectable-card ${selectedAuditTrace?.traceKey === `audit-${log.id}` ? 'is-selected' : ''}`} onClick={() => setSelectedTraceEntryKey(`audit-${log.id}`)} type="button">
                  <div className="stack-title-row">
                    <strong>{formatCodeLabel(log.action)}</strong>
                    <span className={`status-tag status-${log.status.toLowerCase()}`}>{log.status}</span>
                  </div>
                  <p>{log.details}</p>
                  <p className="muted-text">{log.targetType} | {log.targetRef} | {formatTimestamp(log.createdAt)}</p>
                  <p className="trace-line">Request {log.requestId}</p>
                </button>
              )) : <EmptyState>Audit traces will appear once protected operational actions are flowing.</EmptyState>}
            </div>
          </article>
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Trace focus</strong><span className="scenario-type-tag">{selectedAuditTrace ? formatCodeLabel(selectedAuditTrace.traceType) : 'Waiting'}</span></div>
            {selectedAuditTrace ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedAuditTrace.traceType === 'audit' ? formatCodeLabel(selectedAuditTrace.action) : formatCodeLabel(selectedAuditTrace.eventType)}</strong>
                  <p>{selectedAuditTrace.traceType === 'audit' ? selectedAuditTrace.details : selectedAuditTrace.payloadSummary}</p>
                  <p className="muted-text">{selectedAuditTrace.traceType === 'audit' ? `${selectedAuditTrace.targetType} | ${selectedAuditTrace.targetRef}` : `${selectedAuditTrace.source} | ${selectedAuditTrace.id}`}</p>
                  <p className="muted-text">{formatTimestamp(selectedAuditTrace.createdAt)}</p>
                  {selectedAuditTrace.traceType === 'audit' ? <p className="trace-line">Request {selectedAuditTrace.requestId}</p> : null}
                </div>
              </div>
            ) : <EmptyState>Select an audit entry or business event to inspect the exact trace lane and timing context.</EmptyState>}
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Traceability posture</strong><span className="scenario-type-tag">{snapshot.auditLogs.length + snapshot.recentEvents.length}</span></div>
            <div className="utility-metric-grid">
              <div><span>Audit</span><strong>{snapshot.auditLogs.length}</strong></div>
              <div><span>Events</span><strong>{snapshot.recentEvents.length}</strong></div>
              <div><span>Replay</span><strong>{pendingReplayCount}</strong></div>
              <div><span>Incidents</span><strong>{systemIncidents.length}</strong></div>
            </div>
            <div className="history-action-row">
              <button className="ghost-button" onClick={() => navigateToPage('runtime')} type="button">Open Runtime</button>
              <button className="ghost-button" onClick={() => navigateToPage('replay')} type="button">Open Replay</button>
            </div>
            <p className="muted-text">This page should help support and operators line events, protected actions, and recovery flow back to one trusted timeline.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}

