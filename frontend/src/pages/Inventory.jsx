import DataGrid from '../components/DataGrid'
import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { SummaryCard } from '../components/Card'

export default function InventoryPage({ context }) {
  const {
    isAuthenticated,
    isInventoryPage,
    snapshot,
    selectedInventoryId,
    setSelectedInventoryId,
    lowStockInventory,
    highRiskInventory,
    fastMovingInventory,
    warehouseOptions,
    formatCodeLabel,
    formatRelativeHours,
  } = context

  if (!isAuthenticated || !isInventoryPage) {
    return null
  }

  const selectedInventoryItem = snapshot.inventory.find((item) => item.id === selectedInventoryId) || highRiskInventory[0] || lowStockInventory[0] || snapshot.inventory[0]
  const gridRows = snapshot.inventory.slice(0, 6)

  return (
    <section className="content-grid inventory-intelligence-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Inventory intelligence</p><h2>Stock posture, velocity, and risk</h2></div>
          <span className="panel-badge inventory-badge">{snapshot.inventory.length}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Low stock" value={lowStockInventory.length} accent="orange" />
          <SummaryCard label="High risk" value={highRiskInventory.length} accent="rose" />
          <SummaryCard label="Fast movers" value={fastMovingInventory.length} accent="teal" />
          <SummaryCard label="Warehouses" value={warehouseOptions.length} accent="blue" />
        </div>
        <div className="inventory-spotlight-grid" id="inventory-spotlight">
          <article className="stack-card">
            <div className="stack-title-row"><strong>Low-stock focus</strong><span className="status-tag status-failure">{lowStockInventory.length}</span></div>
            <div className="stack-list compact-stack-list">
              {lowStockInventory.slice(0, 5).map((item) => (
                <button
                  key={item.id}
                  className={`stack-card stack-card-compact selectable-card ${selectedInventoryItem?.id === item.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedInventoryId(item.id)}
                  type="button"
                >
                  <strong>{item.productName}</strong>
                  <p>{item.warehouseName}</p>
                  <p className="muted-text">{item.quantityAvailable} available | Threshold {item.reorderThreshold} | Stockout {formatRelativeHours(item.hoursToStockout)}</p>
                </button>
              ))}
              {!lowStockInventory.length ? <EmptyState>No low-stock items right now.</EmptyState> : null}
            </div>
          </article>
          <article className="stack-card">
            <div className="stack-title-row"><strong>Fast-moving items</strong><span className="status-tag status-partial">{fastMovingInventory.length}</span></div>
            <div className="stack-list compact-stack-list">
              {fastMovingInventory.map((item) => (
                <button
                  key={item.id}
                  className={`stack-card stack-card-compact selectable-card ${selectedInventoryItem?.id === item.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedInventoryId(item.id)}
                  type="button"
                >
                  <strong>{item.productName}</strong>
                  <p>{item.warehouseName}</p>
                  <p className="muted-text">{(item.unitsPerHour || 0).toFixed(1)} units/hr | Risk {formatCodeLabel(item.riskLevel)}</p>
                </button>
              ))}
              {!fastMovingInventory.length ? <EmptyState>Velocity metrics will appear as order demand accumulates.</EmptyState> : null}
            </div>
          </article>
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card" id="inventory-focus">
            <div className="stack-title-row">
              <strong>Selected inventory lane</strong>
              <span className={`status-tag ${selectedInventoryItem ? `risk-${selectedInventoryItem.riskLevel}` : 'status-partial'}`}>{selectedInventoryItem ? formatCodeLabel(selectedInventoryItem.riskLevel) : 'Waiting'}</span>
            </div>
            {selectedInventoryItem ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedInventoryItem.productName}</strong>
                  <p>{selectedInventoryItem.warehouseName} | {selectedInventoryItem.productSku}</p>
                  <p className="muted-text">{selectedInventoryItem.quantityAvailable} available | Threshold {selectedInventoryItem.reorderThreshold} | Velocity {(selectedInventoryItem.unitsPerHour || 0).toFixed(1)} units/hr</p>
                  <p className="muted-text">Stockout forecast {formatRelativeHours(selectedInventoryItem.hoursToStockout)}</p>
                </div>
              </div>
            ) : <EmptyState>The most pressured inventory lane appears here so teams can understand risk without scanning every row first.</EmptyState>}
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Inventory signal matrix</strong>
              <span className="scenario-type-tag">{gridRows.length}</span>
            </div>
            {gridRows.length ? (
              <DataGrid
                columns={[
                  { key: 'productName', label: 'Product' },
                  { key: 'warehouseCode', label: 'Warehouse' },
                  { key: 'quantityAvailable', label: 'Qty' },
                  { key: 'reorderThreshold', label: 'Threshold' },
                  { key: 'riskLevel', label: 'Risk', render: (row) => formatCodeLabel(row.riskLevel) },
                ]}
                rows={gridRows}
              />
            ) : <EmptyState>Inventory rows will appear here once the workspace has stock data flowing.</EmptyState>}
          </article>
        </div>
      </Panel>
    </section>
  )
}
