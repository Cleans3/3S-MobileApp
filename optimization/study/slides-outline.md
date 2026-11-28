# slides-outline.md — dàn ý slide (27 slide chính + D1 đến D12 dự phòng)

Khớp nguyên vẹn với `presentation.html` (27 slide chính theo mạch năm phần: A Mở đầu 1–3, B Kết quả 4–10, C Nguyên nhân 11–20 gồm ba trụ cột tối ưu giao diện / bộ nhớ / bảo mật và demo trực tiếp, D Giải pháp 21–26 theo thang năm bậc kỹ thuật từ đơn giản nhất đến chuyên sâu nhất về kỹ thuật, E Kết thúc 27; cộng 12 slide dự phòng D1 đến D12). Kịch bản lời nói đầy đủ nằm trong `presentation-script.md`.

**Quy tắc.**
1. Mọi con số trên slide đều đến từ nguồn sơ cấp ghi trong cột “Bằng chứng” hoặc ghi chú (`aside.notes`) của `presentation.html`; số liệu trong kịch bản được kiểm lại bằng máy qua mục **Kiểm số liệu** ở `presentation-script.md`.
2. Phần giải pháp (slide 21 đến 26) tổ chức theo thang năm bậc kỹ thuật (**Bậc 1** đến **Bậc 5**, từ đơn giản nhất đến chuyên sâu nhất về kỹ thuật): mỗi bậc nêu rõ làm gì, kết quả đo được trong dữ liệu của nhóm, và dừng ở đâu để nối sang bậc kế tiếp.
3. Không khẳng định nhân quả vượt ngoài dữ liệu thực nghiệm; “được xác nhận” nghĩa là điều kiện thống kê đã đăng ký trước được thỏa mãn sau hiệu chỉnh Holm. Giới hạn máy ảo và mẫu nhỏ được nêu thẳng thắn ở slide 4, slide 8 (dẫn sang D1) và chi tiết ở D1; bảng đối chiếu mục tiêu môn học nằm ở slide dự phòng D10.
4. Thứ tự phép kiểm định và hiệu chỉnh Holm nằm ở D2; bằng chứng từng slide ghi ở cột “Bằng chứng” bên dưới.

## Thời lượng

Thời lượng từng slide trong `presentation-script.md` bám đúng mốc `~N giây` trong `aside.notes` của `presentation.html` (tổng 19:30, gồm 120 giây demo trực tiếp ở slide 20).

| Phần | Slide | Thời lượng |
|---|---|---|
| A · Mở đầu | 1–3 | 1:40 |
| B · Kết quả | 4–10 | 4:55 |
| C · Nguyên nhân (giao diện, bộ nhớ, bảo mật, demo) | 11–20 | 8:35 |
| D · Giải pháp (năm bậc kỹ thuật) | 21–26 | 3:50 |
| E · Kết thúc | 27 | 0:30 |
| **Tổng** | **1–27** | **19:30** |

## Danh sách slide chính

### A · Mở đầu (slide 1–3)

