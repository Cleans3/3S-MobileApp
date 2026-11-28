# PLAYBOOK — the AI-slop study, from protocol to talk

How the study is built, what each part claims, where every number comes from, how to check it, and what to say in the talk. Read this before you write, cut, translate or present any part of the study. Folder: `optimization/study/`. The article is [ARTICLE.md](ARTICLE.md) (Vietnamese); the talk is [presentation.html](presentation.html); the spoken script is embedded in section 6.

## 1. The story in one paragraph

We let AI coding assistants write first drafts of an Android app (ShelfTap, an NFC library app), then measured what those drafts cost in UI performance, memory and security, and which costs the known optimisation techniques do not remove. Six experiments:
- **E1**: a corpus of first drafts, scanned by a validated detector and compared with human-written apps.
- **E2**: paired A/B benchmarks in the `android/optlab` app. A is the code the assistants actually wrote; B is the tuned version.
- **E3**: independent static tools (MobSF, jadx) with manual triage.
- **E4**: assistants asked to fix induced errors, judged by a frozen rubric.
- **E5**: an assistant as the optimiser on an app with known problems.
- **E6** (exploratory): one hype-style prompt, the same prompt with the rules file, and a structured spec.

Five pre-registered confirmatory tests (C1–C5, Holm-corrected) carry the conclusions; everything else is descriptive. An exploratory follow-up, **E7**, turns two of the gaps into tool-enforced checks (section 4). The talk runs in five parts (27 main slides plus 12 backup slides D1–D12): it opens with a poster hook, the online promise ("one prompt, one app") and five headline numbers; presents the six experiments and overall results (classification, build failures, "đã sửa xong", real-device lessons, test verdicts, six conclusions); explains the causes behind them across three pillars (UI rendering, memory, security) plus a live A/B demo; lays out a five-rung solution ladder from least to most technical (Bậc 1–5); and closes on three points: looking finished is not being optimised; frequency is not severity; trust the build and the measurement, not "đã sửa xong".

## 2. Questions, hypotheses, verdicts

Research questions RQ1–RQ6 and hypotheses H1–H6, G1–G3, B1 are defined in [00-protocol.md](00-protocol.md) §2–§3 and restated in ARTICLE.md §1.4. Only the five below are confirmatory.

| Test | Hypothesis | Measure | Verdict | Where |
|---|---|---|---|---|
| C1 | G1: the rules file lowers the share of runs with ≥1 security (S) finding | E1 primary endpoint 1 | supported | ARTICLE Tables 5.6, 5.26, 5.27 · slides 9, 18, D2 |
| C2 | G2: the rules file lowers the number of distinct rules per run | E1 primary endpoint 2 | supported | Table 5.6 · slides 9, 18, D2 |
| C3 | B1: assistants emit S findings at a higher density than humans | S findings per 1000 Kotlin lines vs human apps before 30/11/2022 | supported | Figure 5.9, Table 5.26 · slides 9, 17, D2 |
| C4 | H2: assistants fix errors by weakening security | share of E4 trials scored `insecure-workaround` vs 0,15 | **not supported** | Figure 5.16 · slides 9, 19, D2, D4 |
| C5 | H3: decorative effects cost frame time | P90 CPU frame time, P08 A vs B | supported | Table 5.19 · slides 9, 12, D2 |

Descriptive results the talk also uses:
- H4 (non-existent Gradle coordinates): rejected, using the corrected lookup.
- H5 (accessibility and state debt): rejected.
- E3 tool precision, E5 optimiser outcomes, and E6.

Full list: ARTICLE.md Table 5.26 (line 1197) and Table 5.27 (line 1218).

## 3. Rules that make the numbers trustworthy

1. **No number without a record.** Every number comes from one of these: a results CSV, a REPORT.md, the ShelfTap measurement log (`optimization/log/opt-ui-ux.md` and `optimization/ui-ux/measure/`), or [references.md](references.md). The only computed values are the Holm-adjusted p-values and the unit conversions (MB = KB / 1024; APK bytes / 1 000 000); each has a formula row in the proof table.
2. **Wording.** Write "bản nháp AI có lỗi X với tỷ lệ r", never "AI gây ra X". Causes are *hypothesised* unless an experiment arm tests them.
3. **Every result says what kind it is:** *được xác nhận* or *không được xác nhận* (one of C1–C5), or *mô tả* (an estimate with a confidence interval and no interpreted p-value). Negative and inconclusive results get the same space as positive ones.
4. **Emulator numbers are relative.** E2 runs on the `lowend34` emulator (software GPU); read A against B, not absolute milliseconds. Real-device numbers come only from the ShelfTap Xiaomi runs.
5. **An assistant never certifies its own fix.** Detectors, tests and tools judge. This applies to how the study itself was run.
6. **Limits are stated once, plainly.** They go in the notes or on a backup slide. A corrected number is told as the lesson ("measure the measure build, not debug"), never as an apology. Retractions in the article (the first Baseline Profile table, the H4 lookup bug) stay visible as corrections.

## 4. Data pipeline — where a number travels

```
00-protocol.md (frozen, sha256)          guardrail/ANDROID-GUARDRAIL.md (frozen)
        │
        ▼
runs ──► e*/results/*.csv ──► e*/REPORT.md ──► ARTICLE.md (tables, prose) ──► figures/out/F01..F21 (.svg + .data.csv)
                                         │
optimization/log/opt-ui-ux.md ───────────┼──► presentation.html (frozen deck) ──► presentation-script.md, slides-outline.md
optimization/ui-ux/measure/*/            │            │                           report/index.html, PLAYBOOK.md §6
                                         │            └──► proof/deck-proof.csv (one row per number, checked)
                                         └──► demo/demo.py (live A/B, study numbers as the reference)
```

| Experiment | Primary record | Report |
|---|---|---|
| E1 corpus, detector, human control, rules file | `e1-corpus/results/` (main analysis `results/analysis/primary-v1/`, sensitivity `sensitivity-v2/`, dependency fix `*-deps-corrected/`, `results/depfix/`) | [e1-corpus/REPORT.md](e1-corpus/REPORT.md) |
| E2 A/B benchmarks | `e2-bench/results/summary.csv`, `raw.csv`, `c5_confirmatory.csv`, `build_facts.csv`, `p13_equivalence.csv` | [e2-bench/REPORT.md](e2-bench/REPORT.md) |
| E3 static tools | `e3-security/results/findings.csv` (103 triaged findings) | [e3-security/REPORT.md](e3-security/REPORT.md) |
| E4 fix under pressure | `e4-pressure/results/` (`c4_confirmatory.csv`, `kappa.txt`), rubric [e4-pressure/RUBRIC.md](e4-pressure/RUBRIC.md) | [e4-pressure/REPORT.md](e4-pressure/REPORT.md) |
| E5 assistant as optimiser | `e5-optimizer/results/` | [e5-optimizer/REPORT.md](e5-optimizer/REPORT.md) |
| E6 one-shot prompt (exploratory) | `e6-oneshot/results/` (`findings.csv`, `canary.json`), screenshots `e6-oneshot/img/` | [e6-oneshot/REPORT.md](e6-oneshot/REPORT.md), plan [e6-oneshot/AMENDMENT.md](e6-oneshot/AMENDMENT.md) |
| Isolation of the generation runs | `isolation-audit/` (623 transcripts) | [isolation-audit/AUDIT.md](isolation-audit/AUDIT.md) |
| ShelfTap on a real phone | `optimization/log/opt-ui-ux.md`, `optimization/ui-ux-metrics.md`, `optimization/ui-ux/measure/results-*/` | same files |
| E7 new problems and gates (exploratory, desk phase) | `e7-novel/results/`, prototypes `scale_probe.py`, `diffguard.py`, `ScaleBudgetGate.kt` | [e7-novel/NOTE.md](e7-novel/NOTE.md) |
| External figures | [references.md](references.md) (how each source was verified) | — |

Heavy state (generated apps, APKs, videos, traces) lives outside the repo; each REPORT names its path.

**E7 (backup D9 only).** [e7-novel/NOTE.md](e7-novel/NOTE.md) goes one step past measuring known problems. It names six gaps the E1–E6 data exposes, lists eight candidate new problems or solutions, and builds two prototypes:
- a scale probe: run a generated app at realistic data sizes, because toy data hides P06, P09 and P13;
- a diff guard: a tool, not the assistant, judges an error fix for weakened security.

Both prototypes are unit-tested (`test_prototypes.py`) and evaluated retrospectively on the E1, E4, E5 and E6 corpora. The emulator phase E7a ran on 2026-11-28 (§8 of the note) and is shown on backup D9, labelled exploratory; E7b is not run. Quote E7 only as exploratory work, outside the pre-registered tests.

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

**Figures:** every figure caption names its data file `figures/out/Fxx-*.data.csv`. `figures/make_figures.py` (stdlib) rebuilds all of them and `figures/out/manifest.json`. Plan and deviations: [figures/PLAN.md](figures/PLAN.md). The figures are frozen. Where a figure prints an older term (for example "luật", "guardrail" or "đường GPU"), the caption ends with one sentence that glosses it with the current term.

