# Demo minh họa kỹ thuật tối ưu mà nhóm nghiên cứu

Ngày 28/11/2026 · Chủ đề: tối ưu (ứng dụng Android, nghiên cứu `optimization/study`)

Bộ demo này đặt **bản nháp đầu do trợ lý lập trình AI viết (A, chưa tối ưu)** cạnh **bản đã tối ưu (B)** của cùng một màn hình, trên máy ảo Android, với cùng một thao tác cho cả hai bên. Mỗi lần chạy, công cụ in số đo trực tiếp của A và B cạnh số liệu của nghiên cứu.

**Quy tắc khi trình bày:** số liệu của nghiên cứu (E2, `e2-bench/REPORT.md`) là số tham chiếu. Số đo trực tiếp chỉ để minh họa: chỉ so A với B trên cùng một máy ảo, không đọc như kết quả nghiên cứu và không so với điện thoại thật.

Toàn bộ thao tác đi qua một tệp: `demo.py` (Python 3, chỉ dùng thư viện chuẩn, chạy trên Windows và Linux). Hướng dẫn cho trợ lý lập trình của người trình bày nằm ở `AGENT-BRIEF.md` (tiếng Anh).

Số slide trong tài liệu này theo bộ slide 27 trang đã chốt: các slide về từng lỗi là S11 đến S16, **slide 20 "Demo: trước và sau tối ưu" là chỗ chạy demo trực tiếp (120 giây)**, slide 8 là "Thiết bị thật".

Trong tài liệu này lệnh viết cho Windows (`py`). Trên Linux hoặc macOS thay `py` bằng `python3`.

## 1. Cần chuẩn bị

| Thứ cần có | Ghi chú |
|---|---|
| Máy tính Windows 10/11 hoặc Linux, x86_64 | 16 GB RAM để chạy hai máy ảo cùng lúc; 8 GB chỉ chạy được một máy ảo. Khoảng 12 GB ổ đĩa trống. |
| Android Studio (bản mới) | Cài kèm Android SDK, Android Emulator và JDK 21 (thư mục `jbr`). `demo.py` tự tìm cả ba. |
| Trong Android Studio > Settings > Android SDK | Tab *SDK Tools*: Android SDK Platform-Tools, Android Emulator. Tab *SDK Platforms* (bật *Show Package Details*): Android 14.0 (API 34) > **Google APIs Intel x86_64 Atom System Image**. Nếu thiếu, `setup` tự thử tải. |
| Ảo hóa phần cứng | Windows: bật *Windows Hypervisor Platform* (Turn Windows features on or off), hoặc cài *Android Emulator hypervisor driver* trong SDK Tools. Linux: KVM. `doctor` kiểm tra mục này. |
| Python 3.8 trở lên | Windows: bản cài từ python.org (có lệnh `py`). |
| ffmpeg (không bắt buộc) | Chỉ để `record` ghép hai video A và B thành một tệp. |
| Kết nối mạng | Chỉ cần ở lần build đầu (Gradle tải thư viện). Lúc trình bày không cần mạng. |

## 2. Cài đặt (khoảng 10 phút)

Mở PowerShell (Linux: terminal) ở thư mục gốc của repo:

```
cd optimization/study/demo
py demo.py doctor
py demo.py setup
py demo.py run p09
```

- `doctor` in từng mục OK / LỖI và dòng cuối `Kết quả: ĐẠT`. Dòng `--` chỉ là thông tin (ví dụ AVD chưa tạo, setup sẽ tạo).
- `setup` làm bốn việc: build ứng dụng thí nghiệm `android/optlab` (hai bản build: có R8 và không R8), tạo hai AVD `shelftap-demo-a` và `shelftap-demo-b` theo đúng cấu hình máy ảo của nghiên cứu (API 34, 2 nhân, 2 GB RAM, màn hình 720x1280), khởi động máy ảo, rồi cài ứng dụng và tạo dữ liệu (ảnh bìa và 100.000 đầu sách). Lần đầu mất 5 đến 10 phút; build chiếm phần lớn.
- `run p09` là lần chạy thử đầu tiên. Thấy bảng số ở cuối là xong.
- Không thấy Android SDK: đặt biến môi trường rồi chạy lại, ví dụ `$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"` (PowerShell).

