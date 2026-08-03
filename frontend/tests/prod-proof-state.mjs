import path from 'node:path'
import fs from 'node:fs'
import fsPromises from 'node:fs/promises'

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

export function parseHostedProofState(rawState) {
  return JSON.parse(String(rawState || '').replace(/^\uFEFF/, ''))
}

export function readHostedProofStateSync() {
  try {
    return parseHostedProofState(fs.readFileSync(hostedProofStatePath, 'utf8'))
  } catch {
    return {}
  }
}

export async function readHostedProofState() {
  try {
    return parseHostedProofState(await fsPromises.readFile(hostedProofStatePath, 'utf8'))
  } catch {
    return {}
  }
}

export async function writeHostedProofState(nextState) {
  await fsPromises.mkdir(path.dirname(hostedProofStatePath), { recursive: true })
  const currentState = await readHostedProofState()
  await fsPromises.writeFile(
    hostedProofStatePath,
    JSON.stringify({ ...currentState, ...nextState }, null, 2),
    'utf8',
  )
}
