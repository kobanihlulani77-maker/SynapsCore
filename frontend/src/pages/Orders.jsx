import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { MetricCard } from '../components/Card'

export default function OrdersPage({ context }) {
  const {
    isAuthenticated,
    isOrdersPage,
    snapshot,
    fulfillmentOverview,
    selectedOrderId,
    setSelectedOrderId,
    summary,
    warehouseOptions,
    signedInRoles,
    currency,
    formatCodeLabel,
    formatRelativeHours,
    formatTimestamp,
  } = context

  if (!isAuthenticated || !isOrdersPage) {
    return null
  }

  const orderCards = snapshot.recentOrders.map((order) => {
    const relatedFulfillment = fulfillmentOverview.activeFulfillments.find((task) => task.externalOrderId === order.externalOrderId)
    return {
      ...order,
      relatedFulfillment,
    }
  })
  const selectedOrder = orderCards.find((order) => order.id === selectedOrderId) || orderCards[0]
  const delayedOrders = orderCards.filter((order) => order.relatedFulfillment?.fulfillmentStatus === 'DELAYED')
  const linkedOrders = orderCards.filter((order) => order.relatedFulfillment)
  const highValueOrders = orderCards.filter((order) => order.totalAmount >= 500)
  const loadedWarehouses = new Set(orderCards.map((order) => order.warehouseCode).filter(Boolean)).size
  const newestOrder = orderCards[0]
  const mostPressedOrder = delayedOrders[0] || linkedOrders[0] || newestOrder
  const attentionOrders = [
    ...delayedOrders,
    ...orderCards.filter((order) => !order.relatedFulfillment && !delayedOrders.some((candidate) => candidate.id === order.id)),
    ...highValueOrders.filter((order) => !delayedOrders.some((candidate) => candidate.id === order.id)),
  ].filter((order, index, list) => list.findIndex((candidate) => candidate.id === order.id) === index)
  const orderPosture = delayedOrders.length
    ? 'Attention required'
    : orderCards.length && linkedOrders.length === orderCards.length
      ? 'Processing normally'
      : orderCards.length
        ? 'Awaiting linkage'
        : 'Quiet'
  const orderPostureTone = delayedOrders.length
    ? 'status-failure'
    : orderCards.length && linkedOrders.length === orderCards.length
      ? 'status-success'
      : 'status-partial'
  const orderPostureCopy = delayedOrders.length
    ? `${delayedOrders.length} order lane${delayedOrders.length === 1 ? '' : 's'} have delayed fulfillment evidence. Inspect those before normal order review.`
    : orderCards.length && linkedOrders.length === orderCards.length
      ? 'Visible orders are linked into fulfillment. Continue watching warehouse concentration and delivery posture.'
      : orderCards.length
        ? 'Recent orders are visible, but some are still waiting for downstream fulfillment linkage.'
        : 'No recent order pressure is visible in the current workspace window.'

  return (
    <section className="content-grid orders-center-grid">
      <Panel wide id="orders-stream">
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Orders operations</p>
            <h2>Monitor the live order stream</h2>
          </div>
          <span className="panel-badge order-badge">{orderCards.length}</span>
        </div>

        <div className="workflow-decision-hero daily-ops-hero">
          <div className="workflow-decision-copy">
            <p className="panel-kicker">Order posture</p>
            <div className="runtime-decision-title">
              <span className={`runtime-decision-badge ${orderPostureTone}`}>{orderPosture}</span>
              <h2>{attentionOrders.length ? `${attentionOrders.length} order${attentionOrders.length === 1 ? '' : 's'} need review` : 'Order stream is calm.'}</h2>
            </div>
            <p>{orderPostureCopy}</p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Recent {orderCards.length}</span>
              <span className="workspace-meta-pill">Delayed {delayedOrders.length}</span>
              <span className="workspace-meta-pill">Linked {linkedOrders.length}</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Inspect next</span>
              <strong>{mostPressedOrder?.externalOrderId || 'No order selected'}</strong>
              <p>{mostPressedOrder ? 'Focus the order with delay, fulfillment linkage, or newest activity before reviewing secondary flow posture.' : 'No order action is available until order data arrives.'}</p>
            </div>
            <button className="secondary-button" onClick={() => newestOrder && setSelectedOrderId(newestOrder.id)} disabled={!newestOrder} type="button">
              Inspect newest order
            </button>
            <button className="ghost-button" onClick={() => mostPressedOrder && setSelectedOrderId(mostPressedOrder.id)} disabled={!mostPressedOrder} type="button">
              Focus pressured lane
            </button>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Recent orders" value={summary?.recentOrderCount ?? orderCards.length} accent="amber" note="Orders currently visible in the live command window." />
          <MetricCard label="Delayed linked lanes" value={delayedOrders.length} accent="rose" note="Orders already showing downstream fulfillment delay pressure." />
          <MetricCard label="Linked fulfillment" value={linkedOrders.length} accent="blue" note="Orders that already have a fulfillment lane attached." />
          <MetricCard label="Warehouses under flow" value={warehouseOptions.length} accent="teal" note="Warehouse lanes currently participating in order movement." />
        </div>

        <div className="experience-grid daily-ops-workbench">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>{attentionOrders.length ? 'Attention queue' : 'Recent order queue'}</strong>
              <span className={`status-tag ${orderPostureTone}`}>{attentionOrders.length || orderCards.length}</span>
            </div>
            <div className="signal-list">
              {(attentionOrders.length ? attentionOrders : orderCards).length ? (attentionOrders.length ? attentionOrders : orderCards).map((order) => (
                <button
                  key={order.id}
                  className={`signal-list-item selectable-card system-select-card ${selectedOrder?.id === order.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedOrderId(order.id)}
                  type="button"
                >
                  <div className="stack-title-row">
                    <strong>{order.externalOrderId}</strong>
                    <span className={`status-tag ${order.relatedFulfillment?.fulfillmentStatus === 'DELAYED' ? 'status-failure' : order.relatedFulfillment ? 'status-success' : 'status-partial'}`}>
                      {order.relatedFulfillment ? formatCodeLabel(order.relatedFulfillment.fulfillmentStatus) : 'Awaiting link'}
                    </span>
                  </div>
                  <p>{order.warehouseName} | {order.itemCount} units | {currency.format(order.totalAmount)}</p>
                  {order.relatedFulfillment ? (
                    <p className="muted-text">
                      Dispatch due {formatRelativeHours(order.relatedFulfillment.hoursUntilDispatchDue)} | Created {formatTimestamp(order.createdAt)}
                    </p>
                  ) : (
                    <p className="muted-text">Awaiting fulfillment lane linkage | Created {formatTimestamp(order.createdAt)}</p>
                  )}
                </button>
              )) : (
                <EmptyState>
                  No recent orders are visible yet. As inbound demand arrives, this queue becomes the live order surface for the workspace.
                </EmptyState>
              )}
            </div>
          </article>

          <article className="stack-card section-card workflow-selected-panel" id="orders-focus">
            <div className="stack-title-row">
              <strong>Selected order lane</strong>
              <span className={`status-tag ${selectedOrder?.relatedFulfillment?.fulfillmentStatus === 'DELAYED' ? 'status-failure' : selectedOrder?.relatedFulfillment ? 'status-success' : 'status-partial'}`}>
                {selectedOrder?.relatedFulfillment ? formatCodeLabel(selectedOrder.relatedFulfillment.fulfillmentStatus) : selectedOrder ? 'Awaiting link' : 'Waiting'}
              </span>
            </div>
            {selectedOrder ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedOrder.externalOrderId}</strong>
                  <p>{selectedOrder.warehouseName} | {selectedOrder.itemCount} units | {currency.format(selectedOrder.totalAmount)}</p>
                  <p className="muted-text">Created {formatTimestamp(selectedOrder.createdAt)}</p>
                </div>
                <div className="utility-metric-grid">
                  <div><span>Warehouse</span><strong>{selectedOrder.warehouseCode || 'Unknown'}</strong></div>
                  <div><span>Value tier</span><strong>{selectedOrder.totalAmount >= 500 ? 'High' : 'Standard'}</strong></div>
                  <div><span>Fulfillment</span><strong>{selectedOrder.relatedFulfillment ? formatCodeLabel(selectedOrder.relatedFulfillment.fulfillmentStatus) : 'Pending link'}</strong></div>
                  <div><span>Delivery posture</span><strong>{selectedOrder.relatedFulfillment ? formatRelativeHours(selectedOrder.relatedFulfillment.hoursUntilDeliveryDue) : 'Waiting'}</strong></div>
                </div>
                <p className="muted-text">
                  {selectedOrder.relatedFulfillment
                    ? `Dispatch due ${formatRelativeHours(selectedOrder.relatedFulfillment.hoursUntilDispatchDue)} | Delivery due ${formatRelativeHours(selectedOrder.relatedFulfillment.hoursUntilDeliveryDue)}`
                    : 'This order is visible in the workspace and still waiting for downstream fulfillment linkage.'}
                </p>
                <p className="muted-text">Source relationship: SynapseCore observation only. Source-authoritative or reconciled state is not claimed unless the supporting evidence is reported.</p>
                <div className="workflow-action-band">
                  <div>
                    <strong>{selectedOrder.relatedFulfillment?.fulfillmentStatus === 'DELAYED' ? 'Inspect delayed fulfillment evidence' : 'Inspect order context'}</strong>
                    <p>Use this lane to understand status, timing, warehouse context, and whether related recovery evidence is needed. Direct order/fulfillment mutations remain with the supported integration roles and backend authority.</p>
                  </div>
                  <button className="ghost-button" onClick={() => setSelectedOrderId(selectedOrder.id)} type="button">Keep Selected</button>
                </div>
              </div>
            ) : (
              <EmptyState>
                As order events arrive, the lead order lane appears here with fulfillment impact, warehouse context, and timing pressure.
              </EmptyState>
            )}
          </article>
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Flow posture</strong>
              <span className="scenario-type-tag">{orderCards.length}</span>
            </div>
            <div className="utility-metric-grid">
              <div><span>Warehouses under flow</span><strong>{loadedWarehouses}</strong></div>
              <div><span>Fulfillment linked</span><strong>{linkedOrders.length}</strong></div>
              <div><span>Delayed lanes</span><strong>{delayedOrders.length}</strong></div>
              <div><span>High-value orders</span><strong>{highValueOrders.length}</strong></div>
            </div>
            <p className="muted-text">
              Use warehouse count, linked fulfillment, and delay pressure together to decide whether the live order stream needs routing changes or replay attention.
            </p>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Operator guidance</strong>
              <span className="scenario-type-tag">{delayedOrders.length ? 'Action needed' : 'Watching'}</span>
            </div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>What needs attention first</strong>
                <p>
                  {delayedOrders.length
                    ? `${delayedOrders.length} order lane${delayedOrders.length === 1 ? '' : 's'} are already delayed downstream. Start there before backlog spreads.`
                    : linkedOrders.length
                      ? 'Orders are linked into fulfillment. Watch timing pressure and warehouse concentration before delays form.'
                      : 'Recent orders are visible, but downstream fulfillment linkage is still building. Monitor lane attachment first.'}
                </p>
              </div>
              <div className="signal-list-item">
                <strong>What this page should do</strong>
                <p>Give operators one fast place to understand live order movement, warehouse concentration, and fulfillment risk without opening a separate admin tool. Current role: {signedInRoles.length ? signedInRoles.map((role) => formatCodeLabel(role)).join(', ') : 'Unknown'}.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
