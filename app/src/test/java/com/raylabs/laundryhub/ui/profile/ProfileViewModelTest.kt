package com.raylabs.laundryhub.ui.profile

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.profile.state.toUI
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var userUseCase: UserUseCase
    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userUseCase = mock(UserUseCase::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchUser sets user in uiState`() = runTest {
        // Given
        val user = User("id", "Uray", "uray@mail.com", "photoUrl")
        `when`(userUseCase.getCurrentUser()).thenReturn(user)

        // When
        viewModel = ProfileViewModel(userUseCase)
        val actual = viewModel.uiState.value.user.data

        // Then
        assertEquals(user.toUI(), actual)
    }

    @Test
    fun `logOut updates state and calls onSuccess`() = runTest {
        // Given
        `when`(userUseCase.signOut()).thenReturn(true)
        viewModel = ProfileViewModel(userUseCase)
        var called = false

        // When
        viewModel.logOut { called = true }
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.logout.data == true)
        assertTrue(called)
    }

    @Test
    fun `logOut updates state with false if failed`() = runTest {
        // Given
        `when`(userUseCase.signOut()).thenReturn(false)
        viewModel = ProfileViewModel(userUseCase)
        var called = false

        // When
        viewModel.logOut { called = true }
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(false, state.logout.data)
        assertTrue(!called)
    }
}