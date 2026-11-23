# Nhật ký: docs/ui-ux-plan

- **Tách từ:** main tại 7763c7c
- **Mục đích:** kế hoạch thiết kế UI và đánh giá UX cho app thành viên Android
- **Môn:** optimization

## 2026-11-23 18:32 · Kế hoạch UI và UX cho ShelfTap

- **Vấn đề:** app `android/app` có giao diện thô có chủ ý: theme mặc định, chữ tiếng Anh viết cứng, tab không có icon, ảnh bìa không có mô tả (U01, U04, U05). UX chưa từng được đánh giá, dù đề cương của PM có nhắc ở phần mở rộng (mục 3, 4, 9, 13).
- **Thay đổi:** thêm `optimization/ui-ux-plan.md`: đối chiếu với đề cương của PM, hiện trạng, ràng buộc bảo mật và hiệu năng, thiết kế UI (§5), thiết kế UX (§6), cách đo A/B (§8), giai đoạn (§9), rủi ro và việc nhóm cần chốt.
- **Đo trước → sau:** chưa đo: đây là kế hoạch. Các bảng kết quả để trống ("Đo thực tế").
- **AI:** Claude Code (Claude Opus 5.5). AI đọc đề cương `.docx`, `SPEC.md`, taxonomy U01–U06, guardrail và code giao diện, rồi viết kế hoạch. Con người chưa duyệt.
- **Kiểm thử:** đối chiếu các nhận định về code với file nguồn (`ui/Common.kt`, `ui/AppRoot.kt`, `ui/MemberScreens.kt`, `res/values/`). Đã sửa hai chỗ sau khi kiểm tra: Lint `HardcodedText` không bắt chuỗi Compose; `FLAG_SECURE` có thể chặn kiểm tra contrast của Accessibility Scanner.
- **Ghi chú cho báo cáo:** §11 còn chờ nhóm chốt bảng màu, chế độ khách, người tham gia test và thẻ NFC thật.

## 2026-11-23 18:40 · Kế hoạch dùng nhật ký nhánh thay cho ui-ux-log.md

- **Vấn đề:** §8.4 và §12 của kế hoạch nhắc tới `optimization/ui-ux-log.md`, trùng với quy tắc mới là mỗi nhánh có một nhật ký ở `optimization/log/`.
- **Thay đổi:** §8.4 và §12 của `optimization/ui-ux-plan.md` trỏ tới `optimization/log/<tên-nhánh>.md`; thêm `optimization/log/README.md` và nhật ký này.
- **Đo trước → sau:** không áp dụng (tài liệu).
- **AI:** Claude Code (Claude Opus 5.5) theo yêu cầu ghi nhật ký bắt buộc của người dùng.
- **Kiểm thử:** không áp dụng.
- **Ghi chú cho báo cáo:** không có.

## 2026-11-23 18:39 · Sửa mục 2026-11-23 18:40

- **Vấn đề:** mục trên ghi giờ ước lượng, không phải giờ commit thật.
- **Thay đổi:** mục "18:40" thực ra là commit lúc 18:36. Nội dung mục không đổi.
- **Đo trước → sau:** không áp dụng.
- **AI:** Claude Code (Claude Opus 5.5) tự phát hiện khi đối chiếu với `git log`.
- **Kiểm thử:** không áp dụng.
- **Ghi chú cho báo cáo:** không có.

## 2026-11-23 18:57 · Hiệu ứng theo tầng thiết bị và màn hình tỷ lệ đặc biệt

- **Vấn đề:** người dùng muốn thanh điều hướng kiểu kính mờ (progressive blur) và icon morph (Morphicons), tối ưu cho Android 17 và máy có tỷ lệ màn hình đặc biệt. Blur đè lên danh sách đang cuộn là loại hiệu ứng trang trí mà E2 đã đo là tốn khung hình (P08), trái với mục tiêu máy giá rẻ nếu bật cho mọi máy.
- **Thay đổi:** kế hoạch thêm §6.3 (một app, ba tầng hiệu ứng Đầy đủ / Vừa / Nhẹ chọn lúc chạy; blur chỉ ở thanh điều hướng; Morphicons chỉ dùng lúc build vì là thư viện JavaScript), §6.4 (bố cục thích ứng: `WindowSizeClass`, `NavigationSuiteScaffold`, tràn viền, màn lớn), §8.5 (ma trận đo chi phí theo tầng và quy tắc nhận), giai đoạn 5b, ba rủi ro và ba mục cần chốt ở §11. Thêm một dòng đối chiếu đề cương PM (mục 3 và 5) ở §2.
- **Đo trước → sau:** chưa đo: đây là kế hoạch.
- **AI:** Claude Code (Claude Opus 5.5). Người dùng đề xuất bản riêng chỉ cho Android 17; AI đề xuất một app chia tầng thay vì bản riêng, người dùng đồng ý và yêu cầu đưa vào kế hoạch như một hạng mục tối ưu. AI không xác nhận được Android 17 có API glass riêng nên kế hoạch chỉ dùng API đó sau khi đọc tài liệu chính thức và đo được.
- **Kiểm thử:** kiểm tra `node_modules/morphicons` (bản 2.0.2, MIT, các entry React/Vue/Svelte/React Native/DOM) để xác nhận nó không chạy trong Kotlin.
- **Ghi chú cho báo cáo:** tầng Đầy đủ chờ image Android 17; ngưỡng các tầng chốt bằng số đo, không đoán trước.

