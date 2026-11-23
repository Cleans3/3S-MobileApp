# presentation-script.md — kịch bản trình bày 10 phút (số liệu cuối, khớp ARTICLE.md)

Theo kịch bản của đề cương (mục VII). Slide: `slides-outline.md`; hình ở `figures/out/`. Mọi con số dưới đây có trong `ARTICLE.md` và chép từ REPORT hoặc CSV đã commit (hiệu chỉnh Holm là phép tính của bài, Bảng 5.27); người điều phối đối chiếu lại trước buổi trình bày. Khi kết quả là "không kết luận được" hoặc "không được ủng hộ", nói đúng như vậy ở vị trí ngang bằng với kết quả khác. Chỗ ghi `[TÀI NGUYÊN: ...]` là video hoặc ảnh chụp chưa có, không phải số liệu. Lời ở ngôi "chúng tôi"; số từ mỗi đoạn đếm theo khoảng trắng (kiểm bằng máy), mục tiêu khoảng 130 từ mỗi phút.

| Đoạn | Thời lượng | Slide | Từ mục tiêu | Từ thực tế |
|---|---|---|---|---|
| 1. Vấn đề | 1 phút | S1, S2 | 130 | 135 |
| 2. Demo UI, phiên bản A | 2 phút | S3, S4 | 260 | 237 |
| 3. Bảo mật | 2 phút | S5, S6 | 260 | 280 |
| 4. AI phân tích và tối ưu | 2 phút | S7, S8, S9 | 260 | 277 |
| 5. Demo phiên bản B | 2 phút | S10 | 260 (kèm thời gian phát video) | 224 |
| 6. Kết luận | 1 phút | S11 | 130 | 133 |

---

## 1. Vấn đề (0:00 đến 1:00) — S1, S2

**Lời nói.** "Chào thầy cô và các bạn. Hôm nay chúng tôi trình bày một câu hỏi rất thực tế: trợ lý AI có thể dựng cả một ứng dụng Android chỉ từ một mô tả ngắn, nhanh hơn rất nhiều so với tự viết. Nhưng bản nháp đầu tiên đó chưa ai thẩm định. Nó làm ứng dụng tốn thêm gì về giao diện, bộ nhớ và bảo mật? Và trong số đó, phần nào các kỹ thuật tối ưu quen thuộc không xử lý được, vì nguyên nhân nằm ngay trong cách mã được sinh ra? Chúng tôi dùng một ứng dụng thật, ShelfTap, thư viện thông minh dùng NFC, và đo thay vì phỏng đoán. Mọi nhận định trong bài hoặc có trích dẫn đã kiểm chứng, hoặc là số đo của chính nhóm."

**Thao tác.** S1 hiện 10 giây, chuyển S2.
**Lưu ý.** Không dùng cụm "AI gây ra"; chỉ "trợ lý phát ra X với tỷ lệ r, so với người".

## 2. Demo UI, phiên bản A (1:00 đến 3:00) — S3, S4

**Lời nói (S3, 45 giây).** "Chúng tôi làm hai phiên bản của cùng một màn hình: cùng chức năng, cùng dữ liệu. Phiên bản A là mã mà trợ lý thực sự sinh ra, lấy từ chính các lần chạy của chúng tôi và chỉ sửa cơ học để đưa vào ứng dụng thí nghiệm. Phiên bản B là bản đã tối ưu. Chúng tôi đo bảy nhân tố: hiệu ứng trang trí, giải mã ảnh, danh sách, tìm kiếm, độ ổn định kiểu, R8 và Baseline Profile. Tất cả chạy trên máy ảo, nên mọi số mili giây chỉ để so sánh tương đối giữa A và B, không phải số của điện thoại thật."

**Lời nói (S4, 75 giây).** "Đây là bản A. Chúng tôi cuộn danh sách. Với thẻ lưới có đổ bóng, lớp phủ gradient và bìa vẽ bằng canvas, thời gian CPU khung hình ở phân vị 90 là 62,87 mili giây, và 70,52 phần trăm khung hình chậm. Với màn chi tiết dựng cùng lúc 2.000 khoản vay, thời gian đến hiển thị đầu là 1.901 mili giây và 90,05 phần trăm khung hình chậm. Đây là trung vị của ít nhất 12 lần lặp xen kẽ A và B, không phải một lần đo. Riêng thẻ lưới phải đo trên một đường kết xuất GPU khác của máy ảo, vì bản port làm sập đường mặc định; số tuyệt đối của nó không so được với các nhân tố khác."

