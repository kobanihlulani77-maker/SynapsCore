param(
    [string]$BackendEnvFile = "./env/backend.prod.selfhost.env",
    [string]$FrontendEnvFile = "./env/frontend.prod.selfhost.env",
    [switch]$SkipVerify,
    [switch]$AllowPlaceholderEnv
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "ProdEnvTools.ps1")

$rootDir = Get-SynapseRoot -ScriptPath $PSCommandPath
$infraDir = Join-Path $rootDir "infrastructure"

$checkArgs = @(
    "-ExecutionPolicy", "Bypass",
    "-File", (Join-Path $PSScriptRoot "check-prod-config.ps1"),
    "-BackendEnvFile", $BackendEnvFile,
    "-FrontendEnvFile", $FrontendEnvFile
)

if ($AllowPlaceholderEnv) {
    $checkArgs += "-AllowPlaceholders"
}

& powershell @checkArgs
if ($LASTEXITCODE -ne 0) {
    throw "Prod config checks failed."
}

Write-Host "========================================"
Write-Host "SYNAPSECORE PROD START"
Write-Host "========================================"
Write-Host "Backend env file : $BackendEnvFile"
Write-Host "Frontend env file: $FrontendEnvFile"
Write-Host ""

Push-Location $infraDir
try {
    $env:BACKEND_ENV_FILE = $BackendEnvFile
    $env:FRONTEND_ENV_FILE = $FrontendEnvFile
    docker compose -f docker-compose.prod.yml up --build -d
    if ($LASTEXITCODE -ne 0) {
        throw "Production compose startup failed."
    }
}
finally {
    Pop-Location
}

if ($SkipVerify) {
    Write-Host ""
    Write-Host "Production-shaped stack started without smoke verification."
    exit 0
}

Write-Host ""
Write-Host "Running deployment smoke verification..."
& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "verify-deployment.ps1")
if ($LASTEXITCODE -ne 0) {
    throw "Production smoke verification failed."
}
