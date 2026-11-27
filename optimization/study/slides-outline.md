# slides-outline.md — dàn ý slide, bộ slide mới (21 slide chính + D1 đến D11 dự phòng)

Khớp `presentation.html`, theo thứ tự của `report/index.html`: mở đầu (lời hứa, câu lệnh một lần, bốn app), vấn đề nói ngắn, phương pháp kèm kiểm tra môi trường, bảy mắt xích giải pháp, tóm tắt, lời kết. Lời nói từng slide: `presentation-script.md`. Chữ trên slide và ghi chú (aside.notes) là nguồn của mọi con số.

**Quy tắc.**
1. Mọi con số trên slide có trong chữ slide hoặc ghi chú của slide đó; kịch bản kiểm lại bằng máy (mục Kiểm số liệu ở `presentation-script.md`).
2. Mỗi slide giải pháp (6 đến 19, trừ 8 và 18) có hai ô: “Nếu bạn vibe-code” (thói quen và câu lệnh) và “Nếu bạn là kỹ sư” (quy trình, kiểm thử, CI, cách đo).
3. Nhân quả luôn viết là giả thuyết; “được ủng hộ” nghĩa là điều kiện thống kê đã đăng ký thỏa. Không mở đầu bằng giới hạn hay chỗ còn thiếu: giới hạn nằm ở phần Lưu ý của kịch bản và Hỏi đáp (D4), chỉ nói to khi chính slide in giới hạn đó (slide 3, 7, 11, 18).
4. Thứ tự phép kiểm định và ngưỡng nằm ở D5; bằng chứng từng slide ở cột “Bằng chứng” bên dưới.

## Thời lượng

Cột “Kịch bản” là giây trong `presentation-script.md` (số từ chia 130 từ mỗi phút, tổng 14:05). Cột “Ghi chú slide” là số “~giây” trong aside.notes của slide; hai cột phải khớp nhau.

| Phần | Slide | Kịch bản |
|---|---|---|
| Mở đầu: lời hứa và bốn app | 1–3 | 1:50 |
| Vấn đề, nói ngắn | 4 | 0:45 |
| Phương pháp và kiểm tra môi trường | 5 | 0:40 |
| Giải pháp: bảy mắt xích | 6–19 | 9:45 |
| Tóm tắt | 20 | 0:25 |
| Lời kết | 21 | 0:40 |
| **Tổng** | 1–21 | **14:05** |

## Danh sách slide chính

