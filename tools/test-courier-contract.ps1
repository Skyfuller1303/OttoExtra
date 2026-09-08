param(
    [string]$CourierJar = (Join-Path $PSScriptRoot '../../input/courier-2.1.0.jar'),
    [string]$DependencyCache = (Join-Path $env:USERPROFILE '.gradle/caches/modules-2/files-2.1'),
    [switch]$SkipCompile
)
$ErrorActionPreference = 'Stop'
$projectDir = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$courierPath = (Resolve-Path -LiteralPath $CourierJar).Path
$expectedHash = '9310a0acaeaed0123a8fd2007502065090c0864dbfe277e779d266601f789b4f'
if ((Get-FileHash -LiteralPath $courierPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {
    throw 'Courier-JAR weicht von der auditierten 2.1.0 ab; Referenz zuerst erneut prüfen.'
}
$dependencyRoots = @(
    'io.papermc.paper/paper-api/1.21.11-R0.1-SNAPSHOT',
    'net.kyori', 'net.md-5', 'com.google.guava/guava/33.6.0-jre',
    'com.google.code.gson/gson/2.14.0', 'org.yaml/snakeyaml'
)
$dependencyJars = foreach ($relative in $dependencyRoots) {
    $dependencyPath = Join-Path $DependencyCache $relative
    if (!(Test-Path -LiteralPath $dependencyPath)) { throw "Lokale Test-Abhängigkeit fehlt: $dependencyPath" }
    Get-ChildItem -LiteralPath $dependencyPath -Recurse -File -Filter '*.jar' |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
        Select-Object -ExpandProperty FullName
}
Push-Location -LiteralPath $projectDir
try {
    if (!$SkipCompile) {
        & .\gradlew.bat clientClasses --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Client-Kompilierung fehlgeschlagen.' }
    }
    $contractClasspath = (@($courierPath, (Join-Path $projectDir 'build/classes/java/client')) + $dependencyJars) -join [IO.Path]::PathSeparator
    & java --class-path $contractClasspath (Join-Path $PSScriptRoot 'courier/CourierContractCheck.java')
    if ($LASTEXITCODE -ne 0) { throw 'Courier-Vertragsprüfung fehlgeschlagen.' }
} finally {
    Pop-Location
}
