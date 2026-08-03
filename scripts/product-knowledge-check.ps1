Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
Set-Location $rootDir

$blockers = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

function Test-RepoPath {
    param([Parameter(Mandatory = $true)][string]$Path)
    return Test-Path -LiteralPath (Join-Path $rootDir $Path)
}

function Add-Blocker {
    param([Parameter(Mandatory = $true)][string]$Message)
    $blockers.Add($Message)
}

function Add-Warning {
    param([Parameter(Mandatory = $true)][string]$Message)
    $warnings.Add($Message)
}

function Assert-Doc {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-RepoPath $Path)) {
        Add-Blocker "Missing product knowledge doc: $Path"
    }
}

$canonicalProductDocs = @(
    "docs/product-knowledge-base.md",
    "docs/operational-concepts.md",
    "docs/synapsecore-dictionary.md",
    "docs/business-process-library.md",
    "docs/executive-product-guide.md",
    "docs/operations-manager-guide.md",
    "docs/warehouse-manager-guide.md",
    "docs/it-administrator-guide.md",
    "docs/solution-architect-guide.md",
    "docs/future-product-vision.md",
    "docs/official-pilot-program.md"
)

$industryDocs = @(
    "docs/industry-guide-retail.md",
    "docs/industry-guide-manufacturing.md",
    "docs/industry-guide-distribution.md",
    "docs/industry-guide-logistics.md",
    "docs/industry-guide-ecommerce.md"
)

$pilotTemplates = @(
    "docs/templates/pilot-evidence-template.md",
    "docs/templates/pilot-weekly-review-template.md",
    "docs/templates/pilot-final-report-template.md",
    "docs/templates/pilot-incident-log-template.md"
)

foreach ($doc in $canonicalProductDocs) {
    Assert-Doc $doc
}

foreach ($doc in $industryDocs) {
    Assert-Doc $doc
}

foreach ($template in $pilotTemplates) {
    if (-not (Test-RepoPath $template)) {
        Add-Warning "Missing optional pilot evidence template: $template"
    }
}

$indexPath = Join-Path $rootDir "docs/INDEX.md"
$documentationMapPath = Join-Path $rootDir "docs/documentation-map.md"
$readmePath = Join-Path $rootDir "README.md"

if (Test-Path -LiteralPath $indexPath) {
    $indexText = Get-Content -LiteralPath $indexPath -Raw
    if ($indexText -notmatch [regex]::Escape("official-pilot-program.md")) {
        Add-Blocker "docs/INDEX.md does not link official-pilot-program.md"
    }
}
else {
    Add-Blocker "Missing docs/INDEX.md"
}

if (Test-Path -LiteralPath $documentationMapPath) {
    $documentationMapText = Get-Content -LiteralPath $documentationMapPath -Raw
    if ($documentationMapText -notmatch [regex]::Escape("official-pilot-program.md")) {
        Add-Blocker "docs/documentation-map.md does not reference official-pilot-program.md"
    }
}
else {
    Add-Blocker "Missing docs/documentation-map.md"
}

if (Test-Path -LiteralPath $readmePath) {
    $readmeText = Get-Content -LiteralPath $readmePath -Raw
    if ($readmeText -notmatch [regex]::Escape("official-pilot-program.md")) {
        Add-Blocker "README.md does not link official-pilot-program.md"
    }
}
else {
    Add-Blocker "Missing README.md"
}

$classification = "PRODUCT_KNOWLEDGE_READY"
if ($blockers.Count -gt 0) {
    $classification = "NEEDS_ATTENTION"
}
elseif ($warnings.Count -gt 0) {
    $classification = "READY_WITH_WARNINGS"
}

Write-Host "=================================================="
Write-Host "SYNAPSECORE PRODUCT KNOWLEDGE CHECK"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
Write-Host "Classification"
Write-Host "--------------"
Write-Host $classification
Write-Host ""
Write-Host "Canonical Product Docs"
Write-Host "----------------------"
foreach ($doc in $canonicalProductDocs) {
    $status = if (Test-RepoPath $doc) { "present" } else { "missing" }
    Write-Host "- $doc [$status]"
}
Write-Host ""
Write-Host "Industry Docs"
Write-Host "-------------"
foreach ($doc in $industryDocs) {
    $status = if (Test-RepoPath $doc) { "present" } else { "missing" }
    Write-Host "- $doc [$status]"
}
Write-Host ""
Write-Host "Pilot Evidence Templates"
Write-Host "------------------------"
foreach ($template in $pilotTemplates) {
    $status = if (Test-RepoPath $template) { "present" } else { "missing" }
    Write-Host "- $template [$status]"
}

if ($blockers.Count -gt 0) {
    Write-Host ""
    Write-Host "Blockers"
    Write-Host "--------"
    foreach ($blocker in $blockers) {
        Write-Host "- $blocker"
    }
}

if ($warnings.Count -gt 0) {
    Write-Host ""
    Write-Host "Warnings"
    Write-Host "--------"
    foreach ($warning in $warnings) {
        Write-Host "- $warning"
    }
}

Write-Host ""
Write-Host "Notes"
Write-Host "-----"
Write-Host "- Informational only; this script does not edit runtime behavior."
Write-Host "- Product knowledge should be updated from pilot evidence, not speculative scope expansion."

if ($blockers.Count -gt 0) {
    exit 1
}
