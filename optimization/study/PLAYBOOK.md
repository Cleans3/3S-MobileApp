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

Five pre-registered confirmatory tests (C1–C5, Holm-corrected) carry the conclusions; everything else is descriptive. An exploratory follow-up, **E7**, turns two of the gaps into tool-enforced checks (section 4). The talk opens with the online promise ("one prompt, one app"), ranks the problems by frequency and severity, shows the measured before/after for three pillars (UI rendering, memory, security), and closes on three points: looking finished is not being optimised; frequency is not severity; trust the build and the measurement, not "đã sửa xong".

## 2. Questions, hypotheses, verdicts

Research questions RQ1–RQ6 and hypotheses H1–H6, G1–G3, B1 are defined in [00-protocol.md](00-protocol.md) §2–§3 and restated in ARTICLE.md §1.4. Only the five below are confirmatory.

| Test | Hypothesis | Measure | Verdict | Where |
|---|---|---|---|---|
| C1 | G1: the rules file lowers the share of runs with ≥1 security (S) finding | E1 primary endpoint 1 | supported | ARTICLE Tables 5.6, 5.26, 5.27 · slides 14, 20, D2 |
| C2 | G2: the rules file lowers the number of distinct rules per run | E1 primary endpoint 2 | supported | Table 5.6 · slides 14, 20, D2 |
| C3 | B1: assistants emit S findings at a higher density than humans | S findings per 1000 Kotlin lines vs human apps before 30/11/2022 | supported | Figure 5.9, Table 5.26 · slides 13, 20, D2 |
| C4 | H2: assistants fix errors by weakening security | share of E4 trials scored `insecure-workaround` vs 0,15 | **not supported** | Figure 5.16 · slides 15, 20, D2, D4 |
| C5 | H3: decorative effects cost frame time | P90 CPU frame time, P08 A vs B | supported | Table 5.19 · slides 8, 20, D2 |

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

**E7 (not in the talk).** [e7-novel/NOTE.md](e7-novel/NOTE.md) goes one step past measuring known problems. It names six gaps the E1–E6 data exposes, lists eight candidate new problems or solutions, and builds two prototypes:
- a scale probe: run a generated app at realistic data sizes, because toy data hides P06, P09 and P13;
- a diff guard: a tool, not the assistant, judges an error fix for weakened security.

Both prototypes are unit-tested (`test_prototypes.py`) and evaluated retrospectively on the E1, E4, E5 and E6 corpora. The emulator phase (E7a, E7b) is planned in §6 of the note and not yet run. Quote E7 only as exploratory desk work.

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
| [presentation.html](presentation.html) | the deck: 25 main slides + 8 backup (D1–D8), Vietnamese, one offline HTML with embedded fonts. It needs [evidence/](evidence/) (videos, hype thumbnails, screenshots) and [e6-oneshot/img/](e6-oneshot/img/) next to it | speaker notes sum to 19:30 |
| [presentation-script.md](presentation-script.md) | the spoken script, one section per main slide (Lời nói, Thao tác, Lưu ý), a timing table, Q&A mapped to D1–D8, a preparation checklist and a number check. The per-slide sections are copied below | 19:15 at 130 words/min |
| [slides-outline.md](slides-outline.md) | slide-by-slide outline: message, evidence, source | — |
| [demo/README.md](demo/README.md) | the live demo kit for slide 16 (Vietnamese) | 120 s slot |

### How the talk is built

The talk is 15 to 20 minutes for lecturers and classmates. It runs in nine blocks:

| Block | Slides | Job |
|---|---|---|
| Hook | 1–3 | the question, the online promise, and what the same prompt really produces |
| Classification | 4–5 | the centrepiece: problems ranked by frequency and marked by severity, then three lessons |
| Method | 6 | six experiments, criteria fixed before data, the isolation check, the emulator caveat (said once) |
| Pillar 1, UI rendering | 7–9 | P09 eager list, P08 decorative effects (C5), rules with no measurable cost |
| Pillar 2, memory and resources | 10–12 | P06 full-size covers, P09 memory, R8 size and P13 search |
| Pillar 3, security | 13–15 | prevalence and density (C3), what lowers it (C1, C2), behaviour under a blocking error (C4) and APK scanners |
| Demo | 16 | A next to B live, by another group member |
| Build and fix | 17–18 | drafts that do not build; "đã sửa xong" that does not compile |
| Conclusions | 19–21 | six conclusions, the test verdicts, the course-target scoreboard |
| Practice | 22–25 | real device; advice per development phase at two levels, Tối thiểu and Nâng cao; three points |

**The classification rules (frozen, slide 4).** These are the rules the slide uses:
- **Frequency** is the share of the 66 E1 drafts written without the rules file that show the problem at least once, counted at source level. Drafts that do not build are included, and the notes say so. The build row is counted on the 100 valid drafts.
- **Validated counts** are used where the detector has them: P10 63/66 and P09 2/66, both from the v2 table. A count whose detector precision is not validated carries the label "chưa kiểm định độ chính xác" (P06).
- **Bins:** Phổ biến ≥ 50%; Ít gặp 10% to under 50%; Hiếm under 10%. With n = 66, items near 10% (P06, P08, S05) could fall on either side. The notes say so.
- **Severity is marked separately from frequency:**
  - Nghiêm trọng: credentials or personal data leak, or a security control is switched off (S01, S02, S05; under error pressure S03, X01). It also applies when fixing the problem alone moves a lab screen from missing to meeting a course target (P06, P09 memory; P09 frames; P13 search).
  - Đáng kể: a large measured cost that does not decide a target (P10 size; P08 frame time).
  - Không đo được chi phí: P01, P03, P04.
  - Nhẹ: P11.
- **Context counts stay outside the bins.** E4 X01 12/30 and S03 5/30, and E6 P08 11/12, are context rows. Accessibility, layout and dependency rules are on backup D5 only.

**Framing rules.** Advice is organised by the five development phases, with two levels per phase: "Tối thiểu" and "Nâng cao", each tied to a measured result. "vibe coding" appears only as the name of the phenomenon, on slide 2. Slides show no source captions, and the hype examples get no source list. Sources live in each slide's `aside.notes` as `path:line` and in the proof table. Limits are stated once: slide 6, slide 21 and D1.

**Design rules.** The canvas is 1920 × 1080, scaled to the window. A headline is a full sentence that makes the claim. Body text is at least 20 px at a 1280 px window, and nothing may be clipped or leave the canvas. Each main slide carries one picture of evidence (an A/B video, a chart, verbatim code or a screenshot) and few numbers. Statistics such as p, Cliff's delta and intervals go to the notes and to D1–D8.

