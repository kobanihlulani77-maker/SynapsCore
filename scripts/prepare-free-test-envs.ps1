param(
    [Parameter(Mandatory = $true)]
    [string]$PublicIp,
    [Parameter(Mandatory = $true)]
    [string]$AcmeEmail,
    [ValidateSet("sslip.io", "nip.io")]
    [string]$DnsSuffix = "sslip.io",
    [string]$DbPassword,
    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "ProdEnvTools.ps1")

$rootDir = Get-SynapseRoot -ScriptPath $PSCommandPath
$envDir = Join-Path $rootDir "infrastructure\env"
$backendTarget = Join-Path $envDir "backend.prod.env"
$frontendTarget = Join-Path $envDir "frontend.prod.env"
$edgeTarget = Join-Path $envDir "edge.prod.env"

function Assert-ValidIpv4 {
    param([string]$IpAddress)

    $ipObject = $null
    if (-not [System.Net.IPAddress]::TryParse($IpAddress, [ref]$ipObject)) {
        throw "PublicIp must be a valid IPv4 address."
    }

    if ($IpAddress -notmatch '^\d{1,3}(\.\d{1,3}){3}$') {
        throw "PublicIp must be a dotted IPv4 address."
    }
}

function Write-Utf8NoBomFile {
    param(
        [string]$Path,
        [string[]]$Lines
    )

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($Path, $Lines, $encoding)
}

function Set-SynapseEnvValue {
    param(
        [string]$FilePath,
        [string]$Key,
        [string]$Value
    )

    $lines = if (Test-Path -LiteralPath $FilePath) { @(Get-Content -LiteralPath $FilePath) } else { @() }
    $pattern = '^{0}=' -f [regex]::Escape($Key)
    $updated = $false
    $newLines = foreach ($line in $lines) {
        if ($line -match $pattern) {
            $updated = $true
            "${Key}=${Value}"
        }
        else {
            $line
        }
    }

    if (-not $updated) {
        $newLines += "${Key}=${Value}"
    }

    Write-Utf8NoBomFile -Path $FilePath -Lines $newLines
}

function Get-RandomPassword {
    return ([guid]::NewGuid().ToString("N") + [guid]::NewGuid().ToString("N")).Substring(0, 32)
}

Assert-ValidIpv4 -IpAddress $PublicIp

& (Join-Path $PSScriptRoot "prepare-prod-envs.ps1") @(@{ Force = $Force }.GetEnumerator() | ForEach-Object {
    if ($_.Value) { "-$($_.Key)" }
})

$backendValues = Read-SynapseEnvFile -FilePath $backendTarget
$existingDbPassword = if ($backendValues.ContainsKey("DB_PASSWORD")) { [string]$backendValues["DB_PASSWORD"] } else { "" }
$dbPasswordToUse =
    if (-not [string]::IsNullOrWhiteSpace($DbPassword)) {
        $DbPassword
    }
    elseif ([string]::IsNullOrWhiteSpace($existingDbPassword) -or $existingDbPassword -match 'change-me|example|set-at-release') {
        Get-RandomPassword
    }
    else {
        $existingDbPassword
    }

$utcNow = [DateTime]::UtcNow
$buildVersion = "free-test-" + $utcNow.ToString("yyyyMMdd-HHmmss")
$buildTime = $utcNow.ToString("yyyy-MM-ddTHH:mm:ssZ")
$buildCommit = "free-test"
try {
    $candidate = (& git -C $rootDir rev-parse --short HEAD 2>$null).Trim()
    if (-not [string]::IsNullOrWhiteSpace($candidate)) {
        $buildCommit = $candidate
    }
}
catch {
}

$appDomain = "app.$PublicIp.$DnsSuffix"
$apiDomain = "api.$PublicIp.$DnsSuffix"

$backendUpdates = [ordered]@{
    SPRING_PROFILES_ACTIVE = "prod"
    DB_HOST = "postgres"
    DB_PORT = "5432"
    DB_NAME = "synapsecore"
    DB_USER = "synapsecore"
    DB_PASSWORD = $dbPasswordToUse
    SPRING_DATA_REDIS_URL = "redis://redis:6379"
    CORS_ALLOWED_ORIGINS = "https://$appDomain"
    SESSION_COOKIE_SECURE = "true"
    SESSION_COOKIE_SAME_SITE = "Lax"
    ALLOW_HEADER_FALLBACK = "false"
    SPRING_JPA_HIBERNATE_DDL_AUTO = "update"
    SYNAPSECORE_BUILD_VERSION = $buildVersion
    SYNAPSECORE_BUILD_COMMIT = $buildCommit
    SYNAPSECORE_BUILD_TIME = $buildTime
}

$frontendUpdates = [ordered]@{
    VITE_API_URL = "https://$apiDomain"
    VITE_WS_URL = "https://$apiDomain/ws"
    VITE_APP_BUILD_VERSION = $buildVersion
    VITE_APP_BUILD_COMMIT = $buildCommit
    VITE_APP_BUILD_TIME = $buildTime
}

$edgeUpdates = [ordered]@{
    SYNAPSECORE_APP_DOMAIN = $appDomain
    SYNAPSECORE_API_DOMAIN = $apiDomain
    SYNAPSECORE_ACME_EMAIL = $AcmeEmail
}

foreach ($entry in $backendUpdates.GetEnumerator()) {
    Set-SynapseEnvValue -FilePath $backendTarget -Key $entry.Key -Value $entry.Value
}

foreach ($entry in $frontendUpdates.GetEnumerator()) {
    Set-SynapseEnvValue -FilePath $frontendTarget -Key $entry.Key -Value $entry.Value
}

foreach ($entry in $edgeUpdates.GetEnumerator()) {
    Set-SynapseEnvValue -FilePath $edgeTarget -Key $entry.Key -Value $entry.Value
}

Write-Host ""
Write-Host "Prepared free-test deployment env files:"
Write-Host "  $backendTarget"
Write-Host "  $frontendTarget"
Write-Host "  $edgeTarget"
Write-Host ""
Write-Host "Generated public test URLs:"
Write-Host "  Frontend: https://$appDomain"
Write-Host "  Backend : https://$apiDomain"
Write-Host ""
Write-Host "Database password in backend.prod.env:"
Write-Host "  $dbPasswordToUse"
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. powershell -ExecutionPolicy Bypass -File scripts\release-readiness.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env"
Write-Host "2. powershell -ExecutionPolicy Bypass -File scripts\start-public-prod.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env -EdgeEnvFile ./env/edge.prod.env"
Write-Host "3. powershell -ExecutionPolicy Bypass -File scripts\verify-deployment.ps1 -FrontendUrl https://$appDomain -BackendUrl https://$apiDomain"
