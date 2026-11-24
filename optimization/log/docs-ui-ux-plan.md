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

## 2026-11-24 04:18 · Font Nunito, theme sáng và tối, thẻ hiện lần lượt

- **Vấn đề:** người dùng thấy font Be Vietnam Pro trong mockup chưa ưng ý, và mockup chưa có theme tối. Hiệu ứng vào màn chỉ áp cho cả khối, nên thẻ trong băng sách không hiện lần lượt.
- **Thay đổi:** §5.1 chốt font Nunito và thêm quy tắc sáng/tối (token hai giá trị, theo máy hoặc chọn trong Tài khoản, nút chính đảo màu ở nền tối, kính tối bán trong, màu trạng thái sáng hơn, bìa sáng giữ chữ tối). §5.5 đổi dòng font. §6.3 bảng đồng bộ hệ thống ghi rằng người dùng có thể chọn cố định. §6.5 thêm dòng băng sách ở Danh mục.
- **Đo trước → sau:** chưa đo: đây là kế hoạch. Tương phản ở nền tối chưa kiểm bằng công cụ.
- **AI:** Claude Code (Claude Opus 5.5). AI giải thích nguyên nhân chính của cảm giác thô là độ đậm 700 ở cỡ lớn, dựng bảng so bốn font (Be Vietnam Pro, Inter, Plus Jakarta Sans, Nunito) ở nền sáng và tối, và nghiêng về Plus Jakarta Sans; người dùng chọn Nunito. AI đổi font và gom mọi màu viết cứng của 20 màn mockup thành biến CSS bằng một script có xét ngữ cảnh (nền, chữ, viền, nút chính), thêm nút Sáng/Tối cho từng màn và lựa chọn giao diện trong Tài khoản. Mockup chưa lên link vì việc đăng bị chặn, đang chờ người dùng đồng ý.
- **Kiểm thử:** chạy script của cả 20 màn bằng Node, gọi mọi handler và kiểm tra biến trong template. Chưa xem mockup ở nền tối bằng mắt.
- **Ghi chú cho báo cáo:** đây là ví dụ cho câu hỏi nghiên cứu về vai trò của AI: AI đề xuất, con người chọn font theo cảm nhận.

## 2026-11-24 04:25 · Đo tương phản của mockup ở hai theme

- **Vấn đề:** sau khi thêm theme tối, cần đo xem chữ có đạt tương phản 4.5:1 (WCAG 2.2 AA, U03) ở cả hai theme không. Khung của artifact chặn chạy code bên trong màn, nên không đo trực tiếp trên trang được.
- **Thay đổi:** §5.3 thêm cách đo, bảng kết quả, các lỗi đã sửa và giới hạn. Mockup đã sửa: link, nút và ô nhập lấy token `ink`; chữ gợi ý lấy `ink3`; nhãn trên bìa ở Danh mục, ghi chú ở Khởi động và nút bị khóa ở Đổi mật khẩu được nâng tương phản; dải tối ở màn chào đậm hơn và nút kính ở đó bớt trắng.
- **Đo trước → sau:** 353 đoạn chữ mỗi theme. Không đạt: sáng 9 → 0, tối 9 → 0. Chữ trên ảnh ở màn chào (trường hợp xấu nhất): tiêu đề 2.49 → 4.80 (cần 3), nút Bắt đầu 3.70 → 5.57 (cần 4.5), đoạn mô tả 4.47 → 7.03.
- **AI:** Claude Code (Claude Opus 5.5). Người dùng yêu cầu đo tương phản. AI thử chạy code trong khung của artifact (bị chặn), rồi viết script dựng từng màn từ mã nguồn với dữ liệu thật, thay token theo theme, chồng các lớp bán trong, mô phỏng màu mặc định của trình duyệt cho nút, ô nhập, placeholder và link. Script tìm ra một lỗi thật mà lúc xem bằng mắt ở lượt trước đã bỏ sót: link và nút không ghi màu bị đen trên nền tối. Bản đã sửa chưa lên link vì việc đăng bị chặn, đang chờ người dùng đồng ý.
- **Kiểm thử:** chạy lại script sau khi sửa (0 lỗi ở cả hai theme, ngoài chữ trên ảnh được tính riêng); chạy script của cả 20 màn để chắc không hỏng màn nào.
- **Ghi chú cho báo cáo:** đây là ví dụ đo trước/sau đúng nghĩa cho phần UI. Lỗi chữ đen trên nền tối là loại lỗi mà xem bằng mắt vài màn dễ bỏ sót, còn đo toàn bộ thì bắt được.

## 2026-11-24 04:28 · Đưa mã nguồn mockup và script đo tương phản vào repo

- **Vấn đề:** script đo tương phản và mã nguồn mockup chỉ nằm trong thư mục tạm của phiên làm việc, có thể bị xóa; không ai chạy lại được số đo ở §5.3 để viết báo cáo.
- **Thay đổi:** thêm `optimization/ui-ux-mockup/`: 20 màn `.dc.html` và `canvas.json` của bản 14, `tools/contrast.js` (đo tương phản), `tools/hero-worst-case.js` (chữ trên ảnh ở màn chào), `tools/check-scripts.js` (chạy script của từng màn), `contrast-results.json` và README (cách chạy, cách đo, giới hạn, kết quả). §5.3 trỏ tới thư mục này.
- **Đo trước → sau:** không đổi số đo; chạy lại từ thư mục mới cho đúng kết quả cũ: 353 đoạn chữ mỗi theme, sáng còn 5 (đều là chữ trên ảnh ở màn chào, tính riêng thì đạt), tối 0.
- **AI:** Claude Code (Claude Opus 5.5). Người dùng đồng ý chép script vào repo. AI chép thêm mã nguồn mockup vì script cần nó để chạy, viết thêm script tính chữ trên ảnh (trước đó chỉ là lệnh chạy một lần), sửa để kết quả ghi cạnh các màn.
- **Kiểm thử:** chạy cả ba script trong bố cục thư mục giống repo trước khi commit.
- **Ghi chú cho báo cáo:** số liệu tương phản trong báo cáo có thể chạy lại bằng ba lệnh trong README.

