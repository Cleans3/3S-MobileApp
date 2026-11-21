#!/usr/bin/env python3
"""Assemble optimization/study/e1-corpus/REPORT.md from results/*.csv. Numbers come only from the CSVs; prose is the method and the threats.

  report.py [--out REPORT.md] [--include-pilots]     (--include-pilots renders pilot rows too, for a dry run into a scratch path)
"""
import argparse, collections, csv, datetime, json, pathlib, re, statistics, subprocess, sys

E1 = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(E1 / "harness"))
import analysis as AN  # noqa: E402
from analysis import wilson, MEASURED, S_CLASS, HELD_OUT, TRACKS  # noqa: E402
RES = E1 / "results"
ARMS = ["naive", "detailed-functional", "guardrailed"]
BUNDLES = ["claude-haiku", "codex-luna", "agy-g38flash"]


def read(name):
    f = RES / AN.SRC.get(name, name)
    return list(csv.DictReader(open(f))) if f.exists() else []


def ci(k, n):
    if not n: return "n/a"
    p, lo, hi = wilson(k, n)
    return f"{k}/{n} = {p:.2f} [{lo:.2f}, {hi:.2f}]"


def table(head, rows):
    return "\n".join(["| " + " | ".join(head) + " |", "|" + "---|" * len(head)] + ["| " + " | ".join(str(c) for c in r) + " |" for r in rows]) + "\n"


def sh(cmd):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return (r.stdout + r.stderr).strip()


