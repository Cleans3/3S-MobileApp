# PLAYBOOK — the AI-slop study, from protocol to talk

How the study's article is built, what each part claims, where every number comes from, and how to check it. Read this before you write, cut, translate or present any part of the study. Folder: `optimization/study/`. The article itself is [ARTICLE.md](ARTICLE.md) (Vietnamese); the talk is [presentation.html](presentation.html).

## 1. The story in one paragraph

We let AI coding assistants write first drafts of an Android app (ShelfTap, an NFC library app), then measured what those drafts cost in UI performance, memory and security, and which costs the known optimisation techniques do not remove. Five experiments: **E1** a corpus of first drafts scanned by a validated detector and compared with human-written apps; **E2** paired A/B benchmarks (A = code the assistants actually wrote, B = the tuned version) in the `android/optlab` app; **E3** independent static tools (MobSF, jadx) with manual triage; **E4** assistants asked to fix induced errors, judged by a frozen rubric; **E5** an assistant as the optimiser on an app with known problems. Five pre-registered confirmatory tests (C1–C5, Holm-corrected) carry the conclusions; everything else is descriptive. The talk ends on the rule the whole study follows: **"Không để trợ lý tự xác nhận mã của chính nó."**

## 2. Questions, hypotheses, verdicts

Research questions RQ1–RQ6 and hypotheses H1–H6, G1–G3, B1 are defined in [00-protocol.md](00-protocol.md) §2–§3 and restated in ARTICLE.md §1.4. Only the five below are confirmatory.

| Test | Hypothesis | Measure | Verdict | Where |
|---|---|---|---|---|
| C1 | G1: guardrail lowers the share of runs with ≥1 security (S) finding | E1 primary endpoint 1 | supported | ARTICLE Table 5.6, 5.26, 5.27 · deck slide 9 |
| C2 | G2: guardrail lowers the number of distinct rules per run | E1 primary endpoint 2 | supported | Table 5.6 · slide 9 |
| C3 | B1: assistants emit S findings at a higher density than humans | S findings per 1000 Kotlin lines vs human apps before 30/11/2022 | supported | Figure 5.9, Table 5.26 · slide 8 |
| C4 | H2: assistants fix errors by weakening security | share of E4 trials scored `insecure-workaround` vs 0,15 | **not supported** | Figure 5.16 · slide 11 |
| C5 | H3: decorative effects cost frame time | P90 CPU frame time, P08 A vs B | supported | Table 5.19 · slide 6 |

Descriptive results that the talk also uses: H4 (non-existent Gradle coordinates) rejected after we fixed our own lookup bug; H5 (accessibility/state debt) rejected; E3 tool precision; E5 optimiser outcomes. Full list: ARTICLE.md Table 5.26 (line 1197) and Table 5.27 (line 1218).

## 3. Rules that make the numbers trustworthy

1. **No number without a record.** Every number in prose comes from a results CSV, a REPORT.md, the ShelfTap measurement log, or [references.md](references.md). The only computed values in the article are the Holm-adjusted p-values (Table 5.27).
2. **Wording.** "Trợ lý phát ra X với tỷ lệ r so với tỷ lệ nền b của người", never "AI gây ra X". Causes are *hypothesised* unless an experiment arm tests them.
3. **Every result says what kind it is:** *xác nhận* (one of C1–C5) or *mô tả* (estimate with a confidence interval, no interpreted p-value). Negative and inconclusive results get the same space as positive ones.
4. **Emulator numbers are relative.** E2 runs on the `lowend34` emulator (software GPU); read A vs B, not absolute milliseconds. Real-device numbers come only from the ShelfTap Xiaomi runs (`optimization/log/opt-ui-ux.md`).
5. **An assistant never certifies its own fix.** Detectors, tests and tools judge; this applies to how the study was run too.
6. **Retractions stay visible.** The first Baseline Profile table (measured on a debug build) and the H4 lookup bug are reported as corrections, not deleted.

## 4. Data pipeline — where a number travels

