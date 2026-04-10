param(
    [string]$ComposeFile = "./infrastructure/docker-compose.prod.yml",
    [string]$ServiceName = "postgres",
    [string]$BackupDirectory = "./backups",
    [string]$OutputFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "ProdEnvTools.ps1")

$rootDir = Get-SynapseRoot -ScriptPath $PSCommandPath
$composePath = if ([System.IO.Path]::IsPathRooted($ComposeFile)) { $ComposeFile } else { Join-Path $rootDir ($ComposeFile -replace '^[.][\\/]', '') }
$backupDirectoryPath = if ([System.IO.Path]::IsPathRooted($BackupDirectory)) { $BackupDirectory } else { Join-Path $rootDir ($BackupDirectory -replace '^[.][\\/]', '') }

if (-not $OutputFile) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutputFile = Join-Path $backupDirectoryPath "synapsecore-postgres-$timestamp.sql"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputFile)) {
    $OutputFile = Join-Path $rootDir ($OutputFile -replace '^[.][\\/]', '')
}

New-Item -ItemType Directory -Path (Split-Path -Parent $OutputFile) -Force | Out-Null

Write-Host "========================================"
Write-Host "SYNAPSECORE POSTGRES BACKUP"
Write-Host "========================================"
Write-Host "Compose file : $composePath"
Write-Host "Service      : $ServiceName"
Write-Host "Output file  : $OutputFile"
Write-Host ""

$dump = & docker compose -f $composePath exec -T $ServiceName sh -lc 'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges'
if ($LASTEXITCODE -ne 0) {
    throw "Postgres backup command failed."
}

$dumpText = if ($dump -is [System.Array]) {
    (($dump | ForEach-Object { [string]$_ }) -join "`n") + "`n"
} else {
    [string]$dump
}

[System.IO.File]::WriteAllText($OutputFile, $dumpText, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "Backup written to $OutputFile"
