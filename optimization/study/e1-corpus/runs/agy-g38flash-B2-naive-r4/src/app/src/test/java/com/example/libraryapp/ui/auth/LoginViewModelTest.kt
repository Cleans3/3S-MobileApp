package com.example.libraryapp.ui.auth

import com.example.libraryapp.data.model.MemberProfile
import com.example.libraryapp.data.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val authRepository: AuthRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LoginViewModel

    private val mockProfile = MemberProfile(
        id = "mem_1",
        cardNumber = "LIB-1234",
        email = "reader@library.org",
        firstName = "Alice",
        lastName = "Smith",
        memberSince = "2022-05-10"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updating email and password updates ui state`() {
        viewModel.onEmailChanged("reader@library.org")
        viewModel.onPasswordChanged("secret123")

        val state = viewModel.uiState.value
        assertEquals("reader@library.org", state.email)
        assertEquals("secret123", state.password)
    }

    @Test
    fun `toggle password visibility switches boolean flag`() {
        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.onTogglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)
        viewModel.onTogglePasswordVisibility()
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun `login with empty fields sets validation error message without calling repository`() {
        var successCalled = false
        viewModel.login(onSuccess = { successCalled = true })

        assertFalse(successCalled)
        assertNotNull(viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `successful login updates state and triggers onSuccess callback`() = runTest {
        coEvery { authRepository.login("reader@library.org", "validpass") } returns Result.success(mockProfile)

        viewModel.onEmailChanged("reader@library.org")
        viewModel.onPasswordChanged("validpass")

        var successCalled = false
        viewModel.login(onSuccess = { successCalled = true })

        testScheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(mockProfile, viewModel.uiState.value.loginSuccessUser)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `failed login displays error message`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.failure(Exception("Bad credentials"))

        viewModel.onEmailChanged("reader@library.org")
        viewModel.onPasswordChanged("wrongpassword")

        var successCalled = false
        viewModel.login(onSuccess = { successCalled = true })

        testScheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals("Bad credentials", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