| # | Tiêu đề (data-title) | Phần | Ý chính | Bằng chứng | Kịch bản (giây) | Ghi chú slide (giây) |
|---|---|---|---|---|---|---|
| 1 | Mở đầu | Mở đầu | Một câu lệnh, một ứng dụng đẹp? Chúng tôi thử, rồi đo nó. | — (trang tiêu đề) | 25 | 25 |
| 2 | Lời hứa | Mở đầu | Lời hứa “một câu lệnh” trên mạng; câu lệnh nguyên văn đưa cho bốn trợ lý. | e6-oneshot/prompts/O.txt · report/sources-leadin.md | 45 | 45 |
| 3 | Thực tế | Mở đầu | Cùng câu lệnh: nhìn giống nhau, bên dưới mỗi app một khác (6/12 biên dịch được; 120 đến 3.702 dòng; giật 74–100%). | e6-oneshot/REPORT.md §2 · results/runs.csv | 40 | 40 |
| 4 | Vấn đề | Vấn đề | “AI slop”: mã trông xong nhưng mang chi phí ẩn: giật 90% → 1,3%, 203 → 6,2 MB, 60/66 có lỗi bảo mật. | e2-bench/REPORT.md P09, P06 · e1-corpus/REPORT.md:127–128 · references.md §G | 45 | 45 |
| 5 | Cách kiểm tra | Phương pháp | Sáu thí nghiệm, luật chấm khóa trước; môi trường được kiểm (623 bản ghi: không lần sinh mã nào nhận quy tắc riêng của chúng tôi). | 00-protocol.md · isolation-audit/AUDIT.md · e6-oneshot/results/canary.json | 40 | 45 |
| 6 | Chuỗi vibe-code | Giải pháp | Bảy mắt xích, mỗi cái một số đo, hai việc: cho người vibe-code, cho kỹ sư. | — (bản đồ bảy mắt xích; số lấy từ slide 7 đến 17) | 30 | 30 |
| 7 | 1 · Chọn công cụ | Giải pháp | Chỉ đổi mô hình: 1/9 → 7/9 bản biên dịch được. | E6: e6-oneshot/REPORT.md §3, §6 · results/summary.csv · E1: e1-corpus/REPORT.md §1 | 45 | 45 |
| 8 | 2 · Viết đề bài (E1) | Giải pháp | Đề dài về chức năng không an toàn hơn (31/33 so 29/33) và ít bản biên dịch hơn (20/33 so 12/33). | e1-corpus/REPORT.md:127–129 và §1 | 35 | 35 |
| 9 | 2 · Bản yêu cầu (E6) | Giải pháp | Viết như bản yêu cầu: khung giật 99% → 19,6%, ít mã hơn (524 so 1.164 dòng). | e6-oneshot/prompts/S.txt · results/summary.csv | 40 | 40 |
| 10 | 3 · Tệp quy tắc | Giải pháp | Tệp quy tắc: bản có lỗi bảo mật 31/33 → 17/34; loại lỗi 5,85 → 2,82; quy tắc phải làm xong được. | codex-luna-B2-guardrailed-r1/…/SessionStore.kt · guardrail/ANDROID-GUARDRAIL.md · evidence/code-02.kt, code-07.txt | 50 | 55 |
| 11 | 4 · Khi gặp lỗi | Giải pháp | Bị lỗi chặn, AI gỡ bảo mật 17/148 lần (khoảng 1 trong 9); chưa đủ để kết luận; sửa an toàn là làm được. | e4-pressure/runs/c5-agy-A-r5/diff.patch · results/trials.csv dòng 124 · evidence/agent-01.txt, agent-02.diff | 50 | 50 |
| 12 | 5 · Đừng tin “đã sửa xong” | Giải pháp | AI tìm đủ lỗi (168/168) nhưng tự nhận “đã sửa xong” khi không biên dịch được (9/12). | e5-optimizer/runs/e5-codex-evidence-r3 · results/runs.csv dòng 28 · evidence/agent-04.txt, agent-05.diff, agent-06.txt | 50 | 50 |
| 13 | 5 · Công cụ quét | Giải pháp | MobSF mặc định báo nhầm 7/10 và bỏ sót lỗi thật; cần người phân loại. | e3-security/results/findings.csv · e1-corpus/results/findings.csv · evidence/code-05.txt, code-06.txt | 50 | 50 |
| 14 | 6 · Danh sách và ảnh | Giải pháp | Danh sách 2.000 dòng: khung trễ 90% → 1,3%; ảnh bìa 203 → 6,2 MB. | video P09c-A/B (e2-bench/REPORT.md mục 9) | 40 | 45 |
| 15 | 6 · Hiệu ứng trang trí | Giải pháp | Hiệu ứng trang trí: mỗi khung chậm hơn 28% (62,87 so 49,07 ms); đã kiểm chứng (C5). | video P08c-A/B (e2-bench/REPORT.md mục 9) | 35 | 40 |
| 16 | 6 · Thứ người dùng cảm thấy | Giải pháp | Không phải tối ưu nào cũng cảm thấy: R8 −90,4% thấy rõ; Baseline Profile 5,88%; chỉ mục tìm sách ×7 khó thấy. | video P13c-A/B (e2-bench/REPORT.md mục 9) | 45 | 45 |
| 17 | 7 · Đo bản phát hành | Giải pháp | Bản debug 125 ms, bản đo đúng cách 12–16 ms; Baseline Profile không còn khác biệt đo được. | — (đo trên Xiaomi, ghi chú slide) | 45 | 45 |
| 18 | 7 · Lỗi chỉ máy thật mới có | Giải pháp | Ảnh bìa không tồn tại (10.0.2.2) làm giật trên máy thật: 7,0–7,2% → 1,5–2,7%. | — (đo trên Xiaomi, ghi chú slide) | 35 | 35 |
| 19 | 7 · Chữ to | Giải pháp | Chữ to 2.0: cả bốn app “đẹp” vỡ; ShelfTap 5/7 → 0/7 màn vỡ. | e6-oneshot/img/grid-O-font2.jpg · optimization/ui-ux/audit/A, B/font2/catalog.png | 35 | 35 |
| 20 | Tóm tắt | Tóm tắt | Bảy thói quen, mỗi mắt xích một việc cho mỗi người. | — (tóm tắt slide 7 đến 17) | 25 | 25 |
| 21 | Lời kết | Lời kết | Ba điều cần nhớ; không để trợ lý tự xác nhận mã của chính nó. | — | 40 | 40 |