**Hype images (slide 2).** There are six 1280 × 720 YouTube thumbnails, `evidence/img/hype/hype-01.jpg` … `hype-06.jpg`. Their origin (title, channel, video id, fetch date) is recorded in [evidence/img/hype/README.md](evidence/img/hype/README.md) and never shown on a slide. The digits they contain are inside the images, so they need no proof rows.

**Live demo kit (slide 16).** [demo/](demo/README.md) runs A (the AI first draft) next to B (the tuned version) of the same screen, on the emulator. It uses one stdlib Python file, `demo.py`, and works on Windows (`py`) and Linux (`python3`):
- `setup` and `doctor` check the machine before the talk.
- `start` and `stop` handle the emulators.
- `list` and `run p09|p08|p06|p13|p10|p01|p0304` run the scenarios; `record` captures a run.

For the 120-second slot, run `run p09` at 0:00 and `run p13` at 1:00. Both run on the same GPU setting, so no emulator reboot is needed. The fallback is the slide-7 video plus `evidence/video/P13c-AB.mp4`, reading the numbers printed on the slide-16 cards. Live numbers are an illustration only; the study numbers on the slide are the reference. [demo/AGENT-BRIEF.md](demo/AGENT-BRIEF.md) briefs a presenter's coding assistant (English).

**Keys.**
- Navigation: → or Space next, ← back, digits + Enter jump.
- Views: P presenter window, O overview, N notes, T light/dark, B blackout, ? help.
- Print to PDF gives one page per slide, in final state.

The presenter clock target is the sum of the "~N giây" notes of the main slides, so it follows the deck when a slide changes. The same goes for the overrun warning and the "backup from slide N" help text. To show the deck over the tailnet: `python3 -m http.server <port> --bind <tailscale-ip> --directory optimization/study`.

### Slides: what each one claims and where its numbers are proven

The data-title column is exactly the slide's `data-title` in the deck. Script, outline, report and proof key on slide number plus data-title.

| # | data-title | What the slide claims | Experiment | Proof (rows in proof/deck-proof.csv; main sources) |
|---|---|---|---|---|
| 1 | Tối ưu giao diện, bộ nhớ và bảo mật | Opening question: where is the un-optimised part of an AI first draft, and what does it cost; three pillars and the three study-size chips | E1, E6 | 7 rows; `e1-corpus/REPORT.md`, `e6-oneshot/REPORT.md` |
| 2 | Lời hứa trên mạng | Six real thumbnails promise a whole app from one prompt; names the phenomenon (vibe coding) once | hype images | 1 row (labels only) |
| 3 | Chạy đúng prompt đó | The same hype prompt gives four finished-looking apps that are not optimised: 6/12 build, 74–100% janky, 1.195–2.576 ms launch; RAM passes only because the data is 12 sample titles | E6 | 26 rows; `e6-oneshot/REPORT.md`, `e2-bench/REPORT.md` |
| 4 | Phân loại lỗi | Classification matrix: frequency on the 66 drafts without a rules file vs severity; the most frequent problem is not the most dangerous one | E1 (context row E4, note E6) | 35 rows; `e1-corpus/REPORT.md`, `e4-pressure/REPORT.md` |
| 5 | Ba điều rút ra | Three lessons: build first, turn on R8, check the rare but critical problems on purpose; human apps break many UI rules too | E1, E2, E6 | 25 rows; `e1-corpus/REPORT.md`, `e6-oneshot/REPORT.md` |
| 6 | Sáu thí nghiệm | Six experiments, criteria fixed before data, isolation check, emulator numbers read as A/B only | E1–E6, isolation audit | 20 rows; `isolation-audit/AUDIT.md`, `e1-corpus/REPORT.md` |
| 7 | Danh sách dài dựng toàn bộ | A long list composed eagerly (P09): janky 90% → 1,3%, rows composed 2.000 → 31 | E2 | 16 rows; `e2-bench/REPORT.md`, `e1-corpus/REPORT.md` |
| 8 | Hiệu ứng trang trí | Shadows and gradients per card (P08): frame time +28% (C5 confirmed); the tuned card still misses 60 fps; 8/66 drafts, 11/12 hype runs | E2, E1, E6 | 25 rows; `e2-bench/REPORT.md`, `e1-corpus/REPORT.md` |
| 9 | Lỗi không có chi phí đo được | Rules with no measurable cost here (P01, P03+P04); Baseline Profile small on the emulator, nothing on the phone: measure before fixing | E2, ShelfTap log | 28 rows; `e2-bench/REPORT.md`, `../log/opt-ui-ux.md` |
| 10 | Ảnh giải mã ở kích thước gốc | Covers decoded at full size (P06): RSS 180 → 60 MB against the 150 MB target | E2 | 23 rows; `e2-bench/REPORT.md`, `e1-corpus/REPORT.md` |
| 11 | Danh sách dài và bộ nhớ | The same eager list seen as memory: RSS 243 → 43 MB, heap 220 → 7,6 MB, TTID 1.901 → 573,1 ms | E2 | 23 rows; `e2-bench/REPORT.md` |
| 12 | Dung lượng và tìm kiếm | R8 cuts the APK by 90%; an FTS4 index brings the slowest query under 50 ms (96,63 → 37 ms) | E2 | 27 rows; `e2-bench/REPORT.md`, `e2-bench/results/p13_equivalence.csv` |
| 13 | Lỗi bảo mật phổ biến đến mức nào | 60 of 66 drafts carry a security finding; density 2,3× the human apps (C3 confirmed) | E1 | 20 rows; `e1-corpus/REPORT.md` |
| 14 | Điều gì làm giảm lỗi bảo mật | The rules file lowers the share 31/33 → 17/34 (C1) and the rule count 5,85 → 2,82 (C2), but drafts that build also fall 20/33 → 10/34 (detailed prompt 12/33, descriptive); a longer prompt does not lower warnings (29/33); no difference among the 42 drafts that build | E1 | 39 rows; `e1-corpus/REPORT.md` |
| 15 | Khi gặp lỗi chặn | Under a blocking error assistants weaken security in 17/148 trials (C4 not confirmed), only Keystore and cleartext; APK scanner precision 0,291 | E4, E3 | 28 rows; `e4-pressure/REPORT.md`, `e3-security/REPORT.md` |
| 16 | Demo: trước và sau tối ưu | Live demo, A next to B on the emulator (P09, P06, P08, P13); the study numbers on the cards are the reference | demo kit, E2 | 25 rows; `e2-bench/REPORT.md` |
| 17 | Bản nháp build lỗi | Drafts often fail to build (42/100); model choice (1/9 vs 7/9) and a structured spec (janky 99% → 19,6%) matter most | E1, E6 | 17 rows; `e6-oneshot/REPORT.md`, `e1-corpus/REPORT.md` |
| 18 | “Đã sửa xong” chưa phải kết quả | "Đã sửa xong" is not a test result: all 168 seeded issues found in suggest-only mode, but 9/12 codex fixes do not compile | E5 | 13 rows; `e5-optimizer/REPORT.md` |
| 19 | Sáu kết luận | One conclusion per experiment (E1 notes the rules-file trade-off between fewer warnings and fewer builds) | E1–E6 | 22 rows; `e1-corpus/REPORT.md`, `e3-security/REPORT.md` |
| 20 | Kết quả các phép kiểm định | Four of the five pre-registered tests confirmed, C4 not; H4 and H5 rejected | C1–C5, H4, H5 | 31 rows; `e1-corpus/REPORT.md`, `00-protocol.md` |
| 21 | Đối chiếu mục tiêu môn học | Course-target scoreboard: the fixed lab screens meet the targets (emulator); whole-app RAM 94–95 MB PSS on one Xiaomi scenario without covers; cells without a paired measurement say "không áp dụng" and the two unmeasured items say "chưa đo" | E2, E6, ShelfTap log | 48 rows; `e2-bench/REPORT.md`, `../log/opt-ui-ux.md` |
| 22 | Thiết bị thật | Measure the measure build, not debug (125 vs 12–16 ms p90); a cover-URL bug that only a real phone shows | ShelfTap log | 25 rows; `../log/opt-ui-ux.md`, `e2-bench/REPORT.md` |
| 23 | Giai đoạn 1 đến 3 | Phases 1–3 (set requirements, generate, fix errors), each with Tối thiểu and Nâng cao advice tied to a measured result (Phase 2 Tối thiểu notes the rules-file trade-off: warnings 31/33 → 17/34, builds 20/33 → 10/34) | E6, E1, E4 | 28 rows; `e6-oneshot/REPORT.md`, `e1-corpus/REPORT.md` |
| 24 | Giai đoạn 4 và 5 | Phases 4–5 (verify, measure and release), same two levels | E5, E3, E2, ShelfTap log | 24 rows; `e2-bench/REPORT.md`, `../log/opt-ui-ux.md` |
| 25 | Ba điều cần nhớ | Three points to remember; no new numbers | — | 6 rows (labels only) |
| D1 | D1 · Giới hạn | Limits: emulator, sample sizes, what is not measured | E2, E1 | 10 rows; `e2-bench/REPORT.md`, `e1-corpus/REPORT.md` |
| D2 | D2 · Bảng Holm | Holm table for C1–C5 | C1–C5 | 36 rows; `e1-corpus/REPORT.md`, `00-protocol.md` |
| D3 | D3 · Bảng E2 đầy đủ | Full E2 table, seven factors plus the P01 contrast | E2 | 49 rows; `e2-bench/REPORT.md`, `e2-bench/results/c5_confirmatory.csv` |
| D4 | D4 · E4 chi tiết | E4 detail per case and per assistant, Stack Overflow baseline | E4 | 26 rows; `e4-pressure/REPORT.md` |
| D5 | D5 · Tần suất theo quy tắc | Frequency per rule and prompt style next to human apps | E1 | 29 rows; `e1-corpus/REPORT.md` |
| D6 | D6 · E6 chi tiết | E6 builds per assistant and prompt arm | E6 | 10 rows; `e6-oneshot/REPORT.md` |
| D7 | D7 · Kiểm tra môi trường | Isolation check of the generation runs | isolation audit, E6 | 7 rows; `isolation-audit/AUDIT.md` |
| D8 | D8 · Cỡ chữ lớn và bố cục | Large font scale 2.0: the one-prompt apps break; ShelfTap 5/7 → 0/7 | E6, ShelfTap metrics | 14 rows; `../ui-ux-metrics.md`, `../log/opt-ui-ux.md` |

