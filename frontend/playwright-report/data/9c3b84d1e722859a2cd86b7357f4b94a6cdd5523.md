# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> auth flow and the full authenticated page system render cleanly in a browser
- Location: tests\prod-proof.spec.mjs:709:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('.public-signin-card').getByText('Invalid operator credentials.')
Expected: visible
Timeout: 15000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 15000ms
  - waiting for locator('.public-signin-card').getByText('Invalid operator credentials.')

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
              - text: wrong-code
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
```

# Test source

```ts
  68  |   ['/catalog', 'Tenant product catalog'],
  69  |   ['/locations', 'Warehouse and site health'],
  70  |   ['/fulfillment', 'Fulfillment and logistics pressure'],
  71  |   ['/scenarios', 'Decision lab and scenario planning'],
  72  |   ['/scenario-history', 'Scenario history and compare'],
  73  |   ['/approvals', 'Approvals center'],
  74  |   ['/escalations', 'Operational escalation inbox'],
  75  |   ['/integrations', 'Connector management and telemetry'],
  76  |   ['/replay-queue', 'Failed inbound recovery'],
  77  |   ['/runtime', 'Runtime, incidents, and observability'],
  78  |   ['/audit-events', 'Audit trail and business events'],
  79  |   ['/users', 'Users and access control'],
  80  |   ['/company-settings', 'Tenant and workspace settings'],
  81  |   ['/profile', 'Personal profile and session controls'],
  82  |   ['/platform-admin', 'Platform overview and cross-tenant trust'],
  83  |   ['/tenant-management', 'Tenant onboarding and workspace rollout'],
  84  |   ['/system-config', 'System configuration and operational defaults'],
  85  |   ['/releases', 'Release, deployment, and environment'],
  86  | ]
  87  | 
  88  | test.describe.configure({ mode: 'serial' })
  89  | 
  90  | async function createApiContext(credentials) {
  91  |   const api = await playwrightRequest.newContext({
  92  |     baseURL: backendUrl,
  93  |     extraHTTPHeaders: {
  94  |       'X-Synapse-Tenant': credentials.tenantCode,
  95  |     },
  96  |   })
  97  | 
  98  |   const loginResponse = await api.post('/api/auth/session/login', {
  99  |     data: credentials,
  100 |   })
  101 | 
  102 |   expect(loginResponse.ok()).toBeTruthy()
  103 |   return api
  104 | }
  105 | 
  106 | async function readJson(response) {
  107 |   const payload = await response.json()
  108 |   if (!response.ok()) {
  109 |     throw new Error(payload.message || `Request failed with status ${response.status()}.`)
  110 |   }
  111 |   return payload
  112 | }
  113 | 
  114 | async function loginViaUi(page, credentials) {
  115 |   await page.goto('/sign-in')
  116 |   await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  117 |   const signInCard = page.locator('.public-signin-card')
  118 |   await waitForSignInReady(signInCard)
  119 |   await fillSignInForm(signInCard, credentials, credentials.password)
  120 |   await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  121 |   await expect(page).toHaveURL(/\/dashboard$/)
  122 |   await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  123 |   await waitForDashboardSnapshotReady(page)
  124 | }
  125 | 
  126 | async function signOutViaUi(page) {
  127 |   const signOutButton = page.getByRole('button', { name: 'Sign Out' }).first()
  128 |   if (await signOutButton.isVisible()) {
  129 |     await signOutButton.click()
  130 |     await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  131 |   }
  132 | }
  133 | 
  134 | async function fillSignInForm(signInCard, credentials, password) {
  135 |   const tenantField = signInCard.getByRole('combobox', { name: 'Tenant workspace', exact: true })
  136 |   const usernameField = signInCard.getByRole('textbox', { name: 'Username', exact: true })
  137 |   const passwordField = signInCard.getByLabel('Password', { exact: true })
  138 |   const submitButton = signInCard.getByRole('button', { name: 'Enter Platform' })
  139 | 
  140 |   let lastError = null
  141 |   for (let attempt = 0; attempt < 3; attempt += 1) {
  142 |     try {
  143 |       await tenantField.fill(credentials.tenantCode)
  144 |       await expect(tenantField).toHaveValue(credentials.tenantCode)
  145 | 
  146 |       await usernameField.fill(credentials.username)
  147 |       await expect(usernameField).toHaveValue(credentials.username)
  148 | 
  149 |       await passwordField.fill(password)
  150 |       await expect(passwordField).toHaveValue(password)
  151 |       await expect(submitButton).toBeEnabled()
  152 |       return
  153 |     } catch (error) {
  154 |       lastError = error
  155 |     }
  156 |   }
  157 | 
  158 |   throw lastError
  159 | }
  160 | 
  161 | async function waitForSignInReady(signInCard) {
  162 |   await expect(signInCard.getByRole('combobox', { name: 'Tenant workspace', exact: true })).toBeEnabled()
  163 |   await expect(signInCard.getByRole('textbox', { name: 'Username', exact: true })).toBeEnabled()
  164 |   await expect(signInCard.getByLabel('Password', { exact: true })).toBeEnabled()
  165 | }
  166 | 
  167 | async function expectSignInErrorAndRecovery(signInCard, message) {
> 168 |   await expect(signInCard.getByText(message)).toBeVisible({ timeout: 15_000 })
      |                                               ^ Error: expect(locator).toBeVisible() failed
  169 |   await waitForSignInReady(signInCard)
  170 | }
  171 | 
  172 | async function writeHostedProofState(nextState) {
  173 |   await fs.mkdir(path.dirname(hostedProofStatePath), { recursive: true })
  174 |   let currentState = {}
  175 |   try {
  176 |     currentState = JSON.parse(await fs.readFile(hostedProofStatePath, 'utf8'))
  177 |   } catch {
  178 |     currentState = {}
  179 |   }
  180 | 
  181 |   await fs.writeFile(
  182 |     hostedProofStatePath,
  183 |     JSON.stringify({ ...currentState, ...nextState }, null, 2),
  184 |     'utf8',
  185 |   )
  186 | }
  187 | 
  188 | async function navigateWithinApp(page, route) {
  189 |   await page.evaluate((nextRoute) => {
  190 |     window.history.pushState({}, '', nextRoute)
  191 |     window.dispatchEvent(new PopStateEvent('popstate'))
  192 |   }, route)
  193 | }
  194 | 
  195 | async function expectNoFatalUiErrors(page) {
  196 |   const fatalErrors = page.locator('.error-text:visible').filter({
  197 |     hasText: /Snapshot load issue:|Invalid operator credentials\.|Request failed|Failed to|Unable to|Unexpected|Forbidden|Access denied/i,
  198 |   })
  199 |   await expect(fatalErrors).toHaveCount(0)
  200 | }
  201 | 
  202 | function escapeRegExp(value) {
  203 |   return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  204 | }
  205 | 
  206 | function metricCard(page, label) {
  207 |   return page.locator('.summary-card.metric-card').filter({
  208 |     has: page.locator('.summary-label', { hasText: new RegExp(`^${escapeRegExp(label)}$`) }),
  209 |   }).first()
  210 | }
  211 | 
  212 | async function summaryCardValue(page, label) {
  213 |   const card = metricCard(page, label)
  214 |   await expect(card).toBeVisible()
  215 |   const value = await card.locator('.summary-value').textContent()
  216 |   return Number.parseInt((value || '').trim(), 10)
  217 | }
  218 | 
  219 | async function waitForNumericSummaryCard(page, label) {
  220 |   let numericValue = Number.NaN
  221 |   await expect.poll(async () => {
  222 |     numericValue = await summaryCardValue(page, label)
  223 |     return Number.isFinite(numericValue)
  224 |   }, {
  225 |     timeout: 30_000,
  226 |     message: `Expected the ${label} summary card to resolve to a numeric value.`,
  227 |   }).toBeTruthy()
  228 |   return numericValue
  229 | }
  230 | 
  231 | async function refreshWorkspace(page) {
  232 |   const refreshButton = page.getByRole('button', { name: 'Refresh' })
  233 |   if (await refreshButton.isVisible().catch(() => false) && await refreshButton.isEnabled().catch(() => false)) {
  234 |     await refreshButton.click()
  235 |   }
  236 | }
  237 | 
  238 | async function activateSelectableButton(buttonLocator) {
  239 |   await expect(buttonLocator).toBeVisible()
  240 |   await buttonLocator.scrollIntoViewIfNeeded()
  241 |   await buttonLocator.focus()
  242 |   await expect(buttonLocator).toBeFocused()
  243 |   await buttonLocator.press('Enter')
  244 | }
  245 | 
  246 | async function waitForDashboardSnapshotReady(page) {
  247 |   const snapshotTimestamp = page.locator('#workspace-trust-rail .muted-text').filter({ hasText: /^Snapshot / }).first()
  248 |   const snapshotLoadError = page.locator('.error-text:visible').filter({ hasText: /Snapshot load issue:/ }).first()
  249 |   let lastRefreshAt = 0
  250 | 
  251 |   await expect.poll(async () => {
  252 |     if (await snapshotLoadError.isVisible().catch(() => false)) {
  253 |       return 'error'
  254 |     }
  255 |     if (await snapshotTimestamp.isVisible().catch(() => false)) {
  256 |       return 'ready'
  257 |     }
  258 |     if (Date.now() - lastRefreshAt >= 5_000) {
  259 |       lastRefreshAt = Date.now()
  260 |       await refreshWorkspace(page)
  261 |     }
  262 |     return 'waiting'
  263 |   }, {
  264 |     timeout: 60_000,
  265 |     message: 'Expected the hosted dashboard to load a real authenticated snapshot before continuing.',
  266 |   }).toBe('ready')
  267 | }
  268 | 
```