**Thao tác.** S4 phát video cuộn bản A, tạm dừng ở thời điểm đáng chú ý; hiện bảng nhỏ (Bảng 5.13, 5.15). [TÀI NGUYÊN: video A cho mã E1 chưa quay lại, chủ E2.] Nếu video lỗi: ảnh chụp Profiler chụp sẵn.
**Lưu ý.** Chỉ nói "cao hơn" khi nêu đúng số; số của thẻ lưới là trên đường GPU khác (A14), không so với số của nhân tố khác.

## 3. Bảo mật (3:00 đến 5:00) — S5, S6

**Lời nói (S5, 60 giây).** "Về bảo mật, chúng tôi cho ba bộ công cụ AI viết bản nháp đầu từ ba yêu cầu với ba kiểu prompt: 101 bản nháp, 100 hợp lệ. Chúng tôi quét bằng một bộ phát hiện đã được hai người chấm mù kiểm chứng. Khi không có guardrail, có ít nhất một phát hiện nhóm bảo mật ở 31 trên 33 lần chạy naive và 29 trên 33 lần chạy detailed-functional. Trên mỗi 1000 dòng, trung vị là 2,37, so với 1,02 của ứng dụng người viết trước tháng 11 năm 2022; delta Cliff cộng 0,582, vượt ngưỡng 0,33, và được ủng hộ sau hiệu chỉnh Holm. Phải đọc cùng: chỉ 42 trên 100 bản nháp build được, và ứng dụng của người viết là ứng dụng phổ biến, lớn hơn. Chúng tôi nói trợ lý phát ra với tỷ lệ này so với người; chúng tôi không nói AI gây ra."

**Lời nói (S6, 60 giây).** "Không thể để chính AI kết luận mã đã an toàn, nên chúng tôi dùng công cụ độc lập: MobSF, jadx và phân loại thủ công. MobSF cho 103 phát hiện trên 13 APK; sau phân loại, 30 đúng và 73 sai, độ chính xác 0,291. Cả 30 phát hiện đúng là sự kiện về manifest, cấu hình build hoặc chữ ký; không cái nào là điểm yếu ở mức mã. Cả 22 cảnh báo về thành phần exported đều sai. Tùy cách đọc nhãn, độ chính xác nằm giữa 0,244 và 0,806. Bộ phát hiện của chúng tôi thấy hai thứ MobSF không thấy: token lưu văn bản thường và nhật ký ghi cả nội dung HTTP. Hai công cụ bổ sung nhau, không thay nhau."

**Thao tác.** S5 hiện F06 và F10; S6 hiện F17 và ảnh chụp MobSF. [TÀI NGUYÊN: ảnh chụp MobSF, đoạn mã trước và sau, chủ E3.]
**Lưu ý.** Nhắc đúng giới hạn: một miền (thư viện), chỉ Android; phân loại E3 do một người.

## 4. AI phân tích và tối ưu (5:00 đến 7:00) — S7, S8, S9

**Lời nói (S7, 40 giây).** "Một tệp guardrail có giúp không? Tỷ lệ lần chạy có phát hiện nhóm bảo mật giảm từ 31 trên 33 xuống 17 trên 34, hiệu cộng 0,439; số mã luật khác nhau mỗi lần chạy giảm từ 5,85 xuống 2,82. Cả hai được ủng hộ sau hiệu chỉnh Holm. Nhưng các luật bị cố ý bỏ khỏi guardrail cũng giảm, nên chúng tôi đọc là khái quát hóa hoặc ngẫu nhiên; và ở các ứng dụng build được, hiệu của tỷ lệ bằng không. Mỗi nhánh chỉ 33 hoặc 34 lần chạy, dưới mục tiêu 36, nên kết quả bị giới hạn công suất."

**Lời nói (S8, 30 giây).** "Khi gặp lỗi chặn đường, trợ lý sửa an toàn hay làm yếu bảo mật? Trên 148 lượt thử, 17 lượt làm yếu, tỷ lệ 0,115, dưới ngưỡng 0,15: giả thuyết này không được ủng hộ. Sự làm yếu tập trung ở hai lỗi: bỏ yêu cầu xác thực khỏi khóa Keystore, 12 trên 30, và bật cleartext, 5 trên 30. Claude không làm yếu lần nào trong 50 lượt."

