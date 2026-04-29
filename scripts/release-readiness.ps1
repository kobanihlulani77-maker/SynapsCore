param(
    [string]$ComposeFile,
    [string]$BackendEnvFile = "./env/backend.prod.selfhost.env",
    [string]$FrontendEnvFile = "./env/frontend.prod.selfhost.env",
    [switch]$AllowPlaceholderEnv
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "ProdEnvTools.ps1")

$rootDir = Get-SynapseRoot -ScriptPath $PSCommandPath
$infraDir = Join-Path $rootDir "infrastructure"

if ([string]::IsNullOrWhiteSpace($ComposeFile)) {
    $ComposeFile = Join-Path $infraDir "docker-compose.prod.yml"
}

$checkArgs = @(
    "-ExecutionPolicy", "Bypass",
    "-File", (Join-Path $PSScriptRoot "check-prod-config.ps1"),
    "-BackendEnvFile", $BackendEnvFile,
    "-FrontendEnvFile", $FrontendEnvFile
)
if ($AllowPlaceholderEnv) {
    $checkArgs += "-AllowPlaceholders"
}

& powershell.exe @checkArgs | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Prod config checks failed."
}

& docker compose -f $ComposeFile config | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Production compose validation failed."
}

$backendFile = Resolve-SynapseEnvPath -InfrastructureDir $infraDir -RawPath $BackendEnvFile
$frontendFile = Resolve-SynapseEnvPath -InfrastructureDir $infraDir -RawPath $FrontendEnvFile
$backendValues = Read-SynapseEnvFile -FilePath $backendFile
$frontendValues = Read-SynapseEnvFile -FilePath $frontendFile

$backendProfile = Get-RequiredEnvValue -Values $backendValues -Key "SPRING_PROFILES_ACTIVE" -FilePath $backendFile
$backendOrigin = Get-RequiredEnvValue -Values $backendValues -Key "CORS_ALLOWED_ORIGINS" -FilePath $backendFile
$backendCookieSecure = Get-RequiredEnvValue -Values $backendValues -Key "SESSION_COOKIE_SECURE" -FilePath $backendFile
$backendDdlAuto = Get-RequiredEnvValue -Values $backendValues -Key "SPRING_JPA_HIBERNATE_DDL_AUTO" -FilePath $backendFile
$backendFlywayEnabled = if ($backendValues.ContainsKey("SPRING_FLYWAY_ENABLED") -and -not [string]::IsNullOrWhiteSpace([string]$backendValues["SPRING_FLYWAY_ENABLED"])) { [string]$backendValues["SPRING_FLYWAY_ENABLED"] } else { "true" }
$backendVersion = Get-RequiredEnvValue -Values $backendValues -Key "SYNAPSECORE_BUILD_VERSION" -FilePath $backendFile
$backendCommit = Get-RequiredEnvValue -Values $backendValues -Key "SYNAPSECORE_BUILD_COMMIT" -FilePath $backendFile
$backendBuildTime = Get-RequiredEnvValue -Values $backendValues -Key "SYNAPSECORE_BUILD_TIME" -FilePath $backendFile
$frontendApiUrl = Get-RequiredEnvValue -Values $frontendValues -Key "VITE_API_URL" -FilePath $frontendFile
$frontendWsUrl = Get-RequiredEnvValue -Values $frontendValues -Key "VITE_WS_URL" -FilePath $frontendFile
$frontendVersion = Get-RequiredEnvValue -Values $frontendValues -Key "VITE_APP_BUILD_VERSION" -FilePath $frontendFile
$frontendCommit = Get-RequiredEnvValue -Values $frontendValues -Key "VITE_APP_BUILD_COMMIT" -FilePath $frontendFile
$frontendBuildTime = Get-RequiredEnvValue -Values $frontendValues -Key "VITE_APP_BUILD_TIME" -FilePath $frontendFile

Write-Host "========================================"
Write-Host "SYNAPSECORE RELEASE READINESS"
Write-Host "========================================"
Write-Host "Compose file       : $ComposeFile"
Write-Host "Backend env file   : $BackendEnvFile"
Write-Host "Frontend env file  : $FrontendEnvFile"
Write-Host ""
Write-Host "Backend fingerprint"
Write-Host "  Profile          : $backendProfile"
Write-Host "  Version          : $backendVersion"
Write-Host "  Commit           : $backendCommit"
Write-Host "  Build time       : $backendBuildTime"
Write-Host "  Allowed origins  : $backendOrigin"
Write-Host "  Secure cookies   : $backendCookieSecure"
Write-Host "  Hibernate mode   : $backendDdlAuto"
Write-Host "  Flyway enabled   : $backendFlywayEnabled"
Write-Host ""
Write-Host "Frontend fingerprint"
Write-Host "  Version          : $frontendVersion"
Write-Host "  Commit           : $frontendCommit"
Write-Host "  Build time       : $frontendBuildTime"
Write-Host "  API URL          : $frontendApiUrl"
Write-Host "  WS URL           : $frontendWsUrl"
Write-Host ""
Write-Host "Final signoff lane"
Write-Host "  Config gate      : powershell -ExecutionPolicy Bypass -File scripts\\check-prod-config.ps1 -BackendEnvFile $BackendEnvFile -FrontendEnvFile $FrontendEnvFile"
Write-Host "  Hosted prep      : powershell -ExecutionPolicy Bypass -File scripts\\prepare-hosted-proof.ps1"
Write-Host "  Hosted proof     : cd frontend && npm.cmd run test:e2e:prod"
Write-Host ""
Write-Host "Supplemental self-host smoke"
Write-Host "  Smoke verify     : powershell -ExecutionPolicy Bypass -File scripts\\verify-deployment.ps1 -FrontendUrl http://localhost -BackendUrl http://localhost:8080"
Write-Host "  Realtime smoke   : powershell -ExecutionPolicy Bypass -File scripts\\verify-realtime.ps1 -FrontendUrl http://localhost -BackendUrl http://localhost:8080"
Write-Host "  Workflow smoke   : powershell -ExecutionPolicy Bypass -File scripts\\verify-company-readiness.ps1 -FrontendUrl http://localhost -BackendUrl http://localhost:8080"
Write-Host ""
Write-Host "Recovery / operations"
Write-Host "  Backup           : powershell -ExecutionPolicy Bypass -File scripts\\backup-postgres.ps1"
Write-Host "  Restore          : powershell -ExecutionPolicy Bypass -File scripts\\restore-postgres.ps1 -BackupFile backups\\<backup>.sql -Yes"
Write-Host "  Restore drill    : powershell -ExecutionPolicy Bypass -File scripts\\verify-restore-drill.ps1"
Write-Host ""
Write-Host "Hosted proof is the primary final signoff lane."
Write-Host "Localhost verification remains useful for self-host smoke only."
Write-Host ""
Write-Host "Release readiness checks passed."
