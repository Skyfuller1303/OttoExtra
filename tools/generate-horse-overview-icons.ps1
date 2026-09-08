param(
    [string] $OutputRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\ottoextra\textures\gui\horse_overview'),
    [string[]] $Names = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputRoot)
[System.IO.Directory]::CreateDirectory($resolvedOutput) | Out-Null

function Add-PixelRect {
    param(
        [System.Drawing.Graphics] $Graphics,
        [System.Drawing.Brush] $Brush,
        [int] $X,
        [int] $Y,
        [int] $Width,
        [int] $Height
    )
    $Graphics.FillRectangle($Brush, $X, $Y, $Width, $Height)
}

function Write-PixelIcon {
    param(
        [string] $Name,
        [scriptblock] $Paint
    )
    if ($Names.Count -gt 0 -and $Name -notin $Names) { return }
    $bitmap = [System.Drawing.Bitmap]::new(
        16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        & $Paint $graphics $brush
        $target = Join-Path $resolvedOutput ($Name + '.png')
        $bitmap.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $brush.Dispose()
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

Write-PixelIcon 'finder' {
    param($g, $b)
    Add-PixelRect $g $b 4 2 6 2
    Add-PixelRect $g $b 2 4 2 6
    Add-PixelRect $g $b 10 4 2 6
    Add-PixelRect $g $b 4 10 6 2
    Add-PixelRect $g $b 10 10 2 2
    Add-PixelRect $g $b 11 11 2 2
    Add-PixelRect $g $b 12 12 3 3
}

Write-PixelIcon 'delete' {
    param($g, $b)
    Add-PixelRect $g $b 3 3 10 2
    Add-PixelRect $g $b 6 1 4 2
    Add-PixelRect $g $b 4 6 2 8
    Add-PixelRect $g $b 7 6 2 8
    Add-PixelRect $g $b 10 6 2 8
    Add-PixelRect $g $b 4 14 8 1
}

Write-PixelIcon 'gender_mare' {
    param($g, $b)
    Add-PixelRect $g $b 4 1 6 2
    Add-PixelRect $g $b 2 3 2 6
    Add-PixelRect $g $b 10 3 2 6
    Add-PixelRect $g $b 4 9 6 2
    Add-PixelRect $g $b 6 11 2 4
    Add-PixelRect $g $b 4 13 6 2
}

Write-PixelIcon 'gender_stallion' {
    param($g, $b)
    Add-PixelRect $g $b 3 4 6 2
    Add-PixelRect $g $b 1 6 2 6
    Add-PixelRect $g $b 9 6 2 6
    Add-PixelRect $g $b 3 12 6 2
    Add-PixelRect $g $b 9 3 5 2
    Add-PixelRect $g $b 12 3 2 5
    Add-PixelRect $g $b 9 6 3 2
}

Write-PixelIcon 'gender_unknown' {
    param($g, $b)
    Add-PixelRect $g $b 4 2 6 2
    Add-PixelRect $g $b 2 4 2 6
    Add-PixelRect $g $b 10 4 2 6
    Add-PixelRect $g $b 4 10 6 2
    Add-PixelRect $g $b 6 13 2 2
}

Write-PixelIcon 'speed' {
    param($g, $b)
    foreach ($offset in @(1, 7)) {
        Add-PixelRect $g $b $offset 4 2 2
        Add-PixelRect $g $b ($offset + 2) 6 2 2
        Add-PixelRect $g $b ($offset + 4) 8 2 2
        Add-PixelRect $g $b ($offset + 2) 10 2 2
        Add-PixelRect $g $b $offset 12 2 2
    }
}

Write-PixelIcon 'jump' {
    param($g, $b)
    Add-PixelRect $g $b 7 2 2 10
    Add-PixelRect $g $b 5 4 6 2
    Add-PixelRect $g $b 3 6 4 2
    Add-PixelRect $g $b 9 6 4 2
    Add-PixelRect $g $b 2 13 12 2
}

Write-PixelIcon 'sort_ascending' {
    param($g, $b)
    Add-PixelRect $g $b 7 2 2 12
    Add-PixelRect $g $b 5 4 6 2
    Add-PixelRect $g $b 3 6 2 2
    Add-PixelRect $g $b 11 6 2 2
}

Write-PixelIcon 'sort_descending' {
    param($g, $b)
    Add-PixelRect $g $b 7 2 2 12
    Add-PixelRect $g $b 5 10 6 2
    Add-PixelRect $g $b 3 8 2 2
    Add-PixelRect $g $b 11 8 2 2
}

Write-PixelIcon 'sort_none' {
    param($g, $b)
    Add-PixelRect $g $b 4 2 2 11
    Add-PixelRect $g $b 2 4 2 2
    Add-PixelRect $g $b 6 4 2 2
    Add-PixelRect $g $b 10 3 2 11
    Add-PixelRect $g $b 8 10 2 2
    Add-PixelRect $g $b 12 10 2 2
}

Write-PixelIcon 'expand' {
    param($g, $b)
    Add-PixelRect $g $b 4 2 2 2
    Add-PixelRect $g $b 6 4 2 2
    Add-PixelRect $g $b 8 6 2 4
    Add-PixelRect $g $b 6 10 2 2
    Add-PixelRect $g $b 4 12 2 2
}

Write-PixelIcon 'collapse' {
    param($g, $b)
    Add-PixelRect $g $b 2 4 2 2
    Add-PixelRect $g $b 4 6 2 2
    Add-PixelRect $g $b 6 8 4 2
    Add-PixelRect $g $b 10 6 2 2
    Add-PixelRect $g $b 12 4 2 2
}

Write-PixelIcon 'feeding' {
    param($g, $b)
    Add-PixelRect $g $b 6 2 2 12
    Add-PixelRect $g $b 8 0 4 2
    Add-PixelRect $g $b 2 2 4 2
    Add-PixelRect $g $b 8 4 4 2
    Add-PixelRect $g $b 2 6 4 2
    Add-PixelRect $g $b 8 8 4 2
    Add-PixelRect $g $b 4 14 6 2
}

Write-PixelIcon 'breed_ready' {
    param($g, $b)
    Add-PixelRect $g $b 2 2 4 2
    Add-PixelRect $g $b 10 2 4 2
    Add-PixelRect $g $b 0 4 16 4
    Add-PixelRect $g $b 2 8 12 2
    Add-PixelRect $g $b 4 10 8 2
    Add-PixelRect $g $b 6 12 4 2
}

Write-PixelIcon 'shearable' {
    param($g, $b)
    Add-PixelRect $g $b 2 0 2 4
    Add-PixelRect $g $b 12 0 2 4
    Add-PixelRect $g $b 4 4 2 2
    Add-PixelRect $g $b 10 4 2 2
    Add-PixelRect $g $b 6 6 4 2
    Add-PixelRect $g $b 4 8 2 2
    Add-PixelRect $g $b 10 8 2 2
    Add-PixelRect $g $b 0 10 6 2
    Add-PixelRect $g $b 10 10 6 2
    Add-PixelRect $g $b 0 12 2 2
    Add-PixelRect $g $b 4 12 2 2
    Add-PixelRect $g $b 10 12 2 2
    Add-PixelRect $g $b 14 12 2 2
    Add-PixelRect $g $b 2 14 2 2
    Add-PixelRect $g $b 12 14 2 2
}

Write-PixelIcon 'milkable' {
    param($g, $b)
    Add-PixelRect $g $b 4 0 8 2
    Add-PixelRect $g $b 2 2 2 4
    Add-PixelRect $g $b 12 2 2 4
    Add-PixelRect $g $b 2 6 12 2
    Add-PixelRect $g $b 2 8 2 4
    Add-PixelRect $g $b 12 8 2 4
    Add-PixelRect $g $b 4 10 8 4
}

Write-Output "Horse overview icons generated in $resolvedOutput"