Các lệnh khác: `py demo.py list` (danh sách kịch bản), `py demo.py start` (khởi động máy ảo), `py demo.py record p09` (ghi video), `py demo.py stop` (tắt máy ảo).

## 3. Một máy ảo hay hai máy ảo

Mỗi máy ảo dùng khoảng 3 GB RAM của máy tính (2 GB cho Android, phần còn lại cho trình giả lập và GPU giả lập bằng phần mềm). Vì vậy:

- **Mặc định:** khi khởi động, nếu máy tính còn trống từ 7.000 MB RAM trở lên thì `demo.py` bật **hai máy ảo** (trái là A, phải là B), còn không thì bật **một máy ảo** và chạy A rồi đến B. Máy 16 GB đã tắt Android Studio và trình duyệt nặng thường đủ cho hai máy ảo.
- Ép chế độ: thêm `--single` hoặc `--pair` vào `setup`, `start`, `run`, `record`.
- **Hai máy ảo** đẹp hơn khi trình chiếu vì khán giả thấy A và B chạy cùng lúc. Nhưng hai máy ảo tranh CPU của nhau, nên chỉ các số đếm (số mục, MB) là tin được; các số thời gian (ms, %) có thể lệch, nhất là ở B. Khi chạy thử trên máy 16 nhân, tỷ lệ khung hình trễ của B ở P08 là 27% khi chạy một máy ảo và 80% khi chạy hai máy ảo.
- **Một máy ảo** cho số thời gian ổn định hơn. Muốn đọc to số ms hoặc % thì dùng `--single`.

## 4. Trước giờ trình bày (15 phút trước)

1. Đóng Android Studio, trình duyệt có nhiều tab và các ứng dụng nặng.
2. `py demo.py doctor`, xem dòng cuối là `ĐẠT`.
3. `py demo.py start` (hoặc `start --single`). Máy ảo khởi động trong 30 đến 60 giây. Lần đầu, kéo cửa sổ A sang trái và B sang phải; máy ảo nhớ vị trí cho lần sau.
4. Chạy `py demo.py run p09` một lần cho ứng dụng và dữ liệu nóng lên.
5. Mở sẵn thư mục `optimization/study/evidence/video` (video dự phòng, mục 7).

## 5. Từng kịch bản

Cách đọc bảng kết quả: cột **A chưa tối ưu**, **B đã tối ưu** là số đo trực tiếp; cột **Nghiên cứu A → B** là trung vị của ít nhất 12 lần lặp trong nghiên cứu, cùng cấu hình máy ảo. "Đã thấy khi chạy thử" là khoảng giá trị đo được khi chuẩn bị bộ demo (máy Linux 16 nhân, 16 GB RAM), để biết số nào là bình thường.

### p09: Danh sách dài dựng toàn bộ (slide S11, S15, S20)

- **Lệnh:** `py demo.py run p09`
- **Khán giả thấy:** màn hình chi tiết sách với 2.000 lượt mượn. A mở chậm và vuốt không mượt; B mở nhanh và vuốt mượt.
- **Nói:** "Bản nháp đầu dựng cả 2.000 mục ngay khi mở màn hình, dù chỉ khoảng 30 mục hiện lên. Bản đã tối ưu dùng LazyColumn, chỉ dựng phần đang hiện."
- **Chỉ vào số:** số mục đã dựng (luôn là 2.000 so với vài chục), rồi tỷ lệ khung hình trễ (jank) và mức dùng RAM (RSS).

| Chỉ số | Nghiên cứu A → B | Đã thấy khi chạy thử (A / B) |
|---|---|---|
| số mục đã dựng | 2.000 → 31 | 2.000 / 56 đến 58 (vuốt xa hơn nghiên cứu nên B dựng nhiều mục hơn) |
| tỷ lệ khung hình trễ (jank) | 90% → 1,3% | 87 đến 99% / 0 đến 6% |
| thời gian xử lý khung hình (p90) | 27,1 → 5,9 ms | 24 đến 50 ms / 4 đến 11 ms |
| thời gian khởi động (TTID) | 1.901 → 573 ms | 1.568 đến 3.453 ms / 495 đến 1.117 ms (lần chạy đầu sau setup chậm nhất) |
| mức dùng RAM (RSS anon) | 243 MB → 43 MB | 215 đến 224 MB / 41 đến 44 MB |
| Java heap | 220 MB → 7,6 MB (đỉnh) | 97 MB / 8,1 MB (đo sau khi vuốt, không phải đỉnh) |

