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

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Workspace product foundation</strong>
            <p>
              The catalog is the company-owned source of product identity for inventory, order flow, and integration mapping.
              It should feel guided, understandable, and safe for first-time workspace setup.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Workspace-owned SKUs</span>
              <span className="workspace-meta-pill">Admin controlled</span>
              <span className="workspace-meta-pill">Import ready</span>
            </div>
          </div>
          <div className="ops-command-actions">
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

        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>{catalogForm.id ? 'Edit product' : 'Create product'}</strong>
              <span className="status-tag status-success">Workspace scoped</span>
            </div>
            {!canManageTenantAccess ? (
              <p className="muted-text">
                Product creation and import require a tenant admin account. Workspace operators can still review the product foundation and import outcomes.
              </p>
            ) : null}
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
            <div className="history-action-row">
              <button className="secondary-button" onClick={saveCatalogProduct} disabled={!canSubmit} type="button">
                {catalogState.loading ? 'Working...' : catalogForm.id ? 'Update Product' : 'Create Product'}
              </button>
              <button className="ghost-button" onClick={resetCatalogForm} disabled={catalogState.loading || !canManageTenantAccess} type="button">
                Clear
              </button>
            </div>
            <p className="muted-text">Operators and integrations will reference these workspace SKUs directly. Other companies can use the same codes without crossing data boundaries.</p>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>CSV import</strong>
              <span className="scenario-type-tag">sku,name,category</span>
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
            <p className="muted-text">Imports can create new products, update existing workspace SKUs, and report row-level failures without hiding the result behind a raw file dump.</p>
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
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Catalog posture</strong>
              <span className="scenario-type-tag">{products.length ? 'Live' : 'Setup'}</span>
            </div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>What this page should do</strong>
                <p>Give workspace admins a guided, calm way to build product identity before orders, inventory, and integrations rely on it.</p>
              </div>
              <div className="signal-list-item">
                <strong>What comes next</strong>
                <p>After the first products exist, the workspace can seed inventory, map integrations, invite operators, and begin live operational flow.</p>
              </div>
            </div>
          </article>
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Catalog products</strong>
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

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Selected catalog item</strong>
              <span className="status-tag status-partial">{selectedProduct?.sku || 'Waiting'}</span>
            </div>
            {selectedProduct ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedProduct.name}</strong>
                  <p>{selectedProduct.sku} | {selectedProduct.category}</p>
                  <p className="muted-text">Owned by workspace {selectedProduct.tenantCode}. Inventory updates, order intake, and import mapping should all reference this product identity.</p>
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
