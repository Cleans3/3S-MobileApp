# AGENT-BRIEF: optimization demo kit (for the presenter's coding agent)

You are helping a group member set up and run a live demo for a talk. The presenter reads `README.md` (Vietnamese); this file is for you. Everything runs through one script, `demo.py` in this folder (Python 3.8+, standard library only, Windows and Linux).

## The repo and the study, in 30 lines

- Repo `3S-MobileApp`: a course project with three subjects (smart cards, mobile optimization, mobile security). The product app is ShelfTap (`android/app`), an NFC library app. It is **not** the subject of this demo.
- `optimization/study/` is a research study: "AI coding assistants write Android first drafts quickly; what is left un-optimized, and what does it cost?" It has six experiments, E1 to E6. This demo illustrates **E2** (`e2-bench/`).
- E2 took real code from AI-written first drafts (corpus E1), ported it into a lab app `android/optlab` as variant **A**, and wrote a tuned variant **B** of the same screen. Both variants ship in one APK and are picked by intent extras: `--es factor P09c --es variant A|B`.
- E2 measured A against B with Macrobenchmark (`android/benchmark`, harness `e2-bench/harness/run_bench.py`) on an emulator AVD "lowend34" (API 34 google_apis x86_64, 2 cores, 2 GB, 720x1280 at 280 dpi, software GPU `swiftshader_indirect`; P08 on `swangle_indirect`). Results: `e2-bench/REPORT.md`, `e2-bench/results/summary.csv` (medians of 12 or more kept iterations).
- The patterns the talk shows, and the demo's scenario ids:

| scenario | factor | pattern (A) | fix (B) | slide |
|---|---|---|---|---|
| p09 | P09c | 2000 loans built eagerly in a Column + verticalScroll | LazyColumn | S7, S11, S16 |
| p08 | P08c | grid card with 5 dp shadow, gradient overlay, Canvas cover | flat cover box | S8, S16 |
| p06 | P06c | 1500x2250 covers decoded at full size (decodeStream, no sampling) | Coil, decode sized to the 150x210 dp slot | S10, S16 |
| p13 | P13c | `LIKE '%q%'` over 100k titles per keystroke | FTS4 index | S12, S16 |
| p10 | startup | release without R8 / resource shrinking (package `shelftap.optlab.nor8`) | R8 on (`shelftap.optlab`) | S12 |
| p01 | P01c | list items without `key` (null result) | with key + contentType | S9 |
| p0304 | P03P04c | unstable params + work in composition (null result) | stable param + remember | S9 |

- The frozen deck has 25 main slides. Slide numbers in this kit refer to it: the pattern slides are S7 to S12, slide 16 "Demo: trước và sau tối ưu" is the 120 s live-demo slot (cards P09, P06, P08, P13; README §6 has its running order), slide 22 is "Thiết bị thật" (real device).
- The product app ShelfTap has its own measured before/after on a phone (`optimization/log/opt-ui-ux.md`, `optimization/ui-ux/measure/`); README §9 lists what exists and what does not. `demo.py` does not run ShelfTap, and nothing under `android/app` is to be changed for the demo.
- The rule for the talk: **the study's numbers are the reference; a live run is an illustration.** Emulator numbers are A/B ratios on one machine, never phone numbers. The README says this; keep it that way.

## Commands

Windows uses `py`, Linux/macOS `python3`. Run from `optimization/study/demo`.

```
py demo.py doctor                 # exit 0 and "Kết quả: ĐẠT" = ready to set up / run
py demo.py setup [--single|--pair] [--skip-build] [--headless]
py demo.py start [--single|--pair] [--gpu swiftshader_indirect|swangle_indirect] [--headless]
py demo.py list
py demo.py run <scenario> [--single|--pair]
py demo.py record <scenario> [--single|--pair]
py demo.py stop
```

