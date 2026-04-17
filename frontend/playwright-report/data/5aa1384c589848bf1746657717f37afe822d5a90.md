# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> auth flow and the full authenticated page system render cleanly in a browser
- Location: tests\prod-proof.spec.mjs:227:1

# Error details

```
Test timeout of 180000ms exceeded.
```

```
Error: locator.fill: Test timeout of 180000ms exceeded.
Call log:
  - waiting for getByLabel('Tenant workspace')
    - locator resolved to <input disabled value="" type="text" autocomplete="organization" list="tenant-workspace-options" placeholder="Loading workspace directory..."/>
    - fill("SYNAPSE-DEMO")
  - attempting fill action
    2 × waiting for element to be visible, enabled and editable
      - element is not enabled
    - retrying fill action
    - waiting 20ms
    2 × waiting for element to be visible, enabled and editable
      - element is not enabled
    - retrying fill action
      - waiting 100ms
    318 × waiting for element to be visible, enabled and editable
        - element is not enabled
      - retrying fill action
        - waiting 500ms

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
            - combobox "Tenant workspace" [disabled] [ref=e44]
          - generic [ref=e45]:
            - generic [ref=e46]: Username
            - textbox "Username" [disabled] [ref=e47]:
              - /placeholder: workspace.admin
          - generic [ref=e48]:
            - generic [ref=e49]: Password
            - textbox "Password" [disabled] [ref=e50]:
              - /placeholder: Enter workspace password
        - generic [ref=e51]:
          - generic [ref=e52]:
            - checkbox "Remember tenant code and username on this device" [checked] [ref=e53]
            - generic [ref=e54]: Remember tenant code and username on this device
          - generic [ref=e55]: Password recovery is managed by your tenant admin.
        - generic [ref=e56]:
          - button "Enter Platform" [disabled] [ref=e57]
          - button "Product Overview" [ref=e58] [cursor=pointer]
      - generic [ref=e59]:
        - article [ref=e60]:
          - generic [ref=e61]: Tenant scope
          - strong [ref=e62]: Workspace required
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
      - paragraph [ref=e73]: API https://synapscore-3.onrender.com | Realtime wss://synapscore-3.onrender.com/ws
```

# Test source

```ts
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
  229 |   await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  230 | 
> 231 |   await page.getByLabel('Tenant workspace').fill(users.operationsLead.tenantCode)
      |                                             ^ Error: locator.fill: Test timeout of 180000ms exceeded.
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
  330 |       has: page.getByRole('button', { name: 'Execute Scenario' }),
  331 |     }).first()
```