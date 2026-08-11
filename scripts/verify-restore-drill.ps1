param(
    [string]$ComposeFile = "./infrastructure/docker-compose.prod.yml",
    [string]$ServiceName = "postgres",
    [string]$BackupFile = "",
    [switch]$KeepBackupFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "ProdEnvTools.ps1")

$rootDir = Get-SynapseRoot -ScriptPath $PSCommandPath
$composePath = if ([System.IO.Path]::IsPathRooted($ComposeFile)) { $ComposeFile } else { Join-Path $rootDir ($ComposeFile -replace '^[.][\\/]', '') }
$generatedBackup = $false

function Get-ServiceEnvValue {
    param([string]$Key)

    $value = & docker compose -f $composePath exec -T $ServiceName printenv $Key
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to read $Key from the $ServiceName service."
    }
    return ($value | Out-String).Trim()
}

function Get-ServiceContainerId {
    $containerId = & docker compose -f $composePath ps -q $ServiceName
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to resolve the running container for service $ServiceName."
    }
    $containerId = ($containerId | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw "No running container was found for service $ServiceName."
    }
    return $containerId
}

if ([string]::IsNullOrWhiteSpace($BackupFile)) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $BackupFile = Join-Path $rootDir "backups\restore-drill-$timestamp.sql"
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "backup-postgres.ps1") -ComposeFile $composePath -ServiceName $ServiceName -OutputFile $BackupFile
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to create a disposable backup for the restore drill."
    }
    $generatedBackup = $true
} elseif (-not [System.IO.Path]::IsPathRooted($BackupFile)) {
    $BackupFile = Join-Path $rootDir ($BackupFile -replace '^[.][\\/]', '')
}

if (-not (Test-Path -LiteralPath $BackupFile)) {
    throw "Backup file not found: $BackupFile"
}

$scratchDbName = "synapse_restore_verify_{0}" -f ([Guid]::NewGuid().ToString("N").Substring(0, 12))
$postgresUser = Get-ServiceEnvValue -Key "POSTGRES_USER"
$postgresPassword = Get-ServiceEnvValue -Key "POSTGRES_PASSWORD"
$containerId = Get-ServiceContainerId
$containerBackupPath = "/tmp/synapsecore-restore-drill.sql"
$backupItem = Get-Item -LiteralPath $BackupFile
if ($backupItem.Length -le 0) {
    throw "Restore drill backup file is empty: $BackupFile"
}
$backupHash = Get-FileHash -Algorithm SHA256 -Path $BackupFile

function Invoke-PsqlScalar {
    param(
        [string]$Database,
        [string]$Sql
    )

    $result = & docker compose -f $composePath exec -T $ServiceName env "PGPASSWORD=$postgresPassword" psql -v ON_ERROR_STOP=1 -t -A -U $postgresUser -d $Database -c $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "SQL command failed against database $Database."
    }
    return ($result | Out-String).Trim()
}

$countQueries = [ordered]@{
    "flyway_success"       = "SELECT COUNT(*) FROM flyway_schema_history WHERE success;"
    "flyway_latest"        = "SELECT COALESCE(version, 'none') FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1;"
    "public_table_count"   = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';"
    "workspace_count"      = "SELECT COUNT(*) FROM tenants;"
    "user_count"           = "SELECT COUNT(*) FROM access_users;"
    "catalog_count"        = "SELECT COUNT(*) FROM products;"
    "order_count"          = "SELECT COUNT(*) FROM customer_orders;"
    "inventory_count"      = "SELECT COUNT(*) FROM inventory;"
    "alert_count"          = "SELECT COUNT(*) FROM alerts;"
    "recommendation_count" = "SELECT COUNT(*) FROM recommendations;"
    "connector_count"      = "SELECT COUNT(*) FROM integration_connectors;"
    "failed_inbound_count" = "SELECT COUNT(*) FROM integration_inbound_records WHERE status <> 'ACCEPTED';"
    "replay_count"         = "SELECT COUNT(*) FROM integration_replay_records;"
    "scenario_count"       = "SELECT COUNT(*) FROM scenario_runs;"
    "approval_count"       = "SELECT COUNT(*) FROM scenario_runs WHERE approval_status <> 'NOT_REQUIRED' OR approval_stage <> 'NOT_REQUIRED';"
}

