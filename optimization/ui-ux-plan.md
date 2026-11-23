# Kế hoạch thiết kế UI và UX cho ShelfTap (app thành viên Android)

- **Ngày:** 2026-11-23 (bản nháp v0, chờ nhóm duyệt)
- **Môn:** Tối ưu ứng dụng di động. Chủ đề 1 (UI/đồ họa) là chủ đề chính của môn.
- **Phạm vi:** `android/app`. Không đụng `:optlab`, `:benchmark`, server hay desktop.
- **Nguồn đối chiếu:** đề cương của PM `study/De_cuong_AI_Coding_UI_UX_Hieu_nang_Bao_mat_ban_mo_rong.docx` (gọi tắt là *Đề cương*), `SPEC.md` §5, `study/02-taxonomy.md` (U01–U06), `study/guardrail/ANDROID-GUARDRAIL.md`.

## 1. Mục tiêu

Đề tài của nhóm là **tối ưu app với sự hỗ trợ của AI và đo trước/sau**, không phải một đồ án thiết kế. Vì vậy UI và UX ở đây chỉ gồm những phần **đo được**, và mỗi phần phải gắn với một chỉ số. Đề cương mục 13 cũng khuyên giữ phạm vi hẹp như vậy.

1. **UI:** giao diện nhất quán, đọc được, dùng được với TalkBack, có đủ các trạng thái hiển thị, và có tiếng Việt.
2. **UX:** 3 tác vụ chính (tìm sách, tự mượn, gia hạn) làm được nhanh hơn, ít lỗi hơn, chứng minh bằng một bài test với người dùng thật.
3. **Không làm chậm app:** mọi thay đổi UI vẫn phải giữ 4 mục tiêu hiệu năng của môn: tìm offline trên 100k sách dưới 50 ms, cuộn danh mục 60 fps, bộ nhớ dưới 150 MB, chạm tự mượn dưới 1 s.
4. **Phục vụ câu hỏi nghiên cứu:** ghi lại AI làm được gì và con người phải sửa gì khi tối ưu UI/UX (Đề cương mục VI.3 và phần mở rộng mục 7, 8).

## 2. Kế hoạch trước đây có nhắc tới UI/UX không?

**Có, nhưng chủ yếu ở *phần mở rộng* của Đề cương.** Phần chính (mục I–VII) gần như chỉ nói về *hiệu năng giao diện* (render, jank, list, ảnh), không nói về thiết kế UI hay đánh giá UX.

| Mục trong Đề cương | Nội dung về UI/UX | Đã làm trong repo? | Kế hoạch này |
|---|---|---|---|
| I, Câu hỏi nghiên cứu | Chỉ nói "hiệu năng giao diện", không có câu hỏi về UX | Có (RQ của nghiên cứu) | Thêm câu hỏi phụ ở §8.4 |
| II, Chương 2.2 và 3 | Lý thuyết hiệu năng UI: rendering, lazy loading, image caching, giảm giật | Có, trong `ARTICLE.md` | Dùng lại làm ràng buộc (§7) |
| III, Demo 1 | Danh sách lớn, phân trang, tránh việc nặng trên UI thread | Có ở `:optlab` (E2), chưa có ở `:app` | Phiên `opt/app-catalog` làm; kế hoạch này không lặp lại |
| Mở rộng, mục 3 (12 vấn đề UI) | Design system drift, responsive, typography, contrast, touch target, UI states, form validation, icon, localization, accessibility, visual regression | Chỉ dùng để dò lỗi trong mã AI viết (U01–U06), **chưa áp dụng cho ShelfTap** | §5 |
| Mở rộng, mục 4 (10 vấn đề UX) | Task flow, navigation, cognitive load, feedback, error recovery, perceived performance, thiếu bằng chứng usability | **Chưa làm** | §6 |
| Mở rộng, mục 3 và 5 (Responsive layout / Adaptive UI; Overdraw & expensive rendering; Jank) | Bố cục không vỡ trên màn hình nhỏ, lớn, tablet; hiệu ứng nặng làm tốn GPU | E2 đo chi phí hiệu ứng trang trí trên `:optlab` (P08); **chưa áp dụng cho ShelfTap** | §6.3, §6.4, §8.5 |
| Mở rộng, mục 7 (ranh giới của AI) | AI không tự quyết phong cách thương hiệu hay quy trình nghiệp vụ | Không áp dụng | §4, §8.4 |
| Mở rộng, mục 8 (quy trình kiểm soát AI) | Đóng băng baseline, giao từng issue nhỏ, review diff, đo lại | Có cho nghiên cứu | §9 |
| Mở rộng, mục 9 (chỉ số) | UI: visual regression, contrast, touch target, accessibility findings. UX: task success, time-on-task, error rate, số bước, SUS hoặc UMUX-Lite | **Chưa đo trên ShelfTap** | §8 |
| Mở rộng, mục 13 (phạm vi khả thi) | UI: responsive, consistency, accessibility, UI states. UX: task flow, feedback, task success/time-on-task | **Chưa làm** | Đúng phạm vi của kế hoạch này |
| README nghiên cứu | "There is no … UX study" | Ghi nhận là giới hạn | Kế hoạch này lấp giới hạn đó |