**Lời nói (S9, 50 giây).** "Cuối cùng, trợ lý đóng vai người tối ưu. Khi chỉ đọc, nó tìm đủ 14 vấn đề đã gieo ở mọi lần chạy. Khi được tự sửa, codex sửa 84 trên 84 và claude 76 trên 84. Nhưng 9 trên 12 cây codex đã sửa không biên dịch được, và mồi, tức mã đúng, bị đổi ở 21 trên 24 lần chạy sửa. Người phán là bộ kiểm tra độc lập, không phải lời khẳng định của trợ lý. Cho trợ lý xem đầu ra của bộ phát hiện không tạo khác biệt đo được. Đây là kết quả mô tả."

**Thao tác.** S7 hiện F07 và F08; S8 hiện F18; S9 hiện F20 và một prompt cùng một đoạn diff. [TÀI NGUYÊN: prompt và diff quan trọng nhất, chủ E5.]
**Lưu ý.** Mỗi slide nói rõ xác nhận hay mô tả; E5 chỉ mô tả; C4 không được ủng hộ là kết quả, không phải thất bại.

## 5. Demo phiên bản B (7:00 đến 9:00) — S10

**Lời nói.** "Bây giờ lặp lại đúng thao tác cũ trên bản B, cùng dữ liệu, cùng kịch bản cuộn. Với hiệu ứng trang trí, phép kiểm định đã đăng ký trước cho kết quả: thời gian CPU khung hình phân vị 90 giảm từ 62,87 xuống 49,07 mili giây, tức A cao hơn B cộng 28,13 phần trăm, khoảng tin cậy từ 13,38 đến 54,31, p hiệu chỉnh 0,000616: được ủng hộ, trên một đường GPU của máy ảo; bộ nhớ đồ họa chúng tôi không đo. Với ảnh, số byte bitmap giải mã giảm 96,9 phần trăm. Với danh sách lazy, thời gian đến hiển thị đầu giảm từ 1.901 xuống 573,1 mili giây, và khung hình chậm từ 90,05 xuống 1,255 phần trăm. Với tìm kiếm, tổng thời gian truy vấn giảm từ 634,9 xuống 85,42 mili giây, nhưng khung hình không cải thiện. Hai nhân tố cho kết quả rỗng: danh sách không key, và tham số không ổn định với Kotlin 2.4.21, nơi strong skipping là mặc định. R8 giảm APK 90,4 phần trăm; Baseline Profile giảm khởi động 5,88 phần trăm, trong khi con số khoảng 30 phần trăm là của Google, còn số đo của chúng tôi là số đo khác, trên ứng dụng một màn hình. Tất cả là máy ảo: chúng tôi chỉ khẳng định chiều hướng."

**Thao tác.** S10 phát video A và B cạnh nhau; hiện F14 (C5) rồi F13. [TÀI NGUYÊN: video A/B cho mã E1, chủ E2.] Nếu video lỗi: ảnh chụp sẵn.
**Lưu ý.** Không nêu delta% nào ngoài bảng; nhân tố có khoảng chứa 0 thì nói "không có khác biệt đo được trong thiết kế này".

## 6. Kết luận (9:00 đến 10:00) — S11

**Lời nói.** "Tóm lại, ba điều. Một, trong năm phép kiểm định xác nhận, bốn được ủng hộ sau hiệu chỉnh Holm là C1, C2, C3 và C5, còn C4 không được ủng hộ; giả thuyết về tọa độ Gradle không tồn tại chưa kết luận được. Hai, giới hạn: chỉ máy ảo, một miền ứng dụng, chỉ 42 trên 100 bản nháp build được, và cỡ mẫu dưới mục tiêu 36 mỗi nhánh. Ba, vai trò người kiểm duyệt: guardrail không thay cho kiểm tra, cần cổng biên dịch và phân loại thủ công độc lập, và xem kỹ mọi diff chạm cấu hình mạng hoặc khóa Keystore. Nguyên tắc chúng tôi giữ suốt nghiên cứu cũng là lời kết: không để trợ lý tự xác nhận mã của chính nó. Xin cảm ơn."

