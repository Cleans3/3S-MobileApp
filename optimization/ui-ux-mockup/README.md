# Mockup giao diện kính ShelfTap và phép đo tương phản

Mã nguồn của mockup 20 màn ở `optimization/ui-ux-plan.md` §5.5, cùng các script đo dùng cho §5.3.

- **Mockup đang xem được:** https://claude.ai/artifact/GMkMmXLY2J2DP2MKf1yiGf (bản 14, ngày 2026-11-24). Các file `.dc.html` ở đây là bản 14.
- **Mở trực tiếp không chạy:** mỗi màn cần runtime `support.js` của trình xem artifact, file đó không có trong repo. Các script dưới đây không cần runtime: chúng tự đọc template và chạy phần script của từng màn.

## Nội dung

| File | Là gì |
|---|---|
| `*.dc.html` | 20 màn. Màu viết bằng biến CSS (token) có giá trị sáng và tối, font Nunito |
| `canvas.json` | Vị trí và tên các màn trên canvas |
| `tools/contrast.js` | Đo tương phản WCAG cho mọi đoạn chữ, ở theme sáng và tối |
| `tools/hero-worst-case.js` | Tính riêng chữ trắng trên ảnh ở màn chào, ở trường hợp xấu nhất |
| `tools/check-scripts.js` | Chạy script của từng màn, gọi mọi handler, kiểm tra mọi biến trong template đều có giá trị |
| `contrast-results.json` | Kết quả của `tools/contrast.js` trên bản 14 |

## Chạy lại

Cần Node 18 trở lên. Chạy trong thư mục này:

```
node tools/check-scripts.js .
node tools/contrast.js .
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
- Không đo icon và viền (ngưỡng 3:1 cho thành phần không phải chữ).
- Đây là mockup web. Khi làm app phải đo lại bằng Accessibility Scanner trên máy thật.

## Kết quả (bản 14)

| | Sáng | Tối |
|---|---|---|
| Số đoạn chữ | 353 | 353 |
| Không đạt ở bản 13 (trước khi sửa) | 9 | 9 |
| Không đạt ở bản 14 | 5, đều là chữ trên ảnh ở màn chào | 0 |

Chữ trên ảnh ở màn chào, trường hợp xấu nhất (`tools/hero-worst-case.js`):

| Chữ | Cần | Bản 13 | Bản 14 |
|---|---|---|---|
| Dòng đầu của tiêu đề | 3 | 2.49 | 4.80 |
| Đoạn mô tả | 4.5 | 4.47 | 7.03 |
| Nút kính "Bắt đầu" | 4.5 | 3.70 | 5.57 |
| Link cuối màn | 4.5 | 7.65 | 10.11 |

Chi tiết từng lỗi và cách sửa: `optimization/ui-ux-plan.md` §5.3 và `optimization/log/docs-ui-ux-plan.md`.
