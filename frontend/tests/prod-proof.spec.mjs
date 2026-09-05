import { randomUUID } from 'node:crypto'
import { expect, request as playwrightRequest, test } from '@playwright/test'
import {
  authRateLimitCooldownBufferMs,
  authRateLimitWindowMs,
  readHostedProofStateSync,
  writeHostedProofState,
} from './prod-proof-state.mjs'

const hostedProofState = readHostedProofStateSync()

const backendUrl = process.env.PLAYWRIGHT_API_BASE_URL
  || process.env.PLAYWRIGHT_BACKEND_URL
  || hostedProofState.PLAYWRIGHT_API_BASE_URL
  || hostedProofState.PLAYWRIGHT_BACKEND_URL
  || 'https://synapscore-3.onrender.com'
const requiredEnv = (...names) => {
  for (const name of names) {
    const value = process.env[name]
    if (value && value.trim()) {
      return value.trim()
    }
    const stateValue = hostedProofState[name]
    if (stateValue && String(stateValue).trim()) {
      return String(stateValue).trim()
    }
  }
  throw new Error(`Missing required hosted proof value. Run scripts/prepare-hosted-proof.ps1 first, or set one of: ${names.join(', ')} for live production proof.`)
}

const proofTenantCode = requiredEnv('PLAYWRIGHT_TENANT_CODE').toUpperCase()
const deriveDefaultProofProductSku = (tenantCode) => {
  const normalizedTenant = tenantCode.replace(/[^A-Z0-9._-]/g, '-')
  const candidate = `SKU-${normalizedTenant}-PROOF`
  return candidate.length <= 64
    ? candidate
    : `SKU-${normalizedTenant.slice(0, Math.min(normalizedTenant.length, 50))}-PRF`
}
const defaultProofProductSku = deriveDefaultProofProductSku(proofTenantCode)
const proofProductSku = (process.env.PLAYWRIGHT_PROOF_PRODUCT_SKU || hostedProofState.PLAYWRIGHT_PROOF_PRODUCT_SKU || defaultProofProductSku).trim().toUpperCase()
const configuredAuthRateLimitMaxAttempts = Number.parseInt(
  process.env.PLAYWRIGHT_AUTH_RATE_LIMIT_MAX_ATTEMPTS
    || process.env.SYNAPSECORE_RATE_LIMIT_AUTH_LOGIN_MAX_ATTEMPTS
    || '12',
  10,
)
const authRateLimitAttemptBudget = Number.isFinite(configuredAuthRateLimitMaxAttempts) && configuredAuthRateLimitMaxAttempts > 0
  ? configuredAuthRateLimitMaxAttempts + 20
  : 50
const signInHeadingPattern = /Access your operational workspace\.?|Enter the operational platform/i

const users = {
  operationsLead: {
    tenantCode: proofTenantCode,
    username: requiredEnv('PLAYWRIGHT_TENANT_ADMIN_USERNAME', 'PLAYWRIGHT_OPERATIONS_LEAD_USERNAME'),
    password: requiredEnv('PLAYWRIGHT_TENANT_ADMIN_PASSWORD', 'PLAYWRIGHT_OPERATIONS_LEAD_PASSWORD'),
    actorName: 'Operations Lead',
  },
  operationsPlanner: {
    tenantCode: proofTenantCode,
    username: requiredEnv('PLAYWRIGHT_PLANNER_USERNAME', 'PLAYWRIGHT_OPERATIONS_PLANNER_USERNAME'),
    password: requiredEnv('PLAYWRIGHT_PLANNER_PASSWORD', 'PLAYWRIGHT_OPERATIONS_PLANNER_PASSWORD'),
  },
  integrationLead: {
    tenantCode: proofTenantCode,
    username: requiredEnv('PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME', 'PLAYWRIGHT_INTEGRATION_LEAD_USERNAME'),
    password: requiredEnv('PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD', 'PLAYWRIGHT_INTEGRATION_LEAD_PASSWORD'),
  },
  reviewOwner: {
    tenantCode: proofTenantCode,
    username: requiredEnv('PLAYWRIGHT_REVIEW_OWNER_USERNAME'),
    password: requiredEnv('PLAYWRIGHT_REVIEW_OWNER_PASSWORD'),
    actorName: 'North Review Owner',
  },
}

const appPages = [
  ['/dashboard', 'Live operational command center'],
  ['/alerts', 'Operational warning center'],
  ['/recommendations', 'Action queue for the operating team'],
  ['/orders', 'Live order operations'],
  ['/inventory', 'Inventory intelligence'],
  ['/catalog', 'Tenant product catalog'],
  ['/locations', 'Warehouse and site health'],
  ['/fulfillment', 'Fulfillment and logistics pressure'],
  ['/scenarios', 'Decision lab and scenario planning'],
  ['/scenario-history', 'Scenario history and compare'],
  ['/approvals', 'Approvals center'],
  ['/escalations', 'Operational escalation inbox'],
  ['/integrations', 'Connector management and telemetry'],
  ['/replay-queue', 'Failed inbound recovery'],
  ['/runtime', 'Runtime, incidents, and observability'],
  ['/audit-events', 'Audit trail and business events'],
  ['/users', 'Users and access control'],
  ['/company-settings', 'Tenant and workspace settings'],
  ['/profile', 'Personal profile and session controls'],
]

const platformProtectedRoutes = ['/platform-admin', '/tenant-management', '/system-config', '/releases']

test.describe.configure({ mode: 'serial' })

async function createApiContext(credentials) {
  const api = await playwrightRequest.newContext({
    baseURL: backendUrl,
    extraHTTPHeaders: {
      'X-Synapse-Tenant': credentials.tenantCode,
    },
  })

  const loginResponse = await api.post('/api/auth/session/login', {
    data: credentials,
  })

  expect(loginResponse.ok()).toBeTruthy()
  return api
}

async function readJson(response, context = {}) {
  const responseText = await response.text()
  let payload = null
  try {
    payload = responseText ? JSON.parse(responseText) : null
  } catch {
    payload = null
  }

  const responseHeaders = safeResponseHeaders(response)
  const responseMethod = safeResponseMethod(response, context)
  const responseUrl = safeResponseUrl(response, context)

  if (!response.ok()) {
    const failureDetails = {
      method: responseMethod,
      url: responseUrl,
      status: response.status(),
      requestId: responseHeaders['x-request-id'] || payload?.requestId || null,
      responseBody: payload ?? responseText,
      requestPayload: context.requestPayload ?? null,
      requestFormData: context.requestFormData ?? null,
      note: context.note ?? null,
    }
    throw new Error(`SynapseCore API request failed: ${JSON.stringify(failureDetails)}`)
  }

  if (payload !== null) {
    return payload
  }
  if (!responseText) {
    return null
  }
  throw new Error(`Expected JSON response but received non-JSON payload from ${responseMethod} ${responseUrl}: ${responseText}`)
}

function parseApiFailureDetails(error) {
  const message = error?.message || String(error)
  const prefix = 'SynapseCore API request failed: '
  if (!message.startsWith(prefix)) {
    return null
  }

  try {
    return JSON.parse(message.slice(prefix.length))
  } catch {
    return null
  }
}

function isTransientApiReadFailure(error, options = {}) {
  const {
    url = null,
    statuses = [502, 503, 504],
  } = options
  const failureDetails = parseApiFailureDetails(error)
  if (!failureDetails) {
    return false
  }

  const method = `${failureDetails.method || ''}`.toUpperCase()
  const status = Number(failureDetails.status)
  if (method !== 'GET' || !statuses.includes(status)) {
    return false
  }

  if (url && failureDetails.url !== url) {
    return false
  }

  return true
}

function isTransientTransportReadFailure(error, options = {}) {
  const { url = null } = options
  const message = error?.message || String(error)
  const transientSignals = ['ECONNRESET', 'ETIMEDOUT', 'ECONNABORTED', 'socket hang up', 'EAI_AGAIN']

  if (!transientSignals.some((signal) => message.includes(signal))) {
    return false
  }

  if (!message.includes('apiRequestContext.get')) {
    return false
  }

  if (!url) {
    return true
  }

  const absoluteUrl = `${backendUrl}${url}`
  return message.includes(url) || message.includes(absoluteUrl)
}

function isTransientGetReadFailure(error, options = {}) {
  return isTransientApiReadFailure(error, options) || isTransientTransportReadFailure(error, options)
}

async function readJsonGetBestEffort(api, url, context = {}, requestOptions = {}) {
  try {
    return {
      payload: await readJson(await api.get(url, requestOptions), {
        method: 'GET',
        url,
        ...context,
      }),
      error: null,
    }
  } catch (error) {
    if (!isTransientGetReadFailure(error, { url })) {
      throw error
    }
    return {
      payload: null,
      error: error?.message || String(error),
    }
  }
}

async function readApiDiagnosticWithOrigin(api, url, origin, context = {}) {
  try {
    const response = await api.get(url, {
      timeout: context.timeout ?? 8_000,
      headers: origin ? { Origin: origin } : undefined,
    })
    const responseText = await response.text()
    let payload = null
    try {
      payload = responseText ? JSON.parse(responseText) : null
    } catch {
      payload = null
    }
    const responseHeaders = safeResponseHeaders(response)
    return {
      method: 'GET',
      url,
      status: typeof response.status === 'function' ? response.status() : null,
      ok: typeof response.ok === 'function' ? response.ok() : null,
      requestId: responseHeaders['x-request-id'] || payload?.requestId || null,
      accessControlAllowOrigin: responseHeaders['access-control-allow-origin'] || null,
      accessControlAllowCredentials: responseHeaders['access-control-allow-credentials'] || null,
      vary: responseHeaders['vary'] || null,
      contentType: responseHeaders['content-type'] || null,
      bodyPreview: payload ?? responseText.slice(0, 1_200),
      requestOrigin: origin || null,
    }
  } catch (error) {
    return {
      method: 'GET',
      url,
      requestOrigin: origin || null,
      error: error?.message || String(error),
    }
  }
}

function replayQueueLookupUrl(externalOrderId) {
  return `/api/integrations/orders/replay-queue?externalOrderId=${encodeURIComponent(externalOrderId)}`
}

function recentOrdersLookupUrl(externalOrderId) {
  return `/api/orders/recent?externalOrderId=${encodeURIComponent(externalOrderId)}`
}

function safeResponseHeaders(response) {
  try {
    return typeof response?.headers === 'function' ? response.headers() || {} : {}
  } catch {
    return {}
  }
}

function safeResponseMethod(response, context = {}) {
  if (context.method) {
    return context.method
  }
  try {
    if (typeof response?.request === 'function') {
      const request = response.request()
      if (request && typeof request.method === 'function') {
        return request.method()
      }
    }
  } catch {
    return 'UNKNOWN'
  }
  return 'UNKNOWN'
}

function safeResponseUrl(response, context = {}) {
  if (context.url) {
    return context.url
  }
  try {
    return typeof response?.url === 'function' ? response.url() || 'UNKNOWN' : 'UNKNOWN'
  } catch {
    return 'UNKNOWN'
  }
}

async function loginViaUi(page, credentials, options = {}) {
  const { requireDashboardSnapshot = false } = options
  await page.goto('/sign-in')
  const signInCard = await expectSignInShellReady(page)
  await waitForSignInReady(signInCard)
  await fillSignInForm(signInCard, credentials, credentials.password)
  await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  if (requireDashboardSnapshot) {
    await waitForDashboardSnapshotReady(page)
  }
}

async function signOutViaUi(page) {
  const signOutButton = page.getByRole('button', { name: 'Sign Out' }).first()
  if (await signOutButton.isVisible()) {
    await signOutButton.click()
    await expectSignInShellReady(page)
  }
}

