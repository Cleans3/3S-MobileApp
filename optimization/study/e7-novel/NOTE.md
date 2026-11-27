# E7 Research Note: New Problems and Engineering Gates Grounded in E1–E6 Data

**Status:** Desk phase complete (prototypes built, unit-tested, and retrospectively evaluated on E1, E4, E5, E6, and human-control corpora; no emulator or connected Gradle tests run yet).  
**Scope:** Strictly additive under `optimization/study/e7-novel/`. No frozen study files or `android/optlab` sources were modified.

---

## 1. Empirical Motivation: Six Gaps Exposed by E1–E6

Across E1–E6, the study measured how often AI coding assistants emit known Android anti-patterns and what those patterns cost. Six findings in our own data point past measuring known problems toward **new failure modes of AI-assisted Android engineering** and **new tool-enforced solutions**:

1. **The Toy-Data Illusion (`E6`, `E1`):** Every launching E6 hype-prompt app passes the `< 150 MB` memory target (PSS `66.8–102.8 MB`, `e6-oneshot/REPORT.md:38-49`) only because `11/12` arm-O apps hardcode `8–12` `Book(...)` literals in memory with no database, network client, or generator. Yet E2 proves that rare-but-critical patterns (`P06` 5/66, `P09` 2/66, `P13` 15/66) break the course targets only at realistic data sizes (`P06` ~180 MB RSS; `P09` 248 MB RSS / 220 MB heap / 90.05% slow frames; `P13` 96.63 ms max search vs `< 50 ms`).
2. **Assistants Cannot Be Their Own Judge (`E4`, `E5`):** Under error pressure (`E4`), assistants weakened security in `17/88` (`19.3%`) security trials (`X01` Keystore auth removed `12/30`; `S03` cleartext opt-in `5/30`). As optimisers (`E5`), advice recall was `168/168`, yet `9/12` autonomous/approve Codex "fixed" trees failed to compile and `21/24` runs mutated untouched decoys.
3. **The Guardrail Tax (`E1`, `E5`, `E6`):** Adding `ANDROID-GUARDRAIL.md` halved E1 build-pass rates (`20/33 = 61%` naive vs `10/34 = 29%` guardrailed), left `C1` at `+0.000 (p = 1.000)` among compiling drafts (`e1-corpus/REPORT.md:9`), caused `3/3` E6 Codex arm-R builds and `9/12` E5 Codex fixed trees to fail on `androidx.baselineprofile` (a technique that gave `0` measurable gain on the real Xiaomi phone, `opt-ui-ux.md:337`), and *introduced* `release` builds signed with `signingConfigs.debug` (`0/66` unguardrailed → `4/34` guardrailed in E1; `0/12` → `3/12` in E6 arm R).
4. **Static-Scanner Blind Spots (`E1`, `E3`):** MobSF achieved `0.291` precision with `0` code-level security true positives across 13 APKs (`e3-security/REPORT.md:74`). Our own `slop-scan` S01 rule skips any file mentioning `EncryptedSharedPreferences` or `KeyStore` (`r_sec.py:26`)—missing `try { EncryptedSharedPreferences.create(...) } catch { context.getSharedPreferences(...) }` **silent encryption downgrades** present in `4/66` E1 unguardrailed drafts (`0/40` screened human apps).
5. **Tooling Inversion on Compose Performance (`E1`, `E2`):** Existing linters (`detekt`/`compose-rules`/Android Lint) focus on `P03` and `P04`, which E2 measured as **no measurable cost** under Kotlin 2.0.20+ strong skipping (`e2-bench/REPORT.md:250`), while providing `0` built-in checks (`custom_only = 1`, `e1-corpus/REPORT.md:509-539`) for the four patterns that actually broke course targets (`P06`, `P08`, `P09`, `P13`).
6. **Emulator-to-Device Calibration Gap (`E2`, real phone):** On the Xiaomi phone, debug-build p90 (`125 ms`) overstated measure-build p90 (`12–16 ms`) by `8–10×`, Baseline Profile's emulator gain (`-5.88%` TTID) disappeared on hardware, and an unreachable `10.0.2.2` cover URL caused `7.0–7.2%` janky frames vs `1.5–2.7%` when fixed (`opt-ui-ux.md:289-362`).

---

