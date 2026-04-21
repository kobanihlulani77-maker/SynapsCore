param(
    [string]$FrontendUrl = "http://127.0.0.1",
    [string]$BackendUrl = "http://127.0.0.1:8080",
    [string]$SeedTenantCode = "STARTER-OPS",
    [string]$SeedAdminUsername = "operations.lead",
    [string]$SeedAdminPassword = "lead-2026"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Get-ResponseBody {
    param([System.Exception]$Exception)

    if ($null -eq $Exception.Response) {
        return $Exception.Message
    }

    try {
        $stream = $Exception.Response.GetResponseStream()
        if ($null -eq $stream) {
            return $Exception.Message
        }
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        $reader.Dispose()
        return $body
    } catch {
        return $Exception.Message
    }
}

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Url,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session = $null,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $invokeArgs = @{
        Method          = $Method
        Uri             = $Url
        UseBasicParsing = $true
        Headers         = $Headers
    }

    if ($null -ne $Session) {
        $invokeArgs.WebSession = $Session
    }

    if ($null -ne $Body) {
        if ($Body -is [string]) {
            $invokeArgs.Body = $Body
        } else {
            $invokeArgs.Body = $Body | ConvertTo-Json -Depth 12
        }
        $invokeArgs.ContentType = "application/json"
    }

    try {
        $response = Invoke-WebRequest @invokeArgs
    } catch {
        $errorBody = Get-ResponseBody $_.Exception
        throw "Request failed: $Method $Url`n$errorBody"
    }

    $json = $null
    if (-not [string]::IsNullOrWhiteSpace($response.Content)) {
        try {
            $json = $response.Content | ConvertFrom-Json
        } catch {
            $json = $null
        }
    }

    [pscustomobject]@{
        StatusCode = [int]$response.StatusCode
        Json       = $json
        Raw        = $response.Content
        Headers    = $response.Headers
    }
}

function Invoke-MultipartCsvImport {
    param(
        [string]$Url,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [string]$SourceSystem,
        [string]$CsvText
    )

    $uri = [System.Uri]$Url
    $cookieHeader = ($Session.Cookies.GetCookies($uri) | ForEach-Object {
            "$($_.Name)=$($_.Value)"
        }) -join "; "
    $csvFile = [System.IO.Path]::GetTempFileName()
    $responseFile = [System.IO.Path]::GetTempFileName()

    try {
        [System.IO.File]::WriteAllText($csvFile, $CsvText, (New-Object System.Text.UTF8Encoding($false)))

        $curlArgs = @(
            "-sS",
            "-o", $responseFile,
            "-w", "%{http_code}",
            "-H", "Cookie: $cookieHeader",
            "-F", "file=@$csvFile;type=text/csv;filename=orders.csv"
        )

        if (-not [string]::IsNullOrWhiteSpace($SourceSystem)) {
            $curlArgs += @("-F", "sourceSystem=$SourceSystem")
        }

        $curlArgs += $Url

        $statusCode = & curl.exe @curlArgs
        $content = [System.IO.File]::ReadAllText($responseFile)

        if ($statusCode -notmatch '^\d{3}$') {
            throw "Multipart request did not return a valid HTTP status code: $statusCode"
        }

        if ([int]$statusCode -lt 200 -or [int]$statusCode -gt 299) {
            throw "Multipart request failed: POST $Url`n$content"
        }

        [pscustomobject]@{
            StatusCode = [int]$statusCode
            Json       = if ([string]::IsNullOrWhiteSpace($content)) { $null } else { $content | ConvertFrom-Json }
            Raw        = $content
        }
    } finally {
        if (Test-Path $csvFile) {
            Remove-Item $csvFile -Force
        }
        if (Test-Path $responseFile) {
            Remove-Item $responseFile -Force
        }
    }
}

function New-Session {
    New-Object Microsoft.PowerShell.Commands.WebRequestSession
}