$hashQueries = [ordered]@{
    "tenants_hash"   = "SELECT md5(COALESCE(string_agg(id::text || ':' || code || ':' || name, ',' ORDER BY id),'')) FROM tenants;"
    "users_hash"     = "SELECT md5(COALESCE(string_agg(id::text || ':' || COALESCE(tenant_id::text,'') || ':' || username || ':' || active::text || ':' || session_version::text, ',' ORDER BY id),'')) FROM access_users;"
    "products_hash"  = "SELECT md5(COALESCE(string_agg(id::text || ':' || COALESCE(tenant_id::text,'') || ':' || sku || ':' || name, ',' ORDER BY id),'')) FROM products;"
    "inventory_hash" = "SELECT md5(COALESCE(string_agg(id::text || ':' || COALESCE(tenant_id::text,'') || ':' || product_id::text || ':' || warehouse_id::text || ':' || quantity_available::text || ':' || reorder_threshold::text, ',' ORDER BY id),'')) FROM inventory;"
    "orders_hash"    = "SELECT md5(COALESCE(string_agg(id::text || ':' || COALESCE(tenant_id::text,'') || ':' || external_order_id || ':' || status, ',' ORDER BY id),'')) FROM customer_orders;"
    "inbound_hash"   = "SELECT md5(COALESCE(string_agg(id::text || ':' || COALESCE(tenant_code,'') || ':' || source_system || ':' || status, ',' ORDER BY id),'')) FROM integration_inbound_records;"
    "replay_hash"    = "SELECT md5(COALESCE(string_agg(id::text || ':' || COALESCE(tenant_code,'') || ':' || source_system || ':' || status || ':' || replay_attempt_count::text, ',' ORDER BY id),'')) FROM integration_replay_records;"
    "scenario_hash"  = "SELECT md5(COALESCE(string_agg(id::text || ':' || COALESCE(tenant_id::text,'') || ':' || type || ':' || approval_status || ':' || approval_stage, ',' ORDER BY id),'')) FROM scenario_runs;"
}

function Get-Measurements {
    param(
        [string]$Database,
        [System.Collections.Specialized.OrderedDictionary]$Queries
    )

    $measurements = [ordered]@{}
    foreach ($key in $Queries.Keys) {
        $measurements[$key] = Invoke-PsqlScalar -Database $Database -Sql $Queries[$key]
    }
    return $measurements
}

function Assert-MeasurementsMatch {
    param(
        [string]$Label,
        [System.Collections.Specialized.OrderedDictionary]$Source,
        [System.Collections.Specialized.OrderedDictionary]$Restored
    )

    Write-Host ""
    Write-Host "$Label comparison:"
    foreach ($key in $Source.Keys) {
        $sourceValue = $Source[$key]
        $restoredValue = $Restored[$key]
        $matches = $sourceValue -eq $restoredValue
        Write-Host ("- {0}: source={1} restored={2} match={3}" -f $key, $sourceValue, $restoredValue, $matches)
        if (-not $matches) {
            throw "$Label mismatch for $key."
        }
    }
}

try {
    Write-Host "========================================"
    Write-Host "SYNAPSECORE RESTORE DRILL"
    Write-Host "========================================"
    Write-Host "Compose file : $composePath"
    Write-Host "Service      : $ServiceName"
    Write-Host "Backup file  : $BackupFile"
    Write-Host "Backup size  : $($backupItem.Length) bytes"
    Write-Host "Backup SHA256: $($backupHash.Hash)"
    Write-Host "Scratch DB   : $scratchDbName"
    Write-Host ""

    $sourceCounts = Get-Measurements -Database (Get-ServiceEnvValue -Key "POSTGRES_DB") -Queries $countQueries
    $sourceHashes = Get-Measurements -Database (Get-ServiceEnvValue -Key "POSTGRES_DB") -Queries $hashQueries

    & docker compose -f $composePath exec -T $ServiceName env "PGPASSWORD=$postgresPassword" psql -v ON_ERROR_STOP=1 -U $postgresUser -d postgres -c "DROP DATABASE IF EXISTS $scratchDbName;"
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to clear any previous scratch restore database."
    }

    & docker compose -f $composePath exec -T $ServiceName env "PGPASSWORD=$postgresPassword" psql -v ON_ERROR_STOP=1 -U $postgresUser -d postgres -c "CREATE DATABASE $scratchDbName;"
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to create the scratch restore database."
    }

    & docker cp $BackupFile "${containerId}:$containerBackupPath"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to copy the SQL backup into the Postgres container for the restore drill."
    }

    try {
        & docker compose -f $composePath exec -T $ServiceName env "PGPASSWORD=$postgresPassword" psql -v ON_ERROR_STOP=1 -U $postgresUser -d $scratchDbName -f $containerBackupPath
        if ($LASTEXITCODE -ne 0) {
            throw "Restore drill failed while replaying the SQL backup into the scratch database."
        }
    } finally {
        & docker compose -f $composePath exec -T $ServiceName rm -f $containerBackupPath | Out-Null
    }

    $restoredCounts = Get-Measurements -Database $scratchDbName -Queries $countQueries
    $restoredHashes = Get-Measurements -Database $scratchDbName -Queries $hashQueries
    Assert-MeasurementsMatch -Label "Count" -Source $sourceCounts -Restored $restoredCounts
    Assert-MeasurementsMatch -Label "Hash" -Source $sourceHashes -Restored $restoredHashes

    Write-Host "Restore drill passed."
    Write-Host "Public tables : $($restoredCounts["public_table_count"])"
    Write-Host "Flyway latest : $($restoredCounts["flyway_latest"])"
    Write-Host "Workspaces    : $($restoredCounts["workspace_count"])"
    Write-Host "Users         : $($restoredCounts["user_count"])"
} finally {
    & docker compose -f $composePath exec -T $ServiceName env "PGPASSWORD=$postgresPassword" psql -v ON_ERROR_STOP=1 -U $postgresUser -d postgres -c "DROP DATABASE IF EXISTS $scratchDbName;" | Out-Null
    if ($generatedBackup -and -not $KeepBackupFile -and (Test-Path -LiteralPath $BackupFile)) {
        Remove-Item -LiteralPath $BackupFile -Force
    }
}
