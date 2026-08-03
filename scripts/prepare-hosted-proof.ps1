param(
    [string]$ApiBaseUrl,
    [string]$FrontendBaseUrl,
    [string]$TenantCode,
    [string]$TenantName,
    [string]$TenantAdminUsername,
    [string]$TenantAdminPassword,
    [string]$PlannerUsername,
    [string]$PlannerPassword,
    [string]$IntegrationAdminUsername,
    [string]$IntegrationAdminPassword,
    [string]$ProofProductSku,
    [string]$PlatformAdminToken,
    [string]$BootstrapInitialToken
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-FirstValue {
    param([string[]]$Values)

    foreach ($value in $Values) {
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            return $value.Trim()
        }
    }
    return $null
}

function Read-HostedProofState {
    if ([string]::IsNullOrWhiteSpace($script:HostedProofStatePath) -or -not (Test-Path -LiteralPath $script:HostedProofStatePath)) {
        return [pscustomobject]@{}
    }

    try {
        return Get-Content -LiteralPath $script:HostedProofStatePath -Raw | ConvertFrom-Json -ErrorAction Stop
    } catch {
        return [pscustomobject]@{}
    }
}

function Get-HostedProofStateValue {
    param([string[]]$Names)

    foreach ($name in $Names) {
        if ($null -eq $script:ExistingHostedProofState) {
            continue
        }
        $property = $script:ExistingHostedProofState.PSObject.Properties[$name]
        if ($null -ne $property -and -not [string]::IsNullOrWhiteSpace([string]$property.Value)) {
            return ([string]$property.Value).Trim()
        }
    }

    return $null
}

function Write-HostedProofState {
    param([hashtable]$Values)

    $state = [ordered]@{}
    if ($null -ne $script:ExistingHostedProofState) {
        foreach ($property in $script:ExistingHostedProofState.PSObject.Properties) {
            $state[$property.Name] = $property.Value
        }
    }

    foreach ($key in $Values.Keys) {
        $state[$key] = $Values[$key]
    }

    $state["preparedAt"] = (Get-Date).ToUniversalTime().ToString("o")

    $stateDirectory = Split-Path -Parent $script:HostedProofStatePath
    New-Item -ItemType Directory -Path $stateDirectory -Force | Out-Null
    $json = $state | ConvertTo-Json -Depth 8
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($script:HostedProofStatePath, $json, $utf8NoBom)
    $script:ExistingHostedProofState = Read-HostedProofState
}

function New-ProofPassword {
    param([string]$Purpose)

    $safePurpose = ($Purpose.ToLowerInvariant() -replace '[^a-z0-9]+', '-').Trim('-')
    if ([string]::IsNullOrWhiteSpace($safePurpose)) {
        $safePurpose = "proof"
    }
    return "Proof-$safePurpose-$([Guid]::NewGuid().ToString("N").Substring(0, 24))-A1"
}

function Require-Value {
    param(
        [string]$Name,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "$Name is required for hosted proof setup."
    }
    return $Value.Trim()
}

function Require-Password {
    param(
        [string]$Name,
        [string]$Value
    )

    $password = Require-Value -Name $Name -Value $Value
    if ($password.Length -lt 8) {
        throw "$Name must be at least 8 characters because the production API enforces that minimum."
    }
    return $password
}

function Require-TenantCode {
    param(
        [string]$Name,
        [string]$Value
    )

    $tenantCode = Require-Value -Name $Name -Value $Value
    if ($tenantCode -notmatch '^[A-Za-z0-9-]+$') {
        throw "$Name must contain only letters, digits, and hyphens."
    }
    return $tenantCode.ToUpperInvariant()
}

function Require-Username {
    param(
        [string]$Name,
        [string]$Value
    )

    $username = Require-Value -Name $Name -Value $Value
    if ($username -notmatch '^[A-Za-z0-9._-]+$') {
        throw "$Name must contain only letters, digits, dots, underscores, and hyphens. Email-style usernames with @ are not valid for SynapsCore access users."
    }
    return $username.ToLowerInvariant()
}

function Normalize-ProofSku {
    param(
        [string]$Name,
        [string]$Value
    )

    $sku = Require-Value -Name $Name -Value $Value
    $normalized = $sku.ToUpperInvariant()
    if ($normalized -notmatch '^[A-Z0-9][A-Z0-9._-]{0,63}$') {
        throw "$Name must start with a letter or number and may only contain letters, numbers, dots, underscores, and hyphens."
    }
    return $normalized
}

