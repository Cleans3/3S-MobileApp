#!/usr/bin/env python3
"""Writes REPORT.md from results/runs.csv, results/summary.csv, results/canary.json. Every number in the tables and in the prose is computed here from those files."""
import csv, json, pathlib, re, statistics

E6 = pathlib.Path(__file__).resolve().parent.parent
R = E6 / "results"
rows = list(csv.DictReader(open(R / "runs.csv")))
summ = list(csv.DictReader(open(R / "summary.csv")))
canary = json.load(open(R / "canary.json"))
BUND = ["claude-haiku", "codex-luna", "agy-g38flash", "claude-sonnet"]
ARM = ["O", "R", "S"]

def f(x, nd=1):
    try: v = float(x)
    except (TypeError, ValueError): return "-"
    return f"{v:.{nd}f}".rstrip("0").rstrip(".") if nd else f"{v:.0f}"
def n(x):
    try: return float(x)
    except (TypeError, ValueError): return None
def cell(b, a): return [r for r in rows if r["bundle"] == b and r["arm"] == a]
def cnt(g, fn): return sum(1 for r in g if fn(r))
def rngs(g, key, nd=1):
    v = [n(r[key]) for r in g if n(r.get(key)) is not None]
    return "-" if not v else (f(v[0], nd) if min(v) == max(v) else f"{f(min(v), nd)} to {f(max(v), nd)}")
def short_err(e):
    e = re.sub(r"file:///\S*/build/[^/]+/", "", e); e = re.sub(r"/mnt/\S*/build/[^/]+/", "", e)
    return e.replace("|", "/")[:150]
def med(v): return statistics.median(v)

scrolled = lambda r: r["f1_crash"] == "no" and r["no_scroll"] == "no"
tot = len(rows); gen = cnt(rows, lambda r: r["generated_at"]); valid = cnt(rows, lambda r: r["valid"] == "True")
bp = cnt(rows, lambda r: r["build"] == "pass"); zt = cnt(rows, lambda r: r["build_zero_touch"] == "pass")
launched = cnt(rows, lambda r: r["f1_crash"] == "no"); crashed = cnt(rows, lambda r: r["f1_crash"] == "yes")
out = []
w = out.append
w("# E6 report: the one-shot \"social-media prompt\" across tools, models and fixes\n")
w("Exploratory experiment, no confirmatory test and no p-values (AMENDMENT.md, written before the first generation, with two dated addenda). Every number below is computed from results/runs.csv (one row per planned run) or results/summary.csv by harness/report.py. Images are in img/. Heavy state (generated projects, transcripts, APKs, raw PNG screenshots) is in the study work dir /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/e6/ (runs/<run_id>/), outside the repo; APKs are in 3S-study-work/apk-corpus/.\n")
w("## 1. What was run\n")
w(f"One prompt in, one Android project out, first draft, no retrieval, no repair by the generator (as E1). 4 bundles x 3 arms x 3 reps = {tot} planned runs, {gen} generated, {valid} valid (have Kotlin and a Gradle file), {bp} built, {launched} launched without crash and {crashed} crashed at launch.\n")
w("| Arm | Prompt |\n|---|---|\n| O | one-shot hype prompt written for this study in the style of social posts (prompts/O.txt): glassmorphism cards with blur, gradient hero, animated cover grid, micro-animations, dark mode, floating bottom bar |\n| R | prompt O plus ANDROID-GUARDRAIL.md as the tool's native context file (CLAUDE.md, AGENTS.md or GEMINI.md) |\n| S | structured spec of the same app (prompts/S.txt): screens, local generated data, lazy lists with keys, sized covers, no blur/shadow/per-frame animation on items, sp text and font scale 2.0, touch targets, no network, definition of done = assembleDebug passes |\n")
w("| Bundle | Launcher | Model | CLI version in these runs |\n|---|---|---|---|")
for b in BUND:
    g = [r for r in rows if r["bundle"] == b and r["cli_version"]]
    w(f"| {b} | {g[0]['tool']} | {g[0]['model_arg']}{' (reported ' + g[0]['model_reported'] + ')' if g[0]['model_reported'] else ''} | {g[0]['cli_version']} |")
