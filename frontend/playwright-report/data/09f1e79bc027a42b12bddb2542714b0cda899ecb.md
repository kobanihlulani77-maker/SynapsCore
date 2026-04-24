# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> @realtime dashboard summary updates live without a browser refresh
- Location: tests\prod-proof.spec.mjs:371:1

# Error details

```
Error: Expected the dashboard low-stock summary to change through the live websocket path.

expect(received).toBeGreaterThan(expected)

Expected: > 0
Received:   0

Call Log:
- Timeout 30000ms exceeded while waiting on the predicate
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
          - button "Dashboard 3" [ref=e18] [cursor=pointer]:
            - generic [ref=e19]: Dashboard
            - strong [ref=e20]: "3"
          - button "Alerts 1" [ref=e21] [cursor=pointer]:
            - generic [ref=e22]: Alerts
            - strong [ref=e23]: "1"
          - button "Recommendations 5" [ref=e24] [cursor=pointer]:
            - generic [ref=e25]: Recommendations
            - strong [ref=e26]: "5"
      - generic [ref=e27]:
        - paragraph [ref=e28]: Operations
        - generic [ref=e29]:
          - button "Orders 3" [ref=e30] [cursor=pointer]:
            - generic [ref=e31]: Orders
            - strong [ref=e32]: "3"
          - button "Inventory 1" [ref=e33] [cursor=pointer]:
            - generic [ref=e34]: Inventory
            - strong [ref=e35]: "1"
          - button "Catalog 13" [ref=e36] [cursor=pointer]:
            - generic [ref=e37]: Catalog
            - strong [ref=e38]: "13"
          - button "Locations 2" [ref=e39] [cursor=pointer]:
            - generic [ref=e40]: Locations
            - strong [ref=e41]: "2"
          - button "Fulfillment 3" [ref=e42] [cursor=pointer]:
            - generic [ref=e43]: Fulfillment
            - strong [ref=e44]: "3"
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
          - button "Escalations 6" [ref=e57] [cursor=pointer]:
            - generic [ref=e58]: Escalations
            - strong [ref=e59]: "6"
      - generic [ref=e60]:
        - paragraph [ref=e61]: Systems
        - generic [ref=e62]:
          - button "Integrations 2" [ref=e63] [cursor=pointer]:
            - generic [ref=e64]: Integrations
            - strong [ref=e65]: "2"
          - button "Replay Queue 0" [ref=e66] [cursor=pointer]:
            - generic [ref=e67]: Replay Queue
            - strong [ref=e68]: "0"
          - button "Runtime 6" [ref=e69] [cursor=pointer]:
            - generic [ref=e70]: Runtime
            - strong [ref=e71]: "6"
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
          - button "Platform Admin 6" [ref=e87] [cursor=pointer]:
            - generic [ref=e88]: Platform Admin
            - strong [ref=e89]: "6"
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
        - paragraph [ref=e107]: CORE
        - heading "Live operational command center" [level=1] [ref=e108]
        - paragraph [ref=e109]: See what is happening now, what is at risk, and what the business needs to act on next.
      - generic [ref=e110]:
        - generic [ref=e112]:
          - generic [ref=e113]: Global search
          - textbox "Global search" [ref=e114]:
            - /placeholder: Search pages, orders, or alerts
        - generic "Page focus" [ref=e115]:
          - button "Act now" [ref=e116] [cursor=pointer]
          - button "Live state" [ref=e117] [cursor=pointer]
          - button "Trust layer" [ref=e118] [cursor=pointer]
        - generic [ref=e119]:
          - generic [ref=e120]: 2026/04/24, 13:50:00
          - generic [ref=e121]: Reconnecting
          - button "Notifications 7" [ref=e122] [cursor=pointer]
        - generic [ref=e123]:
          - button "Open alerts" [ref=e124] [cursor=pointer]
          - button "Open approvals" [ref=e125] [cursor=pointer]
          - button "Refresh" [ref=e126] [cursor=pointer]
          - button "Hosted Verification Tenant Admin" [ref=e127] [cursor=pointer]
          - button "Sign Out" [ref=e128] [cursor=pointer]
    - generic [ref=e129]:
      - main [ref=e130]:
        - generic [ref=e131]:
          - generic [ref=e132]:
            - paragraph [ref=e133]: Premium operational command center
            - heading "Operate live business pressure with calm, visible control." [level=2] [ref=e134]
            - paragraph [ref=e135]: One workspace for orders, stock, fulfillment, connectors, approvals, incidents, and access control. SynapseCore keeps the operating picture live, ranked, and actionable.
            - generic [ref=e136]:
              - article [ref=e137]:
                - generic [ref=e138]: Workspace Session
                - strong [ref=e139]: PILOT-TENANT Hosted Verification
                - paragraph [ref=e140]: Hosted Verification Tenant Admin is operating as Operations Lead.
              - article [ref=e141]:
                - generic [ref=e142]: Decision Pressure
                - strong [ref=e143]: 6 open signals
                - paragraph [ref=e144]: Alerts and recommendations are live across the tenant decision layer.
              - article [ref=e145]:
                - generic [ref=e146]: Operational Throughput
                - strong [ref=e147]: 3 recent orders
                - paragraph [ref=e148]: 3 backlog items and 0 delayed shipments are active.
          - article [ref=e150]:
            - paragraph [ref=e151]: Workspace pulse
            - heading "Attention needed" [level=3] [ref=e152]
            - paragraph [ref=e153]: Updated 2026/04/24, 13:49:22
            - generic [ref=e154]:
              - generic [ref=e155]:
                - generic [ref=e156]: Pages
                - strong [ref=e157]: "23"
              - generic [ref=e158]:
                - generic [ref=e159]: Connectors
                - strong [ref=e160]: 2/2
              - generic [ref=e161]:
                - generic [ref=e162]: Replay
                - strong [ref=e163]: "0"
              - generic [ref=e164]:
                - generic [ref=e165]: Approvals
                - strong [ref=e166]: "0"
        - generic [ref=e167]:
          - article [ref=e168]:
            - generic [ref=e169]: Orders
            - strong [ref=e170]: "3"
            - paragraph [ref=e171]: Live order activity in the current window
          - article [ref=e172]:
            - generic [ref=e173]: Risk
            - strong [ref=e174]: "0"
            - paragraph [ref=e175]: Inventory lanes under active pressure
          - article [ref=e176]:
            - generic [ref=e177]: Alerts
            - strong [ref=e178]: "1"
            - paragraph [ref=e179]: Warnings requiring ownership
          - article [ref=e180]:
            - generic [ref=e181]: Uptime posture
            - strong [ref=e182]: Accepting Traffic
            - paragraph [ref=e183]: System acceptance and readiness state
        - generic [ref=e184]:
          - article [ref=e185]:
            - generic [ref=e186]:
              - generic [ref=e187]:
                - paragraph [ref=e188]: Act now
                - heading "Urgent operational actions" [level=2] [ref=e189]
              - generic [ref=e190]: "4"
            - generic [ref=e191]:
              - button "Fulfillment backlog building in WH-NORTH High Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 3 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now." [ref=e192] [cursor=pointer]:
                - generic [ref=e193]:
                  - strong [ref=e194]: Fulfillment backlog building in WH-NORTH
                  - generic [ref=e195]: High
                - paragraph [ref=e196]: Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 3 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now.
              - button "Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH Critical Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 2.2 hours at the current demand rate." [ref=e197] [cursor=pointer]:
                - generic [ref=e198]:
                  - strong [ref=e199]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                  - generic [ref=e200]: Critical
                - paragraph [ref=e201]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 2.2 hours at the current demand rate.
              - button "Reorder stock for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH Medium Plan replenishment for PILOT-TENANT Hosted Verification North Hub. Current quantity is 8 units versus a threshold of 12." [ref=e202] [cursor=pointer]:
                - generic [ref=e203]:
                  - strong [ref=e204]: Reorder stock for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                  - generic [ref=e205]: Medium
                - paragraph [ref=e206]: Plan replenishment for PILOT-TENANT Hosted Verification North Hub. Current quantity is 8 units versus a threshold of 12.
              - button "Prioritize fulfillment backlog for WH-NORTH Medium Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 3 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now." [ref=e207] [cursor=pointer]:
                - generic [ref=e208]:
                  - strong [ref=e209]: Prioritize fulfillment backlog for WH-NORTH
                  - generic [ref=e210]: Medium
                - paragraph [ref=e211]: Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 3 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now.
          - article [ref=e212]:
            - generic [ref=e213]:
              - generic [ref=e214]:
                - paragraph [ref=e215]: Risk heat
                - heading "Pressure by location" [level=2] [ref=e216]
              - generic [ref=e217]: "1"
            - generic [ref=e219]:
              - generic [ref=e220]:
                - strong [ref=e221]: PILOT-TENANT Hosted Verification North Hub
                - generic [ref=e222]: Watch
              - paragraph [ref=e223]: WH-NORTH
              - paragraph [ref=e224]: 0 alerts | 0 low stock | 3 backlog | 0 delayed
        - article [ref=e226]:
          - generic [ref=e227]:
            - generic [ref=e228]:
              - paragraph [ref=e229]: Decision lanes
              - heading "Alerts and recommendations that need ownership" [level=2] [ref=e230]
            - generic [ref=e231]: "5"
          - generic [ref=e232]:
            - article [ref=e233]:
              - generic [ref=e234]:
                - strong [ref=e235]: Urgent alerts
                - button "Open Alerts" [ref=e236] [cursor=pointer]
              - button [ref=e238] [cursor=pointer]:
                - article [ref=e239]:
                  - generic [ref=e240]:
                    - strong [ref=e241]: Fulfillment backlog building in WH-NORTH
                    - generic [ref=e242]: HIGH
                  - paragraph [ref=e243]: Fulfillment backlog is building in WH-NORTH with 3 open warehouse tasks and roughly 6.0 hours to clear at the current pace.
                  - paragraph [ref=e244]: Tenant-wide | 2026/04/24, 13:14:07
            - article [ref=e245]:
              - generic [ref=e246]:
                - strong [ref=e247]: Recommendation queue
                - button "Open Recommendations" [ref=e248] [cursor=pointer]
              - generic [ref=e249]:
                - button "Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH CRITICAL Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 2.2 hours at the current demand rate. Tenant-wide | 2026/04/24, 13:46:13" [ref=e250] [cursor=pointer]:
                  - generic [ref=e251]:
                    - strong [ref=e252]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                    - generic [ref=e253]: CRITICAL
                  - paragraph [ref=e254]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 2.2 hours at the current demand rate.
                  - paragraph [ref=e255]: Tenant-wide | 2026/04/24, 13:46:13
                - button "Reorder stock for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH MEDIUM Plan replenishment for PILOT-TENANT Hosted Verification North Hub. Current quantity is 8 units versus a threshold of 12. Tenant-wide | 2026/04/24, 12:45:29" [ref=e256] [cursor=pointer]:
                  - generic [ref=e257]:
                    - strong [ref=e258]: Reorder stock for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                    - generic [ref=e259]: MEDIUM
                  - paragraph [ref=e260]: Plan replenishment for PILOT-TENANT Hosted Verification North Hub. Current quantity is 8 units versus a threshold of 12.
                  - paragraph [ref=e261]: Tenant-wide | 2026/04/24, 12:45:29
                - button "Prioritize fulfillment backlog for WH-NORTH MEDIUM Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 3 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now. Tenant-wide | 2026/04/24, 13:21:10" [ref=e262] [cursor=pointer]:
                  - generic [ref=e263]:
                    - strong [ref=e264]: Prioritize fulfillment backlog for WH-NORTH
                    - generic [ref=e265]: MEDIUM
                  - paragraph [ref=e266]: Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 3 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now.
                  - paragraph [ref=e267]: Tenant-wide | 2026/04/24, 13:21:10
                - button "Prioritize fulfillment backlog for WH-NORTH MEDIUM Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 2 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now. Tenant-wide | 2026/04/24, 13:14:36" [ref=e268] [cursor=pointer]:
                  - generic [ref=e269]:
                    - strong [ref=e270]: Prioritize fulfillment backlog for WH-NORTH
                    - generic [ref=e271]: MEDIUM
                  - paragraph [ref=e272]: Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 2 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now.
                  - paragraph [ref=e273]: Tenant-wide | 2026/04/24, 13:14:36
        - article [ref=e275]:
          - generic [ref=e276]:
            - generic [ref=e277]:
              - paragraph [ref=e278]: What changed recently
              - heading "Activity, approvals, and system health overview" [level=2] [ref=e279]
            - generic [ref=e280]: "12"
          - generic [ref=e281]:
            - article [ref=e282]:
              - generic [ref=e283]:
                - generic [ref=e284]:
                  - paragraph [ref=e285]: Live activity stream
                  - strong [ref=e286]: Recent activity
                - button "Open Audit" [ref=e287] [cursor=pointer]
              - generic [ref=e288]:
                - generic [ref=e289]:
                  - strong [ref=e290]: Request Rejected
                  - paragraph [ref=e291]: High incident
                  - paragraph [ref=e292]: 2026/04/24, 13:49:29
                - generic [ref=e293]:
                  - strong [ref=e294]: Request Rejected
                  - paragraph [ref=e295]: High incident
                  - paragraph [ref=e296]: 2026/04/24, 13:46:06
                - generic [ref=e297]:
                  - strong [ref=e298]: Product Catalog Updated
                  - paragraph [ref=e299]: "Imported product catalog rows: created 1, updated 1, failed 1."
                  - paragraph [ref=e300]: 2026/04/24, 13:49:16
                - generic [ref=e301]:
                  - strong [ref=e302]: Product Catalog Updated
                  - paragraph [ref=e303]: Updated catalog product SKU-UI-CBFE0691 (UI Catalog CBFE0691 Updated).
                  - paragraph [ref=e304]: 2026/04/24, 13:49:11
            - article [ref=e305]:
              - generic [ref=e306]:
                - strong [ref=e307]: Approval attention
                - button "Open Approvals" [ref=e308] [cursor=pointer]
              - generic [ref=e310]: No plans are waiting on review right now. Decision routing will appear here when operators start sending scenarios through governance.
            - article [ref=e311]:
              - generic [ref=e312]:
                - strong [ref=e313]: System health overview
                - button "Open Runtime" [ref=e314] [cursor=pointer]
              - generic [ref=e315]:
                - generic [ref=e316]:
                  - generic [ref=e317]:
                    - strong [ref=e318]: Runtime trust
                    - generic [ref=e319]: Up
                  - paragraph [ref=e320]: Accepting Traffic readiness with 0 pending dispatch items.
                  - paragraph [ref=e321]: Build 0.0.1 | Commit render-
                - generic [ref=e322]:
                  - generic [ref=e323]:
                    - strong [ref=e324]: Incident pressure
                    - generic [ref=e325]: 6 active
                  - paragraph [ref=e326]: 4 high-severity incidents and 0 replay items are currently affecting trust.
                  - paragraph [ref=e327]: 2/2 connectors enabled across the tenant integration surface.
      - complementary [ref=e328]:
        - article [ref=e329]:
          - generic [ref=e330]:
            - paragraph [ref=e331]: Realtime state
            - generic [ref=e332]: Reconnecting
          - strong [ref=e333]: Monitoring live operating state
          - paragraph [ref=e334]: Snapshot 2026/04/24, 13:49:34
          - generic [ref=e335]:
            - generic [ref=e336]:
              - generic [ref=e337]: Alerts
              - strong [ref=e338]: "1"
            - generic [ref=e339]:
              - generic [ref=e340]: Actions
              - strong [ref=e341]: "5"
            - generic [ref=e342]:
              - generic [ref=e343]: Replay
              - strong [ref=e344]: "0"
            - generic [ref=e345]:
              - generic [ref=e346]: Incidents
              - strong [ref=e347]: "6"
        - article [ref=e348]:
          - generic [ref=e349]:
            - paragraph [ref=e350]: Page focus
            - generic [ref=e351]: Dashboard
          - strong [ref=e352]: Live operational command center
          - paragraph [ref=e353]: Updated 2026/04/24, 13:49:22
          - generic [ref=e354]:
            - generic [ref=e355]:
              - generic [ref=e356]: Focus 1
              - strong [ref=e357]: Act now
            - generic [ref=e358]:
              - generic [ref=e359]: Focus 2
              - strong [ref=e360]: Live state
            - generic [ref=e361]:
              - generic [ref=e362]: Focus 3
              - strong [ref=e363]: Trust layer
            - generic [ref=e364]:
              - generic [ref=e365]: Group
              - strong [ref=e366]: Core
        - article [ref=e367]:
          - generic [ref=e368]:
            - paragraph [ref=e369]: Act now
            - generic [ref=e370]: 4 items
          - generic [ref=e371]:
            - button "High Fulfillment backlog building in WH-NORTH Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 3 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now." [ref=e372] [cursor=pointer]:
              - generic [ref=e373]: High
              - strong [ref=e374]: Fulfillment backlog building in WH-NORTH
              - paragraph [ref=e375]: Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 3 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now.
            - button "Critical Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 2.2 hours at the current demand rate." [ref=e376] [cursor=pointer]:
              - generic [ref=e377]: Critical
              - strong [ref=e378]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
              - paragraph [ref=e379]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 2.2 hours at the current demand rate.
            - button "Medium Reorder stock for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH Plan replenishment for PILOT-TENANT Hosted Verification North Hub. Current quantity is 8 units versus a threshold of 12." [ref=e380] [cursor=pointer]:
              - generic [ref=e381]: Medium
              - strong [ref=e382]: Reorder stock for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
              - paragraph [ref=e383]: Plan replenishment for PILOT-TENANT Hosted Verification North Hub. Current quantity is 8 units versus a threshold of 12.
            - button "Medium Prioritize fulfillment backlog for WH-NORTH Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 3 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now." [ref=e384] [cursor=pointer]:
              - generic [ref=e385]: Medium
              - strong [ref=e386]: Prioritize fulfillment backlog for WH-NORTH
              - paragraph [ref=e387]: Dispatch backlog is building in PILOT-TENANT Hosted Verification North Hub with 3 open fulfillment tasks and roughly 6.0 hours to clear at the current pace. Pull warehouse labor forward or rebalance the lane now.
        - article [ref=e388]:
          - generic [ref=e389]:
            - paragraph [ref=e390]: Activity stream
            - generic [ref=e391]: "6"
          - generic [ref=e392]:
            - generic [ref=e393]:
              - strong [ref=e394]: Request Rejected
              - paragraph [ref=e395]: High incident
              - generic [ref=e396]: 2026/04/24, 13:49:29
            - generic [ref=e397]:
              - strong [ref=e398]: Request Rejected
              - paragraph [ref=e399]: High incident
              - generic [ref=e400]: 2026/04/24, 13:46:06
            - generic [ref=e401]:
              - strong [ref=e402]: Product Catalog Updated
              - paragraph [ref=e403]: "Imported product catalog rows: created 1, updated 1, failed 1."
              - generic [ref=e404]: 2026/04/24, 13:49:16
            - generic [ref=e405]:
              - strong [ref=e406]: Product Catalog Updated
              - paragraph [ref=e407]: Updated catalog product SKU-UI-CBFE0691 (UI Catalog CBFE0691 Updated).
              - generic [ref=e408]: 2026/04/24, 13:49:11
            - generic [ref=e409]:
              - strong [ref=e410]: Request Rejected
              - paragraph [ref=e411]: ApiRequest | /api/dashboard/snapshot
              - generic [ref=e412]: 2026/04/24, 13:49:29
            - generic [ref=e413]:
              - strong [ref=e414]: Product Import
              - paragraph [ref=e415]: Product | products.csv
              - generic [ref=e416]: 2026/04/24, 13:49:16
        - article [ref=e417]:
          - generic [ref=e418]:
            - paragraph [ref=e419]: Operator
            - generic [ref=e420]: Healthy
          - strong [ref=e421]: Hosted Verification Tenant Admin
          - paragraph [ref=e422]: PILOT-TENANT Hosted Verification | Operations Lead
          - paragraph [ref=e423]: Roles Escalation Owner, Integration Admin, Integration Operator, Review Owner, Tenant Admin
          - paragraph [ref=e424]: Warehouse scope Tenant-wide
```