## 3. Hiện trạng (kiểm tra trong code ngày 2026-11-23)

`android/README.md` ghi rõ app "barebone on purpose: plain UI, real security".

| Hạng mục | Hiện trạng | Mã taxonomy |
|---|---|---|
| Theme | `Theme.Material.Light.NoActionBar`; Compose dùng `lightColorScheme()` và `darkColorScheme()` mặc định (`ui/Common.kt`, `ShelfTapTheme`). Không có màu thương hiệu, typography hay token khoảng cách | U04 |
| Chuỗi | `res/values/strings.xml` chỉ có tên app; mọi chữ là tiếng Anh viết cứng trong Kotlin; không màn hình nào dùng `stringResource` | (Đề cương: Localization) |
| Điều hướng | Thanh dưới 5 tab (Catalog, Loans, Checkout, Reserved, Account) với `icon = {}`, tức là **không có icon** | (Iconography) |
| Accessibility | Ảnh bìa `contentDescription = null`; chưa kiểm tra contrast, vùng chạm hay font scale | U01, U02, U03, U06 |
| Trạng thái UI | Có spinner, lỗi kèm "Retry", danh sách rỗng ("No books on loan.", "No titles found."). Chưa có skeleton, offline theo từng màn hay placeholder cho ảnh bìa | U05 |
| Tìm kiếm | Phải bấm nút "Search", không tìm khi đang gõ; mỗi lần tìm gọi server | (Task flow, Perceived performance) |
| Khách (Guest) | `SPEC.md` §1 cho Guest tìm sách, nhưng `ui/AppRoot.kt` chỉ hiện danh mục khi đã đăng nhập (`SessionState.Active`). Endpoint danh mục của server là public | (Task flow) |
| Tự mượn | Có 4 trạng thái Idle, Working, Done, Failed (`ui/MemberScreens.kt`). Chưa có phản hồi rung hay âm thanh, và chưa có trạng thái "đã đọc thẻ, đang gửi yêu cầu" | (Feedback) |
| Đánh giá UX | Chưa có | (Usability evidence gap) |

## 4. Nguyên tắc và ràng buộc

1. **Không nới bảo mật để làm đẹp hay để tiện test.** Giữ nguyên mọi cơ chế trong `security/README.md`: `FLAG_SECURE`, không lưu mật khẩu vào saved state, NFC chỉ chạy foreground, step-up cho lịch sử đọc, không log dữ liệu cá nhân.
2. **Không giả thành công.** Tự mượn, gia hạn và đặt trước chỉ báo "thành công" sau khi server xác nhận. Đây là quy tắc bảo mật chính của dự án (app không tự quyết sách có được mang đi hay không), và Đề cương mục 4 (Perceived performance) cũng cảnh báo điều này. Optimistic UI chỉ được dùng cho trạng thái chờ, không dùng cho kết quả.
3. **Hợp đồng trước.** Nếu thay đổi UX cần API mới (ví dụ tải danh mục để tìm offline), sửa `server/API.md` trước. Các mục trong kế hoạch này không cần API mới.
4. **Mỗi thay đổi một issue nhỏ, một commit,** để đo được tác động riêng của từng thay đổi (Đề cương mở rộng mục 8).
5. **Quyết định thương hiệu và nghiệp vụ thuộc về nhóm, không thuộc về AI** (Đề cương mở rộng mục 7). AI đề xuất bảng màu và luồng; nhóm duyệt.
6. **Tiếng Việt là ngôn ngữ mặc định,** vì người dùng là thành viên thư viện ở Việt Nam. Tiếng Anh là bản dịch.

