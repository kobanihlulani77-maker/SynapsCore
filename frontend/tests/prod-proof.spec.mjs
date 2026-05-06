import fs from 'node:fs/promises'
import { randomUUID } from 'node:crypto'
import path from 'node:path'
import { expect, request as playwrightRequest, test } from '@playwright/test'
import {
  authRateLimitCooldownBufferMs,
  authRateLimitWindowMs,
  hostedProofStatePath,
} from './prod-proof-state.mjs'

const backendUrl = process.env.PLAYWRIGHT_API_BASE_URL
  || process.env.PLAYWRIGHT_BACKEND_URL
  || 'https://synapscore-3.onrender.com'
const requiredEnv = (...names) => {
  for (const name of names) {
    const value = process.env[name]
    if (value && value.trim()) {
      return value.trim()
    }
  }
  throw new Error(`Missing required environment variable. Set one of: ${names.join(', ')} for live production proof.`)
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
const proofProductSku = (process.env.PLAYWRIGHT_PROOF_PRODUCT_SKU || defaultProofProductSku).trim().toUpperCase()
const configuredAuthRateLimitMaxAttempts = Number.parseInt(
  process.env.PLAYWRIGHT_AUTH_RATE_LIMIT_MAX_ATTEMPTS
    || process.env.SYNAPSECORE_RATE_LIMIT_AUTH_LOGIN_MAX_ATTEMPTS
    || '30',
  10,
)
const authRateLimitAttemptBudget = Number.isFinite(configuredAuthRateLimitMaxAttempts) && configuredAuthRateLimitMaxAttempts > 0
  ? configuredAuthRateLimitMaxAttempts + 20
  : 50

const users = {
  operationsLead: {
    tenantCode: proofTenantCode,
    username: requiredEnv('PLAYWRIGHT_TENANT_ADMIN_USERNAME', 'PLAYWRIGHT_OPERATIONS_LEAD_USERNAME'),
    password: requiredEnv('PLAYWRIGHT_TENANT_ADMIN_PASSWORD', 'PLAYWRIGHT_OPERATIONS_LEAD_PASSWORD'),
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
  ['/platform-admin', 'Platform overview and cross-tenant trust'],
  ['/tenant-management', 'Tenant onboarding and workspace rollout'],
  ['/system-config', 'System configuration and operational defaults'],
  ['/releases', 'Release, deployment, and environment'],
]

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

  if (!response.ok()) {
    const failureDetails = {
      method: context.method || response.request().method(),
      url: context.url || response.url(),
      status: response.status(),
      requestId: response.headers()['x-request-id'] || payload?.requestId || null,
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
  throw new Error(`Expected JSON response but received non-JSON payload from ${response.request().method()} ${response.url()}: ${responseText}`)
}

async function loginViaUi(page, credentials, options = {}) {
  const { requireDashboardSnapshot = false } = options
  await page.goto('/sign-in')
  await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  const signInCard = page.locator('.public-signin-card')
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
    await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  }
}

async function fillSignInForm(signInCard, credentials, password) {
  const tenantField = signInCard.getByRole('combobox', { name: 'Tenant workspace', exact: true })
  const usernameField = signInCard.getByRole('textbox', { name: 'Username', exact: true })
  const passwordField = signInCard.getByLabel('Password', { exact: true })
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
  await expect(signInCard.getByRole('combobox', { name: 'Tenant workspace', exact: true })).toBeEnabled()
  await expect(signInCard.getByRole('textbox', { name: 'Username', exact: true })).toBeEnabled()
  await expect(signInCard.getByLabel('Password', { exact: true })).toBeEnabled()
}

async function expectSignInErrorAndRecovery(signInCard, message) {
  await expect(signInCard.getByText(message)).toBeVisible({ timeout: 15_000 })
  await waitForSignInReady(signInCard)
}

async function writeHostedProofState(nextState) {
  await fs.mkdir(path.dirname(hostedProofStatePath), { recursive: true })
  let currentState = {}
  try {
    currentState = JSON.parse(await fs.readFile(hostedProofStatePath, 'utf8'))
  } catch {
    currentState = {}
  }

  await fs.writeFile(
    hostedProofStatePath,
    JSON.stringify({ ...currentState, ...nextState }, null, 2),
    'utf8',
  )
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
  const snapshotTimestamp = page.locator('#workspace-trust-rail .muted-text').filter({ hasText: /^Snapshot / }).first()
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

  for (let attempt = 1; attempt <= authRateLimitAttemptBudget; attempt += 1) {
    await fillSignInForm(signInCard, credentials, 'wrong-rate-limit')
    const submitButton = signInCard.getByRole('button', { name: 'Enter Platform' })
    const responsePromise = page.waitForResponse((response) => (
      response.request().method() === 'POST'
        && /\/api\/auth\/session\/login$/i.test(response.url())
    ), { timeout: 20_000 })

    await submitButton.click()
    const response = await responsePromise

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

    await expectSignInErrorAndRecovery(signInCard, invalidMessage)
  }

  throw new Error(`Expected repeated real browser sign-in attempts to reach the hosted auth rate-limit threshold within ${authRateLimitAttemptBudget} tries.`)
}

async function readReplayOutcome(api, externalOrderId) {
  const replayQueue = await readJson(await api.get('/api/integrations/orders/replay-queue'), {
    method: 'GET',
    url: '/api/integrations/orders/replay-queue',
    requestPayload: {
      externalOrderId,
    },
    note: 'Replay queue lookup while verifying hosted replay fixture.',
  })
  const replayRecord = replayQueue.find((record) => record.externalOrderId === externalOrderId)
  if (replayRecord) {
    return { state: 'queued', status: replayRecord.status, record: replayRecord }
  }

  const recentOrders = await readJson(await api.get('/api/orders/recent'), {
    method: 'GET',
    url: '/api/orders/recent',
    requestPayload: {
      externalOrderId,
    },
    note: 'Recent orders lookup while verifying hosted replay fixture.',
  })
  if (recentOrders.some((order) => order.externalOrderId === externalOrderId)) {
    return { state: 'replayed' }
  }

  return { state: 'missing' }
}

function describeReplayOutcome(replayOutcome) {
  if (!replayOutcome || replayOutcome.state === 'missing') {
    return 'missing'
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
  await expect.poll(async () => {
    const replayOutcome = await readReplayOutcome(api, externalOrderId)
    return replayOutcome.state === 'queued' ? `${replayOutcome.state}:${replayOutcome.status}` : replayOutcome.state
  }, {
    timeout,
    message,
  }).toBe('replayed')
}

async function readReplayPageDiagnostics(page, replayFixture) {
  const backendConnectors = await readJson(await replayFixture.api.get('/api/integrations/orders/connectors'), {
    method: 'GET',
    url: '/api/integrations/orders/connectors',
    requestPayload: {
      sourceSystem: replayFixture.sourceSystem,
    },
    note: `Replay connector diagnostics for ${replayFixture.sourceSystem}.`,
  })
  const backendReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)

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
  }
}

async function waitForReplayButtonReady(page, replayFixture) {
  const replayQueueRecord = page.locator('.signal-list-item.selectable-card').filter({
    hasText: replayFixture.externalOrderId,
  }).first()

  const startedAt = Date.now()
  let lastRefreshAt = 0

  while (Date.now() - startedAt < 30_000) {
    const replayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
    if (describeReplayOutcome(replayOutcome) !== 'queued:PENDING') {
      const diagnostics = await readReplayPageDiagnostics(page, replayFixture)
      throw new Error(`Expected ${replayFixture.externalOrderId} to remain manually replayable while waiting for the replay button to enable. Diagnostics: ${JSON.stringify(diagnostics)}`)
    }

    if (Date.now() - lastRefreshAt >= 2_500) {
      lastRefreshAt = Date.now()
      await refreshWorkspace(page)
    }

    if (await replayQueueRecord.isVisible().catch(() => false)) {
      await replayQueueRecord.click().catch(() => {})
    }

    const replayDetail = page.locator('.section-card')
      .filter({ hasText: 'Recovery detail' })
      .filter({ hasText: replayFixture.externalOrderId })
      .last()
    const replayButton = replayDetail.getByRole('button', { name: 'Replay Into Live Flow' }).first()

    if (await replayDetail.isVisible().catch(() => false) && await replayButton.isVisible().catch(() => false)) {
      const buttonState = await replayButton.evaluate((button) => ({
        disabled: button.disabled,
        ariaDisabled: button.getAttribute('aria-disabled') || '',
      })).catch(() => null)

      if (buttonState && buttonState.disabled === false && buttonState.ariaDisabled !== 'true') {
        return replayButton
      }
    }

    await page.waitForTimeout(500)
  }

  const diagnostics = await readReplayPageDiagnostics(page, replayFixture)
  throw new Error(`Expected Replay Into Live Flow to become enabled after connector ${replayFixture.sourceSystem} was re-enabled and the replay queue refreshed. Diagnostics: ${JSON.stringify(diagnostics)}`)
}

async function waitForUsersPageReady(page, expectedOperatorName, expectedUserFullName) {
  const operatorLane = page.locator('.section-card').filter({ hasText: 'Operator lanes' }).first()
  const userRoster = page.locator('.section-card').filter({ hasText: 'User roster' }).first()

  await expect(operatorLane).toBeVisible()
  await expect(userRoster).toBeVisible()
  await expect(operatorLane.getByText(expectedOperatorName).first()).toBeVisible({ timeout: 30_000 })
  await expect(userRoster.getByText(expectedUserFullName).first()).toBeVisible({ timeout: 30_000 })
}

async function createReplayFixture() {
  const inventoryAdmin = await createApiContext(users.operationsLead)
  const api = await createApiContext(users.operationsLead)
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
        await readJson(await api.post('/api/integrations/orders/connectors', {
          data: enableConnectorPayload,
        }), {
          method: 'POST',
          url: '/api/integrations/orders/connectors',
          requestPayload: enableConnectorPayload,
          note: 'Hosted replay fixture connector enable before manual replay.',
        })

        await expect.poll(async () => {
          const connectors = await readJson(await api.get('/api/integrations/orders/connectors'), {
            method: 'GET',
            url: '/api/integrations/orders/connectors',
            requestPayload: {
              sourceSystem,
            },
            note: 'Hosted replay fixture connector enabled verification.',
          })
          return connectors.find((connector) => connector.sourceSystem === sourceSystem && connector.type === 'CSV_ORDER_IMPORT')?.enabled ?? false
        }, {
          timeout: 15_000,
          message: `Expected replay verification connector ${sourceSystem} to become enabled before manual UI replay.`,
        }).toBe(true)
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
    requestedBy: 'Operations Lead',
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
      scenarioId: payload.id,
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

async function ensureRecentOrder(api) {
  const recentOrders = await readJson(await api.get('/api/orders/recent'))
  if (recentOrders.length) {
    return recentOrders
  }

  const suffix = randomUUID().slice(0, 8).toUpperCase()
  await readJson(await api.post('/api/orders', {
    data: {
      externalOrderId: `UI-ORD-${suffix}`,
      warehouseCode: 'WH-NORTH',
      items: [
        {
          productSku: proofProductSku,
          quantity: 1,
          unitPrice: 79,
        },
      ],
    },
  }))

  let nextOrders = []
  await expect.poll(async () => {
    nextOrders = await readJson(await api.get('/api/orders/recent'))
    return nextOrders.length > 0
  }, {
    timeout: 30_000,
    message: 'Expected at least one recent order to appear after seeding the hosted proof order lane.',
  }).toBe(true)
  return nextOrders
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

async function ensureAlertAndRecommendationCoverage(api) {
  const inventory = await readJson(await api.get('/api/inventory'))
  const candidate = inventory.find((item) => item.productSku === proofProductSku && item.warehouseCode === 'WH-NORTH')
    || inventory.find((item) => Number.isFinite(item.quantityAvailable) && Number.isFinite(item.reorderThreshold))

  expect(candidate).toBeTruthy()

  const findCoverage = (snapshot) => {
    const activeAlerts = activeAlertsFromSnapshot(snapshot)
    return {
      alertRecord: activeAlerts.find((alert) => alertReferencesSku(alert, candidate.productSku)),
      recommendationRecord: snapshot.recommendations.find((recommendation) => recommendationReferencesSku(recommendation, candidate.productSku)),
    }
  }

  const initialSnapshot = await readJson(await api.get('/api/dashboard/snapshot'))
  const initialCoverage = findCoverage(initialSnapshot)
  if (initialCoverage.alertRecord && initialCoverage.recommendationRecord) {
    return {
      snapshot: initialSnapshot,
      candidate,
      alertRecord: initialCoverage.alertRecord,
      recommendationRecord: initialCoverage.recommendationRecord,
      restore: async () => {},
    }
  }

  const revertQuantity = candidate.quantityAvailable
  const revertThreshold = candidate.reorderThreshold
  const threshold = Math.max(5, Number.isFinite(candidate.reorderThreshold) ? candidate.reorderThreshold : 5)
  let latestCoverage = null

  await readJson(await api.post('/api/inventory/update', {
    data: {
      productSku: candidate.productSku,
      warehouseCode: candidate.warehouseCode,
      quantityAvailable: Math.max(0, threshold - 1),
      reorderThreshold: threshold,
    },
  }))

  const snapshot = await waitForSnapshotMatch(
    api,
    (nextSnapshot) => {
      latestCoverage = findCoverage(nextSnapshot)
      return Boolean(latestCoverage.alertRecord && latestCoverage.recommendationRecord)
    },
    `Expected low-stock inventory on ${candidate.productSku} to produce matching alert and recommendation coverage from the live backend.`,
  )

  return {
    snapshot,
    candidate,
    alertRecord: latestCoverage?.alertRecord ?? null,
    recommendationRecord: latestCoverage?.recommendationRecord ?? null,
    restore: async () => {
      await readJson(await api.post('/api/inventory/update', {
        data: {
          productSku: candidate.productSku,
          warehouseCode: candidate.warehouseCode,
          quantityAvailable: revertQuantity,
          reorderThreshold: revertThreshold,
        },
      }))
    },
  }
}

test('auth flow and the full authenticated page system render cleanly in a browser', async ({ page }) => {
  await page.goto('/dashboard')
  await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  const signInCard = page.locator('.public-signin-card')
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

  for (const [route, title] of appPages) {
    await navigateWithinApp(page, route)
    await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
    await expect(page.locator('.workspace-topbar')).toBeVisible()
    await expectNoFatalUiErrors(page)
  }

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

  await loginViaUi(page, users.operationsLead, { requireDashboardSnapshot: true })
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  await expect(page.getByText('Realtime state')).toBeVisible()
  await waitForRealtimeConnectionLive(page)

  try {
    const expectedAlertTitle = `Low stock detected for SKU ${realtimeFixture.productSku} in ${realtimeFixture.warehouseCode}`
    const expectedRecommendationTitle = `Urgent reorder for SKU ${realtimeFixture.productSku} at ${realtimeFixture.warehouseCode}`

    await readJson(await api.post('/api/inventory/update', {
      data: {
        productSku: realtimeFixture.productSku,
        warehouseCode: realtimeFixture.warehouseCode,
        quantityAvailable: realtimeFixture.lowQuantity,
        reorderThreshold: realtimeFixture.reorderThreshold,
      },
    }))

    await expect(page.getByText(expectedAlertTitle).first()).toBeVisible({ timeout: 30_000 })
    await expect(page.getByText(expectedRecommendationTitle).first()).toBeVisible({ timeout: 30_000 })
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

test('replay recovery, scenario approval, execution, and browser role gating work through the UI', async ({ page }) => {
  const replayFixture = await createReplayFixture()

  try {
    await loginViaUi(page, users.integrationLead)
    await navigateWithinApp(page, '/replay-queue')
    await expect(page.getByRole('heading', { level: 1, name: 'Failed inbound recovery' })).toBeVisible()

    let currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
    if (currentReplayOutcome.state === 'queued') {
      const replayQueueRecord = page.locator('.signal-list-item.selectable-card').filter({ hasText: replayFixture.externalOrderId }).first()
      await expect.poll(async () => {
        currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
        if (describeReplayOutcome(currentReplayOutcome) !== 'queued:PENDING') {
          return describeReplayOutcome(currentReplayOutcome)
        }
        await refreshWorkspace(page)
        return await replayQueueRecord.isVisible().catch(() => false) ? 'visible' : 'waiting'
      }, {
        timeout: 30_000,
        message: `Expected replay queue ${replayFixture.externalOrderId} to appear in the UI before any automated replay could mutate it.`,
      }).toBe('visible')

      await expect(replayQueueRecord).toBeVisible()
      await replayQueueRecord.click()

      const replayDetail = page.locator('.section-card').filter({ hasText: 'Recovery detail' }).first()
      await expect(replayDetail.getByText(replayFixture.externalOrderId).first()).toBeVisible()

      await replayFixture.enableConnector()
      await expect.poll(async () => {
        currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
        return describeReplayOutcome(currentReplayOutcome)
      }, {
        timeout: 20_000,
        message: `Expected ${replayFixture.externalOrderId} to remain queued for manual replay after enabling the replay connector.`,
      }).toBe('queued:PENDING')

      const replayButton = await waitForReplayButtonReady(page, replayFixture)

      const replayResponsePromise = page.waitForResponse((response) => (
        response.request().method() === 'POST'
          && /\/api\/integrations\/orders\/replay\/\d+$/i.test(response.url())
      ), { timeout: 20_000 })

      await replayButton.scrollIntoViewIfNeeded()
      const [replayResponse] = await Promise.all([
        replayResponsePromise,
        replayButton.click(),
      ])

      const replayResponseText = await replayResponse.text()
      let replayPayload = null
      try {
        replayPayload = replayResponseText ? JSON.parse(replayResponseText) : null
      } catch {
        replayPayload = null
      }
      if (!replayResponse.ok()) {
        throw new Error(JSON.stringify({
          method: 'POST',
          url: replayResponse.url(),
          status: replayResponse.status(),
          requestId: replayResponse.headers()['x-request-id'] || replayPayload?.requestId || null,
          responseBody: replayPayload ?? replayResponseText,
          requestPayload: {
            replayRecordExternalOrderId: replayFixture.externalOrderId,
          },
          note: `Replay request failed for ${replayFixture.externalOrderId}.`,
        }))
      }

      await waitForReplayResolution(
        replayFixture.api,
        replayFixture.externalOrderId,
        30_000,
        `Expected replay verification order ${replayFixture.externalOrderId} to recover into the live order flow.`,
      )

      await refreshWorkspace(page)
      await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
    }

    await waitForReplayResolution(
      replayFixture.api,
      replayFixture.externalOrderId,
      60_000,
      `Expected ${replayFixture.externalOrderId} to reach a replayed state through deterministic manual recovery.`,
    )

  await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
  } finally {
    await replayFixture.api.dispose()
  }

  await signOutViaUi(page)

  const scenarioFixture = await createScenarioFixture()

  try {
    await loginViaUi(page, users.operationsLead)
    await navigateWithinApp(page, '/scenario-history')
    await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()

    const scenarioHistoryCard = await waitForScenarioHistoryCard(page, scenarioFixture.title)
    await activateSelectableButton(scenarioHistoryCard)

    const scenarioActionConsole = page.locator('.section-card').filter({
      hasText: 'Scenario action console',
      has: page.getByText(scenarioFixture.title),
    }).first()
    await expect(scenarioActionConsole).toBeVisible()
    await scenarioActionConsole.getByRole('button', { name: 'Approve Plan' }).click()
    await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()

    await expect(scenarioActionConsole.getByRole('button', { name: 'Execute Scenario' })).toBeVisible()
    await scenarioActionConsole.getByRole('button', { name: 'Execute Scenario' }).click()
    await expect(page.locator('.success-text').filter({ hasText: new RegExp(`^Executed ${scenarioFixture.title} as live order `, 'i') }).first()).toBeVisible()
  } finally {
    await scenarioFixture.api.dispose()
  }

  await signOutViaUi(page)

  await loginViaUi(page, users.operationsPlanner)
  await page.goto('/users')
  await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
  await expect(page.getByText('Tenant admin access required')).toBeVisible()
  await expect(page.getByText('Operators', { exact: true }).first()).toBeVisible()
})

test('alerts, recommendations, orders, inventory, integrations, users, profile, and settings surfaces stay connected to the live backend', async ({ page }) => {
  const api = await createApiContext(users.operationsLead)
  let restoreAlertCoverage = async () => {}

  try {
    const alertCoverage = await ensureAlertAndRecommendationCoverage(api)
    restoreAlertCoverage = alertCoverage.restore
    const recentOrders = await ensureRecentOrder(api)
    const workspace = await readJson(await api.get('/api/access/admin/workspace'))
    const operators = await readJson(await api.get('/api/access/admin/operators'))
    const accessUsers = await readJson(await api.get('/api/access/admin/users'))
    const alertRecord = alertCoverage.alertRecord
    const recommendationRecord = alertCoverage.recommendationRecord
    const orderRecord = recentOrders[0]
    const inventoryRecord = alertCoverage.snapshot.inventory.find((item) => item.lowStock) || alertCoverage.snapshot.inventory[0]
    const connectorCandidates = [
      ...alertCoverage.snapshot.integrationConnectors,
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
    await expect(page.getByText(alertRecord.title).first()).toBeVisible()
    await activateSelectableButton(
      page.getByRole('button', { name: new RegExp(escapeRegExp(alertRecord.title), 'i') }).first(),
    )
    await expect(page.getByText(`Action: ${alertRecord.recommendedAction}`).first()).toBeVisible()

    await navigateWithinApp(page, '/recommendations')
    await expect(page.getByRole('heading', { level: 1, name: 'Action queue for the operating team' })).toBeVisible()
    await expect(page.getByText(recommendationRecord.title).first()).toBeVisible()
    await activateSelectableButton(
      page.locator('.recommendation-board').getByRole('button', { name: new RegExp(escapeRegExp(recommendationRecord.title), 'i') }).first(),
    )
    await expect(page.getByText(recommendationRecord.description).first()).toBeVisible()

    await navigateWithinApp(page, '/orders')
    await expect(page.getByRole('heading', { level: 1, name: 'Live order operations' })).toBeVisible()
    await expect(page.getByText(orderRecord.externalOrderId).first()).toBeVisible()
    await activateSelectableButton(
      page.getByRole('button', { name: new RegExp(escapeRegExp(orderRecord.externalOrderId), 'i') }).first(),
    )
    await expect(page.getByText(orderRecord.warehouseCode).first()).toBeVisible()

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
    await expect(page.getByLabel('Tenant Name').first()).toHaveValue(workspace.tenantName)
    if (workspace.connectors?.length) {
      await expect(page.getByText(workspace.connectors[0].displayName).first()).toBeVisible()
    }

    await navigateWithinApp(page, '/users')
    await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
    await waitForUsersPageReady(page, expectedVisibleOperator.displayName, expectedVisibleUser.fullName)

    await navigateWithinApp(page, '/profile')
    await expect(page.getByRole('heading', { level: 1, name: 'Personal profile and session controls' })).toBeVisible()
    await expect(page.getByText(users.operationsLead.username).first()).toBeVisible()
    await expect(page.getByText(workspace.tenantName).first()).toBeVisible()

    await expectNoFatalUiErrors(page)
  } finally {
    await restoreAlertCoverage()
    await api.dispose()
  }
})

test('frontend surfaces backend auth rate limiting without getting stuck in a loading state', async ({ page }) => {
  await page.goto('/sign-in')
  await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  const signInCard = page.locator('.public-signin-card')
  await waitForSignInReady(signInCard)

  await triggerUiAuthRateLimit(page, signInCard, users.operationsLead)
})
