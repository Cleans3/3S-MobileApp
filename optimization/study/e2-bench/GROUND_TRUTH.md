# optlab ground truth (for E5 and anyone fixing the A variants)

The optlab app carries both variants of each factor in one APK; intent extras pick the screen
(`adb shell am start -n shelftap.optlab/.MainActivity --es factor P08 --es variant A`). Source: `android/optlab/`.
A = slop (the thing to find and fix), B = the tuned reference. A independent check that a fix worked is the
benchmark (`optimization/study/e2-bench/harness/run_bench.py`) plus `results/summary.csv`, not the fixer's own claim.

Every A variant is **provisional**: written by the harness author from the obvious naive pattern. It is replaced by
real E1 corpus code once that lands (provenance per variant: `variants.csv`).

| factor | A file:line | taxonomy | seeded defects (ground truth list) |
|---|---|---|---|
| P08 | EffectsScreen.kt:63 | P08 | 1 shadow(18.dp) 2 blur(8.dp) 3 gradient brush background 4 infinite alpha animation read in composition |
| P06 | ImagesScreen.kt:75 | P06 | 1 decodeFile of a 1500x2250 JPEG without inSampleSize 2 no memory/disk cache (decode repeats on every re-entry) |
| P01+P09 | ListScreen.kt:19 | P09, P01 | 1 Column + verticalScroll + forEach instead of a Lazy list 2 no key |
| P13 | SearchScreen.kt:44 | P13, P04 | 1 linear filter over 100k books in composition 2 runs on the main thread 3 no debounce 4 no index |
| P03+P04 | TickerScreen.kt:44 | P03, P04, P05 | 1 tick read at screen top (state read too high) 2 sortedBy + map in composition without remember 3 unstable RowUi (MutableList member) rebuilt every tick 4 string format in the row body |
| P10 | build.gradle.kts, build type noR8 | P10 | minify + resource shrinking off |
| P11 | n/a (measured with the profile reset) | P11 | no baseline profile in effect |

Not defects, deliberately: A of P06 decodes on `Dispatchers.IO` so P06 does not also contain P07; both variants emit the same
measurement probes (`Probe.kt`), so instrumentation cost cancels.
