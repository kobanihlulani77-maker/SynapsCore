# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> alerts, recommendations, orders, inventory, integrations, users, profile, and settings surfaces stay connected to the live backend
- Location: tests\prod-proof.spec.mjs:846:1

# Error details

```
TypeError: page.getByDisplayValue is not a function
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
      - paragraph [ref=e13]: Hosted Verification Tenant Admin | Operations Lead
    - navigation [ref=e14]:
      - generic [ref=e15]:
        - paragraph [ref=e16]: Overview
        - generic [ref=e17]:
          - button "Dashboard 25" [ref=e18] [cursor=pointer]:
            - generic [ref=e19]: Dashboard
            - strong [ref=e20]: "25"
          - button "Alerts 4" [ref=e21] [cursor=pointer]:
            - generic [ref=e22]: Alerts
            - strong [ref=e23]: "4"
          - button "Recommendations 49" [ref=e24] [cursor=pointer]:
            - generic [ref=e25]: Recommendations
            - strong [ref=e26]: "49"
      - generic [ref=e27]:
        - paragraph [ref=e28]: Operations
        - generic [ref=e29]:
          - button "Orders 15" [ref=e30] [cursor=pointer]:
            - generic [ref=e31]: Orders
            - strong [ref=e32]: "15"
          - button "Inventory 11" [ref=e33] [cursor=pointer]:
            - generic [ref=e34]: Inventory
            - strong [ref=e35]: "11"
          - button "Catalog 55" [ref=e36] [cursor=pointer]:
            - generic [ref=e37]: Catalog
            - strong [ref=e38]: "55"
          - button "Locations 2" [ref=e39] [cursor=pointer]:
            - generic [ref=e40]: Locations
            - strong [ref=e41]: "2"
          - button "Fulfillment 35" [ref=e42] [cursor=pointer]:
            - generic [ref=e43]: Fulfillment
            - strong [ref=e44]: "35"
      - generic [ref=e45]:
        - paragraph [ref=e46]: Control
        - generic [ref=e47]:
          - button "Scenarios 2" [ref=e48] [cursor=pointer]:
            - generic [ref=e49]: Scenarios
            - strong [ref=e50]: "2"
          - button "Scenario History 12" [ref=e51] [cursor=pointer]:
            - generic [ref=e52]: Scenario History
            - strong [ref=e53]: "12"
          - button "Approvals 2" [ref=e54] [cursor=pointer]:
            - generic [ref=e55]: Approvals
            - strong [ref=e56]: "2"
          - button "Escalations 8" [ref=e57] [cursor=pointer]:
            - generic [ref=e58]: Escalations
            - strong [ref=e59]: "8"
      - generic [ref=e60]:
        - paragraph [ref=e61]: Systems
        - generic [ref=e62]:
          - button "Integrations 16" [ref=e63] [cursor=pointer]:
            - generic [ref=e64]: Integrations
            - strong [ref=e65]: "16"
          - button "Replay Queue 0" [ref=e66] [cursor=pointer]:
            - generic [ref=e67]: Replay Queue
            - strong [ref=e68]: "0"
          - button "Runtime 8" [ref=e69] [cursor=pointer]:
            - generic [ref=e70]: Runtime
            - strong [ref=e71]: "8"
          - button "Audit & Events 20" [ref=e72] [cursor=pointer]:
            - generic [ref=e73]: Audit & Events
            - strong [ref=e74]: "20"
      - generic [ref=e75]:
        - paragraph [ref=e76]: Settings
        - generic [ref=e77]:
          - button "Users 4" [ref=e78] [cursor=pointer]:
            - generic [ref=e79]: Users
            - strong [ref=e80]: "4"
          - button "Company Settings 2" [ref=e81] [cursor=pointer]:
            - generic [ref=e82]: Company Settings
            - strong [ref=e83]: "2"
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
        - paragraph [ref=e107]: ADMIN
        - heading "Tenant and workspace settings" [level=1] [ref=e108]
        - paragraph [ref=e109]: Configure workspace metadata, security policies, warehouse details, and connector support ownership.
      - generic [ref=e110]:
        - generic [ref=e112]:
          - generic [ref=e113]: Global search
          - textbox "Global search" [ref=e114]:
            - /placeholder: Search pages, orders, or alerts
        - generic "Page focus" [ref=e115]:
          - button "Workspace profile" [ref=e116] [cursor=pointer]
          - button "Security policy" [ref=e117] [cursor=pointer]
          - button "Connector ownership" [ref=e118] [cursor=pointer]
        - generic [ref=e119]:
          - generic [ref=e120]: 2026/04/30, 00:00:51
          - generic [ref=e121]: Reconnecting
          - button "Notifications 14" [ref=e122] [cursor=pointer]
        - generic [ref=e123]:
          - button "Open users" [ref=e124] [cursor=pointer]
          - button "Open platform" [ref=e125] [cursor=pointer]
          - button "Refresh" [ref=e126] [cursor=pointer]
          - button "Hosted Verification Tenant Admin" [ref=e127] [cursor=pointer]
          - button "Sign Out" [ref=e128] [cursor=pointer]
    - generic [ref=e129]:
      - main [ref=e130]:
        - generic [ref=e131]:
          - generic [ref=e132]:
            - paragraph [ref=e133]: Current page
            - heading "Company Settings" [level=2] [ref=e134]
            - paragraph [ref=e135]: Workspace controls ready for tenant configuration
          - generic [ref=e136]:
            - generic [ref=e137]: Workspace profile
            - generic [ref=e138]: Security policy
            - generic [ref=e139]: Connector ownership
        - article [ref=e141]:
          - generic [ref=e142]:
            - generic [ref=e143]:
              - paragraph [ref=e144]: Company settings
              - heading "Workspace profile, security, and connector policy" [level=2] [ref=e145]
            - generic [ref=e146]: "2"
          - generic [ref=e147]:
            - article [ref=e148]:
              - text: Rotation days
              - strong [ref=e149]: "90"
            - article [ref=e150]:
              - text: Session timeout
              - strong [ref=e151]: "480"
            - article [ref=e152]:
              - text: Warehouse lanes
              - strong [ref=e153]: "2"
            - article [ref=e154]:
              - text: Connectors
              - strong [ref=e155]: "16"
          - generic [ref=e156]:
            - article [ref=e157]:
              - generic [ref=e158]:
                - strong [ref=e159]: Workspace profile
                - generic [ref=e160]: Editable
              - generic [ref=e161]:
                - generic [ref=e162]:
                  - generic [ref=e163]: Tenant Name
                  - textbox "Tenant Name" [ref=e164]:
                    - /placeholder: Tenant workspace name
                    - text: PILOT-TENANT Hosted Verification
                - generic [ref=e165]:
                  - generic [ref=e166]: Description
                  - textbox "Description" [ref=e167]:
                    - /placeholder: Operational workspace summary
                    - text: Hosted technical verification workspace.
              - button "Save Workspace" [ref=e169] [cursor=pointer]
            - article [ref=e170]:
              - generic [ref=e171]:
                - strong [ref=e172]: Security policy
                - generic [ref=e173]: Tenant policy
              - generic [ref=e174]:
                - generic [ref=e175]:
                  - generic [ref=e176]: Password Rotation Days
                  - textbox "Password Rotation Days" [ref=e177]: "90"
                - generic [ref=e178]:
                  - generic [ref=e179]: Session Timeout Minutes
                  - textbox "Session Timeout Minutes" [ref=e180]: "480"
                - generic [ref=e181]:
                  - generic [ref=e182]: Invalidate Other Sessions
                  - checkbox "Invalidate Other Sessions" [ref=e183]
              - button "Save Security Policy" [ref=e185] [cursor=pointer]
          - generic [ref=e186]:
            - article [ref=e187]:
              - generic [ref=e188]:
                - strong [ref=e189]: Warehouse focus
                - generic [ref=e190]: WH-COAST
              - generic [ref=e191]:
                - button "PILOT-TENANT Hosted Verification Coast Hub WH-COAST Verification Coast Hub" [ref=e192] [cursor=pointer]:
                  - strong [ref=e193]: PILOT-TENANT Hosted Verification Coast Hub
                  - paragraph [ref=e194]: WH-COAST
                  - paragraph [ref=e195]: Verification Coast Hub
                - button "PILOT-TENANT Hosted Verification North Hub WH-NORTH Verification North Hub" [ref=e196] [cursor=pointer]:
                  - strong [ref=e197]: PILOT-TENANT Hosted Verification North Hub
                  - paragraph [ref=e198]: WH-NORTH
                  - paragraph [ref=e199]: Verification North Hub
              - generic [ref=e200]:
                - generic [ref=e201]:
                  - generic [ref=e202]: Name
                  - textbox "Name" [ref=e203]: PILOT-TENANT Hosted Verification Coast Hub
                - generic [ref=e204]:
                  - generic [ref=e205]: Location
                  - textbox "Location" [ref=e206]: Verification Coast Hub
              - button "Save Warehouse" [ref=e208] [cursor=pointer]
            - article [ref=e209]:
              - generic [ref=e210]:
                - strong [ref=e211]: Connector focus
                - generic [ref=e212]: Batch File Drop
              - generic [ref=e213]:
                - button "UI Replay 074ED74C ui_replay_074ed74c | Batch File Drop No support owner assigned yet" [ref=e214] [cursor=pointer]:
                  - strong [ref=e215]: UI Replay 074ED74C
                  - paragraph [ref=e216]: ui_replay_074ed74c | Batch File Drop
                  - paragraph [ref=e217]: No support owner assigned yet
                - button "UI Replay 07675F90 ui_replay_07675f90 | Batch File Drop No support owner assigned yet" [ref=e218] [cursor=pointer]:
                  - strong [ref=e219]: UI Replay 07675F90
                  - paragraph [ref=e220]: ui_replay_07675f90 | Batch File Drop
                  - paragraph [ref=e221]: No support owner assigned yet
                - button "UI Replay 0C32AF78 ui_replay_0c32af78 | Batch File Drop No support owner assigned yet" [ref=e222] [cursor=pointer]:
                  - strong [ref=e223]: UI Replay 0C32AF78
                  - paragraph [ref=e224]: ui_replay_0c32af78 | Batch File Drop
                  - paragraph [ref=e225]: No support owner assigned yet
                - button "UI Replay 322EDAF1 ui_replay_322edaf1 | Batch File Drop No support owner assigned yet" [ref=e226] [cursor=pointer]:
                  - strong [ref=e227]: UI Replay 322EDAF1
                  - paragraph [ref=e228]: ui_replay_322edaf1 | Batch File Drop
                  - paragraph [ref=e229]: No support owner assigned yet
                - button "UI Replay 4839AA05 ui_replay_4839aa05 | Batch File Drop No support owner assigned yet" [ref=e230] [cursor=pointer]:
                  - strong [ref=e231]: UI Replay 4839AA05
                  - paragraph [ref=e232]: ui_replay_4839aa05 | Batch File Drop
                  - paragraph [ref=e233]: No support owner assigned yet
                - button "UI Replay 53E2D6F3 ui_replay_53e2d6f3 | Batch File Drop No support owner assigned yet" [ref=e234] [cursor=pointer]:
                  - strong [ref=e235]: UI Replay 53E2D6F3
                  - paragraph [ref=e236]: ui_replay_53e2d6f3 | Batch File Drop
                  - paragraph [ref=e237]: No support owner assigned yet
                - button "UI Replay 7E089F4F ui_replay_7e089f4f | Batch File Drop No support owner assigned yet" [ref=e238] [cursor=pointer]:
                  - strong [ref=e239]: UI Replay 7E089F4F
                  - paragraph [ref=e240]: ui_replay_7e089f4f | Batch File Drop
                  - paragraph [ref=e241]: No support owner assigned yet
                - button "UI Replay 909DCFAD ui_replay_909dcfad | Batch File Drop No support owner assigned yet" [ref=e242] [cursor=pointer]:
                  - strong [ref=e243]: UI Replay 909DCFAD
                  - paragraph [ref=e244]: ui_replay_909dcfad | Batch File Drop
                  - paragraph [ref=e245]: No support owner assigned yet
                - button "UI Replay 9D8A3F3A ui_replay_9d8a3f3a | Batch File Drop No support owner assigned yet" [ref=e246] [cursor=pointer]:
                  - strong [ref=e247]: UI Replay 9D8A3F3A
                  - paragraph [ref=e248]: ui_replay_9d8a3f3a | Batch File Drop
                  - paragraph [ref=e249]: No support owner assigned yet
                - button "UI Replay A875E8F4 ui_replay_a875e8f4 | Batch File Drop No support owner assigned yet" [ref=e250] [cursor=pointer]:
                  - strong [ref=e251]: UI Replay A875E8F4
                  - paragraph [ref=e252]: ui_replay_a875e8f4 | Batch File Drop
                  - paragraph [ref=e253]: No support owner assigned yet
                - button "UI Replay B9B2F14B ui_replay_b9b2f14b | Batch File Drop No support owner assigned yet" [ref=e254] [cursor=pointer]:
                  - strong [ref=e255]: UI Replay B9B2F14B
                  - paragraph [ref=e256]: ui_replay_b9b2f14b | Batch File Drop
                  - paragraph [ref=e257]: No support owner assigned yet
                - button "UI Replay CA7B495F ui_replay_ca7b495f | Batch File Drop No support owner assigned yet" [ref=e258] [cursor=pointer]:
                  - strong [ref=e259]: UI Replay CA7B495F
                  - paragraph [ref=e260]: ui_replay_ca7b495f | Batch File Drop
                  - paragraph [ref=e261]: No support owner assigned yet
                - button "UI Replay DEDFCE3C ui_replay_dedfce3c | Batch File Drop No support owner assigned yet" [ref=e262] [cursor=pointer]:
                  - strong [ref=e263]: UI Replay DEDFCE3C
                  - paragraph [ref=e264]: ui_replay_dedfce3c | Batch File Drop
                  - paragraph [ref=e265]: No support owner assigned yet
                - button "UI Replay F6F698D0 ui_replay_f6f698d0 | Batch File Drop No support owner assigned yet" [ref=e266] [cursor=pointer]:
                  - strong [ref=e267]: UI Replay F6F698D0
                  - paragraph [ref=e268]: ui_replay_f6f698d0 | Batch File Drop
                  - paragraph [ref=e269]: No support owner assigned yet
                - button "UI Replay F9511556 ui_replay_f9511556 | Batch File Drop No support owner assigned yet" [ref=e270] [cursor=pointer]:
                  - strong [ref=e271]: UI Replay F9511556
                  - paragraph [ref=e272]: ui_replay_f9511556 | Batch File Drop
                  - paragraph [ref=e273]: No support owner assigned yet
                - button "UI Replay FA2F5384 ui_replay_fa2f5384 | Batch File Drop No support owner assigned yet" [ref=e274] [cursor=pointer]:
                  - strong [ref=e275]: UI Replay FA2F5384
                  - paragraph [ref=e276]: ui_replay_fa2f5384 | Batch File Drop
                  - paragraph [ref=e277]: No support owner assigned yet
              - generic [ref=e278]:
                - generic [ref=e279]:
                  - generic [ref=e280]: Sync Mode
                  - combobox "Sync Mode" [ref=e281]:
                    - option "Batch File Drop" [selected]
                - generic [ref=e282]:
                  - generic [ref=e283]: Validation
                  - combobox "Validation" [ref=e284]:
                    - option "Standard"
                    - option "Strict"
                    - option "Relaxed" [selected]
                - generic [ref=e285]:
                  - generic [ref=e286]: Transform
                  - combobox "Transform" [ref=e287]:
                    - option "None"
                    - option "Normalize Codes" [selected]
              - generic [ref=e288]:
                - generic [ref=e289]:
                  - generic [ref=e290]: Connector cadence
                  - textbox "Connector cadence" [disabled] [ref=e291]: File-drop batch
                - generic [ref=e292]:
                  - generic [ref=e293]: Support Owner
                  - combobox "Support Owner" [ref=e294]:
                    - option "Unassigned" [selected]
                    - option "Executive Operations Director"
                    - option "Integration Lead"
                    - option "Operations Lead"
                    - option "Operations Planner"
              - paragraph [ref=e295]: CSV order import is implemented as batch file-drop intake only. Scheduled pull and realtime push are not supported for CSV connectors.
              - button "Save Connector Policy" [ref=e297] [cursor=pointer]
      - complementary [ref=e298]:
        - article [ref=e299]:
          - generic [ref=e300]:
            - paragraph [ref=e301]: Realtime state
            - generic [ref=e302]: Reconnecting
          - strong [ref=e303]: Monitoring live operating state
          - paragraph [ref=e304]: Snapshot 2026/04/30, 00:01:04
          - generic [ref=e305]:
            - generic [ref=e306]:
              - generic [ref=e307]: Alerts
              - strong [ref=e308]: "4"
            - generic [ref=e309]:
              - generic [ref=e310]: Actions
              - strong [ref=e311]: "12"
            - generic [ref=e312]:
              - generic [ref=e313]: Replay
              - strong [ref=e314]: "0"
            - generic [ref=e315]:
              - generic [ref=e316]: Incidents
              - strong [ref=e317]: "8"
        - article [ref=e318]:
          - generic [ref=e319]:
            - paragraph [ref=e320]: Workspace focus
            - generic [ref=e321]: Editable
          - strong [ref=e322]: PILOT-TENANT Hosted Verification
          - paragraph [ref=e323]: 2 warehouse lanes | 16 connectors | Rotation 90 days
          - generic [ref=e324]:
            - generic [ref=e325]:
              - generic [ref=e326]: Warehouses
              - strong [ref=e327]: "2"
            - generic [ref=e328]:
              - generic [ref=e329]: Connectors
              - strong [ref=e330]: "16"
            - generic [ref=e331]:
              - generic [ref=e332]: Rotation
              - strong [ref=e333]: "90"
            - generic [ref=e334]:
              - generic [ref=e335]: Timeout
              - strong [ref=e336]: "480"
          - button "Quick route Open users Shift from policy to who can act inside it." [ref=e338] [cursor=pointer]:
            - generic [ref=e339]: Quick route
            - strong [ref=e340]: Open users
            - paragraph [ref=e341]: Shift from policy to who can act inside it.
        - article [ref=e342]:
          - generic [ref=e343]:
            - paragraph [ref=e344]: Act now
            - generic [ref=e345]: 5 items
          - generic [ref=e346]:
            - button "Critical Depletion risk rising for SKU SKU-PILOT-TENANT-PROOF in WH-NORTH Demand is accelerating for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.4 hours at the current demand rate. Review threshold settings and stage replenishment now." [ref=e347] [cursor=pointer]:
              - generic [ref=e348]: Critical
              - strong [ref=e349]: Depletion risk rising for SKU SKU-PILOT-TENANT-PROOF in WH-NORTH
              - paragraph [ref=e350]: Demand is accelerating for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.4 hours at the current demand rate. Review threshold settings and stage replenishment now.
            - button "Critical Delivery delay risk rising in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 25, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e351] [cursor=pointer]:
              - generic [ref=e352]: Critical
              - strong [ref=e353]: Delivery delay risk rising in WH-NORTH
              - paragraph [ref=e354]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 25, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Fulfillment backlog building in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 25, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e355] [cursor=pointer]:
              - generic [ref=e356]: Critical
              - strong [ref=e357]: Fulfillment backlog building in WH-NORTH
              - paragraph [ref=e358]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 25, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Urgent reorder for SKU SKU-RT-52C8463F at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e359] [cursor=pointer]:
              - generic [ref=e360]: Critical
              - strong [ref=e361]: Urgent reorder for SKU SKU-RT-52C8463F at WH-NORTH
              - paragraph [ref=e362]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
            - button "Critical Urgent reorder for SKU SKU-RT-09113A78 at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e363] [cursor=pointer]:
              - generic [ref=e364]: Critical
              - strong [ref=e365]: Urgent reorder for SKU SKU-RT-09113A78 at WH-NORTH
              - paragraph [ref=e366]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
        - article [ref=e367]:
          - generic [ref=e368]:
            - paragraph [ref=e369]: Activity stream
            - generic [ref=e370]: "6"
          - generic [ref=e371]:
            - generic [ref=e372]:
              - strong [ref=e373]: Request Rejected
              - paragraph [ref=e374]: High incident
              - generic [ref=e375]: 2026/04/30, 00:00:09
            - generic [ref=e376]:
              - strong [ref=e377]: Request Rejected
              - paragraph [ref=e378]: High incident
              - generic [ref=e379]: 2026/04/29, 23:59:29
            - generic [ref=e380]:
              - strong [ref=e381]: Scenario Executed
              - paragraph [ref=e382]: Executed scenario 19 into live order ORD-119052F9 for warehouse WH-NORTH.
              - generic [ref=e383]: 2026/04/29, 23:59:26
            - generic [ref=e384]:
              - strong [ref=e385]: Order Ingested
              - paragraph [ref=e386]: Order ORD-119052F9 received for warehouse WH-NORTH with 1 line items.
              - generic [ref=e387]: 2026/04/29, 23:59:26
            - generic [ref=e388]:
              - strong [ref=e389]: Request Rejected
              - paragraph [ref=e390]: ApiRequest | /api/dashboard/snapshot
              - generic [ref=e391]: 2026/04/30, 00:00:09
            - generic [ref=e392]:
              - strong [ref=e393]: Request Rejected
              - paragraph [ref=e394]: ApiRequest | /api/dashboard/snapshot
              - generic [ref=e395]: 2026/04/29, 23:59:29
        - article [ref=e396]:
          - generic [ref=e397]:
            - paragraph [ref=e398]: Operator
            - generic [ref=e399]: Healthy
          - strong [ref=e400]: Hosted Verification Tenant Admin
          - paragraph [ref=e401]: PILOT-TENANT Hosted Verification | Operations Lead
          - paragraph [ref=e402]: Roles Escalation Owner, Integration Admin, Integration Operator, Review Owner, Tenant Admin
          - paragraph [ref=e403]: Warehouse scope Tenant-wide
```

