# optlab ground truth (for E5 and anyone fixing the A variants)

The optlab app carries both variants of each factor in one APK; intent extras pick the screen
(`adb shell am start -n shelftap.optlab/.MainActivity --es factor P08 --es variant A`). Source: `android/optlab/`.
A = slop (the thing to find and fix), B = the tuned reference. A independent check that a fix worked is the
benchmark (`optimization/study/e2-bench/harness/run_bench.py`) plus `results/summary.csv`, not the fixer's own claim.

Two generations of A exist. The author-built screens below (factor ids `P08`, `P06`, `P01P09`, `P13`, `P03P04`) were written by the harness
author from the obvious naive pattern; they stay in the APK as a sensitivity check. The study's A variants are the **corpus ports**
(factor ids `P08c`, `P06c`, `P01c`, `P09c`, `P13c`, `P03P04c`): E1 corpus code with mechanical changes only, rule and diffs in
`ports/README.md`, `ports/<factor>.diff`. P10 and P11 have no corpus A (build and measurement knobs). Provenance per variant: `variants.csv`.

| factor | A file:line | taxonomy | seeded defects (ground truth list) |
|---|---|---|---|
| P08 | EffectsScreen.kt:63 | P08 | 1 shadow(18.dp) 2 blur(8.dp) 3 gradient brush background 4 infinite alpha animation read in composition |
| P06 | ImagesScreen.kt:75 | P06 | 1 decodeFile of a 1500x2250 JPEG without inSampleSize 2 no memory/disk cache (decode repeats on every re-entry) |
| P01+P09 | ListScreen.kt:19 | P09, P01 | 1 Column + verticalScroll + forEach instead of a Lazy list 2 no key |
| P13 | SearchScreen.kt:46 (naiveSearch :63) | P13, P04 | 1 linear scan over 100k books that re-tokenises every row on every query, in composition 2 runs on the main thread 3 no debounce 4 no index |
| P03+P04 | TickerScreen.kt:44 | P03, P04, P05 | 1 tick read at screen top (state read too high) 2 sortedBy + map in composition without remember 3 unstable RowUi (MutableList member) rebuilt every tick 4 string format in the row body |
| P08c | corpus/p08/BookCover.kt + BookCard.kt (agy-g38flash-B1-naive-r1, BookCover.kt:216) | P08 | 1 shadow(5.dp, asymmetric RoundedCornerShape) 2 gradient brush overlay (BookSpineOverlay) 3 procedural Canvas drawing, on every grid item |
| P06c | corpus/p06/CoverBitmap.kt (codex-luna-B3-detailed-functional-r1, MainActivity.kt:39) | P06 | 1 decodeStream of the full 1500x2250 cover without inSampleSize 2 no cache |
| P01c | corpus/p01/HistoryList.kt (claude-haiku-B3-detailed-functional-r1, HistoryScreen.kt:105) | P01 | 1 LazyColumn items without key |
| P09c | corpus/p09/BookDetailSheet.kt (agy-g38flash-B3-naive-r1, BookDetailSheet.kt:300) | P09 | 1 Column + verticalScroll + forEach over the loans |
| P13c | corpus/p13/TitleSearch.kt (claude-haiku-B1-detailed-functional-r1, TitleDao.kt:31) | P13 | 1 unindexed substring LIKE over titles 2 first page 120 in title order |
| P03P04c | corpus/p09/BookDetailSheet.kt lines 58, 65, 67 (agy-g38flash-B3-naive-r1) | P03, P04 | 1 unstable List<Loan> parameter 2 DateTimeFormatter.ofPattern and filters in composition without remember |
| P10 | build.gradle.kts, build type noR8 | P10 | minify + resource shrinking off |
| P11 | n/a (measured with the profile reset) | P11 | no baseline profile in effect |

Not defects, deliberately: A of P06 decodes on `Dispatchers.IO` so P06 does not also contain P07; both variants emit the same
measurement probes (`Probe.kt`), so instrumentation cost cancels.