## 2026-11-24 04:32 · Đo tương phản cho icon, viền và công tắc

- **Vấn đề:** phép đo trước chỉ đo chữ (SC 1.4.3). Icon, viền ô nhập, công tắc và trạng thái đang chọn (SC 1.4.11, ngưỡng 3:1) chưa được đo.
- **Thay đổi:** thêm `optimization/ui-ux-mockup/tools/nontext-contrast.js`, dùng chung phần dựng màn của `tools/contrast.js` (giờ xuất hàm `audit` và ghi nhận thêm các mục không phải chữ). Mockup: công tắc ở Tài khoản và Thông báo làm theo kiểu Material 3 (rãnh tắt có viền `ink3`, token `switch-on` và `switch-knob-on` cho từng theme); viền ô nhập và khung ô tìm kiếm đổi sang 50% của màu chữ. `tools/hero-worst-case.js` tính thêm icon thẻ trên ảnh. Cập nhật README, §5.3 và hai file kết quả JSON.
- **Đo trước → sau:** 37 mục bắt buộc mỗi theme; không đạt: sáng 13 → 0, tối 12 → 0 (icon thẻ trên ảnh ở màn chào tính riêng: 3.70 → 5.57). 62 icon cạnh chữ đều đạt. Mép nút kính (khuyến nghị) vẫn phần lớn dưới 3:1, giữ nguyên theo chủ ý. Phép đo chữ không đổi.
- **AI:** Claude Code (Claude Opus 5.5). Người dùng yêu cầu đo thêm icon và viền. AI chia mục bắt buộc và mục khuyến nghị theo SC 1.4.11, tìm ra công tắc và ô nhập không đạt, sửa theo kiểu Material 3 rồi đo lại. Một lỗi của chính script (ô tìm kiếm có nền `transparent` bị tính là có nền riêng) được phát hiện và sửa trong lúc đo.
- **Kiểm thử:** chạy lại cả bốn script; script của 20 màn không lỗi; phép đo chữ vẫn cho đúng kết quả cũ.
- **Ghi chú cho báo cáo:** công tắc và ô nhập là loại lỗi tương phản mà nhìn bằng mắt dễ cho là "ổn" vì ta biết đó là công tắc; đo bằng số cho thấy rãnh tắt chỉ 1.6:1.

## 2026-11-24 04:35 · Viền cho ô tìm kiếm ở màn Khách

- **Vấn đề:** ở lượt trước, phần sửa viền cho ô tìm kiếm ở màn Khách không được áp dụng: chuỗi cần thay có hai dấu cách sau `background:` nên không khớp, và script sửa không kiểm tra lần thay đó. Ô này là link mở màn Danh sách, trông như ô nhập, nên không nằm trong nhóm bắt buộc, nhưng viền nhạt hơn các ô tìm kiếm khác (1.39 ở nền sáng).
- **Thay đổi:** `optimization/ui-ux-mockup/Guest.dc.html` dùng viền 50% của màu chữ như các ô tìm kiếm khác và có `aria-label="Tìm sách"` (trước đó trình đọc màn hình đọc chữ gợi ý "Tên sách, tác giả, ISBN" như tên link). README cập nhật bản 16 và số mục khuyến nghị.
- **Đo trước → sau:** mép ô ở màn Khách: sáng 1.39 → 3.15, tối 1.51 → 4.74. Mép nút và link (khuyến nghị) không đạt: sáng 79 → 78, tối 84 → 83. Mục bắt buộc và phép đo chữ không đổi.
- **AI:** Claude Code (Claude Opus 5.5). Người dùng giao AI chọn cách làm tối ưu nhất. AI tìm ra vì sao lần sửa trước không áp dụng, sửa lại và thêm tên cho link.
- **Kiểm thử:** chạy lại bốn script; 20 màn không lỗi.
- **Ghi chú cho báo cáo:** lỗi "sửa mà không áp dụng" là lý do mọi phép thay trong script sửa phải kiểm tra số lần khớp.

## 2026-11-24 04:36 · Sửa mục 2026-11-24 04:35

- **Vấn đề:** mục trên ghi sai số đo trước khi sửa của ô tìm kiếm ở màn Khách. Các số 1.39 (sáng) và 1.51 (tối) là của nút "Chi tiết" và nút "Đăng nhập bằng thẻ" trên cùng màn, không phải của ô tìm kiếm.
- **Thay đổi:** số đúng của mép ô tìm kiếm ở màn Khách: sáng 1.2 → 3.15, tối 2.37 → 4.74. Các nội dung khác của mục trên không đổi.
- **Đo trước → sau:** lấy bằng cách chạy `tools/nontext-contrast.js` trên commit `044dca8` (trước khi sửa) và `cc5918c` (sau khi sửa).
- **AI:** Claude Code (Claude Opus 5.5) tự phát hiện khi đối chiếu lại số đo với commit trước.
- **Kiểm thử:** không áp dụng.
- **Ghi chú cho báo cáo:** không có.

## 2026-11-24 04:40 · Rà lại mockup: lối vào còn thiếu và vùng chạm

