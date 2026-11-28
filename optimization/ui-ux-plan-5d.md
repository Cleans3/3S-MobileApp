# Kế hoạch giai đoạn 5d: mockup khớp app, hiệu ứng nút ở trang sách, và các màn còn thiếu

Ngày lập: 2026-11-28. Nối tiếp giai đoạn 5c của `optimization/ui-ux-plan.md` §9. Nguyên tắc: **app là nguồn sự thật cho màn đã dựng** (mockup vẽ lại theo app); **màn chưa dựng thì dựng trong app trước**, rồi mockup vẽ lại theo app, không vẽ mockup trước như ở 5c.

## 1. Việc còn lại

| # | Vấn đề | Nguồn |
|---|---|---|
| 1 | Hiệu ứng nút ở trang chi tiết sách: trong app, nút "Chạm sách để mượn" **đổi hình** thành thanh kính có nút đặt trước (viên thuốc thu lại thành vòng tròn ở cửa sổ rộng, rồi mở ra thành thanh khi vào trang sách); mockup chỉ có hai hình tĩnh | `ActionDock.kt`, `Detail.dc.html`, `WideList`, `MidCatalog` |
| 2 | Màn đã dựng trong app nhưng mockup chưa vẽ lại: Chào, Khởi động, Khóa, Đăng nhập bằng thẻ, Đổi mật khẩu, Tự mượn, nội dung Lịch sử đọc (điện thoại) và các bản Vừa, Rộng tương ứng | `ui-ux/compare/README.md` |
| 3 | Chỗ mockup khác cách chia khung của app: Tài khoản khổ Vừa (app một trang), Lịch sử đọc khổ Rộng (app một khung), `WideList` (app chưa có hai khung cho danh sách và chi tiết) | `ui-ux/compare/README.md` |
| 4 | Ảnh cặp theme tối chưa xem từng cặp | `ui-ux/compare/dark/` |
| 5 | Màn chưa có trong app: **Gợi ý cho bạn**, **Thông báo**, **Tìm khi không có mạng** (và nhóm Thông báo, công tắc "Gợi ý theo lịch sử đọc" trong Tài khoản) | `ui-ux-plan.md` §9 5c, `optimization/search-recommend-plan.md` (nhánh `docs/search-recommend-plan`) |

## 2. Thứ tự và cách làm

### Bước A: mockup khớp app cho phần đã dựng (mục 1 đến 4, không cần quyết định mới)

A1. **Hiệu ứng đổi hình của nút.** Ghi lại số đo thật từ `ActionDock.kt`: viên thuốc 56 dp thành thanh cao 68 dp (nút tròn 40 thành 52 dp, chữ "Chạm sách để mượn" mờ dần trước nửa chặng, nút đặt trước hiện sau nửa chặng, thanh nhấc lên 8 dp, đổ bóng mất dần), thời lượng `Motion.SCREEN_MS`; cửa sổ rộng: vòng tròn 56 dp ở góc dưới phải mở sang trái thành thanh rộng tối đa `AdaptiveSize.dockBarMax` và một phần khung sách. Vẽ lại trong mockup bằng chuyển động CSS: `Detail.dc.html` (vào trang thì thanh mở ra từ viên thuốc, có nút phát lại), `MidCatalog` và `WideList` (chọn sách thì nút tròn mở thành thanh; quay lại danh sách thì thu về), trạng thái đặt trước (viên thuốc đổi màu, dấu sách thành dấu tích) và bản giảm chuyển động. Kiểm: quay video màn hình app và board, so thời lượng và thứ tự.
A2. **Vẽ lại các màn mục 2** theo ảnh cặp mới: từng màn một, sửa board, dựng lại cặp, xem bằng mắt.
A3. **Khớp cách chia khung (mục 3).** Tài khoản khổ Vừa thành một trang xếp ba nhóm; Lịch sử đọc khổ Rộng một khung (bước hỏi mật khẩu giữ nguyên); `WideList` ghi rõ là thiết kế chưa dựng, hoặc bỏ nếu người dùng muốn mockup chỉ có màn đã dựng.
A4. **Xem ảnh cặp tối (mục 4)** và sửa chỗ lệch.
A5. Chạy lại `check-scripts`, đo tương phản, kích thước vùng chạm, kiểm bố cục (7 kiểu hiển thị); ghi nhật ký; đăng lại link.

### Bước B: dựng ba màn còn thiếu trong app (mục 5), mỗi màn đủ ba khổ

