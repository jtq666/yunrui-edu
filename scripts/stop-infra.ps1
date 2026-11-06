$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
$wslProject = (wsl -d Ubuntu-24.04 -- wslpath -a ($project -replace '\\','/')).Trim()
wsl -d Ubuntu-24.04 -- bash -lc "cd '$wslProject' && docker compose stop"
