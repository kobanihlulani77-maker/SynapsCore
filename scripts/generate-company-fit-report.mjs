import fs from "node:fs";
import path from "node:path";
import { companyProfiles, companyProfileMap, platformTruth } from "./company-fit-data.mjs";

function printUsage() {
  const ids = companyProfiles.map((profile) => profile.id).join(", ");
  console.log(`SynapseCore company-fit report generator

Usage:
  node scripts/generate-company-fit-report.mjs --all [--format markdown|html|json] [--output <file>]
  node scripts/generate-company-fit-report.mjs --company-type <id>[,<id>...] [--format markdown|html|json] [--output <file>]
  node scripts/generate-company-fit-report.mjs --list

Available company types:
  ${ids}
`);
}

function parseArgs(argv) {
  const options = {
    format: "markdown",
    companyTypes: [],
    all: false,
    list: false,
    output: null
  };

  for (let index = 0; index < argv.length; index += 1) {
    const current = argv[index];
    switch (current) {
      case "--all":
        options.all = true;
        break;
      case "--list":
        options.list = true;
        break;
      case "--format":
        options.format = argv[index + 1] ?? "";
        index += 1;
        break;
      case "--company-type":
        options.companyTypes.push(...(argv[index + 1] ?? "").split(",").map((value) => value.trim()).filter(Boolean));
        index += 1;
        break;
      case "--output":
        options.output = argv[index + 1] ?? null;
        index += 1;
        break;
      case "--help":
      case "-h":
        options.help = true;
        break;
      default:
        throw new Error(`Unknown argument: ${current}`);
    }
  }

  return options;
}

function escapeHtml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function renderBulletList(items) {
  return items.map((item) => `- ${item}`).join("\n");
}

function renderScenarioMarkdown(scenarios) {
  return scenarios.map((scenario) => [
    `#### ${scenario.title}`,
    "",
    `- Before: ${scenario.before}`,
    `- After: ${scenario.after}`
  ].join("\n")).join("\n\n");
}

function renderProfileMarkdown(profile) {
  return [
    `## ${profile.label}`,
    "",
    profile.headline,
    "",
    "### Operational Pain",
    profile.operationalPain,
    "",
    "### Fragmented Systems Usually Look Like",
    profile.fragmentedSystems,
    "",
    "### Where Delays And Failures Happen",
    profile.delayFailures,
    "",
    "### Why Visibility Breaks",
    profile.visibilityBreaks,
    "",
    "### Where Approvals Become Bottlenecks",
    profile.approvalBottlenecks,
    "",
    "### Where Integrations Fail",
    profile.integrationFailures,
    "",
    "### Where Inventory Or Order Mismatch Appears",
    profile.inventoryMismatch,
    "",
    "### How Replay And Recovery Help",
    profile.replayRecovery,
    "",
    "### How Realtime Visibility Changes Operations",
    profile.realtimeVisibility,
    "",
    "### How Audit And Event Tracing Help",
    profile.auditTracing,
    "",
    "### Why Tenant Isolation Matters",
    profile.tenantIsolation,
    "",
    "### Metrics That Matter Most",
    renderBulletList(profile.metrics),
    "",
    "### Dashboard Views That Matter Most",
    renderBulletList(profile.dashboards),
    "",
    "### Alerts And Recommendations That Matter Most",
    renderBulletList(profile.alerts),
    "",
    "### ROI And Operational Value",
    renderBulletList(profile.roi),
    "",
    "### Realistic Scenarios",
    renderScenarioMarkdown(profile.scenarios)
  ].join("\n");
}

function renderMarkdown(profiles) {
  const companyTypeSummary = profiles.map((profile) => `- \`${profile.id}\` - ${profile.label}`).join("\n");
  const generatedAt = new Date().toISOString();
  return [
    "# SynapseCore Company Fit Report",
    "",
    `Generated at: \`${generatedAt}\``,
    "",
    "## Platform Truth",
    "",
    "SynapseCore should be positioned as an operations control platform for the lanes it actually implements today.",
    "",
    "### Current Supported Scope",
    renderBulletList(platformTruth.currentScope),
    "",
    "### Fit Signals",
    renderBulletList(platformTruth.fitSignals),
    "",
    "### Not Claimed",
    renderBulletList(platformTruth.notClaimed),
    "",
    "### Proof Highlights",
    renderBulletList(platformTruth.proofHighlights),
    "",
    "## Included Company Types",
    companyTypeSummary,
    "",
    profiles.map(renderProfileMarkdown).join("\n\n")
  ].join("\n");
}

