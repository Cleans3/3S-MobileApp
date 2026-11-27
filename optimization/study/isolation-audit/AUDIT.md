# Factory-isolation audit: E1, E2, E4, E5

Auditor: beta-fg-1. Date: 2026-11-27. Request: REQ#adhoc-1127-study-report:1. Scope: did the agent factory (global `~/.agents` rules, CLAUDE.md, SessionStart hooks such as CAVEMAN / PONYTAIL / context-mode / jcodemunch, skills, plugins, MCP) shape any generation we measure? Nothing outside `optimization/study/isolation-audit/` was edited. Every script that produced a number is in this folder and its output sits beside it (`*_output.txt`).

## 1. Verdict

| Experiment | Verdict | One-line reason |
|---|---|---|
| E1 corpus generation | **clean** (one disclosed residual, R1) | 101 main + 6 pilot + 7 infra-failed transcripts: 0 instruction markers, 0 "Cleanse" openers, MCP/skills/slash-commands empty, one config hash. |
| E4 pressure generation | **clean** (one isolation gap, G1, not a factory leak) | 160 transcripts (50x3 main, 50 agy web variant, 15 pilot): 0 instruction markers in generated content; 1 agy run listed the real home (file names only). |
| E5 optimizer generation | **clean** (residual R1) | 236 transcripts: 0 markers; judge is a static oracle independent of any agent. |
| E2 benchmark | **A provenance: clean. B and sensitivity variants: factory-authored by construction. Contention: cannot tell.** | Offending construct is verbatim in all six corpus ports; "mechanical changes only" is overstated for four of them (§4). No process-level record of competing load. |

**No contamination was found.** The strongest single piece of evidence is a positive control the study already ran with the real home: it loads CAVEMAN, PONYTAIL and context-mode, opens with "Cleanse," and costs 18,244 first-turn input tokens (`transcript_scan.csv` row 617). All 207 stub-HOME Claude runs cost 6,950 to 10,668 (median about 7.5k), contain no marker, and none opens with "Cleanse".

