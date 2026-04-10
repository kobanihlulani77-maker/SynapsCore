param(
    [string]$FrontendUrl = "http://127.0.0.1",
    [string]$BackendUrl = "http://127.0.0.1:8080",
    [string]$SeedTenantCode = "SYNAPSE-DEMO",
    [string]$SeedAdminUsername = "operations.lead",
    [string]$SeedAdminPassword = "lead-2026",
    [int]$MaxAttempts = 20,
    [int]$SleepSeconds = 5
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "ProdEnvTools.ps1")

function Invoke-SessionCheck {
    param(
        [string]$Label,
        [string]$Url,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session
    )

    Write-Host "Checking $Label -> $Url"
    $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -WebSession $Session
    if ([int]$response.StatusCode -ne 200) {
        throw "Failed to verify $Label"
    }
}

Invoke-SynapseEndpointCheck -Label "backend health" -Url "$BackendUrl/actuator/health" -MaxAttempts $MaxAttempts -SleepSeconds $SleepSeconds
Invoke-SynapseEndpointCheck -Label "backend readiness" -Url "$BackendUrl/actuator/health/readiness" -MaxAttempts $MaxAttempts -SleepSeconds $SleepSeconds
Invoke-SynapseEndpointCheck -Label "backend prometheus" -Url "$BackendUrl/actuator/prometheus" -MaxAttempts $MaxAttempts -SleepSeconds $SleepSeconds
Invoke-SynapseEndpointCheck -Label "frontend health" -Url "$FrontendUrl/healthz" -MaxAttempts $MaxAttempts -SleepSeconds $SleepSeconds
Invoke-SynapseEndpointCheck -Label "frontend runtime config" -Url "$FrontendUrl/runtime-config.js" -MaxAttempts $MaxAttempts -SleepSeconds $SleepSeconds

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = @{
    tenantCode = $SeedTenantCode
    username   = $SeedAdminUsername
    password   = $SeedAdminPassword
} | ConvertTo-Json

Write-Host "Checking backend sign-in -> $BackendUrl/api/auth/session/login"
$login = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$BackendUrl/api/auth/session/login" -WebSession $session -ContentType "application/json" -Body $loginBody
if ([int]$login.StatusCode -ne 200) {
    throw "Failed to verify backend sign-in."
}

Invoke-SessionCheck -Label "dashboard summary" -Url "$BackendUrl/api/dashboard/summary" -Session $session
Invoke-SessionCheck -Label "system runtime" -Url "$BackendUrl/api/system/runtime" -Session $session
Invoke-SessionCheck -Label "system incidents" -Url "$BackendUrl/api/system/incidents" -Session $session

Write-Host ""
Write-Host "SynapseCore deployment checks passed."
