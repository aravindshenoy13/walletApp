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

& (Join-Path $PSScriptRoot 'gradlew.bat') ':app:assembleDebug'
if ($LASTEXITCODE -ne 0) {
    Stop-WithMessage "Gradle build failed with exit code $LASTEXITCODE. See the output above."
}

$apkPath = Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path -LiteralPath $apkPath)) {
    Stop-WithMessage "Build finished but the APK was not found at $apkPath."
}

Write-Host 'Installing the updated app...' -ForegroundColor Cyan
& $adbPath -s $deviceSerial install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    Stop-WithMessage 'APK installation failed. Check that the device is unlocked and available.'
}

Write-Host 'Launching WalletApp...' -ForegroundColor Cyan
& $adbPath -s $deviceSerial shell monkey -p 'com.aravind.wallet' 1
if ($LASTEXITCODE -ne 0) {
    Stop-WithMessage 'The app installed, but Android could not launch it.'
}

Write-Host 'WalletApp is up to date and running.' -ForegroundColor Green
