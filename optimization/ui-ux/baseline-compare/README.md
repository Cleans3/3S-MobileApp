# ShelfTap Android: bản lần đầu clone và bản hiện tại

Ngày lập: 2026-11-28. Nhánh `opt/ui-ux`. So sánh **app Android như lúc clone về máy này** (2026-11-22, `af2c9cf`; code trùng tag `ui-baseline`, vì `git diff af2c9cf ui-baseline -- android/app/src/main` rỗng) với **`HEAD`**.

Mọi con số ở đây do công cụ trong `optimization/ui-ux/tools/` đếm từ repo hoặc từ bảng một lần chạy ghi ra (cách chạy lại ở `tools/README.md`). Số đo cần máy thật (khung hình, bộ nhớ trên Xiaomi) không đo lại ở đây: chúng ở `optimization/ui-ux-metrics.md`, `optimization/ui-ux/measure/` và nhật ký `optimization/log/opt-ui-ux.md`.

## 1. Đã thay đổi gì

| Mảng | Bản đầu | Bây giờ | Mục nhật ký |
|---|---|---|---|
| Điều hướng | thanh 5 tab chữ, không icon; phải đăng nhập mới thấy sách | menu ☰ (cửa sổ hẹp), thanh dọc (vừa), menu cố định (rộng); khách xem được danh mục; nút nổi "Chạm sách để mượn" đổi hình thành thanh đặt trước; tìm khi gõ | 2026-11-24 16:17, 23:05 |
| Chữ | 51 chuỗi tiếng Anh viết cứng trong Kotlin | 0 chuỗi cứng; tiếng Việt (mặc định) và tiếng Anh, đủ số nhiều | 2026-11-24 15:36 |
| Giao diện | `lightColorScheme()` mặc định của Material, font hệ thống, không chọn sáng/tối | hệ thống thiết kế (`ui/theme/`): màu sáng và tối, Nunito, khoảng cách; chọn sáng/tối/theo máy ở Tài khoản | 2026-11-24 15:52 |
| Kính và hiệu ứng | không có | ba tầng hiệu ứng theo máy (Đầy đủ, Vừa, Nhẹ), blur nền menu, dải kính ở đầu và chân trang (Haze), icon Lucide và icon morph | 2026-11-24 16:28, 17:02; 2026-11-26 |
| Chuyển động | chuyển cảnh mặc định | nhấn co lại, hàng trồi lên, khung xương, hàng đổi chỗ, trượt vào trang sách, cử chỉ back theo ngón tay, vòng NFC, dấu tích | 2026-11-24 17:32, 22:20; 2026-11-25 |
| Trạng thái | chỉ spinner và một dòng chữ khi rỗng | khung xương, trạng thái rỗng có hướng dẫn, lỗi mạng, offline, lý do không gia hạn | 2026-11-24 16:09 |
| Cửa sổ lớn | bố cục điện thoại bị kéo giãn | cửa sổ vừa và rộng: hai cột, hai khung (danh sách và chi tiết) cho Danh mục, Đang mượn, Đặt trước, Tài khoản; đăng nhập chia đôi | 2026-11-24 16:42; 2026-11-26, 27 |
| Màn hình | Catalog, Loans, Checkout, Reservations, Account, Detail, Login dựng thô | dựng lại theo thiết kế: thẻ kính, bìa chữ, Lịch sử đọc, Khóa, Đổi mật khẩu lần đầu, Tự mượn có đĩa NFC | 2026-11-24 đến 2026-11-25 |
| Chức năng mới | không | **Gợi ý cho bạn** (tính trên máy, ba khổ), **Thông báo** (nhắc hạn trả tính trên máy, ẩn tên sách trên màn khóa) | 2026-11-28 |
| Truy cập | 2 nút trùng nhãn đọc giống nhau | nhãn nói rõ sách nào, vùng chạm không dưới 48 dp, liveRegion cho kết quả tự mượn | 2026-11-24 15:37 |
| Hiệu năng | không đo | kiểu build `measure` (R8, Baseline Profile) để đo thật; trang sách dựng ngay từ dữ liệu danh sách; bìa lỗi được nhớ; thẻ dựng trước khi cuộn tới; chi phí của dải kính đã đo trên Xiaomi | 2026-11-24 21:35; 2026-11-25; 2026-11-26 |
| Công cụ | không | bài audit giao diện chạy được trên emulator (Accessibility Test Framework, ảnh vẽ bằng GPU trong tiến trình), kịch bản đo khung hình trên máy thật, bộ mockup 35 bản, ảnh cặp board và app | cả nhánh |

## 2. Con số đếm từ repo

| What | First clone (`ui-baseline`) | Now (`HEAD`) |
|---|---|---|
| Kotlin files in `android/app/src/main` | 27 | 74 |
| Kotlin lines in `android/app/src/main` | 2745 | 9836 |
| files in `ui/` | 6 | 44 |
| hard-coded `Text("` in `ui/` | 51 | 0 |
| strings in `values/strings.xml` (Vietnamese) | 1 | 281 |
| strings in `values-en/strings.xml` (English) | 0 | 281 |
| unit tests (`@Test` in `src/test`) | 59 | 127 |
| instrumented tests (`@Test` in `src/androidTest`) | 6 | 10 |
| files named `*Screen*` | 4 | 13 |

