export default function ScenarioEditor({
  title,
  form,
  setter,
  context,
  warehouseOptions,
  addScenarioLine,
  updateScenarioField,
  updateScenarioLine,
  removeScenarioLine,
}) {
  return (
    <article className="scenario-editor-card">
      <div className="planner-line-header">
        <div><p className="panel-kicker">Scenario editor</p><h2>{title}</h2></div>
        <button className="ghost-button" onClick={() => addScenarioLine(setter, form.warehouseCode)} disabled={!context.productOptions.length} type="button">Add Line</button>
      </div>
      <div className="planner-controls">
        <label className="field">
          <span>Warehouse</span>
          <select value={form.warehouseCode} onChange={(event) => updateScenarioField(setter, 'warehouseCode', event.target.value)}>
            {warehouseOptions.map((warehouse) => <option key={warehouse.code} value={warehouse.code}>{warehouse.name}</option>)}
          </select>
        </label>
      </div>
      <div className="planner-lines">
        {context.lines.map((item, index) => (
          <div key={item.id} className="planner-line-card">
            <div className="planner-line-header">
              <strong>Line {index + 1}</strong>
              {form.items.length > 1 ? <button className="planner-remove" onClick={() => removeScenarioLine(setter, item.id)} type="button">Remove</button> : null}
            </div>
            <div className="planner-line-grid">
              <label className="field">
                <span>Product</span>
                <select value={item.productSku} onChange={(event) => updateScenarioLine(setter, item.id, 'productSku', event.target.value)}>
                  {context.productOptions.map((product) => <option key={product.sku} value={product.sku}>{product.name} ({product.sku})</option>)}
                </select>
              </label>
              <label className="field">
                <span>Quantity</span>
                <input type="number" min="1" value={item.quantity} onChange={(event) => updateScenarioLine(setter, item.id, 'quantity', event.target.value)} />
              </label>
              <label className="field">
                <span>Unit Price</span>
                <input type="number" min="0.01" step="0.01" value={item.unitPrice} onChange={(event) => updateScenarioLine(setter, item.id, 'unitPrice', event.target.value)} />
              </label>
            </div>
            {item.selectedProduct ? <p className="muted-text planner-note">Current buffer: {item.selectedProduct.quantityAvailable} available against a threshold of {item.selectedProduct.reorderThreshold}.</p> : null}
          </div>
        ))}
      </div>
    </article>
  )
}
