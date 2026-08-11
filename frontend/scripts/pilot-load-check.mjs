import fs from 'node:fs'
import path from 'node:path'
import { performance } from 'node:perf_hooks'
import { setTimeout as sleep } from 'node:timers/promises'

import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const DEFAULT_READ_PATHS = [
  '/api/auth/session',
  '/api/dashboard/summary',
  '/api/dashboard/snapshot',
  '/api/orders/recent',
  '/api/inventory',
  '/api/alerts',
  '/api/recommendations',
  '/api/integrations/orders/connectors',
  '/api/integrations/orders/replay-queue',
  '/api/scenarios/history',
  '/api/scenarios/notifications',
  '/api/system/runtime',
]

const METRIC_NAMES = [
  'process.cpu.usage',
  'system.cpu.usage',
  'jvm.memory.used',
  'jvm.memory.max',
  'jvm.threads.live',
  'hikaricp.connections.active',
  'hikaricp.connections.idle',
  'hikaricp.connections.pending',
  'hikaricp.connections.max',
  'http.server.requests.active',
]

const args = parseArgs(process.argv.slice(2))
const baseUrl = normalizeUrl(args.backendUrl || process.env.PLAYWRIGHT_API_BASE_URL || process.env.SYNAPSECORE_BACKEND_URL || 'http://127.0.0.1:8080')
const tenantCode = args.tenant || process.env.SYNAPSECORE_LOAD_TENANT_CODE || 'STARTER-OPS'
const username = args.username || process.env.SYNAPSECORE_LOAD_USERNAME || 'operations.lead'
const password = args.password || process.env.SYNAPSECORE_LOAD_PASSWORD || 'lead-2026'
const runId = args.runId || new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
const outputDir = path.resolve(args.outputDir || process.env.SYNAPSECORE_LOAD_OUTPUT_DIR || path.join('test-results', 'pilot-load'))
const warmupSeconds = Number(args.warmupSeconds || 10)
const durationSeconds = Number(args.durationSeconds || 60)
const soakSeconds = Number(args.soakSeconds || 300)
const thinkMinMs = Number(args.thinkMinMs || 250)
const thinkMaxMs = Number(args.thinkMaxMs || 750)
const loginPauseMs = Number(args.loginPauseMs || 6500)
const wsEventWaitMs = Number(args.wsEventWaitMs || 10000)
const resourceSampleSeconds = Number(args.resourceSampleSeconds || 5)
const httpStages = parseStages(args.stages || '1,3,5,10,15,25')
const wsStages = parseStages(args.wsStages || '1,5,10,25,50')
const mutationUsers = Number(args.mutationUsers || 3)
const productSkuPrefix = args.productSkuPrefix || `LOAD-${runId}`
const orderSku = args.orderSku || process.env.SYNAPSECORE_LOAD_ORDER_SKU || 'SKU-FLX-100'
const warehouseCode = args.warehouseCode || process.env.SYNAPSECORE_LOAD_WAREHOUSE_CODE || 'WH-NORTH'
const skipHttp = flagEnabled(args.skipHttp)
const skipWs = flagEnabled(args.skipWs)
const skipSoak = flagEnabled(args.skipSoak)

fs.mkdirSync(outputDir, { recursive: true })

const finalReport = {
  runId,
  startedAt: new Date().toISOString(),
  tool: 'node-fetch + @stomp/stompjs + sockjs-client',
  backendUrl: baseUrl,
  tenantCode,
  thresholds: {
    httpSuccessRate: 0.99,
    readP95MsTarget: 500,
    realtimeP95MsTarget: 1000,
    fatal5xxPreferred: 0,
  },
  environment: {},
  datasetBefore: null,
  datasetAfter: null,
  http: [],
  mutation: null,
  websocket: [],
  soak: null,
  integrity: null,
  notes: [],
}

let mutationCounter = 0

