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
