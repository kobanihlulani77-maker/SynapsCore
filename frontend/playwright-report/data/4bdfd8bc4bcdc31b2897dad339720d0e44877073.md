# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> alerts, recommendations, orders, inventory, integrations, users, profile, and settings surfaces stay connected to the live backend
- Location: tests\prod-proof.spec.mjs:788:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('button', { name: /UI Replay 074ED74C/i }).first()
Expected: visible
Timeout: 20000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 20000ms
  - waiting for getByRole('button', { name: /UI Replay 074ED74C/i }).first()

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
          - button "Dashboard 21" [ref=e18] [cursor=pointer]:
            - generic [ref=e19]: Dashboard
            - strong [ref=e20]: "21"
          - button "Alerts 4" [ref=e21] [cursor=pointer]:
            - generic [ref=e22]: Alerts
            - strong [ref=e23]: "4"
          - button "Recommendations 33" [ref=e24] [cursor=pointer]:
            - generic [ref=e25]: Recommendations
            - strong [ref=e26]: "33"
      - generic [ref=e27]:
        - paragraph [ref=e28]: Operations
        - generic [ref=e29]:
          - button "Orders 11" [ref=e30] [cursor=pointer]:
            - generic [ref=e31]: Orders
            - strong [ref=e32]: "11"
          - button "Inventory 7" [ref=e33] [cursor=pointer]:
            - generic [ref=e34]: Inventory
            - strong [ref=e35]: "7"
          - button "Catalog 45" [ref=e36] [cursor=pointer]:
            - generic [ref=e37]: Catalog
            - strong [ref=e38]: "45"
          - button "Locations 2" [ref=e39] [cursor=pointer]:
            - generic [ref=e40]: Locations
            - strong [ref=e41]: "2"
          - button "Fulfillment 31" [ref=e42] [cursor=pointer]:
            - generic [ref=e43]: Fulfillment
            - strong [ref=e44]: "31"
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
          - button "Integrations 13" [ref=e63] [cursor=pointer]:
            - generic [ref=e64]: Integrations
            - strong [ref=e65]: "13"
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
        - paragraph [ref=e107]: SYSTEMS
        - heading "Connector management and telemetry" [level=1] [ref=e108]
        - paragraph [ref=e109]: Operate connected systems, inspect health, and understand recent import and sync behavior.
      - generic [ref=e110]:
        - generic [ref=e112]:
          - generic [ref=e113]: Global search
          - textbox "Global search" [ref=e114]:
            - /placeholder: Search pages, orders, or alerts
        - generic "Page focus" [ref=e115]:
          - generic [ref=e116] [cursor=pointer]: Connector health
          - generic [ref=e117] [cursor=pointer]: Import history
          - generic [ref=e118] [cursor=pointer]: Support ownership
        - generic [ref=e119]:
          - generic [ref=e120]: 2026/04/29, 23:34:35
          - generic [ref=e121]: Reconnecting
          - button "Notifications 12" [ref=e122] [cursor=pointer]
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
            - paragraph [ref=e133]: Current page
            - heading "Integrations" [level=2] [ref=e134]
            - paragraph [ref=e135]: 13/13 connectors enabled
          - generic [ref=e136]:
            - generic [ref=e137]: Connector health
            - generic [ref=e138]: Import history
            - generic [ref=e139]: Support ownership
        - article [ref=e141]:
          - generic [ref=e142]:
            - generic [ref=e143]:
              - paragraph [ref=e144]: Integrations
              - heading "Connector health and operational telemetry" [level=2] [ref=e145]
            - generic [ref=e146]: "13"
          - generic [ref=e147]:
            - article [ref=e148]:
              - generic [ref=e149]: Enabled connectors
              - strong [ref=e150]: "13"
              - paragraph [ref=e151]: Routes actively feeding the workspace
            - article [ref=e152]:
              - generic [ref=e153]: Connected systems
              - strong [ref=e154]: "13"
              - paragraph [ref=e155]: Distinct external operating systems connected
            - article [ref=e156]:
              - generic [ref=e157]: Replay queued
              - strong [ref=e158]: "0"
              - paragraph [ref=e159]: Inbound items waiting on recovery
            - article [ref=e160]:
              - generic [ref=e161]: Connector attention
              - strong [ref=e162]: "26"
              - paragraph [ref=e163]: Lanes that need ownership or remediation
          - generic [ref=e164]:
            - article [ref=e165]:
              - generic [ref=e166]:
                - strong [ref=e167]: Connector portfolio
                - generic [ref=e168]: Live
              - generic [ref=e169]:
                - button [ref=e170] [cursor=pointer]:
                  - article [ref=e171]:
                    - generic [ref=e172]:
                      - strong [ref=e173]: UI Replay 074ED74C
                      - generic [ref=e174]: Degraded
                    - paragraph [ref=e175]: ui_replay_074ed74c | Csv Order Import
                    - paragraph [ref=e176]: Batch File Drop | Updated 2026/04/29, 23:33:23
                - button [ref=e177] [cursor=pointer]:
                  - article [ref=e178]:
                    - generic [ref=e179]:
                      - strong [ref=e180]: UI Replay 07675F90
                      - generic [ref=e181]: Degraded
                    - paragraph [ref=e182]: ui_replay_07675f90 | Csv Order Import
                    - paragraph [ref=e183]: Batch File Drop | Updated 2026/04/29, 21:26:08
                - button [ref=e184] [cursor=pointer]:
                  - article [ref=e185]:
                    - generic [ref=e186]:
                      - strong [ref=e187]: UI Replay 0C32AF78
                      - generic [ref=e188]: Degraded
                    - paragraph [ref=e189]: ui_replay_0c32af78 | Csv Order Import
                    - paragraph [ref=e190]: Batch File Drop | Updated 2026/04/29, 22:55:43
                - button [ref=e191] [cursor=pointer]:
                  - article [ref=e192]:
                    - generic [ref=e193]:
                      - strong [ref=e194]: UI Replay 4839AA05
                      - generic [ref=e195]: Degraded
                    - paragraph [ref=e196]: ui_replay_4839aa05 | Csv Order Import
                    - paragraph [ref=e197]: Batch File Drop | Updated 2026/04/24, 14:49:54
                - button [ref=e198] [cursor=pointer]:
                  - article [ref=e199]:
                    - generic [ref=e200]:
                      - strong [ref=e201]: UI Replay 53E2D6F3
                      - generic [ref=e202]: Degraded
                    - paragraph [ref=e203]: ui_replay_53e2d6f3 | Csv Order Import
                    - paragraph [ref=e204]: Batch File Drop | Updated 2026/04/29, 23:14:39
                - button [ref=e205] [cursor=pointer]:
                  - article [ref=e206]:
                    - generic [ref=e207]:
                      - strong [ref=e208]: UI Replay 7E089F4F
                      - generic [ref=e209]: Degraded
                    - paragraph [ref=e210]: ui_replay_7e089f4f | Csv Order Import
                    - paragraph [ref=e211]: Batch File Drop | Updated 2026/04/29, 16:37:54
            - article [ref=e212]:
              - generic [ref=e213]:
                - strong [ref=e214]: Selected connector detail
                - generic [ref=e215]: Relaxed
              - generic [ref=e216]:
                - generic [ref=e217]:
                  - strong [ref=e218]: UI Replay 074ED74C
                  - paragraph [ref=e219]: "Connector is enabled but needs attention: 0 recent inbound issue(s), 0 replay item(s), 0 dead-lettered item(s)."
                  - paragraph [ref=e220]: Source ui_replay_074ed74c | Owner Unassigned | Pending replay 0 | Dead-letter 0
                  - paragraph [ref=e221]: Warehouse fallback is off. Payloads must arrive with a valid lane.
                  - paragraph [ref=e222]: CSV order import is implemented as batch file-drop intake only. Scheduled pull and realtime push are not supported for CSV connectors.
                - generic [ref=e223]:
                  - strong [ref=e224]: Connection posture
                  - paragraph [ref=e225]: This connector is enabled but degrading under inbound failures or replay pressure.
                  - paragraph [ref=e226]: Validation Relaxed | Mapping v1 | Event-driven cadence | Updated 2026/04/29, 23:33:23
                  - paragraph [ref=e227]: Recent inbound failures 0 | Last import Failure at 2026/04/29, 23:33:22
                  - paragraph [ref=e228]: Supported sync modes Batch File Drop
                - generic [ref=e229]:
                  - button "Manage Policies" [ref=e230] [cursor=pointer]
                  - button "Open Replay Queue" [ref=e231] [cursor=pointer]
            - article [ref=e232]:
              - generic [ref=e233]:
                - strong [ref=e234]: Integration posture
                - generic [ref=e235]: 4 recent runs
              - generic [ref=e236]:
                - generic [ref=e237]:
                  - generic: Realtime push
                  - strong: "0"
                - generic [ref=e238]:
                  - generic: Unowned
                  - strong: "13"
                - generic [ref=e239]:
                  - generic: Degraded
                  - strong: "13"
                - generic [ref=e240]:
                  - generic: Fallback on
                  - strong: "0"
                - generic [ref=e241]:
                  - generic: Support incidents
                  - strong: "8"
              - article [ref=e242]:
                - generic [ref=e244]:
                  - paragraph [ref=e245]: Data flow
                  - strong [ref=e246]: Recent sync and import flow
                - generic [ref=e247]:
                  - generic [ref=e248]:
                    - strong [ref=e249]: orders.csv
                    - paragraph [ref=e250]: ui_replay_074ed74c | 1 rows
                    - paragraph [ref=e251]: 0 imported | 1 failed | 2026/04/29, 23:33:22
                  - generic [ref=e252]:
                    - strong [ref=e253]: orders.csv
                    - paragraph [ref=e254]: ui_replay_fa2f5384 | 1 rows
                    - paragraph [ref=e255]: 0 imported | 1 failed | 2026/04/29, 23:26:23
                  - generic [ref=e256]:
                    - strong [ref=e257]: orders.csv
                    - paragraph [ref=e258]: ui_replay_53e2d6f3 | 1 rows
                    - paragraph [ref=e259]: 0 imported | 1 failed | 2026/04/29, 23:14:37
                  - generic [ref=e260]:
                    - strong [ref=e261]: orders.csv
                    - paragraph [ref=e262]: ui_replay_0c32af78 | 1 rows
                    - paragraph [ref=e263]: 0 imported | 1 failed | 2026/04/29, 22:55:42
              - generic [ref=e265]:
                - strong [ref=e266]: What companies should feel here
                - paragraph [ref=e267]: SynapseCore is sitting above the company system estate, showing which lanes are connected, which ones are trusted, and where recovery is needed.
                - paragraph [ref=e268]: Use this page to explain integration confidence, support ownership, supported connector modes, and recovery readiness without leaving the operational workspace.
      - complementary [ref=e269]:
        - article [ref=e270]:
          - generic [ref=e271]:
            - paragraph [ref=e272]: Realtime state
            - generic [ref=e273]: Reconnecting
          - strong [ref=e274]: Monitoring live operating state
          - paragraph [ref=e275]: Snapshot 2026/04/29, 23:34:35
          - generic [ref=e276]:
            - generic [ref=e277]:
              - generic [ref=e278]: Alerts
              - strong [ref=e279]: "4"
            - generic [ref=e280]:
              - generic [ref=e281]: Actions
              - strong [ref=e282]: "12"
            - generic [ref=e283]:
              - generic [ref=e284]: Replay
              - strong [ref=e285]: "0"
            - generic [ref=e286]:
              - generic [ref=e287]: Incidents
              - strong [ref=e288]: "8"
        - article [ref=e289]:
          - generic [ref=e290]:
            - paragraph [ref=e291]: Page focus
            - generic [ref=e292]: Integrations
          - strong [ref=e293]: Connector management and telemetry
          - paragraph [ref=e294]: 13/13 connectors enabled
          - generic [ref=e295]:
            - generic [ref=e296]:
              - generic [ref=e297]: Focus 1
              - strong [ref=e298]: Connector health
            - generic [ref=e299]:
              - generic [ref=e300]: Focus 2
              - strong [ref=e301]: Import history
            - generic [ref=e302]:
              - generic [ref=e303]: Focus 3
              - strong [ref=e304]: Support ownership
            - generic [ref=e305]:
              - generic [ref=e306]: Group
              - strong [ref=e307]: Systems
        - article [ref=e308]:
          - generic [ref=e309]:
            - paragraph [ref=e310]: Act now
            - generic [ref=e311]: 5 items
          - generic [ref=e312]:
            - button "Critical Depletion risk rising for SKU SKU-PILOT-TENANT-PROOF in WH-NORTH Demand is accelerating for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.9 hours at the current demand rate. Review threshold settings and stage replenishment now." [ref=e313] [cursor=pointer]:
              - generic [ref=e314]: Critical
              - strong [ref=e315]: Depletion risk rising for SKU SKU-PILOT-TENANT-PROOF in WH-NORTH
              - paragraph [ref=e316]: Demand is accelerating for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.9 hours at the current demand rate. Review threshold settings and stage replenishment now.
            - button "Critical Delivery delay risk rising in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 21, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e317] [cursor=pointer]:
              - generic [ref=e318]: Critical
              - strong [ref=e319]: Delivery delay risk rising in WH-NORTH
              - paragraph [ref=e320]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 21, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Fulfillment backlog building in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 21, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e321] [cursor=pointer]:
              - generic [ref=e322]: Critical
              - strong [ref=e323]: Fulfillment backlog building in WH-NORTH
              - paragraph [ref=e324]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 21, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Urgent reorder for SKU SKU-RT-111E2EA8 at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e325] [cursor=pointer]:
              - generic [ref=e326]: Critical
              - strong [ref=e327]: Urgent reorder for SKU SKU-RT-111E2EA8 at WH-NORTH
              - paragraph [ref=e328]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
            - button "Critical Investigate logistics anomaly in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 21, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e329] [cursor=pointer]:
              - generic [ref=e330]: Critical
              - strong [ref=e331]: Investigate logistics anomaly in WH-NORTH
              - paragraph [ref=e332]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 21, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
        - article [ref=e333]:
          - generic [ref=e334]:
            - paragraph [ref=e335]: Activity stream
            - generic [ref=e336]: "6"
          - generic [ref=e337]:
            - generic [ref=e338]:
              - strong [ref=e339]: Request Rejected
              - paragraph [ref=e340]: High incident
              - generic [ref=e341]: 2026/04/29, 23:34:00
            - generic [ref=e342]:
              - strong [ref=e343]: Request Rejected
              - paragraph [ref=e344]: High incident
              - generic [ref=e345]: 2026/04/29, 23:33:55
            - generic [ref=e346]:
              - strong [ref=e347]: Scenario Executed
              - paragraph [ref=e348]: Executed scenario 15 into live order ORD-10C2C8D0 for warehouse WH-NORTH.
              - generic [ref=e349]: 2026/04/29, 23:33:52
            - generic [ref=e350]:
              - strong [ref=e351]: Order Ingested
              - paragraph [ref=e352]: Order ORD-10C2C8D0 received for warehouse WH-NORTH with 1 line items.
              - generic [ref=e353]: 2026/04/29, 23:33:52
            - generic [ref=e354]:
              - strong [ref=e355]: Request Rejected
              - paragraph [ref=e356]: ApiRequest | /api/dashboard/snapshot
              - generic [ref=e357]: 2026/04/29, 23:34:00
            - generic [ref=e358]:
              - strong [ref=e359]: Request Rejected
              - paragraph [ref=e360]: ApiRequest | /api/dashboard/snapshot
              - generic [ref=e361]: 2026/04/29, 23:33:55
        - article [ref=e362]:
          - generic [ref=e363]:
            - paragraph [ref=e364]: Operator
            - generic [ref=e365]: Healthy
          - strong [ref=e366]: Hosted Verification Tenant Admin
          - paragraph [ref=e367]: PILOT-TENANT Hosted Verification | Operations Lead
          - paragraph [ref=e368]: Roles Escalation Owner, Integration Admin, Integration Operator, Review Owner, Tenant Admin
          - paragraph [ref=e369]: Warehouse scope Tenant-wide
