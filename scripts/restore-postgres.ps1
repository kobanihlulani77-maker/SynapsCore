param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$ComposeFile = "./infrastructure/docker-compose.prod.yml",
    [string]$ServiceName = "postgres",
    [switch]$Yes
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "ProdEnvTools.ps1")

$rootDir = Get-SynapseRoot -ScriptPath $PSCommandPath
$composePath = if ([System.IO.Path]::IsPathRooted($ComposeFile)) { $ComposeFile } else { Join-Path $rootDir ($ComposeFile -replace '^[.][\\/]', '') }
$backupPath = if ([System.IO.Path]::IsPathRooted($BackupFile)) { $BackupFile } else { Join-Path $rootDir ($BackupFile -replace '^[.][\\/]', '') }

if (-not (Test-Path -LiteralPath $backupPath)) {
    throw "Backup file not found: $backupPath"
}

if (-not $Yes) {
    throw "Refusing to restore without -Yes because this resets the target database schema."
}

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

$postgresUser = Get-ServiceEnvValue -Key "POSTGRES_USER"
$postgresPassword = Get-ServiceEnvValue -Key "POSTGRES_PASSWORD"
$containerId = Get-ServiceContainerId
$containerBackupPath = "/tmp/synapsecore-restore.sql"

Write-Host "========================================"
Write-Host "SYNAPSECORE POSTGRES RESTORE"
Write-Host "========================================"
Write-Host "Compose file : $composePath"
Write-Host "Service      : $ServiceName"
Write-Host "Backup file  : $backupPath"
Write-Host ""
Write-Host "Resetting target schema before restore..."

& docker compose -f $composePath exec -T $ServiceName sh -lc 'PGPASSWORD="$POSTGRES_PASSWORD" psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"'
if ($LASTEXITCODE -ne 0) {
    throw "Failed to reset the target database schema before restore."
}

Write-Host "Restoring backup..."
& docker cp $backupPath "${containerId}:$containerBackupPath"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to copy the SQL backup into the Postgres container."
}

try {
    & docker compose -f $composePath exec -T $ServiceName env "PGPASSWORD=$postgresPassword" psql -v ON_ERROR_STOP=1 -U $postgresUser -d (Get-ServiceEnvValue -Key "POSTGRES_DB") -f $containerBackupPath
    if ($LASTEXITCODE -ne 0) {
        throw "Restore failed while replaying the SQL backup."
    }
} finally {
    & docker compose -f $composePath exec -T $ServiceName rm -f $containerBackupPath | Out-Null
}

Write-Host "Restore completed successfully."
