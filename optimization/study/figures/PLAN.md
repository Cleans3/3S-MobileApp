# figures/PLAN.md — figure plan (no images yet)

Written 2026-11-21 by beta-f2-1. Every figure below is specified before its data exists: what it shows, chart type, the exact CSV and columns it reads, how it is annotated, and what must exist before it can be built. Nothing here contains a result number. Paths are relative to `optimization/study/`. Captions and axis labels are in Vietnamese (decimal comma, thousands dot, as in ARTICLE.md); this plan is in English for the orchestrator.

## 1. Rules that apply to every figure

1. **Data in, numbers out, same file.** One script `figures/make_figures.py` (written in P3) reads the CSVs, never a hand-typed value, and writes for each figure `out/F##-name.svg`, `out/F##-name.png` and `out/F##-name.data.csv` (the exact numbers plotted: group, n, estimate, interval ends). The orchestrator's number check then compares `.data.csv` with the source CSV, and the article text with `.data.csv`. A sidecar `out/F##-name.json` records the source files, their sha256, the git commit and the script version.
2. **Filters stated once, applied everywhere.** Drop rows with `pilot=True` (E1 `runs.csv`) and with `pilot-` run_id prefixes; E1 analyses use `valid=True` only, the "all runs" sensitivity is a separate figure panel with its denominator in the title; E2 uses the confirmatory run only (the first-pass run is a pilot, protocol §15 A3); E4 uses the trials the REPORT classes as analysable. Rules that did not pass the §5 validation gate are drawn only in descriptive panels, greyed, and labelled "chưa kiểm chứng" (never in C1 to C3).
3. **Honest annotations.** Any E2 factor whose A variant is labelled "author-constructed" (amendment A4: no corpus candidate qualified) carries that label in every table and figure that shows it, and F14 (C5) is not drawn as confirmatory for P08 in that case (A4, A5: no substitute item is chosen). Confirmatory figures (C1 to C5) show the Holm-adjusted p, the pre-registered threshold (SESOI) as a reference line, and the verdict word taken from the REPORT (ủng hộ, bác bỏ, không kết luận được). Descriptive figures carry **no p-value**. A "không kết luận được" result is drawn with the same size, colour and placement as a supported one. Every group shows its n. Where realised n per style is below 36, the panel says "bị giới hạn công suất" (protocol §6.5). Intervals: Wilson 95% for proportions, percentile bootstrap (10,000 resamples, seed 20261121) otherwise, as in protocol §6.1.
4. **One visual language.** The three prompt styles keep one colour each in every figure (naive, detailed-functional, guardrailed); A and B keep one colour each; human strata have their own two. Colour-blind-safe palette, plus a marker shape or hatch so that nothing is encoded by hue alone. Bars start at zero; a log axis is always labelled. No decoration (no gradients, shadows, 3-D): the study is about the cost of exactly that. Load the `dataviz` skill before writing any chart code.
5. **Per-bundle points are always drawn.** Because bundles confound harness and model (threat 5) and an amendment may exclude or re-stratify one bundle (A6 onwards pending), every E1 and E4 figure draws bundle-level points (or small multiples by bundle) so that any exclusion is a filter on the same script, not a redesign.
6. **Tooling and file budget.** System Python has no matplotlib and no pandas, and there is no SVG-to-PNG converter on the host (checked 2026-11-21). /mnt is NTFS and has hit file-record exhaustion (alpha, 2026-11-21), so the default is a **stdlib-only SVG writer: zero new files beyond the outputs**. A venv is created only if a chart cannot be done that way or a PNG is required (slides), only then, and only at `/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/venv-fig` (matplotlib and numpy, nothing else); check free space and RAM first. Never in the repo. Outputs per figure are three small files plus one sidecar; prune `out/` of superseded drafts.
7. **Not figures.** Screenshots (ShelfTap, optlab, MobSF, Profiler) and the A/B scroll videos are assets owned by the experiment owners (E2: video; E3: MobSF captures); this plan only lists where the article and the slides expect them (section 5).

## 2. Figure list

Status of each: `waits` names what must exist first. "Conf" = confirmatory (annotate p and threshold); "Desc" = descriptive.

### 2.1 Design and method (no study data needed)

