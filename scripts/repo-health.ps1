Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
Set-Location $rootDir

function Test-PathSafe {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    return Test-Path -LiteralPath (Join-Path $rootDir $Path)
}

$gitStatus = git status --short
$gitIgnoredStatus = git status --ignored --short

$localOnlyFiles = @(
    "backend/.env.local",
    "backend/.env.local.example",
    "frontend/.env.local",
    "frontend/.env.local.example"
)

$artifactPaths = @(
    "frontend/playwright-report",
    "frontend/test-results",
    "frontend/dist",
    "frontend/node_modules",
    "backend/target",
    "backups"
)

$keyDocs = @(
    "README.md",
    "docs/INDEX.md",
    "docs/documentation-map.md",
    "docs/scripts-reference.md",
    "docs/system-architecture.md",
    "docs/proof-and-validation.md"
)

$keyScripts = @(
    "scripts/check-live-connections.ps1",
    "scripts/check-local-connections.ps1",
    "scripts/explain-infrastructure.ps1",
    "scripts/project-map.ps1",
    "scripts/repo-health.ps1",
    "frontend/scripts/frontend-check.mjs"
)

$presentLocalOnlyFiles = @($localOnlyFiles | Where-Object { Test-PathSafe $_ })
$presentArtifactPaths = @($artifactPaths | Where-Object { Test-PathSafe $_ })
$missingDocs = @($keyDocs | Where-Object { -not (Test-PathSafe $_) })
$missingScripts = @($keyScripts | Where-Object { -not (Test-PathSafe $_) })

$frontendPackagePath = Join-Path $rootDir "frontend/package.json"
$frontendVerifyAvailable = $false

if (Test-Path -LiteralPath $frontendPackagePath) {
    try {
        $packageJson = Get-Content -LiteralPath $frontendPackagePath -Raw | ConvertFrom-Json
        if ($packageJson.scripts.verify) {
            $frontendVerifyAvailable = $true
        }
    }
    catch {
        $frontendVerifyAvailable = $false
    }
}

$unexpectedGitStatus = @()
foreach ($line in $gitStatus) {
    if ([string]::IsNullOrWhiteSpace($line)) {
        continue
    }

    if ($line -match "backend/.env.local.example" -or $line -match "frontend/.env.local" -or $line -match "frontend/.env.local.example") {
        continue
    }

    $unexpectedGitStatus += $line
}

$classification = "CLEAN"
if (($presentLocalOnlyFiles.Count -gt 0) -and ($presentArtifactPaths.Count -gt 0 -or $unexpectedGitStatus.Count -gt 0 -or $missingDocs.Count -gt 0 -or $missingScripts.Count -gt 0)) {
    $classification = "NEEDS_ATTENTION"
}
elseif ($presentLocalOnlyFiles.Count -gt 0) {
    $classification = "LOCAL_ONLY_FILES_PRESENT"
}
elseif ($presentArtifactPaths.Count -gt 0) {
    $classification = "ARTIFACTS_PRESENT"
}

Write-Host "=================================================="
Write-Host "SYNAPSECORE REPO HEALTH"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""

Write-Host "Classification"
Write-Host "--------------"
Write-Host $classification
Write-Host ""

Write-Host "Git Status"
Write-Host "----------"
if ($gitStatus.Count -eq 0) {
    Write-Host "- clean working tree"
}
else {
    $gitStatus | ForEach-Object { Write-Host "- $_" }
}
Write-Host ""

Write-Host "Ignored Risky Files / Paths"
Write-Host "--------------------------"
if ($gitIgnoredStatus.Count -eq 0) {
    Write-Host "- none"
}
else {
    $gitIgnoredStatus |
        Where-Object {
            $_ -match "playwright-report" -or
            $_ -match "test-results" -or
            $_ -match "dist/" -or
            $_ -match "node_modules/" -or
            $_ -match "target/" -or
            $_ -match "\.log" -or
            $_ -match "Playwright.env" -or
            $_ -match "backups/"
        } |
        ForEach-Object { Write-Host "- $_" }
}
Write-Host ""

Write-Host "Local Env Files"
Write-Host "---------------"
if ($presentLocalOnlyFiles.Count -eq 0) {
    Write-Host "- none"
}
else {
    $presentLocalOnlyFiles | ForEach-Object { Write-Host "- $_" }
}
Write-Host ""

Write-Host "Artifact Paths Present"
Write-Host "----------------------"
if ($presentArtifactPaths.Count -eq 0) {
    Write-Host "- none"
}
else {
    $presentArtifactPaths | ForEach-Object { Write-Host "- $_" }
}
Write-Host ""

Write-Host "Frontend Verify Availability"
Write-Host "----------------------------"
Write-Host "FRONTEND_VERIFY_AVAILABLE=$frontendVerifyAvailable"
Write-Host ""

Write-Host "Key Docs"
Write-Host "--------"
if ($missingDocs.Count -eq 0) {
    Write-Host "- all required docs present"
}
else {
    $missingDocs | ForEach-Object { Write-Host "- missing: $_" }
}
Write-Host ""

Write-Host "Key Scripts"
Write-Host "-----------"
if ($missingScripts.Count -eq 0) {
    Write-Host "- all required scripts present"
}
else {
    $missingScripts | ForEach-Object { Write-Host "- missing: $_" }
}
Write-Host ""

Write-Host "Notes"
Write-Host "-----"
Write-Host "- This script is informational only."
Write-Host "- It does not delete or modify files."
Write-Host "- Local env files should remain unstaged."
Write-Host "- Ignored artifacts are normal as long as they do not leak into commits."
