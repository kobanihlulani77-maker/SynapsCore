# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> replay recovery, scenario approval, execution, and browser role gating work through the UI
- Location: tests\prod-proof.spec.mjs:611:1

# Error details

```
Error: Expected UI-RPL-F9511556 to reach a replayed state through manual or automated recovery.

expect(received).toBe(expected) // Object.is equality

Expected: "replayed"
Received: "queued"

Call Log:
- Timeout 60000ms exceeded while waiting on the predicate
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
          - button "Dashboard 14" [ref=e18] [cursor=pointer]:
            - generic [ref=e19]: Dashboard
            - strong [ref=e20]: "14"
          - button "Alerts 3" [ref=e21] [cursor=pointer]:
            - generic [ref=e22]: Alerts
            - strong [ref=e23]: "3"
          - button "Recommendations 10" [ref=e24] [cursor=pointer]:
            - generic [ref=e25]: Recommendations
            - strong [ref=e26]: "10"
      - generic [ref=e27]:
        - paragraph [ref=e28]: Operations
        - generic [ref=e29]:
          - button "Orders 4" [ref=e30] [cursor=pointer]:
            - generic [ref=e31]: Orders
            - strong [ref=e32]: "4"
          - button "Inventory 2" [ref=e33] [cursor=pointer]:
            - generic [ref=e34]: Inventory
            - strong [ref=e35]: "2"
          - button "Catalog 30" [ref=e36] [cursor=pointer]:
            - generic [ref=e37]: Catalog
            - strong [ref=e38]: "30"
          - button "Locations 2" [ref=e39] [cursor=pointer]:
            - generic [ref=e40]: Locations
            - strong [ref=e41]: "2"
          - button "Fulfillment 24" [ref=e42] [cursor=pointer]:
            - generic [ref=e43]: Fulfillment
            - strong [ref=e44]: "24"
      - generic [ref=e45]:
        - paragraph [ref=e46]: Control
        - generic [ref=e47]:
          - button "Scenarios 0" [ref=e48] [cursor=pointer]:
            - generic [ref=e49]: Scenarios
            - strong [ref=e50]: "0"
          - button "Scenario History 12" [ref=e51] [cursor=pointer]:
            - generic [ref=e52]: Scenario History
            - strong [ref=e53]: "12"
          - button "Approvals 0" [ref=e54] [cursor=pointer]:
            - generic [ref=e55]: Approvals
            - strong [ref=e56]: "0"
          - button "Escalations 8" [ref=e57] [cursor=pointer]:
            - generic [ref=e58]: Escalations
            - strong [ref=e59]: "8"
      - generic [ref=e60]:
        - paragraph [ref=e61]: Systems
        - generic [ref=e62]:
          - button "Integrations 9" [ref=e63] [cursor=pointer]:
            - generic [ref=e64]: Integrations
            - strong [ref=e65]: "9"
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
          - button "System Config 8" [ref=e93] [cursor=pointer]:
            - generic [ref=e94]: System Config
            - strong [ref=e95]: "8"
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
          - generic [ref=e120]: 2026/04/29, 22:49:51
          - generic [ref=e121]: Reconnecting
          - button "Notifications 12" [ref=e122] [cursor=pointer]
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
              - strong [ref=e149]: "1"
            - article [ref=e150]:
              - text: Replay failed
              - strong [ref=e151]: "0"
            - article [ref=e152]:
              - text: Recovered
              - strong [ref=e153]: "0"
            - article [ref=e154]:
              - text: Connectors
              - strong [ref=e155]: "9"
          - generic [ref=e156]:
            - article [ref=e157]:
              - generic [ref=e158]:
                - strong [ref=e159]: Failed inbound queue
                - generic [ref=e160]: "1"
              - button "UI-RPL-F9511556 Pending ui_replay_f9511556 | WH-NORTH Integration connector ui_replay_f9511556 is disabled and cannot accept CSV imports Attempts 0 | Queued 2026/04/29, 22:48:45" [ref=e162] [cursor=pointer]:
                - generic [ref=e163]:
                  - strong [ref=e164]: UI-RPL-F9511556
                  - generic [ref=e165]: Pending
                - paragraph [ref=e166]: ui_replay_f9511556 | WH-NORTH
                - paragraph [ref=e167]: Integration connector ui_replay_f9511556 is disabled and cannot accept CSV imports
                - paragraph [ref=e168]: Attempts 0 | Queued 2026/04/29, 22:48:45
            - article [ref=e169]:
              - generic [ref=e170]:
                - strong [ref=e171]: Recovery detail
                - generic [ref=e172]: Csv Order Import
              - generic [ref=e173]:
                - generic [ref=e174]:
                  - strong [ref=e175]: UI-RPL-F9511556
                  - paragraph [ref=e176]: Integration connector ui_replay_f9511556 is disabled and cannot accept CSV imports
                  - paragraph [ref=e177]: Source ui_replay_f9511556 | Warehouse WH-NORTH | Attempts 0
                  - paragraph [ref=e178]: Queued 2026/04/29, 22:48:45
                - generic [ref=e179]:
                  - button "Replay Into Live Flow" [ref=e180] [cursor=pointer]
                  - button "View Connector Health" [ref=e181] [cursor=pointer]
                - paragraph [ref=e182]: Recovery keeps failed inbound activity visible, actionable, and auditable instead of hidden inside scripts or operator guesswork.
      - complementary [ref=e183]:
        - article [ref=e184]:
          - generic [ref=e185]:
            - paragraph [ref=e186]: Realtime state
            - generic [ref=e187]: Reconnecting
          - strong [ref=e188]: Monitoring live operating state
          - paragraph [ref=e189]: Snapshot 2026/04/29, 22:49:53
          - generic [ref=e190]:
            - generic [ref=e191]:
              - generic [ref=e192]: Alerts
              - strong [ref=e193]: "3"
            - generic [ref=e194]:
              - generic [ref=e195]: Actions
              - strong [ref=e196]: "12"
            - generic [ref=e197]:
              - generic [ref=e198]: Replay
              - strong [ref=e199]: "1"
            - generic [ref=e200]:
              - generic [ref=e201]: Incidents
              - strong [ref=e202]: "8"
        - article [ref=e203]:
          - generic [ref=e204]:
            - paragraph [ref=e205]: Page focus
            - generic [ref=e206]: Replay Queue
          - strong [ref=e207]: Failed inbound recovery
          - paragraph [ref=e208]: 1 replay item waiting
          - generic [ref=e209]:
            - generic [ref=e210]:
              - generic [ref=e211]: Focus 1
              - strong [ref=e212]: Failed events
            - generic [ref=e213]:
              - generic [ref=e214]: Focus 2
              - strong [ref=e215]: Recovery controls
            - generic [ref=e216]:
              - generic [ref=e217]: Focus 3
              - strong [ref=e218]: Replay history
            - generic [ref=e219]:
              - generic [ref=e220]: Group
              - strong [ref=e221]: Systems
        - article [ref=e222]:
          - generic [ref=e223]:
            - paragraph [ref=e224]: Act now
            - generic [ref=e225]: 5 items
          - generic [ref=e226]:
            - button "Critical Delivery delay risk rising in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e227] [cursor=pointer]:
              - generic [ref=e228]: Critical
              - strong [ref=e229]: Delivery delay risk rising in WH-NORTH
              - paragraph [ref=e230]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Fulfillment backlog building in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e231] [cursor=pointer]:
              - generic [ref=e232]: Critical
              - strong [ref=e233]: Fulfillment backlog building in WH-NORTH
              - paragraph [ref=e234]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Logistics anomaly detected in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e235] [cursor=pointer]:
              - generic [ref=e236]: Critical
              - strong [ref=e237]: Logistics anomaly detected in WH-NORTH
              - paragraph [ref=e238]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Urgent reorder for SKU SKU-RT-934E719D at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e239] [cursor=pointer]:
              - generic [ref=e240]: Critical
              - strong [ref=e241]: Urgent reorder for SKU SKU-RT-934E719D at WH-NORTH
              - paragraph [ref=e242]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
            - button "Critical Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate." [ref=e243] [cursor=pointer]:
              - generic [ref=e244]: Critical
              - strong [ref=e245]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
              - paragraph [ref=e246]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate.
        - article [ref=e247]:
          - generic [ref=e248]:
            - paragraph [ref=e249]: Activity stream
            - generic [ref=e250]: "6"
          - generic [ref=e251]:
            - generic [ref=e252]:
              - strong [ref=e253]: UI Replay F9511556 degraded
              - paragraph [ref=e254]: Medium incident
              - generic [ref=e255]: 2026/04/29, 22:48:47
            - generic [ref=e256]:
              - strong [ref=e257]: Csv Order Import
              - paragraph [ref=e258]: High incident
              - generic [ref=e259]: 2026/04/29, 22:48:45
            - generic [ref=e260]:
              - strong [ref=e261]: Integration Connector Updated
              - paragraph [ref=e262]: Connector ui_replay_f9511556 (CSV_ORDER_IMPORT) updated by Integration Lead with enabled=true and syncMode=BATCH_FILE_DROP.
              - generic [ref=e263]: 2026/04/29, 22:48:46
            - generic [ref=e264]:
              - strong [ref=e265]: Integration Import Processed
              - paragraph [ref=e266]: CSV import processed 0 orders with 1 failures from file orders.csv.
              - generic [ref=e267]: 2026/04/29, 22:48:45
            - generic [ref=e268]:
              - strong [ref=e269]: Integration Connector Updated
              - paragraph [ref=e270]: IntegrationConnector | ui_replay_f9511556:CSV_ORDER_IMPORT
              - generic [ref=e271]: 2026/04/29, 22:48:46
            - generic [ref=e272]:
              - strong [ref=e273]: Csv Order Import
              - paragraph [ref=e274]: IntegrationImport | orders.csv
              - generic [ref=e275]: 2026/04/29, 22:48:45
        - article [ref=e276]:
          - generic [ref=e277]:
            - paragraph [ref=e278]: Operator
            - generic [ref=e279]: Healthy
          - strong [ref=e280]: Hosted Verification Integration Admin
          - paragraph [ref=e281]: PILOT-TENANT Hosted Verification | Integration Lead
          - paragraph [ref=e282]: Roles Integration Admin, Integration Operator
          - paragraph [ref=e283]: Warehouse scope Tenant-wide
```