### p08: Hiệu ứng trang trí trên thẻ sách (slide S12, S20)

- **Lệnh:** `py demo.py run p08`. Kịch bản này cần GPU giả lập khác (`swangle_indirect`, như nghiên cứu), nên lần đầu `demo.py` tự tắt máy ảo và khởi động lại, mất 20 đến 60 giây. Nên để p08 cuối cùng, hoặc nói phần khác trong lúc chờ.
- **Khán giả thấy:** cùng một lưới bìa sách. A có đổ bóng 5 dp, lớp phủ chuyển sắc (gradient) và bìa vẽ bằng Canvas; B dùng khối bìa phẳng.
- **Nói:** "Chỉ bỏ hiệu ứng trang trí, thời gian xử lý khung hình giảm rõ. Nhưng bản B vẫn chưa đạt 60 khung hình mỗi giây trên máy ảo này, nên nhóm xếp lỗi này là đáng kể, không phải nghiêm trọng."
- **Chỉ vào số:** thời gian xử lý khung hình (p90) và tỷ lệ khung hình trễ.

| Chỉ số | Nghiên cứu A → B | Đã thấy khi chạy thử (A / B) |
|---|---|---|
| tỷ lệ khung hình trễ (jank) | 70,52% → 48,65% | 86 đến 93% / 22 đến 27% (một máy ảo), 80% (hai máy ảo) |
| thời gian xử lý khung hình (p90) | 62,87 → 49,07 ms | 35 đến 57 ms / 25 đến 47 ms |
| mức dùng RAM (RSS anon) | 45 MB → 42 MB | 43 đến 44 MB / 38 đến 41 MB |

Với p08 nên dùng `--single` nếu cần đọc số: ở chế độ hai máy ảo, B bị A làm chậm theo.

### p06: Ảnh bìa giải mã ở kích thước gốc (slide S14, S20)

- **Lệnh:** `py demo.py run p06`
- **Khán giả thấy:** danh sách sách có ảnh bìa. Hai bên trông giống nhau; khác biệt nằm ở bộ nhớ.
- **Nói:** "Mỗi ảnh bìa gốc là 1500x2250 điểm ảnh. Bản nháp đầu giải mã nguyên kích thước dù ô hiển thị chỉ 150x210 dp; bản đã tối ưu giải mã vừa ô."
- **Chỉ vào số:** dung lượng ảnh đã giải mã và mức dùng RAM (RSS) so với mục tiêu 150 MB của môn học.

| Chỉ số | Nghiên cứu A → B | Đã thấy khi chạy thử (A / B) |
|---|---|---|
| dung lượng ảnh đã giải mã | 203 MB → 6,2 MB (khoảng 32 lần) | 338 MB / 10 đến 11 MB (vuốt xa hơn nên nhiều ảnh hơn; tỷ lệ vẫn khoảng 32 lần) |
| mức dùng RAM (RSS anon) | 180 MB → 60 MB | 164 đến 182 MB / 57 đến 62 MB |

### p13: Tìm kiếm trên 100.000 đầu sách (slide S16, S20)

- **Lệnh:** `py demo.py run p13`
- **Khán giả thấy:** ô tìm kiếm tự gõ "silent river" từng phím; mỗi phím là một truy vấn.
- **Nói:** "Bản nháp đầu tìm bằng LIKE '%…%', tức là quét cả bảng 100.000 đầu sách ở mỗi phím. Bản đã tối ưu dùng chỉ mục toàn văn FTS4."
- **Chỉ vào số:** truy vấn chậm nhất so với mục tiêu dưới 50 ms của môn học.
- Nói thêm cho trung thực: trong nghiên cứu, chỉ mục không làm khung hình mượt hơn; nó chỉ làm truy vấn nhanh hơn.

