# presentation-script.md — kịch bản trình bày ~14 phút (bộ slide mới: 21 slide chính + D1 đến D11 dự phòng)

Khớp `presentation.html` (bộ slide theo thứ tự của `report/index.html`): mở đầu (lời hứa, câu lệnh một lần, bốn app), vấn đề nói ngắn, phương pháp kèm kiểm tra môi trường, bảy mắt xích giải pháp, tóm tắt, lời kết. Dàn slide, bằng chứng từng slide: `slides-outline.md`. Nhóm đứng trên sân khấu và đọc bản này; viết ở ngôi “chúng tôi”, tiếng Việt nói.

**Quy tắc của bản này.**
- Mọi con số trong phần Lời nói, Lưu ý và Hỏi đáp chép nguyên văn từ chữ trên slide hoặc từ ghi chú (aside.notes) của slide đó, dấu phẩy thập phân kiểu Việt. Kiểm bằng máy: lệnh ở mục “Kiểm số liệu” cuối file.
- Số từ mỗi đoạn đếm theo khoảng trắng bằng máy. Thời lượng = số từ chia 130 từ mỗi phút (tốc độ của bản kịch bản trước), làm tròn 5 giây. Ghi chú trong slide cộng lại 16 phút 5 giây; bản này ngắn hơn để khớp 13 đến 15 phút, nên đọc theo bản này, không theo “~giây” trong ghi chú slide.
- Mỗi slide giải pháp đọc ngắn hai ô: “Nếu bạn vibe-code” (thói quen và câu lệnh) rồi “Nếu bạn là kỹ sư” (quy trình, kiểm thử, CI, cách đo). Trên sân khấu không mở đầu bằng giới hạn hay chỗ còn thiếu: giới hạn nằm ở phần Lưu ý và Hỏi đáp (slide dự phòng D4), chỉ nói to khi chính slide in giới hạn đó (slide 3, 7, 11, 18).
- Nguyên tắc dùng từ: nói “trợ lý phát ra X với tỷ lệ r so với người”, không nói “AI gây ra X”. Kết quả chưa đủ bằng chứng thì nói đúng như vậy, cùng giọng với kết quả khác.

## Bảng thời lượng

| Phần | Slide | Thời lượng | Từ |
|---|---|---|---|
| Mở đầu: lời hứa và bốn app | 1–3 | 1:50 | 239 |
| Vấn đề, nói ngắn | 4 | 0:45 | 96 |
| Phương pháp và kiểm tra môi trường | 5 | 0:40 | 87 |
| Giải pháp: bảy mắt xích | 6–19 | 9:45 | 1281 |
| Tóm tắt | 20 | 0:25 | 51 |
| Lời kết | 21 | 0:40 | 88 |
| **Tổng** | 1–21 | **14:05** | **1842** |

---

## 1. Mở đầu (0:00 đến 0:25) — slide 1

*Mở đầu · 25 giây · 53 từ*

**Lời nói.** Chào thầy cô và các bạn. Một câu lệnh, một ứng dụng đẹp? Chúng tôi thử, rồi đo nó. Đây là ShelfTap, ứng dụng thư viện NFC của nhóm. Phía sau là 6 thí nghiệm, 101 bản nháp do AI viết, 36 lần chạy câu lệnh một lần, và 1 ứng dụng thật.

**Thao tác.** Dừng một nhịp sau câu hỏi trên tiêu đề, rồi mới nói tiếp.

**Lưu ý.** Mã chạy ở nền slide là mẫu minh họa ghép từ các kiểu mã trợ lý đã sinh ra trong nghiên cứu.

## 2. Lời hứa (0:25 đến 1:10) — slide 2

*Mở đầu · 45 giây · 95 từ*

**Lời nói.** Trên mạng, ai cũng thấy ảnh một app thật đẹp, kèm lời hứa: dán một câu lệnh vào là ra y như vậy. Tháng 2 năm 2025, Karpathy đặt tên “vibe coding”: quên rằng mã nguồn tồn tại, bấm “Accept All” mà không đọc. 84% lập trình viên đang hoặc sắp dùng công cụ AI. Chính Karpathy giới hạn cách làm này cho dự án cuối tuần dùng xong bỏ. Chúng tôi viết một câu lệnh đúng kiểu đó, “one shot, production ready”, và dán nguyên văn vào bốn trợ lý, trong môi trường sạch.

**Thao tác.** Đọc câu lệnh trên slide bằng giọng quảng cáo, không đọc hết.