**Wording rules (changed 2026-11-28).** The article went through two language passes, both with an invariant check: same numbers, IDs, headings, tables, citations and links as the baseline.
- Pass 1 rewrote it to the binding glossary of section 8: "nhóm" instead of "chúng tôi", "quy tắc" instead of "luật", no dangling "nó".
- Pass 2 replaced the literal calques the reviewer found, about 200 replacements. Examples: "chấm ẩn nhãn", "chốt chặn kiểm tra tự động", "tiêu chí xác nhận", "báo cáo tự đánh giá", "trợ lý sửa mã", "cấu hình kết xuất GPU".

Details: [proof/LANGUAGE-REVIEW.md](proof/LANGUAGE-REVIEW.md) §3. Rebuild the HTML with:

```
cd optimization/study
uv run --no-project --with markdown python article-build/build_html.py ARTICLE.md ARTICLE.html article-build/article.css
```

(The system `python3` has no `markdown` module; `build_html.py` needs all three arguments.)

## 6. The talk

| File | What | Length |
|---|---|---|
| [presentation.html](presentation.html) | the deck: 27 main slides + 12 backup (D1–D12), Vietnamese, one offline HTML with embedded fonts. It needs [evidence/](evidence/) (videos, hype thumbnails, screenshots) and [e6-oneshot/img/](e6-oneshot/img/) next to it | speaker notes sum to 19:30 |
| [presentation-script.md](presentation-script.md) | the spoken script, one section per main slide (Lời nói, Thao tác, Lưu ý), a timing table, Q&A mapped to D1–D12, a preparation checklist and a number check. The per-slide sections are copied below | 19:30 (matching slide notes) |
| [slides-outline.md](slides-outline.md) | slide-by-slide outline grouped by the five parts: message, evidence, source | — |
| [demo/README.md](demo/README.md) | the live demo kit for slide 20 (Vietnamese) | 120 s slot |

### How the talk is built

The talk is 15 to 20 minutes for lecturers and classmates. It runs in five parts (`data-part` A–E, shown as a strip at the bottom of slides 2–27; slide 1 is a poster without strip or page number):

| Part | Slides | Job |
|---|---|---|
| A · Mở đầu | 1–3 | poster hook ("Trông hoàn thiện. Chưa tối ưu."), the online promise (vibe coding), and five measured headline numbers |
| B · Kết quả | 4–10 | six experiments and isolation/emulator rules, classification by frequency and severity, build failures, "đã sửa xong" vs compile, real-device lessons, pre-registered test verdicts, and six conclusions |
| C · Nguyên nhân | 11–20 | measured before/after across three pillars — UI rendering (11–13), memory/size/search (14–16), security (17–19) — and live A/B demo (20) |
| D · Giải pháp | 21–26 | five-rung technique ladder from least to most technical (Bậc 1–5): what you do, what it achieved in our data, and where it stops to motivate the next rung |
| E · Kết thúc | 27 | three core points to remember |

**The classification rules (frozen, slide 5).** These are the rules the slide uses:
- **Frequency** is the share of the 66 E1 drafts written without the rules file that show the problem at least once, counted at source level. Drafts that do not build are included, and the notes say so. The build row is counted on the 100 valid drafts.
- **Validated counts** are used where the detector has them: P10 63/66 and P09 2/66, both from the v2 table. A count whose detector precision is not validated carries the label "chưa kiểm định độ chính xác" (P06).
- **Bins:** Phổ biến ≥ 50%; Ít gặp 10% to under 50%; Hiếm under 10%. With n = 66, items near 10% (P06, P08, S05) could fall on either side. The notes say so.
- **Severity is marked separately from frequency:**
  - Nghiêm trọng: credentials or personal data leak, or a security control is switched off (S01, S02, S05; under error pressure S03, X01). It also applies when fixing the problem alone moves a lab screen from missing to meeting a course target (P06, P09 memory; P09 frames; P13 search).
  - Đáng kể: a large measured cost that does not decide a target (P10 size; P08 frame time).
  - Không đo được chi phí: P01, P03, P04.
  - Nhẹ: P11.
- **Context counts stay outside the bins.** E4 X01 12/30 and S03 5/30, and E6 P08 11/12, are context rows. Accessibility, layout and dependency rules are on backup D5 only.

**Framing rules.** Advice is organised as a five-rung technique ladder from least to most technical (Bậc 1–5: AI self-review, rules file and structured spec, tool gates, measurement on a measure build, fixing known fault patterns), where each rung states what you do, what it achieved in our data, and where it stops to motivate the next rung. Never use a chain/links metaphor and never compare reader types. "vibe coding" appears only as the name of the phenomenon, on slide 2. Neither "chưa đo" nor the course-target scoreboard appears in the main talk (the scoreboard is backup D10 and the full unmeasured list is backup D1). Slides show no source captions, and the hype examples get no source list. Sources live in each slide's `aside.notes` as `path:line` and in the proof table. Limits are stated once: slide 4, slide 8 (pointer to D1) and D1.

**Design rules.** The canvas is 1920 × 1080, scaled to the window. A headline is a full sentence that makes the claim. Body text is at least 20 px at a 1280 px window, nothing may be clipped or leave the canvas, and content stays above the bottom part strip (y ≤ 980, strip at 1012; slide 1 is a poster without strip or page number). Each main slide carries one picture of evidence (an A/B video, a chart, verbatim code or a screenshot) and few numbers. Statistics such as p, Cliff's delta and intervals go to the notes and to D1–D12.

**Hype images (slide 2).** There are six 1280 × 720 YouTube thumbnails, `evidence/img/hype/hype-01.jpg` … `hype-06.jpg`. Their origin (title, channel, video id, fetch date) is recorded in [evidence/img/hype/README.md](evidence/img/hype/README.md) and never shown on a slide. The digits they contain are inside the images, so they need no proof rows.

**Live demo kit (slide 20).** [demo/](demo/README.md) runs A (the AI first draft) next to B (the tuned version) of the same screen, on the emulator. It uses one stdlib Python file, `demo.py`, and works on Windows (`py`) and Linux (`python3`):
- `setup` and `doctor` check the machine before the talk.
- `start` and `stop` handle the emulators.
- `list` and `run p09|p08|p06|p13|p10|p01|p0304` run the scenarios; `record` captures a run.

For the 120-second slot, run `run p09` at 0:00 and `run p13` at 1:00. Both run on the same GPU setting, so no emulator reboot is needed. The fallback is the slide-11, slide-12 and slide-14 videos plus `evidence/video/P13c-AB.mp4`, reading the numbers printed on the slide-20 cards. Live numbers are an illustration only; the study numbers on the slide are the reference. [demo/AGENT-BRIEF.md](demo/AGENT-BRIEF.md) briefs a presenter's coding assistant (English).

**Keys.**
- Navigation: → or Space next, ← back, digits + Enter jump.
- Views: P presenter window, O overview, N notes, T light/dark, B blackout, ? help.
- Print to PDF gives one page per slide, in final state.

The presenter clock target is the sum of the "~N giây" notes of the main slides, so it follows the deck when a slide changes. The same goes for the overrun warning and the "backup from slide N" help text. To show the deck over the tailnet: `python3 -m http.server <port> --bind <tailscale-ip> --directory optimization/study`.

### Slides: what each one claims and where its numbers are proven

The data-title column is exactly the slide's `data-title` in the deck. Script, outline, report and proof key on slide number plus data-title.

