// ID: code-02
// caption (vi): Cùng yêu cầu nhưng có bộ quy tắc: mã đăng nhập được mã hóa bằng khóa nằm trong két bảo mật của điện thoại (Keystore) rồi mới lưu
// claim: Cùng một đề bài (B2) + cùng prompt, chỉ thêm tệp guardrail: AI tự dùng Android Keystore + AES-GCM, không còn lưu token thô
//
// source: optimization/study/e1-corpus/runs/codex-luna-B2-guardrailed-r1/src/app/src/main/java/vn/shelftap/app/SessionStore.kt
// lines: 45-53, 60, 62-63
// run id: codex-luna-B2-guardrailed-r1 (same tool/model/brief/prompt as code-01; only difference: guardrail file ANDROID-GUARDRAIL.md, sha256 ae5639033f70...)
// E1 detector: no S01 or S05 finding for this run (results/findings.csv)
//

    suspend fun write(session: Session) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val json = JSONObject()
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("mustChangePassword", session.mustChangePassword)
        sessionFile.writeBytes(cipher.iv + cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8)))
    }
        // …
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        // …
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
