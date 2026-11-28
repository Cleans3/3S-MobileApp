# Records the effects of the app as a real video (mp4) on an emulator or phone.
#   .\record_effects.ps1 -Serial emulator-5556 -Label V1
# Builds and installs the debug app and its test, runs UiAuditTest#effectsVideo (the test itself starts and stops `screenrecord` for its
# second pass, after a warm-up pass) and pulls the file to optimization/ui-ux/video/<Label>/effects.mp4.
#
# Why through the test: the app sets FLAG_SECURE, which also blacks out screenrecord. The test clears the flag on its own window only; the
# product code and every build keep it. A release build cannot be recorded this way without being changed (it needs a server to log in to,
# and R8 renames the classes the test uses), so the video is of the debug build after a warm-up pass: the first run through the code is
# the slow one in a debug build; the second shows the effects at the speed the device draws them.
param(
    [Parameter(Mandatory)] [string] $Serial,
    [string] $Label = 'V1',
    [string] $Locale = 'vi',
    # The repository (or worktree) whose android/ is built. The app as first cloned is a worktree of the tag `ui-baseline` with the audit harness
    # of commit c4280b8 and its own effectsVideo test (see optimization/ui-ux/tools/README.md).
    [string] $Repo = ''
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$repo = if ($Repo) { Resolve-Path $Repo } else { Resolve-Path (Join-Path $PSScriptRoot '..\..\..') }
$out = Join-Path $PSScriptRoot "..\video\$Label"
New-Item -ItemType Directory -Force $out | Out-Null
if ((& $adb -s $Serial shell getprop sys.boot_completed).Trim() -ne '1') { throw "$Serial is not booted" }

# The reference window of the audits (390 x 844 dp at 480 dpi), light, normal text, then put back as it was.
$origSize = if ((& $adb -s $Serial shell wm size) -join ' ' -match 'Override size: (\S+)') { $Matches[1] } else { 'reset' }
$origDensity = if ((& $adb -s $Serial shell wm density) -join ' ' -match 'Override density: (\d+)') { $Matches[1] } else { 'reset' }
& $adb -s $Serial shell wm size 1170x2532 | Out-Null
& $adb -s $Serial shell wm density 480 | Out-Null
& $adb -s $Serial shell settings put system font_scale 1.0 | Out-Null
& $adb -s $Serial shell cmd uimode night no | Out-Null
& $adb -s $Serial shell cmd locale set-app-locales shelftap.android --locales $Locale 2>$null | Out-Null
try {
    Push-Location (Join-Path $repo 'android')
    $env:ANDROID_SERIAL = $Serial
    & .\gradlew.bat installDebug installDebugAndroidTest | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'build or install failed' }
    Pop-Location
    & $adb -s $Serial shell rm -f "/data/local/tmp/effects-$Label.mp4" | Out-Null
    & $adb -s $Serial shell am instrument -w -e uiAudit $Label -e uiVideo 1 -e class shelftap.android.uiaudit.UiAuditTest#effectsVideo shelftap.android.test/androidx.test.runner.AndroidJUnitRunner | Out-Host
    & $adb -s $Serial pull "/data/local/tmp/effects-$Label.mp4" (Join-Path $out 'effects.mp4') | Out-Host
    & $adb -s $Serial pull "/sdcard/Android/data/shelftap.android/files/ui-audit/$Label/gfxinfo.txt" (Join-Path $out 'gfxinfo.txt') | Out-Host
    & $adb -s $Serial shell rm -f "/data/local/tmp/effects-$Label.mp4" | Out-Null
    "video: " + (Resolve-Path (Join-Path $out 'effects.mp4'))
} finally {
    if ($origSize -eq 'reset') { & $adb -s $Serial shell wm size reset | Out-Null } else { & $adb -s $Serial shell wm size $origSize | Out-Null }
    if ($origDensity -eq 'reset') { & $adb -s $Serial shell wm density reset | Out-Null } else { & $adb -s $Serial shell wm density $origDensity | Out-Null }
}