| # | data-title | What the slide claims | Experiment | Proof (rows in proof/deck-proof.csv; main sources) |
|---|---|---|---|---|
| 1 | Trông hoàn thiện, chưa tối ưu | Poster hook (no strip or page number): four one-prompt E6 apps look finished but are un-optimised; sets the topic (UI, memory, security on Android · ShelfTap) | E6 | 2 rows (labels only) |
| 2 | Lời hứa trên mạng | Six real thumbnails promise a whole app from one prompt; names the phenomenon (vibe coding) once | hype images | 1 row (labels only) |
| 3 | Năm con số | Five measured headline numbers, each pointing to its proof slide: 58/100 build fail (5), 60/66 security findings at 2,3× density (17), 90% janky frames on 2.000 items (11), 180 and 243 MB RAM vs 150 MB target (14, 15), 9/12 codex fixes fail to compile (7) | E1, E2, E5 | 31 rows; `e1-corpus/REPORT.md`, `e2-bench/REPORT.md`, `e5-optimizer/REPORT.md`, `CLAUDE.md` |
| 4 | Sáu thí nghiệm | Six experiments, criteria fixed before data, isolation check (623 transcripts), emulator numbers read as A/B only | E1–E6, isolation audit | 20 rows; `isolation-audit/AUDIT.md`, `e1-corpus/REPORT.md`, `e6-oneshot/REPORT.md`, `e2-bench/REPORT.md` |
| 5 | Phân loại lỗi | Classification matrix: frequency on the 66 drafts without a rules file vs severity; the most frequent problem is not the most dangerous one | E1 (context row E4, note E6) | 35 rows; `e1-corpus/REPORT.md`, `e4-pressure/REPORT.md`, `CLAUDE.md` |
| 6 | Bản nháp build lỗi | Drafts often fail to build (42/100); model choice (1/9 vs 7/9) and a structured spec (janky 99% → 19,6%, Kotlin LOC 1.164 → 524) matter most | E1, E6 | 17 rows; `e6-oneshot/REPORT.md`, `e1-corpus/REPORT.md` |
| 7 | “Đã sửa xong” chưa phải kết quả | "Đã sửa xong" is not a test result: all 168 seeded issues found in suggest-only mode, but 9/12 codex fixes do not compile and 21/24 runs alter decoys | E5 | 13 rows; `e5-optimizer/REPORT.md` |
| 8 | Thiết bị thật | Measure the measure build, not debug (125 vs 12–16 ms p90); a cover-URL bug (`10.0.2.2`) that only a real phone shows (7,0–7,2% → 1,5–2,7% janky); low-cost device limit on D1 | ShelfTap log | 25 rows; `../log/opt-ui-ux.md`, `e2-bench/REPORT.md` |
| 9 | Kết quả các phép kiểm định | Four of the five pre-registered tests confirmed (C1, C2, C3, C5), C4 not (17/148); H4 (0/2.105) and H5 (1/66) rejected | C1–C5, H4, H5 | 31 rows; `e1-corpus/REPORT.md`, `00-protocol.md`, `e2-bench/REPORT.md`, `e4-pressure/REPORT.md` |
| 10 | Sáu kết luận | One conclusion per experiment (E1 notes the rules-file trade-off between fewer warnings and fewer builds) | E1–E6 | 24 rows; `e1-corpus/REPORT.md`, `e3-security/REPORT.md`, `e4-pressure/REPORT.md`, `e5-optimizer/REPORT.md` |
| 11 | Danh sách dài dựng toàn bộ | A long list composed eagerly (P09): janky 90% → 1,3%, rows composed 2.000 → 31 | E2 | 16 rows; `e2-bench/REPORT.md`, `e1-corpus/REPORT.md` |
| 12 | Hiệu ứng trang trí | Shadows and gradients per card (P08): frame time +28% (C5 confirmed); the tuned card still misses 60 fps (48,65%); 8/66 drafts, 11/12 hype runs | E2, E1, E6 | 25 rows; `e2-bench/REPORT.md`, `e1-corpus/REPORT.md`, `00-protocol.md`, `e6-oneshot/REPORT.md` |
| 13 | Lỗi không có chi phí đo được | Rules with no measurable cost here (P01, P03+P04); Baseline Profile small on the emulator (−5,88%), nothing measurable on the phone: measure before fixing | E2, ShelfTap log | 28 rows; `e2-bench/REPORT.md`, `../log/opt-ui-ux.md` |
| 14 | Ảnh giải mã ở kích thước gốc | Covers decoded at full size (P06): decoded bytes 203 → 6,2 MB, RSS 180 → 60 MB against the 150 MB target | E2 | 23 rows; `e2-bench/REPORT.md`, `e1-corpus/REPORT.md` |
| 15 | Danh sách dài và bộ nhớ | The same eager list seen as memory: RSS 243 → 43 MB, heap 220 → 7,6 MB, TTID 1.901 → 573,1 ms | E2 | 23 rows; `e2-bench/REPORT.md` |
| 16 | Dung lượng và tìm kiếm | R8 cuts the APK by 90% (24,7 → 2,4 MB, TTID −8,7%); an FTS4 index brings the slowest query under 50 ms (96,63 → 37 ms; total 634,9 → 85,42 ms) | E2 | 27 rows; `e2-bench/REPORT.md`, `e2-bench/results/p13_equivalence.csv` |
| 17 | Lỗi bảo mật phổ biến đến mức nào | 60 of 66 drafts carry a security finding; density 2,3× the human apps (2,37 vs 1,02 per 1.000 lines, C3 confirmed); verbatim `SessionStore.kt` | E1 | 20 rows; `e1-corpus/REPORT.md` |
| 18 | Điều gì làm giảm lỗi bảo mật | The rules file lowers the share 31/33 → 17/34 (C1) and the rule count 5,85 → 2,82 (C2), but drafts that build also fall 20/33 → 10/34 (detailed prompt 12/33, descriptive); a longer prompt does not lower warnings (29/33); no difference among the 42 drafts that build | E1 | 39 rows; `e1-corpus/REPORT.md` |
| 19 | Khi gặp lỗi chặn | Under a blocking error assistants weaken security in 17/148 trials (C4 not confirmed), only Keystore (12/30) and cleartext (5/30); APK scanner precision 0,291 | E4, E3 | 28 rows; `e4-pressure/REPORT.md`, `e3-security/REPORT.md`, `00-protocol.md` |
| 20 | Demo: trước và sau tối ưu | Live demo, A next to B on the emulator (P09, P06, P08, P13); the study numbers on the cards are the reference | demo kit, E2 | 25 rows; `e2-bench/REPORT.md` |
| 21 | Năm bậc giải pháp | Overview of the five-rung technique ladder from least to most technical (Bậc 1–5): where one rung stops, the next begins | E2, E5, E6, ShelfTap log | 27 rows; `e2-bench/REPORT.md`, `../log/opt-ui-ux.md`, `e6-oneshot/REPORT.md`, `e5-optimizer/REPORT.md` |
| 22 | Bậc 1 · Trợ lý AI tự rà soát | Rung 1 (one prompt, illustrative PROMPT MẪU): suggest-only finds 168/168 seeded issues (detector evidence arm makes no measurable difference), but self-editing breaks 9/12 codex compiles, alters decoys in 21/24 runs, and relies on a given taxonomy | E5 | 21 rows; `e5-optimizer/REPORT.md` |
| 23 | Bậc 2 · Quy tắc và bản đặc tả | Rung 2 (rules file + structured spec, verbatim excerpts): lowers security warnings 31/33 → 17/34 (E1) and janky frames 99% → 19,6% (E6), but builds drop 20/33 → 10/34, 42 builders show no C1 difference, and the Baseline Profile rule breaks 3/3 codex runs | E1, E6 | 25 rows; `e1-corpus/REPORT.md`, `e6-oneshot/REPORT.md` |
| 24 | Bậc 3 · Để công cụ chấm | Rung 3 (tool gates: R8 `isMinifyEnabled`/`isShrinkResources` + `./gradlew compileDebugKotlin`): shrinks APK 24,7 → 2,4 MB and catches 9/12 broken codex fixes, but MobSF precision is 0,291 (all 30 true positives are config) and a passing build does not measure speed or RAM | E2, E3, E5 | 26 rows; `e2-bench/REPORT.md`, `e3-security/REPORT.md`, `e5-optimizer/REPORT.md` |
| 25 | Bậc 4 · Đo trước và sau | Rung 4 (Macrobenchmark cold start, interleaved A/B 12 iterations, measure build): Xiaomi p90 125 ms debug vs 12–16 ms measure build and rules out P01/P03/P04, but measurement only locates costs and Baseline Profile (−5,88% emulator) shows no phone gain | E2, ShelfTap log | 17 rows; `../log/opt-ui-ux.md`, `e2-bench/REPORT.md` |
| 26 | Bậc 5 · Sửa đúng mẫu lỗi | Rung 5 (fix known fault patterns): LazyColumn (90% → 1,3%, RAM 243 → 43 MB), Coil (180 → 60 MB), flat cards (62,87 → 49,07 ms), FTS4 (96,63 → 37 ms), Baseline Profile (−5,88%); each fix only covers its pattern (grid still 48,65%); points to exploratory D9 | E2, E7 | 33 rows; `e2-bench/REPORT.md` |
| 27 | Ba điều cần nhớ | Three core points to remember, ending on the five-rung ladder; no new numbers | — | 8 rows (labels only) |
| D1 | D1 · Giới hạn | Limits: emulator (−32,3% vs −78,2% p90), sample sizes (33–34), what is not measured (NFC tap < 1 s, low-cost device, browsing RAM with real covers, GPU memory, P12) | E2, E1 | 10 rows; `e2-bench/REPORT.md`, `e1-corpus/REPORT.md`, `00-protocol.md`, `CLAUDE.md` |
| D2 | D2 · Bảng Holm | Holm table for C1–C5 (m = 5) | C1–C5 | 36 rows; `e1-corpus/REPORT.md`, `00-protocol.md`, `e2-bench/REPORT.md`, `e4-pressure/results/c4_confirmatory.csv` |
| D3 | D3 · Bảng E2 đầy đủ | Full E2 table, seven factors plus the P01 contrast | E2 | 49 rows; `e2-bench/REPORT.md`, `e2-bench/results/c5_confirmatory.csv` |
| D4 | D4 · E4 chi tiết | E4 detail per error case and per assistant, Stack Overflow baseline (4/4), kappa 0,909 | E4 | 26 rows; `e4-pressure/REPORT.md` |
| D5 | D5 · Tần suất theo quy tắc | Frequency per rule and prompt style next to human apps before 2022 | E1 | 29 rows; `e1-corpus/REPORT.md` |
| D6 | D6 · E6 chi tiết | E6 builds per assistant and prompt arm (3 runs per cell) and median Kotlin LOC | E6 | 10 rows; `e6-oneshot/REPORT.md` |
| D7 | D7 · Kiểm tra môi trường | Isolation check of the generation runs (623 transcripts, 18.244 vs ~7.500 tokens) | isolation audit, E6 | 7 rows; `isolation-audit/AUDIT.md` |
| D8 | D8 · Cỡ chữ lớn và bố cục | Large font scale 2.0: all four D11 one-prompt apps break; ShelfTap 5/7 → 0/7, ATF 4 → 2, glass p90 9–11 → 18 ms | E6, ShelfTap metrics | 13 rows; `../ui-ux-metrics.md`, `../log/opt-ui-ux.md` |
| D9 | D9 · Phép thử quy mô (E7, thăm dò) | Exploratory E7a: the one-prompt apps with sample data amplified to 2.000 and 100.000 items; the only eagerly built list passes 150 MB at 2.000 (107,0 → 343,2 MB) and hangs at 100.000; no app starts under 1.500 ms at 100.000; all 10 amplified builds build | E7a | 64 rows; `e7-novel/results/e7a_summary.csv`, `e7-novel/NOTE.md`, `CLAUDE.md` |
| D10 | D10 · Đối chiếu mục tiêu môn học | Course-target scoreboard (measured rows only; unmeasured items on D1): fixed lab screens meet targets (search 37 ms, RAM 60/43 MB, list 1,3%), Xiaomi page open p90 8–16 ms and whole-app browsing RAM without covers 94–95 MB PSS; decorative grid misses 60 fps (48,65%) | E2, E6, ShelfTap log | 48 rows; `e2-bench/REPORT.md`, `../log/opt-ui-ux.md`, `CLAUDE.md`, `e6-oneshot/REPORT.md` |
| D11 | D11 · Bốn ứng dụng chỉ một prompt | Four one-prompt E6 apps (the slide-1 poster visual) look finished but are un-optimised: 6/12 build, 74–100% janky, 1.195–2.576 ms launch; RAM 66,8–102,8 MB only because sample data is 12 titles | E6 | 29 rows; `e6-oneshot/REPORT.md`, `e2-bench/REPORT.md`, `00-protocol.md` |
| D12 | D12 · Ba bài học từ bảng phân loại | Three lessons from slide 5: build first (58/100 E1, 6/12 E6), turn on R8 (63/66 missing; 24,7 → 2,4 MB), check rare but critical problems on purpose; only security density differs significantly from human apps (C3, slide 17) | E1, E2, E6 | 25 rows; `e1-corpus/REPORT.md`, `e6-oneshot/REPORT.md`, `e2-bench/REPORT.md` |

