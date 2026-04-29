# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> replay recovery, scenario approval, execution, and browser role gating work through the UI
- Location: tests\prod-proof.spec.mjs:640:1

# Error details

```
Error: The request conflicts with the current SynapseCore operational data.
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
          - button "Dashboard 18" [ref=e18] [cursor=pointer]:
            - generic [ref=e19]: Dashboard
            - strong [ref=e20]: "18"
          - button "Alerts 4" [ref=e21] [cursor=pointer]:
            - generic [ref=e22]: Alerts
            - strong [ref=e23]: "4"
          - button "Recommendations 22" [ref=e24] [cursor=pointer]:
            - generic [ref=e25]: Recommendations
            - strong [ref=e26]: "22"
      - generic [ref=e27]:
        - paragraph [ref=e28]: Operations
        - generic [ref=e29]:
          - button "Orders 8" [ref=e30] [cursor=pointer]:
            - generic [ref=e31]: Orders
            - strong [ref=e32]: "8"
          - button "Inventory 6" [ref=e33] [cursor=pointer]:
            - generic [ref=e34]: Inventory
            - strong [ref=e35]: "6"
          - button "Catalog 42" [ref=e36] [cursor=pointer]:
            - generic [ref=e37]: Catalog
            - strong [ref=e38]: "42"
          - button "Locations 2" [ref=e39] [cursor=pointer]:
            - generic [ref=e40]: Locations
            - strong [ref=e41]: "2"
          - button "Fulfillment 28" [ref=e42] [cursor=pointer]:
            - generic [ref=e43]: Fulfillment
            - strong [ref=e44]: "28"
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
          - button "Integrations 12" [ref=e63] [cursor=pointer]:
            - generic [ref=e64]: Integrations
            - strong [ref=e65]: "12"
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
          - generic [ref=e120]: 2026/04/29, 23:26:26
          - generic [ref=e121]: Reconnecting
          - button "Notifications 13" [ref=e122] [cursor=pointer]
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
              - strong [ref=e155]: "12"
          - generic [ref=e156]:
            - article [ref=e157]:
              - generic [ref=e158]:
                - strong [ref=e159]: Failed inbound queue
                - generic [ref=e160]: "1"
              - paragraph [ref=e161]: The request conflicts with the current SynapseCore operational data.
              - button "UI-RPL-FA2F5384 Pending ui_replay_fa2f5384 | WH-NORTH Integration connector ui_replay_fa2f5384 is disabled and cannot accept CSV imports Attempts 0 | Queued 2026/04/29, 23:26:23" [ref=e163] [cursor=pointer]:
                - generic [ref=e164]:
                  - strong [ref=e165]: UI-RPL-FA2F5384
                  - generic [ref=e166]: Pending
                - paragraph [ref=e167]: ui_replay_fa2f5384 | WH-NORTH
                - paragraph [ref=e168]: Integration connector ui_replay_fa2f5384 is disabled and cannot accept CSV imports
                - paragraph [ref=e169]: Attempts 0 | Queued 2026/04/29, 23:26:23
            - article [ref=e170]:
              - generic [ref=e171]:
                - strong [ref=e172]: Recovery detail
                - generic [ref=e173]: Csv Order Import
              - generic [ref=e174]:
                - generic [ref=e175]:
                  - strong [ref=e176]: UI-RPL-FA2F5384
                  - paragraph [ref=e177]: Integration connector ui_replay_fa2f5384 is disabled and cannot accept CSV imports
                  - paragraph [ref=e178]: Source ui_replay_fa2f5384 | Warehouse WH-NORTH | Attempts 0
                  - paragraph [ref=e179]: Queued 2026/04/29, 23:26:23
                - generic [ref=e180]:
                  - button "Replay Into Live Flow" [ref=e181] [cursor=pointer]
                  - button "View Connector Health" [ref=e182] [cursor=pointer]
                - paragraph [ref=e183]: Recovery keeps failed inbound activity visible, actionable, and auditable instead of hidden inside scripts or operator guesswork.
      - complementary [ref=e184]:
        - article [ref=e185]:
          - generic [ref=e186]:
            - paragraph [ref=e187]: Realtime state
            - generic [ref=e188]: Reconnecting
          - strong [ref=e189]: Monitoring live operating state
          - paragraph [ref=e190]: Snapshot 2026/04/29, 23:26:36
          - generic [ref=e191]:
            - generic [ref=e192]:
              - generic [ref=e193]: Alerts
              - strong [ref=e194]: "4"
            - generic [ref=e195]:
              - generic [ref=e196]: Actions
              - strong [ref=e197]: "12"
            - generic [ref=e198]:
              - generic [ref=e199]: Replay
              - strong [ref=e200]: "1"
            - generic [ref=e201]:
              - generic [ref=e202]: Incidents
              - strong [ref=e203]: "8"
        - article [ref=e204]:
          - generic [ref=e205]:
            - paragraph [ref=e206]: Page focus
            - generic [ref=e207]: Replay Queue
          - strong [ref=e208]: Failed inbound recovery
          - paragraph [ref=e209]: 1 replay item waiting
          - generic [ref=e210]:
            - generic [ref=e211]:
              - generic [ref=e212]: Focus 1
              - strong [ref=e213]: Failed events
            - generic [ref=e214]:
              - generic [ref=e215]: Focus 2
              - strong [ref=e216]: Recovery controls
            - generic [ref=e217]:
              - generic [ref=e218]: Focus 3
              - strong [ref=e219]: Replay history
            - generic [ref=e220]:
              - generic [ref=e221]: Group
              - strong [ref=e222]: Systems
        - article [ref=e223]:
          - generic [ref=e224]:
            - paragraph [ref=e225]: Act now
            - generic [ref=e226]: 5 items
          - generic [ref=e227]:
            - button "Critical Delivery delay risk rising in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 18, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e228] [cursor=pointer]:
              - generic [ref=e229]: Critical
              - strong [ref=e230]: Delivery delay risk rising in WH-NORTH
              - paragraph [ref=e231]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 18, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Fulfillment backlog building in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 18, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e232] [cursor=pointer]:
              - generic [ref=e233]: Critical
              - strong [ref=e234]: Fulfillment backlog building in WH-NORTH
              - paragraph [ref=e235]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 18, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Logistics anomaly detected in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 18, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e236] [cursor=pointer]:
              - generic [ref=e237]: Critical
              - strong [ref=e238]: Logistics anomaly detected in WH-NORTH
              - paragraph [ref=e239]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 18, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Urgent reorder for SKU SKU-RT-29391937 at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e240] [cursor=pointer]:
              - generic [ref=e241]: Critical
              - strong [ref=e242]: Urgent reorder for SKU SKU-RT-29391937 at WH-NORTH
              - paragraph [ref=e243]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
            - button "Critical Urgent reorder for SKU SKU-RT-9B8F56D2 at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e244] [cursor=pointer]:
              - generic [ref=e245]: Critical
              - strong [ref=e246]: Urgent reorder for SKU SKU-RT-9B8F56D2 at WH-NORTH
              - paragraph [ref=e247]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
        - article [ref=e248]:
          - generic [ref=e249]:
            - paragraph [ref=e250]: Activity stream
            - generic [ref=e251]: "6"
          - generic [ref=e252]:
            - generic [ref=e253]:
              - strong [ref=e254]: UI Replay FA2F5384 degraded
              - paragraph [ref=e255]: Medium incident
              - generic [ref=e256]: 2026/04/29, 23:26:24
            - generic [ref=e257]:
              - strong [ref=e258]: Csv Order Import
              - paragraph [ref=e259]: High incident
              - generic [ref=e260]: 2026/04/29, 23:26:23
            - generic [ref=e261]:
              - strong [ref=e262]: Integration Connector Updated
              - paragraph [ref=e263]: Connector ui_replay_fa2f5384 (CSV_ORDER_IMPORT) updated by Integration Lead with enabled=true and syncMode=BATCH_FILE_DROP.
              - generic [ref=e264]: 2026/04/29, 23:26:23
            - generic [ref=e265]:
              - strong [ref=e266]: Integration Import Processed
              - paragraph [ref=e267]: CSV import processed 0 orders with 1 failures from file orders.csv.
              - generic [ref=e268]: 2026/04/29, 23:26:23
            - generic [ref=e269]:
              - strong [ref=e270]: Integration Connector Updated
              - paragraph [ref=e271]: IntegrationConnector | ui_replay_fa2f5384:CSV_ORDER_IMPORT
              - generic [ref=e272]: 2026/04/29, 23:26:23
            - generic [ref=e273]:
              - strong [ref=e274]: Csv Order Import
              - paragraph [ref=e275]: IntegrationImport | orders.csv
              - generic [ref=e276]: 2026/04/29, 23:26:23
        - article [ref=e277]:
          - generic [ref=e278]:
            - paragraph [ref=e279]: Operator
            - generic [ref=e280]: Healthy
          - strong [ref=e281]: Hosted Verification Integration Admin
          - paragraph [ref=e282]: PILOT-TENANT Hosted Verification | Integration Lead
          - paragraph [ref=e283]: Roles Integration Admin, Integration Operator
          - paragraph [ref=e284]: Warehouse scope Tenant-wide
```