**Lưu ý.** Nguồn từng trích dẫn: report/sources-leadin.md; backup D11.

## 3. Thực tế (1:10 đến 1:50) — slide 3

*Mở đầu · 40 giây · 91 từ*

**Lời nói.** Đây là thứ chúng tạo ra. Bốn ảnh nhìn giống nhau: câu lệnh quyết định vẻ ngoài. Nhưng bên dưới, mỗi app một khác: từ 120 đến 3.702 dòng mã, chênh nhau 31 lần. Trong 12 lần chạy, 6 lần biên dịch được, và một app mở lên là sập. Khi cuộn, 74 đến 100% khung hình bị giật ở cả bốn app. Mỗi ảnh là lần chạy tốt nhất trong 3 lần, chọn theo quy tắc ghi trước. Khung giật đo trên máy ảo, chỉ so trong thí nghiệm này.

**Thao tác.** Chỉ lần lượt từng ảnh khi đọc số dòng.

**Lưu ý.** 36 lần chạy = 4 trợ lý × 3 kiểu câu lệnh × 3 lần; slide này chỉ là kiểu câu lệnh một lần (12 lần). Hai trợ lý claude dùng cùng công cụ, chỉ khác mô hình. Ảnh chọn theo quy tắc ghi trước: khung giật thấp nhất trong các lần mở được.

## 4. Vấn đề (1:50 đến 2:35) — slide 4

*Vấn đề · 45 giây · 96 từ*

**Lời nói.** Vấn đề, nói ngắn: “AI slop”, mã trông xong nhưng mang chi phí ẩn mà ảnh chụp không cho thấy. Giật: 90% khung hình bị trễ vì danh sách 2.000 dòng dựng hết; bản sửa còn 1,3%. Tốn bộ nhớ: 15 ảnh bìa tải ở kích thước gốc, 203 MB; bản sửa 6,2 MB. Lộ dữ liệu: 60 trên 66 bản nháp không kèm hướng dẫn bảo mật có lỗi bảo mật, và cảnh báo dày gấp 2,3 lần app người viết trước thời ChatGPT. Vấn đề có thật. Phần còn lại là cách xử lý.

**Thao tác.** Giữ ngắn: chỉ ba ví dụ, không đi vào Veracode hay Stack Overflow trừ khi được hỏi.

**Lưu ý.** Mốc người viết: 20 app mã nguồn mở có commit đầu trước 30/11/2022; số cụ thể 2,37 so với 1,02 ở backup D2.

## 5. Cách kiểm tra (2:35 đến 3:15) — slide 5

*Phương pháp · 40 giây · 87 từ*

**Lời nói.** Chúng tôi kiểm tra thế nào? Sáu thí nghiệm, luật chấm khóa trước khi có dữ liệu: AI viết app, đo A/B, công cụ quét, gây lỗi 148 lần, AI tự tối ưu, và câu lệnh một lần. Môi trường của chúng tôi có làm lệch kết quả không? Mỗi lần sinh mã chạy trong thư mục nhà giả. Quét lại 623 bản ghi hội thoại: không lần sinh mã nào nhận quy tắc riêng của chúng tôi. Đối chứng dùng cấu hình thật thì lộ ngay.

**Thao tác.** Chỉ vào khung “Môi trường của chúng tôi có làm lệch kết quả không?” khi nói đoạn giữa.

**Lưu ý.** Giới hạn chỉ nói khi được hỏi (backup D4): số đo trên máy ảo; mỗi nhóm 33 đến 34 bản nháp; một loại ứng dụng; không xếp hạng trợ lý. Nếu bị hỏi sâu về môi trường: backup D10.

## 6. Chuỗi vibe-code (3:15 đến 3:45) — slide 6

*Giải pháp · 30 giây · 66 từ*

**Lời nói.** Phần còn lại là giải pháp. Không bước nào sửa được tất cả; mỗi mắt xích chặn một phần, và phần nào cũng có số đo. Bảy mắt xích, theo thứ tự làm app: chọn công cụ, viết đề bài, tệp quy tắc, khi gặp lỗi, kiểm tra, sửa kỹ thuật, đo. Mỗi mắt xích có hai việc: một cho người vibe-code, một cho kỹ sư.

**Thao tác.** Chỉ hai ô cuối: “Nếu bạn vibe-code” và “Nếu bạn là kỹ sư”; từ đây mỗi slide giải pháp đọc cả hai.

## 7. 1 · Chọn công cụ (3:45 đến 4:30) — slide 7

