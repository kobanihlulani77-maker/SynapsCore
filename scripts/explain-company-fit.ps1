Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
$generator = Join-Path $PSScriptRoot "generate-company-fit-report.mjs"

Write-Host "=================================================="
Write-Host "SYNAPSECORE COMPANY FIT ANALYZER"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
Write-Host "Using the real company-fit generator grounded in the current supported platform scope."
Write-Host ""

$nodeArgs = @($generator)
if ($args.Count -gt 0) {
    $nodeArgs += $args
} else {
    $nodeArgs += @("--all", "--format", "markdown")
}

& node @nodeArgs