function Login-Session {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [string]$TenantCode,
        [string]$Username,
        [string]$Password
    )

    $login = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/auth/session/login" -Session $Session -Body @{
        tenantCode = $TenantCode
        username   = $Username
        password   = $Password
    }

    Assert-True ($login.StatusCode -eq 200) "Login failed for $TenantCode/$Username"
    return $login.Json
}

function Get-RouteStatus {
    param([string]$Url)
    $response = Invoke-WebRequest -UseBasicParsing $Url
    [int]$response.StatusCode
}

$timestamp = Get-Date -Format "MMddHHmmss"
$tenantCode = "LIVE-$timestamp"
$tenantName = "Live Ops $timestamp"
$adminUsername = "workspace.admin"
$adminPassword = "Ready-Verify-2026!"
$tenantPrefix = $tenantCode.ToLower().Replace("-", "_")
$northWebhookSource = "${tenantPrefix}_north"
$replaySource = "${tenantPrefix}_replay"

$companySummary = [ordered]@{
    tenantCode                = $tenantCode
    tenantName                = $tenantName
    frontendRoutesVerified    = @()
    backendChecksVerified     = @()
    workspaceAdminVerified    = $false
    integrationVerified       = $false
    replayVerified            = $false
    planningVerified          = $false
    escalatedPlanningVerified = $false
    fulfillmentVerified       = $false
    trustSurfaceVerified      = $false
}

Write-Step "Verify frontend product routes"
$routes = @(
    "/",
    "/product",
    "/sign-in",
    "/contact",
    "/dashboard",
    "/alerts",
    "/recommendations",
    "/orders",
    "/inventory",
    "/locations",
    "/fulfillment",
    "/scenarios",
    "/scenario-history",
    "/approvals",
    "/escalations",
    "/integrations",
    "/replay-queue",
    "/runtime",
    "/audit-events",
    "/users",
    "/company-settings",
    "/profile",
    "/platform-admin",
    "/tenant-management",
    "/system-config",
    "/releases"
)
foreach ($route in $routes) {
    $statusCode = Get-RouteStatus "$FrontendUrl$route"
    Assert-True ($statusCode -eq 200) "Frontend route $route did not return 200."
    $companySummary.frontendRoutesVerified += $route
}

Write-Step "Verify backend runtime surface and sign in as seed admin"
$seedSession = New-Session
$null = Login-Session -Session $seedSession -TenantCode $SeedTenantCode -Username $SeedAdminUsername -Password $SeedAdminPassword
$backendUrls = @(
    "$BackendUrl/actuator/health/readiness",
    "$BackendUrl/api/dashboard/summary",
    "$BackendUrl/api/system/runtime",
    "$BackendUrl/api/system/incidents",
    "$BackendUrl/actuator/prometheus"
)
foreach ($url in $backendUrls) {
    $statusCode = if ($url -like "$BackendUrl/actuator/*") {
        (Invoke-WebRequest -UseBasicParsing $url).StatusCode
    } else {
        (Invoke-WebRequest -UseBasicParsing $url -WebSession $seedSession).StatusCode
    }
    Assert-True ($statusCode -eq 200) "Backend check failed for $url"
    $companySummary.backendChecksVerified += $url
}

$onboarding = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/access/tenants" -Session $seedSession -Body @{
    tenantCode       = $tenantCode
    tenantName       = $tenantName
    description      = "Local production-readiness verification workspace."
    adminFullName    = "Workspace Admin $timestamp"
    adminUsername    = $adminUsername
    adminPassword    = $adminPassword
    primaryLocation  = "Johannesburg"
    secondaryLocation = "Cape Town"
}
Assert-True ($onboarding.StatusCode -eq 200) "Tenant onboarding did not succeed."
Assert-True ($onboarding.Json.tenantCode -eq $tenantCode) "Tenant onboarding response returned the wrong tenant code."

$executiveUsername = [string]$onboarding.Json.executiveUsername
$executivePassword = "Executive-Approve-2026!"

Write-Step "Sign in as the new company admin and verify workspace administration"
$adminSession = New-Session
$null = Login-Session -Session $adminSession -TenantCode $tenantCode -Username $adminUsername -Password $adminPassword