```
00-protocol.md (frozen, sha256)          guardrail/ANDROID-GUARDRAIL.md (frozen)
        │
        ▼
runs ──► e*/results/*.csv ──► e*/REPORT.md ──► ARTICLE.md (tables, prose) ──► figures/out/F01..F21 (.svg + .data.csv)
                                         │                               └──► slides-outline.md, presentation-script.md
optimization/log/opt-ui-ux.md ───────────┴──────────────────────────────────► presentation.html
                                                                                   │
                                                          proof/deck-proof.csv ◄───┘ (one row per number, checked)
```

| Experiment | Primary record | Report |
|---|---|---|
| E1 corpus, detector, human control, guardrail | `e1-corpus/results/` (main analysis `results/analysis/primary-v1/`, sensitivity `sensitivity-v2/`, dependency fix `*-deps-corrected/`, `results/depfix/`) | [e1-corpus/REPORT.md](e1-corpus/REPORT.md) |
| E2 A/B benchmarks | `e2-bench/results/summary.csv`, `raw.csv`, `c5_confirmatory.csv`, `build_facts.csv`, `p13_equivalence.csv` | [e2-bench/REPORT.md](e2-bench/REPORT.md) |
| E3 static tools | `e3-security/results/findings.csv` (103 triaged findings) | [e3-security/REPORT.md](e3-security/REPORT.md) |
| E4 fix under pressure | `e4-pressure/results/`, rubric [e4-pressure/RUBRIC.md](e4-pressure/RUBRIC.md) | [e4-pressure/REPORT.md](e4-pressure/REPORT.md) |
| E5 assistant as optimiser | `e5-optimizer/results/` | [e5-optimizer/REPORT.md](e5-optimizer/REPORT.md) |
| ShelfTap on a real phone | `optimization/log/opt-ui-ux.md`, `optimization/ui-ux-metrics.md` | same files |
| External figures | [references.md](references.md) (how each source was verified) | — |

Heavy state (generated apps, APKs, videos, traces) lives outside the repo; each REPORT names its path.

## 5. The article, chapter by chapter

Line numbers are ARTICLE.md lines at the time of writing; search the heading if they drift.

| Chapter (line) | Job | Holds | Main sources |
|---|---|---|---|
| Tóm tắt (11) | the whole result in one page | headline numbers of C1–C5 | Tables 5.26–5.27 |
| 1 Tổng quan (21) | context, related work and the gap, the "AI slop" definition, RQs and hypotheses (§1.4), contributions and what we do **not** claim (§1.5), relation to the group outline (§1.6) | RQ and hypothesis tables | 00-protocol.md §1–§3, 01-related-work.md |
| 2 Cơ sở lý thuyết (109) | the **baseline layer**: what the known techniques do (Compose lists, images, threading, R8, Baseline Profile, Keystore, TLS, logging, accessibility, staleness) | mechanisms, official guidance | references.md |
| 3 Phân tích vấn đề (244) | the catalogue as problem → cause → fix: P01–P13, S01–S12 + X01, U01–U06, D01–D02; §3.6 separates AI-caused from spec-caused and integration-caused; §3.7 maps the group outline's catalogue | catalogue tables | 02-taxonomy.md |
| 4 Phương pháp (364) | pre-registration, ShelfTap briefs B1–B3 and prompt styles, E1–E5 designs, statistics (§4.8), independence and number checks (§4.9), threats (§4.10), amendments (§4.11) | Figures 4.1–4.2 | 00-protocol.md (incl. §15 amendments) |
| 5 Thực nghiệm (557) | results: E1 §5.2 (Tables 5.1–5.7, Figures 5.1–5.10), E2 §5.3 (Tables 5.9–5.20, Figures 5.11–5.14), E3 §5.4 (Tables 5.21–5.23, Figure 5.15), E4 §5.5 (Figures 5.16–5.17), E5 §5.6 (Tables 5.24–5.25, Figure 5.18), verdicts §5.7 (Tables 5.26–5.27, Figure 5.19), practical limits §5.8 | 27 tables, 19 figures | e1…e5 results + REPORTs |
| 6 Kết luận (1252) | what AI coding is good for, limits of letting AI optimise, the reviewer's role, a workflow proposal, future work | recommendations | Chapter 5 only (no new numbers) |
| Tài liệu tham khảo (1292) | every cited source | — | references.md |

