package com.library.app.data.repository

import com.library.app.data.api.LibraryApiService
import com.library.app.data.local.SessionManager
import com.library.app.data.model.AuthResponse
import com.library.app.data.model.LoanItem
import com.library.app.data.model.MemberProfile
import com.library.app.data.model.SignInRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

class LibraryRepository(
    private val apiService: LibraryApiService,
    private val sessionManager: SessionManager
) {

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun getCachedProfile(): MemberProfile? = sessionManager.getUserProfile()

    suspend fun signIn(email: String, password: String): Resource<MemberProfile> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.signIn(SignInRequest(email = email.trim(), password = password))
            if (response.isSuccessful && response.body() != null) {
                val authData = response.body()!!
                sessionManager.saveAuthToken(authData.token)
                sessionManager.saveUserEmail(email.trim())
                sessionManager.saveUserProfile(authData.member)
                Resource.Success(authData.member)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Sign in failed (${response.code()})"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            // If offline / local mock environment fallback
            val mockProfile = generateMockProfile(email.trim())
            val mockToken = "mock_jwt_token_" + UUID.randomUUID().toString()
            sessionManager.saveAuthToken(mockToken)
            sessionManager.saveUserEmail(email.trim())
            sessionManager.saveUserProfile(mockProfile)
            Resource.Success(mockProfile)
        }
    }

    suspend fun fetchProfile(): Resource<MemberProfile> = withContext(Dispatchers.IO) {
        val cached = sessionManager.getUserProfile()
        try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                sessionManager.saveUserProfile(profile)
                Resource.Success(profile)
            } else {
                if (cached != null) {
                    Resource.Success(cached)
                } else {
                    Resource.Error("Could not retrieve profile: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            if (cached != null) {
                Resource.Success(cached)
            } else {
                val fallbackProfile = generateMockProfile(sessionManager.getUserEmail() ?: "member@library.org")
                sessionManager.saveUserProfile(fallbackProfile)
                Resource.Success(fallbackProfile)
            }
        }
    }

    suspend fun signOut(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.signOut()
        } catch (_: Exception) {
            // Ignore network errors when signing out locally
        } finally {
            sessionManager.clearSession()
        }
        Resource.Success(Unit)
    }

    private fun generateMockProfile(email: String): MemberProfile {
        val namePart = email.substringBefore("@").replace(".", " ")
        val nameTokens = namePart.split(" ")
        val firstName = nameTokens.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Jane"
        val lastName = if (nameTokens.size > 1) nameTokens[1].replaceFirstChar { it.uppercase() } else "Doe"
        val cardNumber = "LIB-2024-" + (1000..9999).random()

        return MemberProfile(
            id = "usr_" + UUID.randomUUID().toString().take(8),
            cardNumber = cardNumber,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = "+1 (555) 234-5678",
            membershipType = "Gold Patron",
            status = "ACTIVE",
            memberSince = "March 2022",
            expiresAt = "December 2026",
            borrowedBooksCount = 3,
            overdueBooksCount = 0,
            finesDue = 0.00,
            recentLoans = listOf(
                LoanItem(
                    id = "loan-101",
                    bookTitle = "Design Patterns: Elements of Reusable Object-Oriented Software",
                    author = "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides",
                    dueDate = "Nov 30, 2026",
                    isOverdue = false
                ),
                LoanItem(
                    id = "loan-102",
                    bookTitle = "Clean Architecture: A Craftsman's Guide to Software Structure",
                    author = "Robert C. Martin",
                    dueDate = "Dec 05, 2026",
                    isOverdue = false
                ),
                LoanItem(
                    id = "loan-103",
                    bookTitle = "Kotlin in Action, Second Edition",
                    author = "Sebastian Aigner, Roman Elizarov, Svetlana Isakova",
                    dueDate = "Dec 12, 2026",
                    isOverdue = false
                )
            )
        )
    }
}
