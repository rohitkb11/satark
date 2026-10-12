param([switch]$Test, [switch]$Demo)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$classesPath = Join-Path $projectRoot 'build/classes'
New-Item -ItemType Directory -Force -Path $classesPath | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'core/src/main/java') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
if ($Test) {
    $sources += @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'core/src/test/java') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
}
& javac --release 17 -encoding UTF-8 -d $classesPath $sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
Copy-Item -Path (Join-Path $projectRoot 'core/src/main/resources/*') -Destination $classesPath -Force
if ($Test) {
    & java '-Dfile.encoding=UTF-8' -cp $classesPath in.scamcheck.core.CoreTests
    if ($LASTEXITCODE -ne 0) { throw 'Core checks failed.' }
    & java '-Dfile.encoding=UTF-8' -cp $classesPath in.scamcheck.core.ModelTests
    if ($LASTEXITCODE -ne 0) { throw 'Model checks failed.' }
    & java '-Dfile.encoding=UTF-8' -cp $classesPath in.scamcheck.core.UpdateTests
    if ($LASTEXITCODE -ne 0) { throw 'Signed-update checks failed.' }
}
if ($Demo) {
    & java '-Dfile.encoding=UTF-8' -cp $classesPath in.scamcheck.cli.Main --demo
    if ($LASTEXITCODE -ne 0) { throw 'Demo failed.' }
}
Write-Host 'Build complete. No message data was written by the checker.'
