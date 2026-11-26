# Language review — Vietnamese wording of the deck and the article

Done 2026-11-25. A Gemini review (gemini-4-hydrogen-high) of every slide and speaker note, then a manual pass. Section 1 is what was changed; section 2 is the review as received, kept for the next pass. Terms fixed for the study are in [../PLAYBOOK.md](../PLAYBOOK.md) §8.

## 1. Applied

**In presentation.html**

| Was | Now | Why | Times |
|---|---|---|---|
| khoản mượn | lượt mượn | loan = book borrowing, not a money loan | 1 |
| lỗi cảm ứng | lỗi cố ý gây ra | "cảm ứng" means touch(screen); these are induced errors | 1 |
| khoảng 95% | khoảng tin cậy 95% | bare "khoảng 95%" reads as "about 95%" | 3 |
| giới hạn công suất | giới hạn lực kiểm định | "công suất" is wattage/capacity, not statistical power | 1 |
| kết quả rỗng | kết quả không khác biệt (null) | "rỗng" means empty, not a null result | 1 |
| ứng dụng người viết | ứng dụng do người viết | without "do" it reads "the writer's app" | 4 |
| văn bản thường | dạng rõ (plaintext) | "văn bản thường" is ordinary text, not cryptographic plaintext | 3 |
| viết cứng | gán cứng | standard term for hard-coded | 2 |
| phân loại tay | phân loại thủ công | consistency with slide 18 | 1 |
| Rút lại | Đính chính | standard word for correcting a published result | 1 |
| Dựng lười | LazyColumn | "dựng lười" is a literal calque of lazy composition | 1 |
| dựng lười | LazyColumn | same | 1 |
| cổng 0,80 | ngưỡng 0,80 | "cổng" is a calque of "gate" | 1 |
| cổng biên dịch | bước kiểm tra build | "cổng" sounds like a port | 1 |
| gieo sẵn | cài sẵn | "gieo" (sowing) is a calque of "seeded" | 1 |
| vấn đề gieo | vấn đề cài sẵn | same | 2 |
| nói ở vị trí ngang bằng | trình bày ngang hàng với kết quả ủng hộ | calque of "on equal footing" | 1 |
| trợ lý phát ra X | trợ lý sinh ra X | "phát ra" is emit as in light/sound | 1 |
| tỷ lệ nền của người | tỷ lệ nền ở mã do người viết | too compressed | 1 |
| bộ dựng hình | bộ kết xuất (renderer) | "dựng hình" sounds like 3D modelling | 2 |
| Chính tiêu đề 0,291 | Chính con số 0,291 | "tiêu đề" is a heading, not a headline number | 1 |
| không phòng thủ | không né tránh | calque of "non-defensive" | 1 |
| không nhận đoán là kết quả | không coi suy đoán là kết quả | ungrammatical | 1 |
| giá của kính | chi phí của hiệu ứng kính mờ | reads as "the price of glass" | 1 |
| theo ca và bộ công cụ | theo lỗi và bộ công cụ | "ca" is a shift or a medical case | 1 |
| Hai trùng nhãn của A mất | Hai lỗi trùng nhãn ở bản A biến mất | ungrammatical | 1 |
| đo sai thứ | đo nhầm đối tượng | informal calque | 1 |
| Đo của chúng tôi trên một ứng dụng một màn hình | Kết quả đo của chúng tôi trên một ứng dụng một màn hình | bare verb as subject | 1 |
| lỗi chặn đường | lỗi làm tắc công việc | "chặn đường" is blocking a road | 1 |
| ngưỡng hiệu ứng đặt trước | ngưỡng độ lớn hiệu ứng đặt trước | "hiệu ứng" alone = visual effect | 1 |
| hiệu ứng (A − B) / B | chênh lệch (A − B) / B | same; collides with the UI effects on the slide | 1 |
| bị bắt trong bài | bị phát hiện trong bài | "bị bắt" = arrested | 1 |
| Chúng tôi đọc là | Chúng tôi diễn giải là | calque of "we read this as" | 1 |
| dấu plugin KSP cũ | plugin marker KSP cũ | "dấu" = stamp/diacritic; Gradle term is plugin marker | 1 |
| giải mã đủ ảnh 1500×2250 | giải mã ảnh ở đủ kích thước gốc 1500×2250 | ambiguous | 1 |
| thời gian đến hiển thị đầu | thời gian tới khung hình đầu | calque of "time to initial display" | 1 |
| BODY HTTP | phần thân (body) HTTP | English word order | 1 |
| cây codex | mã nguồn do codex sửa | "cây" (source tree) is not used for a codebase | 1 |
| quy tắc hồ sơ | quy tắc Baseline Profile | "hồ sơ" = dossier/CV | 2 |
| nhận hồ sơ | nhận Baseline Profile | same | 1 |
| Hồ sơ của app chỉ lớn hơn 395 byte; phần lớn hồ sơ đến từ thư viện Compose | Baseline Profile của app chỉ lớn hơn 395 byte; phần lớn profile đến từ thư viện Compose | "hồ sơ" = dossier | 1 |
| phần thắng là của LazyColumn (P09) | mức cải thiện đến từ LazyColumn (P09) | sports phrasing | 1 |
| <td>tổng truy vấn</td> | <td>tổng thời gian truy vấn</td> | without "thời gian" it reads as a query count | 1 |
| nên bìa lỗi là phần lớn nhưng không phải toàn bộ | nên ảnh bìa lỗi gây ra phần lớn, nhưng không phải toàn bộ, độ giật | missing predicate | 1 |
| nút bị vô hiệu | nút bị vô hiệu hóa | truncated term | 1 |