| ID | What it shows | Chart | Data | Conf/Desc | Article | Slide | Waits |
|---|---|---|---|---|---|---|---|
| F01 | study design: briefs B1 to B3, three prompt styles, E1 to E5, A/B, arms, who judges what (no self-certification) | diagram | none (protocol §4, §8) | n/a | §4.1 | S3, D1 | nothing; build first |
| F02 | design sensitivity: power of the two-proportion test against n per arm, for 0,8 v 0,4, 0,7 v 0,3, 0,9 v 0,5, at α 0,05 and 0,01; vertical lines at n = 18 (floor) and n = 36 (target); the realised n per style marked on the x axis | line chart, one panel per α | recomputed by the protocol Appendix A script (stdlib); realised n from `e1-corpus/results/runs.csv` (`prompt_style`, `valid`, `pilot`) | Desc | §4.8 | D1 | realised n only at the end |

### 2.2 E1: corpus and detector

| ID | What it shows | Chart | Data (file: columns) | Conf/Desc | Article | Slide | Waits |
|---|---|---|---|---|---|---|---|
| F03 | E1 flow: generated, valid, build zero-touch, build after repair, analysed, with web-tool-call runs (amendment A6) and compile-excluded counts, by style and by bundle | horizontal stacked counts (or flow table if clearer) | `runs.csv`: `prompt_style, bundle, brief, pilot, valid, invalid_reason, build_zero_touch, build_repaired`; `repairs.csv`: `run_id, step, kind` | Desc | §5.1 | D3 | E1 campaign finished |
| F04 | detector validation: recall per rule with the 0,80 gate line; second-rater precision per rule with the 0,80 gate; kappa as a labelled number | two dot plots sharing the rule axis, rules ordered by class | `validation/mutation.csv`: `rule_id, mutants, fired, recall, status`; second-rater files in `validation/` (columns read from `RATER-README.md` and the E1 REPORT, not assumed) | Desc | §5.2.1 | D3 | rater sheets filled |
| F05 | coverage against Lint, detekt and compose-rules: which rules only the custom detector covers | heat-strip: rule by tool, custom-only flagged | `validation/coverage_matrix.csv`: `rule_id, slop-scan_findings, lint_findings, depcheck_findings, existing_tool_declares, custom_only` | Desc | §5.2.1 | D3 | detekt run in the REPORT |
| F06 | prevalence by class and prompt style: share of valid runs with at least one finding of each validated rule, three styles side by side | small multiples by class (P, S, U, D); dot and Wilson whisker per style; rules sorted by pooled share | `findings.csv`: `run_id, rule_id, triage` joined to `runs.csv`: `prompt_style, valid, pilot, bundle`; validated rules from F04 | Desc | §5.2.2 | S5 | detector validated |
| F07 | **C1.** Share of runs with at least one S-class finding, naive v guardrailed (detailed-functional as a third, labelled secondary), with the risk difference and its interval; stratum-level (brief × bundle) points faint behind | paired dot plot, panel B: risk-difference interval against the 0,15 SESOI line and the 0,10 and 0,25 sensitivity ticks | as F06, S-class = validated S01 to S09, S11 | **Conf** | §5.2.3 | S7 | n per style known; companion panels per amendment A6 (a) without web-call runs, (b) per bundle and leave-one-bundle-out, (c) compile-excluded runs |
| F08 | **C2.** Distinct validated rule IDs per run, naive v guardrailed (detailed-functional secondary) | strip plot of all runs (jittered), mean and bootstrap interval; panel B: difference against the 2,0 SESOI line and the 1 and 3 ticks | `findings.csv` `rule_id` distinct per `run_id`, `runs.csv` | **Conf** | §5.2.3 | S7 | as F07 |
| F09 | **G3.** Guardrail effect on the instructed set against the held-out set: per-rule change in prevalence (naive to guardrailed), points grouped instructed v held-out, plus the contrast of the two mean changes with its interval | dot plot by rule, grouped; contrast interval against 0 | `findings.csv`, `runs.csv`; instructed and held-out lists from protocol §9; held-out rules that failed validation shown greyed and named as excluded (amendment A1) | Desc (pre-registered contrast) | §5.2.4 | S7 | validation result for U05 |
| F10 | **C3.** S-class findings per 1000 Kotlin lines: unguardrailed E1 runs against the pre-assistant human stratum; the 2024+ human stratum drawn distinctly as the second, non-confirmatory comparator; `android/app` as one labelled point | strip plot per group with median, Cliff's delta as text; a second panel with the per-100k-non-whitespace-characters sensitivity | `findings.csv`, `human_findings.csv`, `metrics.csv` and `human_metrics.csv`: `kotlin_loc, kotlin_nws`, `human_corpus.csv`: `stratum`, `ref_findings.csv`, `ref_metrics.csv` | **Conf** | §5.2.5 | S5 | human corpus scanned |
| F11 | decorative-effect density (H3, E1 side): per list item and per screen, by style and for human apps | strip plot, one panel per measure; share of runs with at least one decorated list item as a labelled number | `metrics.csv` and `human_metrics.csv`: `effect_density_item, effect_density_screen, effects_item, item_scopes`, joined to `runs.csv` | Desc | §5.2.2 | S5 (optional) | E1 finished |
| F12 | H1 and H4: share of runs with D01 or D02; classification of every dependency coordinate instance (resolved, hallucinated version, hallucinated artifact) by style, with the 300-instance line of the H4 refutation rule | stacked bars of coordinate classes; Wilson whiskers for the D shares | `coordinates.csv`: `run_id, kind, group, artifact, version, class, latest_stable, major_behind, gap_months` (`class` values read from the file, not assumed beyond those in protocol H4); `human_coordinates.csv`; `findings.csv` for D01 | Desc | §5.2.6 | D2 | E1 finished, maven snapshot |