*Giải pháp · 45 giây · 100 từ*

**Lời nói.** Mắt xích một: chọn công cụ. Cùng Claude Code, cùng câu lệnh, chỉ đổi mô hình: claude-haiku 1 trên 9 bản biên dịch được, claude-sonnet 7 trên 9; mô hình mạnh hơn tốn hơn, 0,48 so với 0,11 USD mỗi lần chạy. Ở E1, codex-luna 10 trên 36, agy-g38flash 19 trên 28. Chúng tôi không xếp hạng, vì mẫu nhỏ; điều chắc chắn là độ chênh. Vibe-code: chạy lại câu lệnh 2 đến 3 lần, so “có biên dịch, có mở được không”, đừng so ảnh đẹp. Kỹ sư: ghi công cụ, mô hình, phiên bản mỗi lần sinh mã.

**Lưu ý.** Cặp claude-haiku và claude-sonnet dùng cùng trình chạy, cùng cờ, nên tách được hiệu ứng mô hình khỏi hiệu ứng công cụ. Chi phí chỉ có cho hai bộ claude.

## 8. 2 · Viết đề bài (E1) (4:30 đến 5:05) — slide 8

*Giải pháp · 35 giây · 78 từ*

**Lời nói.** Mắt xích hai: viết đề bài. Đề dài hơn về chức năng không làm app an toàn hơn, và còn ít bản biên dịch được hơn: 20 trên 33 với đề ngắn, 12 trên 33 với đề chi tiết; lỗi bảo mật vẫn 31 trên 33 và 29 trên 33. Đề dài, AI viết nhiều mã hơn, nhiều chỗ hỏng hơn. Thứ cần thêm không phải chức năng, mà là ràng buộc kỹ thuật và tiêu chí “xong”.

**Lưu ý.** Hai khoảng tin cậy của tỷ lệ lỗi bảo mật chồng lên nhau: không khác biệt, và đó chính là điểm cần nói. Mỗi nhóm 33 bản nháp.

## 9. 2 · Bản yêu cầu (E6) (5:05 đến 5:45) — slide 9

*Giải pháp · 40 giây · 89 từ*

**Lời nói.** Viết thế nào? Như một bản yêu cầu có cấu trúc: màn hình, dữ liệu, danh sách lười có khóa, ảnh đúng kích thước ô, không làm mờ hay đổ bóng trên từng dòng, và “xong” nghĩa là assembleDebug chạy qua. Cùng claude-sonnet, khung giật từ 99% xuống 19%; trung vị cả bốn trợ lý, 99% xuống 19,6%. Mã cũng ít hơn: 524 so với 1.164 dòng. Vibe-code: viết màn hình, dữ liệu và ràng buộc. Kỹ sư: cho nhóm một mẫu đề bài, mỗi lần một màn hình.

**Lưu ý.** Bản yêu cầu chứa nhiều thông tin hơn, nên đây là so “đề bài kỹ thuật” với “đề bài quảng cáo”, không phải thử một yếu tố; mỗi ô 3 lần chạy, số liệu để mô tả.

## 10. 3 · Tệp quy tắc (5:45 đến 6:35) — slide 10

*Giải pháp · 50 giây · 105 từ*

**Lời nói.** Mắt xích ba: tệp quy tắc cho AI, đã kiểm chứng. Cùng đề bài, cùng trợ lý, chỉ thêm một tệp hướng dẫn bảo mật: bản có lỗi bảo mật giảm từ 31 trên 33 xuống 17 trên 34; số loại lỗi mỗi bản từ 5,85 xuống 2,82. Mã đăng nhập giờ được mã hóa bằng khóa trong két bảo mật của máy. Quy tắc nào cũng phải là việc AI làm xong được: “Ship a Baseline Profile” làm cả 3 bản codex hỏng build. Vibe-code: đặt CLAUDE.md hay AGENTS.md ở gốc dự án. Kỹ sư: mỗi quy tắc một hành vi kiểm tra được.

**Thao tác.** Chỉ vào khung mã SessionStore.kt khi nói “mã hóa bằng khóa trong két bảo mật”.

**Lưu ý.** Tệp dùng lại được: guardrail/ANDROID-GUARDRAIL.md. Số kiểm định (C1, C2) ở backup D5. Nếu được hỏi: tệp giảm số loại lỗi, không thay được bước kiểm tra (backup D5, D8).

## 11. 4 · Khi gặp lỗi (6:35 đến 7:25) — slide 11