async function expectSignInShellReady(page) {
  await expect(page).toHaveURL(/\/sign-in(?:$|[?#])/)
  const signInCard = page.locator('.public-signin-card')
  await expect(signInCard).toBeVisible()
  await expect(page.getByRole('heading', { name: signInHeadingPattern }).first()).toBeVisible()
  return signInCard
}

async function fillSignInForm(signInCard, credentials, password) {
  const tenantField = await resolveTenantField(signInCard)
  const usernameField = await resolveUsernameField(signInCard)
  const passwordField = await resolvePasswordField(signInCard)
  const submitButton = signInCard.getByRole('button', { name: 'Enter Platform' })

  let lastError = null
  for (let attempt = 0; attempt < 3; attempt += 1) {
    try {
      await tenantField.fill(credentials.tenantCode)
      await expect(tenantField).toHaveValue(credentials.tenantCode)

      await usernameField.fill(credentials.username)
      await expect(usernameField).toHaveValue(credentials.username)

      await passwordField.fill(password)
      await expect(passwordField).toHaveValue(password)
      await expect(submitButton).toBeEnabled()
      return
    } catch (error) {
      lastError = error
    }
  }

  throw lastError
}

async function waitForSignInReady(signInCard) {
  await expect(await resolveTenantField(signInCard)).toBeEnabled()
  await expect(await resolveUsernameField(signInCard)).toBeEnabled()
  await expect(await resolvePasswordField(signInCard)).toBeEnabled()
}

async function resolveFirstAvailable(candidates, description) {
  for (const candidate of candidates) {
    const field = candidate.first()
    if (await field.count()) {
      return field
    }
  }

  throw new Error(`Unable to locate the ${description} on the sign-in form.`)
}

async function resolveTenantField(signInCard) {
  return resolveFirstAvailable([
    signInCard.getByRole('combobox', { name: /Company workspace code/i }),
    signInCard.getByLabel(/Company workspace code/i),
    signInCard.locator('input[list="tenant-workspace-options"]'),
    signInCard.locator('input[name="tenantCode"], input#tenant-code, input[autocomplete="organization"]'),
  ], 'workspace code input')
}

async function resolveUsernameField(signInCard) {
  return resolveFirstAvailable([
    signInCard.getByRole('textbox', { name: /^Username\b/i }),
    signInCard.getByLabel(/^Username\b/i),
    signInCard.locator('input[name="username"], input[autocomplete="username"]'),
  ], 'username input')
}

async function resolvePasswordField(signInCard) {
  return resolveFirstAvailable([
    signInCard.locator('input[type="password"]'),
    signInCard.getByLabel(/^Password\b/i),
    signInCard.locator('input[name="password"], input[autocomplete="current-password"]'),
  ], 'password input')
}

async function expectSignInErrorAndRecovery(signInCard, message) {
  await expect(signInCard.getByText(message)).toBeVisible({ timeout: 15_000 })
  await waitForSignInReady(signInCard)
}

async function navigateWithinApp(page, route) {
  await page.evaluate((nextRoute) => {
    window.history.pushState({}, '', nextRoute)
    window.dispatchEvent(new PopStateEvent('popstate'))
  }, route)
}

async function expectNoFatalUiErrors(page) {
  const fatalErrors = page.locator('.error-text:visible').filter({
    hasText: /Snapshot load issue:|Invalid operator credentials\.|Request failed|Failed to|Unable to|Unexpected|Forbidden|Access denied/i,
  })
  await expect(fatalErrors).toHaveCount(0)
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function metricCard(page, label) {
  return page.locator('.summary-card.metric-card').filter({
    has: page.locator('.summary-label', { hasText: new RegExp(`^${escapeRegExp(label)}$`) }),
  }).first()
}

async function summaryCardValue(page, label) {
  const card = metricCard(page, label)
  await expect(card).toBeVisible()
  const value = await card.locator('.summary-value').textContent()
  return Number.parseInt((value || '').trim(), 10)
}

async function waitForNumericSummaryCard(page, label) {
  let numericValue = Number.NaN
  await expect.poll(async () => {
    numericValue = await summaryCardValue(page, label)
    return Number.isFinite(numericValue)
  }, {
    timeout: 30_000,
    message: `Expected the ${label} summary card to resolve to a numeric value.`,
  }).toBeTruthy()
  return numericValue
}

async function refreshWorkspace(page) {
  const refreshButton = page.getByRole('button', { name: 'Refresh' })
  if (await refreshButton.isVisible().catch(() => false) && await refreshButton.isEnabled().catch(() => false)) {
    await refreshButton.click()
  }
}

async function activateSelectableButton(buttonLocator) {
  await expect(buttonLocator).toBeVisible()
  await buttonLocator.scrollIntoViewIfNeeded()
  await buttonLocator.focus()
  await expect(buttonLocator).toBeFocused()
  await buttonLocator.press('Enter')
}

async function waitForDashboardSnapshotReady(page, options = {}) {
  const { timeoutMs = 90_000, refreshAfterMs = 25_000, refreshIntervalMs = 20_000 } = options
  const snapshotTimestamp = page.locator('#workspace-trust-rail .muted-text').filter({ hasText: /^(Snapshot |Last successful snapshot )/ }).first()
  const snapshotLoadError = page.locator('.error-text:visible').filter({ hasText: /Snapshot load issue:/ }).first()
  const startedAt = Date.now()
  let lastRefreshAt = 0

  await expect.poll(async () => {
    if (await snapshotLoadError.isVisible().catch(() => false)) {
      return 'error'
    }
    if (await snapshotTimestamp.isVisible().catch(() => false)) {
      return 'ready'
    }
    if (
      Date.now() - startedAt >= refreshAfterMs
      && Date.now() - lastRefreshAt >= refreshIntervalMs
    ) {
      lastRefreshAt = Date.now()
      await refreshWorkspace(page)
    }
    return 'waiting'
  }, {
    timeout: timeoutMs,
    message: 'Expected the hosted dashboard to load a real authenticated snapshot before continuing.',
  }).toBe('ready')
}

async function readRealtimeDiagnostics(page) {
  return page.evaluate(async () => {
    const textOrEmpty = (selector) => {
      const element = globalThis.document?.querySelector?.(selector)
      return element?.textContent?.trim?.() || ''
    }

    const debugState = globalThis.__SYNAPSE_REALTIME_DEBUG__ || null
    const runtimeConfig = globalThis.__SYNAPSE_RUNTIME_CONFIG__ || {}
    const apiBaseUrl = runtimeConfig.apiUrl || ''
    const realtimeBaseUrl = runtimeConfig.wsUrl || ''
    let authSession = null
    let wsInfo = null

    if (apiBaseUrl) {
      try {
        const response = await fetch(`${apiBaseUrl}/api/auth/session`, {
          credentials: 'include',
        })
        authSession = {
          status: response.status,
          payload: await response.json().catch(() => null),
        }
      } catch (error) {
        authSession = {
          error: error?.message || String(error),
        }
      }

      try {
        const wsInfoResponse = await fetch(`${apiBaseUrl}/ws/info?t=${Date.now()}`, {
          credentials: 'include',
        })
        wsInfo = {
          status: wsInfoResponse.status,
          payload: await wsInfoResponse.json().catch(() => null),
        }
      } catch (error) {
        wsInfo = {
          error: error?.message || String(error),
        }
      }
    }

    return {
      pageUrl: globalThis.location?.href || '',
      realtimeBaseUrl,
      connectionBanner: textOrEmpty('#workspace-trust-rail .utility-state'),
      connectionSummary: textOrEmpty('#workspace-trust-rail strong'),
      snapshotStatus: textOrEmpty('#workspace-trust-rail .muted-text'),
      topbarStatus: textOrEmpty('.workspace-status-strip .workspace-status-pill.status-live, .workspace-status-strip .workspace-status-pill.status-connecting, .workspace-status-strip .workspace-status-pill.status-reconnecting, .workspace-status-strip .workspace-status-pill.status-degraded'),
      debugState,
      authSession,
      wsInfo,
    }
  })
}

async function waitForRealtimeConnectionLive(page, options = {}) {
  const { timeoutMs = 90_000 } = options
  await waitForDashboardSnapshotReady(page, { timeoutMs })

  const liveIndicators = [
    page.getByText('Live system').first(),
    page.getByText('Realtime live').first(),
    page.locator('.utility-state.utility-live').first(),
  ]
  const snapshotLoadError = page.locator('.error-text:visible').filter({ hasText: /Snapshot load issue:/ }).first()
  const startedAt = Date.now()

  while (Date.now() - startedAt < timeoutMs) {
    if (await snapshotLoadError.isVisible().catch(() => false)) {
      const diagnostics = await readRealtimeDiagnostics(page)
      throw new Error(`Hosted dashboard surfaced a snapshot load issue while waiting for realtime live state. Diagnostics: ${JSON.stringify(diagnostics)}`)
    }

    for (const indicator of liveIndicators) {
      if (await indicator.isVisible().catch(() => false)) {
        return
      }
    }

    await page.waitForTimeout(1_000)
  }

  const diagnostics = await readRealtimeDiagnostics(page)
  throw new Error(`Expected the hosted dashboard to report a live realtime connection before websocket proof mutates backend state. Diagnostics: ${JSON.stringify(diagnostics)}`)
}

async function findVisibleIntegrationConnector(page, connectors) {
  for (const connector of connectors) {
    if (!connector?.displayName) {
      continue
    }
    const button = page.locator('button.system-select-card').filter({ hasText: connector.displayName }).first()
    if (await button.isVisible().catch(() => false)) {
      return { connector, button }
    }
  }
  return null
}

async function waitForScenarioHistoryCard(page, scenarioTitle) {
  const scenarioCard = page.locator('.approval-board').getByRole('button', {
    name: new RegExp(escapeRegExp(scenarioTitle), 'i'),
  }).first()

  await expect.poll(async () => {
    await refreshWorkspace(page)
    return await scenarioCard.isVisible().catch(() => false)
  }, {
    timeout: 30_000,
    message: `Expected scenario history to render ${scenarioTitle} in the approval board.`,
  }).toBe(true)

  return scenarioCard
}

async function triggerUiAuthRateLimit(page, signInCard, credentials) {
  const invalidMessage = 'Invalid operator credentials.'
  const rateLimitMessage = 'Authentication rate limit exceeded. Wait before attempting another sign-in.'
  let verifiedInvalidMessage = false
  let lastRateLimitHeaders = null

  for (let attempt = 1; attempt <= authRateLimitAttemptBudget; attempt += 1) {
    await fillSignInForm(signInCard, credentials, 'wrong-rate-limit')
    const submitButton = signInCard.getByRole('button', { name: 'Enter Platform' })
    const responsePromise = page.waitForResponse((response) => (
      response.request().method() === 'POST'
        && /\/api\/auth\/session\/login$/i.test(response.url())
    ), { timeout: 20_000 })

    await submitButton.click()
    const response = await responsePromise
    lastRateLimitHeaders = {
      attempt,
      status: response.status(),
      limit: response.headers()['x-synapse-ratelimit-limit'] || '',
      remaining: response.headers()['x-synapse-ratelimit-remaining'] || '',
      resetAfterSeconds: response.headers()['x-synapse-ratelimit-reset-after-seconds'] || '',
      retryAfter: response.headers()['retry-after'] || '',
      requestId: response.headers()['x-request-id'] || '',
    }

    if (response.status() === 429) {
      await expectSignInErrorAndRecovery(signInCard, rateLimitMessage)
      await writeHostedProofState({
        authRateLimitTriggeredAt: Date.now(),
        authRateLimitCooldownUntil: Date.now() + authRateLimitWindowMs + authRateLimitCooldownBufferMs,
      })
      return
    }

    if (response.status() !== 401) {
      const payload = await response.json().catch(() => null)
      throw new Error(payload?.message || `Expected auth warm-up attempts to return 401 or 429, but received ${response.status()}.`)
    }

    const payload = await response.json().catch(() => null)
    if (payload?.message && payload.message !== invalidMessage) {
      throw new Error(`Expected invalid sign-in attempts to return the safe auth message, but received: ${payload.message}`)
    }

    if (!verifiedInvalidMessage) {
      await expectSignInErrorAndRecovery(signInCard, invalidMessage)
      verifiedInvalidMessage = true
    } else {
      await waitForSignInReady(signInCard)
    }
  }

  throw new Error(`Expected repeated real browser sign-in attempts to reach the hosted auth rate-limit threshold within ${authRateLimitAttemptBudget} tries. Last limiter headers: ${JSON.stringify(lastRateLimitHeaders)}`)
}

function ensurePageDiagnostics(page) {
  if (page.__synapsePageDiagnostics) {
    return page.__synapsePageDiagnostics
  }

  const diagnostics = {
    consoleErrors: [],
    failedRequests: [],
    lastApiResponse: null,
  }

  page.on('console', (message) => {
    if (message.type() !== 'error') {
      return
    }
    diagnostics.consoleErrors.push({
      type: message.type(),
      text: message.text(),
      location: message.location(),
    })
    diagnostics.consoleErrors = diagnostics.consoleErrors.slice(-10)
  })

  page.on('requestfailed', (request) => {
    diagnostics.failedRequests.push({
      method: request.method(),
      url: request.url(),
      failure: request.failure()?.errorText || 'unknown failure',
    })
    diagnostics.failedRequests = diagnostics.failedRequests.slice(-10)
  })

  page.on('response', async (response) => {
    if (!/\/api\//i.test(response.url())) {
      return
    }
    let bodyPreview = ''
    try {
      bodyPreview = (await response.text()).replace(/\s+/g, ' ').trim().slice(0, 500)
    } catch {
      bodyPreview = ''
    }
    diagnostics.lastApiResponse = {
      method: response.request().method(),
      url: response.url(),
      status: response.status(),
      requestId: response.headers()['x-request-id'] || '',
      contentType: response.headers()['content-type'] || '',
      bodyPreview,
    }
  })

  page.__synapsePageDiagnostics = diagnostics
  return diagnostics
}

async function readReplayOutcome(api, externalOrderId) {
  const replayQueueUrl = replayQueueLookupUrl(externalOrderId)
  let replayQueueError = null
  try {
    const replayQueue = await readJson(await api.get(replayQueueUrl), {
      method: 'GET',
      url: replayQueueUrl,
      requestPayload: {
        externalOrderId,
      },
      note: 'Replay queue lookup while verifying hosted replay fixture.',
    })
    const replayRecord = replayQueue.find((record) => record.externalOrderId === externalOrderId)
    if (replayRecord) {
      return { state: 'queued', status: replayRecord.status, record: replayRecord }
    }
  } catch (error) {
    if (!isTransientGetReadFailure(error, { url: replayQueueUrl })) {
      throw error
    }
    replayQueueError = error?.message || String(error)
  }

  const recentOrdersUrl = recentOrdersLookupUrl(externalOrderId)
  let recentOrdersError = null
  try {
    const recentOrders = await readJson(await api.get(recentOrdersUrl), {
      method: 'GET',
      url: recentOrdersUrl,
      requestPayload: {
        externalOrderId,
      },
      note: 'Recent orders lookup while verifying hosted replay fixture.',
    })
    if (recentOrders.some((order) => order.externalOrderId === externalOrderId)) {
      return { state: 'replayed' }
    }
  } catch (error) {
    if (!isTransientGetReadFailure(error, { url: recentOrdersUrl })) {
      throw error
    }
    recentOrdersError = error?.message || String(error)
  }

  if (replayQueueError || recentOrdersError) {
    return {
      state: 'transient-error',
      replayQueueError,
      recentOrdersError,
    }
  }

  return { state: 'missing' }
}

async function readDashboardSnapshot(api, note = 'Dashboard snapshot lookup during hosted proof verification.') {
  return readJson(await api.get('/api/dashboard/snapshot'), {
    method: 'GET',
    url: '/api/dashboard/snapshot',
    note,
  })
}

async function readDashboardSnapshotBestEffort(api, options = {}) {
  const {
    timeout = 5_000,
    note = 'Best-effort dashboard snapshot lookup during hosted proof verification.',
    requestPayload = null,
  } = options

  try {
    const snapshot = await readJson(await api.get('/api/dashboard/snapshot', { timeout }), {
      method: 'GET',
      url: '/api/dashboard/snapshot',
      requestPayload,
      note,
    })
    return {
      snapshot,
      snapshotError: null,
    }
  } catch (error) {
    return {
      snapshot: null,
      snapshotError: error?.message || String(error),
    }
  }
}

async function waitForReplayQueueCoverage(api, externalOrderId, sourceSystem, message) {
  let latestCoverage = {
    replayQueueContainsRecord: false,
    snapshotContainsRecord: false,
    replayRecord: null,
    snapshotReplayRecord: null,
    replayQueueError: null,
    snapshotError: null,
  }

  const startedAt = Date.now()
  while (Date.now() - startedAt < 30_000) {
    try {
      const replayQueueUrl = replayQueueLookupUrl(externalOrderId)
      const replayQueue = await readJson(await api.get(replayQueueUrl), {
        method: 'GET',
        url: replayQueueUrl,
        requestPayload: { externalOrderId, sourceSystem },
        note: 'Hosted replay queue readback verification.',
      })

      const replayRecord = Array.isArray(replayQueue)
        ? replayQueue.find((record) => record.externalOrderId === externalOrderId)
        : null

      latestCoverage = {
        ...latestCoverage,
        replayQueueContainsRecord: Boolean(replayRecord),
        replayRecord,
        replayQueueCount: Array.isArray(replayQueue) ? replayQueue.length : null,
        replayQueuePreview: Array.isArray(replayQueue)
          ? replayQueue.slice(0, 12).map((record) => ({
              externalOrderId: record.externalOrderId,
              sourceSystem: record.sourceSystem,
              status: record.status,
            }))
          : [],
        replayQueueError: null,
      }

      if (replayRecord) {
        const { snapshot, snapshotError } = await readDashboardSnapshotBestEffort(api, {
          note: 'Best-effort replay queue snapshot verification.',
          requestPayload: { externalOrderId, sourceSystem },
        })
        const snapshotReplayRecord = Array.isArray(snapshot?.integrationReplayQueue)
          ? snapshot.integrationReplayQueue.find((record) => record.externalOrderId === externalOrderId)
          : null
        latestCoverage = {
          ...latestCoverage,
          snapshotContainsRecord: Boolean(snapshotReplayRecord),
          snapshotReplayRecord,
          snapshotReplayQueueCount: Array.isArray(snapshot?.integrationReplayQueue) ? snapshot.integrationReplayQueue.length : null,
          snapshotError,
        }
        return latestCoverage
      }
    } catch (error) {
      latestCoverage = {
        ...latestCoverage,
        replayQueueError: error?.message || String(error),
      }
    }

    await pageWait(500)
  }

  throw new Error(`${message} Diagnostics: ${JSON.stringify(latestCoverage)}`)
}

function describeReplayOutcome(replayOutcome) {
  if (!replayOutcome || replayOutcome.state === 'missing') {
    return 'missing'
  }

  if (replayOutcome.state === 'transient-error') {
    return 'transient-error'
  }

  if (replayOutcome.state === 'replayed') {
    return 'replayed'
  }

  const nextEligibleAt = replayOutcome.record?.nextEligibleAt
  if (nextEligibleAt && Date.parse(nextEligibleAt) > Date.now()) {
    return `queued:${replayOutcome.status}:waiting`
  }

  return `queued:${replayOutcome.status}`
}

async function waitForReplayResolution(api, externalOrderId, timeout, message) {
  let lastReplayOutcome = null
  const replayStatesSeen = []
  try {
    await expect.poll(async () => {
      const replayOutcome = await readReplayOutcome(api, externalOrderId)
      lastReplayOutcome = replayOutcome
      const describedOutcome = replayOutcome.state === 'queued'
        ? `${replayOutcome.state}:${replayOutcome.status}`
        : replayOutcome.state
      if (!replayStatesSeen.includes(describedOutcome)) {
        replayStatesSeen.push(describedOutcome)
      }
      return describedOutcome === 'transient-error' ? 'waiting' : describedOutcome
    }, {
      timeout,
      message,
    }).toBe('replayed')
  } catch (error) {
    throw new Error(`${message} Diagnostics: ${JSON.stringify({
      externalOrderId,
      lastReplayOutcome,
      replayStatesSeen,
    })} Original error: ${error?.message || String(error)}`)
  }
}

async function readReplayPageDiagnostics(page, replayFixture) {
  const backendConnectors = await readJson(await replayFixture.api.get(`/api/integrations/orders/connectors?sourceSystem=${encodeURIComponent(replayFixture.sourceSystem)}&type=CSV_ORDER_IMPORT`), {
    method: 'GET',
    url: `/api/integrations/orders/connectors?sourceSystem=${encodeURIComponent(replayFixture.sourceSystem)}&type=CSV_ORDER_IMPORT`,
    requestPayload: {
      sourceSystem: replayFixture.sourceSystem,
    },
    note: `Replay connector diagnostics for ${replayFixture.sourceSystem}.`,
  })
  const backendReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  const exactReplayAction = await readExactReplayActionState(page, replayFixture)

  const pageDiagnostics = await page.evaluate(async ({ externalOrderId, sourceSystem }) => {
    const textOrEmpty = (selector) => {
      const element = globalThis.document?.querySelector?.(selector)
      return element?.textContent?.trim?.() || ''
    }

    const replayCards = [...(globalThis.document?.querySelectorAll?.('.signal-list-item.selectable-card') || [])]
    const replayRow = replayCards.find((card) => card.textContent?.includes?.(externalOrderId)) || null
    const replayDetail = [...(globalThis.document?.querySelectorAll?.('.section-card') || [])]
      .find((card) => card.textContent?.includes?.('Recovery detail')) || null
    const exactReplayDetail = [...(globalThis.document?.querySelectorAll?.('.section-card') || [])]
      .find((card) => card.textContent?.includes?.('Recovery detail') && card.textContent?.includes?.(externalOrderId)) || null
    const replayButton = [...(replayDetail?.querySelectorAll?.('button') || [])]
      .find((button) => button.textContent?.trim?.() === 'Replay Into Live Flow') || null
    const exactReplayButton = [...(exactReplayDetail?.querySelectorAll?.('button') || [])]
      .find((button) => button.textContent?.trim?.() === 'Replay Into Live Flow') || null
    const replayMutedLines = [...(replayDetail?.querySelectorAll?.('.muted-text') || [])]
      .map((element) => element.textContent?.trim?.())
      .filter(Boolean)
    const exactReplayMutedLines = [...(exactReplayDetail?.querySelectorAll?.('.muted-text') || [])]
      .map((element) => element.textContent?.trim?.())
      .filter(Boolean)
    const runtimeConfig = globalThis.__SYNAPSE_RUNTIME_CONFIG__ || {}
    const apiBaseUrl = runtimeConfig.apiUrl || ''

    let authSession = null
    let snapshotConnector = null
    let snapshotReplayRecord = null

    if (apiBaseUrl) {
      try {
        const sessionResponse = await fetch(`${apiBaseUrl}/api/auth/session`, {
          credentials: 'include',
        })
        authSession = {
          status: sessionResponse.status,
          payload: await sessionResponse.json().catch(() => null),
        }
      } catch (error) {
        authSession = {
          error: error?.message || String(error),
        }
      }

      try {
        const snapshotResponse = await fetch(`${apiBaseUrl}/api/dashboard/snapshot`, {
          credentials: 'include',
        })
        const snapshotPayload = await snapshotResponse.json().catch(() => null)
        snapshotConnector = snapshotPayload?.integrationConnectors?.find?.((connector) => (
          connector.sourceSystem === sourceSystem && connector.type === 'CSV_ORDER_IMPORT'
        )) || null
        snapshotReplayRecord = snapshotPayload?.integrationReplayQueue?.find?.((record) => (
          record.externalOrderId === externalOrderId
        )) || null
      } catch (error) {
        snapshotConnector = {
          error: error?.message || String(error),
        }
      }
    }

    return {
      pageUrl: globalThis.location?.href || '',
      connectionState: textOrEmpty('.workspace-status-strip .workspace-status-pill.status-live, .workspace-status-strip .workspace-status-pill.status-connecting, .workspace-status-strip .workspace-status-pill.status-reconnecting, .workspace-status-strip .workspace-status-pill.status-degraded'),
      replayRowText: replayRow?.textContent?.trim?.() || '',
      replayButtonDisabled: replayButton?.disabled ?? null,
      replayButtonAriaDisabled: replayButton?.getAttribute?.('aria-disabled') || '',
      exactReplayDetailText: exactReplayDetail?.textContent?.trim?.() || '',
      exactReplayButtonDisabled: exactReplayButton?.disabled ?? null,
      exactReplayButtonAriaDisabled: exactReplayButton?.getAttribute?.('aria-disabled') || '',
      replayMutedLines,
      exactReplayMutedLines,
      authSession,
      snapshotConnector,
      snapshotReplayRecord,
      realtimeDebug: globalThis.__SYNAPSE_REALTIME_DEBUG__ || null,
    }
  }, {
    externalOrderId: replayFixture.externalOrderId,
    sourceSystem: replayFixture.sourceSystem,
  })

  return {
    backendReplayOutcome,
    backendConnector: backendConnectors.find((connector) => (
      connector.sourceSystem === replayFixture.sourceSystem && connector.type === 'CSV_ORDER_IMPORT'
    )) || null,
    page: pageDiagnostics,
    exactReplayAction,
  }
}

async function readExactReplayActionState(page, replayFixture) {
  return page.evaluate(({ externalOrderId, sourceSystem }) => {
    const normalizeText = (value) => value?.replace?.(/\s+/g, ' ')?.trim?.() || ''
    const replayRows = [...(globalThis.document?.querySelectorAll?.('.signal-list-item.selectable-card') || [])]
    const exactReplayRow = replayRows.find((row) => (
      row.textContent?.includes?.(externalOrderId) && row.textContent?.includes?.(sourceSystem)
    )) || replayRows.find((row) => row.textContent?.includes?.(externalOrderId)) || null
    const replayDetails = [...(globalThis.document?.querySelectorAll?.('.section-card') || [])]
    const exactReplayDetail = replayDetails.find((card) => (
      card.textContent?.includes?.('Recovery detail')
        && card.textContent?.includes?.(externalOrderId)
        && card.textContent?.includes?.(sourceSystem)
    )) || replayDetails.find((card) => (
      card.textContent?.includes?.('Recovery detail') && card.textContent?.includes?.(externalOrderId)
    )) || replayDetails.find((card) => card.textContent?.includes?.('Recovery detail')) || null
    const exactReplayButton = [...(exactReplayDetail?.querySelectorAll?.('button') || [])]
      .find((button) => normalizeText(button.textContent) === 'Replay Into Live Flow') || null
    const buttonStyle = exactReplayButton ? globalThis.getComputedStyle(exactReplayButton) : null
    const buttonRect = exactReplayButton?.getBoundingClientRect?.() || null
    const buttonCenter = buttonRect && buttonRect.width > 0 && buttonRect.height > 0
      ? {
          x: buttonRect.left + (buttonRect.width / 2),
          y: buttonRect.top + (buttonRect.height / 2),
        }
      : null
    const overlayElement = buttonCenter
      ? globalThis.document?.elementFromPoint?.(buttonCenter.x, buttonCenter.y) || null
      : null
    const overlayStyle = overlayElement ? globalThis.getComputedStyle(overlayElement) : null
    const overlayChain = []
    let currentOverlay = overlayElement
    while (currentOverlay && overlayChain.length < 4) {
      overlayChain.push({
        tagName: currentOverlay.tagName?.toLowerCase?.() || '',
        className: typeof currentOverlay.className === 'string' ? currentOverlay.className : '',
        text: normalizeText(currentOverlay.textContent).slice(0, 160),
      })
      currentOverlay = currentOverlay.parentElement
    }
    const detailText = normalizeText(exactReplayDetail?.textContent)
    const buttonDisabledAttribute = exactReplayButton?.getAttribute?.('disabled')
    const buttonAriaDisabled = exactReplayButton?.getAttribute?.('aria-disabled') || ''
    const buttonVisible = Boolean(
      exactReplayButton
        && buttonRect
        && buttonRect.width > 0
        && buttonRect.height > 0
        && buttonStyle
        && buttonStyle.display !== 'none'
        && buttonStyle.visibility !== 'hidden'
        && buttonStyle.opacity !== '0'
    )
    const domEnabled = Boolean(
      exactReplayButton
        && exactReplayButton.disabled === false
        && buttonDisabledAttribute == null
        && buttonAriaDisabled !== 'true'
    )

    return {
      rowFound: Boolean(exactReplayRow),
      rowText: normalizeText(exactReplayRow?.textContent),
      detailFound: Boolean(exactReplayDetail),
      detailText,
      detailHtml: exactReplayDetail?.outerHTML?.slice?.(0, 2_000) || '',
      buttonFound: Boolean(exactReplayButton),
      buttonText: normalizeText(exactReplayButton?.textContent),
      buttonDisabled: exactReplayButton?.disabled ?? null,
      buttonDisabledAttribute,
      buttonAriaDisabled,
      buttonConnected: exactReplayButton?.isConnected ?? null,
      buttonVisible,
      buttonPointerEvents: buttonStyle?.pointerEvents || '',
      buttonDisplay: buttonStyle?.display || '',
      buttonVisibility: buttonStyle?.visibility || '',
      buttonOpacity: buttonStyle?.opacity || '',
      buttonBoundingBox: buttonRect ? {
        x: buttonRect.x,
        y: buttonRect.y,
        width: buttonRect.width,
        height: buttonRect.height,
      } : null,
      buttonCenter,
      overlay: overlayElement ? {
        tagName: overlayElement.tagName?.toLowerCase?.() || '',
        className: typeof overlayElement.className === 'string' ? overlayElement.className : '',
        text: normalizeText(overlayElement.textContent).slice(0, 200),
        pointerEvents: overlayStyle?.pointerEvents || '',
        display: overlayStyle?.display || '',
        visibility: overlayStyle?.visibility || '',
        opacity: overlayStyle?.opacity || '',
        html: overlayElement.outerHTML?.slice?.(0, 1_000) || '',
      } : null,
      overlayChain,
      domEnabled,
    }
  }, {
    externalOrderId: replayFixture.externalOrderId,
    sourceSystem: replayFixture.sourceSystem,
  })
}

async function focusReplayRecord(page, replayFixture) {
  return page.evaluate(({ externalOrderId, sourceSystem }) => {
    const replayRows = [...(globalThis.document?.querySelectorAll?.('.signal-list-item.selectable-card') || [])]
    const exactReplayRow = replayRows.find((row) => (
      row.textContent?.includes?.(externalOrderId) && row.textContent?.includes?.(sourceSystem)
    )) || replayRows.find((row) => row.textContent?.includes?.(externalOrderId)) || null
    if (!exactReplayRow) {
      return {
        clicked: false,
        rowText: '',
      }
    }
    exactReplayRow.scrollIntoView({ block: 'center', inline: 'nearest' })
    exactReplayRow.click()
    return {
      clicked: true,
      rowText: exactReplayRow.textContent?.trim?.() || '',
    }
  }, {
    externalOrderId: replayFixture.externalOrderId,
    sourceSystem: replayFixture.sourceSystem,
  })
}

async function waitForReplayButtonReady(page, replayFixture) {
  const replayQueueRecord = page.locator('.signal-list-item.selectable-card').filter({
    hasText: replayFixture.externalOrderId,
  }).first()

  const startedAt = Date.now()
  let lastRefreshAt = 0

  while (Date.now() - startedAt < 12_000) {
    if (Date.now() - lastRefreshAt >= 2_500) {
      lastRefreshAt = Date.now()
      await refreshWorkspace(page)
    }

    if (await replayQueueRecord.isVisible().catch(() => false)) {
      await replayQueueRecord.click().catch(() => {})
    }
    await focusReplayRecord(page, replayFixture).catch(() => {})

    const actionState = await readExactReplayActionState(page, replayFixture)
    if (actionState.domEnabled) {
      return actionState
    }

    await page.waitForTimeout(500)
  }

  const diagnostics = await readReplayPageDiagnostics(page, replayFixture)
  if (diagnostics?.exactReplayAction?.domEnabled) {
    return diagnostics.exactReplayAction
  }
  throw new Error(`Expected Replay Into Live Flow to become enabled after connector ${replayFixture.sourceSystem} was re-enabled and the replay queue refreshed. Diagnostics: ${JSON.stringify(diagnostics)}`)
}

async function clickExactReplayButton(page, replayFixture) {
  const clickResult = await page.evaluate(({ externalOrderId, sourceSystem }) => {
    const normalizeText = (value) => value?.replace?.(/\s+/g, ' ')?.trim?.() || ''
    const replayDetails = [...(globalThis.document?.querySelectorAll?.('.section-card') || [])]
    const exactReplayDetail = replayDetails.find((card) => (
      card.textContent?.includes?.('Recovery detail')
        && card.textContent?.includes?.(externalOrderId)
        && card.textContent?.includes?.(sourceSystem)
    )) || replayDetails.find((card) => (
      card.textContent?.includes?.('Recovery detail') && card.textContent?.includes?.(externalOrderId)
    )) || null
    const exactReplayButton = [...(exactReplayDetail?.querySelectorAll?.('button') || [])]
      .find((button) => normalizeText(button.textContent) === 'Replay Into Live Flow') || null

    if (!exactReplayDetail || !exactReplayButton) {
      return {
        clicked: false,
        reason: 'missing-target',
        detailHtml: exactReplayDetail?.outerHTML?.slice?.(0, 2_000) || '',
        buttonHtml: exactReplayButton?.outerHTML?.slice?.(0, 1_000) || '',
      }
    }

    const disabledAttribute = exactReplayButton.getAttribute('disabled')
    const ariaDisabled = exactReplayButton.getAttribute('aria-disabled') || ''
    if (exactReplayButton.disabled || disabledAttribute != null || ariaDisabled === 'true') {
      return {
        clicked: false,
        reason: 'dom-disabled',
        detailHtml: exactReplayDetail.outerHTML?.slice?.(0, 2_000) || '',
        buttonHtml: exactReplayButton.outerHTML?.slice?.(0, 1_000) || '',
        disabled: exactReplayButton.disabled,
        disabledAttribute,
        ariaDisabled,
      }
    }

    exactReplayButton.scrollIntoView({ block: 'center', inline: 'nearest' })
    exactReplayButton.click()
    return {
      clicked: true,
      buttonHtml: exactReplayButton.outerHTML?.slice?.(0, 1_000) || '',
    }
  }, {
    externalOrderId: replayFixture.externalOrderId,
    sourceSystem: replayFixture.sourceSystem,
  })

  if (!clickResult.clicked) {
    const diagnostics = await readReplayPageDiagnostics(page, replayFixture)
    throw new Error(`Expected to click the exact replay action for ${replayFixture.externalOrderId} after DOM readiness was confirmed. Click result: ${JSON.stringify(clickResult)} Diagnostics: ${JSON.stringify(diagnostics)}`)
  }
}

async function waitForUsersPageReady(page, expectedOperatorName, expectedUserFullName, testInfo) {
  const operatorLane = page.locator('.section-card').filter({ hasText: 'Operator lanes' }).first()
  const userRoster = page.locator('.section-card').filter({ hasText: 'User roster' }).first()
  const pageDiagnostics = ensurePageDiagnostics(page)
  let stuckCheck = 'render operator lanes card'

  try {
    await expect(operatorLane).toBeVisible()
    stuckCheck = 'render user roster card'
    await expect(userRoster).toBeVisible()
    stuckCheck = `show operator ${expectedOperatorName}`
    await expect(operatorLane.getByText(expectedOperatorName).first()).toBeVisible({ timeout: 30_000 })
    stuckCheck = `show user ${expectedUserFullName}`
    await expect(userRoster.getByText(expectedUserFullName).first()).toBeVisible({ timeout: 30_000 })
  } catch (error) {
    let screenshotPath = null
    if (testInfo) {
      screenshotPath = testInfo.outputPath('users-page-timeout.png')
      await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {})
    }

    const lastVisibleHeading = await page.locator('h1, h2').evaluateAll((elements) => (
      elements
        .filter((element) => Boolean(element?.offsetWidth || element?.offsetHeight || element?.getClientRects?.().length))
        .map((element) => element.textContent?.trim?.())
        .filter(Boolean)
        .at(-1) || ''
    )).catch(() => '')
    const pageTextExcerpt = await page.locator('body').textContent()
      .then((text) => text?.replace(/\s+/g, ' ').trim().slice(0, 1_200) || '')
      .catch(() => '')

    throw new Error(`Users page readiness check failed. Diagnostics: ${JSON.stringify({
      stuckCheck,
      currentRoute: page.url(),
      lastVisibleHeading,
      expectedOperatorName,
      expectedUserFullName,
      lastApiResponse: pageDiagnostics.lastApiResponse,
      consoleErrors: pageDiagnostics.consoleErrors,
      failedRequests: pageDiagnostics.failedRequests,
      pageTextExcerpt,
      screenshotPath,
      originalError: error?.message || String(error),
    })}`)
  }
}

async function waitForOrdersPageOrderVisible(page, orderRecord, testInfo, api = null) {
  const pageDiagnostics = ensurePageDiagnostics(page)
  const startedAt = Date.now()
  let lastRefreshAt = 0
  let lastState = null

  while (Date.now() - startedAt < 30_000) {
    if (Date.now() - lastRefreshAt >= 2_500) {
      lastRefreshAt = Date.now()
      await refreshWorkspace(page)
    }

    const orderButton = page.getByRole('button', {
      name: new RegExp(escapeRegExp(orderRecord.externalOrderId), 'i'),
    }).first()
    if (await orderButton.isVisible().catch(() => false)) {
      await activateSelectableButton(orderButton).catch(() => {})
    }

    lastState = await page.evaluate(({ externalOrderId, warehouseCode }) => {
      const normalizeText = (value) => value?.replace?.(/\s+/g, ' ')?.trim?.() || ''
      const orderButtons = [...(globalThis.document?.querySelectorAll?.('button') || [])]
      const exactOrderButton = orderButtons.find((button) => (
        normalizeText(button.textContent).includes(externalOrderId)
      )) || null
      const detailCards = [...(globalThis.document?.querySelectorAll?.('.section-card') || [])]
      const exactDetail = detailCards.find((card) => (
        normalizeText(card.textContent).includes(externalOrderId)
          && normalizeText(card.textContent).includes(warehouseCode)
      )) || detailCards.find((card) => normalizeText(card.textContent).includes(externalOrderId)) || null

      return {
        pageUrl: globalThis.location?.href || '',
        orderButtonFound: Boolean(exactOrderButton),
        orderButtonText: normalizeText(exactOrderButton?.textContent),
        detailText: normalizeText(exactDetail?.textContent),
        detailMatches: Boolean(
          exactDetail
            && normalizeText(exactDetail.textContent).includes(externalOrderId)
            && normalizeText(exactDetail.textContent).includes(warehouseCode)
        ),
        visibleOrderIds: orderButtons
          .map((button) => normalizeText(button.textContent))
          .filter((text) => text.includes('UI-ORD-') || text.includes('ORD-'))
          .slice(0, 20),
      }
    }, {
      externalOrderId: orderRecord.externalOrderId,
      warehouseCode: orderRecord.warehouseCode,
    })

    if (lastState.orderButtonFound && lastState.detailMatches) {
      return lastState
    }

    await page.waitForTimeout(500)
  }

  let screenshotPath = null
  if (testInfo) {
    screenshotPath = testInfo.outputPath('orders-page-timeout.png')
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {})
  }

  const browserOrigin = await page.evaluate(() => globalThis.location?.origin || '').catch(() => '')
  const apiReadbacks = api ? {
    ordersRecent: await readApiDiagnosticWithOrigin(api, recentOrdersLookupUrl(orderRecord.externalOrderId), browserOrigin, { timeout: 8_000 }),
    systemRuntime: await readApiDiagnosticWithOrigin(api, '/api/system/runtime', browserOrigin, { timeout: 8_000 }),
    dashboardSnapshot: await readApiDiagnosticWithOrigin(api, '/api/dashboard/snapshot', browserOrigin, { timeout: 8_000 }),
    authSession: await readApiDiagnosticWithOrigin(api, '/api/auth/session', browserOrigin, { timeout: 8_000 }),
  } : null

  throw new Error(`Orders page failed to render deterministic proof order ${orderRecord.externalOrderId}. Diagnostics: ${JSON.stringify({
    expectedExternalOrderId: orderRecord.externalOrderId,
    expectedWarehouseCode: orderRecord.warehouseCode,
    lastState,
    browserOrigin,
    lastApiResponse: pageDiagnostics.lastApiResponse,
    consoleErrors: pageDiagnostics.consoleErrors,
    failedRequests: pageDiagnostics.failedRequests,
    apiReadbacks,
    screenshotPath,
  })}`)
}

async function waitForAlertPageAlertVisible(page, alertRecord, testInfo) {
  const pageDiagnostics = ensurePageDiagnostics(page)
  const startedAt = Date.now()
  let lastRefreshAt = 0
  let lastState = null

  while (Date.now() - startedAt < 30_000) {
    if (Date.now() - lastRefreshAt >= 2_500) {
      lastRefreshAt = Date.now()
      await refreshWorkspace(page)
    }

    const alertButton = page.locator('#alerts-feed').getByRole('button', {
      name: new RegExp(escapeRegExp(alertRecord.title), 'i'),
    }).first()
    if (await alertButton.isVisible().catch(() => false)) {
      await alertButton.scrollIntoViewIfNeeded().catch(() => {})
      await alertButton.click({ timeout: 1_500 }).catch(async () => {
        await page.evaluate((title) => {
          const normalizeText = (value) => value?.replace?.(/\s+/g, ' ')?.trim?.() || ''
          const alertButtons = [...(globalThis.document?.querySelectorAll?.('#alerts-feed button') || [])]
          const exactAlertButton = alertButtons.find((button) => normalizeText(button.textContent).includes(title))
          exactAlertButton?.click?.()
        }, alertRecord.title).catch(() => {})
      })
    }

    lastState = await page.evaluate(({ title, recommendedAction }) => {
      const normalizeText = (value) => value?.replace?.(/\s+/g, ' ')?.trim?.() || ''
      const alertButtons = [...(globalThis.document?.querySelectorAll?.('#alerts-feed button') || [])]
      const exactAlertButton = alertButtons.find((button) => normalizeText(button.textContent).includes(title)) || null
      const selectedAlert = globalThis.document?.querySelector?.('#alerts-response') || null
      const selectedText = normalizeText(selectedAlert?.textContent)
      return {
        pageUrl: globalThis.location?.href || '',
        alertButtonFound: Boolean(exactAlertButton),
        alertButtonText: normalizeText(exactAlertButton?.textContent),
        selectedText,
        selectedMatches: selectedText.includes(title) && selectedText.includes(recommendedAction),
      }
    }, {
      title: alertRecord.title,
      recommendedAction: alertRecord.recommendedAction,
    })

    if (lastState.alertButtonFound && lastState.selectedMatches) {
      return lastState
    }

    await page.waitForTimeout(500)
  }

  let screenshotPath = null
  if (testInfo) {
    screenshotPath = testInfo.outputPath('alerts-page-timeout.png')
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {})
  }

  throw new Error(`Alerts page failed to render deterministic proof alert ${alertRecord.title}. Diagnostics: ${JSON.stringify({
    expectedTitle: alertRecord.title,
    expectedAction: alertRecord.recommendedAction,
    lastState,
    lastApiResponse: pageDiagnostics.lastApiResponse,
    consoleErrors: pageDiagnostics.consoleErrors,
    failedRequests: pageDiagnostics.failedRequests,
    screenshotPath,
  })}`)
}

async function waitForRecommendationPageVisible(page, recommendationRecord, testInfo) {
  const pageDiagnostics = ensurePageDiagnostics(page)
  const startedAt = Date.now()
  let lastRefreshAt = 0
  let lastState = null

  while (Date.now() - startedAt < 30_000) {
    if (Date.now() - lastRefreshAt >= 2_500) {
      lastRefreshAt = Date.now()
      await refreshWorkspace(page)
    }

    const recommendationButton = page.locator('.recommendation-board').getByRole('button', {
      name: new RegExp(escapeRegExp(recommendationRecord.title), 'i'),
    }).first()
    if (await recommendationButton.isVisible().catch(() => false)) {
      await activateSelectableButton(recommendationButton).catch(() => {})
    }

    lastState = await page.evaluate(({ title, description }) => {
      const normalizeText = (value) => value?.replace?.(/\s+/g, ' ')?.trim?.() || ''
      const recommendationButtons = [...(globalThis.document?.querySelectorAll?.('.recommendation-board button') || [])]
      const exactRecommendationButton = recommendationButtons.find((button) => normalizeText(button.textContent).includes(title)) || null
      const selectedRecommendation = globalThis.document?.querySelector?.('#recommendations-focus') || null
      const selectedText = normalizeText(selectedRecommendation?.textContent)
      return {
        pageUrl: globalThis.location?.href || '',
        recommendationButtonFound: Boolean(exactRecommendationButton),
        recommendationButtonText: normalizeText(exactRecommendationButton?.textContent),
        selectedText,
        selectedMatches: selectedText.includes(title) && selectedText.includes(description),
      }
    }, {
      title: recommendationRecord.title,
      description: recommendationRecord.description,
    })

    if (lastState.recommendationButtonFound && lastState.selectedMatches) {
      return lastState
    }

    await page.waitForTimeout(500)
  }

  let screenshotPath = null
  if (testInfo) {
    screenshotPath = testInfo.outputPath('recommendations-page-timeout.png')
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {})
  }

  throw new Error(`Recommendations page failed to render deterministic proof recommendation ${recommendationRecord.title}. Diagnostics: ${JSON.stringify({
    expectedTitle: recommendationRecord.title,
    expectedDescription: recommendationRecord.description,
    lastState,
    lastApiResponse: pageDiagnostics.lastApiResponse,
    consoleErrors: pageDiagnostics.consoleErrors,
    failedRequests: pageDiagnostics.failedRequests,
    screenshotPath,
  })}`)
}