$workspace = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/access/admin/workspace" -Session $adminSession
$operators = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/access/admin/operators" -Session $adminSession
$users = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/access/admin/users" -Session $adminSession
$warehouses = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/warehouses" -Session $adminSession
$connectors = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/integrations/orders/connectors" -Session $adminSession

Assert-True ($workspace.Json.tenantCode -eq $tenantCode) "Workspace admin payload was not tenant-scoped."
Assert-True ($operators.Json.Count -ge 3) "Expected starter operators in the new workspace."
Assert-True ($users.Json.Count -ge 2) "Expected starter users in the new workspace."
Assert-True ($warehouses.Json.Count -eq 2) "Expected two starter warehouses."
Assert-True ($connectors.StatusCode -eq 200) "Expected connector directory access for the new workspace."

$updatedWorkspace = Invoke-JsonRequest -Method Put -Url "$BackendUrl/api/access/admin/workspace" -Session $adminSession -Body @{
    tenantName  = "$tenantName Control Center"
    description = "Operational workspace verified against the live local production stack."
}
Assert-True ($updatedWorkspace.Json.tenantName -eq "$tenantName Control Center") "Workspace metadata update failed."

$securityUpdate = Invoke-JsonRequest -Method Put -Url "$BackendUrl/api/access/admin/workspace/security" -Session $adminSession -Body @{
    passwordRotationDays  = 120
    sessionTimeoutMinutes = 240
    invalidateOtherSessions = $false
}
Assert-True ($securityUpdate.Json.securitySettings.sessionTimeoutMinutes -eq 240) "Workspace security settings update failed."

$northWarehouse = @($warehouses.Json | Where-Object { $_.code -eq "WH-NORTH" })[0]
Assert-True ($null -ne $northWarehouse) "North warehouse not found in new workspace."
$warehouseUpdate = Invoke-JsonRequest -Method Put -Url "$BackendUrl/api/access/admin/workspace/warehouses/$($northWarehouse.id)" -Session $adminSession -Body @{
    name     = "$tenantName Primary Hub"
    location = "Johannesburg DC"
}
Assert-True ($warehouseUpdate.Json.name -eq "$tenantName Primary Hub") "Warehouse update failed."

$operatorCreate = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/access/admin/operators" -Session $adminSession -Body @{
    actorName       = "North Integration Operator"
    displayName     = "North Integration Operator"
    description     = "Warehouse-scoped operator for inbound recovery and connector ownership."
    active          = $true
    roles           = @("INTEGRATION_OPERATOR")
    warehouseScopes = @("WH-NORTH")
}
Assert-True ($operatorCreate.Json.actorName -eq "North Integration Operator") "Failed to create the scoped integration operator."

$userCreate = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/access/admin/users" -Session $adminSession -Body @{
    username          = "north.integration"
    fullName          = "North Integration Operator"
    password          = "North-Integration-2026!"
    operatorActorName = "North Integration Operator"
}
Assert-True ($userCreate.Json.username -eq "north.integration") "Failed to create the integration operator user."

$northWebhookConnector = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/integrations/orders/connectors" -Session $adminSession -Body @{
    sourceSystem                  = $northWebhookSource
    type                          = "WEBHOOK_ORDER"
    displayName                   = "North Webhook Ingress"
    enabled                       = $true
    syncMode                      = "PUSH_WEBHOOK"
    validationPolicy              = "STRICT"
    transformationPolicy          = "NORMALIZE_CODES"
    allowDefaultWarehouseFallback = $false
    defaultWarehouseCode          = "WH-NORTH"
    notes                         = "North inbound webhook lane for production-readiness verification."
}
Assert-True ($northWebhookConnector.Json.sourceSystem -eq $northWebhookSource) "Failed to create the north webhook connector."

