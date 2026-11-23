#!/usr/bin/env python3
"""E1 analysis (protocol 3.1, 4.1, 6): Wilson CIs, stratified permutation tests C1/C2, Mann-Whitney C3, G3 contrast, prevalence bands.

  analysis.py [--selftest]     reads results/*.csv, writes results/analysis/*.csv and results/analysis/summary.md
Pure stdlib (no numpy on this host). Seed 20261121. Findings count unless triage == fp. A rule counts toward C1-C3/H* only if it is
'validated' in results/validation/rules_status.csv (recall >= 0.80 on seeded mutants AND, once rated, precision >= 0.80 with >= 5 sampled);
the primary endpoints are also computed over ALL rules (sensitivity). Until precision.csv exists the run is labelled PRECISION PENDING.
"""
import argparse, collections, csv, itertools, math, pathlib, random, statistics, sys

E1 = pathlib.Path(__file__).resolve().parents[1]
RES = E1 / "results"
SEED = 20261121
S_CLASS = {f"S{i:02d}" for i in list(range(1, 10)) + [11]}
MEASURED = {f"P{i:02d}" for i in (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13)} | S_CLASS | {f"U{i:02d}" for i in range(1, 6)} | {"D01", "D02"}
INSTRUCTED = {"P01", "P02", "P04", "P06", "P07", "P09", "P10", "P11", "P13", "S01", "S02", "S03", "S04", "S05", "S07", "S08", "S11", "U01", "U02", "D01"}
HELD_OUT = {"P03", "P05", "P08", "S06", "D02", "U04", "U05"}
STYLES = ["naive", "detailed-functional", "guardrailed"]

# ---------------------------------------------------------------- statistics
def wilson(k, n, z=1.959964):
    if n == 0: return (float("nan"),) * 3
    p = k / n; d = 1 + z * z / n
    c = (p + z * z / (2 * n)) / d; h = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return p, max(0.0, c - h), min(1.0, c + h)

def boot_ci(xs, stat, B=10000, seed=SEED):
    if not xs: return (float("nan"),) * 2
    rng = random.Random(seed); n = len(xs); vals = sorted(stat([xs[rng.randrange(n)] for _ in range(n)]) for _ in range(B))
    return vals[int(0.025 * B)], vals[int(0.975 * B) - 1]

def boot_diff_ci(a, b, stat=statistics.fmean, B=10000, seed=SEED):
    if not a or not b: return (float("nan"),) * 2
    rng = random.Random(seed); vals = sorted(stat([a[rng.randrange(len(a))] for _ in a]) - stat([b[rng.randrange(len(b))] for _ in b]) for _ in range(B))
    return vals[int(0.025 * B)], vals[int(0.975 * B) - 1]

def perm_test(strata, stat, perms=100000, seed=SEED):
    """strata: list of (labels, values); permute labels within each stratum. stat(group_a_values, group_b_values) with a='A', b='B'."""
    rng = random.Random(seed)
    def S(ls):
        a = [v for (_, vs), l in zip(strata, ls) for v, x in zip(vs, l) if x == "A"]
        b = [v for (_, vs), l in zip(strata, ls) for v, x in zip(vs, l) if x == "B"]
        return stat(a, b) if a and b else 0.0
    obs = S([s[0] for s in strata]); ge = 0
    labs = [list(s[0]) for s in strata]
    for _ in range(perms):
        for l in labs: rng.shuffle(l)
        if abs(S(labs)) >= abs(obs) - 1e-12: ge += 1
    return obs, (ge + 1) / (perms + 1)

def mannwhitney(x, y):
    """Two-sided p (normal approx, tie + continuity corrected), U for x, Cliff's delta (x > y positive)."""
    n1, n2 = len(x), len(y)
    if not n1 or not n2: return float("nan"), float("nan"), float("nan")
    allv = sorted([(v, 0) for v in x] + [(v, 1) for v in y]); ranks = {}; i = 0
    while i < len(allv):
        j = i
        while j + 1 < len(allv) and allv[j + 1][0] == allv[i][0]: j += 1
        for k in range(i, j + 1): ranks[k] = (i + j) / 2 + 1
        i = j + 1
    r1 = sum(ranks[k] for k, (_, g) in enumerate(allv) if g == 0); u1 = r1 - n1 * (n1 + 1) / 2
    ties = collections.Counter(v for v, _ in allv); t = sum(c ** 3 - c for c in ties.values()); N = n1 + n2
    sd = math.sqrt(n1 * n2 / 12 * ((N + 1) - t / (N * (N - 1)))) if N > 1 else 0
    z = (abs(u1 - n1 * n2 / 2) - 0.5) / sd if sd else 0
    p = math.erfc(z / math.sqrt(2))
    gt = sum(1 for a in x for b in y if a > b); lt = sum(1 for a in x for b in y if a < b)
    return p, u1, (gt - lt) / (n1 * n2)