- **Vấn đề:** người dùng yêu cầu kiểm tra xem còn thiếu gì. Rà bằng script cho thấy: màn Gợi ý cho bạn và cài đặt Thông báo không có màn nào dẫn tới; 32 trong 133 nút, link và công tắc có vùng chạm dưới 48 px (công tắc 52×32, chip và tab 40, "Không quan tâm" 36, vài nút 44), và 4 link chữ không có padding. Kế hoạch còn câu "thêm icon cho 5 tab" dù thanh 5 tab đã bỏ, danh sách menu chưa có Gợi ý, và §12 chưa liệt kê thư mục mockup.
- **Thay đổi:** mockup: mọi nút, link và công tắc có vùng chạm ít nhất 48 × 48 px bằng lớp `::after` trong suốt (hình giữ nguyên), hàng chip cuộn ngang thêm padding để không cắt vùng chạm; menu ☰ thêm "Gợi ý cho bạn"; Tài khoản thêm dòng "Thông báo". Thêm `tools/target-size.js`; `tools/contrast.js` xuất thêm các hàm dựng màn. Kế hoạch: sửa §5.3 (icon menu thay 5 tab, thêm kết quả vùng chạm), §5.5 (lối vào Gợi ý và Thông báo), §6.2 (danh sách menu, menu của khách), §12 (thư mục mockup). README lên bản 17.
- **Đo trước → sau:** vùng chạm dưới 48 px: 32 (+4 không rõ) → 0. Liên kết hỏng: 0 → 0. Màn không có lối vào ngoài các màn hệ thống tự mở (Khởi động, Đăng nhập lại): 2 → 0. Tương phản chữ và tương phản bắt buộc của icon và viền không đổi (thêm 3 đoạn chữ mới, đều đạt).
- **AI:** Claude Code (Claude Opus 5.5). AI viết các kiểm tra tự động (bản đồ liên kết giữa các màn, kích thước vùng chạm) rồi đọc lại kế hoạch tìm chỗ cũ. Chọn mở rộng vùng chạm thay vì phóng to hình để giữ thiết kế, theo đúng cách Compose làm.
- **Kiểm thử:** chạy lại năm script; 20 màn không lỗi.
- **Ghi chú cho báo cáo:** vùng chạm là ví dụ tốt cho việc kiểm bằng script trên toàn bộ màn: mắt người thường không thấy nút 40 px là "nhỏ".

## 2026-11-24 04:47 · Thử bố cục ở rộng 320 dp và cỡ chữ 2.0

- **Vấn đề:** §5.3 và §6.4 yêu cầu không vỡ bố cục ở màn rộng 320 dp và font scale 2.0, nhưng mockup chưa được thử.
- **Thay đổi:** thêm `tools/render-static.js` (dựng 20 màn thành HTML tĩnh ở bốn biến thể) và `tools/layout-check.js` (đếm chữ tràn mép, tràn đáy, chữ đè chữ, chữ bị cắt). Mockup sửa ba lỗi có cả ở cỡ chữ thường: ô nhập có `min-width: 0`, băng sách ở Danh mục cuộn ngang được, dải "Không có mạng" không còn đè dòng đếm. Kế hoạch §6.4 thêm bảng kết quả và bốn quy tắc bố cục cho app (màn cuộn được, thanh nổi chừa chỗ, chữ trong nút viên thuốc xuống dòng, ghi chú đáy nằm trong luồng cuộn). README lên bản 18.
- **Đo trước → sau:** gốc 3 lỗi (3 màn) → 0. Rộng 320: 0 → 0. Chữ 2.0: 13/20 màn, 41 lỗi; rộng 320 và chữ 2.0: 18/20 màn, 65 lỗi (không sửa trên mockup, chuyển thành quy tắc cho app).
- **AI:** Claude Code (Claude Opus 5.5). Người dùng yêu cầu kiểm tra cỡ chữ phóng to và màn rộng 320 dp. Khung artifact không cho chạy code bên trong, nên AI dựng màn ra HTML tĩnh, phục vụ qua web server cục bộ và đo trong trình duyệt của app. Bảng phóng chữ phi tuyến của Android 14 lấy theo trí nhớ của AI, đã ghi rõ là cần đối chiếu mã AOSP. Hai lượt đo đầu có kết quả dương tính giả từ nhãn ẩn 1 px cho trình đọc màn hình; đã sửa script để bỏ qua.
- **Kiểm thử:** chạy lại năm script Node (20 màn không lỗi, tương phản và vùng chạm không đổi) và đo lại bốn biến thể sau khi sửa.
- **Ghi chú cho báo cáo:** số liệu "màn vỡ bố cục ở font scale 2.0" của bảng §8.2 giờ có mốc trên mockup (13/20) để so với app sau khi làm.

## 2026-11-24 04:53 · Băng "Gợi ý cho bạn" trên Danh mục