*Giải pháp · 50 giây · 112 từ*

**Lời nói.** Mắt xích bốn: khi gặp lỗi. Bị lỗi chặn, AI đôi khi gỡ bớt bảo mật cho hết lỗi: lỗi “User not authenticated” biến mất vì khóa giữ mã đăng nhập không còn đòi xác thực, true đổi thành false. Trong 148 lần sửa, 17 lần gỡ bảo mật, khoảng 1 trong 9; thấp hơn ngưỡng 15% đã đặt trước nhưng chưa loại trừ được, nên chưa đủ để kết luận. Cùng lỗi, lần khác, nó thêm màn hình vân tay và giữ nguyên khóa. Vibe-code: thêm “Không được nới lỏng bảo mật để sửa lỗi này”, đọc dòng bị xóa. Kỹ sư: test khóa cứng thuộc tính bảo mật, và CODEOWNERS.

**Thao tác.** Chỉ vào hai dòng diff: dòng bị xóa (true) và dòng thêm vào (false).

**Lưu ý.** Theo bộ công cụ: agy 12/48, codex 5/50, claude 0/50, nhưng không xếp hạng vì harness và mô hình bị lẫn (backup D7).

## 12. 5 · Đừng tin “đã sửa xong” (7:25 đến 8:15) — slide 12

*Giải pháp · 50 giây · 105 từ*

**Lời nói.** Mắt xích năm: kiểm tra trước khi tin. AI tìm ra đủ lỗi đã cài: 168 trên 168 lần chạy tìm đủ 14 lỗi khi được đưa sẵn danh mục. Nhưng nó tự nhận “đã sửa xong” cả khi mã không biên dịch được: 9 trên 12 bản do codex sửa không biên dịch được, vì thêm một plugin dự án không dùng được. Bộ chấm tự động không thấy gì sai. Vibe-code: tự build, tự mở app, tự bấm đúng chỗ vừa sửa. Kỹ sư: build, test, bộ dò chạy trong CI; trợ lý không bao giờ tự xác nhận mã của chính nó.

**Lưu ý.** Phần tìm lỗi dễ vì đã đưa sẵn danh mục; slide chỉ mô tả, không có kiểm định xác nhận. Người phán là bộ kiểm tra độc lập, không phải lời của trợ lý.

## 13. 5 · Công cụ quét (8:15 đến 9:05) — slide 13

*Giải pháp · 50 giây · 105 từ*

**Lời nói.** Công cụ quét cũng cần người kiểm. MobSF, để mặc định, báo nhầm 7 trên 10 lần: ví dụ cảnh báo một Activity chỉ là màn hình xem trước của thư viện, không có trong bản phát hành. Và nó bỏ sót lỗi thật: app ghi toàn bộ nội dung gửi và nhận qua mạng vào nhật ký máy, bộ dò của chúng tôi bắt được. Cụ thể 103 phát hiện, 30 đúng, 73 sai. Vibe-code: cảnh báo đỏ chưa chắc là lỗi, “không có cảnh báo” chưa chắc là an toàn. Kỹ sư: dùng nhiều công cụ, để người phân loại từng cảnh báo.

**Lưu ý.** Con số 0,291 là đầu ra mặc định của MobSF trên các bản dựng này, không phải thuộc tính của ứng dụng. “7 trên 10” là cách nói tròn của 73/103.

## 14. 6 · Danh sách và ảnh (9:05 đến 9:45) — slide 14

*Giải pháp · 40 giây · 91 từ*

**Lời nói.** Mắt xích sáu: sửa kỹ thuật. Hai phiên bản của cùng một màn hình: bên trái là mã trợ lý thực sự sinh ra, bên phải là bản sửa. AI dựng sẵn cả 2.000 dòng dù màn hình chỉ thấy vài dòng: khung hình bị trễ từ 90% xuống 1,3%, chỉ bằng LazyColumn. Cùng kiểu lỗi ở ảnh bìa: 203 MB cho 15 ảnh, bản sửa 6,2 MB. Vibe-code: biết tên cách sửa để yêu cầu đúng, “dùng LazyColumn”. Kỹ sư: ưu tiên theo số đo, danh sách và ảnh trước.

**Thao tác.** Phát video P09c-A/B, bấm tạm dừng khi nói “90%”.

**Lưu ý.** Trung vị của ít nhất 12 lần lặp xen kẽ A và B. Đo trên máy ảo, chỉ so mã AI với bản sửa, không phải tốc độ điện thoại thật. Nếu hỏi về bộ kết xuất: backup D4.

