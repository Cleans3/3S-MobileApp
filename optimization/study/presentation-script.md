# presentation-script.md — kịch bản trình bày 19:30 (27 slide chính + D1 đến D12 dự phòng)

Khớp nguyên vẹn với `presentation.html` (27 slide chính theo mạch năm phần: Mở đầu 1–3, Kết quả 4–10, Nguyên nhân 11–20, Giải pháp 21–26, Kết thúc 27; cộng 12 slide dự phòng D1 đến D12). Dàn ý và bằng chứng từng slide nằm trong `slides-outline.md`. Kịch bản viết theo văn phong thuyết trình học thuật tự nhiên của sinh viên (“nhóm em”, “thầy cô và các bạn”), câu ngắn gọn dễ nói trên sân khấu, không đọc lại nguyên văn chữ trên slide mà giải thích ý nghĩa và nối mượt sang slide kế tiếp.

**Quy tắc của bản này.**
- Mọi con số trong các dòng **Lời nói**, **Thao tác**, **Lưu ý** và bảng **Hỏi đáp dự kiến** đều khớp nguyên văn với chữ hiển thị hoặc ghi chú (`aside.notes`) của `presentation.html`, viết theo định dạng số Việt (dấu phẩy thập phân, dấu chấm phân tách hàng nghìn). Kiểm chứng tự động bằng đoạn lệnh ở mục **Kiểm số liệu** cuối tệp.
- Thời lượng từng slide bám đúng mốc `~N giây` trong ghi chú (`aside.notes`) của `presentation.html` (tổng 19:30), giữ độ dài lời nói quanh tốc độ 130 từ mỗi phút. Riêng slide 20 là phần demo trực tiếp dài 120 giây do một thành viên khác trong nhóm phụ trách; kịch bản chỉ ghi lời chuyển lượt và nhận lại lượt.
- Thuật ngữ thống nhất: dùng *prompt* (giải thích ở lần đầu: “prompt — tức lời nhắc giao cho AI”), *trợ lý lập trình AI* / *trợ lý AI*, *bản nháp đầu*, *chưa tối ưu*, *khung hình trễ (jank)*, *mức dùng RAM (RSS)*, *bản build*, *tệp quy tắc (guardrail)*, *bộ quét tĩnh của nhóm*, *được xác nhận* / *không được xác nhận*. Phần giải pháp tổ chức theo thang năm bậc kỹ thuật (Bậc 1 đến Bậc 5). Tránh khẳng định nhân quả vượt ngoài dữ liệu thực nghiệm.

## Bảng thời lượng

| Phần | Slide | Thời lượng | Từ |
|---|---|---|---|
| A · Mở đầu | 1–3 | 1:40 | 213 |
| B · Kết quả | 4–10 | 4:55 | 631 |
| C · Nguyên nhân (ba trụ cột và demo) | 11–20 | 8:35 | 911 |
| D · Giải pháp (năm bậc kỹ thuật) | 21–26 | 3:50 | 510 |
| E · Kết thúc | 27 | 0:30 | 77 |
| **Tổng** | **1–27** | **19:30** | **2.342** |

---

## 1. Trông hoàn thiện, chưa tối ưu (0:00 đến 0:15) — slide 1

*Mở đầu · 15 giây · 35 từ*

**Lời nói.** Kính chào thầy cô và các bạn. Trông hoàn thiện, nhưng chưa tối ưu — hôm nay nhóm em trình bày về tối ưu giao diện, bộ nhớ và bảo mật trên Android với ShelfTap.

**Thao tác.** Dừng một nhịp ngay sau vế “Trông hoàn thiện, nhưng chưa tối ưu”, để khán giả nhìn bốn màn hình ứng dụng rồi chuyển sang slide 2.

**Lưu ý.** Slide áp phích không hiện số trang. Bốn ảnh nền là bốn ứng dụng E6 (chi tiết ở D11); bối cảnh là ShelfTap, ứng dụng thư viện thông minh dùng NFC của nhóm.

## 2. Lời hứa trên mạng (0:15 đến 0:55) — slide 2

*Mở đầu · 40 giây · 84 từ*

**Lời nói.** Thầy cô và các bạn chắc hẳn đã quen với những bài đăng kiểu này: ảnh giao diện bóng bẩy kèm lời hứa chỉ một prompt — tức lời nhắc giao cho AI — là có ngay ứng dụng hoàn chỉnh. Hiện tượng đó gọi là vibe coding: mô tả bằng lời, nhận mã từ trợ lý lập trình AI rồi dùng ngay mà không kiểm tra lại. Nếu làm đúng theo cách đó trên Android thì kết quả thực tế sẽ ra sao?

**Thao tác.** Lướt tay qua lưới sáu ảnh minh họa, không đọc từng ảnh.

**Lưu ý.** Không nhắc tên nguồn hay đường dẫn ảnh trên sân khấu; nguồn lưu riêng trong evidence/img/hype/README.md.

## 3. Năm con số (0:55 đến 1:40) — slide 3

*Mở đầu · 45 giây · 94 từ*

**Lời nói.** Câu trả lời gói gọn trong năm con số nhóm em đã đo, và phần sau sẽ chứng minh từng con số. Một, 58/100 bản nháp đầu bị build lỗi. Hai, 60/66 bản có cảnh báo bảo mật, mật độ gấp 2,3 lần ứng dụng do người viết. Ba, danh sách 2.000 mục dựng toàn bộ làm 90% khung hình trễ. Bốn, hai lỗi ảnh bìa và danh sách đẩy RAM lên 180 và 243 MB, vượt mục tiêu 150 MB. Năm, 9/12 bản được codex báo “đã sửa xong” không biên dịch được.

**Thao tác.** Bấm hiện lần lượt năm dòng từ trên xuống (build, bảo mật, khung hình, bộ nhớ, lời “đã sửa xong”), không dừng giải thích sâu vì mỗi dòng đã có mốc slide chứng minh bên phải.