## 5. Thiết kế UI

### 5.1 Design system nhỏ (U04)

- File `ui/theme/` gồm `Color.kt`, `Type.kt`, `Spacing.kt`, `Theme.kt`, thay `ShelfTapTheme` hiện tại.
- **Màu:** một bảng màu Material 3 sáng và tối do nhóm chọn. Màu ngữ nghĩa cho "còn sách", "hết sách", "quá hạn" và "lỗi" không chỉ dựa vào màu mà kèm chữ hoặc icon (Đề cương: Color semantics).
- **Chữ:** dùng thang typography của Material 3, kiểm tra hiển thị dấu tiếng Việt.
- **Khoảng cách và hình khối:** token 4, 8, 12, 16, 24 dp; không dùng giá trị `dp` hay `Color(...)` viết cứng trong màn hình.
- **Component dùng chung:** `BookRow`, `CoverImage` (có placeholder và kích thước cố định), `StatusChip`, `EmptyState`, `ErrorState`, `PrimaryButton`.
- **Kiểm tra:** grep không còn `Color(0x` hay `.dp` lẻ ngoài `ui/theme/` (U04 là quy tắc có thể dò tự động).

### 5.2 Ngôn ngữ (Localization)

- Chuyển toàn bộ chữ vào `res/values/strings.xml` (tiếng Việt) và `res/values-en/strings.xml` (tiếng Anh).
- Ngày và tiền theo locale: `formatDate` trong `ui/Common.kt` hiện cần đổi theo locale; tiền phạt là `amountVnd`, hiển thị dạng `1.000 ₫`.
- Kiểm tra chữ tiếng Việt dài hơn không bị cắt hay chồng chữ (Đề cương: text expansion).

### 5.3 Accessibility (U01, U02, U03, U06)

| Việc | Cách kiểm tra |
|---|---|
| Thêm icon cho 5 tab và `contentDescription` lấy từ string resource | Accessibility Scanner, TalkBack |
| Ảnh bìa: mô tả là tên sách, hoặc để trang trí nếu tên sách đã đọc ngay bên cạnh | TalkBack đọc mỗi hàng một lần, không lặp |
| Vùng chạm tối thiểu 48 dp (`minimumInteractiveComponentSize`) | Accessibility Scanner |
| Contrast chữ đạt 4.5:1 (WCAG 2.2 AA) ở cả theme sáng và tối | Accessibility Scanner, UI Check Mode của Android Studio |
| Không vỡ layout ở font scale 2.0 và màn hình rộng 320 dp | Ảnh chụp ở các cấu hình đó |
| Thứ tự focus hợp lý; thông báo kết quả tự mượn bằng `liveRegion` | TalkBack |

### 5.4 Trạng thái UI (U05)

Mỗi màn hình có đủ 5 trạng thái: **đang tải, có dữ liệu, rỗng, lỗi (kèm hướng khắc phục), offline.**