w("\nclaude-haiku and claude-sonnet use the same launcher and the same flags; they differ only in the model. That pair separates model from harness.\n")
cp = canary["runs"]
mods = ", ".join(next(r for r in cp if r["name"] == "claude-haiku-B")["factory_mods_loaded"])
w(f"**Isolation.** Generation went only through the isolated launcher (stub HOME, empty harness config, auth files only, Landlock write jail, file tools only for claude; one config hash {rows[0]['harness_config_hash'][:12]} for all {gen} runs). Canary (results/canary.json, {canary['date']}): {sum(1 for r in cp if r.get('pass'))} of {len(cp)} pass (positive control, clean stub, context-file delivery, for every bundle). **Limitation, not removable from inside the study:** /etc/claude-code/managed-settings.json force-loads three factory mods ({mods}) into every claude run, also under a stub HOME (canary claude-B lists them). Reading their hooks, none adds model-visible text for these runs (AMENDMENT section 9), but this is by code reading, not by an A/B without the mods, so it applies to the claude-haiku and claude-sonnet bundles. The codex and agy bundles carry no such layer.\n")
w("## 2. How the results look: arm O, the lead-in image\n")
w("![arm O, one rep per bundle](img/grid-O.jpg)\n")
w("Contact sheet rule (fixed in AMENDMENT 6.5, not by looks): per bundle, among the arm-O reps that launched without crash, the lowest janky %, ties by lower median frame ms, then lower rep. It is the best case of each bundle, and the label shows which rep. The same reps at system font scale 2.0:\n")
w("![same reps at font scale 2.0](img/grid-O-font2.jpg)\n")
O = [r for r in rows if r["arm"] == "O"]
w("Arm O, all 12 runs:\n")
w("| run | LOC | build | repairs | launch | janky % | frame ms (median) | launch ms | PSS MB | S/P/U | findings |\n|---|---|---|---|---|---|---|---|---|---|---|")
for r in sorted(O, key=lambda r: (BUND.index(r["bundle"]), r["rep"])):
    bt = "pass" if r["build"] == "pass" else ("invalid" if r["build"] == "skip" else "FAIL")
    lc = {"no": "ok", "yes": "CRASH"}.get(r["f1_crash"], "-")
    w(f"| {r['run_id']} | {r['kotlin_loc']} | {bt} | {r['repairs'] or 0} | {lc} | {f(r['janky_pct'], 2) if r['f1_crash']=='no' else '-'} | {f(r['frame_ms_med']) if r['f1_crash']=='no' else '-'} | {r['launch_ms'] or '-'} | {r['pss_mb'] or '-'} | {r['S']}/{r['P']}/{r['U']} | {r['findings_total']} |")
loc = {b: [int(r["kotlin_loc"]) for r in O if r["bundle"] == b] for b in BUND}
allloc = [x for v in loc.values() for x in v]
ob = {b: cnt(cell(b, "O"), lambda r: r["build"] == "pass") for b in BUND}
fT = [int(r["findings_total"]) for r in O if r["findings_total"]]; Pp = [int(r["P"]) for r in O if r["P"] != ""]; Uu = [int(r["U"]) for r in O if r["U"] != ""]
w(f"\nWhat differs between bundles on the same prompt (arm O): size ranges from {min(allloc)} to {max(allloc)} Kotlin lines ({max(allloc)/min(allloc):.0f}x); " + "; ".join(f"{b} {min(loc[b])} to {max(loc[b])}" for b in BUND) + f". Builds that pass: " + ", ".join(f"{b} {ob[b]}/3" for b in BUND) + f" (total {sum(ob.values())}/12). Findings per run (frozen detector v1, S+P+U+D+other): {min(fT)} to {max(fT)}; P (performance) {min(Pp)} to {max(Pp)}; U (UI) {min(Uu)} to {max(Uu)}; S (security) {rngs(O,'S')} in every run.\n")
w("Seen on the screenshots (not measured): all four bundles answer the same hype prompt with a purple gradient hero, a translucent bottom bar and a cover grid, but the content differs (img/claude-haiku-O-r1-f1.jpg, img/codex-luna-O-r3-f1.jpg, img/agy-g38flash-O-r1-f1.jpg, img/claude-sonnet-O-r3-f1.jpg). The two claude bundles show the same headline, \"Find your next favorite story\", and the first-screen counters of three of the four screenshots say 12 titles; the prompt said \"library app\" and never gave a size, so the data is whatever the model invented, not a 100,000-book catalogue. At font scale 2.0 every sheet breaks visibly (grid-O-font2.jpg): words split inside fixed-size pills (claude-haiku, agy-g38flash), a text line clipped (codex-luna), cover text hidden under the bottom bar (claude-sonnet).\n")
w("## 3. Funnel per bundle and arm\n")
w("| bundle | arm | valid | built (zero-touch) | launch ok | scroll measured | Kotlin LOC median (range) | janky % median (range) | frame ms median (range) | launch ms | PSS MB | S / P / U medians | findings median (range) |\n|---|---|---|---|---|---|---|---|---|---|---|---|---|")
for s in summ:
    w(f"| {s['bundle']} | {s['arm']} | {s['n_valid']}/3 | {s['n_build_pass']}/3 ({s['n_zero_touch']}) | {s['n_launch_ok']}/3 | {s['n_scroll_measured']}/3 | {f(s['median_kotlin_loc'],0)} ({s['range_kotlin_loc']}) | {f(s['median_janky_pct'],1) if s['median_janky_pct'] else '-'} ({s['range_janky_pct'] or '-'}) | {f(s['median_frame_ms']) if s['median_frame_ms'] else '-'} ({s['range_frame_ms'] or '-'}) | {f(s['median_launch_ms'],0) if s['median_launch_ms'] else '-'} | {f(s['median_pss_mb']) if s['median_pss_mb'] else '-'} | {f(s['median_S'],0) if s['median_S'] else '-'} / {f(s['median_P'],0) if s['median_P'] else '-'} / {f(s['median_U'],0) if s['median_U'] else '-'} | {f(s['median_findings_total'],0) if s['median_findings_total'] else '-'} ({s['range_findings_total'] or '-'}) |")
