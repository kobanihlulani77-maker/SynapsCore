# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> auth flow and the full authenticated page system render cleanly in a browser
- Location: tests\prod-proof.spec.mjs:227:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('heading', { name: 'Open your company command center' })
Expected: visible
Timeout: 20000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 20000ms
  - waiting for getByRole('heading', { name: 'Open your company command center' })

```

# Page snapshot

```yaml
- generic [ref=e2]: Not Found
```

# Test source

```ts
  129 |   const api = await createApiContext(users.integrationLead)
  130 |   const suffix = randomUUID().slice(0, 8).toUpperCase()
  131 |   const sourceSystem = `ui_replay_${suffix}`.toLowerCase()
  132 |   const externalOrderId = `UI-RPL-${suffix}`
  133 | 
  134 |   try {
  135 |     await readJson(await inventoryAdmin.post('/api/inventory/update', {
  136 |       data: {
  137 |         productSku: 'SKU-PLS-330',
  138 |         warehouseCode: 'WH-NORTH',
  139 |         quantityAvailable: 50,
  140 |         reorderThreshold: 12,
  141 |       },
  142 |     }))
  143 | 
  144 |     await readJson(await api.post('/api/integrations/orders/connectors', {
  145 |       data: {
  146 |         sourceSystem,
  147 |         type: 'CSV_ORDER_IMPORT',
  148 |         displayName: `UI Replay ${suffix}`,
  149 |         enabled: false,
  150 |         syncMode: 'BATCH_FILE_DROP',
  151 |         validationPolicy: 'RELAXED',
  152 |         transformationPolicy: 'NORMALIZE_CODES',
  153 |         allowDefaultWarehouseFallback: false,
  154 |         notes: 'Disposable replay verification connector.',
  155 |       },
  156 |     }))
  157 | 
  158 |     const csvImportResponse = await api.post('/api/integrations/orders/csv-import', {
  159 |       multipart: {
  160 |         file: {
  161 |           name: 'orders.csv',
  162 |           mimeType: 'text/csv',
  163 |           buffer: Buffer.from(
  164 |             `externalOrderId,warehouseCode,productSku,quantity,unitPrice\n${externalOrderId},WH-NORTH,SKU-PLS-330,2,88.00\n`,
  165 |             'utf8',
  166 |           ),
  167 |         },
  168 |         sourceSystem,
  169 |       },
  170 |     })
  171 |     const csvImportPayload = await readJson(csvImportResponse)
  172 |     expect(csvImportPayload.ordersFailed).toBe(1)
  173 | 
  174 |     await readJson(await api.post('/api/integrations/orders/connectors', {
  175 |       data: {
  176 |         sourceSystem,
  177 |         type: 'CSV_ORDER_IMPORT',
  178 |         displayName: `UI Replay ${suffix}`,
  179 |         enabled: true,
  180 |         syncMode: 'BATCH_FILE_DROP',
  181 |         validationPolicy: 'RELAXED',
  182 |         transformationPolicy: 'NORMALIZE_CODES',
  183 |         allowDefaultWarehouseFallback: false,
  184 |         notes: 'Enabled for replay verification.',
  185 |       },
  186 |     }))
  187 | 
  188 |     return { api, sourceSystem, externalOrderId }
  189 |   } catch (error) {
  190 |     await api.dispose()
  191 |     throw error
  192 |   } finally {
  193 |     await inventoryAdmin.dispose()
  194 |   }
  195 | }
  196 | 
  197 | async function createScenarioFixture() {
  198 |   const api = await createApiContext(users.operationsLead)
  199 |   const suffix = randomUUID().slice(0, 8).toUpperCase()
  200 |   const title = `UI Scenario ${suffix}`
  201 | 
  202 |   try {
  203 |     await readJson(await api.post('/api/scenarios/save', {
  204 |       data: {
  205 |         title,
  206 |         requestedBy: 'Operations Lead',
  207 |         request: {
  208 |           warehouseCode: 'WH-NORTH',
  209 |           items: [
  210 |             {
  211 |               productSku: 'SKU-PLS-330',
  212 |               quantity: 1,
  213 |               unitPrice: 95,
  214 |             },
  215 |           ],
  216 |         },
  217 |       },
  218 |     }))
  219 | 
  220 |     return { api, title }
  221 |   } catch (error) {
  222 |     await api.dispose()
  223 |     throw error
  224 |   }
  225 | }
  226 | 
  227 | test('auth flow and the full authenticated page system render cleanly in a browser', async ({ page }) => {
  228 |   await page.goto('/dashboard')
> 229 |   await expect(page.getByRole('heading', { name: 'Open your company command center' })).toBeVisible()
      |                                                                                         ^ Error: expect(locator).toBeVisible() failed
  230 | 
  231 |   await page.getByLabel('Tenant workspace').fill(users.operationsLead.tenantCode)
  232 |   await page.getByLabel('Username').fill(users.operationsLead.username)
  233 |   await page.getByLabel('Password').fill('wrong-code')
  234 |   await page.locator('.public-signin-card').getByRole('button', { name: 'Enter Platform' }).click()
  235 |   await expect(page.getByText('Invalid operator credentials.')).toBeVisible()
  236 | 
  237 |   await page.getByLabel('Password').fill(users.operationsLead.password)
  238 |   await page.locator('.public-signin-card').getByRole('button', { name: 'Enter Platform' }).click()
  239 |   await expect(page).toHaveURL(/\/dashboard$/)
  240 | 
  241 |   for (const [route, title] of appPages) {
  242 |     await navigateWithinApp(page, route)
  243 |     await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
  244 |     await expect(page.locator('.workspace-topbar')).toBeVisible()
  245 |     await expectNoFatalUiErrors(page)
  246 |   }
  247 | 
  248 |   await signOutViaUi(page)
  249 | })
  250 | 
  251 | test('@realtime dashboard summary updates live without a browser refresh', async ({ page }) => {
  252 |   await loginViaUi(page, users.operationsLead)
  253 |   await expect(page.getByText('Live system')).toBeVisible()
  254 | 
  255 |   const api = await createApiContext(users.operationsLead)
  256 |   let candidate = null
  257 | 
  258 |   try {
  259 |     const inventory = await readJson(await api.get('/api/inventory'))
  260 |     candidate = inventory.find((item) => item.lowStock && Number.isFinite(item.quantityAvailable) && Number.isFinite(item.reorderThreshold))
  261 |     expect(candidate).toBeTruthy()
  262 | 
  263 |     const beforeLowStock = await waitForNumericSummaryCard(page, 'Low Stock Items')
  264 |     const nextQuantity = candidate.reorderThreshold + 5
  265 | 
  266 |     await readJson(await api.post('/api/inventory/update', {
  267 |       data: {
  268 |         productSku: candidate.productSku,
  269 |         warehouseCode: candidate.warehouseCode,
  270 |         quantityAvailable: nextQuantity,
  271 |         reorderThreshold: candidate.reorderThreshold,
  272 |       },
  273 |     }))
  274 | 
  275 |     await expect.poll(async () => summaryCardValue(page, 'Low Stock Items'), {
  276 |       timeout: 30_000,
  277 |       message: 'Expected the dashboard low-stock summary to change through the live websocket path.',
  278 |     }).toBeLessThan(beforeLowStock)
  279 |   } finally {
  280 |     if (candidate) {
  281 |       await readJson(await api.post('/api/inventory/update', {
  282 |         data: {
  283 |           productSku: candidate.productSku,
  284 |           warehouseCode: candidate.warehouseCode,
  285 |           quantityAvailable: candidate.quantityAvailable,
  286 |           reorderThreshold: candidate.reorderThreshold,
  287 |         },
  288 |       }))
  289 |     }
  290 |     await api.dispose()
  291 |   }
  292 | })
  293 | 
  294 | test('replay recovery, scenario approval, execution, and browser role gating work through the UI', async ({ page }) => {
  295 |   const replayFixture = await createReplayFixture()
  296 | 
  297 |   try {
  298 |     await loginViaUi(page, users.integrationLead)
  299 |     await navigateWithinApp(page, '/replay-queue')
  300 |     await expect(page.getByRole('heading', { level: 1, name: 'Failed inbound recovery' })).toBeVisible()
  301 | 
  302 |     const replayPanel = page.locator('article.panel').filter({ hasText: replayFixture.externalOrderId }).first()
  303 |     const replayButton = replayPanel.getByRole('button', { name: 'Replay Into Live Flow' }).first()
  304 |     await expect(replayButton).toBeVisible()
  305 |     await replayButton.click()
  306 |     await expect(page.locator('.success-text, .muted-text').filter({ hasText: `Replayed ${replayFixture.externalOrderId} into the live order flow.` }).first()).toBeVisible()
  307 |   } finally {
  308 |     await replayFixture.api.dispose()
  309 |   }
  310 | 
  311 |   await signOutViaUi(page)
  312 | 
  313 |   const scenarioFixture = await createScenarioFixture()
  314 | 
  315 |   try {
  316 |     await loginViaUi(page, users.operationsLead)
  317 |     await navigateWithinApp(page, '/scenario-history')
  318 |     await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()
  319 | 
  320 |     const scenarioApprovalConsole = page.locator('.stack-card').filter({
  321 |       hasText: scenarioFixture.title,
  322 |       has: page.getByRole('button', { name: 'Approve Plan' }),
  323 |     }).first()
  324 |     await expect(scenarioApprovalConsole).toBeVisible()
  325 |     await scenarioApprovalConsole.getByRole('button', { name: 'Approve Plan' }).click()
  326 |     await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()
  327 | 
  328 |     const scenarioExecutionConsole = page.locator('.stack-card').filter({
  329 |       hasText: scenarioFixture.title,
```