- **Vấn đề:** màn Gợi ý chỉ mở được từ menu ☰, dễ bị bỏ qua; phần B4 của `optimization/search-recommend-plan.md` đặt băng gợi ý ngay trên Danh mục. Danh mục là màn cố định 844 px, không còn chỗ.
- **Thay đổi:** `Catalog.dc.html`: nội dung cuộn dọc (chừa 150 px ở đáy cho nút nổi, khối con không bị co); băng "Gợi ý cho bạn" dưới banner chủ đề với 4 sách như màn Gợi ý, mỗi thẻ là bìa chữ (trang trí) và lý do ở dạng chữ bên dưới, tên sách đọc qua nhãn ẩn; "Xem tất cả" mở màn Gợi ý; một dòng ghi "Tính trên máy từ sách bạn đã mượn". `tools/layout-check.js`: không tính chữ trong vùng cuộn đi qua dưới thanh nổi là chữ đè. Kế hoạch §5.5 và §6.4, README lên bản 19.
- **Đo trước → sau:** bản đầu của băng (lý do đè lên bìa) gây 8 lỗi ở chữ 2.0 trên Danh mục; bản cuối (lý do dưới bìa): 0. Toàn bộ, chữ 2.0: 13 màn và 41 lỗi → 10 màn và 30 lỗi; rộng 320 và chữ 2.0: 18 màn và 65 lỗi → 16 màn và 55 lỗi (một phần nhờ script chính xác hơn, ghi rõ trong §6.4). Gốc và rộng 320: 0 lỗi. Tương phản chữ: 367 đoạn mỗi theme, tối 0 lỗi, sáng chỉ còn chữ trên ảnh ở màn chào (tính riêng thì đạt). Vùng chạm: 140 nút, 0 dưới 48 px.
- **AI:** Claude Code (Claude Opus 5.5). Người dùng hỏi có nên thêm phần gợi ý vào Danh mục; AI đề xuất thêm (theo B4 và rủi ro menu ☰ dễ bị bỏ qua), người dùng chọn thêm vào Danh mục. Bản đầu đặt lý do trên nhãn kính đè lên bìa như băng sách chủ đề; phép đo bố cục cho thấy lý do và tên sách dài đè nhau ở chữ 2.0, nên AI chuyển lý do xuống dưới bìa. Một lượt đo có kết quả sai ở màn Thông báo vì font chưa tải xong; đo lại thì hết.
- **Kiểm thử:** chạy lại năm script Node và bốn biến thể bố cục trong trình duyệt.
- **Ghi chú cho báo cáo:** ví dụ đo trước/sau ở mức một thành phần: cùng nội dung, đổi vị trí chữ lý do làm số lỗi ở chữ 2.0 từ 8 về 0.

## 2026-11-24 04:58 · Sửa mục 2026-11-24 04:47 và 04:53

- **Vấn đề:** hai mục trên ghi "README lên bản 18" và "README lên bản 19", như thể artifact được đăng hai lần. Thực tế phần sửa bố cục ở 320 dp và chữ 2.0 không được đăng riêng; artifact đi thẳng từ bản 17 lên bản 18, và bản 18 chứa cả phần sửa bố cục lẫn băng "Gợi ý cho bạn".
- **Thay đổi:** `optimization/ui-ux-mockup/README.md`: các file là bản 18, gồm cả hai thay đổi. Bảng 320/2.0 so với "lượt đo đầu" (bản 17 cộng ba lỗi bố cục đã sửa, trước khi thêm băng) thay vì "bản 18", và đoạn dưới bảng nói "trong bản 18" thay vì "từ bản 19". Số đo không đổi.
- **Đo trước → sau:** không áp dụng.
- **AI:** Claude Code (Claude Opus 5.5) tự phát hiện khi đối chiếu README với số bản mà trang artifact báo sau lần đăng.
- **Kiểm thử:** không áp dụng.

## 2026-11-24 05:07 · Thử mockup ở các màn tỷ lệ đặc biệt

- **Vấn đề:** người dùng hỏi mockup đã tối ưu cho các điện thoại có tỷ lệ màn đặc biệt ra mắt năm nay chưa. Kế hoạch §6.4 đã có quy tắc (WindowSizeClass, tràn viền, màn rộng từ 600 dp), nhưng mockup chỉ mới đo ở 390 và 320 dp, chiều cao cố định 844.
- **Thay đổi:**
  - `tools/render-static.js`: thêm chiều cao và bảy cỡ màn: 21:9 (412 × 960), màn ngoài 10:16 của máy gập ngang (480 × 768), màn phụ máy gập dọc (360 × 400), chia đôi màn hình (390 × 420), xoay ngang (844 × 390), máy gập mở (720 × 840 và 960 × 720). Script báo lỗi nếu không tìm thấy kích thước gốc của màn.
  - `tools/layout-check.js`: tắt hiệu ứng động trước khi đo; thêm hai loại cho màn rộng: dòng chữ trung bình quá 80 ký tự, và khối rộng quá 85% màn khi màn rộng hơn 600 dp.
  - Kế hoạch §6.4: bảng kết quả, hai quy tắc mới cho app (cửa sổ rộng dùng menu cạnh trái và hai khung, giới hạn cột chữ; nút nổi thu nhỏ ở màn thấp, là đề xuất), thêm màn phụ máy gập dọc và chia đôi màn hình vào danh sách cấu hình kiểm tra. §11 thêm mục 15. README thêm mục "Màn tỷ lệ đặc biệt".
  - Không sửa file `.dc.html` nào; artifact không đổi.
- **Đo trước → sau:** đây là phép đo mới, chưa có số trước. Màn vỡ bố cục: 21:9 0/20; màn ngoài 10:16 1/20; màn phụ máy gập dọc 15/20 (84 lỗi); chia đôi 15/20 (85); xoay ngang 14/20 (78); máy gập mở 0/20 và 4/20 (7). Màn bị kéo giãn ở màn rộng: 17 đến 18 trên 20. Bốn biến thể cũ đo lại vẫn ra 0, 0, 30 và 55 lỗi.
- **AI:** Claude Code (Claude Opus 5.5). Người dùng chọn đo thêm. AI tra web về máy gập năm 2026; các nguồn (trang Samsung, HotHardware, Android Authority) không khớp nhau về độ phân giải và màn nào là 4:3, nên AI đo theo nhóm kích thước và ghi rõ là cần đối chiếu trên máy thật. Lượt đo đầu báo 4 lỗi giả ở màn Thông báo do bảng hỏi quyền đang trượt lên; AI tắt hiệu ứng trong script. Kiểm tra "dòng quá dài" ban đầu chỉ tính đoạn từ hai dòng nên bỏ sót dòng đơn dài 150 ký tự trên màn rộng; AI sửa điều kiện và thêm phép đếm khối bị kéo giãn.
- **Kiểm thử:** `node tools/check-scripts.js .` (20 màn không lỗi); dựng 11 biến thể, phục vụ qua web server cục bộ và chạy `tools/layout-check.js` trong trình duyệt của app.
- **Ghi chú cho báo cáo:** màn thấp và chữ 2.0 vỡ cùng một nguyên nhân (khối đặt ở vị trí cố định, màn không cuộn), nên một quy tắc sửa cả hai; số đo này là mốc để so với app khi làm.

