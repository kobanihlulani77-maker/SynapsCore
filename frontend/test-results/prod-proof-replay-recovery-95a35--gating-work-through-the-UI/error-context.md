# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> replay recovery, scenario approval, execution, and browser role gating work through the UI
- Location: tests\prod-proof.spec.mjs:400:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('.success-text, .muted-text').filter({ hasText: 'Replayed UI-RPL-909DCFAD into the live order flow.' }).first()
Expected: visible
Timeout: 20000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 20000ms
  - waiting for locator('.success-text, .muted-text').filter({ hasText: 'Replayed UI-RPL-909DCFAD into the live order flow.' }).first()

```

# Page snapshot

```yaml
- generic [ref=e3]:
  - complementary [ref=e4]:
    - button "S SynapseCore PILOT-TENANT Hosted Verification" [ref=e5] [cursor=pointer]:
      - generic [ref=e6]: S
      - generic [ref=e7]:
        - strong [ref=e8]: SynapseCore
        - generic [ref=e9]: PILOT-TENANT Hosted Verification
    - generic [ref=e10]:
      - generic [ref=e11]: Workspace
      - strong [ref=e12]: PILOT-TENANT Hosted Verification
      - paragraph [ref=e13]: Hosted Verification Integration Admin | Integration Lead
    - navigation [ref=e14]:
      - generic [ref=e15]:
        - paragraph [ref=e16]: Overview
        - generic [ref=e17]:
          - button "Dashboard 2" [ref=e18] [cursor=pointer]:
            - generic [ref=e19]: Dashboard
            - strong [ref=e20]: "2"
          - button "Alerts 1" [ref=e21] [cursor=pointer]:
            - generic [ref=e22]: Alerts
            - strong [ref=e23]: "1"
          - button "Recommendations 3" [ref=e24] [cursor=pointer]:
            - generic [ref=e25]: Recommendations
            - strong [ref=e26]: "3"
      - generic [ref=e27]:
        - paragraph [ref=e28]: Operations
        - generic [ref=e29]:
          - button "Orders 2" [ref=e30] [cursor=pointer]:
            - generic [ref=e31]: Orders
            - strong [ref=e32]: "2"
          - button "Inventory 1" [ref=e33] [cursor=pointer]:
            - generic [ref=e34]: Inventory
            - strong [ref=e35]: "1"
          - button "Catalog 7" [ref=e36] [cursor=pointer]:
            - generic [ref=e37]: Catalog
            - strong [ref=e38]: "7"
          - button "Locations 2" [ref=e39] [cursor=pointer]:
            - generic [ref=e40]: Locations
            - strong [ref=e41]: "2"
          - button "Fulfillment 2" [ref=e42] [cursor=pointer]:
            - generic [ref=e43]: Fulfillment
            - strong [ref=e44]: "2"
      - generic [ref=e45]:
        - paragraph [ref=e46]: Control
        - generic [ref=e47]:
          - button "Scenarios 0" [ref=e48] [cursor=pointer]:
            - generic [ref=e49]: Scenarios
            - strong [ref=e50]: "0"
          - button "Scenario History 2" [ref=e51] [cursor=pointer]:
            - generic [ref=e52]: Scenario History
            - strong [ref=e53]: "2"
          - button "Approvals 0" [ref=e54] [cursor=pointer]:
            - generic [ref=e55]: Approvals
            - strong [ref=e56]: "0"
          - button "Escalations 8" [ref=e57] [cursor=pointer]:
            - generic [ref=e58]: Escalations
            - strong [ref=e59]: "8"
      - generic [ref=e60]:
        - paragraph [ref=e61]: Systems
        - generic [ref=e62]:
          - button "Integrations 2" [ref=e63] [cursor=pointer]:
            - generic [ref=e64]: Integrations
            - strong [ref=e65]: "2"
          - button "Replay Queue 1" [ref=e66] [cursor=pointer]:
            - generic [ref=e67]: Replay Queue
            - strong [ref=e68]: "1"
          - button "Runtime 8" [ref=e69] [cursor=pointer]:
            - generic [ref=e70]: Runtime
            - strong [ref=e71]: "8"
          - button "Audit & Events 20" [ref=e72] [cursor=pointer]:
            - generic [ref=e73]: Audit & Events
            - strong [ref=e74]: "20"
      - generic [ref=e75]:
        - paragraph [ref=e76]: Settings
        - generic [ref=e77]:
          - button "Users 0" [ref=e78] [cursor=pointer]:
            - generic [ref=e79]: Users
            - strong [ref=e80]: "0"
          - button "Company Settings 0" [ref=e81] [cursor=pointer]:
            - generic [ref=e82]: Company Settings
            - strong [ref=e83]: "0"
          - button "Profile 1" [ref=e84] [cursor=pointer]:
            - generic [ref=e85]: Profile
            - strong [ref=e86]: "1"
          - button "Platform Admin 8" [ref=e87] [cursor=pointer]:
            - generic [ref=e88]: Platform Admin
            - strong [ref=e89]: "8"
          - button "Tenant Management 3" [ref=e90] [cursor=pointer]:
            - generic [ref=e91]: Tenant Management
            - strong [ref=e92]: "3"
          - button "System Config 0" [ref=e93] [cursor=pointer]:
            - generic [ref=e94]: System Config
            - strong [ref=e95]: "0"
          - button "Releases 1" [ref=e96] [cursor=pointer]:
            - generic [ref=e97]: Releases
            - strong [ref=e98]: "1"
    - generic [ref=e99]:
      - generic [ref=e102]: Realtime Reconnecting
      - button "Profile & Session" [ref=e103] [cursor=pointer]
  - generic [ref=e104]:
    - banner [ref=e105]:
      - generic [ref=e106]:
        - paragraph [ref=e107]: SYSTEMS
        - heading "Failed inbound recovery" [level=1] [ref=e108]
        - paragraph [ref=e109]: Inspect failed inbound work, understand why it broke, and replay it safely into the live flow.
      - generic [ref=e110]:
        - generic [ref=e112]:
          - generic [ref=e113]: Global search
          - textbox "Global search" [ref=e114]:
            - /placeholder: Search pages, orders, or alerts
        - generic "Page focus" [ref=e115]:
          - generic [ref=e116] [cursor=pointer]: Failed events
          - generic [ref=e117] [cursor=pointer]: Recovery controls
          - generic [ref=e118] [cursor=pointer]: Replay history
        - generic [ref=e119]:
          - generic [ref=e120]: 2026/04/24, 13:16:13
          - generic [ref=e121]: Reconnecting
          - button "Notifications 10" [ref=e122] [cursor=pointer]
        - generic [ref=e123]:
          - button "Open alerts" [ref=e124] [cursor=pointer]
          - button "Open approvals" [ref=e125] [cursor=pointer]
          - button "Refresh" [ref=e126] [cursor=pointer]
          - button "Hosted Verification Integration Admin" [ref=e127] [cursor=pointer]
          - button "Sign Out" [ref=e128] [cursor=pointer]
    - generic [ref=e129]:
      - main [ref=e130]:
        - generic [ref=e131]:
          - generic [ref=e132]:
            - paragraph [ref=e133]: Current page
            - heading "Replay Queue" [level=2] [ref=e134]
            - paragraph [ref=e135]: 1 replay item waiting
          - generic [ref=e136]:
            - generic [ref=e137]: Failed events
            - generic [ref=e138]: Recovery controls
            - generic [ref=e139]: Replay history
        - article [ref=e141]:
          - generic [ref=e142]:
            - generic [ref=e143]:
              - paragraph [ref=e144]: Replay queue
              - heading "Recover failed inbound work safely" [level=2] [ref=e145]
            - generic [ref=e146]: "1"
          - generic [ref=e147]:
            - article [ref=e148]:
              - text: Waiting
              - strong [ref=e149]: "0"
            - article [ref=e150]:
              - text: Replay failed
              - strong [ref=e151]: "1"
            - article [ref=e152]:
              - text: Recovered
              - strong [ref=e153]: "0"
            - article [ref=e154]:
              - text: Connectors
              - strong [ref=e155]: "2"
          - generic [ref=e156]:
            - article [ref=e157]:
              - generic [ref=e158]:
                - strong [ref=e159]: Failed inbound queue
                - generic [ref=e160]: "1"
              - paragraph [ref=e161]: Integration replay record 2 is not eligible for replay until 2026-04-24T11:21:09.030029Z.
              - button "UI-RPL-909DCFAD Replay Failed ui_replay_909dcfad | WH-NORTH Integration connector ui_replay_909dcfad is disabled and cannot accept CSV imports Attempts 1 | Queued 2026/04/24, 13:16:01" [ref=e163] [cursor=pointer]:
                - generic [ref=e164]:
                  - strong [ref=e165]: UI-RPL-909DCFAD
                  - generic [ref=e166]: Replay Failed
                - paragraph [ref=e167]: ui_replay_909dcfad | WH-NORTH
                - paragraph [ref=e168]: Integration connector ui_replay_909dcfad is disabled and cannot accept CSV imports
                - paragraph [ref=e169]: Attempts 1 | Queued 2026/04/24, 13:16:01
            - article [ref=e170]:
              - generic [ref=e171]:
                - strong [ref=e172]: Recovery detail
                - generic [ref=e173]: Csv Order Import
              - generic [ref=e174]:
                - generic [ref=e175]:
                  - strong [ref=e176]: UI-RPL-909DCFAD
                  - paragraph [ref=e177]: Integration connector ui_replay_909dcfad is disabled and cannot accept CSV imports
                  - paragraph [ref=e178]: Source ui_replay_909dcfad | Warehouse WH-NORTH | Attempts 1
                  - paragraph [ref=e179]: Last attempted 2026/04/24, 13:16:09 | Queued 2026/04/24, 13:16:01
                  - paragraph [ref=e180]: "Last replay note: Integration connector ui_replay_909dcfad is disabled and cannot replay failed inbound orders"
                - generic [ref=e181]:
                  - button "Replay Into Live Flow" [ref=e182] [cursor=pointer]
                  - button "View Connector Health" [ref=e183] [cursor=pointer]
                - paragraph [ref=e184]: Recovery keeps failed inbound activity visible, actionable, and auditable instead of hidden inside scripts or operator guesswork.
      - complementary [ref=e185]:
        - article [ref=e186]:
          - generic [ref=e187]:
            - paragraph [ref=e188]: Realtime state
            - generic [ref=e189]: Reconnecting
          - strong [ref=e190]: Monitoring live operating state
          - paragraph [ref=e191]: Snapshot 2026/04/24, 13:16:17
          - generic [ref=e192]:
            - generic [ref=e193]:
              - generic [ref=e194]: Alerts
              - strong [ref=e195]: "1"
            - generic [ref=e196]:
              - generic [ref=e197]: Actions
              - strong [ref=e198]: "3"
            - generic [ref=e199]:
              - generic [ref=e200]: Replay
              - strong [ref=e201]: "1"
            - generic [ref=e202]:
              - generic [ref=e203]: Incidents
              - strong [ref=e204]: "8"
        - article [ref=e205]:
          - generic [ref=e206]:
            - paragraph [ref=e207]: Page focus
            - generic [ref=e208]: Replay Queue
          - strong [ref=e209]: Failed inbound recovery
          - paragraph [ref=e210]: 1 replay item waiting
          - generic [ref=e211]:
            - generic [ref=e212]:
              - generic [ref=e213]: Focus 1
              - strong [ref=e214]: Failed events
            - generic [ref=e215]:
              - generic [ref=e216]: Focus 2
              - strong [ref=e217]: Recovery controls
            - generic [ref=e218]:
              - generic [ref=e219]: Focus 3
              - strong [ref=e220]: Replay history
            - generic [ref=e221]:
              - generic [ref=e222]: Group
              - strong [ref=e223]: Systems
        - article [ref=e224]:
          - generic [ref=e225]:
            - paragraph [ref=e226]: Act now
            - generic [ref=e227]: 4 items
          - generic [ref=e228]:
            - button "High Fulfillment backlog building in WH-NORTH Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 2 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now." [ref=e229] [cursor=pointer]:
              - generic [ref=e230]: High
              - strong [ref=e231]: Fulfillment backlog building in WH-NORTH
              - paragraph [ref=e232]: Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 2 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now.
            - button "Medium Reorder stock for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH Plan replenishment for PILOT-TENANT Hosted Verification North Hub. Current quantity is 8 units versus a threshold of 12." [ref=e233] [cursor=pointer]:
              - generic [ref=e234]: Medium
              - strong [ref=e235]: Reorder stock for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
              - paragraph [ref=e236]: Plan replenishment for PILOT-TENANT Hosted Verification North Hub. Current quantity is 8 units versus a threshold of 12.
            - button "Medium Prioritize fulfillment backlog for WH-NORTH Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 2 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now." [ref=e237] [cursor=pointer]:
              - generic [ref=e238]: Medium
              - strong [ref=e239]: Prioritize fulfillment backlog for WH-NORTH
              - paragraph [ref=e240]: Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 2 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now.
            - button "Medium Prioritize fulfillment backlog for WH-NORTH Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 1 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now." [ref=e241] [cursor=pointer]:
              - generic [ref=e242]: Medium
              - strong [ref=e243]: Prioritize fulfillment backlog for WH-NORTH
              - paragraph [ref=e244]: Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 1 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now.
        - article [ref=e245]:
          - generic [ref=e246]:
            - paragraph [ref=e247]: Activity stream
            - generic [ref=e248]: "6"
          - generic [ref=e249]:
            - generic [ref=e250]:
              - strong [ref=e251]: Csv Order Import
              - paragraph [ref=e252]: High incident
              - generic [ref=e253]: 2026/04/24, 13:16:10
            - generic [ref=e254]:
              - strong [ref=e255]: UI Replay 909DCFAD degraded
              - paragraph [ref=e256]: Medium incident
              - generic [ref=e257]: 2026/04/24, 13:16:09
            - generic [ref=e258]:
              - strong [ref=e259]: Integration Connector Updated
              - paragraph [ref=e260]: Connector ui_replay_909dcfad (CSV_ORDER_IMPORT) updated by Integration Lead with enabled=true and syncMode=BATCH_FILE_DROP.
              - generic [ref=e261]: 2026/04/24, 13:16:10
            - generic [ref=e262]:
              - strong [ref=e263]: Integration Replay Failed
              - paragraph [ref=e264]: "Replay failed for UI-RPL-909DCFAD from ui_replay_909dcfad by system-replay. Reason: Integration connector ui_replay_909dcfad is disabled and cannot replay failed inbound orders"
              - generic [ref=e265]: 2026/04/24, 13:16:09
            - generic [ref=e266]:
              - strong [ref=e267]: Integration Connector Updated
              - paragraph [ref=e268]: IntegrationConnector | ui_replay_909dcfad:CSV_ORDER_IMPORT
              - generic [ref=e269]: 2026/04/24, 13:16:10
            - generic [ref=e270]:
              - strong [ref=e271]: Csv Order Import
              - paragraph [ref=e272]: IntegrationImport | orders.csv
              - generic [ref=e273]: 2026/04/24, 13:16:10
        - article [ref=e274]:
          - generic [ref=e275]:
            - paragraph [ref=e276]: Operator
            - generic [ref=e277]: Healthy
          - strong [ref=e278]: Hosted Verification Integration Admin
          - paragraph [ref=e279]: PILOT-TENANT Hosted Verification | Integration Lead
          - paragraph [ref=e280]: Roles Integration Admin, Integration Operator
          - paragraph [ref=e281]: Warehouse scope Tenant-wide
