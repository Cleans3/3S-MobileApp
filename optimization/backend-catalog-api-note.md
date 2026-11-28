# Ghi chú cho người làm backend: API tải danh mục (chưa làm, để sang đợt sau)

Ngày ghi: 2026-11-28. Nguồn: quyết định ở `optimization/ui-ux-plan-5d.md` §7. **Chưa thêm gì vào `server/API.md` và chưa làm server.** Ghi chú này chỉ lưu ý tưởng để người làm backend nhận lại sau.

## Vì sao cần

Màn **Tìm khi không có mạng** và **Gợi ý** dùng danh mục sách trên máy. Hôm nay app chỉ có tìm qua server (`GET /api/titles?q=...`, tối đa 50 cuốn mỗi trang), nên:
- không tìm được khi mất mạng;
- Gợi ý phải gọi `GET /api/titles/{id}` và tìm vài lần để có ứng viên (giới hạn 20 request mỗi phút của server), nên chậm và chỉ thấy vài chục cuốn.

Mục tiêu đã chốt cho phía app: tìm offline trong 100k sách dưới 50 ms (xem `CLAUDE.md`), kết quả kèm lý do gợi ý và số bản còn lấy từ server khi có mạng.

## Ý tưởng API (nháp, cần nhóm duyệt)

`GET /api/catalog/snapshot?since=<thời điểm hoặc phiên bản>&cursor=<con trỏ>`

- Trả danh mục theo từng khối (cursor), nén gzip, có `ETag` và hỗ trợ `If-None-Match` (trả 304 khi không đổi).
- Chỉ gồm các trường của `TitleSummary` **trừ `copiesAvailable`**: tình trạng còn sách đổi liên tục nên app luôn hỏi server riêng khi có mạng.
- `since` để chỉ tải phần thêm, sửa, xóa từ lần trước (cần cột `updated_at` hoặc số phiên bản, và dấu "đã xóa" để app gỡ sách).
- Giới hạn kích thước khối (ví dụ vài nghìn cuốn) thay cho giới hạn 50 cuốn mỗi trang; giữ giới hạn tốc độ riêng, vì đây là lệnh tải hàng loạt.
- Ai gọi được: thành viên đã đăng nhập hay cả khách? Danh mục không phải dữ liệu cá nhân, nhưng cần quyết định cùng việc chống tải hàng loạt (scraping).
- Trường cần cho tìm không dấu và gợi ý: tên, tác giả, chủ đề, ISBN, mã sách. **Quy tắc chuẩn hóa không dấu phải giống nhau ở server và app** (bỏ dấu, `đ` thành `d`, chữ thường): chốt quy tắc và bộ kiểm thử chung (có `đ`/`Đ`, `ư`, `ơ`, dấu tổ hợp) trước khi làm.

## Phía app sẽ làm khi API có

Room + bảng `@Fts4` trên chữ đã chuẩn hóa, đồng bộ bằng WorkManager (mạng không tính phí, đang sạc, lần đầu có thanh tiến độ), tìm theo tiền tố, nối từ màn "Mất kết nối" bằng nút "Tìm sách khi không có mạng", khi mạng có lại thì tự quay về Danh mục. SQLite của Android có FTS3/4 nhưng không có FTS5, nên không có `bm25`: xếp hạng tính trong app. Chi tiết và chỉ số đo ở `optimization/search-recommend-plan.md` §5 (nhánh `docs/search-recommend-plan`).

## Việc cần chốt với người làm backend

1. Hình dạng API (phiên bản hay thời điểm, cursor, định dạng nén) và ai được gọi.
2. Migration: cột `search_text` không dấu, `pg_trgm`, cột `updated_at`, dấu xóa mềm.
3. Ước lượng dung lượng: 100k sách ở dạng nén là bao nhiêu MB, tải lần đầu mất bao lâu trên mạng di động.
4. Sau khi có API: cập nhật `server/API.md` **trước**, rồi server, rồi app (quy tắc của dự án).
