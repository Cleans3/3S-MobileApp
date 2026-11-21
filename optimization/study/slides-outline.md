# slides-outline.md — dàn ý slide (khung, chưa có số liệu)

Bài trình bày 10 phút theo kịch bản của đề cương (mục VII): 1 phút vấn đề, 2 phút demo UI phiên bản A, 2 phút demo bảo mật, 2 phút AI phân tích và tối ưu, 2 phút demo phiên bản B, 1 phút kết luận. Nguồn nội dung là `ARTICLE.md`; kịch bản lời nói ở `presentation-script.md`.

**Quy tắc.** (1) Slide kết quả chỉ chứa **chỗ trống có tên nguồn** (`TODO[mã]` như trong ARTICLE.md) cho đến khi các REPORT có số; không điền số dự kiến. (2) Mỗi con số trên slide phải khớp một ô của CSV hoặc một khóa trong `references.md`; người điều phối đối chiếu trước buổi trình bày. (3) Theo khuyến nghị của đề cương, quay trước video thao tác và chụp sẵn kết quả (Profiler, MobSF) để không mất thời gian khi trình diễn trực tiếp; video A/B do chủ E2 quay, đường dẫn trong `e2-bench/REPORT.md`. (4) Chữ trên slide ngắn, một ý một slide; chi tiết nằm ở bài viết. (5) Nhân quả luôn viết là "giả thuyết" khi chưa có nhánh thực nghiệm kiểm tra; không viết "AI gây ra".

## Phân bổ thời gian (tổng 10 phút)

| Đoạn | Thời lượng | Slide | Nội dung |
|---|---|---|---|
| 1. Vấn đề | 1 phút | S1, S2 | AI dựng ứng dụng nhanh, nhưng bản nháp đầu tốn gì và ai kiểm chứng |
| 2. Demo UI, phiên bản A | 2 phút | S3, S4 | cuộn danh sách ShelfTap bản A, số đo |
| 3. Bảo mật | 2 phút | S5, S6 | tần suất vấn đề bảo mật trong bản nháp của AI so với người; công cụ độc lập |
| 4. AI phân tích và tối ưu | 2 phút | S7, S8, S9 | guardrail; AI sửa lỗi dưới áp lực; AI tối ưu |
| 5. Demo phiên bản B | 2 phút | S10 | cùng thao tác, bản đã tối ưu, số đo trước và sau |
| 6. Kết luận | 1 phút | S11 | kết quả, hạn chế, vai trò người kiểm duyệt |

Slide dự phòng (không trình chiếu trừ khi được hỏi): D1 giao thức đăng ký trước và công suất; D2 catalogue vấn đề; D3 mối đe dọa; D4 so sánh với nghiên cứu liên quan.

## Danh sách slide

### S1. Tiêu đề (30 giây)
- Tên đề tài, nhóm, ứng dụng minh họa (ShelfTap).
- Hình: logo hoặc ảnh màn hình ShelfTap. Nguồn: `TODO[app]` trong ARTICLE.md §5.1.

### S2. Vấn đề (30 giây)
- Thông điệp: trợ lý AI dựng được cả ứng dụng Android từ một mô tả ngắn; bản nháp đầu chưa ai thẩm định.
- Câu hỏi: bản nháp đó làm ứng dụng tốn thêm gì về giao diện, bộ nhớ, bảo mật; phần nào các kỹ thuật tối ưu quen thuộc không xử lý được.
- Hình: sơ đồ một dòng "mô tả ngắn → bản nháp đầu → ai kiểm chứng?".
- Nguồn: ARTICLE.md §1.1, §1.3.

### S3. Thiết lập (45 giây, mở đầu đoạn 2)
- Thông điệp: hai phiên bản cùng chức năng, cùng dữ liệu; chỉ khác mẫu mã. A lấy từ mã mà AI thật sự sinh ra; B là bản đã tối ưu.
- Nội dung: ứng dụng thí nghiệm, 7 nhân tố (hiệu ứng trang trí, ảnh, danh sách, tìm kiếm, ổn định kiểu, R8, Baseline Profile), đo trên Emulator, số đo chính độc lập thiết bị.
- Hình: sơ đồ A/B. Nguồn: ARTICLE.md §4.4, `e2-bench/variants.csv`.
- Lưu ý bắt buộc trên slide: "chỉ Emulator: số đo tương đối".

### S4. Demo UI, phiên bản A (75 giây)
- Thông điệp: cuộn danh sách bản A; những gì Profiler cho thấy.
- Video: cuộn danh sách bản A. Nguồn: `TODO[e2-video]`.
- Bảng nhỏ: một đến hai số đo chính của A (ví dụ số lần recomposition, byte bitmap đã giải mã). **Chỗ trống:** `TODO[e2-table]` ← `e2-bench/results/summary.csv` (dòng của nhân tố tương ứng, cột `A_median`). Không điền trước.

### S5. Bản nháp của AI chứa gì (60 giây)
- Thông điệp: tần suất các vấn đề trong bản nháp đầu, so với ứng dụng do người viết trước 2022-11-30.
- Hình: thanh tần suất theo lớp (P, S, U, D) và theo kiểu prompt. **Chỗ trống:** `TODO[e1-prevalence]` và `TODO[e1-C3]` ← `e1-corpus/results/findings.csv`, `human_findings.csv`.
- Câu bắt buộc: "tỷ lệ của trợ lý so với tỷ lệ nền của người", không nói "AI gây ra".
- Nếu C3 không kết luận được: slide nói đúng như vậy.