async function waitForRuntimePageReady(page, runtimeRecord, testInfo) {
  const pageDiagnostics = ensurePageDiagnostics(page)
  const startedAt = Date.now()
  let lastRefreshAt = 0
  let lastState = null
  const expectedReadiness = formatProofCodeLabel(runtimeRecord.readinessState)
  const expectedBrokerMode = formatProofCodeLabel(runtimeRecord.backbone?.realtimeBrokerMode || 'unknown')

  while (Date.now() - startedAt < 30_000) {
    if (Date.now() - lastRefreshAt >= 2_500) {
      lastRefreshAt = Date.now()
      await refreshWorkspace(page)
    }

    lastState = await page.evaluate(({ expectedOverallStatus, expectedReadinessText, expectedBrokerModeText }) => {
      const normalizeText = (value) => value?.replace?.(/\s+/g, ' ')?.trim?.() || ''
      const runtimeHealth = globalThis.document?.querySelector?.('#runtime-health') || null
      const runtimeText = normalizeText(runtimeHealth?.textContent)
      return {
        pageUrl: globalThis.location?.href || '',
        runtimeText,
        matches: runtimeText.includes(expectedOverallStatus)
          && runtimeText.includes(expectedReadinessText)
          && runtimeText.includes(expectedBrokerModeText),
      }
    }, {
      expectedOverallStatus: runtimeRecord.overallStatus,
      expectedReadinessText: expectedReadiness,
      expectedBrokerModeText: expectedBrokerMode,
    })

    if (lastState.matches) {
      return lastState
    }

    await page.waitForTimeout(500)
  }

  let screenshotPath = null
  if (testInfo) {
    screenshotPath = testInfo.outputPath('runtime-page-timeout.png')
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {})
  }

  throw new Error(`Runtime page failed to render deterministic backend runtime posture. Diagnostics: ${JSON.stringify({
    expectedOverallStatus: runtimeRecord.overallStatus,
    expectedReadiness,
    expectedBrokerMode,
    lastState,
    lastApiResponse: pageDiagnostics.lastApiResponse,
    consoleErrors: pageDiagnostics.consoleErrors,
    failedRequests: pageDiagnostics.failedRequests,
    screenshotPath,
  })}`)
}

