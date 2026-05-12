import EmptyState from '../components/EmptyState'
import LoadingState from '../components/LoadingState'
import Panel from '../components/Panel'
import { MetricCard } from '../components/Card'
import ActivityFeed from '../components/ActivityFeed'

export default function DashboardPage({ context }) {
  const {
    isAuthenticated,
    isDashboardPage,
    warehouseOptions,
    snapshot,
    catalogState,
    canManageTenantAccess,
    accessAdminUsers,
    signedInSession,
    connectionState,
    liveClockLabel,
    pageStatus,
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
  const dashboardReplayPreview = snapshot.integrationReplayQueue.slice(0, 4)
  const dashboardActivityItems = utilityTimeline.slice(0, 6).map((item) => ({
    id: item.id,
    title: item.title,
    body: item.meta,
    meta: formatTimestamp(item.timestamp),
  }))

  const resolvedRecentOrderCount = Math.max(snapshot.summary?.recentOrderCount ?? 0, snapshot.recentOrders.length)
  const resolvedLowStockCount = Math.max(snapshot.summary?.lowStockItems ?? 0, snapshot.inventory.filter((item) => item.lowStock).length)
  const resolvedActiveAlertCount = Math.max(snapshot.summary?.activeAlerts ?? 0, activeAlerts.length)
  const resolvedRecommendationCount = Math.max(snapshot.summary?.recommendationsCount ?? 0, snapshot.recommendations.length)
  const runtimeStatusLabel = runtime ? formatCodeLabel(runtime.overallStatus) : 'Loading'
  const readinessLabel = runtime ? formatCodeLabel(runtime.readinessState) : 'Monitoring'
  const incidentSeverityCount = systemIncidents.filter((incident) => ['CRITICAL', 'HIGH'].includes(incident.severity)).length
  const degradedConnectorCount = snapshot.integrationConnectors.filter((connector) => connector.enabled && connector.healthStatus && connector.healthStatus !== 'HEALTHY' && connector.healthStatus !== 'UP').length
  const hasDashboardPayload = urgentActions.length
    || warehousePressureCards.length
    || dashboardAlertPreview.length
    || dashboardRecommendationPreview.length
    || dashboardApprovalPreview.length
    || dashboardReplayPreview.length
    || utilityTimeline.length
    || systemIncidents.length

  const showWorkspaceKickoff = !catalogState.products.length
    && !snapshot.inventory.length
    && !snapshot.integrationConnectors.length
    && !snapshot.recentOrders.length
    && !snapshot.recommendations.length
    && !activeAlerts.length

  const kickoffTasks = [
    {
      title: 'Add the first product',
      body: 'Define the first SKU so inventory, orders, and scenarios share the same operational reference.',
      complete: catalogState.products.length > 0,
      actionLabel: 'Open Catalog',
      target: 'catalog',
      tag: 'Catalog',
    },
    {
      title: 'Bring inventory online',
      body: 'Load the first stock position or warehouse signal so the command center can start measuring pressure.',
      complete: snapshot.inventory.length > 0,
      actionLabel: 'Open Inventory',
      target: 'inventory',
      tag: 'Inventory',
    },
    {
      title: 'Invite operators',
      body: 'Set up planners, operators, and approvers who will run the workspace under real operating conditions.',
      complete: canManageTenantAccess ? accessAdminUsers.length > 0 : Boolean(signedInSession),
      actionLabel: canManageTenantAccess ? 'Open Users' : 'Open Profile',
      target: canManageTenantAccess ? 'users' : 'profile',
      tag: 'Access',
    },
    {
      title: 'Configure integrations',
      body: 'Connect the first inbound system and keep replay/recovery ready before exceptions begin landing.',
      complete: snapshot.integrationConnectors.length > 0,
      actionLabel: 'Open Integrations',
      target: 'integrations',
      tag: 'Systems',
    },
    {
      title: 'Run the first scenario',
      body: 'Use the decision lab to model an operational change before it touches live flow.',
      complete: pendingApprovalScenarios.length > 0 || snapshot.recentScenarios.length > 0,
      actionLabel: 'Open Scenarios',
      target: 'scenarios',
      tag: 'Control',
    },
  ]
  const completedKickoffCount = kickoffTasks.filter((task) => task.complete).length

  const guidanceActions = showWorkspaceKickoff
    ? kickoffTasks.filter((task) => !task.complete).map((task) => ({
      id: task.title,
      title: task.title,
      note: task.body,
      target: task.target,
      tag: task.tag,
    }))
    : urgentActions.slice(0, 5)

  const executiveSignals = [
    {
      label: 'Orders',
      value: resolvedRecentOrderCount,
      accent: 'amber',
      note: resolvedRecentOrderCount ? 'Live order activity in the current workspace window' : 'No live order flow has reached the workspace yet',
    },
    {
      label: 'Inventory Risk',
      value: resolvedLowStockCount,
      accent: 'orange',
      note: resolvedLowStockCount ? `${warehouseOptions.length} warehouse lanes visible with low-stock pressure` : 'No low-stock pressure is active right now',
    },
    {
      label: 'Replay Queue',
      value: pendingReplayCount,
      accent: 'rose',
      note: pendingReplayCount ? 'Failed inbound work is waiting for operator recovery' : 'Recovery queue is clear',
    },
    {
      label: 'Alerts',
      value: resolvedActiveAlertCount,
      accent: 'rose',
      note: resolvedActiveAlertCount ? 'Warnings are waiting for ownership' : 'No active alert pressure',
    },
    {
      label: 'Recommendations',
      value: resolvedRecommendationCount,
      accent: 'teal',
      note: resolvedRecommendationCount ? 'Action guidance is ready for the operating team' : 'No immediate action guidance is queued',
    },
    {
      label: 'Runtime & Incidents',
      value: incidentSeverityCount ? `${incidentSeverityCount} hot` : readinessLabel,
      accent: incidentSeverityCount ? 'rose' : 'blue',
      note: incidentSeverityCount ? `${systemIncidents.length} total incidents impacting trust posture` : `${runtimeStatusLabel} runtime trust posture` ,
    },
  ]

  const laneCards = [
    {
      title: 'Fulfillment and order flow',
      tone: resolvedRecentOrderCount || fulfillmentOverview.backlogCount ? 'status-partial' : 'status-success',
      status: resolvedRecentOrderCount || fulfillmentOverview.backlogCount ? 'Live lane' : 'Waiting',
      body: resolvedRecentOrderCount
        ? `${resolvedRecentOrderCount} recent orders, ${fulfillmentOverview.backlogCount} backlog items, and ${fulfillmentOverview.delayedShipmentCount} delayed shipments are shaping the current flow.`
        : 'As orders arrive, this lane becomes the lead surface for live flow, warehouse assignment, and fulfillment pressure.',
      metrics: [
        `${resolvedRecentOrderCount} recent orders`,
        `${fulfillmentOverview.backlogCount} backlog`,
        `${fulfillmentOverview.delayedShipmentCount} delayed`,
      ],
      actionLabel: 'Open Orders',
      target: 'orders',
    },
    {
      title: 'Inventory health',
      tone: resolvedLowStockCount ? 'status-partial' : 'status-success',
      status: resolvedLowStockCount ? 'Watch' : 'Stable',
      body: resolvedLowStockCount
        ? `${resolvedLowStockCount} low-stock items are active across ${warehouseOptions.length} operating location${warehouseOptions.length === 1 ? '' : 's'}.`
        : 'Inventory intelligence is ready to surface low-stock risk, velocity drift, and stockout windows as data arrives.',
      metrics: [
        `${resolvedLowStockCount} low stock`,
        `${snapshot.inventory.length} inventory rows`,
        `${warehouseOptions.length} sites`,
      ],
      actionLabel: 'Open Inventory',
      target: 'inventory',
    },
    {
      title: 'Integration and replay recovery',
      tone: pendingReplayCount || degradedConnectorCount ? 'status-partial' : 'status-success',
      status: pendingReplayCount ? 'Recovery active' : degradedConnectorCount ? 'Degraded' : 'Healthy',
      body: pendingReplayCount
        ? `${pendingReplayCount} replay item${pendingReplayCount === 1 ? '' : 's'} need recovery attention.`
        : snapshot.integrationConnectors.length
          ? `${enabledConnectorCount}/${snapshot.integrationConnectors.length} connectors are enabled across the workspace integration surface.`
          : 'Connector lanes will appear here once the workspace starts integrating with external systems.',
      metrics: [
        `${enabledConnectorCount}/${snapshot.integrationConnectors.length || 0} enabled`,
        `${pendingReplayCount} replay items`,
        `${degradedConnectorCount} degraded`,
      ],
      actionLabel: 'Open Replay Queue',
      target: 'replay',
    },
    {
      title: 'Approval and scenario queue',
      tone: pendingApprovalScenarios.length ? 'status-partial' : 'status-success',
      status: pendingApprovalScenarios.length ? 'Attention needed' : 'Clear',
      body: pendingApprovalScenarios.length
        ? `${pendingApprovalScenarios.length} plan${pendingApprovalScenarios.length === 1 ? '' : 's'} are waiting for approval routing or execution readiness.`
        : 'Scenario planning and approval routing are ready when the team starts modelling operational changes.',
      metrics: [
        `${pendingApprovalScenarios.length} pending`,
        `${snapshot.recentScenarios.length} recent scenarios`,
        `${dashboardApprovalPreview.length} priority items`,
      ],
      actionLabel: 'Open Approvals',
      target: 'approvals',
    },
    {
      title: 'Alerts and guidance',
      tone: resolvedActiveAlertCount || resolvedRecommendationCount ? 'status-partial' : 'status-success',
      status: resolvedActiveAlertCount ? 'Act now' : resolvedRecommendationCount ? 'Guided' : 'Stable',
      body: resolvedActiveAlertCount
        ? `${resolvedActiveAlertCount} alerts and ${resolvedRecommendationCount} recommendations are shaping operator attention right now.`
        : resolvedRecommendationCount
          ? `${resolvedRecommendationCount} recommendations are ready even though the alert lane is calm.`
          : 'The command center will start ranking what matters now as soon as operational pressure becomes visible.',
      metrics: [
        `${resolvedActiveAlertCount} alerts`,
        `${resolvedRecommendationCount} recommendations`,
        `${urgentActions.length} urgent actions`,
      ],
      actionLabel: 'Open Alerts',
      target: 'alerts',
    },
  ]

  if (pageLoading && !hasDashboardPayload) {
    return <LoadingState label="Loading command center..." />
  }

  return (
    <>
      <section className="dashboard-command-header">
        <article className="dashboard-command-identity">
          <p className="panel-kicker">Company command center</p>
          <h2>{signedInSession?.tenantName || 'Operational workspace'}</h2>
          <p>{pageStatus}</p>
          <div className="dashboard-command-meta">
            <span className="workspace-meta-pill">Workspace {signedInSession?.tenantCode || 'Unknown'}</span>
            <span className={`workspace-meta-pill ${connectionState === 'live' ? 'status-live' : 'status-missing'}`}>{connectionState === 'live' ? 'Realtime live' : formatCodeLabel(connectionState)}</span>
            <span className="workspace-meta-pill">Operator {signedInSession?.displayName || 'Unknown'}</span>
          </div>
        </article>
        <article className="dashboard-command-health">
          <div className="dashboard-health-card">
            <span>Live system</span>
            <strong>{connectionState === 'live' ? 'Connected' : formatCodeLabel(connectionState)}</strong>
            <p>{snapshot.generatedAt ? `Snapshot ${formatTimestamp(snapshot.generatedAt)}` : `Monitoring ${liveClockLabel}`}</p>
          </div>
          <div className="dashboard-health-card">
            <span>Runtime trust</span>
            <strong>{runtimeStatusLabel}</strong>
            <p>{runtime?.build?.version ? `Build ${runtime.build.version} | ${readinessLabel}` : `${readinessLabel} | Runtime metadata pending`}</p>
          </div>
          <div className="dashboard-health-card">
            <span>Primary actions</span>
            <div className="dashboard-health-actions">
              <button className="secondary-button" onClick={() => navigateToPage('alerts')} type="button">Open Alerts</button>
              <button className="ghost-button" onClick={() => navigateToPage('runtime')} type="button">Open Runtime</button>
            </div>
          </div>
        </article>
      </section>

      {showWorkspaceKickoff ? (
        <section className="content-grid">
          <Panel wide className="workspace-kickoff-panel">
            <div className="workspace-kickoff-shell">
              <article className="workspace-kickoff-hero">
                <p className="panel-kicker">First workspace run</p>
                <h2>Your operations workspace is live and ready for setup.</h2>
                <p>
                  SynapseCore is ready to become the company command center, but this workspace is still in its first-run state.
                  Start with catalog, inventory, operators, integrations, and the first controlled scenario so live pressure can become visible.
                </p>
                <div className="workspace-kickoff-progress">
                  <span>{completedKickoffCount}/{kickoffTasks.length} setup lanes complete</span>
                  <strong>{completedKickoffCount === kickoffTasks.length ? 'Workspace ready for live operations' : 'Guided onboarding in progress'}</strong>
                </div>
              </article>
              <article className="workspace-kickoff-checklist">
                {kickoffTasks.map((task) => (
                  <button
                    key={task.title}
                    className={`workspace-kickoff-card ${task.complete ? 'is-complete' : ''}`}
                    onClick={() => navigateToPage(task.target)}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{task.title}</strong>
                      <span className={`status-tag ${task.complete ? 'status-success' : 'status-partial'}`}>
                        {task.complete ? 'Complete' : 'Next'}
                      </span>
                    </div>
                    <p>{task.body}</p>
                    <span className="workspace-kickoff-link">{task.actionLabel}</span>
                  </button>
                ))}
              </article>
            </div>
          </Panel>
        </section>
      ) : null}

      <section className="summary-grid dashboard-signal-grid">
        {executiveSignals.map((signal) => (
          <MetricCard
            key={signal.label}
            label={signal.label}
            value={signal.value}
            accent={signal.accent}
            note={signal.note}
            className="dashboard-signal-card"
          />
        ))}
      </section>

      <section className="content-grid dashboard-operations-grid">
        <Panel wide className="dashboard-lanes-panel" kicker="Operational lanes" title="What the team is operating right now">
          <div className="dashboard-lane-grid">
            {laneCards.map((lane) => (
              <button key={lane.title} className="dashboard-lane-card" onClick={() => navigateToPage(lane.target)} type="button">
                <div className="stack-title-row">
                  <strong>{lane.title}</strong>
                  <span className={`status-tag ${lane.tone}`}>{lane.status}</span>
                </div>
                <p>{lane.body}</p>
                <div className="dashboard-lane-metrics">
                  {lane.metrics.map((metric) => <span key={metric}>{metric}</span>)}
                </div>
                <span className="workspace-kickoff-link">{lane.actionLabel}</span>
              </button>
            ))}
          </div>
        </Panel>

        <Panel className="dashboard-guidance-panel" kicker="What needs attention" title={showWorkspaceKickoff ? 'First setup actions' : 'Next best actions'}>
          <div className="stack-list compact-stack-list">
            {guidanceActions.length ? guidanceActions.map((action) => (
              <button
                key={action.id || action.title}
                className="utility-action dashboard-guidance-card"
                onClick={() => {
                  if (action.id?.toString().startsWith('alert-')) {
                    setSelectedAlertId(action.id.replace('alert-', ''))
                  }
                  if (action.id?.toString().startsWith('recommendation-')) {
                    setSelectedRecommendationId(action.id.replace('recommendation-', ''))
                  }
                  navigateToPage(action.target)
                }}
                type="button"
              >
                <span>{action.tag || 'Next action'}</span>
                <strong>{action.title}</strong>
                <p>{action.note}</p>
              </button>
            )) : <EmptyState>No immediate operational action pressure right now. SynapseCore is still monitoring the workspace for the next meaningful move.</EmptyState>}
          </div>
        </Panel>
      </section>

      <section className="content-grid dashboard-activity-grid">
        <ActivityFeed
          kicker="Live activity rail"
          title="Recent business and operator changes"
          items={dashboardActivityItems}
          emptyMessage="Business events, incidents, and audit traces will begin streaming here as the workspace starts operating."
          action={<button className="ghost-button" onClick={() => navigateToPage('audit')} type="button">Open Audit</button>}
        />

        <Panel className="dashboard-side-panel" kicker="Replay and connector posture" title="Recovery and integration lane">
          <div className="signal-list">
            {dashboardReplayPreview.length ? dashboardReplayPreview.map((replay) => (
              <button
                key={replay.id}
                className="signal-list-item selectable-card"
                onClick={() => navigateToPage('replay')}
                type="button"
              >
                <div className="stack-title-row">
                  <strong>{replay.externalOrderId}</strong>
                  <span className={`status-tag ${replay.status === 'PENDING' ? 'status-partial' : 'status-failure'}`}>{replay.status}</span>
                </div>
                <p>{replay.failureMessage || 'Replay item requires operational recovery.'}</p>
                <p className="muted-text">{replay.sourceSystem} | {replay.warehouseCode || 'Tenant-wide'} | {formatTimestamp(replay.createdAt)}</p>
              </button>
            )) : (
              <EmptyState>
                No failed inbound work is active right now. Recovery stays ready when connectors or inbound systems start generating exceptions.
              </EmptyState>
            )}
          </div>
        </Panel>

        <Panel className="dashboard-side-panel" kicker="Approvals and runtime" title="Queues that can slow live control">
          <div className="signal-list">
            {dashboardApprovalPreview.length ? dashboardApprovalPreview.map((scenario) => (
              <button
                key={scenario.id}
                className="signal-list-item selectable-card"
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
            )) : systemIncidents.length ? systemIncidents.slice(0, 3).map((incident) => (
              <button key={incident.incidentKey} className="signal-list-item selectable-card" onClick={() => navigateToPage('runtime')} type="button">
                <div className="stack-title-row">
                  <strong>{incident.title}</strong>
                  <span className={`status-tag ${getRuntimeStatusClassName(incident.severity === 'CRITICAL' ? 'DOWN' : 'UNKNOWN')}`}>{formatCodeLabel(incident.severity)}</span>
                </div>
                <p>{incident.detail}</p>
                <p className="muted-text">{incident.context} | {formatTimestamp(incident.createdAt)}</p>
              </button>
            )) : (
              <EmptyState>
                No approvals or runtime incidents are slowing the workspace right now. Governance and trust lanes will appear here as operational pressure grows.
              </EmptyState>
            )}
          </div>
        </Panel>
      </section>

      <section className="content-grid">
        <Panel wide className="dashboard-response-panel" kicker="Alerts and response guidance" title="Where the operating team should look next">
          <div className="dashboard-response-grid">
            <article className="stack-card section-card">
              <div className="stack-title-row">
                <strong>Alert pressure</strong>
                <button className="ghost-button" onClick={() => navigateToPage('alerts')} type="button">Open Alerts</button>
              </div>
              <div className="signal-list">
                {dashboardAlertPreview.length ? dashboardAlertPreview.map((alert) => (
                  <button
                    key={alert.id}
                    className="signal-list-item selectable-card"
                    onClick={() => {
                      setSelectedAlertId(alert.id)
                      navigateToPage('alerts')
                    }}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{alert.title}</strong>
                      <span className={`severity-tag severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span>
                    </div>
                    <p>{alert.impactSummary}</p>
                    <p className="muted-text">{alert.warehouseCode || 'Tenant-wide'} | {formatTimestamp(alert.createdAt)}</p>
                  </button>
                )) : <EmptyState>No urgent alert pressure right now. This lane lights up when risk needs immediate operator ownership.</EmptyState>}
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
                    className="signal-list-item selectable-card"
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
                )) : <EmptyState>No immediate action guidance is queued. Recommendations will appear here when SynapseCore detects meaningful pressure.</EmptyState>}
              </div>
            </article>
          </div>
        </Panel>
      </section>
    </>
  )
}
