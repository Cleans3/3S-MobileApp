package com.example.libraryapp.data

import com.example.libraryapp.data.api.AuthToken
import com.example.libraryapp.data.api.LibraryApi
import com.example.libraryapp.data.api.LoginRequest
import com.example.libraryapp.data.api.MemberProfile
import com.example.libraryapp.data.session.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import retrofit2.HttpException
import java.io.IOException

class InvalidCredentialsException : Exception("The email or password is incorrect.")

class SessionExpiredException : Exception("Your session has expired. Please sign in again.")

/** Maps any failure to a message that can be shown to the member. */
fun Throwable.toUserMessage(): String = when (this) {
    is InvalidCredentialsException, is SessionExpiredException -> message ?: "Something went wrong."
    is IOException -> "Could not reach the library server. Check your connection and try again."
    is HttpException -> "The library server returned an error (HTTP ${code()}). Please try again."
    else -> message ?: "Something went wrong. Please try again."
}

/** Sign-in state and member account operations, backed by the library REST API. */
class AccountRepository(
    private val api: LibraryApi,
    private val session: SessionStore
) {

    /**
     * True while a token is stored. Collecting this flow also keeps the in-memory
     * token used by the HTTP client in sync with the stored one.
     */
    val isSignedIn: Flow<Boolean> = session.token
        .onEach { AuthToken.value = it }
        .map { !it.isNullOrBlank() }

    /** Signs in with email and password and stores the returned token. */
    suspend fun signIn(email: String, password: String) {
        val token = try {
            api.login(LoginRequest(email = email.trim(), password = password)).token
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) throw InvalidCredentialsException()
            throw e
        }
        AuthToken.value = token
        session.saveToken(token)
    }

    /** Loads the profile of the signed-in member. Signs out if the token is rejected. */
    suspend fun currentProfile(): MemberProfile {
        try {
            return api.currentMember()
        } catch (e: HttpException) {
            if (e.code() == 401) {
                signOut()
                throw SessionExpiredException()
            }
            throw e
        }
    }

    suspend fun signOut() {
        AuthToken.value = null
        session.clearToken()
    }
}
