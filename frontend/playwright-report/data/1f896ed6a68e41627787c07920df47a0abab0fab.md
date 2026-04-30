# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> frontend surfaces backend auth rate limiting without getting stuck in a loading state
- Location: tests\prod-proof.spec.mjs:971:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('.public-signin-card').getByText('Authentication rate limit exceeded. Wait before attempting another sign-in.')
Expected: visible
Timeout: 15000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 15000ms
  - waiting for locator('.public-signin-card').getByText('Authentication rate limit exceeded. Wait before attempting another sign-in.')

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
        - generic [ref=e20]: 3 workspaces visible
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
      - generic [ref=e35]:
        - generic [ref=e36]:
          - generic [ref=e37]:
            - generic [ref=e38]: Tenant workspace
            - combobox "Tenant workspace" [ref=e39]: PILOT-TENANT
          - generic [ref=e40]:
            - generic [ref=e41]: Username
            - textbox "Username" [ref=e42]:
              - /placeholder: workspace.admin
              - text: admin.pilot
          - generic [ref=e43]:
            - generic [ref=e44]: Password
            - textbox "Password" [ref=e45]:
              - /placeholder: Enter workspace password
              - text: wrong-rate-limit
        - generic [ref=e46]:
          - generic [ref=e47]:
            - checkbox "Remember tenant code and username on this device" [checked] [ref=e48]
            - generic [ref=e49]: Remember tenant code and username on this device
          - generic [ref=e50]: Password recovery is managed by your tenant admin.
        - generic [ref=e51]:
          - button "Enter Platform" [ref=e52] [cursor=pointer]
          - button "Product Overview" [ref=e53] [cursor=pointer]
      - generic [ref=e54]:
        - article [ref=e55]:
          - generic [ref=e56]: Tenant scope
          - strong [ref=e57]: PILOT-TENANT Hosted Verification
          - paragraph [ref=e58]: Operators enter a company workspace, not a generic app account.
        - article [ref=e59]:
          - generic [ref=e60]: Session model
          - strong [ref=e61]: Secure browser session
          - paragraph [ref=e62]: Protected actions, approvals, replay, and realtime access all follow the signed-in operator identity.
        - article [ref=e63]:
          - generic [ref=e64]: Realtime posture
          - strong [ref=e65]: Live transport configured
          - paragraph [ref=e66]: SynapseCore opens the command workspace with live operational updates when the session is valid.
      - paragraph [ref=e67]: Signing into PILOT-TENANT Hosted Verification.
      - paragraph [ref=e68]: API https://synapscore-3.onrender.com | Realtime wss://synapscore-3.onrender.com/ws | Transport undefined
      - paragraph [ref=e69]: Unable to reach the SynapseCore backend at https://synapscore-3.onrender.com. Check the live API URL, CORS policy, and backend availability.
