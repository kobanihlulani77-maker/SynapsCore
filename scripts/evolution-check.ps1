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
        Add-Blocker "Missing required evolution doc: $Path"
    }
}

$currentCommit = (git rev-parse HEAD).Trim()
$originMain = (git rev-parse origin/main).Trim()
$gitStatus = @(git status --short)

$productionChanges = @($gitStatus | Where-Object {
    $_ -notmatch 'frontend/proof-run-archive\.zip' -and
    $_ -notmatch 'backend/.env.local.example' -and
    $_ -notmatch 'frontend/.env.local' -and
    $_ -notmatch 'frontend/.env.local.example'
})

$trackedRiskyFiles = @(git ls-files | Where-Object {
    $_ -match '(^|/)playwright-report/' -or
    $_ -match '(^|/)test-results/' -or
    $_ -match 'frontend/\.hosted-proof/' -or
    $_ -match '\.env\.local$' -or
    $_ -match 'proof-run-archive\.zip$'
})

$requiredEvolutionDocs = @(
    "docs/product-evolution-framework.md",
    "docs/improvement-lifecycle.md",
    "docs/product-decision-principles.md",
    "docs/release-evolution-model.md",
    "docs/platform-maturity-model.md"
)

$requiredFoundationDocs = @(
    "docs/pilot-release-candidate.md",
    "docs/release-evidence-2026-08-03.md",
    "docs/product-knowledge-base.md",
    "docs/engineering-review.md",
    "docs/operations-handbook.md",
    "docs/support-playbook.md",
    "docs/quality-gates.md",
    "docs/current-limitations.md",
    "docs/known-operational-risks.md",
    "docs/master-product-roadmap.md"
)

foreach ($doc in $requiredEvolutionDocs) {
    Assert-Doc $doc
}

foreach ($doc in $requiredFoundationDocs) {
    Assert-Doc $doc
}

if ($currentCommit -ne $originMain) {
    Add-Warning "HEAD does not match origin/main. Push or pull before final evolution signoff."
}

if ($productionChanges.Count -gt 0) {
    Add-Blocker "Uncommitted production changes exist: $($productionChanges -join '; ')"
}

if ($trackedRiskyFiles.Count -gt 0) {
    Add-Blocker "Risky proof artifact or env file is tracked: $($trackedRiskyFiles -join '; ')"
}

if ($gitStatus -match 'frontend/proof-run-archive\.zip') {
    Add-Warning "frontend/proof-run-archive.zip is local evidence/artifact and is not committed."
}

$releaseEvidencePath = Join-Path $rootDir "docs/release-evidence-2026-08-03.md"
$releaseEvidence = ""
if (Test-Path -LiteralPath $releaseEvidencePath) {
    $releaseEvidence = Get-Content -LiteralPath $releaseEvidencePath -Raw
    if ($releaseEvidence -notmatch '6 passed') {
        Add-Blocker "Release evidence does not record hosted proof passing."
    }
}

$pilotRcPath = Join-Path $rootDir "docs/pilot-release-candidate.md"
$pilotStatus = "UNKNOWN"
if (Test-Path -LiteralPath $pilotRcPath) {
    $pilotText = Get-Content -LiteralPath $pilotRcPath -Raw
    if ($pilotText -match 'Pilot Release Candidate|v0\.9\.0-pilot-rc1') {
        $pilotStatus = "PILOT_RC_DOCUMENTED"
    }
}

$docMaturity = if ($requiredEvolutionDocs | Where-Object { -not (Test-RepoPath $_) }) {
    "INCOMPLETE"
}
else {
    "EVOLUTION_DOCS_PRESENT"
}

$proofEvidence = if ($releaseEvidence -match '6 passed') {
    "HOSTED_PROOF_RECORDED"
}
else {
    "HOSTED_PROOF_NOT_CONFIRMED"
}

$engineeringReadiness = if ((Test-RepoPath "scripts/engineering-readiness.ps1") -and (Test-RepoPath "docs/repository-maturity.md")) {
    "ENGINEERING_READINESS_LAYER_PRESENT"
}
else {
    "ENGINEERING_READINESS_LAYER_INCOMPLETE"
}

$outstandingImprovementSources = @(
    "docs/current-limitations.md",
    "docs/known-operational-risks.md",
    "docs/enterprise-hardening-roadmap.md",
    "docs/engineering-priorities.md",
    "docs/future-engineering-strategy.md"
) | Where-Object { Test-RepoPath $_ }

$ready = $blockers.Count -eq 0

Write-Host "=================================================="
Write-Host "SYNAPSECORE EVOLUTION CHECK"
Write-Host "=================================================="
Write-Host "Repo root        : $rootDir"
Write-Host "Current release  : v0.9.0-pilot-rc1"
Write-Host "Current commit   : $currentCommit"
Write-Host "Origin main      : $originMain"
Write-Host ""

Write-Host "Current Proof Evidence"
Write-Host "----------------------"
Write-Host "PROOF_EVIDENCE=$proofEvidence"
Write-Host "Evidence doc: docs\release-evidence-2026-08-03.md"
Write-Host ""

Write-Host "Documentation Maturity"
Write-Host "----------------------"
Write-Host "DOC_MATURITY=$docMaturity"
Write-Host "Evolution docs:"
$requiredEvolutionDocs | ForEach-Object {
    $status = if (Test-RepoPath $_) { "present" } else { "missing" }
    Write-Host "- $_ [$status]"
}
Write-Host ""

Write-Host "Pilot Status"
Write-Host "------------"
Write-Host "PILOT_STATUS=$pilotStatus"
Write-Host "Supported scope: controlled pilot platform, not broad enterprise GA"
Write-Host ""

Write-Host "Engineering Readiness Summary"
Write-Host "-----------------------------"
Write-Host "ENGINEERING_READINESS=$engineeringReadiness"
Write-Host "Full readiness command: powershell -ExecutionPolicy Bypass -File scripts\engineering-readiness.ps1"
Write-Host ""

Write-Host "Outstanding Improvement Sources"
Write-Host "-------------------------------"
if ($outstandingImprovementSources.Count -eq 0) {
    Write-Host "- none found"
}
else {
    $outstandingImprovementSources | ForEach-Object { Write-Host "- $_" }
}
Write-Host ""

Write-Host "Evolution Readiness Classification"
Write-Host "----------------------------------"
Write-Host "EVOLUTION_READY=$ready"
Write-Host ""

Write-Host "Blockers"
Write-Host "--------"
if ($blockers.Count -eq 0) {
    Write-Host "- none"
}
else {
    $blockers | ForEach-Object { Write-Host "- $_" }
}
Write-Host ""

Write-Host "Warnings"
Write-Host "--------"
if ($warnings.Count -eq 0) {
    Write-Host "- none"
}
else {
    $warnings | ForEach-Object { Write-Host "- $_" }
}
Write-Host ""

Write-Host "Notes"
Write-Host "-----"
Write-Host "- Informational only; this script does not deploy, edit runtime behavior, or run hosted proof."
Write-Host "- Future product work should start from operational evidence, not feature brainstorming."
Write-Host "- Use the improvement lifecycle before implementing product changes."
