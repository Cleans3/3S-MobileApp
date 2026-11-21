# presentation-script.md — kịch bản trình bày 10 phút (khung, chưa có số liệu)

Theo kịch bản của đề cương (mục VII). Slide: `slides-outline.md`. Mọi chỗ ghi `[SỐ ← TODO[mã]]` là số **chưa có**; chỉ điền khi REPORT tương ứng có số và người điều phối đã đối chiếu với CSV. Không nói số dự kiến. Khi kết quả là "không kết luận được", nói đúng như vậy ở vị trí ngang bằng với kết quả khác. Lời dưới đây ở ngôi "chúng tôi", tốc độ khoảng 130 từ mỗi phút; mỗi đoạn có số từ gợi ý để canh giờ.

| Đoạn | Thời lượng | Slide | Từ gợi ý |
|---|---|---|---|
| 1. Vấn đề | 1 phút | S1, S2 | 130 |
| 2. Demo UI, phiên bản A | 2 phút | S3, S4 | 260 |
| 3. Bảo mật | 2 phút | S5, S6 | 260 |
| 4. AI phân tích và tối ưu | 2 phút | S7, S8, S9 | 260 |
| 5. Demo phiên bản B | 2 phút | S10 | 260 |
| 6. Kết luận | 1 phút | S11 | 130 |

---

## 1. Vấn đề (0:00 đến 1:00) — S1, S2

**Lời nói.** "Chào thầy cô và các bạn. Hôm nay chúng tôi trình bày một câu hỏi rất thực tế: trợ lý AI có thể dựng cả một ứng dụng Android chỉ từ một mô tả ngắn, nhanh hơn rất nhiều so với tự viết. Nhưng bản nháp đầu tiên đó chưa ai thẩm định. Nó làm ứng dụng tốn thêm gì về giao diện, bộ nhớ và bảo mật? Và trong số đó, phần nào các kỹ thuật tối ưu quen thuộc không xử lý được, vì nguyên nhân nằm ngay trong cách mã được sinh ra? Chúng tôi dùng một ứng dụng thật, ShelfTap, thư viện thông minh dùng NFC, và đo thay vì phỏng đoán. Mọi nhận định trong bài hoặc có trích dẫn đã kiểm chứng, hoặc là số đo của chính nhóm."

**Thao tác.** S1 hiện 10 giây, chuyển S2.
**Lưu ý.** Không dùng cụm "AI gây ra"; chỉ "trợ lý phát ra X với tỷ lệ r, so với người".

## 2. Demo UI, phiên bản A (1:00 đến 3:00) — S3, S4

**Lời nói (S3, 45 giây).** "Chúng tôi làm hai phiên bản của cùng một màn hình: cùng chức năng, cùng dữ liệu. Phiên bản A lấy từ mã mà trợ lý thực sự sinh ra. Phiên bản B là bản đã tối ưu. Chúng tôi đo bảy mẫu vấn đề, từ hiệu ứng trang trí và ảnh, đến danh sách, tìm kiếm, R8 và Baseline Profile. Xin lưu ý: tất cả chạy trên máy ảo, nên các số đo là so sánh tương đối giữa A và B, không phải số tuyệt đối của điện thoại thật."

**Lời nói (S4, 75 giây).** "Đây là bản A. Chúng tôi cuộn danh sách, và đây là những gì công cụ đo cho thấy: [SỐ ← TODO[e2-table]: một đến hai số đo chính của A, nói tên đại lượng và đơn vị]. Với mỗi số, chúng tôi ghi đây là số đo trung vị của nhiều lần chạy xen kẽ A và B, không phải một lần đo."

**Thao tác.** S4 phát video cuộn bản A (`TODO[e2-video]`), tạm dừng ở thời điểm đáng chú ý; hiện bảng nhỏ. Nếu video lỗi: dùng ảnh chụp màn hình Profiler chụp sẵn.
**Lưu ý.** Không đọc số khi chưa có; không nói "kết quả cho thấy A tệ hơn" nếu delta% chưa vượt dải đã đăng ký.

## 3. Bảo mật (3:00 đến 5:00) — S5, S6

