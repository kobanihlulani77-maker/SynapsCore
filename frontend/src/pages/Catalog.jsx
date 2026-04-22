import DataGrid from '../components/DataGrid'
import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { SummaryCard } from '../components/Card'

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
  const selectedProduct = products.find((product) => product.id === selectedCatalogProductId)
  const categoryCount = new Set(products.map((product) => product.category).filter(Boolean)).size
  const canSubmit = canManageTenantAccess && catalogForm.sku.trim() && catalogForm.name.trim() && catalogForm.category.trim() && !catalogState.loading

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

        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Products" value={products.length} accent="blue" />
          <SummaryCard label="Categories" value={categoryCount} accent="teal" />
          <SummaryCard label="Import failures" value={catalogState.importResult?.failed || 0} accent="rose" />
          <SummaryCard label="Catalog access" value={canManageTenantAccess ? 'Admin' : 'Read'} accent="amber" />
        </div>

        {catalogState.error ? <p className="error-text">{catalogState.error}</p> : null}
        {catalogState.success ? <p className="success-text">{catalogState.success}</p> : null}
        {!canManageTenantAccess ? <p className="muted-text">Product creation and import require a tenant admin account. Catalog viewing remains available to workspace operators.</p> : null}

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>{catalogForm.id ? 'Edit product' : 'Create product'}</strong>
              <span className="status-tag status-success">Tenant scoped</span>
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
            <div className="history-action-row">
              <button className="secondary-button" onClick={saveCatalogProduct} disabled={!canSubmit} type="button">
                {catalogState.loading ? 'Working...' : catalogForm.id ? 'Update Product' : 'Create Product'}
              </button>
              <button className="ghost-button" onClick={resetCatalogForm} disabled={catalogState.loading || !canManageTenantAccess} type="button">Clear</button>
            </div>
            <p className="muted-text">SKUs are company-visible and tenant-scoped. Another tenant can use the same SKU without crossing data boundaries.</p>
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
            <p className="muted-text">Imports create new products, update existing tenant SKUs, and report duplicate or invalid rows without blocking the whole file.</p>
            {catalogState.importResult ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{catalogState.importResult.created} created | {catalogState.importResult.updated} updated | {catalogState.importResult.failed} failed</strong>
                  <p className="muted-text">{catalogState.importResult.totalRows} rows processed in the latest import.</p>
                </div>
              </div>
            ) : <EmptyState>Upload a product CSV to see row-level import status here.</EmptyState>}
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
                    ),
                  },
                ]}
                rows={products}
                emptyMessage="No products are available for this tenant catalog yet."
              />
            ) : <EmptyState>No products exist yet. Create one product or import the tenant catalog to start real operations.</EmptyState>}
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
                  <p className="muted-text">Owned by tenant {selectedProduct.tenantCode}. Inventory, orders, and imports must reference this tenant catalog SKU.</p>
                </div>
              </div>
            ) : <EmptyState>Select a product to inspect the tenant ownership and operational reference used by inventory and orders.</EmptyState>}

            {catalogState.importResult?.rows?.length ? (
              <>
                <div className="stack-title-row">
                  <strong>Latest import results</strong>
                  <span className="scenario-type-tag">{catalogState.importResult.rows.length}</span>
                </div>
                <DataGrid
                  columns={[
                    { key: 'rowNumber', label: 'Row', sortable: true },
                    { key: 'sku', label: 'SKU', sortable: true },
                    { key: 'status', label: 'Status', sortable: true },
                    { key: 'message', label: 'Message' },
                  ]}
                  rows={catalogState.importResult.rows.map((row) => ({ ...row, id: `import-${row.rowNumber}` }))}
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
