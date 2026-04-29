# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> alerts, recommendations, orders, inventory, integrations, users, profile, and settings surfaces stay connected to the live backend
- Location: tests\prod-proof.spec.mjs:629:1

# Error details

```
Error: Expected low-stock inventory on SKU-PILOT-TENANT-PROOF to produce alert and recommendation coverage.

expect(received).toBe(expected) // Object.is equality

Expected: true
Received: false

Call Log:
- Timeout 30000ms exceeded while waiting on the predicate
```

# Test source

```ts
  223 |         transformationPolicy: 'NORMALIZE_CODES',
  224 |         allowDefaultWarehouseFallback: false,
  225 |         notes: 'Disposable replay verification connector.',
  226 |       },
  227 |     }))
  228 | 
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
  321 | async function waitForSnapshotMatch(api, predicate, message) {
  322 |   let latestSnapshot = null
> 323 |   await expect.poll(async () => {
      |   ^ Error: Expected low-stock inventory on SKU-PILOT-TENANT-PROOF to produce alert and recommendation coverage.
  324 |     latestSnapshot = await readJson(await api.get('/api/dashboard/snapshot'))
  325 |     return Boolean(predicate(latestSnapshot))
  326 |   }, {
  327 |     timeout: 30_000,
  328 |     message,
  329 |   }).toBe(true)
  330 |   return latestSnapshot
  331 | }
  332 | 
  333 | async function ensureRecentOrder(api) {
  334 |   const recentOrders = await readJson(await api.get('/api/orders/recent'))
  335 |   if (recentOrders.length) {
  336 |     return recentOrders
  337 |   }
  338 | 
  339 |   const suffix = randomUUID().slice(0, 8).toUpperCase()
  340 |   await readJson(await api.post('/api/orders', {
  341 |     data: {
  342 |       externalOrderId: `UI-ORD-${suffix}`,
  343 |       warehouseCode: 'WH-NORTH',
  344 |       items: [
  345 |         {
  346 |           productSku: proofProductSku,
  347 |           quantity: 1,
  348 |           unitPrice: 79,
  349 |         },
  350 |       ],
  351 |     },
  352 |   }))
  353 | 
  354 |   let nextOrders = []
  355 |   await expect.poll(async () => {
  356 |     nextOrders = await readJson(await api.get('/api/orders/recent'))
  357 |     return nextOrders.length > 0
  358 |   }, {
  359 |     timeout: 30_000,
  360 |     message: 'Expected at least one recent order to appear after seeding the hosted proof order lane.',
  361 |   }).toBe(true)
  362 |   return nextOrders
  363 | }
  364 | 
  365 | async function ensureAlertAndRecommendationCoverage(api) {
  366 |   const inventory = await readJson(await api.get('/api/inventory'))
  367 |   const candidate = inventory.find((item) => item.productSku === proofProductSku && item.warehouseCode === 'WH-NORTH')
  368 |     || inventory.find((item) => Number.isFinite(item.quantityAvailable) && Number.isFinite(item.reorderThreshold))
  369 | 
  370 |   expect(candidate).toBeTruthy()
  371 | 
  372 |   const initialSnapshot = await readJson(await api.get('/api/dashboard/snapshot'))
  373 |   if (initialSnapshot.alerts.length && initialSnapshot.recommendations.length) {
  374 |     return {
  375 |       snapshot: initialSnapshot,
  376 |       candidate,
  377 |       restore: async () => {},
  378 |     }
  379 |   }
  380 | 
  381 |   const revertQuantity = candidate.quantityAvailable
  382 |   const revertThreshold = candidate.reorderThreshold
  383 |   const threshold = Number.isFinite(candidate.reorderThreshold) ? candidate.reorderThreshold : 5
  384 | 
  385 |   await readJson(await api.post('/api/inventory/update', {
  386 |     data: {
  387 |       productSku: candidate.productSku,
  388 |       warehouseCode: candidate.warehouseCode,
  389 |       quantityAvailable: Math.max(0, threshold - 1),
  390 |       reorderThreshold: threshold,
  391 |     },
  392 |   }))
  393 | 
  394 |   const snapshot = await waitForSnapshotMatch(
  395 |     api,
  396 |     (nextSnapshot) => nextSnapshot.alerts.length > 0 && nextSnapshot.recommendations.length > 0,
  397 |     `Expected low-stock inventory on ${candidate.productSku} to produce alert and recommendation coverage.`,
  398 |   )
  399 | 
  400 |   return {
  401 |     snapshot,
  402 |     candidate,
  403 |     restore: async () => {
  404 |       await readJson(await api.post('/api/inventory/update', {
  405 |         data: {
  406 |           productSku: candidate.productSku,
  407 |           warehouseCode: candidate.warehouseCode,
  408 |           quantityAvailable: revertQuantity,
  409 |           reorderThreshold: revertThreshold,
  410 |         },
  411 |       }))
  412 |     },
  413 |   }
  414 | }
  415 | 
  416 | test('auth flow and the full authenticated page system render cleanly in a browser', async ({ page }) => {
  417 |   await page.goto('/dashboard')
  418 |   await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  419 |   const signInCard = page.locator('.public-signin-card')
  420 |   await waitForSignInReady(signInCard)
  421 | 
  422 |   await fillSignInForm(signInCard, users.operationsLead, 'wrong-code')
  423 |   await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
```