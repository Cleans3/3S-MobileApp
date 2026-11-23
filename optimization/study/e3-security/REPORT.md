# E3 security triage of the study APKs (RQ3, protocol §4.3)

Author: beta-f2-4. Descriptive study, no confirmatory endpoint. Everything below is regenerated from `results/` by `harness/build_findings.py` and `harness/e3_analyze.py`.

## 1. Question and scope

RQ3 asks what fraction of the security problems in AI-generated apps independent static tools find, and with what precision. This report covers 13 APKs: the ShelfTap product release build, the two E2 optlab builds (`optlab-noR8` = A of P10, `optlab-release` = B of P10, both debug-signed) and the 10 E1 corpus survivors selected for E3 (`results/corpus_survivors.csv`: agy 4, claude 3, codex 3; two of them failed the zero-touch build, `build_zero_touch = fail`). The human reference apps of §4.3 were not scanned: the assignment named A/B and survivor APKs only. Android Lint was not needed as a fallback because MobSF ran on every target.

## 2. Method

| item | value |
|---|---|
| MobSF | 4.6.9, Python venv, no Docker, REST API (upload, scan), static analysis only; the server is started and stopped by `e3_run.py` |
| jadx | 1.5.7, `-j 1 -Xmx1536m --no-imports` (see §6, the first run used `-j 2 -Xmx1g` and was incomplete) |
| secret scan | regex over decompiled app code and `res/values`, library prefixes excluded (`e3_run.secret_scan`) |
| host | Intel i7-11800H, 15 GB RAM, Linux 7.0.0-43, Python 3.12.3, OpenJDK 21.0.13.1 |
| execution | one scan at a time, under the shared bench lock (`bench_shared`), paused while MemAvailable < 1.5 GB |

Candidate findings are MobSF findings of severity high or warning (manifest, network config, code analysis, certificate, MobSF secrets) plus the regex scan. Triage:

- **tp** = the described weakness is factually present in the artifact (decoded manifest, decompiled code, certificate info) and deviates from MASVS guidance.
- **fp** = factually wrong, in third-party library code the app does not use for the flagged purpose, or a platform default flagged as a weakness.
- Where a deterministic check exists it decides (`e3_run.triage`: allowBackup, debuggable, cleartext, minSdk, library-only file lists). The remaining rows are triaged by documented rules in `results/triage_manual.csv` (31 rules, matched on target, native rule and evidence prefix; each rule carries its reason). No row is left unknown.
- One triager (beta-f2-4), so there is no triage kappa. The triager did not generate any scanned code. Triage is a factual check against the manifest and decompiled code, not a run of individual MASTG tests; MASVS areas come from MobSF's metadata plus a small normalisation table in `build_findings.py`.

## 3. Results

findings 103 on 13 targets, tp 30, fp 73, precision 0.291 (Wilson 95%: (0.212, 0.385))

### 3.1 By tool and MASVS area (precision with Wilson 95% interval)

| tool | area | n | tp | fp | precision | lo | hi |
|---|---|---|---|---|---|---|---|
| jadx | MASVS-STORAGE | 1 | 0 | 1 | 0.0 | 0 | 0.793 |
| mobsf | MASVS-CODE | 14 | 11 | 3 | 0.786 | 0.524 | 0.924 |
| mobsf | MASVS-CRYPTO | 2 | 0 | 2 | 0.0 | 0 | 0.658 |
| mobsf | MASVS-NETWORK | 2 | 0 | 2 | 0.0 | 0 | 0.658 |
| mobsf | MASVS-PLATFORM | 22 | 0 | 22 | 0.0 | 0.0 | 0.149 |
| mobsf | MASVS-RESILIENCE | 25 | 13 | 12 | 0.52 | 0.335 | 0.7 |
| mobsf | MASVS-STORAGE | 37 | 6 | 31 | 0.162 | 0.077 | 0.311 |

`jadx` is the regex secret scan (one hit in total, a false positive); every other row is MobSF.

### 3.2 By target

| target | kind | tool | style | zero-touch | n | tp | fp | precision |
|---|---|---|---|---|---|---|---|---|
| agy-g38flash-B1-detailed-functional-r1 | debug-type (corpus survivor) | agy | detailed-functional | pass | 12 | 3 | 9 | 0.25 |
| agy-g38flash-B1-guardrailed-r2 | debug-type (corpus survivor) | agy | guardrailed | fail | 11 | 2 | 9 | 0.182 |
| agy-g38flash-B3-detailed-functional-r3 | debug-type (corpus survivor) | agy | detailed-functional | pass | 12 | 3 | 9 | 0.25 |
| agy-g38flash-B3-naive-r2 | debug-type (corpus survivor) | agy | naive | pass | 7 | 3 | 4 | 0.429 |
| claude-haiku-B2-guardrailed-r2 | debug-type (corpus survivor) | claude | guardrailed | pass | 9 | 2 | 7 | 0.222 |
| claude-haiku-B2-naive-r3 | debug-type (corpus survivor) | claude | naive | pass | 9 | 2 | 7 | 0.222 |
| claude-haiku-B3-naive-r3 | debug-type (corpus survivor) | claude | naive | pass | 6 | 3 | 3 | 0.5 |
| codex-luna-B1-naive-r2 | debug-type (corpus survivor) | codex | naive | fail | 5 | 3 | 2 | 0.6 |
| codex-luna-B2-guardrailed-r2 | debug-type (corpus survivor) | codex | guardrailed | pass | 7 | 2 | 5 | 0.286 |
| codex-luna-B3-naive-r1 | debug-type (corpus survivor) | codex | naive | pass | 5 | 4 | 1 | 0.8 |
| optlab-noR8 | release-type (study app) |  |  |  | 8 | 1 | 7 | 0.125 |
| optlab-release | release-type (study app) |  |  |  | 6 | 1 | 5 | 0.167 |
| shelftap-product-release | release-type (product) |  |  |  | 6 | 1 | 5 | 0.167 |