## 15. 6 · Hiệu ứng trang trí (9:45 đến 10:20) — slide 15

*Giải pháp · 35 giây · 77 từ*

**Lời nói.** Hiệu ứng trang trí: bóng đổ, dải màu, hình vẽ trang trí làm mỗi khung hình chậm hơn 28%; thời gian vẽ 62,87 ms với mã AI, 49,07 ms với bìa phẳng. Ngưỡng “chậm hơn ít nhất 10%” được đặt trước khi đo, và kết quả vượt ngưỡng cả ở cận dưới. Vibe-code: kính mờ, bóng đổ để ở màn hình tĩnh, đừng đặt trong từng dòng. Kỹ sư: đo khung hình trước khi giữ hiệu ứng.

**Thao tác.** Phát video P08c-A/B.

**Lưu ý.** Phép kiểm định xác nhận C5: +28,13%, khoảng tin cậy 95% [13,38; 54,31], ngưỡng +10%, p Holm 0,000616 (backup D5). Bộ nhớ đồ họa không đo. Một thẻ sách từ một bản nháp: chi phí của một cấu trúc đã sinh ra, không phải của mọi hiệu ứng.

## 16. 6 · Thứ người dùng cảm thấy (10:20 đến 11:05) — slide 16

*Giải pháp · 45 giây · 97 từ*

**Lời nói.** Không phải tối ưu nào cũng là thứ người dùng cảm thấy. Tìm trong 100.000 sách nhanh gấp khoảng 7 lần, nhưng khung hình khi gõ không mượt hơn: khó thấy. R8 nén app nhỏ hơn 90,4%, từ 24,7 MB xuống 2,4 MB: thấy rõ. Baseline Profile mở app nhanh hơn 5,88%; con số khoảng 30% của Google là tốc độ chạy mã, không phải lúc mở app. Ba “lỗi kinh điển” của Compose: không đo được chi phí nào. Vibe-code: bật R8 cho bản phát hành. Kỹ sư: làm theo thứ tự tác động đo được.

**Thao tác.** Phát video P13c-A/B.

**Lưu ý.** Máy ảo, so tương đối. P13 dùng chỉ mục FTS4 thay LIKE (khác cách so khớp).

## 17. 7 · Đo bản phát hành (11:05 đến 11:50) — slide 17

*Giải pháp · 45 giây · 100 từ*

**Lời nói.** Mắt xích bảy: đo đúng cách. ShelfTap trên Xiaomi: bản debug làm app trông giật gấp nhiều lần. Thời gian vẽ khung hình khi mở trang sách là 125 ms ở bản debug, và 12 đến 16 ms ở bản đo đúng cách. Trên bản debug, Baseline Profile trông như giảm khung giật từ 12,7% xuống 4,8%; trên bản đo đúng cách, có và không có nó đều quanh 1,5%. Vibe-code: thử trên điện thoại thật, bản cài như người dùng nhận. Kỹ sư: đo bản phát hành, xen kẽ A và B, báo khoảng chứ không báo một số.

**Lưu ý.** Bài học tối ưu mạnh nhất của phần app thật: đo trên bản debug là đo nhầm đối tượng. Chưa đo khởi động nguội.

## 18. 7 · Lỗi chỉ máy thật mới có (11:50 đến 12:25) — slide 18

*Giải pháp · 35 giây · 77 từ*

**Lời nói.** Một lỗi chỉ máy thật mới có. Trang chủ giật vì mỗi cuốn sách cố tải một ảnh bìa không tồn tại: địa chỉ 10.0.2.2 chỉ máy ảo mới tới được. Trên điện thoại thật mọi ảnh bìa đều lỗi, và app gọi lại mãi. Bản sửa: nhớ ảnh đã lỗi, không gọi lại. Khung hình giật khi cuộn giảm từ 7,0 đến 7,2% xuống 1,5 đến 2,7%, đo trên bản debug; máy giá rẻ chưa đo.

**Lưu ý.** Khoảng giá trị là min–max của các lượt, không phải khoảng tin cậy. Tắt hẳn ảnh bìa: p90 34–42 ms, nên ảnh bìa lỗi gây ra phần lớn, nhưng không phải toàn bộ, độ giật.

## 19. 7 · Chữ to (12:25 đến 13:00) — slide 19

*Giải pháp · 35 giây · 79 từ*