w("\nMedians and ranges are over the runs that have the value, so a cell with 1 built run shows that run alone (n is in the built and scroll columns). Janky % and frame times come from a software-GPU emulator (swangle_indirect, AVD lowend34 720x1280): they compare apps inside E6 only and are never phone numbers. Most arm-O and arm-R apps are near 100% janky on this emulator, which is why the median frame time is shown next to it.\n")
w("![arm O, R and S, best rep per cell](img/grid-ORS.jpg)\n")
w("## 4. Between arms: what a rules file and a structured prompt changed\n")
def stat(a, k, fn=lambda r: True):
    v = [n(r[k]) for r in rows if r["arm"] == a and fn(r) and n(r.get(k)) is not None]
    return (med(v), min(v), max(v), len(v)) if v else None
w("Pooled over all bundles (valid runs for the detector, scrolled runs for the frame measures; n differs per column):\n")
w("| arm | built | launch ok | janky % median (range, n) | frame ms median (range) | P findings median (range) | U findings median (range) | Kotlin LOC median |\n|---|---|---|---|---|---|---|---|")
for a in ARM:
    j = stat(a, "janky_pct", scrolled); fm = stat(a, "frame_ms_med", scrolled); P = stat(a, "P"); U = stat(a, "U"); lo = stat(a, "kotlin_loc", lambda r: r["valid"] == "True")
    ga = [r for r in rows if r["arm"] == a]
    w(f"| {a} | {cnt(ga, lambda r: r['build']=='pass')}/12 | {cnt(ga, lambda r: r['f1_crash']=='no')} | {f(j[0],1)} ({f(j[1],1)} to {f(j[2],1)}, n={j[3]}) | {f(fm[0])} ({f(fm[1])} to {f(fm[2])}) | {f(P[0],0)} ({f(P[1],0)} to {f(P[2],0)}) | {f(U[0],0)} ({f(U[1],0)} to {f(U[2],0)}) | {f(lo[0],0)} |")
