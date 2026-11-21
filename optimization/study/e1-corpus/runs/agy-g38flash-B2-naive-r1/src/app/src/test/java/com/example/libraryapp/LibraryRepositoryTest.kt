package com.example.libraryapp

import com.example.libraryapp.data.local.SessionManager
import com.example.libraryapp.data.model.ApiResult
import com.example.libraryapp.data.model.AuthResponse
import com.example.libraryapp.data.model.LoginRequest
import com.example.libraryapp.data.model.LogoutResponse
import com.example.libraryapp.data.model.MemberDto
import com.example.libraryapp.data.model.MemberProfileResponse
import com.example.libraryapp.data.remote.LibraryApiService
import com.example.libraryapp.data.repository.LibraryRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class LibraryRepositoryTest {

    private lateinit var fakeApiService: FakeLibraryApiService
    private lateinit var fakeSessionManager: FakeSessionManager
    private lateinit var repository: LibraryRepositoryImpl

    @Before
    fun setUp() {
        fakeApiService = FakeLibraryApiService()
        fakeSessionManager = FakeSessionManager()
        repository = LibraryRepositoryImpl(
            apiService = fakeApiService,
            sessionManager = fakeSessionManager as SessionManager,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun login_success_savesTokenAndMember() = runBlocking {
        val result = repository.login("member@library.org", "password123")
        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals("test_token_123", data.token)
        assertEquals("member@library.org", data.member.email)
        assertTrue(repository.isLoggedIn())
        assertEquals("member@library.org", repository.getCachedMember()?.email)
    }

    @Test
    fun login_failure_returnsError() = runBlocking {
        fakeApiService.shouldFailLogin = true
        val result = repository.login("wrong@library.org", "bad_pass")
        assertTrue(result is ApiResult.Error)
        assertFalse(repository.isLoggedIn())
    }

    @Test
    fun logout_clearsSession() = runBlocking {
        repository.login("member@library.org", "password123")
        assertTrue(repository.isLoggedIn())

        repository.logout()
        assertFalse(repository.isLoggedIn())
        assertEquals(null, repository.getCachedMember())
    }

    @Test
    fun getProfile_success_returnsMemberProfile() = runBlocking {
        repository.login("member@library.org", "password123")
        val result = repository.getProfile()

        assertTrue(result is ApiResult.Success)
        val profile = (result as ApiResult.Success).data
        assertEquals("LIB-9999", profile.member.memberNumber)
        assertEquals(1, profile.activeLoans.size)
    }
}

class FakeSessionManager : SessionManager(createDummyContext()) {
    private var token: String? = null
    private var member: MemberDto? = null

    override fun saveAuthToken(token: String) {
        this.token = token
    }

    override fun getAuthToken(): String? = token

    override fun saveMember(member: MemberDto) {
        this.member = member
    }

    override fun getMember(): MemberDto? = member

    override fun isLoggedIn(): Boolean = !token.isNullOrBlank()

    override fun clearSession() {
        token = null
        member = null
    }

    companion object {
        private fun createDummyContext(): android.content.Context {
            // Unused by overridden methods
            return android.test.mock.MockContext() rescueFallback android.content.ContextWrapper(null)
        }
    }
}

private infix fun Any.rescueFallback(fallback: android.content.Context): android.content.Context {
    return try {
        this as android.content.Context
    } catch (e: Throwable) {
        fallback
    }
}

class FakeLibraryApiService : LibraryApiService {
    var shouldFailLogin = false
    var shouldFailProfile = false

    private val sampleMember = MemberDto(
        id = "mem_1",
        memberNumber = "LIB-9999",
        firstName = "Jane",
        lastName = "Doe",
        email = "member@library.org",
        phone = "+1 555-0199",
        membershipTier = "Standard",
        status = "Active",
        joinedDate = "2023-01-01",
        expiryDate = "2025-01-01",
        borrowedBooksCount = 1,
        maxLoans = 5,
        outstandingFines = 0.0
    )

    override suspend fun login(request: LoginRequest): Response<AuthResponse> {
        return if (!shouldFailLogin) {
            Response.success(
                AuthResponse(
                    token = "test_token_123",
                    tokenType = "Bearer",
                    expiresIn = 3600,
                    member = sampleMember
                )
            )
        } else {
            Response.error(
                401,
                "{\"error\":\"Unauthorized\"}".toResponseBody("application/json".toMediaType())
            )
        }
    }

    override suspend fun logout(): Response<LogoutResponse> {
        return Response.success(LogoutResponse(success = true, message = "Logged out"))
    }

    override suspend fun getMemberProfile(): Response<MemberProfileResponse> {
        return if (!shouldFailProfile) {
            Response.success(
                MemberProfileResponse(
                    member = sampleMember,
                    activeLoans = listOf(
                        com.example.libraryapp.data.model.BorrowedBookDto(
                            id = "book_1",
                            title = "Kotlin Programming",
                            author = "Josh Skeen",
                            isbn = "978-0135161630",
                            borrowDate = "2024-11-01",
                            dueDate = "2024-11-30",
                            isOverdue = false
                        )
                    ),
                    loanHistoryCount = 10,
                    reservationsCount = 0
                )
            )
        } else {
            Response.error(
                401,
                "{\"error\":\"Unauthorized\"}".toResponseBody("application/json".toMediaType())
            )
        }
    }
}
