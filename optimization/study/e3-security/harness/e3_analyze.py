#!/usr/bin/env python3
"""E3 analysis (descriptive, protocol 4.3). Reads results/findings.csv, corpus_survivors.csv and e1-corpus/results/findings.csv; writes
results/{by_area,by_target,by_bundle,agreement,sensitivity}.csv and results/stats_e3.md. Stdlib only."""
import csv, math, pathlib, re, collections
R = pathlib.Path(__file__).resolve().parent.parent / "results"
E1 = R.parent.parent / "e1-corpus" / "results" / "findings.csv"
rd = lambda p: list(csv.DictReader(open(p)))
F = rd(R / "findings.csv"); SV = rd(R / "corpus_survivors.csv"); E1F = rd(E1)
SURV = {r["run_id"]: r for r in SV}
REL = {"optlab-release": "release-type (study app)", "optlab-noR8": "release-type (study app)", "shelftap-product-release": "release-type (product)"}
def kind(t): return REL.get(t, "debug-type (corpus survivor)")
def wilson(k, n, z=1.96):
    if n == 0: return ("", "")
    p = k / n; d = 1 + z * z / n; c = (p + z * z / (2 * n)) / d; h = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return (round(max(0, c - h), 3), round(min(1, c + h), 3))
def prec(tp, fp): return round(tp / (tp + fp), 3) if tp + fp else ""
def out(name, hdr, rows):
    with open(R / name, "w", newline="") as fh:
        w = csv.writer(fh, lineterminator="\n"); w.writerow(hdr); w.writerows(rows)

# --- by tool x MASVS area
c = collections.defaultdict(collections.Counter)
for r in F: c[(r["tool"], r["masvs"] or "none")][r["triage"]] += 1
rows = []
for (tool, area), v in sorted(c.items()):
    lo, hi = wilson(v["tp"], v["tp"] + v["fp"])
    rows.append([tool, area, v["tp"] + v["fp"], v["tp"], v["fp"], prec(v["tp"], v["fp"]), lo, hi])
out("by_area.csv", ["tool", "masvs_area", "n", "tp", "fp", "precision", "wilson95_lo", "wilson95_hi"], rows)
tot = collections.Counter(r["triage"] for r in F)

# --- by target
bt = collections.defaultdict(collections.Counter)
for r in F: bt[r["target"]][r["triage"]] += 1
rows = []
for t in sorted(bt):
    v = bt[t]; s = SURV.get(t, {})
    rows.append([t, kind(t), s.get("tool", ""), s.get("prompt_style", ""), s.get("build_zero_touch", ""), v["tp"] + v["fp"], v["tp"], v["fp"], prec(v["tp"], v["fp"])])
out("by_target.csv", ["target", "artifact_kind", "generator_tool", "prompt_style", "build_zero_touch", "n", "tp", "fp", "precision"], rows)

# --- by generator bundle (survivors only)
bb = collections.defaultdict(lambda: collections.Counter())
scans = collections.Counter()
for t in bt:
    if t in SURV:
        b = SURV[t]["tool"]; scans[b] += 1
        for k, n in bt[t].items(): bb[b][k] += n
rows = [[b, scans[b], bb[b]["tp"], bb[b]["fp"], round(bb[b]["tp"] / scans[b], 2), prec(bb[b]["tp"], bb[b]["fp"])] for b in sorted(bb)]
out("by_bundle.csv", ["generator_tool", "scans", "tp", "fp", "tp_per_scan", "precision"], rows)

# --- sensitivity readings of the label rule
def cat_fp(r):
    m = r["method"]
    if re.search(r"librar|third-party|okhttp|androidx", m, re.I): return "library"
    if re.search(r"debug|assembleDebug", m, re.I): return "debug-artifact"
    return "other"
def buildtype(r):  # a finding that only describes the debug build type of a debug-type artifact
    return kind(r["target"]).startswith("debug") and (r["native_rule"] in ("app_is_debuggable", "android_aar_jar_debug_enabled") or r["native_rule"].startswith("cert:Application signed with debug"))
tp = sum(r["triage"] == "tp" for r in F); fp = sum(r["triage"] == "fp" for r in F)
fpc = collections.Counter(cat_fp(r) for r in F if r["triage"] == "fp")
bt_tp = sum(buildtype(r) and r["triage"] == "tp" for r in F); bt_fp = sum(buildtype(r) and r["triage"] == "fp" for r in F)
sens = [
 ["as triaged (primary)", tp, fp, prec(tp, fp)],
 ["strict: library and debug-artifact fps counted tp (what a scan of the artifact reports as real)", tp + fpc["library"] + fpc["debug-artifact"], fpc["other"], prec(tp + fpc["library"] + fpc["debug-artifact"], fpc["other"])],
 ["release-relevant: findings that only describe the debug build type of a debug-type artifact removed", tp - bt_tp, fp - bt_fp, prec(tp - bt_tp, fp - bt_fp)],
]
rel = [r for r in F if kind(r["target"]).startswith("release")]; dbg = [r for r in F if not kind(r["target"]).startswith("release")]
for lab, S in (("release-type artifacts only (3 targets)", rel), ("debug-type survivors only (10 targets)", dbg)):
    a = sum(r["triage"] == "tp" for r in S); b = sum(r["triage"] == "fp" for r in S); sens.append([lab, a, b, prec(a, b)])