### Script, slide by slide

Copied from [presentation-script.md](presentation-script.md) by `build_playbook_html.py`. Edit the script file, then rebuild; never edit this block by hand. The timing table, the Q&A table ([Hỏi đáp dự kiến](presentation-script.md)), the preparation checklist and the number check stay in the script file.

<!-- SCRIPT:START -->
<!-- filled by build_playbook_html.py from presentation-script.md; edit the script, not this block -->

#### 1. Trông hoàn thiện, chưa tối ưu (0:00 đến 0:15) — slide 1

*Mở đầu · 15 giây · 35 từ*

**Lời nói.** Kính chào thầy cô và các bạn. Trông hoàn thiện, nhưng chưa tối ưu — hôm nay nhóm em trình bày về tối ưu giao diện, bộ nhớ và bảo mật trên Android với ShelfTap.

**Thao tác.** Dừng một nhịp ngay sau vế “Trông hoàn thiện, nhưng chưa tối ưu”, để khán giả nhìn bốn màn hình ứng dụng rồi chuyển sang slide 2.

**Lưu ý.** Slide áp phích không hiện thanh phần và số trang. Bốn ảnh nền là bốn ứng dụng E6 (chi tiết ở D11); bối cảnh là ShelfTap, ứng dụng thư viện thông minh dùng NFC của nhóm.

#### 2. Lời hứa trên mạng (0:15 đến 0:55) — slide 2

*Mở đầu · 40 giây · 84 từ*

**Lời nói.** Thầy cô và các bạn chắc hẳn đã quen với những bài đăng kiểu này: ảnh giao diện bóng bẩy kèm lời hứa chỉ một prompt — tức lời nhắc giao cho AI — là có ngay ứng dụng hoàn chỉnh. Hiện tượng đó gọi là vibe coding: mô tả bằng lời, nhận mã từ trợ lý lập trình AI rồi dùng ngay mà không kiểm tra lại. Nếu làm đúng theo cách đó trên Android thì kết quả thực tế sẽ ra sao?

**Thao tác.** Lướt tay qua lưới sáu ảnh minh họa, không đọc từng ảnh.

**Lưu ý.** Không nhắc tên nguồn hay đường dẫn ảnh trên sân khấu; nguồn lưu riêng trong evidence/img/hype/README.md.

#### 3. Năm con số (0:55 đến 1:40) — slide 3

*Mở đầu · 45 giây · 94 từ*

**Lời nói.** Câu trả lời gói gọn trong năm con số nhóm em đã đo, và phần sau sẽ chứng minh từng con số. Một, 58/100 bản nháp đầu bị build lỗi. Hai, 60/66 bản có cảnh báo bảo mật, mật độ gấp 2,3 lần ứng dụng do người viết. Ba, danh sách 2.000 mục dựng toàn bộ làm 90% khung hình trễ. Bốn, hai lỗi ảnh bìa và danh sách đẩy RAM lên 180 và 243 MB, vượt mục tiêu 150 MB. Năm, 9/12 bản được codex báo “đã sửa xong” không biên dịch được.

**Thao tác.** Bấm hiện lần lượt năm dòng từ trên xuống (build, bảo mật, khung hình, bộ nhớ, lời “đã sửa xong”), không dừng giải thích sâu vì mỗi dòng đã có mốc slide chứng minh bên phải.

**Lưu ý.** Số khung hình và mức dùng RAM đo trên máy ảo, đối chiếu bản nháp AI với bản sửa trên cùng màn hình; các mốc chứng minh nằm ở slide 5, 17, 11, 14, 15 và 7.

#### 4. Sáu thí nghiệm (1:40 đến 2:25) — slide 4

*Kết quả · 45 giây · 104 từ*

**Lời nói.** Bước vào phần kết quả, trước tiên là sáu thí nghiệm tạo ra các số liệu hôm nay, với tiêu chí đánh giá được chốt trước khi thu thập dữ liệu: E1 đo tần suất trên 101 bản nháp; E2 đối chứng A/B trên cùng màn hình; E3 quét 13 APK; E4 thử 148 lượt sửa lỗi chặn; E5 kiểm tra 14 lỗi cài sẵn; E6 chạy prompt kiểu trào lưu. Quét lại 623 bản ghi hội thoại, nhóm em xác nhận không lọt cấu hình riêng. Số đo máy ảo chỉ so tỷ lệ A/B, không phải tốc độ trên thiết bị thật.

**Thao tác.** Chỉ nhanh hai khung ở bên phải slide: Môi trường sạch và Máy ảo.

**Lưu ý.** Giới hạn máy ảo nói một lần ở đây; câu hỏi sâu về kiểm tra cô lập môi trường mở D7, về giới hạn mở D1.

#### 5. Phân loại lỗi (2:25 đến 3:15) — slide 5

*Kết quả · 50 giây · 100 từ*

**Lời nói.** Đây là kết quả trung tâm của E1. Đọc từ trái sang phải là tần suất trên 66 bản nháp không kèm tệp quy tắc (guardrail): phổ biến từ 50% trở lên, ít gặp từ 10% đến dưới 50%, và hiếm dưới 10%; chấm đỏ là lỗi nghiêm trọng. Cột bên trái là lỗi hay gặp nhất như build lỗi 58/100 hay thiếu R8 63/66, nhưng phần lớn chấm đỏ lại rơi vào hai cột sau — lưu token dạng rõ 19/66, tìm kiếm thiếu chỉ mục 15/66, dựng toàn bộ danh sách 2/66 và ảnh kích thước gốc 5/66.

**Thao tác.** Chỉ từ trái sang phải qua ba cột Phổ biến, Ít gặp, Hiếm, rồi khoanh vào các chấm đỏ và dòng bối cảnh E4 phía dưới.

**Lưu ý.** P06 (5/66) chưa kiểm định độ chính xác; dòng khi gặp lỗi chặn (12/30 bỏ xác thực khóa Keystore, 5/30 bật cleartext) thuộc bối cảnh E4, không gộp vào ba cột E1. Nếu được hỏi ưu tiên sửa gì trước, mở D12.

#### 6. Bản nháp build lỗi (3:15 đến 4:00) — slide 6

*Kết quả · 45 giây · 94 từ*

**Lời nói.** Nhìn kỹ vào lỗi phổ biến nhất vừa thấy: bản nháp đầu bị build lỗi. Ở E1, chỉ 42/100 bản build thành công. Hai yếu tố tạo chênh lệch rõ nhất là mô hình và bản đặc tả. Ở E6, dùng cùng công cụ và chỉ đổi mô hình, số bản build thành công tăng từ 1/9 với claude-haiku lên 7/9 với claude-sonnet. Thay prompt kiểu trào lưu bằng bản đặc tả có cấu trúc kéo tỷ lệ khung hình trễ từ 99% xuống 19,6% và lượng mã Kotlin từ 1.164 xuống 524 dòng.

**Thao tác.** Chỉ lần lượt 42/100, cặp 1/9 với 7/9, rồi cặp 99% xuống 19,6%.

**Lưu ý.** E6 là thí nghiệm khám phá với tối đa 3 lần chạy mỗi ô, không có kiểm định p; chi phí claude-haiku 0,11 USD so với claude-sonnet 0,48 USD. Chi tiết từng trợ lý AI ở D6.

#### 7. “Đã sửa xong” chưa phải kết quả (4:00 đến 4:45) — slide 7

*Kết quả · 45 giây · 86 từ*

