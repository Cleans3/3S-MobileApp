# E2 report: what each slop pattern costs on Android (optlab A/B benchmark)

Owner: beta-f2-3. Task: REQ#e2-1 of adhoc-1121-ai-slop-study. Written 2026-11-21 from `results/raw.csv`, `summary.csv`, `summary_all.csv`, `exclusions.csv`,
`runlog.csv`, `iterlog.csv`, `environment.json`, `build_facts.csv`, `p13_equivalence.csv`. Base commit `fbf919f+wip` (the optlab and benchmark modules and the ports are not committed yet).

**Read this first.** Everything below was measured on one **emulator** (API 34, software GPU). Absolute numbers (ms, KB, frames) say little about a phone;
only A/B *ratios* and device-independent counts carry over. This is the "cost" half of *prevalence x cost*; the "prevalence" half is E1 and is joined in §7.
The A variants are **E1 corpus code** (ports with mechanical changes only, protocol A4, `ports/README.md`), so this is the confirmatory run (C5) for
all seven factors. 
Two results are null or weak and are reported as such: P03+P04 shows no measurable cost in its scenario (§4), and P13 B is much faster per query but not on frames (§4).

## 1. Question

RQ2 (contract §2): for each performance factor in the frozen taxonomy, what does the slop version (A) cost compared with the tuned
version (B), in the same app, on the same device, with the same data? Seven factors (contract R9): P08 decorative effects, P06 image
decode and cache, P01+P09 list construction, P13 search, P03+P04 stability and heavy composition, plus the two baseline-technique
factors P10 (R8) and P11 (Baseline Profile).

## 2. Design

**Lab app.** `android/optlab` is a separate application module (`shelftap.optlab`), so slop never reaches the product (`android/app` is untouched).
Each factor is one screen with two implementations selected by an intent extra (`--es factor P06c --es variant A`). Both variants use the same
list container, data and probes; only the factor's pattern differs. **A is a port of the first corpus candidate (rank order of `e1-corpus/results/a_candidates.csv`) that ports with mechanical changes only**
(package, imports, data-source binding, probes; the offending construct is untouched; diffs in `ports/<factor>.diff`). **B is the tuned reference** (generated from A where the fix is local, hand-written otherwise).
Provenance of every variant: `variants.csv`; the seeded defects as a ground-truth list: `GROUND_TRUTH.md`. The five author-constructed A variants of the pilot stay in the APK for a separate sensitivity run (§9).

| factor | A (corpus code, slop) | B (tuned) | scenario (cold start every iteration) |
|---|---|---|---|
| P08 | `BookCard` + `BookCover` of agy-g38flash-B1-naive-r1 in a `LazyVerticalGrid`: shadow 5 dp (asymmetric shape), gradient spine overlay, procedural Canvas, per card | same card, cover replaced by a flat box of the same slot | grid of 200 books, 6 swipes, 3 s idle |
| P06 | `CoverBitmap` of codex-luna-B3-detailed-functional-r1: `decodeStream` of the full 1500x2250 cover, no sampling, no cache | Coil `AsyncImage` in the same 150x210 dp slot, memory + disk cache | list of 300 books, 6 swipes |
| P01+P09 | `BookDetailSheet` of agy-g38flash-B3-naive-r1 (P09 file): `Column` + `verticalScroll` + `forEach` over the loans | `LazyColumn` with `key` and `contentType` | sheet with 2000 loans, 6 swipes |
| P01+P09 / P01 | `HistoryList` of claude-haiku-B3-detailed-functional-r1: `LazyColumn items(list)` without key | same list with `key` and `contentType` | list of 5000 items, 6 swipes (primary phase only) |
| P13 | `TitleSearch` of claude-haiku-B1-detailed-functional-r1: unindexed substring `LIKE` over 100k titles, first page of 120 rows in title order, debounce 200 ms | SQLite FTS4 token-prefix `MATCH`, same debounce, same 120-row title-ordered page | wait for data, type "silent river" one keystroke per ~120 ms, settle |
| P03+P04 | `BookDetailSheet` lines 58, 65, 67 (same corpus file as P09): unstable `List<Loan>` parameter, `DateTimeFormatter.ofPattern` and filters in composition without `remember`; the parent ticks at 10 Hz and passes a fresh equal list | stable `LoanList` wrapper and `remember` | sheet with 300 loans, 2 swipes, 5 s of ticks |
| P10 | same code, R8 minify and resource shrinking off (`noR8` build type) | R8 minify + shrinkResources (`release`) | cold startup + 3 swipes, `CompilationMode.None` |
| P11 | `CompilationMode.None()`: profile reset, app JIT-compiles as it runs | `CompilationMode.Partial(BaselineProfileMode.Require)`: profile installed and used | cold startup + 3 swipes, same `release` APK |

P10 and P11 have no corpus A: they are build and measurement knobs, so A and B run the same code.

**Data.** 100,000 deterministic books (`BookGenerator.book(i)` is a pure function of i). 64 locally generated 1500x2250 JPEG covers (no network).
A SQLite database with an FTS4 index is built once per install (`build_facts.csv`: rows, index build time, sizes).

**Metrics (contract R8).** Primary, device-independent: recomposition counts (`Compose_recompose.count` slice count, and `st.rc.row`, the number of times a row body ran),
decoded bitmap bytes (`st.decodedBytes`), main-thread blocked ms inside app trace sections (`st_*.mainMs`, `Recomposer_recompose.mainMs`), APK / dex / resource size, search latency per query
(`st_search_query.*`), peak RSS and Java heap. Secondary, device-relative: frame CPU duration percentiles and the share of frames over 16.67 ms (`slowFrames_pct`), TTID / TTFD.
The app emits its counters and slices through `Probe.kt` into the Perfetto trace; Macrobenchmark's `TraceMetric` (`StMetrics.kt`) reads them. Both variants carry the same probes, so instrumentation cost cancels.
Where only A's code path carries a probe the B row reads 0 and means "not instrumented", not "free" (`st_img_decode.*`: B decodes inside Coil).