Bản đầu chỉ có một chuỗi trong `strings.xml` (tên app) vì chữ nằm cứng trong Kotlin. Số kiểm thử đơn vị đếm `@Test` trong `android/app/src/test`.

## 3. Accessibility Test Framework

Accessibility Test Framework findings (the same 13 checks; before = `audit/A`, now = optimization/ui-ux/audit/C1). "Same 7 screens" counts only the screens the first audit covered, so the two columns can be compared; "all screens" is every screen the audit visits now:

| Configuration | First clone (7 screens) | Now, same 7 screens | Now, all screens |
| light (720 x 1280 px, 280 dpi) | 4 (DuplicateSpeakableTextCheck 2, TextContrastCheck 2) | 5 (TextContrastCheck 5) | 11 (RedundantDescriptionCheck 1, TextContrastCheck 9, TouchTargetSizeCheck 1) on 22 screens |
| dark | 4 (DuplicateSpeakableTextCheck 2, TextContrastCheck 2) | 5 (TextContrastCheck 5) | 11 (RedundantDescriptionCheck 1, TextContrastCheck 9, TouchTargetSizeCheck 1) on 22 screens |
| font scale 2.0 | 4 (DuplicateSpeakableTextCheck 2, TextContrastCheck 2) | 5 (TextContrastCheck 5) | 11 (RedundantDescriptionCheck 1, TextContrastCheck 9, TouchTargetSizeCheck 1) on 22 screens |
| 320 dp wide | 4 (DuplicateSpeakableTextCheck 2, TextContrastCheck 2) | 1 (TextContrastCheck 1) | 5 (RedundantDescriptionCheck 1, TextContrastCheck 3, TouchTargetSizeCheck 1) on 22 screens |

Đọc thế nào: bản đầu 4 phát hiện (2 contrast ở hai nút bị vô hiệu, 2 trùng nhãn). Cùng 7 màn, bản này không còn trùng nhãn nhưng có 5 cảnh báo `TextContrastCheck`, cả 5 ở Danh mục, ở mép dưới màn (y từ 1207 đến 1280 trên màn 1280 px): chữ "Còn 3 bản", "Đang được mượn · đặt trước" và ba dòng lý do gợi ý của băng **Gợi ý cho bạn**, đang nằm dưới dải lớp phai và nút nổi của chân trang. Đó là cùng loại cảnh báo đã phân loại ở mục nhật ký 2026-11-27 06:00 (chữ của trang dưới dải thanh), nhưng là năm chữ mới do băng Gợi ý thêm vào; hai nút bị vô hiệu của bản đầu không còn trong danh sách. Chưa kiểm độ tương phản của năm chữ này khi cuộn chúng lên giữa màn. Lần chạy này bằng tiếng Việt trên emulator Android 16; bản đầu đo bằng tiếng Anh. Cột "tất cả màn" cho thấy bài audit hôm nay đi qua 22 màn, nhiều hơn 3 lần so với 7 màn của bản đầu, nên số tuyệt đối không so sánh trực tiếp với cột đầu. Không có vùng chạm nào dưới 48 dp ngoài một thành phần của Material (tay kéo của tờ giải thích).

## 4. Ảnh: bản đầu và bản bây giờ

Mỗi ảnh `pairs/<theme>-<màn>.jpg` có **bản đầu bên trái, bản bây giờ bên phải**, cùng cỡ cửa sổ emulator (720 × 1280 px, 280 dpi). Ảnh bản đầu là ảnh đo ngày 2026-11-23 còn trong lịch sử git (`audit/A/` tại `c4280b8`, vẽ từ cây view trong tiến trình); ảnh bản bây giờ do bộ vẽ GPU (`*-hw.png`). Hai bên không cùng trạng thái: bản đầu có thanh 5 tab và không có menu.

| Màn | Sáng | Tối |
|---|---|---|
| Danh mục | [light-catalog](pairs/light-catalog.jpg) | [dark-catalog](pairs/dark-catalog.jpg) |
| Chi tiết sách | [light-detail](pairs/light-detail.jpg) | [dark-detail](pairs/dark-detail.jpg) |
| Đang mượn | [light-loans](pairs/light-loans.jpg) | [dark-loans](pairs/dark-loans.jpg) |
| Đặt trước | [light-reservations](pairs/light-reservations.jpg) | [dark-reservations](pairs/dark-reservations.jpg) |
| Tài khoản | [light-account](pairs/light-account.jpg) | [dark-account](pairs/dark-account.jpg) |
| Tự mượn | [light-checkout](pairs/light-checkout.jpg) | [dark-checkout](pairs/dark-checkout.jpg) |
| Đăng nhập | [light-login](pairs/light-login.jpg) | [dark-login](pairs/dark-login.jpg) |

