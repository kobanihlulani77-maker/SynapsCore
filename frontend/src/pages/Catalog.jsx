import DataGrid from '../components/DataGrid'
import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { MetricCard } from '../components/Card'

export default function CatalogPage({ context }) {
  const {
    isAuthenticated,
    isCatalogPage,
    catalogState,
    catalogForm,
    setCatalogForm,
    selectedCatalogProductId,
    setSelectedCatalogProductId,
    saveCatalogProduct,
    importCatalogProducts,
    resetCatalogForm,
    canManageTenantAccess,
  } = context

  if (!isAuthenticated || !isCatalogPage) return null

  const products = catalogState.products || []
  const selectedProduct = products.find((product) => product.id === selectedCatalogProductId) || products[0]
  const categoryCount = new Set(products.map((product) => product.category).filter(Boolean)).size
  const canSubmit = canManageTenantAccess
    && catalogForm.sku.trim()
    && catalogForm.name.trim()
    && catalogForm.category.trim()
    && !catalogState.loading
  const importRows = catalogState.importResult?.rows || []
  const failedImportRows = importRows.filter((row) => row.status && row.status !== 'SUCCESS')
  const catalogMode = catalogState.importResult
    ? catalogState.importResult.failed
      ? 'Correct import rows'
      : 'Import reviewed'
    : catalogForm.id
      ? 'Edit product'
      : products.length
        ? 'Review catalog'
        : 'Create first product'
  const catalogModeTone = catalogState.importResult?.failed
    ? 'status-failure'
    : products.length || catalogState.importResult
      ? 'status-success'
      : 'status-partial'
  const catalogNextStep = !canManageTenantAccess
    ? 'Product creation and import require a tenant admin account.'
    : catalogState.importResult?.failed
      ? `${catalogState.importResult.failed} import row${catalogState.importResult.failed === 1 ? '' : 's'} failed validation or persistence. Review the row messages before continuing.`
      : catalogForm.id
        ? 'Review identity fields, then update the selected workspace product.'
        : 'Create a single product or import a CSV, then review the catalog table below.'

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Product catalog</p>
            <h2>Tenant-owned product onboarding and import</h2>
          </div>
          <span className="panel-badge inventory-badge">{products.length}</span>
        </div>

        <div className="workflow-decision-hero catalog-admin-hero">
          <div className="workflow-decision-copy">
            <p className="panel-kicker">Catalog workflow</p>
            <div className="runtime-decision-title">
              <span className={`runtime-decision-badge ${catalogModeTone}`}>{catalogMode}</span>
              <h2>{catalogNextStep}</h2>
            </div>
            <p>Choose one administrative path: create one product, import multiple products, correct import results, or review the existing workspace catalog.</p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Products {products.length}</span>
              <span className="workspace-meta-pill">Categories {categoryCount}</span>
              <span className="workspace-meta-pill">Import failures {catalogState.importResult?.failed || 0}</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Safest next action</span>
              <strong>{canManageTenantAccess ? catalogMode : 'Read-only review'}</strong>
              <p>{canManageTenantAccess ? 'Keep product identity clean before inventory, orders, and integrations depend on these SKUs.' : 'Review product data and ask a workspace admin to perform create or import actions.'}</p>
            </div>
            <button className="secondary-button" onClick={resetCatalogForm} disabled={catalogState.loading || !canManageTenantAccess} type="button">
              {catalogForm.id ? 'Create new product' : 'Reset form'}
            </button>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Products" value={products.length} accent="blue" note="Products registered inside this company workspace." />
          <MetricCard label="Categories" value={categoryCount} accent="teal" note="Product groups currently represented in the catalog." />
          <MetricCard label="Import failures" value={catalogState.importResult?.failed || 0} accent="rose" note="Rows from the latest import that still need operator review." />
          <MetricCard label="Catalog access" value={canManageTenantAccess ? 'Admin' : 'Read'} accent="amber" note="Product creation and import stay scoped to workspace admins." />
        </div>

        {catalogState.error ? <p className="error-text">{catalogState.error}</p> : null}
        {catalogState.success ? <p className="success-text">{catalogState.success}</p> : null}

        <div className="catalog-workflow-grid">
          <article className="stack-card section-card workflow-selected-panel">
            <div className="stack-title-row">
              <strong>Zone 1: create one product</strong>
              <span className={`status-tag ${canManageTenantAccess ? 'status-success' : 'status-partial'}`}>{catalogForm.id ? 'Editing' : 'Ready'}</span>
            </div>
            {!canManageTenantAccess ? (
              <p className="muted-text">
                Product creation and import require a tenant admin account. Workspace operators can still review the product foundation and import outcomes.
              </p>
            ) : null}
            <div className="catalog-field-group">
              <div>
                <strong>Identity</strong>
                <p className="muted-text">SKU, name, and category are required for supported catalog onboarding.</p>
              </div>
            <div className="session-control-row">
              <label className="field planner-name-field">
                <span>SKU</span>
                <input
                  value={catalogForm.sku}
                  onChange={(event) => setCatalogForm((current) => ({ ...current, sku: event.target.value }))}
                  placeholder="SKU-ACME-100"
                  disabled={catalogState.loading || !canManageTenantAccess}
                />
              </label>
              <label className="field planner-name-field">
                <span>Name</span>
                <input
                  value={catalogForm.name}
                  onChange={(event) => setCatalogForm((current) => ({ ...current, name: event.target.value }))}
                  placeholder="Product name"
                  disabled={catalogState.loading || !canManageTenantAccess}
                />
              </label>
              <label className="field planner-name-field">
                <span>Category</span>
                <input
                  value={catalogForm.category}
                  onChange={(event) => setCatalogForm((current) => ({ ...current, category: event.target.value }))}
                  placeholder="Operational category"
                  disabled={catalogState.loading || !canManageTenantAccess}
                />
              </label>
            </div>
            </div>
            <div className="history-action-row">
              <button className="secondary-button" onClick={saveCatalogProduct} disabled={!canSubmit} type="button">
                {catalogState.loading ? 'Working...' : catalogForm.id ? 'Update Product' : 'Create Product'}
              </button>
              <button className="ghost-button" onClick={resetCatalogForm} disabled={catalogState.loading || !canManageTenantAccess} type="button">
                Clear
              </button>
            </div>
            <p className="muted-text">Operators and integrations reference workspace SKUs directly. Other companies can use the same codes without crossing tenant boundaries.</p>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Zone 2: import and validate</strong>
              <span className="scenario-type-tag">sku,name,category</span>
            </div>
            <div className="catalog-import-steps" aria-label="Catalog import steps">
              <span className="workspace-meta-pill">1 Choose CSV</span>
              <span className="workspace-meta-pill">2 Submit import</span>
              <span className="workspace-meta-pill">3 Review row results</span>
              <span className="workspace-meta-pill">4 Correct failures</span>
            </div>
            <label className="field planner-name-field">
              <span>Product CSV</span>
              <input
                type="file"
                accept=".csv,text/csv"
                disabled={catalogState.loading || !canManageTenantAccess}
                onChange={(event) => {
                  const file = event.target.files?.[0]
                  if (file) importCatalogProducts(file)
                  event.target.value = ''
                }}
              />
            </label>
            <p className="muted-text">Selecting a CSV immediately starts the supported import flow. Results below show created, updated, and failed rows honestly.</p>
            {catalogState.importResult ? (
              <div className="utility-metric-grid">
                <div><span>Rows</span><strong>{catalogState.importResult.totalRows}</strong></div>
                <div><span>Created</span><strong>{catalogState.importResult.created}</strong></div>
                <div><span>Updated</span><strong>{catalogState.importResult.updated}</strong></div>
                <div><span>Failed</span><strong>{catalogState.importResult.failed}</strong></div>
              </div>
            ) : (
              <EmptyState>Upload a product CSV to review row-level outcomes here.</EmptyState>
            )}
            {failedImportRows.length ? (
              <div className="signal-list">
                {failedImportRows.slice(0, 3).map((row) => (
                  <div key={`failed-${row.rowNumber}-${row.sku}`} className="signal-list-item">
                    <strong>Row {row.rowNumber}: {row.sku || 'Missing SKU'}</strong>
                    <p>{row.message || 'Import row needs correction.'}</p>
                  </div>
                ))}
              </div>
            ) : null}
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Zone 3: review catalog</strong>
              <span className="scenario-type-tag">{products.length ? 'Live' : 'Setup'}</span>
            </div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>What this page should do</strong>
                <p>Help workspace admins decide whether they are creating, importing, correcting, or reviewing product data.</p>
              </div>
              <div className="signal-list-item">
                <strong>What comes next</strong>
                <p>After products exist, inventory, order intake, and integration mapping can reference these workspace SKUs.</p>
              </div>
            </div>
          </article>
        </div>

        <div className="experience-grid daily-ops-workbench catalog-review-workbench">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Product table</strong>
              <span className="scenario-type-tag">{products.length}</span>
            </div>
            {products.length ? (
              <DataGrid
                columns={[
                  { key: 'sku', label: 'SKU', sortable: true },
                  { key: 'name', label: 'Name', sortable: true },
                  { key: 'category', label: 'Category', sortable: true },
                  {
                    key: 'actions',
                    label: 'Action',
                    render: (product) => (
                      <div className="history-action-row">
                        <button
                          className="ghost-button compact-action-button"
                          onClick={() => setSelectedCatalogProductId(product.id)}
                          type="button"
                        >
                          Inspect
                        </button>
                        <button
                          className="ghost-button compact-action-button"
                          onClick={() => {
                            setSelectedCatalogProductId(product.id)
                            setCatalogForm({ id: product.id, sku: product.sku, name: product.name, category: product.category })
                          }}
                          type="button"
                          disabled={!canManageTenantAccess}
                        >
                          Edit
                        </button>
                      </div>
                    ),
                  },
                ]}
                rows={products}
                emptyMessage="No products are available for this workspace catalog yet."
              />
            ) : (
              <EmptyState>
                No products exist yet. Create the first product or import a CSV to initialize the company catalog deliberately.
              </EmptyState>
            )}
          </article>

          <article className="stack-card section-card workflow-selected-panel">
            <div className="stack-title-row">
              <strong>Selected catalog item</strong>
              <span className="status-tag status-partial">{selectedProduct?.sku || 'Waiting'}</span>
            </div>
            {selectedProduct ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedProduct.name}</strong>
                  <p>{selectedProduct.sku} | {selectedProduct.category}</p>
                  <p className="muted-text">Owned by workspace {selectedProduct.tenantCode}. Inventory updates, order intake, and import mapping should reference this product identity.</p>
                </div>
              </div>
            ) : (
              <EmptyState>
                Select a product to review the workspace SKU identity that downstream inventory and orders depend on.
              </EmptyState>
            )}

            {importRows.length ? (
              <>
                <div className="stack-title-row">
                  <strong>Latest import results</strong>
                  <span className="scenario-type-tag">{importRows.length}</span>
                </div>
                <DataGrid
                  columns={[
                    { key: 'rowNumber', label: 'Row', sortable: true },
                    { key: 'sku', label: 'SKU', sortable: true },
                    { key: 'status', label: 'Status', sortable: true },
                    { key: 'message', label: 'Message' },
                  ]}
                  rows={importRows.map((row) => ({ ...row, id: `import-${row.rowNumber}` }))}
                  emptyMessage="No import rows available."
                />
              </>
            ) : null}
          </article>
        </div>
      </Panel>
    </section>
  )
}
