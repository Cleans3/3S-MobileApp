# Proof issues — deck numbers vs their sources

## Current deck

This record is for the deck re-ordered on 2026-11-28 (talk flow hook → claim → experiments → results → problems → solutions → close): 27 main slides plus backup slides D1..D12 (39 slides) in [../presentation.html](../presentation.html).
Current counts from `python3 -I optimization/study/proof/check_deck_proof.py`, run from the repo root (exit 0): 956 rows checked (518 direct, 109 derived, 329 label), 0 bad, 0 stale, 929 data values, 0 unproven.
Slide numbers in this section and in the talk-flow pass below refer to the current deck. The earlier passes under it keep the numbering of the 25 + 9 deck they were written for; the old → new map below translates them. Earlier decks are under "History".

## 2026-11-28 — talk-flow rebuild (25 + D1–D9 → 27 + D1–D12)

The owner asked for the order hook → claim → experiments → results → problems behind the results → solutions → close. Every main slide now carries `data-part` (A Mở đầu, B Kết quả, C Nguyên nhân, D Giải pháp, E Kết thúc), shown as a strip at the bottom of the slide; slide 1 is a poster and shows no strip or page number.

### Old → new slide map

| old | new | data-title (current) |
|---|---|---|
| 1 | 1 (rewritten as a poster) | Trông hoàn thiện, chưa tối ưu |
| 2 | 2 | Lời hứa trên mạng |
| — | 3 (new) | Năm con số |
| 6 | 4 | Sáu thí nghiệm |
| 4 | 5 | Phân loại lỗi |
| 17 | 6 | Bản nháp build lỗi |
| 18 | 7 | “Đã sửa xong” chưa phải kết quả |
| 22 | 8 | Thiết bị thật |
| 20 | 9 | Kết quả các phép kiểm định |
| 19 | 10 | Sáu kết luận |
| 7–9 | 11–13 | Giao diện |
| 10–12 | 14–16 | Bộ nhớ |
| 13–15 | 17–19 | Bảo mật |
| 16 | 20 | Demo: trước và sau tối ưu |
| 23, 24 | removed | replaced by 21–26 |
| — | 21 (new) | Năm bậc giải pháp |
| — | 22–26 (new) | Bậc 1 · Trợ lý AI tự rà soát … Bậc 5 · Sửa đúng mẫu lỗi |
| 25 | 27 (third takeaway reworded) | Ba điều cần nhớ |
| 26–34 (D1–D9) | 28–36 (D1–D9) | unchanged |
| 21 | 37 (D10) | D10 · Đối chiếu mục tiêu môn học |
| 3 | 38 (D11) | D11 · Bốn ứng dụng chỉ một prompt |
| 5 | 39 (D12) | D12 · Ba bài học từ bảng phân loại |

### What changed in the proof table

- **Re-keyed, not rewritten.** Rows of every moved slide were renumbered by the map above. The 65 rows of old slides 1, 23, 24 and 25 were deleted.
- **Stale rows removed (9).** They were labels for cross-references or timings that no longer exist: "~40 giây" on 9, "~45 giây" and "slide 14" on 10, "slide 7" on 15, "slide 7, 8, 10" on 20, "slide 3" on D8 (35), "slide 13" on D12 (39).
- **203 rows added (63 direct, 19 derived, 121 label)** for slides 1, 3, 21–27, the new cross-references and the trimmed timings. Rows for values already proven on an older slide were copied with their source, quote and formula unchanged (for example 58/100 = `100-42`, 180 MB = `184000/1024`, gấp 2,3 lần = `2.37/1.02`). New sources: e5-optimizer/REPORT.md:43 (168 of 168 over 12 sequences), :53 (evidence arm, p = 0.25 to 1.0, 6 pairs), :89 (advice precision 0.82 to 0.94); e6-oneshot/REPORT.md:95 (rule 19 breaks 3 of 3 codex arm-R builds); e2-bench/REPORT.md:51 (12 iterations per variant).
- **Values with two readings get one row per reading** where both readings are on the slide: 12 on slide 25 (12 lần lặp, and 12–16 ms), 60 on slide 26 (60 MB, and 60 fps).
- **New label kinds:** rung numbers ("Bậc N", ladder steps) and the rule number "quy tắc 18" (an ID in the rules file).

### Decisions recorded

- **No new number was invented.** Every value on slides 3 and 21–26 already exists in a REPORT.md, a results CSV or optimization/log/opt-ui-ux.md and has a row. Where a rung has no measured effect in the study, the slide says so in words: Android Lint was not run in E3 (e3-security/REPORT.md:7), so rung 3 names R8, the compile gate and MobSF but gives Lint no number. The diff guard (e7-novel) is only mentioned in the rung-5 notes as retrospective, with no number.
- **The example prompt on rung 1 is labelled "PROMPT MẪU"** and the notes say it is an illustration, not the E5 prompt. The two quotes on rung 2 are verbatim (guardrail/ANDROID-GUARDRAIL.md:24, e6-oneshot/prompts/S.txt); the R8 lines on rung 3 are the two settings named in rule 18 (guardrail/ANDROID-GUARDRAIL.md:31).
- **"chưa đo" leaves the main slides.** The scoreboard moved to D10 with its one unmeasured row (NFC tap on a low-cost device) removed; its headline now ends "trừ lưới thẻ trang trí". Slide 8's notes point to D1 instead of saying "chưa đo". D1 keeps the full list of what was not measured. A grep of the 27 main slides for "chưa đo" returns 0.
- **Notes timing:** 1170 s (19:30), within the 20:00 budget. Trimmed on moved slides: slide 5 60 → 50 s, slide 9 40 → 30 s, slide 10 45 → 35 s. New: slide 1 15 s, slide 3 45 s, slide 21 30 s, slides 22–26 40 s each.
- **Layout check** (headless Chrome, every main slide at its last build step, `#/N/99`, at 1280×720 and 1920×1080): 0 elements outside the canvas, 0 clipped elements, smallest text 20 px at 1280 (30 px at 1920), content bottom at most 980 on slides 2–27 (the strip starts at 1012). Slide 1 has no strip; its phone fan ends at 1008.

## 2026-11-28 — proof pass for the frozen 33-slide deck (25 main + D1–D8)

The deck was rebuilt (24 → 25 main slides, new D1–D8), so every row from the 21-slide deck was stale. All rows were deleted and the table was written again from primary sources: the experiment REPORT.md files, the results CSVs, isolation-audit/AUDIT.md, 00-protocol.md, optimization/log/opt-ui-ux.md, optimization/ui-ux-metrics.md and the repo CLAUDE.md for the course targets. The ~/work inventory was not used. Result: `check_deck_proof.py` exit 0, 730 rows (407 direct, 90 derived, 233 label), 14 source files, 719 data values on 33 slides, 0 bad, 0 stale, 0 unproven.

**No number in the deck is wrong.** Every displayed value matched its source or a computation from it, so presentation.html got no number change.

