param(
    [string]$ProjectRoot = 'F:\shixi\yunrui-edu-interview'
)

$ErrorActionPreference = 'Stop'
$assetRoot = Join-Path $ProjectRoot 'frontend\src\assets'
$sourceRoot = 'F:\shixi\.video-download\DemoVideos'

if (-not (Test-Path -LiteralPath $sourceRoot)) {
    throw "未找到视频源目录：$sourceRoot。请先从 https://github.com/andreasbotsikas/DemoVideos.git 克隆到该目录。"
}

New-Item -ItemType Directory -Force -Path $assetRoot | Out-Null
$files = @(
    @{ Source = 'big_buck_bunny.mp4'; Target = 'course-demo-big-buck-bunny.mp4' },
    @{ Source = 'tears_of_steel.mp4'; Target = 'course-demo-tears-of-steel.mp4' }
)

foreach ($file in $files) {
    $source = Join-Path $sourceRoot $file.Source
    $target = Join-Path $assetRoot $file.Target
    if (-not (Test-Path -LiteralPath $source)) {
        throw "缺少视频文件：$source"
    }
    Copy-Item -LiteralPath $source -Destination $target -Force
    $item = Get-Item -LiteralPath $target
    if ($item.Length -lt 1MB) {
        throw "视频文件大小异常：$target"
    }
    Write-Host ("已准备 {0} ({1:N0} bytes)" -f $item.Name, $item.Length)
}

Write-Host "视频已放入 $assetRoot；启动应用后由 Flyway V18 将已发布课程的前两章绑定到这两个文件。"
