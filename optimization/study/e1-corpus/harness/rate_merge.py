#!/usr/bin/env python3
"""Merge blind ratings into per-rule precision and rule status (protocol 5b gate).

  rate_merge.py [--sheet results/validation/rating_sheet.csv] [--adjudication results/validation/adjudication.csv]

rating_sheet.csv: sample_id, rater, label(tp|fp|unclear), note. Raters are any two distinct names (e.g. r1 = this pane, r2 = independent).
Consensus label = the two agree; a disagreement takes adjudication.csv (sample_id, label; the third reading with the rubric) or stays 'unclear'.
'unclear' is excluded from the precision denominator and reported. Rule status:
  validated            recall >= 0.80 on seeded mutants AND precision >= 0.80 with >= 5 rated findings
  unvalidated-precision  precision < 0.80 or < 5 rated  (kept in the 'all rules' sensitivity analysis, dropped from the primary one)
  unvalidated-recall     recall < 0.80
Overall Cohen's kappa < 0.60 prints GATE FAIL: revise the rubric and re-rate before analysis.
Writes results/validation/precision.csv, rules_status.csv, kappa.json.
"""
import argparse, collections, csv, json, pathlib, sys

E1 = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(E1 / "harness"))
from analysis import wilson  # noqa: E402
VAL = E1 / "results" / "validation"


def kappa(a, b):
    labs = sorted(set(a) | set(b)); n = len(a)
    if not n: return None
    po = sum(x == y for x, y in zip(a, b)) / n
    pe = sum((a.count(l) / n) * (b.count(l) / n) for l in labs)
    return 1.0 if pe == 1 else (po - pe) / (1 - pe)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sheet", default=str(VAL / "rating_sheet.csv"))
    ap.add_argument("--adjudication", default=str(VAL / "adjudication.csv"))
    a = ap.parse_args()
    rule_of = {r["sample_id"]: r["rule_id"] for r in csv.DictReader(open(VAL / "blind_sample.csv"))}
    by = collections.defaultdict(dict)
    for r in csv.DictReader(open(a.sheet)):
        if r["label"].strip(): by[r["sample_id"]][r["rater"]] = r["label"].strip().lower()
    adj = {r["sample_id"]: r["label"].strip().lower() for r in csv.DictReader(open(a.adjudication))} if pathlib.Path(a.adjudication).exists() else {}
    raters = sorted({k for v in by.values() for k in v})
    if len(raters) < 2:
        sys.exit(f"need two raters in {a.sheet}, found {raters}")
    r1, r2 = raters[:2]
    pairs = [(v[r1], v[r2]) for v in by.values() if r1 in v and r2 in v]
    k = kappa([p[0] for p in pairs], [p[1] for p in pairs])
    cons, n_dis = {}, 0
    for sid, v in by.items():
        if r1 in v and r2 in v:
            if v[r1] == v[r2]: cons[sid] = v[r1]
            else: n_dis += 1; cons[sid] = adj.get(sid, "unclear")
    cnt = collections.defaultdict(collections.Counter)
    for sid, lab in cons.items(): cnt[rule_of.get(sid, "?")][lab] += 1
    rec = {r["rule_id"]: float(r["recall"]) for r in csv.DictReader(open(VAL / "mutation.csv"))}
    prec_rows, status = [], []
    for rule in sorted(set(rec) | set(cnt)):
        c = cnt.get(rule, collections.Counter()); tp, fp, un = c["tp"], c["fp"], c["unclear"]; n = tp + fp
        p, lo, hi = wilson(tp, n) if n else (None, None, None)
        prec_rows.append({"rule_id": rule, "n_rated": n, "tp": tp, "fp": fp, "unclear": un,
                          "precision": "" if p is None else f"{p:.3f}", "wilson_lo": "" if lo is None else f"{lo:.3f}", "wilson_hi": "" if hi is None else f"{hi:.3f}"})
        st = "unvalidated-recall" if rec.get(rule, 0) < 0.80 else "validated" if (n >= 5 and p >= 0.80) else "unvalidated-precision"
        status.append({"rule_id": rule, "recall": rec.get(rule, ""), "precision": "" if p is None else f"{p:.3f}", "n_rated": n, "status": st})
    for name, rows in (("precision.csv", prec_rows), ("rules_status.csv", status)):
        with open(VAL / name, "w", newline="") as f:
            w = csv.DictWriter(f, list(rows[0])); w.writeheader(); w.writerows(rows)
    info = {"raters": [r1, r2], "n_pairs": len(pairs), "kappa": None if k is None else round(k, 3), "disagreements": n_dis, "adjudicated": sum(1 for s in adj if s in cons),
            "gate": "FAIL" if (k is not None and k < 0.60) else "ok"}
    (VAL / "kappa.json").write_text(json.dumps(info, indent=1))
    print(json.dumps(info), "\n" + "\n".join(f"{s['rule_id']} {s['status']} n={s['n_rated']} p={s['precision']}" for s in status if s["status"] != "validated"))
    if info["gate"] == "FAIL": print("GATE FAIL: kappa < 0.60, revise rubric and re-rate")


if __name__ == "__main__":
    main()