### One markup change, needed so the check covers slide 4

The checker takes each slide as `<section class="slide">…</section>` with a non-greedy match, and slide 4 (the classification matrix) used `<section class="bin">` for its three columns. The match stopped at the first column's closing tag. Only the "Phổ biến" column was checked; the Ít gặp and Hiếm columns, the E4 line and all of the slide-4 notes were invisible to the check. The three bins are now `<div class="bin">`. No CSS targets `section.bin`; the headless layout check renders slide 4 the same at 1280 and 1920 (0 elements outside the canvas, content bottom 980). Slide 4 went from 6 checked values to 33.

### How the computed values are proven (kind = derived)

- **k/66 prevalence** (slides 4, 5, 7, 8, 10, 13, 19): the naive and detailed-functional counts on the same e1-corpus/REPORT.md rule line are summed, e.g. S01 `8+11` = 19. P10 and P09 use the v2 table (2b, lines 94 and 93: 30+33 = 63, 1+1 = 2), as the deck notes say. The other rules use table 2a (lines 49–77). The percentages on slide 4 are the k/66 share of those sums (`(30+33)/66*100` = 95, etc.).
- **58/100** = `100-42` (e1-corpus/REPORT.md:7).
- **MB**: RSS and heap are KB / 1024 (`184000/1024` = 179.7 → 180; `248400/1024` = 242.6 → 243; `7734/1024` = 7.55 → 7,6). APK bytes are / 1 000 000, as the slide-5 and slide-12 notes say (`24740000/1000000` = 24.7).
- **Holm-adjusted p**: raw p × (5 − rank + 1), with raw p from e1-corpus/REPORT.md:131, :133, :135, e2-bench/REPORT.md:153 and e4-pressure/results/c4_confirmatory.csv:2. C2 `0.00001*5`, C1 `0.00004*4`, C3 `0.00009*3`, C5 `0.000308*2`. For C4, 0.3106 is the raw p, and the multiplier is 1. The thresholds on D2 are 0.05/5 … 0.05/1.
- **"gấp 2,3 lần"** = `2.37/1.02` (e1-corpus/REPORT.md:135).
- **P08 11/12** = `12-1`: 11 distinct arm-O run_ids have rule_id P08 in e6-oneshot/results/findings.csv. The only arm-O run without P08 is agy-g38flash-O-r3, the run that crashed at launch.
- **60 fps** = `1000/16.67` (e2-bench/REPORT.md:45). **"gần 2 giây"** = `1901/1000`.
- **P13 "4 trong 12 tiền tố"** comes from e2-bench/results/p13_equivalence.csv. The 4 rows whose 120-row pages differ are s, si, "silent r" and "silent ri"; i runs 1..12.

### Label rows (not data), and why there are many

The checker reads the speaker notes too. These are the label rows:
- Every line number in a notes source pointer (`REPORT.md:58`).
- Every "~N giây" timing.
- File names (`00-protocol.md`, `code-01.kt`, `agent-06.txt`).
- Slide cross-references ("slide 7").
- The stage and takeaway numbers on slides 23–25.
- Forest-plot axis ticks (slide 9) and the Holm ranks (D2).
- The Kotlin version (2.4.21) and the emulator host address 10.0.2.2.

A label is used only where the value has no data reading anywhere on that slide. If the value is also data, it gets a sourced row instead. For example, 60 on slide 4 is both "~60 giây" and 60 fps, and its row cites the CLAUDE.md target.

### Checker limitations found (not deck errors; the checker was not changed)

1. **Nested `<section>`.** Any `</section>` inside a slide silently ends that slide for the checker (fixed in the deck as above). A slide checked this way only looks green.
2. **Scientific notation.** `1,84e5 KB` in the notes becomes the token "1", because the number regex refuses a digit glued to "e". Those fragments are label rows that name the full value and the row that proves it.
3. **CSV quotes.** `numbers_in` reads `148,17,0.1149` as one number, so a quote taken from a CSV line has to isolate the value between its commas (`,0.1149,`).
4. **matches() is lenient.** It accepts ×100, ×0.01, ×1000, ×0.001, a sign flip and rounding, so a long quote can pass for the wrong reason. Every quote in this table is the narrowest fragment that carries the claimed number.

### Addendum (same day): whole-app browsing RAM on slide 21 is now measured

alpha allowed one factual update to the frozen deck. The teammate's PR measured whole-app memory on a real phone: optimization/log/opt-ui-ux.md:650 and optimization/ui-ux/measure/results-xiaomi-list/results.tsv. The Xiaomi 25053RT47C ran the `measure` build through `scrollList` on 100,013 books, 5 interleaved runs per build. PSS median was 94 MB for the flat build and 95 MB for the cards build: the pssKb medians 96535 and 97493 KB, divided by 1024. RSS was about 234–235 MB. It includes shared libraries, and the log itself says not to compare RSS with the 150 MB target.

The source supports a qualified claim. The row "RAM · toàn ứng dụng ShelfTap khi duyệt — — chưa đo" became "RAM · toàn ứng dụng, cuộn danh mục, chưa có ảnh bìa (PSS) | — | 94–95 MB | đạt, Xiaomi flagship". The qualifiers are stated in the deck:
- one scenario on one flagship phone, not a low-cost device;
- the sample data has no cover images (opt-ui-ux.md:650), so the image-decode cost, the main memory risk of P06, is not in the number.

Two consistency edits follow from it:
- The slide-21 headline changed from "ba hạng mục chưa đo" to "hai hạng mục chưa đo".
- D1 "Chưa đo" changed from "mức dùng RAM khi duyệt toàn ứng dụng" to "mức dùng RAM khi duyệt có ảnh bìa thật".

The slide-21 notes carry the new path:line. 11 proof rows were added. Check: exit 0, 741 rows, 728 data values, 0 unproven. Headless check of slides 21 and 26 at 1280 and 1920: nothing outside the canvas, content bottom 980.

### Addendum (2026-11-28): build-pass cost of the rules file on slides 14, 19, 23; no bare dashes on slide 21