**Protocol.** Macrobenchmark 1.5.0, `StartupMode.COLD` for every iteration (counters start at zero), `CompilationMode.Full()` for the five slop factors
(removes JIT noise; the library documents it as stable but unrealistic), `None` for P10 and for P11-A, `Partial(Require)` for P11-B.
ABAB interleaving in blocks: 4 rounds, each round runs A then B (B then A on odd rounds), 3 iterations per call, so 12 iterations per variant before exclusions.
`androidx.benchmark.suppressErrors=EMULATOR,LOW-BATTERY` (the library otherwise refuses to run on an emulator).
**Two phases, counts first.** Phase `primary` measures counters, trace slices, memory and startup timing; phase `frames` then re-runs the same ABAB design with `FrameTimingMetric` only.
Frame numbers and counts therefore come from different iterations of the same scenario.
**Locks and host conditions** (protocol §15 A2, A7). While a measured block runs the harness holds `bench.lock` (exclusive, one `am instrument` call) and writes `3S-study-work/bench.hold` (its pid) for the whole window: peers' generators wait on the hold file, so no one starts a build or a generation between two blocks; after every factor group the harness releases both and yields for 120 s (`runlog.csv` rows with result `yield`).
At every iteration start it samples host MemAvailable and the 1-minute load average, and the emulator process's (qemu) major page faults between iteration start and end (markers logged by the benchmark, `STBENCH iter-begin/iter-end`).
**Pre-declared exclusion (A7):** an iteration is dropped if MemAvailable < 1024 MB at its start or the qemu process took a major page fault during it. The emulator runs in its own systemd scope with `MemorySwapMax=0` so peers' load cannot page its memory out; the first run without that scope lost every iteration to qemu major faults (VmSwap 1.3 GB, 70-110 faults per iteration). A (factor, variant, phase) that ends short of 12 kept iterations gets up to 3 extra rounds; it is host-limited if more than half is lost after them (none was).
`raw.csv` holds kept iterations only (the primary analysis); `raw_all.csv` holds every measured iteration with its status (the sensitivity analysis, `summary_all.csv`); `iterlog.csv` has the sampled host values per iteration.
Statistics (`harness/summarize.py`, standard library only): median and IQR per variant; Δ% = (B − A) / A; 95% bootstrap CI of Δ% (10,000 resamples, seed 20261121 as in protocol §4.2); Cliff's δ (positive: A larger);
two-sided Mann-Whitney p (tie-corrected normal approximation, in `summary.csv` only). The p values are descriptive: no confirmatory family is declared for E2, so no multiplicity correction is applied.
`frameOverrunMs_*` crosses zero, so a percentage change is meaningless there: `delta_pct` is empty for those rows and the absolute difference `delta_abs` (B − A, ms) with its own bootstrap CI is reported.
Static size metrics (P10 APK, dex, resources) are one deterministic value per variant (n = 1, no interval).
The corpus factors run on one APK build (`optlab-release.apk` `367066ea…`); P10 and P11 were measured earlier, on the build in `3S-study-work/apk/p10p11-run/` (§3), with the same code and harness.

## 3. Environment

| item | value |
|---|---|
| device | AVD `lowend34`: sdk_gphone64_x86_64, Android API 34, Physical size: 720x1280 / Physical density: 280, 2 cores, 2021528 kB RAM, heap 256 MB setting |
| emulator | `-no-snapshot -no-boot-anim -no-audio -no-window -memory 2048 -cores 2 -gpu swiftshader_indirect -port 5570`, in a `systemd-run --user --scope -p MemorySwapMax=0` scope, cold boot, Google APIs x86_64 image, KVM |
| host | 16 cores, 15 GB RAM, other agents' processes running; MemAvailable at the end of a block ranged 1454 to 5985 MB, swap in use up to 8673 MB across the measured blocks in `runlog.csv`, which is why the exclusion rule exists |
| toolchain | AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Coil 3.6.3, coroutines 1.11.0, Macrobenchmark 1.5.0, UiAutomator 2.4.1, profileinstaller 1.4.1, JDK 21 |
| APKs (sha256) | corpus factors: optlab-release `367066eaa2d4ef2e…`, optlab-noR8 `4d025baec525a100…`, benchmark-release `645a82301d499b26…` (`environment.json`). P10/P11 run: optlab-release `e24bab09bea573b5…`, optlab-noR8 `eee9cd7c8e5cc960…`, benchmark-release `645a82301d499b26…` (`environment-p10p11.json`) |
| runs | P10 and P11: 114 iterations, `environment-p10p11.json` (commit `6c06385+wip`). Corpus factors: `runlog.csv` rows from 21:19 to 22:35 +07, 2026-11-21, with `bench.hold`; P01 sub-factor, then P08 (swangle_indirect, `environment-p08-swangle.json`) and the exploratory control `P01+P09@swangle` afterwards. |

