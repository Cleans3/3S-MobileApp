# Board và app cạnh nhau (kế hoạch §5c, bước "ảnh chụp trên máy đặt cạnh board")

**Cập nhật 2026-11-28:** các board của màn đã dựng được vẽ lại theo app (xem "Vẽ lại theo app" trong `ui-ux-mockup/README.md`), và toàn bộ ảnh cặp dưới đây được dựng lại từ board mới và ảnh app mới (audit `ref`, `refdark`, `medium`, `expanded`, tiếng Việt, emulator `lowend36ui`, tầng Vừa; bảng kết quả ở `audit/V1/`). Công cụ ghép tắt hiệu ứng vào cảnh của board, nên ảnh là bố cục cuối chứ không phải một khung giữa chừng. Các mục "Khác biệt còn lại" bên dưới là bản cũ, viết trước lần vẽ lại: mục nào nói board có nút trong menu, lời chào trong menu, dải "Gợi ý cho bạn", thẻ sách mẫu ở màn Khách, "Kệ", "Hạn mượn" hay nút tìm khi không có mạng thì nay đã khớp app; phần "Khác biệt sau lần vẽ lại" cuối tệp là bản hiện hành.

Mỗi ảnh `light/pair-<màn>.jpg` và `dark/pair-<màn>.jpg` có **board bên trái, app bên phải**, cùng cỡ 390 × 844 dp (ảnh 1170 × 2532 thu nhỏ còn một nửa), 17 màn, hai theme.

- **Lưu ý về ảnh sáng:** bản đầu của công cụ dựng board sáng thành tối (Chrome không giao diện theo chế độ tối của Windows và `data-theme` bị thay cả trong CSS); đã sửa bằng cách chỉ đặt `data-theme` ở phần tử gốc và ép `preferredColorScheme`. Ảnh sáng hiện đã đúng cặp.
- **Board:** `ui-ux-mockup/tools/render-static.js` dựng thành HTML tĩnh, `make_compare.py` tách từng board, đặt `data-theme`, chụp bằng Chrome không giao diện.
- **App:** `run_audit.ps1 -Configs ref,refdark -Locale vi` trên emulator `lowend36ui`: màn hình 1170 × 2532 ở 480 dpi, tức đúng 390 × 844 dp, ảnh chụp bằng bộ vẽ GPU.

```
node ui-ux-mockup/tools/render-static.js ui-ux-mockup <thư mục tạm>
cd ui-ux/audit && .\run_audit.ps1 -Label ref1 -Serial emulator-5556 -Configs ref,refdark -Locale vi
python ui-ux/compare/make_compare.py <thư mục tạm> ui-ux/audit/ref1 ui-ux/compare
```

## Khác biệt còn lại (đã xem ở theme tối; theme sáng cùng bố cục)

Có chủ ý hoặc do dữ liệu:
- Danh mục: board có băng "Gợi ý cho bạn" (chưa có API gợi ý); thẻ chủ đề và sách khác nhau vì dữ liệu mẫu khác nhau.
- Danh sách và trang sách: board có "Kệ N", "Hạn mượn" (API chưa trả kệ, §11 mục 13); bìa của board là chữ viết tắt, bìa của app là tên sách (bìa chữ của §5.5).
- Màn chào: lớp mờ lũy tiến dưới chữ chỉ là lớp tối (không blur cỡ màn lúc mở app); tiêu đề 40 sp thay vì 44 vì font Nunito trên Android rộng hơn.
- Khởi động: ghi chú "[Bản phác]" và danh sách màn đích của board không đưa vào app.
- Mất kết nối: nút "Tìm sách khi không có mạng" cần chỉ mục ngoại tuyến (chưa có).
- Cảnh báo thiết bị màu đỏ ("emulator, debugger") chỉ có trên emulator và bản gỡ lỗi.
- Đăng nhập bằng thẻ, Khóa, Mất kết nối, Tự mượn, Đổi mật khẩu: audit chụp nội dung màn như lớp phủ riêng (không có thanh trên, nền thật), nên viền ngoài khác board; Tự mượn trên emulator chỉ thấy "Điện thoại này không có NFC".

Màn Khách đã làm theo board (thanh có nút "Đăng nhập", dòng "Bạn đang xem với tư cách khách", tiêu đề "Tìm sách trong thư viện", ô ghi chú viền đứt, nút nổi "Đăng nhập để mượn sách"). Khác board có chủ ý: app giữ banner chủ đề và các dải sách của danh mục thay cho một thẻ sách mẫu có nút "Chi tiết" và "Đặt trước", và bấm "Đặt trước" ở trang sách thì đi thẳng tới màn đăng nhập (board mở một tờ "Cần đăng nhập").

Blur: các board dùng `backdrop-filter`, app vẽ blur thật ở **tầng Đầy đủ** (Android 17 trở lên) cho thẻ kính của Khóa, Mất kết nối, Đổi mật khẩu và form Đăng nhập, và ở menu ☰; các ảnh trong thư mục này chụp ở tầng Đầy đủ (`run_audit.ps1 -Tier FULL`). Dải mờ trên và dưới của Danh mục và Danh sách (kính che nội dung đang cuộn) vẫn là lớp phai, chưa blur thật; tầng Vừa và Nhẹ giữ thẻ gần như đặc.

Đã sửa sau lần so sánh đầu: mũi tên ↗ của "Bắt đầu" (màn chào), nhãn "Đã cho mượn hết" màu đỏ nhạt, icon vân tay ở nút mở khóa, icon wifi gạch chéo màu đỏ ở màn mất kết nối.

## Khổ Vừa (720 × 840 dp) và khổ Rộng (960 × 720 dp), theme sáng