**In ARTICLE.md, presentation-script.md, slides-outline.md, figures/make_figures.py, figures/PLAN.md** (figures and ARTICLE.html rebuilt)

| Was | Now | ARTICLE | script | outline |
|---|---|---|---|---|
| khoản vay | lượt mượn | 8 | 1 | 1 |
| khoản mượn | lượt mượn | 1 | 0 | 0 |
| lỗi cảm ứng | lỗi cố ý gây ra | 1 | 0 | 0 |
| công suất | lực kiểm định | 8 | 2 | 4 |
| kết quả rỗng | kết quả không khác biệt (null) | 4 | 1 | 0 |
| ứng dụng người viết | ứng dụng do người viết | 12 | 1 | 1 |
| dạng văn bản thường | dạng rõ (plaintext) | 1 | 0 | 0 |
| văn bản thường | dạng rõ (plaintext) | 2 | 1 | 1 |
| viết cứng | gán cứng | 4 | 0 | 0 |
| đặt lại hồ sơ | đặt lại profile | 1 | 0 | 0 |
| ; hồ sơ sinh trên | ; profile sinh trên | 1 | 0 | 0 |
| A: không hồ sơ | A: không có Baseline Profile | 1 | 0 | 0 |
| hồ sơ Baseline Profile | Baseline Profile | 1 | 0 | 0 |

**Not applied, on purpose:** "luật" (rule) is kept: it is the study's term for detector rules across every report and CSV, and Vietnamese developers say "luật lint". "đóng băng" (frozen protocol), "điểm cuối" (endpoint), "gieo" in ARTICLE.md, "mồi" (bait) and "vẻ ngoài AI" are kept as the study's defined terms. Pure style suggestions (slide 1 title, "tốn gì về bảo mật") are left to the presenter.

## 2. Gemini review, as received