**Lưu ý.** Số khung hình và mức dùng RAM đo trên máy ảo, đối chiếu bản nháp AI với bản sửa trên cùng màn hình; các mốc chứng minh nằm ở slide 5, 17, 11, 14, 15 và 7.

## 4. Sáu thí nghiệm (1:40 đến 2:25) — slide 4

*Kết quả · 45 giây · 104 từ*

**Lời nói.** Bước vào phần kết quả, trước tiên là sáu thí nghiệm tạo ra các số liệu hôm nay, với tiêu chí đánh giá được chốt trước khi thu thập dữ liệu: E1 đo tần suất trên 101 bản nháp; E2 đối chứng A/B trên cùng màn hình; E3 quét 13 APK; E4 thử 148 lượt sửa lỗi chặn; E5 kiểm tra 14 lỗi cài sẵn; E6 chạy prompt kiểu trào lưu. Quét lại 623 bản ghi hội thoại, nhóm em xác nhận không lọt cấu hình riêng. Số đo máy ảo chỉ so tỷ lệ A/B, không phải tốc độ trên thiết bị thật.

**Thao tác.** Chỉ nhanh hai khung ở bên phải slide: Môi trường sạch và Máy ảo.

**Lưu ý.** Giới hạn máy ảo nói một lần ở đây; câu hỏi sâu về kiểm tra cô lập môi trường mở D7, về giới hạn mở D1.

## 5. Phân loại lỗi (2:25 đến 3:15) — slide 5

*Kết quả · 50 giây · 100 từ*

**Lời nói.** Đây là kết quả trung tâm của E1. Đọc từ trái sang phải là tần suất trên 66 bản nháp không kèm tệp quy tắc (guardrail): phổ biến từ 50% trở lên, ít gặp từ 10% đến dưới 50%, và hiếm dưới 10%; chấm đỏ là lỗi nghiêm trọng. Cột bên trái là lỗi hay gặp nhất như build lỗi 58/100 hay thiếu R8 63/66, nhưng phần lớn chấm đỏ lại rơi vào hai cột sau — lưu token dạng rõ 19/66, tìm kiếm thiếu chỉ mục 15/66, dựng toàn bộ danh sách 2/66 và ảnh kích thước gốc 5/66.

**Thao tác.** Chỉ từ trái sang phải qua ba cột Phổ biến, Ít gặp, Hiếm, rồi khoanh vào các chấm đỏ và dòng bối cảnh E4 phía dưới.

**Lưu ý.** P06 (5/66) chưa kiểm định độ chính xác; dòng khi gặp lỗi chặn (12/30 bỏ xác thực khóa Keystore, 5/30 bật cleartext) thuộc bối cảnh E4, không gộp vào ba cột E1. Nếu được hỏi ưu tiên sửa gì trước, mở D12.

## 6. Bản nháp build lỗi (3:15 đến 4:00) — slide 6

*Kết quả · 45 giây · 94 từ*

**Lời nói.** Nhìn kỹ vào lỗi phổ biến nhất vừa thấy: bản nháp đầu bị build lỗi. Ở E1, chỉ 42/100 bản build thành công. Hai yếu tố tạo chênh lệch rõ nhất là mô hình và bản đặc tả. Ở E6, dùng cùng công cụ và chỉ đổi mô hình, số bản build thành công tăng từ 1/9 với claude-haiku lên 7/9 với claude-sonnet. Thay prompt kiểu trào lưu bằng bản đặc tả có cấu trúc kéo tỷ lệ khung hình trễ từ 99% xuống 19,6% và lượng mã Kotlin từ 1.164 xuống 524 dòng.

**Thao tác.** Chỉ lần lượt 42/100, cặp 1/9 với 7/9, rồi cặp 99% xuống 19,6%.

**Lưu ý.** E6 là thí nghiệm khám phá với tối đa 3 lần chạy mỗi ô, không có kiểm định p; chi phí claude-haiku 0,11 USD so với claude-sonnet 0,48 USD. Chi tiết từng trợ lý AI ở D6.

## 7. “Đã sửa xong” chưa phải kết quả (4:00 đến 4:45) — slide 7

*Kết quả · 45 giây · 86 từ*

**Lời nói.** Một rủi ro khác là tin vào lời “đã sửa xong”. Ở E5, khi ở chế độ chỉ đề xuất mà không sửa mã, trợ lý AI tìm đủ 168/168 lượt lỗi cài sẵn. Nhưng khi tự sửa, codex tuyên bố đã xong trong khi chạy biên dịch thật thì 9/12 bản lỗi do tự thêm plugin baselineprofile không dùng được. Đồng thời 21/24 lượt làm đổi cả mẫu đối chứng vốn đúng. Rõ ràng lời trợ lý AI không thể thay kết quả kiểm thử.

**Thao tác.** Chỉ hộp lời trả lời của codex ở trên và dòng lỗi biên dịch thật bên dưới.

**Lưu ý.** Nhấn mạnh: biên dịch và kiểm thử độc lập mới là căn cứ xác nhận, không phải lời của trợ lý AI.

## 8. Thiết bị thật (4:45 đến 5:30) — slide 8

*Kết quả · 45 giây · 102 từ*

**Lời nói.** Trên thiết bị thật Xiaomi, ShelfTap cho thấy hai bài học máy ảo không lộ ra. Thứ nhất, đo bằng bản debug là đo sai đối tượng: mở trang sách ở bản debug lên tới 125 ms p90, còn bản build đo hiệu năng (cấu hình tương đương bản phát hành) chỉ 12 đến 16 ms, nằm gọn trong ngưỡng 16,67 ms của 60 fps. Thứ hai, địa chỉ ảnh bìa 10.0.2.2 chỉ truy cập được từ máy ảo, khiến thiết bị thật gọi lại liên tục; khi khắc phục, tỷ lệ khung hình trễ khi cuộn giảm từ 7,0–7,2% xuống 1,5–2,7%.

