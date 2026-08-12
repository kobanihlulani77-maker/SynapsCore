import { mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(__dirname, '..')
const srcRoot = path.join(frontendRoot, 'src')
const defaultOutputDir = path.join(frontendRoot, 'test-results', 'control-inventory')

const args = process.argv.slice(2)
const getArg = (name, fallback = null) => {
  const prefix = `--${name}=`
  const inline = args.find((arg) => arg.startsWith(prefix))
  if (inline) {
    return inline.slice(prefix.length)
  }
  const index = args.indexOf(`--${name}`)
  if (index !== -1 && args[index + 1]) {
    return args[index + 1]
  }
  return fallback
}

const outputDir = path.resolve(frontendRoot, getArg('outputDir', defaultOutputDir))

const routeByFileBase = {
  Alerts: ['/alerts'],
  Approvals: ['/approvals'],
  Audit: ['/audit-events'],
  Catalog: ['/catalog'],
  CreateWorkspace: ['/create-workspace'],
  Dashboard: ['/dashboard'],
  Escalations: ['/escalations'],
  Fulfillment: ['/fulfillment'],
  Integrations: ['/integrations'],
  Inventory: ['/inventory'],
  Locations: ['/locations'],
  Orders: ['/orders'],
  PlatformAdmin: ['/platform-admin'],
  Profile: ['/profile'],
  PublicExperience: ['/', '/product', '/contact'],
  Recommendations: ['/recommendations'],
  Releases: ['/releases'],
  Replay: ['/replay-queue'],
  Runtime: ['/runtime'],
  ScenarioControl: ['/scenario-control'],
  ScenarioHistory: ['/scenario-history'],
  ScenarioPlanner: ['/scenarios'],
  Settings: ['/company-settings'],
  SignIn: ['/sign-in'],
  SystemConfig: ['/system-config'],
  Tenants: ['/tenant-management'],
  Users: ['/users'],
}

const knownSharedRouteFiles = new Set([
  'ActionPanel',
  'ActivityFeed',
  'AppRoutes',
  'DataGrid',
  'EmptyState',
  'LoadingState',
  'Panel',
  'ScenarioDecisionConsole',
  'ScenarioEditor',
  'Sidebar',
  'Topbar',
  'WorkspaceApplication',
  'WorkspaceAuthenticatedApp',
  'WorkspaceNotices',
  'WorkspacePageHeader',
  'WorkspaceRouteSwitch',
  'WorkspaceUtilityRail',
])

function walk(dir) {
  const entries = readdirSync(dir, { withFileTypes: true })
  return entries.flatMap((entry) => {
    const absolutePath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      return walk(absolutePath)
    }
    if (entry.isFile() && /\.(jsx|js)$/.test(entry.name)) {
      return [absolutePath]
    }
    return []
  })
}

function lineNumberAt(source, index) {
  return source.slice(0, index).split(/\r?\n/).length
}

function extractAttribute(attributes, name) {
  const quoted = attributes.match(new RegExp(`${name}\\s*=\\s*["']([^"']+)["']`))
  if (quoted) {
    return quoted[1].trim()
  }
  const braced = attributes.match(new RegExp(`${name}\\s*=\\s*\\{([^}]*)\\}`))
  if (braced) {
    return braced[1].trim()
  }
  return ''
}

function hasAttribute(attributes, name) {
  return new RegExp(`(^|\\s)${name}(\\s|=|$)`).test(attributes)
}

function compact(value) {
  return String(value || '')
    .replace(/\s+/g, ' ')
    .replace(/[{}]/g, '')
    .trim()
}

function inferVisibleLabel(source, tagMatchEnd, attributes) {
  const explicit =
    extractAttribute(attributes, 'aria-label')
    || extractAttribute(attributes, 'title')
    || extractAttribute(attributes, 'placeholder')
    || extractAttribute(attributes, 'value')
    || extractAttribute(attributes, 'name')
    || extractAttribute(attributes, 'id')

  if (explicit) {
    return compact(explicit)
  }

  const nextText = source.slice(tagMatchEnd, Math.min(source.length, tagMatchEnd + 180))
  const textMatch = nextText.match(/^([^<]{1,100})</)
  if (textMatch) {
    return compact(textMatch[1])
  }
  return 'Unlabeled interactive control'
}

