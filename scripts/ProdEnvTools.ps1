Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-SynapseRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptPath
    )

    return Split-Path -Parent (Split-Path -Parent $ScriptPath)
}

function Resolve-SynapseEnvPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$InfrastructureDir,
        [Parameter(Mandatory = $true)]
        [string]$RawPath
    )

    if ([System.IO.Path]::IsPathRooted($RawPath)) {
        return $RawPath
    }

    $relativePath = $RawPath -replace '^[.][\\/]', ''
    return Join-Path $InfrastructureDir $relativePath
}

function Read-SynapseEnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath
    )

    if (-not (Test-Path -LiteralPath $FilePath)) {
        throw "Missing env file: $FilePath"
    }

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $FilePath) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith('#')) {
            continue
        }

        $separatorIndex = $trimmed.IndexOf('=')
        if ($separatorIndex -lt 1) {
            continue
        }

        $key = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1)
        $values[$key] = $value
    }

    return $values
}

function Get-RequiredEnvValue {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$Values,
        [Parameter(Mandatory = $true)]
        [string]$Key,
        [Parameter(Mandatory = $true)]
        [string]$FilePath
    )

    if (-not $Values.ContainsKey($Key) -or [string]::IsNullOrWhiteSpace([string]$Values[$Key])) {
        throw "Missing required key $Key in $FilePath"
    }

    return [string]$Values[$Key]
}

function Assert-EnvEquals {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$Values,
        [Parameter(Mandatory = $true)]
        [string]$Key,
        [Parameter(Mandatory = $true)]
        [string]$Expected,
        [Parameter(Mandatory = $true)]
        [string]$FilePath
    )

    $actual = Get-RequiredEnvValue -Values $Values -Key $Key -FilePath $FilePath
    if ($actual -ne $Expected) {
        throw "Expected $Key=$Expected in $FilePath but found $actual"
    }
}

function Test-SynapsePlaceholderValues {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Values
    )

    foreach ($value in $Values) {
        if ($value -match 'change-me|example\.com|example\.internal|set-at-release') {
            return $true
        }
    }

    return $false
}

function Invoke-SynapseEndpointCheck {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$MaxAttempts = 20,
        [int]$SleepSeconds = 5
    )

    Write-Host "Checking $Label -> $Url"
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing -TimeoutSec 30 | Out-Null
            return
        }
        catch {
            if ($attempt -eq $MaxAttempts) {
                throw "Failed to verify $Label after $MaxAttempts attempts. $($_.Exception.Message)"
            }

            Write-Host "  attempt $attempt/$MaxAttempts did not pass yet; retrying in ${SleepSeconds}s..."
            Start-Sleep -Seconds $SleepSeconds
        }
    }
}
