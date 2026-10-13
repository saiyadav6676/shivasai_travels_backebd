$ErrorActionPreference = 'Stop'
$rootLauncher = Join-Path $PSScriptRoot '..\start-backend.ps1'
if (-not (Test-Path $rootLauncher)) {
  throw "Backend launcher was not found at $rootLauncher."
}
& $rootLauncher
