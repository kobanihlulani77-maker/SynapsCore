param(
    [switch]$SkipFrontendVerify,
    [switch]$SkipLiveCheck
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
Set-Location $rootDir

$blockers = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

function Test-PathSafe {
    param([Parameter(Mandatory = $true)][string]$Path)
    return Test-Path -LiteralPath (Join-Path $rootDir $Path)
}

function Add-Blocker {
    param([string]$Message)
    $blockers.Add($Message)
}

function Add-Warning {
    param([string]$Message)
    $warnings.Add($Message)
}

function Invoke-ReadinessStep {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Script
    )

    Write-Host ""
    Write-Host $Name
    Write-Host ("-" * $Name.Length)

    try {
        & $Script
        Write-Host "PASS"
    }
    catch {
        Write-Host "FAIL: $($_.Exception.Message)"
        Add-Blocker "$Name failed: $($_.Exception.Message)"
    }
}

function Assert-PathPresent {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-PathSafe $Path)) {
        throw "$Path is missing."
    }
}

$currentCommit = (git rev-parse HEAD).Trim()
$originMain = (git rev-parse origin/main).Trim()
$gitStatus = @(git status --short)
$trackedRiskyFiles = @(git ls-files |
    Where-Object {
        $_ -match '(^|/)playwright-report/' -or
        $_ -match '(^|/)test-results/' -or
        $_ -match 'frontend/\.hosted-proof/' -or
        $_ -match '\.env\.local$' -or
        $_ -match 'proof-run-archive\.zip$'
    })

$requiredDocs = @(
    "docs/engineering-review.md",
    "docs/maintainability-guide.md",
    "docs/support-playbook.md",
    "docs/operations-handbook.md",
    "docs/change-management.md",
    "docs/release-engineering.md",
    "docs/quality-gates.md",
    "docs/future-engineering-strategy.md",
    "docs/repository-maturity.md",
    "docs/pilot-release-candidate.md",
    "docs/release-evidence-2026-08-03.md",
    "docs/verification-status.md",
    "docs/hosted-proof.md",
    "docs/current-limitations.md"
)

$requiredScripts = @(
    "scripts/engineering-readiness.ps1",
    "scripts/pilot-rc-check.ps1",
    "scripts/repo-health.ps1",
    "scripts/docs-link-check.ps1",
    "scripts/check-live-connections.ps1",
    "scripts/prepare-hosted-proof.ps1"
)

Write-Host "=================================================="
Write-Host "SYNAPSECORE ENGINEERING READINESS"
Write-Host "=================================================="
Write-Host "Repo root      : $rootDir"
Write-Host "Current commit : $currentCommit"
Write-Host "Origin main    : $originMain"
Write-Host ""

Invoke-ReadinessStep -Name "Repository alignment" -Script {
    if ($currentCommit -ne $originMain) {
        throw "HEAD does not match origin/main."
    }
}

Invoke-ReadinessStep -Name "Working tree production cleanliness" -Script {
    $productionChanges = @($gitStatus | Where-Object {
        $_ -notmatch 'frontend/proof-run-archive\.zip' -and
        $_ -notmatch 'backend/.env.local.example' -and
        $_ -notmatch 'frontend/.env.local' -and
        $_ -notmatch 'frontend/.env.local.example'
    })
    if ($productionChanges.Count -gt 0) {
        throw "Uncommitted production changes exist: $($productionChanges -join '; ')"
    }
    if ($gitStatus -match 'frontend/proof-run-archive\.zip') {
        Add-Warning "frontend/proof-run-archive.zip is local evidence and is not committed."
    }
}

Invoke-ReadinessStep -Name "Tracked artifact and secret safety" -Script {
    if ($trackedRiskyFiles.Count -gt 0) {
        throw "Risky local artifact or env paths are tracked: $($trackedRiskyFiles -join '; ')"
    }
}

Invoke-ReadinessStep -Name "Documentation completeness" -Script {
    foreach ($doc in $requiredDocs) {
        Assert-PathPresent $doc
    }
}

Invoke-ReadinessStep -Name "Script completeness" -Script {
    foreach ($script in $requiredScripts) {
        Assert-PathPresent $script
    }
}

Invoke-ReadinessStep -Name "Hosted proof evidence" -Script {
    $evidence = Get-Content -LiteralPath (Join-Path $rootDir "docs/release-evidence-2026-08-03.md") -Raw
    if ($evidence -notmatch '6 passed \(4\.1m\)') {
        throw "Replacement DB hosted proof result is not recorded."
    }
}

Invoke-ReadinessStep -Name "Docs link check" -Script {
    powershell -ExecutionPolicy Bypass -File scripts\docs-link-check.ps1 | Out-Host
}

if ($SkipFrontendVerify) {
    Write-Host ""
    Write-Host "Frontend verify"
    Write-Host "---------------"
    Write-Host "SKIPPED by -SkipFrontendVerify"
    Add-Warning "Frontend verify was skipped."
}
else {
    Invoke-ReadinessStep -Name "Frontend verify" -Script {
        Push-Location (Join-Path $rootDir "frontend")
        try {
            npm.cmd run verify | Out-Host
        }
        finally {
            Pop-Location
        }
    }
}

if ($SkipLiveCheck) {
    Write-Host ""
    Write-Host "Live connection check"
    Write-Host "---------------------"
    Write-Host "SKIPPED by -SkipLiveCheck"
    Add-Warning "Live connection check was skipped."
}
else {
    Invoke-ReadinessStep -Name "Live connection check" -Script {
        $liveOutput = @(powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1)
        $liveOutput | Out-Host
        if (-not ($liveOutput -contains "PROOF_ALLOWED=True")) {
            throw "Live connection check did not report PROOF_ALLOWED=True."
        }
    }
}

$ready = $blockers.Count -eq 0

Write-Host ""
Write-Host "Engineering Readiness Classification"
Write-Host "------------------------------------"
Write-Host "ENGINEERING_READY=$ready"
Write-Host "CURRENT_COMMIT=$currentCommit"
Write-Host "HOSTED_PROOF_EVIDENCE=docs/release-evidence-2026-08-03.md"

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
Write-Host "- Informational only; this script does not deploy, tag, or run hosted proof."
Write-Host "- Hosted proof should only rerun after runtime/proof-covered changes or intentional release signoff."