function Get-DefaultProofProductSku {
    param([string]$TenantCode)

    $normalizedTenant = ($TenantCode.ToUpperInvariant() -replace '[^A-Z0-9._-]', '-')
    $candidate = "SKU-$normalizedTenant-PROOF"
    if ($candidate.Length -le 64) {
        return $candidate
    }
    return ("SKU-" + $normalizedTenant.Substring(0, [Math]::Min($normalizedTenant.Length, 50)) + "-PRF")
}

function Get-ErrorBody {
    param([object]$ErrorRecord)

    if ($null -ne $ErrorRecord.ErrorDetails -and -not [string]::IsNullOrWhiteSpace($ErrorRecord.ErrorDetails.Message)) {
        return $ErrorRecord.ErrorDetails.Message
    }

    try {
        if ($null -ne $ErrorRecord.Exception.Response -and $ErrorRecord.Exception.Response.GetResponseStream) {
            $stream = $ErrorRecord.Exception.Response.GetResponseStream()
            if ($null -ne $stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $body = $reader.ReadToEnd()
                $reader.Dispose()
                if (-not [string]::IsNullOrWhiteSpace($body)) {
                    return $body
                }
            }
        }
    } catch {
        return $ErrorRecord.Exception.Message
    }

    return $ErrorRecord.Exception.Message
}

function Get-ErrorStatusCode {
    param([object]$ErrorRecord)

    try {
        if ($null -ne $ErrorRecord.Exception.Response -and $null -ne $ErrorRecord.Exception.Response.StatusCode) {
            return [int]$ErrorRecord.Exception.Response.StatusCode
        }
    } catch {
        return $null
    }

    return $null
}

function Get-ErrorHeaderValue {
    param(
        [object]$ErrorRecord,
        [string]$HeaderName
    )

    try {
        if ($null -ne $ErrorRecord.Exception.Response -and $null -ne $ErrorRecord.Exception.Response.Headers) {
            return $ErrorRecord.Exception.Response.Headers[$HeaderName]
        }
    } catch {
        return $null
    }

    return $null
}

function Convert-ResponseContentToText {
    param([object]$Content)

    if ($null -eq $Content) {
        return ""
    }

    if ($Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Content)
    }

    return [string]$Content
}

function Invoke-HostedProbe {
    param(
        [string]$Url,
        [hashtable]$Headers = @{},
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session = $null,
        [int]$TimeoutSec = 30
    )

    $invokeArgs = @{
        Uri = $Url
        UseBasicParsing = $true
        Headers = $Headers
        TimeoutSec = $TimeoutSec
    }

    if ($null -ne $Session) {
        $invokeArgs.WebSession = $Session
    }

    try {
        $response = Invoke-WebRequest @invokeArgs
        $contentText = Convert-ResponseContentToText -Content $response.Content
        $json = $null
        if (-not [string]::IsNullOrWhiteSpace($contentText)) {
            try {
                $json = $contentText | ConvertFrom-Json -ErrorAction Stop
            } catch {
                $json = $null
            }
        }

        return [pscustomobject]@{
            StatusCode = [int]$response.StatusCode
            Content    = $contentText
            Json       = $json
        }
    } catch {
        $body = Get-ErrorBody -ErrorRecord $_
        $statusCode = Get-ErrorStatusCode -ErrorRecord $_
        $json = $null
        if (-not [string]::IsNullOrWhiteSpace($body)) {
            try {
                $json = $body | ConvertFrom-Json -ErrorAction Stop
            } catch {
                $json = $null
            }
        }

        return [pscustomobject]@{
            StatusCode = $statusCode
            Content    = (Convert-ResponseContentToText -Content $body)
            Json       = $json
        }
    }
}