async function createReplayFixture() {
  const inventoryAdmin = await createApiContext(users.operationsLead)
  const api = await createApiContext(users.integrationLead)
  const suffix = randomUUID().slice(0, 8).toUpperCase()
  const sourceSystem = `ui_replay_${suffix}`.toLowerCase()
  const externalOrderId = `UI-RPL-${suffix}`
  const connectorDisplayName = `UI Replay ${suffix}`
  const inventoryPayload = {
    productSku: proofProductSku,
    warehouseCode: 'WH-NORTH',
    quantityAvailable: 50,
    reorderThreshold: 12,
  }
  const connectorPayload = {
    sourceSystem,
    type: 'CSV_ORDER_IMPORT',
    displayName: connectorDisplayName,
    enabled: false,
    syncMode: 'BATCH_FILE_DROP',
    validationPolicy: 'RELAXED',
    transformationPolicy: 'NORMALIZE_CODES',
    allowDefaultWarehouseFallback: false,
    notes: 'Disposable replay verification connector.',
  }
  const csvPreview = `sourceSystem,externalOrderId,warehouseCode,productSku,quantity,unitPrice\n${sourceSystem},${externalOrderId},WH-NORTH,${proofProductSku},2,88.00\n`

  try {
    await readJson(await inventoryAdmin.post('/api/inventory/update', {
      data: inventoryPayload,
    }), {
      method: 'POST',
      url: '/api/inventory/update',
      requestPayload: inventoryPayload,
      note: 'Hosted replay fixture baseline inventory reset.',
    })

    await readJson(await api.post('/api/integrations/orders/connectors', {
      data: connectorPayload,
    }), {
      method: 'POST',
      url: '/api/integrations/orders/connectors',
      requestPayload: connectorPayload,
      note: 'Hosted replay fixture connector creation.',
    })

    const csvImportResponse = await api.post('/api/integrations/orders/csv-import', {
      multipart: {
        file: {
          name: 'orders.csv',
          mimeType: 'text/csv',
          buffer: Buffer.from(
            csvPreview,
            'utf8',
          ),
        },
        sourceSystem,
      },
    })
    const csvImportPayload = await readJson(csvImportResponse, {
      method: 'POST',
      url: '/api/integrations/orders/csv-import',
      requestFormData: {
        sourceSystem,
        fileName: 'orders.csv',
        mimeType: 'text/csv',
        csvPreview: csvPreview.trim(),
      },
      note: 'Hosted replay fixture disabled-connector CSV import.',
    })
    expect(csvImportPayload.ordersFailed).toBe(1)
    expect(csvImportPayload.failedOrders?.[0]?.externalOrderId).toBe(externalOrderId)
    expect(csvImportPayload.failedOrders?.[0]?.failureCode).toBe('CONNECTOR_DISABLED')

    await expect.poll(async () => {
      const replayOutcome = await readReplayOutcome(api, externalOrderId)
      return describeReplayOutcome(replayOutcome)
    }, {
      timeout: 15_000,
      message: `Expected replay verification record ${externalOrderId} to remain queued for manual recovery before UI replay proof enables live recovery.`,
    }).toBe('queued:PENDING')

    return {
      api,
      sourceSystem,
      externalOrderId,
      enableConnector: async () => {
        const enableConnectorPayload = {
          sourceSystem,
          type: 'CSV_ORDER_IMPORT',
          displayName: connectorDisplayName,
          enabled: true,
          syncMode: 'BATCH_FILE_DROP',
          validationPolicy: 'RELAXED',
          transformationPolicy: 'NORMALIZE_CODES',
          allowDefaultWarehouseFallback: false,
          notes: 'Enabled for replay verification.',
        }
        const enableResponse = await api.post('/api/integrations/orders/connectors', {
          data: enableConnectorPayload,
        })
        const enableResponsePayload = await readJson(enableResponse, {
          method: 'POST',
          url: '/api/integrations/orders/connectors',
          requestPayload: enableConnectorPayload,
          note: 'Hosted replay fixture connector enable before manual replay.',
        })
        const enableResponseDetails = {
          status: typeof enableResponse.status === 'function' ? enableResponse.status() : null,
          requestId: typeof enableResponse.headers === 'function'
            ? enableResponse.headers()['x-request-id'] || enableResponsePayload?.requestId || null
            : enableResponsePayload?.requestId || null,
          responseBody: enableResponsePayload,
          actorUsername: users.integrationLead.username,
          tenantCode: users.integrationLead.tenantCode,
        }

        let lastConnectorCheck = {
          phase: 'connector-readback-not-started',
          sourceSystem,
        }
        let lastReplayCheck = {
          phase: 'replay-readback-not-started',
          externalOrderId,
        }
        let lastSnapshotCheck = {
          phase: 'snapshot-readback-skipped',
          sourceSystem,
          externalOrderId,
        }
        try {
          await expect.poll(async () => {
            const startedAt = Date.now()
            try {
              const connectorsPath = `/api/integrations/orders/connectors?sourceSystem=${encodeURIComponent(sourceSystem)}&type=CSV_ORDER_IMPORT`
              const connectorsResponse = await api.get(connectorsPath, { timeout: 8_000 })
              const connectorsText = await connectorsResponse.text()
              let connectorsPayload = null
              try {
                connectorsPayload = connectorsText ? JSON.parse(connectorsText) : null
              } catch {
                connectorsPayload = null
              }
              const connectorFromList = Array.isArray(connectorsPayload)
                ? connectorsPayload.find((connector) => connector.sourceSystem === sourceSystem && connector.type === 'CSV_ORDER_IMPORT')
                : null
              lastConnectorCheck = {
                phase: 'connector-list',
                durationMs: Date.now() - startedAt,
                path: connectorsPath,
                connectorsStatus: typeof connectorsResponse.status === 'function' ? connectorsResponse.status() : null,
                connectorsRequestId: typeof connectorsResponse.headers === 'function' ? connectorsResponse.headers()['x-request-id'] || connectorsPayload?.requestId || null : null,
                connectorsOk: connectorsResponse.ok(),
                payloadShape: Array.isArray(connectorsPayload) ? 'array' : typeof connectorsPayload,
                payloadCount: Array.isArray(connectorsPayload) ? connectorsPayload.length : null,
                connectorFromList,
                payloadPreview: Array.isArray(connectorsPayload)
                  ? connectorsPayload.map((connector) => ({
                    sourceSystem: connector.sourceSystem,
                    type: connector.type,
                    enabled: connector.enabled,
                    healthStatus: connector.healthStatus,
                    pendingReplayCount: connector.pendingReplayCount,
                  }))
                  : connectorsPayload ?? connectorsText.slice(0, 400),
              }
              if (!connectorsResponse.ok() || !Array.isArray(connectorsPayload)) {
                return false
              }
              return Boolean(connectorFromList?.enabled)
            } catch (error) {
              lastConnectorCheck = {
                phase: 'connector-list',
                durationMs: Date.now() - startedAt,
                error: error?.message || String(error),
              }
              return false
            }
          }, {
            timeout: 35_000,
            message: `Expected replay verification connector ${sourceSystem} to become enabled before manual UI replay.`,
          }).toBe(true)

          try {
            const replayOutcome = await readReplayOutcome(api, externalOrderId)
            lastReplayCheck = {
              phase: 'replay-queue-best-effort',
              replayOutcome,
              describedOutcome: describeReplayOutcome(replayOutcome),
            }
          } catch (error) {
            lastReplayCheck = {
              phase: 'replay-queue-best-effort',
              error: error?.message || String(error),
            }
          }
          const snapshotStartedAt = Date.now()
          const { snapshot: snapshotPayload, snapshotError } = await readDashboardSnapshotBestEffort(api, {
            note: `Best-effort replay snapshot readback for ${externalOrderId} after connector enable.`,
            requestPayload: {
              externalOrderId,
              sourceSystem,
            },
          })
          if (snapshotPayload) {
            const connectorFromSnapshot = Array.isArray(snapshotPayload?.integrationConnectors)
              ? snapshotPayload.integrationConnectors.find((connector) => connector.sourceSystem === sourceSystem && connector.type === 'CSV_ORDER_IMPORT')
              : null
            const replayFromSnapshot = Array.isArray(snapshotPayload?.integrationReplayQueue)
              ? snapshotPayload.integrationReplayQueue.find((record) => record.externalOrderId === externalOrderId)
              : null
            lastSnapshotCheck = {
              phase: 'dashboard-snapshot-best-effort',
              durationMs: Date.now() - snapshotStartedAt,
              snapshotOk: true,
              connectorFromSnapshot,
              replayFromSnapshot,
            }
          } else {
            lastSnapshotCheck = {
              phase: 'dashboard-snapshot-best-effort',
              durationMs: Date.now() - snapshotStartedAt,
              error: snapshotError,
            }
          }
        } catch (error) {
          throw new Error(`Expected replay verification connector ${sourceSystem} to become enabled before manual UI replay. Diagnostics: ${JSON.stringify({
            enableResponseDetails,
            lastConnectorCheck,
            lastReplayCheck,
            lastSnapshotCheck,
          })} Original error: ${error?.message || String(error)}`)
        }
      },
    }
  } catch (error) {
    await api.dispose()
    throw error
  } finally {
    await inventoryAdmin.dispose()
  }
}