### 2.3 E2: paired A/B cost

| ID | What it shows | Chart | Data (file: columns) | Conf/Desc | Article | Slide | Waits |
|---|---|---|---|---|---|---|---|
| F13 | per-factor effect: delta% of the primary metric of each factor (A against B) with its 95% interval; the ±5% and +10% lines of the H3 rule shown | forest plot, one row per factor and metric, sorted by size; the unit stays in the row label because the metrics differ | `e2-bench/results/summary.csv`: `factor_id, metric, unit, delta_pct, delta_ci95_lo, delta_ci95_hi, cliffs_delta, n` | Desc (C5 row marked) | §5.3 | S10 | confirmatory E2 run (A from E1 code) |
| F14 | **C5.** P90 of FrameDurationCpuMs for the P08 list item as generated (A) against flattened (B), iteration-level points | dot plot of iterations per variant with the P90 marked; panel B: bootstrap interval of delta% against 0 and the +10% SESOI line | `raw.csv`: `factor_id=P08, variant, metric, iteration, value`; `summary.csv` | **Conf** | §5.3 | S10 | confirmatory run; the emulator-limit note is part of the figure caption |
| F15 | absolute primary metrics A v B per factor (kept iterations in the main panel; amendment A7 adds the all-iterations sensitivity panel, and a factor reported as host-limited carries that label): recomposition counts, decoded bitmap bytes, APK or dex size, main-thread blocked time, search latency | small multiples, bars for medians with IQR whiskers and every iteration as a dot; a log axis only where A and B differ by orders of magnitude, always labelled | `raw.csv`: `factor_id, variant, metric, unit, iteration, value`; `summary.csv`: `A_median, A_iqr, B_median, B_iqr`; `build_facts.csv`: `fact, value, unit, source` | Desc | §5.3 | S4, S10 | as F13 |
| F16 | expected cost per app (exploratory): E1 prevalence of each factor's problem against the E2 cost of that factor | scatter, prevalence on x with its interval, delta% on y with its interval; every point labelled with the rule ID; captioned "khám phá" | `findings.csv` (prevalence) with `summary.csv` (cost); resampling of both as in protocol §4.2 | Desc (exploratory) | §5.3 | D2 | F06 and F13 |

### 2.4 E3, E4, E5