### S6. Công cụ độc lập kiểm tra (60 giây)
- Thông điệp: không để AI tự kết luận mã đã an toàn; MobSF, JADX và kiểm tra thủ công là bên phán.
- Hình: ảnh chụp MobSF (chụp sẵn), một vấn đề cụ thể trong mã (ví dụ token ghi văn bản thường) kèm cách sửa. **Chỗ trống:** `TODO[e3-table]` ← `e3-security/results/findings.csv`.
- Nhấn: số cảnh báo của MobSF chưa phải kết luận; đã phân loại thủ công.

### S7. Guardrail (40 giây)
- Thông điệp: một tệp chỉ dẫn có giúp không, và chỉ giúp ở luật nó nêu hay cả luật nó bỏ.
- Hình: hai nhóm cột (naive, guardrailed) cho tỷ lệ có phát hiện nhóm S; một cột hiệu ứng trên luật được chỉ dẫn so với luật bị giữ lại. **Chỗ trống:** `TODO[e1-C1]`, `TODO[e1-C2]`, `TODO[e1-G3]`.
- Nhấn công suất: nếu n dưới 36 mỗi nhánh, nói "bị giới hạn công suất" (`TODO[e1-power]`).

### S8. AI sửa lỗi dưới áp lực (30 giây)
- Thông điệp: khi lỗi chặn đường, trợ lý sửa an toàn hay làm yếu bảo mật.
- Hình: năm lỗi và tỷ lệ kết quả; nêu một ví dụ cụ thể (ví dụ tin tất cả chứng chỉ). **Chỗ trống:** `TODO[e4-C4]`, `TODO[e4-by-case]` ← `e4-pressure/results/trials.csv`.

### S9. AI tối ưu ứng dụng có sẵn (50 giây)
- Thông điệp: có và không có bằng chứng, ba mức kiểm soát; ai phán bản sửa đúng.
- Hình: bảng độ chính xác, độ phủ, hồi quy; trình chiếu một prompt và một thay đổi quan trọng (theo đề cương). **Chỗ trống:** `TODO[e5-table]` ← `e5-optimizer/`.

### S10. Demo phiên bản B (2 phút)
- Thông điệp: lặp lại đúng thao tác cũ, bản đã tối ưu; so sánh trước và sau.
- Video: cuộn danh sách bản B cạnh bản A (cùng thao tác). Nguồn: `TODO[e2-video]`.
- Bảng: trước và sau cho các nhân tố chính. **Chỗ trống:** `TODO[e2-table]` và `TODO[e2-C5]` ← `summary.csv`, `raw.csv`.
- Nhắc lại: Emulator, tương đối; hiệu ứng của Baseline Profile là con số của Google (khoảng 30%) trong tài liệu, số đo của chúng ta là số đo khác [Baseline-Overview].

### S11. Kết luận (60 giây)
- Ba ý: (1) điều đo được và điều không kết luận được (`TODO[verdicts]`); (2) hạn chế: Emulator, một miền, công suất (ARTICLE.md §4.10); (3) vai trò người kiểm duyệt và quy trình đề xuất (`TODO[concl-process]`).
- Câu chốt: "Không để trợ lý tự xác nhận mã của chính nó."

## Slide dự phòng
- **D1** Giao thức đăng ký trước: năm phép kiểm định xác nhận (C1 đến C5), hiệu chỉnh Holm, bảng công suất (ARTICLE.md §4.8).
- **D2** Catalogue vấn đề: bảng P, S, U, D rút gọn (ARTICLE.md Chương 3).
- **D3** Mối đe dọa tới tính hợp lệ (ARTICLE.md §4.10, §5.8).
- **D4** Nghiên cứu liên quan và khoảng trống (ARTICLE.md §1.2).

## Hình cần chuẩn bị (thuộc `figures/`, làm khi có số liệu)
Kế hoạch đầy đủ (mã F01 đến F21, cột CSV, kiểu biểu đồ, quy tắc chú thích, điều kiện chờ) nằm ở `figures/PLAN.md`; bảng dưới là tóm tắt cho slide.
| Hình | Dùng ở | Nguồn dữ liệu |
|---|---|---|
| tần suất theo lớp và kiểu prompt | S5, bài §5.2.2 | `e1-corpus/results/findings.csv` ghép `runs.csv` |
| C1, C2: hai nhóm naive và guardrailed | S7, bài §5.2.3 | `runs.csv`, `findings.csv` |
| G3: hiệu ứng được chỉ dẫn và bị giữ lại | S7, bài §5.2.4 | `findings.csv` |
| C3: mật độ nhóm S, AI và người | S5, bài §5.2.5 | `findings.csv`, `human_findings.csv`, `human_metrics.csv` |
| A/B theo nhân tố | S4, S10, bài §5.3 | `e2-bench/results/summary.csv`, `raw.csv` |
| C5: P90 khung hình A và B | S10, bài §5.3 | `e2-bench/results/raw.csv` |
| E4: kết quả theo lỗi | S8, bài §5.5 | `e4-pressure/results/trials.csv` |
| E5: độ chính xác, độ phủ, hồi quy | S9, bài §5.6 | `e5-optimizer/` |