**Lời nói.** Một rủi ro khác là tin vào lời “đã sửa xong”. Ở E5, khi ở chế độ chỉ đề xuất mà không sửa mã, trợ lý AI tìm đủ 168/168 lượt lỗi cài sẵn. Nhưng khi tự sửa, codex tuyên bố đã xong trong khi chạy biên dịch thật thì 9/12 bản lỗi do tự thêm plugin baselineprofile không dùng được. Đồng thời 21/24 lượt làm đổi cả mẫu đối chứng vốn đúng. Rõ ràng lời trợ lý AI không thể thay kết quả kiểm thử.

**Thao tác.** Chỉ hộp lời trả lời của codex ở trên và dòng lỗi biên dịch thật bên dưới.

**Lưu ý.** Nhấn mạnh: biên dịch và kiểm thử độc lập mới là căn cứ xác nhận, không phải lời của trợ lý AI.

#### 8. Thiết bị thật (4:45 đến 5:30) — slide 8

*Kết quả · 45 giây · 102 từ*

**Lời nói.** Trên thiết bị thật Xiaomi, ShelfTap cho thấy hai bài học máy ảo không lộ ra. Thứ nhất, đo bằng bản debug là đo sai đối tượng: mở trang sách ở bản debug lên tới 125 ms p90, còn bản build đo hiệu năng (cấu hình tương đương bản phát hành) chỉ 12 đến 16 ms, nằm gọn trong ngưỡng 16,67 ms của 60 fps. Thứ hai, địa chỉ ảnh bìa 10.0.2.2 chỉ truy cập được từ máy ảo, khiến thiết bị thật gọi lại liên tục; khi khắc phục, tỷ lệ khung hình trễ khi cuộn giảm từ 7,0–7,2% xuống 1,5–2,7%.

**Thao tác.** Chỉ bên trái (125 ms so với 12–16 ms) rồi sang bên phải (10.0.2.2).

**Lưu ý.** Số liệu trang chủ 7,0–7,2% xuống 1,5–2,7% đo trên bản debug; thiết bị cấu hình thấp xem giới hạn ở D1.

#### 9. Kết quả các phép kiểm định (5:30 đến 6:00) — slide 9

*Kết quả · 30 giây · 69 từ*

**Lời nói.** Về thống kê, trong năm phép kiểm định đăng ký trước sau hiệu chỉnh Holm, bốn phép được xác nhận là C1, C2, C3 và C5 — về hiệu quả tệp quy tắc, mật độ cảnh báo bảo mật và chi phí hiệu ứng trang trí. C4 không được xác nhận vì 17/148 dưới 15%. Hai giả thuyết mô tả H4 (0/2.105) và H5 (1/66) đều bị bác bỏ.

**Thao tác.** Chỉ nhóm bốn hàng xanh phía trên, tách khỏi C4 và hai hàng H4, H5.

**Lưu ý.** “Được xác nhận” nghĩa là thỏa mãn điều kiện thống kê đã đăng ký; bảng hiệu chỉnh Holm đầy đủ ở D2.

#### 10. Sáu kết luận (6:00 đến 6:35) — slide 10

*Kết quả · 35 giây · 76 từ*

**Lời nói.** Gom lại kết quả của sáu thí nghiệm: E1 chỉ 42/100 bản build thành công, tệp quy tắc giảm cảnh báo nhưng cũng giảm bản build thành công. E2: sửa danh sách giảm 69,9% khởi động, ba lỗi Compose không có chi phí đo được. E3: quét APK đạt 0,291. E4: 17/148 lượt nới lỏng bảo mật. E5: 9/12 bản codex sửa không biên dịch được. E6: bản đặc tả đưa khung hình trễ xuống 19,6%.

**Thao tác.** Lướt nhanh sáu hàng từ E1 đến E6 với nhịp dứt khoát rồi chuyển sang phần nguyên nhân.

**Lưu ý.** Không diễn giải lại chi tiết; dùng slide này khép phần kết quả và mở sang phần nguyên nhân phía sau (slide 11 đến 20).

#### 11. Danh sách dài dựng toàn bộ (6:35 đến 7:20) — slide 11

*Nguyên nhân · Giao diện · 45 giây · 106 từ*

**Lời nói.** Vậy nguyên nhân đằng sau các con số vừa nêu nằm ở đâu? Nhóm em đi qua ba trụ cột: giao diện, bộ nhớ và bảo mật. Ở trụ cột giao diện, đây là cùng một màn hình danh sách 2.000 lượt mượn với cùng thao tác cuộn. Bên trái là mã giữ nguyên từ bản nháp AI dùng Column với forEach, dựng hết 2.000 mục dù màn hình chỉ hiện vài mục, khiến 90% khung hình bị trễ. Bên phải đổi sang LazyColumn: số mục dựng giảm từ 2.000 xuống 31, tỷ lệ khung hình trễ giảm từ 90% xuống 1,3%, đủ đạt 60 fps.

**Thao tác.** Phát video P09c-AB.mp4, dừng ngắn ở đoạn bên trái bị khựng khi cuộn.

**Lưu ý.** Trung vị từ ít nhất 12 lần lặp xen kẽ A/B trên máy ảo; số đầy đủ 90,05% xuống 1,255%, lỗi hiếm 2/66 nhưng nghiêm trọng.

#### 12. Hiệu ứng trang trí (7:20 đến 8:05) — slide 12

*Nguyên nhân · Giao diện · 45 giây · 91 từ*

**Lời nói.** Lỗi thứ hai hay gặp khi dùng prompt kiểu trào lưu là dồn đổ bóng, chuyển sắc (gradient) và vẽ Canvas lên từng thẻ sách. Khi đổi bìa thành khối phẳng, thời gian xử lý khung hình p90 giảm từ 62,87 ms xuống 49,07 ms — mã AI chậm hơn 28%, qua đó xác nhận phép kiểm định C5. Tỷ lệ khung hình trễ giảm từ 70,52% xuống 48,65%, dù chưa đạt 60 fps trên máy ảo. Lỗi gặp ở 8/66 bản E1 nhưng vọt lên 11/12 lần chạy ở E6.

**Thao tác.** Phát video P08c-AB.mp4; nhấn cặp số 8/66 so với 11/12.

**Lưu ý.** P08 đo trên cấu hình kết xuất GPU swangle_indirect nên chỉ so trong nội bộ nhân tố này; bộ nhớ đồ họa chưa được đo.

#### 13. Lỗi không có chi phí đo được (8:05 đến 8:50) — slide 13

*Nguyên nhân · Giao diện · 45 giây · 97 từ*

**Lời nói.** Ngược lại, không phải quy tắc nào cũng tạo khác biệt đo được. Trên biểu đồ, chấm lệch trái vạch 0 nghĩa là bản sửa nhanh hơn: sửa danh sách giảm 69,9% thời gian khởi động. Nhưng thêm key danh sách (P01) và ổn định tham số (P03, P04) đều có khoảng tin cậy cắt qua 0. Baseline Profile giảm 5,88% khởi động trên máy ảo, còn trên thiết bị thật ở bản build đo hiệu năng (cấu hình tương đương bản phát hành) thì không có khác biệt đo được. Hãy đo trước khi tối ưu.

**Thao tác.** Chỉ dọc theo vạch 0 từ trên xuống dưới để đối chiếu hai dòng trên và hai dòng dưới.

**Lưu ý.** Khoảng tin cậy cắt qua 0 nghĩa là chưa ghi nhận chi phí trong kịch bản này (danh sách tĩnh, strong skipping mặc định), không phải luôn miễn phí.

#### 14. Ảnh giải mã ở kích thước gốc (8:50 đến 9:30) — slide 14

*Nguyên nhân · Bộ nhớ · 40 giây · 83 từ*

**Lời nói.** Sang trụ cột thứ hai: bộ nhớ. Với 15 ảnh bìa, bản nháp AI giải mã ở kích thước gốc 1500×2250 cho từng ô nhỏ, không thu nhỏ và không dùng bộ nhớ đệm. Riêng ảnh chiếm 203 MB, đẩy RAM cao nhất lên 180 MB — vượt ngưỡng dưới 150 MB. Khi đổi sang Coil AsyncImage để giải mã đúng kích thước ô và có bộ nhớ đệm, dung lượng ảnh còn 6,2 MB và mức dùng RAM giảm xuống 60 MB.

**Thao tác.** Phát video P06c-AB.mp4, chỉ đường mốc mục tiêu dưới 150 MB.

**Lưu ý.** Lỗi hiếm (5/66, chưa kiểm định độ chính xác) nhưng nghiêm trọng vì chỉ riêng lỗi này đã vượt ngưỡng 150 MB.

#### 15. Danh sách dài và bộ nhớ (9:30 đến 10:10) — slide 15

*Nguyên nhân · Bộ nhớ · 40 giây · 85 từ*

**Lời nói.** Nhóm em nhìn lại danh sách 2.000 lượt mượn ở slide 11 dưới góc độ bộ nhớ. Vì bản nháp AI dựng đủ 2.000 mục trước khung hình đầu tiên, mức dùng RAM vọt lên 243 MB, Java heap 220 MB, thời gian khởi động mất 1.901 ms. Khi chuyển sang LazyColumn, mức dùng RAM giảm xuống 43 MB, Java heap còn 7,6 MB và thời gian khởi động giảm xuống 573,1 ms. Một lỗi danh sách làm trượt cả khung hình lẫn bộ nhớ.

**Thao tác.** Chỉ ba cụm chỉ số: RSS 243 xuống 43 MB, Java heap 220 xuống 7,6 MB, TTID 1.901 xuống 573,1 ms.

