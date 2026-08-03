param(
    [string]$FrontendUrl = "http://127.0.0.1:5173",
    [string]$BackendUrl = "http://127.0.0.1:8080",
    [string]$DockerInfrastructureDir = "C:\Users\asus\Downloads\synapsecore_starter\synapsecore\infrastructure",
    [int]$TimeoutSeconds = 10
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-LocalHttpCheck {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSec = 10
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing -TimeoutSec $TimeoutSec
        return [pscustomobject]@{
            Label  = $Label
            Url    = $Url
            Ok     = $true
            Status = [int]$response.StatusCode
            Error  = ""
        }
    }
    catch {
        return [pscustomobject]@{
            Label  = $Label
            Url    = $Url
            Ok     = $false
            Status = $null
            Error  = $_.Exception.Message
        }
    }
}

function Test-LocalPort {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [string]$HostName = "127.0.0.1",
        [int]$TimeoutMs = 1500
    )

    $client = $null

    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $connectTask = $client.ConnectAsync($HostName, $Port)
        if (-not $connectTask.Wait($TimeoutMs)) {
            return $false
        }

        return $client.Connected
    }
    catch {
        return $false
    }
    finally {
        if ($null -ne $client) {
            $client.Dispose()
        }
    }
}

$dockerComposeAvailable = Test-Path -LiteralPath (Join-Path $DockerInfrastructureDir "docker-compose.yml")
$dockerPs = $null

try {
    if ($dockerComposeAvailable) {
        $dockerPs = docker compose -f (Join-Path $DockerInfrastructureDir "docker-compose.yml") ps 2>$null
    }
}
catch {
    $dockerPs = $null
}

$frontendCheck = Invoke-LocalHttpCheck -Label "frontend" -Url $FrontendUrl -TimeoutSec $TimeoutSeconds
$backendHealthCheck = Invoke-LocalHttpCheck -Label "backend-health" -Url "$BackendUrl/actuator/health" -TimeoutSec $TimeoutSeconds
$backendReadinessCheck = Invoke-LocalHttpCheck -Label "backend-readiness" -Url "$BackendUrl/actuator/health/readiness" -TimeoutSec $TimeoutSeconds
$authSessionCheck = Invoke-LocalHttpCheck -Label "auth-session" -Url "$BackendUrl/api/auth/session" -TimeoutSec $TimeoutSeconds
$wsInfoCheck = Invoke-LocalHttpCheck -Label "ws-info" -Url "$BackendUrl/ws/info" -TimeoutSec $TimeoutSeconds

$postgresPort = Test-LocalPort -Port 5432
$redisPort = Test-LocalPort -Port 6379
$backendPort = Test-LocalPort -Port 8080
$frontendPort = Test-LocalPort -Port 5173

Write-Host "========================================"
Write-Host "SYNAPSECORE LOCAL CONNECTION CHECK"
Write-Host "========================================"
Write-Host "Frontend URL : $FrontendUrl"
Write-Host "Backend URL  : $BackendUrl"
Write-Host ""

Write-Host "Port posture"
Write-Host "------------"
Write-Host "POSTGRES_5432_LISTENING=$postgresPort"
Write-Host "REDIS_6379_LISTENING=$redisPort"
Write-Host "BACKEND_8080_LISTENING=$backendPort"
Write-Host "FRONTEND_5173_LISTENING=$frontendPort"
Write-Host ""

if ($dockerPs) {
    Write-Host "Docker compose services"
    Write-Host "-----------------------"
    $dockerPs | Write-Host
    Write-Host ""
}

foreach ($check in @($frontendCheck, $backendHealthCheck, $backendReadinessCheck, $authSessionCheck, $wsInfoCheck)) {
    Write-Host "[$($check.Label)] $($check.Url)"
    if ($check.Ok) {
        Write-Host "  OK: $($check.Status)"
    }
    else {
        Write-Host "  FAIL: $($check.Error)"
    }
    Write-Host ""
}

$frontendUp = $frontendCheck.Ok
$backendUp = $backendHealthCheck.Ok -or $authSessionCheck.Ok -or $wsInfoCheck.Ok
$dbReady = $backendReadinessCheck.Ok
$authReady = $authSessionCheck.Ok
$wsReady = $wsInfoCheck.Ok

Write-Host "Classification"
Write-Host "--------------"
Write-Host "FRONTEND_UP=$frontendUp"
Write-Host "BACKEND_UP=$backendUp"
Write-Host "DB_READY=$dbReady"
Write-Host "AUTH_READY=$authReady"
Write-Host "WS_READY=$wsReady"
