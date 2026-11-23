#!/usr/bin/env python3
"""results/raw.csv -> results/c5_confirmatory.csv: protocol C5 (P08, P90 of FrameDurationCpuMs, A vs B).
delta% = (A - B) / B of the medians, percentile bootstrap 95% CI (10,000 resamples, seed 20261121), Mann-Whitney raw p. Holm column left empty (alpha applies it across C1-C5).
Usage: python3 c5.py   (stdlib only; reuses summarize.py)"""
import csv, random, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import summarize as S

RES = Path(__file__).resolve().parent.parent / "results"
FACTOR, METRIC = "P08", "frameDurationCpuMs_p90"
vals = {"A": [], "B": []}
for r in csv.DictReader((RES / "raw.csv").open()):
    if r["factor_id"] == FACTOR and r["metric"] == METRIC:
        vals[r["variant"]].append(float(r["value"]))
a, b = vals["A"], vals["B"]
ma, mb = S.median(a), S.median(b)
delta = (ma - mb) / mb * 100
rnd = random.Random(20261121)
ds = []
for _ in range(10000):
    xa = S.median([rnd.choice(a) for _ in a])
    xb = S.median([rnd.choice(b) for _ in b])
    if xb != 0:
        ds.append((xa - xb) / xb * 100)
lo, hi = S.quantile(ds, 0.025), S.quantile(ds, 0.975)
p = S.mann_whitney_p(a, b)
excl = lo > 0 or hi < 0
crit = delta >= 10 and excl
out = RES / "c5_confirmatory.csv"
with out.open("w", newline="") as f:
    w = csv.writer(f, lineterminator="\n")
    w.writerow(["factor_id", "metric", "unit", "n_A", "n_B", "A_median", "B_median", "delta_pct_A_minus_B_over_B", "ci95_lo", "ci95_hi",
                "mann_whitney_p_raw", "holm_adjusted_p", "criterion", "ci_excludes_0", "delta_ge_10pct", "criteria_met_before_holm", "rig_note"])
    w.writerow([FACTOR, METRIC, "ms", len(a), len(b), f"{ma:.6g}", f"{mb:.6g}", f"{delta:.4g}", f"{lo:.4g}", f"{hi:.4g}", f"{p:.4g}", "",
                "Holm-adjusted p < 0.05 and delta% >= +10% and bootstrap 95% CI excludes 0", "yes" if excl else "no", "yes" if delta >= 10 else "no",
                "yes" if crit else "no", "A14: P08 A and B both on swangle_indirect, same window and AVD; other factors on swiftshader_indirect"])
print(out.read_text())