Docs consulted instead of context7 (not available in this pane; alpha accepted this in RE#q1). All accessed 2026-11-21:

- https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview (Macrobenchmark metrics, compilation modes)
- https://developer.android.com/jetpack/androidx/releases/benchmark (release notes, AGP 9 notes)
- https://developer.android.com/topic/performance/baselineprofiles/overview (Baseline Profiles)
- https://dl.google.com/dl/android/maven2/androidx/benchmark/benchmark-macro-junit4/maven-metadata.xml, .../androidx/test/uiautomator/uiautomator/maven-metadata.xml, .../androidx/profileinstaller/profileinstaller/maven-metadata.xml (latest stable per artifact)
- https://coil-kt.github.io/coil/ (Coil 3 AsyncImage)

Version pins check against maven-metadata on 2026-11-21: benchmark-macro-junit4 stable 1.5.0 (1.6.0-alpha02 is newer but alpha, not used), uiautomator 2.4.1 (latest), profileinstaller 1.4.1 (latest). The androidx.baselineprofile Gradle plugin is not used (§9).

## 4. Results

Medians with IQR in brackets; Δ% is B relative to A (negative: B lower). The tables show the headline metrics; all rows are in `results/summary.csv`, raw per-iteration values in `raw.csv`. n is the smaller kept count of the two variants.

### 4.0 Iterations run, kept and dropped

Dropped = the pre-declared exclusion (§2). Every drop in this run was a qemu major fault except 1 (an iteration with no host sample); none was a MemAvailable drop. No group was host-limited. Corpus factors including the P01 sub-factor: 282 iterations run, 243 kept, 39 dropped; P10 and P11: 114 run, 109 kept, 5 dropped. The worst groups are P06 primary B (6 of 18 dropped), P01 sub-factor primary A (6 of 18) and P03+P04 primary B (5 of 18); all were topped up with extra rounds to 12 or more kept iterations.

| factor | phase | variant | iterations run | kept | dropped | of which mem < 1024 MB | of which swap-in | no sample |
|---|---|---|---|---|---|---|---|---|
| P08 | frames | A | 12 | 12 | 0 | 0 | 0 | 0 |
| P08 | frames | B | 12 | 12 | 0 | 0 | 0 | 0 |
| P08 | primary | A | 18 | 14 | 4 | 0 | 4 | 0 |
| P08 | primary | B | 18 | 17 | 1 | 0 | 1 | 0 |
| P06 | frames | A | 15 | 14 | 1 | 0 | 1 | 0 |
| P06 | frames | B | 15 | 14 | 1 | 0 | 1 | 0 |
| P06 | primary | A | 18 | 16 | 2 | 0 | 2 | 0 |
| P06 | primary | B | 18 | 12 | 6 | 0 | 6 | 0 |
| P01+P09 | frames | A | 15 | 13 | 2 | 0 | 2 | 0 |
| P01+P09 | frames | B | 15 | 13 | 2 | 0 | 2 | 0 |
| P01+P09 | primary | A | 12 | 12 | 0 | 0 | 0 | 0 |
| P01+P09 | primary | B | 12 | 12 | 0 | 0 | 0 | 0 |
| P13 | frames | A | 15 | 13 | 2 | 0 | 2 | 0 |
| P13 | frames | B | 15 | 12 | 3 | 0 | 3 | 0 |
| P13 | primary | A | 15 | 14 | 1 | 0 | 1 | 0 |
| P13 | primary | B | 15 | 12 | 3 | 0 | 3 | 0 |
| P03+P04 | frames | A | 15 | 13 | 2 | 0 | 2 | 0 |
| P03+P04 | frames | B | 15 | 15 | 0 | 0 | 0 | 0 |
| P03+P04 | primary | A | 18 | 17 | 1 | 0 | 1 | 0 |
| P03+P04 | primary | B | 18 | 13 | 5 | 0 | 5 | 0 |
| P10 | frames | A | 15 | 14 | 1 | 0 | 1 | 0 |
| P10 | frames | B | 15 | 14 | 1 | 0 | 1 | 0 |
| P10 | primary | A | 15 | 14 | 1 | 0 | 1 | 0 |
| P10 | primary | B | 15 | 15 | 0 | 0 | 0 | 0 |
| P11 | frames | A | 12 | 12 | 0 | 0 | 0 | 0 |
| P11 | frames | B | 12 | 12 | 0 | 0 | 0 | 0 |
| P11 | primary | A | 15 | 13 | 2 | 0 | 2 | 0 |
| P11 | primary | B | 15 | 15 | 0 | 0 | 0 | 0 |
| P01+P09/P01 | primary | A | 18 | 12 | 6 | 0 | 5 | 1 |
| P01+P09/P01 | primary | B | 18 | 16 | 2 | 0 | 2 | 0 |
| P01+P09@swangle | frames | A | 15 | 15 | 0 | 0 | 0 | 0 |
| P01+P09@swangle | frames | B | 15 | 14 | 1 | 0 | 1 | 0 |
| P01+P09@swangle | primary | A | 15 | 14 | 1 | 0 | 1 | 0 |
| P01+P09@swangle | primary | B | 15 | 13 | 2 | 0 | 2 | 0 |

**Sensitivity analysis** (`summary_all.csv`: the same statistics over every measured iteration, dropped ones included). Of 93 comparable rows for the six measured factors the sign of the effect is the same in 92. Five rows change whether the 95% interval spans 0: P13 `frameDurationCpuMs_p99` (primary spans 0, all excludes 0, B worse by 22%), P13 `memoryRssAnonMaxKb`, `st_search_query.maxMs` and `st_search_query.sumMs` (primary excludes 0; with the contended iterations the interval widens to include 0, the median Δ% stays at −58.7% and −85.7%), and P10 `slowFrames_pct` (primary excludes 0, all spans 0). The largest shift of a Δ% median is 26.7 points (P03+P04 `Recomposer_recompose.maxMs`, a tail metric). No conclusion below depends on the choice.

### P08: decorative effects in grid cards
**Measured on `-gpu swangle_indirect`** (ANGLE on SwiftShader Vulkan), not on the `swiftshader_indirect` of the other factors, because the corpus A crashes the emulator process on the default path (§9, `results/environment-p08-swangle.json`). Both variants ran on the same path; read Δ% only within this factor.
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Δ abs (B − A) | 95% CI of Δ abs | Cliff's δ | n |
|---|---|---|---|---|---|---|---|---|---|
| `st.fx.perItem` | count | 2 (0) | 0 (0) | -100 | [-100, -100] | -2 | [-2, -2] | 1 | 14 |
| `st.rc.row` | count | 18 (2) | 20 (0) | 11.1 | [0, 11.1] | 2 | [0, 2] | -0.466 | 14 |
| `Compose_recompose.count` | count | 20 (2) | 21 (1) | 5 | [0, 10.5] | 1 | [0, 2] | -0.5 | 14 |
| `Recomposer_recompose.mainMs` | ms | 0.5052 (0.2503) | 0.7196 (0.282) | 42.4 | [6.46, 110] | 0.2144 | [0.03983, 0.4243] | -0.655 | 14 |
| `Compose_recompose.mainMs` | ms | 30.47 (13.74) | 20.26 (6.612) | -33.5 | [-47, -7.84] | -10.21 | [-16.9, -1.69] | 0.647 | 14 |
| `frameDurationCpuMs_p90` | ms | 62.87 (17.53) | 49.07 (4.162) | -22 | [-35.2, -11.8] | -13.8 | [-26.48, -6.677] | 0.875 | 12 |
| `frameDurationCpuMs_p99` | ms | 127.6 (31.7) | 86.45 (10.07) | -32.3 | [-41.1, -17.7] | -41.15 | [-57.06, -19.43] | 0.875 | 12 |
| `frameOverrunMs_p90` | ms | 68.98 (32.27) | 46.85 (9.337) | — | — | -22.14 | [-46.02, -9.035] | 0.819 | 12 |
| `slowFrames_pct` | % | 70.52 (12.21) | 48.65 (9.92) | -31 | [-38.9, -17.5] | -21.87 | [-30.25, -11.89] | 0.819 | 12 |
| `memoryHeapSizeMaxKb` | KB | 7372 (184) | 6596 (49) | -10.5 | [-12.4, -10] | -776 | [-928, -736] | 1 | 14 |
| `memoryRssAnonMaxKb` | KB | 4.646e+04 (309) | 4.265e+04 (300) | -8.2 | [-8.76, -7.86] | -3810 | [-4086, -3648] | 1 | 14 |

**C5 (protocol §3.1, A14).** P90 of `frameDurationCpuMs`, P08 A against B, delta% = (A − B) / B of the medians, percentile bootstrap 95% CI (10,000 resamples, seed 20261121), Mann-Whitney U on the iteration values (`results/c5_confirmatory.csv`, `harness/c5.py`):

| n (A, B) | A median | B median | delta% (A−B)/B | 95% CI | Mann-Whitney p (raw) | Holm-adjusted p | criteria (Holm p < 0.05, delta% ≥ +10%, CI excludes 0) |
|---|---|---|---|---|---|---|---|
| 12, 12 | 62.87 ms | 49.07 ms | +28.1% | [+13.4%, +54.3%] | 0.000308 | (alpha applies Holm over C1-C5) | delta% and CI criteria met; Holm step pending |

Rig note (A14): P08 A and B were both measured on `swangle_indirect` in one window on the same AVD; every other factor used `swiftshader_indirect`, so P08 absolute frame times are not comparable with the other factors' absolute numbers, only the within-factor contrast is. Reading: B's flat cover cuts frame CPU time by a fifth at p90 (and by a third at p50 and p99) and the share of slow frames from 70% to 49%; peak heap and anonymous RSS are 10% and 8% lower. Recomposition counts do not drop (the corpus A is a static grid with no animation), so the cost sits in drawing the shadow, gradient overlay and procedural cover, not in composition. Graphics memory (the second H3 quantity) was not measured: the optlab benchmark has no graphics-memory metric.

### P06: image decode and cache
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Δ abs (B − A) | 95% CI of Δ abs | Cliff's δ | n |
|---|---|---|---|---|---|---|---|---|---|
| `st.decodedBytes` | bytes | 2.025e+08 (0) | 6.233e+06 (0) | -96.9 | [-96.9, -96.9] | -1.963e+08 | [-1.963e+08, -1.963e+08] | 1 | 12 |
| `st.decodes` | count | 15 (0) | 15 (0) | 0 | [0, 0] | 0 | [0, 0] | 0 | 12 |
| `memoryRssAnonMaxKb` | KB | 1.84e+05 (303) | 6.16e+04 (159) | -66.5 | [-66.6, -66.5] | -1.224e+05 | [-1.226e+05, -1.222e+05] | 1 | 12 |
| `memoryHeapSizeMaxKb` | KB | 7416 (31.25) | 6145 (7.5) | -17.1 | [-17.3, -17] | -1271 | [-1286, -1254] | 1 | 12 |
| `st_img_decode.sumMs` | ms | 1660 (365.4) | 0 (0) | -100 | [-100, -100] | -1660 | [-1727, -1382] | 1 | 12 |
| `st_img_decode.maxMs` | ms | 378.7 (78.87) | 0 (0) | -100 | [-100, -100] | -378.7 | [-416.2, -348.9] | 1 | 12 |
| `frameDurationCpuMs_p90` | ms | 5.937 (1.024) | 4.328 (0.2345) | -27.1 | [-35.9, -22.8] | -1.61 | [-2.41, -1.293] | 0.867 | 14 |
| `frameDurationCpuMs_p99` | ms | 81.48 (13.87) | 17.97 (9.2) | -77.9 | [-81.7, -69.2] | -63.51 | [-71.03, -52.28] | 0.98 | 14 |
| `frameOverrunMs_p90` | ms | -9.044 (1.485) | -11.48 (0.3407) | — | — | -2.436 | [-3.017, -1.444] | 0.857 | 14 |
| `slowFrames_pct` | % | 3.596 (0.3648) | 1.37 (0.7821) | -61.9 | [-68.6, -49.2] | -2.226 | [-2.594, -1.753] | 0.867 | 14 |

Reading: A decodes the full 1500x2250 cover for each of the 15 images the scenario loads (203 MB decoded in total); B decodes the same 15 images at slot size (6.2 MB), a 96.9% cut. Peak anonymous RSS falls from 180 MB to 60 MB (the product's browsing target is 150 MB, CLAUDE.md; this lab screen holds nothing else). Frame CPU p99 falls 78% and slow frames 3.6% to 1.4% (software GPU: direction only). `st_img_decode.*` is A-only instrumentation (B's decode runs in Coil), read it as A's decode time: 1660 ms in total, 378.7 ms for the slowest.

### P01+P09: list construction (eager Column against LazyColumn, 2000 loans)
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Δ abs (B − A) | 95% CI of Δ abs | Cliff's δ | n |
|---|---|---|---|---|---|---|---|---|---|
| `st.rc.row` | count | 2000 (0) | 31 (1) | -98.5 | [-98.5, -98.5] | -1969 | [-1970, -1969] | 1 | 12 |
| `Compose_recompose.count` | count | 1 (0) | 34 (1) | 3.3e+03 | [3.2e+03, 3.3e+03] | 33 | [32, 33] | -1 | 12 |
| `Compose_recompose.mainMs` | ms | 217.3 (16.21) | 20.72 (4.447) | -90.5 | [-91.6, -89.1] | -196.6 | [-206.9, -188.2] | 1 | 12 |
| `timeToInitialDisplayMs` | ms | 1901 (43.36) | 573.1 (32.99) | -69.9 | [-70.8, -68.7] | -1328 | [-1366, -1302] | 1 | 12 |
| `timeToFullDisplayMs` | ms | 2018 (39.63) | 597.7 (80.56) | -70.4 | [-71.2, -66.8] | -1420 | [-1446, -1343] | 1 | 12 |
| `memoryHeapSizeMaxKb` | KB | 2.255e+05 (0) | 7734 (32) | -96.6 | [-96.6, -96.6] | -2.178e+05 | [-2.178e+05, -2.178e+05] | 1 | 12 |
| `memoryRssAnonMaxKb` | KB | 2.484e+05 (9208) | 4.381e+04 (159) | -82.4 | [-82.5, -81.7] | -2.045e+05 | [-2.064e+05, -1.96e+05] | 1 | 12 |
| `frameDurationCpuMs_p90` | ms | 27.14 (2.594) | 5.912 (0.0826) | -78.2 | [-79.1, -76.9] | -21.23 | [-22.3, -19.68] | 1 | 13 |
| `frameDurationCpuMs_p99` | ms | 34.3 (4.213) | 18.21 (13.7) | -46.9 | [-62.2, -20.6] | -16.1 | [-22.91, -7.026] | 0.976 | 13 |
| `frameOverrunMs_p90` | ms | 14.08 (5.564) | -9.664 (0.1975) | — | — | -23.74 | [-25.51, -19.86] | 1 | 13 |
| `slowFrames_pct` | % | 90.05 (17.99) | 1.255 (0.8475) | -98.6 | [-99.2, -98.1] | -88.79 | [-99.15, -80.73] | 1 | 13 |

Reading: A composes every row before the first frame (`st.rc.row` is the row count), so time to first display, heap and frame cost grow with the list; B composes only the visible rows. TTID falls from 1901 to 573.1 ms, peak Java heap from 220 MB to 7.6 MB, and 90.05% of frames are slow in A against 1.255% in B. `Compose_recompose.count` is higher for B on purpose: lazy composition is spread over many small passes while A does one huge composition; `Compose_recompose.mainMs` shows the total. The scaling with the number of loans is the exploratory sweep (§6).

### P01 sub-factor: list items without key (primary phase only)
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Δ abs (B − A) | 95% CI of Δ abs | Cliff's δ | n |
|---|---|---|---|---|---|---|---|---|---|
| `st.rc.row` | count | 28 (1) | 28 (0.25) | 0 | [-3.45, 0] | 0 | [-1, 0] | 0.125 | 12 |
| `Compose_recompose.count` | count | 29 (1) | 29 (0.25) | 0 | [-3.33, 0] | 0 | [-1, 0] | 0.0833 | 12 |
| `Compose_recompose.mainMs` | ms | 70.23 (8.05) | 74.95 (21.27) | 6.72 | [-4.24, 29.5] | 4.723 | [-3.072, 19.76] | -0.396 | 12 |
| `Recomposer_recompose.mainMs` | ms | 0.6055 (0.2474) | 0.6135 (0.1297) | 1.31 | [-20.2, 31.2] | 0.007959 | [-0.1551, 0.1636] | -0.0938 | 12 |
| `timeToInitialDisplayMs` | ms | 617.1 (95.36) | 620 (150.7) | 0.471 | [-6.44, 21.5] | 2.909 | [-41.99, 127.7] | -0.26 | 12 |
| `timeToFullDisplayMs` | ms | 638.2 (76.95) | 680 (165.6) | 6.55 | [-5.41, 24.9] | 41.78 | [-35.92, 150.9] | -0.365 | 12 |
| `memoryHeapSizeMaxKb` | KB | 1.854e+04 (23.25) | 1.855e+04 (16.5) | 0.0378 | [-0.0863, 0.0917] | 7 | [-16, 17] | -0.0938 | 12 |
| `memoryRssAnonMaxKb` | KB | 5.008e+04 (160) | 5.009e+04 (113) | 0.012 | [-0.203, 0.248] | 6 | [-102, 124] | -0.125 | 12 |

Reading: **null result.** Every interval spans 0 and the row-body count is the same (28 visible rows) in both variants: a missing key changes nothing on a static list, it matters on reorder and insertion, which this scenario does not exercise (6 swipes over a fixed list of 5000). The seeded defect is real in code (detector finding P01) but costs nothing here.

### P13: search over 100k books
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Δ abs (B − A) | 95% CI of Δ abs | Cliff's δ | n |
|---|---|---|---|---|---|---|---|---|---|
| `st.search.queries` | count | 9 (0) | 8 (0) | -11.1 | [-11.1, -11.1] | -1 | [-1, -1] | 0.857 | 12 |
| `st_search_query.count` | count | 9 (0) | 8 (0) | -11.1 | [-11.1, -11.1] | -1 | [-1, -1] | 0.857 | 12 |
| `st_search_query.mainMs` | ms | 0 (0) | 0 (0) | — | — | 0 | [0, 0] | 0 | 12 |
| `st_search_query.sumMs` | ms | 634.9 (79.26) | 85.42 (21.72) | -86.5 | [-88.4, -84.1] | -549.5 | [-620.8, -529.5] | 0.714 | 12 |
| `st_search_query.maxMs` | ms | 96.63 (32.69) | 37 (8.245) | -61.7 | [-69.7, -51.4] | -59.63 | [-80.3, -43.65] | 0.714 | 12 |
| `memoryHeapSizeMaxKb` | KB | 7740 (60) | 7108 (36) | -8.17 | [-8.56, -7.58] | -632 | [-664, -584] | 0.905 | 12 |
| `memoryRssAnonMaxKb` | KB | 4.497e+04 (302) | 4.478e+04 (147) | -0.405 | [-0.839, -0.0625] | -182 | [-378, -28] | 0.464 | 12 |
| `frameDurationCpuMs_p90` | ms | 32.06 (5.994) | 34.22 (8.698) | 6.72 | [-8.22, 28.9] | 2.156 | [-2.938, 8.712] | -0.269 | 12 |
| `frameDurationCpuMs_p99` | ms | 132.9 (66.47) | 157.7 (18.72) | 18.7 | [-18.8, 33] | 24.83 | [-36.04, 44.89] | -0.436 | 12 |
| `frameOverrunMs_p90` | ms | 34.62 (10.67) | 36.42 (10.72) | — | — | 1.796 | [-9.297, 6.584] | 0.0128 | 12 |
| `slowFrames_pct` | % | 17.11 (8.383) | 18.9 (2.764) | 10.5 | [-18, 32.7] | 1.794 | [-4.107, 4.728] | -0.218 | 12 |

Reading: both variants run the query on the IO dispatcher behind a 200 ms debounce and return a 120-row title-ordered page, so `st_search_query.mainMs` is 0 for both. The per-query cost differs: the slowest single query takes 96.63 ms in A against 37 ms in B (the product target is search under 50 ms over 100k books: only B meets it, on an emulator and for a typed prefix, so read it as relative), and the summed query time is 634.9 ms against 85.42 ms, about 70 ms against 11 ms per query. The query counts differ by one (9 against 8); the cause is not isolated, which is why per-query figures are given. **The frames do not follow:** the frame CPU p50, p90 and slow-frame share have intervals that span 0 and p99 is, if anything, higher for B (+18.7%, interval [-18.8, 33]; over all iterations the interval excludes 0). Both variants render the same 120-row page after each query, so on this device the frame cost is the list, not the search. Memory differences are small (heap −8%).

**The two variants are not row-for-row equal** (`p13_equivalence.csv`): A is a substring match (`LIKE '%q%'`), B a token-prefix match, so for four of the twelve typed prefixes (s, si, "silent r", "silent ri") the 120-row pages differ (Jaccard of the pages 0.09, 0.16, 0.22 and 0.53); for the other eight prefixes the full result sets and pages are identical. H is a naive token-prefix scan used only to check that B's FTS4 index returns exactly what a token-prefix rule defines (Jaccard 1.0 on all twelve).

| typed prefix | A (corpus LIKE) results | H (naive token-prefix) results | B (FTS4) uncapped | B page (120, title order) | Jaccard A vs B (all) | Jaccard A page vs B page (120 each, title order) | Jaccard H vs B (all) | Jaccard H page vs B page |
|---|---|---|---|---|---|---|---|---|
| `s` | 71106 | 26435 | 26435 | 120 | 0.37176890839029053 | 0.08597285067873303 | 1.0 | 1.0 |
| `si` | 17080 | 9310 | 9310 | 120 | 0.5450819672131147 | 0.15942028985507245 | 1.0 | 1.0 |
| `sil` | 7008 | 7008 | 7008 | 120 | 1.0 | 1.0 | 1.0 | 1.0 |
| `sile` | 2235 | 2235 | 2235 | 120 | 1.0 | 1.0 | 1.0 | 1.0 |
| `silen` | 2235 | 2235 | 2235 | 120 | 1.0 | 1.0 | 1.0 | 1.0 |
| `silent` | 2235 | 2235 | 2235 | 120 | 1.0 | 1.0 | 1.0 | 1.0 |
| `silent ` | 2235 | 2235 | 2235 | 120 | 1.0 | 1.0 | 1.0 | 1.0 |
| `silent r` | 55 | 199 | 199 | 120 | 0.27638190954773867 | 0.22377622377622378 | 1.0 | 1.0 |
| `silent ri` | 55 | 104 | 104 | 104 | 0.5288461538461539 | 0.5288461538461539 | 1.0 | 1.0 |
| `silent riv` | 55 | 55 | 55 | 55 | 1.0 | 1.0 | 1.0 | 1.0 |
| `silent rive` | 55 | 55 | 55 | 55 | 1.0 | 1.0 | 1.0 | 1.0 |
| `silent river` | 55 | 55 | 55 | 55 | 1.0 | 1.0 | 1.0 | 1.0 |

### P03+P04: stability and heavy work in composition
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Δ abs (B − A) | 95% CI of Δ abs | Cliff's δ | n |
|---|---|---|---|---|---|---|---|---|---|
| `st.rc.row` | count | 300 (0) | 300 (0) | 0 | [0, 0] | 0 | [0, 0] | 0 | 13 |
| `Compose_recompose.count` | count | 76 (1) | 75 (1) | -1.32 | [-1.32, 1.33] | -1 | [-1, 1] | 0.181 | 13 |
| `Recomposer_recompose.mainMs` | ms | 11.85 (4.378) | 10.5 (2.314) | -11.4 | [-29.5, 7.16] | -1.35 | [-4.186, 0.7981] | 0.258 | 13 |
| `Compose_recompose.mainMs` | ms | 36.21 (7.524) | 36.75 (7.627) | 1.49 | [-14.4, 13] | 0.541 | [-5.369, 4.515] | 0.0317 | 13 |
| `frameDurationCpuMs_p90` | ms | 11.56 (5.529) | 9.802 (4.329) | -15.2 | [-37.6, 29.8] | -1.76 | [-5.535, 2.736] | 0.231 | 13 |
| `frameDurationCpuMs_p99` | ms | 269.7 (33.56) | 221.3 (115.6) | -18 | [-37.8, -1.38] | -48.46 | [-106.8, -3.633] | 0.538 | 13 |
| `frameOverrunMs_p90` | ms | -4.172 (5.628) | -6.381 (4.507) | — | — | -2.209 | [-6.711, 2.206] | 0.282 | 13 |
| `slowFrames_pct` | % | 6.494 (2.966) | 6.494 (2.094) | 0 | [-32.2, 20.1] | 0 | [-2.966, 1.299] | 0.144 | 13 |

Reading: **null result.** The detector finds the unstable `List<Loan>` parameter and the formatter and filters in composition, and the port keeps them, but in this scenario (300 loans, ticks at 10 Hz, a fresh equal list per tick) A and B do the same work: `st.rc.row` is 300 for both (the first composition only), the recomposition count per run is 76 against 75, and the main-thread recompose time is 36.21 against 36.75 ms (interval [-14.4, 13]%). Frame p90, p50 and slow frames do not differ; the p99 CPU time is lower for B by 18% (interval [−37.8, −1.4]), one of 18 frame rows and not corrected for that. The pilot's hand-written A showed a large effect (its constructs rebuilt the whole list per tick); the corpus code's version of the same patterns does not at this size. Cause not isolated; the number of loans is the knob that would show whether it scales (not run).

### P10: R8 (baseline technique)
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Δ abs (B − A) | 95% CI of Δ abs | Cliff's δ | n |
|---|---|---|---|---|---|---|---|---|---|
| `apk_bytes` | bytes | 2.474e+07 (0) | 2.384e+06 (0) | -90.4 | — | -2.235e+07 | — | — | 1 |
| `dex_bytes` | bytes | 2.41e+07 (0) | 2.17e+06 (0) | -91 | — | -2.193e+07 | — | — | 1 |
| `resources_bytes` | bytes | 4.958e+05 (0) | 9.096e+04 (0) | -81.7 | — | -4.048e+05 | — | — | 1 |
| `timeToInitialDisplayMs` | ms | 505.6 (28.25) | 461.6 (20.27) | -8.7 | [-12.4, -4.97] | -43.97 | [-65.23, -24.78] | 0.771 | 14 |
| `timeToFullDisplayMs` | ms | 545.6 (55.92) | 494.5 (30.26) | -9.35 | [-14.6, -3.58] | -51.03 | [-83.91, -18.97] | 0.743 | 14 |
| `frameDurationCpuMs_p90` | ms | 6.029 (0.3949) | 4.894 (0.209) | -18.8 | [-23.1, -16.6] | -1.136 | [-1.463, -0.9937] | 0.908 | 14 |
| `frameOverrunMs_p90` | ms | -9.05 (0.5368) | -10.25 (0.2427) | — | — | -1.198 | [-1.487, -0.8866] | 0.99 | 14 |
| `slowFrames_pct` | % | 2.532 (0.6204) | 2.08 (0.8437) | -17.8 | [-49.2, -0.417] | -0.4517 | [-1.268, -0.01033] | 0.571 | 14 |

Reading: the size rows are the device-independent result (n = 1, deterministic): R8 and resource shrinking cut the APK by 90%. Startup differs by about 9% on this trivial screen and, like all ms figures, is relative. Frame CPU p90 is 19% lower for B.

### P11: Baseline Profile (baseline technique)
| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Δ abs (B − A) | 95% CI of Δ abs | Cliff's δ | n |
|---|---|---|---|---|---|---|---|---|---|
| `timeToInitialDisplayMs` | ms | 462 (22.43) | 434.8 (18.76) | -5.88 | [-9.87, -2.67] | -27.18 | [-46.76, -12.05] | 0.692 | 13 |
| `timeToFullDisplayMs` | ms | 481.3 (23.08) | 443.4 (32.13) | -7.89 | [-12.1, -2.81] | -37.98 | [-60.39, -13.44] | 0.733 | 13 |
| `frameDurationCpuMs_p90` | ms | 4.875 (0.2987) | 4.512 (0.2496) | -7.45 | [-13.4, -3.99] | -0.3631 | [-0.6896, -0.194] | 0.931 | 12 |
| `frameDurationCpuMs_p99` | ms | 15.96 (31.18) | 17.11 (8.556) | 7.2 | [-62.2, 86.9] | 1.15 | [-25.37, 10.59] | -0.0417 | 12 |
| `frameOverrunMs_p90` | ms | -10.05 (0.638) | -11.16 (0.2606) | — | — | -1.107 | [-1.365, -0.5749] | 0.986 | 12 |
| `slowFrames_pct` | % | 1.281 (0.8334) | 1.653 (0.8298) | 29 | [-49.6, 96.7] | 0.3719 | [-0.8263, 0.8125] | 0.0486 | 12 |

Reading: the profile is generated by `ProfileGenerator` (`BaselineProfileRule`) on the non-minified build and sits in `android/optlab/src/main/baseline-prof.txt` (rule and binary sizes in `build_facts.csv`). B also benefits from the profiles AndroidX libraries embed; A resets all of it. TTID improves by about 6% and TTFD by 8% on a one-screen lab app; slow-frame share does not differ. Expect a larger gain on an app with a long startup path.

## 5. What these numbers say about the hypothesis

H3 (visual-slop tax): C5 is met on the delta and interval criteria (+28.1% at P90, CI [+13.4, +54.3]); the Holm-adjusted p is alpha's step. The effect is draw cost, not composition (§4), and holds on one emulator GPU path only.
The measured slop factors fall into three groups. (1) Large, device-independent effects removed by the textbook fix: P06 (decoded bytes −97%, peak RSS −67%) and P01+P09 (TTID −70%, heap −97%, slow frames 90% to 1%). (2) A large effect on the operation itself that does not reach the frames: P13 (per-query time −86%, frame metrics not improved). (3) No measurable effect: P03+P04, where the corpus code's version of the pattern costs nothing at this scale. The two baseline techniques give modest startup gains (P10 −9%, P11 −6% TTID) and a large size gain for R8.
The novel claim (how often assistants emit each pattern) comes from E1; §7 joins it.

## 6. Scaling of P01+P09

_Not run (dropped for budget, alpha 2026-11-23)._ The sweep (`P01+P09/n=500`, `P01+P09/n=8000` next to the main n = 2000, primary phase only) would have run separately, labelled exploratory (not C5). The pilot's author-written A at 1000, 5000 and 20000 rows (`results/pilot-run1/`, a different A) is not comparable and is not repeated here.

## 7. From cost to expected cost (prevalence x cost)

Expected cost of a pattern in a population of generated apps = (share of apps that contain it, E1) x (cost when present, this report). E1 prevalence is the share of valid runs with at least one finding of the rule in the two unguardrailed arms (naive 33 + detailed-functional 33 = 66 runs; E1 REPORT §2a, frozen v1 detector; the guardrailed arm is a different treatment and is left out). Rules P06, P09 and P10 are `unvalidated-precision` in E1. The cost column is A − B (medians of the kept iterations); the last column multiplies it by the prevalence of each rule of the factor. Sizes are device-independent; ms figures are emulator-relative.

| taxonomy | E2 factor | cost metric | cost when present | E1 prevalence (of 66) | prevalence x cost | note |
|---|---|---|---|---|---|---|
| P08 | P08 | `frameDurationCpuMs_p90` | 13.8 ms | P08 0.12 | 1.67 ms | swangle_indirect path (A14) |
| P06 | P06 | `st.decodedBytes` | 196 MB | P06 0.08 | 14.9 MB |  |
| P09 | P01+P09 | `timeToInitialDisplayMs` | 1328 ms | P09 0.03 | 40.2 ms | the P01 rule is the next row |
| P01 | P01+P09/P01 | `timeToInitialDisplayMs` | no measurable difference | P01 0.21 | 0 | interval spans 0 (null result) |
| P13 | P13 | `st_search_query.sumMs` | 550 ms | P13 0.23 | 125 ms | frames not improved |
| P03, P04 | P03+P04 | `Compose_recompose.mainMs` | no measurable difference | P03 0.09; P04 0.08 | 0 | interval spans 0 (null result) |
| P10 | P10 | `apk_bytes` | 22.4 MB | P10 1.00 | 22.4 MB |  |
| P11 | P11 | `timeToInitialDisplayMs` | 27.2 ms | P11 1.00 | 27.2 ms |  |

Reading: P10 and P11 are in every generated app, so their expected cost equals the cost itself (large for APK size, small for startup). Among the code-level patterns, P13 and P01 appear in about a fifth of the unguardrailed runs, P08 in an eighth, P06, P03, P04 in under a tenth, P09 in 3%.

## 8. Threats to validity

- **Emulator, not a phone.** x86_64 guest on KVM with a software GPU (`swiftshader_indirect`). Frame and startup milliseconds are relative; shadow and blur cost is inflated by software rendering. The software GPU is also a rig limit on its own: the P08 corpus pattern crashes the host-side shader JIT (§9). Macrobenchmark errors suppressed with `EMULATOR`. No physical device was available (user decision, contract §6b).
- **Host contention.** Other agents ran on the same host. The harness holds `bench.lock` and `bench.hold` per window and yields 120 s after every factor group, which stops this study's own generation workers, not other panels' processes. Contention is handled by the pre-declared exclusion rule (§2, flow table in §4.0) and by recording `mem_avail_mb` and `loadavg1` for every iteration. The rule keys on the host state, not on a metric value; it cannot remove all noise (page-cache pressure, CPU steal), and ABAB interleaving spreads the rest across A and B without removing it. 44 of 396 iterations were dropped.
- **n = 12 to 17 kept per variant, one device session, one data seed.** Intervals are bootstrap on that many points; Cliff's δ and p are descriptive.
- **A is corpus code, but the scenario is mine.** The port keeps the offending construct unchanged, yet row counts (2000 loans, 5000 history items, 300 books), the swipe journey, the tick rate and the typed query are my choices, and the effect size depends on them (P03+P04 shows nothing at 300 loans). Each port also needed a host screen and a data binding (`ports/README.md`); B is a tuned version I wrote, not a corpus fix.
- **P13 semantics.** A (substring `LIKE`) and B (FTS4 token prefix) are not row-for-row equal for four of twelve prefixes (§4); the query-time comparison is between different result sets there. Both return a 120-row title-ordered page. FTS4 has no ranking (platform SQLite has no FTS5, `build_facts.csv`).
- **Cold start per iteration with warm page cache.** Iterations after the first of a call hit a warm file cache; first-install conditions are reproduced only by P11-A's profile reset.
- **Counters measure scope executions, not wall time.** `st.rc.row` counts how often a row body ran; it is exact, but wall-time cost is in the `mainMs` rows. A-only probes (`st_img_decode.*`) read 0 for B.
- **P10 A differs from B in two switches** (minify and resource shrinking) and in application id (`.nor8`), so the two APKs were installed side by side.
- **P11 baseline profile** is generated on the emulator from the lab journeys; its rules cover the lab app, not the product app.
- **P10 and P11 ran on a different APK build** than the corpus factors (same code except the corpus screens' changes after that build; `3S-study-work/apk/p10p11-run/`), in an earlier window.

## 9. Deviations, incidents and open items

- **P08 runs on a different GPU path (protocol A10).** Rank 1 of the P08 candidate list (`agy-g38flash-B1-naive-r1`, `BookCover.kt`) is ported and runs, but the emulator process (qemu) dies with SIGSEGV in non-debuggable builds, typically on the second to fifth launch, never on the first after boot; debuggable builds and B survive. A10 says: if the unmodified original corpus app does not crash, the crash is a port defect and the port is to be fixed. The unmodified original, built by E1, survived 3 + 4 + 4 cold-launch cycles, 5 + 5 cycles repackaged as non-debuggable, and 4 cycles of a swipe loop, and it draws the same cover (`ports/a10-crash-check/original-app-catalogue.png`). The crash was localised with gdb to a host render thread executing a SwiftShader JIT-compiled pixel routine (out-of-bounds SIMD load), with the same faulting instruction on two crashes (`qemu-crash-gdb-port2.txt`). Not the cause: R8, targetSdk, cover slot size, number of cards, compile mode, the shader cache, the debuggable flag alone. Same AVD with `-gpu swangle_indirect` does not crash (8 of 8 launches, then the full P08 window), so P08 is measured there; the difference between the original and the port is not isolated (Compose BOM 2024.02 against 2026.09 is untested).  `ports/rejections.csv` stays empty because rank 1 was not rejected.
- **A variants are corpus ports** (swap done 2026-11-21): P06 rank 1, P01 rank 1 (P01 rule), P09 rank 3 (first P09 file; ranks 1 and 2 are P01 files), P13 rank 1, P03+P04 rank 1 (same file as P09). The port review by alpha required one change, applied: P13 B returns the same 120-row title-ordered page as A.
- **Baseline profile without the Gradle plugin.** Generated with `BaselineProfileRule` and copied into `src/main` instead of the `androidx.baselineprofile` plugin, which adds build types that collide with the extra `noR8` build type. The artifact is the same one the plugin produces.
- **Run history.** The one-iteration pilot and a first full run exposed defects in the lab itself and were discarded (a skippable recomposition probe, an unequal comparator in B of P03+P04, `reportFullyDrawn` on a static screen, no scroll in the P10/P11 journey). The first full run that passed (hand-written A, commit 20a190c) is kept in `results/pilot-run1/` (protocol §15 A3) and is not analysed here.
- **Lock protocol.** The shared `bench.lock` has no writer priority; generators that re-take it per generation starved the benchmark for 44 minutes in the first run, then deadlocked with the turnstile file of the second. Resolution (alpha RE#dl-1, REQ#dl-fix): the harness writes `bench.hold` (pid) for its measured window and polls the lock every 0.2 s; peers' generators wait on the hold file (`gen_launch.py` fixed by its owner); `--turnstile` restores the old behaviour.
- **Exclusion rule history.** The first A2 rule keyed on host swap-in (system pswpin); the host swaps all the time, so it dropped nearly everything. Protocol A7 (alpha RE#fair-3) keys on the emulator process's major faults instead, and the emulator runs without swap (`MemorySwapMax=0`). A8 is an erratum to A7's observed range (249 to 3268 faults).
- **Disk.** Builds hit ENOSPC on /mnt (NTFS file-record exhaustion) three times; each time I pruned my own build trees and retried once, then reported it. APKs are copied to `3S-study-work/apk/` after every build and `optlab/build`, `benchmark/build` are deleted.
- **Exploratory sets not run:** the author-A sensitivity pair (`<id>/author` factor ids are in the harness) and the n sweep (§6) were dropped for budget (alpha, 2026-11-23); the harness entries remain.
**Exploratory control (renderer effect behind A14; not C5).** The P01+P09 pair re-run on `-gpu swangle_indirect` (factor id `P01+P09@swangle`, primary and frames, 12 or more kept iterations per variant, `exclusions.csv`) next to the main run on `swiftshader_indirect`:

| metric | swiftshader_indirect A → B (Δ%) | swangle_indirect A → B (Δ% [95% CI]) |
|---|---|---|
| `st.rc.row` | 2000 → 31 (−98.5%) | 2000 → 30 (−98.5%) |
| `timeToInitialDisplayMs` | 1901 → 573 ms (−69.9%) | 3426 → 579 ms (−83.1% [−84.2, −78.2]) |
| `frameDurationCpuMs_p90` | 27.1 → 5.9 ms (−78.2%) | 45.1 → 30.5 ms (−32.3% [−46.8, −11.4]) |
| `frameDurationCpuMs_p99` | 34.3 → 18.2 ms (−46.9%) | 68.4 → 55.9 ms (−18.3% [−34.4, +7.9]) |
| `slowFrames_pct` | 90.1 → 1.3% (−98.6%) | 100 → 46.7% (−53.3% [−66.7, −48.8]) |

Reading: the direction and the count rows are the same on both paths; the frame figures are not. On the ANGLE path every frame is slower, B's too (p90 30.5 ms against 5.9 ms), so the A/B frame contrast shrinks (−32% against −78% at p90). The P08 contrast (+28% in A − B over B terms, §4) was measured on the slower path and the renderer is part of what it measures; this is why A14 limits comparison to within-factor contrasts.
- **Demo clips** (`3S-study-work/video/`, A = corpus code, B = tuned, same finger input, 14 s, 720x1280): `P08c-A-scroll.mp4`, `P08c-B-scroll.mp4` (recorded on swangle_indirect), `P06c-{A,B}-scroll.mp4`, `P09c-{A,B}-scroll.mp4`, `P01c-{A,B}-scroll.mp4`, `P03P04c-{A,B}-scroll.mp4`, `P13c-{A,B}-search.mp4` (12 files). The P03+P04 and P01 pairs show no visible difference, as the numbers say. The older author-A clips in the same folder (`P08-*`, `P06-*`, `P01P09-*`, `P13-*`) are from the pilot.
- Unmeasured by design (contract R14): P12, S10, S12, U06 and the security factors. E3/E4/E5 consume the APKs in `3S-study-work/apk/` (`README.txt` there).

## 10. Reproduce

```bash
export TMPDIR=/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/tmp
cd android && flock <study-work>/gradle.lock ./gradlew :optlab:assembleRelease :optlab:assembleNoR8 :benchmark:assembleRelease   # not while the emulator runs
cp optlab/build/outputs/apk/release/optlab-release.apk optlab/build/outputs/apk/noR8/optlab-noR8.apk benchmark/build/outputs/apk/release/benchmark-release.apk <study-work>/apk/
rm -rf optlab/build benchmark/build            # file-count hygiene: the harness reads the APKs from <study-work>/apk
# emulator: AVD "lowend34" (API 34 google_apis x86_64, 2 cores, 2 GB, 720x1280@280, swiftshader_indirect), no-swap scope:
#   systemd-run --user --scope -p MemorySwapMax=0 emulator -avd lowend34 -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect -memory 2048 -cores 2 -no-metrics -port 5570
adb install -r -t <study-work>/apk/optlab-release.apk   # likewise optlab-noR8.apk and benchmark-release.apk
adb shell am instrument -w -e class shelftap.benchmark.FactorBenchmark#seed shelftap.benchmark/androidx.test.runner.AndroidJUnitRunner   # once per package
# the harness holds bench.lock and 3S-study-work/bench.hold itself; do not wrap it in flock
python3 optimization/study/e2-bench/harness/run_bench.py --factors "P06,P01+P09,P13,P03+P04,P10,P11" --phase primary,frames     # add P08 once A10 is resolved
python3 optimization/study/e2-bench/harness/run_bench.py --factors "P01+P09/P01" --phase primary
python3 optimization/study/e2-bench/harness/summarize.py
python3 optimization/study/e2-bench/harness/report_tables.py   # markdown tables for sections 4, 4.0 and the P13 equivalence table
python3 optimization/study/e2-bench/harness/record_demo.py     # demo clips
```

Demo clips: see §9 (12 corpus clips in `/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/video/`).
