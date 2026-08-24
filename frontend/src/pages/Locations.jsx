import { SummaryCard } from '../components/Card'
import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'
import OperationalGuidance from '../components/OperationalGuidance'

export default function LocationsPage({ context }) {
  const {
    isAuthenticated,
    isLocationsPage,
    warehouseOptions,
    snapshot,
    fulfillmentOverview,
    activeAlerts,
    formatCodeLabel,
    signedInWarehouseScopes,
    signedInRoles,
    pageLoading,
    pageError,
  } = context

  if (!isAuthenticated || !isLocationsPage) return null

  const warehouseHealthCards = warehouseOptions.map((warehouse) => {
    const warehouseInventory = snapshot.inventory.filter((item) => item.warehouseCode === warehouse.code)
    const lowStockCount = warehouseInventory.filter((item) => item.lowStock).length
    const highRiskCount = warehouseInventory.filter((item) => item.riskLevel === 'critical' || item.riskLevel === 'high').length
    const orderLoad = snapshot.recentOrders.filter((order) => order.warehouseCode === warehouse.code).length
    const fulfillmentLoad = fulfillmentOverview.activeFulfillments.filter((task) => task.warehouseCode === warehouse.code)
    const delayedCount = fulfillmentLoad.filter((task) => task.fulfillmentStatus === 'DELAYED').length
    const pressureScore = Math.max(0, 100 - (lowStockCount * 10) - (highRiskCount * 12) - (delayedCount * 12) - (orderLoad * 4))

    return {
      ...warehouse,
      lowStockCount,
      highRiskCount,
      orderLoad,
      backlogCount: fulfillmentLoad.length,
      delayedCount,
      pressureScore,
    }
  })

  const selectedWarehouse = [...warehouseHealthCards].sort((left, right) => (right.delayedCount + right.highRiskCount + right.lowStockCount) - (left.delayedCount + left.highRiskCount + left.lowStockCount))[0]
  const selectedWarehouseInventory = selectedWarehouse ? snapshot.inventory.filter((item) => item.warehouseCode === selectedWarehouse.code) : []
  const selectedWarehouseAlerts = selectedWarehouse ? activeAlerts.filter((alert) => alert.warehouseCode === selectedWarehouse.code).slice(0, 4) : []
  const selectedWarehouseRecommendations = selectedWarehouse ? snapshot.recommendations.filter((recommendation) => recommendation.warehouseCode === selectedWarehouse.code).slice(0, 3) : []

  return (
    <section className="content-grid">
      <Panel wide id="locations-state">
        <div className="panel-header">
          <div><p className="panel-kicker">Locations</p><h2>Operational pressure across sites</h2></div>
          <span className="panel-badge inventory-badge">{warehouseHealthCards.length}</span>
        </div>
        <OperationalGuidance
          stateLabel={pageLoading ? 'Loading locations' : pageError ? 'Unavailable' : 'Scope read'}
          stateTone={pageError ? 'status-failure' : pageLoading ? 'status-partial' : 'status-success'}
          stateDetail={pageLoading ? 'Warehouse scope and operating signals are still loading.' : pageError ? 'The location read is unavailable; do not interpret the visible site count as zero.' : `${warehouseHealthCards.length} warehouse${warehouseHealthCards.length === 1 ? '' : 's'} define the visible operating context.`}
          attention={selectedWarehouse ? `${selectedWarehouse.name} has the highest derived pressure from the current inventory, order, and fulfillment signals.` : 'No warehouse records are available in the current context.'}
          nextAction={selectedWarehouse ? 'Inspect the hottest lane, then follow its alert, recommendation, inventory, order, or fulfillment evidence.' : 'Verify workspace setup and Tenant Admin configuration before treating the location surface as empty.'}
          evidence="Location pressure is derived from the loaded tenant snapshot; it is not a backend health check or source-system reconciliation."
          role={signedInWarehouseScopes.length ? `This operator is restricted to ${signedInWarehouseScopes.join(', ')}.` : signedInRoles.includes('TENANT_ADMIN') ? 'An empty warehouse scope means tenant-wide authority for this session.' : 'An empty warehouse scope means tenant-wide operating scope for this session; configuration authority still depends on role.'}
          limitation="Tenant Admin manages supported warehouse/access configuration; other roles operate within their assigned scope and cannot infer configuration authority from this page."
        />
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Sites" value={warehouseHealthCards.length} accent="blue" />
          <SummaryCard label="Low-stock lanes" value={warehouseHealthCards.filter((warehouse) => warehouse.lowStockCount).length} accent="amber" />
          <SummaryCard label="Backlog sites" value={warehouseHealthCards.filter((warehouse) => warehouse.backlogCount).length} accent="orange" />
          <SummaryCard label="Delayed sites" value={warehouseHealthCards.filter((warehouse) => warehouse.delayedCount).length} accent="rose" />
        </div>
        <div className="warehouse-grid">
          {warehouseHealthCards.length ? warehouseHealthCards.map((warehouse) => (
            <article key={warehouse.code} className="warehouse-health-card">
              <div className="stack-title-row">
                <strong>{warehouse.name}</strong>
                <span className={`status-tag ${warehouse.pressureScore >= 80 ? 'status-success' : warehouse.pressureScore >= 60 ? 'status-partial' : 'status-failure'}`}>Pressure {100 - warehouse.pressureScore}</span>
              </div>
              <p>{warehouse.code}</p>
              <div className="warehouse-stat-grid">
                <div><span>Low stock</span><strong>{warehouse.lowStockCount}</strong></div>
                <div><span>High risk</span><strong>{warehouse.highRiskCount}</strong></div>
                <div><span>Orders</span><strong>{warehouse.orderLoad}</strong></div>
                <div><span>Backlog</span><strong>{warehouse.backlogCount}</strong></div>
              </div>
              <p className="muted-text">{warehouse.delayedCount ? `${warehouse.delayedCount} delayed shipment lane${warehouse.delayedCount === 1 ? '' : 's'}.` : 'No delayed fulfillment lanes right now.'}</p>
            </article>
          )) : <EmptyState>Warehouse health cards will appear once inventory and fulfillment data are active.</EmptyState>}
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>{selectedWarehouse ? `${selectedWarehouse.name} detail` : 'Location detail'}</strong>
              <span className={`status-tag ${selectedWarehouse && selectedWarehouse.pressureScore >= 80 ? 'status-success' : selectedWarehouse && selectedWarehouse.pressureScore >= 60 ? 'status-partial' : 'status-failure'}`}>
                {selectedWarehouse ? `Pressure ${100 - selectedWarehouse.pressureScore}` : 'Waiting'}
              </span>
            </div>
            {selectedWarehouse ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedWarehouse.code}</strong>
                  <p>{selectedWarehouse.orderLoad} recent orders | {selectedWarehouse.backlogCount} active fulfillment lanes</p>
                  <p className="muted-text">{selectedWarehouse.lowStockCount} low-stock items | {selectedWarehouse.highRiskCount} high-risk items | {selectedWarehouse.delayedCount} delayed lanes</p>
                </div>
                <div className="signal-list-item">
                  <strong>Hottest lane inventory focus</strong>
                  <p>{selectedWarehouseInventory.filter((item) => item.lowStock).length} low-stock SKUs in this site.</p>
                  <p className="muted-text">{selectedWarehouseInventory.slice(0, 3).map((item) => item.productName).join(', ') || 'Waiting for live inventory mix.'}</p>
                </div>
              </div>
            ) : <EmptyState>Select a location lane to inspect local stock, order flow, and site-level pressure.</EmptyState>}
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Site action queue</strong>
              <span className="scenario-type-tag">{(selectedWarehouseAlerts.length + selectedWarehouseRecommendations.length) || 0}</span>
            </div>
            <div className="signal-list">
              {selectedWarehouseAlerts.map((alert) => (
                <div key={alert.id} className="signal-list-item">
                  <div className="stack-title-row">
                    <strong>{alert.title}</strong>
                    <span className={`severity-tag severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span>
                  </div>
                  <p>{alert.impactSummary}</p>
                  <p className="muted-text">{alert.recommendedAction}</p>
                </div>
              ))}
              {selectedWarehouseRecommendations.map((recommendation) => (
                <div key={recommendation.id} className="signal-list-item">
                  <div className="stack-title-row">
                    <strong>{recommendation.title}</strong>
                    <span className={`priority-tag priority-${recommendation.priority.toLowerCase()}`}>{recommendation.priority}</span>
                  </div>
                  <p>{recommendation.description}</p>
                </div>
              ))}
              {!selectedWarehouseAlerts.length && !selectedWarehouseRecommendations.length ? <EmptyState>No location-specific actions are active for the hottest site right now.</EmptyState> : null}
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
