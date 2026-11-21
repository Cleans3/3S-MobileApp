package com.example.libraryapp

import com.example.libraryapp.data.model.ApiResult
import com.example.libraryapp.data.model.MemberDto
import com.example.libraryapp.data.model.MemberProfileResponse
import com.example.libraryapp.ui.screens.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: TestLibraryRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = TestLibraryRepository()
        viewModel = ProfileViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadProfile_success_updatesStateWithMemberAndLoans() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.member)
        assertEquals("Alex Morgan", state.member?.fullName)
        assertNull(state.errorMessage)
    }

    @Test
    fun signOut_updatesStateToSignedOut() = runTest {
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSignedOut)
        assertNull(state.member)
        assertTrue(state.activeLoans.isEmpty())
    }
}
