# Bài test UX của ShelfTap (A và B)

- **Môn:** optimization (chủ đề 1, UI/đồ họa)
- **Kế hoạch:** `optimization/ui-ux-plan.md` §6.1 (tác vụ), §8.3 (cách đo)
- **Trạng thái:** kịch bản, phiếu ghi và bảng kết quả. **Chưa có buổi test nào được chạy**, mọi ô kết quả để trống.
- **Phiên bản A:** tag `ui-baseline` (APK debug đã lưu ở thư mục ghi chú chung, SHA-256 trong `ui-ux-metrics.md`). Thanh 5 tab.
- **Phiên bản B:** nhánh `opt/ui-ux` sau khi xong §5 và §6.2. Chưa có.

Không điền số dự kiến vào bảng kết quả như thể đó là kết quả (Đề cương mục VI.1).

## 1. Chuẩn bị

| Việc | Ghi chú |
|---|---|
| Người tham gia | 5 người cho A, 5 người khác cho B. Sinh viên chưa dùng ShelfTap. Mỗi người chỉ dùng một phiên bản. |
| Điện thoại | Một máy Android có NFC, dùng cho cả A và B, cùng cỡ chữ và độ sáng. Ghi tên máy và bản Android vào phiếu. Nhóm chốt (§11, mục 3). |
| Thẻ sách | Cần thẻ NFC ISO 15693 thật cho T2 (§11, mục 4). Nếu chưa có, T2 chỉ chạy được tới bước "giữ sách lên máy" và phải ghi rõ là không đo T2. |
| Tài khoản | Tài khoản dev của server thử. Không dùng thẻ, CCCD hay email thật (Nghị định 13/2023). Mỗi người một tài khoản, đặt lại dữ liệu trước mỗi người (sách chưa mượn, một sách đang mượn cho T3). |
| Dữ liệu | Sách cần tìm cho T1 có ít nhất hai bản, một bản đang cho mượn. Danh mục đủ lớn để phải tìm chứ không lướt hết được. |
| Ghi hình | `FLAG_SECURE` bật nên `adb screenrecord` ra màn đen. **Không tắt `FLAG_SECURE`.** Ghi bằng người quan sát cầm đồng hồ và phiếu, hoặc quay bằng camera ngoài, không quay mặt người tham gia. |
| Dữ liệu cá nhân | Phiếu không ghi tên. Dùng mã `A1`…`A5`, `B1`…`B5`. |

## 2. Kịch bản đọc to

Người quan sát đọc đúng lời này cho mọi người, không giải thích thêm và không gợi ý cách làm. Nếu người tham gia hỏi "bấm vào đâu", trả lời "bạn cứ làm như bạn nghĩ là đúng" và ghi là cần trợ giúp khi đã quá 90 giây không tiến triển.

> Đây là app thư viện. Mình đang kiểm tra app, không phải kiểm tra bạn. Không có đáp án sai. Bạn cứ nói to điều bạn đang nghĩ khi dùng. Nếu muốn dừng lúc nào cũng được.
>
> **Việc 1.** Bạn cần cuốn "[tên sách do nhóm chọn]". Hãy tìm nó và cho mình biết còn bản nào để mượn không.
>
> **Việc 2.** Bạn đang cầm cuốn sách này (đưa sách có thẻ). Hãy mượn nó bằng app.
>
> **Việc 3.** Bạn có một cuốn đang mượn. Hãy gia hạn nó và cho mình biết hạn trả mới.

Thời gian tính từ lúc đọc xong câu hỏi đến lúc người tham gia nói đã xong hoặc đạt điều kiện thành công. Dừng việc sau 180 giây và ghi là thất bại.

| # | Bắt đầu | Thành công khi | Thất bại khi |
|---|---|---|---|
| T1 | Màn hình đầu tiên của app | Mở đúng trang chi tiết sách cần tìm, và nói đúng còn bản hay không | Mở sách khác, hoặc nói sai, hoặc quá 180 s |
| T2 | Đã đăng nhập, cầm sách | Màn hình hiện "Đã mượn" do server xác nhận | Màn hình báo lỗi mà người dùng không xử lý được, hoặc quá 180 s |
| T3 | Đã đăng nhập, có sách đang mượn | Thấy hạn trả mới trên màn hình | Không gia hạn được, hoặc đọc sai hạn, hoặc quá 180 s |

Tác vụ phụ (chỉ làm nếu còn thời gian): đặt trước một sách đã hết; tắt lịch sử đọc.

## 3. Phiếu ghi (quan sát viên, một phiếu mỗi người)