function inferType(tagName, attributes) {
  if (tagName === 'input') {
    const inputType = extractAttribute(attributes, 'type').toLowerCase() || 'text'
    if (inputType === 'checkbox' || inputType === 'radio') {
      return inputType
    }
    return 'input'
  }
  if (tagName === 'a') {
    return 'anchor'
  }
  if (tagName === 'form') {
    return 'form'
  }
  return tagName.toLowerCase()
}

function inferHandler(attributes, type) {
  return compact(
    extractAttribute(attributes, 'onClick')
    || extractAttribute(attributes, 'onSubmit')
    || extractAttribute(attributes, 'onChange')
    || extractAttribute(attributes, 'onKeyDown')
    || extractAttribute(attributes, 'onKeyUp')
    || extractAttribute(attributes, 'onBlur')
    || extractAttribute(attributes, 'href')
    || (type === 'form' ? 'submit event' : ''),
  )
}

function inferRoutes(fileBase) {
  if (routeByFileBase[fileBase]) {
    return routeByFileBase[fileBase]
  }
  if (knownSharedRouteFiles.has(fileBase)) {
    return ['shared-shell']
  }
  return ['shared-or-indirect']
}

function findTagEnd(source, startIndex) {
  let quote = null
  let braceDepth = 0
  for (let index = startIndex; index < source.length; index += 1) {
    const char = source[index]
    const previous = source[index - 1]

    if (quote) {
      if (char === quote && previous !== '\\') {
        quote = null
      }
      continue
    }

    if (char === '"' || char === "'") {
      quote = char
      continue
    }

    if (char === '{') {
      braceDepth += 1
      continue
    }

    if (char === '}') {
      braceDepth = Math.max(0, braceDepth - 1)
      continue
    }

    if (char === '>' && braceDepth === 0) {
      return index
    }
  }
  return -1
}

function inferExpectedState(attributes) {
  if (hasAttribute(attributes, 'disabled')) {
    return 'state-dependent or disabled when expression is true'
  }
  if (hasAttribute(attributes, 'aria-disabled')) {
    return 'aria-disabled state-dependent'
  }
  return 'enabled when rendered'
}

function inferSelector(attributes) {
  const selectorParts = [
    extractAttribute(attributes, 'data-testid') && `data-testid=${extractAttribute(attributes, 'data-testid')}`,
    extractAttribute(attributes, 'id') && `id=${extractAttribute(attributes, 'id')}`,
    extractAttribute(attributes, 'name') && `name=${extractAttribute(attributes, 'name')}`,
    extractAttribute(attributes, 'aria-label') && `aria-label=${extractAttribute(attributes, 'aria-label')}`,
    extractAttribute(attributes, 'href') && `href=${extractAttribute(attributes, 'href')}`,
  ].filter(Boolean)
  return selectorParts.join('; ')
}