What each does:
- `doctor`: finds the SDK (ANDROID_HOME, ANDROID_SDK_ROOT, `android/local.properties`, then `%LOCALAPPDATA%\Android\Sdk`, `~/Android/Sdk`, `~/Library/Android/sdk`), adb, emulator, the API 34 google_apis x86_64 image, `emulator -accel-check`, a JDK 21 (JAVA_HOME, PATH, Android Studio's `jbr`, `/usr/lib/jvm/*21*`), free RAM (decides pair or single), disk. Lines marked `--` are informational.
- `setup`: `android/gradlew :optlab:assembleRelease :optlab:assembleNoR8` (ANDROID_HOME and JAVA_HOME set for the child process only), copies the two APKs to `out/apk/`, writes two AVDs `shelftap-demo-a` / `shelftap-demo-b` directly into the AVD home (config.ini mirrors lowend34; no avdmanager needed), boots, installs both APKs, `cmd package compile -m speed -f` (the study's CompilationMode.Full), seeds covers and the 100k-book DB (waits for logcat tag STLAB "seeded"), and warms up P13.
- `run`: makes sure the right emulators are up (boots, or reboots on a GPU or count change; the console port encodes the GPU: 5580/5582 swiftshader, 5590/5592 swangle), then per variant: atrace start (`-a shelftap.optlab view gfx`), cold launch with `am start -W` (TotalTime = TTID), 4 s settle, a fixed on-device gesture (`input swipe` / `input text`), RssAnon sampled from `/proc/<pid>/status` every 0.5 s, `dumpsys meminfo` for the Java heap, atrace stop and parse. Prints a table: live A, live B, study A → B. Also writes `out/last-run.json` (raw numbers, including `p90_raw`/`janky_raw` with the swap wait included).
- `record`: the same run plus `screenrecord` on each side, pulled to `out/rec/<scn>-A.mp4` and `-B.mp4`; with ffmpeg on PATH also `<scn>-AB.mp4` (hstack).

## File map

| path | what |
|---|---|
| `demo/demo.py` | the runner (scenarios, study reference values, parsing) |
| `demo/README.md` | presenter runbook, Vietnamese |
| `demo/out/` | git-ignored: APK copies, emulator logs, recordings, last-run.json |
| `android/optlab/` | lab app: `MainActivity.kt` (intent extras), `ui/CorpusScreens.kt`, `ui/SearchScreen.kt`, `corpus/*` (the A code), `Probe.kt` (trace counters `st.*`) |
| `android/benchmark/` | the study's Macrobenchmark module (not used by the demo) |
| `e2-bench/REPORT.md` | study method and results; §3 environment, §9 incidents (P08 crash), §10 reproduce |
| `e2-bench/results/summary.csv` | the reference numbers in `demo.py` SCENARIOS[...]["study"] come from here |
| `evidence/video/*-AB.mp4` | fallback clips recorded for the study (A left, B right, 14 s) |

## Known traps of this rig

1. **Software GPU.** All study numbers are on SwiftShader (CPU rendering). Frame times are slow for both variants; only the A/B ratio means anything. Do not switch to `-gpu host` and then compare with the study.
2. **P08 must run on `swangle_indirect`.** The corpus A of P08 crashes the emulator process (qemu SIGSEGV in a SwiftShader JIT routine) on `swiftshader_indirect` in non-debuggable builds, usually on the 2nd to 5th launch (`e2-bench/REPORT.md` §9). `demo.py` reboots the emulators with the right GPU by itself.
3. **Frame metric.** Same definition as the study (Macrobenchmark FrameTimingMetric: `Choreographer#doFrame <id>` start to the end of RenderThread `DrawFrames <id>`; slow = more than 16.67 ms), but the demo subtracts time spent in `eglSwapBuffers*`. On the emulator that wait flips between about 0 and one vsync from run to run and makes A and B look alike (seen: B p90 4 ms in one run, 18 ms in the next). `p90_raw` in `last-run.json` keeps the unadjusted value. The swap slice needs the atrace `gfx` category.
4. **Two emulators share the host CPU.** In pair mode B slows down while A is busy (seen on a 16-core host: P08 B 27% slow frames single, 80% pair). Counts (rows, bytes, APK size, RSS) are fine in pair mode; for times use `--single`.
5. **Cold page cache after boot.** The first FTS query after a boot read from disk (146 ms instead of about 25 ms). `provision()` warms P13 A and B after every boot; keep that.
6. **Typing pace.** P13 debounces at 200 ms. Keystrokes are 0.3 s apart so every key fires a query (13 and 12 queries, like the study's 9 and 8 at its pace). Faster typing merges queries and lowers the counts.
7. **Release build only.** The screens are measured on the R8 release build, which is profileable (manifest) but not debuggable; atrace app tracing works because of `profileable shell="true"`. A debuggable build changes timings a lot (ShelfTap on a phone, slide S22: debug build p90 125 ms against 12 to 16 ms on a measure build).
8. **Cold starts.** Every `run` force-stops and cold-starts each variant, like the study. TTID from `am start -W` is close to, not identical with, Macrobenchmark's timeToInitialDisplayMs.
9. **Heap.** The live Java heap is read once after the gesture (`dumpsys meminfo`, Dalvik Heap "Heap Size"); the study reports the maximum over the run, so it is higher (P09 A: 220 MB study, about 97 MB live).
10. **Gesture distance.** The demo swipes like the fallback clips (8 swipes of 650 px over 250 ms in the clip script; the runner does 6), which scrolls further than the benchmark journey. Absolute counts (P09 B rows, P06 decoded bytes) are larger than the study's; the ratio holds (P06 about 32x in both).
11. **JDK.** `android/optlab` uses `kotlin { jvmToolchain(21) }` with no toolchain download, so Gradle needs a JDK 21 exactly; JDK 17 or 25 alone fails. Android Studio's bundled `jbr` is 21.
12. **Windows.** `gradlew.bat` is started via `cmd /c`; the emulator needs Windows Hypervisor Platform or the Android Emulator hypervisor driver; `-no-window` is only implied on Linux without DISPLAY/WAYLAND_DISPLAY.

## How to verify each step

| step | pass when |
|---|---|
| `doctor` | exit code 0, last line `Kết quả: ĐẠT` |
| `setup` | prints `optlab-release.apk: 2,4 MB`, `optlab-noR8.apk: 24,7 MB`, then `Xong.`; `adb devices` lists `emulator-5580` (and `emulator-5582` in pair mode) |
| `run p09` | "số mục đã dựng" A = 2.000, B under 100; A slow frames above 80%, B under 10% |
| `run p06` | decoded A about 30x B; RSS A above 150 MB, B about 60 MB |
| `run p13` | 12 to 13 queries per side; A slowest query above 50 ms, B below |
| `run p10` | APK 24,7 MB against 2,4 MB |
| `run p08` | boots on port 5590; A p90 above B |
| `run p01`, `run p0304` | A and B close to each other |
| `stop` | `adb devices` lists no demo emulator |

Reference live values seen while building the kit are in README §5 ("Đã thấy khi chạy thử").

## What not to change

- **Frozen study files:** `optimization/study/00-protocol.md`, `guardrail/ANDROID-GUARDRAIL.md`, `e4-pressure/RUBRIC.md`, `02-taxonomy.md`, every `REPORT.md`, every `results/*.csv`, `AMENDMENT.md`, `figures/out/*`, `report/sources-leadin.md`, the `.docx`, and the clips in `evidence/video/`.
- **`android/optlab` and `android/benchmark`:** the study's numbers were measured on this code; changing a screen makes the live demo stop illustrating the study. If a scenario cannot launch, fix `demo.py`, not the lab.
- **`android/app`** (ShelfTap product app) belongs to other work; the demo does not use it.
- The study reference values in `demo.py` (`SCENARIOS[...]["study"]`) are copied from `e2-bench/results/summary.csv`; change them only if that file changes.
- Do not present a live number as a study result, in the README or anywhere else.

## Vietnamese wording (if you edit README.md)

Binding glossary from the talk: ứng dụng (never "app" in running text); bản nháp đầu (first draft); chưa tối ưu / đã tối ưu; khung hình trễ (jank), tỷ lệ khung hình trễ; thời gian xử lý khung hình (p90); thời gian khởi động (TTID); mức dùng RAM (RSS), Java heap; bản build (the build result), biên dịch only for the compile step; máy ảo / thiết bị thật; mục (list item); lượt mượn (loan); trợ lý lập trình AI on first use, then trợ lý AI; prompt (first use: "prompt, tức lời nhắc giao cho AI"). Vietnamese number format: decimal comma, dot for thousands (1.901 ms, 1,3%). Never say "giật 89%"; say tỷ lệ khung hình trễ.
