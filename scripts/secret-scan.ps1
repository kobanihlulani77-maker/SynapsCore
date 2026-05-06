param(
    [switch]$Json
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function New-Finding {
    param(
        [string]$Severity,
        [string]$Category,
        [string]$Rule,
        [string]$Path
    )

    [PSCustomObject]@{
        severity = $Severity
        category = $Category
        rule = $Rule
        path = $Path
    }
}

function Get-TrackedFiles {
    $output = & git -C $repoRoot ls-files
    if ($LASTEXITCODE -ne 0) {
        throw "git ls-files failed in $repoRoot"
    }
    return $output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
}

function Add-ContentFindings {
    param(
        [System.Collections.Generic.List[object]]$Bucket,
        [string[]]$Files,
        [string]$Severity,
        [string]$Category,
        [string]$Rule,
        [string]$Pattern
    )

    foreach ($file in $Files) {
        $absolutePath = Join-Path $repoRoot $file
        if (-not (Test-Path -LiteralPath $absolutePath)) {
            continue
        }

        $content = Get-Content -LiteralPath $absolutePath -Raw -ErrorAction SilentlyContinue
        if ($null -ne $content -and $content -match $Pattern) {
            $Bucket.Add((New-Finding -Severity $Severity -Category $Category -Rule $Rule -Path $file))
        }
    }
}

$trackedFiles = @(Get-TrackedFiles)
$criticalFindings = [System.Collections.Generic.List[object]]::new()
$fixtureFindings = [System.Collections.Generic.List[object]]::new()
$notes = [System.Collections.Generic.List[string]]::new()

$trackedEnvFiles = $trackedFiles | Where-Object { $_ -match '(^|/)\.env($|\.)' }
foreach ($file in $trackedEnvFiles) {
    $criticalFindings.Add((New-Finding -Severity "critical" -Category "tracked-files" -Rule "Tracked env file" -Path $file))
}

$trackedPlaywrightArtifacts = $trackedFiles | Where-Object { $_ -match '^frontend/(playwright-report|test-results)/' }
foreach ($file in $trackedPlaywrightArtifacts) {
    $criticalFindings.Add((New-Finding -Severity "critical" -Category "tracked-files" -Rule "Tracked Playwright artifact" -Path $file))
}

$outwardFacingFiles = $trackedFiles | Where-Object {
    $_ -eq "README.md" -or $_ -like "docs/*" -or $_ -like "scripts/*"
}

Add-ContentFindings -Bucket $criticalFindings `
    -Files $outwardFacingFiles `
    -Severity "critical" `
    -Category "outward-docs" `
    -Rule "Hosted proof password literal in outward-facing docs or scripts" `
    -Pattern 'Admin@123|Planner@123|Integration@123'

Add-ContentFindings -Bucket $criticalFindings `
    -Files $outwardFacingFiles `
    -Severity "critical" `
    -Category "outward-docs" `
    -Rule "Bootstrap or platform-admin token literal in outward-facing docs or scripts" `
    -Pattern 'bootstrap-secret|platform-admin-secret'

Add-ContentFindings -Bucket $criticalFindings `
    -Files $outwardFacingFiles `
    -Severity "critical" `
    -Category "outward-docs" `
    -Rule "Committed proof password env assignment in outward-facing docs or scripts" `
    -Pattern 'PLAYWRIGHT_(TENANT_ADMIN|PLANNER|INTEGRATION_ADMIN)_PASSWORD\s*=\s*["''](?!<)[^"'']+["'']'

$fixtureFiles = $trackedFiles | Where-Object {
    $_ -eq "backend/src/main/java/com/synapsecore/auth/StarterAccessUsers.java" -or $_ -like "backend/src/test/*"
}

Add-ContentFindings -Bucket $fixtureFindings `
    -Files $fixtureFiles `
    -Severity "fixture" `
    -Category "fixture-credentials" `
    -Rule "Committed starter or test credential literal" `
    -Pattern 'Admin@123|lead-2026|planner-2026|integration-admin-2026|bootstrap-secret|platform-admin-secret'

$distPath = Join-Path $repoRoot "frontend\\dist"
if (Test-Path -LiteralPath $distPath) {
    $distFiles = Get-ChildItem -Path $distPath -Recurse -File | ForEach-Object {
        $_.FullName.Substring($repoRoot.Length + 1).Replace('\', '/')
    }

    Add-ContentFindings -Bucket $criticalFindings `
        -Files $distFiles `
        -Severity "critical" `
        -Category "frontend-bundle" `
        -Rule "Frontend bundle contains explicit platform token or proof password literal" `
        -Pattern 'bootstrap-secret|platform-admin-secret|Admin@123|Planner@123|Integration@123'
} else {
    $notes.Add("frontend/dist not present at scan time; build before bundle-level verification if needed.")
}

$result = [PSCustomObject]@{
    repoRoot = $repoRoot
    scannedTrackedFiles = $trackedFiles.Count
    criticalFindings = @($criticalFindings | Sort-Object category, path, rule -Unique)
    fixtureFindings = @($fixtureFindings | Sort-Object category, path, rule -Unique)
    notes = @($notes)
    status = if ($criticalFindings.Count -gt 0) { "FAIL" } else { "PASS" }
}

if ($Json) {
    $result | ConvertTo-Json -Depth 6
} else {
    Write-Host "SynapseCore Secret Scan"
    Write-Host "Repository: $($result.repoRoot)"
    Write-Host "Tracked files scanned: $($result.scannedTrackedFiles)"
    Write-Host "Critical findings: $($result.criticalFindings.Count)"
    Write-Host "Fixture findings: $($result.fixtureFindings.Count)"
    Write-Host "Status: $($result.status)"

    if ($result.criticalFindings.Count -gt 0) {
        Write-Host ""
        Write-Host "Critical findings:"
        $result.criticalFindings | ForEach-Object {
            Write-Host ("- [{0}] {1}: {2}" -f $_.category, $_.rule, $_.path)
        }
    }

    if ($result.fixtureFindings.Count -gt 0) {
        Write-Host ""
        Write-Host "Fixture findings:"
        $result.fixtureFindings | ForEach-Object {
            Write-Host ("- [{0}] {1}: {2}" -f $_.category, $_.rule, $_.path)
        }
    }

    if ($result.notes.Count -gt 0) {
        Write-Host ""
        Write-Host "Notes:"
        $result.notes | ForEach-Object { Write-Host "- $_" }
    }
}

if ($criticalFindings.Count -gt 0) {
    exit 1
}