**Thao tác.** Chỉ bên trái (125 ms so với 12–16 ms) rồi sang bên phải (10.0.2.2).

**Lưu ý.** Số liệu trang chủ 7,0–7,2% xuống 1,5–2,7% đo trên bản debug; thiết bị cấu hình thấp xem giới hạn ở D1.

## 9. Kết quả các phép kiểm định (5:30 đến 6:00) — slide 9

*Kết quả · 30 giây · 69 từ*

**Lời nói.** Về thống kê, trong năm phép kiểm định đăng ký trước sau hiệu chỉnh Holm, bốn phép được xác nhận là C1, C2, C3 và C5 — về hiệu quả tệp quy tắc, mật độ cảnh báo bảo mật và chi phí hiệu ứng trang trí. C4 không được xác nhận vì 17/148 dưới 15%. Hai giả thuyết mô tả H4 (0/2.105) và H5 (1/66) đều bị bác bỏ.

**Thao tác.** Chỉ nhóm bốn hàng xanh phía trên, tách khỏi C4 và hai hàng H4, H5.

**Lưu ý.** “Được xác nhận” nghĩa là thỏa mãn điều kiện thống kê đã đăng ký; bảng hiệu chỉnh Holm đầy đủ ở D2.

## 10. Sáu kết luận (6:00 đến 6:35) — slide 10

*Kết quả · 35 giây · 76 từ*

**Lời nói.** Gom lại kết quả của sáu thí nghiệm: E1 chỉ 42/100 bản build thành công, tệp quy tắc giảm cảnh báo nhưng cũng giảm bản build thành công. E2: sửa danh sách giảm 69,9% khởi động, ba lỗi Compose không có chi phí đo được. E3: quét APK đạt 0,291. E4: 17/148 lượt nới lỏng bảo mật. E5: 9/12 bản codex sửa không biên dịch được. E6: bản đặc tả đưa khung hình trễ xuống 19,6%.

**Thao tác.** Lướt nhanh sáu hàng từ E1 đến E6 với nhịp dứt khoát rồi chuyển sang phần nguyên nhân.

**Lưu ý.** Không diễn giải lại chi tiết; dùng slide này khép phần kết quả và mở sang phần nguyên nhân phía sau (slide 11 đến 20).

## 11. Danh sách dài dựng toàn bộ (6:35 đến 7:20) — slide 11

*Nguyên nhân · Giao diện · 45 giây · 106 từ*

**Lời nói.** Vậy nguyên nhân đằng sau các con số vừa nêu nằm ở đâu? Nhóm em đi qua ba trụ cột: giao diện, bộ nhớ và bảo mật. Ở trụ cột giao diện, đây là cùng một màn hình danh sách 2.000 lượt mượn với cùng thao tác cuộn. Bên trái là mã giữ nguyên từ bản nháp AI dùng Column với forEach, dựng hết 2.000 mục dù màn hình chỉ hiện vài mục, khiến 90% khung hình bị trễ. Bên phải đổi sang LazyColumn: số mục dựng giảm từ 2.000 xuống 31, tỷ lệ khung hình trễ giảm từ 90% xuống 1,3%, đủ đạt 60 fps.

**Thao tác.** Phát video P09c-AB.mp4, dừng ngắn ở đoạn bên trái bị khựng khi cuộn.

**Lưu ý.** Trung vị từ ít nhất 12 lần lặp xen kẽ A/B trên máy ảo; số đầy đủ 90,05% xuống 1,255%, lỗi hiếm 2/66 nhưng nghiêm trọng.

## 12. Hiệu ứng trang trí (7:20 đến 8:05) — slide 12

*Nguyên nhân · Giao diện · 45 giây · 91 từ*

**Lời nói.** Lỗi thứ hai hay gặp khi dùng prompt kiểu trào lưu là dồn đổ bóng, chuyển sắc (gradient) và vẽ Canvas lên từng thẻ sách. Khi đổi bìa thành khối phẳng, thời gian xử lý khung hình p90 giảm từ 62,87 ms xuống 49,07 ms — mã AI chậm hơn 28%, qua đó xác nhận phép kiểm định C5. Tỷ lệ khung hình trễ giảm từ 70,52% xuống 48,65%, dù chưa đạt 60 fps trên máy ảo. Lỗi gặp ở 8/66 bản E1 nhưng vọt lên 11/12 lần chạy ở E6.

**Thao tác.** Phát video P08c-AB.mp4; nhấn cặp số 8/66 so với 11/12.

**Lưu ý.** P08 đo trên cấu hình kết xuất GPU swangle_indirect nên chỉ so trong nội bộ nhân tố này; bộ nhớ đồ họa chưa được đo.

## 13. Lỗi không có chi phí đo được (8:05 đến 8:50) — slide 13

*Nguyên nhân · Giao diện · 45 giây · 97 từ*

**Lời nói.** Ngược lại, không phải quy tắc nào cũng tạo khác biệt đo được. Trên biểu đồ, chấm lệch trái vạch 0 nghĩa là bản sửa nhanh hơn: sửa danh sách giảm 69,9% thời gian khởi động. Nhưng thêm key danh sách (P01) và ổn định tham số (P03, P04) đều có khoảng tin cậy cắt qua 0. Baseline Profile giảm 5,88% khởi động trên máy ảo, còn trên thiết bị thật ở bản build đo hiệu năng (cấu hình tương đương bản phát hành) thì không có khác biệt đo được. Hãy đo trước khi tối ưu.

**Thao tác.** Chỉ dọc theo vạch 0 từ trên xuống dưới để đối chiếu hai dòng trên và hai dòng dưới.

