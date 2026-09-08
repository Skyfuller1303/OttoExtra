param(
    [string]$JavaBin = ''
)
$ErrorActionPreference = 'Stop'
$projectDir = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$outputDir = Join-Path $projectDir 'build/audit-2026-09-08/fixes/long-chat-sender'
$classesDir = Join-Path $outputDir 'classes'
New-Item -ItemType Directory -Path $classesDir -Force | Out-Null

$javacCommand = if ($JavaBin) { Join-Path $JavaBin 'javac.exe' } else { (Get-Command javac).Source }
$javaCommand = if ($JavaBin) { Join-Path $JavaBin 'java.exe' } else { (Get-Command java).Source }
$productionSources = @(
    'src/client/java/de/ottoextra/chat/LongChatSender.java',
    'src/client/java/de/ottoextra/chat/LongChatSplitter.java',
    'src/client/java/de/ottoextra/chat/RpChatSyntax.java'
) | ForEach-Object { (Resolve-Path -LiteralPath (Join-Path $projectDir $_)).Path }
$probeSources = @(
    'LongChatSenderProbe.java',
    'long-chat-stubs/net/minecraft/client/MinecraftClient.java',
    'long-chat-stubs/net/minecraft/client/network/ClientPlayNetworkHandler.java'
) | ForEach-Object { (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot $_)).Path }

& $javacCommand --release 21 -encoding UTF-8 -d $classesDir @productionSources @probeSources 2>&1 |
    Tee-Object -FilePath (Join-Path $outputDir 'compile.log')
if ($LASTEXITCODE -ne 0) { throw 'Kompilierung des LongChatSender-Nachweises fehlgeschlagen.' }

Get-FileHash -LiteralPath $productionSources -Algorithm SHA256 |
    Select-Object Path, Hash |
    ConvertTo-Json |
    Set-Content -LiteralPath (Join-Path $outputDir 'source-sha256.json') -Encoding utf8

& $javaCommand --class-path $classesDir LongChatSenderProbe 2>&1 |
    Tee-Object -FilePath (Join-Path $outputDir 'result.log')
if ($LASTEXITCODE -ne 0) { throw 'LongChatSender-Regression fehlgeschlagen; Ergebnisprotokoll pruefen.' }
