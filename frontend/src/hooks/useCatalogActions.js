export default function useCatalogActions({
  canManageTenantAccess,
  catalogForm,
  createDefaultCatalogForm,
  fetchCatalogProducts,
  fetchJson,
  setCatalogForm,
  setCatalogState,
  setSelectedCatalogProductId,
}) {
  function resetCatalogForm() {
    setCatalogForm(createDefaultCatalogForm())
    setSelectedCatalogProductId(null)
    setCatalogState((current) => ({ ...current, error: '', success: '' }))
  }

  async function saveCatalogProduct() {
    if (!canManageTenantAccess) {
      setCatalogState((current) => ({ ...current, error: 'Tenant admin access is required to manage the product catalog.', success: '' }))
      return
    }
    const payload = {
      sku: catalogForm.sku.trim(),
      name: catalogForm.name.trim(),
      category: catalogForm.category.trim(),
    }
    setCatalogState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const product = await fetchJson(catalogForm.id ? `/api/products/${catalogForm.id}` : '/api/products', {
        method: catalogForm.id ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      await fetchCatalogProducts({ quiet: true })
      setSelectedCatalogProductId(product.id)
      setCatalogForm({ id: product.id, sku: product.sku, name: product.name, category: product.category })
      setCatalogState((current) => ({ ...current, loading: false, error: '', success: `${product.sku} saved to the tenant catalog.` }))
    } catch (error) {
      setCatalogState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  async function importCatalogProducts(file) {
    if (!canManageTenantAccess) {
      setCatalogState((current) => ({ ...current, error: 'Tenant admin access is required to import products.', success: '' }))
      return
    }
    const formData = new FormData()
    formData.append('file', file)
    setCatalogState((current) => ({ ...current, loading: true, error: '', success: '', importResult: null }))
    try {
      const importResult = await fetchJson('/api/products/import', { method: 'POST', body: formData })
      await fetchCatalogProducts({ quiet: true })
      setCatalogState((current) => ({
        ...current,
        loading: false,
        error: '',
        success: `Imported ${importResult.totalRows} rows: ${importResult.created} created, ${importResult.updated} updated, ${importResult.failed} failed.`,
        importResult,
      }))
    } catch (error) {
      setCatalogState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  return {
    resetCatalogForm,
    saveCatalogProduct,
    importCatalogProducts,
  }
}
