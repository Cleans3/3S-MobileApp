# presentation-script.md — kịch bản trình bày 19:15 (25 slide chính + D1 đến D8 dự phòng)

Khớp nguyên vẹn với `presentation.html` (25 slide chính + 8 slide dự phòng D1 đến D8). Dàn ý và bằng chứng từng slide nằm trong `slides-outline.md`. Kịch bản viết theo văn phong thuyết trình học thuật tự nhiên của sinh viên (“nhóm em”, “thầy cô và các bạn”), câu ngắn gọn dễ nói trên sân khấu, không đọc lại nguyên văn chữ trên slide mà giải thích ý nghĩa và nối mượt sang slide kế tiếp.

**Quy tắc của bản này.**
- Mọi con số trong các dòng **Lời nói**, **Thao tác**, **Lưu ý** và bảng **Hỏi đáp dự kiến** đều khớp nguyên văn với chữ hiển thị hoặc ghi chú (`aside.notes`) của `presentation.html`, viết theo định dạng số Việt (dấu phẩy thập phân, dấu chấm phân tách hàng nghìn). Kiểm chứng tự động bằng đoạn lệnh ở mục **Kiểm số liệu** cuối tệp.
- Tốc độ nói chuẩn: 130 từ mỗi phút (đếm theo khoảng trắng trong dòng **Lời nói**), làm tròn thời lượng từng slide về bội số 5 giây và giữ trong khoảng ±10 giây so với ghi chú của slide. Riêng slide 16 là phần demo trực tiếp dài 120 giây do một thành viên khác trong nhóm phụ trách; kịch bản chỉ ghi lời chuyển lượt và nhận lại lượt.
- Thuật ngữ thống nhất: dùng *prompt* (giải thích ở lần đầu: “prompt — tức lời nhắc giao cho AI”), *trợ lý lập trình AI* / *trợ lý AI*, *bản nháp đầu*, *chưa tối ưu*, *khung hình trễ (jank)*, *mức dùng RAM (RSS)*, *bản build*, *tệp quy tắc (guardrail)*, *bộ quét tĩnh của nhóm*, *được xác nhận* / *không được xác nhận*. Tránh khẳng định nhân quả vượt ngoài dữ liệu thực nghiệm.

## Bảng thời lượng

| Phần | Slide | Thời lượng | Từ |
|---|---|---|---|
| Mở đầu và bảng phân loại lỗi | 1–5 | 3:45 | 476 |
| Phương pháp thực nghiệm | 6 | 0:40 | 90 |
| Trụ cột 1: Hiệu năng hiển thị (E2) | 7–9 | 2:10 | 289 |
| Trụ cột 2: Bộ nhớ, dung lượng và tìm kiếm (E2) | 10–12 | 2:00 | 259 |
| Trụ cột 3: Bảo mật (E1, E3, E4) | 13–15 | 2:15 | 293 |
| Demo trực tiếp trước và sau tối ưu | 16 | 2:00 | 54 |
| Build và tự sửa lỗi (E1, E5, E6) | 17–18 | 1:20 | 175 |
| Kết luận và đối chiếu mục tiêu môn học | 19–21 | 2:10 | 282 |
| Thiết bị thật và khuyến nghị theo giai đoạn | 22–25 | 2:55 | 383 |
| **Tổng** | **1–25** | **19:15** | **2.301** |

---

## 1. Tối ưu giao diện, bộ nhớ và bảo mật (0:00 đến 0:30) — slide 1

*Mở đầu · 30 giây · 61 từ*

**Lời nói.** Kính chào thầy cô và các bạn. Hôm nay nhóm em trình bày về tối ưu giao diện, bộ nhớ và bảo mật trên Android với bối cảnh ShelfTap — ứng dụng thư viện NFC. Trợ lý lập trình AI viết bản nháp đầu rất nhanh, nhưng phần chưa tối ưu nằm ở đâu và gây tổn hao đến mức nào?

**Thao tác.** Dừng một nhịp sau câu hỏi rồi chuyển sang slide 2.

**Lưu ý.** Con số trên slide: 6 thí nghiệm, 101 bản nháp đầu, 36 lần chạy với chỉ một prompt (tức lời nhắc giao cho AI).

## 2. Lời hứa trên mạng (0:30 đến 1:10) — slide 2

*Mở đầu · 40 giây · 82 từ*

**Lời nói.** Thầy cô và các bạn chắc hẳn đã quen với những bài đăng kiểu này: ảnh giao diện bóng bẩy kèm lời hứa chỉ một prompt — tức lời nhắc giao cho AI — là có ngay ứng dụng hoàn chỉnh. Hiện tượng đó gọi là vibe coding: mô tả bằng lời, nhận mã từ trợ lý AI rồi dùng ngay mà không kiểm tra lại. Nếu làm đúng theo cách đó trên Android thì kết quả thực tế sẽ ra sao?

**Thao tác.** Lướt tay qua lưới sáu ảnh minh họa, không đọc từng ảnh.

