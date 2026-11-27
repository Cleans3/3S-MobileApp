# slides-outline.md — dàn ý slide (25 slide chính + D1 đến D8 dự phòng)

Khớp nguyên vẹn với `presentation.html` (25 slide chính theo mạch: mở đầu và bảng phân loại lỗi, phương pháp, ba trụ cột tối ưu giao diện / bộ nhớ / bảo mật, demo trực tiếp, build và tự sửa lỗi, kết luận và đối chiếu mục tiêu môn học, thiết bị thật, khuyến nghị theo 5 giai đoạn; cộng 8 slide dự phòng D1 đến D8). Kịch bản lời nói đầy đủ nằm trong `presentation-script.md`.

**Quy tắc.**
1. Mọi con số trên slide đều đến từ nguồn sơ cấp ghi trong cột “Bằng chứng” hoặc ghi chú (`aside.notes`) của `presentation.html`; kịch bản kiểm lại bằng máy qua mục **Kiểm số liệu** ở `presentation-script.md`.
2. Phần khuyến nghị (slide 23 và 24) chia theo năm giai đoạn phát triển, mỗi giai đoạn gồm hai mức **Tối thiểu** và **Nâng cao**, mỗi khuyến nghị gắn với một kết quả đã đo.
3. Không khẳng định nhân quả vượt ngoài dữ liệu thực nghiệm; “được xác nhận” nghĩa là điều kiện thống kê đã đăng ký trước được thỏa sau hiệu chỉnh Holm. Giới hạn máy ảo và mẫu nhỏ được nêu thẳng thắn một lần ở slide 6, slide 21 và chi tiết ở D1.
4. Thứ tự phép kiểm định và hiệu chỉnh Holm nằm ở D2; bằng chứng từng slide ghi ở cột “Bằng chứng” bên dưới.

## Thời lượng

Cột “Kịch bản” là thời lượng tính từ `presentation-script.md` (tốc độ 130 từ mỗi phút, làm tròn 5 giây, cộng 120 giây demo trực tiếp ở slide 16). Cột “Ghi chú slide” là mốc `~giây` trong `aside.notes` của `presentation.html`; từng slide chênh tối đa ±10 giây.

| Phần | Slide | Kịch bản | Ghi chú slide |
|---|---|---|---|
| Mở đầu và bảng phân loại lỗi | 1–5 | 3:25 | 3:40 |
| Phương pháp thực nghiệm | 6 | 0:45 | 0:45 |
| Trụ cột 1: Hiệu năng hiển thị (E2) | 7–9 | 2:15 | 2:15 |
| Trụ cột 2: Bộ nhớ, dung lượng và tìm kiếm (E2) | 10–12 | 2:05 | 2:05 |
| Trụ cột 3: Bảo mật (E1, E3, E4) | 13–15 | 2:20 | 2:15 |
| Demo trực tiếp trước và sau tối ưu | 16 | 2:00 | 2:00 |
| Build và tự sửa lỗi (E1, E5, E6) | 17–18 | 1:30 | 1:30 |
| Kết luận và đối chiếu mục tiêu môn học | 19–21 | 2:15 | 2:15 |
| Thiết bị thật và khuyến nghị theo giai đoạn | 22–25 | 2:55 | 2:45 |
| **Tổng** | **1–25** | **19:30** | **19:30** |

## Danh sách slide chính

