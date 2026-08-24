import { useState } from 'react'
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
    signedInRoles,
    signedInWarehouseScopes,
    hasWarehouseScope,
    fetchJson,
    fetchSnapshot,
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
  const outOfStockInventory = snapshot.inventory.filter((item) => Number(item.quantityAvailable) <= 0)
  const inventoryPosture = outOfStockInventory.length
    ? 'Unavailable'
    : highRiskInventory.length
      ? 'Attention required'
      : lowStockInventory.length
        ? 'Watch'
        : snapshot.inventory.length
          ? 'Normal'
          : 'Insufficient coverage'
  const inventoryPostureTone = outOfStockInventory.length || highRiskInventory.length
    ? 'status-failure'
    : lowStockInventory.length || !snapshot.inventory.length
      ? 'status-partial'
      : 'status-success'
  const inventoryPostureCopy = outOfStockInventory.length
    ? `${outOfStockInventory.length} inventory lane${outOfStockInventory.length === 1 ? '' : 's'} show no available quantity. Confirm the affected product and location before promising downstream work.`
    : highRiskInventory.length
      ? `${highRiskInventory.length} high-risk lane${highRiskInventory.length === 1 ? '' : 's'} need inventory review before order pressure deepens.`
      : lowStockInventory.length
        ? 'Low-stock lanes are visible. Review thresholds, stockout windows, and affected warehouses.'
        : snapshot.inventory.length
          ? 'Inventory lanes look stable inside the current workspace view.'
          : 'Inventory coverage is not available yet. Add stock data before treating the workspace as operationally covered.'
  const canMaintainSelectedInventory = Boolean(
    selectedInventoryItem
      && signedInRoles.includes('TENANT_ADMIN')
      && hasWarehouseScope(signedInWarehouseScopes, selectedInventoryItem.warehouseCode),
  )
  const [adjustmentDelta, setAdjustmentDelta] = useState('')
  const [adjustmentReason, setAdjustmentReason] = useState('')
  const [inventoryActionState, setInventoryActionState] = useState({ loading: false, error: '', success: '' })

  const submitInventoryAdjustment = async (event) => {
    event.preventDefault()
    if (!selectedInventoryItem || !canMaintainSelectedInventory || !fetchJson) {
      return
    }

    const quantityDelta = Number(adjustmentDelta)
    if (!Number.isInteger(quantityDelta) || quantityDelta === 0 || !adjustmentReason.trim()) {
      setInventoryActionState({ loading: false, error: 'Enter a non-zero whole-unit adjustment and a reason before submitting.', success: '' })
      return
    }

    setInventoryActionState({ loading: true, error: '', success: '' })
    try {
      await fetchJson('/api/inventory/adjust', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          productSku: selectedInventoryItem.productSku,
          warehouseCode: selectedInventoryItem.warehouseCode,
          quantityDelta,
          reason: adjustmentReason.trim(),
        }),
      })
      setAdjustmentDelta('')
      setAdjustmentReason('')
      setInventoryActionState({ loading: false, error: '', success: 'Inventory adjustment accepted. Refreshing the selected warehouse lane for readback.' })
      await fetchSnapshot?.()
    } catch (error) {
      setInventoryActionState({ loading: false, error: error.message || 'Inventory adjustment was not accepted.', success: '' })
    }
  }

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

        <div className="workflow-decision-hero daily-ops-hero">
          <div className="workflow-decision-copy">
            <p className="panel-kicker">Inventory risk posture</p>
            <div className="runtime-decision-title">
              <span className={`runtime-decision-badge ${inventoryPostureTone}`}>{inventoryPosture}</span>
              <h2>{mostPressedItem ? `${mostPressedItem.productName} needs review` : 'Inventory coverage is waiting.'}</h2>
            </div>
            <p>{inventoryPostureCopy}</p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Low stock {lowStockInventory.length}</span>
              <span className="workspace-meta-pill">High risk {highRiskInventory.length}</span>
              <span className="workspace-meta-pill">Sites {warehouseCoverage || warehouseOptions.length}</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Inspect next</span>
              <strong>{mostPressedItem?.productSku || 'No risk selected'}</strong>
              <p>{mostPressedItem ? 'Review the selected product, warehouse, available quantity, threshold, and stockout window before taking action in the source system.' : 'Inventory rows will become actionable once stock data is available.'}</p>
            </div>
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

        <div className="inventory-spotlight-grid daily-priority-grid" id="inventory-spotlight">
          <article className="stack-card section-card workflow-selected-panel">
            <div className="stack-title-row">
              <strong>Highest-risk inventory</strong>
              <span className={`status-tag ${inventoryPostureTone}`}>{lowStockInventory.length || highRiskInventory.length}</span>
            </div>
            <div className="signal-list">
              {(highRiskInventory.length ? highRiskInventory : lowStockInventory).slice(0, 5).length ? (highRiskInventory.length ? highRiskInventory : lowStockInventory).slice(0, 5).map((item) => (
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
              )) : <EmptyState>No low-stock or high-risk items right now. The live inventory posture is currently stable.</EmptyState>}
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

        <div className="experience-grid daily-ops-workbench">
          <article className="stack-card section-card workflow-selected-panel" id="inventory-focus">
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
                  This lane shows the product, warehouse, available quantity, and forecasted risk window. SynapseCore observations do not replace the source system as the authoritative stock record during the pilot.
                </p>
                <div className="workflow-action-band">
                  <div>
                    <strong>{canMaintainSelectedInventory ? 'Controlled tenant-admin maintenance is available' : 'Review posture; maintenance is not available here'}</strong>
                    <p>{canMaintainSelectedInventory
                      ? 'Use a reasoned adjustment only for an approved warehouse-scoped correction. Verify the readback and reconcile with the source system afterward.'
                      : 'This role can inspect inventory evidence, but only a warehouse-scoped TENANT_ADMIN may use the supported maintenance action.'}</p>
                  </div>
                  <button className="ghost-button" onClick={() => setSelectedInventoryId(selectedInventoryItem.id)} type="button">Keep Selected</button>
                </div>
                {canMaintainSelectedInventory ? (
                  <form className="inventory-maintenance-panel" onSubmit={submitInventoryAdjustment}>
                    <div className="stack-title-row">
                      <strong>Controlled inventory maintenance</strong>
                      <span className="scenario-type-tag">TENANT_ADMIN</span>
                    </div>
                    <p className="muted-text">Warehouse {selectedInventoryItem.warehouseCode} | Before {selectedInventoryItem.quantityAvailable}. This changes the SynapseCore inventory record; source reconciliation remains a separate responsibility.</p>
                    <div className="session-control-row">
                      <label className="field">
                        <span>Quantity delta</span>
                        <input type="number" step="1" value={adjustmentDelta} onChange={(event) => setAdjustmentDelta(event.target.value)} placeholder="e.g. -2" />
                      </label>
                      <label className="field">
                        <span>Reason</span>
                        <input type="text" maxLength="320" value={adjustmentReason} onChange={(event) => setAdjustmentReason(event.target.value)} placeholder="Cycle count correction" />
                      </label>
                    </div>
                    {inventoryActionState.error ? <p className="error-text">{inventoryActionState.error}</p> : null}
                    {inventoryActionState.success ? <p className="success-text">{inventoryActionState.success}</p> : null}
                    <button className="secondary-button" type="submit" disabled={inventoryActionState.loading}>
                      {inventoryActionState.loading ? 'Applying...' : 'Apply controlled adjustment'}
                    </button>
                  </form>
                ) : null}
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