function inferBackendPath(handler, source, lineNumber) {
  const windowStart = Math.max(0, source.split(/\r?\n/).slice(0, Math.max(0, lineNumber - 40)).join('\n').length)
  const windowEnd = source.split(/\r?\n/).slice(0, Math.min(source.split(/\r?\n/).length, lineNumber + 80)).join('\n').length
  const context = source.slice(windowStart, windowEnd)
  const apiHints = []
  for (const match of context.matchAll(/(?:api\.|client\.|fetch\(|request\(|post\(|put\(|patch\(|delete\(|get\()([A-Za-z0-9_.'"/:${}\-\s,]*)/g)) {
    apiHints.push(compact(match[0]).slice(0, 120))
  }
  if (/navigate|href=/.test(handler)) {
    return 'frontend navigation'
  }
  if (apiHints.length) {
    return [...new Set(apiHints)].slice(0, 3).join(' | ')
  }
  return ''
}

function shouldCountNonSemantic(tagName, attributes) {
  const lower = tagName.toLowerCase()
  if (['button', 'input', 'select', 'textarea', 'a', 'form'].includes(lower)) {
    return false
  }
  return /onClick|onSubmit|onChange|onKeyDown|onKeyUp|role\s*=\s*["']button["']|tabIndex/.test(attributes)
}

function collectControls() {
  const files = walk(srcRoot).filter((file) => {
    const normalized = file.replace(/\\/g, '/')
    return /\/(pages|components|layout)\//.test(normalized)
  })
  const records = []
  const seen = new Set()

  for (const file of files) {
    const source = readFileSync(file, 'utf8')
    const relativePath = path.relative(frontendRoot, file).replace(/\\/g, '/')
    const fileBase = path.basename(file).replace(/\.(jsx|js)$/, '')
    const routes = inferRoutes(fileBase)
    const componentMatch = source.match(/export default function\s+([A-Za-z0-9_]+)/)
    const component = componentMatch?.[1] || fileBase
    const tagStartPattern = /<([A-Za-z][A-Za-z0-9.]*)\b/g
    for (const match of source.matchAll(tagStartPattern)) {
      const [, rawTagName] = match
      const tagEnd = findTagEnd(source, match.index)
      if (tagEnd === -1) {
        continue
      }
      const fullMatch = source.slice(match.index, tagEnd + 1)
      const attributes = fullMatch
        .replace(new RegExp(`^<${rawTagName}\\b`), '')
        .replace(/\/?>$/, '')
      const tagName = rawTagName.toLowerCase()
      const semantic = ['button', 'input', 'select', 'textarea', 'a', 'form'].includes(tagName)
      const roleButton = /role\s*=\s*["']button["']/.test(attributes)
      const nonSemantic = shouldCountNonSemantic(rawTagName, attributes)
      if (!semantic && !roleButton && !nonSemantic) {
        continue
      }

      const lineNumber = lineNumberAt(source, match.index)
      const key = `${relativePath}:${lineNumber}:${rawTagName}:${compact(attributes).slice(0, 80)}`
      if (seen.has(key)) {
        continue
      }
      seen.add(key)

      const type = semantic ? inferType(tagName, attributes) : (roleButton ? 'role-button' : 'interactive-surface')
      const handler = inferHandler(attributes, type)
      const visibleLabel = inferVisibleLabel(source, tagEnd + 1, attributes)
      const routesForRecord = routes.join(', ')
      const selector = inferSelector(attributes)
      const isPublicRoute = routes.some((route) => ['/', '/product', '/contact', '/create-workspace', '/sign-in'].includes(route))

      records.push({
        auditId: '',
        route: routesForRecord,
        component,
        file: relativePath,
        line: lineNumber,
        controlType: type,
        tag: rawTagName,
        visibleLabel,
        existingSelector: selector || 'none',
        requiredRole: isPublicRoute ? 'public' : (routesForRecord.includes('shared') ? 'context-dependent' : 'authenticated operator'),
        expectedEnabledState: inferExpectedState(attributes),
        disabledConditions: compact(extractAttribute(attributes, 'disabled') || extractAttribute(attributes, 'aria-disabled') || 'none declared'),
        handler: handler || 'no explicit handler or native browser behavior',
        apiOrBackendPath: inferBackendPath(handler, source, lineNumber) || 'none detected statically',
        expectedSideEffect: type === 'anchor' || /navigate|href/.test(handler)
          ? 'navigation'
          : type === 'form'
            ? 'submit current form state'
            : /onChange|set[A-Z]|update|select/i.test(handler)
              ? 'local state update or selection'
              : /save|create|submit|approve|reject|execute|replay|delete|import|refresh|sign/i.test(`${handler} ${visibleLabel}`)
                ? 'API mutation, refresh, or command action'
                : 'control-specific UI action',
        persistenceExpectation: /save|create|submit|approve|reject|execute|replay|import|password|tenant|profile|user|catalog|setting/i.test(`${handler} ${visibleLabel}`)
          ? 'backend/readback expected'
          : 'none or route/state scoped',
        realtimeExpectation: /order|inventory|replay|approve|execute|scenario|refresh|dashboard/i.test(`${handler} ${visibleLabel} ${relativePath}`)
          ? 'possible realtime/dashboard refresh expectation'
          : 'none expected',
        staticClassification: handler === 'no explicit handler or native browser behavior' && type !== 'anchor' && type !== 'input' && type !== 'select' && type !== 'textarea'
          ? 'needs manual trace'
          : 'traceable',
        finalResult: 'UNVERIFIED',
      })
    }
  }

  records.sort((left, right) => {
    const routeSort = left.route.localeCompare(right.route)
    if (routeSort !== 0) return routeSort
    const fileSort = left.file.localeCompare(right.file)
    if (fileSort !== 0) return fileSort
    return left.line - right.line
  })

  records.forEach((record, index) => {
    record.auditId = `CTRL-${String(index + 1).padStart(3, '0')}`
  })

  return records
}

function summarize(records) {
  const byType = records.reduce((acc, record) => {
    acc[record.controlType] = (acc[record.controlType] || 0) + 1
    return acc
  }, {})
  const byRoute = records.reduce((acc, record) => {
    for (const route of record.route.split(',').map((value) => value.trim())) {
      acc[route] = (acc[route] || 0) + 1
    }
    return acc
  }, {})
  const byStaticClassification = records.reduce((acc, record) => {
    acc[record.staticClassification] = (acc[record.staticClassification] || 0) + 1
    return acc
  }, {})
  return {
    generatedAt: new Date().toISOString(),
    totalControls: records.length,
    buttons: byType.button || 0,
    inputs: byType.input || 0,
    selects: byType.select || 0,
    textareas: byType.textarea || 0,
    checkboxes: byType.checkbox || 0,
    radios: byType.radio || 0,
    anchors: byType.anchor || 0,
    forms: byType.form || 0,
    roleButtons: byType['role-button'] || 0,
    otherInteractiveControls: records.length
      - (byType.button || 0)
      - (byType.input || 0)
      - (byType.select || 0)
      - (byType.textarea || 0)
      - (byType.checkbox || 0)
      - (byType.radio || 0),
    byType,
    byRoute,
    byStaticClassification,
  }
}

function toMarkdown(summary, records) {
  const lines = [
    '# SynapseCore Control Inventory',
    '',
    `Generated at: ${summary.generatedAt}`,
    '',
    '## Summary',
    '',
    `- Total controls: ${summary.totalControls}`,
    `- Buttons: ${summary.buttons}`,
    `- Inputs: ${summary.inputs}`,
    `- Selects: ${summary.selects}`,
    `- Textareas: ${summary.textareas}`,
    `- Checkboxes: ${summary.checkboxes}`,
    `- Radios: ${summary.radios}`,
    `- Anchors/navigation actions: ${summary.anchors}`,
    `- Forms: ${summary.forms}`,
    `- Role buttons: ${summary.roleButtons}`,
    `- Other interactive controls: ${summary.otherInteractiveControls}`,
    '',
    '## Controls',
    '',
    '| Audit ID | Route | Component | Type | Label | Selector | Handler | Static Result |',
    '| --- | --- | --- | --- | --- | --- | --- | --- |',
  ]

  for (const record of records) {
    lines.push(`| ${record.auditId} | ${record.route} | ${record.component} | ${record.controlType} | ${record.visibleLabel.replace(/\|/g, '/')} | ${record.existingSelector.replace(/\|/g, '/')} | ${record.handler.replace(/\|/g, '/')} | ${record.staticClassification} |`)
  }

  return `${lines.join('\n')}\n`
}

const records = collectControls()
const summary = summarize(records)
mkdirSync(outputDir, { recursive: true })
writeFileSync(path.join(outputDir, 'control-inventory.json'), JSON.stringify({ summary, controls: records }, null, 2))
writeFileSync(path.join(outputDir, 'control-inventory.md'), toMarkdown(summary, records))

console.log('SynapseCore Control Inventory')
console.log(`Generated: ${summary.generatedAt}`)
console.log(`Total controls: ${summary.totalControls}`)
console.log(`Buttons: ${summary.buttons}`)
console.log(`Inputs: ${summary.inputs}`)
console.log(`Selects: ${summary.selects}`)
console.log(`Textareas: ${summary.textareas}`)
console.log(`Checkboxes: ${summary.checkboxes}`)
console.log(`Radios: ${summary.radios}`)
console.log(`Anchors/navigation actions: ${summary.anchors}`)
console.log(`Forms: ${summary.forms}`)
console.log(`Role buttons: ${summary.roleButtons}`)
console.log(`Other interactive controls: ${summary.otherInteractiveControls}`)
console.log(`JSON: ${path.join(outputDir, 'control-inventory.json')}`)
console.log(`Markdown: ${path.join(outputDir, 'control-inventory.md')}`)