Mỗi màn: (1) chốt hợp đồng và phần phụ thuộc, (2) dựng giao diện ở khổ điện thoại, Vừa, Rộng và các kiểu hiển thị khác (320 dp, chữ 2.0, xoay ngang, tầng hiệu ứng), (3) kiểm thử đơn vị và trên máy (`UiAuditTest`, `*ScreenTest`), (4) đo theo `ui-ux-plan.md` §7 và §8.5, (5) một mục nhật ký mỗi thay đổi.

B1. **Gợi ý cho bạn** (`optimization/search-recommend-plan.md` §6: B1 đến B4).
- Dữ liệu: sách đang mượn, lịch sử đọc (chỉ khi bật lịch sử và đăng nhập bằng mật khẩu), đặt trước, "không quan tâm" lưu trên máy; đặc trưng (tác giả, chủ đề) lấy từ `GET /api/titles/{id}`.
- Thuật toán v1 theo nội dung, chạy trên máy, một lớp giao diện `Recommender` để sau này đổi sang danh mục offline mà không sửa màn.
- Màn: băng "Gợi ý cho bạn" ở Danh mục, màn đầy đủ (danh sách kèm lý do, ba trạng thái: mật khẩu, thẻ kèm lời mời nhập mật khẩu, tắt lịch sử); khổ Rộng và Vừa có khung "Vì sao gợi ý".
- Tài khoản: công tắc "Gợi ý theo lịch sử đọc".
- Đo: thời gian tính gợi ý trên máy yếu, bộ nhớ, tác động lên cuộn Danh mục.
- Phụ thuộc: không cần API mới nếu dùng `GET /api/titles/{id}` (chậm hơn danh mục offline); chuyển sang danh mục offline khi B3 xong.

B2. **Thông báo.**
- Phạm vi làm được không cần server: **nhắc hạn trả** tính trên máy từ `dueAt` (lịch báo trong máy), **ẩn tên sách trên màn khóa** (`VISIBILITY_PRIVATE` kèm bản công khai).
- **Báo sách đặt trước sẵn sàng cần cơ chế đẩy từ server** (`ui-ux-plan.md` §11 mục 14): chưa làm cho tới khi nhóm chốt; màn chỉ hiện các tùy chọn làm được.
- Màn: giải thích trước, rồi mới gọi hộp thoại xin quyền `POST_NOTIFICATIONS` (Android 13+), trạng thái bị từ chối (nút mở cài đặt hệ thống), cài đặt từng loại; nằm trong Tài khoản (nhóm "Thông báo", khổ Rộng là một khung trong hai khung).
- Kiểm thử: đặt lịch, hủy lịch khi trả sách hoặc đăng xuất, múi giờ; thử trên máy thật vì emulator không cho thấy màn khóa.

B3. **Tìm khi không có mạng.**
- Chặn bởi hợp đồng: cần **API tải danh mục** (`ETag`, phân trang) và chỉ mục trên máy (Room + FTS4, tìm không dấu) theo `search-recommend-plan.md` §5 (A1 đến A3): nhóm chốt thêm API vào `server/API.md` (`ui-ux-plan.md` §11 mục 13) trước, rồi server, rồi app.
- Chỉ mục tải khi có mạng, cập nhật theo `ETag`; mục tiêu tìm dưới 50 ms trên 100k sách.
- Màn: nối từ "Mất kết nối" bằng nút "Tìm sách khi không có mạng"; khi mạng có lại thì tự quay về Danh mục; không hiện số bản còn.
- Đo: thời gian truy vấn trung vị và p95, kích thước chỉ mục, thời gian dựng chỉ mục, khung hình khi gõ.

### Bước C: vẽ lại mockup theo app cho ba màn mới

Sau khi mỗi màn trong B xong: vẽ lại board điện thoại, Vừa, Rộng của màn đó và các chỗ nối (băng Gợi ý ở Danh mục, nhóm Thông báo ở Tài khoản, nút ở Mất kết nối) theo ảnh chụp app; dựng lại ảnh cặp; cập nhật hai README; đăng lại link.

## 3. Thứ tự và ước lượng

| Bước | Việc | Ước lượng |
|---|---|---|
| A1 | Hiệu ứng nút ở trang sách trong mockup | 0,5 ngày |
| A2 đến A5 | Vẽ lại các màn đã dựng, khớp cách chia khung, ảnh tối, kiểm tra, đăng | 1,5 ngày |
| B1 + C | Gợi ý | 2 đến 3 ngày |
| B2 + C | Thông báo (phần làm được) | 2 ngày |
| B3 + C | Tìm khi không có mạng (sau khi chốt API) | 3 đến 4 ngày, cộng phần server |