**Lưu ý.** Không nhắc tên nguồn hay đường dẫn ảnh trên sân khấu; nguồn lưu riêng trong evidence/img/hype/README.md.

## 3. Chạy đúng prompt đó (1:10 đến 1:55) — slide 3

*Mở đầu · 45 giây · 98 từ*

**Lời nói.** Nhóm em đã thử chạy đúng kiểu prompt đó trên bốn trợ lý AI. Nhìn bề ngoài, cả bốn ứng dụng trông khá hoàn thiện. Nhưng khi đo trên máy ảo, chỉ có 6 trên 12 lần chạy build thành công. Tỷ lệ khung hình trễ khi cuộn lên tới 74 đến 100%, còn thời gian khởi động mất 1.195 đến 2.576 ms. Công bằng mà nói, mức dùng RAM chỉ từ 66,8 đến 102,8 MB vì trợ lý AI mới tự tạo 12 tựa sách mẫu; vấn đề bộ nhớ chỉ lộ ra ở dữ liệu thật.

**Thao tác.** Chỉ lần lượt bốn màn hình trên slide khi nhắc dải 74 đến 100%, rồi nhấn mạnh câu cuối về 12 tựa sách.

**Lưu ý.** Mỗi ảnh là lần chạy có tỷ lệ khung hình trễ thấp nhất trong 3 lần của từng trợ lý AI; số đo trên máy ảo chỉ so sánh trong thí nghiệm này.

## 4. Phân loại lỗi (1:55 đến 2:55) — slide 4

*Mở đầu · 60 giây · 129 từ*

**Lời nói.** Bảng này cho thấy bức tranh tổng thể về các lỗi trong bản nháp đầu. Thầy cô và các bạn có thể đọc nhanh theo hai chiều: từ trái sang phải là tần suất trên 66 bản nháp không kèm tệp quy tắc (guardrail), gồm phổ biến từ 50% trở lên, ít gặp từ 10% đến dưới 50%, và hiếm dưới 10%. Chấm đỏ đánh dấu lỗi nghiêm trọng. Điều đáng chú ý nằm ngay đây: cột bên trái là lỗi hay gặp nhất như build lỗi 58/100 hay thiếu R8 63/66, nhưng phần lớn chấm đỏ lại rơi vào hai cột sau — lưu token dạng rõ 19/66, tìm kiếm thiếu chỉ mục 15/66, dựng toàn bộ danh sách 2/66, giải mã ảnh ở kích thước gốc 5/66.

**Thao tác.** Chỉ từ trái sang phải qua ba cột Phổ biến, Ít gặp, Hiếm, rồi khoanh vào các chấm đỏ và dòng bối cảnh E4 phía dưới.

**Lưu ý.** P06 (5/66) chưa kiểm định độ chính xác; dòng khi gặp lỗi chặn (12/30 bỏ xác thực khóa Keystore, 5/30 bật cleartext) thuộc bối cảnh E4, không gộp vào ba cột E1.

## 5. Ba điều rút ra (2:55 đến 3:45) — slide 5

*Mở đầu · 50 giây · 106 từ*

**Lời nói.** Từ bảng phân loại, nhóm em rút ra ba điều. Trước khi nói đến tối ưu, lỗi phổ biến nhất là bản nháp build lỗi: 58/100 ở E1 và 6/12 ở E6. Thứ hai, lỗi tài nguyên hay gặp nhất lại dễ sửa nhất: 63/66 bản thiếu R8, bật R8 lên là APK giảm từ 24,7 MB xuống 2,4 MB. Thứ ba, lỗi nghiêm trọng nhất lại hiếm, cần chủ động rà soát. Công bằng mà nói, ứng dụng do người viết cũng mắc nhiều lỗi giao diện như thiếu key 14/20; chỉ mật độ cảnh báo bảo mật mới là khác biệt được kiểm định.

**Thao tác.** Đếm lần lượt ba thẻ lớn rồi dừng ngắn ở dòng lưu ý so với ứng dụng do người viết.

**Lưu ý.** Chuyển mượt sang slide 6: giới thiệu sáu thí nghiệm tạo ra các con số trên.

## 6. Sáu thí nghiệm (3:45 đến 4:25) — slide 6

*Phương pháp · 40 giây · 90 từ*

**Lời nói.** Số liệu hôm nay đến từ sáu thí nghiệm với tiêu chí chốt trước khi thu thập dữ liệu: E1 đo tần suất trên 101 bản nháp; E2 đối chứng A/B trên cùng màn hình; E3 quét 13 APK; E4 thử 148 lượt sửa lỗi chặn; E5 kiểm tra 14 lỗi cài sẵn; E6 chạy prompt kiểu trào lưu. Quét lại 623 bản ghi, nhóm em xác nhận không lọt cấu hình riêng. Số đo máy ảo chỉ so tỷ lệ A/B, không phải tốc độ trên thiết bị thật.

**Thao tác.** Chỉ nhanh hai khung ở đáy slide: Môi trường sạch và Máy ảo.

