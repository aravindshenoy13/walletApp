param([string]$AndroidUserHome)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

function Stop-WithMessage([string]$Message) {
    Write-Host "ERROR: $Message" -ForegroundColor Red
    exit 1
}

# Find adb from PATH, Android SDK environment variables, or this project's local.properties.
$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
$adbPath = if ($adbCommand) { $adbCommand.Source } else { $null }

if (-not $adbPath) {
    $sdkRoot = $env:ANDROID_SDK_ROOT
    if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_HOME }

    if (-not $sdkRoot) {
        $localPropertiesPath = Join-Path $PSScriptRoot 'local.properties'
        if (Test-Path -LiteralPath $localPropertiesPath) {
            $sdkEntry = Select-String -LiteralPath $localPropertiesPath -Pattern '^sdk\.dir=' | Select-Object -First 1
            if ($sdkEntry) {
                $sdkRoot = $sdkEntry.Line.Substring('sdk.dir='.Length)
                $sdkRoot = [regex]::Replace($sdkRoot, '\\:', ':')
            }
        }
    }

    if ($sdkRoot) {
        $adbPath = Join-Path $sdkRoot 'platform-tools\adb.exe'
    }
}

if (-not $adbPath -or -not (Test-Path -LiteralPath $adbPath)) {
    Stop-WithMessage 'Could not find adb. Check Android Studio > SDK Manager > Android SDK Location, or add platform-tools to PATH.'
}

# Pick the first online emulator or device so the script also works when more than one is connected.
$deviceOutput = & $adbPath devices
if ($LASTEXITCODE -ne 0) {
    Stop-WithMessage 'adb could not list devices.'
}

$deviceSerials = @()
foreach ($deviceLine in ($deviceOutput | Select-Object -Skip 1)) {
    if ($deviceLine -match '^([^\s]+)\s+device$') {
        $deviceSerials += $Matches[1]
    }
}

if ($deviceSerials.Count -eq 0) {
    Stop-WithMessage 'No ready device found. Start an emulator in Device Manager or connect and authorize your phone, then run this script again.'
}

$deviceSerial = $deviceSerials[0]
Write-Host "Building WalletApp for device $deviceSerial..." -ForegroundColor Cyan

$walletProjectAndroidHome = Join-Path $PSScriptRoot '.tools\android-home'
$walletSigningHome = $AndroidUserHome
if (-not $walletSigningHome -and (Test-Path -LiteralPath (Join-Path $walletProjectAndroidHome 'debug.keystore'))) {
    $walletSigningHome = $walletProjectAndroidHome
}
if (-not $walletSigningHome) { $walletSigningHome = $env:ANDROID_USER_HOME }
if ($walletSigningHome -and -not (Test-Path -LiteralPath (Join-Path $walletSigningHome 'debug.keystore'))) {
    Stop-WithMessage "No debug.keystore found in $walletSigningHome. Choose the Android user directory containing the key used to install this app."
}

$walletPreviousAndroidHome = $env:ANDROID_USER_HOME
$walletPreviousGradleHome = $env:GRADLE_USER_HOME
try {
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-user-home'
    if ($walletSigningHome) {
        $env:ANDROID_USER_HOME = $walletSigningHome
        Write-Host "Using debug key in $walletSigningHome" -ForegroundColor Cyan
    }
    & (Join-Path $PSScriptRoot 'gradlew.bat') ':app:assembleDebug'
    $walletBuildExitCode = $LASTEXITCODE
} finally {
    if ($null -eq $walletPreviousAndroidHome) { Remove-Item Env:ANDROID_USER_HOME -ErrorAction SilentlyContinue }
    else { $env:ANDROID_USER_HOME = $walletPreviousAndroidHome }
    if ($null -eq $walletPreviousGradleHome) { Remove-Item Env:GRADLE_USER_HOME -ErrorAction SilentlyContinue }
    else { $env:GRADLE_USER_HOME = $walletPreviousGradleHome }
}
if ($walletBuildExitCode -ne 0) {
    Stop-WithMessage "Gradle build failed with exit code $walletBuildExitCode. See the output above."
}

$apkPath = Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path -LiteralPath $apkPath)) {
    Stop-WithMessage "Build finished but the APK was not found at $apkPath."
}

Write-Host 'Installing the updated app...' -ForegroundColor Cyan
$walletPreviousErrorAction = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Continue'
    $walletInstallOutput = @(& $adbPath -s $deviceSerial install -r $apkPath 2>&1)
    $walletInstallExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $walletPreviousErrorAction
}
$walletInstallOutput | ForEach-Object { Write-Host "$_" }
if ($walletInstallExitCode -ne 0) {
    if (($walletInstallOutput -join "`n") -match 'INSTALL_FAILED_UPDATE_INCOMPATIBLE') {
        Stop-WithMessage 'The installed app uses a different signing key. Keep the installed app to preserve its data, and rerun with -AndroidUserHome pointing to the directory containing its original debug.keystore.'
    }
    Stop-WithMessage 'APK installation failed. See the adb output above.'
}

Write-Host 'Launching WalletApp...' -ForegroundColor Cyan
& $adbPath -s $deviceSerial shell monkey -p 'com.aravind.wallet' 1
if ($LASTEXITCODE -ne 0) {
    Stop-WithMessage 'The app installed, but Android could not launch it.'
}

Write-Host 'WalletApp is up to date and running.' -ForegroundColor Green
