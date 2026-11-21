package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

class MemberRepository(context: Context) {
    private val api = ApiClient()
    private val store = SessionStore(context.applicationContext)
    private val refreshMutex = Mutex()

    suspend fun signIn(email: String, password: String): MemberProfile = withContext(Dispatchers.IO) {
        val session = api.login(email, password, store.deviceId())
        store.save(session)
        if (session.mustChangePassword) return@withContext pendingPasswordProfile(email)
        authorized { api.profile(it) }
    }

    suspend fun restore(): MemberProfile? = withContext(Dispatchers.IO) {
        val session = store.read() ?: return@withContext null
        if (session.mustChangePassword) return@withContext pendingPasswordProfile("")
        runCatching { authorized { api.profile(it) } }.getOrElse {
            if (it is SessionExpiredException) null else throw it
        }
    }

    suspend fun changePassword(current: String, new: String): MemberProfile = withContext(Dispatchers.IO) {
        authorized { token -> api.changePassword(token, current, new) }
        authorized { api.profile(it) }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val session = store.read()
        if (session != null) runCatching { api.logout(session.refreshToken) }
        store.clear()
    }

    fun clearLocalSession() = store.clear()

    private fun pendingPasswordProfile(email: String) = MemberProfile("", "", email, "", "", false, true)

    private suspend fun <T> authorized(call: suspend (String) -> T): T {
        val session = store.read() ?: throw SessionExpiredException()
        return try {
            call(session.accessToken)
        } catch (error: ApiException) {
            if (error.code != "TOKEN_INVALID" && !(error.status == 401 && error.code == null)) throw error
            val refreshed = try {
                refreshMutex.withLock {
                    val current = store.read() ?: throw SessionExpiredException()
                    if (current.accessToken != session.accessToken) current
                    else api.refresh(current.refreshToken, store.deviceId()).also { store.save(it) }
                }
            } catch (_: Exception) {
                store.clear()
                throw SessionExpiredException()
            }
            try { call(refreshed.accessToken) } catch (second: ApiException) {
                if (second.code == "TOKEN_INVALID" || second.status == 401) {
                    store.clear()
                    throw SessionExpiredException()
                }
                throw second
            }
        } catch (error: IOException) {
            throw error
        }
    }
}
