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
- **Chữ:** font Nunito (giấy phép SIL OFL, có đủ dấu tiếng Việt), nhóm chọn ngày 2026-11-24 sau khi so bốn font trên cùng một đoạn chữ. Dùng thang typography của Material 3 với các độ đậm 400, 600 và 700; kiểm tra dấu chồng (ỷ, ữ, ặ, ộ) ở cỡ nhỏ và ở font scale 2.0.
- **Sáng và tối:** mỗi màu là một token có hai giá trị (`Color.kt`), không màn nào viết màu cứng. Mặc định theo cài đặt sáng hoặc tối của máy; Tài khoản có lựa chọn "Giao diện: Theo hệ thống / Sáng / Tối", lưu trên máy. Ở nền tối:
  - nút chính đổi thành nền sáng chữ tối (đảo vai trò, không giữ nút đen trên nền đen);
  - kính là lớp tối bán trong với viền sáng mảnh, không phải lớp trắng mờ;
  - màu trạng thái (còn sách, quá hạn, cảnh báo) dùng tông sáng hơn để giữ tương phản 4.5:1;
  - bìa chữ giữ màu riêng của sách; bìa màu sáng luôn dùng chữ tối, ở cả hai theme.
- Mockup (§5.5) dùng đúng bộ token này dưới dạng biến CSS; mỗi màn có nút Sáng/Tối để xem cả hai theme.
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
| Icon cho các mục của menu ☰ (thay cho 5 tab, §6.2); mọi nút chỉ có icon có `contentDescription` lấy từ string resource | Accessibility Scanner, TalkBack |
| Ảnh bìa: mô tả là tên sách, hoặc để trang trí nếu tên sách đã đọc ngay bên cạnh | TalkBack đọc mỗi hàng một lần, không lặp |
| Vùng chạm tối thiểu 48 dp (`minimumInteractiveComponentSize`) | Accessibility Scanner |
| Contrast chữ đạt 4.5:1 (WCAG 2.2 AA) ở cả theme sáng và tối | Accessibility Scanner, UI Check Mode của Android Studio |
| Không vỡ layout ở font scale 2.0 và màn hình rộng 320 dp | Ảnh chụp ở các cấu hình đó |
| Thứ tự focus hợp lý; thông báo kết quả tự mượn bằng `liveRegion` | TalkBack |

**Đo tương phản trên mockup (2026-11-24).** Mã nguồn mockup và các script đo nằm ở `optimization/ui-ux-mockup/` (xem README của thư mục đó để chạy lại). Một script Node dựng từng màn của mockup bằng dữ liệu thật của màn đó, thay token màu theo theme, chồng các lớp bán trong lên nền bên dưới, rồi tính tỷ lệ tương phản WCAG cho mọi đoạn chữ. Nó cũng mô phỏng màu mặc định của trình duyệt cho nút, ô nhập, chữ gợi ý (placeholder) và link. Ngưỡng 4.5:1, hoặc 3:1 cho chữ lớn (từ 24 px, hoặc từ 18.66 px và đậm).

| | Sáng | Tối |
|---|---|---|
| Số đoạn chữ đã kiểm | 353 | 353 |
| Không đạt, trước khi sửa | 9 | 9 |
| Không đạt, sau khi sửa | 0 | 0 |

