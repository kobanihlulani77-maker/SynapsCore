import { performance } from 'node:perf_hooks'
import { request } from '@playwright/test'
import {
  authRateLimitCooldownBufferMs,
  authRateLimitWindowMs,
  readHostedProofStateSync,
} from './prod-proof-state.mjs'

const state = readHostedProofStateSync()
const tenantCode = state.PLAYWRIGHT_TENANT_CODE
const username = state.PLAYWRIGHT_TENANT_ADMIN_USERNAME
const password = state.PLAYWRIGHT_TENANT_ADMIN_PASSWORD
const baseURL = state.PLAYWRIGHT_API_BASE_URL

if (!tenantCode || !username || !password || !baseURL) {
  throw new Error('Existing hosted proof credentials are unavailable; no login attempted.')
}

const cooldownUntil = Number(state.authRateLimitTriggeredAt || 0)
  + authRateLimitWindowMs + authRateLimitCooldownBufferMs
if (Number(state.authRateLimitTriggeredAt || 0) > 0 && Date.now() < cooldownUntil) {
  throw new Error('Recorded proof login cooldown is active; no login attempted.')
}

const api = await request.newContext({
  baseURL,
  extraHTTPHeaders: { 'X-Synapse-Tenant': tenantCode },
  timeout: 30_000,
})

async function timed(method, path, options) {
  const startedAtUtc = new Date().toISOString()
  const started = performance.now()
  const response = await api[method](path, options)
  const result = {
    startedAtUtc,
    method: method.toUpperCase(),
    path,
    status: response.status(),
    durationMs: Math.round((performance.now() - started) * 10) / 10,
    requestId: response.headers()['x-request-id'] || null,
  }
  console.log(JSON.stringify(result))
  if (!response.ok()) {
    throw new Error(`${path} returned HTTP ${result.status}; stopped without retry`)
  }
  return response
}

try {
  const readiness = await timed('get', '/actuator/health/readiness')
  const session = await timed('get', '/api/auth/session')
  if (readiness.status() !== 200 || session.status() !== 200) {
    throw new Error('Warm baseline unavailable')
  }

  const login = await timed('post', '/api/auth/session/login', {
    data: { tenantCode, username, password },
  })
  if ((await login.json()).signedIn !== true) {
    throw new Error('Login did not establish a signed-in proof session')
  }

  await timed('get', '/api/dashboard/summary')
  await timed('get', '/api/dashboard/snapshot')
  console.log(`WARM_BASELINE_UTC=${new Date().toISOString()}`)

  for (let sample = 1; sample <= 5; sample += 1) {
    console.log(`SAMPLE=${sample}`)
    await timed('get', '/api/dashboard/summary')
    await timed('get', '/api/dashboard/snapshot')
  }
} finally {
  await api.dispose()
}