**Lưu ý.** Khoảng tin cậy cắt qua 0 nghĩa là chưa ghi nhận chi phí trong kịch bản này (danh sách tĩnh, strong skipping mặc định), không phải luôn miễn phí.

## 14. Ảnh giải mã ở kích thước gốc (8:50 đến 9:30) — slide 14

*Nguyên nhân · Bộ nhớ · 40 giây · 83 từ*

**Lời nói.** Sang trụ cột thứ hai: bộ nhớ. Với 15 ảnh bìa, bản nháp AI giải mã ở kích thước gốc 1500×2250 cho từng ô nhỏ, không thu nhỏ và không dùng bộ nhớ đệm. Riêng ảnh chiếm 203 MB, đẩy RAM cao nhất lên 180 MB — vượt ngưỡng dưới 150 MB. Khi đổi sang Coil AsyncImage để giải mã đúng kích thước ô và có bộ nhớ đệm, dung lượng ảnh còn 6,2 MB và mức dùng RAM giảm xuống 60 MB.

**Thao tác.** Phát video P06c-AB.mp4, chỉ đường mốc mục tiêu dưới 150 MB.

**Lưu ý.** Lỗi hiếm (5/66, chưa kiểm định độ chính xác) nhưng nghiêm trọng vì chỉ riêng lỗi này đã vượt ngưỡng 150 MB.

## 15. Danh sách dài và bộ nhớ (9:30 đến 10:10) — slide 15

*Nguyên nhân · Bộ nhớ · 40 giây · 85 từ*

**Lời nói.** Nhóm em nhìn lại danh sách 2.000 lượt mượn ở slide 11 dưới góc độ bộ nhớ. Vì bản nháp AI dựng đủ 2.000 mục trước khung hình đầu tiên, mức dùng RAM vọt lên 243 MB, Java heap 220 MB, thời gian khởi động mất 1.901 ms. Khi chuyển sang LazyColumn, mức dùng RAM giảm xuống 43 MB, Java heap còn 7,6 MB và thời gian khởi động giảm xuống 573,1 ms. Một lỗi danh sách làm trượt cả khung hình lẫn bộ nhớ.

**Thao tác.** Chỉ ba cụm chỉ số: RSS 243 xuống 43 MB, Java heap 220 xuống 7,6 MB, TTID 1.901 xuống 573,1 ms.

**Lưu ý.** Số MB ít phụ thuộc máy ảo hơn số ms; bản nháp AI vượt ngưỡng 150 MB còn bản sửa đạt.

## 16. Dung lượng và tìm kiếm (10:10 đến 10:55) — slide 16

*Nguyên nhân · Bộ nhớ · 45 giây · 91 từ*

**Lời nói.** Tiếp theo là dung lượng và tìm kiếm. Bật R8 thu nhỏ APK tới 90%, từ 24,7 MB còn 2,4 MB, và giảm thêm 8,7% thời gian khởi động. Với tìm kiếm ngoại tuyến 100.000 tựa sách, truy vấn LIKE chậm nhất mất 96,63 ms, vượt 50 ms; đổi sang chỉ mục FTS4 đưa mức chậm nhất xuống 37 ms và tổng thời gian truy vấn giảm từ 634,9 ms xuống 85,42 ms. Tuy nhiên, khung hình khi gõ không cải thiện vì nút thắt nằm ở danh sách kết quả.

**Thao tác.** Chỉ cột trái R8 rồi chuyển sang cột phải FTS4 và mốc 50 ms.

**Lưu ý.** LIKE khớp chuỗi con còn FTS4 khớp tiền tố từ nên khác tập kết quả ở 4/12 tiền tố.

## 17. Lỗi bảo mật phổ biến đến mức nào (10:55 đến 11:40) — slide 17

*Nguyên nhân · Bảo mật · 45 giây · 100 từ*

**Lời nói.** Sang trụ cột thứ ba: bảo mật — nơi bản nháp AI khác biệt rõ nhất so với ứng dụng do người viết. Trong 66 bản nháp không kèm tệp quy tắc ở E1, có tới 60 bản chứa ít nhất một cảnh báo bảo mật. Trên 1.000 dòng mã, bản nháp AI có trung vị 2,37 cảnh báo, gấp 2,3 lần mức 1,02 ở ứng dụng do người viết trước năm 2022; phép kiểm định C3 xác nhận mức chênh này. Phía dưới là đoạn mã nguyên văn: token đăng nhập được ghi thẳng ở dạng rõ vào SharedPreferences.

**Thao tác.** Chỉ hai dòng `putString` trong khung mã `SessionStore.kt` ở phần dưới slide.

**Lưu ý.** Nói trung lập: bản nháp AI có mật độ cảnh báo bảo mật cao hơn, không nói AI gây ra; đối chứng gồm 20 ứng dụng trước 30/11/2022.

## 18. Điều gì làm giảm lỗi bảo mật (11:40 đến 12:25) — slide 18

*Nguyên nhân · Bảo mật · 45 giây · 100 từ*

**Lời nói.** Vậy cách nào làm giảm cảnh báo bảo mật lúc sinh mã? Ở E1, prompt chức năng chi tiết vẫn có 29/33 bản bị cảnh báo so với 31/33 ở prompt ngắn. Tệp quy tắc kéo tỷ lệ này xuống 17/34 và giảm số loại vấn đề từ 5,85 xuống 2,82, xác nhận C1, C2. Nhưng đánh đổi là bản build thành công sau tối đa 3 lần sửa cũng giảm từ 20/33 xuống 10/34, còn prompt chi tiết là 12/33. Xét riêng 42 bản build thành công, tỷ lệ có cảnh báo không đổi: vẫn cần bước kiểm tra.

**Thao tác.** Chỉ hai cụm biểu đồ bên trái (cảnh báo bảo mật và bản build thành công) rồi sang hai khung bên phải.

