Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot

function Test-SynapsePath {
    param([string]$RelativePath)
    Test-Path -LiteralPath (Join-Path $rootDir $RelativePath)
}

function Read-EnvLines {
    param([string]$RelativePath)
    $path = Join-Path $rootDir $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        return @()
    }

    return @(Get-Content -LiteralPath $path | Where-Object {
        $_ -and -not $_.TrimStart().StartsWith("#")
    })
}

function Test-EnvContainsKey {
    param(
        [string]$RelativePath,
        [string]$Key
    )

    $lines = Read-EnvLines -RelativePath $RelativePath
    return @($lines | Where-Object { $_ -match "^$([regex]::Escape($Key))=" }).Count -gt 0
}

$requiredFiles = @(
    "infrastructure/env/backend.env",
    "infrastructure/env/frontend.env",
    "infrastructure/env/backend.prod.example.env",
    "infrastructure/env/frontend.prod.example.env",
    "backend/src/main/resources/application-dev.yml",
    "backend/src/main/resources/application-prod.yml"
)

$recommendedLocalExamples = @(
    "backend/.env.local.example",
    "frontend/.env.local.example"
)

$frontendKeys = @("VITE_API_URL", "VITE_WS_URL")
$backendProdKeys = @(
    "SPRING_PROFILES_ACTIVE",
    "CORS_ALLOWED_ORIGINS",
    "SESSION_COOKIE_SECURE",
    "SESSION_COOKIE_SAME_SITE",
    "ALLOW_HEADER_FALLBACK",
    "SYNAPSECORE_BUILD_VERSION",
    "SYNAPSECORE_BUILD_COMMIT",
    "SYNAPSECORE_BUILD_TIME"
)

$backendLocalKeys = @(
    "SPRING_PROFILES_ACTIVE",
    "SPRING_DATASOURCE_URL",
    "SPRING_DATASOURCE_USERNAME",
    "SPRING_DATASOURCE_PASSWORD",
    "REDIS_HOST",
    "REDIS_PORT"
)

$missingFiles = @($requiredFiles | Where-Object { -not (Test-SynapsePath $_) })
$missingLocalExamples = @($recommendedLocalExamples | Where-Object { -not (Test-SynapsePath $_) })
$missingFrontendKeys = @($frontendKeys | Where-Object { -not (Test-EnvContainsKey -RelativePath "infrastructure/env/frontend.env" -Key $_) })
$missingFrontendProdKeys = @($frontendKeys | Where-Object { -not (Test-EnvContainsKey -RelativePath "infrastructure/env/frontend.prod.example.env" -Key $_) })
$missingBackendProdKeys = @($backendProdKeys | Where-Object { -not (Test-EnvContainsKey -RelativePath "infrastructure/env/backend.prod.example.env" -Key $_) })
$missingBackendLocalKeys = @($backendLocalKeys | Where-Object { -not (Test-EnvContainsKey -RelativePath "infrastructure/env/backend.env" -Key $_) })

$classification = "CLEAN"
if ($missingFiles.Count -gt 0 -or $missingFrontendKeys.Count -gt 0 -or $missingFrontendProdKeys.Count -gt 0 -or $missingBackendProdKeys.Count -gt 0 -or $missingBackendLocalKeys.Count -gt 0) {
    $classification = "NEEDS_ATTENTION"
}
elseif ($missingLocalExamples.Count -gt 0) {
    $classification = "LOCAL_GUIDANCE_MISSING"
}

Write-Host "=================================================="
Write-Host "SYNAPSECORE ENV SANITY CHECK"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
Write-Host "Classification"
Write-Host "--------------"
Write-Host $classification
Write-Host ""

Write-Host "Required Files"
Write-Host "--------------"
if ($missingFiles.Count -eq 0) {
    Write-Host "- all required env/config files present"
}
else {
    $missingFiles | ForEach-Object { Write-Host "- missing: $_" }
}
Write-Host ""

Write-Host "Recommended Local Examples"
Write-Host "--------------------------"
if ($missingLocalExamples.Count -eq 0) {
    Write-Host "- local example files present"
}
else {
    $missingLocalExamples | ForEach-Object { Write-Host "- missing local example: $_" }
}
Write-Host ""

Write-Host "Frontend Template Keys"
Write-Host "----------------------"
if ($missingFrontendKeys.Count -eq 0 -and $missingFrontendProdKeys.Count -eq 0) {
    Write-Host "- frontend env templates include expected API and WS keys"
}
else {
    $missingFrontendKeys | ForEach-Object { Write-Host "- missing in infrastructure/env/frontend.env: $_" }
    $missingFrontendProdKeys | ForEach-Object { Write-Host "- missing in infrastructure/env/frontend.prod.example.env: $_" }
}
Write-Host ""

Write-Host "Backend Template Keys"
Write-Host "---------------------"
if ($missingBackendProdKeys.Count -eq 0 -and $missingBackendLocalKeys.Count -eq 0) {
    Write-Host "- backend env templates include expected core keys"
}
else {
    $missingBackendLocalKeys | ForEach-Object { Write-Host "- missing in infrastructure/env/backend.env: $_" }
    $missingBackendProdKeys | ForEach-Object { Write-Host "- missing in infrastructure/env/backend.prod.example.env: $_" }
}
Write-Host ""

Write-Host "Notes"
Write-Host "-----"
Write-Host "- This script is informational only."
Write-Host "- It does not validate live secrets."
Write-Host "- It checks template and reference posture, not runtime connectivity."
