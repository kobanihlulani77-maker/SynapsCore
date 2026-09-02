import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { createLatestRequestGate } from '../src/services/latestRequest.js'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(scriptDir, '..')

const deferred = () => {
  let resolve
  const promise = new Promise((nextResolve) => {
    resolve = nextResolve
  })
  return { promise, resolve }
}

const gate = createLatestRequestGate()
const state = { value: 'initial' }
const olderResponse = deferred()
const olderRequestId = gate.begin()
const olderApply = async () => {
  const value = await olderResponse.promise
  if (gate.isCurrent(olderRequestId)) state.value = value
}
const olderApplyPromise = olderApply()

const newerRequestId = gate.begin()
assert.equal(gate.isCurrent(newerRequestId), true)
if (gate.isCurrent(newerRequestId)) state.value = 'new-authoritative-state'
olderResponse.resolve('old-stale-state')
await olderApplyPromise
assert.equal(state.value, 'new-authoritative-state')

const realtimeSource = await fs.readFile(path.join(frontendRoot, 'src', 'hooks', 'useWorkspaceRealtime.js'), 'utf8')
for (const topic of [
  'inventory-topic',
  'orders-topic',
  'incidents-topic',
  'integrations-replay-topic',
  'scenario-notifications-topic',
]) {
  assert.match(realtimeSource, new RegExp(`scheduleAuthoritativeSnapshotRefresh\\('${topic}'\\)`))
}
assert.equal(realtimeSource.includes("scheduleAuthoritativeSnapshotRefresh('alerts-changed-topic')"), true)
assert.equal(realtimeSource.includes("scheduleAuthoritativeSnapshotRefresh('recommendations-changed-topic')"), true)
assert.doesNotMatch(realtimeSource, /mergeSnapshot\(\{ inventory:/)
assert.doesNotMatch(realtimeSource, /mergeSnapshot\(\{ recentOrders:/)
assert.doesNotMatch(realtimeSource, /refreshDecisionSurface/)

const bootstrapSource = await fs.readFile(path.join(frontendRoot, 'src', 'hooks', 'useWorkspaceBootstrap.js'), 'utf8')
assert.match(bootstrapSource, /replaySurfaceRequestGateRef/)
assert.match(bootstrapSource, /orderSurfaceRequestGateRef/)
assert.match(bootstrapSource, /requestContextKey === sessionContextKeyRef\.current/)

console.log('Frontend convergence regression check passed.')
console.log('Verified latest-request stale-response rejection and unversioned realtime invalidation policy.')
