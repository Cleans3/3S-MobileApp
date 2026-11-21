package vn.shelftap.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class EncryptedCache(context: Context, private val io: CoroutineDispatcher = Dispatchers.IO) {
    private val file = File(context.noBackupFilesDir, "shelftap-cache.bin")
    private val alias = "shelftap-cache-key"
    private fun key() = (KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey(alias, null)
        ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey())
    suspend fun save(loans: List<Loan>, history: List<HistoryItem>) = withContext(io) {
        val payload = JSONArray().put(JSONArray(loans.map { org.json.JSONObject().put("loanId", it.loanId).put("copyId", it.copyId).put("titleId", it.titleId).put("title", it.title).put("barcode", it.barcode).put("outAt", it.outAt).put("dueAt", it.dueAt).put("returnedAt", it.returnedAt).put("renewals", it.renewals).put("renewable", it.renewable) })).put(JSONArray(history.map { org.json.JSONObject().put("titleId", it.titleId).put("title", it.title).put("outAt", it.outAt).put("returnedAt", it.returnedAt) })).toString().toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        file.writeBytes(cipher.iv + cipher.doFinal(payload))
    }
    suspend fun load(): Pair<List<Loan>, List<HistoryItem>>? = withContext(io) {
        runCatching {
            val bytes = file.readBytes(); val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            val outer = JSONArray(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size))))
            val l = outer.getJSONArray(0); val h = outer.getJSONArray(1)
            Pair((0 until l.length()).map { l.getJSONObject(it).loan() }, (0 until h.length()).map { val x = h.getJSONObject(it); HistoryItem(x.str("titleId"), x.str("title"), x.str("outAt"), x.optionalString("returnedAt")) })
        }.getOrNull()
    }
}
