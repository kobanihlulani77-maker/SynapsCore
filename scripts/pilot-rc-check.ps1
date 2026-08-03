param(
    [string]$ReleaseCandidate = "v0.9.0-pilot-rc1",
    [switch]$SkipFrontendVerify,
    [switch]$SkipLiveCheck
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
Set-Location $rootDir

$blockers = New-Object System.Collections.Generic.List[string]

function Test-PathSafe {
    param([Parameter(Mandatory = $true)][string]$Path)
    return Test-Path -LiteralPath (Join-Path $rootDir $Path)
}

function Invoke-Step {
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
        return $true
    }
    catch {
        Write-Host "FAIL: $($_.Exception.Message)"
        $blockers.Add("$Name failed: $($_.Exception.Message)")
        return $false
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
        $_ -match '\.env\.local$' -or
        $_ -match 'frontend/\.hosted-proof/' -or
        $_ -match 'proof-run-archive\.zip$'
    })

Write-Host "=================================================="
Write-Host "SYNAPSECORE PILOT RC CHECK"
Write-Host "=================================================="
Write-Host "Release candidate : $ReleaseCandidate"
Write-Host "Repo root         : $rootDir"
Write-Host "Current commit    : $currentCommit"
Write-Host "Origin main       : $originMain"
Write-Host ""

Invoke-Step -Name "Git alignment" -Script {
    if ($currentCommit -ne $originMain) {
        throw "main does not match origin/main."
    }
}

Invoke-Step -Name "Working tree production cleanliness" -Script {
    $productionChanges = @($gitStatus | Where-Object {
        $_ -notmatch 'frontend/proof-run-archive\.zip' -and
        $_ -notmatch 'backend/.env.local.example' -and
        $_ -notmatch 'frontend/.env.local' -and
        $_ -notmatch 'frontend/.env.local.example'
    })
    if ($productionChanges.Count -gt 0) {
        throw "Uncommitted production changes exist: $($productionChanges -join '; ')"
    }
}

Invoke-Step -Name "Tracked artifact and secret safety" -Script {
    if ($trackedRiskyFiles.Count -gt 0) {
        throw "Risky files are tracked: $($trackedRiskyFiles -join '; ')"
    }
}

Invoke-Step -Name "Evidence and pilot docs" -Script {
    Assert-PathPresent "docs/release-evidence-2026-08-03.md"
    Assert-PathPresent "docs/pilot-release-candidate.md"
    Assert-PathPresent "docs/pilot-company-onboarding-checklist.md"
    Assert-PathPresent "docs/pilot-operator-checklist.md"
    Assert-PathPresent "docs/pilot-success-metrics.md"
    Assert-PathPresent "docs/pilot-rollback-and-escalation.md"
}

Invoke-Step -Name "Docs link check" -Script {
    powershell -ExecutionPolicy Bypass -File scripts\docs-link-check.ps1 | Out-Host
}

if ($SkipFrontendVerify) {
    Write-Host ""
    Write-Host "Frontend verify"
    Write-Host "---------------"
    Write-Host "SKIPPED by -SkipFrontendVerify"
}
else {
    Invoke-Step -Name "Frontend verify" -Script {
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
    Write-Host "Live connection gate"
    Write-Host "--------------------"
    Write-Host "SKIPPED by -SkipLiveCheck"
}
else {
    Invoke-Step -Name "Live connection gate" -Script {
        $liveOutput = @(powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1)
        $liveOutput | Out-Host
        if (-not ($liveOutput -contains "PROOF_ALLOWED=True")) {
            throw "Live connection check did not report PROOF_ALLOWED=True."
        }
    }
}

$ready = $blockers.Count -eq 0

Write-Host ""
Write-Host "Pilot RC Classification"
Write-Host "-----------------------"
Write-Host "PILOT_RC_READY=$ready"
Write-Host "CURRENT_COMMIT=$currentCommit"
Write-Host "PROOF_EVIDENCE=docs/release-evidence-2026-08-03.md"

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
Write-Host "Notes"
Write-Host "-----"
Write-Host "- Informational only; this script does not deploy or tag."
Write-Host "- Do not commit frontend/.hosted-proof, frontend/test-results, frontend/playwright-report, or env files."
Write-Host "- Create the release tag only after this script reports PILOT_RC_READY=True on the committed RC state."
