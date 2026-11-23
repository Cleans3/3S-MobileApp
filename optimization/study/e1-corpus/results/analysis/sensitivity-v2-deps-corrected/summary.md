# E1 analysis, detector track: v2c

Runs: 101 generated, 100 valid. Unvalidated rules excluded from the primary analysis: ['P05', 'P06', 'P07', 'S04', 'S08', 'U05'].

## Analysis over validated rules (23 rules)

| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |
|---|---|---|---|---|---|---|---|
| naive | 33 | 31 | 0.939 | [0.804, 0.983] | 6.76 | 5.0 | [5.79, 7.79] |
| detailed-functional | 33 | 28 | 0.848 | [0.691, 0.933] | 6.39 | 6.0 | [5.82, 6.97] |
| guardrailed | 34 | 10 | 0.294 | [0.168, 0.462] | 2.56 | 2.0 | [2.21, 2.94] |

**C1** (naive - guardrailed, 9 strata brief x bundle): difference +0.645, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C2** (naive - guardrailed, 9 strata brief x bundle): difference +4.199, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C3**: AI unguardrailed n=66 median 2.33 S-class findings/kLOC vs human pre n=20 median 0.83; Mann-Whitney p = 0.00086, Cliff's delta = +0.495.

**C3 (second comparator, not confirmatory)**: AI unguardrailed n=66 median 2.33 S-class findings/kLOC vs human post n=20 median 0.28; Mann-Whitney p = 0.00001, Cliff's delta = +0.655.

**C3-size-sensitivity**: AI unguardrailed n=66 median 7.29 S-class findings/100k non-whitespace chars (sensitivity) vs human pre n=20 median 3.04; Mann-Whitney p = 0.00111, Cliff's delta = +0.484.

**C3-size-sensitivity (second comparator, not confirmatory)**: AI unguardrailed n=66 median 7.29 S-class findings/100k non-whitespace chars (sensitivity) vs human post n=20 median 1.04; Mann-Whitney p = 0.00001, Cliff's delta = +0.664.

**G3** (naive - guardrailed): Delta_instructed +2.91, Delta_heldout +1.10, contrast +1.81, bootstrap 95% [1.35, 2.28] (secondary, pre-registered contrast).

**G3** (detailed-functional - guardrailed): Delta_instructed +2.97, Delta_heldout +0.71, contrast +2.26, bootstrap 95% [1.78, 2.72] (secondary, pre-registered contrast).

## Analysis over all rules (29 rules)

| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |
|---|---|---|---|---|---|---|---|
| naive | 33 | 32 | 0.970 | [0.847, 0.995] | 7.21 | 5.0 | [6.12, 8.39] |
| detailed-functional | 33 | 29 | 0.879 | [0.727, 0.952] | 7.09 | 7.0 | [6.39, 7.79] |
| guardrailed | 34 | 11 | 0.324 | [0.191, 0.492] | 3.41 | 3.0 | [2.94, 3.94] |

**C1** (naive - guardrailed, 9 strata brief x bundle): difference +0.646, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C2** (naive - guardrailed, 9 strata brief x bundle): difference +3.800, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C3**: AI unguardrailed n=66 median 2.72 S-class findings/kLOC vs human pre n=20 median 0.83; Mann-Whitney p = 0.00013, Cliff's delta = +0.569.

**C3 (second comparator, not confirmatory)**: AI unguardrailed n=66 median 2.72 S-class findings/kLOC vs human post n=20 median 0.33; Mann-Whitney p = 0.00000, Cliff's delta = +0.724.

**C3-size-sensitivity**: AI unguardrailed n=66 median 8.85 S-class findings/100k non-whitespace chars (sensitivity) vs human pre n=20 median 3.04; Mann-Whitney p = 0.00018, Cliff's delta = +0.555.

**C3-size-sensitivity (second comparator, not confirmatory)**: AI unguardrailed n=66 median 8.85 S-class findings/100k non-whitespace chars (sensitivity) vs human post n=20 median 1.22; Mann-Whitney p = 0.00000, Cliff's delta = +0.730.

**G3** (naive - guardrailed): Delta_instructed +2.77, Delta_heldout +0.85, contrast +1.93, bootstrap 95% [1.33, 2.53] (secondary, pre-registered contrast).

**G3** (detailed-functional - guardrailed): Delta_instructed +3.04, Delta_heldout +0.48, contrast +2.56, bootstrap 95% [2.03, 3.11] (secondary, pre-registered contrast).

## Prevalence over unguardrailed runs (PB rule: supported if Wilson lower >= 0.50, refuted if upper < 0.20)

| hypothesis | k/n | share | Wilson 95% | PB decision |
|---|---|---|---|---|
| H1 (D01 or D02) | 66/66 | 1.000 | [0.945, 1.000] | supported |
| H5 (U01 or U05) | 1/66 | 0.015 | [0.003, 0.081] | refuted |
| H6 (S05) | 10/66 | 0.152 | [0.084, 0.257] | inconclusive |

## Sensitivity analyses (protocol A6; none replaces a confirmatory endpoint)

| variant | n valid | C1 diff (p) | C2 diff (p) | C3 Cliff's delta (p) |
|---|---|---|---|---|
| (a) no web-tool attempt | 100 | +0.645 (p=0.0000) | +4.199 (p=0.0000) | +0.495 (p=0.0009) |
| (c) compile-excluded (build pass) | 42 | +0.000 (p=1.0000) | +4.886 (p=0.0004) | +0.584 (p=0.0005) |
| (b) only agy-g38flash | 28 | +0.500 (p=0.0288) | +8.644 (p=0.0001) | +0.278 (p=0.1479) |
| (b) only claude-haiku | 36 | +0.750 (p=0.0010) | +3.000 (p=0.0000) | +0.233 (p=0.1899) |
| (b) only codex-luna | 36 | +0.667 (p=0.0009) | +2.083 (p=0.0001) | +0.919 (p=0.0000) |
| (b) without agy-g38flash | 72 | +0.708 (p=0.0000) | +2.542 (p=0.0000) | +0.576 (p=0.0002) |
| (b) without claude-haiku | 64 | +0.589 (p=0.0000) | +4.905 (p=0.0000) | +0.644 (p=0.0000) |
| (b) without codex-luna | 64 | +0.634 (p=0.0000) | +5.392 (p=0.0000) | +0.252 (p=0.1119) |
