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
