param(
    [string]$FrontendUrl = "http://localhost",
    [string]$BackendUrl = "http://127.0.0.1:8080"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
$frontendDir = Join-Path $rootDir "frontend"

Write-Host "========================================"
Write-Host "SYNAPSECORE REALTIME VERIFICATION"
Write-Host "========================================"
Write-Host "Frontend URL : $FrontendUrl"
Write-Host "Backend URL  : $BackendUrl"
Write-Host ""

$env:PLAYWRIGHT_BASE_URL = $FrontendUrl
$env:PLAYWRIGHT_API_BASE_URL = $BackendUrl

Push-Location $frontendDir
try {
    npm.cmd run test:e2e:realtime
    if ($LASTEXITCODE -ne 0) {
        throw "Realtime browser verification failed."
    }
} finally {
    Pop-Location
    Remove-Item Env:PLAYWRIGHT_BASE_URL -ErrorAction SilentlyContinue
    Remove-Item Env:PLAYWRIGHT_API_BASE_URL -ErrorAction SilentlyContinue
}