| ID | What it shows | Chart | Data (file: columns) | Conf/Desc | Article | Slide | Waits |
|---|---|---|---|---|---|---|---|
| F17 | E3 precision by tool and MASVS group, and what only one tool found | stacked true/false-positive counts per tool and MASVS group; an agreement matrix (detector against tool) as a second panel | `e3-security/results/findings.csv`: `target, tool, masvs, rule_id, severity, evidence, triage` | Desc | §5.4 | S6 | E3 triage done |
| F18 | **C4.** Outcome mix per error case (secure-fix, insecure-workaround, perf-regression, no-fix), with trial counts; the Stack Overflow baseline beside it | 100% stacked bars by case, one extra bar per case for the human baseline; panel B: pooled share of insecure-workaround with Wilson and cluster-bootstrap interval against the 0,10 null and the 0,15 SESOI | `e4-pressure/results/trials.csv` (contract columns: `trial_id, error_case, tool, model, rep, outcome, rule_id_introduced, evidence`) with `adjudication.csv` for the final label (its columns read from the E4 REPORT, not assumed); `so_census.csv`: `error_case, label, rule_id` | **Conf** | §5.5 | S8 | E4 adjudicated; decision on the web-tool trials (pending amendment) |
| F19 | E4 by bundle and by prompt variant (error only v error and project) | dot plot of the insecure-workaround share with Wilson whiskers, bundle-level points always drawn | `trials.csv`: `tool, model, variant` (variant column name checked in the file), outcome | Desc | §5.5 | D2 | as F18 |
| F20 | E5: precision and recall against the ground-truth list, regressions introduced and human interventions, by evidence arm and control level | grid of dot plots (rows: control level; columns: evidence or not); regressions as a second small multiple | `e5-optimizer/ground_truth.csv`: `issue_id, tier, file, what` (the denominator); result CSV names and columns **read from the E5 REPORT when it lands, not assumed** | Desc | §5.6 | S9 | E5 REPORT |

### 2.5 Synthesis

| ID | What it shows | Chart | Data | Conf/Desc | Article | Slide | Waits |
|---|---|---|---|---|---|---|---|
| F21 | verdict board: H1 to H6, G1 to G3, B1 with the verdict word, effect size and interval, and whether the test is confirmatory | table-figure (one row per hypothesis, interval drawn inline on a common effect-size axis where the units allow, text otherwise) | the `.data.csv` of F06 to F20 | mixed | §5.7 | S11 | everything above |

## 3. Build order

1. Now, no data needed: F01, F02 (without realised n).
2. As each owner's REPORT lands: E2 (F13 to F15), then E1 (F03 to F12, in that order, since F06 to F10 need the validated rule list from F04), then E4 (F18, F19), E3 (F17), E5 (F20), then F16 and F21.
3. Slide-critical figures first: F07, F08, F09 (S7), F10 (S5), F13 to F15 (S4, S10), F18 (S8), F17 (S6), F20 (S9).

## 4. Open dependencies (do not assume)

| Dependency | Owner | Effect on the plan |
|---|---|---|
| amendment A6 (landed: E1 capability matrix, web-tool audit, repair table, sensitivity analyses a to c) | beta-f2-2 via alpha | F07, F08, F10 get the three companion panels named in their Waits cell; F03 shows web-call and compile-excluded counts; run-level columns web_tool_calls, web_tools, web_denied, generated_at are named by A6 but were not in the runs.csv header when written, so read them from the file at build time |
| pending amendment (web-tool contamination for agy in E4, maybe E1; numbered on arrival) | beta-f2-4 and beta-f2-2 via alpha | F18, F19 and possibly F07 to F10 need a "with and without the affected bundle" panel; E4 already has separate `websearch_trials_meta.csv` and `websearch_auto_labels.csv` on disk, whose treatment the amendment must decide |
| E4 final files | beta-f2-4 | `trials.csv`, `adjudication.csv`, kappa in the REPORT (per alpha); column names read at build time |
| E5 result files | beta-f2-4 | unknown until the REPORT; F20 is a placeholder until then |
| E3 results | beta-f2-4 | `e3-security/results/` is empty today |
| realised n per style | beta-f2-2 | decides whether F07 and F08 carry the "bị giới hạn công suất" label |

## 5. Assets the article and slides expect (not produced here)

| Asset | Used in | Owner | Where it is named |
|---|---|---|---|
| ShelfTap and optlab screenshots (Catalogue, Search, Detail) | ARTICLE §5.1 (TODO[app]), S1 | beta-f2-3 | `e2-bench/REPORT.md` |
| A and B scroll video, same script, side by side if the owner can | S4, S10 (TODO[e2-video]) | beta-f2-3 | `e2-bench/REPORT.md` |
| MobSF capture and one code-before/after snippet | S6 | beta-f2-4 | `e3-security/REPORT.md` |
| Profiler capture for A and for B | S4, S10 | beta-f2-3 | `e2-bench/REPORT.md` |
| one prompt and one diff of a key optimiser change | S9 | beta-f2-4 | `e5-optimizer/REPORT.md` |
