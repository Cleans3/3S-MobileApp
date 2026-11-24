# Mockup giao diện kính ShelfTap và phép đo tương phản

Mã nguồn của mockup 32 màn (20 màn điện thoại và 12 màn cửa sổ rộng) ở `optimization/ui-ux-plan.md` §5.5, cùng các script đo dùng cho §5.3.

- **Mockup đang xem được:** https://claude.ai/artifact/GMkMmXLY2J2DP2MKf1yiGf. Các file `.dc.html` ở đây là bản 21 (ngày 2026-11-24): bản 14 cộng phần sửa công tắc và viền ô nhập (bản 15), viền của ô tìm kiếm ở màn Khách (bản 16), vùng chạm tối thiểu 48 px và lối vào màn Gợi ý và Thông báo (bản 17), rồi ba lỗi bố cục tìm ra khi thử rộng 320 và chữ 2.0 cùng băng "Gợi ý cho bạn" trên Danh mục và Danh mục cuộn dọc (cả hai đăng chung trong bản 18), và hai màn cửa sổ rộng `WideCatalog`, `RailCatalog` (bản 21; số bản nhảy từ 18 lên 21 dù chỉ đăng một lần, và 23 file đang đăng ở bản 21 trùng từng byte với repo). 10 màn rộng còn lại (`WideLoans` đến `WideStatus`) và phần thu gọn của `WideCatalog` có trong repo nhưng chưa đăng lên link.
- **Mở trực tiếp không chạy:** mỗi màn cần runtime `support.js` của trình xem artifact, file đó không có trong repo. Các script dưới đây không cần runtime: chúng tự đọc template và chạy phần script của từng màn.

## Nội dung

| File | Là gì |
|---|---|
| `*.dc.html` | 32 màn: 20 màn điện thoại 390 × 844, 11 màn `Wide*` 960 × 720 và `RailCatalog` 720 × 840. Màu viết bằng biến CSS (token) có giá trị sáng và tối, font Nunito |
| `canvas.json` | Vị trí và tên các màn trên canvas |
| `tools/contrast.js` | Đo tương phản WCAG cho mọi đoạn chữ, ở theme sáng và tối (SC 1.4.3) |
| `tools/nontext-contrast.js` | Đo tương phản cho icon, viền ô nhập, công tắc và trạng thái đang chọn (SC 1.4.11) |
| `tools/target-size.js` | Đọc kích thước vẽ và vùng chạm của mọi nút, link và công tắc |
| `tools/render-static.js` | Dựng các màn thành HTML tĩnh ở 11 biến thể: gốc, rộng 320, chữ 2.0, cả hai, và bảy cỡ màn tỷ lệ đặc biệt. Màn cửa sổ rộng giữ kích thước vẽ ở biến thể gốc và chữ 2.0; các màn `Wide*` dựng thêm ở 844 × 390 (`land`) |
| `tools/wide-boards/` | Script sinh 10 màn rộng từ một khung chung (`node tools/wide-boards/gen.js .`, ghi lại các file `.dc.html` và vị trí trong `canvas.json`). Sửa các màn đó ở đây rồi chạy lại; `WideCatalog` và `RailCatalog` viết tay |
| `tools/layout-check.js` | Chạy trong trình duyệt trên trang của `render-static.js`, sau khi tắt hiệu ứng động: đếm chữ tràn mép, tràn đáy, chữ đè chữ, chữ bị cắt; trên màn rộng hơn 600 dp đếm thêm dòng quá 80 ký tự và khối bị kéo giãn |
| `tools/hero-worst-case.js` | Tính riêng chữ trắng trên ảnh ở màn chào, ở trường hợp xấu nhất |
| `tools/check-scripts.js` | Chạy script của từng màn, gọi mọi handler, kiểm tra mọi biến trong template đều có giá trị |
| `contrast-results.json` | Kết quả của `tools/contrast.js` |
| `nontext-results.json` | Kết quả của `tools/nontext-contrast.js` |

## Chạy lại

Cần Node 18 trở lên. Chạy trong thư mục này:

