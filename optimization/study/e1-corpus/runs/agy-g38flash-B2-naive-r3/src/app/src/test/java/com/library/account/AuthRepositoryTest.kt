package com.library.account

import com.library.account.data.api.LibraryApiService
import com.library.account.data.model.LoginRequest
import com.library.account.data.model.LoginResponse
import com.library.account.data.model.MemberProfile
import com.library.account.data.repository.AuthRepository
import com.library.account.data.session.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import retrofit2.Response

class AuthRepositoryTest {

    private lateinit var mockApiService: LibraryApiService
    private lateinit var mockSessionManager: SessionManager
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        mockApiService = mock(LibraryApiService::class.java)
        mockSessionManager = mock(SessionManager::class.java)
        `when`(mockSessionManager.isLoggedIn()).thenReturn(false)
        authRepository = AuthRepository(mockApiService, mockSessionManager)
    }

    @Test
    fun login_successful_savesSession_and_returnsProfile() = runBlocking {
        val email = "member@library.org"
        val password = "secretPassword"
        val loginResponse = LoginResponse(
            token = "jwt_token_12345",
            tokenType = "Bearer",
            expiresIn = 86400,
            memberId = "MEM-001",
            email = email,
            fullName = "Jane Reader",
            memberNumber = "LIB-99120"
        )
        val profile = MemberProfile(
            id = "MEM-001",
            memberNumber = "LIB-99120",
            fullName = "Jane Reader",
            email = email,
            phoneNumber = "555-1234",
            membershipType = "General Adult",
            membershipStatus = "ACTIVE",
            expiryDate = "2027-01-01",
            joinedDate = "2022-01-01",
            homeBranch = "Main Branch",
            activeLoansCount = 0,
            holdsReadyCount = 0,
            outstandingFees = 0.0,
            borrowedBooks = emptyList()
        )

        `when`(mockApiService.login(LoginRequest(email, password)))
            .thenReturn(Response.success(loginResponse))
        `when`(mockApiService.getProfile())
            .thenReturn(Response.success(profile))

        val result = authRepository.login(email, password, staySignedIn = true)

        assertTrue(result.isSuccess)
        assertEquals("Jane Reader", result.getOrNull()?.fullName)
        verify(mockSessionManager).saveSession("jwt_token_12345", email, "Jane Reader", "LIB-99120", true)
        verify(mockSessionManager).saveCachedProfile(profile)
    }

    @Test
    fun login_failure_returnsError() = runBlocking {
        val email = "member@library.org"
        val password = "wrongPassword"

        `when`(mockApiService.login(LoginRequest(email, password)))
            .thenReturn(Response.error(401, "{}".toResponseBody()))

        val result = authRepository.login(email, password, staySignedIn = true)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid library member credentials") == true)
        verify(mockSessionManager, never()).saveSession(anyString(), anyString(), anyString(), anyString(), anyBoolean())
    }

    @Test
    fun logout_clearsSession() = runBlocking {
        `when`(mockApiService.logout()).thenReturn(Response.success(Unit))

        authRepository.logout()

        verify(mockSessionManager).clearSession()
        assertFalse(authRepository.isLoggedIn.value)
    }
}
