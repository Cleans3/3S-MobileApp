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
