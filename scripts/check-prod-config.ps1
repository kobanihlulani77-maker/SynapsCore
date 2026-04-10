param(
    [string]$BackendEnvFile = "./env/backend.prod.selfhost.env",
    [string]$FrontendEnvFile = "./env/frontend.prod.selfhost.env",
    [switch]$AllowPlaceholders
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "ProdEnvTools.ps1")

$rootDir = Get-SynapseRoot -ScriptPath $PSCommandPath
$infraDir = Join-Path $rootDir "infrastructure"

$backendFile = Resolve-SynapseEnvPath -InfrastructureDir $infraDir -RawPath $BackendEnvFile
$frontendFile = Resolve-SynapseEnvPath -InfrastructureDir $infraDir -RawPath $FrontendEnvFile

$backendValues = Read-SynapseEnvFile -FilePath $backendFile
$frontendValues = Read-SynapseEnvFile -FilePath $frontendFile

$backendProfile = Get-RequiredEnvValue -Values $backendValues -Key "SPRING_PROFILES_ACTIVE" -FilePath $backendFile
$backendDbHost = Get-RequiredEnvValue -Values $backendValues -Key "DB_HOST" -FilePath $backendFile
$backendDbName = Get-RequiredEnvValue -Values $backendValues -Key "DB_NAME" -FilePath $backendFile
$backendDbUser = Get-RequiredEnvValue -Values $backendValues -Key "DB_USER" -FilePath $backendFile
$backendDbPassword = Get-RequiredEnvValue -Values $backendValues -Key "DB_PASSWORD" -FilePath $backendFile
$backendRedisHost = Get-RequiredEnvValue -Values $backendValues -Key "REDIS_HOST" -FilePath $backendFile
$backendCorsAllowed = Get-RequiredEnvValue -Values $backendValues -Key "CORS_ALLOWED_ORIGINS" -FilePath $backendFile
$backendCookieSecure = Get-RequiredEnvValue -Values $backendValues -Key "SESSION_COOKIE_SECURE" -FilePath $backendFile
$backendSameSite = Get-RequiredEnvValue -Values $backendValues -Key "SESSION_COOKIE_SAME_SITE" -FilePath $backendFile
$backendHeaderFallback = Get-RequiredEnvValue -Values $backendValues -Key "ALLOW_HEADER_FALLBACK" -FilePath $backendFile
$backendDdlAuto = Get-RequiredEnvValue -Values $backendValues -Key "JPA_DDL_AUTO" -FilePath $backendFile
$backendBuildVersion = Get-RequiredEnvValue -Values $backendValues -Key "SYNAPSECORE_BUILD_VERSION" -FilePath $backendFile
$backendBuildCommit = Get-RequiredEnvValue -Values $backendValues -Key "SYNAPSECORE_BUILD_COMMIT" -FilePath $backendFile
$backendBuildTime = Get-RequiredEnvValue -Values $backendValues -Key "SYNAPSECORE_BUILD_TIME" -FilePath $backendFile
$frontendApiUrl = Get-RequiredEnvValue -Values $frontendValues -Key "VITE_API_URL" -FilePath $frontendFile
$frontendWsUrl = Get-RequiredEnvValue -Values $frontendValues -Key "VITE_WS_URL" -FilePath $frontendFile
$frontendBuildVersion = Get-RequiredEnvValue -Values $frontendValues -Key "VITE_APP_BUILD_VERSION" -FilePath $frontendFile
$frontendBuildCommit = Get-RequiredEnvValue -Values $frontendValues -Key "VITE_APP_BUILD_COMMIT" -FilePath $frontendFile
$frontendBuildTime = Get-RequiredEnvValue -Values $frontendValues -Key "VITE_APP_BUILD_TIME" -FilePath $frontendFile

Assert-EnvEquals -Values $backendValues -Key "SPRING_PROFILES_ACTIVE" -Expected "prod" -FilePath $backendFile
Assert-EnvEquals -Values $backendValues -Key "ALLOW_HEADER_FALLBACK" -Expected "false" -FilePath $backendFile
Assert-EnvEquals -Values $backendValues -Key "JPA_DDL_AUTO" -Expected "validate" -FilePath $backendFile

if ($backendSameSite -notin @("Lax", "Strict", "None")) {
    throw "SESSION_COOKIE_SAME_SITE must be one of Lax, Strict, or None in $backendFile"
}

if ($backendCorsAllowed -match 'https://' -and $backendCookieSecure -ne "true") {
    throw "SESSION_COOKIE_SECURE must be true when using HTTPS origins in $backendFile"
}

if ($frontendApiUrl -like 'https://*' -and $backendCookieSecure -ne "true") {
    throw "SESSION_COOKIE_SECURE must be true when VITE_API_URL is HTTPS"
}

if (-not $frontendWsUrl.EndsWith("/ws")) {
    throw "VITE_WS_URL should end with /ws in $frontendFile"
}

if (-not $AllowPlaceholders) {
    if (Test-SynapsePlaceholderValues -Values @(
            $backendDbHost,
            $backendDbName,
            $backendDbUser,
            $backendDbPassword,
            $backendRedisHost,
            $backendCorsAllowed,
            $backendBuildCommit,
            $backendBuildTime
        )) {
        throw "Backend env file still contains placeholder or example values: $backendFile"
    }

    if (Test-SynapsePlaceholderValues -Values @(
            $frontendApiUrl,
            $frontendWsUrl,
            $frontendBuildVersion,
            $frontendBuildCommit,
            $frontendBuildTime
        )) {
        throw "Frontend env file still contains placeholder example values: $frontendFile"
    }
}

Write-Host "SynapseCore prod config checks passed."
