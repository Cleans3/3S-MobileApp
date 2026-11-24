# Chỉ số UI của ShelfTap: bảng §8.2 của `ui-ux-plan.md`

- **Ngày:** 2026-11-23 (cột A); 2026-11-24 (cột B)
- **Môn:** optimization (chủ đề 1, UI/đồ họa)
- **Phiên bản A:** tag `ui-baseline` = `bbb05e5` (`android/app` giống hệt `main` tại `7763c7c`; nhánh `docs/ui-ux-plan` chỉ thêm tài liệu). Bản dựng: debug.
- **Phiên bản B:** `opt/ui-ux` tại `96a2895` (sau design system, chuỗi, accessibility, trạng thái UI, menu ☰, Guest, tìm khi gõ). Bản dựng: debug. SHA-256 của APK B: `1c4e8d4e11abd49f2b4b358bcf3e78c304149225f90cc4d5e9e3330211b014f4`. **Chưa phải B cuối của kế hoạch:** §5.5 (kính), §6.3 (tầng hiệu ứng), §6.4 (bố cục thích ứng), §6.5 (chuyển động) và định dạng ngày/tiền (§5.2) chưa làm. Bảng này đo lại khi các phần đó xong.

## Bảng chỉ số (§8.2)

| Chỉ số | Công cụ | A | B |
|---|---|---|---|
| Số phát hiện accessibility trên các màn hình chính | Accessibility Test Framework 4.1.1 (13 phép kiểm tra, bên dưới) | **4** (light, 7 màn hình). Gồm 2 contrast và 2 trùng nhãn. Cả 4 cấu hình (light, dark, font 2.0, rộng 320 dp) đều ra đúng 4 mục này. | **2** (light, 7 màn hình cũ; cũng 2 trên 9 màn hình, kể cả "guest" và "menu"). Gồm 2 contrast, 0 trùng nhãn. Cả 4 cấu hình đều ra đúng 2 mục này. |
| Số cặp màu dưới 4.5:1 | `TextContrastCheck` | **2**, cả hai là nút đang bị vô hiệu: "Log in" 1,86 và "Renew" 1,95. Xem ghi chú 2. | **2**, vẫn là hai nút bị vô hiệu: "Log in" 1,93 và "Renew" 2,01. Không có chữ đang bật nào dưới 4,5:1 ở cả 4 cấu hình. |
| Số vùng chạm dưới 48 dp | `TouchTargetSizeCheck` | **0** | **0** |
| Số màn hình vỡ layout ở font scale 2.0 hoặc rộng 320 dp | Ảnh chụp, mình xem | **5 trên 7** ở font scale 2.0; **0 trên 7** ở rộng 320 dp. Xem ghi chú 3. | **0 trên 7** ở font scale 2.0; **0 trên 7** ở rộng 320 dp (mình xem ảnh, chờ người duyệt). Xem ghi chú 3. |
| Số chuỗi viết cứng trong Kotlin | `grep 'Text("'` trong `ui/` | **51** (AuthScreens 22, MemberScreens 12, AccountScreen 7, CatalogScreens 6, Common 4, AppRoot 0) | **0** |

Ba dòng đầu do một bài test chạy lại được trên emulator, không cần người cầm máy. Dòng thứ tư dựa trên ảnh chụp của cùng bài test, do mình xem (chưa có người trong nhóm duyệt). Dòng cuối đo bằng grep.

B đo lại đúng cách đó: `run_audit.ps1 -Label B -Serial emulator-5556` trên cùng AVD `lowend36ui` (Android 16, tiếng Anh, 720x1280 ở 280 dpi), kết quả thô ở `optimization/ui-ux/audit/B/`. Bài đo B có 9 màn hình thay vì 7: thêm "guest" (danh mục khi chưa đăng nhập) và "menu" (menu ☰ đang mở); các con số so A/B ở trên chỉ tính 7 màn hình cũ, và số 9 màn hình ghi riêng. Số phần tử ATF thấy ít hơn ở B (243 so với 317 ở light) vì thanh 5 tab bị bỏ và danh sách ngắn hơn; đây không phải chỉ số.

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

## Ghi chú cột B (2026-11-24)

9. **Phát hiện còn lại ở B là hai nút bị vô hiệu**, đúng như A (ghi chú 2). Hai phát hiện trùng nhãn của A mất vì nút "Gia hạn" và "Hủy" đọc kèm tên sách. Đây là số của công cụ ATF, không phải đánh giá tổng thể: số 0 ở dòng "trùng nhãn" chỉ có nghĩa là 13 phép kiểm tra không thấy gì trên các màn hình này.
10. **Số chuỗi viết cứng = 0** theo đúng lệnh `grep 'Text("'` của A. Lệnh này không thấy chữ truyền qua tham số khác (ghi chú 7); nên đối chiếu thêm: không còn chữ tiếng Anh trong `ui/` sau khi chuyển vào `strings.xml` (113 khóa, tiếng Việt và tiếng Anh), nhưng `formatDate` vẫn in ngày dạng ISO và `memberType`/`status` từ server vẫn là chữ thô.
11. **Vỡ layout ở B** do mình xem ảnh theo cùng tiêu chí với A (nhãn bị ngắt giữa chữ, hoặc chữ che nhau). Ở font 2.0, nhãn của menu và mọi màn không bị ngắt giữa chữ; nút nổi từng che dòng cuối danh sách và đã sửa bằng khoảng chừa 88 dp (commit `96a2895`). Ô tìm kiếm ở font 2.0 xuống hai dòng chứ không bị cắt. Cần người trong nhóm duyệt lại, như A.
12. **Android 17 (Xiaomi 25053RT47C, API 37, 1280x2772 ở 520 dpi, tiếng Việt)**, chỉ sáng và tối, 9 màn hình, chạy ở commit `5e0e5e9` đến `6676595`: ATF cũng ra 2 phát hiện (hai nút bị vô hiệu) ở cả hai chế độ, 0 trùng nhãn, 0 vùng chạm nhỏ. Không chạy font 2.0 và 320 dp trên máy cá nhân. Kết quả thô không đưa vào repo. Máy này chạy tiếng Việt, emulator chạy tiếng Anh, nên không so số phần tử giữa hai máy.
13. **Chưa đo ở B:** màn Tự mượn khi có NFC (chờ chạm thẻ, đang gửi, xong, lỗi: emulator và cả hai bản đo đều chỉ có "không có NFC"); skeleton, trạng thái rỗng, lỗi và offline (bài đo chỉ có trạng thái có dữ liệu); mô tả ảnh bìa với ảnh thật; TalkBack thật; kích thước APK release (font Nunito thêm 276.932 byte); hiệu năng khởi động, cuộn và bộ nhớ (§8.5, giai đoạn 6).
14. **A và B không so được ở thanh tab.** A dùng tab, B dùng menu ☰: số bước và thời gian của T2 và T3 chỉ đo được bằng bài test UX có người (`optimization/ux-test.md`), không phải bằng bảng này.