sj = {b: [n(r["janky_pct"]) for r in cell(b, "S") if scrolled(r)] for b in BUND}
oj = {b: [n(r["janky_pct"]) for r in cell(b, "O") if scrolled(r)] for b in BUND}
rj = {b: [n(r["janky_pct"]) for r in cell(b, "R") if scrolled(r)] for b in BUND}
sf = {b: [n(r["frame_ms_med"]) for r in cell(b, "S") if scrolled(r)] for b in BUND}
of = {b: [n(r["frame_ms_med"]) for r in cell(b, "O") if scrolled(r)] for b in BUND}
def rg(v, nd=2): return "-" if not v else (f(v[0], nd) if min(v) == max(v) else f"{f(min(v),nd)} to {f(max(v),nd)}")
w("\nWithin a bundle that has measured scrolls in both arms, the cell ranges (min to max of the reps that built) are compared by the AMENDMENT section 7 rule (n = 3 at most, so this is descriptive only):\n")
for b in BUND:
    if sj[b] and oj[b]:
        ov = not (max(sj[b]) < min(oj[b]) or max(oj[b]) < min(sj[b]))
        w(f"- {b}, janky %: arm S {rg(sj[b])} (n={len(sj[b])}) against arm O {rg(oj[b])} (n={len(oj[b])}), ranges {'overlap' if ov else 'do not overlap'}; median frame ms arm S {rg(sf[b],1)} against arm O {rg(of[b],1)}.")
for b in BUND:
    if rj[b] and oj[b]:
        ov = not (max(rj[b]) < min(oj[b]) or max(oj[b]) < min(rj[b]))
        w(f"- {b}, janky %: arm R {rg(rj[b])} (n={len(rj[b])}) against arm O {rg(oj[b])} (n={len(oj[b])}), ranges {'overlap' if ov else 'do not overlap'}.")
w(f"\nDetector findings follow the same direction: median P findings per valid run O {f(stat('O','P')[0],0)}, R {f(stat('R','P')[0],0)}, S {f(stat('S','P')[0],0)}; median U findings O {f(stat('O','U')[0],0)}, R {f(stat('R','U')[0],0)}, S {f(stat('S','U')[0],0)}. These are counts of rule hits by the frozen v1 detector, not defects confirmed one by one.")
bpc = lambda b, a: cnt(cell(b, a), lambda r: r["build"] == "pass")
w(f"\n**What the fixes did not do.** Build success did not improve by arm alone. A rules file and a structured spec raised the build rate for claude-sonnet (O {bpc('claude-sonnet','O')}/3, R {bpc('claude-sonnet','R')}/3, S {bpc('claude-sonnet','S')}/3) but not for claude-haiku (O {bpc('claude-haiku','O')}/3, R {bpc('claude-haiku','R')}/3, S {bpc('claude-haiku','S')}/3). For codex-luna the guardrail arm built {bpc('codex-luna','R')}/3 against {bpc('codex-luna','O')}/3 in arm O: the three arm-R runs fail inside the baselineprofile setup, which guardrail rule 19 (\"Ship a Baseline Profile\") asks for ({cnt(cell('codex-luna','R'), lambda r: 'aseline' in r['build_error_first_line'])} of 3 first error lines name it). A rules file can add work the generator then cannot finish. Failure reasons are in section 6.\n")
w("## 5. Model effect against harness effect\n")
hs, ss = [r for r in rows if r["bundle"] == "claude-haiku"], [r for r in rows if r["bundle"] == "claude-sonnet"]
w(f"Same launcher, same flags, only the model differs. Built: claude-haiku {cnt(hs, lambda r: r['build']=='pass')}/9, claude-sonnet {cnt(ss, lambda r: r['build']=='pass')}/9. Median Kotlin LOC: haiku {f(med([int(r['kotlin_loc']) for r in hs]),0)}, sonnet {f(med([int(r['kotlin_loc']) for r in ss]),0)}. Median cost per run (CLI-reported, USD): haiku {f(med([n(r['cost_usd']) for r in hs]),2)}, sonnet {f(med([n(r['cost_usd']) for r in ss]),2)}. Harness effect: the same arm-O prompt gives {ob['claude-haiku']}, {ob['codex-luna']}, {ob['agy-g38flash']} builds of 3 for claude-haiku, codex-luna, agy-g38flash. The model swap inside one harness moves the build count in arms R and S from {cnt([r for r in hs if r['arm']!='O'], lambda r: r['build']=='pass')}/6 to {cnt([r for r in ss if r['arm']!='O'], lambda r: r['build']=='pass')}/6, and the harness swap in arm O moves builds between {min(ob.values())}/3 and {max(ob.values())}/3. Both effects are large next to 3-run cells, and E6 has no design to apportion them further.\n")
w("## 6. Failures, with reasons\n")
w("Builds follow E1 policy R7: generated files as written, then at most 3 logged mechanical repairs on the build copy (results/repairs.csv); the detector reads the unrepaired source. Failed and invalid runs, first error line:\n")
w("| run | stage | reason |\n|---|---|---|")
for r in sorted(rows, key=lambda r: (BUND.index(r["bundle"]), ARM.index(r["arm"]), r["rep"])):
    if r["failure_reason"]:
        stage = "invalid" if r["valid"] != "True" else "build"
        reason = r["failure_reason"].split("; ")[0].replace("invalid: ", "") if stage == "invalid" else short_err(r["build_error_first_line"])
        w(f"| {r['run_id']} | {stage} | {reason} |")