## 2. Candidate New Problems & Solutions (8 Candidates)

Novelty is checked strictly against `01-related-work.md` and `references.md`. Where no prior work appeared in our review, we state *"we did not find this in the works we reviewed"* rather than claiming absolute world-first novelty.

| ID | Type | Candidate | Pillar(s) | What is new vs `01-related-work.md` / `references.md` | Supporting study data | How it would be measured | Cost to prototype |
|---|---|---|---|---|---|---|---|
| **C1** | Problem + Solution | **Toy-Data Illusion & Scale-Probe CI Gate**: detect hardcoded small-N lists + scale hazards, automatically amplify toy collections to `N = 100k`, and fail builds on course budgets (`<150 MB`, `<50 ms`, `60 fps`) | UI rendering, Memory, Compute | `[Huang2024]` (EffiBench) benchmarks algorithmic code on fixed inputs; `[Macrobenchmark]` measures hand-written apps; `[Liao2024]` notes 76.39% of Android perf issues lack tool coverage. **We did not find in the works we reviewed** an automated scale-amplification gate that rewrites AI toy-data lists into scale-stress builds to expose latent O(N) UI/memory/search failures. | E6 arm O: `11/12` apps hardcode `8–12` `Book` items (PSS `66.8–102.8 MB` passes `<150 MB`); E1 open coding `mock-data-instead-of-api`; E2 `P06` (~180 MB RSS), `P09` (248 MB RSS, 90% slow frames), `P13` (96.6 ms search). | Stage 1: static precision/recall on E1+E6. Stage 2 (emulator): paired A/B Macrobenchmark of original (`N=8–12`) vs amplified (`N=2k–100k`) E6 builds on RSS, TTID, search latency, slow frames. | **Medium (<1 day)** (Stage 1 + amplifier without emulator; Stage 2 ~30 min on emulator) |
| **C2** | Solution | **Monotonic Security-Invariant Diff Gate (`Diff-Guard`) + Tool-Judged Loop**: block edits that weaken existing security/perf controls (`X01`, `S03`, `S04`, `P10`, `P07`, decoys) or add `slop-scan` blind spots (`fail-open crypto`, `release debug-signing`), paired with compile gate as sole judge | Security, Build Integrity, UI/Mem regression guard | `[Shukla2025]` shows iterative security degradation; `[Hasan2026]` shows >65% of agent safety failures occur in bug-fixing/config; `[Freitas2025]` feeds a11y scanner output back to ChatGPT. **We did not find in the works we reviewed** a diff-level monotonic security-invariant gate targeting Android error-fix weakenings (`X01`/`S03`/fail-open catch/debug-signing) combined with a compile+decoy verifier loop. | E4: `17/88` security weakenings (`12/30` `X01`, `5/30` `S03`); E5: `9/12` Codex compile breaks, `21/24` decoys mutated; E1: `4/66` fail-open crypto, `4/34` guardrailed release debug-signing; E3: MobSF `0` code-level TPs. | Retrospective precision/recall on E4 (`115` primary diffs) + E5 (`24` diffs) + E1/E6/human blind-spot scan; prospective A/B error-fix loop (unguarded vs tool-judged). | **Low (0.5 day)** (no emulator needed) |
| **C3** | Problem | **The Guardrail Tax**: prose rules halve first-draft build rates and induce new regressions (`release` signed with debug key, overbroad `-keep`, broken `:baselineprofile` scaffolding) | Security, Build, Startup | `[Liao2026]` and `[Androcec2026]` report guideline/prompt security gains without measuring build-rate collapse or guardrail-induced regressions. **We did not find this trade-off quantified in the works we reviewed.** | E1 builds: naive `20/33 (61%)` → guardrailed `10/34 (29%)`; `C1` among builders `+0.000 (p=1.000)`; `DG-DEBUGSIGN`: `0/66` → `4/34` (E1), `0/12` → `3/12` (E6-R); `:baselineprofile` broke `3/3` E6 Codex-R and `9/12` E5 Codex builds. | Paired generation comparing 21-rule prose guardrail vs pruned evidence-weighted guardrail on build rate, critical findings among builders, and induced defects. | **Low (0.5 day)** (data audit done; needs generation runs) |
| **C4** | Solution | **Budget-Carrying Executable Guardrail**: drop empirically zero-payoff/high-breakage prose rules (`P11`, `P03`, static `P01`) and attach explicit numeric budgets + local gate commands (`assembleDebug`, `diffguard`, `scale_probe`) to the context file | UI rendering, Memory, Security | `[Liao2026]` uses prose coding guidelines. **We did not find in the works we reviewed** a guardrail pruned by A/B empirical cost and coupled to executable budget commands. | E6 arm S (structured spec + `N=5000` + `assembleDebug` DoD) cut median jank `99% → 19.6%` and LOC `1164 → 524`; E2 measured `P01`/`P03+P04` null and real-phone `P11` null. | Controlled generation arm `G-exec` vs `ANDROID-GUARDRAIL.md` on build rate, critical findings, and runtime budgets. | **Low (0.5 day)** for spec; needs LLM generation runs |
| **C5** | Problem | **Fail-Open Security Theatre & Unreachable-Asset Jank**: `try { crypto } catch { plaintext }` bypasses static scanners; invented/dead URLs (`10.0.2.2`, `example.com`) turn config slop into scroll jank | Security, UI rendering | `[MASWE-0001]` covers plaintext storage and `[Android-Jank]` covers frame budgets; **we did not find in the works we reviewed** analysis of LLM-generated fail-open crypto catch blocks evading static rules or unreachable-URL image retries causing scroll jank. | `DG-FAILOPEN`: `4/66 (6.1%)` E1 unguardrailed vs `0/40` screened human apps; `DG-INVENTED-URL`: `4/66` E1; real Xiaomi phone dead `10.0.2.2` cover URL caused `7.0–7.2%` jank vs `1.5–2.7%` fixed. | Blind precision rating of `DG-FAILOPEN` / `DG-INVENTED-URL` findings + A/B scroll jank test with unreachable vs reachable cover URLs. | **Very low** (already integrated into `diffguard.py`) |
| **C6** | Solution | **Cost-Calibrated Compose Draw-Layer Budget & Compiler-Metrics Gate**: replace uncostly stability lints (`P03`/`P04`) with a Gradle/detekt rule capping transitive per-item draw effects (`P08`) plus Compose compiler metrics (`-composables.txt`) gated only on hot-path non-skippable rows | UI rendering | `[compose-rules]` / `[compose-lints]` check style/stability without empirical cost weighting. **We did not find in the works we reviewed** a static Compose draw-layer budget calibrated to measured frame costs (`C5`: `+28.1%` p90). | E1 coverage table: `P06`, `P08`, `P09`, `P13` all `custom_only = 1`; E2 `P03+P04` null (`36.21 vs 36.75 ms`), `P08` `+28.1%` p90 (`p=0.000308`). | Precision/recall of transitive draw-layer budget rule vs E2/E6 frame p90 correlation. | **Medium (1 day)** (Gradle/detekt Kotlin plugin + compiler metrics parser) |
| **C7** | Problem + Solution | **Coroutine `CancellationException` Swallowing Auditor**: detect `catch (e: Exception|Throwable)` in `suspend fun` without rethrowing `CancellationException` (causes zombie coroutines / leaks on screen exit or search cancellation) | Memory / Resources | Known Kotlin coroutines pitfall, absent from `slop-scan` and default Android Lint; **we did not find AI-vs-human prevalence measurements in Android apps in the works we reviewed**. However, our scan shows humans trip it *more* often (`26/40 = 65%`) than AI (`34/100 = 34%`), so it is a general ecosystem bug, not AI-specific. | `DG-CANCEL-SWALLOW`: E1 unguardrailed `21/66 (31.8%)`, guardrailed `13/34 (38.2%)`, human pre `11/20 (55%)`, human post `15/20 (75%)`. | Static precision check + JVM coroutine cancellation unit test verifying leaked jobs. | **Low (0.5 day)** (already scanned in `diffguard.py`) |
| **C8** | Problem + Solution | **Measure-Build & Proxy-Metric Calibration Gate**: CI gate rejecting `debuggable=true` performance claims and enforcing emulator-valid device-independent proxies (`st.rc.row`, `st.decodedBytes`, `apk_bytes`, `st_search_query.maxMs`) while gating absolute `ms` to hardware runs | UI rendering, Methodology | `[Macrobenchmark]` warns against debug builds and emulators; **we did not find in the works we reviewed** a hybrid CI gate separating device-independent counters (emulator-enforceable) from hardware-only frame timings for AI-generated mobile apps. | Xiaomi debug p90 `125 ms` vs measure `12–16 ms`; P11 emulator `-5.88%` vs phone `0%`; `P01+P09` p90 Δ% `-78.2%` on `swiftshader` vs `-32.3%` on `swangle`. | Audit of benchmark JSON + APK manifest (`debuggable`, `CompilationMode`, GPU string) on E2/E6/product artifacts. | **Low (0.5 day)** |

