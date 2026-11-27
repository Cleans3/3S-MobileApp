# E6 amendment: the one-shot "social-media prompt" experiment

Written 2026-11-27 17:27 +07, BEFORE any E6 generation. Prompt hashes below are fixed at this time. Status: **exploratory**. No confirmatory test, no p-values, no claim of significance. Numbers are reported as counts, medians and ranges.

## 1. Question

Social media posts show a beautiful app UI and hand out a one-shot prompt that "gives the same result". Q1: when the same one-shot prompt is run through different tools and models, how far apart are the results (does it build, does it launch, how does it look, how smooth is it, how many slop findings)? Q2: do the two fixes a vibe coder can apply without leaving the prompt box, a rules file (arm R) or a structured prompt (arm S), change the outcome?

## 2. Arms (prompt text frozen in prompts/)

| Arm | What the generator receives | File |
|---|---|---|
| O | the one-shot hype prompt, written for this study in the style of social posts (no real post copied) | prompts/O.txt, sha256 `5d0f5571…47edf4c` |
| R | the same O prompt, plus ANDROID-GUARDRAIL.md placed in the run dir under the tool's native context-file name (CLAUDE.md / AGENTS.md / GEMINI.md), as E1's guardrailed style | prompts/O.txt + guardrail/ANDROID-GUARDRAIL.md, sha256 `ae563903…49cf` (frozen, unchanged) |
| S | a structured spec of the same app: screens, data, constraints (lazy lists with keys, sized covers, no blur/shadow/per-frame animation on items, sp text and font scale 2.0, touch targets, no network, allowBackup false, no secrets) and a definition of done (assembleDebug passes) | prompts/S.txt, sha256 `3c4c1236…0af090` |

