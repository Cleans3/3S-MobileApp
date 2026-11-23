# E1 analysis, detector track: v1c

Runs: 101 generated, 100 valid. Unvalidated rules excluded from the primary analysis: ['P05', 'P06', 'P07', 'P09', 'P10', 'S03', 'S04', 'S08', 'U05'].

## Analysis over validated rules (20 rules)

| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |
|---|---|---|---|---|---|---|---|
| naive | 33 | 31 | 0.939 | [0.804, 0.983] | 5.82 | 4.0 | [4.88, 6.82] |
| detailed-functional | 33 | 28 | 0.848 | [0.691, 0.933] | 5.36 | 5.0 | [4.79, 5.94] |
| guardrailed | 34 | 10 | 0.294 | [0.168, 0.462] | 2.56 | 2.0 | [2.21, 2.94] |

**C1** (naive - guardrailed, 9 strata brief x bundle): difference +0.645, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C2** (naive - guardrailed, 9 strata brief x bundle): difference +3.259, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C3**: AI unguardrailed n=66 median 2.33 S-class findings/kLOC vs human pre n=20 median 0.83; Mann-Whitney p = 0.00055, Cliff's delta = +0.513.

**C3 (second comparator, not confirmatory)**: AI unguardrailed n=66 median 2.33 S-class findings/kLOC vs human post n=20 median 0.19; Mann-Whitney p = 0.00000, Cliff's delta = +0.712.

**C3-size-sensitivity**: AI unguardrailed n=66 median 7.29 S-class findings/100k non-whitespace chars (sensitivity) vs human pre n=20 median 3.04; Mann-Whitney p = 0.00083, Cliff's delta = +0.496.

**C3-size-sensitivity (second comparator, not confirmatory)**: AI unguardrailed n=66 median 7.29 S-class findings/100k non-whitespace chars (sensitivity) vs human post n=20 median 0.68; Mann-Whitney p = 0.00000, Cliff's delta = +0.725.

**G3** (naive - guardrailed): Delta_instructed +1.97, Delta_heldout +1.10, contrast +0.87, bootstrap 95% [0.44, 1.32] (secondary, pre-registered contrast).

**G3** (detailed-functional - guardrailed): Delta_instructed +1.94, Delta_heldout +0.71, contrast +1.23, bootstrap 95% [0.75, 1.71] (secondary, pre-registered contrast).

## Analysis over all rules (29 rules)

| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |
|---|---|---|---|---|---|---|---|
| naive | 33 | 32 | 0.970 | [0.847, 0.995] | 7.36 | 6.0 | [6.27, 8.52] |
| detailed-functional | 33 | 29 | 0.879 | [0.727, 0.952] | 7.12 | 7.0 | [6.45, 7.82] |
| guardrailed | 34 | 11 | 0.324 | [0.191, 0.492] | 4.53 | 4.5 | [4.03, 5.03] |

**C1** (naive - guardrailed, 9 strata brief x bundle): difference +0.646, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C2** (naive - guardrailed, 9 strata brief x bundle): difference +2.834, stratified permutation p = 0.00001 (100,000 perms, seed 20261121).

**C3**: AI unguardrailed n=66 median 2.72 S-class findings/kLOC vs human pre n=20 median 0.83; Mann-Whitney p = 0.00013, Cliff's delta = +0.569.

**C3 (second comparator, not confirmatory)**: AI unguardrailed n=66 median 2.72 S-class findings/kLOC vs human post n=20 median 0.33; Mann-Whitney p = 0.00000, Cliff's delta = +0.724.

**C3-size-sensitivity**: AI unguardrailed n=66 median 8.85 S-class findings/100k non-whitespace chars (sensitivity) vs human pre n=20 median 3.04; Mann-Whitney p = 0.00018, Cliff's delta = +0.555.

**C3-size-sensitivity (second comparator, not confirmatory)**: AI unguardrailed n=66 median 8.85 S-class findings/100k non-whitespace chars (sensitivity) vs human post n=20 median 1.22; Mann-Whitney p = 0.00000, Cliff's delta = +0.730.

**G3** (naive - guardrailed): Delta_instructed +1.80, Delta_heldout +0.85, contrast +0.96, bootstrap 95% [0.33, 1.62] (secondary, pre-registered contrast).

**G3** (detailed-functional - guardrailed): Delta_instructed +1.96, Delta_heldout +0.48, contrast +1.47, bootstrap 95% [0.93, 2.04] (secondary, pre-registered contrast).

## Prevalence over unguardrailed runs (PB rule: supported if Wilson lower >= 0.50, refuted if upper < 0.20)

| hypothesis | k/n | share | Wilson 95% | PB decision |
|---|---|---|---|---|
| H1 (D01 or D02) | 66/66 | 1.000 | [0.945, 1.000] | supported |
| H5 (U01 or U05) | 1/66 | 0.015 | [0.003, 0.081] | refuted |
| H6 (S05) | 10/66 | 0.152 | [0.084, 0.257] | inconclusive |

## Sensitivity analyses (protocol A6; none replaces a confirmatory endpoint)

| variant | n valid | C1 diff (p) | C2 diff (p) | C3 Cliff's delta (p) |
|---|---|---|---|---|
| (a) no web-tool attempt | 100 | +0.645 (p=0.0000) | +3.259 (p=0.0000) | +0.513 (p=0.0005) |
| (c) compile-excluded (build pass) | 42 | +0.000 (p=1.0000) | +3.957 (p=0.0012) | +0.613 (p=0.0002) |
| (b) only agy-g38flash | 28 | +0.500 (p=0.0288) | +7.533 (p=0.0001) | +0.311 (p=0.1047) |
| (b) only claude-haiku | 36 | +0.750 (p=0.0010) | +2.250 (p=0.0001) | +0.258 (p=0.1464) |
| (b) only codex-luna | 36 | +0.667 (p=0.0009) | +1.083 (p=0.0098) | +0.919 (p=0.0000) |
| (b) without agy-g38flash | 72 | +0.708 (p=0.0000) | +1.667 (p=0.0000) | +0.589 (p=0.0001) |
| (b) without claude-haiku | 64 | +0.589 (p=0.0000) | +3.857 (p=0.0000) | +0.658 (p=0.0000) |
| (b) without codex-luna | 64 | +0.634 (p=0.0000) | +4.487 (p=0.0000) | +0.281 (p=0.0766) |