function Wait-HostedProbe {
    param(
        [string]$Description,
        [scriptblock]$Probe,
        [scriptblock]$IsReady,
        [int]$TimeoutSeconds = 240,
        [int]$DelaySeconds = 5
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastDetail = "No response yet."

    while ((Get-Date) -lt $deadline) {
        $result = & $Probe
        if ($null -ne $result) {
            $statusSuffix = if ($null -ne $result.StatusCode) { "HTTP $($result.StatusCode)" } else { "HTTP unavailable" }
            $contentSuffix = if (-not [string]::IsNullOrWhiteSpace($result.Content)) { $result.Content } else { "No body" }
            $lastDetail = "$statusSuffix $contentSuffix"
        }

        if (& $IsReady $result) {
            Write-Host "$Description ready."
            return $result
        }

        Start-Sleep -Seconds $DelaySeconds
    }

    throw "$Description did not become ready within $TimeoutSeconds seconds. Last detail: $lastDetail"
}

function Invoke-SynapseJson {
    param(
        [ValidateSet("GET", "POST", "PUT", "DELETE")]
        [string]$Method,
        [string]$Url,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session = $null,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $resolvedHeaders = @{}
    foreach ($headerKey in $Headers.Keys) {
        $resolvedHeaders[$headerKey] = $Headers[$headerKey]
    }
    if (-not [string]::IsNullOrWhiteSpace($script:TenantCodeValue) -and -not $resolvedHeaders.ContainsKey("X-Synapse-Tenant")) {
        $resolvedHeaders["X-Synapse-Tenant"] = $script:TenantCodeValue
    }

    $invokeArgs = @{
        Method          = $Method
        Uri             = $Url
        UseBasicParsing = $true
        Headers         = $resolvedHeaders
    }

    if ($null -ne $Session) {
        $invokeArgs.WebSession = $Session
    }

    if ($null -ne $Body) {
        $invokeArgs.Body = $Body | ConvertTo-Json -Depth 16
        $invokeArgs.ContentType = "application/json"
    }

    try {
        return Invoke-RestMethod @invokeArgs
    } catch {
        $body = Get-ErrorBody -ErrorRecord $_
        $statusCode = Get-ErrorStatusCode -ErrorRecord $_
        $requestId = Get-ErrorHeaderValue -ErrorRecord $_ -HeaderName "x-request-id"
        $renderRequestId = Get-ErrorHeaderValue -ErrorRecord $_ -HeaderName "rndr-id"
        $requestSuffix = ""
        if (-not [string]::IsNullOrWhiteSpace($requestId)) {
            $requestSuffix += " requestId=$requestId"
        }
        if (-not [string]::IsNullOrWhiteSpace($renderRequestId)) {
            $requestSuffix += " renderRequestId=$renderRequestId"
        }
        if ($null -ne $statusCode) {
            throw "$Method $Url failed with HTTP $statusCode.$requestSuffix $body"
        }
        throw "$Method $Url failed.$requestSuffix $body"
    }
}

function Get-JsonArray {
    param(
        [string]$Url,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session = $null
    )

    $raw = Invoke-SynapseJson -Method GET -Url $Url -Session $Session
    if ($null -eq $raw) {
        return @()
    }
    return @($raw)
}

function Get-PropertyValue {
    param(
        [object]$Object,
        [string]$PropertyName
    )

    if ($null -eq $Object) {
        return $null
    }
    $property = $Object.PSObject.Properties[$PropertyName]
    if ($null -eq $property) {
        return $null
    }
    return $property.Value
}

function New-TemporaryPassword {
    return "tmp-" + [Guid]::NewGuid().ToString("N").Substring(0, 20) + "A1!"
}

function New-AuthenticatedSession {
    param(
        [string]$Username,
        [string]$Password
    )

    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $response = Invoke-SynapseJson `
        -Method POST `
        -Url "$script:ApiBaseUrlValue/api/auth/session/login" `
        -Session $session `
        -Body @{
            tenantCode = $script:TenantCodeValue
            username = $Username
            password = $Password
        }

    return [pscustomobject]@{
        Session = $session
        Response = $response
    }
}

function Wait-AuthenticatedProofWarmup {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session
    )

    Write-Host "Verifying authenticated dashboard and runtime warm-up..."

    Wait-HostedProbe `
        -Description "Authenticated session" `
        -Probe {
            Invoke-HostedProbe `
                -Url "$script:ApiBaseUrlValue/api/auth/session" `
                -Session $Session `
                -TimeoutSec 20
        } `
        -IsReady {
            param($result)
            $null -ne $result -and
                $result.StatusCode -eq 200 -and
                (Get-PropertyValue -Object $result.Json -PropertyName "signedIn") -eq $true
        } `
        -TimeoutSeconds 120 | Out-Null

    Wait-HostedProbe `
        -Description "Authenticated dashboard summary" `
        -Probe {
            Invoke-HostedProbe `
                -Url "$script:ApiBaseUrlValue/api/dashboard/summary" `
                -Session $Session `
                -TimeoutSec 30
        } `
        -IsReady {
            param($result)
            $null -ne $result -and
                $result.StatusCode -eq 200 -and
                $null -ne (Get-PropertyValue -Object $result.Json -PropertyName "totalOrders")
        } `
        -TimeoutSeconds 150 | Out-Null

    Wait-HostedProbe `
        -Description "Authenticated runtime" `
        -Probe {
            Invoke-HostedProbe `
                -Url "$script:ApiBaseUrlValue/api/system/runtime" `
                -Session $Session `
                -TimeoutSec 30
        } `
        -IsReady {
            param($result)
            $null -ne $result -and
                $result.StatusCode -eq 200 -and
                -not [string]::IsNullOrWhiteSpace([string](Get-PropertyValue -Object $result.Json -PropertyName "readinessState"))
        } `
        -TimeoutSeconds 150 | Out-Null

    Wait-HostedProbe `
        -Description "Authenticated dashboard snapshot" `
        -Probe {
            Invoke-HostedProbe `
                -Url "$script:ApiBaseUrlValue/api/dashboard/snapshot" `
                -Session $Session `
                -TimeoutSec 60
        } `
        -IsReady {
            param($result)
            $inventory = Get-PropertyValue -Object $result.Json -PropertyName "inventory"
            $null -ne $result -and
                $result.StatusCode -eq 200 -and
                $null -ne $inventory
        } `
        -TimeoutSeconds 180 | Out-Null
}

function Invoke-PasswordChange {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [string]$CurrentPassword,
        [string]$NewPassword
    )

    Invoke-SynapseJson `
        -Method POST `
        -Url "$script:ApiBaseUrlValue/api/auth/session/password" `
        -Session $Session `
        -Body @{
            currentPassword = $CurrentPassword
            newPassword = $NewPassword
        } | Out-Null
}