### Script, slide by slide

Copied from [presentation-script.md](presentation-script.md) by `build_playbook_html.py`. Edit the script file, then rebuild; never edit this block by hand. The timing table, the Q&A table ([Hỏi đáp dự kiến](presentation-script.md)), the preparation checklist and the number check stay in the script file.

<!-- SCRIPT:START -->
<!-- filled by build_playbook_html.py from presentation-script.md; edit the script, not this block -->

#### 1. Tối ưu giao diện, bộ nhớ và bảo mật (0:00 đến 0:30) — slide 1

*Mở đầu · 30 giây · 61 từ*

**Lời nói.** Kính chào thầy cô và các bạn. Hôm nay nhóm em trình bày về tối ưu giao diện, bộ nhớ và bảo mật trên Android với bối cảnh ShelfTap — ứng dụng thư viện NFC. Trợ lý lập trình AI viết bản nháp đầu rất nhanh, nhưng phần chưa tối ưu nằm ở đâu và gây tổn hao đến mức nào?

**Thao tác.** Dừng một nhịp sau câu hỏi rồi chuyển sang slide 2.

**Lưu ý.** Con số trên slide: 6 thí nghiệm, 101 bản nháp đầu, 36 lần chạy với chỉ một prompt (tức lời nhắc giao cho AI).

#### 2. Lời hứa trên mạng (0:30 đến 1:10) — slide 2

*Mở đầu · 40 giây · 82 từ*

**Lời nói.** Thầy cô và các bạn chắc hẳn đã quen với những bài đăng kiểu này: ảnh giao diện bóng bẩy kèm lời hứa chỉ một prompt — tức lời nhắc giao cho AI — là có ngay ứng dụng hoàn chỉnh. Hiện tượng đó gọi là vibe coding: mô tả bằng lời, nhận mã từ trợ lý AI rồi dùng ngay mà không kiểm tra lại. Nếu làm đúng theo cách đó trên Android thì kết quả thực tế sẽ ra sao?

**Thao tác.** Lướt tay qua lưới sáu ảnh minh họa, không đọc từng ảnh.

**Lưu ý.** Không nhắc tên nguồn hay đường dẫn ảnh trên sân khấu; nguồn lưu riêng trong evidence/img/hype/README.md.

#### 3. Chạy đúng prompt đó (1:10 đến 1:55) — slide 3

*Mở đầu · 45 giây · 98 từ*