## 2026-11-24 08:32 · Mockup Danh mục cho cửa sổ rộng

- **Vấn đề:** phép đo ở các màn tỷ lệ đặc biệt cho thấy 17 đến 18 trên 20 màn bị kéo giãn khi cửa sổ rộng hơn 600 dp, và mockup chưa có màn nào cho cửa sổ rộng (mục 15 ở §11). Người dùng chọn vẽ màn rộng.
- **Thay đổi:**
  - Thêm `WideCatalog.dc.html` (960 × 720, cửa sổ từ 840 dp): menu cố định bên trái với nút "Chạm sách để mượn" ở đáy; danh sách ở giữa (ô tìm, chip chủ đề, tìm không dấu, cùng 13 sách dev như màn Danh sách); chi tiết sách bên phải, bấm sách thì khung phải đổi theo, nút "Đặt trước" có đủ trạng thái như màn Chi tiết.
  - Thêm `RailCatalog.dc.html` (720 × 840, cửa sổ 600–840 dp): thanh điều hướng dọc 96 dp với nút "Chạm để mượn", một khung nội dung rộng tối đa 560 dp (4 thẻ gợi ý, lưới sách 2 cột).
  - `canvas.json`: hàng mới "Cửa sổ rộng".
  - `tools/render-static.js`: màn không phải 390 × 844 giữ kích thước vẽ, chỉ dựng ở biến thể gốc và chữ 2.0.
  - `tools/layout-check.js`: so chữ đè trên phần chữ thật sự hiện ra (cắt theo khung cuộn hoặc khung ẩn phần tràn).
  - Kế hoạch: §5.5 thêm nhóm màn "Cửa sổ rộng"; §6.4 tách cửa sổ vừa (600–840 dp, thanh dọc) và rộng (từ 840 dp, menu cố định và hai khung), thêm phần mô tả hai màn và kết quả đo; §11 mục 15 ghi đã vẽ; §9 và §12 đổi số màn thành 22. README thêm mục "Cửa sổ rộng". Kết quả `contrast-results.json` và `nontext-results.json` chạy lại, có thêm hai màn.
- **Đo trước → sau:** hai màn mới, ở chữ thường và chữ 2.0: 0 lỗi vỡ, 0 dòng quá 80 ký tự, 0 khối bị kéo giãn (bố cục điện thoại kéo ra 960 dp: 18 trên 20 màn bị kéo giãn). Lượt đầu của `WideCatalog` ở chữ 2.0 có 3 mục menu đè nhau; sau khi cho danh sách mục trong menu cuộn được: 0. Tương phản chữ 505 đoạn mỗi theme, tối 0 lỗi, sáng chỉ còn 5 đoạn chữ trên ảnh ở màn chào như trước. Mục bắt buộc về icon và viền: 42, chỉ còn icon trên ảnh ở màn chào (tính riêng thì đạt). Vùng chạm: 189 nút, 0 dưới 48 px. Các biến thể điện thoại đo lại không đổi.
- **AI:** Claude Code (Claude Opus 5.5). AI chọn cách chia theo nhóm cỡ cửa sổ của Material 3 (thanh dọc ở cỡ vừa, menu cố định và hai khung ở cỡ rộng) thay cho quy tắc cũ "từ 600 dp dùng menu cố định", vì ở 720 dp ba cột menu, danh sách và chi tiết quá chật. Lượt đo đầu báo 2 lỗi chữ đè giả (chip trong hàng cuộn ngang bị tính là đè lên khung chi tiết dù phần tràn đã bị cắt); AI sửa script thay vì sửa màn. Phiên bị ngắt giữa chừng do hết lượt dùng; phần tài liệu và commit làm ở lượt sau.
- **Kiểm thử:** `node tools/check-scripts.js .` (22 màn không lỗi), `tools/contrast.js`, `tools/nontext-contrast.js`, `tools/target-size.js`; dựng 11 biến thể và chạy `tools/layout-check.js` trong trình duyệt của app.
- **Ghi chú cho báo cáo:** cùng nội dung, đổi từ bố cục điện thoại kéo giãn sang bố cục theo cỡ cửa sổ làm số màn bị kéo giãn ở 960 dp từ 18/20 về 0 trên màn Danh mục.

## 2026-11-24 08:52 · Đăng hai màn cửa sổ rộng

- **Vấn đề:** hai màn `WideCatalog` và `RailCatalog` đã có trong repo nhưng chưa có trên link mockup. Lần đăng đầu bị chế độ tự động chặn; người dùng đổi chế độ quyền rồi yêu cầu đăng lên link mockup.
- **Thay đổi:** đăng `canvas.json` và hai màn mới lên artifact (bản 21). `optimization/ui-ux-mockup/README.md` ghi các file là bản 21.
- **Đo trước → sau:** không áp dụng. Sau khi đăng, tải về cả 23 file đang đăng và so với `docs/ui-ux-plan`: trùng từng byte.
- **AI:** Claude Code (Claude Opus 5.5). Trang artifact báo bản 21 dù trước đó là bản 18 và chỉ đăng một lần; AI không biết bản 19 và 20 từ đâu ra, nên ghi rõ điều đó và kiểm bằng cách so file thay vì suy đoán.
- **Kiểm thử:** so sha của 23 file tải về với blob trên nhánh.