**Lời nói.** Thử như người dùng thật: chữ to. Phóng cỡ chữ hệ thống lên 2.0, cả bốn app “đẹp” ở đầu bài đều vỡ. ShelfTap sửa được: màn vỡ bố cục từ 5 trên 7 xuống 0 trên 7. Vibe-code: trước khi khoe ảnh, đặt cỡ chữ lớn nhất, bật chế độ tối, cuộn nhanh trên máy thật. Kỹ sư: đưa cỡ chữ 2.0, màn hẹp 320 dp và chế độ tối vào bài test ảnh chụp chạy lại được.

**Thao tác.** Chỉ ảnh “Trước” và “Sau” của ShelfTap khi nói “sửa được”.

**Lưu ý.** Vỡ bố cục phát hiện qua ảnh chụp; bài test chạy lại được trên emulator (light, dark, font 2.0, rộng 320 dp).

## 20. Tóm tắt (13:00 đến 13:25) — slide 20

*Tóm tắt · 25 giây · 51 từ*

**Lời nói.** Tóm tắt, đọc theo hàng: mỗi mắt xích một việc cho mỗi người. Cột trái, người vibe-code: thói quen và câu lệnh, không cần biết sâu kỹ thuật. Cột phải, kỹ sư: quy trình của nhóm. Nếu chỉ nhớ một dòng: tự build, tự bấm thử, rồi đo trên máy thật.

**Thao tác.** Không đọc từng ô; lướt tay theo bảy hàng.

## 21. Lời kết (13:25 đến 14:05) — slide 21

*Lời kết · 40 giây · 88 từ*

**Lời nói.** Câu lệnh một lần cho bạn một ảnh đẹp. Bảy mắt xích cho bạn một ứng dụng dùng được. Nếu chỉ nhớ ba điều: viết đề bài như một bản yêu cầu, kèm tệp quy tắc; tự kiểm tra bằng build, công cụ độc lập và đọc từng dòng diff chạm bảo mật; đo bản phát hành trên máy thật, xen kẽ A và B. Không để trợ lý tự xác nhận mã của chính nó. Xin cảm ơn, mời thầy cô và các bạn đặt câu hỏi.

**Thao tác.** Giữ slide 21 trên màn hình khi hỏi đáp; mở backup bằng phím O (lưới) hoặc gõ số slide rồi Enter.

---

## Hỏi đáp dự kiến (mở slide dự phòng tương ứng)