**Lưu ý.** Giới hạn máy ảo nói một lần ở đây; câu hỏi sâu về cô lập môi trường mở D7, về giới hạn mở D1.

## 7. Danh sách dài dựng toàn bộ (4:25 đến 5:10) — slide 7

*Trụ cột 1: Giao diện · 45 giây · 101 từ*

**Lời nói.** Đi vào trụ cột thứ nhất: hiệu năng hiển thị. Đây là cùng một màn hình danh sách 2.000 lượt mượn với cùng thao tác cuộn. Bên trái là mã giữ nguyên từ bản nháp AI dùng Column với forEach, dựng hết 2.000 mục dù màn hình chỉ hiện vài mục, khiến 90% khung hình bị trễ. Bên phải chỉ đổi sang LazyColumn: số mục được dựng giảm từ 2.000 xuống 31, tỷ lệ khung hình trễ giảm mạnh từ 90% xuống 1,3%. Chỉ thay đổi vài dòng mã nhưng lại quyết định màn hình có đạt 60 fps hay không.

**Thao tác.** Phát video P09c-AB.mp4, dừng ngắn ở đoạn bên trái bị khựng khi cuộn.

**Lưu ý.** Trung vị từ ít nhất 12 lần lặp xen kẽ A/B trên máy ảo; số đầy đủ 90,05% xuống 1,255%, lỗi hiếm 2/66 nhưng nghiêm trọng.

## 8. Hiệu ứng trang trí (5:10 đến 5:50) — slide 8

*Trụ cột 1: Giao diện · 40 giây · 91 từ*

**Lời nói.** Lỗi thứ hai hay gặp khi dùng prompt kiểu trào lưu là dồn đổ bóng, chuyển sắc (gradient) và vẽ Canvas lên từng thẻ sách. Khi đổi bìa thành khối phẳng, thời gian xử lý khung hình p90 giảm từ 62,87 ms xuống 49,07 ms — mã AI chậm hơn 28%, qua đó xác nhận phép kiểm định C5. Tỷ lệ khung hình trễ giảm từ 70,52% xuống 48,65%, dù chưa đạt 60 fps trên máy ảo. Lỗi gặp ở 8/66 bản E1 nhưng vọt lên 11/12 lần chạy ở E6.

**Thao tác.** Phát video P08c-AB.mp4; nhấn cặp số 8/66 so với 11/12.

**Lưu ý.** P08 đo trên cấu hình kết xuất GPU swangle_indirect nên chỉ so trong nội bộ nhân tố này; bộ nhớ đồ họa chưa đo.

## 9. Lỗi không có chi phí đo được (5:50 đến 6:35) — slide 9

*Trụ cột 1: Giao diện · 45 giây · 97 từ*

**Lời nói.** Ngược lại, không phải quy tắc nào cũng tạo khác biệt đo được. Trên biểu đồ, chấm lệch trái vạch 0 nghĩa là bản sửa nhanh hơn: sửa danh sách giảm 69,9% thời gian khởi động. Nhưng thêm key danh sách (P01) và ổn định tham số (P03, P04) đều có khoảng tin cậy cắt qua 0. Baseline Profile giảm 5,88% khởi động trên máy ảo, còn trên thiết bị thật ở bản build đo hiệu năng (cấu hình tương đương bản phát hành) thì không có khác biệt đo được. Hãy đo trước khi tối ưu.

**Thao tác.** Chỉ dọc theo vạch 0 từ trên xuống dưới để đối chiếu hai dòng trên và hai dòng dưới.

**Lưu ý.** Khoảng tin cậy cắt qua 0 nghĩa là chưa ghi nhận chi phí trong kịch bản này (danh sách tĩnh, strong skipping mặc định), không phải luôn miễn phí.

## 10. Ảnh giải mã ở kích thước gốc (6:35 đến 7:15) — slide 10

*Trụ cột 2: Bộ nhớ · 40 giây · 83 từ*

**Lời nói.** Sang trụ cột thứ hai: bộ nhớ. Với 15 ảnh bìa, bản nháp AI giải mã ở kích thước gốc 1500×2250 cho từng ô nhỏ, không thu nhỏ và không dùng bộ nhớ đệm. Riêng ảnh chiếm 203 MB, đẩy RAM cao nhất lên 180 MB — vượt ngưỡng dưới 150 MB. Khi đổi sang Coil AsyncImage để giải mã đúng kích thước ô và có bộ nhớ đệm, dung lượng ảnh còn 6,2 MB và mức dùng RAM giảm xuống 60 MB.

**Thao tác.** Phát video P06c-AB.mp4, chỉ đường mốc mục tiêu dưới 150 MB.

**Lưu ý.** Lỗi hiếm (5/66, chưa kiểm định độ chính xác) nhưng nghiêm trọng vì chỉ riêng lỗi này đã vượt ngưỡng 150 MB.

## 11. Danh sách dài và bộ nhớ (7:15 đến 7:55) — slide 11

*Trụ cột 2: Bộ nhớ · 40 giây · 85 từ*