```

# Test source

```ts
  312 |     }))
  313 |     expect(createdProduct.sku).toBe(primarySku)
  314 |     expect(createdProduct.tenantCode).toBe(users.operationsLead.tenantCode)
  315 | 
  316 |     const updatedProduct = await readJson(await api.put(`/api/products/${createdProduct.id}`, {
  317 |       data: {
  318 |         sku: primarySku,
  319 |         name: `UI Catalog ${suffix} Updated`,
  320 |         category: 'Verification',
  321 |       },
  322 |     }))
  323 |     expect(updatedProduct.name).toContain('Updated')
  324 | 
  325 |     const importResult = await readJson(await api.post('/api/products/import', {
  326 |       multipart: {
  327 |         file: {
  328 |           name: 'products.csv',
  329 |           mimeType: 'text/csv',
  330 |           buffer: Buffer.from(
  331 |             `sku,name,category\n${importSku},Imported Product ${suffix},Verification\n${primarySku},Imported Update ${suffix},Verification\n${importSku},Duplicate Product ${suffix},Verification\n`,
  332 |             'utf8',
  333 |           ),
  334 |         },
  335 |       },
  336 |     }))
  337 |     expect(importResult.created).toBe(1)
  338 |     expect(importResult.updated).toBe(1)
  339 |     expect(importResult.failed).toBe(1)
  340 | 
  341 |     const products = await readJson(await api.get('/api/products'))
  342 |     expect(products.some((product) => product.sku === primarySku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  343 |     expect(products.some((product) => product.sku === importSku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  344 | 
  345 |     await loginViaUi(page, users.operationsLead)
  346 |     await navigateWithinApp(page, '/catalog')
  347 |     await expect(page.getByRole('heading', { level: 1, name: 'Tenant product catalog' })).toBeVisible()
  348 |     await expect(page.getByText(primarySku).first()).toBeVisible()
  349 |     await expect(page.getByText(importSku).first()).toBeVisible()
  350 |     await expectNoFatalUiErrors(page)
  351 |   } finally {
  352 |     await api.dispose()
  353 |   }
  354 | })
  355 | 
  356 | test('@realtime dashboard summary updates live without a browser refresh', async ({ page }) => {
  357 |   await loginViaUi(page, users.operationsLead)
  358 |   await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  359 |   await expect(page.getByText('Realtime state')).toBeVisible()
  360 | 
  361 |   const api = await createApiContext(users.operationsLead)
  362 |   let candidate = null
  363 | 
  364 |   try {
  365 |     const inventory = await readJson(await api.get('/api/inventory'))
  366 |     candidate = inventory.find((item) => item.lowStock && Number.isFinite(item.quantityAvailable) && Number.isFinite(item.reorderThreshold))
  367 |     expect(candidate).toBeTruthy()
  368 | 
  369 |     const beforeLowStock = await waitForNumericSummaryCard(page, 'Risk')
  370 |     const nextQuantity = candidate.reorderThreshold + 5
  371 | 
  372 |     await readJson(await api.post('/api/inventory/update', {
  373 |       data: {
  374 |         productSku: candidate.productSku,
  375 |         warehouseCode: candidate.warehouseCode,
  376 |         quantityAvailable: nextQuantity,
  377 |         reorderThreshold: candidate.reorderThreshold,
  378 |       },
  379 |     }))
  380 | 
  381 |     await expect.poll(async () => summaryCardValue(page, 'Risk'), {
  382 |       timeout: 30_000,
  383 |       message: 'Expected the dashboard low-stock summary to change through the live websocket path.',
  384 |     }).toBeLessThan(beforeLowStock)
  385 |   } finally {
  386 |     if (candidate) {
  387 |       await readJson(await api.post('/api/inventory/update', {
  388 |         data: {
  389 |           productSku: candidate.productSku,
  390 |           warehouseCode: candidate.warehouseCode,
  391 |           quantityAvailable: candidate.quantityAvailable,
  392 |           reorderThreshold: candidate.reorderThreshold,
  393 |         },
  394 |       }))
  395 |     }
  396 |     await api.dispose()
  397 |   }
  398 | })
  399 | 
  400 | test('replay recovery, scenario approval, execution, and browser role gating work through the UI', async ({ page }) => {
  401 |   const replayFixture = await createReplayFixture()
  402 | 
  403 |   try {
  404 |     await loginViaUi(page, users.integrationLead)
  405 |     await navigateWithinApp(page, '/replay-queue')
  406 |     await expect(page.getByRole('heading', { level: 1, name: 'Failed inbound recovery' })).toBeVisible()
  407 | 
  408 |     const replayPanel = page.locator('article.panel').filter({ hasText: replayFixture.externalOrderId }).first()
  409 |     const replayButton = replayPanel.getByRole('button', { name: 'Replay Into Live Flow' }).first()
  410 |     await expect(replayButton).toBeVisible()
  411 |     await replayButton.click()
> 412 |     await expect(page.locator('.success-text, .muted-text').filter({ hasText: `Replayed ${replayFixture.externalOrderId} into the live order flow.` }).first()).toBeVisible()
      |                                                                                                                                                                 ^ Error: expect(locator).toBeVisible() failed
  413 |   } finally {
  414 |     await replayFixture.api.dispose()
  415 |   }
  416 | 
  417 |   await signOutViaUi(page)
  418 | 
  419 |   const scenarioFixture = await createScenarioFixture()
  420 | 
  421 |   try {
  422 |     await loginViaUi(page, users.operationsLead)
  423 |     await navigateWithinApp(page, '/scenario-history')
  424 |     await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()
  425 | 
  426 |     const scenarioApprovalConsole = page.locator('.stack-card').filter({
  427 |       hasText: scenarioFixture.title,
  428 |       has: page.getByRole('button', { name: 'Approve Plan' }),
  429 |     }).first()
  430 |     await expect(scenarioApprovalConsole).toBeVisible()
  431 |     await scenarioApprovalConsole.getByRole('button', { name: 'Approve Plan' }).click()
  432 |     await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()
  433 | 
  434 |     const scenarioExecutionConsole = page.locator('.stack-card').filter({
  435 |       hasText: scenarioFixture.title,
  436 |       has: page.getByRole('button', { name: 'Execute Scenario' }),
  437 |     }).first()
  438 |     await expect(scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' })).toBeVisible()
  439 |     await scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' }).click()
  440 |     await expect(page.locator('.success-text').filter({ hasText: new RegExp(`^Executed ${scenarioFixture.title} as live order `, 'i') }).first()).toBeVisible()
  441 |   } finally {
  442 |     await scenarioFixture.api.dispose()
  443 |   }
  444 | 
  445 |   await signOutViaUi(page)
  446 | 
  447 |   await loginViaUi(page, users.operationsPlanner)
  448 |   await navigateWithinApp(page, '/users')
  449 |   await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
  450 |   await expect(page.getByText('Tenant admin access required')).toBeVisible()
  451 |   await expect(page.getByText('Operators', { exact: true }).first()).toBeVisible()
  452 | })
  453 | 
```