**Lưu ý.** Tỷ lệ build thành công (20/33, 12/33, 10/34) là số mô tả; C2 tính trên cả bốn nhóm quy tắc; trong 42 bản build thành công, chênh lệch C1 bằng +0,000 còn C2 vẫn còn (+3,829).

## 19. Khi gặp lỗi chặn (12:25 đến 13:10) — slide 19

*Nguyên nhân · Bảo mật · 45 giây · 93 từ*

**Lời nói.** Rủi ro bảo mật còn lộ ra khi sửa lỗi chặn. Ở E4, gặp UserNotAuthenticatedException của Android Keystore (kho khóa của hệ thống), trợ lý AI có thể xóa yêu cầu xác thực — đổi true thành false — để lỗi biến mất. Trên 148 lượt sửa có 17 lượt nới lỏng cơ chế bảo mật, dưới ngưỡng 15% nên C4 không được xác nhận; tất cả rơi vào Keystore 12/30 và cleartext 5/30. Ở E3, công cụ quét APK đạt độ chính xác 0,291 và không phát hiện lỗi trong mã nguồn.

**Thao tác.** Chỉ dòng đỏ bị xóa và dòng xanh thêm vào trong diff `TokenVault.kt`.

**Lưu ý.** Không xếp hạng trợ lý AI theo 12/48, 5/50, 0/50 vì công cụ và mô hình bị lẫn (mở D4 nếu được hỏi).

## 20. Demo: trước và sau tối ưu (13:10 đến 15:10) — slide 20

*Nguyên nhân · Demo trực tiếp · 120 giây (chuyển và nhận lại lượt: ~25 giây, 65 từ; phần demo trực tiếp: ~95 giây)*

**Lời nói.** *[Chuyển lượt]* Để thầy cô và các bạn quan sát trực tiếp chứ không chỉ nhìn bảng số, thành viên nhóm em sẽ chạy demo song song bản nháp AI và bản tối ưu trên máy ảo. Xin mời bạn. *[Nhận lại lượt]* Cảm ơn phần demo. Sự khác biệt khi cuộn và tìm kiếm thể hiện rất rõ; vậy làm cách nào ngăn và xử lý những lỗi này?

**Thao tác.** Chuyển micro cho thành viên demo (thứ tự gợi ý: P09 danh sách 2.000 lượt mượn, P06 ảnh bìa, P08 thẻ trang trí, P13 tìm kiếm 100.000 tựa sách); nếu máy ảo lỗi, mở lại video ở slide 11, 12, 14.

**Lưu ý.** Số đo trên máy ảo laptop chỉ để minh họa; số tham chiếu của nghiên cứu là bốn mốc trên slide (90% → 1,3%; 180 → 60 MB; 62,87 → 49,07 ms; 96,63 → 37 ms). Chi tiết chạy demo ở optimization/study/demo/README.md.

## 21. Năm bậc giải pháp (15:10 đến 15:40) — slide 21

*Giải pháp · 30 giây · 63 từ*

**Lời nói.** Chuyển sang phần giải pháp. Nhóm em xếp thành năm bậc từ đơn giản nhất đến chuyên sâu nhất về kỹ thuật: nhờ trợ lý AI tự rà soát, viết quy tắc và bản đặc tả trước, để công cụ chấm, đo trên bản build đo hiệu năng, và sửa đúng mẫu lỗi trong mã nguồn. Bậc trước dừng ở đâu, bậc sau tiếp tục từ đó.

**Thao tác.** Bấm hiện lần lượt năm bậc từ trái sang phải dọc theo trục “đơn giản” đến “chuyên sâu về kỹ thuật”.

**Lưu ý.** Năm mốc số trên từng bậc đều đã xuất hiện ở phần trước: 168/168 (slide 7), 99% → 19,6% (slide 6), APK 24,7 → 2,4 MB (slide 16), p90 125 so với 12–16 ms (slide 8), RAM 243 → 43 MB (slide 15).

## 22. Bậc 1 · Trợ lý AI tự rà soát (15:40 đến 16:20) — slide 22

*Giải pháp · 40 giây · 86 từ*

**Lời nói.** Bậc 1 đơn giản nhất: dùng một prompt nhờ trợ lý AI liệt kê rủi ro bảo mật và chỗ chưa tối ưu theo tệp và dòng, chưa sửa mã. Ở E5 chế độ chỉ đề xuất, cách này tìm đủ 168/168 lượt lỗi cài sẵn; kèm kết quả bộ quét tĩnh không tạo khác biệt đo được. Bậc 1 dừng khi tự sửa: 9/12 bản codex không biên dịch được, 21/24 lượt đổi mẫu đối chứng, và prompt đã nêu sẵn danh mục lỗi.

**Thao tác.** Bấm lần lượt ba cột Làm gì, Kết quả đo được và Dừng ở đâu, dừng ngắn ở câu nối sang Bậc 2.

**Lưu ý.** Prompt trên slide ghi nhãn PROMPT MẪU chỉ để minh họa, không phải nguyên văn prompt E5; độ chính xác báo cáo 0,82 đến 0,94; nhánh kèm kết quả bộ quét tĩnh có p từ 0,25 đến 1,0 trên 6 cặp.

## 23. Bậc 2 · Quy tắc và bản đặc tả (16:20 đến 17:00) — slide 23

*Giải pháp · 40 giây · 93 từ*

**Lời nói.** Sang bậc 2: đặt tệp quy tắc ở gốc dự án và dùng bản đặc tả có cấu trúc ghi rõ định nghĩa xong là build thành công. Tệp quy tắc kéo cảnh báo bảo mật từ 31/33 xuống 17/34 ở E1; bản đặc tả kéo tỷ lệ khung hình trễ từ 99% xuống 19,6% ở E6. Nhưng số bản build thành công cũng giảm từ 20/33 xuống 10/34, riêng 42 bản build thành công thì tỷ lệ cảnh báo không đổi, và quy tắc Baseline Profile làm cả 3 bản codex build lỗi.

