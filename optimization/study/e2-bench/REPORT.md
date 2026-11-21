# E2 report: what each slop pattern costs on Android (optlab A/B benchmark)

Owner: beta-f2-3. Task: REQ#e2-1 of adhoc-1121-ai-slop-study. Written 2026-11-21 from `results/raw.csv`, `results/summary.csv`,
`results/runlog.csv`, `results/environment.json`, `results/build_facts.csv`. Base commit `a5969de+wip` (the optlab and benchmark modules were not yet committed).

**Read this first.** Everything below was measured on one **emulator** (API 34, software GPU). Absolute numbers (ms, KB, frames) are
meaningless for a phone; only A/B *ratios* and device-independent counts carry over. This is the "cost" half of *prevalence x cost*;
the "prevalence" half is E1, and the A variants here are still **provisional** (hand-written naive patterns, not yet corpus code; see §9).

## 1. Question

RQ2 (contract §2): for each performance factor in the frozen taxonomy, what does the slop version (A) cost compared with the tuned
version (B), in the same app, on the same device, with the same data? Seven factors (contract R9): P08 decorative effects, P06 image
decode and cache, P01+P09 list construction, P13 search, P03+P04 stability and heavy composition, plus the two baseline-technique
factors P10 (R8) and P11 (Baseline Profile). One sweep, P01+P09 at three row counts, shows how the cost scales.

## 2. Design

**Lab app.** `android/optlab` is a separate application module (`shelftap.optlab`), so slop never reaches the product (`android/app` is untouched).
Each factor is one screen with two implementations selected by an intent extra (`--es factor P08 --es variant A`). Both variants use the same
list container, keys (except where the key is the factor), data and probes; only the factor's pattern differs. Provenance of every variant: `variants.csv`;
the seeded defects as a ground-truth list: `GROUND_TRUTH.md`.

| factor | A (slop) | B (tuned) | scenario (cold start every iteration) |
|---|---|---|---|
| P08 | rows with shadow 18 dp, blur 8 dp, gradient, infinite alpha animation read in composition (4 effects per item) | flat bordered row | open list of 200 books, 6 swipes, 3 s idle |
| P06 | `BitmapFactory.decodeFile` of a 1500x2250 cover, no sampling, no cache (decoded off the main thread) | Coil `AsyncImage` sized to the 80x120 dp slot, memory + disk cache | open list of 300 books, 6 swipes |
| P01+P09 | `Column` + `verticalScroll` + `forEach`: every row composed eagerly, no key | `LazyColumn` with `key` and `contentType` | open list of 5000 books (default row), 6 swipes |
| P13 | `String.contains` filter over 100k books inside composition on the main thread, no debounce | SQLite FTS4 prefix index, `Dispatchers.IO`, 150 ms debounce | wait for data, type "silent river" one keystroke per ~120 ms, settle |
| P03+P04 | tick read at screen top, sort + map in composition, unstable row model rebuilt per tick | tick isolated, sort under `remember`, `@Immutable` Book keyed by id | open 3000-book list, 2 swipes, 5 s of 10 Hz ticks |
| P10 | same code, R8 minify and resource shrinking off (`noR8` build type) | R8 minify + shrinkResources (`release`) | cold startup + 3 swipes, `CompilationMode.None` |
| P11 | `CompilationMode.None()`: profile reset, app JIT-compiles as it runs | `CompilationMode.Partial(BaselineProfileMode.Require)`: profile installed and used | cold startup + 3 swipes, same `release` APK |

**Data.** 100,000 deterministic books (`BookGenerator.book(i)` is a pure function of i). 64 locally generated 1500x2250 JPEG covers (no network).
A SQLite database with an FTS4 index is built once per install (`build_facts.csv`: rows, index build time, sizes).