| Câu hỏi có thể gặp | Trả lời (chỉ dùng số trong bộ slide) | Mở |
|---|---|---|
| Bốn trên năm giả thuyết được xác nhận là những cái nào? | Trong năm phép kiểm định xác nhận, bốn được ủng hộ sau hiệu chỉnh Holm: C1, C2, C3 và C5. C4 chưa đủ bằng chứng: 17/148 là 0,115, thấp hơn ngưỡng 0,15 đặt trước nhưng chưa loại trừ được. “Được ủng hộ” nghĩa là điều kiện thống kê đã đăng ký thỏa; nguyên nhân vẫn chỉ là giả thuyết. | D1, D5 |
| AI có tệ hơn người viết không? | Bản nháp AI có 2,37 cảnh báo bảo mật trên 1.000 dòng mã (trung vị, 66 bản), người viết là 1,02 (20 app có commit đầu trước 30/11/2022). Chúng tôi nói trợ lý phát ra với tỷ lệ này so với người; không nói AI gây ra. App người viết là app lớn, bảo trì nhiều năm, và đây là số cảnh báo của công cụ, không phải số lỗ hổng bị khai thác. | D2 |
| Chỉ 42 trên 100 bản chạy được, mẫu có đủ không? | Mỗi nhóm 33 đến 34 bản nháp (dự kiến 36), một loại ứng dụng, ba trợ lý, nên không xếp hạng trợ lý. Biên dịch được: claude-haiku 13/36, codex-luna 10/36, agy-g38flash 19/28. E6 mỗi ô chỉ 3 lần chạy, nên là số liệu để mô tả. | D3, D4, D9 |
| Kết quả có đúng trên điện thoại thật không? | Máy ảo cho chiều hướng A/B; đổi cách máy ảo vẽ hình, cùng một thay đổi còn −32,3% thay vì −78,2%. ShelfTap trên Xiaomi cho số thật nhưng chưa phải máy giá rẻ. Mục tiêu môn học: tìm ngoại tuyến 100k sách dưới 50 ms (lab: chỉ B đạt, 37 ms, emulator); cuộn 60 fps (Xiaomi đã đo, máy yếu chưa); bộ nhớ dưới 150 MB và chạm tự mượn dưới 1 s chưa đo. | D4 |
| So bản yêu cầu với câu lệnh một lần có công bằng không? | Bản yêu cầu chứa nhiều thông tin hơn, nên đây là so “đề bài kỹ thuật” với “đề bài quảng cáo”, không phải thử một yếu tố. E6 là thí nghiệm khám phá, kế hoạch ghi trước lần chạy đầu, mỗi ô 3 lần chạy. Trung vị khung giật: 5 app cuộn được với câu lệnh một lần, 4 với bản yêu cầu. | slide 9, D9 |
| Ảnh ở slide 3 chọn thế nào? | Theo quy tắc ghi trước khi chạy: khung giật thấp nhất trong các lần mở được của mỗi trợ lý, không chọn theo mắt. 36 lần chạy là 4 trợ lý × 3 kiểu câu lệnh × 3 lần; slide 3 chỉ là kiểu câu lệnh một lần (12 lần). | slide 3, D9 |
| Tệp quy tắc có làm app hết lỗi bảo mật không? | Không. Trên các app build được, tỷ lệ có ít nhất một phát hiện không đổi (C1 +0,000), còn số loại lỗi vẫn giảm (C2 +3,829). Các luật bị cố ý bỏ khỏi tệp cũng giảm (+1,10 và +0,71 phát hiện mỗi lần), nên chúng tôi không khẳng định cơ chế. Ví dụ S06 allowBackup: 29/33, 24/33, 10/34. | slide 10, D5, D8 |
| Sao phân loại E4 bằng AI, có đáng tin không? | Hai bên chấm là tác tử AI, mù bộ công cụ và biến thể prompt, kappa 0,909 trên 134 diff. Làm yếu bảo mật tập trung ở hai ca: Keystore 12/30, cleartext HTTP 5/30; TLS tự ký, luồng chính, R8 là 0/88. Theo bộ công cụ: agy 12/48, codex 5/50, claude 0/50, nhưng không xếp hạng vì harness và mô hình bị lẫn. Đếm nghiêm ngặt: 26/148 = 17,6%. | slide 11, D7 |
| MobSF độ chính xác 0,291 có nghĩa là MobSF kém? | Không. 0,291 là đầu ra mặc định của MobSF trên các bản dựng này, không phải thuộc tính của ứng dụng; tùy cách đọc nhãn, từ 0,244 đến 0,806. 30 phát hiện đúng đều ở manifest, build, chữ ký. Đồng thuận với bộ dò của chúng tôi kappa 0,581: bổ sung, không thay nhau. | slide 13 |
| Baseline Profile: ~30% của Google hay 5,88%? | ~30% của Google là tốc độ chạy mã, không phải lúc mở app. Chúng tôi đo thời gian đến khung hình đầu: 462 xuống 434,8 ms, −5,88% [−9,87; −2,67]. Trên máy thật, bản đo đúng cách có và không có quy tắc của app là 1,44–1,49% so với 1,45–1,60% khung giật: không có khác biệt đo được. | slide 16, slide 17, D6 |
| Thẻ lưới là −22% hay +28%? | Cùng một cặp trung vị, viết hai cách: C5 theo (A − B)/B là +28,13%; bảng E2 theo (B − A)/A là −22%. Kiểm định: khoảng tin cậy 95% [13,38; 54,31], ngưỡng +10%, p Holm 0,000616. | slide 15, D5, D6 |
| Cấu hình AI của máy thí nghiệm có lọt vào kết quả không? | Mỗi lần sinh mã chạy trong thư mục nhà giả chỉ có tệp đăng nhập; quét 623 bản ghi, không lần sinh mã nào nhận quy tắc, kỹ năng hay plugin riêng của chúng tôi; đối chứng với thư mục nhà thật lộ ngay, lượt đầu 18.244 token so với khoảng 7.500. Ghi rõ: một tệp cấu hình hệ thống nạp ba plugin đo đạc vào mọi lần chạy Claude; đọc mã thì chúng không thêm chữ nào vào hội thoại, chưa thử A/B không có chúng. Một lần chạy E4 liệt kê tên tệp thư mục nhà, không mở tệp nào. | slide 5, D10 |
| AI có bịa ra thư viện không tồn tại không? | Không thấy: 0/2105 tọa độ thư viện không tồn tại, cận trên 0,14%. Lần quét đầu báo 22 phiên bản “ảo giác” do tra cứu dùng plugin marker KSP cũ; sửa tra cứu xong còn 1, không kết luận nào đổi. Về trợ năng, 1/66 thiếu mô tả (U01): hiếm. | D1 |
| “AI tìm đủ lỗi” có nghĩa là AI tự tìm ra lỗi chưa biết không? | Không. Mức advice cho thấy việc dễ khi danh mục đã được đưa, không cho thấy công cụ tìm được cái chưa cài sẵn. Ứng dụng đích có 8 tệp Kotlin, 14 vấn đề cài sẵn, 3 vấn đề thật không gieo và 3 mồi. Người phán là bộ kiểm tra độc lập, không phải lời của trợ lý. | slide 12 |
| Vì sao bản debug giật hơn nhiều? | Debug: Compose chưa tối ưu, không R8, có gỡ lỗi. Thời gian vẽ khung ở bốn đoạn: 125, 85, 61, 53 ms; bản đo đúng cách 12–16 ms ở cả bốn đoạn. Chưa đo khởi động nguội. | slide 17 |
| Các nguồn của phần mở đầu là gì? | Karpathy, bài đăng trên X ngày 2/2/2025; Collins Word of the Year 2025; Stack Overflow Developer Survey 2025 (84% dùng hoặc sắp dùng, 46% không tin độ chính xác, 33% tin); Veracode (45% bài kiểm tra mã AI có lỗ hổng, hơn 100 mô hình); Merriam-Webster “slop”; Ouyang và cộng sự, arXiv:2308.02828. Trích nguyên văn và cách kiểm chứng: report/sources-leadin.md. | slide 2, slide 4, D11 |

