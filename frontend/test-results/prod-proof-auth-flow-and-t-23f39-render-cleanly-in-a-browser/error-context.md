# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> auth flow and the full authenticated page system render cleanly in a browser
- Location: tests\prod-proof.spec.mjs:321:1

# Error details

```
Error: expect(locator).toBeEnabled() failed

Locator: locator('.public-signin-card').getByRole('button', { name: 'Enter Platform' })
Expected: enabled
Timeout: 60000ms
Error: element(s) not found

Call log:
  - Expect "toBeEnabled" with timeout 60000ms
  - waiting for locator('.public-signin-card').getByRole('button', { name: 'Enter Platform' })

```

# Page snapshot

```yaml
- main [ref=e3]:
  - generic [ref=e4]:
    - button "S SynapseCore Operational intelligence operating system" [ref=e5] [cursor=pointer]:
      - generic [ref=e6]: S
      - generic [ref=e7]:
        - strong [ref=e8]: SynapseCore
        - generic [ref=e9]: Operational intelligence operating system
    - navigation [ref=e10]:
      - button "Home" [ref=e11] [cursor=pointer]
      - button "Product" [ref=e12] [cursor=pointer]
      - button "Contact" [ref=e13] [cursor=pointer]
  - generic [ref=e14]:
    - article [ref=e15]:
      - paragraph [ref=e16]: Secure company entry
      - heading "Access your operational workspace." [level=1] [ref=e17]
      - paragraph [ref=e18]: Sign in to the right company workspace and move from visibility to action without leaving the control center.
      - generic [ref=e19]:
        - generic [ref=e20]: Checking workspace directory
        - generic [ref=e21]: Realtime path ready
      - generic [ref=e22]:
        - article [ref=e23]:
          - strong [ref=e24]: Live visibility
          - paragraph [ref=e25]: Orders, inventory, locations, fulfillment, incidents, and connectors pulled into one operational picture.
        - article [ref=e26]:
          - strong [ref=e27]: Prediction and guidance
          - paragraph [ref=e28]: Detect risk early, estimate near-term impact, and surface what the team should do next.
        - article [ref=e29]:
          - strong [ref=e30]: Control and trust
          - paragraph [ref=e31]: Run scenarios, route approvals, recover failed inbound work, and keep runtime confidence visible.
    - article [ref=e32]:
      - paragraph [ref=e33]: Company sign in
      - heading "Enter the operational platform" [level=2] [ref=e34]
      - paragraph [ref=e39]: Loading available workspaces...
      - generic [ref=e40]:
        - generic [ref=e41]:
          - generic [ref=e42]:
            - generic [ref=e43]: Tenant workspace
            - combobox "Tenant workspace" [disabled] [ref=e44]: PILOT-TENANT
          - generic [ref=e45]:
            - generic [ref=e46]: Username
            - textbox "Username" [disabled] [ref=e47]:
              - /placeholder: workspace.admin
              - text: admin.pilot
          - generic [ref=e48]:
            - generic [ref=e49]: Password
            - textbox "Password" [disabled] [ref=e50]:
              - /placeholder: Enter workspace password
              - text: wrong-code
        - generic [ref=e51]:
          - generic [ref=e52]:
            - checkbox "Remember tenant code and username on this device" [checked] [ref=e53]
            - generic [ref=e54]: Remember tenant code and username on this device
          - generic [ref=e55]: Password recovery is managed by your tenant admin.
        - generic [ref=e56]:
          - button "Opening Workspace..." [disabled] [ref=e57]
          - button "Product Overview" [ref=e58] [cursor=pointer]
      - generic [ref=e59]:
        - article [ref=e60]:
          - generic [ref=e61]: Tenant scope
          - strong [ref=e62]: PILOT-TENANT
          - paragraph [ref=e63]: Operators enter a company workspace, not a generic app account.
        - article [ref=e64]:
          - generic [ref=e65]: Session model
          - strong [ref=e66]: Secure browser session
          - paragraph [ref=e67]: Protected actions, approvals, replay, and realtime access all follow the signed-in operator identity.
        - article [ref=e68]:
          - generic [ref=e69]: Realtime posture
          - strong [ref=e70]: Live transport configured
          - paragraph [ref=e71]: SynapseCore opens the command workspace with live operational updates when the session is valid.
      - paragraph [ref=e72]: Loading the active workspace directory so operators can sign in against the live tenant list.
      - paragraph [ref=e73]: API https://synapscore-3.onrender.com | Realtime wss://synapscore-3.onrender.com/ws | Transport undefined
```

# Test source

