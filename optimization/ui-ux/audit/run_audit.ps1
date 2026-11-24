# UI audit for optimization/ui-ux-plan.md §8.2. Run from PowerShell:
#   .\run_audit.ps1 -Label A -Serial emulator-5556
# Runs androidTest/uiaudit/UiAuditTest once per -Configs entry (default: light, dark, font scale 2.0 and 320 dp wide), each with the device set up
# for that configuration, and pulls the results to optimization/ui-ux/audit/<Label>/ (findings-<config>.tsv, screens-<config>.tsv,
# elements-<config>.tsv, <config>/<screen>.png). Then it puts the device back as it was.
#
# -Serial is required: connectedAndroidTest otherwise installs on EVERY connected device (a physical phone, another
# session's emulator). The audit must use a device nobody else is measuring on. The reference is the low-end AVD
# "lowend36" (720x1280 @280 dpi, 2 cores, 2 GB), booted with optimization/app-catalog/tools/run_emulator.sh; a second
# instance needs its own AVD folder (copy of lowend36's config.ini) and another -port.
param(
    [Parameter(Mandatory)] [string] $Label,
    [Parameter(Mandatory)] [string] $Serial,
    # Which device configurations to run. On a physical phone use 'light','dark': font2 and w320 change the phone's font and display size.
    [string[]] $Configs = @('light', 'dark', 'font2', 'w320', 'medium', 'expanded'),
    # App language for the run (per-app locale, Android 13+), for example 'vi' to compare with the Vietnamese mockup. Default: leave as the device has it.
    [string] $Locale = ''
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$repo = Resolve-Path (Join-Path $PSScriptRoot '..\..\..')
$out = Join-Path $PSScriptRoot $Label
$remote = "/sdcard/Android/data/shelftap.android/files/ui-audit/$Label"

function Sh($cmd) { & $adb -s $Serial shell $cmd | Out-Null }
# Remember how the device was set up, and put exactly that back afterwards (a phone may be on dark mode or a larger font).
function Get($cmd) { ((& $adb -s $Serial shell $cmd) -join ' ').Trim() }
$origFont = Get 'settings get system font_scale'
if ($origFont -eq 'null' -or $origFont -eq '') { $origFont = '1.0' }
$nightNow = Get 'cmd uimode night'
$origNight = if ($nightNow -match 'auto') { 'auto' } elseif ($nightNow -match 'yes') { 'yes' } else { 'no' }
$origSize = if ((Get 'wm size') -match 'Override size: (\S+)') { $Matches[1] } else { 'reset' }
function ApplyBase { Sh "wm size $origSize"; Sh "settings put system font_scale $origFont"; Sh "cmd uimode night $origNight" }
# Each configuration starts from light, 1.0 font and the physical size; 'dark' etc. then change one thing.
function ResetDevice { Sh 'wm size reset'; Sh 'settings put system font_scale 1.0'; Sh 'cmd uimode night no' }

if ((& $adb -s $Serial shell getprop sys.boot_completed).Trim() -ne '1') { throw "$Serial is not booted" }
$size = (& $adb -s $Serial shell wm size) -join ' '
$density = (& $adb -s $Serial shell wm density) -join ' '
Write-Host "device $Serial : $size / $density"

if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force $out | Out-Null
Sh "rm -rf $remote"
$env:ANDROID_SERIAL = $Serial
Sh 'input keyevent KEYCODE_WAKEUP'; Sh 'wm dismiss-keyguard'

foreach ($config in $Configs) {
    ResetDevice
    switch ($config) {
        'dark'  { Sh 'cmd uimode night yes' }
        'font2' { Sh 'settings put system font_scale 2.0' }
        'w320'  { Sh 'wm size 560x1120' } # 320 dp x 640 dp at 280 dpi
        'medium'   { Sh 'wm size 1260x1470' } # 720 dp x 840 dp at 280 dpi: a folded-open phone, nearly square
        'expanded' { Sh 'wm size 1680x1260' } # 960 dp x 720 dp at 280 dpi: a tablet or an unfolded phone held sideways
    }
    if ($Locale) { Sh "cmd locale set-app-locales shelftap.android --locales $Locale" }
    Write-Host "== $config"
    Push-Location (Join-Path $repo 'android')
    try {
        & .\gradlew.bat :app:connectedDebugAndroidTest --console=plain `
            "-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true" `
            "-Pandroid.testInstrumentationRunnerArguments.class=shelftap.android.uiaudit.UiAuditTest" `
            "-Pandroid.testInstrumentationRunnerArguments.uiAudit=$Label" `
            "-Pandroid.testInstrumentationRunnerArguments.config=$config"
        if ($LASTEXITCODE -ne 0) { throw "audit run '$config' failed" }
    } finally { Pop-Location }
    & $adb -s $Serial pull "$remote/." $out | Out-Null
}
ApplyBase
if ($Locale) { Sh 'cmd locale set-app-locales shelftap.android --locales ""' }
Write-Host "results in $out"
