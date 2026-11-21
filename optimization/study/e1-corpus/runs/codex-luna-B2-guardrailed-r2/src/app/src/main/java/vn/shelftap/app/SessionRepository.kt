package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class SessionRepository(context: Context, private val io: CoroutineDispatcher = Dispatchers.IO) {
    private val store = SecureStore(context)
    private val api = ShelfTapApi(io)
    private val refreshLock = Mutex()

    private suspend fun deviceId(): String = withContext(io) {
        store.get("device_id") ?: UUID.randomUUID().toString().also { store.put("device_id", it) }
    }

    suspend fun signIn(email: String, password: String): Member {
        val (tokens, mustChange) = api.login(email.trim(), password, deviceId())
        withContext(io) { saveTokens(tokens) }
        val member = api.me(tokens.accessToken)
        return member.copy(mustChangePassword = mustChange || member.mustChangePassword)
    }

    suspend fun restore(): Member? {
        val access = withContext(io) { store.get("access_token") } ?: return null
        return try { authenticated { api.me(it) } } catch (_: ApiFailure) { clear(); null }
    }

    suspend fun changePassword(current: String, new: String): Member {
        authenticated { api.changePassword(it, current, new) }
        return authenticated { api.me(it) }
    }

    suspend fun signOut() {
        val refresh = withContext(io) { store.get("refresh_token") }
        if (refresh != null) {
            try { api.logout(refresh) } catch (_: ApiFailure) { /* Local sign-out still completes. */ }
        }
        clear()
    }

    private suspend fun <T> authenticated(block: suspend (String) -> T): T {
        val access = withContext(io) { store.get("access_token") } ?: throw ApiFailure(401, "TOKEN_INVALID", null, null)
        return try { block(access) } catch (failure: ApiFailure) {
            if (failure.status != 401 || failure.code != "TOKEN_INVALID") throw failure
            val refreshed = refreshLock.withLock {
                val latest = withContext(io) { store.get("access_token") }
                if (latest != null && latest != access) latest else refreshTokens()
            }
            try {
                block(refreshed)
            } catch (retryFailure: ApiFailure) {
                if (retryFailure.status == 401 && retryFailure.code == "TOKEN_INVALID") clear()
                throw retryFailure
            }
        }
    }

    private suspend fun refreshTokens(): String {
        val refresh = withContext(io) { store.get("refresh_token") } ?: throw ApiFailure(401, "TOKEN_INVALID", null, null)
        return try {
            val (tokens, _) = api.refresh(refresh, deviceId())
            withContext(io) { saveTokens(tokens) }
            tokens.accessToken
        } catch (failure: ApiFailure) {
            clear()
            throw failure
        }
    }

    private fun saveTokens(tokens: Tokens) {
        store.put("access_token", tokens.accessToken)
        store.put("refresh_token", tokens.refreshToken)
    }

    private suspend fun clear() = withContext(io) { store.removeAllExceptDevice() }
}

private fun SecureStore.removeAllExceptDevice() {
    val device = get("device_id")
    removeAll()
    if (device != null) put("device_id", device)
}
