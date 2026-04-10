param(
    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "ProdEnvTools.ps1")

$rootDir = Get-SynapseRoot -ScriptPath $PSCommandPath
$envDir = Join-Path $rootDir "infrastructure\env"

$backendSource = Join-Path $envDir "backend.prod.example.env"
$frontendSource = Join-Path $envDir "frontend.prod.example.env"
$edgeSource = Join-Path $envDir "edge.prod.example.env"
$backendTarget = Join-Path $envDir "backend.prod.env"
$frontendTarget = Join-Path $envDir "frontend.prod.env"
$edgeTarget = Join-Path $envDir "edge.prod.env"

function Copy-Template {
    param(
        [string]$SourceFile,
        [string]$TargetFile,
        [string]$Label
    )

    if ((Test-Path -LiteralPath $TargetFile) -and -not $Force) {
        Write-Host "$Label already exists: $TargetFile"
        return
    }

    Copy-Item -LiteralPath $SourceFile -Destination $TargetFile -Force
    Write-Host "Prepared ${Label}: $TargetFile"
}

Copy-Template -SourceFile $backendSource -TargetFile $backendTarget -Label "backend prod env"
Copy-Template -SourceFile $frontendSource -TargetFile $frontendTarget -Label "frontend prod env"
Copy-Template -SourceFile $edgeSource -TargetFile $edgeTarget -Label "edge prod env"

Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Edit $backendTarget"
Write-Host "2. Edit $frontendTarget"
Write-Host "3. Edit $edgeTarget"
Write-Host "4. Run: powershell -ExecutionPolicy Bypass -File scripts\\release-readiness.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env"
Write-Host "5. Self-hosted deploy: powershell -ExecutionPolicy Bypass -File scripts\\start-prod.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env"
Write-Host "6. Public deploy with Caddy: powershell -ExecutionPolicy Bypass -File scripts\\start-public-prod.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env -EdgeEnvFile ./env/edge.prod.env"