| Màn hình | Cần thêm |
|---|---|
| Danh mục | Skeleton cho hàng sách khi tải trang đầu; placeholder ảnh bìa cố định kích thước, để layout không nhảy; trạng thái offline với nút thử lại |
| Chi tiết sách | Skeleton; nút "Đặt trước" bị vô hiệu hóa kèm lý do khi đã đặt hoặc đang mượn (`ALREADY_RESERVED`) |
| Sách đang mượn | Nổi bật sách quá hạn hoặc sắp đến hạn; lý do không gia hạn được (`RENEWAL_NOT_ALLOWED`) viết rõ bằng lời |
| Tự mượn | Tách "Đang đọc thẻ sách" và "Đang gửi yêu cầu mượn"; kết quả rõ ràng cho từng mã lỗi (`COPY_NOT_AVAILABLE`, `LOAN_LIMIT_REACHED`, `UNPAID_FINE`, `RESERVED_BY_OTHER`) |
| Đặt trước, lịch sử đọc | Trạng thái rỗng có hướng dẫn; lịch sử đang tắt khác với lịch sử trống |
| Đăng nhập | Lỗi tại chỗ, giữ lại email đã nhập; không bao giờ giữ mật khẩu |

Mã lỗi lấy từ `web/ErrorCode.java` của server; app rẽ nhánh theo `code`, không theo `detail` (`server/API.md`).

## 6. Thiết kế UX

### 6.1 Người dùng và tác vụ chính

Người dùng của app là **Guest** và **Member** (`SPEC.md` §1). Ba tác vụ được chọn để đo, vì chúng tạo ra phần lớn giá trị của app:

| # | Tác vụ | Bắt đầu | Kết thúc thành công |
|---|---|---|---|
| T1 | Tìm một cuốn sách cho trước và xem còn bản nào không | Màn hình đầu tiên | Mở đúng trang chi tiết sách |
| T2 | Tự mượn một cuốn sách bằng cách chạm thẻ sách | Đã đăng nhập, cầm sách | Thấy "Đã mượn" do server xác nhận |
| T3 | Gia hạn một cuốn đang mượn | Đã đăng nhập | Thấy hạn trả mới |

Tác vụ phụ (nếu còn thời gian): đặt trước một sách đã hết, tắt lịch sử đọc.

### 6.2 Thay đổi UX đề xuất

| Vấn đề (Đề cương mục 4) | Thay đổi | Liên quan hiệu năng |
|---|---|---|
| Task flow: phải đăng nhập mới xem được sách | Cho Guest xem danh mục và chi tiết sách mà không cần đăng nhập, đúng `SPEC.md` §1; chỉ đòi đăng nhập khi mượn hoặc đặt trước | Không đổi API: endpoint danh mục đã public |
| Task flow, perceived performance: phải bấm nút mới tìm | Tìm khi đang gõ, có debounce, hủy truy vấn cũ (guardrail quy tắc 17) | Ít request hơn, mỗi lần gõ không bắn một request |
| Feedback: tự mượn chỉ có spinner | Rung nhẹ khi đọc được thẻ, rồi trạng thái "đang gửi", rồi kết quả | Giúp đạt cảm nhận "dưới 1 s" |
| Error recovery | Mỗi lỗi có một hành động tiếp theo (thử lại, xem tiền phạt, liên hệ thủ thư) | – |
| Navigation, iconography | 5 tab có icon; tab "Checkout" đổi tên dễ hiểu ("Mượn sách") | – |
| Cognitive load | Hàng sách chỉ hiện tên, tác giả, tình trạng; chi tiết để ở trang riêng | Hàng nhẹ hơn, cuộn mượt hơn |

### 6.3 Hiệu ứng theo tầng thiết bị (adaptive effects)

Nhóm đã chốt (2026-11-23): **một app duy nhất, tự bật hiệu ứng theo khả năng của từng máy**, không tách bản riêng cho Android 17. Đây cũng là một hạng mục tối ưu: app chỉ trả chi phí cho hiệu ứng đẹp ở máy chịu được, và tự bảo vệ máy yếu.

**Ba tầng:**

| Tầng | Điều kiện (gợi ý ban đầu, chốt bằng số đo ở §8.5) | Thanh điều hướng | Icon |
|---|---|---|---|
| **Đầy đủ** | Android 17 (`SDK_INT >= 37`), không phải máy RAM thấp | Kính mờ dần (progressive blur) phủ lên nội dung đang cuộn | Icon morph khi đổi tab |
| **Vừa** | Android 12–16 (API 31–36), không phải máy RAM thấp | Blur nền nhẹ, bán kính cố định | Đổi trạng thái bằng mờ dần và co giãn |
| **Nhẹ** | Android 8–11 (API 26–30), **hoặc** `ActivityManager.isLowRamDevice()`, **hoặc** đang tiết kiệm pin, **hoặc** người dùng tắt hoạt ảnh (animator duration scale = 0) | Nền bán trong suốt, không blur | Đổi màu, không hoạt ảnh |