**Lời nói.** Nhóm em đã thử chạy đúng kiểu prompt đó trên bốn trợ lý AI. Nhìn bề ngoài, cả bốn ứng dụng trông khá hoàn thiện. Nhưng khi đo trên máy ảo, chỉ có 6 trên 12 lần chạy build thành công. Tỷ lệ khung hình trễ khi cuộn lên tới 74 đến 100%, còn thời gian khởi động mất 1.195 đến 2.576 ms. Công bằng mà nói, mức dùng RAM chỉ từ 66,8 đến 102,8 MB vì trợ lý AI mới tự tạo 12 tựa sách mẫu; vấn đề bộ nhớ chỉ lộ ra ở dữ liệu thật.

**Thao tác.** Chỉ lần lượt bốn màn hình trên slide khi nhắc dải 74 đến 100%, rồi nhấn mạnh câu cuối về 12 tựa sách.

**Lưu ý.** Mỗi ảnh là lần chạy có tỷ lệ khung hình trễ thấp nhất trong 3 lần của từng trợ lý AI; số đo trên máy ảo chỉ so sánh trong thí nghiệm này.

#### 4. Phân loại lỗi (1:55 đến 2:55) — slide 4

*Mở đầu · 60 giây · 129 từ*

**Lời nói.** Bảng này cho thấy bức tranh tổng thể về các lỗi trong bản nháp đầu. Thầy cô và các bạn có thể đọc nhanh theo hai chiều: từ trái sang phải là tần suất trên 66 bản nháp không kèm tệp quy tắc (guardrail), gồm phổ biến từ 50% trở lên, ít gặp từ 10% đến dưới 50%, và hiếm dưới 10%. Chấm đỏ đánh dấu lỗi nghiêm trọng. Điều đáng chú ý nằm ngay đây: cột bên trái là lỗi hay gặp nhất như build lỗi 58/100 hay thiếu R8 63/66, nhưng phần lớn chấm đỏ lại rơi vào hai cột sau — lưu token dạng rõ 19/66, tìm kiếm thiếu chỉ mục 15/66, dựng toàn bộ danh sách 2/66, giải mã ảnh ở kích thước gốc 5/66.

**Thao tác.** Chỉ từ trái sang phải qua ba cột Phổ biến, Ít gặp, Hiếm, rồi khoanh vào các chấm đỏ và dòng bối cảnh E4 phía dưới.

**Lưu ý.** P06 (5/66) chưa kiểm định độ chính xác; dòng khi gặp lỗi chặn (12/30 bỏ xác thực khóa Keystore, 5/30 bật cleartext) thuộc bối cảnh E4, không gộp vào ba cột E1.

#### 5. Ba điều rút ra (2:55 đến 3:45) — slide 5

*Mở đầu · 50 giây · 106 từ*

**Lời nói.** Từ bảng phân loại, nhóm em rút ra ba điều. Trước khi nói đến tối ưu, lỗi phổ biến nhất là bản nháp build lỗi: 58/100 ở E1 và 6/12 ở E6. Thứ hai, lỗi tài nguyên hay gặp nhất lại dễ sửa nhất: 63/66 bản thiếu R8, bật R8 lên là APK giảm từ 24,7 MB xuống 2,4 MB. Thứ ba, lỗi nghiêm trọng nhất lại hiếm, cần chủ động rà soát. Công bằng mà nói, ứng dụng do người viết cũng mắc nhiều lỗi giao diện như thiếu key 14/20; chỉ mật độ cảnh báo bảo mật mới là khác biệt được kiểm định.

**Thao tác.** Đếm lần lượt ba thẻ lớn rồi dừng ngắn ở dòng lưu ý so với ứng dụng do người viết.

**Lưu ý.** Chuyển mượt sang slide 6: giới thiệu sáu thí nghiệm tạo ra các con số trên.

#### 6. Sáu thí nghiệm (3:45 đến 4:25) — slide 6

*Phương pháp · 40 giây · 90 từ*

**Lời nói.** Số liệu hôm nay đến từ sáu thí nghiệm với tiêu chí chốt trước khi thu thập dữ liệu: E1 đo tần suất trên 101 bản nháp; E2 đối chứng A/B trên cùng màn hình; E3 quét 13 APK; E4 thử 148 lượt sửa lỗi chặn; E5 kiểm tra 14 lỗi cài sẵn; E6 chạy prompt kiểu trào lưu. Quét lại 623 bản ghi, nhóm em xác nhận không lọt cấu hình riêng. Số đo máy ảo chỉ so tỷ lệ A/B, không phải tốc độ trên thiết bị thật.

**Thao tác.** Chỉ nhanh hai khung ở đáy slide: Môi trường sạch và Máy ảo.

**Lưu ý.** Giới hạn máy ảo nói một lần ở đây; câu hỏi sâu về cô lập môi trường mở D7, về giới hạn mở D1.

#### 7. Danh sách dài dựng toàn bộ (4:25 đến 5:10) — slide 7

*Trụ cột 1: Giao diện · 45 giây · 101 từ*

**Lời nói.** Đi vào trụ cột thứ nhất: hiệu năng hiển thị. Đây là cùng một màn hình danh sách 2.000 lượt mượn với cùng thao tác cuộn. Bên trái là mã giữ nguyên từ bản nháp AI dùng Column với forEach, dựng hết 2.000 mục dù màn hình chỉ hiện vài mục, khiến 90% khung hình bị trễ. Bên phải chỉ đổi sang LazyColumn: số mục được dựng giảm từ 2.000 xuống 31, tỷ lệ khung hình trễ giảm mạnh từ 90% xuống 1,3%. Chỉ thay đổi vài dòng mã nhưng lại quyết định màn hình có đạt 60 fps hay không.

**Thao tác.** Phát video P09c-AB.mp4, dừng ngắn ở đoạn bên trái bị khựng khi cuộn.

**Lưu ý.** Trung vị từ ít nhất 12 lần lặp xen kẽ A/B trên máy ảo; số đầy đủ 90,05% xuống 1,255%, lỗi hiếm 2/66 nhưng nghiêm trọng.

#### 8. Hiệu ứng trang trí (5:10 đến 5:50) — slide 8

*Trụ cột 1: Giao diện · 40 giây · 91 từ*

**Lời nói.** Lỗi thứ hai hay gặp khi dùng prompt kiểu trào lưu là dồn đổ bóng, chuyển sắc (gradient) và vẽ Canvas lên từng thẻ sách. Khi đổi bìa thành khối phẳng, thời gian xử lý khung hình p90 giảm từ 62,87 ms xuống 49,07 ms — mã AI chậm hơn 28%, qua đó xác nhận phép kiểm định C5. Tỷ lệ khung hình trễ giảm từ 70,52% xuống 48,65%, dù chưa đạt 60 fps trên máy ảo. Lỗi gặp ở 8/66 bản E1 nhưng vọt lên 11/12 lần chạy ở E6.

