package com.example.libraryapp.data

import com.example.libraryapp.data.api.AuthTokenHolder
import com.example.libraryapp.data.api.LibraryApi
import com.example.libraryapp.data.model.LoginRequest
import com.example.libraryapp.data.model.MemberProfile
import com.example.libraryapp.data.session.TokenStorage
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** User-facing failures of the account flow. */
sealed class AccountException(message: String) : Exception(message) {
    class InvalidCredentials : AccountException("Email or password is incorrect.")
    class SessionExpired : AccountException("Your session has expired. Please sign in again.")
    class Network : AccountException("Could not reach the library server. Check your connection and try again.")
    class Server(code: Int) : AccountException("The library server returned an error ($code). Please try again later.")
    class Unexpected : AccountException("Something went wrong. Please try again.")
}

class AccountRepository(
    private val api: LibraryApi,
    private val tokenStorage: TokenStorage,
    private val tokenHolder: AuthTokenHolder,
) {

    /** Loads the saved token into memory. Returns true when a member is still signed in. */
    suspend fun restoreSession(): Boolean {
        val token = tokenStorage.currentToken()
        tokenHolder.token = token
        return token != null
    }

    /** Signs in with email and password, then loads the member's profile. */
    suspend fun signIn(email: String, password: String): Result<MemberProfile> {
        val login = guard(AccountException.InvalidCredentials()) {
            api.login(LoginRequest(email = email.trim(), password = password))
        }.getOrElse { return Result.failure(it) }

        tokenHolder.token = login.accessToken
        tokenStorage.save(login.accessToken)

        return getProfile().also { result ->
            if (result.isFailure) clearSession()
        }
    }

    /** Fetches the signed-in member's profile. Clears the session if the server rejects the token. */
    suspend fun getProfile(): Result<MemberProfile> {
        val result = guard(AccountException.SessionExpired()) { api.getMyProfile() }
        if (result.exceptionOrNull() is AccountException.SessionExpired) clearSession()
        return result
    }

    /** Signs out. The local session is always cleared, even if the server call fails. */
    suspend fun signOut() {
        if (tokenHolder.token != null) {
            guard(AccountException.SessionExpired()) { api.logout() }
        }
        clearSession()
    }

    private suspend fun clearSession() {
        tokenHolder.token = null
        tokenStorage.clear()
    }

    private suspend fun <T> guard(unauthorized: AccountException, block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val error = if (e.code() == 401) unauthorized else AccountException.Server(e.code())
            Result.failure(error)
        } catch (e: IOException) {
            Result.failure(AccountException.Network())
        } catch (e: Exception) {
            Result.failure(AccountException.Unexpected())
        }
    }
}
