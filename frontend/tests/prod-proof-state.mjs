import path from 'node:path'
import fs from 'node:fs'

const authRateLimitWindowSeconds = Number.parseInt(
  process.env.PLAYWRIGHT_AUTH_RATE_LIMIT_WINDOW_SECONDS
    || process.env.SYNAPSECORE_RATE_LIMIT_AUTH_LOGIN_WINDOW_SECONDS
    || '60',
  10,
)

export const authRateLimitWindowMs = Number.isFinite(authRateLimitWindowSeconds) && authRateLimitWindowSeconds > 0
  ? authRateLimitWindowSeconds * 1000
  : 60_000

export const authRateLimitCooldownBufferMs = 5_000

export const hostedProofStatePath = path.resolve(process.cwd(), '.hosted-proof', 'hosted-proof-state.json')

export function readHostedProofStateSync() {
  try {
    return JSON.parse(fs.readFileSync(hostedProofStatePath, 'utf8'))
  } catch {
    return {}
  }
}
