param(
    [string]$FrontendUrl = "http://127.0.0.1:5173",
    [string]$BackendUrl = "http://127.0.0.1:8080",
    [string]$TenantCode = "STARTER-OPS",
    [string]$OperationsLeadUsername = "operations.lead",
    [string]$OperationsLeadPassword = "lead-2026",
    [string]$OperationsPlannerUsername = "operations.planner",
    [string]$OperationsPlannerPassword = "planner-2026",
    [string]$IntegrationLeadUsername = "integration.lead",
    [string]$IntegrationLeadPassword = "integration-admin-2026",
    [string]$ProofProductSku = "SKU-FLX-100"
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
$env:PLAYWRIGHT_TENANT_CODE = $TenantCode
$env:PLAYWRIGHT_TENANT_ADMIN_USERNAME = $OperationsLeadUsername
$env:PLAYWRIGHT_TENANT_ADMIN_PASSWORD = $OperationsLeadPassword
$env:PLAYWRIGHT_PLANNER_USERNAME = $OperationsPlannerUsername
$env:PLAYWRIGHT_PLANNER_PASSWORD = $OperationsPlannerPassword
$env:PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME = $IntegrationLeadUsername
$env:PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD = $IntegrationLeadPassword
$env:PLAYWRIGHT_PROOF_PRODUCT_SKU = $ProofProductSku

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
    Remove-Item Env:PLAYWRIGHT_TENANT_CODE -ErrorAction SilentlyContinue
    Remove-Item Env:PLAYWRIGHT_TENANT_ADMIN_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:PLAYWRIGHT_TENANT_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:PLAYWRIGHT_PLANNER_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:PLAYWRIGHT_PLANNER_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:PLAYWRIGHT_PROOF_PRODUCT_SKU -ErrorAction SilentlyContinue
}
