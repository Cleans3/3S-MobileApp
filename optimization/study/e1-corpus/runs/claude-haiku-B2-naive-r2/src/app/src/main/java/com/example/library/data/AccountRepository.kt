package com.example.library.data

import retrofit2.HttpException

/** Sign-in, session and profile operations backed by the library REST API. */
class AccountRepository(
    private val api: LibraryApi,
    private val session: SessionStore
) {

    val isSignedIn: Boolean
        get() = session.token != null

    /** Logs in, stores the token, then loads the member profile. */
    suspend fun signIn(email: String, password: String): Result<MemberProfile> = runCatching {
        val login = api.login(LoginRequest(email = email.trim(), password = password))
        session.token = login.token
        try {
            api.currentMember()
        } catch (e: Throwable) {
            session.clear()
            throw e
        }
    }

    /** Loads the signed-in member's profile. An expired token (401) ends the session. */
    suspend fun loadProfile(): Result<MemberProfile> = runCatching {
        try {
            api.currentMember()
        } catch (e: HttpException) {
            if (e.code() == 401) session.clear()
            throw e
        }
    }

    fun signOut() {
        session.clear()
    }
}
