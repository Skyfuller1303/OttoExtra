param(
    [string]$Filter = '',
    [switch]$KeepServer
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$healthUrl = 'http://127.0.0.1:25580/health'
$startedServer = $false
$logRoot = Join-Path $projectRoot 'build/courier-e2e'
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null
function Read-Health {
    try { Invoke-RestMethod -Uri $healthUrl -TimeoutSec 3 } catch { $null }
}
Push-Location -LiteralPath $projectRoot
try {
    $health = Read-Health
    if (-not $health) {
        & (Join-Path $PSScriptRoot 'server/build.ps1')
        & (Join-Path $PSScriptRoot 'server/start.ps1')
        $startedServer = $true
        $deadline = [DateTime]::UtcNow.AddSeconds(90)
        do {
            Start-Sleep -Milliseconds 500
            $health = Read-Health
        } until ($health -or [DateTime]::UtcNow -gt $deadline)
    }
    if (-not $health.ok -or $health.courier -ne '2.1.0') { throw 'Expected local Courier 2.1.0 test server unavailable' }
    $logFile = Join-Path $logRoot ('client-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
    $gradleArgs = @('-I', 'tools/courier-e2e/client.init.gradle', 'runClientGameTest', '-x', 'dist', '--console=plain')
    if ($Filter) { $gradleArgs += "-PcourierE2eFilter=$Filter" }
    & ./gradlew.bat @gradleArgs 2>&1 | Tee-Object -FilePath $logFile
    if ($LASTEXITCODE -ne 0) { throw "Courier E2E failed; see $logFile and build/courier-e2e/results/results.json" }
    Write-Output 'Courier E2E successful. Results: build/courier-e2e/results/results.json'
} finally {
    if ($startedServer -and -not $KeepServer) {
        try { Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:25580/stop' -TimeoutSec 5 | Out-Null } catch { Write-Warning $_ }
    }
    Pop-Location
}
