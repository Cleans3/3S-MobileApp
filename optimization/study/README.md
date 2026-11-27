# optimization/study — AI slop in Android apps

**EN.** An empirical study of what AI-assisted first drafts cost an Android app in UI, memory and security, and which of those costs the known techniques do not remove. Demo domain: ShelfTap (NFC library app). Shape: problem, cause, solution, every claim backed by a citation or by our own measurement. Baseline techniques (Compose lists, images, threading, R8, Baseline Profiles, Keystore, TLS, logging) are measured, not only listed; the novel part is the set of problems that come from the generation process.

**VI.** Nghiên cứu thực nghiệm: mã do trợ lý AI sinh ra ở bản nháp đầu gây ra những chi phí gì cho ứng dụng Android về giao diện, bộ nhớ và bảo mật, và phần nào các kỹ thuật tối ưu quen thuộc không xử lý được. Ứng dụng demo: ShelfTap (thư viện NFC). Mỗi nhận định đều có trích dẫn đã kiểm chứng hoặc số đo của chính nghiên cứu; bài viết cuối (ARTICLE.md) viết bằng tiếng Việt.

Source outline: [De_cuong_AI_Coding_UI_UX_Hieu_nang_Bao_mat_ban_mo_rong.docx](De_cuong_AI_Coding_UI_UX_Hieu_nang_Bao_mat_ban_mo_rong.docx) (Cuong). The binding agreement between the parts is the contract kept outside the repo by the orchestrator; its taxonomy IDs and CSV formats are reproduced in 02-taxonomy.md and in each experiment's REPORT.md.

## Read in this order

| File | What it is | State |
|---|---|---|
| [00-protocol.md](00-protocol.md) | pre-registered protocol: research questions, hypotheses with decision rules, five confirmatory tests (Holm), briefs B1-B3 (exact texts), held-out rules, detector validation gates, human control, threats, amendments | frozen v1.0 (commit a5969de); amended in section 15 only; current sha256 in [00-protocol.sha256](00-protocol.sha256), history in [00-protocol.sha256.log](00-protocol.sha256.log) |
| [guardrail/ANDROID-GUARDRAIL.md](guardrail/ANDROID-GUARDRAIL.md) | the guardrail context file given to assistants in the guardrailed arm; written from OWASP and Android documentation only, without reading the detector; seven taxonomy rules deliberately omitted | frozen, sha256 in [guardrail/ANDROID-GUARDRAIL.sha256](guardrail/ANDROID-GUARDRAIL.sha256) |
| [01-related-work.md](01-related-work.md) | related work, search log, explicit contribution claims (primary: H3 decorative-effect cost, H2 in an Android error-fix loop; replication and extension: H1, H4) | written |
| [02-taxonomy.md](02-taxonomy.md) | the frozen problem catalogue: 13 performance, 12 security, 6 UI, 2 staleness IDs (plus the accepted addition X01, used in E4 only), each with cause (evidenced or hypothesised), known fix, source, MASVS/MASWE mapping, where it is measured; mapping of the outline's catalogue | written |
| [references.md](references.md) | every cited source with how it was verified and every figure used; list of candidates dropped as unverifiable (97 entries) | written |
| [ARTICLE.md](ARTICLE.md) | the final article, in Vietnamese: Chapters 1-4 (overview and related work, theory = the baseline layer, taxonomy as problem-cause-solution, method), Chapter 5 (results for E1 to E5 and C1 to C5 with Holm, 27 tables, 19 figures) and Chapter 6 (discussion and recommendations); every result number is copied from a committed REPORT or CSV (Holm is the one computed value, Table 5.27); no `TODO[...]` left | complete; uncommitted until alpha commits; open: two exploratory E2 runs, re-run of the literature searches before submission |
| [figures/PLAN.md](figures/PLAN.md) | figure plan F01 to F21: what each shows, chart type, exact CSV columns, annotation rules; section 6 records the build status and every deviation | built: `figures/make_figures.py` (stdlib only) writes `figures/out/F01..F21` (`.svg`, `.data.csv`) and `manifest.json` |
| [slides-outline.md](slides-outline.md) | slide-by-slide outline of the 25-slide talk (message, evidence, source per slide; D1–D8 backup), Vietnamese | rewritten 2026-11-28 for the frozen deck; language-reviewed |
| [presentation-script.md](presentation-script.md) | spoken script in Vietnamese, one section per main slide (Lời nói, Thao tác, Lưu ý), timing table (19:15 at 130 words/min), Q&A mapped to D1–D8, preparation checklist, and a number check that every spoken number is on its slide | rewritten 2026-11-28; language-reviewed; its per-slide sections are embedded in PLAYBOOK.md §6 by `build_playbook_html.py` |
| [PLAYBOOK.md](PLAYBOOK.md) ([HTML](PLAYBOOK.html)) | how the study and the talk are built: questions and verdicts, data pipeline, article chapters, the talk (structure, classification rules, slide table with proof pointers, the full per-slide script), the report, proof, binding glossary, checklists. Rebuild with `python3 build_playbook_html.py`; `--check` exits 1 if the script block or the HTML is stale | rewritten 2026-11-28 |
| [proof/](proof/) | proof of every number in the deck: `deck-proof.csv` (one row per number: source file, line, verbatim quote, or formula), `check_deck_proof.py` (fails on any unproven, wrong or stale row), `ISSUES.md` (what each proof pass changed), `LANGUAGE-REVIEW.md` (Vietnamese review rounds of deck, article, script, outline and report) | 741 rows for 728 values on 33 slides, all proven (2026-11-28) |
| [evidence/](evidence/) | what the audience sees as proof: side-by-side A/B videos (`video/`, cut from the E2 clips), UI screenshots (`img/`), the six hype thumbnails of slide 2 (`img/hype/`, origin per file in its README, not shown on slides), and verbatim excerpts of AI drafts, MobSF output, E4 diffs and E5 transcripts (`code-*`, `agent-*`), each with source path, lines and run id; indexed in `EVIDENCE-code.md` and `EVIDENCE-agent.md` | 13 excerpts, 5 videos, 2 screenshots, 6 hype thumbnails |
| [report/](report/index.html) | the plain-language report (Vietnamese, one web page) in the deck order: the online promise and the one-prompt apps, classification by frequency and severity, method, the three pillars with measured before/after, demo scenarios, build and fix, conclusions, test verdicts and course-target scoreboard, real device, advice per development phase (Tối thiểu / Nâng cao), limits; every number is a deck number | rewritten 2026-11-28; language-reviewed |
| [isolation-audit/](isolation-audit/AUDIT.md) | audit of whether our own agent configuration leaked into any generation run (623 transcripts, positive control, canary rerun, E2 port provenance, benchmark contention) | done 2026-11-27: no contamination found; disclosures in protocol A16 |
| [presentation.html](presentation.html) | the talk as one offline HTML deck (Vietnamese, 25 main slides + D1–D8 backup, notes sum to 19:30): hook with the online promise and the one-prompt apps, classification of problems (slide 4), method, three pillars (UI rendering, memory, security) with A/B evidence, live demo (slide 16), build and fix, conclusions, verdicts, course-target scoreboard, real device, advice per phase, three points; presenter window (P), overview (O), notes (N), light/dark (T), print to PDF | frozen 2026-11-28; every number proven in `proof/` |
| [demo/](demo/README.md) | live demo kit for slide 16: A (AI first draft) next to B (tuned) of the same screen on the emulator, one stdlib `demo.py` (setup, doctor, start, run p09/p08/p06/p13/p10/p01/p0304, record, stop), Windows and Linux; README in Vietnamese, `AGENT-BRIEF.md` for a presenter's coding assistant | ready 2026-11-28; all 7 scenarios ran A and B |
| [e7-novel/](e7-novel/NOTE.md) | E7 (exploratory, desk phase): new problems and tool-enforced gates grounded in E1–E6 (scale probe for toy-data apps, diff guard for error fixes), prototypes with unit tests and retrospective evaluation; emulator phase planned, not run | desk phase done 2026-11-27 |

