# E1 analysis, detector track: v2

Runs: 101 generated, 100 valid. Unvalidated rules excluded from the primary analysis: ['P05', 'P06', 'P07', 'S04', 'S08', 'U05'].

## Analysis over validated rules (23 rules)

| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |
|---|---|---|---|---|---|---|---|
| naive | 33 | 31 | 0.939 | [0.804, 0.983] | 6.79 | 5.0 | [5.79, 7.85] |
| detailed-functional | 33 | 29 | 0.879 | [0.727, 0.952] | 6.73 | 7.0 | [6.09, 7.36] |
| guardrailed | 34 | 17 | 0.500 | [0.341, 0.659] | 2.82 | 3.0 | [2.41, 3.26] |

**C1** (naive - guardrailed, 9 strata brief x bundle): difference +0.439, stratified permutation p = 0.00004 (100,000 perms, seed 20261121).

**C2** (naive - guardrailed, 9 strata brief x bundle): difference +3.964, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C3**: AI unguardrailed n=66 median 2.37 S-class findings/kLOC vs human pre n=20 median 1.02; Mann-Whitney p = 0.00013, Cliff's delta = +0.567.

**C3 (second comparator, not confirmatory)**: AI unguardrailed n=66 median 2.37 S-class findings/kLOC vs human post n=20 median 0.38; Mann-Whitney p = 0.00000, Cliff's delta = +0.695.

**C3-size-sensitivity**: AI unguardrailed n=66 median 7.83 S-class findings/100k non-whitespace chars (sensitivity) vs human pre n=20 median 3.57; Mann-Whitney p = 0.00024, Cliff's delta = +0.545.

**C3-size-sensitivity (second comparator, not confirmatory)**: AI unguardrailed n=66 median 7.83 S-class findings/100k non-whitespace chars (sensitivity) vs human post n=20 median 1.37; Mann-Whitney p = 0.00000, Cliff's delta = +0.706.

**G3** (naive - guardrailed): Delta_instructed +2.68, Delta_heldout +1.10, contrast +1.57, bootstrap 95% [1.06, 2.09] (secondary, pre-registered contrast).

**G3** (detailed-functional - guardrailed): Delta_instructed +3.04, Delta_heldout +0.71, contrast +2.33, bootstrap 95% [1.79, 2.87] (secondary, pre-registered contrast).

## Analysis over all rules (29 rules)

| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |
|---|---|---|---|---|---|---|---|
| naive | 33 | 32 | 0.970 | [0.847, 0.995] | 7.24 | 5.0 | [6.15, 8.42] |
| detailed-functional | 33 | 30 | 0.909 | [0.764, 0.969] | 7.42 | 8.0 | [6.67, 8.18] |
| guardrailed | 34 | 17 | 0.500 | [0.341, 0.659] | 3.68 | 3.0 | [3.15, 4.26] |

**C1** (naive - guardrailed, 9 strata brief x bundle): difference +0.470, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C2** (naive - guardrailed, 9 strata brief x bundle): difference +3.566, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C3**: AI unguardrailed n=66 median 2.72 S-class findings/kLOC vs human pre n=20 median 1.02; Mann-Whitney p = 0.00001, Cliff's delta = +0.647.

**C3 (second comparator, not confirmatory)**: AI unguardrailed n=66 median 2.72 S-class findings/kLOC vs human post n=20 median 0.38; Mann-Whitney p = 0.00000, Cliff's delta = +0.773.

**C3-size-sensitivity**: AI unguardrailed n=66 median 9.46 S-class findings/100k non-whitespace chars (sensitivity) vs human pre n=20 median 3.57; Mann-Whitney p = 0.00002, Cliff's delta = +0.627.

**C3-size-sensitivity (second comparator, not confirmatory)**: AI unguardrailed n=66 median 9.46 S-class findings/100k non-whitespace chars (sensitivity) vs human post n=20 median 1.37; Mann-Whitney p = 0.00000, Cliff's delta = +0.779.

**G3** (naive - guardrailed): Delta_instructed +2.54, Delta_heldout +0.85, contrast +1.69, bootstrap 95% [1.06, 2.32] (secondary, pre-registered contrast).

**G3** (detailed-functional - guardrailed): Delta_instructed +3.11, Delta_heldout +0.48, contrast +2.63, bootstrap 95% [2.06, 3.20] (secondary, pre-registered contrast).

## Prevalence over unguardrailed runs (PB rule: supported if Wilson lower >= 0.50, refuted if upper < 0.20)

| hypothesis | k/n | share | Wilson 95% | PB decision |
|---|---|---|---|---|
| H1 (D01 or D02) | 66/66 | 1.000 | [0.945, 1.000] | supported |
| H5 (U01 or U05) | 1/66 | 0.015 | [0.003, 0.081] | refuted |
| H6 (S05) | 10/66 | 0.152 | [0.084, 0.257] | inconclusive |

## Sensitivity analyses (protocol A6; none replaces a confirmatory endpoint)

| variant | n valid | C1 diff (p) | C2 diff (p) | C3 Cliff's delta (p) |
|---|---|---|---|---|
| (a) no web-tool attempt | 100 | +0.439 (p=0.0000) | +3.964 (p=0.0000) | +0.567 (p=0.0001) |
| (c) compile-excluded (build pass) | 42 | +0.000 (p=1.0000) | +4.757 (p=0.0010) | +0.659 (p=0.0001) |
| (b) only agy-g38flash | 28 | +0.200 (p=0.4286) | +8.356 (p=0.0001) | +0.433 (p=0.0235) |
| (b) only claude-haiku | 36 | +0.583 (p=0.0066) | +2.750 (p=0.0001) | +0.315 (p=0.0767) |
| (b) only codex-luna | 36 | +0.500 (p=0.0243) | +1.917 (p=0.0005) | +0.919 (p=0.0000) |
| (b) without agy-g38flash | 72 | +0.542 (p=0.0001) | +2.333 (p=0.0000) | +0.617 (p=0.0001) |
| (b) without claude-haiku | 64 | +0.361 (p=0.0060) | +4.680 (p=0.0000) | +0.711 (p=0.0000) |
| (b) without codex-luna | 64 | +0.407 (p=0.0017) | +5.121 (p=0.0000) | +0.365 (p=0.0212) |