## Chuẩn bị trước buổi trình bày
- [ ] Chạy thử bằng đồng hồ, từng đoạn; tổng 13 đến 15 phút. Nếu chậm hơn khoảng 1 phút: rút slide 15 và 18 còn một câu mỗi slide (hai slide này cộng 70 giây ở bảng trên).
- [ ] Chốt ai nói đoạn nào; đổi người ở ranh giới phần (bảng thời lượng).
- [ ] Ba video P09c, P08c, P13c (slide 14, 15, 16) mở được không cần mạng; ảnh slide 3 và slide 19 tải sẵn.
- [ ] Phím điều hướng: O mở lưới; gõ số slide rồi Enter. D1 là slide 22, D2 là 23, và cứ thế đến D11 là slide 32.
- [ ] Đọc lại slide 11 và slide 18: hai chỗ slide tự in giới hạn (“chưa đủ để kết luận”, “máy giá rẻ chưa đo”); nói đúng như chữ trên slide, một lần, không nhấn mạnh.
- [ ] Mở sẵn `report/sources-leadin.md` cho câu hỏi về nguồn phần mở đầu.

## Kiểm số liệu

Mỗi số trong các dòng **Lời nói**, **Thao tác**, **Lưu ý** và bảng Hỏi đáp phải có trong chữ của `presentation.html` (đã bỏ CSS, script, ảnh nhúng). Chạy trong thư mục `optimization/study`:

```
python3 -I - presentation.html presentation-script.md <<'PY'
import re,sys,html
def corpus(p):
    t=open(p,encoding='utf-8').read()
    t=re.sub(r'<style[\s\S]*?</style>','',t); t=re.sub(r'<script[\s\S]*?</script>','',t)
    t=re.sub(r'data:[^"\')]{50,}','',t); t=re.sub(r'<[^>]+>',' ',t)
    return html.unescape(t)
tok=re.compile(r'\d+(?:[.,]\d+)*')
html_t=corpus(sys.argv[1]); have=set(tok.findall(html_t))
sec=''; bad=[]; n=0
for ln,l in enumerate(open(sys.argv[2],encoding='utf-8'),1):
    if l.startswith('## '): sec=l; continue
    inseg = re.match(r'## \d+\.',sec)
    inqa = sec.startswith('## Hỏi đáp')
    chk = (inseg and l.startswith(('**Lời nói','**Thao tác','**Lưu ý'))) or (inqa and l.startswith('| ') and not l.startswith('| Câu hỏi'))
    if not chk: continue
    for t in tok.findall(l):
        n+=1
        if t not in have: bad.append((ln,t))
print('tokens checked:',n,'| not in presentation.html:',len(bad))
for b in bad: print('  line %d: %s'%b)
sys.exit(1 if bad else 0)
PY
```

Kết quả mong đợi: `tokens checked: N | not in presentation.html: 0`, thoát mã 0.