```
node tools/check-scripts.js .
node tools/contrast.js .
node tools/nontext-contrast.js .
node tools/target-size.js .
node tools/render-static.js . rendered   # rồi mở rendered/*.html qua một web server cục bộ và chạy tools/layout-check.js trong trang
node tools/hero-worst-case.js
```

## Cách `tools/contrast.js` đo

1. Chạy `renderVals()` của màn với theme sáng hoặc tối, rồi dựng template: mở `sc-if` và `sc-for`, thay `{{...}}`.
2. Thay mỗi `var(--token)` bằng giá trị của theme đó, lấy từ khối `.st-root` trong `<style>` của chính màn.
3. Với mỗi đoạn chữ: màu chữ lấy từ phần tử gần nhất có ghi màu; màu nền là các lớp nền của phần tử cha chồng lên nhau theo độ trong (alpha), bắt đầu từ nền của màn.
4. Mô phỏng màu mặc định của trình duyệt: nút và ô nhập không ghi màu thì chữ đen; link lấy quy tắc `a{color:...}` của màn; chữ gợi ý (placeholder) là #757575 nếu màn không ghi màu riêng.
5. Ngưỡng WCAG 2.2 AA: 4.5:1, hoặc 3:1 cho chữ lớn (từ 24 px, hoặc từ 18.66 px và đậm 700).

**Giới hạn:**
- Không tính được nền là ảnh hay gradient nằm ở phần tử anh em (không phải cha). Vì vậy 5 đoạn chữ trắng trên ảnh ở màn chào luôn bị báo là không đạt; chúng được tính riêng bằng `tools/hero-worst-case.js`.
- Bỏ qua `backdrop-filter`: kính được tính như lớp bán trong phẳng, không có blur.
- Đây là mockup web. Khi làm app phải đo lại bằng Accessibility Scanner trên máy thật.

## Kết quả: chữ (SC 1.4.3)

| | Sáng | Tối |
|---|---|---|
| Số đoạn chữ | 353 | 353 |
| Không đạt ở bản 13 (trước khi sửa) | 9 | 9 |
| Không đạt ở bản 14 và 15 | 5, đều là chữ trên ảnh ở màn chào | 0 |

Chữ trên ảnh ở màn chào, trường hợp xấu nhất (`tools/hero-worst-case.js`):

| Chữ | Cần | Bản 13 | Bản 14 |
|---|---|---|---|
| Dòng đầu của tiêu đề | 3 | 2.49 | 4.80 |
| Đoạn mô tả | 4.5 | 4.47 | 7.03 |
| Nút kính "Bắt đầu" | 4.5 | 3.70 | 5.57 |
| Link cuối màn | 4.5 | 7.65 | 10.11 |
| Icon thẻ trên nút kính (không phải chữ) | 3 | 3.70 | 5.57 |

## Cách `tools/nontext-contrast.js` đo (SC 1.4.11, ngưỡng 3:1)

Dùng chung cách dựng màn và tính màu của `tools/contrast.js`, rồi chia làm hai nhóm:

- **Bắt buộc theo SC 1.4.11:**
  - icon trong nút hoặc link không có chữ hiện ra (☰, quay lại, mắt, chia sẻ…), so với nền ngay sau icon;
  - ô nhập: viền hoặc nền của ô, so với nền bên ngoài; ô tìm kiếm thì lấy khung `<label>` bao quanh;
  - công tắc: rãnh so với nền bên ngoài, núm so với rãnh, ở cả trạng thái bật và tắt;
  - chip hoặc tab đang chọn (`aria-pressed="true"`): nền so với nền bên ngoài.
- **Khuyến nghị:** icon đứng cạnh chữ, và mép của nút hoặc link có chữ. SC 1.4.11 không bắt buộc mép nút khi chữ hoặc icon trong nút đã nhận diện được nút.

Bỏ qua phần chỉ có trong bản phác: thanh xem trước trạng thái và nút Sáng/Tối.

## Kết quả: icon, viền và trạng thái (SC 1.4.11)

| | Sáng | Tối |
|---|---|---|
| Mục bắt buộc | 37 | 37 |
| Không đạt ở bản 14 | 13 | 12 |
| Không đạt ở bản 15 | 1, là icon thẻ trên ảnh ở màn chào (tính riêng: 5.57) | 0 |
| Icon khuyến nghị (icon cạnh chữ) | 62, đều đạt | 62, đều đạt |
| Mép nút và link (khuyến nghị) | 100, 78 không đạt | 100, 83 không đạt |

