param(
    [string]$ApiBaseUrl,
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
            quantityAvailable = 8
            reorderThreshold = 12
        } | Out-Null
}

$script:ApiBaseUrlValue = (Require-Value `
    -Name "PLAYWRIGHT_API_BASE_URL" `
    -Value (Get-FirstValue -Values @($ApiBaseUrl, $env:PLAYWRIGHT_API_BASE_URL, $env:PLAYWRIGHT_BACKEND_URL, "https://synapscore-3.onrender.com"))).TrimEnd("/")
$script:TenantCodeValue = Require-TenantCode -Name "PLAYWRIGHT_TENANT_CODE" -Value (Get-FirstValue -Values @($TenantCode, $env:PLAYWRIGHT_TENANT_CODE))
$TenantNameValue = Get-FirstValue -Values @($TenantName, $env:PLAYWRIGHT_TENANT_NAME, "$script:TenantCodeValue Hosted Verification")
$TenantAdminUsernameValue = Require-Username -Name "PLAYWRIGHT_TENANT_ADMIN_USERNAME" -Value (Get-FirstValue -Values @($TenantAdminUsername, $env:PLAYWRIGHT_TENANT_ADMIN_USERNAME, $env:PLAYWRIGHT_OPERATIONS_LEAD_USERNAME))
$TenantAdminPasswordValue = Require-Password -Name "PLAYWRIGHT_TENANT_ADMIN_PASSWORD" -Value (Get-FirstValue -Values @($TenantAdminPassword, $env:PLAYWRIGHT_TENANT_ADMIN_PASSWORD, $env:PLAYWRIGHT_OPERATIONS_LEAD_PASSWORD))
$PlannerUsernameValue = Require-Username -Name "PLAYWRIGHT_PLANNER_USERNAME" -Value (Get-FirstValue -Values @($PlannerUsername, $env:PLAYWRIGHT_PLANNER_USERNAME, $env:PLAYWRIGHT_OPERATIONS_PLANNER_USERNAME))
$PlannerPasswordValue = Require-Password -Name "PLAYWRIGHT_PLANNER_PASSWORD" -Value (Get-FirstValue -Values @($PlannerPassword, $env:PLAYWRIGHT_PLANNER_PASSWORD, $env:PLAYWRIGHT_OPERATIONS_PLANNER_PASSWORD))
$IntegrationAdminUsernameValue = Require-Username -Name "PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME" -Value (Get-FirstValue -Values @($IntegrationAdminUsername, $env:PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME, $env:PLAYWRIGHT_INTEGRATION_LEAD_USERNAME))
$IntegrationAdminPasswordValue = Require-Password -Name "PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD" -Value (Get-FirstValue -Values @($IntegrationAdminPassword, $env:PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD, $env:PLAYWRIGHT_INTEGRATION_LEAD_PASSWORD))
$ProofProductSkuValue = Normalize-ProofSku -Name "PLAYWRIGHT_PROOF_PRODUCT_SKU" -Value (Get-FirstValue -Values @($ProofProductSku, $env:PLAYWRIGHT_PROOF_PRODUCT_SKU, (Get-DefaultProofProductSku -TenantCode $script:TenantCodeValue)))
$PlatformAdminTokenValue = Get-FirstValue -Values @($PlatformAdminToken, $env:SYNAPSECORE_PLATFORM_ADMIN_TOKEN)
$BootstrapInitialTokenValue = Get-FirstValue -Values @($BootstrapInitialToken, $env:SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN)

if ($script:TenantCodeValue -ieq "SYNAPSE-DEMO") {
    throw "SYNAPSE-DEMO is blocked for hosted proof. Use a real verification tenant created through /api/access/tenants."
}

$distinctUsernames = @($TenantAdminUsernameValue, $PlannerUsernameValue, $IntegrationAdminUsernameValue) | Select-Object -Unique
if ($distinctUsernames.Count -ne 3) {
    throw "Hosted proof requires three distinct sign-in accounts: tenant admin, planner/operator, and integration admin."
}

Write-Host "========================================"
Write-Host "SYNAPSECORE HOSTED PROOF PREP"
Write-Host "========================================"
Write-Host "Backend API : $script:ApiBaseUrlValue"
Write-Host "Tenant      : $script:TenantCodeValue"
Write-Host "Mode        : real tenant/admin APIs, no seed or DB edits"
Write-Host ""

$tenants = @(Get-JsonArray -Url "$script:ApiBaseUrlValue/api/access/tenants")
$tenant = $tenants | Where-Object { $null -ne $_ -and (Get-PropertyValue -Object $_ -PropertyName "code") -ieq $script:TenantCodeValue } | Select-Object -First 1

if ($null -eq $tenant) {
    $tenantHeaders = @{}
    if ($tenants.Count -eq 0) {
        if ([string]::IsNullOrWhiteSpace($BootstrapInitialTokenValue)) {
            throw "No tenants exist yet. Set SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN so the first tenant can be created safely."
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

Write-Host ""
Write-Host "Hosted proof credential path is ready."
Write-Host "Use these non-secret values when running frontend hosted proof:"
Write-Host "PLAYWRIGHT_API_BASE_URL=$script:ApiBaseUrlValue"
Write-Host "PLAYWRIGHT_TENANT_CODE=$script:TenantCodeValue"
Write-Host "PLAYWRIGHT_PROOF_PRODUCT_SKU=$ProofProductSkuValue"
Write-Host "PLAYWRIGHT_TENANT_ADMIN_USERNAME=$TenantAdminUsernameValue"
Write-Host "PLAYWRIGHT_PLANNER_USERNAME=$PlannerUsernameValue"
Write-Host "PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME=$IntegrationAdminUsernameValue"
Write-Host "Keep the password env vars set to the secret values supplied to this script."
