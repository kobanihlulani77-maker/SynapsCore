import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { createLatestRequestGate, createSingleFlightRequest } from '../src/services/latestRequest.js'
import { emptySnapshot, normalizeSnapshot } from '../src/config/workspaceModel.js'

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

const replayRecord = {
  id: 82,
  externalOrderId: 'UI-RPL-A11934C3',
  sourceSystem: 'ui_replay_a11934c3',
  connectorType: 'CSV_ORDER_IMPORT',
  status: 'PENDING',
  warehouseCode: 'WH-NORTH',
}
const replaySnapshot = normalizeSnapshot({
  ...emptySnapshot,
  integrationReplayQueue: [replayRecord],
}, emptySnapshot)
assert.equal(replaySnapshot.integrationReplayQueue[0].externalOrderId, replayRecord.externalOrderId)
assert.equal(replaySnapshot.integrationReplayQueue.length, 1)

const singleFlight = createSingleFlightRequest()
let releaseSnapshot
let snapshotCallCount = 0
const pendingSnapshot = singleFlight.run('tenant|integration-admin', () => {
  snapshotCallCount += 1
  return new Promise((resolve) => {
    releaseSnapshot = resolve
  })
})
const refreshStormSnapshot = singleFlight.run('tenant|integration-admin', () => {
  snapshotCallCount += 1
  return Promise.resolve({ integrationReplayQueue: [] })
})
assert.strictEqual(refreshStormSnapshot, pendingSnapshot)
await Promise.resolve()
assert.equal(snapshotCallCount, 1)
releaseSnapshot(replaySnapshot)
assert.strictEqual(await refreshStormSnapshot, replaySnapshot)

const replaySource = await fs.readFile(path.join(frontendRoot, 'src', 'pages', 'Replay.jsx'), 'utf8')
assert.match(replaySource, /const queuedRecords = snapshot\.integrationReplayQueue/)
assert.match(replaySource, /queuedRecords\.length \? queuedRecords\.map/)

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
assert.match(bootstrapSource, /snapshotSingleFlightRef/)
assert.match(bootstrapSource, /snapshotSingleFlightRef\.current\.run\(sessionContextKey/)
assert.match(bootstrapSource, /requestContextKey === sessionContextKeyRef\.current/)

console.log('Frontend convergence regression check passed.')
console.log('Verified latest-request stale-response rejection and unversioned realtime invalidation policy.')