**Lời nói (S5, 60 giây).** "Về bảo mật, chúng tôi cho các bộ công cụ AI viết các bản nháp đầu từ ba yêu cầu khác nhau, với ba kiểu prompt, rồi quét bằng một bộ phát hiện đã được kiểm chứng. Để biết con số nào là bình thường, chúng tôi đối chiếu với ứng dụng mã nguồn mở do người viết trước ngày công bố ChatGPT. Kết quả: [SỐ ← TODO[e1-prevalence], TODO[e1-C3]: tần suất vấn đề nhóm bảo mật theo kiểu prompt; mật độ trên 1000 dòng của AI so với người; kết luận theo quy tắc đã đăng ký: ủng hộ, bác bỏ hay không kết luận được]. Chúng tôi nói 'trợ lý phát ra với tỷ lệ này so với người', chúng tôi không nói 'AI gây ra'."

**Lời nói (S6, 60 giây).** "Không thể để chính AI kết luận mã đã an toàn. Vì vậy chúng tôi dùng công cụ độc lập, MobSF, JADX, và kiểm tra thủ công theo OWASP. Đây là ảnh chụp kết quả MobSF. Số cảnh báo chưa phải kết luận: chúng tôi phân loại từng phát hiện, loại dương tính giả. Ví dụ một vấn đề cụ thể: [VÍ DỤ ← TODO[e3-table]: một phát hiện đã xác nhận trong mã, cách sửa]. Kết quả phân loại: [SỐ ← TODO[e3-table]: số phát hiện đúng và sai, độ chính xác theo công cụ]."

**Thao tác.** S5 hiện biểu đồ; S6 hiện ảnh chụp MobSF chụp sẵn và một đoạn mã trước và sau.
**Lưu ý.** Nhắc đúng giới hạn: một miền (thư viện), chỉ Android.

## 4. AI phân tích và tối ưu (5:00 đến 7:00) — S7, S8, S9

**Lời nói (S7, 40 giây).** "Một tệp chỉ dẫn, gọi là guardrail, có giúp không? Chúng tôi viết nó từ tài liệu OWASP và Android, không nhìn vào bộ phát hiện, và cố ý bỏ bảy luật ra khỏi tệp để biết nó chỉ giúp ở luật được nêu hay cả ở luật không được nêu. Kết quả: [SỐ ← TODO[e1-C1], TODO[e1-C2], TODO[e1-G3]]. Với cỡ mẫu của chúng tôi, [NẾU n dưới 36 mỗi nhánh: 'kết quả này bị giới hạn công suất và không cho phép kết luận không có hiệu ứng']."

**Lời nói (S8, 30 giây).** "Khi gặp một lỗi chặn đường, trợ lý sửa an toàn hay làm yếu bảo mật cho qua lỗi? Chúng tôi đặt trợ lý trước năm lỗi có chủ đích, chẳng hạn chứng chỉ tự ký. Tỷ lệ kết quả an toàn và không an toàn: [SỐ ← TODO[e4-C4], TODO[e4-by-case]]. Một ví dụ cụ thể: [VÍ DỤ ← TODO[e4-by-case]]."

**Lời nói (S9, 50 giây).** "Cuối cùng, giao cho trợ lý vai trò người tối ưu một ứng dụng đã có, với và không có bằng chứng, ở ba mức kiểm soát. Ai phán xem bản sửa đúng? Không phải trợ lý: bộ kiểm thử độc lập. Kết quả: [SỐ ← TODO[e5-table]: độ chính xác, độ phủ, hồi quy, số lần người phải can thiệp]. Đây là prompt chúng tôi đưa và đây là thay đổi quan trọng nhất mà trợ lý đề xuất: [CHI TIẾT ← TODO[e5-table]]."

**Thao tác.** S7 biểu đồ hai nhóm; S8 bảng năm lỗi; S9 hiện prompt và đoạn khác biệt mã (diff).
**Lưu ý.** Mỗi slide phải nói rõ có phải kết luận xác nhận hay chỉ mô tả; E5 chỉ mô tả.

## 5. Demo phiên bản B (7:00 đến 9:00) — S10