| Chỉ số | Nghiên cứu A → B | Đã thấy khi chạy thử (A / B) |
|---|---|---|
| truy vấn chậm nhất | 96,63 → 37 ms | 83 đến 111 ms / 24 đến 30 ms |
| tổng thời gian truy vấn | 634,9 → 85,42 ms | 858 đến 1.091 ms / 69 đến 91 ms (13 và 12 truy vấn) |

### p10: Bản build không bật R8 (slide S16)

- **Lệnh:** `py demo.py run p10`
- **Khán giả thấy:** không có gì để xem trên màn hình; đây là kịch bản về số.
- **Nói:** "Cùng mã nguồn. Bản build tắt R8 nặng gấp khoảng 10 lần. Thời gian khởi động chỉ chênh vài phần trăm."
- **Chỉ vào số:** dung lượng APK (không đổi giữa các lần chạy). Thời gian khởi động chênh ít, có thể đảo chiều ở một lần chạy; nếu vậy, nói số của nghiên cứu (giảm 8,7%).

| Chỉ số | Nghiên cứu A → B | Đã thấy khi chạy thử (A / B) |
|---|---|---|
| dung lượng APK | 24,7 MB → 2,4 MB | 24,7 MB / 2,4 MB |
| thời gian khởi động (TTID) | 505,6 → 461,6 ms | 488 đến 496 / 470 đến 471 ms (một máy ảo), 806 / 766 ms (hai máy ảo) |

### p01 và p0304: Những gì không tốn chi phí đo được (slide S13)

- **Lệnh:** `py demo.py run p01`, `py demo.py run p0304`
- **Khán giả thấy:** hai bên giống nhau.
- **Nói:** "Có những điều trợ lý AI hay làm sai theo sách vở, như mục danh sách không có key (P01), hay tham số không ổn định và tính toán trong composition (P03+P04), nhưng đo thì không thấy chi phí. Bài học: đo trước khi tối ưu."
- **Chỉ vào số:** hai cột A và B gần bằng nhau. Ở p0304 thời gian xử lý khung hình p90 dao động quanh 16,7 ms, nên lần này A nhỉnh hơn, lần sau B nhỉnh hơn. Đó chính là "không khác biệt".

| Kịch bản | Chỉ số | Nghiên cứu A → B | Đã thấy khi chạy thử (A / B) |
|---|---|---|---|
| p01 | số mục đã dựng | 28 → 28 | 48 / 48 |
| p01 | thời gian khởi động (TTID) | 617,1 → 620 ms | 452 đến 453 / 444 đến 476 ms (một máy ảo), 676 / 714 ms (hai máy ảo) |
| p01 | tỷ lệ khung hình trễ | (nghiên cứu không đo khung hình cho P01) | 0% / 0% |
| p0304 | thời gian recompose (luồng chính) | 36,21 → 36,75 ms | 32 đến 38 / 31 đến 33 ms |
| p0304 | thời gian xử lý khung hình (p90) | 11,56 → 9,80 ms (khoảng tin cậy chứa 0) | 8 đến 19 ms, cả hai bên |

## 6. Thứ tự trình bày

**2 phút cho slide 20** ("Demo: trước và sau tối ưu", 120 giây). Chạy hai kịch bản rõ nhất, cùng một GPU nên không phải khởi động lại máy ảo. Điều kiện: đã làm mục 4 trước giờ trình bày (máy ảo đang chạy, đã chạy `run p09` một lần). Gõ sẵn lệnh đầu tiên trong cửa sổ lệnh trước khi tới slide 20.