# Test source

```ts
  304 |   for (const [route, title] of appPages) {
  305 |     await navigateWithinApp(page, route)
  306 |     await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
  307 |     await expect(page.locator('.workspace-topbar')).toBeVisible()
  308 |     await expectNoFatalUiErrors(page)
  309 |   }
  310 | 
  311 |   await signOutViaUi(page)
  312 | })
  313 | 
  314 | test('product catalog onboarding works through tenant-scoped API and browser surface', async ({ page }) => {
  315 |   const api = await createApiContext(users.operationsLead)
  316 |   const suffix = randomUUID().slice(0, 8).toUpperCase()
  317 |   const primarySku = `SKU-UI-${suffix}`
  318 |   const importSku = `SKU-IMP-${suffix}`
  319 | 
  320 |   try {
  321 |     const createdProduct = await readJson(await api.post('/api/products', {
  322 |       data: {
  323 |         sku: primarySku,
  324 |         name: `UI Catalog ${suffix}`,
  325 |         category: 'Verification',
  326 |       },
  327 |     }))
  328 |     expect(createdProduct.sku).toBe(primarySku)
  329 |     expect(createdProduct.tenantCode).toBe(users.operationsLead.tenantCode)
  330 | 
  331 |     const updatedProduct = await readJson(await api.put(`/api/products/${createdProduct.id}`, {
  332 |       data: {
  333 |         sku: primarySku,
  334 |         name: `UI Catalog ${suffix} Updated`,
  335 |         category: 'Verification',
  336 |       },
  337 |     }))
  338 |     expect(updatedProduct.name).toContain('Updated')
  339 | 
  340 |     const importResult = await readJson(await api.post('/api/products/import', {
  341 |       multipart: {
  342 |         file: {
  343 |           name: 'products.csv',
  344 |           mimeType: 'text/csv',
  345 |           buffer: Buffer.from(
  346 |             `sku,name,category\n${importSku},Imported Product ${suffix},Verification\n${primarySku},Imported Update ${suffix},Verification\n${importSku},Duplicate Product ${suffix},Verification\n`,
  347 |             'utf8',
  348 |           ),
  349 |         },
  350 |       },
  351 |     }))
  352 |     expect(importResult.created).toBe(1)
  353 |     expect(importResult.updated).toBe(1)
  354 |     expect(importResult.failed).toBe(1)
  355 | 
  356 |     const products = await readJson(await api.get('/api/products'))
  357 |     expect(products.some((product) => product.sku === primarySku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  358 |     expect(products.some((product) => product.sku === importSku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  359 | 
  360 |     await loginViaUi(page, users.operationsLead)
  361 |     await navigateWithinApp(page, '/catalog')
  362 |     await expect(page.getByRole('heading', { level: 1, name: 'Tenant product catalog' })).toBeVisible()
  363 |     await expect(page.getByText(primarySku).first()).toBeVisible()
  364 |     await expect(page.getByText(importSku).first()).toBeVisible()
  365 |     await expectNoFatalUiErrors(page)
  366 |   } finally {
  367 |     await api.dispose()
  368 |   }
  369 | })
  370 | 
  371 | test('@realtime dashboard summary updates live without a browser refresh', async ({ page }) => {
  372 |   await loginViaUi(page, users.operationsLead)
  373 |   await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  374 |   await expect(page.getByText('Realtime state')).toBeVisible()
  375 | 
  376 |   const api = await createApiContext(users.operationsLead)
  377 |   let candidate = null
  378 |   let revertQuantity = null
  379 |   let revertThreshold = null
  380 | 
  381 |   try {
  382 |     const inventory = await readJson(await api.get('/api/inventory'))
  383 |     candidate = inventory.find((item) => item.productSku === proofProductSku && item.warehouseCode === 'WH-NORTH')
  384 |       || inventory.find((item) => Number.isFinite(item.quantityAvailable) && Number.isFinite(item.reorderThreshold))
  385 |     expect(candidate).toBeTruthy()
  386 | 
  387 |     revertQuantity = candidate.quantityAvailable
  388 |     revertThreshold = candidate.reorderThreshold
  389 | 
  390 |     const beforeRisk = await waitForNumericSummaryCard(page, 'Risk')
  391 |     const threshold = Number.isFinite(candidate.reorderThreshold) ? candidate.reorderThreshold : 5
  392 |     const forceLowQuantity = Math.max(0, threshold - 1)
  393 |     const safeQuantity = threshold + 5
  394 | 
  395 |     await readJson(await api.post('/api/inventory/update', {
  396 |       data: {
  397 |         productSku: candidate.productSku,
  398 |         warehouseCode: candidate.warehouseCode,
  399 |         quantityAvailable: candidate.lowStock ? safeQuantity : forceLowQuantity,
  400 |         reorderThreshold: threshold,
  401 |       },
  402 |     }))
  403 | 
> 404 |     await expect.poll(async () => summaryCardValue(page, 'Risk'), {
      |     ^ Error: Expected the dashboard low-stock summary to change through the live websocket path.
  405 |       timeout: 30_000,
  406 |       message: 'Expected the dashboard low-stock summary to change through the live websocket path.',
  407 |     })[candidate.lowStock ? 'toBeLessThan' : 'toBeGreaterThan'](beforeRisk)
  408 |   } finally {
  409 |     if (candidate && revertQuantity != null && revertThreshold != null) {
  410 |       await readJson(await api.post('/api/inventory/update', {
  411 |         data: {
  412 |           productSku: candidate.productSku,
  413 |           warehouseCode: candidate.warehouseCode,
  414 |           quantityAvailable: revertQuantity,
  415 |           reorderThreshold: revertThreshold,
  416 |         },
  417 |       }))
  418 |     }
  419 |     await api.dispose()
  420 |   }
  421 | })
  422 | 
  423 | test('replay recovery, scenario approval, execution, and browser role gating work through the UI', async ({ page }) => {
  424 |   const replayFixture = await createReplayFixture()
  425 | 
  426 |   try {
  427 |     await loginViaUi(page, users.integrationLead)
  428 |     await navigateWithinApp(page, '/replay-queue')
  429 |     await expect(page.getByRole('heading', { level: 1, name: 'Failed inbound recovery' })).toBeVisible()
  430 | 
  431 |     const replayPanel = page.locator('article.panel').filter({ hasText: replayFixture.externalOrderId }).first()
  432 |     const replayButton = replayPanel.getByRole('button', { name: 'Replay Into Live Flow' }).first()
  433 |     await expect(replayButton).toBeVisible()
  434 |     await replayButton.click()
  435 |     await expect(page.locator('.success-text, .muted-text').filter({ hasText: `Replayed ${replayFixture.externalOrderId} into the live order flow.` }).first()).toBeVisible()
  436 |   } finally {
  437 |     await replayFixture.api.dispose()
  438 |   }
  439 | 
  440 |   await signOutViaUi(page)
  441 | 
  442 |   const scenarioFixture = await createScenarioFixture()
  443 | 
  444 |   try {
  445 |     await loginViaUi(page, users.operationsLead)
  446 |     await navigateWithinApp(page, '/scenario-history')
  447 |     await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()
  448 | 
  449 |     const scenarioApprovalConsole = page.locator('.stack-card').filter({
  450 |       hasText: scenarioFixture.title,
  451 |       has: page.getByRole('button', { name: 'Approve Plan' }),
  452 |     }).first()
  453 |     await expect(scenarioApprovalConsole).toBeVisible()
  454 |     await scenarioApprovalConsole.getByRole('button', { name: 'Approve Plan' }).click()
  455 |     await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()
  456 | 
  457 |     const scenarioExecutionConsole = page.locator('.stack-card').filter({
  458 |       hasText: scenarioFixture.title,
  459 |       has: page.getByRole('button', { name: 'Execute Scenario' }),
  460 |     }).first()
  461 |     await expect(scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' })).toBeVisible()
  462 |     await scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' }).click()
  463 |     await expect(page.locator('.success-text').filter({ hasText: new RegExp(`^Executed ${scenarioFixture.title} as live order `, 'i') }).first()).toBeVisible()
  464 |   } finally {
  465 |     await scenarioFixture.api.dispose()
  466 |   }
  467 | 
  468 |   await signOutViaUi(page)
  469 | 
  470 |   await loginViaUi(page, users.operationsPlanner)
  471 |   await navigateWithinApp(page, '/users')
  472 |   await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
  473 |   await expect(page.getByText('Tenant admin access required')).toBeVisible()
  474 |   await expect(page.getByText('Operators', { exact: true }).first()).toBeVisible()
  475 | })
  476 | 
```