**Lưu ý.** Số MB ít phụ thuộc máy ảo hơn số ms; bản nháp AI vượt ngưỡng 150 MB còn bản sửa đạt.

#### 16. Dung lượng và tìm kiếm (10:10 đến 10:55) — slide 16

*Nguyên nhân · Bộ nhớ · 45 giây · 91 từ*

**Lời nói.** Tiếp theo là dung lượng và tìm kiếm. Bật R8 thu nhỏ APK tới 90%, từ 24,7 MB còn 2,4 MB, và giảm thêm 8,7% thời gian khởi động. Với tìm kiếm ngoại tuyến 100.000 tựa sách, truy vấn LIKE chậm nhất mất 96,63 ms, vượt 50 ms; đổi sang chỉ mục FTS4 đưa mức chậm nhất xuống 37 ms và tổng thời gian truy vấn giảm từ 634,9 ms xuống 85,42 ms. Tuy nhiên, khung hình khi gõ không cải thiện vì nút thắt nằm ở danh sách kết quả.

**Thao tác.** Chỉ cột trái R8 rồi chuyển sang cột phải FTS4 và mốc 50 ms.

**Lưu ý.** LIKE khớp chuỗi con còn FTS4 khớp tiền tố từ nên khác tập kết quả ở 4/12 tiền tố.

#### 17. Lỗi bảo mật phổ biến đến mức nào (10:55 đến 11:40) — slide 17

*Nguyên nhân · Bảo mật · 45 giây · 100 từ*

**Lời nói.** Sang trụ cột thứ ba: bảo mật — nơi bản nháp AI khác biệt rõ nhất so với ứng dụng do người viết. Trong 66 bản nháp không kèm tệp quy tắc ở E1, có tới 60 bản chứa ít nhất một cảnh báo bảo mật. Trên 1.000 dòng mã, bản nháp AI có trung vị 2,37 cảnh báo, gấp 2,3 lần mức 1,02 ở ứng dụng do người viết trước năm 2022; phép kiểm định C3 xác nhận mức chênh này. Phía dưới là đoạn mã nguyên văn: token đăng nhập được ghi thẳng ở dạng rõ vào SharedPreferences.

**Thao tác.** Chỉ hai dòng `putString` trong khung mã `SessionStore.kt` ở phần dưới slide.

**Lưu ý.** Nói trung lập: bản nháp AI có mật độ cảnh báo bảo mật cao hơn, không nói AI gây ra; đối chứng gồm 20 ứng dụng trước 30/11/2022.

#### 18. Điều gì làm giảm lỗi bảo mật (11:40 đến 12:25) — slide 18

*Nguyên nhân · Bảo mật · 45 giây · 100 từ*

**Lời nói.** Vậy cách nào làm giảm cảnh báo bảo mật lúc sinh mã? Ở E1, prompt chức năng chi tiết vẫn có 29/33 bản bị cảnh báo so với 31/33 ở prompt ngắn. Tệp quy tắc kéo tỷ lệ này xuống 17/34 và giảm số loại vấn đề từ 5,85 xuống 2,82, xác nhận C1, C2. Nhưng đánh đổi là bản build thành công sau tối đa 3 lần sửa cũng giảm từ 20/33 xuống 10/34, còn prompt chi tiết là 12/33. Xét riêng 42 bản build thành công, tỷ lệ có cảnh báo không đổi: vẫn cần bước kiểm tra.

**Thao tác.** Chỉ hai cụm biểu đồ bên trái (cảnh báo bảo mật và bản build thành công) rồi sang hai khung bên phải.

**Lưu ý.** Tỷ lệ build thành công (20/33, 12/33, 10/34) là số mô tả; C2 tính trên cả bốn nhóm quy tắc; trong 42 bản build thành công, chênh lệch C1 bằng +0,000 còn C2 vẫn còn (+3,829).

#### 19. Khi gặp lỗi chặn (12:25 đến 13:10) — slide 19

*Nguyên nhân · Bảo mật · 45 giây · 93 từ*

**Lời nói.** Rủi ro bảo mật còn lộ ra khi sửa lỗi chặn. Ở E4, gặp UserNotAuthenticatedException của Android Keystore (kho khóa của hệ thống), trợ lý AI có thể xóa yêu cầu xác thực — đổi true thành false — để lỗi biến mất. Trên 148 lượt sửa có 17 lượt nới lỏng cơ chế bảo mật, dưới ngưỡng 15% nên C4 không được xác nhận; tất cả rơi vào Keystore 12/30 và cleartext 5/30. Ở E3, công cụ quét APK đạt độ chính xác 0,291 và không phát hiện lỗi trong mã nguồn.

**Thao tác.** Chỉ dòng đỏ bị xóa và dòng xanh thêm vào trong diff `TokenVault.kt`.

**Lưu ý.** Không xếp hạng trợ lý AI theo 12/48, 5/50, 0/50 vì công cụ và mô hình bị lẫn (mở D4 nếu được hỏi).

#### 20. Demo: trước và sau tối ưu (13:10 đến 15:10) — slide 20

*Nguyên nhân · Demo trực tiếp · 120 giây (chuyển và nhận lại lượt: ~25 giây, 65 từ; phần demo trực tiếp: ~95 giây)*

**Lời nói.** *[Chuyển lượt]* Để thầy cô và các bạn quan sát trực tiếp chứ không chỉ nhìn bảng số, thành viên nhóm em sẽ chạy demo song song bản nháp AI và bản tối ưu trên máy ảo. Xin mời bạn. *[Nhận lại lượt]* Cảm ơn phần demo. Sự khác biệt khi cuộn và tìm kiếm thể hiện rất rõ; vậy làm cách nào ngăn và xử lý những lỗi này?

**Thao tác.** Chuyển micro cho thành viên demo (thứ tự gợi ý: P09 danh sách 2.000 lượt mượn, P06 ảnh bìa, P08 thẻ trang trí, P13 tìm kiếm 100.000 tựa sách); nếu máy ảo lỗi, mở lại video ở slide 11, 12, 14.

**Lưu ý.** Số đo trên máy ảo laptop chỉ để minh họa; số tham chiếu của nghiên cứu là bốn mốc trên slide (90% → 1,3%; 180 → 60 MB; 62,87 → 49,07 ms; 96,63 → 37 ms). Chi tiết chạy demo ở optimization/study/demo/README.md.

#### 21. Năm bậc giải pháp (15:10 đến 15:40) — slide 21

*Giải pháp · 30 giây · 63 từ*

**Lời nói.** Chuyển sang phần giải pháp. Nhóm em xếp thành năm bậc từ đơn giản nhất đến chuyên sâu nhất về kỹ thuật: nhờ trợ lý AI tự rà soát, viết quy tắc và bản đặc tả trước, để công cụ chấm, đo trên bản build đo hiệu năng, và sửa đúng mẫu lỗi trong mã nguồn. Bậc trước dừng ở đâu, bậc sau tiếp tục từ đó.

**Thao tác.** Bấm hiện lần lượt năm bậc từ trái sang phải dọc theo trục “đơn giản” đến “chuyên sâu về kỹ thuật”.

**Lưu ý.** Năm mốc số trên từng bậc đều đã xuất hiện ở phần trước: 168/168 (slide 7), 99% → 19,6% (slide 6), APK 24,7 → 2,4 MB (slide 16), p90 125 so với 12–16 ms (slide 8), RAM 243 → 43 MB (slide 15).

#### 22. Bậc 1 · Trợ lý AI tự rà soát (15:40 đến 16:20) — slide 22

*Giải pháp · 40 giây · 86 từ*

**Lời nói.** Bậc 1 đơn giản nhất: dùng một prompt nhờ trợ lý AI liệt kê rủi ro bảo mật và chỗ chưa tối ưu theo tệp và dòng, chưa sửa mã. Ở E5 chế độ chỉ đề xuất, cách này tìm đủ 168/168 lượt lỗi cài sẵn; kèm kết quả bộ quét tĩnh không tạo khác biệt đo được. Bậc 1 dừng khi tự sửa: 9/12 bản codex không biên dịch được, 21/24 lượt đổi mẫu đối chứng, và prompt đã nêu sẵn danh mục lỗi.

**Thao tác.** Bấm lần lượt ba cột Làm gì, Kết quả đo được và Dừng ở đâu, dừng ngắn ở câu nối sang Bậc 2.

**Lưu ý.** Prompt trên slide ghi nhãn PROMPT MẪU chỉ để minh họa, không phải nguyên văn prompt E5; độ chính xác báo cáo 0,82 đến 0,94; nhánh kèm kết quả bộ quét tĩnh có p từ 0,25 đến 1,0 trên 6 cặp.

#### 23. Bậc 2 · Quy tắc và bản đặc tả (16:20 đến 17:00) — slide 23

*Giải pháp · 40 giây · 93 từ*

**Lời nói.** Sang bậc 2: đặt tệp quy tắc ở gốc dự án và dùng bản đặc tả có cấu trúc ghi rõ định nghĩa xong là build thành công. Tệp quy tắc kéo cảnh báo bảo mật từ 31/33 xuống 17/34 ở E1; bản đặc tả kéo tỷ lệ khung hình trễ từ 99% xuống 19,6% ở E6. Nhưng số bản build thành công cũng giảm từ 20/33 xuống 10/34, riêng 42 bản build thành công thì tỷ lệ cảnh báo không đổi, và quy tắc Baseline Profile làm cả 3 bản codex build lỗi.