### 3.3 By generator bundle (survivors only)

| tool | scans | tp | fp | tp/scan | precision |
|---|---|---|---|---|---|
| agy | 4 | 11 | 31 | 2.75 | 0.262 |
| claude | 3 | 7 | 17 | 2.33 | 0.292 |
| codex | 3 | 9 | 8 | 3.0 | 0.529 |

Descriptive only: 3 or 4 scans per bundle, and all 30 true positives are manifest, build-configuration or signing facts.

### 3.4 What the 30 true positives are

debuggable=true: 10 (all survivors, which are debug builds); minSdk below the supported-version recommendation: 11; allowBackup (set true or absent without rules): 6; debug certificate on a release-type artifact: 2 (the optlab pair); Janus (v1 signature with minSdk 23): 1. **No true positive is a code-level weakness**: MobSF found no TLS bypass, no hard-coded credential, no plaintext store and no cleartext traffic in any target. All 22 MASVS-PLATFORM findings (exported components) and both MASVS-NETWORK and MASVS-CRYPTO findings are false positives (library receivers guarded by system permissions, explicitly exported components, platform-default trust anchors, library code for the flagged RNG and logging uses).

## 4. Sensitivity to the label rule

| reading | tp | fp | precision |
|---|---|---|---|
| as triaged (primary) | 30 | 73 | 0.291 |
| strict: library and debug-artifact fps counted tp (what a scan of the artifact reports as real) | 83 | 20 | 0.806 |
| release-relevant: findings that only describe the debug build type of a debug-type artifact removed | 20 | 62 | 0.244 |
| release-type artifacts only (3 targets) | 3 | 17 | 0.15 |
| debug-type survivors only (10 targets) | 27 | 56 | 0.325 |

fp categories: {'library': 42, 'other': 20, 'debug-artifact': 11}; build-type rows removed in the third reading: 10 tp, 11 fp

The primary rule counts `debuggable=true` on a debug build as a true positive (automatic, factual check) while the manual rules call a debug certificate or `BuildConfig.DEBUG` on a debug-type survivor a false positive ("describes the build type"). That is inconsistent. The third row removes every finding that only describes the debug build type of a debug-type artifact (10 tp, 11 fp); the second row is the opposite reading, in which a scan of the artifact counts library and debug-artifact findings as real. Precision runs from 0.24 to 0.81 depending on the reading, so the headline 0.29 describes MobSF's default output on these artifacts, not a property of the apps.

## 5. Detector against tool: slop-scan (E1) vs MobSF

Items are survivor x security class for the 8 classes both tools have a rule for (S01-S06, S08, S09); slop-scan S10 and S12 are cited-only and MobSF has no S07 mapping.

| comparison | items | both | slop only | mobsf only | neither | kappa | PABAK | agreement |
|---|---|---|---|---|---|---|---|---|
| MobSF raw (any finding of the class) | 80 | 7 | 2 | 6 | 65 | 0.581 | 0.8 | 0.9 |
| MobSF triaged tp | 80 | 6 | 3 | 0 | 71 | 0.78 | 0.925 | 0.963 |

The two readings differ because slop-scan agrees with MobSF's triaged findings almost only on S06 (allowBackup, 6 of 6). The slop-scan side is raw detector output; E1 reports its precision.

### Found by one tool only

