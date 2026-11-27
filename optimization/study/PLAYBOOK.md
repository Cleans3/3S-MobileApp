# PLAYBOOK — the AI-slop study, from protocol to talk

How the study's article is built, what each part claims, where every number comes from, and how to check it. Read this before you write, cut, translate or present any part of the study. Folder: `optimization/study/`. The article itself is [ARTICLE.md](ARTICLE.md) (Vietnamese); the talk is [presentation.html](presentation.html).

## 1. The story in one paragraph

We let AI coding assistants write first drafts of an Android app (ShelfTap, an NFC library app), then measured what those drafts cost in UI performance, memory and security, and which costs the known optimisation techniques do not remove. Five experiments: **E1** a corpus of first drafts scanned by a validated detector and compared with human-written apps; **E2** paired A/B benchmarks (A = code the assistants actually wrote, B = the tuned version) in the `android/optlab` app; **E3** independent static tools (MobSF, jadx) with manual triage; **E4** assistants asked to fix induced errors, judged by a frozen rubric; **E5** an assistant as the optimiser on an app with known problems. Five pre-registered confirmatory tests (C1–C5, Holm-corrected) carry the conclusions; everything else is descriptive. The talk ends on the rule the whole study follows: **"Không để trợ lý tự xác nhận mã của chính nó."**

## 2. Questions, hypotheses, verdicts

Research questions RQ1–RQ6 and hypotheses H1–H6, G1–G3, B1 are defined in [00-protocol.md](00-protocol.md) §2–§3 and restated in ARTICLE.md §1.4. Only the five below are confirmatory.

| Test | Hypothesis | Measure | Verdict | Where |
|---|---|---|---|---|
| C1 | G1: guardrail lowers the share of runs with ≥1 security (S) finding | E1 primary endpoint 1 | supported | ARTICLE Table 5.6, 5.26, 5.27 · deck slide 10 |
| C2 | G2: guardrail lowers the number of distinct rules per run | E1 primary endpoint 2 | supported | Table 5.6 · slide 10 |
| C3 | B1: assistants emit S findings at a higher density than humans | S findings per 1000 Kotlin lines vs human apps before 30/11/2022 | supported | Figure 5.9, Table 5.26 · backup D2 |
| C4 | H2: assistants fix errors by weakening security | share of E4 trials scored `insecure-workaround` vs 0,15 | **not supported** | Figure 5.16 · slide 11 |
| C5 | H3: decorative effects cost frame time | P90 CPU frame time, P08 A vs B | supported | Table 5.19 · slide 15 |

Descriptive results that the talk also uses: H4 (non-existent Gradle coordinates) rejected (with the corrected lookup); H5 (accessibility/state debt) rejected; E3 tool precision; E5 optimiser outcomes. Full list: ARTICLE.md Table 5.26 (line 1197) and Table 5.27 (line 1218).

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
| [presentation.html](presentation.html) | the deck: 21 slides + 11 backup (D1–D11), Vietnamese, offline; same order as [report/index.html](report/index.html); needs [evidence/](evidence/) and [e6-oneshot/img/](e6-oneshot/img/) next to it | ~14 min |
| [presentation-script.md](presentation-script.md) | speaker script with timing and Q&A, one segment per slide | ~14 min |
| [slides-outline.md](slides-outline.md) | slide-by-slide outline of the deck | ~14 min |