## 2026-11-24 09:01 · Màn rộng cho mọi chức năng

- **Vấn đề:** người dùng thấy bản cửa sổ rộng mới có Danh mục, còn các chức năng khác thì chưa. Rà lại: 17 trên 20 màn điện thoại chưa có bản rộng, và các màn rộng chưa được đo ở điện thoại xoay ngang (844 × 390, theo Material 3 cũng là cửa sổ rộng). Người dùng chọn vẽ đủ 10 màn.
- **Thay đổi:**
  - 10 màn mới 960 × 720: `WideLoans`, `WideReservations`, `WideHistory`, `WideRecommend`, `WideOfflineSearch`, `WideGuest` (danh sách và chi tiết), `WideAccount` (gộp Thông báo, cài đặt hai khung), `WideSignIn` (gộp Màn chào, Đăng nhập, Thẻ, Đổi mật khẩu), `WideStatus` (gộp Khởi động, Khóa, Mất kết nối, Đăng xuất kèm lý do), `WideCheckout`. Nội dung, câu chữ và trạng thái lấy từ màn điện thoại tương ứng. Hộp xác nhận nằm trong khung chi tiết thay cho bảng trượt.
  - `tools/wide-boards/`: script sinh 10 màn trên từ một khung chung (menu, hai khung, thanh xem trước, theme). Chạy lại từ thư mục repo ra đúng từng byte các file đã ghi.
  - Cửa sổ rộng mà hẹp hoặc thấp (dưới 900 dp rộng hoặc 480 dp cao): menu thu thành thanh dọc 72 dp chỉ có icon, làm bằng container query; `WideCatalog` sửa theo, và menu của nó trỏ sang các màn rộng.
  - `canvas.json`: thêm ba hàng cho màn rộng.
  - `tools/render-static.js`: dựng thêm các màn `Wide*` ở 844 × 390; bỏ chữ `<#root>` bị in lên đầu mỗi màn.
  - `tools/layout-check.js`: chữ trong vùng cuộn không còn bị tính là "bị cắt".
  - Kế hoạch §5.5, §6.4 (bảng 10 màn, hộp xác nhận trong khung, vị trí ăng-ten NFC, thu menu khi cửa sổ thấp, kết quả đo, công tắc "Gợi ý theo lịch sử đọc" còn thiếu ở màn Tài khoản điện thoại), §9, §11 mục 15, §12. README thêm bảng 12 màn rộng và kết quả.
- **Đo trước → sau:** lượt đầu có 2 lỗi ở kích thước vẽ và chữ 2.0 (khung bao màn Trạng thái tính là kéo giãn; ghi chú màn Tự mượn tràn đáy ở chữ 2.0); ở 844 × 390 có 6 màn cắt chữ vì khung chi tiết chỉ còn 244 dp. Sau khi thêm phần thu gọn và sửa: 12 màn rộng 0 lỗi ở kích thước vẽ và chữ 2.0, 11 màn từ 840 dp 0 lỗi ở 844 × 390. Tương phản chữ 862 đoạn mỗi theme, tối 0 lỗi, sáng 5 (chữ trên ảnh ở màn chào như trước); icon và viền bắt buộc 51, 1 lỗi cũ ở màn chào; 310 nút, 0 dưới 48 px. Màn phụ máy gập dọc và chia đôi màn hình đo lại ra 85 và 84 (ghi trước là 84 và 85).
- **AI:** Claude Code (Claude Opus 5.5). AI đề xuất gộp theo kiểu bố cục để mỗi chức năng có một màn rộng (10 màn thay vì 17), người dùng chọn vẽ đủ 10. AI dùng script sinh vì 10 màn chung khung, tránh chép tay. Phần thu menu khi cửa sổ thấp là AI thêm sau khi đo ở 844 × 390, theo hướng dẫn thanh điều hướng dọc của Material 3. Tên API vị trí ăng-ten NFC lấy theo trí nhớ, đã ghi cần đối chiếu. Hai số đo của màn điện thoại lệch một lỗi so với lần ghi trước; AI dựng lại từ commit trước, đo bằng script hiện tại cũng ra số mới, nên ghi số mới và nói rõ.
- **Kiểm thử:** `node tools/check-scripts.js .` (32 màn không lỗi), `tools/contrast.js`, `tools/nontext-contrast.js`, `tools/target-size.js`; dựng 11 biến thể và chạy `tools/layout-check.js` hai lượt trong trình duyệt của app, kết quả giống nhau; xem bằng mắt `WideLoans` ở 844 × 390, `WideAccount`, `WideSignIn`, `WideStatus`.
- **Ghi chú cho báo cáo:** khung chi tiết 244 dp ở điện thoại xoay ngang là ví dụ cho việc chỉ dựa vào bề rộng (từ 840 dp là "rộng") chưa đủ; cần xét cả chiều cao.

## 2026-11-24 09:04 · Đăng 10 màn rộng

- **Vấn đề:** 10 màn rộng mới và phần thu gọn của `WideCatalog` đã có trong repo nhưng chưa có trên link mockup. Người dùng yêu cầu đăng lên link mockup.
- **Thay đổi:** đăng `canvas.json`, `WideCatalog` và 10 màn mới lên artifact (bản 23). `optimization/ui-ux-mockup/README.md` ghi các file là bản 23.
- **Đo trước → sau:** không áp dụng. Sau khi đăng, tải về cả 33 file đang đăng và so với `docs/ui-ux-plan`: trùng từng byte.
- **AI:** Claude Code (Claude Opus 5.5). Trang artifact báo bản 23 dù trước đó là bản 21 và chỉ đăng một lần, giống lần đăng trước (18 lên 21); AI ghi lại và kiểm bằng cách so file.
- **Kiểm thử:** so 33 file tải về với blob trên nhánh.