# Test source

```ts
  823 |       hasText: 'Scenario action console',
  824 |       has: page.getByText(scenarioFixture.title),
  825 |     }).first()
  826 |     await expect(scenarioActionConsole).toBeVisible()
  827 |     await scenarioActionConsole.getByRole('button', { name: 'Approve Plan' }).click()
  828 |     await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()
  829 | 
  830 |     await expect(scenarioActionConsole.getByRole('button', { name: 'Execute Scenario' })).toBeVisible()
  831 |     await scenarioActionConsole.getByRole('button', { name: 'Execute Scenario' }).click()
  832 |     await expect(page.locator('.success-text').filter({ hasText: new RegExp(`^Executed ${scenarioFixture.title} as live order `, 'i') }).first()).toBeVisible()
  833 |   } finally {
  834 |     await scenarioFixture.api.dispose()
  835 |   }
  836 | 
  837 |   await signOutViaUi(page)
  838 | 
  839 |   await loginViaUi(page, users.operationsPlanner)
  840 |   await navigateWithinApp(page, '/users')
  841 |   await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
  842 |   await expect(page.getByText('Tenant admin access required')).toBeVisible()
  843 |   await expect(page.getByText('Operators', { exact: true }).first()).toBeVisible()
  844 | })
  845 | 
  846 | test('alerts, recommendations, orders, inventory, integrations, users, profile, and settings surfaces stay connected to the live backend', async ({ page }) => {
  847 |   const api = await createApiContext(users.operationsLead)
  848 |   let restoreAlertCoverage = async () => {}
  849 | 
  850 |   try {
  851 |     const alertCoverage = await ensureAlertAndRecommendationCoverage(api)
  852 |     restoreAlertCoverage = alertCoverage.restore
  853 |     const recentOrders = await ensureRecentOrder(api)
  854 |     const workspace = await readJson(await api.get('/api/access/admin/workspace'))
  855 |     const operators = await readJson(await api.get('/api/access/admin/operators'))
  856 |     const accessUsers = await readJson(await api.get('/api/access/admin/users'))
  857 |     const alertRecord = alertCoverage.alertRecord
  858 |     const recommendationRecord = alertCoverage.recommendationRecord
  859 |     const orderRecord = recentOrders[0]
  860 |     const inventoryRecord = alertCoverage.snapshot.inventory.find((item) => item.lowStock) || alertCoverage.snapshot.inventory[0]
  861 |     const connectorCandidates = [
  862 |       ...alertCoverage.snapshot.integrationConnectors,
  863 |       ...(workspace.connectors || []),
  864 |     ]
  865 | 
  866 |     expect(alertRecord).toBeTruthy()
  867 |     expect(recommendationRecord).toBeTruthy()
  868 |     expect(orderRecord).toBeTruthy()
  869 |     expect(inventoryRecord).toBeTruthy()
  870 |     expect(workspace).toBeTruthy()
  871 |     expect(operators.length).toBeGreaterThan(0)
  872 |     expect(accessUsers.length).toBeGreaterThan(0)
  873 |     expect(connectorCandidates.length).toBeGreaterThan(0)
  874 | 
  875 |     await loginViaUi(page, users.operationsLead)
  876 | 
  877 |     await navigateWithinApp(page, '/alerts')
  878 |     await expect(page.getByRole('heading', { level: 1, name: 'Operational warning center' })).toBeVisible()
  879 |     await expect(page.getByText(alertRecord.title).first()).toBeVisible()
  880 |     await activateSelectableButton(
  881 |       page.getByRole('button', { name: new RegExp(escapeRegExp(alertRecord.title), 'i') }).first(),
  882 |     )
  883 |     await expect(page.getByText(`Action: ${alertRecord.recommendedAction}`).first()).toBeVisible()
  884 | 
  885 |     await navigateWithinApp(page, '/recommendations')
  886 |     await expect(page.getByRole('heading', { level: 1, name: 'Action queue for the operating team' })).toBeVisible()
  887 |     await expect(page.getByText(recommendationRecord.title).first()).toBeVisible()
  888 |     await activateSelectableButton(
  889 |       page.locator('.recommendation-board').getByRole('button', { name: new RegExp(escapeRegExp(recommendationRecord.title), 'i') }).first(),
  890 |     )
  891 |     await expect(page.getByText(recommendationRecord.description).first()).toBeVisible()
  892 | 
  893 |     await navigateWithinApp(page, '/orders')
  894 |     await expect(page.getByRole('heading', { level: 1, name: 'Live order operations' })).toBeVisible()
  895 |     await expect(page.getByText(orderRecord.externalOrderId).first()).toBeVisible()
  896 |     await activateSelectableButton(
  897 |       page.getByRole('button', { name: new RegExp(escapeRegExp(orderRecord.externalOrderId), 'i') }).first(),
  898 |     )
  899 |     await expect(page.getByText(orderRecord.warehouseCode).first()).toBeVisible()
  900 | 
  901 |     await navigateWithinApp(page, '/inventory')
  902 |     await expect(page.getByRole('heading', { level: 1, name: 'Inventory intelligence' })).toBeVisible()
  903 |     await expect(page.getByText(inventoryRecord.productName).first()).toBeVisible()
  904 |     await activateSelectableButton(
  905 |       page.getByRole('button', { name: new RegExp(escapeRegExp(inventoryRecord.productName), 'i') }).first(),
  906 |     )
  907 |     await expect(page.getByText(inventoryRecord.productSku).first()).toBeVisible()
  908 | 
  909 |     await navigateWithinApp(page, '/integrations')
  910 |     await expect(page.getByRole('heading', { level: 1, name: 'Connector management and telemetry' })).toBeVisible()
  911 |     await expect(page.locator('button.system-select-card').first()).toBeVisible()
  912 |     const visibleConnectorMatch = await findVisibleIntegrationConnector(page, connectorCandidates)
  913 |     if (!visibleConnectorMatch) {
  914 |       throw new Error('Expected at least one integration connector rendered in the UI to match backend connector data.')
  915 |     }
  916 |     await activateSelectableButton(
  917 |       visibleConnectorMatch.button,
  918 |     )
  919 |     await expect(page.getByText(visibleConnectorMatch.connector.sourceSystem).first()).toBeVisible()
  920 |     await page.getByRole('button', { name: 'Manage Policies' }).click()
  921 | 
  922 |     await expect(page.getByRole('heading', { level: 1, name: 'Tenant and workspace settings' })).toBeVisible()
> 923 |     await expect(page.getByDisplayValue(workspace.tenantName)).toBeVisible()
      |                       ^ TypeError: page.getByDisplayValue is not a function
  924 |     if (workspace.connectors?.length) {
  925 |       await expect(page.getByText(workspace.connectors[0].displayName).first()).toBeVisible()
  926 |     }
  927 | 
  928 |     await navigateWithinApp(page, '/users')
  929 |     await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
  930 |     await expect(page.getByText(operators[0].displayName).first()).toBeVisible()
  931 |     await expect(page.getByText(accessUsers[0].fullName).first()).toBeVisible()
  932 | 
  933 |     await navigateWithinApp(page, '/profile')
  934 |     await expect(page.getByRole('heading', { level: 1, name: 'Personal profile and session controls' })).toBeVisible()
  935 |     await expect(page.getByText(users.operationsLead.username).first()).toBeVisible()
  936 |     await expect(page.getByText(workspace.tenantName).first()).toBeVisible()
  937 | 
  938 |     await expectNoFatalUiErrors(page)
  939 |   } finally {
  940 |     await restoreAlertCoverage()
  941 |     await api.dispose()
  942 |   }
  943 | })
  944 | 
  945 | test('frontend surfaces backend auth rate limiting without getting stuck in a loading state', async ({ page }) => {
  946 |   await page.goto('/sign-in')
  947 |   await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  948 |   const signInCard = page.locator('.public-signin-card')
  949 |   await waitForSignInReady(signInCard)
  950 | 
  951 |   const hitRateLimit = await page.evaluate(async ({ nextBackendUrl, tenantCode, username }) => {
  952 |     const body = JSON.stringify({
  953 |       tenantCode,
  954 |       username,
  955 |       password: 'wrong-rate-limit',
  956 |     })
  957 | 
  958 |     for (let attempt = 0; attempt < 35; attempt += 1) {
  959 |       const response = await fetch(`${nextBackendUrl}/api/auth/session/login`, {
  960 |         method: 'POST',
  961 |         headers: { 'Content-Type': 'application/json' },
  962 |         body,
  963 |       })
  964 |       if (response.status === 429) {
  965 |         return true
  966 |       }
  967 |     }
  968 | 
  969 |     return false
  970 |   }, {
  971 |     nextBackendUrl: backendUrl,
  972 |     tenantCode: users.operationsLead.tenantCode,
  973 |     username: users.operationsLead.username,
  974 |   })
  975 | 
  976 |   expect(hitRateLimit).toBeTruthy()
  977 | 
  978 |   await fillSignInForm(signInCard, users.operationsLead, 'wrong-rate-limit')
  979 |   await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  980 |   await expect(signInCard.getByRole('button', { name: 'Enter Platform' })).toBeEnabled({ timeout: 60_000 })
  981 |   await expect(signInCard.getByText('Authentication rate limit exceeded. Wait before attempting another sign-in.')).toBeVisible({ timeout: 15_000 })
  982 | })
  983 | 
```