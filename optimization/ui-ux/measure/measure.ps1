# Repeated, interleaved frame measurements of the ShelfTap member app (optimization/ui-ux-plan.md section 8.5).
#
#   .\measure.ps1 -Serial <serial máy> -Apks 'head=C:\path\app-measure.apk','other=C:\path\other.apk' -Rounds 5
#
# Why this exists: one run per build has noise of about +-2% janky frames and +-10 ms at the 90th percentile, so a single number cannot say
# that one build is better than another. This script
#   * runs every build in every round, and swaps their order each round, so drift (a warming phone) does not favour one of them;
#   * reinstalls the build each time and gives it what a store install would (the baseline profile, then a profile compile), because a
#     DEBUGGABLE build is not compiled that way and is about 5 times slower; use the `measure` build type (assembleMeasure), never debug;
#   * rests between runs so the phone cools;
#   * keeps every run as a row (results.tsv) and reports the MEDIAN and the MIN-MAX of each build, per scenario;
#   * says a build is better only when the two ranges do not overlap.
# Scenarios (what the member does, `gfxinfo` frames of each):
#   openBook    tap the first book card, wait, back, repeat (page transition, the book page, the dock morph)
#   openMenu    open and close the menu (phones: the burger)
#   scrollHome  swipe the catalog home up and down
#   scrollList  open "see all" and swipe the long list (pages are loaded as it scrolls)
# -Tier light switches the system animations off, which is what makes the app pick its light tier; auto leaves the phone as it is.
# The phone must be unlocked, on the catalog, and able to reach the server the build was made for (assembleMeasure -Pshelftap.apiBaseUrl=...).
param(
    [Parameter(Mandatory)] [string] $Serial,
    [Parameter(Mandatory)] [string[]] $Apks,
    [string[]] $Scenarios = @('openBook', 'openMenu', 'scrollHome', 'scrollList'),
    [int] $Rounds = 5,
    [int] $Cool = 20,
    [ValidateSet('auto', 'light')] [string] $Tier = 'auto',
    [string] $Out = (Join-Path $PSScriptRoot 'results'),
    [string] $Package = 'shelftap.android'
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
function Sh([string]$c) { (& $adb -s $Serial shell $c) -join "`n" }

function ScreenSize {
    $s = Sh 'wm size'
    if ($s -match 'Override size: (\d+)x(\d+)') { return @([int]$Matches[1], [int]$Matches[2]) }
    if ($s -match 'Physical size: (\d+)x(\d+)') { return @([int]$Matches[1], [int]$Matches[2]) }
    throw "cannot read the screen size: $s"
}

# The centre of the first node whose text or content description matches $pattern, or $null.
function Centre([string]$pattern, [int]$tries = 6) {
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

# The app starts on the welcome screen when nobody is signed in: take "browse without logging in" to get to the catalog. (Does nothing when the
# build starts on the catalog, as older builds do.)
function EnterCatalog {
    $guest = Centre '^(Xem sách mà không cần đăng nhập|Browse books without logging in)$' 2
    if ($guest) { Tap $guest; Start-Sleep -Seconds 3 }
}

function RunScenario([string]$name) {
    $w, $h = ScreenSize
    $mid = [int]($w / 2)
    switch ($name) {
        'openBook' {
            $card = Centre ', (Còn|Đã cho mượn hết)'
            if (-not $card) { throw 'openBook: no book card on screen' }
            Sh 'dumpsys gfxinfo shelftap.android reset' | Out-Null
            for ($i = 0; $i -lt 12; $i++) {
                Tap $card; Start-Sleep -Milliseconds 1500
                Sh 'input keyevent KEYCODE_BACK' | Out-Null; Start-Sleep -Milliseconds 900
            }
        }
        'openMenu' {
            $burger = Centre '^(Mở menu|Open menu)$'
            if (-not $burger) { return $false } # a wide window has no burger: nothing to measure
            Sh 'dumpsys gfxinfo shelftap.android reset' | Out-Null
            for ($i = 0; $i -lt 15; $i++) {
                Tap $burger; Start-Sleep -Milliseconds 700
                Tap $burger; Start-Sleep -Milliseconds 600
            }
        }
        'scrollHome' {
            Sh 'dumpsys gfxinfo shelftap.android reset' | Out-Null
            $y0 = [int]($h * 0.8); $y1 = [int]($h * 0.3)
            for ($i = 0; $i -lt 12; $i++) {
                Sh "input swipe $mid $y0 $mid $y1 250" | Out-Null; Start-Sleep -Milliseconds 450
                Sh "input swipe $mid $y0 $mid $y1 250" | Out-Null; Start-Sleep -Milliseconds 450
                Sh "input swipe $mid $y1 $mid $y0 250" | Out-Null; Start-Sleep -Milliseconds 450
            }
        }
        'scrollList' {
            $y0 = [int]($h * 0.8); $y1 = [int]($h * 0.3)
            # The "see all books" button is the last item of the home: swipe up until it is on screen (the strips above it differ in length).
            $all = $null
            for ($i = 0; $i -lt 14 -and -not $all; $i++) {
                $all = Centre '^(Xem tất cả sách|See all books)$' 1
                if (-not $all) { Sh "input swipe $mid $y0 $mid $y1 300" | Out-Null; Start-Sleep -Milliseconds 500 }
            }
            if (-not $all) { return $false }
            Tap $all; Start-Sleep -Seconds 4
            Sh 'dumpsys gfxinfo shelftap.android reset' | Out-Null
            for ($i = 0; $i -lt 12; $i++) {
                Sh "input swipe $mid $y0 $mid $y1 250" | Out-Null; Start-Sleep -Milliseconds 400
                Sh "input swipe $mid $y0 $mid $y1 250" | Out-Null; Start-Sleep -Milliseconds 400
                Sh "input swipe $mid $y1 $mid $y0 250" | Out-Null; Start-Sleep -Milliseconds 400
            }
        }
        default { throw "unknown scenario $name" }
    }
    return $true
}

function Stats {
    $g = Sh "dumpsys gfxinfo $Package"
    $row = [ordered]@{ frames = 0; janky = 0; jankPct = 0.0; p50 = 0; p90 = 0; p95 = 0; p99 = 0 }
    if ($g -match 'Total frames rendered: (\d+)') { $row.frames = [int]$Matches[1] }
    if ($g -match 'Janky frames: (\d+) \(([\d.]+)%\)') { $row.janky = [int]$Matches[1]; $row.jankPct = [double]$Matches[2] }
    foreach ($q in 50, 90, 95, 99) { if ($g -match "$q" + 'th percentile: (\d+)ms') { $row["p$q"] = [int]$Matches[1] } }
    $m = Sh "dumpsys meminfo $Package"
    $row.rssKb = if ($m -match 'TOTAL RSS:\s+(\d+)') { [int]$Matches[1] } else { 0 }
    $row.pssKb = if ($m -match 'TOTAL PSS:\s+(\d+)') { [int]$Matches[1] } else { 0 }
    return $row
}

function Median($xs) { $s = @($xs | Sort-Object); $n = $s.Count; if ($n -eq 0) { return 0 }; if ($n % 2) { $s[($n - 1) / 2] } else { ($s[$n / 2 - 1] + $s[$n / 2]) / 2 } }

$builds = $Apks | ForEach-Object { $k, $v = $_ -split '=', 2; [pscustomobject]@{ Label = $k; Path = $v } }
New-Item -ItemType Directory -Force $Out | Out-Null
$tsv = Join-Path $Out 'results.tsv'
"label`tround`tscenario`ttier`tframes`tjanky`tjankPct`tp50`tp90`tp95`tp99`trssKb`tpssKb`tbatteryC" | Set-Content $tsv
$device = (Sh 'getprop ro.product.model').Trim() + ' / Android ' + (Sh 'getprop ro.build.version.release').Trim()
Write-Host "device: $device; tier: $Tier; rounds: $Rounds; builds: $($builds.Label -join ', ')"

# -Tier light turns the system animations off; the phone's own values are kept and put back afterwards (also if the script fails). If the
# script is killed from outside, nothing can put them back: run `adb shell settings put global animator_duration_scale 1.0` (and
# transition_animation_scale, window_animation_scale) by hand, or the app will stay in its light tier with no motion.
$animKeys = 'animator_duration_scale', 'transition_animation_scale', 'window_animation_scale'
$animOriginal = @{}
if ($Tier -eq 'light') {
    foreach ($k in $animKeys) { $v = (Sh "settings get global $k").Trim(); $animOriginal[$k] = if ($v -and $v -ne 'null') { $v } else { '1.0' } }
    foreach ($k in $animKeys) { Sh "settings put global $k 0" | Out-Null }
}
try {
    for ($round = 1; $round -le $Rounds; $round++) {
        # The order swaps each round, so no build always runs first (cold) or last (warm).
        $order = if ($round % 2) { $builds } else { @($builds)[($builds.Count - 1)..0] }
        foreach ($b in $order) {
            & $adb -s $Serial install -r -t $b.Path | Out-Null
            Sh 'input keyevent KEYCODE_WAKEUP' | Out-Null; Sh 'wm dismiss-keyguard' | Out-Null
            Sh "am start -n $Package/.MainActivity" | Out-Null; Start-Sleep -Seconds 5
            # What a store install does with the profile inside the APK: write it, then compile by it.
            Sh "am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE $Package/androidx.profileinstaller.ProfileInstallReceiver" | Out-Null
            Start-Sleep -Seconds 4
            Sh "cmd package compile -f -m speed-profile $Package" | Out-Null
            Sh "am force-stop $Package" | Out-Null; Sh "am start -n $Package/.MainActivity" | Out-Null; Start-Sleep -Seconds 8
            EnterCatalog
            foreach ($sc in $Scenarios) {
                $ran = RunScenario $sc
                if ($ran -eq $false) { Write-Host "  skip $sc"; continue }
                $s = Stats
                $temp = if ((Sh 'dumpsys battery') -match 'temperature: (\d+)') { [int]$Matches[1] / 10 } else { 0 }
                "$($b.Label)`t$round`t$sc`t$Tier`t$($s.frames)`t$($s.janky)`t$($s.jankPct)`t$($s.p50)`t$($s.p90)`t$($s.p95)`t$($s.p99)`t$($s.rssKb)`t$($s.pssKb)`t$temp" | Add-Content $tsv
                Write-Host ("  round {0} {1,-6} {2,-10} janky {3,5}%  p90 {4,3} ms  rss {5} MB" -f $round, $b.Label, $sc, $s.jankPct, $s.p90, [int]($s.rssKb / 1024))
                Sh "am force-stop $Package" | Out-Null; Sh "am start -n $Package/.MainActivity" | Out-Null; Start-Sleep -Seconds 6
                EnterCatalog
            }
            Start-Sleep -Seconds $Cool
        }
    }
} finally {
    if ($Tier -eq 'light') { foreach ($k in $animKeys) { Sh "settings put global $k $($animOriginal[$k])" | Out-Null } }
}

# Summary: the median and the range of each build, and whether two builds' ranges overlap.
$rows = Import-Csv $tsv -Delimiter "`t"
$sum = Join-Path $Out 'summary.tsv'
"scenario`tlabel`truns`tjankMedian`tjankMin`tjankMax`tp90Median`tp90Min`tp90Max`trssMedianMb" | Set-Content $sum
foreach ($sc in ($rows | Select-Object -ExpandProperty scenario -Unique)) {
    foreach ($b in $builds) {
        $r = @($rows | Where-Object { $_.scenario -eq $sc -and $_.label -eq $b.Label })
        if ($r.Count -eq 0) { continue }
        $j = $r | ForEach-Object { [double]$_.jankPct }; $p = $r | ForEach-Object { [double]$_.p90 }; $m = $r | ForEach-Object { [double]$_.rssKb / 1024 }
        "$sc`t$($b.Label)`t$($r.Count)`t$(Median $j)`t$(($j | Measure-Object -Minimum).Minimum)`t$(($j | Measure-Object -Maximum).Maximum)`t$(Median $p)`t$(($p | Measure-Object -Minimum).Minimum)`t$(($p | Measure-Object -Maximum).Maximum)`t$([int](Median $m))" | Add-Content $sum
    }
}
Write-Host "`n== summary ($device; $Tier) =="
Import-Csv $sum -Delimiter "`t" | Format-Table -AutoSize | Out-String | Write-Host
Write-Host "raw rows: $tsv"