function Ensure-Operator {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$AdminSession,
        [string]$ActorName,
        [string]$DisplayName,
        [string]$Description,
        [string[]]$Roles
    )

    $body = @{
        actorName = $ActorName
        displayName = $DisplayName
        description = $Description
        active = $true
        roles = @($Roles)
        warehouseScopes = @()
    }

    $operators = @(Get-JsonArray -Url "$script:ApiBaseUrlValue/api/access/admin/operators" -Session $AdminSession)
    $existing = $operators | Where-Object { $null -ne $_ -and (Get-PropertyValue -Object $_ -PropertyName "actorName") -ieq $ActorName } | Select-Object -First 1

    if ($null -eq $existing) {
        return Invoke-SynapseJson `
            -Method POST `
            -Url "$script:ApiBaseUrlValue/api/access/admin/operators" `
            -Session $AdminSession `
            -Body $body
    }

    $operatorId = Get-PropertyValue -Object $existing -PropertyName "id"
    return Invoke-SynapseJson `
        -Method PUT `
        -Url "$script:ApiBaseUrlValue/api/access/admin/operators/$operatorId" `
        -Session $AdminSession `
        -Body $body
}

function Ensure-User {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$AdminSession,
        [string]$Username,
        [string]$FullName,
        [string]$OperatorActorName,
        [string]$FinalPassword
    )

    $temporaryPassword = New-TemporaryPassword
    $users = @(Get-JsonArray -Url "$script:ApiBaseUrlValue/api/access/admin/users" -Session $AdminSession)
    $existing = $users | Where-Object { $null -ne $_ -and (Get-PropertyValue -Object $_ -PropertyName "username") -ieq $Username } | Select-Object -First 1

    if ($null -eq $existing) {
        Invoke-SynapseJson `
            -Method POST `
            -Url "$script:ApiBaseUrlValue/api/access/admin/users" `
            -Session $AdminSession `
            -Body @{
                username = $Username
                fullName = $FullName
                password = $temporaryPassword
                operatorActorName = $OperatorActorName
            } | Out-Null
    } else {
        $userId = Get-PropertyValue -Object $existing -PropertyName "id"
        Invoke-SynapseJson `
            -Method PUT `
            -Url "$script:ApiBaseUrlValue/api/access/admin/users/$userId" `
            -Session $AdminSession `
            -Body @{
                fullName = $FullName
                active = $true
                operatorActorName = $OperatorActorName
            } | Out-Null

        Invoke-SynapseJson `
            -Method POST `
            -Url "$script:ApiBaseUrlValue/api/access/admin/users/$userId/reset-password" `
            -Session $AdminSession `
            -Body @{ password = $temporaryPassword } | Out-Null
    }

    $userLogin = New-AuthenticatedSession -Username $Username -Password $temporaryPassword
    Invoke-PasswordChange -Session $userLogin.Session -CurrentPassword $temporaryPassword -NewPassword $FinalPassword
}

function Find-ProofProduct {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$AdminSession,
        [string]$Sku,
        [string]$Name,
        [string]$Category
    )

    $normalizedSku = $Sku.ToUpperInvariant()
    $internalSku = "{0}::{1}" -f $script:TenantCodeValue, $normalizedSku
    $products = @(Get-JsonArray -Url "$script:ApiBaseUrlValue/api/products" -Session $AdminSession)
    $product = $products | Where-Object {
        $responseSku = Get-PropertyValue -Object $_ -PropertyName "sku"
        $responseCatalogSku = Get-PropertyValue -Object $_ -PropertyName "catalogSku"
        $responseInternalSku = Get-PropertyValue -Object $_ -PropertyName "internalSku"
        $null -ne $_ -and (
            $responseSku -ieq $normalizedSku -or
            $responseSku -ieq $internalSku -or
            $responseCatalogSku -ieq $normalizedSku -or
            $responseCatalogSku -ieq $internalSku -or
            $responseInternalSku -ieq $internalSku
        )
    } | Select-Object -First 1

    if ($null -ne $product) {
        return $product
    }

    return $products | Where-Object {
        $null -ne $_ -and
        (Get-PropertyValue -Object $_ -PropertyName "name") -ieq $Name -and
        (Get-PropertyValue -Object $_ -PropertyName "category") -ieq $Category
    } | Select-Object -First 1
}

