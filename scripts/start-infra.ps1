$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
$wslProject = (wsl -d Ubuntu-24.04 -- wslpath -a ($project -replace '\\','/')).Trim()
wsl -d Ubuntu-24.04 -- bash -lc "sudo service docker start >/dev/null 2>&1 || true; cd '$wslProject' && docker compose up -d"
wsl -d Ubuntu-24.04 -- docker ps --filter "name=yunrui-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
