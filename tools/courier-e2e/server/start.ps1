$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'validation.ps1')
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$runtimeRoot = Join-Path $projectRoot 'build/courier-e2e/server'
$javaExe = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe'
$expectedCourierHash = '9310A0ACAEAED0123A8FD2007502065090C0864DBFE277E779D266601F789B4F'
if ((Get-FileHash -LiteralPath (Join-Path $runtimeRoot 'plugins/Courier-2.1.0.jar') -Algorithm SHA256).Hash -ne $expectedCourierHash) { throw 'Courier JAR changed' }
if (-not ((Get-Content -LiteralPath (Join-Path $runtimeRoot 'eula.txt')) -match '^eula=true$')) { throw 'No existing accepted EULA copied into isolated server' }
Assert-CourierE2EProperties (Join-Path $runtimeRoot 'server.properties')
Assert-CourierE2ECoreFiles $runtimeRoot
foreach ($port in @(25579,25580)) { if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "Port $port already in use" } }
$process = Start-Process -FilePath $javaExe -ArgumentList @('-Xms512M','-Xmx2G','-jar','paper-1.21.11-132.jar','--nogui') -WorkingDirectory $runtimeRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeRoot 'console.log') -RedirectStandardError (Join-Path $runtimeRoot 'console.err.log')
$process.Id | Set-Content -LiteralPath (Join-Path $runtimeRoot 'server.pid')
Write-Output "Server PID $($process.Id), Minecraft 127.0.0.1:25579, HTTP 127.0.0.1:25580"