Design rule for the main slides (audience is not technical): one plain sentence as the headline, one picture of real evidence (A/B video, verbatim code, tool output, screenshot) and at most one or two numbers. Statistics (p, Cliff's delta, intervals, n) go to the speaker notes and backup slides D1–D11. Every evidence item shows its source path on the slide; each one is documented in [evidence/EVIDENCE-code.md](evidence/EVIDENCE-code.md) and [evidence/EVIDENCE-agent.md](evidence/EVIDENCE-agent.md).

Deck order (rebuilt 2026-11-28 to follow the plain-language report), the evidence each slide shows, and where it comes from:

| Slide | Says | Evidence on the slide | Source | Kind |
|---|---|---|---|---|
| 1 | title: "Một câu lệnh, một ứng dụng đẹp?" | — | — | — |
| 2 | lead-in: the "vibe coding" hype and the one-shot prompt | Karpathy, Stack Overflow 2025, Collins, Merriam-Webster; prompts/O.txt verbatim | references.md §G, report/sources-leadin.md | cited |
| 3 | E6: same prompt, four assistants, very different apps (6/12 build, 120–3.702 lines, 74–100% janky) | four arm-O screenshots | e6 REPORT §2 | mô tả |
| 4 | the problem in short: 90% → 1,3%, 203 → 6,2 MB, 60/66; 2,3× vs human apps | — | e2 REPORT, e1 REPORT:127–128 | mô tả |
| 5 | method E1–E6, and the isolation check (623 transcripts, 0 markers, canary 12/12) | — | 00-protocol, isolation-audit/AUDIT.md | — |
| 6 | the seven links of the vibe-code chain, one number each | — | report/index.html §4 | — |
| 7 | link 1, tool and model: haiku 1/9 vs sonnet 7/9 (E6), E1 build rate per bundle | — | e6 REPORT §5, e1 REPORT | mô tả |
| 8 | link 2, prompt (E1): longer functional prompt is not safer, builds less | — | e1 REPORT:127–129, §1 | mô tả |
| 9 | link 2, prompt (E6): structured spec, janky 99% → 19,6% | claude-sonnet arm O vs arm S screenshots | e6 REPORT §4 | mô tả |
| 10 | link 3, rules file: C1/C2 plus E6 arm R and the Baseline Profile rule | code-02, code-07 | Tables 5.6–5.7, e6 REPORT §4 | xác nhận |
| 11 | link 4, when it errors: C4 17/148 | agent-01, agent-02 | §5.5 | xác nhận (not supported) |
| 12 | link 5, verify: E5 "fixed" claim vs compile error | agent-06, agent-04, agent-05 | §5.6 | mô tả |
| 13 | link 5, scanners: MobSF wrong 7 in 10, misses real issues | code-05, code-06 | §5.4 | mô tả |
| 14 | link 6, lists and images: 90% → 1,3% | video P09c A/B | Tables 5.14–5.15 | mô tả |
| 15 | link 6, decorative effects: C5 +28% | video P08c A/B | Table 5.19 | xác nhận |
| 16 | link 6, what the user feels: P13, R8, Baseline Profile, Compose nulls | video P13c A/B | Tables 5.9, 5.10, 5.16–5.18 | mô tả |
| 17 | link 7, measure the release build: 125 vs 12–16 ms | — | opt-ui-ux.md | mô tả |
| 18 | link 7, bugs only a real phone shows: 10.0.2.2 cover URL | — | opt-ui-ux.md | mô tả |
| 19 | link 7, large font: all four E6 apps break at 2.0; ShelfTap 5/7 → 0/7 | e6 img/grid-O-font2.jpg; ShelfTap A/B screenshots | e6 REPORT §2, opt-ui-ux.md | mô tả |
| 20 | seven habits, vibe coder vs engineer | — | report/index.html §5 | — |
| 21 | closing: three habits, "Không để trợ lý tự xác nhận mã của chính nó." | — | — | — |

Every solution slide (7, 9–19 except 8 and 18) ends with the two audience boxes: "Nếu bạn vibe-code" (habits and prompts) and "Nếu bạn là kỹ sư" (process, tests, CI, measurement). Backup D1–D11: verdict board, C3 vs human, E1 drafts, limits, Holm table, full E2 table, E4 detail, rule prevalence, E6 build table, isolation check detail, lead-in sources. Framing rule (user, 2026-11-28): never lead with admitting a mistake; a corrected number is told as the lesson ("debug builds mislead, measure the release"), and limits are stated once, plainly, in notes or backup.

Keys: → / Space next, ← back, P presenter window, O overview, N notes, T light/dark, F fullscreen, B blackout, digits + Enter jump, ? help. Print to PDF renders every slide in its final state. To show it over the tailnet: `python3 -m http.server <port> --bind <tailscale-ip> --directory optimization/study`.


## 6b. The plain-language report

[report/index.html](report/index.html) tells the study to a non-technical reader in the order the user asked for on 2026-11-27: a relatable lead-in (the "vibe coding" hype and the social-media one-shot prompt, with cited sources [Karpathy2025], [Collins2025], [SOSurvey2025], [Veracode2025], [MerriamWebster2025], [Ouyang2023], then our own E6 grid of four tools answering the same prompt), the problem kept short (three numbers and one code excerpt), how we tested (E1 to E6 plus the isolation audit), and the bulk on solutions along seven links of the vibe-code chain:

| Link | Evidence in the report | Source |
|---|---|---|
| 1 Choose tool and model | E1 build rate per bundle; E6 haiku 1/9 vs sonnet 7/9 builds, same launcher | e1 REPORT §1, e6 REPORT §5 |
| 2 Write the prompt | E1 S-class share by prompt style (31/33, 29/33, 17/34) and build rate by style (20/33, 12/33, 10/34); E6 arm S janky median 19,6% vs 99% | e1 REPORT:127-129 and §1, e6 REPORT §4 |
| 3 Rules file | code-01 vs code-02; C1/C2; builders-only caveat; E6 arm R and the Baseline Profile rule that broke codex | e1 REPORT, e6 REPORT §4 |
| 4 When it errors | E4 diff removing setUserAuthenticationRequired, 17/148 | agent-01, agent-02 |
| 5 Verify before trusting | E5 claim vs compile error; MobSF false positive vs missed Level.BODY | agent-06, agent-04, code-05, code-06 |
| 6 Engineering fixes | P09c and P08c A/B videos, table of measured effects | e2 REPORT |
| 7 Measure right | ShelfTap debug vs release, cover URL bug, font-scale screenshots, E6 font 2.0 grid | opt-ui-ux.md, e6 img/grid-O-font2.jpg |

Each link ends with two boxes: what a vibe coder does, what an engineer does. Design: one column, plain sans, two chart colours (orange = AI-written, blue = fixed; validated with the dataviz palette checker), every figure with a source line. E6 numbers are descriptive (exploratory, n = 3 per cell, software-GPU emulator) and the report says so.

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
- [ ] Rehearse to ~14 min. For 10 minutes, move slides 8, 13 and 16 to backup (each repeats a point another slide already makes).
- [ ] Know the answers in presentation-script.md's Q&A section, especially: emulator vs real phone, apples-to-oranges for C3, why C1 vanishes on buildable apps, why C4 is "not supported".

**Before submitting the article**
- [ ] Every number in ARTICLE.md still matches its CSV (orchestrator check, README.md "Rules").
- [ ] `python3 optimization/study/figures/make_figures.py` rebuilds all figures without change.
- [ ] Literature searches re-run (README.md open item); two exploratory E2 runs settled.
- [ ] Protocol sha256 still matches 00-protocol.sha256.