try {
  console.log('========================================')
  console.log('SYNAPSCORE PILOT LOAD CHECK')
  console.log('========================================')
  console.log(`Backend URL : ${baseUrl}`)
  console.log(`Tenant      : ${tenantCode}`)
  console.log(`Run ID      : ${runId}`)
  console.log(`Output dir  : ${outputDir}`)
  console.log('')

  await waitForReadiness()
  const setupSession = await login()
  finalReport.environment = await collectEnvironment(setupSession)
  finalReport.datasetBefore = await collectDatasetCounts(setupSession)

  if (!skipHttp) {
    const maxHttpUsers = Math.max(...httpStages, mutationUsers)
    const sessionPool = await createSessionPool(maxHttpUsers, [setupSession])
    for (const users of httpStages) {
      finalReport.http.push(await runHttpStage({ label: `read-${users}`, users, scenario: 'read', durationSeconds, warmupSeconds, sessionPool }))
    }
    finalReport.mutation = await runHttpStage({ label: `mutation-${mutationUsers}`, users: mutationUsers, scenario: 'mutation', durationSeconds, warmupSeconds, sessionPool })
    if (!skipSoak) {
      const maxUsers = httpStages[httpStages.length - 1]
      finalReport.soak = await runHttpStage({ label: `soak-read-${maxUsers}`, users: maxUsers, scenario: 'read', durationSeconds: soakSeconds, warmupSeconds, sessionPool })
    }
  }

  if (!skipWs) {
    for (const sessions of wsStages) {
      finalReport.websocket.push(await runWebSocketStage(sessions))
    }
  }

  finalReport.datasetAfter = await collectDatasetCounts()
  finalReport.integrity = await collectIntegrity()
  finalReport.finishedAt = new Date().toISOString()
  finalReport.summary = summarizeReport(finalReport)

  const outputFile = path.join(outputDir, `gate3-load-${runId}.json`)
  fs.writeFileSync(outputFile, `${JSON.stringify(finalReport, null, 2)}\n`)
  printSummary(finalReport, outputFile)
} catch (error) {
  finalReport.finishedAt = new Date().toISOString()
  finalReport.error = error.stack || String(error)
  const outputFile = path.join(outputDir, `gate3-load-${runId}-failed.json`)
  fs.writeFileSync(outputFile, `${JSON.stringify(finalReport, null, 2)}\n`)
  console.error(error.stack || error.message || error)
  console.error(`Failure report written to ${outputFile}`)
  process.exitCode = 1
}

function parseArgs(argv) {
  const parsed = {}
  for (let index = 0; index < argv.length; index += 1) {
    const item = argv[index]
    if (!item.startsWith('--')) {
      continue
    }
    const key = item.slice(2)
    const next = argv[index + 1]
    if (!next || next.startsWith('--')) {
      parsed[key] = true
    } else {
      parsed[key] = next
      index += 1
    }
  }
  return parsed
}

function parseStages(value) {
  return String(value)
    .split(',')
    .map((entry) => Number(entry.trim()))
    .filter((entry) => Number.isFinite(entry) && entry > 0)
}

function flagEnabled(value) {
  return value === true || String(value).toLowerCase() === 'true' || String(value) === '1'
}

function normalizeUrl(url) {
  return String(url).replace(/\/+$/, '')
}

async function waitForReadiness() {
  for (let attempt = 1; attempt <= 30; attempt += 1) {
    try {
      const response = await fetch(`${baseUrl}/actuator/health/readiness`)
      if (response.ok) {
        return
      }
    } catch {
      // Retry while the local backend finishes warm-up.
    }
    await sleep(2000)
  }
  throw new Error('Backend readiness did not become healthy before load test.')
}

async function collectEnvironment(session) {
  const [health, runtime, metrics] = await Promise.all([
    getJson('/actuator/health/readiness', null, { allowPublic: true }),
    getJson('/api/system/runtime', session, { allowPublic: false }).catch((error) => ({ error: error.message })),
    sampleMetrics(),
  ])
  return {
    health,
    runtime,
    metrics,
    nodeVersion: process.version,
    warmupSeconds,
    durationSeconds,
    soakSeconds,
    httpStages,
    wsStages,
  }
}

async function collectDatasetCounts(session = null) {
  const effectiveSession = session || await login()
  const endpoints = {
    products: '/api/products',
    inventory: '/api/inventory',
    orders: '/api/orders/recent',
    alerts: '/api/alerts',
    recommendations: '/api/recommendations',
    connectors: '/api/integrations/orders/connectors',
    replay: '/api/integrations/orders/replay-queue',
    scenarios: '/api/scenarios/history',
    users: '/api/access/admin/users',
  }

  const counts = {}
  for (const [key, endpoint] of Object.entries(endpoints)) {
    const payload = await getJson(endpoint, effectiveSession)
    counts[key] = Array.isArray(payload) ? payload.length : 1
  }
  return counts
}

