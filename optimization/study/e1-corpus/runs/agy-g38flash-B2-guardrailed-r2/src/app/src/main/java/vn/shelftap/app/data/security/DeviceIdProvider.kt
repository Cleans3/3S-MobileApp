package vn.shelftap.app.data.security

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DeviceIdProvider(
    private val context: Context,
    private val crypto: KeystoreCrypto,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val deviceIdFile = File(context.filesDir, "device_id.bin")

    @Volatile
    private var cachedDeviceId: String? = null

    suspend fun getDeviceId(): String = withContext(ioDispatcher) {
        cachedDeviceId?.let { return@withContext it }

        synchronized(this@DeviceIdProvider) {
            cachedDeviceId?.let { return@synchronized it }

            if (deviceIdFile.exists()) {
                try {
                    val encrypted = deviceIdFile.readBytes()
                    val decrypted = crypto.decrypt(encrypted)
                    val id = String(decrypted, Charsets.UTF_8).trim()
                    if (id.isNotEmpty()) {
                        cachedDeviceId = id
                        return@synchronized id
                    }
                } catch (e: Exception) {
                    // Corrupted or tampered file, generate fresh below
                }
            }

            val newId = UUID.randomUUID().toString()
            val encrypted = crypto.encrypt(newId.toByteArray(Charsets.UTF_8))
            deviceIdFile.writeBytes(encrypted)
            cachedDeviceId = newId
            newId
        }
    }

    fun getDeviceIdSync(): String {
        cachedDeviceId?.let { return it }

        synchronized(this) {
            cachedDeviceId?.let { return it }

            if (deviceIdFile.exists()) {
                try {
                    val encrypted = deviceIdFile.readBytes()
                    val decrypted = crypto.decrypt(encrypted)
                    val id = String(decrypted, Charsets.UTF_8).trim()
                    if (id.isNotEmpty()) {
                        cachedDeviceId = id
                        return id
                    }
                } catch (e: Exception) {
                    // Fall through to generate fresh ID
                }
            }

            val newId = UUID.randomUUID().toString()
            val encrypted = crypto.encrypt(newId.toByteArray(Charsets.UTF_8))
            deviceIdFile.writeBytes(encrypted)
            cachedDeviceId = newId
            return newId
        }
    }
}