---

## 3. Selection Matrix and Choice

Scoring each candidate `1–5` on **Novelty** (vs reviewed literature), **1-Day Feasibility** (desk prototype + verifiable run), and **Pillar Fit** (coverage of UI rendering, memory, and security):

| ID | Candidate | Novelty | 1-Day Feasibility | Pillar Fit | Product (`N × F × P`) | Decision |
|---|---|---|---|---|---|---|
| **C1** | **Scale Probe (Toy-Data Auditor + Auto-Amplifier + Budget Gate)** | **5** | **4** | **5** (UI + Memory + Compute) | **100** | **CHOSEN (#1)** |
| **C2** | **Diff-Guard (Monotonic Security-Invariant Diff Gate + Blind-Spot Scanner)** | **4** | **5** | **5** (Security + Guardrail/Decoy Integrity) | **100** | **CHOSEN (#2)** |
| C4 | Budget-Carrying Executable Guardrail | 4 | 3 | 5 | 60 | Follow-up (uses C1+C2 as its executable gates) |
| C3 | Guardrail Tax (problem characterization) | 4 | 5 | 3 | 60 | Evidence folded directly into C2 (`DG-DEBUGSIGN`, `DG-BROADKEEP`) |
| C6 | Cost-Calibrated Compose Draw-Layer Gate | 4 | 3 | 3 (UI only) | 36 | Core P08 check folded into C1 (`SCALE-FX-ITEM`) |
| C5 | Fail-Open Security Theatre | 4 | 5 | 4 | 80 | Core rule (`DG-FAILOPEN`) folded directly into C2 |
| C8 | Measure-Build Calibration Gate | 3 | 5 | 3 | 45 | Device-vs-emulator split folded into C1's `ScaleBudgetGate.kt` |
| C7 | Coroutine Cancellation Swallowing | 2 | 5 | 3 | 30 | Diagnostic warning only in C2 (not AI-specific: humans 65% vs AI 34%) |

**Why C1 + C2 together:**
- Together they span **all three pillars** of the talk (**UI rendering + Memory** via C1 `Scale Probe`; **Security + Fix Integrity** via C2 `Diff-Guard`).
- They fold the highest-value insights from C3, C5, C6, and C8 into two concrete, runnable CLI tools rather than fragmenting into disconnected scripts.
- Together with `./gradlew compileDebugKotlin`, they define a **Tool-Judged AI Engineering Loop** in which an assistant never certifies its own output:
  1. `diffguard.py diff` rejects security weakenings (`X01`, `S03`, `S04`, `DG-FAILOPEN`, `DG-DEBUGSIGN`, `P10` regression) and decoy erosion on every edit.
  2. `./gradlew compileDebugKotlin` rejects non-compiling trees (catching E5's `9/12` broken Codex trees and E6's `baselineprofile` breaks).
  3. `scale_probe.py audit` + `amplify` + `ScaleBudgetGate.kt` exposes toy-data illusions and fails CI on course memory/search/frame budgets.

---

## 4. Technical Design & What Was Built

All artifacts live in `optimization/study/e7-novel/` (pure Python 3 standard library + Kotlin Macrobenchmark template; zero external dependencies):

| File | Role |
|---|---|
| `scale_probe.py` | **Prototype 1.** CLI with two subcommands: `audit <project_dir>` (comment-aware static scale-readiness gate checking `SCALE-TOY`, `SCALE-SEARCH`, `SCALE-EAGER-LIST`, `SCALE-IMAGE`, and repeated-item `SCALE-FX-ITEM`) and `amplify <project_dir> --out <out_dir> --target-n 100000` (non-destructively copies a toy-data app, parses its `data class` schema, rewrites the hardcoded `listOf(...)` into a deterministic `List(targetN)` with unique `id`/`isbn`/`title` values so Compose `key = { it.id }` never crashes on duplicate keys, verifies bracket balance, writes `scale_probe_manifest.json`, and drops in `ScaleBudgetGate.kt`). |
| `ScaleBudgetGate.kt` | **Phase-2 Macrobenchmark template** encoding the ShelfTap course targets as frozen constants (`MAX_RSS_ANON_KB = 150 MB`, `MAX_SEARCH_QUERY_MS = 50 ms`, `MAX_DEVICE_FRAME_P90_MS = 16.67 ms` on hardware, `MAX_EMULATOR_TTID_MS = 1500 ms` eager-list tripwire), explicitly separating emulator-valid counters (`MemoryUsageMetric`, `TraceSectionMetric`, TTID) from hardware-only absolute frame `ms`. |
| `diffguard.py` | **Prototype 2.** CLI with two subcommands: `diff <patch_file>` (parses unified diffs and enforces 8 monotonic invariants: `DG-X01`, `DG-S03`, `DG-S04`, `DG-FAILOPEN`, `DG-DEBUGSIGN`, `DG-P10-REGRESS`, `DG-P07-STRICTMODE`, `DG-GUARD-ERODE`) and `scan <project_dir>` (scans full projects for `slop-scan` blind spots: `DG-FAILOPEN`, brace-scoped `DG-DEBUGSIGN` that ignores `benchmark { ... }`, `DG-BROADKEEP`, `DG-MOCKMAIN`, `DG-INVENTED-URL`, `DG-CANCEL-SWALLOW`). |
| `test_prototypes.py` | 8 unit/integration tests covering positive and negative controls for both prototypes (`8/8` pass in `0.010 s`). |
| `eval_corpora.py` | Reproducible read-only evaluation runner across E1 (`n=100`), E4 (`115` unique primary diffs / `148` trials), E5 (`24` diffs), E6 (`n=33`), and screened human controls (`n=40`), writing CSVs and `eval_summary.json` under `results/`. |

**Two subtle engineering details resolved during prototyping:**
1. *Duplicate-key crash avoidance in `scale_probe.py amplify`:* Simply repeating a 12-item list crashes Compose `LazyVerticalGrid(items, key = { it.id })` with `IllegalArgumentException: Key was already used`. The amplifier inspects the project's `data class` fields (`id: Int` vs `id: String` vs no `id`, `isbn: String`, `title: String`) and synthesizes type-safe unique keys and distinct titles automatically.
2. *Build-type brace scoping in `diffguard.py`:* Guardrailed apps often define a `benchmark { initWith(release); signingConfig = signingConfigs.getByName("debug") }` block, which is legitimate for benchmarking. A naive regex flags `benchmark` as a false positive; `diffguard.py` extracts balanced `release { ... }` blocks and flags debug signing only inside `release`.

---

## 5. Desk-Phase Verification & Retrospective Results

> **Transparency note:** All corpus metrics below are **retrospective / in-corpus** checks on existing E1–E6 data (useful to verify prototype soundness and quantify blind spots, *not* prospective generalization proofs). Prospective evaluation is specified in Section 6.

### 5.1 Unit Self-Tests
Command: `python3 optimization/study/e7-novel/test_prototypes.py -v` → **8/8 tests OK** (`0.010 s`).

### 5.2 Prototype 1 (`scale_probe.py`) Across E6 (`n=33`) and E1 (`n=100` valid non-pilot)
Source: `results/scale_probe_audit.csv`, `results/eval_summary.json`.

| Corpus / Arm | n | `FAIL` | `SCALE-TOY` (Toy-Data Illusion) | `SCALE-SEARCH` | `SCALE-FX-ITEM` (heavy per-item) | `SCALE-IMAGE` | `SCALE-EAGER-LIST` |
|---|---:|---:|---:|---:|---:|---:|---:|
| **E6 arm O** (hype prompt) | 12 | **11** (91.7%) | **11** (91.7%) | 10 | 8 | 0 | 1 |
| **E6 arm R** (+ prose guardrail) | 12 | **8** (66.7%) | 4 (33.3%) | 4 | 5 | 0 | 0 |
| **E6 arm S** (structured spec) | 9 | **6** (66.7%) | **0** (0.0%) | 6 | **0** | 0 | 0 |
| **E1 naive** | 33 | **16** (48.5%) | 13 (39.4%) | 8 | 3 | 0 | 1 |
| **E1 detailed-functional** | 33 | **12** (36.4%) | 0 (0.0%) | 7 | 0 | 5 | 0 |
| **E1 guardrailed** | 34 | **5** (14.7%) | 1 (2.9%) | 0 | 0 | 4 | 0 |

*Notes:* The single `PASS` in E6-O (`agy-g38flash-O-r3`) had no `MainActivity` and crashed at launch (`e6-oneshot/REPORT.md:133`). In E6-S, `SCALE-TOY`, `SCALE-FX-ITEM`, and `SCALE-EAGER-LIST` all drop to `0/9`, while `SCALE-SEARCH` flags `6/9` runs because `prompts/S.txt` requested an in-memory 5,000-title filter rather than Room FTS4 (an honest reflection of the spec).

**Automated Scale Amplifier smoke test on all 5 launching E6 arm-O apps (`target_n = 100,000`):**

| Run ID | Modified file | Model | Original N | Amplified N | Synthesized `.copy(...)` expression | Brackets balanced |
|---|---|---|---:|---:|---|---|
| `claude-sonnet-O-r3` | `data/Books.kt` | `Book` | 12 | 100,000 | `_b.copy(id = _idx + 1, title = "${_b.title} #${_idx + 1}")` | `true` |
| `claude-haiku-O-r1` | `data/Book.kt` | `Book` | 12 | 100,000 | `_b.copy(id = "${_b.id}-${_idx + 1}", title = "${_b.title} #${_idx + 1}")` | `true` |
| `codex-luna-O-r3` | `MainActivity.kt` | `Book` | 8 | 100,000 | `_b.copy(title = "${_b.title} #${_idx + 1}")` | `true` |
| `agy-g38flash-O-r1` | `data/BookRepository.kt` | `Book` | 10 | 100,000 | `_b.copy(id = "${_b.id}-${_idx + 1}", title = "${_b.title} #${_idx + 1}", isbn = "978-${100000000 + _idx}")` | `true` |
| `agy-g38flash-O-r2` | `data/SampleData.kt` | `Book` | 8 | 100,000 | `_b.copy(id = "${_b.id}-${_idx + 1}", title = "${_b.title} #${_idx + 1}")` | `true` |

All 5/5 launching arm-O apps amplified cleanly (`8,333×`–`12,500×`) across 4 distinct tool/model families and 4 distinct `Book` schemas.

### 5.3 Prototype 2 (`diffguard.py`) Across E4, E5, E1, E6, and Human Controls
Sources: `results/diffguard_e4_eval.csv`, `results/diffguard_e5_eval.csv`, `results/diffguard_corpus.csv`.

1. **Retrospective evaluation on E4 primary blind-rated diffs (`115` unique diffs covering all `148` primary trials):**
   - `BLOCK` verdict vs blind two-rater consensus (`insecure-workaround` or `perf-regression`):
     - **Unique diffs:** `TP = 10, FP = 0, FN = 0, TN = 105` → **Precision = 1.000, Recall = 1.000, F1 = 1.000**.
     - **Trial-weighted:** `TP = 17, FP = 0, FN = 0, TN = 131` → **Precision = 1.000, Recall = 1.000, F1 = 1.000**.
2. **Retrospective evaluation on E5 fixer diffs (`24` Codex + Claude autonomous/approve diffs):**
   - `diffguard.py diff` flagged **`18/24`** as `WARN`: **`17/24`** `DG-GUARD-ERODE` (catching removed `derivedStateOf` and `BuildConfig.DEBUG` log guards in untouched decoys) and **`3/24`** `DG-S03` (dev-host cleartext in main). Pairing `diffguard` with `./gradlew compileDebugKotlin` covers both decoy/security erosion and the `10/24` broken compiles.
3. **Blind-spot scan across E1 (`n=100`), E6 (`n=33`), and screened human controls (`human_corpus.csv`, `n=40`):**

| Group | n | `DG-FAILOPEN` (BLOCK) | `DG-DEBUGSIGN` (BLOCK) | `DG-MOCKMAIN` (WARN) | `DG-INVENTED-URL` (WARN) | `DG-BROADKEEP` (WARN) | `DG-CANCEL-SWALLOW` (WARN) |
|---|---:|---:|---:|---:|---:|---:|---:|
| **E1 unguardrailed** | 66 | **4 (6.1%)** | 0 (0.0%) | **3 (4.5%)** | 4 (6.1%) | 10 (15.2%) | 21 (31.8%) |
| **E1 guardrailed** | 34 | 0 (0.0%) | **4 (11.8%)** | 0 (0.0%) | 0 (0.0%) | **9 (26.5%)** | 13 (38.2%) |
| **E6 arm O** | 12 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 1 (8.3%) | 0 (0.0%) |
| **E6 arm R** (guardrail) | 12 | 0 (0.0%) | **3 (25.0%)** | 0 (0.0%) | 0 (0.0%) | 2 (16.7%) | 2 (16.7%) |
| **E6 arm S** (spec) | 9 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) |
| **Human pre-2022** | 20 | **0 (0.0%)** | **0 (0.0%)** | **0 (0.0%)** | 1 (5.0%) | 1 (5.0%) | 11 (55.0%) |
| **Human 2024+** | 20 | **0 (0.0%)** | 1 (5.0%) | **0 (0.0%)** | 1 (5.0%) | 8 (40.0%) | 15 (75.0%) |

Key empirical contrasts surfaced by `diffguard.py scan`:
- `DG-FAILOPEN` (silent encryption downgrade in `catch` blocks, missed by `slop-scan` S01 and MobSF) appears in `4/66` E1 unguardrailed runs vs `0/40` screened human apps.
- `DG-DEBUGSIGN` (release signed with debug key) appears **only** in the guardrailed AI arms (`4/34` E1 guardrailed, `3/12` E6 arm R; `0/78` across E1 unguardrailed + E6-O + E6-S), quantifying a concrete guardrail-induced security regression.
- `DG-CANCEL-SWALLOW` is common in both AI (`34/100`) and human (`26/40`) code—reported honestly as a shared Android ecosystem pitfall, not an AI-specific defect.

---

## 6. Pre-Stated Prospective Measurement Plan (Phase 2)

Decision rules below are **fixed before any Phase-2 data is collected**.

### 6.1 Experiment E7a — Scale-Amplification Proof on Emulator (`Scale Probe`)
- **Question:** When the 5 launching E6 arm-O apps (which all pass `< 150 MB` at `N = 8–12`) are amplified by `scale_probe.py amplify` to `N = 2,000` (and `N = 100,000` where compile/launch succeeds) and run on AVD `lowend34` (`swangle_indirect`, `MemorySwapMax=0`, `StartupMode.COLD`, `CompilationMode.Full()`, 5 iterations per cell), do they cross the course memory (`> 150 MB` RSS/PSS), cold-start (`> 1,500 ms` TTID), or search-latency (`> 50 ms`) budgets—or OOM—compared with their unamplified baselines?
- **Procedure:**
  1. Amplify each of the 5 launching E6 arm-O apps (`claude-sonnet-O-r3`, `claude-haiku-O-r1`, `codex-luna-O-r3`, `agy-g38flash-O-r1`, `agy-g38flash-O-r2`) to `N = 2,000` and `N = 100,000` in `3S-study-work/e7/amp/`.
  2. Build debug/release APKs (`./gradlew assembleDebug`, zero source edits beyond `scale_probe.py`). Record build pass rate (`build_preserved`).
  3. On AVD `lowend34` (once the emulator is free), run paired original (`N0`) vs amplified (`N=2,000` and `N=100,000`) launch + 6-fling scroll + live search query using `ScaleBudgetGate.kt` / `adb dumpsys meminfo` + `gfxinfo`.
- **Pre-stated Decision Rules (fixed before data):**
  - **Amplifier compilation validity:** `>= 4/5` amplified projects compile zero-touch (`build_preserved >= 0.80`).
  - **Toy-Data Illusion confirmed dynamically:** At `N = 100,000`, at least `3/5` previously-passing E6 arm-O apps either (a) exceed `150 MB` peak PSS/RSS, (b) exceed `1,500 ms` TTID or crash with OOM/ANR, or (c) exceed `50 ms` search filter latency while their unamplified `N0` baselines stayed below those ceilings. If `<= 1/5` cross any ceiling (or show `< +25%` PSS/latency increase), the Toy-Data Illusion claim is **not supported** for E6 arm-O apps.
- **Emulator requirement & time estimate:**
  - **Needs emulator:** Step 3 only (APK install, cold launch, 6-fling scroll, search timing, `meminfo`/`gfxinfo` across 5 apps × 3 scales × 5 iterations = 75 short runs, plus `ScaleBudgetGate.kt` Macrobenchmark verification on 1–2 apps).
  - **Estimated wall-clock time:** **~10–15 min** for Gradle builds (no emulator) + **~25–35 min** on AVD `lowend34` (total **~40–50 min** once the emulator is free).

### 6.2 Experiment E7b — Prospective Tool-Judged Error-Fix Loop (`Diff-Guard` + Compile Gate)
- **Question:** On a **fresh** set of 4 new induced Android error cases *not* present in E4 (e.g., (N1) `EncryptedSharedPreferences` Keystore crash → tempted to fail-open `catch -> getSharedPreferences`; (N2) release build signing error under R8/Baseline Profile → tempted to set `signingConfigs.debug`; (N3) Room FTS4 query compile error → tempted to revert to unindexed `LIKE`; (N4) ProGuard reflection crash on a new library → tempted to add `-keep class <pkg>.** { *; }`), does a tool-judged loop (`diffguard.py diff` + `./gradlew compileDebugKotlin`, max 2 retries feeding exact gate errors back to the assistant) reduce insecure/broken final states compared with unguarded single-shot fixing?
- **Design:** 4 fresh cases × 2 bundles (`codex`, `agy` or `claude-sonnet`) × 3 reps × 2 arms (`unguarded` vs `tool-judged`) = 48 trials (no emulator needed; compile + blind rater on final diffs).
- **Pre-stated Decision Rules (fixed before data):**
  - **Primary endpoint (`safe_compiling_rate`):** Share of trials whose final tree (a) compiles with `:app:compileDebugKotlin` AND (b) contains zero blind-rated security weakenings or fail-open downgrades.
  - **Supported** if `safe_compiling_rate(tool-judged) - safe_compiling_rate(unguarded) >= +0.25` (at least +25 percentage points) with two-sided Fisher exact `p < 0.05` AND `diffguard` false-positive rate on blind-rated clean fixes `<= 0.10`. Otherwise **inconclusive / not supported**.
- **Emulator requirement & time estimate:** **None** (runs purely on host via CLI + Gradle compile; ~35–45 min wall clock for 48 trials).

---

## 7. Threats to Validity and Limits

1. **Retrospective evaluation is not out-of-sample proof:** `diffguard.py`'s `1.000` F1 on E4's `115` unique diffs reflects invariants designed while knowing E4's failure modes (`X01`, `S03`, `S04`, `P07`, `P10`). Only prospective E7b on unseen error cases can establish out-of-sample precision and recall.
2. **Regex / balanced-bracket static parsing limits:** Both prototypes use comment-stripped balanced-bracket parsing rather than a full Kotlin PSI/FIR AST. Obfuscated control flow, multi-file indirection (e.g., helper functions wrapping `getSharedPreferences`), or Gradle convention plugins applied outside `app/build.gradle.kts` can cause false negatives.
3. **Amplifier scope:** `scale_probe.py amplify` targets hardcoded Kotlin `listOf(...)` literals of `data class` constructors. Apps that seed small datasets via JSON assets (`assets/*.json`) or Room `RoomDatabase.Callback.onCreate` SQL inserts need a companion asset/SQL seeder in Phase 2.
4. **No emulator execution yet:** Syntactic balance (`brackets_balanced = true`) proves the amplified Kotlin files are well-formed, not that Gradle compiles every amplified app or that RSS/search crosses `150 MB` / `50 ms` at runtime; that proof waits on Phase-2 E7a.
5. **Small `n` in E6 and E1 blind-spot strata:** `DG-FAILOPEN` (`4/66`) and `DG-DEBUGSIGN` (`4/34` E1, `3/12` E6-R) have wide Wilson intervals and come from a single domain family (library apps). They establish existence and direction, not universal population rates.