$connectorSupportUpdate = Invoke-JsonRequest -Method Put -Url "$BackendUrl/api/access/admin/workspace/connectors/$($northWebhookConnector.Json.id)" -Session $adminSession -Body @{
    supportOwnerActorName = "North Integration Operator"
    notes                 = "North inbound lane owned by the scoped operator."
}
Assert-True ($connectorSupportUpdate.Json.supportOwnerActorName -eq "North Integration Operator") "Connector support ownership update failed."

$executiveUser = @($users.Json | Where-Object { $_.username -eq $executiveUsername })[0]
Assert-True ($null -ne $executiveUser) "Executive approver user was not returned in the tenant user directory."

$executivePasswordReset = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/access/admin/users/$($executiveUser.id)/reset-password" -Session $adminSession -Body @{
    password = $executivePassword
}
Assert-True ($executivePasswordReset.StatusCode -eq 200) "Failed to reset the executive approver password for readiness verification."
$companySummary.workspaceAdminVerified = $true

Write-Step "Create a replay connector and verify live webhook ingestion"
$replayConnector = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/integrations/orders/connectors" -Session $adminSession -Body @{
    sourceSystem                  = $replaySource
    type                          = "CSV_ORDER_IMPORT"
    displayName                   = "Replay Recovery Feed"
    enabled                       = $false
    syncMode                      = "BATCH_FILE_DROP"
    validationPolicy              = "RELAXED"
    transformationPolicy          = "NORMALIZE_CODES"
    allowDefaultWarehouseFallback = $false
    notes                         = "Starts disabled so replay recovery can be proven before go-live."
}
Assert-True (-not $replayConnector.Json.enabled) "Replay connector should start disabled."

$webhookOrder = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/integrations/orders/webhook" -Session $adminSession -Body @{
    sourceSystem    = $northWebhookSource
    externalOrderId = "LIVE-WH-$timestamp"
    customerReference = "CUST-$timestamp"
    occurredAt      = (Get-Date).ToUniversalTime().ToString("o")
    items           = @(
        @{
            productSku = "sku-flx-100"
            quantity   = 2
            unitPrice  = 95.00
        }
    )
}
Assert-True ($webhookOrder.StatusCode -eq 201) "Webhook ingestion did not return Created."
Assert-True ($webhookOrder.Json.order.warehouseCode -eq "WH-NORTH") "Webhook order did not normalize into the north warehouse."

$inventoryPressure = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/inventory/update" -Session $adminSession -Body @{
    productSku        = "SKU-VDR-210"
    warehouseCode     = "WH-COAST"
    quantityAvailable = 5
    reorderThreshold  = 12
}
Assert-True ($inventoryPressure.Json.lowStock -eq $true) "Inventory pressure update did not trigger low-stock state."

$alerts = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/alerts" -Session $adminSession
$recommendations = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/recommendations" -Session $adminSession
Assert-True ($alerts.Json.activeAlerts.Count -ge 1) "Expected active alerts after low-stock pressure."
Assert-True ($recommendations.Json.Count -ge 1) "Expected recommendations after low-stock pressure."
$companySummary.integrationVerified = $true

Write-Step "Verify fulfillment and delayed logistics handling"
$fulfillmentBefore = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/fulfillment" -Session $adminSession
$matchingFulfillment = @($fulfillmentBefore.Json.activeFulfillments | Where-Object { $_.externalOrderId -eq "LIVE-WH-$timestamp" })[0]
Assert-True ($null -ne $matchingFulfillment) "Fulfillment task was not created for the webhook order."

$fulfillmentUpdate = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/fulfillment/updates" -Session $adminSession -Body @{
    externalOrderId     = "LIVE-WH-$timestamp"
    status              = "DELAYED"
    carrier             = "CourierX"
    trackingReference   = "TRK-$timestamp"
    expectedDeliveryAt  = (Get-Date).AddHours(6).ToUniversalTime().ToString("o")
    occurredAt          = (Get-Date).ToUniversalTime().ToString("o")
    note                = "Delay introduced during production-readiness verification."
}
Assert-True ($fulfillmentUpdate.Json.fulfillmentStatus -eq "DELAYED") "Fulfillment status did not update to DELAYED."

