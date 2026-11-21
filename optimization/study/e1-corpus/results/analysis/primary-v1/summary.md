# E1 analysis, detector track: v1

Runs: 101 generated, 100 valid. Unvalidated rules excluded from the primary analysis: ['P05', 'P06', 'P07', 'P09', 'P10', 'S03', 'S04', 'S08', 'U05'].

## Analysis over validated rules (20 rules)

| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |
|---|---|---|---|---|---|---|---|
| naive | 33 | 31 | 0.939 | [0.804, 0.983] | 5.85 | 4.0 | [4.91, 6.85] |
| detailed-functional | 33 | 29 | 0.879 | [0.727, 0.952] | 5.70 | 6.0 | [5.09, 6.33] |
| guardrailed | 34 | 17 | 0.500 | [0.341, 0.659] | 2.82 | 3.0 | [2.41, 3.26] |

**C1** (naive - guardrailed, 9 strata brief x bundle): difference +0.439, stratified permutation p = 0.00004 (100,000 perms, seed 20261121).

**C2** (naive - guardrailed, 9 strata brief x bundle): difference +3.025, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C3**: AI unguardrailed n=66 median 2.37 S-class findings/kLOC vs human pre n=20 median 1.02; Mann-Whitney p = 0.00009, Cliff's delta = +0.582.

**C3 (second comparator, not confirmatory)**: AI unguardrailed n=66 median 2.37 S-class findings/kLOC vs human post n=20 median 0.24; Mann-Whitney p = 0.00000, Cliff's delta = +0.742.

**C3-size-sensitivity**: AI unguardrailed n=66 median 7.83 S-class findings/100k non-whitespace chars (sensitivity) vs human pre n=20 median 3.47; Mann-Whitney p = 0.00018, Cliff's delta = +0.556.

**C3-size-sensitivity (second comparator, not confirmatory)**: AI unguardrailed n=66 median 7.83 S-class findings/100k non-whitespace chars (sensitivity) vs human post n=20 median 0.87; Mann-Whitney p = 0.00000, Cliff's delta = +0.758.

**G3** (naive - guardrailed): Delta_instructed +1.74, Delta_heldout +1.10, contrast +0.63, bootstrap 95% [0.15, 1.14] (secondary, pre-registered contrast).

**G3** (detailed-functional - guardrailed): Delta_instructed +2.01, Delta_heldout +0.71, contrast +1.30, bootstrap 95% [0.75, 1.84] (secondary, pre-registered contrast).

## Analysis over all rules (29 rules)

| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |
|---|---|---|---|---|---|---|---|
| naive | 33 | 32 | 0.970 | [0.847, 0.995] | 7.39 | 6.0 | [6.30, 8.58] |
| detailed-functional | 33 | 30 | 0.909 | [0.764, 0.969] | 7.45 | 8.0 | [6.73, 8.21] |
| guardrailed | 34 | 17 | 0.500 | [0.341, 0.659] | 4.79 | 5.0 | [4.24, 5.38] |

**C1** (naive - guardrailed, 9 strata brief x bundle): difference +0.470, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C2** (naive - guardrailed, 9 strata brief x bundle): difference +2.600, stratified permutation p = 0.00002 (100,000 perms, seed 20261121).

**C3**: AI unguardrailed n=66 median 2.72 S-class findings/kLOC vs human pre n=20 median 1.02; Mann-Whitney p = 0.00001, Cliff's delta = +0.647.

**C3 (second comparator, not confirmatory)**: AI unguardrailed n=66 median 2.72 S-class findings/kLOC vs human post n=20 median 0.38; Mann-Whitney p = 0.00000, Cliff's delta = +0.773.

**C3-size-sensitivity**: AI unguardrailed n=66 median 9.46 S-class findings/100k non-whitespace chars (sensitivity) vs human pre n=20 median 3.57; Mann-Whitney p = 0.00002, Cliff's delta = +0.627.

**C3-size-sensitivity (second comparator, not confirmatory)**: AI unguardrailed n=66 median 9.46 S-class findings/100k non-whitespace chars (sensitivity) vs human post n=20 median 1.37; Mann-Whitney p = 0.00000, Cliff's delta = +0.779.

**G3** (naive - guardrailed): Delta_instructed +1.57, Delta_heldout +0.85, contrast +0.72, bootstrap 95% [0.06, 1.39] (secondary, pre-registered contrast).

**G3** (detailed-functional - guardrailed): Delta_instructed +2.02, Delta_heldout +0.48, contrast +1.54, bootstrap 95% [0.95, 2.14] (secondary, pre-registered contrast).

## Prevalence over unguardrailed runs (PB rule: supported if Wilson lower >= 0.50, refuted if upper < 0.20)

| hypothesis | k/n | share | Wilson 95% | PB decision |
|---|---|---|---|---|
| H1 (D01 or D02) | 66/66 | 1.000 | [0.945, 1.000] | supported |
| H5 (U01 or U05) | 1/66 | 0.015 | [0.003, 0.081] | refuted |
| H6 (S05) | 10/66 | 0.152 | [0.084, 0.257] | inconclusive |

## Sensitivity analyses (protocol A6; none replaces a confirmatory endpoint)

| variant | n valid | C1 diff (p) | C2 diff (p) | C3 Cliff's delta (p) |
|---|---|---|---|---|
| (a) no web-tool attempt | 100 | +0.439 (p=0.0000) | +3.025 (p=0.0000) | +0.582 (p=0.0001) |
| (c) compile-excluded (build pass) | 42 | +0.000 (p=1.0000) | +3.829 (p=0.0028) | +0.678 (p=0.0000) |
| (b) only agy-g38flash | 28 | +0.200 (p=0.4286) | +7.244 (p=0.0001) | +0.456 (p=0.0172) |
| (b) only claude-haiku | 36 | +0.583 (p=0.0066) | +2.000 (p=0.0004) | +0.340 (p=0.0559) |
| (b) only codex-luna | 36 | +0.500 (p=0.0243) | +0.917 (p=0.0518) | +0.919 (p=0.0000) |
| (b) without agy-g38flash | 72 | +0.542 (p=0.0001) | +1.458 (p=0.0000) | +0.629 (p=0.0000) |
| (b) without claude-haiku | 64 | +0.361 (p=0.0060) | +3.632 (p=0.0000) | +0.720 (p=0.0000) |
| (b) without codex-luna | 64 | +0.407 (p=0.0017) | +4.216 (p=0.0000) | +0.389 (p=0.0141) |