**Thao tác.** S11 hiện F21, một slide ba ý. Dừng còn 30 giây để nhận câu hỏi.

---

## Hỏi đáp dự kiến (trả lời bằng nội dung của bài)

| Câu hỏi có thể gặp | Hướng trả lời (nguồn trong bài) |
|---|---|
| Sao không đo trên điện thoại thật? | Chỉ có Emulator; mọi số đo E2 là bằng chứng tương đối; đã nêu là mối đe dọa (§4.10, §5.8). |
| Sao thẻ lưới đo trên đường GPU khác? | Bản port của ứng viên P08 làm sập đường mặc định, bản gốc thì không; theo A10 là lỗi của bản port nên giữ ứng viên và đo trên swangle_indirect (A14); chỉ đọc đối chiếu A/B (§5.3.3). |
| C5 có nghĩa là hiệu ứng trang trí của AI luôn tốn 28%? | Không: một ứng viên, một lần chạy E1, một đường GPU của emulator, 12 lần giữ mỗi biến thể; bộ nhớ đồ họa không đo (§5.3.3). |
| Có chứng minh AI gây ra lỗi không? | Không: so với tỷ lệ nền của người, dữ liệu quan sát; nhân quả chỉ là giả thuyết khi chưa có nhánh kiểm tra (§1.3). |
| Chỉ 42 trên 100 bản nháp build được thì sao? | Ở các ứng dụng build được C1 là +0,000 còn C2 vẫn +3,829: guardrail giảm độ rộng nhưng không giảm việc có hay không có phát hiện ở tập con đó (§5.2.3). |
| Sao C4 không được ủng hộ mà vẫn nói về Keystore? | C4 gộp là 17/148 = 0,115, dưới 0,15; sự làm yếu tập trung ở hai lỗi và hai bộ công cụ là mô tả (§5.5, §6.4). |
| Sao chọn ngưỡng 0,15 hay 10%? | Là quy ước đặt trước khi có dữ liệu, kèm dải độ nhạy; người nghe có thể không đồng ý và vẫn dùng dữ liệu (§4.8). |
| Cỡ mẫu có đủ không? | Mục tiêu từ 36 mỗi kiểu prompt, thực tế 33, 33, 34; bị giới hạn công suất (§4.8, §5.2.3). |
| Tại sao bỏ bảy luật khỏi guardrail? | Để tách làm theo chỉ dẫn khỏi khái quát hóa (G3); chọn trước khi viết guardrail; sau kiểm chứng còn năm luật (§4.3, §5.2.4). |
| Công cụ nào tốt nhất? | Không xếp hạng: bộ công cụ gồm harness cộng mô hình, hai thứ bị lẫn (§4.10, mối đe dọa 5). |
| Kết quả có đúng với Compose phiên bản mới không? | Chi phí P03 phụ thuộc phiên bản: từ Kotlin 2.0.20 strong skipping bật mặc định, và ở E2 P03 cùng P04 không có chi phí đo được (§2.2.3, §5.3.3). |
| H4 (tọa độ Gradle không tồn tại) thì sao? | Chưa kết luận: S11 còn "pending"; tệp tọa độ không có lớp hallucinated-artifact nhưng REPORT E1 chưa xác nhận (§5.2.6). |
| Sao dùng ShelfTap, không phải SmartTask như đề cương? | Để tình huống bảo mật là thật (token, lịch sử mượn); lý do ở §1.6. |
| Có nhóm lập trình viên tự tối ưu để so không? | Không: nằm ngoài phạm vi vì không có người tham gia (§4.7, §4.10). |

## Chuẩn bị trước buổi trình bày
- [ ] Mọi số trên slide đã đối chiếu với ARTICLE.md, CSV hoặc khóa của `references.md` (người điều phối).
- [ ] [TÀI NGUYÊN] Video A và B (quay lại cho mã E1), ảnh chụp Profiler và MobSF đã tải sẵn, mở được không cần mạng.
- [ ] Đã chạy thử bằng đồng hồ: mỗi đoạn không vượt thời lượng ở bảng đầu.
- [ ] Phương án khi video lỗi: ảnh chụp chụp sẵn ở cùng thư mục với slide.
- [ ] Slide dự phòng D1 đến D5 sẵn sàng cho câu hỏi.
