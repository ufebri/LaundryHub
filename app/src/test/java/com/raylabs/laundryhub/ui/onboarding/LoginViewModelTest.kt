package com.raylabs.laundryhub.ui.onboarding

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.usecase.auth.CheckUserLoggedInUseCase
import com.raylabs.laundryhub.core.domain.usecase.auth.SignInWithGoogleUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockSignInWithGoogleUseCase: SignInWithGoogleUseCase = mock()
    private val mockCheckUserLoggedInUseCase: CheckUserLoggedInUseCase = mock()
    private val mockUserUseCase: UserUseCase = mock()

    private val dummyUser = User(
        uid = "abc123",
        displayName = "Raihan",
        email = "rai@labs.com",
        urlPhoto = "http://img.com/pp.jpg"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init with already logged in user updates userState`() = runTest {
        whenever(mockCheckUserLoggedInUseCase.invoke()).thenReturn(true)
        whenever(mockUserUseCase.getCurrentUser()).thenReturn(dummyUser)

        val vm = LoginViewModel(
            mockSignInWithGoogleUseCase,
            mockCheckUserLoggedInUseCase,
            mockUserUseCase
        )
        // Tidak perlu advanceUntilIdle karena tidak ada suspend
        assertEquals(dummyUser, vm.userState.value)
        assertNull(vm.errorState.value)
    }

    @Test
    fun `init with no logged in user keeps userState null`() = runTest {
        whenever(mockCheckUserLoggedInUseCase.invoke()).thenReturn(false)

        val vm = LoginViewModel(
            mockSignInWithGoogleUseCase,
            mockCheckUserLoggedInUseCase,
            mockUserUseCase
        )
        assertNull(vm.userState.value)
    }

    @Test
    fun `signInGoogle success updates userState and loading state`() = runTest {
        whenever(mockSignInWithGoogleUseCase.invoke(any())).thenReturn(dummyUser)
        whenever(mockCheckUserLoggedInUseCase.invoke()).thenReturn(false)

        val vm = LoginViewModel(
            mockSignInWithGoogleUseCase,
            mockCheckUserLoggedInUseCase,
            mockUserUseCase
        )

        vm.signInGoogle("token")
        advanceUntilIdle()

        assertEquals(dummyUser, vm.userState.value)
        assertFalse(vm.isLoading.value)
        assertNull(vm.errorState.value)
    }

    @Test
    fun `signInGoogle fail sets errorState`() = runTest {
        whenever(mockSignInWithGoogleUseCase.invoke(any())).thenReturn(null)
        whenever(mockCheckUserLoggedInUseCase.invoke()).thenReturn(false)

        val vm = LoginViewModel(
            mockSignInWithGoogleUseCase,
            mockCheckUserLoggedInUseCase,
            mockUserUseCase
        )

        vm.signInGoogle("token")
        advanceUntilIdle()

        assertNull(vm.userState.value)
        assertFalse(vm.isLoading.value)
        assertEquals("Failed to sign in", vm.errorState.value)
    }

    @Test
    fun `signInGoogle handles exception as error`() = runTest {
        whenever(mockSignInWithGoogleUseCase.invoke(any())).thenThrow(RuntimeException("Network error"))
        whenever(mockCheckUserLoggedInUseCase.invoke()).thenReturn(false)

        val vm = LoginViewModel(
            mockSignInWithGoogleUseCase,
            mockCheckUserLoggedInUseCase,
            mockUserUseCase
        )

        vm.signInGoogle("token")
        advanceUntilIdle()

        assertNull(vm.userState.value)
        assertFalse(vm.isLoading.value)
        assertEquals("Network error", vm.errorState.value)
    }

    @Test
    fun `clearUser sets userState to null`() {
        whenever(mockCheckUserLoggedInUseCase.invoke()).thenReturn(true)
        whenever(mockUserUseCase.getCurrentUser()).thenReturn(dummyUser)
        val vm = LoginViewModel(
            mockSignInWithGoogleUseCase,
            mockCheckUserLoggedInUseCase,
            mockUserUseCase
        )
        vm.clearUser()
        assertNull(vm.userState.value)
    }
}