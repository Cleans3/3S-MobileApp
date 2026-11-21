#!/usr/bin/env python3
"""results/raw.csv -> results/summary.csv (+ summary.md). Standard library only.

Per (factor, metric): medians and IQR for A and B, delta_pct = (B_median - A_median) / A_median * 100 (negative = B lower),
a 95% bootstrap CI of delta_pct (10000 resamples of each variant's iterations, seed 7), Cliff's delta (> 0: A tends larger),
a two-sided Mann-Whitney U p-value (normal approximation, tie-corrected, descriptive: no confirmatory family is declared for E2),
and n = iterations per variant (the smaller of the two).
Usage: python3 summarize.py [results_dir]
"""
import csv, math, random, sys
from collections import defaultdict
from pathlib import Path

RES = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / "results"


def quantile(xs, q):
    s = sorted(xs)
    k = (len(s) - 1) * q
    lo = int(k)
    hi = min(lo + 1, len(s) - 1)
    return s[lo] + (s[hi] - s[lo]) * (k - lo)


def median(xs):
    return quantile(xs, 0.5)


def iqr(xs):
    return quantile(xs, 0.75) - quantile(xs, 0.25)


def cliffs_delta(a, b):
    gt = sum(1 for x in a for y in b if x > y)
    lt = sum(1 for x in a for y in b if x < y)
    return (gt - lt) / (len(a) * len(b))


def mann_whitney_p(a, b):
    n1, n2 = len(a), len(b)
    pooled = sorted([(v, 0) for v in a] + [(v, 1) for v in b])
    ranks, ties, i = [0.0] * len(pooled), 0.0, 0
    while i < len(pooled):
        j = i
        while j + 1 < len(pooled) and pooled[j + 1][0] == pooled[i][0]:
            j += 1
        for k in range(i, j + 1):
            ranks[k] = (i + j) / 2 + 1
        ties += (j - i + 1) ** 3 - (j - i + 1)
        i = j + 1
    r1 = sum(r for r, (_, g) in zip(ranks, pooled) if g == 0)
    u1 = r1 - n1 * (n1 + 1) / 2
    n = n1 + n2
    sigma = math.sqrt(n1 * n2 / 12 * ((n + 1) - ties / (n * (n - 1))))
    if sigma == 0:
        return 1.0
    z = max(abs(u1 - n1 * n2 / 2) - 0.5, 0) / sigma
    return math.erfc(z / math.sqrt(2))


def bootstrap_delta_ci(a, b, reps=10000, seed=7):
    rnd = random.Random(seed)
    out = []
    for _ in range(reps):
        ma = median([rnd.choice(a) for _ in a])
        mb = median([rnd.choice(b) for _ in b])
        if ma != 0:
            out.append((mb - ma) / ma * 100)
    if len(out) < reps // 2:
        return None, None
    return quantile(out, 0.025), quantile(out, 0.975)


def fmt(x, digits=4):
    return "" if x is None else f"{x:.{digits}g}"


def main():
    groups = defaultdict(lambda: {"A": [], "B": []})
    units = {}
    for r in csv.DictReader((RES / "raw.csv").open()):
        key = (r["factor_id"], r["metric"])
        groups[key][r["variant"]].append(float(r["value"]))
        units[key] = r["unit"]
    rows = []
    for (fid, metric), g in groups.items():
        a, b = g["A"], g["B"]
        if not a or not b:
            continue
        ma, mb = median(a), median(b)
        n = min(len(a), len(b))
        delta = (mb - ma) / ma * 100 if ma != 0 else None
        lo, hi = bootstrap_delta_ci(a, b) if n > 1 else (None, None)
        rows.append([
            fid, metric, units[(fid, metric)], fmt(ma), fmt(iqr(a)), fmt(mb), fmt(iqr(b)),
            fmt(delta, 3), fmt(lo, 3), fmt(hi, 3),
            fmt(cliffs_delta(a, b), 3) if n > 1 else "", fmt(mann_whitney_p(a, b), 3) if n > 1 else "", n,
        ])
    order = ["P08", "P06", "P01+P09", "P13", "P03+P04", "P10", "P11"]
    rows.sort(key=lambda r: (order.index(r[0]) if r[0] in order else 99, r[1]))
    head = ["factor_id", "metric", "unit", "A_median", "A_iqr", "B_median", "B_iqr", "delta_pct", "delta_ci95_lo", "delta_ci95_hi", "cliffs_delta", "p", "n"]
    with (RES / "summary.csv").open("w", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(head)
        w.writerows(rows)
    md = ["| " + " | ".join(head) + " |", "|" + "---|" * len(head)] + ["| " + " | ".join(map(str, r)) + " |" for r in rows]
    (RES / "summary.md").write_text("\n".join(md) + "\n")
    print(f"{len(rows)} rows -> {RES / 'summary.csv'}")


if __name__ == "__main__":
    main()
