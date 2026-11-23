# slides-outline.md — dàn ý slide (số liệu cuối, khớp ARTICLE.md)

Bài trình bày 10 phút theo kịch bản của đề cương (mục VII): 1 phút vấn đề, 2 phút demo UI phiên bản A, 2 phút bảo mật, 2 phút AI phân tích và tối ưu, 2 phút demo phiên bản B, 1 phút kết luận. Nguồn nội dung là `ARTICLE.md` (Chương 5 và 6); kịch bản lời nói ở `presentation-script.md`; hình ở `figures/out/` (SVG, số liệu vẽ trong `.data.csv` cạnh từng hình; kế hoạch và các sai khác ở `figures/PLAN.md` mục 6).

**Quy tắc.** (1) Mọi con số trên slide có trong ARTICLE.md và chép từ một REPORT hoặc CSV đã commit; không tính lại, trừ hiệu chỉnh Holm (Bảng 5.27). Người điều phối đối chiếu trước buổi trình bày. (2) Chỗ còn trống chỉ là **tài nguyên** (video, ảnh chụp), ghi `[TÀI NGUYÊN: ...]`, không phải số liệu. (3) Quay trước video thao tác và chụp sẵn Profiler, MobSF; video A/B do chủ E2 quay, và cần quay lại cho biến thể A lấy từ mã E1 (`e2-bench/REPORT.md` mục 10). (4) Chữ trên slide ngắn, một ý một slide. (5) Nhân quả luôn viết là "giả thuyết"; không viết "AI gây ra". (6) Slide kết quả ghi rõ xác nhận hay mô tả, và nêu một giới hạn chính.

## Phân bổ thời gian (tổng 10 phút)

| Đoạn | Thời lượng | Slide | Nội dung |
|---|---|---|---|
| 1. Vấn đề | 1 phút | S1, S2 | AI dựng ứng dụng nhanh, nhưng bản nháp đầu tốn gì và ai kiểm chứng |
| 2. Demo UI, phiên bản A | 2 phút | S3, S4 | cuộn danh sách ShelfTap bản A, số đo |
| 3. Bảo mật | 2 phút | S5, S6 | tần suất vấn đề bảo mật trong bản nháp của AI so với người; công cụ độc lập |
| 4. AI phân tích và tối ưu | 2 phút | S7, S8, S9 | guardrail; AI sửa lỗi dưới áp lực; AI tối ưu |
| 5. Demo phiên bản B | 2 phút | S10 | cùng thao tác, bản đã tối ưu, số đo trước và sau |
| 6. Kết luận | 1 phút | S11 | kết quả, hạn chế, vai trò người kiểm duyệt |

Slide dự phòng (không trình chiếu trừ khi được hỏi): D1 giao thức đăng ký trước, Holm và công suất; D2 catalogue vấn đề; D3 mối đe dọa; D4 so sánh với nghiên cứu liên quan; D5 câu hỏi mở của E1 (H4, S11).

## Danh sách slide

### S1. Tiêu đề (30 giây)
- Tên đề tài, nhóm, ứng dụng minh họa (ShelfTap).
- Hình: ảnh màn hình ShelfTap. [TÀI NGUYÊN: ảnh chụp ShelfTap và optlab do chủ E2 chụp, chưa có trong repo.]

### S2. Vấn đề (30 giây)
- Thông điệp: trợ lý AI dựng được cả ứng dụng Android từ một mô tả ngắn; bản nháp đầu chưa ai thẩm định.
- Câu hỏi: bản nháp đó làm ứng dụng tốn thêm gì về giao diện, bộ nhớ, bảo mật; phần nào các kỹ thuật tối ưu quen thuộc không xử lý được.
- Hình: sơ đồ một dòng "mô tả ngắn → bản nháp đầu → ai kiểm chứng?". Nguồn: ARTICLE.md §1.1, §1.3.

### S3. Thiết lập (45 giây, mở đầu đoạn 2)
- Thông điệp: hai phiên bản cùng chức năng, cùng dữ liệu; chỉ khác mẫu mã. A là mã mà AI thật sự sinh ra (một ứng viên của E1 cho mỗi nhân tố, chỉ sửa cơ học); B là bản đã tối ưu.
- Nội dung: bảy nhân tố (hiệu ứng trang trí P08, ảnh P06, danh sách P01 cùng P09, tìm kiếm P13, ổn định kiểu P03 cùng P04, R8 P10, Baseline Profile P11); P10 và P11 là cùng một mã với hai cấu hình.
- Hình: Hình 4.1 (F01) hoặc Bảng 5.11. Nguồn: ARTICLE.md §5.3.3.
- Lưu ý bắt buộc trên slide: "chỉ Emulator: số đo tương đối; thẻ lưới P08 đo trên một đường GPU khác (A14)".

