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

try {
    Write-Host "========================================"
    Write-Host "SYNAPSECORE RESTORE DRILL"
    Write-Host "========================================"
    Write-Host "Compose file : $composePath"
    Write-Host "Service      : $ServiceName"
    Write-Host "Backup file  : $BackupFile"
    Write-Host "Scratch DB   : $scratchDbName"
    Write-Host ""

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

    $publicTableCount = [int](Invoke-PsqlScalar -Database $scratchDbName -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';")
    $auditCount = [int](Invoke-PsqlScalar -Database $scratchDbName -Sql "SELECT COUNT(*) FROM audit_logs;")
    $eventCount = [int](Invoke-PsqlScalar -Database $scratchDbName -Sql "SELECT COUNT(*) FROM business_events;")
    $scenarioCount = [int](Invoke-PsqlScalar -Database $scratchDbName -Sql "SELECT COUNT(*) FROM scenario_runs;")

    if ($publicTableCount -lt 10) {
        throw "Restore drill loaded too few public tables ($publicTableCount)."
    }
    if ($auditCount -lt 1) {
        throw "Restore drill found no audit logs in the restored snapshot."
    }
    if ($eventCount -lt 1) {
        throw "Restore drill found no business events in the restored snapshot."
    }
    if ($scenarioCount -lt 1) {
        throw "Restore drill found no scenario history in the restored snapshot."
    }

    Write-Host "Restore drill passed."
    Write-Host "Public tables : $publicTableCount"
    Write-Host "Audit logs    : $auditCount"
    Write-Host "Business events: $eventCount"
    Write-Host "Scenario runs : $scenarioCount"
} finally {
    & docker compose -f $composePath exec -T $ServiceName env "PGPASSWORD=$postgresPassword" psql -v ON_ERROR_STOP=1 -U $postgresUser -d postgres -c "DROP DATABASE IF EXISTS $scratchDbName;" | Out-Null
    if ($generatedBackup -and -not $KeepBackupFile -and (Test-Path -LiteralPath $BackupFile)) {
        Remove-Item -LiteralPath $BackupFile -Force
    }
}
