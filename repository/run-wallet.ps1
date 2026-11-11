param([string]$AndroidUserHome, [string]$DeviceSerial)

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

# Ask which device to use when more than one is online.
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

if ($DeviceSerial) {
    if ($deviceSerials -notcontains $DeviceSerial) {
        Stop-WithMessage "Device $DeviceSerial is not ready. Connected devices: $($deviceSerials -join ', ')."
    }
    $deviceSerial = $DeviceSerial
} elseif ($deviceSerials.Count -eq 1) {
    $deviceSerial = $deviceSerials[0]
} else {
    Write-Host 'Choose a device:' -ForegroundColor Cyan
    for ($walletIndex = 0; $walletIndex -lt $deviceSerials.Count; $walletIndex++) {
        $walletSerial = $deviceSerials[$walletIndex]
        $walletDeviceName = ''
        if ($walletSerial -like 'emulator-*') {
            try {
                $walletAvdOutput = @(& $adbPath -s $walletSerial emu avd name 2>$null)
                if ($LASTEXITCODE -eq 0 -and $walletAvdOutput.Count -gt 0) {
                    $walletDeviceName = "$($walletAvdOutput[0])".Trim()
                }
            } catch { }
        }
        $walletLabel = if ($walletDeviceName) { "$walletDeviceName ($walletSerial)" } else { $walletSerial }
        Write-Host "  $($walletIndex + 1). $walletLabel"
    }
    do {
        $walletChoice = Read-Host 'Enter a number, or press Enter to cancel'
        if ([string]::IsNullOrWhiteSpace($walletChoice)) {
            Write-Host 'No device selected.'
            exit 0
        }
        $walletChoiceNumber = 0
        $walletValidChoice = [int]::TryParse($walletChoice, [ref]$walletChoiceNumber) -and
            $walletChoiceNumber -ge 1 -and $walletChoiceNumber -le $deviceSerials.Count
        if (-not $walletValidChoice) { Write-Host 'Choose one of the listed numbers.' }
    } while (-not $walletValidChoice)
    $deviceSerial = $deviceSerials[$walletChoiceNumber - 1]
}
Write-Host "Building WalletApp for device $deviceSerial..." -ForegroundColor Cyan

$walletProjectAndroidHome = Join-Path $PSScriptRoot '.tools\android-home'
$apkPath = Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
$walletSigningHomes = @()
if ($AndroidUserHome) {
    if (-not (Test-Path -LiteralPath (Join-Path $AndroidUserHome 'debug.keystore'))) {
        Stop-WithMessage "No debug.keystore found in $AndroidUserHome."
    }
    $walletSigningHomes += (Resolve-Path -LiteralPath $AndroidUserHome).Path
} else {
    $walletCandidateHomes = @($walletProjectAndroidHome)
    if ($env:ANDROID_USER_HOME) { $walletCandidateHomes += $env:ANDROID_USER_HOME }
    if ($env:USERPROFILE) { $walletCandidateHomes += (Join-Path $env:USERPROFILE '.android') }
    foreach ($walletCandidate in $walletCandidateHomes) {
        if (Test-Path -LiteralPath (Join-Path $walletCandidate 'debug.keystore')) {
            $walletResolvedHome = (Resolve-Path -LiteralPath $walletCandidate).Path
            if ($walletSigningHomes -notcontains $walletResolvedHome) { $walletSigningHomes += $walletResolvedHome }
        }
    }
    if ($walletSigningHomes.Count -eq 0) { $walletSigningHomes = @('') }
}

for ($walletKeyIndex = 0; $walletKeyIndex -lt $walletSigningHomes.Count; $walletKeyIndex++) {
    $walletSigningHome = $walletSigningHomes[$walletKeyIndex]
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
    if ($walletInstallExitCode -eq 0) { break }
    if (($walletInstallOutput -join "`n") -match 'INSTALL_FAILED_UPDATE_INCOMPATIBLE') {
        if ($walletKeyIndex + 1 -lt $walletSigningHomes.Count) {
            Write-Host 'The installed app uses another key; trying the next existing debug key.' -ForegroundColor Yellow
            continue
        }
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