**Lời nói.** Nhóm em nhìn lại danh sách 2.000 lượt mượn ở slide 7 dưới góc độ bộ nhớ. Vì bản nháp AI dựng đủ 2.000 mục trước khung hình đầu tiên, mức dùng RAM vọt lên 243 MB, Java heap 220 MB, thời gian khởi động mất 1.901 ms. Khi chuyển sang LazyColumn, mức dùng RAM giảm xuống 43 MB, Java heap còn 7,6 MB và thời gian khởi động giảm xuống 573,1 ms. Một lỗi danh sách làm trượt cả khung hình lẫn bộ nhớ.

**Thao tác.** Chỉ ba cụm chỉ số: RSS 243 xuống 43 MB, Java heap 220 xuống 7,6 MB, TTID 1.901 xuống 573,1 ms.

**Lưu ý.** Số MB ít phụ thuộc máy ảo hơn số ms; bản nháp AI vượt ngưỡng 150 MB còn bản sửa đạt.

## 12. Dung lượng và tìm kiếm (7:55 đến 8:35) — slide 12

*Trụ cột 2: Bộ nhớ · 40 giây · 91 từ*

**Lời nói.** Tiếp theo là dung lượng và tìm kiếm. Bật R8 thu nhỏ APK tới 90%, từ 24,7 MB còn 2,4 MB, và giảm thêm 8,7% thời gian khởi động. Với tìm kiếm ngoại tuyến 100.000 tựa sách, truy vấn LIKE chậm nhất mất 96,63 ms, vượt 50 ms; đổi sang chỉ mục FTS4 đưa mức chậm nhất xuống 37 ms và tổng thời gian truy vấn giảm từ 634,9 ms xuống 85,42 ms. Tuy nhiên, khung hình khi gõ không cải thiện vì nút thắt nằm ở danh sách kết quả.

**Thao tác.** Chỉ cột trái R8 rồi chuyển sang cột phải FTS4 và mốc 50 ms.

**Lưu ý.** LIKE khớp chuỗi con còn FTS4 khớp tiền tố từ nên khác tập kết quả ở 4/12 tiền tố.

## 13. Lỗi bảo mật phổ biến đến mức nào (8:35 đến 9:20) — slide 13

*Trụ cột 3: Bảo mật · 45 giây · 100 từ*

**Lời nói.** Sang trụ cột thứ ba: bảo mật — nơi bản nháp AI khác biệt rõ nhất so với ứng dụng do người viết. Trong 66 bản nháp không kèm tệp quy tắc ở E1, có tới 60 bản chứa ít nhất một cảnh báo bảo mật. Trên 1.000 dòng mã, bản nháp AI có trung vị 2,37 cảnh báo, gấp 2,3 lần mức 1,02 ở ứng dụng do người viết trước năm 2022; phép kiểm định C3 xác nhận mức chênh này. Phía dưới là đoạn mã nguyên văn: token đăng nhập được ghi thẳng ở dạng rõ vào SharedPreferences.

**Thao tác.** Chỉ hai dòng `putString` trong khung mã `SessionStore.kt` ở phần dưới slide.

**Lưu ý.** Nói trung lập: bản nháp AI có mật độ cảnh báo bảo mật cao hơn, không nói AI gây ra; đối chứng gồm 20 ứng dụng trước 30/11/2022.

## 14. Điều gì làm giảm lỗi bảo mật (9:20 đến 10:05) — slide 14

*Trụ cột 3: Bảo mật · 45 giây · 100 từ*

**Lời nói.** Vậy cách nào làm giảm cảnh báo bảo mật lúc sinh mã? Ở E1, prompt chức năng chi tiết vẫn có 29/33 bản bị cảnh báo so với 31/33 ở prompt ngắn. Tệp quy tắc kéo tỷ lệ này xuống 17/34 và giảm số loại vấn đề từ 5,85 xuống 2,82, xác nhận C1, C2. Nhưng đánh đổi là bản build thành công sau tối đa 3 lần sửa cũng giảm từ 20/33 xuống 10/34, còn prompt chi tiết là 12/33. Xét riêng 42 bản build thành công, tỷ lệ có cảnh báo không đổi: vẫn cần bước kiểm tra.

**Thao tác.** Chỉ hai cụm biểu đồ bên trái (cảnh báo bảo mật và bản build thành công) rồi sang hai khung bên phải.

**Lưu ý.** Tỷ lệ build thành công (20/33, 12/33, 10/34) là số mô tả; C2 tính trên cả bốn nhóm quy tắc; trong 42 bản build thành công, chênh lệch C1 bằng 0,000 còn C2 vẫn còn (+3,829).

## 15. Khi gặp lỗi chặn (10:05 đến 10:50) — slide 15

*Trụ cột 3: Bảo mật · 45 giây · 93 từ*