- **Lỗi thật đã sửa:** link và nút không ghi màu bị lấy màu đen mặc định, nên ở nền tối gần như không đọc được (1.1:1). Đã sửa bằng một quy tắc chung: chữ trong nút, ô nhập và link lấy token `ink`.
- **Chữ gợi ý trong ô nhập:** màu xám mặc định của trình duyệt (#757575) đạt 3.4:1 ở nền tối và 4.4:1 ở nền sáng. Đã đổi sang token `ink3`.
- **Chỗ sát ngưỡng đã nâng lên:** nhãn trên bìa sách ở Danh mục khi ở nền tối (4.37:1), dòng ghi chú ở màn Khởi động (4.46:1), nút "Lưu mật khẩu" đang bị khóa (4.31:1; nút bị khóa được miễn theo WCAG, nhưng vẫn sửa).
- **Chữ trên ảnh ở màn chào:** script không tính được nền ảnh, nên tính riêng trường hợp xấu nhất, với vùng sáng nhất của ảnh (#d9d1c6) nằm sau chữ. Trước khi sửa, tiêu đề chỉ đạt 2.49:1 (cần 3) và nút "Bắt đầu" bằng kính đạt 3.70:1 (cần 4.5). Sau khi tăng độ đậm của dải tối (0.5 ở 25% chiều cao, 0.8 ở đáy) và giảm độ trắng của nút kính từ 0.22 xuống 0.16, các giá trị là 4.80, 7.03 và 5.57.
- **Icon, viền và trạng thái (SC 1.4.11, ngưỡng 3:1).** Các mục bắt buộc gồm: icon trong nút không có chữ, viền ô nhập, rãnh và núm công tắc, chip hoặc tab đang chọn. Có 37 mục bắt buộc mỗi theme. Trước khi sửa: sáng 13 không đạt, tối 12; sau khi sửa: 0 ở cả hai theme. Riêng icon thẻ trên ảnh ở màn chào được tính riêng như chữ trên ảnh và đạt 5.57.
  - Công tắc tắt chỉ đạt 1.59 đến 1.94, và rãnh bật ở nền tối đạt 1.97. Đã đổi sang kiểu Material 3: rãnh tắt có viền và núm màu `ink3`; rãnh bật dùng token `switch-on` riêng cho từng theme.
  - Viền ô nhập chỉ đạt 1.33 đến 1.5, còn khung ô tìm kiếm gần như không thấy (1.0 đến 1.2). Đã đổi sang viền 50% của màu chữ.
  - 62 icon đứng cạnh chữ đều đạt. Mép các nút kính phần lớn dưới 3:1; đây là chủ ý của phong cách kính và SC 1.4.11 không bắt buộc, vì chữ hoặc icon trong nút đã nhận diện được nút. Nếu bài test UX (§8.3) cho thấy người dùng khó nhận ra nút thì tăng viền.
**Vùng chạm trên mockup (2026-11-24).** `optimization/ui-ux-mockup/tools/target-size.js` đọc kích thước của 135 nút, link và công tắc trên 20 màn. 32 cái vẽ nhỏ hơn 48 px: công tắc (52×32), chip và tab (40), nút "Không quan tâm" (36), vài nút 44, và 4 link chữ không có padding. Mockup giữ nguyên kích thước vẽ nhưng mở rộng vùng chạm lên ít nhất 48 × 48 px bằng một lớp trong suốt (`::after`), cùng ý với `minimumInteractiveComponentSize` của Compose; hàng chip cuộn ngang được thêm padding để không cắt mất vùng chạm. Kết quả: 0 vùng chạm dưới 48 px. Khi làm app, đo lại bằng Accessibility Scanner.

- **Giới hạn:** mockup là trang web. Kính thật có blur trên nền đang cuộn nên màu nền thay đổi theo nội dung; khi làm app phải đo lại bằng Accessibility Scanner trên máy.

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

### 5.5 Hướng thiết kế: giao diện kính cho toàn app

Nhóm chọn (2026-11-23) phong cách "kính mờ dần" (progressive blur) cho **cả app**, không chỉ một hiệu ứng. Bản phác 22 màn (20 màn điện thoại và 2 màn cửa sổ rộng) có thể bấm thử, có hiệu ứng động (§6.5): **ShelfTap glass mockup** (https://claude.ai/artifact/GMkMmXLY2J2DP2MKf1yiGf). Mockup là trang web dùng `backdrop-filter` của trình duyệt; nó cho thấy giao diện, **không** đo được hiệu năng trên Android.

**Các thành phần:**
- **Ảnh lớn mờ dần** ở màn chào, banner chủ đề và chi tiết sách: nửa dưới ảnh mờ dần để chữ đặt lên vẫn đọc được.
- **Nút kính tròn và nút kính dạng viên thuốc** nổi trên nội dung (quay lại, chia sẻ, menu); **một hành động chính nổi ở đáy** mỗi màn (đặt trước, gia hạn, chạm sách để mượn).
- **Thẻ bo góc lớn** (22–32 dp), băng cuộn ngang có nhãn kính, chữ đậm cỡ lớn, tông trung tính ấm.
- **Bìa chữ**: sách không có ảnh bìa (`cover_url` rỗng, như toàn bộ dữ liệu dev) hiện nền màu và tên sách thay vì ô trống.
- **Font:** Nunito (§5.1; thay cho Be Vietnam Pro của bản phác đầu, vì Be Vietnam Pro ở độ đậm 700 trông thô ở cỡ lớn), đóng gói trong app; không tải font qua mạng lúc chạy.

**Các màn (khớp với mockup):**

| Nhóm | Màn |
|---|---|
| Khám phá | Màn chào (mới), Danh mục (banner chủ đề, băng "Gợi ý cho bạn", băng theo chủ đề, ô tìm kiếm; cuộn dọc), Danh sách sách (mới, mở từ "Xem tất cả"), Chi tiết sách |
| Đăng nhập và bảo mật | Đăng nhập bằng mật khẩu, Đăng nhập bằng thẻ (3 bước), Đổi mật khẩu lần đầu (điều kiện hiện ngay khi gõ, tối thiểu 10 ký tự như server), Khóa sinh trắc học |
| Trạng thái hệ thống | Khởi động (kiểm tra phiên rồi chuyển tới Khóa, Màn chào hoặc Mất kết nối), Mất kết nối (thử lại có giới hạn, tìm sách offline, đăng xuất), Đăng nhập lại kèm lý do (4 giá trị của `LogoutReason`) và cảnh báo máy không an toàn (`IntegrityBanner`: chỉ cảnh báo, không chặn) |
| Khách và mở rộng | Chế độ khách (menu rút gọn, mời đăng nhập khi bấm Đặt trước hay Mượn), Thông báo (giải thích trước khi xin quyền, màn khóa không hiện tên sách, cài đặt từng loại), Tìm khi không có mạng, Gợi ý cho bạn (theo `optimization/search-recommend-plan.md` phần B) |
| Cửa sổ rộng (§6.4) | Danh mục ở cửa sổ từ 840 dp (menu cố định bên trái, danh sách và chi tiết cạnh nhau), Danh mục ở cửa sổ 600–840 dp (thanh điều hướng dọc, một khung nội dung) |
| Thành viên | Sách đang mượn (gia hạn, quá hạn, tiền phạt), Tự mượn sách (chờ chạm, đang gửi, thành công, lỗi), Đặt trước (hủy có xác nhận), Lịch sử đọc (yêu cầu mật khẩu nếu đăng nhập bằng thẻ), Tài khoản (công tắc lịch sử, vân tay, đổi mật khẩu, đăng xuất qua bảng xác nhận) |

**Trang danh sách sách** (màn `List` trong mockup):
- Mở từ "Xem tất cả" ở Danh mục. Kết quả tìm kiếm dùng chung màn này, để chỉ có một danh sách dài cần tối ưu.
- Thanh trên bằng kính (nút quay lại, tiêu đề, ô tìm, hàng chip chủ đề) mờ dần phủ lên danh sách đang cuộn. **Hàng sách nền đặc, không blur** (§7). Bìa chữ 52×74 dp cho sách thiếu ảnh.
- Mỗi hàng: tên sách, tác giả, trạng thái ("Còn N bản" từ `copiesAvailable` của `TitleSummary`; bằng 0 thì "Đang được mượn · đặt trước"), cả hàng là một vùng bấm cao ít nhất 48 dp, TalkBack đọc một câu gộp.
- Trạng thái (§5.4): khung xương (skeleton) khi tải lần đầu, "Không tìm thấy sách" kèm gợi ý, lỗi mạng có nút thử lại, "Đang tải thêm…" ở cuối khi còn trang (`Page.total`).
- Đây chính là màn cuộn 100k sách mà `opt/app-catalog` đo, nên mọi thứ thêm vào hàng phải qua §7 và §8.5.
- **Giới hạn so với API hiện tại** (`server/API.md`):
  - `TitleSummary` và `TitleDetail` không có kệ: kệ nằm ở từng bản sách (`copy.shelf`). Mockup hiện "Kệ A0x" ở danh sách và chi tiết là **đề xuất**; muốn có thì sửa `API.md` trước (§4), nếu không thì bỏ khỏi hàng.
  - `q` đã khớp cả chủ đề, nhưng chưa có tham số lọc riêng theo chủ đề. Chip chủ đề cần một tham số mới (ví dụ `subject=`), làm cùng đợt sửa API với `optimization/search-recommend-plan.md`.
  - Mockup tìm không dấu ("chi pheo" ra "Chí Phèo") bằng cách bỏ dấu ngay trên trang với 13 sách dev. Server hiện là `ILIKE` có phân biệt dấu; cách làm thật nằm ở phần A của `optimization/search-recommend-plan.md`.

**Các màn trạng thái và mở rộng** (hàng cuối của mockup). Các màn có nhiều trạng thái có một thanh "xem trước trạng thái" ở đáy; thanh này chỉ có trong bản phác.
- **Khởi động:** dùng SplashScreen API của Android 12 trở lên, không giữ màn lâu hơn lúc kiểm tra phiên (`SessionState.Loading`).
- **Mất kết nối:** thay `OfflineScreen` hiện tại. Không tự thử lại liên tục; thử lại khi người dùng bấm hoặc khi Android báo có mạng.
- **Lý do đăng xuất:** một thông báo cho mỗi giá trị `SESSION_EXPIRED`, `TOKEN_REUSED`, `INACTIVITY`, `LOCK_KEY_INVALIDATED`. `TOKEN_REUSED` khuyên đổi mật khẩu.
- **Cảnh báo máy không an toàn:** giữ đúng hành vi hiện tại: chỉ cảnh báo, server cũng nhận cờ qua `X-Device-Integrity`.
- **Chế độ khách:** phụ thuộc mục 2 ở §11. Không có nút tự mượn; nút nổi đổi thành "Đăng nhập để mượn sách"; đăng nhập xong quay lại đúng cuốn sách.
- **Thông báo:** giải thích trước, rồi mới gọi hộp thoại xin quyền của hệ thống (`POST_NOTIFICATIONS`, Android 13 trở lên). Trên màn khóa dùng bản công khai không có tên sách (`VISIBILITY_PRIVATE` kèm `setPublicVersion`), vì sách đã mượn là dữ liệu nhạy cảm (`SPEC.md` §7, Nghị định 13/2023). Nhắc hạn trả tính trên máy từ `dueAt` của `Loan` (WorkManager), không cần API mới. Báo sách đặt trước đã sẵn sàng thì **`server/API.md` chưa có** cơ chế đẩy; cần sửa API trước (§11).
- **Tìm khi không có mạng:** mở ngay, không có khung xương. Không hiện số bản còn vì bản lưu không chứa `copiesAvailable` (phần A2 của `optimization/search-recommend-plan.md`).
- **Gợi ý cho bạn:** ba trạng thái theo phần B của kế hoạch tìm kiếm và gợi ý: đăng nhập bằng mật khẩu (theo lịch sử và sách đang mượn), đăng nhập bằng thẻ (chỉ theo sách đang mượn, kèm nút nhập mật khẩu), lịch sử tắt và không mượn gì (không có gợi ý). Mỗi gợi ý có lý do và nút "Không quan tâm". Lối vào chính là băng "Gợi ý cho bạn" trên Danh mục, ngay dưới banner chủ đề (theo phần B4 của kế hoạch tìm kiếm và gợi ý); "Xem tất cả" mở màn này, menu ☰ là lối vào thứ hai. Mỗi thẻ trong băng là bìa chữ (trang trí, cỡ cố định như ảnh bìa) và lý do ở dạng chữ bên dưới; trình đọc màn hình đọc tên sách rồi lý do. Băng không hiện với khách và khi chưa có dữ liệu để gợi ý. Vì có thêm băng, nội dung Danh mục cuộn dọc và chừa chỗ ở đáy cho nút nổi. Cài đặt thông báo mở từ Tài khoản.

**Theo tầng (§6.3):** blur thật chỉ ở tầng Đầy đủ. Tầng Vừa và Nhẹ thay vùng mờ dần bằng **dải gradient phủ** (scrim) và nền bán trong suốt: trông gần giống mà rẻ hơn nhiều.

**Giới hạn của hướng này:**
- Băng cuộn dùng **chủ đề có sẵn** (ví dụ "Văn học Việt Nam"), vì API chưa có "sách mới" hay "mượn nhiều". Muốn có các băng đó thì sửa `server/API.md` trước (§4).
- Ảnh lớn đè lên mục tiêu bộ nhớ dưới 150 MB: tải đúng kích thước hiển thị, giới hạn cache (§7).
- Chữ trên ảnh phải đạt tương phản 4.5:1 (U03): lớp mờ hoặc gradient phải đủ đậm ở chỗ có chữ.
- Lấy cảm hứng phong cách từ ảnh mẫu của một tác giả khác, **không chép** bố cục hay hình ảnh của họ.

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
| Navigation, iconography | Bỏ thanh 5 tab. Nút kính ☰ ở góc trên trái mở một **khung kính bo góc xếp dọc** (Danh mục, Gợi ý cho bạn, Sách đang mượn, Đặt trước, Lịch sử đọc, Tài khoản; khách chỉ thấy Danh mục, Tất cả sách và Đăng nhập); icon ☰ morph thành ✕ khi mở; đóng bằng bấm ra ngoài, cử chỉ quay lại hoặc chọn mục. **Tự mượn sách** không nằm trong menu mà là nút nổi "Chạm sách để mượn" luôn hiện ở đáy các màn chính | Blur của menu chỉ chạy khi menu mở, lúc nội dung phía sau đứng yên |
| Navigation: rủi ro của menu ☰ | Menu ☰ giấu điều hướng, nên có thể làm chậm người dùng. Đo trực tiếp trong bài test UX: A (app hiện tại, thanh tab) với B (menu ☰ cộng nút nổi), trên T2 và T3 (§8.3) | – |
| Cognitive load | Hàng sách chỉ hiện tên, tác giả, tình trạng; chi tiết để ở trang riêng | Hàng nhẹ hơn, cuộn mượt hơn |

### 6.3 Hiệu ứng theo tầng thiết bị (adaptive effects)

Nhóm đã chốt (2026-11-23): **một app duy nhất, tự bật hiệu ứng theo khả năng của từng máy**, không tách bản riêng cho Android 17. Đây cũng là một hạng mục tối ưu: app chỉ trả chi phí cho hiệu ứng đẹp ở máy chịu được, và tự bảo vệ máy yếu.

**Ba tầng:**

| Tầng | Điều kiện (gợi ý ban đầu, chốt bằng số đo ở §8.5) | Lớp kính (menu ☰, nút nổi, ảnh mờ dần) | Icon |
|---|---|---|---|
| **Đầy đủ** | Android 17 (`SDK_INT >= 37`), không phải máy RAM thấp | Kính mờ dần (progressive blur) phủ lên nội dung đang cuộn | Icon morph khi đổi tab |
| **Vừa** | Android 12–16 (API 31–36), không phải máy RAM thấp | Blur nền nhẹ, bán kính cố định | Đổi trạng thái bằng mờ dần và co giãn |
| **Nhẹ** | Android 8–11 (API 26–30), **hoặc** `ActivityManager.isLowRamDevice()`, **hoặc** đang tiết kiệm pin, **hoặc** người dùng tắt hoạt ảnh (animator duration scale = 0) | Nền bán trong suốt, không blur | Đổi màu, không hoạt ảnh |

**Cách làm:**
- Một hàm duy nhất `EffectTier.current(context)` trong `ui/theme/` quyết định tầng lúc chạy. Mọi composable hỏi hàm này, không tự kiểm tra phiên bản Android rải rác.
- Tầng được tính lại khi máy bật hoặc tắt tiết kiệm pin.
- Bản debug có một công tắc ép tầng (Đầy đủ, Vừa, Nhẹ) để đo A/B trên cùng một máy. Bản release không có công tắc này.
- Blur nền: dùng thư viện Haze (`dev.chrisbanes.haze`) hoặc `RenderEffect` trực tiếp (API 31+). Thêm thư viện mới cần nhóm duyệt (§11).
- **Blur chỉ ở các lớp nổi và vùng mờ dần của ảnh lớn** (menu ☰, nút kính, thanh hành động nổi, nửa dưới ảnh ở màn chào, banner và chi tiết sách). Hàng trong danh sách sách không có blur (§7). (Bản trước của mục này chỉ có thanh điều hướng; §5.5 mở rộng ra toàn app.)
- Nếu Android 17 có API glass hay blur riêng, chỉ dùng nó ở tầng Đầy đủ, sau khi đọc tài liệu chính thức của Android 17 và đo được trên máy hoặc emulator Android 17.

**Icon:**
- Morphicons (`node_modules/morphicons`, giấy phép MIT) là thư viện JavaScript, **không chạy trong app Kotlin/Compose**. App chỉ dùng được dữ liệu icon và ý tưởng morph.
- Bộ icon: lấy SVG từ một bộ có giấy phép mở (ví dụ Lucide, ISC) rồi chuyển sang `ImageVector` (nhóm chọn bộ icon, §11).
- Morph ở tầng Đầy đủ: chạy Morphicons bằng một script Node **lúc build** để chuẩn hóa từng cặp icon (thường và đang chọn) thành path tương thích, rồi xuất ra `AnimatedVectorDrawable`. Script và các file sinh ra để trong `android/app/`, kèm lệnh chạy lại. App không mang JavaScript vào lúc chạy.
- `node_modules/` đã có trong `.gitignore` (2026-11-24), ở mọi cấp thư mục nên vẫn đúng khi chuyển sang thư mục của script build. `package.json` và `package-lock.json` ở gốc repo chưa được git theo dõi: khi làm script build thì chuyển chúng vào thư mục của script và commit ở đó, để ai cũng cài lại được đúng phiên bản.
- **Chỗ dùng icon morph** (chỉ ở tầng Đầy đủ; tắt khi người dùng tắt hoạt ảnh; không dùng trong hàng danh sách):

| Chỗ | Morph | Khi nào |
|---|---|---|
| Nút menu | ☰ → ✕ | Mở và đóng menu |
| Ô tìm kiếm | kính lúp → ✕ | Khi có chữ trong ô (bấm để xóa) |
| Ô mật khẩu | mắt → mắt gạch chéo | Hiện hoặc ẩn mật khẩu |
| Đặt trước | bookmark → bookmark có dấu tích | **Sau khi server xác nhận** |
| Tự mượn | biểu tượng chạm → dấu tích, hoặc → dấu chấm than | Kết quả, **sau khi server trả lời** |
| Gia hạn | đồng hồ → lịch có dấu tích | **Sau khi server xác nhận** |
| Khóa sinh trắc học | ổ khóa → ổ khóa mở | Mở khóa thành công |

**Đồng bộ với hệ thống của máy:** không có API công khai để đọc bán kính blur, độ trong hay màu phủ mà Pixel, Samsung, Xiaomi… dùng, nên app không sao chép được hiệu ứng kính của từng hãng. Thay vào đó app theo các tín hiệu chính thức:

| Tín hiệu | API | App làm gì |
|---|---|---|
| Hệ thống đang cho phép blur | `WindowManager.isCrossWindowBlurEnabled()` và listener (API 31+) | Hệ thống tắt blur (tiết kiệm pin, máy yếu, cài đặt người dùng) thì app hạ xuống tầng Nhẹ |
| Màu theo hình nền (Material You) | `dynamicLightColorScheme` / `dynamicDarkColorScheme` (API 31+) | Lớp kính và nền lấy màu hệ thống; màu thương hiệu giữ cho nút chính và logo (nhóm chốt, §11) |
| Sáng hoặc tối | Theme hệ thống, trừ khi người dùng chọn cố định trong Tài khoản | Đổi toàn bộ token màu (§5.1), kể cả độ đậm và màu của lớp kính |
| Độ tương phản | `UiModeManager.getContrast()` (API 34+) | Tương phản cao thì giảm độ trong, tăng độ đặc của lớp kính |
| Hoạt ảnh | Hệ số thời lượng hoạt ảnh của hệ thống | Bằng 0 thì không morph, không chuyển cảnh |

Bán kính blur và độ trong chọn theo hướng dẫn Material, rồi chụp đặt cạnh bảng cài đặt nhanh của vài máy (Pixel, Samsung, Xiaomi) để chỉnh cho gần nhau.

### 6.4 Màn hình tỷ lệ đặc biệt (adaptive layout)

Áp dụng cho mọi tầng, không cần bản riêng.

- **Kích thước cửa sổ:** dùng `WindowSizeClass` (Material 3 Adaptive). Cửa sổ hẹp (điện thoại thường, máy dài 20:9 hoặc 21:9) dùng menu ☰ dạng khung kính (§6.2). Cửa sổ vừa (600–840 dp, ví dụ máy gập mở gần vuông) dùng thanh điều hướng dọc bên trái với một khung nội dung. Cửa sổ rộng (từ 840 dp: máy gập cầm ngang, tablet) hiện khung menu cố định ở cạnh trái và Danh mục chia hai khung. Cả hai không cần bấm ☰. Hiệu ứng tầng ở §6.3 áp dụng cho cả hai.
- **Tràn viền:** app nhắm `targetSdk` 36 nên Android bắt buộc tràn viền. Thanh kính nổi trên nội dung; nội dung chừa đúng vùng an toàn và phần khuyết camera bằng `WindowInsets`.
- **Màn lớn:** từ Android 16, app nhắm `targetSdk` 36 không còn khóa được hướng xoay và tỷ lệ trên màn rộng từ 600dp, nên ShelfTap phải hiển thị đúng khi xoay và khi mở máy gập. Danh mục trên màn rộng có thể chia hai cột (danh sách, chi tiết).
- **Kiểm tra:** ảnh chụp ở các cấu hình: điện thoại thường, 21:9, máy gập lúc gập và lúc mở, màn phụ của máy gập dọc, chia đôi màn hình, tablet, xoay ngang, font scale 2.0.

**Thử trên mockup: rộng 320 dp và cỡ chữ 2.0 (2026-11-24).** `optimization/ui-ux-mockup/tools/render-static.js` dựng 20 màn thành HTML tĩnh ở bốn biến thể. `tools/layout-check.js` chạy trong trình duyệt và đếm bốn loại lỗi: chữ hoặc nút tràn ra mép trái hay phải, nằm dưới đáy màn mà không cuộn được, chữ đè lên chữ, chữ bị cắt mà không có dấu "…". Cỡ chữ 2.0 dùng kiểu phóng phi tuyến của Android 14 trở lên (chữ nhỏ gấp đôi, chữ lớn tăng ít hơn), lấy theo bảng của AOSP như tôi nhớ; cần đối chiếu mã AOSP trước khi đưa con số vào báo cáo.

| Biến thể | Màn có lỗi | Số lỗi |
|---|---|---|
| Gốc: rộng 390, chữ bình thường | 0 / 20 | 0 |
| Rộng 320 | 0 / 20 | 0 |
| Chữ 2.0 | 10 / 20 | 30 (18 chữ đè, 11 tràn đáy, 1 tràn ngang) |
| Rộng 320 và chữ 2.0 | 16 / 20 | 55 (31 chữ đè, 19 tràn đáy, 5 tràn ngang) |

Số trên là sau hai thay đổi: Danh mục cuộn được (cùng lúc thêm băng gợi ý), và script không còn tính chữ trong vùng cuộn đi qua dưới thanh nổi là "chữ đè", vì người dùng cuộn được nội dung lên trên thanh. Lượt đo đầu, trước hai thay đổi này, là 13 màn với 41 lỗi (chữ 2.0) và 18 màn với 65 lỗi (rộng 320 và chữ 2.0).

Ba lỗi thật có cả ở cỡ chữ thường đã sửa trong mockup: ô nhập giữ bề rộng tối thiểu mặc định (khoảng 20 ký tự) nên tràn ra ngoài ở chữ lớn (`min-width: 0`); băng sách ở Danh mục tràn mép phải mà không cuộn ngang được; dải "Không có mạng" ở màn Tìm khi không có mạng đè lên dòng đếm.

Lỗi ở chữ 2.0 đến từ cách mockup đặt mọi khối ở vị trí cố định trong một màn cao 844 px, không cuộn. Đây là quy tắc cho app, không sửa trên mockup:
- **Mọi màn cuộn dọc được** (`verticalScroll` hoặc `LazyColumn`), không đặt khối ở toạ độ `y` cố định. Tài khoản, Chi tiết, Sách đang mượn, Đổi mật khẩu và Mất kết nối là các màn tràn đáy nhiều nhất.
- **Thanh nổi chừa chỗ:** nội dung có `contentPadding` ở đáy bằng chiều cao thực của thanh nổi ("Chạm sách để mượn", thanh Đặt trước), đo lúc chạy chứ không viết cứng, vì thanh cao lên khi chữ lớn.
- **Chữ trong nút dạng viên thuốc được xuống dòng** (tối đa 2 dòng), không ép một dòng; "Chạm sách để mượn" và "Đăng nhập để mượn sách" tràn mép ở rộng 320 và chữ 2.0.
- **Ghi chú ở đáy màn** (Đăng nhập, Khóa, Mất kết nối, Đăng nhập lại) nằm trong luồng cuộn, không ghim ở đáy.
- Đo lại bằng ảnh chụp ở font scale 2.0 trên máy, theo bảng ở §8.2.

**Thử trên mockup: các màn tỷ lệ đặc biệt (2026-11-24).** Cùng hai script, thêm bảy cỡ màn (đơn vị dp). Đây là nhóm kích thước, không phải số đo của từng mẫu máy: thông số các máy gập ra năm 2026 ở các nguồn không khớp nhau (ví dụ dòng Galaxy Z Fold8 có một màn 4:3 và một màn 10:16, nhưng [trang của Samsung ở SoftBank](https://www.softbank.jp/en/mobile/products/samsung-galaxy/samsung-galaxy-z-fold8/) và [tin của Samsung Canada](https://news.samsung.com/ca/galaxy-z-fold8-is-designed-for-your-favorite-content-heres-why) ghi ngược nhau màn nào là màn ngoài). Cần đối chiếu bằng ảnh chụp trên máy thật. Cho màn rộng, `tools/layout-check.js` đếm thêm hai loại: dòng chữ trung bình quá 80 ký tự, và thẻ, hàng hay nút rộng quá 85% màn khi màn rộng hơn 600 dp (bố cục điện thoại bị kéo giãn).

| Cỡ màn | dp | Màn vỡ bố cục | Lỗi vỡ | Màn bị kéo giãn |
|---|---|---|---|---|
| Điện thoại dài 21:9 | 412 × 960 | 0 / 20 | 0 | không áp dụng |
| Màn ngoài rộng 10:16 của máy gập ngang | 480 × 768 | 1 / 20 | 1 (nút Đăng xuất ở Tài khoản nằm dưới đáy) | không áp dụng |
| Màn phụ của máy gập dọc | 360 × 400 | 15 / 20 | 84 (74 tràn đáy, 10 chữ đè) | không áp dụng |
| Chia đôi màn hình | 390 × 420 | 15 / 20 | 85 (70 tràn đáy, 15 chữ đè) | không áp dụng |
| Điện thoại xoay ngang | 844 × 390 | 14 / 20 | 78 (74 tràn đáy, 4 chữ đè) | 18 / 20 |
| Máy gập mở, gần vuông | 720 × 840 | 0 / 20 | 0 | 17 / 20 |
| Máy gập mở, cầm ngang (4:3) | 960 × 720 | 4 / 20 | 7 | 18 / 20 |

"Lỗi vỡ" là bốn loại ở bảng trên (tràn mép, tràn đáy, chữ đè, chữ bị cắt). Bốn biến thể cũ đo lại cho đúng số cũ.

Đọc kết quả:
- **Màn cao hơn 844 dp, hoặc rộng hơn một chút** (21:9, màn ngoài 10:16) gần như không sao.
- **Màn thấp** (màn phụ máy gập dọc, chia đôi màn hình, xoay ngang) vỡ ở 14 đến 15 màn. Chỉ các màn có vùng cuộn giữ nguyên: Danh mục, Danh sách, Thông báo, Tìm khi không có mạng, Gợi ý (khi xoay ngang thêm Màn chính). Cùng nguyên nhân với lỗi ở chữ 2.0, nên quy tắc "mọi màn cuộn dọc được" ở trên sửa cả hai.
- **Màn rộng hơn 600 dp** không vỡ nhưng bị kéo giãn: 17 đến 18 trên 20 màn có thẻ hoặc nút rộng gần hết màn, dòng chữ dài tới khoảng 150 ký tự (Gợi ý, ở 960 dp). Mockup chưa có màn nào cho cửa sổ rộng (menu cố định cạnh trái, Danh mục hai cột).

Quy tắc thêm cho app:
- **Cửa sổ rộng không kéo giãn bố cục điện thoại:** từ 600 dp dùng thanh điều hướng dọc, từ 840 dp dùng menu cố định cạnh trái và hai khung (danh sách, chi tiết), như ở trên. Cột chữ giới hạn khoảng 600 dp hoặc 80 ký tự mỗi dòng.
- **Màn thấp:** ngoài quy tắc cuộn dọc, khi chiều cao dưới khoảng 480 dp thì nút nổi tự mượn thu thành nút tròn chỉ có icon, để không che phần lớn màn. Đây là đề xuất, cần thử trên máy.

**Mockup cho cửa sổ rộng (2026-11-24).** Hai màn mới ở hàng cuối của mockup, theo cách chia cỡ cửa sổ của Material 3:
- **Danh mục ở cửa sổ từ 840 dp** (`WideCatalog`, 960 × 720): menu cố định bên trái, nút "Chạm sách để mượn" ở đáy menu thay cho nút nổi. Khung giữa là danh sách (ô tìm, chip chủ đề, tìm không dấu như màn Danh sách). Khung phải là chi tiết của sách đang chọn: bấm sách khác thì khung phải đổi theo, không chuyển trang; nút "Đặt trước" đi qua các trạng thái như màn Chi tiết. Hàng đang chọn có nền đặc và viền.
- **Danh mục ở cửa sổ 600–840 dp** (`RailCatalog`, 720 × 840): thanh điều hướng dọc 96 dp, trên cùng là nút "Chạm để mượn". Một khung nội dung rộng tối đa 560 dp: 4 thẻ gợi ý, lưới sách 2 cột. Bấm sách thì mở trang Chi tiết như trên điện thoại.
- **Đo:** cả hai màn không vỡ bố cục ở chữ thường và chữ 2.0, không có dòng quá 80 ký tự, không có khối bị kéo giãn. Không thêm lỗi tương phản chữ; 5 mục bắt buộc mới về icon và viền đều đạt; mọi nút có vùng chạm ít nhất 48 px. Lượt đo đầu tìm ra một lỗi thật: ở chữ 2.0 các mục menu cố định đè nhau vì khung menu không cuộn; đã sửa bằng vùng cuộn cho danh sách mục, nút mượn giữ ở đáy.
- **Chưa vẽ:** các màn khác ở cửa sổ rộng (Sách đang mượn, Tài khoản…). Theo cùng quy tắc: thanh dọc hoặc menu cố định, nội dung giới hạn bề rộng; màn danh sách và chi tiết nào cũng dùng hai khung từ 840 dp.

### 6.5 Hiệu ứng động (motion)

Hiệu ứng động giúp người dùng biết chuyện gì vừa xảy ra (menu mở ra từ đâu, server đã nhận yêu cầu chưa), không để trang trí. Mockup (§5.5) đã có các hiệu ứng dưới đây để nhóm bấm thử.

**Quy tắc:**
- **Chỉ đổi transform và opacity** (dịch, co giãn, xoay, độ trong). Trong Compose làm bằng `Modifier.graphicsLayer` để không phải đo và bố cục lại. Không tạo hiệu ứng cho kích thước, padding hay bán kính blur.
- **Hiệu ứng "thành công" chỉ chạy sau khi server trả lời** (đặt trước, gia hạn, tự mượn), đúng nguyên tắc không báo thành công giả ở §4. Lúc chờ chỉ có trạng thái "đang gửi".
- **Không có hiệu ứng trong hàng danh sách lúc cuộn.** Hàng chỉ trồi lên một lần khi tải trang đầu, tối đa 8 hàng đầu.
- **Hiệu ứng lặp** (vòng NFC lan ra, khung xương lấp lánh) chỉ có ở màn chờ chạm và lúc đang tải, và phải dừng khi màn rời khỏi màn hình hoặc app xuống nền.
- **Tôn trọng cài đặt của máy:** hệ số thời lượng hoạt ảnh bằng 0, hoặc bật "Xóa ảnh động" trong Hỗ trợ tiếp cận, thì tắt hết hiệu ứng và đổi trạng thái ngay. Thời lượng nhân theo hệ số hoạt ảnh của hệ thống. TalkBack đọc trạng thái mới, không phụ thuộc hiệu ứng.
- Mockup dùng `@media (prefers-reduced-motion: reduce)` để mô phỏng việc tắt hiệu ứng.

**Danh sách hiệu ứng:**

| Chỗ | Hiệu ứng | Thời lượng | Tầng Nhẹ |
|---|---|---|---|
| Vào màn | Các khối trồi lên 16 dp và hiện dần, khối sau chậm hơn 80 ms | 420 ms | Hiện ngay |
| Băng sách ở Danh mục | Bìa ở banner và thẻ trong băng cuộn trồi lên lần lượt, chậm dần 70–90 ms | 420 ms | Hiện ngay |
| Nút menu | ☰ chuyển thành ✕: hai gạch xoay ±45°, gạch giữa mờ đi | 260 ms | Đổi icon ngay |
| Menu ☰ | Khung kính phóng từ góc nút (co giãn 0.92 → 1 và hiện dần), các mục chậm dần 35 ms, nền phía sau tối dần | 260 ms, nền 200 ms | Hiện ngay, không blur |
| Danh sách sách | Hàng trồi lên chậm dần 40 ms (chỉ lần tải đầu); khung xương lấp lánh khi tải; vòng quay "đang tải thêm" | 320 ms; lấp lánh lặp 1.2 s | Khung xương đứng yên |
| Chip, nút, thanh chọn trạng thái | Đổi màu nền mượt; co 0.96 khi nhấn | 120–220 ms | Đổi màu ngay |
| Chờ chạm NFC (tự mượn, đăng nhập bằng thẻ) | Hai vòng lan ra lệch nhau; thẻ hoặc vòng giữa "thở" nhẹ | Lặp 1.8 s và 2.4 s | Vòng đứng yên |
| Kết quả từ server | Dấu tích được vẽ ra; vòng tròn bật lên (co giãn 0.6 → 1); lỗi thì thẻ lỗi rung ngang | 380–420 ms; rung 360 ms | Hiện ngay |
| Công tắc trong Tài khoản | Núm trượt ngang, nền đổi màu | 220 ms | Đổi ngay |
| Bảng xác nhận đăng xuất | Trượt lên từ đáy, nền tối dần | 320 ms | Hiện ngay |
| Khóa sinh trắc học | Icon ổ khóa mở bật lên | 380 ms | Đổi icon ngay |

Tầng Vừa có đủ các hiệu ứng trên, chỉ khác là không blur (§6.3). Icon morph thật (bằng `AnimatedVectorDrawable`) vẫn chỉ ở tầng Đầy đủ; tầng Vừa dùng cách đổi icon bằng xoay và mờ dần như mockup.

**Đường cong:** vào màn và mở bảng dùng đường cong giảm tốc (gần `cubic-bezier(0.2, 0.8, 0.2, 1)` trong mockup). Nếu bản Material 3 nhóm dùng có sẵn bộ token chuyển động thì dùng token đó thay cho số tự đặt.

**Đo:** thêm vào §8.5 các hành trình mở và đóng menu ☰, chuyển màn Danh mục → Danh sách → Chi tiết, và tải trang đầu của danh sách.

## 7. Ràng buộc hiệu năng cho mọi thay đổi UI

UI mới không được làm hỏng phần tối ưu của phiên `opt/app-catalog`:

- Hàng sách giữ tham số ổn định (stable) để Compose bỏ qua vẽ lại; component mới không truyền `List` thô.
- Ảnh bìa luôn có kích thước cố định và được Coil tải đúng kích thước hiển thị.
- Không thêm hiệu ứng trang trí nặng (bóng đổ, blur, gradient động) cho hàng danh sách: E2 của nghiên cứu đã đo được chi phí khung hình của loại hiệu ứng này (`study/e2-bench/REPORT.md`, yếu tố P08). Ngoại lệ là các lớp kính ở §5.5 và §6.3 (menu ☰, nút nổi, vùng mờ dần của ảnh lớn), không bao giờ nằm trong hàng danh sách, chỉ bật theo tầng và phải qua phép đo §8.5.
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
  - Một câu hỏi thêm cho bản B: "Giao diện app có hợp với điện thoại của bạn không?" (thang 1–5), để đo nỗi lo người dùng so sánh hiệu ứng kính của app với của máy
- **Điều hướng:** A dùng thanh tab, B dùng menu ☰ cộng nút nổi. Với T2 và T3, ghi thêm số lần bấm và người tham gia có tìm ra chức năng mà không cần trợ giúp không (§6.2).
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

- **Cách đo:** cùng một APK release, dùng công tắc ép tầng của bản debug-benchmark, cuộn danh mục 100k sách với nút nổi "Chạm sách để mượn" và vùng mờ dần ở đáy màn đè lên; đo thêm lúc mở và đóng menu ☰, lúc chuyển màn Danh mục → Danh sách → Chi tiết và lúc tải trang đầu của danh sách (hiệu ứng động ở §6.5). Ở màn đứng yên (không có hiệu ứng lặp), kiểm tra app không vẽ thêm frame nào (`dumpsys gfxinfo`), để chắc hiệu ứng lặp đã dừng. Dùng chung kịch bản và bộ dữ liệu của `opt/app-catalog` (Macrobenchmark, `FrameTimingMetric`), chạy theo thứ tự ABAB, nhiều vòng, báo trung vị và khoảng.
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
| 3. Thiết kế | Nhóm góp ý trên mockup 22 màn (§5.5) và các hiệu ứng động (§6.5) bằng bình luận; chốt design system, bảng màu, font; viết `ux-test.md` | Nhóm duyệt mockup | 1 ngày |
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
- **Khối lượng việc tăng mạnh** khi thiết kế lại toàn app (§5.5). Làm theo thứ tự: design system → chi tiết sách → danh mục và menu ☰ → đăng nhập → các màn thành viên → màn chào, và đo sau mỗi bước.
- **Baseline đã đạt chuẩn:** phiên `opt/app-catalog` đo trên Android 16 (commit `b7459dc`) thấy cuộn và bộ nhớ của app hiện tại đã đạt mục tiêu. Giao diện kính là thứ có thể kéo app vượt ngưỡng, nên §8.5 là bắt buộc trước khi nhận.
- **Menu ☰ giấu điều hướng** (§6.2); kết quả test có thể cho thấy cần quay lại thanh tab.

## 11. Việc nhóm cần chốt

1. Bảng màu và tên hiển thị trong app (ví dụ "ShelfTap" hay tên tiếng Việt).
2. Có cho Guest xem danh mục không (đúng `SPEC.md`, nhưng là thay đổi luồng).
3. Ai tuyển người tham gia, ở đâu, khi nào; điện thoại dùng để test.
4. Có thẻ sách NFC thật cho T2 chưa.
5. Thời điểm bắt đầu so với phiên `opt/app-catalog`.
6. Có thêm thư viện Haze (blur) và Material 3 Adaptive không.
7. Bộ icon (ví dụ Lucide hay Material Symbols) và có làm icon morph ở tầng Đầy đủ không.
8. Đã chốt: một app duy nhất, hiệu ứng theo tầng (§6.3), không làm bản riêng cho Android 17.
9. Đã chốt: phong cách kính cho toàn app (§5.5) và menu ☰ dạng khung kính dọc, cộng nút nổi tự mượn (§6.2).
10. Màu theo hình nền (Material You) hay màu thương hiệu cố định, hay kết hợp (kính và nền theo hệ thống, nút chính và logo theo thương hiệu).
11. Nguồn ảnh thư viện có giấy phép cho màn chào và banner.
12. Duyệt bộ hiệu ứng động ở §6.5 trên mockup: giữ, bỏ hay đổi thời lượng hiệu ứng nào.
13. Có sửa `server/API.md` để thêm kệ vào `TitleSummary` và tham số lọc theo chủ đề cho trang danh sách không (cùng đợt với `optimization/search-recommend-plan.md`).
14. Báo sách đặt trước đã sẵn sàng: thêm cơ chế đẩy vào `server/API.md` (ví dụ FCM, cần thêm dịch vụ của Google) hay app tự hỏi server định kỳ (tốn pin hơn, chậm hơn), hay tạm chỉ hiện trong app.
15. Đã vẽ (2026-11-24): hai màn Danh mục cho cửa sổ rộng (§6.4). Nhóm duyệt cách chia: thanh dọc ở 600–840 dp, menu cố định và hai khung từ 840 dp.

## 12. Sản phẩm đầu ra

- Nhánh `ui-ux` với design system, chuỗi tiếng Việt và tiếng Anh, accessibility, trạng thái UI, các thay đổi UX, hiệu ứng theo tầng và bố cục thích ứng, theo hướng thiết kế kính ở §5.5.
- Bảng chi phí hiệu ứng theo tầng (§8.5) và ngưỡng đã chốt cho từng tầng.
- `optimization/ux-test.md`: kịch bản, phiếu ghi và kết quả A/B.
- `optimization/log/<tên-nhánh>.md`: nhật ký từng thay đổi, gồm cả phần AI đề xuất và phần con người sửa.
- `optimization/ui-ux-mockup/`: mã nguồn mockup 22 màn và các script đo (tương phản chữ, tương phản icon và viền, vùng chạm), chạy lại được để lấy số cho báo cáo.
- Bảng chỉ số UI và UX trước/sau, ảnh chụp trước/sau, và một đoạn cho báo cáo và slide.