**Thao tác.** Chỉ hai trích dẫn ở cột Làm gì rồi bấm sang hai cột kết quả và giới hạn bên phải.

**Lưu ý.** Hai trích dẫn trên slide là nguyên văn tiếng Anh từ tệp quy tắc và bản đặc tả của nhóm; tỷ lệ build thành công 20/33 → 10/34 là số mô tả; chỉ viết prompt dài hơn mà không kèm tệp quy tắc thì không cải thiện kết quả (29/33).

#### 24. Bậc 3 · Để công cụ chấm (17:00 đến 17:40) — slide 24

*Giải pháp · 40 giây · 89 từ*

**Lời nói.** Bậc 3 để công cụ chấm thay cho lời trợ lý AI: bật R8 trong build.gradle.kts cho bản phát hành và chạy biên dịch thật sau mỗi lần sửa. Nhờ đó APK giảm từ 24,7 xuống 2,4 MB, còn biên dịch thật bắt ngay 9/12 bản codex sửa lỗi mà bộ kiểm tra tĩnh cho qua. Nhưng bậc 3 dừng ở đây: MobSF đạt độ chính xác 0,291 và cả 30 cảnh báo đúng đều là cấu hình; build thành công chưa nói ứng dụng nhanh hay tốn RAM.

**Thao tác.** Chỉ hai khung mã R8 và lệnh biên dịch, rồi chuyển qua cột Kết quả và Dừng ở đâu.

**Lưu ý.** R8 cũng giảm 90,4% dung lượng và 8,7% thời gian khởi động; token dạng rõ chỉ bộ quét tĩnh của nhóm ghi nhận; Android Lint không được dùng trong nghiên cứu này nên không nêu số.

#### 25. Bậc 4 · Đo trước và sau (17:40 đến 18:20) — slide 25

*Giải pháp · 40 giây · 86 từ*

**Lời nói.** Lên bậc 4: đo bằng Macrobenchmark, khởi động nguội, chạy xen kẽ A và B 12 lần lặp trên bản build đo hiệu năng. Đo đúng đối tượng cho thấy khác biệt ngay: mở trang sách trên Xiaomi ở bản debug là 125 ms p90, bản build đo hiệu năng chỉ 12–16 ms; số đo cũng loại P01, P03, P04 vì không có chi phí đo được. Tuy nhiên đo không tự sửa mã, và Baseline Profile giảm 5,88% trên máy ảo nhưng không đổi trên thiết bị thật.

**Thao tác.** Chỉ ba yêu cầu đo ở cột trái, nhấn cặp 125 → 12–16 ms rồi nối sang Bậc 5.

**Lưu ý.** Số ms trên máy ảo chỉ dùng để so A với B; thiết bị thật của nghiên cứu là Xiaomi flagship, giới hạn đầy đủ ở D1.

#### 26. Bậc 5 · Sửa đúng mẫu lỗi (18:20 đến 19:00) — slide 26

*Giải pháp · 40 giây · 93 từ*

**Lời nói.** Bậc 5 chuyên sâu nhất về kỹ thuật là sửa đúng từng mẫu lỗi đã biết, nơi tạo cải thiện lớn nhất. LazyColumn kéo danh sách 2.000 mục từ 90% xuống 1,3% khung hình trễ và RAM từ 243 xuống 43 MB. Coil AsyncImage kéo RAM ảnh bìa từ 180 xuống 60 MB. Bìa phẳng giảm p90 từ 62,87 xuống 49,07 ms; FTS4 đưa truy vấn chậm nhất từ 96,63 xuống 37 ms; Baseline Profile giảm 5,88%. Tuy vậy mỗi cách chỉ sửa đúng mẫu đó — lưới thẻ vẫn 48,65%, chưa đạt 60 fps.

**Thao tác.** Lướt nhanh bảng sáu hàng bên trái rồi dừng ở hộp giới hạn và gợi ý thăm dò D9 bên phải.

**Lưu ý.** Mọi số trong bảng là so A/B trên máy ảo; phần thăm dò ở D9 phóng dữ liệu mẫu lên quy mô thật để ngưỡng mục tiêu quyết định thay cho trợ lý AI (công cụ chặn diff mới đánh giá hồi cứu).

#### 27. Ba điều cần nhớ (19:00 đến 19:30) — slide 27

*Kết thúc · 30 giây · 77 từ*

**Lời nói.** Khép lại bài, nhóm em chốt ba điểm cốt lõi cần nhớ. Một, trông hoàn thiện chưa phải đã tối ưu: hãy đo với dữ liệu thật. Hai, tần suất không phản ánh mức nghiêm trọng: cần kiểm tra riêng lỗi bộ nhớ và bảo mật. Ba, tin bản build và số đo, không tin lời “đã sửa xong”, đi đủ năm bậc giải pháp. Xin cảm ơn, mời thầy cô và các bạn đặt câu hỏi.

**Thao tác.** Giữ slide kết khi hỏi đáp; nhấn phím O để mở lưới chọn nhanh slide dự phòng D1 đến D12.

**Lưu ý.** Không đưa số mới ở slide kết. Các slide dự phòng D1 đến D12 nằm ngay sau slide kết (mục tiêu môn học ở D10, bốn ứng dụng chỉ một prompt ở D11, ưu tiên sửa lỗi ở D12).

<!-- SCRIPT:END -->

## 6b. The plain-language report

[report/index.html](report/index.html) is one Vietnamese web page for readers without a presenter. It follows the deck's order and says more than a slide can:
- Opening hook, online promise, and the five headline numbers.
- A short summary per experiment (E1 to E6), the classification table with its rules, build and self-fix findings, real-device lessons, and the verdict table of the pre-registered tests (including C4).
- For each pillar (UI rendering, memory, security): the problem, the measured before/after, why it happens, and the fix, plus the four-scenario A/B comparison.
- The five-rung technique ladder (Bậc 1–5) from least to most technical.
- Three core points and the limits (plus backup material such as the full E2 table and the course-target scoreboard on D10).

Every number in it is a number of the deck. Its owner checks it by a set difference against the deck and by rendering at 1280 and 390 px. The framing rules of section 6 apply: advice is organised as the five-rung technique ladder (Bậc 1–5), and there is no sources section; the lead-in sources stay in [report/sources-leadin.md](report/sources-leadin.md) as a record.

| Report section | Deck slides |
|---|---|
| Mở đầu: lời hứa trên mạng và năm con số đo được | 1–3, D11 |
| Kết quả: sáu thí nghiệm, bảng phân loại, build lỗi, tự sửa, thiết bị thật và kiểm định | 4–10, D2, D7, D8, D10, D12 |
| Nguyên nhân: giao diện, bộ nhớ, bảo mật và đối chiếu trực tiếp bốn kịch bản | 11–20, D3, D4, D5 |
| Giải pháp: thang năm bậc kỹ thuật (Bậc 1 đến Bậc 5) | 21–26, D9 |
| Kết thúc: ba điểm cốt lõi cần nhớ và giới hạn của nghiên cứu | 27, D1 |

## 7. Proof of every number in the deck

- [proof/deck-proof.csv](proof/deck-proof.csv) has 956 rows (518 direct, 109 derived, 329 label) covering 929 values across all 39 slides (27 main + D1–D12: visible text, speaker notes, chart data attributes and bar lengths). Columns: `slide,value,kind,claim,source,line,quote,formula,note`.
  - `direct`: the number is on `source:line`, inside the verbatim `quote`.
  - `cited`: the same, from an external source recorded in references.md (not used by the current deck).
  - `derived`: `formula` recomputes the shown value from proven inputs. Examples: k/66 as naive + detailed (`8+11`); MB as KB / 1024; APK bytes / 1 000 000; Holm as raw p × (5 − rank + 1); "gấp 2,3 lần" as `2.37/1.02`; P08 11/12 counted from `e6-oneshot/results/findings.csv`.
  - `label`: not data, and `note` says what it is. Examples: a line number in a notes source pointer, a "~N giây" timing, a file name, a slide cross-reference, a ladder rung or rule number, an axis tick, a version, an address. A value that is also data anywhere on that slide gets a sourced row instead of a label.
- [proof/check_deck_proof.py](proof/check_deck_proof.py) (stdlib) reads the deck and lists every number on every slide. It fails unless each number has a row whose quote is really on that line and contains the number (after rounding), and each formula reproduces the shown value. It also fails on any row whose number is no longer on its slide (stale).

```
python3 -I optimization/study/proof/check_deck_proof.py            # whole deck; exit 0 = 0 bad, 0 stale, 0 unproven
python3 -I optimization/study/proof/check_deck_proof.py --list 5   # numbers on slide 5 that need a row
python3 -I optimization/study/proof/check_deck_proof.py --slide 5  # check one slide
```

- [proof/ISSUES.md](proof/ISSUES.md) records what each proof pass found and changed. The 2026-11-28 talk-flow rebuild re-ordered the deck from 25 + D1–D9 to 27 + D1–D12 (old → new slide map in its top section) and verified all 956 rows with 0 bad, 0 stale, 0 unproven. Earlier passes recorded the `div` fix for the classification slide (now slide 5, formerly slide 4) and the whole-app RAM update (94–95 MB PSS on Xiaomi, now on backup D10).
- **Checker limits to keep in mind:**
  - Never nest a `<section>` inside a slide.
  - Scientific notation in notes (`1,84e5`) is cut at the "e"; its leading digit gets a label row that points to the proving row.
  - A quote taken from a CSV line must isolate the value between commas, because `numbers_in` reads `148,17,0.1149` as one number.
  - The matcher accepts ×100, ×0.01, ×1000, ×0.001, a sign flip and rounding. Keep every quote to the narrowest fragment that holds the claimed number.