**Thao tác.** Phát video P08c-AB.mp4; nhấn cặp số 8/66 so với 11/12.

**Lưu ý.** P08 đo trên cấu hình kết xuất GPU swangle_indirect nên chỉ so trong nội bộ nhân tố này; bộ nhớ đồ họa chưa đo.

#### 9. Lỗi không có chi phí đo được (5:50 đến 6:35) — slide 9

*Trụ cột 1: Giao diện · 45 giây · 97 từ*

**Lời nói.** Ngược lại, không phải quy tắc nào cũng tạo khác biệt đo được. Trên biểu đồ, chấm lệch trái vạch 0 nghĩa là bản sửa nhanh hơn: sửa danh sách giảm 69,9% thời gian khởi động. Nhưng thêm key danh sách (P01) và ổn định tham số (P03, P04) đều có khoảng tin cậy cắt qua 0. Baseline Profile giảm 5,88% khởi động trên máy ảo, còn trên thiết bị thật ở bản build đo hiệu năng (cấu hình tương đương bản phát hành) thì không có khác biệt đo được. Hãy đo trước khi tối ưu.

**Thao tác.** Chỉ dọc theo vạch 0 từ trên xuống dưới để đối chiếu hai dòng trên và hai dòng dưới.

**Lưu ý.** Khoảng tin cậy cắt qua 0 nghĩa là chưa ghi nhận chi phí trong kịch bản này (danh sách tĩnh, strong skipping mặc định), không phải luôn miễn phí.

#### 10. Ảnh giải mã ở kích thước gốc (6:35 đến 7:15) — slide 10

*Trụ cột 2: Bộ nhớ · 40 giây · 83 từ*

**Lời nói.** Sang trụ cột thứ hai: bộ nhớ. Với 15 ảnh bìa, bản nháp AI giải mã ở kích thước gốc 1500×2250 cho từng ô nhỏ, không thu nhỏ và không dùng bộ nhớ đệm. Riêng ảnh chiếm 203 MB, đẩy RAM cao nhất lên 180 MB — vượt ngưỡng dưới 150 MB. Khi đổi sang Coil AsyncImage để giải mã đúng kích thước ô và có bộ nhớ đệm, dung lượng ảnh còn 6,2 MB và mức dùng RAM giảm xuống 60 MB.

**Thao tác.** Phát video P06c-AB.mp4, chỉ đường mốc mục tiêu dưới 150 MB.

**Lưu ý.** Lỗi hiếm (5/66, chưa kiểm định độ chính xác) nhưng nghiêm trọng vì chỉ riêng lỗi này đã vượt ngưỡng 150 MB.

#### 11. Danh sách dài và bộ nhớ (7:15 đến 7:55) — slide 11

*Trụ cột 2: Bộ nhớ · 40 giây · 85 từ*

**Lời nói.** Nhóm em nhìn lại danh sách 2.000 lượt mượn ở slide 7 dưới góc độ bộ nhớ. Vì bản nháp AI dựng đủ 2.000 mục trước khung hình đầu tiên, mức dùng RAM vọt lên 243 MB, Java heap 220 MB, thời gian khởi động mất 1.901 ms. Khi chuyển sang LazyColumn, mức dùng RAM giảm xuống 43 MB, Java heap còn 7,6 MB và thời gian khởi động giảm xuống 573,1 ms. Một lỗi danh sách làm trượt cả khung hình lẫn bộ nhớ.

**Thao tác.** Chỉ ba cụm chỉ số: RSS 243 xuống 43 MB, Java heap 220 xuống 7,6 MB, TTID 1.901 xuống 573,1 ms.

**Lưu ý.** Số MB ít phụ thuộc máy ảo hơn số ms; bản nháp AI vượt ngưỡng 150 MB còn bản sửa đạt.

#### 12. Dung lượng và tìm kiếm (7:55 đến 8:35) — slide 12

*Trụ cột 2: Bộ nhớ · 40 giây · 91 từ*

**Lời nói.** Tiếp theo là dung lượng và tìm kiếm. Bật R8 thu nhỏ APK tới 90%, từ 24,7 MB còn 2,4 MB, và giảm thêm 8,7% thời gian khởi động. Với tìm kiếm ngoại tuyến 100.000 tựa sách, truy vấn LIKE chậm nhất mất 96,63 ms, vượt 50 ms; đổi sang chỉ mục FTS4 đưa mức chậm nhất xuống 37 ms và tổng thời gian truy vấn giảm từ 634,9 ms xuống 85,42 ms. Tuy nhiên, khung hình khi gõ không cải thiện vì nút thắt nằm ở danh sách kết quả.

**Thao tác.** Chỉ cột trái R8 rồi chuyển sang cột phải FTS4 và mốc 50 ms.

**Lưu ý.** LIKE khớp chuỗi con còn FTS4 khớp tiền tố từ nên khác tập kết quả ở 4/12 tiền tố.

#### 13. Lỗi bảo mật phổ biến đến mức nào (8:35 đến 9:20) — slide 13

*Trụ cột 3: Bảo mật · 45 giây · 100 từ*

**Lời nói.** Sang trụ cột thứ ba: bảo mật — nơi bản nháp AI khác biệt rõ nhất so với ứng dụng do người viết. Trong 66 bản nháp không kèm tệp quy tắc ở E1, có tới 60 bản chứa ít nhất một cảnh báo bảo mật. Trên 1.000 dòng mã, bản nháp AI có trung vị 2,37 cảnh báo, gấp 2,3 lần mức 1,02 ở ứng dụng do người viết trước năm 2022; phép kiểm định C3 xác nhận mức chênh này. Phía dưới là đoạn mã nguyên văn: token đăng nhập được ghi thẳng ở dạng rõ vào SharedPreferences.

**Thao tác.** Chỉ hai dòng `putString` trong khung mã `SessionStore.kt` ở phần dưới slide.

**Lưu ý.** Nói trung lập: bản nháp AI có mật độ cảnh báo bảo mật cao hơn, không nói AI gây ra; đối chứng gồm 20 ứng dụng trước 30/11/2022.

#### 14. Điều gì làm giảm lỗi bảo mật (9:20 đến 10:05) — slide 14

*Trụ cột 3: Bảo mật · 45 giây · 100 từ*

