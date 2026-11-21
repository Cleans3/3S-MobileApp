package vn.shelftap.app.data.local

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Creates and persists a unique UUID deviceId on the first app launch,
 * keeping it for the entire life of the installation.
 */
class DeviceIdProvider(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        synchronized(this) {
            var deviceId = prefs.getString(KEY_DEVICE_ID, null)
            if (deviceId.isNullOrEmpty()) {
                deviceId = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
            }
            return deviceId
        }
    }

    companion object {
        private const val PREFS_NAME = "shelftap_device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
