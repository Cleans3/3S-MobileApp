# Nhật ký: opt/ui-ux

- **Tách từ:** docs/ui-ux-plan tại bbb05e5
- **Mục đích:** làm giao diện và UX của app thành viên Android theo `optimization/ui-ux-plan.md`, đo A/B
- **Môn:** optimization

## 2026-11-23 19:20 · Baseline UI: gắn tag `ui-baseline`, đo cột A của §8.2

- **Vấn đề:** chưa có số liệu nào về UI hiện tại để so sánh. Các lỗi dự kiến: tab không có icon, chuỗi tiếng Anh viết cứng, theme mặc định (U01–U06, U04) mới chỉ là nhận định trong kế hoạch, chưa được đo.
- **Thay đổi:** không sửa app. Gắn tag `ui-baseline` vào `bbb05e5` (code Android giống `main` tại `7763c7c`). Thêm bài đo UI dùng lại được: `androidTest/.../uiaudit/UiAuditTest.kt` (chạy app thật với dữ liệu giả `AuditData`/`AuditVault`, kiểm tra bằng Accessibility Test Framework và chụp ảnh từng màn hình), `optimization/ui-ux/audit/run_audit.ps1` (chạy bốn cấu hình: light, dark, font 2.0, rộng 320 dp), thư viện chỉ cho `androidTest` trong `app/build.gradle.kts` và `libs.versions.toml` (ATF 4.1.1, Compose ui-test, Guava; không vào APK). Kết quả thô ở `optimization/ui-ux/audit/A/`, bảng và giới hạn ở `optimization/ui-ux-metrics.md`.
- **Đo trước → sau:** đây là phép đo A, chưa có B. Trên AVD `lowend36ui` (Android 16, 720x1280 ở 280 dpi, 2 nhân, 2 GB): ATF báo 4 phát hiện trên 7 màn hình (2 contrast, 2 trùng nhãn); 2 cặp màu dưới 4,5:1 (cả hai là nút bị vô hiệu); 0 vùng chạm dưới 48 dp; vỡ layout 5/7 màn hình ở font 2.0 (nhãn thanh dưới bị ngắt giữa chữ) và 0/7 ở rộng 320 dp (phần này do AI xem ảnh, chờ người duyệt); 51 chuỗi `Text("` viết cứng. Chưa đo: màn Tự mượn ở trạng thái chờ chạm thẻ (emulator không có NFC), ảnh bìa thật.
- **AI:** Claude Code (Opus 5.5, sau đó Sonnet 5.5; phiên đổi model giữa chừng). Prompt: yêu cầu của người dùng. AI đề xuất đo bằng Robolectric cho nhanh; thử thì ATF chỉ thấy khung View chứ không thấy phần tử Compose (1 phần tử mỗi màn hình, 0 phát hiện), nên AI bỏ cách đó và chuyển sang androidTest trên emulator. Lần đầu ATF cũng chỉ thấy DecorView khi truy vấn trong tiến trình; sau đổi sang lấy cây accessibility từ hệ thống qua UiAutomation thì thấy 17–67 phần tử mỗi màn hình. AI tự xem ảnh để đếm màn hình vỡ layout và ghi rõ chờ người duyệt. Con người chưa duyệt gì ở bước này.
- **Kiểm thử:** `./gradlew assembleDebug testDebugUnitTest` trên A: qua, 59 test unit. `run_audit.ps1 -Label A` chạy hai lượt đủ bốn cấu hình, số liệu giống nhau. Bài đo chạy trên một emulator riêng (`-Serial emulator-5556`) để không đụng emulator `lowend36` đang dùng cho phép đo catalog của phiên `opt/app-catalog`.
- **Ghi chú cho báo cáo:** công cụ là ATF, không phải app Accessibility Scanner; B phải dùng cùng công cụ. ATF báo contrast trên nút bị vô hiệu dù WCAG miễn trừ; nhóm quyết có đếm hay không trước khi đo B. Hai dạng lỗi dễ thấy ngay: nhãn tab vỡ ở font 2.0 và nhãn "Renew"/"Cancel" trùng nhau (đọc lên không biết sách nào).
## 2026-11-24 15:33 · Bài test UX: viết `optimization/ux-test.md` (giai đoạn 3)

- **Vấn đề:** §8.3 cần kịch bản, phiếu ghi và bảng kết quả trước khi tuyển người; chưa có file nào (Task flow, đo UX).
- **Thay đổi:** thêm `optimization/ux-test.md`: kịch bản đọc to cho T1–T3, điều kiện thành công và thất bại, phiếu ghi cho người quan sát (thời gian, lỗi, bước, số lần tìm menu), SUS 10 câu kèm cách tính, câu hỏi "hợp với điện thoại" cho B, bảng kết quả A/B **để trống**, giới hạn. Không sửa code.
- **Đo trước → sau:** không áp dụng, đây là tài liệu. Chưa chạy buổi test nào, không có số liệu.
- **AI:** Claude Code (Sonnet 5.5). Prompt: yêu cầu của người dùng. AI soạn toàn bộ chữ, gồm lời kịch bản, ngưỡng 90 s và 180 s, định nghĩa lỗi và bước. Con người chưa duyệt: các ngưỡng thời gian và tên cuốn sách cho T1 là chỗ nhóm cần chốt.
- **Kiểm thử:** không có (chỉ thêm file tài liệu).
- **Ghi chú cho báo cáo:** T2 cần thẻ NFC thật (§11 mục 4); nếu chưa có thì T2 không đo được, và file đã ghi sẵn điều này.

