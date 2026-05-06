Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Write-Host "SynapseCore security verification"
Write-Host "Repository: $repoRoot"
Write-Host ""

Write-Host "1. Running secret scan..."
& (Join-Path $PSScriptRoot "secret-scan.ps1")
$secretScanExit = $LASTEXITCODE
Write-Host ""

Write-Host "2. Checking working tree..."
$statusLines = @(& git -C $repoRoot status --short)
if ($LASTEXITCODE -ne 0) {
    throw "git status failed in $repoRoot"
}

if ($statusLines.Count -eq 0) {
    Write-Host "- Working tree is clean."
} else {
    Write-Host "- Working tree has local changes:"
    $statusLines | ForEach-Object { Write-Host "  $_" }
}

Write-Host ""
Write-Host "3. Checking for tracked proof artifacts in git status..."
$artifactStatus = @($statusLines | Where-Object { $_ -match 'frontend/(playwright-report|test-results)' })
if ($artifactStatus.Count -eq 0) {
    Write-Host "- No Playwright proof artifacts are present in git status."
} else {
    Write-Host "- Playwright proof artifacts are present in git status:"
    $artifactStatus | ForEach-Object { Write-Host "  $_" }
}

if ($secretScanExit -ne 0) {
    exit $secretScanExit
}