**Lời nói.** Rủi ro bảo mật còn lộ ra khi sửa lỗi chặn. Ở E4, gặp UserNotAuthenticatedException của Android Keystore (kho khóa của hệ thống), trợ lý AI có thể xóa yêu cầu xác thực — đổi true thành false — để lỗi biến mất. Trên 148 lượt sửa có 17 lượt nới lỏng cơ chế bảo mật, dưới ngưỡng 15% nên C4 không được xác nhận; tất cả rơi vào Keystore 12/30 và cleartext 5/30. Ở E3, công cụ quét APK đạt độ chính xác 0,291 và không phát hiện lỗi trong mã nguồn.

**Thao tác.** Chỉ dòng đỏ bị xóa và dòng xanh thêm vào trong diff `TokenVault.kt`.

**Lưu ý.** Không xếp hạng trợ lý AI theo 12/48, 5/50, 0/50 vì công cụ và mô hình bị lẫn (mở D4 nếu được hỏi).

## 16. Demo: trước và sau tối ưu (10:50 đến 12:50) — slide 16

*Demo trực tiếp · 120 giây (chuyển và nhận lại lượt: ~25 giây, 54 từ; phần demo trực tiếp: ~95 giây)*

**Lời nói.** *[Chuyển lượt]* Để thầy cô và các bạn quan sát trực tiếp chứ không chỉ nhìn bảng số, thành viên nhóm em sẽ chạy demo song song bản nháp AI và bản tối ưu trên máy ảo. Xin mời bạn. *[Nhận lại lượt]* Cảm ơn phần demo. Sự khác biệt khi cuộn và tìm kiếm thể hiện rất rõ.

**Thao tác.** Chuyển micro cho thành viên demo (thứ tự gợi ý: P09 danh sách 2.000 lượt mượn, P06 ảnh bìa, P08 thẻ trang trí, P13 tìm kiếm 100.000 tựa sách); nếu máy ảo lỗi, mở lại video ở slide 7, 8, 10.

**Lưu ý.** Số đo trên máy ảo laptop chỉ để minh họa; số tham chiếu của nghiên cứu là bốn mốc trên slide (90% → 1,3%; 180 → 60 MB; 62,87 → 49,07 ms; 96,63 → 37 ms). Chi tiết chạy demo ở optimization/study/demo/README.md.

## 17. Bản nháp build lỗi (12:50 đến 13:30) — slide 17

*Build và tự sửa lỗi · 40 giây · 89 từ*

**Lời nói.** Quay lại lỗi phổ biến nhất: bản nháp build lỗi. Ở E1, chỉ 42/100 bản build thành công. Hai yếu tố tạo chênh lệch rõ nhất là mô hình và bản đặc tả. Ở E6, dùng cùng công cụ và chỉ đổi mô hình, số bản build thành công tăng từ 1/9 với claude-haiku lên 7/9 với claude-sonnet. Thay prompt kiểu trào lưu bằng bản đặc tả có cấu trúc kéo tỷ lệ khung hình trễ từ 99% xuống 19,6% và lượng mã Kotlin từ 1.164 xuống 524 dòng.

**Thao tác.** Chỉ lần lượt 42/100, cặp 1/9 với 7/9, rồi cặp 99% xuống 19,6%.

**Lưu ý.** E6 là thí nghiệm khám phá với tối đa 3 lần chạy mỗi ô, không có kiểm định p; chi phí claude-haiku 0,11 USD so với claude-sonnet 0,48 USD.

## 18. “Đã sửa xong” chưa phải kết quả (13:30 đến 14:10) — slide 18

*Build và tự sửa lỗi · 40 giây · 86 từ*

**Lời nói.** Một rủi ro khác là tin vào lời “đã sửa xong”. Ở E5, khi ở chế độ chỉ đề xuất mà không sửa mã, trợ lý AI tìm đủ 168/168 lượt lỗi cài sẵn. Nhưng khi tự sửa, codex tuyên bố đã xong trong khi chạy biên dịch thật thì 9/12 bản lỗi do tự thêm plugin baselineprofile không dùng được. Đồng thời 21/24 lượt làm đổi cả mẫu đối chứng vốn đúng. Rõ ràng lời trợ lý AI không thể thay kết quả kiểm thử.

**Thao tác.** Chỉ hộp lời trả lời của codex ở trên và dòng lỗi biên dịch thật bên dưới.

**Lưu ý.** Nhấn mạnh: biên dịch và kiểm thử độc lập mới là căn cứ xác nhận, không phải lời của trợ lý AI.

## 19. Sáu kết luận (14:10 đến 14:50) — slide 19

*Kết luận · 40 giây · 90 từ*

**Lời nói.** Sáu thí nghiệm cho sáu kết luận. E1: 42/100 bản build thành công, hầu hết có cảnh báo bảo mật; tệp quy tắc giảm cảnh báo nhưng cũng giảm bản build thành công. E2: danh sách và ảnh làm trượt mục tiêu; ba lỗi Compose kinh điển không có chi phí đo được. E3: quét APK đạt 0,291, bỏ sót lỗi mã. E4: 17/148 lượt nới lỏng cơ chế bảo mật. E5: 9/12 bản codex sửa không biên dịch được. E6: bản đặc tả đưa khung hình trễ xuống 19,6%.