### S4. Demo UI, phiên bản A (75 giây)
- Thông điệp: cuộn danh sách bản A; những gì công cụ đo cho thấy.
- Video: cuộn danh sách bản A. [TÀI NGUYÊN: video A cho mã E1, chủ E2.]
- Bảng nhỏ (Bảng 5.13 và 5.15, trung vị các lần lặp được giữ): thẻ lưới P08, CPU khung hình p90 là 62,87 ms và 70,52% khung chậm (đường GPU swangle_indirect); danh sách P01+P09 dựng 2.000 khoản vay, TTID 1.901 ms và 90,05% khung chậm.

### S5. Bản nháp của AI chứa gì (60 giây)
- Thông điệp: 101 bản nháp, 100 hợp lệ; ít nhất một phát hiện nhóm S ở 31/33 lần chạy naive (0,939) và 29/33 detailed-functional (0,879); mật độ nhóm S trên 1000 dòng, trung vị 2,37 (AI không guardrail) so với 1,02 (người viết trước 30/11/2022); delta Cliff +0,582, ngưỡng 0,33 (C3, được ủng hộ, p hiệu chỉnh 0,00027).
- Hình: F06 (tần suất theo luật và kiểu prompt) và F10 (C3).
- Câu bắt buộc: "tỷ lệ của trợ lý so với tỷ lệ nền của người", không nói "AI gây ra". Giới hạn: chỉ 42 trên 100 bản nháp build được; ứng dụng người viết phổ biến và lớn hơn.

### S6. Công cụ độc lập kiểm tra (60 giây)
- Thông điệp: không để AI tự kết luận mã đã an toàn. MobSF 103 phát hiện trên 13 APK: 30 đúng, 73 sai, độ chính xác 0,291 [0,212; 0,385]; cả 30 là sự kiện manifest, build hoặc chữ ký, không có điểm yếu ở mức mã; cả 22 cảnh báo exported đều sai; tùy cách đọc nhãn, độ chính xác từ 0,244 đến 0,806.
- Bộ phát hiện E1 thấy cái MobSF không thấy (token ghi văn bản thường, ghi nhật ký BODY); hai công cụ bổ sung nhau (kappa 0,581 so với mọi cảnh báo, 0,78 so với tp đã phân loại).
- Hình: F17. [TÀI NGUYÊN: ảnh chụp MobSF và một đoạn mã trước sau, chủ E3.]

### S7. Guardrail (40 giây)
- Thông điệp: C1, tỷ lệ có phát hiện S giảm từ 31/33 xuống 17/34 (+0,439, p hiệu chỉnh 0,00016); C2, số mã luật khác nhau mỗi lần từ 5,85 xuống 2,82 (+3,025, p hiệu chỉnh 0,00005); cả hai được ủng hộ.
- G3: luật bị giữ lại cũng giảm (chênh +0,63 [0,15; 1,14] và +1,30 [0,75; 1,84]): "khái quát hóa hoặc ngẫu nhiên".
- Giới hạn trên slide: ở các ứng dụng build được C1 là +0,000 (C2 vẫn +3,829); n = 33, 33, 34 dưới mục tiêu 36, bị giới hạn công suất.
- Hình: F07, F08, F09.

### S8. AI sửa lỗi dưới áp lực (30 giây)
- Thông điệp: C4, 17/148 = 0,115 [0,073; 0,176] làm yếu bảo mật, dưới ngưỡng 0,15: **không được ủng hộ**.
- Tập trung: bỏ yêu cầu xác thực khỏi khóa Keystore 12/30, bật cleartext 5/30, ba lỗi còn lại 0; theo bộ công cụ agy 12/48, codex 5/50, claude 0/50.
- Hình: F18 (và F19 nếu hỏi).

### S9. AI tối ưu ứng dụng có sẵn (50 giây)
- Thông điệp: advice tìm đủ 14 vấn đề gieo (168/168); autonomous codex sửa 84/84, claude 76/84; nhưng 9/12 cây codex đã sửa không biên dịch được và mồi bị đổi ở 21/24 lần chạy sửa; cho xem đầu ra của bộ phát hiện không tạo khác biệt đo được. Mô tả, không có kiểm định xác nhận.
- Hình: F20. [TÀI NGUYÊN: một prompt và một thay đổi quan trọng (diff), chủ E5.]