$fulfillmentAfter = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/fulfillment" -Session $adminSession
Assert-True ($fulfillmentAfter.Json.delayedShipmentCount -ge 1) "Delayed fulfillment count did not increase."
$companySummary.fulfillmentVerified = $true

Write-Step "Verify failed inbound recovery and replay"
$csvImportFailure = Invoke-MultipartCsvImport -Url "$BackendUrl/api/integrations/orders/csv-import" -Session $adminSession -SourceSystem $replaySource -CsvText @"
externalOrderId,warehouseCode,productSku,quantity,unitPrice
LIVE-RPL-$timestamp,WH-NORTH,SKU-PLS-330,2,88.00
"@
Assert-True ($csvImportFailure.Json.ordersImported -eq 0) "Disabled replay connector should not import orders."
Assert-True ($csvImportFailure.Json.ordersFailed -eq 1) "Disabled replay connector should create a failed inbound order."

$imports = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/integrations/orders/imports/recent" -Session $adminSession
$replayQueue = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/integrations/orders/replay-queue" -Session $adminSession
Assert-True ($imports.Json.Count -ge 1) "Expected recent import history after CSV ingestion."
Assert-True ($replayQueue.Json.Count -ge 1) "Expected replay queue entries after failed CSV ingestion."
$replayRecord = @($replayQueue.Json | Where-Object { $_.sourceSystem -eq $replaySource })[0]
Assert-True ($null -ne $replayRecord) "Replay queue record for the replay connector was not found."

$enabledReplayConnector = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/integrations/orders/connectors" -Session $adminSession -Body @{
    sourceSystem                  = $replaySource
    type                          = "CSV_ORDER_IMPORT"
    displayName                   = "Replay Recovery Feed"
    enabled                       = $true
    syncMode                      = "BATCH_FILE_DROP"
    validationPolicy              = "RELAXED"
    transformationPolicy          = "NORMALIZE_CODES"
    allowDefaultWarehouseFallback = $false
    notes                         = "Enabled after replay recovery preflight."
}
Assert-True ($enabledReplayConnector.Json.enabled -eq $true) "Replay connector did not enable correctly."

$integrationSession = New-Session
$null = Login-Session -Session $integrationSession -TenantCode $tenantCode -Username "north.integration" -Password "North-Integration-2026!"

try {
    $null = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/access/admin/workspace" -Session $integrationSession
    throw "Scoped integration operator unexpectedly accessed workspace admin."
} catch {
    if ($_.Exception.Message -notmatch "403" -and $_ -notmatch "403") {
        throw
    }
}

$replayQueueScoped = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/integrations/orders/replay-queue" -Session $integrationSession
Assert-True ($replayQueueScoped.Json.Count -ge 1) "Scoped integration operator could not see the replay queue."

$replayResult = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/integrations/orders/replay/$($replayRecord.id)" -Session $integrationSession
Assert-True ($replayResult.Json.replay.status -eq "REPLAYED") "Replay did not complete successfully."
Assert-True ($replayResult.Json.order.warehouseCode -eq "WH-NORTH") "Replay did not restore the order into the north warehouse."

$replayQueueAfter = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/integrations/orders/replay-queue" -Session $integrationSession
Assert-True ($replayQueueAfter.Json.Count -eq 0) "Replay queue should be empty after successful replay."
$companySummary.replayVerified = $true

Write-Step "Verify planning, approvals, and execution"
$scenarioSave = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/scenarios/save" -Session $adminSession -Body @{
    title       = "North execution candidate $timestamp"
    requestedBy = "Operations Lead"
    request     = @{
        warehouseCode = "WH-NORTH"
        items         = @(
            @{
                productSku = "SKU-VDR-210"
                quantity   = 2
                unitPrice  = 140.00
            }
        )
    }
}
Assert-True ($scenarioSave.StatusCode -eq 201) "Scenario save did not return Created."

