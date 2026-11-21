package vn.shelftap.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class SecureSnapshotStore(private val context: Context) {
    private val file = File(context.filesDir, "shelftap_snapshot.bin")
    private val alias = "shelftap_snapshot_key"
    private fun key() = (KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey(alias, null) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build() }.generateKey()).also { }
    private fun encrypt(value: String): ByteArray { val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key()); return cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)) }
    private fun decrypt(bytes: ByteArray): String { val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))); return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8) }
    suspend fun saveLoans(value: List<Loan>) = save("loans", JSONArray(value.map { org.json.JSONObject().put("loanId",it.loanId).put("copyId",it.copyId).put("titleId",it.titleId).put("title",it.title).put("barcode",it.barcode).put("outAt",it.outAt).put("dueAt",it.dueAt).put("returnedAt",it.returnedAt).put("renewals",it.renewals).put("renewable",it.renewable) }).toString())
    suspend fun saveHistory(value: List<HistoryItem>) = save("history", JSONArray(value.map { org.json.JSONObject().put("titleId",it.titleId).put("title",it.title).put("outAt",it.outAt).put("returnedAt",it.returnedAt) }).toString())
    private suspend fun save(kind: String, value: String) = withContext(Dispatchers.IO) { val old = if (file.exists()) runCatching { decrypt(file.readBytes()) }.getOrDefault("{}") else "{}"; val root = org.json.JSONObject(old).put(kind, value); file.writeBytes(encrypt(root.toString())) }
    suspend fun loadLoans() = load("loans")?.let { JSONArray(it).toLoans() }
    suspend fun loadHistory() = load("history")?.let { JSONArray(it).toHistory() }
    private suspend fun load(kind: String) = withContext(Dispatchers.IO) { if (!file.exists()) null else runCatching { org.json.JSONObject(decrypt(file.readBytes())).optString(kind).takeIf(String::isNotEmpty) }.getOrNull() }
}
