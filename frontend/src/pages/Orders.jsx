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

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Live order operations</strong>
            <p>
              Keep warehouse flow, fulfillment linkage, and order value moving through one command surface.
              The highest-pressure lanes should be obvious before teams need to drill into downstream details.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Warehouse-scoped</span>
              <span className="workspace-meta-pill">Fulfillment-aware</span>
              <span className="workspace-meta-pill">Recent live flow</span>
            </div>
          </div>
          <div className="ops-command-actions">
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

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Recent order queue</strong>
              <span className="scenario-type-tag">{orderCards.length ? 'Live' : 'Quiet'}</span>
            </div>
            <div className="signal-list">
              {orderCards.length ? orderCards.map((order) => (
                <button
                  key={order.id}
                  className={`signal-list-item selectable-card system-select-card ${selectedOrder?.id === order.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedOrderId(order.id)}
                  type="button"
                >
                  <div className="stack-title-row">
                    <strong>{order.externalOrderId}</strong>
                    <span className="order-total">{currency.format(order.totalAmount)}</span>
                  </div>
                  <p>{order.warehouseName} | {order.itemCount} units</p>
                  <p className="muted-text">{formatTimestamp(order.createdAt)}</p>
                  {order.relatedFulfillment ? (
                    <p className="muted-text">
                      Fulfillment {formatCodeLabel(order.relatedFulfillment.fulfillmentStatus)}
                      {' | '}
                      Dispatch due {formatRelativeHours(order.relatedFulfillment.hoursUntilDispatchDue)}
                    </p>
                  ) : (
                    <p className="muted-text">Awaiting fulfillment lane linkage.</p>
                  )}
                </button>
              )) : (
                <EmptyState>
                  No recent orders are visible yet. As inbound demand arrives, this queue becomes the live order surface for the workspace.
                </EmptyState>
              )}
            </div>
          </article>

          <article className="stack-card section-card" id="orders-focus">
            <div className="stack-title-row">
              <strong>Selected order lane</strong>
              <span className="scenario-type-tag">{selectedOrder ? selectedOrder.warehouseCode : 'Waiting'}</span>
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
                <p>Give operators one fast place to understand live order movement, warehouse concentration, and fulfillment risk without opening a separate admin tool.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
