import { randomUUID } from 'node:crypto'
import { expect, request as playwrightRequest, test } from '@playwright/test'

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

async function readJson(response) {
  const payload = await response.json()
  if (!response.ok()) {
    throw new Error(payload.message || `Request failed with status ${response.status()}.`)
  }
  return payload
}

async function loginViaUi(page, credentials) {
  await page.goto('/sign-in')
  await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  const signInCard = page.locator('.public-signin-card')
  await waitForSignInReady(signInCard)
  await fillSignInForm(signInCard, credentials, credentials.password)
  await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
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

async function readReplayOutcome(api, externalOrderId) {
  const replayQueue = await readJson(await api.get('/api/integrations/orders/replay-queue'))
  const replayRecord = replayQueue.find((record) => record.externalOrderId === externalOrderId)
  if (replayRecord) {
    return { state: 'queued', status: replayRecord.status, record: replayRecord }
  }

  const recentOrders = await readJson(await api.get('/api/orders/recent'))
  if (recentOrders.some((order) => order.externalOrderId === externalOrderId)) {
    return { state: 'replayed' }
  }

  return { state: 'missing' }
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

async function createReplayFixture() {
  const inventoryAdmin = await createApiContext(users.operationsLead)
  const api = await createApiContext(users.integrationLead)
  const suffix = randomUUID().slice(0, 8).toUpperCase()
  const sourceSystem = `ui_replay_${suffix}`.toLowerCase()
  const externalOrderId = `UI-RPL-${suffix}`

  try {
    await readJson(await inventoryAdmin.post('/api/inventory/update', {
      data: {
        productSku: proofProductSku,
        warehouseCode: 'WH-NORTH',
        quantityAvailable: 50,
        reorderThreshold: 12,
      },
    }))

    await readJson(await api.post('/api/integrations/orders/connectors', {
      data: {
        sourceSystem,
        type: 'CSV_ORDER_IMPORT',
        displayName: `UI Replay ${suffix}`,
        enabled: false,
        syncMode: 'BATCH_FILE_DROP',
        validationPolicy: 'RELAXED',
        transformationPolicy: 'NORMALIZE_CODES',
        allowDefaultWarehouseFallback: false,
        notes: 'Disposable replay verification connector.',
      },
    }))

    const csvImportResponse = await api.post('/api/integrations/orders/csv-import', {
      multipart: {
        file: {
          name: 'orders.csv',
          mimeType: 'text/csv',
          buffer: Buffer.from(
            `externalOrderId,warehouseCode,productSku,quantity,unitPrice\n${externalOrderId},WH-NORTH,${proofProductSku},2,88.00\n`,
            'utf8',
          ),
        },
        sourceSystem,
      },
    })
    const csvImportPayload = await readJson(csvImportResponse)
    expect(csvImportPayload.ordersFailed).toBe(1)

    await readJson(await api.post('/api/integrations/orders/connectors', {
      data: {
        sourceSystem,
        type: 'CSV_ORDER_IMPORT',
        displayName: `UI Replay ${suffix}`,
        enabled: true,
        syncMode: 'BATCH_FILE_DROP',
        validationPolicy: 'RELAXED',
        transformationPolicy: 'NORMALIZE_CODES',
        allowDefaultWarehouseFallback: false,
        notes: 'Enabled for replay verification.',
      },
    }))

    await expect.poll(async () => {
      const connectors = await readJson(await api.get('/api/integrations/orders/connectors'))
      return connectors.find((connector) => connector.sourceSystem === sourceSystem && connector.type === 'CSV_ORDER_IMPORT')?.enabled ?? false
    }, {
      timeout: 30_000,
      message: `Expected replay verification connector ${sourceSystem} to become enabled before UI replay proof.`,
    }).toBe(true)

    await expect.poll(async () => {
      const replayQueue = await readJson(await api.get('/api/integrations/orders/replay-queue'))
      const replayRecord = replayQueue.find((record) => record.externalOrderId === externalOrderId)
      if (!replayRecord) {
        return 'missing'
      }
      if (replayRecord.nextEligibleAt && Date.parse(replayRecord.nextEligibleAt) > Date.now()) {
        return 'waiting'
      }
      return replayRecord.status
    }, {
      timeout: 30_000,
      message: `Expected replay verification record ${externalOrderId} to be present and eligible before UI replay proof.`,
    }).toBe('PENDING')

    return { api, sourceSystem, externalOrderId }
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

  try {
    await readJson(await api.post('/api/products', {
      data: {
        sku: productSku,
        name: `Scenario Proof ${suffix}`,
        category: 'Verification',
      },
    }))

    await readJson(await api.post('/api/inventory/update', {
      data: {
        productSku,
        warehouseCode,
        quantityAvailable: 40,
        reorderThreshold: 10,
      },
    }))

    const payload = await readJson(await api.post('/api/scenarios/save', {
      data: {
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
      },
    }))

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
  await expect(signInCard.getByRole('button', { name: 'Enter Platform' })).toBeEnabled({ timeout: 60_000 })
  await expect(signInCard.getByText('Invalid operator credentials.')).toBeVisible({ timeout: 15_000 })

  await waitForSignInReady(signInCard)
  await fillSignInForm(signInCard, users.operationsLead, users.operationsLead.password)
  await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await page.reload()
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()

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

  await loginViaUi(page, users.operationsLead)
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  await expect(page.getByText('Realtime state')).toBeVisible()

  try {
    const beforeRisk = await waitForNumericSummaryCard(page, 'Risk')
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
    await expect.poll(async () => summaryCardValue(page, 'Risk'), {
      timeout: 30_000,
      message: `Expected the dashboard low-stock summary to increase through the live websocket path for ${realtimeFixture.productSku}.`,
    }).toBeGreaterThanOrEqual(beforeRisk + 1)
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
        if (currentReplayOutcome.state === 'replayed') {
          return 'replayed'
        }
        await refreshWorkspace(page)
        return await replayQueueRecord.isVisible().catch(() => false) ? 'visible' : 'waiting'
      }, {
        timeout: 30_000,
        message: `Expected replay queue ${replayFixture.externalOrderId} to appear in the UI or auto-recover before manual replay.`,
      }).not.toBe('waiting')

      if (currentReplayOutcome.state === 'replayed') {
        await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
      } else {
        await expect(replayQueueRecord).toBeVisible()
        await replayQueueRecord.click()

        const replayDetail = page.locator('.section-card').filter({ hasText: 'Recovery detail' }).first()
        await expect(replayDetail.getByText(replayFixture.externalOrderId).first()).toBeVisible()

        const replayButton = replayDetail.getByRole('button', { name: 'Replay Into Live Flow' })
        await expect(replayButton).toBeVisible()
        await expect(replayButton).toBeEnabled()

        const replayResponsePromise = page.waitForResponse((response) => (
          response.request().method() === 'POST'
            && /\/api\/integrations\/orders\/replay\/\d+$/i.test(response.url())
        ), { timeout: 20_000 })

        let replayResponse = null
        try {
          await replayButton.scrollIntoViewIfNeeded()
          ;[replayResponse] = await Promise.all([
            replayResponsePromise,
            replayButton.click(),
          ])
        } catch (error) {
          currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
          if (currentReplayOutcome.state !== 'replayed') {
            throw error
          }
        }

        if (replayResponse) {
          const replayPayload = await replayResponse.json().catch(() => null)
          if (!replayResponse.ok()) {
            const replayFailureMessage = replayPayload?.message
              || `Replay request failed with status ${replayResponse.status()} for ${replayFixture.externalOrderId}.`

            let replayResolvedAfterConflict = false
            try {
              await waitForReplayResolution(
                replayFixture.api,
                replayFixture.externalOrderId,
                20_000,
                `Expected ${replayFixture.externalOrderId} to settle into a replayed state after replay response ${replayResponse.status()}.`,
              )
              replayResolvedAfterConflict = true
            } catch {
              replayResolvedAfterConflict = false
            }

            if (!replayResolvedAfterConflict) {
              throw new Error(replayFailureMessage)
            }

            await refreshWorkspace(page)
            await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
          } else {
            await expect(page.locator('.success-text').filter({
              hasText: `Replayed ${replayFixture.externalOrderId} into the live order flow.`,
            }).first()).toBeVisible()
          }
        }
      }
    }

    await waitForReplayResolution(
      replayFixture.api,
      replayFixture.externalOrderId,
      60_000,
      `Expected ${replayFixture.externalOrderId} to reach a replayed state through manual or automated recovery.`,
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
  await navigateWithinApp(page, '/users')
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
    await expect(page.getByText(operators[0].displayName).first()).toBeVisible()
    await expect(page.getByText(accessUsers[0].fullName).first()).toBeVisible()

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

  const rateLimitApi = await playwrightRequest.newContext({ baseURL: backendUrl })

  try {
    let hitRateLimit = false
    for (let attempt = 0; attempt < 35; attempt += 1) {
      const response = await rateLimitApi.post('/api/auth/session/login', {
        data: {
          tenantCode: users.operationsLead.tenantCode,
          username: users.operationsLead.username,
          password: 'wrong-rate-limit',
        },
      })
      if (response.status() === 429) {
        hitRateLimit = true
        break
      }
    }

    expect(hitRateLimit).toBeTruthy()

    await fillSignInForm(signInCard, users.operationsLead, 'wrong-rate-limit')
    await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
    await expect(signInCard.getByRole('button', { name: 'Enter Platform' })).toBeEnabled({ timeout: 60_000 })
    await expect(signInCard.getByText('Authentication rate limit exceeded. Wait before attempting another sign-in.')).toBeVisible({ timeout: 15_000 })
  } finally {
    await rateLimitApi.dispose()
  }
})