Validator findings F1 and F3. Slide 14 now shows, beside the warning bars and at the same size, the share of drafts that build after at most 3 logged repairs per prompt style (e1-corpus/REPORT.md:17-25, last column): naive 8/12 + 5/12 + 7/9 = 20/33, detailed-functional 3/12 + 2/12 + 7/9 = 12/33, rules file 2/12 + 3/12 + 5/10 = 10/34. These are descriptive counts, not one of the pre-registered tests; the slide tags them "số mô tả" and the notes say so. Each sum has a derived row with the formula; each term has a direct row on its table line. Slides 19 (E1 line) and 23 (phase 2, Tối thiểu) carry the trade-off in words; slide 23 adds the 20/33 → 10/34 evidence with the same rows. Slide 21: empty cells read "không áp dụng" (the one-prompt apps have no tuned version; the two ShelfTap rows measure the team's own app, with no matching AI draft) or "chưa đo" (NFC tap on a low-cost device); the table got a tighter cell padding so the last row stays above the footer. Notes timings unchanged (sum 1170 s). Check: exit 0, 763 rows, 741 data values, 0 bad, 0 stale, 0 unproven. Headless layout check of slides 14, 19, 21, 23 at 1280x720 and 1920x1080: 0 elements outside the canvas, content bottom at most 970 (footer at 1016), smallest text 20 px at 1280.

### Addendum (2026-11-28): backup slide D9, the exploratory E7a result

New backup slide 34 "D9 · Phép thử quy mô (E7, thăm dò)" after D8; main slides 1–25 are unchanged. Every value on its face and in its notes is a direct row on e7-novel/results/e7a_summary.csv (one line per app and size; the quote isolates the value between commas, e.g. `,343.2,`, see checker limitation 3) or on e7-novel/NOTE.md (the §6.1 rule on line 154, §8 lines 181–238); the 150 MB target cites CLAUDE.md:17. Values with several readings on the slide (1, 2, 4, 10) get one row per reading. No derived rows: medians are shown as in the CSV (1168.0 → 1.168) and the ratio range 1,78–2,39 is copied from NOTE.md:229. 64 rows added (54 direct, 10 label). Check: exit 0, 827 rows, 799 data values, 0 bad, 0 stale, 0 unproven. Headless check of slide 34 at 1280x720 and 1920x1080: 0 elements outside the canvas, nothing clipped, content bottom 980, smallest text 20 px at 1280.

## History: proof passes for earlier decks

Slide numbers and quoted wording below refer to the decks before the 2026-11-28 rebuild (the last one at commit cf39ad5) and do not match the current slides.

This is the 2026-11-25 pass over the deck of that date, not the current one. It was found while building the earlier rows of [deck-proof.csv](deck-proof.csv), which were all deleted and rewritten on 2026-11-28 (see Current deck). **No number in that deck contradicted its source.** The issues are wording that overstated a source, labels that were missing, and pairings that mixed run types. Section 1 lists what was changed in [../presentation.html](../presentation.html) at the time; section 2 lists what stays open and why; section 3 keeps the reviewers' full notes.

### 1. Fixed in the deck

| Slide | Was | Now | Source |
|---|---|---|---|
| 1 notes | background code called "the real samples the assistant generated" | "illustrative composite of patterns the assistant generated, not verbatim" | several lines are not in the corpus verbatim |
| 4 | "gần như bản nào cũng có vấn đề bảo mật" | "…bản nào không có guardrail cũng có ít nhất một phát hiện bảo mật" | e1 REPORT.md:127-129 (guardrailed arm 17/34) |
| 4 | "gồm cả 58 bản không build được" | "gồm cả 34 bản không build được trong 66 bản này" | e1 REPORT.md:17-24 (the bars cover naive + detailed only) |
| 5, 6 | "> 16 ms" | "> 16,67 ms" | e2 REPORT.md:45 |
| 6 | "Bóng, gradient, blur" | "Bóng, gradient, Canvas"; notes name the measured card (shadow 5 dp, gradient overlay, procedural Canvas, no blur) | e2 REPORT.md:29 |
| 7 | P01 alone "+0,471%" next to a recompose figure | labelled "TTID +0,471%" | e2 REPORT.md:197 |
| 7 | "nhanh gấp ~7 lần" | "(khác cách so khớp)" added: A is LIKE substring, B token prefix | e2 REPORT.md:221 |
| 7 | Google "~30%" set beside our TTID | "~30% của Google là tốc độ thực thi mã, không phải TTID" | references.md:372 |
| 7 | "Kết quả bảo mật E3 không đổi" | "Số cảnh báo đúng của E3 không đổi (1 và 1)" (findings fell 8 → 6) | e3 REPORT.md:58-59 |
| 10 | "Máy quét phổ biến sai 7 trên 10 lần" | "Ở cấu hình mặc định, … sai 7 trên 10 cảnh báo" | e3 REPORT.md:81-88 (0,24-0,81 by label reading) |
| 10 | "hai người chấm mù kiểm chứng, cổng 0,80" | "kiểm chứng bằng hai bên chấm, ngưỡng độ chính xác 0,80" | 00-protocol.md:121-122; the raters were not fully blind (ARTICLE §4.10) |
| 10 | "Đồng thuận kappa 0,581" | "Đồng thuận với MobSF: kappa 0,581" | e3 REPORT.md:96 |
| 11 | "dồn vào khóa Keystore" | "phần lớn ở khóa Keystore (12/17)" | e4 REPORT.md:62-64 |
| 11, 21 notes | "hai người chấm mù" | "hai bên chấm là tác tử AI, mù bộ công cụ và biến thể prompt" | e4 REPORT.md:27, 85 |
| 12 | compile failure "là do" the baselineprofile plugin | "có lẽ do …; REPORT chưa kiểm tra sâu" | e5 REPORT.md:74 |
| 12 | 168/168 | "(prompt nêu sẵn danh mục)" | e5 REPORT.md:114 |
| 13 | "12,7% → 1,5%" | "12,7% → 1,5–1,6%", and "Debug: các lượt đơn trước đó; measure: 4 lượt xen kẽ" | opt-ui-ux.md:321, 362 |
| 14 | "Trang chủ khựng vì …" | "khựng phần lớn vì …"; caveat names the two bundled changes | opt-ui-ux.md:288-289 |
| 15 | "Tốn gấp đôi" | "Tốn khoảng gấp đôi" | opt-ui-ux.md:237 |
| 15 | caveat | adds "ATF đo trên emulator (bản mới nhất: 3); chi phí kính đo trên Xiaomi" | opt-ui-ux.md:263, 490 |
| 16 | C3 "ứng dụng do người viết" | "… trước 2022" | e1 REPORT.md:588 |
| 16 notes | H4/H5 under the Holm heading | "nằm ngoài họ Holm; H5 chỉ xét U01" | 00-protocol.md:56 |
| 17 | "n dưới 36" | "n = 33–34 so với mục tiêu 36" | e1 REPORT.md:47 |
| 17 | "37 ms" | "truy vấn chậm nhất 37 ms" | e2 REPORT.md:211 |
| 20 | "Tám nhân tố" | "Bảy nhân tố và một đối chiếu phụ (P01 riêng)" | 00-protocol.md:94, ARTICLE.md:451 |
| 20 | "recompose" | "Compose_recompose" (names the metric) | e2 REPORT.md:243-244 |
| 20 notes | — | P08 on a different GPU path; P13 A 9 queries vs B 8; P06 degenerate interval, P10 none | e2 REPORT.md:62, 134, 207 |
| 21 | "Stack Overflow (23 câu)" | "23 câu trả lời: riêng ca cleartext, 4/4" | e4 REPORT.md:72 |
| 21 notes | strict count only | "Bảng dùng cách đếm chính (đếm nới cho cùng 11,5%); đếm nghiêm ngặt 26/148 = 17,6%" | e4 REPORT.md:68 |
| 22 | "Luật xuất hiện nhiều nhất" | "Bảy luật tiêu biểu … (nhánh chính, tra cứu phụ thuộc lần đầu)" | e1 REPORT.md table 2a |
| 22 | D02 row | "(chưa kiểm chứng)" | e1 REPORT.md:50 |
| 22 | "17/20 và 18/20" | "17/20 trước 2022; 18/20 từ 2024, nhóm này có thể đã dùng trợ lý" | e1 REPORT.md:615 |

One reviewer suggestion was **rejected** after checking: slide 21's table was said to use a "lenient" count. e4 REPORT.md:68 shows the table's 11,5% is the primary count, and lenient counting gives the same value.

### 2. Open (not changed, by choice)

- **Slide 13:** a later measure build reports janky 0,35–0,82%, p90 8–9 ms (opt-ui-ux.md:402). The slide keeps the first measure round, which is the one paired with the debug numbers.
- **Slide 14:** the 100.013-title catalogue size appears only in the log narrative (opt-ui-ux.md:287).
- **Slides 8, 9, 11, 16, 19:** Holm-adjusted p-values exist only in ARTICLE.md Table 5.27; the proof rows recompute them from each REPORT's raw p.
- **Slide 17:** −32,3% and −78,2% come from the filtered summary (n = 14, 13); the unfiltered summary gives −32,1% and −78,5% (n = 15).
- **Slide 19:** C1 +0,439 and C2 +3,025 are the primary analysis; the dependency-corrected re-analysis (+0,645, +3,259, e1 REPORT.md:421-422) is a sensitivity analysis.
- **Slide 5:** "MB" mixes decimal MB (bytes decoded) and MiB (RSS, heap), as the E2 REPORT does.
- **Slide 12:** "mồi" (bait) kept on purpose: the E5 design plants correct code as a lure.

### 3. Reviewers' notes, in full

#### Slides 1-6

##### Issues, slides 1-6 (proof table s1-6.csv: 75 rows, 0 bad rows, 0 unproven)

No deck number on slides 1-6 contradicts its source. The issues below are wording that overstates or mislabels what the source supports, plus two low-severity unit/threshold notes.

###### 1. Slide 1, speaker notes: "Mã chạy phía sau là các mẫu thật mà trợ lý đã sinh ra"
- Deck says: the code scrolling behind the title slide is real samples generated by the assistants.
- Deck source: presentation.html lines 921-929 hard-code a 9-pattern array (`L = [...]`), not read from the corpus.
- Corpus check (grep over optimization/study/e1-corpus/runs, 50 archived run folders): `HttpLoggingInterceptor.Level.BODY` found in 10 files, `android:allowBackup="true"` in 43, `access_token` in 20 files. NOT found verbatim: `android:usesCleartextTraffic="true"` (only "false" occurs, e.g. runs/*/AndroidManifest.xml), `setUserAuthenticationRequired(false)` (0), `shadow(24.dp).blur(16.dp)` (0), `loans.forEach { loan -> LoanCard(loan) }` (0), `putString("access_token", token)` (0). The measured P08 code uses `.shadow(elevation = if (isLarge) 10.dp else 5.dp)` and no blur (e1-corpus/runs/agy-g38flash-B1-naive-r1/src/app/src/main/java/com/example/librarycatalogue/ui/components/BookCover.kt:56-57).
- Fix: say "mã minh họa dựng từ các mẫu mà bài bắt được" (illustrative composite), or replace the lines with verbatim corpus lines.

###### 2. Slide 4, headline: "gần như bản nào cũng có vấn đề bảo mật"
- Deck says: almost every draft (title is about the 100 valid drafts) has a security problem.
- Source: only the two unguardrailed arms are 31/33 (0.939) and 29/33 (0.879) (e1-corpus/REPORT.md:127-128). The guardrailed arm is "| guardrailed | 34 | 17 | 0.500 |" (REPORT.md:129). Across all 100 valid drafts that is 77/100. Also "vấn đề bảo mật" is "at least one S-class detector finding at source level" (v1 detector, S03/S04/S08 not counted), not a confirmed vulnerability.
- Fix: "Chỉ 42 trên 100 bản nháp build được, và gần như bản nào không có guardrail cũng có ít nhất một phát hiện bảo mật (31/33 và 29/33)."

###### 3. Slide 4, caveat: "tỷ lệ trên là của mọi bản nháp ở mức mã nguồn, gồm cả 58 bản không build được"
- Deck says: the two rates include all 58 non-building drafts.
- Source: the two bars cover only naive (33) and detailed-functional (33). Recomputed from e1-corpus/results/runs.csv: non-building valid runs are naive 13, detailed-functional 21, guardrailed 24 (total 58), so the bars include 34 of the 58, not all 58.
- Fix: "gồm cả 34 bản không build được trong 66 bản này" (or "gồm cả các bản không build được").

###### 4. Slide 6, headline, mock caption and notes: "blur" is not in the measured code
- Deck says: "Bóng, gradient, blur trang trí tốn khung hình thật"; notes: "blur, bóng, gradient, alpha trong mỗi ô danh sách (P08)".
- Source: the A that was measured is "shadow 5 dp (asymmetric shape), gradient spine overlay, procedural Canvas, per card" (e2-bench/REPORT.md:29); and "the cost sits in drawing the shadow, gradient overlay and procedural cover" (REPORT.md:155). The BookCover.kt of agy-g38flash-B1-naive-r1 has `.shadow`, `Brush.horizontalGradient`, `Canvas`, `copy(alpha=...)`, but no `blur`. Blur appears only in the taxonomy definition of P08 (02-taxonomy.md:21).
- Fix: drop "blur" from the title and notes: "Bóng, gradient trang trí tốn khung hình thật". The mock caption "gradient, bóng, viền sáng, alpha" is consistent with the code.

###### 5. Slide 6, headline scope (low)
- Deck says: decorative effects cost real frames, stated generally. Source ARTICLE.md:1014 reads C5 narrowly: one E1 candidate (agy-g38flash-B1-naive-r1), one emulator GPU path (swangle_indirect), "không phải của mọi biến thể của 'vẻ ngoài AI'". The slide foot and notes carry this caveat, but the headline does not.
- Fix (optional): "Một ô lưới mẫu có bóng và gradient tốn +28,13% ..." or keep and rely on the foot.

###### 6. Slide 5 legend "khung chậm (> 16 ms)" and slide 6 caveat "quá 16 ms" (low)
- Source threshold is 16.67 ms: "the share of frames over 16.67 ms (slowFrames_pct)" (e2-bench/REPORT.md:45). A 16.3 ms frame is "> 16 ms" but not counted slow.
- Fix: "> 16,67 ms". Rows for value 16 are therefore kind=label with this note.

###### 7. Slide 5, mixed units in one sentence (low)
- "giải mã 203 MB ... 6,2 MB (−96,9%). RSS đỉnh 180 → 60 MB": decoded bytes are 2.025e+08 and 6.233e+06 bytes, i.e. decimal MB (e2-bench/REPORT.md:160). RSS and heap are in KB divided by 1024 (184008 KB = 179.7 MiB; 61600 KB = 60.2 MiB; heap 225535 KB = 220.3 MiB). Both are printed "MB" in E2 REPORT.md:171 and :188 and the deck copies that. Also 202.5 rounds to 203 only with round-half-up.
- Fix (optional): footnote "MB = 10^6 byte cho giải mã, MiB cho RSS và heap".

###### 8. Slide 6, p hiệu chỉnh Holm 0,000616 (information, not an error)
- 0.000616 is not in any experiment file. E2 REPORT.md:153 gives raw p 0.000308 and "Holm step pending". The value is ARTICLE.md:1225 (Table 5.27: rank 4 of 5, multiplier 5-4+1 = 2, 0.000308 x 2). Arithmetic and ranks checked (C2 0.00001, C1 0.00004, C3 0.00009 are smaller). Row is kind=derived with formula 0.000308*2 and says so.

###### Checked, no issue
- Slide 2 "trong vài phút": e1-corpus/results/runs.csv duration_s, 100 valid runs, median 221.9 s (about 3.7 min).
- Every E1 figure on slide 4 recomputed from raw files: 100 valid, 42 pass (13/36, 10/36, 19/28), S-class share 31/33, 29/33, 17/34 (rule set excluding unvalidated S03, S04, S08), S06 29/33 naive.
- Every E2 figure on slides 5-6 recomputed as medians from e2-bench/results/raw.csv (P08 p90 62.8726 vs 49.0694; P08 slow frames 70.52 vs 48.65; P01+P09 TTID 1901.275 vs 573.11; slow frames 90.05 vs 1.255; rows 2000 vs 31; decoded bytes 202500000 vs 6233100; RSS 184008 vs 61600 KB).
- Slide 5 "-32.3% instead of -78.2%": E2 REPORT.md:334 and :183 (the -32.3 on :143 is a different thing, P08 p99).

#### Slides 7-12

##### Issues found while proving slides 7-12 (every deck number is proven by a row; these are wording / consistency problems)

Source paths are repo-relative. Severity: M = changes what a reader concludes, L = precision / wording.

###### Slide 7 (E2)

1. **M. +1,49% and +0,471% are different metrics shown as one list (notes).**
   Deck notes: "P03+P04 (+1,49% [−14,4; 13]) và P01 riêng (+0,471%)".
   Source: optimization/study/e2-bench/REPORT.md:244 `Compose_recompose.mainMs ... | 1.49 | [-14.4, 13]` (recompose time) but REPORT.md:197 `timeToInitialDisplayMs | ... | 0.471 | [-6.44, 21.5]` (start-up time). For P01 alone the recompose metric is REPORT.md:195 `Compose_recompose.mainMs | ms | 70.23 (8.05) | 74.95 (21.27) | 6.72 | [-4.24, 29.5]`.
   Fix: say "P01 riêng: TTID +0,471% [−6,44; 21,5]" or use the same metric as P03+P04 (+6,72% [−4,24; 29,5]). The +0,471% is the smallest of P01's rows (TTFD +6.55%, recompose +6.72%).

2. **M. "nhanh gấp ~7 lần" (card P13) omits that A and B are not equivalent queries.**
   Source: e2-bench/REPORT.md:221 "The two variants are not row-for-row equal ... A is a substring match (LIKE '%q%'), B a token-prefix match, so for four of the twelve typed prefixes ... the 120-row pages differ"; also the query count differs (9 vs 8, REPORT.md:219) and per query the ratio is about 70 vs 11 ms = 6.4x, summed 7.43x.
   Fix: "~6 đến 7 lần, nhưng B dùng khớp tiền tố từ, A khớp chuỗi con (kết quả khác ở 4/12 chuỗi gõ)"; the slide caveat only says "Emulator, tương đối".

3. **M. Google's "~30%" is a different quantity than the TTID the deck compares it with.**
   Source: optimization/study/references.md:372 "Baseline Profiles improve code execution speed by about 30% from first launch" (code execution speed). Deck: "Con số ~30% là của Google. Đo của chúng tôi ...: khiêm tốn" next to −5,88% TTID.
   Fix: "Google nêu ~30% tốc độ thực thi mã; thời gian hiển thị đầu của chúng tôi giảm 5,88%".

4. **L. "Kết quả bảo mật E3 không đổi" (R8 card) is looser than the E3 table.**
   Source: e3-security/REPORT.md:58-59: optlab-noR8 n=8, tp 1, fp 7, precision 0.125; optlab-release n=6, tp 1, fp 5, precision 0.167. True positives are equal (1 and 1) but the finding count falls 8 to 6.
   Fix: "số phát hiện đúng không đổi (1 và 1); tổng phát hiện 8 → 6".

5. **L. "Strong skipping mặc định" with Kotlin 2.4.21** has no number and no primary source in e2-bench/REPORT.md (only ARTICLE.md:142, "từ Kotlin 2.0.20"). Add a reference if the claim stays on the slide.

###### Slide 8 (C3)

6. **L. Holm p 0,00027 (slide) is not in any REPORT.** Only ARTICLE.md:1224 (Table 5.27) computes it as raw p 0.00009 x 3 from the 5-decimal printed raw p (REPORT: e1-corpus/REPORT.md:135 "p = 0.00009"). Same for slide 9 (0,00016 = 0.00004 x 4; 0,00005 = 0.00001 x 5) and slide 11 (0,3106 = raw p, rank 5). They are exact only to the printed rounding; the deck should say "Holm (tính trong bài)" or keep as is.

7. **L. The notes' "riêng claude-haiku chỉ +0,340" needs its p.** e1-corpus/REPORT.md:186 "(b) only claude-haiku | 36 | +0.583 (p=0.0066) | +2.000 (p=0.0004) | +0.340 (p=0.0559)": the haiku-only delta is barely above 0.33 and its raw p is not significant; codex-luna alone is +0.919 (REPORT.md:187). Bundle heterogeneity is large, which supports the "the effect is not uniform" reading better than "chỉ +0,340".

###### Slide 9 (C1, C2)

8. **L. G3 numbers cover only validated held-out rules.** Deck notes: "Bảy luật được cố ý bỏ khỏi guardrail" (protocol 00-protocol.md:281 "Seven rules are held out"); but the +1,10 / +0,71 (e1-corpus/REPORT.md:143,145) use the validated held-out rules only (00-protocol.md:309, A1: a held-out rule enters G3 only if it passes the gate; P05 and U05 are unvalidated in the v1 track, E1 REPORT "Unvalidated rules excluded ... 'P05', ... 'U05'"). Fix: "các luật bỏ khỏi guardrail đã kiểm chứng (5 trên 7: P05 và U05 bị loại)".

###### Slide 10 (E3)

9. **M. Title "Máy quét phổ biến sai 7 trên 10 lần" overstates.** Source e3-security/REPORT.md:88: "Precision runs from 0.24 to 0.81 depending on the reading, so the headline 0.29 describes MobSF's default output on these artifacts, not a property of the apps"; strict reading (REPORT.md:81) gives precision 0.806 (about 2 of 10 wrong). One triager (REPORT.md:23, "no triage kappa"), 13 APKs, 10 from one generator pipeline (REPORT.md:136), one scanner (MobSF) plus a regex scan. The slide caveat does not carry the range; only the notes do.
   Fix: "MobSF, ở cấu hình mặc định, báo sai khoảng 7/10 cảnh báo trên 13 APK (tùy cách gán nhãn: 0,24 đến 0,81 đúng)".

10. **L. "cổng 0,80" can be read as a kappa gate.** protocol 00-protocol.md:121-122: 0.80 is the per-rule recall/precision gate; the kappa gate is 0.60 (00-protocol.md:122, ARTICLE.md:607, kappa 0.687). Fix: "cổng 0,80 cho độ nhạy và độ chính xác từng luật; kappa 0,687".

11. **L. "0,581 kappa" is detector vs MobSF raw output (80 items), not rater agreement.** e3-security/REPORT.md:96. The slide puts it right after "hai người chấm mù", so a reader may confuse it with inter-rater kappa (0.687 in E1, 0.909 in E4). Fix: label "kappa giữa hai công cụ 0,581".

###### Slide 11 (C4)

12. **M. Title "dồn vào khóa Keystore" is stronger than the data.** e4-pressure/REPORT.md:62-64: of 17 weakenings 12 are C5 Keystore (71%) and 5 are C1 cleartext (29%); the slide body itself shows 12/30 and 5/30. Also 12 of 17 come from one bundle (agy, REPORT.md:60: 12/48) and C4 cannot separate model from harness (REPORT.md:74). Fix: "phần lớn dồn vào khóa Keystore (12/17), còn lại là cleartext (5/17)".

13. **L. "ít khi làm yếu bảo mật" (17/148 = 11,5%)** is pooled over cases that include 60 performance trials (C3, C4) where no security weakening was possible; among the security cases it is 17/88 = 19.3% [12.4, 28.8] (REPORT.md:54), 42.9% for agy. The protocol endpoint is the pooled one, so the verdict stands, but the title should name the pooled denominator.

14. **L. Gauge --hi .587.** Uses 0.176 (rounded) / 0.30; unrounded 0.1763/0.30 = .588. Immaterial (less than 1 px).

###### Slide 12 (E5)

15. **L. Notes "Lỗi biên dịch là do bản sửa tự thêm plugin baselineprofile" is the REPORT's inference.** e5-optimizer/REPORT.md:74: all 10 failures share the plugin error and "the cause line behind the plugin error was not inspected further". Fix: "nhiều khả năng do ...".

16. **L. 168/168 depends on the prompt naming the taxonomy.** e5-optimizer/REPORT.md:114: "the advice ceiling says the task is easy once the taxonomy is given, not that the tools find unseeded slop"; the notes say it, the slide body does not. Title "Trợ lý tìm ra mọi vấn đề đã gieo" is accurate as written (seeded) but add "khi đã có danh mục" if space allows.

###### Checker note (not a deck error)
check_deck_proof.py passes the tiny-p rows (9e-05, 5e-05) only through the derived rule, whose tolerance is 0.5 when the value has 0 decimals, so those two formulas are not truly tested by the script; they were verified by hand (0.00009 is the REPORT raw p; 0.00001*5 = 0.00005).

#### Slides 13-18

##### Issues, slides 13-18 (all numbers proven; these are wording and provenance risks, no wrong number found)

1. Slide 13, bars and "12,7% -> 1,5%" (medium). Deck presents debug vs measure as one clean comparison. Source: optimization/log/opt-ui-ux.md:362 puts the debug figures (125/85/61/53 ms, 12,7/8,5/4,7/5,2%) as "các lượt trước hôm nay, bản đã có trang dựng sẵn"; optimization/log/opt-ui-ux.md:321 says "mỗi bản đo một lượt, chưa xen kẽ". Only the measure arm (12-16 ms, 1,5-1,6%) is 4 interleaved rounds. The debug and measure builds also differ in more than "debuggable" (R8, resource shrinking, profile install). Fix: add to the slide caveat "debug: lượt đo riêng trước đó, một lượt mỗi đoạn; measure: 4 lượt xen kẽ", and say "bản debug (không R8, có gỡ lỗi)" not only "bản debug". The headline "12,7% -> 1,5%" pairs debug segment 1 with the LOW end of measure (1,5-1,6%); use "1,5-1,6%".

2. Slide 13, stale measure figures (low). optimization/log/opt-ui-ux.md:402 records a later measure build: janky 0,35-0,82%, p90 8-9 ms, p99 13-15 ms ("1,5-1,6% ở lượt đo measure ngày 25/11 trước khi có trang dựng sẵn"). The slide shows the older 25/11 round (12-16 ms, 1,5-1,6%). Not wrong, but the slide should label them "lượt đo measure đầu" or use the newer numbers.

3. Slide 13, bar axis (low). The bar for measure is drawn at 16/130 (upper end of 12-16 ms); the other bars are value/130. Axis max 130 is not stated on the slide; fine for the proof but the label "12-16 ms" next to a 16 ms-long bar is an upper-bound choice.

4. Slide 14, headline causal wording (medium). Headline says the page stutters "vì mỗi thẻ sách gọi một ảnh bìa không bao giờ tải được". optimization/log/opt-ui-ux.md:289: "bìa lỗi chiếm phần lớn nhưng không phải toàn bộ" (cover-off build p90 34-42 ms vs 46-53 ms same pass; fixed build 29-36 ms). The slide's footer says this, the headline does not. Also the "Sửa" bundles two changes (remember failed covers + 600 dp pre-composition, optimization/log/opt-ui-ux.md:288) and the before/after cannot split them. Fix: headline "phần lớn vì"; footer "hai thay đổi gộp".

5. Slide 14, 100.013 titles (low). Only source is the log narrative (optimization/log/opt-ui-ux.md:287 "server có 100.013 đầu sách"); no seed/DB file in the repo states the count.

6. Slide 15, "4 -> 2" is the B column of optimization/ui-ux-metrics.md (2026-11-24 build, emulator), but the latest builds report ATF 3 (optimization/log/opt-ui-ux.md:490, 498, 506, 530; cause optimization/log/opt-ui-ux.md:263 scrolling under the floating button). Only the speaker notes say so; the slide face shows "2" with no caveat. Also the slide mixes devices: ATF/layout/strings on the emulator, glass cost on Xiaomi 120 Hz, not stated on the cards. Fix: footer "ATF 3 ở bản mới nhất" and device per card.

7. Slide 15, glass cost (low). "Tốn gấp đôi" rests on p90 9-11 -> 18 ms over two interleaved rounds, one device (optimization/log/opt-ui-ux.md:237 itself says "khoảng gấp đôi"); the card already marks "bản kính đầu tiên". OK, but "gấp đôi" should be "khoảng gấp đôi".

8. Slide 16, Holm p-values (provenance). Adjusted p 0,00016 / 0,00005 / 0,00027 / 0,000616 are not in any REPORT or results CSV; ARTICLE.md:1222-1226 (Table 5.27) computes them. I proved them as raw p x (6 - rank) from optimization/study/e1-corpus/REPORT.md:131,133,135 and optimization/study/e2-bench/results/c5_confirmatory.csv:2; they match. Raw p is rounded to 5 decimals in the REPORT (C1 0.00004 x4 = 0.00016 exactly as shown).

9. Slide 16, C3 label (low). "Mật độ cao hơn ứng dụng người viết": the comparison group is human code from BEFORE 30/11/2022 (ARTICLE.md:1210, optimization/study/e1-corpus/REPORT.md:135 "human pre"), not apps written by humans in general. Fix: "ứng dụng người viết trước 2022".

10. Slide 16, H4/H5 under a "sau hiệu chỉnh Holm" header (low). H4 and H5 are descriptive and outside the Holm family (optimization/study/00-protocol.md:56 "no, descriptive"); the notes say so, the slide face does not. H5 covers U01 only (U05 not verified, ARTICLE.md:779).

11. Slide 17, "n dưới 36" (low). 36 is the design target per prompt style (optimization/study/00-protocol.md:84); realised n are 33, 33, 34 (ARTICLE.md:729); per tool the cells are 36/36/28 (optimization/study/e1-corpus/REPORT.md:7). Fix: "n = 33-34 so với mục tiêu 36".

12. Slide 17, -32,3% / -78,2% (low). Taken from summary.csv (filtered, n = 14 and 13). The all-iterations file e2-bench/results/summary_all.csv gives -32.1 (n=15) and -78.5 (n=15). Both are in the REPORT sensitivity note; fine, but the slide should say "p90 CPU của khung, bản đã lọc".

13. Slide 17, 37 ms (low). 37 ms is the median over 12 runs of the SLOWEST single query (st_search_query.maxMs, optimization/study/e2-bench/REPORT.md:211), emulator. A is 96,63 ms. The row "chỉ B đạt" is correct against < 50 ms, but say "truy vấn chậm nhất".

#### Slides 19-20

##### Issues for slides 19-20 (D1 Holm table, D2 E2 table)

No deck number was found wrong: all 70 distinct values prove out (see s19-20.csv). The items below are wording or labelling problems.

###### 1. Slide 20, heading "Tám nhân tố" (eight factors) overstates
- Deck: h2 "Tám nhân tố, trung vị của các lần lặp được giữ, khoảng bootstrap 95%." The table has 8 rows.
- Source: optimization/study/ARTICLE.md:451 "Chỉ bảy nhân tố, ứng với các mẫu có thể đo độc lập: P08 ...; P06 ...; P01 cùng P09 ...; P13 ...; P03 cùng P04 ...". optimization/study/00-protocol.md:94 lists seven: "Factors (R9): P08, P06, P01+P09, P13, P03/P04, P10, P11". The eighth row, "P01 riêng", is the sub-factor table in optimization/study/e2-bench/REPORT.md:190 ("P01 sub-factor: list items without key (primary phase only)"), a contrast of the P01+P09 pair, not an eighth factor. P10 and P11 are "baseline technique" rows (REPORT.md:252, 266), not factors taken from E1 code.
- Fix: "Bảy nhân tố và một đối chiếu phụ (P01 riêng)", or drop the P01-riêng row from the count.

###### 2. Slide 20, P08 row mixes a different GPU path with the other rows, caveat missing
- Deck: P08 62,87 -> 49,07 ms sits in one table with the other factors; the only caveat is "Emulator, tương đối."
- Source: optimization/study/e2-bench/REPORT.md:134 "Measured on -gpu swangle_indirect ... not on the swiftshader_indirect of the other factors" and :155 "P08 absolute frame times are not comparable with the other factors' absolute numbers, only the within-factor contrast is"; c5_confirmatory.csv:2 rig_note (A14).
- Fix: add to the notes "P08 đo trên swangle_indirect, các nhân tố khác trên swiftshader_indirect; chỉ so A với B trong cùng một hàng."

###### 3. Slide 20, P13 "tổng truy vấn" -86,5% compares sums over unequal query counts
- Deck: P13 tìm kiếm, tổng truy vấn 634,9 ms -> 85,42 ms, -86,5% [-88,4; -84,1].
- Source: e2-bench/REPORT.md:219 "The query counts differ by one (9 against 8); the cause is not isolated, which is why per-query figures are given" (about 70 ms against 11 ms per query; slowest single query 96.63 -> 37 ms, -61.7% in summary.csv:76). REPORT.md:131: with contended iterations included, the interval of st_search_query.sumMs widens to include 0 (median -85.7%).
- Fix: either label the metric "tổng thời gian truy vấn (9 so với 8 truy vấn)" or show the per-query figure; add the sensitivity caveat if the CI is kept.

###### 4. Slide 20, heading promises a 95% bootstrap interval for every row, two rows have none
- Deck: heading "khoảng bootstrap 95%"; P06 (-96,9%) and P10 (-90,4%) show no interval.
- Source: REPORT.md:160 gives the P06 interval as the degenerate [-96.9, -96.9] (IQR 0 in both arms); REPORT.md:255 gives P10 "—" (n = 1, deterministic build); REPORT.md:264 "the size rows are the device-independent result (n = 1, deterministic)".
- Fix: footnote "P06 và P10: đo xác định (IQR 0 hoặc n = 1), không có khoảng". Not wrong, but a reader may think the intervals were dropped.

###### 5. Slide 20, P03+P04 "recompose" is ambiguous between two metrics
- Deck: P03+P04 recompose 36,21 -> 36,75 ms, +1,49% [-14,4; 13].
- Source: REPORT.md:244 is Compose_recompose.mainMs (36.21 / 36.75). REPORT.md:243 is Recomposer_recompose.mainMs, 11.85 -> 10.5 ms, -11.4% [-29.5, 7.16], also a null result. Both are in the same table.
- Fix: label the cell "Compose_recompose (ms chính)" or the Vietnamese equivalent, so the row cannot be matched to the other metric.

###### 6. Slide 19, C1 and C2 effects are the first-draft (v1) analysis, slide does not say so
- Deck: C1 +0,439, C2 +3,025 as the Holm inputs, no version label.
- Source: optimization/study/ARTICLE.md:15 "C1: ở bản nháp đầu ... hiệu +0,439". The dependency-corrected re-analysis gives C1 +0.645 and C2 +3.259 (e1-corpus/REPORT.md:421-422 table: "| C1 | +0.439, p=0.00004 | +0.645, p=0.00001 | ..."), declared exploratory/sensitivity (not confirmatory). The Holm table is correct for the registered confirmatory analysis.
- Fix: optional footnote "C1, C2 từ phân tích xác nhận (bản nháp đầu); bản sửa tra cứu phụ thuộc là độ nhạy, không xác nhận."

###### 7. Slide 19, notes claim "phép tính duy nhất của bài không chép từ REPORT" is scoped to Chapter 5
- Source: ARTICLE.md:559 "Mọi số trong chương này được chép từ các tệp REPORT và CSV ... không tính lại, trừ hiệu chỉnh Holm". True for the Chapter 5 numbers; the deck itself also converts bytes to MB on slide 20 (P10 24 737 707 B -> 24,7 MB, 2 384 488 B -> 2,4 MB; the REPORT only gives bytes in scientific notation) and computes Holm thresholds 0,0125, 0,0167, 0,025.
- Fix: "duy nhất của Chương 5" or "phép tính thống kê duy nhất"; harmless otherwise.

###### Checker limitations (not deck errors)
- p-values written as 0,00001, 0,00004, 0,00009 normalise to exponent form (1e-05 ...), which check_deck_proof.py cannot compare with a quote (decimals() returns 0). Those three rows are kind=derived with formula equal to the printed value and the exact source line in source/line/quote. The adjusted p 0,00005 passes only because the formula tolerance is 0.5 for exponent-form values; its arithmetic (0.00001*5) is correct.
- Raw p-values are the printed rounded ones, so adjusted p are exact only at the printed rounding (ARTICLE.md:1216 says the same). 0.00001 for C2 is the floor of a 100,000-permutation test.
- P06 203 MB: median 202,500,000 B = 202.5 MB exactly, shown 203 (half-up, as in REPORT.md:171).

#### Slides 21-22

##### Issues, slides 21-22

All 38 data values of slides 21 and 22 are proven (56 rows, bad rows 0, unproven 0). No number is wrong. The items below are wording or labelling problems.

###### Slide 21 (D3, E4 detail)

1. Notes: "Hai người chấm mù, kappa 0,909 trên 134 diff."
   - Source: optimization/study/e4-pressure/REPORT.md:27 "two raters blind to the tool bundle and the prompt variant ... Rater 1 is beta-f2-4, rater 2 is an independent agent spawned by alpha". Both raters are AI agents, not people. REPORT.md:85: "Rater 1 saw the project cases while designing the rubric".
   - The number 0,909 and 134 are right (results/kappa.txt:1). The word "người" (people) overstates what the source supports.
   - Fix: "Hai bên chấm (hai tác tử), mù bộ công cụ và biến thể prompt, kappa 0,909 trên 134 diff."

2. Table row "cleartext HTTP (S03) 5/30" mixes counting rules with the notes line "Đếm nghiêm ngặt: 26/148 = 17,6%".
   - Source: results/summary.csv, row case=c1-cleartext any weakening 30,5 is the LENIENT (primary) count. The strict count adds 9 domain-scoped cleartext trials, all in c1-cleartext (results/auto_labels.csv tag dev-host-scoped-in-main, 9 rows, all c1). Strict C1 would be 14/30, and the agy and codex shares would rise.
   - Not an error, but the slide does not say the table is the lenient count. Fix: label the table "đếm thường (lenient)" next to "đếm nghiêm ngặt".

3. "Stack Overflow (23 câu): khuyên bật cleartext 4/4."
   - Source: e4-pressure/REPORT.md:72 "Human baseline (Stack Overflow census, 23 labelled answers). C1: 4 of 4 answers recommend the cleartext opt-in". The 23 are answers, not questions ("câu" is ambiguous), and 4/4 is only the C1 subset (4 answers), not 4 of 23.
   - Fix: "Stack Overflow (23 câu trả lời): riêng ca cleartext, 4/4 câu trả lời khuyên bật cleartext."

###### Slide 22 (D4, common rules)

4. Title "Luật xuất hiện nhiều nhất, theo kiểu prompt." overstates. The table is a selection of 7 rules, not the most frequent ones.
   - Source: optimization/study/e1-corpus/REPORT.md table 2a: D01 25/33, 22/33, 25/34 (line 49), S11 12/33 in detailed (line 72), U05 16/34 guardrailed (line 77), P02 12/33 naive (line 52) are more frequent than S01 8/33, P08 8/33, U04 9/33, S05 4/33 that the slide lists. P10 (33/33, 33/33, 34/34, line 60) is omitted because it is unvalidated.
   - Fix: "Bảy luật tiêu biểu theo kiểu prompt" or "Luật phổ biến nhất trong các luật đã kiểm chứng và có chênh lệch giữa các kiểu".

5. Row "D02 phụ thuộc cũ 33/33, 33/33, 34/34" is shown without its status.
   - Source: e1-corpus/REPORT.md:50 status column "pending" (ARTICLE.md Table 5.4 marks D02 with a dagger, "pending"). D02 has no row in results/validation/rules_status.csv.
   - Fix: add a footnote "D02: chưa kiểm chứng độ chính xác".

6. Note "ứng dụng người viết cũng có (17/20 và 18/20)".
   - Source: e1-corpus/REPORT.md:615 "The human 2024+ stratum may itself contain assistant-written code". So "người viết" is not guaranteed for the 2024+ group (18/20). The pre-2022 group (17/20) is safe.
   - Also the strata are not named on the slide. Fix: "ứng dụng GitHub trước 2022 (17/20) và từ 2024 (18/20, có thể chứa mã do trợ lý viết)".

7. Table 2a is the first-scan dependency lookup (v1). REPORT.md:597 says the lookup was corrected post hoc. D02 and S11 are the affected rules. The slide numbers match tables 2a and 2b (first-scan lookup) and my recount of results/findings.csv; I did not check the corrected-lookup per-rule D02 counts, which have no per-rule table in REPORT.md. The slide does not say which track it uses. Fix: footnote "nhánh v1, quét lần đầu".

###### 7. Slides 5 and 31, "0 dấu vết" is looser than the isolation audit

- Slide 5: "623 bản ghi hội thoại quét lại: 0 dấu vết". Slide 31: "không có dấu vết quy tắc riêng nào".
- Source: isolation-audit/AUDIT.md:36-50. Zero runs open with "Cleanse", zero use MCP/skills/slash, zero hook events (E1/E4/E5 main rows). But the scan lists 3 strong-marker hits: E1 claude-haiku-B3-naive-r2 (usage-meter UI toast, a UI notice, not model context), E4 c1-agy-B-r4 (home-dir listing, which slide 31 already admits), and the 2 real-home controls (by design). AUDIT.md:14 also says the 207 stub-HOME Claude runs "contain no marker", which the audit's own table does not fully back for the toast run.
- Proof row 0 on slides 5 and 31 quotes the E1 main row of the audit table and states what the 0 counts. The number is honest only as "0 runs show rule text in the model context"; the strict "0 traces" is not.
- Fix: slide 5 "0 lần có quy tắc riêng lọt vào hội thoại" (or "0 lần mở bằng quy tắc riêng"), and let slide 31 keep the two admitted exceptions.
