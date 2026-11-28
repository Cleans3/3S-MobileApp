# Records one fixed sequence on a PHONE with the system's `screenrecord`, with the frame statistics of that pass.
#   .\record_phone.ps1 -Serial 2a71036b -Apk C:\t\apks\now-capture.apk -Label NOW -Mode now
#   .\record_phone.ps1 -Serial 2a71036b -Apk C:\t\apks\base-capture.apk -Label BASE -Mode base
# Writes optimization/ui-ux/video/phone-<Label>/effects.mp4 and gfxinfo.txt (and stats.txt: the same numbers measure.ps1 reads).
#
# The app sets FLAG_SECURE, which blacks out screenrecord. The APK given here must therefore be a THROWAWAY build made from a copy of the
# source with that one line removed (a `measure` build: R8, no debug flag, the Baseline Profile; the copy is never committed, and no build
# that is installed for anyone else is made this way). The sequence is driven with `adb input`, like measure.ps1; one pass is made unrecorded
# first (a store install is profile-compiled, as measure.ps1 does, and the first pass still wakes the code up).
#   -Mode now   the current app: browse as a guest: swipe the home, open and close the menu, open a book and go back, "see all" and swipe the list
#   -Mode base  the app as first cloned: it needs a signed-in member (sign in on the phone first): swipe the list, open a book and go back, Loans, Reserved, Catalog
param(
    [Parameter(Mandatory)] [string] $Serial,
    [Parameter(Mandatory)] [string] $Apk,
    [string] $Label = 'NOW',
    [ValidateSet('now', 'base')] [string] $Mode = 'now',
    [string] $Package = 'shelftap.android'
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$out = Join-Path $PSScriptRoot "..\video\phone-$Label"
New-Item -ItemType Directory -Force $out | Out-Null
function Sh([string]$c) { (& $adb -s $Serial shell $c) -join "`n" }
function ScreenSize {
    $s = Sh 'wm size'
    if ($s -match 'Override size: (\d+)x(\d+)') { return @([int]$Matches[1], [int]$Matches[2]) }
    if ($s -match 'Physical size: (\d+)x(\d+)') { return @([int]$Matches[1], [int]$Matches[2]) }
    throw "cannot read the screen size: $s"
}
function Centre([string]$pattern, [int]$tries = 4) {
    for ($t = 0; $t -lt $tries; $t++) {
        Sh 'uiautomator dump /sdcard/ui.xml' | Out-Null
        $xml = Sh 'cat /sdcard/ui.xml'
        foreach ($node in [regex]::Matches($xml, '<node [^>]*>')) {
            $tag = $node.Value
            $text = if ($tag -match ' text="([^"]*)"') { $Matches[1] } else { '' }
            $desc = if ($tag -match 'content-desc="([^"]*)"') { $Matches[1] } else { '' }
            if (($text -match $pattern -or $desc -match $pattern) -and $tag -match 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') {
                return @([int](([int]$Matches[1] + [int]$Matches[3]) / 2), [int](([int]$Matches[2] + [int]$Matches[4]) / 2))
            }
        }
        Start-Sleep -Seconds 2
    }
    return $null
}
function Tap($p) { Sh "input tap $($p[0]) $($p[1])" | Out-Null }
function Swipe([int]$x, [int]$y0, [int]$y1, [int]$ms = 250) { Sh "input swipe $x $y0 $x $y1 $ms" | Out-Null }

$w, $h = ScreenSize
$mid = [int]($w / 2); $y0 = [int]($h * 0.8); $y1 = [int]($h * 0.3)

function Pass {
    if ($Mode -eq 'now') {
        Start-Sleep -Milliseconds 1200
        Swipe $mid $y0 $y1; Start-Sleep -Milliseconds 900
        Swipe $mid $y1 $y0; Start-Sleep -Milliseconds 900
        $burger = Centre '^(Mở menu|Open menu)$' 2
        if ($burger) {
            Tap $burger; Start-Sleep -Milliseconds 1300
            Tap $burger; Start-Sleep -Milliseconds 1100
        }
        $card = Centre ', (Còn|Đã cho mượn hết)' 2
        if ($card) { Tap $card; Start-Sleep -Milliseconds 2200; Sh 'input keyevent KEYCODE_BACK' | Out-Null; Start-Sleep -Milliseconds 1400 }
        $all = $null
        for ($i = 0; $i -lt 14 -and -not $all; $i++) {
            $all = Centre '^(Xem tất cả sách|See all books)$' 1
            if (-not $all) { Swipe $mid $y0 $y1 300; Start-Sleep -Milliseconds 500 }
        }
        if ($all) {
            Tap $all; Start-Sleep -Seconds 3
            for ($i = 0; $i -lt 5; $i++) { Swipe $mid $y0 $y1; Start-Sleep -Milliseconds 500 }
            for ($i = 0; $i -lt 3; $i++) { Swipe $mid $y1 $y0; Start-Sleep -Milliseconds 500 }
            Sh 'input keyevent KEYCODE_BACK' | Out-Null; Start-Sleep -Milliseconds 1400
        }
    } else {
        Start-Sleep -Milliseconds 1200
        Swipe $mid $y0 $y1; Start-Sleep -Milliseconds 900
        Swipe $mid $y0 $y1; Start-Sleep -Milliseconds 900
        Swipe $mid $y1 $y0; Start-Sleep -Milliseconds 900
        $card = Centre '^(\d+ available|All copies on loan)$' 2
        if ($card) { Tap $card; Start-Sleep -Milliseconds 2200; Sh 'input keyevent KEYCODE_BACK' | Out-Null; Start-Sleep -Milliseconds 1400 }
        foreach ($tab in 'Loans', 'Reserved', 'Catalog') {
            $t = Centre "^$tab$" 2
            if ($t) { Tap $t; Start-Sleep -Milliseconds 1600 }
        }
    }
}

& $adb -s $Serial install -r -t $Apk | Out-Null
Sh 'input keyevent KEYCODE_WAKEUP' | Out-Null; Sh 'wm dismiss-keyguard' | Out-Null
Sh "am start -n $Package/.MainActivity" | Out-Null; Start-Sleep -Seconds 5
Sh "am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE $Package/androidx.profileinstaller.ProfileInstallReceiver" | Out-Null
Start-Sleep -Seconds 4
Sh "cmd package compile -f -m speed-profile $Package" | Out-Null
Sh "am force-stop $Package" | Out-Null; Sh "am start -n $Package/.MainActivity" | Out-Null; Start-Sleep -Seconds 8
if ($Mode -eq 'now') {
    $guest = Centre '^(Xem sách mà không cần đăng nhập|Browse books without logging in)$' 2
    if ($guest) { Tap $guest; Start-Sleep -Seconds 3 }
} elseif (-not (Centre '^(\d+ available|All copies on loan)$' 4)) {
    throw 'the first clone is not on its catalog: sign in on the phone first, then run again'
}
Pass   # warm-up, not recorded
$remote = "/sdcard/phone-$Label.mp4"
Sh "rm -f $remote" | Out-Null
$rec = Start-Process -FilePath $adb -ArgumentList @('-s', $Serial, 'shell', "screenrecord --bit-rate 6000000 --size 576x1248 --time-limit 150 $remote") -PassThru -WindowStyle Hidden
Start-Sleep -Seconds 3
Sh "dumpsys gfxinfo $Package reset" | Out-Null
Pass
Sh "dumpsys gfxinfo $Package" | Set-Content (Join-Path $out 'gfxinfo.txt')
Sh 'pkill -INT screenrecord' | Out-Null
Start-Sleep -Seconds 4
& $adb -s $Serial pull $remote (Join-Path $out 'effects.mp4') | Out-Host
Sh "rm -f $remote" | Out-Null
Sh "dumpsys meminfo $Package" | Set-Content (Join-Path $out 'meminfo.txt')
"video: " + (Resolve-Path (Join-Path $out 'effects.mp4'))
