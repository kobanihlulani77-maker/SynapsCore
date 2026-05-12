import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { MetricCard } from '../components/Card'

const severityPriority = {
  CRITICAL: 3,
  HIGH: 2,
  MEDIUM: 1,
  LOW: 0,
}

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
  const warehouseHitCount = new Set(activeAlerts.map((alert) => alert.warehouseCode).filter(Boolean)).size
  const mostSevereAlert = [...activeAlerts].sort((left, right) => (severityPriority[right.severity] || 0) - (severityPriority[left.severity] || 0))[0]

  return (
    <section className="content-grid alerts-center-grid">
      <Panel wide id="alerts-feed">
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Alerts center</p>
            <h2>Operational warnings in one lane</h2>
          </div>
          <span className="panel-badge alert-badge">{activeAlerts.length}</span>
        </div>

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Operational warning center</strong>
            <p>
              Severity, warehouse impact, and recommended action should all be visible together so operators can respond
              calmly and quickly instead of translating raw incidents into next steps.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Severity led</span>
              <span className="workspace-meta-pill">Warehouse scoped</span>
              <span className="workspace-meta-pill">Action guided</span>
            </div>
          </div>
          <div className="ops-command-actions">
            <button className="secondary-button" onClick={() => mostSevereAlert && setSelectedAlertId(mostSevereAlert.id)} disabled={!mostSevereAlert} type="button">
              Focus highest severity
            </button>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Active alerts" value={activeAlerts.length} accent="amber" note="Operational warnings currently shaping the workspace response queue." />
          <MetricCard label="Critical" value={criticalAlertCount} accent="rose" note="Alerts that need the fastest operational response." />
          <MetricCard label="High" value={highAlertCount} accent="orange" note="Warnings with meaningful risk but slightly more room to react." />
          <MetricCard label="Warehouses hit" value={warehouseHitCount} accent="blue" note="Warehouse lanes currently touched by active alerts." />
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Active alert queue</strong>
              <span className="scenario-type-tag">{activeAlerts.length ? 'Live' : 'Quiet'}</span>
            </div>
            <div className="signal-list">
              {activeAlerts.length ? activeAlerts.map((alert) => (
                <button
                  key={alert.id}
                  className={`signal-list-item selectable-card system-select-card ${selectedAlert?.id === alert.id ? 'is-selected' : ''}`}
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
              )) : (
                <EmptyState>
                  No active alerts. This page becomes the warning center as soon as live operational pressure starts forming.
                </EmptyState>
              )}
            </div>
          </article>

          <article className="stack-card section-card" id="alerts-response">
            <div className="stack-title-row">
              <strong>Selected alert</strong>
              <span className="scenario-type-tag">{selectedAlert ? selectedAlert.severity : 'Clear'}</span>
            </div>
            {selectedAlert ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedAlert.title}</strong>
                  <p>{selectedAlert.description}</p>
                  <p className="muted-text">{selectedAlert.impactSummary}</p>
                </div>
                <div className="utility-metric-grid">
                  <div><span>Severity</span><strong>{selectedAlert.severity}</strong></div>
                  <div><span>Warehouse</span><strong>{selectedAlert.warehouseCode || 'Tenant-wide'}</strong></div>
                  <div><span>Status</span><strong>Active</strong></div>
                  <div><span>Created</span><strong>{formatTimestamp(selectedAlert.createdAt)}</strong></div>
                </div>
                <p className="action-line">Action: {selectedAlert.recommendedAction}</p>
              </div>
            ) : (
              <EmptyState>
                Select an alert from the queue to review likely impact, severity, and the next response path.
              </EmptyState>
            )}
          </article>
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Severity posture</strong>
              <span className="scenario-type-tag">{activeAlerts.length}</span>
            </div>
            <div className="utility-metric-grid">
              <div><span>Critical</span><strong>{criticalAlertCount}</strong></div>
              <div><span>High</span><strong>{highAlertCount}</strong></div>
              <div><span>Warehouses hit</span><strong>{warehouseHitCount}</strong></div>
              <div><span>Actionable</span><strong>{activeAlerts.filter((alert) => Boolean(alert.recommendedAction)).length}</strong></div>
            </div>
            <p className="muted-text">Use severity, affected warehouse scope, and actionability together to decide what the team needs to resolve first.</p>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Response guidance</strong>
              <span className="scenario-type-tag">Action first</span>
            </div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>What this page should do</strong>
                <p>Explain the operational issue, affected lane, and likely impact before teams need to leave the workspace for supporting context.</p>
              </div>
              <div className="signal-list-item">
                <strong>What a strong alert looks like</strong>
                <p>Every alert should point toward the next action, whether that means replenishment, transfer, replay, escalation, or decision review.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