function Test-IsProductConflict {
    param([string]$Message)

    return $Message -match "Product SKU already exists|Product internal SKU already exists|hidden legacy catalog row still occupies|Multiple orphan catalog rows exist|Product catalog write conflicted with an existing tenant-visible or legacy hidden SKU"
}

function Upsert-ProofProduct {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$AdminSession,
        [string]$Sku,
        [string]$Name,
        [string]$Category
    )

    $productBody = @{
        sku = $Sku
        name = $Name
        category = $Category
    }

    $existingProduct = Find-ProofProduct -AdminSession $AdminSession -Sku $Sku -Name $Name -Category $Category
    if ($null -ne $existingProduct) {
        $productId = Get-PropertyValue -Object $existingProduct -PropertyName "id"
        return Invoke-SynapseJson `
            -Method PUT `
            -Url "$script:ApiBaseUrlValue/api/products/$productId" `
            -Session $AdminSession `
            -Body $productBody
    }

    try {
        return Invoke-SynapseJson `
            -Method POST `
            -Url "$script:ApiBaseUrlValue/api/products" `
            -Session $AdminSession `
            -Body $productBody
    } catch {
        $message = $_.Exception.Message
        if (-not (Test-IsProductConflict -Message $message)) {
            throw
        }

        Write-Host "Product $Sku already exists; refetching and reusing existing tenant product."
        $conflictingProduct = Find-ProofProduct -AdminSession $AdminSession -Sku $Sku -Name $Name -Category $Category
        if ($null -eq $conflictingProduct) {
            throw "Product create for $Sku conflicted, but /api/products did not return a matching tenant product by SKU or proof name. Original error: $message"
        }

        $productId = Get-PropertyValue -Object $conflictingProduct -PropertyName "id"
        return Invoke-SynapseJson `
            -Method PUT `
            -Url "$script:ApiBaseUrlValue/api/products/$productId" `
            -Session $AdminSession `
            -Body $productBody
    }
}

function Ensure-ProofCatalogAndInventory {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$AdminSession,
        [string]$Sku
    )

    Upsert-ProofProduct `
        -AdminSession $AdminSession `
        -Sku $sku `
        -Name "Pulse Relay Verification Product" `
        -Category "Verification" | Out-Null

    Invoke-SynapseJson `
        -Method POST `
        -Url "$script:ApiBaseUrlValue/api/inventory/update" `
        -Session $AdminSession `
        -Body @{
            productSku = $sku
            warehouseCode = "WH-NORTH"
            quantityAvailable = 24
            reorderThreshold = 12
        } | Out-Null
}

$script:HostedProofStatePath = Join-Path (Join-Path (Split-Path -Parent $PSScriptRoot) "frontend") ".hosted-proof\hosted-proof-state.json"
$script:ExistingHostedProofState = Read-HostedProofState

