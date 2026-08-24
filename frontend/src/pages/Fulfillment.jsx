import { SummaryCard } from '../components/Card'
import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'
import OperationalGuidance from '../components/OperationalGuidance'

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
    signedInRoles,
    pageLoading,
    pageError,
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
      <Panel wide id="fulfillment-state">
        <div className="panel-header">
          <div><p className="panel-kicker">Fulfillment and logistics</p><h2>Backlog, dispatch, and delivery pressure</h2></div>
          <span className="panel-badge fulfillment-badge">{fulfillmentOverview.activeFulfillments.length}</span>
        </div>
        <OperationalGuidance
          stateLabel={pageLoading ? 'Loading fulfillment' : pageError ? 'Unavailable' : 'Flow read'}
          stateTone={pageError ? 'status-failure' : pageLoading ? 'status-partial' : fulfillmentOverview.delayedShipmentCount ? 'status-partial' : 'status-success'}
          stateDetail={pageLoading ? 'Fulfillment lanes are still loading.' : pageError ? 'The fulfillment read is unavailable; do not interpret visible counts as zero.' : `${fulfillmentOverview.activeFulfillments.length} active fulfillment lane${fulfillmentOverview.activeFulfillments.length === 1 ? '' : 's'} are visible.`}
          attention={fulfillmentOverview.delayedShipmentCount || fulfillmentOverview.overdueDispatchCount ? `${fulfillmentOverview.delayedShipmentCount} delayed shipment${fulfillmentOverview.delayedShipmentCount === 1 ? '' : 's'} and ${fulfillmentOverview.overdueDispatchCount} overdue dispatch lane${fulfillmentOverview.overdueDispatchCount === 1 ? '' : 's'} need review.` : 'No delayed or overdue fulfillment pressure is currently returned.'}
          nextAction={selectedFulfillment ? 'Inspect the affected order and warehouse, then verify connector, inventory, replay, or scenario evidence before changing the operating path.' : 'Monitor the flow; no fulfillment mutation is exposed on this page.'}
          evidence="Fulfillment state reflects SynapseCore records and connector support signals; it does not prove completion in every external source system."
          role={signedInRoles.includes('INTEGRATION_ADMIN') || signedInRoles.includes('INTEGRATION_OPERATOR') ? 'Integration authority governs supported fulfillment writes.' : 'This page is an operational read for the current role; direct fulfillment writes remain governed by integration authority.'}
          limitation="Do not interpret a local fulfillment status as source-authoritative completion without reconciliation."
        />
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