| slide | exact current text | suggested text | why |
|---|---|---|---|
| Slide 1 (VISIBLE) | `Rồi chúng tôi đo nó.` | `Rồi chúng tôi tiến hành đo đạc.` | "đo nó" is a literal calque of "measured it"; Vietnamese prefers omitting the pronoun or naming the object/activity. |
| Slide 1 (VISIBLE) | `tốn gì về giao diện, bộ nhớ và bảo mật` | `gây đánh đổi gì về giao diện, bộ nhớ và bảo mật` (or `tốn gì về giao diện, bộ nhớ và có rủi ro gì về bảo mật`) | "tốn" works for memory/CPU, but "tốn gì về bảo mật" is unnatural—security is compromised or put at risk, not "spent". |
| Slide 1 (VISIBLE) | `5 phép kiểm định đăng ký trước` | `5 phép kiểm định được đăng ký trước (preregistered)` | "đăng ký trước" alone sounds like signing up in advance for an event; adding "được" and the study sense avoids confusion. |
| Slide 1 (NOTES) | `dòng đỏ là dòng sẽ bị bắt trong bài.` | `dòng đỏ là dòng sẽ bị bộ kiểm tra phát hiện trong bài.` | "bị bắt" is a calque of "caught" (as by a person/police); static rules "phát hiện" or "cảnh báo" code issues. |
| Slide 2 (VISIBLE) | `danh sách dựng hết` | `toàn bộ danh sách bị dựng sẵn` | "danh sách dựng hết" is ambiguous/awkwardly ordered; sounds like "the list is used up/finished". |
| Slide 2, 10, 22 (VISIBLE) | `token văn bản thường` | `token dạng rõ (plaintext)` or `token văn bản thuần` | "văn bản thường" means ordinary/unformatted text, not cryptographic plaintext (calque of "plain text"). |
| Slide 3 (VISIBLE) | `luật chơi được khóa trước khi có dữ liệu` | `giao thức được chốt trước khi có dữ liệu` | "luật chơi" is too informal for a university talk, and "khóa trước" is a calque of "locked before". |
| Slide 3 (VISIBLE) | `3 bộ công cụ × 3 brief × 3 kiểu prompt` | `3 bộ công cụ × 3 đề bài × 3 kiểu prompt` | "brief" is agency/design jargon; "đề bài" or "mô tả yêu cầu" (used on Slide 2) is standard in Vietnamese CS talks. |
| Slide 3 (VISIBLE) | `phân loại tay` | `phân loại thủ công` | "phân loại tay" is an unnatural calque of "manual classification" and conflicts with "phân loại thủ công" on Slide 18. |
| Slide 3 (VISIBLE) | `làm yếu bảo mật để hết lỗi?` | `hạ thấp bảo mật để vượt qua lỗi?` (or `làm suy yếu bảo mật...`) | "làm yếu" is a weak calque of "weaken"; "để hết lỗi" sounds as if the security relaxation actually fixes the root bug. |
| Slide 3, 12 (VISIBLE/NOTES) | `14 vấn đề gieo sẵn` / `vấn đề gieo` | `14 vấn đề cài sẵn` | "gieo" (sowing seeds) is a literal translation of "seeded" and sounds strange to a CS lecturer; "cài sẵn" or "cấy sẵn" is standard. |
| Slide 3 (VISIBLE) | `ngưỡng hiệu ứng đặt trước` | `ngưỡng độ lớn hiệu ứng đặt trước` | "hiệu ứng" alone means visual effects (as on Slide 6), not statistical effect size. |
| Slide 3 (VISIBLE) | `“trợ lý phát ra X với tỷ lệ r, so với tỷ lệ nền của người”` | `“trợ lý sinh ra X với tỷ lệ r, so với mức nền ở mã do người viết”` | Code assistants do not "phát ra" (emit like radio/light) vulnerabilities; "tỷ lệ nền của người" is overly compressed. |
| Slide 3 (NOTES) | `khoản mượn, lịch sử đọc` | `lượt mượn, lịch sử đọc` | Leftover "khoản mượn" from "loan"; "khoản" is for money/debts, while library borrowing is "lượt mượn" (as on Slide 5). |
| Slide 3 (NOTES) | `Giao thức đóng băng trước` | `Giao thức được chốt cố định trước` | "đóng băng" is a literal calque of "frozen protocol". |
| Slide 3 (NOTES) | `được nói ở vị trí ngang bằng.` | `được trình bày bình đẳng như kết quả ủng hộ.` | "nói ở vị trí ngang bằng" is an awkward calque of "reported on equal footing". |
| Slide 4 (VISIBLE) | `phát hiện nhóm bảo mật (S)` | `cảnh báo thuộc nhóm bảo mật (S)` | Using "phát hiện" as a countable noun is a calque of "finding(s)" and misses "thuộc". |
| Slide 4 (VISIBLE) | `ở mức mã nguồn` | `ở cấp độ mã nguồn` (or `tính trên mã nguồn`) | "mức mã nguồn" is a calque of "source-code level"; Vietnamese uses "cấp độ" or "mức độ". |
| Slide 4 (NOTES) | `101 bản nháp sinh ra, 100 hợp lệ.` | `101 bản nháp được sinh ra, 100 bản hợp lệ.` | Missing passive marker "được" and classifier "bản", making it sound telegraphic. |
| Slide 5 (VISIBLE) | `Dựng lười: còn 1,3%.` | `LazyColumn (chỉ dựng phần hiển thị): còn 1,3%.` | "Dựng lười" is a literal calque of "lazy rendering/composition" that sounds humorous/odd in Vietnamese. |
| Slide 5 (VISIBLE) | `thời gian đến hiển thị đầu (TTID)` | `thời gian hiển thị khung hình đầu (TTID)` | "đến hiển thị đầu" is a word-for-word calque of "to initial display". |
| Slide 5 (VISIBLE) | `phần thắng là của dựng lười (P09).` | `mức cải thiện chủ yếu đến từ LazyColumn (P09).` | "phần thắng" (the win/victory) is colloquial sports phrasing for a benchmark attribution. |
| Slide 5 (NOTES) | `giải mã đủ ảnh 1500×2250 cho 15 ảnh.` | `giải mã toàn bộ kích thước gốc 1500×2250 cho 15 ảnh.` | "giải mã đủ ảnh" is unclear ("decoded enough images"? vs. full resolution). |
| Slide 5, 17 (NOTES/VISIBLE) | `bộ dựng hình` | `bộ kết xuất đồ họa (renderer)` | "bộ dựng hình" sounds like 3D modeling/rigging rather than a GPU rendering backend. |
| Slide 6 (VISIBLE) | `Thẻ lưới “vẻ ngoài AI”` | `Thẻ giao diện “phong cách AI”` | "Thẻ lưới" is ambiguous ("grid card" vs "card grid"), and "vẻ ngoài AI" is a literal calque of "AI look". |
| Slide 6 (VISIBLE) | `hiệu ứng (A − B) / B, khoảng 95%` | `mức chênh lệch (A − B) / B, khoảng tin cậy 95%` | Two issues: "hiệu ứng" collides with UI effects two lines above, and "khoảng 95%" in Vietnamese reads as "approximately 95%" rather than a 95% CI. |
| Slide 6 (VISIBLE) | `Một ứng viên E1, một đường GPU của emulator` | `Một mẫu từ E1, một cấu hình GPU của emulator` | "ứng viên" means job/election candidate, and "đường GPU" is a literal calque of "GPU path". |
| Slide 7 (VISIBLE) | `tạo khác biệt ở nơi người dùng thấy.` | `tạo khác biệt rõ rệt cho trải nghiệm người dùng.` | "ở nơi người dùng thấy" is a calque of "where the user sees". |
| Slide 7 (VISIBLE) | `Đo của chúng tôi trên một ứng dụng một màn hình: khiêm tốn.` | `Kết quả đo của chúng tôi trên ứng dụng một màn hình khá khiêm tốn.` | Nominalizing bare verb "Đo" as the subject ("Đo của chúng tôi") is ungrammatical in Vietnamese. |
| Slide 7 (VISIBLE) | `khoảng 95% chứa 0` | `khoảng tin cậy 95% chứa 0` | Missing "tin cậy"; "khoảng 95%" reads as "~95%". |
| Slide 7 (NOTES) | `lên thao tác nhưng không tới khung hình` | `cải thiện tốc độ thao tác nhưng không ảnh hưởng tốc độ khung hình` | Overly telegraphic; "lên thao tác" is unidiomatic. |
| Slide 7 (NOTES) | `Hai nhân tố cho kết quả rỗng` | `Hai nhân tố không ghi nhận khác biệt (null result)` | "kết quả rỗng" is a mistranslation of "null result" ("rỗng" means empty set/null pointer, not statistical non-significance). |
| Slide 7 (NOTES) | `với Kotlin 2.4.21, nơi strong skipping là mặc định.` | `với Kotlin 2.4.21, trong đó strong skipping là mặc định.` | "nơi" is for physical/abstract locations, not a software version (calque of English "where"). |
| Slide 8, 16, 22 (VISIBLE) | `ứng dụng người viết` | `ứng dụng do người viết` | Missing "do"; "ứng dụng người viết" reads as "the author's/writer's app". |
| Slide 9 (VISIBLE) | `cắt đôi số loại vấn đề` | `giảm một nửa số loại vấn đề` | "cắt đôi" ("cut in half") sounds physical/colloquial for abstract issue counts. |
| Slide 9, 22 (VISIBLE/NOTES) | `luật bị cố ý bỏ khỏi guardrail` / `Luật phổ biến` | `quy tắc bị cố ý bỏ khỏi guardrail` / `Quy tắc phổ biến` | "luật" sounds like legal statutes; lint/static-analysis rules are "quy tắc". |
| Slide 9 (VISIBLE) | `Chúng tôi đọc là khái quát hóa hoặc ngẫu nhiên` | `Chúng tôi diễn giải đây là do khái quát hóa hoặc ngẫu nhiên` | "đọc là" is an English calque ("we read this as"). |
| Slide 9 (VISIBLE) | `bị giới hạn công suất.` | `bị giới hạn lực kiểm định (statistical power).` | "công suất" means electrical/mechanical wattage or factory capacity, NOT statistical power. |
| Slide 10 (VISIBLE) | `bắt được công cụ của chính mình nói sai.` | `phát hiện chính công cụ của mình cũng báo sai.` | "bắt được ... nói sai" over-personifies a static analyzer. |
| Slide 10 (VISIBLE) | `hai người chấm mù kiểm chứng, cổng 0,80` | `hai người gán nhãn độc lập (đánh giá mù), ngưỡng đồng thuận 0,80` | "cổng" is a literal calque of "gate" (threshold); "chấm mù" is slightly odd without "đánh giá". |
| Slide 10 (VISIBLE) | `nhật ký ghi cả BODY HTTP.` | `nhật ký ghi cả phần thân (body) của HTTP.` | All-caps "BODY HTTP" reverses Vietnamese noun order awkwardly. |
| Slide 10 (VISIBLE) | `tra cứu dùng dấu plugin KSP cũ.` | `bộ tra cứu dùng mã nhận diện plugin KSP cũ.` | "dấu" means stamp/diacritic, not a plugin signature/identifier. |
| Slide 10 (NOTES) | `Chính tiêu đề 0,291 là đầu ra mặc định của MobSF` | `Con số chính 0,291 là đầu ra mặc định của MobSF` | "tiêu đề" (heading/title) is a mistranslation of "headline (number)". |
| Slide 10 (NOTES) | `0 artifact không tồn tại trong 2105 tọa độ` | `0 thư viện không tồn tại trong 2.105 tọa độ` | Leaving "artifact" in English breaks consistency with "tọa độ thư viện" on Slide 16. |
| Slide 10 (NOTES) | `Sửa tra cứu là độ nhạy post hoc` | `Việc sửa tra cứu là phân tích độ nhạy hậu kiểm (post-hoc)` | Incomplete calque of "post-hoc sensitivity (analysis)". |
| Slide 11 (VISIBLE) | `làm yếu bảo mật để thoát` | `làm suy yếu bảo mật để vượt qua lỗi` | "để thoát" is an abrupt calque of "to escape/get past". |
| Slide 11 (VISIBLE) | `khoảng 95%, so với ngưỡng 0,15` | `khoảng tin cậy 95%, so với ngưỡng 0,15` | Same "khoảng 95%" ambiguity as Slides 6–7. |
| Slide 11 (VISIBLE) | `không đủ bác bỏ : khoảng vẫn chạm ngưỡng.` | `chưa đủ bằng chứng bác bỏ: khoảng tin cậy vẫn chạm ngưỡng.` | Missing object ("bằng chứng") and "tin cậy". |
| Slide 11 (NOTES) | `5 lỗi cảm ứng` | `5 lỗi giả lập` (or `5 lỗi được cố ý tạo ra`) | Critical mistranslation of "induced errors": "cảm ứng" in Vietnamese means touchscreen/touch! |
| Slide 11 (NOTES) | `Khi gặp lỗi chặn đường` | `Khi gặp lỗi chặn luồng làm việc` (or `gặp lỗi gây tắc`) | "chặn đường" literally means blocking a physical road. |
| Slide 12 (VISIBLE) | `9/12 cây codex đã sửa không biên dịch được` | `9/12 mã nguồn do codex sửa không biên dịch được` | "cây" (calque of "source tree") is not used in Vietnamese to mean a codebase/project. |
| Slide 12 (VISIBLE/NOTES) | `mã đúng dùng làm mồi` / `3 mồi` | `mã đúng làm mẫu đối chứng` / `3 mẫu đối chứng` | "mồi" means fishing bait or prey; in experiments, decoys/controls are "mẫu đối chứng". |
| Slide 12 (VISIBLE) | `Người phán là bộ kiểm tra độc lập` | `Tiêu chuẩn đánh giá là bộ kiểm tra độc lập` | "Người phán" sounds like a human judge/referee for an automated test suite. |
| Slide 13 (VISIBLE) | `là của bản debug , không phải của mã ứng dụng.` | `bắt nguồn từ bản debug, không phải từ mã ứng dụng.` | "là của" is a slightly awkward possessive calque ("is of the debug build"). |
| Slide 13 (VISIBLE/NOTES) | `Bản measure : như release (R8, nhận hồ sơ)` / `quy tắc hồ sơ` | `Bản measure: giống release (R8, hỗ trợ Baseline Profile)` / `quy tắc Baseline Profile` | "hồ sơ" means personal dossier/CV in Vietnamese; keep `Baseline Profile` as on Slides 7 and 22. |
| Slide 13 (VISIBLE) | `Rút lại` | `Đính chính` | "Rút lại" sounds like withdrawing from an event or taking back a promise; "Đính chính" is standard for correcting prior results. |
| Slide 13 (VISIBLE) | `quy tắc hồ sơ của app không khác nhau đo được .` | `quy tắc Baseline Profile của app không tạo khác biệt đo được.` | Ungrammatical subject-predicate ("rules do not differ measurably" instead of "make no measurable difference"). |
| Slide 13 (NOTES) | `đo trên bản debug là đo sai thứ.` | `đo trên bản debug là đo nhầm đối tượng.` | "đo sai thứ" is an informal calque of "measuring the wrong thing". |
| Slide 13 (NOTES) | `Nhật ký giữ nguyên mục sai` | `Nhật ký thí nghiệm giữ nguyên mục sai` | Clarifies that this is lab/experiment notes, distinct from app logs ("nhật ký") elsewhere. |
| Slide 14 (VISIBLE) | `gọi một ảnh bìa` | `gửi yêu cầu tải một ảnh bìa` | "gọi một ảnh" is colloquial/calqued from "calls/fetches an image". |
| Slide 14 (VISIBLE) | `không tới được từ điện thoại thật.` | `không truy cập được từ điện thoại thật.` | "tới được" is too colloquial for network reachability. |
| Slide 14 (VISIBLE) | `bìa lỗi là phần lớn` | `ảnh bìa lỗi là nguyên nhân chính` | Missing predicate noun ("nguyên nhân"); a noun cannot just "be the majority". |
| Slide 14 (NOTES) | `không nhận đoán là kết quả.` | `không coi suy đoán là kết quả.` | "nhận đoán" is ungrammatical/awkward. |
| Slide 15 (VISIBLE) | `nút bị vô hiệu` | `nút bị vô hiệu hóa` | Truncated form of "vô hiệu hóa" (disabled). |
| Slide 15, 22 (VISIBLE) | `chuỗi viết cứng` / `màu viết cứng` | `chuỗi gán cứng (hardcode)` / `màu gán cứng` | "viết cứng" is a literal calque of "hard-coded"; Vietnamese devs say "gán cứng" or "hardcode". |
| Slide 15 (VISIBLE) | `giá của kính` | `chi phí của hiệu ứng kính mờ` | "giá của kính" literally means "the price of glass". |
| Slide 15 (VISIBLE) | `sát ngân sách 60 Hz.` | `sát ngưỡng thời gian khung hình 60 Hz.` | "ngân sách 60 Hz" is a literal calque of "60 Hz budget" and sounds odd without "khung hình". |
| Slide 15 (VISIBLE) | `vỡ bố cục do xem ảnh, chờ người trong nhóm duyệt.` | `vỡ bố cục phát hiện qua kiểm tra ảnh, chờ thành viên nhóm duyệt.` | "do xem ảnh" currently reads as "layout breaks caused by viewing images" instead of "observed via screenshot inspection". |
| Slide 15 (NOTES) | `Hai trùng nhãn của A mất` | `Hai lỗi trùng nhãn ở bản A biến mất` | Missing classifier/noun ("lỗi"); "hai trùng nhãn" is ungrammatical. |
| Slide 16 (NOTES) | `điều kiện thống kê đã đăng ký thỏa` | `điều kiện thống kê đã đăng ký được thỏa mãn` | Bare "thỏa" at clause end sounds abruptly truncated in formal speech. |
| Slide 17 (NOTES) | `Nói thẳng, không phòng thủ.` | `Nói thẳng, không né tránh.` | "không phòng thủ" is a calque of "non-defensive" ("phòng thủ" in Vietnamese means military/sports defense). |
| Slide 18 (VISIBLE) | `cổng biên dịch` | `bước kiểm tra biên dịch (build gate)` | "cổng biên dịch" sounds like a network/hardware port. |
| Slide 19 (VISIBLE) | `điểm cuối` | `tiêu chí đánh giá` | "điểm cuối" is a literal calque of statistical "endpoint" (which means final physical point or API endpoint in Vietnamese). |
| Slide 19 (NOTES) | `Holm bước xuống trên m = 5.` | `Hiệu chỉnh Holm giảm dần (step-down) với m = 5.` | "bước xuống" is a literal calque of "step-down". |
| Slide 19 (NOTES) | `không chép từ REPORT.` | `không trích từ REPORT.` | "chép" carries a negative connotation (copying/cheating); "trích" is academic. |
| Slide 20 (VISIBLE) | `các lần lặp được giữ` | `các lần lặp hợp lệ` | "được giữ" is a vague calque of "retained" (retained runs). |
| Slide 20 (VISIBLE) | `tổng truy vấn` | `tổng thời gian truy vấn` | Without "thời gian", "tổng truy vấn" means total count of queries, not duration. |
| Slide 21 (VISIBLE) | `theo ca và bộ công cụ` | `theo tình huống lỗi và bộ công cụ` | "ca" is used for medical cases or work shifts, not software error scenarios. |
| Slide 22 (VISIBLE) | `phụ thuộc cũ` | `thư viện phụ thuộc cũ` | Bare "phụ thuộc" sounds like an adjective/verb ("old dependence") rather than software dependencies. |