**Thao tác.** Chỉ hai trích dẫn ở cột Làm gì rồi bấm sang hai cột kết quả và giới hạn bên phải.

**Lưu ý.** Hai trích dẫn trên slide là nguyên văn tiếng Anh từ tệp quy tắc và bản đặc tả của nhóm; tỷ lệ build thành công 20/33 → 10/34 là số mô tả; chỉ viết prompt dài hơn mà không kèm tệp quy tắc thì không cải thiện kết quả (29/33).

## 24. Bậc 3 · Để công cụ chấm (17:00 đến 17:40) — slide 24

*Giải pháp · 40 giây · 89 từ*

**Lời nói.** Bậc 3 để công cụ chấm thay cho lời trợ lý AI: bật R8 trong build.gradle.kts cho bản phát hành và chạy biên dịch thật sau mỗi lần sửa. Nhờ đó APK giảm từ 24,7 xuống 2,4 MB, còn biên dịch thật bắt ngay 9/12 bản codex sửa lỗi mà bộ kiểm tra tĩnh cho qua. Nhưng bậc 3 dừng ở đây: MobSF đạt độ chính xác 0,291 và cả 30 cảnh báo đúng đều là cấu hình; build thành công chưa nói ứng dụng nhanh hay tốn RAM.

**Thao tác.** Chỉ hai khung mã R8 và lệnh biên dịch, rồi chuyển qua cột Kết quả và Dừng ở đâu.

**Lưu ý.** R8 cũng giảm 90,4% dung lượng và 8,7% thời gian khởi động; token dạng rõ chỉ bộ quét tĩnh của nhóm ghi nhận; Android Lint không được dùng trong nghiên cứu này nên không nêu số.

## 25. Bậc 4 · Đo trước và sau (17:40 đến 18:20) — slide 25

*Giải pháp · 40 giây · 86 từ*

**Lời nói.** Lên bậc 4: đo bằng Macrobenchmark, khởi động nguội, chạy xen kẽ A và B 12 lần lặp trên bản build đo hiệu năng. Đo đúng đối tượng cho thấy khác biệt ngay: mở trang sách trên Xiaomi ở bản debug là 125 ms p90, bản build đo hiệu năng chỉ 12–16 ms; số đo cũng loại P01, P03, P04 vì không có chi phí đo được. Tuy nhiên đo không tự sửa mã, và Baseline Profile giảm 5,88% trên máy ảo nhưng không đổi trên thiết bị thật.

**Thao tác.** Chỉ ba yêu cầu đo ở cột trái, nhấn cặp 125 → 12–16 ms rồi nối sang Bậc 5.

**Lưu ý.** Số ms trên máy ảo chỉ dùng để so A với B; thiết bị thật của nghiên cứu là Xiaomi flagship, giới hạn đầy đủ ở D1.

## 26. Bậc 5 · Sửa đúng mẫu lỗi (18:20 đến 19:00) — slide 26

*Giải pháp · 40 giây · 93 từ*

**Lời nói.** Bậc 5 chuyên sâu nhất về kỹ thuật là sửa đúng từng mẫu lỗi đã biết, nơi tạo cải thiện lớn nhất. LazyColumn kéo danh sách 2.000 mục từ 90% xuống 1,3% khung hình trễ và RAM từ 243 xuống 43 MB. Coil AsyncImage kéo RAM ảnh bìa từ 180 xuống 60 MB. Bìa phẳng giảm p90 từ 62,87 xuống 49,07 ms; FTS4 đưa truy vấn chậm nhất từ 96,63 xuống 37 ms; Baseline Profile giảm 5,88%. Tuy vậy mỗi cách chỉ sửa đúng mẫu đó — lưới thẻ vẫn 48,65%, chưa đạt 60 fps.

**Thao tác.** Lướt nhanh bảng sáu hàng bên trái rồi dừng ở hộp giới hạn và gợi ý thăm dò D9 bên phải.

**Lưu ý.** Mọi số trong bảng là so A/B trên máy ảo; phần thăm dò ở D9 phóng dữ liệu mẫu lên quy mô thật để ngưỡng mục tiêu quyết định thay cho trợ lý AI (công cụ chặn diff mới đánh giá hồi cứu).

## 27. Ba điều cần nhớ (19:00 đến 19:30) — slide 27

*Kết thúc · 30 giây · 77 từ*

**Lời nói.** Khép lại bài, nhóm em chốt ba điểm cốt lõi cần nhớ. Một, trông hoàn thiện chưa phải đã tối ưu: hãy đo với dữ liệu thật. Hai, tần suất không phản ánh mức nghiêm trọng: cần kiểm tra riêng lỗi bộ nhớ và bảo mật. Ba, tin bản build và số đo, không tin lời “đã sửa xong”, đi đủ năm bậc giải pháp. Xin cảm ơn, mời thầy cô và các bạn đặt câu hỏi.

**Thao tác.** Giữ slide kết khi hỏi đáp; nhấn phím O để mở lưới chọn nhanh slide dự phòng D1 đến D12.

**Lưu ý.** Không đưa số mới ở slide kết. Các slide dự phòng D1 đến D12 nằm ngay sau slide kết (mục tiêu môn học ở D10, bốn ứng dụng chỉ một prompt ở D11, ưu tiên sửa lỗi ở D12).

---

## Hỏi đáp dự kiến (mở slide dự phòng tương ứng)