`make_compare_wide.py` ghép `mid/pair-<màn>.jpg` (9 màn) và `wide/pair-<màn>.jpg` (8 màn): board bên trái, app bên phải. App chụp bằng `run_audit.ps1 -Label N -Serial emulator-5556 -Configs medium,expanded -Locale vi` (emulator, tầng Vừa). Audit: 0 phát hiện ATF ở cả hai khổ. Đã xem 7 cặp (Danh mục ở hai khổ, Đang mượn ở hai khổ, Tài khoản và Đăng nhập khổ Rộng, Khách khổ Vừa); chưa xem Đặt trước, Lịch sử, Tự mượn và các cặp còn lại.

Khác biệt còn lại, do thiết kế hoặc chưa làm (cập nhật sau khi làm hai khung và đăng nhập chia đôi):
- **Hai khung ở khổ Rộng:** Đang mượn (danh sách và "Chi tiết lượt mượn"), Đặt trước (danh sách ngắn và thẻ chi tiết có nút hủy) và Tài khoản (nhóm cài đặt rồi nội dung nhóm) đã chia hai khung, như Danh mục. Lịch sử đọc vẫn một khung: board chỉ vẽ trạng thái "cần mật khẩu" của nó, không có chi tiết từng lượt. Thiếu so với board: chi tiết không có tác giả, "Gia hạn 14 ngày" (API không trả số ngày), nhóm "Thông báo" (chưa làm), "/ tối đa 5" của số đang mượn.
- **Nút "Chạm để mượn":** board đặt nó ở đầu thanh dọc (khổ Vừa) hoặc chân khung menu (khổ Rộng). App dùng nút tròn nổi ở góc dưới phải, **theo quyết định của người dùng** (đã thử đưa vào thanh dọc, người dùng không chọn; xem nhật ký). Vì vậy ở mọi cặp ảnh, nút nằm trong menu là của **board** (bên trái), không phải app.
- **Khung menu khổ Rộng:** board có lời chào "Thư viện ShelfTap" ở đầu khung; app chỉ có nút thu gọn. Mục "Gợi ý" chưa có (chưa có API gợi ý); nhãn "Đang mượn" ngắn hơn "Sách đang mượn" của board.
- **Thanh dọc khổ Vừa:** nhãn "Danh mục" và "Lịch sử đọc" xuống hai dòng; mục đang chọn là khối trắng thay vì viền quanh biểu tượng.
- **Chào, Đăng nhập và Đổi mật khẩu lần đầu ở khổ Vừa và Rộng:** cả ba chia đôi (cột giá sách tối bên trái chiếm 45%, nội dung bên phải, không còn menu và thanh tiêu đề). Chào có thêm nút "Đăng nhập bằng thẻ thư viện" (board chỉ có Bắt đầu và liên kết xem sách; app có đường vào bằng thẻ). Đăng nhập có thêm "Xem sách mà không cần đăng nhập" dưới form vì không còn menu để quay lại. Board chỉ vẽ bước "Chào" (các bước Mật khẩu, Thẻ, Đổi mật khẩu bấm trong bản phác), nên ảnh `pair-login` và `pair-password-first` so với cùng một board.
- Nút chuyển sáng/tối "Tối" của board không có trong app (app theo cài đặt Tài khoản).


## Khác biệt sau lần vẽ lại (2026-11-28)

- **Khớp app (đã xem ảnh cặp):** Danh mục và Khách ở khổ điện thoại, Vừa, Rộng (lời chào, ô tìm, banner chủ đề, dải sách có tấm thông tin; khách có ghi chú viền đứt và nút "Đăng nhập"); Đang mượn, Đặt trước, Tài khoản ở khổ điện thoại (tiêu đề ở thanh trên); menu, thanh dọc và nút tròn "Chạm sách để mượn" ở góc dưới phải ở khổ Vừa và Rộng; Đang mượn, Đặt trước và Tài khoản hai khung ở khổ Rộng.
- **Còn khác, do dữ liệu mẫu:** tên thành viên, tên sách, ngày và khoản phạt của board khác dữ liệu seed; bìa của board có tên sách hoặc chữ viết tắt, bìa của app là tên sách; banner của app ghép bìa theo dữ liệu thật.
- **Còn khác, có chủ ý hoặc chưa dựng:** Tài khoản ở khổ Vừa: app xếp cả ba nhóm trong một trang, board vẫn là danh sách rồi mới sang nhóm; Lịch sử đọc ở khổ Rộng: app một khung, board hai khung; `WideList` (danh sách cạnh chi tiết) là thiết kế chưa dựng; các màn Gợi ý, Thông báo, Tìm khi không có mạng chưa dựng nên không có ảnh cặp; dòng cảnh báo thiết bị màu đỏ chỉ có trên emulator và bản gỡ lỗi; nút chuyển sáng/tối "Tối" của board không có trong app.
- **Chưa vẽ lại:** các màn điện thoại Chào, Khởi động, Khóa, Đăng nhập bằng thẻ, Đổi mật khẩu, Tự mượn, Lịch sử đọc (nội dung), Đăng xuất kèm lý do vẫn như trước, và các màn Vừa, Rộng tương ứng ngoài Chào và Đăng nhập; chưa so từng cặp của chúng sau lần vẽ lại.
- **Audit ATF cùng lượt chụp:** `ref` 3 phát hiện, `refdark` 2, `medium` 1, `expanded` 0. Các phát hiện về chữ ở danh sách ("Còn 2 bản"), nhóm "Bảo mật" và nút "Sáng" nằm dưới thanh trên hoặc dưới nút nổi, nơi lớp phai làm chữ nhạt đi có chủ ý (cột `under_bar`); phát hiện còn lại là vùng chạm 23 dp của hàng giao diện trong Tài khoản, đã được kiểm lại trước đó và không phải lỗi. Không phát hiện nào do phần vẽ lại.
