param(
    [ValidateRange(1, 10)]
    [int]$Samples = 5
)

$ErrorActionPreference = 'Stop'
$apiBase = 'https://synapscore-3.onrender.com'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$passwordPointer = [IntPtr]::Zero
$signedIn = $false

function Measure-Read {
    param([string]$Path, [string]$Stage)

    $startedAt = (Get-Date).ToUniversalTime()
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest -Uri "$apiBase$Path" -Method Get -WebSession $session `
            -UseBasicParsing -TimeoutSec 30
        $watch.Stop()
        $requestId = [string]$response.Headers['X-Request-Id']
        $count = if ($Path -eq '/api/platform/activity') {
            @($response.Content | ConvertFrom-Json).Count
        } else { $null }
        [pscustomobject]@{
            stage = $Stage
            startUtc = $startedAt.ToString('o')
            endUtc = (Get-Date).ToUniversalTime().ToString('o')
            method = 'GET'
            path = $Path
            status = [int]$response.StatusCode
            durationMs = [math]::Round($watch.Elapsed.TotalMilliseconds, 1)
            requestId = $requestId
            activityCount = $count
        }
    } catch {
        $watch.Stop()
        $failureResponse = $_.Exception.Response
        $status = if ($null -ne $failureResponse) { [int]$failureResponse.StatusCode } else { 'no HTTP response' }
        $requestId = if ($null -ne $failureResponse) {
            [string]$failureResponse.Headers['X-Request-Id']
        } else { '' }
        throw "$Stage $Path failed with $status after $([math]::Round($watch.Elapsed.TotalMilliseconds, 1)) ms; requestId=$requestId. No response body was saved."
    }
}

try {
    $preflight = @(
        Measure-Read '/actuator/health/readiness' 'preflight'
        Measure-Read '/api/platform/session' 'preflight'
        Measure-Read '/actuator/health/readiness' 'warm-check'
        Measure-Read '/api/platform/session' 'warm-check'
    )
    $preflight | ForEach-Object { $_ | ConvertTo-Json -Compress | Write-Output }
    if (@($preflight | Where-Object { $_.stage -eq 'warm-check' -and $_.durationMs -ge 2000 }).Count -ne 0) {
        throw 'Preflight is not warm; do not classify subsequent reads as a warm index comparison.'
    }

    $username = Read-Host 'Platform Owner username'
    $securePassword = Read-Host 'Platform Owner password' -AsSecureString
    if ([string]::IsNullOrWhiteSpace($username)) { throw 'Username is required.' }
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $loginBody = @{ username = $username.Trim(); password = $password } | ConvertTo-Json -Compress
    $password = $null
    $loginStartedAt = (Get-Date).ToUniversalTime()
    $loginWatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $loginResponse = Invoke-WebRequest -Uri "$apiBase/api/platform/session/login" -Method Post `
            -ContentType 'application/json' -Body $loginBody -WebSession $session `
            -UseBasicParsing -TimeoutSec 30
    } catch {
        $failureResponse = $_.Exception.Response
        $status = if ($null -ne $failureResponse) { [int]$failureResponse.StatusCode } else { 'no HTTP response' }
        $retryAfter = if ($null -ne $failureResponse) {
            $failureResponse.Headers['X-Synapse-RateLimit-Reset-After-Seconds']
        } else { $null }
        $retryNote = if ($retryAfter) { " Retry after at least $retryAfter seconds." } else { '' }
        throw "Platform Owner login failed with $status.$retryNote No credentials or response body were saved."
    }
    $loginWatch.Stop()
    $loginBody = $null
    if ($loginResponse.StatusCode -ne 200 -or -not ($loginResponse.Content | ConvertFrom-Json).signedIn) {
        throw 'Platform Owner login did not establish an authenticated session.'
    }
    $signedIn = $true
    [pscustomobject]@{
        stage = 'login'
        startUtc = $loginStartedAt.ToString('o')
        endUtc = (Get-Date).ToUniversalTime().ToString('o')
        method = 'POST'
        path = '/api/platform/session/login'
        status = [int]$loginResponse.StatusCode
        durationMs = [math]::Round($loginWatch.Elapsed.TotalMilliseconds, 1)
        requestId = [string]$loginResponse.Headers['X-Request-Id']
    } | ConvertTo-Json -Compress | Write-Output
    if ($loginWatch.Elapsed.TotalMilliseconds -ge 2000) {
        throw 'Authenticated login was slow; do not classify subsequent reads as a warm index comparison.'
    }

    $warm = @(
        Measure-Read '/api/platform/overview' 'warmup'
        Measure-Read '/api/platform/activity' 'warmup'
    )
    if (@($warm | Where-Object { $_.status -ne 200 }).Count -ne 0) {
        throw 'Warm baseline did not return HTTP 200 for both reads.'
    }
    Write-Output "Live endpoint: $apiBase"
    Write-Output "Warm baseline UTC: $((Get-Date).ToUniversalTime().ToString('o'))"
    $warm | ForEach-Object { $_ | ConvertTo-Json -Compress | Write-Output }

    for ($sample = 1; $sample -le $Samples; $sample++) {
        Measure-Read '/api/platform/overview' "sample-$sample" |
            ForEach-Object { $_ | ConvertTo-Json -Compress | Write-Output }
        Measure-Read '/api/platform/activity' "sample-$sample" |
            ForEach-Object { $_ | ConvertTo-Json -Compress | Write-Output }
    }
} finally {
    if ($signedIn) {
        try {
            Invoke-WebRequest -Uri "$apiBase/api/platform/session/logout" -Method Post `
                -WebSession $session -UseBasicParsing -TimeoutSec 10 | Out-Null
        } catch {
            Write-Warning 'The probe could not confirm sign-out; the server session will expire normally.'
        }
    }
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    Remove-Variable password, loginBody, securePassword -ErrorAction SilentlyContinue
}
