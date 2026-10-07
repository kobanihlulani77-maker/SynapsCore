param(
    [ValidateRange(1, 10)]
    [int]$Samples = 5
)

$ErrorActionPreference = 'Stop'
$passwordPointer = [IntPtr]::Zero

try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker is required for the temporary PostgreSQL client.'
    }
    $previousErrorAction = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
        $dockerExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorAction
    }
    if ($dockerExitCode -ne 0) {
        throw 'Docker Desktop is not running. Start it before entering the database URL.'
    }

    $secureUrl = Read-Host 'Render External Database URL' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureUrl)
    $databaseUrl = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $parsedUrl = $null
    if (-not [Uri]::TryCreate($databaseUrl, [UriKind]::Absolute, [ref]$parsedUrl) -or
        $parsedUrl.Scheme -notin @('postgres', 'postgresql') -or
        $parsedUrl.Host -notlike '*.render.com') {
        throw 'Expected a Render External PostgreSQL URL. The URL was not printed.'
    }

    $query = @'
select to_char(clock_timestamp() at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as sampled_at_utc,
       current_database() as database_name,
       pg_backend_pid() as postgres_pid,
       i.indisvalid as audit_index_valid
from pg_class c join pg_index i on i.indexrelid = c.oid
where c.relname = 'idx_audit_logs_created_at_desc'
  and c.relnamespace = (select oid from pg_namespace where nspname = current_schema());
'@
    $auditRead = @'
explain (analyze, buffers)
select id, action, actor, created_at, details, request_id,
       source, status, target_ref, target_type, tenant_code
from audit_logs order by created_at desc fetch first 20 rows only;
'@
    $sql = "\set ON_ERROR_STOP on`nBEGIN READ ONLY;`nSET LOCAL statement_timeout = '5000ms';`n"
    $sql += $query + "`n\echo WARMUP`n" + $auditRead + "`n"
    for ($sample = 1; $sample -le $Samples; $sample++) {
        $sql += "\echo SAMPLE_$sample`n" + $auditRead + "`n"
    }
    $sql += "COMMIT;`n"

    Write-Output 'Running one read-only warmup and bounded audit SQL samples; no Platform Owner login.'
    $databaseUrl + "`n" + $sql |
        docker run --rm -i -e PGSSLMODE=require postgres:17 sh -c `
            'IFS= read -r uri; psql "$uri" -X -v ON_ERROR_STOP=1 -P pager=off -f -'
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL measurement failed with client exit code $LASTEXITCODE."
    }
} finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    Remove-Variable secureUrl, databaseUrl, parsedUrl, sql, query, auditRead,
        previousErrorAction, dockerExitCode -ErrorAction SilentlyContinue
}