**Lời nói.** Vậy cách nào làm giảm cảnh báo bảo mật lúc sinh mã? Ở E1, prompt chức năng chi tiết vẫn có 29/33 bản bị cảnh báo so với 31/33 ở prompt ngắn. Tệp quy tắc kéo tỷ lệ này xuống 17/34 và giảm số loại vấn đề từ 5,85 xuống 2,82, xác nhận C1, C2. Nhưng đánh đổi là bản build thành công sau tối đa 3 lần sửa cũng giảm từ 20/33 xuống 10/34, còn prompt chi tiết là 12/33. Xét riêng 42 bản build thành công, tỷ lệ có cảnh báo không đổi: vẫn cần bước kiểm tra.

**Thao tác.** Chỉ hai cụm biểu đồ bên trái (cảnh báo bảo mật và bản build thành công) rồi sang hai khung bên phải.

**Lưu ý.** Tỷ lệ build thành công (20/33, 12/33, 10/34) là số mô tả; C2 tính trên cả bốn nhóm quy tắc; trong 42 bản build thành công, chênh lệch C1 bằng 0,000 còn C2 vẫn còn (+3,829).

#### 15. Khi gặp lỗi chặn (10:05 đến 10:50) — slide 15

*Trụ cột 3: Bảo mật · 45 giây · 93 từ*

**Lời nói.** Rủi ro bảo mật còn lộ ra khi sửa lỗi chặn. Ở E4, gặp UserNotAuthenticatedException của Android Keystore (kho khóa của hệ thống), trợ lý AI có thể xóa yêu cầu xác thực — đổi true thành false — để lỗi biến mất. Trên 148 lượt sửa có 17 lượt nới lỏng cơ chế bảo mật, dưới ngưỡng 15% nên C4 không được xác nhận; tất cả rơi vào Keystore 12/30 và cleartext 5/30. Ở E3, công cụ quét APK đạt độ chính xác 0,291 và không phát hiện lỗi trong mã nguồn.

**Thao tác.** Chỉ dòng đỏ bị xóa và dòng xanh thêm vào trong diff `TokenVault.kt`.

**Lưu ý.** Không xếp hạng trợ lý AI theo 12/48, 5/50, 0/50 vì công cụ và mô hình bị lẫn (mở D4 nếu được hỏi).

#### 16. Demo: trước và sau tối ưu (10:50 đến 12:50) — slide 16

*Demo trực tiếp · 120 giây (chuyển và nhận lại lượt: ~25 giây, 54 từ; phần demo trực tiếp: ~95 giây)*

**Lời nói.** *[Chuyển lượt]* Để thầy cô và các bạn quan sát trực tiếp chứ không chỉ nhìn bảng số, thành viên nhóm em sẽ chạy demo song song bản nháp AI và bản tối ưu trên máy ảo. Xin mời bạn. *[Nhận lại lượt]* Cảm ơn phần demo. Sự khác biệt khi cuộn và tìm kiếm thể hiện rất rõ.

**Thao tác.** Chuyển micro cho thành viên demo (thứ tự gợi ý: P09 danh sách 2.000 lượt mượn, P06 ảnh bìa, P08 thẻ trang trí, P13 tìm kiếm 100.000 tựa sách); nếu máy ảo lỗi, mở lại video ở slide 7, 8, 10.

**Lưu ý.** Số đo trên máy ảo laptop chỉ để minh họa; số tham chiếu của nghiên cứu là bốn mốc trên slide (90% → 1,3%; 180 → 60 MB; 62,87 → 49,07 ms; 96,63 → 37 ms). Chi tiết chạy demo ở optimization/study/demo/README.md.

#### 17. Bản nháp build lỗi (12:50 đến 13:30) — slide 17

*Build và tự sửa lỗi · 40 giây · 89 từ*

**Lời nói.** Quay lại lỗi phổ biến nhất: bản nháp build lỗi. Ở E1, chỉ 42/100 bản build thành công. Hai yếu tố tạo chênh lệch rõ nhất là mô hình và bản đặc tả. Ở E6, dùng cùng công cụ và chỉ đổi mô hình, số bản build thành công tăng từ 1/9 với claude-haiku lên 7/9 với claude-sonnet. Thay prompt kiểu trào lưu bằng bản đặc tả có cấu trúc kéo tỷ lệ khung hình trễ từ 99% xuống 19,6% và lượng mã Kotlin từ 1.164 xuống 524 dòng.

**Thao tác.** Chỉ lần lượt 42/100, cặp 1/9 với 7/9, rồi cặp 99% xuống 19,6%.

**Lưu ý.** E6 là thí nghiệm khám phá với tối đa 3 lần chạy mỗi ô, không có kiểm định p; chi phí claude-haiku 0,11 USD so với claude-sonnet 0,48 USD.

#### 18. “Đã sửa xong” chưa phải kết quả (13:30 đến 14:10) — slide 18

*Build và tự sửa lỗi · 40 giây · 86 từ*

**Lời nói.** Một rủi ro khác là tin vào lời “đã sửa xong”. Ở E5, khi ở chế độ chỉ đề xuất mà không sửa mã, trợ lý AI tìm đủ 168/168 lượt lỗi cài sẵn. Nhưng khi tự sửa, codex tuyên bố đã xong trong khi chạy biên dịch thật thì 9/12 bản lỗi do tự thêm plugin baselineprofile không dùng được. Đồng thời 21/24 lượt làm đổi cả mẫu đối chứng vốn đúng. Rõ ràng lời trợ lý AI không thể thay kết quả kiểm thử.

**Thao tác.** Chỉ hộp lời trả lời của codex ở trên và dòng lỗi biên dịch thật bên dưới.

**Lưu ý.** Nhấn mạnh: biên dịch và kiểm thử độc lập mới là căn cứ xác nhận, không phải lời của trợ lý AI.

#### 19. Sáu kết luận (14:10 đến 14:50) — slide 19

*Kết luận · 40 giây · 90 từ*

**Lời nói.** Sáu thí nghiệm cho sáu kết luận. E1: 42/100 bản build thành công, hầu hết có cảnh báo bảo mật; tệp quy tắc giảm cảnh báo nhưng cũng giảm bản build thành công. E2: danh sách và ảnh làm trượt mục tiêu; ba lỗi Compose kinh điển không có chi phí đo được. E3: quét APK đạt 0,291, bỏ sót lỗi mã. E4: 17/148 lượt nới lỏng cơ chế bảo mật. E5: 9/12 bản codex sửa không biên dịch được. E6: bản đặc tả đưa khung hình trễ xuống 19,6%.

