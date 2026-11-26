// ID: code-04
// caption (vi): Ảnh bìa sách cỡ 1500x2250 được giải mã nguyên kích thước (A); thư viện Coil chỉ giải mã đúng cỡ khung 150x210 và nhớ lại (B)
// claim: P06: một dòng BitmapFactory.decodeStream tải cả ảnh khổng lồ vào bộ nhớ; bản B chỉ cần một dòng AsyncImage
//
// A (AI draft, slop)
// source: android/optlab/src/main/kotlin/shelftap/optlab/corpus/p06/CoverBitmap.kt
// lines: 34, 36-38, 40
// run id: codex-luna-B3-detailed-functional-r1 (tool codex, model luna, brief B3, detailed-functional prompt)
// Original: optimization/study/e1-corpus/runs/codex-luna-B3-detailed-functional-r1/src/app/src/main/java/vn/shelftap/app/MainActivity.kt line 39 (a single 1346-character minified line containing the same BitmapFactory.decodeStream(URL(url).openStream()) call).
// The excerpt is the E2 lab port of that line, only reformatted (e2-bench/ports/P06.diff); the offending call is untouched.
//
// B (tuned, Coil)
// source: android/optlab/src/main/kotlin/shelftap/optlab/ui/CorpusScreens.kt
// lines: 129
// run id: n/a (tuned reference written for the E2 lab app, not an AI trial)
//

// ===== A (AI draft, slop) =====
fun CoverBitmap(coverUrl: String?) {
    // …
    LaunchedEffect(coverUrl) {
        val url = coverUrl
        if (url != null) bitmap = withContext(Dispatchers.IO) {
                // …
                runCatching { BitmapFactory.decodeStream(URL(url).openStream()) }.getOrNull()?.also {

// ===== B (tuned, Coil) =====
    AsyncImage(request, contentDescription = null, modifier = CorpusCoverSlot, contentScale = ContentScale.Crop)