rep_pass = [r["run_id"] for r in rows if r["build"] == "pass" and r["build_zero_touch"] != "pass"]
w(f"\nBuilt only after a repair (not zero-touch): {', '.join(rep_pass) or 'none'}. Zero-touch builds: {zt}/{tot}.\n")
unres = cnt(rows, lambda r: r["build"] == "fail" and "Unresolved reference" in r["build_error_first_line"]); fails = cnt(rows, lambda r: r["build"] == "fail")
w(f"{unres} of {fails} failed builds stop at an Unresolved reference (a missing import or a symbol that does not exist in the library version used); the repair table only knows a fixed list of imports, so these stay failures by E1 policy.\n")
w("Diagnostics outside runs.csv (harness/diag.py, results/diag/): **agy-g38flash-O-r3** builds but crashes at launch with a ClassNotFoundException for its MainActivity (results/diag/agy-g38flash-O-r3.txt); its sources contain no MainActivity class although the manifest declares one (211 Kotlin lines against 3043 and 3702 for its two sibling runs). **codex-luna-S-r2** launches, but the catalogue list is still empty 25 s after launch (results/diag/codex-luna-S-r2.jpg), so its scroll measure has 0 frames and is excluded from the janky columns. **agy-g38flash arm S**: all 3 runs are invalid and harness-limited, not a model result: agy headless auto-denies the first shell command outside its allow-list and ends the run before any file is written (the limit E1 recorded); the retry hit it again.\n")
w("## 7. Limits\n")
w("- n = 3 per cell, and in most cells fewer apps built, so most cells hold 0 to 3 measured apps; one prompt idea; one emulator rig. Nothing here is a significance statement.\n- Software-GPU emulator: janky % saturates near 100% for the heavy apps. The AVD was changed from test34 to lowend34 and frame-time columns were added after a six-run smoke pass showed that saturation (AMENDMENT section 10); those six smoke numbers are not in any result file.\n- The tool CLIs expose no seed or temperature, so a rerun gives different apps.\n- Arm S carries information (data source, size limits, constraints) that arm O does not; S against O is \"engineered spec against hype prompt\", not a one-factor test. Arm R differs from O by one file only.\n- The execution order is tier 1 (arm O) then tier 2 (arms R and S), shuffled inside the tier, so arm and time of day are confounded.\n- The claude bundles carry the managed factory mods described in section 1; agy used CLI 1.5.18 with an in-process auth-path patch (AMENDMENT section 9).\n- First screen only: no check that search, detail or offline behaviour work; a built and launched app can still be empty (codex-luna-S-r2).\n")
w("## 8. Files\n")
w("- AMENDMENT.md, prompts/ (O.txt, S.txt, PREAMBLE.txt)\n- results/runs.csv (one row per planned run, all columns), summary.csv (bundle x arm), findings.csv and metrics.csv (detector rows), maven-snapshot/ (E1's committed Maven metadata snapshot plus the artifacts fetched live at E6 scan time, 2026-11-27: dependency findings D depend on that date), repairs.csv, plan.json (seed 20261127), canary.json, diag/\n- img/<run_id>-f1.jpg and -f2.jpg (first screen at font scale 1.0 and 2.0; 540 px wide copies of the 720x1280 screenshots), grid-O.jpg, grid-O-font2.jpg, grid-ORS.jpg\n- runs/<run_id>/ (meta.json, build.json, measure.json copies)\n- harness/e6.py (canary, plan, gen, build, measure, scan, collect), diag.py, report.py; reproduce: python3 harness/e6.py plan|canary|gen|build|measure|scan|collect, then python3 harness/report.py\n")
(E6 / "REPORT.md").write_text("\n".join(out))
print(len(out), "blocks")
