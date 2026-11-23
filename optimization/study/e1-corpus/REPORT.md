# E1: AI-generated Android apps, prevalence of taxonomy problems

Generated 2026-11-23 from results/*.csv by harness/report.py. Frozen inputs: protocol sha256 4bbbb97ef311, guardrail sha256 ae5639033f70. Scope: first-draft, no-retrieval (protocol section 1): one brief in, one project out, no network, no reviewer, no detector feedback.

## Read this first

1. Only 42 of 100 valid runs build (pass after at most 3 logged table repairs; 34 pass zero-touch). Per bundle: claude-haiku 13/36; codex-luna 10/36; agy-g38flash 19/28. Every prevalence number below is source-level and covers all valid runs, including apps that do not compile.

2. Among the 42 apps that build (compile-excluded sensitivity, protocol A6, not a confirmatory endpoint), C1 (share of runs with at least one S-class finding, naive minus guardrailed) is +0.000 (p=1.0000) on the primary track and +0.000 (p=1.0000) on the v2 track: the guardrail effect on S-class presence does not hold among apps that build. The count and density effects persist among builders (C2 +3.829 (p=0.0028) primary, +4.757 (p=0.0010) v2; C3 Cliff's delta +0.678 (p=0.0000) primary, +0.659 (p=0.0001) v2), so the guardrail reduces how many distinct S-class problems appear, not whether any appear in a working app.

3. Dependency lookup defect, found by the H4 check: 21 of the 22 hallucinated-version instances in coordinates.csv were false (a stale google() KSP plugin marker was checked first). H4 itself is unaffected (0 hallucinated artifacts in 2105 instances: refuted). The defect cuts both ways (it inflated the guardrailed S-class share and the S-class density of AI and human apps alike); the depfix tracks (3c, 3d, side by side in 3e) are a post-hoc sensitivity analysis disclosed in section 7; the primary analysis stays as pre-registered and run.

## 1. Corpus actually generated

| bundle | style | generated | valid | invalid reasons | median kotlin LOC | median code kchars (non-ws) | zero-touch build pass | pass after <=3 table repairs |
|---|---|---|---|---|---|---|---|---|
| claude-haiku | naive | 12 | 12 | - | 511 | 15.0 | 5/12 = 0.42 [0.19, 0.68] | 8/12 = 0.67 [0.39, 0.86] |
| claude-haiku | detailed-functional | 12 | 12 | - | 922 | 27.1 | 3/12 = 0.25 [0.09, 0.53] | 3/12 = 0.25 [0.09, 0.53] |
| claude-haiku | guardrailed | 12 | 12 | - | 1138 | 32.9 | 2/12 = 0.17 [0.05, 0.45] | 2/12 = 0.17 [0.05, 0.45] |
| codex-luna | naive | 12 | 12 | - | 153 | 6.3 | 3/12 = 0.25 [0.09, 0.53] | 5/12 = 0.42 [0.19, 0.68] |
| codex-luna | detailed-functional | 12 | 12 | - | 157 | 12.4 | 0/12 = 0.00 [0.00, 0.24] | 2/12 = 0.17 [0.05, 0.45] |
| codex-luna | guardrailed | 12 | 12 | - | 264 | 14.8 | 3/12 = 0.25 [0.09, 0.53] | 3/12 = 0.25 [0.09, 0.53] |
| agy-g38flash | naive | 10 | 9 | harness-limited: agy headless denied a command before any file was written x1 | 1718 | 43.6 | 7/9 = 0.78 [0.45, 0.94] | 7/9 = 0.78 [0.45, 0.94] |
| agy-g38flash | detailed-functional | 9 | 9 | - | 1880 | 49.8 | 7/9 = 0.78 [0.45, 0.94] | 7/9 = 0.78 [0.45, 0.94] |
| agy-g38flash | guardrailed | 10 | 10 | - | 1686 | 44.8 | 4/10 = 0.40 [0.17, 0.69] | 5/10 = 0.50 [0.24, 0.76] |

Total 101 generated, 100 valid. Valid = at least one .kt file and a Gradle build file (protocol section 7); a run without them is reported with its reason and kept in the all-runs denominator. Pilots (6) are excluded from every number here.

Runs where the agent's headless mode auto-denied at least one shell command: 1 (agy only; the first denied command ends the whole run, so such a run may be truncated at its end, or empty if it came first).

### Generator capability matrix (protocol A6)

| bundle | what the generator could do | runs | median shell commands/run | shell commands total | runs with a denied command | web-tool attempts | web_denied | runs with a network/build command |
|---|---|---|---|---|---|---|---|---|
| claude-haiku | file tools only (Read, Edit, Write, Glob, Grep); no shell, no web tool | 36 | 0.0 | 0 | 0 | 0 | na x36 | 0 |
| codex-luna | shell, sandbox off, Landlock write-jail, network not blocked by the jail | 36 | 4.0 | 200 | 0 | 0 | na x36 | 1 |
| agy-g38flash | shell allow-list (shared read-only set + E1 housekeeping and JDK probes), headless ends the run at the first denied command; web tools denied by hook from the switch-over (web_denied = y) | 29 | 6 | 212 | 1 | 0 | n x9; y x20 | 0 |

Containment audit (harness/audit_outside.py, results/outside_access.csv): reads are not jailed, so every absolute path in a tool call is checked. 4 of 107 runs reference a path outside their own directory: agy-g38flash-B1-naive-r2 (/mnt/01DC198BE9192500/github | /mnt/01DC198BE9192500/github/Schooldev/3S-MobileApp/optimiz); claude-haiku-B3-naive-r1 (/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/loans/src/main/java/com/example/libra); codex-luna-B2-naive-r1 (/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/.gitignore); codex-luna-B3-naive-r3 (/root/.gradle/caches/modules-2/files-2.1/com.android.tools.build/gradle | /root/.gradle/ca). A denied write into the parent directory is not a read; a run that reads another run's output is invalid and listed.

No run built or ran the app (net_audit counts executed build, install and network commands): the claude launcher has no shell and the agy allow-list denies build tools, while codex had an open shell and was only audited. Web-tool attempts are counted from tool invocations in the transcripts (attempts a hook denied still count).

## 2. Prevalence per rule and arm (share of valid runs with at least one finding; n per arm in the header)

### 2a. PRIMARY: frozen v1 detector (alpha A13)

| rule | naive (n=33) | detailed-functional (n=33) | guardrailed (n=34) | human pre-2022 (n=20) | human 2024+ (n=20) | held-out | status |
|---|---|---|---|---|---|---|---|
| D01 | 25/33 = 0.76 [0.59, 0.87] | 22/33 = 0.67 [0.50, 0.80] | 25/34 = 0.74 [0.57, 0.85] | 20/20 = 1.00 [0.84, 1.00] | 14/20 = 0.70 [0.48, 0.85] |  | validated |
| D02 | 33/33 = 1.00 [0.90, 1.00] | 33/33 = 1.00 [0.90, 1.00] | 34/34 = 1.00 [0.90, 1.00] | 17/20 = 0.85 [0.64, 0.95] | 18/20 = 0.90 [0.70, 0.97] | yes | pending |
| P01 | 6/33 = 0.18 [0.09, 0.34] | 8/33 = 0.24 [0.13, 0.41] | 0/34 = 0.00 [0.00, 0.10] | 14/20 = 0.70 [0.48, 0.85] | 12/20 = 0.60 [0.39, 0.78] |  | validated |
| P02 | 12/33 = 0.36 [0.22, 0.53] | 7/33 = 0.21 [0.11, 0.38] | 4/34 = 0.12 [0.05, 0.27] | 11/20 = 0.55 [0.34, 0.74] | 14/20 = 0.70 [0.48, 0.85] |  | validated |
| P03 | 5/33 = 0.15 [0.07, 0.31] | 1/33 = 0.03 [0.01, 0.15] | 3/34 = 0.09 [0.03, 0.23] | 15/20 = 0.75 [0.53, 0.89] | 16/20 = 0.80 [0.58, 0.92] | yes | validated |
| P04 | 3/33 = 0.09 [0.03, 0.24] | 2/33 = 0.06 [0.02, 0.20] | 0/34 = 0.00 [0.00, 0.10] | 6/20 = 0.30 [0.15, 0.52] | 10/20 = 0.50 [0.30, 0.70] |  | validated |
| P05 | 0/33 = 0.00 [0.00, 0.10] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 1/20 = 0.05 [0.01, 0.24] | 5/20 = 0.25 [0.11, 0.47] | yes | unvalidated-precision |
| P06 | 0/33 = 0.00 [0.00, 0.10] | 5/33 = 0.15 [0.07, 0.31] | 3/34 = 0.09 [0.03, 0.23] | 9/20 = 0.45 [0.26, 0.66] | 9/20 = 0.45 [0.26, 0.66] |  | unvalidated-precision |
| P07 | 5/33 = 0.15 [0.07, 0.31] | 8/33 = 0.24 [0.13, 0.41] | 12/34 = 0.35 [0.21, 0.52] | 7/20 = 0.35 [0.18, 0.57] | 13/20 = 0.65 [0.43, 0.82] |  | unvalidated-precision |
| P08 | 8/33 = 0.24 [0.13, 0.41] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 8/20 = 0.40 [0.22, 0.61] | 5/20 = 0.25 [0.11, 0.47] | yes | validated |
| P09 | 1/33 = 0.03 [0.01, 0.15] | 1/33 = 0.03 [0.01, 0.15] | 0/34 = 0.00 [0.00, 0.10] | 6/20 = 0.30 [0.15, 0.52] | 12/20 = 0.60 [0.39, 0.78] |  | unvalidated-precision |
| P10 | 33/33 = 1.00 [0.90, 1.00] | 33/33 = 1.00 [0.90, 1.00] | 34/34 = 1.00 [0.90, 1.00] | 18/20 = 0.90 [0.70, 0.97] | 20/20 = 1.00 [0.84, 1.00] |  | unvalidated-precision |
| P11 | 33/33 = 1.00 [0.90, 1.00] | 33/33 = 1.00 [0.90, 1.00] | 7/34 = 0.21 [0.10, 0.37] | 20/20 = 1.00 [0.84, 1.00] | 20/20 = 1.00 [0.84, 1.00] |  | validated |
| P13 | 7/33 = 0.21 [0.11, 0.38] | 8/33 = 0.24 [0.13, 0.41] | 0/34 = 0.00 [0.00, 0.10] | 6/20 = 0.30 [0.15, 0.52] | 8/20 = 0.40 [0.22, 0.61] |  | validated |
| S01 | 8/33 = 0.24 [0.13, 0.41] | 11/33 = 0.33 [0.20, 0.50] | 0/34 = 0.00 [0.00, 0.10] | 2/20 = 0.10 [0.03, 0.30] | 3/20 = 0.15 [0.05, 0.36] |  | validated |
| S02 | 2/33 = 0.06 [0.02, 0.20] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 2/20 = 0.10 [0.03, 0.30] | 1/20 = 0.05 [0.01, 0.24] |  | validated |
| S03 | 0/33 = 0.00 [0.00, 0.10] | 0/33 = 0.00 [0.00, 0.10] | 1/34 = 0.03 [0.01, 0.15] | 4/20 = 0.20 [0.08, 0.42] | 6/20 = 0.30 [0.15, 0.52] |  | unvalidated-precision |
| S04 | 0/33 = 0.00 [0.00, 0.10] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 0/20 = 0.00 [0.00, 0.16] | 1/20 = 0.05 [0.01, 0.24] |  | unvalidated-precision |
| S05 | 4/33 = 0.12 [0.05, 0.27] | 6/33 = 0.18 [0.09, 0.34] | 0/34 = 0.00 [0.00, 0.10] | 6/20 = 0.30 [0.15, 0.52] | 3/20 = 0.15 [0.05, 0.36] |  | validated |
| S06 | 29/33 = 0.88 [0.73, 0.95] | 24/33 = 0.73 [0.56, 0.85] | 10/34 = 0.29 [0.17, 0.46] | 11/20 = 0.55 [0.34, 0.74] | 8/20 = 0.40 [0.22, 0.61] | yes | validated |
| S07 | 0/33 = 0.00 [0.00, 0.10] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 2/20 = 0.10 [0.03, 0.30] | 6/20 = 0.30 [0.15, 0.52] |  | validated |
| S08 | 5/33 = 0.15 [0.07, 0.31] | 3/33 = 0.09 [0.03, 0.24] | 1/34 = 0.03 [0.01, 0.15] | 1/20 = 0.05 [0.01, 0.24] | 2/20 = 0.10 [0.03, 0.30] |  | unvalidated-precision |
| S09 | 0/33 = 0.00 [0.00, 0.10] | 1/33 = 0.03 [0.01, 0.15] | 0/34 = 0.00 [0.00, 0.10] | 2/20 = 0.10 [0.03, 0.30] | 1/20 = 0.05 [0.01, 0.24] |  | validated |
| S11 | 1/33 = 0.03 [0.01, 0.15] | 12/33 = 0.36 [0.22, 0.53] | 9/34 = 0.26 [0.15, 0.43] | 18/20 = 0.90 [0.70, 0.97] | 13/20 = 0.65 [0.43, 0.82] |  | pending |
| U01 | 0/33 = 0.00 [0.00, 0.10] | 1/33 = 0.03 [0.01, 0.15] | 0/34 = 0.00 [0.00, 0.10] | 9/20 = 0.45 [0.26, 0.66] | 6/20 = 0.30 [0.15, 0.52] |  | validated |
| U02 | 0/33 = 0.00 [0.00, 0.10] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 4/20 = 0.20 [0.08, 0.42] | 7/20 = 0.35 [0.18, 0.57] |  | validated |
| U03 | 8/33 = 0.24 [0.13, 0.41] | 6/33 = 0.18 [0.09, 0.34] | 2/34 = 0.06 [0.02, 0.19] | 6/20 = 0.30 [0.15, 0.52] | 7/20 = 0.35 [0.18, 0.57] |  | validated |
| U04 | 9/33 = 0.27 [0.15, 0.44] | 13/33 = 0.39 [0.25, 0.56] | 2/34 = 0.06 [0.02, 0.19] | 19/20 = 0.95 [0.76, 0.99] | 17/20 = 0.85 [0.64, 0.95] | yes | validated |
| U05 | 7/33 = 0.21 [0.11, 0.38] | 8/33 = 0.24 [0.13, 0.41] | 16/34 = 0.47 [0.31, 0.63] | 12/20 = 0.60 [0.39, 0.78] | 14/20 = 0.70 [0.48, 0.85] | yes | unvalidated-precision |

### 2b. SENSITIVITY: v2 detector (five false-positive bugs fixed, validation/fixes-v2.md)

| rule | naive (n=33) | detailed-functional (n=33) | guardrailed (n=34) | human pre-2022 (n=20) | human 2024+ (n=20) | held-out | status |
|---|---|---|---|---|---|---|---|
| D01 | 25/33 = 0.76 [0.59, 0.87] | 22/33 = 0.67 [0.50, 0.80] | 25/34 = 0.74 [0.57, 0.85] | 20/20 = 1.00 [0.84, 1.00] | 14/20 = 0.70 [0.48, 0.85] |  | validated |
| D02 | 33/33 = 1.00 [0.90, 1.00] | 33/33 = 1.00 [0.90, 1.00] | 34/34 = 1.00 [0.90, 1.00] | 17/20 = 0.85 [0.64, 0.95] | 18/20 = 0.90 [0.70, 0.97] | yes | pending |
| P01 | 6/33 = 0.18 [0.09, 0.34] | 8/33 = 0.24 [0.13, 0.41] | 0/34 = 0.00 [0.00, 0.10] | 14/20 = 0.70 [0.48, 0.85] | 12/20 = 0.60 [0.39, 0.78] |  | validated |
| P02 | 12/33 = 0.36 [0.22, 0.53] | 7/33 = 0.21 [0.11, 0.38] | 4/34 = 0.12 [0.05, 0.27] | 11/20 = 0.55 [0.34, 0.74] | 14/20 = 0.70 [0.48, 0.85] |  | validated |
| P03 | 5/33 = 0.15 [0.07, 0.31] | 1/33 = 0.03 [0.01, 0.15] | 3/34 = 0.09 [0.03, 0.23] | 15/20 = 0.75 [0.53, 0.89] | 16/20 = 0.80 [0.58, 0.92] | yes | validated |
| P04 | 3/33 = 0.09 [0.03, 0.24] | 2/33 = 0.06 [0.02, 0.20] | 0/34 = 0.00 [0.00, 0.10] | 6/20 = 0.30 [0.15, 0.52] | 10/20 = 0.50 [0.30, 0.70] |  | validated |
| P05 | 0/33 = 0.00 [0.00, 0.10] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 1/20 = 0.05 [0.01, 0.24] | 2/20 = 0.10 [0.03, 0.30] | yes | unvalidated-precision |
| P06 | 0/33 = 0.00 [0.00, 0.10] | 5/33 = 0.15 [0.07, 0.31] | 3/34 = 0.09 [0.03, 0.23] | 9/20 = 0.45 [0.26, 0.66] | 9/20 = 0.45 [0.26, 0.66] |  | unvalidated-precision |
| P07 | 3/33 = 0.09 [0.03, 0.24] | 7/33 = 0.21 [0.11, 0.38] | 9/34 = 0.26 [0.15, 0.43] | 7/20 = 0.35 [0.18, 0.57] | 10/20 = 0.50 [0.30, 0.70] |  | unvalidated-precision |
| P08 | 8/33 = 0.24 [0.13, 0.41] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 8/20 = 0.40 [0.22, 0.61] | 5/20 = 0.25 [0.11, 0.47] | yes | validated |
| P09 | 1/33 = 0.03 [0.01, 0.15] | 1/33 = 0.03 [0.01, 0.15] | 0/34 = 0.00 [0.00, 0.10] | 6/20 = 0.30 [0.15, 0.52] | 12/20 = 0.60 [0.39, 0.78] |  | validated |
| P10 | 30/33 = 0.91 [0.76, 0.97] | 33/33 = 1.00 [0.90, 1.00] | 0/34 = 0.00 [0.00, 0.10] | 16/20 = 0.80 [0.58, 0.92] | 11/20 = 0.55 [0.34, 0.74] |  | validated |
| P11 | 33/33 = 1.00 [0.90, 1.00] | 33/33 = 1.00 [0.90, 1.00] | 7/34 = 0.21 [0.10, 0.37] | 18/20 = 0.90 [0.70, 0.97] | 20/20 = 1.00 [0.84, 1.00] |  | validated |
| P13 | 7/33 = 0.21 [0.11, 0.38] | 8/33 = 0.24 [0.13, 0.41] | 0/34 = 0.00 [0.00, 0.10] | 6/20 = 0.30 [0.15, 0.52] | 8/20 = 0.40 [0.22, 0.61] |  | validated |
| S01 | 8/33 = 0.24 [0.13, 0.41] | 11/33 = 0.33 [0.20, 0.50] | 0/34 = 0.00 [0.00, 0.10] | 2/20 = 0.10 [0.03, 0.30] | 3/20 = 0.15 [0.05, 0.36] |  | validated |
| S02 | 2/33 = 0.06 [0.02, 0.20] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 2/20 = 0.10 [0.03, 0.30] | 1/20 = 0.05 [0.01, 0.24] |  | validated |
| S03 | 0/33 = 0.00 [0.00, 0.10] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 4/20 = 0.20 [0.08, 0.42] | 5/20 = 0.25 [0.11, 0.47] |  | validated |
| S04 | 0/33 = 0.00 [0.00, 0.10] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 0/20 = 0.00 [0.00, 0.16] | 1/20 = 0.05 [0.01, 0.24] |  | unvalidated-precision |
| S05 | 4/33 = 0.12 [0.05, 0.27] | 6/33 = 0.18 [0.09, 0.34] | 0/34 = 0.00 [0.00, 0.10] | 6/20 = 0.30 [0.15, 0.52] | 3/20 = 0.15 [0.05, 0.36] |  | validated |
| S06 | 29/33 = 0.88 [0.73, 0.95] | 24/33 = 0.73 [0.56, 0.85] | 10/34 = 0.29 [0.17, 0.46] | 11/20 = 0.55 [0.34, 0.74] | 8/20 = 0.40 [0.22, 0.61] | yes | validated |
| S07 | 0/33 = 0.00 [0.00, 0.10] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 2/20 = 0.10 [0.03, 0.30] | 6/20 = 0.30 [0.15, 0.52] |  | validated |
| S08 | 5/33 = 0.15 [0.07, 0.31] | 3/33 = 0.09 [0.03, 0.24] | 1/34 = 0.03 [0.01, 0.15] | 1/20 = 0.05 [0.01, 0.24] | 2/20 = 0.10 [0.03, 0.30] |  | unvalidated-precision |
| S09 | 0/33 = 0.00 [0.00, 0.10] | 1/33 = 0.03 [0.01, 0.15] | 0/34 = 0.00 [0.00, 0.10] | 2/20 = 0.10 [0.03, 0.30] | 1/20 = 0.05 [0.01, 0.24] |  | validated |
| S11 | 1/33 = 0.03 [0.01, 0.15] | 12/33 = 0.36 [0.22, 0.53] | 9/34 = 0.26 [0.15, 0.43] | 18/20 = 0.90 [0.70, 0.97] | 13/20 = 0.65 [0.43, 0.82] |  | pending |
| U01 | 0/33 = 0.00 [0.00, 0.10] | 1/33 = 0.03 [0.01, 0.15] | 0/34 = 0.00 [0.00, 0.10] | 9/20 = 0.45 [0.26, 0.66] | 6/20 = 0.30 [0.15, 0.52] |  | validated |
| U02 | 0/33 = 0.00 [0.00, 0.10] | 0/33 = 0.00 [0.00, 0.10] | 0/34 = 0.00 [0.00, 0.10] | 4/20 = 0.20 [0.08, 0.42] | 7/20 = 0.35 [0.18, 0.57] |  | validated |
| U03 | 8/33 = 0.24 [0.13, 0.41] | 6/33 = 0.18 [0.09, 0.34] | 2/34 = 0.06 [0.02, 0.19] | 6/20 = 0.30 [0.15, 0.52] | 7/20 = 0.35 [0.18, 0.57] |  | validated |
| U04 | 9/33 = 0.27 [0.15, 0.44] | 13/33 = 0.39 [0.25, 0.56] | 2/34 = 0.06 [0.02, 0.19] | 19/20 = 0.95 [0.76, 0.99] | 17/20 = 0.85 [0.64, 0.95] | yes | validated |
| U05 | 7/33 = 0.21 [0.11, 0.38] | 8/33 = 0.24 [0.13, 0.41] | 16/34 = 0.47 [0.31, 0.63] | 12/20 = 0.60 [0.39, 0.78] | 14/20 = 0.70 [0.48, 0.85] | yes | unvalidated-precision |

Wilson 95% intervals. A rule that fires in nearly every human app (read the human columns) separates nothing; the analysis section applies the pre-registered prevalence-band rule. Rules marked unvalidated in the status column are shown here but are not in the primary endpoints of that track.

## 3. Pre-registered endpoints

### 3a. PRIMARY (frozen v1 detector, first-scan dependency lookup)

### E1 analysis, detector track: v1

Runs: 101 generated, 100 valid. Unvalidated rules excluded from the primary analysis: ['P05', 'P06', 'P07', 'P09', 'P10', 'S03', 'S04', 'S08', 'U05'].

#### Analysis over validated rules (20 rules)

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

#### Analysis over all rules (29 rules)

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

#### Prevalence over unguardrailed runs (PB rule: supported if Wilson lower >= 0.50, refuted if upper < 0.20)

| hypothesis | k/n | share | Wilson 95% | PB decision |
|---|---|---|---|---|
| H1 (D01 or D02) | 66/66 | 1.000 | [0.945, 1.000] | supported |
| H5 (U01 or U05) | 1/66 | 0.015 | [0.003, 0.081] | refuted |
| H6 (S05) | 10/66 | 0.152 | [0.084, 0.257] | inconclusive |

#### Sensitivity analyses (protocol A6; none replaces a confirmatory endpoint)

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

### 3b. SENSITIVITY (v2 detector, first-scan dependency lookup)

### E1 analysis, detector track: v2

Runs: 101 generated, 100 valid. Unvalidated rules excluded from the primary analysis: ['P05', 'P06', 'P07', 'S04', 'S08', 'U05'].

#### Analysis over validated rules (23 rules)

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

#### Analysis over all rules (29 rules)

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

#### Prevalence over unguardrailed runs (PB rule: supported if Wilson lower >= 0.50, refuted if upper < 0.20)

| hypothesis | k/n | share | Wilson 95% | PB decision |
|---|---|---|---|---|
| H1 (D01 or D02) | 66/66 | 1.000 | [0.945, 1.000] | supported |
| H5 (U01 or U05) | 1/66 | 0.015 | [0.003, 0.081] | refuted |
| H6 (S05) | 10/66 | 0.152 | [0.084, 0.257] | inconclusive |

#### Sensitivity analyses (protocol A6; none replaces a confirmatory endpoint)

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

### 3c. Post-hoc sensitivity (depfix): v1 detector, dependency lookup corrected (not confirmatory, section 7)

### E1 analysis, detector track: v1 with depfix (post-hoc sensitivity, not confirmatory)

Runs: 101 generated, 100 valid. Unvalidated rules excluded from the validated-rule analysis: ['P05', 'P06', 'P07', 'P09', 'P10', 'S03', 'S04', 'S08', 'U05'].

#### Analysis over validated rules (20 rules)

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

#### Analysis over all rules (29 rules)

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

#### Prevalence over unguardrailed runs (PB rule: supported if Wilson lower >= 0.50, refuted if upper < 0.20)

| hypothesis | k/n | share | Wilson 95% | PB decision |
|---|---|---|---|---|
| H1 (D01 or D02) | 66/66 | 1.000 | [0.945, 1.000] | supported |
| H5 (U01 or U05) | 1/66 | 0.015 | [0.003, 0.081] | refuted |
| H6 (S05) | 10/66 | 0.152 | [0.084, 0.257] | inconclusive |

#### Sensitivity analyses (protocol A6; none replaces a confirmatory endpoint)

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

### 3d. Post-hoc sensitivity (depfix): v2 detector, dependency lookup corrected (not confirmatory, section 7)

### E1 analysis, detector track: v2 with depfix (post-hoc sensitivity, not confirmatory)

Runs: 101 generated, 100 valid. Unvalidated rules excluded from the validated-rule analysis: ['P05', 'P06', 'P07', 'S04', 'S08', 'U05'].

#### Analysis over validated rules (23 rules)

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

#### Analysis over all rules (29 rules)

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

#### Prevalence over unguardrailed runs (PB rule: supported if Wilson lower >= 0.50, refuted if upper < 0.20)

| hypothesis | k/n | share | Wilson 95% | PB decision |
|---|---|---|---|---|
| H1 (D01 or D02) | 66/66 | 1.000 | [0.945, 1.000] | supported |
| H5 (U01 or U05) | 1/66 | 0.015 | [0.003, 0.081] | refuted |
| H6 (S05) | 10/66 | 0.152 | [0.084, 0.257] | inconclusive |

#### Sensitivity analyses (protocol A6; none replaces a confirmatory endpoint)

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

### 3e. Primary and depfix side by side (validated rules; C1 = share of runs with at least one S-class finding, naive minus guardrailed; C2 = distinct S rule IDs per run, naive minus guardrailed; C3 = S-class findings per kLOC, unguardrailed AI vs human pre-2022)

| endpoint | 3a primary (v1, first scan) | 3c depfix (v1) | 3b sensitivity (v2, first scan) | 3d depfix (v2) |
|---|---|---|---|---|
| C1 | +0.439, p=0.00004 | +0.645, p=0.00001 | +0.439, p=0.00004 | +0.645, p=0.00001 |
| C2 | +3.025, p=0.00001 | +3.259, p=0.00001 | +3.964, p=0.00001 | +4.199, p=0.00001 |
| C3 | AI 2.37 vs human 1.02 per kLOC, delta +0.582, p=0.00009 | AI 2.33 vs human 0.83 per kLOC, delta +0.513, p=0.00055 | AI 2.37 vs human 1.02 per kLOC, delta +0.567, p=0.00013 | AI 2.33 vs human 0.83 per kLOC, delta +0.495, p=0.00086 |

No C1 to C3 verdict changes under depfix: every endpoint keeps its sign and stays below p = 0.01 (the Holm bound for the weakest of five at alpha 0.05) in all four columns. The first-scan lookup bug cuts both ways: it inflated the guardrailed S-class share (against C1) and the S-class density of AI and human apps alike (C3), so the C3 depfix column is the one to read for the human comparison. The primary analysis stays as run (pre-registered pipeline); depfix is a sensitivity track.

### H4: hallucinated dependencies (descriptive, not confirmatory)

Decision rule (protocol section 3): supported if at least one coordinate instance is classed hallucinated-artifact (group:artifact absent from every repository the project declares, on the generation date); refuted if 0 hallucinated artifacts in at least 300 instances; a real artifact with a non-existent version is hallucinated-version and counted separately. **Verdict: H4 REFUTED by the pre-registered rule (0 hallucinated artifacts in 2105 >= 300 instances; rule of three upper 95% bound 0.14%).** Instances: 2105 coordinates in 100 valid runs (bom 27, lib 1532, plugin 546); resolved against the metadata of the declared repositories (default pluginManagement repositories for plugins); no lookup errors; lookup date 2026-11-23.

hallucinated-artifact: 0 first scan, 0 corrected. hallucinated-version: 22 in the first scan (21 KSP plugin, 1 other), 1 after the lookup correction. The first-scan lookup stopped at the first declared repository that answered; for plugins that was google(), whose KSP plugin marker is stale (25 versions, last 1.5.30-1.0.0, 2021), so five real KSP versions (1.9.22-1.0.17, 1.9.23-1.0.20, 1.9.24-1.0.20, 2.0.20-1.0.25, 2.0.21-1.0.28) that exist on Maven Central and the Gradle Plugin Portal were called nonexistent. Gradle falls through to the next repository, so the corrected lookup takes the union over the declared repositories. The one confirmed hallucinated version: codex-luna org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:1.9.24 (the Compose compiler plugin starts at 2.0.0; absent from Central and the Plugin Portal).

S11 status: S11 (nonexistent, hallucinated or known-vulnerable dependency) is a registry lookup, so it has no seeded mutants and no blind precision sample; its check is the resolution itself. The first-scan lookup had the defect above (21 false hallucinated-version findings in 21 runs, 16 more in the human apps), so S11 is not validated in tracks 3a and 3b; in tracks 3c and 3d the lookup is corrected and S11 is validated by resolution (one remaining hallucinated-version, plus known-vulnerable findings from OSV at the lookup date).

## 4. Detector validation

Seeded-mutation recall: 99/99 mutants detected over 27 rules; benign false fires 0 over 24 controls. The mutants were written by the detector's author, so this is an upper bound on recall; precision needs the blind sample below.

Detector fixes (validation/fixes-v2.md): the v1 rule-level rating pass exposed false-positive bugs in five rules (P10, S03, P05, P09, P07); they were fixed in slop-scan/r_perf.py and r_sec.py with seeded-mutation recall unchanged and no benign false fire. The v1 scans were frozen first (results/validation/v1/) and are the PRIMARY track; the fixed detector is the SENSITIVITY track. Fixed rules are validated on fresh pack-v2 items only (none of them in pack v1, at least 6 per rule requested, wider context, whole file for U05); v1 survivors of a fixed rule are in-sample and appear only in track v1. P05 had a single fresh item, so it stays unvalidated-precision on fresh data.

Blind second-rater precision. Pack v1 (157 items drawn before any detector change): raters ['r1', 'r2'], 157 double-rated, kappa 0.671, 22 disagreements. Pack v2 (fresh items): 31 double-rated, kappa 0.748, 4 disagreements. Pooled kappa 0.687, gate (>= 0.60) ok. Consensus = both raters agree, else the adjudicated label (results/validation/adjudication*.csv), else unclear; unclear is excluded from the precision denominator. Rule status: recall >= 0.80 and precision >= 0.80 with >= 5 rated items = validated.

**Track v1 (PRIMARY): rules on their v1 labels, U05 also on its fresh items**

| rule | rated | tp | fp | unclear | precision | Wilson 95% | status |
|---|---|---|---|---|---|---|---|
| D01 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P01 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P02 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P03 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P04 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P05 | 6 | 4 | 2 | 0 | 0.667 | [0.300, 0.903] | unvalidated-precision |
| P06 | 4 | 4 | 0 | 2 | 1.000 | [0.510, 1.000] | unvalidated-precision |
| P07 | 2 | 1 | 1 | 4 | 0.500 | [0.095, 0.905] | unvalidated-precision |
| P08 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P09 | 6 | 4 | 2 | 0 | 0.667 | [0.300, 0.903] | unvalidated-precision |
| P10 | 5 | 1 | 4 | 1 | 0.200 | [0.036, 0.624] | unvalidated-precision |
| P11 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P13 | 5 | 5 | 0 | 1 | 1.000 | [0.566, 1.000] | validated |
| S01 | 6 | 5 | 1 | 0 | 0.833 | [0.436, 0.970] | validated |
| S02 | 6 | 5 | 1 | 0 | 0.833 | [0.436, 0.970] | validated |
| S03 | 6 | 2 | 4 | 0 | 0.333 | [0.097, 0.700] | unvalidated-precision |
| S04 | 1 | 1 | 0 | 0 | 1.000 | [0.207, 1.000] | unvalidated-precision |
| S05 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| S06 | 6 | 5 | 1 | 0 | 0.833 | [0.436, 0.970] | validated |
| S07 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| S08 | 6 | 4 | 2 | 0 | 0.667 | [0.300, 0.903] | unvalidated-precision |
| S09 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| U01 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| U02 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| U03 | 5 | 5 | 0 | 1 | 1.000 | [0.566, 1.000] | validated |
| U04 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| U05 | 6 | 4 | 2 | 6 | 0.667 | [0.300, 0.903] | unvalidated-precision |

**Track v2 (SENSITIVITY): unchanged rules on v1 labels, fixed rules on fresh items only**

| rule | rated | tp | fp | unclear | precision | Wilson 95% | status |
|---|---|---|---|---|---|---|---|
| D01 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P01 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P02 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P03 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P04 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P05 | 1 | 1 | 0 | 0 | 1.000 | [0.207, 1.000] | unvalidated-precision |
| P06 | 4 | 4 | 0 | 2 | 1.000 | [0.510, 1.000] | unvalidated-precision |
| P07 | 4 | 1 | 3 | 2 | 0.250 | [0.046, 0.699] | unvalidated-precision |
| P08 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P09 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P10 | 5 | 5 | 0 | 1 | 1.000 | [0.566, 1.000] | validated |
| P11 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| P13 | 5 | 5 | 0 | 1 | 1.000 | [0.566, 1.000] | validated |
| S01 | 6 | 5 | 1 | 0 | 0.833 | [0.436, 0.970] | validated |
| S02 | 6 | 5 | 1 | 0 | 0.833 | [0.436, 0.970] | validated |
| S03 | 6 | 5 | 1 | 0 | 0.833 | [0.436, 0.970] | validated |
| S04 | 1 | 1 | 0 | 0 | 1.000 | [0.207, 1.000] | unvalidated-precision |
| S05 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| S06 | 6 | 5 | 1 | 0 | 0.833 | [0.436, 0.970] | validated |
| S07 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| S08 | 6 | 4 | 2 | 0 | 0.667 | [0.300, 0.903] | unvalidated-precision |
| S09 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| U01 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| U02 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| U03 | 5 | 5 | 0 | 1 | 1.000 | [0.566, 1.000] | validated |
| U04 | 6 | 6 | 0 | 0 | 1.000 | [0.610, 1.000] | validated |
| U05 | 6 | 4 | 2 | 6 | 0.667 | [0.300, 0.903] | unvalidated-precision |

Coverage of the taxonomy by the other tools (declared coverage and observed counts):

| rule_id | slop-scan_findings | lint_findings | depcheck_findings | existing_tool_declares | custom_only |
|---|---|---|---|---|---|
| P01 | 15 | 0 | 0 | none | 1 |
| P02 | 29 | 0 | 0 | none | 1 |
| P03 | 13 | 0 | 0 | detekt:UnstableCollections|MutableParams|MutableStateParam|StateParam | 0 |
| P04 | 15 | 0 | 0 | detekt:RememberMissing|RememberContentMissing | 0 |
| P05 | 0 | 0 | 0 | lint:FrequentlyChangingValue | 0 |
| P06 | 8 | 0 | 0 | none | 1 |
| P07 | 19 | 0 | 0 | none | 1 |
| P08 | 16 | 0 | 0 | none | 1 |
| P09 | 2 | 0 | 0 | none | 1 |
| P10 | 63 | 0 | 0 | none | 1 |
| P11 | 73 | 0 | 0 | none | 1 |
| P13 | 18 | 0 | 0 | none | 1 |
| S01 | 43 | 0 | 0 | none | 1 |
| S02 | 2 | 0 | 0 | none | 1 |
| S03 | 0 | 0 | 0 | lint:UsesCleartextTraffic|InsecureBaseConfiguration | 0 |
| S04 | 0 | 0 | 0 | lint:TrustAllX509TrustManager|BadHostnameVerifier|AllowAllHostnameVerifier|CustomX509TrustManager | 0 |
| S05 | 12 | 0 | 0 | none | 1 |
| S06 | 45 | 18 | 0 | lint:AllowBackup|DataExtractionRules|FullBackupContent | 0 |
| S07 | 0 | 0 | 0 | lint:ExportedActivity|ExportedService|ExportedReceiver|ExportedContentProvider | 0 |
| S08 | 16 | 0 | 0 | lint:GetInstance|DeprecatedProvider | 0 |
| S09 | 4 | 0 | 0 | lint:SetJavaScriptEnabled|JavascriptInterface | 0 |
| S11 | 0 | 0 | 22 | depcheck:Maven Central / Google Maven existence + OSV | 0 |
| U01 | 3 | 0 | 0 | lint:ContentDescription (views only) | 0 |
| U02 | 0 | 0 | 0 | none | 1 |
| U03 | 29 | 0 | 0 | none | 1 |
| U04 | 220 | 0 | 0 | none | 1 |
| U05 | 65 | 0 | 0 | none | 1 |
| D01 | 123 | 0 | 0 | lint:Deprecated|GradleDeprecated | 0 |
| D02 | 0 | 932 | 1229 | lint:GradleDependency|NewerVersionAvailable;depcheck:latest-stable | 0 |

## 5. Emergent patterns (open coding)

| code | in taxonomy | runs | example evidence |
|---|---|---|---|
| invented-base-url | no | 5 | app/build.gradle.kts buildConfigField LIBRARY_API_BASE_URL https://library.example.com/ (the naive prompt gives no URL) |
| force-unwrap-in-compose | no | 4 | ProfileScreen.kt:134 state.profile!! |
| broad-catch-in-suspend | no | 3 | AuthInterceptor.kt:51,116 and four view models catch (e: Exception) without rethrowing CancellationException |
| dense-minified-source | no | 3 | MainActivity.kt and Api.kt: several statements per line, lines up to about 1300 characters |
| exception-swallowed-to-default | no | 2 | CatalogueRepository.loadPage: runCatching{...}.getOrDefault(false) hides network and database failures |
| overbroad-keep-rule | no | 2 | proguard-rules.pro: -keep class com.example.library.data.** { *; } |
| allowbackup-true-with-session | yes (S06) | 2 | AndroidManifest.xml:9 allowBackup=true in an app that stores a session |
| handwritten-http-json | no | 2 | Api.kt: HttpURLConnection and org.json by hand instead of a client library; response.body!! |
| silent-wrong-default | no | 2 | ShelfTapViewModel.kt:43 daysLeft() = runCatching{...}.getOrDefault(0): a parse failure shows 0 days left |
| wrong-processor-artifact | no | 1 | app/build.gradle.kts: ksp(libs.androidx.room.runtime) instead of room-compiler, Room annotations would not be processed |
| r8-without-keep-rules | no | 1 | isMinifyEnabled=true with a proguard-rules.pro that holds one comment, Retrofit and Gson DTOs |
| deprecated-security-crypto | partial (D01/S08) | 1 | SessionStore: EncryptedSharedPreferences + MasterKey (androidx.security:security-crypto 1.0.0) |
| release-signed-with-debug-key | no | 1 | app/build.gradle.kts release and benchmark types: signingConfig = signingConfigs.getByName("debug") |
| guardrail-scaffolding | no | 1 | extra :baselineprofile module and a benchmark build type beyond the brief (the guardrail names baseline profiles) |
| duplicated-base-url | no | 1 | ShelfTapApp.kt:59 const BASE_URL and AuthInterceptor.kt:23 baseUrl hold the same literal twice |
| cancellation-rethrown | no | 1 | four view models catch CancellationException before Exception (correct pattern; positive code) |
| guardrail-adherent-minify | no | 1 | isMinifyEnabled=true plus a proguard-rules.pro of comments only, with the stated reason: no reflection, libraries ship c |
| narrow-catch-io-json | no | 1 | CatalogueSync.kt:65-67, ViewModels.kt:69-71 catch IOException and JSONException only (positive code) |
| compose-api-compile-error | no | 1 | build fails: CatalogueScreen.kt Cannot access RowColumnParentData (Compose scope-receiver misuse) |
| mock-data-instead-of-api | no | 1 | MainActivity.kt:88-95 eight hardcoded Book(...) rows plus a literal '100,000 books' label; no network call at all |
| imperative-views-custom-canvas | partial (U01/U02) | 1 | no Compose: programmatic View tree and a Canvas-drawn catalogue, colours and strings as literals in MainActivity.kt |
| hallucinated-view-property | no | 1 | MainActivity.kt:50 singleLine = true on EditText (Kotlin property is isSingleLine): Unresolved reference, build fails |
| stale-pinned-versions | partial (D02) | 1 | build.gradle.kts AGP 8.5.2 and Kotlin 1.9.24 while newer stable releases exist |
| offline-fallback-with-saved-copy | no | 1 | repo returns Loaded.Saved and the UI shows showingSaved; typed ApiException code RENEWAL_NOT_ALLOWED mapped to a message |
| hallucinated-package-path | no | 1 | AppGraph.kt:5:24 Unresolved reference 'auth' (an invented import), build fails |
| allowbackup-true-with-user-data | yes (S06) | 1 | AndroidManifest.xml:3 allowBackup=true in an app that caches loans, history and the member profile |
| mock-backend-enabled-by-default | no | 1 | NetworkClient.kt:23 MockLibraryInterceptor(enabled = true) added to the production OkHttp client: login and profile are  |
| encryption-silent-downgrade | partial (S08) | 1 | SessionManager.kt: EncryptedSharedPreferences.create wrapped in try/catch(Exception) that falls back to plain MODE_PRIVA |

## 6. Environment and provenance

| bundle | tool | model arg | model reported | CLI version | harness config hash |
|---|---|---|---|---|---|
| claude-haiku | claude | haiku | claude-haiku-5-5 | 2.1.330 (Claude Code) | 0b805af84eba |
| codex-luna | codex | gpt-5.6-luna | codex-cli 0.170.0 | codex-cli 0.170.0 | 0b805af84eba |
| agy-g38flash | agy | gemini-3.8-flash-medium | 1.5.13 | 1.5.13 | 0b805af84eba |

Host: Linux 7.0.0-43-generic; JDK: openjdk version "21.0.13.1" 2026-11-17; Gradle gradle-9.6.0-bin; detekt-cli 2.0.0-alpha.6 with compose-rules 0.6.8; Android Lint = the version of the AGP each generated project declares (lintDebug); dependency existence from Maven metadata snapshots dated by results/maven-snapshot/; advisories from OSV on the scan date.

Generation: harness/generate.py; one run = one empty directory, one headless call, stub HOME holding only copied auth files, Landlock write jail, shared bench lock, at most two generations in flight. Isolation canary (results/canary.json): a user-level canary appears in 4/4 bundles (the stub is the config in effect), a clean stub shows no canary in 4/4, a project context file is delivered 5/5. Plan: 3 bundles x 3 briefs x 3 styles x reps, rep-major, shuffled within a rep with seed 20261121 (results/plan.json).

Build policy R7: zero-touch build first, then at most three logged table repairs (Gradle version bump, android.useAndroidX, existing-artifact version bump, JDK toolchain to 17, missing import from a fixed table), recorded in results/repairs.csv; repairs touch the build copy, never the analysed source. All analysis is at source level; APK-level numbers are survivors only.

Human control (R3): GitHub search, seeded draw (seed 20261121), screened to Kotlin Compose apps of 2000 to 50000 LOC; 20 apps with first commit up to 2022-11-30 (pre-assistant, confirmatory) and 20 from 2024 on (second comparator). Queries, draw rank and screen verdicts: results/human_corpus*.csv. After scanning, non-source files of the clones (images, fonts, docs, binaries; 95,604 -> 14,221 files) were deleted because the NTFS work volume ran out of file records; every scanner reads only .kt, .xml, Gradle, TOML, .pro, .properties, baseline-prof.txt and stability_config.conf, so a rescan gives the same input.

## 7. Disclosures and deviations

- Campaign size: 101 of 108 planned cells generated (100 valid). Missing cells (agy-g38flash naive: 10/12; agy-g38flash detailed-functional: 9/12; agy-g38flash guardrailed: 10/12, generated/planned of 12 per bundle and style) are infrastructure losses, not model outcomes: 7 agy repetition-4 attempts failed with 0 model calls because the agy OAuth token expired (results/infra_failures.csv, attempts under the work volume infra-failed/). Per protocol A11 they are treated as missing completely at random and were not filled, because agy was not re-authenticated before analysis.py first ran on the confirmatory data. Every table above reports n per arm.
- Early looks (protocol A6 and later): an analysis dry run on 13 runs was computed during the campaign and an emergent-pattern count by bundle (17:50 on the day of open coding) was seen before open coding finished. The dry run showed the author arm-level numbers, so the pre-registered condition that no arm-level C1 to C3 number be seen before the detector fixes was not met; this is why the frozen v1 detector is PRIMARY and the fixed v2 detector is SENSITIVITY (alpha A13).
- Detector fixes were driven by the rule-level pass of the first rater without an arm split (five rules, validation/fixes-v2.md); no rule was retuned for prevalence.
- Rater independence: rater 1 is the author of the detector and of the seeded mutants; rater 2 never saw E1 data and rated from the pack alone. After rater 1 had finished each sheet the file-change notices of the work harness displayed some of rater 2's rows to rater 1 (41 rows of the v1 sheet; the v2 sheet in full: the notice listed 32 lines, the header plus all 31 rows); the labels in rater 1's sheets were written before and were not edited afterwards.
- Resource incidents: the campaign aborted once on the root-volume free-space guard and was resumed; the NTFS work volume ran out of file records three times (ENOSPC despite free bytes); redundant corpus copies and Gradle caches were deleted, one build was rerun (codex-luna-B3-naive-r4, final result compile failure), and the scan was regenerated. No generated source was modified.
- Dependency lookup correction (post-hoc): after the first analysis, the H4 check by another pane found that depcheck stopped at the first declared repository that answered, so a stale google() KSP plugin marker made 21 real KSP versions look nonexistent (S11 false findings in 21 runs, 16 in human apps) and left their latest-stable version empty (D02 missing for them). The lookup now falls through the declared repositories (slop-scan/depcheck.py check(), harness/depfix_apply.py, results/depfix/). Only those rows changed (runs: 21 class changes, 21 latest-stable values filled, no OSV change; human: 16 class changes); the first-scan files stay as they were and every number is reported both ways (sections 3a/3b first scan, 3c/3d corrected). The correction was made after arm-level results were seen, so it is a researcher degree of freedom; it moves the guardrailed share from 17/34 to 10/34 and the human S-class density down as well, and it changes no C1 to C3 verdict (section 3e). The primary analysis was not changed: altering the pre-registered pipeline after the results are known would be a forking path, so depfix is a sensitivity track.
- Containment: results/containment_notes.md triages the four runs that reference a path outside their own directory; no write escaped the run directory.
- Adjudication: 26 rater disagreements (22 in pack v1, 4 in pack v2) were settled by a third reading with the whole source open, written to results/validation/adjudication*.csv with a reason per item. The third reader is rater 1, the detector's author, so it is not independent of the detector. Agreement was lowest where rater 2 could not see callers or colour constants in the window (results/validation/kappa_by_rule.csv: P08 0/6, U03 2/6, P02 3/6, S01 3/6, P09 7/12 agree). Rater-2 sheet sha256 prefixes: v1 cb310a27, v2 f630c4c1.
- G3 held-out rules (P03, P05, P08, S06, D02, U04, U05) were frozen before any data (A1, no substitution). P05 and U05 fail the section 5 precision gate, so the held-out side of G3 rests on P03, P08, S06, D02, U04 in both tracks.
- P07 stays unvalidated-precision in both tracks (1 tp, 3 fp, 2 unclear on the fresh items after the first fix); no second fix round was run, because iterating fixes against fresh samples is tuning (alpha decision). P05 had one fresh item, so its precision rests on the v1 items (secondary, in-sample).
- Pack v1 items for U05 were unjudgeable from a 6-line window; U05 therefore also uses its fresh pack-v2 items with whole-file context in both tracks.

## 8. Threats to validity

- Construct: a rule that fires is a taxonomy finding, not a user-visible defect. Decorative-effect density is a proxy for the AI look. S-class share rewards verbose apps unless normalised; physical LOC is format-dependent (one generator packs about 130 characters per line), so C3 is also reported per 100k non-whitespace characters.
- Detector validity: the detector's author also wrote the seeded mutants, so recall is an upper bound. Precision rests on the blind second-rater sample; rules below 0.80 or with fewer than 5 rated findings are unvalidated-precision and sit outside the primary endpoints. U05 (missing loading, empty or error state) was visibly noisy on the human sample. Static rules cannot see runtime behaviour.
- Isolation: the claude launcher loads three user mods (compact-pilot, model-gate, usage-meter) and five built-in plugins from the real ~/.agents/mods because the home directory is resolved through the password database, not HOME. Tools stay limited to Edit, Glob, Grep, Read, Write, no instruction text and no hook events were observed, and a clean-stub reply was unaffected, but the leak is not zero-proof.
- Shell policy: agy headless mode ends the whole run at the first command outside its allow-list. The allow-list is the shared launcher's read-only set plus file-system housekeeping and read-only JDK probes (E1-local); network, install and build tools stay denied. A run can still end early on a denied command. Runs that end empty are reported as harness-limited, not as model refusals.
- Network audit: the write jail does not restrict the network (the CLIs need their API). Network and build use is audited from executed command strings in the transcripts. The claude launcher has no shell tool; a tool-internal fetch that leaves no command string would not be seen.
- Tool bundles confound harness and model, and model versions can drift inside the generation window; temperature and seed are not exposed by any of the CLIs.
- Scheduling: generations shared the machine with another pane's benchmarks through a shared lock; wall-clock durations exclude lock waits but a rate-limit backoff would be logged in attempts[].
- Build environment: JDK 17 and 21 only, SDK platforms 34 to 37.0. A project asking for something else fails or is repaired by the table. Compile errors outside the import table are failures by policy; build pass rate is therefore a lower bound on what a developer would get after a few minutes of fixing.
- Valid run is deliberately weak (at least one .kt and a Gradle file), so a very small draft counts as valid; the LOC and character columns in section 1 allow a size-floor sensitivity.
- Dependency findings (D02, S11) are dated: known-vulnerable means as of the scan date for AI and human apps alike. The human 2024+ stratum may itself contain assistant-written code, which is why it is a second comparator only.
- Human corpus: GitHub search plus star and size screens select popular, larger, maintained apps, unlike a first draft; per-kLOC rates reduce but do not remove the size mismatch.
- External validity: one app family (ShelfTap briefs B1 to B3), Kotlin and Compose, a stateless first draft that is not how professionals use assistants.

## 9. Reproduce

python3 harness/build_prompts.py; python3 harness/generate.py plan; python3 harness/generate.py campaign --workers 2; python3 harness/build_loop.py; python3 harness/collect.py; python3 harness/scan_corpus.py runs; python3 harness/scan_corpus.py human; python3 slop-scan/validate.py; python3 harness/blind_sample.py; python3 harness/blind_sample_v2.py --per-rule 6 --seed 20261121 --rules P10,S03,P05,P09,P07,U05; python3 harness/rate_merge2.py --r2-v1 PATH --r2-v2 PATH; python3 harness/analysis.py (both tracks); python3 harness/coverage.py; python3 harness/report.py. Heavy state (clones, build copies, Gradle home) lives outside the repo; run sources, transcripts, build logs and scan caches are under runs/.