| # | Tiêu đề (`data-title`) | Mục (`data-sec`) | Ý chính | Bằng chứng | Thời lượng (giây) |
|---|---|---|---|---|---|
| 1 | Trông hoàn thiện, chưa tối ưu | Mở đầu | Slide áp phích mở đầu (không số trang): bốn màn hình ứng dụng E6 trông hoàn thiện nhưng chưa tối ưu; giới thiệu bối cảnh giao diện, bộ nhớ, bảo mật trên Android với ShelfTap. | e6-oneshot/img/*-f1.jpg · e6-oneshot/REPORT.md:30 | 15 |
| 2 | Lời hứa trên mạng | Mở đầu | Sáu ví dụ thật về lời hứa “chỉ một prompt ra ngay ứng dụng đẹp” và định nghĩa hiện tượng vibe coding. | evidence/img/hype/hype-01..06.jpg · evidence/img/hype/README.md | 40 |
| 3 | Năm con số | Mở đầu | Câu trả lời ngắn qua năm con số đã đo, mỗi con số trỏ tới slide chứng minh phía sau: 58/100 build lỗi (slide 5), 60/66 cảnh báo bảo mật gấp 2,3 lần (slide 17), 90% khung hình trễ ở danh sách 2.000 mục (slide 11), RAM 180 và 243 MB vượt 150 MB (slide 14, 15), 9/12 bản codex sửa không biên dịch được (slide 7). | e1-corpus/REPORT.md:7, :127–128, :135 · e2-bench/REPORT.md:162, :176, :182, :186 · e5-optimizer/REPORT.md:74 · CLAUDE.md:17 | 45 |

### B · Kết quả (slide 4–10)

| # | Tiêu đề (`data-title`) | Mục (`data-sec`) | Ý chính | Bằng chứng | Thời lượng (giây) |
|---|---|---|---|---|---|
| 4 | Sáu thí nghiệm | Phương pháp | Tóm tắt sáu thí nghiệm E1–E6 với tiêu chí đánh giá chốt trước khi thu thập dữ liệu, kiểm tra môi trường sạch (623 bản ghi hội thoại) và lưu ý giới hạn máy ảo. | 00-protocol.md · isolation-audit/AUDIT.md:14, :32 · e1-corpus/REPORT.md:27 · e3-security/REPORT.md:28 · e4-pressure/REPORT.md:52 · e5-optimizer/REPORT.md:14 · e6-oneshot/REPORT.md:7, :24 · e2-bench/REPORT.md:334 | 45 |
| 5 | Phân loại lỗi | Phân loại | Kết quả trung tâm của E1: bảng tần suất (Phổ biến / Ít gặp / Hiếm) tách riêng mức nghiêm trọng (chấm đỏ) trên 66 bản nháp không kèm tệp quy tắc, cộng dòng bối cảnh E4. | e1-corpus/REPORT.md:7, :56–68, :93–94 · e6-oneshot/results/findings.csv · e4-pressure/REPORT.md:64 | 50 |
| 6 | Bản nháp build lỗi | Build và sửa lỗi | E1 chỉ 42/100 bản build thành công; E6 dùng cùng công cụ và đổi mô hình tăng tỷ lệ build thành công 1/9 → 7/9; bản đặc tả có cấu trúc giảm tỷ lệ khung hình trễ 99% → 19,6% và mã Kotlin 1.164 → 524 dòng. | e1-corpus/REPORT.md:7 · e6-oneshot/REPORT.md:13, :82, :84, :99 | 45 |
| 7 | “Đã sửa xong” chưa phải kết quả | Build và sửa lỗi | E5: trợ lý AI phát hiện đủ 168/168 lượt lỗi cài sẵn ở chế độ chỉ đề xuất, nhưng khi tự sửa tuyên bố “đã sửa xong” trong khi 9/12 bản do codex sửa không biên dịch được (plugin baselineprofile) và 21/24 lượt đổi cả mẫu đối chứng. | e5-optimizer/REPORT.md:14, :43, :74, :88 · evidence/agent-06.txt | 45 |
| 8 | Thiết bị thật | Thiết bị thật | Hai bài học chỉ lộ ra trên thiết bị thật Xiaomi: bản debug 125 ms p90 so với bản build đo hiệu năng 12–16 ms; địa chỉ ảnh bìa máy ảo 10.0.2.2 làm tỷ lệ khung hình trễ trang chủ lên 7,0–7,2% → giảm còn 1,5–2,7% khi sửa (giới hạn thiết bị cấu hình thấp ở D1). | ../log/opt-ui-ux.md:287–289, :362 · e2-bench/REPORT.md:45 | 45 |
| 9 | Kết quả các phép kiểm định | Kiểm định | Tổng hợp kiểm định: C1, C2, C3, C5 được xác nhận sau hiệu chỉnh Holm trên m = 5; C4 (17/148) không được xác nhận; H4 (0/2.105) và H5 (1/66) bị bác bỏ. | e1-corpus/REPORT.md:11, :127–135, :176 · e2-bench/REPORT.md:153 · e4-pressure/REPORT.md:52 | 30 |
| 10 | Sáu kết luận | Tổng hợp | Tóm gọn 6 thí nghiệm E1–E6, mỗi dòng một kết luận và một con số đại diện (E1 nêu rõ tệp quy tắc giảm cảnh báo bảo mật nhưng cũng giảm số bản build thành công): trông hoàn thiện chưa phải đã tối ưu. | e1–e6 REPORT.md (tổng hợp, chi tiết ở slide 11–19) | 35 |

### C · Nguyên nhân (slide 11–20)

| # | Tiêu đề (`data-title`) | Mục (`data-sec`) | Ý chính | Bằng chứng | Thời lượng (giây) |
|---|---|---|---|---|---|
| 11 | Danh sách dài dựng toàn bộ | Giao diện | Danh sách 2.000 mục dựng bằng Column + forEach làm 90% khung hình bị trễ; đổi sang LazyColumn giảm số mục dựng 2.000 → 31 và tỷ lệ khung hình trễ xuống 1,3%. | e2-bench/REPORT.md:31, :176, :186 · evidence/video/P09c-AB.mp4 | 45 |
| 12 | Hiệu ứng trang trí | Giao diện | Đổ bóng, chuyển sắc, Canvas trên từng thẻ sách làm p90 tăng 28% (62,87 so với 49,07 ms, C5 được xác nhận); tỷ lệ khung hình trễ 70,52% → 48,65%; gặp 8/66 ở E1 nhưng 11/12 lần chạy ở E6. | e2-bench/REPORT.md:134, :142, :145, :153 · e1-corpus/REPORT.md:58 · evidence/video/P08c-AB.mp4 | 45 |
| 13 | Lỗi không có chi phí đo được | Giao diện | Biểu đồ đối chiếu: danh sách (−69,9%) rất lớn; ba lỗi Compose kinh điển (P01, P03, P04) có khoảng tin cậy cắt qua 0; Baseline Profile −5,88% trên máy ảo nhưng không có khác biệt đo được trên thiết bị thật. | e2-bench/REPORT.md:179, :197, :244, :269 · ../log/opt-ui-ux.md:362 | 45 |
| 14 | Ảnh giải mã ở kích thước gốc | Bộ nhớ | 15 ảnh bìa 1500×2250 giải mã ở kích thước gốc tốn 203 MB và đẩy RAM (RSS) lên 180 MB (vượt 150 MB); Coil AsyncImage đưa về 6,2 MB và 60 MB RAM. | e2-bench/REPORT.md:30, :160, :162, :171 · evidence/video/P06c-AB.mp4 | 40 |
| 15 | Danh sách dài và bộ nhớ | Bộ nhớ | Cùng lỗi P09 ở slide 11 nhìn từ phía bộ nhớ: RSS 243 → 43 MB, Java heap 220 → 7,6 MB, thời gian khởi động (TTID) 1.901 → 573,1 ms. | e2-bench/REPORT.md:179, :181–182, :188 | 40 |
| 16 | Dung lượng và tìm kiếm | Bộ nhớ | Bật R8 thu nhỏ APK 90% (24,7 → 2,4 MB, TTID −8,7%); chỉ mục FTS4 đưa truy vấn chậm nhất trên 100.000 tựa sách từ 96,63 → 37 ms (dưới 50 ms), tổng 634,9 → 85,42 ms, nhưng không cải thiện độ trễ khung hình khi gõ. | e2-bench/REPORT.md:210–214, :219, :221, :255, :258 | 45 |
| 17 | Lỗi bảo mật phổ biến đến mức nào | Bảo mật | 60/66 bản nháp có cảnh báo bảo mật; mật độ 2,37 so với 1,02 cảnh báo/1.000 dòng ở ứng dụng do người viết trước 2022 (gấp 2,3 lần, C3 được xác nhận); trích nguyên văn `SessionStore.kt` lưu token dạng rõ. | e1-corpus/REPORT.md:127–128, :135 · evidence/code-01.kt | 45 |
| 18 | Điều gì làm giảm lỗi bảo mật | Bảo mật | Prompt chức năng chi tiết không làm giảm cảnh báo bảo mật (29/33); tệp quy tắc (guardrail) kéo tỷ lệ đó từ 31/33 → 17/34 (C1) và số loại vấn đề 5,85 → 2,82 (C2), nhưng số bản build thành công sau tối đa 3 lần sửa cấu hình cũng giảm từ 20/33 → 10/34 (prompt chi tiết 12/33, số mô tả); riêng trong 42 bản build thành công thì C1 không đổi. | e1-corpus/REPORT.md:9, :17–25, :127–133, :184 · guardrail/ANDROID-GUARDRAIL.md | 45 |
| 19 | Khi gặp lỗi chặn | Bảo mật | Bị lỗi chặn (E4), trợ lý AI nới lỏng cơ chế bảo mật 17/148 lượt (C4 không được xác nhận), tập trung ở Keystore 12/30 và cleartext 5/30; công cụ quét APK (E3) độ chính xác 0,291 và không thấy lỗi trong mã. | e4-pressure/REPORT.md:50, :52, :58–60, :64 · evidence/agent-01.txt · e3-security/REPORT.md:28, :74, :109 | 45 |
| 20 | Demo: trước và sau tối ưu | Demo | Thành viên nhóm chạy demo trực tiếp song song bản nháp AI và bản tối ưu trên máy ảo cho 4 kịch bản P09, P06, P08, P13; người thuyết trình chuyển lượt và nhận lại lượt. | optimization/study/demo/README.md · e2-bench/REPORT.md:142, :162, :186, :211 | 120 |

### D · Giải pháp (slide 21–26)

| # | Tiêu đề (`data-title`) | Mục (`data-sec`) | Ý chính | Bằng chứng | Thời lượng (giây) |
|---|---|---|---|---|---|
| 21 | Năm bậc giải pháp | Giải pháp | Tổng quan thang năm bậc kỹ thuật từ đơn giản nhất đến chuyên sâu nhất về kỹ thuật: (1) một prompt — trợ lý AI tự rà soát, (2) viết tài liệu — quy tắc và bản đặc tả, (3) công cụ build — để công cụ chấm, (4) bộ đo hiệu năng — đo trên bản build đo hiệu năng, (5) sửa mã nguồn — sửa đúng mẫu lỗi; bậc trước dừng ở đâu, bậc sau tiếp tục từ đó. | e5-optimizer/REPORT.md:43 · e6-oneshot/REPORT.md:82, :84 · e2-bench/REPORT.md:182, :255 · ../log/opt-ui-ux.md:362 | 30 |
| 22 | Bậc 1 · Trợ lý AI tự rà soát | Giải pháp | Làm gì: prompt mẫu yêu cầu rà soát theo tệp và dòng, chưa sửa mã (chế độ chỉ đề xuất E5). Đạt: tìm đủ 168/168 lượt lỗi cài sẵn; kèm kết quả bộ quét tĩnh không tạo khác biệt đo được. Dừng ở đâu: tự sửa thì 9/12 bản codex không biên dịch được, 21/24 lượt đổi mẫu đối chứng, và prompt đã nêu sẵn danh mục lỗi. | e5-optimizer/REPORT.md:14, :43, :53, :74, :88–89, :114 | 40 |
| 23 | Bậc 2 · Quy tắc và bản đặc tả | Giải pháp | Làm gì: đặt tệp quy tắc ở gốc dự án và viết bản đặc tả có cấu trúc (trích nguyên văn quy tắc và định nghĩa “xong”). Đạt: cảnh báo bảo mật 31/33 → 17/34 (E1), khung hình trễ 99% → 19,6% (E6). Dừng ở đâu: build thành công giảm 20/33 → 10/34, trong 42 bản build thành công tỷ lệ cảnh báo không đổi, quy tắc Baseline Profile làm cả 3 bản codex build lỗi. | e1-corpus/REPORT.md:9, :17–25, :127–129, :184 · e6-oneshot/REPORT.md:82, :84, :95 · guardrail/ANDROID-GUARDRAIL.md:24 · e6-oneshot/prompts/S.txt | 40 |
| 24 | Bậc 3 · Để công cụ chấm | Giải pháp | Làm gì: bật R8 (`isMinifyEnabled`, `isShrinkResources`) và chạy biên dịch thật `./gradlew compileDebugKotlin` sau mỗi lần sửa. Đạt: APK 24,7 → 2,4 MB; biên dịch thật phát hiện 9/12 bản do codex sửa không biên dịch được. Dừng ở đâu: MobSF độ chính xác 0,291 và cả 30 cảnh báo đúng đều là cấu hình; build thành công chưa cho biết ứng dụng nhanh hay tốn RAM. | e2-bench/REPORT.md:255, :258 · guardrail/ANDROID-GUARDRAIL.md:31 · e5-optimizer/REPORT.md:74, :88 · e3-security/REPORT.md:7, :28, :74, :109 | 40 |
| 25 | Bậc 4 · Đo trước và sau | Giải pháp | Làm gì: Macrobenchmark khởi động nguội, A và B chạy xen kẽ 12 lần lặp mỗi bản trên bản build đo hiệu năng. Đạt: Xiaomi debug 125 ms so với bản build đo hiệu năng 12–16 ms p90; loại P01, P03, P04 vì không có chi phí đo được. Dừng ở đâu: số đo chỉ cho biết nghẽn ở đâu và tổn hao bao nhiêu, chứ không tự sửa lỗi; Baseline Profile −5,88% trên máy ảo nhưng không tạo khác biệt đo được trên thiết bị thật. | e2-bench/REPORT.md:49, :51, :197, :244, :269 · ../log/opt-ui-ux.md:362 | 40 |
| 26 | Bậc 5 · Sửa đúng mẫu lỗi | Giải pháp | Làm gì và đạt (máy ảo A/B): LazyColumn (90% → 1,3% khung hình trễ; RAM 243 → 43 MB), Coil AsyncImage (RAM 180 → 60 MB), bỏ đổ bóng và chuyển sắc (62,87 → 49,07 ms), FTS4 (96,63 → 37 ms), Baseline Profile (−5,88% khởi động). Dừng ở đâu: mỗi cách chỉ xử lý đúng mẫu lỗi đó, lưới thẻ vẫn 48,65% chưa đạt 60 fps; gợi ý thăm dò D9. | e2-bench/REPORT.md:142, :145, :162, :182, :186, :211, :269 · e7-novel/NOTE.md:171 | 40 |

### E · Kết thúc (slide 27)

| # | Tiêu đề (`data-title`) | Mục (`data-sec`) | Ý chính | Bằng chứng | Thời lượng (giây) |
|---|---|---|---|---|---|
| 27 | Ba điều cần nhớ | Kết thúc | Chốt 3 điểm cốt lõi: trông hoàn thiện chưa phải đã tối ưu; tần suất xuất hiện không phản ánh mức độ nghiêm trọng; tin bản build và số đo, không tin lời “đã sửa xong”, đi đủ năm bậc giải pháp. | Tổng hợp slide 3–26 | 30 |

## Slide dự phòng (không trình chiếu trừ khi được hỏi)

Mở bằng phím `O` (lưới) hoặc gõ số slide rồi `Enter`.

| Mã | Slide số | Tiêu đề (`data-title`) | Nội dung | Bằng chứng |
|---|---|---|---|---|
| D1 | 28 | D1 · Giới hạn | Giới hạn máy ảo (đổi GPU: −32,3% thay vì −78,2% p90), mẫu 33–34 mỗi nhóm nên không xếp hạng trợ lý AI, các hạng mục chưa đo (chạm thẻ NFC tự mượn sách dưới 1 s, thiết bị cấu hình thấp, mức dùng RAM khi duyệt có ảnh bìa thật, bộ nhớ đồ họa, P12). | e1-corpus/REPORT.md:47 · e2-bench/REPORT.md:155, :334, :340 |
| D2 | 29 | D2 · Bảng Holm | Bảng hiệu chỉnh Holm bước xuống trên m = 5 cho C1–C5 (p thô, bậc, ngưỡng bậc, p hiệu chỉnh, độ lớn so với ngưỡng, kết luận). | Tính từ p thô ở e1-corpus/REPORT.md:131–135 · e2-bench/REPORT.md:153 · e4-pressure/REPORT.md:52 |
| D3 | 30 | D3 · Bảng E2 đầy đủ | Đầy đủ 7 nhân tố E2 và đối chiếu phụ P01 (trung vị A, B, thay đổi Δ% và khoảng bootstrap 95%), giải thích cách viết −22% so với +28,13% của P08. | e2-bench/REPORT.md:142, :160, :179, :197, :210, :244, :255, :269 |
| D4 | 31 | D4 · E4 chi tiết | Chi tiết nới lỏng cơ chế bảo mật theo kịch bản lỗi (Keystore 12/30, cleartext 5/30, 3 kịch bản còn lại 0/88) và theo trợ lý AI (agy 12/48, codex 5/50, claude 0/50), kèm Stack Overflow 4/4 và kappa 0,909. | e4-pressure/REPORT.md:50, :58–60, :64, :68, :72 |
| D5 | 32 | D5 · Tần suất theo quy tắc | Tần suất E1 theo 3 kiểu prompt và nhóm ứng dụng do người viết trước 2022 cho các quy tắc ngoài ba trụ cột chính (S06, P11, D02, D01, U04, U03, P01, U01). | e1-corpus/REPORT.md:49–51, :61, :68, :73, :75–76 |
| D6 | 33 | D6 · E6 chi tiết | Bảng số lần build thành công theo 4 trợ lý AI × 3 kiểu prompt (mỗi ô 3 lần chạy) và trung vị dòng mã Kotlin; ghi chú lý do agy-g38flash không sinh được bản nháp khi dùng bản đặc tả. | e6-oneshot/REPORT.md:59–70 · e6-oneshot/AMENDMENT.md |
| D7 | 34 | D7 · Kiểm tra môi trường | Kiểm tra cô lập môi trường (HOME giả lập, khóa ghi, 623 bản ghi hội thoại, mẫu đối chứng 18.244 so với khoảng 7.500 token) và công khai 3 plugin đo đạc tự nạp vào Claude. | isolation-audit/AUDIT.md:14, :32, :58 · e6-oneshot/REPORT.md:24 |
| D8 | 35 | D8 · Cỡ chữ lớn và bố cục | Chất lượng giao diện ở tỷ lệ cỡ chữ 2.0: 4/4 ứng dụng sinh từ chỉ một prompt ở D11 đều vỡ bố cục; ShelfTap giảm 5/7 → 0/7 màn hình vỡ và ATF 4 → 2; chi phí hiệu ứng kính mờ trên Xiaomi làm p90 tăng 9–11 → 18 ms. | e6-oneshot/REPORT.md:53 · ../ui-ux-metrics.md:12–16 · ../log/opt-ui-ux.md:237 · e6-oneshot/img/grid-O-font2.jpg · evidence/img/ui-A\|B-font2-catalog.jpg |
| D9 | 36 | D9 · Phép thử quy mô (E7, thăm dò) | Kết quả thăm dò E7a: phóng dữ liệu mẫu của 5 ứng dụng chỉ một prompt từ 8–12 lên 2.000 và 100.000 mục. Ứng dụng duy nhất dựng danh sách toàn bộ vượt 150 MB từ 2.000 mục (107,0 → 343,2 MB) và treo ở 100.000 mục; ở 100.000 mục không ứng dụng nào khởi động dưới 1.500 ms (ví dụ 1.168 → 2.514 ms); cả 10 bản phóng to build thành công. Máy ảo, n = 5 mỗi ô, ngoài các phép kiểm định đăng ký trước. | e7-novel/NOTE.md:154, :195, :201–217, :222–229, :234–238 · e7-novel/results/e7a_summary.csv · e7-novel/results/e7a_builds.csv |
| D10 | 37 | D10 · Đối chiếu mục tiêu môn học | Bảng đối chiếu các mục tiêu môn học đã đo: bản sửa đạt trên màn hình thử nghiệm (tìm kiếm 37 ms, RAM 60/43 MB, danh sách 1,3%), trên Xiaomi mở trang sách đạt p90 8–16 ms và RAM toàn ứng dụng khi cuộn danh mục chưa có ảnh bìa đạt 94–95 MB (PSS); chỉ lưới thẻ trang trí trên máy ảo chưa đạt (48,65%). Các hạng mục chưa đo liệt kê ở D1. | e2-bench/REPORT.md:145, :162, :182, :186, :211 · e6-oneshot/REPORT.md:38–49 · ../log/opt-ui-ux.md:362, :402, :650 · CLAUDE.md:17 |
| D11 | 38 | D11 · Bốn ứng dụng chỉ một prompt | Bốn ứng dụng trông hoàn thiện ở nền slide 1 nhưng chưa tối ưu: 6/12 lần chạy build thành công, tỷ lệ khung hình trễ 74–100% (89% / 74% / 100% / 99%), thời gian khởi động 1.195–2.576 ms; RAM 66,8–102,8 MB chỉ vì có 12 tựa sách mẫu (quy mô thật xem slide 14, 15 và D9). | e6-oneshot/REPORT.md:38–53, :82 · e6-oneshot/img/*-f1.jpg |
| D12 | 39 | D12 · Ba bài học từ bảng phân loại | Ba bài học từ bảng phân loại (slide 5): ưu tiên build thành công (58/100 ở E1, 6/12 ở E6), bật R8 (63/66 thiếu; 24,7 → 2,4 MB), chủ động rà soát lỗi ít gặp hoặc hiếm mà nghiêm trọng; chỉ mật độ cảnh báo bảo mật mới là khác biệt được kiểm định giữa bản nháp AI và ứng dụng do người viết (C3, slide 17). | e1-corpus/REPORT.md:7, :51–67, :93–94 · e2-bench/REPORT.md:255 · e6-oneshot/REPORT.md:51 |

## Video, hình ảnh và demo

- **Ảnh 4 ứng dụng E6 (nền slide 1 và D11):** `e6-oneshot/img/claude-haiku-O-r1-f1.jpg`, `codex-luna-O-r3-f1.jpg`, `agy-g38flash-O-r1-f1.jpg`, `claude-sonnet-O-r3-f1.jpg`.
- **Ảnh mở đầu (slide 2):** `evidence/img/hype/hype-01.jpg` đến `hype-06.jpg` (1280×720, không hiển thị nguồn trên slide; nguồn lưu tại `evidence/img/hype/README.md`).
- **Ba video A/B chạy offline (14 giây, 720×1280):** `evidence/video/P09c-AB.mp4` (slide 11), `evidence/video/P08c-AB.mp4` (slide 12), `evidence/video/P06c-AB.mp4` (slide 14), mỗi video có ảnh poster `.jpg` tương ứng.
- **Demo trực tiếp (slide 20):** chạy song song bản nháp AI và bản tối ưu trên máy ảo theo `optimization/study/demo/README.md` (120 giây); phương án dự phòng khi lỗi máy ảo là phát lại ba video A/B ở slide 11, 12, 14.
- **Ảnh dự phòng cỡ chữ 2.0 (D8):** `e6-oneshot/img/grid-O-font2.jpg`, `evidence/img/ui-A-font2-catalog.jpg`, `evidence/img/ui-B-font2-catalog.jpg`.