**Lời nói.** "Bây giờ lặp lại đúng thao tác cũ trên phiên bản B, cùng dữ liệu, cùng kịch bản cuộn. Bên trái là A, bên phải là B. Kết quả đo: [SỐ ← TODO[e2-table]: trước và sau cho các nhân tố chính]. Với hiệu ứng trang trí, phép kiểm định đã đăng ký trước cho kết quả: [SỐ ← TODO[e2-C5]: delta% của thời gian khung hình, khoảng tin cậy, kết luận theo quy tắc]. Xin nhắc lại, đây là máy ảo nên số đo khung hình chỉ mang tính tương đối; chúng tôi chỉ khẳng định chiều hướng, không khẳng định số tuyệt đối. Về Baseline Profile, con số khoảng 30 phần trăm là con số do Google nêu trong tài liệu; số đo của chúng tôi là một số đo khác, trên một ứng dụng đơn giản."

**Thao tác.** S10 phát video A và B cạnh nhau (`TODO[e2-video]`); hiện bảng trước sau. Nếu video lỗi: ảnh chụp sẵn.
**Lưu ý.** Không nêu delta% nào ngoài bảng; nếu một nhân tố không vượt SESOI, nói "không có khác biệt đo được trong thiết kế này".

## 6. Kết luận (9:00 đến 10:00) — S11

**Lời nói.** "Tóm lại, ba điều. Một, những điều chúng tôi đo được và những điều không kết luận được: [TỔNG KẾT ← TODO[verdicts]]. Hai, giới hạn của nghiên cứu: chỉ máy ảo, một miền ứng dụng, cỡ mẫu giới hạn công suất ở một số phép so sánh. Ba, vai trò người kiểm duyệt: [KHUYẾN NGHỊ ← TODO[concl-process]: chỉ những khuyến nghị mà số liệu hỗ trợ]. Nguyên tắc chúng tôi giữ suốt nghiên cứu cũng là lời kết: không để trợ lý tự xác nhận mã của chính nó. Xin cảm ơn."

**Thao tác.** S11 một slide ba ý. Dừng còn 30 giây để nhận câu hỏi.

---

## Hỏi đáp dự kiến (trả lời bằng nội dung đã đăng ký, không bằng kết quả chưa có)

| Câu hỏi có thể gặp | Hướng trả lời (nguồn trong bài) |
|---|---|
| Sao không đo trên điện thoại thật? | Chỉ có Emulator; mọi số đo E2 là bằng chứng tương đối; đã nêu là mối đe dọa (§4.10). |
| Có chứng minh AI gây ra lỗi không? | Không: so với tỷ lệ nền của người, dữ liệu quan sát; nhân quả chỉ là giả thuyết khi chưa có nhánh kiểm tra (§1.3). |
| Sao chọn ngưỡng 0,15 hay 10%? | Là quy ước đặt trước khi có dữ liệu, kèm dải độ nhạy; người nghe có thể không đồng ý và vẫn dùng dữ liệu (§4.8). |
| Cỡ mẫu có đủ không? | Mục tiêu từ 36 mỗi kiểu prompt; dưới đó, kết quả không kết luận được là kết quả được kỳ vọng và được báo cáo là bị giới hạn công suất (§4.8). |
| Tại sao bỏ bảy luật khỏi guardrail? | Để tách làm theo chỉ dẫn khỏi khái quát hóa (G3); chọn trước khi viết guardrail (§4.3). |
| Công cụ nào tốt nhất? | Không xếp hạng: bộ công cụ gồm harness cộng mô hình, hai thứ bị lẫn (§4.10, mối đe dọa 5). |
| Kết quả có đúng với Compose phiên bản mới không? | Chi phí P03 phụ thuộc phiên bản: từ Kotlin 2.0.20 strong skipping bật mặc định (§2.2.3). |
| Sao dùng ShelfTap, không phải SmartTask như đề cương? | Để tình huống bảo mật là thật (token, lịch sử mượn); lý do ở §1.6. |
| Có nhóm lập trình viên tự tối ưu để so không? | Không: nằm ngoài phạm vi vì không có người tham gia (§4.7, §4.10). |

## Chuẩn bị trước buổi trình bày
- [ ] Mọi số trên slide đã đối chiếu với CSV hoặc khóa của `references.md` (người điều phối).
- [ ] Video A, B và ảnh chụp Profiler, MobSF đã tải sẵn, mở được không cần mạng.
- [ ] Đã chạy thử bằng đồng hồ: mỗi đoạn không vượt thời lượng ở bảng đầu.
- [ ] Phương án khi video lỗi: ảnh chụp chụp sẵn ở cùng thư mục với slide.
- [ ] Slide dự phòng D1 đến D4 sẵn sàng cho câu hỏi.
