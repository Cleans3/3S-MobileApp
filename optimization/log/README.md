# Nhật ký thay đổi theo nhánh

Mỗi nhánh có một file nhật ký ở đây: `optimization/log/<tên-nhánh>.md`, trong đó `/` của tên nhánh đổi thành `-` (nhánh `opt/app-catalog` ghi vào `opt-app-catalog.md`). Nhật ký là nguồn để viết báo cáo, nên mọi thay đổi code đều phải có một mục.

## Quy tắc

1. **Mỗi commit sửa code thêm đúng một mục** vào nhật ký của nhánh, trong **cùng commit** đó. Mục không cần ghi hash: `git log -- optimization/log/<tên-nhánh>.md` nối mục với commit.
2. Commit chỉ sửa tài liệu cũng nên có mục ngắn, để nhật ký kể đủ việc của nhánh.
3. **Chỉ ghi số đã đo.** Chưa đo thì ghi "chưa đo" và lý do. Không ghi số dự kiến như thể là kết quả.
4. Không sửa hay xóa mục cũ. Nếu một mục sai, thêm mục mới ghi "Sửa mục <ngày giờ>" và nói rõ điều gì sai.
5. Không ghi bí mật, mật khẩu, token hay dữ liệu cá nhân thật vào nhật ký.

## Đầu file

```markdown
# Nhật ký: <tên-nhánh>

- **Tách từ:** <nhánh gốc> tại <hash ngắn>
- **Mục đích:** <một câu>
- **Môn:** smart-card | optimization | security
```

## Mẫu một mục

Mục mới thêm vào cuối file.

```markdown
## <YYYY-MM-DD HH:MM> · <tiêu đề ngắn>

- **Vấn đề:** <triệu chứng, và mã taxonomy nếu có, ví dụ P09, U05>
- **Thay đổi:** <làm gì; các file chính>
- **Đo trước → sau:** <chỉ số, công cụ, thiết bị/Android, số lần chạy, trung vị> hoặc "chưa đo: <lý do>"
- **AI:** <công cụ và model; prompt tóm tắt; AI đề xuất gì; con người sửa gì; nhận hay bỏ>
- **Kiểm thử:** <lệnh đã chạy và kết quả>
- **Ghi chú cho báo cáo:** <rủi ro, giới hạn, việc còn lại>
```