| Thời điểm | Làm gì | Câu nói |
|---|---|---|
| 0:00 | `py demo.py run p09` (chạy 25 đến 50 giây) | "Cùng màn hình 2.000 lượt mượn: bên trái là bản nháp đầu của trợ lý AI, bên phải là bản đã tối ưu. Nhìn bên trái mở chậm và vuốt không mượt." |
| 0:45 | Chỉ vào dòng "số mục đã dựng" trong bảng kết quả | "Bản nháp dựng cả 2.000 mục, bản đã tối ưu chỉ dựng vài chục mục đang hiện. Nghiên cứu đo khung hình trễ 90% xuống 1,3%." |
| 1:00 | `py demo.py run p13` (chạy 30 đến 50 giây) | "Gõ cùng một từ khóa trong 100.000 đầu sách: bản nháp quét cả bảng ở mỗi phím, bản đã tối ưu dùng chỉ mục." |
| 1:45 | Chỉ vào dòng "truy vấn chậm nhất" | "Mục tiêu môn học là dưới 50 ms. Nghiên cứu đo 96,63 ms xuống 37 ms; số trên máy ảo này chỉ để minh họa." |
| 2:00 | Sang slide tiếp theo | |

Dự phòng cho slide 20: nếu máy ảo chưa sẵn sàng hoặc một lệnh lỗi, không sửa giữa buổi. Quay lại slide 11 (video P09 có sẵn trong slide) rồi mở `optimization/study/evidence/video/P13c-AB.mp4` (video P13 không có trong slide); số cần đọc là số trên các thẻ của slide 20. Nếu chỉ còn thời gian cho một kịch bản, chạy `run p09`.

**5 phút** (ba kịch bản, cùng một GPU, không phải khởi động lại):

| Phút | Lệnh | Slide |
|---|---|---|
| 0:00 | `run p09`: danh sách dài, khung hình trễ và RAM | S11, S15 |
| 2:00 | `run p06`: ảnh bìa và RAM | S14 |
| 3:30 | `run p13`: tìm kiếm so với mục tiêu 50 ms | S16 |
| 5:00 | Kết: "số liệu nghiên cứu là tham chiếu, số vừa chạy chỉ minh họa" | |

**10 phút** (p08 để cuối vì phải đổi GPU):

| Phút | Lệnh | Slide |
|---|---|---|
| 0:00 | `run p09` | S11, S15 |
| 2:00 | `run p06` | S14 |
| 3:30 | `run p13` | S16 |
| 5:00 | `run p10` (chỉ có số, chạy khoảng 30 giây) | S16 |
| 6:00 | `run p01`: "không phải điều gì sách vở bảo sai cũng tốn chi phí" | S13 |
| 7:30 | `run p08`: máy ảo tự khởi động lại 20 đến 60 giây; trong lúc chờ, giới thiệu thẻ sách có đổ bóng và gradient | S12 |
| 10:00 | Kết | |

Mỗi lệnh `run` mất 30 đến 60 giây (một máy ảo lâu hơn hai máy ảo vì chạy A rồi mới đến B).

## 7. Khi có sự cố giữa buổi

| Hiện tượng | Làm gì |
|---|---|
| Máy ảo treo, đóng đột ngột, hoặc `run` báo lỗi `device offline` | Mở video dự phòng (bảng dưới) và nói tiếp. Sau phần đó: `py demo.py stop`, rồi `py demo.py start`. |
| Số trực tiếp lạ (ví dụ B chậm hơn A) | Nói: "số trực tiếp dao động; số của nghiên cứu là ..." rồi đọc cột Nghiên cứu. Chạy lại bằng `--single` nếu còn thời gian. |
| `run` không chịu chạy vì thiếu AVD hoặc APK | Đã chưa chạy `setup` trên máy này: dùng video dự phòng. |
| Máy tính hết RAM, chậm toàn bộ | `py demo.py stop`, rồi `py demo.py start --single`. |
| p08 làm máy ảo đóng đột ngột | Không nên xảy ra với `swangle_indirect` (nghiên cứu: 8 trên 8 lần mở không lỗi). Nếu xảy ra: mở `P08c-AB.mp4`. |

**Video dự phòng** (không cần máy ảo), trong `optimization/study/evidence/video/`, mỗi video ghép A (trái) và B (phải), cùng thao tác vuốt, 14 giây, quay khi làm nghiên cứu. Ba video đã nằm sẵn trong slide: P09 ở slide 11, P08 ở slide 12, P06 ở slide 14.