# Test source

```ts
  598 | 
  599 | test('@realtime dashboard summary updates live without a browser refresh', async ({ page }) => {
  600 |   const api = await createApiContext(users.operationsLead)
  601 |   const realtimeFixture = await createRealtimeInventoryFixture(api)
  602 | 
  603 |   await loginViaUi(page, users.operationsLead)
  604 |   await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  605 |   await expect(page.getByText('Realtime state')).toBeVisible()
  606 | 
  607 |   try {
  608 |     const beforeRisk = await waitForNumericSummaryCard(page, 'Risk')
  609 |     const expectedAlertTitle = `Low stock detected for SKU ${realtimeFixture.productSku} in ${realtimeFixture.warehouseCode}`
  610 |     const expectedRecommendationTitle = `Urgent reorder for SKU ${realtimeFixture.productSku} at ${realtimeFixture.warehouseCode}`
  611 | 
  612 |     await readJson(await api.post('/api/inventory/update', {
  613 |       data: {
  614 |         productSku: realtimeFixture.productSku,
  615 |         warehouseCode: realtimeFixture.warehouseCode,
  616 |         quantityAvailable: realtimeFixture.lowQuantity,
  617 |         reorderThreshold: realtimeFixture.reorderThreshold,
  618 |       },
  619 |     }))
  620 | 
  621 |     await expect(page.getByText(expectedAlertTitle).first()).toBeVisible({ timeout: 30_000 })
  622 |     await expect(page.getByText(expectedRecommendationTitle).first()).toBeVisible({ timeout: 30_000 })
  623 |     await expect.poll(async () => summaryCardValue(page, 'Risk'), {
  624 |       timeout: 30_000,
  625 |       message: `Expected the dashboard low-stock summary to increase through the live websocket path for ${realtimeFixture.productSku}.`,
  626 |     }).toBeGreaterThanOrEqual(beforeRisk + 1)
  627 |   } finally {
  628 |     await readJson(await api.post('/api/inventory/update', {
  629 |       data: {
  630 |         productSku: realtimeFixture.productSku,
  631 |         warehouseCode: realtimeFixture.warehouseCode,
  632 |         quantityAvailable: realtimeFixture.safeQuantity,
  633 |         reorderThreshold: realtimeFixture.reorderThreshold,
  634 |       },
  635 |     }))
  636 |     await api.dispose()
  637 |   }
  638 | })
  639 | 
  640 | test('replay recovery, scenario approval, execution, and browser role gating work through the UI', async ({ page }) => {
  641 |   const replayFixture = await createReplayFixture()
  642 | 
  643 |   try {
  644 |     await loginViaUi(page, users.integrationLead)
  645 |     await navigateWithinApp(page, '/replay-queue')
  646 |     await expect(page.getByRole('heading', { level: 1, name: 'Failed inbound recovery' })).toBeVisible()
  647 | 
  648 |     let currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  649 |     if (currentReplayOutcome.state === 'queued') {
  650 |       const replayQueueRecord = page.locator('.signal-list-item.selectable-card').filter({ hasText: replayFixture.externalOrderId }).first()
  651 |       await expect.poll(async () => {
  652 |         currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  653 |         if (currentReplayOutcome.state === 'replayed') {
  654 |           return 'replayed'
  655 |         }
  656 |         await refreshWorkspace(page)
  657 |         return await replayQueueRecord.isVisible().catch(() => false) ? 'visible' : 'waiting'
  658 |       }, {
  659 |         timeout: 30_000,
  660 |         message: `Expected replay queue ${replayFixture.externalOrderId} to appear in the UI or auto-recover before manual replay.`,
  661 |       }).not.toBe('waiting')
  662 | 
  663 |       if (currentReplayOutcome.state === 'replayed') {
  664 |         await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
  665 |       } else {
  666 |         await expect(replayQueueRecord).toBeVisible()
  667 |         await replayQueueRecord.click()
  668 | 
  669 |         const replayDetail = page.locator('.section-card').filter({ hasText: 'Recovery detail' }).first()
  670 |         await expect(replayDetail.getByText(replayFixture.externalOrderId).first()).toBeVisible()
  671 | 
  672 |         const replayButton = replayDetail.getByRole('button', { name: 'Replay Into Live Flow' })
  673 |         await expect(replayButton).toBeVisible()
  674 |         await expect(replayButton).toBeEnabled()
  675 | 
  676 |         const replayResponsePromise = page.waitForResponse((response) => (
  677 |           response.request().method() === 'POST'
  678 |             && /\/api\/integrations\/orders\/replay\/\d+$/i.test(response.url())
  679 |         ), { timeout: 20_000 })
  680 | 
  681 |         let replayResponse = null
  682 |         try {
  683 |           await replayButton.scrollIntoViewIfNeeded()
  684 |           ;[replayResponse] = await Promise.all([
  685 |             replayResponsePromise,
  686 |             replayButton.click(),
  687 |           ])
  688 |         } catch (error) {
  689 |           currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  690 |           if (currentReplayOutcome.state !== 'replayed') {
  691 |             throw error
  692 |           }
  693 |         }
  694 | 
  695 |         if (replayResponse) {
  696 |           const replayPayload = await replayResponse.json().catch(() => null)
  697 |           if (!replayResponse.ok()) {
> 698 |             throw new Error(
      |                   ^ Error: The request conflicts with the current SynapseCore operational data.
  699 |               replayPayload?.message
  700 |                 || `Replay request failed with status ${replayResponse.status()} for ${replayFixture.externalOrderId}.`,
  701 |             )
  702 |           }
  703 |           await expect(page.locator('.success-text').filter({
  704 |             hasText: `Replayed ${replayFixture.externalOrderId} into the live order flow.`,
  705 |           }).first()).toBeVisible()
  706 |         }
  707 |       }
  708 |     }
  709 | 
  710 |     await expect.poll(async () => {
  711 |       const replayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  712 |       return replayOutcome.state === 'queued' ? `${replayOutcome.state}:${replayOutcome.status}` : replayOutcome.state
  713 |     }, {
  714 |       timeout: 60_000,
  715 |       message: `Expected ${replayFixture.externalOrderId} to reach a replayed state through manual or automated recovery.`,
  716 |     }).toBe('replayed')
  717 | 
  718 |   await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
  719 |   } finally {
  720 |     await replayFixture.api.dispose()
  721 |   }
  722 | 
  723 |   await signOutViaUi(page)
  724 | 
  725 |   const scenarioFixture = await createScenarioFixture()
  726 | 
  727 |   try {
  728 |     await loginViaUi(page, users.operationsLead)
  729 |     await navigateWithinApp(page, '/scenario-history')
  730 |     await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()
  731 | 
  732 |     const scenarioApprovalConsole = page.locator('.stack-card').filter({
  733 |       hasText: scenarioFixture.title,
  734 |       has: page.getByRole('button', { name: 'Approve Plan' }),
  735 |     }).first()
  736 |     await expect(scenarioApprovalConsole).toBeVisible()
  737 |     await scenarioApprovalConsole.getByRole('button', { name: 'Approve Plan' }).click()
  738 |     await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()
  739 | 
  740 |     const scenarioExecutionConsole = page.locator('.stack-card').filter({
  741 |       hasText: scenarioFixture.title,
  742 |       has: page.getByRole('button', { name: 'Execute Scenario' }),
  743 |     }).first()
  744 |     await expect(scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' })).toBeVisible()
  745 |     await scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' }).click()
  746 |     await expect(page.locator('.success-text').filter({ hasText: new RegExp(`^Executed ${scenarioFixture.title} as live order `, 'i') }).first()).toBeVisible()
  747 |   } finally {
  748 |     await scenarioFixture.api.dispose()
  749 |   }
  750 | 
  751 |   await signOutViaUi(page)
  752 | 
  753 |   await loginViaUi(page, users.operationsPlanner)
  754 |   await navigateWithinApp(page, '/users')
  755 |   await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
  756 |   await expect(page.getByText('Tenant admin access required')).toBeVisible()
  757 |   await expect(page.getByText('Operators', { exact: true }).first()).toBeVisible()
  758 | })
  759 | 
  760 | test('alerts, recommendations, orders, inventory, integrations, users, profile, and settings surfaces stay connected to the live backend', async ({ page }) => {
  761 |   const api = await createApiContext(users.operationsLead)
  762 |   let restoreAlertCoverage = async () => {}
  763 | 
  764 |   try {
  765 |     const alertCoverage = await ensureAlertAndRecommendationCoverage(api)
  766 |     restoreAlertCoverage = alertCoverage.restore
  767 |     const recentOrders = await ensureRecentOrder(api)
  768 |     const workspace = await readJson(await api.get('/api/access/admin/workspace'))
  769 |     const operators = await readJson(await api.get('/api/access/admin/operators'))
  770 |     const accessUsers = await readJson(await api.get('/api/access/admin/users'))
  771 |     const alertRecord = alertCoverage.alertRecord
  772 |     const recommendationRecord = alertCoverage.recommendationRecord
  773 |     const orderRecord = recentOrders[0]
  774 |     const inventoryRecord = alertCoverage.snapshot.inventory.find((item) => item.lowStock) || alertCoverage.snapshot.inventory[0]
  775 |     const connectorRecord = alertCoverage.snapshot.integrationConnectors[0] || workspace.connectors?.[0]
  776 | 
  777 |     expect(alertRecord).toBeTruthy()
  778 |     expect(recommendationRecord).toBeTruthy()
  779 |     expect(orderRecord).toBeTruthy()
  780 |     expect(inventoryRecord).toBeTruthy()
  781 |     expect(workspace).toBeTruthy()
  782 |     expect(operators.length).toBeGreaterThan(0)
  783 |     expect(accessUsers.length).toBeGreaterThan(0)
  784 |     expect(connectorRecord).toBeTruthy()
  785 | 
  786 |     await loginViaUi(page, users.operationsLead)
  787 | 
  788 |     await navigateWithinApp(page, '/alerts')
  789 |     await expect(page.getByRole('heading', { level: 1, name: 'Operational warning center' })).toBeVisible()
  790 |     await expect(page.getByText(alertRecord.title).first()).toBeVisible()
  791 |     await activateSelectableButton(
  792 |       page.getByRole('button', { name: new RegExp(escapeRegExp(alertRecord.title), 'i') }).first(),
  793 |     )
  794 |     await expect(page.getByText(`Action: ${alertRecord.recommendedAction}`).first()).toBeVisible()
  795 | 
  796 |     await navigateWithinApp(page, '/recommendations')
  797 |     await expect(page.getByRole('heading', { level: 1, name: 'Action queue for the operating team' })).toBeVisible()
  798 |     await expect(page.getByText(recommendationRecord.title).first()).toBeVisible()
```