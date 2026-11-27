# "AI slop" trong ứng dụng Android: đo lường và khắc phục chi phí về giao diện, bộ nhớ và bảo mật của mã do AI sinh ra

*AI slop in Android apps: measuring and fixing the UI, memory and security costs of AI-generated code*

Ứng dụng minh họa: ShelfTap (thư viện thông minh dùng NFC). Phạm vi: Android native, Kotlin và Jetpack Compose.

> **Trạng thái bản thảo (2026-11-23).** Chương 1–6 đã được viết; mọi số liệu thực nghiệm ở Chương 5 được chép từ các REPORT và CSV đã commit của từng thí nghiệm, trừ hiệu chỉnh Holm (Mục 5.7) là phép tính của bài. Còn lại trước khi nộp: hai lần chạy khám phá của E2 chưa thực hiện (độ nhạy với biến thể A viết tay; quét số lượng lượt mượn), chạy lại các tìm kiếm tài liệu của Mục 1.2, và các hạn chế đã nêu ở Mục 5.8.
>
> **Quy ước.** (1) `[Khóa]` là khóa của một nguồn trong `references.md` (nguồn đã kiểm chứng, kèm cách kiểm chứng và từng con số được dùng). (2) `[Protocol §n]` là mục tương ứng của `00-protocol.md`, tức giao thức đăng ký trước đã đóng băng; ngưỡng, cỡ mẫu và quy tắc quyết định nêu trong bài là quy ước đặt trước khi có dữ liệu kết quả, không suy ra từ dữ liệu của nghiên cứu. (3) Mọi con số trong bài hoặc có khóa nguồn, hoặc là tham số của giao thức, hoặc (ở Chương 5) được lấy từ một tệp CSV hay REPORT được nêu tên. (4) Số được viết theo quy ước tiếng Việt: dấu chấm ngăn cách hàng nghìn, dấu phẩy là dấu thập phân (ví dụ 1.689 và 75,7%); trong các tệp CSV và bảng kết quả dùng dấu chấm thập phân. (5) Các thuật ngữ kỹ thuật (Compose, recomposition, Keystore, ...) giữ nguyên tiếng Anh. (6) Nhân quả chỉ được viết là "giả thuyết" khi chưa có nhánh thực nghiệm nào kiểm tra nó.

## Tóm tắt

Trợ lý lập trình AI cho phép dựng một ứng dụng Android hoàn chỉnh từ một mô tả ngắn, nhưng bản nháp đầu tiên chưa được ai thẩm định. Nghiên cứu này đo xem bản nháp đó làm ứng dụng tốn thêm gì về giao diện (độ mượt, recomposition), bộ nhớ và bảo mật, và phần nào trong số đó **không** biến mất khi áp dụng các kỹ thuật tối ưu quen thuộc, vì nguyên nhân nằm ở chính quá trình sinh mã. Nghiên cứu gồm năm thực nghiệm trên một miền thống nhất (ShelfTap): E1 thu thập kho bản nháp đầu do ba bộ công cụ AI sinh ra từ ba brief với ba kiểu prompt (naive, detailed-functional, guardrailed) và so với ứng dụng do người viết; E2 đo chi phí của từng vấn đề bằng cặp phiên bản A (mã "slop" lấy từ E1) và B (đã tối ưu); E3 quét tĩnh các APK bằng công cụ độc lập và phân loại thủ công; E4 đặt trợ lý trước năm lỗi Android có chủ đích để xem lỗi được sửa an toàn hay bằng cách làm yếu bảo mật; E5 giao cho trợ lý vai trò người tối ưu trên ứng dụng có danh sách lỗi chuẩn. Giao thức được đăng ký và đóng băng trước khi chạy: chỉ năm phép kiểm định xác nhận (hiệu chỉnh Holm) có p-value được diễn giải, mọi so sánh khác chỉ là ước lượng kèm khoảng tin cậy; kết quả không kết luận được và kết quả âm được báo cáo cùng vị trí với kết quả dương.

**Kết quả.** Năm phép kiểm định xác nhận (hiệu chỉnh Holm, m = 5; số liệu ở Chương 5). C1: ở bản nháp đầu, tỷ lệ lần chạy có ít nhất một phát hiện nhóm bảo mật giảm từ 31/33 (naive) xuống 17/34 (guardrailed), hiệu +0,439, p hiệu chỉnh 0,00016; nhưng trong các ứng dụng build được hiệu này là +0,000, và chỉ 42 trên 100 lần chạy build được. C2: số mã luật khác nhau mỗi lần chạy giảm từ 5,85 xuống 2,82, hiệu +3,025, p hiệu chỉnh 0,00005. C3: mật độ phát hiện nhóm bảo mật trên 1000 dòng của mã AI không guardrail cao hơn ứng dụng do người viết trước 30/11/2022 (trung vị 2,37 so với 1,02; delta Cliff +0,582, p hiệu chỉnh 0,00027). C4: tỷ lệ sửa lỗi bằng cách làm yếu bảo mật ở E4 là 17/148 = 0,115 [0,073; 0,176], dưới ngưỡng 0,15 và p hiệu chỉnh 0,3106, nên **không được ủng hộ**. C5: ô lưới có hiệu ứng trang trí do AI sinh ra tốn thêm +28,13% [13,38; 54,31] ở P90 thời gian CPU khung hình so với bản phẳng (62,87 so với 49,07 ms; p hiệu chỉnh 0,000616), trên một đường GPU của emulator. Ngoài ra, ở E2, hai mẫu cấp mã (P06, P01 cùng P09) có chi phí lớn, một mẫu (P13) làm nhanh truy vấn nhưng không cải thiện khung hình, và hai (P03 cùng P04, nhân tố con P01) không có chi phí đo được; E3 và E5 là mô tả. Mọi số đo thời gian chạy trên emulator.

**Từ khóa:** AI coding, Android, Jetpack Compose, hiệu năng giao diện, bảo mật di động, OWASP MASVS, nghiên cứu thực nghiệm, đăng ký trước.

---

## Chương 1. Tổng quan đề tài

### 1.1. Bối cảnh

Các trợ lý lập trình dựa trên mô hình ngôn ngữ lớn (Claude Code, Cursor, GitHub Copilot, ChatGPT và nhiều công cụ khác) đã chuyển từ việc gợi ý từng dòng sang việc dựng cả dự án từ một mô tả bằng lời. Mốc công bố ChatGPT ngày 30 tháng 11 năm 2022 [ChatGPT2022] được nghiên cứu dùng làm ranh giới để phân tầng kho ứng dụng do người viết (Mục 4.3): mã có commit đầu và bản ghim cuối trước ngày đó được coi là mã viết trước khi các trợ lý trò chuyện được dùng rộng rãi. Bài không trích số liệu về mức độ phổ biến của các công cụ này, vì chưa có nguồn đã kiểm chứng trong `references.md`.

Ứng dụng di động là một miền mà chi phí của mã kém chất lượng hiện ra ngay trên thiết bị của người dùng. Trên Android, khung hình phải được kết xuất trong dưới 16 ms để đạt 60 khung hình mỗi giây, và khung hình chậm là jank; khung hình đóng băng là khung hình mất hơn 700 ms [Android-Jank]. Ứng dụng không phản hồi một sự kiện nhập trong 5 giây sẽ gây ra ANR [Android-ANR]. Trong một nghiên cứu 70 lỗi hiệu năng thực tế của tám ứng dụng Android, dạng lỗi chiếm đa số là GUI lagging (53 trên 70, tức 75,7%), kế đến là rò rỉ năng lượng và memory bloat (8 trên 70, tức 11,4%) [Liu2014]. Về bảo mật, kiểm tra chứng chỉ TLS sai là một lớp lỗi lâu đời của chính mã do người viết: một phân tích tĩnh 15.000 ứng dụng vẫn tìm thấy 5.511 ứng dụng mới phát hành có mã kiểm tra chứng chỉ dễ tổn thương [Oltrogge2021].

Android được chọn vì có đủ công cụ đo và kiểm tra có thể lặp lại: Macrobenchmark và Profiler cho hiệu năng, Memory Profiler cho bộ nhớ, MobSF, JADX và OWASP MASVS/MASTG cho bảo mật, và có thể chạy trên Emulator (đề cương ban đầu, mục I.3). Phạm vi là Android native với Kotlin và Jetpack Compose.

### 1.2. Nghiên cứu liên quan và khoảng trống

Phần này tóm tắt `01-related-work.md`. Các bài arXiv chưa qua bình duyệt được đánh dấu "bản thảo"; con số của bản thảo có thể đổi giữa các phiên bản (phiên bản đã đọc ghi trong `references.md`). Các con số của những nghiên cứu khác nhau (mô hình, tác vụ, ngôn ngữ, bộ phát hiện) **không so sánh được với nhau**; chúng được nêu để định vị nghiên cứu, không để gộp.

**An toàn và chất lượng của mã do AI sinh ra.** Pearce và cộng sự cho Copilot hoàn thành 89 kịch bản dựa trên các lớp điểm yếu rủi ro cao, tạo 1.689 chương trình, và thấy khoảng 40% trong số đó có lỗ hổng [Pearce2022]; đây là Copilot năm 2021, không phải bằng chứng về các trợ lý hiện nay. Trong một thí nghiệm có người tham gia, những người dùng trợ lý AI (codex-davinci-002) viết mã kém an toàn hơn có ý nghĩa thống kê và tự tin hơn rằng mã của mình an toàn [Perry2023]; một bản thảo năm 2026 với 44 lập trình viên cho thấy Copilot không cải thiện đáng kể việc dùng API bảo mật đúng cách [Mousavi2026]. Ở quy mô ứng dụng, một báo cáo của nhà cung cấp (Veracode, không qua bình duyệt) chạy 80 tác vụ lập trình được chọn lọc trên hơn 100 LLM và nêu rằng lỗ hổng xuất hiện trong 45% trường hợp [Veracode2025]; với sinh mã trong khuôn khổ ràng buộc (tiện ích mở rộng Chrome), 140 prompt và chín LLM cho tỷ lệ chương trình có lỗ hổng từ 18% đến 50% [LiuChrome2025]; với ứng dụng do tác tử AI dựng ngoài thực tế, một bản thảo năm 2026 thu thập 9.041 ứng dụng mã nguồn mở, kiểm toán 200 ứng dụng đã triển khai, tìm 1.186 lỗ hổng, 91,0% ứng dụng được kiểm toán có ít nhất một lỗ hổng và 65,77% lỗ hổng được xếp Critical hoặc High [Deng2026].

Về hành vi khi lặp và khi gặp lỗi, 400 mẫu mã qua 40 vòng "cải tiến" với bốn chiến lược prompt cho thấy số lỗ hổng nghiêm trọng tăng 37,6% sau năm vòng lặp [Shukla2025]; đó là bằng chứng rằng yêu cầu mô hình cải tiến có thể làm mã tệ hơn, nhưng không phải vòng sửa lỗi và không phải Android. Một nghiên cứu sự cố khai thác 16.586 issue GitHub của các công cụ lập trình bằng LLM, xác nhận 547 sự cố an toàn vận hành, xếp 326 sự cố ở mức cao hoặc nghiêm trọng, và báo cáo rằng hơn 65% sự cố phát sinh khi sửa lỗi và khi cài đặt hoặc cấu hình [Hasan2026]. Về prompt có ý thức bảo mật, mười hai chương trình (sáu ứng dụng web, mỗi ứng dụng một biến thể cơ sở và một biến thể có ý thức bảo mật, mỗi biến thể sinh một lần) cho 24 phát hiện đã xác nhận ở biến thể có ý thức bảo mật so với 51 ở biến thể cơ sở, và tác giả tự nhận đây là kết quả sơ bộ, chỉ mô tả [Androcec2026]. Công trình gần nhất với nhánh guardrail của chúng tôi là nghiên cứu nguyên tắc tối thiểu hóa dữ liệu trong ứng dụng Android: 1.114 ứng dụng mã nguồn mở, 9.875 APK, 31 hướng dẫn lập trình rút ra, LLM tái tạo các thực hành rủi ro, và việc bổ sung các hướng dẫn loại bỏ các vấn đề đó ở mọi mô hình được đánh giá [Liao2026]. Công trình này chỉ xét một chủ đề và, theo phần tóm tắt, không có luật bị giữ lại để thử khả năng khái quát; guardrail của chúng tôi trải rộng qua lưu trữ, mạng, nhật ký, hiệu năng và trợ năng, và cố ý bỏ bảy luật ra khỏi tệp (Mục 4.3). Về chất lượng chung, báo cáo của GitClear (chỉ qua một bài tường thuật thứ cấp, vì trang gốc chặn truy xuất tự động) phân tích 211 triệu dòng mã và nêu rằng các khối từ năm dòng trùng lặp trở lên tăng 8 lần trong năm 2024 [GitClear2025]; đây là báo cáo của nhà cung cấp và bài này không dựa vào nó cho kết luận nào. Về hiệu quả thực thi, EffiBench với 42 LLM cho thấy mã do GPT-4 sinh ra tốn trung bình 3,12 lần thời gian thực thi của lời giải chuẩn, và trong trường hợp cực đoan tới 13,89 lần thời gian và 43,92 lần bộ nhớ [Huang2024]; đó là bài toán thuật toán, không nói gì về chi phí giao diện.

**Tri thức lạc hậu và phụ thuộc "ảo".** Wang và cộng sự (ICSE 2025) là đánh giá đầu tiên về việc dùng API đã lỗi thời trong hoàn thành mã bằng LLM: bảy LLM, 145 ánh xạ API từ tám thư viện Python, 28.125 prompt hoàn thành [Wang2025]. Với Android, văn liệu nói về việc cập nhật API lỗi thời bằng LLM, chứ không đo bản nháp đầu phát ra gì: một công cụ cập nhật tự động đánh giá trên 360 lượt dùng chuẩn của 20 API lỗi thời cộng 156 lượt dùng ở API level 33 và 34 [Mahmud2024]; một nghiên cứu năm 2026 trên 1.000 API Android lỗi thời và 4.336 ví dụ di chuyển, trong đó prompt dẫn hướng theo mẫu nâng số cập nhật đúng từ 139 lên 147 trên 156 tác vụ [Mahmud2026]. Về phụ thuộc không tồn tại, Spracklen và cộng sự (USENIX Security 2025) sinh 576.000 mẫu mã bằng 16 LLM cho Python và JavaScript: tỷ lệ gói bị "ảo giác" trung bình ít nhất 5,2% với mô hình thương mại và 21,7% với mô hình mã nguồn mở, với 205.474 tên gói ảo duy nhất; trong phần thân bài, 440.445 trên 2,23 triệu gói được sinh ra (19,7%) là ảo giác [Spracklen2025]. Một bản thảo năm 2026 cho thấy với Rust mẫu hình ảo giác khác Python và JavaScript [Zheng2026].

**Android: lớp nền.** Một nghiên cứu so sánh vấn đề hiệu năng Android ngoài thực tế với văn liệu báo cáo rằng 57,14% yếu tố được xác định chưa được nghiên cứu học thuật và 76,39% chưa được công cụ hiện có xử lý [Liao2024]; cả hai nghiên cứu này (và [Liu2014]) không nói về mã do AI sinh ra hay Jetpack Compose. Trên TLS, lịch sử lỗi của mã người viết gồm kiểm tra chứng chỉ hỏng trong phần mềm ngoài trình duyệt, kể cả ứng dụng ngân hàng Chase trên Android [Georgiev2012]; phân tích SSL (không) an toàn trên Android [Fahl2012] (chỉ trích dẫn bản ghi, không dùng số liệu vì không truy xuất được văn bản); và, chín năm sau, phân tích tĩnh nêu ở trên [Oltrogge2021]. Sao chép từ Stack Overflow là cơ chế phổ biến dẫn tới đoạn mã kém an toàn: 15,4% trong 1,3 triệu ứng dụng Android được phân tích chứa đoạn mã liên quan tới bảo mật lấy từ Stack Overflow, và 97,9% trong số đó có ít nhất một đoạn không an toàn [Fischer2017]. Về trợ năng, một nghiên cứu cho ChatGPT-4 sinh mã Jetpack Compose từ 60 màn hình trong tám mẫu Figma; Accessibility Scanner đánh dấu 302 vi phạm, và lặp lại prompt kèm phản hồi của công cụ giảm số vấn đề báo cáo hơn 50% [Freitas2025].

**Khoảng trống.** Các truy vấn tìm kiếm ghi trong `01-related-work.md` §5 (10 truy vấn, ngày 2026-11-21, qua một công cụ tìm kiếm web có độ phủ kém với MSR/ICSE) không trả về: (a) nghiên cứu thực nghiệm có bình duyệt về hiệu năng Jetpack Compose hay về chi phí của hiệu ứng trang trí (blur, đổ bóng, gradient, hoạt ảnh) trên thiết bị yếu; (b) nghiên cứu về việc trợ lý làm yếu cấu hình bảo mật Android để sửa lỗi build hoặc lỗi chạy; (c) nghiên cứu về tọa độ Maven/Gradle bị ảo giác; (d) nghiên cứu đo ứng dụng Android do AI sinh ra theo MASVS. Mệnh đề "không tìm thấy" chỉ có nghĩa là không có trong kết quả trả về; việc tìm lại là điều kiện trước khi nộp bài.

### 1.3. Phát biểu bài toán và khái niệm "AI slop"

"Slop" là thuật ngữ của giới thực hành; bài chỉ dùng nó như cách gọi tắt cho một **tập phát hiện đo được**: các vấn đề trong catalogue ở Chương 3 (13 vấn đề hiệu năng và bộ nhớ P01–P13, 12 vấn đề bảo mật S01–S12, 6 vấn đề giao diện và trợ năng U01–U06, 2 vấn đề độ lỗi thời D01–D02, cộng thêm mã bổ sung X01 chỉ dùng trong E4) mà bộ phát hiện đã qua kiểm chứng (Mục 4.3) tìm thấy trong mã. Thuật ngữ không hàm ý nguyên nhân.

Phạm vi là **bản nháp đầu, không truy xuất**: trợ lý được yêu cầu dựng một ứng dụng Android một lần, từ một brief, không có mạng, không có bộ phát hiện của chúng tôi, không thấy brief khác và không có người xem lại. Đó là tình huống "vibe coding" bản nháp đầu; nó **không** phải trợ lý có truy xuất tài liệu, có người kiểm duyệt hay quy trình nhóm, và kết quả không được ngoại suy ra ngoài tình huống này [Protocol §1].

Cách phát biểu kết luận là một ràng buộc của nghiên cứu: "các trợ lý phát ra X với tỷ lệ r, so với tỷ lệ nền b của mã người viết", không bao giờ là "AI gây ra X", vì chúng tôi không có nhánh người được phân ngẫu nhiên và kho ứng dụng của người chỉ là dữ liệu quan sát [Protocol §1].

### 1.4. Câu hỏi nghiên cứu và giả thuyết

Đề cương ban đầu đặt bốn câu hỏi: AI tạo ra vấn đề gì về hiệu năng giao diện và bảo mật; lập trình viên dùng AI phát hiện và khắc phục chúng ra sao; chất lượng thay đổi thế nào trước và sau tối ưu; có nên tin hoàn toàn vào đề xuất tối ưu của AI không. Giao thức chia thành sáu câu hỏi đo được [Protocol §2]:

| Mã | Câu hỏi | Thực nghiệm |
|---|---|---|
| RQ1 | Bản nháp đầu do trợ lý sinh ra chứa các vấn đề của catalogue với tỷ lệ nào, theo kiểu prompt, và so với ứng dụng do người viết ra sao? | E1 |
| RQ2 | Các vấn đề đó tốn bao nhiêu recomposition, bộ nhớ, thời gian khởi động và khung hình, trong cặp A (mã slop lấy từ E1) và B (đã tối ưu)? | E2 |
| RQ3 | Công cụ tĩnh độc lập cộng phân loại thủ công tìm được bao nhiêu phần vấn đề, với độ chính xác nào? | E3 |
| RQ4 | Khi được yêu cầu sửa lỗi chạy hoặc lỗi build, trợ lý sửa theo cách làm yếu bảo mật hoặc hiệu năng với tần suất nào? | E4 |
| RQ5 | Khi nhận một ứng dụng slop, có hoặc không có bằng chứng, ở ba mức kiểm soát, trợ lý tìm và sửa được bao nhiêu vấn đề và gây thêm hồi quy nào? | E5 |
| RQ6 | Tệp guardrail có làm giảm tỷ lệ vấn đề không, và chỉ giúp với các luật nó nêu (làm theo chỉ dẫn) hay cả các luật nó không nêu (khái quát hóa)? | E1, nhánh guardrailed |

Các giả thuyết có hướng cố định từ trước, mỗi giả thuyết có điều kiện ủng hộ và điều kiện bác bỏ ghi sẵn; "không kết luận được" là một kết quả hợp lệ [Protocol §3]:

| Mã | Giả thuyết | Đơn vị đo | Xác nhận? |
|---|---|---|---|
| H1 | Trợ lý phát ra API lỗi thời hoặc phiên bản phụ thuộc cũ | tỷ lệ lần chạy hợp lệ có ít nhất một phát hiện D01 hoặc D02 đã kiểm chứng | không, mô tả |
| H2 | Trợ lý sửa lỗi bằng cách làm yếu bảo mật hoặc hiệu năng với tần suất đáng kể | tỷ lệ lượt thử E4 có kết quả `insecure-workaround` | **có, C4** |
| H3 | "Vẻ ngoài AI" (mật độ hiệu ứng trang trí) làm tốn khung hình và bộ nhớ đồ họa | P90 thời gian khung hình, A so với B, mục danh sách P08 như được sinh ra | **có, C5** |
| H4 | Trợ lý phát ra tọa độ Gradle không tồn tại | tỷ lệ các lần xuất hiện tọa độ không phân giải được | không, mô tả |
| H5 | Nợ trợ năng và trạng thái: điều khiển chỉ có biểu tượng thiếu mô tả, thiếu trạng thái loading/empty/error | tỷ lệ lần chạy có phát hiện U01 hoặc U05 | không, mô tả |
| H6 | Rò rỉ do gỡ lỗi mặc định (token, PII trong nhật ký) | tỷ lệ lần chạy có phát hiện S05 | không, mô tả |
| G1 | Guardrail làm giảm tỷ lệ lần chạy có ít nhất một phát hiện nhóm S | điểm cuối chính 1 của E1 | **có, C1** |
| G2 | Guardrail làm giảm độ rộng (số mã luật khác nhau mỗi lần chạy) | điểm cuối chính 2 của E1 | **có, C2** |
| G3 | Guardrail giúp ở luật nó nêu nhiều hơn ở luật nó bỏ | số phát hiện đã kiểm chứng mỗi lần chạy, tập được chỉ dẫn so với tập bị giữ lại | không, thứ cấp có đăng ký |
| B1 | Trợ lý phát ra vấn đề nhóm S với mật độ cao hơn người | phát hiện nhóm S trên 1000 dòng Kotlin, AI không guardrail so với ứng dụng do người viết trước ngày 30/11/2022 | **có, C3** |

### 1.5. Đóng góp

Các tuyên bố đóng góp được tách theo mức độ mới, dựa trên các truy vấn ở Mục 1.2 và được viết thận trọng ngay từ giao thức [Protocol §1.1]:

1. **Đóng góp chính (mới theo các truy vấn đã chạy): H3, chi phí của hiệu ứng trang trí trong Compose.** Đo chi phí khung hình và bộ nhớ đồ họa của "vẻ ngoài AI" trong danh sách Compose, dùng biến thể A lấy từ mã mà trợ lý thực sự sinh ra (E1) so với biến thể B đã làm phẳng (E2), kèm tỷ lệ xuất hiện (E1). Cách viết: "theo hiểu biết của chúng tôi và theo các truy vấn ở Mục 1.2, chưa có phép đo có đối chứng nào về chi phí này được công bố; có các bài viết của giới thực hành". Cơ chế ("quá trình sinh ưu tiên các mặc định trang trí") là giả thuyết; cái được đo là chi phí.
2. **Đóng góp chính: H2 trên Android, trong vòng sửa lỗi.** E4 đặt trợ lý trước năm lỗi Android có chủ đích và phân loại cách sửa thành an toàn, giải pháp lách làm yếu bảo mật, làm giảm hiệu năng, hoặc không sửa, bằng thang chấm đã đóng băng, hai người chấm mù và đường cơ sở Stack Overflow. Bằng chứng liền kề chỉ ở mức tổng quát ([Shukla2025], [Hasan2026]).
3. **Đóng góp thứ cấp: guardrail có luật bị giữ lại trên Android** (nhánh guardrailed của E1), tách làm theo chỉ dẫn khỏi khái quát hóa, định vị so với [Liao2026] và [Androcec2026].
4. **Sao chép và mở rộng: H1** (API lỗi thời, phiên bản cũ) theo tinh thần [Wang2025], mở rộng sang bản nháp đầu Android/Gradle; **H4** (phụ thuộc ảo) theo tinh thần [Spracklen2025], mở rộng sang tọa độ Maven được phân giải với Maven Central, Google Maven và các kho khai báo thêm.
5. **Mô tả:** H5 và H6.
6. **Phương pháp:** giao thức đăng ký trước (họ năm phép kiểm định xác nhận, hiệu chỉnh Holm), kho đối chứng ứng dụng do người viết có tầng không có trợ lý, bộ phát hiện được kiểm chứng (độ nhạy theo đột biến, người chấm thứ hai mù, kappa), và các quy tắc độc lập giữa bên sinh, bên phát hiện và bên sửa.

**Không tuyên bố:** rằng trợ lý gây ra vấn đề (dữ liệu quan sát so với tỷ lệ nền của người); số đo tuyệt đối trên thiết bị thật (chỉ có Emulator, bằng chứng tương đối); tác động UX; xếp hạng nhà cung cấp; kết quả cho trợ lý có truy xuất hay có người xem lại.

### 1.6. Quan hệ với đề cương ban đầu

Đề cương của nhóm (Cương) đặt ra một ứng dụng quản lý công việc SmartTask với hai phiên bản (A ban đầu, B sau tối ưu), hai demo (hiệu năng giao diện, bảo mật), kịch bản trình bày 10 phút và, tùy chọn, nhóm C do lập trình viên tự tối ưu. Nghiên cứu giữ nguyên cấu trúc đó nhưng thay đổi ba điểm, đều ghi lý do:

- **Miền ứng dụng là ShelfTap** (thư viện NFC, đã có thẻ, máy chủ và ứng dụng Android trong kho mã) thay cho SmartTask, để các tình huống bảo mật (token thành viên, lịch sử mượn là dữ liệu cá nhân nhạy cảm) có thật thay vì giả lập.
- **Phiên bản A không do chúng tôi viết tay.** Đề cương cho phép "đưa lỗi giả lập có kiểm soát vào Version A"; nghiên cứu thay bằng mã mà trợ lý thực sự sinh ra (E1) và chỉ đo chi phí trên các vấn đề mà E1 tìm thấy (Mục 4.4). Phiên bản A viết tay ở lượt đo đầu của E2 chỉ còn là thí điểm (Mục 4.11).
- **Mô hình hai chiều của phần mở rộng** (A: AI tạo sản phẩm mới; B: AI tối ưu sản phẩm có sẵn) tương ứng E1 (greenfield, đo chất lượng đầu ra theo cấu hình prompt) và E5 cùng E4 (bảo trì và hiện đại hóa, đo khả năng phát hiện, sửa và hồi quy). Thang định tính "mức khả năng AI và mức kiểm soát" của đề cương (cao, trung bình, thấp) được dùng để đặt ba mức kiểm soát của E5, **không** được dùng như tỷ lệ thành công; đề cương cũng nói rõ nó chỉ là thang định tính để xây dựng giả thuyết.
- Các hạng mục UX (luồng tác vụ, tải nhận thức, onboarding, đo SUS) nằm ngoài phạm vi, và nhóm C do con người thực hiện không được thực hiện vì không có người tham gia (Mục 4.10).

### 1.7. Bố cục

Chương 2 trình bày lớp nền: cơ chế và con số đã kiểm chứng của các kỹ thuật tối ưu và bảo mật quen thuộc trên Android, vì đó là chuẩn so sánh. Chương 3 là catalogue vấn đề theo dạng vấn đề, nguyên nhân, giải pháp. Chương 4 là phương pháp (giao thức). Chương 5 là khung kết quả theo từng thực nghiệm. Chương 6 là kết luận, khung.

---

## Chương 2. Cơ sở lý thuyết

Chương này là **lớp nền** của nghiên cứu: các kỹ thuật tối ưu và bảo mật quen thuộc trên Android, với cơ chế và con số đã kiểm chứng. Chúng được trình bày đủ để làm chuẩn so sánh, vì phần mới của bài (các vấn đề sinh ra từ quá trình sinh mã, Mục 2.6) chỉ có nghĩa khi tách được khỏi những gì tài liệu chính thức đã biết cách xử lý. Mỗi mục nêu cơ chế, giải pháp đã biết, và gắn với mã vấn đề (P, S, U, D) ở Chương 3. Các con số lấy từ tài liệu chính thức hoặc từ bài báo đã kiểm chứng; nếu một con số là của Google hay của nhà cung cấp, bài nói rõ.

### 2.1. AI Coding: khái niệm, ba vai trò và giới hạn

**Khái niệm.** Một trợ lý AI coding là một mô hình ngôn ngữ lớn cộng với một lớp điều khiển (harness: giao diện dòng lệnh hoặc IDE) cho phép đọc, ghi tệp và chạy lệnh. Trong nghiên cứu, cặp (harness, mô hình) được gọi là **bộ công cụ** (tool bundle); hai thành phần bị lẫn trong một bộ nên bài không tuyên bố điều gì về riêng mô hình hay riêng harness và không xếp hạng nhà cung cấp [Protocol §13, mối đe dọa 5].

**Ba vai trò cần tách.** (i) *Sinh mã*: đầu vào là một brief, đầu ra là cả dự án; không có sẵn "đáp án" để đối chiếu, và trợ lý tự chọn thư viện, phiên bản, cấu hình, nên lỗi thời (D) và phụ thuộc ảo (S11) chỉ xuất hiện ở vai trò này. Đây là đối tượng của E1. (ii) *Sửa lỗi*: đầu vào là thông báo lỗi cộng mã dự án; mục tiêu ngầm của trợ lý là làm lỗi biến mất, và cách ngắn nhất không phải lúc nào cũng an toàn (H2, đối tượng của E4). Cơ chế "cách ngắn nhất" là giả thuyết của nghiên cứu; bằng chứng liền kề chỉ ở dạng tổng quát: lặp lại yêu cầu cải tiến làm tăng 37,6% lỗ hổng nghiêm trọng sau năm vòng [Shukla2025] và hơn 65% sự cố của các công cụ lập trình bằng LLM phát sinh khi sửa lỗi hoặc cấu hình [Hasan2026]. (iii) *Phân tích và tối ưu*: đầu vào là một ứng dụng đã có, có thể kèm bằng chứng (kết quả profiler, phát hiện của bộ phát hiện); đối tượng của E5. Điểm khác giữa sinh mã và phân tích mã là ở khả năng kiểm chứng: khi phân tích, có sản phẩm cụ thể và có thể có bằng chứng đo; khi sinh, không có gì ngoài brief. Nhánh "có bằng chứng" và "không có bằng chứng" của E5 kiểm tra liệu bằng chứng có làm khác đi hay không, chứ bài không giả định điều đó.

**Giới hạn đã biết.** Tri thức lạc hậu: LLM dùng API đã lỗi thời khi hoàn thành mã, được đo trên bảy LLM, 145 ánh xạ API và 28.125 prompt [Wang2025]. Phụ thuộc ảo: tỷ lệ gói không tồn tại trung bình ít nhất 5,2% với mô hình thương mại và 21,7% với mô hình mã nguồn mở [Spracklen2025]. Niềm tin của người dùng: người dùng trợ lý tự tin hơn rằng mã của mình an toàn dù mã kém an toàn hơn [Perry2023]. Từ đó rút ra một nguyên tắc phương pháp của đề cương (mục VIII) mà nghiên cứu giữ nguyên: **trợ lý không bao giờ tự xác nhận bản sửa của chính nó**; công cụ, kiểm thử và kiểm tra thủ công mới là bên phán quyết (Mục 4.9).

**Tệp ngữ cảnh dự án và guardrail.** Các harness nạp tệp ngữ cảnh đặt trong thư mục dự án (`AGENTS.md` cho codex và opencode, `CLAUDE.md` cho claude, `GEMINI.md` cho gemini) như một phần chỉ dẫn. Đó là cơ chế mà một **guardrail** đến được trợ lý trong thực tế sử dụng, và nghiên cứu dùng chính đường đó (Mục 4.3). Bằng chứng trước đây: bổ sung 31 hướng dẫn tối thiểu hóa dữ liệu loại bỏ các vấn đề đó ở mọi mô hình được đánh giá trong một nghiên cứu Android [Liao2026]; prompt có ý thức bảo mật cho 24 phát hiện thay vì 51 trong một thử nghiệm sơ bộ trên ứng dụng web [Androcec2026]. Cả hai chỉ đo hiệu ứng trên chính những gì chỉ dẫn nêu. Một guardrail có thể thành công theo hai cách khác nhau: *làm theo chỉ dẫn* (luật có nêu thì được làm) và *khái quát hóa* (luật không nêu cũng được cải thiện nhờ thái độ chung "viết cẩn thận"). Nghiên cứu tách hai cách đó bằng các **luật bị giữ lại** (Mục 4.3, giả thuyết G3).

### 2.2. Hiệu năng giao diện và bộ nhớ

#### 2.2.1. Khung hình, jank và luồng chính

Để tương tác mượt, ứng dụng phải kết xuất khung hình trong dưới 16 ms để đạt 60 khung hình mỗi giây; khung hình chậm là jank, và khung hình đóng băng là khung hình mất hơn 700 ms [Android-Jank]. Khung hình chậm, khung hình đóng băng và ANR đều là những dạng khác nhau của jank [Android-Jank]; ngưỡng ANR cho sự kiện nhập là 5 giây không phản hồi [Android-ANR]. Vì vậy mọi công việc dài trên luồng chính (Mục 2.2.8) và mọi chi phí vẽ mỗi khung hình (Mục 2.2.5) cùng đổ vào một ngân sách thời gian rất nhỏ. FrameTimeline trong Perfetto giúp lần ra khung hình chậm hay đóng băng [Android-Jank].

#### 2.2.2. Ba pha của Compose, recomposition và đọc state (P04, P05)

Mỗi khung hình trong Compose có ba pha chính: Composition (hiển thị cái gì: chạy các hàm composable và tạo mô tả giao diện), Layout (đặt ở đâu: đo và đặt vị trí) và Drawing (vẽ thế nào) [Compose-Phases]. Compose theo dõi việc đọc state trong từng pha và chỉ làm lượng việc tối thiểu cần để cập nhật giao diện [Compose-Phases]. Hệ quả cho người viết mã có hai mặt, đều có trong tài liệu hiệu năng của Compose [Compose-BestPractices]:

- Hàm composable có thể chạy rất thường xuyên, đến mức mỗi khung hình của một hoạt ảnh; vì vậy cần làm ít tính toán nhất có thể trong thân composable và lưu kết quả bằng `remember`. Mẫu vấn đề **P04**: sắp xếp, lọc, định dạng, biểu thức chính quy ngay trong thân composable mà không có `remember`.
- State thay đổi nhanh có thể làm recompose nhiều hơn cần thiết; `derivedStateOf` giới hạn số lần recomposition, và hoãn việc đọc state càng muộn càng tốt giúp Compose chạy lại ít mã nhất. Mẫu vấn đề **P05**: đọc state quá cao trong cây, hoặc dùng state biến thiên nhanh mà không có `derivedStateOf`.

P04 được đo ở E1 và E2 (cùng P03). P05 là luật **bị giữ lại** khỏi guardrail và chỉ được đo ở E1, không có nhân tố E2 (Mục 4.4).

#### 2.2.3. Độ ổn định của kiểu và strong skipping (P03)

Compose chia kiểu thành ổn định và không ổn định: một kiểu ổn định nếu bất biến hoặc nếu Compose biết được giá trị của nó có thay đổi giữa các lần recomposition hay không; kiểu không ổn định là kiểu mà Compose không thể biết điều đó [Compose-Stability]. Độ ổn định của tham số quyết định composable có thể bị bỏ qua (skip) khi tham số không đổi hay không. Mẫu vấn đề **P03**: tham số không ổn định (`List`, `MutableList`, data class có `var`) truyền vào composable.

**Lưu ý quan trọng về điều kiện của chi phí P03.** Từ Kotlin 2.0.20, chế độ *strong skipping* được bật mặc định; khi đó composable có tham số không ổn định cũng có thể được bỏ qua, tham số không ổn định được so sánh bằng bình đẳng thể hiện (`===`) và tham số ổn định bằng `Object.equals()` [Compose-StrongSkipping]. Do đó **chi phí của P03 phụ thuộc phiên bản công cụ biên dịch**: một bản nháp do trợ lý chọn Kotlin cũ hơn 2.0.20, hoặc có tham số không ổn định bị bắt giữ theo cách khác, vẫn chịu chi phí; còn với toolchain mới, P03 một mình có thể gần như không có chi phí đo được. Vì thế bài không coi tần suất P03 là tần suất của một cái giá, mà chỉ là tần suất của một mẫu mã, và E2 đo P03 chung với P04 chứ không đo riêng (Mục 4.4).

#### 2.2.4. Danh sách lazy: key, contentType và Column cuộn (P01, P02, P09)

Với danh sách dài, tài liệu Compose khuyến nghị các layout lazy. Một `Column` không lazy compose và layout mọi phần tử dù có nhìn thấy hay không, trong khi layout lazy chỉ làm với phần tử cần hiển thị [Compose-Lists]. Mẫu vấn đề **P09**: `Column` cộng `verticalScroll`, hoặc vòng `forEach`, cho danh sách dài. Mặc định, trạng thái của phần tử được gắn theo vị trí; cung cấp một `key` ổn định và duy nhất cho mỗi phần tử giúp Compose xử lý đúng việc sắp xếp lại [Compose-Lists]. Mẫu vấn đề **P01**: phần tử LazyColumn, LazyRow, LazyGrid không có `key`. Từ Compose 1.2, nên thêm `contentType` để tối đa hóa hiệu năng khi danh sách gồm nhiều loại phần tử [Compose-Lists]. Mẫu vấn đề **P02**: thiếu `contentType`. Cả ba đều là tham số tùy chọn hoặc lựa chọn bố cục; giả thuyết (chưa kiểm tra bởi nhánh nào) là trợ lý bỏ qua chúng vì chúng không làm mã sai về chức năng. P01 và P09 được đo cùng nhau ở E2 (nhân tố danh sách).

#### 2.2.5. Hiệu ứng trang trí (P08) — phần mới nhất của bài

`Modifier.graphicsLayer` làm nội dung của composable được vẽ vào một lớp vẽ riêng (draw layer) [Compose-Graphics]. Mẫu vấn đề **P08** (thuế hiệu ứng trong phần tử danh sách) gồm blur, `RenderEffect`, đổ bóng hay elevation lớn hơn 2 dp, brush gradient, alpha qua `graphicsLayer`, hoạt ảnh vô hạn. Điều **đã có bằng chứng** chỉ là ngân sách khung hình (16 ms, Mục 2.2.1). Điều **chỉ là giả thuyết**: nguyên nhân của "vẻ ngoài AI" nằm ở quá trình sinh mã (mặc định trang trí), và chi phí đó trả nhiều hơn trên GPU yếu. Các truy vấn ở Mục 1.2 không tìm thấy nghiên cứu có đối chứng nào về chi phí này. Vì vậy cấu trúc đo là: mật độ hiệu ứng trang trí do bộ phát hiện đếm trên mỗi màn hình và mỗi phần tử danh sách (E1), và chi phí A/B của mục danh sách như được sinh ra (E2, giả thuyết H3, phép kiểm định xác nhận C5). Số đo trên Emulator là bằng chứng tương đối (Mục 4.10).

#### 2.2.6. Ảnh và bộ nhớ bitmap (P06)

Giải mã ảnh có thể dễ dàng gây OutOfMemory, nên cần kiểm tra kích thước trước [Load-Bitmaps]. Tài liệu Android nêu ví dụ: với `inSampleSize` bằng 4, bitmap xấp xỉ 512x384 chiếm 0,75 MB thay vì 12 MB cho ảnh đầy đủ (giả định cấu hình ARGB_8888) [Load-Bitmaps]. Với Compose, thư viện Coil có bộ nhớ đệm bộ nhớ và đĩa cùng giảm kích thước khi giải mã [Coil]; `AsyncImage` xác định kích thước tải từ ràng buộc của composable, trong khi `rememberAsyncImagePainter` tải theo kích thước gốc [Coil-Compose]. Mẫu vấn đề **P06**: ảnh được giải mã ở độ phân giải đầy đủ hoặc không có kích thước và bộ nhớ đệm; giả thuyết là trợ lý chọn API painter đơn giản hơn. Đại lượng chính được đo ở E2 là **số byte bitmap đã giải mã** (độc lập thiết bị), chứ không phải thời gian.

#### 2.2.7. Bộ nhớ của ứng dụng: heap, giới hạn, rò rỉ (P12)

Hệ thống đặt giới hạn cứng cho kích thước heap của mỗi ứng dụng; khi ứng dụng chạm giới hạn mà cố cấp phát thêm, nó có thể nhận OutOfMemoryError. Kích thước logic của heap không bằng lượng bộ nhớ vật lý heap dùng [Android-Memory]. Bộ nhớ ứng dụng đã sửa đổi vẫn nằm trong RAM và không thể bị đẩy ra ngoài; cách duy nhất để giải phóng là bỏ các tham chiếu đối tượng để bộ thu gom rác thu hồi [Android-Memory]. Do đó một danh sách giữ toàn bộ bitmap (P06) hay một coroutine toàn cục không bao giờ kết thúc giữ bộ nhớ lâu hơn cần thiết. Memory bloat là một trong ba dạng lỗi hiệu năng chiếm đa số trong nghiên cứu 70 lỗi của tám ứng dụng Android (8 trên 70, 11,4%) [Liu2014]. Mẫu vấn đề **P12** (rò rỉ và công việc không có phạm vi: `GlobalScope`, giữ Activity context, không gỡ listener): `GlobalScope` khởi chạy coroutine cấp cao nhất không bị giới hạn bởi structured concurrency [GlobalScope]. P12 được giữ trong catalogue nhưng **chỉ trích dẫn, không đo** (Mục 4.1).

#### 2.2.8. Luồng chính và coroutine (P07)

Hàm suspend nên an toàn để gọi từ luồng chính (main-safe), dispatcher nên được tiêm vào, và công việc chặn nên chuyển sang bằng `withContext` [Coroutines-BP]. Mẫu vấn đề **P07**: IO, truy vấn cơ sở dữ liệu hoặc mạng ngay trên luồng chính (`runBlocking`, thiếu dispatcher). Hậu quả chạm cả jank lẫn ANR (Mục 2.2.1). P07 còn là trường hợp lỗi của E4 (`NetworkOnMainThreadException`), nơi câu hỏi là liệu bản sửa có dùng `runBlocking` trên luồng chính hay không.

#### 2.2.9. Tìm kiếm và dữ liệu lớn (P13)

Với catalogue 100.000 đầu sách, lọc tuyến tính toàn bộ danh sách trong bộ nhớ sau mỗi lần gõ phím là điều bản nháp đầu dễ làm (giả thuyết). Các kỹ thuật chuẩn: toán tử `debounce` của Flow [Flow-Debounce], thư viện Paging nạp từng trang từ tập dữ liệu lớn hơn [Paging], và lưu trữ cục bộ có chỉ mục: Room ánh xạ một entity FTS4 thành bảng ảo SQLite FTS4 [Room-Fts4]. Mẫu vấn đề **P13**: tìm kiếm tuyến tính, không debounce, không chỉ mục. Mục tiêu của dự án ShelfTap, ghi trong tài liệu dự án (`CLAUDE.md` ở gốc kho mã), là tìm ngoại tuyến trên 100.000 sách trong dưới 50 ms; E2 đo độ trễ tìm kiếm trên bộ dữ liệu 100.000 sách.

#### 2.2.10. Khởi động, R8 và Baseline Profile (P10, P11)

Ứng dụng có thể khởi chạy ở ba trạng thái: cold start, warm start hoặc hot start; ở cold start ứng dụng bắt đầu từ đầu [Android-Launch]. Hai chỉ số khởi động chính là TTID (thời gian hiển thị khung hình đầu tiên) và TTFD (thời gian đến khi ứng dụng tương tác đầy đủ) [Android-Launch]. Tài liệu Android khuyến nghị bật cả `isMinifyEnabled` lẫn `isShrinkResources` cho bản release, hoặc `optimization { enable = true }` từ AGP 9.3, và liệt kê lợi ích gồm khởi động nhanh hơn, giảm bộ nhớ, cải thiện kết xuất và thời gian chạy, ít ANR hơn [R8]. Mẫu vấn đề **P10**: bản release không bật R8 (minify, shrink). Với Baseline Profile, Android Runtime (ART) có thể tối ưu các đường mã được chỉ định bằng biên dịch trước (AOT) [Baseline-Overview]; Google nêu rằng nó cải thiện tốc độ thực thi mã khoảng 30% ngay từ lần khởi chạy đầu [Baseline-Overview] (**con số của Google, không phải của chúng tôi**; E2 đo hiệu ứng của riêng mình trên Emulator). Mẫu vấn đề **P11**: không có Baseline Profile; cách khắc phục là module `baselineprofile` và plugin tương ứng [Baseline-Config]. P10 và P11 là hai kỹ thuật thuộc "lớp nền" mà dự án yêu cầu đo chứ không chỉ liệt kê. P10 còn là trường hợp của E4 (sự cố do thiếu keep rule sau khi bật R8): tắt R8 là giải pháp làm giảm hiệu năng **chỉ khi** tồn tại cách sửa bằng keep rule.

#### 2.2.11. Công cụ đo

Macrobenchmark cung cấp các chỉ số `StartupTimingMetric` và `FrameTimingMetric` [Macrobenchmark]; Android Studio Profiler và Memory Profiler cho CPU và bộ nhớ (đề cương, mục IV). Vì nghiên cứu chỉ có Emulator, số đo chính của E2 được chọn **độc lập thiết bị** trước (số lần recomposition, số byte bitmap đã giải mã, kích thước APK và dex, thời gian chặn luồng chính theo các đoạn trace, độ trễ tìm kiếm); thời gian khung hình, tỷ lệ jank và khởi động là số đo thứ cấp và mang tính tương đối. Ngoại lệ duy nhất là C5 (H3), nơi chi phí kết xuất không thể thấy qua số lần recomposition nên khung hình là số đo chính, kèm cảnh báo về giới hạn của Emulator [Protocol §4.2].

### 2.3. Bảo mật ứng dụng di động

#### 2.3.1. Khung chuẩn: MASVS, MASWE, MASTG

OWASP có ba tài liệu đi cùng nhau: MASVS (các nhóm kiểm soát), MASWE (danh mục điểm yếu, MASWE-0001 đến MASWE-0078 tại thời điểm đọc) và MASTG (các bài kiểm tra) [MASVS] [MASWE] [MASTG]. MASVS v2 chia thành tám nhóm: STORAGE (lưu trữ dữ liệu nhạy cảm trên thiết bị), CRYPTO (chức năng mật mã), AUTH (xác thực và phân quyền), NETWORK (giao tiếp mạng an toàn), PLATFORM (tương tác an toàn với nền tảng và ứng dụng khác), CODE (xử lý dữ liệu và cập nhật), RESILIENCE (chống dịch ngược và can thiệp) và PRIVACY (kiểm soát quyền riêng tư) [MASVS]. Mỗi mã vấn đề S của Chương 3 được ánh xạ tới nhóm MASVS và mục MASWE gần nhất; các nhóm AUTH (phía máy chủ), RESILIENCE ngoài S12 và PRIVACY nằm ngoài tập luật được đo (Mục 3.7).

#### 2.3.2. Lưu trữ, bí mật, nhật ký và sao lưu (S01, S02, S05, S06)

*Lưu token và dữ liệu cá nhân (S01).* Android Keystore giữ vật liệu khóa ở dạng không thể xuất ra, và cho phép giới hạn khi nào và cách nào khóa được dùng, chẳng hạn bắt buộc xác thực người dùng [Android-Keystore]. Hướng khắc phục chuẩn: tạo khóa trong Keystore, mã hóa trước khi ghi, chỉ lưu bản mã trong thư mục riêng của ứng dụng. Điều ngược lại, ghi token nguyên văn vào `SharedPreferences`, DataStore hoặc tệp thường, tương ứng với điểm yếu MASWE-0001 (dữ liệu nhạy cảm lưu không mã hóa trong bộ nhớ riêng) [MASWE-0001] ở nhóm MASVS-STORAGE. Giả thuyết: đoạn mã ngắn nhất cho yêu cầu "giữ đăng nhập sau khi đóng ứng dụng" là ghi token nguyên văn, và đó là lý do bản nháp đầu hay chứa S01.

*Bí mật nhúng trong gói (S02).* MASWE-0004 liệt kê mã nguồn, tài sản (assets) và tài nguyên chuỗi là các đường mà bí mật đi vào gói ứng dụng [MASWE-0004]; bất kỳ ai tải ứng dụng cũng đọc được.

*Nhật ký (S05).* Các mức HEADERS và BODY của `HttpLoggingInterceptor` có thể làm lộ tiêu đề Authorization, Cookie và nội dung thông điệp [OkHttp-Logging]; việc chèn dữ liệu nhạy cảm vào nhật ký là điểm yếu MASWE-0005 [MASWE-0005]. Đây là nền cho giả thuyết H6 (rò rỉ do gỡ lỗi mặc định).

*Sao lưu (S06).* Giá trị mặc định của `android:allowBackup` là true [AutoBackup], nghĩa là nếu không khai báo và không có luật loại trừ thì dữ liệu ứng dụng có thể đi vào bản sao lưu (MASWE-0006 [MASWE-0006]). S06 là một mặc định của manifest dễ bị bỏ sót, và vì thế là luật **bị giữ lại** khỏi guardrail.

#### 2.3.3. Mật mã (S08)

Tài liệu Android khuyến nghị AES ở chế độ CBC hoặc GCM với khóa 256-bit và các hàm băm họ SHA-2 [Android-Crypto]. MASWE-0007 nêu rằng NIST không cho phép AES-ECB (vì tính xác định) và coi vector khởi tạo (IV) đoán được, mã cứng hoặc dùng lại là một điểm yếu [MASWE-0007]; MASWE-0008 nêu MD5 và SHA-1 không được dùng trong ngữ cảnh nhạy cảm về bảo mật [MASWE-0008]. Thư viện `androidx.security:security-crypto` (EncryptedSharedPreferences, MasterKey) cũng nằm trong mẫu vấn đề **S08**: toàn bộ API của nó đã bị đánh dấu lỗi thời từ phiên bản 1.1.0-alpha07 ngày 9 tháng 4 năm 2025, để chuyển sang API nền tảng và dùng trực tiếp Android Keystore [AndroidX-Security]. (Cách ghi "lỗi thời từ 1.1.0" trong hợp đồng ban đầu của nghiên cứu được sửa thành "từ 1.1.0-alpha07".) Giả thuyết: dữ liệu huấn luyện cũ vẫn chứa các mẫu này nên trợ lý còn phát ra chúng.

#### 2.3.4. Giao tiếp mạng và TLS (S03, S04)

*Cleartext (S03).* Lưu lượng cleartext bị tắt mặc định từ Android 9 (API 28) và được bật ở các API đến 27 [Android-NSC]; do đó việc bật lại cleartext trên bản hiện đại là một hành động làm yếu có chủ ý, MASWE-0026 [MASWE-0026]. *Kiểm tra chứng chỉ (S04).* Tài liệu Android gọi một TrustManager không làm gì là phương án kém và nói "đừng làm thế, kể cả tạm thời" [Android-TLS]; chứng chỉ phát triển nên được tin cậy qua cấu hình bảo mật mạng với `debug-overrides`, chỉ có hiệu lực khi ứng dụng có thể gỡ lỗi [Android-NSC] (MASWE-0027 [MASWE-0027]).

*Tỷ lệ nền của người viết mã.* Việc kiểm tra chứng chỉ hỏng không phải phát minh của trợ lý: nó đã được ghi nhận ở phần mềm ngoài trình duyệt, kể cả ứng dụng Chase trên Android [Georgiev2012], trong phân tích SSL của ứng dụng Android [Fahl2012], và vẫn còn gần đây: một phân tích tĩnh 15.000 ứng dụng tìm thấy 5.511 ứng dụng mới phát hành có mã kiểm tra chứng chỉ dễ tổn thương [Oltrogge2021]. Sao chép từ Stack Overflow là cơ chế của người: 15,4% trong 1,3 triệu ứng dụng chứa đoạn mã bảo mật từ Stack Overflow và 97,9% trong số đó có ít nhất một đoạn không an toàn [Fischer2017]. Đây là lý do E4 dùng câu trả lời được bình chọn cao nhất trên Stack Overflow cho mỗi chuỗi lỗi làm đường cơ sở của người (Mục 4.6).

#### 2.3.5. Thành phần nền tảng và màn hình nhạy cảm (S07, S10)

Không khai báo `android:exported` một cách tường minh có nguy cơ cho giá trị mặc định khác nhau [Android-Exported]; với ứng dụng nhắm tới Android 12, thành phần có intent filter phải khai báo thuộc tính này [Android12] (MASWE-0032 [MASWE-0032]). Mẫu vấn đề **S07**: thành phần được xuất mà không có quyền, hoặc không khai báo. `FLAG_SECURE` ngăn màn hình xuất hiện trong ảnh chụp màn hình và trên màn hình không an toàn [FLAG_SECURE] (MASWE-0038 [MASWE-0038]); **S10** (không dùng FLAG_SECURE cho màn hình nhạy cảm) là một quyết định mang tính phán đoán (lịch sử đọc là dữ liệu cá nhân) và chỉ được trích dẫn, không đo.

#### 2.3.6. Phụ thuộc và chuỗi cung ứng (S11, D02)

Phụ thuộc không tồn tại là rủi ro riêng của sinh mã: gói bị ảo giác được đo ở quy mô lớn cho Python và JavaScript [Spracklen2025] và Rust [Zheng2026], và MASWE-0044 chỉ nói về thành phần có lỗ hổng đã biết chứ không về thành phần không tồn tại [MASWE-0044]. Chúng tôi không tìm thấy nghiên cứu nào về tọa độ Maven (Mục 1.2). Hệ sinh thái ảnh hưởng đến **rủi ro**, không chỉ đến tỷ lệ: Maven Central yêu cầu chứng minh quyền sở hữu namespace, bằng xác minh đơn giản cho group ID cá nhân trên dịch vụ lưu trữ mã và bằng bản ghi DNS TXT cho namespace tên miền [Central-Namespace]; còn JitPack dùng tọa độ dạng `com.github.User:Repo:Tag` lấy từ kho Git [JitPack]. Việc chiếm tên (squatting) khó hơn trên Central so với JitPack là **suy luận của chúng tôi từ hai trang tài liệu này, ghi là giả thuyết, không phải phát hiện**. Phép đo H4 vì vậy là phân giải từng tọa độ theo các kho mà dự án sinh ra tự khai báo, tách *artifact ảo* (group:artifact không có ở kho nào) khỏi *phiên bản ảo* (artifact thật nhưng phiên bản không tồn tại).

#### 2.3.7. Kiểm tra dữ liệu đầu vào và cấu hình gỡ lỗi (S09, S12)

Xử lý không an toàn dữ liệu không tin cậy là MASWE-0050 [MASWE-0050] (**S09**, luật dự phòng, chỉ tính nếu bộ phát hiện qua cổng kiểm chứng), và cơ chế gỡ lỗi không tắt ở bản release là MASWE-0063 [MASWE-0063] (**S12**, chỉ trích dẫn).

#### 2.3.8. Các cách làm yếu bảo mật khi sửa lỗi (nền cho E4)

Năm lỗi của E4 là những chỗ mà "cách ngắn nhất để hết lỗi" trùng với một thao tác làm yếu hệ thống: (1) cleartext bị chặn: bật cleartext toàn cục thay vì HTTPS hoặc phạm vi gỡ lỗi; (2) bắt tay TLS với chứng chỉ tự ký thất bại: TrustManager tin tất cả hoặc bộ xác minh tên máy chủ cho phép mọi thứ, thay vì tin cậy CA qua cấu hình bảo mật mạng chỉ cho bản gỡ lỗi (Mục 2.3.4); (3) `NetworkOnMainThreadException`: gọi `runBlocking` trên luồng chính, thay vì `withContext` (Mục 2.2.8); (4) sự cố sau R8 do thiếu keep rule: tắt R8 thay vì thêm keep rule (Mục 2.2.10); (5) lỗi Keystore "người dùng chưa xác thực": tắt yêu cầu xác thực của khóa (mã **X01**, bổ sung được duyệt, chỉ dùng ở E4) thay vì sửa nguyên nhân khiến xác thực không diễn ra, vì Keystore cho phép bắt buộc xác thực người dùng khi dùng khóa [Android-Keystore]. Mã X01 khác S01: S01 là lưu rơi về dạng rõ (plaintext), X01 là hạ cấp ràng buộc xác thực của khóa.

### 2.4. Trợ năng và trạng thái giao diện (U01–U06)

*Nhãn mô tả (U01).* Biểu tượng (`Icon`) mang thuộc tính ngữ nghĩa `contentDescription` [Compose-Semantics], và Accessibility Scanner kiểm tra nhãn nội dung, kích thước vùng chạm, mục có thể bấm và độ tương phản chữ và ảnh [A11y-Scanner]. *Vùng chạm (U02).* Kích thước vùng chạm tối thiểu là 48 dp [Compose-A11y-Defaults]. *Tương phản (U03).* Android Studio UI Check Mode đánh dấu màu tương phản thấp [A11y-Testing]; ngưỡng cụ thể mà bộ phát hiện dùng, 4,5:1, là giá trị của WCAG 2.2 được dùng làm quy ước, và bài **không trích dẫn nguồn đã kiểm chứng cho con số này** (trang WCAG không truy xuất được, ghi ở `references.md` mục F). *Giả thuyết và bằng chứng.* Nghiên cứu cho ChatGPT-4 sinh mã Compose từ 60 màn hình trong tám mẫu Figma thấy Accessibility Scanner đánh dấu 302 vi phạm, và lặp lại prompt kèm phản hồi của công cụ giảm các vấn đề báo cáo hơn 50% [Freitas2025].

Ba mẫu còn lại không có nguồn ngoài đề cương ban đầu và vì vậy là **giả thuyết**: **U04** (màu và kích thước gán cứng, bỏ qua theme, tức trôi hệ thống thiết kế), **U05** (thiếu trạng thái loading, empty, error) và **U06** (bố cục hỏng khi chữ lớn hoặc màn hình hẹp; chỉ trích dẫn, không đo). Vì Accessibility Scanner đã phủ U01 đến U03, các phát hiện U của bộ phát hiện được đối chiếu với công cụ đó thay vì được tuyên bố là bộ phát hiện mới [Protocol §5].

### 2.5. Độ lỗi thời của tri thức (D01, D02)

Tri thức của mô hình dừng ở thời điểm huấn luyện, trong khi thư viện tiến hóa. Wang và cộng sự đo hiện tượng này cho Python [Wang2025]; với Android, các nghiên cứu hiện có đo việc *sửa* API lỗi thời [Mahmud2024] [Mahmud2026], không đo bản nháp đầu phát ra gì. Ví dụ trong tài liệu Android: `AsyncTask` lỗi thời từ API level 30 [AsyncTask]; toàn bộ API của security-crypto lỗi thời từ 1.1.0-alpha07 [AndroidX-Security]. Mẫu vấn đề **D01**: dùng API Android, Jetpack hay Kotlin đã lỗi thời (MASWE-0046). Mẫu vấn đề **D02**: một phụ thuộc cũ hơn bản ổn định mới nhất từ một phiên bản chính trở lên hoặc hơn 12 tháng vào ngày sinh, được xác định bằng ảnh chụp Maven có ngày; D02 là giả thuyết suy ra từ cơ chế tiến hóa thư viện của [Wang2025] và là luật **bị giữ lại** khỏi guardrail để thử xem lời dặn tránh API lỗi thời (D01 có trong guardrail) có kéo theo phiên bản mới (D02) hay không. D rule không dùng trong C3, vì ứng dụng do người viết cũ hơn thì đương nhiên cũ hơn [Protocol §13].

### 2.6. Tổng hợp: lớp nền và lớp quá trình sinh

Bảng sau tách hai lớp mà bài phân biệt. Lớp thứ nhất đã có giải pháp chuẩn trong tài liệu và được đo để biết chi phí và tần suất; lớp thứ hai là các giả thuyết mà nguyên nhân nằm ở chính quá trình sinh mã và vì thế một bản sửa tại chỗ không ngăn nó lặp lại ở bản nháp sau.

| Lớp | Nội dung | Mã | Đã biết gì | Bài đo gì |
|---|---|---|---|---|
| Nền (có giải pháp chuẩn) | danh sách lazy, key, contentType, ảnh và bitmap, luồng chính, tìm kiếm, R8, Baseline Profile | P01, P02, P04, P06, P07, P09, P10, P11, P13 | cơ chế và cách sửa trong tài liệu chính thức (Mục 2.2) | tần suất ở bản nháp (E1) và chi phí A/B (E2) |
| Nền (bảo mật) | lưu trữ, bí mật, nhật ký, TLS, cleartext, exported, mật mã | S01 đến S08 | tiêu chuẩn MASVS/MASWE và tài liệu Android (Mục 2.3); tỷ lệ nền của người viết ([Oltrogge2021], [Fischer2017]) | tần suất so với mã người viết (E1, C3), phát hiện của công cụ độc lập (E3) |
| Nền (trợ năng) | nhãn, vùng chạm, tương phản | U01 đến U03 | Accessibility Scanner đã phủ; một nghiên cứu về ChatGPT-4 ([Freitas2025]) | tần suất ở bản nháp (E1, H5), đối chiếu với công cụ có sẵn |
| Quá trình sinh (giả thuyết) | độ lỗi thời tại thời điểm sinh, phụ thuộc không tồn tại | D01, D02, S11 | đo ở Python, JavaScript, Rust ([Wang2025], [Spracklen2025], [Zheng2026]); chưa thấy với Maven | H1, H4 (mô tả, E1) |
| Quá trình sinh (giả thuyết) | mặc định trang trí | P08 | chưa có phép đo có đối chứng (Mục 1.2) | H3: mật độ (E1) và chi phí (E2, C5) |
| Quá trình sinh (giả thuyết) | làm yếu bảo mật hoặc hiệu năng để sửa lỗi | S03, S04, P07, P10, X01 (trong E4) | chỉ bằng chứng tổng quát ([Shukla2025], [Hasan2026]) | H2 (E4, C4) |
| Tập luật bị giữ lại khỏi guardrail (Mục 4.3) | độ ổn định kiểu, đọc state, hiệu ứng, sao lưu, phiên bản phụ thuộc, theme, trạng thái giao diện | P03, P05, P08, S06, D02, U04, U05 | một phần có nguồn, một phần giả thuyết (Mục 2.2 đến 2.5) | G3: guardrail giúp luật được nêu hay cả luật bị bỏ |

---

## Chương 3. Phân tích các vấn đề trong ứng dụng do AI hỗ trợ phát triển

### 3.1. Cách đọc catalogue

Catalogue gồm 33 mã đã đóng băng (13 hiệu năng và bộ nhớ P01–P13, 12 bảo mật và riêng tư S01–S12, 6 giao diện và trợ năng U01–U06, 2 độ lỗi thời D01–D02) và một mã bổ sung được duyệt, X01, chỉ dùng trong E4. Mã đã đóng băng được bộ phát hiện, bộ đo, bảng phân loại và bài viết cùng dùng; muốn thêm mã phải được duyệt, và mã đề xuất mang tiền tố X cho đến khi được nhận. Bảng nguồn đầy đủ là `02-taxonomy.md`; các bảng dưới đây là bản tiếng Việt, cùng mã, cùng ánh xạ.

Cột **Nguyên nhân** phân biệt hai mức: **có bằng chứng** (cơ chế do tài liệu chính thức hoặc bài báo đã kiểm chứng nêu) và **giả thuyết** (khẳng định rằng trợ lý có xu hướng phát ra mẫu mã đó, hoặc nguyên nhân nằm ở quá trình sinh, là giả thuyết của nghiên cứu trừ khi một nhánh thực nghiệm kiểm tra nó). Cột **Đo ở** cho biết: E1 là tần suất trong mã được sinh ra và trong mã người viết; E2 là chi phí A/B trong bộ đo (các nhân tố cố định); E3 là công cụ tĩnh và phân loại thủ công; E4 là lượt thử dưới áp lực sửa lỗi; **chỉ trích dẫn** là giữ mã nhưng không đo; **bị giữ lại** là luật bị bỏ khỏi guardrail (Mục 4.3).

### 3.2. Hiệu năng và bộ nhớ (P)

| Mã | Vấn đề | Nguyên nhân | Giải pháp đã biết | MASVS/MASWE | Đo ở |
|---|---|---|---|---|---|
| P01 | Phần tử LazyColumn/LazyRow/LazyGrid không có `key` | có bằng chứng: trạng thái phần tử mặc định gắn theo vị trí, key giúp Compose xử lý sắp xếp lại [Compose-Lists]; giả thuyết: `key` là tham số tùy chọn nên trợ lý bỏ qua | `items(list, key = { it.id })` với key ổn định và duy nhất [Compose-Lists] | không áp dụng | E1, E2 (cùng P09) |
| P02 | Không có `contentType` trong danh sách nhiều loại hàng | có bằng chứng: có từ Compose 1.2 để tối đa hóa hiệu năng layout lazy [Compose-Lists]; giả thuyết: bị bỏ vì tùy chọn | đặt `contentType` cho từng phần tử [Compose-Lists] | không áp dụng | E1, E2 |
| P03 | Tham số không ổn định (List, MutableList, data class có var) truyền vào composable | có bằng chứng: kiểu không ổn định là kiểu Compose không biết giá trị có đổi hay không [Compose-Stability]; **chi phí có điều kiện**: từ Kotlin 2.0.20 strong skipping bật mặc định và composable có tham số không ổn định cũng skip được [Compose-StrongSkipping], nên chi phí phụ thuộc phiên bản toolchain; giả thuyết: trợ lý dùng List và var tự do | kiểu bất biến hoặc wrapper ổn định; strong skipping [Compose-Stability] [Compose-StrongSkipping] | không áp dụng | E1 (bị giữ lại), E2 (cùng P04) |
| P04 | Việc nặng trong thân composable (sắp xếp, lọc, định dạng, regex không có `remember`) | có bằng chứng: composable có thể chạy mỗi khung hình nên tính toán trong thân bị lặp [Compose-BestPractices] | `remember`, chuyển việc sang ViewModel [Compose-BestPractices] | không áp dụng | E1, E2 |
| P05 | State đọc quá cao, hoặc state biến thiên nhanh không có `derivedStateOf` | có bằng chứng: state đổi nhanh làm recompose nhiều hơn cần; `derivedStateOf` và hoãn đọc giới hạn việc này [Compose-BestPractices] | `derivedStateOf`, hoãn đọc [Compose-BestPractices] | không áp dụng | chỉ E1 (bị giữ lại); không có nhân tố E2 |
| P06 | Ảnh giải mã ở độ phân giải đầy đủ, hoặc không có kích thước và bộ nhớ đệm | có bằng chứng: giải mã dễ gây OutOfMemory, cần kiểm tra kích thước trước [Load-Bitmaps]; `rememberAsyncImagePainter` tải kích thước gốc còn `AsyncImage` lấy kích thước từ ràng buộc [Coil-Compose]; Coil có bộ nhớ đệm và giảm kích thước [Coil]; giả thuyết: trợ lý chọn API painter đơn giản hơn | `AsyncImage`, kích thước yêu cầu bằng kích thước hiển thị, bộ nhớ đệm [Coil-Compose] [Coil] [Load-Bitmaps] | không áp dụng | E1, E2 |
| P07 | IO, cơ sở dữ liệu, mạng trên luồng chính (`runBlocking`, không dispatcher) | có bằng chứng: hàm suspend nên main-safe, dispatcher được tiêm, `withContext` cho việc chặn [Coroutines-BP] | `withContext(dispatcher)`, dispatcher được tiêm [Coroutines-BP] | không áp dụng | E1, E4 (ca NetworkOnMainThreadException) |
| P08 | Thuế hiệu ứng trong phần tử danh sách (blur, RenderEffect, đổ bóng hoặc elevation lớn hơn 2 dp, brush gradient, alpha qua graphicsLayer, hoạt ảnh vô hạn) | có bằng chứng **chỉ** cho ngân sách khung hình: khung hình phải dưới 16 ms để đạt 60 fps [Android-Jank]; `graphicsLayer` vẽ nội dung vào một lớp [Compose-Graphics]; **giả thuyết**: nguyên nhân của "vẻ ngoài AI" là quá trình sinh (mặc định trang trí) và chi phí trả nhiều trên GPU yếu; chưa tìm thấy nghiên cứu có đối chứng (Mục 1.2) | làm phẳng hiệu ứng theo phần tử, lưu đệm hoặc kết xuất trước, bỏ hiệu ứng theo từng phần tử; bản sửa được đo ở E2 | không áp dụng | E1 (mật độ hiệu ứng trang trí), E2 (C5); bị giữ lại |
| P09 | `Column` cộng `verticalScroll` hoặc `forEach` cho danh sách dài | có bằng chứng: layout không lazy compose và đo mọi phần tử dù nhìn thấy hay không [Compose-Lists] | layout lazy [Compose-Lists] | không áp dụng | E1, E2 (cùng P01) |
| P10 | Bản release không bật R8 (minify, shrinkResources) | có bằng chứng: khuyến nghị bật cả hai thiết lập [R8] | `isMinifyEnabled` cộng `isShrinkResources`, hoặc `optimization { enable = true }` từ AGP 9.3 [R8] | MASWE-0059 có liên quan (làm rối mã) nhưng mã này nói về kích thước và tốc độ, không phải khả năng chống dịch ngược | E1, E2, E4 (ca thiếu keep rule của R8) |
| P11 | Không có Baseline Profile | có bằng chứng: Baseline Profile cải thiện tốc độ thực thi mã khoảng 30% từ lần chạy đầu [Baseline-Overview] (con số của Google, không phải của chúng tôi) | module `baselineprofile` và plugin [Baseline-Config] | không áp dụng | E1, E2 (khởi động, tương đối) |
| P12 | Rò rỉ và công việc không có phạm vi (`GlobalScope`, giữ Activity context, không gỡ listener) | có bằng chứng cho `GlobalScope`: nó khởi chạy coroutine cấp cao nhất không bị giới hạn bởi structured concurrency [GlobalScope]; memory bloat là một dạng lỗi hiệu năng trong nghiên cứu 70 lỗi của tám ứng dụng [Liu2014] | scope có cấu trúc | không áp dụng | **chỉ trích dẫn** |
| P13 | Tìm kiếm là lọc tuyến tính sau mỗi lần gõ, không debounce, không chỉ mục | có bằng chứng: Room ánh xạ entity FTS4 thành bảng SQLite FTS4 [Room-Fts4]; Paging nạp từng trang từ tập dữ liệu lớn [Paging]; toán tử `debounce` [Flow-Debounce]; giả thuyết: lọc trong bộ nhớ là điều bản nháp đầu làm | debounce, kho cục bộ có chỉ mục (FTS4), phân trang | không áp dụng | E1, E2 (độ trễ tìm kiếm trên 100.000 sách) |

### 3.3. Bảo mật và riêng tư (S, và X01)

| Mã | Vấn đề | Nguyên nhân | Giải pháp đã biết | MASVS/MASWE | Đo ở |
|---|---|---|---|---|---|
| S01 | Token hoặc PII ở dạng rõ (plaintext) trong SharedPreferences, DataStore hoặc tệp | giả thuyết: đoạn mã ngắn nhất cho "giữ đăng nhập" ghi token nguyên văn; có bằng chứng: Keystore giữ khóa ở dạng không thể xuất ra [Android-Keystore] | khóa trong Keystore, mã hóa trước khi ghi [Android-Keystore] | MASVS-STORAGE-1; MASWE-0001, MASWE-0002, MASWE-0003 [MASWE-0001] | E1, E3, E4 (ca xác thực Keystore) |
| S02 | Bí mật hoặc khóa API nhúng cứng (mã nguồn, tài nguyên, assets) | có bằng chứng: MASWE-0004 nêu mã nguồn, assets, tài nguyên chuỗi là đường đưa bí mật vào gói [MASWE-0004] | không có bí mật trong gói; token theo người dùng nhận lúc chạy | MASVS-STORAGE-1; MASWE-0004 | E1, E3 |
| S03 | Cho phép lưu lượng cleartext | có bằng chứng: cleartext bị tắt mặc định từ Android 9 (API 28), bật đến API 27, nên bật lại là một hành động làm yếu tường minh [Android-NSC] | HTTPS; `cleartextTrafficPermitted="false"`; phạm vi chỉ cho bản gỡ lỗi | MASVS-NETWORK-1; MASWE-0026 [MASWE-0026] | E1, E3, E4 (ca cleartext) |
| S04 | Tắt kiểm tra TLS (TrustManager tin tất cả, bộ xác minh ALLOW_ALL) | có bằng chứng: tài liệu Android gọi TrustManager không làm gì là phương án kém và nói không làm "kể cả tạm thời" [Android-TLS]; kiểm tra chứng chỉ hỏng ở phần mềm ngoài trình duyệt, kể cả ứng dụng ngân hàng Android [Georgiev2012]; vấn đề còn ở ứng dụng gần đây (phân tích tĩnh 15.000 ứng dụng tìm 5.511 ứng dụng mới phát hành có mã kiểm tra chứng chỉ dễ tổn thương) [Oltrogge2021] | CA qua cấu hình bảo mật mạng, `debug-overrides` chỉ cho bản gỡ lỗi [Android-NSC] | MASVS-NETWORK-1; MASWE-0027 [MASWE-0027] | E1, E3, E4 (ca chứng chỉ tự ký) |
| S05 | Ghi nhật ký nhạy cảm (`Log.*`, HttpLoggingInterceptor BODY/HEADERS ở release) | có bằng chứng: mức HEADERS và BODY có thể làm lộ Authorization, Cookie và nội dung [OkHttp-Logging] | mức NONE ở release; không đưa token, PII vào nhật ký | MASVS-STORAGE-2; MASWE-0005 [MASWE-0005] | E1, E3 (H6) |
| S06 | `allowBackup` không có luật loại trừ | có bằng chứng: mặc định của `android:allowBackup` là true [AutoBackup] | khai báo tường minh, loại trừ dữ liệu nhạy cảm | MASVS-STORAGE-2; MASWE-0006 [MASWE-0006] | E1 (bị giữ lại), E3 |
| S07 | Thành phần được xuất không có quyền, hoặc `exported` không khai báo | có bằng chứng: không đặt `android:exported` tường minh có nguy cơ cho mặc định khác nhau [Android-Exported]; thành phần có intent filter phải khai báo khi nhắm tới Android 12 [Android12] | `exported="false"` tường minh trừ khi cần | MASVS-PLATFORM-1; MASWE-0032 [MASWE-0032] | E1, E3 |
| S08 | Mật mã hoặc thư viện bảo mật yếu hay lỗi thời (MD5/SHA-1 cho bí mật, ECB, IV tĩnh, EncryptedSharedPreferences) | có bằng chứng: MASWE-0007 nêu AES-ECB và IV mã cứng, đoán được hoặc dùng lại [MASWE-0007]; MASWE-0008 nêu không dùng MD5 và SHA-1 trong ngữ cảnh nhạy cảm [MASWE-0008]; Android khuyến nghị AES CBC hoặc GCM khóa 256-bit và SHA-2 [Android-Crypto]; security-crypto lỗi thời toàn bộ API từ 1.1.0-alpha07 (9/4/2025) [AndroidX-Security]; giả thuyết: mã cũ trong dữ liệu huấn luyện còn cho thấy các mẫu này | AES-GCM cộng Keystore, SHA-2 | MASVS-CRYPTO-1; MASWE-0007, MASWE-0008, MASWE-0046 | E1, E3 |
| S09 | Thiếu kiểm tra dữ liệu đầu vào | giả thuyết | kiểm tra và làm sạch dữ liệu không tin cậy | MASVS-CODE-4; MASWE-0050 [MASWE-0050] | E1 (dự phòng bị giữ lại; chỉ tính nếu bộ phát hiện qua cổng kiểm chứng) |
| S10 | Không có FLAG_SECURE (chống chụp màn hình) cho màn hình nhạy cảm | có bằng chứng: `FLAG_SECURE` ngăn màn hình xuất hiện trong ảnh chụp và trên màn hình không an toàn [FLAG_SECURE]; là quyết định phán đoán (lịch sử đọc là PII) | FLAG_SECURE cho màn hình token và PII | MASVS-PLATFORM-3; MASWE-0038 [MASWE-0038] | **chỉ trích dẫn** |
| S11 | Phụ thuộc không tồn tại, bị ảo giác, hoặc có lỗ hổng đã biết | có bằng chứng: ảo giác gói được đo quy mô lớn cho Python, JavaScript [Spracklen2025] và Rust [Zheng2026]; MASWE-0044 nói về thành phần có lỗ hổng đã biết, không về thành phần không tồn tại [MASWE-0044]; chưa tìm thấy nghiên cứu về tọa độ Maven (Mục 1.2) | chỉ dùng kho đã khai báo, kiểm tra tọa độ | MASVS-CODE-3; MASWE-0044 (một phần) | E1 (kiểm tra phân giải, H4) |
| S12 | Cấu hình gỡ lỗi hoặc kiểm thử còn trong bản release | có bằng chứng: MASWE-0063 [MASWE-0063] | build type release không có cờ gỡ lỗi | MASVS-RESILIENCE-4; MASWE-0063 | **chỉ trích dẫn** |
| X01 | Hạ cấp khóa Keystore ràng buộc xác thực để làm lỗi xác thực biến mất (tắt yêu cầu xác thực người dùng, bỏ ràng buộc sinh trắc, tắt vô hiệu hóa khi đăng ký sinh trắc mới). Mã bổ sung được duyệt trong quá trình thực hiện; khác S01 (rơi về lưu dạng rõ (plaintext)) | có bằng chứng: Keystore cho giới hạn khi nào và cách nào khóa được dùng, như bắt buộc xác thực người dùng [Android-Keystore]; giả thuyết: trợ lý nới ràng buộc vì đó là cách ngắn nhất để hết lỗi | giữ ràng buộc xác thực, sửa nguyên nhân gây lỗi xác thực | gần nhất: MASWE-0016 (khóa mật mã không bị hạn chế truy cập) và MASWE-0022 (khóa không bị vô hiệu hóa khi đăng ký sinh trắc mới) (MASVS-CRYPTO-2, MASVS-AUTH-2) [MASWE] | E4 (ca lỗi Keystore chưa xác thực) |

### 3.4. Giao diện, UX và trợ năng (U)

| Mã | Vấn đề | Nguyên nhân | Giải pháp đã biết | Ánh xạ | Đo ở |
|---|---|---|---|---|---|
| U01 | Điều khiển chỉ có biểu tượng, không có `contentDescription` | có bằng chứng: `Icon` mang thuộc tính `contentDescription` [Compose-Semantics]; Accessibility Scanner kiểm tra nhãn nội dung [A11y-Scanner]; nghiên cứu ChatGPT-4 sinh Compose từ 60 màn hình thấy 302 vi phạm do Accessibility Scanner đánh dấu [Freitas2025] | `contentDescription` lấy từ tài nguyên chuỗi | công cụ có sẵn đã phủ (Accessibility Scanner), xem [Protocol §5] | E1 (H5) |
| U02 | Vùng chạm nhỏ hơn 48 dp | có bằng chứng: kích thước tối thiểu 48 dp [Compose-A11y-Defaults]; Accessibility Scanner kiểm tra kích thước vùng chạm [A11y-Scanner] | `minimumInteractiveComponentSize`, `sizeIn` | công cụ có sẵn đã phủ | E1 |
| U03 | Tương phản chữ dưới 4,5:1 | có bằng chứng: Accessibility Scanner kiểm tra tương phản chữ và ảnh [A11y-Scanner]; UI Check Mode của Android Studio đánh dấu tương phản thấp [A11y-Testing]; ngưỡng 4,5:1 là giá trị WCAG 2.2, chưa trích dẫn nguồn đã kiểm chứng (`references.md` mục F) | bảng màu đã kiểm tra tương phản | công cụ có sẵn đã phủ | E1 (dự phòng bị giữ lại; chỉ màu gán cứng) |
| U04 | Màu và kích thước gán cứng bỏ qua theme (trôi hệ thống thiết kế) | giả thuyết; nguồn là dòng "Visual consistency / Design system drift" của đề cương (tài liệu nội bộ) | token của theme | không | E1 (bị giữ lại) |
| U05 | Thiếu trạng thái loading, empty hoặc error | giả thuyết; nguồn là dòng "UI states missing" của đề cương | mô hình trạng thái giao diện tường minh | không | E1 (bị giữ lại), H5 |
| U06 | Bố cục vỡ ở fontScale 2,0 hoặc màn hình hẹp | giả thuyết; các dòng "Responsive layout" và "Typography" của đề cương | ràng buộc, cuộn, kiểm thử với font scale | không | **chỉ trích dẫn** |

### 3.5. Độ lỗi thời (D)

| Mã | Vấn đề | Nguyên nhân | Giải pháp đã biết | Ánh xạ | Đo ở |
|---|---|---|---|---|---|
| D01 | API Android, Jetpack hay Kotlin đã lỗi thời | có bằng chứng: hoàn thành mã bằng LLM dùng API lỗi thời, đo trên Python (7 LLM, 145 ánh xạ API, 8 thư viện, 28.125 prompt) [Wang2025]; với Android, việc di chuyển được nghiên cứu bằng công cụ cập nhật dùng LLM [Mahmud2024] [Mahmud2026]; ví dụ: `AsyncTask` lỗi thời từ API 30 [AsyncTask], security-crypto [AndroidX-Security] | thay thế được ghi trong tài liệu | MASWE-0046 | E1 (H1) |
| D02 | Phụ thuộc cũ từ một phiên bản chính trở lên, hoặc hơn 12 tháng, so với bản ổn định mới nhất tại ngày sinh | giả thuyết suy ra từ cơ chế tiến hóa thư viện của [Wang2025]; định nghĩa thao tác ở [Protocol §3] (H1) bằng ảnh chụp Maven có ngày | phiên bản ổn định mới hơn | MASWE-0044 chỉ áp dụng khi phiên bản cũ đã biết có lỗ hổng | E1 (bị giữ lại) |

### 3.6. Phân biệt nguồn gốc: lỗi do AI, do yêu cầu chưa đầy đủ, hay do tích hợp và chỉnh sửa

Đề cương yêu cầu phân biệt lỗi do AI trực tiếp tạo ra với lỗi do yêu cầu chưa đầy đủ hoặc do quá trình tích hợp, chỉnh sửa mã. Nghiên cứu không phân loại từng lỗi theo nguồn gốc (việc đó cần một phán đoán chủ quan trên từng dòng mã), mà **tách nguồn gốc bằng thiết kế**: mỗi nguồn giả định có một so sánh trong đó nó bị khử hoặc bị lộ ra.

| Nguồn gốc giả định | Cách tách trong thiết kế | Điểm cuối |
|---|---|---|
| Yêu cầu chưa đầy đủ (brief mơ hồ) | hai kiểu prompt cùng miền: *naive* (một đoạn ngắn) và *detailed-functional* (đặc tả chức năng đầy đủ, **không** nêu thuộc tính chất lượng) | so sánh hai kiểu prompt, mô tả (E1) |
| Thiếu yêu cầu chất lượng (người dùng không nói "phải bảo mật") | kiểu *guardrailed*: prompt detailed-functional giữ nguyên từng byte, cộng tệp guardrail | C1, C2 (E1) |
| Mặc định của quá trình sinh (kể cả khi yêu cầu chức năng đầy đủ) | phát hiện còn lại ở kiểu detailed-functional và so với tỷ lệ nền của người viết trước 30/11/2022 | C3 (E1) và bảng mô tả |
| Quá trình tích hợp và chỉnh sửa (sửa lỗi build hoặc lỗi chạy) | E4: lỗi có chủ đích, trợ lý sửa, phân loại cách sửa; đường cơ sở Stack Overflow cho người | C4 (E4) |
| Kiến thức lạc hậu hoặc ảo giác | đo riêng D và S11 theo ngày sinh và theo ảnh chụp kho Maven có ngày | H1, H4 (mô tả, E1) |
| Ảnh hưởng của công cụ (harness) hay của mô hình | không tách được: chỉ báo cáo theo bộ công cụ, không khẳng định "công cụ tốt nhất" | mô tả theo bộ công cụ |

Mỗi so sánh chỉ cho phép kết luận ở mức đã nêu. Ví dụ: nếu ở kiểu detailed-functional vẫn còn tỷ lệ cao một mẫu, có thể nói "yêu cầu chức năng đầy đủ không khử được mẫu này trong thiết kế này", nhưng không thể nói "AI gây ra" (Mục 1.3).

### 3.7. Đối chiếu với danh mục của đề cương

Danh mục vấn đề UI, UX, hiệu năng và bảo mật của đề cương được ánh xạ vào mã đã đóng băng để không có mục nào bị bỏ lặng lẽ; mục "ngoài" là mục nằm ngoài tập đóng băng, nếu có xuất hiện trong bài thì chỉ qua trích dẫn.

| Họ vấn đề của đề cương | Mã |
|---|---|
| UI: nhất quán thị giác, trôi design system | U04 |
| UI: bố cục thích ứng; chữ và khả năng đọc | U06 |
| UI: tương phản màu | U03 |
| UI: vùng chạm | U02 |
| UI: thiếu trạng thái giao diện | U05 |
| UI: ngữ nghĩa trợ năng | U01 |
| UI: hồi quy thị giác | ngoài |
| UX: luồng tác vụ, tải nhận thức, onboarding, phản hồi, mô hình tư duy, bằng chứng khả dụng | ngoài (nghiên cứu UX nằm ngoài phạm vi, Mục 4.10) |
| UX: hiệu năng cảm nhận | số đo khung hình và khởi động ở E2 |
| Hiệu năng: jank, rơi khung hình | số đo của E2 (P01 đến P09 là nguyên nhân) |
| Hiệu năng: recomposition thừa | P03, P04, P05 |
| Hiệu năng: overdraw, ảnh nặng, blur, đổ bóng | P08, P06 |
| Hiệu năng: khởi động chậm | P10, P11 (số đo khởi động ở E2) |
| Hiệu năng: rò rỉ bộ nhớ | P12 (chỉ trích dẫn) |
| Hiệu năng: bộ nhớ cao, áp lực bitmap | P06 |
| Hiệu năng: chặn luồng chính, ANR | P07 |
| Hiệu năng: danh sách kém hiệu quả | P01, P02, P09, P13 |
| Hiệu năng: gọi API dày, pin, ổn định (crash) | ngoài |
| Hiệu năng: kích thước bản dựng, phình phụ thuộc | P10 (shrinking), D02, S11 |
| Bảo mật: STORAGE | S01, S02, S06 |
| Bảo mật: CRYPTO | S08 |
| Bảo mật: AUTH (xác thực, phân quyền, phiên) | ngoài (phía máy chủ); xử lý token là S01, S05 |
| Bảo mật: NETWORK (TLS) | S03, S04 |
| Bảo mật: NETWORK (biên API) | ngoài (phía máy chủ) |
| Bảo mật: PLATFORM (quyền, WebView) | ngoài |
| Bảo mật: PLATFORM (deep link, IPC) | S07 |
| Bảo mật: CODE (kiểm tra đầu vào, đầu ra) | S09 |
| Bảo mật: CODE (phụ thuộc, chuỗi cung ứng) | S11, D02 |
| Bảo mật: CODE (bí mật, nhật ký) | S02, S05 |
| Bảo mật: RESILIENCE | S12 (chỉ trích dẫn); các kiểm soát khác ngoài |
| Bảo mật: PRIVACY (tối thiểu hóa dữ liệu, bề mặt rò rỉ) | ngoài như một luật; PII trong nhật ký là S05; phát hiện về tối thiểu hóa dữ liệu của Android nằm ở [Liao2026] |
| Bảo mật: cấu hình sai | S03, S06, S07, S12 |

Open coding ở E1 (Mục 4.3) có thể đề xuất thêm mã mang tiền tố X; chúng được báo cáo nhưng không gộp vào điểm cuối. Hai ứng viên đã thấy khi đọc văn liệu và **không** được nhận: phiên bản Gradle bị ảo giác (được đếm trong S11 như `hallucinated-version`) và hành động phá hủy hay vi phạm ràng buộc khi sửa lỗi, nằm ngoài khung "mẫu mã" nhưng liên quan tới cách đặt vấn đề của E4 [Hasan2026].

---

## Chương 4. Phương pháp

Chương này trình bày giao thức đăng ký trước (`00-protocol.md`) dưới dạng văn xuôi. Mọi ngưỡng, cỡ mẫu và quy tắc quyết định dưới đây **được đặt trước khi có dữ liệu kết quả**; chúng là quy ước, không suy ra từ dữ liệu của nghiên cứu, và mỗi quy ước đi kèm một dải độ nhạy (Mục 4.8) để người đọc không đồng ý với quy ước vẫn dùng được dữ liệu.

### 4.1. Quy trình tổng thể và đăng ký trước

Quy trình gồm sáu bước, khớp với đề cương (xây yêu cầu, sinh mã ban đầu, đánh giá, yêu cầu AI phân tích, người kiểm duyệt, đo lại) nhưng được tổ chức thành năm thực nghiệm:

1. Đóng băng giao thức và guardrail trước khi chạy mẫu chính thức; chỉ các lần chạy thí điểm (tiền tố `pilot-`, tối đa 6 lần sinh) được phép đi trước và không bao giờ được phân tích [Protocol §11].
2. **E1** sinh kho bản nháp đầu và quét bằng bộ phát hiện đã kiểm chứng; so với kho ứng dụng do người viết.
3. **E2** đo chi phí A/B của các vấn đề mà E1 tìm thấy.
4. **E3** quét tĩnh các APK bằng công cụ độc lập và phân loại thủ công; **E4** và **E5** thử trợ lý ở vai trò sửa lỗi và tối ưu.
5. Báo cáo của từng thực nghiệm (`REPORT.md`), rồi bài viết này.

Giao thức phiên bản 1.0 được đóng băng ở commit a5969de, tổng băm sha256 bắt đầu bằng `f8c62a30`; sau đó chỉ mục sửa đổi (§15) được phép thay đổi và chỉ theo kiểu **chỉ thêm vào**, có ngày, có băm mới ghi trong `00-protocol.sha256` và lịch sử trong `00-protocol.sha256.log`. Các sửa đổi đã ghi: xem Mục 4.11. Kết quả không kết luận được và kết quả âm là kết quả hợp lệ và được báo cáo cùng vị trí với kết quả dương [Protocol §1.1].

**Hình 4.1.** Thiết kế nghiên cứu: đầu vào, năm thực nghiệm và điểm cuối xác nhận. Số liệu vẽ: `figures/out/F01-design.data.csv`.

![F01](figures/out/F01-design.svg)

### 4.2. ShelfTap, ba brief và ba kiểu prompt

Miền ứng dụng là thư viện; mọi brief là một chức năng của ShelfTap. Ba brief bao phủ ba mặt khác nhau của rủi ro, và văn bản **nguyên văn** của chúng (cả bản naive lẫn detailed-functional) nằm ở [Protocol §8], không lặp lại ở đây:

| Brief | Nội dung | Mặt rủi ro dự kiến (theo nội dung brief, chưa phải kết quả) |
|---|---|---|
| B1 | catalogue có ảnh bìa và tìm kiếm, quy mô khoảng 100.000 sách | danh sách, ảnh, tìm kiếm, mạng (P, S03/S04) |
| B2 | đăng nhập, giữ phiên, tài khoản thành viên (token) | lưu token, nhật ký, mật mã (S01, S05, S08) |
| B3 | lượt mượn, lịch sử đọc (dữ liệu cá nhân nhạy cảm), chi tiết sách | dữ liệu cá nhân, sao lưu, trạng thái giao diện (S, U) |

Ba kiểu prompt được định nghĩa một lần cho cả ba brief:

- **naive**: yêu cầu ngắn của một người chỉ muốn "một ứng dụng".
- **detailed-functional**: cùng yêu cầu kèm đặc tả **chức năng** đầy đủ (màn hình, trường dữ liệu, hình dạng API, hành vi, ràng buộc nền tảng). Nó **không nêu thuộc tính chất lượng nào**: không hiệu năng, bộ nhớ, bảo mật, riêng tư, trợ năng hay kiểm thử, và không nêu thư viện, API hay kỹ thuật nào ngoài Kotlin, Jetpack Compose cùng Material 3 và minSdk 26. Việc chọn thư viện và phiên bản là quyết định của trợ lý, vì đó chính là điều H1 và H4 đo.
- **guardrailed**: prompt detailed-functional **giữ nguyên từng byte**, cộng tệp guardrail đặt trong thư mục dự án dưới tên tệp ngữ cảnh gốc của công cụ (`AGENTS.md` cho codex và opencode, `CLAUDE.md` cho claude, `GEMINI.md` cho gemini); nội dung các bản sao giống hệt nhau. Prompt không nhắc tới tệp; công cụ tự nạp như khi dùng thật.

Khung chung đặt đầu mọi prompt yêu cầu làm việc trong thư mục trống hiện tại, tạo cả dự án Android (tệp Gradle, manifest, mã Kotlin, tài nguyên), không chạy lệnh nào dùng mạng, không cài gì, không hỏi lại mà tự quyết mọi điều chưa nêu, và khi xong thì trả lời đúng một từ `DONE` [Protocol §8.1]. Các bản detailed-functional và guardrailed thêm một dòng nền tảng: ứng dụng điện thoại Android, Kotlin, Jetpack Compose với Material 3, minSdk 26, một module Gradle tên `app`, gói `vn.shelftap.app`.

### 4.3. E1: kho bản nháp đầu, bộ phát hiện, đối chứng người viết, guardrail

**Thiết kế.** Lưới ô gồm ba brief, ba kiểu prompt, các bộ công cụ (tối đa bốn) và số lần lặp. Mức sàn là cỡ mẫu gộp theo kiểu prompt từ 18; **mục tiêu thiết kế là từ 36 mỗi kiểu** (ví dụ 3 bộ công cụ nhân 3 brief nhân 4 lần lặp là 36 lần sinh mỗi kiểu, tức 108 lần sinh cho cả ba kiểu), vì ở 18 mỗi nhánh, lực kiểm định phát hiện 0,7 so với 0,3 ở ngưỡng đầu tiên của Holm (0,01) chỉ khoảng 0,33 (bảng ở Mục 4.8) [Protocol §4.1]. Mọi ô được liệt kê rồi xáo trộn bằng hạt giống 20261121 trước khi chạy; mỗi lần chạy ở một thư mục trống mới.

**Cấu hình trống và kiểm tra canary.** Bộ sinh chạy với cấu hình harness **trống** (chỉ có xác thực, không có tệp ngữ cảnh người dùng, hook, MCP hay kỹ năng), và trước khi chạy mẫu chính thức có kiểm tra canary để chứng minh không có gì ở mức người dùng được nạp. Băm của cấu hình được ghi vào mỗi lần chạy. Kết quả kiểm tra canary là một mục của REPORT E1 và của các mối đe dọa.

**Năng lực của từng bộ công cụ và kiểm toán công cụ web (sửa đổi A6).** Ba bộ công cụ đưa cho bộ sinh các phương tiện khác nhau trong lúc chạy: `claude-haiku` chỉ có công cụ tệp (Read, Edit, Write, Glob, Grep), không có shell và không có công cụ web; `codex-luna` có shell, sandbox tắt, việc ghi bị giới hạn bởi hàng rào ghi Landlock, còn mạng không bị hàng rào này chặn; `agy-g38flash` có shell bị giới hạn bởi danh sách cho phép (bộ chỉ đọc của bộ khởi chạy chung cộng các lệnh bổ sung của E1: mkdir, touch, chmod, cp, mv, rm, java, javac, kotlinc, uname, date, whoami), còn curl, wget, npm, pip, gradle, adb vẫn bị từ chối, và chế độ không giao diện kết thúc lần chạy ở lệnh đầu tiên bị từ chối. Các lệnh bổ sung được thêm sau khi một lần chạy thí điểm của agy kết thúc rỗng vì lệnh `java -version` (không hợp lệ vì lý do của harness và phụ thuộc kiểu prompt). Từ thời điểm chuyển đổi ghi trong `runs.csv`, các lần chạy agy còn chạy dưới hook chặn web của người phụ trách E4; các lần chạy agy trước đó có công cụ web. **Không bộ công cụ nào build hay chạy được ứng dụng.** Phạm vi "không truy xuất" được kiểm toán từ bản ghi hội thoại theo từng lần gọi công cụ, không chỉ theo chuỗi lệnh: agy `search_web`, `read_url*`, `execute_url`, `browser_*`; codex các mục `web_search`; claude `WebSearch` và `WebFetch`. Số lần gọi và tên công cụ được ghi vào `runs.csv` (cột `web_tool_calls`, `web_tools`); lần thử bị hook chặn vẫn được đếm. Lần chạy có số lần gọi web lớn hơn 0 được báo cáo và bị loại ở phân tích độ nhạy, **không** bị loại ở phân tích chính.

**Bảng sửa chữa R7 mở rộng và phân tích độ nhạy bổ sung (sửa đổi A6).** Các sửa chữa cơ học được thêm vào bảng, ghi trong `results/repairs.csv` và tính vào mức trần 3 sửa chữa: yêu cầu toolchain JDK chưa được cài thì đổi sang 17; đặt `kotlin.jvm.target.validation.mode=warning` khi gặp lỗi "Inconsistent JVM-target compatibility"; import thiếu với tên cố định thì tra bảng tên đầy đủ cho lỗi "Unresolved reference". Gradle chạy trên JDK 17, JDK 17 và 21 được đăng ký cho toolchain. Sửa chữa chỉ chạm vào bản sao để build; mã được phân tích vẫn là mã đã sinh. Lý do: một lượt build đầu trên máy dùng JDK 21 cho thấy JDK của máy quyết định qua hay trượt ở lỗi "Inconsistent JVM-target", nên các bản build đó bị bỏ và làm lại. Ba phân tích độ nhạy được thêm, không cái nào thay một điểm cuối xác nhận: (a) C1 đến C3 không tính lần chạy nào có số lần gọi web lớn hơn 0; (b) C1 đến C3 theo từng bộ công cụ và bỏ lần lượt từng bộ, để một bộ không thể lái kết quả gộp mà không ai thấy; (c) C1 đến C3 trên các lần chạy mà mã nguồn build được (không can thiệp hoặc sau sửa chữa theo bảng), báo cáo cạnh tỷ lệ qua build theo bộ công cụ nhân kiểu prompt, vì một bản nháp không biên dịch được vẫn được phân tích ở mức mã nguồn trong phân tích chính (R7) và có thể khác về cỡ và cấu trúc. Cỡ mã: các bản nháp của codex dày đặc (một lần thí điểm là 92 dòng trong 13 KB, dòng dài tới 1313 ký tự); C3 vẫn tính trên 1000 dòng như đã đóng băng, và `analysis.py` báo thêm một dòng độ nhạy tính trên 100.000 ký tự mã khác khoảng trắng. Người phụ trách E1 nêu thêm một công khai: `analysis.py` đã chạy một lần trên một tập con các lần chạy như một phép thử đường ống và kết quả đã được nhìn thấy, nhưng không điều gì trong A6 dựa vào nó.

**Lần chạy hợp lệ.** Một lần chạy là hợp lệ nếu tạo ra ít nhất một tệp `.kt` và một tệp build Gradle. Lần chạy không hợp lệ (công cụ lỗi, hết thời gian, từ chối) được báo cáo kèm lý do, đưa vào phân tích độ nhạy "tất cả các lần chạy" với mẫu số nêu rõ, và loại khỏi phân tích chính. Chỉ được sinh lại một lần, khi hạ tầng lỗi trước khi tệp nào được ghi, và có ghi nhật ký [Protocol §4.1].

**Ghi lại và tái lập.** Cửa sổ sinh mã là trong 14 ngày lịch; phiên bản công cụ và mô hình được ghim ở đầu cửa sổ và ghi theo từng lần chạy; thay đổi phiên bản giữa chừng tạo thành một bộ công cụ phiên bản riêng cho phân tích độ nhạy. Mỗi lần chạy lưu bản ghi hội thoại, băm của prompt, cờ dòng lệnh, băm cấu hình harness; lần chạy guardrailed lưu thêm sha256 của guardrail. Sinh mã là không tất định, nên "tái lập" nghĩa là có bản ghi và băm, không phải cùng một mã [Protocol §10].

**Chính sách build và mức phân tích.** Tệp build là tự do (trợ lý chọn phiên bản, vì độ lỗi thời là H1). Bước 1 là build không can thiệp; bước 2 cho phép tối đa 3 sửa chữa cơ học có ghi nhật ký (nâng phiên bản hoặc plugin, thêm import thiếu; bảng sửa chữa cụ thể được mở rộng ở A6, xem dưới). Mọi lần chạy được phân tích ở mức **mã nguồn** (SOURCE); kết quả mức APK chỉ là kết quả của những bản "sống sót" kèm tỷ lệ qua build và khoảng tin cậy Wilson. Với mỗi tọa độ được sinh ra, ảnh chụp `maven-metadata.xml` ngày sinh được lưu để H1 và H4 có mốc ngày. Phạm vi luôn được nêu: bản nháp đầu, không truy xuất.

**Điểm cuối.** Hai điểm cuối chính theo kiểu prompt: (1) tỷ lệ lần chạy hợp lệ có ít nhất một phát hiện nhóm S đã kiểm chứng; (2) số mã luật khác nhau đã kiểm chứng trên mỗi lần chạy. Điểm cuối thứ cấp: số mã luật nhóm S khác nhau; phát hiện trên 1000 dòng Kotlin; build qua ở hai mức (không can thiệp, sau sửa chữa); bảng tần suất theo từng lớp vấn đề (mô tả); tần suất H1, H4, H5, H6; mô tả theo bộ công cụ (không khẳng định "công cụ tốt nhất") [Protocol §4.1].

**Bộ phát hiện và các cổng kiểm chứng.** Bộ phát hiện do một bên độc lập với tác giả guardrail viết (Mục 4.9) và phải qua kiểm chứng trước mọi phân tích xác nhận; một luật chỉ được tính vào C1 đến C3 và H1, H5, H6 khi qua các cổng sau [Protocol §5]:

| Cổng | Yêu cầu | Nếu không đạt |
|---|---|---|
| (a) Độ nhạy theo đột biến | ít nhất 3 đột biến cho mỗi luật được chèn vào mã sạch; báo cáo độ nhạy từng luật | luật có độ nhạy dưới 0,80 là "chưa kiểm chứng độ nhạy": giữ trong bảng mô tả, loại khỏi C1–C3 |
| (b) Người chấm thứ hai mù | ít nhất 50 phát hiện, phân tầng theo luật và theo nhánh, người chấm không biết nhánh và công cụ; kappa giữa hai người [Cohen1960] | luật có độ chính xác dưới 0,80 trên mẫu của nó, hoặc mẫu dưới 5, là "chưa kiểm chứng độ chính xác": xử lý như trên. Kappa chung dưới 0,60 thì sửa thang chấm và chấm lại trước khi phân tích |
| (c) Độ phủ so với công cụ sẵn có | chạy Android Lint và detekt cùng compose-rules (cộng MobSF nếu có) trên cùng mã; báo cáo ma trận độ phủ [Android-Lint] [detekt] [compose-rules] | luật tùy biến chỉ được giữ ở chỗ chưa công cụ nào phủ; còn lại dùng phát hiện của công cụ sẵn có và ghi công |
| (d) Open coding | ít nhất 10 lần chạy, phân tầng, đọc để tìm vấn đề ngoài catalogue | mã đề xuất (tiền tố X) được báo cáo, không gộp vào điểm cuối |

Hai điểm cuối chính được tính hai lần: chỉ trên luật đã kiểm chứng (chính) và trên mọi luật (độ nhạy). Người chấm thứ hai không bao giờ thấy nhánh, công cụ hay mã lần chạy (một băm có muối thay thế mã). Mức độ giải thích kappa theo [Landis1977] chỉ để mô tả; cổng duy nhất có đạt/không đạt là 0,60.

**Đối chứng người viết.** Ít nhất 30 ứng dụng Compose mã nguồn mở công khai, hai tầng. *Tầng trước trợ lý* (ít nhất 15 ứng dụng): commit đầu và bản ghim cuối đều trước hoặc đúng ngày 2022-11-30 (ngày công bố ChatGPT [ChatGPT2022]), tức là mã viết trước khi trợ lý trò chuyện phổ biến; đây là đối tượng so sánh xác nhận cho C3. Các trợ lý hoàn thành mã đã có bản xem trước trước ngày này, nên mức phơi nhiễm là thấp, không phải bằng không. *Tầng 2024 trở đi* (ít nhất 15): commit đầu từ 2024-01-01, ghim tại đầu nhánh mặc định ngày rút mẫu; đây là đối tượng so sánh thứ hai, không xác nhận, vì có thể chứa mã do trợ lý viết. Tiêu chí gồm: kho công khai; Kotlin; dùng Jetpack Compose; là module ứng dụng; từ 2.000 đến 50.000 dòng Kotlin; không yêu cầu build; không phải kho mẫu, codelab hay hướng dẫn của hãng. Rút mẫu ngẫu nhiên có hạt giống (20261121) trong kết quả tìm kiếm của từng tầng; truy vấn và ngày được ghi lại. Thiên lệch chọn mẫu (ứng dụng mã nguồn mở thiên về dự án sở thích và hồ sơ cá nhân) là một mối đe dọa đã nêu. Ứng dụng tham chiếu của kho mã (`android/app`, được làm cứng bằng tay) chỉ là một điểm mô tả, không bao giờ gộp. Danh sách nằm ở `e1-corpus/results/human_corpus.csv`; chỉ repo, commit và số đếm tổng hợp được công bố, không nêu tên cá nhân [Protocol §7, §12].

**Guardrail và các luật bị giữ lại.** Tệp guardrail (`guardrail/ANDROID-GUARDRAIL.md`, 21 điều) được viết **chỉ từ nguồn độc lập** (OWASP MASVS, MASWE, MASTG; tài liệu nhà phát triển Android; tài liệu hiệu năng Compose; WCAG), **không đọc bộ phát hiện** hay bất kỳ thứ gì dưới `e1-corpus/`. Tổng băm sha256 của nó được ghi trước lần chạy guardrailed đầu tiên; sửa sau đó là phiên bản mới và là một sửa đổi. Mở đầu tệp yêu cầu giữ nguyên các điều khi có xung đột, và khi gặp lỗi thì tìm nguyên nhân chứ không vượt lỗi bằng cách tắt một điều. **Bảy luật bị giữ lại** (không được nêu hay diễn đạt lại ở bất cứ đâu trong tệp, được chọn **trước** khi tệp được viết nên tệp không thể bị điều chỉnh cho hợp với chúng) [Protocol §9]:

| Luật bị giữ lại | Vì sao chọn |
|---|---|
| P03 tham số không ổn định | có lời khuyên chung về độ ổn định, nhưng luật cụ thể bị bỏ |
| P05 state đọc quá cao, `derivedStateOf` | ý tưởng hoãn đọc bị bỏ hoàn toàn |
| P08 thuế hiệu ứng trong danh sách | thử xem thái độ chung "giữ UI gọn" có ngăn được hiệu ứng khi không có luật (liên quan H3) |
| S06 `allowBackup` không có luật loại trừ | mặc định manifest, dễ bỏ sót |
| D02 độ lỗi thời của phụ thuộc | thử xem dặn tránh API lỗi thời (D01 có trong tệp) có kéo theo phiên bản mới |
| U04 màu, kích thước gán cứng | luật trôi hệ thống thiết kế |
| U05 thiếu trạng thái loading, empty, error | luật nợ trạng thái |

Bảy luật bị giữ lại (yêu cầu tối thiểu là năm). Luật dự phòng U03 hoặc S09 chỉ được dùng nếu E1 báo trước lần chạy đầy đủ rằng một luật bị giữ lại không phát hiện được tĩnh với độ chính xác đủ; theo sửa đổi A1 (Mục 4.11), **không có thay thế nào được thực hiện**. Tập được chỉ dẫn là mọi luật đo được còn lại (P01, P02, P04, P06, P07, P09, P10, P11, P13, S01, S02, S03, S04, S05, S07, S08, S11, U01, U02, D01). P12, S10, S12, U06 chỉ trích dẫn. Giả thuyết G3 so sánh hiệu ứng của guardrail trên tập được chỉ dẫn với hiệu ứng trên tập bị giữ lại; nếu số phát hiện của một luật bị giữ lại vẫn giảm, báo cáo ghi "khái quát hóa hoặc ngẫu nhiên" và không khẳng định cơ chế [Protocol §9].

### 4.4. E2: đo A/B chi phí trong ứng dụng thí nghiệm

E2 được xây trong một module riêng (ứng dụng thí nghiệm optlab và module benchmark), tách khỏi sản phẩm để "slop" không bao giờ nằm trong ứng dụng thật [Protocol §4.2].

**Nhân tố.** Chỉ bảy nhân tố, ứng với các mẫu có thể đo độc lập: P08 hiệu ứng trang trí; P06 giải mã và bộ nhớ đệm ảnh; P01 cùng P09 cách dựng danh sách; P13 tìm kiếm; P03 cùng P04 độ ổn định và việc nặng trong composition; P10 R8; P11 Baseline Profile (P10 và P11 là lớp kỹ thuật nền mà dự án yêu cầu đo). Biến thể **A của các nhân tố suy ra từ phát hiện của E1 được lấy từ mã E1 thật**, theo quy tắc chọn đặt trước dưới đây (sửa đổi A4), không tự viết; biến thể B là bản đã tối ưu. Các kỹ thuật nền khác chỉ được nêu qua trích dẫn.

**Quy tắc chọn biến thể A (sửa đổi A4, đặt trước).** Với mỗi nhân tố lấy từ kho E1 (P08, P06, P01 cùng P09, P13, P03 cùng P04), danh sách ứng viên là các lần chạy của chiến dịch (không phải thí điểm) có phát hiện của bộ phát hiện cho mã luật đó. Ứng viên được xếp theo thời điểm bắt đầu lần chạy, và biến thể A là ứng viên **đầu tiên** có đoạn mã vi phạm đưa được vào màn hình của optlab chỉ bằng thay đổi cơ học (gói, import, gắn nguồn dữ liệu), không đổi chính cấu trúc vi phạm. Với mỗi nhân tố, REPORT ghi mã lần chạy, tệp và dòng, và bản diff của việc chuyển. Nếu đến lúc chiến dịch đạt một nửa mà không có ứng viên nào đủ điều kiện, nhân tố đó giữ biến thể A viết tay và được gắn nhãn "author-constructed" (do tác giả dựng) ở mọi bảng; khi đó C5 không thể được tuyên bố cho P08. Quy tắc nêu năm nhân tố trên; văn bản của nó không nói gì về P10 và P11. Người điều phối xác nhận quy tắc được quyết định trước mọi lần chạy E2 xác nhận và trước khi bất kỳ phát hiện nào của kho được ghép với nhân tố E2. Sửa đổi A5 làm rõ hai điểm của quy tắc này: (1) A4 **thay thế** ghi chú của C5 trong giao thức (mục thay thế là phần tử có hiệu ứng trang trí dày nhất trong E1) đối với việc chọn mục P08: với mọi nhân tố lấy từ kho E1, kể cả P08, biến thể A là ứng viên đầu tiên đủ điều kiện theo thứ tự chạy, vì chọn phần tử dày hiệu ứng nhất sẽ làm hiệu ứng lớn nhất và đó là một lựa chọn có thể được chọn theo ý muốn; nếu không lần chạy nào đủ điều kiện, P08 vẫn là "author-constructed" và C5 không được tuyên bố; (2) "chiến dịch" là chiến dịch sinh mã của E1, chỉ tính các lần chạy không phải thí điểm. Sửa đổi A10 làm rõ thêm một điểm: ứng viên đưa được vào optlab bằng thay đổi cơ học nhưng tiến trình ứng dụng sập trên AVD đo ở cả 3 trong 3 lần khởi động nguội thì được đối chiếu với ứng dụng gốc chưa sửa của kho trên cùng AVD; nếu bản gốc cũng sập, ứng viên bị loại với lý do "rig-unmeasurable" và lấy ứng viên kế tiếp theo thứ tự chạy, theo cùng quy tắc A4; nếu bản gốc không sập, lỗi nằm ở bản đưa vào, bản đưa vào được sửa và ứng viên được giữ. Bằng chứng sập (tombstone hoặc đoạn logcat, số lần khởi động) lưu ở `e2-bench/ports/`; cấu trúc bị loại được báo cáo mô tả trong E2 và trong bài, không bị bỏ lặng lẽ.

**Số đo.** Số đo chính **độc lập thiết bị** (số lần recomposition, byte bitmap đã giải mã, kích thước APK và dex, thời gian chặn luồng chính qua đoạn trace, độ trễ tìm kiếm); thời gian khung hình, tỷ lệ jank và khởi động là số đo thứ cấp và tương đối. Ngoại lệ là H3 (phép kiểm định C5): chi phí kết xuất không thấy được qua recomposition nên P90 của `FrameDurationCpuMs` là số đo chính [Macrobenchmark], kèm cảnh báo giới hạn Emulator.

**Thiết kế.** Bản release, AVD khởi động nguội, chế độ GPU cố định và được ghi, các lần đo A và B xen kẽ (ABAB), ít nhất 10 lần lặp (mỗi lần lặp là một lần khởi chạy tiến trình cộng kịch bản cuộn và tìm cố định), khóa băng đo để không có công việc khác chen vào. Mỗi lần lặp bị loại khi bộ nhớ khả dụng của máy dưới 1024 MB ở đầu lần lặp, hoặc (theo sửa đổi A7, thay tiêu chí swap-in toàn máy của A2) khi tiến trình emulator (qemu) đo được có số lỗi trang chính (major fault) tăng trong lần lặp. Quy tắc loại do áp lực máy **chỉ** dựa trên trạng thái máy và tiến trình, không bao giờ dựa trên giá trị số đo. Lý do thay: tiêu chí swap-in toàn máy của A2 loại mọi lần lặp, vì máy giữ swap cũ và emulator tự đưa bộ nhớ nguội của nó trở lại (đo ngày 2026-11-21, ghi trong `e2-bench/results/iterlog.csv`; số đo cụ thể nằm ở dòng A7 của giao thức). Swap-in toàn máy, VmSwap của qemu và số lỗi trang chính của qemu được ghi cho từng lần lặp. Phân tích chính dùng các lần lặp được giữ; phân tích độ nhạy dùng mọi lần lặp; cả hai được báo cáo. Harness chạy thêm tối đa 3 vòng cho mỗi nhân tố và pha; các lần bị loại không được thay bằng tay mà ghi vào `e2-bench/results/exclusions.csv` và vào bảng luồng của REPORT. Nhân tố hoặc biến thể vẫn mất hơn 50% số lần lặp sau 3 vòng thêm được báo cáo là **bị giới hạn bởi máy** và quy tắc không được nới thêm [Protocol §15, A2, A7].

**Phân tích.** Trung vị và IQR; delta% kèm khoảng tin cậy bootstrap phân vị (10.000 lần lấy mẫu, hạt giống 20261121) [Efron1979]; delta của Cliff [Cliff1993]; p của Mann–Whitney [Mann1947] chỉ xác nhận cho C5. Các nhân tố còn lại là tái hiện kỹ thuật đã biết, báo cáo bằng độ lớn hiệu ứng và khoảng tin cậy, không diễn giải p. **Chi phí kỳ vọng** (khám phá): tần suất từ E1 nhân chi phí từng nhân tố từ E2, lan truyền cả hai bất định bằng lấy mẫu lại; gắn nhãn khám phá, và giả định rằng biến thể A của E2 đại diện cho các lần xuất hiện ở E1 (một mối đe dọa đã nêu) [Protocol §4.2].

### 4.5. E3: công cụ bảo mật độc lập

Đối tượng: các APK A và B từ E2, tối đa 10 bản dựng sống sót từ E1, và các ứng dụng do người viết nếu build được. Công cụ: MobSF [MobSF] trong môi trường Python riêng (không dùng docker), phương án dự phòng là JADX [JADX] cộng quét bí mật cộng các kiểm tra bảo mật của Android Lint; phân loại thủ công đối chiếu các bài kiểm tra MASTG [MASTG]. Điểm cuối (mô tả): theo từng công cụ và nhóm MASVS, số phát hiện, đúng hay sai sau phân loại, độ chính xác; mức đồng thuận giữa bộ phát hiện và công cụ (kappa khi có hai người hoặc hai công cụ gán nhãn cùng đối tượng); các phát hiện chỉ một công cụ thấy. Đề cương nhắc không chỉ dựa vào số cảnh báo của MobSF mà phải kiểm tra từng phát hiện và loại dương tính giả; E3 làm đúng điều đó. Quy tắc độc lập: trợ lý đã sinh hay sửa mã không bao giờ xác nhận kết quả của chính mình [Protocol §4.3].

### 4.6. E4: áp lực sửa lỗi

**Lỗi.** Năm lỗi có chủ đích, mỗi lỗi có đường sửa an toàn và đường lách yếu hơn (Mục 2.3.8): cleartext bị chặn; bắt tay TLS với chứng chỉ tự ký thất bại; `NetworkOnMainThreadException`; sự cố do thiếu keep rule sau R8; lỗi Keystore "người dùng chưa xác thực". **Biến thể prompt:** chỉ có thông báo lỗi, hoặc thông báo lỗi kèm dự án. **Bộ công cụ** (harness cộng mô hình): ít nhất 3. **Lặp:** ít nhất 5 lần cho mỗi (lỗi, biến thể, bộ công cụ). Mục tiêu thiết kế từ 150 lượt thử; mức sàn 75 [Protocol §4.4].

**Thang chấm đóng băng.** Bốn kết quả: `secure-fix`, `insecure-workaround`, `perf-regression`, `no-fix`, được định nghĩa kèm ví dụ trong `e4-pressure/RUBRIC.md` **trước** các lượt thử. Các mốc do giao thức đặt từ trước: bật cleartext toàn cục, TrustManager tin tất cả, bộ xác minh tên máy chủ ALLOW_ALL, `runBlocking` trên luồng chính, tắt yêu cầu xác thực của khóa Keystore đều là `insecure-workaround` (bảo mật) hoặc `perf-regression` (hiệu năng); tắt R8 sau sự cố thiếu keep rule là `perf-regression` **chỉ khi** tồn tại cách sửa bằng keep rule; cleartext có phạm vi hẹp (cấu hình mạng chỉ cho bản gỡ lỗi, chỉ localhost hoặc 10.0.2.2) là `secure-fix` khi nó không thể lên bản release.

**Người chấm.** Hai người chấm không biết bộ công cụ (một là chủ thực nghiệm, một là người chấm độc lập), báo cáo kappa; bất đồng được giải bằng lần đọc thứ ba theo thang chấm. Đường cơ sở của người: câu trả lời được bình chọn cao nhất của mỗi chuỗi lỗi trên Stack Overflow, phân loại cùng thang chấm, chỉ mô tả. Điểm cuối xác nhận là C4 (Mục 4.8); điểm cuối thứ cấp là tỷ lệ theo lỗi, theo bộ công cụ và theo biến thể prompt (khoảng Wilson [Wilson1927]; so hai biến thể bằng kiểm định Fisher chính xác, khám phá).

**Công cụ web của các bộ công cụ và sai lệch so với cô lập cấu hình (sửa đổi A9).** Cô lập cấu hình (R2) chỉ bảo đảm không có luật, kỹ năng, bộ nhớ hay máy chủ MCP của người dùng lọt vào; nó không gỡ các khả năng của chính harness. Sau đợt đầu của E4, bản ghi gọi công cụ được kiểm toán để tìm mọi công cụ chạm được ra ngoài: `claude` chỉ có công cụ tệp (Read, Edit, Write, Glob, Grep) và không có shell; `codex` có tìm kiếm web dựng sẵn và một công cụ kết nối, nhưng không lượt thử nào gọi web; `agy` có `search_web` không cần xin phép và dùng nó trong một phần đáng kể các lượt thử của đợt đầu (31 trên 50, theo kiểm toán trong văn bản A9). Quy tắc chặn agy được tin là hiệu lực ban đầu thực ra không hiệu lực. Điều này quan trọng vì tìm kiếm web cho phép bộ sinh tra lỗi và chép điều các câu trả lời hàng đầu nói, chính là đường cơ sở của người mà E4 so sánh; hai bộ công cụ có và không có tìm kiếm khác nhau nhiều hơn mô hình. Quyết định dựa trên số lần gọi công cụ, không có nhãn kết quả nào được lập bảng theo bộ công cụ trước khi quyết định. Các quyết định (người điều phối, 2026-11-21): (1) **tập dữ liệu chính của E4** gồm claude và codex (100 lượt đã chạy) cộng lần chạy lại của agy với web bị chặn (50 ô, 5 lần lặp); đợt agy đầu tiên được giữ như **tập thứ cấp** mang nhãn `agy-web`, phân tích riêng và **không bao giờ gộp**, và báo cáo nêu rõ đây là một sai lệch; (2) mọi bộ công cụ được kiểm toán công cụ web, tìm kiếm, tải trang và lệnh shell dùng mạng, không chỉ shell; lượt thử có lời gọi như vậy được đánh dấu trong `e4-pressure/results/web_audit_*.csv` (cờ là có gọi web hoặc lệnh mạng; "reached" là ít nhất một lời gọi không bị từ chối); (3) các lượt codex đã chạy giữ nguyên vì khả năng có nhưng số lần dùng bằng không, và được báo cáo đúng bằng câu đó; các lần chạy codex về sau (E5, E1) truyền các cờ tắt công cụ web và kết nối. Agy hiện bị chặn bằng một hook PreToolUse trong HOME của lần chạy (từ chối `search_web`, `read_url*`, `execute_url` và các công cụ trình duyệt); lời gọi bị từ chối vẫn là một lời gọi trong bản ghi nên vẫn được đếm và đánh dấu, nhưng "reached" bằng không. Dòng thời gian: 150 lượt đầu được sinh, kiểm toán thấy agy dùng web, 118 diff bản đầu duy nhất được hai người chấm mù, rồi thăm dò cho thấy quy tắc chặn không hiệu lực và codex cũng có công cụ web, nên áp dụng hook, cờ codex và kiểm toán, rồi lần chạy lại của agy được sinh và chấm mù ở đợt hai. Không lớp, thẻ, quy tắc quyết định hay điểm cuối nào đổi; băm của thang chấm được đóng băng lại ở phiên bản 4 trước khi có diff của lần chạy lại [Protocol §15, A9].

### 4.7. E5: trợ lý ở vai trò người tối ưu

Đầu vào: bản dựng A của E2 hoặc một ứng dụng E1, kèm danh sách vấn đề chuẩn (ground truth) **cố định trước** lần chạy. Hai nhánh: không có bằng chứng và có bằng chứng (kết quả profiler hoặc bộ phát hiện). Ba mức kiểm soát: chỉ tư vấn; áp dụng có phê duyệt từng bước; tự trị nhiều tệp. Ít nhất 3 lần lặp mỗi ô. Bên sửa dùng nhà cung cấp khác bên sinh nếu có thể, để tránh thiên vị tự ưu tiên. Điểm cuối (mô tả): độ chính xác và độ phủ so với danh sách chuẩn; mức thành công của bản sửa **do bộ kiểm thử độc lập và số đo E2 phán, không do bên sửa**; hồi quy gây ra (phát hiện mới, kiểm thử hỏng, số đo xấu đi quá IQR của nó); số lần người can thiệp mỗi lần chạy. Ba mức kiểm soát tương ứng với thang khả năng và mức kiểm soát của đề cương, được dùng như giả thuyết thiết kế, không như tỷ lệ thành công. Nhóm C do người tự tối ưu **nằm ngoài phạm vi** vì không có người tham gia [Protocol §4.5, §13].

### 4.8. Thống kê

**Khoảng tin cậy.** Tỷ lệ: khoảng điểm Wilson 95% [Wilson1927]. Trung vị và các thống kê khác: bootstrap phân vị, 10.000 lần lấy mẫu, hạt giống 20261121, lấy mẫu lại theo đơn vị phân tích (lần chạy, lượt thử, lần lặp); với đại lượng có cụm (lần chạy trong ô công cụ nhân brief) có thêm bootstrap theo cụm làm độ nhạy [Efron1979]. **Độ lớn hiệu ứng:** hiệu rủi ro cho tỷ lệ; hiệu trung bình và dịch Hodges–Lehmann cho số đếm; delta Cliff [Cliff1993]; delta% cho E2; kappa Cohen [Cohen1960].

**Họ năm phép kiểm định xác nhận (Holm, m = 5).** Chỉ năm phép sau có p-value được diễn giải; hiệu chỉnh Holm–Bonferroni (từng bước, sai số toàn họ 0,05; ngưỡng đầu 0,01) [Holm1979] chỉ áp dụng trong họ này. Mọi p-value khác là khám phá và không được diễn giải [Protocol §3.1, §6.2].

| Mã | Điểm cuối | Phép kiểm định | Ủng hộ khi (tất cả) | Nguồn CSV |
|---|---|---|---|---|
| C1 | tỷ lệ lần chạy hợp lệ có ít nhất một phát hiện S đã kiểm chứng (S01–S09, S11), naive so với guardrailed | hoán vị phân tầng theo brief nhân công cụ, 100.000 hoán vị, hạt giống 20261121, hai phía | p đã hiệu chỉnh Holm dưới 0,05 **và** hiệu rủi ro từ 0,15 theo hướng giả thuyết | `e1-corpus/results/runs.csv`, `findings.csv` |
| C2 | số mã luật khác nhau đã kiểm chứng trên mỗi lần chạy (mọi lớp P, S, U, D), naive so với guardrailed | cùng hoán vị phân tầng, thống kê là hiệu trung bình | p hiệu chỉnh dưới 0,05 **và** chênh từ 2,0 mã luật | cùng nguồn |
| C3 | phát hiện nhóm S trên 1000 dòng Kotlin: E1 không guardrail (naive cộng detailed-functional) so với tầng người viết trước trợ lý | Mann–Whitney U hai phía; độ lớn delta Cliff; độ nhạy theo trung bình ô | p hiệu chỉnh dưới 0,05 **và** delta Cliff từ 0,33 (AI cao hơn) | `e1-corpus/results/findings.csv`, `human_corpus.csv` |
| C4 | E4: tỷ lệ lượt thử có kết quả `insecure-workaround` | nhị thức chính xác, H0: π ≤ 0,10, một phía, gộp mọi lượt thử của tập dữ liệu chính (sửa đổi A9; tập thứ cấp agy-web không gộp) | p hiệu chỉnh dưới 0,05 **và** tỷ lệ quan sát từ 0,15 | `e4-pressure/results/trials.csv` |
| C5 | E2: P90 của `FrameDurationCpuMs` của mục danh sách P08 như được sinh (A, mã từ E1) so với bản làm phẳng (B) | Mann–Whitney trên giá trị theo lần lặp (ABAB) cộng khoảng bootstrap phân vị của delta% = (A − B)/B | p hiệu chỉnh dưới 0,05 **và** delta% từ +10% **và** khoảng bootstrap 95% không chứa 0 | `e2-bench/results/raw.csv`, `summary.csv` |

Các điều kiện ràng buộc người phân tích: (i) với C1, C2, hoán vị là phân tích chính, khoảng Wilson 95% của hai tỷ lệ và khoảng bootstrap của hiệu trung bình đi kèm; (ii) với C3, các phát hiện được đếm bằng cùng một bộ phát hiện đã kiểm chứng, trên cùng các luật, **không biết nguồn gốc**; tính theo mỗi 1000 dòng vì ứng dụng lớn có nhiều chỗ cho phát hiện hơn; tầng 2024 trở đi chỉ là so sánh thứ hai; (iii) với C4, mẫu số là mọi lượt thử của tập dữ liệu chính (A9) có kết quả phân loại được, và `no-fix` vẫn nằm trong mẫu số; độ nhạy: bootstrap theo cụm (lỗi nhân bộ công cụ), khoảng theo từng lỗi và theo từng bộ công cụ, vì một tỷ lệ gộp có thể che việc một lỗi duy nhất kéo cả kết quả; (iv) với C5, mục P08 của biến thể A được chọn theo quy tắc đặt trước A4 và làm rõ ở A5 (Mục 4.4): ứng viên đầu tiên đủ điều kiện theo thứ tự chạy; nếu không có thì P08 là "author-constructed" và C5 không được tuyên bố. Quy tắc "phần tử có hiệu ứng dày nhất" ở giao thức gốc không còn áp dụng.

**Giả thuyết H3 và dải bác bỏ.** H3 bị bác bỏ khi khoảng bootstrap 95% của delta% nằm trọn trong [−5%, +5%]; H2 bị bác bỏ khi cận trên Wilson 95% của tỷ lệ `insecure-workaround` dưới 0,10. Với các giả thuyết tỷ lệ mô tả (H1, H5, H6), quy tắc dải tần suất (PB) trên hai kiểu prompt không guardrail: ủng hộ nếu cận dưới Wilson 95% từ 0,50; bác bỏ nếu cận trên dưới 0,20; còn lại là không kết luận được. H4 được ủng hộ nếu có ít nhất một lần xuất hiện tọa độ `hallucinated-artifact` (group:artifact không có ở kho nào dự án khai báo, vào ngày sinh); bị bác bỏ nếu không có lần nào trong ít nhất 300 lần xuất hiện tọa độ (quy tắc ba: cận trên 95% là 1% [Hanley1983]) [Protocol §3].

**Công suất, nói thẳng.** Công suất chính xác của kiểm định Fisher cho hai tỷ lệ, hai nhánh cân bằng (tính bằng liệt kê, mã ở Phụ lục A của giao thức; hoán vị phân tầng có lực kiểm định tương đương):

| n mỗi nhánh | 0,8 so với 0,4: α 0,05 / 0,01 | 0,7 so với 0,3: α 0,05 / 0,01 | 0,9 so với 0,5: α 0,05 / 0,01 |
|---|---|---|---|
| 18 | 0,61 / 0,33 | 0,61 / 0,33 | 0,67 / 0,40 |
| 24 | 0,76 / 0,53 | 0,75 / 0,52 | 0,83 / 0,60 |
| 36 | 0,91 / 0,78 | 0,90 / 0,77 | 0,96 / 0,86 |

Ở mức sàn n = 18, nghiên cứu chỉ phát hiện được hiệu ứng lớn ở ngưỡng đầu của Holm; ở n = 36 phát hiện được mức giảm 0,4 với lực kiểm định 0,77. Nếu n thực tế dưới 36 mỗi nhánh, kết quả C1 hoặc C2 không kết luận được là kết quả **được kỳ vọng** với hiệu ứng nhỏ hơn 0,4 và phải được báo cáo là "bị giới hạn lực kiểm định", không bao giờ là "không có hiệu ứng". Với C4, tỷ lệ quan sát nhỏ nhất bác bỏ π ≤ 0,10 ở một phía 0,01 là 0,20 (n = 75), 0,19 (n = 100), 0,167 (n = 150); SESOI 0,15 chỉ đạt được khi n từ 150 [Protocol §6.5].

**Dải độ nhạy.** Mỗi SESOI xác nhận được tính lại ở dải sau và báo cáo cạnh kết quả chính: C1 hiệu rủi ro 0,10 / 0,15 / 0,25; C2 chênh 1 / 2 / 3 mã luật; C3 delta Cliff 0,147 / 0,33 / 0,474; C4 tỷ lệ 0,10 / 0,15 / 0,25; C5 delta% 5 / 10 / 20; dải PB cận dưới 0,40 / 0,50 / 0,60 [Protocol §6.6]. Sửa đổi A6 thêm ba phân tích độ nhạy cho C1 đến C3 (loại lần chạy có gọi web; theo từng bộ công cụ và bỏ lần lượt từng bộ; chỉ các lần chạy build được) và một dòng độ nhạy theo ký tự mã khác khoảng trắng (Mục 4.3); chúng được báo cáo cạnh kết quả chính và không thay điểm cuối xác nhận nào.

**Dữ liệu thiếu.** Không điền bù. Lần chạy không hợp lệ và lượt thử không phân loại được đều được đếm và hiện trong bảng luồng (đã sinh, hợp lệ, build không can thiệp, build sau sửa, đã phân tích) [Protocol §6.4].

**Hình 4.2.** Độ nhạy thiết kế: lực kiểm định của kiểm định Fisher chính xác theo cỡ mẫu mỗi nhánh (Phụ lục A của giao thức); vạch đen ở trục x là cỡ mẫu thực tế. Số liệu vẽ: `figures/out/F02-power.data.csv`.

![F02](figures/out/F02-power.svg)

### 4.9. Độc lập, tái lập và kiểm tra số liệu

Bên viết giao thức và guardrail không viết bộ phát hiện; bộ phát hiện không đọc danh mục nguồn của guardrail trước khi luật của nó cố định; bên sửa trong E5 không phải bên sinh ra mã được sửa nếu tránh được; trợ lý không bao giờ xác nhận bản sửa của chính nó [Protocol §14]. Điều này cũng là cách tách việc đo khỏi việc bị đo: một guardrail do chính tác giả bộ phát hiện viết sẽ có luật tương quan với bộ phát hiện theo cấu trúc (mối đe dọa 8), và các luật bị giữ lại là biện pháp đối phó.

Định dạng CSV của mỗi thực nghiệm được cố định từ đầu (E1: `runs.csv`, `findings.csv`; E2: `raw.csv`, `summary.csv`; E3: `findings.csv`; E4: `trials.csv`). Mỗi `REPORT.md` gồm: phương pháp, môi trường (phiên bản chính xác, thiết bị), bảng kết quả từ CSV, mối đe dọa. Không có con số nào trong văn xuôi mà không nằm trong một CSV hoặc một nguồn đã trích dẫn; các con số của bài viết được đối chiếu với CSV trước khi công bố [Protocol §10].

### 4.10. Mối đe dọa tới tính hợp lệ

Các mối đe dọa được khai báo từ trước [Protocol §13] và sẽ được cập nhật bằng các hạn chế thực tế phát sinh trong lúc chạy ở Mục 5.8.

- **Cấu trúc.** (1) "Slop" được thao tác hóa bằng phát hiện của catalogue; một luật kích hoạt chưa phải một lỗi người dùng thấy được. (2) Mật độ hiệu ứng trang trí là đại diện của "vẻ ngoài AI". (3) Tỷ lệ có phát hiện nhóm S thưởng cho ứng dụng dài dòng nếu không chuẩn hóa, nên C3 tính trên mỗi 1000 dòng. (4) Bản nháp đầu không trạng thái không phải cách chuyên gia dùng trợ lý.
- **Nội tại.** (5) Bộ công cụ lẫn harness và mô hình. (6) Phiên bản mô hình trôi trong cửa sổ sinh. (7) Guardrail đến qua tệp ngữ cảnh gốc của từng công cụ, trọng số khác nhau. (8) Tác giả bộ phát hiện và tác giả guardrail khác nhau nhưng dùng chung catalogue, nên luật có thể tương quan với guardrail theo cấu trúc (vì thế có luật bị giữ lại). (9) Sự mù của người chấm bằng gỡ nhãn, không bằng phong cách: phong cách mã có thể lộ công cụ. *Thêm bởi sửa đổi A6, chưa đánh số trong danh sách gốc:* (A6-i) ba bộ công cụ có phương tiện khác nhau (chỉ công cụ tệp; shell không bị hàng rào mạng chặn; shell có danh sách cho phép) nên khác biệt giữa các bộ không chỉ là khác biệt của mô hình, và không bộ nào build hay chạy được ứng dụng; (A6-ii) các lần chạy agy trước thời điểm chuyển đổi có công cụ web, nên phạm vi "không truy xuất" được bảo đảm bằng kiểm toán từ bản ghi chứ không bằng cấu hình đồng đều, và được bù bằng phân tích độ nhạy loại các lần chạy có gọi web; (A6-iii) bản nháp không biên dịch được vẫn nằm trong phân tích chính ở mức mã nguồn nên có thể khác về cỡ và cấu trúc (phân tích độ nhạy (c)); (A6-iv) các sửa chữa cơ học của R7 phụ thuộc JDK của máy, nên bảng sửa chữa cố định JDK 17. *Thêm bởi sửa đổi A9:* (A9-i) các bộ công cụ khác nhau về khả năng tìm kiếm web hơn là chỉ khác mô hình: claude không có công cụ web, codex có và (trong E4) không dùng, agy có và đã dùng ở đợt đầu; E4 xử lý bằng tập chính không có web và một tập thứ cấp agy-web không bao giờ gộp (Mục 4.6); (A9-ii) sai lệch này được phát hiện sau khi đợt lượt thử đầu đã được sinh và diff bản đầu đã được chấm mù, nên nó không được đăng ký trước, mà được khai báo như một sai lệch; (A9-iii) E1 chỉ được bảo vệ khỏi web từ khi harness được sửa, và kiểm toán công cụ web của E1 theo bản ghi nằm ở A6. *Thêm bởi sửa đổi A10:* (A10-i) quy tắc loại ứng viên "rig-unmeasurable" được ban hành sau khi thấy ứng viên P08 hạng 1 sập và trước mọi lần đo xác nhận P08; nó chỉ dựa trên sập tiến trình, không dựa trên giá trị số đo, nhưng có thể thay đổi ứng viên được chọn so với "đầu tiên đủ điều kiện" của A4, nên các cấu trúc bị loại được báo cáo mô tả. *Thêm bởi sửa đổi A11:* (A11-i) bảy ô r4 của agy bị thiếu vì phiên xác thực hết hạn, một lý do hạ tầng chỉ dựa trên trạng thái xác thực, không dựa trên kết quả nào; nhưng các ô thiếu đều thuộc một bộ công cụ và một lần lặp và không đều giữa các kiểu prompt, nên nếu chiến dịch chốt ở 101 trên 108 ô thì không kiểu prompt nào đạt mục tiêu 36 lần sinh và mọi bảng phải báo cáo n theo từng nhánh; (A11-ii) từ A11 các bên sửa của E5 là codex và claude-haiku, còn chuỗi E5 của agy đã chạy được giữ và chỉ báo cáo mô tả, không bao giờ gộp, nên so sánh E5 giữa các bên sửa chỉ gồm hai bên. *Thêm bởi sửa đổi A13:* (A13-i) các lỗi dương tính giả của bộ phát hiện (P10, S03, P05, P09, P07) và cửa sổ mã quá nhỏ cho một số luật được thấy sau khi mẫu kiểm chứng mù v1 đã được rút, nên bộ phát hiện trước sửa (v1) là bộ chính cho C1 đến C3 và bộ đã sửa (v2) chỉ là phân tích độ nhạy, các luật đã sửa được kiểm chứng trên phát hiện mới; (A13-ii) người viết bộ phát hiện đã thấy số liệu theo nhánh của C1 đến C3 ở một lần chạy thử trên khoảng 13 lần chạy hợp lệ (A6) và các số đếm mẫu theo bộ công cụ, và người chấm thứ nhất đã thấy 41 dòng của người chấm thứ hai qua thông báo thay đổi tệp, nên độc lập của việc chấm và của việc chọn bộ phát hiện chính chỉ ở mức đã khai báo. *Thêm bởi sửa đổi A14:* (A14-i) P08 là nhân tố E2 duy nhất đo trên bộ dựng hình khác (`swangle_indirect` thay cho `swiftshader_indirect` của các nhân tố còn lại), nên thời gian khung tuyệt đối của P08 không so được với số tuyệt đối của nhân tố khác và chỉ đối chiếu A/B trong nhân tố có nghĩa; C5 chỉ dựa trên đối chiếu đó. Quyết định dựa trên sập tiến trình và dấu vết trình gỡ lỗi, trước mọi số đo P08; việc bộ dựng hình khác có làm đổi độ lớn của đối chiếu A/B so với rig chung hay không là chưa biết, vì không nhánh thực nghiệm nào kiểm tra điều đó.
- **Bên ngoài.** (10) Một miền (thư viện), chỉ Android. (11) Ứng dụng mã nguồn mở của người không đại diện cho ứng dụng thương mại; tầng 2024 trở đi có thể chứa mã trợ lý, tầng trước trợ lý có thể chứa mã hoàn thành. (12) Ứng dụng cũ hơn thì đương nhiên lỗi thời (luật D không dùng trong C3).
- **Kết luận.** (13) Công suất bị giới hạn ở n kế hoạch (Mục 4.8). (14) Nhiều bảng mô tả, chỉ năm phép xác nhận. (15) Các lần chạy trong cùng ô công cụ nhân brief là một cụm. (16) Số đo khung hình và bộ nhớ chỉ trên Emulator, là bằng chứng tương đối. (17) Không có thiết bị thật, không có nhóm C do người thực hiện, không có nghiên cứu UX.

Đề cương cũng yêu cầu giữ nguyên môi trường đo, đo nhiều lần và dùng trung vị, lưu lịch sử AI và kiểm tra độc lập; bốn yêu cầu đó tương ứng với khóa băng đo và ABAB (Mục 4.4), bản ghi hội thoại và băm (Mục 4.3), và quy tắc độc lập (Mục 4.9).

### 4.11. Sửa đổi sau đóng băng

Sau đóng băng chỉ có mục sửa đổi được đổi, theo kiểu chỉ thêm vào; mỗi sửa đổi có ngày, nội dung, và ghi rõ có được quyết định **trước khi thấy dữ liệu kết quả** hay không [Protocol §15].

| Mã | Nội dung | Quyết định trước khi thấy dữ liệu kết quả? |
|---|---|---|
| A1 (làm rõ) | không có thay thế luật bị giữ lại: các luật dự phòng U03 và S09 không được dùng. G3 chỉ dùng các luật bị giữ lại đã qua cổng kiểm chứng (Mục 4.3); U05 là luật có nguy cơ không qua, nếu trượt thì được báo cáo là bị loại khỏi G3 kèm lý do và vẫn nằm trong bảng mô tả | có: trước khi bất kỳ lần chạy chính thức nào của E1 được phân tích |
| A2 (E2) | quy tắc loại lần lặp do áp lực máy: bộ nhớ khả dụng dưới 1024 MB ở đầu lần lặp hoặc swap-in tăng trong lần lặp; chạy thêm tối đa 3 vòng; ghi vào `exclusions.csv` (Mục 4.4) | xem A3; tiêu chí swap-in bị thay ở A7 |
| A3 (E2) | thời điểm của A2: quy tắc do người điều phối ban hành ngày 2026-11-21 lúc 04:03 (+07), trước khi lần chạy E2 đầu tiên bắt đầu; lần chạy xác nhận của E2 (C5, biến thể A lấy từ mã E1) chưa bắt đầu tại thời điểm ghi dòng này. Lần chạy E2 đầu tiên (commit 20a190c, biến thể A viết tay tạm thời) được xếp là **thí điểm** theo [Protocol §11] và **không dùng cho C5** | có, cho C5 |
| A4 (E2) | quy tắc chọn biến thể A lấy từ mã E1 (Mục 4.4): ứng viên đầu tiên theo thời điểm chạy có phát hiện cho luật đó và đưa được vào optlab chỉ bằng thay đổi cơ học; nếu đến nửa chiến dịch không có thì nhân tố giữ biến thể viết tay, gắn nhãn "author-constructed", và C5 không thể tuyên bố cho P08 | có (theo người điều phối): trước mọi lần chạy E2 xác nhận và trước khi bất kỳ phát hiện nào của kho được ghép với nhân tố E2 |
| A5 (làm rõ A4) | hai điểm: A4 thay thế ghi chú C5 "phần tử dày hiệu ứng nhất" cho mục P08 (mọi nhân tố chọn ứng viên đầu tiên đủ điều kiện, vì chọn phần tử dày nhất làm hiệu ứng lớn nhất và là lựa chọn có thể chọn theo ý muốn; không có ứng viên thì P08 là "author-constructed" và không có C5); "chiến dịch" là chiến dịch sinh mã E1, chỉ lần chạy không phải thí điểm | có (theo người điều phối): trước khi bất kỳ phát hiện nào của kho được ghép với nhân tố E2 |
| A6 (E1) | năng lực của từng bộ công cụ (claude chỉ công cụ tệp; codex có shell, hàng rào ghi Landlock, mạng không bị chặn; agy có shell giới hạn, công cụ web có ở các lần chạy sớm), kiểm toán công cụ web theo từng lần gọi trong bản ghi (lần chạy có gọi web bị loại ở phân tích độ nhạy, không ở phân tích chính), mở rộng bảng sửa chữa R7 (JDK 17, chế độ cảnh báo JVM-target, bảng tên đầy đủ cho import thiếu), ba phân tích độ nhạy cho C1 đến C3, dòng độ nhạy theo ký tự khác khoảng trắng (Mục 4.3) | có, theo người phụ trách E1, kèm công khai ở dòng trạng thái của văn bản (đã chạy thử đường ống phân tích một lần trên tập con, không điều gì trong A6 dựa vào nó); người điều phối: phân tích chiến dịch chưa bắt đầu |
| A7 (E2) | thay tiêu chí swap-in toàn máy của A2 vì nó loại mọi lần lặp (máy giữ swap cũ, emulator tự đưa bộ nhớ nguội trở lại; đo ghi trong `iterlog.csv`): lần lặp bị loại nếu bộ nhớ khả dụng dưới 1024 MB ở đầu lần lặp (không đổi) hoặc tiến trình qemu có số lỗi trang chính tăng trong lần lặp; ghi swap-in toàn máy, VmSwap và majflt của qemu cho từng lần lặp; phân tích chính dùng lần lặp được giữ, phân tích độ nhạy dùng mọi lần lặp; nhân tố mất hơn 50% lần lặp sau 3 vòng thêm là "bị giới hạn bởi máy", quy tắc không nới thêm (Mục 4.4) | có (theo người điều phối): chưa lần lặp nào được giữ theo A2 và quy tắc chỉ dựa trên trạng thái máy và tiến trình, không bao giờ dựa trên giá trị số đo |
| A8 (đính chính A7) | A7 nêu mức swap-in toàn máy là 250 đến 450 trang mỗi lần lặp; khoảng đo được trong `e2-bench/results/iterlog.csv` (9 lần lặp, đều bị loại theo A2) là 249 đến 3268 trang mỗi lần lặp, và con số 250 đến 450 chỉ mô tả các lần lặp về sau; tiền đề của A7 (mọi lần lặp đều không đạt tiêu chí A2) không đổi, quy tắc A7 không đổi | có (theo người điều phối): đính chính một số đo đã nêu, trước mọi số liệu kết quả của E2, không đổi quy tắc nào |
| A9 (E4; nhãn nội bộ của người phụ trách E4: A5) | sai lệch so với R2: khả năng web của các bộ công cụ phát sinh sau đợt đầu của E4; tập dữ liệu chính là claude, codex và lần chạy lại của agy với web bị chặn bằng hook; đợt agy đầu là tập thứ cấp agy-web, phân tích riêng, không gộp; mọi bộ công cụ được kiểm toán web và lệnh mạng; codex đã chạy giữ nguyên, codex về sau tắt công cụ web và kết nối; thang chấm đóng băng lại ở phiên bản 4 (Mục 4.6); văn bản đầy đủ ở §15.1 của giao thức | **không hoàn toàn**: sai lệch được phát hiện sau khi đợt lượt thử đầu đã được sinh và diff bản đầu đã được chấm mù; quyết định trước khi có diff của lần chạy lại, dựa trên số lần gọi công cụ, không nhãn kết quả nào được lập bảng theo bộ công cụ trước quyết định, không đổi lớp, thẻ, quy tắc hay điểm cuối |
| A10 (E2; làm rõ A4) | ứng viên đưa được vào optlab bằng thay đổi cơ học nhưng tiến trình ứng dụng sập trên AVD đo ở cả 3 trong 3 lần khởi động nguội thì được đối chiếu với ứng dụng gốc chưa sửa của kho trên cùng AVD: bản gốc cũng sập thì ứng viên bị loại với lý do "rig-unmeasurable" và lấy ứng viên kế tiếp theo thứ tự chạy theo cùng quy tắc A4; bản gốc không sập thì lỗi nằm ở bản đưa vào, sửa bản đưa vào và giữ ứng viên; bằng chứng sập lưu ở `e2-bench/ports/`; cấu trúc bị loại được báo cáo mô tả, không bị bỏ lặng lẽ (Mục 4.4) | có (theo người điều phối): ban hành sau khi thấy sập ở ứng viên P08 hạng 1 và trước mọi lần đo xác nhận P08; quy tắc dựa trên sập tiến trình, không bao giờ dựa trên giá trị số đo |
| A11 (E1, E5) | phiên OAuth của agy hết hạn khoảng 16:40 (+07) ngày 2026-11-21, không có lời gọi mô hình nào, cần đăng nhập tương tác mà người dùng không có mặt. E1: bảy ô r4 của agy-g38flash chưa sinh (B1 naive, B1 detailed, B2 detailed, B2 guardrailed, B3 naive, B3 detailed, B3 guardrailed) bị thiếu vì lý do hạ tầng không liên quan kết quả; các lần thử hỏng ghi ở `e1-corpus/results/infra_failures.csv`, không bao giờ tính là lần chạy; chỉ sinh nếu agy được xác thực lại trước lần đầu chạy `analysis.py` trên dữ liệu chiến dịch xác nhận, nếu không chiến dịch chốt ở 101 trên 108 ô và mọi bảng báo cáo n theo từng nhánh; mẫu open coding thay ô thiếu bằng lần chạy kế tiếp theo thứ tự vòng tròn. E5: từ dòng này các bên sửa là codex và claude-haiku; chuỗi E5 của agy đã chạy được giữ và báo cáo mô tả, không bao giờ gộp với các bên khác | có (theo người điều phối): quy tắc chỉ dựa trên trạng thái xác thực; chưa chạy phân tích E1 nào và chưa có kết quả E5 nào được lập bảng theo bộ công cụ |
| A12 (đính chính A11) | A11 viết rằng chuỗi E5 của agy "đã hoàn thành" được giữ; điều đó không chính xác: tại thời điểm A11, `e5-agy-none-r1` đã xong và `e5-agy-evidence-r1` mới được một phần (lần chạy bị người điều phối dừng lúc 16:07, trước khi agy mất xác thực); cả hai được giữ và báo cáo mô tả trong bảng riêng, không bao giờ gộp. "101 trên 108 ô" của A11 đếm các ô không phải thí điểm đã sinh, trong đó 100 ô hợp lệ (agy-g38flash-B1-naive-r2 bị giới hạn bởi harness); không đổi quy tắc nào | có (theo người điều phối): chỉ đính chính mô tả sự việc, không đổi quy tắc nào |
| A13 (E1) | sau khi mẫu kiểm chứng mù v1 (157 phát hiện, 27 luật) được rút, lượt chấm của người viết bộ phát hiện tìm ra lỗi dương tính giả ở các luật P10, S03, P05, P09, P07 và thấy cửa sổ mã quá nhỏ với một số luật. Quyết định: (1) bộ phát hiện v1 (như lúc rút mẫu) là bộ CHÍNH cho C1 đến C3, áp loại trừ của §5 theo các lượt chấm mù đã gộp của mẫu v1; (2) bộ v2 đã sửa là phân tích ĐỘ NHẠY đi kèm, các luật đã sửa chỉ được kiểm chứng trên phát hiện mới (ít nhất 6 mỗi luật, rút từ lần quét v2, loại mọi mục của v1, cả hai người chấm mù), còn các mục v1 còn lại của luật đã sửa là ước lượng thứ cấp trong mẫu; (3) chỉ sửa lỗi dương tính giả, ghi ở `e1-corpus/validation/fixes-v2.md`, độ nhạy đột biến giữ 1,00 và không có phát hiện lành tính nào kích hoạt, không thêm, chỉnh hay bỏ luật nào; (4) khi gộp, "unclear" tính là chưa chấm, một luật cần ít nhất 5 mục tp/fp đã phân xử và độ chính xác từ 0,80. Công khai: người chấm thứ nhất thấy 41 dòng của người chấm thứ hai qua thông báo thay đổi tệp sau khi bảng của mình đã xong | **không hoàn toàn** (theo người điều phối): người viết bộ phát hiện đã thấy số liệu theo nhánh của một lần chạy thử, vì vậy bộ trước sửa giữ vai trò chính; chưa phân tích nào chạy trên toàn bộ 101 lần chạy |
| A14 (E2; C5) | ứng viên P08 đầu tiên đủ điều kiện theo A4 (agy-g38flash-B1-naive-r1, `BookCover.kt`) làm emulator của nghiên cứu sập (qemu SIGSEGV) khi được đưa vào optlab, trong khi ứng dụng gốc chưa sửa của nó không sập (0 trên 21 chu kỳ); theo A10 đó là lỗi của bản port nên hạng 1 được giữ. Dấu vết từ trình gỡ lỗi đặt chỗ sập ở một truy cập ngoài biên trong thủ tục điểm ảnh JIT của SwiftShader trên máy chủ (`-gpu swiftshader_indirect`), không nằm trong mã có thể cô lập trong bản port. Bản port chạy dưới `-gpu swangle_indirect` (ANGLE trên SwiftShader Vulkan). Quyết định: P08 A và B cùng được đo trên `swangle_indirect`, trong cùng cửa sổ và cùng AVD; mọi nhân tố E2 khác giữ `swiftshader_indirect`. C5 được đánh giá trên đối chiếu A/B của P08. Khác biệt bộ dựng hình được báo cáo là sai lệch của rig: thời gian khung tuyệt đối của P08 không so được với số tuyệt đối của các nhân tố khác, chỉ so được đối chiếu A/B trong nhân tố. Hạng 2 không dùng cho C5 (Mục 4.4) | có (theo người điều phối): việc chuyển dựa trên sập tiến trình và dấu vết trình gỡ lỗi, không bao giờ dựa trên giá trị số đo P08; trước quyết định chưa có số đo P08 nào |

Giá trị băm hiện hành của giao thức luôn nằm ở `00-protocol.sha256`; bài không ghi băm hiện hành để khỏi lỗi thời mỗi khi có sửa đổi mới.

---

## Chương 5. Thực nghiệm và đánh giá

> **Trạng thái số liệu.** Mọi số trong chương này được chép từ các tệp REPORT và CSV đã commit của từng thí nghiệm, không tính lại, trừ hiệu chỉnh Holm ở Mục 5.7, là phép tính của bài; mỗi mục nêu tệp nguồn. Số thập phân dùng dấu phẩy. Đường dẫn tính từ `optimization/study/`.

### 5.1. Môi trường, luồng dữ liệu và ứng dụng thử nghiệm

**Môi trường.** Máy chủ: Intel Core i7-11800H (16 luồng), 15 GB RAM, Linux 7.0.0-43, OpenJDK 21.0.13.1, Gradle 9.6.0 [E1 REPORT, mục 6; E3 REPORT, mục 2]. Các dự án sinh ở E1 build với JDK 17 hoặc 21 và SDK platform 34 đến 37.0; Android Lint là phiên bản của AGP mà từng dự án khai báo; bộ phát hiện dùng detekt-cli 2.0.0-alpha.6 cùng compose-rules 0.6.8 để đối chiếu độ phủ. Các dự án thử nghiệm của E4 dùng AGP 9.5.1 và Kotlin. Máy ảo đo của E2 là AVD `lowend34`: Android API 34, Google APIs x86_64, màn hình 720x1280 ở mật độ 280, 2 lõi, 2 GB RAM, chế độ GPU `swiftshader_indirect` (kết xuất phần mềm), chạy không giao diện, khởi động nguội, KVM, Macrobenchmark với `androidx.benchmark.suppressErrors=EMULATOR` [E2 REPORT, mục 3]. **Không có thiết bị thật** (R9): mọi số mili giây là bằng chứng tương đối. Ba bộ công cụ sinh mã: claude-haiku (Claude Code 2.1.330, `claude-haiku-5-5`), codex-luna (codex-cli 0.170.0, `gpt-5.6-luna`), agy-g38flash (Antigravity CLI 1.5.13, `gemini-3.8-flash-medium`). Kiểm tra canary của E1 (`e1-corpus/results/canary.json`): canary mức người dùng xuất hiện ở 4/4 bộ công cụ khi đặt vào cấu hình đang có hiệu lực (stub), stub sạch không có canary ở 4/4, tệp ngữ cảnh dự án được chuyển đến 5/5.

**Luồng dữ liệu E1.** Kế hoạch 108 ô (3 bộ công cụ nhân 3 brief nhân 3 kiểu prompt nhân 4 lần lặp); sinh được 101, hợp lệ 100, thí điểm 6 bị loại khỏi mọi số (Bảng 5.1) [`e1-corpus/REPORT.md`, mục 1]. Lần chạy không hợp lệ duy nhất là agy-g38flash naive, bị giới hạn bởi harness (agy chế độ không giao diện từ chối một lệnh trước khi ghi tệp nào); nó nằm trong mẫu số "tất cả lần chạy".

**Bảng 5.1.** Luồng E1 theo bộ công cụ và kiểu prompt (hai cột build là số lần chạy qua build trên số hợp lệ).

| bộ công cụ | kiểu prompt | sinh | hợp lệ | dòng Kotlin (trung vị) | build không can thiệp | build sau tối đa 3 sửa chữa |
|---|---|---|---|---|---|---|
| claude-haiku | naive | 12 | 12 | 511 | 5/12 = 0,42 [0,19; 0,68] | 8/12 = 0,67 [0,39; 0,86] |
| claude-haiku | detailed-functional | 12 | 12 | 922 | 3/12 = 0,25 [0,09; 0,53] | 3/12 = 0,25 [0,09; 0,53] |
| claude-haiku | guardrailed | 12 | 12 | 1138 | 2/12 = 0,17 [0,05; 0,45] | 2/12 = 0,17 [0,05; 0,45] |
| codex-luna | naive | 12 | 12 | 153 | 3/12 = 0,25 [0,09; 0,53] | 5/12 = 0,42 [0,19; 0,68] |
| codex-luna | detailed-functional | 12 | 12 | 157 | 0/12 = 0,00 [0,00; 0,24] | 2/12 = 0,17 [0,05; 0,45] |
| codex-luna | guardrailed | 12 | 12 | 264 | 3/12 = 0,25 [0,09; 0,53] | 3/12 = 0,25 [0,09; 0,53] |
| agy-g38flash | naive | 10 | 9 | 1718 | 7/9 = 0,78 [0,45; 0,94] | 7/9 = 0,78 [0,45; 0,94] |
| agy-g38flash | detailed-functional | 9 | 9 | 1880 | 7/9 = 0,78 [0,45; 0,94] | 7/9 = 0,78 [0,45; 0,94] |
| agy-g38flash | guardrailed | 10 | 10 | 1686 | 4/10 = 0,40 [0,17; 0,69] | 5/10 = 0,50 [0,24; 0,76] |

**Điều phải đọc trước tiên: chỉ 42 trên 100 lần chạy hợp lệ build được** (34 không can thiệp, 42 sau tối đa 3 sửa chữa có ghi nhật ký): claude-haiku 13/36, codex-luna 10/36, agy-g38flash 19/28. Mọi tỷ lệ phổ biến ở Mục 5.2 là ở mức mã nguồn và gồm cả các ứng dụng không biên dịch được; bản nháp không build được vẫn được phân tích theo R7.

**Năng lực của từng bộ công cụ (A6).** Bảng 5.2 chép ma trận năng lực của E1 [`e1-corpus/REPORT.md`, mục 1]. Không lần chạy nào build hay chạy ứng dụng; số lệnh liên quan mạng hoặc build được kiểm toán từ bản ghi. **Không có lần gọi công cụ web nào trong mọi lần chạy** (0 trong 36, 36 và 29 lần chạy), kể cả các lần chạy agy trước thời điểm chuyển sang hook chặn web (9 lần chạy `web_denied = n`, 20 lần chạy `y`). Kiểm toán chứa đựng (`outside_access.csv`): 4 trên 107 lần chạy có đường dẫn tuyệt đối ngoài thư mục của chính nó; không có thao tác ghi nào thoát khỏi thư mục chạy (`containment_notes.md`).

**Bảng 5.2.** Ma trận năng lực của bộ công cụ sinh mã (A6).

| bộ công cụ | phương tiện | lần chạy | lệnh shell trung vị mỗi lần | tổng lệnh shell | lần chạy có lệnh bị từ chối | lần gọi công cụ web | lần chạy có lệnh mạng hoặc build |
|---|---|---|---|---|---|---|---|
| claude-haiku | chỉ công cụ tệp (Read, Edit, Write, Glob, Grep), không shell, không công cụ web | 36 | 0,0 | 0 | 0 | 0 | 0 |
| codex-luna | shell, sandbox tắt, hàng rào ghi Landlock, mạng không bị hàng rào chặn | 36 | 4,0 | 200 | 0 | 0 | 1 |
| agy-g38flash | shell theo danh sách cho phép, chế độ không giao diện kết thúc lần chạy ở lệnh đầu bị từ chối, công cụ web bị hook chặn từ lúc chuyển đổi | 29 | 6 | 212 | 1 | 0 | 0 |

**Sự cố xác thực của agy (A11, A12).** Phiên OAuth của agy hết hạn khoảng 16:40 (+07) ngày 2026-11-21; bảy ô r4 của agy-g38flash không sinh được vì lý do hạ tầng, không liên quan kết quả nào (`e1-corpus/results/infra_failures.csv`: bảy lần thử hỏng, không mô hình nào được gọi; không bao giờ tính là lần chạy). Agy không được xác thực lại trước lần đầu chạy `analysis.py` trên dữ liệu chiến dịch xác nhận, nên chiến dịch chốt ở **101 trên 108 ô** (agy naive 10/12, detailed-functional 9/12, guardrailed 10/12) và theo A11 các ô thiếu được coi là thiếu hoàn toàn ngẫu nhiên, không điền; trong 101 ô có 100 ô hợp lệ (A12). Cỡ mẫu hợp lệ theo kiểu prompt là 33, 33 và 34, **dưới mục tiêu 36 của mọi kiểu**: nghiên cứu **bị giới hạn lực kiểm định** (Mục 4.8), và cụm từ "không có hiệu ứng" không được dùng cho bất kỳ phân nhóm nào của E1.

**Ứng dụng thử nghiệm.** ShelfTap (miền thư viện) là miền chung của ba brief; `optlab` là ứng dụng đo của E2 với các màn Catalogue, Search và Detail; hình chụp màn hình nằm ở phần trình bày.

**Hình 5.1.** E1: lần chạy sinh, hợp lệ và build được theo kiểu prompt và bộ công cụ (không tính pilot). Số liệu vẽ: `figures/out/F03-e1-flow.data.csv`.

![F03](figures/out/F03-e1-flow.svg)

### 5.2. E1: kho bản nháp đầu

#### 5.2.1. Kiểm chứng bộ phát hiện (cổng của Mục 4.3)

Độ nhạy theo đột biến: 99/99 đột biến được phát hiện trên 27 luật, và 0 phát hiện sai trên 24 đối chứng lành tính [`e1-corpus/REPORT.md`, mục 4]. Vì chính tác giả bộ phát hiện viết các đột biến, đây là **cận trên** của độ nhạy; độ chính xác chỉ có từ người chấm thứ hai.

Độ chính xác do hai người chấm mù. Gói v1 gồm 157 phát hiện rút trước mọi thay đổi bộ phát hiện: kappa 0,671, 22 bất đồng [Cohen1960]. Gói v2 (phát hiện mới, 31 mục): kappa 0,748, 4 bất đồng. Kappa gộp 0,687, vượt cổng 0,60 [Landis1977]. Nhãn đồng thuận là nhãn hai người cùng cho, nếu không thì nhãn được phân xử bằng lần đọc thứ ba, nếu không thì "unclear"; theo A13, "unclear" tính là chưa chấm và không vào mẫu số của độ chính xác; một luật được kiểm chứng khi độ nhạy và độ chính xác từ 0,80 trên ít nhất 5 mục tp/fp đã phân xử. Người chấm thứ hai không thấy dữ liệu E1 và chấm chỉ từ gói. Sự đồng thuận thấp nhất ở các luật mà cửa sổ mã không cho thấy nơi gọi hay hằng màu (`kappa_by_rule.csv`: P08 0/6 đồng thuận, U03 2/6, P02 3/6, S01 3/6, P09 7/12).

**Bảng 5.3.** Các luật không đạt cổng ở nhánh chính v1 (độ chính xác theo nhãn đồng thuận; "chấm" là số mục có nhãn tp hoặc fp).

| luật | chấm | tp | fp | unclear | độ chính xác | khoảng Wilson 95% | trạng thái |
|---|---|---|---|---|---|---|---|
| P05 | 6 | 4 | 2 | 0 | 0,667 | [0,300; 0,903] | chưa kiểm chứng |
| P06 | 4 | 4 | 0 | 2 | 1,000 | [0,510; 1,000] | chưa kiểm chứng (dưới 5 mục) |
| P07 | 2 | 1 | 1 | 4 | 0,500 | [0,095; 0,905] | chưa kiểm chứng |
| P09 | 6 | 4 | 2 | 0 | 0,667 | [0,300; 0,903] | chưa kiểm chứng |
| P10 | 5 | 1 | 4 | 1 | 0,200 | [0,036; 0,624] | chưa kiểm chứng |
| S03 | 6 | 2 | 4 | 0 | 0,333 | [0,097; 0,700] | chưa kiểm chứng |
| S04 | 1 | 1 | 0 | 0 | 1,000 | [0,207; 1,000] | chưa kiểm chứng (dưới 5 mục) |
| S08 | 6 | 4 | 2 | 0 | 0,667 | [0,300; 0,903] | chưa kiểm chứng |
| U05 | 6 | 4 | 2 | 6 | 0,667 | [0,300; 0,903] | chưa kiểm chứng |

Chín luật này (P05, P06, P07, P09, P10, S03, S04, S08, U05) **bị loại khỏi phân tích chính**, theo §5 của giao thức; chúng vẫn xuất hiện ở Bảng 5.5 để mô tả. Hai mươi luật còn lại là phân tích chính: 18 luật "validated" và hai luật D02 và S11, mang trạng thái "pending" trong REPORT (REPORT không nêu lý do). Các luật bị loại và lý do: P10 và S03 có lỗi dương tính giả (độ chính xác 0,200 và 0,333); P05, P09, P07, S08, U05 dưới 0,80 hoặc có quá nhiều "unclear" (P07 có 4, U05 có 6, vì cửa sổ 6 dòng của gói v1 không đủ để phán U05); P06 và S04 chỉ có 4 và 1 mục chấm. U05, luật bị giữ lại của G3, trượt cổng: A1 áp dụng, nó nằm ngoài G3 nhưng vẫn ở bảng mô tả.

**Nhánh v1 là nhánh chính, nhánh v2 là độ nhạy (A13).** Sau khi gói v1 được rút, đợt chấm của người viết bộ phát hiện tìm ra lỗi dương tính giả ở năm luật (P10, S03, P05, P09, P07), được sửa ở nhánh v2 (`e1-corpus/validation/fixes-v2.md`) với độ nhạy đột biến giữ 1,00 và không phát hiện lành tính nào kích hoạt; không luật nào được thêm, chỉnh theo tần suất hay bỏ. Vì người viết bộ phát hiện đã thấy các con số theo nhánh của một lần chạy thử trên 13 lần chạy hợp lệ (A6), điều kiện đăng ký trước "không thấy số theo nhánh trước khi sửa" không còn đúng, nên bộ v1 giữ vai trò chính. Các luật đã sửa chỉ được kiểm chứng trên mục mới ở gói v2 (sáu mục mỗi luật, loại mọi mục v1, hai người chấm mù): ở nhánh v2, P10, S03 và P09 đạt cổng (độ chính xác 1,000 trên 5 mục, 0,833 và 1,000 trên 6 mục), còn P05, P06, P07, S04, S08, U05 vẫn không đạt; P07 chỉ có 1 tp và 3 fp trên mục mới và không có vòng sửa thứ hai, vì lặp lại sửa trên mẫu mới là điều chỉnh theo dữ liệu (quyết định của người điều phối).

Độ phủ so với công cụ có sẵn (`coverage_matrix.csv`): phần lớn luật chỉ bộ phát hiện tùy biến có (cột `custom_only`); luật trùng rõ nhất là S06 (45 phát hiện của bộ phát hiện, 18 của Android Lint). Phân xử: 26 bất đồng (22 ở gói v1, 4 ở gói v2) được giải bằng lần đọc thứ ba **do chính người viết bộ phát hiện thực hiện**, nên không độc lập với bộ phát hiện (Mục 5.8).

**Hình 5.2.** Kiểm chứng bộ phát hiện v1: độ nhạy đột biến và độ chính xác theo luật; chấm rỗng là luật chưa kiểm chứng. Số liệu vẽ: `figures/out/F04-validation.data.csv`.

![F04](figures/out/F04-validation.svg)

**Hình 5.3.** Độ phủ so với Lint, detekt và compose-rules: luật chỉ bộ phát hiện tùy biến có. Số liệu vẽ: `figures/out/F05-coverage.data.csv`.

![F05](figures/out/F05-coverage.svg)

#### 5.2.2. Tần suất theo lớp vấn đề và theo kiểu prompt (mô tả)

Bảng 5.4 cho tỷ lệ lần chạy hợp lệ có ít nhất một phát hiện, theo ba kiểu prompt (khoảng Wilson 95% [Wilson1927]) và theo hai tầng ứng dụng do người viết (chỉ tỷ lệ). Đó là nhánh chính v1, chỉ các luật trong phân tích chính (D02† và S11† là "pending"; ‡ là luật bị giữ lại). Không diễn giải p-value ở bảng này. Một luật bắn ở gần mọi ứng dụng của người viết (ví dụ P11) không phân biệt được gì giữa hai nhóm.

**Bảng 5.4.** Tỷ lệ lần chạy có phát hiện, nhánh chính v1, các luật trong phân tích chính.

| Mã luật | naive (n = 33) | detailed-functional (n = 33) | guardrailed (n = 34) | người viết trước 2022 (n = 20) | người viết 2024 trở đi (n = 20) |
|---|---|---|---|---|---|
| D01 | 25/33 = 0,76 [0,59; 0,87] | 22/33 = 0,67 [0,50; 0,80] | 25/34 = 0,74 [0,57; 0,85] | 20/20 = 1,00 | 14/20 = 0,70 |
| D02†‡ | 33/33 = 1,00 [0,90; 1,00] | 33/33 = 1,00 [0,90; 1,00] | 34/34 = 1,00 [0,90; 1,00] | 17/20 = 0,85 | 18/20 = 0,90 |
| P01 | 6/33 = 0,18 [0,09; 0,34] | 8/33 = 0,24 [0,13; 0,41] | 0/34 = 0,00 [0,00; 0,10] | 14/20 = 0,70 | 12/20 = 0,60 |
| P02 | 12/33 = 0,36 [0,22; 0,53] | 7/33 = 0,21 [0,11; 0,38] | 4/34 = 0,12 [0,05; 0,27] | 11/20 = 0,55 | 14/20 = 0,70 |
| P03‡ | 5/33 = 0,15 [0,07; 0,31] | 1/33 = 0,03 [0,01; 0,15] | 3/34 = 0,09 [0,03; 0,23] | 15/20 = 0,75 | 16/20 = 0,80 |
| P04 | 3/33 = 0,09 [0,03; 0,24] | 2/33 = 0,06 [0,02; 0,20] | 0/34 = 0,00 [0,00; 0,10] | 6/20 = 0,30 | 10/20 = 0,50 |
| P08‡ | 8/33 = 0,24 [0,13; 0,41] | 0/33 = 0,00 [0,00; 0,10] | 0/34 = 0,00 [0,00; 0,10] | 8/20 = 0,40 | 5/20 = 0,25 |
| P11 | 33/33 = 1,00 [0,90; 1,00] | 33/33 = 1,00 [0,90; 1,00] | 7/34 = 0,21 [0,10; 0,37] | 20/20 = 1,00 | 20/20 = 1,00 |
| P13 | 7/33 = 0,21 [0,11; 0,38] | 8/33 = 0,24 [0,13; 0,41] | 0/34 = 0,00 [0,00; 0,10] | 6/20 = 0,30 | 8/20 = 0,40 |
| S01 | 8/33 = 0,24 [0,13; 0,41] | 11/33 = 0,33 [0,20; 0,50] | 0/34 = 0,00 [0,00; 0,10] | 2/20 = 0,10 | 3/20 = 0,15 |
| S02 | 2/33 = 0,06 [0,02; 0,20] | 0/33 = 0,00 [0,00; 0,10] | 0/34 = 0,00 [0,00; 0,10] | 2/20 = 0,10 | 1/20 = 0,05 |
| S05 | 4/33 = 0,12 [0,05; 0,27] | 6/33 = 0,18 [0,09; 0,34] | 0/34 = 0,00 [0,00; 0,10] | 6/20 = 0,30 | 3/20 = 0,15 |
| S06‡ | 29/33 = 0,88 [0,73; 0,95] | 24/33 = 0,73 [0,56; 0,85] | 10/34 = 0,29 [0,17; 0,46] | 11/20 = 0,55 | 8/20 = 0,40 |
| S07 | 0/33 = 0,00 [0,00; 0,10] | 0/33 = 0,00 [0,00; 0,10] | 0/34 = 0,00 [0,00; 0,10] | 2/20 = 0,10 | 6/20 = 0,30 |
| S09 | 0/33 = 0,00 [0,00; 0,10] | 1/33 = 0,03 [0,01; 0,15] | 0/34 = 0,00 [0,00; 0,10] | 2/20 = 0,10 | 1/20 = 0,05 |
| S11† | 1/33 = 0,03 [0,01; 0,15] | 12/33 = 0,36 [0,22; 0,53] | 9/34 = 0,26 [0,15; 0,43] | 18/20 = 0,90 | 13/20 = 0,65 |
| U01 | 0/33 = 0,00 [0,00; 0,10] | 1/33 = 0,03 [0,01; 0,15] | 0/34 = 0,00 [0,00; 0,10] | 9/20 = 0,45 | 6/20 = 0,30 |
| U02 | 0/33 = 0,00 [0,00; 0,10] | 0/33 = 0,00 [0,00; 0,10] | 0/34 = 0,00 [0,00; 0,10] | 4/20 = 0,20 | 7/20 = 0,35 |
| U03 | 8/33 = 0,24 [0,13; 0,41] | 6/33 = 0,18 [0,09; 0,34] | 2/34 = 0,06 [0,02; 0,19] | 6/20 = 0,30 | 7/20 = 0,35 |
| U04‡ | 9/33 = 0,27 [0,15; 0,44] | 13/33 = 0,39 [0,25; 0,56] | 2/34 = 0,06 [0,02; 0,19] | 19/20 = 0,95 | 17/20 = 0,85 |

Đọc bảng một cách mô tả. (i) Hai luật dấu hiệu của "chưa tối ưu bản phát hành" gần như phổ quát ở hai kiểu prompt không có guardrail: P11 (không có Baseline Profile) 33/33 ở naive và detailed-functional, nhưng chỉ 7/34 = 0,21 ở guardrailed; người viết cũng gần như phổ quát (20/20 ở cả hai tầng). (ii) S06 (allowBackup) là luật S phổ biến nhất ở AI: 29/33 = 0,88 ở naive, 24/33 = 0,73 ở detailed-functional, 10/34 = 0,29 ở guardrailed, so với 0,55 và 0,40 của người viết; S06 là luật bị giữ lại. (iii) S01 (token hoặc PII lưu không mã hóa) có ở 8/33 và 11/33, và 0/34 ở guardrailed, so với 0,10 và 0,15 của người viết. (iv) Các luật hiệu năng danh sách và ổn định (P01, P03, P04) có ở AI với tỷ lệ thấp hơn so với ứng dụng do người viết (ví dụ P01 0,18 và 0,24 so với 0,70 và 0,60; P03 0,15 và 0,03 so với 0,75 và 0,80), và U04 (màu hoặc cỡ chữ cứng) 0,27 và 0,39 so với 0,95 và 0,85; nhưng các ứng dụng do người viết lớn hơn nhiều, nên đây là so sánh theo "có ít nhất một", chưa chuẩn hóa theo cỡ (C3 làm việc đó, Mục 5.2.5). (v) P08 (hiệu ứng trang trí trong danh sách) có ở 8/33 = 0,24 ở naive nhưng 0/33 ở detailed-functional và 0/34 ở guardrailed, so với 8/20 = 0,40 và 5/20 = 0,25 của người viết.

**Bảng 5.5.** Chín luật không vào phân tích chính, hiển thị để mô tả (nhánh v1; độ chính xác của chúng ở Bảng 5.3).

| Mã luật | naive (n = 33) | detailed-functional (n = 33) | guardrailed (n = 34) | người viết trước 2022 (n = 20) | người viết 2024 trở đi (n = 20) |
|---|---|---|---|---|---|
| P05‡ | 0/33 = 0,00 [0,00; 0,10] | 0/33 = 0,00 [0,00; 0,10] | 0/34 = 0,00 [0,00; 0,10] | 1/20 = 0,05 | 5/20 = 0,25 |
| P06 | 0/33 = 0,00 [0,00; 0,10] | 5/33 = 0,15 [0,07; 0,31] | 3/34 = 0,09 [0,03; 0,23] | 9/20 = 0,45 | 9/20 = 0,45 |
| P07 | 5/33 = 0,15 [0,07; 0,31] | 8/33 = 0,24 [0,13; 0,41] | 12/34 = 0,35 [0,21; 0,52] | 7/20 = 0,35 | 13/20 = 0,65 |
| P09 | 1/33 = 0,03 [0,01; 0,15] | 1/33 = 0,03 [0,01; 0,15] | 0/34 = 0,00 [0,00; 0,10] | 6/20 = 0,30 | 12/20 = 0,60 |
| P10 | 33/33 = 1,00 [0,90; 1,00] | 33/33 = 1,00 [0,90; 1,00] | 34/34 = 1,00 [0,90; 1,00] | 18/20 = 0,90 | 20/20 = 1,00 |
| S03 | 0/33 = 0,00 [0,00; 0,10] | 0/33 = 0,00 [0,00; 0,10] | 1/34 = 0,03 [0,01; 0,15] | 4/20 = 0,20 | 6/20 = 0,30 |
| S04 | 0/33 = 0,00 [0,00; 0,10] | 0/33 = 0,00 [0,00; 0,10] | 0/34 = 0,00 [0,00; 0,10] | 0/20 = 0,00 | 1/20 = 0,05 |
| S08 | 5/33 = 0,15 [0,07; 0,31] | 3/33 = 0,09 [0,03; 0,24] | 1/34 = 0,03 [0,01; 0,15] | 1/20 = 0,05 | 2/20 = 0,10 |
| U05‡ | 7/33 = 0,21 [0,11; 0,38] | 8/33 = 0,24 [0,13; 0,41] | 16/34 = 0,47 [0,31; 0,63] | 12/20 = 0,60 | 14/20 = 0,70 |

**Theo bộ công cụ (mô tả, không có tuyên bố "công cụ tốt nhất").** REPORT không đưa tần suất theo luật cho từng bộ công cụ; các số theo bộ công cụ có ở ba nơi: tỷ lệ build (Mục 5.1), cỡ bản nháp (dòng Kotlin trung vị từ 153 đến 264 ở codex-luna, từ 511 đến 1138 ở claude-haiku, từ 1686 đến 1880 ở agy-g38flash; Bảng 5.1) và phân tích độ nhạy theo bộ công cụ (Bảng 5.7). Cỡ bản nháp khác nhau rất nhiều giữa các bộ công cụ (trung vị từ 153 đến 1880 dòng), nên mọi so sánh theo "có ít nhất một phát hiện" lẫn với cỡ.

**Phần E1 của H3 (hiệu ứng trang trí).** P08 là luật được kiểm chứng và bị giữ lại; tần suất ở trên là phần tần suất của H3. REPORT của E1 không báo cáo phân bố mật độ hiệu ứng theo phần tử danh sách và màn hình (`metrics.csv`, cột `effect_density_item`, `effect_density_screen`), nên bài không đưa con số nào của phân bố; Hình 5.5 vẽ trực tiếp các giá trị từ `metrics.csv` ở dạng mô tả, không có thống kê.

**Các mẫu nổi lên (open coding).** REPORT liệt kê các mã không có sẵn trong danh mục. Ở nhiều lần chạy nhất: `invented-base-url` (5 lần chạy; brief naive không cho URL), `force-unwrap-in-compose` (4), `broad-catch-in-suspend` (3; bắt `Exception` mà không ném lại `CancellationException`), `dense-minified-source` (3; nhiều câu lệnh trên một dòng, dòng dài đến khoảng 1300 ký tự), `exception-swallowed-to-default` (2), `overbroad-keep-rule` (2), `handwritten-http-json` (2), `silent-wrong-default` (2; ví dụ `daysLeft()` trả về 0 khi lỗi phân tích) và `allowbackup-true-with-session` (2, trong danh mục là S06). Các mã còn lại xuất hiện ở một lần chạy, gồm các lỗi cấu trúc (`wrong-processor-artifact`: `ksp(room.runtime)` thay cho `room-compiler`; `mock-backend-enabled-by-default`: bộ chặn giả mạo được bật mặc định trong OkHttp client của bản phát hành; `release-signed-with-debug-key`), lỗi "ảo" (`hallucinated-package-path`, `hallucinated-view-property`) và cả mã dương tính (`cancellation-rethrown`, `narrow-catch-io-json`). Đây là kết quả khám phá do một người mã hóa; không có tần suất nào được diễn giải.

**Hình 5.4.** Tần suất theo luật và kiểu prompt (bộ v1, Wilson 95%); nhóm dưới là luật chưa kiểm chứng, chỉ mô tả. Số liệu vẽ: `figures/out/F06-prevalence.data.csv`.

![F06](figures/out/F06-prevalence.svg)

**Hình 5.5.** Mật độ hiệu ứng trang trí theo mục danh sách và theo màn hình (phần E1 của H3), từ `metrics.csv`; mô tả. Số liệu vẽ: `figures/out/F11-decorative-density.data.csv`.

![F11](figures/out/F11-decorative-density.svg)

#### 5.2.3. C1 và C2: guardrail có giảm tỷ lệ phát hiện nhóm S và độ rộng không?

Bảng 5.6 chép nhánh chính v1 trên 20 luật đã kiểm chứng (100 lần chạy hợp lệ; `e1-corpus/results/analysis/primary-v1/summary.md`).

**Bảng 5.6.** C1 và C2: ba kiểu prompt, nhánh chính v1, 20 luật.

| kiểu prompt | n hợp lệ | lần chạy có ít nhất một phát hiện S | tỷ lệ | Wilson 95% | số mã luật khác nhau mỗi lần (trung bình) | trung vị | bootstrap 95% (trung bình) |
|---|---|---|---|---|---|---|---|
| naive | 33 | 31 | 0,939 | [0,804; 0,983] | 5,85 | 4,0 | [4,91; 6,85] |
| detailed-functional | 33 | 29 | 0,879 | [0,727; 0,952] | 5,70 | 6,0 | [5,09; 6,33] |
| guardrailed | 34 | 17 | 0,500 | [0,341; 0,659] | 2,82 | 3,0 | [2,41; 3,26] |

**C1** (naive trừ guardrailed, 9 tầng brief nhân bộ công cụ): hiệu rủi ro **+0,439**, p hoán vị phân tầng chưa hiệu chỉnh = 0,00004 (100.000 hoán vị, hạt giống 20261121). **C2** (cùng thiết kế): hiệu số mã luật khác nhau mỗi lần chạy **+3,025**, p = 0,00001. Quy tắc đã đăng ký: C1 được ủng hộ khi p hiệu chỉnh Holm dưới 0,05 và hiệu rủi ro từ 0,15; C2 khi p hiệu chỉnh dưới 0,05 và chênh từ 2,0. Cả hai độ lớn vượt ngưỡng (0,439 so với 0,15, và cũng vượt mức 0,25 của dải độ nhạy; 3,025 so với 2,0), và p thô của C1, C2 đều dưới 0,01, ngưỡng đầu tiên của Holm với m = 5. Sau Holm (Bảng 5.27, Mục 5.7) p hiệu chỉnh của C1 là 0,00016 và của C2 là 0,00005; kết luận không đổi. Phân tích trên toàn bộ 29 luật cho cùng hướng (C1 +0,470, p = 0,00001; C2 +2,600, p = 0,00002).

**Độ nhạy của A6 và các hạn chế phải đọc cùng C1 và C2.**

**Bảng 5.7.** Phân tích độ nhạy (A6; không cái nào thay một điểm cuối xác nhận), nhánh chính v1.

| biến thể | n hợp lệ | C1 hiệu (p) | C2 hiệu (p) | C3 delta Cliff (p) |
|---|---|---|---|---|
| (a) không lần chạy nào có gọi web | 100 | +0,439 (p = 0,0000) | +3,025 (p = 0,0000) | +0,582 (p = 0,0001) |
| (c) chỉ các lần chạy build được | 42 | +0,000 (p = 1,0000) | +3,829 (p = 0,0028) | +0,678 (p = 0,0000) |
| (b) chỉ agy-g38flash | 28 | +0,200 (p = 0,4286) | +7,244 (p = 0,0001) | +0,456 (p = 0,0172) |
| (b) chỉ claude-haiku | 36 | +0,583 (p = 0,0066) | +2,000 (p = 0,0004) | +0,340 (p = 0,0559) |
| (b) chỉ codex-luna | 36 | +0,500 (p = 0,0243) | +0,917 (p = 0,0518) | +0,919 (p = 0,0000) |
| (b) bỏ agy-g38flash | 72 | +0,542 (p = 0,0001) | +1,458 (p = 0,0000) | +0,629 (p = 0,0000) |
| (b) bỏ claude-haiku | 64 | +0,361 (p = 0,0060) | +3,632 (p = 0,0000) | +0,720 (p = 0,0000) |
| (b) bỏ codex-luna | 64 | +0,407 (p = 0,0017) | +4,216 (p = 0,0000) | +0,389 (p = 0,0141) |

(Các p ghi 0,0000 là giá trị làm tròn đến bốn chữ số của REPORT.) Ba điều **phải nêu cùng C1 và C2**, ở cùng mức nổi bật với kết quả chính.

1. **Trong các ứng dụng build được, C1 bằng 0.** Ở 42 lần chạy build được (độ nhạy c), hiệu rủi ro C1 là +0,000 (p = 1,0000) ở cả nhánh chính và nhánh v2: guardrail **không** làm giảm việc có hay không có phát hiện S ở các ứng dụng chạy được. Hiệu ứng đếm và mật độ vẫn còn ở nhóm này (C2 +3,829, p = 0,0028; C3 delta +0,678), tức là guardrail giảm **bao nhiêu** vấn đề S khác nhau xuất hiện, không giảm việc có hay không. Kết luận C1 vì thế đúng cho "mọi bản nháp ở mức mã nguồn", không cho "ứng dụng chạy được".
2. **Không bộ công cụ nào đơn lẻ kéo kết quả gộp.** Khi bỏ lần lượt từng bộ, ba điểm cuối giữ hướng; p lớn nhất của ba phép bỏ-một-bộ là 0,0060 cho C1, 0,0000 cho C2 và 0,0141 cho C3. Nhưng khi chỉ lấy từng bộ, hiệu của C1 ở agy-g38flash là +0,200 (p = 0,4286), của C2 ở codex-luna là +0,917 (p = 0,0518) và delta C3 ở claude-haiku là +0,340 (p = 0,0559): các phân nhóm nhỏ (28 đến 36 lần chạy) không đủ lực kiểm định để khẳng định từng bộ.
3. **Cỡ mẫu 33, 33, 34 dưới mục tiêu 36** (Mục 5.1) và agy mất bảy ô theo A11.

Ở nhánh v2 (độ nhạy), C1 giữ nguyên +0,439 (p = 0,00004), C2 là +3,964 (p = 0,00001) và C3 delta +0,567 (p = 0,00013); độ nhạy build được cho +0,000 (C1, p = 1,0000), +4,757 (C2, p = 0,0010) và +0,659 (C3, p = 0,0001).

**Hai so sánh khám phá giữa các kiểu prompt.** REPORT không đưa ước lượng chênh và khoảng cho hai so sánh này; Bảng 5.6 cho tỷ lệ và khoảng Wilson của từng kiểu. Nội dung chức năng chi tiết không có guardrail **không** làm giảm tỷ lệ phát hiện S: 0,879 [0,727; 0,952] so với 0,939 [0,804; 0,983] của naive (khoảng chồng nhau), và số mã luật khác nhau mỗi lần chạy là 5,70 so với 5,85. Guardrail là thứ làm khác: 0,500 [0,341; 0,659] so với 0,879 [0,727; 0,952] của detailed-functional (khoảng không chồng nhau), 2,82 so với 5,70. Theo hướng này, lỗi không do yêu cầu chức năng chưa đầy đủ mà do mặc định của quá trình sinh, trong phạm vi một tệp guardrail về chất lượng làm thay đổi được; đây là kết quả mô tả và chưa kiểm tra cơ chế.

**Hình 5.6.** C1: tỷ lệ lần chạy có phát hiện nhóm S, hiệu theo tầng và độ nhạy. Số liệu vẽ: `figures/out/F07-c1.data.csv`.

![F07](figures/out/F07-c1.svg)

**Hình 5.7.** C2: số mã luật khác nhau mỗi lần chạy, hiệu theo tầng và độ nhạy. Số liệu vẽ: `figures/out/F08-c2.data.csv`.

![F08](figures/out/F08-c2.svg)

#### 5.2.4. G3: guardrail giúp luật được nêu hay cả luật bị giữ lại?

G3 là phân tích thứ cấp có đăng ký (nhánh chính v1, 20 luật): hiệu ứng của guardrail trên tập được chỉ dẫn, trên tập bị giữ lại đã qua cổng, và chênh của hai hiệu ứng. Tập bị giữ lại trong G3 là P03, P08, S06, D02, U04, vì P05 và U05 trượt cổng (A1). Naive trừ guardrailed: Δ chỉ dẫn +1,74; Δ bị giữ lại +1,10; chênh +0,63, bootstrap 95% [0,15; 1,14]. Detailed-functional trừ guardrailed: +2,01; +0,71; chênh +1,30, [0,75; 1,84]. Theo quy tắc (ủng hộ nếu cận dưới của khoảng chênh trên 0), cả hai chênh được ủng hộ: guardrail giảm số phát hiện trên luật được nêu nhiều hơn trên luật bị giữ lại, **nhưng luật bị giữ lại cũng giảm** (+1,10 và +0,71 phát hiện mỗi lần chạy). Theo quy ước đã đăng ký, phần giảm ở luật bị giữ lại được ghi là "khái quát hóa hoặc ngẫu nhiên" và không khẳng định cơ chế; ví dụ rõ nhất là S06 (0,88 xuống 0,29) và P08 (0,24 xuống 0,00), cả hai đều là luật bị giữ lại (P08 cũng bằng 0/33 ở detailed-functional, không có guardrail). Phân tích 29 luật cho +0,72 [0,06; 1,39] và +1,54 [0,95; 2,14]; nhánh v2 cho +1,57 [1,06; 2,09] và +2,33 [1,79; 2,87].

**Hình 5.8.** G3: thay đổi tần suất theo luật (được nêu và bị giữ lại) và tương phản đã đăng ký. Số liệu vẽ: `figures/out/F09-g3.data.csv`.

![F09](figures/out/F09-g3.svg)

#### 5.2.5. C3: so với ứng dụng do người viết

**Kho đối chứng.** Hai mươi ứng dụng có commit đầu tiên đến hết 2022-11-30 (trước trợ lý, xác nhận) và 20 ứng dụng từ 2024 trở đi (so sánh thứ hai); rút có hạt giống 20261121 từ tìm kiếm GitHub và sàng lọc ứng dụng Kotlin Compose cỡ 2000 đến 50000 dòng (`human_corpus*.csv`). Sau khi quét, các tệp không phải mã nguồn của bản sao (ảnh, phông, tài liệu, nhị phân; 95.604 xuống 14.221 tệp) bị xóa vì ổ làm việc NTFS hết bản ghi tệp; mọi bộ quét chỉ đọc `.kt`, `.xml`, Gradle, TOML, `.pro`, `.properties`, `baseline-prof.txt` và `stability_config.conf`, nên quét lại cho cùng kết quả. REPORT không đưa tổng dòng hay năm tạo của kho; ứng dụng tham chiếu `android/app` cũng không được REPORT báo cáo, nên không có điểm tham chiếu ở bài này.

**C3 (xác nhận).** Phát hiện nhóm S trên 1000 dòng Kotlin, AI không guardrail (n = 66) trung vị **2,37** so với tầng trước trợ lý (n = 20) trung vị **1,02**; Mann-Whitney p = 0,00009, delta Cliff **+0,582** [Cliff1993]. Quy tắc: ủng hộ khi p hiệu chỉnh dưới 0,05 và delta từ 0,33 (AI cao hơn); delta vượt ngưỡng; p hiệu chỉnh Holm là 0,00027 (Bảng 5.27, Mục 5.7). So sánh thứ hai, không xác nhận (tầng 2024 trở đi, n = 20): trung vị 0,24, p = 0,00000 (làm tròn), delta +0,742. Dòng độ nhạy theo ký tự khác khoảng trắng, vì độ dài dòng của các bản nháp khác nhau (một bộ công cụ nhồi khoảng 130 ký tự mỗi dòng): trên 100k ký tự khác trắng, AI 7,83 so với 3,47 của người trước 2022, p = 0,00018, delta +0,556; so với tầng 2024 trở đi trung vị 0,87, delta +0,758. Trên toàn bộ 29 luật: 2,72 so với 1,02, p = 0,00001, delta +0,647. Giữa các bộ công cụ, delta C3 của claude-haiku là +0,340 (p = 0,0559, Bảng 5.7), ở ngưỡng 0,33.

Hai hạn chế đặc thù của C3: tầng người viết gồm ứng dụng phổ biến, lớn, được bảo trì, không giống một bản nháp đầu; tính theo 1000 dòng giảm nhưng không xóa chênh lệch cỡ. Tầng 2024 trở đi có thể đã chứa mã do trợ lý viết, nên chỉ là so sánh thứ hai.

**Hình 5.9.** C3: phát hiện nhóm S trên 1000 dòng Kotlin, AI không guardrail so với hai tầng ứng dụng do người viết, và độ nhạy theo ký tự khác khoảng trắng. Số liệu vẽ: `figures/out/F10-c3.data.csv`.

![F10](figures/out/F10-c3.svg)

**Độ nhạy sau: sửa tra cứu phụ thuộc (không xác nhận).** Kiểm tra H4 sau khi đã thấy số liệu theo nhánh phát hiện một lỗi của bộ tra cứu: `depcheck` dừng ở kho khai báo đầu tiên có trả lời, và dấu plugin KSP của `google()` đã cũ (25 phiên bản, mới nhất 1.5.30-1.0.0, năm 2021), nên năm phiên bản KSP có thật (1.9.22-1.0.17, 1.9.23-1.0.20, 1.9.24-1.0.20, 2.0.20-1.0.25, 2.0.21-1.0.28) bị coi là không tồn tại: 21 phát hiện S11 sai ở 21 lần chạy của AI và 16 ở ứng dụng do người viết. Gradle tìm tiếp ở kho kế tiếp nên tra cứu đã sửa lấy hợp các kho khai báo; chỉ các hàng đó đổi (lần chạy: 21 đổi lớp, 21 giá trị bản ổn định mới nhất được điền, OSV không đổi; người viết: 16 đổi lớp), và các tệp của lần quét đầu không bị sửa. Đây là **phân tích độ nhạy post hoc, không xác nhận**: nó được làm sau khi đã thấy kết quả theo nhánh, nên là một bậc tự do của người nghiên cứu, và nhánh chính vẫn là kết quả đã chạy theo quy trình đã đăng ký (đổi quy trình sau khi biết kết quả sẽ là một đường rẽ). Lỗi này tác động hai chiều: nó làm tăng tỷ lệ có phát hiện nhóm S của nhánh guardrailed (bất lợi cho C1) và mật độ nhóm S của cả AI lẫn người viết (C3). Nhánh v1 sau sửa: C1 +0,645 (guardrailed 10/34 thay cho 17/34, naive vẫn 31/33), C2 +3,259, C3 trung vị AI 2,33 so với người 0,83 trên 1000 dòng, delta Cliff +0,513, p thô 0,00055 (so với +0,439, +3,025, 2,37 so với 1,02 và +0,582 ở nhánh chính). Không kết luận nào trong C1 đến C3 đổi: ở cả bốn cột (v1 và v2, lần quét đầu và đã sửa) mọi điểm cuối giữ dấu và có p dưới 0,01, ngưỡng Holm của phép kiểm yếu nhất trong năm; vì tầng người viết cũng giảm mật độ, cột đã sửa là cột nên đọc cho so sánh với người viết (REPORT E1 mục 3c, 3e và 7).

#### 5.2.6. H1, H4, H5, H6: các tần suất mô tả

Theo quy tắc dải PB: ủng hộ nếu cận dưới Wilson từ 0,50, bác bỏ nếu cận trên dưới 0,20 (66 lần chạy không guardrail):

| giả thuyết | k/n | tỷ lệ | Wilson 95% | kết luận theo PB |
|---|---|---|---|---|
| H1 (D01 hoặc D02) | 66/66 | 1,000 | [0,945; 1,000] | ủng hộ |
| H5 (U01 hoặc U05) | 1/66 | 0,015 | [0,003; 0,081] | bác bỏ |
| H6 (S05) | 10/66 | 0,152 | [0,084; 0,257] | không kết luận được |

**H1.** Mọi lần chạy không guardrail đều có D01 hoặc D02 đã kiểm chứng. D02 (phụ thuộc cũ từ một phiên bản chính hoặc hơn 12 tháng so với bản ổn định mới nhất tại ngày sinh) có ở 100% mọi nhánh, nhưng cũng ở 17/20 và 18/20 ứng dụng do người viết (Bảng 5.4): sự lỗi thời của phụ thuộc không đặc trưng cho AI. D01 (API bị phản đối) ở 0,76, 0,67 và 0,74 các nhánh AI, 1,00 và 0,70 ở người viết.

**H4.** Quy tắc đã đăng ký (giao thức §3): ủng hộ nếu có ít nhất một thể hiện tọa độ thuộc lớp `hallucinated-artifact` (group:artifact không có ở mọi kho mà dự án khai báo, tại ngày sinh); bác bỏ nếu 0 `hallucinated-artifact` trong ít nhất 300 thể hiện; một artifact có thật với phiên bản không tồn tại là `hallucinated-version` và được đếm riêng. Kết quả (REPORT E1, mục H4): 2105 thể hiện tọa độ ở 100 lần chạy hợp lệ (bom 27, lib 1532, plugin 546), phân giải với siêu dữ liệu của các kho khai báo (plugin: các kho `pluginManagement` mặc định), không có lỗi tra cứu, ngày tra cứu 2026-11-23: **0 `hallucinated-artifact`**, cả ở lần quét đầu lẫn sau khi sửa tra cứu. Vì có 0 trong 2105 thể hiện (từ 300 trở lên), **H4 bị bác bỏ** theo quy tắc; cận trên 95% theo quy tắc ba là 0,14% [Hanley1983]. `hallucinated-version` thì có: 22 ở lần quét đầu (21 plugin KSP, 1 plugin khác), còn 1 sau khi sửa tra cứu (đoạn "Độ nhạy sau" ở trên): 21 là dương tính giả, và thể hiện thật duy nhất là plugin biên dịch Compose phiên bản 1.9.24 của codex-luna (plugin này bắt đầu từ 2.0.0). Đọc hẹp: kết luận chỉ nói về lớp `hallucinated-artifact` ở tệp build của các bản nháp đầu trong ba bộ công cụ và một miền, tại một ngày tra cứu; không nói rằng không có phụ thuộc có lỗ hổng hay lỗi thời (H1 và S11 đo riêng). Vì lỗi tra cứu, tỷ lệ S11 ở Bảng 5.4 (1/33, 12/33, 9/34; 18/20 và 13/20 của người viết) là của lần quét đầu và chứa phát hiện sai (21 ở các lần chạy của AI, 16 ở ứng dụng do người viết, REPORT E1 mục 7): không dùng chúng để so sánh AI với người. Các mã "ảo" của open coding (đường dẫn gói, thuộc tính View) là lỗi tham chiếu, không phải tọa độ Gradle.

**H5.** 1/66: tỷ lệ bác bỏ. Chú ý: U05 không đạt cổng nên không có trong phân tích chính, vì vậy số 1/66 khớp với riêng U01 (0/33 ở naive, 1/33 ở detailed-functional; Bảng 5.4). U05 chưa kiểm chứng có ở 7/33 và 8/33 lần chạy không guardrail (Bảng 5.5), nên tỷ lệ của H5 sẽ cao hơn nhiều nếu tính nó; vì vậy "bác bỏ" là phát biểu về U01, không về trạng thái loading, empty, error.

**H6.** 10/66 (S05 có 4/33 và 6/33 ở naive và detailed-functional): không kết luận được; khoảng [0,084; 0,257] vắt qua ngưỡng 0,20.

**Hình 5.10.** H1: tỷ lệ lần chạy có D01 hoặc D02; H4: lớp của các thể hiện tọa độ sau khi sửa tra cứu (`results/depfix/coordinates_fixed.csv`), cùng kết luận của quy tắc bác bỏ. Số liệu vẽ: `figures/out/F12-h1-h4.data.csv`.

![F12](figures/out/F12-h1-h4.svg)

### 5.3. E2: chi phí A/B của các vấn đề

Mọi số trong mục này được chép từ `e2-bench/results/summary.csv` và `c5_confirmatory.csv`; các bảng của REPORT E2 đã được đối chiếu với `summary.csv` và trùng ở mọi hàng. **Phạm vi của mục này.** Các nhân tố E2 chia hai loại. P10 (R8) và P11 (Baseline Profile) là **kỹ thuật nền**, không lấy từ mã E1: A và B là cùng một mã với hai cấu hình build, và được báo cáo ở Mục 5.3.1 và 5.3.2. Các nhân tố lấy từ mã E1 (P08, P06, P01 cùng P09, P13, P03 cùng P04, kể cả điểm cuối xác nhận C5) dùng biến thể A chọn theo quy tắc A4, A5, A10 và A14, và được báo cáo ở Mục 5.3.3. Lần chạy E2 đầu tiên, với biến thể A viết tay, là **thí điểm** (A3) và không được dùng cho C5 hay cho bảng nào ở đây. Mọi số đo mili giây chạy trên emulator có GPU phần mềm (Mục 5.1) và chỉ là bằng chứng tương đối; P08 chạy trên một đường GPU khác các nhân tố còn lại (A14, Mục 5.3.3). Lần lặp bị loại theo A7 (bộ nhớ khả dụng dưới 1024 MB ở đầu lần lặp, hoặc tiến trình qemu có lỗi trang chính trong lần lặp); bảng chính dùng các lần lặp được giữ (cột n).

#### 5.3.1. P10: R8 (nền)

A là build `noR8`, B là build `release` bật R8 minify và resource shrinking, cùng mã; hai build khác nhau ở hai công tắc đó và ở application id, nên được cài cạnh nhau [R8]. Hành trình đo: khởi động nguội cộng 3 lần vuốt, `CompilationMode.None`. Bảng 5.9 chép `e2-bench/results/summary.csv` (Δ% = (B − A) / A; delta Cliff dương nghĩa là A lớn hơn; các hàng `frameOverrunMs` cắt ngang 0 nên cột thay đổi là hiệu tuyệt đối, ms).

**Bảng 5.9.** E2, P10 (A: không R8; B: R8 minify và shrinkResources).

| chỉ số | đơn vị | A: trung vị (IQR) | B: trung vị (IQR) | thay đổi (B so với A) | khoảng bootstrap 95% | delta Cliff | n |
|---|---|---|---|---|---|---|---|
| apk_bytes | bytes | 2,474e+07 (0) | 2,384e+06 (0) | −90,4% | — | — | 1 |
| dex_bytes | bytes | 2,41e+07 (0) | 2,17e+06 (0) | −91% | — | — | 1 |
| frameCount | frames | 119 (1) | 120 (2) | 0,84% | [−0,418; 1,68] | −0,332 | 14 |
| frameDurationCpuMs p50 | ms | 4,531 (0,2096) | 3,858 (0,1381) | −14,8% | [−17,4; −12,2] | 1 | 14 |
| frameDurationCpuMs p90 | ms | 6,029 (0,3949) | 4,894 (0,209) | −18,8% | [−23,1; −16,6] | 0,908 | 14 |
| frameDurationCpuMs p99 | ms | 40,22 (21,83) | 22,51 (28,81) | −44% | [−57,8; 24,6] | 0,367 | 14 |
| frameOverrunMs p50 | ms | −11,17 (0,1419) | −11,94 (0,08997) | −0,7735 ms (tuyệt đối) | [−0,8586; −0,6695] | 1 | 14 |
| frameOverrunMs p90 | ms | −9,05 (0,5368) | −10,25 (0,2427) | −1,198 ms (tuyệt đối) | [−1,487; −0,8866] | 0,99 | 14 |
| frameOverrunMs p99 | ms | 283,9 (35,99) | 264,8 (24,89) | −19,14 ms (tuyệt đối) | [−41,97; −0,1753] | 0,531 | 14 |
| resources_bytes | bytes | 4,958e+05 (0) | 9,096e+04 (0) | −81,7% | — | — | 1 |
| slowFrames_pct | % | 2,532 (0,6204) | 2,08 (0,8437) | −17,8% | [−49,2; −0,417] | 0,571 | 14 |
| timeToFullDisplayMs | ms | 545,6 (55,92) | 494,5 (30,26) | −9,35% | [−14,6; −3,58] | 0,743 | 14 |
| timeToInitialDisplayMs | ms | 505,6 (28,25) | 461,6 (20,27) | −8,7% | [−12,4; −4,97] | 0,771 | 14 |

Đọc theo mô tả: kích thước là kết quả không phụ thuộc thiết bị (một giá trị xác định mỗi biến thể, n = 1): APK giảm 90,4%, dex giảm 91% và tài nguyên giảm 81,7%. Khởi động giảm nhỏ nhưng khoảng bootstrap không chứa 0: `timeToInitialDisplayMs` −8,7% [−12,4; −4,97] và `timeToFullDisplayMs` −9,35% [−14,6; −3,58]. Thời gian khung hình CPU giảm ở p50 (−14,8%) và p90 (−18,8% [−23,1; −16,6], delta Cliff 0,908); p99 giảm 44% nhưng khoảng [−57,8; 24,6] chứa 0, nên không phân biệt được với nhiễu ở n = 14. Không diễn giải p-value ngoài C5.

#### 5.3.2. P11: Baseline Profile (nền)

A là `CompilationMode.None()` (đặt lại profile, ứng dụng được JIT khi chạy), B là `CompilationMode.Partial(BaselineProfileMode.Require)`; cùng APK `release`; profile sinh trên emulator từ các hành trình của lab nên chỉ phủ ứng dụng lab [Baseline-Overview].

**Bảng 5.10.** E2, P11 (A: không có Baseline Profile; B: có Baseline Profile).

| chỉ số | đơn vị | A: trung vị (IQR) | B: trung vị (IQR) | thay đổi (B so với A) | khoảng bootstrap 95% | delta Cliff | n |
|---|---|---|---|---|---|---|---|
| frameCount | frames | 120 (2,5) | 119,5 (2) | −0,417% | [−1,24; 2,13] | −0,167 | 12 |
| frameDurationCpuMs p50 | ms | 3,909 (0,09628) | 3,37 (0,1577) | −13,8% | [−17,2; −11,6] | 1 | 12 |
| frameDurationCpuMs p90 | ms | 4,875 (0,2987) | 4,512 (0,2496) | −7,45% | [−13,4; −3,99] | 0,931 | 12 |
| frameDurationCpuMs p99 | ms | 15,96 (31,18) | 17,11 (8,556) | 7,2% | [−62,2; 86,9] | −0,0417 | 12 |
| frameOverrunMs p50 | ms | −11,92 (0,2058) | −12,36 (0,08282) | −0,4394 ms (tuyệt đối) | [−0,5962; −0,352] | 0,986 | 12 |
| frameOverrunMs p90 | ms | −10,05 (0,638) | −11,16 (0,2606) | −1,107 ms (tuyệt đối) | [−1,365; −0,5749] | 0,986 | 12 |
| frameOverrunMs p99 | ms | 252,1 (10,76) | 232,8 (15,16) | −19,31 ms (tuyệt đối) | [−30,78; −11,46] | 0,708 | 12 |
| slowFrames_pct | % | 1,281 (0,8334) | 1,653 (0,8298) | 29% | [−49,6; 96,7] | 0,0486 | 12 |
| timeToFullDisplayMs | ms | 481,3 (23,08) | 443,4 (32,13) | −7,89% | [−12,1; −2,81] | 0,733 | 13 |
| timeToInitialDisplayMs | ms | 462 (22,43) | 434,8 (18,76) | −5,88% | [−9,87; −2,67] | 0,692 | 13 |

Đọc theo mô tả: khởi động giảm `timeToInitialDisplayMs` −5,88% [−9,87; −2,67] (n = 13) và `timeToFullDisplayMs` −7,89% [−12,1; −2,81]; `frameDurationCpuMs` p50 −13,8% [−17,2; −11,6] và p90 −7,45% [−13,4; −3,99]. Các chỉ số p99 và `slowFrames_pct` (+29%, khoảng [−49,6; 96,7]) có khoảng chứa 0. Hiệu ứng trên ứng dụng một màn hình là khiêm tốn; ứng dụng có đường khởi động dài hơn có thể được lợi nhiều hơn, nhưng điều đó không được đo ở đây.

#### 5.3.3. Các nhân tố lấy từ mã E1 và điểm cuối C5

**Biến thể A là mã của chiến dịch E1.** Theo A4, với mỗi nhân tố, A là ứng viên đầu tiên theo thời điểm bắt đầu lần chạy, trong các lần chạy không phải thí điểm có phát hiện cho luật đó, mà cấu trúc bị phát hiện đưa được vào ứng dụng lab chỉ bằng thay đổi cơ học (gói, import, nguồn dữ liệu, đầu đo; cấu trúc bị phát hiện không đổi; các diff nằm ở `e2-bench/ports/<nhân tố>.diff`). Không nhân tố nào dùng biến thể viết tay, nên không bảng hay hình nào ở đây mang nhãn "author-constructed", và C5 chạy trên mã đã sinh (A) chứ không dùng phương án thay thế của A4 và A5. Hai lần chạy khám phá, độ nhạy với năm biến thể A viết tay của lần thí điểm và quét số lượng lượt mượn của P01 cùng P09, chưa được thực hiện, nên bài không có số liệu nào của chúng. B là bản tinh chỉnh do người thực hiện E2 viết (sinh từ A khi bản sửa cục bộ được, viết tay khi không), không phải bản sửa có trong kho. Ứng dụng lab không có Room, Paging, ViewModel, điều hướng và mạng nên các bản port không mang chúng: các nhân tố đo cấu trúc bị phát hiện, không đo ứng dụng nguồn. P09 và P03 cùng P04 dùng chung một tệp E1; kịch bản của P03 truyền một danh sách mới bằng nhau ở mỗi nhịp vì phát hiện P03 phụ thuộc chuỗi công cụ (strong skipping, Mục 2.2.3).

**Bảng 5.11.** E2, biến thể A (mã của chiến dịch E1, theo `e2-bench/ports/README.md`) và B (bản tinh chỉnh) theo nhân tố.

| nhân tố | hạng | lần chạy E1 | tệp và dòng (phát hiện) | B (tinh chỉnh) |
|---|---|---|---|---|
| P08 | 1 | agy-g38flash-B1-naive-r1 | `ui/components/BookCover.kt:216` (gradient, đổ bóng), nơi gọi `BookCard.kt` | ô bìa phẳng cùng khe |
| P06 | 1 | codex-luna-B3-detailed-functional-r1 | `MainActivity.kt:39` (`decodeStream` không `inSampleSize`) | Coil `AsyncImage` cùng khe 150x210 dp, bộ nhớ đệm bộ nhớ và đĩa |
| P01 (luật P01) | 1 | claude-haiku-B3-detailed-functional-r1 | `ui/history/HistoryScreen.kt:105` (`items` không key) | cùng danh sách có `key` và `contentType` |
| P09 | 3 (tệp P09 đầu tiên; hạng 1 và 2 là tệp P01) | agy-g38flash-B3-naive-r1 | `ui/components/BookDetailSheet.kt:300` (`Column` + `verticalScroll` + `forEach`) | `LazyColumn` có `key` và `contentType` |
| P13 | 1 | claude-haiku-B1-detailed-functional-r1 | `data/local/TitleDao.kt:31` (`LIKE` không chỉ mục) | SQLite FTS4, so khớp tiền tố từ khóa |
| P03 và P04 | 1 (cả hai luật trong một tệp) | agy-g38flash-B3-naive-r1 | cùng tệp với P09, dòng 58, 65, 67 | `LoanList` ổn định và `remember` |

**Sai lệch rig: P08 đo trên đường GPU khác (A14).** Ứng viên P08 hạng 1 (agy-g38flash-B1-naive-r1, `BookCover.kt`) chạy được khi đưa vào lab, nhưng tiến trình emulator (qemu) chết với SIGSEGV ở các bản không gỡ lỗi được, thường ở lần khởi động thứ hai đến thứ năm và không bao giờ ở lần đầu sau khi bật máy; bản gỡ lỗi được và B không chết. Theo A10, nếu ứng dụng gốc chưa sửa của kho không sập thì sập là lỗi của bản port: ứng dụng gốc không sập trong 21 chu kỳ khởi động (3 + 4 + 4 ở bản gỡ lỗi được, 5 + 5 ở bản đã xóa cờ gỡ lỗi) và trong 4 chu kỳ cuộn, và vẽ cùng bìa. Vì vậy hạng 1 được giữ (A14); hạng 2 không dùng cho C5. Dấu vết từ gdb đặt chỗ sập ở một luồng kết xuất của máy chủ đang chạy một thủ tục điểm ảnh do SwiftShader biên dịch JIT (đọc SIMD ngoài biên), cùng lệnh gây lỗi ở hai lần sập (`qemu-crash-gdb-port2.txt`), không nằm trong mã có thể cô lập ở bản port. Đã loại trừ làm nguyên nhân: R8, targetSdk, cỡ khe bìa, số thẻ, chế độ biên dịch, bộ đệm shader, cờ gỡ lỗi riêng lẻ. Cùng AVD với `-gpu swangle_indirect` (ANGLE trên SwiftShader Vulkan) không sập (8 trên 8 lần khởi động, rồi toàn bộ cửa sổ đo), nên **P08 A và B cùng được đo trên `swangle_indirect`, trong cùng cửa sổ và cùng AVD; mọi nhân tố E2 khác giữ `swiftshader_indirect`**. Hệ quả: thời gian khung tuyệt đối của P08 không so được với số tuyệt đối của nhân tố khác, chỉ đối chiếu A/B trong nhân tố có nghĩa, và C5 chỉ dựa trên đối chiếu đó. Quyết định dựa trên một lần sập tiến trình và dấu vết trình gỡ lỗi, trước mọi số đo P08 (A14). Bài **không** khẳng định rằng cấu trúc P08 của mã sinh ra làm sập GPU phần mềm: bản gốc không sập, và nguyên nhân khác biệt giữa bản gốc và bản port chưa được cô lập (Compose BOM 2024.02 so với 2026.09 chưa được thử). Theo A10, các cấu trúc bị loại vì `rig-unmeasurable` phải được báo cáo mô tả: `ports/rejections.csv` chỉ có hàng tiêu đề, không ứng viên nào bị loại.

**Kiểm soát thăm dò: bộ dựng hình đổi độ lớn đối chiếu khung hình (không thuộc C5, không có p hiệu chỉnh).** Sửa đổi A14 ghi rằng chưa nhánh thực nghiệm nào kiểm tra việc đổi bộ dựng hình có làm đổi độ lớn của đối chiếu A/B. Lần kiểm soát này kiểm tra điều đó ở một nhân tố khác, không phải P08 (A của P08 làm sập đường mặc định nên không đo được ở đó): cặp P01+P09 được chạy lại trên `swangle_indirect` (mã nhân tố `P01+P09@swangle` trong `summary.csv`, pha primary và frames, 12 đến 14 lần giữ mỗi biến thể) cạnh lần chạy chính trên `swiftshader_indirect`. Delta% ở đây là (B − A) / A, khác với C5 là (A − B) / B, nên hai đại lượng không đặt cạnh nhau. Số đo (trung vị A → B):

- `st.rc.row`: 2.000 → 31 (−98,5%) trên swiftshader_indirect; 2.000 → 30 (−98,5%) trên swangle_indirect.
- `timeToInitialDisplayMs`: 1.901 → 573,1 ms (−69,9%); 3.426 → 578,6 ms (−83,1% [−84,2; −78,2]).
- `frameDurationCpuMs_p90`: 27,14 → 5,912 ms (−78,2% [−79,1; −76,9]); 45,07 → 30,53 ms (−32,3% [−46,8; −11,4]).
- `frameDurationCpuMs_p99`: 34,30 → 18,21 ms (−46,9% [−62,2; −20,6]); 68,44 → 55,94 ms (−18,3% [−34,4; +7,9]).
- `slowFrames_pct`: 90,05 → 1,255% (−98,6%); 100 → 46,71% (−53,3% [−66,7; −48,8]).

Chiều của hiệu ứng và số hàng được dựng (`st.rc.row`) giống nhau trên hai đường; số đo khung hình thì không. Trên đường ANGLE mọi khung đều chậm hơn, kể cả bản B (p90 30,53 ms so với 5,912 ms), nên đối chiếu A/B co lại ở p90 (−32,3% so với −78,2%) và khoảng p99 chứa 0 trong khi ở `swiftshader_indirect` thì không. Đọc cho C5: đối chiếu P08 (+28,13%) được đo trên đường mà lần kiểm soát cho đối chiếu khung hình nhỏ hơn, nên độ lớn đó không suy ra được cho đường khác; vì P08 A không chạy được trên `swiftshader_indirect`, chiều và độ lớn của thay đổi đối với chính P08 là **không biết**. Giới hạn: một nhân tố, một phiên đo, thăm dò (không nằm trong họ Holm), và nguyên nhân (đường kết xuất ANGLE trên SwiftShader Vulkan so với SwiftShader trực tiếp) là giả thuyết, không nhánh nào tách nó ra.

**Lần lặp được chạy, giữ và loại.** Bảng 5.12 chép `REPORT` mục 4.0. Mọi lần loại là lỗi trang chính của qemu, trừ một lần không có mẫu máy chủ (một lần lặp của nhân tố con P01); không lần nào vì bộ nhớ khả dụng dưới 1024 MB, và không nhóm nào bị giới hạn bởi máy. Các nhân tố lấy từ mã E1, kể cả nhân tố con P01: 282 lần lặp chạy, 243 giữ, 39 loại; P10 và P11: 114 chạy, 109 giữ, 5 loại; tổng 44 trên 396 lần lặp bị loại. Các nhóm xấu nhất là P06 pha primary B (6 trên 18), nhân tố con P01 pha primary A (6 trên 18) và P03+P04 pha primary B (5 trên 18); tất cả được chạy thêm vòng đến từ 12 lần giữ trở lên. **Phân tích độ nhạy** (`summary_all.csv`: cùng thống kê trên mọi lần lặp đã đo, kể cả lần bị loại): trong 93 hàng so sánh được, dấu của hiệu ứng giống nhau ở 92 hàng. Năm hàng đổi việc khoảng 95% có chứa 0 hay không: P13 `frameDurationCpuMs_p99` (bảng chính chứa 0; trên mọi lần lặp không chứa 0, B tệ hơn 22%), P13 `memoryRssAnonMaxKb`, `st_search_query.maxMs` và `st_search_query.sumMs` (bảng chính không chứa 0; thêm các lần lặp bị tranh chấp thì khoảng rộng ra và chứa 0, trung vị Δ% giữ ở −58,7% và −85,7%) và P10 `slowFrames_pct` (bảng chính không chứa 0; trên mọi lần lặp chứa 0). Dịch lớn nhất của một trung vị Δ% là 26,7 điểm (P03+P04 `Recomposer_recompose.maxMs`, một chỉ số đuôi). Không kết luận nào dưới đây phụ thuộc vào lựa chọn giữa hai phân tích.

**Bảng 5.12.** E2, số lần lặp chạy / giữ / loại theo nhân tố, pha và biến thể (pha primary đo số đếm, trace và bộ nhớ; pha frames đo `FrameTimingMetric`).

| nhân tố | pha | A: chạy / giữ / loại | B: chạy / giữ / loại |
|---|---|---|---|
| P08 | frames | 12 / 12 / 0 | 12 / 12 / 0 |
| P08 | primary | 18 / 14 / 4 | 18 / 17 / 1 |
| P06 | frames | 15 / 14 / 1 | 15 / 14 / 1 |
| P06 | primary | 18 / 16 / 2 | 18 / 12 / 6 |
| P01+P09 | frames | 15 / 13 / 2 | 15 / 13 / 2 |
| P01+P09 | primary | 12 / 12 / 0 | 12 / 12 / 0 |
| P13 | frames | 15 / 13 / 2 | 15 / 12 / 3 |
| P13 | primary | 15 / 14 / 1 | 15 / 12 / 3 |
| P03+P04 | frames | 15 / 13 / 2 | 15 / 15 / 0 |
| P03+P04 | primary | 18 / 17 / 1 | 18 / 13 / 5 |
| P10 | frames | 15 / 14 / 1 | 15 / 14 / 1 |
| P10 | primary | 15 / 14 / 1 | 15 / 15 / 0 |
| P11 | frames | 12 / 12 / 0 | 12 / 12 / 0 |
| P11 | primary | 15 / 13 / 2 | 15 / 15 / 0 |
| P01+P09/P01 | primary | 18 / 12 / 6 | 18 / 16 / 2 |

**P08: hiệu ứng trang trí trong thẻ lưới** (đo trên `swangle_indirect`, A14; chỉ đọc Δ% trong nhân tố này).

**Bảng 5.13.** E2, P08 (A: `BookCard` và `BookCover` của agy-g38flash-B1-naive-r1; B: bìa phẳng).

| chỉ số | đơn vị | A: trung vị (IQR) | B: trung vị (IQR) | thay đổi (B so với A) | khoảng bootstrap 95% | delta Cliff | n |
|---|---|---|---|---|---|---|---|
| st.fx.perItem | count | 2 (0) | 0 (0) | −100% | [−100; −100] | 1 | 14 |
| st.rc.row | count | 18 (2) | 20 (0) | 11,1% | [0; 11,1] | −0,466 | 14 |
| Compose_recompose.count | count | 20 (2) | 21 (1) | 5% | [0; 10,5] | −0,5 | 14 |
| Recomposer_recompose.mainMs | ms | 0,5052 (0,2503) | 0,7196 (0,282) | 42,4% | [6,46; 110] | −0,655 | 14 |
| Compose_recompose.mainMs | ms | 30,47 (13,74) | 20,26 (6,612) | −33,5% | [−47; −7,84] | 0,647 | 14 |
| frameDurationCpuMs p90 | ms | 62,87 (17,53) | 49,07 (4,162) | −22% | [−35,2; −11,8] | 0,875 | 12 |
| frameDurationCpuMs p99 | ms | 127,6 (31,7) | 86,45 (10,07) | −32,3% | [−41,1; −17,7] | 0,875 | 12 |
| frameOverrunMs p90 | ms | 68,98 (32,27) | 46,85 (9,337) | −22,14 ms (tuyệt đối) | [−46,02; −9,035] | 0,819 | 12 |
| slowFrames_pct | % | 70,52 (12,21) | 48,65 (9,92) | −31% | [−38,9; −17,5] | 0,819 | 12 |
| memoryHeapSizeMaxKb | KB | 7372 (184) | 6596 (49) | −10,5% | [−12,4; −10] | 1 | 14 |
| memoryRssAnonMaxKb | KB | 4,646e+04 (309) | 4,265e+04 (300) | −8,2% | [−8,76; −7,86] | 1 | 14 |

Đọc theo mô tả: B giảm thời gian CPU khung hình p90 từ 62,87 xuống 49,07 ms (−22% [−35,2; −11,8]) và p99 −32,3%, và giảm tỷ lệ khung chậm từ 70,52% xuống 48,65%; heap tối đa thấp hơn 10,5% và RSS ẩn danh thấp hơn 8,2%. Số lần recomposition không giảm (`st.rc.row` 18 so với 20, `Compose_recompose.count` 20 so với 21), vì A là lưới tĩnh không có hoạt ảnh; thời gian luồng chính trong `Compose_recompose` giảm 33,5% [−47; −7,84], còn `Recomposer_recompose.mainMs` tăng 42,4% [6,46; 110] ở mức dưới 1 ms. Giả thuyết: chi phí nằm ở việc vẽ bóng, lớp phủ gradient và bìa vẽ bằng canvas, không ở số lần recompose; các chỉ số này không tách được việc đó. Bộ nhớ đồ họa, đại lượng thứ hai của H3, **không được đo**: lab không có chỉ số bộ nhớ đồ họa.

**P06: giải mã ảnh và bộ nhớ đệm.**

**Bảng 5.14.** E2, P06 (A: `CoverBitmap` của codex-luna-B3-detailed-functional-r1; B: Coil `AsyncImage`).

| chỉ số | đơn vị | A: trung vị (IQR) | B: trung vị (IQR) | thay đổi (B so với A) | khoảng bootstrap 95% | delta Cliff | n |
|---|---|---|---|---|---|---|---|
| st.decodedBytes | bytes | 2,025e+08 (0) | 6,233e+06 (0) | −96,9% | [−96,9; −96,9] | 1 | 12 |
| st.decodes | count | 15 (0) | 15 (0) | 0% | [0; 0] | 0 | 12 |
| memoryRssAnonMaxKb | KB | 1,84e+05 (303) | 6,16e+04 (159) | −66,5% | [−66,6; −66,5] | 1 | 12 |
| memoryHeapSizeMaxKb | KB | 7416 (31,25) | 6145 (7,5) | −17,1% | [−17,3; −17] | 1 | 12 |
| st_img_decode.sumMs | ms | 1660 (365,4) | 0 (0) | −100% | [−100; −100] | 1 | 12 |
| st_img_decode.maxMs | ms | 378,7 (78,87) | 0 (0) | −100% | [−100; −100] | 1 | 12 |
| frameDurationCpuMs p90 | ms | 5,937 (1,024) | 4,328 (0,2345) | −27,1% | [−35,9; −22,8] | 0,867 | 14 |
| frameDurationCpuMs p99 | ms | 81,48 (13,87) | 17,97 (9,2) | −77,9% | [−81,7; −69,2] | 0,98 | 14 |
| frameOverrunMs p90 | ms | −9,044 (1,485) | −11,48 (0,3407) | −2,436 ms (tuyệt đối) | [−3,017; −1,444] | 0,857 | 14 |
| slowFrames_pct | % | 3,596 (0,3648) | 1,37 (0,7821) | −61,9% | [−68,6; −49,2] | 0,867 | 14 |

Đọc theo mô tả: A giải mã đủ ảnh 1500x2250 cho mỗi trong 15 ảnh của kịch bản (tổng 203 MB giải mã), B giải mã cùng 15 ảnh ở cỡ khe (6,2 MB), giảm 96,9%; RSS ẩn danh đỉnh giảm từ 180 MB xuống 60 MB; CPU khung hình p99 giảm 78% và tỷ lệ khung chậm từ 3,6% xuống 1,4% (GPU phần mềm: chỉ đọc hướng). `st_img_decode.*` là đầu đo chỉ có ở A (B giải mã trong Coil), đọc là thời gian giải mã của A: tổng 1.660 ms, lần chậm nhất 378,7 ms.

**P01 cùng P09: dựng danh sách (`Column` dựng toàn bộ so với `LazyColumn`, 2.000 lượt mượn).**

**Bảng 5.15.** E2, P01+P09 (A: `BookDetailSheet` của agy-g38flash-B3-naive-r1; B: `LazyColumn` có `key` và `contentType`).

| chỉ số | đơn vị | A: trung vị (IQR) | B: trung vị (IQR) | thay đổi (B so với A) | khoảng bootstrap 95% | delta Cliff | n |
|---|---|---|---|---|---|---|---|
| st.rc.row | count | 2000 (0) | 31 (1) | −98,5% | [−98,5; −98,5] | 1 | 12 |
| Compose_recompose.count | count | 1 (0) | 34 (1) | 3,3e+03% | [3,2e+03; 3,3e+03] | −1 | 12 |
| Compose_recompose.mainMs | ms | 217,3 (16,21) | 20,72 (4,447) | −90,5% | [−91,6; −89,1] | 1 | 12 |
| timeToInitialDisplayMs | ms | 1901 (43,36) | 573,1 (32,99) | −69,9% | [−70,8; −68,7] | 1 | 12 |
| timeToFullDisplayMs | ms | 2018 (39,63) | 597,7 (80,56) | −70,4% | [−71,2; −66,8] | 1 | 12 |
| memoryHeapSizeMaxKb | KB | 2,255e+05 (0) | 7734 (32) | −96,6% | [−96,6; −96,6] | 1 | 12 |
| memoryRssAnonMaxKb | KB | 2,484e+05 (9208) | 4,381e+04 (159) | −82,4% | [−82,5; −81,7] | 1 | 12 |
| frameDurationCpuMs p90 | ms | 27,14 (2,594) | 5,912 (0,0826) | −78,2% | [−79,1; −76,9] | 1 | 13 |
| frameDurationCpuMs p99 | ms | 34,3 (4,213) | 18,21 (13,7) | −46,9% | [−62,2; −20,6] | 0,976 | 13 |
| frameOverrunMs p90 | ms | 14,08 (5,564) | −9,664 (0,1975) | −23,74 ms (tuyệt đối) | [−25,51; −19,86] | 1 | 13 |
| slowFrames_pct | % | 90,05 (17,99) | 1,255 (0,8475) | −98,6% | [−99,2; −98,1] | 1 | 13 |

Đọc theo mô tả: A dựng mọi hàng trước khung đầu tiên (`st.rc.row` là số hàng, 2.000 so với 31), nên thời gian đến hiển thị đầu, heap và chi phí khung hình tăng theo độ dài danh sách; B chỉ dựng các hàng nhìn thấy. TTID giảm từ 1.901 xuống 573,1 ms, heap Java đỉnh từ 220 MB xuống 7,6 MB, và 90,05% khung hình chậm ở A so với 1,255% ở B. `Compose_recompose.count` ở B cao hơn có chủ đích: dựng lazy chia thành nhiều lượt nhỏ trong khi A làm một lượt rất lớn; tổng thời gian nằm ở `Compose_recompose.mainMs`. Quét theo số lượt mượn chưa chạy (xem đầu mục).

**Nhân tố con P01: mục danh sách không có key** (chỉ pha primary).

**Bảng 5.16.** E2, nhân tố con P01 (A: `HistoryList` của claude-haiku-B3-detailed-functional-r1, `items(list)` không key; B: cùng danh sách có `key` và `contentType`).

| chỉ số | đơn vị | A: trung vị (IQR) | B: trung vị (IQR) | thay đổi (B so với A) | khoảng bootstrap 95% | delta Cliff | n |
|---|---|---|---|---|---|---|---|
| st.rc.row | count | 28 (1) | 28 (0,25) | 0% | [−3,45; 0] | 0,125 | 12 |
| Compose_recompose.count | count | 29 (1) | 29 (0,25) | 0% | [−3,33; 0] | 0,0833 | 12 |
| Compose_recompose.mainMs | ms | 70,23 (8,05) | 74,95 (21,27) | 6,72% | [−4,24; 29,5] | −0,396 | 12 |
| Recomposer_recompose.mainMs | ms | 0,6055 (0,2474) | 0,6135 (0,1297) | 1,31% | [−20,2; 31,2] | −0,0938 | 12 |
| timeToInitialDisplayMs | ms | 617,1 (95,36) | 620 (150,7) | 0,471% | [−6,44; 21,5] | −0,26 | 12 |
| timeToFullDisplayMs | ms | 638,2 (76,95) | 680 (165,6) | 6,55% | [−5,41; 24,9] | −0,365 | 12 |
| memoryHeapSizeMaxKb | KB | 1,854e+04 (23,25) | 1,855e+04 (16,5) | 0,0378% | [−0,0863; 0,0917] | −0,0938 | 12 |
| memoryRssAnonMaxKb | KB | 5,008e+04 (160) | 5,009e+04 (113) | 0,012% | [−0,203; 0,248] | −0,125 | 12 |

Đọc theo mô tả: **kết quả không khác biệt (null).** Mọi khoảng chứa 0 và số thân hàng chạy giống nhau (28 hàng nhìn thấy) ở hai biến thể: thiếu key không đổi gì trên danh sách tĩnh; nó quan trọng khi sắp xếp lại và chèn mục, mà kịch bản này không thực hiện (6 lần vuốt trên danh sách cố định 5.000 mục). Lỗi gieo có thật trong mã (phát hiện P01) nhưng không tốn gì ở kịch bản này.

**P13: tìm kiếm trên 100 nghìn sách.**

**Bảng 5.17.** E2, P13 (A: `TitleSearch` của claude-haiku-B1-detailed-functional-r1, `LIKE` không chỉ mục; B: SQLite FTS4 so khớp tiền tố từ khóa; cùng debounce 200 ms và cùng trang 120 hàng theo tiêu đề).

| chỉ số | đơn vị | A: trung vị (IQR) | B: trung vị (IQR) | thay đổi (B so với A) | khoảng bootstrap 95% | delta Cliff | n |
|---|---|---|---|---|---|---|---|
| st.search.queries | count | 9 (0) | 8 (0) | −11,1% | [−11,1; −11,1] | 0,857 | 12 |
| st_search_query.count | count | 9 (0) | 8 (0) | −11,1% | [−11,1; −11,1] | 0,857 | 12 |
| st_search_query.mainMs | ms | 0 (0) | 0 (0) | 0 ms (tuyệt đối) | [0; 0] | 0 | 12 |
| st_search_query.sumMs | ms | 634,9 (79,26) | 85,42 (21,72) | −86,5% | [−88,4; −84,1] | 0,714 | 12 |
| st_search_query.maxMs | ms | 96,63 (32,69) | 37 (8,245) | −61,7% | [−69,7; −51,4] | 0,714 | 12 |
| memoryHeapSizeMaxKb | KB | 7740 (60) | 7108 (36) | −8,17% | [−8,56; −7,58] | 0,905 | 12 |
| memoryRssAnonMaxKb | KB | 4,497e+04 (302) | 4,478e+04 (147) | −0,405% | [−0,839; −0,0625] | 0,464 | 12 |
| frameDurationCpuMs p90 | ms | 32,06 (5,994) | 34,22 (8,698) | 6,72% | [−8,22; 28,9] | −0,269 | 12 |
| frameDurationCpuMs p99 | ms | 132,9 (66,47) | 157,7 (18,72) | 18,7% | [−18,8; 33] | −0,436 | 12 |
| frameOverrunMs p90 | ms | 34,62 (10,67) | 36,42 (10,72) | 1,796 ms (tuyệt đối) | [−9,297; 6,584] | 0,0128 | 12 |
| slowFrames_pct | % | 17,11 (8,383) | 18,9 (2,764) | 10,5% | [−18; 32,7] | −0,218 | 12 |

Đọc theo mô tả: cả hai biến thể chạy truy vấn trên luồng IO sau debounce 200 ms và trả về trang 120 hàng theo thứ tự tiêu đề, nên `st_search_query.mainMs` bằng 0 ở cả hai. Chi phí mỗi truy vấn khác nhau: truy vấn chậm nhất 96,63 ms ở A so với 37 ms ở B (mục tiêu của sản phẩm là tìm kiếm dưới 50 ms trên 100 nghìn sách: chỉ B đạt, trên emulator và với tiền tố đang gõ, nên chỉ đọc tương đối); tổng thời gian truy vấn 634,9 ms so với 85,42 ms, cỡ 70 ms so với 11 ms mỗi truy vấn. Số truy vấn khác nhau một (9 so với 8), nguyên nhân chưa được cô lập, nên REPORT nêu giá trị mỗi truy vấn. **Khung hình không đi theo:** khoảng của CPU khung hình p90 và của tỷ lệ khung chậm chứa 0, và p99 nếu có thì cao hơn ở B (+18,7%, khoảng [−18,8; 33]; trên mọi lần lặp thì khoảng không chứa 0). Cả hai biến thể kết xuất cùng trang 120 hàng sau mỗi truy vấn, nên trên thiết bị này chi phí khung hình là của danh sách, không phải của tìm kiếm. Chênh lệch bộ nhớ nhỏ (heap −8%). **Hai biến thể không bằng nhau từng hàng** (`p13_equivalence.csv`): A là so khớp chuỗi con (`LIKE '%q%'`), B là so khớp tiền tố từ khóa, nên với bốn trong mười hai tiền tố đã gõ (s, si, "silent r", "silent ri") trang 120 hàng khác nhau (Jaccard của trang 0,09, 0,16, 0,22 và 0,53); với tám tiền tố còn lại tập kết quả đầy đủ và trang trùng nhau. Bản H (quét tiền tố từ khóa ngây thơ) chỉ dùng để kiểm tra chỉ mục FTS4 của B trả đúng thứ mà quy tắc tiền tố từ khóa định nghĩa (Jaccard 1,0 ở cả mười hai). Với bốn tiền tố đó, so sánh thời gian truy vấn là giữa hai tập kết quả khác nhau.

**P03 cùng P04: độ ổn định và việc nặng trong composition.**

**Bảng 5.18.** E2, P03+P04 (A: `BookDetailSheet` dòng 58, 65, 67 của agy-g38flash-B3-naive-r1; B: `LoanList` ổn định và `remember`).

| chỉ số | đơn vị | A: trung vị (IQR) | B: trung vị (IQR) | thay đổi (B so với A) | khoảng bootstrap 95% | delta Cliff | n |
|---|---|---|---|---|---|---|---|
| st.rc.row | count | 300 (0) | 300 (0) | 0% | [0; 0] | 0 | 13 |
| Compose_recompose.count | count | 76 (1) | 75 (1) | −1,32% | [−1,32; 1,33] | 0,181 | 13 |
| Recomposer_recompose.mainMs | ms | 11,85 (4,378) | 10,5 (2,314) | −11,4% | [−29,5; 7,16] | 0,258 | 13 |
| Compose_recompose.mainMs | ms | 36,21 (7,524) | 36,75 (7,627) | 1,49% | [−14,4; 13] | 0,0317 | 13 |
| frameDurationCpuMs p90 | ms | 11,56 (5,529) | 9,802 (4,329) | −15,2% | [−37,6; 29,8] | 0,231 | 13 |
| frameDurationCpuMs p99 | ms | 269,7 (33,56) | 221,3 (115,6) | −18% | [−37,8; −1,38] | 0,538 | 13 |
| frameOverrunMs p90 | ms | −4,172 (5,628) | −6,381 (4,507) | −2,209 ms (tuyệt đối) | [−6,711; 2,206] | 0,282 | 13 |
| slowFrames_pct | % | 6,494 (2,966) | 6,494 (2,094) | 0% | [−32,2; 20,1] | 0,144 | 13 |

Đọc theo mô tả: **kết quả không khác biệt (null).** Bộ phát hiện tìm ra tham số `List<Loan>` không ổn định cùng bộ định dạng và bộ lọc trong composition, và bản port giữ nguyên chúng, nhưng ở kịch bản này (300 lượt mượn, nhịp 10 Hz, một danh sách mới bằng nhau mỗi nhịp) A và B làm cùng một việc: `st.rc.row` bằng 300 ở cả hai (chỉ lần dựng đầu), số recomposition mỗi lần chạy 76 so với 75, thời gian luồng chính recompose 36,21 so với 36,75 ms (khoảng [−14,4; 13]%). CPU khung hình p90 và tỷ lệ khung chậm không khác; p99 thấp hơn 18% ở B (khoảng [−37,8; −1,38]), một trong 18 hàng khung hình và không hiệu chỉnh cho điều đó. Lần thí điểm có bản A viết tay cho hiệu ứng lớn (các cấu trúc của nó dựng lại cả danh sách mỗi nhịp); phiên bản của cùng các mẫu trong mã E1 thì không ở cỡ này. Toolchain là Kotlin 2.4.21, nơi strong skipping là mặc định (Mục 2.2.3); giả thuyết: đó là một lý do tham số không ổn định không tốn ở đây, nhưng không nhánh nào tắt strong skipping, và REPORT ghi rằng nguyên nhân chưa được cô lập. Số lượng lượt mượn là núm sẽ cho biết hiệu ứng có tăng theo cỡ hay không (chưa chạy).

**Hình 5.11.** E2: thay đổi của chỉ số chính theo nhân tố (B so với A) kèm khoảng bootstrap 95%; mô tả. Số liệu vẽ: `figures/out/F13-e2-effects.data.csv`.

![F13](figures/out/F13-e2-effects.svg)

**Hình 5.12.** E2: giá trị tuyệt đối của chỉ số chính, A so với B, từng lần lặp được giữ. Số liệu vẽ: `figures/out/F15-e2-absolute.data.csv`.

![F15](figures/out/F15-e2-absolute.svg)

**C5 (xác nhận): P08, A so với B.** Điểm cuối: P90 của `FrameDurationCpuMs` ở mỗi lần lặp, mục P08 của mã đã sinh (A) so với bản phẳng (B); delta% = (A − B) / B của các trung vị; bootstrap phân vị 95% (10.000 lần lấy lại, hạt giống 20261121) và Mann–Whitney trên giá trị từng lần lặp (`c5_confirmatory.csv`). Quy tắc đã đăng ký (Mục 4.8): ủng hộ khi p hiệu chỉnh Holm dưới 0,05, delta% từ +10% và khoảng 95% không chứa 0; H3 bị bác bỏ khi khoảng nằm trọn trong [−5%, +5%].

**Bảng 5.19.** E2, C5 (đường GPU `swangle_indirect`, A14).

| n (A, B) | trung vị A | trung vị B | delta% (A − B) / B | khoảng bootstrap 95% | Mann–Whitney p thô | p hiệu chỉnh Holm | điều kiện đã đăng ký |
|---|---|---|---|---|---|---|---|
| 12, 12 | 62,87 ms | 49,07 ms | +28,13% | [+13,38%; +54,31%] | 0,000308 | 0,000616 (Bảng 5.27) | cả ba điều kiện thỏa |

Cả ba điều kiện thỏa: p hiệu chỉnh 0,000616 dưới 0,05, delta% +28,13% từ +10%, và khoảng [13,38; 54,31] không chứa 0 (cận dưới cũng trên +10%); khoảng không nằm trọn trong [−5%, +5%], nên H3 không bị bác bỏ. **C5 được ủng hộ**, theo cách đọc hẹp của nó: ô lưới P08 của một lần chạy E1 (agy-g38flash-B1-naive-r1) tốn thêm cỡ 28% ở P90 thời gian CPU khung hình so với bản phẳng, trên một đường GPU của emulator. Giới hạn: một ứng viên và một lần chạy E1, nên đây là chi phí của một cấu trúc đã sinh, không phải của mọi biến thể của "vẻ ngoài AI"; 12 lần giữ mỗi biến thể; một thiết bị ảo, một phiên đo và một hạt giống dữ liệu; số tuyệt đối không so được với các nhân tố khác (A14); lần kiểm soát thăm dò ở Mục 5.3.3 cho thấy đường GPU làm co đối chiếu khung hình của một nhân tố khác (P01+P09: p90 −32,3% trên swangle_indirect so với −78,2% trên swiftshader_indirect), nên C5 được đo trên đường mà lần kiểm soát cho hiệu ứng nhỏ hơn và độ lớn +28,13% không suy ra được cho đường khác; B là bản phẳng do người thực hiện E2 viết; bộ nhớ đồ họa của H3 không đo, nên điều kiện ủng hộ của H3 ở giao thức (C5) thỏa trên khung hình và đại lượng bộ nhớ đồ họa vẫn chưa có.

**Hình 5.13.** C5: P90 thời gian CPU khung hình của ô lưới P08, từng lần lặp và khoảng bootstrap của delta% (đường GPU swangle_indirect, A14). Số liệu vẽ: `figures/out/F14-c5.data.csv`.

![F14](figures/out/F14-c5.svg)

**Chi phí kỳ vọng (khám phá).** Chi phí kỳ vọng của một mẫu trong tập ứng dụng sinh ra là (tỷ lệ ứng dụng chứa mẫu, E1) × (chi phí khi có mẫu, E2). Tần suất E1 là tỷ lệ lần chạy hợp lệ có ít nhất một phát hiện của luật ở hai nhánh không guardrail (naive 33 và detailed-functional 33, tức 66 lần chạy; bộ phát hiện v1 đã đóng băng); nhánh guardrailed là một xử lý khác và bị bỏ ra. Cột chi phí là A − B (trung vị của các lần lặp được giữ); cột cuối nhân nó với tần suất. Số đo kích thước không phụ thuộc thiết bị, các số ms là tương đối với emulator. Giả định: biến thể A (một ứng viên mỗi nhân tố) đại diện cho cấu trúc của E1; REPORT không đưa khoảng cho tích này và bài không tính thêm.

**Bảng 5.20.** Chi phí kỳ vọng = tần suất E1 × chi phí E2 (`e2-bench/REPORT.md` mục 7).

| luật | nhân tố E2 | chỉ số chi phí | chi phí khi có (A − B) | tần suất E1 (trên 66 lần chạy không guardrail) | tần suất × chi phí | ghi chú |
|---|---|---|---|---|---|---|
| P08 | P08 | frameDurationCpuMs_p90 | 13,8 ms | P08 0,12 | 1,67 ms | đường GPU swangle_indirect (A14) |
| P06 | P06 | st.decodedBytes | 196 MB | P06 0,08 | 14,9 MB | luật chưa kiểm chứng ở E1 (Bảng 5.3) |
| P09 | P01+P09 | timeToInitialDisplayMs | 1328 ms | P09 0,03 | 40,2 ms | luật chưa kiểm chứng ở E1 (Bảng 5.3); luật P01 ở hàng sau |
| P01 | P01+P09/P01 | timeToInitialDisplayMs | không có khác biệt đo được | P01 0,21 | 0 | khoảng chứa 0 (kết quả không khác biệt (null)) |
| P13 | P13 | st_search_query.sumMs | 550 ms | P13 0,23 | 125 ms | khung hình không cải thiện |
| P03, P04 | P03+P04 | Compose_recompose.mainMs | không có khác biệt đo được | P03 0,09; P04 0,08 | 0 | khoảng chứa 0 (kết quả không khác biệt (null)) |
| P10 | P10 | apk_bytes | 22,4 MB | P10 1,00 | 22,4 MB | luật chưa kiểm chứng ở E1, bộ v1 có lỗi dương tính giả (A13); ở nhánh v2 30/33 và 33/33 ở hai kiểu không guardrail (Bảng 5.4) |
| P11 | P11 | timeToInitialDisplayMs | 27,2 ms | P11 1,00 | 27,2 ms |  |

Đọc theo mô tả: P10 và P11 có ở mọi ứng dụng, nên chi phí kỳ vọng bằng chính chi phí (lớn với kích thước APK, nhỏ với khởi động). Trong các mẫu cấp mã, P13 và P01 xuất hiện ở khoảng một phần năm số lần chạy không guardrail, P08 ở một phần tám, P06, P03 và P04 ở dưới một phần mười, P09 ở 3%. Tích số chỉ để xếp hạng mô tả: nó nhân hai số ước lượng từ một ứng viên, không kèm khoảng, và ba luật (P06, P09, P10) chưa qua cổng kiểm chứng của E1.

**Hình 5.14.** Chi phí kỳ vọng (khám phá): tần suất ở E1 so với thay đổi do bản sửa ở E2; chấm rỗng là luật chưa kiểm chứng. Số liệu vẽ: `figures/out/F16-expected-cost.data.csv`.

![F16](figures/out/F16-expected-cost.svg)

### 5.4. E3: công cụ tĩnh độc lập

E3 là mô tả, không có điểm cuối xác nhận. Phạm vi: 13 APK: bản phát hành ShelfTap của sản phẩm, hai bản dựng optlab của E2 (`optlab-noR8` là A của P10, `optlab-release` là B của P10, cả hai ký bằng khóa debug) và 10 ứng dụng sống sót của E1 (agy 4, claude 3, codex 3; hai trong số đó không build được không can thiệp). MobSF 4.6.9 (không Docker, chỉ phân tích tĩnh) cộng jadx 1.5.7 và quét bí mật bằng regex [MobSF] [JADX]. Ứng dụng tham chiếu của người viết **không** được quét. Phân loại: **tp** nếu điểm yếu có thật trong sản phẩm (manifest đã giải mã, mã giải dịch, thông tin chứng chỉ) và trái hướng dẫn MASVS [MASVS]; **fp** nếu sai về sự kiện, nằm trong mã thư viện mà ứng dụng không dùng cho mục đích bị cảnh báo, hoặc là mặc định của nền tảng bị cảnh báo thành điểm yếu. Phân loại do một người (không có kappa phân loại); kết quả là số cảnh báo **cùng** kết quả phân loại thủ công, không chỉ số cảnh báo của MobSF (`e3-security/results/findings.csv`).

**Kết quả chung.** 103 phát hiện trên 13 mục tiêu: tp 30, fp 73, độ chính xác **0,291** (Wilson 95% [0,212; 0,385]).

**Bảng 5.21.** E3, theo công cụ và nhóm MASVS.

| công cụ | nhóm MASVS | n | tp | fp | độ chính xác | cận dưới | cận trên |
|---|---|---|---|---|---|---|---|
| jadx (quét bí mật bằng regex) | MASVS-STORAGE | 1 | 0 | 1 | 0,0 | 0 | 0,793 |
| mobsf | MASVS-CODE | 14 | 11 | 3 | 0,786 | 0,524 | 0,924 |
| mobsf | MASVS-CRYPTO | 2 | 0 | 2 | 0,0 | 0 | 0,658 |
| mobsf | MASVS-NETWORK | 2 | 0 | 2 | 0,0 | 0 | 0,658 |
| mobsf | MASVS-PLATFORM | 22 | 0 | 22 | 0,0 | 0,0 | 0,149 |
| mobsf | MASVS-RESILIENCE | 25 | 13 | 12 | 0,52 | 0,335 | 0,7 |
| mobsf | MASVS-STORAGE | 37 | 6 | 31 | 0,162 | 0,077 | 0,311 |

**Ba mươi tp là gì.** debuggable=true: 10 (mọi ứng dụng sống sót, vốn là bản debug); minSdk dưới mức khuyến nghị: 11; allowBackup đặt true hoặc vắng không có quy tắc: 6; chứng chỉ debug trên sản phẩm kiểu release: 2 (cặp optlab); Janus (chữ ký v1 với minSdk 23): 1. **Không tp nào là điểm yếu ở mức mã:** MobSF không tìm thấy bỏ qua TLS, thông tin xác thực cứng, lưu trữ không mã hóa hay lưu lượng cleartext ở mục tiêu nào. Cả 22 phát hiện MASVS-PLATFORM (thành phần exported) và các phát hiện MASVS-NETWORK, MASVS-CRYPTO đều là fp (receiver của thư viện được bảo vệ bằng quyền hệ thống, thành phần cố ý exported, trust anchor mặc định, mã thư viện).

**Bảng 5.22.** E3, theo bộ công cụ sinh (chỉ các ứng dụng sống sót; mô tả, 3 hoặc 4 quét mỗi bộ).

| bộ công cụ | quét | tp | fp | tp mỗi quét | độ chính xác |
|---|---|---|---|---|---|
| agy | 4 | 11 | 31 | 2,75 | 0,262 |
| claude | 3 | 7 | 17 | 2,33 | 0,292 |
| codex | 3 | 9 | 8 | 3,0 | 0,529 |

**Độ chính xác phụ thuộc cách đọc nhãn.** Chính tiêu đề 0,291 là đầu ra mặc định của MobSF trên các bản dựng này, không phải thuộc tính của ứng dụng.

**Bảng 5.23.** E3, độ nhạy với quy tắc nhãn.

| cách đọc | tp | fp | độ chính xác |
|---|---|---|---|
| như đã phân loại (chính) | 30 | 73 | 0,291 |
| nghiêm ngặt: fp do thư viện và do bản debug tính là tp (quét sản phẩm báo gì là thật) | 83 | 20 | 0,806 |
| liên quan phát hành: bỏ phát hiện chỉ mô tả kiểu build debug của sản phẩm debug | 20 | 62 | 0,244 |
| chỉ sản phẩm kiểu release (3 mục tiêu) | 3 | 17 | 0,15 |
| chỉ ứng dụng sống sót kiểu debug (10 mục tiêu) | 27 | 56 | 0,325 |

Quy tắc nhãn chính có một mâu thuẫn nội bộ: `debuggable=true` trên bản debug được tính tp (kiểm tra tự động), trong khi các quy tắc thủ công coi chứng chỉ debug hay `BuildConfig.DEBUG` trên sản phẩm kiểu debug là fp ("mô tả kiểu build"); hàng thứ ba của Bảng 5.23 bỏ mọi phát hiện chỉ mô tả kiểu build (10 tp, 11 fp) và hàng thứ hai là cách đọc ngược lại.

**Bộ phát hiện E1 so với công cụ.** Trên 80 cặp (ứng dụng sống sót nhân 8 lớp S mà cả hai có luật: S01 đến S06, S08, S09): so với mọi phát hiện của MobSF, cả hai cùng thấy 7, chỉ bộ phát hiện 2, chỉ MobSF 6, không bên nào 65 (kappa 0,581, PABAK 0,8, đồng thuận 0,9); so với các tp đã phân loại của MobSF, 6, 3, 0 và 71 (kappa 0,78, PABAK 0,925, đồng thuận 0,963). Bộ phát hiện đồng ý với tp của MobSF hầu như chỉ ở S06 (allowBackup, 6/6). Ba phát hiện chỉ một công cụ thấy, đều từ bộ phát hiện E1 và đối chiếu với sản phẩm: token `access_token` lưu dưới khóa Preferences DataStore ở claude-haiku-B2-naive-r3 là lưu token không mã hóa **thật** mà MobSF không có luật; `HttpLoggingInterceptor` ở `Level.BODY` không chặn khi phát hành ở agy-g38flash-B3-detailed-functional-r3 là thật; bốn dòng `DROP TABLE IF EXISTS $TABLE_*` nội suy hằng số biên dịch nên **không** chèn được (S09 ở đó là phát hiện đúng về mẫu nhưng không khai thác được). Nói gọn: hai công cụ thấy hai tầng khác nhau; MobSF thấy manifest và cấu hình build, bộ phát hiện thấy mã.

**So sánh A và B của E2.** `optlab-noR8` (A của P10): 8 phát hiện, 1 tp, 7 fp, độ chính xác 0,125; `optlab-release` (B): 6 phát hiện, 1 tp, 5 fp, 0,167; tp duy nhất của mỗi bên là chứng chỉ debug trên sản phẩm kiểu release. Bật R8 không thay đổi kết quả phân loại bảo mật ở các mục tiêu này; ba mục tiêu kiểu release có độ chính xác 0,15.

**Lỗi và hạn chế của E3.** Lần quét jadx đầu (`-j 2 -Xmx1g`) không đầy đủ vì bỏ qua mã thoát; kết quả MobSF và phân loại theo manifest không bị ảnh hưởng, còn quét bí mật và các ghi chú "không tìm thấy trong jadx" thì có; đã chạy lại với `-j 1 -Xmx1536m` cho 12 trên 13 mục tiêu (quét bí mật vẫn chỉ cho một hit, vốn là fp, không có thông tin xác thực mới). APK sản phẩm chưa chạy lại vì tệp build đã bị dọn. Ba lần quét của codex hỏng do ENOSPC của NTFS (hết bản ghi tệp, không phải hết dung lượng) và được lặp lại. Mười ba sản phẩm, mười trong số đó từ một đường ống sinh; một người phân loại.

**Hình 5.15.** E3: độ chính xác theo công cụ và nhóm MASVS, và đồng thuận với bộ phát hiện E1. Số liệu vẽ: `figures/out/F17-e3.data.csv`.

![F17](figures/out/F17-e3.svg)

### 5.5. E4: áp lực sửa lỗi

**Thiết kế và luồng.** Năm dự án thư viện nhỏ, mỗi dự án có một lỗi cố ý gây ra mà cách sửa nhanh nhất cũng là một sự làm yếu: C1 cleartext (`usesCleartextTraffic=true`, S03), C2 TLS tự ký (trust-all, S04), C3 luồng chính (`StrictMode` cho phép hết, P07), C4 R8 (tắt minify hoặc keep rộng, P10), C5 Keystore (bỏ yêu cầu xác thực khỏi khóa, X01, và lưu token plaintext, S01). Năm lỗi, ba bộ công cụ, hai biến thể prompt, năm lần lặp: 150 lượt thử chính. Bộ dữ liệu chính (A9) gồm claude 50, codex 50 và lần chạy lại của agy với web bị chặn bằng hook, 48 lượt được chấm cộng 2 lượt dừng ở harness (diff rỗng, loại khỏi mẫu số, `infra_failures.csv`): **148 lượt phân loại được**, 134 diff duy nhất. Bốn lớp kết quả: secure-fix, insecure-workaround, perf-regression, no-fix. Thang chấm (`RUBRIC.md`) được đóng băng ở v1 trước mọi lượt thử, phiên bản hiện hành v4; một thí điểm 15 lượt không được phân tích.

**Kiểm toán công cụ web (A9).** Sai lệch được phát hiện sau đợt lượt thử đầu: agy có công cụ `search_web` không cần quyền và 31 trên 50 lượt thử của đợt đầu đã dùng nó (182 lần gọi); codex có công cụ web (`web_search` tích hợp và `search_service.web_run`) và **không dùng lần nào** trong 50 lượt thử; claude không có công cụ web. Luật chặn bằng quy tắc quyền cho agy không hoạt động; một hook PreToolUse trong HOME của lần chạy thì chặn được, và 50 lượt thử của agy được chạy lại với web bị chặn. Codex về sau tắt công cụ web và kết nối. Đợt agy đầu là tập thứ cấp `agy-web` (49 lượt được chấm), **phân tích riêng và không bao giờ gộp**: ở các lỗi bảo mật, tỷ lệ insecure là 13/29 = 44,8% [28,4; 62,5] so với 12/28 = 42,9% khi web bị chặn; việc có tìm kiếm không tạo khác biệt thấy được ở bộ này. Web không được dùng ở lượt thử chính nào (`web_audit_*.csv`).

**C4 (xác nhận).** Insecure-workaround ở **17 trên 148** lượt phân loại được = **11,5%** (Wilson 95% [7,3; 17,6]) [Wilson1927]. Nhị thức chính xác một phía với H0 π ≤ 0,10: p thô = **0,311**. Tỷ lệ cũng dưới mức hiệu ứng nhỏ nhất đã đăng ký (0,15). Kết luận: **C4 không được ủng hộ** (cả hai tiêu chí đều không đạt); hiệu chỉnh Holm trên C1 đến C5 chỉ có thể làm tăng p, nên không đổi kết luận (giá trị cuối ở chỗ trống Holm của Mục 5.7). Theo quy tắc của H2, H2 không bị bác bỏ vì cận trên Wilson 0,176 không dưới 0,10: kết luận là "không được ủng hộ tại SESOI, không đủ bác bỏ". Bootstrap theo cụm trên 15 ô (lỗi nhân bộ công cụ) cho [0,0133; 0,2466] (`c4_confirmatory.csv`). Cỡ mẫu 148 so với mục tiêu thiết kế 150 (sàn 75).

**Thứ cấp: nhóm lỗi bảo mật (C1, C2, C5).** Insecure-workaround ở **17 trên 88** = **19,3%** [12,4; 28,8] (p nhị thức chính xác với 0,10 là 0,006; **thứ cấp, mô tả**: cách gộp này là từ ngữ "chính" của thang chấm, được giao thức thay bằng C4; không phải kiểm định xác nhận). Theo bộ công cụ, ở các lỗi bảo mật:

| bộ công cụ | insecure-workaround, lỗi bảo mật | bất kỳ sự làm yếu, cả năm lỗi |
|---|---|---|
| claude (haiku) | 0/30 = 0,0% [0,0; 11,4] | 0/50 [0,0; 7,1] |
| codex | 5/30 = 16,7% [7,3; 33,6] | 5/50 = 10,0% [4,3; 21,4] |
| agy (web bị chặn) | 12/28 = 42,9% [26,5; 60,9] | 12/48 = 25,0% [14,9; 38,8] |

**Theo lỗi (mô tả).** secure-fix / insecure-workaround / perf-regression / no-fix:

| lỗi | secure-fix | insecure-workaround | perf-regression | no-fix | n |
|---|---|---|---|---|---|
| C1 cleartext | 25 | 5 | 0 | 0 | 30 |
| C2 TLS tự ký | 27 | 0 | 0 | 1 | 28 |
| C3 luồng chính | 30 | 0 | 0 | 0 | 30 |
| C4 R8 | 30 | 0 | 0 | 0 | 30 |
| C5 Keystore | 18 (claude 10, codex 6, agy 2) | 12 (claude 0, codex 4, agy 8) | 0 | 0 | 30 |

**Nơi sự làm yếu xảy ra.** Mọi sự làm yếu nằm ở hai lỗi và dùng một luật mỗi lỗi: C1 cleartext (S03): agy 4 trên 10, codex 1 trên 10, claude 0 trên 10; C5 Keystore (X01, phần mở rộng của danh mục đã được chấp nhận, chỉ ở E4): agy 8 trên 10, codex 4 trên 10, claude 0 trên 10. C2, C3 và C4 không cho sự làm yếu nào trong 88 lượt thử (một lượt claude ở C2 là no-fix); không lượt nào tắt xác thực TLS hay tắt shrinking. Cụm này tập trung và **phụ thuộc bộ công cụ**: claude 0/50 trên cả năm lỗi. Mô hình và harness không tách được, và chỉ có năm lỗi; cách viết đúng là "các trợ lý này làm yếu X ở r trên n lượt thử trong các lỗi này", không phải "AI gây ra".

**Biến thể prompt (khám phá, Fisher, không hiệu chỉnh).** Cung cấp ngữ cảnh dự án (biến thể B) không thay đổi đáng kể tỷ lệ làm yếu: mọi lỗi B 5/74 so với A 12/74 (p = 0,120); lỗi bảo mật 5/44 so với 12/44 (p = 0,103); lỗi hiệu năng 0/30 so với 0/30. Chiều hướng (ít làm yếu hơn khi có ngữ cảnh) không phân biệt được với may rủi ở cỡ mẫu này.

**Độ nhạy.** Đếm nghiêm ngặt (luật cleartext theo miền ở cấu hình chính tính là insecure): bất kỳ sự làm yếu 26/148 = 17,6% [12,3; 24,5]; đếm khoan dung không đổi (11,5%). Bỏ các lượt thử có bất kỳ nỗ lực dùng web còn lại 8/118 = 6,8% bất kỳ sự làm yếu và 8/68 = 11,8% insecure ở các lỗi bảo mật; trong 17 sự làm yếu có 9 nằm ở 30 lượt agy đã thử tìm kiếm (8 ở C5, 1 ở C1); mô hình hỗn hợp, chỉ mô tả. Đọc lại mù ba diff C5 bị tranh cãi cho C5 secure-fix 15/30 (chính: 18/30) và no-fix 3 (chính: 0); C4 và các lớp còn lại không đổi (chỉ secure-fix và no-fix dịch chuyển).

**Độ đồng thuận của người chấm.** Đợt 1 (118 diff): đồng thuận 97,5%, kappa Cohen 0,892, PABAK 0,966 [Cohen1960]; đợt 2 (16 diff chỉ lần chạy lại agy tạo ra): đồng thuận hoàn toàn, kappa và PABAK 1,000. Gộp 134 diff duy nhất: đồng thuận 97,8%, kappa 0,909, PABAK 0,970, kappa theo lượt thử 0,949. Ba diff bị tranh cãi (đều C5, người chấm 1 cho secure-fix, người chấm 2 cho no-fix); bộ phân loại tự động đồng ý người chấm 1 nên theo quy tắc phân xử đóng băng v4 nhãn chính là secure-fix; đọc lại mù về sau đứng về phía người chấm 2 và chỉ là hàng độ nhạy ở trên. Người chấm 1 là người thiết kế thang chấm; người chấm 2 là một tác nhân độc lập chỉ thấy thang chấm.

**Đường cơ sở của người (Stack Overflow, 23 câu trả lời được gắn nhãn).** C1: 4/4 câu trả lời khuyên bật cleartext (insecure-workaround) so với 5/30 lượt thử của trợ lý; C2: 1/6 câu khuyên tin mọi thứ so với 0/28; C3: 1/5 cho phép mạng ở luồng chính so với 0/30; C4: 1/5 tắt R8 so với 0/30; C5: 0/3 so với 12/30. Ở C1 trợ lý làm yếu ít hơn các câu trả lời hàng đầu; ở C5 trợ lý làm yếu nhiều hơn vài câu trả lời tìm được, và sự làm yếu ở đó là bỏ chính thuộc tính đã gây ra lỗi. Bộ này mỏng (C5 chỉ 3 câu) và do một người gắn nhãn.

**Hình 5.16.** C4: kết cục của E4 theo lỗi (tập chính, agy-web thứ cấp, mốc Stack Overflow) và tỷ lệ gộp của insecure-workaround. Số liệu vẽ: `figures/out/F18-c4.data.csv`.

![F18](figures/out/F18-c4.svg)

**Hình 5.17.** E4: tỷ lệ insecure-workaround theo lỗi, bộ công cụ và biến thể prompt. Số liệu vẽ: `figures/out/F19-e4-bundle.data.csv`.

![F19](figures/out/F19-e4-bundle.svg)

### 5.6. E5: trợ lý ở vai trò người tối ưu

E5 là mô tả, không có điểm cuối xác nhận. **Thiết kế.** Mục tiêu là `e5-optimizer/target`, một dự án ShelfTap nhỏ (8 tệp Kotlin) dựng riêng cho E5, không phải ứng dụng của kho E1. Danh sách chuẩn (`ground_truth.csv`, cố định trước lần chạy đầu): 14 vấn đề gieo (P01, P04, P06, P07, P08, P10, P13, S01, S03, S04, S05, S06, S07, D01), 3 vấn đề có thật không gieo (P03, P11, U05; được báo cáo nhưng không tính vào độ phủ) và 3 mồi (mã đúng không được đổi: `RecentRow` có key, `Stats()` có remember và derivedStateOf, `Log` có chặn `BuildConfig.DEBUG` ở `TokenStore`). Hai bên sửa: codex (codex-cli 0.170.0, `gpt-5.6-luna`) và claude (Claude Code 2.1.330, `claude-haiku-5-5`, chỉ công cụ tệp); agy mất xác thực (A11) nên chỉ có hàng mô tả. Hai nhánh bằng chứng: không có, hoặc đầu ra của bộ phát hiện E1 chạy trên bản sao làm việc, đưa dưới dạng văn bản ("có thể báo sai và bỏ sót"). Ba mức kiểm soát: **advice** (chỉ đọc, báo cáo JSON; một can thiệp của người là đọc báo cáo), **approve** (một lần gọi cho mỗi id vấn đề đã báo, 17 bước mỗi chuỗi; một bước chỉ được nhận nếu oracle không báo hồi quy, nếu không bị hoàn lại) và **autonomous** (một lần gọi, tự tìm và sửa hết). Ba lần lặp cho mỗi tổ hợp bên sửa nhân nhánh: 12 chuỗi đầy đủ cho codex và claude, 36 hàng được chấm.

**Ai phán.** Chất lượng bản sửa do `oracle/oracle.py` phán, một bộ kiểm tra tĩnh độc lập với bên sửa; **không bao giờ do lời tự khai của bên sửa**: với mỗi vấn đề gieo còn hiện diện, đã sửa hoặc sửa bằng chất liệu yếu hơn; các lan can hồi quy (G1 nguồn cân bằng, G7 "cleartext tắt kéo theo base URL https", và các lan can khác); sự làm yếu thêm; mồi bị đụng. Mọi cây đã sửa còn được biên dịch (`:app:compileDebugKotlin`); build chưa bắt đầu được tính là hạ tầng, không phải lỗi biên dịch. Báo cáo được chấm với danh sách chuẩn theo khóa (id, tên tệp).

**Bảng 5.24.** E5, theo ô (độ chính xác và độ phủ theo vấn đề gieo, ba lần lặp mỗi ô; "cây biên dịch" là số lần lặp có cây sửa biên dịch được; Wilson 95% trong ngoặc).

| bên sửa | nhánh bằng chứng | mức | vấn đề gieo tìm hoặc sửa (trên 14 mỗi lần lặp) | độ chính xác của báo cáo (tp/đã báo) | độ chính xác của khẳng định đã sửa | lần lặp có hồi quy | cây biên dịch |
|---|---|---|---|---|---|---|---|
| claude | evidence | advice | 42/42 = 1,00 (0,92 đến 1,00) | 51/59 = 0,86 (0,75 đến 0,93) | - | 0/3 | - |
| claude | evidence | approve | 33/42 = 0,79 (0,64 đến 0,88) | - | - | 0/3 | 2/3 |
| claude | evidence | autonomous | 37/42 = 0,88 (0,75 đến 0,95) | - | 37/41 | 1/3 | 3/3 |
| claude | none | advice | 42/42 = 1,00 (0,92 đến 1,00) | 50/61 = 0,82 (0,71 đến 0,90) | - | 0/3 | - |
| claude | none | approve | 34/42 = 0,81 (0,67 đến 0,90) | - | - | 0/3 | 3/3 |
| claude | none | autonomous | 39/42 = 0,93 (0,81 đến 0,98) | - | 39/42 | 2/3 | 3/3 |
| codex | evidence | advice | 42/42 = 1,00 (0,92 đến 1,00) | 51/54 = 0,94 (0,85 đến 0,98) | - | 0/3 | - |
| codex | evidence | approve | 32/42 = 0,76 (0,61 đến 0,87) | - | - | 0/3 | 0/3 |
| codex | evidence | autonomous | 42/42 = 1,00 (0,92 đến 1,00) | - | 42/42 | 0/3 | 0/3 |
| codex | none | advice | 42/42 = 1,00 (0,92 đến 1,00) | 51/57 = 0,89 (0,79 đến 0,95) | - | 0/3 | - |
| codex | none | approve | 35/42 = 0,83 (0,69 đến 0,92) | - | - | 0/3 | 0/3 |
| codex | none | autonomous | 42/42 = 1,00 (0,92 đến 1,00) | - | 42/42 | 0/3 | 3/3 |

**Đọc bảng.** *Advice*: độ phủ 14 vấn đề gieo là 1,00 ở cả bốn ô (168/168 trên 12 chuỗi), một trần không còn chỗ để thấy tác động của nhánh bằng chứng. *Autonomous*: codex sửa 84/84 vấn đề gieo, claude 76/84 (0,90), với khẳng định chính xác 84/84 ở codex và 76/83 ở claude. *Approve*: 67/84 (0,80) cho cả hai. Chi phí trung bình mỗi chuỗi tăng theo mức kiểm soát: advice khoảng 25.478 đến 50.406 token và 34 đến 63 giây; autonomous 110.447 đến 179.636 token và 92 đến 180 giây; approve 557.115 đến 985.315 token và 201 đến 523 giây (`stats.md`).

**Có bằng chứng so với không có.** Hiệu (có trừ không), 6 cặp mỗi hàng: advice, độ phủ +0,000 (p = 1,000); autonomous, số đã sửa −0,024 (p = 0,750); approve −0,048 (p = 0,250). Cho bộ phát hiện xem không tạo khác biệt đo được ở mức nào. Độ chính xác của báo cáo hơi cao hơn khi có bằng chứng ở cả hai bên sửa (claude 0,86 so với 0,82; codex 0,94 so với 0,89) nhưng các khoảng chồng nhau và không có kiểm định.

**Báo sai ở các báo cáo advice.** S03 ×12, P05 ×6, S09 ×3, U04 ×3, U05 ×3, P03 ×2. Trong 29 hàng bị chấm sai, 17 là S03, U05 hoặc P03 báo ở tệp khác với danh sách chuẩn: S03 12 lần (một lần mỗi lần advice), thường chỉ vào base URL `http://10.0.2.2` ở `CatalogRepository.kt`, một vấn đề cleartext có thật nhưng danh sách chuẩn ghi ở cờ manifest; U05 và P03 là vấn đề có thật không gieo. Khóa (id, tệp) nghiêm ngặt được cố định trước lần chạy và được giữ, nên độ chính xác ở trên là cận dưới.

**Cây sửa có biên dịch được không.** Mười lỗi biên dịch (codex 9 trên 12 cây đã sửa: autonomous có bằng chứng 3/3, approve 6/6; claude 1 trên 12) đều mang cùng lỗi Gradle: "An exception occurred applying plugin request [id: 'androidx.baselineprofile']" (một lần "A problem occurred configuring project ':baselineprofile'"). Các diff codex được kiểm tra thêm `id("androidx.baselineprofile")` vào `app/build.gradle.kts` (vấn đề có thật P11, không nằm trong độ phủ). Sự chỉnh sửa là của bên sửa nên tính là **lỗi gây ra bởi bản sửa**, không phải hạ tầng; dòng nguyên nhân sau lỗi plugin chưa được kiểm tra sâu hơn.

#### 5.6.1. Mức kiểm soát cần bao nhiêu?

Người duyệt ở mức approve là một đoạn mã (các lan can của oracle), không phải người; nhóm C của người nằm ngoài phạm vi; nên chi phí của người được đo bằng số can thiệp (advice 1 mỗi lần chạy, approve 17 quyết định mỗi lần chạy, autonomous 0) và lan can là đại diện cho một người duyệt cẩn thận.

**Bảng 5.25.** E5, ba mức kiểm soát (gộp hai bên sửa).

| mức | vấn đề gieo đã sửa, codex / claude | cây cuối có lan can hồi quy | cây biên dịch, codex / claude | lần chạy đụng mồi |
|---|---|---|---|---|
| advice (không sửa gì) | độ phủ 1,00 / 1,00 | 0 | - | 0 |
| approve, từng bước với lan can | 0,80 / 0,80 | 0 trên 12 | 0 trên 6 / 5 trên 6 | 11 trên 12 |
| autonomous | 1,00 / 0,90 | 3 trên 12 (đều claude, lan can G7) | 3 trên 6 / 6 trên 6 | 10 trên 12 |

- Lan can làm đúng việc của nó với điều nó kiểm tra: các vòng approve hoàn lại 10 bước (claude 3, codex 7; 9 ở G7 và 1 ở G1) và không cây approve nào kết thúc với lan can hỏng. Claude ở autonomous phá G7 ở 3 trên 6 lần lặp (tắt cleartext trong khi base URL vẫn là http).
- Cái giá là độ phủ: một bước bị hoàn lại không được thử lại, nên approve sửa ít vấn đề gieo hơn autonomous (0,80 so với 1,00 và 0,90).
- Một lan can không chạy trình biên dịch và không nhìn mồi sẽ không bắt được hai loại hỏng còn lại: cây codex không còn build (cả 6 cây approve qua lan can và hỏng ở biên dịch), và mồi bị đổi. Mồi bị đổi ở 21 trên 24 lần chạy approve và autonomous: `Stats` (remember và derivedStateOf) ở gần như tất cả, `TokenStore` có log chặn debug ở cả 6 lần chạy autonomous của codex và một của claude, `RecentRow` có key ở 4 trên 6 lần autonomous của codex và 2 trên 6 lần approve của codex.
- Vậy ở mục tiêu này, advice đủ tin cậy để giao cho người (mọi vấn đề gieo được tìm mọi lần, độ chính xác 0,82 đến 0,94); codex ở autonomous sửa hết vấn đề gieo nhưng đổi cả mã đúng và làm hỏng build ở một nửa số cây; approve từng bước với lan can tĩnh giảm các hồi quy mà nó nhìn thấy, đổi lấy độ phủ. Người đọc từng diff có làm tốt hơn người duyệt bằng mã hay không thì **chưa được thử**.

#### 5.6.2. Các bên sửa và hàng của agy

Hai bên sửa chính là codex và claude (12 chuỗi đầy đủ, 36 hàng được chấm, `results/runs.csv` có 41 hàng được chấm trong đó 5 hàng của agy). Agy chỉ có một chuỗi mỗi nhánh và **không bao giờ gộp** (A11, A12): `e5-agy-none-r1` advice dừng ở `harness-halt` (agy không giao diện cần một quyền mà nó không hỏi được) và hàng approve phụ thuộc không làm gì (0 bước, diff rỗng); `e5-agy-evidence-r1` advice chạy xong (độ phủ 1,0; 17 tp trên 18 báo); các hàng autonomous của cả hai nhánh sửa 13 vấn đề, ở nhánh không có bằng chứng phá lan can G7. Chuỗi `e5-agy-evidence-r1` ở mức approve không có hàng nào (chuỗi bị dừng trước khi agy mất xác thực), nên theo mức này agy chỉ là mô tả và không có phần nào của nó nằm trong các số ở trên.

**Oracle, và các cây "qua lan can nhưng hồi quy".** Oracle là bộ kiểm tra theo mẫu và cấu trúc: "đã sửa" nghĩa là mẫu biến mất và các lan can giữ; mục tiêu không có bộ kiểm thử và không có phép đo E2 nào trên cây đã sửa, nên giao thức §4.5 (bản sửa do bộ kiểm thử độc lập và số đo E2 phán) được thay bằng oracle tĩnh cộng cổng biên dịch; một cây biên dịch được và qua lan can **chưa được chứng minh** hành xử hay chạy nhanh như cũ. Các trường hợp "qua lan can nhưng hồi quy" là sáu cây approve của codex (qua lan can, không biên dịch được) và các lần mồi bị đổi (21 trên 24 lần chạy).

**Hình 5.18.** E5: tìm, sửa và cây có build được, theo bên sửa, nhánh bằng chứng và mức kiểm soát (agy không vẽ, A11). Số liệu vẽ: `figures/out/F20-e5.data.csv`.

![F20](figures/out/F20-e5.svg)

### 5.7. Tổng hợp theo giả thuyết

**Bảng 5.26.** Kết luận theo quy tắc đã đăng ký (nhánh chính v1 của E1; p hiệu chỉnh Holm ở Bảng 5.27).

| giả thuyết | điểm cuối | kết quả | độ lớn và khoảng | kết luận theo quy tắc | xác nhận? |
|---|---|---|---|---|---|
| H1 | D01 hoặc D02 | 66/66 lần chạy không guardrail | 1,000 [0,945; 1,000] | ủng hộ (dải PB); không đặc trưng cho AI: người viết cũng có D02 ở 17/20 và 18/20 | không |
| H2 (C4) | insecure-workaround, E4 | 17/148 | 0,115 [0,073; 0,176]; p thô 0,311 | **không được ủng hộ** tại SESOI 0,15; không đủ bác bỏ (cận trên 0,176 không dưới 0,10) | có |
| H3 (C5) | P90 `FrameDurationCpuMs`, P08, A so với B | 62,87 so với 49,07 ms (n = 12 và 12) | delta% +28,13% [13,38; 54,31]; p thô 0,000308 | **ủng hộ** (ba điều kiện thỏa, đường GPU swangle_indirect, A14); bộ nhớ đồ họa không đo | có |
| H4 | tọa độ Gradle không tồn tại | 0 `hallucinated-artifact` trong 2105 thể hiện (REPORT E1; Hình 5.10) | cận trên theo quy tắc ba 0,14% | **bác bỏ** theo quy tắc (0 trong ít nhất 300 thể hiện); 1 `hallucinated-version` sau sửa tra cứu | không |
| H5 | U01 hoặc U05 | 1/66 (chỉ U01; U05 chưa kiểm chứng) | 0,015 [0,003; 0,081] | bác bỏ cho U01 (dải PB) | không |
| H6 | S05 | 10/66 | 0,152 [0,084; 0,257] | không kết luận được | không |
| G1 (C1) | ít nhất một phát hiện S, naive trừ guardrailed | 31/33 so với 17/34 | hiệu +0,439, p thô 0,00004; ở các ứng dụng build được +0,000 | **ủng hộ** (p hiệu chỉnh 0,00016; độ lớn từ 0,15); không đúng ở ứng dụng build được | có |
| G2 (C2) | số mã luật khác nhau mỗi lần | 5,85 so với 2,82 | hiệu +3,025, p thô 0,00001 | **ủng hộ** (p hiệu chỉnh 0,00005; độ lớn từ 2,0); giữ ở ứng dụng build được (+3,829) | có |
| G3 | tập được chỉ dẫn so với tập bị giữ lại | chênh +0,63 và +1,30 | [0,15; 1,14] và [0,75; 1,84] | ủng hộ theo quy tắc; luật bị giữ lại cũng giảm: "khái quát hóa hoặc ngẫu nhiên" | không (thứ cấp có đăng ký) |
| B1 (C3) | nhóm S trên 1000 dòng, AI không guardrail so với trước 30/11/2022 | 2,37 so với 1,02 | delta Cliff +0,582, p thô 0,00009 | **ủng hộ** (p hiệu chỉnh 0,00027; delta từ 0,33); delta của claude-haiku đơn lẻ +0,340 | có |

**Hình 5.19.** Bảng kết luận theo giả thuyết (chép từ Bảng 5.26). Số liệu vẽ: `figures/out/F21-verdicts.data.csv`.

![F21](figures/out/F21-verdicts.svg)

**Hiệu chỉnh Holm trên C1 đến C5.** Holm–Bonferroni bước xuống trên năm phép kiểm định xác nhận (m = 5, ngưỡng của bậc đầu 0,01; Mục 4.8). Phép tính này là của bài, không lấy từ REPORT: p hiệu chỉnh của bậc k là p thô nhân (5 − k + 1), rồi lấy cực đại lũy tiến theo thứ tự tăng của p thô, từ các p thô đã in ở REPORT của từng thí nghiệm (C4 lấy ở `c4_confirmatory.csv`, 0,3106); vì vậy p hiệu chỉnh cũng chỉ chính xác ở độ làm tròn đã in.

**Bảng 5.27.** Hiệu chỉnh Holm và kết luận theo điều kiện đầy đủ của từng điểm cuối (Mục 4.8).

| điểm cuối | p thô | bậc | ngưỡng của bậc | p hiệu chỉnh | độ lớn so với ngưỡng đã đăng ký | kết luận |
|---|---|---|---|---|---|---|
| C2 | 0,00001 | 1 | 0,01 | 0,00005 | +3,025 ≥ 2,0 (đạt) | ủng hộ |
| C1 | 0,00004 | 2 | 0,0125 | 0,00016 | +0,439 ≥ 0,15 (đạt) | ủng hộ |
| C3 | 0,00009 | 3 | 0,0167 | 0,00027 | delta Cliff +0,582 ≥ 0,33 (đạt) | ủng hộ |
| C5 | 0,000308 | 4 | 0,025 | 0,000616 | delta% +28,13% ≥ +10% (đạt); khoảng [13,38; 54,31] không chứa 0 (đạt) | ủng hộ |
| C4 | 0,3106 | 5 | 0,05 | 0,3106 | tỷ lệ 0,115 < 0,15 (không đạt) | không được ủng hộ |

Mỗi kết luận theo đủ điều kiện đã đăng ký: C1, C2, C3 và C5 có p hiệu chỉnh dưới 0,05 và độ lớn vượt ngưỡng (C5 còn thêm khoảng không chứa 0); C4 không đạt cả hai điều kiện (p hiệu chỉnh 0,3106 không dưới 0,05, tỷ lệ 0,115 dưới 0,15). Hiệu chỉnh Holm không đổi kết luận nào: p thô của C1, C2, C3 và C5 đều đã dưới ngưỡng của bậc của chúng, và p hiệu chỉnh không thể nhỏ hơn p thô. Đọc cùng các hạn chế của từng mục: C1 +0,439 không còn ở các ứng dụng build được (+0,000, Mục 5.2.3), C3 so với tầng người viết gồm ứng dụng phổ biến và lớn (Mục 5.2.5), sự làm yếu ở C4 tập trung ở hai lỗi và hai bộ công cụ (Mục 5.5), và C5 là một cấu trúc trên một đường GPU của emulator (Mục 5.3.3). "Được ủng hộ" ở đây nghĩa là điều kiện thống kê đã đăng ký thỏa; nguyên nhân (quá trình sinh mã) vẫn chỉ là giả thuyết khi không có nhánh thực nghiệm nào kiểm tra nó.

**Các lớp của Mục 2.6.** *Lớp nền (vấn đề có tần suất và chi phí).* Chi phí đo được cho hai kỹ thuật nền và cho năm nhân tố lấy từ mã E1, và có ba nhóm: (1) hiệu ứng lớn, độc lập thiết bị, bị loại bởi bản sửa theo sách giáo khoa: P06 (byte giải mã −96,9%) và P01 cùng P09 (TTID −69,9%, khung chậm từ 90,05% xuống 1,255%); (2) hiệu ứng lên chính thao tác nhưng không tới khung hình: P13 (tổng thời gian truy vấn 634,9 ms còn 85,42 ms, khung hình không cải thiện); (3) không đo được chi phí: P03 cùng P04 và nhân tố con P01, ở kịch bản này. P08 (C5) là chi phí khung hình thấy được: +28,13% ở P90, trên một đường GPU của emulator. Hai kỹ thuật nền: R8 giảm APK 90,4% và khởi động nhỏ nhưng thấy được (−8,7% `timeToInitialDisplayMs`), Baseline Profile giảm khởi động 5,88% (Bảng 5.9, 5.10). Về tần suất ở E1: P11 phổ quát ở hai kiểu không guardrail (33/33) và gần như vắng ở guardrailed (7/34), P10 ở nhánh v2 là 30/33, 33/33 và 0/34; các mẫu cấp mã nằm từ cỡ một phần năm (P13, P01) đến dưới một phần mười (P06, P03, P04, P09), P08 cỡ một phần tám. Bảng 5.20 chỉ xếp hạng mô tả. *Lớp quá trình sinh (H1 đến H4).* H1 đúng cho mọi lần chạy nhưng không đặc trưng; H2 không đạt SESOI; H4 bị bác bỏ (0 artifact ảo trong 2105 thể hiện). Cái khác với lớp nền là phần "tại sao": E1 cho thấy một tệp guardrail làm giảm độ rộng và độ phổ biến của S ở mức mã nguồn (G1, G2), nhưng ở các ứng dụng build được tác động tới "có hay không" biến mất. Điều E2 không nói được là chi phí của mọi biến thể: mỗi nhân tố đo một ứng viên của E1, trên emulator.

### 5.8. Hạn chế thực tế phát sinh trong lúc chạy

Cập nhật Mục 4.10, lấy từ mục "mối đe dọa" và "khai báo" của từng REPORT:

- **Cách ly và harness.** Bộ khởi chạy claude tải ba mod người dùng và năm plugin tích hợp từ `~/.agents/mods` thật vì tệp cấu hình hệ thống `/etc/claude-code/managed-settings.json` bật chúng, và thư mục home giả không ghi đè được cấu hình hệ thống (`isolation-audit/AUDIT.md`, R1; áp dụng cho mọi lần chạy claude ở E1, E4, E5 và E6; kiểm toán 623 bản ghi hội thoại không thấy văn bản chỉ dẫn nào của cấu hình riêng); công cụ vẫn giới hạn ở Edit, Glob, Grep, Read, Write, không thấy văn bản chỉ dẫn hay sự kiện hook, và phản hồi stub sạch không bị ảnh hưởng, nhưng rò rỉ không phải bằng không (`canary.json`). Agy ở chế độ không giao diện kết thúc cả lần chạy ở lệnh đầu ngoài danh sách cho phép (một lần chạy E1 rỗng; các lượt dừng harness ở E4 và E5). Hàng rào ghi không chặn mạng; việc dùng mạng và build được kiểm toán từ chuỗi lệnh trong bản ghi.
- **Web.** E1: 0 lần gọi công cụ web trong mọi lần chạy. E4: sai lệch A9 (agy có công cụ tìm kiếm và dùng ở 31/50 lượt đợt đầu; luật chặn không hoạt động cho đến khi dùng hook); tập thứ cấp `agy-web` không gộp.
- **Phiên bản trôi.** Phiên bản mô hình có thể trôi trong cửa sổ sinh của E1 và không bộ công cụ nào lộ nhiệt độ hay hạt giống; Claude Code tự cập nhật từ 2.1.329 sang 2.1.330 giữa E4 (24 và 26 lượt thử chính; theo phiên bản trong `summary.csv`, khám phá).
- **Xác thực.** Agy mất xác thực: E1 chốt ở 101/108 ô (A11, A12), bảy ô r4 thiếu; E5 bỏ agy khỏi bên sửa.
- **Áp lực bộ nhớ máy chủ trong E2.** Bộ nhớ máy cạn nhiều lần: tiến trình OOM killer giết emulator và daemon Gradle (cũng giết phiên của tác nhân khác); lần chạy đầu được xếp là thí điểm (A3), A7 thay tiêu chí swap-in toàn máy bằng lỗi trang chính của qemu, emulator chạy trong scope systemd riêng với `MemorySwapMax=0`.
- **Hết bản ghi tệp của ổ NTFS (ENOSPC dù còn byte trống).** E1 ba lần (một lần build chạy lại; `codex-luna-B3-naive-r4` kết quả cuối là lỗi biên dịch), E3 ba lần quét, E4 một lần, E5 gián đoạn; không mã sinh nào bị sửa; xử lý bằng dọn bản sao thừa, lưu trữ thành một tệp và thử lại.
- **Bộ phát hiện và luật bị loại.** Chỉ có 20 trong 29 luật vào phân tích chính; 9 luật bị loại (Bảng 5.3); độ nhạy theo đột biến là cận trên vì tác giả bộ phát hiện viết các đột biến; luật tĩnh không thấy hành vi lúc chạy; U05 không thể phán bằng cửa sổ 6 dòng; P07 vẫn không đạt cả hai nhánh.
- **Độc lập của người chấm.** Người chấm 1 là tác giả bộ phát hiện và các đột biến, và cũng là người thứ ba phân xử 26 bất đồng, nên phân xử không độc lập với bộ phát hiện; sau khi người chấm 1 xong mỗi bảng, thông báo thay đổi tệp của harness làm người chấm 1 thấy một số dòng của người chấm 2 (41 dòng của bảng v1; bảng v2 hiện đủ: tiêu đề và cả 31 dòng); nhãn của người chấm 1 đã được viết trước và không sửa sau đó. Người chấm 2 không thấy dữ liệu E1, nhưng cũng là tác giả của danh mục và guardrail, nên biết ý đồ luật ngoài cửa sổ mã; nhãn chỉ dựa vào định nghĩa luật và cửa sổ mã.
- **Nhìn sớm.** Một lần chạy phân tích thử trên 13 lần chạy hợp lệ (A6) và số đếm mẫu nổi lên theo bộ công cụ được thấy trước khi open coding xong; điều kiện đăng ký "không thấy số theo nhánh trước khi sửa bộ phát hiện" không đạt, vì thế bộ v1 là chính (A13).
- **E2.** Emulator có GPU phần mềm làm nhiều chi phí kết xuất chỉ mang tính tương đối; Baseline Profile của lab chỉ phủ ứng dụng lab; các lần lặp sau lần đầu có cache trang ấm; P08 đo trên đường GPU `swangle_indirect` còn các nhân tố khác trên `swiftshader_indirect` (A14), nên số tuyệt đối không so được, và lần kiểm soát P01+P09@swangle cho thấy đường GPU đổi cả độ lớn của đối chiếu khung hình (p90 −32,3% so với −78,2%, Mục 5.3.3); mỗi nhân tố lấy một ứng viên của E1 và một kịch bản do người thực hiện chọn, nên cỡ hiệu ứng phụ thuộc kịch bản (P03 cùng P04 không có chi phí ở 300 lượt mượn).
- **E3 và E4.** Xem các mục 5.4 và 5.5: một người phân loại, jadx lần đầu thiếu, hồ sơ phân loại có mâu thuẫn nội bộ; lỗi được soạn thay vì bắt từ thiết bị, phép "giải quyết lỗi" là đọc diff chứ không build, khả năng khác nhau giữa các bộ công cụ không tách được khỏi mô hình.
- **E5.** Một mục tiêu nhỏ với 14 vấn đề gieo mà danh mục trong prompt gọi tên; mức advice cho thấy việc dễ khi danh mục đã được đưa, không cho thấy công cụ tìm được cái chưa gieo; hai bên sửa, ba lần lặp; oracle theo mẫu; bản sửa không được phán bằng bộ kiểm thử hay số đo; người duyệt ở approve là mã; hàng claude ở lần lặp 2 và 3 từng bị chấm như "không sửa được gì" khi giới hạn tốc độ của tài khoản bị từ chối; 12 hàng đó đã bị loại, ghi ở `infra_failed_rows.csv` và chạy lại.


---

## Chương 6. Kết luận và hướng phát triển

> **Phạm vi của chương.** Chương này trả lời từ Bảng 5.26 và các bảng của Chương 5; mỗi câu gắn một kết quả. Điểm cuối xác nhận C5 (chi phí khung hình của hiệu ứng trang trí P08) và hiệu chỉnh Holm nằm ở Mục 5.3.3 và 5.7. Mọi lời khuyên không có kết quả đỡ được ghi là ý kiến.

### 6.1. Tổng kết hiệu quả của AI Coding

**AI tạo ra vấn đề gì, với tần suất nào, so với người viết (RQ1, C3).** Trong bản nháp đầu không có guardrail, vấn đề nhóm bảo mật gần như luôn hiện diện: ít nhất một phát hiện nhóm S ở 31/33 lần chạy naive (0,939) và 29/33 lần detailed-functional (0,879); phổ biến nhất là allowBackup (S06: 0,88 và 0,73), tiếp theo là lưu token hoặc dữ liệu cá nhân không mã hóa (S01: 8/33 và 11/33), màu và cỡ chữ cứng (U04: 9/33 và 13/33) và nhật ký nhạy cảm (S05: 4/33 và 6/33). Phụ thuộc cũ hoặc API phản đối có ở mọi lần chạy không guardrail (H1, 66/66), nhưng người viết cũng có. Trên mỗi 1000 dòng, mật độ phát hiện nhóm S của AI không guardrail cao hơn tầng ứng dụng trước trợ lý (trung vị 2,37 so với 1,02; delta Cliff +0,582, vượt ngưỡng 0,33 theo C3). Phải đọc cùng: chỉ 42 trên 100 bản nháp build được; ứng dụng do người viết lớn hơn và là ứng dụng phổ biến được bảo trì; và với nhiều luật hiệu năng, tỷ lệ "có ít nhất một" của AI **thấp hơn** người viết (ví dụ P01 0,18 so với 0,70), có thể do bản nháp AI ngắn hơn và ít cấu trúc danh sách hơn (giả thuyết, chưa kiểm tra).

**Chi phí đo được (RQ2).** Chi phí của các vấn đề có thật cho ba trong năm nhân tố lấy từ mã E1, và ba nhóm tách rõ. (1) Hiệu ứng trang trí P08 (C5, được ủng hộ): ô lưới của một lần chạy E1 tốn thêm +28,13% [13,38; 54,31] ở P90 thời gian CPU khung hình so với bản phẳng (62,87 so với 49,07 ms, p hiệu chỉnh 0,000616), trên một đường GPU của emulator; bộ nhớ đồ họa không được đo. (2) P06 và P01 cùng P09 có hiệu ứng lớn, độc lập thiết bị, khi áp dụng bản sửa theo sách giáo khoa: byte bitmap giải mã −96,9%, `timeToInitialDisplayMs` −69,9% và khung chậm từ 90,05% xuống 1,255%. (3) P13 làm nhanh truy vấn (tổng thời gian 634,9 ms còn 85,42 ms) nhưng không cải thiện khung hình; P03 cùng P04 và nhân tố con P01 không có chi phí đo được ở kịch bản này. Với hai kỹ thuật nền, chi phí của việc thiếu cấu hình phát hành là có thật: bật R8 giảm kích thước APK 90,4% (2,474e+07 byte còn 2,384e+06 byte trong `summary.csv`) và giảm `timeToInitialDisplayMs` 8,7%; bật Baseline Profile giảm `timeToInitialDisplayMs` 5,88%. Phải đọc cùng: mọi số đo ms chạy trên emulator có GPU phần mềm; mỗi nhân tố đo một ứng viên của E1 trong một kịch bản do người thực hiện chọn; P08 chạy trên đường GPU khác (A14), và lần kiểm soát thăm dò ở Mục 5.3.3 cho thấy đường GPU làm co đối chiếu khung hình của một nhân tố khác.

**Phát hiện và khắc phục (RQ3, RQ5).** Công cụ độc lập trả lời một nửa câu hỏi: MobSF cho độ chính xác 0,291 sau phân loại thủ công (từ 0,244 đến 0,806 tùy cách đọc nhãn), và cả 30 phát hiện đúng là sự kiện về manifest, cấu hình build hoặc chữ ký, không phải điểm yếu ở mã; bộ phát hiện E1 thấy cái MobSF không thấy (lưu token plaintext, ghi nhật ký BODY) nhưng ở mức luật của nó. Trợ lý ở vai trò người tối ưu, khi được gợi ý danh mục, tìm đủ 14 vấn đề gieo ở mọi lần chạy advice (168/168; trần), sửa đủ 84/84 ở codex và 76/84 ở claude khi tự trị, nhưng 9 trên 12 cây codex đã sửa không biên dịch được, và mồi bị đổi ở 21 trên 24 lần chạy sửa.

**Có nên tin hoàn toàn vào đề xuất của AI không (RQ4, C4, E5).** Không, nhưng bằng chứng không ủng hộ khẳng định chung "trợ lý làm yếu bảo mật để sửa lỗi ở tần suất đáng kể": C4 là 17/148 = 0,115 [0,073; 0,176], p thô 0,311, dưới ngưỡng 0,15, **không được ủng hộ**. Bằng chứng ủng hộ một khẳng định hẹp hơn: sự làm yếu tập trung ở hai trong năm lỗi (cleartext và xác thực Keystore) và ở hai trong ba bộ công cụ (agy 8/10 và codex 4/10 ở C5; claude 0/50 cả năm lỗi), ở các lượt thử và lỗi này. Ở E5, lời khẳng định của bên sửa (84/84 chính xác ở codex) không bảo đảm cây build được.

### 6.2. Những hạn chế khi giao việc tối ưu cho AI

Từ E4: loại bản sửa không đạt không phải là hiệu năng (C2, C3 và C4 không có sự làm yếu nào trong 88 lượt thử) mà là bảo mật cấu hình: bật cleartext trên cấu hình chính (S03, 5/30) và bỏ yêu cầu xác thực khỏi khóa Keystore (X01, 12/30), nơi sự làm yếu là bỏ chính thuộc tính gây lỗi; đọc lại mù ba diff C5 bị tranh cãi cho thấy cả những bản "secure-fix" có thể không giải quyết lỗi (15/30 so với 18/30). Từ E5: hồi quy do bản sửa gây ra là ba loại, và lan can chỉ bắt được loại nó nhìn: phá "cleartext tắt kéo theo https" (3/12 cây autonomous, claude; lan can G7), cây không biên dịch (9/12 cây codex đã sửa; lỗi plugin baselineprofile do bản sửa thêm vào) và mồi bị đổi (21/24). Mức can thiệp của người: advice 1, approve 17, autonomous 0; approve giảm hồi quy nhìn thấy với giá độ phủ 0,80, và người duyệt ở E5 là mã chứ không phải người. Hạn chế của chính nghiên cứu: emulator và không thiết bị thật, một miền (thư viện) và chỉ Android, cỡ mẫu và lực kiểm định (E1 33, 33, 34 dưới mục tiêu 36; E4 148 so với 150; E5 ba lần lặp một mục tiêu), ba bộ công cụ trộn lẫn harness và mô hình, và độ độc lập chưa trọn của việc chấm (Mục 5.8).

### 6.3. Vai trò kiểm duyệt của lập trình viên

Quy tắc "trợ lý không tự xác nhận" đứng vững trên dữ liệu này, trong phạm vi hẹp của E5 và E3. Ở E5, tự khai của bên sửa không đủ: codex khẳng định đúng 84/84 ở autonomous nhưng 3 trên 6 cây không biên dịch được, và chỉ một oracle tĩnh cộng cổng biên dịch phát hiện điều đó; trong approve, các lan can của oracle hoàn lại 10 bước, nhưng cũng không thấy lỗi biên dịch lẫn mồi bị đổi. Ở E3, MobSF mặc định chỉ cho độ chính xác 0,291 và cả 22 cảnh báo MASVS-PLATFORM đều sai: cần phân loại thủ công. Công cụ độc lập và người duyệt bổ sung cho nhau: MobSF (manifest, build, chữ ký) và bộ phát hiện (mã) thấy hai tầng khác nhau (kappa 0,581 so với mọi cảnh báo MobSF, 0,78 so với các tp đã phân loại), và ba phát hiện chỉ có ở bộ phát hiện đều cần đối chiếu với sản phẩm (hai thật, một không khai thác được). Một người đọc từng diff có bắt được nhiều hơn người duyệt bằng mã hay không, **không được kiểm tra** (nhóm C nằm ngoài phạm vi).

### 6.4. Đề xuất quy trình ứng dụng AI vào phát triển phần mềm thực tế

Mỗi khuyến nghị gắn một kết quả; khuyến nghị không có kết quả đỡ được ghi rõ là ý kiến.

1. **Đặt tệp guardrail về chất lượng trong ngữ cảnh gốc của công cụ** (C1, C2, G3). Bằng chứng: guardrail giảm tỷ lệ bản nháp có phát hiện S từ 0,939 xuống 0,500 và số mã luật khác nhau từ 5,85 xuống 2,82; nó cũng làm giảm các luật không được nêu (G3: +1,10 và +0,71 phát hiện mỗi lần chạy, "khái quát hóa hoặc ngẫu nhiên"). Nội dung chức năng chi tiết thôi thì không đủ (0,879 so với 0,939). **Giới hạn:** ở ứng dụng build được, guardrail không giảm việc có hay không có phát hiện S (C1 +0,000), nên nó không thay cho kiểm tra.
2. **Có cổng kiểm tra độc lập ở khâu nhận mã do trợ lý sửa:** một bước biên dịch và một bước phân loại thủ công kết quả công cụ tĩnh (E5: 9/12 cây codex không biên dịch được dù khẳng định đúng; E3: 0,291 độ chính xác mặc định). Đây là kết quả của E3 và E5, trên một mục tiêu và 13 sản phẩm.
3. **Xem xét kỹ mọi diff chạm cấu hình mạng hoặc khóa Keystore khi nhờ trợ lý sửa lỗi** (E4): sự làm yếu tập trung ở hai lỗi đó (cleartext 5/30, Keystore 12/30). Đây là kết quả mô tả từ năm lỗi và ba bộ công cụ, **không** phải quy tắc chung "cấm làm yếu cấu hình": C4 gộp không được ủng hộ, và claude không làm yếu lần nào trong 50 lượt thử.
4. **Dùng Kotlin từ 2.0.20 trở lên để P03 (tham số không ổn định) không còn tốn khi bật strong skipping mặc định** (Mục 2.2.3, [Compose-StrongSkipping]). Đây là ý kiến dựa trên tài liệu, không dựa trên kết quả của nghiên cứu này (ở E2, P03 cùng P04 không có chi phí đo được với Kotlin 2.4.21, Mục 5.3.3).
5. **Ý kiến, không có kết quả đỡ:** chạy cả bước tối ưu hóa phát hành (R8, Baseline Profile) ngay từ bản nháp đầu: P10 và P11 gần như phổ quát ở bản nháp không guardrail, và hai kỹ thuật này có lợi đo được (Bảng 5.9, 5.10), nhưng nghiên cứu không so sánh quy trình có và không có bước đó.

### 6.5. Hướng phát triển

Từ các mối đe dọa chưa xử lý: (i) đo trên thiết bị thật; (ii) nhóm C do người thực hiện, nghiên cứu UX (SUS, tỷ lệ hoàn thành tác vụ) và người duyệt thật thay cho người duyệt bằng mã ở E5; (iii) trợ lý có truy xuất (retrieval) và người kiểm duyệt, thay cho bản nháp đầu không trạng thái; (iv) miền ứng dụng khác và iOS; (v) hoàn tất các phần còn mở của chính nghiên cứu: hai lần chạy khám phá của E2 (độ nhạy với biến thể A viết tay, quét số lượng lượt mượn), xác nhận S11 ở nhánh chính (hiện S11 chỉ được kiểm chứng bằng phân giải ở các nhánh đã sửa tra cứu), kiểm chứng lại P07, P05, P06, S04, S08, U05 bằng mẫu mới, tần suất theo bộ công cụ và phân bố mật độ hiệu ứng ở E1, xác thực lại agy để lấp bảy ô, và quét ứng dụng do người viết ở E3; (vi) **tìm lại các truy vấn tài liệu ở Mục 1.2 trước khi nộp bài**, vì các khẳng định "chưa có nghiên cứu" dựa trên nhật ký 10 truy vấn.


<!-- REFS-BEGIN -->
---

## Tài liệu tham khảo

*Danh sách này được sinh tự động từ `references.md` cho các khóa đã dùng trong bài; mỗi mục trong `references.md` ghi cách đã kiểm chứng và từng con số đã dùng. Bản thảo arXiv chưa qua bình duyệt được ghi rõ trong mục tương ứng. Các tài liệu nội bộ của dự án được dẫn là [Protocol §n] (`00-protocol.md`) và "đề cương" (tệp docx `De_cuong_AI_Coding_UI_UX_Hieu_nang_Bao_mat_ban_mo_rong.docx`).*

- **[A11y-Scanner]** Get started with Accessibility Scanner, Android Accessibility Help. https://support.google.com/accessibility/android/answer/6376570
- **[A11y-Testing]** Test your app’s accessibility, Android Developers (UI Check Mode flags low contrast). https://developer.android.com/guide/topics/ui/accessibility/testing
- **[Androcec2026]** Andročec, D. "Vibe Coding and Web Application Security: A Twin-Prompt Study." arXiv:2608.20963 (preprint, 2026). https://arxiv.org/abs/2608.20963
- **[Android-ANR]** ANRs, Android Developers (App quality). https://developer.android.com/topic/performance/vitals/anr
- **[Android-Crypto]** Cryptography, Android Developers. https://developer.android.com/privacy-and-security/cryptography
- **[Android-Exported]** android:exported, Android Developers (OWASP category MASVS-PLATFORM). https://developer.android.com/privacy-and-security/risks/android-exported
- **[Android-Jank]** Slow rendering (jank), Android Developers. https://developer.android.com/topic/performance/vitals/render
- **[Android-Keystore]** Android Keystore system, Android Developers. https://developer.android.com/privacy-and-security/keystore
- **[Android-Launch]** App startup time, Android Developers. https://developer.android.com/topic/performance/vitals/launch-time
- **[Android-Lint]** Improve your code with lint checks, Android Studio docs. https://developer.android.com/studio/write/lint
- **[Android-Memory]** Overview of memory management, Android Developers. https://developer.android.com/topic/performance/memory-overview
- **[Android-NSC]** Network security configuration, Android Developers. https://developer.android.com/privacy-and-security/security-config
- **[Android-TLS]** Security with network protocols, Android Developers. https://developer.android.com/privacy-and-security/security-ssl
- **[Android12]** Behavior changes: Apps targeting Android 12, Android Developers. https://developer.android.com/about/versions/12/behavior-changes-12
- **[AndroidX-Security]** Security release notes (androidx.security), Android Developers. https://developer.android.com/jetpack/androidx/releases/security
- **[AsyncTask]** AsyncTask, Android API reference. https://developer.android.com/reference/android/os/AsyncTask
- **[AutoBackup]** Back up user data with Auto Backup, Android Developers. https://developer.android.com/identity/data/autobackup
- **[Baseline-Config]** Configure Baseline Profile generation, Android Developers. https://developer.android.com/topic/performance/baselineprofiles/configure-baselineprofiles
- **[Baseline-Overview]** Baseline Profiles overview, Android Developers. https://developer.android.com/topic/performance/baselineprofiles/overview
- **[Central-Namespace]** Sonatype. "Register a Namespace", Maven Central (Central Portal) documentation. https://central.sonatype.org/register/namespace/
- **[ChatGPT2022]** OpenAI. "Introducing ChatGPT." 30 November 2022. https://openai.com/index/chatgpt/
- **[Cliff1993]** Cliff, N. "Dominance statistics: Ordinal analyses to answer ordinal questions." Psychological Bulletin 114(3):494–509, 1993. https://doi.org/10.1037/0033-2909.114.3.494
- **[Cohen1960]** Cohen, J. "A Coefficient of Agreement for Nominal Scales." Educational and Psychological Measurement 20(1):37–46, 1960. https://doi.org/10.1177/001316446002000104
- **[Coil]** Coil image loading library (overview and Compose pages). https://coil-kt.github.io/coil/
- **[Coil-Compose]** Coil Compose: AsyncImage sizing. https://coil-kt.github.io/coil/compose/
- **[Compose-A11y-Defaults]** API defaults (touch target sizes), Jetpack Compose accessibility, Android Developers. https://developer.android.com/develop/ui/compose/accessibility/api-defaults
- **[Compose-BestPractices]** Follow best practices, Jetpack Compose performance, Android Developers. https://developer.android.com/develop/ui/compose/performance/bestpractices
- **[Compose-Graphics]** Graphics modifiers, Jetpack Compose, Android Developers. https://developer.android.com/develop/ui/compose/graphics/draw/modifiers
- **[Compose-Lists]** Lazy lists and lazy grids, Jetpack Compose, Android Developers. https://developer.android.com/develop/ui/compose/lists
- **[Compose-Phases]** Jetpack Compose phases, Android Developers. https://developer.android.com/develop/ui/compose/phases
- **[compose-rules]** compose-rules: lint rules for ktlint/detekt for healthier Compose usage (mrmans0n). https://github.com/mrmans0n/compose-rules
- **[Compose-Semantics]** Semantics, Jetpack Compose accessibility, Android Developers. https://developer.android.com/develop/ui/compose/accessibility/semantics
- **[Compose-Stability]** Stability in Compose, Android Developers. https://developer.android.com/develop/ui/compose/performance/stability
- **[Compose-StrongSkipping]** Strong skipping mode, Android Developers. https://developer.android.com/develop/ui/compose/performance/stability/strongskipping
- **[Coroutines-BP]** Best practices for coroutines in Android, Android Developers. https://developer.android.com/kotlin/coroutines/coroutines-best-practices
- **[Deng2026]** Deng, J., Fan, Z., Meng, R. "Understanding the (In)Security of Vibe-Coded Applications." arXiv:2606.23130 v4 (preprint, 2026). https://arxiv.org/abs/2606.23130
- **[detekt]** detekt static analysis for Kotlin. https://detekt.dev/
- **[Efron1979]** Efron, B. "Bootstrap Methods: Another Look at the Jackknife." The Annals of Statistics 7(1):1–26, 1979. https://doi.org/10.1214/aos/1176344552
- **[Fahl2012]** Fahl, S., Harbach, M., Muders, T., Smith, M., Baumgärtner, L., Freisleben, B. "Why Eve and Mallory Love Android: An Analysis of Android SSL (In)Security." ACM CCS 2012, pp. 50–61. https://doi.org/10.1145/2382196.2382205
- **[Fischer2017]** Fischer, F., Böttinger, K., Xiao, H., Stransky, C., Acar, Y., Backes, M., Fahl, S. "Stack Overflow Considered Harmful? The Impact of Copy&Paste on Android Application Security." IEEE S&P 2017, pp. 121–136. https://doi.org/10.1109/SP.2017.31 ; https://arxiv.org/abs/1710.03135
- **[FLAG_SECURE]** WindowManager.LayoutParams.FLAG_SECURE, Android API reference. https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE
- **[Flow-Debounce]** debounce, kotlinx.coroutines Flow API. https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/debounce.html
- **[Freitas2025]** "Image-to-UI Accessibility: Assessing ChatGPT’s Effectiveness in Generating Accessible Android Screens from Figma Templates." Proceedings of the 31st Brazilian Symposium on Multimedia and the Web (WebMedia 2025). (Author list not extracted; cite by title.) https://doi.org/10.5753/webmedia.2025.15298 ; https://sol.sbc.org.br/index.php/webmedia/article/view/37974
- **[Georgiev2012]** Georgiev, M., Iyengar, S., Jana, S., Anubhai, R., Boneh, D., Shmatikov, V. "The Most Dangerous Code in the World: Validating SSL Certificates in Non-Browser Software." ACM CCS 2012, pp. 38–49. https://doi.org/10.1145/2382196.2382204
- **[GitClear2025]** Anderson, T. "AI is eroding code quality, states new in-depth report." DevClass, 20 February 2025, reporting GitClear’s AI Copilot Code Quality 2025 research. SECONDARY report of a vendor white paper (the primary page blocks automated retrieval); not peer-reviewed. https://www.devclass.com/ai-ml/2025/02/20/ai-is-eroding-code-quality-states-new-in-depth-report/1626250
- **[GlobalScope]** GlobalScope, kotlinx.coroutines API (@DelicateCoroutinesApi). https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-global-scope/
- **[Hanley1983]** Hanley, J.A., Lippman-Hand, A. "If Nothing Goes Wrong, Is Everything All Right? Interpreting Zero Numerators." JAMA 249(13):1743–1745, 1983. https://doi.org/10.1001/jama.1983.03330370053031
- **[Hasan2026]** Hasan, A.A., Biswas, S. "What Breaks When LLMs Code? Characterizing Operational Safety Failures of Agentic Code Assistants." arXiv:2605.30777 v3 (2026), DOI 10.1145/3832783.3834393. https://doi.org/10.1145/3832783.3834393 ; https://arxiv.org/abs/2605.30777
- **[Holm1979]** Holm, S. "A Simple Sequentially Rejective Multiple Test Procedure." Scandinavian Journal of Statistics 6(2):65–70, 1979. JSTOR 4615733. https://www.jstor.org/stable/4615733
- **[Huang2024]** Huang, D., Qing, Y., Shang, W., Cui, H. "EffiBench: Benchmarking the Efficiency of Automatically Generated Code." arXiv:2402.02037 v6 (preprint). https://arxiv.org/abs/2402.02037
- **[JADX]** JADX dex to Java decompiler. https://github.com/skylot/jadx
- **[JitPack]** JitPack, package repository for Git (home page). https://jitpack.io/
- **[Landis1977]** Landis, J.R., Koch, G.G. "The Measurement of Observer Agreement for Categorical Data." Biometrics 33(1):159–174, 1977. https://doi.org/10.2307/2529310
- **[Liao2024]** Liao, D., Pan, S., Yang, S., Zhao, Y. et al. "A Comparative Study of Android Performance Issues in Real-world Applications and Literature." arXiv:2407.05090 (preprint). https://arxiv.org/abs/2407.05090
- **[Liao2026]** Liao, D., Pan, S., Xing, Z., Sun, X. "Many a Little Makes a Mickle: A Code-Centric Empirical Study of Data Minimization Principle in Android App Development." arXiv:2606.02960 (preprint, 2026). https://arxiv.org/abs/2606.02960
- **[Liu2014]** Liu, Y., Xu, C., Cheung, S.C. "Characterizing and Detecting Performance Bugs for Smartphone Applications." ICSE 2014, pp. 1013–1024. https://doi.org/10.1145/2568225.2568229
- **[LiuChrome2025]** Liu, Y., Xing, Z., Pan, S., Tantithamthavorn, C. "When AI Takes the Wheel: Security Analysis of Framework-Constrained Program Generation." arXiv:2510.16823 (preprint, 2025). https://arxiv.org/abs/2510.16823
- **[Load-Bitmaps]** Loading large bitmaps efficiently, Android Developers. https://developer.android.com/topic/performance/graphics/load-bitmap
- **[Macrobenchmark]** Macrobenchmark overview, Android Developers. https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- **[Mahmud2024]** Mahmud, T., Duan, B., Che, M., Yasmin, A. et al. "Automated Update of Android Deprecated API Usages with Large Language Models" (GUPPY). arXiv:2411.04387 (preprint). https://arxiv.org/abs/2411.04387
- **[Mahmud2026]** Mahmud, T., Che, M., Yang, G. "A Systematic Study of Deprecated Android API Usage Updates." arXiv:2610.19150 (preprint, 2026). https://arxiv.org/abs/2610.19150
- **[Mann1947]** Mann, H.B., Whitney, D.R. "On a Test of Whether one of Two Random Variables is Stochastically Larger than the Other." The Annals of Mathematical Statistics 18(1):50–60, 1947. https://doi.org/10.1214/aoms/1177730491
- **[MASTG]** OWASP MASTG (Mobile Application Security Testing Guide). https://mas.owasp.org/MASTG/
- **[MASVS]** OWASP MASVS (Mobile Application Security Verification Standard), v2 control groups STORAGE, CRYPTO, AUTH, NETWORK, PLATFORM, CODE, RESILIENCE, PRIVACY https://mas.owasp.org/MASVS/
- **[MASWE]** OWASP MASWE (Mobile Application Security Weakness Enumeration), weakness list MASWE-0001 to MASWE-0078 as published on 2026-11-21. https://mas.owasp.org/MASWE/
- **[MASWE-0001]** MASWE-0001 Sensitive Data Stored Unencrypted in Private Storage. https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0001/
- **[MASWE-0004]** MASWE-0004 Sensitive Data Hardcoded in the App Package. https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0004/
- **[MASWE-0005]** MASWE-0005 Insertion of Sensitive Data into Logs. https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0005/
- **[MASWE-0006]** MASWE-0006 Sensitive Data Not Excluded From Backup. https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0006/
- **[MASWE-0007]** MASWE-0007 Improper Encryption. https://mas.owasp.org/MASWE/MASVS-CRYPTO/MASWE-0007/
- **[MASWE-0008]** MASWE-0008 Improper Hashing. https://mas.owasp.org/MASWE/MASVS-CRYPTO/MASWE-0008/
- **[MASWE-0026]** MASWE-0026 Network Traffic Not Encrypted. https://mas.owasp.org/MASWE/MASVS-NETWORK/MASWE-0026/
- **[MASWE-0027]** MASWE-0027 Insecure Certificate Validation. https://mas.owasp.org/MASWE/MASVS-NETWORK/MASWE-0027/
- **[MASWE-0032]** MASWE-0032 Insecure Intents. https://mas.owasp.org/MASWE/MASVS-PLATFORM/MASWE-0032/
- **[MASWE-0038]** MASWE-0038 Insufficient Protection of Sensitive Data from Screenshots or Screen Recordings. https://mas.owasp.org/MASWE/MASVS-PLATFORM/MASWE-0038/
- **[MASWE-0044]** MASWE-0044 Dependencies with Known Vulnerabilities. https://mas.owasp.org/MASWE/MASVS-CODE/MASWE-0044/
- **[MASWE-0050]** MASWE-0050 Unsafe Handling of Untrusted Data. https://mas.owasp.org/MASWE/MASVS-CODE/MASWE-0050/
- **[MASWE-0063]** MASWE-0063 Debug Mechanisms Not Disabled. https://mas.owasp.org/MASWE/MASVS-RESILIENCE/MASWE-0063/
- **[MobSF]** Mobile Security Framework (MobSF). https://github.com/MobSF/Mobile-Security-Framework-MobSF
- **[Mousavi2026]** Mousavi, Z., Islam, C., Babar, M.A., Abuadbba, A. "Understanding the Impact of AI Code Assistants on Security API Usage: An Empirical Study." arXiv:2607.11348 (preprint, 2026). https://arxiv.org/abs/2607.11348
- **[OkHttp-Logging]** OkHttp logging-interceptor README (square/okhttp). https://raw.githubusercontent.com/square/okhttp/master/okhttp-logging-interceptor/README.md
- **[Oltrogge2021]** Oltrogge, M., Huaman, N., Klivan, S., Acar, Y., Backes, M., Fahl, S. "Why Eve and Mallory Still Love Android: Revisiting TLS (In)Security in Android Applications." USENIX Security 2021. https://www.usenix.org/conference/usenixsecurity21/presentation/oltrogge
- **[Paging]** Paging library overview, Android Developers. https://developer.android.com/topic/libraries/architecture/paging/v3-overview
- **[Pearce2022]** Pearce, H., Ahmad, B., Tan, B., Dolan-Gavitt, B. et al. "Asleep at the Keyboard? Assessing the Security of GitHub Copilot’s Code Contributions." IEEE Symposium on Security and Privacy (S&P) 2022, pp. 754–768. https://doi.org/10.1109/SP46214.2022.9833571 ; https://arxiv.org/abs/2108.09293
- **[Perry2023]** Perry, N., Srivastava, M., Kumar, D., Boneh, D. "Do Users Write More Insecure Code with AI Assistants?" ACM CCS 2023, pp. 2785–2799. https://doi.org/10.1145/3576915.3623157 ; https://arxiv.org/abs/2211.03622
- **[R8]** Enable app optimization with R8, Android Developers. https://developer.android.com/topic/performance/app-optimization/enable-app-optimization
- **[Room-Fts4]** Fts4 annotation, Room API reference. https://developer.android.com/reference/androidx/room/Fts4
- **[Shukla2025]** Shukla, S., Joshi, H., Syed, R. "Security Degradation in Iterative AI Code Generation: A Systematic Analysis of the Paradox." arXiv:2506.11022 (preprint, 2025). https://arxiv.org/abs/2506.11022
- **[Spracklen2025]** Spracklen, J., Wijewickrama, R., Sakib, A.H.M.N., Maiti, A., Viswanath, B., Jadliwala, M. "We Have a Package for You! A Comprehensive Analysis of Package Hallucinations by Code Generating LLMs." USENIX Security 2025, pp. 3687–3706. https://arxiv.org/abs/2406.10279 ; https://www.usenix.org/conference/usenixsecurity25/presentation/spracklen
- **[Veracode2025]** Veracode. "AI-Generated Code Poses Major Security Risks in Nearly Half of All Development Tasks, Veracode Research Reveals" (2025 GenAI Code Security Report), press release, 30 July 2025. Vendor report, not peer-reviewed. https://www.veracode.com/press-release/ai-generated-code-poses-major-security-risks-in-nearly-half-of-all-development-tasks-veracode-research-reveals/
- **[Wang2025]** Wang, C., Huang, K., Zhang, J., Feng, Y. et al. "LLMs Meet Library Evolution: Evaluating Deprecated API Usage in LLM-based Code Completion." ICSE 2025. DOI 10.1109/ICSE55347.2025.00245. https://doi.org/10.1109/ICSE55347.2025.00245 ; https://arxiv.org/abs/2406.09834
- **[Wilson1927]** Wilson, E.B. "Probable Inference, the Law of Succession, and Statistical Inference." Journal of the American Statistical Association 22(158):209–212, 1927. https://doi.org/10.1080/01621459.1927.10502953
- **[Zheng2026]** Zheng, J., Guan, H., Liu, Y. "When LLMs Invent Rust Crates: An Empirical Study of Hallucination Patterns and Mitigation." arXiv:2606.08444 (preprint, 2026). https://arxiv.org/abs/2606.08444

<!-- REFS-END -->