### S10. Demo phiên bản B (2 phút)
- Thông điệp: lặp lại đúng thao tác cũ trên bản B; so sánh trước và sau.
- Video: bản B cạnh bản A. [TÀI NGUYÊN: video A/B cho mã E1, chủ E2.]
- C5 (xác nhận): P08 p90 62,87 so với 49,07 ms, delta% +28,13% [13,38; 54,31], p hiệu chỉnh 0,000616: ủng hộ, trên một đường GPU của emulator, bộ nhớ đồ họa không đo (F14).
- Mô tả (F13, F15): P06 byte giải mã −96,9%; P01+P09 TTID 1.901 xuống 573,1 ms, khung chậm 90,05% xuống 1,255%; P13 tổng thời gian truy vấn 634,9 xuống 85,42 ms nhưng khung hình không cải thiện; P03+P04 và nhân tố con P01: không có chi phí đo được; R8: APK −90,4%; Baseline Profile: TTID −5,88%.
- Nhắc lại: Emulator, tương đối; khoảng 30% của Baseline Profile là con số của Google [Baseline-Overview], số đo của chúng ta là số đo khác.

### S11. Kết luận (60 giây)
- Ba ý: (1) C1, C2, C3, C5 được ủng hộ sau Holm, C4 không được ủng hộ (F21, Bảng 5.27); H5 bác bỏ cho U01 (1/66), H4 chưa kết luận được; (2) hạn chế: Emulator, một miền, 42/100 build được, n dưới 36; (3) vai trò người kiểm duyệt: guardrail không thay cho kiểm tra, cổng biên dịch và phân loại thủ công, xem kỹ diff chạm cấu hình mạng và khóa Keystore (ARTICLE.md §6.4).
- Câu chốt: "Không để trợ lý tự xác nhận mã của chính nó."

## Slide dự phòng
- **D1** Giao thức đăng ký trước: năm phép kiểm định xác nhận (C1 đến C5), Holm (Bảng 5.27), công suất (Hình 4.2, F02).
- **D2** Catalogue vấn đề: bảng P, S, U, D rút gọn (ARTICLE.md Chương 3); F04, F05 (kiểm chứng và độ phủ của bộ phát hiện).
- **D3** Mối đe dọa tới tính hợp lệ (ARTICLE.md §4.10, §5.8).
- **D4** Nghiên cứu liên quan và khoảng trống (ARTICLE.md §1.2).
- **D5** Câu hỏi mở của E1: `coordinates.csv` có 2105 thể hiện tọa độ, 22 `hallucinated-version`, không có `hallucinated-artifact`; S11 còn "pending" nên H4 chưa kết luận (F12).

## Hình (đã dựng, `figures/out/`)
| Hình | Dùng ở | Tệp |
|---|---|---|
| thiết kế nghiên cứu | S3, bài Hình 4.1 | `F01-design.svg` |
| công suất | D1, bài Hình 4.2 | `F02-power.svg` |
| luồng E1 | S5 (tùy chọn) | `F03-e1-flow.svg` |
| kiểm chứng bộ phát hiện, độ phủ | D2 | `F04-validation.svg`, `F05-coverage.svg` |
| tần suất theo luật và kiểu prompt | S5 | `F06-prevalence.svg` |
| mật độ hiệu ứng trang trí (E1) | tùy chọn | `F11-decorative-density.svg` |
| C1, C2 | S7 | `F07-c1.svg`, `F08-c2.svg` |
| G3 | S7 | `F09-g3.svg` |
| C3 | S5 | `F10-c3.svg` |
| H1, tọa độ | D5 | `F12-h1-h4.svg` |
| E2: hiệu ứng, tuyệt đối, C5, chi phí kỳ vọng | S4, S10 | `F13-e2-effects.svg`, `F15-e2-absolute.svg`, `F14-c5.svg`, `F16-expected-cost.svg` |
| E3 | S6 | `F17-e3.svg` |
| C4, E4 theo bộ công cụ | S8 | `F18-c4.svg`, `F19-e4-bundle.svg` |
| E5 | S9 | `F20-e5.svg` |
| bảng kết luận | S11 | `F21-verdicts.svg` |