**Cách làm:**
- Một hàm duy nhất `EffectTier.current(context)` trong `ui/theme/` quyết định tầng lúc chạy. Mọi composable hỏi hàm này, không tự kiểm tra phiên bản Android rải rác.
- Tầng được tính lại khi máy bật hoặc tắt tiết kiệm pin.
- Bản debug có một công tắc ép tầng (Đầy đủ, Vừa, Nhẹ) để đo A/B trên cùng một máy. Bản release không có công tắc này.
- Blur nền: dùng thư viện Haze (`dev.chrisbanes.haze`) hoặc `RenderEffect` trực tiếp (API 31+). Thêm thư viện mới cần nhóm duyệt (§11).
- **Chỉ thanh điều hướng có blur.** Hàng sách, ảnh bìa và trang chi tiết không có blur (§7).
- Nếu Android 17 có API glass hay blur riêng, chỉ dùng nó ở tầng Đầy đủ, sau khi đọc tài liệu chính thức của Android 17 và đo được trên máy hoặc emulator Android 17.

**Icon:**
- Morphicons (`node_modules/morphicons`, giấy phép MIT) là thư viện JavaScript, **không chạy trong app Kotlin/Compose**. App chỉ dùng được dữ liệu icon và ý tưởng morph.
- Bộ icon: lấy SVG từ một bộ có giấy phép mở (ví dụ Lucide, ISC) rồi chuyển sang `ImageVector` (nhóm chọn bộ icon, §11).
- Morph ở tầng Đầy đủ: chạy Morphicons bằng một script Node **lúc build** để chuẩn hóa từng cặp icon (thường và đang chọn) thành path tương thích, rồi xuất ra `AnimatedVectorDrawable`. Script và các file sinh ra để trong `android/app/`, kèm lệnh chạy lại. App không mang JavaScript vào lúc chạy.
- `package.json`, `package-lock.json` và `node_modules/` ở gốc repo hiện chưa được git theo dõi và cũng không bị ignore. Chuyển chúng vào thư mục của script build, và thêm `node_modules/` vào `.gitignore`.

### 6.4 Màn hình tỷ lệ đặc biệt (adaptive layout)

Áp dụng cho mọi tầng, không cần bản riêng.

- **Kích thước cửa sổ:** dùng `WindowSizeClass` và `NavigationSuiteScaffold` (Material 3 Adaptive). Cửa sổ hẹp (điện thoại thường, máy dài 20:9 hoặc 21:9) dùng thanh dưới. Cửa sổ rộng (máy gập khi mở, tablet, chia đôi màn hình) dùng thanh bên (navigation rail). Hiệu ứng tầng ở §6.3 áp dụng cho cả hai.
- **Tràn viền:** app nhắm `targetSdk` 36 nên Android bắt buộc tràn viền. Thanh kính nổi trên nội dung; nội dung chừa đúng vùng an toàn và phần khuyết camera bằng `WindowInsets`.
- **Màn lớn:** từ Android 16, app nhắm `targetSdk` 36 không còn khóa được hướng xoay và tỷ lệ trên màn rộng từ 600dp, nên ShelfTap phải hiển thị đúng khi xoay và khi mở máy gập. Danh mục trên màn rộng có thể chia hai cột (danh sách, chi tiết).
- **Kiểm tra:** ảnh chụp ở các cấu hình: điện thoại thường, 21:9, máy gập lúc gập và lúc mở, tablet, xoay ngang, font scale 2.0.

## 7. Ràng buộc hiệu năng cho mọi thay đổi UI

UI mới không được làm hỏng phần tối ưu của phiên `opt/app-catalog`:

- Hàng sách giữ tham số ổn định (stable) để Compose bỏ qua vẽ lại; component mới không truyền `List` thô.
- Ảnh bìa luôn có kích thước cố định và được Coil tải đúng kích thước hiển thị.
- Không thêm hiệu ứng trang trí nặng (bóng đổ, blur, gradient động) cho hàng danh sách: E2 của nghiên cứu đã đo được chi phí khung hình của loại hiệu ứng này (`study/e2-bench/REPORT.md`, yếu tố P08). Ngoại lệ duy nhất là thanh điều hướng kính ở §6.3, chỉ bật theo tầng và phải qua phép đo §8.5.
- Sau khi làm UI, chạy lại đúng các phép đo của `opt/app-catalog` trên hai emulator Android 16 và 17. Nếu một chỉ số xấu đi thì không nhận thay đổi đó (Đề cương mở rộng mục 8, bước "rollback").

## 8. Đo và đánh giá

### 8.1 Phiên bản A và B

- **A:** `main` tại thời điểm bắt đầu, gắn tag `ui-baseline`, lưu APK.
- **B:** nhánh `ui-ux` sau khi làm xong §5 và §6.
- Cùng thiết bị, cùng phiên bản Android, cùng dữ liệu (Đề cương mục VIII). Danh mục dev hiện chỉ có 13 đầu sách, nên cần một bộ dữ liệu lớn hơn chung với phiên `opt/app-catalog`.

### 8.2 Chỉ số UI

| Chỉ số | Công cụ | A | B |
|---|---|---|---|
| Số phát hiện của Accessibility Scanner trên 6 màn hình chính | Accessibility Scanner | Đo thực tế | Đo thực tế |
| Số cặp màu dưới 4.5:1 | Accessibility Scanner, UI Check Mode | Đo thực tế | Đo thực tế |
| Số vùng chạm dưới 48 dp | Accessibility Scanner | Đo thực tế | Đo thực tế |
| Số màn hình vỡ layout ở font scale 2.0 hoặc rộng 320 dp | Ảnh chụp, người duyệt | Đo thực tế | Đo thực tế |
| Số chuỗi viết cứng trong Kotlin | grep `Text("` trong `ui/` (Lint `HardcodedText` chỉ bắt layout XML, không bắt Compose) | Đo thực tế | Đo thực tế |

### 8.3 Bài test UX (usability test)

- **Người tham gia:** 5 người cho A và 5 người khác cho B, là sinh viên chưa dùng ShelfTap. Mỗi người chỉ dùng một phiên bản, để không học thuộc tác vụ.
- **Tác vụ:** T1, T2, T3 (§6.1), theo một kịch bản đọc to giống nhau cho mọi người.
- **Chỉ số:**
  - task success (thành công, cần trợ giúp, thất bại)
  - time-on-task
  - số lỗi
  - số bước
  - **SUS** (10 câu, điểm 0–100) sau buổi test
- **Ghi hình:** app bật `FLAG_SECURE`, nên quay màn hình bằng `adb screenrecord` sẽ ra màn đen. **Không tắt `FLAG_SECURE`.** Hãy ghi bằng một người quan sát cầm đồng hồ và phiếu ghi, hoặc quay bằng camera ngoài.
- **Báo kết quả:** cỡ mẫu nhỏ, nên chỉ báo số mô tả (trung vị, khoảng, số người thành công trên 5), **không tính p-value**, theo đúng quy tắc của nghiên cứu (chỉ C1–C5 mới có p-value).
- **Dữ liệu cá nhân:** không ghi tên người tham gia; tài khoản test là tài khoản dev, không dùng thẻ hay CCCD thật (Nghị định 13/2023).
- **Biểu mẫu:** `optimization/ux-test.md` (tạo ở giai đoạn 3) chứa kịch bản, phiếu ghi và bảng kết quả để trống.

| Chỉ số (mỗi tác vụ) | A | B |
|---|---|---|
| Task success (n/5) | Đo thực tế | Đo thực tế |
| Time-on-task, trung vị (s) | Đo thực tế | Đo thực tế |
| Số lỗi, trung vị | Đo thực tế | Đo thực tế |
| SUS, trung vị | Đo thực tế | Đo thực tế |