## 2026-11-24 09:24 · Nút thu gọn menu và màn cỡ vừa cho mọi chức năng

- **Vấn đề:** người dùng hỏi vì sao menu ở cửa sổ từ 840 dp không thu gọn được như thanh dọc ở 600–840 dp, và chỉ ra rằng cỡ 600–840 dp mới có một màn (`RailCatalog`). Người dùng chọn làm cả hai.
- **Thay đổi:**
  - Nút thu gọn ở đầu menu của 11 màn `Wide*` (48 × 48 dp, `aria-expanded`, tên đổi theo trạng thái). Bấm thì menu thu thành thanh dọc 72 dp, danh sách và chi tiết dịch sang trái. Nút ẩn khi cửa sổ đã tự thu gọn. `WideCatalog` sửa tay theo cùng quy tắc.
  - 11 màn `Mid*` 720 × 840: `MidCatalog` (danh sách rồi chi tiết sách), `MidLoans`, `MidReservations`, `MidHistory`, `MidRecommend`, `MidOfflineSearch`, `MidGuest`, `MidAccount`, `MidSignIn`, `MidStatus`, `MidCheckout`. Thanh dọc 96 dp, một khung mỗi lúc với nút quay lại, nội dung rộng tối đa 528 dp.
  - `tools/wide-boards/`: mỗi màn dựng hai lần (rộng và vừa) từ cùng khung; `gen.js` ghi thêm các màn `Mid*` và ba hàng mới trong `canvas.json`.
  - `RailCatalog`: thanh dọc, sách và "Xem tất cả" trỏ sang các màn `Mid*` thay cho màn điện thoại.
  - `tools/render-static.js`: thêm 4 biến thể (`navmini`, `middetail`, `mid600`, `mid600d`) để đo cả trạng thái menu thu gọn, khung chi tiết cỡ vừa và cạnh hẹp 600 dp của cỡ vừa.
  - Giới hạn độ rộng đoạn chữ ở màn rộng và vừa: 40em xuống 36em.
  - README mockup và kế hoạch §5.5, §6.4, §9, §11 mục 15, §12: 43 màn, mục "Cửa sổ vừa", kết quả đo.
- **Đo trước → sau:** trước: cỡ 600–840 dp có 1 màn, trạng thái menu thu gọn và khung chi tiết cỡ vừa không được đo. Sau, lượt đầu: 1 lỗi (ghi chú ở Sách đang mượn khoảng 86 ký tự một dòng, gặp ở `navmini` và `middetail`). Sau khi sửa: 23 màn rộng và vừa 0 lỗi ở cả 7 biến thể có màn lớn, đo hai lượt cùng kết quả. Tương phản chữ 1212 đoạn mỗi theme (trước 862), chỉ còn 5 lỗi cũ ở màn chào; icon và viền bắt buộc 67 (trước 51), 1 lỗi cũ; 455 nút (trước 310), 0 có vùng chạm dưới 48 px. 20 màn điện thoại không đổi.
- **AI:** Claude Code (Claude Opus 5.5). AI chọn một khung mỗi lúc và nút quay lại cho cỡ vừa theo cách Material 3 chia cỡ cửa sổ, và giới hạn 528 dp để giữ khoảng 80 ký tự một dòng. AI thêm các biến thể đo mới sau khi nhận ra bản dựng tĩnh chỉ có trạng thái đầu (menu mở, khung danh sách), nên hai trạng thái mới sẽ không được đo nếu không thêm.
- **Kiểm thử:** `node tools/wide-boards/gen.js .`, `node tools/check-scripts.js .` (43 màn không lỗi), `tools/contrast.js`, `tools/nontext-contrast.js`, `tools/target-size.js`; dựng 15 biến thể, chạy `tools/layout-check.js` hai lượt trong trình duyệt của app; xem bằng mắt `WideAccount` với menu thu gọn và `MidAccount` ở khung chi tiết 600 dp. So với nhánh: 20 màn điện thoại không đổi.

## 2026-11-24 09:29 · Đăng nút thu gọn menu và màn cỡ vừa

- **Vấn đề:** nút thu gọn menu ở 11 màn rộng và 11 màn cỡ vừa `Mid*` đã có trong repo nhưng chưa có trên link mockup. Người dùng yêu cầu đăng lên link mockup.
- **Thay đổi:** đăng `canvas.json`, 11 màn `Wide*`, `RailCatalog` và 11 màn `Mid*` lên artifact (bản 25). `optimization/ui-ux-mockup/README.md` ghi các file là bản 25.
- **Đo trước → sau:** không áp dụng. Sau khi đăng, tải về cả 44 file đang đăng và so với `docs/ui-ux-plan`: trùng từng byte.
- **AI:** Claude Code (Claude Opus 5.5). Trang artifact báo bản 25 dù trước đó là bản 23 và chỉ đăng một lần, giống hai lần đăng trước; AI ghi lại và kiểm bằng cách so file.
- **Kiểm thử:** so hash của 44 file tải về với blob trên nhánh.

## 2026-11-24 09:33 · Tối ưu hiệu ứng thẻ và chuyển trang

