$ErrorActionPreference = "Stop"
$frontend = Join-Path (Split-Path -Parent $PSScriptRoot) "frontend"
$env:npm_config_cache = "F:\shixi\.npm-cache"
Set-Location $frontend
npm run dev
