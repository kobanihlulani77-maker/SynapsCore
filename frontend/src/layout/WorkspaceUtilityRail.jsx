export default function WorkspaceUtilityRail({ context }) {
  const {
    isAuthenticated,
    recommendationNow,
    recommendationSoon,
    recommendationWatch,
    activeAlerts,
    selectedAlertId,
    selectedRecommendationId,
    selectedOrderId,
    selectedInventoryId,
    currentPage,
    snapshot,
    formatCodeLabel,
    currency,
    formatRelativeHours,
    lowStockInventory,
    highRiskInventory,
    fastMovingInventory,
    warehouseOptions,
    fulfillmentOverview,
    selectedRuntimeIncident,
    runtime,
    formatTimestamp,
    formatMetricValue,
    systemIncidents,
    pendingReplayCount,
    selectedAuditTrace,
    accessAdminOperators,
    accessAdminUsers,
    workspaceAdmin,
    selectedAccessSubject,
    selectedTenantPortfolio,
    tenantDirectoryState,
    frontendBuildVersion,
    apiUrl,
    wsUrl,
    effectivePageMeta,
    pageStatusMap,
    navigateToPage,
    connectionState,
    urgentActions,
    utilityTimeline,
    passwordChangeRequired,
    passwordRotationRequired,
    signedInSession,
    signedInRoles,
    signedInWarehouseScopes,
    enabledConnectorCount,
    scenarioHistoryItems,
    selectedHistoryScenario,
    pendingApprovalScenarios,
    overdueScenarios,
    approvedScenarios,
    rejectedScenarios,
    selectedApprovalScenario,
    escalatedScenarios,
    selectedEscalationScenario,
  } = context

  if (!isAuthenticated) {
    return null
  }

  const recommendationCandidates = [...recommendationNow, ...recommendationSoon, ...recommendationWatch]
  const focusedAlert = activeAlerts.find((alert) => alert.id === selectedAlertId) || activeAlerts[0]
  const focusedRecommendation = recommendationCandidates.find((recommendation) => recommendation.id === selectedRecommendationId) || recommendationCandidates[0]
  const focusedOrder = snapshot.recentOrders.find((order) => order.id === selectedOrderId) || snapshot.recentOrders[0]
  const focusedInventory = snapshot.inventory.find((item) => item.id === selectedInventoryId) || highRiskInventory[0] || lowStockInventory[0] || snapshot.inventory[0]

  const pageUtilityContext = (() => {
    switch (currentPage) {
      case 'alerts':
        return {
          kicker: 'Alert focus',
          state: focusedAlert ? formatCodeLabel(focusedAlert.severity) : 'Clear',
          title: focusedAlert ? focusedAlert.title : 'No active alerts',
          detail: focusedAlert ? focusedAlert.recommendedAction || focusedAlert.impactSummary : 'This lane stays quiet until operational risk begins forming.',
          metrics: [
            { label: 'Critical', value: activeAlerts.filter((alert) => alert.severity === 'CRITICAL').length },
            { label: 'High', value: activeAlerts.filter((alert) => alert.severity === 'HIGH').length },
            { label: 'Warehouses', value: new Set(activeAlerts.map((alert) => alert.warehouseCode).filter(Boolean)).size },
            { label: 'Actionable', value: activeAlerts.filter((alert) => Boolean(alert.recommendedAction)).length },
          ],
          actions: [{ title: 'Open recommendations', note: 'Move from warning to next action.', target: 'recommendations' }],
        }
      case 'recommendations':
        return {
          kicker: 'Action focus',
          state: focusedRecommendation ? focusedRecommendation.priority : 'Clear',
          title: focusedRecommendation ? focusedRecommendation.title : 'No active recommendations',
          detail: focusedRecommendation ? focusedRecommendation.description : 'Guided actions will appear here as the system starts ranking what operators should do next.',
          metrics: [
            { label: 'Urgent', value: recommendationNow.length },
            { label: 'Soon', value: recommendationSoon.length },
            { label: 'Watch', value: recommendationWatch.length },
            { label: 'Alerts', value: activeAlerts.length },
          ],
          actions: [{ title: 'Open alerts', note: 'See the risk that is driving action guidance.', target: 'alerts' }],
        }
      case 'orders':
        return {
          kicker: 'Order focus',
          state: focusedOrder ? focusedOrder.warehouseCode || 'Tenant-wide' : 'Waiting',
          title: focusedOrder ? focusedOrder.externalOrderId : 'No active order lane',
          detail: focusedOrder ? `${focusedOrder.warehouseName} | ${focusedOrder.itemCount} units | ${currency.format(focusedOrder.totalAmount)}` : 'Orders will appear here as soon as the live flow starts moving.',
          metrics: [
            { label: 'Recent', value: snapshot.recentOrders.length },
            { label: 'Warehouses', value: new Set(snapshot.recentOrders.map((order) => order.warehouseCode).filter(Boolean)).size },
            { label: 'Delayed', value: snapshot.recentOrders.filter((order) => fulfillmentOverview.activeFulfillments.some((task) => task.externalOrderId === order.externalOrderId && task.fulfillmentStatus === 'DELAYED')).length },
            { label: 'Backlog', value: fulfillmentOverview.backlogCount },
          ],
          actions: [{ title: 'Open fulfillment', note: 'Follow the downstream dispatch and delivery lane.', target: 'fulfillment' }],
        }
      case 'inventory':
        return {
          kicker: 'Inventory focus',
          state: focusedInventory ? formatCodeLabel(focusedInventory.riskLevel) : 'Waiting',
          title: focusedInventory ? focusedInventory.productName : 'No active inventory focus',
          detail: focusedInventory ? `${focusedInventory.warehouseName} | ${focusedInventory.quantityAvailable} available | Stockout ${formatRelativeHours(focusedInventory.hoursToStockout)}` : 'Inventory intelligence becomes active as stock posture and velocity start diverging.',
          metrics: [
            { label: 'Low stock', value: lowStockInventory.length },
            { label: 'High risk', value: highRiskInventory.length },
            { label: 'Fast movers', value: fastMovingInventory.length },
            { label: 'Warehouses', value: warehouseOptions.length },
          ],
          actions: [{ title: 'Open locations', note: 'View site-level pressure behind the inventory lane.', target: 'locations' }],
        }
      case 'runtime':
        return {
          kicker: 'Runtime focus',
          state: selectedRuntimeIncident ? formatCodeLabel(selectedRuntimeIncident.severity) : (runtime?.overallStatus || 'Loading'),
          title: selectedRuntimeIncident ? selectedRuntimeIncident.title : 'Runtime health board',
          detail: selectedRuntimeIncident
            ? `${selectedRuntimeIncident.context} | ${formatTimestamp(selectedRuntimeIncident.createdAt)}`
            : runtime
              ? `Readiness ${formatCodeLabel(runtime.readinessState)} | Pending ${runtime.backbone.pendingDispatchCount} | Failed ${runtime.backbone.failedDispatchCount}`
              : 'Runtime, queue, and incident posture will appear here once the trust surface is loaded.',
          metrics: [
            { label: 'Incidents', value: systemIncidents.length },
            { label: 'Queued', value: runtime?.backbone?.pendingDispatchCount ?? 0 },
            { label: 'Failed', value: runtime?.backbone?.failedDispatchCount ?? 0 },
            { label: 'Metrics', value: formatMetricValue(runtime?.metrics?.dispatchProcessed) },
          ],
          actions: [{ title: 'Open releases', note: 'Check deployment fingerprint beside runtime trust.', target: 'releases' }],
        }
      case 'audit':
        return {
          kicker: 'Trace focus',
          state: selectedAuditTrace ? formatCodeLabel(selectedAuditTrace.traceType) : 'Waiting',
          title: selectedAuditTrace
            ? selectedAuditTrace.traceType === 'audit'
              ? formatCodeLabel(selectedAuditTrace.action)
              : formatCodeLabel(selectedAuditTrace.eventType)
            : 'No trace selected',
          detail: selectedAuditTrace
            ? selectedAuditTrace.traceType === 'audit'
              ? `${selectedAuditTrace.targetType} | ${selectedAuditTrace.targetRef} | ${formatTimestamp(selectedAuditTrace.createdAt)}`
              : `${selectedAuditTrace.source} | ${formatTimestamp(selectedAuditTrace.createdAt)}`
            : 'Audit and business timeline entries will appear here as soon as protected actions and live events accumulate.',
          metrics: [
            { label: 'Audit', value: snapshot.auditLogs.length },
            { label: 'Events', value: snapshot.recentEvents.length },
            { label: 'Replay', value: pendingReplayCount },
            { label: 'Incidents', value: systemIncidents.length },
          ],
          actions: [{ title: 'Open runtime', note: 'Pair traceability with the live trust lane.', target: 'runtime' }],
        }
      case 'users':
        return {
          kicker: 'Access focus',
          state: selectedAccessSubject ? formatCodeLabel(selectedAccessSubject.subjectType) : 'Waiting',
          title: selectedAccessSubject
            ? selectedAccessSubject.subjectType === 'operator'
              ? selectedAccessSubject.displayName
              : selectedAccessSubject.fullName
            : 'No access subject selected',
          detail: selectedAccessSubject
            ? selectedAccessSubject.subjectType === 'operator'
              ? `${selectedAccessSubject.actorName} | ${selectedAccessSubject.roles.map((role) => formatCodeLabel(role)).join(', ') || 'No roles'}`
              : `${selectedAccessSubject.username} | ${selectedAccessSubject.operatorDisplayName || selectedAccessSubject.operatorActorName}`
            : 'Operator lanes and user accounts will surface here as tenant admins start shaping workspace access.',
          metrics: [
            { label: 'Operators', value: accessAdminOperators.length },
            { label: 'Users', value: accessAdminUsers.length },
            { label: 'Resets', value: workspaceAdmin?.supportDiagnostics?.activeUsersRequiringPasswordChange || 0 },
            { label: 'Blocked', value: workspaceAdmin?.supportDiagnostics?.activeUsersBlockedByInactiveOperator || 0 },
          ],
          actions: [{ title: 'Open settings', note: 'Tune workspace policy alongside access lanes.', target: 'settings' }],
        }
      case 'settings':
        return {
          kicker: 'Workspace focus',
          state: workspaceAdmin?.tenantName ? 'Editable' : 'Loading',
          title: workspaceAdmin?.tenantName || 'Workspace settings',
          detail: workspaceAdmin
            ? `${workspaceAdmin.warehouses?.length || 0} warehouse lanes | ${workspaceAdmin.connectors?.length || 0} connectors | Rotation ${workspaceAdmin.securitySettings?.passwordRotationDays || 0} days`
            : 'Workspace profile, security posture, and connector ownership will appear here once tenant admin data loads.',
          metrics: [
            { label: 'Warehouses', value: workspaceAdmin?.warehouses?.length || 0 },
            { label: 'Connectors', value: workspaceAdmin?.connectors?.length || 0 },
            { label: 'Rotation', value: workspaceAdmin?.securitySettings?.passwordRotationDays || 0 },
            { label: 'Timeout', value: workspaceAdmin?.securitySettings?.sessionTimeoutMinutes || 0 },
          ],
          actions: [{ title: 'Open users', note: 'Shift from policy to who can act inside it.', target: 'users' }],
        }
      case 'platform':
        return {
          kicker: 'Platform focus',
          state: selectedTenantPortfolio ? selectedTenantPortfolio.code : (runtime?.overallStatus || 'Loading'),
          title: selectedTenantPortfolio ? selectedTenantPortfolio.name : 'Platform overview',
          detail: selectedTenantPortfolio ? selectedTenantPortfolio.description || 'Operational workspace ready for rollout.' : 'Cross-tenant health, queue posture, and release trust appear here for platform operators.',
          metrics: [
            { label: 'Tenants', value: tenantDirectoryState.items.length },
            { label: 'Incidents', value: systemIncidents.length },
            { label: 'Queued', value: runtime?.backbone?.pendingDispatchCount ?? 0 },
            { label: 'Replay', value: pendingReplayCount },
          ],
          actions: [{ title: 'Open releases', note: 'Review build and environment trust next.', target: 'releases' }],
        }
      case 'releases':
        return {
          kicker: 'Release focus',
          state: runtime?.overallStatus || 'Loading',
          title: `Backend ${runtime?.build?.version || 'Waiting'}`,
          detail: `Frontend ${frontendBuildVersion || 'Waiting'} | API ${apiUrl || 'Default'} | WS ${wsUrl || 'Default'}`,
          metrics: [
            { label: 'Profile', value: runtime?.activeProfiles?.join(', ') || 'Loading' },
            { label: 'Readiness', value: runtime ? formatCodeLabel(runtime.readinessState) : 'Loading' },
            { label: 'API', value: apiUrl ? 'Configured' : 'Default' },
            { label: 'WS', value: wsUrl ? 'Configured' : 'Default' },
          ],
          actions: [{ title: 'Open runtime', note: 'Check live trust after release posture.', target: 'runtime' }],
        }
      case 'scenario-history':
        return {
          kicker: 'Scenario focus',
          state: selectedHistoryScenario ? formatCodeLabel(selectedHistoryScenario.type) : 'Waiting',
          title: selectedHistoryScenario ? selectedHistoryScenario.title : 'No saved plan selected',
          detail: selectedHistoryScenario ? `${selectedHistoryScenario.warehouseCode || 'Tenant-wide'} | ${formatCodeLabel(selectedHistoryScenario.approvalStatus)} | ${selectedHistoryScenario.recommendedOption || 'Decision path recorded'}` : 'Saved plans, revisions, and compare memory will become actionable here as teams explore scenarios.',
          metrics: [
            { label: 'History', value: scenarioHistoryItems.length },
            { label: 'Revisions', value: scenarioHistoryItems.filter((scenario) => scenario.revisionNumber).length },
            { label: 'Executable', value: scenarioHistoryItems.filter((scenario) => scenario.executable).length },
            { label: 'Escalated', value: scenarioHistoryItems.filter((scenario) => scenario.slaEscalated).length },
          ],
          actions: [{ title: 'Open planner', note: 'Shift from saved memory back into the decision lab.', target: 'scenarios' }],
        }
      case 'approvals':
        return {
          kicker: 'Approval focus',
          state: selectedApprovalScenario ? formatCodeLabel(selectedApprovalScenario.approvalStage || selectedApprovalScenario.approvalStatus) : 'Clear',
          title: selectedApprovalScenario ? selectedApprovalScenario.title : 'No approval selected',
          detail: selectedApprovalScenario ? `${selectedApprovalScenario.reviewOwner || 'Unassigned'} | Due ${formatTimestamp(selectedApprovalScenario.approvalDueAt)}` : 'Pending, approved, rejected, and overdue decisions will appear here as soon as control queues begin filling.',
          metrics: [
            { label: 'Pending', value: pendingApprovalScenarios.length },
            { label: 'Overdue', value: overdueScenarios.length },
            { label: 'Approved', value: approvedScenarios.length },
            { label: 'Rejected', value: rejectedScenarios.length },
          ],
          actions: [{ title: 'Open escalations', note: 'Check the inbox when the queue starts breaching SLA.', target: 'escalations' }],
        }
      case 'escalations':
        return {
          kicker: 'Escalation focus',
          state: selectedEscalationScenario ? 'Escalated' : systemIncidents.length ? 'Incident pressure' : 'Clear',
          title: selectedEscalationScenario ? selectedEscalationScenario.title : systemIncidents[0]?.title || 'No escalations active',
          detail: selectedEscalationScenario ? `Escalated to ${selectedEscalationScenario.slaEscalatedTo || 'Monitoring'} | Due ${formatTimestamp(selectedEscalationScenario.approvalDueAt)}` : systemIncidents[0]?.detail || 'The escalation inbox remains quiet until SLA or trust pressure needs immediate ownership.',
          metrics: [
            { label: 'Escalated', value: escalatedScenarios.length },
            { label: 'Acknowledged', value: escalatedScenarios.filter((scenario) => scenario.slaAcknowledged).length },
            { label: 'Incidents', value: systemIncidents.length },
            { label: 'Critical', value: systemIncidents.filter((incident) => ['CRITICAL', 'HIGH'].includes(incident.severity)).length },
          ],
          actions: [{ title: 'Open runtime', note: 'Inspect the trust layer beside the escalation queue.', target: 'runtime' }],
        }
      default:
        return {
          kicker: 'Page focus',
          state: effectivePageMeta.label,
          title: effectivePageMeta.title,
          detail: pageStatusMap[currentPage],
          metrics: [
            { label: 'Focus 1', value: effectivePageMeta.focus[0] || 'Live state' },
            { label: 'Focus 2', value: effectivePageMeta.focus[1] || 'Action' },
            { label: 'Focus 3', value: effectivePageMeta.focus[2] || 'Trust' },
            { label: 'Group', value: effectivePageMeta.group ? formatCodeLabel(effectivePageMeta.group) : 'Workspace' },
          ],
          actions: [],
        }
    }
  })()

  return (
    <aside className="workspace-utility-rail">
      <article className="utility-card" id="workspace-trust-rail">
        <div className="utility-card-header">
          <p className="panel-kicker">Realtime state</p>
          <span className={`utility-state utility-${connectionState}`}>{connectionState === 'live' ? 'Live' : formatCodeLabel(connectionState)}</span>
        </div>
        <strong>{snapshot.simulation.active ? 'Continuous activity is flowing' : 'Monitoring live operating state'}</strong>
        <p className="muted-text">{snapshot.generatedAt ? `Snapshot ${formatTimestamp(snapshot.generatedAt)}` : 'Awaiting the first synchronized snapshot.'}</p>
        <div className="utility-metric-grid">
          <div><span>Alerts</span><strong>{snapshot.alerts.activeAlerts.length}</strong></div>
          <div><span>Actions</span><strong>{snapshot.recommendations.length}</strong></div>
          <div><span>Replay</span><strong>{pendingReplayCount}</strong></div>
          <div><span>Incidents</span><strong>{systemIncidents.length}</strong></div>
        </div>
      </article>
      <article className="utility-card" id="workspace-page-focus">
        <div className="utility-card-header">
          <p className="panel-kicker">{pageUtilityContext.kicker}</p>
          <span className="utility-state utility-good">{pageUtilityContext.state}</span>
        </div>
        <strong>{pageUtilityContext.title}</strong>
        <p className="muted-text">{pageUtilityContext.detail}</p>
        <div className="utility-metric-grid">
          {pageUtilityContext.metrics.map((metric) => (
            <div key={metric.label}>
              <span>{metric.label}</span>
              <strong>{metric.value}</strong>
            </div>
          ))}
        </div>
        {pageUtilityContext.actions.length ? (
          <div className="stack-list compact-stack-list">
            {pageUtilityContext.actions.map((action) => (
              <button key={action.title} className="utility-action" onClick={() => navigateToPage(action.target)} type="button">
                <span>Quick route</span>
                <strong>{action.title}</strong>
                <p>{action.note}</p>
              </button>
            ))}
          </div>
        ) : null}
      </article>
      <article className="utility-card">
        <div className="utility-card-header">
          <p className="panel-kicker">Act now</p>
          <span className="scenario-type-tag">{urgentActions.length ? `${urgentActions.length} items` : 'Stable'}</span>
        </div>
        <div className="stack-list compact-stack-list">
          {urgentActions.length ? urgentActions.map((action) => (
            <button key={action.id} className="utility-action" onClick={() => navigateToPage(action.target)} type="button">
              <span>{action.kicker}</span>
              <strong>{action.title}</strong>
              <p>{action.note}</p>
            </button>
          )) : <div className="empty-state">No immediate operational action pressure right now.</div>}
        </div>
      </article>
      <article className="utility-card">
        <div className="utility-card-header">
          <p className="panel-kicker">Activity stream</p>
          <span className="scenario-type-tag">{utilityTimeline.length}</span>
        </div>
        <div className="utility-timeline">
          {utilityTimeline.length ? utilityTimeline.map((item) => (
            <div key={item.id} className="utility-timeline-item">
              <strong>{item.title}</strong>
              <p>{item.meta}</p>
              <span>{formatTimestamp(item.timestamp)}</span>
            </div>
          )) : <div className="empty-state">Business activity, incidents, and audit events will begin streaming here as the workspace operates.</div>}
        </div>
      </article>
      <article className="utility-card">
        <div className="utility-card-header">
          <p className="panel-kicker">Operator</p>
          <span className={`utility-state ${passwordChangeRequired || passwordRotationRequired ? 'utility-alert' : 'utility-good'}`}>
            {passwordChangeRequired || passwordRotationRequired ? 'Attention' : 'Healthy'}
          </span>
        </div>
        <strong>{signedInSession.displayName}</strong>
        <p className="muted-text">{signedInSession.tenantName || signedInSession.tenantCode} | {signedInSession.actorName}</p>
        <p className="muted-text">Roles {signedInRoles.length ? signedInRoles.map((role) => formatCodeLabel(role)).join(', ') : 'None'}</p>
        <p className="muted-text">Warehouse scope {signedInWarehouseScopes.length ? signedInWarehouseScopes.join(', ') : 'Tenant-wide'}</p>
      </article>
    </aside>
  )
}