| Câu hỏi có thể gặp | Trả lời (chỉ dùng số trong bộ slide) | Mở |
|---|---|---|
| Số đo trên máy ảo có đại diện cho thiết bị thật không? | Số liệu E2 trên máy ảo chỉ dùng để đối chiếu tỷ lệ A/B giữa bản nháp AI và bản sửa, không phải tốc độ trên thiết bị thật. Khi đổi cấu hình kết xuất GPU của máy ảo, mức cải thiện p90 của cùng bản sửa danh sách còn −32,3% thay vì −78,2%. Nhóm em cũng ghi rõ các hạng mục chưa đo gồm thời gian chạm thẻ NFC tự mượn sách dưới 1 s, thiết bị cấu hình thấp, mức dùng RAM khi duyệt có ảnh bìa thật, bộ nhớ đồ họa và P12. | D1 |
| Chi tiết năm phép kiểm định và hiệu chỉnh Holm ra sao? | Năm phép kiểm định đăng ký trước được hiệu chỉnh Holm trên m = 5. Bốn phép kiểm định được xác nhận là C2 (p hiệu chỉnh 0,00005, chênh lệch +3,025), C1 (0,00016, +0,439), C3 (0,00027, δ Cliff +0,582) và C5 (0,000616, +28,13%). Riêng C4 có p hiệu chỉnh 0,3106 và tỷ lệ 0,115 thấp hơn ngưỡng 0,15 nên không được xác nhận. | D2 |
| Vì sao hiệu ứng trang trí khi thì ghi +28,13%, khi thì ghi −22%? | Cả hai cùng mô tả một cặp trung vị 62,87 ms và 49,07 ms: phép kiểm định C5 tính theo (A − B)/B là +28,13%, còn bảng E2 tính theo (B − A)/A là −22% với khoảng tin cậy 95% [−35,2; −11,8]. Bảng D3 liệt kê đầy đủ bảy nhân tố của E2 cùng đối chiếu riêng cho P01. | D3 |
| Trong E4, trợ lý AI nào nới lỏng cơ chế bảo mật nhiều hơn và cách chấm có khách quan không? | Hai người chấm độc lập được ẩn thông tin về trợ lý AI và biến thể prompt, đạt hệ số kappa 0,909 trên 134 bản diff. Toàn bộ lượt nới lỏng cơ chế bảo mật rơi vào Keystore 12/30 và cleartext 5/30; ba kịch bản còn lại là 0/88. Theo từng trợ lý AI: agy 12/48, codex 5/50, claude 0/50, nhưng nhóm em không xếp hạng vì công cụ và mô hình bị lẫn vào nhau. | D4 |
| Ứng dụng do người viết có mắc các lỗi tương tự không? | Có, thậm chí cao hơn ở nhiều quy tắc giao diện và hiệu năng: thiếu key danh sách P01 là 14/20 ở nhóm ứng dụng do người viết trước 2022, màu và kích thước gán cứng U04 là 19/20, thiếu mô tả biểu tượng U01 là 9/20. Vì vậy nhóm em không khẳng định mọi lỗi phổ biến đều riêng của AI; phép so sánh được kiểm định giữa bản nháp AI và ứng dụng do người viết là mật độ cảnh báo bảo mật C3 (2,37 so với 1,02). | D5, slide 17 |
| Các trợ lý AI khác nhau thế nào trong thí nghiệm E6? | Mỗi ô trong E6 có 3 lần chạy. Với chỉ một prompt, agy-g38flash build thành công 3/3 còn ba trợ lý còn lại chỉ đạt 1/3; nhưng khi chuyển sang tệp quy tắc và bản đặc tả, claude-sonnet đạt 3/3 ở cả hai, trong khi claude-haiku là 0/3. Riêng agy-g38flash ở bản đặc tả không sinh được bản nháp ở cả 3 lần do chế độ chạy không giao diện từ chối một lệnh trước khi ghi tệp. | D6 |
| Cấu hình AI cá nhân trên máy thí nghiệm có làm nhiễu kết quả không? | Mỗi lần sinh mã chạy trong thư mục HOME giả lập chỉ chứa tệp xác thực và bị khóa ghi ra ngoài. Quét lại 623 bản ghi hội thoại không thấy dấu vết cấu hình riêng; mẫu đối chứng chạy với thư mục HOME thật lộ ngay ở lượt đầu với 18.244 token so với khoảng 7.500. Nhóm em cũng công khai rằng cấu hình hệ thống tự nạp ba plugin đo đạc vào Claude; khi đọc mã nguồn không thấy chúng chèn chữ vào hội thoại nhưng chưa thử A/B khi tắt chúng. | D7 |
| Còn vấn đề cỡ chữ lớn 2.0 và trợ năng thì sao? | Nhóm em xếp cỡ chữ lớn và bố cục sang phần chất lượng giao diện ở D8 để bài chính tập trung vào ba trụ cột tối ưu. Ở tỷ lệ cỡ chữ 2.0, cả bốn ứng dụng sinh từ chỉ một prompt đều vỡ bố cục; trên ShelfTap, nhóm em giảm số màn hình vỡ bố cục từ 5/7 xuống 0/7 và cảnh báo ATF từ 4 xuống 2. | D8 |
| Tệp quy tắc có đảm bảo hết lỗi bảo mật khi ứng dụng build thành công không? | Không. Trên toàn bộ tập E1, tỷ lệ có cảnh báo bảo mật giảm từ 31/33 xuống 17/34, nhưng số bản build thành công sau tối đa 3 lần sửa cũng giảm từ 20/33 xuống 10/34 (prompt chi tiết là 12/33), và nếu chỉ xét 42 bản build thành công thì chênh lệch C1 bằng +0,000 trong khi số loại lỗi C2 vẫn giảm (+3,829). Vì vậy tệp quy tắc ở bậc 2 chỉ là lớp phòng ngừa khi sinh mã, vẫn cần kiểm tra bản build và quét bảo mật độc lập ở bậc 3. | slide 18, D2 |
| Vì sao Baseline Profile không còn tác dụng rõ trên thiết bị thật? | Trên máy ảo E2, Baseline Profile giảm thời gian khởi động từ 462 ms xuống 434,8 ms (−5,88%). Khi đo trên thiết bị thật Xiaomi bằng bản build đo hiệu năng, tỷ lệ khung hình trễ có và không có Baseline Profile lần lượt là 1,44–1,49% và 1,45–1,60%, không có khác biệt đo được. Điều này nhắc lại nguyên tắc đo đúng bản build ở bậc 4 và ưu tiên sửa danh sách, giải mã ảnh trước ở bậc 5. | slide 13, slide 8, D3 |
| Nhóm có đóng góp gì mới ngoài việc đo các lỗi đã biết? | Có một kết quả thăm dò, không thuộc các phép kiểm định đăng ký trước. Ở D11, các ứng dụng chỉ một prompt chỉ có 8 đến 12 tựa sách mẫu nên bộ nhớ trông ổn. Nhóm em viết công cụ tự động phóng dữ liệu mẫu đó lên 2.000 và 100.000 mục, rồi để ngưỡng mục tiêu quyết định đạt hay chưa; cả 10 bản phóng to vẫn build thành công. Ứng dụng duy nhất dựng danh sách toàn bộ tăng từ 107,0 MB lên 343,2 MB ngay ở 2.000 mục, vượt mục tiêu 150 MB, và treo ở 100.000 mục. Bốn ứng dụng còn lại vẫn dưới 150 MB, nhưng ở 100.000 mục không ứng dụng nào khởi động dưới 1.500 ms, ví dụ từ 1.168 lên 2.514 ms. Đây là số đo trên máy ảo, mỗi ô 5 lần, và danh sách phóng to được tạo trong bộ nhớ lúc khởi động nên một phần thời gian tăng thêm đến từ chính cách phóng to. | D9 |
| Các bản sửa có đạt mục tiêu môn học của ShelfTap không? | Trên màn hình thử nghiệm ở máy ảo, bản sửa đạt các mục tiêu đã đo: tìm ngoại tuyến 100.000 sách còn 37 ms (dưới 50 ms), mức dùng RAM khi duyệt còn 60 MB và 43 MB (dưới 150 MB), danh sách dài còn 1,3% khung hình trễ. Trên thiết bị thật Xiaomi ở bản build đo hiệu năng, mở trang sách đạt p90 8–16 ms và mức dùng RAM toàn ứng dụng khi cuộn danh mục chưa có ảnh bìa đạt 94–95 MB (PSS). Chỉ lưới thẻ trang trí trên máy ảo vẫn còn 48,65%, chưa đạt 60 fps; các hạng mục chưa đo nằm riêng ở D1. | D10, D1 |
| Bốn ứng dụng chỉ một prompt ở slide đầu cho kết quả đo cụ thể ra sao? | Bốn ảnh nền ở slide 1 là bốn ứng dụng sinh từ cùng một prompt kiểu trào lưu ở E6. Nhìn bề ngoài khá hoàn thiện nhưng chỉ 6/12 lần chạy build thành công; tỷ lệ khung hình trễ khi cuộn trên máy ảo nằm trong khoảng 74–100% (claude-haiku 89%, codex-luna 74%, agy-g38flash 100%, claude-sonnet 99%) và thời gian khởi động mất 1.195–2.576 ms. Mức dùng RAM giữ ở 66,8–102,8 MB chỉ vì các trợ lý AI tự tạo 12 tựa sách mẫu; khi phóng lên quy mô thật, vấn đề bộ nhớ lộ ra ở D9. | D11, D9 |
| Nếu không thể sửa hết mọi lỗi cùng lúc, nên ưu tiên thứ tự nào từ bảng phân loại? | Ba bài học từ bảng phân loại ở slide 5 cho thứ tự rõ ràng. Thứ nhất là bản build chạy được, vì 58/100 bản nháp E1 và 6/12 lần chạy E6 chỉ một prompt bị build lỗi. Thứ hai là bật R8 — thiếu ở 63/66 bản nhưng chỉ một dòng cấu hình đã thu nhỏ APK từ 24,7 MB xuống 2,4 MB. Thứ ba là chủ động rà các lỗi ít gặp hoặc hiếm nhưng nghiêm trọng như dựng toàn bộ danh sách 2/66, ảnh kích thước gốc 5/66, lưu token dạng rõ 19/66 và ghi log nhạy cảm 10/66. | D12, slide 5 |