```
Mã người tham gia: ____    Phiên bản: A / B    Ngày: ____    Giờ bắt đầu: ____
Máy: ____    Android: ____    Người quan sát: ____

Việc  | Thời gian (giây) | Thành công / Cần trợ giúp / Thất bại | Số lỗi | Số bước (chạm) | Số lần bấm tìm menu | Ghi chú (lời nói, chỗ vướng)
T1    |                  |                                      |        |                |                     |
T2    |                  |                                      |        |                |                     |
T3    |                  |                                      |        |                |                     |
```

Định nghĩa:

- **Lỗi:** chạm vào chỗ không dẫn tới mục tiêu và phải quay lại, hoặc nhập sai và phải sửa. Mỗi lần tính một.
- **Bước:** mỗi lần chạm vào màn hình. Cuộn không tính.
- **Cần trợ giúp:** quan sát viên phải nhắc, hoặc quá 90 s không tiến triển rồi mới xong. Tính là thành công một phần, báo riêng.
- **Tìm menu (chỉ B, T2 và T3):** người tham gia có tìm ra chức năng mà không cần trợ giúp không (có/không), và số lần chạm trước khi thấy đúng mục (§6.2, rủi ro của menu ☰).

## 4. Bảng câu hỏi sau buổi test

**SUS** (System Usability Scale): 10 câu, mỗi câu thang 1 (hoàn toàn không đồng ý) đến 5 (hoàn toàn đồng ý). Điểm tính theo cách chuẩn: câu lẻ lấy (điểm − 1), câu chẵn lấy (5 − điểm), cộng lại rồi nhân 2,5; kết quả 0–100.

1. Tôi nghĩ tôi sẽ muốn dùng app này thường xuyên.
2. Tôi thấy app phức tạp không cần thiết.
3. Tôi thấy app dễ dùng.
4. Tôi nghĩ tôi cần người hỗ trợ kỹ thuật để dùng được app này.
5. Tôi thấy các chức năng của app được ghép với nhau hợp lý.
6. Tôi thấy app có quá nhiều chỗ không nhất quán.
7. Tôi nghĩ hầu hết mọi người sẽ học cách dùng app này rất nhanh.
8. Tôi thấy app rất rườm rà khi dùng.
9. Tôi thấy tự tin khi dùng app.
10. Tôi cần học nhiều thứ trước khi dùng được app này.

**Chỉ ở bản B:** "Giao diện app có hợp với điện thoại của bạn không?" (1–5). Đo nỗi lo người dùng so sánh hiệu ứng kính của app với của máy.

| Mã | C1 | C2 | C3 | C4 | C5 | C6 | C7 | C8 | C9 | C10 | Điểm SUS | Hợp với máy (1–5, chỉ B) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| A1 | | | | | | | | | | | | – |
| A2 | | | | | | | | | | | | – |
| A3 | | | | | | | | | | | | – |
| A4 | | | | | | | | | | | | – |
| A5 | | | | | | | | | | | | – |
| B1 | | | | | | | | | | | | |
| B2 | | | | | | | | | | | | |
| B3 | | | | | | | | | | | | |
| B4 | | | | | | | | | | | | |
| B5 | | | | | | | | | | | | |

## 5. Bảng kết quả (để trống)

Chỉ báo số mô tả: trung vị, khoảng (nhỏ nhất–lớn nhất), số người thành công trên 5. **Không tính p-value**: cỡ mẫu nhỏ, và theo quy tắc của nghiên cứu chỉ C1–C5 mới có p-value.

| Chỉ số | Việc | A | B |
|---|---|---|---|
| Task success (n/5) | T1 | | |
| | T2 | | |
| | T3 | | |
| Time-on-task, trung vị (s) | T1 | | |
| | T2 | | |
| | T3 | | |
| Số lỗi, trung vị | T1 | | |
| | T2 | | |
| | T3 | | |
| Số bước, trung vị | T1 | | |
| | T2 | | |
| | T3 | | |
| SUS, trung vị | cả buổi | | |
| Tìm ra menu không cần trợ giúp (n/5) | T2, T3 (chỉ B) | – | |
| "Hợp với điện thoại" (1–5), trung vị | cả buổi (chỉ B) | – | |

## 6. Cách dùng với các chỉ số UI

Bảng này đo người dùng. Các chỉ số của máy (phát hiện accessibility, contrast, vùng chạm, vỡ layout, chuỗi cứng) ở `optimization/ui-ux-metrics.md`, cột A đã đo, cột B chưa. Hai bảng bổ sung cho nhau và không thay thế nhau: một app có thể đạt hết chỉ số máy mà vẫn khó dùng.

## 7. Giới hạn

- 5 người mỗi nhóm chỉ đủ thấy lỗi lớn, không đủ kết luận thống kê.
- Hai nhóm khác người, nên khác biệt A và B có thể đến từ người chứ không từ app.
- Người quan sát bấm giờ bằng tay, sai số cỡ một hai giây.
- Nếu không có thẻ NFC thật thì T2 không đo được; ghi rõ ở cả hai cột.
