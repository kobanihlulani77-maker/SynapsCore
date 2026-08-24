import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { MetricCard } from '../components/Card'
import OperationalGuidance from '../components/OperationalGuidance'

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
    pageLoading,
    pageError,
  } = context

  if (!isAuthenticated || !isAlertsPage) {
    return null
  }

  const selectedAlert = activeAlerts.find((alert) => alert.id === selectedAlertId) || activeAlerts[0]
  const criticalAlertCount = activeAlerts.filter((alert) => alert.severity === 'CRITICAL').length
  const highAlertCount = activeAlerts.filter((alert) => alert.severity === 'HIGH').length
  const warehouseHitCount = new Set(activeAlerts.map((alert) => alert.warehouseCode).filter(Boolean)).size
  const sortedAlerts = [...activeAlerts].sort((left, right) => {
    const severityDelta = (severityPriority[right.severity] || 0) - (severityPriority[left.severity] || 0)
    if (severityDelta) {
      return severityDelta
    }
    return new Date(right.createdAt || 0).getTime() - new Date(left.createdAt || 0).getTime()
  })
  const mostSevereAlert = [...activeAlerts].sort((left, right) => (severityPriority[right.severity] || 0) - (severityPriority[left.severity] || 0))[0]
  const responsePosture = criticalAlertCount
    ? 'Critical response'
    : highAlertCount
      ? 'Elevated response'
      : activeAlerts.length
        ? 'Monitor'
        : 'Clear'
  const actionableAlertCount = activeAlerts.filter((alert) => Boolean(alert.recommendedAction)).length

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

        <div className="workflow-decision-hero attention-ops-hero">
          <div className="workflow-decision-copy">
            <strong>Operational warning center</strong>
            <p>
              Severity, warehouse impact, and recommended action should all be visible together so operators can respond
              calmly and quickly instead of translating raw incidents into next steps.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">{responsePosture}</span>
              <span className="workspace-meta-pill">Assign during triage</span>
              <span className="workspace-meta-pill">{actionableAlertCount} action guided</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>First decision</span>
              <strong>{mostSevereAlert ? `${mostSevereAlert.severity} alert first` : 'No active warning'}</strong>
              <p>{mostSevereAlert ? mostSevereAlert.title : 'The alert lane stays quiet until operational risk forms.'}</p>
            </div>
            <div className="workflow-action-card">
              <span>Ownership</span>
              <strong>Operator triage required</strong>
              <p>SynapseCore exposes the issue and suggested response; the team still assigns the human owner.</p>
            </div>
            <div className="ops-command-actions">
              <button className="secondary-button" onClick={() => mostSevereAlert && setSelectedAlertId(mostSevereAlert.id)} disabled={!mostSevereAlert} type="button">
                Focus highest severity
              </button>
            </div>
          </div>
        </div>

        <OperationalGuidance
          stateLabel={pageLoading ? 'Loading feed' : pageError ? 'Unavailable' : 'Active feed'}
          stateTone={pageError ? 'status-failure' : pageLoading ? 'status-partial' : 'status-success'}
          stateDetail={pageLoading ? 'The active alert feed is still loading.' : pageError ? 'The alert read is unavailable; do not interpret the visible count as zero.' : `${activeAlerts.length} active alert record${activeAlerts.length === 1 ? '' : 's'} returned for this workspace.`}
          attention={activeAlerts.length ? `${actionableAlertCount} alert${actionableAlertCount === 1 ? '' : 's'} include a recommended response; severity and affected warehouse still need operator review.` : 'No active alert records are present in the current feed.'}
          nextAction={selectedAlert ? 'Inspect the selected condition, then follow its evidence path to Inventory, Orders, Integrations, Runtime, or a governed scenario.' : 'Wait for an active record or review Runtime if the workspace itself appears unhealthy.'}
          evidence="Alert records expose condition, timing, impact, and available recommendation context; they do not establish ownership or causation."
          role="Alert response remains an operator responsibility; no alert owner is assigned by this page."
          limitation="This surface is an active-alert feed. Resolved or historical alert state is not represented as an actionable queue here."
        />

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Active alerts" value={activeAlerts.length} accent="amber" note="Operational warnings currently shaping the workspace response queue." />
          <MetricCard label="Critical" value={criticalAlertCount} accent="rose" note="Alerts that need the fastest operational response." />
          <MetricCard label="High" value={highAlertCount} accent="orange" note="Warnings with meaningful risk but slightly more room to react." />
          <MetricCard label="Warehouses hit" value={warehouseHitCount} accent="blue" note="Warehouse lanes currently touched by active alerts." />
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Response queue</strong>
              <span className="scenario-type-tag">{activeAlerts.length ? responsePosture : 'Quiet'}</span>
            </div>
            <p className="muted-text">Sorted by severity so the riskiest operational lane is never visually buried.</p>
            <div className="signal-list">
              {sortedAlerts.length ? sortedAlerts.map((alert) => (
                <button
                  key={alert.id}
                  className={`signal-list-item selectable-card system-select-card attention-queue-card ${selectedAlert?.id === alert.id ? 'is-selected' : ''}`}
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
                  <div className="attention-card-meta">
                    <span>Owner: assign during triage</span>
                    <span>Status: {alert.status || 'ACTIVE'}</span>
                  </div>
                  <p className="action-line">Recommended action: {alert.recommendedAction}</p>
                </button>
              )) : (
                <EmptyState>
                  No active alerts. This page becomes the warning center as soon as live operational pressure starts forming.
                </EmptyState>
              )}
            </div>
          </article>

          <article className="stack-card section-card workflow-selected-panel attention-detail-panel" id="alerts-response">
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
                  <div><span>Status</span><strong>{selectedAlert.status || 'Active'}</strong></div>
                  <div><span>Created</span><strong>{formatTimestamp(selectedAlert.createdAt)}</strong></div>
                  <div><span>Owner</span><strong>Assign</strong></div>
                  <div><span>Next step</span><strong>{selectedAlert.recommendedAction ? 'Action' : 'Review'}</strong></div>
                </div>
                <p className="action-line">Action: {selectedAlert.recommendedAction}</p>
                {selectedAlert.policyExplanation ? <p className="muted-text">Policy context: {selectedAlert.policyExplanation}</p> : null}
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
              <div><span>Actionable</span><strong>{actionableAlertCount}</strong></div>
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