| target | class | found by | evidence |
|---|---|---|---|
| agy-g38flash-B3-detailed-functional-r3 | S05 | slop-scan only | HttpLoggingInterceptor Level.BODY not guarded for release |
| agy-g38flash-B3-detailed-functional-r3 | S09 | slop-scan only | SQL built by interpolation: execSQL("DROP TABLE IF EXISTS $TABLE_LOANS") db.execSQL("DROP |
| claude-haiku-B2-naive-r3 | S01 | slop-scan only | Preferences DataStore key "access_token" stored without encryption |

Checked against the artifacts: (a) `access_token` held under a Preferences DataStore key in `claude-haiku-B2-naive-r3` is real, plaintext token storage, and MobSF has no rule for it; (b) `HttpLoggingInterceptor` at `Level.BODY` in `agy-g38flash-B3-detailed-functional-r3` is real in the source (`NetworkClient.kt`, not guarded for release); (c) the four `DROP TABLE IF EXISTS $TABLE_*` lines interpolates compile-time constants and is not injectable. In the 8 compared classes MobSF found nothing among its true positives that slop-scan lacks, but its true positives are manifest facts, which slop-scan only covers for allowBackup. In short: the two tools see different layers (MobSF the manifest and build configuration, slop-scan the source), and code-level security slop was found only by the source detector.

## 6. Defects found and deviations

1. **Incomplete jadx trees in the first scans.** The first run used `-j 2 -Xmx1g` and ignored jadx's exit status; it wrote only part of the classes. MobSF results and the manifest-based triage do not use that tree, so tp/fp labels are unaffected. The jadx secret scan and the triage notes that cited "not found in the jadx output" were affected. Repair: `harness/e3_rejadx.py` re-ran jadx with `-j 1 -Xmx1536m` for all 13 targets (each tree archived as `e3/archive/<target>.jadx-full.tgz`) and reran the secret scan. App classes written now match the dex in every re-run target; the scan found one hit (the known false positive) and no new credentials; the credential-looking literals in `results/jadx_literals.csv` are field names, route names and `toString` text. The four 32-hex "weak" false positives are Room schema `identity_hash` values (found in the generated database classes), now documented as such. `e3_run.py` now uses the same settings and fails on OOM.

| target | app top-level classes in dex | in old tree | in new tree | java files old | java files new |
|---|---|---|---|---|---|
| agy-g38flash-B1-detailed-functional-r1 | 32 | 16 | 32 | 16720 | 17649 |
| agy-g38flash-B1-guardrailed-r2 | 36 | 17 | 36 | 5220 | 18415 |
| agy-g38flash-B3-detailed-functional-r3 | 38 | 26 | 38 | 16670 | 17314 |
| agy-g38flash-B3-naive-r2 | 18 | 10 | 18 | 16502 | 16663 |
| claude-haiku-B2-guardrailed-r2 | 32 | 16 | 32 | 5419 | 7797 |
| claude-haiku-B2-naive-r3 | 22 | 19 | 22 | 6394 | 6879 |
| claude-haiku-B3-naive-r3 | 2 | 2 | 2 | 6138 | 6351 |
| codex-luna-B1-naive-r2 | 5 | 5 | 5 | 2868 | 2868 |
| codex-luna-B2-guardrailed-r2 | 12 | 3 | 12 | 3428 | 5951 |
| codex-luna-B3-naive-r1 | 4 | 4 | 4 | 688 | 688 |
| optlab-noR8 | 37 | 22 | 38 | 7293 | 7907 |
| optlab-release | 1 | 2 | 2 | 2510 | 2507 |

2. **Product release APK re-run on a rebuilt file.** The APK scanned on 2026-11-21 was deleted; beta-f2-3 rebuilt `app-release-unsigned.apk` from HEAD e0781e0 (2026-11-23, sha256 7ff008313f67ef5d..., minified release type) and jadx was run on it. The tree has 3,801 java files, the same count as the first (also complete) tree, no app classes are missing, and the regex scan found 0 hits and 0 credential-looking literals. The 6 MobSF rows of this target come from the 2026-11-21 build, not from the rebuilt file.
3. **ENOSPC and file records.** Three codex scans failed on ENOSPC from the ntfs-3g volume (file-record exhaustion, not disk fill) and were repeated; a failed scan wrote no rows. After each scan `e3_run.py` now archives the report and the work tree into one tarball (entry count verified before deletion), which took mobsf-data from 308,140 to 38 files and e3 from 97,266 to 35.
4. **Scope.** Human reference apps not scanned (§1); no second triager (§2).

## 7. Threats to validity

- 13 artifacts, 10 of them from one generator pipeline; per-bundle numbers are not comparable across bundles.
- Debug-type survivors make debuggable, debug certificate and `BuildConfig.DEBUG` findings artifacts of the build type (§4).
- One triager and rule-based triage: reproducible from `triage_manual.csv`, but not independently rated.
- MobSF 4.6.9 rules only; `android_hardcoded` and `secret` are keyword and regex rules that produced 24 false positives (the regex scan added one).
- slop-scan results are not re-triaged here.

## 8. Files

`results/findings.csv` (103 triaged findings), `findings_candidates.csv`, `triage_manual.csv`, `summary_e3.csv`, `by_area.csv`, `by_target.csv`, `by_bundle.csv`, `sensitivity.csv`, `agreement.csv`, `one_tool_only.csv`, `stats_e3.md`, `jadx_completeness.csv`, `jadx_rows.csv`, `jadx_literals.csv`, `mobsf/<target>.json.gz`; archives in `3S-study-work/e3/archive/` (per-scan `<target>.tgz`, `<target>.jadx-full.tgz`, `MobSF-4.6.9-venv.tgz`). Harness: `e3_run.py`, `e3_batch.py`, `e3_archive.py`, `e3_rejadx.py`, `build_findings.py`, `e3_analyze.py`, `mobsf_scan.py`.
