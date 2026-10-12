param()
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    & "$PSScriptRoot/build.ps1" -Test
    & java -cp build/classes in.scamcheck.cli.Phase4Evaluate evaluation/phase4_scenarios.tsv evaluation/phase4-results.json
    if($LASTEXITCODE -ne 0){throw 'Evaluation failed.'}
} finally {Pop-Location}
