# Chỉ số UI của ShelfTap: bảng §8.2 của `ui-ux-plan.md`

- **Ngày:** 2026-11-23
- **Môn:** optimization (chủ đề 1, UI/đồ họa)
- **Phiên bản A:** tag `ui-baseline` = `bbb05e5` (`android/app` giống hệt `main` tại `7763c7c`; nhánh `docs/ui-ux-plan` chỉ thêm tài liệu). Bản dựng: debug.
- **Phiên bản B:** chưa có. Cột B để trống cho đến khi làm xong §5 và §6.2.

## Bảng chỉ số (§8.2)

| Chỉ số | Công cụ | A | B |
|---|---|---|---|
| Số phát hiện accessibility trên các màn hình chính | Accessibility Test Framework 4.1.1 (13 phép kiểm tra, bên dưới) | **4** (light, 7 màn hình). Gồm 2 contrast và 2 trùng nhãn. Cả 4 cấu hình (light, dark, font 2.0, rộng 320 dp) đều ra đúng 4 mục này. | chưa đo |
| Số cặp màu dưới 4.5:1 | `TextContrastCheck` | **2**, cả hai là nút đang bị vô hiệu: "Log in" 1,86 và "Renew" 1,95. Xem ghi chú 2. | chưa đo |
| Số vùng chạm dưới 48 dp | `TouchTargetSizeCheck` | **0** | chưa đo |
| Số màn hình vỡ layout ở font scale 2.0 hoặc rộng 320 dp | Ảnh chụp, mình xem | **5 trên 7** ở font scale 2.0; **0 trên 7** ở rộng 320 dp. Xem ghi chú 3. | chưa đo |
| Số chuỗi viết cứng trong Kotlin | `grep 'Text("'` trong `ui/` | **51** (AuthScreens 22, MemberScreens 12, AccountScreen 7, CatalogScreens 6, Common 4, AppRoot 0) | chưa đo |

Ba dòng đầu do một bài test chạy lại được trên emulator, không cần người cầm máy. Dòng thứ tư dựa trên ảnh chụp của cùng bài test, do mình xem (chưa có người trong nhóm duyệt). Dòng cuối đo bằng grep.

## Cách đo

Chạy `optimization/ui-ux/audit/run_audit.ps1 -Label A -Serial <thiết bị>`. Script chạy bài test `androidTest/.../uiaudit/UiAuditTest.kt` bốn lần, mỗi lần với cấu hình thiết bị khác:

| Cấu hình | Cách đặt trên thiết bị |
|---|---|
| light | mặc định |
| dark | `cmd uimode night yes` |
| font2 | `settings put system font_scale 2.0` |
| w320 | `wm size 560x1120` (320 x 640 dp ở 280 dpi) |

- **Thiết bị:** AVD `lowend36ui`: bản sao cấu hình của `lowend36` (Android 16, API 36, 720x1280 ở 280 dpi, 2 nhân, 2 GB RAM, GPU phần mềm). Dùng bản sao vì `lowend36` đang chạy phép đo catalog của phiên `opt/app-catalog`; không cài gì lên emulator đó hay lên điện thoại thật.
- **App:** `MainActivity` thật, theme và điều hướng thật, mã sản phẩm không sửa. Chỉ có `AppContainer` được thay bằng dữ liệu giả (`AuditData`, tiếng Việt có dấu, một tựa dài; không có người thật, thẻ thật hay CCCD). `FLAG_SECURE` vẫn bật.
- **Cây accessibility** lấy từ hệ thống qua UiAutomation, giống cách Accessibility Scanner lấy. `FLAG_SECURE` không chặn cây này.
- **Ảnh chụp** vẽ từ cây view trong tiến trình (`View.draw`), không chụp qua hệ thống, nên không bị `FLAG_SECURE` làm đen. ATF dùng ảnh này để kiểm tra contrast.
- **7 màn hình:** đăng nhập, danh mục, chi tiết sách, sách đang mượn, tự mượn, đặt trước, tài khoản. Kế hoạch nói "6 màn hình chính"; mình thêm đăng nhập vì nó có ô nhập và nút.
- **13 phép kiểm tra** (preset `LATEST`, ghi ở `audit/A/checks.txt`): SpeakableTextPresent, EditableContentDesc, TouchTargetSize, DuplicateSpeakableText, TextContrast, ClickableSpan, DuplicateClickableBounds, RedundantDescription, ImageContrast, ClassName, TraversalOrder, LinkPurposeUnclear, TextSize.
- **Số lần chạy:** hai lượt đầy đủ bốn cấu hình. Số phần tử và số phát hiện của từng màn hình ở cấu hình light giống nhau giữa hai lượt. Các số trên là của lượt cuối.
- **Kết quả thô:** `optimization/ui-ux/audit/A/`: `findings-<cấu hình>.tsv` (từng phát hiện kèm thông báo của ATF), `screens-<cấu hình>.tsv` (số phần tử và số phát hiện mỗi màn hình), `elements-<cấu hình>.tsv` (cây ATF thấy), `<cấu hình>/<màn hình>.png`.
- **APK A** (debug, `ui-baseline`): SHA-256 `ed75917c91c43831a2b4962c2e2e1bc60fbb1d24e26fa0b1fb4b969f6e291963`. File nằm ở thư mục ghi chú chung của các phiên, không đưa vào repo.