Lỗi ở bản 14 và cách sửa ở bản 15:
- **Công tắc** (Tài khoản, Thông báo): rãnh tắt chỉ 1.59 (sáng) và 1.94 (tối), núm trắng trên rãnh tắt 1.65, rãnh bật ở nền tối 1.97. Bản 15 làm theo kiểu Material 3: rãnh tắt có viền 2 px màu `ink3` và núm nhỏ màu `ink3`; rãnh bật màu `--switch-on` (xanh đậm ở nền sáng, xanh nhạt ở nền tối) với núm tương phản.
- **Ô nhập** (Đăng nhập, Đăng nhập lại, Đổi mật khẩu, Lịch sử đọc): viền 14% chỉ đạt 1.33 đến 1.5. Bản 15 dùng viền 50% của màu chữ.
- **Ô tìm kiếm** (Danh mục, Danh sách, Tìm khi không có mạng): khung kính gần như không thấy (1.0 đến 1.2). Bản 15 thêm viền 50% của màu chữ.
- **Ô tìm kiếm ở màn Khách:** đây là link mở màn Danh sách nhưng trông như ô nhập, nên không nằm trong nhóm bắt buộc. Bản 16 vẫn cho nó cùng viền với các ô tìm kiếm khác (3.15 ở nền sáng, 4.74 ở nền tối) để giao diện nhất quán, và đặt tên "Tìm sách" cho trình đọc màn hình.

Mép nút kính không đạt 3:1 là chủ ý của phong cách kính và không bắt buộc. Khi làm app, nếu kiểm tra với người dùng cho thấy khó nhận ra nút thì tăng viền.

## Vùng chạm (kế hoạch: 48 dp; WCAG 2.2 SC 2.5.8 AA: 24 px)

`tools/target-size.js` đọc `width`, `height` và `min-height` của từng nút, link và công tắc sau khi dựng màn. Màn nào có quy tắc `::after` (lớp trong suốt rộng ít nhất 48 × 48 px quanh mỗi nút) thì vùng chạm tính là ít nhất 48 px. Script không biết phần tử cha có cắt mất lớp đó hay không, nên các hàng cuộn ngang được kiểm bằng mắt.

| | Bản 16 | Bản 17 |
|---|---|---|
| Nút, link, công tắc | 133 | 135 (thêm 2 lối vào) |
| Vẽ nhỏ hơn 48 px | 32 | 32 (giữ nguyên hình) |
| Vùng chạm nhỏ hơn 48 px | 32, cộng 4 link chữ không rõ kích thước | 0 |

## Rộng 320 dp và cỡ chữ 2.0

`tools/render-static.js` dựng các màn ở theme sáng, trạng thái mặc định (menu và bảng đóng). Cỡ chữ 2.0 phóng phi tuyến theo kiểu Android 14 trở lên; bảng số trong script lấy theo AOSP như tôi nhớ, cần đối chiếu trước khi trích. `tools/layout-check.js` bỏ qua lớp trang trí (`aria-hidden`), phần nằm trong vùng cuộn, và nhãn ẩn 1 px cho trình đọc màn hình.

| Biến thể | Màn có lỗi | Số lỗi |
|---|---|---|
| Gốc: rộng 390, chữ bình thường | 0 / 20 | 0 (bản 17: 3 lỗi thật, đã sửa ở bản 18) |
| Rộng 320 | 0 / 20 | 0 |
| Chữ 2.0 | 10 / 20 (lượt đo đầu: 13) | 30 (lượt đo đầu: 41) |
| Rộng 320 và chữ 2.0 | 16 / 20 (lượt đo đầu: 18) | 55 (lượt đo đầu: 65) |

"Lượt đo đầu" là bản 17 cộng ba lỗi bố cục đã sửa, trước khi thêm băng gợi ý; trạng thái này không được đăng riêng. Trong bản 18, Danh mục cuộn được và script không tính chữ trong vùng cuộn đi qua dưới thanh nổi là chữ đè (vùng cuộn chừa chỗ ở đáy). Hai thay đổi này cùng làm số giảm; Danh mục từ 6 lỗi (chữ 2.0) còn 0.