def holm(ps):
    """ps: dict id -> p. Returns dict id -> Holm-adjusted p (step-down)."""
    items = sorted(ps.items(), key=lambda kv: kv[1]); m = len(items); out = {}; run = 0.0
    for i, (k, p) in enumerate(items):
        run = max(run, min(1.0, (m - i) * p)); out[k] = run
    return out

# ---------------------------------------------------------------- data
SRC = {}          # track overrides: logical name -> path under results/ (v1 primary reads the frozen pre-fix scans)
TRACKS = {
    "v1": {"findings.csv": "validation/v1/pre_fix_findings.csv", "metrics.csv": "validation/v1/pre_fix_metrics.csv", "human_findings.csv": "validation/v1/pre_fix_human_findings.csv",
           "human_metrics.csv": "validation/v1/pre_fix_human_metrics.csv", "validation/rules_status.csv": "validation/rules_status_v1.csv"},
    "v2": {"validation/rules_status.csv": "validation/rules_status_v2.csv"},
    # dependency lookup corrected (alpha Q#dep-fix): the same detector rows with the depcheck rows re-resolved over the union of declared repos
    "v1c": {"findings.csv": "depfix/findings_v1_corrected.csv", "metrics.csv": "validation/v1/pre_fix_metrics.csv", "human_findings.csv": "depfix/human_findings_v1_corrected.csv",
            "human_metrics.csv": "validation/v1/pre_fix_human_metrics.csv", "validation/rules_status.csv": "validation/rules_status_v1.csv"},
    "v2c": {"findings.csv": "depfix/findings_v2_corrected.csv", "human_findings.csv": "depfix/human_findings_v2_corrected.csv", "validation/rules_status.csv": "validation/rules_status_v2.csv"},
}
OUTDIR = {"v1": "primary-v1", "v2": "sensitivity-v2", "v1c": "primary-v1-deps-corrected", "v2c": "sensitivity-v2-deps-corrected"}

def read(name):
    f = RES / SRC.get(name, name)
    return list(csv.DictReader(open(f))) if f.exists() else []

def rule_status():
    st = {}
    for r in read("validation/rules_status.csv"):
        st[r["rule_id"]] = r["status"] == "validated"
    return st

def load():
    runs = [r for r in read("runs.csv") if not r["run_id"].startswith("pilot-")]
    by = {r["run_id"]: r for r in runs}
    findings = collections.defaultdict(list)
    for f in read("findings.csv"):
        if f["run_id"] in by and f["triage"] != "fp" and f["rule_id"] in MEASURED: findings[f["run_id"]].append(f)
    metrics = {m["run_id"]: m for m in read("metrics.csv")}
    return runs, findings, metrics

def valid_runs(runs): return [r for r in runs if str(r.get("valid")) in ("True", "true", "1")]

def rules_of(findings, rid, allowed): return {f["rule_id"] for f in findings.get(rid, []) if f["rule_id"] in allowed}

