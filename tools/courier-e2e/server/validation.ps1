function Assert-CourierE2EProperties([string] $PropertiesPath) {
    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $PropertiesPath) {
        if ($line -match '^\s*(#|!|$)') { continue }
        # Paper writes canonical key=value properties. Reject ambiguous alternate encodings
        # instead of interpreting an escaped or continued key differently from Java.
        if ($line -notmatch '^([^=:\s\\]+)\s*=(.*)$' -or $line.EndsWith('\')) {
            throw "Expected an unambiguous key=value property in $PropertiesPath : $line"
        }
        $key = $Matches[1]
        $value = $Matches[2].Trim()
        if ($properties.ContainsKey($key)) { throw "Duplicate property '$key' in $PropertiesPath" }
        $properties[$key] = $value
    }
    $required = @{
        'server-ip' = '127.0.0.1'
        'server-port' = '25579'
        'online-mode' = 'false'
        'enable-rcon' = 'false'
        'enable-query' = 'false'
    }
    foreach ($key in $required.Keys) {
        if (-not $properties.ContainsKey($key) -or $properties[$key] -cne $required[$key]) {
            throw "Isolated server requires $key=$($required[$key]) in $PropertiesPath; found '$($properties[$key])'"
        }
    }
}

function Assert-CourierE2ECoreFiles([string] $ServerRoot) {
    foreach ($relative in @('paper-1.21.11-132.jar', 'cache/mojang_1.21.11.jar', 'versions/1.21.11/paper-1.21.11.jar')) {
        $path = Join-Path $ServerRoot $relative
        if (-not (Test-Path -LiteralPath $path -PathType Leaf) -or (Get-Item -LiteralPath $path).Length -eq 0) {
            throw "Incomplete Paper cache: required nonempty JAR missing at $path. Restore this file from the existing local Paper 1.21.11 build 132 server before retrying."
        }
    }
    $apiRoot = Join-Path $ServerRoot 'libraries/io/papermc/paper/paper-api'
    $api = @(Get-ChildItem -LiteralPath $apiRoot -Recurse -Filter '*.jar' -File -ErrorAction SilentlyContinue)
    if (-not ($api | Where-Object Length -gt 0)) {
        throw "Incomplete Paper libraries: no paper-api JAR in $apiRoot. Restore the complete libraries directory from the existing local server before retrying."
    }
}