```ts
  229 |     const csvImportResponse = await api.post('/api/integrations/orders/csv-import', {
  230 |       multipart: {
  231 |         file: {
  232 |           name: 'orders.csv',
  233 |           mimeType: 'text/csv',
  234 |           buffer: Buffer.from(
  235 |             `externalOrderId,warehouseCode,productSku,quantity,unitPrice\n${externalOrderId},WH-NORTH,${proofProductSku},2,88.00\n`,
  236 |             'utf8',
  237 |           ),
  238 |         },
  239 |         sourceSystem,
  240 |       },
  241 |     })
  242 |     const csvImportPayload = await readJson(csvImportResponse)
  243 |     expect(csvImportPayload.ordersFailed).toBe(1)
  244 | 
  245 |     await readJson(await api.post('/api/integrations/orders/connectors', {
  246 |       data: {
  247 |         sourceSystem,
  248 |         type: 'CSV_ORDER_IMPORT',
  249 |         displayName: `UI Replay ${suffix}`,
  250 |         enabled: true,
  251 |         syncMode: 'BATCH_FILE_DROP',
  252 |         validationPolicy: 'RELAXED',
  253 |         transformationPolicy: 'NORMALIZE_CODES',
  254 |         allowDefaultWarehouseFallback: false,
  255 |         notes: 'Enabled for replay verification.',
  256 |       },
  257 |     }))
  258 | 
  259 |     await expect.poll(async () => {
  260 |       const connectors = await readJson(await api.get('/api/integrations/orders/connectors'))
  261 |       return connectors.find((connector) => connector.sourceSystem === sourceSystem && connector.type === 'CSV_ORDER_IMPORT')?.enabled ?? false
  262 |     }, {
  263 |       timeout: 30_000,
  264 |       message: `Expected replay verification connector ${sourceSystem} to become enabled before UI replay proof.`,
  265 |     }).toBe(true)
  266 | 
  267 |     await expect.poll(async () => {
  268 |       const replayQueue = await readJson(await api.get('/api/integrations/orders/replay-queue'))
  269 |       const replayRecord = replayQueue.find((record) => record.externalOrderId === externalOrderId)
  270 |       if (!replayRecord) {
  271 |         return 'missing'
  272 |       }
  273 |       if (replayRecord.nextEligibleAt && Date.parse(replayRecord.nextEligibleAt) > Date.now()) {
  274 |         return 'waiting'
  275 |       }
  276 |       return replayRecord.status
  277 |     }, {
  278 |       timeout: 30_000,
  279 |       message: `Expected replay verification record ${externalOrderId} to be present and eligible before UI replay proof.`,
  280 |     }).toBe('PENDING')
  281 | 
  282 |     return { api, sourceSystem, externalOrderId }
  283 |   } catch (error) {
  284 |     await api.dispose()
  285 |     throw error
  286 |   } finally {
  287 |     await inventoryAdmin.dispose()
  288 |   }
  289 | }
  290 | 
  291 | async function createScenarioFixture() {
  292 |   const api = await createApiContext(users.operationsLead)
  293 |   const suffix = randomUUID().slice(0, 8).toUpperCase()
  294 |   const title = `UI Scenario ${suffix}`
  295 | 
  296 |   try {
  297 |     await readJson(await api.post('/api/scenarios/save', {
  298 |       data: {
  299 |         title,
  300 |         requestedBy: 'Operations Lead',
  301 |         request: {
  302 |           warehouseCode: 'WH-NORTH',
  303 |           items: [
  304 |             {
  305 |               productSku: proofProductSku,
  306 |               quantity: 1,
  307 |               unitPrice: 95,
  308 |             },
  309 |           ],
  310 |         },
  311 |       },
  312 |     }))
  313 | 
  314 |     return { api, title }
  315 |   } catch (error) {
  316 |     await api.dispose()
  317 |     throw error
  318 |   }
  319 | }
  320 | 
  321 | test('auth flow and the full authenticated page system render cleanly in a browser', async ({ page }) => {
  322 |   await page.goto('/dashboard')
  323 |   await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  324 |   const signInCard = page.locator('.public-signin-card')
  325 |   await waitForSignInReady(signInCard)
  326 | 
  327 |   await fillSignInForm(signInCard, users.operationsLead, 'wrong-code')
  328 |   await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
> 329 |   await expect(signInCard.getByRole('button', { name: 'Enter Platform' })).toBeEnabled({ timeout: 60_000 })
      |                                                                            ^ Error: expect(locator).toBeEnabled() failed
  330 |   await expect(signInCard.getByText('Invalid operator credentials.')).toBeVisible({ timeout: 15_000 })
  331 | 
  332 |   await waitForSignInReady(signInCard)
  333 |   await fillSignInForm(signInCard, users.operationsLead, users.operationsLead.password)
  334 |   await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  335 |   await expect(page).toHaveURL(/\/dashboard$/)
  336 | 
  337 |   for (const [route, title] of appPages) {
  338 |     await navigateWithinApp(page, route)
  339 |     await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
  340 |     await expect(page.locator('.workspace-topbar')).toBeVisible()
  341 |     await expectNoFatalUiErrors(page)
  342 |   }
  343 | 
  344 |   await signOutViaUi(page)
  345 | })
  346 | 
  347 | test('product catalog onboarding works through tenant-scoped API and browser surface', async ({ page }) => {
  348 |   const api = await createApiContext(users.operationsLead)
  349 |   const suffix = randomUUID().slice(0, 8).toUpperCase()
  350 |   const primarySku = `SKU-UI-${suffix}`
  351 |   const importSku = `SKU-IMP-${suffix}`
  352 | 
  353 |   try {
  354 |     const createdProduct = await readJson(await api.post('/api/products', {
  355 |       data: {
  356 |         sku: primarySku,
  357 |         name: `UI Catalog ${suffix}`,
  358 |         category: 'Verification',
  359 |       },
  360 |     }))
  361 |     expect(createdProduct.sku).toBe(primarySku)
  362 |     expect(createdProduct.tenantCode).toBe(users.operationsLead.tenantCode)
  363 | 
  364 |     const updatedProduct = await readJson(await api.put(`/api/products/${createdProduct.id}`, {
  365 |       data: {
  366 |         sku: primarySku,
  367 |         name: `UI Catalog ${suffix} Updated`,
  368 |         category: 'Verification',
  369 |       },
  370 |     }))
  371 |     expect(updatedProduct.name).toContain('Updated')
  372 | 
  373 |     const importResult = await readJson(await api.post('/api/products/import', {
  374 |       multipart: {
  375 |         file: {
  376 |           name: 'products.csv',
  377 |           mimeType: 'text/csv',
  378 |           buffer: Buffer.from(
  379 |             `sku,name,category\n${importSku},Imported Product ${suffix},Verification\n${primarySku},Imported Update ${suffix},Verification\n${importSku},Duplicate Product ${suffix},Verification\n`,
  380 |             'utf8',
  381 |           ),
  382 |         },
  383 |       },
  384 |     }))
  385 |     expect(importResult.created).toBe(1)
  386 |     expect(importResult.updated).toBe(1)
  387 |     expect(importResult.failed).toBe(1)
  388 | 
  389 |     const products = await readJson(await api.get('/api/products'))
  390 |     expect(products.some((product) => product.sku === primarySku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  391 |     expect(products.some((product) => product.sku === importSku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  392 | 
  393 |     await loginViaUi(page, users.operationsLead)
  394 |     await navigateWithinApp(page, '/catalog')
  395 |     await expect(page.getByRole('heading', { level: 1, name: 'Tenant product catalog' })).toBeVisible()
  396 |     await expect(page.getByText(primarySku).first()).toBeVisible()
  397 |     await expect(page.getByText(importSku).first()).toBeVisible()
  398 |     await expectNoFatalUiErrors(page)
  399 |   } finally {
  400 |     await api.dispose()
  401 |   }
  402 | })
  403 | 
  404 | test('@realtime dashboard summary updates live without a browser refresh', async ({ page }) => {
  405 |   await loginViaUi(page, users.operationsLead)
  406 |   await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  407 |   await expect(page.getByText('Realtime state')).toBeVisible()
  408 | 
  409 |   const api = await createApiContext(users.operationsLead)
  410 |   let candidate = null
  411 |   let revertQuantity = null
  412 |   let revertThreshold = null
  413 | 
  414 |   try {
  415 |     const inventory = await readJson(await api.get('/api/inventory'))
  416 |     candidate = inventory.find((item) => item.productSku === proofProductSku && item.warehouseCode === 'WH-NORTH')
  417 |       || inventory.find((item) => Number.isFinite(item.quantityAvailable) && Number.isFinite(item.reorderThreshold))
  418 |     expect(candidate).toBeTruthy()
  419 | 
  420 |     revertQuantity = candidate.quantityAvailable
  421 |     revertThreshold = candidate.reorderThreshold
  422 | 
  423 |     const beforeRisk = await waitForNumericSummaryCard(page, 'Risk')
  424 |     const threshold = Number.isFinite(candidate.reorderThreshold) ? candidate.reorderThreshold : 5
  425 |     const forceLowQuantity = Math.max(0, threshold - 1)
  426 |     const safeQuantity = threshold + 5
  427 | 
  428 |     await readJson(await api.post('/api/inventory/update', {
  429 |       data: {
```