## Experiments (owners and outputs)

| Folder | Experiment | Owner | Read first |
|---|---|---|---|
| [e1-corpus/](e1-corpus/) | E1: first-draft corpus (3 briefs x 3 prompt styles x tool bundles x repetitions), slop detector, human control corpus | beta-f2-2 | REPORT.md when present; results/*.csv |
| [e2-bench/](e2-bench/) | E2: paired A (slop) / B (tuned) benchmarks in the optlab app (android/optlab, android/benchmark), Macrobenchmark and device-independent probes | beta-f2-3 | [e2-bench/REPORT.md](e2-bench/REPORT.md) |
| [e3-security/](e3-security/) | E3: static tools (MobSF, JADX, Lint) plus manual triage of A/B and corpus APKs | beta-f2-4 | REPORT.md when present |
| [e4-pressure/](e4-pressure/) | E4: assistants asked to fix induced Android errors; frozen rubric, two blind raters | beta-f2-4 | [e4-pressure/RUBRIC.md](e4-pressure/RUBRIC.md) |
| [e5-optimizer/](e5-optimizer/) | E5: an assistant as optimiser on a known-ground-truth app | beta-f2-4 | REPORT.md when present |
| [e6-oneshot/](e6-oneshot/REPORT.md) | E6 (exploratory): one hype-style one-shot prompt vs the same prompt with the guardrail vs a structured spec, 4 bundles x 3 reps, build, emulator screenshots, scroll, detector | alpha-fg-1 panel (beta-fg-2) | [e6-oneshot/AMENDMENT.md](e6-oneshot/AMENDMENT.md) |
| [e7-novel/](e7-novel/NOTE.md) | E7 (exploratory): scale probe and diff guard prototypes, retrospective evaluation on E1, E4, E5, E6 | alpha-fg-1 panel | [e7-novel/NOTE.md](e7-novel/NOTE.md) |

Heavy state (generated sources, APKs, videos, traces, gradle homes, tool venvs) lives outside the repo in the study work directory chosen by the orchestrator; REPORTs state the paths.

## Rules that make the numbers trustworthy

- No number appears in prose that is not in a results CSV or in [references.md](references.md). The orchestrator checks the article's numbers against the CSVs.
- Claims are worded "assistants emit X at rate r against a human base rate b", never "AI causes X". Causes are labelled hypothesised unless an experiment arm tests them.
- Only the five confirmatory tests of protocol section 3.1 carry interpreted p-values; everything else is an estimate with a confidence interval. Inconclusive and negative results are reported with the same prominence as positive ones.
- Emulator numbers are relative A/B evidence only. There is no physical device, no UX study and no human group in E5; protocol section 13 lists the threats.
- An assistant never certifies its own fix: detectors, tests and tools judge.

## Pipeline

Protocol and guardrail frozen, then E1 generation, E2 runs, E3 on APKs, E4 trials, E5, E6, then the reports and the article (Vietnamese). The talk deck is built from the reports and the ShelfTap measurement log, proven number by number in `proof/`, and frozen; the script, outline, report and playbook follow the frozen deck (PLAYBOOK.md §4 draws the flow).