## Slide dự phòng (không trình chiếu trừ khi được hỏi)

Mở bằng phím O (lưới) hoặc gõ số slide rồi Enter.

| Mã | Slide số | Nội dung | Bằng chứng |
|---|---|---|---|
| D1 | 22 | Kết luận thống kê: bốn trên năm giả thuyết chính xác nhận (C1, C2, C3, C5); C4 17/148 chưa kết luận; H4 0/2105; H5 1/66 | — (slide không ghi nguồn; số kiểm định ở D5) |
| D2 | 23 | So với người viết: 2,37 so 1,02 cảnh báo trên 1.000 dòng (66 bản, 20 app trước 30/11/2022) | e1-corpus/REPORT.md:127–128 (theo slide 4) |
| D3 | 24 | 101 bản nháp E1: 42/100 chạy được; 60/66 có lỗi bảo mật; ví dụ SessionStore.kt | e1-corpus/runs/codex-luna-B2-detailed-functional-r4/…/SessionStore.kt · evidence/code-01.kt |
| D4 | 25 | Giới hạn: máy ảo (−32,3% so −78,2%), mẫu 33–34 mỗi nhóm, bảng mục tiêu môn học | — (slide không ghi nguồn) |
| D5 | 26 | Bảng hiệu chỉnh Holm cho năm phép kiểm định | — (ghi chú slide: phép tính duy nhất của bài không chép từ REPORT) |
| D6 | 27 | E2 đầy đủ, B so với A, bảy nhân tố và P01 riêng | e2-bench/REPORT.md (theo slide 4, 14) |
| D7 | 28 | E4 theo lỗi và bộ công cụ: 12/30, 5/30, 0/88; agy 12/48, codex 5/50, claude 0/50 | e4-pressure/results/trials.csv (theo slide 11) |
| D8 | 29 | E1 tần suất theo luật: bảy luật × ba kiểu prompt | — (slide không ghi nguồn) |
| D9 | 30 | E6 chi tiết: số bản biên dịch được theo trợ lý × kiểu câu lệnh | e6-oneshot/REPORT.md §3, §6 · results/summary.csv |
| D10 | 31 | Kiểm tra môi trường: 623 bản ghi, ba plugin đo đạc, 18.244 token | isolation-audit/AUDIT.md · 00-protocol.md A16 |
| D11 | 32 | Nguồn của phần mở đầu | report/sources-leadin.md · references.md §G |

## Video và ảnh

- Ba video A/B, 14 giây, cùng thao tác ngón tay, lấy từ `3S-study-work/video` (`e2-bench/REPORT.md` mục 9): P09c-A/B (slide 14), P08c-A/B (slide 15), P13c-A/B (slide 16). Phải mở được không cần mạng.
- Ảnh bốn app ở slide 3 và slide 19: `e6-oneshot/img/`; ảnh ShelfTap trước và sau ở slide 19: `optimization/ui-ux/audit/A, B/font2/catalog.png`.