**Thao tác.** Lướt nhanh sáu hàng từ E1 đến E6 với nhịp dứt khoát.

**Lưu ý.** Không diễn giải lại chi tiết; dùng slide này gom toàn cảnh trước khi chốt kiểm định.

## 20. Kết quả các phép kiểm định (14:50 đến 15:30) — slide 20

*Kết luận · 40 giây · 87 từ*

**Lời nói.** Về thống kê, trong năm phép kiểm định đăng ký trước sau hiệu chỉnh Holm, bốn phép được xác nhận là C1, C2, C3 và C5 — khẳng định hiệu quả tệp quy tắc, mật độ cảnh báo bảo mật cao hơn ứng dụng do người viết và chi phí hiệu ứng trang trí. C4 không được xác nhận vì 17/148 dưới 15%. Hai giả thuyết mô tả — H4 bịa thư viện (0/2.105) và H5 thiếu mô tả trợ năng (1/66) — đều bị bác bỏ.

**Thao tác.** Chỉ nhóm bốn hàng xanh phía trên, tách khỏi C4 và hai hàng H4, H5.

**Lưu ý.** “Được xác nhận” nghĩa là thỏa mãn điều kiện thống kê đã đăng ký; bảng hiệu chỉnh Holm đầy đủ ở D2.

## 21. Đối chiếu mục tiêu môn học (15:30 đến 16:20) — slide 21

*Kết luận · 50 giây · 105 từ*

**Lời nói.** Đối chiếu mục tiêu môn học: trên màn hình thử nghiệm, bản sửa đạt yêu cầu — tìm 100.000 sách còn 37 ms, dưới 50 ms; RAM 60 và 43 MB dưới 150 MB; danh sách dài 1,3% khung hình trễ. Trên Xiaomi, mở trang sách đạt p90 8 đến 16 ms, cuộn danh mục đạt PSS 94 đến 95 MB. Các ô không có cặp đối chiếu được ghi rõ không áp dụng. Lưới thẻ trên máy ảo vẫn 48,65%; ứng dụng chỉ một prompt có 12 tựa sách; chạm NFC dưới 1 s và thiết bị cấu hình thấp ghi rõ chưa đo.

**Thao tác.** Lướt dọc cột Trạng thái để phân tách phần đạt và hai hạng mục chưa đo.

**Lưu ý.** Ô “không áp dụng” là nơi chỉ một prompt không có bản đã sửa hoặc phép đo ShelfTap trên Xiaomi không có bản nháp AI tương ứng; hai hạng mục cuối ghi rõ “chưa đo”.

## 22. Thiết bị thật (16:20 đến 17:05) — slide 22

*Thiết bị thật · 45 giây · 102 từ*

**Lời nói.** Trên thiết bị thật Xiaomi, ShelfTap cho thấy hai bài học máy ảo không lộ ra. Thứ nhất, đo bằng bản debug là đo sai đối tượng: mở trang sách ở bản debug lên tới 125 ms p90, còn bản build đo hiệu năng (cấu hình tương đương bản phát hành) chỉ 12 đến 16 ms, nằm gọn trong ngưỡng 16,67 ms của 60 fps. Thứ hai, địa chỉ ảnh bìa 10.0.2.2 chỉ truy cập được từ máy ảo, khiến thiết bị thật gọi lại liên tục; khi khắc phục, tỷ lệ khung hình trễ khi cuộn giảm từ 7,0–7,2% xuống 1,5–2,7%.

**Thao tác.** Chỉ bên trái (125 ms so với 12–16 ms) rồi sang bên phải (10.0.2.2).

**Lưu ý.** Số liệu trang chủ 7,0–7,2% xuống 1,5–2,7% đo trên bản debug; thiết bị cấu hình thấp chưa đo.

## 23. Giai đoạn 1 đến 3 (17:05 đến 17:55) — slide 23

*Khuyến nghị · 50 giây · 105 từ*

**Lời nói.** Nhóm em đúc kết quy trình năm giai đoạn, mỗi giai đoạn có mức tối thiểu và nâng cao. Giai đoạn 1 đặt yêu cầu: tối thiểu viết bản đặc tả có cấu trúc; nâng cao gắn mục tiêu đo được và định nghĩa xong là build thành công. Giai đoạn 2 sinh mã: tối thiểu đặt tệp quy tắc ở gốc dự án rồi kiểm tra bản nháp vẫn build thành công; nâng cao chọn mô hình build tốt và bỏ yêu cầu quá sức. Giai đoạn 3 sửa lỗi: cấm nới lỏng cơ chế bảo mật, đọc dòng xóa và khóa bằng test.

**Thao tác.** Dẫn khán giả đi theo ba hàng 1, 2, 3 mà không đọc các ô số nhỏ.

**Lưu ý.** Mọi khuyến nghị đều gắn với một kết quả đo đã nêu (99% → 19,6%; cảnh báo bảo mật 31/33 → 17/34 nhưng build thành công 20/33 → 10/34; 12/30 Keystore).