function renderProfileHtml(profile) {
  const section = (title, body) => `<section><h3>${escapeHtml(title)}</h3>${body}</section>`;
  const list = (items) => `<ul>${items.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>`;
  const scenarios = profile.scenarios.map((scenario) => `
    <section class="scenario">
      <h3>${escapeHtml(scenario.title)}</h3>
      <p><strong>Before:</strong> ${escapeHtml(scenario.before)}</p>
      <p><strong>After:</strong> ${escapeHtml(scenario.after)}</p>
    </section>
  `).join("");

  return `
    <article class="profile">
      <h2>${escapeHtml(profile.label)}</h2>
      <p class="headline">${escapeHtml(profile.headline)}</p>
      ${section("Operational Pain", `<p>${escapeHtml(profile.operationalPain)}</p>`)}
      ${section("Fragmented Systems Usually Look Like", `<p>${escapeHtml(profile.fragmentedSystems)}</p>`)}
      ${section("Where Delays And Failures Happen", `<p>${escapeHtml(profile.delayFailures)}</p>`)}
      ${section("Why Visibility Breaks", `<p>${escapeHtml(profile.visibilityBreaks)}</p>`)}
      ${section("Where Approvals Become Bottlenecks", `<p>${escapeHtml(profile.approvalBottlenecks)}</p>`)}
      ${section("Where Integrations Fail", `<p>${escapeHtml(profile.integrationFailures)}</p>`)}
      ${section("Where Inventory Or Order Mismatch Appears", `<p>${escapeHtml(profile.inventoryMismatch)}</p>`)}
      ${section("How Replay And Recovery Help", `<p>${escapeHtml(profile.replayRecovery)}</p>`)}
      ${section("How Realtime Visibility Changes Operations", `<p>${escapeHtml(profile.realtimeVisibility)}</p>`)}
      ${section("How Audit And Event Tracing Help", `<p>${escapeHtml(profile.auditTracing)}</p>`)}
      ${section("Why Tenant Isolation Matters", `<p>${escapeHtml(profile.tenantIsolation)}</p>`)}
      ${section("Metrics That Matter Most", list(profile.metrics))}
      ${section("Dashboard Views That Matter Most", list(profile.dashboards))}
      ${section("Alerts And Recommendations That Matter Most", list(profile.alerts))}
      ${section("ROI And Operational Value", list(profile.roi))}
      <section>
        <h3>Realistic Scenarios</h3>
        ${scenarios}
      </section>
    </article>
  `;
}

function renderHtml(profiles) {
  const generatedAt = new Date().toISOString();
  const htmlList = (items) => `<ul>${items.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>`;
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>SynapseCore Company Fit Report</title>
  <style>
    :root {
      color-scheme: light;
      --bg: #f4f1ea;
      --panel: #fffdfa;
      --ink: #182128;
      --muted: #51606b;
      --line: #d3c6b5;
      --accent: #0c6d63;
      --accent-soft: #e2f3f1;
    }
    body {
      margin: 0;
      font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
      color: var(--ink);
      background: linear-gradient(180deg, #f3eee5 0%, #f8f6f1 100%);
      line-height: 1.55;
    }
    main {
      max-width: 1100px;
      margin: 0 auto;
      padding: 40px 24px 80px;
    }
    header, article {
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 20px;
      box-shadow: 0 18px 40px rgba(24, 33, 40, 0.08);
    }
    header {
      padding: 32px;
      margin-bottom: 28px;
    }
    article {
      padding: 28px;
      margin-bottom: 24px;
    }
    h1, h2 {
      margin-top: 0;
      letter-spacing: -0.02em;
    }
    h1 {
      font-size: 2.3rem;
    }
    h2 {
      font-size: 1.65rem;
      border-bottom: 1px solid var(--line);
      padding-bottom: 10px;
    }
    h3 {
      font-size: 1rem;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: var(--accent);
      margin-bottom: 8px;
    }
    p, li {
      color: var(--ink);
    }
    .muted {
      color: var(--muted);
    }
    .grid {
      display: grid;
      gap: 18px;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    }
    .card {
      background: var(--accent-soft);
      border-radius: 14px;
      padding: 16px;
    }
    .headline {
      font-size: 1.05rem;
      color: var(--muted);
    }
    .scenario {
      border-left: 4px solid var(--accent);
      padding-left: 14px;
      margin-bottom: 14px;
    }
  </style>
</head>
<body>
  <main>
    <header>
      <h1>SynapseCore Company Fit Report</h1>
      <p class="muted">Generated at ${escapeHtml(generatedAt)}</p>
      <p>SynapseCore should be positioned as an operational control platform for the lanes it actually proves today: tenant-safe access, warehouse-aware catalog and inventory control, inbound order ingestion, deterministic replay and recovery, scenario governance, realtime operations, and runtime trust surfaces.</p>
      <div class="grid">
        <div class="card">
          <h3>Current Supported Scope</h3>
          ${htmlList(platformTruth.currentScope)}
        </div>
        <div class="card">
          <h3>Fit Signals</h3>
          ${htmlList(platformTruth.fitSignals)}
        </div>
        <div class="card">
          <h3>Proof Highlights</h3>
          ${htmlList(platformTruth.proofHighlights)}
        </div>
      </div>
    </header>
    ${profiles.map(renderProfileHtml).join("\n")}
  </main>
</body>
</html>`;
}

function renderJson(profiles) {
  return JSON.stringify({
    generatedAt: new Date().toISOString(),
    platformTruth,
    profiles
  }, null, 2);
}

function resolveProfiles(options) {
  if (options.all || options.companyTypes.length === 0) {
    return companyProfiles;
  }
  return options.companyTypes.map((id) => {
    const profile = companyProfileMap.get(id);
    if (!profile) {
      throw new Error(`Unknown company type: ${id}`);
    }
    return profile;
  });
}

function renderOutput(profiles, format) {
  switch (format) {
    case "markdown":
      return renderMarkdown(profiles);
    case "html":
      return renderHtml(profiles);
    case "json":
      return renderJson(profiles);
    default:
      throw new Error(`Unsupported format: ${format}`);
  }
}

function writeOutput(outputPath, content) {
  const absolutePath = path.resolve(outputPath);
  fs.mkdirSync(path.dirname(absolutePath), { recursive: true });
  fs.writeFileSync(absolutePath, content, "utf8");
  return absolutePath;
}

try {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    printUsage();
    process.exit(0);
  }
  if (options.list) {
    console.log(companyProfiles.map((profile) => `${profile.id}\t${profile.label}`).join("\n"));
    process.exit(0);
  }

  const profiles = resolveProfiles(options);
  const content = renderOutput(profiles, options.format);
  if (options.output) {
    const written = writeOutput(options.output, content);
    console.log(`Wrote ${options.format} report to ${written}`);
  } else {
    process.stdout.write(content);
  }
} catch (error) {
  console.error(error.message);
  printUsage();
  process.exit(1);
}
