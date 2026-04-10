param(
    [string]$BackendEnvFile = "./env/backend.prod.env",
    [string]$FrontendEnvFile = "./env/frontend.prod.env",
    [string]$EdgeEnvFile = "./env/edge.prod.env",
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

$edgeFile = Resolve-SynapseEnvPath -InfrastructureDir $infraDir -RawPath $EdgeEnvFile
$edgeValues = Read-SynapseEnvFile -FilePath $edgeFile
$appDomain = Get-RequiredEnvValue -Values $edgeValues -Key "SYNAPSECORE_APP_DOMAIN" -FilePath $edgeFile
$apiDomain = Get-RequiredEnvValue -Values $edgeValues -Key "SYNAPSECORE_API_DOMAIN" -FilePath $edgeFile
$acmeEmail = Get-RequiredEnvValue -Values $edgeValues -Key "SYNAPSECORE_ACME_EMAIL" -FilePath $edgeFile

if (-not $AllowPlaceholderEnv -and (Test-SynapsePlaceholderValues -Values @($appDomain, $apiDomain, $acmeEmail))) {
    throw "Edge env file still contains placeholder or example values: $edgeFile"
}

Write-Host "========================================"
Write-Host "SYNAPSECORE PUBLIC PROD START"
Write-Host "========================================"
Write-Host "Backend env file : $BackendEnvFile"
Write-Host "Frontend env file: $FrontendEnvFile"
Write-Host "Edge env file    : $EdgeEnvFile"
Write-Host "App domain       : $appDomain"
Write-Host "API domain       : $apiDomain"
Write-Host ""

Push-Location $infraDir
try {
    $env:BACKEND_ENV_FILE = $BackendEnvFile
    $env:FRONTEND_ENV_FILE = $FrontendEnvFile
    $env:EDGE_ENV_FILE = $EdgeEnvFile
    docker compose -f docker-compose.public.yml up --build -d
    if ($LASTEXITCODE -ne 0) {
        throw "Public production compose startup failed."
    }
}
finally {
    Pop-Location
}

if ($SkipVerify) {
    Write-Host ""
    Write-Host "Public production stack started without smoke verification."
    exit 0
}

Write-Host ""
Write-Host "Running public deployment smoke verification..."
& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "verify-deployment.ps1") -FrontendUrl "https://$appDomain" -BackendUrl "https://$apiDomain"
if ($LASTEXITCODE -ne 0) {
    throw "Public production smoke verification failed."
}