![Danh mục, trước và sau](pairs/light-catalog.jpg)

## 5. Hiệu ứng chuyển động (khung hình do chính app vẽ)

Bản đầu không có hiệu ứng nào đáng kể: chuyển màn mặc định, không menu, không nút đổi hình. Dưới đây là hiệu ứng của bản bây giờ, từ các khung hình bài audit ghi lại (bài test tăng đồng hồ của Compose bằng tay, nên mốc thời gian mỗi khung là chính xác). Mỗi hiệu ứng có ảnh động (`.webp` và `.gif`) và một tờ các khung (`-frames.png`) có thời gian dưới mỗi khung.

| Hiệu ứng | Ảnh động | Các khung |
|---|---|---|
| Mở trang sách: màn trượt vào, nút nổi đổi thành thanh có nút đặt trước (260 ms) | [app-detail-open.webp](anim/app-detail-open.webp), [gif](anim/app-detail-open.gif) | [khung](anim/app-detail-open-frames.png) |
| Quay lại từ trang sách | [app-detail-close.webp](anim/app-detail-close.webp) | [khung](anim/app-detail-close-frames.png) |
| Mở menu ☰ | [app-menu-open.webp](anim/app-menu-open.webp) | [khung](anim/app-menu-open-frames.png) |
| Đóng menu | [app-menu-close.webp](anim/app-menu-close.webp) | [khung](anim/app-menu-close-frames.png) |
| Cùng hiệu ứng mở trang sách trong **mockup** (CSS, ở các mốc 0 đến 700 ms) | [mockup-detail-open.webp](anim/mockup-detail-open.webp) | [khung](anim/mockup-detail-open-frames.png) |

![Mở trang sách trong app](anim/app-detail-open.gif)

## 6. Video thật và thống kê khung hình

Hai video quay trên cùng emulator `lowend36ui`, cùng cỡ cửa sổ, cùng kiểu build (debug), sau một lượt khởi động nóng, cùng một kiểu thao tác: cuộn trang chủ, mở một sách rồi quay lại, chuyển Đang mượn, Đặt trước, về Danh mục. Mở `video/compare.html` trong trình duyệt để xem hai video chạy song song (có nút phát, dừng, tốc độ 0,25). File: `video/BASE/effects.mp4` (bản đầu), `video/NOW/effects.mp4` (bản bây giờ).

| Frame statistics of the recorded pass | First clone (BASE) | Now (NOW) |
|---|---|---|
| Total frames rendered | 295 | 172 |
| Janky frames | 84 | 40 |
| Janky frames (%) | 28.47 | 23.26 |
| Janky frames, legacy | 224 | 152 |
| 50th percentile (ms) | 25 | 29 |
| 90th percentile (ms) | 48 | 48 |
| 95th percentile (ms) | 65 | 65 |
| 99th percentile (ms) | 400 | 200 |
| Missed Vsync | 20 | 20 |
| High input latency | 421 | 261 |
| Slow UI thread | 40 | 19 |
| Slow bitmap uploads | 2 | 3 |
| Slow issue draw commands | 63 | 30 |
| Frame deadline missed | 84 | 40 |

**Đọc con số này thận trọng.** Đây là emulator và bản debug: nó cho thấy cả hai bản chạy ở mức tương đương (phân vị 90 đều 48 ms, trung vị 25 so với 29 ms), không cho thấy bản mới nhanh hơn. Số khung khác nhau (295 so với 172) vì hai lượt thao tác không vẽ cùng số khung (màn khác nhau); tỷ lệ khung giật 28,5% so với 23,3% và phân vị 99 400 ms so với 200 ms chịu ảnh hưởng của vài khung đầu mỗi màn và không đủ để kết luận. Số đo hiệu năng đáng tin hơn là trên Xiaomi 25053RT47C, bản `measure` (R8, Baseline Profile), 100.013 đầu sách, nhiều lượt xen kẽ: giật 0,02% ở mọi cảnh cuộn; phân vị 90 từ 5 đến 14 ms tùy cảnh và cấu hình (có hoặc không có dải kính) (nhật ký 2026-11-26 19:00, 21:30 và 2026-11-27 04:30). Bản đầu **chưa được đo trên Xiaomi bằng cùng kịch bản**: kịch bản đo khung hình (`measure.ps1`) được viết sau, nên chưa có cột "bản đầu" trên máy thật.

## 7. Chưa làm, chưa so được

- Cột "bản đầu" của đo khung hình và bộ nhớ trên máy thật (bản đầu cần máy Xiaomi, server dev và Docker; Docker không chạy lúc làm).
- Video bản **release**: không quay được nguyên trạng vì `FLAG_SECURE` và vì cần server để đăng nhập; hai video đều là bản debug.
- Không có video `.mp4` của mockup: hiệu ứng mockup là CSS, được chụp thành khung và ảnh động (WebP, GIF).
- Thử trên máy có tỷ lệ đặc biệt thật (gập, bầu dục).
- Hộp xin quyền thông báo (cửa sổ riêng) chưa có ảnh app.
