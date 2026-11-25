# Board và app cạnh nhau (kế hoạch §5c, bước "ảnh chụp trên máy đặt cạnh board")

Mỗi ảnh `light/pair-<màn>.jpg` và `dark/pair-<màn>.jpg` có **board bên trái, app bên phải**, cùng cỡ 390 × 844 dp (ảnh 1170 × 2532 thu nhỏ còn một nửa), 17 màn, hai theme.

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

Thật sự khác, chưa sửa:
- **Chế độ khách:** board `Guest` có dòng "Bạn đang xem với tư cách khách", nút "Đăng nhập" trên thanh, hộp ghi chú "Khách xem và tìm sách được…" và nút nổi "Đăng nhập để mượn sách"; app hiện danh mục thường với nút tài khoản. Cần làm riêng.

Đã sửa sau lần so sánh đầu: mũi tên ↗ của "Bắt đầu" (màn chào), nhãn "Đã cho mượn hết" màu đỏ nhạt, icon vân tay ở nút mở khóa, icon wifi gạch chéo màu đỏ ở màn mất kết nối.