## 24. Giai đoạn 4 và 5 (17:55 đến 18:40) — slide 24

*Khuyến nghị · 45 giây · 100 từ*

**Lời nói.** Sang hai giai đoạn cuối, ở giai đoạn 4 kiểm tra: tối thiểu là tự build, tự mở ứng dụng và thử lại đúng chỗ vừa sửa; nâng cao là chạy build, kiểm thử và bộ quét mã nguồn trong CI, kết hợp công cụ quét APK cho phần cấu hình. Giai đoạn 5 đo và phát hành: tối thiểu là bật R8 và đo trên thiết bị thật bằng bản build đo hiệu năng; nâng cao là đo A/B xen kẽ bằng Macrobenchmark, ưu tiên sửa danh sách và giải mã ảnh trước khi đầu tư vào Baseline Profile.

**Thao tác.** Chỉ hai hàng 4 và 5, nhấn tiêu đề: tin bản build và số đo, không tin lời trợ lý AI.

**Lưu ý.** Chuyển trực tiếp sang slide chốt cuối cùng.

## 25. Ba điều cần nhớ (18:40 đến 19:15) — slide 25

*Kết thúc · 35 giây · 76 từ*

**Lời nói.** Khép lại bài trình bày, nhóm em xin đúc kết ba điều cần nhớ. Một là trông hoàn thiện chưa phải đã tối ưu: hãy đo với dữ liệu thật. Hai là lỗi hay gặp không phải lỗi nguy hiểm nhất: cần rà riêng lỗi bộ nhớ và bảo mật. Ba là tin bản build và số đo, không tin lời “đã sửa xong”. Xin cảm ơn, mời thầy cô và các bạn đặt câu hỏi.

**Thao tác.** Giữ slide 25 khi hỏi đáp; nhấn phím O để chọn nhanh slide dự phòng D1 đến D8.

**Lưu ý.** Không đưa số mới ở slide kết. Các slide dự phòng D1 đến D8 nằm ngay sau slide 25.

---

## Hỏi đáp dự kiến (mở slide dự phòng tương ứng)

