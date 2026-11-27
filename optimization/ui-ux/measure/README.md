# Đo khung hình của app thành viên (kế hoạch §8.5)

`measure.ps1` đo số khung hình trễ hạn và thời gian khung của app thành viên ShelfTap theo cách lặp lại được: nhiều lượt, các bản xen kẽ, báo trung vị và khoảng min–max. Lý do: một lượt đo cho mỗi bản có nhiễu cỡ ±2% khung trễ và ±10 ms ở phân vị 90, nên một con số không cho biết bản nào tốt hơn.

## Cách chạy

1. Dựng bản đo, không dùng bản debug. Bản debug (có cờ gỡ lỗi) chậm khoảng 5 lần và ART không biên dịch được nó (`status=verify`):

   ```
   cd android
   .\gradlew.bat :app:assembleMeasure "-Pshelftap.apiBaseUrl=https://<máy chủ dev>:8443/"
   ```

   Kiểu build `measure` giống `release` (R8, nhận Baseline Profile) nhưng ký bằng khoá debug và tin chứng chỉ dev của máy chủ để đo được với máy chủ dev. **Không bao giờ phát hành.** Chứng chỉ dev phải được cài làm CA người dùng trên máy đo.
2. Máy đã mở khóa, có mạng tới máy chủ dev (khoảng 100.000 đầu sách để `scrollList` có việc làm). App mở bằng màn chào khi chưa đăng nhập; script tự bấm "Xem sách mà không cần đăng nhập" sau mỗi lần khởi động. Chứng chỉ dev phải đã cài làm CA người dùng, nếu không danh sách trống và số đo vô nghĩa.
3. Chạy:

   ```
   cd optimization\ui-ux\measure
   .\measure.ps1 -Serial <serial> -Apks 'head=<apk>','truoc=<apk khác>' -Rounds 5 -Tier auto -Out .\results-<máy>-auto
   .\measure.ps1 -Serial <serial> -Apks 'head=<apk>' -Rounds 5 -Tier light -Out .\results-<máy>-light
   ```

## Điều script làm

- Mỗi lượt, mỗi bản được cài lại và nhận hồ sơ như khi cài từ cửa hàng (ghi hồ sơ trong APK rồi `compile -m speed-profile`).
- Thứ tự các bản đổi mỗi lượt (bản chạy đầu không luôn là bản lạnh), nghỉ `-Cool` giây giữa các lần để máy nguội.
- `-Tier light` tắt hoạt ảnh hệ thống, khiến app chọn tầng nhẹ; `auto` để nguyên máy (Android 17 là tầng đầy đủ, 12 đến 16 là tầng vừa).
- Ghi từng lượt vào `results.tsv` (khung, khung trễ, phân vị 50/90/95/99, RSS và PSS, nhiệt pin) và `summary.tsv` (trung vị và min–max cho mỗi bản, mỗi kịch bản).

## Kịch bản

| Tên | Làm gì |
|---|---|
| `openBook` | chạm thẻ sách đầu, chờ, quay lại, 12 lần |
| `openMenu` | mở và đóng menu ☰, 15 lần (bỏ qua ở cửa sổ không có nút ☰) |
| `scrollHome` | vuốt trang chủ lên xuống, 36 lần vuốt |
| `scrollList` | mở "Xem tất cả" rồi vuốt danh sách dài (tải thêm trang khi cuộn) |

## Cách đọc kết quả

- Chỉ kết luận "bản A tốt hơn bản B" khi **khoảng min–max của hai bản không chồng lên nhau**. Nếu chồng lên nhau thì ghi "không khác nhau đo được".
- Nêu số lượt (`runs`) bên cạnh số: 5 lượt vẫn là mẫu nhỏ.
- Số trên emulator chỉ để so sánh giữa các bản, vì emulator vẽ GPU bằng phần mềm (§8.5).

## Nếu script bị dừng giữa chừng ở `-Tier light`

`-Tier light` đặt ba thang hoạt ảnh hệ thống về 0 và khôi phục khi kết thúc (cả khi lỗi). Nếu bị dừng từ bên ngoài thì không khôi phục được, và app sẽ ở tầng nhẹ, không có chuyển động. Khôi phục bằng tay:

```
adb shell settings put global animator_duration_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global window_animation_scale 1.0
```

## Giới hạn

- Script chỉ chạy kịch bản bằng cách chạm và vuốt từ `adb`; cử chỉ back, chuyển cảnh nhiều bước và ảnh bìa thật chưa có kịch bản.
- Bộ chọn thẻ sách dựa vào mô tả "tên, tác giả, Còn N bản": đổi chữ này thì phải sửa `openBook`.
- Chưa có phép so với bản trước khi làm giao diện (`ui-baseline`): cần dựng bản đó với kiểu `measure` và sửa bộ chọn cho giao diện cũ.