All three start with the same harness preamble (prompts/PREAMBLE.txt, the first paragraph of E1's prompts word for word: write the project in the empty dir, no network, no questions, reply DONE). It is the harness, not part of the arm. Arm S states its data (a local generated catalogue) because O and R leave it open; that is part of what "a structured prompt" means, and is stated as such in the report.

## 3. Bundles (tool + model, launched through the isolated launcher only)

| Bundle | Launcher | Model arg |
|---|---|---|
| claude-haiku | claude | haiku |
| codex-luna | codex | gpt-5.6-luna |
| agy-g38flash | agy | gemini-3.8-flash-medium |
| claude-sonnet | claude (same launcher as claude-haiku) | claude-sonnet-5-5 |

claude-sonnet vs claude-haiku differ only in the model (same CLI, same launcher flags, same stub HOME), so that pair separates the model effect from the harness effect. A bundle that cannot run is recorded with the reason and the rest continues.

## 4. Design and order

4 bundles x 3 arms x 3 reps = 36 runs, one run = one prompt in, one project out (first draft, no retrieval, no repair by the generator), exactly as E1. Execution order: tier 1 = all 12 arm-O cells, tier 2 = the 24 arm R and S cells; each tier shuffled with `random.Random(20261127)`. The tiering is for priority (arm O gives the lead-in image first); it is not a randomisation of arms, so any drift over the day is confounded with arm. Plan written to results/plan.json.

## 5. Isolation (the point of the experiment)

Generation goes only through `e4-pressure/harness/gen_launch.py` (stub HOME, empty harness config, auth files only, env stripped of CLAUDE*/CODEX*/GEMINI*/OPENCODE*, Landlock write-jail, file-tools-only for claude, web tools denied for agy and codex), imported read-only. The factory's own session, config, skills, hooks and rules are never loaded. The canary (A positive control, B clean-stub, C context-file delivery) runs for every bundle before the first generation; result in results/canary.json. E1/E4 harness files are not edited: E6 imports them and patches only module-level paths in its own process.

## 6. Per-run measures

1. Generation: duration, tokens, cost where the CLI reports it, files, Kotlin LOC (non-blank non-comment), network/web audit, valid (has .kt and a Gradle file).
2. Build: E1's build_run.py as is (R7: zero-touch first, then at most 3 table repairs on the build copy, Gradle 9.6.0 wrapper on JDK 17). Columns: build, zero-touch, repairs, first error line. Detector input stays the unrepaired source.
3. Emulator (only if an APK exists), AVD test34 (1080x2280, 420 dpi, Google APIs x86_64 API 34), headless, KVM, `-no-snapshot -no-boot-anim -no-audio -no-window -memory 2048 -cores 2 -gpu swangle_indirect -port 5570` in a MemorySwapMax=0 scope; software GPU, so jank percentages are comparable inside E6 only, never with a phone. Fresh install per run, `pm clear`, then: launch (`am start -W` TotalTime), launch crash = yes if logcat has a FATAL EXCEPTION of the package or the process is gone 6 s after launch; screenshot of the first screen at font_scale 1.0 and 2.0; at 1.0 a scripted scroll (12 swipes, alternating down/up, `input swipe 540 1800 540 700 300`, 0.7 s apart) between `dumpsys gfxinfo <pkg> reset` and `dumpsys gfxinfo <pkg>`, recording janky frames % and total frames rendered; total PSS after the scroll (`dumpsys meminfo`). A scroll with fewer than 30 rendered frames is flagged "no-scroll" (first screen was not scrollable, or the app is not on its list screen); its jank% is kept but marked. One scripted pass per run, no repeats inside a run.
4. Detector: scan_corpus.scan_one (frozen v1: slop-scan + depcheck + detekt + lint rows) -> counts of S (security), P (performance), U (UI) findings, plus D (dependency) and the rest listed separately.
5. Screenshots stored as JPEG in img/<run_id>-f1.jpg and -f2.jpg. Contact sheets: grid-O.jpg (arm O, one rep per bundle) and grid-ORS.jpg. "Best rep" for the contact sheet is chosen by a rule fixed here, not by looks: among the cell's reps that launched without crash, the one with the lowest janky %; ties by lower rep number. It is a showcase of the best case, the report says so, and it shows the rep number.

## 7. What counts as success

- Pipeline success: every one of the 36 planned runs has a row in results/runs.csv (a failed run with its reason is a row), the canary result is saved, and arm O has at least one screenshot per bundle that built.
- Finding wording is pre-set. "Bundles differ" on a measure means the cell ranges (min to max over 3 reps) of two bundles do not overlap; "arms differ" likewise. Overlap = "no visible difference at n=3". Nothing stronger is claimed, and no per-finding generalisation outside this prompt, these CLI versions and this date.

## 8. Known limits (stated up front)

n = 3 per cell; one app idea; software-GPU emulator; models are not seeded (CLIs expose no temperature or seed); a tool can change between the canary and the last run (CLI versions are recorded per run); the claude-sonnet bundle is a larger model than the E1 generators and is a deliberate extra; arm S carries more information than arm O (that is what it is for), so S vs O is "prompt with an engineered spec vs hype prompt", not a controlled single-factor test; screenshots show the first screen only.

## 9. Addendum before the first generation (2026-11-27, canary run, no app generated yet)

Written after the canary and before any E6 generation (first generation timestamp is in each runs/<id>/meta.json, generated_at).

- Canary (results/canary.json): 12 of 12 pass, CLIs claude 2.1.332, codex 0.171.0, agy 1.5.18 (E1 canary of 2026-11-21 had claude 2.1.330, codex 0.170.0, agy 1.5.13). A positive control, B clean stub, C context-file delivery for all four bundles, claude-sonnet included.
- Launcher patch inside E6's process only: agy 1.5.18 stores its login in ~/.gemini/antigravity-cli/antigravity-oauth-token; gen_launch.AUTH only copied the 1.5.13 file, so the first canary of agy failed with "Authentication required". E6 adds that file to AUTH in-process. Effect: harness_config_hash differs from E1's for all bundles (the hash includes the auth file names); the stub HOME content itself is unchanged.
- Limitation found, not fixable from inside the study: /etc/claude-code/managed-settings.json force-loads three factory mods (compact-pilot, model-gate, usage-meter, from ~/.agents/mods/claude) into every claude run regardless of HOME. The same plugin list is in E1's canary (claude-B, 2026-11-21), so E1, E4, E5 and E6 claude bundles all carry it. CLAUDE_CODE_MANAGED_SETTINGS_PATH was tried and the CLI still reads /etc/claude-code. Why it is judged inert for E6 generation (code read, ~/.agents/mods/claude/*/hooks/register.ts): model-gate only gates agent spawns and adds a warning only when the cwd is a repo named in its graph (the run cwd is under 3S-study-work/e6/corpus, no such repo); compact-pilot adds resume text only on a compaction SessionStart; usage-meter only logs. The run's Tools list is Read/Edit/Write/Glob/Grep, so no spawn exists. Evidence of "inert" is by reading code and the canary text check (no leak marker, no persona token), not by an A/B without the mods. Reported to alpha; the report states it as a limitation for the claude bundles (claude-haiku, claude-sonnet) of E6.
- claude-sonnet is claude-sonnet-5-5, named by alpha for this experiment; the launcher's own default for Anthropic generators is haiku.


## 10. Addendum 2 (2026-11-27, after tier 1 was generated and built, before the tier-1 emulator pass that counts and before any tier-2 run)

- AVD changed from test34 (1080x2280@420) to **lowend34**, the low-end rig E2 used (API 34 google_apis x86_64, 720x1280@280, 2 cores, 2 GB; flags as in section 6.3, `ANDROID_AVD_HOME=3S-study-work/avd`). Reason: a 6-run smoke pass on test34 (the six tier-1 apps that had built) gave gfxinfo janky frames of 99.5% to 100% for every app, so the measure could not separate anything, and the course target is a low-cost device. This was decided after seeing those six smoke numbers, which are kept only in the work dir (3S-study-work/e6/smoke-test34/) and are not in any result file. The scripted swipes were rescaled to the screen (`input swipe 360 1000 360 350 300`, same shape).
- Added measures from `dumpsys gfxinfo <pkg> framestats` (last up to 120 frames of the scroll): median and p90 total frame time in ms (FrameCompleted minus IntendedVsync, flags 0 only), columns frame_ms_med and frame_ms_p90, plus the gfxinfo histogram percentiles p50/p90/p99 (50 ms buckets above 150 ms). janky% is still recorded and still used first in the contact-sheet rule; ties go to the lower median frame ms, then the lower rep number. If janky% saturates again on lowend34, the report says so and reads frame ms instead; that is stated here, not decided later.


## 11. Addendum 3 (2026-11-27, before the final emulator pass)

A first pass on lowend34 over the six tier-1 apps was discarded: its frame_ms_med values were wrong because the framestats parser used fixed column positions and API 34 adds a FrameTimelineVsyncId column. The parser now reads the header line. Every number in results/runs.csv comes from one final emulator pass (one session, all 15 built apps, each installed fresh), after all 36 generations and builds had finished. The discarded first-pass files are in 3S-study-work/e6/smoke-lowend34-firstpass/ and are in no result file.