async function createScenarioFixture() {
  const api = await createApiContext(users.operationsLead)
  const suffix = randomUUID().slice(0, 8).toUpperCase()
  const title = `UI Scenario ${suffix}`
  const productSku = `SKU-SCN-${suffix}`
  const warehouseCode = 'WH-NORTH'
  const productPayload = {
    sku: productSku,
    name: `Scenario Proof ${suffix}`,
    category: 'Verification',
  }
  const inventoryPayload = {
    productSku,
    warehouseCode,
    quantityAvailable: 40,
    reorderThreshold: 10,
  }
  const scenarioPayload = {
    title,
    requestedBy: users.operationsLead.actorName,
    request: {
      warehouseCode,
      items: [
        {
          productSku,
          quantity: 1,
          unitPrice: 95,
        },
      ],
    },
  }

  try {
    await readJson(await api.post('/api/products', {
      data: productPayload,
    }), {
      method: 'POST',
      url: '/api/products',
      requestPayload: productPayload,
      note: 'Hosted scenario fixture product creation.',
    })

    await readJson(await api.post('/api/inventory/update', {
      data: inventoryPayload,
    }), {
      method: 'POST',
      url: '/api/inventory/update',
      requestPayload: inventoryPayload,
      note: 'Hosted scenario fixture inventory baseline.',
    })

    const payload = await readJson(await api.post('/api/scenarios/save', {
      data: scenarioPayload,
    }), {
      method: 'POST',
      url: '/api/scenarios/save',
      requestPayload: scenarioPayload,
      note: 'Hosted scenario fixture save request.',
    })

    expect(payload.approvalPolicy).toBe('STANDARD')
    expect(payload.approvalStatus).toBe('PENDING_APPROVAL')

    return {
      api,
      title,
      productSku,
      warehouseCode,
      scenarioId: payload.scenarioRunId ?? payload.id,
      approvalPolicy: payload.approvalPolicy,
      approvalStatus: payload.approvalStatus,
    }
  } catch (error) {
    await api.dispose()
    throw error
  }
}