Lỗi ở chữ 2.0 là do mockup đặt khối ở vị trí cố định trong màn 844 px không cuộn. Mockup không sửa phần này; quy tắc cho app ở `optimization/ui-ux-plan.md` §6.4. Một vài lỗi chữ đè ở Tự mượn, Gợi ý và Đăng nhập lại là thanh xem trước trạng thái, chỉ có trong bản phác.

## Màn tỷ lệ đặc biệt

Bảy cỡ màn thêm vào `tools/render-static.js`, chữ bình thường, đơn vị dp. Đây là nhóm kích thước chứ không phải số đo của từng mẫu máy, vì thông số các máy gập năm 2026 ở các nguồn không khớp nhau (xem kế hoạch §6.4).

| Cỡ màn | dp | Màn vỡ bố cục | Lỗi vỡ | Màn bị kéo giãn |
|---|---|---|---|---|
| Điện thoại dài 21:9 (`tall`) | 412 × 960 | 0 / 20 | 0 | không áp dụng |
| Màn ngoài rộng 10:16 của máy gập ngang (`wide`) | 480 × 768 | 1 / 20 | 1 | không áp dụng |
| Màn phụ của máy gập dọc (`flipcover`) | 360 × 400 | 15 / 20 | 85 | không áp dụng |
| Chia đôi màn hình (`split`) | 390 × 420 | 15 / 20 | 84 | không áp dụng |
| Điện thoại xoay ngang (`land`) | 844 × 390 | 14 / 20 | 78 | 18 / 20 |
| Máy gập mở, gần vuông (`unfold`) | 720 × 840 | 0 / 20 | 0 | 17 / 20 |
| Máy gập mở, cầm ngang 4:3 (`unfoldl`) | 960 × 720 | 4 / 20 | 7 | 18 / 20 |

"Lỗi vỡ" là tràn mép, tràn đáy, chữ đè và chữ bị cắt. "Màn bị kéo giãn" là màn có dòng quá 80 ký tự hoặc có thẻ, hàng hay nút rộng quá 85% màn; chỉ tính khi màn rộng hơn 600 dp.

Màn thấp vỡ vì cùng lý do với chữ 2.0: chỉ năm màn có vùng cuộn (Danh mục, Danh sách, Thông báo, Tìm khi không có mạng, Gợi ý) giữ nguyên. Màn rộng không vỡ nhưng là bố cục điện thoại bị kéo giãn, vì mockup chưa có màn nào cho cửa sổ rộng.

Lượt đo đầu có 4 lỗi "tràn đáy" giả ở màn Thông báo: bảng hỏi quyền còn đang trượt lên khi đo. Từ lượt này, `tools/layout-check.js` tắt hiệu ứng động trước khi đo.

Bảng trên là số đo ngày 2026-11-24 với script hiện tại, đo hai lượt cho cùng kết quả. Lần ghi trước, `flipcover` là 84 (74 tràn đáy) và `split` là 85 (70 tràn đáy); nay là 85 (75 tràn đáy, 10 chữ đè) và 84 (69 tràn đáy, 15 chữ đè). Các màn điện thoại không đổi, và bản dựng từ commit trước đo bằng script hiện tại cũng ra đúng các số mới; vậy chênh lệch đến từ script đo hoặc lượt đo cũ, không phải từ mockup.

## Cửa sổ rộng

12 màn theo cách chia cỡ cửa sổ của Material 3 (kế hoạch §6.4). Mỗi chức năng của 20 màn điện thoại có một màn rộng:

| Màn | Thay cho màn điện thoại | Bố cục ở cửa sổ từ 840 dp |
|---|---|---|
| `WideCatalog` | Danh mục, Danh sách, Chi tiết | Menu cố định, danh sách, chi tiết sách; bấm sách thì khung phải đổi theo |
| `WideLoans` | Sách đang mượn | Danh sách lượt mượn, chi tiết và nút gia hạn; sách quá hạn không gia hạn được |
| `WideReservations` | Đặt trước | Danh sách, chi tiết; hủy có hộp xác nhận ngay trong khung phải |
| `WideHistory` | Lịch sử đọc | Bước hỏi mật khẩu khi đăng nhập bằng thẻ, rồi danh sách và chi tiết |
| `WideRecommend` | Gợi ý cho bạn | Danh sách kèm lý do, chi tiết "vì sao gợi ý"; ba trạng thái mật khẩu, thẻ, tắt lịch sử |
| `WideOfflineSearch` | Tìm khi không có mạng | Như Danh mục, không có số bản còn, nút đặt trước tắt vì cần mạng |
| `WideCheckout` | Tự mượn sách | Hình chỉ chỗ có ăng-ten NFC bên trái, kết quả bên phải; bốn trạng thái |
| `WideAccount` | Tài khoản, Thông báo | Cài đặt hai khung: mục bên trái, nội dung bên phải; đăng xuất xác nhận trong khung |
| `WideSignIn` | Màn chào, Đăng nhập, Đăng nhập bằng thẻ, Đổi mật khẩu | Ảnh kệ sách bên trái, nội dung bên phải |
| `WideStatus` | Khởi động, Khóa, Mất kết nối, Đăng xuất kèm lý do | Một thẻ ở giữa, rộng 460 dp |
| `WideGuest` | Chế độ khách | Menu rút gọn, danh sách, chi tiết; "Đặt trước" mở lời mời đăng nhập ngay trong khung |
| `RailCatalog` | Danh mục ở cửa sổ 600–840 dp | Thanh điều hướng dọc 96 dp, một khung nội dung rộng tối đa 560 dp |

Màn gộp nhiều chức năng có thanh xem trước trạng thái (nền tối), chỉ có trong bản phác. Menu trong các màn rộng trỏ sang màn rộng tương ứng.

**Cửa sổ rộng mà hẹp hoặc thấp.** Điện thoại xoay ngang rộng hơn 840 dp nên cũng dùng bố cục rộng, nhưng chỉ cao khoảng 390 dp. Khi cửa sổ hẹp hơn 900 dp hoặc thấp hơn 480 dp, menu cố định thu thành thanh dọc 72 dp chỉ có icon (tên mục đọc qua `aria-label`), hai khung dịch sang trái. Mockup làm bằng container query trên khung màn, nên `render-static.js` dựng các màn `Wide*` ở 844 × 390 để đo phần này.

| Biến thể | Màn rộng | Lỗi vỡ | Dòng quá 80 ký tự hoặc khối kéo giãn |
|---|---|---|---|
| Kích thước vẽ | 12 | 0 | 0 |
| Chữ 2.0 | 12 | 0 | 0 |
| 844 × 390 (`land`) | 11 (không tính `RailCatalog`) | 0 | 0 |

Tương phản chữ: 862 đoạn mỗi theme, tối 0 lỗi, sáng chỉ còn 5 đoạn chữ trên ảnh ở màn chào như trước. Mục bắt buộc về icon và viền: 51, chỉ còn icon trên ảnh ở màn chào. Vùng chạm: 310 nút, 0 dưới 48 px.

Các lỗi tìm ra và đã sửa khi vẽ: menu cố định của `WideCatalog` đè mục ở chữ 2.0 vì không cuộn; ở 844 × 390 khung chi tiết chỉ còn 244 dp và cắt chữ (dẫn tới phần thu gọn ở trên); ghi chú ở cột trái màn Tự mượn tràn đáy ở chữ 2.0.

Sửa trong script đo:
- `tools/layout-check.js` chỉ so chữ đè trên phần chữ thật sự hiện ra (chip trong hàng cuộn ngang từng bị báo nhầm), và không coi chữ trong vùng cuộn là "bị cắt" (người dùng cuộn tới được). Khung bao ngoài toàn màn của `WideStatus` và `WideHistory` đổi từ `section` sang `div` để không bị tính là "khối kéo giãn"; nhãn vùng chuyển vào thẻ ở giữa.
- `tools/render-static.js` trước đây in chữ `<#root>` lên đầu mỗi màn; đã bỏ. Số đo không đổi vì script đo tính theo khung màn.

Chi tiết từng lỗi và cách sửa: `optimization/ui-ux-plan.md` §5.3 và `optimization/log/docs-ui-ux-plan.md`.