**Thao tác.** Lướt nhanh sáu hàng từ E1 đến E6 với nhịp dứt khoát.

**Lưu ý.** Không diễn giải lại chi tiết; dùng slide này gom toàn cảnh trước khi chốt kiểm định.

#### 20. Kết quả các phép kiểm định (14:50 đến 15:30) — slide 20

*Kết luận · 40 giây · 87 từ*

**Lời nói.** Về thống kê, trong năm phép kiểm định đăng ký trước sau hiệu chỉnh Holm, bốn phép được xác nhận là C1, C2, C3 và C5 — khẳng định hiệu quả tệp quy tắc, mật độ cảnh báo bảo mật cao hơn ứng dụng do người viết và chi phí hiệu ứng trang trí. C4 không được xác nhận vì 17/148 dưới 15%. Hai giả thuyết mô tả — H4 bịa thư viện (0/2.105) và H5 thiếu mô tả trợ năng (1/66) — đều bị bác bỏ.

**Thao tác.** Chỉ nhóm bốn hàng xanh phía trên, tách khỏi C4 và hai hàng H4, H5.

**Lưu ý.** “Được xác nhận” nghĩa là thỏa mãn điều kiện thống kê đã đăng ký; bảng hiệu chỉnh Holm đầy đủ ở D2.

#### 21. Đối chiếu mục tiêu môn học (15:30 đến 16:20) — slide 21

*Kết luận · 50 giây · 105 từ*

**Lời nói.** Đối chiếu mục tiêu môn học: trên màn hình thử nghiệm, bản sửa đạt yêu cầu — tìm 100.000 sách còn 37 ms, dưới 50 ms; RAM 60 và 43 MB dưới 150 MB; danh sách dài 1,3% khung hình trễ. Trên Xiaomi, mở trang sách đạt p90 8 đến 16 ms, cuộn danh mục đạt PSS 94 đến 95 MB. Các ô không có cặp đối chiếu được ghi rõ không áp dụng. Lưới thẻ trên máy ảo vẫn 48,65%; ứng dụng chỉ một prompt có 12 tựa sách; chạm NFC dưới 1 s và thiết bị cấu hình thấp ghi rõ chưa đo.

**Thao tác.** Lướt dọc cột Trạng thái để phân tách phần đạt và hai hạng mục chưa đo.

**Lưu ý.** Ô “không áp dụng” là nơi chỉ một prompt không có bản đã sửa hoặc phép đo ShelfTap trên Xiaomi không có bản nháp AI tương ứng; hai hạng mục cuối ghi rõ “chưa đo”.

#### 22. Thiết bị thật (16:20 đến 17:05) — slide 22

*Thiết bị thật · 45 giây · 102 từ*

**Lời nói.** Trên thiết bị thật Xiaomi, ShelfTap cho thấy hai bài học máy ảo không lộ ra. Thứ nhất, đo bằng bản debug là đo sai đối tượng: mở trang sách ở bản debug lên tới 125 ms p90, còn bản build đo hiệu năng (cấu hình tương đương bản phát hành) chỉ 12 đến 16 ms, nằm gọn trong ngưỡng 16,67 ms của 60 fps. Thứ hai, địa chỉ ảnh bìa 10.0.2.2 chỉ truy cập được từ máy ảo, khiến thiết bị thật gọi lại liên tục; khi khắc phục, tỷ lệ khung hình trễ khi cuộn giảm từ 7,0–7,2% xuống 1,5–2,7%.

**Thao tác.** Chỉ bên trái (125 ms so với 12–16 ms) rồi sang bên phải (10.0.2.2).

**Lưu ý.** Số liệu trang chủ 7,0–7,2% xuống 1,5–2,7% đo trên bản debug; thiết bị cấu hình thấp chưa đo.

#### 23. Giai đoạn 1 đến 3 (17:05 đến 17:55) — slide 23

*Khuyến nghị · 50 giây · 105 từ*

**Lời nói.** Nhóm em đúc kết quy trình năm giai đoạn, mỗi giai đoạn có mức tối thiểu và nâng cao. Giai đoạn 1 đặt yêu cầu: tối thiểu viết bản đặc tả có cấu trúc; nâng cao gắn mục tiêu đo được và định nghĩa xong là build thành công. Giai đoạn 2 sinh mã: tối thiểu đặt tệp quy tắc ở gốc dự án rồi kiểm tra bản nháp vẫn build thành công; nâng cao chọn mô hình build tốt và bỏ yêu cầu quá sức. Giai đoạn 3 sửa lỗi: cấm nới lỏng cơ chế bảo mật, đọc dòng xóa và khóa bằng test.

**Thao tác.** Dẫn khán giả đi theo ba hàng 1, 2, 3 mà không đọc các ô số nhỏ.

**Lưu ý.** Mọi khuyến nghị đều gắn với một kết quả đo đã nêu (99% → 19,6%; cảnh báo bảo mật 31/33 → 17/34 nhưng build thành công 20/33 → 10/34; 12/30 Keystore).

#### 24. Giai đoạn 4 và 5 (17:55 đến 18:40) — slide 24

*Khuyến nghị · 45 giây · 100 từ*

**Lời nói.** Sang hai giai đoạn cuối, ở giai đoạn 4 kiểm tra: tối thiểu là tự build, tự mở ứng dụng và thử lại đúng chỗ vừa sửa; nâng cao là chạy build, kiểm thử và bộ quét mã nguồn trong CI, kết hợp công cụ quét APK cho phần cấu hình. Giai đoạn 5 đo và phát hành: tối thiểu là bật R8 và đo trên thiết bị thật bằng bản build đo hiệu năng; nâng cao là đo A/B xen kẽ bằng Macrobenchmark, ưu tiên sửa danh sách và giải mã ảnh trước khi đầu tư vào Baseline Profile.

**Thao tác.** Chỉ hai hàng 4 và 5, nhấn tiêu đề: tin bản build và số đo, không tin lời trợ lý AI.

**Lưu ý.** Chuyển trực tiếp sang slide chốt cuối cùng.

#### 25. Ba điều cần nhớ (18:40 đến 19:15) — slide 25

*Kết thúc · 35 giây · 76 từ*

**Lời nói.** Khép lại bài trình bày, nhóm em xin đúc kết ba điều cần nhớ. Một là trông hoàn thiện chưa phải đã tối ưu: hãy đo với dữ liệu thật. Hai là lỗi hay gặp không phải lỗi nguy hiểm nhất: cần rà riêng lỗi bộ nhớ và bảo mật. Ba là tin bản build và số đo, không tin lời “đã sửa xong”. Xin cảm ơn, mời thầy cô và các bạn đặt câu hỏi.

