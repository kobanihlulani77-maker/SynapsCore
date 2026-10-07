param(
    [ValidateRange(1, 10)]
    [int]$Samples = 5
)

$ErrorActionPreference = 'Stop'
$passwordPointer = [IntPtr]::Zero
$process = $null

try {
    $java = Get-Command java -ErrorAction Stop
    $sourcePath = Join-Path $PSScriptRoot 'MeasurePlatformAuditSql.java'
    $driverJar = Get-ChildItem -LiteralPath (Join-Path $HOME '.m2\repository\org\postgresql\postgresql') `
        -Filter 'postgresql-*.jar' -Recurse -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
    if (-not $driverJar) {
        throw 'The PostgreSQL JDBC driver is not cached. Build the backend dependencies before running this probe.'
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

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $java.Source
    $startInfo.Arguments = '-cp "{0}" "{1}" {2}' -f $driverJar, $sourcePath, $Samples
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    if (-not $process.Start()) { throw 'Could not start the local Java PostgreSQL client.' }
    $outputTask = $process.StandardOutput.ReadToEndAsync()
    $errorTask = $process.StandardError.ReadToEndAsync()
    $process.StandardInput.WriteLine($databaseUrl)
    $process.StandardInput.Close()
    if (-not $process.WaitForExit(120000)) {
        $process.Kill()
        throw 'The bounded PostgreSQL measurement exceeded two minutes.'
    }
    $output = $outputTask.Result.TrimEnd()
    $errorOutput = $errorTask.Result.TrimEnd()
    if ($output) { Write-Output $output }
    if ($process.ExitCode -ne 0) {
        throw "PostgreSQL measurement failed. $errorOutput"
    }
} finally {
    if ($process) { $process.Dispose() }
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    Remove-Variable secureUrl, databaseUrl, parsedUrl, output, errorOutput -ErrorAction SilentlyContinue
}
