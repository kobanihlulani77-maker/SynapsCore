$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot 'backend'
$jdbcUrl = if ($env:SPRING_DATASOURCE_URL) { $env:SPRING_DATASOURCE_URL } elseif ($env:DATABASE_URL) { $env:DATABASE_URL } else { '' }
$dbUser = if ($env:SPRING_DATASOURCE_USERNAME) { $env:SPRING_DATASOURCE_USERNAME } elseif ($env:DB_USER) { $env:DB_USER } else { '' }
$dbPassword = if ($env:SPRING_DATASOURCE_PASSWORD) { $env:SPRING_DATASOURCE_PASSWORD } elseif ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { '' }

if (-not $jdbcUrl) {
    throw 'Set SPRING_DATASOURCE_URL or DATABASE_URL before running Flyway validation.'
}

Push-Location $backendDir
try {
    Write-Host "Validating Flyway migrations against $jdbcUrl"
    cmd /c mvnw.cmd "-Dflyway.url=$jdbcUrl" "-Dflyway.user=$dbUser" "-Dflyway.password=$dbPassword" org.flywaydb:flyway-maven-plugin:validate
} finally {
    Pop-Location
}