## 2026-11-24 15:36 · Chuỗi: chuyển toàn bộ chữ trong `ui/` sang `strings.xml` (tiếng Việt mặc định + tiếng Anh)

- **Vấn đề:** chữ tiếng Anh viết cứng trong Kotlin, không có `values-en`, ngày giờ và thông báo lỗi không theo ngôn ngữ máy (§5.2, localization). Cột A: 51 chuỗi `Text("` viết cứng.
- **Thay đổi:** `res/values/strings.xml` (tiếng Việt, mặc định) và `res/values-en/strings.xml` (113 khóa mỗi file, khóa giống nhau). Thêm `ui/UiText.kt` (id chuỗi + đối số, đổi thành chữ khi hiển thị) để ViewModel giữ thông báo mà không cần `Context`; `userMessage()`, `Load.Failed`, `ErrorText` và các `error`/`message` trong state đổi từ `String` sang `UiText`. Nhãn tab, lý do đăng xuất, tiêu đề hộp thoại sinh trắc học đọc từ resource. Bài đo `UiAuditTest` lấy chữ qua `getString`, nên chạy được ở cả hai ngôn ngữ. Ở `CatalogScreens.kt` chỉ đổi chuỗi và kiểu của `error`/`message` (nhánh `opt/app-catalog` cũng sửa file này, khi gộp sẽ phải giải quyết xung đột ở các dòng này).
- **Đo trước → sau:** `grep -rc 'Text("' ui/` từ 51 xuống 0. Đây là số trung gian bằng đúng lệnh của cột A, **chưa điền vào cột B**: B chỉ điền khi xong §5 và §6.2, đo lại bằng `run_audit.ps1`. Chưa đo: chữ dài hơn trong tiếng Việt có bị cắt hay chồng không (cần chạy `run_audit.ps1` ở font 2.0 và 320 dp sau khi xong các thay đổi layout).
- **AI:** Claude Code (Sonnet 5.5). Prompt: yêu cầu của người dùng. AI soạn toàn bộ bản dịch tiếng Việt. **Con người chưa duyệt bản dịch:** cần thủ thư hoặc người bản ngữ xem lại cách xưng hô ("bạn"), từ "đầu sách", "áp thẻ", "đặt trước". Chưa làm: `formatDate` vẫn in ngày dạng ISO (`2026-11-24`) và `memberType`/`status` từ server vẫn là chữ thô viết thường; tiền phạt `1.000 ₫` chưa có chỗ hiển thị trong app. Ghi vào việc còn lại, không coi là xong.
- **Kiểm thử:** `cd android && ./gradlew assembleDebug testDebugUnitTest compileDebugAndroidTestKotlin`: qua (59 test unit). Chưa chạy lại `run_audit.ps1` trên emulator, nên chưa xác nhận bằng mắt rằng màn hình hiển thị đúng chữ.
- **Ghi chú cho báo cáo:** máy đặt ngôn ngữ tiếng Anh sẽ thấy tiếng Anh, mọi ngôn ngữ khác thấy tiếng Việt. Khi đo B phải ghi rõ ngôn ngữ máy dùng cho bài đo, vì cột A đo ở tiếng Anh.

## 2026-11-24 15:37 · Accessibility: nút "Gia hạn"/"Hủy" nói rõ sách nào; thông báo kết quả tự mượn bằng liveRegion

- **Vấn đề:** U02 (nhãn trùng). Ở cột A, ATF báo 2 phát hiện `DuplicateSpeakableText`: các nút "Renew" ở Sách đang mượn và "Cancel" ở Đặt trước đọc giống nhau, không biết sách nào. Kết quả tự mượn (Đã mượn hay lỗi) không được TalkBack đọc ra khi hiện (U06, §5.3).
- **Thay đổi:** `MemberScreens.kt`: hai nút gắn `contentDescription` có tên sách ("Gia hạn <tên sách>", "Hủy đặt trước <tên sách>", chuỗi `loans_renew_for`, `reservations_cancel_for` ở cả hai ngôn ngữ). Tiêu đề "Đã mượn: …" và thông báo lỗi của màn Tự mượn đặt `liveRegion = Polite`. Chữ hiển thị không đổi.
- **Đo trước → sau:** chưa đo. Cột A: 2 phát hiện trùng nhãn trong 4. Cần chạy lại `run_audit.ps1` để biết còn bao nhiêu; làm một lần sau khi xong các thay đổi UI, để không đo nhiều lần cho cùng một cột B. TalkBack chưa thử trên máy thật.
- **AI:** Claude Code (Sonnet 5.5). Prompt: yêu cầu của người dùng. AI chọn gắn `contentDescription` có tên sách thay vì đổi chữ nút. Con người chưa duyệt.
- **Kiểm thử:** `assembleDebug testDebugUnitTest compileDebugAndroidTestKotlin`: qua.
- **Ghi chú cho báo cáo:** `liveRegion` chưa kiểm tra với TalkBack thật. Phần "nhãn tab vỡ ở font 2.0" (A: 5/7 màn) chưa sửa riêng: thanh 5 tab sẽ được thay bằng menu ☰ nếu nhóm đồng ý (`ui-ux-plan.md` §6.2), nên chưa sửa tạm.
