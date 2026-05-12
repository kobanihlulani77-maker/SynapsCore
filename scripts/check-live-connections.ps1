param(
    [string]$FrontendUrl = "https://synapscore-frontend-3.onrender.com",
    [string]$BackendUrl = "https://synapscore-3.onrender.com",
    [int]$TimeoutSeconds = 20
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-SynapseHttpCheck {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSec = 20
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing -TimeoutSec $TimeoutSec
        $body = [string]$response.Content
        if ($body.Length -gt 400) {
            $body = $body.Substring(0, 400)
        }

        return [pscustomobject]@{
            Label     = $Label
            Url       = $Url
            Ok        = $true
            Status    = [int]$response.StatusCode
            StatusText = $response.StatusDescription
            Body      = $body
            Error     = ""
        }
    }
    catch {
        $status = $null
        $statusText = ""
        $body = ""

        if ($_.Exception.Response) {
            try {
                $status = [int]$_.Exception.Response.StatusCode
                $statusText = [string]$_.Exception.Response.StatusDescription
            }
            catch {
            }
        }

        return [pscustomobject]@{
            Label      = $Label
            Url        = $Url
            Ok         = $false
            Status     = $status
            StatusText = $statusText
            Body       = $body
            Error      = $_.Exception.Message
        }
    }
}

$checks = @(
    (Invoke-SynapseHttpCheck -Label "frontend" -Url $FrontendUrl -TimeoutSec $TimeoutSeconds),
    (Invoke-SynapseHttpCheck -Label "backend-health" -Url "$BackendUrl/actuator/health" -TimeoutSec $TimeoutSeconds),
    (Invoke-SynapseHttpCheck -Label "backend-readiness" -Url "$BackendUrl/actuator/health/readiness" -TimeoutSec $TimeoutSeconds),
    (Invoke-SynapseHttpCheck -Label "backend-liveness" -Url "$BackendUrl/actuator/health/liveness" -TimeoutSec $TimeoutSeconds),
    (Invoke-SynapseHttpCheck -Label "auth-session" -Url "$BackendUrl/api/auth/session" -TimeoutSec $TimeoutSeconds),
    (Invoke-SynapseHttpCheck -Label "ws-info" -Url "$BackendUrl/ws/info" -TimeoutSec $TimeoutSeconds)
)

Write-Host "========================================"
Write-Host "SYNAPSECORE LIVE CONNECTION CHECK"
Write-Host "========================================"
Write-Host "Frontend URL : $FrontendUrl"
Write-Host "Backend URL  : $BackendUrl"
Write-Host ""

foreach ($check in $checks) {
    Write-Host "[$($check.Label)] $($check.Url)"
    if ($check.Ok) {
        Write-Host "  OK: $($check.Status) $($check.StatusText)"
        if (-not [string]::IsNullOrWhiteSpace($check.Body)) {
            Write-Host "  Body: $($check.Body)"
        }
    }
    else {
        $statusDisplay = if ($null -eq $check.Status) { "no-status" } else { [string]$check.Status }
        Write-Host "  FAIL: $statusDisplay $($check.StatusText)"
        Write-Host "  Error: $($check.Error)"
    }
    Write-Host ""
}

$frontendUp = ($checks | Where-Object Label -eq "frontend").Ok
$backendHealth = ($checks | Where-Object Label -eq "backend-health")
$backendReadiness = ($checks | Where-Object Label -eq "backend-readiness")
$backendLiveness = ($checks | Where-Object Label -eq "backend-liveness")
$authSession = ($checks | Where-Object Label -eq "auth-session")
$wsInfo = ($checks | Where-Object Label -eq "ws-info")

$backendUp = $backendHealth.Ok -or $backendLiveness.Ok -or $authSession.Ok -or $wsInfo.Ok
$dbReady = $backendReadiness.Ok
$authReady = $authSession.Ok
$wsReady = $wsInfo.Ok
$proofAllowed = $frontendUp -and $backendUp -and $backendReadiness.Ok -and $backendLiveness.Ok -and $authReady -and $wsReady

Write-Host "Classification"
Write-Host "--------------"
Write-Host "FRONTEND_UP=$frontendUp"
Write-Host "BACKEND_UP=$backendUp"
Write-Host "DB_READY=$dbReady"
Write-Host "AUTH_READY=$authReady"
Write-Host "WS_READY=$wsReady"
Write-Host "PROOF_ALLOWED=$proofAllowed"

if (-not $backendUp) {
    Write-Host ""
    Write-Host "Interpretation: frontend may be live while the backend is unavailable or startup-blocked."
}
elseif (-not $dbReady) {
    Write-Host ""
    Write-Host "Interpretation: backend is reachable enough to answer some traffic, but readiness is not passing yet."
}