| # | Tiêu đề (`data-title`) | Phần | Ý chính | Bằng chứng | Kịch bản (giây) | Ghi chú slide (giây) |
|---|---|---|---|---|---|---|
| 1 | Tối ưu giao diện, bộ nhớ và bảo mật | Mở đầu | Đặt câu hỏi trung tâm: trợ lý lập trình AI viết bản nháp đầu rất nhanh, phần chưa tối ưu nằm ở đâu và tốn bao nhiêu? (6 thí nghiệm, 101 bản nháp, 36 lần chạy). | 00-protocol.md · e1-corpus/REPORT.md:27 · e6-oneshot/REPORT.md:7 | 25 | 25 |
| 2 | Lời hứa trên mạng | Mở đầu | Sáu ví dụ thật về lời hứa “chỉ một prompt ra ngay ứng dụng đẹp” và định nghĩa hiện tượng vibe coding. | evidence/img/hype/hype-01..06.jpg · evidence/img/hype/README.md | 35 | 40 |
| 3 | Chạy đúng prompt đó | Mở đầu | Bốn ứng dụng trông hoàn thiện nhưng chưa tối ưu: 6/12 build thành công, khung hình trễ 74–100%, khởi động 1.195–2.576 ms; RAM 66,8–102,8 MB chỉ vì có 12 tựa sách mẫu. | e6-oneshot/REPORT.md:38–53, :82 · e6-oneshot/img/*-f1.jpg | 40 | 45 |
| 4 | Phân loại lỗi | Mở đầu | Điểm nhấn trung tâm: bảng tần suất (Phổ biến / Ít gặp / Hiếm) tách riêng mức nghiêm trọng (chấm đỏ) trên 66 bản nháp E1, cộng dòng bối cảnh E4. | e1-corpus/REPORT.md:7, :56–68, :93–94 · e6-oneshot/results/findings.csv · e4-pressure/REPORT.md:64 | 55 | 60 |
| 5 | Ba điều rút ra | Mở đầu | Ba bài học từ bảng phân loại: ưu tiên build (58/100 lỗi), bật R8 (63/66 thiếu; 24,7 → 2,4 MB), chủ động rà lỗi hiếm mà nghiêm trọng; chỉ mật độ bảo mật là khác biệt kiểm định giữa AI và người. | e1-corpus/REPORT.md:7, :51–67, :93–94 · e2-bench/REPORT.md:255 · e6-oneshot/REPORT.md:51 | 50 | 50 |
| 6 | Sáu thí nghiệm | Phương pháp | Tóm tắt sáu thí nghiệm E1–E6 với tiêu chí đánh giá chốt trước, kiểm tra môi trường sạch (623 bản ghi) và lưu ý giới hạn máy ảo. | 00-protocol.md · isolation-audit/AUDIT.md:14, :32 · e6-oneshot/REPORT.md:24 · e2-bench/REPORT.md:334 | 45 | 45 |
| 7 | Danh sách dài dựng toàn bộ | Trụ cột 1: Giao diện | Danh sách 2.000 mục dựng bằng Column + forEach làm 90% khung hình bị trễ; đổi sang LazyColumn giảm số mục dựng 2.000 → 31 và khung hình trễ xuống 1,3%. | e2-bench/REPORT.md:31, :176, :186 · evidence/video/P09c-AB.mp4 | 45 | 45 |
| 8 | Hiệu ứng trang trí | Trụ cột 1: Giao diện | Đổ bóng, chuyển sắc, Canvas trên từng thẻ sách làm p90 tăng 28% (62,87 so với 49,07 ms, C5 được xác nhận); khung hình trễ 70,52% → 48,65%; gặp 8/66 ở E1 nhưng 11/12 ở E6. | e2-bench/REPORT.md:134, :142, :145, :153 · e1-corpus/REPORT.md:58 · evidence/video/P08c-AB.mp4 | 45 | 45 |
| 9 | Lỗi không có chi phí đo được | Trụ cột 1: Giao diện | Biểu đồ đối chiếu: danh sách (−69,9%) rất lớn; ba lỗi Compose kinh điển (P01, P03, P04) có khoảng tin cậy cắt qua 0; Baseline Profile −5,88% trên máy ảo nhưng không có khác biệt đo được trên thiết bị thật. | e2-bench/REPORT.md:179, :197, :244, :269 · ../log/opt-ui-ux.md:362 | 45 | 45 |
| 10 | Ảnh giải mã ở kích thước gốc | Trụ cột 2: Bộ nhớ | 15 ảnh bìa 1500×2250 giải mã nguyên gốc tốn 203 MB và đẩy RAM (RSS) lên 180 MB (vượt 150 MB); Coil AsyncImage đưa về 6,2 MB và 60 MB RAM. | e2-bench/REPORT.md:30, :160, :162, :171 · evidence/video/P06c-AB.mp4 | 40 | 40 |
| 11 | Danh sách dài và bộ nhớ | Trụ cột 2: Bộ nhớ | Cùng lỗi P09 nhìn từ phía bộ nhớ: RSS 243 → 43 MB, Java heap 220 → 7,6 MB, thời gian khởi động (TTID) 1.901 → 573,1 ms. | e2-bench/REPORT.md:179, :181–182, :188 | 40 | 40 |
| 12 | Dung lượng và tìm kiếm | Trụ cột 2: Bộ nhớ | Bật R8 thu nhỏ APK 90% (24,7 → 2,4 MB, TTID −8,7%); chỉ mục FTS4 đưa truy vấn chậm nhất trên 100.000 sách từ 96,63 → 37 ms (dưới 50 ms), tổng 634,9 → 85,42 ms, không cải thiện khung hình khi gõ. | e2-bench/REPORT.md:210–214, :219, :221, :255, :258 | 45 | 45 |
| 13 | Lỗi bảo mật phổ biến đến mức nào | Trụ cột 3: Bảo mật | 60/66 bản nháp có cảnh báo bảo mật; mật độ 2,37 so với 1,02 cảnh báo/1.000 dòng ở ứng dụng do người viết trước 2022 (gấp 2,3 lần, C3 xác nhận); trích nguyên văn `SessionStore.kt` lưu token dạng rõ. | e1-corpus/REPORT.md:127–128, :135 · evidence/code-01.kt | 45 | 45 |
| 14 | Điều gì làm giảm lỗi bảo mật | Trụ cột 3: Bảo mật | Prompt chức năng dài hơn không giảm bảo mật (29/33); tệp quy tắc (guardrail) giảm 31/33 → 17/34 (C1) và số loại lỗi 5,85 → 2,82 (C2), nhưng trên 42 bản build được thì C1 không đổi. | e1-corpus/REPORT.md:9, :127–133, :184 · guardrail/ANDROID-GUARDRAIL.md | 50 | 45 |
| 15 | Khi gặp lỗi chặn | Trụ cột 3: Bảo mật | Bị lỗi chặn (E4), trợ lý AI nới lỏng bảo mật 17/148 lượt (C4 không được xác nhận), tập trung ở Keystore 12/30 và cleartext 5/30; công cụ quét APK (E3) độ chính xác 0,291 và không thấy lỗi trong mã. | e4-pressure/REPORT.md:50, :52, :58–60, :64 · evidence/agent-01.txt · e3-security/REPORT.md:28, :74, :109 | 45 | 45 |
| 16 | Demo: trước và sau tối ưu | Demo trực tiếp | Thành viên nhóm chạy demo trực tiếp song song bản nháp AI và bản tối ưu trên máy ảo cho 4 kịch bản P09, P06, P08, P13; người thuyết trình chuyển lượt và nhận lại lượt. | optimization/study/demo/README.md · e2-bench/REPORT.md:142, :162, :186, :211 | 120 | 120 |
| 17 | Bản nháp build lỗi | Build và sửa lỗi | E1 chỉ 42/100 bản build thành công; E6 cùng công cụ đổi mô hình tăng 1/9 → 7/9; bản đặc tả có cấu trúc giảm khung hình trễ 99% → 19,6% và mã 1.164 → 524 dòng. | e1-corpus/REPORT.md:7 · e6-oneshot/REPORT.md:13, :82, :84, :99 | 45 | 45 |
| 18 | “Đã sửa xong” chưa phải kết quả | Build và sửa lỗi | E5: trợ lý AI phát hiện đủ 168/168 lượt lỗi cài sẵn nhưng tự nhận “đã sửa xong” khi 9/12 bản codex sửa không biên dịch được (plugin baselineprofile) và 21/24 lượt đổi cả mẫu đối chứng. | e5-optimizer/REPORT.md:14, :43, :74, :88 · evidence/agent-06.txt | 45 | 45 |
| 19 | Sáu kết luận | Kết luận | Tóm gọn 6 thí nghiệm E1–E6, mỗi dòng một kết luận và một con số đại diện: trông hoàn thiện chưa phải đã tối ưu. | e1–e6 REPORT.md (tổng hợp các slide 3–18) | 45 | 45 |
| 20 | Kết quả các phép kiểm định | Kết luận | Tổng hợp kiểm định: C1, C2, C3, C5 được xác nhận sau hiệu chỉnh Holm; C4 (17/148) không được xác nhận; H4 (0/2.105) và H5 (1/66) bị bác bỏ. | e1-corpus/REPORT.md:11, :127–135, :176 · e2-bench/REPORT.md:153 · e4-pressure/REPORT.md:52 | 40 | 40 |
| 21 | Đối chiếu mục tiêu môn học | Kết luận | Bảng đối chiếu 4 mục tiêu môn học: bản sửa đạt trên màn hình lab (tìm kiếm 37 ms, RAM 60/43 MB, danh sách 1,3%) và mở trang trên Xiaomi (p90 8–16 ms); còn 3 hạng mục chưa đo (RAM toàn ứng dụng, NFC < 1 s, máy cấu hình thấp) và lưới thẻ trên máy ảo chưa đạt. | e2-bench/REPORT.md:145, :162, :182, :186, :211 · e6-oneshot/REPORT.md:38–49 · ../log/opt-ui-ux.md:362, :402 | 50 | 50 |
| 22 | Thiết bị thật | Thiết bị thật | Hai bài học chỉ lộ ra trên thiết bị thật Xiaomi: bản debug 125 ms p90 so với bản build đo hiệu năng 12–16 ms; địa chỉ ảnh bìa máy ảo 10.0.2.2 gây giật trang chủ 7,0–7,2% → 1,5–2,7% khi sửa. | ../log/opt-ui-ux.md:287–289, :362 · e2-bench/REPORT.md:45 | 45 | 45 |
| 23 | Giai đoạn 1 đến 3 | Khuyến nghị | Khuyến nghị mức Tối thiểu và Nâng cao cho Đặt yêu cầu, Sinh mã, Sửa lỗi, mỗi mục gắn với một số đo của E1, E4, E6. | e1-corpus/REPORT.md:127–129 · e4-pressure/REPORT.md:52, :64 · e6-oneshot/REPORT.md:82–84, :95, :99 | 50 | 45 |
| 24 | Giai đoạn 4 và 5 | Khuyến nghị | Khuyến nghị mức Tối thiểu và Nâng cao cho Kiểm tra, Đo và phát hành: tin bản build và số đo, bật R8, đo bản build đo hiệu năng, ưu tiên danh sách và ảnh trước Baseline Profile. | e2-bench/REPORT.md:162, :182, :255, :269 · e3-security/REPORT.md:28, :74, :109 · e5-optimizer/REPORT.md:74 · ../log/opt-ui-ux.md:362 | 45 | 45 |
| 25 | Ba điều cần nhớ | Kết thúc | Chốt 3 điểm cốt lõi: trông hoàn thiện chưa phải đã tối ưu; tần suất không phản ánh mức nghiêm trọng; tin bản build và số đo, không tin lời “đã sửa xong”. | Tổng hợp slide 3–24 | 35 | 30 |

## Slide dự phòng (không trình chiếu trừ khi được hỏi)

Mở bằng phím `O` (lưới) hoặc gõ số slide rồi `Enter`.

| Mã | Slide số | Tiêu đề (`data-title`) | Nội dung | Bằng chứng |
|---|---|---|---|---|
| D1 | 26 | D1 · Giới hạn | Giới hạn máy ảo (đổi GPU: −32,3% thay vì −78,2% p90), mẫu 33–34 mỗi nhóm không xếp hạng trợ lý, các hạng mục chưa đo (NFC < 1 s, máy cấu hình thấp, RAM toàn ứng dụng, bộ nhớ đồ họa, P12). | e1-corpus/REPORT.md:47 · e2-bench/REPORT.md:155, :334, :340 |
| D2 | 27 | D2 · Bảng Holm | Bảng hiệu chỉnh Holm bước xuống trên m = 5 cho C1–C5 (p thô, bậc, ngưỡng bậc, p hiệu chỉnh, độ lớn so với ngưỡng, kết luận). | Tính từ p thô ở e1-corpus/REPORT.md:131–135 · e2-bench/REPORT.md:153 · e4-pressure/REPORT.md:52 |
| D3 | 28 | D3 · Bảng E2 đầy đủ | Đầy đủ 7 nhân tố E2 và đối chiếu phụ P01 (trung vị A, B, thay đổi Δ% và khoảng bootstrap 95%), giải thích cách viết −22% so với +28,13% của P08. | e2-bench/REPORT.md:142, :160, :179, :197, :210, :244, :255, :269 |
| D4 | 29 | D4 · E4 chi tiết | Chi tiết nới lỏng bảo mật theo kịch bản lỗi (Keystore 12/30, cleartext 5/30, 3 ca còn lại 0/88) và theo trợ lý AI (agy 12/48, codex 5/50, claude 0/50), kèm Stack Overflow 4/4 và kappa 0,909. | e4-pressure/REPORT.md:50, :58–60, :64, :68, :72 |
| D5 | 30 | D5 · Tần suất theo quy tắc | Tần suất E1 theo 3 kiểu prompt và nhóm người viết trước 2022 cho các quy tắc phụ trợ (S06, P11, D02, D01, U04, U03, P01, U01). | e1-corpus/REPORT.md:49–51, :61, :68, :73, :75–76 |
| D6 | 31 | D6 · E6 chi tiết | Bảng số lần build thành công theo 4 trợ lý AI × 3 kiểu prompt (mỗi ô 3 lần chạy) và trung vị dòng mã Kotlin; ghi chú lý do agy-g38flash không sinh được ở bản đặc tả. | e6-oneshot/REPORT.md:59–70 · e6-oneshot/AMENDMENT.md |
| D7 | 32 | D7 · Kiểm tra môi trường | Kiểm tra cô lập môi trường (HOME giả lập, khóa ghi, 623 bản ghi hội thoại, mẫu đối chứng 18.244 so với khoảng 7.500 token) và công khai 3 plugin đo đạc nạp vào Claude. | isolation-audit/AUDIT.md:14, :32, :58 · e6-oneshot/REPORT.md:24 |
| D8 | 33 | D8 · Cỡ chữ lớn và bố cục | Chất lượng giao diện ở tỷ lệ cỡ chữ 2.0: 4/4 ứng dụng một prompt vỡ bố cục; ShelfTap giảm 5/7 → 0/7 màn hình vỡ và ATF 4 → 2; chi phí kính mờ trên Xiaomi p90 9–11 → 18 ms. | e6-oneshot/REPORT.md:53 · ../ui-ux-metrics.md:12–16 · ../log/opt-ui-ux.md:237 · e6-oneshot/img/grid-O-font2.jpg · evidence/img/ui-A|B-font2-catalog.jpg |

## Video, hình ảnh và demo

- **Ảnh mở đầu (slide 2):** `evidence/img/hype/hype-01.jpg` đến `hype-06.jpg` (1280×720, không hiển thị nguồn trên slide; nguồn lưu tại `evidence/img/hype/README.md`).
- **Ảnh 4 ứng dụng E6 (slide 3):** `e6-oneshot/img/claude-haiku-O-r1-f1.jpg`, `codex-luna-O-r3-f1.jpg`, `agy-g38flash-O-r1-f1.jpg`, `claude-sonnet-O-r3-f1.jpg`.
- **Ba video A/B chạy offline (14 giây, 720×1280):** `evidence/video/P09c-AB.mp4` (slide 7), `evidence/video/P08c-AB.mp4` (slide 8), `evidence/video/P06c-AB.mp4` (slide 10), mỗi video có ảnh poster `.jpg` tương ứng.
- **Demo trực tiếp (slide 16):** chạy song song bản nháp AI và bản tối ưu trên máy ảo theo `optimization/study/demo/README.md` (120 giây); phương án dự phòng khi lỗi máy ảo là phát lại ba video A/B.
- **Ảnh dự phòng cỡ chữ 2.0 (D8):** `e6-oneshot/img/grid-O-font2.jpg`, `evidence/img/ui-A-font2-catalog.jpg`, `evidence/img/ui-B-font2-catalog.jpg`.
