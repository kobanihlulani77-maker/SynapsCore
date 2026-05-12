import DataGrid from '../components/DataGrid'
import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { MetricCard } from '../components/Card'

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

  const selectedInventoryItem = snapshot.inventory.find((item) => item.id === selectedInventoryId)
    || highRiskInventory[0]
    || lowStockInventory[0]
    || snapshot.inventory[0]
  const gridRows = snapshot.inventory.slice(0, 6)
  const mostPressedItem = highRiskInventory[0] || lowStockInventory[0] || fastMovingInventory[0]
  const warehouseCoverage = new Set(snapshot.inventory.map((item) => item.warehouseCode).filter(Boolean)).size

  return (
    <section className="content-grid inventory-intelligence-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Inventory intelligence</p>
            <h2>Stock posture, velocity, and risk</h2>
          </div>
          <span className="panel-badge inventory-badge">{snapshot.inventory.length}</span>
        </div>

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Inventory health command view</strong>
            <p>
              Bring low-stock exposure, fast movers, and warehouse-specific pressure into one decision surface so operators
              can act before availability risk turns into missed fulfillment.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Availability-first</span>
              <span className="workspace-meta-pill">Velocity-aware</span>
              <span className="workspace-meta-pill">Warehouse-scoped</span>
            </div>
          </div>
          <div className="ops-command-actions">
            <button className="secondary-button" onClick={() => mostPressedItem && setSelectedInventoryId(mostPressedItem.id)} disabled={!mostPressedItem} type="button">
              Focus highest risk
            </button>
            <button className="ghost-button" onClick={() => fastMovingInventory[0] && setSelectedInventoryId(fastMovingInventory[0].id)} disabled={!fastMovingInventory[0]} type="button">
              Inspect fast mover
            </button>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Low stock" value={lowStockInventory.length} accent="orange" note="Items already approaching or below their operating threshold." />
          <MetricCard label="High risk" value={highRiskInventory.length} accent="rose" note="Inventory lanes most likely to create near-term operational pressure." />
          <MetricCard label="Fast movers" value={fastMovingInventory.length} accent="teal" note="Items consuming quickly enough to shape replenishment attention." />
          <MetricCard label="Warehouse coverage" value={warehouseCoverage || warehouseOptions.length} accent="blue" note="Warehouse lanes currently represented in the live stock picture." />
        </div>

        <div className="inventory-spotlight-grid" id="inventory-spotlight">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Low-stock focus</strong>
              <span className="status-tag status-failure">{lowStockInventory.length}</span>
            </div>
            <div className="signal-list">
              {lowStockInventory.slice(0, 5).length ? lowStockInventory.slice(0, 5).map((item) => (
                <button
                  key={item.id}
                  className={`signal-list-item selectable-card system-select-card ${selectedInventoryItem?.id === item.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedInventoryId(item.id)}
                  type="button"
                >
                  <div className="stack-title-row">
                    <strong>{item.productName}</strong>
                    <span className={`status-tag risk-${item.riskLevel}`}>{formatCodeLabel(item.riskLevel)}</span>
                  </div>
                  <p>{item.warehouseName}</p>
                  <p className="muted-text">{item.quantityAvailable} available | Threshold {item.reorderThreshold}</p>
                  <p className="muted-text">Stockout {formatRelativeHours(item.hoursToStockout)}</p>
                </button>
              )) : <EmptyState>No low-stock items right now. The live inventory posture is currently stable.</EmptyState>}
            </div>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Fast-moving items</strong>
              <span className="status-tag status-partial">{fastMovingInventory.length}</span>
            </div>
            <div className="signal-list">
              {fastMovingInventory.length ? fastMovingInventory.map((item) => (
                <button
                  key={item.id}
                  className={`signal-list-item selectable-card system-select-card ${selectedInventoryItem?.id === item.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedInventoryId(item.id)}
                  type="button"
                >
                  <div className="stack-title-row">
                    <strong>{item.productName}</strong>
                    <span className={`status-tag risk-${item.riskLevel}`}>{formatCodeLabel(item.riskLevel)}</span>
                  </div>
                  <p>{item.warehouseName}</p>
                  <p className="muted-text">{(item.unitsPerHour || 0).toFixed(1)} units/hr | {item.quantityAvailable} available</p>
                </button>
              )) : <EmptyState>Velocity metrics will appear here as outbound order demand begins shaping the workspace.</EmptyState>}
            </div>
          </article>
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card" id="inventory-focus">
            <div className="stack-title-row">
              <strong>Selected inventory lane</strong>
              <span className={`status-tag ${selectedInventoryItem ? `risk-${selectedInventoryItem.riskLevel}` : 'status-partial'}`}>
                {selectedInventoryItem ? formatCodeLabel(selectedInventoryItem.riskLevel) : 'Waiting'}
              </span>
            </div>
            {selectedInventoryItem ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedInventoryItem.productName}</strong>
                  <p>{selectedInventoryItem.warehouseName} | {selectedInventoryItem.productSku}</p>
                </div>
                <div className="utility-metric-grid">
                  <div><span>Available</span><strong>{selectedInventoryItem.quantityAvailable}</strong></div>
                  <div><span>Threshold</span><strong>{selectedInventoryItem.reorderThreshold}</strong></div>
                  <div><span>Velocity</span><strong>{(selectedInventoryItem.unitsPerHour || 0).toFixed(1)}/hr</strong></div>
                  <div><span>Stockout</span><strong>{formatRelativeHours(selectedInventoryItem.hoursToStockout)}</strong></div>
                </div>
                <p className="muted-text">
                  This lane shows the product, warehouse, available quantity, and forecasted risk window without requiring a separate inventory export.
                </p>
              </div>
            ) : (
              <EmptyState>
                The most pressured inventory lane appears here first so teams can understand risk and speed without scanning the full stock matrix.
              </EmptyState>
            )}
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
                  { key: 'quantityAvailable', label: 'Available' },
                  { key: 'reorderThreshold', label: 'Threshold' },
                  { key: 'riskLevel', label: 'Risk', render: (row) => formatCodeLabel(row.riskLevel) },
                ]}
                rows={gridRows}
              />
            ) : (
              <EmptyState>
                Inventory rows will appear here once the workspace has stock data flowing from warehouse updates or integrations.
              </EmptyState>
            )}
          </article>
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Replenishment guidance</strong>
              <span className="scenario-type-tag">{lowStockInventory.length ? 'Action needed' : 'Watching'}</span>
            </div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>What needs attention first</strong>
                <p>
                  {highRiskInventory.length
                    ? `${highRiskInventory.length} inventory lane${highRiskInventory.length === 1 ? '' : 's'} already qualify as high risk. Prioritize replenishment or transfer decisions there.`
                    : lowStockInventory.length
                      ? 'Low-stock lanes are visible. Review the forecasted stockout window before order pressure deepens.'
                      : 'No critical inventory gaps are visible right now. Use this page to watch velocity and warehouse concentration.'}
                </p>
              </div>
              <div className="signal-list-item">
                <strong>What this page should do</strong>
                <p>Help operators balance availability, movement speed, and threshold pressure without turning inventory review into a spreadsheet exercise.</p>
              </div>
            </div>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Coverage posture</strong>
              <span className="scenario-type-tag">{warehouseCoverage || warehouseOptions.length}</span>
            </div>
            <div className="utility-metric-grid">
              <div><span>Warehouses covered</span><strong>{warehouseCoverage || warehouseOptions.length}</strong></div>
              <div><span>Items in matrix</span><strong>{snapshot.inventory.length}</strong></div>
              <div><span>Fast movers</span><strong>{fastMovingInventory.length}</strong></div>
              <div><span>High-risk lanes</span><strong>{highRiskInventory.length}</strong></div>
            </div>
            <p className="muted-text">Inventory health needs to feel alive and explainable. This surface should make it obvious whether the workspace is stable, thinning, or headed toward shortages.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}
