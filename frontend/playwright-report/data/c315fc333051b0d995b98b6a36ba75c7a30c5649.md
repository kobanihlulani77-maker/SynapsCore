# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> product catalog onboarding works through tenant-scoped API and browser surface
- Location: tests\prod-proof.spec.mjs:637:1

# Error details

```
Error: expect(received).toBeTruthy()

Received: false
```

# Test source

```ts
  1   | import { randomUUID } from 'node:crypto'
  2   | import { expect, request as playwrightRequest, test } from '@playwright/test'
  3   | 
  4   | const backendUrl = process.env.PLAYWRIGHT_API_BASE_URL
  5   |   || process.env.PLAYWRIGHT_BACKEND_URL
  6   |   || 'https://synapscore-3.onrender.com'
  7   | const requiredEnv = (...names) => {
  8   |   for (const name of names) {
  9   |     const value = process.env[name]
  10  |     if (value && value.trim()) {
  11  |       return value.trim()
  12  |     }
  13  |   }
  14  |   throw new Error(`Missing required environment variable. Set one of: ${names.join(', ')} for live production proof.`)
  15  | }
  16  | 
  17  | const proofTenantCode = requiredEnv('PLAYWRIGHT_TENANT_CODE').toUpperCase()
  18  | const deriveDefaultProofProductSku = (tenantCode) => {
  19  |   const normalizedTenant = tenantCode.replace(/[^A-Z0-9._-]/g, '-')
  20  |   const candidate = `SKU-${normalizedTenant}-PROOF`
  21  |   return candidate.length <= 64
  22  |     ? candidate
  23  |     : `SKU-${normalizedTenant.slice(0, Math.min(normalizedTenant.length, 50))}-PRF`
  24  | }
  25  | const defaultProofProductSku = deriveDefaultProofProductSku(proofTenantCode)
  26  | const proofProductSku = (process.env.PLAYWRIGHT_PROOF_PRODUCT_SKU || defaultProofProductSku).trim().toUpperCase()
  27  | 
  28  | const users = {
  29  |   operationsLead: {
  30  |     tenantCode: proofTenantCode,
  31  |     username: requiredEnv('PLAYWRIGHT_TENANT_ADMIN_USERNAME', 'PLAYWRIGHT_OPERATIONS_LEAD_USERNAME'),
  32  |     password: requiredEnv('PLAYWRIGHT_TENANT_ADMIN_PASSWORD', 'PLAYWRIGHT_OPERATIONS_LEAD_PASSWORD'),
  33  |   },
  34  |   operationsPlanner: {
  35  |     tenantCode: proofTenantCode,
  36  |     username: requiredEnv('PLAYWRIGHT_PLANNER_USERNAME', 'PLAYWRIGHT_OPERATIONS_PLANNER_USERNAME'),
  37  |     password: requiredEnv('PLAYWRIGHT_PLANNER_PASSWORD', 'PLAYWRIGHT_OPERATIONS_PLANNER_PASSWORD'),
  38  |   },
  39  |   integrationLead: {
  40  |     tenantCode: proofTenantCode,
  41  |     username: requiredEnv('PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME', 'PLAYWRIGHT_INTEGRATION_LEAD_USERNAME'),
  42  |     password: requiredEnv('PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD', 'PLAYWRIGHT_INTEGRATION_LEAD_PASSWORD'),
  43  |   },
  44  | }
  45  | 
  46  | const appPages = [
  47  |   ['/dashboard', 'Live operational command center'],
  48  |   ['/alerts', 'Operational warning center'],
  49  |   ['/recommendations', 'Action queue for the operating team'],
  50  |   ['/orders', 'Live order operations'],
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
> 86  |   expect(loginResponse.ok()).toBeTruthy()
      |                              ^ Error: expect(received).toBeTruthy()
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
  151 |   await expect(signInCard.getByText(message)).toBeVisible({ timeout: 15_000 })
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
```