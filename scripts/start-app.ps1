$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
$wslIp = ((wsl -d Ubuntu-24.04 -- hostname -I) -replace "`0", "").Trim().Split(' ')[0]
if (-not $wslIp) { throw "无法获取 WSL 地址，请先执行 start-infra.ps1" }
$env:DB_URL = "jdbc:mysql://${wslIp}:13307/yunrui_edu?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:RABBITMQ_HOST = $wslIp
$env:REDIS_HOST = $wslIp
$env:JWT_SECRET = "yunrui-local-development-secret-key-at-least-32-bytes"
Set-Location $project
mvn spring-boot:run