| Câu hỏi có thể gặp | Trả lời (chỉ dùng số trong bộ slide) | Mở |
|---|---|---|
| Số đo trên máy ảo có đại diện cho thiết bị thật không? | Số liệu E2 trên máy ảo chỉ dùng để đối chiếu tỷ lệ A/B giữa bản nháp AI và bản sửa, không phải tốc độ trên thiết bị thật. Khi đổi cấu hình kết xuất GPU của máy ảo, mức cải thiện p90 của cùng bản sửa danh sách còn −32,3% thay vì −78,2%. Nhóm em cũng ghi rõ các hạng mục chưa đo gồm thời gian chạm thẻ NFC tự mượn sách dưới 1 s, thiết bị cấu hình thấp, RAM toàn ứng dụng, bộ nhớ đồ họa và P12. | D1 |
| Chi tiết năm phép kiểm định và hiệu chỉnh Holm ra sao? | Năm phép kiểm định đăng ký trước được hiệu chỉnh Holm trên m = 5. Bốn phép kiểm định được xác nhận là C2 (p hiệu chỉnh 0,00005, chênh lệch +3,025), C1 (0,00016, +0,439), C3 (0,00027, δ Cliff +0,582) và C5 (0,000616, +28,13%). Riêng C4 có p hiệu chỉnh 0,3106 và tỷ lệ 0,115 thấp hơn ngưỡng 0,15 nên không được xác nhận. | D2 |
| Vì sao hiệu ứng trang trí khi thì ghi +28,13%, khi thì ghi −22%? | Cả hai cùng mô tả một cặp trung vị 62,87 ms và 49,07 ms: phép kiểm định C5 tính theo (A − B)/B là +28,13%, còn bảng E2 tính theo (B − A)/A là −22% với khoảng tin cậy 95% [−35,2; −11,8]. Bảng D3 liệt kê đầy đủ bảy nhân tố của E2 cùng đối chiếu riêng cho P01. | D3 |
| Trong E4, trợ lý AI nào nới lỏng cơ chế bảo mật nhiều hơn và cách chấm có khách quan không? | Hai người chấm độc lập được ẩn thông tin về trợ lý AI và biến thể prompt, đạt hệ số kappa 0,909 trên 134 bản diff. Toàn bộ lượt nới lỏng cơ chế bảo mật rơi vào Keystore 12/30 và cleartext 5/30; ba kịch bản còn lại là 0/88. Theo từng trợ lý AI: agy 12/48, codex 5/50, claude 0/50, nhưng nhóm em không xếp hạng vì công cụ và mô hình bị lẫn vào nhau. | D4 |
| Ứng dụng do người viết có mắc các lỗi tương tự không? | Có, thậm chí cao hơn ở nhiều quy tắc giao diện và hiệu năng: thiếu key danh sách P01 là 14/20 ở nhóm ứng dụng do người viết trước 2022, màu và kích thước gán cứng U04 là 19/20, thiếu mô tả biểu tượng U01 là 9/20. Vì vậy nhóm em không khẳng định mọi lỗi phổ biến đều riêng của AI; phép so sánh được kiểm định giữa bản nháp AI và ứng dụng do người viết là mật độ cảnh báo bảo mật C3 (2,37 so với 1,02). | D5, slide 13 |
| Các trợ lý AI khác nhau thế nào trong thí nghiệm E6? | Mỗi ô trong E6 có 3 lần chạy. Với chỉ một prompt, agy-g38flash build thành công 3/3 còn ba trợ lý còn lại chỉ đạt 1/3; nhưng khi chuyển sang tệp quy tắc và bản đặc tả, claude-sonnet đạt 3/3 ở cả hai, trong khi claude-haiku là 0/3. Riêng agy-g38flash ở bản đặc tả không sinh được bản nháp ở cả 3 lần do chế độ chạy không giao diện từ chối một lệnh trước khi ghi tệp. | D6 |
| Cấu hình AI cá nhân trên máy thí nghiệm có làm nhiễu kết quả không? | Mỗi lần sinh mã chạy trong thư mục HOME giả lập chỉ chứa tệp xác thực và bị khóa ghi ra ngoài. Quét lại 623 bản ghi hội thoại không thấy dấu vết cấu hình riêng; mẫu đối chứng chạy với thư mục HOME thật lộ ngay ở lượt đầu với 18.244 token so với khoảng 7.500. Nhóm em cũng công khai rằng cấu hình hệ thống tự nạp ba plugin đo đạc vào Claude; khi đọc mã nguồn không thấy chúng chèn chữ vào hội thoại nhưng chưa thử A/B khi tắt chúng. | D7 |
| Còn vấn đề cỡ chữ lớn 2.0 và trợ năng thì sao? | Nhóm em xếp cỡ chữ lớn và bố cục sang phần chất lượng giao diện ở D8 để bài chính tập trung vào ba trụ cột tối ưu. Ở tỷ lệ cỡ chữ 2.0, cả bốn ứng dụng sinh từ chỉ một prompt đều vỡ bố cục; trên ShelfTap, nhóm em giảm số màn hình vỡ bố cục từ 5/7 xuống 0/7 và cảnh báo ATF từ 4 xuống 2. | D8 |
| Tệp quy tắc có đảm bảo hết lỗi bảo mật khi ứng dụng build thành công không? | Không. Trên toàn bộ tập E1, tỷ lệ có cảnh báo bảo mật giảm từ 31/33 xuống 17/34, nhưng số bản build thành công sau tối đa 3 lần sửa cũng giảm từ 20/33 xuống 10/34 (prompt chi tiết là 12/33), và nếu chỉ xét 42 bản build thành công thì chênh lệch C1 bằng +0,000 trong khi số loại lỗi C2 vẫn giảm (+3,829). Vì vậy tệp quy tắc chỉ là lớp phòng ngừa ở khâu sinh mã, vẫn cần kiểm tra bản build và quét bảo mật độc lập ở giai đoạn 4. | slide 14, D2 |
| Vì sao Baseline Profile không còn tác dụng rõ trên thiết bị thật? | Trên máy ảo E2, Baseline Profile giảm thời gian khởi động từ 462 ms xuống 434,8 ms (−5,88%). Khi đo trên thiết bị thật Xiaomi bằng bản build đo hiệu năng, tỷ lệ khung hình trễ có và không có Baseline Profile lần lượt là 1,44–1,49% và 1,45–1,60%, không có khác biệt đo được. Điều này nhắc lại nguyên tắc đo đúng bản build và ưu tiên sửa danh sách, giải mã ảnh trước. | slide 9, slide 22, D3 |

## Chuẩn bị trước buổi trình bày
- [ ] Chạy thử bấm giờ toàn bài (gồm cả 120 giây demo trực tiếp ở slide 16); mục tiêu tổng thời lượng 15 đến 20 phút. Nếu cần rút gọn 1 phút, đọc gọn slide 19 và slide 20.
- [ ] Phân công rõ người thuyết trình chính và thành viên phụ trách demo trực tiếp ở slide 16; khớp trước câu chuyển lượt và câu nhận lại lượt.
- [ ] Kiểm tra ba video A/B chạy mượt không cần mạng: `evidence/video/P09c-AB.mp4` (slide 7), `P08c-AB.mp4` (slide 8), `P06c-AB.mp4` (slide 10); kiểm tra ảnh ở slide 2, slide 3 và D8.
- [ ] Kiểm tra sẵn môi trường demo theo `optimization/study/demo/README.md` và sẵn sàng chuyển sang video A/B nếu máy ảo gặp sự cố.
- [ ] Nhớ phím tắt điều hướng trong `presentation.html`: `P` (cửa sổ người trình bày), `O` (lưới chọn slide), `N` (ghi chú), `T` (sáng/tối). Slide dự phòng D1 đến D8 tương ứng số thứ tự 26 đến 33.

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