# ---------------------------------------------------------------- analyses
def analyse(allowed_rules, label, runs, findings, metrics, human, hmetrics, hcorpus, out):
    vr = valid_runs(runs); lines = [f"\n## Analysis over {label} rules ({len(allowed_rules)} rules)\n"]
    cells = collections.defaultdict(list)
    for r in vr: cells[(r["brief"], r["bundle"])].append(r)
    # endpoint 1 and 2 by style
    ep = {}
    lines.append("| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |\n|---|---|---|---|---|---|---|---|")
    for st in STYLES:
        rs = [r for r in vr if r["prompt_style"] == st]
        s_flags = [int(bool(rules_of(findings, r["run_id"], allowed_rules & S_CLASS))) for r in rs]
        dist = [len(rules_of(findings, r["run_id"], allowed_rules)) for r in rs]
        k, n = sum(s_flags), len(rs); p, lo, hi = wilson(k, n)
        bl, bh = boot_ci(dist, statistics.fmean)
        ep[st] = (s_flags, dist, rs)
        lines.append(f"| {st} | {n} | {k} | {p:.3f} | [{lo:.3f}, {hi:.3f}] | {statistics.fmean(dist) if dist else float('nan'):.2f} | {statistics.median(dist) if dist else float('nan'):.1f} | [{bl:.2f}, {bh:.2f}] |")
    out["endpoints_" + label] = lines[-4:]
    # C1/C2: naive vs guardrailed, stratified by brief x bundle
    def strata_for(idx):
        sv = []
        for key, rs in cells.items():
            a = [r for r in rs if r["prompt_style"] == "naive"]; b = [r for r in rs if r["prompt_style"] == "guardrailed"]
            if a and b:
                vals = [(ep["naive"][idx][ep["naive"][2].index(r)]) for r in a] + [(ep["guardrailed"][idx][ep["guardrailed"][2].index(r)]) for r in b]
                sv.append((["A"] * len(a) + ["B"] * len(b), vals))
        return sv
    res = {}
    for cid, idx in (("C1", 0), ("C2", 1)):
        sv = strata_for(idx)
        if sv:
            mean = lambda a, b: statistics.fmean(a) - statistics.fmean(b)
            obs, p = perm_test(sv, mean, perms=100000)
            res[cid] = (obs, p, len(sv))
            lines.append(f"\n**{cid}** (naive - guardrailed, {len(sv)} strata brief x bundle): difference {obs:+.3f}, stratified permutation p = {p:.5f} (100,000 perms, seed {SEED}).")
        else:
            lines.append(f"\n**{cid}**: no stratum has both naive and guardrailed valid runs yet.")
    # C3: S-class findings per 1000 LOC, unguardrailed AI vs pre-assistant humans
    # Two size normalisers: physical LOC (frozen, protocol 5) and non-whitespace code chars (format-invariant sensitivity: codex packs ~130 chars/line).
    def per_k(fl, size, scale, sc=S_CLASS): return scale * sum(1 for f in fl if f["rule_id"] in (allowed_rules & sc)) / max(1, size)
    hf = collections.defaultdict(list)
    for f in human:
        if f["triage"] != "fp" and f["rule_id"] in MEASURED: hf[f["run_id"]].append(f)
    for key, scale, unit, confirm in (("kotlin_loc", 1000, "kLOC", True), ("kotlin_nws", 100000, "100k non-whitespace chars (sensitivity)", False)):
        ai = [per_k(findings.get(r["run_id"], []), float(metrics.get(r["run_id"], {}).get(key) or 0), scale) for r in vr if r["prompt_style"] in ("naive", "detailed-functional")]
        hh = {s: [] for s in ("pre", "post")}
        for h in hcorpus:
            rid = "human:" + h["repo"] + "@" + h["commit"][:8]
            size = float((hmetrics.get(rid) or {}).get(key) or 0)
            if size: hh[h["stratum"]].append(per_k(hf.get(rid, []), size, scale))
        for strat, xs in hh.items():
            if ai and xs:
                p, u, cd = mannwhitney(ai, xs)
                tag = "C3" if confirm else "C3-size-sensitivity"
                lines.append(f"\n**{tag}{'' if strat == 'pre' else ' (second comparator, not confirmatory)'}**: AI unguardrailed n={len(ai)} median {statistics.median(ai):.2f} S-class findings/{unit} vs human {strat} n={len(xs)} median {statistics.median(xs):.2f}; Mann-Whitney p = {p:.5f}, Cliff's delta = {cd:+.3f}.")
                if strat == "pre" and confirm: res["C3"] = (cd, p, len(xs))
    out["C"] = res
    # G3
    def count(rs, sset): return [len(rules_of(findings, r["run_id"], allowed_rules & sset)) for r in rs]
    for base in ("naive", "detailed-functional"):
        b_rs, g_rs = ep[base][2], ep["guardrailed"][2]
        if b_rs and g_rs:
            di = statistics.fmean(count(b_rs, INSTRUCTED)) - statistics.fmean(count(g_rs, INSTRUCTED))
            dh = statistics.fmean(count(b_rs, HELD_OUT)) - statistics.fmean(count(g_rs, HELD_OUT))
            rng = random.Random(SEED); vals = []
            bi, gi, bh, gh = count(b_rs, INSTRUCTED), count(g_rs, INSTRUCTED), count(b_rs, HELD_OUT), count(g_rs, HELD_OUT)
            for _ in range(10000):
                ib = [rng.randrange(len(b_rs)) for _ in b_rs]; ig = [rng.randrange(len(g_rs)) for _ in g_rs]
                vals.append((statistics.fmean(bi[i] for i in ib) - statistics.fmean(gi[i] for i in ig)) - (statistics.fmean(bh[i] for i in ib) - statistics.fmean(gh[i] for i in ig)))
            vals.sort()
            lines.append(f"\n**G3** ({base} - guardrailed): Delta_instructed {di:+.2f}, Delta_heldout {dh:+.2f}, contrast {di - dh:+.2f}, bootstrap 95% [{vals[250]:.2f}, {vals[9749]:.2f}] (secondary, pre-registered contrast).")
    return lines

