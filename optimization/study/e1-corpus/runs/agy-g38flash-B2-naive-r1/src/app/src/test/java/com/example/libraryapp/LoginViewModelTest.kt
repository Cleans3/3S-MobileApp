package com.example.libraryapp

import com.example.libraryapp.data.model.ApiResult
import com.example.libraryapp.data.model.AuthResponse
import com.example.libraryapp.data.model.LogoutResponse
import com.example.libraryapp.data.model.MemberDto
import com.example.libraryapp.data.model.MemberProfileResponse
import com.example.libraryapp.data.repository.LibraryRepository
import com.example.libraryapp.ui.screens.login.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: TestLibraryRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = TestLibraryRepository()
        viewModel = LoginViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emailValidation_invalidEmail_setsError() = runTest {
        viewModel.onEmailChanged("invalid-email")
        viewModel.onPasswordChanged("validPassword123")
        viewModel.signIn()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun passwordValidation_shortPassword_setsError() = runTest {
        viewModel.onEmailChanged("test@library.org")
        viewModel.onPasswordChanged("12")
        viewModel.signIn()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.passwordError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun signIn_successful_updatesStateToSuccess() = runTest {
        viewModel.onEmailChanged("user@library.org")
        viewModel.onPasswordChanged("secret123")
        viewModel.signIn()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertNull(viewModel.uiState.value.generalError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun signIn_failure_setsGeneralError() = runTest {
        fakeRepository.shouldFail = true
        viewModel.onEmailChanged("user@library.org")
        viewModel.onPasswordChanged("secret123")
        viewModel.signIn()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertEquals("Invalid credentials", viewModel.uiState.value.generalError)
    }
}

class TestLibraryRepository : LibraryRepository {
    var shouldFail = false
    private var loggedIn = false
    private val _isLoggedInFlow = MutableStateFlow(false)
    override val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow

    private val sampleMember = MemberDto(
        id = "1",
        memberNumber = "MEM-001",
        firstName = "Alex",
        lastName = "Morgan",
        email = "user@library.org",
        phone = "123456",
        membershipTier = "Standard",
        status = "Active",
        joinedDate = "2023-01-01",
        expiryDate = "2026-01-01"
    )

    override fun isLoggedIn(): Boolean = loggedIn

    override fun getCachedMember(): MemberDto? = if (loggedIn) sampleMember else null

    override suspend fun login(email: String, password: String): ApiResult<AuthResponse> {
        return if (!shouldFail) {
            loggedIn = true
            _isLoggedInFlow.value = true
            ApiResult.Success(AuthResponse("token_abc", "Bearer", 3600, sampleMember))
        } else {
            ApiResult.Error("Invalid credentials", 401)
        }
    }

    override suspend fun logout(): ApiResult<LogoutResponse> {
        loggedIn = false
        _isLoggedInFlow.value = false
        return ApiResult.Success(LogoutResponse(true, "Logged out"))
    }

    override suspend fun getProfile(): ApiResult<MemberProfileResponse> {
        return ApiResult.Success(MemberProfileResponse(sampleMember, emptyList(), 0, 0))
    }

    override fun clearSession() {
        loggedIn = false
        _isLoggedInFlow.value = false
    }
}