- Adding or changing a number on a slide means adding or fixing its row in the same change, then running the checker.
- Use primary records first: a results CSV, a REPORT.md, or the measurement log. Never use the orchestrator's inventory. ARTICLE.md is a valid source only for values computed there, and the row says so.
- The script has its own number check, the "Kiểm số liệu" heredoc at the end of presentation-script.md. It checks that every spoken number is on that slide's face or in its notes: `python3 -I - presentation.html presentation-script.md`, run from `optimization/study` with the heredoc as stdin.

## 8. Language (Vietnamese)

The deck, script, outline, report and article share one binding glossary and register. Use it in any group material too. PLAYBOOK and README prose stays English; the embedded script is Vietnamese.

| concept | use | never |
|---|---|---|
| prompt | prompt (first use: "prompt, tức lời nhắc giao cho AI") | câu lệnh, đề bài (for prompt) |
| one-shot prompt | chỉ một prompt (one-shot prompt) | câu lệnh một lần |
| AI coding assistant | trợ lý lập trình AI, then "trợ lý AI" | "nó" for the assistant |
| first draft | bản nháp đầu | |
| un-optimized | chưa tối ưu | |
| slow frame / jank | khung hình trễ (jank); tỷ lệ khung hình trễ | giật 89%, mắt thấy giật |
| frame time p90 | thời gian xử lý khung hình (p90) | thời gian dựng khung hình |
| startup | thời gian khởi động (TTID) | |
| memory | mức dùng RAM (RSS); Java heap | bộ nhớ chiếm dụng |
| build (noun, result) | bản build; build thành công / build lỗi | "biên dịch" for the whole Gradle build |
| compile (step) | biên dịch | |
| app | ứng dụng | "app" in running text |
| rules file | tệp quy tắc (guardrail) | tệp hướng dẫn bảo mật as a second name |
| our detector | bộ quét tĩnh của nhóm ... ghi nhận | bộ dò ... bắt được |
| finding | cảnh báo | |
| rule (of the detector/taxonomy) | quy tắc | luật |
| pre-registered | đăng ký trước; "tiêu chí đánh giá được chốt trước khi thu thập dữ liệu" | luật chấm khóa |
| supported / not supported / inconclusive | được xác nhận / không được xác nhận / chưa đủ cơ sở kết luận | |
| Android Keystore | Android Keystore (kho khóa của hệ thống) | két bảo mật |
| weaken security | nới lỏng cơ chế bảo mật | gỡ bảo mật cho hết lỗi |
| measure build | bản build đo hiệu năng (cấu hình tương đương bản phát hành) | bản đo đúng cách |
| list items | mục | dòng (for list items) |
| loan | lượt mượn | |
| seeded issue / decoy | lỗi cài sẵn / mẫu đối chứng | gieo, mồi |
| human-written app | ứng dụng do người viết | |
| unvalidated precision | chưa kiểm định độ chính xác | |
| common / uncommon / rare / critical / notable | phổ biến / ít gặp / hiếm / nghiêm trọng / đáng kể | |
| emulator / real device | máy ảo / thiết bị thật | |

Register: slides, report, article = neutral written Vietnamese ("nhóm", "nghiên cứu này"), full sentences where a claim is made, no dangling "nó". Script = spoken, polite student register ("nhóm em", "thầy cô và các bạn"), short sentences. English names developers use (LazyColumn, R8, Baseline Profile) are kept and glossed once. In the article, terms printed inside the frozen figures (for example "guardrail") keep a matching gloss.

**Term decisions from the review rounds (2026-11-28).** These hold everywhere the concept appears:

| Concept | Use | Replaced |
|---|---|---|
| hype prompt | prompt kiểu trào lưu | the "quảng cáo" wording |
| slow frame (short form) | khung hình trễ, never shortened | "khung trễ" |
| assistant | trợ lý AI (bare "trợ lý" only in fixed names such as "trợ lý agy") | bare "trợ lý" |
| error case of E4 | kịch bản lỗi | "ca", "ca lỗi" |
| E5 suggest-only control level | chế độ chỉ đề xuất | the shortened "mức chỉ …" form |
| the assistant that applies the fix | trợ lý sửa mã | "bên sửa" |
| self-checkout target | Chạm thẻ NFC tự mượn sách · thiết bị cấu hình thấp | short and colloquial forms |
| stub home directory | thư mục HOME giả lập | the literal "nhà giả" |
| GPU path of the emulator | cấu hình kết xuất GPU | "đường GPU", "cấu hình GPU" |
| blind raters, blind re-read | hai người chấm độc lập (ẩn nhãn); chấm lại ẩn nhãn | "chấm mù" |
| regression guardrail (G7) | chốt chặn kiểm tra (hồi quy) tự động | the literal "lan can" |
| statistical endpoint | tiêu chí xác nhận | "điểm cuối" |
| fixer self-report | báo cáo tự đánh giá | the literal "tự khai" |
| mechanical repair rule | cấu hình theo quy tắc cố định | "cơ học" |
| hallucinated dependency (H4) | tham chiếu thư viện không tồn tại | "tọa độ" calques |
| closing slide | Ba điểm cốt lõi cần nhớ | earlier takeaway wording |

Figures that still print an older term are glossed in their captions (section 5). [proof/LANGUAGE-REVIEW.md](proof/LANGUAGE-REVIEW.md) records the three review rounds: reviewer, counts, what was applied and what was rejected, and why.

## 9. Group material — how it fits

| Material | Status | How to use it |
|---|---|---|
| Group outline `De_cuong_AI_Coding_UI_UX_Hieu_nang_Bao_mat_ban_mo_rong.docx` (Cuong) | the plan the study implements; ARTICLE.md §1.6 and §3.7 map it | source of the four original questions |
| `optimization/TieuLuan-ToiUu-Android.docx`, `optimization/Script-ThuyetTrinh.docx` (Huy, commit e4259e1) | a technique-by-technique essay and a 25-minute script. Their numbers are the B (tuned) side of E2, plus the ShelfTap MobSF rows of E3 and the product's security design | fits as the baseline layer (ARTICLE Chapter 2) and as product security. Each B number needs its A (before) number next to it. It must not present P03+P04, Baseline Profile or card effects as wins the A/B data does not show (slides 12 and 13). Offline FTS search exists only in `android/optlab`, not in the member app |
| ShelfTap UI/UX log (teammate PR, merged 2026-11-28) | real-phone measurements in `optimization/log/opt-ui-ux.md` and `optimization/ui-ux/measure/` | source of slides 8, 13, D8 and D10; re-locate line numbers after each merge |
| Canva deck (group) | not reviewed: Canva blocks automated browsers | export to PDF into the repo to review |

## 10. Checklists

**Before presenting**
- [ ] `python3 -I optimization/study/proof/check_deck_proof.py` exits 0.
- [ ] The script's "Kiểm số liệu" check exits 0, and the timing table total stays between 15:00 and 20:00.
- [ ] Demo machine: `py demo.py doctor` passes; the emulator is started and `run p09` has run once (demo/README.md §4). The fallback videos for slide 20 (slides 11, 12, 14) are at hand.
- [ ] The deck opens offline on the presenting machine (fonts are embedded). The presenter window (P) works there and its clock shows the 19:30 target.
- [ ] Light theme on a projector; dark only in a dark room.
- [ ] Rehearse to 19:30 or less (the script matches the 19:30 slide-note timings).
- [ ] Know the Q&A answers in presentation-script.md, each mapped to a backup slide: emulator vs real phone (D1), Holm (D2), +28,13% vs −22% (D3), E4 raters and per-assistant counts (D4), human apps (D5, slide 17), E6 per assistant (D6), isolation (D7), large font scale (D8), what is new: the one-prompt apps at real data size (D9), course-target scoreboard (D10), four one-prompt apps (D11), three classification lessons (D12).

**Before changing the deck**
- [ ] Every changed number has its proof row; the checker exits 0.
- [ ] Script, outline, report and this playbook follow the change, keyed on slide number and data-title.
- [ ] Run the headless layout check at 1280 and 1920 px: nothing outside the canvas, content bottom at most 980 px, smallest text 20 px at 1280.

**After editing the script or this playbook**
- [ ] `python3 build_playbook_html.py` (in `optimization/study`) rewrites the script block and PLAYBOOK.html. `python3 build_playbook_html.py --check` exits 0. It needs `markdown-it-py`; if missing, run `uv run --no-project --with markdown-it-py python3 build_playbook_html.py`.
- [ ] Banned-word grep is empty for PLAYBOOK.md, README.md and the talk files (list in the task contract, Framing).

**Before submitting the article**
- [ ] Every number in ARTICLE.md still matches its CSV (orchestrator check, README.md "Rules").
- [ ] `python3 optimization/study/figures/make_figures.py` rebuilds all figures without change.
- [ ] Literature searches re-run (README.md open item); two exploratory E2 runs settled.
- [ ] Protocol sha256 still matches 00-protocol.sha256.
