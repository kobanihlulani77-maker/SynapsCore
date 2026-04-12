import { SummaryCard } from '../components/Card'
import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'

export default function FulfillmentPage({ context }) {
  const {
    isAuthenticated,
    isFulfillmentPage,
    delayedFulfillments,
    fulfillmentOverview,
    warehouseOptions,
    formatCodeLabel,
    formatRelativeHours,
    getFulfillmentStatusClassName,
    enabledConnectorCount,
    snapshot,
    pendingReplayCount,
  } = context

  if (!isAuthenticated || !isFulfillmentPage) return null

  const selectedFulfillment = delayedFulfillments[0] || fulfillmentOverview.activeFulfillments[0]
  const lanePressure = warehouseOptions.map((warehouse) => {
    const tasks = fulfillmentOverview.activeFulfillments.filter((task) => task.warehouseCode === warehouse.code)
    return {
      code: warehouse.code,
      name: warehouse.name,
      total: tasks.length,
      delayed: tasks.filter((task) => task.fulfillmentStatus === 'DELAYED').length,
      exceptions: tasks.filter((task) => task.fulfillmentStatus === 'EXCEPTION').length,
      dispatched: tasks.filter((task) => task.fulfillmentStatus === 'DISPATCHED').length,
    }
  }).filter((lane) => lane.total)

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Fulfillment and logistics</p><h2>Backlog, dispatch, and delivery pressure</h2></div>
          <span className="panel-badge fulfillment-badge">{fulfillmentOverview.activeFulfillments.length}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Backlog" value={fulfillmentOverview.backlogCount} accent="amber" />
          <SummaryCard label="Overdue dispatch" value={fulfillmentOverview.overdueDispatchCount} accent="orange" />
          <SummaryCard label="Delayed shipments" value={fulfillmentOverview.delayedShipmentCount} accent="rose" />
          <SummaryCard label="At risk" value={fulfillmentOverview.atRiskCount} accent="teal" />
        </div>
        <div className="warehouse-grid">
          {delayedFulfillments.length ? delayedFulfillments.map((task) => (
            <article key={task.id} className="warehouse-health-card">
              <div className="stack-title-row">
                <strong>{task.externalOrderId}</strong>
                <span className={`status-tag ${getFulfillmentStatusClassName(task.fulfillmentStatus)}`}>{formatCodeLabel(task.fulfillmentStatus)}</span>
              </div>
              <p>{task.warehouseName}</p>
              <p className="muted-text">Dispatch due {formatRelativeHours(task.hoursUntilDispatchDue)} | Delivery {formatRelativeHours(task.hoursUntilDeliveryDue)}</p>
              <p>{task.impactSummary}</p>
            </article>
          )) : <EmptyState>No delayed or high-risk fulfillment lanes right now.</EmptyState>}
        </div>
        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Lane pressure</strong><span className="scenario-type-tag">{lanePressure.length}</span></div>
            <div className="signal-list">
              {lanePressure.length ? lanePressure.map((lane) => (
                <div key={lane.code} className="signal-list-item">
                  <div className="stack-title-row">
                    <strong>{lane.name}</strong>
                    <span className={`status-tag ${lane.delayed || lane.exceptions ? 'status-failure' : lane.total ? 'status-partial' : 'status-success'}`}>{lane.delayed || lane.exceptions ? 'Pressed' : 'Flowing'}</span>
                  </div>
                  <p>{lane.total} active lanes | {lane.dispatched} dispatched</p>
                  <p className="muted-text">{lane.delayed} delayed | {lane.exceptions} exceptions</p>
                </div>
              )) : <EmptyState>Fulfillment lane pressure will appear once dispatch and delivery activity is flowing.</EmptyState>}
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Selected fulfillment detail</strong>
              <span className={`status-tag ${selectedFulfillment ? getFulfillmentStatusClassName(selectedFulfillment.fulfillmentStatus) : 'status-partial'}`}>{selectedFulfillment ? formatCodeLabel(selectedFulfillment.fulfillmentStatus) : 'Waiting'}</span>
            </div>
            {selectedFulfillment ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedFulfillment.externalOrderId}</strong>
                  <p>{selectedFulfillment.warehouseName}</p>
                  <p className="muted-text">Dispatch due {formatRelativeHours(selectedFulfillment.hoursUntilDispatchDue)} | Delivery due {formatRelativeHours(selectedFulfillment.hoursUntilDeliveryDue)}</p>
                  <p className="muted-text">{selectedFulfillment.impactSummary}</p>
                </div>
              </div>
            ) : <EmptyState>Select a fulfillment lane to inspect dispatch timing, delivery pressure, and the likely operational impact.</EmptyState>}
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Delivery support posture</strong><span className="scenario-type-tag">{enabledConnectorCount}/{snapshot.integrationConnectors.length || 0} live</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Connector support</strong>
                <p>{enabledConnectorCount} enabled connectors supporting inbound operational flow.</p>
                <p className="muted-text">{pendingReplayCount} replay item{pendingReplayCount === 1 ? '' : 's'} waiting for recovery.</p>
              </div>
              <div className="signal-list-item">
                <strong>Fulfillment posture</strong>
                <p>{fulfillmentOverview.delayedShipmentCount} delayed shipments | {fulfillmentOverview.overdueDispatchCount} overdue dispatch lanes</p>
                <p className="muted-text">Use the recommendations and replay lanes to recover delivery pressure before it spreads.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}