def prevalence(runs, findings, allowed, out):
    vr = [r for r in valid_runs(runs) if r["prompt_style"] in ("naive", "detailed-functional")]
    groups = {"H1 (D01 or D02)": {"D01", "D02"}, "H5 (U01 or U05)": {"U01", "U05"}, "H6 (S05)": {"S05"}}
    lines = ["\n## Prevalence over unguardrailed runs (PB rule: supported if Wilson lower >= 0.50, refuted if upper < 0.20)\n", "| hypothesis | k/n | share | Wilson 95% | PB decision |\n|---|---|---|---|---|"]
    for name, rs in groups.items():
        k = sum(1 for r in vr if rules_of(findings, r["run_id"], allowed & rs)); n = len(vr); p, lo, hi = wilson(k, n)
        dec = "supported" if lo >= 0.5 else "refuted" if hi < 0.2 else "inconclusive"
        lines.append(f"| {name} | {k}/{n} | {p:.3f} | [{lo:.3f}, {hi:.3f}] | {dec} |")
    return lines

def sensitivity(allowed, runs, findings, metrics, human, hmet, hc):
    """Protocol A6 sensitivity variants as one compact table: (a) no web-tool attempt, (b) per bundle and leave-one-bundle-out, (c) compile-excluded."""
    bundles = sorted({r["bundle"] for r in runs})
    variants = [("(a) no web-tool attempt", [r for r in runs if int(r.get("web_tool_calls") or 0) == 0]),
                ("(c) compile-excluded (build pass)", [r for r in runs if r.get("build") == "pass"])]
    variants += [(f"(b) only {b}", [r for r in runs if r["bundle"] == b]) for b in bundles]
    variants += [(f"(b) without {b}", [r for r in runs if r["bundle"] != b]) for b in bundles]
    rows = ["\n## Sensitivity analyses (protocol A6; none replaces a confirmatory endpoint)\n", "| variant | n valid | C1 diff (p) | C2 diff (p) | C3 Cliff's delta (p) |\n|---|---|---|---|---|"]
    for name, rs in variants:
        o = {}
        analyse(allowed, name, rs, findings, metrics, human, hmet, hc, o)
        c = o.get("C", {})
        f = lambda k: f"{c[k][0]:+.3f} (p={c[k][1]:.4f})" if k in c else "n/a"
        rows.append(f"| {name} | {len(valid_runs(rs))} | {f('C1')} | {f('C2')} | {f('C3')} |")
    return rows

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--selftest", action="store_true")
    ap.add_argument("--track", default="both", choices=["both", "v1", "v2", "v1c", "v2c", "corrected", "legacy"], help="v1 = frozen pre-fix detector (PRIMARY, alpha A13), v2 = fixed detector (sensitivity), legacy = old single run on the current files")
    a = ap.parse_args()
    if a.selftest:
        assert abs(wilson(10, 20)[1] - 0.299) < 0.002 and abs(wilson(10, 20)[2] - 0.701) < 0.002
        p, u, d = mannwhitney([1, 2, 3, 4, 5], [6, 7, 8, 9, 10]); assert d == -1.0 and p < 0.02
        assert holm({"a": 0.01, "b": 0.04, "c": 0.03}) == {"a": 0.03, "c": 0.06, "b": 0.06}
        obs, p = perm_test([(["A"] * 8 + ["B"] * 8, [1] * 8 + [0] * 8)], lambda x, y: statistics.fmean(x) - statistics.fmean(y), perms=5000)
        assert obs == 1.0 and p < 0.01
        print("selftest ok"); return
    if a.track in ("both", "v1", "v2", "v1c", "v2c", "corrected"):
        for tr in ({"both": ["v1", "v2"], "corrected": ["v1c", "v2c"]}.get(a.track) or [a.track]):
            SRC.clear(); SRC.update(TRACKS[tr]); run_track(OUTDIR[tr], tr)
        SRC.clear(); return
    run_track("", "legacy")

def run_track(sub, label):
    runs, findings, metrics = load()
    hc = read("human_corpus.csv"); human = read("human_findings.csv"); hmet = {m["run_id"]: m for m in read("human_metrics.csv")}
    status = rule_status(); pending = not (RES / SRC.get("validation/rules_status.csv", "validation/precision.csv")).exists()
    allowed_valid = {r for r in MEASURED if status.get(r, True)}
    out = {}; md = [f"# E1 analysis, detector track: {label}{' (PRECISION PENDING: provisional, rules validated on recall only)' if pending else ''}\n"]
    vr = valid_runs(runs)
    md.append(f"Runs: {len(runs)} generated, {len(vr)} valid. Unvalidated rules excluded from the primary analysis: {sorted(MEASURED - allowed_valid) or 'none'}.")
    md += analyse(allowed_valid, "validated", runs, findings, metrics, human, hmet, hc, out)
    md += analyse(MEASURED, "all", runs, findings, metrics, human, hmet, hc, out)
    md += prevalence(runs, findings, allowed_valid, out)
    md += sensitivity(allowed_valid, runs, findings, metrics, human, hmet, hc)
    adir = RES / "analysis" / sub; adir.mkdir(parents=True, exist_ok=True)
    (adir / "summary.md").write_text("\n".join(md) + "\n")
    print("\n".join(md)[:6000])

if __name__ == "__main__":
    main()