**Figures:** every figure caption names its data file `figures/out/Fxx-*.data.csv`; `figures/make_figures.py` (stdlib) rebuilds all of them and `figures/out/manifest.json`. Plan and deviations: [figures/PLAN.md](figures/PLAN.md).

## 6. The talk

| File | What | Length |
|---|---|---|
| [presentation.html](presentation.html) | the deck: 18 slides + 4 backup (D1 Holm, D2 full E2 table, D3 E4 detail, D4 rule prevalence), Vietnamese, offline; needs the [evidence/](evidence/) folder next to it for videos and screenshots | ~14 min |
| [presentation-script.md](presentation-script.md) | speaker script with timing and Q&A | 10 min version |
| [slides-outline.md](slides-outline.md) | the 10-minute outline from the group plan | 10 min |

Design rule for the main slides (audience is not technical): one plain sentence as the headline, one picture of real evidence (A/B video, verbatim code, tool output, screenshot) and at most one or two numbers. Statistics (p, Cliff's delta, intervals, n) go to the speaker notes and backup slides D1–D4. Every evidence item shows its source path on the slide; each one is documented in [evidence/EVIDENCE-code.md](evidence/EVIDENCE-code.md) and [evidence/EVIDENCE-agent.md](evidence/EVIDENCE-agent.md).

Deck order, the evidence each slide shows, and the article section it comes from:

| Slide | Says | Evidence on the slide | Article | Kind |
|---|---|---|---|---|
| 1–3 | title, problem, method E1–E5 + pre-registration | — | §1, §4 | — |
| 4 | E1: 42/100 drafts build; 31/33 unguarded drafts have a security finding | code-01: token saved in plain SharedPreferences | §5.2 | mô tả |
| 5 | E2 P01+P09 long list (90% → 1,3% slow frames), P06 images | video P09c A/B | §5.3 Tables 5.14–5.15 | mô tả |
| 6 | C5 decorative card cost (+28%) | video P08c A/B | Table 5.19 | xác nhận |
| 7 | E2: which optimisations a user feels (P13, R8, Baseline Profile, Compose nulls) | video P13c A/B | Tables 5.9, 5.10, 5.16–5.18 | mô tả |
| 8 | C3 AI vs human density (2,3×) | — | §5.2, Figure 5.9 | xác nhận |
| 9 | C1/C2 guardrail, buildable subset | code-02 (same brief with guardrail: Keystore AES-GCM), code-07 rule 4 | Tables 5.6–5.7, Figure 5.8 | xác nhận |
| 10 | E3: MobSF wrong 7 in 10, misses real issues | code-05 (exported false positive + triage row), code-06 (Level.BODY logging MobSF missed) | §5.4, Figure 5.10 | mô tả |
| 11 | C4 fix under pressure (17/148) | agent-01 (error + diff removing setUserAuthenticationRequired), agent-02 (safe fix) | §5.5 | xác nhận (not supported) |
| 12 | E5: claims "fixed" while the tree does not compile | agent-06 (claim + runs.csv compiles=0), agent-04, agent-05 | §5.6 | mô tả |
| 13–15 | ShelfTap on a Xiaomi phone: debug vs measure build, home scroll fix, UI scorecard | cover URL 10.0.2.2; screenshots A/B at font scale 2.0 | opt-ui-ux.md (not in ARTICLE) | mô tả |
| 16 | verdict board, plain words | — | Tables 5.26–5.27 | — |
| 17–18 | limitations, three habits, closing line | — | §5.8, §6 | — |

Keys: → / Space next, ← back, P presenter window, O overview, N notes, T light/dark, F fullscreen, B blackout, digits + Enter jump, ? help. Print to PDF renders every slide in its final state. To show it over the tailnet: `python3 -m http.server <port> --bind <tailscale-ip> --directory optimization/study`.

## 7. Proof of every number in the deck

- [proof/deck-proof.csv](proof/deck-proof.csv): one row per distinct number per slide (visible text, speaker notes, chart data attributes, bar lengths). Columns: `slide,value,kind,claim,source,line,quote,formula,note`.
  - `direct`: the number is on `source:line`, inside the verbatim `quote`.
  - `cited`: same, from an external source recorded in references.md.
  - `derived`: `formula` recomputes it from proven inputs (percent changes, bar lengths).
  - `label`: not data (slide counts, talk minutes, an address); `note` says what it is.
- [proof/check_deck_proof.py](proof/check_deck_proof.py) (stdlib): reads the deck, lists every number, and fails unless each has a row whose quote is really on that line and contains the number (after rounding), and each formula reproduces the shown value.

```
python3 optimization/study/proof/check_deck_proof.py            # whole deck, exit 0 = all proven
python3 optimization/study/proof/check_deck_proof.py --list 5   # numbers on slide 5 that need a row
python3 optimization/study/proof/check_deck_proof.py --slide 5  # check one slide
```

- [proof/ISSUES.md](proof/ISSUES.md): numbers that did **not** match their source, what was changed, and why.
- Adding or changing a number on a slide = add or fix its row in the same change, then run the checker.
- Primary records first (results CSV, REPORT, measurement log). ARTICLE.md is a valid source only for values computed there (Holm p-values), and the row says so.

## 8. Language (Vietnamese)

Terms fixed for the whole study; use them in the article, the deck, the script and any group material.

| Concept | Use | Avoid |
|---|---|---|
| a book loan / borrowing record | **lượt mượn** (one borrowing), **phiếu mượn** (the slip) | khoản vay (means a money loan) |
| first draft from an assistant | bản nháp đầu | bản thảo AI |
| AI coding assistant | trợ lý (lập trình) AI | "AI" alone as the subject of a causal verb |
| confirmatory / descriptive result | xác nhận / mô tả | "chứng minh" for a descriptive estimate |
| human-written app | ứng dụng do người viết | ứng dụng "thật" |

[proof/LANGUAGE-REVIEW.md](proof/LANGUAGE-REVIEW.md) holds the full language review of the deck and what was applied.

## 9. Group material — how it fits

| Material | Status | How to use it |
|---|---|---|
| Group outline `De_cuong_AI_Coding_UI_UX_Hieu_nang_Bao_mat_ban_mo_rong.docx` (Cuong) | the plan the study implements; ARTICLE.md §1.6 and §3.7 map it | source of the four original questions |
| `optimization/TieuLuan-ToiUu-Android.docx`, `optimization/Script-ThuyetTrinh.docx` (Huy, commit e4259e1) | a technique-by-technique essay and a 25-minute script; numbers are the B (tuned) side of E2 plus the ShelfTap MobSF rows of E3 and the product's security design | fits as the baseline layer (ARTICLE Chapter 2) and as product security; needs the A (before) numbers next to each B number, and must not present P03+P04, Baseline Profile or card effects as wins the A/B data does not show (see §2, slide 7, slide 6). Offline FTS search exists only in `android/optlab`, not in the member app |
| Canva deck (group) | not reviewed: Canva blocks automated browsers | export to PDF into the repo to review |

## 10. Checklists

**Before presenting**
- [ ] `python3 optimization/study/proof/check_deck_proof.py` exits 0.
- [ ] Deck opens offline on the presenting machine (fonts are embedded); presenter window (P) works there.
- [ ] Light theme on a projector; dark only for a dark room.
- [ ] Rehearse to ~14 min, or cut to the 10-minute outline (drop slides 7, 12, 15 and keep them as backup).
- [ ] Know the answers in presentation-script.md's Q&A section, especially: emulator vs real phone, apples-to-oranges for C3, why C1 vanishes on buildable apps, why C4 is "not supported".

**Before submitting the article**
- [ ] Every number in ARTICLE.md still matches its CSV (orchestrator check, README.md "Rules").
- [ ] `python3 optimization/study/figures/make_figures.py` rebuilds all figures without change.
- [ ] Literature searches re-run (README.md open item); two exploratory E2 runs settled.
- [ ] Protocol sha256 still matches 00-protocol.sha256.
