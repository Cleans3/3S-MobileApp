# Mockup giao diện kính ShelfTap và phép đo tương phản

Mã nguồn của mockup 20 màn ở `optimization/ui-ux-plan.md` §5.5, cùng các script đo dùng cho §5.3.

- **Mockup đang xem được:** https://claude.ai/artifact/GMkMmXLY2J2DP2MKf1yiGf. Các file `.dc.html` ở đây là bản 16 (ngày 2026-11-24): bản 14 cộng phần sửa công tắc và viền ô nhập sau khi đo tương phản cho icon và viền (bản 15), và viền của ô tìm kiếm ở màn Khách (bản 16).
- **Mở trực tiếp không chạy:** mỗi màn cần runtime `support.js` của trình xem artifact, file đó không có trong repo. Các script dưới đây không cần runtime: chúng tự đọc template và chạy phần script của từng màn.

## Nội dung

| File | Là gì |
|---|---|
| `*.dc.html` | 20 màn. Màu viết bằng biến CSS (token) có giá trị sáng và tối, font Nunito |
| `canvas.json` | Vị trí và tên các màn trên canvas |
| `tools/contrast.js` | Đo tương phản WCAG cho mọi đoạn chữ, ở theme sáng và tối (SC 1.4.3) |
| `tools/nontext-contrast.js` | Đo tương phản cho icon, viền ô nhập, công tắc và trạng thái đang chọn (SC 1.4.11) |
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

Chi tiết từng lỗi và cách sửa: `optimization/ui-ux-plan.md` §5.3 và `optimization/log/docs-ui-ux-plan.md`.