**Metrics (contract R8).** Primary, device-independent: recomposition counts (`Compose_recompose.count` slice count, and `st.rc.row`, the number of times a row body ran),
decoded bitmap bytes (`st.decodedBytes`), main-thread blocked ms inside app trace sections (`st_*.mainMs`, `Recomposer_recompose.mainMs`), APK / dex / resource size, search latency per query
(`st_search_query.*`), peak RSS and Java heap. Secondary, device-relative: frame CPU duration percentiles and the share of frames over 16.67 ms (`slowFrames_pct`), TTID / TTFD.
The app emits its counters and slices through `Probe.kt` into the Perfetto trace; Macrobenchmark's `TraceMetric` (`StMetrics.kt`) reads them. Both variants carry the same probes, so instrumentation cost cancels.

**Protocol.** Macrobenchmark 1.5.0, `StartupMode.COLD` for every iteration (counters start at zero), `CompilationMode.Full()` for the five slop factors
(removes JIT noise; the library documents it as stable but unrealistic), `None` for P10 and for P11-A, `Partial(Require)` for P11-B.
ABAB interleaving in blocks: 4 rounds, each round runs A then B (B then A on odd rounds), 3 iterations per call, so n = 12 iterations per variant.
`androidx.benchmark.suppressErrors=EMULATOR,LOW-BATTERY` (the library otherwise refuses to run on an emulator). Locks per contract R1: `gradle.lock` and `bench.lock` held for the whole run; nothing else built while measuring.
Statistics (`harness/summarize.py`, standard library only): median and IQR per variant; Δ% = (B − A) / A; 95% bootstrap CI of Δ% (10,000 resamples, seed 7); Cliff's δ (positive: A larger);
two-sided Mann-Whitney p (tie-corrected normal approximation, in `summary.csv` only). The p values are descriptive: no confirmatory family is declared for E2, so no multiplicity correction is applied.
Static size metrics (P10 APK, dex, resources) are one deterministic value per variant (n = 1, no interval).

## 3. Environment

| item | value |
|---|---|
| device | AVD `lowend34`: sdk_gphone64_x86_64, Android API 34, Physical size: 720x1280 / Physical density: 280, 2 cores, 2021528 kB RAM, heap 256 MB setting |
| emulator | `-no-snapshot -no-boot-anim -no-audio -memory 2048 -cores 2`, GPU swiftshader_indirect (software), headless -no-window, cold boot, Google APIs x86_64 image, KVM |
| host | 16 cores, 15 GB RAM, other agents' processes running; MemAvailable between blocks never below 6540 MB, swap in use never above 3426 MB (`build_facts.csv`) |
| toolchain | AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Coil 3.6.3, coroutines 1.11.0, Macrobenchmark 1.5.0, UiAutomator 2.4.1, profileinstaller 1.4.1, JDK 21 |
| APKs (sha256) | optlab-release `478cb846be5f54bb…`, optlab-noR8 `1157f22ed64991db…`, benchmark-release `31ff92fd1314454d…` (full hashes in `environment.json`) |
| run | started 2026-11-21T05:42:04+0700; 76 am-instrument calls in `runlog.csv`; the only failures are P01+P09/n=20000 variant A (§6) |

