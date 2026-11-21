# E1 analysis (PRECISION PENDING: provisional, rules validated on recall only)

Runs: 13 generated, 13 valid. Unvalidated rules excluded from the primary analysis: none.

## Analysis over validated rules (29 rules)

| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |
|---|---|---|---|---|---|---|---|
| naive | 7 | 7 | 1.000 | [0.646, 1.000] | 8.00 | 7.0 | [5.71, 10.43] |
| detailed-functional | 4 | 4 | 1.000 | [0.510, 1.000] | 6.75 | 6.5 | [5.50, 8.25] |
| guardrailed | 2 | 1 | 0.500 | [0.095, 0.905] | 5.00 | 5.0 | [4.00, 6.00] |

**C1** (naive - guardrailed, 2 strata brief x bundle): difference +0.500, stratified permutation p = 1.00000 (100,000 perms, seed 20261121).

**C2** (naive - guardrailed, 2 strata brief x bundle): difference +5.000, stratified permutation p = 0.50048 (100,000 perms, seed 20261121).

**C3**: AI unguardrailed n=11 median 2.18 S-class findings/kLOC vs human pre n=20 median 1.02; Mann-Whitney p = 0.00604, Cliff's delta = +0.609.

**C3 (second comparator, not confirmatory)**: AI unguardrailed n=11 median 2.18 S-class findings/kLOC vs human post n=20 median 0.38; Mann-Whitney p = 0.00019, Cliff's delta = +0.827.

**C3-size-sensitivity**: AI unguardrailed n=11 median 7.40 S-class findings/100k non-whitespace chars (sensitivity) vs human pre n=20 median 3.57; Mann-Whitney p = 0.00685, Cliff's delta = +0.600.

**C3-size-sensitivity (second comparator, not confirmatory)**: AI unguardrailed n=11 median 7.40 S-class findings/100k non-whitespace chars (sensitivity) vs human post n=20 median 1.37; Mann-Whitney p = 0.00019, Cliff's delta = +0.827.

**G3** (naive - guardrailed): Delta_instructed +0.71, Delta_heldout +2.00, contrast -1.29, bootstrap 95% [-3.00, 0.29] (secondary, pre-registered contrast).

**G3** (detailed-functional - guardrailed): Delta_instructed +0.75, Delta_heldout +1.00, contrast -0.25, bootstrap 95% [-1.50, 1.00] (secondary, pre-registered contrast).

## Analysis over all rules (29 rules)

| style | n valid | runs with >=1 S-class | share | Wilson 95% | distinct rule IDs/run mean | median | bootstrap 95% (mean) |
|---|---|---|---|---|---|---|---|
| naive | 7 | 7 | 1.000 | [0.646, 1.000] | 8.00 | 7.0 | [5.71, 10.43] |
| detailed-functional | 4 | 4 | 1.000 | [0.510, 1.000] | 6.75 | 6.5 | [5.50, 8.25] |
| guardrailed | 2 | 1 | 0.500 | [0.095, 0.905] | 5.00 | 5.0 | [4.00, 6.00] |

**C1** (naive - guardrailed, 2 strata brief x bundle): difference +0.500, stratified permutation p = 1.00000 (100,000 perms, seed 20261121).

**C2** (naive - guardrailed, 2 strata brief x bundle): difference +5.000, stratified permutation p = 0.50048 (100,000 perms, seed 20261121).

**C3**: AI unguardrailed n=11 median 2.18 S-class findings/kLOC vs human pre n=20 median 1.02; Mann-Whitney p = 0.00604, Cliff's delta = +0.609.

**C3 (second comparator, not confirmatory)**: AI unguardrailed n=11 median 2.18 S-class findings/kLOC vs human post n=20 median 0.38; Mann-Whitney p = 0.00019, Cliff's delta = +0.827.

**C3-size-sensitivity**: AI unguardrailed n=11 median 7.40 S-class findings/100k non-whitespace chars (sensitivity) vs human pre n=20 median 3.57; Mann-Whitney p = 0.00685, Cliff's delta = +0.600.

**C3-size-sensitivity (second comparator, not confirmatory)**: AI unguardrailed n=11 median 7.40 S-class findings/100k non-whitespace chars (sensitivity) vs human post n=20 median 1.37; Mann-Whitney p = 0.00019, Cliff's delta = +0.827.

**G3** (naive - guardrailed): Delta_instructed +0.71, Delta_heldout +2.00, contrast -1.29, bootstrap 95% [-3.00, 0.29] (secondary, pre-registered contrast).

**G3** (detailed-functional - guardrailed): Delta_instructed +0.75, Delta_heldout +1.00, contrast -0.25, bootstrap 95% [-1.50, 1.00] (secondary, pre-registered contrast).

## Prevalence over unguardrailed runs (PB rule: supported if Wilson lower >= 0.50, refuted if upper < 0.20)

| hypothesis | k/n | share | Wilson 95% | PB decision |
|---|---|---|---|---|
| H1 (D01 or D02) | 11/11 | 1.000 | [0.741, 1.000] | supported |
| H5 (U01 or U05) | 4/11 | 0.364 | [0.152, 0.646] | inconclusive |
| H6 (S05) | 0/11 | 0.000 | [0.000, 0.259] | inconclusive |

## Sensitivity analyses (protocol A6; none replaces a confirmatory endpoint)

| variant | n valid | C1 diff (p) | C2 diff (p) | C3 Cliff's delta (p) |
|---|---|---|---|---|
| (a) no web-tool attempt | 13 | +0.500 (p=1.0000) | +5.000 (p=0.5005) | +0.609 (p=0.0060) |
| (c) compile-excluded (build pass) | 4 | n/a | n/a | +0.150 (p=0.6701) |
| (b) only agy-g38flash | 3 | +0.000 (p=1.0000) | +7.000 (p=1.0000) | -0.150 (p=0.7752) |
| (b) only claude-haiku | 5 | n/a | n/a | +0.600 (p=0.0451) |
| (b) only codex-luna | 5 | +1.000 (p=1.0000) | +3.000 (p=1.0000) | +1.000 (p=0.0022) |
| (b) without agy-g38flash | 10 | +1.000 (p=1.0000) | +3.000 (p=1.0000) | +0.778 (p=0.0011) |
| (b) without claude-haiku | 8 | +0.500 (p=1.0000) | +5.000 (p=0.5005) | +0.617 (p=0.0263) |
| (b) without codex-luna | 8 | +0.000 (p=1.0000) | +7.000 (p=1.0000) | +0.386 (p=0.1426) |