### Terms to keep consistent across the deck

- **Book borrowing instance**: **`lượt mượn`** (fix `khoản mượn` in Slide 3 NOTES to match Slide 5).
- **Static/lint rules**: **`quy tắc`** (replace `luật` / `mã luật` on Slides 9, 22).
- **Tool findings**: **`cảnh báo`** or **`mục phát hiện`** (avoid bare countable noun `phát hiện` on Slides 4, 8, 9, 10, 15, 16).
- **Visual effects vs. statistical effect size**: **`hiệu ứng`** only for UI visual effects (Slides 6, 22); **`độ lớn hiệu ứng`** for statistics (replace `hiệu ứng` / `ngưỡng hiệu ứng` / `độ lớn` across Slides 3, 6, 19).
- **Confidence interval**: **`khoảng tin cậy 95%`** (fix bare `khoảng 95%` / `khoảng` on Slides 6, 7, 11).
- **Baseline Profile**: **`Baseline Profile`** everywhere (replace `hồ sơ` / `nhận hồ sơ` / `quy tắc hồ sơ` on Slide 13).
- **Build vs. compile**: **`build được`** for whether an app builds (Slides 2, 4, 8, 9, 17), **`bản dựng`** for a build output, and **`biên dịch`** strictly for compilation (Slides 12, 18).
- **Manual labeling/classification**: **`phân loại thủ công`** (fix `phân loại tay` on Slide 3 to match Slide 18).
- **Plaintext**: **`dạng rõ (plaintext)`** or **`văn bản thuần`** (replace `văn bản thường` on Slides 2, 10, 22).
- **Hardcoded**: **`gán cứng`** or **`hardcode`** (replace `viết cứng` on Slides 15, 22).
- **Generated app vs. trial run**: **`bản nháp`** for a generated codebase, **`lần chạy`** for an experimental run (avoid mixing `bản nháp`, `lần chạy`, `ứng viên`, `cây` across Slides 1, 4, 6, 9, 12).
- **Human-written apps**: **`ứng dụng do người viết`** (fix `ứng dụng người viết` on Slides 8, 16, 22).
- **Seeded/injected issues**: **`vấn đề cài sẵn`** / **`lỗi cài sẵn`** (replace `gieo` on Slides 3, 12 and `cảm ứng` on Slide 11).
- **Low-end test device**: **`máy giá rẻ`** (or **`thiết bị cấu hình thấp`**) everywhere (align `máy yếu` and `máy giá rẻ` on Slides 14, 17).
- **Logs**: **`nhật ký ứng dụng`** (or **`log`**) vs. **`nhật ký thí nghiệm`** (distinguish Slide 13 NOTES from app logs on Slides 2, 10, 22).
- **Search**: **`tìm kiếm`** everywhere (align `tìm` on Slides 3, 17 with `tìm kiếm` on Slides 7, 20).