Không điền số dự kiến vào các bảng trên như thể đó là kết quả (Đề cương mục VI.1).

### 8.4 Vai trò của AI (gắn với câu hỏi nghiên cứu)

Mỗi issue UI/UX giao cho Claude Code được ghi thành một mục trong nhật ký của nhánh, `optimization/log/<tên-nhánh>.md` (mẫu và quy tắc ở `optimization/log/README.md`), trong cùng commit với thay đổi:
- prompt đã dùng
- AI đề xuất gì
- con người sửa gì
- issue có được nhận không
- chỉ số trước và sau

Từ log này trả lời được: AI làm tốt phần UI nào (dự kiến: accessibility, trạng thái, chuỗi), và phần nào cần con người quyết (bảng màu, luồng nghiệp vụ). Đây chính là thang "AI tối ưu / kiểm soát" trong Đề cương mở rộng mục 2.

### 8.5 Chi phí của hiệu ứng theo tầng

Câu hỏi cho báo cáo: **hiệu ứng kính tốn bao nhiêu khung hình, và chia tầng có giữ được máy yếu ở mục tiêu 60 fps không?**

- **Cách đo:** cùng một APK release, dùng công tắc ép tầng của bản debug-benchmark, cuộn danh mục 100k sách với thanh điều hướng đè lên. Dùng chung kịch bản và bộ dữ liệu của `opt/app-catalog` (Macrobenchmark, `FrameTimingMetric`), chạy theo thứ tự ABAB, nhiều vòng, báo trung vị và khoảng.
- **Chỉ số:** tỷ lệ frame trễ hạn, `frameDurationCpuMs` p50 và p90, `frameOverrunMs` nếu có, bộ nhớ RSS.
- **Ma trận:**

| Máy | Tầng Nhẹ | Tầng Vừa | Tầng Đầy đủ |
|---|---|---|---|
| Emulator cấu hình thấp, Android 16 | Đo thực tế | Đo thực tế | – (không đủ điều kiện) |
| Emulator cấu hình thấp, Android 17 | Đo thực tế | Đo thực tế | Đo thực tế |
| Điện thoại thật (nếu có) | Đo thực tế | Đo thực tế | Đo thực tế nếu máy chạy Android 17 |

- **Quy tắc nhận:** một tầng chỉ được bật mặc định trên một nhóm máy nếu ở nhóm đó nó vẫn đạt mục tiêu cuộn 60 fps của môn. Nếu không đạt, hạ ngưỡng của tầng xuống, và ghi lý do vào nhật ký.
- **Lưu ý:** emulator vẽ GPU bằng phần mềm, nên chi phí blur trên emulator có thể khác xa máy thật. Số đo trên emulator chỉ dùng để so sánh giữa các tầng, không dùng làm con số tuyệt đối. E2 cũng có cùng giới hạn này.

## 9. Giai đoạn

Thời lượng là ước lượng; nhóm chốt ngày thật.

| Giai đoạn | Việc | Kết quả | Ước lượng |
|---|---|---|---|
| 0. Chốt | Nhóm duyệt kế hoạch này và chốt các mục ở §11 | Kế hoạch v1 | 0,5 ngày |
| 1. Baseline | Tag `ui-baseline`, lưu APK A, đo chỉ số UI (§8.2) | Bảng A của UI | 0,5 ngày |
| 2. Test UX lần A | 5 người làm T1–T3 trên A | Bảng A của UX | 1 ngày |
| 3. Thiết kế | Design system, bảng màu, wireframe cho 6 màn hình chính; viết `ux-test.md` | Nhóm duyệt | 1 ngày |
| 4. Làm UI | §5.1 đến §5.4, mỗi issue một commit, có log AI | Nhánh `ui-ux` | 2–3 ngày |
| 5. Làm UX | §6.2, mỗi thay đổi một commit | Nhánh `ui-ux` | 1–2 ngày |
| 5b. Tầng hiệu ứng và màn hình đặc biệt | §6.3 (tầng, thanh kính, icon, script morph lúc build) và §6.4 (bố cục thích ứng) | Nhánh `ui-ux` | 2–3 ngày |
| 6. Kiểm tra hiệu năng | Chạy lại các phép đo của `opt/app-catalog` trên Android 16 và 17 (§7); đo ma trận tầng (§8.5) và chốt ngưỡng | Không có chỉ số nào xấu đi; bảng §8.5 | 1 ngày |
| 7. Test UX lần B | 5 người khác làm T1–T3 trên B; đo lại chỉ số UI | Bảng B | 1 ngày |
| 8. Báo cáo | Thêm phần UI/UX vào báo cáo và slide: bảng A/B, ảnh trước/sau, log AI | Mục báo cáo | 1 ngày |

