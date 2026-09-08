$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'validation.ps1')
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$workspaceRoot = (Resolve-Path (Join-Path $projectRoot '..')).Path
$runtimeRoot = Join-Path $projectRoot 'build/courier-e2e/server'
$paperSource = Join-Path $workspaceRoot 'OttonienItems-local-server'
$courierSource = Join-Path $workspaceRoot 'input/courier-2.1.0.jar'
$courierHash = '9310A0ACAEAED0123A8FD2007502065090C0864DBFE277E779D266601F789B4F'
if ((Get-FileHash -LiteralPath $courierSource -Algorithm SHA256).Hash -ne $courierHash) { throw 'Unexpected original Courier JAR' }
if (-not ((Get-Content -LiteralPath (Join-Path $paperSource 'eula.txt')) -match '^eula=true$')) { throw 'No previously accepted local EULA available; not creating a new acceptance' }
Assert-CourierE2ECoreFiles $paperSource
New-Item -ItemType Directory -Force -Path $runtimeRoot, (Join-Path $runtimeRoot 'plugins/Courier'), (Join-Path $runtimeRoot 'reports') | Out-Null
foreach ($folder in @('libraries','cache','versions')) {
    if (-not (Test-Path -LiteralPath (Join-Path $runtimeRoot $folder))) {
        Copy-Item -LiteralPath (Join-Path $paperSource $folder) -Destination $runtimeRoot -Recurse
    }
}
foreach ($file in @('paper-1.21.11-132.jar','eula.txt')) {
    if (-not (Test-Path -LiteralPath (Join-Path $runtimeRoot $file))) {
        Copy-Item -LiteralPath (Join-Path $paperSource $file) -Destination $runtimeRoot
    }
}
Assert-CourierE2ECoreFiles $runtimeRoot
$courierDestination = Join-Path $runtimeRoot 'plugins/Courier-2.1.0.jar'
if (-not (Test-Path -LiteralPath $courierDestination)) { Copy-Item -LiteralPath $courierSource -Destination $courierDestination }
if ((Get-FileHash -LiteralPath $courierDestination -Algorithm SHA256).Hash -ne $courierHash) { throw 'Isolated Courier copy differs from the supplied original' }
if (-not (Test-Path -LiteralPath (Join-Path $runtimeRoot 'server.properties'))) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'server.properties') -Destination $runtimeRoot
}
if (-not (Test-Path -LiteralPath (Join-Path $runtimeRoot 'plugins/Courier/config.yml'))) {
    Copy-Item -LiteralPath (Join-Path $workspaceRoot 'input/Courier-main/Courier-main/src/main/resources/config.yml') -Destination (Join-Path $runtimeRoot 'plugins/Courier/config.yml')
}
$jdaVersion = '5.6.1'
$jdaSha256 = '9230BA05BDB4BD1027263DD8F1E4727630DF25521061563A2ED4C93527CB06F5'
$jdaUrl = "https://github.com/discord-jda/JDA/releases/download/v$jdaVersion/JDA-$jdaVersion.jar"
$jdaJar = Join-Path $runtimeRoot "dependencies/JDA-$jdaVersion.jar"
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $jdaJar) | Out-Null
if (-not (Test-Path -LiteralPath $jdaJar)) { Invoke-WebRequest -Uri $jdaUrl -OutFile $jdaJar }
if ((Get-FileHash -LiteralPath $jdaJar -Algorithm SHA256).Hash -ne $jdaSha256) { throw 'Unexpected JDA runtime artifact' }
$provenance = [ordered]@{
    createdAt = (Get-Date).ToUniversalTime().ToString('o')
    courierSource = $courierSource
    courierSha256 = $courierHash
    paperSource = (Join-Path $paperSource 'paper-1.21.11-132.jar')
    paperSha256 = (Get-FileHash -LiteralPath (Join-Path $runtimeRoot 'paper-1.21.11-132.jar') -Algorithm SHA256).Hash
    eulaSource = (Join-Path $paperSource 'eula.txt')
    eulaEvidence = 'Existing user acceptance dated 2026-07-26; copied verbatim'
    vaultApiRevision = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'vendor/VAULTAPI-REVISION.txt') -Raw).Trim()
    testMaterialOverride = 'letter-item.MythicItem removed; letter-item.ItemStack = PAPER with display name Pergament und Feder'
    economy = 'Authentic VaultAPI Economy/EconomyResponse types; deterministic test provider hosted by test-only plugin named Vault'
    jdaVersion = $jdaVersion
    jdaSource = $jdaUrl
    jdaSha256 = $jdaSha256
    jdaReason = 'Courier Announcement class verification requires JDA even with Discord disabled; real unmodified JDA classes embedded in test helper only'
    remoteServices = 'Discord disabled; Minecraft and control HTTP bind loopback only'
}
$provenance | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $runtimeRoot 'reports/provenance.json') -Encoding utf8
Write-Output "Prepared isolated server at $runtimeRoot"