```

# Test source

```ts
  101 |   const signInCard = page.locator('.public-signin-card')
  102 |   await waitForSignInReady(signInCard)
  103 |   await fillSignInForm(signInCard, credentials, credentials.password)
  104 |   await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  105 |   await expect(page).toHaveURL(/\/dashboard$/)
  106 |   await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  107 | }
  108 | 
  109 | async function signOutViaUi(page) {
  110 |   const signOutButton = page.getByRole('button', { name: 'Sign Out' }).first()
  111 |   if (await signOutButton.isVisible()) {
  112 |     await signOutButton.click()
  113 |     await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  114 |   }
  115 | }
  116 | 
  117 | async function fillSignInForm(signInCard, credentials, password) {
  118 |   const tenantField = signInCard.getByRole('combobox', { name: 'Tenant workspace', exact: true })
  119 |   const usernameField = signInCard.getByRole('textbox', { name: 'Username', exact: true })
  120 |   const passwordField = signInCard.getByLabel('Password', { exact: true })
  121 |   const submitButton = signInCard.getByRole('button', { name: 'Enter Platform' })
  122 | 
  123 |   let lastError = null
  124 |   for (let attempt = 0; attempt < 3; attempt += 1) {
  125 |     try {
  126 |       await tenantField.fill(credentials.tenantCode)
  127 |       await expect(tenantField).toHaveValue(credentials.tenantCode)
  128 | 
  129 |       await usernameField.fill(credentials.username)
  130 |       await expect(usernameField).toHaveValue(credentials.username)
  131 | 
  132 |       await passwordField.fill(password)
  133 |       await expect(passwordField).toHaveValue(password)
  134 |       await expect(submitButton).toBeEnabled()
  135 |       return
  136 |     } catch (error) {
  137 |       lastError = error
  138 |     }
  139 |   }
  140 | 
  141 |   throw lastError
  142 | }
  143 | 
  144 | async function waitForSignInReady(signInCard) {
  145 |   await expect(signInCard.getByRole('combobox', { name: 'Tenant workspace', exact: true })).toBeEnabled()
  146 |   await expect(signInCard.getByRole('textbox', { name: 'Username', exact: true })).toBeEnabled()
  147 |   await expect(signInCard.getByLabel('Password', { exact: true })).toBeEnabled()
  148 | }
  149 | 
  150 | async function navigateWithinApp(page, route) {
  151 |   await page.evaluate((nextRoute) => {
  152 |     window.history.pushState({}, '', nextRoute)
  153 |     window.dispatchEvent(new PopStateEvent('popstate'))
  154 |   }, route)
  155 | }
  156 | 
  157 | async function expectNoFatalUiErrors(page) {
  158 |   const fatalErrors = page.locator('.error-text:visible').filter({
  159 |     hasText: /Snapshot load issue:|Invalid operator credentials\.|Request failed|Failed to|Unable to|Unexpected|Forbidden|Access denied/i,
  160 |   })
  161 |   await expect(fatalErrors).toHaveCount(0)
  162 | }
  163 | 
  164 | function escapeRegExp(value) {
  165 |   return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  166 | }
  167 | 
  168 | function metricCard(page, label) {
  169 |   return page.locator('.summary-card.metric-card').filter({
  170 |     has: page.locator('.summary-label', { hasText: new RegExp(`^${escapeRegExp(label)}$`) }),
  171 |   }).first()
  172 | }
  173 | 
  174 | async function summaryCardValue(page, label) {
  175 |   const card = metricCard(page, label)
  176 |   await expect(card).toBeVisible()
  177 |   const value = await card.locator('.summary-value').textContent()
  178 |   return Number.parseInt((value || '').trim(), 10)
  179 | }
  180 | 
  181 | async function waitForNumericSummaryCard(page, label) {
  182 |   let numericValue = Number.NaN
  183 |   await expect.poll(async () => {
  184 |     numericValue = await summaryCardValue(page, label)
  185 |     return Number.isFinite(numericValue)
  186 |   }, {
  187 |     timeout: 30_000,
  188 |     message: `Expected the ${label} summary card to resolve to a numeric value.`,
  189 |   }).toBeTruthy()
  190 |   return numericValue
  191 | }
  192 | 
  193 | async function refreshWorkspace(page) {
  194 |   const refreshButton = page.getByRole('button', { name: 'Refresh' })
  195 |   if (await refreshButton.isVisible().catch(() => false) && await refreshButton.isEnabled().catch(() => false)) {
  196 |     await refreshButton.click()
  197 |   }
  198 | }
  199 | 
  200 | async function activateSelectableButton(buttonLocator) {
> 201 |   await expect(buttonLocator).toBeVisible()
      |                               ^ Error: expect(locator).toBeVisible() failed
  202 |   await buttonLocator.scrollIntoViewIfNeeded()
  203 |   await buttonLocator.focus()
  204 |   await expect(buttonLocator).toBeFocused()
  205 |   await buttonLocator.press('Enter')
  206 | }
  207 | 
  208 | async function readReplayOutcome(api, externalOrderId) {
  209 |   const replayQueue = await readJson(await api.get('/api/integrations/orders/replay-queue'))
  210 |   const replayRecord = replayQueue.find((record) => record.externalOrderId === externalOrderId)
  211 |   if (replayRecord) {
  212 |     return { state: 'queued', status: replayRecord.status, record: replayRecord }
  213 |   }
  214 | 
  215 |   const recentOrders = await readJson(await api.get('/api/orders/recent'))
  216 |   if (recentOrders.some((order) => order.externalOrderId === externalOrderId)) {
  217 |     return { state: 'replayed' }
  218 |   }
  219 | 
  220 |   return { state: 'missing' }
  221 | }
  222 | 
  223 | async function waitForReplayResolution(api, externalOrderId, timeout, message) {
  224 |   await expect.poll(async () => {
  225 |     const replayOutcome = await readReplayOutcome(api, externalOrderId)
  226 |     return replayOutcome.state === 'queued' ? `${replayOutcome.state}:${replayOutcome.status}` : replayOutcome.state
  227 |   }, {
  228 |     timeout,
  229 |     message,
  230 |   }).toBe('replayed')
  231 | }
  232 | 
  233 | async function createReplayFixture() {
  234 |   const inventoryAdmin = await createApiContext(users.operationsLead)
  235 |   const api = await createApiContext(users.integrationLead)
  236 |   const suffix = randomUUID().slice(0, 8).toUpperCase()
  237 |   const sourceSystem = `ui_replay_${suffix}`.toLowerCase()
  238 |   const externalOrderId = `UI-RPL-${suffix}`
  239 | 
  240 |   try {
  241 |     await readJson(await inventoryAdmin.post('/api/inventory/update', {
  242 |       data: {
  243 |         productSku: proofProductSku,
  244 |         warehouseCode: 'WH-NORTH',
  245 |         quantityAvailable: 50,
  246 |         reorderThreshold: 12,
  247 |       },
  248 |     }))
  249 | 
  250 |     await readJson(await api.post('/api/integrations/orders/connectors', {
  251 |       data: {
  252 |         sourceSystem,
  253 |         type: 'CSV_ORDER_IMPORT',
  254 |         displayName: `UI Replay ${suffix}`,
  255 |         enabled: false,
  256 |         syncMode: 'BATCH_FILE_DROP',
  257 |         validationPolicy: 'RELAXED',
  258 |         transformationPolicy: 'NORMALIZE_CODES',
  259 |         allowDefaultWarehouseFallback: false,
  260 |         notes: 'Disposable replay verification connector.',
  261 |       },
  262 |     }))
  263 | 
  264 |     const csvImportResponse = await api.post('/api/integrations/orders/csv-import', {
  265 |       multipart: {
  266 |         file: {
  267 |           name: 'orders.csv',
  268 |           mimeType: 'text/csv',
  269 |           buffer: Buffer.from(
  270 |             `externalOrderId,warehouseCode,productSku,quantity,unitPrice\n${externalOrderId},WH-NORTH,${proofProductSku},2,88.00\n`,
  271 |             'utf8',
  272 |           ),
  273 |         },
  274 |         sourceSystem,
  275 |       },
  276 |     })
  277 |     const csvImportPayload = await readJson(csvImportResponse)
  278 |     expect(csvImportPayload.ordersFailed).toBe(1)
  279 | 
  280 |     await readJson(await api.post('/api/integrations/orders/connectors', {
  281 |       data: {
  282 |         sourceSystem,
  283 |         type: 'CSV_ORDER_IMPORT',
  284 |         displayName: `UI Replay ${suffix}`,
  285 |         enabled: true,
  286 |         syncMode: 'BATCH_FILE_DROP',
  287 |         validationPolicy: 'RELAXED',
  288 |         transformationPolicy: 'NORMALIZE_CODES',
  289 |         allowDefaultWarehouseFallback: false,
  290 |         notes: 'Enabled for replay verification.',
  291 |       },
  292 |     }))
  293 | 
  294 |     await expect.poll(async () => {
  295 |       const connectors = await readJson(await api.get('/api/integrations/orders/connectors'))
  296 |       return connectors.find((connector) => connector.sourceSystem === sourceSystem && connector.type === 'CSV_ORDER_IMPORT')?.enabled ?? false
  297 |     }, {
  298 |       timeout: 30_000,
  299 |       message: `Expected replay verification connector ${sourceSystem} to become enabled before UI replay proof.`,
  300 |     }).toBe(true)
  301 | 
```