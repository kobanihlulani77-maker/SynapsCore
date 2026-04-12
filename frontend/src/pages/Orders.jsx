import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { SummaryCard } from '../components/Card'

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
  const loadedWarehouses = new Set(orderCards.map((order) => order.warehouseCode).filter(Boolean)).size

  return (
    <section className="content-grid orders-center-grid">
      <Panel wide id="orders-stream">
        <div className="panel-header">
          <div><p className="panel-kicker">Orders operations</p><h2>Monitor the live order stream</h2></div>
          <span className="panel-badge order-badge">{orderCards.length}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Recent orders" value={summary?.recentOrderCount ?? orderCards.length} accent="amber" />
          <SummaryCard label="Delayed linked lanes" value={orderCards.filter((order) => order.relatedFulfillment?.fulfillmentStatus === 'DELAYED').length} accent="rose" />
          <SummaryCard label="Backlog-linked orders" value={orderCards.filter((order) => order.relatedFulfillment).length} accent="orange" />
          <SummaryCard label="Warehouses under flow" value={warehouseOptions.length} accent="blue" />
        </div>
        <div className="stack-list">
          {orderCards.length ? orderCards.map((order) => (
            <button
              key={order.id}
              className={`stack-card selectable-card ${selectedOrder?.id === order.id ? 'is-selected' : ''}`}
              onClick={() => setSelectedOrderId(order.id)}
              type="button"
            >
              <div className="stack-title-row">
                <strong>{order.externalOrderId}</strong>
                <span className="order-total">{currency.format(order.totalAmount)}</span>
              </div>
              <p>{order.warehouseName} | {order.itemCount} units</p>
              <p className="muted-text">{formatTimestamp(order.createdAt)}</p>
              {order.relatedFulfillment ? <p className="muted-text">Fulfillment {formatCodeLabel(order.relatedFulfillment.fulfillmentStatus)} | Dispatch due {formatRelativeHours(order.relatedFulfillment.hoursUntilDispatchDue)}</p> : <p className="muted-text">Awaiting fulfillment lane linkage.</p>}
            </button>
          )) : <EmptyState>No recent orders are visible yet. As order events arrive, this page becomes the live operational queue.</EmptyState>}
        </div>
        <div className="experience-grid experience-grid-split">
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
                  <p className="muted-text">{selectedOrder.relatedFulfillment ? `Fulfillment ${formatCodeLabel(selectedOrder.relatedFulfillment.fulfillmentStatus)} | Delivery due ${formatRelativeHours(selectedOrder.relatedFulfillment.hoursUntilDeliveryDue)}` : 'Fulfillment lane is still being linked.'}</p>
                </div>
              </div>
            ) : <EmptyState>As order events arrive, the lead order lane appears here with fulfillment impact and SLA posture.</EmptyState>}
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Order flow posture</strong>
              <span className="scenario-type-tag">{orderCards.length}</span>
            </div>
            <div className="utility-metric-grid">
              <div><span>Warehouses under flow</span><strong>{loadedWarehouses}</strong></div>
              <div><span>Linked fulfillment</span><strong>{orderCards.filter((order) => order.relatedFulfillment).length}</strong></div>
              <div><span>Delayed lanes</span><strong>{orderCards.filter((order) => order.relatedFulfillment?.fulfillmentStatus === 'DELAYED').length}</strong></div>
              <div><span>High value</span><strong>{orderCards.filter((order) => order.totalAmount >= 500).length}</strong></div>
            </div>
            <p className="muted-text">This lane helps operators understand which order flow is driving downstream inventory, fulfillment, and alert pressure.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}