| Kịch bản | Video | Ghi chú |
|---|---|---|
| p09 | `P09c-AB.mp4` (slide 11) | A vuốt không mượt, B mượt |
| p08 | `P08c-AB.mp4` (slide 12) | quay trên GPU `swangle_indirect` |
| p06 | `P06c-AB.mp4` (slide 14) | hai bên trông như nhau; nói số RAM |
| p13 | `P13c-AB.mp4` (không có trong slide, mở riêng) | gõ "silent river" |
| p01 | `P01c-AB.mp4` | không thấy khác biệt, đúng như số đo |
| p10 | không có video | đọc bảng số (dung lượng APK) |
| p0304 | không có video | đọc bảng số; có thể dùng `P01c-AB.mp4` làm ví dụ "không khác biệt" |

Muốn có video mới của chính máy mình: `py demo.py record p09` ghi `out/rec/p09-A.mp4` và `out/rec/p09-B.mp4`; nếu có ffmpeg thì ghép thêm `out/rec/p09-AB.mp4`.

## 8. Vì sao số trực tiếp khác số nghiên cứu

- **Máy ảo khác, thao tác khác.** Nghiên cứu lấy trung vị của ít nhất 12 lần lặp, mỗi lần khởi động nguội, bằng Macrobenchmark. Demo chạy một lần và vuốt theo kịch bản của video dự phòng, xa hơn kịch bản đo. Vì vậy số mục đã dựng và số ảnh đã giải mã lớn hơn, nhưng tỷ lệ A so với B vẫn như nghiên cứu.
- **Thời gian xử lý khung hình** được tính theo cùng định nghĩa với nghiên cứu: từ lúc luồng chính bắt đầu khung hình đến lúc RenderThread vẽ xong, khung vượt 16,67 ms là khung hình trễ. Riêng demo trừ thêm thời gian RenderThread chờ máy ảo nhận khung hình (`eglSwapBuffers`). Khoảng chờ này trên máy ảo lúc gần 0, lúc bằng một chu kỳ màn hình, tùy lần chạy, và làm cả A lẫn B trông chậm như nhau. Trừ nó đi thì số ổn định giữa các lần chạy.
- **Java heap** được đo một lần sau khi vuốt; nghiên cứu lấy giá trị lớn nhất trong cả lần chạy, nên số của nghiên cứu cao hơn.
- **Hai máy ảo cùng lúc** tranh CPU với nhau (mục 3).
- Số đo trên máy ảo là tỷ lệ giữa A và B, không phải số của một điện thoại thật.

## 9. ShelfTap làm đối tượng demo (không bắt buộc)

ShelfTap (`android/app`), ứng dụng thư viện của nhóm, không phải đối tượng của nghiên cứu. Nhóm đã đo trước và sau một số thay đổi của chính ứng dụng này trên một điện thoại thật (Xiaomi 25053RT47C, Android 17, màn 120 Hz) bằng `dumpsys gfxinfo`. Có thể dùng các kết quả này làm ví dụ "trước và sau" trên thiết bị thật, bên cạnh demo trên máy ảo. Đây là số đã đo, không phải số chạy trực tiếp.

**Kết quả đã có** (đường dẫn tính từ thư mục `optimization/`):

