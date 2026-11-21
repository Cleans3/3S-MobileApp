#!/usr/bin/env python3
"""Merge both rating rounds into per-track rule status (alpha A13): v1 detector = PRIMARY, v2 (fixed) detector = SENSITIVITY.

  rate_merge2.py --r2-v1 PATH --r2-v2 PATH [--out DIR] [--adj-v1 CSV] [--adj-v2 CSV]

Inputs: results/validation/rating_sheet_r1.csv (v1 pack, r1), --r2-v1 (v1 pack, r2), results/validation/v2/rating_sheet_r1.csv, --r2-v2 (v2 pack, r2).
Consensus per item = both raters agree, else adjudication (item id, label), else 'unclear'; 'unclear' is excluded from the precision denominator.
Track v1: every rule on its v1 labels; U05 also takes the v2 fresh items (its detector did not change; v1 items were unjudgeable from a 6-line window).
Track v2: rules not fixed use their v1 labels; FIXED rules use ONLY the fresh v2 items (v1 survivors are in-sample, reported by the analysis as secondary); U05 as in track v1.
Rule status: recall >= 0.80 on seeded mutants AND precision >= 0.80 with >= 5 rated items = validated, else unvalidated-precision / unvalidated-recall.
Writes precision_v1.csv, precision_v2.csv, rules_status_v1.csv, rules_status_v2.csv, kappa.json; precision.csv and rules_status.csv are copies of track v1 (the primary).
"""
import argparse, collections, csv, json, pathlib, shutil, sys

E1 = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(E1 / "harness"))
from analysis import wilson  # noqa: E402
from rate_merge import kappa  # noqa: E402
VAL = E1 / "results" / "validation"
FIXED = {"P10", "S03", "P05", "P09", "P07"}
POOL_V2_BOTH = {"U05"}

def sheet(paths):
    by = collections.defaultdict(dict)
    for p in paths:
        for r in csv.DictReader(open(p)):
            if r["label"].strip(): by[r["sample_id"]][r["rater"]] = r["label"].strip().lower()
    return by

def consensus(by, adj):
    raters = sorted({k for v in by.values() for k in v})
    if len(raters) < 2: return {}, [], raters, 0
    r1, r2 = raters[:2]; cons, pairs, dis = {}, [], 0
    for sid, v in by.items():
        if r1 in v and r2 in v:
            pairs.append((v[r1], v[r2]))
            if v[r1] == v[r2]: cons[sid] = v[r1]
            else: dis += 1; cons[sid] = adj.get(sid, "unclear")
    return cons, pairs, raters, dis

def adjfile(p): return {r["sample_id"]: r["label"].strip().lower() for r in csv.DictReader(open(p))} if p and pathlib.Path(p).exists() else {}

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--r2-v1", required=True); ap.add_argument("--r2-v2", required=True); ap.add_argument("--out", default=str(VAL))
    ap.add_argument("--adj-v1", default=str(VAL / "adjudication.csv")); ap.add_argument("--adj-v2", default=str(VAL / "v2" / "adjudication.csv")); a = ap.parse_args()
    out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    rule1 = {r["sample_id"]: r["rule_id"] for r in csv.DictReader(open(VAL / "v1" / "blind_sample.csv"))}
    rule2 = {r["sample_id"]: r["rule_id"] for r in csv.DictReader(open(VAL / "v2" / "blind_sample.csv"))}
    c1, p1, rat1, d1 = consensus(sheet([VAL / "rating_sheet_r1.csv", a.r2_v1]), adjfile(a.adj_v1))
    c2, p2, rat2, d2 = consensus(sheet([VAL / "v2" / "rating_sheet_r1.csv", a.r2_v2]), adjfile(a.adj_v2))
    if not c1 or not c2: sys.exit(f"need two raters on both packs; found v1 {rat1}, v2 {rat2}")
    rec = {r["rule_id"]: float(r["recall"]) for r in csv.DictReader(open(VAL / "mutation.csv"))}
    cnt1 = collections.defaultdict(collections.Counter); cnt2 = collections.defaultdict(collections.Counter)
    for sid, lab in c1.items(): cnt1[rule1.get(sid, "?")][lab] += 1
    for sid, lab in c2.items(): cnt2[rule2.get(sid, "?")][lab] += 1
    tracks = {}
    for tr in ("v1", "v2"):
        rows, status = [], []
        for rule in sorted(rec):
            c = collections.Counter()
            if tr == "v2" and rule in FIXED: c.update(cnt2.get(rule, {}))
            else:
                c.update(cnt1.get(rule, {}))
                if rule in POOL_V2_BOTH: c.update(cnt2.get(rule, {}))
            tp, fp, un = c["tp"], c["fp"], c["unclear"]; n = tp + fp
            p, lo, hi = wilson(tp, n) if n else (None, None, None)
            rows.append({"rule_id": rule, "n_rated": n, "tp": tp, "fp": fp, "unclear": un, "precision": "" if p is None else f"{p:.3f}",
                         "wilson_lo": "" if lo is None else f"{lo:.3f}", "wilson_hi": "" if hi is None else f"{hi:.3f}",
                         "source": "v2 fresh items" if (tr == "v2" and rule in FIXED) else "v1 items" + (" + v2 fresh" if rule in POOL_V2_BOTH else "")})
            st = "unvalidated-recall" if rec.get(rule, 0) < 0.80 else "validated" if (n >= 5 and p >= 0.80) else "unvalidated-precision"
            status.append({"rule_id": rule, "recall": rec.get(rule, ""), "precision": "" if p is None else f"{p:.3f}", "n_rated": n, "status": st})
        for name, rr in ((f"precision_{tr}.csv", rows), (f"rules_status_{tr}.csv", status)):
            with open(out / name, "w", newline="") as f:
                w = csv.DictWriter(f, list(rr[0])); w.writeheader(); w.writerows(rr)
        tracks[tr] = status
    shutil.copyfile(out / "precision_v1.csv", out / "precision.csv"); shutil.copyfile(out / "rules_status_v1.csv", out / "rules_status.csv")
    kall = kappa([x for x, _ in p1 + p2], [y for _, y in p1 + p2])
    info = {"raters_v1": rat1[:2], "raters_v2": rat2[:2], "n_pairs_v1": len(p1), "n_pairs_v2": len(p2),
            "kappa_v1": round(kappa([x for x, _ in p1], [y for _, y in p1]), 3), "kappa_v2": round(kappa([x for x, _ in p2], [y for _, y in p2]), 3),
            "kappa": round(kall, 3), "disagreements_v1": d1, "disagreements_v2": d2, "gate": "FAIL" if kall < 0.60 else "ok"}
    (out / "kappa.json").write_text(json.dumps(info, indent=1))
    print(json.dumps(info))
    for tr in ("v1", "v2"):
        print(tr, "not validated:", ", ".join(f"{s['rule_id']}({s['status'].split('-')[-1]},n={s['n_rated']},p={s['precision']})" for s in tracks[tr] if s["status"] != "validated"))
    if info["gate"] == "FAIL": print("GATE FAIL: pooled kappa < 0.60, revise rubric and re-rate")

if __name__ == "__main__":
    main()