Đã chạy lại `./gradlew assembleDebug testDebugUnitTest` trên A: qua, 59 test unit, 0 lỗi.

## Ghi chú và giới hạn

1. **Đây không phải app Accessibility Scanner.** Scanner là app trên Play Store, không cài được ở đây. ATF là bộ kiểm tra Scanner dùng bên dưới, nên các loại phát hiện giống nhau, nhưng số có thể khác chút. Khi ghi B phải dùng đúng công cụ này.
2. **Hai phát hiện contrast đều là nút bị vô hiệu** ("Log in" khi chưa nhập email và mật khẩu; "Renew" của sách không được gia hạn). Ảnh chụp xác nhận cả hai đang xám. WCAG 1.4.3 miễn trừ thành phần bị vô hiệu, nhưng ATF vẫn báo, nên mình đếm theo đúng kết quả của công cụ và ghi chú ở đây. Nếu nhóm muốn loại, số "dưới 4.5:1" của A là 0 và B nên tính cùng cách. Hai phát hiện còn lại là nhãn trùng: hai nút "Renew" ở danh sách sách đang mượn và hai nút "Cancel" ở danh sách đặt trước, đọc lên không biết thuộc sách nào.
3. **Vỡ layout là do mình xem ảnh, nhóm cần duyệt lại** (bảng dùng cột "người duyệt"). Ở font 2.0, nhãn của thanh dưới bị ngắt giữa chữ ("Catalo/g", "Check/out", "Reser/ved", "Accou/nt") trên 5 màn hình có thanh này (danh mục, đang mượn, tự mượn, đặt trước, tài khoản); ô tìm kiếm ở danh mục cũng bị chật. Chi tiết sách và đăng nhập không có thanh dưới nên không vỡ. Ở rộng 320 dp mình không thấy chỗ vỡ nào; phần dưới của màn Tài khoản bị cắt là do cuộn, không phải lỗi layout.
4. **Tự mượn chỉ đo được trạng thái "không có NFC".** Emulator không có NFC, nên tab này hiện "This phone has no NFC.", chưa phải màn hình chờ chạm thẻ và các trạng thái Working, Done, Failed. Những trạng thái đó cần điện thoại có NFC, và phải làm lại ở cả A lẫn B nếu muốn so.
5. **Ảnh bìa** trong dữ liệu giả là `null` (emulator không với tới server dev), nên hàng sách có ô ảnh trống. Chưa đo được mô tả ảnh bìa (U01) với ảnh thật.
6. **Biểu ngữ "Device warning: emulator, debugger"** hiện ở đăng nhập và tài khoản vì chạy trên emulator có debugger. Đây là chữ đỏ có thật trong ảnh chụp; ATF không báo gì về nó. Trên máy thật sẽ không có.
7. **Chuỗi cứng:** `grep 'Text("'` chỉ đếm các lời gọi `Text(` có chuỗi ngay sau. Chữ truyền qua tham số khác (nhãn ô nhập, `label = { Text(...) }` thì có đếm; thông báo trong `userMessage()` ở `Common.kt` thì không) chưa được tính. Nếu muốn số đầy đủ hơn, nhóm nên chốt một định nghĩa khác ngay bây giờ, trước khi đo B.
8. Chưa có số nào về hiệu năng trong file này. Các phép đo hiệu năng của `opt/app-catalog` (§7) chạy sau khi làm xong B.