async function collectIntegrity() {
  const session = await login()
  const [products, orders, inventory, replay, scenarios] = await Promise.all([
    getJson('/api/products', session),
    getJson('/api/orders/recent', session),
    getJson('/api/inventory', session),
    getJson('/api/integrations/orders/replay-queue', session),
    getJson('/api/scenarios/history', session),
  ])
  const productSkus = products.map((product) => product.sku || product.catalogSku).filter(Boolean)
  const duplicateProductSkus = productSkus.length - new Set(productSkus).size
  const impossibleInventoryRows = inventory.filter((row) => Number(row.quantityAvailable ?? row.availableQuantity ?? 0) < 0).length

  return {
    duplicateProductSkus,
    impossibleInventoryRows,
    ordersReadable: Array.isArray(orders),
    replayReadable: Array.isArray(replay),
    scenariosReadable: Array.isArray(scenarios),
  }
}

async function login() {
  const startedAt = performance.now()
  const response = await fetch(`${baseUrl}/api/auth/session/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tenantCode, username, password }),
  })
  const body = await response.text()
  if (!response.ok) {
    throw new Error(`Login failed with HTTP ${response.status}: ${body.slice(0, 180)}`)
  }
  const cookie = parseCookie(response.headers)
  if (!cookie) {
    throw new Error('Login succeeded but no session cookie was returned.')
  }
  return { cookie, loginMs: performance.now() - startedAt }
}

async function createSessionPool(users, existingSessions = []) {
  console.log(`Creating ${users} reusable authenticated operator session(s) with ${loginPauseMs}ms setup spacing.`)
  const sessions = [...existingSessions]
  if (sessions.length > 0 && sessions.length < users && loginPauseMs > 0) {
    await sleep(loginPauseMs)
  }
  for (let index = 0; index < users; index += 1) {
    if (sessions[index]) {
      continue
    }
    sessions.push(await login())
    if (index < users - 1 && loginPauseMs > 0) {
      await sleep(loginPauseMs)
    }
  }
  return sessions
}

function parseCookie(headers) {
  const setCookie = typeof headers.getSetCookie === 'function'
    ? headers.getSetCookie()[0]
    : headers.get('set-cookie')
  return setCookie ? setCookie.split(';')[0] : null
}

async function getJson(endpoint, session, options = {}) {
  const headers = {}
  if (session?.cookie) {
    headers.Cookie = session.cookie
  } else if (!options.allowPublic) {
    throw new Error(`Session is required for ${endpoint}`)
  }
  const response = await fetch(`${baseUrl}${endpoint}`, { headers })
  const text = await response.text()
  if (!response.ok) {
    throw new Error(`${endpoint} failed with HTTP ${response.status}: ${text.slice(0, 180)}`)
  }
  return text ? JSON.parse(text) : null
}

async function request(endpoint, session, options = {}) {
  const startedAt = performance.now()
  let status = 0
  let error = null
  let bytes = 0
  try {
    const headers = { ...(options.headers || {}) }
    if (session?.cookie) {
      headers.Cookie = session.cookie
    }
    const response = await fetch(`${baseUrl}${endpoint}`, {
      method: options.method || 'GET',
      headers,
      body: options.body,
    })
    status = response.status
    const text = await response.text()
    bytes = text.length
  } catch (requestError) {
    error = requestError.message
  }
  return {
    endpoint,
    method: options.method || 'GET',
    status,
    error,
    bytes,
    latencyMs: performance.now() - startedAt,
  }
}

async function runHttpStage({ label, users, scenario, durationSeconds, warmupSeconds, sessionPool }) {
  console.log(`HTTP stage ${label}: users=${users} scenario=${scenario} warmup=${warmupSeconds}s duration=${durationSeconds}s`)
  const sessions = sessionPool.slice(0, users)
  const loginResults = sessions.map((session) => ({ latencyMs: session.loginMs, status: 200, endpoint: '/api/auth/session/login', method: 'POST' }))

  const beforeMetrics = await sampleMetrics()
  const resourceSamples = []
  const sampler = setInterval(() => {
    sampleMetrics()
      .then((metrics) => resourceSamples.push({ sampledAt: new Date().toISOString(), metrics }))
      .catch(() => {})
  }, Math.max(1000, resourceSampleSeconds * 1000))
  await runWorkers({ sessions, scenario, durationMs: warmupSeconds * 1000, collect: false })
  const stageResults = await runWorkers({ sessions, scenario, durationMs: durationSeconds * 1000, collect: true })
  clearInterval(sampler)
  const afterMetrics = await sampleMetrics()
  const summary = summarizeSamples(stageResults.samples, durationSeconds)
  const authSummary = summarizeSamples(loginResults, Math.max(1, loginResults.length))
  const byEndpoint = summarizeByEndpoint(stageResults.samples)
  const stage = {
    label,
    users,
    scenario,
    warmupSeconds,
    durationSeconds,
    startedAt: new Date().toISOString(),
    summary,
    authSummary,
    byEndpoint,
    resources: {
      before: beforeMetrics,
      after: afterMetrics,
      samples: resourceSamples,
      peaks: summarizeMetricPeaks(resourceSamples.map((sample) => sample.metrics)),
    },
  }
  console.log(formatStageLine(stage))
  return stage
}

async function runWorkers({ sessions, scenario, durationMs, collect }) {
  const until = performance.now() + durationMs
  const samples = []
  await Promise.all(sessions.map((session, index) => workerLoop({ session, index, scenario, until, samples, collect })))
  return { samples }
}

async function workerLoop({ session, index, scenario, until, samples, collect }) {
  let iteration = 0
  while (performance.now() < until) {
    const operations = operationsForScenario({ scenario, userIndex: index, iteration })
    for (const operation of operations) {
      if (performance.now() >= until) {
        break
      }
      const sample = await request(operation.endpoint, session, operation.options)
      if (collect) {
        samples.push(sample)
      }
      await sleep(randomInt(thinkMinMs, thinkMaxMs))
    }
    iteration += 1
  }
}

function operationsForScenario({ scenario, userIndex, iteration }) {
  if (scenario === 'mutation') {
    const uniqueId = `${userIndex}-${iteration}-${mutationCounter++}`
    const sku = `${productSkuPrefix}-${uniqueId}`
    const externalOrderId = `${productSkuPrefix}-ORD-${uniqueId}`
    return [
      { endpoint: '/api/dashboard/summary' },
      {
        endpoint: '/api/products',
        options: {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ sku, name: `Load Test ${sku}`, category: 'Load Test' }),
        },
      },
      {
        endpoint: '/api/orders',
        options: {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            externalOrderId,
            warehouseCode,
            items: [{ productSku: orderSku, quantity: 1, unitPrice: 1.25 }],
          }),
        },
      },
      { endpoint: '/api/orders/recent' },
      { endpoint: '/api/inventory' },
    ]
  }
  const offset = (userIndex + iteration) % DEFAULT_READ_PATHS.length
  return [
    { endpoint: DEFAULT_READ_PATHS[offset] },
    { endpoint: DEFAULT_READ_PATHS[(offset + 3) % DEFAULT_READ_PATHS.length] },
    { endpoint: DEFAULT_READ_PATHS[(offset + 7) % DEFAULT_READ_PATHS.length] },
  ]
}

async function runWebSocketStage(sessions) {
  console.log(`WebSocket stage: sessions=${sessions}`)
  const clients = []
  const connections = []
  const deliveries = []
  const eventMarker = `WS-${runId}-${sessions}-${Date.now()}`
  let duplicatePayloads = 0
  let extraMessages = 0
  let missed = 0
  let triggerSample = null

  try {
    const sharedSession = await login()
    for (let index = 0; index < sessions; index += 1) {
      const startedAt = performance.now()
      const client = new Client({
        webSocketFactory: () => new SockJS(`${baseUrl}/ws`, null, {
          transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
        }),
        connectHeaders: {
          Cookie: sharedSession.cookie,
          'X-Synapse-Tenant': tenantCode,
        },
        reconnectDelay: 0,
        debug: () => {},
      })
      await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => reject(new Error('STOMP connect timeout')), 10000)
        client.onConnect = () => {
          clearTimeout(timeout)
          connections.push(performance.now() - startedAt)
          let delivered = false
          const seenPayloads = new Set()
          client.subscribe(`/topic/tenant/${tenantCode}/dashboard.summary`, (message) => {
            const latency = performance.now() - triggerStart
            if (seenPayloads.has(message.body)) {
              duplicatePayloads += 1
            }
            seenPayloads.add(message.body)
            if (delivered) {
              extraMessages += 1
              return
            }
            delivered = true
            deliveries.push(latency)
          })
          resolve()
        }
        client.onStompError = (frame) => reject(new Error(frame.headers?.message || 'STOMP error'))
        client.onWebSocketError = (error) => reject(error)
        client.activate()
      })
      clients.push(client)
    }

    var triggerStart = performance.now()
    triggerSample = await request('/api/orders', sharedSession, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        externalOrderId: eventMarker,
        warehouseCode,
        items: [{ productSku: orderSku, quantity: 1, unitPrice: 1.25 }],
      }),
    })
    await sleep(wsEventWaitMs)
    missed = Math.max(0, sessions - deliveries.length)
  } finally {
    await Promise.allSettled(clients.map((client) => client.deactivate()))
  }

  const stage = {
    sessions,
    attempted: sessions,
    established: connections.length,
    failed: sessions - connections.length,
    connection: summarizeNumbers(connections),
    eventDelivery: summarizeNumbers(deliveries),
    trigger: triggerSample,
    duplicatePayloads,
    extraMessages,
    missed,
  }
  console.log(`WS sessions=${sessions} established=${stage.established}/${stage.attempted} eventP95=${stage.eventDelivery.p95Ms ?? 'n/a'}ms missed=${missed} duplicatePayloads=${duplicatePayloads} extraMessages=${extraMessages}`)
  return stage
}

async function sampleMetrics() {
  const metrics = {}
  for (const name of METRIC_NAMES) {
    try {
      const response = await fetch(`${baseUrl}/actuator/metrics/${name}`)
      if (!response.ok) {
        continue
      }
      const payload = await response.json()
      metrics[name] = Object.fromEntries((payload.measurements || []).map((entry) => [entry.statistic, entry.value]))
    } catch {
      // Keep load checks useful even if a local actuator metric is absent.
    }
  }
  return metrics
}

function summarizeSamples(samples, sampleDurationSeconds = durationSeconds) {
  const total = samples.length
  const statuses = { success: 0, fourxx: 0, fivexx: 0, timeout: 0, network: 0 }
  const latencies = []
  for (const sample of samples) {
    if (sample.error) {
      statuses.network += 1
      if (sample.error.toLowerCase().includes('timeout')) {
        statuses.timeout += 1
      }
    } else if (sample.status >= 200 && sample.status < 400) {
      statuses.success += 1
      latencies.push(sample.latencyMs)
    } else if (sample.status >= 400 && sample.status < 500) {
      statuses.fourxx += 1
      latencies.push(sample.latencyMs)
    } else if (sample.status >= 500) {
      statuses.fivexx += 1
      latencies.push(sample.latencyMs)
    }
  }
  return {
    requests: total,
    rps: round(total / Math.max(1, sampleDurationSeconds)),
    successRate: total === 0 ? 0 : round(statuses.success / total),
    fourxxRate: total === 0 ? 0 : round(statuses.fourxx / total),
    fivexxRate: total === 0 ? 0 : round(statuses.fivexx / total),
    timeoutRate: total === 0 ? 0 : round(statuses.timeout / total),
    networkErrorRate: total === 0 ? 0 : round(statuses.network / total),
    ...summarizeNumbers(latencies),
  }
}

function summarizeByEndpoint(samples) {
  const grouped = new Map()
  for (const sample of samples) {
    const key = `${sample.method} ${sample.endpoint}`
    if (!grouped.has(key)) {
      grouped.set(key, [])
    }
    grouped.get(key).push(sample)
  }
  return Object.fromEntries(Array.from(grouped.entries()).map(([key, values]) => [key, summarizeSamples(values)]))
}

function summarizeNumbers(values) {
  const sorted = values.filter(Number.isFinite).sort((a, b) => a - b)
  if (sorted.length === 0) {
    return { count: 0, p50Ms: null, p95Ms: null, p99Ms: null, averageMs: null, maxMs: null }
  }
  const sum = sorted.reduce((total, value) => total + value, 0)
  return {
    count: sorted.length,
    p50Ms: round(percentile(sorted, 0.5)),
    p95Ms: round(percentile(sorted, 0.95)),
    p99Ms: round(percentile(sorted, 0.99)),
    averageMs: round(sum / sorted.length),
    maxMs: round(sorted[sorted.length - 1]),
  }
}

function percentile(sorted, percentileValue) {
  const index = Math.min(sorted.length - 1, Math.ceil(sorted.length * percentileValue) - 1)
  return sorted[Math.max(0, index)]
}

function summarizeReport(report) {
  const stableHttp = report.http.filter((stage) => stage.summary.successRate >= report.thresholds.httpSuccessRate && stage.summary.fivexxRate === 0 && stage.summary.timeoutRate === 0)
  const maxStable = stableHttp[stableHttp.length - 1] || null
  const stableWs = report.websocket.filter((stage) => stage.failed === 0 && stage.missed === 0)
  const maxStableWs = stableWs[stableWs.length - 1] || null
  return {
    maximumStableConcurrentOperators: maxStable?.users || 0,
    maximumStableRps: maxStable?.summary.rps || 0,
    p95AtMaximumStableMs: maxStable?.summary.p95Ms || null,
    p99AtMaximumStableMs: maxStable?.summary.p99Ms || null,
    maximumStableWebSocketSessions: maxStableWs?.sessions || 0,
    websocketP95AtMaximumStableMs: maxStableWs?.eventDelivery?.p95Ms || null,
    mutationIntegrityPassed: report.integrity
      ? report.integrity.duplicateProductSkus === 0 && report.integrity.impossibleInventoryRows === 0
      : false,
  }
}

function summarizeMetricPeaks(samples) {
  const peaks = {}
  for (const metricSet of samples) {
    for (const [metricName, measurements] of Object.entries(metricSet || {})) {
      for (const [statistic, value] of Object.entries(measurements || {})) {
        const key = `${metricName}.${statistic}`
        if (!Number.isFinite(value)) {
          continue
        }
        peaks[key] = Math.max(peaks[key] ?? Number.NEGATIVE_INFINITY, value)
      }
    }
  }
  return Object.fromEntries(Object.entries(peaks).map(([key, value]) => [key, round(value)]))
}

function printSummary(report, outputFile) {
  console.log('')
  console.log('========================================')
  console.log('GATE 3 LOAD SUMMARY')
  console.log('========================================')
  console.log(`Output file                 : ${outputFile}`)
  console.log(`Max stable HTTP operators   : ${report.summary.maximumStableConcurrentOperators}`)
  console.log(`Max stable RPS              : ${report.summary.maximumStableRps}`)
  console.log(`P95 at max stable HTTP      : ${report.summary.p95AtMaximumStableMs} ms`)
  console.log(`P99 at max stable HTTP      : ${report.summary.p99AtMaximumStableMs} ms`)
  console.log(`Max stable WS sessions      : ${report.summary.maximumStableWebSocketSessions}`)
  console.log(`WS p95 at max stable        : ${report.summary.websocketP95AtMaximumStableMs} ms`)
  console.log(`Mutation integrity passed   : ${report.summary.mutationIntegrityPassed}`)
}

function formatStageLine(stage) {
  return [
    `HTTP ${stage.label}`,
    `requests=${stage.summary.requests}`,
    `rps=${stage.summary.rps}`,
    `success=${stage.summary.successRate}`,
    `4xx=${stage.summary.fourxxRate}`,
    `5xx=${stage.summary.fivexxRate}`,
    `p50=${stage.summary.p50Ms}ms`,
    `p95=${stage.summary.p95Ms}ms`,
    `p99=${stage.summary.p99Ms}ms`,
  ].join(' ')
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

function round(value) {
  return value == null ? null : Math.round(value * 100) / 100
}
