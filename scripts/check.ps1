$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'build.ps1')
& java '-Dfile.encoding=UTF-8' -cp (Join-Path $projectRoot 'build/classes') in.scamcheck.cli.Main
if ($LASTEXITCODE -ne 0) { throw 'Check did not complete.' }