## Chuẩn bị trước buổi trình bày
- [ ] Chạy thử bấm giờ toàn bài (gồm cả 120 giây demo trực tiếp ở slide 20); mục tiêu tổng thời lượng 15 đến 20 phút (khung chuẩn 19:30). Nếu cần rút gọn 1 phút, đọc gọn slide 9 và slide 10.
- [ ] Phân công rõ người thuyết trình chính và thành viên phụ trách demo trực tiếp ở slide 20; khớp trước câu chuyển lượt và câu nhận lại lượt.
- [ ] Kiểm tra ba video A/B chạy mượt không cần mạng: `evidence/video/P09c-AB.mp4` (slide 11), `P08c-AB.mp4` (slide 12), `P06c-AB.mp4` (slide 14); kiểm tra bốn ảnh nền ở slide 1 (và D11), sáu ảnh ở slide 2 và ảnh cỡ chữ 2.0 ở D8.
- [ ] Kiểm tra sẵn môi trường demo theo `optimization/study/demo/README.md` và sẵn sàng chuyển sang video A/B ở slide 11, 12, 14 nếu máy ảo gặp sự cố.
- [ ] Nhớ phím tắt điều hướng trong `presentation.html`: `P` (cửa sổ người trình bày, đồng hồ mục tiêu 19:30), `O` (lưới chọn slide), `N` (ghi chú), `T` (sáng/tối). Slide dự phòng D1 đến D12 tương ứng số thứ tự 28 đến 39.

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