| Thay đổi | Trước → sau | Nguồn |
|---|---|---|
| Cuộn trang chủ: nhớ các ảnh bìa không tải được để không yêu cầu lại, dựng sẵn thẻ phía trước khi cuộn | tỷ lệ khung hình trễ 7,0–7,2% → 1,5–2,7%; p90 53–57 → 29–36 ms; p99 109–117 → 65–81 ms (3 và 2 lượt xen kẽ) | `log/opt-ui-ux.md:289` |
| Mở trang sách: bản build debug → bản build đo hiệu năng (`measure`, cấu hình tương đương bản phát hành) | p90 125 / 85 / 61 / 53 ms → 12–16 ms; khung hình trễ 12,7 / 8,5 / 4,7 / 5,2% → 1,5–1,6%; p99 → 29–44 ms (số này cũng nằm ở slide 8) | `log/opt-ui-ux.md:362` |
| Baseline Profile riêng của ứng dụng, cùng bản build đo | khung hình trễ 1,44–1,49% so với 1,45–1,60%: không khác nhau đo được | `log/opt-ui-ux.md:362` |
| Mở trang sách sau các thay đổi tiếp theo (trang dựng sẵn, bỏ vòng tròn nền) | khung hình trễ 0,35–0,82%; p90 8–9 ms; p99 13–15 ms | `log/opt-ui-ux.md:402` |
| Chiều ngược lại, hiệu ứng tốn thêm: menu nền kính mờ, mở và đóng 20 lần | p90 9–11 → 18 ms; khung trễ theo hệ thống 1 → 1 | `log/opt-ui-ux.md:237` |
| Chiều ngược lại: dải kính mờ ở mép trên khi cuộn (`measure.ps1`, 5 lượt xen kẽ, bản build đo) | p90 cuộn trang chủ 5 → 8 ms, cuộn danh sách 5 → 9 ms; khung hình trễ 0,02% ở cả hai | `log/opt-ui-ux.md:683`, `ui-ux/measure/results-xiaomi-haze/summary.tsv:2-5` |
| Thẻ kính thay hàng phẳng trong danh sách dài (5 lượt xen kẽ) | p90 5 ms so với 5 ms; mức dùng RAM (PSS) trung vị 94 so với 95 MB | `log/opt-ui-ux.md:650`, `ui-ux/measure/results-xiaomi-list/summary.tsv:2-3` |

Hai dòng "chiều ngược lại" khớp với P08 của nghiên cứu: hiệu ứng trang trí có chi phí đo được. Ngoài hiệu năng còn có số về giao diện (không phải tối ưu): phát hiện accessibility 4 → 2, màn hình vỡ bố cục ở cỡ chữ 2.0 là 5/7 → 0/7, chuỗi viết cứng 51 → 0 (`ui-ux-metrics.md:12-16`).

**Cách trình chiếu:**

1. Rẻ nhất: đọc bảng trên, hoặc dùng slide 8 "Thiết bị thật" đã có số bản build debug so với bản build đo và số ảnh bìa lỗi.
2. Ảnh: `ui-ux/audit/A/light/` (trước khi làm giao diện, tag `ui-baseline`) cạnh `ui-ux/audit/B/light/` (sau), cùng tên tệp cho mỗi màn hình (ví dụ `catalog.png`). Đây là trước và sau về giao diện, không phải về hiệu năng. Ảnh trong `ui-ux/compare/` là bản thiết kế (trái) cạnh ứng dụng (phải), không phải trước và sau.
3. Hai bản build trên điện thoại thật: `ui-ux/measure/measure.ps1` (PowerShell) cài lần lượt từng bản, đo 5 lượt xen kẽ và ghi `summary.tsv`. Cách chạy ở `ui-ux/measure/README.md`. Cần bản build `measure` trỏ tới máy chủ dev có khoảng 100.000 đầu sách và chứng chỉ dev cài làm CA người dùng trên điện thoại. Mỗi lần đo mất nhiều phút, nên chạy trước buổi trình bày và chỉ chiếu `summary.tsv`, không chạy trong 120 giây của slide 20.

**Chưa có, nói thẳng nếu được hỏi:**

- Không có video trước và sau của ShelfTap. Ứng dụng đặt `FLAG_SECURE`, nên ảnh chụp và quay màn hình (kể cả `demo.py record` và phản chiếu màn hình) đều ra màn đen (`log/opt-ui-ux.md:153`). Muốn quay thì dùng camera ngoài.
- `demo.py` không chạy ShelfTap: ứng dụng cần máy chủ dev, chứng chỉ và dữ liệu, và máy ảo không có NFC.
- Bản build debug và bản build đo dùng cùng tên gói `shelftap.android`, nên không cài song song trên một máy. So sánh tận mắt cần hai điện thoại, hoặc cài lại lần lượt như `measure.ps1` làm.
- Chưa có bản `ui-baseline` dựng kiểu `measure`, nên chưa có số hiệu năng trước khi làm giao diện (`ui-ux/measure/README.md`, mục Giới hạn).
- Chưa đo trên máy giá rẻ; mọi số ở trên là của một máy flagship 120 Hz.