**Thao tác.** Giữ slide 25 khi hỏi đáp; nhấn phím O để chọn nhanh slide dự phòng D1 đến D8.

**Lưu ý.** Không đưa số mới ở slide kết. Các slide dự phòng D1 đến D8 nằm ngay sau slide 25.

<!-- SCRIPT:END -->

## 6b. The plain-language report

[report/index.html](report/index.html) is one Vietnamese web page for readers without a presenter. It follows the deck's order and says more than a slide can:
- For each pillar: the problem, the measured before/after, why it happens, and the fix.
- A short paragraph per experiment, E1 to E6.
- The classification table with its rules.
- The verdict table of the pre-registered tests, including C4.
- The full E2 table, the course-target scoreboard (synced with slide 21), and the real-device lessons.
- The five-phase advice at the Tối thiểu and Nâng cao levels.
- Three points and the limits.

Every number in it is a number of the deck. Its owner checks it by a set difference against the deck and by rendering at 1280 and 390 px. The framing rules of section 6 apply: advice is organised by the five development phases with two levels ("Tối thiểu" and "Nâng cao"), and there is no sources section; the lead-in sources stay in [report/sources-leadin.md](report/sources-leadin.md) as a record.

| Report section (h2) | Deck slides |
|---|---|
| Lời hứa trên mạng và thực tế khi chạy đúng prompt đó | 2–3 |
| Phân loại lỗi theo tần suất và mức độ nghiêm trọng | 4–5 |
| Phương pháp nghiên cứu và kiểm tra môi trường | 6, D7 |
| Trụ cột giao diện và hiệu năng hiển thị | 7–9 |
| Trụ cột bộ nhớ, dung lượng và tìm kiếm | 10–12 |
| Trụ cột bảo mật trong bản nháp đầu và khi sửa lỗi | 13–15 |
| Đối chiếu trực tiếp bốn kịch bản trước và sau tối ưu | 16 |
| Bản nháp build lỗi và giới hạn của báo cáo “đã sửa xong” | 17–18 |
| Tổng hợp sáu thí nghiệm, kết quả kiểm định và đối chiếu mục tiêu môn học | 19–21, D2, D3 |
| Bài học từ thiết bị thật và kiểm thử ở tỷ lệ cỡ chữ lớn | 22, D8 |
| Khuyến nghị thực hành theo năm giai đoạn phát triển | 23–24 |
| Ba điểm cốt lõi cần nhớ và giới hạn của nghiên cứu | 25, D1 |

## 7. Proof of every number in the deck

- [proof/deck-proof.csv](proof/deck-proof.csv) has 763 rows (425 direct, 99 derived, 239 label) covering 741 values across all 33 slides (visible text, speaker notes, chart data attributes and bar lengths). Columns: `slide,value,kind,claim,source,line,quote,formula,note`.
  - `direct`: the number is on `source:line`, inside the verbatim `quote`.
  - `cited`: the same, from an external source recorded in references.md (not used by the current deck).
  - `derived`: `formula` recomputes the shown value from proven inputs. Examples: k/66 as naive + detailed (`8+11`); MB as KB / 1024; APK bytes / 1 000 000; Holm as raw p × (5 − rank + 1); "gấp 2,3 lần" as `2.37/1.02`; P08 11/12 counted from `e6-oneshot/results/findings.csv`.
  - `label`: not data, and `note` says what it is. Examples: a line number in a notes source pointer, a "~N giây" timing, a file name, a slide cross-reference, an axis tick, a version, an address. A value that is also data anywhere on that slide gets a sourced row instead of a label.
- [proof/check_deck_proof.py](proof/check_deck_proof.py) (stdlib) reads the deck and lists every number on every slide. It fails unless each number has a row whose quote is really on that line and contains the number (after rounding), and each formula reproduces the shown value. It also fails on any row whose number is no longer on its slide (stale).

```
python3 -I optimization/study/proof/check_deck_proof.py            # whole deck; exit 0 = 0 bad, 0 stale, 0 unproven
python3 -I optimization/study/proof/check_deck_proof.py --list 5   # numbers on slide 5 that need a row
python3 -I optimization/study/proof/check_deck_proof.py --slide 5  # check one slide
```

- [proof/ISSUES.md](proof/ISSUES.md) records what each proof pass found and changed. The 2026-11-28 pass found no wrong number. It changed one piece of markup: the slide-4 columns are now `div`, because a nested `</section>` hid most of that slide from the checker. It also records the slide-21 update, whole-app RAM 94–95 MB PSS on the Xiaomi.
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
| `optimization/TieuLuan-ToiUu-Android.docx`, `optimization/Script-ThuyetTrinh.docx` (Huy, commit e4259e1) | a technique-by-technique essay and a 25-minute script. Their numbers are the B (tuned) side of E2, plus the ShelfTap MobSF rows of E3 and the product's security design | fits as the baseline layer (ARTICLE Chapter 2) and as product security. Each B number needs its A (before) number next to it. It must not present P03+P04, Baseline Profile or card effects as wins the A/B data does not show (slides 8 and 9). Offline FTS search exists only in `android/optlab`, not in the member app |
| ShelfTap UI/UX log (teammate PR, merged 2026-11-28) | real-phone measurements in `optimization/log/opt-ui-ux.md` and `optimization/ui-ux/measure/` | source of slides 9, 21, 22 and D8; re-locate line numbers after each merge |
| Canva deck (group) | not reviewed: Canva blocks automated browsers | export to PDF into the repo to review |

## 10. Checklists

**Before presenting**
- [ ] `python3 -I optimization/study/proof/check_deck_proof.py` exits 0.
- [ ] The script's "Kiểm số liệu" check exits 0, and the timing table total stays between 15:00 and 20:00.
- [ ] Demo machine: `py demo.py doctor` passes; the emulator is started and `run p09` has run once (demo/README.md §4). The fallback video for slide 16 is at hand.
- [ ] The deck opens offline on the presenting machine (fonts are embedded). The presenter window (P) works there and its clock shows the 19:30 target.
- [ ] Light theme on a projector; dark only in a dark room.
- [ ] Rehearse to 19:30 or less (the script times itself at 19:15).
- [ ] Know the Q&A answers in presentation-script.md, each mapped to a backup slide: emulator vs real phone (D1), Holm (D2), +28,13% vs −22% (D3), E4 raters and per-assistant counts (D4), human apps (D5, slide 13), E6 per assistant (D6), isolation (D7).

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