async function waitForSnapshotMatch(api, predicate, message) {
  let latestSnapshot = null
  await expect.poll(async () => {
    latestSnapshot = await readJson(await api.get('/api/dashboard/snapshot'))
    return Boolean(predicate(latestSnapshot))
  }, {
    timeout: 30_000,
    message,
  }).toBe(true)
  return latestSnapshot
}

function activeAlertsFromSnapshot(snapshot) {
  return snapshot?.alerts?.activeAlerts ?? []
}

function textReferencesSku(value, sku) {
  return typeof value === 'string' && value.toUpperCase().includes(sku.toUpperCase())
}

function alertReferencesSku(alert, sku) {
  return textReferencesSku(alert?.title, sku)
    || textReferencesSku(alert?.description, sku)
    || textReferencesSku(alert?.recommendedAction, sku)
}

function recommendationReferencesSku(recommendation, sku) {
  return textReferencesSku(recommendation?.title, sku)
    || textReferencesSku(recommendation?.description, sku)
}

function formatProofCodeLabel(value) {
  if (!value) {
    return 'Unknown'
  }
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

async function waitForBackendLowStockCoverage(api, fixture, message) {
  const startedAt = Date.now()
  let latestCoverage = null

  while (Date.now() - startedAt < 30_000) {
    const [inventory, alertFeed, recommendations] = await Promise.all([
      readJson(await api.get('/api/inventory'), {
        method: 'GET',
        url: '/api/inventory',
        requestPayload: {
          productSku: fixture.productSku,
          warehouseCode: fixture.warehouseCode,
        },
        note: `Inventory coverage lookup for realtime proof SKU ${fixture.productSku}.`,
      }),
      readJson(await api.get('/api/alerts'), {
        method: 'GET',
        url: '/api/alerts',
        requestPayload: {
          productSku: fixture.productSku,
        },
        note: `Alert coverage lookup for realtime proof SKU ${fixture.productSku}.`,
      }),
      readJson(await api.get('/api/recommendations'), {
        method: 'GET',
        url: '/api/recommendations',
        requestPayload: {
          productSku: fixture.productSku,
        },
        note: `Recommendation coverage lookup for realtime proof SKU ${fixture.productSku}.`,
      }),
    ])

    const inventoryRecord = inventory.find((item) => (
      item.productSku === fixture.productSku && item.warehouseCode === fixture.warehouseCode
    )) || null
    const alertRecord = alertFeed?.activeAlerts?.find((alert) => alertReferencesSku(alert, fixture.productSku)) || null
    const recommendationRecord = recommendations.find((recommendation) => (
      recommendationReferencesSku(recommendation, fixture.productSku)
    )) || null

    latestCoverage = {
      inventoryRecord,
      alertRecord,
      recommendationRecord,
      snapshot: null,
      snapshotCoverage: null,
      snapshotError: null,
      alertCount: alertFeed?.activeAlerts?.length ?? 0,
      recommendationCount: recommendations.length,
    }

    if (
      inventoryRecord?.lowStock
      && alertRecord
      && recommendationRecord
    ) {
      const { snapshot, snapshotError } = await readDashboardSnapshotBestEffort(api, {
        note: `Best-effort snapshot coverage lookup for realtime proof SKU ${fixture.productSku}.`,
        requestPayload: {
          productSku: fixture.productSku,
        },
      })
      const snapshotCoverage = realtimeCoverageFromSnapshot(snapshot, fixture.productSku)
      return latestCoverage
        ? {
            ...latestCoverage,
            snapshot,
            snapshotCoverage,
            snapshotError,
          }
        : {
            inventoryRecord,
            alertRecord,
            recommendationRecord,
            snapshot,
            snapshotCoverage,
            snapshotError,
            alertCount: alertFeed?.activeAlerts?.length ?? 0,
            recommendationCount: recommendations.length,
          }
    }

    await new Promise((resolve) => setTimeout(resolve, 500))
  }

  const { snapshot, snapshotError } = await readDashboardSnapshotBestEffort(api, {
    note: `Best-effort snapshot coverage lookup for realtime proof SKU ${fixture.productSku}.`,
    requestPayload: {
      productSku: fixture.productSku,
    },
  })
  const snapshotCoverage = realtimeCoverageFromSnapshot(snapshot, fixture.productSku)
  latestCoverage = {
    ...latestCoverage,
    snapshot,
    snapshotCoverage,
    snapshotError,
  }

  throw new Error(`${message} Diagnostics: ${JSON.stringify({
    inventoryRecord: latestCoverage?.inventoryRecord ?? null,
    alertRecord: latestCoverage?.alertRecord ?? null,
    recommendationRecord: latestCoverage?.recommendationRecord ?? null,
    snapshotAlertRecord: latestCoverage?.snapshotCoverage?.alertRecord ?? null,
    snapshotRecommendationRecord: latestCoverage?.snapshotCoverage?.recommendationRecord ?? null,
    snapshotGeneratedAt: latestCoverage?.snapshot?.generatedAt ?? null,
    alertCount: latestCoverage?.alertCount ?? 0,
    recommendationCount: latestCoverage?.recommendationCount ?? 0,
  })}`)
}

async function ensureRecentOrder(api) {
  const suffix = randomUUID().slice(0, 8).toUpperCase()
  const externalOrderId = `UI-ORD-${suffix}`
  const createdOrder = await readJson(await api.post('/api/orders', {
    data: {
      externalOrderId,
      warehouseCode: 'WH-NORTH',
      items: [
        {
          productSku: proofProductSku,
          quantity: 1,
          unitPrice: 79,
        },
      ],
    },
  }, {
    method: 'POST',
    url: '/api/orders',
    requestPayload: {
      externalOrderId,
      warehouseCode: 'WH-NORTH',
      productSku: proofProductSku,
    },
    note: `Creating deterministic hosted proof order ${externalOrderId}.`,
  }))

  const startedAt = Date.now()
  let latestCoverage = null

  while (Date.now() - startedAt < 30_000) {
    const recentOrdersUrl = recentOrdersLookupUrl(externalOrderId)
    const { payload: recentOrders, error: recentOrdersError } = await readJsonGetBestEffort(api, recentOrdersUrl, {
      requestPayload: {
        externalOrderId,
      },
      note: `Recent order lookup for deterministic hosted proof order ${externalOrderId}.`,
    })

    if (!Array.isArray(recentOrders)) {
      latestCoverage = {
        order: null,
        snapshotOrder: null,
        snapshotGeneratedAt: null,
        snapshotError: null,
        recentOrderIds: [],
        recentOrdersError,
        snapshotOrderIds: [],
      }
      await new Promise((resolve) => setTimeout(resolve, 500))
      continue
    }

    const order = recentOrders.find((candidate) => candidate.externalOrderId === externalOrderId) || null
    latestCoverage = {
      order,
      snapshotOrder: null,
      snapshotGeneratedAt: null,
      snapshotError: null,
      recentOrderIds: recentOrders.map((candidate) => candidate.externalOrderId).slice(0, 12),
      recentOrdersError,
      snapshotOrderIds: [],
    }

    if (order) {
      const { snapshot, snapshotError } = await readDashboardSnapshotBestEffort(api, {
        note: `Best-effort snapshot recent order lookup for deterministic hosted proof order ${externalOrderId}.`,
        requestPayload: {
          externalOrderId,
        },
      })
      const snapshotOrder = snapshot?.recentOrders?.find((candidate) => candidate.externalOrderId === externalOrderId) || null
      return {
        createdOrder,
        order,
        snapshotOrder,
        snapshot,
        snapshotError,
      }
    }

    await new Promise((resolve) => setTimeout(resolve, 500))
  }

  const { snapshot, snapshotError } = await readDashboardSnapshotBestEffort(api, {
    note: `Best-effort snapshot recent order lookup for deterministic hosted proof order ${externalOrderId}.`,
    requestPayload: {
      externalOrderId,
    },
  })
  latestCoverage = {
    ...latestCoverage,
    snapshotOrder: snapshot?.recentOrders?.find((candidate) => candidate.externalOrderId === externalOrderId) || null,
    snapshotGeneratedAt: snapshot?.generatedAt ?? null,
    snapshotError,
    snapshotOrderIds: (snapshot?.recentOrders || []).map((candidate) => candidate.externalOrderId).slice(0, 12),
  }

  throw new Error(`Expected deterministic proof order ${externalOrderId} to appear in /api/orders/recent before the orders UI assertion. Diagnostics: ${JSON.stringify(latestCoverage)}`)
}

async function waitForOrderReadModelCoverage(api, externalOrderId, message) {
  const startedAt = Date.now()
  let latestCoverage = null

  while (Date.now() - startedAt < 30_000) {
    const recentOrdersUrl = recentOrdersLookupUrl(externalOrderId)
    const { payload: recentOrders, error: recentOrdersError } = await readJsonGetBestEffort(api, recentOrdersUrl, {
      requestPayload: {
        externalOrderId,
      },
      note: `Recent order lookup for hosted proof order ${externalOrderId}.`,
    })

    if (!Array.isArray(recentOrders)) {
      latestCoverage = {
        order: null,
        snapshotOrder: null,
        snapshotGeneratedAt: null,
        snapshotError: null,
        recentOrderIds: [],
        recentOrdersError,
        snapshotOrderIds: [],
      }
      await pageWait(500)
      continue
    }

    const order = Array.isArray(recentOrders)
      ? recentOrders.find((candidate) => candidate.externalOrderId === externalOrderId) || null
      : null

    latestCoverage = {
      order,
      snapshotOrder: null,
      snapshotGeneratedAt: null,
      snapshotError: null,
      recentOrderIds: Array.isArray(recentOrders) ? recentOrders.map((candidate) => candidate.externalOrderId).slice(0, 12) : [],
      recentOrdersError,
      snapshotOrderIds: [],
    }

    if (order) {
      const { snapshot, snapshotError } = await readDashboardSnapshotBestEffort(api, {
        note: `Best-effort snapshot order lookup for hosted proof order ${externalOrderId}.`,
        requestPayload: {
          externalOrderId,
        },
      })
      const snapshotOrder = Array.isArray(snapshot?.recentOrders)
        ? snapshot.recentOrders.find((candidate) => candidate.externalOrderId === externalOrderId) || null
        : null
      return {
        order,
        snapshotOrder,
        snapshot,
        snapshotError,
      }
    }

    await pageWait(500)
  }

  const { snapshot, snapshotError } = await readDashboardSnapshotBestEffort(api, {
    note: `Best-effort snapshot order lookup for hosted proof order ${externalOrderId}.`,
    requestPayload: {
      externalOrderId,
    },
  })
  latestCoverage = {
    ...latestCoverage,
    snapshotOrder: Array.isArray(snapshot?.recentOrders)
      ? snapshot.recentOrders.find((candidate) => candidate.externalOrderId === externalOrderId) || null
      : null,
    snapshotGeneratedAt: snapshot?.generatedAt ?? null,
    snapshotError,
    snapshotOrderIds: Array.isArray(snapshot?.recentOrders) ? snapshot.recentOrders.map((candidate) => candidate.externalOrderId).slice(0, 12) : [],
  }

  throw new Error(`${message} Diagnostics: ${JSON.stringify(latestCoverage)}`)
}

async function pageWait(timeoutMs) {
  await new Promise((resolve) => setTimeout(resolve, timeoutMs))
}

async function waitForApprovedScenarioCoverage(api, scenarioFixture, message) {
  const startedAt = Date.now()
  let latestCoverage = null

  while (Date.now() - startedAt < 30_000) {
    const history = await readJson(await api.get('/api/scenarios/history'), {
      method: 'GET',
      url: '/api/scenarios/history',
      requestPayload: {
        scenarioRunId: scenarioFixture.scenarioId,
        scenarioTitle: scenarioFixture.title,
      },
      note: `Scenario approval history lookup for hosted proof scenario ${scenarioFixture.title}.`,
    })

    const scenarioRun = Array.isArray(history)
      ? history.find((candidate) => (
          String(candidate.id) === String(scenarioFixture.scenarioId)
          || candidate.title === scenarioFixture.title
        )) || null
      : null

    latestCoverage = {
      scenarioRun,
      historyPreview: Array.isArray(history)
        ? history.slice(0, 12).map((candidate) => ({
            id: candidate.id,
            title: candidate.title,
            approvalStatus: candidate.approvalStatus,
            approvalStage: candidate.approvalStage,
            executable: candidate.executable,
          }))
        : [],
    }

    if (scenarioRun?.approvalStatus === 'APPROVED' && scenarioRun?.executable === false) {
      return scenarioRun
    }

    await pageWait(500)
  }

  throw new Error(`${message} Diagnostics: ${JSON.stringify(latestCoverage)}`)
}

async function approveScenarioAndWaitForExternalHandoff(page, api, scenarioFixture, scenarioActionConsole) {
  const approveButton = scenarioActionConsole.getByRole('button', { name: 'Approve Plan' })

  await expect(approveButton).toBeVisible()
  await approveButton.click()

  const approvedScenario = await waitForApprovedScenarioCoverage(
    api,
    scenarioFixture,
    `Expected scenario ${scenarioFixture.title} to become governed and ready for external action after approval.`,
  )

  await expect(page.locator('.success-text').filter({
    hasText: new RegExp(`^Approved decision ${escapeRegExp(scenarioFixture.title)} is governed and ready for external action under Standard approval\\.$`, 'i'),
  }).first()).toBeVisible({ timeout: 2_500 }).catch(() => {})

  return {
    approvedScenario,
  }
}

async function createRealtimeInventoryFixture(api) {
  const suffix = randomUUID().slice(0, 8).toUpperCase()
  const productSku = `SKU-RT-${suffix}`
  const productName = `Realtime Proof ${suffix}`
  const warehouseCode = 'WH-NORTH'
  const reorderThreshold = 10
  const safeQuantity = 20
  const lowQuantity = 5

  await readJson(await api.post('/api/products', {
    data: {
      sku: productSku,
      name: productName,
      category: 'Verification',
    },
  }))

  await readJson(await api.post('/api/inventory/update', {
    data: {
      productSku,
      warehouseCode,
      quantityAvailable: safeQuantity,
      reorderThreshold,
    },
  }))

  return {
    productSku,
    productName,
    warehouseCode,
    reorderThreshold,
    safeQuantity,
    lowQuantity,
  }
}

function realtimeCoverageFromSnapshot(snapshot, sku) {
  const activeAlerts = activeAlertsFromSnapshot(snapshot)
  return {
    alertRecord: activeAlerts.find((alert) => alertReferencesSku(alert, sku)) || null,
    recommendationRecord: snapshot?.recommendations?.find((recommendation) => recommendationReferencesSku(recommendation, sku)) || null,
  }
}

async function ensureAlertAndRecommendationCoverage(api) {
  const fixture = await createRealtimeInventoryFixture(api)
  await readJson(await api.post('/api/inventory/update', {
    data: {
      productSku: fixture.productSku,
      warehouseCode: fixture.warehouseCode,
      quantityAvailable: fixture.lowQuantity,
      reorderThreshold: fixture.reorderThreshold,
    },
  }, {
    method: 'POST',
    url: '/api/inventory/update',
    requestPayload: {
      productSku: fixture.productSku,
      warehouseCode: fixture.warehouseCode,
      quantityAvailable: fixture.lowQuantity,
      reorderThreshold: fixture.reorderThreshold,
    },
    note: `Driving deterministic hosted proof inventory ${fixture.productSku} into low-stock state.`,
  }))

  const coverage = await waitForBackendLowStockCoverage(
    api,
    fixture,
    `Expected deterministic low-stock fixture ${fixture.productSku} to propagate through inventory, alerts, and recommendations before the UI verification.`,
  )

  return {
    fixture,
    snapshot: coverage.snapshot,
    inventoryRecord: coverage.inventoryRecord,
    alertRecord: coverage.alertRecord,
    recommendationRecord: coverage.recommendationRecord,
    snapshotAlertRecord: coverage.snapshotCoverage?.alertRecord ?? null,
    snapshotRecommendationRecord: coverage.snapshotCoverage?.recommendationRecord ?? null,
    restore: async () => {
      await readJson(await api.post('/api/inventory/update', {
        data: {
          productSku: fixture.productSku,
          warehouseCode: fixture.warehouseCode,
          quantityAvailable: fixture.safeQuantity,
          reorderThreshold: fixture.reorderThreshold,
        },
      }, {
        method: 'POST',
        url: '/api/inventory/update',
        requestPayload: {
          productSku: fixture.productSku,
          warehouseCode: fixture.warehouseCode,
          quantityAvailable: fixture.safeQuantity,
          reorderThreshold: fixture.reorderThreshold,
        },
        note: `Restoring deterministic hosted proof inventory ${fixture.productSku} to its safe quantity.`,
      }))
    },
  }
}

test('auth flow and the full authenticated page system render cleanly in a browser', async ({ page }) => {
  await page.goto('/dashboard')
  const signInCard = await expectSignInShellReady(page)
  await waitForSignInReady(signInCard)

  await fillSignInForm(signInCard, users.operationsLead, 'wrong-code')
  await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  await expectSignInErrorAndRecovery(signInCard, 'Invalid operator credentials.')

  await fillSignInForm(signInCard, users.operationsLead, users.operationsLead.password)
  await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await page.reload()
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  await waitForDashboardSnapshotReady(page)

  const operationsLeadPages = appPages.filter(([route]) => route !== '/approvals')
  for (const [route, title] of operationsLeadPages) {
    await navigateWithinApp(page, route)
    await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
    await expect(page.locator('.workspace-topbar')).toBeVisible()
    await expectNoFatalUiErrors(page)
  }

  await navigateWithinApp(page, '/approvals')
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()

  await signOutViaUi(page)
  await loginViaUi(page, users.reviewOwner)
  await navigateWithinApp(page, '/approvals')
  await expect(page.getByRole('heading', { level: 1, name: 'Approvals center' })).toBeVisible()
  await expect(page.locator('.workspace-topbar')).toBeVisible()
  await expectNoFatalUiErrors(page)

  const tenantPlatformResponse = await page.request.get(`${backendUrl}/api/platform/overview`)
  expect(tenantPlatformResponse.status()).toBe(403)
  for (const route of platformProtectedRoutes) {
    await page.goto(route)
    await expect(page.getByRole('heading', { level: 1, name: 'Access the SynapseCore control plane.' })).toBeVisible()
    await expect(page.getByText('Separate platform authority')).toBeVisible()
  }
  await page.goto('/dashboard')
  await waitForDashboardSnapshotReady(page)

  await signOutViaUi(page)
})

test('product catalog onboarding works through tenant-scoped API and browser surface', async ({ page }) => {
  const api = await createApiContext(users.operationsLead)
  const suffix = randomUUID().slice(0, 8).toUpperCase()
  const primarySku = `SKU-UI-${suffix}`
  const importSku = `SKU-IMP-${suffix}`

  try {
    const createdProduct = await readJson(await api.post('/api/products', {
      data: {
        sku: primarySku,
        name: `UI Catalog ${suffix}`,
        category: 'Verification',
      },
    }))
    expect(createdProduct.sku).toBe(primarySku)
    expect(createdProduct.tenantCode).toBe(users.operationsLead.tenantCode)

    const updatedProduct = await readJson(await api.put(`/api/products/${createdProduct.id}`, {
      data: {
        sku: primarySku,
        name: `UI Catalog ${suffix} Updated`,
        category: 'Verification',
      },
    }))
    expect(updatedProduct.name).toContain('Updated')

    const importResult = await readJson(await api.post('/api/products/import', {
      multipart: {
        file: {
          name: 'products.csv',
          mimeType: 'text/csv',
          buffer: Buffer.from(
            `sku,name,category\n${importSku},Imported Product ${suffix},Verification\n${primarySku},Imported Update ${suffix},Verification\n${importSku},Duplicate Product ${suffix},Verification\n`,
            'utf8',
          ),
        },
      },
    }))
    expect(importResult.created).toBe(1)
    expect(importResult.updated).toBe(1)
    expect(importResult.failed).toBe(1)

    const products = await readJson(await api.get('/api/products'))
    expect(products.some((product) => product.sku === primarySku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
    expect(products.some((product) => product.sku === importSku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()

    await loginViaUi(page, users.operationsLead)
    await navigateWithinApp(page, '/catalog')
    await expect(page.getByRole('heading', { level: 1, name: 'Tenant product catalog' })).toBeVisible()
    await expect(page.getByText(primarySku).first()).toBeVisible()
    await expect(page.getByText(importSku).first()).toBeVisible()
    await expectNoFatalUiErrors(page)
  } finally {
    await api.dispose()
  }
})

test('@realtime dashboard summary updates live without a browser refresh', async ({ page }) => {
  const api = await createApiContext(users.operationsLead)
  const realtimeFixture = await createRealtimeInventoryFixture(api)

  await loginViaUi(page, users.operationsLead)
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  await expect(page.getByText('Realtime state')).toBeVisible()
  await waitForRealtimeConnectionLive(page)

  try {
    await readJson(await api.post('/api/inventory/update', {
      data: {
        productSku: realtimeFixture.productSku,
        warehouseCode: realtimeFixture.warehouseCode,
        quantityAvailable: realtimeFixture.lowQuantity,
        reorderThreshold: realtimeFixture.reorderThreshold,
      },
    }, {
      method: 'POST',
      url: '/api/inventory/update',
      requestPayload: {
        productSku: realtimeFixture.productSku,
        warehouseCode: realtimeFixture.warehouseCode,
        quantityAvailable: realtimeFixture.lowQuantity,
        reorderThreshold: realtimeFixture.reorderThreshold,
      },
      note: `Driving realtime proof SKU ${realtimeFixture.productSku} into low-stock state from the hosted proof.`,
    }))

    const liveCoverage = await waitForBackendLowStockCoverage(
      api,
      realtimeFixture,
      `Expected low-stock realtime proof inventory on ${realtimeFixture.productSku} to propagate through backend inventory, alerts, and recommendations before asserting the live UI update.`,
    )
    const expectedAlertText = liveCoverage.snapshotCoverage?.alertRecord?.title
      || liveCoverage.snapshotCoverage?.alertRecord?.description
      || liveCoverage.snapshotCoverage?.alertRecord?.recommendedAction
      || liveCoverage.alertRecord?.title
      || liveCoverage.alertRecord?.description
      || liveCoverage.alertRecord?.recommendedAction
    const expectedRecommendationText = liveCoverage.snapshotCoverage?.recommendationRecord?.title
      || liveCoverage.snapshotCoverage?.recommendationRecord?.description
      || liveCoverage.recommendationRecord?.title
      || liveCoverage.recommendationRecord?.description

    expect(expectedAlertText).toBeTruthy()
    expect(expectedRecommendationText).toBeTruthy()

    const liveActivity = page.locator('.dashboard-activity-grid').first()
    await expect(liveActivity.getByText(realtimeFixture.productSku, { exact: false }).first()).toBeVisible({ timeout: 30_000 })
    await expectNoFatalUiErrors(page)
  } finally {
    await readJson(await api.post('/api/inventory/update', {
      data: {
        productSku: realtimeFixture.productSku,
        warehouseCode: realtimeFixture.warehouseCode,
        quantityAvailable: realtimeFixture.safeQuantity,
        reorderThreshold: realtimeFixture.reorderThreshold,
      },
    }))
    await api.dispose()
  }
})

test('replay recovery, scenario approval, execution, and browser role gating work through the UI', async ({ page }, testInfo) => {
  testInfo.setTimeout(360_000)
  const replayFixture = await createReplayFixture()

  try {
    const backendReplayCoverage = await waitForReplayQueueCoverage(
      replayFixture.api,
      replayFixture.externalOrderId,
      replayFixture.sourceSystem,
      `Expected replay verification record ${replayFixture.externalOrderId} to be visible in the replay queue API before UI verification.`,
    )

    await loginViaUi(page, users.integrationLead)
    await navigateWithinApp(page, '/replay-queue')
    await expect(page.getByRole('heading', { level: 1, name: 'Failed inbound recovery' })).toBeVisible()
    await refreshWorkspace(page)

    let currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
      if (currentReplayOutcome.state === 'queued') {
        const replayQueueRecord = page.locator('.signal-list-item.selectable-card').filter({ hasText: replayFixture.externalOrderId }).first()
        let lastReplayQueueUiState = null
        try {
          await expect.poll(async () => {
            await refreshWorkspace(page)
            lastReplayQueueUiState = await page.evaluate((expectedExternalOrderId) => {
              const normalizeText = (value) => value?.replace?.(/\s+/g, ' ')?.trim?.() || ''
              const queueButtons = [...(globalThis.document?.querySelectorAll?.('.signal-list-item.selectable-card') || [])]
              return {
                pageUrl: globalThis.location?.href || '',
                queueRows: queueButtons.map((button) => normalizeText(button.textContent)).slice(0, 12),
                matchingRowFound: queueButtons.some((button) => normalizeText(button.textContent).includes(expectedExternalOrderId)),
                selectedReplayDetail: normalizeText(
                  [...(globalThis.document?.querySelectorAll?.('.section-card') || [])]
                    .find((card) => normalizeText(card.textContent).includes('Recovery detail'))
                    ?.textContent,
                ),
              }
            }, replayFixture.externalOrderId)
            return lastReplayQueueUiState.matchingRowFound && await replayQueueRecord.isVisible().catch(() => false) ? 'visible' : 'waiting'
        }, {
          timeout: 30_000,
          message: `Expected replay queue ${replayFixture.externalOrderId} to appear in the UI before any automated replay could mutate it.`,
        }).toBe('visible')
      } catch (error) {
        throw new Error(`Expected replay queue ${replayFixture.externalOrderId} to appear in the UI before any automated replay could mutate it. Diagnostics: ${JSON.stringify({
          backendReplayCoverage,
          currentReplayOutcome,
          lastReplayQueueUiState,
        })} Original error: ${error?.message || String(error)}`)
      }

      await expect(replayQueueRecord).toBeVisible()
      await replayQueueRecord.click()

      const replayDetail = page.locator('.section-card')
        .filter({ hasText: 'Recovery detail' })
        .filter({ hasText: replayFixture.externalOrderId })
        .last()
      await expect(replayDetail.getByText(replayFixture.externalOrderId).first()).toBeVisible()

      await replayFixture.enableConnector()
      let postEnableReplayState = 'waiting'
      let postEnableReplayUiState = null
      const postEnableReplayStatesSeen = []
      try {
        await expect.poll(async () => {
          currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
          postEnableReplayState = describeReplayOutcome(currentReplayOutcome)
          if (!postEnableReplayStatesSeen.includes(postEnableReplayState)) {
            postEnableReplayStatesSeen.push(postEnableReplayState)
          }
          postEnableReplayUiState = await readExactReplayActionState(page, replayFixture).catch(() => null)
          return currentReplayOutcome?.state === 'replayed'
            || currentReplayOutcome?.state === 'queued'
            || Boolean(postEnableReplayUiState?.rowFound)
        }, {
          timeout: 20_000,
          message: `Expected ${replayFixture.externalOrderId} to either remain queued for manual replay or auto-replay after enabling the replay connector.`,
        }).toBe(true)
      } catch (error) {
        const replayPageDiagnostics = await readReplayPageDiagnostics(page, replayFixture).catch(() => null)
        throw new Error(`Expected ${replayFixture.externalOrderId} to either remain queued for manual replay or auto-replay after enabling the replay connector. Diagnostics: ${JSON.stringify({
          postEnableReplayState,
          postEnableReplayStatesSeen,
          currentReplayOutcome,
          postEnableReplayUiState,
          replayPageDiagnostics,
        })} Original error: ${error?.message || String(error)}`)
      }

      if (currentReplayOutcome?.state !== 'replayed') {
        const replayPageDiagnostics = ensurePageDiagnostics(page)
        await waitForReplayButtonReady(page, replayFixture)

        const replayRequestPromise = page.waitForRequest((request) => (
          request.method() === 'POST'
            && /\/api\/integrations\/orders\/replay\/\d+$/i.test(request.url())
        ), { timeout: 10_000 })
        const replayResponsePromise = page.waitForResponse((response) => (
          response.request().method() === 'POST'
            && /\/api\/integrations\/orders\/replay\/\d+$/i.test(response.url())
        ), { timeout: 45_000 }).catch(() => null)

        let replayRequest = null
        try {
          [replayRequest] = await Promise.all([
            replayRequestPromise,
            clickExactReplayButton(page, replayFixture),
          ])
        } catch (error) {
          throw new Error(`Expected replay request submission for ${replayFixture.externalOrderId} after the manual replay button became enabled. Diagnostics: ${JSON.stringify({
            replayRecordExternalOrderId: replayFixture.externalOrderId,
            lastApiResponse: replayPageDiagnostics.lastApiResponse,
            consoleErrors: replayPageDiagnostics.consoleErrors,
            failedRequests: replayPageDiagnostics.failedRequests,
          })} Original error: ${error?.message || String(error)}`)
        }

        await expect(page.getByRole('button', { name: 'Replaying...' }).first()).toBeVisible({ timeout: 5_000 }).catch(() => {})

        const replayResponse = await replayResponsePromise
        if (replayResponse && !replayResponse.ok()) {
          const replayResponseText = await replayResponse.text()
          let replayPayload = null
          try {
            replayPayload = replayResponseText ? JSON.parse(replayResponseText) : null
          } catch {
            replayPayload = null
          }
          throw new Error(JSON.stringify({
            method: 'POST',
            url: replayResponse.url(),
            status: replayResponse.status(),
            requestId: replayResponse.headers()['x-request-id'] || replayPayload?.requestId || null,
            responseBody: replayPayload ?? replayResponseText,
            requestPayload: {
              replayRecordExternalOrderId: replayFixture.externalOrderId,
              observedRequestUrl: replayRequest?.url?.() || null,
            },
            note: `Replay request failed for ${replayFixture.externalOrderId}.`,
          }))
        }

        if (!replayResponse) {
          currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId).catch(() => null)
          if (currentReplayOutcome?.state === 'queued') {
            await expect(page.locator('.error-text').filter({ hasText: /failed|unable|forbidden|denied|error/i }).first()).toBeVisible({ timeout: 2_500 }).catch(() => {})
          }
        }
      }

      await waitForReplayResolution(
        replayFixture.api,
        replayFixture.externalOrderId,
        45_000,
        `Expected replay verification order ${replayFixture.externalOrderId} to recover into the live order flow.`,
      )

      await refreshWorkspace(page)
      await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
    }

  await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
  } finally {
    await replayFixture.api.dispose()
  }

  await signOutViaUi(page)

  const scenarioFixture = await createScenarioFixture()

  try {
    await loginViaUi(page, users.reviewOwner)
    await navigateWithinApp(page, '/scenario-history')
    await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()

    const scenarioHistoryCard = await waitForScenarioHistoryCard(page, scenarioFixture.title)
    await activateSelectableButton(scenarioHistoryCard)

    const scenarioActionConsole = page.locator('.section-card').filter({
      hasText: 'Scenario action console',
      has: page.getByText(scenarioFixture.title),
    }).first()
    await expect(scenarioActionConsole).toBeVisible()
    await approveScenarioAndWaitForExternalHandoff(page, scenarioFixture.api, scenarioFixture, scenarioActionConsole)

    await refreshWorkspace(page)
    const approvedScenarioCard = await waitForScenarioHistoryCard(page, scenarioFixture.title)
    await activateSelectableButton(approvedScenarioCard)

    const approvedScenarioActionConsole = page.locator('.section-card').filter({
      hasText: 'Scenario action console',
      has: page.getByText(scenarioFixture.title),
    }).first()
    await expect(approvedScenarioActionConsole).toBeVisible()
    await expect(approvedScenarioActionConsole.getByRole('button', { name: 'Execute Scenario' })).toHaveCount(0)
    await expect(approvedScenarioActionConsole).toContainText('ready for external operational follow-through')
  } finally {
    await scenarioFixture.api.dispose()
  }

  await signOutViaUi(page)

  await loginViaUi(page, users.operationsPlanner)
  await page.goto('/users')
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  await expect(page.getByRole('button', { name: /^Users\b/ })).toHaveCount(0)

  const plannerApi = await createApiContext(users.operationsPlanner)
  try {
    const usersResponse = await plannerApi.get('/api/access/admin/users')
    expect(usersResponse.status()).toBe(403)
  } finally {
    await plannerApi.dispose()
  }
})

test('alerts, recommendations, orders, inventory, integrations, users, profile, and settings surfaces stay connected to the live backend', async ({ page }, testInfo) => {
  const api = await createApiContext(users.operationsLead)
  const integrationApi = await createApiContext(users.integrationLead)
  let restoreAlertCoverage = async () => {}
  ensurePageDiagnostics(page)

  try {
    const alertCoverage = await ensureAlertAndRecommendationCoverage(api)
    restoreAlertCoverage = alertCoverage.restore
    const recentOrder = await ensureRecentOrder(integrationApi)
    const workspace = await readJson(await api.get('/api/access/admin/workspace'))
    const operators = await readJson(await api.get('/api/access/admin/operators'))
    const accessUsers = await readJson(await api.get('/api/access/admin/users'))
    const runtimeRecord = await readJson(await api.get('/api/system/runtime'))
    const alertRecord = alertCoverage.alertRecord
    const recommendationRecord = alertCoverage.recommendationRecord
    const orderRecord = recentOrder.order
    const inventoryRecord = alertCoverage.inventoryRecord
    const connectorCandidates = [
      ...(alertCoverage.snapshot?.integrationConnectors || []),
      ...(workspace.connectors || []),
    ]

    expect(alertRecord).toBeTruthy()
    expect(recommendationRecord).toBeTruthy()
    expect(orderRecord).toBeTruthy()
    expect(inventoryRecord).toBeTruthy()
    expect(workspace).toBeTruthy()
    expect(operators.length).toBeGreaterThan(0)
    expect(accessUsers.length).toBeGreaterThan(0)
    expect(connectorCandidates.length).toBeGreaterThan(0)

    const expectedVisibleOperator = operators
      .slice(0, 5)
      .find((operator) => ['Operations Lead', 'Operations Planner', 'Integration Lead'].includes(operator.displayName))
    const expectedVisibleUser = accessUsers
      .slice(0, 5)
      .find((user) => ['Hosted Verification Planner', 'Hosted Verification Integration Admin'].includes(user.fullName))

    expect(expectedVisibleOperator).toBeTruthy()
    expect(expectedVisibleUser).toBeTruthy()

    await loginViaUi(page, users.operationsLead)

    await navigateWithinApp(page, '/alerts')
    await expect(page.getByRole('heading', { level: 1, name: 'Operational warning center' })).toBeVisible()
    await waitForAlertPageAlertVisible(page, alertRecord, testInfo)

    await navigateWithinApp(page, '/recommendations')
    await expect(page.getByRole('heading', { level: 1, name: 'Action queue for the operating team' })).toBeVisible()
    await waitForRecommendationPageVisible(page, recommendationRecord, testInfo)

    await navigateWithinApp(page, '/orders')
    await expect(page.getByRole('heading', { level: 1, name: 'Live order operations' })).toBeVisible()
    await waitForOrdersPageOrderVisible(page, orderRecord, testInfo, api)

    await navigateWithinApp(page, '/inventory')
    await expect(page.getByRole('heading', { level: 1, name: 'Inventory intelligence' })).toBeVisible()
    await expect(page.getByText(inventoryRecord.productName).first()).toBeVisible()
    await activateSelectableButton(
      page.getByRole('button', { name: new RegExp(escapeRegExp(inventoryRecord.productName), 'i') }).first(),
    )
    await expect(page.getByText(inventoryRecord.productSku).first()).toBeVisible()

    await navigateWithinApp(page, '/integrations')
    await expect(page.getByRole('heading', { level: 1, name: 'Connector management and telemetry' })).toBeVisible()
    await expect(page.locator('button.system-select-card').first()).toBeVisible()
    const visibleConnectorMatch = await findVisibleIntegrationConnector(page, connectorCandidates)
    if (!visibleConnectorMatch) {
      throw new Error('Expected at least one integration connector rendered in the UI to match backend connector data.')
    }
    await activateSelectableButton(
      visibleConnectorMatch.button,
    )
    await expect(page.getByText(visibleConnectorMatch.connector.sourceSystem).first()).toBeVisible()
    await page.getByRole('button', { name: 'Manage Policies' }).click()

    await expect(page.getByRole('heading', { level: 1, name: 'Tenant and workspace settings' })).toBeVisible()
    await expect(page.getByLabel('Company workspace name').first()).toHaveValue(workspace.tenantName)
    if (workspace.connectors?.length) {
      await expect(page.getByText(workspace.connectors[0].displayName).first()).toBeVisible()
    }

    await navigateWithinApp(page, '/runtime')
    await expect(page.getByRole('heading', { level: 1, name: 'Runtime, incidents, and observability' })).toBeVisible()
    await waitForRuntimePageReady(page, runtimeRecord, testInfo)

    await navigateWithinApp(page, '/users')
    await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
    await waitForUsersPageReady(page, expectedVisibleOperator.displayName, expectedVisibleUser.fullName, testInfo)

    await navigateWithinApp(page, '/profile')
    await expect(page.getByRole('heading', { level: 1, name: 'Personal profile and session controls' })).toBeVisible()
    await expect(page.getByText(users.operationsLead.username).first()).toBeVisible()
    await expect(page.getByText(workspace.tenantName).first()).toBeVisible()

    await expectNoFatalUiErrors(page)
  } finally {
    await restoreAlertCoverage()
    await integrationApi.dispose()
    await api.dispose()
  }
})

test('frontend surfaces backend auth rate limiting without getting stuck in a loading state', async ({ page }) => {
  await page.goto('/sign-in')
  const signInCard = await expectSignInShellReady(page)
  await waitForSignInReady(signInCard)

  await triggerUiAuthRateLimit(page, signInCard, users.operationsLead)
})
