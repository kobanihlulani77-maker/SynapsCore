Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
$docFiles = @(
    (Join-Path $rootDir "README.md")
) + @(Get-ChildItem -LiteralPath (Join-Path $rootDir "docs") -Filter *.md -File -Recurse | Select-Object -ExpandProperty FullName)

$missingLinks = @()
$checkedLinks = 0
$linkPattern = '\[[^\]]+\]\(([^)]+)\)'

foreach ($file in $docFiles) {
    $content = Get-Content -LiteralPath $file -Raw
    $matches = [regex]::Matches($content, $linkPattern)
    foreach ($match in $matches) {
        $target = $match.Groups[1].Value.Trim()
        if ([string]::IsNullOrWhiteSpace($target)) {
            continue
        }
        if ($target.StartsWith("http://") -or $target.StartsWith("https://") -or $target.StartsWith("mailto:") -or $target.StartsWith("#")) {
            continue
        }

        $relativeTarget = $target.Split("#")[0]
        if ([string]::IsNullOrWhiteSpace($relativeTarget)) {
            continue
        }

        $resolved = [System.IO.Path]::GetFullPath((Join-Path (Split-Path -Parent $file) $relativeTarget))
        $checkedLinks++
        if (-not (Test-Path -LiteralPath $resolved)) {
            $missingLinks += [pscustomobject]@{
                File = $file.Replace($rootDir + "\", "")
                Target = $target
            }
        }
    }
}

$classification = if ($missingLinks.Count -eq 0) { "CLEAN" } else { "NEEDS_ATTENTION" }

Write-Host "=================================================="
Write-Host "SYNAPSECORE DOCS LINK CHECK"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
Write-Host "Classification"
Write-Host "--------------"
Write-Host $classification
Write-Host ""
Write-Host "Checked Links"
Write-Host "-------------"
Write-Host $checkedLinks
Write-Host ""

Write-Host "Missing Local Links"
Write-Host "-------------------"
if ($missingLinks.Count -eq 0) {
    Write-Host "- none"
}
else {
    $missingLinks | ForEach-Object {
        Write-Host "- $($_.File) -> $($_.Target)"
    }
}
Write-Host ""
Write-Host "Notes"
Write-Host "-----"
Write-Host "- This script checks local markdown links only."
Write-Host "- Web URLs and anchor-only links are ignored."