## 10. Rủi ro và giới hạn

- **5 người mỗi nhóm là ít.** Chỉ đủ để thấy vấn đề lớn, không đủ để khẳng định thống kê. Báo cáo phải ghi rõ điều này.
- **Người tham gia là sinh viên,** không phải thành viên thư viện thật, nên kết quả không đại diện cho mọi người dùng.
- **Tự mượn cần thẻ sách NFC thật** (ISO 15693). Nếu chưa có thẻ, T2 chỉ test được luồng giao diện, không test được việc đọc thẻ.
- **Xung đột với `opt/app-catalog`:** cả hai cùng sửa `ui/CatalogScreens.kt`. Làm `ui-ux` sau khi `opt/app-catalog` đã merge, hoặc rebase lên nó.
- **Không quay được màn hình** vì `FLAG_SECURE` (§8.3). Accessibility Scanner cũng dựa vào ảnh chụp màn hình để kiểm tra contrast, nên có thể không kiểm tra được màn hình đang bật `FLAG_SECURE`. Khi đó, kiểm tra contrast bằng UI Check Mode trên Compose Preview trong Android Studio (không cần chạy app), và chỉ dùng Scanner cho phần nó vẫn đọc được qua cây accessibility (nhãn, vùng chạm).

- **Chưa có image Android 17 dùng được** trên máy phát triển (thư mục `android-37.1` rỗng, 2026-11-23). Tầng Đầy đủ chỉ làm và đo được sau khi cài image qua SDK Manager của Android Studio.
- **Blur trên emulator** chạy bằng GPU phần mềm, nên số đo không đại diện cho máy thật (§8.5).
- **Thư viện mới** (Haze, Material 3 Adaptive) làm app nặng thêm và cần kiểm tra giấy phép, kích thước APK và tương thích R8.

## 11. Việc nhóm cần chốt

1. Bảng màu và tên hiển thị trong app (ví dụ "ShelfTap" hay tên tiếng Việt).
2. Có cho Guest xem danh mục không (đúng `SPEC.md`, nhưng là thay đổi luồng).
3. Ai tuyển người tham gia, ở đâu, khi nào; điện thoại dùng để test.
4. Có thẻ sách NFC thật cho T2 chưa.
5. Thời điểm bắt đầu so với phiên `opt/app-catalog`.
6. Có thêm thư viện Haze (blur) và Material 3 Adaptive không.
7. Bộ icon (ví dụ Lucide hay Material Symbols) và có làm icon morph ở tầng Đầy đủ không.
8. Đã chốt: một app duy nhất, hiệu ứng theo tầng (§6.3), không làm bản riêng cho Android 17.

## 12. Sản phẩm đầu ra

- Nhánh `ui-ux` với design system, chuỗi tiếng Việt và tiếng Anh, accessibility, trạng thái UI, các thay đổi UX, hiệu ứng theo tầng và bố cục thích ứng.
- Bảng chi phí hiệu ứng theo tầng (§8.5) và ngưỡng đã chốt cho từng tầng.
- `optimization/ux-test.md`: kịch bản, phiếu ghi và kết quả A/B.
- `optimization/log/<tên-nhánh>.md`: nhật ký từng thay đổi, gồm cả phần AI đề xuất và phần con người sửa.
- Bảng chỉ số UI và UX trước/sau, ảnh chụp trước/sau, và một đoạn cho báo cáo và slide.