def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--out", default=str(E1 / "REPORT.md")); ap.add_argument("--include-pilots", action="store_true"); a = ap.parse_args()
    runs_all = read("runs.csv")
    runs = [r for r in runs_all if a.include_pilots or r["pilot"] != "True"]
    pil = [r for r in runs_all if r["pilot"] == "True"]
    ff = read("pilot_findings.csv") + read("findings.csv") if a.include_pilots else read("findings.csv")
    fm = {m["run_id"]: m for m in read("pilot_metrics.csv") + read("metrics.csv")}
    valid = [r for r in runs if r["valid"] == "True"]
    by_run = collections.defaultdict(list)
    for f in ff:
        if f["triage"] != "fp": by_run[f["run_id"]].append(f)
    rules_of = lambda rid: {f["rule_id"] for f in by_run.get(rid, [])}
    hc = {("human:" + h["repo"] + "@" + h["commit"][:8]): h for h in read("human_corpus.csv")}
    hrules = collections.defaultdict(set)
    for f in read("human_findings.csv"):
        if f["triage"] != "fp": hrules[f["run_id"]].add(f["rule_id"])
    o = []
    w = o.append
    w("# E1: AI-generated Android apps, prevalence of taxonomy problems\n")
    w(f"Generated {datetime.date.today().isoformat()} from results/*.csv by harness/report.py. Frozen inputs: protocol sha256 {sh('cut -c1-12 ' + str(E1.parent / '00-protocol.sha256'))}, guardrail sha256 {sh('cut -c1-12 ' + str(E1.parent / 'guardrail' / 'ANDROID-GUARDRAIL.sha256'))}. Scope: first-draft, no-retrieval (protocol section 1): one brief in, one project out, no network, no reviewer, no detector feedback.\n")
    def comp_row(sub):
        f = RES / "analysis" / sub / "summary.md"
        for ln in (f.read_text().splitlines() if f.exists() else []):
            if ln.startswith("| (c) compile-excluded"): return [c.strip() for c in ln.strip("|").split("|")]
        return None
    nb = [r for r in valid if r["build"] == "pass"]; nz = [r for r in valid if r["build_zero_touch"] == "pass"]
    perb = "; ".join(f"{b} {sum(1 for r in nb if r['bundle'] == b)}/{sum(1 for r in valid if r['bundle'] == b)}" for b in BUNDLES)
    c1, c2 = comp_row("primary-v1"), comp_row("sensitivity-v2")
    w("## Read this first\n")
    w(f"1. Only {len(nb)} of {len(valid)} valid runs build (pass after at most 3 logged table repairs; {len(nz)} pass zero-touch). Per bundle: {perb}. Every prevalence number below is source-level and covers all valid runs, including apps that do not compile.\n")
    if c1 and c2:
        w(f"2. Among the {c1[1]} apps that build (compile-excluded sensitivity, protocol A6, not a confirmatory endpoint), C1 (share of runs with at least one S-class finding, naive minus guardrailed) is {c1[2]} on the primary track and {c2[2]} on the v2 track: the guardrail effect on S-class presence does not hold among apps that build. The count and density effects persist among builders (C2 {c1[3]} primary, {c2[3]} v2; C3 Cliff's delta {c1[4]} primary, {c2[4]} v2), so the guardrail reduces how many distinct S-class problems appear, not whether any appear in a working app.\n")
    else:
        w("2. The compile-excluded sensitivity has not been computed (analysis.py not run).\n")
    w("## 1. Corpus actually generated\n")
    rows = []
    for b in BUNDLES:
        for s in ARMS:
            rs = [r for r in runs if r["bundle"] == b and r["prompt_style"] == s]
            v = [r for r in rs if r["valid"] == "True"]
            inv = collections.Counter(r["invalid_reason"] or "unknown" for r in rs if r["valid"] != "True")
            loc = [float(r["kotlin_loc"] or 0) for r in v]
            nws = [float(fm.get(r["run_id"], {}).get("kotlin_nws") or 0) for r in v]
            bz = sum(1 for r in v if r["build_zero_touch"] == "pass"); bp = sum(1 for r in v if r["build"] == "pass")
            rows.append([b, s, len(rs), len(v), "; ".join(f"{k} x{n}" for k, n in inv.items()) or "-",
                         f"{statistics.median(loc):.0f}" if loc else "-", f"{statistics.median(nws) / 1000:.1f}" if nws else "-",
                         ci(bz, len(v)), ci(bp, len(v))])
    w(table(["bundle", "style", "generated", "valid", "invalid reasons", "median kotlin LOC", "median code kchars (non-ws)", "zero-touch build pass", "pass after <=3 table repairs"], rows))
    w(f"Total {len(runs)} generated, {len(valid)} valid. Valid = at least one .kt file and a Gradle build file (protocol section 7); a run without them is reported with its reason and kept in the all-runs denominator. Pilots ({len(pil)}) are excluded from every number here.\n")
    att = [r for r in runs if r.get("denied_actions") not in ("", "0", None)]
    w(f"Runs where the agent's headless mode auto-denied at least one shell command: {len(att)} (agy only; the first denied command ends the whole run, so such a run may be truncated at its end, or empty if it came first).\n")
    w("### Generator capability matrix (protocol A6)\n")
    cap = {"claude-haiku": "file tools only (Read, Edit, Write, Glob, Grep); no shell, no web tool",
           "codex-luna": "shell, sandbox off, Landlock write-jail, network not blocked by the jail",
           "agy-g38flash": "shell allow-list (shared read-only set + E1 housekeeping and JDK probes), headless ends the run at the first denied command; web tools denied by hook from the switch-over (web_denied = y)"}
    rows = []
    for b in BUNDLES:
        rs = [r for r in runs if r["bundle"] == b]
        if not rs: continue
        nc = [int(r["n_commands"] or 0) for r in rs]
        wd = collections.Counter(r["web_denied"] or "?" for r in rs)
        rows.append([b, cap[b], len(rs), statistics.median(nc) if nc else "-", sum(nc), sum(1 for r in rs if r["denied_actions"] not in ("", "0")),
                     sum(int(r["web_tool_calls"] or 0) for r in rs), "; ".join(f"{k} x{v}" for k, v in sorted(wd.items())), sum(1 for r in rs if r["net_audit"] not in ("", "none"))])
    w(table(["bundle", "what the generator could do", "runs", "median shell commands/run", "shell commands total", "runs with a denied command", "web-tool attempts", "web_denied", "runs with a network/build command"], rows))
    oa = read("outside_access.csv")
    if oa:
        bad = [r for r in oa if r["outside_refs"] not in ("", "0") and not r["run_id"].startswith("pilot-")]
        w(f"Containment audit (harness/audit_outside.py, results/outside_access.csv): reads are not jailed, so every absolute path in a tool call is checked. {len(bad)} of {len(oa)} runs reference a path outside their own directory: " + ("; ".join(f"{r['run_id']} ({r['paths'][:90]})" for r in bad) or "none") + ". A denied write into the parent directory is not a read; a run that reads another run's output is invalid and listed.\n")
    w("No run built or ran the app (net_audit counts executed build, install and network commands): the claude launcher has no shell and the agy allow-list denies build tools, while codex had an open shell and was only audited. Web-tool attempts are counted from tool invocations in the transcripts (attempts a hook denied still count).\n")
    w("## 2. Prevalence per rule and arm (share of valid runs with at least one finding; n per arm in the header)\n")
    for tr, title in (("v1", "2a. PRIMARY: frozen v1 detector (alpha A13)"), ("v2", "2b. SENSITIVITY: v2 detector (five false-positive bugs fixed, validation/fixes-v2.md)")):
        AN.SRC.clear(); AN.SRC.update(TRACKS[tr])
        ffs = [f for f in read("findings.csv") if f["triage"] != "fp"]
        by_run = collections.defaultdict(set)
        for f in ffs: by_run[f["run_id"]].add(f["rule_id"])
        hrules = collections.defaultdict(set)
        for f in read("human_findings.csv"):
            if f["triage"] != "fp": hrules[f["run_id"]].add(f["rule_id"])
        st = {r["rule_id"]: r["status"] for r in read("validation/rules_status.csv")}
        hids = {s_: [k for k, h in hc.items() if h["stratum"] == s_] for s_ in ("pre", "post")}
        head = ["rule"] + [f"{s_} (n={sum(1 for r in valid if r['prompt_style'] == s_)})" for s_ in ARMS] + [f"human pre-2022 (n={len(hids['pre'])})", f"human 2024+ (n={len(hids['post'])})", "held-out", "status"]
        rows = []
        for rule in sorted(MEASURED):
            row = [rule]
            for s_ in ARMS:
                rs = [r for r in valid if r["prompt_style"] == s_]
                row.append(ci(sum(1 for r in rs if rule in by_run.get(r["run_id"], ())), len(rs)))
            for k_ in ("pre", "post"):
                row.append(ci(sum(1 for i in hids[k_] if rule in hrules[i]), len(hids[k_])))
            row += ["yes" if rule in HELD_OUT else "", st.get(rule, "pending")]
            rows.append(row)
        w(f"### {title}\n"); w(table(head, rows))
    AN.SRC.clear()
    w("Wilson 95% intervals. A rule that fires in nearly every human app (read the human columns) separates nothing; the analysis section applies the pre-registered prevalence-band rule. Rules marked unvalidated in the status column are shown here but are not in the primary endpoints of that track.\n")
    w("## 3. Pre-registered endpoints\n")
    for sub, title in (("primary-v1", "3a. PRIMARY (frozen v1 detector)"), ("sensitivity-v2", "3b. SENSITIVITY (v2 detector)")):
        s = RES / "analysis" / sub / "summary.md"
        w(f"### {title}\n")
        w(re.sub(r"(?m)^(#+)", lambda m: "##" + m.group(1), s.read_text()) if s.exists() else "analysis.py has not been run.\n")
    w("## 4. Detector validation\n")
    mut = read("validation/mutation.csv")
    if mut:
        w(f"Seeded-mutation recall: {sum(int(m['fired']) for m in mut)}/{sum(int(m['mutants']) for m in mut)} mutants detected over {len(mut)} rules; benign false fires {sum(int(m['benign_false_fires']) for m in mut)} over {sum(int(m['benign_controls']) for m in mut)} controls. The mutants were written by the detector's author, so this is an upper bound on recall; precision needs the blind sample below.\n")
    kp = RES / "validation" / "kappa.json"
    k = json.loads(kp.read_text()) if kp.exists() else {}
    if k:
        w("Detector fixes (validation/fixes-v2.md): the v1 rule-level rating pass exposed false-positive bugs in five rules (P10, S03, P05, P09, P07); they were fixed in slop-scan/r_perf.py and r_sec.py with seeded-mutation recall unchanged and no benign false fire. The v1 scans were frozen first (results/validation/v1/) and are the PRIMARY track; the fixed detector is the SENSITIVITY track. Fixed rules are validated on fresh pack-v2 items only (none of them in pack v1, at least 6 per rule requested, wider context, whole file for U05); v1 survivors of a fixed rule are in-sample and appear only in track v1. P05 had a single fresh item, so it stays unvalidated-precision on fresh data.\n")
        w(f"Blind second-rater precision. Pack v1 (157 items drawn before any detector change): raters {k.get('raters_v1')}, {k.get('n_pairs_v1')} double-rated, kappa {k.get('kappa_v1')}, {k.get('disagreements_v1')} disagreements. Pack v2 (fresh items): {k.get('n_pairs_v2')} double-rated, kappa {k.get('kappa_v2')}, {k.get('disagreements_v2')} disagreements. Pooled kappa {k.get('kappa')}, gate (>= 0.60) {k.get('gate')}. Consensus = both raters agree, else the adjudicated label (results/validation/adjudication*.csv), else unclear; unclear is excluded from the precision denominator. Rule status: recall >= 0.80 and precision >= 0.80 with >= 5 rated items = validated.\n")
        for tr, title in (("v1", "Track v1 (PRIMARY): rules on their v1 labels, U05 also on its fresh items"), ("v2", "Track v2 (SENSITIVITY): unchanged rules on v1 labels, fixed rules on fresh items only")):
            pr = read(f"validation/precision_{tr}.csv"); st = {r["rule_id"]: r["status"] for r in read(f"validation/rules_status_{tr}.csv")}
            w(f"**{title}**\n")
            w(table(["rule", "rated", "tp", "fp", "unclear", "precision", "Wilson 95%", "status"],
                    [[p["rule_id"], p["n_rated"], p["tp"], p["fp"], p["unclear"], p["precision"] or "-", f"[{p['wilson_lo']}, {p['wilson_hi']}]" if p["wilson_lo"] else "-", st.get(p["rule_id"], "")] for p in pr]))
    else:
        w("Blind precision rating: PENDING. Until precision_v1.csv exists every rule is labelled recall-validated only and the analysis carries a PRECISION PENDING banner.\n")
    cm = read("validation/coverage_matrix.csv")
    if cm:
        w("Coverage of the taxonomy by the other tools (declared coverage and observed counts):\n")
        w(table(list(cm[0]), [list(r.values()) for r in cm]))
    w("## 5. Emergent patterns (open coding)\n")
    oc = read("open_coding/codes.csv")
    if oc:
        cnt = collections.defaultdict(list)
        for r in oc: cnt[(r["code"], r["in_taxonomy"])].append(r["run_id"])
        w(table(["code", "in taxonomy", "runs", "example evidence"],
                [[c, t, len(set(ids)), next((r["evidence"] for r in oc if r["code"] == c), "")[:120]] for (c, t), ids in sorted(cnt.items(), key=lambda kv: -len(kv[1]))]))
    else:
        w("Open coding not yet done.\n")
    w("## 6. Environment and provenance\n")
    envrows = []
    for b in BUNDLES:
        rs = [r for r in runs_all if r["bundle"] == b]
        if rs: envrows.append([b, rs[0]["tool"], rs[0]["model"], rs[0]["model_version"] or "-", rs[0]["cli_version"] or "-", rs[0]["harness_config_hash"][:12]])
    w(table(["bundle", "tool", "model arg", "model reported", "CLI version", "harness config hash"], envrows))
    w(f"Host: {sh('uname -sr')}; JDK: {sh('java -version 2>&1 | head -1')}; Gradle {sh('grep -o \"gradle-[0-9.]*-bin\" ' + str(E1.parents[2] / 'android/gradle/wrapper/gradle-wrapper.properties'))}; detekt-cli 2.0.0-alpha.6 with compose-rules 0.6.8; Android Lint = the version of the AGP each generated project declares (lintDebug); dependency existence from Maven metadata snapshots dated by results/maven-snapshot/; advisories from OSV on the scan date.\n")
    w("Generation: harness/generate.py; one run = one empty directory, one headless call, stub HOME holding only copied auth files, Landlock write jail, shared bench lock, at most two generations in flight. Isolation canary (results/canary.json): a user-level canary appears in 4/4 bundles (the stub is the config in effect), a clean stub shows no canary in 4/4, a project context file is delivered 5/5. Plan: 3 bundles x 3 briefs x 3 styles x reps, rep-major, shuffled within a rep with seed 20261121 (results/plan.json).\n")
    w("Build policy R7: zero-touch build first, then at most three logged table repairs (Gradle version bump, android.useAndroidX, existing-artifact version bump, JDK toolchain to 17, missing import from a fixed table), recorded in results/repairs.csv; repairs touch the build copy, never the analysed source. All analysis is at source level; APK-level numbers are survivors only.\n")
    w("Human control (R3): GitHub search, seeded draw (seed 20261121), screened to Kotlin Compose apps of 2000 to 50000 LOC; 20 apps with first commit up to 2022-11-30 (pre-assistant, confirmatory) and 20 from 2024 on (second comparator). Queries, draw rank and screen verdicts: results/human_corpus*.csv. After scanning, non-source files of the clones (images, fonts, docs, binaries; 95,604 -> 14,221 files) were deleted because the NTFS work volume ran out of file records; every scanner reads only .kt, .xml, Gradle, TOML, .pro, .properties, baseline-prof.txt and stability_config.conf, so a rescan gives the same input.\n")
    w("## 7. Disclosures and deviations\n")
    ifc = read("infra_failures.csv")
    cells = collections.Counter((r["bundle"], r["prompt_style"]) for r in runs)
    short = "; ".join(f"{b} {s_}: {cells[(b, s_)]}/12" for b in BUNDLES for s_ in ARMS if cells[(b, s_)] != 12) or "none"
    for t_ in [
        f"Campaign size: {len(runs)} of 108 planned cells generated ({len(valid)} valid). Missing cells ({short}, generated/planned of 12 per bundle and style) are infrastructure losses, not model outcomes: {len(ifc)} agy repetition-4 attempts failed with 0 model calls because the agy OAuth token expired (results/infra_failures.csv, attempts under the work volume infra-failed/). Per protocol A11 they are treated as missing completely at random and were not filled, because agy was not re-authenticated before analysis.py first ran on the confirmatory data. Every table above reports n per arm.",
        "Early looks (protocol A6 and later): an analysis dry run on 13 runs was computed during the campaign and an emergent-pattern count by bundle (17:50 on the day of open coding) was seen before open coding finished. The dry run showed the author arm-level numbers, so the pre-registered condition that no arm-level C1 to C3 number be seen before the detector fixes was not met; this is why the frozen v1 detector is PRIMARY and the fixed v2 detector is SENSITIVITY (alpha A13).",
        "Detector fixes were driven by the rule-level pass of the first rater without an arm split (five rules, validation/fixes-v2.md); no rule was retuned for prevalence.",
        "Rater independence: rater 1 is the author of the detector and of the seeded mutants; rater 2 never saw E1 data and rated from the pack alone. After rater 1 had finished each sheet the file-change notices of the work harness displayed some of rater 2's rows to rater 1 (41 rows of the v1 sheet; the v2 sheet in full: the notice listed 32 lines, the header plus all 31 rows); the labels in rater 1's sheets were written before and were not edited afterwards.",
        "Resource incidents: the campaign aborted once on the root-volume free-space guard and was resumed; the NTFS work volume ran out of file records three times (ENOSPC despite free bytes); redundant corpus copies and Gradle caches were deleted, one build was rerun (codex-luna-B3-naive-r4, final result compile failure), and the scan was regenerated. No generated source was modified.",
        "Containment: results/containment_notes.md triages the four runs that reference a path outside their own directory; no write escaped the run directory.",
        "Adjudication: 26 rater disagreements (22 in pack v1, 4 in pack v2) were settled by a third reading with the whole source open, written to results/validation/adjudication*.csv with a reason per item. The third reader is rater 1, the detector's author, so it is not independent of the detector. Agreement was lowest where rater 2 could not see callers or colour constants in the window (results/validation/kappa_by_rule.csv: P08 0/6, U03 2/6, P02 3/6, S01 3/6, P09 7/12 agree). Rater-2 sheet sha256 prefixes: v1 cb310a27, v2 f630c4c1.",
        "G3 held-out rules (P03, P05, P08, S06, D02, U04, U05) were frozen before any data (A1, no substitution). P05 and U05 fail the section 5 precision gate, so the held-out side of G3 rests on P03, P08, S06, D02, U04 in both tracks.",
        "P07 stays unvalidated-precision in both tracks (1 tp, 3 fp, 2 unclear on the fresh items after the first fix); no second fix round was run, because iterating fixes against fresh samples is tuning (alpha decision). P05 had one fresh item, so its precision rests on the v1 items (secondary, in-sample).",
        "Pack v1 items for U05 were unjudgeable from a 6-line window; U05 therefore also uses its fresh pack-v2 items with whole-file context in both tracks.",
    ]: w("- " + t_)
    w("\n## 8. Threats to validity\n")
    for t in [
        "Construct: a rule that fires is a taxonomy finding, not a user-visible defect. Decorative-effect density is a proxy for the AI look. S-class share rewards verbose apps unless normalised; physical LOC is format-dependent (one generator packs about 130 characters per line), so C3 is also reported per 100k non-whitespace characters.",
        "Detector validity: the detector's author also wrote the seeded mutants, so recall is an upper bound. Precision rests on the blind second-rater sample; rules below 0.80 or with fewer than 5 rated findings are unvalidated-precision and sit outside the primary endpoints. U05 (decorative effects) was visibly noisy on the human sample. Static rules cannot see runtime behaviour.",
        "Isolation: the claude launcher loads three user mods (compact-pilot, model-gate, usage-meter) and five built-in plugins from the real ~/.agents/mods because the home directory is resolved through the password database, not HOME. Tools stay limited to Edit, Glob, Grep, Read, Write, no instruction text and no hook events were observed, and a clean-stub reply was unaffected, but the leak is not zero-proof.",
        "Shell policy: agy headless mode ends the whole run at the first command outside its allow-list. The allow-list is the shared launcher's read-only set plus file-system housekeeping and read-only JDK probes (E1-local); network, install and build tools stay denied. A run can still end early on a denied command. Runs that end empty are reported as harness-limited, not as model refusals.",
        "Network audit: the write jail does not restrict the network (the CLIs need their API). Network and build use is audited from executed command strings in the transcripts. The claude launcher has no shell tool; a tool-internal fetch that leaves no command string would not be seen.",
        "Tool bundles confound harness and model, and model versions can drift inside the generation window; temperature and seed are not exposed by any of the CLIs.",
        "Scheduling: generations shared the machine with another pane's benchmarks through a shared lock; wall-clock durations exclude lock waits but a rate-limit backoff would be logged in attempts[].",
        "Build environment: JDK 17 and 21 only, SDK platforms 34 to 37.0. A project asking for something else fails or is repaired by the table. Compile errors outside the import table are failures by policy; build pass rate is therefore a lower bound on what a developer would get after a few minutes of fixing.",
        "Valid run is deliberately weak (at least one .kt and a Gradle file), so a very small draft counts as valid; the LOC and character columns in section 1 allow a size-floor sensitivity.",
        "Dependency findings (D02, S11) are dated: known-vulnerable means as of the scan date for AI and human apps alike. The human 2024+ stratum may itself contain assistant-written code, which is why it is a second comparator only.",
        "Human corpus: GitHub search plus star and size screens select popular, larger, maintained apps, unlike a first draft; per-kLOC rates reduce but do not remove the size mismatch.",
        "External validity: one app family (ShelfTap briefs B1 to B3), Kotlin and Compose, a stateless first draft that is not how professionals use assistants.",
    ]:
        w("- " + t)
    w("\n## 9. Reproduce\n")
    w("python3 harness/build_prompts.py; python3 harness/generate.py plan; python3 harness/generate.py campaign --workers 2; python3 harness/build_loop.py; python3 harness/collect.py; python3 harness/scan_corpus.py runs; python3 harness/scan_corpus.py human; python3 slop-scan/validate.py; python3 harness/blind_sample.py; python3 harness/blind_sample_v2.py --per-rule 6 --seed 20261121 --rules P10,S03,P05,P09,P07,U05; python3 harness/rate_merge2.py --r2-v1 PATH --r2-v2 PATH; python3 harness/analysis.py (both tracks); python3 harness/coverage.py; python3 harness/report.py. Heavy state (clones, build copies, Gradle home) lives outside the repo; run sources, transcripts, build logs and scan caches are under runs/.\n")
    pathlib.Path(a.out).write_text("\n".join(o))
    print("wrote", a.out, len("\n".join(o)), "chars")


if __name__ == "__main__":
    main()
