param(
    [string]$JavaBin = ''
)
$ErrorActionPreference = 'Stop'
$projectDir = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$outputDir = Join-Path $projectDir 'build/audit-2026-09-08/fixes/persistence'
$classesDir = Join-Path $outputDir 'classes'
New-Item -ItemType Directory -Path $classesDir -Force | Out-Null

$javacCommand = if ($JavaBin) { Join-Path $JavaBin 'javac.exe' } else { (Get-Command javac).Source }
$javaCommand = if ($JavaBin) { Join-Path $JavaBin 'java.exe' } else { (Get-Command java).Source }
$productionClasses = @(
    'build/classes/java/client/de/ottoextra/config/OttoExtraConfig.class',
    'build/classes/java/client/de/ottoextra/rpnames/store/LocalRpIdentityStore.class'
) | ForEach-Object { (Resolve-Path -LiteralPath (Join-Path $projectDir $_)).Path }
$productionSources = @(
    'src/client/java/de/ottoextra/config/OttoExtraConfig.java',
    'src/client/java/de/ottoextra/rpnames/store/LocalRpIdentityStore.java'
) | ForEach-Object { (Resolve-Path -LiteralPath (Join-Path $projectDir $_)).Path }
for ($index = 0; $index -lt $productionClasses.Count; $index++) {
    if ((Get-Item -LiteralPath $productionSources[$index]).LastWriteTimeUtc -gt
        (Get-Item -LiteralPath $productionClasses[$index]).LastWriteTimeUtc) {
        throw 'Produktklassen sind aelter als die Quellen; bitte zuerst den bestehenden Projekt-Build ausfuehren.'
    }
}

$loomCache = Join-Path $projectDir '.gradle/loom-cache'
$launchConfig = Get-Content -LiteralPath (Join-Path $loomCache 'launch.cfg')
$namedMinecraftJars = $launchConfig | Where-Object { $_ -match '^\s*fabric\.gameJarPath(?:\.client)?=' } |
    ForEach-Object { ($_ -split '=', 2)[1] }
if ($namedMinecraftJars.Count -ne 2) { throw 'Benannte Minecraft-Client/Common-JARs fehlen in der lokalen Loom-Konfiguration.' }
$mappedModJars = $launchConfig | Where-Object { $_ -match '^\s*fabric\.classPathGroups=' } |
    ForEach-Object { (($_ -split '=', 2)[1]) -split [IO.Path]::PathSeparator } |
    Where-Object { $_.EndsWith('.jar') }
$remapEntries = (Get-Content -LiteralPath (Join-Path $loomCache 'remapClasspath.txt') -Raw).Trim() -split [IO.Path]::PathSeparator
$dependencyJars = @($namedMinecraftJars + $mappedModJars + $remapEntries) |
    Where-Object { $_ -and $_ -notmatch 'minecraft-(common|clientonly)-intermediary' } |
    Select-Object -Unique
foreach ($dependencyJar in $dependencyJars) {
    if (!(Test-Path -LiteralPath $dependencyJar)) { throw "Lokale Test-Abhaengigkeit fehlt: $dependencyJar" }
}
$productClasspath = (@(
    (Join-Path $projectDir 'build/classes/java/client'),
    (Join-Path $projectDir 'build/classes/java/main')
) + $dependencyJars) -join [IO.Path]::PathSeparator

$compileArgs = Join-Path $outputDir 'javac.args'
@(
    '--release', '21', '-encoding', 'UTF-8', '-proc:none', '-cp', ('"' + $productClasspath.Replace('\', '/') + '"'),
    '-d', ('"' + $classesDir.Replace('\', '/') + '"'),
    ('"' + (Join-Path $PSScriptRoot 'PersistenceProbe.java').Replace('\', '/') + '"')
) | Set-Content -LiteralPath $compileArgs -Encoding utf8
& $javacCommand "@$compileArgs" 2>&1 |
    Tee-Object -FilePath (Join-Path $outputDir 'compile.log')
if ($LASTEXITCODE -ne 0) { throw 'Kompilierung des Persistenz-Nachweises fehlgeschlagen.' }

Get-FileHash -LiteralPath ($productionSources + $productionClasses + $dependencyJars) -Algorithm SHA256 |
    Select-Object Path, Hash |
    ConvertTo-Json |
    Set-Content -LiteralPath (Join-Path $outputDir 'source-class-dependency-sha256.json') -Encoding utf8

$runtimeArgs = Join-Path $outputDir 'java.args'
@(
    '--class-path', ('"' + ($classesDir + [IO.Path]::PathSeparator + $productClasspath).Replace('\', '/') + '"'),
    'de.ottoextra.rpnames.store.PersistenceProbe', ('"' + $outputDir.Replace('\', '/') + '"')
) | Set-Content -LiteralPath $runtimeArgs -Encoding utf8
Push-Location -LiteralPath $outputDir
try {
    & $javaCommand "@$runtimeArgs" 2>&1 | Tee-Object -FilePath (Join-Path $outputDir 'result.log')
    if ($LASTEXITCODE -ne 0) { throw 'Persistenz-Regression fehlgeschlagen; Ergebnisprotokoll pruefen.' }
} finally {
    Pop-Location
}