## 2026-11-23 19:22 · Hướng thiết kế kính toàn app, menu ☰, icon morph và đồng bộ với hệ thống

- **Vấn đề:** người dùng chọn phong cách kính mờ dần cho cả app (theo một ảnh mẫu), muốn menu ☰ mở khung kính bo góc xếp dọc, icon morph ở nhiều chỗ, và lo người dùng so sánh hiệu ứng kính của app với của điện thoại.
- **Thay đổi:** kế hoạch thêm §5.5 (hướng thiết kế kính, 12 màn khớp với mockup https://claude.ai/artifact/GMkMmXLY2J2DP2MKf1yiGf, bìa chữ cho sách thiếu ảnh, font Be Vietnam Pro, giới hạn); §6.2 thay thanh 5 tab bằng menu ☰ cộng nút nổi tự mượn, và ghi rủi ro của menu ☰; §6.3 thêm bảng 7 chỗ dùng icon morph và bảng đồng bộ với hệ thống (`isCrossWindowBlurEnabled`, Material You, tương phản, hoạt ảnh); §6.4, §7, §8.5 đổi từ "thanh điều hướng" sang các lớp kính mới; §8.3 thêm câu hỏi về độ hợp với máy và so sánh thanh tab với menu ☰; §9 giai đoạn 3 dùng mockup; §10 thêm bốn rủi ro; §11 thêm hai quyết định đã chốt và hai việc cần chốt.
- **Đo trước → sau:** chưa đo: đây là kế hoạch. Mockup là trang web, không đo được hiệu năng Android.
- **AI:** Claude Code (Claude Opus 5.5). AI đọc ảnh mẫu, đề xuất cách áp vào các màn của ShelfTap, dựng mockup 12 màn bằng dữ liệu dev thật (tên sách, tác giả, chủ đề, ISBN, kệ) và dữ liệu mẫu có ghi chú cho ngày và tiền phạt. Người dùng chọn menu ☰ dạng khung kính dọc, yêu cầu thêm các màn đăng nhập, đăng xuất và màn thành viên, cho phép đăng mockup lên link công khai, và yêu cầu cập nhật kế hoạch. AI không xác nhận được có API để đọc hiệu ứng kính của từng hãng, nên kế hoạch chỉ đồng bộ theo tín hiệu chính thức.
- **Kiểm thử:** độ dài mật khẩu tối thiểu 10 ký tự lấy từ `MeController.PasswordRequest` của server. Mockup chưa được mở ra kiểm tra hiển thị.
- **Ghi chú cho báo cáo:** baseline của `opt/app-catalog` (commit `b7459dc`, Android 16) đã đạt mục tiêu cuộn và bộ nhớ, nên mọi lớp kính phải qua §8.5 trước khi nhận.

## 2026-11-24 03:55 · Trang danh sách sách và hiệu ứng động

- **Vấn đề:** kế hoạch và mockup chưa có trang danh sách sách ("Xem tất cả" ở Danh mục trỏ về chính nó), dù đây là màn cuộn 100k sách mà `opt/app-catalog` đo. Các màn trong mockup chưa có hiệu ứng động nào.
- **Thay đổi:** §5.5 thêm trang danh sách sách (thanh kính, chip chủ đề, hàng nền đặc không blur, các trạng thái) và ghi hai chỗ API hiện tại chưa có: kệ trong `TitleSummary` và tham số lọc theo chủ đề. Thêm §6.5 về hiệu ứng động: chỉ transform và opacity, hiệu ứng thành công chỉ sau khi server trả lời, không có hiệu ứng trong hàng lúc cuộn, hiệu ứng lặp phải dừng, tắt hết khi máy tắt hoạt ảnh, bảng 10 chỗ dùng hiệu ứng theo tầng. §8.5 đo thêm lúc chuyển màn, lúc tải trang đầu và kiểm tra màn đứng yên không vẽ thêm frame. §9 và §11 cập nhật (thêm mục 12 và 13). §6.3 ghi rằng thư mục npm ở gốc repo mới chỉ được ignore cục bộ.
- **Đo trước → sau:** chưa đo: đây là kế hoạch. Mockup là trang web, không đo được hiệu năng Android.
- **AI:** Claude Code (Claude Opus 5.5). Người dùng đồng ý thêm trang danh sách và hỏi có thể thiết kế hiệu ứng động phù hợp cho mockup không. AI dựng màn `List` bằng 13 sách dev thật, thêm hiệu ứng vào 13 màn của mockup (bản 10 của artifact), rồi viết các mục kế hoạch. AI chưa xác nhận tên bộ token chuyển động của Material 3 nên kế hoạch không ghi tên API đó.
- **Kiểm thử:** đối chiếu với `server/API.md`: `TitleSummary` có `copiesAvailable` nhưng không có kệ, `q` đã khớp chủ đề. Số bản còn lấy từ `V100__dev_catalog.sql`; trạng thái "đang được mượn" của Tắt đèn là dữ liệu mẫu. Chưa mở mockup ra xem hiệu ứng chạy thật.
- **Ghi chú cho báo cáo:** §6.5 là nơi giải thích vì sao hiệu ứng động không làm giảm điểm hiệu năng: chỉ `graphicsLayer`, tắt ở tầng Nhẹ, và được đo ở §8.5.

## 2026-11-24 03:58 · Thêm node_modules vào .gitignore

- **Vấn đề:** `node_modules/` của Morphicons ở gốc repo chưa bị git ignore, dễ bị commit nhầm cả nghìn file.
- **Thay đổi:** `.gitignore` thêm `node_modules/` (khớp ở mọi cấp thư mục). §6.3 của kế hoạch ghi việc này và việc còn lại: chuyển `package.json` và `package-lock.json` vào thư mục script build khi làm script đó.
- **Đo trước → sau:** không áp dụng (cấu hình git).
- **AI:** Claude Code (Claude Opus 5.5) theo yêu cầu của người dùng. AI không ignore `package.json` vì file đó cần được commit cùng script build sau này.
- **Kiểm thử:** `git check-ignore` trên cây của commit này (xem commit message).
- **Ghi chú cho báo cáo:** không có.

## 2026-11-24 04:09 · Bảy màn trạng thái, khách và mở rộng

- **Vấn đề:** so với `SPEC.md` §5 và code Android, mockup còn thiếu: màn mất kết nối (`OfflineScreen`), lý do đăng xuất (`LogoutReason`), cảnh báo máy không an toàn (`IntegrityBanner`), màn khởi động, chế độ khách, thông báo, tìm khi không có mạng và gợi ý sách.
- **Thay đổi:** §5.5 thêm hai nhóm màn vào bảng và mô tả từng màn, kèm quy tắc: màn khóa không hiện tên sách, giải thích trước khi xin quyền thông báo, không tự thử lại mạng liên tục, tìm offline không hiện số bản còn. §9 đổi sang mockup 20 màn. §11 thêm mục 14 (cơ chế báo sách đặt trước đã sẵn sàng, vì `server/API.md` chưa có).
- **Đo trước → sau:** chưa đo: đây là kế hoạch.
- **AI:** Claude Code (Claude Opus 5.5). Người dùng hỏi mockup còn thiếu màn nào so với kế hoạch ban đầu, rồi yêu cầu thêm tất cả. AI dựng 7 màn mới (Splash, Offline, SignedOut, Guest, Notifications, OfflineSearch, Recommend) và đổi link "Xem sách mà không cần đăng nhập" ở màn chào sang màn khách. Đăng lên link mockup bị chặn vì link công khai, đang chờ người dùng đồng ý.
- **Kiểm thử:** chạy script của cả 20 màn bằng Node, gọi mọi handler một lần và kiểm tra mọi biến trong template đều có giá trị. Lý do đăng xuất lấy từ `session/SessionState.kt`, hành vi cảnh báo máy lấy từ `ui/Common.kt`, việc API chưa có thông báo đẩy kiểm tra trong `server/API.md`. Chưa mở mockup ra xem.
- **Ghi chú cho báo cáo:** thông báo trên màn khóa là một điểm giao giữa UX và bảo mật: hữu ích nhưng không được lộ sách đã mượn.
