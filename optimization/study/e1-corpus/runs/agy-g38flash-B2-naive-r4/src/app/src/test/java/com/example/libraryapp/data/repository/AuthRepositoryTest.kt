package com.example.libraryapp.data.repository

import com.example.libraryapp.data.local.SessionManager
import com.example.libraryapp.data.model.LoginRequest
import com.example.libraryapp.data.model.LoginResponse
import com.example.libraryapp.data.model.MemberProfile
import com.example.libraryapp.data.remote.LibraryApiService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    private val apiService: LibraryApiService = mockk()
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private lateinit var repository: AuthRepository

    private val mockProfile = MemberProfile(
        id = "mem_1",
        cardNumber = "LIB-1234",
        email = "member@library.org",
        firstName = "John",
        lastName = "Doe",
        membershipTier = "Standard",
        memberSince = "2023-01-01",
        status = "Active",
        activeLoansCount = 1,
        holdsCount = 0,
        outstandingFines = 0.0,
        loans = emptyList()
    )

    @Before
    fun setup() {
        repository = AuthRepositoryImpl(apiService, sessionManager)
    }

    @Test
    fun `login success saves token and profile`() = runTest {
        val loginResponse = LoginResponse(
            accessToken = "token_abc123",
            refreshToken = "refresh_xyz",
            tokenType = "Bearer",
            expiresIn = 3600,
            member = mockProfile
        )

        coEvery { apiService.login(LoginRequest("member@library.org", "password123")) } returns Response.success(loginResponse)

        val result = repository.login("member@library.org", "password123")

        assertTrue(result.isSuccess)
        assertEquals(mockProfile, result.getOrNull())

        verify { sessionManager.saveAuthToken("token_abc123") }
        verify { sessionManager.saveRefreshToken("refresh_xyz") }
        verify { sessionManager.saveUserProfile(mockProfile) }
    }

    @Test
    fun `login failure returns error result and does not save token`() = runTest {
        val errorResponseBody = "{\"message\":\"Invalid credentials\"}".toResponseBody()
        coEvery { apiService.login(any()) } returns Response.error(401, errorResponseBody)

        val result = repository.login("wrong@library.org", "wrongpass")

        assertTrue(result.isFailure)
        verify(exactly = 0) { sessionManager.saveAuthToken(any()) }
    }

    @Test
    fun `logout clears session manager tokens`() = runTest {
        coEvery { apiService.logout() } returns Response.success(Unit)

        val result = repository.logout()

        assertTrue(result.isSuccess)
        verify { sessionManager.clearSession() }
    }

    @Test
    fun `isLoggedIn delegates to session manager`() {
        every { sessionManager.isLoggedIn() } returns true
        assertTrue(repository.isLoggedIn())

        every { sessionManager.isLoggedIn() } returns false
        assertFalse(repository.isLoggedIn())
    }
}