## 4. Việc cần người dùng chốt trước Bước B

1. Làm B1 đến B3 trên nhánh nào: tiếp tục `opt/ui-ux` hay tách nhánh riêng cho từng màn (kế hoạch tìm kiếm đề xuất `opt/search`, `opt/recommend`). Chỉ tạo nhánh sau khi được đồng ý.
2. Thêm API tải danh mục vào `server/API.md` và làm phần server (cần cho B3), hay để B3 sang đợt sau.
3. Báo sách đặt trước sẵn sàng: đẩy từ server (cần dịch vụ ngoài), tự hỏi server định kỳ, hay tạm chỉ hiện trong app.
4. `WideList` (danh sách cạnh chi tiết): giữ làm thiết kế hay bỏ khỏi mockup.

## 5. Rủi ro

- Gợi ý dùng `GET /api/titles/{id}` cho từng sách: giới hạn 20 request mỗi phút của server; phải gộp và lưu tạm.
- Danh mục offline 100k sách có thể vượt 150 MB bộ nhớ khi dựng chỉ mục; đo trước khi làm giao diện.
- Thông báo trên máy khóa chỉ thử được trên máy thật.
- Mỗi màn mới thêm vào mọi khổ nên phải chạy lại audit ATF cho ba khổ và ba tầng hiệu ứng.

## 6. Tiến độ (2026-11-28, lưu lại khi dừng giữa chừng)

- **Xong:** bước A (mockup khớp app cho phần đã dựng, hiệu ứng đổi hình của nút), link mockup bản 32. B1 (Gợi ý) trong app: thuật toán, dịch vụ, màn hình ba khổ, băng ở Danh mục, công tắc ở Tài khoản, kiểm thử đơn vị, audit ATF (commit `493b53c`).
- **Đang dở (chưa đăng link):** vẽ lại mockup cho Gợi ý theo app: `Recommend.dc.html` (điện thoại), `MidRecommend` và `WideRecommend` (sinh từ `tools/wide-boards/boards1.js`), băng "Gợi ý cho bạn" ở `Catalog`, `RailCatalog`, `WideCatalog` (`shell.js` `home()`), mục "Gợi ý" trong menu, công tắc ở `Account`, `MidAccount`, `WideAccount`. Đo xong: `check-scripts`, tương phản, vùng chạm như trước; kiểm bố cục trong trình duyệt sạch ở khổ gốc, rộng 320, 320 cùng chữ 2.0, 600, menu thu gọn; **còn 2 chỗ chữ đè nhau ở chữ 2.0** trên `Catalog`, `RailCatalog`, `WideCatalog` (băng "Gợi ý cho bạn": tấm thông tin đè lên tiêu đề, cần tăng chỗ chừa như đã làm cho băng chủ đề).
- **Chưa làm:** dựng lại ảnh cặp (cần chạy `run_audit.ps1 -Configs ref,refdark,medium,expanded -Locale vi` rồi `make_compare*.py`), cập nhật hai README, nhật ký cho phần mockup, đăng lại link; đo thời gian tính gợi ý trên dữ liệu thật (cần Docker và server dev), thử trên máy thật; B2 (Thông báo) và B3 (Tìm khi không có mạng) chưa bắt đầu, chờ nhóm chốt §4.

## 7. Quyết định của người dùng (2026-11-28) cho các mục ở §4

1. **Nhánh:** tiếp tục trên `opt/ui-ux`, không tạo nhánh mới.
2. **API tải danh mục:** để sang đợt sau; không sửa `server/API.md`, không làm server. Ý tưởng để lại cho người làm backend ở `optimization/backend-catalog-api-note.md`. Hệ quả: **Tìm khi không có mạng** chưa dựng trong app (không có dữ liệu để tìm); chỉ vẽ mockup theo thiết kế cũ khi nào có API.
3. **Báo sách đặt trước sẵn sàng:** tạm chỉ hiện trong app, không đẩy từ server, không hỏi server định kỳ. Thông báo chỉ có phần làm được trên máy: nhắc hạn trả và ẩn tên sách trên màn khóa.
4. **`WideList`:** giữ trong mockup, ghi rõ là thiết kế chưa dựng (như README hiện tại).

Việc tiếp theo: dựng **Thông báo** (nhóm trong Tài khoản) trong app ở ba khổ, rồi vẽ lại mockup theo app.
