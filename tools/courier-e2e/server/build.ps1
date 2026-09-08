$ErrorActionPreference = 'Stop'
$sourceRoot = $PSScriptRoot
& (Join-Path $sourceRoot 'prepare.ps1')
$projectRoot = (Resolve-Path (Join-Path $sourceRoot '../../..')).Path
$runtimeRoot = Join-Path $projectRoot 'build/courier-e2e/server'
$javaBin = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin'
$classes = Join-Path $runtimeRoot 'helper-classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$classpath = (@(Get-ChildItem -LiteralPath (Join-Path $runtimeRoot 'libraries') -Recurse -Filter '*.jar') | ForEach-Object FullName) -join ';'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $sourceRoot 'src'), (Join-Path $sourceRoot 'vendor') -Recurse -Filter '*.java') | ForEach-Object FullName
$arguments = @('--release', '21', '-proc:none', '-encoding', 'UTF-8', '-classpath', ('"' + $classpath.Replace('\','/') + '"'), '-d', ('"' + $classes.Replace('\','/') + '"'))
$arguments += $sources | ForEach-Object { '"' + $_.Replace('\','/') + '"' }
$argumentFile = Join-Path $runtimeRoot 'helper-javac.args'
[IO.File]::WriteAllLines($argumentFile, $arguments, [Text.UTF8Encoding]::new($false))
& (Join-Path $javaBin 'javac.exe') "@$argumentFile"
if ($LASTEXITCODE -ne 0) { throw 'Helper compilation failed' }
# Courier verifies JDA types on the ordinary announcement path even when Discord is disabled.
# Expose the genuine runtime via its declared Vault dependency; never alter the supplied Courier JAR.
Push-Location -LiteralPath $classes
try {
    & (Join-Path $javaBin 'jar.exe') --extract --file (Join-Path $runtimeRoot 'dependencies/JDA-5.6.1.jar') net
    if ($LASTEXITCODE -ne 0) { throw 'JDA runtime extraction failed' }
} finally { Pop-Location }
Copy-Item -LiteralPath (Join-Path $sourceRoot 'plugin.yml') -Destination $classes -Force
Copy-Item -LiteralPath (Join-Path $sourceRoot 'vendor/JDA-LICENSE.txt') -Destination $classes -Force
& (Join-Path $javaBin 'jar.exe') --create --file (Join-Path $runtimeRoot 'plugins/Vault-CourierE2E.jar') -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Helper packaging failed' }