out("sensitivity.csv", ["reading", "tp", "fp", "precision"], sens)

# --- detector (slop-scan) vs MobSF: presence of a security class in a survivor
CLS = ["S01", "S02", "S03", "S04", "S05", "S06", "S08", "S09"]  # classes both tools have a rule for (slop-scan S10/S12 are cited-only, MobSF has no S07 mapping)
surv = sorted(SURV)
slop = collections.defaultdict(list)
for r in E1F:
    if r["run_id"] in SURV and r["detector"] == "slop-scan" and r["rule_id"] in CLS: slop[(r["run_id"], r["rule_id"])].append(r)
mraw = collections.defaultdict(list); mtp = collections.defaultdict(list)
for r in F:
    if r["target"] in SURV and r["rule_id"] in CLS:
        mraw[(r["target"], r["rule_id"])].append(r)
        if r["triage"] == "tp": mtp[(r["target"], r["rule_id"])].append(r)
def kappa(a, b, c_, d):
    n = a + b + c_ + d; po = (a + d) / n; pe = ((a + b) * (a + c_) + (c_ + d) * (b + d)) / (n * n)
    return (round((po - pe) / (1 - pe), 3) if pe < 1 else ""), round(2 * po - 1, 3), round(po, 3)
agree = []; one = []
for lab, M in (("MobSF raw (any finding of the class)", mraw), ("MobSF triaged tp", mtp)):
    a = b = c_ = d = 0
    for t in surv:
        for k in CLS:
            s, m = bool(slop[(t, k)]), bool(M[(t, k)])
            a += s and m; b += s and not m; c_ += m and not s; d += (not s) and (not m)
            if lab.endswith("tp)") or lab.endswith("tp"):
                if s != m: one.append([t, k, "slop-scan only" if s else "MobSF only", (slop[(t, k)][0]["evidence"][:100] if s else M[(t, k)][0]["evidence"][:100])])
    kp, pabak, po = kappa(a, b, c_, d)
    agree.append([lab, len(surv) * len(CLS), a, b, c_, d, kp, pabak, po])
out("agreement.csv", ["comparison", "items", "both", "slop_only", "mobsf_only", "neither", "cohen_kappa", "pabak", "observed_agreement"], agree)
out("one_tool_only.csv", ["target", "class", "found_by", "evidence"], one)

# --- stats_e3.md
def md(h, rows): return "| " + " | ".join(h) + " |\n|" + "---|" * len(h) + "\n" + "\n".join("| " + " | ".join(str(x) for x in r) + " |" for r in rows)
with open(R / "stats_e3.md", "w") as fh:
    fh.write(f"# E3 statistics (descriptive)\n\nfindings {len(F)} on {len(bt)} targets, tp {tot['tp']}, fp {tot['fp']}, precision {prec(tot['tp'], tot['fp'])} (Wilson 95%: {wilson(tot['tp'], tot['tp'] + tot['fp'])})\n\n")
    fh.write("## by tool x MASVS area\n\n" + md(["tool", "area", "n", "tp", "fp", "precision", "lo", "hi"], rows := [r for r in csv.reader(open(R / "by_area.csv"))][1:]) + "\n\n")
    fh.write("## by target\n\n" + md(["target", "kind", "tool", "style", "zero-touch", "n", "tp", "fp", "precision"], [r for r in csv.reader(open(R / "by_target.csv"))][1:]) + "\n\n")
    fh.write("## by generator bundle (survivors)\n\n" + md(["tool", "scans", "tp", "fp", "tp/scan", "precision"], [r for r in csv.reader(open(R / "by_bundle.csv"))][1:]) + "\n\n")
    fh.write("## label-rule sensitivity\n\n" + md(["reading", "tp", "fp", "precision"], sens) + f"\n\nfp categories: {dict(fpc)}; build-type rows removed in the third reading: {bt_tp} tp, {bt_fp} fp\n\n")
    fh.write("## slop-scan vs MobSF (survivors x classes S01-S06,S08,S09)\n\n" + md(["comparison", "items", "both", "slop only", "mobsf only", "neither", "kappa", "PABAK", "agreement"], agree) + "\n\n")
    fh.write("## one tool only (MobSF triaged tp vs slop-scan)\n\n" + (md(["target", "class", "found by", "evidence"], one) if one else "none") + "\n")
print(open(R / "stats_e3.md").read())