$script:ApiBaseUrlValue = (Require-Value `
    -Name "PLAYWRIGHT_API_BASE_URL" `
    -Value (Get-FirstValue -Values @($ApiBaseUrl, $env:PLAYWRIGHT_API_BASE_URL, $env:PLAYWRIGHT_BACKEND_URL, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_API_BASE_URL", "PLAYWRIGHT_BACKEND_URL")), "https://synapscore-3.onrender.com"))).TrimEnd("/")
$FrontendBaseUrlValue = Get-FirstValue -Values @($FrontendBaseUrl, $env:PLAYWRIGHT_BASE_URL, $env:PLAYWRIGHT_FRONTEND_URL, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_BASE_URL", "PLAYWRIGHT_FRONTEND_URL")), "https://synapscore-frontend-3.onrender.com")
if (-not [string]::IsNullOrWhiteSpace($FrontendBaseUrlValue)) {
    $FrontendBaseUrlValue = $FrontendBaseUrlValue.TrimEnd("/")
}
$script:TenantCodeValue = Require-TenantCode -Name "PLAYWRIGHT_TENANT_CODE" -Value (Get-FirstValue -Values @($TenantCode, $env:PLAYWRIGHT_TENANT_CODE, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_TENANT_CODE")), "HOSTED-PROOF"))
$TenantNameValue = Get-FirstValue -Values @($TenantName, $env:PLAYWRIGHT_TENANT_NAME, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_TENANT_NAME")), "$script:TenantCodeValue Hosted Verification")
$TenantAdminUsernameValue = Require-Username -Name "PLAYWRIGHT_TENANT_ADMIN_USERNAME" -Value (Get-FirstValue -Values @($TenantAdminUsername, $env:PLAYWRIGHT_TENANT_ADMIN_USERNAME, $env:PLAYWRIGHT_OPERATIONS_LEAD_USERNAME, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_TENANT_ADMIN_USERNAME", "PLAYWRIGHT_OPERATIONS_LEAD_USERNAME")), "hosted.proof.admin"))
$TenantAdminPasswordValue = Require-Password -Name "PLAYWRIGHT_TENANT_ADMIN_PASSWORD" -Value (Get-FirstValue -Values @($TenantAdminPassword, $env:PLAYWRIGHT_TENANT_ADMIN_PASSWORD, $env:PLAYWRIGHT_OPERATIONS_LEAD_PASSWORD, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_TENANT_ADMIN_PASSWORD", "PLAYWRIGHT_OPERATIONS_LEAD_PASSWORD")), (New-ProofPassword -Purpose "admin")))
$PlannerUsernameValue = Require-Username -Name "PLAYWRIGHT_PLANNER_USERNAME" -Value (Get-FirstValue -Values @($PlannerUsername, $env:PLAYWRIGHT_PLANNER_USERNAME, $env:PLAYWRIGHT_OPERATIONS_PLANNER_USERNAME, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_PLANNER_USERNAME", "PLAYWRIGHT_OPERATIONS_PLANNER_USERNAME")), "hosted.proof.planner"))
$PlannerPasswordValue = Require-Password -Name "PLAYWRIGHT_PLANNER_PASSWORD" -Value (Get-FirstValue -Values @($PlannerPassword, $env:PLAYWRIGHT_PLANNER_PASSWORD, $env:PLAYWRIGHT_OPERATIONS_PLANNER_PASSWORD, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_PLANNER_PASSWORD", "PLAYWRIGHT_OPERATIONS_PLANNER_PASSWORD")), (New-ProofPassword -Purpose "planner")))
$IntegrationAdminUsernameValue = Require-Username -Name "PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME" -Value (Get-FirstValue -Values @($IntegrationAdminUsername, $env:PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME, $env:PLAYWRIGHT_INTEGRATION_LEAD_USERNAME, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME", "PLAYWRIGHT_INTEGRATION_LEAD_USERNAME")), "hosted.proof.integration"))
$IntegrationAdminPasswordValue = Require-Password -Name "PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD" -Value (Get-FirstValue -Values @($IntegrationAdminPassword, $env:PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD, $env:PLAYWRIGHT_INTEGRATION_LEAD_PASSWORD, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD", "PLAYWRIGHT_INTEGRATION_LEAD_PASSWORD")), (New-ProofPassword -Purpose "integration")))
$ProofProductSkuValue = Normalize-ProofSku -Name "PLAYWRIGHT_PROOF_PRODUCT_SKU" -Value (Get-FirstValue -Values @($ProofProductSku, $env:PLAYWRIGHT_PROOF_PRODUCT_SKU, (Get-HostedProofStateValue -Names @("PLAYWRIGHT_PROOF_PRODUCT_SKU")), (Get-DefaultProofProductSku -TenantCode $script:TenantCodeValue)))
$PlatformAdminTokenValue = Get-FirstValue -Values @($PlatformAdminToken, $env:SYNAPSECORE_PLATFORM_ADMIN_TOKEN)
$BootstrapInitialTokenValue = Get-FirstValue -Values @($BootstrapInitialToken, $env:SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN)
if ($BootstrapInitialTokenValue -match '^<.*>$') {
    throw "SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN is still a placeholder. Copy the real private Render backend value into this shell before running hosted proof preparation."
}

if ($script:TenantCodeValue -ieq "SYNAPSE-DEMO") {
    throw "SYNAPSE-DEMO is blocked for hosted proof. Use a real verification tenant created through /api/access/tenants."
}

$distinctUsernames = @($TenantAdminUsernameValue, $PlannerUsernameValue, $IntegrationAdminUsernameValue) | Select-Object -Unique
if ($distinctUsernames.Count -ne 3) {
    throw "Hosted proof requires three distinct sign-in accounts: tenant admin, planner/operator, and integration admin."
}

Write-HostedProofState -Values @{
    PLAYWRIGHT_BASE_URL = $FrontendBaseUrlValue
    PLAYWRIGHT_API_BASE_URL = $script:ApiBaseUrlValue
    PLAYWRIGHT_TENANT_CODE = $script:TenantCodeValue
    PLAYWRIGHT_TENANT_NAME = $TenantNameValue
    PLAYWRIGHT_TENANT_ADMIN_USERNAME = $TenantAdminUsernameValue
    PLAYWRIGHT_TENANT_ADMIN_PASSWORD = $TenantAdminPasswordValue
    PLAYWRIGHT_PLANNER_USERNAME = $PlannerUsernameValue
    PLAYWRIGHT_PLANNER_PASSWORD = $PlannerPasswordValue
    PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME = $IntegrationAdminUsernameValue
    PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD = $IntegrationAdminPasswordValue
    PLAYWRIGHT_PROOF_PRODUCT_SKU = $ProofProductSkuValue
}

Write-Host "========================================"
Write-Host "SYNAPSECORE HOSTED PROOF PREP"
Write-Host "========================================"
Write-Host "Backend API : $script:ApiBaseUrlValue"
if (-not [string]::IsNullOrWhiteSpace($FrontendBaseUrlValue)) {
    Write-Host "Frontend    : $FrontendBaseUrlValue"
}
Write-Host "Tenant      : $script:TenantCodeValue"
Write-Host "Mode        : real tenant/admin APIs, no seed or DB edits"
Write-Host "Proof state : $script:HostedProofStatePath"
Write-Host ""

$readinessUrl = "$script:ApiBaseUrlValue/actuator/health/readiness"
$authSessionUrl = "$script:ApiBaseUrlValue/api/auth/session"
$realtimeInfoUrl = "$script:ApiBaseUrlValue/ws/info?t=$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"

Write-Host "Waiting for backend readiness and warm-up..."
Wait-HostedProbe `
    -Description "Backend readiness" `
    -Probe { Invoke-HostedProbe -Url $readinessUrl } `
    -IsReady {
        param($result)
        $null -ne $result -and
            $result.StatusCode -eq 200 -and
            (Get-PropertyValue -Object $result.Json -PropertyName "status") -eq "UP"
    } | Out-Null

Wait-HostedProbe `
    -Description "Auth session endpoint" `
    -Probe { Invoke-HostedProbe -Url $authSessionUrl } `
    -IsReady {
        param($result)
        $null -ne $result -and
            $result.StatusCode -eq 200 -and
            $null -ne (Get-PropertyValue -Object $result.Json -PropertyName "signedIn")
    } | Out-Null

Wait-HostedProbe `
    -Description "Realtime SockJS endpoint" `
    -Probe { Invoke-HostedProbe -Url $realtimeInfoUrl } `
    -IsReady {
        param($result)
        $null -ne $result -and
            $result.StatusCode -eq 200 -and
            $null -ne (Get-PropertyValue -Object $result.Json -PropertyName "websocket")
    } | Out-Null

if (-not [string]::IsNullOrWhiteSpace($FrontendBaseUrlValue)) {
    Wait-HostedProbe `
        -Description "Frontend sign-in shell" `
        -Probe { Invoke-HostedProbe -Url "$FrontendBaseUrlValue/sign-in" } `
        -IsReady {
            param($result)
            $null -ne $result -and
                $result.StatusCode -eq 200 -and
                -not [string]::IsNullOrWhiteSpace($result.Content) -and
                $result.Content -match "SynapseCore"
        } | Out-Null
}

$tenants = @(Get-JsonArray -Url "$script:ApiBaseUrlValue/api/access/tenants")
$tenant = $tenants | Where-Object { $null -ne $_ -and (Get-PropertyValue -Object $_ -PropertyName "code") -ieq $script:TenantCodeValue } | Select-Object -First 1

if ($null -eq $tenant) {
    $tenantHeaders = @{}
    if ($tenants.Count -eq 0) {
        if ([string]::IsNullOrWhiteSpace($BootstrapInitialTokenValue)) {
            throw "No tenants exist yet. Proof tenant/operator values have been generated and written to $script:HostedProofStatePath. Set SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN to the private Render backend bootstrap secret, then rerun this script so the first tenant can be created safely through /api/access/tenants."
        }
        $tenantHeaders["X-Synapse-Bootstrap-Token"] = $BootstrapInitialTokenValue
    } else {
        if ([string]::IsNullOrWhiteSpace($PlatformAdminTokenValue)) {
            throw "Tenant $script:TenantCodeValue does not exist. Set SYNAPSECORE_PLATFORM_ADMIN_TOKEN to create hosted verification tenants after initial bootstrap."
        }
        $tenantHeaders["X-Synapse-Platform-Admin-Token"] = $PlatformAdminTokenValue
    }

    Write-Host "Creating tenant workspace $script:TenantCodeValue through /api/access/tenants..."
    Invoke-SynapseJson `
        -Method POST `
        -Url "$script:ApiBaseUrlValue/api/access/tenants" `
        -Headers $tenantHeaders `
        -Body @{
            tenantCode = $script:TenantCodeValue
            tenantName = $TenantNameValue
            description = "Hosted technical verification workspace."
            adminFullName = "Hosted Verification Tenant Admin"
            adminUsername = $TenantAdminUsernameValue
            adminPassword = $TenantAdminPasswordValue
            primaryLocation = "Verification North Hub"
            secondaryLocation = "Verification Coast Hub"
        } | Out-Null
} else {
    Write-Host "Tenant workspace $script:TenantCodeValue already exists; reusing it."
}

try {
    $adminLogin = New-AuthenticatedSession -Username $TenantAdminUsernameValue -Password $TenantAdminPasswordValue
} catch {
    throw "Tenant admin sign-in failed for $TenantAdminUsernameValue in $script:TenantCodeValue. If this tenant already existed, reset it with another tenant admin or use a fresh verification tenant code. Platform tokens intentionally cannot mutate tenant users through this API. $($_.Exception.Message)"
}

$adminSession = $adminLogin.Session
if ([bool](Get-PropertyValue -Object $adminLogin.Response -PropertyName "passwordChangeRequired")) {
    $temporaryAdminPassword = New-TemporaryPassword
    Invoke-PasswordChange -Session $adminSession -CurrentPassword $TenantAdminPasswordValue -NewPassword $temporaryAdminPassword
    Invoke-PasswordChange -Session $adminSession -CurrentPassword $temporaryAdminPassword -NewPassword $TenantAdminPasswordValue
}

Write-Host "Ensuring proof operators and users..."
Ensure-Operator `
    -AdminSession $adminSession `
    -ActorName "Operations Lead" `
    -DisplayName "Operations Lead" `
    -Description "Hosted proof tenant administrator." `
    -Roles @("TENANT_ADMIN", "REVIEW_OWNER", "ESCALATION_OWNER", "INTEGRATION_ADMIN", "INTEGRATION_OPERATOR") | Out-Null

Ensure-Operator `
    -AdminSession $adminSession `
    -ActorName "Operations Planner" `
    -DisplayName "Operations Planner" `
    -Description "Hosted proof planner/operator with non-admin access." `
    -Roles @() | Out-Null

Ensure-Operator `
    -AdminSession $adminSession `
    -ActorName "Integration Lead" `
    -DisplayName "Integration Lead" `
    -Description "Hosted proof integration administrator." `
    -Roles @("INTEGRATION_ADMIN", "INTEGRATION_OPERATOR") | Out-Null

Ensure-User `
    -AdminSession $adminSession `
    -Username $PlannerUsernameValue `
    -FullName "Hosted Verification Planner" `
    -OperatorActorName "Operations Planner" `
    -FinalPassword $PlannerPasswordValue

Ensure-User `
    -AdminSession $adminSession `
    -Username $IntegrationAdminUsernameValue `
    -FullName "Hosted Verification Integration Admin" `
    -OperatorActorName "Integration Lead" `
    -FinalPassword $IntegrationAdminPasswordValue

Write-Host "Preparing real catalog and inventory baseline for proof flows..."
Ensure-ProofCatalogAndInventory -AdminSession $adminSession -Sku $ProofProductSkuValue

Wait-AuthenticatedProofWarmup -Session $adminSession

Write-Host ""
Write-Host "Hosted proof credential path is ready."
Write-Host "Use these non-secret values when running frontend hosted proof:"
Write-Host "PLAYWRIGHT_API_BASE_URL=$script:ApiBaseUrlValue"
Write-Host "PLAYWRIGHT_TENANT_CODE=$script:TenantCodeValue"
Write-Host "PLAYWRIGHT_PROOF_PRODUCT_SKU=$ProofProductSkuValue"
Write-Host "PLAYWRIGHT_TENANT_ADMIN_USERNAME=$TenantAdminUsernameValue"
Write-Host "PLAYWRIGHT_PLANNER_USERNAME=$PlannerUsernameValue"
Write-Host "PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME=$IntegrationAdminUsernameValue"
Write-Host "Secret proof passwords are stored in the ignored proof state file for this machine."
Write-Host "Playwright reads that state file automatically; do not commit or print it."
Write-Host ""
Write-Host "Official hosted proof order:"
Write-Host "1. readiness warm-up (performed by this script and Playwright global setup)"
Write-Host "2. hosted proof tenant prep (this script)"
Write-Host "3. cd frontend"
Write-Host "4. npm.cmd run test:e2e:prod"
