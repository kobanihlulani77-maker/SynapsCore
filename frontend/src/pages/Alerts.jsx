import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'

export default function AlertsPage({ context }) {
  const {
    isAuthenticated,
    isAlertsPage,
    activeAlerts,
    selectedAlertId,
    setSelectedAlertId,
    formatTimestamp,
  } = context

  if (!isAuthenticated || !isAlertsPage) {
    return null
  }

  const selectedAlert = activeAlerts.find((alert) => alert.id === selectedAlertId) || activeAlerts[0]
  const criticalAlertCount = activeAlerts.filter((alert) => alert.severity === 'CRITICAL').length
  const highAlertCount = activeAlerts.filter((alert) => alert.severity === 'HIGH').length

  return (
    <section className="content-grid alerts-center-grid">
      <Panel wide id="alerts-feed">
        <div className="panel-header">
          <div><p className="panel-kicker">Alerts center</p><h2>Operational warnings in one lane</h2></div>
          <span className="panel-badge alert-badge">{activeAlerts.length}</span>
        </div>
        <div className="filter-chip-row">
          <span className="scenario-type-tag">Severity</span>
          <span className="scenario-type-tag">Warehouse</span>
          <span className="scenario-type-tag">Type</span>
          <span className="scenario-type-tag">Status</span>
          <span className="scenario-type-tag">Search</span>
        </div>
        <div className="stack-list">
          {activeAlerts.length ? activeAlerts.map((alert) => (
            <button
              key={alert.id}
              className={`stack-card selectable-card ${selectedAlert?.id === alert.id ? 'is-selected' : ''}`}
              onClick={() => setSelectedAlertId(alert.id)}
              type="button"
            >
              <div className="stack-title-row">
                <strong>{alert.title}</strong>
                <div className="stack-tag-row">
                  <span className={`severity-tag severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span>
                  {alert.warehouseCode ? <span className="scenario-type-tag">{alert.warehouseCode}</span> : null}
                </div>
              </div>
              <p>{alert.description}</p>
              <p className="muted-text">{alert.impactSummary}</p>
              <p className="action-line">Recommended action: {alert.recommendedAction}</p>
            </button>
          )) : <EmptyState>No active alerts. This page becomes the command lane when operational risk starts forming.</EmptyState>}
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card" id="alerts-response">
            <div className="stack-title-row">
              <strong>Severity posture</strong>
              <span className="scenario-type-tag">{activeAlerts.length}</span>
            </div>
            <div className="utility-metric-grid">
              <div><span>Critical</span><strong>{criticalAlertCount}</strong></div>
              <div><span>High</span><strong>{highAlertCount}</strong></div>
              <div><span>Warehouses hit</span><strong>{new Set(activeAlerts.map((alert) => alert.warehouseCode).filter(Boolean)).size}</strong></div>
              <div><span>Actionable</span><strong>{activeAlerts.filter((alert) => Boolean(alert.recommendedAction)).length}</strong></div>
            </div>
            <p className="muted-text">Use severity, warehouse, and actionability together to decide what the team needs to resolve first.</p>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Alert response model</strong>
              <span className="scenario-type-tag">Action-first</span>
            </div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>What is wrong</strong>
                <p>Alerts explain the operational issue, the affected lane, and the likely impact before teams need to drill further.</p>
              </div>
              <div className="signal-list-item">
                <strong>What to do next</strong>
                <p>Every strong alert should point the team toward the next action, whether that is replenishment, transfer, replay, escalation, or approval.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
      <Panel>
        <div className="panel-header">
          <div><p className="panel-kicker">Selected alert</p><h2>Impact and next step</h2></div>
          <span className="panel-badge notification-badge">{selectedAlert ? selectedAlert.severity : 'Clear'}</span>
        </div>
        {selectedAlert ? (
          <div className="stack-list">
            <div className="stack-card">
              <div className="stack-title-row">
                <strong>{selectedAlert.title}</strong>
                <span className={`severity-tag severity-${selectedAlert.severity.toLowerCase()}`}>{selectedAlert.severity}</span>
              </div>
              <p>{selectedAlert.description}</p>
              <p className="muted-text">{selectedAlert.impactSummary}</p>
              <p className="muted-text">Warehouse {selectedAlert.warehouseCode || 'Tenant-wide'} | {formatTimestamp(selectedAlert.createdAt)}</p>
              <p className="action-line">Action: {selectedAlert.recommendedAction}</p>
            </div>
          </div>
        ) : <EmptyState>Select an alert from the list to review the likely impact and response path.</EmptyState>}
      </Panel>
    </section>
  )
}
