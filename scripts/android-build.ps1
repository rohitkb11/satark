param([switch]$Test, [switch]$Lint)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$sdkPath = Join-Path $projectRoot '.tools/sdk'
if (-not (Test-Path -LiteralPath $sdkPath)) {
    $sdkPath = $env:ANDROID_HOME
    if (-not $sdkPath) { $sdkPath = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
}
if (-not (Test-Path -LiteralPath (Join-Path $sdkPath 'platforms/android-35/android.jar'))) {
    throw 'Android SDK 35 is required. Install it with Android Studio or the SDK manager, then set ANDROID_HOME.'
}
$sdkProperty = $sdkPath.Replace('\','/')
Set-Content -LiteralPath (Join-Path $projectRoot 'local.properties') -Value "sdk.dir=$sdkProperty" -Encoding ascii
$gradlePath = Join-Path $projectRoot '.tools/gradle-8.14.3/bin/gradle.bat'
if (-not (Test-Path -LiteralPath $gradlePath)) { $gradlePath = Join-Path $projectRoot 'gradlew.bat' }
if (-not (Test-Path -LiteralPath $gradlePath)) { throw 'Gradle wrapper or Gradle 8.14.3 is required.' }
$tasks = @(':app:assembleDebug')
if ($Test) { $tasks += ':app:testDebugUnitTest' }
if ($Lint) { $tasks += ':app:lintDebug' }
Push-Location $projectRoot
try {
    & $gradlePath --no-daemon --console=plain $tasks
    if ($LASTEXITCODE -ne 0) { throw 'Android build or checks failed.' }
    New-Item -ItemType Directory -Force -Path 'output' | Out-Null
    Copy-Item -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk' -Destination 'output/ScamCheck-debug.apk' -Force
    Write-Host 'APK ready: output/ScamCheck-debug.apk'
} finally { Pop-Location }