$simpleScenarioId = [long]$scenarioSave.Json.scenarioRunId
$simpleApproval = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/scenarios/$simpleScenarioId/approve" -Session $adminSession -Body @{
    actorRole    = "REVIEW_OWNER"
    approverName = "Operations Lead"
}
Assert-True ($simpleApproval.Json.approvalStatus -eq "APPROVED") "Simple scenario approval failed."

$simpleExecution = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/scenarios/$simpleScenarioId/execute" -Session $adminSession
Assert-True ($simpleExecution.Json.order.warehouseCode -eq "WH-NORTH") "Simple scenario execution did not create a live north order."
$companySummary.planningVerified = $true

$escalatedScenario = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/scenarios/save" -Session $adminSession -Body @{
    title       = "Escalated coast plan $timestamp"
    requestedBy = "Operations Planner"
    reviewOwner = "Operations Lead"
    request     = @{
        warehouseCode = "WH-COAST"
        items         = @(
            @{
                productSku = "SKU-VDR-210"
                quantity   = 2
                unitPrice  = 140.00
            }
        )
    }
}
Assert-True ($escalatedScenario.Json.approvalStage -eq "PENDING_REVIEW") "Escalated scenario should start in review."
Assert-True ($escalatedScenario.Json.approvalPolicy -eq "ESCALATED") "Escalated scenario did not save with the expected escalated approval policy."
$escalatedScenarioId = [long]$escalatedScenario.Json.scenarioRunId

$reviewApproval = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/scenarios/$escalatedScenarioId/approve" -Session $adminSession -Body @{
    actorRole    = "REVIEW_OWNER"
    approverName = "Operations Lead"
    approvalNote = "Owner review confirms the staged coast exposure."
}
Assert-True ($reviewApproval.Json.approvalStage -eq "PENDING_FINAL_APPROVAL") "Review approval did not move the escalated plan forward."

$executiveSession = New-Session
$null = Login-Session -Session $executiveSession -TenantCode $tenantCode -Username $executiveUsername -Password $executivePassword
$finalApproval = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/scenarios/$escalatedScenarioId/approve" -Session $executiveSession -Body @{
    actorRole    = "FINAL_APPROVER"
    approverName = "Executive Operations Director"
    approvalNote = "Final approval granted after risk acknowledgement."
}
Assert-True ($finalApproval.Json.approvalStatus -eq "APPROVED") "Final approval failed for the escalated plan."

$escalatedExecution = Invoke-JsonRequest -Method Post -Url "$BackendUrl/api/scenarios/$escalatedScenarioId/execute" -Session $executiveSession
Assert-True ($escalatedExecution.Json.order.warehouseCode -eq "WH-COAST") "Escalated scenario did not execute into the coast warehouse."
$companySummary.escalatedPlanningVerified = $true

Write-Step "Verify runtime trust, audit, events, and dashboard surfaces"
$summary = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/dashboard/summary" -Session $adminSession
$snapshot = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/dashboard/snapshot" -Session $adminSession
$runtime = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/system/runtime" -Session $adminSession
$incidents = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/system/incidents" -Session $adminSession
$events = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/events/recent" -Session $adminSession
$audit = Invoke-JsonRequest -Method Get -Url "$BackendUrl/api/audit/recent" -Session $adminSession

Assert-True ($summary.Json.totalOrders -ge 4) "Expected multiple live orders after the company workflow."
Assert-True ($snapshot.Json.integrationConnectors.Count -ge 4) "Snapshot should include tenant integration connectors."
Assert-True ($runtime.Json.overallStatus -eq "UP") "Runtime endpoint did not report UP."
Assert-True ($runtime.Json.metrics.ordersIngested -ge 1) "Runtime metrics did not reflect order ingestion."
Assert-True ($events.Json.Count -ge 1) "Recent events feed is empty."
Assert-True ($audit.Json.Count -ge 1) "Recent audit feed is empty."
Assert-True ($incidents.StatusCode -eq 200) "System incidents payload was not returned."
$companySummary.trustSurfaceVerified = $true

Write-Step "Company readiness verification passed"
$companySummary | ConvertTo-Json -Depth 12
