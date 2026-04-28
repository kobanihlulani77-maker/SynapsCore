$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot 'backend'
$outputPath = Join-Path $backendDir 'target\\flyway-baseline.sql'

Push-Location $backendDir
try {
    Write-Host 'Exporting the current JPA schema into a Flyway baseline script...'
    cmd /c mvnw.cmd "-Dspring-boot.run.profiles=schema-export" spring-boot:run
    if (-not (Test-Path $outputPath)) {
        throw "Expected Flyway baseline output at $outputPath, but no file was generated."
    }
    Write-Host "Flyway baseline exported to $outputPath"
} finally {
    Pop-Location
}
