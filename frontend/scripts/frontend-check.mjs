import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(scriptDir, '..')
const repoRoot = path.resolve(frontendRoot, '..')
const srcDir = path.join(frontendRoot, 'src')
const docsDir = path.join(repoRoot, 'docs')

const requiredDocs = [
  path.join(docsDir, 'frontend-demo-guide.md'),
  path.join(docsDir, 'frontend-qa-checklist.md'),
  path.join(docsDir, 'frontend-demo-mode.md'),
]

const proofCriticalLabels = [
  'Replay Into Live Flow',
  'Scenario action console',
  'Approval action console',
  'Live operational command center',
  'Access your operational workspace.',
  'Failed inbound recovery',
  'Live order operations',
  'Operational warning center',
]

const sourcePolicyChecks = [
  { description: 'direct console logging', pattern: /console\.(log|debug)\s*\(/ },
  { description: 'debugger statement', pattern: /\bdebugger\b/ },
  { description: 'launch TODO marker', pattern: /\bTODO\b/ },
  { description: 'launch FIXME marker', pattern: /\bFIXME\b/ },
]

const documentationPolicyChecks = [
  { description: 'launch TODO marker', pattern: /\bTODO\b/ },
  { description: 'launch FIXME marker', pattern: /\bFIXME\b/ },
]

async function collectFiles(rootDir, extensions) {
  const entries = await fs.readdir(rootDir, { withFileTypes: true })
  const files = []

  for (const entry of entries) {
    const fullPath = path.join(rootDir, entry.name)
    if (entry.isDirectory()) {
      files.push(...await collectFiles(fullPath, extensions))
      continue
    }

    if (extensions.has(path.extname(entry.name))) {
      files.push(fullPath)
    }
  }

  return files
}

function relativeToRepo(fullPath) {
  return path.relative(repoRoot, fullPath).replaceAll('\\', '/')
}

function findPolicyViolations(content, checks) {
  const lines = content.split(/\r?\n/)
  const violations = []

  lines.forEach((line, index) => {
    checks.forEach((check) => {
      if (check.pattern.test(line)) {
        violations.push({
          lineNumber: index + 1,
          description: check.description,
          line: line.trim(),
        })
      }
      check.pattern.lastIndex = 0
    })
  })

  return violations
}

async function main() {
  const failures = []
  const sourceFiles = await collectFiles(srcDir, new Set(['.js', '.jsx', '.css']))
  const documentationFiles = [
    path.join(repoRoot, 'README.md'),
    ...requiredDocs,
  ]

  for (const docPath of requiredDocs) {
    try {
      await fs.access(docPath)
    } catch {
      failures.push(`Missing required frontend doc: ${relativeToRepo(docPath)}`)
    }
  }

  for (const filePath of sourceFiles) {
    const content = await fs.readFile(filePath, 'utf8')
    const violations = findPolicyViolations(content, sourcePolicyChecks)
    for (const violation of violations) {
      failures.push(`${relativeToRepo(filePath)}:${violation.lineNumber} ${violation.description}: ${violation.line}`)
    }
  }

  for (const filePath of documentationFiles) {
    try {
      const content = await fs.readFile(filePath, 'utf8')
      const violations = findPolicyViolations(content, documentationPolicyChecks)
      for (const violation of violations) {
        failures.push(`${relativeToRepo(filePath)}:${violation.lineNumber} ${violation.description}: ${violation.line}`)
      }
    } catch {
      failures.push(`Missing expected documentation file: ${relativeToRepo(filePath)}`)
    }
  }

  const sourceCorpus = (await Promise.all(sourceFiles.map((filePath) => fs.readFile(filePath, 'utf8')))).join('\n')
  for (const label of proofCriticalLabels) {
    if (!sourceCorpus.includes(label)) {
      failures.push(`Missing proof-critical label: "${label}"`)
    }
  }

  const pageRegistry = await fs.readFile(path.join(srcDir, 'config', 'pageRegistry.js'), 'utf8')
  const platformApplication = await fs.readFile(path.join(srcDir, 'components', 'PlatformApplication.jsx'), 'utf8')
  const tenantAppRoutes = await fs.readFile(path.join(srcDir, 'components', 'AppRoutes.jsx'), 'utf8')
  const workspaceAppModel = await fs.readFile(path.join(srcDir, 'hooks', 'useWorkspaceAppModel.js'), 'utf8')
  const workspaceRealtime = await fs.readFile(path.join(srcDir, 'hooks', 'useWorkspaceRealtime.js'), 'utf8')
  const accessBoundarySignals = [
    [pageRegistry.includes("audience: 'platform'"), 'Platform routes must use the dedicated platform audience.'],
    [pageRegistry.includes('buildRoleAwareNavGroups'), 'Tenant navigation must be generated from role-aware policy.'],
    [!pageRegistry.includes("keys: ['users', 'settings', 'profile', 'platform'"), 'Tenant navigation must not include platform routes.'],
    [platformApplication.includes('/api/platform/session/login'), 'Platform UI must authenticate through the platform session endpoint.'],
    [platformApplication.includes('/api/platform/overview'), 'Platform UI must read the metadata-only overview endpoint.'],
    [!platformApplication.includes('localStorage'), 'Platform authority must not be stored in localStorage.'],
    [!platformApplication.includes('X-Synapse-Platform-Admin-Token'), 'The browser must not receive the automation platform token.'],
    [!tenantAppRoutes.includes('PlatformAdminPage'), 'Tenant route rendering must not retain the previous platform-admin page path.'],
    [workspaceAppModel.includes('signedInRoles:'), 'Realtime policy must receive the signed-in role set.'],
    [workspaceAppModel.includes('signedInWarehouseScopes:'), 'Realtime policy must receive warehouse scopes.'],
    [workspaceRealtime.includes("hasIntegrationAccess"), 'Integration realtime subscriptions must be role-aware.'],
    [workspaceRealtime.includes("/integrations.changed"), 'Scoped integration realtime must use the metadata-only change signal.'],
    [workspaceRealtime.includes('hasTenantWideWarehouseAccess'), 'Raw tenant-wide realtime topics must be scope-aware.'],
  ]
  accessBoundarySignals.forEach(([valid, message]) => {
    if (!valid) failures.push(message)
  })

  if (failures.length) {
    console.error('Frontend launch-readiness check failed:\n')
    failures.forEach((failure) => {
      console.error(`- ${failure}`)
    })
    process.exit(1)
  }

  console.log('Frontend launch-readiness check passed.')
  console.log(`Checked ${sourceFiles.length} frontend source files.`)
  console.log(`Verified ${requiredDocs.length} frontend demo/QA docs and proof-critical labels.`)
}

await main()