Docs consulted instead of context7 (not available in this pane; alpha accepted this in RE#q1). All accessed 2026-11-21:

- https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview (Macrobenchmark metrics, compilation modes)
- https://developer.android.com/jetpack/androidx/releases/benchmark (release notes, AGP 9 notes)
- https://developer.android.com/topic/performance/baselineprofiles/overview (Baseline Profiles)
- https://dl.google.com/dl/android/maven2/androidx/benchmark/benchmark-macro-junit4/maven-metadata.xml, .../androidx/test/uiautomator/uiautomator/maven-metadata.xml, .../androidx/profileinstaller/profileinstaller/maven-metadata.xml (latest stable per artifact)
- https://coil-kt.github.io/coil/ (Coil 3 AsyncImage)

Version pins check against maven-metadata on 2026-11-21: benchmark-macro-junit4 stable 1.5.0 (1.6.0-alpha02 is newer but alpha, not used), uiautomator 2.4.1 (latest), profileinstaller 1.4.1 (latest). All three pinned to the latest stable; the build and the benchmark run were done with them under AGP 9.5.1 (no probe needed). The androidx.baselineprofile Gradle plugin is not used (see §9).
The AndroidX Benchmark release notes list "Baseline Profile Gradle Plugin no longer requires newDsl=false in AGP 9.0" (1.5.0 alphas) and a bumped maximum recommended AGP; version 1.5.0 (stable) was used.

## 4. Results

Medians with IQR in brackets; Δ% is B relative to A (negative: B lower). All rows are in `results/summary.csv` (full metric list); raw per-iteration values in `raw.csv`.

### P08: decorative effects in list items
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Cliff's δ | n |
|---|---|---|---|---|---|---|---|
| `st.fx.perItem` | count | 4 (0) | 0 (0) | -100 | [-100, -100] | 1 | 12 |
| `st.rc.row` | count | 3230 (380.5) | 22 (1) | -99.3 | [-99.4, -99.3] | 1 | 12 |
| `Compose_recompose.count` | count | 3232 (382) | 23 (1) | -99.3 | [-99.4, -99.3] | 1 | 12 |
| `Recomposer_recompose.mainMs` | ms | 291.3 (67.93) | 0.5572 (0.08945) | -99.8 | [-99.8, -99.8] | 1 | 12 |
| `Compose_recompose.mainMs` | ms | 137.2 (29.18) | 10.21 (4.329) | -92.6 | [-93.9, -89.9] | 1 | 12 |
| `frameDurationCpuMs_p90` | ms | 34.09 (3.841) | 4.229 (0.106) | -87.6 | [-87.8, -84.2] | 1 | 12 |
| `slowFrames_pct` | % | 99.38 (4.636) | 0.4141 (0.1072) | -99.6 | [-99.6, -99.4] | 1 | 12 |
| `memoryHeapSizeMaxKb` | KB | 1.997e+04 (1302) | 6000 (53) | -70 | [-72, -69.8] | 1 | 12 |
| `memoryRssAnonMaxKb` | KB | 5.358e+04 (1349) | 3.68e+04 (180) | -31.3 | [-33.1, -31] | 1 | 12 |

Reading: the infinite alpha animation read in composition forces every visible row to recompose every frame (`st.rc.row`, `Compose_recompose.count`), which is the device-independent cost. The frame figures are inflated by the software GPU, which makes shadow and blur far costlier than a phone GPU does, so use them as direction only.

### P06: image decode and cache
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Cliff's δ | n |
|---|---|---|---|---|---|---|---|
| `st.decodedBytes` | bytes | 3.375e+08 (0) | 2.94e+06 (0) | -99.1 | [-99.1, -99.1] | 1 | 12 |
| `st.decodes` | count | 25 (0) | 25 (0) | 0 | [0, 0] | 0 | 12 |
| `memoryRssAnonMaxKb` | KB | 2.077e+05 (212) | 6.261e+04 (167) | -69.9 | [-69.9, -69.8] | 1 | 12 |
| `memoryHeapSizeMaxKb` | KB | 7328 (16) | 6611 (16.5) | -9.78 | [-10.2, -9.77] | 1 | 12 |
| `st_img_decode.sumMs` | ms | 2324 (415) | 0 (0) | -100 | [-100, -100] | 1 | 12 |
| `st_img_decode.maxMs` | ms | 384.8 (86.28) | 0 (0) | -100 | [-100, -100] | 1 | 12 |
| `frameDurationCpuMs_p90` | ms | 60.33 (1.049) | 4.195 (0.1732) | -93 | [-93.3, -92.9] | 1 | 12 |
| `slowFrames_pct` | % | 40.44 (3.123) | 1.544 (0.5424) | -96.2 | [-96.8, -95] | 1 | 12 |

Reading: both variants perform the same number of decodes in the scenario; A decodes the full 1500x2250 bitmap each time, B decodes at slot size. Decoded bytes are the primary metric. The product's memory target is 150 MB while browsing (CLAUDE.md); A's peak anonymous RSS is the row to compare with it.

### P01+P09: list construction (main row n = 5000)
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Cliff's δ | n |
|---|---|---|---|---|---|---|---|
| `st.rc.row` | count | 5000 (0) | 41 (0) | -99.2 | [-99.2, -99.2] | 1 | 12 |
| `Compose_recompose.count` | count | 1 (0) | 53 (0) | 5.2e+03 | [5.2e+03, 5.2e+03] | -1 | 12 |
| `Compose_recompose.mainMs` | ms | 440.3 (52.51) | 27.53 (3.063) | -93.7 | [-94.3, -93.2] | 1 | 12 |
| `timeToInitialDisplayMs` | ms | 2009 (144.7) | 433 (31.28) | -78.4 | [-80, -77.4] | 1 | 12 |
| `timeToFullDisplayMs` | ms | 2067 (135.6) | 465 (37.04) | -77.5 | [-79.3, -76.6] | 1 | 12 |
| `memoryHeapSizeMaxKb` | KB | 2.033e+05 (4) | 8561 (16.75) | -95.8 | [-95.8, -95.8] | 1 | 12 |
| `memoryRssAnonMaxKb` | KB | 2.455e+05 (1.351e+04) | 3.944e+04 (93) | -83.9 | [-84.3, -83.3] | 1 | 12 |
| `frameDurationCpuMs_p90` | ms | 10.2 (0.7388) | 4.114 (0.1298) | -59.7 | [-62.5, -58.6] | 1 | 12 |
| `slowFrames_pct` | % | 0.4914 (1.947) | 0.4292 (0.4152) | -12.7 | [-78, 79.3] | 0.167 | 12 |

Reading: A composes every row before the first frame (`st.rc.row` is the row count), so time to first display and memory grow with the list; B composes only the visible rows. `Compose_recompose.count` is *higher* for B on purpose: lazy composition is spread over many small passes, while A does one huge composition. That row is not a B regression; `Compose_recompose.mainMs` shows the total.

### P13: search over 100k books
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Cliff's δ | n |
|---|---|---|---|---|---|---|---|
| `st.search.queries` | count | 12 (0) | 8 (0) | -33.3 | [-33.3, -33.3] | 1 | 12 |
| `st_search_query.count` | count | 12 (0) | 8 (0) | -33.3 | [-33.3, -33.3] | 1 | 12 |
| `st_search_query.mainMs` | ms | 517.5 (76.28) | 0 (0) | -100 | [-100, -100] | 1 | 12 |
| `st_search_query.sumMs` | ms | 517.5 (76.28) | 22.2 (3.626) | -95.7 | [-96.2, -95.2] | 1 | 12 |
| `st_search_query.maxMs` | ms | 57.08 (45.42) | 11.2 (6.112) | -80.4 | [-89.8, -73.5] | 1 | 12 |
| `frameDurationCpuMs_p90` | ms | 51.91 (5.578) | 32.03 (6.976) | -38.3 | [-48.4, -32.6] | 0.986 | 12 |
| `slowFrames_pct` | % | 36.92 (3.173) | 18.87 (3.47) | -48.9 | [-55.9, -43] | 1 | 12 |
| `memoryHeapSizeMaxKb` | KB | 6.434e+04 (3228) | 7029 (48) | -89.1 | [-89.6, -89.1] | 1 | 12 |
| `memoryRssAnonMaxKb` | KB | 9.783e+04 (3396) | 3.988e+04 (284) | -59.2 | [-60.6, -59.1] | 1 | 12 |

Reading: A runs the filter in composition, so every query blocks the main thread (`mainMs`); B runs on the IO dispatcher (`mainMs` is 0) and the debounce reduces the number of queries. The product target is search under 50 ms over 100k books: compare `st_search_query.maxMs` (slowest single query) for A and B above; on an emulator the comparison is relative. The variants do not return identical result sets (A is substring match, B is token-prefix AND), so result counts are not compared.

### P03+P04: stability and heavy work in composition
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Cliff's δ | n |
|---|---|---|---|---|---|---|---|
| `st.rc.row` | count | 1046 (49) | 27 (0) | -97.4 | [-97.4, -97.3] | 1 | 12 |
| `st_sort.count` | count | 76 (0) | 1 (0) | -98.7 | [-98.7, -98.7] | 1 | 12 |
| `st_sort.mainMs` | ms | 1237 (25.31) | 20.23 (5.429) | -98.4 | [-98.5, -98] | 1 | 12 |
| `Compose_recompose.count` | count | 2134 (96.25) | 103 (0) | -95.2 | [-95.2, -95] | 1 | 12 |
| `Recomposer_recompose.mainMs` | ms | 1296 (28.75) | 17.22 (2.143) | -98.7 | [-98.7, -98.5] | 1 | 12 |
| `Compose_recompose.mainMs` | ms | 1299 (37.28) | 45.33 (4.983) | -96.5 | [-96.7, -96.2] | 1 | 12 |
| `frameDurationCpuMs_p90` | ms | 22.77 (0.3747) | 4.372 (0.1387) | -80.8 | [-81.4, -80.6] | 1 | 12 |
| `slowFrames_pct` | % | 64.52 (3.725) | 1.001 (0.8801) | -98.4 | [-99, -97.3] | 1 | 12 |

Reading: the ticker updates 10 times a second; A re-sorts and rebuilds the whole list and recomposes the visible rows on each tick, B recomposes only the badge. With strong skipping (on by default in the current Compose compiler), instability alone is not what costs here: the fresh unstable instances built in composition defeat skipping.

### P10: R8 (baseline technique)
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Cliff's δ | n |
|---|---|---|---|---|---|---|---|
| `apk_bytes` | bytes | 2.408e+07 (0) | 2.319e+06 (0) | -90.4 | — | — | 1 |
| `dex_bytes` | bytes | 2.345e+07 (0) | 2.096e+06 (0) | -91.1 | — | — | 1 |
| `resources_bytes` | bytes | 4.958e+05 (0) | 9.096e+04 (0) | -81.7 | — | — | 1 |
| `timeToInitialDisplayMs` | ms | 496.3 (37.89) | 462.7 (23.94) | -6.76 | [-13, -2.68] | 0.764 | 12 |
| `timeToFullDisplayMs` | ms | 548.6 (64.52) | 494.3 (28.6) | -9.9 | [-14.2, -1.76] | 0.611 | 12 |
| `frameDurationCpuMs_p90` | ms | 6.09 (0.3958) | 4.927 (0.1783) | -19.1 | [-24.5, -17.3] | 1 | 12 |
| `slowFrames_pct` | % | 2.09 (1.042) | 1.667 (0.8369) | -20.3 | [-42.9, 26.1] | 0.208 | 12 |

Reading: the size rows are the device-independent result (n = 1, deterministic). Startup differences are small on this trivial screen and, like all ms figures, relative.

### P11: Baseline Profile (baseline technique)
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Cliff's δ | n |
|---|---|---|---|---|---|---|---|
| `timeToInitialDisplayMs` | ms | 459.7 (26.09) | 443 (14.83) | -3.64 | [-8.25, -0.867] | 0.611 | 12 |
| `timeToFullDisplayMs` | ms | 483.5 (42.31) | 481.6 (34.66) | -0.409 | [-5.94, 5.89] | -0.0139 | 12 |
| `frameDurationCpuMs_p90` | ms | 5.103 (0.2116) | 4.516 (0.1895) | -11.5 | [-14.6, -7.52] | 0.833 | 12 |
| `slowFrames_pct` | % | 1.66 (1.014) | 1.646 (0.8143) | -0.823 | [-50, 32.6] | 0.521 | 12 |

Reading: the profile is generated by `ProfileGenerator` (`BaselineProfileRule`) on the non-minified build and sits in `android/optlab/src/main/baseline-prof.txt` (rule and binary sizes in `build_facts.csv`). B also benefits from the profiles AndroidX libraries embed; A resets all of it. The effect on a one-screen lab app is modest; expect a larger gain on an app with a long startup path. Rows whose CI includes 0 are not distinguishable from noise at n = 12.

## 5. What these numbers say about the hypothesis

H3 (visual-slop tax): the decorative-effect pattern (P08) multiplies recomposition work and, on this device class, frame cost; the effect density is a countable construct (`st.fx.perItem`: A and B values in the P08 table). The count-based rows hold on any device; the frame rows need a physical-device check (contract R9: not available).
The other slop factors show the same shape: a real cost in the primary device-independent metric, removed by the textbook fix. That is the baseline-technique layer. The novel claim (how often assistants emit these patterns) comes from E1.

## 6. Scaling of P01+P09

| rows n | A time to initial display (ms) | B time to initial display (ms) | A peak anon RSS (KB) | B peak anon RSS (KB) | A row bodies run | B row bodies run |
|---|---|---|---|---|---|---|
| 1000 | 624.3 | 421.2 | 1.206e+05 | 3.789e+04 | 1000 | 41 |
| 5000 | 2009 | 433 | 2.455e+05 | 3.944e+04 | 5000 | 41 |
| 20000 | no run: 8 of 8 attempts failed | 461.1 | no run | 45254 | no run | 41 |

At 20,000 rows A never completes a launch: a manual reproduction logs `OutOfMemoryError` at the device heap growth limit (`results/p01p09-n20000-A-oom.txt`, limit in `build_facts.csv`), and the harness recorded "target package is not running" or "unable to confirm activity launch" in every attempt (`runlog.csv`). The catalog the product targets has 100,000 books, five times larger. B's n=20000 values are medians of 12 raw iterations (`raw.csv`, factor id `P01+P09/n=20000`). The n=1000 and n=5000 rows come from `summary.csv` (factor ids `P01+P09/n=1000` and `P01+P09`).

## 7. From cost to expected cost (prevalence x cost)

Expected cost of a pattern in a population of generated apps = (share of apps that contain it, E1) x (cost when present, this report). The pairing below fixes which E2 metric stands for each taxonomy ID; E1 prevalence is filled in when the full corpus is analysed (`e1-corpus/results/findings.csv`), not here.

| taxonomy | E2 factor | cost metric (A − B, medians) | value | unit | E1 prevalence |
|---|---|---|---|---|---|
| P08 | P08 | Recomposer_recompose.mainMs | 290.7 | ms | pending E1 |
| P06 | P06 | st.decodedBytes | 3.346e+8 | bytes | pending E1 |
| P01, P09 | P01+P09 | timeToInitialDisplayMs | 1576 | ms | pending E1 |
| P13 | P13 | st_search_query.mainMs | 517.5 | ms | pending E1 |
| P03, P04, P05 | P03+P04 | Recomposer_recompose.mainMs | 1279 | ms | pending E1 |
| P10 | P10 | apk_bytes | 2.176e+7 | bytes | pending E1 |
| P11 | P11 | timeToInitialDisplayMs | 16.7 | ms | pending E1 |

## 8. Threats to validity

- **Emulator, not a phone.** x86_64 guest on KVM with a software GPU (`swiftshader_indirect`). Frame and startup milliseconds are relative; blur and shadow cost is inflated by software rendering. Macrobenchmark errors suppressed with `EMULATOR`. No physical device was available (user decision, contract §6b).
- **Host contention.** Other agents ran on the same host. The final run saw MemAvailable ≥ 6540 MB and swap ≤ 3426 MB between blocks (`build_facts.csv`); an earlier attempt was cut short by the kernel OOM killer (§9). ABAB interleaving spreads drift across A and B but does not remove it.
- **n = 12 per variant, one device session, one data seed.** Intervals are bootstrap on 12 points; Cliff's δ and p are descriptive.
- **A is hand-written.** The effect size depends on my constants (4 effects, 5000 rows, 12 keystrokes at about 8 per second, 10 Hz ticks). A will be replaced by E1 corpus code; the direction should hold, the magnitudes will change.
- **Semantics differ in P13.** A is substring match, B is token-prefix AND; B returns at most 50 rows and A all matches. Latency is compared, not result sets.
- **Platform SQLite has no FTS5.** B uses FTS4 (no bm25 rank; docid order). FTS5 would add ranking, not change the latency class (`build_facts.csv`: device_fts5 absent).
- **Cold start per iteration with warm page cache.** Iterations after the first of a call hit a warm file cache; first-install conditions are reproduced only by P11-A's profile reset.
- **Counters measure scope executions, not wall time.** `st.rc.row` counts how often a row body ran; it is exact, but wall-time cost is in the `mainMs` rows.
- **P10 A differs from B in two switches** (minify and resource shrinking) and in application id (`.nor8`), so the two APKs were installed side by side.
- **P11 baseline profile** is generated on the emulator from the lab journeys; its rules cover the lab app, not the product app.

## 9. Deviations, incidents and open items

- **A variants are provisional** (`variants.csv`; the `build_type` column of `raw.csv` carries `;A-provisional`). E1 corpus code did not exist when the harness finished (`optimization/study/e1-corpus/results` was absent). The step "swap A to corpus code" is open: replace the A composables and re-run `harness/run_bench.py`; the harness, scenarios and statistics do not change.
- **Baseline profile without the Gradle plugin.** Generated with `BaselineProfileRule` and copied into `src/main`, instead of the `androidx.baselineprofile` plugin: the plugin adds build types to the consumer app, which collides with the extra `noR8` build type, and the manual path avoids an AGP-9 compatibility risk. The artifact is the same one the plugin produces.
- **Run history.** Earlier runs (a one-iteration pilot, then a full run) exposed four defects in the lab itself and were discarded: the recomposition probe was a skippable composable and undercounted, B of P03+P04 sorted with a cheaper comparator than A, `reportFullyDrawn` produced no later frame on a static screen (StartupTimingMetric threw), and P10/P11 used a startup journey with no scroll. All fixed; only the final run is in `results/`.
- **Infrastructure incident.** While the first builds ran, host memory was exhausted: the kernel OOM killer killed the emulator and the Gradle daemon several times (and other agents' sessions on this host). Builds then ran with a 1.3 GB heap and in-process Kotlin, the emulator was shut down during builds, and a retry wrapper re-ran a build when the daemon disappeared. The final run is clean (`runlog.csv`).
- **P01+P09 at 20,000 rows, variant A** cannot run (§6); it is reported as a failure, not imputed.
- Unmeasured by design (contract R14): P12, S10, S12, U06 and the security factors. E3/E4/E5 consume the APKs in `3S-study-work/apk/` (`README.txt` there).

## 10. Reproduce

```bash
export TMPDIR=/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/tmp
cd android && flock <study-work>/gradle.lock ./gradlew :optlab:assembleRelease :optlab:assembleNoR8 :benchmark:assembleRelease
# emulator: AVD "lowend34" (API 34 google_apis x86_64, 2 cores, 2 GB, 720x1280@280, swiftshader_indirect); boot flags in environment.json
adb install -r -t optlab/build/outputs/apk/release/optlab-release.apk   # likewise optlab-noR8.apk and benchmark-release.apk
adb shell am instrument -w -e class shelftap.benchmark.FactorBenchmark#seed shelftap.benchmark/androidx.test.runner.AndroidJUnitRunner   # once per package
flock <study-work>/gradle.lock flock <study-work>/bench.lock python3 optimization/study/e2-bench/harness/run_bench.py --factors "P08,P06,P01+P09,P13,P03+P04,P10,P11,P01+P09/n=1000,P01+P09/n=20000"
python3 optimization/study/e2-bench/harness/summarize.py
python3 optimization/study/e2-bench/harness/record_demo.py   # demo clips
```

Demo clips (A vs B, same finger input): `/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/video/` (P08, P06, P01P09 scroll; P13 search).