- **Vấn đề:** người dùng yêu cầu tối ưu khi thêm hiệu ứng động cho thẻ và chuyển trang. Kế hoạch §6.5 có quy tắc chung cho hiệu ứng (chỉ đổi transform và opacity, không hiệu ứng khi cuộn) nhưng chưa nói cách chuyển giữa các màn, thẻ sách mở Chi tiết ra sao, và cách giữ chi phí thấp. App hiện dùng chuyển cảnh mặc định của `NavHost`.
- **Thay đổi:** kế hoạch §6.5 thêm bảng chuyển trang theo kiểu Material motion (đổi mục menu, đi sâu một bậc, thẻ → Chi tiết bằng shared element, cửa sổ vừa và rộng, nhấn thẻ) và 10 quy tắc tối ưu: đọc giá trị hiệu ứng ở pha vẽ, không đổi kích thước, shared element chỉ cho bìa và tên, dùng lại ảnh bìa trong bộ nhớ, màn đích không làm việc nặng ở khung đầu, tắt blur trong lúc chuyển (đề xuất), ngắt được, không cộng vào thời gian tự mượn, `animateItem` khi danh sách đổi, baseline profile. §8.5 thêm các hành trình chuyển cảnh, phép so shared element với mờ dần và không hiệu ứng, số lần recompose mỗi khung hình, và quy tắc chọn theo số đo.
- **Đo trước → sau:** chưa đo; đây là kế hoạch. Phép đo nằm ở §8.5.
- **AI:** Claude Code (Claude Opus 5.5). AI đọc §6.3, §6.5, §7, §8.5 và `ui/AppRoot.kt`, rồi đề xuất các quy tắc trên. Tên API lấy theo trí nhớ, đã ghi trong kế hoạch là cần đối chiếu tài liệu. "Tắt blur trong lúc chuyển" là giả thuyết, chỉ giữ nếu số đo cho thấy có lợi.
- **Kiểm thử:** không áp dụng (chỉ sửa tài liệu); kiểm diff chỉ chạm §6.5 và §8.5.

## 2026-11-24 09:41 · Chuyển khung và nhấn thẻ trong mockup

- **Vấn đề:** người dùng chọn vẽ vào mockup cho phần chuyển trang ở §6.5. Trước đó, màn rộng chỉ cho khung chi tiết hiện dần một lần lúc mở màn, chọn mục khác thì không có hiệu ứng; màn cỡ vừa quay lại danh sách thì danh sách chỉ hiện dần và các hàng trồi lên lại.
- **Thay đổi:**
  - `tools/wide-boards/shell.js`: ở cửa sổ vừa, khung chi tiết trượt vào từ phải 30 dp và hiện dần (300 ms); quay lại thì danh sách trượt vào từ trái và các hàng không trồi lên lại. Ở cửa sổ rộng, chọn mục khác thì nội dung chi tiết hiện dần lại (150 ms): tên keyframe đổi luân phiên để hiệu ứng chạy lại mà không phải dựng lại khung. Nhấn hàng thì hàng co 0.98. Giảm chuyển động thì tắt hết (`!important` vì hiệu ứng đặt trong style).
  - `boards2.js` (Tài khoản), `WideCatalog` sửa tay theo cùng cách; `RailCatalog`: thẻ sách co 0.96 khi nhấn (dùng thuộc tính `scale` riêng để không đè hiệu ứng trồi lên).
  - Sinh lại 21 màn `Wide*` và `Mid*`. README mockup và kế hoạch §6.5 ghi phần mockup đã có; bảng §6.5 thêm "hàng rộng hết khung co 0.98".
- **Đo trước → sau:** 23 màn rộng và vừa vẫn 0 lỗi bố cục ở cả 7 biến thể có màn lớn; tương phản chữ 1212 đoạn mỗi theme, chỉ 5 lỗi cũ; icon và viền bắt buộc 67, 1 lỗi cũ; 455 nút, 0 có vùng chạm dưới 48 px; script của 43 màn chạy không lỗi. Thử trong trình duyệt: đổi tên keyframe trong style thì trình duyệt tạo hiệu ứng mới và chạy lại từ đầu.
- **AI:** Claude Code (Claude Opus 5.5). AI chọn đổi tên keyframe luân phiên thay cho dựng lại khung, vì dựng lại khung tốn hơn và làm mất vị trí cuộn; đây cũng là ý của quy tắc "không đổi bố cục khi chuyển" trong §6.5. Mockup chỉ làm phần khung mới đi vào. AI ghi nhận thêm: số lỗi của hai biến thể điện thoại `flipcover` và `split` đổi chỗ một lỗi tràn đáy cho nhau (84/85 hay 85/84) tùy lúc đo trước hay sau khi font web tải xong; màn điện thoại không đổi, tổng không đổi. Ba lượt đo chờ 2,5 giây cho cùng 84 (74 tràn đáy, 10 chữ đè) và 85 (70 tràn đáy, 15 chữ đè).
- **Kiểm thử:** `node tools/wide-boards/gen.js .`, `tools/check-scripts.js`, `tools/contrast.js`, `tools/nontext-contrast.js`, `tools/target-size.js`; dựng 15 biến thể, chạy `tools/layout-check.js` trong trình duyệt của app; một trang thử riêng cho việc chạy lại hiệu ứng khi đổi tên keyframe.

## 2026-11-24 09:43 · Đăng chuyển khung và nhấn thẻ

- **Vấn đề:** chuyển khung và hiệu ứng nhấn thẻ đã có trong repo nhưng chưa có trên link mockup. Người dùng yêu cầu đăng lên link mockup.
- **Thay đổi:** đăng 11 màn `Wide*`, 11 màn `Mid*` và `RailCatalog` lên artifact (bản 26; `canvas.json` không đổi). `optimization/ui-ux-mockup/README.md` ghi các file là bản 26.
- **Đo trước → sau:** không áp dụng. Sau khi đăng, tải về 26 file (23 màn vừa đăng, `canvas.json` và hai màn điện thoại) và so với `docs/ui-ux-plan`: trùng từng byte; 18 màn điện thoại còn lại không gửi lần này và đã trùng ở bản 25.
- **AI:** Claude Code (Claude Opus 5.5). Lần này số bản tăng đúng một (25 lên 26).
- **Kiểm thử:** so hash của các file tải về với blob trên nhánh.
