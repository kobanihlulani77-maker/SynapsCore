import EmptyState from '../components/EmptyState'
import LoadingState from '../components/LoadingState'
import Panel from '../components/Panel'
import { AlertCard, MetricCard } from '../components/Card'
import ActionPanel from '../components/ActionPanel'
import ActivityFeed from '../components/ActivityFeed'

export default function DashboardPage({ context }) {
  const {
    isAuthenticated,
    isDashboardPage,
    warehouseOptions,
    snapshot,
    fulfillmentOverview,
    activeAlerts,
    urgentActions,
    navigateToPage,
    setSelectedAlertId,
    setSelectedRecommendationId,
    setSelectedScenarioId,
    pendingApprovalScenarios,
    runtime,
    systemIncidents,
    utilityTimeline,
    formatCodeLabel,
    formatTimestamp,
    formatBuildValue,
    getRuntimeStatusClassName,
    enabledConnectorCount,
    pendingReplayCount,
    pageLoading,
  } = context

  if (!isAuthenticated || !isDashboardPage) {
    return null
  }

  const warehousePressureCards = warehouseOptions.slice(0, 4).map((warehouse) => {
    const warehouseInventory = snapshot.inventory.filter((item) => item.warehouseCode === warehouse.code)
    const warehouseFulfillment = fulfillmentOverview.activeFulfillments.filter((task) => task.warehouseCode === warehouse.code)
    const alertCount = activeAlerts.filter((alert) => alert.warehouseCode === warehouse.code).length
    return {
      code: warehouse.code,
      name: warehouse.name,
      lowStockCount: warehouseInventory.filter((item) => item.lowStock).length,
      backlogCount: warehouseFulfillment.length,
      delayedCount: warehouseFulfillment.filter((task) => task.fulfillmentStatus === 'DELAYED').length,
      alertCount,
    }
  })

  const dashboardAlertPreview = activeAlerts.slice(0, 4)
  const dashboardRecommendationPreview = snapshot.recommendations.slice(0, 4)
  const dashboardApprovalPreview = pendingApprovalScenarios.slice(0, 4)
  const runtimeStatusLabel = runtime ? formatCodeLabel(runtime.overallStatus) : 'Loading'
  const incidentSeverityCount = systemIncidents.filter((incident) => ['CRITICAL', 'HIGH'].includes(incident.severity)).length
  const recentChanges = utilityTimeline.slice(0, 4).map((item) => ({
    id: item.id,
    title: item.title,
    body: item.meta,
    meta: formatTimestamp(item.timestamp),
  }))
  const hasDashboardPayload = urgentActions.length
    || warehousePressureCards.length
    || dashboardAlertPreview.length
    || dashboardRecommendationPreview.length
    || dashboardApprovalPreview.length
    || utilityTimeline.length
    || systemIncidents.length

  if (pageLoading && !hasDashboardPayload) {
    return <LoadingState label="Loading command center..." />
  }

  return (
    <>
      <section className="summary-grid">
        <MetricCard label="Orders" value={snapshot.summary?.recentOrderCount ?? snapshot.recentOrders.length} accent="amber" note="Live order activity in the current window" />
        <MetricCard label="Risk" value={snapshot.summary?.lowStockItems ?? snapshot.inventory.filter((item) => item.lowStock).length} accent="orange" note="Inventory lanes under active pressure" />
        <MetricCard label="Alerts" value={activeAlerts.length} accent="rose" note="Warnings requiring ownership" />
        <MetricCard label="Uptime posture" value={runtime ? formatCodeLabel(runtime.readinessState) : 'Loading'} accent="teal" note="System acceptance and readiness state" />
      </section>
      <section className="content-grid dashboard-command-grid">
        <ActionPanel
          title="Urgent operational actions"
          kicker="Act now"
          badge={<span className="panel-badge recommendation-badge">{urgentActions.length}</span>}
          actions={urgentActions.map((action) => ({
            id: action.id,
            title: action.title,
            tag: action.kicker,
            note: action.note,
            onClick: () => navigateToPage(action.target),
          }))}
          emptyMessage="No urgent actions are waiting right now. SynapseCore is still watching live state and risk drift."
        />
        <Panel id="dashboard-live-state">
          <div className="panel-header">
            <div><p className="panel-kicker">Risk heat</p><h2>Pressure by location</h2></div>
            <span className="panel-badge alert-badge">{warehousePressureCards.length}</span>
          </div>
          <div className="stack-list">
            {warehousePressureCards.length ? warehousePressureCards.map((warehouse) => (
              <div key={warehouse.code} className="stack-card">
                <div className="stack-title-row">
                  <strong>{warehouse.name}</strong>
                  <span className={`status-tag ${warehouse.delayedCount || warehouse.alertCount ? 'status-failure' : warehouse.lowStockCount || warehouse.backlogCount ? 'status-partial' : 'status-success'}`}>
                    {warehouse.delayedCount || warehouse.alertCount ? 'Hot' : warehouse.lowStockCount || warehouse.backlogCount ? 'Watch' : 'Stable'}
                  </span>
                </div>
                <p>{warehouse.code}</p>
                <p className="muted-text">{warehouse.alertCount} alerts | {warehouse.lowStockCount} low stock | {warehouse.backlogCount} backlog | {warehouse.delayedCount} delayed</p>
              </div>
            )) : <EmptyState>Warehouse heat will appear as soon as inventory and fulfillment signals are active.</EmptyState>}
          </div>
        </Panel>
      </section>
      <section className="content-grid">
        <Panel wide>
          <div className="panel-header">
            <div><p className="panel-kicker">Decision lanes</p><h2>Alerts and recommendations that need ownership</h2></div>
            <span className="panel-badge notification-badge">{dashboardAlertPreview.length + dashboardRecommendationPreview.length}</span>
          </div>
          <div className="experience-grid experience-grid-split">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Urgent alerts</strong>
                <button className="ghost-button" onClick={() => navigateToPage('alerts')} type="button">Open Alerts</button>
              </div>
              <div className="signal-list">
                {dashboardAlertPreview.length ? dashboardAlertPreview.map((alert) => (
                  <button
                    key={alert.id}
                    className="dashboard-card-button"
                    onClick={() => {
                      setSelectedAlertId(alert.id)
                      navigateToPage('alerts')
                    }}
                    type="button"
                  >
                    <AlertCard
                      title={alert.title}
                      body={alert.impactSummary}
                      tone={alert.severity.toLowerCase() === 'critical' ? 'failure' : 'partial'}
                      meta={`${alert.warehouseCode || 'Tenant-wide'} | ${formatTimestamp(alert.createdAt)}`}
                      action={<span className={`severity-tag severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span>}
                    />
                  </button>
                )) : <EmptyState>No urgent alert pressure right now. This lane will light up as soon as risk needs immediate ownership.</EmptyState>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Recommendation queue</strong>
                <button className="ghost-button" onClick={() => navigateToPage('recommendations')} type="button">Open Recommendations</button>
              </div>
              <div className="signal-list">
                {dashboardRecommendationPreview.length ? dashboardRecommendationPreview.map((recommendation) => (
                  <button
                    key={recommendation.id}
                    className="signal-list-item selectable-card system-select-card"
                    onClick={() => {
                      setSelectedRecommendationId(recommendation.id)
                      navigateToPage('recommendations')
                    }}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{recommendation.title}</strong>
                      <span className={`priority-tag priority-${recommendation.priority.toLowerCase()}`}>{recommendation.priority}</span>
                    </div>
                    <p>{recommendation.description}</p>
                    <p className="muted-text">{recommendation.warehouseCode || 'Tenant-wide'} | {formatTimestamp(recommendation.createdAt)}</p>
                  </button>
                )) : <EmptyState>No immediate action guidance is waiting. The queue will fill when SynapseCore detects meaningful operational pressure.</EmptyState>}
              </div>
            </article>
          </div>
        </Panel>
      </section>
      <section className="content-grid">
        <Panel wide>
          <div className="panel-header">
            <div><p className="panel-kicker">What changed recently</p><h2>Activity, approvals, and system health overview</h2></div>
            <span className="panel-badge audit-badge">{utilityTimeline.length + dashboardApprovalPreview.length + systemIncidents.length}</span>
          </div>
          <div className="experience-grid experience-grid-three">
            <ActivityFeed
              kicker="Live activity stream"
              title="Recent activity"
              items={recentChanges}
              emptyMessage="Business events, incidents, and audit entries will appear here once the workspace is processing live activity."
              action={<button className="ghost-button" onClick={() => navigateToPage('audit')} type="button">Open Audit</button>}
            />
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Approval attention</strong>
                <button className="ghost-button" onClick={() => navigateToPage('approvals')} type="button">Open Approvals</button>
              </div>
              <div className="signal-list">
                {dashboardApprovalPreview.length ? dashboardApprovalPreview.map((scenario) => (
                  <button
                    key={scenario.id}
                    className="signal-list-item selectable-card system-select-card"
                    onClick={() => {
                      setSelectedScenarioId(scenario.id)
                      navigateToPage('approvals')
                    }}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{scenario.title}</strong>
                      <span className={`status-tag ${scenario.overdue ? 'status-failure' : 'status-partial'}`}>
                        {scenario.overdue ? 'Overdue' : formatCodeLabel(scenario.approvalStage || scenario.approvalStatus)}
                      </span>
                    </div>
                    <p>{scenario.summary}</p>
                    <p className="muted-text">{scenario.reviewOwner || 'Unassigned'} | Due {formatTimestamp(scenario.approvalDueAt)}</p>
                  </button>
                )) : <EmptyState>No plans are waiting on review right now. Decision routing will appear here when operators start sending scenarios through governance.</EmptyState>}
              </div>
            </article>
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>System health overview</strong>
                <button className="ghost-button" onClick={() => navigateToPage('runtime')} type="button">Open Runtime</button>
              </div>
              <div className="signal-list">
                <div className="signal-list-item">
                  <div className="stack-title-row">
                    <strong>Runtime trust</strong>
                    <span className={`status-tag ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'status-partial'}`}>{runtimeStatusLabel}</span>
                  </div>
                  <p>{runtime ? `${formatCodeLabel(runtime.readinessState)} readiness with ${runtime.backbone.pendingDispatchCount} pending dispatch item${runtime.backbone.pendingDispatchCount === 1 ? '' : 's'}.` : 'Runtime telemetry is still loading for this workspace.'}</p>
                  <p className="muted-text">{runtime?.build?.version ? `Build ${runtime.build.version} | Commit ${formatBuildValue(runtime.build.commit).slice(0, 7)}` : 'Build metadata will appear when runtime telemetry responds.'}</p>
                </div>
                <div className="signal-list-item">
                  <div className="stack-title-row">
                    <strong>Incident pressure</strong>
                    <span className={`status-tag ${incidentSeverityCount ? 'status-failure' : systemIncidents.length ? 'status-partial' : 'status-success'}`}>
                      {systemIncidents.length ? `${systemIncidents.length} active` : 'Clear'}
                    </span>
                  </div>
                  <p>{systemIncidents.length ? `${incidentSeverityCount} high-severity incident${incidentSeverityCount === 1 ? '' : 's'} and ${pendingReplayCount} replay item${pendingReplayCount === 1 ? '' : 's'} are currently affecting trust.` : 'No active runtime incidents are visible right now.'}</p>
                  <p className="muted-text">{snapshot.integrationConnectors.length ? `${enabledConnectorCount}/${snapshot.integrationConnectors.length} connectors enabled across the tenant integration surface.` : 'Connector posture will appear when external systems are attached.'}</p>
                </div>
              </div>
            </article>
          </div>
        </Panel>
      </section>
    </>
  )
}