# Test source

```ts
  539 |       },
  540 |     }))
  541 |     expect(updatedProduct.name).toContain('Updated')
  542 | 
  543 |     const importResult = await readJson(await api.post('/api/products/import', {
  544 |       multipart: {
  545 |         file: {
  546 |           name: 'products.csv',
  547 |           mimeType: 'text/csv',
  548 |           buffer: Buffer.from(
  549 |             `sku,name,category\n${importSku},Imported Product ${suffix},Verification\n${primarySku},Imported Update ${suffix},Verification\n${importSku},Duplicate Product ${suffix},Verification\n`,
  550 |             'utf8',
  551 |           ),
  552 |         },
  553 |       },
  554 |     }))
  555 |     expect(importResult.created).toBe(1)
  556 |     expect(importResult.updated).toBe(1)
  557 |     expect(importResult.failed).toBe(1)
  558 | 
  559 |     const products = await readJson(await api.get('/api/products'))
  560 |     expect(products.some((product) => product.sku === primarySku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  561 |     expect(products.some((product) => product.sku === importSku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  562 | 
  563 |     await loginViaUi(page, users.operationsLead)
  564 |     await navigateWithinApp(page, '/catalog')
  565 |     await expect(page.getByRole('heading', { level: 1, name: 'Tenant product catalog' })).toBeVisible()
  566 |     await expect(page.getByText(primarySku).first()).toBeVisible()
  567 |     await expect(page.getByText(importSku).first()).toBeVisible()
  568 |     await expectNoFatalUiErrors(page)
  569 |   } finally {
  570 |     await api.dispose()
  571 |   }
  572 | })
  573 | 
  574 | test('@realtime dashboard summary updates live without a browser refresh', async ({ page }) => {
  575 |   const api = await createApiContext(users.operationsLead)
  576 |   const realtimeFixture = await createRealtimeInventoryFixture(api)
  577 | 
  578 |   await loginViaUi(page, users.operationsLead)
  579 |   await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  580 |   await expect(page.getByText('Realtime state')).toBeVisible()
  581 | 
  582 |   try {
  583 |     const beforeRisk = await waitForNumericSummaryCard(page, 'Risk')
  584 | 
  585 |     await readJson(await api.post('/api/inventory/update', {
  586 |       data: {
  587 |         productSku: realtimeFixture.productSku,
  588 |         warehouseCode: realtimeFixture.warehouseCode,
  589 |         quantityAvailable: realtimeFixture.lowQuantity,
  590 |         reorderThreshold: realtimeFixture.reorderThreshold,
  591 |       },
  592 |     }))
  593 | 
  594 |     await expect.poll(async () => summaryCardValue(page, 'Risk'), {
  595 |       timeout: 30_000,
  596 |       message: `Expected the dashboard low-stock summary to increase through the live websocket path for ${realtimeFixture.productSku}.`,
  597 |     }).toBe(beforeRisk + 1)
  598 |   } finally {
  599 |     await readJson(await api.post('/api/inventory/update', {
  600 |       data: {
  601 |         productSku: realtimeFixture.productSku,
  602 |         warehouseCode: realtimeFixture.warehouseCode,
  603 |         quantityAvailable: realtimeFixture.safeQuantity,
  604 |         reorderThreshold: realtimeFixture.reorderThreshold,
  605 |       },
  606 |     }))
  607 |     await api.dispose()
  608 |   }
  609 | })
  610 | 
  611 | test('replay recovery, scenario approval, execution, and browser role gating work through the UI', async ({ page }) => {
  612 |   const replayFixture = await createReplayFixture()
  613 | 
  614 |   try {
  615 |     await loginViaUi(page, users.integrationLead)
  616 |     await navigateWithinApp(page, '/replay-queue')
  617 |     await expect(page.getByRole('heading', { level: 1, name: 'Failed inbound recovery' })).toBeVisible()
  618 | 
  619 |     const initialReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  620 |     if (initialReplayOutcome.state === 'queued') {
  621 |       const replayQueueRecord = page.locator('.signal-list-item').filter({ hasText: replayFixture.externalOrderId }).first()
  622 |       if (await replayQueueRecord.isVisible().catch(() => false)) {
  623 |         await replayQueueRecord.click()
  624 |       }
  625 | 
  626 |       const replayButton = page.getByRole('button', { name: 'Replay Into Live Flow' }).first()
  627 |       if (await replayButton.isVisible().catch(() => false) && await replayButton.isEnabled().catch(() => false)) {
  628 |         try {
  629 |           await replayButton.click({ timeout: 10_000 })
  630 |         } catch (error) {
  631 |           const message = error instanceof Error ? error.message : String(error)
  632 |           if (!/detached|timeout/i.test(message)) {
  633 |             throw error
  634 |           }
  635 |         }
  636 |       }
  637 |     }
  638 | 
> 639 |     await expect.poll(async () => (await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)).state, {
      |     ^ Error: Expected UI-RPL-F9511556 to reach a replayed state through manual or automated recovery.
  640 |       timeout: 60_000,
  641 |       message: `Expected ${replayFixture.externalOrderId} to reach a replayed state through manual or automated recovery.`,
  642 |     }).toBe('replayed')
  643 | 
  644 |   await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
  645 |   } finally {
  646 |     await replayFixture.api.dispose()
  647 |   }
  648 | 
  649 |   await signOutViaUi(page)
  650 | 
  651 |   const scenarioFixture = await createScenarioFixture()
  652 | 
  653 |   try {
  654 |     await loginViaUi(page, users.operationsLead)
  655 |     await navigateWithinApp(page, '/scenario-history')
  656 |     await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()
  657 | 
  658 |     const scenarioApprovalConsole = page.locator('.stack-card').filter({
  659 |       hasText: scenarioFixture.title,
  660 |       has: page.getByRole('button', { name: 'Approve Plan' }),
  661 |     }).first()
  662 |     await expect(scenarioApprovalConsole).toBeVisible()
  663 |     await scenarioApprovalConsole.getByRole('button', { name: 'Approve Plan' }).click()
  664 |     await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()
  665 | 
  666 |     const scenarioExecutionConsole = page.locator('.stack-card').filter({
  667 |       hasText: scenarioFixture.title,
  668 |       has: page.getByRole('button', { name: 'Execute Scenario' }),
  669 |     }).first()
  670 |     await expect(scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' })).toBeVisible()
  671 |     await scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' }).click()
  672 |     await expect(page.locator('.success-text').filter({ hasText: new RegExp(`^Executed ${scenarioFixture.title} as live order `, 'i') }).first()).toBeVisible()
  673 |   } finally {
  674 |     await scenarioFixture.api.dispose()
  675 |   }
  676 | 
  677 |   await signOutViaUi(page)
  678 | 
  679 |   await loginViaUi(page, users.operationsPlanner)
  680 |   await navigateWithinApp(page, '/users')
  681 |   await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
  682 |   await expect(page.getByText('Tenant admin access required')).toBeVisible()
  683 |   await expect(page.getByText('Operators', { exact: true }).first()).toBeVisible()
  684 | })
  685 | 
  686 | test('alerts, recommendations, orders, inventory, integrations, users, profile, and settings surfaces stay connected to the live backend', async ({ page }) => {
  687 |   const api = await createApiContext(users.operationsLead)
  688 |   let restoreAlertCoverage = async () => {}
  689 | 
  690 |   try {
  691 |     const alertCoverage = await ensureAlertAndRecommendationCoverage(api)
  692 |     restoreAlertCoverage = alertCoverage.restore
  693 |     const recentOrders = await ensureRecentOrder(api)
  694 |     const workspace = await readJson(await api.get('/api/access/admin/workspace'))
  695 |     const operators = await readJson(await api.get('/api/access/admin/operators'))
  696 |     const accessUsers = await readJson(await api.get('/api/access/admin/users'))
  697 |     const alertRecord = alertCoverage.alertRecord
  698 |     const recommendationRecord = alertCoverage.recommendationRecord
  699 |     const orderRecord = recentOrders[0]
  700 |     const inventoryRecord = alertCoverage.snapshot.inventory.find((item) => item.lowStock) || alertCoverage.snapshot.inventory[0]
  701 |     const connectorRecord = alertCoverage.snapshot.integrationConnectors[0] || workspace.connectors?.[0]
  702 | 
  703 |     expect(alertRecord).toBeTruthy()
  704 |     expect(recommendationRecord).toBeTruthy()
  705 |     expect(orderRecord).toBeTruthy()
  706 |     expect(inventoryRecord).toBeTruthy()
  707 |     expect(workspace).toBeTruthy()
  708 |     expect(operators.length).toBeGreaterThan(0)
  709 |     expect(accessUsers.length).toBeGreaterThan(0)
  710 |     expect(connectorRecord).toBeTruthy()
  711 | 
  712 |     await loginViaUi(page, users.operationsLead)
  713 | 
  714 |     await navigateWithinApp(page, '/alerts')
  715 |     await expect(page.getByRole('heading', { level: 1, name: 'Operational warning center' })).toBeVisible()
  716 |     await expect(page.getByText(alertRecord.title).first()).toBeVisible()
  717 |     await page.locator('.stack-card.selectable-card').filter({ hasText: alertRecord.title }).first().click()
  718 |     await expect(page.getByText(`Action: ${alertRecord.recommendedAction}`).first()).toBeVisible()
  719 | 
  720 |     await navigateWithinApp(page, '/recommendations')
  721 |     await expect(page.getByRole('heading', { level: 1, name: 'Action queue for the operating team' })).toBeVisible()
  722 |     await expect(page.getByText(recommendationRecord.title).first()).toBeVisible()
  723 |     await page.locator('.stack-card.selectable-card').filter({ hasText: recommendationRecord.title }).first().click()
  724 |     await expect(page.getByText(recommendationRecord.description).first()).toBeVisible()
  725 | 
  726 |     await navigateWithinApp(page, '/orders')
  727 |     await expect(page.getByRole('heading', { level: 1, name: 'Live order operations' })).toBeVisible()
  728 |     await expect(page.getByText(orderRecord.externalOrderId).first()).toBeVisible()
  729 |     await page.locator('.stack-card.selectable-card').filter({ hasText: orderRecord.externalOrderId }).first().click()
  730 |     await expect(page.getByText(orderRecord.warehouseCode).first()).toBeVisible()
  731 | 
  732 |     await navigateWithinApp(page, '/inventory')
  733 |     await expect(page.getByRole('heading', { level: 1, name: 'Inventory intelligence' })).toBeVisible()
  734 |     await expect(page.getByText(inventoryRecord.productName).first()).toBeVisible()
  735 |     await page.locator('.stack-card.selectable-card').filter({ hasText: inventoryRecord.productName }).first().click()
  736 |     await expect(page.getByText(inventoryRecord.productSku).first()).toBeVisible()
  737 | 
  738 |     await navigateWithinApp(page, '/integrations')
  739 |     await expect(page.getByRole('heading', { level: 1, name: 'Connector management and telemetry' })).toBeVisible()
```