```

# Test source

```ts
  51  |   ['/inventory', 'Inventory intelligence'],
  52  |   ['/catalog', 'Tenant product catalog'],
  53  |   ['/locations', 'Warehouse and site health'],
  54  |   ['/fulfillment', 'Fulfillment and logistics pressure'],
  55  |   ['/scenarios', 'Decision lab and scenario planning'],
  56  |   ['/scenario-history', 'Scenario history and compare'],
  57  |   ['/approvals', 'Approvals center'],
  58  |   ['/escalations', 'Operational escalation inbox'],
  59  |   ['/integrations', 'Connector management and telemetry'],
  60  |   ['/replay-queue', 'Failed inbound recovery'],
  61  |   ['/runtime', 'Runtime, incidents, and observability'],
  62  |   ['/audit-events', 'Audit trail and business events'],
  63  |   ['/users', 'Users and access control'],
  64  |   ['/company-settings', 'Tenant and workspace settings'],
  65  |   ['/profile', 'Personal profile and session controls'],
  66  |   ['/platform-admin', 'Platform overview and cross-tenant trust'],
  67  |   ['/tenant-management', 'Tenant onboarding and workspace rollout'],
  68  |   ['/system-config', 'System configuration and operational defaults'],
  69  |   ['/releases', 'Release, deployment, and environment'],
  70  | ]
  71  | 
  72  | test.describe.configure({ mode: 'serial' })
  73  | 
  74  | async function createApiContext(credentials) {
  75  |   const api = await playwrightRequest.newContext({
  76  |     baseURL: backendUrl,
  77  |     extraHTTPHeaders: {
  78  |       'X-Synapse-Tenant': credentials.tenantCode,
  79  |     },
  80  |   })
  81  | 
  82  |   const loginResponse = await api.post('/api/auth/session/login', {
  83  |     data: credentials,
  84  |   })
  85  | 
  86  |   expect(loginResponse.ok()).toBeTruthy()
  87  |   return api
  88  | }
  89  | 
  90  | async function readJson(response) {
  91  |   const payload = await response.json()
  92  |   if (!response.ok()) {
  93  |     throw new Error(payload.message || `Request failed with status ${response.status()}.`)
  94  |   }
  95  |   return payload
  96  | }
  97  | 
  98  | async function loginViaUi(page, credentials) {
  99  |   await page.goto('/sign-in')
  100 |   await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
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
  150 | async function expectSignInErrorAndRecovery(signInCard, message) {
> 151 |   await expect(signInCard.getByText(message)).toBeVisible({ timeout: 15_000 })
      |                                               ^ Error: expect(locator).toBeVisible() failed
  152 |   await waitForSignInReady(signInCard)
  153 | }
  154 | 
  155 | async function navigateWithinApp(page, route) {
  156 |   await page.evaluate((nextRoute) => {
  157 |     window.history.pushState({}, '', nextRoute)
  158 |     window.dispatchEvent(new PopStateEvent('popstate'))
  159 |   }, route)
  160 | }
  161 | 
  162 | async function expectNoFatalUiErrors(page) {
  163 |   const fatalErrors = page.locator('.error-text:visible').filter({
  164 |     hasText: /Snapshot load issue:|Invalid operator credentials\.|Request failed|Failed to|Unable to|Unexpected|Forbidden|Access denied/i,
  165 |   })
  166 |   await expect(fatalErrors).toHaveCount(0)
  167 | }
  168 | 
  169 | function escapeRegExp(value) {
  170 |   return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  171 | }
  172 | 
  173 | function metricCard(page, label) {
  174 |   return page.locator('.summary-card.metric-card').filter({
  175 |     has: page.locator('.summary-label', { hasText: new RegExp(`^${escapeRegExp(label)}$`) }),
  176 |   }).first()
  177 | }
  178 | 
  179 | async function summaryCardValue(page, label) {
  180 |   const card = metricCard(page, label)
  181 |   await expect(card).toBeVisible()
  182 |   const value = await card.locator('.summary-value').textContent()
  183 |   return Number.parseInt((value || '').trim(), 10)
  184 | }
  185 | 
  186 | async function waitForNumericSummaryCard(page, label) {
  187 |   let numericValue = Number.NaN
  188 |   await expect.poll(async () => {
  189 |     numericValue = await summaryCardValue(page, label)
  190 |     return Number.isFinite(numericValue)
  191 |   }, {
  192 |     timeout: 30_000,
  193 |     message: `Expected the ${label} summary card to resolve to a numeric value.`,
  194 |   }).toBeTruthy()
  195 |   return numericValue
  196 | }
  197 | 
  198 | async function refreshWorkspace(page) {
  199 |   const refreshButton = page.getByRole('button', { name: 'Refresh' })
  200 |   if (await refreshButton.isVisible().catch(() => false) && await refreshButton.isEnabled().catch(() => false)) {
  201 |     await refreshButton.click()
  202 |   }
  203 | }
  204 | 
  205 | async function activateSelectableButton(buttonLocator) {
  206 |   await expect(buttonLocator).toBeVisible()
  207 |   await buttonLocator.scrollIntoViewIfNeeded()
  208 |   await buttonLocator.focus()
  209 |   await expect(buttonLocator).toBeFocused()
  210 |   await buttonLocator.press('Enter')
  211 | }
  212 | 
  213 | async function findVisibleIntegrationConnector(page, connectors) {
  214 |   for (const connector of connectors) {
  215 |     if (!connector?.displayName) {
  216 |       continue
  217 |     }
  218 |     const button = page.locator('button.system-select-card').filter({ hasText: connector.displayName }).first()
  219 |     if (await button.isVisible().catch(() => false)) {
  220 |       return { connector, button }
  221 |     }
  222 |   }
  223 |   return null
  224 | }
  225 | 
  226 | async function waitForScenarioHistoryCard(page, scenarioTitle) {
  227 |   const scenarioCard = page.locator('.approval-board').getByRole('button', {
  228 |     name: new RegExp(escapeRegExp(scenarioTitle), 'i'),
  229 |   }).first()
  230 | 
  231 |   await expect.poll(async () => {
  232 |     await refreshWorkspace(page)
  233 |     return await scenarioCard.isVisible().catch(() => false)
  234 |   }, {
  235 |     timeout: 30_000,
  236 |     message: `Expected scenario history to render ${scenarioTitle} in the approval board.`,
  237 |   }).toBe(true)
  238 | 
  239 |   return scenarioCard
  240 | }
  241 | 
  242 | async function waitForAuthLoginBucketToClear() {
  243 |   const api = await playwrightRequest.newContext({ baseURL: backendUrl })
  244 |   try {
  245 |     await expect.poll(async () => {
  246 |       const response = await api.post('/api/auth/session/login', {
  247 |         data: {
  248 |           tenantCode: users.operationsLead.tenantCode,
  249 |           username: users.operationsLead.username,
  250 |           password: 'wrong-code',
  251 |         },
```