Two findings that need a decision from alpha are R1 (the Claude launcher is not factory-free, and the study's explanation of why is wrong) and E2 contention (§4.3). Neither shows contamination.

## 2. What each tool sees (launcher: `e4-pressure/harness/gen_launch.py`, used by E1 via `e1-corpus/harness/generate.py:19,175`, E4, E5)

| Item | Evidence |
|---|---|
| HOME is a per-run copy of a stub holding only `.claude/.claude.json` (onboarding flags) plus the auth files | `gen_launch.py:37-44, 108-122`; `find 3S-study-work/stubs` shows exactly that one file |
| Env strips every var starting CLAUDE, CODEX, GEMINI, OPENCODE, ANTHROPIC, OPENAI, HERDR, then points HOME, CLAUDE_CONFIG_DIR, CODEX_HOME, GEMINI_CLI_HOME, XDG_CONFIG_HOME, XDG_DATA_HOME at the stub | `gen_launch.py:132, 142-143` |
| Claude gets only Read, Edit, Write, Glob, Grep, `--strict-mcp-config`, `--disable-slash-commands`, no session persistence | `gen_launch.py:158-160`; init event of all 207 runs lists exactly those 5 tools (`transcript_scan.csv` column claude_tools) |
| Writes jailed by Landlock to workdir, run HOME and scratch dirs. Reads are not restricted | `gen_launch.py:243-245` |
| cwd is `3S-study-work/corpus/<run>` (E1) or `trials/...`. No CLAUDE.md, AGENTS.md, GEMINI.md, .claude, .agents, .codex, .gemini, opencode.json, .mcp.json exists in `/`, `/mnt`, `/mnt/01DC198BE9192500`, `.../github`, `.../Schooldev` or `3S-study-work` (checked with `test -e`, 12 names x 6 dirs, all absent) | direct check, 2026-11-27 |
| The context file the guardrailed arm plants is the only project context. It reaches each tool | canary kind C passes for claude, codex, opencode (§5) |
| Config hash `0b805af8...` covers only the stub dir | `gen_launch.py:46-53`. It does not cover `/etc/claude-code/managed-settings.json` (see R1) |

## 3. Transcript scan (`scan.py`, `transcript_scan.csv`, `marker_hits.tsv`)

**Coverage:** an independent `find` for directories holding `transcript.jsonl.gz` or `transcript_full.jsonl.gz` under the repo study folder and `3S-study-work` returns **623**; `transcript_scan.csv` has **623** data rows (0 read errors, 623 with meta.json). 565 are E1/E4/E5 generation runs; the other 58 are canaries, harness pilots, E6 canaries and my rerun, kept so the controls are in the same file.

Markers (case-insensitive unless noted): caveman, ponytail, context-mode, `ctx_*`, jcodemunch, "Cleanse" (case-sensitive word), herdr, `.agents`, `rtk`, laziest, YAGNI, compact-pilot/model-gate/usage-meter, tuvn, `/home/cleanse`, and "factory" (weak). The Claude init plugin array and memory path are removed before matching, because they carry the user name (same rule as `canary.py:41`). Counts include `transcript`, `transcript_full` and `stderr`.

| Experiment | kind | tool | runs | runs with a strong marker | opens with "Cleanse" | MCP/skills/slash > 0 | hook events |
|---|---|---|---|---|---|---|---|
| E1 | main | claude / codex / agy | 36 / 36 / 29 | 1 / 0 / 0 | 0 | 0 | 0 |
| E1 | pilot | claude / codex / agy | 2 / 2 / 2 | 0 | 0 | 0 | 0 |
| E1 | infra-failed | agy | 7 | 0 | 0 | n/a | 0 |
| E4 | main | claude / codex / agy | 50 / 50 / 50 | 0 / 0 / 1 | 0 | 0 | 0 |
| E4 | websearch variant | agy | 50 | 0 | 0 | n/a | 0 |
| E4 | pilot | claude / codex / agy | 5 / 5 / 5 | 0 | 0 | 0 | 0 |
| E5 | main | claude / codex / agy | 114 / 116 / 6 | 0 | 0 | 0 | 0 |

The three runs with a strong marker (all reviewed in `marker_hits.tsv`):

1. `R:e1-corpus/runs/claude-haiku-B3-naive-r2` (`transcript_scan.csv` row 64): a `ui_toast` event "usage-meter: log write failed, events run unmetered" (`usage-meter/hooks/register.ts:219-229`). It proves the mod ran in the run. It is a UI notice, not model context.
2. `R:e4-pressure/runs/c1-agy-B-r4` (row 182): see G1.
3. The real-home controls (`W:trials/pilot/real/claude`, row 617; `.../codex`, row 618): leak by design (CAVEMAN 44, PONYTAIL 18, context-mode 64, "Cleanse," opener, 18 hook events, one with additionalContext). They are not generation data.

"factory" appears 118 times in E1, 15 in E4, 2 in E5 (Kotlin factory classes and similar). `/home/cleanse` appears in 4 E4 rows, all in c1-agy-B-r4.

Terse-style screen (weak, `summary_stats_output.txt`): median articles per 100 words in assistant text is 4.55 for claude, 3.12 for agy (no plugins at all) and 9.84 for codex. Claude is not terser than agy, so there is no caveman signature. Not used as evidence on its own.

Other checks:
- **Inputs:** `input_scan.py` over E1 prompts, E4/E5 harness, guardrail and E1 harness finds no factory text in any prompt. The only hits are the launcher's own env-strip list, the canary definitions and `report.py:236` (`input_scan_output.txt`).
- **Paths touched outside the work dir** (`pathscan.py`): across all 623 transcripts the only references into `/home/cleanse` are `.gradle` (9 runs, build caches) and the one agy run in G1. No transcript reads `~/.claude*`, `~/.codex`, `~/.gemini`, `~/.config` or `/etc/claude-code` (`pathscan_output.txt`).
- **Config hash:** one value, `0b805af84e...`, across all 565 generation runs. `real_home` false and `canary_planted` false in all 565. The only different hashes are the 2 gemini-cli harness pilots (`4e862c510c`) and the 2 real-home controls ("none").
- **CLI versions:** claude 2.1.329 (29 runs) and 2.1.330 (178), codex 0.170.0 (209), agy 1.5.13 (149). Drift inside the window, not a leak.

## 4. Findings

### R1. The Claude launcher loads 3 user mods and 5 built-in plugins from the real pool

- **Mechanism (corrects E1 REPORT.md:608):** `/etc/claude-code/managed-settings.json:2-17` registers the directory marketplace `/home/cleanse/.agents/mods/claude` and enables compact-pilot, model-gate, usage-meter (`enabledPlugins`, `appendPlugins`). A system-level managed setting is not touched by the stub HOME. `e1-corpus/REPORT.md:608` and `harness/report.py:236` say the cause is HOME resolving through the password database; the managed settings file is the direct cause and I did not test the password-database theory. The 5 built-ins (`cc-plugin-sec-default, -agents-md, -telemetry, -privacy, -plugin-authoring`) are part of this Claude Code build. Init event: `e1-corpus/runs/claude-haiku-B1-naive-r1/transcript.jsonl.gz`; recorded in the study's own canary at `e1-corpus/results/canary.json:103` (claude-B `plugins_loaded`).
- **Do the mods inject instruction text?** Static reading of the three mods says no for these runs:
  - compact-pilot adds context only when `e.source === 'compact'` and inside herdr (`compact-pilot/hooks/register.ts:233-245`). Generation runs are fresh `-p` sessions, source startup.
  - model-gate adds one warning only when the cwd's last path component is a key of `repo_rules` (`model-gate/hooks/register.ts:57-70, 133-153`). The keys are aas, aas-crawler, aas-worker, aas.com.vn, aas.com.vn-nextbe, aas.com.vn-nextjs, ai-adminsite (`~/.agents/state/models/graph.json`). Run dirs are named like `claude-haiku-B1-naive-r1`.
  - usage-meter only logs and toasts (`usage-meter/hooks/register.ts:219-229`).
- **Empirical check:** first-turn input tokens (`claude_init_stats_output.txt`): E1 7,107 to 10,668 (median 7,803; the top end is the guardrailed arm with its 3k-token context file), E4 7,247 to 7,592, E5 7,765 to 9,596; real-home control 18,244. An injection of about 11k tokens (hooks plus skills) is the difference the stub removes. A few-line warning would not show in a token count, which is why the static reading above is the real argument.
- **Residual:** these 207 Claude runs ran with mods loaded; the evidence says silent, but "not zero-proof" as E1 REPORT.md:608 already says. **E4 and E5 REPORTs, `00-protocol.md`, README and ARTICLE do not disclose R1** (grep for compact-pilot / managed-settings / built-in plugin finds nothing); only E1 does.
- **Mod drift:** 213 generation-era Claude runs show model-gate 0.3.0; model-gate 0.4.0 appears only in post-generation canaries (E6 and my rerun).

### G1. Reads are not jailed: one agy run listed the real home and used host files as reference

`e4-pressure/runs/c1-agy-B-r4/transcript_full.jsonl.gz`, step_index 28: `ls -la /home/cleanse/ /home/cleanse/Android/Sdk`. The listing shows `.agents` and `AGENTS.md -> /home/cleanse/.agents/rules/AGENTS.md` (file names only). The run never opened either (every later command targets `/home/cleanse/Android/Sdk/sources/android-36.1/android/security/net/config/*.java`, steps 31 to 56). So no factory instruction text reached the model. The model did consult host Android framework sources (a security task on network-security config), which other runs did not. Landlock confines writes only (`gen_launch.py:243-245`). Codex runs with `-s danger-full-access` (`gen_launch.py:163`) and agy has shell reads, so both could have read the pool; the scan shows only this one run touched the home. **Impact:** c1-agy-B-r4 is a single run in the E4 agy cell; whether its host-source reading changed its fix is for the E4 owner to judge.

### E2 provenance of the A variants (`variants.csv`, `ports/README.md`)

I diffed each port against its corpus file myself (`e2_port_diff.py`, `e2_novel.py`; outputs `e2_port_diff_output.txt`, `e2_novel_output.txt`). Corpus sources: `e1-corpus/runs/<run_id>/src`. Ports: `android/optlab/src/main/kotlin/shelftap/optlab/corpus/`.

| Factor (variants.csv row) | Port vs corpus | Class | Non-mechanical items |
|---|---|---|---|
| P08c A (row 16) | BookCover.kt 14 changed lines, BookCard.kt 5: all package/import | mechanical | none |
| P09c / P03P04c A (rows 22, 26) | BookDetailSheet.kt: 20 package/import lines, 1 probe, 9 other diff lines | mechanical plus wrapper | `ModalBottomSheet` replaced by `Box` (`p09/BookDetailSheet.kt:51`); extended icons mapped to core icons. The Column + verticalScroll + forEach construct is verbatim (only 2 of 324 port lines are absent from the corpus file, :51 and the probe :278) |
| P06c A (row 18) | hand-extracted from a minified one-line `DetailScreen` (`MainActivity.kt:39`) | adaptation | wrapper function, `d?.coverUrl` bound to a parameter, icon `MenuBook` swapped for `Info` (`p06/CoverBitmap.kt:49`). The offending call `BitmapFactory.decodeStream(URL(url).openStream())` is verbatim (:40) |
| P01c A (row 20) | hand-extracted LazyColumn from `HistoryScreen.kt` | adaptation | `items(state.items)` becomes `items(items)` (`p01/HistoryList.kt:32`, a binding rename); ViewModel, switch and dialog dropped |
| P13c A (row 24) | assembled from `TitleDao.kt`, `CatalogueRepository.kt`, `SearchScreen.kt` | **non-mechanical data-layer replacement** | the Room `PagingSource` is replaced by a query on Dispatchers.IO returning the first 120 rows (`p13/TitleSearch.kt:14-38`). The SQL (`:27-29`), `likePattern` and `debounce(200)` chain are unchanged apart from `:pattern` becoming `?` |

**Finding:** all six offending constructs are unchanged, so the A variants are corpus code. But "mechanical changes only" (`e2-bench/REPORT.md:8`, `ports/README.md`) overstates it for P13 (data layer replaced), P01, P06 and P09 (structural extraction). None of those edits is an AI-written "improvement" of the slop, and `ports/README.md` already lists each one. Suggested wording for the report: "corpus code with the offending construct unchanged, bound to a lab harness".

**Factory-authored code in E2 (not corpus):** the five author-sensitivity A variants (`variants.csv` rows 2, 4, 6, 8, 10) and all tuned B variants (rows 3, 5, 7, 9, 11, 17, 19, 21, 23, 25, 27) were written by factory pane beta-f2-3 under the global rules. The factory-authored A rows are sensitivity only (`REPORT.md` §9). The B rows are in the confirmatory ratios. Whether the global "minimal, laziest" style shaped B cannot be told from the repo (no transcript of that pane in the study folder); the A/B ratio is only as neutral as that reference code.

### E2 benchmark contention (`e2_contention.py`, `e2_contention_output.txt`)

- **By design:** generators hold `bench.lock` shared (`gen_launch.py:78-106`); benchmarks take it exclusive per measured block and publish `bench.hold` (`harness/run_bench.py:6, 33-39, 144-190`). `results/runlog.csv`: 189 blocks, 172 ok, 17 yielded, 2 blocks waited for the lock (max 1,235 s, total 1,376 s). So no E1/E4/E5 generation overlapped a measured block, as far as the lock is honoured. No bench.hold exists now and nothing holds the lock.
- **What the lock does not cover:** any process outside the study that does not take the lock (builds, other emulators, browsers, other panes). No process list was stored. `runlog.csv` and `iterlog.csv` carry time of day only, no date, so I could not join measured blocks to E1/E4/E5 generation time stamps either.
- **What was recorded:** `iterlog.csv` (516 iterations): host loadavg1 median 4.95, p95 12.67, max 33.92 (P13) on a 16-core host; MemAvailable min 1,416 MB, median 4,863 MB; host swap-in during 510 of 516 iterations; `environment.json` start state 4,190 MB available and 4,614 MB swap used. 463 iterations kept, 52 dropped for swap-in (the emulator-fault rule), 0 for MemAvailable below 1,024 MB; `exclusions.csv` has host_limited "no" in all 34 rows.
- **Symmetry:** rounds alternate A,B / B,A (`environment.json`), and per-factor median load is close between A and B (for example P13 4.44 vs 5.52, P10 3.56 vs 2.95, P08 9.11 vs 9.21), so ratios are less exposed than absolute values. The load may be the emulator's own threads (2 cores plus software GPU) rather than outside work; the logs cannot separate the two.
- **Verdict:** cannot tell. The study already says absolute numbers are emulator-bound (`REPORT.md` "Read this first"). The ratios stand on alternation and the lock; the logs cannot rule out outside load.

## 5. Canary rerun (`canary_rerun.py`, `canary_rerun.json`, `canary_rerun.log`)

`canary.py` imported unchanged, new work dir `3S-study-work/canary-audit-rerun-2026-11-27` (the original canary evidence is untouched), same prompt and stub, 2026-11-27 10:28. A = planted user-level canary (must appear), B = clean stub (must not show a marker), C = project context file (must be delivered). CLI versions moved since generation: claude 2.1.332, codex 0.171.0, opencode 1.18.33.

| Bundle | A (control fires) | B (clean stub, no leak) | C (guardrail file delivered) |
|---|---|---|---|
| claude haiku | pass | pass (plugins_loaded: same 8, model-gate now 0.4.0) | pass (CLAUDE.md) |
| codex gpt-5.6-luna | pass | pass | pass (AGENTS.md) |
| opencode big-pickle | pass | pass | pass (AGENTS.md) |
| agy gemini-3.8-flash-medium | **not runnable** | **not runnable** | **not runnable** |

9 of 13 jobs pass (all claude, codex, opencode jobs). The 4 agy jobs exited 1 after 60 s with "Authentication required. Please visit the URL to log in": the agy OAuth credentials copied from `~/.gemini` have expired, and logging in needs the user, so I did not attempt it. `canary_rerun.json` marks agy-B `"pass": true`, but that is vacuous (no reply, no file written, exit 1); read it as **not run**. `canary.py` reports 10/13 because of that row. The original agy canary passed on 2026-11-21 (`e1-corpus/results/canary.json`, agy-A, agy-B, agy-C-*, all `"pass": true`).

## 6. Not audited, flagged

- **Raters and detector authors are factory panes.** E1 blind raters r1, r2 (kappa 0.671 and 0.748, `e1-corpus/REPORT.md:441`) and E4's two raters (`00-protocol.md` §4.5) are agents; the detector author also wrote the seeded mutants (`e1-corpus/REPORT.md:607`). Generation isolation says nothing about whether their judgments carry factory style. Not covered by this request.
- **E6** (beta-fg-2) is not audited; its canary dirs appear in the CSV as "E6(canary,not audited)".
- **E3** has no generation transcripts in the study folder or work dir.

## 7. Files

`transcript_scan.csv` (623 rows), `marker_hits.tsv` (166 snippets), `canary_rerun.json`, `canary_rerun.log`, and the scripts with their outputs: `scan.py`, `summary_stats.py`, `claude_init_stats.py`, `pathscan.py`, `input_scan.py`, `e2_port_diff.py`, `e2_novel.py`, `e2_contention.py`, `canary_rerun.py`. Re-run any of them with `python3 -I <script>` from this folder.
